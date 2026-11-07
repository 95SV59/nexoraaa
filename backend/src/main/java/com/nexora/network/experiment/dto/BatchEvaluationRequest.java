package com.nexora.network.experiment.dto;

import java.util.List;

public class BatchEvaluationRequest {
    private String predictionModel = "LINEAR_TREND";
    private List<String> profiles = List.of("EXCELLENT", "GOOD", "MODERATE", "POOR", "UNSTABLE");
    private int durationSec = 60;

    public BatchEvaluationRequest() {}

    public BatchEvaluationRequest(String predictionModel, List<String> profiles, int durationSec) {
        this.predictionModel = predictionModel;
        this.profiles = profiles;
        this.durationSec = durationSec;
    }

    public String getPredictionModel() {
        return predictionModel;
    }

    public void setPredictionModel(String predictionModel) {
        this.predictionModel = predictionModel;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(int durationSec) {
        this.durationSec = durationSec;
    }
}
