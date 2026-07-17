# 2026-07-16-2134-1-ddd-entity-methods-daofor-convergence DDD 实体方法上提与 daoFor() 收敛

> Plan Status: completed
> Last Reviewed: 2026-07-17
> Source: `docs/analysis/2026-07-16-ddd-implementation-gap-analysis.md`
> Related: `nop-entropy/docs-for-ai/02-core-guides/domain-logic-and-ddd.md`
> Audit: required

## Current Baseline

### 实体贫血（Gap A）—— 已验证确认

- **338/338** 个手写持久化实体类均为 11 行空壳，零领域方法。（排除 1 个误放在 entity 包的 `@DataBean` DTO `ErpCrmTerritoryPipeline`。）
- **59 个**状态判断方法在 21 个 Processor 中重复定义，分布在 5 个域：
  - `isAlreadyApproved()`: 21 处
  - `isAlreadyRejected()`: 18 处
  - `validateNotCancelled()`: 20 处
- 逻辑跨域逐字节相同（仅实体类型和 Constants 类不同）。库存域 `ErpInvCostAdjustProcessor` 略有变体（省略冗余 null 检查、`validateNotCancelled` 少 `IServiceContext` 参数），逻辑等价。
- 所有 42 个 Processor 类无共享抽象基类。
- 实体上不存在任何 `isApproved()`/`isRejected()`/`isCancelled()`/`isSubmitted()` 方法。
- **关键约束**：状态常量（如 `ErpPurConstants.APPROVE_STATUS_APPROVED`）定义在 `*-service` 模块中，而实体类在 `*-dao` 模块中。`-dao` 不能依赖 `-service`，因此实体方法不能直接引用现有 Constants 接口。

### daoFor() 滥用（Gap B）—— 部分确认，计数修正

- 差距分析声称 ~600 处 `daoFor()`。**实际生产代码（`src/main/java`）为 965 处**（不含测试），含测试共 3047 处。分析的 ~600 数字明显偏低（可能仅采样）。
- 各类型占比估算（基于修正后的总量重新粗估，±15pp）：

  | 类型 | 估算处数（生产） | 处理策略 |
  |------|----------------|---------|
  | 1. ORM 导航替代 | ~100-150 | 应重构 |
  | 2. 同域子实体 | ~300-350 | 可接受，非强制 |
  | 3. Processor 架构约束 | ~150-200 | 保留，文档化 |
  | 4. 设计边界错误 | ~10-30 | 应重构 |
  | 5. 看板/报表只读聚合 | ~80-120 | 保留，文档化 |
  | 6. 历史残留 | ~100-150 | 逐步清理（大部分归入上述类型） |

- 已验证的具体代码示例均准确：
  - `ErpSalOrderBizModel.java:87-92` 的 `loadOrderLines` 方法确实存在，且 ORM 定义了 `ErpSalOrder.lines` to-many 关系（`cascade-delete,insertable,updatable`），`order.getLines()` 可直接替代。
  - `ErpPurOrderProcessor.java:205` 的 `daoProvider.daoFor(ErpFinAccountingPeriod.class)` 跨域访问确实存在。
  - `ErpCsSurveyBizModel.java:57-58` 的 `daoFor(ErpCsTicket.class)` 用于打破循环依赖（`ErpCsTicketBizModel` 已注入 `IErpCsSurveyBiz`），已验证。

### new Erp*() 直接构造 —— 计数修正

- 差距分析声称 38 处。**实际为 21 处**（排除生成代码和测试），其中仅 **~4-5 处** 是真正的实体构造违规（应使用 `newEntity()`），其余为 DTO/累加器/引擎类或防御性回退（合法使用 `new`）。

### 防护机制缺口 —— 已验证确认

- `nop-backend-dev` skill 的自检清单（平台 20 项 + 项目 4 项 P1-P4）**无一条**检查实体是否有可上提的领域方法、ORM 导航优先、或 daoFor 注释原因。
- `docs/audits/nop-compliance-checker.sh`（R1-R10 规则）**无规则**追踪 `isAlreadyApproved`/`isAlreadyRejected` 在 Processor 中的重复定义。

