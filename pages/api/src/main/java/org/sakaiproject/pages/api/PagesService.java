package org.sakaiproject.pages.api;

import java.util.List;

public interface PagesService {

    /**
     * Saves a page. Tests if the page exists already
     */
    public void savePage(PageTransferBean page);

    public List<PageTransferBean> getPagesForSite(String siteId);
}
