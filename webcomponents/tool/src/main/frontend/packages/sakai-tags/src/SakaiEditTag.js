import { html, nothing } from "lit";
import { SakaiElement } from "@sakai-ui/sakai-element";

export class SakaiEditTag extends SakaiElement {

  static properties = {
    tag: { type: Object },
    collectionId: { attribute: "collection-id", type: String },
    collectionName: { attribute: "collection-name", type: String },
  };

  constructor() {

    super();

    this.tag = {};

    this.loadTranslations("tagservice");
  }

  _save() {

    const data = new FormData(this.querySelector("#create-tag-form"));

    const id = data.get("id");
    const url = `/api/tags/collections/${this.collectionId}/tags${id ? `/${id}` : ""}`;
    fetch(url, {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(Object.fromEntries(data)),
    })
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw new Error(`Network error while saving tag at ${url}`);
    })
    .then(collection => {
      this.dispatchEvent(new CustomEvent("tag-edited", { detail: collection }));
    });
  }

  _cancel() {
    this.dispatchEvent(new CustomEvent("cancel-edit-tag"));
  }

  shouldUpdate() {
    return this._i18n;
  }

  render() {

    return html`
      <header class="tagservice-section-header">
        <h3>
          <span>${this._i18n.tag}</span>
          <span class="incollection">
            (${this._i18n.tagcollection}:
            <span>${this.collectionName}</span>)
          </span>
        </h3>
      </header>

      <div class="container-fluid">
        <form id="create-tag-form" class="form-horizontal">
          <div class="form-group">
            <label class="col-sm-2 control-label" for="label">${this._i18n.label}</label>
            <div class="col-sm-10">
              <input class="form-control"
                  type="text"
                  name="label"
                  id="label"
                  .value=${this.tag?.label || ""}
                  required
                  ?readonly=${this.tag?.externalcreation} />
              ${this._labelError ? html`
                <div class="text-danger">${this._labelError}</div>
              ` : nothing}
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label" for="description">${this._i18n.description}</label>
            <div class="col-sm-10">
              <textarea class="form-control"
                  name="description"
                  id="description"
                  .value=${this.tag?.description || ""}
                  ?readonly=${this.tag?.externalcreation}>
              </textarea>
            </div>
          </div>

          <div class="form-group">
            <label class="col-sm-2 control-label" for="externalId">${this._i18n.externalid}</label>
            <div class="col-sm-10">
              <input class="form-control" type="text" name="externalId" id="externalId" .value=${this.tag?.externalId || ""} ?readonly=${this.tag?.externalcreation} />
            </div>
          </div>

          <div class="form-group" style="${this.tag?.id ? "display:none" : ""}">
            <label class="col-sm-2 control-label" for="collectionName">${this._i18n.tagcollectionName}</label>
            <div class="col-sm-10">
              <input class="form-control"
                  type="text"
                  ?readonly=${this.collectionIdReadonly}
                  name="collectionName"
                  id="collectionName"
                  .value=${this.collectionName}
                  required />
            </div>
          </div>

          <div hidden class="form-group">
            <div class="col-sm-10">
              <input type="hidden" name="id" id="id" .value=${this.tag?.id || ""} />
              <input type="hidden" name="createdBy" id="createdBy" .value=${this.tag?.createdBy || ""} />
              <input type="hidden" name="creationDate" id="creationDate" .value=${this.tag?.creationDate || ""} />
              <input type="hidden" name="lastModifiedBy" id="lastModifiedBy" .value=${this.tag?.lastModifiedBy || ""} />
              <input type="hidden" name="lastModificationDate" id="lastModificationDate" .value=${this.tag?.lastModificationDate || ""} />
              <input type="hidden" name="alternativeLabels" id="alternativeLabels" .value=${this.tag?.alternativeLabels || ""} />
              <input type="hidden" name="externalCreation" id="externalCreation" .value=${this.tag?.externalCreation || ""} />
              <input type="hidden" name="externalCreationDate" id="externalCreationDate" .value=${this.tag?.externalCreationDate || ""} />
              <input type="hidden" name="externalUpdate" id="externalUpdate" .value=${this.tag?.externalUpdate || ""} />
              <input type="hidden" name="lastUpdateDateInExternalSystem" id="lastUpdateDateInExternalSystem" .value=${this.tag?.lastUpdateDateInExternalSystem || ""} />
              <input type="hidden" name="parentId" id="parentId" .value=${this.tag?.parentId || ""} />
              <input type="hidden" name="externalHierarchyCode" id="externalHierarchyCode" .value=${this.tag?.externalHierarchyCode || ""} />
              <input type="hidden" name="externalType" id="externalType" .value=${this.tag?.externalType || ""} />
              <input type="hidden" name="data" id="data" .value=${this.tag?.data || ""} />
              <input type="hidden" name="collectionId" id="collectionId" .value=${this.collectionId || ""} />
            </div>
          </div>

          <div class="col-sm-offset-2 col-sm-10">
            <button @click=${this._save} class="btn btn-primary" type="button">${this._i18n.save_tag}</button>
            <button @click=${this._cancel} type="button" class="btn btn-secondary tagservice-cancel-btn">${this._i18n.Cancel}</button>
          </div>
        </form>
      </div>
    `;
  }
}
