# 2026-08-12-0617-2-entity-state-machine-m0-2-inventory 全域状态轴清单与 M2/M3/M4 展开（M0.2）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Mission: entity-state-machine
> Work Item: M0.2
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M0.2（todo，deps: M0.1）
> Related: `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（N=1，本计划前置）；17 个域 `docs/design/<domain>/state-machine.md`；`docs/skills/state-machine-business-review-prompt.md`
> Audit: required

## Current Baseline

- 17 个业务域有独立 `docs/design/<domain>/state-machine.md`（logistics/assets/maintenance/finance/purchase/inventory/aps/human-resource/b2b/manufacturing/crm/projects/customer-service/quality/sales/contract/drp）。master-data 与 notify **无** `state-machine.md`——其状态语义分别在 `master-data/README.md` 与 `notify/README.md` 维护；M0.2 必须以真实 owner docs 评估这两域是否存在应纳入的独立业务状态轴（路线图基线条）。
- 状态字段/dict 的真相源是各域 `module-<domain>/model/app-erp-<domain>.orm.xml`（`ext:dict` + dict yaml）。命名约定：字典命名空间 `erp-<short>/<dict-name>`，`valueType` 统一 `string`。
- 现存写路径散布在 BizModel `validateTransitionForXxx`、per-mutation Processor 内联比较、facade helper。**「149 Processor」是历史 S-mutation 拆分统计，不是当前状态写路径完整清单**——M0.2 必须从实际 `setStatus`/`setDocStatus`/`setApproveStatus` writer 扫描**重新建立范围**（路线图基线 + 横切关注点）。
- 已知风险：dict 死状态（dict 声明 N 值但生产 writer 仅部分，未写的永不出现），经多轮审计跨 finance/mfg/hr/inv/qa/prj/contract/aps/logistics 同型复发（`docs/lessons/` lesson 10 + `behavioral-failure-mode-scan-prompt.md §2`）。M0.2 的四方对照（dict ↔ owner-doc 迁移图 ↔ StateMachine 元数据 ↔ writer）直接服务于此风险。
- 客服 Ticket `reopen` 会删除未答问卷，M1.1 属 data-deletion ask-first 试点；**用户已于 2026-08-06 批准 M1.1 迁移**（保留既有 reopen 删除行为）。路线图 M1 试点纪律要求：**M0.2 必须在 M1.1 之前完成 Ticket SLA 起算语义的 drift 分类与 owner-doc 对齐**——`customer-service/state-machine.md §实现约定` 已记录实现偏离（`startDateTime = 首次 IN_PROGRESS`，与设计表「SLA 从创建时开始计时」表述不同）；M0.2 须对此 drift 给出明确裁决，否则试点矩阵不得固化任一解释。
- M2/M3/M4 三个里程碑表当前为**空**（路线图规则：展开前不含占位项）；仅 M0.2 有权基于盘点结果向其追加原子工作项，每项一行、一个实体一条状态轴、带行级依赖。
- **M0.1 契约是 M0.2 的前置**：M0.2 的分类法（独立业务轴 / 普通标志 / 技术状态 / 工作流状态）、扫描方法与测试义务依赖 M0.1 `docs/architecture/entity-state-machine-bean.md` 的裁定（尤其是 CRUD 写入边界与只读元数据接口）。

## Goals

- 产出一份**全域状态轴清单文档**（`docs/analysis/2026-08-12-entity-state-axis-inventory.md`），穷举 17 域 + master-data + notify 的全部候选状态字段，逐条裁定**纳入 / 不纳入**并记录理由与重开触发条件。
- 对每个纳入轴记录：迁移语义摘要、关联 owner doc、ORM dict、生产 writer 清单（区分生产/框架入口/测试 fixture）、既有测试、保护区域标记、财务影响标记、跨域副作用标记。
- 完成客服 Ticket **SLA 起算语义 drift 裁决**与 owner-doc 对齐（解除 M1.1 的矩阵固化前置）。
- 向路线图 M2/M3/M4 追加原子工作项（每项一实体一轴、带行级依赖、owner doc、skill、保护区域/财务影响分类），未纳入项记录理由 + 重开触发条件。
- 路线图 Work Item 表与依赖图与本清单一致。

## Non-Goals

- 不写任何代码、不创建任何 StateMachine Bean（迁移归 M1.1 及各展开项）。
- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml（仅只读引用）。
- 不裁定 M0.1 的契约（颗粒度/CRUD 边界/方法形状等）——M0.2 消费 M0.1 产物，不重开契约。
- 不「为每个字典状态字段都生成 Bean」——M0.2 须按分类裁定，普通 `ACTIVE/INACTIVE` 标志、技术处理状态、notify 无业务迁移矩阵的记录均排除（路线图 Non-Goal）。
- 不抽样——M5.1 才是全集审计，但 M0.2 的**纳入/不纳入裁定**必须覆盖全集（路线图 M0.2 纪律 + M5 闭环纪律）。
- 不在本清单中固化迁移矩阵的字面值（迁移矩阵由各迁移项 plan + M0.1 元数据接口落地）。

## Task Route

- Type: `verification or audit work`（全集盘点 + 分类裁定 + 路线图展开，零代码）
- Owner Docs: 17 个 `docs/design/<domain>/state-machine.md` + `master-data/README.md` + `notify/README.md` + 各域 `module-<domain>/model/app-erp-<domain>.orm.xml`（只读真相源）+ `docs/architecture/entity-state-machine-bean.md`（M0.1 产物，提供分类法）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（路线图 M0.2 指定；其「dict 可达性 grep 步骤」+ 状态定义/可达性/异常路径/dict writer 对照正匹配本清单的四方对照与分类裁定工作方法）。其必需输入（定义状态机的 owner doc + 相关需求）已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯文档 + 路线图编辑，零运行时依赖）。
- 前置依赖：M0.1 契约定稿通过（`docs/architecture/entity-state-machine-bean.md` 提供分类法、CRUD 边界裁定与元数据接口形状）。

## Execution Plan

### Phase 1 - 候选穷举与分类裁定

Status: completed
Targets: `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（新）
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 done

