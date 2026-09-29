---
target: frontend/src/App.tsx
total_score: 25
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
target_identity: "file:C:\\Desenvolvimento\\ProjetoIntegrador-ADS2026\\frontend\\src\\App.tsx"
target_fingerprint: "sha256:ff4ee46ce51ff25c0feedde02eb459c1e0558a1a9c3e1c9b59161d95ce219028"
target_path: "C:\\Desenvolvimento\\ProjetoIntegrador-ADS2026\\frontend\\src\\App.tsx"
timestamp: 2026-09-29T14-59-50Z
slug: frontend-src-app-tsx
---
Method: dual-agent (A: design review · B: detector + browser)

## Design Health Score (25/40, Acceptable)
| # | Heuristic | Score | Key issue |
|---|---|---|---|
| 1 | Visibility of system status | 3 | Job list said only "Candidatura enviada", without the stage |
| 2 | Match system / real world | 3 | "Encerrar candidatura" (reject a person) sounded like "Encerrar vaga" |
| 3 | User control and freedom | 2 | Rejecting a candidate had no confirmation |
| 4 | Consistency and standards | 2 | Close vaga confirmed, reject person did not; mixed button heights and insets |
| 5 | Error prevention | 2 | Apply with empty CV; the mobile warning was hidden under the fixed bars |
| 6 | Recognition rather than recall | 3 | Main HR action (ver candidatos) only in the kebab menu |
| 7 | Flexibility and efficiency | 2 | HR rows not clickable; no sorting/bulk |
| 8 | Aesthetic and minimalist | 3 | AI banner and permanent save bar competed with content |
| 9 | Error recovery | 3 | Login and upload errors good; network failure surfaced "Failed to fetch" |
| 10 | Help and documentation | 2 | Resume fields without examples; no password recovery (RF03) |

## Design specificity
Shell (glass nav, glows, orb, Fustat) is authored; screen interiors were generic shadcn. Detector CLI: 0 findings. Browser detector: white on brand green 4.29:1 (all views), placeholder 3.4:1, HR count 3.1:1, skipped-heading (h1→h3), oversized-h1 on login (pinned by brief: false positive), dark-glow on orb/buttons (intentional).

## Priority issues
- [P1] Mobile job detail: sticky apply bar hid the empty-CV warning.
- [P1] HR reject candidate: no confirmation, no undo.
- [P1] HR vagas: rows look clickable but aren't; "Ver candidatos" buried.
- [P2] Touch targets under 44px on mobile (segmented 36, theme 34, password 36, tag × 22, back link 36, bell 32, link 32).
- [P2] Hidden horizontal scroll in the modality filter at ≤390px.
- [P2] Applications: no next step; approval/rejection not marked.
- [P2] Floating chrome relies on backdrop blur; save bar always visible.
- [P3] Stage track misaligned with card text; card insets 18–26px vary.
- Login: vertical seam from the animated orb's outer shadow.

## Persona red flags
Casey: applies blind with empty CV; clipped filter; 22–36px targets. Jordan: no CV nudge on job list. Sam: h3 inside button, skipped headings, 4.29:1 buttons. Alex: two clicks to candidates, no confirm on reject.
