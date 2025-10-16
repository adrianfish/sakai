/******************************************************************************
 * Copyright 2023 sakaiproject.org Licensed under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.sakaiproject.tags.impl.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;

import org.hibernate.Session;

import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;
import org.sakaiproject.tags.api.model.TagAssociation;
import org.sakaiproject.tags.api.repository.TagAssociationRepository;

import org.springframework.transaction.annotation.Transactional;

public class TagAssociationRepositoryImpl extends SpringCrudRepositoryImpl<TagAssociation, String>  implements TagAssociationRepository {

	@Override
	public List<TagAssociation> findByCollectionNameAndReference(String collectionName, String reference) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<TagAssociation> query = cb.createQuery(TagAssociation.class);
        Root<TagAssociation> tagAssociation = query.from(TagAssociation.class);
        query.where(cb.and(cb.equal(tagAssociation.get("tag").get("tagCollection").get("name"), collectionName),
                            cb.equal(tagAssociation.get("reference"), reference)));

        return session.createQuery(query).list();
    }

	@Override
	public Optional<TagAssociation> findByReferenceAndTagId(String reference, String tagId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<TagAssociation> query = cb.createQuery(TagAssociation.class);
        Root<TagAssociation> tagAssociation = query.from(TagAssociation.class);
        query.where(cb.and(cb.equal(tagAssociation.get("tag").get("id"), tagId),
                            cb.equal(tagAssociation.get("reference"), reference)));

        return session.createQuery(query).uniqueResultOptional();
	}

	@Override
	public void deleteByReference(String reference) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaDelete<TagAssociation> delete = cb.createCriteriaDelete(TagAssociation.class);
        Root<TagAssociation> tagAssociation = delete.from(TagAssociation.class);
        delete.where(cb.equal(tagAssociation.get("reference"), reference));

        session.createQuery(delete).executeUpdate();
	}
}
