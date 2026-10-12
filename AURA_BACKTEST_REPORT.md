# AURA Deterministic Backtest Report

This report records automated deterministic cases present in the repository. It
does not claim that the requested fixtures A–S or the complete 20-scenario
acceptance matrix are finished; those remain tracked in
[AURA_COMPLETION_LEDGER.md](AURA_COMPLETION_LEDGER.md).

## Verified scenario families

| Scenario | Expected outcome | Automated evidence |
|---|---|---|
| Conflict-free generation | Complete initialized solution without hard penalty | Solver service and performance tests |
| Registered-student overlap | Hard penalty and persisted clash | Constraint and clash-detector tests |
| Hard offering conflict | Hard penalty | Constraint tests |
| Instructor/room/section/student unavailable | Assignment rejected or penalized at configured level | Constraint and readiness tests |
| Room capacity/type/facility mismatch | Hard penalty | Constraint and detector tests |
| Odd/even week coexistence | No false overlap | Constraint tests |
| Calendar closure | Assignment receives hard penalty | Constraint and migration/runtime tests |
| Teaching-group duplicate attendance | Hard penalty even across different times | Constraint tests |
| Impossible/difficult travel | Hard/medium penalty | Constraint and detector tests |
| Lecture after linked activity | Hard penalty | Constraint and detector tests |
| Pinned session | Move/repair protection | Service and browser tests |
| Safe draft move/swap | Preview and atomic apply | Service and browser tests |
| Published mutation | Rejected | Service and browser tests |
| Stale repair preview | Rejected | Repair service tests |
| Emergency room closure | Review draft with reassignment attempt | Service and browser tests |
| Resolution capacity/clash failure | Suggestion remains unsafe and cannot apply | Resolution service tests |
| Equivalent/parallel/makeup candidate | Classified and revalidated | Resolution repository/service tests |
| Resource what-if | Baseline/projected clash delta persisted | Scenario service tests |
| Cross-university identifier | Not found/denied | Authorization and browser tests |

## Performance backtests

Deterministic synthetic runs at approximately 300, 1,000, and 5,000
occurrences completed with zero hard penalties. Exact timings and memory
observations are in [AURA_BENCHMARK_REPORT.md](AURA_BENCHMARK_REPORT.md).

## Remaining acceptance work

The fixture naming matrix, a stable machine-readable expected/actual manifest,
multi-session dependency repair cases, all registration policy combinations,
and non-generation performance measurements are not yet complete. This report
therefore supplies evidence for implemented scenarios without marking the full
backtest gate complete.
