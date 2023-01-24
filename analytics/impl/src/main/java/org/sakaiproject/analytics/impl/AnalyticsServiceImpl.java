package org.sakaiproject.analytics.impl;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sakaiproject.analytics.api.AnalyticsConstants;
import org.sakaiproject.analytics.api.AnalyticsService;
import org.sakaiproject.analytics.api.ReportParams;
import org.sakaiproject.analytics.api.model.AnalyticsGrade;
import org.sakaiproject.analytics.api.repository.AnalyticsGradeRepository;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.service.gradebook.shared.GradeDefinition;
import org.sakaiproject.service.gradebook.shared.GradingReferenceReckoner;
import org.sakaiproject.service.gradebook.shared.GradingReferenceReckoner.GradingReference;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService, Observer {

  @Autowired
  private AnalyticsGradeRepository analyticsGradeRepository;

  @Autowired
  private AuthzGroupService authzGroupService;

  @Autowired
  private EventTrackingService eventTrackingService;

  @Autowired
  private FunctionManager functionManager;

  @Autowired
  @Qualifier("org_sakaiproject_service_gradebook_GradebookService")
  private GradebookService gradebookService;

  @Autowired
  private SecurityService securityService;

  @Autowired
  private SessionManager sessionManager;

  @Autowired
  private SiteService siteService;

  @Autowired
  private UserDirectoryService userDirectoryService;

  private static final DecimalFormat df = new DecimalFormat("0.00");

  public void init() {

    eventTrackingService.addLocalObserver(this);
    functionManager.registerFunction(AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, false);
  }

  public Map<String, Object> getGradeDistribution(ReportParams params) {

    Map<Integer, Double> boundaries = new HashMap<>();
    Map<Integer, Long> dist = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      dist.put(i, 0L);
      boundaries.put(i, Double.valueOf(i) * 10D);
    }
    boundaries.put(10, 100.1D);

    String currentUserId = sessionManager.getCurrentSessionUserId();

    if (params.getSiteRefs().size() > 0) {
      params.setSiteRefs(authzGroupService.getAuthzGroupsIsAllowed(currentUserId
          , AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW
          , params.getSiteRefs()));
    }

    Double total = 0D;

    List<AnalyticsGrade> grades = analyticsGradeRepository.findByParams(params);
    Collections.sort(grades, (g1, g2) -> Double.compare(g1.getPoints(), g2.getPoints()));
    for (AnalyticsGrade grade : grades) {

      total += grade.getPoints();

      Double percent = grade.getPercentage();
      for (int i = 1; i <= 10; i++) {
        Double boundary = boundaries.get(i - 1);
        Double upper = boundary + (i < 10 ? 10D : 10.1D);
        if (percent >= boundary && percent < upper) {
          dist.put(i - 1, dist.get(i - 1) + 1);
          break;
        }
      }
    }

    Map<String, Object> data = new HashMap<>();
    data.put("dist", dist);
    data.put("total", grades.size());

    if (!grades.isEmpty()) {
      double mean = total / grades.size();

      double variance = grades.stream().reduce(0D, (acc, g) -> acc + Math.pow(g.getPoints() - mean, 2), Double::sum) / grades.size();

      data.put("mean", df.format(mean));
      data.put("median", df.format(grades.get(((grades.size() % 2 == 0) ? grades.size() + 1 : grades.size()) / 2).getPoints()));
      data.put("standardDeviation", df.format(Math.sqrt(variance)));
      data.put("lowest", df.format(grades.get(0).getPoints()));
      data.put("highest", df.format(grades.get(grades.size() - 1).getPoints()));
    }

    return data;
  }

  public Set<User> getUsersForGradeReport(String siteRef) {

    String currentUserId = sessionManager.getCurrentSessionUserId();

    Function<AnalyticsGrade, User> getUser = grade -> {

        try {
          return userDirectoryService.getUser(grade.getStudentId());
        } catch (UserNotDefinedException unde) {
          log.warn("No user for user id {}. This user will be omitted : {}", grade.getStudentId(), unde.toString());
        }
        return null;
    };

    Set<String> siteRefs = new HashSet<>();
    if (StringUtils.isBlank(siteRef)) {
      siteRefs = authzGroupService.getAuthzGroupsIsAllowed(currentUserId, AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, null);
      return analyticsGradeRepository.findBySiteRefIn(siteRefs).stream().map(getUser).collect(Collectors.toSet());
    } else {
      if (securityService.unlock(AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, siteRef)) {
        siteRefs = new HashSet<>(Collections.singletonList(siteRef));
        return analyticsGradeRepository.findBySiteRefIn(siteRefs).stream().map(getUser).collect(Collectors.toSet());
      } else {
          log.warn("The current user doesn't have {} for {}. An empty list will be returned.", AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, siteRef);
          return Collections.<User>emptySet();
      }
    }
  }

  public Set<String> getTermsForGradeReport(String siteRef) {

    String currentUserId = sessionManager.getCurrentSessionUserId();

    if (StringUtils.isBlank(siteRef)) {
      Set<String> siteRefs = authzGroupService.getAuthzGroupsIsAllowed(currentUserId, AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, null);
      return analyticsGradeRepository.findBySiteRefIn(siteRefs).stream().map(AnalyticsGrade::getTermId).collect(Collectors.toSet());
    } else {
      if (securityService.unlock(AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, siteRef)) {
        Set<String> siteRefs = new HashSet<>(Collections.singletonList(siteRef));
        return analyticsGradeRepository.findBySiteRefIn(siteRefs).stream().map(AnalyticsGrade::getTermId).filter(Objects::nonNull).collect(Collectors.toSet());
      } else {
          log.warn("The current user doesn't have {} for {}. An empty list will be returned.", AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, siteRef);
          return Collections.<String>emptySet();
      }
    }
  }

  public Set<String> getDepartmentsForGradeReport(String siteRef) {

    String currentUserId = sessionManager.getCurrentSessionUserId();

    if (StringUtils.isBlank(siteRef)) {
      Set<String> siteRefs = authzGroupService.getAuthzGroupsIsAllowed(currentUserId, AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, null);
      return analyticsGradeRepository.findBySiteRefIn(siteRefs).stream().map(AnalyticsGrade::getDepartment).filter(Objects::nonNull).collect(Collectors.toSet());
    } else {
      if (securityService.unlock(AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, siteRef)) {
        Set<String> siteRefs = new HashSet<>(Collections.singletonList(siteRef));
        return analyticsGradeRepository.findBySiteRefIn(siteRefs).stream().map(AnalyticsGrade::getDepartment).collect(Collectors.toSet());
      } else {
          log.warn("The current user doesn't have {} for {}. An empty list will be returned.", AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, siteRef);
          return Collections.<String>emptySet();
      }
    }
  }

 public Set<String> getSubjectsForGradeReport(String siteRef) {

    String currentUserId = sessionManager.getCurrentSessionUserId();

    if (StringUtils.isBlank(siteRef)) {
      Set<String> siteRefs = authzGroupService.getAuthzGroupsIsAllowed(currentUserId, AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, null);
      return analyticsGradeRepository.findBySiteRefIn(siteRefs).stream().map(AnalyticsGrade::getSubject).filter(Objects::nonNull).collect(Collectors.toSet());
    } else {
      if (securityService.unlock(AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, siteRef)) {
        Set<String> siteRefs = new HashSet<>(Collections.singletonList(siteRef));
        return analyticsGradeRepository.findBySiteRefIn(siteRefs).stream().map(AnalyticsGrade::getSubject).collect(Collectors.toSet());
      } else {
          log.warn("The current user doesn't have {} for {}. An empty list will be returned.", AnalyticsConstants.PERM_ANALYTICS_GRADE_VIEW, siteRef);
          return Collections.<String>emptySet();
      }
    }
  }

  public void update(Observable o, Object arg) {

    if (!(arg instanceof Event)) {
      return;
    }

    Event event = (Event) arg;

    if (event.getLrsStatement() != null) {
      return;
    }

    String eventName = event.getEvent();
    String siteId = event.getContext();

    if (eventName.equals(GradebookService.EVENT_GRADED)) {

      GradingReference ref = GradingReferenceReckoner.reckoner().reference(event.getResource()).reckon();
      String itemId = ref.getItem().toString();
      String studentId = ref.getStudent();
      String toolId = ref.getTool();
      Optional<AnalyticsGrade> existingGrade = analyticsGradeRepository.findByItemIdAndStudentId(itemId, studentId);
      if (existingGrade.isPresent()) {
        AnalyticsGrade existing = existingGrade.get();
        existing.setPoints(Double.parseDouble(gradebookService.getGradeDefinitionForStudentForItem(siteId, ref.getItem(), ref.getStudent()).getGrade()));
        existing.setPercentage((existing.getPoints() / existing.getMaxPoints()) * 100D);
        analyticsGradeRepository.save(existingGrade.get());
      } else {
        AnalyticsGrade grade = new AnalyticsGrade();

        grade.setSiteRef(siteService.siteReference(siteId));
        grade.setItemId(itemId);
        grade.setStudentId(ref.getStudent());
        grade.setToolId(toolId);
        GradeDefinition gd = gradebookService.getGradeDefinitionForStudentForItem(siteId, ref.getItem(), ref.getStudent());
        grade.setPoints(Double.parseDouble(gd.getGrade()));
        grade.setMaxPoints(gradebookService.getAssignment(siteId, ref.getItem()).getPoints());
        grade.setPercentage((grade.getPoints() / grade.getMaxPoints()) * 100D);
        addSiteProperties(grade, siteId);

        analyticsGradeRepository.save(grade);
      }
    }
  }

  private void addSiteProperties(AnalyticsGrade grade, String siteId) {

    Site site;
    try {
      site = siteService.getSite(siteId);
    } catch (Exception e) {
      log.error("Exception thrown while getting site with ref {}: {}", siteId, e.toString());
      return;
    }

    ResourceProperties props = site.getProperties();
    grade.setTermId(props.getProperty(Site.PROP_SITE_TERM_EID));
    grade.setDepartment(props.getProperty("Department"));
    grade.setSchool(props.getProperty("School"));
    grade.setSubject(props.getProperty("Subject"));
  }
}
