package org.sakaiproject.pages.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import org.sakaiproject.springframework.data.PersistableEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PAGES_PAGE")
@Setter
public class PagesPage implements PersistableEntity<String> {

    @Id
    @Column(name = "ID", nullable = false, length = 36)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Getter
    private String id;

    @Column(name = "TITLE", nullable = false, length = 255)
    private String title;

    @Column(name = "CONTENTS", nullable = false, length = 65535)
    @Lob
    private String contents;

    @Column(name = "PARENT_PAGE_ID", length = 36)
    private String parentPageId;
}
