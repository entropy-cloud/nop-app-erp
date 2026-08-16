# 2026-08-16-0904-1-rc-mr1-r1-49-mfg-bom-snapshot RC-R1.49 — mfg BOM 快照（MR1 越界项：ORM 结构变更 + 成本正确性）

> Plan Status: completed
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.49（P1-RC-009 mfg BOM 快照[UC-MFG-10 断言④⑤⑥]）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.49 行 + `docs/audits/arm-index.md` P1-RC-009 行 + 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 A 类：mfg RC-R1.49 ORM 变更批准——ErpMfgWorkOrder 加 snapshotBomVersion + 快照内容 或 新增 BOM 快照实体，对齐 Q3 纯加性类自动执行范围，越界回落双独立子 agent 批准**）
> Related: `docs/design/manufacturing/use-cases.md`（L1 UC-MFG-10 :176-187）；`docs/design/manufacturing/bom-and-routing.md`（§BOM 版本快照规则 :156-164）；`docs/design/manufacturing/state-machine.md`（:72/:228）；`docs/audits/2026-08-06-1926-rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime.md`（A4.2.6/7/8 运行时确认）；`docs/audits/2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md`（A1.10 审计报告）
> Audit: required

## Current Baseline

- **finding P1-RC-009（arm-index:146，UC-MFG-10）**：L1（`use-cases.md:180-185`）逐字断言④「工单审核时快照 BOM(工单行记录当时 BOM 内容)」/ 断言⑤「BOM 后续修改 → 不影响已审核工单的物料需求/成本」/ 断言⑥「新建工单才用新 BOM」。L3 实仓（HEAD 核查）：
  - `ErpMfgWorkOrder`（`app-erp-manufacturing.orm.xml:570-665`）：业务列 1-38（含 `bomId` propId 4 可空弱引用 :577）、系统列 39-44（delVersion..updateTime），**无 `snapshotBomVersion`/快照内容列**；**空闲 propId 45+**；
  - `ErpMfgWorkOrderLine`（orm.xml:668-697）：lineType(4, OUTPUT/INPUT/BYPRODUCT) 等 13 业务列 + 审计 14-19，**空闲 propId 20+**；
  - `ErpMfgBomLine.version`（propId 14, :250）= 乐观锁数据版本（domain=version），**非内容版本**；
  - 快照机制全仓 grep `snapshotBomVersion|bom-snapshot-strategy|LOCK_AT_CREATION|AUTO_UPGRADE`（module-manufacturing）= **0 命中**（A1.10 + MA4 双重证实）；无 `ErpMfg*BomSnapshot` 实体；
  - 快照复制动作在 submit/approve 路径**零存在**：`ErpMfgWorkOrderSubmitForApprovalProcessor:35-41` → `doSubmit:237-241`（仅置 SUBMITTED，无 BOM 复制）；`ErpMfgWorkOrderApproveProcessor:31-37` → `doApprove:248-256`（守卫 + APPROVED + createReservations:255，无 BOM 复制）。
