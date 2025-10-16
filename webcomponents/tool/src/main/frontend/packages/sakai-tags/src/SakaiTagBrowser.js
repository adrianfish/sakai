import { html } from "lit";
import { SakaiElement } from "@sakai-ui/sakai-element";

export class SakaiTagBrowser extends SakaiElement {

  static properties = {
    siteId: { attribute: "site-id", type: String },
  };

  connectedCallback() {

    super.connectedCallback();

    if (this.reference) {
      const url = `/api/tags/associations?reference=${this.reference}`;
      fetch(url)
        .then(r => {
          if (r.ok) {
            return r.json();
          }
          throw new Error(`Failed to get tag associations at ${url}`);
        })
        .then(data => this._selectedTags = data)
        .catch (error => console.error(error));
    }
  }

  render() {

    return html`
      <div class="d-flex">
        <div>
          ${this._selectedTags.map(t => html`
            <span>${t.label}</span>
          `)}
        </div>
        <div>
        <button type="button" class="btn btn-small"data-bs-toggle="modal" data-bs-target="#exampleModal">Add</button>
      </div>

      <div class="modal fade" id="exampleModal" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h1 class="modal-title fs-5" id="exampleModalLabel">Modal title</h1>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
              <sakai-tag-browser @tag-selected=${this._tagSelected}></sakai-tag-browser>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
              <button type="button" class="btn btn-primary">Save changes</button>
            </div>
          </div>
        </div>
      </div>
    `;
  }
}
