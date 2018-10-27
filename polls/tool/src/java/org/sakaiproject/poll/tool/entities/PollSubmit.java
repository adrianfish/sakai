package org.sakaiproject.poll.tool.entities;

import org.sakaiproject.poll.model.Poll;

import lombok.Data;

@Data
public class PollSubmit {

    /*
    public PollSubmit(Poll poll) {

        this.question = poll.getText();
        this.instructions = poll.getDescription();
        this.voteOpen = poll.getVoteOpen();
        this.voteClose = poll.getVoteClose();
        this.minOptions = poll.getMinOptions();
        this.maxOptions = poll.getMaxOptions();
        this.publicAccess = poll.getIsPublic();
        this.release = poll.getDisplayResult();
    }
    */

    private String question;
    private String instructions;
    private String voteOpen;
    private String voteClose;
    private Integer minOptions;
    private Integer maxOptions;
    private Boolean publicAccess;
    private String release;
}
