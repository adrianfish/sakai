/*
 * Copyright (c) 2003-2021 The Apereo Foundation
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
package org.sakaiproject.analytics.tool;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.sakaiproject.analytics.api.AnalyticsConstants;
import org.sakaiproject.analytics.api.AnalyticsService;
import org.sakaiproject.analytics.tool.exception.MissingSessionException;
import org.sakaiproject.portal.util.PortalUtils;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class AnalyticsController {

	@Resource
	private AnalyticsService analyticsService;

	@Resource
	private SessionManager sessionManager;

	@Resource
	private SiteService siteService;

	@Resource
	private ToolManager toolManager;

  private ObjectMapper objectMapper = new ObjectMapper();

	@GetMapping(value = "/gradedistribution")
	public String gradeDistribution(Model model, HttpServletRequest request) {

        String currentUserId = checkSakaiSession().getUserId();

        loadModel(model, request);

        // If we are on a user site, supply the sites data.
        String siteId = toolManager.getCurrentPlacement().getContext();

        boolean userSite = siteService.isUserSite(siteId);
        if (siteService.isUserSite(siteId)) {

            final List<Map<String, String>> sites = siteService.getUserSites().stream()
                    .filter(s -> s.isAllowed(currentUserId, AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW))
                    .map(s -> {

                Map<String, String> site = new HashMap<>();
                site.put("ref", siteService.siteReference(s.getId()));
                site.put("title", s.getTitle());
                return site;
            }).collect(Collectors.toList());

            try {
              model.addAttribute("sitesJSON", objectMapper.writeValueAsString(sites));
            } catch (Exception e) {
              log.warn("Exception thrown while serialising sites to json: {}", e.toString());
            }

            try {
              model.addAttribute("termsJSON", objectMapper.writeValueAsString(
                                      analyticsService.getTermsForGradeReport(userSite ? null
                                                              : siteService.siteReference(siteId))));
            } catch (Exception e) {
              log.warn("Exception thrown while serialising terms to json: {}", e.toString());
            }

            try {
              model.addAttribute("departmentsJSON", objectMapper.writeValueAsString(
                                      analyticsService.getDepartmentsForGradeReport(userSite ? null
                                                              : siteService.siteReference(siteId))));
            } catch (Exception e) {
              log.warn("Exception thrown while serialising departments to json: {}", e.toString());
            }

            try {
              model.addAttribute("subjectsJSON", objectMapper.writeValueAsString(
                                      analyticsService.getSubjectsForGradeReport(userSite ? null
                                                              : siteService.siteReference(siteId))));
            } catch (Exception e) {
              log.warn("Exception thrown while serialising subjects to json: {}", e.toString());
            }


        } else {
            // Not on user site. We don't want to allow site selection
            model.addAttribute("siteRef", siteService.siteReference(siteId));
        }

        // Now add all the users this user can query
        final List<Map<String, String>> users = analyticsService.getUsersForGradeReport(userSite ? null : siteService.siteReference(siteId)).stream()
            .map(user -> {
                Map<String, String> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("displayName", user.getDisplayName());
                return map;
            }).collect(Collectors.toList());
        try {
          model.addAttribute("usersJSON", objectMapper.writeValueAsString(users));
        } catch (Exception e) {
          log.warn("Exception thrown while serialising users to json: {}", e.toString());
        }
        
        return "gradedistribution";
	}

  @GetMapping(value = {"/", "/index"})
	public String index(Model model, HttpServletRequest request) {

        checkSakaiSession();

        loadModel(model, request);

        return "reports";
	}


	@GetMapping(value = "/competencies")
	public String pageCompetencies(Model model, HttpServletRequest request) {

        checkSakaiSession();

        loadModel(model, request);
        return "competencies";
	}

    private void loadModel(Model model, HttpServletRequest request) {

        model.addAttribute("cdnQuery", PortalUtils.getCDNQuery());

        Placement placement = toolManager.getCurrentPlacement();
        model.addAttribute("siteId", placement.getContext());
        model.addAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));
    }

    /**
     * Check for a valid session
     * if not valid a 403 Forbidden will be returned
     */
	private Session checkSakaiSession() {

	    try {
            Session session = sessionManager.getCurrentSession();
            if (StringUtils.isBlank(session.getUserId())) {
                log.error("Sakai user session is invalid");
                throw new MissingSessionException();
            }
            return session;
        } catch (IllegalStateException e) {
	        log.error("Could not retrieve the sakai session");
            throw new MissingSessionException(e.getCause());
        }
    }
}