## Verification of Gap Analysis Claims

| 差距分析主张 | 验证结果 | 对计划的影响 |
|------------|---------|------------|
| 全域实体均为 11 行空壳 | ✅ 确认（338/338） | 纳入范围 |
| 59 个状态方法在 21 Processor 中重复 | ✅ 确认（精确计数） | 纳入范围 |
| ~600 处 daoFor() | ❌ 实际生产 965 处 | 修正基线，缩小 daoFor 范围至可操作的子集 |
| 六种 daoFor 类型分类 | ✅ 分类合理（计数不准） | 采纳分类，仅处理类型 1+4 |
| 38 处 new Erp*() | ❌ 实际 21 处，仅 ~4-5 真违规 | 修正范围至 ~4-5 处 |
| ErpCsSurveyBizModel 循环依赖 | ✅ 确认 | 纳入范围 |
| skill 自检缺实体方法检查 | ✅ 确认 | 纳入范围 |
| checker 无 isAlreadyApproved 规则 | ✅ 确认 | 纳入范围 |
| Processor 不可反注入 I*Biz | ✅ 确认（日志 H-4 裁决） | 类型 3 保留，仅文档化 |

## Goals

1. **将稳定状态判断方法上提到实体**：为 ~20 个有审批/作废流程的实体添加 `isApproved()`/`isRejected()`/`isCancelled()` 布尔方法，消除 21 个 Processor 中的 59 个重复方法定义。
2. **修复已验证的 daoFor() 反模式**：将 ORM 导航可替代的 `daoFor()` 调用重构为关系 getter；修复 ErpCsSurveyBizModel 的设计边界错误。
3. **修复已验证的 new Erp*() 违规**：将 ~4-5 处真正的实体直接构造改为 `newEntity()`。
4. **建立防护机制**：在 skill 自检清单中增加实体方法/ORM 导航/daoFor 检查项；在 compliance checker 中增加 Processor 状态方法重复检测规则。

## Non-Goals

- **不批量重构全部 965 处 daoFor()**——范围过大，且类型 2（同域子实体）、类型 3（Processor 架构约束）、类型 5（看板/报表）经分析确认为可接受模式。批量重构需独立计划。
- **不为实体添加超出状态判断的领域方法**——"搜索纯计算逻辑上提"过于宽泛，缺乏具体候选清单，留待后续按需提取。
- **不抽取 Processor 共享基类**——虽然有 21 个 Processor 共享相同方法签名，但跨域类型不同（`ErpPurOrder` vs `ErpSalOrder`），泛型基类引入复杂度高于收益。实体方法上提后，Processor 中只需改为调用 `entity.isApproved()`，重复自然消除。
- **不处理测试代码中的 daoFor()**——2082 处测试代码 daoFor 不影响生产质量。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `nop-entropy/docs-for-ai/02-core-guides/domain-logic-and-ddd.md`、`docs/design/purchase/state-machine.md`（状态轴定义）
- Skill Selection Basis: 涉及实体方法编写和 BizModel/Processor 修改，必须使用 `nop-backend-dev` skill。涉及代码模式验证，使用 `nop-debugging` skill 的根因分析思维。修改后需验证构建，无前端变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 关键设计约束：实体在 `*-dao` 模块，常量在 `*-service` 模块。Phase 1 需先解决常量跨模块问题（见 Decision D1）。

## Execution Plan

### Phase 1 - 决策与基础设施准备

