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
package org.sakaiproject.tags.impl.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;

import org.hibernate.Session;

import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;
import org.sakaiproject.tags.api.model.TagCollection;
import org.sakaiproject.tags.api.repository.TagCollectionRepository;

import org.springframework.data.domain.Pageable;

public class TagCollectionRepositoryImpl extends SpringCrudRepositoryImpl<TagCollection, String> implements TagCollectionRepository {

    public Optional<TagCollection> findByName(String name) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<TagCollection> query = cb.createQuery(TagCollection.class);
        Root<TagCollection> tagCollection = query.from(TagCollection.class);
        query.where(cb.equal(tagCollection.get("name"), name));

        return session.createQuery(query).uniqueResultOptional();
    }

    public List<TagCollection> findAllOrderByName() {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<TagCollection> query = cb.createQuery(TagCollection.class);
        Root<TagCollection> tagCollection = query.from(TagCollection.class);
        query.orderBy(cb.asc(tagCollection.get("name")));

        return session.createQuery(query).list();
    }

    public List<TagCollection> findAllPaginatedOrderByName(Pageable pageable) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<TagCollection> query = cb.createQuery(TagCollection.class);
        Root<TagCollection> tagCollection = query.from(TagCollection.class);
        query.orderBy(cb.asc(tagCollection.get("name")));

        return session.createQuery(query).setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).list();
    }

    public Optional<TagCollection> findByExternalSourceName(String externalSourceName) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<TagCollection> query = cb.createQuery(TagCollection.class);
        Root<TagCollection> tagCollection = query.from(TagCollection.class);
        query.where(cb.equal(tagCollection.get("externalSourceName"), externalSourceName));

        return session.createQuery(query).uniqueResultOptional();
    }
}
