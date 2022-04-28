package org.sakaiproject.conversations.api.repository;

import java.util.List;

import org.sakaiproject.conversations.api.model.PostReaction;
import org.sakaiproject.springframework.data.SpringCrudRepository;

public interface PostReactionRepository extends SpringCrudRepository<PostReaction, Long> {

    List<PostReaction> findByPostIdAndUserId(String postId, String userId);
    Integer deleteByPostId(String postId);
}
