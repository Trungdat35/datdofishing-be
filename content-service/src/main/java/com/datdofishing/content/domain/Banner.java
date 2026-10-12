package com.datdofishing.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "banners")
@Getter
@Setter
public class Banner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(nullable = false)
    private String imageUrl;
    private String linkUrl;
    /** HOME_SLIDER, HOME_SIDE, CATEGORY_TOP... */
    private String position = "HOME_SLIDER";
    private int sortOrder;
    private boolean active = true;
}
