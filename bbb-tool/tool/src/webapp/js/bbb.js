import {loadProperties} from "/webcomponents/sakai-i18n.js";
import {MeetingUtils} from "./bbb_utils.js";
import {BBBPermissions} from "./bbb_perms.js";
import {BBBTemplates} from "./bbb_templates.js";
import {render} from "/webcomponents/assets/lit-html/lit-html.js";

/* Stuff that we always expect to be setup */
meetings.currentMeetings = [];
meetings.currentRecordings = Array();
meetings.checkOneMeetingAvailabilityId = null;
meetings.checkAllMeetingAvailabilityId = null;
meetings.checkRecordingAvailabilityId = null;
meetings.refreshRecordingListId = null;
meetings.errorLog = new Object();
meetings.browserTimezoneOffset = 0;

meetings.loadSiteDataAndInit = async function () {

  meetings.utils = new MeetingUtils(meetings.i18n);

  try {
    meetings.utils.bbbUserSelectionOptions = await meetings.utils.getUserSelectionOptions(meetings.startupArgs.siteId);
  } catch (error) {
    meetings.utils.handleError(meetings.i18n["bbb_err_user_sel_options"]);
  }

  Array.prototype.addUpdateMeeting = function (meeting) {

    if (meeting && meeting.id) {
      const index = this.findIndex(id => id === meeting.id);
      if (index >= 0) {
        this[index] = meeting;
      } else {
        this.push(meeting);
      }
    } else if (meeting) {
      this.push(meeting);
    }
  };

  Date.prototype.stdTimezoneOffset = function () {

    var jan = new Date(this.getFullYear(), 0, 1);
    var jul = new Date(this.getFullYear(), 6, 1);
    return Math.max(jan.getTimezoneOffset(), jul.getTimezoneOffset());
  };

  Date.prototype.dst = function () {
    return this.getTimezoneOffset() < this.stdTimezoneOffset();
  };

  const arg = meetings.startupArgs;

  if (!arg || !arg.siteId) {
    meetings.utils.showMessage(meetings.i18n.bbb_err_no_siteid, 'error');
    return;
  }

  // Load language for datepick.
  const lang = arg['language'].split("_");
  if (lang[0] != "en") {
    if (lang.length == 2) {
      const getScriptResponse = jQuery.getScript("lib/jquery.datepick.package-3.7.5/jquery.datepick-" + lang[0] + "-" + lang[1] + ".js", function () {});
      if (getScriptResponse.status == 404) {
        jQuery.getScript("lib/jquery.datepick.package-3.7.5/jquery.datepick-" + lang[0] + ".js");
      }
    } else {
      jQuery.getScript("lib/jquery.datepick.package-3.7.5/jquery.datepick-" + lang[0] + ".js");
    }
  }

  // We need the toolbar in a template so we can swap in the translations.
  //utils.render('bbb_toolbar_template', {}, 'bbb_toolbar');
  render(BBBTemplates.toolbar(meetings.i18n), document.getElementById("bbb_toolbar"));

  $('#bbb_home_link').click(function (e) {
    return meetings.switchState('currentMeetings');
  }).show();

  $('#bbb_permissions_link').click(function (e) {
    return meetings.switchState('permissions');
  }).hide();

  $('#bbb_recordings_link').click(function (e) {
    return meetings.switchState('recordings');
  }).hide();

  // Setup Ajax defaults.
  meetings.utils.setupAjax();

  meetings.utils.getSettings(arg.siteId).then(s => {

    meetings.settings = s;
    meetings.currentUser = meetings.settings.currentUser;

    meetings.userPerms = new BBBPermissions(meetings.currentUser.permissions);
    meetings.startupArgs.timezoneoffset = arg.timezoneoffset;
    var d = new Date();
    meetings.browserTimezoneOffset = d.getTimezoneOffset() * 60 * 1000 * -1;

    // Now switch into the requested state.
    if (meetings.currentUser != null) {
      meetings.switchState(arg.state, arg);
    } else {
      meetings.utils.showMessage(meetings.i18n["bbb_err_no_user"], 'error');
      jQuery('#bbb_container').empty();
    }

    // If configured, show text notice (first time access).
    meetings.utils.addNotice();
  });
};

