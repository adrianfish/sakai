package org.sakaiproject.collaborativemaps.api.persistence;

import org.sakaiproject.collaborativemaps.api.model.CollaborativeMap;

import org.sakaiproject.hibernate.CrudRepository;

public interface CollaborativeMapRepository extends CrudRepository<CollaborativeMap, Long> {
}
