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
package org.sakaiproject.meetings.api;

import java.util.List;

import org.sakaiproject.meetings.api.storage.Meeting;

public interface MeetingsStorageManager {

    boolean storeMeeting(Meeting meeting);
    boolean updateMeeting(Meeting meeting, boolean updateParticipants);
    List<Meeting> getSiteMeetings(String siteId, boolean includeDeleted);
    Meeting getMeeting(String meetingId);
    boolean deleteMeeting(String meetingId);
    boolean deleteMeeting(String meetingId, boolean fullDelete);
    String getMeetingHost(String meetingId);
    List<Meeting> getAllMeetings();
    boolean setMeetingHost(String meetingId, String hostUrl);
}