meetings.switchState = async function (state, arg) {

  if (meetings.checkOneMeetingAvailabilityId != null) clearInterval(meetings.checkOneMeetingAvailabilityId);
  if (meetings.checkAllMeetingAvailabilityId != null) clearInterval(meetings.checkAllMeetingAvailabilityId);
  if (meetings.checkRecordingAvailabilityId != null) clearInterval(meetings.checkRecordingAvailabilityId);
  if (meetings.refreshRecordingListId != null) clearInterval(meetings.refreshRecordingListId);

  meetings.utils.hideMessage();

    // Clean navbar button state.
  $("#bbb_toolbar_items li>span").removeClass('current');

  if ('currentMeetings' === state) {
    $("#bbb_home_link").parent().addClass('current');
    // Show recordings links only if site maintainer or if has specific view permission.
    $('#bbb_recordings_link').unbind('click');
    if ((!meetings.userPerms.bbbAdmin && !meetings.userPerms.bbbRecordingView) || !meetings.settings.config.addUpdateFormParameters.recordingEnabled) {
      $('#bbb_recordings_link').parent().parent().hide();
    } else {
      $('#bbb_recordings_link').parent().parent().show();
      $('#bbb_recordings_link').click(function (e) {
        return meetings.switchState('recordings');
      }).show();
    }

    // Show permissions links only if site maintainer.
    $('#bbb_permissions_link').unbind('click');
    if (meetings.userPerms.bbbAdmin) {
      $('#bbb_permissions_link').parent().parent().show();
      $('#bbb_permissions_link').click(function (e) {
        return meetings.switchState('permissions');
      }).show();
    } else {
      $('#bbb_permissions_link').parent().parent().hide();
    }

    if (meetings.userPerms.bbbDeleteAny) {
      $('#bbb_end_meetings_link').parent().parent().show();
    } else {
      $('#bbb_end_meetings_link').parent().parent().hide();
    }

    // Show meeting list.
    if (meetings.userPerms.bbbViewMeetingList) {
      await meetings.refreshMeetingList();
      render(BBBTemplates.rooms(meetings.i18n, meetings.currentMeetings), document.getElementById("bbb_content"));

      document.querySelectorAll(".bbb-delete-meeting-link").forEach(l => {

        l.removeEventListener("click", meetings.deleteMeetingEventListener);
        l.addEventListener("click", meetings.deleteMeetingEventListener);
      });

      document.querySelectorAll(".bbb-edit-meeting-link").forEach(l => {

        l.removeEventListener("click", meetings.editMeetingEventListener);
        l.addEventListener("click", meetings.editMeetingEventListener);
      });

      document.querySelectorAll(".bbb-meeting-info-link").forEach(l => {

        l.removeEventListener("click", meetings.meetingInfoEventListener);
        l.addEventListener("click", meetings.meetingInfoEventListener);
      });

      $('#bbb_create_meeting_link').bind('click', function (e) {
        return meetings.switchState('addUpdateMeeting');
      });

      var $rows = $('#bbb_meeting_table tbody tr');
      $('.search').keyup(function () {
        var val = $.trim($(this).val()).replace(/ +/g, ' ').toLowerCase();

        $rows.show().filter(function () {
          var text = $(this).text().replace(/\s+/g, ' ').toLowerCase();
          return !~text.indexOf(val);
        }).hide();
      });

      // Show links if user has appropriate permissions.
      if (meetings.userPerms.bbbCreate) {
        $('#bbb_create_meeting_link').show();
      } else {
        $('#bbb_create_meeting_link').hide();
      }

      // Auto hide actions.
      jQuery('.meetingRow')
        .bind('mouseenter', function () {
          jQuery(this).addClass('bbb_even_row');
        })
        .bind('mouseleave', function () {
          jQuery(this).removeClass('bbb_even_row');
        });

      // Add parser for customized date format.
      $.tablesorter.addParser({
        id: "bbbDateTimeFormat",
        is: function (s) {
          return false;
        },
        format: function (s, table) {
          return $.tablesorter.formatFloat(new Date(s).getTime());
        },
        type: "numeric"
      });

      // Add sorting capabilities.
      $("#bbb_meeting_table").tablesorter({
        cssHeader: 'bbb_sortable_table_header',
        cssAsc: 'bbb_sortable_table_header_sortup',
        cssDesc: 'bbb_sortable_table_header_sortdown',
        headers: {
          2: {
            sorter: 'bbbDateTimeFormat'
          },
          3: {
            sorter: 'bbbDateTimeFormat'
          }
        },
        // Sort DESC status:
        sortList: (meetings.currentMeetings.length > 0) ? [[0, 0]] : [],
      });

      if (meetings.settings.config.autorefreshInterval.meetings > 0) {
        meetings.checkAllMeetingAvailabilityId = setInterval("utils.checkAllMeetingAvailability()", meetings.settings.config.autorefreshInterval.meetings);
      }
    } else {
      // Warn about lack of permissions.
      if (meetings.userPerms.siteUpdate) {
        meetings.utils.showMessage(bbb_err_no_tool_permissions_maintainer);
      } else {
        meetings.utils.showMessage(bbb_err_no_tool_permissions);
      }
      $('#bbb_content').empty();
    }
  } else if ('addUpdateMeeting' === state) {
    $('#bbb_recordings_link').parent().parent().hide();
    $('#bbb_end_meetings_link').parent().parent().hide();
    $('#bbb_permissions_link').parent().parent().hide();

    const isNew = !(arg && arg.meetingId);
    (isNew ? Promise.resolve({name: ""}) : meetings.utils.getMeeting(arg.meetingId)).then(async meeting => {

      meetings.utils.setMeetingPermissionParams(meeting);
      const contextData = {
        isNew: isNew,
        meeting: meeting,
        selTypes: meetings.utils.getUserSelectionTypes(meetings.i18n),
        selOptions: meetings.utils.bbbUserSelectionOptions,
        siteId: meetings.startupArgs.siteId,
        recordingEnabled: meetings.settings.config.addUpdateFormParameters.recordingEnabled,
        recordingEditable: meetings.settings.config.addUpdateFormParameters.recordingEditable,
        recordingDefault: meetings.settings.config.addUpdateFormParameters.recordingDefault,
        durationEnabled: meetings.settings.config.addUpdateFormParameters.durationEnabled,
        durationDefault: meetings.settings.config.addUpdateFormParameters.durationDefault,
        waitmoderatorEnabled: meetings.settings.config.addUpdateFormParameters.waitmoderatorEnabled,
        waitmoderatorEditable: meetings.settings.config.addUpdateFormParameters.waitmoderatorEditable,
        waitmoderatorDefault: meetings.settings.config.addUpdateFormParameters.waitmoderatorDefault,
        multiplesessionsallowedEnabled: meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedEnabled,
        multiplesessionsallowedEditable: meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedEditable,
        multiplesessionsallowedDefault: meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedDefault,
        preuploadpresentationEnabled: meetings.settings.config.addUpdateFormParameters.preuploadpresentationEnabled,
        groupsessionsEnabled: meetings.settings.config.addUpdateFormParameters.groupsessionsEnabled,
        groupsessionsEditable: meetings.settings.config.addUpdateFormParameters.groupsessionsEditable,
        groupsessionsDefault: meetings.settings.config.addUpdateFormParameters.groupsessionsDefault,
        actionUrl: isNew ? "/direct/bbb-tool/new" : "/direct/bbb-tool/" + meeting.id + "/edit",
        recordingChecked: isNew ? meetings.settings.config.addUpdateFormParameters.recordingDefault : meeting.recording,
        showEndDate: meeting.isNew ? false : meeting.endDate && meeting.endDate > 0,
        showStartDate: isNew ? false : meeting.startDate && meeting.startDate > 0,
        thing: (isNew && meetings.userPerms.calendarNew) || (!isNew && ((meeting.ownerId == meetings.currentUser.id && meetings.userPerms.calendarReviseOwn) || (meeting.ownerId != meetings.currentUser.id && meetings.userPerms.calendarReviseAny))),
        addToCalendar: isNew ? true : meeting.properties.calendarEventId != null,
        waitForModerator: isNew ? meetings.settings.config.addUpdateFormParameters.waitmoderatorDefault : meeting.waitForModerator,
        multipleSessionsAllowed: isNew ? meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedDefault : meeting.multipleSessionsAllowed,
        groupSessions: isNew ? meetings.settings.config.addUpdateFormParameters.groupsessionsDefault : meeting.groupSessions,
        statusClass: meeting.joinable ? `status_joinable_${meeting.joinableMode}` : (meeting.notStarted ? 'bbb_status_notstarted' : 'bbb_status_finished'),
        statusText: meeting.joinable ? (meeting.joinableMode == 'available' ? bbb_status_joinable_available: meeting.joinableMode == 'inprogress'? bbb_status_joinable_inprogress: meeting.joinableMode == 'unavailable'? bbb_status_joinable_unavailable: meeting.joinableMode == 'unreachable'? bbb_status_joinable_unreachable: '' ) : (meeting.notStarted ? bbb_status_notstarted : bbb_status_finished),
      };

      await render(BBBTemplates.addUpdateMeeting(meetings.i18n, contextData), document.getElementById("bbb_content"));

      $('#startDate1').change(function (e) {

        if ($(this).prop('checked')) {
          $('#startDateBox').show();
        } else {
          $('#startDateBox').hide();
          $('.time-picker').hide();
        }
      });

      // Show the presentation/file upload if meeting has one.
      if (meeting.presentation) {
        var url = meeting.presentation;
        $("#fileUrl").val(url);
        $("#url").attr("href", url);
        $("#url").text(url.substring(url.lastIndexOf("/") + 1));
        $("#fileView").show();
        $("#selectFile").attr("disabled", true);
      }

      $("#selectFile").change(function () {

        meetings.utils.hideMessage();
        if (!this.files[0]) return;

        var acceptedTypes = ['ppt', 'pptx', 'pdf', 'jpeg', 'png', 'gif', 'jpg'];
        var extension = $(this).val().split('.').pop();
        if (acceptedTypes.indexOf(extension) == -1) {
          meetings.utils.showMessage(bbb_warning_bad_filetype, 'warning');
          $(this).val('');
          return;
        }
        var maxFileSize = meetings.startupArgs.maxFileSizeInBytes;
        if (this.files[0].size > maxFileSize * 1024 * 1024) {
          meetings.utils.showMessage(bbb_warning_max_filesize(maxFileSize), 'warning');
          $(this).val('');
          return;
        }

        $("#selectFile").attr("disabled", true);
        meetings.utils.doUpload(this, siteId).then(url => {

          $('#bbb_save,#bbb_cancel').prop('disabled', false);
          this.hideAjaxIndicator('#bbb_addFile_ajaxInd');
          $("#fileUrl").val(url.substring(url.indexOf('/access')));
          $("#url").attr("href", url);
          $("#url").text(url.substring(url.lastIndexOf("/") + 1));
          $("#fileView").show();
        })
        .catch(error => {
          console.error("Failed to upload files");
          $('#bbb_save,#bbb_cancel').prop('disabled', false);
          meetings.utils.hideAjaxIndicator('#bbb_addFile_ajaxInd');
        });
      });

      $("#removeUpload").click(function () {

        let resourceId = $("#fileUrl").val();
        resourceId = resourceId.substring(resourceId.indexOf('/attachment'));
        if (!isNew) {
          meetings.utils.removeUpload(resourceId, meeting.id);
        } else {
          meetings.utils.removeUpload(resourceId);
        }
      });

      $('#endDate1').change(function (e) {

        if ($(this).prop('checked')) {
          $('#endDateBox').show();
        } else {
          $('#endDateBox').hide();
          $('.time-picker').hide();
        }
      });

      // Focus on meeting name/title.
      $('#bbb_meeting_name_field').focus();

      // Setup description/welcome msg editor.
      meetings.utils.makeInlineCKEditor('bbb_welcome_message_textarea', 'BBB', '480', '200');

      // Setup dates.
      var now = new Date();
      var now_utc = new Date(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate(), now.getUTCHours(), now.getUTCMinutes(), now.getUTCSeconds());
      var now_local = new Date(parseInt(now_utc.getTime()) + parseInt(meetings.startupArgs.timezoneoffset));
      var now_local_plus_1 = new Date(parseInt(now_utc.getTime()) + parseInt(meetings.startupArgs.timezoneoffset) + 3600000);

      var startDate = (!isNew && meeting.startDate) ? new Date(parseInt(meeting.startDate) - parseInt(meetings.browserTimezoneOffset) + parseInt(meetings.startupArgs.timezoneoffset) + ((new Date()).dst() && !(new Date(parseInt(meeting.startDate) - parseInt(meetings.browserTimezoneOffset) + parseInt(meetings.startupArgs.timezoneoffset))).dst() ? 3600000 : !(new Date()).dst() && (new Date(parseInt(meeting.startDate) - parseInt(meetings.browserTimezoneOffset) + parseInt(meetings.startupArgs.timezoneoffset))).dst() ? (3600000 * -1) : 0)) : now_local;
      var endDate = (!isNew && meeting.endDate) ? new Date(parseInt(meeting.endDate) - parseInt(meetings.browserTimezoneOffset) + parseInt(meetings.startupArgs.timezoneoffset) + ((new Date()).dst() && !(new Date(parseInt(meeting.endDate) - parseInt(meetings.browserTimezoneOffset) + parseInt(meetings.startupArgs.timezoneoffset))).dst() ? 3600000 : !(new Date()).dst() && (new Date(parseInt(meeting.endDate) - parseInt(meetings.browserTimezoneOffset) + parseInt(meetings.startupArgs.timezoneoffset))).dst() ? (3600000 * -1) : 0)) : now_local_plus_1;

      // Setup time picker.
      var zeropad = function (num) {
        return ((num < 10) ? '0' : '') + num;
      }
      jQuery('#startTime').val(zeropad(startDate.getHours()) + ':' + zeropad(startDate.getMinutes()));
      jQuery('#endTime').val(zeropad(endDate.getHours()) + ':' + zeropad(endDate.getMinutes()));
      jQuery(".time-picker").remove();
      jQuery("#startTime, #endTime").timePicker({ separator: ':' });

      // Setup date picker.
      jQuery.datepick.setDefaults({
        dateFormat: jQuery.datepick.W3C,
        defaultDate: '+0',
        showDefault: true,
        showOn: 'both',
        buttonImageOnly: true,
        buttonImage: '/library/calendar/images/calendar/cal.gif'
      });
      jQuery('#startDate2, #endDate2').datepick();
      jQuery('#startDate2').datepick('setDate', startDate);
      jQuery('#endDate2').datepick('setDate', endDate);

      // Add meeting participants.
      meetings.addParticipantSelectionToUI(meeting, isNew);

      $('#bbb_save').click(function (e) {

        meetings.utils.addUpdateMeeting().then(meeting => {

          meetings.currentMeetings.addUpdateMeeting(meeting);
          meetings.utils.hideAjaxIndicator('#bbb_addUpdate_ajaxInd');
          meetings.switchState('currentMeetings');
        })
        .catch(error => {

          meetings.utils.hideAjaxIndicator('#bbb_addUpdate_ajaxInd');
          $('#bbb_save,#bbb_cancel').prop('disabled', false);
          if (isNew) {
            meetings.utils.handleError(meetings.i18n["bbb_err_create_meeting"]);
          } else {
            meetings.utils.handleError(meetings.i18n["bbb_err_update_meeting"]);
          }
        });
        return false;
      });

      $('#bbb_cancel').click(function (e) {

        if (!meeting.presentation && $('#fileUrl').val()) {
          $('#removeUpload').click();
        }
        $('#bbb_home_link').click();
      });

      // User warnings.
      meetings.utils.getSitePermissions(meetings.startupArgs.siteId).then(perms => {

        Object.keys(perms).forEach(role => {

          if (perms[role].indexOf("bbb.participate") === -1) {
            meetings.utils.showMessage(meetings.i18n["bbb_err_not_everyone_can_participate"]);
          }
        });
      });
    });
  } else if ('permissions' === state) {
    $("#bbb_permissions_link").parent().addClass('current');
  } else if ('joinMeeting' === state || 'meetingInfo' === state) {
    if ('joinMeeting' === state) meetings.refreshMeetingList();
    $('#bbb_recordings_link').parent().parent().hide();
    $('#bbb_end_meetings_link').parent().parent().hide();
    $('#bbb_permissions_link').parent().parent().hide();

    console.log(arg);

    if (arg && arg.meetingId) {
      const meeting = meetings.currentMeetings.find(m => m.id === arg.meetingId);

      if (meeting) {
        var groups = [];
        if (meeting.groupSessions && meetings.settings.config.addUpdateFormParameters.groupsessionsEnabled) {
          groups = await meetings.utils.getGroups(meeting);

          if (jQuery.isEmptyObject(groups)) {
            groups = [];
          }
        }

        console.log(meeting);

        const context = {
          dispNotStarted: meeting.notStarted ? 'display:inline' : 'display:none',
          dispInProgress: meeting.joinable ? 'display:inline' : 'display:none',
          dispFinished: meeting.finished ? 'display:inline' : 'display:none',
          labelJoinable: meeting.joinableMode == 'available' ? meetings.i18n["bbb_status_joinable_available"] : meeting.joinableMode == 'inprogress' ? meetings.i18n["bbb_status_joinable_inprogress"] : meeting.joinableMode == 'unavailable' ? meetings.i18n["bbb_status_joinable_unavailable"] : meeting.joinableMode == 'unreachable' ? meetings.i18n["bbb_status_joinable_unreachable"] : "",
          displayLink: meeting.joinUrl && meeting.joinable && meeting.joinableMode == 'available'? 'display:inline' : 'display:none',
          multiplesessions: meeting.multipleSessionsAllowed && meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedEnabled,
          end_meetingClass: (meeting.joinable && meeting.joinableMode == 'inprogress') ? "bbb_end_meeting_shown": "bbb_end_meeting_hidden",
          hideRecordings: !meetings.userPerms.bbbRecordingView || !meetings.settings.config.addUpdateFormParameters.recordingEnabled || !meeting.recording,
          canJoin: meeting.joinable && meetings.userPerms.bbbParticipate,
          meeting: meeting,
          timezoneoffset: meetings.startupArgs.timezoneoffset,
          groups: groups
        };

        render(BBBTemplates.meetingInfo(meetings.i18n, context), document.getElementById("bbb_content"));

        // Sort group drop-down.
        if ($('#groupSession')) {
          meetings.sortDropDown('#groupSession');
        }

        if (meeting.groupSessions) {
          $("#groupSession").change(function () {
            // Clear timeout if group sessions is changed so the meeting info page isn't updated with wrong meeting.
            clearTimeout(meetings.updateMeetingOnceTimeoutId);
            var multiplesessions = meeting.multipleSessionsAllowed && meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedEnabled;
            if (this.value != "Default") {
              $("#joinMeetingLink").attr("onclick", "return meetings.utils.joinMeeting('" + meeting.id + "', '#joinMeetingLink', " + multiplesessions + ", '" + this.value + "', '" + $('#groupSession option:selected').text() + "');");
              $("#meetingName").html(meeting.name + ' (' + $('#groupSession option:selected').text() + ')');

              meetings.utils.checkOneMeetingAvailability(meeting.id, this.value);
              meetings.utils.checkRecordingAvailability(meeting.id, this.value);
              $("#updateMeetingInfo").attr("onclick", "utils.checkOneMeetingAvailability('" + meeting.id + "', '" + this.value + "'); return false;");
              if (meetings.settings.config.autorefreshInterval.meetings > 0) {
                meetings.checkOneMeetingAvailabilityId = setInterval("utils.checkOneMeetingAvailability('" + meeting.id + "', '" + this.value + "')", meetings.settings.config.autorefreshInterval.meetings);
              }
              return;
            } else {
              $("#joinMeetingLink").attr("onclick", "return meetings.utils.joinMeeting('" + meeting.id + "', '#joinMeetingLink', " + multiplesessions + ");");
              $("#meetingName").html(meeting.name);

              meetings.utils.checkOneMeetingAvailability(meeting.id);
              meetings.utils.checkRecordingAvailability(meeting.id);
              $("#updateMeetingInfo").attr("onclick", "utils.checkOneMeetingAvailability('" + meeting.id + "');");
              if (meetings.settings.config.autorefreshInterval.meetings > 0) {
                meetings.checkOneMeetingAvailabilityId = setInterval("utils.checkOneMeetingAvailability('" + meeting.id + "')", meetings.settings.config.autorefreshInterval.meetings);
              }
              return;
            }
          });
        }

        meetings.utils.checkOneMeetingAvailability(arg.meetingId);
        meetings.utils.checkRecordingAvailability(meetings.i18n, arg.meetingId);

        if (meetings.settings.config.autorefreshInterval.meetings > 0) {
          meetings.checkOneMeetingAvailabilityId = setInterval("utils.checkOneMeetingAvailability('" + arg.meetingId + "')", meetings.settings.config.autorefreshInterval.meetings);
        }
      } else {
          meetings.utils.hideMessage();
          meetings.utils.showMessage(meetings.i18n["bbb_err_meeting_unavailable_instr"], 'warning', meetings.i18n["bbb_err_meeting_unavailable"], false);
      }
    } else {
      meetings.switchState('currentMeetings');
    }
  } else if ('recordings' === state) {
    $("#bbb_recordings_link").parent().addClass('current');

    // Show meeting list.
    if (meetings.userPerms.bbbViewMeetingList) {
      // Get recording list.
      meetings.refreshRecordingList();

      /*
      meetings.utils.render('bbb_recordings_template', {
        'recordings': meetings.currentRecordings,
        'stateFunction': 'recordings'
      }, 'bbb_content');
      */

      var $rows = $('#bbb_recording_table tbody tr');
      $('.search').keyup(function () {
        var val = $.trim($(this).val()).replace(/ +/g, ' ').toLowerCase();

        $rows.show().filter(function () {
          var text = $(this).text().replace(/\s+/g, ' ').toLowerCase();
          return !~text.indexOf(val);
        }).hide();
      });

      if ($('a.preview')) {
        var xOffset = 5;
        var yOffset = 15;

        $('a.preview').hover(function (e) {
          this.t = this.title;
          this.title = '';
          var c = (this.t != '') ? '<br/>' + this.t : '';
          $('body').append("<p id='preview'><img id='previewImage' src='" + this.href + "' alt='Full size image preview' />" + c + "</p>");
          $('#preview').css('top', (e.pageY - xOffset) + 'px').css('left', (e.pageX + yOffset) + 'px').fadeIn('fast');
        }, function () {
          this.title = this.t;
          $('#preview').remove();
        });
        $('a.preview').mousemove(function (e) {
          $('#preview').css('top', (e.pageY - xOffset) + 'px').css('left', (e.pageX + yOffset) + 'px');
        });
      }

      // Auto hide actions.
      jQuery('.recordingRow')
        .bind('mouseenter', function () {
          jQuery(this).addClass('bbb_even_row');
        })
        .bind('mouseleave', function () {
          jQuery(this).removeClass('bbb_even_row');
        });

      // Add parser for customized date format.
      $.tablesorter.addParser({
        id: "bbbRecDateTimeFormat",
        is: function (s) { return false; },
        format: function (s, table) {
          s = s.replace(/[a-zA-Z].*/g, '');
          s = s.trim();
          return $.tablesorter.formatFloat(new Date(s).getTime());
        },
        type: "numeric"
      });

      // Add sorting capabilities.
      $("#bbb_recording_table").tablesorter({
        cssHeader: 'bbb_sortable_table_header',
        cssAsc: 'bbb_sortable_table_header_sortup',
        cssDesc: 'bbb_sortable_table_header_sortdown',
        headers: {
          1: {
            sorter: false
          },
          3: {
            sorter: 'bbbRecDateTimeFormat'
          },
          4: {
            sorter: false
          }
        },
        // Sort DESC status:
        // sortList: (bbbCurrentMeetings.length > 0) ? [[2,1]] : [].
        sortList: (meetings.currentRecordings.length > 0) ? [[0, 0]] : [],
      });

      if (meetings.settings.config.autorefreshInterval.recordings > 0) {
        meetings.refreshRecordingListId = setInterval("meetings.switchState('recordings')", meetings.settings.config.autorefreshInterval.recordings);
      }
    } else {
      // Warn about lack of permissions.
      if (meetings.userPerms.siteUpdate) {
        meetings.utils.showMessage(bbb_err_no_tool_permissions_maintainer);
      } else {
        meetings.utils.showMessage(bbb_err_no_tool_permissions);
      }
      $('#bbb_content').empty();
    }
  } else if ('recordings_meeting' === state) {
    if (arg && arg.meetingId) {
      if (meetings.userPerms.bbbViewMeetingList) {
        // Get meeting list.
        meetings.refreshRecordingList(arg.meetingId, arg.groupId);

        /*
        meetings.utils.render('bbb_recordings_template', {
          'recordings': meetings.currentRecordings,
          'stateFunction': 'recordings_meeting',
          'meetingId': arg.meetingId
        }, 'bbb_content');
        */

        if ($('a.preview')) {
          var xOffset = 5;
          var yOffset = 15;

          $('a.preview').hover(function (e) {
            this.t = this.title;
            this.title = '';
            var c = (this.t != '') ? '<br/>' + this.t : '';
            $('body').append("<p id='preview'><img id='previewImage' src='" + this.href + "' alt='Full size image preview' />" + c + "</p>");
            $('#preview').css('top', (e.pageY - xOffset) + 'px').css('left', (e.pageX + yOffset) + 'px').fadeIn('fast');
          }, function () {
            this.title = this.t;
            $('#preview').remove();
          });
          $('a.preview').mousemove(function (e) {
            $('#preview').css('top', (e.pageY - xOffset) + 'px').css('left', (e.pageX + yOffset) + 'px');
          });
        }

        // Auto hide actions.
        jQuery('.recordingRow')
          .bind('mouseenter', function () {
            jQuery(this).addClass('bbb_even_row');
          })
          .bind('mouseleave', function () {
            jQuery(this).removeClass('bbb_even_row');
          });

        // Add parser for customized date format.
        $.tablesorter.addParser({
          id: "bbbRecDateTimeFormat",
          is: function (s) {
            return false;
          },
          format: function (s, table) {
            s = s.replace(/[a-zA-Z].*/g, '');
            s = s.trim();
            return $.tablesorter.formatFloat(new Date(s).getTime());
          },
          type: "numeric"
        });

        // Add sorting capabilities.
        $("#bbb_recording_table").tablesorter({
          cssHeader: 'bbb_sortable_table_header',
          cssAsc: 'bbb_sortable_table_header_sortup',
          cssDesc: 'bbb_sortable_table_header_sortdown',
          headers: {
            1: {
              sorter: false
            },
            3: {
              sorter: 'bbbRecDateTimeFormat'
            },
            4: {
              sorter: false
            }
          },
          // Sort DESC status:
          sortList: (meetings.currentRecordings.length > 0) ? [[0, 0]] : [],
        });

        if (meetings.settings.config.autorefreshInterval.recordings > 0) {
          meetings.refreshRecordingListId = setInterval("meetings.switchState('recordings_meeting',{'meetingId':'" + arg.meetingId + "'})", meetings.settings.config.autorefreshInterval.recordings);
        }
      } else {
        // Warn about lack of permissions.
        if (meetings.userPerms.siteUpdate) {
          meetings.utils.showMessage(bbb_err_no_tool_permissions_maintainer);
        } else {
          meetings.utils.showMessage(bbb_err_no_tool_permissions);
        }
        $('#bbb_content').empty();
      }
    } else {
      meetings.switchState('recordings');
    }
  }
};

