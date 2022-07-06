/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.event.impl;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.lang.IgniteBiPredicate;

import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityAdvisor.SecurityAdvice;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventDelayHandler;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.LearningResourceStoreService.LRS_Statement;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.event.api.UsageSession;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.ignite.EagerIgniteSpringBean;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * BaseEventTrackingService is the base implmentation for the EventTracking service.
 * </p>
 */
@Slf4j
public class BaseEventTrackingService implements EventTrackingService
{
	@Setter
	private EagerIgniteSpringBean ignite;

	@Autowired
	private UsageSessionService usageSessionService;

	@Autowired
	private SessionManager sessionManager;

	@Autowired
	private SecurityService securityService;

	@Autowired
	private ToolManager toolManager;

	@Autowired
	private EntityManager entityManager;

	@Setter
	private EventDelayHandler eventDelayHandler;

	private IgniteMessaging messaging;
	
	private Map<Observer, IgniteBiPredicate> listenerMap = new HashMap<>();

	public void init() {

		log.info(this + ".init()");
		messaging = ignite.message(ignite.cluster().forLocal());
	}

	public void destroy() {

		log.info(this + ".destroy()");
	}

	public void postEvent(Event event) {

		messaging.send("EVENTS", event);
	}

	@Override
	public Event newEvent(String event, String resource, boolean modify) {

		return new BaseEvent(event, resource, modify, NotificationService.NOTI_OPTIONAL, null);
	}

	@Override
	public Event newEvent(String event, String resource, boolean modify, int priority) {

		return new BaseEvent(event, resource, modify, priority, null);
	}

	@Override
	public Event newEvent(String event, String resource, String context, boolean modify, int priority) {

		return new BaseEvent(event, resource, context, modify, priority, null);
	}

	@Override
	public Event newEvent(String event, String resource, String context, boolean modify, int priority, boolean isTransient) {

		return new BaseEvent(event, resource, context, modify, priority, isTransient);
	}

	@Override
	public Event newEvent(String event, String resource, String context, boolean modify, int priority, LRS_Statement lrsStatement) {

		return new BaseEvent(event, resource, context, modify, priority, lrsStatement);
	}


	@Override
	public void post(Event event) {

		BaseEvent be = ensureBaseEvent(event);
		// get the session id or user id
		String id = usageSessionService.getSessionId();
		if (id != null) {
			be.setSessionId(id);
		} else {
			// post for the session "thread" user
			id = sessionManager.getCurrentSessionUserId();
			if (id == null) {
				id = UNKNOWN_USER;
			}

			be.setUserId(id);
		}

		postEvent(be);
	}

	@Override
	public void post(Event event, UsageSession session) {

		BaseEvent be = ensureBaseEvent(event);
		String id = UNKNOWN_USER;
		if (session != null) id = session.getId();

		be.setSessionId(id);

		postEvent(be);
	}

	@Override
	public void post(Event event, User user) {

		BaseEvent be = ensureBaseEvent(event);
		String id = UNKNOWN_USER;
		if (user != null) id = user.getId();

		be.setUserId(id);

		// establish a security advisor if the user id is set on the event.  this ensures that
		// false permission exceptions aren't encountered during an event refiring.
		boolean useAdvisor = false;
		if (be.getUserId() != null)
		{
			useAdvisor = true;
			securityService.pushAdvisor(newResourceAdvisor(be.getUserId()));
		}

		postEvent(be);

		// if an advisor was used, pop it off.
		if (useAdvisor)
			securityService.popAdvisor();
	}

	@Override
	public void delay(Event event, Instant fireTime) {

		Instant now = Instant.now();
		if (fireTime == null || fireTime.isBefore(now)) {
			postEvent(event);
		} else {
			if (eventDelayHandler != null) {
				// Make sure there is a userid associated with the event

				String id = event.getUserId();

				if (id == null) {
					id = sessionManager.getCurrentSessionUserId();
				}

				if (id == null) {
					id = UNKNOWN_USER;
				}

				eventDelayHandler.createDelay(event, id, fireTime);
			} else {
				log.warn("Unable to create delayed event because delay handler is unset.  Firing now.");
				postEvent(event);
			}
		}
	}

	@Override
	public void cancelDelays(String resource) {

		if (eventDelayHandler != null) {
			eventDelayHandler.deleteDelay(resource);
		}
	}

