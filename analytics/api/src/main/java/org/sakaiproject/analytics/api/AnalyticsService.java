package org.sakaiproject.analytics.api;

import java.util.Map;
import java.util.Set;

import org.sakaiproject.user.api.User;

public interface AnalyticsService {

    Map<String, Object> getGradeDistribution(ReportParams params);
    Set<User> getUsersForGradeReport(String siteRef);
    Set<String> getTermsForGradeReport(String siteRef);
    Set<String> getDepartmentsForGradeReport(String siteRef);
    Set<String> getSubjectsForGradeReport(String siteRef);
}
