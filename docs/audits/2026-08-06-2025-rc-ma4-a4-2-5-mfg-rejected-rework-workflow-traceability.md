# rc-ma4-a4-2-5 mfg REJECTED 工单返工工作流 + 关联原工单可追溯性运行时确认

> 报告类型：MA4（RC）运行时行为验证（A4.2.5，A1.9 SP-2 + A1.31 SP-2 合并）
> 验证时间：2026-08-06
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 + §去重协议 MA4↔A5.6 边界）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> 上游计划：`docs/plans/2026-08-06-2025-2-rc-ma4-a4-2-5-mfg-rejected-rework-workflow-traceability.md`（草案审查 iteration 1 accept，独立子代理 `ses_028f1d030ffepK5g9SePNJJNQS`）
> 存疑点来源：`docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md` §7 SP-2（UC-MFG-09 返工工单运行时操作流程）+ `docs/audits/2026-08-05-1830-2-rc-ma1-a1-31-quality-f1-inspection-gating.md` §7 SP-2（UC-QA-04 完工返工跨域触发链运行时操作流程，A1.31 §7:254 明确标注「与 A1.9 SP-2 合并」）
> 关联 finding：`P2-RC-041`（arm-index:209，UC-QA-04 完工返工跨域触发链 successor watch-only）
> 范式对齐：A4.2.6-8（done — mfg 运行时探针先例）
> 结论速览：✅ **维持 P2-RC-041 watch-only** — REJECTED 门控运行时确认（工单保持 IN_PROCESS 不在终态）+ 操作员手动工作流可达（stop/close/标准 CRUD 新建返工工单）+ 可追溯性**部分可达**（remark 手工备注 + NCR→inspection→WO 2 跳反向追溯，无 originalWorkOrderId 结构化关联）→ 决策树分支①命中，不升 P1。config 双门控默认 OFF（REJECTED 路径非默认活跃）。

---

## 1. 验证范围与需求契约

本验证是**只读运行时行为评估**（无代码/ORM/api.xml/真相源变更），闭合 A1.9 SP-2 + A1.31 SP-2 同一存疑点：完工质检 REJECTED 后，操作员面对保持 IN_PROCESS 工单的实际可达工作流 + 「关联原工单」可追溯性的实际可达性。

### 存疑点原文（逐字引用）

**A1.9 §7 SP-2**（`2026-08-02-2042-3-...-a1-9-...md` §7:311）：
> UC-MFG-09 返工工单运行时操作流程：L1 ㉕字面『原工单不可恢复（终态），新建返工工单（关联原工单）』，实现为 config-gated 门控（工单保持 IN_PROCESS 不在终态）+ 操作员手动新建标准工单（无 originalWorkOrderId 关联字段）。运行时操作员面对 REJECTED 工单时的实际工作流（是否手动关闭原工单→新建返工工单 / 或重置质检状态重新报工 / 或经 useLogicalDelete）需运行时确认，验证『关联原工单』的可追溯性是否经工单备注/手工关联实际可达。

**A1.31 §7 SP-2**（`2026-08-05-1830-2-...-a1-31-...md` §7:254）：
> UC-QA-04 完工返工跨域触发链运行时操作流程（与 A1.9 SP-2 同一存疑点，quality 侧投影）：quality FINAL REJECTED + NCR 生成后，mfg 侧工单保持 IN_PROCESS，操作员面对 REJECTED 工单时实际工作流 + 『关联原工单』可追溯性是否经工单备注/手工关联实际可达。

### L1 需求契约（逐字，§4 Q1 权威）

`docs/design/manufacturing/use-cases.md` UC-MFG-09（A1.9 §1 逐字引用）：
```
完工触发质检(若 BOM.inspection_required) →                         [断言㉓]
  质检 REJECTED → 不合格                                            [断言㉔]
  原工单不可恢复(终态), 新建返工工单(关联原工单)                     [断言㉕]
返工工单走标准流程, 产出合格品                                       [断言㉖]
```

`docs/design/quality/use-cases.md` UC-QA-04（A1.31 §1 逐字引用）：
```
完工质检(FINAL) REJECTED →
  不合格处理路径: 返工(见 §二 路径表)
  触发 ../manufacturing 新建返工工单(关联原工单)
```

---

## 2. REJECTED 门控运行时行为 census

