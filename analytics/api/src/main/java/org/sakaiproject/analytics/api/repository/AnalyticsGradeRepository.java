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
package org.sakaiproject.analytics.api.repository;

import org.sakaiproject.analytics.api.ReportParams;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sakaiproject.analytics.api.model.AnalyticsGrade;
import org.sakaiproject.springframework.data.SpringCrudRepository;

public interface AnalyticsGradeRepository extends SpringCrudRepository<AnalyticsGrade, Long> {

    List<AnalyticsGrade> findByDepartment(String department);
    List<AnalyticsGrade> findBySchool(String school);
    List<AnalyticsGrade> findBySiteId(String siteId);
    List<AnalyticsGrade> findBySiteRefIn(Set<String> siteRefs);
    List<AnalyticsGrade> findByParams(ReportParams params);
    Optional<AnalyticsGrade> findByItemIdAndStudentId(String itemId, String studentId);
    List<AnalyticsGrade> findBySubject(String subject);
    List<AnalyticsGrade> findByTermId(String termId);
}
