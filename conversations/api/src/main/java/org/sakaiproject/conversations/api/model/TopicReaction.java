package org.sakaiproject.conversations.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.sakaiproject.conversations.api.Reaction;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CONV_TOPIC_REACTIONS",
    uniqueConstraints = { @UniqueConstraint(name = "UniqueTopicReactions", columnNames = { "TOPIC_ID", "USER_ID", "REACTION" }) },
    indexes = { @Index(name = "conv_topic_reactions_topic_user_idx", columnList = "TOPIC_ID, USER_ID"),
                @Index(name = "conv_topic_reactions_topic_idx",columnList = "TOPIC_ID") })
@Getter
@Setter
public class TopicReaction implements PersistableEntity<Long> {

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @Column(name = "TOPIC_ID", length = 36, nullable = false)
    private String topicId;

    @Column(name = "USER_ID", length = 99, nullable = false)
    private String userId;

    @Column(name = "REACTION", nullable = false)
    private Reaction reaction;

    @Column(name = "REACTION_STATE")
    private Boolean state = Boolean.FALSE;
}
