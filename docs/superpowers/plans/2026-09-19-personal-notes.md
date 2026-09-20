# Personal Notes Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan in this session. User explicitly requested starting implementation; continue without another authorization round.

**Goal:** Build a working Android notes APK with selectable scrollable home widgets, inline hololive stickers, colorful glowing text, and faded photo backgrounds.

**Architecture:** Kotlin platform Activities, local SQLite, serializable styled text, and RemoteViews. A shared bitmap renderer produces line-aligned content tiles so the widget supports the same rich text as the editor. Widget bindings are independent of note records.

**Tech Stack:** Kotlin, Android SDK 35, AGP 8.11.1, Gradle 8.13, JDK 17; Android 8/API 26 minimum.

**Spec:** ../specs/2026-09-19-note-widget-design.md (including latest authorized personalization addendum).

## Global Constraints

- Traditional Chinese UI; local persistence; no login or runtime Internet permission.
- Preserve readable text size and vertical scrolling; no continuously animated widget effects.
- Source image imports must survive restarts and must not modify original files.
- Do not claim actual launcher verification unless performed.

## Review Focus

- Formatting overlapping selections must preserve unaffected styles; unit tests on StyleRanges.
- Emoji and long paragraphs must survive tile rendering without text loss; renderer instrumentation tests.
- Missing/deleted notes and canceled configuration must never display another note; repository/configuration tests and manual checks.
- High-resolution imported photos must be sampled before decoding; image importer bounds and invalid input checks.
- Rotation and process restoration must preserve unsaved text/styles; Activity saved-state path and device check.

## Task 1: Document model, persistence, and build

Files: root Gradle configuration, app/build.gradle.kts, model/StyleRanges.kt, data/Note.kt, data/NoteStore.kt, test/StyleRangesTest.kt.

- [ ] Configure application `tw.local.memonote`, SDK 35/min26, JDK17, Kotlin.
- [ ] Write tests for `StyleRanges.apply(ranges, start, end, patch)` with overlapping colors, rainbow/glow combination, and clearing.
  Example contract: formatting `[2,4)` red within blue `[0,6)` yields blue `[0,2)`, red `[2,4)`, blue `[4,6)`.
- [ ] Run test, observe missing implementation failure; implement immutable style ranges and verify tests pass.
- [ ] Implement SQLite records and JSON rich-text codec with explicit stored version. Repository signatures: `all(): List<Note>`, `find(id: Long): Note?`, `save(note: Note): Long`, `delete(id: Long)`.

## Task 2: Shared renderer and assets

Files: rich/RichText.kt, rich/NoteRenderer.kt, data/ImageFiles.kt, assets/stickers/*, assets/stickers.json, ASSET_SOURCES.md.

- [ ] Obtain publicly downloadable chibi assets for personal use, retain source provenance. Import custom sticker support remains available.
- [ ] Create editable spans from rich text JSON; preserve selected colors, gradients, glow and inline image positions across save/load.
- [ ] Use StaticLayout to generate line-aligned tiles bounded in height; expose `renderTile(index): Bitmap` with matching spoken text. No complete long-note bitmap allocation.
- [ ] Add tests for multiline Chinese/emoji text, overlapping spans, inline sticker roundtrip, and tile boundaries.
- [ ] Background renderer supports presets and sampled imported photos, alpha `1f - fade / 100f`.

## Task 3: Notes UI

Files: MainActivity.kt, EditorActivity.kt, ui/Ui.kt, ui/StickerPicker.kt.

- [ ] Build note cards, empty state, new/edit/delete flows and widget setup guidance.
- [ ] Editor has title/body, selection color tools, rainbow/glow toggles, clear styles, sticker palette, background controls, preview, save.
- [ ] SavedInstanceState serializes entire draft; explicit dirty-exit dialog protects changes.
- [ ] Image picker imports to private files with bounded decoding; cancellation preserves previous background.
- [ ] Failed saves retain draft and show retry message. Delete requires confirmation.

## Task 4: Real Android widget

Files: widget/NoteWidgetProvider.kt, widget/NoteWidgetService.kt, WidgetConfigActivity.kt, res/layout/widget*.xml, res/xml/note_widget_info.xml, AndroidManifest.xml.

- [ ] Bind each widget ID separately to one note ID; canceled initial configuration returns RESULT_CANCELED.
- [ ] Render fixed background with header and scrollable tile ListView. Use identity-bearing service intent per widget to avoid content crossover.
- [ ] Handle resize, update, remove, missing note, reconfigure and edit intents.
- [ ] After save/delete, update relevant widgets; show retry/choose message for missing data rather than stale content.
- [ ] Bound each bitmap and use on-demand collection loading to keep IPC and memory small.

## Task 5: Verification and delivery

Files: app/src/androidTest/*, README.md, docs/verification.md, build-local.ps1.

- [ ] Run unit tests, lint and assembleDebug with workspace-local toolchain.
- [ ] If accessible, use Android emulator to run persistence/rendering instrumentation and inspect editor/widget screenshots.
- [ ] Review source independently while packaging docs; resolve correctness findings and rerun affected checks.
- [ ] Deliver APK at `output/MemoNote-debug.apk`, full source, setup/use instructions and explicit verification limits.

## Execution record

- User authorized implementation and all three personalized features in the latest message.
- Workspace has no existing repository; implement directly in this empty project, no unrelated checkout changes.
- Static rainbow/glow is the initial interpretation; background fade modifies appearance only.

## Progress and review record

- Model tests: observed four intended failures before StyleRanges implementation; all five pass afterward.
- App, rich text, widget, asset imports and editor implemented.
- Independent reviewer identified unbounded software layer, Binder draft size, sticker aspect ratio and EXIF handling. Each was reproduced by device tests and fixed; all eight device tests pass on API30 after fixes.
- Draft recovery now uses AtomicFile and small saved-state references. Software layer removed; hardware text effects retained. Sticker width and height fit a square; photo transforms honor EXIF.
- Static glow/rainbow interpretation and per-note backgrounds retained from announced design. No deferred review findings.
- Local SDK/JDK/Gradle installed under .tools. No existing Git repository to commit into. Wrapper and source archive will make the delivered project portable.

## Delivery complete — 2026-09-20

All five implementation tasks are complete. Final build/lint passed, 5 JVM tests passed, and 8 device tests passed with the final test runner. Actual Launcher3 checks covered pinning, selecting, resizing to the full grid width, reaching the last line, switching notes, and edit/save refresh. APK signature verified. Deliverables are in output; detailed limits are recorded in docs/verification.md.
