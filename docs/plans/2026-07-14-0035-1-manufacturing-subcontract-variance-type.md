# 2026-07-14-0035-1-manufacturing-subcontract-variance-type 制造委外差异类型

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-13-0455-2-manufacturing-cost-element-decomposition.md` Deferred But Adjudicated「subcontract 委外差异（ProductionVarianceCalculator SUBCONTRACT 差异类型）」（Successor Required: yes，触发条件=委外差异分析业务需求落地时——委外引擎 0455-1 + 委外费归集 0455-2 Phase 2 已落地，标准侧 subcontractCost 已由 CostRollupService 聚合填充，实际侧 wo.subcontractCost 列已存在但差异引擎从未消费，触发条件已满足）
> Related: `2026-07-13-0455-1`（委外引擎 completed）、`2026-07-13-0455-2`（成本要素拆分 completed，本计划解除其 Deferred）、`2026-07-05-1838-2`（生产差异 5 类 completed，本计划补第 6 类 SUBCONTRACT）
> Audit: required

## Current Baseline

- `ProductionVarianceCalculator`（`module-manufacturing/erp-mfg-service/.../costing/ProductionVarianceCalculator.java`，443 行）当前计算 5 类差异：`MATERIAL_USAGE` / `LABOR_EFFICIENCY` / `LABOR_RATE` / `OVERHEAD` / `VOLUME`。类 Javadoc :62-64 明示「SUBCONTRACT 要素本期不算差异（5 类差异类型未含 SUBCONTRACT，委外差异需求落地时新增类型码）」。引擎读取 `wo.materialCost`(:128)、`wo.laborCost`(:143)、`wo.overheadCost`(:169)，但**从不读取 `wo.subcontractCost`**。
- `erp-mfg/variance-type` 字典（`module-manufacturing/model/app-erp-manufacturing.orm.xml:147-153`）仅含上述 5 码，无 SUBCONTRACT。配套 `erp-mfg/cost-element` 字典（:155-160）**已含 SUBCONTRACT**（4 码：MATERIAL/LABOR/OVERHEAD/SUBCONTRACT）。
- `ErpMfgConstants`（`module-manufacturing/erp-mfg-service/.../ErpMfgConstants.java:175-179`）仅含 5 个 `VARIANCE_TYPE_*` 常量。
- `ErpMfgWorkOrder` 实体**已有 `subcontractCost` 列**（`_ErpMfgWorkOrder.java:113-114`，orm.xml :594，propId 23），实际侧数据源已就位。
- `CostRollupService.CostBreakdown`（:365-372）已含 `subcontract` 字段，经 `aggregateSubcontractCost(materialId)`（:257-280，config-gated `erp-mfg.subcontract-cost-aggregation-enabled` 默认关）聚合 `ErpMfgSubcontractOrder.processingFee` 填充 `ErpMfgCostRollupLine.subcontractCost`（orm.xml :1269）。标准侧数据源已就位。
- `ProductionVarianceDispatcher`（171 行）`:92-94` 仅提取 3 个要素桶（MATERIAL/LABOR/OVERHEAD），无 subcontract 桶。
- `ProductionVarianceAcctDocProvider`（144 行）`:52-57` 定义 3 对科目（1410/1411 材料、1412/1413 人工、1414/1415 制造费用），无委外差异科目对。
- 委外费 GL 过账（`SUBCONTRACT_FEE(504)` Dr 1408/Cr 2202）经 `SubcontractPostingDispatcher.dispatchFeePosting`(:119-144) 已落地。
- 设计文档 `docs/design/manufacturing/variance-analysis.md`（99 行）差异分类表 :14-22 列 6 概念类型（无委外），`varianceType` 枚举 :35 仅 5 码。
- `docs/design/finance/costing-methods.md:76` 明示「subcontract 委外差异 successor」为本计划触发源。

剩余差距：6 层一致性缺失——(1) 字典码、(2) Java 常量、(3) 计算引擎第 6 行、(4) 派发器第 4 桶、(5) 科目对、(6) 设计文档段。

## Goals

- 在 `ProductionVarianceCalculator` 中新增第 6 类差异 `SUBCONTRACT`（委外费差异），标准侧从 `ErpMfgCostRollupLine.subcontractCost` 取值、实际侧从 `ErpMfgWorkOrder.subcontractCost` 取值。
- 扩展 `variance-type` 字典 + `ErpMfgConstants` 常量，保持模型与代码一致。
- 扩展 `ProductionVarianceDispatcher` 提取第 4 要素桶（SUBCONTRACT），经 `ProductionVarianceAcctDocProvider` 过账至新科目对。
- 更新 `variance-analysis.md` 设计文档补 SUBCONTRACT 差异段。

## Non-Goals

- 工作中心 laborRate/overheadRate schema 拆分（0455-2 Deferred，须 ask-first ORM 批准）——独立 successor。
- 委外订单 reverseProcess 红冲（0701-2 Deferred，后端未实现）——独立 successor。
- 独立 SubcontractIssue/Receipt/Invoice 实体（0455-1 Deferred）——独立 successor。
- 委外费行级精确归集（当前为订单头 processingFee 单一金额，subcontracting.md:231 已声明 successor）——本计划沿用既有头级金额通道。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/manufacturing/variance-analysis.md`、`docs/design/finance/costing-methods.md`
- Skill Selection Basis: 后端 BizModel/计算引擎/过账 Provider 变更，加载 `nop-backend-dev`（实体服务 + 跨实体调用 + 过账 Provider 模式）；ORM 字典码变更走标准 model→codegen→dao 链，加载 `nop-backend-dev` 决策门。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 委外费标准成本聚合 config `erp-mfg.subcontract-cost-aggregation-enabled`（默认关）在测试中按需开启；`erp-mfg.variance-auto-calc-enabled`（默认关）控制完工触发差异计算——两 config 已在 `playwright.config.ts` webServer JVM args 累积启用（1800-2 范式），本计划沿用。

