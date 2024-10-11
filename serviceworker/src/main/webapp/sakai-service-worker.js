self.addEventListener("activate", event => {

  console.debug("Activated. Claiming clients ...");
  return self.clients.claim();
});

// The "install" event listener. Cache enough stuff to make the pwa available offline. Look at
// the "fetch" event listener to see the code which compliments this, the fetch proxy code.
self.addEventListener("install", async event => {

  event.waitUntil(
    caches
      .open("sakai-v1")
      .then(cache =>
        cache.addAll([
          "/favicon.ico",
          "/pwa/",
          "/api/keys/sakaipush",
          "/images/sakaiger_144.png",
          "/images/sakaiger_192.png",
          "/images/sakaiger_512.png",
          "/library/skin/default-skin/images/sakaiLogo.png",
          "/library/skin/default-skin/fonts/bootstrap-icons.woff",
          "/library/skin/default-skin/fonts/bootstrap-icons.woff2?24e3eb84d0bcaf83d77f904c78ac1f47",
          "/library/skin/default-skin/fonts/fontawesome-webfont.woff?v=4.7.0",
          "/library/skin/default-skin/fonts/fontawesome-webfont.woff2?v=4.7.0",
          "/library/skin/default-skin/pwa.css",
          "/library/webjars/bootstrap/5.2.0/js/bootstrap.bundle.min.js",
          "/webcomponents/bundles/pwa.js",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=account-panel-wc",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=announcements",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=calendar-wc",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=dashboard",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=dashboard-widget",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=dialog-content",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=forums",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=grades",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=profile-wc",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=sakai-notifications",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=sakai-pwa",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=site-picker",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=tasks",
          "/sakai-ws/rest/i18n/getI18nProperties?locale=en_GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=widgetpanel",
        ])
      )
  );
});

/**
 * Message all the client windows or tabs of this worker
 */
self.messageClients = async message => {

  return new Promise(async resolve => {

    const clients = await self.clients.matchAll({ includeUncontrolled: true });
    clients?.forEach(c => c.postMessage(message));
    resolve();
  });
};

self.addEventListener("message", event => {

  if (event.data === "LOGOUT") {
    self.registration.pushManager?.getSubscription().then(sub => sub?.unsubscribe());
  }
});

// We just pass push events straight onto the clients.
self.addEventListener("push", event => {

  const json = event.data.json();

  if (self.registration.showNotification) {
    event.waitUntil(self.registration.showNotification(json.title));
  }

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

self.addEventListener("fetch", async event => {

  console.debug(`Trying to get ${event.request.url} from the cache ...`);

  // Prevent the default, and handle the request ourselves.
  event.respondWith(
    (async () => {

      // Check the cache
      const cache = await caches.open("sakai-v1");
      const cachedResponse = await cache.match(event.request);


      if (cachedResponse) {
        console.debug(`${event.request.url} is in the cache.`);
        if (navigator.onLine) {
          // We have a cached copy so return it. But first kick off a cache refresh so the latest
          // is pulled and cached asynchronously.
          console.debug(`We're online. Triggering an asynchronous refresh of the cached copy of ${event.request.url} ...`);
          event.waitUntil(cache.add(event.request));
        }
        return cachedResponse;
      }

      console.debug(`${event.request.url} is not currently in the cache`);

      if (navigator.onLine) {

        console.debug(`We're online. Fetching ${event.request.url} ...`);

        // Fetch it and cache if needed.
        return fetch(event.request).then(fetchedResponse => {

          if (event.request.method === "GET" && (event.request.url.match(/profile.*thumb$/)
                                                  || event.request.url.match(/api\/users\/.*\/profile$/)
                                                  || event.request.url.match(/resourcebundle/))) {
            console.debug(`Caching ${event.request.url} ...`);
            cache.put(event.request, fetchedResponse.clone());
          }
          return fetchedResponse;
        });
      } else {
        console.debug(`We're not online. ${event.request.url} will not be fetched or cached.`);
        return undefined;
      }
    })()
  );
});
