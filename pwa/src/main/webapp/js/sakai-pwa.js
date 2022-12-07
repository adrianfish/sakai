const notificationsButton = document.getElementById("pwa-notifications-button");
notificationsButton && (notificationsButton.style.display = Notification.permission !== "granted" ? "inline" : "none");

const permissionsInformation = document.getElementById("pwa-permission-information");
permissionsInformation && (permissionsInformation.style.display = Notification.permission !== "granted" ? "inline" : "none");

notificationsButton.addEventListener("click", () => {

  notificationsButton.style.display = "none";
  permissionsInformation.style.display = "none";
});

