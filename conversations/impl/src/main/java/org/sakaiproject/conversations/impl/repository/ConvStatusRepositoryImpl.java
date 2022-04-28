package org.sakaiproject.conversations.impl.repository;

import java.util.Optional;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.sakaiproject.conversations.api.model.ConvStatus;
import org.sakaiproject.conversations.api.repository.ConvStatusRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

import org.springframework.transaction.annotation.Transactional;

public class ConvStatusRepositoryImpl extends SpringCrudRepositoryImpl<ConvStatus, Long>  implements ConvStatusRepository {

    @Transactional
    public Optional<ConvStatus> findBySiteIdAndUserId(String siteId, String userId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ConvStatus> query = cb.createQuery(ConvStatus.class);
        Root<ConvStatus> status = query.from(ConvStatus.class);
        query.where(cb.and(cb.equal(status.get("siteId"), siteId),
                            cb.equal(status.get("userId"), userId)));

        return session.createQuery(query).uniqueResultOptional();
    }
}
