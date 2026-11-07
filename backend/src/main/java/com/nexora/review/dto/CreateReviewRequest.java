package com.nexora.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @NotBlank(message = "Review body cannot be empty")
    private String body;

    private Boolean recommended;
    private Boolean spoiler;

    public CreateReviewRequest() { }

    public CreateReviewRequest(Integer rating, String body, Boolean recommended, Boolean spoiler) {
        this.rating = rating;
        this.body = body;
        this.recommended = recommended;
        this.spoiler = spoiler;
    }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Boolean getRecommended() { return recommended != null ? recommended : (rating != null && rating >= 3); }
    public void setRecommended(Boolean recommended) { this.recommended = recommended; }
    public Boolean getSpoiler() { return Boolean.TRUE.equals(spoiler); }
    public void setSpoiler(Boolean spoiler) { this.spoiler = spoiler; }
}