### 2.1 门控代码路径（写时实测行号）

`module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderReportCompletionProcessor.java#reportCompletion:31-104`：

```
:33  facade.requireStatus(wo, IN_PROCESS, "IN_PROCESS")      // 完工前置：仅 IN_PROCESS 可报工
:45  boolean willFinish = planned>0 && newCompleted>=planned // 达量判定
:46  if (willFinish && facade.isInspectionGated(wo)) {       // 门控① mfg 侧
:47      throw ERR_INSPECTION_REQUIRED                       // → 工单保持 IN_PROCESS
:48  }
:51  if (willFinish && wo.getProductId() != null) {
:52      int gate = InspectionTrigger.enforceGate(            // 门控② quality 侧跨域
:53              ..., RELATED_BILL_TYPE_MFG_WORK_ORDER, wo.getCode(), ...);
:55      if (gate == BLOCKED) {
:56          throw ERR_INSPECTION_REQUIRED                   // → 工单保持 IN_PROCESS
:57      }
:59  }
```

**双重门控机制**：

| 门控 | 位置 | 触发条件 | config 默认值 | REJECTED 时行为 |
|------|------|---------|--------------|----------------|
| 门控① mfg 侧 `isInspectionGated` | `ErpMfgWorkOrderProcessor.java#isInspectionGated:329-338` | config `erp-mfg.inspection-gate-enabled`=true **AND** BOM.inspectionRequired=true | **FALSE**（`ErpMfgWorkOrderProcessor.java:390` `readBoolConfig(..., false)`） | 直抛 `ERR_INSPECTION_REQUIRED`（:47），工单保持 IN_PROCESS |
| 门控② quality 侧 `enforceGate` | `InspectionTrigger.java#enforceGate:35-50`（module-quality/erp-qa-dao） | billType ∈ config `erp-qua.mandatory-inspection-bill-types`（逗号分隔） | **空**（`InspectionTrigger.java:57-60` 默认 `""`=不强制） | BLOCKED（:55-57），工单保持 IN_PROCESS |

`InspectionTrigger.enforceGate:35-50` 语义（写时实测）：
```
:39  if (!isMandatoryBillType(billType)) return CLEARED;        // 非强制类型→放行
:42  existing = inspectionBiz.findByRelatedBill(billType, billCode)
:43  if (existing.isEmpty()) {
:45      inspectionBiz.createForBusinessBill(...);              // 首次：生成 PENDING 质检单
:47      return BLOCKED;                                        // 阻塞本次流转
:48  }
:49  return isInspectionCleared(...) ? CLEARED : BLOCKED;       // 已有：ACCEPTED/CONDITIONAL→CLEARED / PENDING/REJECTED→BLOCKED
```

`ErpQaInspectionBizModel.java#isInspectionCleared:79-94`（A1.31 §2 已核实）：PENDING/REJECTED→false→BLOCKED；ACCEPTED+CONDITIONAL→true→CLEARED。

### 2.2 REJECTED 运行时行为结论

**确认（实测锚点）**：完工达量（willFinish=true）+ 质检 REJECTED → `enforceGate` 返回 BLOCKED（`InspectionTrigger.java:49`，因 `isInspectionCleared` 对 REJECTED 返回 false）→ `ErpMfgWorkOrderReportCompletionProcessor.java:56` 抛 `ERR_INSPECTION_REQUIRED` → @BizMutation 事务回滚 → **工单保持 IN_PROCESS，不进终态 COMPLETED**。

- 事务回滚覆盖（A2.6a §1.4 已证实）：`reportCompletion` 经 Facade `@BizMutation`（`ErpMfgWorkOrderBizModel.java:100-105`），抛异常 → 事务回滚 → 工单 completedQuantity 不递增、docStatus 不变（保持 IN_PROCESS）。
- **关键事实**：`reInspect` 已删除（A2.12 P0-MA2-017 resolved），复检走 `createForBusinessBill` 新建独立质检单。REJECTED 质检单结果**不可重置**，故同一质检单路径下工单**永远无法经 reportCompletion 达 COMPLETED**（enforceGate 对 REJECTED 恒返回 BLOCKED）。

