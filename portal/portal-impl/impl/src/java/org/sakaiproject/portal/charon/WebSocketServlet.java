/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006, 2007, 2008 The Sakai Foundation
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
package org.sakaiproject.portal.charon;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.portal.api.PushRegistry;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketServlet extends HttpServlet {

	private static final long serialVersionUID = 2645929710236293079L;

	public void init(ServletConfig config) throws ServletException {

        PushRegistry pushRegistry = (PushRegistry) ComponentManager.get("pushRegistry");

        ServerContainer serverContainer
            = (ServerContainer) config.getServletContext().getAttribute("javax.websocket.server.ServerContainer");

        try {
            for (ServerEndpointConfig endpoint : pushRegistry.getEndpoints()) {
                log.debug("Setting up endpoint: " + endpoint.getPath());
                serverContainer.addEndpoint(endpoint);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
	}
}