	@Override
	public void cancelDelays(String resource, String event) {

		if (eventDelayHandler != null) {
			eventDelayHandler.deleteDelay(resource, event);
		}
	}

	/**
	 * Ensure that the provided event is an instance of BaseEvent.  If not, create a new BaseEvent
	 * and transfer state.
	 *
	 * @param e
	 * @return
	 */
	private BaseEvent ensureBaseEvent(Event e) {

		BaseEvent event = null;
		if (e instanceof BaseEvent) {
			event = (BaseEvent) e;
		} else {
			event = new BaseEvent(e.getEvent(), e.getResource(), e.getModify(), e.getPriority(),null);
			event.setSessionId(e.getSessionId());
			event.setUserId(e.getUserId());
		}
		return event;
	}

	/**
	 * Refired events can occur under a different user and session than was originally available.
	 * To make sure permission exceptions aren't falsely encountered, a security advisor should be
	 * pushed on the stack to recreate the correct environment for security checks.
	 *
	 * @param userId
	 */
	private SecurityAdvisor newResourceAdvisor(final String eventUserId) {

		// security advisor is needed if an event is refired.  the refired event is under the
		// auspices of the job scheduler user and needs to be advised by the original user.

		return (u, p, r) -> u.equals(eventUserId) ? SecurityAdvice.ALLOWED : SecurityAdvice.PASS;
	}

	@Override
	public void addObserver(Observer observer) {

		IgniteBiPredicate handler = (nodeId, message) -> {

			System.out.println("message: " + message);

			observer.update(null, message);
			return true;
		};

		messaging.localListen("EVENTS", handler);

		listenerMap.put(observer, handler);
	}

	@Override
	public void addPriorityObserver(Observer observer) {

		IgniteBiPredicate handler = (nodeId, message) -> {

			System.out.println("message: " + message);

			observer.update(null, message);
			return true;
		};

		messaging.localListen("EVENTS", handler);

		listenerMap.put(observer, handler);
	}

	@Override
	public void addLocalObserver(Observer observer) {

		IgniteBiPredicate handler = (nodeId, message) -> {

			System.out.println("nodeId: " + nodeId);
			System.out.println("message: " + message);

			observer.update(null, message);
			return true;
		};

		messaging.localListen("EVENTS", handler);

		listenerMap.put(observer, handler);
	}

