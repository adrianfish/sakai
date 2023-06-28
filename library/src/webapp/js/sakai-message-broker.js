portal = portal || {};
portal.notifications = portal.notifications || {};

console.log("HERE1");
if ("serviceWorker" in navigator) {
  console.log("ServiceWorker is supported");
}

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

if (portal?.user?.id) {

  const lastSubscribedUser = localStorage.getItem("last-sakai-user");
  const differentUser = lastSubscribedUser && lastSubscribedUser !== portal.user.id;

  navigator.serviceWorker.register("/sakai-service-worker.js").then(registration => {

    if (differentUser) {
      console.debug("Different user. Removing the current subscription ...");

      if (registration.pushManager) {
        registration.pushManager.getSubscription().then(subscription => subscription && subscription.unsubscribe());
      }
    }
  });

  if (portal.notifications.pushEnabled && (Notification.permission === "default" || differentUser)) {

    // Permission has neither been granted or denied yet.

    console.debug("No permission set or user changed");

    console.debug("about to register");

    navigator.serviceWorker.register("/sakai-service-worker.js").then(registration => {

      portal.notifications.callSubscribeIfPermitted = () => {
        return portal.notifications.subscribeIfPermitted(registration);
      };

      window.addEventListener("DOMContentLoaded", () => {

        console.debug("DOM loaded. Setting up permission triggers ...");

        // We're using the bullhorn buttons to trigger the permission request from the user. You
        // can only instigate a permissions request from a user action.
        document.querySelectorAll(".portal-notifications-button").forEach(b => {

          b.addEventListener("click", e => {

            portal.notifications.subscribeIfPermitted(registration);
          }, { once: true });
        });
      });
    });
  }

  portal.notifications.subscribeIfPermitted = registration => {

    return new Promise(resolve => {

      console.log("Requesting notifications permission ...");

      Notification.requestPermission().then(permission => {

        console.log("HDSDFSDSDSD");

        console.log(permission);

        if (permission === "granted") {

          console.debug("Permission granted. Subscribing ...");

          // We have permission, Grab the public app server key.
          fetch("/api/keys/sakaipush").then(r => r.text()).then(key => {

            console.debug("Got the key. Subscribing for push ...");

            // Subscribe with the public key
            registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: key }).then(sub => {

              console.debug("Subscribed. Sending details to Sakai ...");

              const params = {
                endpoint: sub.endpoint,
                auth: sub.toJSON().keys.auth,
                userKey: sub.toJSON().keys.p256dh,
                browserFingerprint: getBrowserFingerprint(),
              };

              const url = "/api/users/me/prefs/pushEndpoint";
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
                localStorage.setItem("last-sakai-user", portal.user.id);
              })
              .catch (error => console.error(error))
              .finally(() => resolve());
            });
          });
        }
      })
      .catch (error => console.error(error));
    });
  };

  portal.notifications.setupServiceWorkerListener = () => {

    console.debug("setupServiceWorkerListener");

    // When the worker's EventSource receives an event it will message us (the client). This
    // code looks up the matching callback and calls it.
    navigator.serviceWorker.addEventListener('message', e => {

      console.log(e.data);

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
      .then(registration => {

        const worker = registration.active;

        if (worker) {

          // The serivce worker is already active, setup the listener and register function.

          portal.notifications.setupServiceWorkerListener();
          console.debug("Worker registered and setup");
          resolve();
        } else {
          console.debug("No active worker. Waiting for update ...");

          // Not active. We'll listen for an update then hook things up.

          registration.addEventListener("updatefound", () => {

            console.debug("Worker updated. Waiting for state change ...");

            const installingWorker = registration.installing;

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
