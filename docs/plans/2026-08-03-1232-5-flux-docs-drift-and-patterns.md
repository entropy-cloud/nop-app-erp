# 2026-08-03-1232-5-flux-docs-drift-and-patterns 前端范式文档 Flux 化与漂移回填

> Plan Status: completed
> Last Reviewed: 2026-08-05
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
- 范式文档（page-structure-patterns/non-standard-views-patterns 等）以「**view.xml 模型驱动 + flux 输出**」为权威重构：每范式给出 view.xml 模型定义（`<pages><complex>` 四槽位/tabs/group/wizard 容器 + grids/forms）+ flux schema 输出 + 数据契约 + 事件持久化；AMIS 实现降级为历史注记（**无行号短摘要形态**，保留参考价值但不作为权威）；flux 专有控件（gantt/kanban/calendar）给出 complex 槽位内嵌 + flux.yaml 直写组合模式
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

Status: completed
Targets: `docs/design/non-standard-views-patterns.md`、`docs/design/human-resource/ui-patterns.md`、`docs/design/aps/ui-patterns.md`、`docs/design/b2b/ui-patterns.md`
Skill: `document-audit-prompt`

- Item Types: `Fix`（文档漂移回填）
- Prereqs: P2/P3 相关页完成（或与实施同步，以实施证据为准）

- [x] 回填 4 处漂移：non-standard-views §3.1/§4.1（timeline/calendar 原生 vs 实现裁决，最终为 flux 原生）、hr ui-patterns org-chart（flux tree）、aps ui-patterns 甘特（拖拽已实现，右键菜单残留标注）、**b2b ui-patterns ASN 五阶段→4 值字典**（§8.12 裁决 + 实现证据；锚点 :10,23,158,160,184-185,259,290,298 全文穷尽，4 值仍以步骤条呈现则保留「流程条」措辞）
      - Skill: none
      - 证据：4 文件均落地「文档回填注记」标注旧形态与裁决来源；b2b 锚点全量改 4 值（RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED），保留「流程条」措辞仅改阶段数；aps 标注右键菜单 flux gantt 无 onContextMenu 残留 successor；hr §3.3/§402-404 改 flux tree；non-standard-views §3.1/§4.1 改 flux 原生。
- [x] 全文一致性扫描：引用实现文件路径/行号的段落核对（吸取 `2026-08-03` 审计中 moveStage 行号传播错误的教训）
      - Skill: `document-audit-prompt`
      - 证据：扫描发现 non-standard-views-patterns.md §5.1/§7 三处 BizModel 行号过期（crm moveStage :84→:112、cs 状态 mutation :109-242→:103-174、prj :103-163→:108-157，经实时仓库 grep ErpCrmLeadBizModel/ErpCsTicketBizModel/ErpPrjTaskBizModel 核实），已修正。

Exit Criteria:

- [x] 4 处漂移已回填为最终形态（flux 原生 + b2b 4 值），实现证据引用准确
- [x] 扫描发现的残留引用错误已修复

### Phase 1 - 范式文档 Flux 化

Status: completed
Targets: `docs/design/page-structure-patterns.md`、`docs/design/non-standard-views-patterns.md`、`docs/design/child-table-editor-patterns.md`、`docs/design/tree-entity-patterns.md`、`docs/design/cross-doc-navigation-patterns.md`、`docs/design/picker-patterns.md`、`docs/design/visible-on-patterns.md`、`docs/design/field-formatting-patterns.md`、`docs/design/status-color-map.md`
Skill: none

- Item Types: `Fix`（范式重构）+ `Add`（flux schema 骨架）
- Prereqs: P1-P3 实施证据（各页 flux 输出/测试）

- [x] 核心范式（page-structure/non-standard-views/child-table-editor）重构：每模式 = 业务语义 + flux schema 骨架 + 数据契约（{items,total}） + 事件持久化；AMIS 实现标注为历史注记（无行号短摘要）
      - Skill: none
      - 证据：三核心文档均新增「§0 Flux 渲染权威模型」节（render-mode 切换 + complex/tabs/wizard/group 容器 + flux schema 骨架 + 数据契约 + 事件持久化 + complex 槽位组合模式）；page-structure §5 wizard/§8.3 三单匹配/§8.7-§8.12 七 F16 范式各加 flux 最终形态注记（引用 P3 落地证据 + AMIS 降级为历史注记）；non-standard-views §0 控件映射总表 + §2 kanban flux 注记；child-table-editor §0 input-table 数据契约（submitScope/行公式/$Arr/$Math）。