- **读侧控制点（P1-RC-009 核心）**：`BomExpander.loadLines(bomId)`（`bom/BomExpander.java:144-149`）**实时查询 `ErpMfgBomLine` 无版本/快照门控**；消费方——`KitAvailabilityChecker.check:62-89`（resolveBomId:140-150 → explode:66，齐套二次检查，approve 路径接线 :255）、`CostRollupService.rollup:93-103`（自带 loadLines:170/325-330 实时 BomLine + loadOperations:332-337 实时 BomOperation）、`ProductionVarianceCalculator`（材料标准 = FIRMED `ErpMfgCostRollupLine` :114/129 已冻结不受 BOM 编辑影响；人工/工序标准 = 实时 `ErpMfgBomOperation` :123/140/147）；MRP/SimulationMrpEngine 计划期用默认 BOM（新建工单，与快照无关）。
- **owner doc 契约（bom-and-routing.md §BOM 版本快照规则 :156-164，实现契约）**：快照时机 = 工单 DRAFT→SUBMITTED 时复制 BOM 头+子件行+工艺行到工单快照字段（:160）；快照内容 = BOM 头（版本号、产出量）+ 子件行（物料、数量、工序）+ 工艺行（工序、工作中心、费率）（:161）；快照锁定 = 工单创建后即使 BOM 修改仍用快照（:162）；版本追溯 = `snapshotBomVersion` 字段（:163）；策略配置 = `erp-mfg.bom-snapshot-strategy`（LOCK_AT_CREATION / AUTO_UPGRADE 按物料可配）（:164）。§实现注记 :175 的「Non-Goal」标注经 A1.10 判定**非有效 documented simplification**（无人工批准痕迹，git log 全 AI commits）→ P1 义务成立。
- **审计证据**：A1.10（`2026-08-02-2231-1-...-a1-10-...md` §5.2/§4 :156）：断言④完全未实现（grep 零命中 + ORM 无快照列 + submit/approve 无复制动作）、断言⑤部分（bomId 弱隔离，同 bomId 内容编辑无保护）、断言⑥已实现；三判据 (i) plan 审计非人工批准 (ii) owner doc Non-Goal 无人工痕迹 (iii) product-scope 无排除 → **P1 维持，Q4=(a) 禁止方案 B**；修复方向（方案 A :218）= 新增快照实体/工单快照列 + 审核 Processor 复制 + 读侧切换 + config 接线。MA4（`2026-08-06-1926-rc-ma4-a4-2-6-7-8-...md` :206-208）确认 P1 不升 P0（材料标准已冻结、完工成本经领料单、config 默认关），MR1 修复方向同 A1.10。
- **预授权判据**：2026-08-12 批量裁决 A 类（roadmap:35/441）——mfg RC-R1.49 ORM 变更**批量批准**（ErpMfgWorkOrder 加 snapshotBomVersion + 快照内容 或 新增 BOM 快照实体）；越界回落双独立子 agent 批准（roadmap:13/29 + ai-autonomy-policy:79/83 2026-08-15 升级：ORM 保护区域 `auto + dual-agent-approval`，两个独立子 agent 批准记录落盘计划）。**须 双独立子 agent 批准 + 独立 plan-audit**。roadmap 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-manufacturing/model/app-erp-manufacturing.orm.xml`（ErpMfgWorkOrder/WorkOrderLine + 新快照实体）；`bom/BomExpander.java`（loadLines 快照门控或新增 snapshot-aware 入口）；`workorder/KitAvailabilityChecker.java`（resolveBomId/explode 读快照）；`costing/ProductionVarianceCalculator.java`（工序标准读快照）；`ErpMfgWorkOrderSubmitForApprovalProcessor.java`/`ErpMfgWorkOrderProcessor.java`（submit 复制动作）；`ErpMfgConstants.java`（新 config key）；新测试类；owner doc `bom-and-routing.md`/`state-machine.md` 注记 + arm-index/roadmap/logs 回填。
- **测试基线**：`TestErpMfgBomExplosion`（默认 BOM 选择/单级/多级/phantom/环/深度/GraphQL 接线，零快照覆盖）；`TestErpMfgProductionVariance`/`TestErpMfgCostRollup`/`TestErpMfgWorkOrderEndToEnd`/`TestErpMfgReservationLifecycle`（R1.48，approve 路径接线 createReservations 依赖 resolveBomId——快照切换后须回归）；erp-mfg-service 基线 270 tests（R1.48 closure 269 + MA4 A4.2.3 并发探针 +1，实证 arm-index P1-RC-008 行 A4.2.3 注记「mfg 270 tests 全绿」）。

## Goals

- **UC-MFG-10 断言④⑤⑥ 运行时成立（P1-RC-009 核心）**：工单 DRAFT→SUBMITTED 时快照 BOM（头+子件行+工艺行）到快照载体；已审核（SUBMITTED/APPROVED 及此后）工单的齐套检查/工序标准成本读快照而非实时 BOM；新建工单用最新 BOM——BOM 编辑不再影响已审核工单（断言⑤闭合），版本可追溯（`snapshotBomVersion`）。
- **快照载体**：按 Phase 1 D1 裁决——倾向新增快照实体族（`ErpMfgWorkOrderBomSnapshot` 头 + `ErpMfgWorkOrderBomLineSnapshot` 行 + `ErpMfgWorkOrderBomOperationSnapshot` 工艺行，纯加性实体，Q3 授权范围）或 ErpMfgWorkOrder 快照列 + WorkOrderLine 冻结列。
- **读侧切换语义**：齐套（KitAvailabilityChecker）与工序标准（ProductionVarianceCalculator 人工/工序维度）切换至快照；卷算（CostRollupService）为 BOM 级工具无工单上下文——保留实时读（快照仅约束工单实例，不约束 BOM 级成本卷算；材料标准已 FIRMED 冻结无需切换）——Phase 1 D2 裁决。
- **config 接线**：`erp-mfg.bom-snapshot-strategy`（LOCK_AT_CREATION 默认 / AUTO_UPGRADE 按物料可配，owner doc :164 两值均实现；Phase 1 D3 裁决 AUTO_UPGRADE 语义）。
- **测试**：① submit 后快照落库断言（内容 = 提交时点 BOM 头/行/工艺行 + snapshotBomVersion）；② BOM 编辑后已审核工单齐套/工序标准不受影响（断言⑤）；③ 新建工单用新 BOM（断言⑥）；④ AUTO_UPGRADE 语义；⑤ 幂等（重复 submit/resubmit 不重复快照）；⑥ 既有 mfg 测试零回归（含 R1.48 预留生命周期）。
- **零回归**：erp-mfg-service 全量测试（270 基线）全绿 + 全仓 `mvn test` + 全量构建 + compliance checker 零漂移（或 R2c 基线上调带 per-site 证据——新增实体 daoFor 面产生时按先例 R1.48 登记）。
- **owner doc 收敛**：bom-and-routing.md §BOM 版本快照规则 补实现注记（载体/时机/读侧切换/AUTO_UPGRADE 语义）；state-machine.md 同步；arm-index P1-RC-009 → done (RC-R1.49) + roadmap 行 done + logs 条目。

## Non-Goals

- **不实现 BOM 自身的版本历史管理**（ErpMfgBom 多版本并存/发布流程——快照机制只需在工单实例上锁定内容，BOM 头仍单版本模型）。
- **不改变 MRP/仿真计划期逻辑**（计划期始终用默认 BOM 展开，与工单快照无关）。
- **不实现 AUTO_UPGRADE 的逐物料可配粒度**（owner doc :164「按物料可配」——Phase 1 D3 若裁决为全局键语义则逐物料粒度登记 successor；不扩大 ORM 面）。
- **不改变材料标准成本来源**（FIRMED CostRollupLine 已冻结，零改动）。
- **不重写 CostRollupService 为快照感知**（BOM 级工具，保留实时读；D2 裁决）。
- **不改真相源契约段落**（use-cases L1 不动；bom-and-routing.md 契约段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：ORM 纯加性实体/列[A 类批量授权] + 成本计算读侧[双独立子 agent 批准]；Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/manufacturing/use-cases.md`（L1 UC-MFG-10）+ `docs/design/manufacturing/bom-and-routing.md`（§BOM 版本快照规则）
- Skill Selection Basis: ORM 模型变更 + 增量重生成（`nop-backend-dev`：orm.xml 模型优先 + per-mutation Processor + 平台辅助工具）；读侧切换 Java 逻辑（`nop-backend-dev`：BomExpander/KitAvailabilityChecker/ProductionVarianceCalculator 修改）；测试（`nop-testing`：JunitBaseTestCase 直断言 + `_cases/` 快照 + 零回归验证）。

