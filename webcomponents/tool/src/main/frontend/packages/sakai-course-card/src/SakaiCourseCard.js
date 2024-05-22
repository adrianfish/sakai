import { html } from "lit";
import { ifDefined } from "lit-html/directives/if-defined.js";
import { SakaiElement } from "@sakai-ui/sakai-element";
import { pushSetupComplete, registerPushCallback } from "@sakai-ui/sakai-push-utils";
import "@sakai-ui/sakai-icon";

export class SakaiCourseCard extends SakaiElement {

  static properties = {

    courseData: { attribute: "course-data", type: Object },

    _i18n: { state: true },
  };

  constructor() {

    super();

    this.loadTranslations("coursecard").then(r => this._i18n = r);
  }

  connectedCallback() {

    super.connectedCallback();

    pushSetupComplete.then(() => {

      if (Notification.permission !== "granted") return;

      registerPushCallback("notifications", message => {

        this.courseData.tools.forEach(t => {

          if (t.id === message.tool) t.hasAlerts = true;
          this.requestUpdate();
        });
      });
    })
    .catch(error => console.error(error));
  }

  _togglePinned(e) {

    e.preventDefault();

    this.courseData.pinned = !this.courseData.pinned;

    const detail = { siteId: this.courseData.siteId, pinned: this.courseData.pinned, title: this.courseData.title };

    fetch("/portal/favorites/update", {
      method: "POST",
      body: new URLSearchParams(detail),
    }).then(r => {

      if (r.ok) {
        document.body.dispatchEvent(new CustomEvent("site-pin-changed", { detail, bubbles: true }));
        this.requestUpdate();
      } else {
        this.courseData.pinned = !this.courseData.pinned;
      }
    });
  }

  shouldUpdate() {
    return this._i18n && this.courseData;
  }

  render() {

    console.log("asdfasdf");

    return html`
      <div class="info-block" style="background: linear-gradient( rgba(0, 0, 0, 0.5), rgba(0, 0, 0, 0.5) ), url(${ifDefined(this.courseData.image)})">
        <div class="d-flex align-items-center">
          <div>
            <i class="si ${this.courseData.pinned ? "si-pin-fill" : "si-pin"}" @click=${this._togglePinned}></i>
          </div>
          <div>
            <a href="${this.courseData.url}" title="${this._i18n.visit} ${this.courseData.title}">
              <div class="ms-2">${this.courseData.title}</div>
            </a>
          </div>
        </div>
        <a href="${this.courseData.url}" title="${this._i18n.visit} ${this.courseData.title}">
          <div class="code-block">${this.courseData.code}</div>
        </a>
      </div>
      <div class="tool-alerts-block">
        ${this.courseData.tools.filter(tool => tool.hasAlerts).map(tool => html`
          <div class="d-inline-block">
            <a href="${tool.url}" title="${tool.title}" style="position: relative">
              <i class="si ${tool.iconClass}"></i>
              <span class="portal-notifications-indicator p-1 rounded-circle"><span class="visually-hidden">sdfs</span></span>
            </a>
          </div>
        `)}
      </div>
    `;
  }
}