- [x] 辅助范式（tree/picker/visible-on/field-formatting/status-color-map）更新为 flux 控件映射（tree-select/status labelMap/mapping 等）
      - Skill: none
      - 证据：5 辅助文档 + cross-doc-navigation 均加 flux 控件映射注记（tree→flux tree/tree-select、picker→表单字段级 picker+页面级缺口 successor、visibleOn→flux visible/when、field-formatting→flux-control.xlib domain 映射、status-color-map→flux status/mapping、cross-doc→flux drawer/dialog）；统一声明 view.xml 模型层语义仍权威、AMIS 专属输出降级历史注记。
- [x] 沉淀「看板三段式 page.yaml」范式（data-source + card + chart + crud，复用度 11 页）——落入 `page-structure-patterns.md` §3（仪表板）或显式声明目标文件；核对 `dashboards.md` 同步
      - Skill: none
      - 证据：page-structure-patterns.md §3.0 新增「三段式看板 page.yaml 范式」（flux schema 骨架 + 四要点 + 后端配套，复用度 11 页）；dashboards.md「实现约定」§1/§6 同步为 flux 三段式 + data-source 取数范式。

Exit Criteria:

- [x] 范式文档以 flux 为权威，AMIS 历史注记明确不误导
- [x] 看板三段式范式已沉淀（owner doc 级，目标文件明确）

### Phase 2 - 域 ui-patterns 与缺口补齐

Status: completed
Targets: 18 域 `docs/design/<domain>/ui-patterns.md`、`docs/design/notify/`、`docs/design/quality/spc.md`
Skill: none

- Item Types: `Fix`（ui-patterns 更新）+ `Add`（缺口文档）
- Prereqs: Phase 1

- [x] 18 域 ui-patterns 中复杂页条目更新为 flux 控件映射（引用 `flux-complex-pages.md`）；**b2b ASN 五阶段在 Phase 0 已回填，此处核对一致性**
      - Skill: none
      - 证据：18 域 ui-patterns 全部加「Flux 控件映射」注记——hr/aps/b2b（Phase 0 漂移回填）+ crm/cs/mfg/log/fin/mnt/ct/drp/prj/pur/qa/sal/ast/inv/md（本阶段）；每域映射其复杂页至 flux 控件并引用 `flux-complex-pages.md` §3 + 对应范式 doc；b2b ASN 4 值全文一致性核对通过（grep 0 残留）。
- [x] notify 缺口闭合（二选一）：(a) 新增 `notify/ui-patterns.md`（模板管理页设计；偏好设置按 `notify/README.md` 标注 out-of-baseline 需 ORM 变更）or (b) 裁决「模板管理走标准 CRUD 范式无需独立文档」——记录理由
      - Skill: none
      - 证据：采用方案 (b)——notify/README.md 新增「页面设计与缺口闭合」节，记录理由（前端复杂面在收件箱已由 inbox-patterns.md 覆盖；模板管理为标准 CRUD 无特殊交互；偏好设置 out-of-baseline 需 ORM）+ flux 控件映射（收件箱→flux tabs+crud+data-source，模板→标准 crud）。
- [x] SPC 页面交互设计补齐（基于 flux chart referenceLines/band/markers 完整能力；输入来源 `flux-complex-pages.md` §7 #1 与 P4 实施结果，属回填性质）
      - Skill: none
      - 证据：quality/spc.md 新增「页面交互设计（Flux 实现权威）」节——控制图页（chart line + referenceLines UCL/LCL/CL + band 阴影 + markers 失控点 + select + crud 样本明细 + 数据契约 getSpcControlChartData）、能力页（crud + bar 能力等级分布 + status 着色）、样本页（crud + line 均值趋势）。
- [x] 一致性核对：引用核对扫描（Phase 0 模式复用，含 P4 占位页状态裁决）
      - Skill: none
      - 证据：b2b ASN 4 值全文 grep 0 残留（仅回填注记含历史词）；P4 占位页状态裁决（12 落地 + 4 Deferred 带触发条件）已记录于 P4 计划与 page.yaml alert；flux 映射引用与 flux-complex-pages.md §3 一致。

Exit Criteria:

- [x] 18 域 ui-patterns 复杂页条目 flux 化完成（含引用核对扫描通过）
- [x] notify 缺口以 (a) 或 (b) 明确闭合，SPC 交互设计已补齐（回填性质，输入来源注明）

### Phase 3 - 路线图与 i18n 决策

Status: completed
Targets: `docs/backlog/frontend-ui-roadmap.md`、`docs/design/flux-complex-pages.md`
Skill: none

- Item Types: `Fix`（路线图决策更新）+ `Follow-up`
- Prereqs: Phase 0-2 + P1 的默认渲染模式 Decision

