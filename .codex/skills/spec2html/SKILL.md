---
name: spec2html
description: Use this skill whenever the user wants to turn a product spec, feature idea, requirements draft, PRD, SRS, or agent-generated specification into an interactive HTML review document where humans can comment, choose one option, choose multiple options, rate/mark sections, and finally copy all review decisions as an XML/tagged prompt for an AI. Use it even when the user only says they want an agent to think through and create the spec first, then make it reviewable as HTML.
---

# Spec To HTML Review

Create an interactive review artifact from either an existing spec or a feature idea. The goal is not just pretty HTML; the HTML must help a human evaluate the spec and export the evaluation as a structured prompt that another AI can consume.

## When To Use

Use this skill when the user asks to:

- Convert a Markdown/text spec into HTML for review.
- Create a spec first, then make it reviewable.
- Add comments, choices, multi-select decisions, ratings, priority labels, approvals, or follow-up flags to a spec.
- Export all human evaluation results into XML/tagname format.

If the user gives only an idea, first synthesize a reviewable spec. If they provide a Markdown spec, preserve its structure and convert it.

## Core Workflow

1. Identify the input mode.
   - Existing Markdown/text spec: parse headings, lists, tables, options, open questions, and acceptance criteria.
   - Idea only: create a concise spec with assumptions, goals, user flows, requirements, options, risks, and open questions.
   - Existing HTML: preserve content and add review controls around sections.

2. Normalize the spec into sections.
   - Give every section a stable id like `sec-001`, `sec-002`.
   - Keep original wording where supplied.
   - Separate decisions from explanatory content.
   - Turn alternatives into explicit option groups.

3. Generate a single self-contained HTML file unless the user asks otherwise.
   - Inline CSS and JavaScript.
   - No build step.
   - Prefer no external dependencies; if using a CDN, explain why.
   - Must work from a local file in a browser.

4. Add review controls to each meaningful section.
   - Comments: free text notes per section.
   - Single choice: radio buttons for mutually exclusive options.
   - Multiple choice: checkboxes for compatible options.
   - Status: `approved`, `needs-revision`, `rejected`, `open-question`.
   - Priority: `must`, `should`, `could`, `wont` when relevant.
   - Rating: 1-5 for clarity, completeness, feasibility, or confidence.
   - Follow-up: flag and owner/next-action fields when useful.

5. Persist review state.
   - Store data in browser state, preferably `localStorage` for a single-file tool.
   - Include manual export/import JSON if practical.
   - Never make the user rely only on the browser tab for important work.

6. Export the final AI prompt.
   - Provide a visible `Copy XML Prompt` or equivalent action.
   - Include the original/edited spec content plus all human comments, decisions, ratings, statuses, and flags.
   - Escape XML special characters.
   - Keep the XML deterministic so it is easy to diff and reuse.

## Required XML Prompt Shape

Use this structure by default. Add fields only when they are useful for the user's review context.

```xml
<spec_review_prompt version="1.0">
  <metadata>
    <title>...</title>
    <source>provided_spec|agent_generated_spec|html_import</source>
    <created_at>ISO-8601 timestamp</created_at>
    <review_goal>...</review_goal>
  </metadata>
  <spec>
    <section id="sec-001">
      <title>...</title>
      <content>...</content>
      <review>
        <status>approved|needs-revision|rejected|open-question</status>
        <priority>must|should|could|wont</priority>
        <ratings>
          <rating name="clarity">1-5</rating>
          <rating name="completeness">1-5</rating>
          <rating name="feasibility">1-5</rating>
        </ratings>
        <comments>
          <comment>...</comment>
        </comments>
        <decisions>
          <choice group="..." type="single">selected option</choice>
          <choice group="..." type="multiple">selected option</choice>
        </decisions>
        <follow_ups>
          <follow_up owner="...">...</follow_up>
        </follow_ups>
      </review>
    </section>
  </spec>
  <summary>
    <approved_sections>0</approved_sections>
    <needs_revision_sections>0</needs_revision_sections>
    <open_questions>0</open_questions>
    <selected_options>0</selected_options>
  </summary>
  <instruction>
    Use the reviewed spec, comments, selected options, and follow-ups above as the source of truth for the next implementation or planning step.
  </instruction>
</spec_review_prompt>
```

## HTML Requirements

The generated HTML should include:

- A clear title and review progress summary.
- A table of contents or section navigation for medium/large specs.
- Section cards with the spec content and review controls close together.
- Comment boxes that can hold multiple comments per section.
- Radio groups for alternatives where only one answer is allowed.
- Checkbox groups for choices where multiple answers can be valid.
- A copy button that copies the XML prompt to the clipboard.
- A preview area showing the XML prompt before copying.
- Responsive layout for desktop and mobile.

Prefer simple, robust interactions over complex UI. A single reviewer using a local browser is the default scenario.

## Agent-Generated Spec Guidance

When the user asks the agent to think and create the spec first:

1. Ask at most two critical clarifying questions if missing details would materially change the spec.
2. If the user wants speed, make reasonable assumptions and list them in an `Assumptions` section.
3. Include these sections unless inappropriate:
   - Overview
   - Goals
   - Non-goals
   - Users / actors
   - Core workflow
   - Functional requirements
   - Data model or state
   - UI requirements
   - Options / tradeoffs
   - Acceptance criteria
   - Risks and open questions
4. Mark uncertain areas as reviewable options or open questions rather than hiding them.

## Quality Checks

Before saying the output is done, verify:

- The HTML opens without a server.
- Comments, single-select, and multi-select controls update exported XML.
- The XML escapes `&`, `<`, `>`, `"`, and `'` correctly.
- Copy-to-clipboard has a fallback for browsers that block clipboard APIs.
- Empty/unreviewed sections still export deterministically.
- The output preserves the user's original spec content when a spec was supplied.

## Common Mistakes To Avoid

- Do not export only comments and omit the spec text; the AI prompt needs both.
- Do not make selected options ambiguous; include group names and option text.
- Do not require a backend unless the user explicitly asks for collaboration or server persistence.
- Do not convert every paragraph into a separate section; group content by meaningful headings.
- Do not invent decisions as if the human selected them. Defaults should be visibly unselected unless the user gave a decision.
