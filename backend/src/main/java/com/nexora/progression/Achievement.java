package com.nexora.progression;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievement")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_slug", length = 60)
    private String gameSlug;

    @Column(name = "slug", nullable = false, unique = true, length = 60)
    private String slug;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "badge_icon", nullable = false, length = 30)
    private String badgeIcon = "🏆";

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward = 50;

    @Column(name = "criteria_type", nullable = false, length = 60)
    private String criteriaType;

    @Column(name = "criteria_threshold", nullable = false)
    private int criteriaThreshold = 1;

    @Column(name = "rarity_tier", nullable = false, length = 20)
    private String rarityTier = "COMMON"; // COMMON, RARE, EPIC, LEGENDARY

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Achievement() {}

    public Achievement(String gameSlug, String slug, String title, String description,
                       String badgeIcon, int xpReward, String criteriaType,
                       int criteriaThreshold, String rarityTier) {
        this.gameSlug = gameSlug;
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.badgeIcon = badgeIcon;
        this.xpReward = xpReward;
        this.criteriaType = criteriaType;
        this.criteriaThreshold = criteriaThreshold;
        this.rarityTier = rarityTier;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getGameSlug() { return gameSlug; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getBadgeIcon() { return badgeIcon; }
    public String getIconUrl() { return iconUrl; }
    public int getXpReward() { return xpReward; }
    public String getCriteriaType() { return criteriaType; }
    public int getCriteriaThreshold() { return criteriaThreshold; }
    public String getRarityTier() { return rarityTier; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