	@Override
	public void deleteObserver(Observer observer) {

		IgniteBiPredicate handler = listenerMap.get(observer);
		messaging.stopLocalListen("EVENTS", handler);
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Event implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * <p>
	 * BaseEvent is the implementation of the Event interface.
	 * </p>
	 * <p>
	 * Event objects are posted to the EventTracking service, and may be listened for.
	 * </p>
	 */
	@Getter @Setter
	private class BaseEvent implements Event {
		/**
		 * Be a good Serializable citizen
		 */
		private static final long serialVersionUID = 3690761674282252600L;

		/** The Event's sequence number. */
		protected long seq = 0;

		/** The Event's id string. */
		protected String id = "";

		/** The Event's resource reference string. */
		protected String resource = "";

		/** The Event's context. May be null. */
		protected String context = null;
		
		/** The Event's session id string. May be null. */
		protected String sessionId = null;

		/** The Event's user id string. May be null. */
		protected String userId = null;

		/** The Event's modify flag (true if the event caused a resource modification). */
		protected boolean modify = false;

		/** The Event's notification priority. */
		protected int priority = NotificationService.NOTI_OPTIONAL;

		/** Event creation time. */
		protected Date time = null;

		/** Event LRS Statement */
		protected LRS_Statement lrsStatement = null;

        /** Do we store this event? */
		protected boolean isTransient = false;

		/**
		 * Construct
		 *
		 * @param event
		 *        The Event id.
		 * @param resource
		 *        The resource id.
		 * @param modify
		 *        If the event caused a modify, true, if it was just an access, false.
		 * @param priority
		 *        The Event's notification priority.
		 */
		public BaseEvent(String event, String resource, boolean modify, int priority, LRS_Statement lrsStatement) {

			setEvent(event);
			setResource(resource);
			this.lrsStatement = lrsStatement;
			this.modify = modify;
			this.priority = priority;

			// Find the context using the reference (let the service that it belongs to parse it)
			if (resource != null && !"".equals(resource)) {
				Reference ref = entityManager.newReference(resource);
				if (ref != null) {
					this.context = ref.getContext();
				}
			}

			// If we still need to find the context, try the tool placement
			if (this.context == null) {
				Placement placement = toolManager.getCurrentPlacement();
				if (placement != null) {
					this.context = placement.getContext();
				}
			}

			// KNL-997
			String uId = sessionManager.getCurrentSessionUserId();
			if (uId == null) {
				uId = UNKNOWN_USER;
			}
			setUserId(uId);
		}

		/**
		 * Construct
		 *
		 * @param event
		 *        The Event id.
		 * @param resource
		 *        The resource id.
		 * @param modify
		 *        If the event caused a modify, true, if it was just an access, false.
		 * @param context
		 *        The Event's context (may be null)
		 * @param priority
		 *        The Event's notification priority.
		 */
		public BaseEvent(String event, String resource, String context, boolean modify, int priority, LRS_Statement lrsStatement) {

			this(event, resource, modify, priority, lrsStatement);
			//Use the context parameter if it's not null, otherwise default to the detected context
			if (context != null) {
				this.context = context;
			}
		}
		
		/**
		 * Construct
		 *
		 * @param seq
		 *        The event sequence number.
		 * @param event
		 *        The Event id.
		 * @param resource
		 *        The resource id.
		 * @param modify
		 *        If the event caused a modify, true, if it was just an access, false.
		 * @param priority
		 *        The Event's notification priority.
		 */
		public BaseEvent(String event, String resource, String context, boolean modify, int priority) {

			this(event, resource, context, modify, priority, null);
		}

		/**
		 * Construct
		 *
		 * @param event
		 *        The Event id.
		 * @param resource
		 *        The resource id.
		 * @param modify
		 *        If the event caused a modify, true, if it was just an access, false.
		 * @param priority
		 *        The Event's notification priority.
		 * @param isTransient
		 *        If true, this event will never be written to storage. It will only exist in memory.
		 */
		public BaseEvent(String event, String resource, String context, boolean modify, int priority, boolean isTransient) {

			this(event, resource, context, modify, priority);
			this.isTransient = isTransient;
		}

		/**
		 * Construct
		 *
		 * @param seq
		 *        The event sequence number.
		 * @param event
		 *        The Event id.
		 * @param resource
		 *        The resource id.
		 * @param modify
		 *        If the event caused a modify, true, if it was just an access, false.
		 * @param priority
		 *        The Event's notification priority.
		 */
		public BaseEvent(long seq, String event, String resource, String context, boolean modify, int priority) {

			this(event, resource, context, modify, priority);
			this.seq = seq;
		}
		
		public BaseEvent(long seq, String event, String resource, String context, boolean modify, int priority, Date eventDate) {

			this(event, resource, context, modify, priority);
			this.seq = seq;
			this.time = eventDate;
		}

		@Override
		public String getEvent() {
			return id;
		}

		/**
		 * Set the event id.
		 *
		 * @param id
		 *        The event id string.
		 */
		private void setEvent(String id) {

			if (id != null) {
				this.id = id;
			} else {
				this.id = "";
			}
		}

		@Override
		public boolean getModify() {
			return modify;
		}

		/**
		 * Set the resource id.
		 *
		 * @param id
		 *        The resource id string.
		 */
		private void setResource(String id) {

			if (id != null) {
				this.resource = id;
			} else {
				this.resource = "";
			}
		}

		/**
		 * Set the session id.
		 *
		 * @param id
		 *        The session id string.
		 */
		private void setSessionId(String id) {

			if (id != null && id.length() > 0) {
				this.sessionId = id;
			} else {
				this.sessionId = null;
			}
		}

		/**
		 * Set the user id.
		 *
		 * @param id
		 *        The user id string.
		 */
		private void setUserId(String id) {

			if (id != null && id.length() > 0) {
				this.userId = id;
			} else {
				this.userId = null;
			}
		}

		/**
		 * @return A representation of this event's values as a string.
		 */
		public String toString() {
			return this.seq + ":" + getEvent() + "@" + getResource() + "[" + (getModify() ? "m" : "a") + ", " + getPriority() + "]";
		}

		@Override
		public Date getEventTime() {
			return new Date(this.time.getTime());
		}
	}
}