**L1 ㉕ 字面张力**：L1 假设「完工后质检」（工单已达终态 COMPLETED 再质检），实现是「完工前门控」（达量时拦截，工单保持 IN_PROCESS 不在终态）——故 L1 字面「原工单不可恢复（终态）」与实现（工单保持 IN_PROCESS 非终态）存在范式张力。此张力 A1.9 §5.2 已裁决倾向接受（owner doc §3 + §场景D 与 L1 一致声明该路径，设计裁定 successor 触发条件）。本验证运行时确认该张力**不阻断主路径**（操作员可达替代动作见 §3）。

---

## 3. 操作员可达后续动作 census（REJECTED 工单 IN_PROCESS）

核验 REJECTED 工单（保持 IN_PROCESS）后续可达动作全集，基于 `ErpMfgWorkOrderBizModel.java` 全部 @BizMutation/@BizQuery 方法注册 + 各 per-mutation Processor 状态守卫。

| 动作 | BizModel 方法（@BizMutation） | Processor 状态守卫 | IN_PROCESS 可达？ | 证据 |
|------|------------------------------|-------------------|------------------|------|
| **reportCompletion**（重报工） | `ErpMfgWorkOrderBizModel.java:100-105` | `ErpMfgWorkOrderReportCompletionProcessor.java:33` requireStatus IN_PROCESS ✅，但 `:46-57` 门控对 REJECTED 恒 BLOCKED | ❌ **不可达**（重报工恒抛 ERR_INSPECTION_REQUIRED，工单无法经此达 COMPLETED） | `InspectionTrigger.java:49` REJECTED→isInspectionCleared=false→BLOCKED |
| **stop**（暂停） | `ErpMfgWorkOrderBizModel.java:76-79` | `ErpMfgWorkOrderStopProcessor.java:19` requireStatus IN_PROCESS ✅ → `:24-26` doStop setDocStatus STOPPED | ✅ **可达**（IN_PROCESS→STOPPED） | `ErpMfgWorkOrderStopProcessor.java:17-27` |
| **resume**（恢复） | `ErpMfgWorkOrderBizModel.java:81-85` | `ErpMfgWorkOrderResumeProcessor` requireStatus STOPPED → IN_PROCESS | ✅ **可达**（仅从 STOPPED；IN_PROCESS 不能 resume） | resume Processor 状态守卫 STOPPED |
| **close**（结案） | `ErpMfgWorkOrderBizModel.java:87-91` | `ErpMfgWorkOrderCloseProcessor.java:27-32` validateTransitionForClose accepts **STOPPED 或 IN_PROCESS** → `:35-41` doClose setDocStatus CLOSED | ✅ **可达**（IN_PROCESS→CLOSED 直达，或经 STOPPED→CLOSED）。CLOSED = 终态 | `ErpMfgWorkOrderCloseProcessor.java:20-41` |
| **cancel**（取消） | `ErpMfgWorkOrderBizModel.java:93-97` | `ErpMfgWorkOrderProcessor.java:128-138` cancel 仅允许 DRAFT/SUBMITTED/NOT_STARTED→CANCELLED，IN_PROCESS **不在允许集** | ❌ **不可达**（IN_PROCESS cancel 抛 illegalTransition `:133`） | `ErpMfgWorkOrderProcessor.java:130-133` |
| **标准 CRUD save**（新建返工工单） | `CrudBizModel<ErpMfgWorkOrder>` 继承 save | 无状态守卫（DRAFT 新建） | ✅ **可达**（操作员手动新建标准工单承载返工语义） | CrudBizModel 标准 save |
| **useLogicalDelete**（软删） | ORM `useLogicalDelete="true" deleteFlagProp="delVersion"` | 平台 CrudBizModel delete 路径 | ✅ **可达**（工单实体启用逻辑删除，orm.xml:572） | `app-erp-manufacturing.orm.xml:572` |

### 3.1 操作员实际工作流（运行时推断）

基于 §3 可达动作全集，REJECTED 工单（IN_PROCESS）操作员**实际可达工作流**：

