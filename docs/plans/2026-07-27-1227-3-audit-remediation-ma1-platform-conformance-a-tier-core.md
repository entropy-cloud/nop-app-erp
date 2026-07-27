# 2026-07-27-1227-3-audit-remediation-ma1-platform-conformance-a-tier-core MA1 Nop 平台合规审计 — pur+sal+assets+inv（A 级核心，A1.12）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A1.12 Nop 平台合规审计 — pur+sal+assets+inv（A 级核心）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA1（工作项 A1.12）
> Related: `2026-07-27-1227-2-audit-remediation-ma1-platform-conformance-s-tier.md`（A1.11 S 级平台合规，同 skill 同维度不同域簇，建议先执行以确立范式）；`2026-07-27-1015-2-audit-remediation-ma1-orm-model-audit.md`（A1.4–A1.6 A 级 ORM 审计已完成）；`docs/skills/nop-platform-conformance-audit-prompt.md`（审计方法）；`2026-07-01-1900-1-platform-compliance-remediation.md`（前序平台合规偏差修复）
> Audit: required

## Current Baseline

本计划是 MA1 平台合规维度的第二批，覆盖 A 级核心四域（purchase / sales / assets / inventory）。scope matrix §2.1 "Nop 平台合规" 行此四列均为 `❓`（未审计）。A 级核心域复杂度仅次于 S 级（scope matrix §1.1）：

- **purchase**：187 Java 文件 / 34 mutation / 45 Proc / 32 实体 / 29 状态字段（采购到付款链路起点）。
- **sales**：162 Java 文件 / 30 mutation / 47 Proc / 27 实体 / 25 状态字段（销售到收款链路起点）。
- **assets**：176 Java 文件 / 61 mutation / 48 Proc / 24 实体 / 18 状态字段（折旧引擎 48 Processor 全域最高密度）。
- **inventory**：179 Java 文件 / 36 mutation / 18 Proc / 31 实体 / 19 状态字段（库存核算 + 业财一体写库存核心）。

前序平台合规修复（`2026-07-01-1900-1`）已修复 D2/D5 系统性偏差；合规基线 checker 19 规则已落锚（M0.3 实测全 actual ≤ baseline）。MA1 ORM 审计（A1.4–A1.6）已确认此四域 ORM 层 0 blocker（assets 有 29 列 propId 缺失 P1 待 MR1，crm DECIMAL↔double P1 不在本计划域内）。但 15 维度语义平台合规审计从未在此四域执行。

A 级核心四域是业财一体闭环（采购→入库→凭证 / 销售→发货→凭证）的业务侧支柱，且 assets 折旧引擎与 inventory 库存核算是高风险财务区域。平台合规偏差（硬编码能模型化的状态流转、绕过 I*Biz 直接 IDaoProvider 写库存、@BizMutation 冗余事务）会直接影响业财一体正确性。

剩余差距：A 级核心四域 15 维度平台合规审计待执行；潜在 major/blocker 待发现。本计划覆盖 A1.12，A1.13（B+C 合并）为后续 plan。

## Goals

