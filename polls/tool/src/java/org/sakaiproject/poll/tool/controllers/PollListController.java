/**********************************************************************************
 * $URL: $
 * $Id:  $
 ***********************************************************************************
 *
 * Copyright (c) 2006, 2007, 2008, 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.poll.tool.controllers;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.poll.logic.ExternalLogic;
import org.sakaiproject.poll.logic.PollListManager;
import org.sakaiproject.poll.logic.PollVoteManager;
import org.sakaiproject.poll.model.Option;
import org.sakaiproject.poll.model.Poll;
import org.sakaiproject.poll.model.Vote;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.format.annotation.DateTimeFormat;

@Slf4j
@Controller
public class PollListController extends PollsController {

	@GetMapping(value = {"/", "/index"})
	public String viewPolls(Model model) {

        log.debug("listPolls");
        List<Poll> polls = pollListManager.findAllPolls(toolManager.getCurrentPlacement().getContext());

        List<Poll> votablePolls = new ArrayList<>();
        List<Poll> nonVotablePolls = new ArrayList<>();

        for (Poll poll : polls) {
            if (pollVoteManager.pollIsVotable(poll)) votablePolls.add(poll); else nonVotablePolls.add(poll);
        }
        model.addAttribute("votablePolls", votablePolls);
        model.addAttribute("nonVotablePolls", nonVotablePolls);
        model.addAttribute("active", "index");
        model.addAttribute("hasPolls", votablePolls.size() > 0 || nonVotablePolls.size() > 0);

	    return "index";
	}

    @PostMapping(value = {"/", "/index"}, params = "delete")
	public String removePolls(Model model, @RequestParam List<Long> pollIds) {

		log.debug("removePolls");

        for (Long pollId : pollIds) {
            pollListManager.deletePoll(pollListManager.getPollById(pollId));
        }

	    return "redirect:/index";
	}

    @PostMapping(value = {"/", "/index"}, params = "reset")
	public String resetPolls(Model model, @RequestParam List<Long> pollIds) {

		log.debug("resetPolls");

        for (Long pollId : pollIds) {
            Poll poll = pollListManager.getPollById(pollId);
            if (pollListManager.userCanDeletePoll(poll)) {
                pollVoteManager.deleteAll(pollVoteManager.getAllVotesForPoll(poll));
            }
        }

	    return "redirect:/index";
	}
}
