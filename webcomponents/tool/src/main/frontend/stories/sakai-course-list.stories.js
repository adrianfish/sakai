import { html } from 'lit-html';
import fetchMock from "fetch-mock";
import { withA11y } from "@storybook/addon-a11y";
import { withCssResources } from '@storybook/addon-cssresources';

import '../js/sakai-course-list.js';

// Mockup the i18n strings
const coursecardI18n = `
options_menu_tooltip=Click to see options for this course
select_tools_to_display=Select tools to display:
favourite_this_course=Favourite this course?
assignments_tooltip=Click to view your assignments for this course
gradebook_tooltip=Click to view the gradebook for this course
forums_tooltip=Click to view the forums for this course
visit=Visit
`;

const courselistI18n = `
view_all_sites=View All Sites
favourites=Favourites
all_projects=All Projects
all_courses=All Courses
new_activity=New Activity
title_a_to_z=Title: A-Z
title_z_to_a=Title: Z-A
code_a_to_z=Code: A-Z
code_z_to_a=Code: Z-A
`;

// Mockup the toolname mapping properties
const toolnameMappings = `
assignments=Assignments
gradebook=Gradebook
forums=Forums
`;

export default {
  title: 'Sakai Course List',
  decorators: [withCssResources, withA11y, (storyFn) => {
    parent.portal = {locale: "en-GB"};
    const baseUrl = "/sakai-ws/rest/i18n/getI18nProperties?locale=en-GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=";
    const coursecardI18nUrl = `${baseUrl}coursecard`;
    const courselistI18nUrl = `${baseUrl}courselist`;
    fetchMock
      .get(coursecardI18nUrl, coursecardI18n, {overwriteRoutes: true})
      .get(courselistI18nUrl, courselistI18n, {overwriteRoutes: true})
      .get(/addfavourite/, 200, {overwriteRoutes: true})
      .get(/removefavourite/, 200, {overwriteRoutes: true})
      .get("*", 500, {overwriteRoutes: true});
    return storyFn();
  }],
  parameters: {
    cssresources: [
      {
        id: `default`,
        code: `
          <style>
            body {
              --sakai-course-card-width: 403px;
              --sakai-course-card-bg-color: white;
              --sakai-course-card-font-family: roboto, arial;
              --sakai-course-card-info-height: 90px;
              --sakai-course-card-border-width: 0;
              --sakai-course-card-border-color: black;
              --sakai-course-card-border-radius: 4px;
              --sakai-course-card-padding: 20px;
              --sakai-course-card-info-block-bg-color: #0f4b6f;
              --sakai-icon-favourite-color: yellow;
              --sakai-course-card-title-color: white;
              --sakai-course-card-title-font-size: 16px;
              --sakai-course-card-code-color: white;
              --sakai-course-card-code-font-size: 12px;
              --sakai-course-card-tool-alerts-height: 40px;
              --sakai-course-card-tool-alerts-padding:5px;
              --sakai-course-card-border-width: 0;
              --sakai-course-card-border-color: black;
              --sakai-course-card-border-radius: 4px;
              --sakai-course-card-tool-alerts-color: black;
              --sakai-course-card-tool-alert-icon-color: rgb(15,75,111);
              --sakai-options-menu-invoker-color: white;
              --sakai-course-card-options-menu-favourites-block-color: black;
              --sakai-course-card-options-menu-favourites-block-font-size: inherit;
              --sakai-course-card-options-menu-favourites-block-font-weight: bold;
              --sakai-course-card-options-menu-tools-title-font-weight: bold;
            }
          </style>`,
        picked: false,
      },
      {
        id: `anothertheme`,
        code: `
          <style>
            body {
              --sakai-course-card-bg-color: lightgrey;
              --sakai-course-card-font-family: roboto, arial;
              --sakai-course-card-info-height: 90px;
              --sakai-course-card-border-width: 0;
              --sakai-course-card-border-color: black;
              --sakai-course-card-border-radius: 4px;
              --sakai-course-card-padding: 20px;
              --sakai-course-card-info-block-bg-color: #0f4b6f;
              --sakai-icon-favourite-color: blue;
              --sakai-course-card-title-color: orange;
              --sakai-course-card-title-font-size: 18px;
              /* Add more css vars here ! */
            }
          </style>`,
        picked: false,
      },
    ],
  },
};

export const WithData = () => {

  const courseData = [{
    id: "bio",
    title: "Biogeochemical Oceanography",
    code: "BCO 104",
    url: "http://www.facebook.com",
    alerts: ["forums"],
    favourite: false,
    course: true,
  },
  {
    id: "fre",
    title: "French 101",
    code: "LING",
    url: "http://www.ebay.co.uk",
    alerts: ["assignments", "forums"],
    favourite: true,
    course: true,
  },
  {
    id: "footsoc",
    title: "Football Society",
    code: "FOOTSOC",
    url: "http://www.open.ac.uk",
    favourite: false,
    project: true,
  }];

  return html`
    <sakai-course-list course-data="${JSON.stringify(courseData)}">
  `;
};
