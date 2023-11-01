package org.sakaiproject.pages.api;

import org.sakaiproject.pages.api.model.PagesPage;

public class PageTransferBean {

    public String id;
    public String title;
    public String content;

    public PagesPage asPage() {

        PagesPage page = new PagesPage();

        page.setTitle(this.title);
        page.setContents(this.content);

        return page;
    }
}
