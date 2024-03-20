/*
 * Copyright (c) 2003-2021 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.user.impl.repository;

import java.util.Optional;

import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.sakaiproject.user.api.model.WebAuthnCredential;
import org.sakaiproject.user.api.repository.WebAuthnCredentialRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;

public class WebAuthnCredentialRepositoryImpl extends SpringCrudRepositoryImpl<WebAuthnCredential, Long> implements WebAuthnCredentialRepository {

    public Optional<WebAuthnCredential> findByCredentialId(byte[] credentialId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<WebAuthnCredential> query = cb.createQuery(WebAuthnCredential.class);
        Root<WebAuthnCredential> cred = query.from(WebAuthnCredential.class);
        query.where(cb.equal(cred.get("credentialId"), credentialId));
        return session.createQuery(query).uniqueResultOptional();
	}
}