meetings.addParticipantSelectionToUI = function (meeting, isNew) {

  const selOptions = meetings.utils.bbbUserSelectionOptions;
  if (isNew) {
    var defaults = selOptions['defaults'];

    // Meeting creator (default: as moderator.
    var ownerDefault = defaults['bbb.default.participants.owner'];
    if (ownerDefault != 'none') {
      meetings.addParticipantRow('user', meetings.currentUser.id, meetings.currentUser.displayName + ' (' + meetings.currentUser.displayId + ')', ownerDefault == 'moderator');
    }

    // All site participants (default: none).
    var allUsersDefault = defaults['bbb.default.participants.all_users'];
    if (allUsersDefault != 'none') {
      meetings.addParticipantRow('all', null, null, allUsersDefault == 'moderator');
    }
  } else {
    // Existing participants.
    meeting.participants.forEach(p => {

      const selectionType = p.selectionType;
      const selectionId = p.selectionId;
      const role = p.role;

      if (selectionType == 'all') {
        meetings.addParticipantRow('all', null, null, role == 'moderator');
      } else {
        let opts = null;
        if (selectionType == 'user') opts = selOptions['users'];
        if (selectionType == 'group') opts = selOptions['groups'];
        if (selectionType == 'role') opts = selOptions['roles'];

        opts.forEach(o => {

          if (o['id'] == selectionId) {
            meetings.addParticipantRow(selectionType, selectionId, o['title'], role == 'moderator');
          }
        });
      }
    });
  }
};

