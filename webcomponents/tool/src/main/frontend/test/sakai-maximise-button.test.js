import 'jsdom-global';
import { html } from 'lit-html';
import { expect, fixture } from '@open-wc/testing';
import fetchMock from "fetch-mock";
import '../js/sakai-maximise-button.js';

describe('<sakai-maximise-button>', () => {

  before(() => {

    const i18n = `
      normal_view=Click to exit fullscreen mode
      fullscreen_view=Click to enter fullscreen mode for this tool
    `;

    fetchMock.get(/sakai-ws\/rest\/i18n\/getI18nProperties.*maximise-button/, i18n, {overwriteRoutes: true});
  });

  it('contains the compress icon when in full screen mode', async () => {

    const el = await fixture(html`<sakai-maximise-button full-screen></sakai-maximise-button>`);
    console.log(el);
  });

});
