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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.sakaiproject.webapi.beans.UserRestBean;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.memory.api.SimpleConfiguration;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
@RestController
public class MentionsController extends AbstractSakaiApiController {

    @Autowired private MemoryService memoryService;
    @Autowired private SiteService siteService;
    @Autowired private UserDirectoryService userDirectoryService;

    private Cache<String, List<User>> siteUsersCache;

    private static final String SITE_CACHE = "org.sakaiproject.webapi.mentionsSiteUsersCache";

    @PostConstruct
    public void init() {

        siteUsersCache = memoryService.createCache(SITE_CACHE, new SimpleConfiguration(0));
    }

    @GetMapping(value = "/sites/{siteId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserRestBean>> getUsersForSite(@PathVariable String siteId, @RequestParam String name) {

        checkSakaiSession();

        List<User> siteUsers = siteUsersCache.get(siteId);

        if (siteUsers == null) {

            Site site = null;
            try {
                site = siteService.getSite(siteId);
                siteUsers = userDirectoryService.getUsers(site.getUsers());
                siteUsersCache.put(siteId, siteUsers);
            } catch (IdUnusedException idue) {
                return ResponseEntity.badRequest().build();
            }
        }

        String lcName = name.toLowerCase();

        return ResponseEntity.ok(siteUsers
                .stream()
                .map(UserRestBean::of)
                .filter(b -> b.displayName.toLowerCase().contains(lcName) || b.eid.toLowerCase().contains(lcName))
                .collect(Collectors.toList()));
    }
}
