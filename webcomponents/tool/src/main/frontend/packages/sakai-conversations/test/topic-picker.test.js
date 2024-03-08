import "../conversations-topic-picker.js";
import { elementUpdated, expect, fixture, html, oneEvent, waitUntil } from "@open-wc/testing";
import * as data from "./data.js";
import fetchMock from "fetch-mock/esm/client";

describe("conversations-topic-picker tests", () => {

  beforeEach(() => {
    fetchMock.get(data.i18nUrl, data.i18n);
  });

  afterEach(() => {
    fetchMock.restore();
  });

  it ("renders with no topics", async () => {

    fetchMock.get(`/api/sites/${data.siteId}/conversations/topics`, []);

    const el = await fixture(html`
      <conversations-topic-picker site-id="${data.siteId}"></conversations-topic-picker>
    `);

    await elementUpdated(el);

    await expect(el).to.be.accessible();

    expect(el.querySelector(".sak-banner-info")).to.exist;
  });

  it ("renders some topics", async () => {

    fetchMock.get(`/api/sites/${data.siteId}/conversations/topics`, [ data.discussionTopic, data.questionTopic ]);

    const el = await fixture(html`
      <conversations-topic-picker site-id="${data.siteId}"></conversations-topic-picker>
    `);

    await elementUpdated(el);

    await expect(el).to.be.accessible();

    expect(el.querySelector(".sak-banner-info")).to.not.exist

    expect(el.querySelectorAll("input[type=checkbox]").length).to.equal(2);

    expect(el.querySelector("div:nth-child(1) > label").innerHTML).to.contain(data.discussionTopic.title);
    expect(el.querySelector("div:nth-child(2) > label").innerHTML).to.contain(data.questionTopic.title);
  });

  it ("sends the selections to the endpoint", async () => {

    const topicsUrl = `/api/sites/${data.siteId}/conversations/topics`;
    fetchMock.get(topicsUrl, [ data.discussionTopic, data.questionTopic ]);

    const endpoint = `/api/sites/${data.siteId}/lessons/pages/1/embedded-items`;
    fetchMock.post(endpoint, 200);

    const el = await fixture(html`
      <conversations-topic-picker site-id="${data.siteId}" endpoint="${endpoint}"></conversations-topic-picker>
    `);

    await waitUntil(() => el._topics);

    expect(fetchMock.called(topicsUrl)).to.be.true;

    await elementUpdated(el);

    el.querySelector(`input[value="${data.discussionTopic.reference}"]`).checked = true;

    expect(fetchMock.called(endpoint)).to.be.false;

    el.querySelector("button").click();

    expect(fetchMock.called(endpoint)).to.be.true;
  });
});
