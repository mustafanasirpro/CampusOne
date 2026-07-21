# AURA Solver Benchmark Report

Measured locally on 2026-07-20 using Java 21 and Timefold Solver Community Edition. These figures describe this machine and synthetic deterministic inputs; they are not production capacity guarantees.

## Results

| Occurrences | Solver limit | End-to-end elapsed | Score | Approximate memory delta | Acceptance |
|---:|---:|---:|---|---:|---|
| 300 | 2 seconds | 1,975 ms | `0hard/0medium/0soft` | 20,598,008 bytes | Complete, zero hard constraints |
| 1,000 | 2 seconds | 1,217 ms | `0hard/0medium/0soft` | 69,072,104 bytes | Complete, zero hard constraints |
| 5,000 | 2 seconds | 20,701 ms | `0hard/0medium/0soft` | 2,483,357,744 bytes | Complete, zero hard constraints |

## Interpretation

A deterministic construction pass now assigns eligible rooms, instructors, and timeslots while respecting known hard occupancy and availability facts before Timefold optimization begins. This removed the incomplete initialization observed for 1,000 occurrences in the earlier implementation.

The 5,000-occurrence case spent about 18.9 seconds initializing the Timefold score and consumed substantial heap despite the two-second solver limit. It completed with no uninitialized assignments and no hard penalty, but the memory result is a scaling warning. Production sizing should use representative university data and explicit container memory limits before enabling this scale.

## Reproduction

The default Maven suite runs the 300-occurrence sanity case. Larger cases are opt-in:

```powershell
cd backend
mvn -Dtest=AuraSolverPerformanceTest `
  -Daura.benchmark.sizes=300,1000,5000 `
  -Daura.benchmark.seconds=2 test
```

The benchmark records actual elapsed time, score, and process-memory observations. It does not fabricate utilization, student-gap, or repair metrics; those remain open in the completion ledger.
