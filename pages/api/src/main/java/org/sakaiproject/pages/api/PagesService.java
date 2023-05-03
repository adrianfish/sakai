package org.sakaiproject.pages.api;

public interface PagesService {

    /**
     * Saves a page. Tests if the page exists already
     */
    public void savePage(PageTransferBean page);
}
