import { SakaiElement } from "@sakai-ui/sakai-element";
import { html, nothing } from "lit";
import { unsafeHTML } from "lit-html/directives/unsafe-html.js";
import "@sakai-ui/sakai-notifications/sakai-notifications.js";
import "@sakai-ui/sakai-home-dashboard/sakai-home-dashboard.js";
import "../sakai-account-panel.js";
import "@sakai-ui/sakai-login/sakai-login.js";
import { Signal } from "signal-polyfill";
import { logout, onLogin } from "@sakai-ui/sakai-push-utils";

export class SakaiPWA extends SakaiElement {

  static properties = {

    userId: { attribute: "user-id", type: String },
    userName: { attribute: "user-name", type: String },
    userDisplayName: { attribute: "user-display-name", type: String },
    userTimezone: { type: String },
    usePortalSearch: { attribute: "use-portal-search", type: String },
    _showMotd: { state: true },
    _offline: { state: true },
    _state: { state: true },
    _i18n: { state: true },
  };

  constructor() {

    super();

    this._offline = !navigator.onLine;

    window.addEventListener("online", () => {

      this._offline = false;
      this._pendingCommands.forEach(command => command());
      this._pendingCommands = [];
    });

    window.addEventListener("offline", () => this._offline = true);

    this.loadTranslations("sakai-pwa").then(r => this._i18n = r);

    this._pendingCommands = [];
  }

  connectedCallback() {

    super.connectedCallback();

    if (!this.userId) {
      SakaiPWA.loggedOutSignal.set(1);
      this._state = SakaiPWA.SPLASH;
    } else {
      SakaiPWA.loggedOutSignal.set(0);
      this._state = SakaiPWA.DASHBOARD;
    }
  }

  _loginSuccessful(e) {

    // Logged in successfully.
    onLogin(e.detail.user.userId);

    this.userId = e.detail.user.userId;
    this.userName = e.detail.user.userName;
    this.userDisplayName = e.detail.user.userDisplayName;
    this.userTimezone = e.detail.user.userTimezone;

    // TODO: this is nasty ...
    portal.user.timezone = this.userTimezone;

    this._state = SakaiPWA.DASHBOARD;

    SakaiPWA.loggedOutSignal.set(0);

    this.updateComplete.then(() => this.querySelector("sakai-notifications").loadNotifications());
  }

  _setState(e) { this._state = e.target.dataset.state; }

  _toggleMotd() { this._showMotd = !this._showMotd; }

  _logoutCommand() {

    const url = "/api/logout";
    fetch(url, { credentials: "include" })
    .then(r => {

      if (r.ok) {
        logout();
        this.userId = undefined;
        this._state = SakaiPWA.SPLASH;
        navigator.setAppBadge?.(0);
        SakaiPWA.loggedOutSignal.set(1);
      } else {
        throw Error(`Error while logging in at ${url}`);
      }
    })
    .catch(error => console.error(error));
  }

  _logout() {

    bootstrap.Offcanvas.getInstance(document.getElementById("sakai-account-panel")).hide();

    if (!this._offline) {
      this._logoutCommand();
    } else {
      this.userId = undefined;
      this._pendingCommands.push(this._logoutCommand.bind(this));
    }
  }

  _notificationsLoaded(e) {

    const indicator = this.querySelector(".portal-notifications-indicator");
    indicator.classList.remove("d-none");
    indicator.style.display = e.detail.count > 0 ? "inline" : "none";
  }

  shouldUpdate() {
    return this._i18n;
  }

  firstUpdated() {
    this._displayNotificationsButton = Notification.permission === "default";
  }

  _renderHeader() {

    return html`
      <div id="pwa-header" class="d-flex align-items-center justify-content-between">
        <div>
          <button type="button"
              class="pwa-header-button btn icon-button"
              ?disabled=${this._offline || !this.userId}>
            <span class="bi bi-list"></span>
          </button>
        </div>
        <div class="d-flex justify-content-center">
          <div id="pwa-header-logo">
            <img src="/library/skin/default-skin/images/sakaiLogo.png" alt="Sakai Logo">
          </div>
        </div>
        <div>
        ${!this._offline && !this.userId ? html`
          <!-- WE ARE ONLINE BUT NOT LOGGED IN -->

          <button type="button"
              class="btn btn-link"
              data-state="${SakaiPWA.LOGIN}"
              @click=${this._setState}
              ?disabled=${this._state === SakaiPWA.LOGIN}>
            Login
          </button>
        ` : nothing }

          ${this.userId ? html`
          <button class="btn icon-button sak-sysInd-account"
              type="button"
              data-bs-toggle="offcanvas"
              data-bs-target="#sakai-account-panel"
              aria-controls="sakai-account-panel"
              title="${this._i18n.account_panel_title}">
            <img id="profile-image" class="rounded-circle"
                src="/direct/profile/${this.userId}/image/thumb"
                alt="${this._i18n.profile_image_alt}" />
          </button>
          ` : nothing}
        </div>
      </div>
    `;
  }

