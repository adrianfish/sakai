import { SakaiElement } from "@sakai-ui/sakai-element";
import { html, nothing } from "lit";
import "@sakai-ui/sakai-notifications/sakai-notifications.js";
import "@sakai-ui/sakai-login/sakai-login.js";
import { logout, onLogin } from "@sakai-ui/sakai-push-utils";

export class SakaiPWA extends SakaiElement {

  static properties = {

    userId: { attribute: "user-id", type: String },
    usePortalSearch: { attribute: "use-portal-search", type: String },
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

    this._state = SakaiPWA.NOTIFICATIONS;

    this.loadTranslations("sakai-pwa").then(r => this._i18n = r);

    this._pendingCommands = [];
  }

  _loginSuccessful(e) {

    // Logged in successfully.
    onLogin(e.detail.userId);

    this.userId = e.detail.userId;
  }

  _setState(e) {

    switch (e.target.dataset.state) {
      case SakaiPWA.NOTIFICATIONS:
        this._state = SakaiPWA.NOTIFICATIONS;
        break;
      case SakaiPWA.TASKS:
        this._state = SakaiPWA.TASKS;
        break;
      default:
    }
  }

  _logoutCommand() {

    const url = "/api/logout";
    fetch(url, { credentials: "include" })
    .then(r => {

      if (r.ok) {
        logout();
        this.userId = undefined;
      } else {
        throw Error(`Error while logging in at ${url}`);
      }
    })
    .catch(error => console.error(error));
  }

  _logout() {

    if (!this._offline) {
      this._logoutCommand();
    } else {
      this.userId = undefined;
      this._pendingCommands.push(this._logoutCommand.bind(this));
    }
  }

  _notificationsLoaded(e) {

    document.body.querySelectorAll(".portal-notifications-indicator").forEach(i => {

      if (Notification.permission === "granted") {
        i.classList.remove("d-none");
        i.style.display = e.detail.count > 0 ? "inline" : "none";
      }
    });
  }

  shouldUpdate() {
    return this._i18n;
  }

  firstUpdated() {
    this._displayNotificationsButton = Notification.permission === "default";
  }

  _renderNotifications() {

    return html`
      <div class="d-flex justify-content-center">
        <h2 class="text-center">${this._i18n.notifications}</h2>
      </div>
      <div class="p-2">
        <sakai-notifications user-id="${this.userId}" url="/direct/portal/notifications.json" @notifications-loaded=${this._notificationsLoaded} ?offline=${this._offline}></sakai-notifications>
      </div>
    `;
  }

  _renderTasks() {

    return html`
      <h2 class="text-center">${this._i18n.tasks}</h2>
    `;
  }

  _renderHeader() {

    return html`
      <div id="pwa-header" class="d-flex align-items-center justify-content-between">
        <div>
          <button type="button"
              class="pwa-header-button btn icon-button"
              data-bs-toggle="offcanvas"
              data-bs-target="#pwa-menu"
              aria-controls="pwa-menu"
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
          <button type="button"
              class="pwa-header-button btn icon-button"
              @click=${this._logout}
              ?disabled=${!this.userId}>
            <span class="bi bi-box-arrow-right"></span>
          </button>
        </div>
      </div>
    `;
  }

  _renderFooter() {

    return html`
      <div id="pwa-footer" class="bg-dark d-flex align-items-center justify-content-around">
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
              data-bs-target="#sakai-notificationsPanel"
              aria-controls="sakai-notificationsPanel"
              aria-label="${this._i18n.notifications}"
              title="${this._i18n.notifications}">
            <i class="bi-bell"></i>
            <i class="portal-notifications-no-permissions-indicator si si-warning d-none"
               aria-label="${this._i18n.notifications_not_permitted_label}">
            </i>
            <span class="portal-notifications-indicator p-1 bg-danger rounded-circle d-none">
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
      ${this._state === SakaiPWA.NOTIFICATIONS ? this._renderNotifications() : nothing }
      ${this._state === SakaiPWA.TASKS ? this._renderTasks() : nothing }
    `;
  }

  render() {

    return html`

      <div class="offcanvas offcanvas-start" tabindex="-1" id="pwa-menu" aria-labelledby="pwa-toolmenu-title">
        <div id="pwa-activities-header" class="offcanvas-header">
          <h5 class="offcanvas-title" id="pwa-toolmenu-title">Activities</h5>
          <button type="button" class="btn-close text-reset" data-bs-dismiss="offcanvas" aria-label="Close"></button>
        </div>
        <div class="offcanvas-body">
          <div>
            <button type="button"
                class="btn btn-transparent ${this._state === SakaiPWA.NOTIFICATIONS ? "fw-bold" : ""}"
                data-bs-dismiss="offcanvas"
                data-state="${SakaiPWA.NOTIFICATIONS}"
                @click=${this._setState}>
              Notifications
            </button>
          </div>
          <div>
            <button type="button"
                class="btn btn-transparent ${this._state === SakaiPWA.TASKS ? "fw-bold" : ""}"
                data-bs-dismiss="offcanvas"
                data-state="${SakaiPWA.TASKS}"
                @click=${this._setState}>
              Tasks
            </button>
          </div>
        </div>
      </div>

      ${this._renderHeader()}

      <div id="pwa-main">
        ${this._offline ? html`
          <div class="sak-banner-info">You are offline</div>
          ${!this.userId ? html`
            <div class="sak-banner-warn">You are not logged in. You need to go online first.</div>
          ` : this._renderContent() }
        ` : nothing }

        ${!this._offline ? html`
          ${!this.userId ? html`
            <!-- WE ARE ONLINE BUT NOT LOGGED IN -->
            <sakai-login @login-successful=${this._loginSuccessful} no-header></sakai-login>
          ` : html `
            <!-- WE ARE ONLINE AND LOGGED IN -->
            ${this._renderContent()}
          `}
        ` : nothing }
      </div>

      ${this._renderFooter()}
    `;
  }
}

SakaiPWA.NOTIFICATIONS = "notifications";
SakaiPWA.TASKS = "tasks";
