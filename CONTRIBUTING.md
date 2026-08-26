# Contributing to Ultimate XOX

Thank you for your interest in contributing.

## Ways to contribute

- Improve the rules engine or AI heuristics
- Fix UI issues or accessibility problems
- Improve tests and documentation
- Add support for additional platforms or packaging workflows
- Help with design, onboarding, and contributor experience

## Local setup

```bash
npm install
npm test
```

For Java desktop work:

```bash
cd UltimateXOX
mvn test
```

## Workflow

1. Fork the repository and create a branch.
2. Keep changes focused and behavior-preserving.
3. Add or update tests when changing game logic or AI evaluation.
4. Run the relevant checks before opening a pull request.
5. Explain the motivation and scope clearly in the PR description.

## Code expectations

- Prefer clear names over clever abstractions.
- Preserve the current gameplay rules unless the issue explicitly requires a rule change.
- Keep new code consistent with the existing structure and naming conventions.
- Document non-obvious logic and algorithmic decisions.

## Reporting issues

Please open an issue with:

- a clear description of the problem
- reproduction steps
- expected behavior
- environment details (browser, OS, Java version, device, etc.)

## Communication

Keep discussions respectful, technical, and solution-oriented.
