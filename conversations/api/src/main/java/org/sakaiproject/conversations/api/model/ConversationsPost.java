package org.sakaiproject.conversations.api.model;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CONV_POSTS", indexes = { @Index(name = "conv_posts_topic_idx", columnList = "TOPIC_ID"),
                                        @Index(name = "conv_posts_topic_creator_idx", columnList = "TOPIC_ID, CREATOR"),
                                        @Index(name = "conv_posts_site_idx", columnList = "SITE_ID"),
                                        @Index(name = "conv_posts_parent_post_idx", columnList = "PARENT_POST_ID"),
                                        @Index(name = "conv_posts_parent_thread_idx", columnList = "PARENT_THREAD_ID") })
@Getter
@Setter
public class ConversationsPost implements PersistableEntity<String> {

    @Id
    @Column(name = "POST_ID", length = 36, nullable = false)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @Column(name = "TOPIC_ID", length = 36, nullable = false)
    private String topicId;

    @Column(name = "PARENT_POST_ID", length = 36)
    private String parentPostId;

    // This holds the oldest ancestor, the thread starter, in this thread of posts
    @Column(name = "PARENT_THREAD_ID", length = 36)
    private String parentThreadId;

    @Column(name = "SITE_ID", length = 99, nullable = false)
    private String siteId;

    @Lob
    @Column(name = "MESSAGE", nullable = false)
    private String message;

    @Column(name = "NUMBER_OF_COMMENTS")
    private Integer numberOfComments = 0;

    // This is only used in a thread context, ie when a post is a top level
    // reply to a topic
    @Column(name = "NUMBER_OF_THREAD_REPLIES")
    private Integer numberOfThreadReplies = 0;

    @Column(name = "NUMBER_OF_THREAD_REACTIONS")
    private Integer numberOfThreadReactions = 0;

    @Column(name = "DEPTH")
    private Integer depth = 1;

    @Column(name = "HOW_ACTIVE")
    private Integer howActive = 0;

    @Column(name = "DRAFT")
    private Boolean draft = Boolean.FALSE;

    @Column(name = "HIDDEN")
    private Boolean hidden = Boolean.FALSE;

    @Column(name = "LOCKED")
    private Boolean locked = Boolean.FALSE;

    @Column(name = "UPVOTES")
    private Integer upvotes = 0;

    @Column(name = "PRIVATE_POST")
    private Boolean privatePost = Boolean.FALSE;

    @Column(name = "ANONYMOUS")
    private Boolean anonymous = Boolean.FALSE;

    @Embedded
    private Metadata metadata;
}
