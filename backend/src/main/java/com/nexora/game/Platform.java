package com.nexora.game;

import jakarta.persistence.*;

/** Maps to platform (docs/DATABASE_DESIGN.md). */
@Entity
@Table(name = "platform")
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    protected Platform() { }

    public Platform(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
}
