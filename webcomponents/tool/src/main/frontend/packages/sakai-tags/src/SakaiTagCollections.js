import { html, nothing } from "lit";
import { SakaiElement } from "@sakai-ui/sakai-element";
import "../sakai-edit-tag-collection.js";
import "../sakai-tag-collection.js";

export class SakaiTagCollections extends SakaiElement {

  static properties = {
    _collections: { state: true },
    _canCreate: { state: true },
    _showPagination: { state: true },
    _totalPages: { state: true },
    _state: { state: true },
  };

  constructor() {

    super();

    this._collections = [];
    this._state = "COLLECTIONS";

    this.loadTranslations("tagservice");
  }

  connectedCallback() {

    super.connectedCallback();

    this._loadCollections(1);
  }

  _loadCollections(pageNum) {

    const url = `/api/tags/collections?pageNum=${pageNum}`;
    fetch(url)
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw new Error(`Network error while loading collections from ${url}`);
    })
    .then(data => {

      this._collections = data.collections;
      this._canCreate = data.canCreate;
      this._showPagination = data.showPagination;
      this._pageNum = data.pageNum;
      this._totalPages = data.totalPages;
      this._state = "COLLECTIONS";
    });
  }

  _createCollection() {

    this._collectionBeingEdited = {};
    this._state = "EDIT_COLLECTION";
  }

  _editCollection(e) {

    this._collectionBeingEdited = this._collections.find(c => c.id === e.currentTarget.dataset.collectionId);
    this._state = "EDIT_COLLECTION";
  }

  _prevPage() {
    this._loadCollections(this._pageNum - 1);
  }

  _page(e) {
    this._loadCollections(e.currentTarget.dataset.pageNumber);
  }

  _nextPage() {
    this._loadCollections(this._pageNum + 1);
  }

  _cancelEditCollection() { this._state = "COLLECTIONS"; }

  _deleteCollection(e) {

    if (!confirm(this._i18n.delete_collection_confirmation.replace("{name}", e.currentTarget.dataset.collectionName))) {
      return;
    }

    const url = `/api/tags/collections/${e.currentTarget.dataset.collectionId}`;
    fetch(url, {
      method: "DELETE",
    })
    .then(r => {

      if (r.ok) {
        this._loadCollections(this._pageNum);
      } else {
        throw new Error(`Network error while deleting collection from ${url}`);
      }
    });
  }

  _handleCollectionEdited() {
    this._loadCollections(1);
  }

  _viewCollection(e) {

    this._collectionBeingViewed = this._collections.find(c => c.id === e.currentTarget.dataset.collectionId);
    this._state = "VIEW_COLLECTION";
  }

  _viewCollections() {
    this._state = "COLLECTIONS";
  }

  shouldUpdate() {
    return this._i18n;
  }

  _renderCollection() {

    return html`
      <sakai-tag-collection .collection=${this._collectionBeingViewed} @view-collections=${this._viewCollections}></sakai-tag-collection>
    `;
  }

  _renderCollections() {

    return html`

      <section class="tagservice-section">
        <header class="tagservice-section-header">
          <h3>${this._i18n.tagcollections}</h3>
          ${this._canCreate ? html`
            <div class="btn-toolbar float-end">
              <button @click=${this._createCollection} class="btn btn-primary btn-sm pull-right">${this._i18n.create_tag_collection}</button>
            </div>
          ` : nothing}
        </header>

        <div class="container-fluid">
          <div class="row mb-3 fw-bold border-bottom">
            <div class="col-2 h4">${this._i18n.name}</div>
            <div class="col-4 h4">${this._i18n.description}</div>
            <div class="col-2 h4">${this._i18n.externalSourceName}</div>
            <div class="col-2 h4">${this._i18n.externalSourceDescription}</div>
            <div class="col-2 h4 d-flex justify-content-end">${this._i18n.actions}</div>
          </div>
            ${this._collections.length === 0 ? html`
            <div class="row gy-4">
              <div class="col-12 text-center">
                <div class="alert alert-info" style="margin-top: 15px;">
                    ${this._i18n.no_collections_found}
                </div>
              </div>
            </div>
            ` : nothing}
            ${this._collections.map(collection => html`
            <div class="row mb-2 align-items-center">
              <div class="col-2 h5">
                <a @click=${this._viewCollection}
                    href="#"
                    data-collection-id="${collection.id}"
                    aria-label="${this._i18n.manage_tags}"
                    title="${this._i18n.manage_tags}">
                  ${collection.name}
                </a>
              </div>
              <div class="col-4">${collection.description}</div>
              <div class="col-2">${collection.externalSourceName}</div>
              <div class="col-2">${collection.externalSourceDescription}</div>
              <div class="col-2 text-end">
                ${this._canCreate ? html`
                <button type="button"
                    @click=${this._editCollection}
                    class="btn btn-primary btn-xs"
                    data-collection-id="${collection.id}"
                    title="${this._i18n.Edit}"
                    aria-label="${this._i18n.Edit}">
                  <span class="si si-edit" aria-hidden="true"></span>
                </button>
                ` : nothing}
                ${this._canCreate ? html`
                <button type="button"
                    @click=${this._deleteCollection}
                    class="btn btn-warning btn-xs tagservice-delete-btn"
                    data-collection-id="${collection.id}"
                    data-collection-name="${collection.name}"
                    title="${this._i18n.delete_tag_collection}"
                    aria-label="${this._i18n.delete_tag_collection}">
                  <span class="si si-delete" aria-hidden="true"></span>
                </button>
                ` : nothing}
              </div>
            </div>
            `)}
          </div>
        </div>

        <!-- Pagination -->
        ${this._showPagination ? html`
        <div class="paging_bootstrap pt-2 mt-3 border-top">
          <ul class="pagination">
            <li class="${this._pageNum > 0 ? "page-item prev" : "page-item prev disabled"}">
              <a @click=${this._prevPage} class="page-link">${this._i18n.previous}</a>
            </li>
            
            ${Array(this._totalPages).fill(0).map((_, i) => html`
            <li class="${i + 1 === this._pageNum ? "page-item active" : "page-item"}">
              <a class="page-link" @click=${this._page} data-page-number="${i + 1}">${i + 1}</a>
            </li>
            `)}
            
            <li class="${this._pageNum < (this._totalPages) ? "page-item next" : "page-item next disabled"}">
              <a @click=${this._nextPage} class="page-link">${this._i18n.next}</a>
            </li>
          </ul>
        </div>
        ` : nothing}
      </section>
    `;
  }

  _renderEditCollection() {

    return html`
      <sakai-edit-tag-collection .collection=${this._collectionBeingEdited}
          @collection-edited=${this._handleCollectionEdited}
          @cancel-edit-collection=${this._cancelEditCollection}>
      </sakai-edit-tag-collection>
    `;
  }

  render() {

    switch (this._state) {
      case "COLLECTIONS":
        return this._renderCollections();
      case "EDIT_COLLECTION":
        return this._renderEditCollection();
      case "VIEW_COLLECTION":
        return this._renderCollection();
    }
  }
}