## Execution Plan

### Phase 1 — 模型层：variance-type 字典码 + 科目种子

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`（dict :147-153）；`docs/design/manufacturing/variance-analysis.md`（:14-22 差异分类表 + :35 varianceType 枚举 + 核心计算逻辑段）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: 无

- [x] 在 `erp-mfg/variance-type` 字典中新增 `SUBCONTRACT` 码（标签「委外费差异」），插入 VOLUME 之后。
  - Skill: `nop-backend-dev`
- [x] 在 `ErpMfgConstants` 新增 `VARIANCE_TYPE_SUBCONTRACT = "SUBCONTRACT"` 常量（:179 之后）。
  - Skill: `nop-backend-dev`
- [x] 在 `variance-analysis.md` 差异分类表补 SUBCONTRACT 行（标准 = rollupLine.subcontractCost × 完工量；实际 = wo.subcontractCost）；`varianceType` 枚举补 SUBCONTRACT；核心计算逻辑补公式。
  - Skill: `none`
- [x] 在 `costing-methods.md` :76 将 successor 注解替换为已落地标记（指向本计划）。
  - Skill: `none`

Exit Criteria:

> 字典码经 `mvn clean install -DskipTests` 增量重新生成后 `_app.orm.xml` 镜像含 SUBCONTRACT；设计文档内部无残留「successor」声明指向本差异类型。

- [x] `xmllint --noout module-manufacturing/model/app-erp-manufacturing.orm.xml` 通过
- [x] `rg "SUBCONTRACT" module-manufacturing/erp-mfg-dao/src/main/resources/_vfs/erp/mfg/orm/_app.orm.xml` 命中新字典码

### Phase 2 — 计算引擎：ProductionVarianceCalculator 第 6 类差异

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../costing/ProductionVarianceCalculator.java`（:104-199 calculateVariances 方法体 + :62-64 Javadoc）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 在 `calculateVariances` 中，于 VOLUME 行之后新增 SUBCONTRACT 差异行：标准 = `stdLine.subcontractCost × completed`；实际 = `wo.subcontractCost`；`costElement = SUBCONTRACT`；`varianceType = SUBCONTRACT`。仅当 `stdLine.subcontractCost` 或 `wo.subcontractCost` 非零时生成行（对齐既有「零差异不生成行」范式：:182-186 VOLUME 守卫）。
  - Skill: `nop-backend-dev`
