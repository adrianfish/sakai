/******************************************************************************
 * Copyright 2015 sakaiproject.org Licensed under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.sakaiproject.webapi.controllers;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.messaging.api.UserMessagingService;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesEdit;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.UserDirectoryService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Flux;

@Slf4j
@RestController
public class EventsController extends AbstractSakaiApiController {

	@Resource
	private EntityManager entityManager;

	@Resource
	private PreferencesService preferencesService;

	@Resource
	private SecurityService securityService;

	@Resource(name = "org.sakaiproject.component.api.ServerConfigurationService")
	private ServerConfigurationService serverConfigurationService;

	@Resource
	private SiteService siteService;

	@Resource
	private UserDirectoryService userDirectoryService;

    @Resource
    private UserMessagingService userMessagingService;

    @GetMapping("/keys/sakaipush")
    public ResponseEntity<String> getPushKey() {

        String home = serverConfigurationService.getSakaiHomePath();
        String fileName = serverConfigurationService.getString(userMessagingService.PUSH_PUBKEY_PROPERTY, "sakai_push.key.pub");

        try {
            return ResponseEntity.ok(String.join("", Files.readAllLines(Paths.get(home, fileName))));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/users/me/prefs/pushEndpoint")
    public ResponseEntity setPushEndpoint(@RequestParam String endpoint, @RequestParam(required = false) String auth, @RequestParam(required = false) String userKey) {

		String currentUserId = checkSakaiSession().getUserId();

        log.debug("ENDPOINT: {}", endpoint);
        log.debug("AUTH: {}", auth);
        log.debug("KEY: {}", userKey);

        try {
            PreferencesEdit prefs = preferencesService.edit(currentUserId);
            ResourcePropertiesEdit props = prefs.getPropertiesEdit("sakai:notifications");
            props.addProperty("push-endpoint", endpoint);
            if (userKey != null) {
                props.addProperty("push-user-key", userKey);
            }
            if (auth != null) {
                props.addProperty("push-auth", auth);
            }
            preferencesService.commit(prefs);
        } catch (Exception e) {
            log.error("Failed to add push-endpoint to user {}'s preferences: {}", currentUserId, e.toString());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}/events")
    public ResponseEntity<Flux<ServerSentEvent<String>>> streamEvents() {

        Session session = checkSakaiSession();

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

        int pingSeconds = serverConfigurationService.getInt("sse.ping.interval.seconds", 59);
        Flux<ServerSentEvent<String>> ping = Flux.interval(Duration.ofSeconds(pingSeconds))
            .map(i -> ServerSentEvent.<String>builder().event("ping").build())
            .doFinally(signalType -> log.debug("Ping flux ended with signal {}", signalType));

        return ResponseEntity.ok().header("X-Accel-Buffering", "no")
                .body(Flux.merge(ping, Flux.<ServerSentEvent<String>>create(emitter -> {

                    userMessagingService.listen("USER#" + session.getUserId(), message -> {

                        String event = "notifications";

                        try {
                            emitter.next(ServerSentEvent.<String>builder()
                            .event(event)
                            .data(mapper.writeValueAsString(message))
                            .build());
                        } catch (Exception e) {
                            log.error("Failed to emit SSE event", e);
                        }
                    });

                    userMessagingService.listen("GENERAL", message -> {

                        try {
                            emitter.next(ServerSentEvent.<String> builder()
                            .data((new ObjectMapper()).writeValueAsString(message))
                            .build());
                        } catch (Exception e) {
                            log.error("Failed to emit SSE event", e);
                        }
                    });
                })));
    }
}
