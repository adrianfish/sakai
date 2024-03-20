export const getUserId = () => window.top?.portal?.user?.id || "";
export const getSiteId = () => window.top?.portal?.siteId || "";
export const getUserLocale = () => (window.top?.portal?.locale || window.top?.sakai?.locale?.userLocale || "en-US").replace("_", "-");
export const getOffsetFromServerMillis = () => window.top?.portal?.user.offsetFromServerMillis || 0;
export const getTimezone = () => {

  if (window.top?.portal?.user.timezone) {
    return window.top?.portal?.user.timezone;
  }

  const userJson = localStorage.getItem("sakai-user");
  if (userJson) return JSON.parse(userJson).timezone;
  return "";
};
export const getServiceName = () => window.top?.portal?.serviceName || "Sakai";
export const setupSearch = options => window.top?.portal?.search?.setup(options);
