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

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.GeneratedValue;

import org.hibernate.annotations.GenericGenerator;

import org.sakaiproject.tags.api.TagCollectionRecord;
import org.sakaiproject.springframework.data.PersistableEntity;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "tagservice_collection", uniqueConstraints = {
    @UniqueConstraint(name = "externalsourcename", columnNames = { "externalsourcename" }),
    @UniqueConstraint(name = "name", columnNames = { "name" }),
})
public class TagCollection implements PersistableEntity<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "tagcollectionid", length = 36, nullable = false)
    private String id;

    @Column(name = "name", length = 255)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "createdby", length = 255)
    private String createdBy;

    @Column(name = "creationdate")
    private Long creationDate = 0L;

    @Column(name = "externalsourcename", length = 255)
    private String externalSourceName;

    @Lob
    @Column(name = "externalsourcedescription")
    private String externalSourceDescription;

    @Column(name = "lastmodifiedby", length = 255)
    private String lastModifiedBy;

    @Column(name = "lastmodificationdate")
    private Long lastModificationDate = 0L;

    @Column(name = "externalupdate")
    private Boolean externalUpdate = Boolean.FALSE;

    @Column(name = "externalcreation")
    private Boolean externalCreation = Boolean.FALSE;

    @Column(name = "lastsynchronizationdate")
    private Long lastSynchronizationDate = 0L;

    @Column(name = "lastupdatedateinexternalsystem")
    private Long lastUpdateDateInExternalSystem = 0L;

    public static TagCollection fromRecord(TagCollectionRecord tcr) {

        TagCollection tc = new TagCollection();
        tc.setId(tcr.id());
        tc.setName(tcr.name());
        tc.setDescription(tcr.description());
        tc.setCreatedBy(tcr.createdBy());
        tc.setCreationDate(tcr.creationDate());
        if (StringUtils.isNotBlank(tcr.externalSourceName())) {
            tc.setExternalSourceName(tcr.externalSourceName());
        }
        tc.setExternalSourceDescription(tcr.externalSourceDescription());
        tc.setLastModifiedBy(tcr.lastModifiedBy());
        tc.setLastModificationDate(tcr.lastModificationDate());
        tc.setExternalUpdate(tcr.externalUpdate());
        tc.setExternalCreation(tcr.externalCreation());
        tc.setLastSynchronizationDate(tcr.lastSynchronizationDate());
        tc.setLastUpdateDateInExternalSystem(tcr.lastUpdateDateInExternalSystem());

        return tc;
    }
}
