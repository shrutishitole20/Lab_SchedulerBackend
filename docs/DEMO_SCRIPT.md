# ⏱️ 5-Minute Live Demonstration Script

This document provides a minute-by-minute demonstration script for showcasing the **Lab Sample Scheduler**. It includes exact spoken narration, screen interactions, and key technical concepts to highlight.

---

## 📋 Pre-Demo Checklist (30 Seconds Before Demo)

1. **MySQL Database**: Ensure MySQL is running and `database/schema.sql` has been executed.
2. **Start Backend**: Open a terminal in `lab-scheduler/` and run:
   ```powershell
   mvn spring-boot:run
   ```
3. **Open Browser**: Open Google Chrome or Microsoft Edge and navigate to:
   ```text
   http://localhost:8080/
   ```
4. **Prepare Two Browser Windows / Incognito** (Optional, for quick role comparison):
   - Window 1: For `staff` (`staff` / `staff123`)
   - Window 2: For `admin` (`admin` / `admin123`)

---

## 🎬 Minute-by-Minute Demo Flow

```text
[0:00 - 1:00] Overview & Architecture   ──> Problem, Tech Stack, & Clean UI
[1:00 - 2:00] Staff Login & Dashboard   ──> Live Workload, SLA Highlights & Auto-Refresh
[2:00 - 3:00] Priority Queue & Safety   ──> Emergency Jump, Non-Preemption Invariant
[3:00 - 4:00] Failover & Hold Workflow  ──> Machine DOWN/UP, Failover, Audit History Modal
[4:00 - 5:00] Analytics & Admin Roles   ──> Daily SLA Report, Role-Based Security & Wrap-up
```

---

### ⏱️ Minute 0:00 – 1:00: Overview & Architecture

#### 🗣️ Spoken Narration:

> _"Good morning/afternoon everyone. Today I'm presenting the **Lab Sample Scheduler**, a real-time clinical laboratory management system. In modern diagnostic labs, samples like Blood, Urine, and Thyroid arrive continuously with varying urgency—from routine health checks to critical ICU emergencies._
>
> _Our goal was to design an optimal, deterministic scheduling engine that guarantees turnaround times, prevents instrument overload, and protects specimens from being interrupted._
>
> _The backend is built with **Java 17** and **Spring Boot 3.3.5**, backed by **MySQL 8.0** with strict `PreparedStatement` data integrity. The scheduling engine uses a custom **4-tier PriorityQueue** in memory. On the frontend, we built a responsive **Bootstrap 5 Single-Page Application** with real-time live polling every 10 seconds."_

#### 🖥️ Action:

- Display the clean login screen at `http://localhost:8080/`.

---

### ⏱️ Minute 1:00 – 2:00: Staff Login & Real-Time Dashboard

#### 🗣️ Spoken Narration:

> _"Let's log in as a laboratory technician using our staff credentials (`staff` / `staff123`)._
>
> _Notice the clean operational dashboard. At the top, we have our **Machine Workload** meters. Machine `M1` is a high-throughput hematology analyzer with capacity 2, currently supporting Blood and Urine. Machine `M2` is an immunoassay unit with capacity 1._
>
> _Below, we have the **Live Queue Table**. Every sample shows its priority, test type, arrival time, and SLA status badge._
>
> _Notice our color-coded SLA indicators: **Green** means On Track; **Yellow** highlights samples approaching 80% of their turnaround time; and **Red** highlights samples that are Overdue. The entire dashboard automatically refreshes every 10 seconds without full-page reloads."_

#### 🖥️ Action:

1. Log in with:
   - **Username**: `staff`
   - **Password**: `staff123`
2. Point out:
   - Machine load progress bars and free slot counts.
   - Seed samples (`S001`-`S005`) with their respective SLA badges.
   - The auto-refresh pulse indicator in the top navbar.

---

### ⏱️ Minute 2:00 – 3:00: Priority Queue & Non-Preemptive Safety

#### 🗣️ Spoken Narration:

> _"Now let's demonstrate the core scheduling policy: **Priority Precedence** and **Non-Preemptive Safety**._
>
> _Currently, `M1` has capacity 2. Let's register two routine Blood samples to occupy both slots._
>
> _(Click Register Sample $\rightarrow$ Normal Blood)._
> _Both are immediately assigned and are now `PROCESSING` on `M1`._
>
> _Now, imagine a routine sample arrives and is queued. But suddenly, an **EMERGENCY** trauma sample arrives from the emergency room!_
>
> _(Register Emergency Blood sample)._
> _Notice two crucial invariants:_
>
> 1. _The Emergency sample instantly jumps to the very top of the queue, ahead of the routine sample._
> 2. _However, the routine sample currently running on `M1` is **NOT interrupted or evicted**. In clinical labs, stopping a spinning centrifuge or reagent incubation ruins the specimen. Non-preemption is strictly preserved!_
>
> _The moment technician completes the routine sample, watch what happens:_
> _(Click Complete on M1's sample)._
> _The free slot opens, and the Emergency sample is instantly dispatched!"_

#### 🖥️ Action:

1. Click **Register Sample** in the sidebar:
   - **Test Type**: `Blood`
   - **Priority**: `NORMAL`
   - **Target (min)**: `120`
   - Click **Register Sample**.
2. Register a second `NORMAL` sample to fill `M1` (`Load: 2/2`).
3. Register a third sample with `NORMAL` priority (it enters `QUEUED`).
4. Register a fourth sample with **`EMERGENCY`** priority.
5. Point out to the audience: Emergency sample is at the top of the waiting queue.
6. Click the green **Complete** button on the first running sample.
7. Show that the Emergency sample immediately transitions to `PROCESSING` on `M1`.

---

### ⏱️ Minute 3:00 – 4:00: Machine Maintenance & Dynamic Failover

#### 🗣️ Spoken Narration:

> _"What happens when laboratory equipment breaks down or requires calibration?_
>
> _Let's mark machine `M1` as **DOWN** for maintenance._
> _(Click Mark Down on M1)._
>
> _Notice that `M1` status turns red to `MAINTENANCE`. The system guarantees that:_
>
> - _Samples already running on `M1` are allowed to finish safely._
> - _No new samples will ever be assigned to `M1` while it is offline._
> - _Pending samples in the queue automatically route to operational backup machines—like `M2`._
>
> _Once maintenance is finished, clicking **Mark Up** instantly restores the machine to `ACTIVE` and the scheduler redistributes any waiting backlog._
>
> _Now let's click the **History** button on any sample._
> _Here is the full immutable audit trail: every status transition, machine assignment, and timestamp is stored in the database for compliance and traceability."_

#### 🖥️ Action:

1. Click the red **Mark Down** button on machine `M1`.
2. Observe badge update to `MAINTENANCE` and button toggling to **Mark Up**.
3. Click the yellow **Hold** button on a queued sample; show it moves to `ON_HOLD`.
4. Click **Release** to return it to `QUEUED`.
5. Click the blue **History** button on any sample to open the modal and scroll through the timeline.
6. Click **Mark Up** on `M1` to restore it to service.

---

### ⏱️ Minute 4:00 – 5:00: Daily Reports & Admin Role-Based Security

#### 🗣️ Spoken Narration:

> _"For lab supervisors, monitoring turnaround compliance is essential. Clicking on **Daily Reports** gives us an executive overview._
>
> _We can select any date to see total completed samples, in-progress workloads, overdue SLA violations, and on-hold rates, complete with average turnaround time metrics._
>
> _Finally, let's look at **Role-Based Security**._
> _Right now, as `staff`, operational buttons are available, but administrative configuration is hidden._
>
> _Let's log out and log in as `admin` (`admin` / `admin123`)._
>
> _Notice that an **Admin Console** tab appears in the navigation bar! From here, administrators can dynamically add new diagnostic machines, configure capacity and test compatibility, and manage turnaround SLAs and test types without changing code._
>
> _In summary, the Lab Sample Scheduler combines algorithmic precision through Java PriorityQueues with real-world clinical resilience, failover protection, and an intuitive user interface. Thank you!"_

#### 🖥️ Action:

1. Click **Daily Reports** in the navbar.
2. Select today's date and click **Generate Report** to show summary metric cards and sample tables.
3. Click **Logout** in the top-right corner.
4. Log in as **`admin`** / **`admin123`**.
5. Point out the newly visible **Admin Console** tab in the navigation bar.
6. Open **Admin Console** and showcase the forms:
   - Add Machine with multi-select test capabilities.
   - Configure Priorities and target turnaround minutes.
   - Configure Test Types.

---

## 💡 Quick Reference Card for Presenter

| Action                 | Credentials / Input                  | Expected Visual Feedback                                                    |
| :--------------------- | :----------------------------------- | :-------------------------------------------------------------------------- |
| **Login as Staff**     | `staff` / `staff123`                 | Dashboard loads; Admin tab is hidden.                                       |
| **Register Emergency** | Test: `Blood`, Priority: `EMERGENCY` | Sample jumps to top of queue with red `EMERGENCY` badge.                    |
| **Complete Sample**    | Click green **Complete** button      | Sample moves to `COMPLETED`; next queued sample starts `PROCESSING`.        |
| **Hold Sample**        | Click yellow **Hold** button         | Sample status changes to `ON_HOLD`; removed from active scheduling.         |
| **Release Sample**     | Click blue **Release** button        | Sample returns to `QUEUED` and re-enters scheduling pool.                   |
| **Machine Down**       | Click red **Mark Down** button       | Machine badge turns red `MAINTENANCE`; no new samples dispatched to it.     |
| **Machine Up**         | Click green **Mark Up** button       | Machine badge turns green `ACTIVE`; pending samples immediately dispatched. |
| **View Audit Trail**   | Click blue **History** button        | Modal opens showing timestamped actions and status transitions.             |
| **Daily Report**       | Select today's date                  | Summary cards show Completed, Pending, Delayed, On-Hold count.              |
| **Login as Admin**     | `admin` / `admin123`                 | **Admin Console** tab is visible; can add machines/priorities/tests.        |
