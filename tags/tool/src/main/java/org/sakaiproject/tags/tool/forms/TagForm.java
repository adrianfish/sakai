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

import org.sakaiproject.tags.api.Errors;
import org.sakaiproject.tags.api.MissingUuidException;
import org.sakaiproject.tags.api.TagRecord;
import org.sakaiproject.tags.api.TagRecordBuilder;
import org.sakaiproject.util.api.FormattedText;

import java.util.Optional;

/**
 * Maps to and from the tag HTML form and a tag data object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TagForm extends BaseForm {

    private String id;
    private String collectionId;
    private String label;
    private String description;
    private String createdBy;
    private long creationDate;
    private String lastModifiedBy;
    private long lastModificationDate;
    private String externalId;
    private String alternativeLabels;
    private Boolean externalCreation;
    private long externalCreationDate;
    private Boolean externalUpdate;
    private long lastUpdateDateInExternalSystem;
    private String parentId;
    private String externalHierarchyCode;
    private String externalType;
    private String data;
    private String collectionName;

    public static TagForm fromTag(TagRecord existingTag) {

            String id = existingTag.id();

            if (id == null) {
                throw new RuntimeException("No tagId has been set for this tag");
            }

            return new TagForm(id,
                    existingTag.collectionId(),
                    existingTag.label(),
                    existingTag.description(),
                    existingTag.createdBy(),
                    existingTag.creationDate(),
                    existingTag.lastModifiedBy(),
                    existingTag.lastModificationDate(),
                    existingTag.externalId(),
                    existingTag.alternativeLabels(),
                    existingTag.externalCreation(),
                    existingTag.externalCreationDate(),
                    existingTag.externalUpdate(),
                    existingTag.lastUpdateDateInExternalSystem(),
                    existingTag.parentId(),
                    existingTag.externalHierarchyCode(),
                    existingTag.externalType(),
                    existingTag.data(),
                    existingTag.collectionName());
    }

    public Errors validate(FormattedText formattedText) {

        Errors errors = new Errors();

        // Validate required fields
        if (label == null || label.trim().isEmpty()) {
            errors.addError("label", "tag_label_required");
        }

        // Validate field lengths
        if (label != null && label.length() > 255) {
            errors.addError("label", "tag_label_too_long");
        }

        if (description != null && description.length() > 1000) {
            errors.addError("description", "description_too_long");
        }

        if (externalId != null && externalId.length() > 255) {
            errors.addError("externalId", "external_id_too_long");
        }

        // XSS validation checks
        StringBuilder tagMessages = new StringBuilder();
        formattedText.processFormattedText(label, tagMessages);
        if (!tagMessages.isEmpty()) {
            errors.addError("label", "contains_xss");
        }

        StringBuilder descriptionMessages = new StringBuilder();
        formattedText.processFormattedText(description, descriptionMessages);
        if (!descriptionMessages.isEmpty()) {
            errors.addError("description", "contains_xss");
        }

        return errors;
    }

    public TagRecord toTag() {

        System.out.println("ID: " + collectionId);

        return TagRecordBuilder.builder()
            .id(id)
            .collectionId(collectionId)
            .label(label)
            .description(description)
            .createdBy(createdBy)
            .creationDate(creationDate)
            .lastModifiedBy(lastModifiedBy)
            .lastModificationDate(lastModificationDate)
            .externalId(externalId)
            .alternativeLabels(alternativeLabels)
            .externalCreation(externalCreation)
            .externalCreationDate(externalCreationDate)
            .externalUpdate(externalUpdate)
            .lastUpdateDateInExternalSystem(lastUpdateDateInExternalSystem)
            .parentId(parentId)
            .externalHierarchyCode(externalHierarchyCode)
            .externalType(externalType)
            .data(data)
            .collectionName(collectionName)
            .build();
    }
}
