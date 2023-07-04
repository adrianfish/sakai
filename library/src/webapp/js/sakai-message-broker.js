portal = portal || {};
portal.notifications = portal.notifications || {};

/*
portal.notifications.urlB64ToUint8Array = base64String => {

  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding)
    .replace(/\-/g, "+")
    .replace(/_/g, "/");
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
};
*/

portal.notifications.pushCallbacks = new Map();

portal.notifications.registerPushCallback = (toolOrAll, cb) => {

  console.debug(`Registering push callback for ${toolOrAll}`);

  const callbacks = portal.notifications.pushCallbacks.get(toolOrAll) || [];
  callbacks.push(cb);
  portal.notifications.pushCallbacks.set(toolOrAll, callbacks);
};

console.debug("Registering sakai-service-worker ...");
navigator.serviceWorker.register("/sakai-service-worker.js").then(reg => {

  portal.notifications.setupServiceWorkerListener = () => {

    console.debug("setupServiceWorkerListener");

    // When the worker's EventSource receives an event it will message us (the client). This
    // code looks up the matching callback and calls it.
    navigator.serviceWorker.addEventListener('message', e => {

      const allCallbacks = portal.notifications.pushCallbacks.get("all");
      allCallbacks && allCallbacks.forEach(cb => cb(e.data));
      const toolCallbacks = portal.notifications.pushCallbacks.get(e.data.tool);
      toolCallbacks && toolCallbacks.forEach(cb => cb(e.data));
    });
  };

  portal.notifications.subscribe = reg => {

    // Subscribe with the public key
    reg.pushManager.subscribe({
      userVisibleOnly: true,
      //applicationServerKey: portal.notifications.urlB64ToUint8Array(portal.notifications.applicationServerKey),
      applicationServerKey: portal.notifications.applicationServerKey,
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
      })
      .catch (error => console.error(error))
      .finally(() => resolve());
    });

  };

  portal.notifications.subscribeIfPermitted = reg => {

    return new Promise(resolve => {

      if (window.Notification && Notification.permission !== "default") {
        resolve();
      } else {
        console.debug("Requesting notifications permission ...");

        if (window.Notification) {
          Notification.requestPermission().then(permission => {

            if (Notification.permission === "granted") {

              console.debug("Permission granted. Subscribing ...");
              portal.notifications.subscribe(reg);
            }
          })
          .catch (error => console.error(error));
        } else {
          portal.notifications.subscribe(reg);
        }
      }
    });
  };

  // We set this up for other parts of the code to call, without needing to register
  // the service worker first. We capture the registration in the closure.
  portal.notifications.callSubscribeIfPermitted = () => portal.notifications.subscribeIfPermitted(reg);

  /**
   * Create a promise which will setup the service worker and message registration
   * functions before fulfilling. Consumers can wait on this promise and then register
   * the push event they want to listen for. For an example of this, checkout
   * sui-notifications.js in webcomponents
   */
  portal.notifications.setup = new Promise((resolve, reject) => {

    fetch("/api/keys/sakaipush").then(r => r.text()).then(key => portal.notifications.applicationServerKey = key);

    const worker = reg.active;

    if (worker) {

      // The serivce worker is already active, setup the listener and register function.

      portal.notifications.setupServiceWorkerListener();
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

            portal.notifications.setupServiceWorkerListener();
            console.debug("Worker registered and setup");
            resolve();
          }
        });
      });
    }
  });

  portal.notifications.setup.then(() => console.debug("Notifications setup complete"));
});


portal.notifications.setAppBadge = number => {

  if ( 'setAppBadge' in navigator ) {
    navigator.setAppBadge(number);
  } else {
    console.debug('setAppBadge not available');
  }
};

portal.notifications.clearAppBadge = () => {

  if ( 'clearAppBadge' in navigator ) {
    navigator.clearAppBadge();
  } else {
    console.debug("clearAppBadge not available");
  }
};

portal.notifications.onLogin = () => {

  caches.open("sakai-assets").then(cache => cache.add("/pwa/"));

  const lastSubscribedUser = localStorage.getItem("last-sakai-user");
  const differentUser = lastSubscribedUser && lastSubscribedUser !== portal.user.id;
  localStorage.setItem("last-sakai-user", portal.user.id);

  // If the logged in user has changed, we should resubscribe this device
  if (differentUser) {
    console.debug("Different user. Removing the current subscription ...");
    navigator.serviceWorker.register("/sakai-service-worker.js").then(reg => {

      if (reg.pushManager) {
        reg.pushManager.getSubscription().then(subscription => {

          if (subscription) {
            subscription.unsubscribe().then(success => {
              portal.notifications.callSubscribeIfPermitted();
            });
          } else {
            portal.notifications.callSubscribeIfPermitted();
          }
        });
      }
    });
  }
};

portal.notifications.logout = () => {

  const url = "/api/users/me/pushEndpoint/delete";
  fetch(url, {
    credentials: "include",
    method: "POST",
    body: new URLSearchParams({ browserFingerprint: getBrowserFingerprint() }),
  })
  .then(r => {

    if (!r.ok) {
      throw new Error(`Network error while deleting push endpoint: ${url}`);
    }

    console.debug("Push endpoint details deleted successfully");
  })
  .catch (error => console.error(error));

  portal.notifications.pushCallbacks.clear();

  caches.open("sakai-assets").then(cache => cache.delete("/direct/portal/notifications.json"));

  navigator.serviceWorker.ready.then(reg => reg.active.postMessage("LOGOUT"));
};

if (portal?.user?.id) {

  const lastSubscribedUser = localStorage.getItem("last-sakai-user");
  const differentUser = lastSubscribedUser && lastSubscribedUser !== portal.user.id;
  localStorage.setItem("last-sakai-user", portal.user.id);

  // If the logged in user has changed, we should resubscribe this device
  if (differentUser) {
    console.debug("Different user. Removing the current subscription ...");
    navigator.serviceWorker.register("/sakai-service-worker.js").then(reg => {

      if (reg.pushManager) {
        reg.pushManager.getSubscription().then(subscription => {

          subscription && subscription.unsubscribe().then(success => {
            portal.notifications.callSubscribeIfPermitted();
          });
        });
      }
    });
  }
}
