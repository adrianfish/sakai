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
package org.sakaiproject.pwa.controllers;

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.portal.util.PortalUtils;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;


import org.sakaiproject.event.cover.UsageSessionService;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class PwaController {

	@Resource
	private PreferencesService preferencesService;

	@Resource
	private SecurityService securityService;

	@Resource(name = "org.sakaiproject.component.api.ServerConfigurationService")
	private ServerConfigurationService serverConfigurationService;

	@Resource(name = "org.sakaiproject.tool.api.SessionManager")
	private SessionManager sessionManager;

	@Resource
	private SiteService siteService;

	@Resource
	private TimeService timeService;

	@Resource
	private UserDirectoryService userDirectoryService;

    @GetMapping(value = { "/", "/index.html" })
    public ModelAndView index(Model model, HttpServletRequest req) {

        String userId = sessionManager.getCurrentSessionUserId();

        boolean notificationsPushEnabled
            = serverConfigurationService.getBoolean("portal.notifications.push.enabled", false);

        model.addAttribute("pushEnabled", notificationsPushEnabled);

        if (StringUtils.isNotBlank(userId)) {
            model.addAttribute("userId", userId);

            try {
                User user = userDirectoryService.getUser(userId);
                model.addAttribute("userName", user.getEid());
                model.addAttribute("userDisplayName", user.getDisplayName());
                TimeZone userTz = timeService.getLocalTimeZone();
                model.addAttribute("userTimezone", userTz.getID());
            } catch (UserNotDefinedException unde) {
                log.error("No user found for id {}", userId);
            }
        }

        model.addAttribute("cdnQuery", PortalUtils.getCDNQuery());

        if (StringUtils.isNotBlank(userId)) {
            model.addAttribute("locale", preferencesService.getLocale(userId).toString());
        } else {
            model.addAttribute("locale", Locale.getDefault().toString());
        }

        return new ModelAndView("index", "balls", model);
    }

    @GetMapping(value = "/logout")
    public ModelAndView logout(Model model, HttpServletRequest req) {

        UsageSessionService.logout();

        return index(model, req);
    }
}
