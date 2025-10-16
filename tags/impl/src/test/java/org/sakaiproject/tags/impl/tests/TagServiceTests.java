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
package org.sakaiproject.tags.impl.tests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.AopTestUtils;

import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tags.api.TagCollectionRecord;
import org.sakaiproject.tags.api.TagCollectionRecordBuilder;
import org.sakaiproject.tags.api.TagRecord;
import org.sakaiproject.tags.api.TagRecordBuilder;
import org.sakaiproject.tags.api.TagService;
import org.sakaiproject.tags.api.exceptions.InvalidCollectionException;
import org.sakaiproject.tags.impl.TagServiceImpl;
import org.sakaiproject.tags.api.model.Tag;
import org.sakaiproject.tags.api.model.TagCollection;
import org.sakaiproject.util.ResourceLoader;

import lombok.extern.slf4j.Slf4j;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TagServiceTestConfiguration.class})
public class TagServiceTests extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private SiteService siteService;

    @Autowired
    private SqlService sqlService;

    @Autowired
    private TagService tagService;

    private ResourceLoader resourceLoader;

    @Before
    public void init() {

        resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getLocale()).thenReturn(Locale.ENGLISH);
        ((TagServiceImpl) AopTestUtils.getTargetObject(tagService)).setResourceLoader(resourceLoader);
    }

    @Test
    public void crudTag() throws InvalidCollectionException {

        TagCollectionRecord tagCollection = TagCollectionRecordBuilder.builder().name("test").build();
        tagCollection = tagService.createTagCollection(tagCollection);

        String tagLabel = "tag";

        TagRecord badTag = TagRecordBuilder.builder().collectionId("invalid").label(tagLabel).build();
        assertThrows(InvalidCollectionException.class, () -> tagService.createTag(badTag));

        TagRecord tag = TagRecordBuilder.builder().collectionId(tagCollection.id()).label(tagLabel).build();
        TagRecord createdTag = tagService.createTag(tag);
        assertNotNull(createdTag.id());
        assertEquals(tagLabel, createdTag.label());

        assertTrue(tagService.getForId(createdTag.id()).isPresent());

        List<TagRecord> tags = tagService.getAll();
        assertEquals(1, tags.size());
        assertEquals(createdTag.id(), tags.get(0).id());

        String newTagLabel = "new tag";
        TagRecord updatedTag = createdTag.with().label(newTagLabel).build();
        updatedTag = tagService.updateTag(updatedTag);

        Optional<TagRecord> optTag = tagService.getForId(updatedTag.id());
        assertTrue(optTag.isPresent());
        assertEquals(newTagLabel, optTag.get().label());

        tagService.deleteTag(updatedTag.id());
        assertFalse(tagService.getForId(updatedTag.id()).isPresent());
    }

    @Test
    public void getTagsByLabel() throws InvalidCollectionException {

        TagCollectionRecord bananas = TagCollectionRecordBuilder.builder().name("bananas").build();
        bananas = tagService.createTagCollection(bananas);

        TagCollectionRecord fish = TagCollectionRecordBuilder.builder().name("fish").build();
        fish = tagService.createTagCollection(fish);

        String label1 = "Cape Verde";
        TagRecord tag1 = TagRecordBuilder.builder().collectionId(bananas.id()).label(label1).build();
        tagService.createTag(tag1);

        TagRecord tag2 = TagRecordBuilder.builder().collectionId(fish.id()).label(label1).build();
        tagService.createTag(tag2);

        String label2 = "Dominican Republic";
        TagRecord tag3 = TagRecordBuilder.builder().collectionId(bananas.id()).label(label2).build();
        tagService.createTag(tag3);

        List<TagRecord> tags = tagService.getTagsByExactLabel(label1);
        assertEquals(2, tags.size());

        tags = tagService.getTagsByPartialLabel("%Repub%");
        assertEquals(1, tags.size());

        tags = tagService.getTagsByPrefixInLabel("Cap");
        assertEquals(2, tags.size());

        int totalTags = tagService.getTotalTagsByPrefixInLabel("Cap");
        assertEquals(2, totalTags);
    }

    @Test
    public void collectionMethods() throws InvalidCollectionException {

        TagCollectionRecord fruitsCollection = TagCollectionRecordBuilder.builder().name("fruits").build();
        fruitsCollection = tagService.createTagCollection(fruitsCollection);

        TagCollectionRecord mammalsCollection = TagCollectionRecordBuilder.builder().name("mammals").build();
        mammalsCollection = tagService.createTagCollection(mammalsCollection);

        String appleLabel = "apple";
        TagRecord appleTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).label(appleLabel).build();
        tagService.createTag(appleTag);

        String orangeLabel = "orange";
        TagRecord orangeTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).label(orangeLabel).build();
        tagService.createTag(orangeTag);

        List<TagRecord> tags = tagService.getAllInCollection(mammalsCollection.id());
        assertTrue(tags.isEmpty());

        TagRecord batTag = TagRecordBuilder.builder().collectionId(mammalsCollection.id()).label("bat").build();
        tagService.createTag(batTag);

        tags = tagService.getAllInCollection(fruitsCollection.id());
        assertEquals(2, tags.size());
        assertTrue(tags.stream().filter(t -> t.label().equals(appleLabel)).count() == 1L);
        assertTrue(tags.stream().filter(t -> t.label().equals(orangeLabel)).count() == 1L);

        assertTrue(tagService.getTotalTagsInCollection(fruitsCollection.id()) == 2);
    }

    @Test
    public void getTagsPaginatedInCollection() throws InvalidCollectionException {

        TagCollectionRecord fruitsCollection = TagCollectionRecordBuilder.builder().name("fruits").build();
        fruitsCollection = tagService.createTagCollection(fruitsCollection);

        List<TagRecord> tags = tagService.getTagsPaginatedInCollection(1, 2, fruitsCollection.id());
        assertEquals(0, tags.size());

        String appleLabel = "apple";
        TagRecord appleTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).label(appleLabel).build();
        tagService.createTag(appleTag);

        String orangeLabel = "orange";
        TagRecord orangeTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).label(orangeLabel).build();
        tagService.createTag(orangeTag);

        String bananaLabel = "banana";
        TagRecord bananaTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).label(bananaLabel).build();
        tagService.createTag(bananaTag);

        String pineappleLabel = "pineapple";
        TagRecord pineappleTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).label(pineappleLabel).build();
        tagService.createTag(pineappleTag);

        String mangoLabel = "mango";
        TagRecord mangoTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).label(mangoLabel).build();
        tagService.createTag(mangoTag);

        tags = tagService.getTagsPaginatedInCollection(1, 2, fruitsCollection.id());
        assertEquals(2, tags.size());

        assertEquals(appleLabel, tags.get(0).label());
        assertEquals(orangeLabel, tags.get(1).label());

        tags = tagService.getTagsPaginatedInCollection(2, 2, fruitsCollection.id());
        assertEquals(2, tags.size());
        assertEquals(bananaLabel, tags.get(0).label());
        assertEquals(pineappleLabel, tags.get(1).label());
    }

    @Test
    public void getTagsPaginatedByPrefixInLabel() throws InvalidCollectionException {

        TagCollectionRecord fruitsCollection = TagCollectionRecordBuilder.builder().name("fruits").build();
        fruitsCollection = tagService.createTagCollection(fruitsCollection);

        String pineLabel = "pine";
        TagRecord pineTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).label(pineLabel).build();
        tagService.createTag(pineTag);

        String pineappleLabel = "pineapple";
        TagRecord pineappleTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).label(pineappleLabel).build();
        tagService.createTag(pineappleTag);

        TagCollectionRecord defaultCollection = TagCollectionRecordBuilder.builder().name("default").build();
        defaultCollection = tagService.createTagCollection(defaultCollection);

        String pineasLabel = "pineas";
        TagRecord pineasTag = TagRecordBuilder.builder().collectionId(defaultCollection.id()).label(pineasLabel).build();
        tagService.createTag(pineasTag);

        List<TagRecord> tags = tagService.getTagsPaginatedByPrefixInLabel(1, 2, "pinea");
        assertEquals(2, tags.size());

        tags = tagService.getTagsPaginatedByPrefixInLabel(1, 2, "pine");
        assertEquals(2, tags.size());

        tags = tagService.getTagsPaginatedByPrefixInLabel(2, 2, "pine");
        assertEquals(1, tags.size());
    }

    public void getForExternalIdAndCollection() throws InvalidCollectionException {

        String externalId = "external_fruit";

        TagCollectionRecord fruitsCollection = TagCollectionRecordBuilder.builder().name("fruits").build();
        fruitsCollection = tagService.createTagCollection(fruitsCollection);

        String pineExternalId = "pine";
        TagRecord pineTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).externalId(pineExternalId).build();
        tagService.createTag(pineTag);

        String pineappleExternalId = "pineapple";
        TagRecord pineappleTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).externalId(pineappleExternalId).build();
        tagService.createTag(pineappleTag);

        Optional<TagRecord> optTag = tagService.getForExternalIdAndCollection(pineExternalId, fruitsCollection.id());
        assertTrue(optTag.isPresent());
        assertEquals(pineExternalId, optTag.get().externalId());
    }

    public void testDeleteTagsOlderThanDateFromCollection() throws InvalidCollectionException {

        String collectionId = "fruits";

        TagCollectionRecord fruitsCollection = TagCollectionRecordBuilder.builder().name("fruits").build();
        fruitsCollection = tagService.createTagCollection(fruitsCollection);

        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        TagRecord appleTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).lastModificationDate(twoDaysAgo.toEpochMilli()).build();
        tagService.createTag(appleTag);

        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        TagRecord pineappleTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).lastModificationDate(oneDayAgo.toEpochMilli()).build();
        tagService.createTag(pineappleTag);

        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        TagRecord orangeTag = TagRecordBuilder.builder().collectionId(fruitsCollection.id()).lastModificationDate(oneHourAgo.toEpochMilli()).build();
        tagService.createTag(orangeTag);

        List<TagRecord> tags = tagService.getAllInCollection(fruitsCollection.id());
        assertEquals(3, tags.size());

        tagService.deleteTagsOlderThanDateFromCollection(fruitsCollection.id(), oneDayAgo.toEpochMilli());
        tags = tagService.getAllInCollection(collectionId);
        assertEquals(2, tags.size());

        tagService.deleteTagsOlderThanDateFromCollection(fruitsCollection.id(), oneHourAgo.toEpochMilli());
        tags = tagService.getAllInCollection(fruitsCollection.id());
        assertEquals(1, tags.size());
    }

    @Test
    public void crudTagCollection() {

        String name = "test";
        String externalSourceName = "test2";
        TagCollectionRecord tagCollection = TagCollectionRecordBuilder.builder().name(name).externalSourceName(externalSourceName).build();
        tagCollection = tagService.createTagCollection(tagCollection);

        String createdId = tagCollection.id();

        Optional<TagCollectionRecord> createdTagCollectionOpt = tagService.getCollectionForId(tagCollection.id());
        assertTrue(createdTagCollectionOpt.isPresent());
        assertEquals(name, createdTagCollectionOpt.get().name());

        createdTagCollectionOpt = tagService.getCollectionForName(name);
        assertTrue(createdTagCollectionOpt.isPresent());
        assertEquals(createdId, tagCollection.id());

        createdTagCollectionOpt = tagService.getCollectionForExternalSourceName(externalSourceName);
        assertTrue(createdTagCollectionOpt.isPresent());
        assertEquals(createdId, tagCollection.id());

        String test2 = "test2";
        TagCollectionRecord updatedTagCollection = createdTagCollectionOpt.get().with().name(test2).build();

        updatedTagCollection = tagService.updateTagCollection(updatedTagCollection);

        createdTagCollectionOpt = tagService.getCollectionForId(updatedTagCollection.id());
        assertTrue(createdTagCollectionOpt.isPresent());
        assertEquals(test2, createdTagCollectionOpt.get().name());

        tagService.deleteTagCollection(createdId);
        createdTagCollectionOpt = tagService.getCollectionForId(createdId);
        assertFalse(createdTagCollectionOpt.isPresent());
    }

    @Test
    public void getAllCollections() {

        String collectionName1 = "collection1";
        TagCollectionRecord tagCollection1 = TagCollectionRecordBuilder.builder().name(collectionName1).build();
        tagService.createTagCollection(tagCollection1);

        String collectionName2 = "collection2";
        TagCollectionRecord tagCollection2 = TagCollectionRecordBuilder.builder().name(collectionName2).build();
        tagService.createTagCollection(tagCollection2);

        List<TagCollectionRecord> tagCollections = tagService.getAllCollections();
        assertEquals(2, tagCollections.size());
        assertEquals(collectionName1, tagCollections.get(0).name());
        assertEquals(collectionName2, tagCollections.get(1).name());
    }

    @Test
    public void getCollectionsPaginated() {

        String collectionName1 = "collection1";
        TagCollectionRecord tagCollection1 = TagCollectionRecordBuilder.builder().name(collectionName1).build();
        tagService.createTagCollection(tagCollection1);

        String collectionName2 = "collection2";
        TagCollectionRecord tagCollection2 = TagCollectionRecordBuilder.builder().name(collectionName2).build();
        tagService.createTagCollection(tagCollection2);

        String collectionName3 = "collection3";
        TagCollectionRecord tagCollection3 = TagCollectionRecordBuilder.builder().name(collectionName3).build();
        tagService.createTagCollection(tagCollection3);

        String collectionName4 = "collection4";
        TagCollectionRecord tagCollection4 = TagCollectionRecordBuilder.builder().name(collectionName4).build();
        tagService.createTagCollection(tagCollection4);

        String collectionName5 = "collection5";
        TagCollectionRecord tagCollection5 = TagCollectionRecordBuilder.builder().name(collectionName5).build();
        tagService.createTagCollection(tagCollection5);

        List<TagCollectionRecord> tagCollections = tagService.getCollectionsPaginated(1, 2);
        assertEquals(2, tagCollections.size());
        assertEquals(collectionName1, tagCollections.get(0).name());
        assertEquals(collectionName2, tagCollections.get(1).name());

        tagCollections = tagService.getCollectionsPaginated(2, 2);
        assertEquals(2, tagCollections.size());
        assertEquals(collectionName3, tagCollections.get(0).name());
        assertEquals(collectionName4, tagCollections.get(1).name());

        tagCollections = tagService.getCollectionsPaginated(3, 2);
        assertEquals(1, tagCollections.size());
        assertEquals(collectionName5, tagCollections.get(0).name());
    }

    @Test
    public void updateTagAssociations() throws InvalidCollectionException {

        String siteId = "site1";
        String siteTitle = "Site 1";

        when(resourceLoader.getString("user_collection")).thenReturn("User Collection");
        when(resourceLoader.getFormattedMessage("site_collection", siteId)).thenReturn("Site Collection");

        Site site = mock(Site.class);
        when(site.getTitle()).thenReturn(siteTitle);
        when(siteService.getOptionalSite(siteId)).thenReturn(Optional.of(site));
        String reference = "/items/1";

        String tag1Label = "Tag 1";
        TagRecord tag1 = TagRecordBuilder.builder().collectionId(siteId).label(tag1Label).build();

        String tag2Label = "Tag 2";

        tagService.updateTagAssociations(siteId, reference, List.of(tag1Label, tag2Label), true);

        List<TagRecord> tags = tagService.getAssociatedTagsForReference(siteId, reference);
        assertEquals(2, tags.size());
    }
}

