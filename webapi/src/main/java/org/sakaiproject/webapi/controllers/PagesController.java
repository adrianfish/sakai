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

import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.PagesPermissionException;
import org.sakaiproject.pages.api.PagesService;

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

/**
 */
@Slf4j
@RestController
public class PagesController extends AbstractSakaiApiController {

	@Autowired
	private PagesService pagesService;

	@GetMapping(value = "/sites/{siteId}/pages", produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityModel<PagesRestBean> getSitePages(@PathVariable String siteId) {

		String currentUserId = checkSakaiSession().getUserId();

        PagesRestBean pagesRestBean = new PagesRestBean();
        pagesRestBean.siteId = siteId;
        pagesRestBean.userId = currentUserId;
        pagesRestBean.pages = pagesService.getPagesForSite(siteId, /* populate */ false);

        List<Link> links = new ArrayList<>();
        links.add(Link.of("/api/sites/" + siteId + "/pages", "addPage"));
        return EntityModel.of(pagesRestBean, links);
    }

    @PostMapping(value = "/sites/{siteId}/pages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageTransferBean> createPage(@RequestBody PageTransferBean pageTransferBean) {

        checkSakaiSession();

        try {
            return ResponseEntity.ok(pagesService.savePage(pageTransferBean));
        } catch (PagesPermissionException ppe) {
            log.error("createPage rest endpoint accessed without permission: {}", ppe.toString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

	@GetMapping(value = "/sites/{siteId}/pages/{pageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageTransferBean> getSitePage(@PathVariable String siteId, @PathVariable String pageId) {

        checkSakaiSession();

        Optional<PageTransferBean> optBean = pagesService.getPage(siteId, pageId);

        if (!optBean.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(optBean.get());
    }

    @PutMapping(value = "/sites/{siteId}/pages/{pageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageTransferBean> updateSitePage(@PathVariable String siteId, @PathVariable String pageId, @RequestBody PageTransferBean pageTransferBean) {

        checkSakaiSession();

        try {
            return ResponseEntity.ok(pagesService.savePage(pageTransferBean));
        } catch (PagesPermissionException ppe) {
            log.error("updateSitePage rest endpoint accessed without permission: {}", ppe.toString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping(value = "/sites/{siteId}/pages/{pageId}")
    public ResponseEntity deleteSitePage(@PathVariable String siteId, @PathVariable String pageId) {

        checkSakaiSession();

        try {
            pagesService.deletePage(pageId, siteId);
            return ResponseEntity.ok().build();
        } catch (PagesPermissionException ppe) {
            log.error("updateSitePage rest endpoint accessed without permission: {}", ppe.toString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /*
    private EntityModel entityModelForPageTransferBean(PageTransferBean pageBean) {

        List<Link> links = new ArrayList<>();
        links.add(Link.of(pageBean.url, "self"));
    }
    */
}
