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
import org.sakaiproject.poll.model.Vote;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Controller
public class DeleteOptionController extends PollsController {

	@GetMapping("/deleteoption")
	public String renderDeleteOption(Model model, @ModelAttribute("sakaiHtmlHead") String sakaiHtmlHead
                                    , @RequestParam(required=false) Long optionId) {

        log.debug("deleteOption");
        Option option = pollListManager.getOptionById(optionId);
        model.addAttribute("option", option);
        return "deleteoption";
    }

	@PostMapping("/deleteoption")
    public String deleteOption(Model model, @RequestParam Long optionId, @RequestParam String handleVotes) {

        Option option = pollListManager.getOptionById(optionId);
        if (handleVotes.equals("return-votes")) {
            System.out.println("return-votes");
            //hard-delete the option. It will no longer have any votes
            pollListManager.deleteOption(option);
            Set<String> userEids = new HashSet<>();
            for (Vote vote : pollVoteManager.getAllVotesForOption(option)) {
                String userId = vote.getUserId();
                if (userId != null) {
                    String userEid = externalLogic.getUserEidFromId(userId);
                    userEids.add(userEid);
                }
                pollVoteManager.deleteVote(vote);
            }

            //send the notification to affected users
            Poll poll = pollListManager.getPollById(option.getPollId());
            String siteTitle = externalLogic.getSiteTile(poll.getSiteId());
            externalLogic.notifyDeletedOption(userEids, siteTitle, poll.getText());
        } else {
            //soft delete the option. we still want it to show up in the results
            Option persistentOption = pollListManager.getOptionById(option.getOptionId());
            pollListManager.deleteOption(persistentOption, true);
        }

        model.addAttribute("poll", pollListManager.getPollById(option.getPollId()));

        return "add";
    }
}
