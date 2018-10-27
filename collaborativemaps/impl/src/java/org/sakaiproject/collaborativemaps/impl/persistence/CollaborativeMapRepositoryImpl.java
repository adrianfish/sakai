package org.sakaiproject.collaborativemaps.impl.persistence;

import org.sakaiproject.collaborativemaps.api.model.CollaborativeMap;
import org.sakaiproject.collaborativemaps.api.persistence.CollaborativeMapRepository;
import org.sakaiproject.hibernate.HibernateCrudRepository;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CollaborativeMapRepositoryImpl extends HibernateCrudRepository<CollaborativeMap, Long> implements CollaborativeMapRepository {
}
