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

import org.sakaiproject.poll.model.Option;
import org.sakaiproject.poll.model.Poll;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Controller
public class AddPollController extends PollsController {

	@GetMapping("/add")
	public String addOrEditPoll(Model model, @ModelAttribute("sakaiHtmlHead") String sakaiHtmlHead
                                    , @RequestParam(required=false) Long pollId) {

		log.debug("addOrEditPoll");
		log.debug("pollId: {}", pollId);
        Poll poll = (pollId != null) ? pollListManager.getPollById(pollId, true) : new Poll();
        model.addAttribute("poll", poll);
        model.addAttribute("active", "add");
        model.addAttribute("voteOpen", poll.getVoteOpen());
        model.addAttribute("voteClose", poll.getVoteClose());
	    return "add";
	}

	@PostMapping("/add")
	public String savePoll(Model model, @ModelAttribute Poll poll
                                , @RequestParam String action
                                , @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") Date voteOpen
                                , @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") Date voteClose) {

		log.debug("savePoll");

        if (action.equals("cancel")) {
	        return "redirect:/index";
        }

        poll.setVoteOpen(voteOpen);
        poll.setVoteClose(voteClose);

        poll.setOwner(sessionManager.getCurrentSessionUserId());
        poll.setSiteId(toolManager.getCurrentPlacement().getContext());

        pollListManager.savePoll(poll);

        if (action.equals("saveandadd")) {
            model.addAttribute("poll", poll);
            Option option = new Option();
            option.setPollId(poll.getPollId());
            model.addAttribute("option", option);
            return "addoption";
        } else {
	        return "redirect:/index";
        }
	}
}
