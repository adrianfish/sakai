package org.sakaiproject.conversations.api.repository;

import java.util.List;

import org.sakaiproject.conversations.api.model.ConversationsTopic;
import org.sakaiproject.springframework.data.SpringCrudRepository;

public interface ConversationsTopicRepository extends SpringCrudRepository<ConversationsTopic, String> {

    List<ConversationsTopic> findBySiteId(String siteId);
    List<ConversationsTopic> findByTags_Id(Long tagId);
    Long countBySiteIdAndMetadata_Creator_Id(String siteId, String creatorId);
    Integer lockBySiteId(String siteId, Boolean locked);
}
