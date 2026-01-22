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

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "SAKAI_SITE", indexes = {
                        @Index(name = "IE_SAKAI_SITE_CREATED", columnList = "CREATEDBY, CREATEDON"),
                        @Index(name = "IE_SAKAI_SITE_MODDED", columnList = "MODIFIEDBY, MODIFIEDON"),
                        @Index(name = "IE_SAKAI_SITE_FLAGS", columnList = "SITE_ID, IS_SPECIAL, IS_USER")
                    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SiteEntity implements PersistableEntity<String> {

    @Id
    @Column(name = "SITE_ID", length = 99, nullable = false)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @EqualsAndHashCode.Include
    private String id;

    @Column(name = "TITLE", length = 99)
    private String title;

    @Column(name = "TYPE", length = 99)
    private String type;

    @Lob
    @Column(name = "SHORT_DESC")
    private String shortDescription;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ICON_URL", length = 255)
    private String iconUrl;

    @Column(name = "INFO_URL", length = 255)
    private String infoUrl;

    @Column(name = "SKIN", length = 255)
    private String skin;

    @Column(name = "PUBLISHED")
    private Boolean published;

    @Column(name = "JOINABLE")
    private Boolean joinable;

    @Column(name = "PUBVIEW")
    private Boolean pubView;

    @Column(name = "JOIN_ROLE", length = 99)
    private String joinRole;

    @Column(name = "CREATEDBY", length = 99)
    private String createdBy;

    @Column(name = "MODIFIEDBY", length = 99)
    private String modifiedBy;

    @Column(name = "CREATEDON")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOn;

    @Column(name = "MODIFIEDON")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedOn;

    @Column(name = "IS_SPECIAL")
    private Boolean special;

    @Column(name = "IS_USER")
    private Boolean isUser;

    @Column(name = "CUSTOM_PAGE_ORDERED")
    private Boolean customPageOrdered;

    @Column(name = "IS_SOFTLY_DELETED", nullable = false)
    private Boolean softlyDeleted = Boolean.FALSE;

    @Column(name = "SOFTLY_DELETED_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date softlyDeletedDate;

    @ElementCollection
    @CollectionTable(name = "SAKAI_SITE_PROPERTY", joinColumns = @JoinColumn(name = "SITE_ID"))
    @MapKeyColumn(name = "NAME", length = 99)
    @Lob
    @Column(name = "VALUE")
    @Fetch(FetchMode.SUBSELECT)
    private Map<String, String> properties = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "SAKAI_SITE_USER", joinColumns = @JoinColumn(name = "SITE_ID"), indexes = @Index(name = "IE_SAKAI_SITE_USER_USER2", columnList = "USER_ID"))
    @MapKeyColumn(name = "USER_ID", length = 99)
    @Column(name = "PERMISSION", nullable = false)
    @Fetch(FetchMode.SUBSELECT)
    private Map<String, Integer> users = new HashMap<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "SITE_ID")
    private Set<SitePageEntity> pages;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "SITE_ID")
    private Set<SitePageProperty> pageProperties;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "SITE_ID")
    private Set<SiteTool> tools;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "SITE_ID")
    private Set<SiteToolProperty> toolProperties;
}
