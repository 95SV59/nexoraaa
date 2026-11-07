package com.nexora.review;

import com.nexora.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "review_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"review_id", "user_id"})
})
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected ReviewLike() { }

    public ReviewLike(Review review, User user) {
        this.review = review;
        this.user = user;
    }

    public Long getId() { return id; }
    public Review getReview() { return review; }
    public User getUser() { return user; }
}
