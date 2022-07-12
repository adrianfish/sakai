import { SakaiElement } from "./sakai-element.js";
import { html } from "./assets/lit-html/lit-html.js";

/**
 * A simple wrapper for Sakai's profile picture.
 *
 * Usage:
 *
 * Renders adrian's profile picture and pops up the profile panel when clicked.
 * <sakai-user-photo user-id="adrian">
 *
 * Renders adrian's profile picture without a popup
 * <sakai-user-photo user-id="adrian" no-popup>
 *
 * Renders adrian's profile picture with a popup and some custom styles from the supplied class.
 * <sakai-user-photo user-id="adrian" classes="custom">
 * @element sakai-user-photo
 * @property {string} user-id - A Sakai user id
 * @property {string} [classes] - Extra classes to style the content
 * @property {string} [popup] By default, profile popups are off. Set this to "on" if you want them
 * @property {boolean} [official] Set this if you want the official Sakai photo
 */
class SakaiUserPhoto extends SakaiElement {

  constructor() {

    super();

    this.classes = "large-thumbnail";
    this.popup = SakaiUserPhoto.OFF;
  }

  static get properties() {

    return {
      userId: { attribute: "user-id", type: String },
      classes: { type: String },
      popup: { type: String },
      official: { type: Boolean },
    };
  }

  attributeChangedCallback(name, oldValue, newValue) {

    super.attributeChangedCallback(name, oldValue, newValue);

    if (this.userId) {
      this.generatedId = `sakai-user-photo-${this.userId}-${Math.floor(Math.random() * 100)}`;

      this.url = `/direct/profile/${this.userId}/image/${this.official ? "official" : "thumb"}`
                  + (this.siteId && `?siteId=${this.siteId}`);
    }

    if (this.popup == SakaiUserPhoto.ON && this.generatedId) {
      this.updateComplete.then(() => {
        profile.attachPopups($(`#${this.generatedId}`));
      });
    }
  }

  shouldUpdate() {
    return this.userId;
  }

  render() {

    return html`
      <div id="${this.generatedId}"
          data-user-id="${this.userId}"
          class="sakai-user-photo ${this.classes}"
          style="background-image:url(/direct/profile/${this.userId}/image/${this.official ? "official" : "thumb"}) ${this.noPopup ? "" : ";cursor: pointer;"}">
    `;
  }
}

SakaiUserPhoto.OFF = "off";
SakaiUserPhoto.ON = "on";

const tagName = "sakai-user-photo";
!customElements.get(tagName) && customElements.define(tagName, SakaiUserPhoto);
