import { css, html, LitElement } from "../assets/lit-element/lit-element.js";
import { loadProperties } from "../sakai-i18n.js";
import "../sakai-tabs/sakai-tabs.js";
import "../sakai-icon.js";

class SakaiFilePicker extends LitElement {

  static get properties() {

    return {
      heading: { type: String },
      instruction: { type: String },
      resources: { attribute: false, type: Object },
      i18n: { attribute: false, type: Object },
    };
  }

  constructor() {

    super();

    this.resources = [];

    loadProperties("file-picker").then(r => this.i18n = r);
  }

  _loadResourcesData() {

    const url = `/api/sites/${this.siteId}/resources`;
    fetch(url, { credentials: "include" })
    .then(r => {

      if (r.ok) {
        return r.json();
      }

      throw new Error(`Network error while retrieving resources from ${url}: ${r.status}`);
    })
    .then(resources => this.resources = resources)
    .catch (error => console.error(error));
  }

  _tabSelected() {

    if (this.shadowRoot.getElementById("tabs").selectedIndex === 2) {
      this._loadResourcesData();
    }
  }

  
  _flattenResources(startResources) {

    const flattenAll = (resources = []) => {
      return resources.flatMap(r => [ r, ...this._flattenResources(r.resources) ]);
    };

    return flattenAll(startResources);
  }

  _findResource(id) {

    const flattened = this._flattenResources(this.resources);

    return flattened.find(r => r.id === id);
  }

  _toggleResource(e) {

    const id = e.target.dataset.resourceId;
    const resource = this._findResource(id);
    if (resource.expanded) {
      resource.expanded = false;
      this.requestUpdate();
    } else {
      if (!resource.resources?.length) {
        const url = `/api/sites/${this.siteId}/resources/${id}`;
        fetch(url, { credentials: "include" })
        .then(r => {
          
          if (r.ok) {
            return r.json();
          }

          throw new Error(`Network error while fetching from ${url}: ${r.status}`);
        })
        .then(resources => {

          resource.resources = resources;
          resource.expanded = true;
          this.requestUpdate();
        })
        .catch (error => console.error(error));
      } else {
        resource.expanded = true;
        this.requestUpdate();
      }
    }
  }

  _renderResources(resources) {

    return resources ? html`
      ${resources.map(resource => html`
        <div class="folder-container">
          <div class="folder">
            <div>${resource.title}</div>
            ${resource.type === "folder" ? html`
            <div>
              <button class="btn-transparent" data-resource-id="${resource.id}" @click=${this._toggleResource}>
                <sakai-icon type="${resource.expanded ? "minus" : "add"}" size="small"></sakai-icon>
              </button>
            </div>
            ` : ""}
          </div>
          ${resource.expanded ? html`
            ${this._renderResources(resource.resources)}
          ` : ""}
        </div>
      `)}
    ` : "";
  }

  render() {

    return html`
      <h3>${this.heading}</h3>
      <div class="instruction">${this.instruction}</div>
      <sakai-tabs id="tabs" @selected-changed=${this._tabSelected}>

        <div slot="tab">${this.i18n.from_local}</div>
        <div slot="panel" class="panel">
          <h4><label for="upload">${this.i18n.from_local_instruction}</label></h4>
          <input type="file" id="upload">
          <span id="submitnotifxxx" class="messageProgress" style="display: none">${this.i18n.uploading_files}</span>
        </div>

        <div slot="tab">${this.i18n.from_website}</div>
        <div slot="panel" class="panel">
          <h4><label for="url">${this.i18n.from_website_instruction}</label></h4>
          <input type="text" id="url" size="40">
          <input type="button" value="${this.i18n.add}">
        </div>

        <div slot="tab">${this.i18n.from_resources}</div>
        <div slot="panel" class="panel">
          <h4>${this.i18n.from_resources_instruction}</h4>
          ${this._renderResources(this.resources)}
        </div>

      </sakai-tabs>
    `;
  }

  static get styles() {

    return css`
      .folder-container {
      margin-left: 20px;
      }
      .folder {
        display: flex;
        align-items: center;
      }
      .folder div:nth-child(2) {
        margin-left: 4px;
        padding-top: 4px;
      }
      .btn-transparent {
        border: none;
        background: none;
      }
    `;
    
  }
}

customElements.define("sakai-file-picker", SakaiFilePicker);