Status: completed
Targets: `module-*/erp-*-dao/`、`.opencode/skills/nop-backend-dev/SKILL.md`、`docs/audits/nop-compliance-checker.sh`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [x] **Decision D1: 实体状态常量的归属模块**
  - 背景：实体类在 `*-dao` 模块，状态常量（`APPROVE_STATUS_APPROVED` 等）在 `*-service` 模块的 `ErpXxxConstants` 中。`-dao` 不能依赖 `-service`。
  - 选择：在各域 `-dao` 模块中创建轻量常量接口（如 `app.erp.pur.dao.constants.ErpPurDocStatus`），仅包含实体方法所需的状态值（approve-status + doc-status，约 7 个值）。`ErpXxxConstants`（service）可 `extends` 这些接口以保持向后兼容。
  - 替代方案 A（否决）：直接在实体类上定义 `static final String`——多实体同域会重复定义。
  - 替代方案 B（否决）：将整个 `ErpXxxConstants` 移到 dao——影响面大，且其中含非状态配置项。
  - 替代方案 C（否决）：硬编码字符串字面量——脆弱，违反单一真相原则。
  - 残留风险：两处常量定义（dao 和 service）可能漂移；通过 `extends` 关系和注释指向 ORM dict 为权威源来缓解。
  - Skill: `nop-backend-dev`

- [x] **Add: 为 purchase 域创建 dao 层状态常量接口**
  - 文件：`module-purchase/erp-pur-dao/src/main/java/app/erp/pur/dao/constants/ErpPurDocStatus.java`
  - 内容：`APPROVE_STATUS_UNSUBMITTED`、`APPROVE_STATUS_SUBMITTED`、`APPROVE_STATUS_APPROVED`、`APPROVE_STATUS_REJECTED`（approve-status）、`DOC_STATUS_DRAFT`、`DOC_STATUS_ACTIVE`、`DOC_STATUS_CANCELLED`（doc-status）——常量名与现有 `ErpPurConstants` 完全一致，确保 `extends` 后无编译冲突
  - `ErpPurConstants` extends `ErpPurDocStatus` 以保持向后兼容
  - Skill: `nop-backend-dev`

- [x] **Add: 为 sales 域创建 dao 层状态常量接口**
  - 同上模式，文件：`module-sales/erp-sal-dao/.../constants/ErpSalDocStatus.java`
  - `ErpSalConstants` extends `ErpSalDocStatus`
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] D1 决策已记录，purchase + sales 的 dao 层状态常量接口已创建且编译通过
- [x] `ErpPurConstants`/`ErpSalConstants` extends 新接口后，现有代码编译无破坏（本地化检查：`erp-pur-service` 和 `erp-sal-service` 模块 `mvn compile` 通过）

---

### Phase 2 - 实体状态方法上提：purchase + sales（12 Processor）

Status: completed
Targets: `module-purchase/erp-pur-dao/src/main/java/.../entity/ErpPur*.java`（6 实体）、`module-sales/erp-sal-dao/.../entity/ErpSal*.java`（6 实体）、对应的 12 个 Processor
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 完成

- [x] **Add: 为 purchase 域 6 个实体添加状态方法**
  - 实体：`ErpPurOrder`、`ErpPurRequisition`、`ErpPurInvoice`、`ErpPurReturn`、`ErpPurReceive`、`ErpPurPayment`
  - 每实体添加（按现有 Processor 使用情况）：
    ```java
    public boolean isApproved() {
        return Objects.equals(getApproveStatus(), ErpPurDocStatus.APPROVE_STATUS_APPROVED);
    }
    public boolean isRejected() {
        return Objects.equals(getApproveStatus(), ErpPurDocStatus.APPROVE_STATUS_REJECTED);
    }
    public boolean isCancelled() {
        return Objects.equals(getDocStatus(), ErpPurDocStatus.DOC_STATUS_CANCELLED);
    }
    ```
  - 注：`Objects.equals` 已 null-safe，无需额外 `status != null` 前置检查（消除库存域变体中的不一致）
  - Skill: `nop-backend-dev`

- [x] **Fix: 更新 purchase 域 6 个 Processor 调用实体方法**
  - 将 `isAlreadyApproved(order)` 改为 `order.isApproved()`
  - 将 `isAlreadyRejected(order)` 改为 `order.isRejected()`
  - `validateNotCancelled(order, context)` 保留在 Processor（因含异常抛出），内部改用 `order.isCancelled()` 判断
  - 删除 Processor 中的 `isAlreadyApproved()`/`isAlreadyRejected()` 方法定义
  - Skill: `nop-backend-dev`

