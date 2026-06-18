package com.tripio.etf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "style_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StyleTag {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
