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
@Table(name = "CONV_POST_REACTION_TOTALS",
    indexes = { @Index(name = "conv_post_reaction_totals_post_idx", columnList = "POST_ID") },
    uniqueConstraints = { @UniqueConstraint(name = "UniquePostReactionTotals", columnNames = { "POST_ID", "REACTION" }) })
@Getter
@Setter
public class PostReactionTotal implements PersistableEntity<Long> {

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @Column(name = "POST_ID", length = 36, nullable = false)
    private String postId;

    @Column(name = "REACTION", nullable = false)
    private Reaction reaction;

    @Column(name = "TOTAL", nullable = false)
    private Integer total;
}
