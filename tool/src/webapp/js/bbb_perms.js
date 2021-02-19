/**
 * Copyright (c) 2010 onwards - The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export class BBBPermissions {

  constructor(data) {

    if (data) {
      data.forEach(p => {

        switch (p) {
          case "bbb.admin":
            this.bbbAdmin = true;
            break;
          case "bbb.create":
            this.bbbCreate = true;
            break;
          case "bbb.edit.own":
            this.bbbEditOwn = true;
            this.bbbViewMeetingList = true;
            break;
          case "bbb.edit.any":
            this.bbbEditAny = true;
            this.bbbViewMeetingList = true;
            break;
          case "bbb.delete.own":
            this.bbbDeleteOwn = true;
            this.bbbViewMeetingList = true;
            break;
          case "bbb.delete.any":
            this.bbbDeleteAny = true;
            this.bbbViewMeetingList = true;
            break;
          case "bbb.participate":
            this.bbbParticipate = true;
            this.bbbViewMeetingList = true;
            break;
          case "bbb.recording.view":
            this.bbbRecordingView = true;
            break;
          case "bbb.recording.edit.own":
            this.bbbRecordingEditOwn = true;
            break;
          case "bbb.recording.edit.any":
            this.bbbRecordingEditAny = true;
            break;
          case "bbb.recording.delete.own":
            this.bbbRecordingDeleteOwn = true;
            break;
          case "bbb.recording.delete.any":
            this.bbbRecordingDeleteAny = true;
            break;
          case "bbb.recording.extendedformats.own":
            this.bbbRecordingExtendedFormatsOwn = true;
            break;
          case "bbb.recording.extendedformats.any":
            this.bbbRecordingExtendedFormatsAny = true;
            break;
          case "site.upd":
            this.siteUpdate = true;
            break;
          case "site.viewRoster":
            this.siteViewRoster = true;
            break;
          case "calendar.new":
            this.calendarNew = true;
            break;
          case "calendar.revise.own":
            this.calendarReviseOwn = true;
            break;
          case "calendar.revise.any":
            this.calendarReviseAny = true;
            break;
          case "calendar.delete.own":
            this.calendarDeleteOwn = true;
            break;
          case "calendar.delete.any":
            this.calendarDeleteAny = true;
            break;
          default:
        }
      });
    }
  }
};