meetings.updateParticipantSelectionUI = async function () {

  const selOptions = await meetings.utils.getUserSelectionOptions(meetings.startupArgs.siteId);
  var selType = jQuery('#selType').val();
  jQuery('#selOption option').remove();

  if (selType == 'user' || selType == 'group' || selType == 'role') {
    var opts = null;
    if (selType == 'user') opts = selOptions['users'];
    if (selType == 'group') opts = selOptions['groups'];
    if (selType == 'role') opts = selOptions['roles'];
    opts.forEach(o => {
      jQuery('#selOption').append(`<option value="${o['id']}">${o['title']}</option>`);
    });

    $("#selOption").html($("#selOption option").sort(function (a, b) {
      return a.text == b.text ? 0 : a.text < b.text ? -1 : 1
    }));

    jQuery('#selOption').removeAttr('disabled');
  } else {
    jQuery('#selOption').attr('disabled', 'disabled');
  }
};

/** Insert a Participant row on create/edit meeting page */
meetings.addParticipantRow = function (_selType, _id, _title, _moderator) {

  var selectionType = _selType + '_' + _id;
  var selectionId = _selType + '-' + 'role_' + _id;
  var selectionTitle = null;
  if (_selType == 'all') selectionTitle = '<span class="bbb_role_selection">' + meetings.i18n["bbb_seltype_all"] + '</span>';
  if (_selType == 'group') selectionTitle = '<span class="bbb_role_selection">' + meetings.i18n["bbb_seltype_group"] + ':</span> ' + _title;
  if (_selType == 'role') selectionTitle = '<span class="bbb_role_selection">' + meetings.i18n["bbb_seltype_role"] + ':</span> ' + _title;
  if (_selType == 'user') selectionTitle = '<span class="bbb_role_selection">' + meetings.i18n["bbb_seltype_user"] + ':</span> ' + _title;
  var moderatorSelection = _moderator ? ' selected' : '';
  var attendeeSelection = _moderator ? '' : ' selected';

  var trId = 'row-' + _selType + '-' + btoa(_id).slice(0, -2);
  var trRowClass = 'row-' + _selType;
  if (jQuery('#' + trId).length == 0) {
    var row = jQuery(
      '<tr id="' + trId + '" class="' + trRowClass + '" style="display:none">' +
      '<td>' +
      '<a href="#" title="' + meetings.i18n["bbb_remove"] + '" onclick="jQuery(this).parent().parent().remove();return false"><img src="/library/image/silk/cross.png" alt="X" style="vertical-align:middle"/></a>&nbsp;' +
      selectionTitle +
      '</td>' +
      '<td>' +
      '<span class="bbb_role_selection_as">' + meetings.i18n["bbb_as_role"] + '</span>' +
      '<select name="' + selectionId + '"><option value="attendee"' + attendeeSelection + '>' + meetings.i18n["bbb_role_atendee"] + '</option><option value="moderator"' + moderatorSelection + '>' + meetings.i18n["bbb_role_moderator"] + '</option></select>' +
      '<input type="hidden" name="' + selectionType + '" value="' + _id + '"/>' +
      '</td>' +
      '</tr>');
    if (jQuery('table#selContainer tbody tr.' + trRowClass + ':last').size() > 0) {
      jQuery('table#selContainer tbody tr.' + trRowClass + ':last').after(row);
    } else {
      jQuery('table#selContainer tbody').append(row);
    }
    row.fadeIn();
  } else {
    jQuery('#' + trId).animate({
      opacity: 'hide'
    }, 'fast', function () {
      jQuery('#' + trId).animate({
        opacity: 'show'
      }, 'slow');
    });
  }
};

