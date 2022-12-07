import { html } from 'lit-html';
import fetchMock from "fetch-mock";
import { notifications } from "../sui-notifications/data/notifications.js";
import { notificationsI18n } from "../sui-notifications/i18n/sui-notifications.js";
import { pwaI18n } from "./i18n/sui-pwa.js";

import '../../js/sui-pwa/sui-pwa.js';

export default {
  title: 'SUI PWA',
  decorators: [storyFn => {

    window.portal = {};
    window.portal.notifications = {
      registerForMessages: () => { return; },
      setup: new Promise(resolve => resolve()),
    };

    fetchMock
      .get(/.*i18n.*sui-notifications$/, notificationsI18n, { overwriteRoutes: true })
      .get(/.*i18n.*sui-pwa$/, pwaI18n, { overwriteRoutes: true })
      .get(/\/direct\/portal\/notifications.json/, notifications, { overwriteRoutes: true })
      .get("*", 500, {overwriteRoutes: true});

    return storyFn();
  }],
};

export const BasicDisplay = () => {

  return html`
    <div>
      <sui-pwa></sui-pwa>
    </div>
  `;
};
