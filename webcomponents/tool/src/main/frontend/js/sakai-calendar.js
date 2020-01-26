import {SakaiElement} from "./sakai-element.js";
import {html} from "./assets/lit-element/lit-element.js";

class SakaiCalendar extends SakaiElement {

  static get properties() {

    return {
    };
  }

  constructor() {

    super();
    this.divId = `calendar-${Math.floor(Math.random() * Math.floor(1000))}`;
  }

  firstUpdated(changedProperties) {

    var el = this.querySelector(`#${this.divId}`);
    var calendar = new FullCalendar.Calendar(el, {
      plugins: ['interaction', 'dayGrid', 'timeGrid'],
      editable: true, // enable draggable events
      selectable: true,
      aspectRatio: 1.8,
      scrollTime: '00:00', // undo default 6am scrollTime
      header: {
        left: 'today prev,next',
        center: 'title',
        right: 'resourceTimelineDay,resourceTimelineThreeDays,timeGridWeek,dayGridMonth'
      },
      defaultView: 'resourceTimelineDay',
      views: {
        resourceTimelineThreeDays: {
          type: 'resourceTimeline',
          duration: { days: 3 },
          buttonText: '3 days'
        }
      },
      defaultView: 'dayGridMonth',
      displayEventTime: false,
      header: {
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth,timeGridWeek,timeGridDay'
      },
      eventSources: [{
        events: function (event, successCallback, failureCallback) {
          var start_date = moment(event.start).format('YYYY-MM-DD');
          var end_date = moment(event.end).format('YYYY-MM-DD');
          fetch(`/direct/calendar/site/${portal.siteId}.json`, {cache: "no-cache", credentials: "same-origin"})
            .then(res => res.json())
            .then(data => {

              var events = [];
              data['calendar_collection'].forEach(e => {
                var startTime = e['firstTime']['time'];
                var startDate = new Date(startTime);
                var endTime = e['firstTime']['time'] + e['duration'] ;
                var endDate = new Date();
                endDate.setTime(endTime);
                events.push({
                  title: e['title'],
                  description: e['description'],
                  descriptionFormatted: e['descriptionFormatted'],
                  start: startDate,
                  site_name: e['siteName'],
                  type: e['type'],
                  //icon: e['eventIcon'],
                  icon:"icon-calendar-exam",
                  event_id: e['eventId'],
                  attachments: e['attachments'] || [],
                  eventReference: e['eventReference'],
                  end: endDate
                });
              });
              successCallback(events);
            });
        },
        color: '#D4DFEE',
        textColor: '#0064cd'
      }],
      eventSourceSuccess: function(content, xhr) {
        return content.eventArray;
      },
      eventClick: (eventClick) => {

        //to adjust startdate and enddate as per user's locale
        var startDate = new Date().toLocaleString();
        var endDate = new Date(eventClick.event.end).toLocaleString();
        this.querySelector("#startTime").innerText = moment(eventClick.event.start).format('LLLL');
        this.querySelector("#endTime").innerText = moment(eventClick.event.end).format('LLLL');
        this.querySelector("#event-type-icon").setAttribute("class","icon " + eventClick.event.extendedProps.icon);
        this.querySelector("#event-type").innerText = eventClick.event.extendedProps.type;
        this.querySelector("#event-description").innerHTML = eventClick.event.extendedProps.descriptionFormatted;
        this.querySelector("#site-name").innerText = eventClick.event.extendedProps.site_name;
        //if event has attachment show attachment info div , else hide it
        if (eventClick.event.extendedProps.attachments.length >= 1) {
          var attachments = '<br><ul class="eventAttachmentList">';
          eventClick.event.extendedProps.attachments.forEach(a => {

            var filename = a.url.split('/');
            filename = filename[filename.length-1];
            attachments += `
              <li class="eventAttachmentItem">
                <a href="${a.url}" target="_blank">
                  <span class="icon icon-calendar-attachment"></span>
                  <span class="eventAttachmentFilename">${filename}</span>
                </a>
              </li>`;
          });
          attachments += '</ul>';
          this.querySelector("#event-attachments").innerHTML = attachments;
          this.querySelector("#eventAttachmentsDiv").style.display = "block";
        } else {
          //$('#eventAttachmentsDiv').hide();
        }
        //var more_info = moreInfoUrl + eventClick.event.extendedProps.eventReference + '&panel=Main&sakai_action=doDescription&sakai.state.reset=true';
        //var fullDetailsText = msg('simplepage.calendar-more-info');
        //when Full Details is clicked, event in the Calendar tool is shown.
        //$('#fullDetails').html('<a href=' + more_info + ' target=_top>' + fullDetailsText + '</a>');
        //On event click dialog is opened near the event
        $('#calendarEventDialog').dialog({ modal: true, title: eventClick.event.title, width: 400 });
      }
    });
    calendar.render();
  }

  render() {

    return html`
      <div id="${this.divId}"></điv>
      <div id="calendarEventDialog" style="display:none;">
        <div id="eventInfo">
          <label for="startTime">Start time</label>
          <span id="startTime"></span><br/><br/>
          <label for="endTime">End time</label>
          <span id="endTime"></span><br/><br/>
          <label for="event-type">Type</label>
          <span id="event-type-icon"></span><span id="event-type"></span><br/><br/>
          <label for="event-description">Description</label>
          <span id="event-description"></span><br/><br/>
          <label for="site-name">Site</label>
          <span id="site-name"></span><br/>
          <span id="eventAttachmentsDiv">
            <label for="event-attachments">Attachments</label>
            <span id="event-attachments"></span><br/>
          </span>
          <br/>
        </div>
      </div>
    `;
  }
}

customElements.define("sakai-calendar", SakaiCalendar);
