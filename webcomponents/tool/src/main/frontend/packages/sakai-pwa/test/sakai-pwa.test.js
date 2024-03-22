import "../sakai-pwa.js";
import { html } from "lit";
import * as data from "./data.js";
import { expect, fixture, aTimeout, waitUntil } from "@open-wc/testing";
import fetchMock from "fetch-mock/esm/client";

describe("sakai-pwa tests", () => {

  beforeEach(() =>  {

    /*
    window.top.portal = { locale: "en_GB", siteId: data.siteId };
    window.top.portal.notifications = {
      registerPushCallback: (type, callback) => {},
      setup: Promise.resolve(),
    };
    */

    fetchMock
      .get(data.i18nUrl, data.i18n, { overwriteRoutes: true })
      .get("*", 500, { overwriteRoutes: true });
  });

  it ("renders correctly", async () => {

    let el = await fixture(html`
      <sakai-pwa></sakai-pwa>
    `);

    await waitUntil(() => el._i18n);
  });

  it ("is accessible", async () => {

    let el = await fixture(html`
      <sakai-pwa></sakai-pwa>
    `);
  });
});
