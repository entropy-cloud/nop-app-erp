# 2026-08-03-1232-5-flux-docs-drift-and-patterns 前端范式文档 Flux 化与漂移回填

> Plan Status: draft
> Last Reviewed: 2026-08-03
> Source: 用户决策（2026-08-03）——界面全面转向 nop-chaos-flux；`2026-08-03-1000` 深度分析 §6.3-6.4（文档缺口与漂移）
> Related: `2026-08-03-1232-1/2/3/4`（实施计划，输入来源）
> Audit: required

## Current Baseline

- **4 处已确认文档漂移**（`2026-08-03-1000` §6.4，实现与文档冲突）：
  - `non-standard-views-patterns.md` §3.1/§4.1 宣称原生 timeline/calendar 可用，实现证据相反（crm/cs timeline prop 失败 → each+tpl；crm calendar React #130 → 卡片网格）——P2 重写后范式将再变（flux 原生），本计划统一更新为最终形态
  - `human-resource/ui-patterns.md` 设计 AMIS tree（org-chart :142），实现降级缩进列表——P2 后为 flux tree
  - `aps/ui-patterns.md` 设计含拖拽/右键菜单（:167-168），实现只读（§8.7 已裁决 Non-Goal）——P3 后拖拽实现、右键菜单残留（flux gantt 无 onContextMenu）
  - **`b2b/ui-patterns.md` ASN 五阶段流程条**（锚点 :10,158,160,184-185,259,290,298 全文穷尽，含跨页面导航流 :259「ASN 详情(五阶段流程条)」与 :23 流程条——**裁决：4 值仍以步骤条呈现则保留「流程条」措辞，仅改阶段数**），实现为 4 值字典（RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED，`2026-08-03-1000` §4.10/§8.12 已裁决）——**已确认漂移，必须回填**
- **文档缺口**（`2026-08-03-1000` §6.3）：
  - notify 域无 ui-patterns.md（`notify/inbox-patterns.md` 仅覆盖收件箱——**缺口是通知模板管理、偏好设置的页面设计无 owner 文档**；偏好设置按 `notify/README.md` 标注 out-of-baseline 需 ORM 变更）
  - SPC 独立页交互设计浅（`quality/spc.md` 偏数据模型）
  - 看板实现细节无文档（`dashboards.md` 只定义指标）
  - **page.yaml 层 i18n 空白**（F15 延后项：view.xml 层已完成 351 文件，page.yaml 无 i18n 机制）
- **范式文档现状**（AMIS 视角，需 flux 化）：
  - `page-structure-patterns.md`（F12/F16：tabs 机制 A/B、仪表板、wizard、12 类复杂页范式）
  - `non-standard-views-patterns.md`（F13：看板/时间线/日历）
  - `child-table-editor-patterns.md`（F4：子表行内编辑 §16 凭证变体）
  - `tree-entity-patterns.md`（F10：树形 CRUD 三件套）
  - `cross-doc-navigation-patterns.md`（F9：关联单据 drawer）
  - `picker-patterns.md`（7 高频 picker 范式）
  - `visible-on-patterns.md`、`query-filter-patterns.md`、`field-formatting-patterns.md`、`batch-operation-patterns.md`、`status-color-map.md`、`voucher-back-link-patterns.md`
  - 18 域 `ui-patterns.md`
- **路线图**：`docs/backlog/frontend-ui-roadmap.md` 的「Flux 渲染引擎备选」段需更新为「全量迁移」决策（用户 2026-08-03 决策：后续不再考虑 AMIS）
- **设计文档**：`docs/design/flux-complex-pages.md`（409 行，本系列计划的设计依据，§2 渲染机制需按 P1 实测更新）

## Goals

- 4 处文档漂移按各自最终形态回填（3 处 flux 原生——依赖 P2/P3 落地；b2b ASN 4 值——仅依赖 §8.12 裁决与实现证据，**不受 P2/P3 门控**），消除「文档宣称 vs 实现」冲突
- 范式文档（page-structure-patterns/non-standard-views-patterns 等）以「flux 实现为权威」重构：每范式给出 flux schema 骨架 + 数据契约 + 事件持久化，AMIS 实现降级为历史注记（**无行号短摘要形态**，保留参考价值但不作为权威）
- 18 域 ui-patterns 中涉及复杂页的条目更新为 flux 控件映射
- 文档缺口补齐：notify 模板管理设计（或裁决走标准 CRUD 范式无需独立文档）、SPC 页面交互设计（基于 flux chart referenceLines/band/markers 能力）、看板三段式 page.yaml 范式沉淀
- `frontend-ui-roadmap.md` 决策更新：全量 flux 迁移 + AMIS 退役路径（引用 P1 的 Follow-up）
- page.yaml 层 i18n 方案登记（flux `12-i18n.md` 的 initFluxI18n/t()，作为复杂页迁移的横切要求）

