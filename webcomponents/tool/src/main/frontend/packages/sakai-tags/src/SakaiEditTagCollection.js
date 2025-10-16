import { html, nothing } from "lit";
import { SakaiElement } from "@sakai-ui/sakai-element";

export class SakaiEditTagCollection extends SakaiElement {

  static properties = {
    collection: { type: Object },
  };

  constructor() {

    super();

    this.collection = {};

    this.loadTranslations("tagservice");
  }

  _save() {

    const data = new FormData(this.querySelector("#create-tag-collection-form"));

    const id = data.get("id");
    const url = `/api/tags/collections${id ? `/${id}` : ""}`;
    console.log(url);
    fetch(url, {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(Object.fromEntries(data)),
    })
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw new Error(`Network error while saving tag collection from ${url}`);
    })
    .then(collection => {
      this.dispatchEvent(new CustomEvent("collection-edited", { detail: collection }));
    });
  }

  _cancel() {
    this.dispatchEvent(new CustomEvent("cancel-edit-collection"));
  }

  shouldUpdate() {
    return this._i18n;
  }

  render() {

    return html`

      <div class="container-fluid">
        <form id="create-tag-collection-form">
          <div class="form-group">
            <label class="col-sm-2 control-label" for="name">${this._i18n.name}</label>
            <div class="col-sm-10">
              <input class="form-control" type="text" name="name" id="name" required .value=${this.collection?.name || ""} />
              ${this._nameError ? html`
              <div class="text-danger">${this._nameError}</div>
              ` : nothing}
            </div>
          </div>
          
          <div class="form-group">
            <label class="col-sm-2 control-label" for="description" >${this._i18n.description}</label>
              <div class="col-sm-10">
                <input class="form-control" type="text" name="description" id="description" .value=${this.collection?.description || ""} />
              </div>
          </div>
          
          <div class="form-group">
            <label class="col-sm-2 control-label" for="externalSourceName">${this._i18n.externalSourceName}</label>
              <div class="col-sm-10">
                <input class="form-control" type="text" name="externalSourceName" id="externalSourceName" .value=${this.collection?.externalSourceName || ""} />
                ${this._externalSourceNameError ? html`
                <div class="text-danger">${this._externalSourceNameError}</div>
                ` : nothing}
              </div>
          </div>
          
          <div class="form-group">
            <label class="col-sm-2 control-label" for="externalSourceDescription">${this._i18n.externalSourceDescription}</label>
              <div class="col-sm-10">
                <input class="form-control" type="text" name="externalSourceDescription" id="externalSourceDescription" .value=${this.collection?.externalSourceDescription || ""} />
              </div>
          </div>
          
          <div hidden class="form-group">
            <div class="col-sm-10">
              <input type="hidden" name="id" id="id" .value=${this.collection?.id || ""} />
              <input type="hidden" name="externalUpdate" id="externalUpdate" .value=${this.collection?.externalUpdate || ""} />
              <input type="hidden" name="externalCreation" id="externalCreation" .value=${this.collection?.externalCreation || ""} />
              <input type="hidden" name="lastSynchronizationDate" id="lastSynchronizationDate" .value=${this.collection?.lastSynchronizationDate || ""} />
              <input type="hidden" name="lastUpdateDateInExternalSystem" id="lastUpdateDateInExternalSystem" .value=${this.collection?.lastUpdateDateInExternalSystem || ""} />
              <input type="hidden" name="lastModifiedBy" id="lastModifiedBy" .value=${this.collection?.lastModifiedBy || ""} />
              <input type="hidden" name="lastModificationDate" id="lastModificationDate" .value=${this.collection?.lastModificationDate || ""} />
              <input type="hidden" name="createdBy" id="createdBy" .value=${this.collection?.createdBy || ""} />
              <input type="hidden" name="creationDate" id="creationDate" .value=${this.collection?.creationDate || ""} />
            </div>
          </div>

          <div class="col-sm-offset-2 col-sm-10">
            <input @click=${this._save} class="btn btn-primary" type="button" name="save" .value=${this._i18n.save_tag_collection} />
            <a class="btn btn-primary tagservice-cancel-btn" @click=${this._cancel}>${this._i18n.Cancel}</a>
          </div>
        </form>
      </div>
    `;
  }
}