## Infrastructure And Config Prereqs

- 新 config key：`erp-mfg.bom-snapshot-strategy`（默认 LOCK_AT_CREATION，register 于 ErpMfgConfigs/Constants，默认值常量 + yaml 零 override 普查）。
- ORM 变更触发增量重生成：`mvn clean install -DskipTests`（gen-orm.xgen 增量链，不重跑 nop-cli gen）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-manufacturing/erp-mfg-service`。

## Execution Plan

### Phase 1 - 载体/时机/策略裁决（Decision）

Status: completed
Targets: `app-erp-manufacturing.orm.xml`（ErpMfgWorkOrder/WorkOrderLine/新快照实体）；`ErpMfgConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **D1 快照载体：选项 A 选定** = 新增快照实体族 `ErpMfgWorkOrderBomSnapshot`（workOrderId/versionLabel/qty/productId + lines/operations to-many 级联）+ `ErpMfgWorkOrderBomLineSnapshot`（materialId/skuId/uoMId/quantity/operationId/scrapRate/warehouseId/alternativeMaterialId/lineNo 镜像 ErpMfgBomLine）+ `ErpMfgWorkOrderBomOperationSnapshot`（operationId/workcenterId/standardTime/timeUnit/rate/lineNo 镜像 ErpMfgBomOperation）+ `ErpMfgWorkOrder.snapshotBomVersion`（propId 45，VARCHAR 50，可空无默认无索引无 UK）+ `snapshotBomId`（propId 46，BIGINT 可空，快照实体归属锚）——owner doc :161 快照内容三件套完整承载，行/工艺行分离查询免 JSON 解析，2026-08-12 裁决「或 新增 BOM 快照实体」字面路径；**选项 B（否决候选）** = ErpMfgWorkOrderLine 冻结列（lineType=INPUT 行加快照内容列 + 工艺行无处安放需 ErpMfgWorkOrder 加 operations 快照 JSON 列——工艺行 JSON 化违背 orm.xml 显式列纪律 + 行冻结与正常 INPUT 行语义耦合）。**理由**：A 完整承载 owner doc 三件套 + 读侧一次 join 取齐 + 纯加性三实体不触既有语义。**残留风险（显式）**：三实体 + 二列扩 ORM 面（R2c 计数 daoFor 面变化——新实体读侧经 to-one join 或 IBiz，实施前核对 compliance 影响，若新增 daoFor 站点按 R1.48 先例基线上调带 per-site 证据）；快照为提交时点复制（提交后 BOM 继续编辑不影响已快照工单——断言⑤语义正确）。**D1 补充定稿（snapshotBomId 归属锚语义）**：`snapshotBomId` 记录快照来源 BOM 的 `ErpMfgBom.id`（追溯提交时点所用 BOM 头），快照实体 `ErpMfgWorkOrderBomSnapshot.bomId` 同值；读侧经 `snapshotBomId`（或 WorkOrderBomSnapshot.workOrderId）反查快照内容，无快照（DRAFT/未提交）回退实时 BOM。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D2 读侧切换范围：选项 A 选定** = 切换齐套（KitAvailabilityChecker 全路径：resolveBomId 后优先取快照行）+ 工序标准（ProductionVarianceCalculator 的 sumBomOperationStandardMins/deriveStandardLaborRate 改读快照工艺行，无快照回退实时 BomOperation）+ 齐套 approve 路径（R1.48 createReservations 经 explodeRequirements——自动继承 KitAvailabilityChecker 切换）；**CostRollupService 保留实时读**（BOM 级工具，无工单上下文；材料标准 FIRMED 冻结零改动）；**选项 B（否决）** = 卷算也快照化（需工单上下文注入，超 L1 断言面且卷算为成本维护工具非工单实例计算）。**理由**：L1 断言⑤约束「已审核工单的物料需求/成本」——物料需求=齐套展开、成本=差异计算工序标准；卷算是 BOM 级标准成本维护，非工单实例。**⚠ 与 MA4 §7（:208「二次齐套/卷算/差异读快照」字面）的偏离声明**：MA4 修复方向建议卷算亦读快照，但卷算（CostRollupService）无工单上下文（BOM 级工具），且卷算结果经 FIRMED 冻结后才被工单消费（材料标准 :114/:129 已冻结先行）——已审核工单的成本正确性由「快照齐套 + 快照工序标准 + FIRMED 材料标准」三件套保证，卷算实时读不破坏断言⑤；此偏离经本 D2 显式裁决（非疏漏），closure audit 按此核验。**残留风险**：完工成本（ErpMfgMaterialIssueConfirmProcessor 领料 Σ）不经 BOM 不受快照约束（MA4 已证，接受）；材料标准 FIRMED 冻结先行（已成立）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D3 AUTO_UPGRADE 语义：选项 A 选定** = config 两值全局键实现——LOCK_AT_CREATION（默认，提交时快照、读侧恒用快照）；AUTO_UPGRADE（读侧对已快照工单 re-resolve 默认 BOM 头（findDefaultBomOrNull）+ 实时展开，等价「自动升级到最新版本」且不写回快照）；**选项 B（否决）** = 逐物料可配粒度（owner doc :164「按物料可配」——需物料级配置载体，扩 ORM/配置面超授权）。**理由**：A 实现 owner doc 两值语义且零额外载体；逐物料粒度登记 successor（触发条件=运营要求物料级升级控制时立项）。**残留风险**：AUTO_UPGRADE 时断言⑤语义被显式配置覆盖（配置即选择，owner doc :164 设计意图）。
      - Skill: `nop-backend-dev`
