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
package org.sakaiproject.analytics.impl.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.sakaiproject.analytics.api.ReportParams;
import org.sakaiproject.analytics.api.model.AnalyticsGrade;
import org.sakaiproject.analytics.api.repository.AnalyticsGradeRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

public class AnalyticsGradeRepositoryImpl extends SpringCrudRepositoryImpl<AnalyticsGrade, Long> implements AnalyticsGradeRepository {

  public List<AnalyticsGrade> findByDepartment(String department) {
    return null;
  }

  public List<AnalyticsGrade> findBySchool(String school) {
    return null;
  }

  public List<AnalyticsGrade> findByParams(ReportParams params) {

    Session session = sessionFactory.getCurrentSession();

    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<AnalyticsGrade> query = cb.createQuery(AnalyticsGrade.class);
    Root<AnalyticsGrade> grade = query.from(AnalyticsGrade.class);
    List<Predicate> restrictions = new ArrayList<>();
    if (params.getSiteRefs().size() > 0) {
      restrictions.add(grade.get("siteRef").in(params.getSiteRefs()));
    }
    if (params.getUserIds() != null && params.getUserIds().size() > 0) {
      restrictions.add(grade.get("studentId").in(params.getUserIds()));
    }
    if (params.getTermIds() != null && params.getTermIds().size() > 0) {
      restrictions.add(grade.get("termId").in(params.getTermIds()));
    }
    if (params.getDepartments() != null && params.getDepartments().size() > 0) {
      restrictions.add(grade.get("department").in(params.getDepartments()));
    }
    if (params.getSubjects() != null && params.getSubjects().size() > 0) {
      restrictions.add(grade.get("subject").in(params.getSubjects()));
    }

    if (restrictions.size() == 0) {
      return Collections.<AnalyticsGrade>emptyList();
    }

    return session.createQuery(query.where(cb.and(restrictions.toArray(new Predicate[0])))).list();
  }

  public List<AnalyticsGrade> findBySiteId(String siteId) {

    Session session = sessionFactory.getCurrentSession();

    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<AnalyticsGrade> query = cb.createQuery(AnalyticsGrade.class);
    Root<AnalyticsGrade> grade = query.from(AnalyticsGrade.class);
    return session.createQuery(query.where(cb.equal(grade.get("siteId"), siteId))).list();
  }

  public List<AnalyticsGrade> findBySiteRefIn(Set<String> siteRefs) {

    Session session = sessionFactory.getCurrentSession();

    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<AnalyticsGrade> query = cb.createQuery(AnalyticsGrade.class);
    Root<AnalyticsGrade> grade = query.from(AnalyticsGrade.class);

    return session.createQuery(query.where(grade.get("siteRef").in(siteRefs))).list();
  }

  public Optional<AnalyticsGrade> findByItemIdAndStudentId(String itemId, String studentId) {

    Session session = sessionFactory.getCurrentSession();

    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<AnalyticsGrade> query = cb.createQuery(AnalyticsGrade.class);
    Root<AnalyticsGrade> grade = query.from(AnalyticsGrade.class);
    return session.createQuery(query.where(cb.and(cb.equal(grade.get("itemId"), itemId), cb.equal(grade.get("studentId"), studentId)))).uniqueResultOptional();
  }


  public List<AnalyticsGrade> findBySubject(String subject) {
    return null;
  }

  public List<AnalyticsGrade> findByTermId(String termId) {
    return null;
  }
}
