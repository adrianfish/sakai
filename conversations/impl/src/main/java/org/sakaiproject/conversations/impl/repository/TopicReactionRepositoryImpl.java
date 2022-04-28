package org.sakaiproject.conversations.impl.repository;

import java.util.List;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.sakaiproject.conversations.api.model.TopicReaction;
import org.sakaiproject.conversations.api.repository.TopicReactionRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

import org.springframework.transaction.annotation.Transactional;

public class TopicReactionRepositoryImpl extends SpringCrudRepositoryImpl<TopicReaction, Long>  implements TopicReactionRepository {

    @Transactional
    public List<TopicReaction> findByTopicIdAndUserId(String topicId, String userId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<TopicReaction> query = cb.createQuery(TopicReaction.class);
        Root<TopicReaction> reaction = query.from(TopicReaction.class);
        query.where(cb.and(cb.equal(reaction.get("userId"), userId),
                            cb.equal(reaction.get("topicId"), topicId)));

        return session.createQuery(query).list();
    }

    @Transactional
    public Integer deleteByTopicId(String topicId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaDelete<TopicReaction> delete = cb.createCriteriaDelete(TopicReaction.class);
        Root<TopicReaction> reaction = delete.from(TopicReaction.class);
        delete.where(cb.equal(reaction.get("topicId"), topicId));

        return session.createQuery(delete).executeUpdate();
    }
}
