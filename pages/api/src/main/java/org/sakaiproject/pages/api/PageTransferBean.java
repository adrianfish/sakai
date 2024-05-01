package org.sakaiproject.pages.api;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.pages.api.model.PagesPage;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class PageTransferBean implements Entity {

    public String id;
    public String title;
    public String content;
    public String siteId;
    public Boolean draft;
    public Map<String, String> links = new HashMap<>();
    public boolean canDelete;
    public boolean canEdit;
    public boolean canPublish;

    public PagesPage asPage() {

        PagesPage page = new PagesPage();

        page.setId(this.id);
        page.setTitle(this.title);
        page.setContent(this.content);
        page.setSiteId(this.siteId);
        page.setDraft(this.draft);

        return page;
    }

    public static PageTransferBean of(PagesPage page) {

        PageTransferBean bean = new PageTransferBean();
        bean.id = page.getId();
        bean.title = page.getTitle();
        bean.content = page.getContent();
        bean.siteId = page.getSiteId();
        bean.draft = page.getDraft();
        return bean;
    }

    public String getReference() {
        return PagesService.REFERENCE_ROOT + Entity.SEPARATOR + id;
    }
}
