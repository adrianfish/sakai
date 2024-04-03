/******************************************************************************
 * Copyright 2015 sakaiproject.org Licensed under the Educational
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

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.PagesPermissionException;
import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.Permissions;
import org.sakaiproject.site.api.SiteService;

import org.sakaiproject.webapi.beans.PagesRestBean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class PagesController extends AbstractSakaiApiController {

	@Autowired
	private PagesService pagesService;

	@Autowired
	private SiteService siteService;

	@Autowired
	private SecurityService securityService;

	@GetMapping(value = "/sites/{siteId}/pages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagesRestBean> getSitePages(@PathVariable String siteId) {

		String currentUserId = checkSakaiSession().getUserId();

        PagesRestBean pagesRestBean = new PagesRestBean();
        pagesRestBean.siteId = siteId;
        pagesRestBean.userId = currentUserId;

        try {
            pagesRestBean.pages = pagesService.getPagesForSite(siteId, /* populate */ false);
        } catch (PagesPermissionException ppe) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (securityService.unlock(Permissions.ADD_PAGE, siteService.siteReference(siteId))) {
            pagesRestBean.links.put("addPage", "/api/sites/" + siteId + "/pages");
        }

        pagesRestBean.pages.forEach(this::addLinks);

        return ResponseEntity.ok(pagesRestBean);
    }

    @PostMapping(value = "/sites/{siteId}/pages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageTransferBean> createPage(@RequestBody PageTransferBean pageTransferBean) {

        checkSakaiSession();

        try {
            return ResponseEntity.ok(addLinks(pagesService.savePage(pageTransferBean)));
        } catch (PagesPermissionException ppe) {
            log.error("createPage rest endpoint accessed without permission: {}", ppe.toString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

	@GetMapping(value = "/sites/{siteId}/pages/{pageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageTransferBean> getSitePage(@PathVariable String siteId, @PathVariable String pageId) {

        checkSakaiSession();

        try {
            return pagesService.getPage(siteId, pageId).map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (PagesPermissionException ppe) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PutMapping(value = "/sites/{siteId}/pages/{pageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageTransferBean> updateSitePage(@PathVariable String siteId, @PathVariable String pageId, @RequestBody PageTransferBean pageTransferBean) {

        checkSakaiSession();

        try {
            return ResponseEntity.ok(addLinks(pagesService.savePage(pageTransferBean)));
        } catch (PagesPermissionException ppe) {
            log.error("updateSitePage rest endpoint accessed without permission: {}", ppe.toString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping(value = "/sites/{siteId}/pages/{pageId}")
    public ResponseEntity deleteSitePage(@PathVariable String siteId, @PathVariable String pageId) {

        checkSakaiSession();

        try {
            pagesService.deletePage(siteId, pageId);
            return ResponseEntity.ok().build();
        } catch (PagesPermissionException ppe) {
            log.error("updateSitePage rest endpoint accessed without permission: {}", ppe.toString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private PageTransferBean addLinks(PageTransferBean page) {

        if (page.canDelete) {
            page.links.put("deletePage", "/api/sites/" + page.siteId + "/pages/" + page.id);
        }

        if (page.canEdit) {
            page.links.put("editPage", "/api/sites/" + page.siteId + "/pages/" + page.id);
        }

        return page;

    }
}
