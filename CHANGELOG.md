<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# cypress-radar-jetbrains Changelog

## [Unreleased]

## [0.0.2] - 2026-05-14

### Added

- `timeframe` config option to control how many days of run history are fetched (default: 7)
- `cache` config option to adjust the result cache TTL in minutes (default: 30)
- `projects` config option to filter results by specific Cypress project IDs

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

[Unreleased]: https://github.com/clementsehan/cypress-radar-jetbrains/compare/0.0.2...HEAD
[0.0.2]: https://github.com/clementsehan/cypress-radar-jetbrains/compare/0.0.1...0.0.2
[0.0.1]: https://github.com/clementsehan/cypress-radar-jetbrains/commits/0.0.1
