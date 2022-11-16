portal = portal || {};
portal.notifications = portal.notifications || {};

portal.notifications.pushCallbacks = new Map();
portal.notifications.sseCallbacks = new Map();

if (portal?.user?.id) {

  if (portal.notifications.pushEnabled && Notification.permission === "default") {

    // Permission has neither been granted or denied yet.

    if (portal.notifications.debug) console.debug("No permission set");

    navigator.serviceWorker.register("/api/sakai-sse-service-worker.js").then(registration => {

      if (!registration.pushManager) {
        // This must be Safari, or maybe IE3 or something :)
        console.warn("No pushManager on this registration");
        return;
      }

      window.addEventListener("DOMContentLoaded", () => {

        if (portal.notifications.debug) console.debug("DOM loaded. Setting up permission triggers ...");

        // We're using the bullhorn buttons to trigger the permission request from the user. You
        // can only instigate a permissions request from a user action.
        document.querySelectorAll(".portal-notifications-button").forEach(b => {

          b.addEventListener("click", e => {

            if (portal.notifications.debug) console.debug("Requesting notifications permission ...");

            Notification.requestPermission().then(permission => {

              if (permission === "granted") {

                if (portal.notifications.debug) console.log("Permission granted. Subscribing ...");

                // We have permission, Grab the public app server key.
                fetch("/api/keys/sakaipush").then(r => r.text()).then(key => {

                  if (portal.notifications.debug) console.log("Got the key. Subscribing for push ...");

                  // Subscribe with the public key
                  registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: key }).then(sub => {

                    if (portal.notifications.debug) console.debug("Subscribed. Sending details to Sakai ...");

                    const url = "/api/users/me/prefs/pushEndpoint";
                    fetch(url, {
                      credentials: "include",
                      method: "POST",
                      body: new URLSearchParams({ endpoint: sub.endpoint, auth: sub.toJSON().keys.auth, userKey: sub.toJSON().keys.p256dh }),
                    })
                    .then(r => {

                      if (!r.ok) {
                        throw new Error(`Network error while posting push endpoint: ${url}`);
                      }

                      if (portal.notifications.debug) console.debug("Subscription details sent successfully");
                    })
                    .catch (error => console.error(error));
                  });
                });
              }
            });
          }, { once: true });
        });
      });
    });
  }

  portal.notifications.setupServiceWorkerListener = () => {

    if (portal.notifications.debug) console.debug("setupServiceWorkerListener");

    // When the worker's EventSource receives an event it will message us (the client). This
    // code looks up the matching callback and calls it.
    navigator.serviceWorker.addEventListener('message', e => {

      if (e.data.type) {

        if (portal.notifications.debug) {
          console.debug(`SSE MESSAGE RECEIVED FOR TYPE: ${e.data.type}`);
          console.debug(e.data.data);
        }

        portal.notifications.sseCallbacks.has(e.data.type) && portal.notifications.sseCallbacks.get(e.data.type)(e.data.data);
      } else {

        if (portal.notifications.debug) {
          console.debug("PUSH MESSAGE RECEIVED");
          console.log(e.data);
        }

        const allCallbacks = portal.notifications.pushCallbacks.get("all");
        allCallbacks && allCallbacks.forEach(cb => cb(e.data));
        const toolCallbacks = portal.notifications.pushCallbacks.get(e.data.tool);
        toolCallbacks && toolCallbacks.forEach(cb => cb(e.data));
      }
    });
  };

  portal.notifications.setupRegisterForMessages = worker => {

    portal.notifications.registerForMessages = (sakaiEvent, callback) => {

      if (portal.notifications.debug) console.debug(`Registering callback on ${sakaiEvent} ...`);
      portal.notifications.sseCallbacks.set(sakaiEvent, callback);

      // Tell the worker we want to listen for this event on the EventSource
      if (portal.notifications.debug) console.debug(`Telling the worker we want an SSE event for ${sakaiEvent} ...`);
      worker.postMessage(sakaiEvent);
    };
  };

  portal.notifications.registerPushCallback = (toolOrAll, cb) => {

    if (portal.notifications.debug) console.debug(`Registering push callback for ${toolOrAll}`);

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

    if (portal.notifications.debug) console.debug("Registering worker ...");

    navigator.serviceWorker.register("/api/sakai-sse-service-worker.js")
      .then(registration => {

        const worker = registration.active;

        if (worker) {

          // The serivce worker is already active, setup the listener and register function.

          portal.notifications.setupServiceWorkerListener();
          portal.notifications.setupRegisterForMessages(worker);
          if (portal.notifications.debug) console.debug("Worker registered and setup");
          resolve();
        } else {
          if (portal.notifications.debug) console.debug("No active worker. Waiting for update ...");

          // Not active. We'll listen for an update then hook things up.

          registration.addEventListener("updatefound", () => {

            if (portal.notifications.debug) console.debug("Worker updated. Waiting for state change ...");

            const installingWorker = registration.installing;

            installingWorker.addEventListener("statechange", e => {

              if (portal.notifications.debug) console.debug("Worker state changed");

              if (e.target.state === "activated") {

                if (portal.notifications.debug) console.debug("Worker activated. Setting up ...");

                // The service worker has been updated, setup the listener and register function.

                portal.notifications.setupServiceWorkerListener();
                portal.notifications.setupRegisterForMessages(installingWorker);
                if (portal.notifications.debug) console.debug("Worker registered and setup");
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
} else {
  // Logged out. Tell the worker to close the EventSource.
  navigator.serviceWorker.register("/api/sakai-sse-service-worker.js").then(registration => {

    const worker = registration.active;
    if (portal.notifications.debug) console.debug("Logged out. Sending close event source signal to worker ...");
    worker && worker.postMessage({ signal: "closeEventSource" });
  });
}
