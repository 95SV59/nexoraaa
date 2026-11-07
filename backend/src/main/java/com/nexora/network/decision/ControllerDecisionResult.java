package com.nexora.network.decision;

import com.nexora.network.candidate.CandidateAction;
import com.nexora.network.whatif.WhatIfResult;

import java.util.List;

/** Maps to controller_decision (docs/DATABASE_DESIGN.md). */
public class ControllerDecisionResult {
    public final CandidateAction selected;
    public final boolean confidenceGatePassed;
    public final String reason;
    public final List<WhatIfResult> allEvaluations;

    public ControllerDecisionResult(CandidateAction selected, boolean confidenceGatePassed,
                                     String reason, List<WhatIfResult> allEvaluations) {
        this.selected = selected;
        this.confidenceGatePassed = confidenceGatePassed;
        this.reason = reason;
        this.allEvaluations = allEvaluations;
    }
}
