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
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.Web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 */
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
	private UserDirectoryService userDirectoryService;

    private static ResourceLoader rloader = new ResourceLoader("sui-notifications");

    @GetMapping(value = { "/", "/index.html" })
    public ModelAndView index(Model model, HttpServletRequest req) {

        String userId = sessionManager.getCurrentSessionUserId();

        if (StringUtils.isBlank(userId)) {
            Session session = sessionManager.getCurrentSession();
            session.setAttribute(Tool.HELPER_DONE_URL, Web.returnUrl(req, ""));
            return new ModelAndView("redirect:" + serverConfigurationService.getServerUrl() + "/portal/xlogin", "balls", model);
        }

        String locale = preferencesService.getLocale(userId).toString();

        boolean notificationsPushEnabled
            = serverConfigurationService.getBoolean("portal.notifications.push.enabled", false);

        boolean debugNotifications
            = serverConfigurationService.getBoolean("portal.notifications.debug", false);

        model.addAttribute("pushEnabled", notificationsPushEnabled);
        model.addAttribute("debugNotifications", debugNotifications);
        model.addAttribute("userId", userId);
        model.addAttribute("locale", preferencesService.getLocale(userId).toString());
        model.addAttribute("cdnQuery", PortalUtils.getCDNQuery());

        return new ModelAndView("index", "balls", model);
    }
}
