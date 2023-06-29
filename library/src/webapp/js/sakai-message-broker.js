portal = portal || {};
portal.notifications = portal.notifications || {};

portal.notifications.pushCallbacks = new Map();

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

portal.notifications.logout = () => {

  const params = {
    browserFingerprint: getBrowserFingerprint(),
  };

  const url = "/api/users/me/pushEndpoint/delete";
  fetch(url, {
    credentials: "include",
    method: "POST",
    body: new URLSearchParams(params),
  })
  .then(r => {

    if (!r.ok) {
      throw new Error(`Network error while deleting push endpoint: ${url}`);
    }

    console.debug("Push endpoint details deleted successfully");
  })
  .catch (error => console.error(error));

  navigator.serviceWorker.ready.then(reg => reg.active.postMessage("LOGOUT"));
};

if (portal?.user?.id) {

  const lastSubscribedUser = localStorage.getItem("last-sakai-user");
  const differentUser = lastSubscribedUser && lastSubscribedUser !== portal.user.id;
  localStorage.setItem("last-sakai-user", portal.user.id);


  navigator.serviceWorker.register("/sakai-service-worker.js").then(reg => {

    if (differentUser) {
      console.debug("Different user. Removing the current subscription ...");

      if (reg.pushManager) {
        reg.pushManager.getSubscription().then(subscription => subscription && subscription.unsubscribe());
      }
    }
  });

  if (portal.notifications.pushEnabled) {

    // Permission has neither been granted or denied yet.

    console.debug("No permission set or user changed");

    console.debug("about to register");

    navigator.serviceWorker.register("/sakai-service-worker.js").then(reg => {

      portal.notifications.callSubscribeIfPermitted = () => portal.notifications.subscribeIfPermitted(reg);

      window.addEventListener("DOMContentLoaded", () => {

        console.debug("DOM loaded. Setting up permission triggers ...");

        // We're using the bullhorn buttons to trigger the permission request from the user. You
        // can only instigate a permissions request from a user action.
        document.querySelectorAll(".portal-notifications-button").forEach(b => {

          b.addEventListener("click", e => {

            portal.notifications.subscribeIfPermitted(reg);
          }, { once: true });
        });
      });
    });
  }

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

  portal.notifications.subscribeIfPermitted = reg => {

    return new Promise(resolve => {

      if (Notification.permission !== "default") {
        resolve();
      } else {
        console.debug("Requesting notifications permission ...");

        Notification.requestPermission().then(permission => {

          if (Notification.permission === "granted") {

            console.debug("Permission granted. Subscribing ...");

            // Subscribe with the public key
            reg.pushManager.subscribe({
              userVisibleOnly: true,
              applicationServerKey: portal.notifications.urlB64ToUint8Array(portal.notifications.applicationServerKey)
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
          }
        })
        .catch (error => console.error(error));
      }
    });
  };

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

  portal.notifications.registerPushCallback = (toolOrAll, cb) => {

    console.debug(`Registering push callback for ${toolOrAll}`);

    const callbacks = portal.notifications.pushCallbacks.get(toolOrAll) || [];
    callbacks.push(cb);
    portal.notifications.pushCallbacks.set(toolOrAll, callbacks);
  };

  /**
   * Create a promise which will setup the service worker and message registration
   * functions before fulfilling. Consumers can wait on this promise and then register
   * the push event they want to listen for. For an example of this, checkout
   * sui-notifications.js in webcomponents
   */
  portal.notifications.setup = new Promise((resolve, reject) => {

    console.debug("Registering worker ...");

    navigator.serviceWorker.register("/sakai-service-worker.js")
      .then(reg => {

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
      })
      .catch (error => {

        console.error(`Failed to register service worker ${error}`);
        reject();
      });
  });

  portal.notifications.setup.then(() => console.debug("Notifications setup complete"));
}