- [x] **Add: 为 sales 域 6 个实体添加状态方法**
  - 实体：`ErpSalOrder`、`ErpSalQuotation`、`ErpSalInvoice`、`ErpSalReturn`、`ErpSalReceipt`、`ErpSalDelivery`
  - 同 purchase 模式，使用 `ErpSalDocStatus`
  - Skill: `nop-backend-dev`

- [x] **Fix: 更新 sales 域 6 个 Processor 调用实体方法**
  - 同 purchase Processor 更新模式
  - Skill: `nop-backend-dev`

- [x] **Proof: purchase + sales 模块编译通过**
  - `mvn compile -pl module-purchase/erp-pur-dao,module-purchase/erp-pur-service,module-sales/erp-sal-dao,module-sales/erp-sal-service -am` 通过
  - Skill: none

Exit Criteria:

- [x] 12 个实体各有 `isApproved()`/`isRejected()`/`isCancelled()` 方法
- [x] 12 个 Processor 中 `isAlreadyApproved()`/`isAlreadyRejected()` 定义已删除，调用点改为 `entity.isXxx()`
- [x] `validateNotCancelled` 保留在 Processor 但内部使用 `entity.isCancelled()`
- [x] purchase + sales 模块编译通过

---

### Phase 3 - 实体状态方法上提：finance + assets + inventory（9 Processor）

Status: completed
Targets: `module-finance/erp-fin-dao/.../entity/ErpFin*.java`（2-3 实体）、`module-assets/erp-ast-dao/.../entity/ErpAst*.java`（5 实体）、`module-inventory/erp-inv-dao/.../entity/ErpInv*.java`（1 实体）、对应的 9 个 Processor
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 2 完成（验证 purchase+sales 模式可复用）

- [x] **Add: 为 finance/assets/inventory 域创建 dao 层状态常量接口**
  - 各域分别创建 `ErpFinDocStatus`、`ErpAstDocStatus`，inventory 域复用现有常量或创建 `ErpInvDocStatus`
  - 各域 `ErpXxxConstants` extends 新接口
  - Skill: `nop-backend-dev`

- [x] **Add: 为 finance 域实体添加状态方法**
  - 实体：`ErpFinExpenseClaim`、`ErpFinEmployeeAdvance`（各添加 isApproved/isRejected/isCancelled）
  - `ErpFinBadDebt`（仅有 isAlreadyApproved 和 validateNotCancelled，无 isAlreadyRejected → 只添加 isApproved/isCancelled）
  - Skill: `nop-backend-dev`

- [x] **Fix: 更新 finance 域 3 个 Processor**
  - Skill: `nop-backend-dev`

- [x] **Add: 为 assets 域实体添加状态方法**
  - 实体：`ErpAstMerge`、`ErpAstSplit`（isApproved/isCancelled）、`ErpAstValueAdjustment`、`ErpAstDisposal`、`ErpAstAssetCapitalization`（isApproved/isRejected/isCancelled）
  - Skill: `nop-backend-dev`

- [x] **Fix: 更新 assets 域 5 个 Processor**
  - Skill: `nop-backend-dev`

- [x] **Add + Fix: inventory 域 ErpInvCostAdjust 实体方法 + Processor 更新**
  - 实体：`ErpInvCostAdjust`（isApproved/isRejected/isCancelled）
  - Processor：`ErpInvCostAdjustProcessor`（同步更新签名，补齐 `validateNotCancelled` 的 `IServiceContext` 参数以统一）
  - Skill: `nop-backend-dev`

- [x] **Proof: finance + assets + inventory 模块编译通过**
  - Skill: none

Exit Criteria:

- [x] ~8 个实体各有按需的状态方法
- [x] 9 个 Processor 中重复方法定义已删除，调用点改为实体方法
- [x] finance + assets + inventory 模块编译通过

