package org.sakaiproject.conversations.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.time.Instant;

import lombok.Getter;

@Entity
@Table(name = "CONV_USER_STATISTICS", indexes = @Index(name = "conv_user_stats_user_idx", columnList = "USER_ID"))
@Getter
public class UserStatistics {

    @Id
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "USER_ID", length = 99, nullable = false)
    private String userId;

    @Column(name = "TOPIC_ID", length = 36)
    private String topicId;

    @Column(name = "NUMBER_OF_POSTS")
    private Integer numberOfPosts = 0;

    @Column(name = "LAST_POST_DATE")
    private Instant lastPostDate;

    @Column(name = "NUMBER_OF_UPVOTES")
    private Integer numberOfUpvotes = 0;

    @Column(name = "NUMBER_OF_REACTIONS")
    private Integer numberOfReactions = 0;

    @Column(name = "NUMBER_OF_REPLIES")
    private Integer numberOfReplies = 0;

    @Column(name = "NUMBER_READ")
    private Integer numberRead = 0;

}
