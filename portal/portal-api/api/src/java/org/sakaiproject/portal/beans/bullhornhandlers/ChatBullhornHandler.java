/**
 * Copyright (c) 2003-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.portal.beans.bullhornhandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.sakaiproject.chat2.model.ChatChannel;
import org.sakaiproject.chat2.model.ChatFunctions;
import org.sakaiproject.chat2.model.ChatManager;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.portal.api.BullhornData;
import org.sakaiproject.portal.beans.BullhornAlert;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ChatBullhornHandler extends AbstractBullhornHandler {

    @Inject
    private ChatManager chatManager;

    @Inject
    private EntityManager entityManager;

    @Inject
    private ServerConfigurationService serverConfigurationService;

    @Resource(name = "org.sakaiproject.springframework.orm.hibernate.GlobalSessionFactory")
    private SessionFactory sessionFactory;

    @Inject
    private SiteService siteService;

    @Resource(name = "org.sakaiproject.springframework.orm.hibernate.GlobalTransactionManager")
    private PlatformTransactionManager transactionManager;

    @Override
    public List<String> getHandledEvents() {
        return Arrays.asList(ChatFunctions.CHAT_FUNCTION_NEW);
    }

    @Override
    public Optional<List<BullhornData>> handleEvent(Event e, Cache<String, Map> countCache) {

        String from = e.getUserId();

        String ref = e.getResource();
        System.out.println(ref);
        String[] pathParts = ref.split("/");

        String siteId = pathParts[3];
        String channelId = pathParts[4];
        String messagId = pathParts[5];

        Site site = null;
        try {
            site = siteService.getSite(siteId);
        } catch (IdUnusedException idue) {
            log.error("No site for site id: {}", siteId);
            return Optional.empty();
        }

        ChatChannel channel = chatManager.getChatChannel(channelId);

        String url = channel.getUrl();
        String title = channel.getTitle();

        List<BullhornData> bhEvents = new ArrayList<>();

        site.getUsers().stream().filter(uid -> !from.equals(uid)).forEach(to -> {

            bhEvents.add(new BullhornData(from, to, siteId, title, url, false));
            countCache.remove(to);
        });

        return Optional.of(bhEvents);
    }
}