- [x] 更新类 Javadoc :62-64：移除「SUBCONTRACT 要素本期不算差异」声明，改为「6 类差异类型含 SUBCONTRACT」。
  - Skill: `none`

Exit Criteria:

> 计算引擎在标准侧 subcontractCost 非零且实际侧 subcontractCost 非零时生成 SUBCONTRACT 差异行；两侧均为零时不生成行（不污染既有 5 类差异输出）。

- [x] `mvn compile -pl module-manufacturing/erp-mfg-service` 通过（计算引擎变更可编译，不破坏既有 115 tests）

### Phase 3 — 过账链：Dispatcher 第 4 桶 + AcctDocProvider 科目对 + 测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../posting/ProductionVarianceDispatcher.java`（:92-94 要素桶提取）；`module-manufacturing/erp-mfg-service/.../posting/ProductionVarianceAcctDocProvider.java`（:52-57 科目常量 + :80-108 过账逻辑）；`module-manufacturing/erp-mfg-service/src/test/...`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] 在 `ProductionVarianceDispatcher` 要素桶提取中新增 SUBCONTRACT 桶（group by `costElement=SUBCONTRACT` 聚合 netVariance）。
  - Skill: `nop-backend-dev`
- [x] 在 `ProductionVarianceAcctDocProvider` 新增科目常量对：`SUBJECT_SUBCONTRACT_VARIANCE = "1416"`（制造差异-委外）+ `SUBJECT_WIP_SUBCONTRACT = "1417"`（在制品-委外）；在 `appendElementFacts` 过账逻辑中处理 SUBCONTRACT 要素（方向与既有 3 要素一致：不利 Dr 差异/Cr 在制品，有利 Dr 在制品/Cr 差异）。
  - Skill: `nop-backend-dev`
- [x] 补充种子 COA：`_init-data/erp_md_subject.csv` 新增 1416/1417 科目行（对齐 1800-2 1410-1415 范式）。
  - Skill: `none`
- [x] 新增/扩展单元测试：`TestProductionVarianceCalculator`（或既有测试类）覆盖 SUBCONTRACT 差异行生成（标准侧非零 + 实际侧非零 → 行生成 + 金额正确；两侧零 → 行不生成）；`TestProductionVarianceDispatcher`（或既有）覆盖 SUBCONTRACT 桶聚合 + `PRODUCTION_VARIANCE` 凭证行 Dr 1416/Cr 1417 精确数值断言。
  - Skill: `nop-testing`

Exit Criteria:

> 完工入库触发差异计算时，SUBCONTRACT 要素净差异非零则生成 `PRODUCTION_VARIANCE` 凭证含 1416/1417 科目对；净差异为零时不影响既有 3 要素凭证。

- [x] `mvn test -pl module-manufacturing/erp-mfg-service` 全绿（含新增测试 + 既有 115 tests 无回归）

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0a3a82413ffelPJQxVHiZZpTSh) because 全部基线声明经实时仓库核实诚实、范围单结果表面清晰、反松弛无违规词、退出标准可测试无样板、技能使用逐项记录。非阻塞 minor M1（Phase 2 退出标准前向引用 Phase 3 测试→改为自包含 compile 检查）/ M2（既有测试数 102 stale→修正 115）/ M3（行号偏移→修正）均已修复。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成（6 类差异含 SUBCONTRACT + 过账链贯通）
- [x] 相关文档对齐（variance-analysis.md + costing-methods.md）
- [x] 已运行验证：`mvn test -pl module-manufacturing/erp-mfg-service` 全绿（118 tests，0 failures，含 TestErpMfgProductionVariance 10 tests 含 3 新增 SUBCONTRACT 用例）；结束审计重跑确认 BUILD SUCCESS
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 [ ] 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（暂无——本计划范围窄、为单一差异类型闭合。工作中心费率拆分 / 委外红冲 / 行级委外费归集均为已记录的独立 successor，非本计划范围。）

