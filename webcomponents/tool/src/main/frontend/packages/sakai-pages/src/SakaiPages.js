import { html, nothing } from "lit";
import { unsafeHTML } from "lit-html/directives/unsafe-html.js";
import { SakaiElement } from "@sakai-ui/sakai-element";
import "@sakai-ui/sakai-editor/sakai-editor.js";
import { ADD_PAGE, PAGES, PERMISSIONS, VIEW_PAGE } from "./states.js";

export class SakaiPages extends SakaiElement {

  static properties = {

    siteId: { attribute: "site-id", type: String },
    pageId: { attribute: "page-id", type: String },

    _topLevelPages: { state: true },
    _i18n: { state: true },
    _addPageUrl: { state: true },
    _state: { state: true },
    _pages: { state: true },
    _pageBeingEdited: { state: true },
    _pageBeingViewed: { state: true },
  };

  constructor() {

    super();

    this._state = PAGES;

    this._pages = [];

    this.loadTranslations("pages").then(i18n => this._i18n = i18n);

    this._templatePageBean = { siteId: "", title: "", content: "" };

    this._pageBeingEdited = { ...this._templatePageBean };
  }

  set _state(value) {

    const old = this.__state;

    // Ensure that the site data has been loaded if we want the PAGES view. It may not have been
    // if we were at a single page url, for example.
    if (value === PAGES && !this._pages?.length) this._getData();

    this.__state = value;

    this.requestUpdate("_state", old);
  }

  get _state() { return this.__state; }

  attributeChangedCallback(name, oldValue, newValue) {

    super.attributeChangedCallback(name, oldValue, newValue);


    if (this.siteId) {
      if (this.pageId) {
        this._viewPage(this.pageId);
      } else {
        this._getData();
      }
    }
  }

  _getData() {

    // Get the initial load of JSON data. This will include the top level pages for the site.

    const url = `/api/sites/${this.siteId}/pages`;
    fetch(url, { credentials: "include" })
    .then(r => {

      if (r.ok) {
        return r.json();
      }

      throw new Error(`Network error whilst getting initial data from ${url}`);
    })
    .then(data => {

      this._pages = data.pages;
      this._addPageUrl = data.links?.addPage;
    })
    .catch(error => console.error(error));

  }

  _addPage() { this._state = ADD_PAGE; }

  _showPermissions() {

    import("@sakai-ui/sakai-permissions/sakai-permissions.js").then(() => {
      this._state = PERMISSIONS;
    });
  }

  _onViewPage(e) { this._viewPage(e.target.dataset.pageId); }