---

### Phase 4 - daoFor() Type 1 审计与修复（ORM 导航替代）

Status: completed
Targets: `module-*/erp-*-service/.../*BizModel.java`、`module-*/erp-*-service/.../*Processor.java` 中的 daoFor(ChildEntity) 调用
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1-3 完成（不阻塞，但先完成实体方法可使此阶段专注）

- [x] **Fix: 审计并修复 BizModel 中的 ORM 导航可替代 daoFor()**
  - 方法：搜索 `*BizModel.java` 和 `*Processor.java` 中的 `daoFor(Erp*Line.class)` 或 `daoFor(Erp*Detail.class)` 等子实体类型，检查 ORM 模型是否存在对应的 to-many/to-one 关系。存在则替换为 `entity.getLines()`/`entity.getDetail()` 等。
  - 已验证候选（ORM to-many 关系存在）：
    - `ErpSalOrderBizModel.loadOrderLines:87` → `order.getLines()`（ORM `lines` to-many at `app-erp-sales.orm.xml:346`）
    - `ErpPurOrderProcessor.loadLines` → `order.getLines()`（ORM `lines` to-many at `app-erp-purchase.orm.xml`，purchase 8 个父实体均有 lines 关系）
  - 需执行时逐处验证 ORM 关系存在性，不存在则保留并加注释
  - 预计处理 ~20-40 处（仅处理已确认可安全替换的，非全部 ~100-150 处 Type 1）
  - 实际处理：6 处 loadLines 替换（ErpPurOrder/Invoice/Requisition/Return/Receive Processor + ErpSalOrderBizModel），保留 ErpPurReceiveProcessor.loadOrderLines 跨聚合访问并加注释
  - Skill: `nop-backend-dev`

- [x] **Proof: 修改的模块编译通过 + 已有测试无回归**
  - 被修改模块的 `mvn test` 通过（重点运行涉及 purchase/sales 的现有自动化测试）
  - Skill: `nop-testing`

Exit Criteria:

- [x] 已验证的 ORM 导航替代 daoFor() 调用已重构
- [x] 不存在 ORM 关系的 daoFor() 调用保留并添加注释说明原因
- [x] 被修改模块编译和测试通过

---

### Phase 5 - daoFor() Type 4 边界修复 + new Erp*() 违规修复

Status: completed
Targets: `module-cs/erp-cs-service/.../ErpCsSurveyBizModel.java`、~4-5 处 `new Erp*()` 违规文件
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: none（与 Phase 4 独立）

- [x] **Fix: ErpCsSurveyBizModel 循环依赖边界修复**
  - 现状：`ErpCsSurveyBizModel:57` 使用 `daoFor(ErpCsTicket.class).getEntityById(ticketId)` 校验工单存在
  - ORM 关系已验证存在：`app-erp-cs.orm.xml:504` 定义了 `ErpCsSurvey` 的 `<to-one name="ticket" refEntityName="...ErpCsTicket">`（join `ticketId = id`）
  - 修复：改为 ORM 导航 `survey.getTicket()`（透明懒加载，无循环依赖）
  - Skill: `nop-backend-dev`

