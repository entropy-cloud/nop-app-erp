# 2026-07-24-0930-3-governed-path-cost-eval-arch-doc-alignment F1 治理路径成本评估 + 架构文档对齐

> Plan Status: completed
> Last Reviewed: 2026-07-24
> Source: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §闭包前必须项 #1（F1，P0）+ #2（F1，P0）+ #7（F3，P1）+ #8（F5，P1）
> Related: `docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md`（checker R2 精确基线 R2c=1108/R2b=319 为 F1 成本评估的定量输入）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（daoFor 965→1108 处 6 类分类先例）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-24）：

**F1 governed path 成本（Core Question 7）**：
- checker 实测基线（`2026-07-24-0930-1` 全仓运行）：R2c（生产 daoFor 总量）= **1108**，R2b（BizModel 跨域 daoFor）= **319**，R2a（BizModel daoFor ErpMd*）= **37**，R2d（Processor daoFor ErpMd*）= **34**
- `2026-07-16-2134-1` 计划已将 daoFor 分 6 类，真违规子集（Type 1 ORM 导航可替代 ~100-150 + Type 4 设计边界错误 ~10-30）≈ 110-180 处
- **governed path 真实成本有代码佐证**（治理审查 §F1）：`ErpCtInvoicePlanBizModel` Javadoc "硬注入跨域发票 BizModel 会将其完整服务依赖链级联进合同域，破坏其隔离单元测试"；`ErpApsAtpCtpServiceImpl` Javadoc "跨域 I*Biz 强注入会在 aps-service 单模块部署/测试时因依赖模块未组装而启动失败"
- **未实测**：`mvn test -pl module-contract`（单模块测试是否因 I*Biz 强注入而启动失败）——治理审查残留风险 #4 明确要求闭包前验证

**F1 #2 跨域写豁免登记缺口**：
- `docs/architecture/posting-exemptions.md` 已登记 2 处豁免：`MrpReleaseService`（mfg→pur）+ `ErpCtRebateSettlementBizModel`（contract→pur/sal）
- **`ErpB2bAsnBizModel` 未登记**（半治理：有 :59 javadoc 解释但无架构登记）——实测 `ErpB2bAsnBizModel.java:215` `daoFor(ErpPurReceive.class).newEntity()` + `:226` `saveEntity(receive)` 跨域写 b2b→pur，config-gated（`erp-b2b.asn-auto-create-receive=true`）。注：治理审查原报 `:212/:223`，经实仓核实正确行号为 `:215/:226`（计划采用实仓值）

**F3 ORM DAG 边登记缺口**：
- `data-dependency-matrix.md §5.6.2`（lines 506-528）跨业务域引用汇总表遗漏：drp→inv `ErpInvStockMove`（`module-drp/model/app-erp-drp.orm.xml:297,298` inboundMove/outboundMove 已落地但 §5.6.2 该行"跨业务域引用"列显示 "—"）；mfg→inv `ErpInvBatch` + mnt→ast `ErpAstAsset` 在多处文档记载（§2.2/§4.1/§6.5）但 §5.6.2 汇总表列遗漏

**F5 notify 子系统 owner doc 不完整**：
- notify 子系统**已有部分 owner doc**：`docs/design/notify/inbox-patterns.md`（129 行，inbox 页面设计）+ `docs/architecture/notification-strategy.md`（73 行，通知类型/频率/实现）。**缺口**：无 `docs/design/notify/README.md`（域级入口，对照其余 18 域均有）+ `module-boundaries.md §Owner Docs` 表（lines 82-103）无 notify 行（仅 18 业务域）
- 近 2 月 `module-notify/` 有 13 次 code commit，但域级 owner doc 入口缺失

## Goals

1. **F1 成本评估（P0）**：实测 I*Biz 强注入对单模块测试启动的影响（contract/aps 代表域），量化 governed path 真实成本；产出裁决（daoFor 真违规子集是否可安全重构 / 需平台层 lazy/SPI 解耦）
2. **F1 豁免登记（P0）**：将 `ErpB2bAsnBizModel` 跨域写豁免补登到 `posting-exemptions.md`（含理由/风险/补偿/收敛条件）
3. **F3 DAG 边登记（P1）**：补全 `data-dependency-matrix.md §5.6.2` 的 drp→inv/mfg→inv/mnt→ast 边
4. **F5 notify owner doc 入口补全（P1）**：创建 `docs/design/notify/README.md`（域级入口，索引既有 inbox-patterns.md + notification-strategy.md）+ 在 `module-boundaries.md §Owner Docs` 增 notify 行

## Non-Goals