- [x] `Proof`（候选穷举）：扫描 17 域 `state-machine.md` + master-data/notify README + 各域 ORM `ext:dict` 状态字段，列出**全部候选状态字段**（实体 × 字段 × dict），形成可复核的全集清单。每条标注物理位置（orm.xml 路径:行 / owner doc 章节）。Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（逐条分类裁定）：对每个候选按 M0.1 分类法裁定为四类之一——`独立业务状态轴`（纳入）/ `普通标志（ACTIVE/INACTIVE 等）`（排除）/ `技术处理状态`（排除）/ `工作流状态（nop-wf）`（排除）。**纳入/不纳入裁定必须覆盖全集**；每个排除项记录理由 + 重开触发条件（路线图横切「工作项扩展」+ M0.2 纪律）。三轴字段（`docStatus`/`approveStatus`/`posted`）按 M0.1 三轴边界分别裁定；`posted` 一律排除为迁移轴。Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（master-data / notify 裁定）：明确裁定这两域是否存在应纳入的独立业务状态轴；若全排除，记录理由（如「仅 ACTIVE/INACTIVE 普通标志」或「notify 模板无业务迁移矩阵」）+ 重开触发条件，回应路线图基线条的未决问题。Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 清单文档已创建，候选穷举段覆盖 17 域 + master-data + notify，每条候选可追溯到 orm.xml/owner-doc 位置。
- [x] 每个候选有明确的纳入/不纳入裁定 + 分类；每个排除项有理由 + 重开触发条件；master-data/notify 裁定段存在且非空。

### Phase 2 - writer 盘点 + 纳入轴属性登记 + Ticket SLA drift 裁决

Status: completed
Targets: `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（补全）；`docs/design/customer-service/state-machine.md`（SLA drift 对齐补注，如裁决要求）
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof | Add | Decision | Fix`
- Prereqs: Phase 1

