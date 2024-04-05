package org.sakaiproject.pages.api;

import java.util.Collection;
import java.util.Optional;

import org.sakaiproject.entity.api.Entity;

public interface PagesService {

    public static final String REFERENCE_ROOT = Entity.SEPARATOR + "pages";
    public static final String TOOL_ID = "sakai.pages";

    /**
     * Saves a page. Tests if the page exists already
     */
    public PageTransferBean savePage(PageTransferBean page) throws PagesPermissionException;

    public Collection<PageTransferBean> getPagesForSite(String siteId, boolean populate) throws PagesPermissionException;

    public Optional<PageTransferBean> getPage(String siteId, String pageId) throws PagesPermissionException;

    public void deletePage(String siteId, String pageId) throws PagesPermissionException;
}