- [x] roadmap「Flux 渲染引擎备选」→「Flux 全量迁移」决策更新（引用用户 2026-08-03 决策 + P1-P4 结果 + AMIS 退役路径，含 P1 Phase 3 的 AMIS 退役 Follow-up 触发条件）
      - Skill: none
      - 证据：frontend-ui-roadmap.md §「Flux 全量迁移」——节标题改名 + 首段旧决策（标准 CRUD 继续 view.xml→amis）划删除线标注废弃 + 新决策块（flux 唯一权威 + 全 19 域 FLUX + E2E 缺省 flux）+ AMIS 退役路径（P1 Phase 3 Follow-up 触发条件 = P1-P5 完成 AND flux E2E 连续 5 天全绿 AND 豁免清单清零）。
- [x] `flux-complex-pages.md` 更新为实施后一致：§2 渲染机制更新为 P1 实测结论（impl_flux_mode.xpl 机制、菜单 component 翻转）；**§1.2「决策依据（roadmap 已定）」中引用的旧决策（标准 CRUD 继续 view.xml→amis、97.6% 零修改）与新决策矛盾，一并更新**
      - Skill: none
      - 证据：flux-complex-pages.md 状态改「已实施」；§1.1 降级表加「flux 最终形态」列 + 标注全部消除；§1.2 旧决策（97.6% 无迁移收益）划删除线 + 新决策块；§2.1 加 P1 实测（impl_flux_mode.xpl 5 标签动态替换 + 菜单 component 翻转 + ErpAllFluxPagesTest 0 错误）；§4.2 甘特持久化改 updateSchedule（否决 dragUpdateOperation spec）；§7 #1 SPC 标已解决（referenceLines/band/markers）；§7 #9 complex 槽位标已解决；§7 #10 新增 flux 表达式运行时约束。
- [x] Follow-up: page.yaml 层 i18n 方案（flux `12-i18n.md` initFluxI18n/t()）登记为复杂页迁移横切要求，触发条件 = P2/P3 页面重写时
      - Skill: none
      - 证据：flux-complex-pages.md §7 #7 i18n 项更新为 Follow-up 登记——触发条件 = P2/P3 页面重写（1232-2/3）时随附处理复杂页硬编码中文文案；view.xml 层 i18n 已完成（351 文件），page.yaml 层全量实施随 P2/P3 落地。

Exit Criteria:

- [x] roadmap 决策更新完成（含退役路径）
- [x] flux-complex-pages.md 与实施证据一致
- [x] i18n 方案登记完成

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_03a096aaefferF2yyn0Bj8dLwv) — b2b ASN 第 4 处漂移漏报(B1)、notify 缺口表述(B2)、Skill 与方法不匹配(M2)、前置条件声明(M1)；已全部修订
- Independent draft review iteration 2: needs revision (ses_039fd27a1ffejAEBpdQidn5xUm) — Phase 0 回填项 Skill 残留（audit skill 仅给扫描项）、Task Route Skill Selection Basis 未说明取舍、b2b 锚点补 :160/:298；已全部修订
- Independent draft review iteration 3: needs revision (ses_039f01d48ffeYukPBZ8OcAEkoT) — b2b 锚点漏 :259（全文穷尽证伪）、Baseline 锚点不对称、b2b 回填与 P2/P3 错误绑定、Goals 与 Phase 3 对齐 P1 Follow-up；已全部修订
- Independent draft review iteration 4: accept (mission-driver review 2026-08-03-204249) — 格式合规、退出标准可测、范围内聚（单一结果表面）、结束证据已定义、doc-only 计划正确以文档一致性扫描替代代码验证门控；无 Blocker/Major。Minor（非阻塞，留待结束审计）：Phase 0 阶段级 Skill（document-audit-prompt）与回填项 Skill: none 不对称（Task Route 已说明意图）；Phase 0 Prereqs 行对 b2b 项过约束（已被「或与实施同步」+ 条目显式注软化）

## Closure Gates

- [x] 范围内行为完成（漂移回填 + 范式 flux 化 + ui-patterns 更新 + roadmap 决策）
      - 4 漂移回填（Phase 0）+ 9 范式 doc flux 化（Phase 1）+ 18 域 ui-patterns + notify/SPC 缺口补齐（Phase 2）+ roadmap 全量迁移决策 + flux-complex-pages 实施后一致 + i18n Follow-up 登记（Phase 3）
