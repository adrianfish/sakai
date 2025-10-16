import "../sakai-tag-picker.js";
import { elementUpdated, expect, fixture, html, waitUntil } from "@open-wc/testing";
import * as data from "./data.js";

describe("sakai-tag-picker tests", () => {

  it ("renders correctly", async () => {
 
    const el = await fixture(html`
      <sakai-tag-picker
          site-id="${data.siteId}"
          tool="${data.toolId}"
          collection-id="${data.collectionId}">
      </sakai-tag-picker>
    `);

    await elementUpdated(el);
    await expect(el).to.be.accessible();
  });
});
