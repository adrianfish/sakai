/****************************************************************************** 
 * Copyright 2015 sakaiproject.org Licensed under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://opensource.org/licenses/ECL-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.sakaiproject.poll.tool.controllers;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.poll.logic.ExternalLogic;
import org.sakaiproject.poll.logic.PollListManager;
import org.sakaiproject.poll.logic.PollVoteManager;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;

import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

public class PollsController {

    @Resource(name = "org.sakaiproject.poll.logic.ExternalLogic")
    ExternalLogic externalLogic;

    @Resource(name = "org.sakaiproject.poll.logic.PollListManager")
    PollListManager pollListManager;

    @Resource(name = "org.sakaiproject.tool.poll.api.PollVoteManager")
    PollVoteManager pollVoteManager;

    @Resource(name = "org.sakaiproject.tool.api.SessionManager")
    SessionManager sessionManager;

    @Resource(name = "org.sakaiproject.tool.api.ToolManager")
    ToolManager toolManager;

    @ModelAttribute(name="sakaiHtmlHead")
    public String getSakaiHtmlHead(HttpServletRequest request) {
        return (String) request.getAttribute("sakai.html.head");
    }
}