> **前置条件声明**：本计划以「flux 为权威」的翻转依赖 P1 Phase 3 的默认渲染模式 Decision 落地为「flux 全量」（用户 2026-08-03 决策明确后续不再考虑 AMIS，P1 决策预期一致）。若 P1 决策意外落地为「amis 双栈共存」，本计划前提崩塌，须暂停重审。

## Non-Goals

- 实施 P1-P4 的页面重写（本计划只改文档）
- nop-chaos-flux 仓库的控件扩展（SPC chart 已实现，仅消费）
- 新建范式文档（若既有范式可 flux 化则更新，不新增平行文档）

## Task Route

- Type: `verification or audit work`（文档一致性）+ `app-layer design change`（范式重构）
- Owner Docs: 全部 `docs/design/*.md` 前端范式 + 18 域 ui-patterns + `docs/backlog/frontend-ui-roadmap.md` + `docs/index.md`（若索引需更新）
- Skill Selection Basis: authoring 项记 `Skill: none`（文档编写非审计方法）；`document-audit-prompt` 仅用于 Phase 0 一致性扫描项（审计基线、按严重性返回发现）；范式重构的基线正确性由实施证据 + 结束审计把关

## Infrastructure And Config Prereqs

- 前置：P1-P4 完成（本计划以实施结果为准更新文档；若并行，需 P1 的渲染机制实测结论先行）
- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 0 - 漂移回填与一致性基线

Status: planned
Targets: `docs/design/non-standard-views-patterns.md`、`docs/design/human-resource/ui-patterns.md`、`docs/design/aps/ui-patterns.md`、`docs/design/b2b/ui-patterns.md`
Skill: `document-audit-prompt`

- Item Types: `Fix`（文档漂移回填）
- Prereqs: P2/P3 相关页完成（或与实施同步，以实施证据为准）

- [ ] 回填 4 处漂移：non-standard-views §3.1/§4.1（timeline/calendar 原生 vs 实现裁决，最终为 flux 原生）、hr ui-patterns org-chart（flux tree）、aps ui-patterns 甘特（拖拽已实现，右键菜单残留标注）、**b2b ui-patterns ASN 五阶段→4 值字典**（§8.12 裁决 + 实现证据；锚点 :10,23,158,160,184-185,259,290,298 全文穷尽，4 值仍以步骤条呈现则保留「流程条」措辞）
      - Skill: none
- [ ] 全文一致性扫描：引用实现文件路径/行号的段落核对（吸取 `2026-08-03` 审计中 moveStage 行号传播错误的教训）
      - Skill: `document-audit-prompt`

Exit Criteria:

- [ ] 4 处漂移已回填为最终形态（flux 原生 + b2b 4 值），实现证据引用准确
- [ ] 扫描发现的残留引用错误已修复

### Phase 1 - 范式文档 Flux 化

Status: planned
Targets: `docs/design/page-structure-patterns.md`、`docs/design/non-standard-views-patterns.md`、`docs/design/child-table-editor-patterns.md`、`docs/design/tree-entity-patterns.md`、`docs/design/cross-doc-navigation-patterns.md`、`docs/design/picker-patterns.md`、`docs/design/visible-on-patterns.md`、`docs/design/field-formatting-patterns.md`、`docs/design/status-color-map.md`
Skill: none

- Item Types: `Fix`（范式重构）+ `Add`（flux schema 骨架）
- Prereqs: P1-P3 实施证据（各页 flux 输出/测试）

- [ ] 核心范式（page-structure/non-standard-views/child-table-editor）重构：每模式 = 业务语义 + flux schema 骨架 + 数据契约（{items,total}） + 事件持久化；AMIS 实现标注为历史注记（无行号短摘要）
      - Skill: none
- [ ] 辅助范式（tree/picker/visible-on/field-formatting/status-color-map）更新为 flux 控件映射（tree-select/status labelMap/mapping 等）
      - Skill: none
- [ ] 沉淀「看板三段式 page.yaml」范式（data-source + card + chart + crud，复用度 11 页）——落入 `page-structure-patterns.md` §3（仪表板）或显式声明目标文件；核对 `dashboards.md` 同步
      - Skill: none

Exit Criteria:

- [ ] 范式文档以 flux 为权威，AMIS 历史注记明确不误导
- [ ] 看板三段式范式已沉淀（owner doc 级，目标文件明确）

