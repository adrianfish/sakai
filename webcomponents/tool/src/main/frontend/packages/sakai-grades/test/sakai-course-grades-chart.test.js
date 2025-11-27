import "../sakai-course-grades-chart.js";
import * as data from "./data.js";
import { elementUpdated, fixture, expect, html, waitUntil } from "@open-wc/testing";
import fetchMock from "fetch-mock/esm/client";

describe("sakai-course-grades-chart tests", () => {

  beforeEach(async () => {

    fetchMock
      .get(data.i18nUrl, data.i18n);
  });

  afterEach(async () => {
    fetchMock.restore();
  });

  it ("renders in user mode correctly", async () => {

    fetchMock.get(data.chartDataUrl, data.chartData);

    // In user mode, we'd expect to get announcements from multiple sites.
    let el = await fixture(html`
      <sakai-course-grades-chart data-url="${data.chartDataUrl}"></sakai-course-grades-chart>
    `);

    await elementUpdated(el);

    await expect(el).to.be.accessible();
  });
});
