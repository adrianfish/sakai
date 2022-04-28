package org.sakaiproject.conversations.api.model;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CONV_COMMENTS", indexes = { @Index(name = "conv_comments_post_idx", columnList = "POST_ID"),
                                        @Index(name = "conv_comments_site_idx", columnList = "SITE_ID"),
                                        @Index(name = "conv_comments_topic_idx", columnList = "TOPIC_ID") })
@Getter
@Setter
public class ConversationsComment implements PersistableEntity<String> {

    @Id
    @Column(name = "COMMENT_ID", length = 36, nullable = false)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @Column(name = "SITE_ID", length = 99, nullable = false)
    private String siteId;

    @Column(name = "POST_ID", length = 36, nullable = false)
    private String postId;

    @Column(name = "TOPIC_ID", length = 36, nullable = false)
    private String topicId;

    @Column(name = "MESSAGE", length = 255, nullable = false)
    private String message;

    @Column(name = "LOCKED")
    private Boolean locked = Boolean.FALSE;

    @Embedded
    private Metadata metadata;
}
