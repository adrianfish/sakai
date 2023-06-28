/**
 * Message all the client windows or tabs of this worker
 */
self.messageClients = async message => {

  const clients = await self.clients.matchAll({ includeUncontrolled: true });
  clients && clients.forEach(c => c.postMessage(message));
};

self.addEventListener("message", event => {

  if (event.data === "LOGOUT") {
    caches.delete("sakai-assets");
    navigator.clearAppBadge();
  }
});

// We just pass push events straight onto the clients.
self.addEventListener("push", event => {

  console.log("PUSH");

  //navigator.setAppBadge();
  self.messageClients(event.data.json());
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

self.addEventListener("install", async event => {

  console.debug("INSTALL");

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
      // Try to get the response from a cache.
      const cache = await caches.open("sakai-assets");
      const cachedResponse = await cache.match(event.request);

      if (cachedResponse) {
        event.waitUntil(cache.add(event.request));
        return cachedResponse;
      }

      return fetch(event.request).then(fetchedResp => {

        if (event.request.method === "GET" && (event.request.url.match(/notifications\.json/)
                                                || event.request.url.match(/profile.*thumb$/))) {
          cache.put(event.request, fetchedResp.clone());
        }
        return fetchedResp;
      });
    })()
  );
});

