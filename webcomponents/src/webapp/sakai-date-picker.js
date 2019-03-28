import {SakaiElement} from "./sakai-element.js";
import {html} from "./node_modules/lit-element/lit-element.js";

class SakaiDatePicker extends SakaiElement {

  static get properties() {

    return {
      input: { type: String },
      name: { type: String },
      useTime: { attribute: "use-time", type: Number },
      parseFormat: { attribute: "parse-format", type: String },
      getVal: { attribute: "get-val", type: String },
      asHidden: { attribute: "as-hidden", type: String },
    };
  }

  render() {

    return html`
      <input type="hidden" name="${this.name}" .value="MM/DD/YYYY" size="25" maxlength="25" />
      <input type="text" id="opendate" />
    `;
  }
}

customElements.define("sakai-date-picker", SakaiDatePicker);
