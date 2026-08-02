# 2026-07-28-1510-2-audit-remediation-ma3-design-doc-baseline MA3 设计文档作为行为基线审计（A3.1）

> Plan Status: completed
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA3（工作项 A3.1）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「设计文档基线」行（MA3）；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/design-doc-audit-prompt.md`（审计方法——已有文档质量 12 维度 + 功能覆盖度外部基准）；`docs/design/README.md`（设计文档索引与编写规则 owner doc）；`docs/analysis/erp-survey/` + `docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`（维度 1 外部对照基准）；`docs/plans/2026-07-28-1510-3-audit-remediation-ma3-design-completeness-scan.md`（A3.2 前瞻性缺口扫描——本审计审"存在的内容"，A3.2 找"缺失的内容"，互补不重叠）
> Audit: required

## Current Baseline

设计文档作为行为基线审计（文档-实现一致性层 MA3 首项）。`docs/design/` 是 nop-app-erp 的**稳定应用层行为 owner doc**——它定义"产品行为是什么"，是实施、页面定制、测试与契约变更的行为基线。若设计文档描述临时/演示行为、与需求冲突、跨越 owner-doc 边界（重复 schema/字典/实现细节）、或声称的功能覆盖无实质支撑，将导致**实施者按错误基线实现**、**设计与代码漂移积累**、**owner-doc 边界碎裂（重复维护点）**。

owner doc `docs/design/README.md` 已规定设计文档结构与编写规则：

- **范围边界**：`docs/requirements/`="应该做什么"；`docs/design/`="稳定应用层基线"；`docs/architecture/`="技术设计"；`model/*.orm.xml`="持久化真相"。设计文档不重复表目录/逐字段 schema/字典清单/平台实现细节。
- **全局文档**：app-overview / domain-design-guidelines / domain-glossary / flow-overview / roles-and-permissions / dashboards / feature-inventory。
- **业务域文档**：18 域各 `docs/design/<domain>/` 目录，结构因域而异（状态机重的域才有 state-machine.md；跨域协作复杂的域才有 cross-domain.md；主数据启停二态非状态机）。第二批扩展 8 域 + portal(future extension)。
- **审查方法引用**：`design-doc-audit-prompt.md`（已有文档质量）/ `design-completeness-scan-prompt.md`（缺失扫描）。

实时仓库 `docs/design/` 已有规模（待审查）：

- **全局文档**：app-overview.md / domain-design-guidelines.md / domain-glossary.md / flow-overview.md / roles-and-permissions.md / dashboards.md / feature-inventory.md。
- **18 业务域目录**：master-data / inventory / purchase / sales / finance / assets / projects / manufacturing / quality / maintenance / crm / customer-service / human-resource / aps / contract / drp / logistics / b2b；+ portal(future) + notify + l10n。
- **跨域模式文档**：batch-operation-patterns / child-table-editor-patterns / cross-doc-navigation-patterns / date-ranged-validity-pattern / field-formatting-patterns / non-standard-views-patterns / page-structure-patterns / picker-patterns / processor-delegation-auto-gen / query-filter-patterns / status-color-map / tree-entity-patterns / use-case-authoring-guide / visible-on-patterns / voucher-back-link-patterns / erp-design-audit-checklist / i18n-glossary。
- **MA2 业务正确性审计已大量引用**各域 owner doc 作为行为基线（A2.1–A2.17 的 owner-doc 对齐裁决），并登记多处 owner-doc drift（如 P1-MA2-067 closeProject owner doc 用词「或确认剩余不再执行」软门控 / P1-MA2-073 b2b state-machine.md 自动化承诺 vs README/MFT transport Deferred 文档不一致 / P2-MA2-065/067/068/069/070/071 多域 state-machine.md 缺多状态承载实体独立章节）。

**但从未做过一次将 `docs/design/` 整体作为行为基线的系统性质量审查**。已知未核验控制点（design-doc-audit-prompt 12 维度 + 功能覆盖度外部基准）：

- **维度 1 功能覆盖度（核心，外部基准）**：以 `docs/analysis/erp-survey/`（16 开源 ERP + 7 补充项目实测）+ `feature-coverage-matrix.md` 为外部基准，核查基准系统标配核心能力在 `docs/design/` 是否都有对应 owner doc——不仅查"域存在"，更查**功能深度**（如 CRM lead scoring / ERPNext 销售预测 / Axelor 替代工艺路线）。缺失的核心标配为 blocker，重要能力缺失为 major。**反模式**：仅凭"需求没写"认为功能缺失不构成问题。
- **维度 2 产品基线**：文档描述正式产品行为而非临时原型/演示/模拟/部分质量/后续重写行为。
- **维度 3 稳定与时间敏感责任**：设计回答"产品行为是什么"而非"本周实现什么"——实现顺序/待办/计划状态/路线图排序不应在设计文档重复。
- **维度 4 需求对齐**：设计声明与实现就绪需求对齐；原始输入/原型/历史审计文本/聊天记忆不覆盖综合需求；矛盾分类为设计漂移/需求差距/有意基线变更/需人工决策。
- **维度 5 Owner-doc 边界**：设计解释业务语义，不重复表目录/逐字段 schema/字典/契约/生成代码/平台实现细节（应路由到 orm.xml/architecture）。
- **维度 6 跨设计一致性**：app-overview / feature-inventory / 域文档 / roles-and-permissions 在内容、谁可用、哪个 owner 控制细节上一致；同概念不多文件冲突命名/生命周期/所有权。
- **维度 7 域语言与有界上下文**：每主要业务概念有自然所属域文档；面向业务语言不被表名/枚举码/类名取代。
- **维度 8 工作流与状态清晰度**：核心流程有业务级序列细节；状态转换标识触发者/业务前提/结果；终端/异常/退款取消重试/超时/回退/集成失败路径在影响产品行为时业务级指定。
- **维度 9 角色权限与受保护操作**：角色文档与功能/域文档一致；敏感行为（支付/退款/数据删除/账户管理/权限管理）有明确 owner-doc 基线或升级。
- **维度 10 页面与交互行为**：重要页面/交互结果在业务级涵盖（所需验证/资格/空错误状态/用户反馈）。
- **维度 11 配置与操作语义**：面向业务的配置与技术调度/存储/部署/集成机制分离。
- **维度 12 维护成本与重复**：设计事实有一个自然 owner doc；重复矩阵/功能状态列表/复制规则块视为风险。

剩余差距：需要一次系统性设计文档行为基线审查，发现任何 blocker/major（**核心标配功能从未设计（无 owner doc 也无"产品基线外"声明）** / **设计描述临时/演示行为** / **owner-doc 边界碎裂（设计重复 schema/字典/实现细节）** / **跨文档冲突的命名/生命周期/所有权** / **敏感行为（支付/退款/数据删除）无 owner-doc 基线**）登记为 P1（文档类 P1 目标 MR2，依赖 MA3+MA4 done）走 MR2 批量修复。设计文档审查为文档层，原则上不产生 P0（P0 为代码/契约/数据破坏级缺陷）；若发现设计描述与已落地代码严重背离致错误实现风险，升级标注并交接 A3.3-A3.5 owner doc vs 代码 drift 审计。

## Goals

- 按 `design-doc-audit-prompt.md` 将 `docs/design/` 整体作为正式产品行为基线做系统性审查，产出审计报告。审查维度：维度 1 功能覆盖度（外部基准 erp-survey/feature-coverage-matrix）+ 维度 2-12 已有文档质量（产品基线/稳定时间敏感/需求对齐/owner-doc 边界/跨设计一致性/域语言/工作流状态/角色权限/页面交互/配置操作语义/维护成本重复）。
- **功能覆盖度与文档质量分别给结论**（维度 1 与维度 2-12 性质不同：前者查"该写的功能是否都写了"，后者查"已写文档写得好不好"——两者都不可省略）。
- 重点核验已识别控制点：(1) 功能覆盖度外部基准对照（功能深度非仅域存在）；(2) 产品基线（无临时/演示/模拟行为混入正式文档）；(3) owner-doc 边界（设计不重复 schema/字典/实现细节）；(4) 跨文档一致性（app-overview/feature-inventory/域文档/roles 一致）；(5) 敏感行为（支付/退款/数据删除）owner-doc 基线；(6) MA2 已登记 owner-doc drift 复核（P1-MA2-067/073 + P2-MA2-065/067-071 等文档不一致项）。
- scope matrix §2.3「设计文档基线」行终态标记（`❓` → `✅`/`⚠️(P1)`）。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总（目标 MR2）。roadmap A3.1 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做前瞻性缺失扫描（找"从未设计的整个功能"的广度扫描）— 归 A3.2（`design-completeness-scan-prompt.md`）。本审计（`design-doc-audit-prompt.md`）审"存在的内容"的质量，维度 1 功能覆盖度只对**已有文档声称的覆盖**做外部基准交叉验证 + 标注已知的明显缺口，不重复 A3.2 的全量域/文档/功能点扫描。两者互补不重叠（各自 skill 已明确分工）。
- **不**做 owner doc vs 代码 drift 的逐行核对 — 归 A3.3-A3.5（finance/mfg/pur-sal-inv owner doc vs 代码 drift）。本审计核验设计文档**内部**作为行为基线的质量（自洽/边界/覆盖声明），不做设计 vs Java/ORM 逐字段比对。若发现设计声明与已落地代码严重背离，标注并交接 A3.3-A3.5。
- **不**做 API 契约（api.xml）vs 实现一致性 — 归 A3.6。
- **不**做索引路由有效性 — 归 A3.7。
- **不**做可定制性验证（Delta/扩展字段实际可用性）— 归 A3.8。
- **不**做状态机正确性（迁移完整性/守卫/并发）— 归 MA2（A2.5-A2.15 已 done）。本审计维度 8 只核验状态文档的**业务级序列/触发者/前提/终端异常路径**清晰度，不重做状态机正确性裁决。
- **不**在本计划内批量修复 P1 — P1（文档类）经 R2.0 展开机制进入 MR2。本审计为文档层，原则上无 P0；若发现致错误实现的高风险设计-代码背离，升级标注交接 A3.3-A3.5，不在本审计改代码。
- **不**手改生成物或 ORM。文档修复在 MR2 批量进行。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/README.md`（设计文档索引与编写规则）；`docs/design/` 下全部文件（app-overview / domain-design-guidelines / domain-glossary / flow-overview / roles-and-permissions / dashboards / feature-inventory + 18 域目录 + 跨域模式文档）；`docs/requirements/product-scope.md`（维度 4 需求对齐基准）；`docs/architecture/` 相关文件（仅当设计引用实现策略/横切技术行为时）；`docs/analysis/erp-survey/` + `docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`（维度 1 外部对照基准）
- Skill Selection Basis: `design-doc-audit-prompt.md`（roadmap A3.1 指定此 skill，设计文档行为基线审查专用方法——12 维度 + 功能覆盖度外部基准 + 严重性指南 blocker/major/minor/note。项目定制化层见 `docs/skills/README.md`）。本审计审"已有文档质量"，A3.2 找"缺失内容"，互补。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。文档修复在 MR2 批量进行（本审计只识别 P1 不修复）。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为纯文档审查，不构建/不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。实际上文档审查无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - 设计文档行为基线系统性审查（12 维度 + 功能覆盖度）

Status: completed
Targets: `docs/design/` 全部文件（全局文档 + 18 域目录 + 跨域模式文档）；`docs/requirements/product-scope.md`；`docs/analysis/erp-survey/` + `feature-coverage-matrix.md`（外部基准）
Skill: `design-doc-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA2 A2.1–A2.17 done（owner-doc drift findings 已在 arm-index，本审计复核引用）。MA3 各项仅依赖 0.3（roadmap deps），A3.1 不依赖 A2.18 完成。本审计为 MA3 首项。

- [x] 维度「1 功能覆盖度（核心，外部基准）」：以 `docs/analysis/erp-survey/` + `feature-coverage-matrix.md` 为外部基准，核查已有文档**声称的覆盖**是否有实质设计支撑，并对照外部基准识别其未列入但应具备的能力。不仅查域存在，查功能深度（CRM lead scoring / 销售预测 / 替代工艺路线等开源标配）。缺失分级（核心标配 blocker / 重要 major / 长尾 note 建议声明"产品基线外"）。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「2 产品基线」：审查文档是否描述正式产品行为而非临时原型/演示/模拟/部分质量/后续重写；支付/退款/权限/数据删除/集成/账户管理行为是否定义为正式产品行为或显式路由需求澄清。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「3 稳定与时间敏感责任」：审查文档是否回答"产品行为是什么"而非混入实现顺序/待办/计划状态/当前阻塞/活动工作/路线图排序（这些应在 backlog/plans）。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「4 需求对齐」：审查设计声明与 `docs/requirements/product-scope.md` 对齐；原始输入/原型/历史审计文本不覆盖综合需求；矛盾分类（设计漂移/需求差距/有意基线变更/需人工决策）。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「5 Owner-doc 边界」：审查设计是否重复表目录/逐字段 schema/字典/契约/生成代码/平台实现细节（应路由 orm.xml/architecture）；实现关注点（事务/锁/缓存/调度/集成协议/模块连接）是否路由到 architecture。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「6 跨设计一致性」：审查 app-overview / feature-inventory / 域文档 / roles-and-permissions 在内容、谁可用、哪个 owner 控制细节上一致；同概念不多文件冲突命名/生命周期/所有权/资格规则。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「7 域语言与有界上下文」：审查每主要业务概念有自然所属域文档；面向业务语言不被表名/枚举码/类名/框架机制取代；跨域流程描述为业务工作流。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「8 工作流与状态清晰度」：审查核心流程业务级序列细节；状态转换标识触发者/业务前提/结果；终端/异常/退款取消重试/超时/回退/集成失败路径在影响产品行为时业务级指定。**仅核验文档清晰度，不重做 MA2 状态机正确性裁决**。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「9 角色权限与受保护操作」：审查角色文档与功能/域文档一致；敏感行为（支付/退款/数据删除/账户管理/权限管理）有明确 owner-doc 基线或升级。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「10 页面与交互行为」：审查重要页面/交互结果在业务级涵盖（所需验证/资格/空错误状态/用户可见反馈）；UI/原型细节非除非需求/设计已接受。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「11 配置与操作语义」：审查面向业务配置与技术调度/存储/部署/集成机制分离；操作默认值/回退/管理员可见控件仅在影响支持应用行为时定义。
      - Skill: `design-doc-audit-prompt.md`
- [x] 维度「12 维护成本与重复」：审查设计事实是否有单一 owner doc；重复矩阵/功能状态列表/复制规则块视为风险（除非故意为单一所有者）。
      - Skill: `design-doc-audit-prompt.md`
- [x] MA2 已登记 owner-doc drift 复核：复核 MA2 审计中已标注的 owner-doc 不一致项（P1-MA2-067 closeProject owner doc 用词软门控 / P1-MA2-073 b2b state-machine.md 自动化承诺 vs README/MFT Deferred 文档不一致 / P2-MA2-065/067/068/069/070/071 多域 state-machine.md 缺多状态承载实体独立章节 等），确认其在本审计维度下的分类与归属。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-1510-arm-ma3-design-doc-baseline.md`（含：12 维度逐项裁决 + 功能覆盖度结论（外部基准缺口清单分级）、需求/设计冲突分类摘要、owner 边界摘要、域边界摘要、维护成本摘要、blocker/major/minor/note finding 清单、MA2 owner-doc drift 复核表、裁决通过/失败、剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] 12 维度逐项裁决产出（每维度至少一句裁决，含"本维度无发现"）
- [x] 功能覆盖度结论产出（外部基准缺口清单 + 分级 blocker/major/note），与文档质量结论分别给出
- [x] blocker/major/minor/note finding 清单产出，每个含严重性/受影响文件/问题/重要性/建议处理方式
- [x] MA2 已登记 owner-doc drift 复核表产出

### Phase 2 - finding 汇总交接 MR2 + 索引/矩阵更新

Status: completed
Targets: 设计文档审计 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「设计文档基线」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] finding 汇总：全部 blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA3-NNN`、报告、描述、目标 MR2、修复状态 todo）。文档类 P1 目标 MR2（依赖 MA3+MA4 done，由 R2.0 展开机制转化为具体修复工作项行）。
      - Skill: none
- [x] 若发现致错误实现的高风险设计-代码背离，升级标注并交接 A3.3-A3.5（owner doc vs 代码 drift）+ A4.6-A4.8（view.xml drift），在报告中明确交接路径。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.3「设计文档基线」行终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 blocker/major 已登记 arm-index §P1 汇总（目标 MR2），待 R2.0 展开
- [x] 高风险设计-代码背离已标注交接路径（A3.3-A3.5 / A4.6-A4.8）
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05869e0edffe5Il2T6C4c8AIDx`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：roadmap A3.1 = `todo`，Owner Doc/Skill/Deps 匹配 ✓；7 全局设计文档 + 18 域 README + 跨域模式文档实仓存在 ✓；MA2 owner-doc drift findings（P1-MA2-067 closeProject / P1-MA2-073 b2b EDI / P2-MA2-065/067-071）arm-index 确认 ✓；外部基准（feature-coverage-matrix.md + erp-survey/ 40+ 文件）存在 ✓；design-doc-audit vs design-completeness-scan 互补分工经两 skill 文件原文确认（audit-prompt line 33 / scan-prompt line 9）✓；Non-Goals 明确排除 A3.2-A3.8 + MA2 状态机正确性 ✓；反松弛零禁词；结构匹配参考 A2.17。**采纳 3 项非阻塞修订**：(1) Phase 1 Prereqs 措辞去自相矛盾（MA3 各项仅依赖 0.3，A3.1 不依赖 A2.18 完成）；(2) Closure Gates note 改"不改应用代码"（本审计产出报告+索引更新）；(3) scope matrix §2.3 无逐域 `❓` 单元格——执行时加终态标注行而非翻转既有单元格（实现细节，记录在案）。Plan Status 转 active。

## Closure Gates

> 本计划主体是文档审查（不改应用代码；产出为审计报告 + arm-index/scope-matrix 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。文档修复在 MR2 批量进行，本审计只识别 finding。

- [x] 范围内行为完成（A3.1 设计文档行为基线审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [x] 已运行验证：文档审查无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A3.2 前瞻性缺失扫描（找"从未设计的整个功能"）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计（design-doc-audit）审"存在的内容"的质量；A3.2（design-completeness-scan）找"缺失的内容"。维度 1 功能覆盖度只对已有文档声称的覆盖做外部基准交叉验证 + 标注明显缺口，不重复 A3.2 的全量域/文档/功能点扫描。
- Successor Required: `yes`——A3.2 执行时复核（同批起草）。

### A3.3-A3.5 owner doc vs 代码 drift（逐行核对）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计核验设计文档**内部**作为行为基线的质量（自洽/边界/覆盖声明），不做设计 vs Java/ORM 逐字段比对。逐行 drift 归 A3.3-A3.5。本审计发现的设计-代码背离标注交接。
- Successor Required: `yes`——A3.3-A3.5 执行时复核。

### MA2 状态机正确性（迁移/守卫/并发）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 状态机正确性裁决归 MA2（A2.5-A2.15 已 done）。本审计维度 8 只核验状态文档的业务级清晰度，不重做正确性裁决。
- Successor Required: `no`——状态机正确性 MA2 已收口。

## Closure

Status Note: 完成。A3.1 设计文档行为基线审计已执行完毕——产出审计报告 `docs/audits/2026-07-28-1510-arm-ma3-design-doc-baseline.md`（Verdict: FAIL——零 BLOCKER + 功能覆盖度维度 PASS；13 项 MAJOR → P1-MA3-001~013 目标 MR2 + 8 项 P2 watch-only + MA2 owner-doc drift 复核全部确认分类一致），13 项 P1 已登记 arm-index §P1 详细清单，scope matrix §2.3 终态标记 `⚠️(P1)`，roadmap A3.1 推进至 done。本审计为文档层，无代码变更（`git status` 仅 4 个 docs/ 文件），build/test 门控作回归基线确认（`mvn clean install -DskipTests` + `mvn test` 均 BUILD SUCCESS，0 failures/0 errors）。文档修复在 MR2 批量进行。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计（fresh-context general 子代理 `ses_057a9614cffeRlNps2QDQf9HvF`，2026-07-28）
- Verdict: **passes closure audit**（无 BLOCKER / 1 MINOR）
- Evidence: (1) Phase 1 & 2 checklist items 全 `- [x]` + Status `completed` + Exit Criteria 全 `- [x]`；(2) 交付物存在且实质——审计报告含 Verdict/12 维度裁决/维度 1 功能覆盖度结论/finding 清单/MA2 drift 复核表；arm-index 报告清单行 + §P1 详细清单 13 行 P1-MA3-001~013 + MA3 summary note；scope matrix §2.3 终态 `⚠️(P1)` 标注；(3) `git status` 确认仅 docs/ 下 4 文件变更，零 `module-*/`/`app-erp-all/`/`*.orm.xml`/`*.java`/`*.xbiz.xml` 触及（文档审查如声明）；(4) 验证范围：`mvn clean install -DskipTests`（154 模块 full reactor）+ `mvn test` 均 BUILD SUCCESS；(5) citation 抽查 3 项 MAJOR finding（P1-MA3-002/006/008）所引文件路径全部存在，0 非存在引用。
- MINOR（已处置）：roadmap A3.1 闭包时仍 `todo`——执行者已在本 closure 中翻转为 `done`。

Follow-up:

- **MR2（文档类 P1 批量修复）**：P1-MA3-001~013（13 项，依赖 MA3+MA4 done 后由 R2.0 展开机制转化为具体修复工作项行）+ P2-MA3-014~021（8 项 watch-only）。
- **A3.2**（前瞻性缺失扫描，找「从未设计的整个功能」）——successor required，执行时复核本审计维度 1 结论。
- **A3.3-A3.5**（owner doc vs 代码 drift 逐行核对）——successor required；本审计 §9 已标注交接路径（P1-MA2-067/073 + P2-MA2-067/069）。
- **A4.6-A4.8**（view.xml drift）——本审计 §9 已标注交接（dashboards 指标表 vs 实现替代）。
- **每日开发日志**：已更新 `docs/logs/2026/07-28.md`。
