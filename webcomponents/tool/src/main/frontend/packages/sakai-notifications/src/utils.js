export const fetchNotifications = url => {

  console.debug("loadNotifications");

  return fetch(url, {
    cache: "no-cache",
    headers: { "Content-Type": "application/json" },
  })
  .then (r => {

    if (r.ok) {
      return r.json();
    }

    throw new Error(`Network error while retrieving notifications from ${url}`);
  })
  .catch(error => console.error(error));
};

export const markNotificationsViewed = (siteId, toolId) => {

  const url = `/api/users/me/notifications/markViewed${siteId && toolId ? `?siteId=${siteId}&toolId=${toolId}` : ""}`;
  return fetch(url, { method: "POST", credentials: "include" });
};
