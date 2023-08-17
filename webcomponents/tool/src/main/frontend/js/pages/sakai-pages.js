import { html } from "../assets/lit-element/lit-element.js";
import { SakaiElement } from "../sakai-element.js";

export class SakaiPages extends SakaiElement {

  static get properties() {

    return {
      siteId: { attribute: "site-id", type: String },
      _topLevelPages: { attribute: false, type: Array},
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

  render() {

    return html`
      <h1>Welcome to the Pages tool at ${this.siteId} !</h1>
    `;
  }
}

!customElements.get("sakai-pages") && customElements.define("sakai-pages", SakaiPages);
