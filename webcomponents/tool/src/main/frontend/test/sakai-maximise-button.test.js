import { html } from 'lit-html';
import { expect, fixture } from '@open-wc/testing';
import '../js/sakai-maximise-button.js';

describe('<sakai-maximise-button>', () => {

  const i18n = { "normal_view": "normal_view", "fullscreen_view": "fullscreen_view" };

  it('contains the compress icon when in full screen mode', async () => {

    const el = await fixture(html`<sakai-maximise-button i18n="${JSON.stringify(i18n)}" full-screen></sakai-maximise-button>`);
    expect(el.querySelector("fa-icon[i-class='fas compress-arrows-alt']")).to.exist;
  });

  it('contains the expand icon when in normal mode', async () => {

    const el = await fixture(html`<sakai-maximise-button i18n="${JSON.stringify(i18n)}"></sakai-maximise-button>`);
    expect(el.querySelector("fa-icon[i-class='fas expand-arrows-alt']")).to.exist;
  });

});
