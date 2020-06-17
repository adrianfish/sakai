import { html } from 'lit-html';
import { withKnobs, number } from "@storybook/addon-knobs";

import '../js/sakai-multicolumn-layout.js';

export default {
  title: 'Sakai Multicolumn Layout',
  decorators: [withKnobs]
};

const columnsAndRows = { rows: 2, cols: 2 };

const columns = [
  {
    rows: [
      { content: "<div>TASKS</div>" },
      { content: "<div>FORUMS</div>" },
    ],
  },
  {
    rows: [
      { content: "<div>ASSIGNMENTS</div>" }
    ],
  }
];

export const BasicDisplay = () => html`

  <sakai-multicolumn-layout columns=${JSON.stringify(columns)}>
`;
