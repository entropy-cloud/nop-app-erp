# 2026-07-12-1400-1-processor-private-to-protected-for-delta-customization Processor private 方法全面开放为 protected（Delta 纯文件定制就绪）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/analysis/productization-readiness-analysis.md` §6.1「需要改为 protected 的方法清单」+ §7.2 最终结论
> Related: 无
> Audit: required

## Current Baseline

`docs/analysis/productization-readiness-analysis.md` 审计确认：nop-app-erp 的 Delta 纯文件定制可行性约 85%。所有四层 Delta 入口（view.xml / xbiz / beans.xml / orm.xml）的 `x:extends` 链均已就绪，全部 41 个 Processor 类均为 `public class`（非 `final`），全部 `@Inject` 字段为包级可见（非 `private`），全部默认构造器可用，xbiz `inject('bean-id')` 全部通过 bean ID 解析（支持 Delta 覆盖）。

**唯一阻点**：24 个 Processor 类中散布的 **57 个 `private` 方法**阻止下游通过 Delta 派生类覆盖单一步骤。其中影响最大的是：

| Processor | private 方法数 | 最关键阻塞 |
|-----------|:-------------:|-----------|
| `ErpFinAccountingPeriodProcessor` | 10 + 1 内部类 | 期末结账全部预检查询锁死（findUnpostedVoucherCodes / findUnsettledArApCodes / reverseCloseVoucher 等） |
| `ErpPrjProjectSettlementProcessor` | 9 | 结算实体加载/持久化/查询全部锁死（requireSettlement / save / findBillings / findCostCollections 等） |
| `ErpFinPostingProcessor` | 5 + 1 内部构造器 | 过账失败记录/日志/计时埋点锁死（recordPostFailure / logFailure / timeStage 等） |
| `ErpFinBudgetScenarioProcessor` | 3 | 预算场景加载/保存锁死 |
| `ErpFinEmployeeAdvanceProcessor` | 3 | 状态标记锁死（markPosted / clearPosted） |
| `ErpAstAssetCapitalizationProcessor` | 3 | 状态标记锁死（markPosted / clearPosted） |
| `ErpAstMergeProcessor` | 2 | 资产汇总计算锁死 |
| `ErpAstSplitProcessor` | 2 | 凭证行 Map 构造锁死 |
| `ErpAstInventoryProcessor` | 2 | |
| `ErpAstCipProcessor` | 2 | |
| `ErpFinExpenseClaimProcessor` | 2 | |
| `ErpInvStockMoveProcessor` | 2 | 库存移动编号生成锁死（newMoveCode） |
| `ErpInvOwnershipTransferProcessor` | 2 | DAO 访问器锁死（transferDao） |
| `ErpCrmConversionProcessor` | 0 + 1 内部接口 | Setter 函数式接口不可访问 |
| 其余 10 个 Processor | 各 1 | 多为 `nz(BigDecimal)` 空安全工具方法 |
| **合计** | **57 方法 + 2 内部类型 + 1 构造器** | |

**验证依据**：每个 Processor 类的 Javadoc 均已声明产品化意图——*"各步骤为 protected 方法、单一职责、以 IServiceContext 为末参。客户/行业覆盖单步实现时，写派生 Processor 重载目标 protected 方法，在 Delta beans.xml 以同名 bean id 注册覆盖基线。"* 但实际代码中散布的 `private` 方法与此声明矛盾。

**修复性质**：纯可见性拓宽（`private` → `protected`），零行为变更、零 API 变更、零 ORM 模型变更。是所有改动中风险最低的一类。

## Goals

- 全部 24 个 Processor 文件中的全部 57 个 `private` 方法改为 `protected`（或 `protected static`），使下游 Delta 派生类可覆盖任意步骤。
- 2 个 `private` 内部类型（`TbAgg` 类 + `Setter` 接口）改为 `protected`，使下游可访问和扩展。
- 修复后 nop-app-erp 可宣称"纯 Delta 全定制就绪"（分析报告 §7.2 覆盖度从 85% 提升至 ~98%）。

## Non-Goals

