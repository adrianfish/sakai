/**
 * Copyright (c) 2003-2017 The Apereo Foundation
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
package org.sakaiproject.pages.impl.test;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.pages.api.PageReferenceReckoner;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.PagesPermissionException;
import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.Permissions;
import org.sakaiproject.pages.impl.PagesServiceImpl;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.user.api.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.AopTestUtils;

import static org.mockito.Mockito.*;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PagesTestConfiguration.class})
public class PagesServiceTests extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired private PagesService pagesService;
    @Autowired private SecurityService securityService;
    @Autowired private ServerConfigurationService serverConfigurationService;
    @Autowired private SiteService siteService;

    private String instructor = "instructor";
    private User instructorUser = null;
    private String user1 = "user1";
    private User user1User = null;
    private String user2 = "user2";
    private User user2User = null;

    private String siteId = "playpen";
    private String siteReference = "/site/" + siteId;
    private String title = "eggs";
    private String content = "beans";

    @Before
    public void setup() {

        instructorUser = mock(User.class);
        when(instructorUser.getDisplayName()).thenReturn(instructor);

        user1User = mock(User.class);
        when(user1User.getDisplayName()).thenReturn(user1);

        user2User = mock(User.class);
        when(user2User.getDisplayName()).thenReturn(user2);

        when(siteService.siteReference(siteId)).thenReturn(siteReference);
        when(securityService.unlock(SiteService.SITE_VISIT, siteReference)).thenReturn(true);

        reset(securityService);
    }

    @Test
    public void savePage() throws PagesPermissionException {

        PageTransferBean pageBean = getPageTransferBean();

        assertNull(pageBean.id);

        assertThrows(PagesPermissionException.class, () -> pagesService.savePage(pageBean));

        when(securityService.unlock(Permissions.ADD_PAGE, siteReference)).thenReturn(true);

        PageTransferBean savedBean = pagesService.savePage(pageBean);

        assertNotNull(savedBean.id);
        assertEquals(pageBean.title, savedBean.title);
        assertEquals(pageBean.content, savedBean.content);
        assertEquals(pageBean.siteId, savedBean.siteId);

        when(securityService.unlock(SiteService.SITE_VISIT, siteReference)).thenReturn(true);
        Collection<PageTransferBean> pages = pagesService.getPagesForSite(siteId, true);
        assertEquals(1, pages.size());

        savedBean.content = "beans and sauce";
        PageTransferBean updatedBean = pagesService.savePage(savedBean);

        assertNotNull(updatedBean.id);
        assertEquals(savedBean.id, updatedBean.id);
        assertEquals(savedBean.title, updatedBean.title);
        assertEquals(savedBean.content, updatedBean.content);
        assertEquals(savedBean.siteId, updatedBean.siteId);

        pages = pagesService.getPagesForSite(siteId, true);
        assertEquals(1, pages.size());

        PageTransferBean returnedBean = pages.iterator().next();
        assertEquals(returnedBean.id, updatedBean.id);
        assertEquals(returnedBean.title, updatedBean.title);
        assertEquals(returnedBean.content, updatedBean.content);
        assertEquals(returnedBean.siteId, updatedBean.siteId);
    }

    @Test
    public void getPagesForSite() throws PagesPermissionException {

        when(securityService.unlock(Permissions.ADD_PAGE, siteReference)).thenReturn(true);

        PageTransferBean pageBean = getPageTransferBean();

        PageTransferBean savedBean = pagesService.savePage(pageBean);
        when(securityService.unlock(SiteService.SITE_VISIT, siteReference)).thenReturn(true);
        Collection<PageTransferBean> pages = pagesService.getPagesForSite(siteId, true);
        assertEquals(pageBean.content, pages.iterator().next().content);

        pages = pagesService.getPagesForSite(siteId, false);
        assertEquals("", pages.iterator().next().content);
    }

    @Test
    public void getPage() throws PagesPermissionException {

        when(securityService.unlock(Permissions.ADD_PAGE, siteReference)).thenReturn(true);

        PageTransferBean pageBean = getPageTransferBean();

        PageTransferBean savedBean = pagesService.savePage(pageBean);

        when(securityService.unlock(SiteService.SITE_VISIT, siteReference)).thenReturn(true);

        Optional<PageTransferBean> retrievedBeanOpt = pagesService.getPage(siteId, savedBean.id);
        assertTrue(retrievedBeanOpt.isPresent());

        assertEquals(pageBean.title, retrievedBeanOpt.get().title);
        assertEquals(pageBean.content, retrievedBeanOpt.get().content);

        retrievedBeanOpt = pagesService.getPage(siteId, "bob");
        assertFalse(retrievedBeanOpt.isPresent());
    }

    @Test
    public void deletePage() throws PagesPermissionException {

        when(securityService.unlock(Permissions.ADD_PAGE, siteReference)).thenReturn(true);

        PageTransferBean pageBean = getPageTransferBean();

        PageTransferBean savedBean = pagesService.savePage(pageBean);

        when(securityService.unlock(SiteService.SITE_VISIT, siteReference)).thenReturn(true);

        Collection<PageTransferBean> pages = pagesService.getPagesForSite(siteId, true);
        assertEquals(1, pages.size());

        assertThrows(PagesPermissionException.class, () -> pagesService.deletePage(siteId, savedBean.id));

        when(securityService.unlock(Permissions.DELETE_PAGE, siteReference)).thenReturn(true);

        pagesService.deletePage(siteId, savedBean.id);

        pages = pagesService.getPagesForSite(siteId, true);
        assertEquals(0, pages.size());
    }

    @Test
    public void getEntityUrl() throws PagesPermissionException {

        when(securityService.unlock(Permissions.ADD_PAGE, siteReference)).thenReturn(true);

        PageTransferBean pageBean = getPageTransferBean();

        PageTransferBean savedBean = pagesService.savePage(pageBean);

        String toolId = "my-pages";

        ToolConfiguration tc = mock(ToolConfiguration.class);
        when(tc.getId()).thenReturn(toolId);

        Site site = mock(Site.class);
        when(site.getToolForCommonId(PagesService.TOOL_ID)).thenReturn(tc);
        try {
            when(siteService.getSite(pageBean.siteId)).thenReturn(site);
        } catch (IdUnusedException iue) {
            fail("Failed to get site during mocking");
        }

        String ref = PageReferenceReckoner.reckoner().page(savedBean).reckon().toString();

        Reference reference = mock(Reference.class);
        when(reference.getReference()).thenReturn(ref);

        String portalUrl = "http://mysakai.com/portal";
        
        when(serverConfigurationService.getPortalUrl()).thenReturn(portalUrl);

        String testUrl = portalUrl + "/directtool/" + toolId + "/pages/" + savedBean.id;

        PagesServiceImpl impl = (PagesServiceImpl) AopTestUtils.getTargetObject(pagesService);

        Optional<String> url = impl.getEntityUrl(reference, Entity.UrlType.PORTAL);
        assertTrue(url.isPresent());
        assertEquals(testUrl, url.get());
    }

    private PageTransferBean getPageTransferBean() {

        PageTransferBean pageBean = new PageTransferBean();
        pageBean.title = title;
        pageBean.content = content;
        pageBean.siteId = siteId;
        return pageBean;
    }
}

