import { html, nothing } from "lit";
import { SakaiElement } from "@sakai-ui/sakai-element";
import { ifDefined } from "lit/directives/if-defined.js";
import { unsafeHTML } from "lit/directives/unsafe-html.js";
import "@sakai-ui/sakai-icon/sakai-icon.js";
import "@sakai-ui/sakai-course-list/sakai-course-list.js";
import "@sakai-ui/sakai-widgets";
import "@sakai-ui/sakai-widgets/sakai-widget-panel.js";
import "@sakai-ui/sakai-button/sakai-button.js";

export class SakaiHomeDashboard extends SakaiElement {

  static properties = {

    courses: { type: Array },
    userId: { attribute: "user-id", type: String },
    showSites: { attribute: "show-sites", type: Boolean },
    _data: { state: true },
    _i18n: { state: true },
    _showMotd: { state: true },
    _editing: { state: true },
  };

  constructor() {

    super();

    this.showSites = true;

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

        this._data = r;
        this._showMotd = this._data.motd;
      })
      .catch(error => console.error(error));
  }

  shouldUpdate() {
    return this._i18n && this._data;
  }

  widgetLayoutChange(e) {
    this._data.layout = e.detail.layout;
  }

  edit() {

    this._editing = !this._editing;
    this.layoutBackup = [ ...this._data.layout ];
  }

  cancel() {

    this._editing = false;
    this._data.layout = [ ...this.layoutBackup ];
    this.requestUpdate();
  }

  save() {

    this._editing = !this._editing;

    const url = `/api/users/${this.userId}/dashboard`;
    fetch(url, {
      method: "PUT",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ layout: this._data.layout }),
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

      <div class="hd-container">
        <div class="hd-welcome-and-edit-block">
          <div class="hd-welcome">${this._i18n.welcome} ${this._data.givenName}</div>
          <div class="hd-edit-block">
          ${this._editing ? html`
            <div class="hd-save">
              <sakai-button @click=${this.save} title="${this._i18n.save_tooltip}" aria-label="${this._i18n.save_tooltip}">${this._i18n.save}</sakai-button>
            </div>
            <div>
              <sakai-button @click=${this.cancel} title="${this._i18n.cancel_tooltip}" aria-label="${this._i18n.cancel_tooltip}">${this._i18n.cancel}</sakai-button>
            </div>
          ` : html`
            <div>
              <sakai-button slot="invoker" @click=${this.edit} title="${this._i18n.edit_tooltip}" arial-label="${this._i18n.edit_tooltip}">${this._i18n.edit}</sakai-button>
            </div>
          `}
          </div>
        </div>
        ${this._data.worksiteSetupUrl ? html`
          <div class="hd-toolbar">
            <sakai-button href="${this._data.worksiteSetupUrl}" title="${this._i18n.worksite_setup_tooltip}" aria-label="${this._i18n.worksite_setup_tooltip}">
              <div class="hd-add-worksite">
                <div><sakai-icon type="add" size="small"></sakai-icon></div>
                <div>${this._i18n.worksite_setup}</div>
              </div>
            </sakai-button>
          </div>
        ` : nothing}
        ${this._data.motd ? html`
          <div class="hd-motd">
            <div class="hd-motd-title-block" @click=${this._toggleMotd}>
              <div class="hd-motd-title">${this._i18n.motd}</div>
              <div>
                <a href="javascript:;"
                  title="${this._showMotd ? this._i18n.hide_motd_tooltip : this._i18n.show_motd_tooltip}"
                  aria-label="${this._showMotd ? this._i18n.hide_motd_tooltip : this._i18n.show_motd_tooltip}">
                  <sakai-icon type="${this._showMotd ? "up" : "down"}" size="small"></sakai-icon>
                </a>
              </div>
            </div>
            <div class="hd-motd-message" style="display: ${this._showMotd ? "block" : "none"}">${unsafeHTML(this._data.motd)}</div>
          </div>
        ` : nothing}
        <div class="hd-courses-and-widgets">
          ${this.showSites ? html`
            <div class="hd-courses">
              <sakai-course-list user-id="${this.userId}"></sakai-course-list>
            </div>
          ` : nothing}
          <div class="hd-widgets">
            <sakai-widget-panel
              @changed=${this.widgetLayoutChange}
              .widgetIds=${this._data.widgets}
              .layout=${this._data.layout}
              site-id=""
              user-id="${ifDefined(this.userId)}"
              columns="2"
              ?editing=${this._editing}>
            </sakai-widget-panel>
          </div>
        </div>
      </div>
    `;
  }

  /*
  static styles = css`
    #container {
      font-family: var(--sakai-font-family);
      background-color: var(--sakai-tool-bg-color);
    }
      #welcome-and-edit-block {
        display: flex;
        align-items: center;
      }
        #welcome {
          flex: 1;
          font-size: var(--sakai-dashboard-welcome-font-size);
        }
        #edit-block {
          flex: 1;
        }
          #save {
            margin-bottom: 4px;
          }
      #toolbar {
        display: flex;
        justify-content: flex-end;
        margin-top: 20px;
      }
        #add-worksite {
          flex: 0;
          display: flex;
          justify-content: space-between;
          text-align: center;
          white-space: nowrap;
        }
          #add-worksite div {
            flex: 1;
          }
          #add-worksite sakai-icon {
            margin-right: 10px;
          }
      #motd {
        border-radius: var(--sakai-course-card-border-radius);
        background-color: var(--sakai-motd-bg-color);
        font-size: var(--sakai-motd-font-size);
        font-weight: var(--sakai-motd-font-weight);
        border: solid 1px #e0e0e0;
        padding: var(--sakai-motd-padding);
        margin-top: var(--sakai-motd-margin-top);
      }

        #motd-title-block {
          display: flex;
          align-items: center;
        }

        #motd-title {
          margin-right: 14px;
        }
        #motd-message {
          font-size: var(--sakai-motd-message-font-size);
          margin-top: 30px;
          padding-left: 20px;
        }
      #courses-and-widgets {
        display: flex;
        margin-top: 30px;
      }
        #courses {
          flex: 0;
          margin-right: 20px;
        }
        #widgets {
          flex: 1;
        }
          sakai-widget-panel {
            width: 100%;
          }

        #edit-block {
          flex: 1;
          text-align:right;
        }
        #edit-block div {
          display: inline-block;
        }
  `;
  */
}