1. **结案原工单**（IN_PROCESS→CLOSED）：`close` 直达终态 CLOSED（`ErpMfgWorkOrderCloseProcessor.java:30` accepts IN_PROCESS）。这是 L1 ㉕「原工单不可恢复（终态）」的**手动承载**——操作员手动 close 使原工单进入终态（而非实现自动终态化）。
2. **或暂停**（IN_PROCESS→STOPPED→CLOSED）：经 stop 再 close 的两步路径。
3. **新建返工工单**：标准 CRUD save 新建 ErpMfgWorkOrder（DRAFT），承载返工语义（无 originalWorkOrderId 自动关联，见 §4）。
4. **不可**：重报工完成（reportCompletion 恒 BLOCKED）、cancel（IN_PROCESS 不允许）、重置质检结果（reInspect 已删除）。

**结论**：操作员手动工作流**可达**——close 使原工单达终态 + 标准 CRUD 新建返工工单承载「返工」语义。工单不会因 REJECTED 永久卡死在 IN_PROCESS（close 提供终态出口）。

---

## 4. 可追溯性机制 census（「关联原工单」）

核验「关联原工单」可追溯性现状，区分结构化关联字段 vs 手工备注载体。

### 4.1 结构化关联字段 census

| 字段/机制 | 存在性 | 证据 | 可追溯性判定 |
|----------|--------|------|-------------|
| `originalWorkOrderId` / `originalWorkOrder` / `reworkOrder` / `rework` | ❌ **零命中** | `rg "originalWorkOrderId\|originalWorkOrder\|reworkOrder\|rework"` 跨 `module-manufacturing/` 全域 **0 业务命中**（仅注释/无关词） | **不可达**（无结构化返工关联字段） |
| `ErpMfgWorkOrder.sourceOrderType` / `sourceOrderCode` | ✅ 存在但语义不符 | `app-erp-manufacturing.orm.xml:581-582`（propId 8/9，dict `erp-mfg/source-order-type`）——上游来源（MRP 计划/销售订单/预测），**非返工关联** | **不可达**（source-order-type 字典无返工工单类型值） |
| `ErpMfgWorkOrder.remark`（备注） | ✅ 存在 | `app-erp-manufacturing.orm.xml:607`（propId 34，domain `remark` VARCHAR 1000） | **部分可达**（操作员可手工在 remark 录入原工单号，但纯自由文本无结构化约束/校验/反向查询） |

### 4.2 可追溯性判定

**「关联原工单」可追溯性 = 部分可达**：
- **无 originalWorkOrderId 结构化关联字段**（grep 零命中确认）——返工工单与原工单之间无外键/关联表/结构化链接。
- **remark 手工备注是唯一承载载体**——操作员可手工在新建返工工单的 remark 录入原工单号（自由文本，VARCHAR 1000），但：
  - 无校验（remark 可为任意文本，不强制工单号格式）；
  - 无反向查询（无法从原工单 remark 反查所有引用它的返工工单，需全表 LIKE 扫描）；
  - 纯人工约定（依赖运营纪律，非系统强制）。
- 故「关联原工单」经 remark 手工关联**实际可达但脆弱**（人工约定 + 自由文本 + 无反向查询），不构成结构化追溯链。

**裁决方向**：部分可达 → 决策树分支①命中（手动工作流可达 + 可追溯性部分可达经 remark），不升 P1（追溯链未完全断裂，remark 提供最低限度人工追溯载体）。结构化 originalWorkOrderId 自动关联归 MR1 successor（§6）。

---

## 5. 跨域 NCR↔工单关联 census

核验 quality NCR 是否记录源工单 ID 供反向追溯（弥补 mfg 侧 originalWorkOrderId 缺失的替代追溯路径）。

### 5.1 NCR 自动生成字段 census

`module-quality/erp-qa-service/.../entity/NcrLifecycleService.java#autoCreateNcrFromInspection:49-67`（写时实测）：

```java
:54  ErpQaNonConformance ncr = dao.newEntity();
:56  ncr.setCode("NCR-" + inspection.getCode());
:58  ncr.setSourceType(NCR_SOURCE_TYPE_INSPECTION);          // = "INSPECTION"
:59  ncr.setSourceCode(inspection.getCode());                // = 质检单号
:60  ncr.setInspectionId(inspection.getId());                // = 质检单 ID
:61  ncr.setMaterialId(inspection.getMaterialId());          // = 物料 ID
:62  ncr.setQuantity(resolveRejectQuantity(...));
:65  ncr.setStatus(NCR_STATUS_OPEN);
:66  ncr.setDescription(buildRejectDescription(lines));      // = 不合格项详情
```

