package org.sakaiproject.discussions.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "DISCUSSIONS")
public class Discussion {

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @Column(name = "ENTITY_REF", length = 255, nullable = false)
    private String entityRef;

    @Column(name = "MESSAGE", nullable = false)
    private String message;
}
