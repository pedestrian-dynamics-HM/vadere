# Writing Changelog Entries

## Introduction

A good changelog entry should be descriptive and concise. It should explain the change to a reader who has **zero** context about the change. Each change should be categorized into one of the following types: `added`, `removed`, `changed`, `fixed`, `performance`, `security`, `deprecated` or `other`.

- **Bad:** Highlight agents.
- **Good:** In class `OnlineVisualization`, highlight the agent which was selected with left-mouse button.

The first example doest not provide any context where the change was made or what benefit it brings to the user.

## Writing into Changelog.md

1. Each new version number gets an own subsection prefixed with `##` and a date, e.g., `## v0.1 (2018-01-31)`.
   Notes:
   * Unreleased versions should be marked explicitly, e.g., `## 0.1 (unreleased)`.
   * New versions should be added on top (and not at bottom).
2. Each change type gets an own subsubsection prefixed with `###`, e.g., `Added`.
3. Each changelog entry should contain following parts:
   1. An own bullet point.
   2. The description of the change.
   3. The commit hash.
   4. A list of affected simulator components: `Annotation`, `GUI`, `Simulator`, `State` or `Utils`.
   5. Optional: More details as sub bullet points.

For instance:

```
## v0.1 (2018-01-31)

### Removed

- Removed obsolete model attribute "foo" from class "bar". (e8af1b77: State).
  * ...
  * ...
```
