import {html} from "/webcomponents/assets/lit-html/lit-html.js";
import {unsafeHTML} from "/webcomponents/assets/lit-html/directives/unsafe-html.js";

class BBBTemplates {
}
  
BBBTemplates.toolbar = function (i18n) {

  return html`
    <ul id="bbb_toolbar_items" class="navIntraTool actionToolBar" role="menu">
      <li class="firstToolBarItem" role="menuitem">
        <span><a id="bbb_home_link" title="${i18n["bbb_home_tooltip"]}" href="javascript:;">${i18n["bbb_home_label"]}</a></span>
      </li>
      <li role="menuitem">
        <span><a id="bbb_recordings_link" title="${i18n["bbb_recordings_tooltip"]}" href="javascript:;">${i18n["bbb_recordings_label"]}</a></span>
      </li>
      <li role="menuitem">
        <span><a id="bbb_permissions_link" title="${i18n["bbb_permissions_tooltip"]}" href="javascript:;">${i18n["bbb_permissions_label"]}</a></span>
      </li>
    </ul>
  `;
};

BBBTemplates.rooms = function (i18n, meetings) {

  return html`
    <input type="button" id="bbb_create_meeting_link" title="${i18n["bbb_create_meeting_tooltip"]}" value="${i18n["bbb_create_meeting_label"]}" />
    <input type="text" class="search" placeholder="${i18n["bbb_search"]}">
    <table id="bbb_meeting_table" class="listHier">
      <thead>
        <tr>
          <th class="bbb_name">${i18n["bbb_th_meetingname"]}</th>
          <th class="bbb_status">${i18n["bbb_th_status"]}</th>
          <th class="bbb_startDate">${i18n["bbb_th_startdate"]}</th>
          <th class="bbb_endDate">${i18n["bbb_th_enddate"]}</th>
          <th class="owner">${i18n["bbb_th_owner"]}</th>
        </tr>
      </thead>
      <tbody>
      ${meetings.map(m => html`
        ${!m.deleted ? html`
          <tr class="meetingRow">
            <td>
              ${m.joinable ? html`
                <a href="javascript:;" class="bbb-meeting-info-link" data-meeting-id="${m.id}" title="${i18n["bbb_meeting_details_tooltip"]}">${m.name}</a>
              ` : html`<span>${m.name}</span>`}
              <div class="itemAction" style="margin:0; padding:0;">
                <small>
                  ${m.canEdit ? html`
                  <div id="edit_meeting_${m.id}"  style="display: inline; margin:0; padding:0;">
                    <a href="javascript:;" class="bbb-edit-meeting-link" data-meeting-id="${m.id}" title="${i18n["bbb_action_edit_meeting_tooltip"]}">${i18n["bbb_action_edit_meeting"]}</a>
                  </div>
                  ` : ""}
                  ${m.canDelete ? html`
                  <div id="delete_meeting_${m.id}" style="display: inline; margin:0; padding:0;">
                    &nbsp;|&nbsp;
                    <a href="javascript:;" class="bbb-delete-meeting-link" data-name="${m.name}" data-meeting-id="${m.id}" title="${i18n["bbb_action_delete_meeting_tooltip"]}">${i18n["bbb_action_delete_meeting"]}</a>
                  </div>
                  ` : ""}
                  ${m.canEnd && m.joinableMode === "inprogress" ? html`
                  <div id="end_meeting_${m.id}" style="display: inline; margin:0; padding:0;">
                    &nbsp;|&nbsp;
                    <a href="javascript:;" class="bbb-end-meeting-link" onclick="return meetings.utils.endMeeting('${escape(m.name)}','${m.id}');" title="${i18n["bbb_action_end_meeting_tooltip"]}">${i18n["bbb_action_end_meeting"]}</a>
                  </div>
                  ` : ""}
                </small>
              </div>
            </td>
            <td id="meeting_status_${m.id}" class="${m.statusClass}">${i18n[m.statusText]}</td>
            <td>${m.formattedStartDate}</td>
            <td>${m.formattedEndDate}</td>
            <td>${m.ownerDisplayName}</td>
          </tr>
        ` : ""}
      `)}
        <tr>
          <td></td>
          <td></td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
      </tbody>
    </table>
  `;
};

