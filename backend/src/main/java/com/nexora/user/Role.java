package com.nexora.user;

import jakarta.persistence.*;

/** Maps to role (docs/DATABASE_DESIGN.md). Seeded at startup by RoleSeeder, since nothing
 *  else in the project creates seed data yet (Phase 4's catalogue seeding doesn't exist
 *  either) and registration is meaningless without at least a PLAYER row to assign. */
@Entity
@Table(name = "role")
public class Role {

    public static final String PLAYER = "PLAYER";
    public static final String ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    protected Role() { }

    public Role(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