meetings.updateMeetingInfo = function (meeting) {

  jQuery('#bbb_meeting_info_participants_count').html('?');
  var meetingInfo = meeting;
  if (meetingInfo != null) {
    if (meetingInfo.participantCount != null && parseInt(meetingInfo.participantCount) >= 0) {
      // prepare participant count text
      var attendeeCount = meetingInfo.participantCount - meetingInfo.moderatorCount;
      var moderatorCount = meetingInfo.moderatorCount;
      var attendeeText = attendeeCount + ' ' + (attendeeCount == 1 ? bbb_meetinginfo_participants_atendee : bbb_meetinginfo_participants_atendees);
      var moderatorText = moderatorCount + ' ' + (moderatorCount == 1 ? bbb_meetinginfo_participants_moderator : bbb_meetinginfo_participants_moderators);
      // prepare participant links
      if (attendeeCount > 0) {
        var attendees = '';
        for (var p = 0; p < meetingInfo.attendees.length; p++) {
          if (meetingInfo.attendees[p].role == 'VIEWER') {
            if (attendees != '') {
              attendees += ', ' + meetingInfo.attendees[p].fullName;
            } else {
              attendees = meetingInfo.attendees[p].fullName;
            }
          }
        }
        attendeeText = '<a id="attendees" title="' + attendees + '" href="javascript:;" onclick="return false;">' + attendeeText + '</a>';
      }
      if (moderatorCount > 0) {
        var moderators = '';
        for (var p = 0; p < meetingInfo.attendees.length; p++) {
          if (meetingInfo.attendees[p].role == 'MODERATOR') {
            if (moderators != '') {
              moderators += ', ' + meetingInfo.attendees[p].fullName;
            } else {
              moderators = meetingInfo.attendees[p].fullName;
            }
          }
        }
        moderatorText = '<a id="moderators" title="' + moderators + '" href="javascript:;" onclick="return false;">' + moderatorText + '</a>';
      }
      var countText = meetingInfo.participantCount > 0 ?
          meetingInfo.participantCount + ' (' + attendeeText + ' + ' + moderatorText + ')' :
          '0';
      // update participant info & tooltip
      jQuery('#bbb_meeting_info_participants_count').html(countText);
      jQuery('#attendees, #moderators').tipTip({
        activation: 'click',
        keepAlive: 'true'
      });

      meetingInfo.attendees.forEach(a => {

        if ((!meeting.multipleSessionsAllowed || !meetings.settings.config.addUpdateFormParameters.multiplesessionsallowedEnabled) && meetings.currentUser.id === a.userID) {
          $('#meeting_joinlink_' + meeting.id).hide();
        }
      });
    } else if (meetingInfo.participantCount == null || parseInt(meetingInfo.participantCount) == -1) {
        jQuery('#bbb_meeting_info_participants_count_tr').hide();
        return;
    } else {
        jQuery('#bbb_meeting_info_participants_count').html('0');
    }
    jQuery('#bbb_meeting_info_participants_count_tr').fadeIn();
  } else {
    jQuery('#bbb_meeting_info_participants_count_tr').hide();
  }
};