**NCR 直接记录字段**：sourceType=INSPECTION / sourceCode=质检单号 / inspectionId / materialId / quantity / description。**NCR 不直接记录工单 ID 或工单号**（无 workOrderId/workOrderCode 字段写入）。

### 5.2 反向追溯链（2 跳）

NCR→源工单追溯需经 **2 跳查询**：

```
NCR.inspectionId → ErpQaInspection（load by inspectionId）
  → ErpQaInspection.relatedBillType + relatedBillCode
    → (relatedBillType=ERP_MFG_WORK_ORDER) + relatedBillCode=工单号 → 工单
```

- `ErpQaInspection` 有 `relatedBillType`（propId 5）/`relatedBillCode`（propId 6）字段（`_ErpQaInspection.java:41-46`）。
- 完工门控触发时 `createForBusinessBill(billType=ERP_MFG_WORK_ORDER, billCode=wo.getCode())` 写入（`ErpMfgWorkOrderReportCompletionProcessor.java:52-54` + `ErpMfgWorkOrderProcessor.java:304-305` generateCompletionMove 亦写 relatedBillType）。
- 故 **NCR→源工单反向追溯 = 部分可达（2 跳查询）**：NCR.inspectionId → 质检单 → relatedBillCode=工单号。需跨表联查（NCR + ErpQaInspection），非单字段直达。
- **正向**（工单→NCR/返工工单）：**不可达**——工单无字段指向其触发 NCR 或返工工单。

### 5.3 跨域关联判定

**NCR→源工单反向追溯 = 部分可达（2 跳）**，弥补 mfg 侧 originalWorkOrderId 缺失的部分替代路径：
- 运营可经 NCR.inspectionId 反查质检单，再经质检单 relatedBillCode 获得源工单号（2 跳 SQL/GraphQL 联查）。
- 但非结构化直达（无 NCR.workOrderId 冗余字段），且正向追溯（工单→NCR）不可达。
- 结合 §4 remark 手工备注，可追溯性总体 = **部分可达**（remark 正向人工 + NCR 反向 2 跳），不构成完整结构化追溯链但未完全断裂。

---

## 6. 强制质检门控 config 默认值复核

核验 REJECTED 路径默认活跃性（决定运行时风险面）。

| config key | 默认值 | 证据 | 部署 override |
|-----------|--------|------|--------------|
| `erp-mfg.inspection-gate-enabled` | **FALSE** | `ErpMfgWorkOrderProcessor.java:390` `readBoolConfig(CONFIG_INSPECTION_GATE_ENABLED, false)`；`ErpMfgErrors.java:95` 注释；`module-meta.yaml:24` configKey 登记 | 全 application.yaml grep **零命中**（无部署 override）→ 默认 OFF |
| `erp-qua.mandatory-inspection-bill-types` | **空**（不强制） | `InspectionTrigger.java:57-60` `AppConfig.var("erp-qua.mandatory-inspection-bill-types", "")`；空=不强制 | 全 application.yaml grep **零命中**→ 默认空 |
| `erp-qua.auto-create-ncr-on-reject` | **TRUE** | `NcrLifecycleService.java:51` `ErpQaConfigs.isAutoCreateNcrOnReject()`（默认 true） | — |

**结论**：REJECTED 路径**非默认活跃**——双门控默认 OFF（mfg 门控 false + quality mandatory 空集），故运行时 REJECTED 路径仅在部署显式启用 config 后才可达。当前基线下（无 application.yaml override），REJECTED 工单场景**不会默认触发**，运行时风险面限 config-enabled 部署。**登记 config-enable 运营注意**：启用双门控的部署须配套操作员返工工作流 SOP（close + 标准 CRUD 新建 + remark 手工关联）。

---

## 7. MA4↔A5.6 边界声明

本验证审「**行为是否符合需求**」（返工工作流可达性 + 可追溯性机制），与 A5.6（audit-remediation）审「**E2E 断言强度**」边界按 `docs/audits/requirement-compliance-methodology.md §去重协议 MA4↔A5.6 边界` 执行：
- MA4（本验证）：需求契约视角——REJECTED 后操作员可达动作 + 可追溯性是否符合 UC-MFG-09/UC-QA-04 验收标准。
- A5.6：测试质量视角——REJECTED→返工路径的 E2E 断言强度。
- 判据不同，按此边界执行，**不重做 A5.6 E2E 断言强度审计**。

