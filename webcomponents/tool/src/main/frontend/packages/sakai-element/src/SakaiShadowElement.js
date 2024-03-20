import { css, LitElement } from "lit";
import { loadProperties, tr } from "@sakai-ui/sakai-i18n";
import { getDocumentStyleSheets } from "./global-styles.js";
import { Signal } from "signal-polyfill";
import { userChanged } from "@sakai-ui/sakai-signals";

export class SakaiShadowElement extends LitElement {

  static properties = {
    _user: { state: true },
    cacheName: { attribute: "cache-name", type: String },
    _online: { state: true },
    _i18n: { state: true },
  };

  constructor() {

    super();

    this.userChangedWatcher = new Signal.subtle.Watcher(() => {

      queueMicrotask(() => {

        this._user = userChanged.get();
        this.userChangedWatcher.watch();
      });
    });

    this.userChangedWatcher.watch(userChanged);
  }

  connectedCallback() {

    super.connectedCallback();

    this._online = navigator.onLine;

    window.addEventListener("online", () => this._online = true );
    window.addEventListener("offline", () => this._online = false );

    // The idea here is that the userChangedWatcher may well not be hooked up in time to catch the
    // signal update from wherever, so we add this as a way of components just setting their state
    // explicitly from the signal
    this._user = userChanged.get();
    this._userChanged?.();
  }

  /**
   * Convenience wrapper for sakai-18n.tr.
   *
   * Example:
   *
   * confirm_coolness=This is {} cool
   * let translated = mySakaiElementSubclass.tr("confirm_coolness", ["really"]);
   *
   * @param {string} key The i18n key we want to translate
   * @params {(string[]|Object)} options This can either be an array of replacement strings, or an object
   * which contains token names to values, as well as options like debug: true.
   * @param {boolean} [forceBundle=this.bundle] The bundle to use in preference to this.bundle
   */
  tr(key, options, forceBundle) {
    return tr(forceBundle || this.bundle, key, options);
  }

  loadTranslations(options) {

    if (typeof options === "string") {
      this.bundle = options;
    } else {
      this.bundle = options.bundle;
    }

    const promise = loadProperties(options);
    promise.then(r => this._i18n = r);
    return promise;
  }

  setSetting(component, name, value) {

    const currentString = localStorage.getItem(`${component}-settings`);
    const settings = currentString ? JSON.parse(currentString) : {};
    settings[name] = value;
    localStorage.setItem(`${component}-settings`, JSON.stringify(settings));
  }

  getSetting(component, name) {

    const currentString = localStorage.getItem(`${component}-settings`);
    return !currentString ? null : JSON.parse(currentString)[name];
  }

  static styles = [
    ...getDocumentStyleSheets(),
    css`
      select[multiple], select[size]:not([size='1']) {
        background-image: none;
      }
    `
  ];
}
