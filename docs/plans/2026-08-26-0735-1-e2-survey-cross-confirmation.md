# 2026-08-26-0735-1-e2-survey-cross-confirmation E2 对照确认与边界声明（记账内核审计性 / 低代码平台边界 / 扩展机制三方对照，零代码）

> Plan Status: completed
> Mission: erp-enhancement
> Work Item: E2.1 + E2.2 + E2.3
> Last Reviewed: 2026-08-26
> Source: `docs/backlog/erp-enhancement-roadmap.md` §5 Milestone E2（对照确认与边界声明，零代码）
> Related: E1 设计文档 7 份（对照对象，已完成）；`docs/plans/2026-08-26-0735-2-e3-integrated-implementation.md`（同批计划 2，E3 整体实现，本计划先行）
> Audit: required

## Current Baseline

- **E1 已全部 done**（7 份设计文档，roadmap §5）；E3 全部 todo。E2 三项 todo（roadmap §2 计数：todo 12 / done 7）。
- **E2.1 落点现状**：`docs/design/finance/posting.md`（565 行）含三层过账模型 / businessType 映射 / 过账引擎 Provider 机制 / 冲销机制 / 反写契约 / 多币种等章节，**无任何 Beancount/对照相关段落**（grep 零命中）。对照输入 = `docs/analysis/erp-survey/2026-08-12-0000-beancount.md`（§2 核心架构源码实测 + §3 借鉴）。既有可核对凭证引擎证据链：三层过账 + 红字冲销 + 平衡校验 + `posting-log.md` 会计日志 + 冲销反写闭环（M5 全 done）。
- **E2.2 落点现状**：`docs/analysis/erp-survey/` 无 Frappe/Baserow vs Nop 模型驱动路径差异的对照结论注记（08-12 批次 14 份报告独立存在；`survey-index.md` 已含 08-12 批次 14 报告索引行——2026-08-12 增补，但索引行不含路径差异对照结论）。原始素材已备：`2026-08-12-0000-frappe-framework.md` §2.1 DocType 元数据驱动 / §2.3 与 Nop Platform 的路径差异、`2026-08-12-0000-baserow.md` §2（BaserowFormula / AI 助手 Kuma / 应用构建器）。roadmap §4.1 已对 08-12 批次借鉴点做预归类（多为「对照确认不立项」），E2.2 交付 = 把归类结论落成可引用的注记 + 工具链对照（frappe bench vs 本项目 build.sh/nop-cli 路径）。
- **E2.3 落点现状**：`docs/analysis/plugin-hot-management-research.md`（444 行）含 §5 三路径对比矩阵（OSGi 动态 Bundle / Maven Module Isolation / NocoBase 应用层插件管理器）+ §6 推荐路径裁决（Decision R1 原文：路径 2 默认 + 路径 3 补充 + 路径 1 不采用 + **当前阶段不实施任何路径、维持 app-erp-all 全量聚合**；Delta 定制 + SPI 注入为既有扩展范式，roadmap §4.1 行缩写「Delta+SPI」即指此）+ §8 平台约束佐证 + §10 Follow-up，**缺 InvenTree plugin registry / Fleetbase extensions 的三方对照段**。对照输入：`2026-08-12-0000-inventree.md`、`2026-08-12-0000-fleetbase.md`、既有 `2026-07-20-0000-nocobase-compare.md`。
- **约束**：E2 为零代码里程碑（roadmap §5 标题明示「零代码」）——不改任何生产代码 / ORM / 页面 / 测试 / config。

## Goals

- `posting.md` 新增「记账内核审计性对照」章节：Beancount 审计性/平衡校验约束清单逐项核对既有凭证引擎，每项标记结论（已覆盖+证据 / 部分覆盖+差距说明 / 不适用+理由）。
- `docs/analysis/erp-survey/` 落低代码平台边界对照注记：Frappe/Baserow 与 Nop 模型驱动的路径差异结论（引用既有报告素材）+ 工具链对照（frappe bench vs build.sh/nop-cli）。
- `plugin-hot-management-research.md` 新增三方对照段：InvenTree plugin registry / Fleetbase extensions / NocoBase 应用层插件管理器 vs 既有 Delta+SPI 裁决，结论登记（佐证或分歧）。
- roadmap §2/§5/§6 收口：E2.1/E2.2/E2.3 → done（todo 12→9）。

