import { html } from "../assets/lit-element/lit-element.js";
import { SakaiElement } from "../sakai-element.js";

export class SakaiPages extends SakaiElement {

  constructor() {

    super();

    this.loadTranslations("pages").then(i18n => this._i18n = i18n);
  }

  static get properties() {

    return {
      siteId: { attribute: "site-id", type: String },
      _topLevelPages: { attribute: false, type: Array},
      _i18n: { attribute: false, type: Object },
      _addPageUrl: { attribute: false, type: String },
    };
  }

  set siteId(value) {

    const oldValue = this._siteId;

    this._siteId = value;

    this._getData();

    this.requestUpdate("siteId", oldValue);
  }

  get siteId() { return this._siteId; }

  _getData() {

    fetch(`/api/sites/${this.siteId}/pages`, { credentials: "include" })
    .then(r => {

      if (r.ok) {
        return r.json();
      }

      throw new Error("asdfasdf");
    })
    .then(data => {

      console.log(data);

      this._addPageUrl = data.links.find(link => link.rel === "addPage")?.href;
    });

    // Get the initial load of JSON data. This will include the top level pages for the site.
  }

  _addPage(e) {

    console.log("clicked");
  }

  shouldUpdate() {
    return this._i18n;
  }

  render() {

    return html`
      <div class="d-flex justify-content-between">
        <div>Pages</div>

        ${this._addPageUrl ? html`
          <div>
            <button type="button" @click=${this._addPage} class="btn btn-icon"><i class="bi bi-window-plus"></i></button>
          </div>
        ` : ""}
      </div>
    `;
  }
}

!customElements.get("sakai-pages") && customElements.define("sakai-pages", SakaiPages);
