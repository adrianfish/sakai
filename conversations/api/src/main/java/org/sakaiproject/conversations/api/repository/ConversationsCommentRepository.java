package org.sakaiproject.conversations.api.repository;

import java.util.List;

import org.sakaiproject.conversations.api.model.ConversationsComment;
import org.sakaiproject.springframework.data.SpringCrudRepository;

public interface ConversationsCommentRepository extends SpringCrudRepository<ConversationsComment, String> {

    List<ConversationsComment> findByPostId(String postId);
    List<ConversationsComment> findBySiteId(String siteId);
    Integer deleteByPostId(String postId);
    Integer deleteByTopicId(String topicId);
    Integer lockByPostId(String postId, Boolean locked);
    Integer lockBySiteId(String siteId, Boolean locked);
}
