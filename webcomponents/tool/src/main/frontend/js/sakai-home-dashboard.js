import { html } from './assets/lit-element/lit-element.js';
import { SakaiElement } from './sakai-element.js';
import { ifDefined } from './assets/lit-html/directives/if-defined.js';
import { unsafeHTML } from './assets/lit-html/directives/unsafe-html.js';
import './sakai-icon.js';
import "./sakai-course-list.js";
import "./widgets/sakai-widget-panel.js";
import "./sakai-button.js";

export class SakaiHomeDashboard extends SakaiElement {

  static get properties() {

    return {
      data: Object,
      i18n: Object,
      state: String,
      courses: { type: Array},
      userId: { attribute: "user-id", type: String },
      showSites: { attribute: "show-sites", type: Boolean },
      _editing: { attribute: false, type: Boolean },
      _i18n: { attribute: false, type: Object },
      _showMotd: { attribute: false, type: Boolean },
    };
  }

  constructor() {

    super();

    this.loadTranslations("dashboard").then(r => this._i18n = r);
  }

  set userId(value) {

    this._userId = value;
    this._loadData();
  }

  get userId() { return this._userId; }

  _loadData() {

    const url = `/api/users/${this.userId}/dashboard`;
    fetch(url, { credentials: "include" })
      .then(r => {

        if (r.ok) {
          return r.json();
        }
        throw new Error(`Failed to get dashboard data from ${url}`);

      })
      .then(r => {

        this.data = r;
        this._showMotd = this.data.motd;
      })
      .catch(error => console.error(error));
  }

  shouldUpdate() {
    return this._i18n && this.data;
  }

  widgetLayoutChange(e) {
    this.data.layout = e.detail.layout;
  }

  edit() {

    this._editing = !this._editing;
    this.layoutBackup = [...this.data.layout];
  }

  cancel() {

    this._editing = false;
    this.data.layout = [...this.layoutBackup];
    this.requestUpdate();
  }

  save() {

    this._editing = !this._editing;

    const url = `/api/users/${this.userId}/dashboard`;
    fetch(url, {
      method: "PUT",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ layout: this.data.layout }),
    }).then(r => {

      if (!r.ok) {
        throw new Error(`Failed to update dashboard for url ${url}`);
      }
    }).catch(error => console.error(error.message));
  }

  _toggleMotd() {
    this._showMotd = !this._showMotd;
  }

  render() {

    return html`

      <div id="home-dashboard-container">
        <div id="welcome-and-edit-block">
          <div id="welcome">${this._i18n.welcome} ${this.data.givenName}</div>
          <div id="edit-block">
          ${this._editing ? html`
            <div id="save">
              <sakai-button @click=${this.save} title="${this._i18n.save_tooltip}" aria-label="${this._i18n.save_tooltip}">${this._i18n.save}</sakai-button>
            </div>
            <div id="cancel">
              <sakai-button @click=${this.cancel} title="${this._i18n.cancel_tooltip}" aria-label="${this._i18n.cancel_tooltip}">${this._i18n.cancel}</sakai-button>
            </div>
          ` : html`
            <div id="edit">
              <sakai-button slot="invoker" @click=${this.edit} title="${this._i18n.edit_tooltip}" arial-label="${this._i18n.edit_tooltip}">${this._i18n.edit}</sakai-button>
            </div>
          `}
          </div>
        </div>
        ${this.data.worksiteSetupUrl ? html`
          <div id="toolbar">
            <sakai-button href="${this.data.worksiteSetupUrl}" title="${this._i18n.worksite_setup_tooltip}" aria-label="${this._i18n.worksite_setup_tooltip}">
              <div id="add-worksite">
                <div><sakai-icon type="add" size="small"></sakai-icon></div>
                <div>${this._i18n.worksite_setup}</div>
              </div>
            </sakai-button>
          </div>
        ` : ""}
        ${this.data.motd ? html`
          <div id="motd">
            <div id="motd-title-block" @click=${this._toggleMotd}>
              <div id="motd-title">${this._i18n.motd}</div>
              <div id="motd-icon">
                <a href="javascript:;"
                  title="${this._showMotd ? this._i18n.hide_motd_tooltip : this._i18n.show_motd_tooltip}"
                  aria-label="${this._showMotd ? this._i18n.hide_motd_tooltip : this._i18n.show_motd_tooltip}">
                  <sakai-icon type="${this._showMotd ? "up" : "down"}" size="small"></sakai-icon>
                </a>
              </div>
            </div>
            <div id="motd-message" style="display: ${this._showMotd ? "block" : "none"}">${unsafeHTML(this.data.motd)}</div>
          </div>
        ` : ""}
        <div id="courses-and-widgets">
          ${this.showSites ? html`
          <div id="courses"><sakai-course-list></div>
          ` : ""}
          <div id="widgets">
            <sakai-widget-panel
              id="widget-grid"
              @changed=${this.widgetLayoutChange}
              widget-ids=${JSON.stringify(this.data.widgets)}
              layout="${JSON.stringify(this.data.layout)}"
              site-id=""
              user-id="${ifDefined(this.userId ? this.userId : "")}"
              columns="2"
              ?editing=${this._editing}>
          </div>
        </div>
      </div>
    `;
  }
}

if (!customElements.get("sakai-home-dashboard")) {
  customElements.define("sakai-home-dashboard", SakaiHomeDashboard);
}