- **`PostingRun` 的 private 构造器不改**——`ErpFinPostingProcessor.PostingRun` 是 `protected static final class`，其 `private` 构造器强制使用 `forPost()` / `forReverse()` 工厂方法，是有意的设计约束，不属于 Delta 阻塞。
- **障碍 B（Processor 使用 `daoProvider.daoFor()` 绕过 CrudBizModel 管道）不在本计划范围**——ORM 拦截器提供了纯 Delta 替代方案（见分析报告 §5.1 第 15 行），不构成绝对阻塞。归后续 successor（触发条件：产品要求 Processor 写操作必须经过 BizModel 权限/审计管道时）。
- **障碍 C（9 域无 Processor）不在本计划范围**——这些域的 `@BizMutation` 方法均为 public，xbiz 可覆盖，不构成 Delta 阻塞。归后续 successor（触发条件：这些域需要步骤级细粒度定制时）。
- **不创建 `_delta/` 目录、不配置 `nop.core.vfs.delta-dir-ids`**——这是下游集成方的项目搭建职责，不属于产品基线。
- **不创建 Delta 派生类演示或测试**——本计划仅开放可见性，不新增功能。Delta 派生类验证归分析报告落地后的端到端 successor。

## Task Route

- Type: `implementation-only change`（纯可见性拓宽，不改 API/模型/认证/集成/部署）
- Owner Docs: `docs/analysis/productization-readiness-analysis.md` §6.1（权威修复清单）
- Skill Selection Basis: 纯 Java 可见性拓宽，不涉及 ORM 模型/xbiz/view/codegen → `Skill: none`（自检纪律参照 `nop-backend-dev` 但无需加载技能，因为不写新方法、不改方法签名、不加注解）

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。纯 Java 源码可见性变更，无端口/环境变量/密钥/外部服务/数据迁移依赖。

## Execution Plan

### Phase 1 - Finance + Projects Processor private 方法开放（9 文件 / 35 方法 + 1 内部类型）

Status: completed
Targets: `ErpFinPostingProcessor` / `ErpFinAccountingPeriodProcessor` / `ErpFinBudgetScenarioProcessor` / `ErpFinEmployeeAdvanceProcessor` / `ErpFinExpenseClaimProcessor` / `ErpFinNotesReceivableProcessor` / `ErpFinNotesPayableProcessor` / `ErpFinBadDebtProcessor` / `ErpPrjProjectSettlementProcessor`
Skill: none

- Item Types: `Fix`（修复产品化设计声明与实际代码的不一致——Javadoc 声称 "protected step 方法"，实际有 private）
- Prereqs: 无

- [x] `Fix`: `ErpFinPostingProcessor`（`module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java`）——5 个 `private` 方法改为 `protected`：
  - L258 `private <T> T timeStage(...)` → `protected`
  - L271 `private void timeStageVoid(...)` → `protected`
  - L283 `private void logFailure(...)` → `protected`
  - L295 `private void recordPostFailure(...)` → `protected`
  - L321 `private void recordReverseFailure(...)` → `protected`
  - 不改 `PostingRun` 的 private 构造器（Non-Goal，有意工厂约束）
- [x] `Fix`: `ErpFinAccountingPeriodProcessor`（`module-finance/erp-fin-service/.../processor/ErpFinAccountingPeriodProcessor.java`）——10 个 `private` 方法改为 `protected` + 1 个内部类改为 `protected`：
  - L269 `resolveDefaultOrgId()` / L419 `moduleStatusOf(...)` / L436 `setModuleStatus(...)` / L518 `findPostedVoucherIds(...)` / L529 `resolveAcctSchemaId(Long)` / L565 `findUnpostedVoucherCodes(...)` / L575 `findUnsettledArApCodes(...)` / L589 `findUnresolvedPostingExceptionKeys(...)` / L606 `reverseCloseVoucher(...)` / L659 `resolveAcctSchemaId(ErpFinAccountingPeriod)` → 全部 `protected`
  - L549 `private static final class TbAgg` → `protected static final class TbAgg`
- [x] `Fix`: `ErpFinBudgetScenarioProcessor`——3 个 `private` 方法改为 `protected`：`requireScenario` / `save` / `join`
- [x] `Fix`: `ErpFinEmployeeAdvanceProcessor`——3 个方法：`markPosted` / `clearPosted` → `protected`；`nz` → `protected static`
- [x] `Fix`: `ErpFinExpenseClaimProcessor`——2 个方法：`nz` → `protected static`；`isBlank` → `protected static`
- [x] `Fix`: `ErpFinNotesReceivableProcessor`——1 个方法：`nz` → `protected static`
- [x] `Fix`: `ErpFinNotesPayableProcessor`——1 个方法：`nz` → `protected static`
- [x] `Fix`: `ErpFinBadDebtProcessor`——1 个方法：`nz` → `protected static`
- [x] `Fix`: `ErpPrjProjectSettlementProcessor`——9 个 `private` 方法改为 `protected`：`requireSettlement` / `save` / `findBillings` / `findCostCollections` / `loadProject` / `resolveUserId` / `illegalTransition` / `nz`（改为 `protected`，去掉 `static`，因为是实例方法）/ `parseAmount`

