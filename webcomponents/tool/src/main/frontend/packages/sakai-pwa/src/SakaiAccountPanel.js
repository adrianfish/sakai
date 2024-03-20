import { SakaiElement } from "@sakai-ui/sakai-element";
import { html, nothing } from "lit";
import "@sakai-ui/sakai-picture-changer/sakai-picture-changer.js";

export class SakaiAccountPanel extends SakaiElement {

  static properties = {

    userId: { attribute: "user-id", type: String },
    userDisplayName: { attribute: "user-display-name", type: String },
    userName: { attribute: "user-name", type: String },
  };

  constructor() {

    super();

    this.loadTranslations("account-panel-wc").then(r => this._i18n = r);
  }

  _logout() { this.dispatchEvent(new CustomEvent("logout")); }

  shouldUpdate() {
    return this._i18n;
  }

  render() {

    return html`

      <sakai-picture-changer user-id="${this.userId}" dialog-title="balls"></sakai-picture-changer>
      <div class="text-center mb-3 pb-2">
        <div id="sakai-profile-image-block">
          <button class="btn only-icon" data-bs-toggle="modal" data-bs-target="#profile-image-upload">
            <img class="sakai-accountProfileImage rounded-circle"
                width="100"
                src="/direct/profile/${this.userId}/image/thumb"
                alt="Profile image" />
            <div id="sakai-profile-image-change">${this._i18n.change}</div>
          </button>
        </div>
        <h3 class="m-0 mt-2">${this.userDisplayName}</h3>
        <h4 class="fw-normal text-muted">${this.userName}</h4>
        <ul class="nav flex-column">
          ${this.prefsToolUrl ? html`
          <li class="nav-item"><a href="${this.prefsToolUrl}" class="nav-link">${this._i18n.manage_preferences}</a></li>
          ` : nothing}
          ${this.profileToolUrl ? html`
          <li class="nav-item"><a href="${this.profileToolUrl}" class="nav-link">${this._i18n.view_profile}</a></li>
          ` : nothing}
        </ul>
      </div>
      ${this._online ? html`
      <div class="text-center pt-3 mt-auto">
        <button type="button"
            class="pwa-header-button btn icon-button"
            @click=${this._logout}
            ?disabled=${!this.userId}>
          ${this._i18n.logout}
        </button>
      </div>
      ` : nothing}
    `;
  }
}
