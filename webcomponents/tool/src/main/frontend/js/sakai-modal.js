import { html } from "../assets/lit-element/lit-element.js";
import { SakaiElement } from "../sakai-element.js";
import "../sakai-icon.js";

export class SakaiModal extends SakaiElement {

  /*
  static get properties() {

    return {
      i18n: { attribute: false, type: Object },
    };
  }
  */

  /*
  constructor() {
    super();
  }
  */

  trigger() {}
  title() {}
  content() {}
  //buttons() {}

  render() {

    console.log("render");

    return html`
      ${this.trigger()}
      <div>
        <a
            href="javascript:;"
            title="${this.i18n.add_task}"
            data-toggle="modal"
            data-target="#balls"
            aria-label="${this.i18n.add_task}">
          <sakai-icon type="add" size="small">
        </a>
      </div>
      <div id="balls" class="modal" tabindex="-1" role="dialog">
        <div class="modal-dialog" role="document">
          <div class="modal-content">
            <div class="modal-header">
              ${this.title()}
              <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                <span aria-hidden="true">&times;</span>
              </button>
            </div>
            <div class="modal-body">
              ${this.content()}
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-primary">Save changes</button>
              <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
            </div>
          </div>
        </div>
      </div>
    `;
  }
}

const tagName = "sakai-modal";
!customElements.get(tagName) && customElements.define(tagName, SakaiModal);
