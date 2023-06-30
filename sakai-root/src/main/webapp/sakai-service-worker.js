/**
 * Message all the client windows or tabs of this worker
 */
self.messageClients = async message => {

  return new Promise(async resolve => {

    const clients = await self.clients.matchAll({ includeUncontrolled: true });
    clients && clients.forEach(c => c.postMessage(message));
    resolve();
  });
};

self.addEventListener("message", event => {

  if (event.data === "LOGOUT") {
    //caches.delete("sakai-assets");
    if (self.registration.pushManager) {
      self.registration.pushManager.getSubscription().then(subscription => subscription && subscription.unsubscribe());
    }
    //self.registration.unregister();
    navigator.clearAppBadge();
  }
});

// We just pass push events straight onto the clients.
self.addEventListener("push", event => {

  const json = event.data.json();

  event.waitUntil(self.registration.showNotification(json.title));

  event.waitUntil(self.messageClients(json));
});

self.addEventListener("pushsubscriptionchange", event => {

  // The push service might have cancelled the subscription, or it might have just expired. The
  // precedure here seems to be to resubscribe but just pass the endpoint in to the application
  // server

  swRegistration.pushManager.subscribe(event.oldSubscription.options).then(subscription => {

    const url = "/api/users/me/prefs/pushEndpoint";
    fetch(url, {
      credentials: "include",
      method: "POST",
      body: new URLSearchParams({ endpoint: sub.endpoint }),
    })
    .then(r => {

      if (!r.ok) {
        console.error(`Network error while posting push endpoint: ${url}`);
      }
    })
    .catch(error => console.error(error));
  });
}, false);

// The "install" event listener. Cache enough stuff to make the pwa available offline. Look at
// the "fetch" event listener to see the code which compliments this, the fetch proxy code.
self.addEventListener("install", async event => {

  event.waitUntil(
    caches
      .open("sakai-assets")
      .then(cache =>
        cache.addAll([
          "/pwa/",
          "/library/skin/default-skin/images/sakaiLogo.png",
          "/library/skin/default-skin/fonts/bootstrap-icons.woff2",
          "/library/skin/default-skin/pwa.css",
          "/library/js/sakai-message-broker.js",
          "/library/webjars/bootstrap/5.2.0/js/bootstrap.bundle.min.js",
          "/webcomponents/assets/get-browser-fingerprint/src/index.js",
          "/webcomponents/bundles/pwa.js",
        ])
      )
  );
});

self.addEventListener("fetch", async event => {

  console.debug(`Trying to get ${event.request.url} from the cache ...`);

  // Prevent the default, and handle the request ourselves.
  event.respondWith(
    (async () => {

      // Check the cache
      const cache = await caches.open("sakai-assets");
      const cachedResponse = await cache.match(event.request);

      if (cachedResponse) {
        // We have a cached copy so return it. But first kick off a cache refresh so the latest
        // is pulled and cached asynchronously.
        event.waitUntil(cache.add(event.request));
        return cachedResponse;
      }

      // Not in the cache. Fetch it and cache if needed.
      return fetch(event.request).then(fetchedResp => {

        if (event.request.method === "GET" && (event.request.url.match(/notifications\.json/)
                                                || event.request.url.match(/profile.*thumb$/)
                                                || event.request.url.match(/resourcebundle/))) {
          cache.put(event.request, fetchedResp.clone());
        }
        return fetchedResp;
      });
    })()
  );
});