meetings.refreshMeetingList = async function () {

  meetings.currentMeetings = await meetings.utils.getMeetingList(meetings.startupArgs.siteId);

  // Watch for permissions changes, check meeting dates
  meetings.currentMeetings.forEach(m => {

    meetings.utils.setMeetingPermissionParams(m);
    if (m.joinable) {
      m.joinableMode = "";
    }
    meetings.utils.setMeetingJoinableModeParams(m);

    m.formattedStartDate = m.startDate ? new Date(m.startDate).toLocaleString(portal.locale, { dateStyle: "short", timeStyle: "short" }) : "";
    m.formattedEndDate = m.endDate ? new Date(m.endDate).toLocaleString(portal.locale, { dateStyle: "short", timeStyle: "short" }): "";
    m.statusClass = m.joinable ? `status_joinable_${m.joinableMode}` : (m.notStarted ? 'bbb_status_notstarted' : 'bbb_status_finished');
    m.statusText = m.joinable ? (m.joinableMode == 'available' ? "bbb_status_joinable_available" : m.joinableMode == 'inprogress'? "bbb_status_joinable_inprogress": m.joinableMode == 'unavailable'? "bbb_status_joinable_unavailable" : m.joinableMode == 'unreachable' ? "bbb_status_joinable_unreachable" : '') : (m.notStarted ? "bbb_status_notstarted" : "bbb_status_finished");
  });
};

