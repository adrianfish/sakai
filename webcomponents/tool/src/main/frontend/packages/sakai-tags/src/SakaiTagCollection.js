import { html, nothing } from "lit";
import { SakaiElement } from "@sakai-ui/sakai-element";
import "../sakai-edit-tag.js";
import "../sakai-view-tag.js";

export class SakaiTagCollection extends SakaiElement {

  static properties = {
    collection: { type: Object },
    _tags: { state: true },
    _canCreate: { state: true },
    _showPagination: { state: true },
    _state: { state: true },
  };

  constructor() {

    super();

    this.collection = {};

    this._state = "TAGS";

    this.loadTranslations("tagservice");
  }

  connectedCallback() {

    super.connectedCallback();

    this._loadTags(1);
  }

  _loadTags(pageNum) {

    const url = `/api/tags/collections/${this.collection.id}/tags?pageNum=${pageNum}`;
    fetch(url)
    .then(r => {

      if (r.ok) {
        return r.json();
      }
      throw new Error(`Network error while loading tags from ${url}`);
    })
    .then(data => {

      this._tags = data.tags;
      this._canCreate = data.canCreate;
      this._pageNum = data.pageNum;
      this._showPagination = data.showPagination;
      this._totalPages = data.totalPages;
      this._state = "TAGS";
    });
  }

  _createTag() {

    this._tagBeingEdited = {};
    this._state = "EDIT_TAG";
  }

  _editTag(e) {

    this._tagBeingEdited = this._tags.find(c => c.id === e.currentTarget.dataset.tagId);
    this._state = "EDIT_TAG";
  }

  _deleteTag(e) {

    if (!confirm(this._i18n.delete_tag_confirmation.replace("{label}", e.currentTarget.dataset.tagLabel))) {
      return;
    }

    const url = `/api/tags/collections/${this.collection.id}/tags/${e.currentTarget.dataset.tagId}`;
    fetch(url, { method: "DELETE" })
    .then(r => {

      if (r.ok) {
        this._loadTags(1);
      } else {
        throw new Error(`Network error while deleting tag from ${url}`);
      }
    });
  }

  _viewTag(e) {

    this._tagBeingViewed = this._tags.find(c => c.id === e.currentTarget.dataset.tagId);
    this._state = "VIEW_TAG";
  }

  _prevPage() {
    this._loadTags(this._pageNum - 1);
  }

  _page(e) {
    this._loadTags(e.currentTarget.dataset.pageNumber);
  }

  _nextPage() {
    this._loadTags(this._pageNum + 1);
  }

  _handleTagEdited() {
    this._loadTags(1);
  }

  _cancelEditTag() {
    this._state = "TAGS";
  }

  _cancelViewTag() {
    this._state = "TAGS";
  }

  _viewCollections() {
    this.dispatchEvent(new CustomEvent("view-collections"));
  }

  shouldUpdate() {
    return this._i18n;
  }

  _renderEditTag() {

    return html`
      <sakai-edit-tag .tag=${this._tagBeingEdited}
          .collectionName=${this.collection?.name}
          .collectionId=${this.collection?.id}
          @tag-edited=${this._handleTagEdited}
          @cancel-edit-tag=${this._cancelEditTag}>
      </sakai-edit-tag>
    `;
  }

  _renderViewTag() {

    return html`
      <sakai-view-tag .tag=${this._tagBeingViewed} @tag-viewed=${this._cancelViewTag}>
      </sakai-view-tag>
    `;
  }

  _renderTags() {

    return html`

      <section class="tagservice-section">
        <header class="tagservice-section-header">
          <h3>${this._i18n.tagcollection}: ${this.collection?.name || ""}</h3>
          <div class="btn-toolbar float-end">
            <button @click=${this._createTag} class="btn btn-primary btn-sm pull-right me-1">${this._i18n.create_tag}</button>
            <button @click=${this._viewCollections} class="btn btn-primary btn-sm pull-right">${this._i18n.Return}</button>
          </div>
        </header>

        <div class="container-fluid">
          <div class="row mb-3 fw-bold border-bottom">
            <div class="col-3 h4">${this._i18n.tag_label}</div>
            <div class="col-6 h4">${this._i18n.description}</div>
            <div class="col-3 h4 d-flex justify-content-end">${this._i18n.actions}</div>
          </div>

            ${this?._tags?.length === 0 ? html`
            <div class="row gy-4">
              <div class="col-12 text-center">
                <div class="alert alert-info" style="margin-top: 15px;">${this._i18n.no_tags_in_collection}</div>
              </div>
            </div>
            ` : nothing}
            ${this?._tags?.map(tag => html`
            <div class="row mb-2 align-items-center">
              <div class="col-3 h5">
                <a href="#"
                    @click=${this._viewTag}
                    data-tag-id="${tag.id}">
                  ${tag.label}
                </a>
              </div>
              <div class="col-6 text-truncate">${tag.description || ""}</div>
              <div class="col-3 text-end">
                <button @click=${this._editTag}
                    class="btn btn-primary btn-xs"
                    data-tag-id="${tag.id}"
                    aria-label="${this._i18n.Edit}"
                    title="${this._i18n.Edit}">
                  <span class="si si-edit" aria-hidden="true"></span>
                </button>
                <button @click=${this._deleteTag}
                    class="btn btn-warning btn-xs tagservice-delete-btn"
                    data-tag-id="${tag.id}"
                    data-tag-label="${tag.label}"
                    data-record-type="tag"
                    aria-label="${this._i18n.delete_tag}"
                    title="${this._i18n.delete_tag}">
                  <span class="si si-delete" aria-hidden="true"></span>
                </button>
              </div>
            </div>
            `)}
        </div>

        <!-- Pagination -->
        ${this._showPagination ? html`
        <div class="paging_bootstrap mt-3">
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

  render() {

    switch (this._state) {
      case "TAGS":
        return this._renderTags();
      case "EDIT_TAG":
        return this._renderEditTag();
      case "VIEW_TAG":
        return this._renderViewTag();
    }
  }
}
