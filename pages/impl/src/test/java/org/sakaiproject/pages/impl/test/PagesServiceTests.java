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

import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.user.api.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.*;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
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

    private String instructor = "instructor";
    private User instructorUser = null;
    private String user1 = "user1";
    private User user1User = null;
    private String user2 = "user2";
    private User user2User = null;

    private String siteId = "playpen";
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
    }

    @Test
    public void savePage() {

        PageTransferBean pageBean = getPageTransferBean();

        assertNull(pageBean.id);

        PageTransferBean savedBean = pagesService.savePage(pageBean);

        assertNotNull(savedBean.id);
        assertEquals(pageBean.title, savedBean.title);
        assertEquals(pageBean.content, savedBean.content);
        assertEquals(pageBean.siteId, savedBean.siteId);

        List<PageTransferBean> pages = pagesService.getPagesForSite(siteId, true);
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

        PageTransferBean returnedBean = pages.get(0);
        assertEquals(returnedBean.id, updatedBean.id);
        assertEquals(returnedBean.title, updatedBean.title);
        assertEquals(returnedBean.content, updatedBean.content);
        assertEquals(returnedBean.siteId, updatedBean.siteId);
    }

    @Test
    public void getPagesForSite() {

        PageTransferBean pageBean = getPageTransferBean();

        PageTransferBean savedBean = pagesService.savePage(pageBean);
        List<PageTransferBean> pages = pagesService.getPagesForSite(siteId, true);
        assertEquals(pageBean.content, pages.get(0).content);

        pages = pagesService.getPagesForSite(siteId, false);
        assertEquals("", pages.get(0).content);
    }

    @Test
    public void getPage() {

        PageTransferBean pageBean = getPageTransferBean();

        PageTransferBean savedBean = pagesService.savePage(pageBean);

        Optional<PageTransferBean> retrievedBeanOpt = pagesService.getPage(siteId, savedBean.id);
        assertTrue(retrievedBeanOpt.isPresent());

        assertEquals(pageBean.title, retrievedBeanOpt.get().title);
        assertEquals(pageBean.content, retrievedBeanOpt.get().content);

        retrievedBeanOpt = pagesService.getPage(siteId, "bob");
        assertFalse(retrievedBeanOpt.isPresent());
    }

    @Test
    public void deletePage() {

        PageTransferBean pageBean = getPageTransferBean();

        PageTransferBean savedBean = pagesService.savePage(pageBean);

        List<PageTransferBean> pages = pagesService.getPagesForSite(siteId, true);
        assertEquals(1, pages.size());

        pagesService.deletePage(savedBean.id, siteId);

        pages = pagesService.getPagesForSite(siteId, true);
        assertEquals(0, pages.size());
    }

    private PageTransferBean getPageTransferBean() {

        PageTransferBean pageBean = new PageTransferBean();
        pageBean.title = title;
        pageBean.content = content;
        pageBean.siteId = siteId;
        return pageBean;
    }
}

