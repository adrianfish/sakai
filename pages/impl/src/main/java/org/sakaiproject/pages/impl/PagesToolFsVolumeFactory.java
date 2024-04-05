/**
 * Copyright (c) 2003-2016 The Apereo Foundation
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
package org.sakaiproject.pages.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.elfinder.FsType;
import org.sakaiproject.elfinder.ReadOnlyFsVolume;
import org.sakaiproject.elfinder.SakaiFsItem;
import org.sakaiproject.elfinder.SakaiFsService;
import org.sakaiproject.elfinder.ToolFsVolume;
import org.sakaiproject.elfinder.ToolFsVolumeFactory;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.pages.api.PageReferenceReckoner;
import org.sakaiproject.pages.api.PagesPermissionException;
import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.model.PagesPage;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class PagesToolFsVolumeFactory implements ToolFsVolumeFactory {

    private EntityManager entityManager;
    private PagesService pagesService;
    private SakaiFsService sakaiFsService;
    private ServerConfigurationService serverConfigurationService;

    public void init() {
        sakaiFsService.registerToolVolume(this);
    }

    @Override
    public String getPrefix() {
        return FsType.PAGE.toString();
    }

    @Override
    public ToolFsVolume getVolume(String siteId) {
        return new PagesToolFsVolume(sakaiFsService, siteId);
    }

    @Override
    public String getToolId() {
        return PagesService.TOOL_ID;
    }
    
    public class PagesToolFsVolume extends ReadOnlyFsVolume implements ToolFsVolume {

        private static final String PAGE_URL_PREFIX = "/direct/pages/";
        private SakaiFsService service;
        private String siteId;

        public PagesToolFsVolume(SakaiFsService service, String siteId) {

            this.service = service;
            this.siteId = siteId;
        }

        @Override
        public String getSiteId() {
            return siteId;
        }

        @Override
        public ToolFsVolumeFactory getToolVolumeFactory() {
            return PagesToolFsVolumeFactory.this;
        }

        @Override
        public SakaiFsItem fromPath(String path){

            if (StringUtils.isNotBlank(path)) {
                String[] parts = path.split("/");
                if (parts.length > 2 && (getPrefix().equals(parts[1]))) {
                    try {
                        Optional<PageTransferBean> page = pagesService.getPage("balls", parts[2]);
                        if (!page.isPresent()) {
                            log.warn("No page found for id {}", parts[2]);
                            return null;
                        }
                        return new SakaiFsItem(page.get().id, page.get().title, this, FsType.PAGE);
                    } catch (PagesPermissionException ppe) {
                        log.warn("No permission to read page with id {}", parts[2]);
                    }
                }
            }
            return this.getRoot();
        }

        @Override
        public String getMimeType(SakaiFsItem fsItem) {
            return this.isFolder(fsItem) ? "directory" : "sakai/pages";
        }

        @Override
        public String getName() {
            return "Pages";
        }

        @Override
        public String getName(SakaiFsItem fsItem) {

            if (this.getRoot().equals(fsItem)) {
                return getName();
            } else if (FsType.PAGE.equals(fsItem.getType())) {
                return fsItem.getTitle();
            } else {
                throw new IllegalArgumentException("Could not get title for: " + fsItem.toString());
            }
        }

        @Override
        public SakaiFsItem getParent(SakaiFsItem fsItem) {

            if (this.getRoot().equals(fsItem)) {
                return service.getSiteVolume(siteId).getRoot();
            } else if(FsType.PAGE.equals(fsItem.getType())) {
                return this.getRoot();
            }
            return null;
        }

        @Override
        public String getPath(SakaiFsItem fsi) throws IOException {

            if (this.getRoot().equals(fsi)) {
                return "/" + getPrefix() + "/" + siteId;
            } else if (FsType.PAGE.equals(fsi.getType())) {
                return "/" + getPrefix() + "/" + siteId + "/" + fsi.getId();
            } else {
                throw new IllegalArgumentException("Wrong Type: " + fsi.toString());
            }
        }

        @Override
        public SakaiFsItem getRoot() {
            return new SakaiFsItem("", "", this, FsType.PAGE);
        }

        @Override
        public boolean isFolder(SakaiFsItem fsItem) {
            return FsType.PAGE.equals(fsItem.getType()) && fsItem.getTitle().equals("");
        }

        @Override
        public SakaiFsItem[] listChildren(SakaiFsItem fsItem) {

            List<SakaiFsItem> items = new ArrayList<>();
            if (this.getRoot().equals(fsItem)) {
                try {
                    for (PageTransferBean page : pagesService.getPagesForSite(siteId, false)) {
                        items.add(new SakaiFsItem(page.id, page.title, this, FsType.PAGE));
                    }
                } catch (PagesPermissionException ppe) {
                    log.warn("No permission to get pages for site {}", siteId);
                }

            } else if (FsType.PAGE.equals(fsItem.getType())) {
                items.add(fsItem);
            }

            return items.toArray(new SakaiFsItem[0]);
        }

        @Override
        public String getURL(SakaiFsItem fsItem) {

            String url = null;
            if (FsType.PAGE.equals(fsItem.getType())) {
                try {
                    String ref = PageReferenceReckoner.reckoner().siteId(siteId).id(fsItem.getId()).reckon().toString();
                    url = entityManager.getUrl(ref, Entity.UrlType.PORTAL).orElse("");
                    System.out.println("URL: " + url);
                } catch (Exception e) {
                    log.warn("Could not create url for page {}", fsItem.getId());
                }
            }
            return url;
        }
    }
}