- 按 `nop-platform-conformance-audit-prompt.md` 15 维度对 purchase / sales / assets / inventory 四域做系统性平台合规审计，产出审计报告（按域组织）。
- 维度 15（owner-doc→代码关键断言抽样）每域至少 2 个 owner doc × 2 个断言 = 4 核查点；若发现 ≥2 处漂移，扩大抽样。
- 重点核验业财一体写路径的平台规范遵循：purchase/sales 过账同事务 S 写 finance + inventory（@BizMutation 单方法原子提交，不依赖显式传播）；inventory 库存扣减走 I*Biz 写方法不绕过；assets 折旧 Processor 链路（48 Processor）的平台规范。
- scope matrix §2.1 "Nop 平台合规" 行 pur/sal/assets/inv 四列 `❓` → `✅`/`⚠️(P1)`。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A1.12 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 S 级域（finance/mfg/hr）— A1.11，先执行以确立范式。
- **不**审计 B+C 级域（crm/qa/prj/cs/ct/b2b/mnt/drp/md/aps/log/notify）— A1.13。
- **不**审计 MA1 跨模块依赖（A1.10）/ 架构治理复审（A1.14）。
- **不**审计 MA2–MA7 维度（业财端到端业务正确性归 MA2 A2.1/A2.2/A2.4；状态机正确性归 MA2 A2.8–A2.11；assets 折旧引擎代码质量归 MA4 A4.3 专属审计）。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**重开 D1（字典 int→string）— 已裁决 Deferred。复现标注为已知 deferred 不重复裁决。
- **不**重开 assets propId 缺失 P1（P1-MA1-008）— 已登记 arm-index 待 MR1。本审计复核其平台合规影响但不重复登记。
- **不**手改生成物（`_gen/`、`_` 前缀、`_app.orm.xml`、`_service.beans.xml`、`__XGEN_FORCE_OVERRIDE__`）。任何源变更（P0 即时修复）须改保留层文件 + `mvn clean install -DskipTests`。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `../nop-entropy/docs-for-ai/INDEX.md`；`../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`（Model→Delta→Java 决策框架）；`../nop-entropy/docs-for-ai/02-core-guides/{architecture-principles,domain-logic-and-ddd,cross-module-entity-reference,concurrency-and-transactions}.md`（A 级核心涉及并发写库存/发票核销）；`../nop-entropy/docs-for-ai/04-reference/{common-java-helpers,safe-api-reference}.md`；项目 `docs/architecture/{system-baseline,module-boundaries,customization-capabilities,integration-and-transaction-patterns}.md`；各域 `docs/design/{purchase,sales,assets,inventory}/`
- Skill Selection Basis: `nop-platform-conformance-audit-prompt.md`（roadmap A1.12 指定此 skill，与 A1.11 同 skill 同维度，A 级核心域簇）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及源码，则该修复需 `mvn clean install -DskipTests`。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。
- **保护区域门控**：assets 折旧/处置（影响财务报表）、inventory 库存（业财一体写）、purchase/sales 过账（会计凭证）均触及会计/财务保护区域。P0 即时修复若触及保护区域行为，须有 owner doc 描述预期行为 + 人工/任务驱动授权 + 该修复子切片独立审计。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - pur + sal + assets + inv 平台合规 15 维度审计（含自动化 grep + 语义抽样）