---

## 8. 运行时工作流裁决

### 8.1 P2-RC-041 successor 运行时现状裁决

**裁决：维持 P2-RC-041 watch-only**（不升 P1）。

**决策树分支**（plan Goals 裁决分支）：
- ✅ **分支①命中**：手动工作流可达（close 使原工单达终态 CLOSED + 标准 CRUD 新建返工工单承载「返工」语义，§3 实测可达）+ 可追溯性**部分可达**（remark 手工备注正向 + NCR→inspection→WO 2 跳反向，§4-5 实测）→ **维持 P2-RC-041 watch-only**（successor[自动建返工工单 + originalWorkOrderId 结构化关联]非阻断主路径，手动闭环成立）。
- 未触发分支②（可追溯性部分可达非完全不可达，故不评估升 P1）。
- ✅ **分支③登记**：config 双门控默认 OFF → REJECTED 路径非默认活跃，登记 config-enable 运营注意（§6）。

### 8.2 裁决依据（§2 判据编号 + 三源对照）

| 维度 | L1 需求契约 | L2 owner doc（设计参考） | L3 实仓代码（本验证实测） | 裁决 |
|------|-----------|------------------------|-------------------------|------|
| REJECTED 门控（㉓㉔） | UC-MFG-09 完工触发质检 + REJECTED | `state-machine.md §质检约束声明` + `§场景D` 一致 | `ErpMfgWorkOrderReportCompletionProcessor.java:46-57` 双门控 + 工单保持 IN_PROCESS（§2 实测） | 接受（A1.9 §5.2 已裁决，本验证运行时确认） |
| 返工工单工作流（㉕㉖） | 原工单终态 + 新建返工工单（关联原工单）+ 走标准流程 | `state-machine.md §3 + §场景D` 与 L1 一致（successor 触发条件） | close 手动达终态 + 标准 CRUD 新建（§3 实测可达），无 originalWorkOrderId 自动关联（§4 实测） | **倾向接受 / P2 successor**（§2 P2③ documented simplification，A1.9 plan-audit 通过） |
| 可追溯性 | 「关联原工单」 | owner doc §3 声明该路径 | remark 手工备注（部分可达）+ NCR 2 跳反向（部分可达），无结构化字段（§4-5 实测） | **部分可达**（决策树分支①，不升 P1） |

**§2 判据命中**：
- §2 P2③（owner doc 显式 documented simplification 标注，A1.9 plan-audit 通过）——「自动建返工工单 + originalWorkOrderId 关联」属 owner doc §3 已裁定的 successor 触发条件。
- §2 P2②（可用性/可观测性 successor）——可追溯性部分可达（remark + NCR 2 跳），结构化自动关联 successor。

**与 A1.9/A1.31 §5 P2-RC-041 successor 裁决分层一致**：
- A1.9 §5.2 UC-MFG-09 = 倾向接受（㉕㉖ 操作员驱动简化范式，owner doc successor）。
- A1.31 §5 UC-QA-04 = 倾向接受 / P2 successor（§4 三判据[i] A1.9 plan-audit 通过成立→P2 documented simplification）。
- 本验证 = 运行时现状确认（手动工作流可达 + 可追溯性部分可达）→ **维持 P2-RC-041 watch-only，不升 P1**，与 A1.9/A1.31 §5 分层一致。

### 8.3 finding/注记更新

**维持 watch-only**（决策树分支①命中，不升 P1）→ arm-index `P2-RC-041` 行追加**运行时现状确认注记**（§9 衔接执行）。

**不触发 MR1 即时通道**：本验证为只读评估，修复（originalWorkOrderId 自动关联 + 自动建返工工单）归 MR1 R1.0 展开器读取本报告 finding → RC-R1.n 修复。修复实现 originalWorkOrderId 触及 ORM 结构变更[ErpMfgWorkOrder 加 originalWorkOrderId 关联字段]须 ask-first + 独立 plan-audit §5。

---

