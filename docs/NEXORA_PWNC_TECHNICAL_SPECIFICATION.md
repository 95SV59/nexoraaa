# NEXORA — PREDICTIVE WHAT-IF NETWORK CONTROLLER (PWNC)
## Technical and Mathematical Specification

**Document Version:** 1.1.0-PROD  
**Invention:** Predictive What-If Network Control for Real-Time Multiplayer Gaming  
**Abbreviation:** PWNC  
**Platform Integration:** NEXORA Multiplayer Engine & Gaming Hub  
**Date:** November 2026  

---

## 1. System Overview and Closed-Loop Data Flow

PWNC replaces traditional reactive network throttling with an anticipatory, constraint-aware, closed-loop control system:

$$\text{Telemetry (200)} \longrightarrow \text{Forecasting (201)} \longrightarrow \text{Consistency Risk (202)} \longrightarrow \text{Candidate Generation (203)} \longrightarrow \text{Virtual Queue Sandbox (204)} \longrightarrow \text{Optimization (205)} \longrightarrow \text{Confidence Gate (206)} \longrightarrow \text{Live Control (207)} \longrightarrow \text{Outcome Feedback (208)}$$

### 1.1 Architectural Modules & Responsibilities
| Numeral | Module | Class | Mathematical & System Responsibility |
|---|---|---|---|
| **101** | Telemetry Monitor | `TelemetryCollector.java` | Server-timestamped RTT, EWMA jitter, probe loss calculation. |
| **102** | Network Predictor | `NetworkPredictor.java` | Multi-model time-series forecasting (Linear Trend, EWMA, Persistence) with bias correction. |
| **103** | Consistency Engine | `ConsistencyEngine.java` | Element-level spatial and temporal divergence estimation against critical limits. |
| **104** | Candidate Generator | `CandidateGenerator.java` | Strategy synthesis ($a \in \mathcal{A}$) targeting identified state element risks. |
| **105** | Virtual Queue Sandbox | `VirtualQueueSimulator.java` | In-memory discrete-event queue simulation modeling buffer backlog and delays. |
| **106** | Decision Controller | `DecisionController.java` | Stage A hard feasibility filtering + Stage B cost optimization subject to confidence gate ($\ge 0.80$) and dwell hysteresis. |
| **107** | Live Network Controller | `BroadcastRateRegistry.java`, `GameLoopManager.java` | Per-client and per-room broadcast scheduling without altering 30 Hz simulation. |
| **108** | Feedback Engine | `FeedbackService.java` | Error tracking at horizon expiry, updating rolling exponential error bias. |

---

## 2. Mathematical Formulations & Dimensional Consistency

### 2.1 Telemetry Metric Ingestion (Stage 200 / Numeral 101)
- Let $T_{\text{ping}}$ be the server dispatch timestamp and $T_{\text{pong}}$ be the server arrival timestamp of client pong:
  $$\text{RTT}_k = T_{\text{pong}} - T_{\text{ping}} \quad [\text{ms}]$$
- Jitter is updated via RFC 3550 EWMA filtering ($\beta = 0.125$):
  $$J_k = (1 - \beta) J_{k-1} + \beta \left| \text{RTT}_k - \text{RTT}_{k-1} \right| \quad [\text{ms}]$$
- Application-level probe loss rate over sliding window $W$ ($W = 15$):
  $$L_k = \frac{\sum_{i=0}^{W-1} \mathbb{I}(\text{probe } i \text{ timed out})}{W} \times 100 \quad [\%]$$

### 2.2 Time-Series Forecasting Models (Stage 201 / Numeral 102)
For prediction horizon $h \in [200, 1000]\text{ ms}$:
1. **Ordinary Least Squares (OLS) Linear Trend:**
   $$\hat{y}_{t+h} = \bar{y} + m \cdot (N + h - \bar{x}) + \Delta_{\text{bias}} \quad [\text{ms}]$$
   where slope $m = \frac{\sum_{i=1}^N (x_i - \bar{x})(y_i - \bar{y})}{\sum_{i=1}^N (x_i - \bar{x})^2}$.
2. **EWMA Smoothing:**
   $$\hat{y}_{t+h} = \alpha y_t + (1 - \alpha) \hat{y}_{t-1} + \Delta_{\text{bias}} \quad [\text{ms}]$$
3. **Persistence Baseline:**
   $$\hat{y}_{t+h} = y_t + \Delta_{\text{bias}} \quad [\text{ms}]$$
4. **Prediction Uncertainty & Confidence Gate:**
   $$\text{RMSE} = \sqrt{\frac{1}{N}\sum_{i=1}^N (y_i - \hat{y}_i)^2}, \quad \text{Confidence} = \max\left(0.1, 1.0 - \frac{\text{RMSE}}{\bar{y} + 1.0}\right)$$