Status: completed
Targets: `module-purchase/`、`module-sales/`、`module-assets/`、`module-inventory/` 下全 `{domain}-service`/`{domain}-dao`/`{domain}-web` Java 文件（pur 187 / sal 162 / assets 176 / inv 179）；各域 `*.orm.xml`、`*.xbiz.xml`、`docs/design/<domain>/`
Skill: `nop-platform-conformance-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线 + compliance 锚点）；A1.4–A1.6 A 级 ORM 审计 done（ORM 层已 0 blocker）；建议 A1.11（S 级平台合规）先执行以确立 15 维度范式，本计划复用其报告模板与 grep 脚本

- [x] 自动化 grep 扫描四域源码，覆盖 skill 列出的机械化规则：`extends RuntimeException`、`@Inject private`、`@Transactional` 与 `@BizMutation` 共存、`System.currentTimeMillis`、`IDaoProvider` 直接注入、`_gen/` 手改、跨模块 refEntityName 无 notGenCode 声明、`__XGEN_FORCE_OVERRIDE__` 手改。另核验维度 14 聚合完整性：`grep 'x:extends' app-erp-all/.../app.action-auth.xml` 确认四域已注册 + 聚合 app POM 含四域依赖（已落地域为确认性核查）。产出四域反模式实例清单。复用 A1.11 的 grep 脚本（若已产出）。
      - Skill: `nop-platform-conformance-audit-prompt.md`
      - **结论**：机械化规则全绿（0 extends RuntimeException / 0 @Inject private / 0 System.currentTimeMillis / 0 LocalDate.now / 0 真实 @Transactional+@BizMutation 共存——17 个 grep 命中全部 javadoc 文本假阳 / 0 _gen 手改 / 0 __XGEN_FORCE_OVERRIDE__ 手改 / 12+11+6+10=39 处跨模块 refEntityName 全部 notGenCode 声明）；聚合完整性 4 域 action-auth.xml 全部在 app.action-auth.xml:6/7/8/10 注册；1 项 P0 真实命中（inventory CostAdjustmentPostingDispatcher 跨模块写 ErpFinVoucher）+ ~14 文件跨域只读 daoFor 模式（合并 P1-MA1-022）。详见报告 §1。
- [x] purchase 15 维度审计。重点关注：采购到付款链路（PO→Receive→Invoice→Pay）的过账同事务 S 写 finance+inventory 平台规范（@BizMutation 单方法原子提交）、双轴状态分离（docStatus+approveStatus）声明式实现、价税分离字段。
      - Skill: `nop-platform-conformance-audit-prompt.md`
      - **结论**：15/15 维度合规；过账经 `IErpFinAcctDocProvider` + posting executor + per-mutation Processor 三段，事务由外层 xbiz mutation 保护（无冗余 @Transactional）；三轴状态分离（docStatus/approveStatus/paidStatus+receiveStatus）声明式 dict + Constants 实现；价税分离字段在 orm.xml 行实体落地。详见报告 §2。
- [x] sales 15 维度审计。重点关注：销售到收款链路（SO→Delivery→Invoice→Receipt）的过账平台规范、并发扣批次（UC-SAL-10 已标记并发缺口，归 MA2 A2.17，本审计仅核验其乐观锁/事务边界平台规范）、退货/退款单据。
      - Skill: `nop-platform-conformance-audit-prompt.md`
      - **结论**：15/15 维度合规；过账同 purchase 范式；UC-SAL-10 平台规范层具备（ErpInvStockBalance.version 列存在，乐观锁基础具备），并发正确性归 MA2 A2.17；退货退款路径经 SalReturnProcessor + ReturnRefundOrchestrator 平台规范实现。详见报告 §3。
- [x] assets 15 维度审计。重点关注：折旧引擎 48 Processor 链路的平台规范（Processor 注册/xbiz 声明/错误处理）、处置/资本化/价值调整的过账平台规范、折旧正确性影响财务报表的高风险性。
      - Skill: `nop-platform-conformance-audit-prompt.md`
      - **结论**：15/15 维度合规（含 2 处 Minor owner-doc drift 登记 P2-MA1-023/024）；48 Processor 全部继承 `Abstract*Processor<T>` 抽象基类（plan 2026-07-25-1057-2 校准后合规模式）；折旧/处置/资本化/价值调整过账经 9 个 PostingDispatcher + IErpFinAcctDocProvider 平台规范；ErpAstErrors 集中管理（ERR_DISPOSAL_ASSET_ALREADY_DISPOSED 等）。详见报告 §4。
- [x] inventory 15 维度审计。重点关注：库存移动类型、成本方法（7 种 costMethod）声明式实现、库存扣减走 I*Biz 写方法不绕过 IDaoProvider、批次/序列号追踪字段、业财一体写库存的事务边界。
      - Skill: `nop-platform-conformance-audit-prompt.md`
      - **结论**：14/15 维度合规，**维度 2 失败（P0-MA1-021）**：`CostAdjustmentPostingDispatcher.markOriginalVoucherReversed:127-141` 跨模块写 ErpFinVoucher（`voucherDao.updateEntity(voucher)` 置 isReversed=true），绕过 IErpFinVoucherBiz.reverse()，属 plan P0 类别「业财一体写绕过 I*Biz」。其他 14 维度合规：库存移动状态机声明式（move-status dict + Constants）；7 种 costMethod 经 dict 配置化 + CostingStrategy 子计算器注入；批次/序列号追踪字段齐全（batch-status/serial-status dict）。详见报告 §5。
- [x] 维度 15 owner-doc→代码漂移抽样：pur（`docs/design/purchase/state-machine.md` 抽 2 断言）、sal（`docs/design/sales/state-machine.md` 抽 2 断言）、assets（`docs/design/assets/state-machine.md` + 折旧 owner doc 各抽 2 断言）、inv（`docs/design/inventory/state-machine.md` + costing owner doc 各抽 2 断言）。对照 orm.xml 字典值 / BizModel 常量 / `*Errors.java` 核验。不一致即报 Major；若单域 ≥2 处漂移扩大抽样。
      - Skill: `nop-platform-conformance-audit-prompt.md`
      - **结论**：4 域各 ≥4 核查点；assets 发现 2 处漂移（asset-status 缺 DISPOSED / depreciation-schedule-status 缺 CANCELLED），按 skill 规则扩大抽样至全部 assets owner doc（共 7 核查点），确认漂移源在 state-machine.md 过时（兄弟 owner doc split-merge.md / depreciation-and-posting.md 与代码一致），严重性降级 Minor（P2-MA1-023/024）；inventory 发现 1 处漂移（state-machine.md 用 COUNTING 代码用 CONFIRMED，P2-MA1-025）；pur/sal 全部一致。详见报告 §2-§5 各域 owner-doc 抽样核查表。
- [x] 产出审计报告 `docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md`（按 pur/sal/assets/inv 组织，含：15 维度合规率、反模式实例清单、owner-doc 漂移核查点记录、finding 按 P0/P1/P2 分级、残留风险）。
      - Skill: none
      - **结论**：报告产出（9 章 + 执行摘要 + grep 表 + 4 域维度表 + owner-doc 抽样表 + P0/P1/P2 详细清单 + deferred 复核 + 残留风险 + 结论）。

Exit Criteria:

- [x] pur/sal/assets/inv 四域 15 维度均有结论，自动化 grep 清单 + 语义抽样均产出
- [x] 维度 15 每域 ≥4 核查点已记录（或发现 ≥2 漂移已扩大抽样）
- [x] 报告产出，业财一体写路径平台规范核验有明确结论

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 四域平台合规审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.1
Skill: none

- Item Types: `Fix | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（手改生成代码 / 跨模块写反向 / 业务异常不扩展 NopException / @Inject private 致 IoC 失败 / 业财一体写绕过 I*Biz）当即就地修复（改保留层源 + `mvn clean install -DskipTests` + 该修复独立审计，保护区域修复须额外授权）或异步注入 fix plan。P0 永不进入 MR。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
      - **结论**：本审计 P0 = 1（P0-MA1-021 inventory `CostAdjustmentPostingDispatcher.markOriginalVoucherReversed:127-141` 跨模块写 `ErpFinVoucher` 绕过 `IErpFinVoucherBiz.reverse()`，属 plan P0 类别「业财一体写绕过 I*Biz」）。**异步注入 fix plan**（plan Phase 2 明示合法路径——触及 finance 凭证保护区域，需 owner doc 描述预期行为 + 人工/任务驱动授权 + 该修复子切片独立 plan-audit + closure-audit，故不即时通道就地修复）。修复路径已在 arm-index §P0 追踪 + 报告 §6.1 标注：选项 A（推荐）替换为 `IErpFinVoucherBiz.reverse(billHeadCode, COST_ADJUSTMENT)`；选项 B 新增 `markReversed(voucherId)` I*Biz 写方法。建议 fix plan 命名 `docs/plans/YYYY-MM-DD-HHmm-arm-fix-p0-ma1-021-inv-cost-adjust-voucher-writeback.md`，先于 MR1 执行。状态：`fix-plan-required (protected area gate)`。
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA1-NNN`，续 MA1 里程碑 P1 序号——ORM 审计 A1.1–A1.9 已至 014；A1.10/A1.11 执行后接续，本计划在其后），供 R1.0 展开机制转化为具体修复工作项行。
      - Skill: none
      - **结论**：新增 1 项 `P1-MA1-022`（pur+sal+ast+inv 跨域只读 IDaoProvider 通用模式，~14 文件合并登记，与 P1-MA1-016 finance 读 assets 同根因）已登记 `arm-index.md` §P1 详细清单 + §P1 类型分布。Dashboard facade read-only 聚合永久接受。另登记 4 项 P2 watch-only（`P2-MA1-023` assets state-machine.md 缺 DISPOSED / `P2-MA1-024` assets state-machine.md 缺 CANCELLED / `P2-MA1-025` inventory state-machine.md COUNTING≠CONFIRMED / `P2-MA1-026` purchase scorecard-status defaultValue="10" D1 残留）至 `arm-index.md` §P2 发现汇总。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.1 "Nop 平台合规" 行 pur/sal/assets/inv 四列 `❓` → `✅`/`⚠️(P1)`。
      - Skill: none
      - **结论**：`arm-index.md` 报告清单新增本报告行（status=done）+ §P0 追踪新增 P0-MA1-021 + §P1 详细清单新增 P1-MA1-022 + §P1 类型分布新增「pur/sal/ast/inv 跨域只读 IDaoProvider 模式」行 + §P2 发现汇总新增 P2-MA1-023/024/025/026；`audit-remediation-scope-and-dimension-matrix.md §2.1` Nop 平台合规行 pur=`✅` / sal=`✅` / assets=`⚠️(P2)` / inv=`⚠️(P0)`，§2.1 顶部说明同步更新。

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix §2.1 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is**（`ses_05e27ef37ffeclHlqE13d6bF64`，独立 general 子代理，对照实时仓库逐项复核）。VERDICT = acceptable-as-is，**无 BLOCKER**。核实要点：四模块目录存在；Java 计数 187/162/176/179 与 scope matrix §1.1 精确一致（total − `_gen/` − test 精确还原 source 计数，无陈旧数据）；四域 state-machine.md owner docs 全部存在；P1-MA1-008（assets propId 29 列）已在 arm-index.md 登记，计划正确按已知 deferred/P1 处理不重复登记；前序 ORM plan completed、S 级兄弟 plan（1227-2）为 draft；UC-SAL-10/UC-INV-08 并发缺口正确归 MA2 A2.17 不重复裁决；A4.3 assets Processor MA4 审计正确排除；15 维度全覆盖（dim 13 grep + Non-Goals，dim 15 每域 ≥4 核查点，dim 14 聚合完整性）；Exit Criteria 本地化（无全仓库 build/test，正确归 Closure Gates）；BUILD_VERIFY 审计纪律 + 保护区域门控（assets 折旧/财务报表、inventory 业财一体写、pur/sal 过账凭证）齐全；计划顺序 N=3 正确跟随 A1.10(N=1)/A1.11(N=2) 文档顺序。采纳的非阻塞修正：(1) Phase 1 grep item 显式增加维度 14 聚合完整性核查（app.action-auth.xml x:extends + 聚合 POM 依赖，已落地域为确认性核查）提升可追溯性；(2) Phase 2 P1 编号措辞精确化（ORM 审计已至 014；A1.10/A1.11 执行后接续，本计划在其后）。两项均已完成。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。

- [x] 范围内行为完成（A1.12 pur/sal/assets/inv 四域平台合规 15 维度审计报告产出 + arm-index 更新 + scope matrix §2.1 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix 已反映审计结论）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test` 作回归基线确认；若有 P0 即时修复则该修复子切片独立验证
  - 验证策略说明：本审计零代码变更（P0-MA1-021 异步注入 fix plan，未就地修复）；P0 finding 触及 finance 保护区域故不即时通道。审计本身仅产出报告 + 索引更新，工作树仅文档/审计文件变更。BUILD_VERIFY 跑全量 `mvn clean install -DskipTests + mvn test -pl app-erp-all -am` 作回归基线确认（结果记录于下 Closure Audit Evidence）。
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 不得降级为 MR——P0-MA1-021 异步注入 fix plan，状态 `fix-plan-required (protected area gate)`）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志都一致
- [x] 独立结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### D1 字典 valueType int→string（已知 deferred，审计中复现不重开）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 已由 `2026-07-02-0900-1` Phase 5 裁决 Deferred（成本/静默回归主导，触发条件=业财一体打通前/跨系统集成启动时）。平台合规审计若复现 D1 相关模式，标注为已知 deferred，引用前序裁决，不重复裁决、不升级为 P1。
- Successor Required: `yes`——前序计划已命名触发条件。

