/**
 * Copyright (c) 2003-2019 The Apereo Foundation
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
package org.sakaiproject.user.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "WEBAUTHN_CREDENTIALS")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WebAuthnCredential implements PersistableEntity<Long> {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "user_notification_id_sequence")
    @SequenceGenerator(name = "user_notification_id_sequence", sequenceName = "USER_NOTIFICATIONS_S")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "USER_ID", length = 99, nullable = false)
    private String userId;

    // For length explanation, see https://github.com/w3c/webauthn/pull/1664
    @Column(name = "CREDENTIAL_ID", length = 1024, unique = true, nullable = false)
    private byte[] credentialId;

    @Lob
    @Column(name = "ATTESTED_CREDENTIAL_DATA", nullable = false)
    private byte[] attestedCredentialData;

    @Lob
    @Column(name = "ATTESTATION_STATEMENT", nullable = false)
    private byte[] attestationStatement;

    @Column(name = "TRANSPORTS",  nullable = false)
    private String transports;

    @Column(name = "COUNTER")
    private Long counter;

    @Lob
    @Column(name = "AUTHENTICATOR_EXTENSIONS")
    private byte[] authenticatorExtensions;

    @Column(name = "CLIENT_EXTENSIONS")
    private String clientExtensions;
}
