/**********************************************************************************
 *
 * Copyright (c) 2016 The Sakai Foundation
 *
 * Original developers:
 *
 *   Unicon
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.tags.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.sakaiproject.tags.api.TagRecord;
import org.sakaiproject.springframework.data.PersistableEntity;

import org.hibernate.annotations.GenericGenerator;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tagservice_tag",
    indexes = {
        @Index(name = "tagservice_tag_taglabel", columnList = "taglabel"),
        @Index(name = "tagservice_tag_tagcollectionid", columnList = "tagcollectionid"),
        @Index(name = "tagservice_tag_externalid", columnList = "externalid")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tagservice_tag_label_collection", columnNames = { "taglabel", "tagcollectionid" }),
    })
public class Tag implements PersistableEntity<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "tagid", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tagcollectionid")
    private TagCollection tagCollection;

    @Column(name = "taglabel", length = 255)
    private String label;

    @Lob
    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "createdby", length = 255)
    private String createdBy;

    @Column(name = "creationdate")
    private Long creationDate = 0L;

    @Column(name = "lastmodifiedby", length = 255)
    private String lastModifiedBy;

    @Column(name = "lastmodificationdate")
    private Long  lastModificationDate = 0L;

    @Column(name = "externalid", length = 255)
    private String externalId;

    @Lob
    @Column(name = "alternativelabels")
    private String alternativeLabels;

    @Column(name = "externalcreation")
    private Boolean externalCreation = Boolean.FALSE;

    @Column(name = "externalcreationDate")
    private Long  externalCreationDate = 0L;

    @Column(name = "externalupdate")
    private Boolean externalUpdate = Boolean.FALSE;

    @Column(name = "lastupdatedateinexternalsystem")
    private Long  lastUpdateDateInExternalSystem = 0L;

    @Column(name = "parentid", length = 255)
    private String parentId;

    @Lob
    @Column(name = "externalhierarchycode")
    private String externalHierarchyCode;

    @Column(name = "externaltype")
    private String externalType;

    @Lob
    @Column(name = "data")
    private String data;

    public static Tag fromRecord(TagRecord tag) {

        return Tag.builder()
            .id(tag.id())
            .label(tag.label())
            .description(tag.description())
            .createdBy(tag.createdBy())
            .creationDate(tag.creationDate())
            .lastModifiedBy(tag.lastModifiedBy())
            .lastModificationDate(tag.lastModificationDate())
            .externalId(tag.externalId())
            .externalCreation(tag.externalCreation())
            .externalCreationDate(tag.externalCreationDate())
            .externalUpdate(tag.externalUpdate())
            .lastUpdateDateInExternalSystem(tag.lastUpdateDateInExternalSystem())
            .parentId(tag.parentId())
            .externalHierarchyCode(tag.externalHierarchyCode())
            .externalType(tag.externalType())
            .data(tag.data())
            .build();
    }
}
