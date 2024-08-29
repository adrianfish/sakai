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

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.user.api.UserNotDefinedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class FrontendLoggerController extends AbstractSakaiApiController {

    @Autowired
    private ServerConfigurationService serverConfigurationService;

    private String recipientUserId;

    @PostConstruct
    public void init() {

        recipientUserId = serverConfigurationService.getString("frontend-logger.recipientUserId");
        if (StringUtils.isBlank(recipientUserId)) {
            log.error("frontend-logger.recipientUserId is not configured in sakai properties");
        }
    }

    @PostMapping(value = "/frontend-logger")
    public void log(@RequestBody String stack) throws UserNotDefinedException {

        Session session = checkSakaiSession();

        Set.of(userDirectoryService.getUser(recipientUserId));
        userMessagingService.message(Set.of(userDirectoryService.getUser(recipientUserId)),
            Message.builder().tool("sakai.webapi").type("frontend_error").build(),
            List.of(MessageMedium.EMAIL),
            Map.of(


        System.out.println(recipientUserId);
        User user = userDirectoryService.getUser(recipientUserId);

        System.out.println(session.getUserEid());
        System.out.println(session.getId());
        System.out.println(stack);
    }
}
