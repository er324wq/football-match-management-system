# Phase 4 snapshot notes

- The repository has no Git metadata. `before/` contains pre-edit copies of the changed controller/service/test/report sources; `snapshot_before_sha256.csv` records hashes.
- `ApplicationServiceImpl.java` was not included in the initial phase 4 snapshot selection. Before the file was edited, its original version was reconstructed from the then-current source by reverting exactly the two phase 4 changes (`updateById(application)` false check and `teamService.save(team)` false check); all remaining content was copied unchanged. Its snapshot hash is recorded in the before manifest.
- The additional `MatchResultMapper.java` before-copy was captured before the logical-delete restoration edit.
- `after/` has the final changed sources, reports and Maven output; its hashes are recorded in `snapshot_after_sha256.csv`.
- Phase 3 evidence files were not rewritten. The 41-entry Phase 3 artifact manifest has 37 current-file matches; four mismatches are expected edits to TECHNICAL_AUDIT.md, TECHNICAL_RESULTS.md, RESUME_PROJECT_RESULTS.md and MatchControllerTest.java. Their pre-phase-4 copies and earlier phase 3 after snapshots are preserved.