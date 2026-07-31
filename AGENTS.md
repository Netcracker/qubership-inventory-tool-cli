# Repository agent instructions

## Scope

- This repository builds the Java/Vert.x Qubership Inventory Tool CLI for collecting, merging, querying, and exporting
  component inventory graphs.
- This file contains repository-wide guidance; place narrower instructions next to the affected subtree.

## Repository map

- `src/main/java/org/qubership/itool/cli/` contains the Picocli entry point, commands, configuration loading, and SPIs.
- `src/main/java/org/qubership/itool/modules/` contains reusable graph, parsing, merger, query, and reporting services;
  `src/main/java/org/qubership/itool/tasks/` contains the Vert.x tasks assembled into CLI flows.
- `src/main/resources/org/qubership/itool/cli/` contains the built-in flow definitions loaded by the commands.
- `inventory-tool/` contains runtime profiles, configuration, and custom-task locations copied to
  `target/inventory-tool/` during Maven's `validate` phase.

## Commands

- Use JDK 21, matching `.github/workflows/maven-build.yml`, but keep source compatible with the Java 11 release set in
  `pom.xml`; the repository has no Maven wrapper.
- Focused test from the repository root: `mvn -Dtest=CommandProviderTest test` (replace the class as needed).
- Full build and test gate from the repository root: `mvn package`.
- Run targeted pre-commit hooks as `pre-commit run trailing-whitespace --files <changed-path>...`; running all hooks
  also runs the configuration-updating `pre-commit-update` hook.

## Non-obvious invariants

- When adding, removing, or renaming a built-in command, update its `CommandProvider`, its entry in
  `src/main/resources/META-INF/services/org.qubership.itool.cli.spi.CommandProvider`, and the count and name assertions
  in `CommandProviderTest`; then run that test.
- External extensions register their providers in the extension JAR's own `META-INF/services` files. Use
  `CommandProvider` for commands and `ModuleProvider` for a Guice override; do not add external providers to the core
  service file. Only the first discovered `ModuleProvider` is applied. No repository test loads the example's service
  descriptors against the current core, so inspect those files directly and report this gap.
- Edit runtime inputs under `inventory-tool/`, not Maven output under `target/inventory-tool/`; `pom.xml` copies the
  source tree during `validate`. Verify the packaged copy with `mvn package`.
- Edit the troubleshooting catalog at
  `agent-packages/troubleshoot-inventory-tool/.apm/skills/troubleshoot-inventory-tool/references/troubleshooting.md`.
  Preserve `docs/troubleshooting.md` as its relative symlink and validate catalog access with the adjacent
  `scripts/show_cases.py` helper.

## Done when

- Behavior changes have focused tests, and the affected test class passes.
- `mvn package` passes for every PR, matching the Maven Build workflow.
- Applicable targeted pre-commit hooks and GitHub Super-Linter pass; the repository-wide Markdown link check passes.
- The final response lists commands run and any checks that could not be run.

## Context routing

- Before changing extension points, read the current `CommandProvider.java`, `ModuleProvider.java`,
  `ApplicationContextHolder.java`, and `docs/qubership-inventory-tool-extension/` service descriptors and providers.
  They define current registration and module overrides; `docs/extension_guide.md` still contains older factory APIs.
- Before changing component `inventory.json` input, read `docs/readMe.md` and `docs/inventory_schema.json`; for the dump
  envelope read `docs/graph_dump_schema.json` and `docs/DevGuide/graphDumpSchema_v1.md`; for vertex and edge fields read
  `docs/DevGuide/graphModel_v3.md`.
- Before changing the troubleshooting package, read its `README.md` and `SKILL.md` because they define the canonical
  reference, read-only behavior, evidence rules, and validation helper.
