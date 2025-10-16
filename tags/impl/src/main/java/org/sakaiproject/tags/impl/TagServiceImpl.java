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

package org.sakaiproject.tags.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tags.api.I18n;
import org.sakaiproject.tags.api.TagCollectionRecord;
import org.sakaiproject.tags.api.TagCollectionRecordBuilder;
import org.sakaiproject.tags.api.TagRecord;
import org.sakaiproject.tags.api.TagService;
import org.sakaiproject.tags.api.exceptions.InvalidCollectionException;
import org.sakaiproject.tags.api.model.Tag;
import org.sakaiproject.tags.api.model.TagAssociation;
import org.sakaiproject.tags.api.model.TagCollection;
import org.sakaiproject.tags.api.repository.TagRepository;
import org.sakaiproject.tags.api.repository.TagAssociationRepository;
import org.sakaiproject.tags.api.repository.TagCollectionRepository;
import org.sakaiproject.tags.impl.common.SakaiI18n;
import org.sakaiproject.util.ResourceLoader;

/**
 * The implementation of the Tags Service service.  Provides system initialization
 * and access to the Tags Service and sub-services.
 */
@Slf4j
public class TagServiceImpl implements TagService {

    private static final String TAGSERVICE_MAXPAGESIZE =  "tagservice.maxpagesize";
    private static final String TAGSERVICE_ENABLED =  "tagservice.enabled";
    private static final Boolean TAGSERVICE_ENABLED_DEFAULT_VALUE =  true;
    private static final int TAGSERVICE_MAXPAGESIZE_DEFAULT_VALUE = 200;
    private static final int TAG_MAX_LABEL = 255;

    @Autowired
    private EventTrackingService eventTrackingService;

    @Autowired
    private FunctionManager functionManager;

    @Autowired
    private ServerConfigurationService serverConfigurationService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private TagAssociationRepository tagAssociationRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TagCollectionRepository tagCollectionRepository;

    @Autowired
    @Qualifier("org.sakaiproject.util.ResourceLoader.tagservice")
    @Setter
    private ResourceLoader resourceLoader;

    @Override
    public void init() {
        functionManager.registerFunction(TAGSERVICE_MANAGE_PERMISSION);
    }

    @Transactional
    private void saveTagAssociation(String reference, Tag tag) {

        TagAssociation tagAssociation = new TagAssociation();
        tagAssociation.setReference(reference);
        tagAssociation.setTag(tag);
        tagAssociationRepository.save(tagAssociation);
    }

    @Override
    public List<String> getTagAssociationIds(String collectionName, String reference) {

        return tagAssociationRepository.findByCollectionNameAndReference(collectionName, reference)
          .stream()
          .map(ta -> ta.getTag().getLabel())
          .collect(Collectors.toList());
    }

    @Override
    public List<TagRecord> getAssociatedTagsForReference(String collectionId, String reference) {

        return tagAssociationRepository.findByCollectionNameAndReference(collectionId, reference)
            .stream()
            .map(ta -> TagRecord.fromTag(ta.getTag()))
            .collect(Collectors.toList());
        /*
        List<String> tagIds = getTagAssociationIds(collectionId, reference);
        List<TagRecord> associatedTags = new ArrayList<>();
        for (String tagId : tagIds) {
            TagRecord t = getForId(tagId).orElse(null);
            if (t != null) {
                associatedTags.add(t);
            } else {
                log.warn("Associated tag with id {} does not exist anymore {}", tagId);
            }
        }
        */
        //return associatedTags;
    }

