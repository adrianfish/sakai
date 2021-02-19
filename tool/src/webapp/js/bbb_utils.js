class MeetingUtils {

  constructor(i18n) {

    this.bbbUserSelectionOptions = null;
    this.getOptions = {
      credentials: "same-origin",
      cache: "no-store",
    };
    this.i18n = i18n;
  }

  getSettings(siteId) {
    return fetch(`/direct/bbb-tool/getSettings.json?siteId=${siteId}`, this.getOptions).then(r => r.json());
  }

  getMeeting(meetingId) {
    return fetch(`/direct/bbb-tool/${meetingId}.json`, this.getOptions).then(r => r.json());
  }

  async getMeetingList(siteId) {

    return fetch(`/direct/bbb-tool.json?siteId=${siteId}`, this.getOptions)
      .then(r => r.json()).then(m => { 
        return m['bbb-tool_collection'] ? m['bbb-tool_collection'] : [];
      });
  }

  doUpload(files, siteId) {

    const fd = new FormData();
    fd.append('file', files.files[0]);

    $('#bbb_save,#bbb_cancel').prop('disabled', true);
    this.showAjaxIndicator('#bbb_addFile_ajaxInd');

    return fetch(`/direct/bbb-tool/doUpload?siteId=${siteId}`, {
      credentials: "same-origin",
      method: "POST",
      body: fd,
    })
    .then(r => r.text());
  }

  removeUpload(url, meetingId) {

    const meetingID = (typeof meetingId != undefined) ? `&meetingId=${meetingId}` : "";

    $('#bbb_save,#bbb_cancel').prop('disabled', true);
    this.showAjaxIndicator('#bbb_addFile_ajaxInd');

    fetch(`/direct/bbb-tool/removeUpload?url=${url}${meetingID}`)
      .then(r => {
        $('#bbb_save,#bbb_cancel').attr('disabled', false);
        this.hideAjaxIndicator('#bbb_addFile_ajaxInd');
        $("#fileUrl").val('');
        $("#selectFile").val('');
        $("#selectFile").prop("disabled", false);
        $("#fileView").hide();
      })
      .catch(error => {
        log.error("Failed to remove upload");
        $('#bbb_save,#bbb_cancel').attr('disabled', false);
        this.hideAjaxIndicator('#bbb_addFile_ajaxInd');
      });
  }

  addUpdateMeeting() {

    // Consolidate date + time fields.
    var today = new Date();
    var startMillis = 0, endMillis = 0;
    if ($('#startDate1').prop('checked')) {
      var date = $('#startDate2').datepick('getDate');
      var time = $('#startTime').val().split(':');
      startMillis = date.getTime();
      startMillis += time[0] * 60 * 60 * 1000;
      startMillis += time[1] * 60 * 1000;
      startMillis -= date.getTimezoneOffset() * 60 * 1000;
      startMillis += (parseInt(meetings.startupArgs.timezoneoffset) * -1);
      date.setTime(startMillis);

      $('#startDate').val(startMillis);
    } else {
      $('#startDate').removeAttr('name');
      $('#startDate').val(null);
      $('#addToCalendar').removeAttr('checked');
    }
    if ($('#endDate1').attr('checked')) {
      var date = $('#endDate2').datepick('getDate');
      var time = $('#endTime').val().split(':');
      endMillis = date.getTime();
      endMillis += time[0] * 60 * 60 * 1000;
      endMillis += time[1] * 60 * 1000;
      endMillis -= date.getTimezoneOffset() * 60 * 1000;
      endMillis += (parseInt(meetings.startupArgs.timezoneoffset) * -1);
      date.setTime(endMillis);

      $('#endDate').val(endMillis);
    } else {
      $('#endDate').removeAttr('name');
      $('#endDate').val(null);
    }

    // Validation.
    this.hideMessage();
    var errors = false;

    // Validate title field.
    var meetingTitle = $('#bbb_meeting_name_field').val().replace(/^\s+/, '').replace(/\s+$/, '');
    if (meetingTitle == '') {
      this.showMessage(bbb_err_no_title, 'warning');
      errors = true;
    }

    // Validate participants list.
    if ($('#selContainer tbody tr').length == 0) {
      this.showMessage(bbb_err_no_participants, 'warning');
      errors = true;
    }

    // Validate date fields.
    if ($('#startDate1').prop('checked') && $('#endDate1').prop('checked')) {
      if (endMillis == startMillis) {
        this.showMessage(bbb_err_startdate_equals_enddate, 'warning');
        errors = true;
      } else if (endMillis < startMillis) {
        this.showMessage(bbb_err_startdate_after_enddate, 'warning');
        errors = true;
      }
    }

    // Get description/welcome msg from CKEditor.
    this.updateFromInlineCKEditor('bbb_welcome_message_textarea');

    // Validate description length.
    var maxLength = meetings.settings.config.addUpdateFormParameters.descriptionMaxLength;
    var descriptionLength = $('#bbb_welcome_message_textarea').val().length;
    if (descriptionLength > maxLength) {
      meetings.utils.showMessage(bbb_err_meeting_description_too_long(maxLength, descriptionLength), 'warning');
      meetings.utils.makeInlineCKEditor('bbb_welcome_message_textarea', 'BBB', '480', '200');
      errors = true;
    }

    if (errors) return false;

    $('.bbb_site_member,.bbb_site_member_role').removeAttr('disabled');

    // Submit.
    var isNew = $("#isNew").val() == true || $("#isNew").val() == 'true';
    var actionUrl = $("#bbb_add_update_form").attr('action');
    var meetingId = $("#meetingId").val();
    this.hideMessage();
    $('#bbb_save,#bbb_cancel').prop('disabled', true);
    this.showAjaxIndicator('#bbb_addUpdate_ajaxInd');

    const fd = new FormData(document.getElementById("bbb_add_update_form"));

    return fetch(actionUrl, {
      credentials: "same-origin",
      method: "POST",
      body: fd,
    })
      .then(r => r.text())
      .then(returnedMeetingId => {
        return this.getMeeting(returnedMeetingId ? returnedMeetingId : meetingId);
      });
  }

  setMeetingInfo(meeting, groupId) {

    const groupID = groupId ? "?groupId=" + groupId : "";
    fetch(`/direct/bbb-tool/${meeting.id}/getMeetingInfo.json${groupID}`, this.getOptions)
      .then(r => r.json())
      .then(meetingInfo => {

        this.setMeetingInfoParams(meeting, meetingInfo);
        this.setMeetingJoinableModeParams(meeting);
      });
  }

  async getGroups(meeting) {
    return fetch(`/direct/bbb-tool/getGroups.json?meetingID=${meeting.id}`, this.getOptions).then(r => r.json());
  }

  setMeetingsParams(meetingsInfo) {

    BBBMeetings = meetingsInfo.meetings ? meetingsInfo.meetings : [];

    for (var i = 0; i < meetings.currentMeetings.length; i++) {
        //Clear attendees
        if (meetings.currentMeetings[i].attendees && meetings.currentMeetings[i].attendees.length > 0)
            delete meetings.currentMeetings[i].attendees;
        if (meetings.currentMeetings[i].running)
            delete meetings.currentMeetings[i].running;
        meetings.currentMeetings[i].attendees = new Array();
        meetings.currentMeetings[i].hasBeenForciblyEnded = "false";
        meetings.currentMeetings[i].participantCount = 0;
        meetings.currentMeetings[i].moderatorCount = 0;
        meetings.currentMeetings[i].unreachableServer = "false";

        // Extend the meetings that are present in the BBBMeetings array.
        for (var j = 0; j < BBBMeetings.length; j++) {
            if (BBBMeetings[j].meetingID === meetings.currentMeetings[i].id) {
                meetings.currentMeetings[i].hasBeenForciblyEnded = BBBMeetings[j].hasBeenForciblyEnded;
                meetings.currentMeetings[i].participantCount = BBBMeetings[j].participantCount;
                meetings.currentMeetings[i].running = BBBMeetings[j].running;
            }
            // Check if group session is active.
            else if ((BBBMeetings[j].meetingID).indexOf(meetings.currentMeetings[i].id) != -1 && BBBMeetings[j].participantCount != "0") {
                meetings.currentMeetings[i].groupActive = true;
            }
        }

        if (meetingsInfo.returncode != 'SUCCESS')
            meetings.currentMeetings[i].unreachableServer = "true";

        // If joinable set the joinableMode.
        meetings.currentMeetings[i].joinableMode = "nojoinable";
        if (meetings.currentMeetings[i].joinable) {
            if (meetings.currentMeetings[i].unreachableServer == null) {
                meetings.currentMeetings[i].joinableMode = "";
            } else if (meetings.currentMeetings[i].unreachableServer == "false") {
                meetings.currentMeetings[i].joinableMode = "available";
                if (meetings.currentMeetings[i].hasBeenForciblyEnded == "true") {
                    meetings.currentMeetings[i].joinableMode = "unavailable";
                } else if (meetings.currentMeetings[i].running) {
                    meetings.currentMeetings[i].joinableMode = "inprogress";
                }
            } else {
                meetings.currentMeetings[i].joinableMode = "unreachable";
            }
        }

        // Update status in the view.
        var statusClass = meetings.currentMeetings[i].joinable ? 'status_joinable_' + meetings.currentMeetings[i].joinableMode : (meetings.currentMeetings[i].notStarted ? 'status_notstarted' : 'status_finished')
        var statusText = meetings.currentMeetings[i].joinable ? (meetings.currentMeetings[i].joinableMode == 'available' ? bbb_status_joinable_available : meetings.currentMeetings[i].joinableMode == 'inprogress' ? bbb_status_joinable_inprogress : meetings.currentMeetings[i].joinableMode == 'unavailable' ? bbb_status_joinable_unavailable : meetings.currentMeetings[i].joinableMode == 'unreachable' ? bbb_status_joinable_unreachable : '') : (meetings.currentMeetings[i].notStarted ? bbb_status_notstarted : bbb_status_finished);
        // If status is 'available', but a group is active, set status to 'inprogress'.
        if (statusText === bbb_status_joinable_available && meetings.currentMeetings[i].groupActive)
            statusText = bbb_status_joinable_inprogress;
        $('#meeting_status_' + meetings.currentMeetings[i].id).toggleClass(statusClass).html(statusText);
        // If meeting can be ended, update end action link in the view.
        if (meetings.currentMeetings[i].canEnd) {
            var end_meetingClass = "bbb_end_meeting_hidden";
            var end_meetingText = "";
            if (meetings.currentMeetings[i].groupActive || (meetings.currentMeetings[i].joinable && meetings.currentMeetings[i].joinableMode == 'inprogress')) {
                end_meetingClass = "bbb_end_meeting_shown";
                if (meetings.currentMeetings[i].groupSessions) {
                    end_meetingText = "&nbsp;|&nbsp;&nbsp;" + "<a href=\"javascript:;\" onclick=\"return meetings.utils.endMeeting('" + escape(meetings.currentMeetings[i].name) + "','" + meetings.currentMeetings[i].id + "', " + undefined + ", true);\" title=\"" + bbb_action_end_meeting_tooltip + "\">" + bbb_action_end_meeting + "</a>";
                } else {
                    end_meetingText = "&nbsp;|&nbsp;&nbsp;" + "<a href=\"javascript:;\" onclick=\"return meetings.utils.endMeeting('" + escape(meetings.currentMeetings[i].name) + "','" + meetings.currentMeetings[i].id + "');\" title=\"" + bbb_action_end_meeting_tooltip + "\">" + bbb_action_end_meeting + "</a>";
                }
            }
            $('#end_meeting_' + meetings.currentMeetings[i].id).toggleClass(end_meetingClass);//.html(end_meetingText);
        }
    }
  }

  setMeetingInfoParams(meeting, meetingInfo) {

    // Clear attendees.
    if (meeting.attendees && meeting.attendees.length > 0) {
        delete meeting.attendees;
    }
    meeting.attendees = new Array();
    meeting.hasBeenForciblyEnded = "false";
    meeting.participantCount = 0;
    meeting.moderatorCount = 0;
    meeting.unreachableServer = "false";

    if (meetingInfo != null && meetingInfo.returncode != null) {
        if (meetingInfo.returncode != 'FAILED') {
            meeting.attendees = meetingInfo.attendees;
            meeting.hasBeenForciblyEnded = meetingInfo.hasBeenForciblyEnded;
            meeting.participantCount = meetingInfo.participantCount;
            meeting.moderatorCount = meetingInfo.moderatorCount;
            meeting.running = meetingInfo.running;
        } else if (meetingInfo.messageKey != 'notFound') {
            // Different errors can be handled here.
            meeting.unreachableServer = "true";
        }
    } else {
        delete meeting.running;
    }
  }

  setRecordingPermissionParams(recording) {

    // Specific recording permissions.
    var offset = meetings.settings.config.serverTimeInDefaultTimezone.timezoneOffset;
    recording.timezoneOffset = "GMT" + (offset > 0 ? "+" : "") + (offset / 3600000);
    if (meetings.currentUser.id === recording.ownerId) {
        recording.canEdit = meetings.userPerms.bbbRecordingEditOwn || meetings.userPerms.bbbRecordingEditAny;
        recording.canDelete = meetings.userPerms.bbbRecordingDeleteOwn || meetings.userPerms.bbbRecordingDeleteAny;
        recording.canViewExtendedFormats = meetings.userPerms.bbbRecordingExtendedFormatsOwn || meetings.userPerms.bbbRecordingExtendedFormatsAny;
    } else {
        recording.canEdit = meetings.userPerms.bbbRecordingEditAny;
        recording.canDelete = meetings.userPerms.bbbRecordingDeleteAny;
        recording.canViewExtendedFormats = meetings.userPerms.bbbRecordingExtendedFormatsAny;
    }
  }

  setMeetingPermissionParams(meeting) {

    // Joinable only if on specified date interval (if any).
    var serverTimeStamp = parseInt(meetings.settings.config.serverTimeInDefaultTimezone.timestamp);
    serverTimeStamp = (serverTimeStamp - serverTimeStamp % 1000);

    var startOk = !meeting.startDate || meeting.startDate == 0 || serverTimeStamp >= meeting.startDate;
    var endOk = !meeting.endDate || meeting.endDate == 0 || serverTimeStamp < meeting.endDate;

    meeting.notStarted = !startOk && endOk;
    meeting.finished = startOk && !endOk;
    meeting.joinable = startOk && endOk;

    // Specific meeting permissions.
    if (meetings.currentUser.id === meeting.ownerId) {
        meeting.canEdit = meetings.userPerms.bbbEditOwn || meetings.userPerms.bbbEditAny;
        meeting.canEnd = (meetings.userPerms.bbbEditOwn || meetings.userPerms.bbbEditAny) && (meetings.userPerms.bbbDeleteOwn || meetings.userPerms.bbbDeleteAny);
        meeting.canDelete = meetings.userPerms.bbbDeleteOwn || meetings.userPerms.bbbDeleteAny;
    } else {
        meeting.canEdit = meetings.userPerms.bbbEditAny;
        meeting.canEnd = meetings.userPerms.bbbEditAny && meetings.userPerms.bbbDeleteAny;
        meeting.canDelete = meetings.userPerms.bbbDeleteAny;
    }
  }

  setMeetingJoinableModeParams(meeting) {

    // If joinable set the joinableMode.
    meeting.joinableMode = "nojoinable";
    if (meeting.joinable) {
        if (meeting.unreachableServer == null) {
            meeting.joinableMode = "";
        } else if (meeting.unreachableServer == "false") {
            meeting.joinableMode = "available";
            $('#meetingStatus').show();
            if (meeting.hasBeenForciblyEnded == "true") {
                meeting.joinableMode = "unavailable";
                $('#meetingStatus').hide();
            } else if (meeting.running) {
                meeting.joinableMode = "inprogress";
                if (!meeting.canEnd && (!meeting.multipleSessionsAllowed || !meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedEnabled) && meetings.utils.isUserInMeeting(meetings.currentUser.displayName, meeting))
                    $('#meetingStatus').hide();
            }
        } else {
            meeting.joinableMode = "unreachable";
        }
    }

    // Update status in the view.
    var statusClass = meeting.joinable ? 'status_joinable_' + meeting.joinableMode : (meeting.notStarted ? 'status_notstarted' : 'status_finished')
    var statusText = meeting.joinable ? (meeting.joinableMode == 'available' ? this.i18n["bbb_status_joinable_available"] : meeting.joinableMode == 'inprogress' ? this.i18n["bbb_status_joinable_inprogress"] : meeting.joinableMode == 'unavailable' ? this.i18n["bbb_status_joinable_unavailable"] : meeting.joinableMode == 'unreachable' ? this.i18n["bbb_status_joinable_unreachable"] : '') : (meeting.notStarted ? this.i18n["bbb_status_notstarted"] : this.i18n["bbb_status_finished"]);
    $('#meeting_status_' + meeting.id).toggleClass(statusClass).html(statusText);
    // If meeting can be ended, update end action link in the view.
    if (meeting.canEnd) {
        var end_meetingClass = "bbb_end_meeting_hidden";
        var end_meetingText = "";
        if (meeting.joinable && meeting.joinableMode == 'inprogress') {
            end_meetingClass = "bbb_end_meeting_shown";
            if (meeting.groupSessions) {
                end_meetingText = "&nbsp;|&nbsp;&nbsp;" + "<a href=\"javascript:;\" onclick=\"return meetings.utils.endMeeting('" + escape(meeting.name) + "','" + meeting.id + "', " + undefined + ", true);\" title=\"" + bbb_action_end_meeting_tooltip + "\">" + bbb_action_end_meeting + "</a>";
            } else {
                end_meetingText = "&nbsp;|&nbsp;&nbsp;" + "<a href=\"javascript:;\" onclick=\"return meetings.utils.endMeeting('" + escape(meeting.name) + "','" + meeting.id + "');\" title=\"" + bbb_action_end_meeting_tooltip + "\">" + bbb_action_end_meeting + "</a>";
            }
        }
        $('#end_meeting_' + meeting.id).toggleClass(end_meetingClass);//.html(end_meetingText);
    }
  }

  endMeeting(name, meetingID, groupID, endAll) {

    var question;
    if (endAll) {
        question = this.i18n["bbb_action_end_all_meeting_question"].replace("{0}", unescape(name));
    } else {
        question = this.i18n["bbb_action_end_meeting_question"].replace("{0}", unescape(name));
    }
    if (!confirm(question)) return;

    const groupId = groupID ? "&groupId=" + groupID : "";
    const endAllMeetings = endAll ? "&endAll=true" : "";
    fetch(`/direct/bbb-tool/endMeeting?meetingID=${meetingID}${groupId}${endAllMeetings}`, this.getOptions)
      .then(r => meetings.utils.checkOneMeetingAvailability(meetingID, groupID))
      .catch(error => {

        const msg = this.i18n["bbb_err_end_meeting"].replace("{0}", name);
        this.handleError(msg);
      });
  }

  deleteMeeting(name, meetingId, i18n) {

    const question = i18n["bbb_action_delete_meeting_question"].replace("{0}", unescape(name));
    if (!confirm(question)) return;

    return fetch(`/direct/bbb-tool/${meetingId}`, {
        credentials: "same-origin",
        method: "DELETE",
    });
  }

  deleteRecordings(meetingID, recordID, stateFunction, confirmationMsg, i18n) {

    const question = i18n["bbb_action_delete_recording_question"].replace("{0}", unescape(confirmationMsg));

    if (!confirm(question)) return;

    jQuery.ajax({
        url: `/direct/bbb-tool/deleteRecordings?meetingID=${meetingID}&recordID=${recordID}`,
        dataType: 'text',
        type: "GET",
        success: function (result) {
            if (stateFunction == 'recordings')
                meetings.switchState('recordings');
            else
                meetings.switchState('recordings_meeting', {
                    'meetingId': meetingID
                });
        },
        error: function (xmlHttpRequest, status, error) {
            var msg = bbb_err_delete_recording(recordID);
            meetings.utils.handleError(msg, xmlHttpRequest.status, xmlHttpRequest.statusText);
        }
    });
  }

  publishRecordings(meetingID, recordID, stateFunction) {
    meetings.utils.setRecordings(meetingID, recordID, "true", stateFunction);
  }

  unpublishRecordings(meetingID, recordID, stateFunction) {
    meetings.utils.setRecordings(meetingID, recordID, "false", stateFunction);
  }

  setRecordings(meetingID, recordID, action, stateFunction) {

    jQuery.ajax({
        url: "/direct/bbb-tool/publishRecordings?meetingID=" + meetingID + "&recordID=" + recordID + "&publish=" + action,
        dataType: 'text',
        type: "GET",
        success: function (result) {
            if (stateFunction == 'recordings')
                meetings.switchState('recordings');
            else
                meetings.switchState('recordings_meeting', {
                    'meetingId': meetingID
                });
        },
        error: function (xmlHttpRequest, status, error) {
            if (action == 'true')
                var msg = bbb_err_publish_recording(recordID);
            else
                var msg = bbb_err_unpublish_recording(recordID);
            meetings.utils.handleError(msg, xmlHttpRequest.status, xmlHttpRequest.statusText);
        }
    });
  }

  protectRecordings(meetingID, recordID, stateFunction) {
    meetings.utils.updateRecordings(meetingID, recordID, "true", stateFunction);
  }

  unprotectRecordings(meetingID, recordID, stateFunction) {
    meetings.utils.updateRecordings(meetingID, recordID, "false", stateFunction);
  }

  updateRecordings(meetingID, recordID, action, stateFunction) {

    jQuery.ajax({
        url: "/direct/bbb-tool/protectRecordings?meetingID=" + meetingID + "&recordID=" + recordID + "&protect=" + action,
        dataType: 'text',
        type: "GET",
        success: function (result) {
            if (stateFunction == 'recordings')
                meetings.switchState('recordings');
            else
                meetings.switchState('recordings_meeting', {
                    'meetingID': meetingID
                });
        },
        error: function (xmlHttpRequest, status, error) {
            if (action == 'true')
                var msg = bbb_err_protect_recording(recordID);
            else
                var msg = bbb_err_unprotect_recording(recordID);
            meetings.utils.handleError(msg, xmlHttpRequest.status, xmlHttpRequest.statusText);
        }
    });
  }

  getMeetingInfo(meetingId, groupId, asynch) {

    var groupID = groupId ? "?groupId=" + groupId : "";
    var meetingInfo = null;
    if (typeof asynch == 'undefined') asynch = true;
    jQuery.ajax({
        url: "/direct/bbb-tool/" + meetingId + "/getMeetingInfo.json" + groupID,
        dataType: "json",
        async: asynch,
        success: function (data) {
            meetingInfo = data;
        },
        error: function (xmlHttpRequest, status, error) {
            meetings.utils.handleError(bbb_err_get_meeting, xmlHttpRequest.status, xmlHttpRequest.statusText);
            return null;
        }
    });
    return meetingInfo;
  }

  getSiteRecordingList(siteId) {

    if (siteId == null) siteId = "";

    var response = Object();
    jQuery.ajax({
        url: "/direct/bbb-tool/getSiteRecordings.json?siteId=" + siteId,
        dataType: "json",
        async: false,
        success: function (data) {
            response = data;
        },
        error: function (xmlHttpRequest, status, error) {
            meetings.utils.handleError(bbb_err_get_recording, xmlHttpRequest.status, xmlHttpRequest.statusText);
        }
    });
    return response;
  }

  getMeetingRecordingList(meetingId, groupId) {

    if (meetingId == null) meetingId = "";

    var groupID = groupId ? "?groupId=" + groupId : "";

    var response = Object();
    jQuery.ajax({
        url: "/direct/bbb-tool/" + meetingId + "/getRecordings.json" + groupID,
        dataType: "json",
        async: false,
        success: function (data) {
            response = data;
        },
        error: function (xmlHttpRequest, status, error) {
            meetings.utils.handleError(bbb_err_get_meeting, xmlHttpRequest.status, xmlHttpRequest.statusText);
        }
    });
    return response;
  }

  joinMeeting(meetingId, linkSelector, multipleSessionsAllowed, groupId, groupTitle) {

    var nonce = new Date().getTime();
    var url = "/direct/bbb-tool/" + meetingId + "/joinMeeting?nonce=" + nonce;
    url += groupId ? "&groupId=" + groupId : "";
    url += groupTitle ? "&groupTitle=" + groupTitle : "";
    meetings.utils.hideMessage();
    if (linkSelector) {
        $(linkSelector).attr('href', url);
        if (!multipleSessionsAllowed) {
            $('#meeting_joinlink_' + meetingId).hide();
            $('#meetingStatus').hide();
        }
        //.After joining stop requesting periodic updates.
        clearInterval(meetings.checkOneMeetingAvailabilityId);
        clearInterval(meetings.checkRecordingAvailabilityId);

        // After joining execute requesting updates only once.
        var onceAutorefreshInterval = meetings.settings.config.autorefreshInterval.meetings > 0 ? meetings.settings.config.autorefreshInterval.meetings : 15000;
        var groupID = groupId ? ", '" + groupId + "'" : "";
        meetings.updateMeetingOnceTimeoutId = setTimeout("meetings.utils.checkOneMeetingAvailability('" + meetingId + "'" + groupID + ")", onceAutorefreshInterval);
    }
    return true;
  }

  isUserInMeeting(userName, meeting) {

    for (var p = 0; p < meeting.attendees.length; p++) {
        if (meetings.currentUser.displayName === meeting.attendees[p].fullName) {
            return true;
        }
    }
    return false;
  }

  checkOneMeetingAvailability(meetingId, groupId) {

    if (typeof (groupId) === 'undefined') {
      meetings.currentMeetings.forEach(cm => {

        if (cm.id == meetingId) {
          meetings.utils.setMeetingInfo(cm);
          meetings.utils.checkMeetingAvailability(cm);
          meetings.updateMeetingInfo(cm);
          $("#end_session_link").attr("onclick", `return meetings.utils.endMeeting("${cm.name}","${cm.id}");`);
          return;
        }
      });
    } else {
      const cm = meetings.utils.getMeeting(meetingId);
      meetings.utils.setMeetingInfo(cm, groupId);
      meetings.utils.checkMeetingAvailability(m);
      meetings.updateMeetingInfo(cm);
      $("#end_session_link").attr("onclick", `return meetings.utils.endMeeting("${cm.name}","${cmid}","${groupId}");`);
      return;
    }
  }

  checkAllMeetingAvailability() {

    meetings.currentMeetings.forEach(cm => {

      if (!cm.joinable) {
        meetings.utils.setMeetingInfo(cm);
      }
      meetings.utils.setMeetingJoinableModeParams(cm);
      meetings.utils.checkMeetingAvailability(cm);
    });
  }

  checkMeetingAvailability(meeting) {

    if (meeting.joinable) {
      if (meeting.joinableMode === "available") {
        if (meeting.multipleSessionsAllowed && meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedEnabled) {
          $('#meeting_joinlink_' + meeting.id).show();
        } else {
          if (!meetings.utils.isUserInMeeting(meetings.currentUser.displayName, meeting)) {
            $('#meeting_joinlink_' + meeting.id).show();
          } else {
            $('#meeting_joinlink_' + meeting.id).hide();
          }
        }
        // Update the actionbar on the list.
        if (meeting.canEnd) {
          $('#end_meeting_' + meeting.id)
            .removeClass()
            .addClass('bbb_end_meeting_hidden');
          $('#end_meeting_intermediate_' + meeting.id)
            .removeClass()
            .addClass('bbb_end_meeting_hidden');
        }
        // Update for list.
        $('#meeting_status_' + meeting.id)
          .removeClass()
          .addClass('status_joinable_available')
          .text(bbb_status_joinable_available);
        // Update for detail.
        $('#meeting_status_joinable_' + meeting.id)
          .removeClass()
          .addClass('status_joinable_available')
          .text(bbb_status_joinable_available);
      } else if (meeting.joinableMode === "inprogress") {
        var end_meetingTextIntermediate = "&nbsp;|&nbsp;&nbsp;<a id=\"end_session_link\" href=\"javascript:;\" onclick=\"return meetings.utils.endMeeting('" + escape(meeting.name) + "','" + meeting.id + "');\" title=\"" + bbb_action_end_meeting_tooltip + "\" style=\"font-weight:bold\">" + bbb_action_end_meeting + "</a>&nbsp;<span><i class=\"fa fa-stop\"></i></span>";
        if (meeting.multipleSessionsAllowed && meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedEnabled) {
          $('#meeting_joinlink_' + meeting.id).show();
        } else {
          if (!meetings.utils.isUserInMeeting(meetings.currentUser.displayName, meeting)) {
            $('#meeting_joinlink_' + meeting.id).show();
          } else {
            $('#meeting_joinlink_' + meeting.id).hide();
            end_meetingTextIntermediate = "<a id=\"end_session_link\" href=\"javascript:;\" onclick=\"return meetings.utils.endMeeting('" + escape(meeting.name) + "','" + meeting.id + "');\" title=\"" + bbb_action_end_meeting_tooltip + "\" style=\"font-weight:bold\">" + bbb_action_end_meeting + "</a>&nbsp;<span><i class=\"fa fa-stop\"></i></span>";
          }
        }
        $('#end_meeting_intermediate_' + meeting.id).toggleClass("bbb_end_meeting_shown").html(end_meetingTextIntermediate);

        // Update the actionbar on the list.
        if (meeting.canEnd) {
          $('#end_meeting_' + meeting.id)
            .removeClass()
            .addClass('bbb_end_meeting_shown');
          $('#end_meeting_intermediate_' + meeting.id)
            .removeClass()
            .addClass('bbb_end_meeting_shown');
        }
        // Update for list.
        $('#meeting_status_' + meeting.id)
          .removeClass()
          .addClass('status_joinable_inprogress')
          .text(bbb_status_joinable_inprogress);
        // Update for detail.
        $('#meeting_status_joinable_' + meeting.id)
          .removeClass()
          .addClass('status_joinable_inprogress')
          .text(bbb_status_joinable_inprogress);
      } else if (meeting.joinableMode === "unavailable") {
        $('#meeting_joinlink_' + meeting.id).fadeOut();
        // Update the actionbar on the list.
        if (meeting.canEnd) {
          $('#end_meeting_' + meeting.id)
            .removeClass()
            .addClass('bbb_end_meeting_hidden');
          $('#end_meeting_intermediate_' + meeting.id)
            .removeClass()
            .addClass('bbb_end_meeting_hidden');
        }
        // Update for list.
        $('#meeting_status_' + meeting.id)
          .removeClass()
          .addClass('status_joinable_unavailable')
          .text(bbb_status_joinable_unavailable);
        // Update for detail.
        $('#meeting_status_joinable_' + meeting.id)
          .removeClass()
          .addClass('status_joinable_unavailable')
          .text(bbb_status_joinable_unavailable);

        $('#bbb_meeting_info_participants_count').html('0');
        $('#bbb_meeting_info_participants_count_tr').fadeOut();
        $('#bbb_meeting_info_participants_count_tr').hide();
      } else if (meeting.joinableMode === "unreachable") {
        $('#meeting_joinlink_' + meeting.id).fadeOut();
        // Update the actionbar on the list.
        if (meeting.canEnd) {
          $('#end_meeting_' + meeting.id)
            .removeClass()
            .addClass('bbb_end_meeting_hidden');
          $('#end_meeting_intermediate_' + meeting.id)
            .removeClass()
            .addClass('bbb_end_meeting_hidden');
        }
        // Update for list.
        $('#meeting_status_' + meeting.id)
          .removeClass()
          .addClass('status_joinable_unreachable')
          .text(bbb_status_joinable_unreachable);
        // Update for detail.
        $('#meeting_status_joinable_' + meeting.id)
          .removeClass()
          .addClass('status_joinable_unreachable')
          .text(bbb_status_joinable_unreachable);

        $('#bbb_meeting_info_participants_count').html('0');
        $('#bbb_meeting_info_participants_count_tr').fadeOut();
        $('#bbb_meeting_info_participants_count_tr').hide();
      }
    } else if (meeting.notStarted) {
      $('#meeting_joinlink_' + meeting.id).fadeOut();
      $('#meeting_status_' + meeting.id)
        .removeClass()
        .addClass('status_notstarted')
        .text(bbb_status_notstarted);
    } else if (meeting.finished) {
      $('#meeting_joinlink_' + meeting.id).fadeOut();
      $('#meeting_status_' + meeting.id)
        .removeClass()
        .addClass('status_finished')
        .text(bbb_status_finished);
    }
  }

  checkOneRecordingAvailability(meetingId) {

    const meeting = meetings.currentMeetings.find(m => m.id === meetingId);
    meeting && meetings.utils.checkRecordingAvailability(meeting);
  }

  checkAllRecordingAvailability() {
    meetings.currentMeetings.forEach(m => meetings.utils.checkRecordingAvailability(m));
  }

  checkRecordingAvailability(i18n, meetingId, groupId) {

    const recordings = meetings.utils.getMeetingRecordingList(meetingId, groupId).recordings;
    if (recordings == null) {
      meetings.utils.showMessage(i18n["bbb_err_get_recording"], 'warning');
    } else {
      meetings.utils.hideMessage();
      const meeting = meetings.currentMeetings.find(m => m.id === meetingId);
      const meetingRecordingEnabled = meeting && meeting.recording;
      if (!meetings.userPerms.bbbRecordingView || !meetings.settings.config.addUpdateFormParameters.recordingEnabled || !meetingRecordingEnabled) {
        $('#meeting_recordings').hide();
      } else {
        $('#meeting_recordings').show();
        var htmlRecordings = "";
        var groupID = groupId ? "', 'groupId':'" + groupId : "";
        if (recordings.length > 0) {
          htmlRecordings = '(<a href="javascript:;" onclick="return meetings.switchState(\'recordings_meeting\',{\'meetingId\':\'' + meetingId + groupID + '\'})" title="">' + i18n["bbb_meetinginfo_recordings"].replace("{0}", unescape(recordings.length)) + '</a>)&nbsp;&nbsp;';
        } else {
            htmlRecordings = "(" + i18n["bbb_meetinginfo_recordings"].replace("{0}", unescape(recordings.length)) + ")";
        }

        $('#recording_link_' + meetingId).html(htmlRecordings);
      }
    }
  }

  addNotice() {

    jQuery.ajax({
        url: "/direct/bbb-tool/getNoticeText.json",
        dataType: "json",
        async: true,
        success: function (notice) {
            if (notice && notice.text) {
                meetings.utils.showMessage(notice.text, notice.level);
            }
        }
    });
  }

  getParticipantFromMeeting(meeting) {

    var userId = meetings.currentUser.id;
    var role = meetings.currentUser.roles != null ? meetings.currentUser.roles.role : null;
    if (meeting && meeting.participants) {
        // 1. we want to first check individual user selection as it may
        // override all/group/role selection...
        for (var i = 0; i < meeting.participants.length; i++) {
            if (meeting.participants[i].selectionType == 'user' &&
                meeting.participants[i].selectionId == userId) {
                return meeting.participants[i];
            }
        }

        // 2. ... then with group/role selection types...
        for (var i = 0; i < meeting.participants.length; i++) {
            if (meeting.participants[i].selectionType == 'role' &&
                meeting.participants[i].selectionId == role) {
                return meeting.participants[i];
            }
        }

        // 3. ... then go with 'all' selection type
        for (var i = 0; i < meeting.participants.length; i++) {
            if (meeting.participants[i].selectionType == 'all') {
                return meeting.participants[i];
            }
        }
    }
    return null;
  }

  getUserSelectionTypes(i18n) {

    var selTypes = {
      all: {
        id: 'all',
        title: i18n["bbb_seltype_all"]
      },
      user: {
        id: 'user',
        title: i18n["bbb_seltype_user"]
      },
      group: {
        id: 'group',
        title: i18n["bbb_seltype_group"]
      },
      role: {
        id: 'role',
        title: i18n["bbb_seltype_role"]
      }
    };
    return selTypes;
  }

  async getUserSelectionOptions(siteId) {
    return fetch(`/direct/bbb-tool/getUserSelectionOptions.json?siteId=${siteId}`).then(r => r.json());
  }

  getSitePermissions(siteId) {
    return fetch(`/direct/site/${siteId}/perms/bbb.json`, this.getOptions).then(r => r.json());
  }

  render(templateName, contextObject, output) {

    contextObject._MODIFIERS = {};
    var templateNode = document.getElementById(templateName);
    var firstNode = templateNode.firstChild;
    var template = '';
    if (firstNode && (firstNode.nodeType === 8 || firstNode.nodeType === 4)) {
      template += templateNode.firstChild.data.toString();
    } else {
      template += templateNode.innerHTML.toString();
    }

    var trimpathTemplate = TrimPath.parseTemplate(template, templateName);

    var render = trimpathTemplate.process(contextObject);

    if (output) {
      document.getElementById(output).innerHTML = render;
    }

    return render;
  }

  setupAjax() {

    jQuery.ajaxSetup({
      async: true,
      cache: false,
      timeout: 30000,
      complete: function (request, textStatus) {
        try {
          if (request.status &&
            request.status != 200 && request.status != 201 &&
            request.status != 204 && request.status != 404 && request.status != 1223) {
            if (request.status == 403) {
              this.handleError(bbb_err_no_permissions, request.status, request.statusText);
              $('#bbb_content').empty();
            } else {
              // Handled by error() callbacks.
            }
          }
        } catch (e) {}
      }
    });
  }

  handleError(message) {
    meetings.utils.showMessage("", "error", message);
  }

  /**
   * Render a message with a specific severity
   * @argument msgBod: The message to be displayed
   * @argument severity: Message severity [optional, defaults to 'information')
   * @argument msgTitle: Message title [optional, defaults to nothing]
   */
  showMessage(msgBody, severity, msgTitle, hideMsgBody) {

    var useAlternateStyle = true;
    if (typeof hideMsgBody == 'undefined' && msgTitle && msgBody) hideMsgBody = true;

    if (!meetings.errorLog[msgBody]) {
      meetings.errorLog[msgBody] = true;

      // severity
      var msgClass = null;
      if (!severity || severity == 'info' || severity == 'information') {
        msgClass = !useAlternateStyle ? 'information' : 'messageInformation';
      } else if (severity == 'success') {
        msgClass = !useAlternateStyle ? 'success' : 'messageSuccess';
      } else if (severity == 'warn' || severity == 'warning' || severity == 'error' || severity == 'fail') {
        msgClass = !useAlternateStyle ? 'alertMessage' : 'messageError';
      }

      // add contents
      var id = Math.floor(Math.random() * 1000);
      var msgId = 'msg-' + id;
      var msgDiv = $('<div class="bbb_message" id="' + msgId + '"></div>');
      var msgsDiv = $('#bbb_messages').append(msgDiv);
      var message = $('<div class="' + msgClass + '"></div>');
      if (msgTitle && msgTitle != '') {
        message.append('<h4>' + msgTitle + '</h4>');
        if (hideMsgBody) message.append('<span id="msgShowDetails-' + id + '">&nbsp;<small>(<a href="#" onclick="$(\'#msgBody-' + id + '\').slideDown();$(\'#msgShowDetails-' + id + '\').hide();return false;">' + bbb_err_details + '</a>)</small></span>');
      }
      $('<p class="closeMe">  (x) </p>').click(function () {
        meetings.utils.hideMessage(msgId);
      }).appendTo(message);
      if (msgBody) {
        var msgBodyContent = $('<div id="msgBody-' + id + '" class="content">' + msgBody + '</div>');
        message.append(msgBodyContent);
        if (hideMsgBody) msgBodyContent.hide();
      }

      // display, adjust frame height, scroll to top.
      msgDiv.html(message);
      msgsDiv.fadeIn();
      $('html, body').animate({scrollTop: 0}, 'slow');
    }
  }

  hideMessage(id) {

    /*
    delete meetings.errorLog;
    meetings.errorLog = new Object();
    if (id) {
      $('#' + id).fadeOut();
    } else {
      $('#bbb_messages').empty().hide();
    }
    */
  }

  showAjaxIndicator(outputSelector) {

    $(outputSelector).empty()
      .html('<img src="/bbb-tool/images/ajaxload.gif" alt="..." class="bbb_imgIndicator"/>')
      .show();
  }

  hideAjaxIndicator(outputSelector) {
    $(outputSelector).hide();
  }

  makeInlineCKEditor(textAreaId, toolBarSet, width, height) {
    this.editor = sakai.editor.launch(textAreaId, "basic", width, height);
  }

  updateFromInlineCKEditor(textAreaId) {

    if (typeof CKEDITOR != "undefined") {
      var editor = CKEDITOR.instances[textAreaId];
      if (editor != null) {
        if (editor.checkDirty()) {
          editor.updateElement();
          var ta_temp = document.createElement("textarea");
          ta_temp.innerHTML = editor.getData().replace(/</g, "&lt;").replace(/>/g, "&gt;");
          var decoded_html = ta_temp.value;
          $('#' + textAreaId).text(decoded_html);
        }
        editor.destroy();
      }
    }
  }

  setNotifictionOptions() {

    if ($('#notifyParticipants')[0].checked) {
      $('#notifyParticipants_iCalAttach_span').empty()
        .html('<br>' + bbb_notification_notify_ical + '&nbsp;<input id="iCalAttached" name="iCalAttached" type="checkbox" ' + (meetings.startupArgs.checkICalOption ? 'checked="checked" ' : ' ') + 'onclick="meetings.utils.setNotifictioniCalOptions();"/>&nbsp;<span id="notifyParticipants_iCalAlarm_span"></span>')
        .show();
      if (meetings.startupArgs.checkICalOption) {
        $('#notifyParticipants_iCalAlarm_span').empty()
          .html('<br>' + bbb_notification_notify_ical_alarm + '&nbsp;<input id="iCalAlarmMinutes" name="iCalAlarmMinutes" type="text" value="30" style="width: 35px;" />&nbsp;' + bbb_notification_notify_ical_alarm_units)
          .show();
      }
    } else {
      if ($('#iCalAttached')[0].checked) {
        // Hide the iCalAlarm.
        $('#notifyParticipants_iCalAlarm_span').empty().hide();
        // Uncheck the iCalAttach checkbox.
        $('#iCalAttached').removeAttr('checked');
      }
      // Hide the iCalAttach.
      $('#notifyParticipants_iCalAttach_span').empty().hide();
    }
  }

  setNotifictioniCalOptions() {

    if ($('#iCalAttached')[0].checked) {
      $('#notifyParticipants_iCalAlarm_span').empty()
        .html('<br>' + bbb_notification_notify_ical_alarm + '&nbsp;<input id="iCalAlarmMinutes" name="iCalAlarmMinutes" type="text" value="30" style="width: 35px;" />&nbsp;' + bbb_notification_notify_ical_alarm_units)
        .show();
    } else {
      // Hide the iCalAlarm.
      $('#notifyParticipants_iCalAlarm_span').empty().hide();
    }
  }
}

export {MeetingUtils};
