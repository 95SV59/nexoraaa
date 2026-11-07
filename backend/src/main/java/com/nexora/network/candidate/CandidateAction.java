package com.nexora.network.candidate;

import java.util.List;
import java.util.Map;

/** One proposed network-control strategy, not yet applied. Maps to candidate_action
 *  (docs/DATABASE_DESIGN.md); parameters is stored as JSON there. */
public class CandidateAction {
    public final ActionType actionType;
    public final List<String> targetElementNames;
    public final Map<String, Object> parameters;

    public CandidateAction(ActionType actionType, List<String> targetElementNames, Map<String, Object> parameters) {
        this.actionType = actionType;
        this.targetElementNames = targetElementNames;
        this.parameters = parameters;
    }

    public static CandidateAction noAction() {
        return new CandidateAction(ActionType.NO_ACTION, List.of(), Map.of());
    }
}