- [x] `Proof` **调用面全集确认（实施前实仓 census，2026-08-16）**：BomExpander 消费方全清单——① `KitAvailabilityChecker.check:66`（explode 多级展开）+ `explodeRequirements:106`（R1.48 预留复用）；② `ErpMfgBomBizModel:52/:66/:75`（findDefaultBom/explode/flat 链）；③ `MrpEngine:124/:152`（计划期默认 BOM 单级展开，D2 裁决 MRP 计划期不改）；④ `SimulationMrpEngine:324/:352`（仿真计划期默认 BOM，同 MRP 不改）；⑤ `CostRollupService:152`（findDefaultBomOrNull）+ 自有 `loadLines:170/:325-330`/`loadOperations:332-337`（实时读，D2 裁决保留）。ProductionVarianceCalculator 三方法（`sumBomOperationStandardMins:369`/`deriveStandardLaborRate:397`/`resolvePrimaryWorkcenterId:424`）消费方 = 仅 `calculateVariances:123/:140/:147` 内部（外部入口 = 完工 willFinish config-gated 分支 + `IErpMfgCostVarianceBiz.calculateVariances` 手动重算）。submit 路径 = `ErpMfgWorkOrderSubmitForApprovalProcessor.submitForApproval`（Pattern B 1:1 复刻 → `ErpMfgWorkOrderProcessor.doSubmit:237-241`）；reject→resubmit 交互（reject 仅置 approveStatus=REJECTED，docStatus 保持 SUBMITTED；submit 守卫 approveStatus∈{UNSUBMITTED,REJECTED} + docStatus=DRAFT）——**D4 内联裁决：幂等 = snapshotBomVersion 已存在则跳过复制（不重复快照，reject→修改→resubmit 场景不重快照，保持提交时点内容锁定）**。config key 零 override 普查：全 20 生产 application.yaml + job.yaml 无 `erp-mfg.bom-snapshot-strategy` 任何 override（grep 零命中）。R1.48 createReservations 接线点确认（`ErpMfgWorkOrderProcessor:500-569`：resolveBomId :506 → explodeRequirements :518 → aggregateRequirements :519——approve 路径自动继承 KitAvailabilityChecker 快照化切换）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] D1-D3 裁决记录落盘（选择 + 备选 + 理由 + 残留风险），调用面全集清单（读侧 4 消费方 + submit 路径 + R1.48 接线）产出
- [x] ORM 快照实体族 + 2 列 propId 分配（WorkOrder 45/46 空闲实证——业务 1-38 + 系统 39-44，orm.xml:574-618 逐列核对）与 Q3 纯加性范围核对（无 NOT NULL 无默认值无索引无 UK——D1 定稿列定义全可空无默认无 UK）

### Phase 2 - 双独立子 agent 批准（ORM 变更前置硬门）

Status: completed
Targets: `app-erp-manufacturing.orm.xml`（快照实体族 + 2 列设计定稿）
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1 D1-D3 裁决完成

