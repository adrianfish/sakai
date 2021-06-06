import { html } from 'lit-html';
import { unsafeHTML } from 'lit-html/directives/unsafe-html';
import fetchMock from "fetch-mock";
import { styles as sakaiStyles } from "../styles/sakai-styles.js";
import { openapereoI18n } from "./i18n/openapereo.js";

import '../../js/openapereo/sakai-openapereo.js';

export default {
  title: 'Sakai OpenApereo',
  decorators: [storyFn => {

    //parent.portal = {locale: "en-GB"};
    fetchMock
      .get(/sakai-ws\/rest\/i18n\/getI18nProperties.*/, openapereoI18n, {overwriteRoutes: true})
      .get("*", 500, {overwriteRoutes: true});
    return storyFn();
  }],
};

export const BasicDisplay = () => {

  return html`
    ${unsafeHTML(sakaiStyles)}
    <sakai-openapereo></sakai-openapereo>
  `;
};
