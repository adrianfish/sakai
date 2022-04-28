package org.sakaiproject.conversations.impl.repository;

import java.util.Optional;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.sakaiproject.conversations.api.model.Settings;
import org.sakaiproject.conversations.api.repository.SettingsRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

import org.springframework.transaction.annotation.Transactional;

public class SettingsRepositoryImpl extends SpringCrudRepositoryImpl<Settings, Long>  implements SettingsRepository {

    @Transactional
    public Optional<Settings> findBySiteId(String siteId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Settings> query = cb.createQuery(Settings.class);
        query.where(cb.equal(query.from(Settings.class).get("siteId"), siteId));

        return session.createQuery(query).uniqueResultOptional();
    }
}