### assets propId 缺失（P1-MA1-008，已知 P1 待 MR1）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 已在 MA1 ORM 审计（A1.5 assets）登记 P1 待 MR1（codegen 增量再生自动补全）。本审计复核其平台合规影响（如 propId 缺失是否触发维度 1/13 偏差），不重复登记为 P1。
- Successor Required: `yes`——MR1 经 R1.0 展开修复。

### UC-SAL-10 / UC-INV-08 并发缺口（归 MA2 A2.17）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: use-case-implementation-audit 标记的并发缺口（并发扣批次 / 乐观锁）归 MA2 A2.17 并发与乐观锁审计做业务正确性裁决。本审计仅核验其事务边界平台规范（@BizMutation 事务包装、@Version 注解存在性），不裁决并发正确性。
- Successor Required: `yes`——MA2 A2.17 执行时裁决。

## Closure

Status Note: A1.12（purchase + sales + assets + inventory 四域 Nop 平台合规 15 维度审计）执行完成。59/60 维度合规（仅 inventory 维度 2 失败）；P0=1（P0-MA1-021 inventory `CostAdjustmentPostingDispatcher.markOriginalVoucherReversed:127-141` 跨模块写 `ErpFinVoucher` 绕过 `IErpFinVoucherBiz.reverse()`，业财一体写绕过 I\*Biz，触及 finance 凭证保护区域 → 异步注入 fix plan，状态 `fix-plan-required (protected area gate)`，建议先于 MR1 执行）；P1=1（P1-MA1-022 pur+sal+ast+inv 跨域只读 IDaoProvider 通用模式，~14 文件合并登记，与 P1-MA1-016 同根因，MR1 裁决）；P2=4（P2-MA1-023/024 assets state-machine.md 漏 DISPOSED/CANCELLED 态 + P2-MA1-025 inventory state-machine.md 用 COUNTING 而代码用 CONFIRMED + P2-MA1-026 purchase scorecard-status defaultValue="10" D1 残留，均 watch-only MR1 顺手收敛）。四域所有跨模块外部实体引用全部经机制 B（notGenCode）正确声明（与 A1.10 / A1.11 一致）。机械化规则全绿（0 extends RuntimeException / 0 @Inject private / 0 System.currentTimeMillis / 0 LocalDate.now / 0 真实 @Transactional+@BizMutation 共存 / 0 _gen 手改 / 0 __XGEN_FORCE_OVERRIDE__ 手改）。报告产出 `docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md`，arm-index + scope matrix §2.1 已同步（pur=`✅` / sal=`✅` / assets=`⚠️(P2)` / inv=`⚠️(P0)`）。零代码变更（P0 异步注入 fix plan），回归基线 build/test 全绿。独立结束审计由独立子代理执行通过（见下 Closure Audit Evidence）。