### Phase 2 - 域 ui-patterns 与缺口补齐

Status: planned
Targets: 18 域 `docs/design/<domain>/ui-patterns.md`、`docs/design/notify/`、`docs/design/quality/spc.md`
Skill: none

- Item Types: `Fix`（ui-patterns 更新）+ `Add`（缺口文档）
- Prereqs: Phase 1

- [ ] 18 域 ui-patterns 中复杂页条目更新为 flux 控件映射（引用 `flux-complex-pages.md`）；**b2b ASN 五阶段在 Phase 0 已回填，此处核对一致性**
      - Skill: none
- [ ] notify 缺口闭合（二选一）：(a) 新增 `notify/ui-patterns.md`（模板管理页设计；偏好设置按 `notify/README.md` 标注 out-of-baseline 需 ORM 变更）or (b) 裁决「模板管理走标准 CRUD 范式无需独立文档」——记录理由
      - Skill: none
- [ ] SPC 页面交互设计补齐（基于 flux chart referenceLines/band/markers 完整能力；输入来源 `flux-complex-pages.md` §7 #1 与 P4 实施结果，属回填性质）
      - Skill: none
- [ ] 一致性核对：引用核对扫描（Phase 0 模式复用，含 P4 占位页状态裁决）

Exit Criteria:

- [ ] 18 域 ui-patterns 复杂页条目 flux 化完成（含引用核对扫描通过）
- [ ] notify 缺口以 (a) 或 (b) 明确闭合，SPC 交互设计已补齐（回填性质，输入来源注明）

### Phase 3 - 路线图与 i18n 决策

Status: planned
Targets: `docs/backlog/frontend-ui-roadmap.md`、`docs/design/flux-complex-pages.md`
Skill: none

- Item Types: `Fix`（路线图决策更新）+ `Follow-up`
- Prereqs: Phase 0-2 + P1 的默认渲染模式 Decision

- [ ] roadmap「Flux 渲染引擎备选」→「Flux 全量迁移」决策更新（引用用户 2026-08-03 决策 + P1-P4 结果 + AMIS 退役路径，含 P1 Phase 3 的 AMIS 退役 Follow-up 触发条件）
      - Skill: none
- [ ] `flux-complex-pages.md` 更新为实施后一致：§2 渲染机制更新为 P1 实测结论（impl_flux_mode.xpl 机制、菜单 component 翻转）；**§1.2「决策依据（roadmap 已定）」中引用的旧决策（标准 CRUD 继续 view.xml→amis、97.6% 零修改）与新决策矛盾，一并更新**
      - Skill: none
- [ ] Follow-up: page.yaml 层 i18n 方案（flux `12-i18n.md` initFluxI18n/t()）登记为复杂页迁移横切要求，触发条件 = P2/P3 页面重写时
      - Skill: none

Exit Criteria:

- [ ] roadmap 决策更新完成（含退役路径）
- [ ] flux-complex-pages.md 与实施证据一致
- [ ] i18n 方案登记完成

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_03a096aaefferF2yyn0Bj8dLwv) — b2b ASN 第 4 处漂移漏报(B1)、notify 缺口表述(B2)、Skill 与方法不匹配(M2)、前置条件声明(M1)；已全部修订
- Independent draft review iteration 2: needs revision (ses_039fd27a1ffejAEBpdQidn5xUm) — Phase 0 回填项 Skill 残留（audit skill 仅给扫描项）、Task Route Skill Selection Basis 未说明取舍、b2b 锚点补 :160/:298；已全部修订
- Independent draft review iteration 3: needs revision (ses_039f01d48ffeYukPBZ8OcAEkoT) — b2b 锚点漏 :259（全文穷尽证伪）、Baseline 锚点不对称、b2b 回填与 P2/P3 错误绑定、Goals 与 Phase 3 对齐 P1 Follow-up；已全部修订

## Closure Gates

- [ ] 范围内行为完成（漂移回填 + 范式 flux 化 + ui-patterns 更新 + roadmap 决策）
- [ ] 相关文档对齐（与 P1-P4 实施结果一致）
- [ ] 已运行验证（文档一致性扫描 + 引用路径核对；无代码变更故验证门控为文档审查）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### AMIS 范式文档的物理移除

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: AMIS 历史注记保留在范式文档中供参考（迁移审计可追溯）；物理移除需全部 P1-P4 完成后统一评估
- Successor Required: `no`

### page.yaml i18n 全量实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅登记方案与触发条件（F15 延后项）；全量实施需 P2/P3 页面重写时随附
- Successor Required: `yes`（随 P2/P3）

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
