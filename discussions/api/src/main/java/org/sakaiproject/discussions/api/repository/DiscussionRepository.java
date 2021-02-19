package org.sakaiproject.discussions.api.repository;

import org.sakaiproject.discussions.api.model.Discussion;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface DiscussionRepository extends PagingAndSortingRepository<Discussion, Long> {
}