    @Override
    @Transactional
    public void updateTagAssociations(String collectionId, String reference, Collection<String> tagLabels, boolean isSite) {

        if (isSite && siteService.getOptionalSite(collectionId).isEmpty()) {
            log.warn("Site {} does not exist", collectionId);
            return;
        }

        // create collection if it doesn't exist
        TagCollection col = tagCollectionRepository.findByName(collectionId).orElseGet(() -> {

            String description = resourceLoader.getString("user_collection");
            String name = collectionId;
            if (isSite) {
                description = resourceLoader.getFormattedMessage("site_collection", collectionId);
            }

            TagCollectionRecord tcr = TagCollectionRecordBuilder.builder()
                .name(name)
                .description(description)
                .build();
            tcr = createTagCollection(tcr);
            return tagCollectionRepository.findByName(collectionId).orElse(null);
        });

        if (col == null) {
            log.warn("Collection {} does not exist", collectionId);
            return;
        }

        // Delete any previous associations
        tagAssociationRepository.deleteByReference(reference);

        tagLabels.forEach(label -> {

            Tag tag = tagRepository.findByLabelAndCollection(label, col).orElseGet(() -> {
                Tag t = new Tag();
                t.setTagCollection(col);
                t.setLabel(label);
                return tagRepository.save(t);
            });

            // save tag association
            saveTagAssociation(reference, tag);
        });
    }

    @Override
    public int getMaxPageSize() { return serverConfigurationService.getInt(TAGSERVICE_MAXPAGESIZE, TAGSERVICE_MAXPAGESIZE_DEFAULT_VALUE); }

    @Override
    public Boolean getServiceActive (){
        return serverConfigurationService.getBoolean(TAGSERVICE_ENABLED, TAGSERVICE_ENABLED_DEFAULT_VALUE);
    }

    @Override
    public void deleteTag(String tagId) {
        tagRepository.deleteById(tagId);
    }

    @Override
    public TagRecord updateTag(TagRecord tag) {

        return tagRepository.findById(tag.id()).map(t -> {
            return TagRecord.fromTag(tagRepository.save(tag.mergeInto(t)));
        }).orElseGet(() -> {
            log.warn("Tag with id {} does not exist", tag.id());
            return null;
        });
    }

    @Override
    public TagRecord createTag(TagRecord tr) throws InvalidCollectionException {

        return tagCollectionRepository.findById(tr.collectionId()).map(tc -> {

            Tag newTag = Tag.fromRecord(tr);
            newTag.setTagCollection(tc);
            return TagRecord.fromTag(tagRepository.save(newTag));
        }).orElseThrow(() -> new InvalidCollectionException("Collection " + tr.collectionId() + " does not exist"));
    }

    @Override
    public List<TagRecord> getAll() {
        return tagRepository.findAll().stream().map(TagRecord::fromTag).collect(Collectors.toList());
    }

    @Override
    public Optional<TagRecord> getForId(String tagId) {
        return tagRepository.findById(tagId).map(TagRecord::fromTag);
    }

    @Override
    public List<TagRecord> getTagsPaginatedInCollection(int pageNum, int pageSize, String collectionId) {
 
        // PageRequest assumes zero based page numbers, but the api has historically used one based page numbers
        return tagRepository.findByCollectionPaged(collectionId, PageRequest.of(pageNum - 1, pageSize))
            .stream()
            .map(TagRecord::fromTag)
            .collect(Collectors.toList());
    }

    @Override
    public int getTotalTagsInCollection(String collectionId) {
        return (int) tagRepository.countByCollection(collectionId);
    }

    @Override
    public int getTotalTagsByPrefixInLabel(String label) {
        return (int) tagRepository.countByLabelLike(label + "%");
    }

    @Override
    public Optional<TagRecord> getForExternalIdAndCollection(String externalId,String collectionId) {

        return tagRepository.findByExternalIdAndCollection(externalId, collectionId)
            .stream()
            .findAny()
            .map(TagRecord::fromTag);
    }

    @Override
    public List<TagRecord> getAllInCollection(String tagCollectionId) {

        return tagRepository.findByCollection(tagCollectionId)
            .stream()
            .map(TagRecord::fromTag)
            .collect(Collectors.toList());
    }

    @Override
    public List<TagRecord> getTagsByExactLabel(String label) {

        return tagRepository.findByLabel(label)
            .stream()
            .map(TagRecord::fromTag)
            .collect(Collectors.toList());
    }

    @Override
    public List<TagRecord> getTagsByPartialLabel(String label) {

        return tagRepository.findByLabelLike("%"+ label + "%")
            .stream()
            .map(TagRecord::fromTag)
            .collect(Collectors.toList());
    }

    @Override
    public List<TagRecord> getTagsByPrefixInLabel(String label) {

        return tagRepository.findByLabelLike(label + "%")
            .stream()
            .map(TagRecord::fromTag)
            .collect(Collectors.toList());
    }