- [x] `Proof`（writer 实际盘点）：对每个**纳入轴**，从实际代码扫描写路径（`setStatus`/`setDocStatus`/`setApproveStatus` 等），区分**生产 writer**（命名动作 BizModel/Processor）、**框架入口**（标准 CRUD `__save`/`save`）、**测试 fixture**。**禁止**沿用「149 Processor」历史统计作为范围（路线图基线）。Skill: `state-machine-business-review-prompt.md`
- [x] `Add`（纳入轴属性登记）：每个纳入轴在清单中登记：迁移语义摘要、关联 owner doc 章节链接、ORM dict 命名空间、生产 writer 清单、既有测试位置、保护区域标记（财务/数据删除）、财务影响标记（是否触发/冲销/补偿库存或会计过账）、跨域副作用标记。Skill: `state-machine-business-review-prompt.md`
- [x] `Decision | Fix`（Ticket SLA 起算 drift 裁决）：裁定 `customer-service/state-machine.md §实现约定` 记录的 drift（`startDateTime = 首次 IN_PROGRESS` vs 设计表「SLA 从创建时开始计时」）——选定权威解释，分类为 `intentional legacy behavior` / `doc drift` / `implementation drift`。**三分支均有显式 successor，不得静默丢弃**（指南规则 13）：(a) `intentional legacy` → owner doc 补注裁决结论（对齐 §实现约定 既有偏离登记范式）；(b) `doc drift` → 修正设计表表述与 §实现约定 一致；(c) `implementation drift`（代码错）→ **确认为已生效代码缺陷**，本零代码计划不修代码，但须在清单中登记为缺陷并显式指派 **M1.1（Ticket 试点）** 接管代码修复，**不得**降级为 Follow-up。此裁决解除 M1.1 的矩阵固化前置。Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 每个纳入轴的八属性（迁移语义摘要 / owner doc / dict / 生产 writer / 既有测试 / 保护区域 / 财务影响 / 跨域副作用）均非空且可追溯（与 Goals 及 Phase 2 属性登记项一致）。
- [x] Ticket SLA drift 裁决段存在，三分支结论已写入清单：(a)/(b) 完成对应 owner-doc 对齐（仅文档，不改业务行为）；(c) 若为 `implementation drift`，缺陷已登记且显式指派 M1.1 接管代码修复（非 Follow-up 降级）。

### Phase 3 - M2/M3/M4 原子工作项展开 + 路线图同步

Status: completed
Targets: `docs/backlog/entity-state-machine-migration-roadmap.md`（M2/M3/M4 表追加行）；`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（汇总索引回链）
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Add | Decision`
- Prereqs: Phase 2

- [x] `Add`（M2 直接生命周期项展开）：向路线图 M2 表追加每个**纳入的非保护、无财务影响、简单生命周期**状态轴的独立工作项（一实体一轴），每行含：Work Item 编号、Status=`todo`、Owner Doc、Deps（行级，含 M1.3）、Skill。Skill: `state-machine-business-review-prompt.md`
- [x] `Add`（M3 复杂业务/审批轴展开）：向 M3 表追加每个**纳入的无财务影响复杂业务或审批**状态轴的独立工作项；审批轴与业务生命周期轴分开；有跨域副作用的 action 仅替换固定来源/目标态判断（路线图 M3 纪律）。每行含同上字段。Skill: `state-machine-business-review-prompt.md`
- [x] `Add`（M4 财务影响/保护域展开）：向 M4 表单独列出 finance 各状态轴 + 任何域中会触发/冲销/补偿/回写库存或会计过账的 action 所在状态轴；每项标 `plan-first`，行级依赖在展开时定义（不以整个 M2/M3 完成为前置）；`posted` 本身不入表（路线图 M4 纪律）。Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（路线图一致性）：更新路线图依赖图与 Work Item Status 块，使其与新追加的 M2/M3/M4 行一致；确保不被任何 active/draft plan 的 Source/Related 引用的旧表述被同步修正。Skill: none（依赖图与状态块同步是机械文本操作，非状态机业务审查任务，故不套用 `state-machine-business-review-prompt.md`）

Exit Criteria:

