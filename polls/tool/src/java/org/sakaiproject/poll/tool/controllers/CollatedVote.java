package org.sakaiproject.poll.tool.controllers;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CollatedVote {

    private Long optionId ;
    private String optionText;
    private int votes = 0;
    private Boolean deleted;
    private String percentage;

    public void incrementVotes(){
        this.votes++;
    }
}
