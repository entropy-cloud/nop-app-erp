# 2026-08-23-1645-1-id-mission-closing-backfill id-string-migration 收尾登记与文档回填

> Plan Status: superseded
> Last Reviewed: 2026-08-23
> Superseded by: `docs/plans/2026-08-23-1752-1-id-string-mission-closure-backfill.md`（同范围，`Plan Status: active`，已通过两轮独立草案审查；两份源审计 `> Audit Status: planned` 分流指针均指向该计划）
> Source: 双独立并审审计 P1 义务——`docs/audits/2026-08-22-0702-open-audit-id-string-migration.md`（发现 1/2/3）+ `docs/audits/2026-08-22-0702-multi-audit-id-string-migration.md`（发现 1/2/3；两审计 P1 集合相同，本计划按去重后 3 项覆盖全部 P1）
> Related: `docs/backlog/id-string-migration-roadmap.md`（mission roadmap，已完成态）
> Audit: required

## Current Baseline

（2026-08-23 本会话 live 复核，三项 P1 目标均仍处于未修复态——与两审计落盘时点一致，两审计间零兑付）

- **P1-1（状态回填缺口）**：`docs/backlog/README.md:70` 行为 `| P0 | 主键/外键 stdDataType string 化迁移（全 19 域 1605 列 PK/FK long→string…） | id-string-migration-roadmap.md | todo | plan-first（ORM 保护区域…） |`——mission 实际已完成（roadmap 头部「MISSION 完成 🎉 M0-M4 全 done」+ 依赖图全 done + 登记册 259/259 retired），但 README 行仍 `todo` 且计数 1605 为 08-16 废止口径（权威 1662 = M0 审计 §1 + 工具 scan）。README 同表内已完成 mission 惯例标 `✅ done`（30+ 行）。
- **P1-2（契约模板漂移）**：`docs/architecture/data-dependency-matrix.md:632`（§5.6.4 标准代码模式 (b)，notGenCode 外部实体声明模板）仍为 `<column name="id" code="ID" stdSqlType="BIGINT" primary="true" stdDataType="long"/>`——实况权威源已全量翻转（如 `module-finance/model/app-erp-finance.orm.xml:2245` 同名 stub ErpMdSubject 现为 `stdDataType="string"`）。该节是新增跨域引用的复制粘贴入口：按模板执行会重新引入 mission 消灭的「stub↔权威源 stdDataType 错配 → `_gen` to-one 胶水对称编译破坏」失败模式（M0 审计 §10.1）。
- **P1-3（承诺的 follow-up 无登记实体）**：`docs/backlog/id-string-migration-roadmap.md:15` 声明「孤儿操作人列建模问题已登记 follow-up，另案裁决」，但全 `docs/` 树 grep「孤儿操作人」命中仅 roadmap 自身 + 各域 plan 执行记录 + 日志 + 审计文件——`docs/backlog/README.md`、其他 roadmap、`docs/discussions/`、`docs/analysis/` 零登记。约 15 个操作人列（completedBy/assignedTo/verificationPerson/signedBy/responsiblePerson/resolvedBy/requestedBy/reconciledBy/acceptedBy）在 String-id 世界保持 Long（规则 4 合法排除），已产生运行时桥产物（如 `ErpMntVisitReportAdditionalFaultProcessor` `toLongUserId` try-catch 桥）。「已登记」声明不被证据支持。
- **两份源审计**当前 `> Audit Status: open`（本起草批次已按 mission driver 规则转为 `planned`）；审计尾部自注「三项均为单文件/单行级编辑，完成后可关闭」。
- **P2 处置已在本起草批次完成**（不属本计划范围）：两审计 P2 去重 4 项已汇集至 roadmap 新增 `## Follow-up Backlog` 节（含来源审计路径），P2 不立项。

## Goals

- backlog 登记状态与 mission 实况一致：README 行回填 `✅ done` + 计数对齐 1662（P1-1）。
- 架构 owner-doc 契约模板与实况权威源一致：§5.6.4(b) 示例 id 列 String 化 + 方案 B 注记，杜绝失败模式经文档复活（P1-2）。
- roadmap「已登记 follow-up」声明获得真实登记实体：README 补孤儿操作人列 watch 行（含触发条件）（P1-3）。
- 修复落地后将两份源审计转入关闭态并附证据指针。

## Non-Goals

- 不修改任何生产代码 / ORM 模型 / 契约 / 测试 / E2E（三项 P1 均为文档与登记回填）。
- 不处理 P2 项（已汇集 roadmap `## Follow-up Backlog`，其中 E2E TS 类型注解批处理等归后续 backlog 顺带清理）。
- 不重开孤儿操作人列建模裁决本身（仅登记 watch 实体；实际建模变更在触发条件满足时另案立项）。
- 不改动 roadmap 既有历史记录段（Draft Review Record 等）。

## Task Route

- Type: `implementation-only change`（纯文档/登记回填，无代码/模型/契约变更）
- Owner Docs: `docs/architecture/data-dependency-matrix.md`（P1-2 修复对象）、`docs/backlog/README.md`（P1-1/P1-3 修复对象）、`docs/backlog/id-string-migration-roadmap.md`（P1-3 声明来源）
- Skill Selection Basis: `Skill: none`——本计划为登记/文档回填，不触及 ORM 模型、BizModel、view、测试编写等技能覆盖面（nop-backend-dev/nop-testing/nop-frontend-dev 均不匹配）；无匹配技能时按 AGENTS.md 强制扫描规则记录扫描结果。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 — 收尾登记与文档回填（Fix-heavy，5/5 items tagged Fix）

