package org.sakaiproject.pages.api;

import org.sakaiproject.pages.api.model.PagesPage;

import java.util.HashMap;
import java.util.Map;

public class PageTransferBean {

    public String id;
    public String title;
    public String content;
    public String siteId;
    public Map<String, String> links = new HashMap<>();
    public boolean canDelete;
    public boolean canEdit;

    public PagesPage asPage() {

        PagesPage page = new PagesPage();

        page.setId(this.id);
        page.setTitle(this.title);
        page.setContent(this.content);
        page.setSiteId(this.siteId);

        return page;
    }

    public static PageTransferBean of(PagesPage page) {

        PageTransferBean bean = new PageTransferBean();
        bean.id = page.getId();
        bean.title = page.getTitle();
        bean.content = page.getContent();
        bean.siteId = page.getSiteId();
        return bean;
    }
}