## Non-Goals

- 不改任何生产代码 / ORM 模型 / 页面 / 测试 / config（零代码边界）。
- 不重写 E1 设计文档内容（对照确认引用既有结论，不复制设计）。
- 不实施对照中发现的行为差距（差距只登记去向：owner doc 注记或 roadmap follow-up 候选，实施归后续计划/工作项）。
- 不覆盖 roadmap §4.1 已显式归类「触发条件驱动/不立项」的借鉴点（注记仅引用其结论，不重新裁决）。
- 不动 E3 工作项（归同批计划 2 `2026-08-26-0735-2`）。

## Task Route

- Type: `verification or audit work`（零代码对照确认，文档产物）
- Owner Docs: `docs/design/finance/posting.md`（E2.1 落点）、`docs/analysis/erp-survey/`（E2.2 落点）、`docs/analysis/plugin-hot-management-research.md`（E2.3 落点）、`docs/backlog/erp-enhancement-roadmap.md`（收口）
- Skill Selection Basis: Skill `none`——纯文档对照与结论登记，无代码/模型/页面/测试产出；可用技能（nop-backend-dev / nop-frontend-dev / nop-testing / nop-debugging）均不匹配文档对照工作方法。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯文档工作，无运行时依赖）。

## Execution Plan

### Phase 1 — E2.1 记账内核审计性对照（posting.md 补对照段）

Status: completed
Targets: `docs/design/finance/posting.md`
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: 无（与 Phase 2/3 可并行）

- [x] Proof: 从 `2026-08-12-0000-beancount.md` §2/§3 提取审计性/平衡校验约束清单（**以报告实际内容为清单权威源，不得虚构核对行**；报告现有素材含借贷平衡 invariant、GL 不可变冲销语义、期初/期末一致性、账户树、查询引擎），逐项对照既有凭证引擎**实时仓库证据**（三层过账 / 红字冲销同向取负 / 平衡校验 / `ErpFinPostingExceptionRecorder` 失败处理 / 会计日志与冲销反写闭环），证据锚点引用具体类/文件/既有 E2E 断言。
- [x] Decision: 对照结论登记——逐项「已覆盖（证据）/ 部分覆盖（差距说明）/ 不适用（理由）」；确认为行为差距的项显式登记去向（owner doc 注记就地 + roadmap follow-up 候选），不在本计划实施。

Exit Criteria:

- [x] `posting.md` 含「记账内核审计性对照」章节：清单逐项核对 + 结论 + 代码证据锚点，与既有「稳定约束 vs 可配置策略」「冲销机制」等章节无事实矛盾（矛盾处以实时仓库为准修正旧表述并登记）。

### Phase 2 — E2.2 低代码平台边界对照 + 工具链对照（erp-survey 注记）

Status: completed
Targets: `docs/analysis/erp-survey/`（落点 Phase 内裁决）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: 无（与 Phase 1/3 可并行）

- [x] Decision: 注记落点裁决——`survey-index.md` 增段 vs 新建 08-12 批次对照注记文件（索引行已含 08-12 批次，无需补索引）。
- [x] Add: 路径差异结论注记——Frappe（DocType 元数据驱动 + 框架级内置领域目录）与 Baserow（自研公式语言 / AI 助手 / 应用构建器）vs Nop 模型驱动（orm.xml → codegen 增量链），引用 `frappe-framework.md` §2.1/§2.3、`baserow.md` §2 既有素材，结论对齐 roadmap §4.1 预归类（对象以代码定义 / 公式语言 / AI 建表助手等均「对照确认不立项或触发条件驱动」）。
- [x] Add: 工具链对照注记——frappe bench（app/site 管理工具链）vs 本项目 `build.sh` / `nop-cli gen`（首代生成）+ `mvn clean install` 增量重生成（`docs/context/project-context.md` 验证命令为权威），说明职责差异与本项目采用路径。