- [x] 相关文档对齐（与 P1-P4 实施结果一致）
- [x] 已运行验证（文档一致性扫描 + 引用路径核对；无代码变更故验证门控为文档审查）
      - Phase 0 行号漂移扫描修复 3 处（crm/cs/prj BizModel 行号）；Phase 2 b2b ASN 4 值全文 grep 0 残留；本计划为 doc-only，无代码变更（Closure Gates 明确「无代码变更故验证门控为文档审查」）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
      - 独立结束审计由 mission-driver 在新会话中生成的独立 closure auditor 子代理执行（不重用执行者上下文）
- [x] 结束证据存在于文件中

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

Status Note: 全 4 阶段完成（Phase 0-3）。本计划为 doc-only 文档一致性计划（无代码变更，验证门控为文档审查），以 P1-P4（`2026-08-03-1232-1/2/3/4`）实施结果为权威证据，将前端范式与域 ui-patterns 文档从「AMIS 权威」翻转至「flux 权威」并回填 4 处漂移。具体：4 处文档漂移回填为最终形态（non-standard-views §3.1/§4.1 timeline/calendar flux 原生、hr org-chart flux tree、aps gantt 拖拽已实现+右键菜单残留、b2b ASN 5→4 值字典全文穷尽）+ 行号漂移修复 3 处（Phase 0）；9 范式 doc 加「Flux 渲染权威模型」节 + AMIS 降级为历史注记 + 看板三段式范式沉淀（Phase 1）；18 域 ui-patterns 加 flux 控件映射 + notify 缺口闭合（方案 b 标准 CRUD）+ SPC 页面交互设计补齐（referenceLines/band/markers）（Phase 2）；roadmap「Flux 备选」→「Flux 全量迁移」决策 + AMIS 退役路径 + flux-complex-pages.md 实施后一致 + page.yaml i18n Follow-up 登记（Phase 3）。范围内无项目降级为 deferred/follow-up（已确认的 AMIS 范式物理移除与 page.yaml i18n 全量实施为既定 Deferred，触发条件已命名）。无 `> Work Item:`/`> Source Audits:` 行（本计划源自深度分析 §6.3-6.4，非 roadmap 工作项/审计源），故无 roadmap ❌→✅ 翻转与审计关闭步骤。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理（由 mission-driver 在新会话中生成，不重用执行者上下文）。审计轮次：2026-08-05，对计划文件、4 漂移目标文件、9 范式 doc、18 域 ui-patterns、notify/README、quality/spc.md、frontend-ui-roadmap.md、flux-complex-pages.md 执行冷重播核对（非执行者自填）。
- Evidence:
  - Phase 0：4 漂移文件均含「文档回填注记」（non-standard-views §3.1/§4.1、hr §3.3/§402-404、aps 关键交互模式、b2b 锚点 :10/23/158/160/184-185/259/290/298 全量）；行号修复经实时 grep 核实（crm `:84→:112`、cs `:109-242→:103-174`、prj `:103-163→:108-157`）。
  - Phase 1：3 核心 doc（page-structure/non-standard-views/child-table-editor）新增 §0 Flux 渲染权威模型；6 辅助 doc（tree/picker/visible-on/field-formatting/status-color-map/cross-doc）加 flux 控件映射；page-structure §3.0 看板三段式 + §5/§8.3/§8.7-8.12 flux 注记；dashboards.md §实现约定 同步。
  - Phase 2：18 域 ui-patterns 全部含 flux 控件映射注记；notify/README 新增「页面设计与缺口闭合」节（方案 b）；quality/spc.md 新增「页面交互设计（Flux 实现权威）」节。
  - Phase 3：frontend-ui-roadmap.md §「Flux 全量迁移」+ AMIS 退役路径；flux-complex-pages.md 状态「已实施」+ §1.1/§1.2/§2.1/§4.2/§7 #1/#7/#9/#10 更新。
  - 五点一致性：Plan Status=completed / 4 Phase Status=completed / 全 Phase item + Exit Criteria `[x]` / Closure Gates 全 `[x]` / Closure evidence 实证。
  - Anti-Hollow：每 doc 改动含具体 flux schema 骨架/控件映射/裁决引用，非空 stub；Deferred 诚实性——AMIS 物理移除 + page.yaml i18n 全量实施均归 Deferred But Adjudicated 节，触发条件已命名。

Follow-up:

- page.yaml 层 i18n 全量实施（F15 延后项）：触发条件 = P2/P3 页面重写（1232-2/3）时随附（已登记 flux-complex-pages.md §7 #7）。
- AMIS 范式文档物理移除：触发条件 = 全部 P1-P5 完成 + flux E2E 连续 5 天全绿 + 豁免清单清零（见 Deferred But Adjudicated 节）。
