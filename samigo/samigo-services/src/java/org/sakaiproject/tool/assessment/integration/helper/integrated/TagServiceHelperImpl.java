/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.tool.assessment.integration.helper.integrated;

import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.tags.api.MissingUuidException;
import org.sakaiproject.tags.api.TagService;
import org.sakaiproject.tags.api.TagCollectionRecord;
import org.sakaiproject.tags.api.TagRecord;
import org.sakaiproject.tool.assessment.integration.helper.ifc.TagEventHandler;
import org.sakaiproject.tool.assessment.integration.helper.ifc.TagServiceHelper;

import java.util.Optional;

public class TagServiceHelperImpl implements TagServiceHelper {

    private NotificationService notificationService;
    private TagService tagService;

    @Override
    public void registerTagEventHandler(TagEventHandler tagEventHandler) {
        tagEventHandler.registerEventCallbacks(notificationService);
    }

    @Override
    public Optional<TagView> findTagById(String id) {
        Optional<TagRecord> results = tagService.getForId(id);
        if ( results.isPresent() ) {
            TagRecord tag = results.get();
                TagView tagView = new TagView(tag.id(), tag.label(), tag.collectionId(), tag.collectionName());
                return Optional.of(tagView);

        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<TagCollectionView> findTagCollectionById(String id) {
        final Optional<TagCollectionRecord> results = tagService.getCollectionForId(id);
        if ( results.isPresent() ) {
            TagCollectionRecord tagCollection = results.get();
            TagCollectionView tagView = new TagCollectionView(tagCollection.id(), tagCollection.name());
            return Optional.of(tagView);
        } else {
            return Optional.empty();
        }
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setTagService(TagService tagService) {
        this.tagService = tagService;
    }
}