## Closure

Status Note: 6 层一致性闭合已落地并经独立结束审计验证——(1) `erp-mfg/variance-type` 字典补 SUBCONTRACT 码 + `_app.orm.xml` 镜像重生成；(2) `ErpMfgConstants.VARIANCE_TYPE_SUBCONTRACT` 常量 + `_ErpMfgDaoConstants` 镜像；(3) `ProductionVarianceCalculator` 第 6 类差异（标准侧/实际侧任一非零守卫）；(4) `ProductionVarianceDispatcher` 第 4 要素桶 + 全零守卫；(5) `ProductionVarianceAcctDocProvider` 1416/1417 科目对 + COA 种子；(6) `variance-analysis.md` + `costing-methods.md` 设计文档段。源 plan `2026-07-13-0455-2` Deferred「subcontract 委外差异」successor 标记为已收口。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文）— 通过 mission-driver closure-audit 流程触发
- Audit Method: 逐项对照实时仓库核实（非信任 `[x]` 标记）
- Code Verification:
  - `module-manufacturing/model/app-erp-manufacturing.orm.xml:154` 字典含 `<option code="SUBCONTRACT" label="委外费差异" value="SUBCONTRACT"/>`；镜像 `_app.orm.xml:135` 同步
  - `ErpMfgConstants.java:181` `VARIANCE_TYPE_SUBCONTRACT = "SUBCONTRACT"`；`_ErpMfgDaoConstants.java:399` 镜像
  - `ProductionVarianceCalculator.java:191-203` 第 6 类差异行（守卫 `stdSubcontractPerUnit.signum() != 0 || actSubcontract.signum() != 0`，对齐既有零差异范式）；Javadoc :59-66 已更新为「6 类差异含 SUBCONTRACT」
  - `ProductionVarianceDispatcher.java:95,99,103,134,141` SUBCONTRACT 桶聚合 + 全零守卫 + buildEvent 第 4 参数
  - `ProductionVarianceAcctDocProvider.java:60-61` `SUBJECT_SUBCONTRACT_VARIANCE = "1416"` / `SUBJECT_WIP_SUBCONTRACT = "1417"`；`:92-93` appendElementFacts 第 4 要素
  - `app-erp-all/.../_init-data/erp_md_subject.csv:23-24` COA 1416/1417 种子行
- Test Verification: `mvn test -pl module-manufacturing/erp-mfg-service` 重跑 → `Tests run: 118, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`；`TestErpMfgProductionVariance` 10 tests（含新增 `testSubcontractVarianceGeneratedWhenNonZero` / `testSubcontractLineOmittedWhenBothSidesZero` / `testSubcontractVariancePosting`，后者断言 PRODUCTION_VARIANCE 凭证 Dr 1416/Cr 1417 精确金额）
- Docs Verification: `variance-analysis.md:23,36,68,86` 含 SUBCONTRACT 行/枚举/公式/6 类描述；`costing-methods.md:76` successor 注解替换为「已收口，见 plan 2026-07-14-0035-1 实现注记」+ `:78-84` 实现注记段
- Log Verification: `docs/logs/2026/07-14.md` 含本计划聚合日志条目（6 层闭合 + 测试 + successor 收口）
- Anti-Hollow: SUBCONTRACT 差异行经完工触发 `ProductionVarianceDispatcher.dispatch` 实际消费 `ErpMfgCostVariance.costElement=SUBCONTRACT` 聚合，非死代码；`appendElementFacts` 第 4 调用实际生成 VoucherFact，经 `testSubcontractVariancePosting` 端到端验证凭证落库
- Text Consistency: Plan Status `completed` ↔ 3 Phase `completed` ↔ 全部 Exit Criteria `[x]` ↔ 全部 Closure Gates `[x]` ↔ 日志条目一致
- Deferred Honesty: Deferred But Adjudicated 段为空（本计划范围窄），所有 successor（工作中心费率拆分 / 委外红冲 / 行级归集）均为独立已记录 successor，无范围内缺陷降级

Follow-up:

- 无。
