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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sakaiproject.tags.api.exceptions.InvalidCollectionException;
import org.sakaiproject.tags.api.model.Tag;
import org.sakaiproject.tags.api.model.TagCollection;

/**
 * The interface for the tags service.
 */
public interface TagService {

    static final String TAGSERVICE_MANAGE_PERMISSION = "tagservice.manage";
    static final String TOOL_ASSIGNMENTS = "assignments";
    static final String TOOL_PRIVATE_MESSAGES = "privatemessages";
    
    static final String TAGSERVICE_ENABLED_INTEGRATION_PROP = "tagservice.enable.integrations";
    static final boolean TAGSERVICE_ENABLED_INTEGRATION_DEFAULT = true;

    void init();

    TagRecord createTag(TagRecord tr) throws InvalidCollectionException;

    TagRecord updateTag(TagRecord tag);

    void deleteTag(String tagId);

    List<TagRecord> getAll();

    Optional<TagRecord> getForId(String tagId);

    List<TagRecord> getTagsPaginatedInCollection(int pageNum, int pageSize, String tagcollectionid);

    int getTotalTagsInCollection(String tagcollectionid);

    int getTotalTagsByPrefixInLabel(String label);

    Optional<TagRecord> getForExternalIdAndCollection(String tagExternalId,String tagCollectionId);

    List<TagRecord> getAllInCollection(String tagCollectionId);

    List<TagRecord> getTagsByExactLabel(String label);

    List<TagRecord> getTagsByPartialLabel(String label);

    List<TagRecord> getTagsByPrefixInLabel(String label);

    List<TagRecord> getTagsPaginatedByPrefixInLabel(int pageNum, int pageSize, String label);

    void deleteTagsOlderThanDateFromCollection(String tagCollectionId, long lastmodificationdate );

    void deleteTagFromExternalCollection(String externalId, String tagCollectionId );

    TagCollectionRecord createTagCollection(TagCollectionRecord tagCollection);

    TagCollectionRecord updateTagCollection(TagCollectionRecord tagCollection);

    void deleteTagCollection(String tagCollectionId);

    List<TagCollectionRecord> getAllCollections();

    Optional<TagCollectionRecord> getCollectionForId(String tagCollectionId);

    Optional<TagCollectionRecord> getCollectionForName(String tagCollectionId);

    Optional<TagCollectionRecord> getCollectionForExternalSourceName(String externalSourceName);

    List<TagCollectionRecord> getCollectionsPaginated(int pageNum, int pageSize);

    int getTotalTagCollections();

    /**
     * Return if the service is enabled or not.
     */
    Boolean getServiceActive ();

    /**
     * Return the max size of the pages
     */
    int getMaxPageSize();

    List<String> getTagAssociationIds(String collectionId, String itemId);
    List<TagRecord> getAssociatedTagsForReference(String collectionId, String reference);
    void updateTagAssociations(String collectionId, String reference, Collection<String> tagLabels, boolean isSite);
}
