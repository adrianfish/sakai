package org.sakaiproject.conversations.api.repository;

import java.util.List;
import java.util.Optional;

import org.sakaiproject.conversations.api.Reaction;
import org.sakaiproject.conversations.api.model.PostReactionTotal;
import org.sakaiproject.springframework.data.SpringCrudRepository;

public interface PostReactionTotalRepository extends SpringCrudRepository<PostReactionTotal, Long> {

    List<PostReactionTotal> findByPostId(String postId);
    Optional<PostReactionTotal> findByPostIdAndReaction(String postId, Reaction reaction);
    Integer deleteByPostId(String postId);
}
