# 🧪 Comprehensive Test Cases & Verification Report

This document details the automated JUnit 5 test suite implemented in [`SchedulerServiceTest.java`](file:///c:/Users/shruti%20shitole/Downloads/lab-scheduler/lab-scheduler/src/test/java/com/lab/service/SchedulerServiceTest.java). The suite rigorously verifies the core scheduling algorithm, priority ordering, machine dispatch constraints, non-preemptive execution, failover handling, and SLA delay detection.

---

## 🏃 How to Run the Test Suite

All tests execute **deterministically in milliseconds without real-time waiting** (`Thread.sleep`) by utilizing fixed `LocalDateTime` timestamps.

### 1. Run via Maven CLI
Execute the following commands from the project root (`lab-scheduler/`):

```powershell
# Run the complete test suite (Unit tests + DAO tests + Integration tests)
mvn test

# Run specifically the 7 core scheduling policy tests
mvn test -Dtest=SchedulerServiceTest

# Run tests with detailed test execution output
mvn test -Dtest=SchedulerServiceTest -Dsurefire.printSummary=true
```

### 2. Run via IDE (IntelliJ IDEA / VS Code / Eclipse)
- Navigate to `src/test/java/com/lab/service/SchedulerServiceTest.java`.
- Click the green **Run** (▶) icon next to the class name or any individual `@Test` method.
- All 7 tests will execute and display green checkmarks in the test runner tree.

---

## 📊 Summary Table of the 7 Core Test Cases

| Case | Test Method Name | Scenario & Objective | Input & Initial Setup | Expected Result | Actual Result | Core Principle Proved |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | `testEmergencyAssignedBeforeNormalWaitingSamples` | Emergency arrives while normal sample is waiting | - Machine `M1` (capacity 1, Blood)<br>- `sInitial` (Normal, 09:00, 60m) occupying `M1`<br>- `sNormal` (Normal, 09:02, 120m) waiting in queue<br>- `sEmergency` (Emergency, 09:05, 15m) arrives<br>- `sInitial` completes | When `M1` becomes free, `sEmergency` is dispatched to `M1`; `sNormal` remains `QUEUED` | **PASSED** ✅ | **Priority Precedence:** Urgency tier (Emergency > Normal) always takes precedence regardless of arrival sequence. |
| **2** | `testSamePriorityEarlierDeadlineAssignedFirst` | Same priority, different deadlines | - Machine `M1` (capacity 1, Blood)<br>- `sInitial` (High, 09:00, 30m) occupying `M1`<br>- `sA` (High, 09:00, target 45m $\rightarrow$ Deadline 09:45)<br>- `sB` (High, 09:00, target 20m $\rightarrow$ Deadline 09:20)<br>- `sInitial` completes | `sB` is dispatched to `M1` first because its deadline (09:20) is earlier than `sA` (09:45); `sA` remains `QUEUED` | **PASSED** ✅ | **Earliest Deadline First (EDF):** Among identical priority levels, samples with tighter turnaround deadlines are served first. |
| **3** | `testSamePriorityAndDeadlineEarlierReceivedTimeFirst` | Same priority and same deadline | - Machine `M1` (capacity 1, Blood)<br>- `sInitial` (Normal, 09:00, 10m) occupying `M1`<br>- `sA` (Normal, 09:00, target 60m $\rightarrow$ Deadline 10:00)<br>- `sB` (Normal, 09:30, target 30m $\rightarrow$ Deadline 10:00)<br>- `sInitial` completes | Both have deadline 10:00. `sA` is assigned to `M1` first; `sB` remains `QUEUED` | **PASSED** ✅ | **FIFO Fairness:** When priority and deadline tie, the sample received earlier physically takes precedence. |
| **4** | `testNoCompatibleMachineKeepsSampleQueued` | Sample arrives with no compatible machine | - Machine `M1` (capacity 2, supports `Blood`, `Urine`)<br>- `sThyroid` (High, 09:00, 60m, requires `Thyroid`) arrives | Sample cannot be scheduled on `M1`; remains in `QUEUED` status; `machineId = null`; machine load remains 0 | **PASSED** ✅ | **Capability Safety:** Incompatible samples are never placed on unsuitable instruments; queue head remains intact. |
| **5** | `testMachineUnavailableRoutesWaitingSamplesToAnotherMachine` | Machine marked DOWN while samples wait | - `M1` (Blood, cap 1), `M2` (Blood, cap 1)<br>- `s1` runs on `M1`, `s2` runs on `M2`<br>- `s3` waits in queue<br>- `M1` marked DOWN<br>- `s1` completes on `M1`<br>- `s2` completes on `M2` | `s3` is NOT routed to `M1` upon `s1` finishing. When `M2` completes `s2`, `s3` is immediately routed to `M2` | **PASSED** ✅ | **Failover Routing:** Offline machines are bypassed; pending work automatically routes to healthy operational equipment. |
| **6** | `testSampleExceedingTargetReturnsOverdue` | SLA delay status transition over time | - `s` received at 09:00, target 30m (Deadline 09:30)<br>- Probe at 09:10 (33% elapsed)<br>- Probe at 09:24 (80% elapsed)<br>- Probe at 09:30 (100% elapsed)<br>- Probe at 09:35 (116% elapsed) | - 09:10 $\rightarrow$ `ON_TIME`<br>- 09:24 $\rightarrow$ `APPROACHING`<br>- 09:30 $\rightarrow$ `OVERDUE`<br>- 09:35 $\rightarrow$ `OVERDUE` | **PASSED** ✅ | **SLA Boundary Accuracy:** Accurate 3-tier status transitions strictly based on target threshold formula ($elapsed \ge 0.8 \times target$). |
| **7** | `testHigherPriorityArrivalDoesNotMoveProcessingSample` | Higher-priority sample arrives during active run | - Machine `M1` (capacity 1, Blood)<br>- `sNormal` (Normal, 09:00, 120m) actively `PROCESSING` on `M1`<br>- `sEmergency` (Emergency, 09:10, 15m) arrives | `sNormal` remains `PROCESSING` on `M1`; `sEmergency` is placed into `QUEUED` without interrupting `sNormal` | **PASSED** ✅ | **Non-Preemptive Invariant:** Active diagnostic runs are never aborted or evicted; lab equipment integrity is preserved. |
| **8** | `testLowPriorityOrderedBehindNormal` | Low priority support and ordering | - Machine `M1` (capacity 1, Blood)<br>- `sInitial` runs on `M1`<br>- `sLow` (Low, 240m) queued<br>- `sNormal` (Normal, 120m) queued later | `sNormal` is assigned before `sLow` because Rank 3 takes precedence over Rank 4 | **PASSED** ✅ | **Extended Priority Tiers:** 4-tier urgency (Emergency > High > Normal > Low) verified. |
| **9** | `testInvalidStatusTransitions` | Prevent invalid lifecycle status transitions | - Sample moves `RECEIVED -> QUEUED -> PROCESSING -> COMPLETED`<br>- Attempt `putOnHold` on completed sample<br>- Attempt `completeSample` on completed sample | Throws `IllegalStateException`; transitions are rejected | **PASSED** ✅ | **State Machine Integrity:** Terminal and illegal status transitions are safely blocked. |
| **10** | `testUniqueIdGenerationAndExtendedFields` | Unique ID generation and specimen attributes | - Sample registered with specimen type, category, and processing type | Sample ID generated uniquely (`S...`), extended attributes preserved | **PASSED** ✅ | **Domain Completeness:** FR-03 attributes verified. |

---

## 🔍 Detailed Test Case Analysis

### Case 1: Emergency Prioritization Over Waiting Workload
- **Method**: `testEmergencyAssignedBeforeNormalWaitingSamples()`
- **Clinical Motivation**: In an emergency department, a critical trauma troponin blood sample arrives. Previous routine checkup samples must wait so the trauma sample can be processed the instant a slot opens.
- **Test Steps**:
  1. Add Machine `M1` with capacity 1.
  2. Register `sInitial` (Normal). It immediately transitions to `PROCESSING` on `M1` (load: 1/1).
  3. Register `sNormal` at `09:02`. Machine is full; `sNormal` is placed in `QUEUED`.
  4. Register `sEmergency` at `09:05`. Machine is still full; `sEmergency` is placed in `QUEUED`.
  5. Call `completeSample(sInitial.getId())`.
- **Assertions**:
  ```java
  assertEquals(Status.PROCESSING, sEmergency.getStatus());
  assertEquals("M1", sEmergency.getAssignedMachineId());
  assertEquals(Status.QUEUED, sNormal.getStatus());
  ```
- **What It Proves**: The `PriorityQueue` comparator evaluates `Priority.rank` (1 vs 3) before arrival timestamp. The emergency sample leaps over previously queued normal samples.

---

### Case 2: Same Priority, Earlier Deadline First (EDF)
- **Method**: `testSamePriorityEarlierDeadlineAssignedFirst()`
- **Clinical Motivation**: When multiple high-priority samples are pending, the one closest to breaching its clinical SLA deadline must be scheduled first to prevent diagnostic breach penalties.
- **Test Steps**:
  1. Add Machine `M1` (capacity 1).
  2. Register `sInitial` to fill `M1`.
  3. Register `sA` (`HIGH`, received 09:00, target 45 min $\rightarrow$ Deadline 09:45).
  4. Register `sB` (`HIGH`, received 09:00, target 20 min $\rightarrow$ Deadline 09:20).
  5. Complete `sInitial`.
- **Assertions**:
  ```java
  assertEquals(Status.PROCESSING, sB.getStatus());
  assertEquals("M1", sB.getAssignedMachineId());
  assertEquals(Status.QUEUED, sA.getStatus());
  ```
- **What It Proves**: When priority ranks are identical, Tier 2 comparator ordering (`s1.getDeadline().compareTo(s2.getDeadline())`) determines dispatch priority.

---

### Case 3: Same Priority & Deadline (FIFO Fairness)
- **Method**: `testSamePriorityAndDeadlineEarlierReceivedTimeFirst()`
- **Clinical Motivation**: When clinical urgency and deadlines are identical, patient fairness dictates that whoever arrived first is processed first.
- **Test Steps**:
  1. Add Machine `M1` (capacity 1).
  2. Register `sInitial` to fill `M1`.
  3. Register `sA` (`NORMAL`, received 09:00, target 60 min $\rightarrow$ Deadline 10:00).
  4. Register `sB` (`NORMAL`, received 09:30, target 30 min $\rightarrow$ Deadline 10:00).
  5. Both share the exact same deadline (`10:00:00`).
  6. Complete `sInitial`.
- **Assertions**:
  ```java
  assertEquals(sA.getDeadline(), sB.getDeadline());
  assertEquals(Status.PROCESSING, sA.getStatus());
  assertEquals("M1", sA.getAssignedMachineId());
  assertEquals(Status.QUEUED, sB.getStatus());
  ```
- **What It Proves**: Tier 3 comparator ordering (`s1.getReceivedTime().compareTo(s2.getReceivedTime())`) breaks ties using First-In, First-Out arrival order.

---

### Case 4: Incompatible Machine Handling
- **Method**: `testNoCompatibleMachineKeepsSampleQueued()`
- **Clinical Motivation**: Chemical reagents on a Hematology analyzer cannot run Thyroid hormone immunoassays. The scheduler must never assign a sample to an incompatible machine.
- **Test Steps**:
  1. Add Machine `M1` configured only for `Blood` and `Urine`.
  2. Register `sThyroid` (`Thyroid`, `HIGH`).
- **Assertions**:
  ```java
  assertEquals(Status.QUEUED, sThyroid.getStatus());
  assertNull(sThyroid.getAssignedMachineId());
  assertEquals(0, m1.getCurrentLoad());
  assertEquals(1, scheduler.getQueue().size());
  ```
- **What It Proves**: The scheduling loop verifies `machine.canProcess(sample.getTestType())`. Unmatched samples remain safely queued without consuming slots or blocking other compatible samples.

---

### Case 5: Dynamic Machine Failover (Mark Down / Mark Up)
- **Method**: `testMachineUnavailableRoutesWaitingSamplesToAnotherMachine()`
- **Clinical Motivation**: When an instrument breaks down or undergoes calibration, pending work must seamlessly shift to backup instruments without dropping samples.
- **Test Steps**:
  1. Add `M1` (capacity 1) and `M2` (capacity 1). Both support `Blood`.
  2. Fill both machines with `s1` and `s2`.
  3. Queue `s3`.
  4. Call `scheduler.markMachineDown("M1")`.
  5. Complete `s1` on `M1`. `s3` must NOT be placed on `M1` because `isAvailable == false`.
  6. Complete `s2` on `M2`.
- **Assertions**:
  ```java
  scheduler.completeSample(s1.getId());
  assertEquals(Status.QUEUED, s3.getStatus()); // M1 bypassed

  scheduler.completeSample(s2.getId());
  assertEquals(Status.PROCESSING, s3.getStatus());
  assertEquals("M2", s3.getAssignedMachineId()); // Routed to M2
  ```
- **What It Proves**: Machine availability flags are respected in real time. Deactivated equipment is omitted from allocation, and subsequent frees on healthy machines pull from the backlog.

---

### Case 6: Delay SLA Boundary Detection
- **Method**: `testSampleExceedingTargetReturnsOverdue()`
- **Clinical Motivation**: Supervisors need clear, early alerts when samples risk breaching clinical turnaround guarantees.
- **Test Steps**:
  1. Create a `HIGH` sample `s` received at `09:00` with target `30 min` (deadline `09:30`).
  2. Query `checkDelay` at:
     - `09:10` (10 minutes elapsed, 33%)
     - `09:24` (24 minutes elapsed, 80% threshold reached)
     - `09:30` (30 minutes elapsed, deadline reached)
     - `09:35` (35 minutes elapsed, deadline exceeded)
- **Assertions**:
  ```java
  assertEquals(DelayStatus.ON_TIME, scheduler.checkDelay(s, received.plusMinutes(10)));
  assertEquals(DelayStatus.APPROACHING, scheduler.checkDelay(s, received.plusMinutes(24)));
  assertEquals(DelayStatus.OVERDUE, scheduler.checkDelay(s, received.plusMinutes(30)));
  assertEquals(DelayStatus.OVERDUE, scheduler.checkDelay(s, received.plusMinutes(35)));
  ```
- **What It Proves**: Formula $\text{elapsed} \ge 0.8 \times \text{target}$ accurately triggers warning states before overdue violations occur.

---

### Case 7: Non-Preemptive Execution Safety
- **Method**: `testHigherPriorityArrivalDoesNotMoveProcessingSample()`
- **Clinical Motivation**: In clinical diagnostics, interrupting a centrifuge or reagent incubation cycle destroys the biological specimen. Active jobs must never be canceled or preempted.
- **Test Steps**:
  1. Add Machine `M1` (capacity 1).
  2. Start processing a `NORMAL` sample `sNormal`.
  3. Register an `EMERGENCY` sample `sEmergency` at `09:10`.
- **Assertions**:
  ```java
  assertEquals(Status.PROCESSING, sNormal.getStatus());
  assertEquals("M1", sNormal.getAssignedMachineId());
  assertEquals(Status.QUEUED, sEmergency.getStatus());
  assertNull(sEmergency.getAssignedMachineId());
  assertEquals(1, m1.getCurrentLoad());
  ```
- **What It Proves**: The system maintains non-preemptive integrity. Emergency samples queue at the head of the waitlist to take the next available slot, but never terminate active runs.

---

## 📈 Quality & Reliability Highlights

1. **Zero Flakiness**: No dependencies on system clocks, network roundtrips, or background threads. All tests run on pure Java standard library data structures.
2. **Deterministic Reproducibility**: Fixed timestamps eliminate date-boundary or time-zone variances across development and CI/CD environments.
3. **Graceful Persistence Independence**: All unit tests run against `SchedulerService` in memory without requiring an active MySQL instance, ensuring rapid feedback cycles during continuous integration.
