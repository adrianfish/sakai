/******************************************************************************
 * Copyright 2023 sakaiproject.org Licensed under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.sakaiproject.webapi.controllers;

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.assignment.api.AssignmentConstants;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tags.api.TagCollectionRecord;
import org.sakaiproject.tags.api.TagRecord;
import org.sakaiproject.tags.api.TagService;
import org.sakaiproject.tags.api.exceptions.InvalidCollectionException;
import org.sakaiproject.webapi.exception.ForbiddenAccessException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class TagsController extends AbstractSakaiApiController {

	@Resource
	private TagService tagService;

	@Resource
	private SecurityService securityService;

	@Resource
	private ServerConfigurationService serverConfigurationService;

    private int maxPageSize;

    @PostConstruct
    public void init() {
        maxPageSize = serverConfigurationService.getInt("tagservice.maxpagesize", 100);
    }

	@GetMapping(value = "/tags/collections", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getAllCollections(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        if (!securityService.isSuperUser()) {
            // Only a superuser can see all the tag collections
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (pageSize > maxPageSize) {
            pageSize = maxPageSize;
        }

        int totalTagCollections = tagService.getTotalTagCollections();
        List<TagCollectionRecord> collections = tagService.getCollectionsPaginated(pageNum, pageSize);

        int totalPages = totalTagCollections > 0 ? (int) Math.ceil((double) totalTagCollections / (double) pageSize) : 0;

        Map<String, Object> data = Map.of(
            "pageSize", pageSize,
            "pageNum", pageNum,
            "totalPages", totalPages,
            "showPagination", totalTagCollections > 0 && totalPages > 1,
            "collections", collections,
            "tagserviceactive", tagService.getServiceActive(),
            "canCreate", securityService.isSuperUser()
        );
        
        return ResponseEntity.ok(data);
    }

	@PostMapping(value = "/tags/collections", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TagCollectionRecord> createCollection(@RequestBody TagCollectionRecord collection) {

        if (!securityService.isSuperUser()) {
            // Only a superuser can see all the tag collections
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TagCollectionRecord saved = tagService.createTagCollection(collection);

        return ResponseEntity.ok(saved);
    }

	@PutMapping(value = "/tags/collections/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TagCollectionRecord> updateCollection(@RequestBody TagCollectionRecord collection) {

        checkSakaiSession();

        System.out.println(collection);

        TagCollectionRecord saved = tagService.updateTagCollection(collection);

        return ResponseEntity.ok(saved);
    }

	@DeleteMapping(value = "/tags/collections/{collectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity deleteCollection(@PathVariable String collectionId) {

        checkSakaiSession();

        tagService.deleteTagCollection(collectionId);

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/tags/collections/{collectionId}/tags", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getTagsForCollection(
            @PathVariable String collectionId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        checkSakaiSession();

        if (pageSize > maxPageSize) {
            pageSize = maxPageSize;
        }

        int totalTags = tagService.getTotalTagsInCollection(collectionId);
        List<TagRecord> tags = tagService.getTagsPaginatedInCollection(pageNum, pageSize, collectionId);

        Map<String, Object> data = new HashMap<>();
        
        int totalPages = totalTags > 0 ? (int) Math.ceil((double) totalTags / (double) pageSize) : 0;
        
        data.put("pageSize", pageSize);
        data.put("pageNum", pageNum);
        data.put("totalPages", totalPages);
        data.put("showPagination", totalTags > 0 && totalPages > 1);
        data.put("tags", tags);
        data.put("tagserviceactive", tagService.getServiceActive());
        data.put("canCreate", securityService.isSuperUser());
        
        return ResponseEntity.ok(data);
    }

	@PostMapping(value = "/tags/collections/{collectionId}/tags", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> createTag(@RequestBody TagRecord tag) {

        if (!checkManageIfSite(tag.collectionId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to manage this site collection."));
        }

        try {
            return ResponseEntity.ok(Map.of("tag", tagService.createTag(tag)));
        } catch (InvalidCollectionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

	@PutMapping(value = "/tags/collections/{collectionId}/tags/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TagRecord> updateTag(@RequestBody TagRecord tag) {

        if (!checkManageIfSite(tag.collectionId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TagRecord saved = tagService.updateTag(tag);

        if (saved == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(saved);
    }

	@DeleteMapping(value = "/tags/collections/{collectionId}/tags/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity deleteTag(@PathVariable String collectionId, @PathVariable String id) {

        if (!checkManageIfSite(collectionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        tagService.deleteTag(id);

        return ResponseEntity.ok().build();
    }

	@GetMapping(value = "/sites/{siteId}/tools/{tool}/tags/{collectionId}/items/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Iterable<TagRecord> getTagsForItem(@PathVariable String siteId, @PathVariable String tool, @PathVariable String collectionId, @PathVariable String itemId) {
		checkSakaiSession();
		checkAccess(siteId, tool);

		return tagService.getAssociatedTagsForReference(collectionId, itemId);
	}
	
	@GetMapping(value = "/sites/{siteId}/tools/{tool}/tags/{collectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Iterable<TagRecord> getTagsForCollection(@PathVariable String siteId, @PathVariable String tool, @PathVariable String collectionId) {
		checkSakaiSession();
		checkAccess(siteId, tool);

		return tagService.getAllInCollection(collectionId);
	}

	private void checkAccess(String siteId, String tool) {
		if (StringUtils.isNotEmpty(siteId) && StringUtils.isNotEmpty(tool)) {
			if (securityService.unlock(TagService.TAGSERVICE_MANAGE_PERMISSION, "/site/" + siteId)) {
				return;
			}
			if (tool.equals(TagService.TOOL_ASSIGNMENTS)) {
				Site site = checkSite(siteId);
				ToolConfiguration tc = site.getToolForCommonId(AssignmentConstants.TOOL_ID);
				String optionTagsValue = tc.getPlacementConfig().getProperty(AssignmentConstants.SHOW_TAGS_STUDENT);
				if (Boolean.TRUE.equals(optionTagsValue) && securityService.unlock(SiteService.SITE_VISIT, "/site/" + siteId)) {
					return;
				}
			}
		}
		throw new ForbiddenAccessException();
	}

    /**
     * Checks if the supplied collectionId is a site and, if it is, checks if the user has
     * permission to manage the site . If the supplied collectionId is not a
     * site, returns true. If the user is a superuser, returns true.
     */
    private boolean checkManageIfSite(String collectionId) {

        if (securityService.isSuperUser()) {
            return true;
        }

        Optional<Site> optSite = siteService.getOptionalSite(collectionId);
        if (optSite.isPresent()) {
            return securityService.unlock(TagService.TAGSERVICE_MANAGE_PERMISSION, "/site/" + collectionId);
        } else {
            return true;
        }
    }
}
