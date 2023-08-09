import { html } from "../assets/lit-element/lit-element.js";
import { SakaiElement } from "../sakai-element.js";

export class SakaiPages extends SakaiElement {

  static get properties() {

    return {
    };
  }

  render() {

    return html`
      <h1>Welcome to the Pages tool !</h1>
    `;
  }
}

!customElements.get("sakai-pages") && customElements.define("sakai-pages", SakaiPages);
