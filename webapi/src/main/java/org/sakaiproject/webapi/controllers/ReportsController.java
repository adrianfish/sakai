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

import java.util.HashMap;
import java.util.Map;

import org.sakaiproject.analytics.api.AnalyticsService;
import org.sakaiproject.analytics.api.ReportParams;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class ReportsController extends AbstractSakaiApiController {

	@Autowired private AnalyticsService analyticsService;
	@Autowired private ServerConfigurationService serverConfigurationService;
	@Autowired private SiteService siteService;

	@PostMapping(value = "/reports/grades/distribution", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getGradeDistribution(@RequestBody ReportParams params) {

		Session session = checkSakaiSession();

        return ResponseEntity.ok(analyticsService.getGradeDistribution(params));
	}
}
