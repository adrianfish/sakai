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

    self.eventSource && self.eventSource.close();
    self.eventSource = undefined;
  } else if (!self.eventSource) {

    // The default of a newly logged in user wanting to setup the event source

    self.eventSource = new EventSource("/api/users/me/events");
    self.eventSource.onerror = e => console.debug("events connection failed");

    self.eventSource.onopen = e => {

      // It's open, so we can add our single event listener to it. When events come in, this code
      // passes them onto the worker's clients. At the moment, that is the sakai-message-broker
      // code. Other js in Sakai will talk to the broker, not to the service worker directly.

      self.eventSource.addEventListener(clientEvent.data, message => {

        const stripped = { type: message.type, data: JSON.parse(message.data) };
        self.messageClients(stripped);
      });
    };
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
