package org.sakaiproject.conversations.api.repository;

import java.util.List;
import java.util.Optional;

import org.sakaiproject.conversations.api.Reaction;
import org.sakaiproject.conversations.api.model.TopicReactionTotal;
import org.sakaiproject.springframework.data.SpringCrudRepository;

public interface TopicReactionTotalRepository extends SpringCrudRepository<TopicReactionTotal, Long> {

    List<TopicReactionTotal> findByTopicId(String topicId);
    Optional<TopicReactionTotal> findByTopicIdAndReaction(String topicId, Reaction reaction);
    Integer deleteByTopicId(String topicId);
}
