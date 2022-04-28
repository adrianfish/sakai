package org.sakaiproject.conversations.impl.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.sakaiproject.conversations.api.model.PostStatus;
import org.sakaiproject.conversations.api.repository.PostStatusRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

import org.springframework.transaction.annotation.Transactional;

public class PostStatusRepositoryImpl extends SpringCrudRepositoryImpl<PostStatus, Long>  implements PostStatusRepository {

    @Transactional(readOnly = true)
    public List<PostStatus> findByUserId(String userId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<PostStatus> query = cb.createQuery(PostStatus.class);
        query.where(cb.equal(query.from(PostStatus.class).get("userId"), userId));

        return session.createQuery(query).list();
    }

    @Transactional(readOnly = true)
    public Optional<PostStatus> findByPostIdAndUserId(String postId, String userId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<PostStatus> query = cb.createQuery(PostStatus.class);
        Root<PostStatus> postStatus = query.from(PostStatus.class);
        query.where(cb.and(cb.equal(postStatus.get("postId"), postId), cb.equal(postStatus.get("userId"), userId)));

        return session.createQuery(query).uniqueResultOptional();
    }

    @Transactional(readOnly = true)
    public List<PostStatus> findByPostIdAndUserIdNot(String postId, String userId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<PostStatus> query = cb.createQuery(PostStatus.class);
        Root<PostStatus> postStatus = query.from(PostStatus.class);
        query.where(cb.and(cb.equal(postStatus.get("postId"), postId), cb.notEqual(postStatus.get("userId"), userId)));

        return session.createQuery(query).list();
    }

    @Transactional(readOnly = true)
    public List<PostStatus> findByTopicIdAndUserId(String topicId, String userId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<PostStatus> query = cb.createQuery(PostStatus.class);
        Root<PostStatus> postStatus = query.from(PostStatus.class);
        query.where(cb.and(cb.equal(postStatus.get("topicId"), topicId), cb.equal(postStatus.get("userId"), userId)));

        return session.createQuery(query).list();
    }

    @Transactional(readOnly = true)
    public List<PostStatus> findByTopicIdAndUserIdAndViewed(String topicId, String userId, Boolean viewed) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<PostStatus> query = cb.createQuery(PostStatus.class);
        Root<PostStatus> postStatus = query.from(PostStatus.class);
        query.where(cb.and(cb.equal(postStatus.get("topicId"), topicId),
                            cb.equal(postStatus.get("userId"), userId),
                            cb.equal(postStatus.get("viewed"), viewed)));

        return session.createQuery(query).list();
    }

    @Transactional
    public Integer deleteByPostId(String postId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaDelete<PostStatus> delete = cb.createCriteriaDelete(PostStatus.class);
        Root<PostStatus> postStatus = delete.from(PostStatus.class);
        delete.where(cb.equal(postStatus.get("postId"), postId));

        return session.createQuery(delete).executeUpdate();
    }
}
