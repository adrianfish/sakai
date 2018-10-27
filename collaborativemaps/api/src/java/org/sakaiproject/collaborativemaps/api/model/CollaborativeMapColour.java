package org.sakaiproject.collaborativemaps.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "COLLABMAP_COLOUR")
@Data
public class CollaborativeMapColour {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue
    private Long id;

    @Column(name = "MAP_ID", nullable = false)
    private Long mapId;

    @Column(name = "COLOUR", length = 7)
    private String colour;

    @Column(name = "LABEL", length = 255)
    private String label;
}
