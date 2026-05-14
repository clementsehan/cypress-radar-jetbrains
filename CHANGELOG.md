<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# cypress-radar-jetbrains Changelog

## [Unreleased]

## [0.0.1] - 2026-05-09
### Added
- Inline health blocks above each `it()` and `test()` call showing pass rate, run count, and failing run numbers pulled from the Cypress Cloud Data Extract API (last 7 days)
- Color-coded blocks: green (100%), yellow (75–99%), red (below 75% or currently failing), gray (new test), blue (dynamic title)
- Clickable failing run numbers that open the Cypress Cloud test replay URL directly in the browser
- Support for single-quoted, double-quoted, backtick template literal, and multi-line `it()` title forms
- Dynamic title detection for interpolated template literals and variable titles
- Project-level result cache (30-minute TTL matching Cypress Cloud's refresh cadence)
- Configuration via `flake-guard.json` in the project root (`provider` and `apiToken` fields)
- Auto-loads on opening `.cy.ts`, `.cy.js`, `.spec.ts`, and `.spec.js` files
