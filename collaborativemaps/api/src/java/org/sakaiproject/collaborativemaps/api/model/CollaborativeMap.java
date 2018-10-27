package org.sakaiproject.collaborativemaps.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "COLLABMAP")
@Data
public class CollaborativeMap {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "SITE_ID", length = 99, nullable = false)
    private String siteId;

    @Column(name = "GROUP_ID", length = 99, nullable = false)
    private String groupId;

    @Column(name = "NAME", length = 255, nullable = false)
    private String name;

    @Column(name = "MIN_MARKERS")
    private Integer minMarkers;

    @Column(name = "MAX_MARKERS")
    private Integer maxMarkers;

    @Column(name = "ALLOW_COEDITING")
    private Boolean allowCoediting;

    @Column(name = "INITIAL_LATITUDE")
    private Float initialLatitude;

    @Column(name = "INITIAL_LONGITUDE")
    private Float initialLongitude;

    @Column(name = "INITIAL_ZOOM")
    private Integer initialZoom;
}
