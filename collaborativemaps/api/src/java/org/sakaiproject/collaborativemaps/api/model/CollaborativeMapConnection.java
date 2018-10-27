package org.sakaiproject.collaborativemaps.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "COLLABMAP_CONNECTION")
@Data
public class CollaborativeMapConnection {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue
    private Long id;

    @Column(name = "FROM_MARKER", nullable = false)
    private Integer fromMarker;

    @Column(name = "TO_MARKER", nullable = false)
    private Integer toMarker;

    @Column(name = "CREATOR", length = 36, nullable = false)
    private String creator;
}
