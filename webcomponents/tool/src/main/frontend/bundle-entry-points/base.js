import "@sakai-ui/sakai-date-picker/sakai-date-picker.js";
import "@sakai-ui/sakai-user-photo/sakai-user-photo.js";
import "imagesloaded";
import "@sakai-ui/sakai-profile/sakai-profile.js";
import "@sakai-ui/sakai-pronunciation-player/sakai-pronunciation-player.js";
import "@sakai-ui/sakai-picture-changer/sakai-picture-changer.js";
import "@sakai-ui/sakai-notifications/sakai-notifications.js";


import imagesLoaded from "imagesloaded";
globalThis.imagesLoaded = imagesLoaded;
import Sortable from "sortablejs";
globalThis.Sortable = Sortable;

import { loadProperties, tr } from "@sakai-ui/sakai-i18n";
globalThis.loadProperties = loadProperties;
globalThis.tr = tr;

import { mapStackTrace } from "@sakai-ui/sakai-utils";

window.addEventListener("unhandledrejection", async e => {

  const stack = e?.stack || e?.reason?.stack;

  if (!stack) return false;

  mapStackTrace(stack).then(t => console.log(t));
  return false;
});
