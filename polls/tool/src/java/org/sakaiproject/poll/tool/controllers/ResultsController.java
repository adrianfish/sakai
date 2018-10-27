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
public class ResultsController extends PollsController {

	@GetMapping("/results")
    public String viewResults(Model model, @RequestParam Long pollId) {

		Poll poll = pollListManager.getPollById(pollId);

		if (!pollListManager.isAllowedViewResults(poll, externalLogic.getCurrentUserId())) {
	        return "redirect:/index";
		}

		//get the number of votes
		int voters = pollVoteManager.getDisctinctVotersForPoll(poll);
		if (poll.getMaxOptions() > 1) {
            model.addAttribute("distinctVotes", voters);
        }

		log.debug("{} have voted on this poll", voters);

        model.addAttribute("poll", poll);

		List<Option> pollOptions = poll.getOptions();

		log.debug("Got a list of {} options", pollOptions.size());
		//Append an option for no votes
		if (poll.getMinOptions()==0) {
			Option noVote = new Option(0L);
			//noVote.setOptionText(messageLocator.getMessage("result_novote"));
			noVote.setText("No options selected");
			noVote.setPollId(poll.getPollId());
			pollOptions.add(noVote);
		}

		List<Vote> votes = pollVoteManager.getAllVotesForPoll(poll);
		int totalVotes= votes.size();
		log.debug("Got {} votes", totalVotes);
        NumberFormat nf = NumberFormat.getPercentInstance();
		nf.setMinimumFractionDigits(0);
		List<CollatedVote> collatedVotes = new ArrayList<>();

		for (Option option : pollOptions) {
			CollatedVote cv = new CollatedVote();
			log.debug("Collating option {} ...", option.getOptionId());
			cv.setOptionId(option.getOptionId());
			cv.setOptionText(option.getText());
			cv.setDeleted(option.getDeleted());
			for (Vote vote : votes) {
				if (vote.getPollOption().equals(option.getOptionId())){
					log.debug("Got a vote for option {}", option.getOptionId());
					cv.incrementVotes();
				}
			}
			String optionText = cv.getOptionText();
			if (cv.getDeleted()) {
				//optionText += messageLocator.getMessage("deleted_option_tag_html");
				cv.setOptionText(optionText + "&nbsp;<i>(deleted)</i>");
			}
            double percent = 0.0;
			if (totalVotes > 0  && poll.getMaxOptions() == 1) {
				percent = ((double)cv.getVotes()/(double)totalVotes); //*(double)100;
            } else if (totalVotes > 0  && poll.getMaxOptions() > 1) {
				percent = ((double)cv.getVotes()/(double)voters); //*(double)100;
            } else {
				percent = 0.0;
            }
			cv.setPercentage(nf.format(percent));
			collatedVotes.add(cv);
		}

        model.addAttribute("collatedVotes", collatedVotes);

		//UIBranchContainer adefault = UIBranchContainer.make(tofill,"answers-default:");
		//adefault.decorators = new DecoratorList(new UITooltipDecorator(messageLocator.getMessage("results_answers_default_tooltip")));
		
		//output the votes
        /*
		Map<Long,String> chartTextData = new LinkedHashMap<>();
		Map<Long,String> chartValueData = new LinkedHashMap<>();
		for (CollatedVote cv : collation) {

			//setup chartdata, use percentages for the values
			//also, remove the &nbsp; from the beginning of the label, POLL-139
			//we use the same number formatter which adds a % to the end of the data, remove that as well.
			chartTextData.put(cv.getoptionId(), StringUtils.removeStart(optionText, "&nbsp;"));
			chartValueData.put(cv.getoptionId(), StringUtils.removeEnd(nf.format(percent), "%"));
		}
        */
        model.addAttribute("votesTotal", totalVotes);
		if (totalVotes > 0 && poll.getMaxOptions() == 1) {
            model.addAttribute("totalPercent", "100%");
        }
		model.addAttribute("chartEnabled", externalLogic.isResultsChartEnabled() && totalVotes > 0);
		
		/** CHART **/
        /*
		if(externalLogic.isResultsChartEnabled() && totalVotes > 0) {
			
			//chart selector label
			UIOutput.make(tofill,"chart-type-label",messageLocator.getMessage("results_chart_type"));

			//chart selector - no binding, JQuery handles it.
			String[] chartTypes = new String[]{"bar","pie"};
			UISelect min = UISelect.make(tofill,"chart-type",chartTypes,"null","bar");
			
			//setup bar chart
			//data separator is |
			StringBuilder sbBar = new StringBuilder();
			sbBar.append("https://chart.googleapis.com/chart?");
			sbBar.append("cht=bvg&");
			sbBar.append("chxt=y&");
			sbBar.append("chs=500x400&");
			sbBar.append("chd=t:" + StringUtils.join(chartValueData.values(),'|') + "&");
			sbBar.append("chdl=" + StringUtils.join(chartTextData.values(),'|') + "&");
			sbBar.append("chco=FF0000,00FF00,0000FF,FFFF00,00FFFF,FF00FF,C0C0C0,800080,000080,808000,800000,FF00FF,008080,800000,008000");
			
			UILink barChart = UILink.make(tofill,"poll-chart-bar",sbBar.toString());
			log.debug("bar chart URL:" + sbBar.toString());
		
			//setup pie chart
			//data separator is ,
			StringBuilder sbPie = new StringBuilder();
			sbPie.append("https://chart.googleapis.com/chart?");
			sbPie.append("cht=p&");
			sbPie.append("chs=500x400&");
			sbPie.append("chd=t:" + StringUtils.join(chartValueData.values(),',') + "&");
			sbPie.append("chl=" + StringUtils.join(chartTextData.values(),'|') + "&");
			sbPie.append("chco=FF0000,00FF00,0000FF,FFFF00,00FFFF,FF00FF,C0C0C0,800080,000080,808000,800000,FF00FF,008080,800000,008000");
			
			UILink pieChart = UILink.make(tofill,"poll-chart-pie",sbPie.toString());
			log.debug("pie chart URL:" + sbPie.toString());
			
			//refresh link
			UIInternalLink resultsLink =  UIInternalLink.make(tofill, "results-refresh", messageLocator.getMessage("action_refresh_results"), new PollViewParameters(ResultsProducer.VIEW_ID, poll.getPollId().toString()));
			resultsLink.decorators = new DecoratorList(new UITooltipDecorator(messageLocator.getMessage("action_refresh_results")+ ":" + poll.getText()));
		}
        */

		externalLogic.postEvent("poll.viewResult", "poll/site/" + externalLogic.getCurrentLocationId() +"/poll/" +  poll.getPollId(), false);

        return "results";
	}
}