- [x] `Proof` **双独立子 agent 批准（ORM 结构变更[快照实体族 + 2 列] + 成本计算读侧[工序标准]变更门控，硬门，批准落盘前不得进入 Phase 3 ORM 实施）**：两个独立子代理（fresh session，无执行者上下文）分别检查批准（批准记录落盘本计划，对齐 2026-08-12 A 类裁决「越界回落双独立子 agent 批准」+ roadmap:13/29 + ai-autonomy-policy:79「两个批准均通过后才可实施」）。**批准记录（2026-08-16 实盘）**：
      - **批准 agent #1**（`ses_ff7cde424ffeMWzqBEhU89gxRq`，fresh session 独立审查）→ **APPROVE**：A 纯加性 PASS（propId 1-44 逐列核对连续无重复，45/46 空闲实证；新列可空无默认对照 bomId/sourceScheduleId 弱引用范式；三新实体零触碰既有列；Phase 3 外键 IDX 属新表 DDL 非既有表索引改造，符合授权）+ B DRAFT 零回归 PASS（快照列无默认 → DRAFT 恒 null 天然回退；KitAvailabilityChecker/BomExpander 快照入口复用既有算法；ProductionVarianceCalculator 三方法仅内部消费）+ C Q4 收敛 PASS（owner doc :160-164 逐条落地，断言④⑤⑥ 全覆盖）+ D 审批流 PASS（Pattern B 1:1 复刻链不破坏，protected step 接线可行；D4 幂等有锁定语义支撑）+ E 残留风险 PASS（CostRollupService 签名无工单上下文 + FIRMED 冻结链实证 MA4 偏离成立）。
      - **批准 agent #2**（`ses_ff7cdbfb6ffeVu4BBb6JQl2mSF`，fresh session 独立复核）→ **APPROVE**：A 调用面 PASS（全仓扫描无遗漏消费方；异实体 loadLines 命中排除；工单实例读 BOM 三路径全覆盖——⚠ 注明 explodeRequirements 快照化须显式接线非字面自动，closure audit 以测试⑦+②交叉验证）+ B 幂等时序 PASS（doSubmit 唯一复制点 + 全重复提交场景自洽）+ C config PASS（全局键两值 + 逐物料粒度 successor 显式登记 + 零 override 实证）+ D 测试 PASS（7 组覆盖 ④⑤⑥ + 零回归义务）+ E compliance PASS（R2c 漂移风险三处显式登记，R1.48 先例核实）。
      - 三批准前置条件（纯加性 / DRAFT 零回归 / Q4 收敛）双 agent 均满足 → **两个批准均通过，Phase 3 ORM 实施解锁**。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 双独立子 agent 批准记录落盘（批准人 2 个独立子代理 + 结论），批准覆盖 ORM 快照实体族 + 2 列 + 读侧切换范围

### Phase 3 - ORM 快照载体 + 增量重生成（A 类批量授权变更）

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 D1 裁决（快照实体族选定）+ **Phase 2 双独立子 agent 批准落盘**

- [x] `Add` ORM 按 D1 裁决落地：`ErpMfgWorkOrderBomSnapshot`/`ErpMfgWorkOrderBomLineSnapshot`/`ErpMfgWorkOrderBomOperationSnapshot` 三实体（纯加性，tagSet=gid,erp.manufacturing + useLogicalDelete 对齐既有实体范式，propId 从 1 连续）+ `ErpMfgWorkOrder` 加 `snapshotBomVersion`（propId 45）/`snapshotBomId`（propId 46）+ to-many `bomSnapshot`（cascade-delete,insertable,updatable 对齐 lines 范式）+ 索引仅加快照实体外键 IDX（对齐既有 IDX 范式，不加 UK）。**实施记录**：orm.xml 三实体落于 ErpMfgWorkOrderLine 之后（:721 起）；WorkOrder 新列 45/46 插于 sourceScheduleId(:612) 与 delVersion 之间（对齐 RC-R1.43 finance 先例：业务列在前系统列在后）；三新实体全业务列可空无默认（Q3 纯加性，DDL 实证 NULL 无默认），系统列对齐既有范式；外键 IDX 每表对齐源实体索引面（头 workOrderId/bomId/productId，行 snapshotId/materialId/skuId/uoMId/operationId/warehouseId/alternativeMaterialId，工艺 snapshotId/operationId/workcenterId）；to-one 关系镜像源实体（line→material/sku/uoM/operation/warehouse/alternativeMaterial，op→operation/workcenter）。
      - Skill: `nop-backend-dev`
- [x] `Proof` 增量重生成验证：`mvn clean install -DskipTests`（EXIT 0）→ 生成产物核对：三实体 Entity 类（`erp-mfg-dao/.../entity/ErpMfgWorkOrderBom{Snapshot,LineSnapshot,OperationSnapshot}.java`）+ `_gen/_ErpMfgWorkOrder.java` getter/setter 实证（getSnapshotBomVersion :2388 / setSnapshotBomId :2415 / getSnapshotBomId :2407）+ xmeta（`_ErpMfgWorkOrder.xmeta` + i18n zh-CN/en 同步 `snapshotBomVersion`）+ DDL 三方言（`deploy/sql/{mysql,oracle,postgresql}/_create_erp-mfg.sql:575` CREATE TABLE erp_mfg_work_order_bom_snapshot + mysql :474-475 `SNAPSHOT_BOM_VERSION VARCHAR(50) NULL`/`SNAPSHOT_BOM_ID BIGINT NULL` 纯加性实证）+ `_app.orm.xml` 6 引用同步 + 三 BizModel 生成（service/entity/）+ API 模块 Input/Output Bean + Api 接口生成；propId 分配核对（45/46 无冲突，三实体 propId 1 连续）+ 编译通过。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 三实体 + 2 列落地（orm.xml/Entity/xmeta/DDL 四同步 grep 核对），`mvn clean install -DskipTests` 生成链通过，分域编译通过

