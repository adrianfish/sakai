import { html, LitElement } from "../assets/lit-element/lit-element.js";

/**
 * We're extending LitElement here, so our markup will be in the shadow dom. Shadow dom is a double
 * edged sword. It gives us scoped styling which means we can keep our ids and classnames short, but
 * it doesn't work too well with things like jQuery dialogs or CKEditor which expect to be able to
 * put markup in the body, etc.
 */
class SakaiOpenApereo extends LitElement {

  render() {

    return html`
      <h1>Welcome to Open Apereo</h1>
    `;
  }
}

const tagName = "sakai-openapereo";
!customElements.get(tagName) && customElements.define(tagName, SakaiOpenApereo);