Exit Criteria:

- [x] erp-survey 存在对照注记（落点经裁决）：路径差异结论 + 工具链对照，全部引用既有报告与仓库事实；如产生新候选工作项，显式登记「为何暂不立项/触发条件」而非留开放结尾。

### Phase 3 — E2.3 扩展机制三方对照（plugin-hot-management-research.md 补对照段）

Status: completed
Targets: `docs/analysis/plugin-hot-management-research.md`
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: 无（与 Phase 1/2 可并行）

- [x] Proof: 提取 InvenTree plugin registry 与 Fleetbase extensions 机制要点（注册表/生命周期/钩子形态，以两报告 §2/§3 实测内容为准），与既有 §4 NocoBase 应用层插件管理器路径、§5 对比矩阵、§6 Decision R1（原文裁决：路径 2 默认 + 路径 3 补充 + 路径 1 不采用 + 当前阶段维持 app-erp-all 全量聚合，Delta+SPI 为既有扩展范式）三方对照。
- [x] Decision: 对照结论登记——两开源形态是否动摇 §6 裁决（预期：佐证——均为应用级注册/钩子机制，与 Delta+SPI 扩展范式非互斥；签名分发等能力对齐 roadmap §4.1「模块注册表/ed25519 签名：触发条件=插件分发需求」行）；如有实质分歧，按 §10 Follow-up 机制登记，不直接改 §6 裁决。

Exit Criteria:

- [x] `plugin-hot-management-research.md` 含三方对照段，结论与 §5/§6/§10 及 roadmap §4.1 对应行一致（或显式登记分歧与后续动作）。

### Phase 4 — roadmap 收口 + 日志

