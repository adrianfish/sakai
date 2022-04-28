package org.sakaiproject.conversations.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.time.Instant;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CONV_POST_STATUS",
    indexes = { @Index(name = "conv_post_status_user_idx", columnList = "USER_ID"),
                @Index(name = "conv_post_status_post_idx", columnList = "POST_ID"),
                @Index(name = "conv_post_status_post_user_idx", columnList = "POST_ID, USER_ID"),
                @Index(name = "conv_post_status_topic_user_idx", columnList = "TOPIC_ID, USER_ID") },
    uniqueConstraints = { @UniqueConstraint(name = "UniquePostStatus", columnNames = { "POST_ID", "USER_ID" }) })
@Getter
@Setter
public class PostStatus implements PersistableEntity<Long> {

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @Column(name = "TOPIC_ID", length = 36, nullable = false)
    private String topicId;

    @Column(name = "POST_ID", length = 36, nullable = false)
    private String postId;

    @Column(name = "USER_ID", length = 99, nullable = false)
    private String userId;

    @Column(name = "VIEWED_DATE")
    private Instant viewedDate;

    @Column(name = "VIEWED")
    private Boolean viewed = Boolean.FALSE;

    @Column(name = "UPVOTED")
    private Boolean upvoted = Boolean.FALSE;

    public PostStatus() {
    }

    public PostStatus(String topicId, String postId, String userId) {

        this.topicId = topicId;
        this.postId = postId;
        this.userId = userId;
    }
}
