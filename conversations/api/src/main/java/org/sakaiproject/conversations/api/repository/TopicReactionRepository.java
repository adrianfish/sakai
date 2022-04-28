package org.sakaiproject.conversations.api.repository;

import java.util.List;

import org.sakaiproject.conversations.api.model.TopicReaction;
import org.sakaiproject.springframework.data.SpringCrudRepository;

public interface TopicReactionRepository extends SpringCrudRepository<TopicReaction, Long> {

    List<TopicReaction> findByTopicIdAndUserId(String topicId, String userId);
    Integer deleteByTopicId(String topicId);
}
