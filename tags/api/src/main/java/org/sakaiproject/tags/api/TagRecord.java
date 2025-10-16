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

import org.sakaiproject.tags.api.model.Tag;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record TagRecord(String id, String collectionId, String label, String description,
    String alternativeLabels, String createdBy, Long creationDate, String lastModifiedBy,
    long  lastModificationDate, String externalId, Boolean externalCreation,
    long  externalCreationDate, Boolean externalUpdate, long  lastUpdateDateInExternalSystem,
    String parentId, String externalHierarchyCode, String externalType, String data, String collectionName) implements TagRecordBuilder.With {

    public static TagRecord fromTag(Tag tag) {

        return TagRecordBuilder.builder()
            .id(tag.getId())
            .collectionId(tag.getTagCollection().getId())
            .label(tag.getLabel())
            .description(tag.getDescription())
            .alternativeLabels(tag.getAlternativeLabels())
            .createdBy(tag.getCreatedBy())
            .creationDate(tag.getCreationDate())
            .lastModifiedBy(tag.getLastModifiedBy())
            .lastModificationDate(tag.getLastModificationDate())
            .externalId(tag.getExternalId())
            .externalCreation(tag.getExternalCreation())
            .externalCreationDate(tag.getExternalCreationDate())
            .externalUpdate(tag.getExternalUpdate())
            .lastUpdateDateInExternalSystem(tag.getLastUpdateDateInExternalSystem())
            .parentId(tag.getParentId())
            .externalHierarchyCode(tag.getExternalHierarchyCode())
            .externalType(tag.getExternalType())
            .data(tag.getData())
            .collectionName(tag.getTagCollection().getName())
            .build();
    }

    public Tag mergeInto(Tag tag) {

        tag.setLabel(label);
        tag.setDescription(description);
        tag.setAlternativeLabels(alternativeLabels);
        tag.setLastModifiedBy(lastModifiedBy);
        tag.setLastModificationDate(lastModificationDate);
        tag.setExternalId(externalId);
        tag.setLastUpdateDateInExternalSystem(lastUpdateDateInExternalSystem);
        tag.setParentId(parentId);
        tag.setExternalHierarchyCode(externalHierarchyCode);
        tag.setExternalType(externalType);
        tag.setData(data);
        return tag;
    }
}