Status: planned
Targets: `docs/backlog/README.md`, `docs/architecture/data-dependency-matrix.md`, `docs/backlog/id-string-migration-roadmap.md`, `docs/audits/2026-08-22-0702-open-audit-id-string-migration.md`, `docs/audits/2026-08-22-0702-multi-audit-id-string-migration.md`, `docs/logs/2026/08-23.md`
Skill: none

- Item Types: `Fix`
- Prereqs: 无（三项 P1 相互独立，可同批落地）

- [ ] Fix（P1-1）：`docs/backlog/README.md:70` 行状态 `todo` → `✅ done`，描述中废止计数 1605 对齐权威口径 1662（或注明权威口径见 roadmap），补 plan/完成时间注记对齐同表惯例
      - Skill: none
- [ ] Fix（P1-2）：`docs/architecture/data-dependency-matrix.md` §5.6.4(b) 模板 id 列 `stdDataType="long"` → `stdDataType="string"`，并在示例邻近加一行注记（Java 层 String / DB 层 BIGINT 保持，方案 B，对齐 `orm-model-design.md` §主键设计与 19 域实况）
      - Skill: none
- [ ] Fix | Decision（P1-3）：孤儿操作人列 follow-up 实体化登记——在 `docs/backlog/README.md` 补一行 watch/follow-up 工作项（含触发条件：操作人列需 FK 化/审计实体化时另案裁决；对齐同表 `watch-only residual` 行惯例），使 roadmap:15「已登记 follow-up」声明为真
      - Decision 记录：选择「补登记实体」而非「roadmap 措辞降级为建议未来登记」——登记实体保留建模债的可路由性与触发条件，措辞降级仅移除虚假声明不还债；残留风险：watch 行长期不触发则持续为 watch-only（可接受，同表既有先例）
      - Skill: none
- [ ] Fix：三项落地后将两份源审计 `> Audit Status: planned` 更新为关闭态（对齐审计尾部自注「完成后可关闭」），附修复证据指针（README 行/模板行/登记行）
      - Skill: none
- [ ] Fix：更新 `docs/logs/2026/08-23.md` 日条目（含验证状态）
      - Skill: none

Exit Criteria:

- [ ] `docs/backlog/README.md` 该行 grep 复核：状态 `✅ done` + 计数 1662（或权威口径指针），无 `todo` 残留；孤儿操作人列 watch 行在表且含触发条件
- [ ] `docs/architecture/data-dependency-matrix.md` §5.6.4(b) grep 复核：示例 id 列 `stdDataType="string"` + 方案 B 注记在位
- [ ] 两份源审计状态行不再为 `open`/`planned`，含证据指针

## Draft Review Record

- Independent draft review iteration 1: needs revision → **superseded**（2026-08-23 草案审查，live 复核）— **BLOCKER：同范围重复立项**。审查复核全部基线事实为真（README:70 `todo`+1605、matrix:632 `stdDataType="long"`、roadmap:15「已登记」声明全树零登记实体、finance orm:2245 stub 已 `string`），但发现同日更晚批次计划 `2026-08-23-1752-1-id-string-mission-closure-backfill.md` 覆盖完全相同的范围（P1×3 + 审计关闭 + 日志），已完成两轮独立草案审查（iteration 2: accept）并处于 `Plan Status: active`；两份源审计的 `Audit Status: planned` 分流指针均指向 1752-1 而非本计划。若将本计划转 active 将形成双 active 同结果表面（违反计划指南最低规则 4），并产生双执行与闭包证据冲突。次要问题（与 1752-1 迭代 1 审查发现同源）：① Current Baseline 对审计状态表述自相矛盾（`open` 与 `planned` 并述）；② Phase 头 `Fix-heavy 5/5` 与 Item 3 `Fix | Decision` 标注计数不一致且缺 Proof 项。处置：按计划指南状态流将本计划标记 `superseded`，唯一执行载体为 1752-1；全仓 grep 确认无其他文件引用本计划 id，supersede 零断链。

## Closure Gates

> 本计划为纯文档/登记回填，无代码变更——删除验证命令门控（无 typecheck/build/test 适用对象），以逐项 grep/read-back 复核替代。

- [ ] 范围内行为完成（3 P1 全部落地）
- [ ] 相关文档对齐（README 行、架构模板、roadmap 声明-实体一致）
- [ ] 逐项复核已运行（Exit Criteria 三条 grep 复核）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

（无）

## Closure

Status Note: 本计划被同范围计划 `2026-08-23-1752-1-id-string-mission-closure-backfill.md` 取代（见 Draft Review Record），无独立结束义务；其结束证据由 successor 计划持有。

Closure Audit Evidence:

- Auditor / Agent: 不适用（superseded，无执行与结束）
- Evidence: 见 Draft Review Record 迭代 1 的 live 复核记录

Follow-up:

- （无；P2 项已在起草批次汇集至 roadmap `## Follow-up Backlog`，非本计划范围）
