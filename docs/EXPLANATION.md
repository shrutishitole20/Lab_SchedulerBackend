# 🧠 Technical Architecture & System Design Explanation

This document explains the core architectural and algorithmic design choices of the Lab Sample Scheduler in simple, clear terms.

---

## 1. The Queue Ordering Policy

When samples wait in the queue, how does the system decide who goes first? We implemented a deterministic **4-tier ordering policy** in `SampleComparator`:

```text
[Priority Rank]  --->  [Earlier Deadline]  --->  [Earlier Arrival]  --->  [Sample ID]
  (Emergency 1)            (09:15 vs 09:30)            (FIFO order)          (Alphabetical)
```

1. **Tier 1: Priority Rank (Urgency First)**
   - Every priority tier has a numeric rank:
     - `EMERGENCY` = Rank 1 (highest urgency)
     - `HIGH` = Rank 2
     - `NORMAL` = Rank 3
   - Lower numbers always jump ahead. An `EMERGENCY` sample will always be evaluated before a `HIGH` sample, even if the high-priority sample arrived earlier.

2. **Tier 2: Earlier Deadline First (Turnaround SLA)**
   - If two samples share the same priority (e.g., both are `HIGH`), the one due sooner gets processed first.
   - Deadline is calculated as: `deadline = receivedTime + targetMinutes`.
   - A sample due at $09:30$ will take precedence over a sample due at $09:45$.

3. **Tier 3: Earlier Received Time First (FIFO Fairness)**
   - If two samples have the same priority *and* the exact same deadline, we fall back to standard First-In, First-Out (FIFO) fairness. The sample that physically arrived at the lab earlier gets processed first.

4. **Tier 4: Sample ID Alphabetical (Deterministic Tie-Breaker)**
   - In the rare event that two samples have the same priority, deadline, and arrival timestamp, their unique ID (e.g., `S001` vs `S005`) provides a final deterministic tie-break so queue ordering is 100% predictable and reproducible across all JVM executions.

---

## 2. How the Next Sample is Selected

The queue is backed by a Java **`PriorityQueue<Sample>`** configured with `SampleComparator`.

- Unlike a regular `LinkedList` or `Queue` where elements exit in the order they were inserted, a `PriorityQueue` organizes elements internally as a **binary min-heap**.
- When `queue.poll()` is called, the min-heap root is extracted in $O(\log N)$ time, instantly yielding the single most urgent, earliest-due sample currently waiting in the laboratory.
- To inspect all waiting samples without disturbing queue order during assignment cycles, the scheduler traverses the queue in strict priority order using sequential `poll()` extractions. Any sample that cannot be placed into a machine is placed into a temporary holding buffer and restored into the queue via `queue.addAll()`.

---

## 3. How Machine Assignment Works

Laboratory machines have physical real-world constraints:
- They can only process certain diagnostic test types (e.g., machine `M1` can run `Blood` and `Urine`, but cannot run `Thyroid`).
- They have a maximum concurrent load limit (`capacity`).
- They might be taken offline for maintenance (`is_available = false`).

### The Assignment Cycle (`assignSamples()`)
1. The scheduler picks the highest-priority sample waiting at the head of the queue.
2. It checks all registered machines looking for a machine that meets three mandatory conditions:
   $$\text{Machine Valid} = (\text{isAvailable} == \text{true}) \land (\text{canProcess}(\text{testType}) == \text{true}) \land (\text{currentLoad} < \text{capacity})$$
3. **If a matching machine is found**:
   - The sample is removed from the waiting queue.
   - Its status transitions from `QUEUED` $\rightarrow$ `PROCESSING`.
   - Its `startTime` is recorded, and `assignedMachineId` is attached.
   - The machine's `currentLoad` counter is incremented by 1.
   - An audit event is logged in `sample_history`.
4. **If no matching machine has a free slot**:
   - The sample is kept in the queue in its exact priority position.
   - The scheduler moves on to check the next sample in line (for instance, an available machine might support a different test type needed by a sample further down the queue).
5. **Non-Preemptive Principle**:
   - The system **never interrupts or unseats** a sample that is already `PROCESSING`. Even if a life-or-death Emergency sample arrives, active equipment finishes its current sample safely before admitting the new one.

---

## 4. How Delays Are Detected (SLA Monitoring)

Turnaround time tracking is evaluated by `checkDelay(Sample s, LocalDateTime now)`:

$$\text{Target Turnaround} = \text{targetMinutes} \times 60 \text{ seconds}$$
$$\text{Elapsed Time} = \text{Duration between } \text{receivedTime} \text{ and } \text{now}$$

Three distinct states are classified:

1. 🔴 **`OVERDUE`**:
   - Condition: `now >= deadline`
   - The sample has exceeded its committed processing window. This is flagged in bright **red** on the live UI dashboard.
2. 🟡 **`APPROACHING`**:
   - Condition: $\text{Elapsed Time} \ge 0.80 \times \text{Target Turnaround}$ and `now < deadline`
   - $80\%$ or more of the sample's target turnaround time has elapsed, warning staff that the deadline is imminent. This is highlighted in **yellow** on the dashboard.
3. 🟢 **`ON_TIME`**:
   - Condition: Elapsed time is under the $80\%$ threshold. Highlighted in **green**.

Because `checkDelay` accepts any `LocalDateTime now` parameter, the laboratory can monitor live current time in real-time or simulate point-in-time SLA performance for historical reporting.

---

## 5. Why We Used `PriorityQueue`

We chose Java's `java.util.PriorityQueue` instead of a standard `ArrayList`, `LinkedList`, or re-sorting on every operation for three key reasons:

1. **Computational Efficiency ($O(\log N)$ vs $O(N \log N)$)**:
   - In a busy hospital, hundreds of samples arrive continuously.
   - If we stored samples in an `ArrayList` and sorted it every time a new sample arrived or completed, each insertion would cost $O(N \log N)$.
   - `PriorityQueue` implements a binary heap: adding a sample (`add`) and extracting the most urgent sample (`poll`) both take only **$O(\log N)$ time**, while inspecting the next candidate (`peek`) is **$O(1)$ constant time**.
2. **Dynamic In-Flight Re-Ordering**:
   - If an emergency sample arrives 10 minutes into a shift, it automatically bubbles up to the front of the heap ahead of 50 normal samples without needing an explicit manual array resort.
3. **Memory Footprint & Concurrency**:
   - The heap is backed by a compact contiguous array with no pointer overhead, making it lightweight and cache-friendly on modern CPU architectures.
