import { html } from 'lit-html';
import { unsafeHTML } from 'lit-html/directives/unsafe-html';
import fetchMock from "fetch-mock";
import { conversationsI18n } from "./conversations-i18n.js";
import { topic1 } from "./data/topic1.js";
import { blankTopic } from "./data/blankTopic.js";

import '../../js/conversations/sakai-add-topic.js';

export default {
  title: 'Sakai Add Topic',
  decorators: [storyFn => {

    parent.portal = {
      locale: "en-GB",
      user: {
        id: "mike",
        offsetFromServerMillis: 3600000,
      }
    }

    fetchMock
      .get(/sakai-ws\/rest\/i18n\/getI18nProperties.*/, conversationsI18n, {overwriteRoutes: true})
      .get(/api\/conversations\/topics\/topic1/, topic1, {overwriteRoutes: true})
      .post(/api\/topics$/, (url, opts) => {

        const requestTopic = JSON.parse(opts.body);
        return {
          id: "" + Math.floor(Math.random() * 20) + 1,
          creator: "adrian",
          created: Date.now(),
          title: requestTopic.title,
          message: requestTopic.message,
          creatorDisplayName: "Adrian Fish",
          type: requestTopic.type,
          pinned: requestTopic.pinned,
          draft: requestTopic.draft,
          visibility: requestTopic.visibility,
        };
      }, {overwriteRoutes: true})
      .post(/api\/topics\/.*/, (url, opts) => {

        const requestTopic = JSON.parse(opts.body);
        return {
          id: requestTopic.id,
          creator: "adrian",
          created: Date.now(),
          title: requestTopic.title,
          message: requestTopic.message,
          creatorDisplayName: "Adrian Fish",
          type: requestTopic.type,
          pinned: requestTopic.pinned,
          draft: requestTopic.draft,
          visibility: requestTopic.visibility,
        };

      }, {overwriteRoutes: true})
      .get("*", 500, {overwriteRoutes: true});

    return storyFn();
  }],
};

const availableTags = [ { id: 1, label: "pheasant"}, { id: 2, label: "chicken" }, { id: 3, label:"turkey" }, { id: 4, label: "bigbird" }];

export const AddTopic = () => {

  return html`
    <div class="Mrphs-sakai-conversations">
      <sakai-add-topic about-reference="/site/playpen" topic="${blankTopic}" tags="${JSON.stringify(availableTags)}" can-anon can-pin></sakai-add-topic>
    </div>
  `;
};


export const UpdateTopic = () => {

  return html`
    <div>
      <sakai-add-topic can-anon @topic-saved=${e => console.log(e.detail.topic)} topic="${topic1}" available-tags="${JSON.stringify(availableTags)}"></sakai-add-topic>
    </div>
  `;
};
