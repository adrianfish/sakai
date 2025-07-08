import "../sakai-account.js";
import * as data from "./data.js";
import { aTimeout, waitUntil, elementUpdated, expect, fixture, html } from "@open-wc/testing";
import fetchMock from "fetch-mock/esm/client";

describe("sakai-account tests", () => {

  beforeEach(async () => {
    fetchMock.get(data.i18nUrl, data.i18n);
  });

  afterEach(() => {
    fetchMock.restore();
  });

  it ("renders correctly with no profile data set", async () => {

    fetchMock.get(data.profileUrl, data.emptyProfile);

    const el = await fixture(html`
      <sakai-account user-id="${data.userId}"></sakai-account>
    `);

    await elementUpdated(el);

    await expect(el).to.be.accessible();

    expect(el.renderRoot.querySelectorAll(".profile_instruction").length).to.equal(4);
  });

  it ("renders correctly with basic profile data set", async () => {

    const profile = { ...data.emptyProfile, firstName: "Adrian", lastName: "Fish", nickname: "Fishy", pronouns: "he/him" };
    fetchMock.get(data.profileUrl, profile);

    const el = await fixture(html`
      <sakai-account user-id="${data.userId}"></sakai-account>
    `);

    await elementUpdated(el);

    await expect(el).to.be.accessible();

    expect(el.renderRoot.querySelectorAll(".profile_instruction").length).to.equal(3);

    const contentElements = el.renderRoot.querySelectorAll(".content");
    expect(contentElements.length).to.equal(4);

    expect(contentElements[0].textContent).to.contain(profile.firstName);
    expect(contentElements[1].textContent).to.contain(profile.lastName);
    expect(contentElements[2].textContent).to.contain(profile.nickname);
    expect(contentElements[3].textContent).to.contain(profile.pronouns);
  });

  it ("renders correctly with pronunciation profile data set", async () => {

    const profile = { ...data.emptyProfile, phoneticName: "Ay-Dree-An", nameRecordingUrl: "https://example.com/name-recording.mp3" };
    fetchMock.get(data.profileUrl, profile);

    const el = await fixture(html`
      <sakai-account user-id="${data.userId}"></sakai-account>
    `);

    await elementUpdated(el);

    await expect(el).to.be.accessible();

    expect(el.renderRoot.querySelectorAll(".profile_instruction").length).to.equal(3);

    const contentElements = el.renderRoot.querySelectorAll(".content");
    expect(contentElements.length).to.equal(2);

    expect(contentElements[0].textContent).to.contain(profile.phoneticName);
    expect(contentElements[1].querySelector("audio").src).to.equal(profile.nameRecordingUrl);
  });

  it ("renders correctly with contact profile data set", async () => {

    const profile = { ...data.emptyProfile, email: "adrian@mailinator.com", mobile: "0483 4444" };
    fetchMock.get(data.profileUrl, profile);

    const el = await fixture(html`
      <sakai-account user-id="${data.userId}"></sakai-account>
    `);

    await elementUpdated(el);

    await expect(el).to.be.accessible();

    expect(el.renderRoot.querySelectorAll(".profile_instruction").length).to.equal(3);

    const contentElements = el.renderRoot.querySelectorAll(".content");
    expect(contentElements.length).to.equal(2);

    expect(contentElements[0].textContent).to.equal(profile.email);
    expect(contentElements[1].textContent).to.equal(profile.mobile);
  });

  it ("renders correctly with social profile data set", async () => {

    const profile = { ...data.emptyProfile, facebook: "https://facebook.com/adrian", linkedin: "https://linkedin.com/adrian", instagram: "https://instagram.com/adrian" };
    fetchMock.get(data.profileUrl, profile);

    const el = await fixture(html`
      <sakai-account user-id="${data.userId}"></sakai-account>
    `);

    await elementUpdated(el);

    await expect(el).to.be.accessible();

    expect(el.renderRoot.querySelectorAll(".profile_instruction").length).to.equal(3);

    const contentElements = el.renderRoot.querySelectorAll(".content");
    expect(contentElements.length).to.equal(3);

    expect(contentElements[0].textContent).to.equal(profile.facebook);
    expect(contentElements[1].textContent).to.equal(profile.linkedin);
    expect(contentElements[2].textContent).to.equal(profile.instagram);
  });

  it ("displays the basic details editors", async () => {

    const profile = { ...data.emptyProfile, firstName: "Adrian", lastName: "Fish", nickname: "Fishy", pronouns: "he/him" };
    fetchMock.get(data.profileUrl, profile);

    const el = await fixture(html`
      <sakai-account user-id="${data.userId}"></sakai-account>
    `);

    await elementUpdated(el);

    await waitUntil(() => el.renderRoot.querySelectorAll(".content")[0]);
    expect(el.renderRoot.querySelectorAll(".content")[0].textContent).to.contain(profile.firstName);

    expect(el.renderRoot.querySelector("#basic-info-save-button")).to.not.exist;

    const editButton = el.renderRoot.querySelector("#basic-info-edit-button");
    expect(editButton).to.exist;

    editButton.click();
    await elementUpdated(el);
    await expect(el).to.be.accessible();

    expect(el.renderRoot.querySelector("#first-name-input").value).to.equal(profile.firstName);
    expect(el.renderRoot.querySelector("#last-name-input").value).to.equal(profile.lastName);
    expect(el.renderRoot.querySelector("#nickname-input").value).to.equal(profile.nickname);
    expect(el.renderRoot.querySelector("#pronouns-input").value).to.equal(profile.pronouns);
  });

  it ("updates basic details", async () => {

    const profile = { ...data.emptyProfile, firstName: "Adrian", lastName: "Fish", nickname: "Fishy", pronouns: "he/him" };
    fetchMock.get(data.profileUrl, profile);

    const el = await fixture(html`
      <sakai-account user-id="${data.userId}"></sakai-account>
    `);

    await elementUpdated(el);

    await waitUntil(() => el.renderRoot.querySelectorAll(".content")[0]);

    el.renderRoot.querySelector("#basic-info-edit-button").click();

    await elementUpdated(el);

    fetchMock.patch(data.profileUrl, 200, { name: "patchRequest" });

    const newFirstName = "James";
    const newLastName = "Bond";
    const newNickname = "Jimmy";
    const newPronouns = "secret/agent";

    el.renderRoot.querySelector("#first-name-input").value = newFirstName;
    el.renderRoot.querySelector("#last-name-input").value = newLastName;
    el.renderRoot.querySelector("#nickname-input").value = newNickname;
    el.renderRoot.querySelector("#pronouns-input").value = newPronouns;
    const saveButton = el.renderRoot.querySelector("#basic-info-save-button");
    expect(saveButton).to.exist;
    saveButton.click();

    // Now we can inspect the fetch call
    expect(fetchMock.calls("patchRequest").length).to.equal(1);
    const formData = fetchMock.calls("patchRequest")[0][1].body;
    expect(formData.get("firstName")).to.equal(newFirstName);
    expect(formData.get("lastName")).to.equal(newLastName);
    expect(formData.get("nickname")).to.equal(newNickname);
    expect(formData.get("pronouns")).to.equal(newPronouns);

    await waitUntil(() => !el.editingBasicInfo);
    await elementUpdated(el);
    expect(el._user.firstName).to.equal(newFirstName);
    expect(el._user.lastName).to.equal(newLastName);
    expect(el._user.nickname).to.equal(newNickname);
    expect(el._user.pronouns).to.equal(newPronouns);
    expect(el.renderRoot.querySelector("#basic-info-save-button")).to.not.exist;
  });

  it ("displays the pronunciation details editors", async () => {

    const profile = { ...data.emptyProfile, phoneticName: "Ay-Dree-An", nameRecordingUrl: "https://example.com/name-recording.mp3" };
    fetchMock.get(data.profileUrl, profile);

    const el = await fixture(html`
      <sakai-account user-id="${data.userId}"></sakai-account>
    `);

    await elementUpdated(el);

    await waitUntil(() => el.renderRoot.querySelectorAll(".content")[0]);

    const editButton = el.renderRoot.querySelector("#pronunciation-info-edit-button");
    expect(editButton).to.exist;

    editButton.click();
    await elementUpdated(el);

    expect(el.renderRoot.querySelector("#phonetic-name-input").value).to.equal(profile.phoneticName);
  });
});
