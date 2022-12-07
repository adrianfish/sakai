/**
 * Message all the client windows or tabs of this worker
 */
self.messageClients = async message => {

  const clients = await self.clients.matchAll({ includeUncontrolled: true });
  clients && clients.forEach(c => c.postMessage(message));
};

self.addEventListener("message", clientEvent => {

  if (clientEvent.data === "closeEventSource") {

    // A special data indicating to close the event source

    console.debug("Closing event source ...");
    self.eventSource && self.eventSource.close();
    self.eventSource = undefined;
  } else if (!self.eventSource) {
    console.debug("EventSource not opened yet. Opening ...");

    // The default of a newly logged in user wanting to setup the event source

    self.eventSource = new EventSource("/api/users/me/events");
    console.debug("EventSource source created. Waiting for open event ...");
    self.eventSource.onerror = e => console.debug("events connection failed");

    self.eventSource.onopen = e => {

      console.debug("Event source opened ...");

      // It's open, so we can add our single event listener to it. When events come in, this code
      // passes them onto the worker's clients. At the moment, that is the sakai-message-broker
      // code. Other js in Sakai will talk to the broker, not to the service worker directly.

      self.eventSource.addEventListener(clientEvent.data, message => {

        console.debug("SSE message received");
        console.debug(message);

        const stripped = { type: message.type, data: JSON.parse(message.data) };
        self.messageClients(stripped);
      });
    };
  }
});

self.addEventListener("install", event => {

  console.debug("Service worker install");

  event.waitUntil(caches.open("sakai-assets"));

    /*
  event.waitUntil((() => {
    caches.open("sakai-assets").then(cache => {

    caches.open("sakai-assets").then(cache => {

      cache.add("/pwa/index.html");
      cache.add("/library/webjars/bootstrap/5.2.0/js/bootstrap.min.js");
      cache.add("/library/skin/default-skin/fonts/fontawesome-webfont.woff?v=4.7.0");
      cache.add("/library/skin/default-skin/fonts/fontawesome-webfont.woff2?v=4.7.0");
      cache.add("/library/skin/default-skin/fonts/fontawesome-webfont.ttf?v=4.7.0");
      cache.add("/library/skin/default-skin/pwa.css");
      cache.add("/library/js/sakai-message-broker.js");
      cache.add("/webcomponents/sui-pwa/sui-pwa.js");
    });
  })());
  */
});

self.addEventListener("fetch", async event => {

  console.log(event.request);

  // if we are offline, go to the cache directly
  if (event.request.method === 'GET') {

    event.respondWith(caches.open("sakai-assets").then(cache => {

      cache.match(event.request).then(cachedResp => {

        return cachedResp || fetch(event.request.url).then(fetchedResp => {

            cache.put(event.request, fetchedResp.clone());
            return fetchedResp;
          });
      });

        /*
        console.log(resp);

        if (resp) return resp;

        console.log("NOT CACHED");
        const requestUrl = event.request.clone();
        fetch(requestUrl);
      });
        */
    }));
  }
});

// We just pass push events straight onto the clients.
self.addEventListener("push", event => self.messageClients(event.data.json()));

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