### 2.3 State Consistency & Spatial Divergence (Stage 202 / Numeral 103)
State elements $e \in \mathcal{E}$ are classified into:
- $\text{CRITICAL}$ (scores, health, flags): $D_e^{\max} = 0\text{ px}$, $S_e^{\max} = 100\text{ ms}$.
- $\text{INTERPOLATED}$ (position, aim, ball): $D_e^{\max} = 25\text{ px}$, $S_e^{\max} = 250\text{ ms}$.
- $\text{COSMETIC}$ (particles, animations): $D_e^{\max} = \infty$, $S_e^{\max} = 1000\text{ ms}$.

Projected divergence at future time $t + h$:
$$D_e(t+h) = v_e \cdot (t + h - \tau_e) \quad [\text{px}]$$
where $v_e$ is current velocity $[\text{px/ms}]$ and $\tau_e$ is timestamp of last dispatch.

### 2.4 Discrete-Event Virtual Queue Model (Stage 204 / Numeral 105)
Physical Units:
- Message generation: $S_{\text{base}} = 128\text{ bytes/element}$.
- Service rate: $\mu = \text{effective\_rate} \times S_{\text{base}} \quad [\text{B/s}]$.
- Per-tick drain capacity: $\mu_{\text{tick}} = \mu \times \frac{\Delta t}{1000} \quad [\text{B/tick}]$, where $\Delta t = 33.3\text{ ms}$.
- Queue dynamics over simulation ticks $k = 1, \dots, K$:
  $$Q_{k+1}(a) = \max\left(0, Q_k(a) + (\lambda_k(a) - \mu_{\text{tick}})\right) \quad [\text{bytes}]$$
- Average Queuing Delay:
  $$d_{\text{queue}}(a) = \frac{\bar{Q}(a)}{\mu} \times 1000 \quad [\text{ms}]$$
- Simulated Total Element Staleness:
  $$S_e(a) = (K - k_e) \cdot \Delta t + \frac{\text{RTT}_{\text{pred}}}{2} + J_{\text{pred}} + d_{\text{queue}}(a) \quad [\text{ms}]$$

### 2.5 Multi-Objective Optimization & Hysteresis (Stages 205–206 / Numeral 106)
1. **Stage A Hard Feasibility Filter:**
   $$\mathcal{A}_{\text{feasible}} = \left\{ a \in \mathcal{A} \mid \forall e \in \mathcal{E}_{\text{crit}}, D_e(a) \le D_e^{\max} \ \land \ S_e(a) \le S_e^{\max} \ \land \ Q(a) \le Q_{\max} \right\}$$
2. **Stage B Cost Minimization:**
   $$a^* = \arg\min_{a \in \mathcal{A}_{\text{feasible}}} \left[ w_1 C_{\text{consistency}}(a) + w_2 C_{\text{network}}(a) + w_3 C_{\text{proc}}(a) + w_4 C_{\text{overhead}}(a) \right]$$
   where $w_1 = 0.40, w_2 = 0.30, w_3 = 0.15, w_4 = 0.15$.
3. **Per-Room Dwell-Time Hysteresis & Confidence Gating:**
   $$a_{\text{applied}}(r) = \begin{cases}
   a^* & \text{if } \text{confidence} \ge 0.80 \ \land \ (t - t_{\text{last}}(r)) \ge 6000\text{ ms} \\
   a_{\text{current}}(r) & \text{otherwise (hold existing policy)}
   \end{cases}$$

### 2.6 Per-Client Outbound Broadcast Modulation (Stage 207 / Numeral 107)
The server simulation tick frequency $f_{\text{sim}} = 30\text{ Hz}$ is invariant.
For each connected client $i$ with throttle factor $N_i \in \{1, 2, 3, 4\}$:
$$f_{\text{client}_i} = \frac{f_{\text{sim}}}{N_i}$$
Dispatched on tick $t$:
$$\mathbb{I}(\text{send to player } i) = (t \pmod{N_i} == 0)$$

### 2.7 Outcome Feedback & Bias Recalibration (Stage 208 / Numeral 108)
When observed ground-truth telemetry $y_{\text{actual}}$ arrives at horizon expiration $t + h$:
$$\epsilon_t = y_{\text{actual}} - \hat{y}_{\text{pred}}$$
Rolling error bias is exponentially updated ($\gamma = 0.20$):
$$\Delta_{\text{bias}}(t) = (1 - \gamma)\Delta_{\text{bias}}(t-1) + \gamma \epsilon_t$$
The updated bias $\Delta_{\text{bias}}$ is applied additively to subsequent forecasts: $\hat{y}_{t+h}^{\text{corrected}} = \hat{y}_{t+h} + \Delta_{\text{bias}}$.