Exit Criteria:

- [x] 9 个 finance + projects Processor 文件中全部 35 个 `private` 方法 + `TbAgg` 内部类改为 `protected`，零 `private` 方法残留（`PostingRun` private 构造器除外，属 Non-Goal）
- [x] 涉及模块 `mvn compile -DskipTests -pl module-finance/erp-fin-service,module-projects/erp-prj-service -am` 编译通过

### Phase 2 - Assets + Inventory + Purchase + Sales + CRM Processor private 方法开放（15 文件 / 22 方法 + 1 内部类型）

Status: completed
Targets: 9 个 Assets Processor / 3 个 Inventory Processor / 1 个 Purchase Processor / 1 个 Sales Processor / 1 个 CRM Processor
Skill: none

- Item Types: `Fix`
- Prereqs: Phase 1 已验证范式

- [x] `Fix`: `ErpAstAssetCapitalizationProcessor`——3 方法：`markPosted` / `clearPosted` → `protected`；`nz` → `protected static`
- [x] `Fix`: `ErpAstMergeProcessor`——2 方法：`nz` → `protected static`；`sum` → `protected static`
- [x] `Fix`: `ErpAstInventoryProcessor`——2 方法：`nz` → `protected static`；`nzInt` → `protected static`
- [x] `Fix`: `ErpAstSplitProcessor`——2 方法：`nz` → `protected static`；`lineMap` → `protected static`
- [x] `Fix`: `ErpAstCipProcessor`——2 方法：`nz(BigDecimal, BigDecimal)` → `protected static`；`nz(BigDecimal)` → `protected static`
- [x] `Fix`: `ErpAstValueAdjustmentProcessor`——1 方法：`nz` → `protected static`
- [x] `Fix`: `ErpAstDepreciationScheduleProcessor`——1 方法：`nz` → `protected static`
- [x] `Fix`: `ErpAstDisposalProcessor`——1 方法：`nz` → `protected static`
- [x] `Fix`: `ErpAstMaintenanceProcessor`——1 方法：`nz` → `protected static`
- [x] `Fix`: `ErpInvStockMoveProcessor`——2 方法：`nz` → `protected static`；`newMoveCode` → `protected static`
- [x] `Fix`: `ErpInvOwnershipTransferProcessor`——2 方法：`transferDao` → `protected`；`nz` → `protected static`
- [x] `Fix`: `ErpInvLandedCostProcessor`——1 方法：`nz` → `protected static`
- [x] `Fix`: `ErpPurReceiveProcessor`——1 方法：`addLineQuantities` → `protected`
- [x] `Fix`: `ErpSalDeliveryProcessor`——1 方法：`addLineQuantities` → `protected`
- [x] `Fix`: `ErpCrmConversionProcessor`——内部接口 `Setter` 从 `private interface` → `protected interface`
- [x] `Proof`: 全量扫描确认 24 个 Processor 文件中零 `private` 方法残留（`PostingRun` private 构造器除外），经 `rg 'private\s+(static\s+)?(final\s+)?[\w<>\[\],\s]+\s+\w+\s*\(' --type java -g '*Processor*.java' module-*/` 验证

Exit Criteria:

- [x] 15 个 assets + inventory + purchase + sales + CRM Processor 文件中全部 22 个 `private` 方法 + `Setter` 内部接口改为 `protected`，零残留
- [x] 涉及模块编译通过

## Draft Review Record

- Independent draft review iteration 1: accept after fixes (主代理草案审查，2026-07-12). 格式、范围、Non-Goals、item 类型标注、Deferred 触发条件均合规；item 级清单（方法名 + 行号）已对照实时仓库 grep 验证，57 方法 + 2 内部类型全部命中，无遗漏、无虚报。发现 Major 文本一致性问题：阶段标题与 Exit Criteria 中的汇总计数（文件数 / 方法数 / 内部类型数）陈旧——系早期 Phase 1 仅含 finance 时计算、加入 Projects 后未同步更新。已直接修正：Phase 1（8→9 文件，25→35 方法，2→1 内部类型）、Phase 2（16→15 文件，32→22 方法）、Phase 2 targets（4→3 Inventory Processor，因 `ErpInvCostAdjustProcessor` 无 private 方法）。修正后阶段汇总与 item 清单一致，Closure Gates 引用的总计（24 文件 / 57 方法 / 2 内部类型）本就正确无需改动。无 Blocker。

