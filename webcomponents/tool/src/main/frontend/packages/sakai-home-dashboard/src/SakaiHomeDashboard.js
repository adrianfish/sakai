import { html, nothing } from "lit";
import { SakaiElement } from "@sakai-ui/sakai-element";
import { ifDefined } from "lit/directives/if-defined.js";
import { unsafeHTML } from "lit/directives/unsafe-html.js";
import { Signal } from "signal-polyfill";
import { loggedOut } from "@sakai-ui/sakai-signals";
import "@sakai-ui/sakai-icon/sakai-icon.js";
import "@sakai-ui/sakai-course-list/sakai-course-list.js";
import "@sakai-ui/sakai-widgets";
import "@sakai-ui/sakai-widgets/sakai-widget-panel.js";
import "@sakai-ui/sakai-button/sakai-button.js";

export class SakaiHomeDashboard extends SakaiElement {

  static properties = {

    userId: { attribute: "user-id", type: String },
    hideSites: { attribute: "hide-sites", type: Boolean },
    _data: { state: true },
    _showMotd: { state: true },
    _editing: { state: true },
  };

  constructor() {

    super();

    this.hideSites = true;

    this.loadTranslations("dashboard");

    this.logoutWatcher = new Signal.subtle.Watcher(() => {

      queueMicrotask(() => {

        if (loggedOut.get() === 1) {
          caches.open(this.cacheName).then(c => c.delete(`/api/users/${this.userId}/dashboard`));
        }

        this.logoutWatcher.watch();
      });
    });

    this.logoutWatcher.watch(loggedOut);
  }

  connectedCallback() {

    super.connectedCallback();

    this._loadData();
  }

  _userChanged() {
    this.loadTranslations({ bundle: "dashboard", lang: this._user.locale }).then(r => this._i18n = r);
  }

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
        if (this.cacheName && !this.siteId) {
          caches.open(this.cacheName).then(c => c.put(url, Response.json(r)));
        }
      })
      .catch(error => console.error(error));
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

  _toggleMotd() { this._showMotd = !this._showMotd; }

  shouldUpdate() {
    return this._i18n && this._data;
  }

  render() {

    return html`

      <div>
        <div class="d-flex flex-wrap align-items-center justify-content-between mb-2">
          <div class="fs-2 mb-md-2">${this._i18n.welcome} ${this._data.givenName}</div>
          <div class="d-flex justify-content-end">
          ${this._editing ? html`
              <div class="me-1">
                <button type="button"
                    class="btn btn-secondary"
                    @click=${this.save}
                    title="${this._i18n.save_tooltip}"
                    aria-label="${this._i18n.save_tooltip}">
                  ${this._i18n.save}
                </button>
              </div>
              <div>
                <button type="button"
                    class="btn btn-secondary"
                    @click=${this.cancel}
                    title="${this._i18n.cancel_tooltip}"
                    aria-label="${this._i18n.cancel_tooltip}">
                  ${this._i18n.cancel}
                </button>
              </div>
          ` : html`
            ${this._online ? html`
            <div>
              <button type="button"
                  class="btn btn-secondary"
                  @click=${this.edit}
                  title="${this._i18n.edit_tooltip}"
                  arial-label="${this._i18n.edit_tooltip}">
                ${this._i18n.edit}
              </button>
            </div>
            ` : nothing }
          `}
          </div>
        </div>
        ${this._data.worksiteSetupUrl ? html`
          <div class="d-flex justify-content-end mt-4">
            <sakai-button href="${this._data.worksiteSetupUrl}" title="${this._i18n.worksite_setup_tooltip}" aria-label="${this._i18n.worksite_setup_tooltip}">
              <div class="d-flex justify-content-between text-center">
                <div><sakai-icon type="add" size="small" class="me-3"></sakai-icon></div>
                <div>${this._i18n.worksite_setup}</div>
              </div>
            </sakai-button>
          </div>
        ` : nothing}
        ${this._data.motd ? html`
          <div class="p-3 mt-2 mb-3 border border-1 rounded-1 fs-5 fw-normal">
            <div class="d-flex mb-4 align-items-center" @click=${this._toggleMotd}>
              <div class="me-3">${this._i18n.motd}</div>
              <div>
                <a href="javascript:;"
                  title="${this._showMotd ? this._i18n.hide_motd_tooltip : this._i18n.show_motd_tooltip}"
                  aria-label="${this._showMotd ? this._i18n.hide_motd_tooltip : this._i18n.show_motd_tooltip}">
                  <sakai-icon type="${this._showMotd ? "up" : "down"}" size="small"></sakai-icon>
                </a>
              </div>
            </div>
            <div class="mt-4 ps-4 fs-6" style="display: ${this._showMotd ? "block" : "none"}">${unsafeHTML(this._data.motd)}</div>
          </div>
        ` : nothing}
        <div class="d-lg-flex">
          ${!this.hideSites ? html`
            <div class="me-lg-3 mb-4 mb-lg-0">
              <sakai-course-list user-id="${this.userId}"></sakai-course-list>
            </div>
          ` : nothing}
          <div class="w-100">
            <sakai-widget-panel
              @changed=${this.widgetLayoutChange}
              .widgetIds=${this._data.widgets}
              .layout=${this._data.layout}
              site-id=""
              user-id="${ifDefined(this.userId)}"
              cache-name="${ifDefined(this.cacheName)}"
              ?editing=${this._editing}>
            </sakai-widget-panel>
          </div>
        </div>
      </div>
    `;
  }
}
