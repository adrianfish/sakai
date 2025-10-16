import { html } from "lit";
import { SakaiElement } from "@sakai-ui/sakai-element";

export class SakaiViewTag extends SakaiElement {

  static properties = {
    tag: { type: Object },
  };

  constructor() {

    super();

    this.loadTranslations("tagservice");
  }

  _returnToCollection() {
    this.dispatchEvent(new CustomEvent("tag-viewed"));
  }

  shouldUpdate() {
    return this._i18n && this.tag;
  }

  render() {

    return html`
      <h3>${this.tr("viewing_tag", { label: this.tag.label })}</h3>

      <h4>${this.tag.label}</h4>

      <div class="my-2">${this.tag.description}</div>

      <button @click=${this._returnToCollection} class="btn btn-primary btn-sm mt-2">${this._i18n.Return}</button>
    `;
  }
}