## Closure Gates

- [x] 范围内行为完成：全部 24 个 Processor 文件的 57 个 `private` 方法 + 2 个内部类型改为 `protected`
- [x] 相关文档对齐：`docs/analysis/productization-readiness-analysis.md` §6.1 修复清单全部标记为已修复
- [x] 已运行验证：`mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS + `mvn test`（finance + projects + assets + inventory 代表域现有测试全绿，无回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 障碍 B：Processor 使用 daoProvider.daoFor() 绕过 CrudBizModel 管道

- Classification: `optimization candidate`
- Why Not Blocking Closure: ORM 拦截器（`IOrmInterceptor` + `orm-interceptor.xml`）提供纯 Delta 替代方案，在 ORM 层面拦截所有实体写操作。不构成纯 Delta 绝对阻塞，只增加了定制复杂度（无法按实体精确控制）。
- Successor Required: `yes`（触发条件：产品要求 Processor 写操作必须经过 BizModel 权限/审计管道，或下游反馈 ORM 拦截器粒度过粗时）

### 障碍 C：9 域无 Processor 模式

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 这些域的 `@BizMutation` 方法均为 `public`，xbiz 可覆盖，不构成 Delta 阻塞。只是缺少步骤级粒度。
- Successor Required: `yes`（触发条件：这些域（b2b / contract / logistics / drp / maintenance / cs / hr / master-data / notify）出现步骤级定制需求时）

## Closure

Status Note: 全部 24 个 Processor 文件中的 57 个 `private` 方法 + 2 个内部类型已改为 `protected`（Phase 1：9 文件 / 35 方法 + `TbAgg`；Phase 2：15 文件 / 22 方法 + `Setter`）。唯一保留的 `private` 是 `ErpFinPostingProcessor.PostingRun` 构造器（Non-Goal，有意工厂方法约束）。验证：`mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS；`mvn test` finance+projects+assets+inventory+purchase+sales+crm 代表域全绿无回归。文档对齐：`docs/analysis/productization-readiness-analysis.md` §6.1 已标注"已修复"。待独立子代理执行结束审计。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0a85925a1ffeEjFMJ2snSVOw1y（新会话，read-only 闭包审计，2026-07-13）
- Verdict: PASS（7/7 门控全绿，无 Open Issues）
- Evidence:
  - Gate 1（范围完整性）：24 Processor 文件全量 `rg` 扫描零 `private` 方法/内部类型残留；`git diff --stat` 确认 24 文件 60/60 对称插入/删除，与 57 方法 + 2 内部类型一致。
  - Gate 2（Non-Goal 保留）：`ErpFinPostingProcessor.java:431` 保留 `private PostingRun(...)` 构造器，工厂方法 `forPost()`/`forReverse()` 完好。
  - Gate 3（无范围外编辑）：`ErpMfgWorkOrderProcessor` / `ErpSalOrderProcessor` / `ErpInvCostAdjustProcessor` 不在 diff 中。
  - Gate 4（纯可见性变更）：抽查 3 文件 diff 确认仅 `private`→`protected`（含 static/interface/class 变体），无方法体/签名/注解/import 变更，60/60 对称比例佐证纯 token 替换。
  - Gate 5（构建完整性）：`mvn clean install -DskipTests` → BUILD SUCCESS，155 模块。
  - Gate 6（文档对齐）：`productization-readiness-analysis.md:257` §6.1 "已修复"横幅就位。
  - Gate 7（计划文件一致性）：Plan Status completed；Phase 1+2 全部 [x] + Status completed；Exit Criteria 全 [x]。

Follow-up:

- 障碍 B successor：Processor 写操作回归 CrudBizModel 管道（触发条件见 Deferred）
- 障碍 C successor：为 9 个无 Processor 域创建 Processor（触发条件见 Deferred）
- Delta 端到端验证 successor：创建演示 Delta 派生 Processor + Delta beans.xml，验证纯 Delta 定制全链路（触发条件：产品需要提供 Delta 定制参考实现时）
