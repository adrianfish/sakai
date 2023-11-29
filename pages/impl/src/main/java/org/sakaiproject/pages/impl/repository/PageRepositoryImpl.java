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
package org.sakaiproject.pages.impl.repository;

//import javax.persistence.criteria.CriteriaBuilder;
//import javax.persistence.criteria.CriteriaQuery;

import org.sakaiproject.pages.api.model.PagesPage;
import org.sakaiproject.pages.api.repository.PageRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

import java.util.List;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

//import org.springframework.transaction.annotation.Transactional;

public class PageRepositoryImpl extends SpringCrudRepositoryImpl<PagesPage, String> implements PageRepository {

    public List<PagesPage> findBySiteId(String siteId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<PagesPage> query = cb.createQuery(PagesPage.class);
        Root<PagesPage> page = query.from(PagesPage.class);
        query.where(cb.equal(page.get("siteId"), siteId));

        return session.createQuery(query).list();
    }
}
