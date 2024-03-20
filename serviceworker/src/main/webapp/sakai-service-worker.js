const PRECACHE_ASSETS = [
  "/favicon.ico",
  "/manifest.json",
  "/pwa/",
  "/pwa/index.html",
  "/api/keys/sakaipush",
  "/images/sakaiger_144.png",
  "/images/sakaiger_192.png",
  "/images/sakaiger_512.png",
  "/webcomponents/bundles/pwa.js",
  "/library/skin/default-skin/pwa.css",
  "/profile2-tool/images/no_image_thumbnail.gif",
  "/library/skin/default-skin/images/sakaiLogo.png",
  "/library/skin/default-skin/fonts/bootstrap-icons.woff",
  "/library/skin/default-skin/fonts/bootstrap-icons.woff2",
];

const CACHED_DATA_PATTERNS = [
  /profile\/.+\/image\/thumb$/,
  /api\/.*$/,
];

const cacheName = "sakai-v1";

// The "install" event listener. Cache enough stuff to make the pwa available offline. Look at
// the "fetch" event listener to see the code which compliments this, the fetch proxy code.
self.addEventListener("install", async event => {

  event.waitUntil(
    caches.open(cacheName).then(cache => cache.addAll(PRECACHE_ASSETS))
  );
});

self.addEventListener("activate", async event => {

  console.debug("Removing any old caches ...");
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(name => {
          if (cacheName !== name) {
            return caches.delete(name);
          }
        }),
      );
    }),
  );

  if (self.registration.navigationPreload) { await self.registration.navigationPreload.enable(); }

  console.debug("Activated. Claiming clients ...");
  return self.clients.claim();
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

  if (event.data !== "LOGOUT") return;

  self.registration.pushManager?.getSubscription().then(sub => sub?.unsubscribe());

  // Clean out all the dynamic data from the cache
  /*
  caches.open(cacheName).then(cache => {
    cache.keys().then(keys => {
      keys.forEach(key => {
        CACHED_DATA_PATTERNS.forEach(pattern => {
          if (key.url.match(pattern)) {
            cache.delete(key);
          }
        });
      });
    });
  });
  */
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

  if (!navigator.onLine) {
    console.debug(`We're not online. Using cache only strategy for ${event.request.url} ...`);
    event.respondWith(cacheOnlyStrategy(event.request, cacheName));
  } else {
    const url = new URL(event.request.url);

    if (PRECACHE_ASSETS.includes(url.pathname)) {
      console.debug(`${event.request.url} is precached and likely longlived. Cache first strategy ...`);
      event.respondWith(cacheFirstStrategy(event, cacheName, url));
    } else {
      console.debug(`${event.request.url} is not precached. Network first strategy ...`);
      event.respondWith(networkFirstStrategy(event.request, cacheName));
    }
  }
});

async function cacheIfSuitable(request, response, cacheName) {

  if (request.method !== "GET") return;

  const cache = await caches.open(cacheName);

  CACHED_DATA_PATTERNS.forEach(pattern => {

    if (request.url.match(pattern)) {
      console.debug(`Caching ${request.url} ...`);
      cache.put(request, response.clone());
    }
  });
}

async function cacheOnlyStrategy(request, cacheName) {

  const cache = await caches.open(cacheName);
  return cache.match(request);
}

async function cacheFirstStrategy(event, cacheName, url) {

  const cache = await caches.open(cacheName);
  const cachedResponse = await cache.match(url.pathname);

  if (cachedResponse) {
    console.debug(`${url.pathname} is in the cache.`);
    // We have a cached copy so return it. But first kick off a cache refresh so the latest
    // is pulled and cached asynchronously.
    console.debug(`Triggering an asynchronous refresh of the cached copy of ${url.pathname} ...`);
    cache.add(url.pathname);
  } else {
    console.debug(`${event.request.url} is not in the cache. It should be.`);
  }

  return cachedResponse;
}

// Network-first strategy: Try network, fallback to cache
async function networkFirstStrategy(request, cacheName) {

  // Try network first
  const networkResponse = await fetch(request);

  console.debug(`Network request for ${request.url} returned ${networkResponse.status} ...`);

  // Cache the response for later
  if (networkResponse.ok) {
    //cacheIfSuitable(request, networkResponse, cacheName);
    return networkResponse;
  }

  console.debug(`Network request for ${request.url} failed. Trying cache ...`);

  const cache = await caches.open(cacheName);

  const cachedResponse = await cache.match(request);

  if (!cachedResponse) {
    console.warn(`Request ${request.url} failed from network and cache.`);
    return networkResponse;
  }

  return cachedResponse;
}
