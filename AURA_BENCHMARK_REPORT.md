# AURA Solver Benchmark Report

Measured locally on 2026-07-27 using Java 21 and Timefold Solver Community
Edition after the current constraint changes. These figures describe this
machine and synthetic deterministic inputs; they are not production capacity
guarantees.

## Results

| Occurrences | Solver limit | End-to-end elapsed | Score | Approximate memory delta | Acceptance |
|---:|---:|---:|---|---:|---|
| 300 | 2 seconds | 2,805 ms | `0hard/0medium/-150soft` | 95,285,648 bytes | Complete, zero hard constraints |
| 1,000 | 2 seconds | 2,152 ms | `0hard/0medium/-200soft` | 221,093,024 bytes | Complete, zero hard constraints |
| 5,000 | 2 seconds | 17,237 ms | `0hard/0medium/-500soft` | 1,987,455,008 bytes | Complete, zero hard constraints |

## Interpretation

A deterministic construction pass now assigns eligible rooms, instructors, and timeslots while respecting known hard occupancy and availability facts before Timefold optimization begins. This removed the incomplete initialization observed for 1,000 occurrences in the earlier implementation.

The new soft scores reflect undesirable-time and stability preferences in the
synthetic fixture; acceptance depends on the zero hard score, not a zero soft
score. The 5,000-occurrence case spent about 15.5 seconds initializing/scoring
before termination and consumed substantial heap despite the two-second solver
limit. It completed with no uninitialized assignments and no hard penalty, but
the memory result remains a scaling warning. Production sizing should use
representative university data and explicit container memory limits before
enabling this scale.

## Reproduction

The default Maven suite runs the 300-occurrence sanity case. Larger cases are opt-in:

```powershell
cd backend
mvn -Dtest=AuraSolverPerformanceTest `
  -Daura.benchmark.sizes=300,1000,5000 `
  -Daura.benchmark.seconds=2 test
```

The benchmark records actual elapsed time, score, and process-memory observations. It does not fabricate utilization, student-gap, query, import, clash-detection, or repair benchmark metrics; those remain open in the completion ledger and prevent full performance acceptance.