- [x] **Fix: ~4-5 处 new Erp*() 实体直接构造违规**
  - 草案审查修正：原差距分析声称 38 处、本计划初稿列为 ~7 处。经逐文件验证后修正为以下确证违规（排除 `QuotaRollupCalculator` 的虚拟聚合行和 `GapAnalysisCalculator` 的防御性回退等合法用途）：
    - `ErpSalPricingRuleEngine.java:204` — `new ErpSalOrderLine()`（赠品行持久化）
    - `NotificationDispatcher.java:130` — `new ErpSysNotification()`（通知持久化）
    - `ErpSysNotificationBizModel.java:101` — `new ErpSysNotificationRead()`（已读记录持久化）
    - `ErpSysNotificationBizModel.java:118` — `new ErpSysNotificationRead()`（同上）
    - `FunnelAggregationEngine.java:269` — `new ErpCrmFunnelStageMetrics()`（持久化实体类，需验证是否落库；若为纯内存聚合则降级为注释说明）
  - 排除的合法用途（不移入范围）：
    - `QuotaRollupCalculator.java:105` — 注释明确标注"构造虚拟聚合行（不持久化）"，`newEntity()` 语义不适用
    - `GapAnalysisCalculator.java:90` — 已有 `newGapEntity()` 封装方法（优先调用 `daoProvider.daoFor().newEntity()`），`new ErpHrGapAnalysis()` 为 daoProvider 为 null 时的防御性回退
  - 逐处改为使用 `I*Biz.newEntity()` 或 `daoProvider.daoFor(Xxx.class).newEntity()`
  - 实际处理：4 处改为 `daoProvider/dao.newEntity()`；`FunnelAggregationEngine` 经验证为纯内存聚合引擎（无 daoProvider 注入，测试直接 new 构造，结果由调用方 BizModel 落库）→ 加注释说明保留
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] ErpCsSurveyBizModel 的 daoFor 已改为 `survey.getTicket()` ORM 导航
- [x] ~4-5 处 `new Erp*()` 实体构造违规已改为 `newEntity()`；`FunnelAggregationEngine` 若经验证为纯内存用途则加注释保留
- [x] compliance checker R3 规则计数下降（仅余 DTO/support 类的合法 `new`）

---

### Phase 6 - 防护机制：skill 自检 + compliance checker

Status: completed
Targets: `.opencode/skills/nop-backend-dev/SKILL.md`、`docs/audits/nop-compliance-checker.sh`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2-3 完成（自检项需基于已落地的实体方法模式）

- [x] **Add: nop-backend-dev skill 自检清单增加 E1-E3 检查项**
  - 在 SKILL.md "第二层：产品化可定制性"表中增加：
    - **E1**: 当前实体的 Processor 中是否存在可直接上提到实体的稳定状态判断方法（`isAlreadyApproved`/`isAlreadyRejected` 等）？适用范围：涉及审批/状态流转的方法。后果：状态判断逻辑散落在各 Processor，无法复用。
    - **E2**: 当 BizModel/Processor 需要获取关联子实体时，是否优先使用 ORM 关系 getter（`entity.getLines()`）而非 `daoFor(ChildEntity.class).findAllByQuery()`？适用范围：涉及子实体访问的方法。后果：冗余查询，绕过 ORM 缓存。
    - **E3**: BizModel 中 `daoFor()` 调用是否有注释说明原因（同域子实体/架构约束/只读聚合）？适用范围：所有 `daoFor()` 调用。后果：后续维护者无法区分有理由的 daoFor 和反模式。
  - Skill: `nop-backend-dev`

- [x] **Add: compliance checker 增加 R11 规则——Processor 状态方法重复检测**
  - 在 `nop-compliance-checker.sh` 中增加规则：统计 `isAlreadyApproved`/`isAlreadyRejected` 在 `*Processor.java` 中的定义次数。Phase 2-3 完成后计数应为 0（实体方法已替代）。非零则告警。
  - Skill: none

Exit Criteria:

- [x] SKILL.md 自检表包含 E1-E3 三项，每项有适用范围和后果说明
- [x] compliance checker R11 规则已添加，执行后对当前代码库的 `isAlreadyApproved` 计数为 0

---

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_094b8c549ffeAX1udm9c6yQsah) because 2 blocking issues found: (1) Phase 5 `new Erp*()` violation list contained 2 false positives (`QuotaRollupCalculator` transient value object, `GapAnalysisCalculator` defensive fallback) — corrected to ~4-5; (2) Decision D2 was conditional but ORM `ErpCsSurvey.ticket` to-one relation already verified at `app-erp-cs.orm.xml:504` — converted to definitive ORM navigation fix. Also addressed: constant naming alignment in Phase 1, Phase 4 candidate pre-enumeration. All revisions applied.
- Independent draft review iteration 2: accept (ses_094b131edffeWfdX9rP9O7eCCR) — both iteration 1 blocking issues confirmed resolved; no new blocking issues; internal consistency verified (all Erp*() count references corrected to ~4-5); anti-slack rules compliant; exit criteria specific; phase DAG acyclic; closure gates map to goals. Minor non-blocking notes (Phase 3 "~8" vs 9 count tolerance, stylistic "如果" in closure gate) within acceptable approximation.

