export const getUserId = () => globalThis.portal?.user?.id || "";
export const getSiteId = () => globalThis.portal?.siteId || "";
export const getUserLocale = () => (globalThis.portal?.locale || globalThis.sakai?.locale?.userLocale || "en-US").replace("_", "-");
export const getOffsetFromServerMillis = () => globalThis.portal?.user.offsetFromServerMillis || 0;

export const getTimezone = () => {

  if (globalThis.portal?.user.timezone) {
    return globalThis.portal.user.timezone;
  }

  const userJson = localStorage.getItem("sakai-user");
  if (userJson) return JSON.parse(userJson).timezone;
  return "";
};

export const getServiceName = () => globalThis.portal?.serviceName || "Sakai";
export const setupSearch = options => globalThis.portal?.search?.setup(options);
