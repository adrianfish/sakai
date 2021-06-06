import { html, LitElement } from "../assets/lit-element/lit-element.js";
import { loadProperties } from "../sakai-i18n.js";

/**
 * We're extending LitElement here, so our markup will be in the shadow dom. Shadow dom is a double
 * edged sword. It gives us scoped styling which means we can keep our ids and classnames short, but
 * it doesn't work too well with things like jQuery dialogs or CKEditor which expect to be able to
 * put markup in the body, etc.
 */
class SakaiOpenApereo extends LitElement {

  constructor() {

    super();

    loadProperties("openapereo").then(r => this.i18n = r);
  }

  shouldUpdate() {
    return this.i18n;
  }

  render() {

    return html`
      <h1>${this.i18n["welcome"]}</h1>
    `;
  }
}

const tagName = "sakai-openapereo";
!customElements.get(tagName) && customElements.define(tagName, SakaiOpenApereo);
