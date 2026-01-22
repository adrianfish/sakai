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
package org.sakaiproject.site.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.util.Set;

import org.hibernate.annotations.GenericGenerator;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "SAKAI_SITE_TOOL")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SiteTool implements PersistableEntity<String> {

    @Id
    @Column(name = "TOOL_ID", length = 99, nullable = false)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @EqualsAndHashCode.Include
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PAGE_ID", nullable = false)
    private SitePageEntity page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SITE_ID",  nullable = false)
    private SiteEntity site;

    @Column(name = "REGISTRATION", length = 99, nullable = false)
    private String registration;

    @Column(name = "PAGE_ORDER", nullable = false)
    private Integer pageOrder;

    @Column(name = "TITLE", length = 99)
    private String title;

    @Column(name = "LAYOUT_HINTS", length = 99)
    private String layoutHints;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "TOOL_ID")
    private Set<SiteToolProperty> properties;
}
