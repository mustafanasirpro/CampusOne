# AURA Import Guide

AURA imports scheduling data through a preview, validation, and apply workflow.
Only an authenticated AURA administrator for the target university can import.

## Supported files

| Format | Behavior |
|---|---|
| CSV | Header-based row import |
| XLS/XLSX | Visible-sheet selection and compatible visible-sheet aggregation |
| Text PDF | Extracts table-like text |
| Scanned PDF | Returns `OCR_REQUIRED`; OCR is not performed |

Workbook imports propagate merged values within a merged region, which supports
common vertically grouped row layouts. When **All visible sheets** is selected,
each non-empty sheet must use the same normalized headers. Formula cells are
rejected; replace formulas with displayed values before upload.

## Workflow

1. Choose the term, import type, and file.
2. For workbooks, choose one visible sheet or **All visible sheets**.
3. Review detected headers, sample rows, warnings, and suggested mapping.
4. Map every required AURA field to a source column.
5. Validate the complete file and correct row-level issues.
6. Apply only after every required row is accepted.

Application is transactional. A failed apply must not leave partially imported
scheduling data.

## Safety limits

- Filename extension and file signature must agree.
- Empty, malformed, password-protected, oversized, binary CSV, and excessive
  row/sheet/cell inputs are rejected.
- Cell content is bounded and control characters are rejected.
- Spreadsheet formulas are never evaluated or imported.
- University and term scope come from authenticated server-side checks.

## Mapping guidance

Use stable human identifiers such as course code, section name, employee code,
room code, and timeslot/day values. Preview warnings and validation errors name
the row and field that need attention. Unknown references must be created or
corrected before apply.

Current layout limitations are recorded honestly in
[AURA_COMPLETION_LEDGER.md](AURA_COMPLETION_LEDGER.md).
