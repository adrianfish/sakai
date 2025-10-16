/**********************************************************************************
 *
 * Copyright (c) 2016 The Sakai Foundation
 *
 * Original developers:
 *
 *   Unicon
 *
 *
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

package org.sakaiproject.tags.tool.forms;

import javax.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.tags.api.TagCollectionRecord;
import org.sakaiproject.tags.api.TagCollectionRecordBuilder;
import org.sakaiproject.tags.api.Errors;

/**
 * Maps to and from the collection HTML form and a collection data object.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class TagCollectionForm extends BaseForm {

    private String id;
    private String name;
    private String description;
    private String createdBy;
    private long creationDate;
    private String externalSourceName;
    private String externalSourceDescription;
    private String lastModifiedBy;
    private long lastModificationDate;
    private Boolean externalUpdate;
    private Boolean externalCreation;
    private long lastSynchronizationDate;
    private long lastUpdateDateInExternalSystem;

    public static TagCollectionForm fromTagCollection(TagCollectionRecord collection) {

        try {
            return new TagCollectionForm(
                    collection.id(),
                    collection.name(),
                    collection.description(),
                    collection.createdBy(),
                    collection.creationDate(),
                    collection.externalSourceName(),
                    collection.externalSourceDescription(),
                    collection.lastModifiedBy(),
                    collection.lastModificationDate(),
                    collection.externalUpdate(),
                    collection.externalCreation(),
                    collection.lastSynchronizationDate(),
                    collection.lastUpdateDateInExternalSystem());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TagCollectionRecord toTagCollection() {

        return TagCollectionRecordBuilder.builder()
            .id(id)
            .name(name)
            .description(description)
            .createdBy(createdBy)
            .creationDate(creationDate)
            .externalSourceName(externalSourceName)
            .externalSourceDescription(externalSourceDescription)
            .lastModifiedBy(lastModifiedBy)
            .lastModificationDate(lastModificationDate)
            .externalUpdate(externalUpdate)
            .externalCreation(externalCreation)
            .lastSynchronizationDate(lastSynchronizationDate)
            .lastUpdateDateInExternalSystem(lastUpdateDateInExternalSystem)
            .build();
    }
}

