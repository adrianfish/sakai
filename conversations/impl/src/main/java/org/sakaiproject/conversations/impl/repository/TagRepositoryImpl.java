package org.sakaiproject.conversations.impl.repository;

import java.util.List;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.sakaiproject.conversations.api.model.Tag;
import org.sakaiproject.conversations.api.repository.TagRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

import org.springframework.transaction.annotation.Transactional;

public class TagRepositoryImpl extends SpringCrudRepositoryImpl<Tag, Long>  implements TagRepository {

    @Transactional
    public List<Tag> findBySiteId(String siteId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Tag> query = cb.createQuery(Tag.class);
        query.where(cb.equal(query.from(Tag.class).get("siteId"), siteId));

        return session.createQuery(query).list();
    }
}