- 执行 Type 1+4 的 ~110-180 处真违规 daoFor 重构——本计划仅**评估成本 + 产出裁决**，重构归裁决后的独立计划
- 修改 ORM 模型/`ext:dict` 引用/BizModel 业务逻辑——本计划为评估 + 文档对齐，零生产代码变更（除可能的单模块测试探针，探针不在最终 diff 中）
- `app-erp-common-api` 共享内核抽取（F4）——归独立 successor
- Nop 平台层 lazy/SPI 解耦机制实现——超出本项目范围（属 nop-entropy 协同），本计划仅识别需求

## Task Route

- Type: `architecture change`（F3/F5 文档 = 架构真相源对齐为主结果面；F1 成本评估为支撑 Decision，产出裁决文档纳入架构真相）
- Owner Docs: `docs/audits/2026-07-23-0000-architecture-governance-review.md`（F1/F3/F5）、`docs/architecture/posting-exemptions.md`、`docs/architecture/data-dependency-matrix.md`、`docs/architecture/module-boundaries.md`、`docs/design/notify/inbox-patterns.md`
- Skill Selection Basis: `nop-backend-dev`（I*Biz/daoFor 机制 + 单模块测试启动验证 + notify README 反映 BizModel 服务面）；`nop-debugging`（若成本评估发现启动失败需根因定位）
- **Bundling 裁决（rule 14）**：4 项均源自同一架构治理审查（`2026-07-23-0000`），共享 "architecture boundary governance" 结果面 + 同一 owner doc（审查报告）。Phase 2/3 的文档对齐（豁免登记/DAG 边/notify 入口）是治理审查 F1/F3/F5 的直接闭包项；Phase 1 成本评估的裁决（I*Biz 强注入是否破坏单模块测试）为后续 daoFor 重构 successor 提供前置条件，与文档对齐共享同一 owner doc 一致性义务。各 Phase 可并行（Prereqs 已声明），不构成硬串行依赖

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. F1 成本评估仅需 `mvn test -pl module-contract` / `module-aps`。

## Execution Plan

### Phase 1 — F1 governed path 成本评估（Explore + Decision）

