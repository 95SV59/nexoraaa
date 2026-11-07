import { api } from "./api";
import type { BatchEvaluationResult, ComparisonReport, SessionListItem, SessionSummary } from "../types/experiments";

/** Phase 20 — Experimental Evaluation REST client. */
export const experimentService = {
  listSessions: (limit = 20) =>
    api.get<SessionListItem[]>("/analysis/sessions", { params: { limit } }).then((r) => r.data),

  summarize: (sessionId: number) =>
    api.get<SessionSummary>(`/analysis/sessions/${sessionId}/summary`).then((r) => r.data),

  compare: (baselineSessionId: number, proposedSessionId: number) =>
    api
      .get<ComparisonReport>("/analysis/compare", { params: { baselineSessionId, proposedSessionId } })
      .then((r) => r.data),

  downloadSessionCsv: (sessionId: number) => {
    window.open(`/api/analysis/sessions/${sessionId}/export/csv`, "_blank");
  },

  downloadComparisonCsv: (baselineSessionId: number, proposedSessionId: number) => {
    window.open(`/api/analysis/compare/export/csv?baselineSessionId=${baselineSessionId}&proposedSessionId=${proposedSessionId}`, "_blank");
  },

  runBatchEvaluation: (req?: { predictionModel?: string; profiles?: string[]; durationSec?: number }) =>
    api.post<BatchEvaluationResult>("/analysis/batch-run", req || {}).then((r) => r.data),
};