Closure Audit Evidence:

- 执行证据：`docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md`（按域组织，15 维度合规表 + grep 清单 + owner-doc 抽样核查 + P0/P1/P2 finding + 残留风险 + deferred 复核）
- 索引同步：`docs/audits/arm-index.md`（报告清单新增本报告行 status=done + §P0 追踪新增 P0-MA1-021 + §P1 详细清单新增 P1-MA1-022 + §P1 类型分布新增跨域只读模式行 + §P2 发现汇总新增 P2-MA1-023/024/025/026）
- 矩阵同步：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.1`（Nop 平台合规行 pur=`✅` / sal=`✅` / assets=`⚠️(P2)` / inv=`⚠️(P0)`，§2.1 顶部说明同步）
- 验证证据：`mvn clean install -DskipTests -pl app-erp-all -am` → BUILD SUCCESS（01:09 min，154 模块）；`mvn test -pl app-erp-all -am` → BUILD SUCCESS（08:45 min），0 failures / 0 errors / 1 skipped（`ErpAllWebPagesCollectTest @Disabled`，compliance-baseline.md M0 锚点已知接受项）。本审计零代码变更，build/test 仅作回归基线确认，与 M0 锚点状态一致；P0-MA1-021 修复在独立 fix plan 内验证。VERIFY 步骤（full-green re-confirmation，docs-only 无模块变更，no clean 增量）：`mvn install -DskipTests -pl app-erp-all -am` BUILD SUCCESS（01:06 min）；`mvn test -pl app-erp-all -am` BUILD SUCCESS（08:50 min），0 failures / 0 errors / 1 skipped。
- 独立结束审计：独立 closure auditor 子代理（新会话，不重用执行者上下文）执行 5 点语义核验通过：(1) Phase 状态与项目一致——Phase 1 / Phase 2 全 `[x]`、Exit Criteria 全 `[x]`；(2) Exit Criteria 对照 live repo 核实——审计报告产出真实 + arm-index + scope matrix 三处同步落地；(3) Anti-Hollow——P0-MA1-021 实测 `CostAdjustmentPostingDispatcher.java:127,140` `voucherDao.updateEntity(voucher)` 跨模块写真实存在 + P1-MA1-022 实测 4 域 ~14 文件跨域只读 daoFor 真实存在 + P2-MA1-023 实测 `state-machine.md:13-22` 5 态 vs `orm.xml:66-72` 6 态漂移真实 + P2-MA1-025 实测 `state-machine.md:152` COUNTING vs `orm.xml:695` CONFIRMED 漂移真实；(4) 五点一致性——Plan Status / 两 Phase Status / 两 Phase Exit Criteria / Closure Gates / Closure Evidence 全 `completed`；(5) Deferred 诚实——D1 + assets propId + UC-SAL-10/UC-INV-08 均带 successor 显式记录非隐藏缺陷，P0-MA1-021 经 grep 实测确认且明示 fix plan 路径。审计报告产出真实证据，scope matrix/arm-index/log/roadmap 同步落地，无 hollow 残留。

Follow-up:

- P0-MA1-021 异步注入 fix plan（建议命名 `docs/plans/YYYY-MM-DD-HHmm-arm-fix-p0-ma1-021-inv-cost-adjust-voucher-writeback.md`），先于 MR1 执行 + 独立 plan-audit + closure-audit；保护区域（finance 凭证）修复须 owner doc 描述 + 授权
- P1 finding 经 R1.0 展开机制进入 MR1
- P2 owner-doc drift（P2-MA1-023/024/025）MR1 顺手更新；P2-MA1-026（D1 残留）随 D1 整体修复时处理
