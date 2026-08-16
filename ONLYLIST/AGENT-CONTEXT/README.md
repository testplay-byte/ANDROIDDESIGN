# AGENT-CONTEXT — Agent Memory & Rules

> This folder is the AI agent's persistent memory + rules for the ONLYLIST project.
> It is versioned in the GitHub repo so any future AI agent (or session) can pick up
> where the last one left off.

## Structure

```
AGENT-CONTEXT/
├── README.md              ← this file
├── rules/
│   └── CORE_RULES.md      ← the non-negotiable rules (read FIRST at session start)
├── memory/
│   ├── progress.md        ← current state, what's done, what's next (read SECOND)
│   ├── decisions.md       ← architectural decisions D-NNN (PROPOSED / CONFIRMED)
│   ├── lessons-learned.md ← self-learning log (grep for tags at task start)
│   ├── open-questions.md  ← unresolved questions for the user
│   └── session-log.md     ← append-only session history
└── knowledge/
    ├── README.md          ← index of knowledge files
    ├── architecture.md    ← tech stack, module graph, data flow
    ├── cline-agent.md     ← quick-reference: Cline porting notes (→ research/R-1)
    ├── anilist-api.md     ← quick-reference: AniList API (→ research/R-2)
    ├── kitsu-jikan-api.md ← quick-reference: Kitsu + Jikan (→ research/R-3)
    └── android-design-system.md ← quick-reference: Android patterns (→ research/R-4)
```

## How to use this folder (for any AI agent starting a new session)

1. **Read `rules/CORE_RULES.md` FIRST.** It governs everything. Pay attention to §15 (Sandbox Recovery) and §0 (project identity placeholders).
2. **Read `memory/progress.md` SECOND.** It tells you where the project is.
3. **Read `memory/open-questions.md` THIRD.** If there are unanswered blocking questions, the project is paused waiting on the user — don't proceed past them without answers.
4. **Read `memory/decisions.md` for context.** Status PROPOSED = awaiting user confirmation; CONFIRMED = locked in.
5. **Grep `memory/lessons-learned.md`** for tags matching your current task type (`[INSIGHT]`, `[PATTERN]`, `[MISTAKE]`).
6. **Read `knowledge/architecture.md`** for the big picture.
7. **Read the relevant `knowledge/*.md`** for the domain you're working in.

## What goes where (do NOT mix)

- `memory/` — agent state (progress, decisions, lessons, questions, session log). Main agent only updates this.
- `rules/` — the rules. Main agent only edits.
- `knowledge/` — quick-reference summaries. Link to detailed docs in `research/` or `APP/ani-design/DOCUMENTATION/`.

## Sub-agents: DO NOT EDIT THIS FOLDER

Sub-agents report back to the main agent, who updates AGENT-CONTEXT after verifying their work. (CORE_RULES §14.) If you're a sub-agent, write only to the folder you were assigned.