### Phase 4 - 快照复制 + 读侧切换

Status: completed
Targets: `ErpMfgWorkOrderSubmitForApprovalProcessor.java`/`ErpMfgWorkOrderProcessor.java`；`bom/BomExpander.java`；`workorder/KitAvailabilityChecker.java`；`costing/ProductionVarianceCalculator.java`；`ErpMfgConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 3 完成（Phase 2 批准已落盘）

- [x] `Add` submit 快照复制：`ErpMfgWorkOrderProcessor.doSubmit`（:304-309 现序）接线 protected step `snapshotBomOnSubmit`（DRAFT→SUBMITTED 同事务复制 BOM 头[versionLabel/qty/productId/bomId] + `ErpMfgBomLine` 行 + `ErpMfgBomOperation` 工艺行到快照实体 + 写 `snapshotBomVersion`/`snapshotBomId`；无 BOM → LOG.warn 不阻断；幂等 = snapshotBomVersion/snapshotBomId 已存在跳过；来源 BOM 解析 `resolveSnapshotSourceBom` = `wo.getBom()` to-one 优先 + `findDefaultBomOrNull` 回落；行/工艺行经 `bomExpander.loadLines/loadOperations`（public 化）+ 快照头 `saveEntity` 后取 `snap.getId()` 显式 FK 落子行（对齐 CostRollupService.createHead→writeLines 先例）。Pattern B 复刻链（`ErpMfgWorkOrderSubmitForApprovalProcessor:35-41`）不破坏——快照步骤在 facade helper `doSubmit` 内，下游可 Delta 覆盖。
      - Skill: `nop-backend-dev`
- [x] `Fix` 读侧切换（D2 选项 A）：`KitAvailabilityChecker` 增 `hasBomSnapshot(wo)` + `explodeRequirements(wo, qty)` 快照感知入口（有快照 → `loadBomSnapshot` 经 `wo.getBomSnapshot()` to-many 取头 → `bomExpander.explodeFromSnapshot` 展开；无快照 → resolveBomId + 实时 explode 零回归；AUTO_UPGRADE → re-resolve 默认 BOM 实时展开，无默认 BOM 防御回退快照）；`check()` 与 approve 预留路径（`createReservations`）复用该入口；`ProductionVarianceCalculator` 三方法（`sumBomOperationStandardMins`/`deriveStandardLaborRate`/`resolvePrimaryWorkcenterId`）经 `snapshotOperationsOrNull` 快照工艺行优先（无快照/AUTO_UPGRADE → null 回退实时；**快照存在但工艺行为空 → 空列表 = 锁定空不回落实时**）；`BomExpander` 增 `explodeFromSnapshot`（复用 expandLines DFS/phantom/环/深度算法，首级取快照行，子级制造/phantom BOM 实时递归——快照仅锁定工单自身 BOM，Non-Goal 边界）。
      - Skill: `nop-backend-dev`
- [x] `Add` config 接线（D3 选项 A）：`ErpMfgConstants` 增 `CONFIG_BOM_SNAPSHOT_STRATEGY`/`BOM_SNAPSHOT_STRATEGY_LOCK_AT_CREATION`/`BOM_SNAPSHOT_STRATEGY_AUTO_UPGRADE`/`DEFAULT_BOM_SNAPSHOT_STRATEGY`（=LOCK_AT_CREATION）+ `ErpMfgConfigs.getBomSnapshotStrategy`/`isBomSnapshotAutoUpgrade`（空值=默认 LOCK_AT_CREATION）；读侧（KitAvailabilityChecker + ProductionVarianceCalculator）统一经 `ErpMfgConfigs` 判断；全仓 yaml 零 override 普查实证（Phase 1 Proof）。
      - Skill: `nop-backend-dev`
- [x] `Fix` 工艺标准计算数据源统一：ProductionVarianceCalculator 快照工艺行加载经 **ORM to-many 关系 getter**（`wo.getBomSnapshot()` → `snap.getOperations()`）零新增 daoFor 站点（compliance R2c 仅快照复制写路径 +3 + 费率解析 +1 + loadOperations +1 = +5 按 R1.48 先例基线上调登记，per-site 证据落 compliance-baseline.md）；工作中心费率为主数据实时读（与实时路径同口径同工具调用——快照冻结 BOM 工艺行内容，费率属主数据语义在 owner doc 注记声明）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] submit 快照复制 + 读侧切换 + config 接线落地（grep 证据：snapshotBomOnSubmit/explodeFromSnapshot/explodeRequirements(wo,)/snapshotOperationsOrNull/CONFIG_BOM_SNAPSHOT_STRATEGY 全落盘）
- [x] 未审核工单（无快照）行为零变化（既有测试路径回归证明：TestErpMfgReservationLifecycle/WorkOrderEndToEnd/ProductionVariance/BomExplosion 全绿）+ DRAFT 工单仍走实时 BOM（无快照回退分支 + testNoBomSubmitSkipsSnapshot/testNewWorkOrderUsesCurrentBomOnSubmit 实证）

### Phase 5 - 测试 + 文档回填 + 零回归验证

Status: completed
Targets: 新增 `TestErpMfgBomSnapshot`；`bom-and-routing.md`；`state-machine.md`；arm-index/roadmap/`docs/logs/`
Skill: `nop-testing` + `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 4 完成

