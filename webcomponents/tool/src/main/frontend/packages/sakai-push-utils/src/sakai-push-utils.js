import getBrowserFingerprint from "get-browser-fingerprint";
import { getUserId } from "@sakai-ui/sakai-portal-utils";

export const NOT_PUSH_CAPABLE = "NOT_PUSH_CAPABLE";

const pushCallbacks = new Map();
const pushPermissionRequestCompletedCallbacks = [];

export const registerPushPermissionRequestCompletedCallback = cb => {
  pushPermissionRequestCompletedCallbacks.push(cb);
};

const serviceWorkerPath = "/sakai-service-worker.js";

navigator.serviceWorker.register(serviceWorkerPath);

document.addEventListener("DOMContentLoaded", () => {

  if (getUserId() && Notification?.permission !== "granted") {
    document.querySelectorAll(".portal-notifications-indicator").forEach(b => b.classList.add("d-none"));
    document.querySelectorAll(".portal-notifications-no-permissions-indicator").forEach(b => b.classList.remove("d-none"));
  }
});

const subscribe = (reg, resolve) => {

  const pushKeyUrl = "/api/keys/sakaipush";
  console.debug(`Fetching the push key from ${pushKeyUrl} ...`);
  fetch(pushKeyUrl).then(r => r.text()).then(key => {

    // Subscribe with the public key
    reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: key,
    })
    .then(sub => {

      console.debug("Subscribed. Sending details to Sakai ...");

      const params = {
        endpoint: sub.endpoint,
        auth: sub.toJSON().keys.auth,
        userKey: sub.toJSON().keys.p256dh,
        browserFingerprint: getBrowserFingerprint(),
      };

      const url = "/api/users/me/pushEndpoint";
      fetch(url, {
        credentials: "include",
        method: "POST",
        body: new URLSearchParams(params),
      })
      .then(r => {

        if (!r.ok) {
          throw new Error(`Network error while posting push endpoint: ${url}`);
        }

        console.debug("Subscription details sent successfully");

        document.querySelectorAll(".portal-notifications-no-permissions-indicator").forEach(b => b.classList.add("d-none"));
      })
      .catch (error => console.error(error))
      .finally(() => resolve("granted"));
    });
  });
}; // subscribe

export const subscribeIfPermitted = reg => {

  console.debug("subscribeIfPermitted");

  document.body?.querySelectorAll(".portal-notifications-indicator")
    .forEach(el => el.classList.remove("d-none"));

  return new Promise(resolve => {

    if (Notification?.permission === "granted") {
      console.debug("Permission already granted. Subscribing ...");
      subscribe(reg, resolve);
    } else if (Notification?.permission === "default") {

      console.debug("Push permission not set yet");

      try {
        Notification.requestPermission().then(permission => {

          if (permission === "granted") {

            console.debug("Permission granted. Subscribing ...");
            subscribe(reg, resolve);
          } else {
            resolve(permission);
          }
        })
        .catch (error => console.error(error));
      } catch (err) {
        console.error(err);
      }
    } else {
      resolve();
    }
  });
}; // subscribeIfPermitted

const serviceWorkerMessageListener = e => {

  // When the worker's EventSource receives an event it will message us (the client). This
  // code looks up the matching callback and calls it.

  const notificationsCallbacks = pushCallbacks.get("notifications");
  notificationsCallbacks?.forEach(cb => cb(e.data));
};

const setupServiceWorkerListener = () => {

  console.debug("setupServiceWorkerListener");

  navigator.serviceWorker.addEventListener("message", serviceWorkerMessageListener);
};

export const checkUserChangedThenSet = userId => {

  if (!userId) return false;

  localStorage.setItem("last-sakai-user", userId);

  return differentUser;
};

/**
 * Create a promise which will setup the service worker and message registration
 * functions before fulfilling. Consumers can wait on this promise and then register
 * the push event they want to listen for. For an example of this, checkout
 * sakai-notifications.js in webcomponents
 */
export const pushSetupComplete = new Promise((resolve, reject) => {

  if (!navigator.serviceWorker || !window.Notification) {
    console.error("Service worker not supported");
    reject(NOT_PUSH_CAPABLE);
    return;
  }

  navigator.serviceWorker.register(serviceWorkerPath).then(reg => {

    if (!reg.pushManager) {
      reject(NOT_PUSH_CAPABLE);
      return;
    }

    const worker = reg.active;

    if (worker) {

      // The service worker is already active, setup the listener and register function.

      setupServiceWorkerListener();
      console.debug("Worker registered and setup");
      resolve();
    } else {
      console.debug("No active worker. Waiting for update ...");

      // Not active. We'll listen for an update then hook things up.

      reg.addEventListener("updatefound", () => {

        console.debug("Worker updated. Waiting for state change ...");

        const installingWorker = reg.installing;

        installingWorker.addEventListener("statechange", e => {

          console.debug("Worker state changed");

          if (e.target.state === "activated") {

            console.debug("Worker activated. Setting up ...");

            // The service worker has been updated, setup the listener and register function.

            setupServiceWorkerListener();
            console.debug("Worker registered and setup");
            resolve();
          }
        });
      });
    }
  });
}); // pushSetupComplete

// We set this up for other parts of the code to call, without needing to register
// the service worker first. We capture the registration in the closure.
export const callSubscribeIfPermitted = async () => {

  const reg = await navigator.serviceWorker.register(serviceWorkerPath);
  return subscribeIfPermitted(reg);
};

export const registerPushCallback = (toolOrNotifications, cb, max) => {

  console.debug(`Registering push callback for ${toolOrNotifications}`);

  const callbacks = pushCallbacks.get(toolOrNotifications) || [];

  if (callbacks.length === max) {
    console.debug(`Max number of callbacks reached for ${toolOrNotifications}`);
    return;
  }

  callbacks.push(cb);
  pushCallbacks.set(toolOrNotifications, callbacks);
};

if (checkUserChangedThenSet(getUserId())) {

  console.debug("The user has changed. Unsubscribing the previous user ...");

  navigator.serviceWorker.register(serviceWorkerPath).then(reg => {

    // The user has changed. If there is a subscription, unsubscribe it and try to subscribe
    // for the new user.
    reg.pushManager?.getSubscription().then(sub => {

      if (sub) {
        sub.unsubscribe().finally(() => {

          if (Notification?.permission === "granted") {
            subscribeIfPermitted(reg);
          }
        });
      } else if (Notification?.permission === "granted") {
        subscribeIfPermitted(reg);
      }
    });
  });
}

export const onLogin = userId => {

  console.debug(`onLogin(${userId})`);

  const differentUser = checkUserChangedThenSet(userId);

  // If the logged in user has changed, we should resubscribe this device
  if (differentUser) {

    console.debug("The user has changed. Unsubscribing the previous user ...");

    navigator.serviceWorker.register("/sakai-service-worker.js").then(reg => {

      reg?.pushManager.getSubscription().then(sub => {

        if (sub) {
          sub.unsubscribe().finally(() => {

            if (Notification?.permission === "granted" && differentUser) {
              subscribeIfPermitted(reg);
            }
          });
        } else if (Notification?.permission === "granted" && differentUser) {
          subscribeIfPermitted(reg);
        }
      });
    });
  }
};

export const logout = () => {
  navigator.serviceWorker.ready.then(reg => reg.active.postMessage("LOGOUT"));
};