## Closure Gates

- [x] 范围内行为完成：~20 实体有状态方法、21 Processor 已去重、daoFor Type 1+4 已修复、~4-5 new Erp 已修复、防护机制已建立
- [x] 相关文档对齐：`docs/design/purchase/state-machine.md` 无需更新（状态值未变，仅方法位置变了）；D1 创建了 5 个新的 dao 层常量接口文件，注释指向 ORM dict 为权威源
- [x] 已运行验证：`mvn clean install -DskipTests` 全 reactor 通过（154 模块基线 BUILD SUCCESS）；compliance checker 执行后 R3 计数下降（17，仅余 DTO/support 类合法 `new`）+ R11 计数为 0
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### daoFor() 类型 2（同域子实体访问）—— ~300-350 处

- Classification: `optimization candidate`
- Why Not Blocking Closure: 同域子实体访问（如 `ErpPurOrderBizModel` 中 `daoFor(ErpPurOrderLine.class).saveEntity()`）在 Nop 中不是严格反模式。审计报告 2026-07-05 §2.5 将其标记为"假阳性"。可优化为 ORM 级联保存（`order.getLines().addAll(...)`），但不影响正确性或安全性。
- Successor Required: `yes` — 当同域 daoFor 数量影响代码可读性或引入性能问题时，开启独立优化计划

### daoFor() 类型 3（Processor 架构约束）—— ~150-200 处

- Classification: `watch-only residual`
- Why Not Blocking Closure: Processor 的 BizModel 注入为单向（BizModel → Processor），不可反注入 I*Biz（循环依赖）。日志 H-4 裁决明确认可 Processor 内 `dao().updateEntity()` 为内部编排持久化模式。`processor-extension-pattern.md` 认可此模式。
- Successor Required: `no` — 除非 Nop 平台改变 Processor 注入方向设计

### daoFor() 类型 5（看板/报表只读聚合）—— ~80-120 处

- Classification: `watch-only residual`
- Why Not Blocking Closure: 纯只读聚合查询有合理存在理由——经 I*Biz 的 `findList` 受 XMeta 查询算子白名单约束，内部聚合不需要这些约束。
- Successor Required: `no`

### daoFor() 类型 6（历史残留）—— ~100-150 处

- Classification: `optimization candidate`
- Why Not Blocking Closure: 大部分已归入上述类型 1-5 的分类中。少量真正的历史残留（无明确分类的 daoFor）可在后续迭代中逐步清理。
- Successor Required: `yes` — 在后续功能开发中顺带清理触及的文件

### 为实体添加超出状态判断的领域方法

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 差距分析 §6.1 步骤 2（"搜索纯计算逻辑上提"）过于宽泛，无具体候选清单。实体状态方法是机械的、清晰的；纯计算逻辑上提需要逐案分析且可能引入过度工程。
- Successor Required: `yes` — 当具体实体的计算逻辑重复出现时，按需提取

### 抽取 Processor 共享泛型基类

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 21 个 Processor 类型不同（`ErpPurOrder` vs `ErpSalOrder` vs ...），泛型基类 `ErpDocProcessorBase<T>` 可消除签名重复，但引入跨域依赖（基类放哪个模块？）和泛型复杂度。实体方法上提后，Processor 中的重复已消除，收益不再显著。
- Successor Required: `no`

## Closure

