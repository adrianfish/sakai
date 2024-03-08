import { html, nothing } from "lit";
import { ifDefined } from "lit/directives/if-defined.js";
import { SakaiElement } from "@sakai-ui/sakai-element";

export class ConversationsTopicPicker extends SakaiElement {

  static properties = {

    siteId: { attribute: "site-id", type: String },
    endpoint: { type: String },

    _topics: { state: true },
  };

  constructor() {

    super();

    this.loadTranslations("conversations");
  }

  set siteId(value) {

    this._siteId = value;

    this._loadTopics();
  }

  get siteId() { return this._siteId; }

  _loadTopics() {

    const url = `/api/sites/${this.siteId}/conversations/topics`;
    fetch(url)
      .then(r => {

        if (r.ok) {
          return r.json();
        }
        throw new Error(`Network error while loading topics from ${url}`);
      })
      .then(topics => {

        this._topics = topics;
      })
      .catch (error => console.error(error));
  }

  shouldUpdate() {
    return this._i18n && this._topics;
  }

  _postToEndpoint() {

    // Build an array of the selected topics
    const refs = [...this.querySelectorAll("input[type=checkbox]:checked")].map(i => ({ reference: i.value, title: i.dataset.title }));

    fetch(this.endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(refs)
    })
    .then(r => {
    })
    .catch(error => console.error(error));
  }

  render() {

    if (!this._topics.length) {
      return html`
        <div class="sak-banner-info">${this._i18n.no_topics_yet}</div>
      `;
    }

    return html`
      <div>
      ${this._topics.map(topic => html`
        <div role="listitem">
          <label>
            <input type="checkbox" name="conversations-topic-selection" value=${topic.reference} data-title="${topic.title}">${topic.title}
          </label>
        </div>
      `)}
      </div>
      <p class="act">
        <button type="button" class="btn btn-primary" @click=${this._postToEndpoint}>Save</button>
      </p>
    `;
  }
}

