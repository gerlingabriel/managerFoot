---
name: match-engine-simulation
description: Use when designing, changing, or evaluating football match simulation, tactics, goal rates, injuries, cards, player strength, or season balance.
---

# Match engine simulation

1. Read the approved rules in `SPEC.md` and inspect the current engine before changing formulas.
2. Separate confirmed rules from provisional assumptions. Ask for approval before changing a major gameplay rule.
3. Prefer a reproducible random seed or a documented way to replay a simulation.
4. Define measurable hypotheses before running simulations: home advantage, goal distribution, tactical impact, upset rate, injuries, and red cards.
5. Run large enough batches to compare distributions, not just individual example matches.
6. Report sample size, seed strategy, assumptions, observed distributions, and uncertainty/limitations.
7. Compare tactical formations and team-strength gaps; ensure offensive tactics create both upside and defensive risk.
8. Check for degenerate outcomes, such as implausibly frequent extreme scores, tactics that dominate every alternative, or a weak team never winning.
9. Do not tune the engine solely to force a list of exact scorelines. Use plausible distributions and preserve rare surprises.
10. Add regression tests for bugs and deterministic edge cases; keep statistical tests robust against random flakiness.
