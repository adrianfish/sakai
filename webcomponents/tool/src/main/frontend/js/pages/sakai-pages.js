import { html } from "../assets/lit-element/lit-element.js";
import { unsafeHTML } from "../assets/lit-html/directives/unsafe-html.js"
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
    };
  }

  constructor() {

    super();

    this._state = "PAGES";

    this._pages = [];

    this.loadTranslations("pages").then(i18n => this._i18n = i18n);

    this._templatePageBean = { siteId: "", title: "", content: "" };
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

  _savePage() {

    this._templatePageBean.siteId = this.siteId;

    fetch(`/api/sites/${this.siteId}/pages`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(this._templatePageBean)
    })
    .then(r => {

      if (r.ok) {
        return r.json();
      }
    })
    .then(page => {

      this._pages.push(page);
      this._state = "PAGES";
    })
    .catch(error => console.error(error));
  }

  _cancelAddPage() { this._state = "PAGES"; }

  _updateTitle(e) { this._templatePageBean.title = e.target.value; }

  _updateContent(e) { this._templatePageBean.content = e.detail.content; }

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
        <input id="pages-title-input" type="text" @keyup=${this._updateTitle}>
      </div>
      <div class="mt-3">${this._i18n.add_page_content_label}</div>
      <div>
        <sakai-editor @changed=${this._updateContent}></sakai-editor>
      </div>
      <div class="mt-2">
        <button type="button" @click=${this._savePage} class="btn btn-primary">${this._i18n.save}</button>
        <button type="button" @click=${this._cancelAddPage} class="btn btn-secondary">${this._i18n.cancel}</button>
      </div>
    `;
  }

  render() {

    return html`
      ${this._state === "PAGES" ? html`
        <div class="d-flex justify-content-between">
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
        <div>
          <table>
          <tbody>
          ${this._pages.map(page => html`
            <tr><td>${page.title}</td><td>${unsafeHTML(page.content)}</td></tr>
          `)}
          </tbody>
          </table>
        </div>
      ` : ""}

      ${this._state === "ADD_PAGE" ? this._renderAddPage() : ""}
    `;
  }
}

!customElements.get("sakai-pages") && customElements.define("sakai-pages", SakaiPages);