  _renderFooter() {

    return html`
      <div id="pwa-footer" class="bg-dark d-flex align-items-center justify-content-around py-2">
        <div>
          <button class="btn icon-button responsive-allsites-button"
              data-bs-toggle="offcanvas"
              data-bs-target="#select-site-sidebar"
              aria-label="${this._i18n.allsites}"
              aria-controls="select-site-sidebar"
              title="${this._i18n.allsites}">
              <i class="si si-all-sites"></i>
          </button>
        </div>

        <div>
          <button class="portal-notifications-button btn icon-button"
              data-bs-toggle="offcanvas"
              data-bs-target="#sakai-notifications-panel"
              aria-controls="sakai-notifications-panel"
              aria-label="${this._i18n.notifications}"
              title="${this._i18n.notifications}">
            <i class="bi-bell"></i>
            <span class="portal-notifications-indicator p-1 rounded-circle d-none">
              <span class="visually-hidden">${this._i18n.new_notifications_label}</span>
            </span>
          </button>
        </div>

        <div>
          <a class="btn icon-button"
              href="/portal"
              title="${this._i18n.portal_home}">
            <i class="bi-house"></i>
          </a>
        </div>

        ${this.usePortalSearch ? html`
          <div>
            <button class="btn icon-button portal-search-button"
                data-bs-toggle="offcanvas"
                data-bs-target="#sakai-search-panel"
                aria-controls="sakai-search-panel"
                aria-label="${this._i18n.search_for_content}"
                title="${this._i18n.search_for_content}">
              <i class="bi-search"></i>
            </button>
          </div>
        ` : nothing}
      </div>
    </div>
    `;
  }

  _renderContent() {

    return html`
      <div class="p-4">
        ${this._state === SakaiPWA.DASHBOARD ? html`
          <sakai-home-dashboard user-id="${this.userId}" cache-name="sakai-v1" hide-sites></sakai-home-dashboard>
        ` : nothing }
        ${this._state === SakaiPWA.SPLASH ? html`
          <div class="text-center mt-5">
            <img src="/images/sakaiger_512.png" />
          </div>
        ` : nothing }
      </div>
    `;
  }

  render() {

    return html`

      <aside class="offcanvas offcanvas-end" tabindex="-1" id="sakai-account-panel" aria-labelledby="sakai-account-panel-title">
        <div class="offcanvas-header">
          <h2 class="offcanvas-title m-0" id="sakai-account-panel-title">${this._i18n.account_panel_title}</h2>
          <span>&nbsp;</span>
          <button type="button" class="btn-close text-reset" data-bs-dismiss="offcanvas" aria-label="Close this account menu"></button>
        </div>

        <div class="offcanvas-body d-flex flex-column">
          <sakai-account-panel user-id="${this.userId}"
              @logout=${this._logout}
              user-name="${this.userName}"
              user-display-name="${this.userDisplayName}">
          </sakai-account-panel>
        </div>
      </aside>

      <aside class="offcanvas offcanvas-end" tabindex="-1" id="sakai-notifications-panel" aria-labelledby="sakai-notifications-panel-title">
        <div class="offcanvas-header">
          <h2 class="offcanvas-title m-0" id="sakai-notifications-panel-title">${this._i18n.notifications}</h2>
          <span>&nbsp;</span>
          <button type="button" class="btn-close text-reset" data-bs-dismiss="offcanvas" aria-label="Close this notifications menu"></button>
        </div>

        <div class="offcanvas-body d-flex flex-column">
        <sakai-notifications
            user-id="${this.userId}"
            url="/api/users/me/notifications"
            @notifications-loaded=${this._notificationsLoaded}
            ?offline=${this._offline}
            cache-name="sakai-v1"
            defer-load>
        </sakai-notifications>
        </div>
      </aside>

      ${this._renderHeader()}

      <div id="pwa-main">
        ${this._offline ? html`
          <div class="sak-banner-info">You are offline</div>
          ${!this.userId ? html`
            <div class="sak-banner-warn">You are not logged in. You need to go online first.</div>
          ` : this._renderContent() }
        ` : nothing }

        ${this._state === SakaiPWA.LOGIN ? html`
          <sakai-login @login-successful=${this._loginSuccessful} no-header></sakai-login>
        ` : html `
          ${this._renderContent()}
        `}
      </div>

      ${this._renderFooter()}
    `;
  }
}

SakaiPWA.DASHBOARD = "dashboard";
SakaiPWA.LOGIN = "login";
SakaiPWA.SPLASH = "splash";
SakaiPWA.loggedOutSignal = new Signal.State(1);