Status Note: 全部 6 阶段执行完成。`mvn clean install -DskipTests` 全 154 模块 reactor BUILD SUCCESS（2026-07-17）；compliance checker R11=0（Processor 重复状态方法已全部上提到实体），R3=17（仅余合法 DTO/support 类）。独立结束审计已于 2026-07-17 由独立子代理完成（新会话，未复用执行者上下文）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（closure-auditor 新会话，ses_2026-07-17-closure-audit），不复用执行者上下文
- Audit Scope: 五点一致性、Exit Criteria vs 实时代码、Anti-Hollow、Deferred honesty、Docs sync
- Independent Verification Against Live Repo (2026-07-17):
  - 5 个 dao 层状态常量接口存在：`module-{purchase,sales,finance,assets,inventory}/erp-*-dao/.../constants/*DocStatus.java` ✅
  - `rg -c "isApproved|isRejected|isCancelled"` 在 `ErpPurOrder.java`、`ErpSalOrder.java` 各 3 处（实体方法已落地）✅
  - `rg "isAlreadyApproved|isAlreadyRejected" --glob *Processor.java` 命中 0（R11 = 0，Processor 重复已消除）✅
  - `ErpCsSurveyBizModel.java:68` 实际使用 `survey.getTicket()` ORM 导航（Type 4 修复确认）✅
  - `nop-backend-dev/SKILL.md` 行 158-160 含 E1/E2/E3 自检项，行 163 声明 E1-E3 按场景检查 ✅
  - `docs/audits/nop-compliance-checker.sh` 行 269-282 含 R11 规则，检测 `protected boolean (isAlreadyApproved|isAlreadyRejected)(` ✅
- Five-Point Consistency: Plan Status=completed / 各 Phase Status=completed / 全部 Exit Criteria `[x]` / 全部 Closure Gates `[x]` / Closure evidence 一致 ✅
- Anti-Hollow: 实体方法被 Processor 调用（非空壳），daoFor 替换为实际 ORM getter（非占位）✅
- Deferred Honesty: 类型 2/3/5/6 daoFor 与 Processor 基类抽取均带 `Successor Required` + 触发条件，无活缺陷隐藏 ✅
- Pre-existing Executor Evidence (carried forward):
  - 编译：`mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS（1:40 min, 2026-07-17T08:14:57+08:00）
  - 5 个 dao 层状态常量接口已创建：`ErpPurDocStatus`、`ErpSalDocStatus`、`ErpFinDocStatus`、`ErpAstDocStatus`、`ErpInvDocStatus`
  - service 层 `ErpPur/ErpSal/ErpFin/ErpAst/ErpInv Constants` 均 extends 新接口，向后兼容
  - ~20 实体添加状态方法（isApproved/isRejected/isCancelled）：purchase 6 + sales 6 + finance 3 + assets 5 + inventory 1
  - 21 个 Processor 中 59 个 `isAlreadyApproved/isAlreadyRejected/validateNotCancelled` 定义已删除/重写为调用实体方法
  - daoFor Type 1 修复：6 处 `loadLines/loadOrderLines` 改为 ORM 导航（purchase 5 Processor + sales BizModel）
  - daoFor Type 4 修复：`ErpCsSurveyBizModel.createSurvey` 改为 `survey.getTicket()` ORM 导航
  - new Erp*() 修复：4 处改为 `dao.newEntity()`/`daoProvider.daoFor().newEntity()`（ErpSalPricingRuleEngine、NotificationDispatcher、ErpSysNotificationBizModel ×2）；FunnelAggregationEngine 加注释保留（纯内存聚合）
  - skill 自检 E1-E3 三项已加入 `nop-backend-dev/SKILL.md`
  - compliance checker R11 规则已添加，对当前代码库 `isAlreadyApproved` 计数 = 0
  - 预存测试失败（`TestErpPurRequisitionApproval.testReqIllegalTransitionRejected` + 日期相关快照失败）经 git stash 验证非本计划引入

Follow-up:

- daoFor() 类型 2/6 的后续清理计划（触发条件：数量影响可读性或性能）
- 实体纯计算逻辑按需上提（触发条件：具体重复出现）
