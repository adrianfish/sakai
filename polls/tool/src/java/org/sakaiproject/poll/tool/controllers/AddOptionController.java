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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
public class AddOptionController extends PollsController {

	@GetMapping("/addoption")
	public String addOrEditOption(Model model, @ModelAttribute("sakaiHtmlHead") String sakaiHtmlHead
                                    , @RequestParam(required=false) Long pollId
                                    , @RequestParam(required=false) Long optionId) {

        log.debug("addOrEditOption");

		log.debug("POLLID: {}", pollId);
		log.debug("OPTIONID: {}", optionId);

        Option option = null;
        if (optionId == null) {
            option = new Option();
            option.setPollId(pollId);
        } else {
            option = pollListManager.getOptionById(optionId);
        }

        model.addAttribute("poll", pollListManager.getPollById(pollId));

        model.addAttribute("option", option);
        model.addAttribute("active", "add");

	    return "addoption";
    }

	@PostMapping("/addoption")
	public String saveOption(Model model, @ModelAttribute Option option, @RequestParam String action) {

        System.out.println("SAVEOPTION");
        log.debug("saveOption");

        pollListManager.saveOption(option);

        model.addAttribute("active", "add");

        if (action.equals("saveandadd")) {
            model.addAttribute("poll", pollListManager.getPollById(option.getPollId()));
            Option anotherOption = new Option();
            anotherOption.setPollId(option.getPollId());
            model.addAttribute("option", anotherOption);
            return "addoption";
        } else {
            model.addAttribute("poll", pollListManager.getPollById(option.getPollId()));
            return "add";
        }
    }
}
