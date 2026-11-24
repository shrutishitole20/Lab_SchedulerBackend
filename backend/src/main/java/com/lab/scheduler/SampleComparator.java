package com.lab.scheduler;

import com.lab.enums.Priority;
import com.lab.model.Sample;

import java.util.Comparator;

/**
 * Priority comparator for laboratory samples.
 * Ordering rules:
 * 1. Priority rank (EMERGENCY=1, HIGH=2, NORMAL=3, LOW=4)
 * 2. Deadline (earliest deadline first)
 * 3. Received timestamp (FIFO for same priority & deadline)
 * 4. Sample ID (alphabetical deterministic tie-breaker)
 */
public class SampleComparator implements Comparator<Sample> {

    @Override
    public int compare(Sample s1, Sample s2) {
        if (s1 == s2) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;

        int r1 = (s1.getPriority() != null) ? s1.getPriority().getRank() : Priority.NORMAL.getRank();
        int r2 = (s2.getPriority() != null) ? s2.getPriority().getRank() : Priority.NORMAL.getRank();
        int priorityCompare = Integer.compare(r1, r2);
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        if (s1.getDeadline() != null && s2.getDeadline() != null) {
            int deadlineCompare = s1.getDeadline().compareTo(s2.getDeadline());
            if (deadlineCompare != 0) {
                return deadlineCompare;
            }
        } else if (s1.getDeadline() != null) {
            return -1;
        } else if (s2.getDeadline() != null) {
            return 1;
        }

        if (s1.getReceivedTime() != null && s2.getReceivedTime() != null) {
            int receivedCompare = s1.getReceivedTime().compareTo(s2.getReceivedTime());
            if (receivedCompare != 0) {
                return receivedCompare;
            }
        } else if (s1.getReceivedTime() != null) {
            return -1;
        } else if (s2.getReceivedTime() != null) {
            return 1;
        }

        if (s1.getId() != null && s2.getId() != null) {
            return s1.getId().compareTo(s2.getId());
        }

        return 0;
    }
}
