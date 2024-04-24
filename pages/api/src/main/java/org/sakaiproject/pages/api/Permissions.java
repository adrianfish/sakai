package org.sakaiproject.pages.api;

public interface Permissions {

    static final String ADD_PAGE = "pages.add.page";
    static final String DELETE_PAGE = "pages.delete.page";
    static final String EDIT_PAGE = "pages.edit.page";

    // This allows both publishing and unpublishing becuase that just makes sense.
    static final String PUBLISH_PAGE = "pages.publish.page";
}
