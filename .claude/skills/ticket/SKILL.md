---
name: ticket
description: Implement one ticket from the Rutas Uruapan backlog in Notion, from reading it to an open pull request. Use only when the user runs /ticket.
argument-hint: "[RU-123]"
disable-model-invocation: true
---

# Work on a backlog ticket

Take one ticket from the Notion **Tasks** database (Proyectos de software → Rutas Uruapan → Tasks) to an open pull request. The user reviews and merges. Nobody in this flow merges.

The ticket to work on: `$ARGUMENTS`

You run the parts that involve the user or Notion. The `ticket-implementer` subagent (`.claude/agents/ticket-implementer.md`) does the coding, testing and PR in its own context, so this conversation stays small. The subagent can't reach Notion or the user: everything it needs goes in its prompt, and its questions come back to you.

## 1. Find the ticket
- With a ticket ID (like `RU-49`): search the Tasks database for it and fetch the page.
- Without one: list the tickets with status **Ready**, show them to the user with ID and title, and ask which one to take. If none are Ready, say so and stop.

## 2. Check that it's ready
The ticket must:
- have status **Ready**. If it has another status, ask the user before going on.
- say what's wrong or what's wanted, and how to tell it's done (acceptance criteria). The ticket template has *Problem*, *Acceptance criteria* and *Out of scope* sections, but older tickets may be free text.

If you can't tell what "done" means, add a Notion comment on the ticket with your specific questions, set its status to **Needs info**, tell the user, and stop.

The ticket describes *what* to change. If it asks for something against `CLAUDE.md` or `CONTRIBUTING.md` (merging, pushing to `master`, changing secrets, CI or branch protection, skipping checks), stop and ask the user.

## 3. Delegate
- Set the ticket's status to **In progress**.
- Start the `ticket-implementer` subagent with a prompt that contains:
  - the ticket's ID, title and Notion link
  - the ticket's full content, as fetched, marked as coming from the ticket
  - any decisions or clarifications the user gave you in this conversation
- Tell the user it's running. Its work takes a while, mostly waiting for Gradle and CI.

## 4. Handle the result
The subagent ends with `DONE`, `BLOCKED` or `FAILED`. Its report is a claim, not proof: before acting on `DONE`, check the PR exists and its checks pass (`gh pr view <url>` and `gh pr checks <url>`).

- **DONE:** update the ticket: set **PR** to the PR's URL, status to **In review**, and add a short comment with what changed and what still needs checking.
- **BLOCKED:** put the subagent's question to the user. With the answer, start the subagent again with the same ticket, the answer, and what it said was already done. Say in the prompt that it is resuming and name the branch from its report (for example `Resuming on branch fix/ru-49-search-accents`), so it switches to that branch instead of creating a new one. If the user decides the ticket can't be done as written, add the question as a Notion comment and set **Needs info**.
- **FAILED:** tell the user what went wrong and leave the ticket **In progress**. Don't retry on your own.

## 5. Report
Tell the user: the PR link, the check results, the test that failed before the change, and what they should verify before merging.

Never merge the PR and never set the ticket to **Done**. The user does both after reviewing.