    @Override
    public List<TagRecord> getTagsPaginatedByPrefixInLabel(int pageNum, int pageSize, String label) {

        // PageRequest assumes zero based page numbers, but the api has historically used one based page numbers
        return tagRepository.findByLabelLikePaged(label + "%", PageRequest.of(pageNum - 1, pageSize))
            .stream()
            .map(TagRecord::fromTag)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTagsOlderThanDateFromCollection(String tagCollectionId, long lastModificationDate ) {

        List<String> idsToEventOn
            = tagRepository.findByCollectionAndLastModificationDateLessThan(tagCollectionId, lastModificationDate)
                .stream().map(Tag::getId).collect(Collectors.toList());

        tagRepository.deleteByCollectionAndLastModificationDateLessThan(tagCollectionId, lastModificationDate);

        idsToEventOn.forEach(this::postTagDeleteEvent);
    }

    @Override
    public void deleteTagFromExternalCollection(String externalId, String tagCollectionId ) {

        List<String> idsToEventOn
            = tagRepository.findByExternalIdAndCollection(externalId, tagCollectionId)
                .stream().map(Tag::getId).collect(Collectors.toList());

        tagRepository.deleteByExternalIdAndCollection(externalId, tagCollectionId);

        idsToEventOn.forEach(this::postTagDeleteEvent);
    }

    @Override
    @Transactional
    public TagCollectionRecord createTagCollection(TagCollectionRecord tcr) {

        tcr = TagCollectionRecord.fromTagCollection(tagCollectionRepository.save(TagCollection.fromRecord(tcr)));
        //eventTrackingService.post(eventTrackingService.newEvent("tags.new.collection", "/tagcollections/" + tcr.id(), true));
        return tcr;
    }

    @Override
    public TagCollectionRecord updateTagCollection(TagCollectionRecord tcr) {

        return tagCollectionRepository.findById(tcr.id()).map(tc -> {

            tagCollectionRepository.save(tcr.mergeInto(tc));
            eventTrackingService.post(eventTrackingService.newEvent("tags.update.collection",
                    "/tagcollections/" + tcr.id(), true));
            return tcr;
        }).orElseGet(() -> {

            log.warn("Tag collection with id {} does not exist", tcr.id());
            return null;
        });
    }

    @Override
    public void deleteTagCollection(String collectionId) {

        tagCollectionRepository.deleteById(collectionId);
        eventTrackingService.post(eventTrackingService.newEvent("tags.delete.collection", "/tagcollections/" + collectionId, true));
    }

    @Override
    public List<TagCollectionRecord> getAllCollections() {

        return tagCollectionRepository.findAllOrderByName()
            .stream().map(TagCollectionRecord::fromTagCollection).collect(Collectors.toList());
    }

    @Override
    public Optional<TagCollectionRecord> getCollectionForId(String collectionId) {
        return tagCollectionRepository.findById(collectionId).map(TagCollectionRecord::fromTagCollection);
    }

    @Override
    public Optional<TagCollectionRecord> getCollectionForName(String name) {
        return tagCollectionRepository.findByName(name).map(TagCollectionRecord::fromTagCollection);
    }

    @Override
    public Optional<TagCollectionRecord> getCollectionForExternalSourceName(String externalSourceName) {
        return tagCollectionRepository.findByExternalSourceName(externalSourceName).map(TagCollectionRecord::fromTagCollection);
    }

    @Override
    public List<TagCollectionRecord> getCollectionsPaginated(int pageNum, int pageSize) {

        // PageRequest assumes zero based page numbers, but the api has historically used one based page numbers
        return tagCollectionRepository.findAllPaginatedOrderByName(PageRequest.of(pageNum - 1, pageSize))
            .stream().map(TagCollectionRecord::fromTagCollection).collect(Collectors.toList());
    }

    @Override
    public int getTotalTagCollections() {
        return (int) tagCollectionRepository.count();
    }

    private void postTagDeleteEvent(String tagId) {
        eventTrackingService.post(eventTrackingService.newEvent("tags.delete.tag", "/tags/" + tagId, true));
    }
}
