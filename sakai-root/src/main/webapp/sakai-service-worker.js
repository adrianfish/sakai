/**
 * Message all the client windows or tabs of this worker
 */
self.messageClients = async message => {

  const clients = await self.clients.matchAll({ includeUncontrolled: true });
  clients && clients.forEach(c => c.postMessage(message));
};

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

self.addEventListener("fetch", async event => {

  console.log(event.request);
  if (event.request.method === 'GET') {
  }

  // if we are offline, go to the cache directly
  /*
  if (event.request.method === 'GET') {

    event.respondWith(caches.open("sakai-assets").then(cache => {

      cache.match(event.request).then(cachedResp => {

        return cachedResp || fetch(event.request.url).then(fetchedResp => {

            cache.put(event.request, fetchedResp.clone());
            return fetchedResp;
          });
      });
    }));
  }
  */
});

