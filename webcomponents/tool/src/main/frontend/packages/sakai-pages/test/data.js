export const i18nUrl = /.*getI18nProperties.*resourcebundle=pages/;

export const i18n = `
actions=Actions
add_page_content_label=Content:
add_page_header=Add Page
add_page_title_label=Title:
cancel=Cancel
pages_header=Pages
save=Save
title=Title
`;

export const siteId = "xyz";

export const pagesUrl= `/api/sites/${siteId}/pages`;

const page1 = { title: "Eggs" };

const pages = [ page1 ];

const links = [
  {
    rel: "addPage",
    href: pagesUrl,
  },
];

export const pagesDataForStudent = { pages, links: [] };
export const pagesDataForInstructor = { pages, links };
