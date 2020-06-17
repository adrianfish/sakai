import { html, css, LitElement } from './assets/lit-element/lit-element.js';
import { unsafeHTML } from './assets/lit-html/directives/unsafe-html.js';

export class SakaiMultiColumnLayout extends LitElement {

  static get styles() {

    return css`
      :host {
        display: block;
        width: var(--sakai-multi-column-layout-width, 600px);
        font-family: var(--sakai-font-family, roboto, arial, sans-serif);
        background-color: white;
      }

      #grid {
        display: flex;
        justify-content: space-between;
      }
        .column {
          flex: 1;
        }
          .row {
          }
    `;
  }

  static get properties() {

    return {
      columns: {type: Array},
    };
  }

  constructor() {

    super();
  }

  /*
  set columnsAndRows(value) {

    this._columnsAndRows = value;

    if (!this.content) {
      this.content = [];
      for (let row = 0;row < value.rows; row++) {
        for (let col = 0;col < value.cols; col++) {
          this.content.push({ row: row, col: col, content: `<div>Add some content here</div>` });
        }
      }
      console.log(this.content);
    }
  }

  get columnsAndRows() { return this._columnsAndRows; }
  */

  render() {

    return html`
      <div id="grid">
        ${this.columns.map(c => html`
          <div class="column">
            ${c.rows.map(r => html`<div class="row">${unsafeHTML(r.content)}</div>`)}
          </div>
        `)}
      </div>
    `;
  }
}

if (!customElements.get("sakai-multicolumn-layout")) {
  customElements.define("sakai-multicolumn-layout", SakaiMultiColumnLayout);
}
