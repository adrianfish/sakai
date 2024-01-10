package org.sakaiproject.pages.api;

import java.util.List;
import java.util.Optional;

public interface PagesService {

    /**
     * Saves a page. Tests if the page exists already
     */
    public PageTransferBean savePage(PageTransferBean page);

    public List<PageTransferBean> getPagesForSite(String siteId, boolean populate);

    public Optional<PageTransferBean> getPage(String siteId, String pageId);

    public void deletePage(String pageId, String siteId);
}