- [x] `Add` 新增 `TestErpMfgBomSnapshot`（**9 组**，超出计划 7 组——②⑤ 增需求侧直断言拆分）：① `testSnapshotCapturedOnSubmit` submit 后快照落库（头[workOrderId/bomId/productId/versionLabel/qty] + 行[materialId/quantity/lineNo/uoMId] + 工艺行[workcenterId/standardTime] + snapshotBomVersion）；② `testBomLineEditAfterSubmitKitUnchanged` BOM 行编辑后已审核工单齐套展开不变（断言⑤物料需求面：check 全齐 + 需求侧 `explodeRequirements(wo)` = 快照 2×2=4 vs 实时 5×2=10 对照；预留关闭隔离口径）；③ `testBomOperationEditAfterSubmitVarianceUnchanged` 工序标准成本编辑后已审核工单 variance 不变（断言⑤成本面：标准工时 60×2=120 非实时 600×2=1200 + FIRMED rollup 冻结）；④ `testNewWorkOrderUsesCurrentBomOnSubmit` 新建工单用当前 BOM（断言⑥：WO1 快照 2 → 编辑 → WO2 快照 5）；⑤ `testAutoUpgradeReResolvesLatestBom` AUTO_UPGRADE 配置下 re-resolve 最新 BOM（实时展开 10>7 部分齐套 + 不写回快照）；⑥ `testIdempotentSubmitNoDuplicateSnapshot`（状态复位后重复 submit 幂等跳过）+ `testNoBomSubmitSkipsSnapshot`（无 BOM 提交不阻断零快照）；⑦ `testApproveReservationFromSnapshot`（R1.48 预留经快照化展开：提交后编辑 BOM 行，approve 预留量 = min(快照 4, 可用 10) = 4 非实时 10）+ `testSnapshotEntitiesReachableViaGraphQL`（快照头/行 findPage 可达，对齐 TestErpMfgBomExplosion 接线范式）。
      - Skill: `nop-testing`
- [x] `Add` owner doc 注记：`bom-and-routing.md §BOM 版本快照规则` 补实现注记（载体/时机/读侧切换范围/AUTO_UPGRADE 语义/卷算保留实时读声明/存量数据回退姿势）+ §实现注记 Non-Goal 清单移除「BOM 版本快照」（已实现）+ `state-machine.md §4` BOM 变更异常路径行同步；`use-cases.md` 不动。
      - Skill: `nop-backend-dev`
