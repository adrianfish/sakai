import { html } from "../assets/lit-element/lit-element.js";
import { unsafeHTML } from "../assets/lit-html/directives/unsafe-html.js";
import { SakaiElement } from "../sakai-element.js";
import "../sakai-editor.js";

export class SakaiPages extends SakaiElement {

  static get properties() {

    return {
      siteId: { attribute: "site-id", type: String },
      _topLevelPages: { attribute: false, type: Array },
      _i18n: { attribute: false, type: Object },
      _addPageUrl: { attribute: false, type: String },
      _state: { attribute: false, type: String },
      _pages: { attribute: false, type: Array },
      _pageBeingEdited: { attribute: false, type: Object },
      _pageBeingViewed: { attribute: false, type: Object },
    };
  }

  constructor() {

    super();

    this._state = "PAGES";

    this._pages = [];

    this.loadTranslations("pages").then(i18n => this._i18n = i18n);

    this._templatePageBean = { siteId: "", title: "", content: "" };

    this._pageBeingEdited = { ...this._templatePageBean };
  }

  set siteId(value) {

    const oldValue = this._siteId;

    this._siteId = value;

    this._getData();

    this.requestUpdate("siteId", oldValue);
  }

  get siteId() { return this._siteId; }

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
      this._addPageUrl = data.links.find(link => link.rel === "addPage")?.href;
    })
    .catch(error => console.error(error));

  }

  _addPage() { this._state = "ADD_PAGE"; }

  _viewPage(e) {

    const pageId = e.target.dataset.pageId;

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
      this._state = "VIEW_PAGE";
    })
    .catch(error => console.error(error));
  }

  _viewPages() { this._state = "PAGES"; }

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
      this._state = "PAGES"; // This will trigger an update
    })
    .catch(error => console.error(error));
  }

  _cancelAddPage() { this._state = "PAGES"; }

  _updateTitle(e) { this._pageBeingEdited.title = e.target.value; }

  _updateContent(e) { this._pageBeingEdited.content = e.detail.content; }

  _editPage(e) {

    const pageId = e.target.dataset.pageId;

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
      this._state = "ADD_PAGE";
    })
    .catch(error => console.error(error));
  }

  _deletePage(e) {

    const pageId = e.target.dataset.pageId;

    const url = `/api/sites/${this.siteId}/pages/${pageId}`;
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

  shouldUpdate() {
    return this._i18n;
  }

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
        <button type="button" class="btn btn-secondary" @click=${this._viewPages}>Done</button>
      </div>
    `;
  }

  render() {

    return html`
      ${this._state === "PAGES" ? html`
        <div class="d-flex justify-content-between mb-3">
          <div>
            <h1 class="d-inline">${this._i18n.pages_header}</h1>
          </div>

          ${this._addPageUrl ? html`
            <div>
              <button type="button"
                  @click=${this._addPage}
                  class="btn btn-icon">
                <i class="si si-add"></i>
                <span class="ms-2">${this._i18n.add_page_header}</span>
              </button>
            </div>
          ` : ""}
        </div>

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
                      @click=${this._viewPage}>
                    ${page.title}
                  </button>
                </td>
                <td style="width: 140px;">
                  <button type="button"
                      class="btn btn-link"
                      data-page-id="${page.id}"
                      @click=${this._editPage}>
                    <i class="si si-edit pe-none"></i>
                  </button>
                  <button type="button"
                      class="btn btn-link"
                      data-page-id="${page.id}"
                      @click=${this._deletePage}>
                    <i class="si si-trash pe-none"></i>
                  </button>
                </td>
              </tr>
            `)}
            </tbody>
          </table>
        </div>
      ` : ""}

      ${this._state === "ADD_PAGE" ? this._renderAddPage() : ""}
      ${this._state === "VIEW_PAGE" ? this._renderViewPage() : ""}
    `;
  }
}

!customElements.get("sakai-pages") && customElements.define("sakai-pages", SakaiPages);
