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
package org.sakaiproject.analytics.impl.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.sakaiproject.analytics.api.AnalyticsConstants;
import org.sakaiproject.analytics.api.AnalyticsService;
import org.sakaiproject.analytics.api.GradeDistribution;
import org.sakaiproject.analytics.api.ReportParams;
import org.sakaiproject.analytics.api.model.AnalyticsGrade;
import org.sakaiproject.analytics.impl.AnalyticsServiceImpl;
import org.sakaiproject.analytics.api.repository.AnalyticsGradeRepository;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.service.gradebook.shared.GradingReferenceReckoner;
import org.sakaiproject.service.gradebook.shared.GradeDefinition;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.BaseResourceProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.AopTestUtils;

import static org.mockito.Mockito.*;

import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AnalyticsTestConfiguration.class})
public class AnalyticsServiceTests extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired private AnalyticsGradeRepository analyticsGradeRepository;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private AuthzGroupService authzGroupService;
    @Autowired private EntityManager entityManager;
    @Autowired private GradebookService gradebookService;
    @Autowired private SecurityService securityService;
    @Autowired private SessionManager sessionManager;
    @Autowired private SiteService siteService;
    @Autowired private UserDirectoryService userDirectoryService;

    private String siteId = "math101";
    private String user1Id = "user_1";
    private String user1DisplayName = "User 1";
    private User user1 = mock(User.class);
    private String user2Id = "user_2";
    private String user2DisplayName = "User 2";
    private User user2 = mock(User.class);

    private Long itemId = 342L;
    private Double grade = 56D;
    private Double points = 88D;
    private String termEid = "lent_2023";
    private String toolId = "sakai.tool";
    private String site1 = "site1";
    private String site2 = "site2";
    private Set<String> siteIds = new HashSet<>();
    private Set<String> siteRefs = new HashSet<>();

    @Before
    public void setup() {

        reset(sessionManager);
        reset(securityService);
        reset(userDirectoryService);

        when(user1.getId()).thenReturn(user1Id);
        when(user1.getDisplayName()).thenReturn(user1DisplayName);
        try {
          when(userDirectoryService.getUser(user1Id)).thenReturn(user1);
        } catch (UserNotDefinedException unde) {
        }

        when(user2.getId()).thenReturn(user2Id);
        when(user2.getDisplayName()).thenReturn(user2DisplayName);
        try {
          when(userDirectoryService.getUser(user2Id)).thenReturn(user2);
        } catch (UserNotDefinedException unde) {
        }

        siteIds.clear();
        siteIds.add(site1);
        siteIds.add(site2);

        siteRefs.clear();
        siteRefs.add("/site/" + site1);
        siteRefs.add("/site/" + site2);

        when(siteService.siteReference(site1)).thenReturn("/site/" + site1);
        when(siteService.siteReference(site1)).thenReturn("/site/" + site1);

        AuthzGroup authzGroup1 = mock(AuthzGroup.class);
        when(authzGroup1.getUsers()).thenReturn(new HashSet(Collections.singletonList(user1Id)));
        try {
            when(authzGroupService.getAuthzGroup("/site/" + site1)).thenReturn(authzGroup1);
        } catch (Exception e) {
        }

        AuthzGroup authzGroup2 = mock(AuthzGroup.class);
        when(authzGroup2.getUsers()).thenReturn(new HashSet(Collections.singletonList(user2Id)));
        try {
            when(authzGroupService.getAuthzGroup("/site/" + site2)).thenReturn(authzGroup2);
        } catch (Exception e) {
        }

    }

    @Test
    public void handleGradebookEvent() {

        Event event = createEvent();

        BaseResourceProperties props = new BaseResourceProperties();
        props.addProperty(Site.PROP_SITE_TERM_EID, termEid);
        Site site = mock(Site.class);
        when(site.getProperties()).thenReturn(props);

        GradeDefinition def = new GradeDefinition();
        def.setGrade(grade.toString());

        org.sakaiproject.service.gradebook.shared.Assignment ass
            = new org.sakaiproject.service.gradebook.shared.Assignment();
        ass.setPoints(points);

        when(gradebookService.getGradeDefinitionForStudentForItem(siteId, itemId, user1Id)).thenReturn(def);
        when(gradebookService.getAssignment(siteId, itemId)).thenReturn(ass);
        try {
            when(siteService.getSite(siteId)).thenReturn(site);
        } catch (Exception e) {
        }

        when(siteService.siteReference(siteId)).thenReturn("/site/" + siteId);

        ((AnalyticsServiceImpl) AopTestUtils.getTargetObject(analyticsService)).update(null, event);

        List<AnalyticsGrade> grades = analyticsGradeRepository.findAll();
        assertEquals(1, grades.size());
        assertEquals(grade, grades.get(0).getPoints());
        assertEquals(itemId.toString(), grades.get(0).getItemId());
        assertEquals(user1Id, grades.get(0).getStudentId());
        assertEquals(termEid, grades.get(0).getTermId());

        String ref = GradingReferenceReckoner.reckoner().item(itemId).student(user1Id).reckon().getReference();
        when(event.getResource()).thenReturn(ref);

        ((AnalyticsServiceImpl) AopTestUtils.getTargetObject(analyticsService)).update(null, event);

        grades = analyticsGradeRepository.findAll();
        assertEquals(1, grades.size());

        ref = GradingReferenceReckoner.reckoner().item(itemId).student(user2Id).tool(toolId).reckon().getReference();
        when(event.getResource()).thenReturn(ref);

        when(gradebookService.getGradeDefinitionForStudentForItem(siteId, itemId, user2Id)).thenReturn(def);

        ((AnalyticsServiceImpl) AopTestUtils.getTargetObject(analyticsService)).update(null, event);

        grades = analyticsGradeRepository.findAll();
        assertEquals(2, grades.size());
    }

    @Test
    public void getGradeDistribution() {

        ReportParams params = new ReportParams();
        params.setSiteRefs(siteIds);

        when(sessionManager.getCurrentSessionUserId()).thenReturn("user1");

        when(authzGroupService.getAuthzGroupsIsAllowed("user1", AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, siteIds))
            .thenReturn(siteIds);

        AnalyticsGrade grade1 = new AnalyticsGrade();
        grade1.setItemId("grade1item");
        grade1.setSiteRef(site1);
        grade1.setStudentId(user1Id);
        grade1.setToolId(toolId);
        grade1.setPoints(10D);
        grade1.setMaxPoints(10D);
        grade1.setPercentage(33.33D);
        analyticsGradeRepository.save(grade1);

        AnalyticsGrade grade2 = new AnalyticsGrade();
        grade2.setItemId("grade2item");
        grade2.setSiteRef(site1);
        grade2.setStudentId(user1Id);
        grade2.setToolId(toolId);
        grade2.setPoints(20D);
        grade2.setMaxPoints(10D);
        grade2.setPercentage(44.155D);
        analyticsGradeRepository.save(grade2);

        AnalyticsGrade grade3 = new AnalyticsGrade();
        grade3.setItemId("grade3item");
        grade3.setSiteRef(site2);
        grade3.setStudentId(user1Id);
        grade3.setToolId(toolId);
        grade3.setPoints(30D);
        grade3.setMaxPoints(10D);
        grade3.setPercentage((grade3.getPoints() / grade3.getMaxPoints()) * 100D);
        grade3.setPercentage(58.441D);
        analyticsGradeRepository.save(grade3);

        AnalyticsGrade grade4 = new AnalyticsGrade();
        grade4.setItemId("grade4item");
        grade4.setSiteRef(site2);
        grade4.setStudentId(user1Id);
        grade4.setToolId(toolId);
        grade4.setPoints(40D);
        grade4.setMaxPoints(10D);
        grade4.setPercentage(83.33D);
        analyticsGradeRepository.save(grade4);

        AnalyticsGrade grade5 = new AnalyticsGrade();
        grade5.setItemId("grade5item");
        grade5.setSiteRef(site2);
        grade5.setStudentId(user1Id);
        grade5.setToolId(toolId);
        grade5.setPoints(50D);
        grade5.setMaxPoints(10D);
        grade5.setPercentage(100D);
        analyticsGradeRepository.save(grade5);

        AnalyticsGrade grade6 = new AnalyticsGrade();
        grade6.setItemId("grade6item");
        grade6.setSiteRef(site2);
        grade6.setStudentId(user1Id);
        grade6.setToolId(toolId);
        grade6.setPoints(50D);
        grade6.setMaxPoints(10D);
        grade6.setPercentage(100D);
        analyticsGradeRepository.save(grade6);

        Map<String, Object> data = analyticsService.getGradeDistribution(params);
        Map<Integer, Long> dist = (Map<Integer, Long>) data.get("dist");

        assertEquals("33.33", (String) data.get("mean"));
        assertEquals("40.00", (String) data.get("median"));
        assertEquals("14.91", (String) data.get("standardDeviation"));
        assertEquals("50.00", (String) data.get("highest"));
        assertEquals("10.00", (String) data.get("lowest"));

        assertNotNull(dist);
        assertEquals(10, dist.size());

        assertEquals(new Long(1), dist.get(3));
        assertEquals(new Long(1), dist.get(4));
        assertEquals(new Long(1), dist.get(5));
        assertEquals(new Long(1), dist.get(8));
        assertEquals(new Long(2), dist.get(9));
    }

    @Test
    public void getUsersForGradeReport() {

        AnalyticsGrade grade1 = new AnalyticsGrade();
        grade1.setItemId("grade1item");
        grade1.setSiteRef("/site/" + site1);
        grade1.setStudentId(user1Id);
        grade1.setToolId(toolId);
        grade1.setPoints(10D);
        grade1.setMaxPoints(10D);
        grade1.setPercentage(33.33D);
        analyticsGradeRepository.save(grade1);

        AnalyticsGrade grade2 = new AnalyticsGrade();
        grade2.setItemId("grade2item");
        grade2.setSiteRef("/site/" + site2);
        grade2.setStudentId(user2Id);
        grade2.setToolId(toolId);
        grade2.setPoints(20D);
        grade2.setMaxPoints(10D);
        grade2.setPercentage(44.155D);
        analyticsGradeRepository.save(grade2);

      String instructor = "instructor";

      when(sessionManager.getCurrentSessionUserId()).thenReturn(instructor);

      when(authzGroupService.getAuthzGroupsIsAllowed(instructor, AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, null))
          .thenReturn(siteRefs);

      Set<User> users = analyticsService.getUsersForGradeReport(null);
      assertEquals(2, users.size());

      users = analyticsService.getUsersForGradeReport("/site/" + site1);
      assertEquals(0, users.size());

      when(securityService.unlock(AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, "/site/" + site1)).thenReturn(true);

      users = analyticsService.getUsersForGradeReport("/site/" + site1);
      assertEquals(1, users.size());
    }

    public void getTermsForGradeReport() {

        AnalyticsGrade grade1 = new AnalyticsGrade();
        grade1.setItemId("grade1item");
        grade1.setSiteRef("/site/" + site1);
        grade1.setStudentId(user1Id);
        grade1.setToolId(toolId);
        grade1.setPoints(10D);
        grade1.setTermId("term1");
        grade1.setMaxPoints(10D);
        grade1.setPercentage(33.33D);
        analyticsGradeRepository.save(grade1);

        AnalyticsGrade grade2 = new AnalyticsGrade();
        grade2.setItemId("grade2item");
        grade2.setSiteRef("/site/" + site2);
        grade2.setStudentId(user2Id);
        grade2.setToolId(toolId);
        grade2.setTermId("term2");
        grade2.setPoints(20D);
        grade2.setMaxPoints(10D);
        grade2.setPercentage(44.155D);
        analyticsGradeRepository.save(grade2);

      String instructor = "instructor";

      when(sessionManager.getCurrentSessionUserId()).thenReturn(instructor);

      when(authzGroupService.getAuthzGroupsIsAllowed(instructor, AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, null))
          .thenReturn(siteRefs);

      Set<String> terms = analyticsService.getTermsForGradeReport(null);
      assertEquals(2, terms.size());

      terms = analyticsService.getTermsForGradeReport("/site/" + site1);
      assertEquals(0, terms.size());

      when(securityService.unlock(AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, "/site/" + site1)).thenReturn(true);

      terms = analyticsService.getTermsForGradeReport("/site/" + site1);
      assertEquals(1, terms.size());
    }

    private Event createEvent() {

        Event event = mock(Event.class);
        when(event.getContext()).thenReturn(siteId);
        when(event.getEvent()).thenReturn(GradebookService.EVENT_GRADED);
        String ref = GradingReferenceReckoner.reckoner().item(itemId).student(user1Id).reckon().getReference();
        when(event.getResource()).thenReturn(ref);
        return event;
    }
}