- [x] `Proof` 零回归验证：`mvn test -pl module-manufacturing/erp-mfg-service` **279 tests 全绿**（270 基线[arm-index P1-RC-008 行 A4.2.3 注记实证，较 plan Baseline 段 269 口径为后续 A4.2.3 探针 +1] + 9 新增零回归）+ 既有 4 测试类 `_cases` 快照重录（新 seq 面 NEXT_VALUE 漂移 + 响应新列机械重录：TestErpMfgWorkOrderStateMachine/CompletionPosting/WorkOrderEndToEnd/CostFlowEndToEnd，RECORDING→CHECKING 核验）+ 全仓 `mvn test`（见 Closure Gates）+ `mvn clean install -DskipTests` 全量构建 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（**R2c 1408→1413 基线上调登记**，5 新站点 per-site 证据落 compliance-baseline.md 注记：snapshotBomOnSubmit 3 处快照实体 newEntity/save + BomExpander.loadOperations 1 处 + ProductionVarianceCalculator 快照分支工作中心费率 1 处；读侧快照访问全经 ORM to-many 关系 getter 零新增 daoFor）+ 回填（arm-index P1-RC-009 → done (RC-R1.49) + roadmap RC-R1.49 行 done ✅ + `docs/logs/2026/08-16.md` 日志条目）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试全绿（9 组 ≥ 计划 7 组）+ erp-mfg-service 既有测试零回归（279 全绿）+ 全量构建通过 + compliance checker actual ≤ baseline（R2c 基线上调带 per-site 证据）
- [x] owner doc 注记（bom-and-routing.md + state-machine.md）+ 三处回填（arm-index/roadmap/log）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_ff7e1ae7effe1bVC51GmydeaRa`）——1 MAJOR + 4 MINOR。MAJOR-1 已修正：**双独立子 agent 批准门控位置错误**——原 Phase 2 先行实施 ORM（受保护 `model/*.orm.xml` 变更）后 Phase 3 才批准，违反 ai-autonomy-policy:79「两个批准均通过后才可实施」——重构为独立 Phase 2「双独立子 agent 批准（ORM 变更前置硬门）」置于 ORM 实施（现 Phase 3）之前，Phase 3 Prereqs 显式引用批准落盘。4 MINOR 已修正：(1) 测试基线「269/270」歧义统一为 269（R1.48 closure 证据）；(2) D2 补「与 MA4 §7 :208 卷算字面偏离」显式声明（卷算保留实时读 = 有意裁决非疏漏，closure audit 按此核验）；(3) 引用修正 roadmap:34/441 → :35/441；(4) 测试组数「6+」→「7 组」+ GraphQL 冒烟项并入 ⑦。
- Independent draft review iteration 2: `needs revision`（独立子代理 `ses_ff7d90147ffemnO93vJbeLpAve`）——0 MAJOR + 2 文本级 MINOR。已修正：(1) Goals 零回归残留「269/270」→「269 基线」（与 Baseline/Phase 5 统一）；(2) Phase 1 D1 残留风险陈旧「Phase 3 前核对 compliance」→「实施前核对 compliance R2c 影响」（对齐 Phase 4 同型措辞）。
- Independent draft review iteration 3: `accept`（独立子代理 `ses_ff7d2d8abffeX4EUhTHz7MsDdI`）——两项修正逐项验证（「269/270」「Phase 3 前核对」零残留）+ 全链一致性复核（Phase 1→5 顺序/prereqs 链/批准硬门前置/项目类型齐全/D1-D3 决策完整/269 计数三处一致/无重复覆盖计划）→ **共识达成，计划可转 active**。

## Closure Gates

- [x] 范围内行为完成（P1-RC-009：快照载体 + submit 复制 + 读侧切换 + config + 测试）
- [x] 相关文档对齐（bom-and-routing.md 注记 + state-machine.md + arm-index P1-RC-009 → done (RC-R1.49) + roadmap 行 done）
- [x] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl module-manufacturing/erp-mfg-service` 279 全绿 + 全仓 `mvn test` BUILD SUCCESS 3457/0/0 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline[R2c=1413≤1413]）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（Draft Review Record 3 轮收敛 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（独立审计 `ses_ff7753e24ffe0NqWzdLgEZEK3Q` 执行，首轮 FAIL 五项文本/卫生级修复 + 复审见 Closure 段）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### AUTO_UPGRADE 逐物料可配粒度

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc :164「按物料可配」粒度需物料级配置载体（扩 ORM/配置面超 A 类授权）；全局键两值语义已实现 L1 断言面
- Successor Required: `yes`（触发条件：运营要求物料级 BOM 升级控制时，按 ORM ask-first 流程立项）

### BOM 头多版本并存/发布流程

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 快照机制只需在工单实例锁定内容；BOM 头单版本模型不违反 UC-MFG-10 断言（断言⑥新建工单用新 BOM 已由默认 BOM 选择满足）
- Successor Required: `no`

## Closure

Status Note: 五 Phase 全部完成并勾选，验证全绿（erp-mfg-service 279 tests + 全仓 3457 tests + 全量构建 + compliance actual ≤ baseline[R2c 1413]，R2c 基线上调带 per-site 证据）。独立结束审计执行：首轮 `FAIL`（`ses_ff7753e24ffe0NqWzdLgEZEK3Q`）——五项文本/卫生级修复已全部落地（①Closure Gates 勾选 + Closure 段 draft 状态注记修正；②arm-index/roadmap R2c「1408→1412」数值漂移 → 1413 修正；③`DebugSnapshotProbe/` 调试探针目录删除；④AUTO_UPGRADE 无默认 BOM 防御回退语义与计划字面对齐——`KitAvailabilityChecker.explodeRequirements` 无默认 BOM 时回退快照展开 + test⑤ 增回退断言；⑤log 条目非顶部为并发时序所致非实质缺口）；**复审 PASS**（同会话 `ses_ff7753e24ffe0NqWzdLgEZEK3Q` 续用，五项逐项复核实证 + 实测 9/9 全绿 + checker 零漂移，剩余缺口无）→ **Plan Status: completed**。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_ff7753e24ffe0NqWzdLgEZEK3Q`（fresh session，无执行者上下文；首轮 FAIL → 执行者修复 → 同会话复审 PASS）
- Evidence: 审计逐项核验：A 文本一致性（首轮 Closure Gates 未勾选 + Status Note 残留 → 已修复，复审 PASS）/ B ORM 纯加性 PASS（orm.xml 三实体 + 2 列 + DDL 三方言实证 + 生成产物同步）/ C 代码 PASS（snapshotBomOnSubmit/explodeFromSnapshot/explodeRequirements(wo,)/snapshotOperationsOrNull/config 接线 + @Inject 非 private 自检；AUTO_UPGRADE 回退语义复审 PASS）/ D 测试 PASS（TestErpMfgBomSnapshot 9 组实测全绿，复审再实测 9/9）/ E 零回归 PASS（mfg 279 tests 实测 + _cases 重录限 4 类机械差异 + checker actual 1413 ≤ baseline 1413）/ F 回填 PASS（arm-index/roadmap/log/bom-and-routing.md/state-machine.md 五处，use-cases.md 零变更；R2c 数值复审 PASS）/ G 范围 PASS（Deferred But Adjudicated 两项显式登记，无静默降级）

Follow-up:

- （无）