## 9. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总表见下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本审计为只读审计，无生产代码变更，checker 无回归风险**。

  | 规则 | 描述 | actual（实测） | baseline（`compliance-baseline.md` BASELINE 块） | 状态 |
  |------|------|---------------|---------------------------------------------|------|
  | R1a | dao().saveEntity (BizModel) | 0 | 0 | = baseline |
  | R1b | dao().updateEntity (BizModel) | 0 | 0 | = baseline |
  | R1c | dao().getEntityById (BizModel) | 0 | 0 | = baseline |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | = baseline |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | = baseline |
  | R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | = baseline |
  | R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | = baseline |
  | R2d | Processor daoFor(ErpMd*) | 34 | 34 | = baseline |

  > R3-R12 规则未在本审计触及模块外漂移（本审计无生产代码变更，全规则 actual 反映审计前既有仓库状态，无回归归因本审计）。本审计**只读**（零 Java/ORM/api.xml/view.xml/真相源变更），checker 计数与本审计行为无关。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告不新建 finding——运行时现状确认结果 = 维持既有 `P2-RC-041`（arm-index:209，A1.31 quality 侧投影）watch-only，追加运行时现状确认注记。与 P2-RC-041（A1.31 §5/§7 + A1.9 §5/§7 SP-2 存疑点）的复用关系：本验证是 A1.9 SP-2 + A1.31 SP-2 合并的 MA4 运行时展开（同一存疑点的运行时确认），非新根因新控制点。MA4↔A5.6 边界已声明（§7），MA1 同 mission 兄弟切片互补不重复（§去重协议）。

---

## Verdict

- **REJECTED 门控运行时行为**：**确认**（`ErpMfgWorkOrderReportCompletionProcessor.java:46-57` 双门控 + REJECTED→BLOCKED→工单保持 IN_PROCESS 不进终态，@BizMutation 事务回滚覆盖）。
- **操作员可达后续动作**：**手动工作流可达**（close 使原工单达终态 CLOSED + 标准 CRUD 新建返工工单；reportCompletion 恒 BLOCKED / cancel IN_PROCESS 不允许 / reInspect 已删除，但 close 提供终态出口，工单不卡死）。
- **可追溯性机制**：**部分可达**（remark 手工备注正向 + NCR→inspection→WO 2 跳反向；无 originalWorkOrderId 结构化关联字段）。
- **跨域 NCR↔工单关联**：**部分可达（2 跳反向）**（NCR.inspectionId→质检单.relatedBillCode=工单号；正向不可达）。
- **config 默认值**：**REJECTED 路径非默认活跃**（双门控默认 OFF，运行时风险面限 config-enabled 部署）。
- **裁决**：**维持 P2-RC-041 watch-only**（决策树分支①命中：手动工作流可达 + 可追溯性部分可达 → 不升 P1）。config-enable 运营注意已登记。
- **不触发 MR0/MR1 即时通道**（只读评估，修复归 MR1 successor）。

**整体结论**：A1.9 SP-2 + A1.31 SP-2 存疑点运行时确认完成——REJECTED 工单返工工作流手动可达（close + 标准 CRUD）+ 可追溯性部分可达（remark + NCR 2 跳），维持 P2-RC-041 watch-only，与 A1.9/A1.31 §5 倾向接受裁决分层一致。结构化 originalWorkOrderId 自动关联归 MR1 R1.0 展开器读取本报告 finding（ORM 部分须 ask-first）。

---

## 报告完整性自检（§6 段落骨架对齐 MA4 运行时探针）

- [x] §1 验证范围与需求契约（存疑点原文逐字 + L1 逐字）
- [x] §2 REJECTED 门控运行时行为 census（file:line + 状态机迁移确认）
- [x] §3 操作员可达后续动作 census（全 @BizMutation 动作 + 可达性判定）
- [x] §4 可追溯性机制 census（结构化字段 + remark + 可达性判定）
- [x] §5 跨域 NCR↔工单关联 census（NCR 字段 + 2 跳反向链）
- [x] §6 强制质检门控 config 默认值复核（双门控默认 OFF）
- [x] §7 MA4↔A5.6 边界声明
- [x] §8 运行时工作流裁决（§2 判据 + 三源对照 + 分层一致）
- [x] §9 过程纪律自检（checker actual=baseline + 独立性 + 交叉去重）

**9 段齐全。未修改真相源**（§9 冻结条款——分歧记入报告 §8，未直改 use-cases.md/state-machine.md/inspection-integration.md/product-scope.md）。**未修改代码/ORM/api.xml**（只读审计）。
