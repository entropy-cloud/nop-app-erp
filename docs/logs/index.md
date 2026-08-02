# Logs Index

Use `docs/logs/YYYY/MM-DD.md` for daily implementation notes.

Start each new day by creating the dated file if it does not already exist.

Example:

- `docs/logs/2026/05-21.md`

Current:

> **机制说明（Decision，P1-MA3-054）**：完整最新列表以 `docs/logs/2026/` 目录为准（文件按 `MM-DD.md` 命名，`ls`/文件管理器自然按日期排序）——不再逐条手工登记全部日志（曾导致索引停在 06-25 而目录已至 07-31 的过时漂移）。下方仅保留**最近若干条**导航锚点；新增日志时追加到列表顶部，旧条目按需滚动移除。自动化逐日刷新（生成脚本）为 successor，当前目录即真相源。

- `docs/logs/2026/07-31.md` — 审计-修复 MR2 R2.7 API 契约一致性（4 Phase 全 done）
- `docs/logs/2026/06-25.md` — AI 自动化开发就绪度深度分析 + 4 阶段 Roadmap
- `docs/logs/2026/06-22.md` — project bootstrap (AGE init + ORM skeleton)
