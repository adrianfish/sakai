import "../sakai-pages.js";
import { html } from "lit";
import * as data from "./data.js";
import { expect, fixture, waitUntil } from "@open-wc/testing";
import fetchMock from "fetch-mock/esm/client";

describe("sakai-pages tests", () => {

  window.top.portal = { locale: "en_GB" };

  fetchMock
    .get(data.i18nUrl, data.i18n, { overwriteRoutes: true })
    .get(data.pagesUrl, data.pagesDataForStudent, { overwriteRoutes: true })
    .delete(data.page1Url, {}, { overwriteRoutes: true })
    .get("*", 500, { overwriteRoutes: true });

  it ("renders for student correctly", async () => {

    let el = await fixture(html`
      <sakai-pages site-id="${data.siteId}"></sakai-pages>
    `);

    await waitUntil(() => el.querySelector("div.justify-content-between"), "Top level PAGES div not displayed");

    // This should only appear for instructors, the add page button icon
    expect(el.querySelector("i.si-add")).to.not.exist;
  });

  it ("renders for instructor correctly", async () => {

    fetchMock
      .get(data.pagesUrl, data.pagesDataForInstructor, { overwriteRoutes: true });

    let el = await fixture(html`
      <sakai-pages site-id="${data.siteId}"></sakai-pages>
    `);

    await waitUntil(() => el.querySelector("div.justify-content-between"), "Top level PAGES div not displayed");

    // This should only appear for instructors, the add page button icon
    await waitUntil(() => el.querySelector("i.si-add"), "Add page button not displayed");
  });

  it ("prompts the user to confirm deletion of a page", async () => {

    fetchMock
      .get(data.pagesUrl, data.pagesDataForInstructor, { overwriteRoutes: true });

    let el = await fixture(html`
      <sakai-pages site-id="${data.siteId}"></sakai-pages>
    `);

    await waitUntil(() => el.querySelector("button.delete-page-button"), "Delete page button not displayed");

    el.querySelector("button.delete-page-button").click();
  });

  it ("is accessible", async () => {

    let el = await fixture(html`
      <sakai-pages site-id="${data.siteId}"></sakai-pages>
    `);

    await waitUntil(() => el.querySelector("div.justify-content-between"), "Top level PAGES div not displayed");

    expect(el).to.be.accessible();
  });
});
