import { html } from 'lit-html';
import { unsafeHTML } from 'lit-html/directives/unsafe-html';
import { styles as sakaiStyles } from "../styles/sakai-styles.js";

import '../../js/openapereo/sakai-openapereo.js';

export default {
  title: 'Sakai OpenApereo',
};

export const BasicDisplay = () => {

  return html`
    ${unsafeHTML(sakaiStyles)}
    <sakai-openapereo></sakai-openapereo>
  `;
};
