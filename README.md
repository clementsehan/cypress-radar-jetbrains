# Cypress Radar

![Build](https://github.com/clementsehan/cypress-radar-jetbrains/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/31748.svg)](https://plugins.jetbrains.com/plugin/31748)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/31748.svg)](https://plugins.jetbrains.com/plugin/31748)

<!-- Plugin description -->
**Cypress Radar** shows the historical pass rate of each Cypress test directly inside your spec files, powered by the Cypress Cloud Data Extract API.

For each `it()` or `test()` call, a color-coded block appears above the line showing the last 7 days of run history:

- **Green** — 100% pass rate
- **Yellow** — 75–99% pass rate (flaky)
- **Red** — below 75% or failing in the most recent run
- **Gray** — no historical data yet (new test)
- **Blue** — dynamic title that cannot be statically matched

Each block shows `passes/total (rate%), failed in runs: #1234 #1235`. Run numbers are clickable and open the Cypress Cloud test replay directly in the browser.

**Supports all common `it()` forms:** single-quoted, double-quoted, backtick template literals, and multi-line declarations.

**How to activate**

Place a `flake-guard.json` file in your project root:

```json
{ "provider": "cypress-cloud", "apiToken": "YOUR_DATA_EXTRACT_API_TOKEN" }
```

The plugin activates automatically when you open a `.cy.ts`, `.cy.js`, `.spec.ts`, or `.spec.js` file. Results are cached for 30 minutes.

> Requires a **Cypress Cloud Enterprise** plan. Generate an API token at cloud.cypress.io → Integrations → Data Extract API.

---

**Works with:** IntelliJ IDEA, WebStorm, and all other JetBrains IDEs.

---

If you find this plugin useful, consider [buying me a coffee ☕](https://ko-fi.com/clemsehan) — it helps keep the project alive!

---

**Keywords:** cypress, flaky tests, test health, pass rate, inline, spec, e2e, test analytics, cypress cloud
<!-- Plugin description end -->

## Compatibility

| IDE                                 | Minimum version |
|-------------------------------------|-----------------|
| IntelliJ IDEA Community & Ultimate  | 2024.1          |
| WebStorm                            | 2024.1          |
| PyCharm Community & Professional    | 2024.1          |
| GoLand                              | 2024.1          |
| Rider                               | 2024.1          |
| CLion                               | 2024.1          |
| RubyMine                            | 2024.1          |
| PhpStorm                            | 2024.1          |

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Cypress Radar"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31748) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/31748/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/clementsehan/cypress-radar-jetbrains/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Development

```bash
# Run in a sandbox IDE
./gradlew runIde

# Build the plugin ZIP
./gradlew buildPlugin

# Run tests
./gradlew test
```

## Support

If you find this plugin useful, consider [buying me a coffee ☕](https://ko-fi.com/clemsehan) on Ko-fi — it helps keep the project alive!

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
