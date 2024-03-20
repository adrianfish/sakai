import { SakaiElement } from "@sakai-ui/sakai-element";
import { html, nothing } from "lit";
import "@sakai-ui/sakai-notifications/sakai-notifications.js";
import "@sakai-ui/sakai-home-dashboard/sakai-home-dashboard.js";
import "../sakai-account-panel.js";
import "@sakai-ui/sakai-login/sakai-login.js";
import { userChanged, loggedOut } from "@sakai-ui/sakai-signals";
import { logout, onLogin } from "@sakai-ui/sakai-push-utils";

export class SakaiPWA extends SakaiElement {

  static properties = {

    _userId: { state: true },
    _userEid: { state: true },
    _userDisplayName: { state: true },
    _userTimezone: { state: true },
    usePortalSearch: { attribute: "use-portal-search", type: String },
    _showMotd: { state: true },
    _state: { state: true },
    _deviceRegistered: { state: true },
  };

  constructor() {

    super();

    window.addEventListener("online", () => {

      this._pendingCommands.forEach(command => command());
      this._pendingCommands = [];
    });

    this.loadTranslations("sakai-pwa").then(r => this._i18n = r);

    this._pendingCommands = [];
  }

  connectedCallback() {

    super.connectedCallback();

    console.debug("connectedCallback");

    const userString = localStorage.getItem("sakai-user");
    if (!this._userId && userString) {
      const user = JSON.parse(userString);
      this._loginSuccessful({ detail: { user } });
    }

    if (this._online) {
      if (!this._userId) {
        loggedOut.set(1);
        this._state = SakaiPWA.SPLASH;
      } else {
        this._state = SakaiPWA.DASHBOARD;
      }
    }
  }

  _loginSuccessful(e) {

    // Logged in successfully.
    onLogin(e.detail.user.id);

    this._userId = e.detail.user.id;
    this._userEid = e.detail.user.eid;
    this._userDisplayName = e.detail.user.displayName;
    this._userTimezone = e.detail.user.timezone;

    this._state = SakaiPWA.DASHBOARD;

    loggedOut.set(0);

    this.updateComplete.then(() => this.querySelector("sakai-notifications").loadNotifications());

    userChanged.set(e.detail.user);

    localStorage.setItem("sakai-user", JSON.stringify(e.detail.user));
  }

  _setState(e) { this._state = e.target.dataset.state; }

  _toggleMotd() { this._showMotd = !this._showMotd; }

  _logoutCommand() {

    const url = "/api/logout";
    fetch(url, { credentials: "include" })
    .then(r => {

      if (r.ok) {
        logout();
        this._userId = undefined;
        this._state = SakaiPWA.SPLASH;
        navigator.setAppBadge?.(0);
        loggedOut.set(1);
        userChanged.set({});
        localStorage.removeItem("sakai-user");
      } else {
        throw Error(`Error while logging in at ${url}`);
      }
    })
    .catch(error => console.error(error));
  }

  _logout() {

    bootstrap.Offcanvas.getInstance(document.getElementById("sakai-account-panel")).hide();

    if (this._online) {
      this._logoutCommand();
    } else {
      this._userId = undefined;
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
      <header id="pwa-header" class="d-flex align-items-center p-2">
        <div id="pwa-header-logo" class="mx-auto">
          <img src="/library/skin/default-skin/images/sakaiLogo.png" alt="Sakai Logo">
        </div>
      </header>
    `;
  }

  _renderFooter() {

    return html`
      <div id="pwa-footer" class="bg-dark d-flex align-items-center justify-content-around py-2 pb-4">
        ${this._userId ? html`
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

          ${this._userId ? html`
          <button class="btn icon-button sak-sysInd-account"
              type="button"
              data-bs-toggle="offcanvas"
              data-bs-target="#sakai-account-panel"
              aria-controls="sakai-account-panel"
              title="${this._i18n.account_panel_title}">
            <img id="profile-image" class="rounded-circle"
                src="/direct/profile/${this._userId}/image/thumb"
                alt="${this._i18n.profile_image_alt}" />
          </button>
          ` : nothing}
        ` : html`
          ${this._state !== SakaiPWA.LOGIN ? html`
          <button type="button"
              class="btn btn-lg btn-primary fs-2"
              data-state="${SakaiPWA.LOGIN}"
              @click=${this._setState}>
            ${this._i18n.login}
          </button>
          ` : nothing}
        `}
      </div>
    `;
  }

  _renderContent() {

    return html`
      <div class="p-3">
        ${this._state === SakaiPWA.DASHBOARD ? html`
          <sakai-home-dashboard user-id="${this._userId}" cache-name="sakai-v1" hide-sites></sakai-home-dashboard>
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

      ${this._userId ? html`
        <aside class="offcanvas offcanvas-end" tabindex="-1" id="sakai-account-panel" aria-labelledby="sakai-account-panel-title">
          <div class="offcanvas-header">
            <h2 class="offcanvas-title m-0" id="sakai-account-panel-title">${this._i18n.account_panel_title}</h2>
            <span>&nbsp;</span>
            <button type="button" class="btn-close text-reset" data-bs-dismiss="offcanvas" aria-label="Close this account menu"></button>
          </div>

          <div class="offcanvas-body d-flex flex-column">
            <sakai-account-panel user-id="${this._userId}"
                @logout=${this._logout}
                user-name="${this._userEid}"
                user-display-name="${this._userDisplayName}">
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
                user-id="${this._userId}"
                url="/api/users/me/notifications"
                @notifications-loaded=${this._notificationsLoaded}
                ?offline=${!this._online}
                cache-name="sakai-v1"
                precache-thumbnails>
            </sakai-notifications>
          </div>
        </aside>
      ` : nothing}

      ${!this._userId ? this._renderHeader() : nothing}

      <main id="pwa-main">
        ${!this._online ? html`
          <div class="sak-banner-info">You are offline</div>
          ${!this._userId ? html`
            <div class="sak-banner-warn">You are not logged in. You need to go online first.</div>
          ` : this._renderContent() }
        ` : html`
            ${this._state === SakaiPWA.LOGIN ? html`
              <sakai-login @login-successful=${this._loginSuccessful} no-header></sakai-login>
            ` : this._renderContent()
            }
        `}
      </main>

      ${this._renderFooter()}
    `;
  }
}

SakaiPWA.DASHBOARD = "dashboard";
SakaiPWA.LOGIN = "login";
SakaiPWA.SPLASH = "splash";