- [x] 路线图 M2/M3/M4 三表已按分类填充，每行一实体一轴 + 行级 Deps + Owner Doc + Skill；无占位空行、无「某域全部状态机」单项。
- [x] 路线图依赖图与 Work Item Status 块与新增行一致；清单文档含对新增行的汇总索引（域 × 轴 → 路线图编号）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_00d13d46dffeCGgVpBSvVURmin`) — 无 BLOCKER；1 MAJOR（Ticket SLA drift 三分支中 `implementation drift` 分支缺显式代码修复 successor，违反指南规则 13「已确认缺陷不得降级为 Follow-up」）+ 4 MINOR（Phase 2 项类型声明漏 `Add` / Deferred 分类 `successor-owned` 非模板值 / Closure Gate git-diff 未列路径允许表 / Phase 3 item4 Skill 无理由）。虽裁定 acceptable，仍主动修订 MAJOR 以满足规则 13：drift 裁决项显式三分支 successor（c=implementation drift 登记缺陷并指派 M1.1 接管，非降级）+ Exit Criteria 同步；MINOR 一并修。
- Independent draft review iteration 2: `acceptable as-is` (`ses_00d0f46b3ffehbj7g0b0EXjUkf`) — MAJOR（规则 13 三分支 successor）确认正确落地（implementation drift 分支显式登记缺陷并指派 M1.1，非降级；Exit Criteria 同步）；MINOR 全部处理（Item Types 补 Add / Deferred 分类用合规值 / Closure Gate git-diff 路径允许表 / Phase3 item4 Skill 理由）；新发现 1 MINOR（Exit Criteria 属性计数「六」与列举不符且漏迁移语义摘要，应为八）已随后修正。完整性复扫全部通过（零代码、M0.1 前置消费、全集覆盖、禁用 149 统计、M2/M3/M4 一实体一轴、doc-only 门控、死状态登记不修）。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划为**纯文档 + 路线图编辑**（零代码、零 ORM、零契约变更）。按计划指南，移除构建/测试验证门控并说明：无生产代码变更即无回归可能，验证以「全集覆盖 + 四方可追溯 + 路线图自洽」的文档完整性检查替代。

- [x] 范围内行为完成（清单文档三段齐全 + 路线图 M2/M3/M4 已展开）
- [x] 相关文档对齐（客服 SLA drift owner-doc 补注如裁决要求；路线图依赖图与 Work Item Status 一致）
- [x] 已运行验证（文档完整性，非构建）：(1) 候选穷举覆盖 17 域 + master-data + notify 全集；(2) 每个纳入轴八属性（含迁移语义摘要）非空可追溯；(3) 每个排除项有理由 + 重开触发条件；(4) M2/M3/M4 每行一实体一轴 + 行级 Deps；(5) `git diff --stat` 证实改动仅在 `docs/analysis/`、`docs/backlog/`、`docs/design/`、`docs/logs/` 下——无 `module-*/`、无 `*-codegen/`、无 `model/`、无生产代码 → `docs/audits/nop-compliance-checker.sh` 不可能漂移（仍可运行以留证）
- [x] 无范围内项目降级为 deferred/follow-up（Ticket SLA drift 裁决必须落地，不得悬置；纳入/不纳入裁定必须覆盖全集）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：清单文档、路线图 Work Item 表/依赖图、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 具体迁移矩阵字面值与 Bean 实现

- Classification: `out-of-scope improvement`（指南模板仅允许 `watch-only residual | optimization candidate | out-of-scope improvement`；此项是真正的后续迁移工作，非优化候选，故取 `out-of-scope improvement`）
- Why Not Blocking Closure: M0.2 交付的是分类裁定 + 属性登记 + 路线图展开；每个轴的矩阵字面值与 Bean 实现归对应 M2/M3/M4 迁移项 plan + M0.1 元数据接口。
- Successor Required: yes（触发条件 = 各迁移项 plan 启动时）

### dict 死状态的逐项修复

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.2 的四方对照会**暴露**死状态，但修复（删 dict 值 / 补 writer / 改 owner doc）属各迁移项或独立 Fix plan，按路线图规则 5「发现的 dict 死状态必须按 Fix/Decision 处理」。M0.2 须在清单中**登记**发现的死状态并指派 successor，但不负责修复。
- Successor Required: yes（触发条件 = 清单登记的死状态，由对应迁移项或独立 Fix plan 接管）

## Closure

Status Note: M0.2 全域状态轴清单已完成。产物：(1) `docs/analysis/2026-08-12-entity-state-axis-inventory.md`——穷举 19 模块全部候选状态字段（~120 ERP 业务候选 + ~45 posted + ~30 md-ext-ref），逐条分类裁定（纳入 103 轴：M2=19 + M3=19 + M4=65；排除项均记录理由 + 重开触发条件），每个纳入轴登记八属性 + 死状态登记 + dict 异常登记；(2) 路线图 M2/M3/M4 三表展开 103 行原子工作项 + 依赖图更新 + M0.2 Status→done；(3) Ticket SLA drift 裁决为 intentional legacy behavior + owner-doc 补注。零代码计划——验证以文档完整性检查替代构建/测试；git diff 证实改动仅在 `docs/` 下。

Closure Audit Evidence:

- Auditor / Agent: 执行会话内自验证（零代码文档计划）；独立结束审计由后续子代理新会话执行
- Evidence: 清单文档 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（全集裁定 + 八属性 + drift 裁决 + 死状态登记）；路线图 `docs/backlog/entity-state-machine-migration-roadmap.md`（M2=19+M3=19+M4=65 展开 + 依赖图 + M0.2 done）；`docs/design/customer-service/state-machine.md` §实现约定 M0.2 裁决补注；Draft Review Record 2 轮收敛
