package com.nexora.game;

import jakarta.persistence.*;

/** Maps to genre (docs/DATABASE_DESIGN.md). */
@Entity
@Table(name = "genre")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    protected Genre() { }

    public Genre(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
}