meetings.refreshRecordingList = function (meetingId, groupId) {

  const getRecordingResponse = (meetingId == null) ? meetings.utils.getSiteRecordingList(meetings.startupArgs.siteId) : meetings.utils.getMeetingRecordingList(meetingId, groupId);

  if (getRecordingResponse.returncode == 'SUCCESS') {
    meetings.currentRecordings = getRecordingResponse.recordings;
    meetings.currentRecordings.forEach(r => {

      let length = parseInt(r.endTime) - parseInt(r.startTime);
      r.formattedDuration = Math.round(length / 60000);

      r.formattedStartTime = r.startTime ? new Date(parseInt(r.startTime)).toLocaleString(portal.locale, { dateStyle: "short", timeStyle: "short" }) : "";
      r.ownerId = "";
      meetings.utils.setRecordingPermissionParams(r);

      let images = [];
      r.playback.forEach(p => {
        if (p.preview && p.preview.length > images.length) {
          images = p.preview;
        }
      });

      if (images.length) {
        r.images = images;
      }
    });
  } else {
    meetings.currentRecordings = [];

    if (getRecordingResponse.messageKey != null) {
      meetings.utils.showMessage(getRecordingResponse.messageKey + ":" + getRecordingResponse.message, 'warning');
    } else {
      meetings.utils.showMessage(bbb_warning_no_server_response, 'warning');
    }
  }
};