BBBTemplates.addUpdateMeeting = function (i18n, context) {

  return html`
    <form id="bbb_add_update_form" action="${context.actionUrl}" method="post" enctype="multipart/form-data">
      <input id="openDate" type="hidden" value=""/>
      <input id="closeDate" type="hidden" value=""/>
      <h3>${i18n["bbb_info"]}</h3>
      <p class="instruction">${i18n["bbb_info_instr"]}</p>
      <table cellpadding="0" cellspacing="0" border="0" class="bbb_form_container">
        <tr>
          <td class="bbb_lbl">${i18n["bbb_info_title"]}</td>
          <td>
            <input id="bbb_meeting_name_field" name="name" type="text" .value=${context.meeting.name} style="width: 400px;" />
          </td>
        </tr>
        <tr>
          <td class="bbb_lbl">${i18n["bbb_info_description"]}</td>
          <td>
            <textarea
              class="bbb_welcome_message_textarea"
              id="bbb_welcome_message_textarea"
              name="properties.welcomeMessage"
              cols="36"
              rows="10"
              style="width: 480px;"
              wrap="virtual">
              ${context.isNew ? html`${i18n["bbb_default_welcome_description"]}` : html`${context.meeting.properties.welcomeMessage}`}
            </textarea>
          </td>
        </tr>
        ${context.recordingEnabled ? html`
            ${context.recordingEditable ? html`
            <tr>
              <td class="bbb_lbl">${i18n["bbb_info_recording"]}</td>
              <td>
                  <input id="recording" name="recording" type="checkbox" .checked=${context.recordingChecked}/>
              </td>
            </tr>
            ` : html`
            <input id="recording" name="recording" type="hidden" .value=${context.recordingChecked}/>
            `}
        ` : ""}
        ${context.durationEnabled ? html`
        <tr>
          <td class="bbb_lbl">${i18n["bbb_info_duration"]}</td>
          <td>
            <input id="recordingDuration" name="recordingDuration" type="text" .value="${context.meeting.recordingDuration}" style="width: 35px;" />&nbsp;${i18n["bbb_info_recording_duration_units"]}
          </td>
        </tr>
        ` : ""}
        ${context.waitmoderatorEnabled ? html`
            ${context.waitmoderatorEditable ? html`
            <tr>
              <td class="bbb_lbl">${i18n["bbb_info_waitformoderator"]}</td>
              <td>
                <input id="waitForModerator" name="waitForModerator" type="checkbox" .checked=${context.waitForModerator}/>
              </td>
            </tr>
            ` : html`
            <input id="waitForModerator" name="waitForModerator" type="hidden" ${context.waitmoderatorEditable? html`value="${context.meeting.waitForModerator}"` : ""}/>
            `}
        ` : ""}
        ${context.meeting.multiplesessionsallowedEnabled ? html`
            ${context.meeting.multiplesessionsallowedEditable ? html`
            <tr>
              <td class="bbb_lbl">${i18n["bbb_info_multiplesessionsallowed"]}</td>
              <td>
                <input id="multipleSessionsAllowed" name="multipleSessionsAllowed" type="checkbox" .checked=${context.multipleSessionsAllowed}/>
              </td>
            </tr>
            ` : html`
            <input id="multipleSessionsAllowed" name="mulitpleSessionsAllowed" type="hidden" .value=${context.multipleSessionsAllowed}/>
            `}
        ` : ""}
        ${context.preuploadpresentationEnabled ? html`
        <tr>
          <td class="bbb_lbl">${i18n["bbb_info_preuploadpresentation"]}</td>
          <td>
              <input type="file" id="selectFile" name="selectFile" style="display:inline;"/>
              <span id="bbb_addFile_ajaxInd"></span>
              <input type="hidden" id="fileUrl" name="presentation" />
              <table id="fileView" class="attachList listHier indnt1" style="margin-bottom:.5em; margin-top:0; width:auto; display:none;">
                  <tr>
                      <td><a href="#" id="url" target="_blank"/></td>
                      <td><a href="javascript:;" id="removeUpload">${i18n["bbb_info_remove"]}</a></td>
                  </tr>
              </table>
          </td>
        </tr>
        ` : ""}
        ${context.groupsessionsEnabled ? html`
            ${context.groupsessionsEditable ? html`
            <tr>
              <td class="bbb_lbl">${i18n["bbb_info_groupsessions"]}</td>
              <td>
                <input id="groupSessions" name="groupSessions" type="checkbox" .checked=${context.groupSessions}/>
              </td>
            </tr>
            ` : html`
            <input id="groupSessions" name="groupSessions" type="hidden" ${context.groupSessions ? html`value="${context.groupSessions}"` : ""}/>
            `}
        ` : ""}
      </table>
      <br/>
      <h3>${i18n["bbb_participants"]}</h3>
      <p class="instruction">${i18n["bbb_participants_instr"]}</p>
      <table cellpadding="0" cellspacing="0" border="0" class="bbb_form_container">
        <tr>
          <td class="bbb_lbl">${i18n["bbb_participants_add"]}</td>
          <td>
            <select id="selType" onchange="meetings.updateParticipantSelectionUI()">
              <option value="${context.selTypes.all.id}" selected="selected">${context.selTypes.all.title}</option>
              <option value="${context.selTypes.user.id}">${context.selTypes.user.title}</option>
              ${context.selOptions['groups'].length > 0 ? html`
              <option value="${context.selTypes.group.id}">${context.selTypes.group.title}</option>
              ` : ""}
              <option value="${context.selTypes.role.id}">${context.selTypes.role.title}</option>
            </select>
            <select id="selOption" disabled="disabled"></select>
            <input id="bbb_add" type="button" value="${i18n["bbb_add"]}"
                onclick="meetings.addParticipantRow(jQuery('#selType').val(), jQuery('#selOption').val(), jQuery('#selOption option:selected').text());return false;"/>
          </td>
        </tr>
        <tr>
          <td class="bbb_lbl" style="padding-top: 6px">${i18n["bbb_participants_list"]}</td>
          <td>
            <table id="selContainer">
              <tbody></tbody>
            </table>
          </td>
        </tr>
      </table>
      <br/>
      <h3>${i18n["bbb_availability"]}</h3>
      <p class="instruction">${i18n["bbb_availability_instr"]}</p>
      <table cellpadding="0" cellspacing="0" border="0" class="bbb_form_container">
        <tr>
          <td class="bbb_lbl">${i18n["bbb_availability_startdate"]}</td>
          <td>
            <input id="startDate1" type="checkbox" .checked=${context.showStartDate} />
            <span id="startDateBox" style="${!context.showStartDate ? "display:none;" : ""}">
              <input id="startDate2" type="text" class="date-picker-field"/>
              <input id="startTime" type="text" class="time-picker-field"/>
              <input id="startDate" name="startDate" type="hidden"/>
              ${context.thing ? html`
                <input id="addToCalendar" type="checkbox" name="addToCalendar" .checked=${context.addToCalendar}/>
                <span>${i18n["bbb_availability_addtocal"]}</span>
              ` : ""}
            </span>
          </td>
        </tr>
        <tr>
          <td class="bbb_lbl">${i18n["bbb_availability_enddate"]}</td>
          <td>
            <input id="endDate1" type="checkbox" .checked=${context.showEndDate} />
            <span id="endDateBox" style="${!content.showEndDate ? "display:none;" : ""}">
              <input id="endDate2" type="text" class="date-picker-field"/>
              <input id="endTime" type="text" class="time-picker-field"/>
              <input id="endDate" name="endDate" type="hidden"/>
            </span>
          </td>
        </tr>
      </table>

      <br/>
      <h3>${i18n["bbb_notification"]}</h3>
      <p class="instruction">${context.isNew ? html`${i18n["bbb_notification_instr"]}` : html`${i18n["bbb_notification_instr_edit"]}`}</p>
      <table cellpadding="0" cellspacing="0" border="0" class="bbb_form_container">
        <tr>
          <td class="bbb_lbl">${context.isNew? html`${i18n["bbb_notification_notify"]}` : html`${i18n["bbb_notification_notify_edit"]}`}</td>
          <td>
            <input id="notifyParticipants" name="notifyParticipants" type="checkbox" onclick="meetings.utils.setNotifictionOptions();"/>
            <span id="notifyParticipants_iCalAttach_span"></span>
          </td>
        </tr>
      </table>

      <input id="isNew" type="hidden" value="${context.isNew}"/>
      <input id="siteId" name="siteId" type="hidden" value="${context.siteId}"/>
      ${!context.isNew ? html`<input id="meetingId" type="hidden" value="${context.meeting.id}"/>` : ""}
      <div class="act">
        <input id="bbb_save" type="button" class="active" value="${i18n["bbb_save"]}"/>
        <input id="bbb_cancel" type="button" value="${i18n["bbb_cancel"]}"/>
        <span id="bbb_addUpdate_ajaxInd"></span>
      </div>
    </form>
  `;
};

