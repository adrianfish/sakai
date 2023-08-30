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
    };
  }

  set siteId(value) {

    const oldValue = this._siteId;

    this._siteId = value;

    this._getInitialData();

    this.requestUpdate("siteId", oldValue);
  }

  get siteId() { return this._siteId; }

  _getInitialData() {

    // Get the initial load of JSON data. This will include the top level pages for the site.
  }

  shouldUpdate() {
    return this._i18n;
  }

  render() {

    return html`
      <h1>Welcome to the Pages tool at ${this.siteId} !</h1>
      <h2>${this._i18n["pages.hello_world"]}</h2>
    `;
  }
}

!customElements.get("sakai-pages") && customElements.define("sakai-pages", SakaiPages);
