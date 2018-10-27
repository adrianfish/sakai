package org.sakaiproject.collaborativemaps.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "COLLABMAP_MARKER")
@Data
public class CollaborativeMapMarker {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue
    private Long id;

    @Column(name = "MAP_ID", nullable = false)
    private Long mapId;

    @Column(name = "COLOUR", length = 7)
    private String colour;

    @Column(name = "TITLE", length = 255, nullable = false)
    private String title;

    @Column(name = "URL", length = 255)
    private String url;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IMAGE_FILENAME")
    private String imageFilename;

    @Column(name = "MAX_MARKERS")
    private Integer maxMarkers;

    @Column(name = "ALLOW_COEDITING")
    private Boolean allowCoediting;

    @Column(name = "LATITUDE", nullable = false)
    private Float latitude;

    @Column(name = "LONGITUDE", nullable = false)
    private Float longitude;

    @Column(name = "CREATOR", length = 36, nullable = false)
    private String creator;
}
