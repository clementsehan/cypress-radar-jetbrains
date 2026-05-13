# Cypress Radar

A JetBrains IDE plugin that displays Cypress test health inline in your spec files, powered by the **Cypress Cloud Data Extract API**.

## What it does

For each `it()` or `test()` call in a spec file, a color-coded block appears above the line showing the historical pass rate pulled from the last 7 days of Cypress Cloud runs:

| Color | Meaning |
|-------|---------|
| 🟢 Green | 100% pass rate |
| 🟡 Yellow | 75–99% pass rate (flaky) |
| 🔴 Red | Below 75% or failing in the most recent run |
| ⬜ Gray | No historical data (new test) |
| 🔵 Blue | Dynamic title — cannot be statically matched |

Each block displays `passes/total (rate%), failed in runs: #1234 #1235`. Run numbers are clickable and open the Cypress Cloud test replay directly.

Supports all common `it()` forms:
- `it('title', ...)`
- `it("title", ...)`
- `` it(`title`, ...) ``
- Multi-line form where the title is on the next line

Results are fetched once per spec file and cached for 5 minutes, so subsequent file opens are instant.

## Requirements

- A Cypress Cloud **Enterprise** plan (the Data Extract API is an Enterprise feature)
- A Data Extract API token — generate one at **cloud.cypress.io → Integrations → Data Extract API**

## Configuration

Place a `flake-guard.json` file in your project root:

```json
{
  "provider": "cypress-cloud",
  "apiToken": "YOUR_DATA_EXTRACT_API_TOKEN"
}
```

The plugin activates automatically when you open a `.cy.ts`, `.cy.js`, `.spec.ts`, or `.spec.js` file.

## Installation

**From disk (local build):**

1. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```
2. In your IDE: **Settings → Plugins → ⚙️ → Install Plugin from Disk**
3. Select `build/distributions/cypress-radar-0.0.1.zip`
4. Restart the IDE

**From JetBrains Marketplace** *(once published):*

**Settings → Plugins → Marketplace** → search for **Cypress Radar** → Install

## Development

```bash
# Run in a sandbox IDE
./gradlew runIde

# Build the plugin ZIP
./gradlew buildPlugin

# Run tests
./gradlew test
```
