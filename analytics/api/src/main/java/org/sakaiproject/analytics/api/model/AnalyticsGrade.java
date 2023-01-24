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
package org.sakaiproject.analytics.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ANAL_GRADE",
    uniqueConstraints = { @UniqueConstraint(name = "UniqueItemStudent", columnNames = { "ITEM_ID", "STUDENT_ID" }) })
@Getter
@Setter
public class AnalyticsGrade implements PersistableEntity<Long> {

    @Id
    @GeneratedValue
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "ITEM_ID", nullable = false)
    private String itemId;

    @Column(name = "STUDENT_ID", length = 99, nullable = false)
    private String studentId;

    @Column(name = "POINTS", nullable = false)
    private Double points;

    @Column(name = "MAX_POINTS", nullable = false)
    private Double maxPoints;

    @Column(name = "PERCENTAGE", nullable = false)
    private Double percentage;

    @Column(name = "SITE_REF", length = 99, nullable = false)
    private String siteRef;

    @Column(name = "TERM_ID", length = 255)
    private String termId;

    @Column(name = "TOOL_ID", length = 255)
    private String toolId;

    @Column(name = "DEPARTMENT", length = 255)
    private String department;

    @Column(name = "SCHOOL", length = 255)
    private String school;

    @Column(name = "SUBJECT", length = 255)
    private String subject;
}
