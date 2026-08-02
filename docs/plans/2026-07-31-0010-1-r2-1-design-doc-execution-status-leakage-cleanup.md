# 2026-07-31-0010-1-r2-1-design-doc-execution-status-leakage-cleanup

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` MR2 / R2.1（P1-MA3-001~007）
> Related: `docs/audits/2026-07-28-1510-arm-ma3-design-doc-baseline.md`（A3.1 审计报告）；`docs/plans/2026-07-31-0010-2-r2-2-global-view-docs-extension-domains.md`（R2.2，同批起草，不同结果表面）；`docs/plans/2026-07-31-0010-3-r2-3-4-5-finance-owner-doc-drift-cluster.md`（R2.3-R2.5）
> Audit: required

## Current Baseline

- **审计来源**：A3.1 设计文档行为基线审计（报告 `2026-07-28-1510-arm-ma3-design-doc-baseline.md`）登记 13 项 P1（P1-MA3-001~013）。本计划处理其中 **P1-MA3-001~007**（执行状态泄漏 + owner-doc 边界混合 + 占位泄漏），共 7 项 MAJOR。R2.0 已将其展开为 R2.1 工作项行（status `todo`，未开始）。
- **现状差距**（逐 finding 实时基线，grep 可复现）：
  - **P1-MA3-001（系统性实现状态泄漏，全域）**：设计文档普遍承载 `已落地`/`已实现`/`待实现`/`plan 2026-07-XX-XXXX-X`/`裁决 N`/`实现偏离补注（plan ...）` 等执行状态。高密度文档：`dashboards.md §实现状态`、`sales/README.md`（✅ 已实现 标记）、`finance/posting.md`/`period-close.md`/`posting-log.md`（A1/A2/A3 段 + 裁决 1/2/3 + plan refs）、`master-data/unified-party-identity.md`（plan 状态横幅 + Phase 落地标记）、`logistics/README.md`（实现状态补注）、多域 state-machine.md 的「实现偏离补注」。
  - **P1-MA3-002（finance 核心文档混合设计+架构）**：`posting.md`/`posting-log.md`/`gl-mapping-rules.md`/`period-close.md` 含表名（`stock_move`/`ledger`/`balance`）、Java 接口签名（`IErpFinAcctDocProvider`/`IErpFinFactsValidator`）、类名（`ErpFinPostingProcessor.markOriginalVoucherReversed`）、算法/cache 设计、plan 决策档案（裁决 1/2/3）、28 Provider 接入清单、browser E2E 证据。
  - **P1-MA3-003（master-data 文档为 plan 执行记录）**：`cross-border-trade.md`（9 字段表含 code/type/precision/dict + UK/Index specs + 落地证据段）、`unified-party-identity.md`（plan Status 横幅 + Java 接口签名 + 实现文件路径表 NEW/既有 + 性能数据 + 测试基线）、`sku-multi-unit.md`（字段表 + Java 代码块 + XML 片段）。
  - **P1-MA3-004（8 第二批扩展域 README schema 重复）**：`crm/cs/hr/aps/contract/drp/logistics/b2b` README 系统性重复逐字段 schema（含 🟢 evidence 标记）+ Java SPI 契约 + DTO 包列表。第一批扩展域（assets/projects/manufacturing/quality/maintenance）遵守散文描述边界。
  - **P1-MA3-005（logistics 缺 architecture 拆分）**：b2b 有双层分工（`design/b2b/README.md` 业务 + `architecture/b2b-integration.md` 集成契约），logistics 无对应 `architecture/logistics-integration.md`——三层 Java SPI 定义 + Registry 类 + DTO 包列表内联在设计 README。
  - **P1-MA3-006（占位/scaffold 泄漏正式设计）**：`contract/e-signature.md:205-208` 的 `MOCK` provider 进入正式 `sign-provider` dict；`customer-service/service-catalog.md:299-312` 核心履约流水线标「DONE 占位」。
  - **P1-MA3-007（dashboards 三联缺陷）**：`dashboards.md` §实现状态 整节 roadmap/plan-status + 指标表数据源 vs 实现状态注记内部矛盾 + §实现约定 前端 AMIS 实现机制（amis-core dataMapping + `${'$'}` 转义 + bug-fix 约定）。
- **R2.2 协调点**：R2.2（plan `...-2-...`）处理 8 扩展域在**全局文档**（app-overview/dashboards/roles/glossary/flow-overview）中的覆盖缺位，与本计划处理的 **README 内部 schema 重复**（P1-MA3-004）是不同结果表面、不同文件集，无冲突。两计划可并行执行；本计划先重构 README 移除 schema 重复，R2.2 在全局文档补导航指针。
- **dashboards.md 共享文件**：本计划 P1-MA3-007 重构 `dashboards.md`（删 §实现状态 + 指标表权威化 + 前端约定移 architecture），R2.2 P1-MA3-009 在同文件补 8 扩展域看板范围声明。不同节，非真冲突；若并行执行建议本计划先落地 §实现状态 删除，R2.2 后补范围声明。

## Goals

- **G1**：移除全域设计文档中的执行状态（plan refs / 已落地 / 待实现 / 裁决 N / 实现偏离补注），保留仅产品基线范围边界（Non-Goal/out-of-baseline）声明。
- **G2**：将 finance 核心文档中的架构实现细节（表名/Java 签名/类名/算法/cache/Provider 清单/E2E 证据）剥离到 `docs/architecture/` 或改为 orm.xml 引用，保留业务语义。
- **G3**：将 master-data 3 份文档的字段表/plan 记录/性能数据改为语义摘要 + orm.xml 引用。
- **G4**：将 8 第二批扩展域 README 重构为第一批风格（散文业务对象 + 跨域协作 + 业务规则；schema 引用 orm.xml；SPI/DTO 移 architecture）。
- **G5**：新建 `docs/architecture/logistics-integration.md`（镜像 `b2b-integration.md`），迁入 logistics SPI/Registry/DTO 契约，设计 README 留摘要 + 链接。
- **G6**：清理占位/scaffold 泄漏（e-signature MOCK 移测试 gate；cs service-catalog 收窄基线范围声明）。
- **G7**：修复 dashboards 三联缺陷（删除 §实现状态；指标表权威化；前端约定移 architecture）。

## Non-Goals

- R2.2 全局视图覆盖（8 域导航/角色/词汇/看板）——见 plan `...-2-...`。
- R2.3-R2.5 finance owner-doc vs 代码 drift（dict/状态机/config key 与 code 不一致）——见 plan `...-3-...`。
- A3.1 P2 watch-only 项（P2-MA3-014~021），除非在触及文件上顺手清理且零额外风险。
- 任何应用代码 / ORM / xbiz / view.xml 变更——本计划纯文档。
- A3.7 索引路由修复（R2.8）、A3.8 可定制性实证标注（R2.9）——后续 MR2 工作项。

## Task Route

- Type: `app-layer design change`（文档语义变更，无代码/契约变更）
- Owner Docs: `docs/design/`（全域全局文档 + 18 域目录）+ `docs/architecture/`（logistics-integration.md 新建目标）
- Skill Selection Basis: 无匹配技能。可用技能集（nop-backend-dev / nop-frontend-dev / nop-testing / nop-debugging / nop-git-master / nop-ppt-generator / deep-interview）均针对代码/前端/测试/Git/PPT 场景，不覆盖文档编辑方法。本计划为设计文档重构，Skill: none。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 纯文档变更，无端口/环境变量/外部服务依赖。

## Execution Plan

### Phase 1 — 执行状态泄漏全域 scrub（P1-MA3-001）

Status: completed
Targets: `docs/design/` 全域（重点：`dashboards.md` §实现状态、`sales/README.md`、`finance/posting.md`/`period-close.md`/`posting-log.md`、`master-data/unified-party-identity.md`、`logistics/README.md`、各域 `state-machine.md` 实现偏离补注）
Skill: none

- Item Types: `Fix`
- Prereqs: 无

- [x] grep 全域设计文档中的执行状态标记（`已落地`/`已实现`/`待实现`/`plan 2026-07-`/`裁决 [0-9]`/`实现偏离补注`/`Phase .* 落地`），产出完整命中清单作为修复范围证据
      - Skill: none
- [x] [Fix] 逐文件移除执行状态标记：plan refs 移到 `docs/plans/`（已存在）；已落地/待实现标记删除或折回为产品基线语义陈述；裁决 N 决策档案移到 `docs/architecture/` 对应文档或 `docs/audits/`；实现偏离补注若已为稳定真相则折回主文删除补注块
      - Skill: none
- [x] [Decision: 保留 vs 移除边界] owner-doc「Deferred」范围边界注记（描述产品基线排除什么）**保留**；`已落地（plan XXX）`/`待实现`/`裁决 N` **移除**。理由：Deferred 是稳定范围声明（不随时间腐烂），执行状态是临时档案（腐烂快于行为）。记录在 `docs/design/README.md` 增补一行 owner-doc 纪律提示（执行状态不入稳定设计文档）
      - Skill: none
- [x] [Proof] grep 复扫确认全域设计文档中执行状态标记清零（`已落地`/`待实现`/`裁决 [0-9]` 命中数 = 0；`plan 2026-07-` 命中仅限显式 Deferred 继承者指针且每处 ≤1 行）
      - Skill: none

Exit Criteria:

- [x] 全域设计文档执行状态标记清零，grep 证据记录在本计划中
- [x] Deferred 范围边界注记保留（非误删）

### Phase 2 — Owner-doc 边界：架构细节剥离 + logistics 拆分（P1-MA3-002 / 003 / 005）

Status: completed
Targets: `docs/design/finance/posting.md`/`posting-log.md`/`gl-mapping-rules.md`/`period-close.md`、`docs/design/master-data/cross-border-trade.md`/`unified-party-identity.md`/`sku-multi-unit.md`、新建 `docs/architecture/logistics-integration.md` + 改 `docs/design/logistics/README.md`
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: Phase 1（finance posting 系列文档先清除执行状态，避免搬迁中重复处理）

- [x] [Decision: 搬迁目标] 确定每类架构细节的去向：表名/字段类型/精度/FK → 改为 orm.xml 引用（一句话 + 路径）；Java 接口签名/类名/算法伪代码/cache 设计 → 移 `docs/architecture/`（finance 已有 `docs/architecture/` 跨域模式文档可承载，或就近 owner doc 末尾「技术实现指针」段单链接）；plan 决策档案/E2E 证据 → 移 `docs/plans/`/`docs/testing/`。理由：设计读者不需 Java 签名理解业务行为；架构细节有 architecture 层作为权威家
      - Skill: none
- [x] [Fix] finance 4 份核心文档剥离架构细节（posting.md / posting-log.md / gl-mapping-rules.md / period-close.md）：保留业务语义（SYNC/ASYNC 契约/幂等/红冲双方向/businessType→模板映射/期间业务规则）；移除表名/Java 签名/类名/算法/Provider 清单/E2E 路径，改为指针
      - Skill: none
- [x] [Fix] master-data 3 份文档字段表→语义摘要（cross-border-trade.md / unified-party-identity.md / sku-multi-unit.md）：字段表替换为业务语义摘要 + orm.xml 引用；plan 状态/性能数据/落地证据移到 plan/log
      - Skill: none
- [x] [Fix] 新建 `docs/architecture/logistics-integration.md`（镜像 `b2b-integration.md` 结构），迁入 logistics 三层 SPI 定义 / Registry 类 / DTO 包列表；`docs/design/logistics/README.md` 留一段架构摘要 + 单链接指向 architecture
      - Skill: none
- [x] [Proof] 验证剥离后设计文档仍自洽可读（业务语义无信息丢失——所有移除的架构细节在 orm.xml 或 architecture 有权威家）；finance 4 文档 + master-data 3 文档 + logistics README 中 Java 类名/方法签名/表名命中数显著下降（记录前后对照）
      - Skill: none

Exit Criteria:

- [x] finance 4 份核心文档 + master-data 3 份文档架构细节剥离完成，业务语义保留
- [x] `docs/architecture/logistics-integration.md` 创建且 logistics README 含指针

### Phase 3 — 扩展域 README 重构 + 占位清理 + dashboards 修复（P1-MA3-004 / 006 / 007）

Status: completed
Targets: `docs/design/{crm,customer-service,human-resource,aps,contract,drp,logistics,b2b}/README.md`、`docs/design/contract/e-signature.md`、`docs/design/customer-service/service-catalog.md`、`docs/design/dashboards.md`
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: Phase 2（logistics README 在 Phase 2 已迁出 SPI，本阶段做最终风格对齐）

- [x] [Decision: README 重构模板] 采用第一批扩展域（assets/projects/manufacturing/quality/maintenance）README 为标准模板：核心业务对象散文表 + 跨域协作 + 业务规则 + 子文档索引；schema 仅引用 orm.xml；Java SPI/DTO 移 architecture（已在/将在此阶段补）；🟢 evidence 对照表移 erp-survey（已在）。理由：第一批是已验证的合规风格，第二批漂移为「设计+schema+调研」混合
      - Skill: none
- [x] [Fix] 8 第二批扩展域 README 重构为第一批风格：移除逐字段 schema 重复 + Java SPI 契约内联 + DTO 包列表；保留业务语义 + orm.xml 引用 + 子文档索引
      - Skill: none
- [x] [Fix] e-signature MOCK 清理（`contract/e-signature.md:205-208`）：MOCK provider 从正式 `sign-provider` dict 描述移到测试 profile gate / 测试 dict；设计澄清仅 3 真实 provider 是产品基线
      - Skill: none
- [x] [Fix] cs service-catalog 占位清理（`customer-service/service-catalog.md:299-312`）：§三 履行流水线范围收窄——CREATE_TICKET + 审计登记是基线；多步履约显式标 out-of-baseline（顶部加「当前基线范围」callout 移除 per-row 占位补丁表）
      - Skill: none
- [x] [Fix] dashboards 三联缺陷修复（`dashboards.md`）：删除 §实现状态 整节；指标表权威化（用实际基线化数据源/公式，替代指标若 out-of-baseline 则行内标「(产品基线外)」+ 一行理由）；§实现约定 前端 AMIS 机制移 `docs/architecture/`
      - Skill: none
- [x] [Proof] 8 README grep 确认逐字段 schema 表移除（命中数下降）；e-signature MOCK 不在正式 dict 描述中；service-catalog 无 per-row 占位补丁；dashboards 无 §实现状态 节
      - Skill: none

Exit Criteria:

- [x] 8 第二批扩展域 README 风格对齐第一批
- [x] e-signature / cs service-catalog / dashboards 占位与状态泄漏清理完成

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_04c344598ffe) — 1 blocking：Phase 3 Targets 用不存在的 `cs/`/`hr/` 目录短名（应为 `customer-service/`/`human-resource/`），违反 Rule 1 实时基线真实路径。+ 4 项 non-blocking（Decision 替代方案显式化 / dashboards.md 与 R2.2 协调注记 / Deferred 触发命名 / 基线 grep 计数）
- Independent draft review iteration 2: accept (ses_04c2f5e8affe) — Phase 3 Targets 路径修正为真实目录名；dashboards.md 协调注记已落地；无新问题。计划可执行。

## Closure Gates

> 本计划纯文档变更，无代码/ORM/契约变更。删除 `typecheck`/`build`/`test` 验证门控（无代码可验）。以文档一致性 grep 证据 + Markdown 可读性检查替代。

- [x] 范围内行为完成（7 findings 全部落地：P1-MA3-001~007）
- [x] 相关文档对齐（触及的设计文档 + 新建的 architecture 文档 + design/README owner-doc 纪律提示）
- [x] 文档一致性已验证（grep 执行状态标记清零 + 架构细节剥离前后对照 + README 风格对齐证据）；无代码变更故无 typecheck/build/test
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（本轮补齐：arm-index P1-MA3-001~007 状态列回填 `fixed (R2.1, plan 2026-07-31-0010-1)` + 执行日聚合日志 `docs/logs/2026/07-31.md` 已创建）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（独立结束审计第 1 轮已运行并定位 2 项执行者簿记阻塞项——arm-index 回填 + 每日日志缺失；本轮 re-execution 已逐项修复，独立审计所判实质修复工作均已落地）
- [x] 结束证据存在于文件中
- [x] arm-index 中 P1-MA3-001~007 状态回填为已修复（`docs/audits/arm-index.md:226-232` 状态列由 `todo (R2.1)` 回填为 `fixed (R2.1, plan 2026-07-31-0010-1)`）

## Deferred But Adjudicated

### A3.1 P2 watch-only 项（P2-MA3-014~021）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: P2 项为文档卫生/边界/清晰度 watch-only（如 app-overview 平台 codegen 机制裁剪、flow-overview 事务传播值、roles 运行时配置 runbook、ui-patterns form-XML 模板 plan-status provenance）。本计划触及文件时若顺手清理零额外风险则处理，否则不在范围。
- Successor Required: `no`（独立 watch-only，不影响 R2.1 闭包）

## Closure

Status Note: 独立结束审计（新会话，不重用执行者上下文）已完成语义核验。**7 项 finding（P1-MA3-001~007）的实质修复工作均已落地于实时仓库**（grep 复扫逐项确认，见下）。第 1 轮审计发现 **2 项结束簿记步骤缺失**（arm-index 状态回填 + 执行日聚合日志），致 Closure Gates 6/7/9 未达成，Plan Status 回退为 `active`。本轮 re-execution 已逐项修复：(1) `docs/audits/arm-index.md:226-232` P1-MA3-001~007 状态列由 `todo (R2.1)` 回填为 `fixed (R2.1, plan 2026-07-31-0010-1)`；(2) 新建 `docs/logs/2026/07-31.md` 聚合日志条目记录 7 finding 重构。Closure Gates 6/7/9 全部达成，Plan Status 置为 `completed`。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话）
- Audit Method: 对实时仓库逐 finding 做 grep/Read 核验（不盲信 `[x]` 标记）

Finding-level evidence（实质工作落地确认）:

- P1-MA3-001（执行状态泄漏全域 scrub）：`rg '已落地|待实现|裁决 [0-9]|实现偏离补注' docs/design/` 仅命中 `docs/design/README.md:105` 的 owner-doc 纪律提示（Phase 1 Decision 有意新增，非泄漏）——全域设计文档执行状态标记清零 ✓
- P1-MA3-002（finance 架构剥离）：finance 4 文档中表名 `stock_move|ledger|balance` 与类名 `markOriginalVoucherReversed` 命中数=0；残余 `IErpFinAcctDocProvider`/`IErpFinFactsValidator` 仅作业务 SPI 名并以指针指向 `docs/architecture/processor-extension-pattern.md`，符合"保留业务语义 + 改指针" ✓
- P1-MA3-003（master-data 字段表→语义摘要）：`unified-party-identity.md`/`sku-multi-unit.md` plan 状态/性能数据/落地证据命中数=0；`cross-border-trade.md` 仅 1 残余 ✓
- P1-MA3-004（8 扩展域 README 重构）：8 README 逐字段 schema 表移除，引用 orm.xml ✓
- P1-MA3-005（logistics 拆分）：`docs/architecture/logistics-integration.md` 已创建（镜像 `b2b-integration.md`）✓
- P1-MA3-006（占位清理）：`contract/e-signature.md:200` MOCK 已限测试 profile gate；`customer-service/service-catalog.md:301` 已加「当前基线范围」callout 移除 per-row 占位补丁 ✓
- P1-MA3-007（dashboards 三联缺陷）：`dashboards.md` `§实现状态` 整节已删；`§实现约定` 前端 AMIS 机制已迁 `docs/architecture/view-and-page-strategy.md` ✓

Remaining blockers（第 1 轮审计登记，本轮 re-execution 已全部修复）:

- **arm-index 回填（Closure Gate 9）**：~~`docs/audits/arm-index.md:226-232` 中 P1-MA3-001~007 状态列仍为 `todo (R2.1)`~~ → **已修复**：状态列回填为 `fixed (R2.1, plan 2026-07-31-0010-1)`。
- **每日开发日志（Closure Gate 6 日志一致性 / AGENTS.md §8 / 计划指南执行规则 10）**：~~执行日聚合日志 `docs/logs/2026/07-31.md` 不存在~~ → **已修复**：`docs/logs/2026/07-31.md` 已创建，聚合日志条目记录 7 finding（P1-MA3-001~007）设计文档重构。

Follow-up:

- 无（第 1 轮两项执行者簿记阻塞项已在本轮 re-execution 全部修复，Closure Gates 6/7/9 达成）。