BBBTemplates.meetingInfo = function (i18n, context) {

  return html`
    <h3>${i18n["bbb_meetinginfo_title"]}</h3>
    <br/>
    <table cellpadding="0" cellspacing="0" border="0" id="bbb_meeting_info_table">
      ${context.groups && context.meeting.groupSessions ? html`
      <tr>
        <td><h5>${i18n["bbb_meetinginfo_group"]}</h5></td>
        <td>
          <select id="groupSession">
            <option selected value="Default">${i18n["bbb_meetinginfo_defaultGroup"]}</option>
          ${Object.keys(context.groups).map(k => html`
            <option value="${context.groups[k].groupId}">${context.groups[k].groupTitle}</option>
          `)}
          </select>
        </td>
      </tr>
      ` : ""}
      <tr>
        <td><h5>${i18n["bbb_info_title"]}</h5></td>
        <td id="meetingName">${context.meeting.name}</td>
      </tr>
      <tr>
        <td><h5>${i18n["bbb_info_description"]}</h5></td>
        <td>${unsafeHTML(context.meeting.properties.welcomeMessage)}</td>
      </tr>
      ${context.meeting.startDate ? html`
      <tr>
        <td><h5>${i18n["bbb_availability_startdate"]}</h5></td>
        <td>${new Date(parseInt(context.meeting.startDate) - parseInt(context.browserTimezoneOffset) + parseInt(context.timezoneoffset)).toISO8601String()}</td>
      </tr>
      ` : ""}
      ${context.meeting.endDate ? html`
      <tr>
        <td><h5>${i18n["bbb_availability_enddate"]}</h5></td>
        <td>${new Date(parseInt(context.meeting.endDate) - parseInt(context.browserTimezoneOffset) + parseInt(context.timezoneoffset)).toISO8601String()}</td>
      </tr>
      ` : ""}
      <tr>
        <td><h5>${i18n["bbb_meetinginfo_status"]}</h5></td>
        <td>
            <span id="meeting_status_notstarted_${context.meeting.id}" class="bbb_status_notstarted" style="${context.dispNotStarted}">
                ${i18n["bbb_status_notstarted"]}
            </span>
            <span id="meeting_status_joinable_${context.meeting.id}" class="status_joinable_inprogress" style="${context.dispInProgress}">
                ${context.labelJoinable}
            </span>
            <span id="meeting_status_finished_${context.meeting.id}" class="bbb_status_finished" style="${context.dispFinished}">
                ${i18n["bbb_status_finished"]}
            </span>
            <span id="meetingStatus">
                (
                <span id="meeting_joinlink_${context.meeting.id}" style="${context.displayLink}">
                  ${context.canJoin ? html`
                  <a id="joinMeetingLink" target="_blank" href="javascript:;" onclick="return meetings.utils.joinMeeting('${context.meeting.id}', '#joinMeetingLink', ${context.multiplesessions});" title="${i18n["bbb_meetinginfo_launch_meeting_tooltip"]}" style="font-weight:bold">${i18n["bbb_meetinginfo_link"]}</a>
                  <i class="fa fa-sign-in"></i>
                  ` : ""}
                </span>
                ${context.meeting.canEnd ? html`
                <div id="end_meeting_intermediate_${context.meeting.id}" class="${context.end_meetingClass}" >
                    &nbsp;|&nbsp;
                    <a id="end_session_link" href="javascript:;" onclick="return meetings.utils.endMeeting('${escape(context.meeting.name)}','${context.meeting.id}');" title="${i18n["bbb_action_end_meeting_tooltip"]}" style="font-weight:bold">${i18n["bbb_action_end_meeting"]}</a>
                    <span><i class="fa fa-stop"></i></span>
                </div>
                ` : ""}
                )
            <span>
        </td>
      </tr>
      <tr id="bbb_meeting_info_participants_count_tr" style="display:none">
        <td><h5>${i18n["bbb_meetinginfo_participants_count"]}</h5></td>
        <td>
            <span id="bbb_meeting_info_participants_count"></span>
            <a id="updateMeetingInfo" href="javascript:;" onclick="meetings.utils.checkOneMeetingAvailability('${context.meeting.id}'); return false;" title="${i18n["bbb_meetinginfo_updateinfo_tooltip"]}">
                <i class="fa fa-refresh" title="${i18n["bbb_refresh"]}"></i>
            </a>
        </td>
      </tr>
      <tr id="meeting_recordings" style="${context.hideRecordings ? "display:none;" : ""}">
        <td><h5>${i18n["bbb_info_recordings"]}</h5></td>
        <td id="recording_link_${context.meeting.id}">
        </td>
      </tr>
    </table>
  `;
};

export {BBBTemplates};
