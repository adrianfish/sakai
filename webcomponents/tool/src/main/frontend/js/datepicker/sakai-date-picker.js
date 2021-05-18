import { html, css } from "../assets/lit-element/lit-element.js";
import { ifDefined } from "../assets/lit-html/directives/if-defined.js";
import { SakaiCalendar } from '../calendar/sakai-calendar.js';
import { LionInputDatepicker } from "../assets/@lion/input-datepicker/src/LionInputDatepicker.js";
import { LionCalendarOverlayFrame } from "../assets/@lion/input-datepicker/src/LionCalendarOverlayFrame.js";
//import { loadProperties } from "../sakai-i18n.js";
//
console.log(SakaiCalendar);

/**
 * Renders an input which, when clicked, launches a Flatpickr instance.
 *
 * @example <caption>Usage:</caption>
 * <sakai-date-picker epoch-millis="345922925445" @/>
 * <sakai-date-picker hours-from-now="5" />
 *
 * The tag fires the event 'datetime-selected'. You'd handle that with (vanillajs):
 *
 * sakaiDatePicker.addEventListener("datetime-selected", (e) => console.log(e.detail.epochMillis));
 *
 * @extends LitElement
 * @author Adrian Fish <adrian.r.fish@gmail.com>
 */
class SakaiDatePicker extends LionInputDatepicker {

  static get scopedElements() {

    return {
      ...super.scopedElements,
      'sakai-calendar': SakaiCalendar,
      'lion-calendar-overlay-frame': LionCalendarOverlayFrame,
    };
  }

  constructor() {

    super();

    //loadProperties("date-picker-wc").then(r => this.i18n = r);
  }

  balls(e) {
    console.log(e);
  }

  _calendarTemplate() {

    console.log(this.__calendarMinDate);
    console.log(this.__calendarMaxDate);

    return html`
      <sakai-calendar
        slot="content"
        .selectedDate="${(this.constructor).__getSyncDownValue(this.modelValue,)}"
        .minDate="${this.__calendarMinDate}"
        .maxDate="${this.__calendarMaxDate}"
        .disableDates="${ifDefined(this.__calendarDisableDates)}"
        @user-selected-date-changed="${this._onCalendarUserSelectedChanged}"
      ></sakai-calendar>
    `;
  }
        //@user-selected-date-changed="${this._onCalendarUserSelectedChanged}"

  static get styles() {
    return [
      ...super.styles,
      css`
        #overlay-content-node-wrapper {
          border: 1px solid black;
        }
      `
    ];
  }
}

if (!customElements.get("sakai-date-picker")) {
  customElements.define("sakai-date-picker", SakaiDatePicker);
}