meetings.sortDropDown = function (dropDownId) {

  var defaultGroup = $(dropDownId + ' option:first');
  var groupNames = $(dropDownId + ' option:not(:first)').sort(function (a, b) {
    return a.text.toUpperCase() == b.text.toUpperCase() ? 0 : a.text.toUpperCase().localeCompare(b.text.toUpperCase());
  });
  $(dropDownId).html(groupNames).prepend(defaultGroup);
};

meetings.meetingInfoEventListener = e => {
  meetings.switchState('meetingInfo',{meetingId: e.target.dataset["meetingId"]});
};

meetings.editMeetingEventListener = e => {
  meetings.switchState("addUpdateMeeting",{meetingId: e.target.dataset["meetingId"]});
}

meetings.deleteMeetingEventListener = e => {

  meetings.utils.deleteMeeting(e.target.dataset["name"], e.target.dataset["meetingId"], meetings.i18n)
    .then(r => {

      const i = meetings.currentMeetings.findIndex(cm => meetingId === cm.id);
      meetings.currentMeetings.splice(i, 1);
      meetings.switchState('currentMeetings');
    })
    .catch(error => {
      const msg = meetings.i18n["bbb_err_end_meeting"].replace("{0}", name);
      meetings.utils.handleError(msg);
    });
};


var loadMeetings = function () {

  loadProperties("meetings").then(i18n => {

    meetings.i18n = i18n;
    meetings.loadSiteDataAndInit();
  });
};

export {loadMeetings};
