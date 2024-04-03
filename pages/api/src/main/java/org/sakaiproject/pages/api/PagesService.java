package org.sakaiproject.pages.api;

import java.util.List;
import java.util.Optional;

public interface PagesService {

    /**
     * Saves a page. Tests if the page exists already
     */
    public PageTransferBean savePage(PageTransferBean page) throws PagesPermissionException;

    public List<PageTransferBean> getPagesForSite(String siteId, boolean populate) throws PagesPermissionException;

    public Optional<PageTransferBean> getPage(String siteId, String pageId) throws PagesPermissionException;

    public void deletePage(String siteId, String pageId) throws PagesPermissionException;
}
