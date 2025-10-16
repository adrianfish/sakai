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

import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;
import org.sakaiproject.tags.api.model.Tag;
import org.sakaiproject.tags.api.model.TagCollection;
import org.sakaiproject.tags.api.repository.TagRepository;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public class TagRepositoryImpl extends SpringCrudRepositoryImpl<Tag, String>  implements TagRepository {

    public List<Tag> findByCollection(String collectionId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Tag> query = cb.createQuery(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.equal(tag.get("tagCollection").get("id"), collectionId));

        return session.createQuery(query).list();
    }

    public List<Tag> findByLabel(String label) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Tag> query = cb.createQuery(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.equal(cb.lower(tag.get("label")), label.toLowerCase()));

        return session.createQuery(query).list();
    }

    public Optional<Tag> findByLabelAndCollection(String label, TagCollection collection) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Tag> query = cb.createQuery(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.and(cb.equal(cb.lower(tag.get("label")), label.toLowerCase()),
                            cb.equal(tag.get("tagCollection"), collection)));

        return session.createQuery(query).uniqueResultOptional();
    }

    public List<Tag> findByLabelLike(String pattern) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Tag> query = cb.createQuery(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.like(cb.lower(tag.get("label")), pattern.toLowerCase()));

        return session.createQuery(query).list();
    }

    public List<Tag> findByLabelLikePaged(String pattern, Pageable pageable) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Tag> query = cb.createQuery(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.like(cb.lower(tag.get("label")), pattern.toLowerCase()));

        return session.createQuery(query).setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).list();
    }

    public List<Tag> findByCollectionPaged(String tagCollectionId, Pageable pageable) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Tag> query = cb.createQuery(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.equal(tag.get("tagCollection").get("id"), tagCollectionId));

        return session.createQuery(query).setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).list();
    }

    public long countByLabelLike(String pattern) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Tag> tag = query.from(Tag.class);
        query.select(cb.count(tag)).where(cb.like(cb.lower(tag.get("label")), pattern.toLowerCase()));

        return session.createQuery(query).getSingleResult();
    }

    public long countByCollection(String collectionId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Tag> tag = query.from(Tag.class);
        query.select(cb.count(tag)).where(cb.equal(tag.get("tagCollection").get("id"), collectionId));

        return session.createQuery(query).getSingleResult();
    }

    public List<Tag> findByExternalIdAndCollection(String externalId, String collectionId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Tag> query = cb.createQuery(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.equal(tag.get("tagCollection").get("id"), collectionId), cb.equal(tag.get("externalId"), externalId));

        return session.createQuery(query).list();
    }

    public void deleteByExternalIdAndCollection(String externalId, String tagCollectionId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaDelete<Tag> query = cb.createCriteriaDelete(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.equal(tag.get("tagCollection").get("id"), tagCollectionId), cb.equal(tag.get("externalId"), externalId));

        session.createQuery(query).executeUpdate();
    }

    public List<Tag> findByCollectionAndLastModificationDateLessThan(String tagCollectionId, long lastModificationDate) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Tag> query = cb.createQuery(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.equal(tag.get("tagCollection").get("id"), tagCollectionId), cb.lessThan(tag.get("lastModificationDate"), lastModificationDate));

        return session.createQuery(query).list();
    }

    public void deleteByCollectionAndLastModificationDateLessThan(String tagCollectionId, long lastModificationDate) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaDelete<Tag> query = cb.createCriteriaDelete(Tag.class);
        Root<Tag> tag = query.from(Tag.class);
        query.where(cb.equal(tag.get("tagCollection").get("id"), tagCollectionId), cb.lessThan(tag.get("lastModificationDate"), lastModificationDate));

        session.createQuery(query).executeUpdate();
    }
}