Status: completed
Targets: `docs/backlog/erp-enhancement-roadmap.md`、`docs/logs/2026/08-26.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 + Phase 2 + Phase 3

- [x] Add: roadmap §2 状态计数（todo 12→9，done 7→10）+ §5 E2.1/E2.2/E2.3 → done + §6 交付物列补 ✅ 标记；`docs/logs/` 按日志规范落条目。

Exit Criteria:

- [x] roadmap E2 三项 done 且状态计数一致；日志条目存在。

## Draft Review Record

- Independent draft review iteration 1: **acceptable as-is (accept)**（task `ses_fc4bb6994ffeLXM17vi8yDuQDY`，fresh session）——逐维度通过：基线抽查（行数/grep/章节/计数算术）精确；范围与 roadmap §5/§6/§4.1 双向对齐（含工具链对照无遗漏）；模板/Item Types/反松弛词零命中/零代码门控例外合规。3 项 MINOR（① survey-index.md 实已含 08-12 批次索引行的表述失实；② 「D4 裁决 Delta+SPI」为转述缩写，应引 §6.1 原文；③ Phase 1 示例约束清单以报告为权威源防虚构）——审查者裁定均不阻塞、属激活前建议修正项；已按建议逐项修正基线与 Phase 1/2/3 措辞（本文件当前版本即修正后版本），无新增范围变更。共识达成 → 转 active。

## Closure Gates

> 本计划零代码（仅文档变更），删除 typecheck/build/test 验证命令门控——理由：无任何代码/模型/契约变更，机器验证不适用；以文档事实核查门控替代（下条）。

- [x] 范围内行为完成（三份对照产物落盘 + roadmap 收口 + 日志）
- [x] 相关文档对齐（三处落点文档交叉引用一致；与 roadmap §4.1 预归类无未登记矛盾）
- [x] 文档事实核查：对照段引用的代码锚点（类/文件/行为）经实时仓库 grep/读文件复核存在且语义相符
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——E2 为零代码确认批次；对照中发现的行为差距按 Phase 1 Decision 项登记去向，不构成本计划延迟项）

## Closure

Status Note: 4 个 Phase 全部完成（2026-08-26）。交付物：① `posting.md §记账内核审计性对照（Beancount，E2.1）`（9 项清单：7 已覆盖 / 1 不适用 / 1 部分覆盖——零金额非零校验差距已登记 roadmap §4.1 触发条件行）；② `docs/analysis/erp-survey/2026-08-26-0000-lowcode-boundary-and-toolchain-notes.md`（E2.2，路径差异 + 工具链对照，落点裁决见其 §0）；③ `plugin-hot-management-research.md §11`（E2.3，三方对照结论：佐证无分歧）。roadmap §2 计数 todo 12→9 / done 7→10，§5 三项 done，§6 ✅；日志 `docs/logs/2026/08-26.md`。零代码边界保持：无生产代码 / ORM / 页面 / 测试 / config 变更，按本计划 Closure Gates 以文档事实核查门控替代 build/test 门控（对照段引用代码锚点经实时仓库 grep/读文件复核）。Closure Gates 勾选与结束审计证据归独立结束审计（CLOSURE_VERIFY）步骤填写。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（CLOSURE_VERIFY，新会话、非执行者；mission `2026-08-26-072347-mission-driver`，2026-08-26）
- Evidence: 实时仓库逐项复核通过——① 三份交付物落盘：`docs/design/finance/posting.md:567`「记账内核审计性对照（Beancount，E2.1）」9 项清单（7 已覆盖 / 1 不适用 / 1 部分覆盖+触发条件登记）、`docs/analysis/erp-survey/2026-08-26-0000-lowcode-boundary-and-toolchain-notes.md`（§0 落点裁决 + §1 路径差异 + §2 工具链对照 + §3 无新候选）、`docs/analysis/plugin-hot-management-research.md:433` §11（三方对照，结论佐证无分歧）；② roadmap 收口：§2 计数 `todo 9 / done 10`、§5 L107-109 三项 done、§6 L136-138 ✅、§4.1 L73（金额非零差距触发条件行）/ L87（工具链行已产出）双向引用一致；③ 日志 `docs/logs/2026/08-26.md` L5-10 存在且与交付物一致；④ 代码锚点抽查全数命中且语义相符：`ErpFinPostingProcessor.java` balanceTotals:758 / assertBalanced:772（抛 ERR_UNBALANCED）/ resolveSubjects:615 / translateFactsForSchema:701 / buildReversalDraft:780 / dispatchReversalEvent:379 / recordListenerFailures:419 / O-8 markOriginalVoucherReversed 注释与实现 :963-970；`ErpFinPostingErrors.java` ERR_SUBJECT_NOT_FOUND:32 / ERR_PERIOD_CLOSED:41 / ERR_UNBALANCED:44；`ErpFinPostingExceptionRecorder.java:43`；`IErpFinFactsValidator.java:11`；`ErpFinVoucherBillR.java`（erp-fin-dao）；`ErpFinVoucherDocumentStateMachine.java:19-30`；`ErpFinAccountingPeriodProcessor:380/:392`（closing 聚合 + isReversed=false 过滤）；`AnnualCloseService:118/:182`；测试锚点 `TestErpFinPostingService:200-205`（红字+原凭证 isReversed=true 断言）/ `TestErpFinBadDebtReversal:45-47` / `TestErpFinBadDebtProvisionReversal:47-50`（Dr 6701=-X / Cr 1231=-X 同向取负）；SPI/Registry（IErpFinAcctDocProvider / IErpFinVoucherReversedListener / ErpFinReversalListenerRegistry）与属性测试（PropertyErpFinDebitCreditBalance / PropertyErpFinMultiCurrencyBalance）均存在；⑤ 零代码边界保持：全部变更为文档，无生产代码 / ORM / 页面 / 测试 / config 变更；⑥ `plan-check.mjs --strict` 修正后复跑通过（0 unchecked + Closure 证据非占位）。

Follow-up:

- 无非阻塞跟进项——对照中确认的唯一行为差距（零金额非零校验）已按 Phase 1 Decision 项登记去向（roadmap §4.1 触发条件行，触发时经 `IErpFinFactsValidator` 扩展点补校验），签名分发等能力对齐 roadmap §4.1 既有触发条件行，均非本计划 follow-up。
