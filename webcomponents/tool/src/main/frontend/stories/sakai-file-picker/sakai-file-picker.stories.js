import { html } from 'lit-html';
import { unsafeHTML } from 'lit-html/directives/unsafe-html';
import fetchMock from "fetch-mock";
import { styles } from "../styles/sakai-styles.js";
import { filepickerI18n } from "./i18n/file-picker.js";
import { topLevelResources } from "./data/top-level-resources.js";
import { eggsResources } from "./data/eggs-resources.js";

import '../../js/sakai-file-picker/sakai-file-picker.js';

export default {
  title: 'Sakai File Picker',
  decorators: [storyFn => {

    parent.portal = {locale: "en-GB"};
    const baseUrl = "/sakai-ws/rest/i18n/getI18nProperties?locale=en-GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=";
    const filepickerI18nUrl = `${baseUrl}file-picker`;
    fetchMock
      .get(filepickerI18nUrl, filepickerI18n, {overwriteRoutes: true})
      .get(/\/api\/.*\/resources$/, topLevelResources, {overwriteRoutes: true})
      .get(/\/api\/.*\/resources\/eggs/, eggsResources, {overwriteRoutes: true})
      .get("*", 500, {overwriteRoutes: true});
    return storyFn();
  }],
};

export const BasicDisplay = () => {

  return html`
    ${unsafeHTML(styles)}
    <sakai-file-picker instruction="Pick some files">
  `;
};
