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
@Table(name = "CONV_POST_REACTIONS",
    uniqueConstraints = { @UniqueConstraint(name = "UniquePostReactions", columnNames = { "POST_ID", "USER_ID", "REACTION" }) },
    indexes = { @Index(name = "conv_post_reactions_post_idx", columnList = "POST_ID"),
                @Index(name = "conv_post_reactions_post_user_idx", columnList = "POST_ID, USER_ID") })
@Getter
@Setter
public class PostReaction implements PersistableEntity<Long> {

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @Column(name = "POST_ID", length = 36, nullable = false)
    private String postId;

    @Column(name = "USER_ID", length = 99, nullable = false)
    private String userId;

    @Column(name = "REACTION", nullable = false)
    private Reaction reaction;

    @Column(name = "REACTION_STATE")
    private Boolean state = Boolean.FALSE;
}
