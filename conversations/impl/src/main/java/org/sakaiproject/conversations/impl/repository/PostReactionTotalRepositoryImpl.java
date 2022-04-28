package org.sakaiproject.conversations.impl.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.sakaiproject.conversations.api.Reaction;
import org.sakaiproject.conversations.api.model.PostReactionTotal;
import org.sakaiproject.conversations.api.repository.PostReactionTotalRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

import org.springframework.transaction.annotation.Transactional;

public class PostReactionTotalRepositoryImpl extends SpringCrudRepositoryImpl<PostReactionTotal, Long>  implements PostReactionTotalRepository {

    @Transactional(readOnly = true)
    public List<PostReactionTotal> findByPostId(String postId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<PostReactionTotal> query = cb.createQuery(PostReactionTotal.class);
        Root<PostReactionTotal> total = query.from(PostReactionTotal.class);
        query.where(cb.equal(total.get("postId"), postId));

        return session.createQuery(query).list();
    }

    @Transactional(readOnly = true)
    public Optional<PostReactionTotal> findByPostIdAndReaction(String postId, Reaction reaction) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<PostReactionTotal> query = cb.createQuery(PostReactionTotal.class);
        Root<PostReactionTotal> total = query.from(PostReactionTotal.class);
        query.where(cb.and(cb.equal(total.get("reaction"), reaction), cb.equal(total.get("postId"), postId)));

        return session.createQuery(query).uniqueResultOptional();
    }

    @Transactional
    public Integer deleteByPostId(String postId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaDelete<PostReactionTotal> delete = cb.createCriteriaDelete(PostReactionTotal.class);
        Root<PostReactionTotal> total = delete.from(PostReactionTotal.class);
        delete.where(cb.equal(total.get("postId"), postId));

        return session.createQuery(delete).executeUpdate();
    }
}
