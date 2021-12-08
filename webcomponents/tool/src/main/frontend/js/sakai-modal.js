import { html } from "./assets/lit-element/lit-element.js";
import { SakaiElement } from "./sakai-element.js";

export class SakaiModal extends SakaiElement {

  /*
  static get properties() {

    return {
      i18n: { attribute: false, type: Object },
    };
  }
  */

  constructor() {
    super();
  }

  trigger() {}
  title() {}
  content() {}
  //buttons() {}

  render() {

    return html`
      ${this.trigger()}
      <div id="balls" class="modal" tabindex="-1" role="dialog">
        <div class="sakai-modal modal-dialog" role="document">
          <div class="sakai-modal-content modal-content">
            <div class="sakai-modal-header modal-header">
              <div id="titlebar">
                <div id="title">${this.title()}</div>
                <div id="close">
                  <a href="javascript:;" aria-label="close" data-dismiss="modal">
                    <sakai-icon type="close"></sakai-icon>
                  </a>
                </div>
              </div>
            </div>
            <div class="sakai-modal-body modal-body">
              ${this.content()}
            </div>
            <div class="modal-footer">
              ${this.buttons()}
            </div>
          </div>
        </div>
      </div>
    `;
  }
}

const tagName = "sakai-modal";
!customElements.get(tagName) && customElements.define(tagName, SakaiModal);
