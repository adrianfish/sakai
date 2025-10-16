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

package org.sakaiproject.tags.api;

import org.sakaiproject.tags.api.model.TagCollection;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record TagCollectionRecord(String id, String name, String description,
    String createdBy, Long creationDate, String externalSourceName,
    String externalSourceDescription, String lastModifiedBy, Long lastModificationDate,
    Boolean externalUpdate, Boolean externalCreation, Long lastSynchronizationDate,
    Long lastUpdateDateInExternalSystem) implements TagCollectionRecordBuilder.With {

    public static TagCollectionRecord fromTagCollection(TagCollection tc) {

        return TagCollectionRecordBuilder.builder()
            .id(tc.getId())
            .name(tc.getName())
            .description(tc.getDescription())
            .createdBy(tc.getCreatedBy())
            .creationDate(tc.getCreationDate())
            .externalSourceName(tc.getExternalSourceName())
            .externalSourceDescription(tc.getExternalSourceDescription())
            .lastModifiedBy(tc.getLastModifiedBy())
            .lastModificationDate(tc.getLastModificationDate())
            .externalUpdate(tc.getExternalUpdate())
            .externalCreation(tc.getExternalCreation())
            .lastSynchronizationDate(tc.getLastSynchronizationDate())
            .lastUpdateDateInExternalSystem(tc.getLastUpdateDateInExternalSystem())
            .build();
    }

    public TagCollection mergeInto(TagCollection tc) {

        tc.setName(name);
        tc.setDescription(description);
        tc.setExternalSourceName(externalSourceName);
        tc.setExternalSourceDescription(externalSourceDescription);
        tc.setLastModifiedBy(lastModifiedBy);
        tc.setLastModificationDate(lastModificationDate);
        tc.setExternalUpdate(externalUpdate);
        tc.setExternalCreation(externalCreation);
        tc.setLastSynchronizationDate(lastSynchronizationDate);
        tc.setLastUpdateDateInExternalSystem(lastUpdateDateInExternalSystem);

        return tc;
    }
}
    