Status: completed
Targets: `module-contract/erp-ct-service`（代表域，已注释说明级联依赖问题）+ `module-aps/erp-aps-service` + `docs/analysis/governed-path-cost-evaluation.md`（NEW 裁决文档）
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Proof`
- Prereqs: 无（checker 基线已由 `2026-07-24-0930-1` 产出，本计划引用不重跑）

- [x] Explore: 实测单模块测试启动——`mvn test -pl module-contract/erp-ct-service`（37 tests, 0 fail, BUILD SUCCESS）+ `mvn test -pl module-aps/erp-aps-service`（22 tests, 0 fail, BUILD SUCCESS）。两域均启动成功，无 I*Biz 强注入跨域依赖缺失异常
      - Skill: `nop-backend-dev`
- [x] Explore: 根因定位——测试通过因两域主动回避跨域 I*Biz 强注入（ct-service/aps-service 跨域 `@Inject I*Biz` 字段数=0），仅依赖跨域 DAO jar（contract→purchase-dao/sales-dao，aps→inventory-dao/manufacturing-dao），不依赖跨域 service jar。Javadoc 声称的级联依赖成本经依赖结构反事实验证为真（若注入 IErpPurInvoiceBiz 需级联 purchase-service→inventory-service，单模块 IoC 启动失败）
      - Skill: `nop-debugging`
- [x] Decision: 裁决=分支 (b)：I*Biz 强注入会破坏单模块测试启动。Type 1（~100-150 ORM 导航可替代，不需 I*Biz）可安全重构；Type 4（~10-30 跨域写/读聚合，需 I*Biz）受阻塞——需 nop-entropy 平台层 lazy/SPI 解耦或保留为登记豁免。`@Inject(required=false)` 临时缓解需平台验证（Nop IoC 当前无此属性）。替代方案 + 残留风险已记录
      - Skill: `nop-backend-dev`
- [x] Proof: 裁决记录写入独立成本评估文档 `docs/analysis/governed-path-cost-evaluation.md`（含实测证据：测试运行数 + 依赖结构 + 反事实推理 + 分类型前置条件）
      - Skill: `none`

Exit Criteria:

> Phase 1 产出可执行的 governed path 裁决（含实测证据），明确 daoFor 真违规子集重构的前置条件。

- [x] 单模块测试实测证据已记录（contract + aps 两域：启动成功/失败 + 异常 + 运行数）
- [x] governed path 裁决已记录（选择 + 替代方案 + 残留风险 + 重构前置条件）

### Phase 2 — F1 豁免登记 + F3 DAG 边登记（架构文档对齐）

Status: completed
Targets: `docs/architecture/posting-exemptions.md`（+b2b 豁免）、`docs/architecture/data-dependency-matrix.md`（§5.6.2 补边）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Item Types Note: Phase 2 is Add-heavy (doc registration)
- Prereqs: 无（与 Phase 1 可并行）

- [x] Add: `posting-exemptions.md` 增 `ErpB2bAsnBizModel`（b2b→pur）豁免条目——对齐既有 MrpReleaseService/ErpCtRebateSettlementBizModel 格式：位置（:215,226 头创建 + :266 行级回填）/config-gated（`erp-b2b.asn-auto-create-receive` 默认 false）/跨域写行为/理由（核心零污染 + config-gated ASN→Receive 自动创建）/风险/补偿机制/收敛条件（待采购域提供 `createFromAsn` I*Biz，前置条件需平台 lazy/SPI 解耦）
      - Skill: `nop-backend-dev`
- [x] Add: `data-dependency-matrix.md §5.6.2` 跨业务域引用汇总表补 3 边：drp→inv `ErpInvStockMove`（inboundMove/outboundMove，orm:297,298）+ mfg→inv `ErpInvBatch`（inputLot/outputLot，orm:1495,1497）+ mnt→ast `ErpAstAsset`（asset，orm:153）；均标注"批准保留"（DAG 单向合法 + module-boundaries.md 已登记依赖方向）；同步补全 §6.5 manufacturing/maintenance/drp 行与第二批域注释
      - Skill: `nop-backend-dev`
- [x] Proof: 文档变更经实时 ORM 核实（orm.xml 行号 + §2.2/§4.1/§6.5 交叉引用一致）；grep 验证 §5.6.2 含 3 边（ErpInvBatch:518 / ErpAstAsset:520 / ErpInvStockMove:526 命中）+ `grep ErpB2bAsn posting-exemptions.md` 命中 4 处
      - Skill: `none`

Exit Criteria:

> Phase 2 补全跨域写豁免登记 + DAG 边真相源表格。

- [x] `posting-exemptions.md` 含 ErpB2bAsnBizModel 条目（grep `ErpB2bAsn posting-exemptions.md` 命中）
- [x] `data-dependency-matrix.md §5.6.2` 含 3 边（grep `ErpInvStockMove\|ErpInvBatch\|ErpAstAsset` §5.6.2 命中）

### Phase 3 — F5 notify owner doc 入口补全 + module-boundaries 对齐

Status: completed
Targets: `docs/design/notify/README.md`（NEW 域级入口）、`docs/architecture/module-boundaries.md`（§Owner Docs +notify 行）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（与 Phase 2 可并行）

- [x] Add: 创建 `docs/design/notify/README.md`——域级入口，索引既有 `inbox-patterns.md` + 交叉引用 `docs/architecture/notification-strategy.md`；补全既有 owner doc 未覆盖的子系统结构（派发链 ErpSysNotificationBizModel→NotificationDispatcher→RecipientResolver/MergeCoordinator + 接收人解析策略表 + 与业务域消费者关系 6 域+workflow + 配置项 4 项 + lifecycle 状态机 + 失败降级语义）。经 `module-notify/` 实仓代码核实（ErpNotifyConfigs/Constants/Dispatcher/RecipientResolver/BizModel + ORM 字典 + 11 个跨域消费者调用点），对齐其余 18 域 README 结构。注：异步总线 nop-message（topic/partition/lease）经核实为 Deferred（notification-strategy.md 明确本期不接入），README 据实描述同步派发现有 + 标记 successor，未虚构未落地能力
      - Skill: `nop-backend-dev`
- [x] Add: `module-boundaries.md §Owner Docs` 表增 notify 行（标注跨域子系统定位 + owner doc 路径 `docs/design/notify/README.md`）
      - Skill: `none`
- [x] Proof: `ls docs/design/notify/README.md` 存在（9215 bytes）+ `grep notify module-boundaries.md` 返回 Owner Docs 行（line 104）
      - Skill: `none`

Exit Criteria:

> Phase 3 补齐 notify 子系统域级 owner doc 入口 + boundaries 表覆盖（不与既有 inbox-patterns.md/notification-strategy.md 重复）。

- [x] `docs/design/notify/README.md` 存在且索引既有 owner doc + 补全子系统结构
- [x] `module-boundaries.md §Owner Docs` 含 notify 行

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_070423529) because BLOCKER F5 基线事实错误（声称 "notify 子系统无 owner doc" 但 docs/design/notify/inbox-patterns.md 129 行 + docs/architecture/notification-strategy.md 73 行已存在；计划查错路径 notification-dispatch/ 并提议建冲突新目录）+ MAJOR scope 过度打包（4 finding × 2 task type 未给 rule 14 裁决）+ MAJOR Phase 1 "或" slack 证据位置 + 行号 :212/:223 静默修正未注
- Independent draft review iteration 2: accept (ses_0703b98eb) after 修正 F5 基线为 "缺 README.md + module-boundaries.md 行" + Phase 3 交付物改 docs/design/notify/README.md + 增 Bundling 裁决（rule 14）+ Phase 1 证据落单一位置 docs/analysis/governed-path-cost-evaluation.md + 行号差异注记；MINORS 非阻塞（Bundling 裁决的 runtime-gating 措辞与 Phase 2 "可并行" 轻微张力——已进一步软化为 "共享 owner doc 一致性义务，各 Phase 可并行"）

## Closure Gates

> 本计划为评估 + 文档对齐，零生产代码变更（F1 Phase 1 探针不在最终 diff 中）。完整仓库验证：`mvn clean install -DskipTests`（确认文档/探针零回归）。

- [x] 范围内行为完成（governed path 裁决 + b2b 豁免登记 + DAG 边补全 + notify owner doc）
- [x] 相关文档对齐（治理审查 F1/F3/F5 闭包项 #1/#2/#7/#8 verification checkpoint 达成）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS（Total time 01:35 min，154 reactor 模块全绿，确认零生产代码回归）
- [x] 无范围内项目降级为 deferred/follow-up（governed path 重构本就是裁决 Non-Goal，已明确归 successor）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 needs-revision → iteration 2 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Type 1+4 真违规 daoFor 重构（~110-180 处）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅评估 governed path 成本 + 产出裁决；重构需独立计划（依赖 Phase 1 裁决的前置条件——I*Biz 强注入是否破坏单模块测试）
- Successor Required: `yes`（触发条件：Phase 1 裁决为"可安全重构"时，开按域分批重构计划；若裁决为"需平台层解耦"则待 nop-entropy 协同）

### Nop 平台层 lazy/SPI 解耦机制

- Classification: `watch-only residual`
- Why Not Blocking Closure: 若 Phase 1 裁决 I*Biz 强注入破坏单模块测试，解耦需 nop-entropy 平台层支持（超出本项目范围）
- Successor Required: `yes`（触发条件：Phase 1 确认破坏 + 业务方要求封堵 daoFor 直访）

## Closure

Status Note: 全部 3 Phase 执行完成，零生产代码变更（纯文档对齐 + 评估）。验证 `mvn clean install -DskipTests` BUILD SUCCESS（154 模块全绿）。治理审查闭包项 #1/#2/#7/#8 全部达成，审计文件已同步标注解决状态。

执行证据：
- Phase 1：单模块测试实测 contract=37/aps=22 全绿 + 根因（零跨域 I*Biz + DAO-only 依赖）→ 裁决分支 (b)，落 `docs/analysis/governed-path-cost-evaluation.md`
- Phase 2：`posting-exemptions.md` 补登 ErpB2bAsnBizModel（grep 命中 4）+ `data-dependency-matrix.md §5.6.2` 补 3 边（ErpInvBatch:518/ErpAstAsset:520/ErpInvStockMove:526）
- Phase 3：`docs/design/notify/README.md` 创建（9215 bytes）+ `module-boundaries.md §Owner Docs` 增 notify 行（line 104）

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 ses_06f67b2b3ffeHgZuKMGSYmXqrF）—— **CLOSURE AUDIT: PASS**（11/11 检查通过）
  - Phase 1：cost-evaluation.md 含双域实测（ct=37/aps=22）+ 根因 + 分支 (b) 裁决 + Type 1/4 前置条件拆分 ✓
  - Phase 2：posting-exemptions.md ErpB2bAsnBizModel 条目（:43-61）完整 + §5.6.2 三边（:518/520/526）+ ORM 行号核实一致 ✓
  - Phase 3：notify/README.md（150 行，索引既有 + 子系统结构）+ module-boundaries.md:104 notify 行 ✓
  - 一致性：全 Phase [x] + Status:completed + Plan Status:completed + 审计闭包项 #1/#2/#7/#8 ✅ ✓
  - 反虚构：notify README 据实描述同步派发（NotificationDispatcher 同步 + nop-message Deferred），零虚构异步基础设施 ✓

Follow-up:

- Type 1（~100-150 ORM 导航可替代）daoFor 按域分批重构计划（Phase 1 裁决=可安全重构，无前置阻塞）
- Type 4（~10-30 跨域写/读）daoFor 重构——阻塞中，待 nop-entropy 平台层 lazy/SPI 解耦或保留为登记豁免
- `app-erp-common-api` 共享 SPI 内核（F4，独立 successor）
