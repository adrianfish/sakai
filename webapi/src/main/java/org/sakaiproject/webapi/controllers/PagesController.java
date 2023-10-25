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
import org.sakaiproject.pages.api.PagesService;

import org.sakaiproject.webapi.beans.PagesRestBean;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
@RestController
public class PagesController extends AbstractSakaiApiController {

	//@Resource
	//private PagesService pagesService;

	@GetMapping(value = "/sites/{siteId}/pages", produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityModel<PagesRestBean> getSitePages(@PathVariable String siteId) {

		String currentUserId = checkSakaiSession().getUserId();

        PagesRestBean pagesRestBean = new PagesRestBean();
        pagesRestBean.siteId = siteId;
        pagesRestBean.userId = currentUserId;

        List<Link> links = new ArrayList<>();
        links.add(Link.of("/api/sites/" + siteId + "/pages", "addPage"));
        return EntityModel.of(pagesRestBean, links);
    }

    @PostMapping(value = "/sites/{siteId}/pages", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageTransferBean createPage(@RequestBody PageTransferBean pageTransferBean) {

        System.out.println(pageTransferBean.title);
        System.out.println(pageTransferBean.content);

        checkSakaiSession();
        return null;
    }

    /*
    private EntityModel entityModelForPageTransferBean(PageTransferBean pageBean) {

        List<Link> links = new ArrayList<>();
        links.add(Link.of(pageBean.url, "self"));
    }
    */
}