  _viewPage(pageId) {

    const url = `/api/sites/${this.siteId}/pages/${pageId}`;
    fetch(url, { credentials: "include" })
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw new Error(`Network error whilst getting page data from ${url}`);
    })
    .then(page => {

      this._pageBeingViewed = page;
      this._state = VIEW_PAGE;
    })
    .catch(error => console.error(error));
  }

  _viewPages() { this._state = PAGES; }

  _savePage() {

    this._pageBeingEdited.siteId = this.siteId;

    const isNew = !this._pageBeingEdited.id;

    const url = `/api/sites/${this.siteId}/pages${isNew ? "" : `/${this._pageBeingEdited.id}`}`;
    fetch(url, {
      method: isNew ? "POST" : "PUT",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(this._pageBeingEdited)
    })
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw new Error(`Network error while saving page at ${url}`);
    })
    .then(page => {

      const index = this._pages.findIndex(p => p.id === page.id);
      if (index !== -1) {
        this._pages.splice(index, 1, page);
      } else {
        this._pages.push(page);
      }

      this._pageBeingEdited = { ...this._templatePageBean };

      this._state = PAGES; // This will trigger an update
    })
    .catch(error => console.error(error));
  }

  _cancelAddPage() {

    this._pageBeingEdited = { ...this._templatePageBean };
    this._state = PAGES;
  }

  _updateTitle(e) { this._pageBeingEdited.title = e.target.value; }

  _updateContent(e) { this._pageBeingEdited.content = e.detail.content; }

  _editPage(e) {

    const pageId = e.currentTarget.dataset.pageId;

    const url = `/api/sites/${this.siteId}/pages/${pageId}`;
    fetch(url, { credentials: "include" })
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw new Error(`Network error whilst getting page data from ${url}`);
    })
    .then(page => {

      this._pageBeingEdited = page;
      this._state = ADD_PAGE;
    })
    .catch(error => console.error(error));
  }

  _deletePage(e) {

    if (!confirm("Are you sure you want to delete this page?")) {
      return false;
    }

    const pageId = e.currentTarget.dataset.pageId;

    const url = this._pages.find(page => page.id === pageId)?.links?.deletePage;

    if (!url) {
      console.error("_deletePage called without a deletePage url");
      return;
    }

    fetch(url, { method: "DELETE", credentials: "include" })
    .then(r => {

      if (r.ok) {
        const index = this._pages.findIndex(p => p.id === pageId);
        this._pages.splice(index, 1);

        // We need to request an update because lit only watches the array instance, not the contents
        this.requestUpdate();
      } else {
        throw new Error(`Network error whilst getting page data from ${url}`);
      }
    })
    .catch(error => console.error(error));
  }

  _onPermissionsCancelled() { this._state = PAGES; }

  shouldUpdate() { return this._i18n; }

  _renderAddPage() {

    return html`
      <div class="mb-4">
        <h1 class="d-inline">${this._i18n.add_page_header}</h1>
      </div>
      <div>${this._i18n.add_page_title_label}</div>
      <div>
        <input id="pages-title-input" type="text" @keyup=${this._updateTitle} .value=${this._pageBeingEdited.title}>
      </div>
      <div class="mt-3">${this._i18n.add_page_content_label}</div>
      <div>
        <sakai-editor @changed=${this._updateContent} content="${this._pageBeingEdited.content}"></sakai-editor>
      </div>
      <div class="mt-2">
        <button type="button" @click=${this._savePage} class="btn btn-primary">${this._i18n.save}</button>
        <button type="button" @click=${this._cancelAddPage} class="btn btn-secondary">${this._i18n.cancel}</button>
      </div>
    `;
  }

  _renderViewPage() {

    return html`
      <h1>${this._pageBeingViewed.title}</h1>
      <div>${unsafeHTML(this._pageBeingViewed.content)}</div>
      <div class="mt-2">
        <button type="button" class="btn btn-secondary" @click=${this._viewPages}>${this._i18n.done}</button>
      </div>
    `;
  }

  render() {

    return html`
      ${this._state === PAGES ? html`
        <div class="d-flex justify-content-between mb-3">
          <div>
            <h1 class="d-inline">${this._i18n.pages_header}</h1>
          </div>

          <div class="d-flex">
            ${this._addPageUrl ? html`
                <button type="button"
                    @click=${this._addPage}
                    class="btn btn-icon d-flex align-items-center">
                  <i class="si si-add"></i>
                  <span class="ms-2">${this._i18n.add_page_header}</span>
                </button>
            ` : nothing}

              <div class="dropdown">
                <button type="button"
                    id="pages-options-menu-button"
                    class="btn btn-icon ms-2"
                    data-bs-toggle="dropdown"
                    aria-expanded="false"
                    aria-label="${this._i18n.options_menu_label}">
                  <i class="si si-kebob"></i>
                </button>
                <ul class="dropdown-menu" aria-labelledby="pages-options-menu-button">
                  <li><button class="dropdown-item" @click=${this._showPermissions}>${this._i18n.permissions}</button></li>
                </ul>
              </div>
          </div>

        </div>

        ${this._pages?.length ? html`
          <!-- PAGES TABLE STARTS HERE -->
          <div class="table-responsive">
            <table class="table table-striped table-bordered table-hover">
              <thead>
                <tr>
                  <th>${this._i18n.title}</th>
                  <th>${this._i18n.actions}</th>
                </tr>
              </thead>
              <tbody>
              ${this._pages.map(page => html`
                <tr>
                  <td>
                    <button type="button"
                        class="btn btn-link"
                        data-page-id="${page.id}"
                        @click=${this._onViewPage}>
                      ${page.title}
                    </button>
                  </td>
                  <td style="width: 80px;">

                    <div class="dropdown">
                      <button type="button"
                          id="page-${page.id}-options-menu-button"
                          class="btn btn-icon ms-2"
                          data-bs-toggle="dropdown"
                          aria-expanded="false"
                          aria-label="${this._i18n.options_menu_label}">
                        <i class="si si-kebob"></i>
                      </button>
                      <ul class="dropdown-menu" aria-labelledby="pages-${page.id}-options-menu-button">
                        ${page?.links?.editPage ? html`
                        <li>
                          <button type="button"
                              class="dropdown-item"
                              data-page-id="${page.id}"
                              @click=${this._editPage}>
                            <i class="si si-edit"></i>
                            <span>${this._i18n.edit}</span>
                          </button>
                        </li>
                        ` : nothing}
                        ${page?.links?.deletePage ? html`
                        <li>
                          <button type="button"
                              class="dropdown-item"
                              data-page-id="${page.id}"
                              @click=${this._deletePage}>
                            <i class="si si-trash"></i>
                            <span>${this._i18n.delete}</span>
                          </button>
                        </li>
                        ` : nothing}
                        ${page?.links?.publishPage ? html`
                        <li>
                          <button type="button"
                              class="dropdown-item"
                              data-page-id="${page.id}"
                              @click=${this._publishPage}>
                            <i class="si si-visible"></i>
                            <span>${this._i18n.publish}</span>
                          </button>
                        </li>
                        ` : nothing}
                        ${page?.links?.unpublishPage ? html`
                        <li>
                          <button type="button"
                              class="dropdown-item"
                              data-page-id="${page.id}"
                              @click=${this._unpublishPage}>
                            <i class="si si-hidden"></i>
                            <span>${this._i18n.unpublish}</span>
                          </button>
                        </li>
                        ` : nothing}


                      </ul>
                    </div>
                  </td>
                </tr>
              `)}
              </tbody>
            </table>
          </div>
          ` : html`
            <h4>No pages</h4>
          `}
      ` : nothing}

      ${this._state === ADD_PAGE ? this._renderAddPage() : nothing}
      ${this._state === VIEW_PAGE ? this._renderViewPage() : nothing}
      ${this._state === PERMISSIONS ? html`
        <sakai-permissions tool="pages"
            @permissions-cancelled=${this._onPermissionsCancelled}
            fire-cancel-event>
        </sakai-permissions>
      ` : nothing}
    `;
  }
}
