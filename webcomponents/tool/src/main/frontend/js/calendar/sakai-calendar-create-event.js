import { SakaiElement } from "../sakai-element.js?version=76c54fa6";
import { LitElement, html, css } from "../assets/lit-element/lit-element.js?version=76c54fa6";
import "../sakai-editor.js?version=76c54fa6";

class SakaiCalendarCreateEvent extends SakaiElement {

  constructor() {

    super();

    this.idSalt = Math.floor(Math.random() * Math.floor(1000));

    this.i18n = {};
    this.loadTranslations("calendar").then(r => this.i18n = r);

    this.start = moment();
    this.start = moment(this.start).add(30 - (this.start.minute() % 30), "minutes");
    this.end = moment(this.start).add(1, "hour");
  }

  static get properties() {

    return {
    };
  }

  firstUpdated(changedProperties) {

    const self = this;

    flatpickr(`#start-${this.idSalt}`, { enableTime: true,
                                        time_24hr: true,
                                        allowInput: true,
                                        defaultHour: this.start.hours(),
                                        defaultMinute: this.start.minutes(),
                                        static: true,
                                        onReady() {
                                          this.showTimeInput = true;
                                          this.setDate(self.start.toDate());
                                        }
                                      });
    flatpickr(`#end-${this.idSalt}`, { enableTime: true,
                                        time_24hr: true,
                                        defaultHour: this.end.hours(),
                                        defaultMinute: this.end.minutes(),
                                        static: true,
                                        onReady() {
                                          this.showTimeInput = true;
                                          this.setDate(self.end.toDate());
                                        }
                                      });
  }

  _closeOverlay() {
    this.dispatchEvent(new Event('close-overlay', { bubbles: true }));
  }

  render() {

    return html`
      <h1>${this.i18n["java.new"]}</h1>

      <p class="sak-banner-info">
        ${this.i18n["new.a"]} <span class="reqStarInline"> * </span>
      </p>

      <div style="display: table">
        <div class="start-block" style="display: table-row">
          <div style="display: table-cell">
            <span class="reqStar">*</span>
            <label for="activitytitle">${this.i18n["new.title"]}</label>
          </div>
          <div style="display: table-cell"><input type="text" name="activitytitle" size="50" maxlength="150" /></div>
        </div>
        <div class="start-block" style="display: table-row">
          <div style="display: table-cell"><label for="start-${this.idSalt}">Start</label></div>
          <div style="display: table-cell"><input type="text" id="start-${this.idSalt}" size="30" placeholder="Enter the start date and time" /></div>
        </div>
        <div class="end-block" style="display: table-row">
          <div style="display: table-cell"><label for="end-${this.idSalt}">End</label></div>
          <div style="display: table-cell"><input type="text" id="end-${this.idSalt}" size="30" placeholder="Enter the end date and time" /></div>
        </div>
      </div>

		  <div class="itemSummary">
			  <label for="body" class="block">${this.i18n["new.descr"]}</label>
        <sakai-editor editor-id="editor-classic"></sakai-editor>
      </div>
      <button class="close-button" @click=${this._closeOverlay}>⨯</button>
    `;
  }
}

customElements.define("sakai-calendar-create-event", SakaiCalendarCreateEvent);
