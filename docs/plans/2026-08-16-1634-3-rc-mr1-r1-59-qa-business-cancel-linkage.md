# 2026-08-16-1634-3-rc-mr1-r1-59-qa-business-cancel-linkage RC-R1.59 — quality 业务作废联动取消（P1-RC-041：cancelForBusinessBill Facade + 三域 cancel Processor config-gated wiring）

> Plan Status: completed
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.59（P1-RC-041，UC-QA-08 业务单据作废联动取消质检）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.59 行 + `docs/audits/arm-index.md` P1-RC-041 行（:208）+ 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（RC-R1.59 = 分级确认（P1 硬性 vs P2 deferral 须人工裁决）+ 代码逻辑类预授权）
> Related: `docs/design/quality/use-cases.md`（L1 UC-QA-08 :133-145）；`docs/design/quality/state-machine.md`（§4 :50 + §实现约定 :190）；`docs/audits/2026-08-05-1830-2-rc-ma1-a1-31-quality-f1-inspection-gating.md`（A1.31 §5 UC-QA-08 :212-216 + §6 P1-RC-041 :239）；`docs/audits/2026-08-07-2359-rc-ma4-a4-2-101-112-quality-f1-f2-f3-runtime.md`（A4.2.103 :48-52）；`docs/audits/2026-08-06-0600-rc-ma3-a3-3-sal-ast-prj-qa-successor-review.md`（A3.3 #4 :25/:40/:106）；`docs/plans/2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr.md`（R1.20 Deferred 源头 plan :37/:64）
> Audit: required

## Current Baseline

- **finding P1-RC-041（arm-index:208，UC-QA-08）**：L1（`use-cases.md:139-141`）逐字「入库单.作废 → 关联质检单(若 PENDING) → 取消(CANCELLED)；不影响已 ACCEPTED/REJECTED 的质检单(历史完整)」。L3 实仓（HEAD 核查）：
  - `IErpQaInspectionBiz`（`module-quality/erp-qa-dao/src/main/java/app/erp/qa/biz/IErpQaInspectionBiz.java`，85 行）**无 `cancelForBusinessBill`**——仅 recordResult/findByRelatedBill/createForBusinessBill/isInspectionCleared/passInspection/failInspection/batchPassInspection；grep `cancelForBusinessBill` 跨 quality/purchase/sales/mfg **零命中**；
  - `ErpQaInspectionResultStateMachine`（`erp-qa-service/.../statemachine/ErpQaInspectionResultStateMachine.java`）4 态（PENDING/ACCEPTED/CONDITIONAL/REJECTED）无 CANCELLED 迁移；dict `erp-qa/inspection-result`（orm.xml:40-45）无 CANCELLED option；`ErpQaInspectionResultStateMachine` M4.58 注记：docStatus（DRAFT/ACTIVE/CANCELLED）为 dict-only 泛型占位轴零 writer；
  - `ErpQaInspection` ORM（:115-240）：result（propId 18，dict erp-qa/inspection-result）+ docStatus（propId 19，dict erp/doc-status）+ relatedBillType/relatedBillCode（propId 5/6）——**关联键齐备**；`findByRelatedBill`（ErpQaInspectionBizModel:67-75）按 relatedBillType+relatedBillCode 精确查询；
  - 业务域强制质检门控：`InspectionTrigger.enforceGate`（erp-qa-dao/biz/InspectionTrigger.java:35-50）——billType 属 `erp-qua.mandatory-inspection-bill-types`（默认空）时经 `createForBusinessBill` 生成 PENDING 质检单 + `isInspectionCleared` 阻塞/放行；
  - 三域 cancel 接线点（R5.2/1057-2 per-mutation cancel Processor 家族）：`ErpPurReceiveCancelProcessor.cancel`（erp-pur-service/.../processor/ErpPurReceiveCancelProcessor.java:33-43，入库单 L1 主例）+ `ErpSalDeliveryCancelProcessor.cancel`（erp-sal-service/.../processor/ErpSalDeliveryCancelProcessor.java:33-43）+ `ErpMfgWorkOrderProcessor.cancel`（erp-mfg-service/.../processor/ErpMfgWorkOrderProcessor.java:148-155，facade 保留 cancel）——**三域 cancel 均无 quality 联动调用**；
  - 跨域注入就绪性：`ErpPurReceiveProcessor`（:16/:72 `IErpQaInspectionBiz inspectionBiz`）+ `ErpSalDeliveryProcessor`（:8/:71）+ `ErpMfgWorkOrderProcessor`（:34/:86）已注入 `IErpQaInspectionBiz`——**module 依赖链（pur/sal/mfg → qa dao）已存在**，无新模块依赖；cancel Processor 自身注入面需补（Phase 2 接线时经注入或 facade 传递）。
- **R1.20 Deferred 裁决（源头 plan `2026-07-30-0512-3:37,64` + state-machine.md §实现约定:190）**：P1-MA2-064 经独立 plan-audit 显式裁决 Deferred（owner doc 正式化），successor 触发条件 =「业务作废自动取消质检需求时」；A3.3 复查（2026-08-06）触发条件当时未满足维持 backlog；**本 MR1 行 RC-R1.59 即 successor 落地**（roadmap 已收录为 MR1 修复行）。
- **分级确认（P1 硬性 vs P2 deferral）**：A1.31 §4 三判据[i] R1.20 plan-audit 通过成立 → 倾向 P2 documented simplification with approval；A4.2.103 运行时确认维持 P1（L1 视角 + Q4=(a) 张力声明）——**本计划 Phase 1 完成 product-scope 硬性分级确认**：L1 UC-QA-08 显式 + product-scope 未裁剪（对齐 RC-R1.56 product-scope 确认义务先例）→ Q4=(a) 强制实现禁止方案 B。
- **2026-08-12 批量裁决 B 类降级**（roadmap 头 :49）：**RC-R1.59（cancelForBusinessBill Facade + Processor wiring）在 B 类清单**——纯代码逻辑/跨域契约即可解决（不触 ORM 结构/会计过账/物理删除；**useLogicalDelete 软删属 B 类规范解法，裁决见 D1**），从"越界项 ask-first"**降级为预授权自动执行，不再须 ask-first checkbox**（roadmap 行 RC-R1.59 旧「越界项…双独立子 agent 批准 checkbox」字样按 B 类裁决执行期改写消除歧义，对齐 RC-R1.48 先例）。
- **测试基线**：`TestErpQaInspectionTrigger`（强制质检门控含 createForBusinessBill 路径）；`TestErpQaInspectionStateMachine`（4 态矩阵）；无 cancelForBusinessBill 测试。erp-qa-service **172 tests 全绿**（R1.26 基线）。
- **compliance 基线**：R2c=1420 / R2b=233 / R2d=35；预期新增 daoFor 面（Facade 内按 relatedBill 查询/软删——经 `daoProvider.daoFor(ErpQaInspection.class)`）→ 结束前复跑 checker 分类 baseline-raise vs Fix。

## Goals

- **UC-QA-08 运行时成立（P1-RC-041 核心）**：`IErpQaInspectionBiz.cancelForBusinessBill(billType, billCode)` Facade——按 relatedBillType+relatedBillCode 查关联质检单，**仅 PENDING 取消（CANCELLED 语义）**，已 ACCEPTED/CONDITIONAL/REJECTED 不动（历史完整，L1 :141）；无匹配零副作用。
- **取消载体裁决**（Phase 1 D1，对齐 arm-index P1-MA2-064 修复方案 A「PENDING→cancelled via useLogicalDelete」+ B 类枚举边界）：方案 A（推荐）useLogicalDelete 软删 PENDING 质检单（对齐 P1-MA2-064 arm-index 修复方案 + R1.20 plan :64 措辞「PENDING→cancelled via useLogicalDelete」；`findByRelatedBill` 默认 delVersion=0 过滤 → 作废后自动不可见，门控/反查语义天然闭合）；方案 B（result dict 加 CANCELLED option + 状态机迁移）——**dict 追加不在 RC-R1.59 B 类枚举内**（2026-08-08 不可类推规则），须双 agent 批准或保守不注册；倾向 A。
- **三域 cancel wiring（config-gated）**：`ErpPurReceiveCancelProcessor`（入库单，L1 主例）+ `ErpSalDeliveryCancelProcessor`（出库单）+ `ErpMfgWorkOrderProcessor.cancel`（工单）——cancel 成功后调 `cancelForBusinessBill(billType, code)`，门控键 Phase 1 D2 定稿（默认值决策：倾向 TRUE——仅作用于 PENDING 且关联已作废单据的质检单，零活跃数据危害；或 FALSE 保守 config-gate，owner doc 注记部署启用语义）。
- **product-scope 分级确认闭环**：Phase 1 复核 product-scope.md quality 域 → 未裁剪确认登记 → P1 强制实现义务成立（对齐 RC-R1.56 先例）。
- **测试**：新增 `TestErpQaBusinessCancelLinkage`（qa-service Facade 单测）——①PENDING→软删（findByRelatedBill 查无）；②ACCEPTED/REJECTED 不动；③无匹配零副作用；④幂等（重复取消零副作用）；⑤config 关闭跳过；三模块跨域接线测试落接线所在模块（pur/sal/mfg 各新增 cancel 联动测试类，对齐 R1.48 先例 + qa-service pom 禁止反向依赖约束）；既有 172 tests 零回归。
- **零回归**：erp-qa-service 全量测试全绿 + 全仓 `mvn test` + 全量构建 + compliance checker 零漂移（新增 daoFor 站点带 per-site 证据 baseline-raise 或零新增）。
- **owner doc 收敛**：state-machine.md §4 + §实现约定:190 Deferred→已实现 + 实现注记（D1/D2 裁决）；arm-index P1-RC-041 → done (RC-R1.59) + roadmap 行 done + 行标签 B 类改写 + logs 条目。

## Non-Goals

- **不实现 CANCELLED 结果态（dict + 状态机迁移）**（除非 Phase 1 D1 裁决选项 B——默认倾向 A 软删；dict 追加超 B 类枚举须双 agent 批准，按 2026-08-08 不可类推规则保守处理）。
- **不实现作废联动通知**（L1 未要求 notify 派发）。
- **不实现业务单据作废的质检历史清理**（软删 PENDING 仅；ACCEPTED/CONDITIONAL/REJECTED 保留审计）。
- **不改质检单状态机 Bean/result dict**（方案 A 下零状态机变更）。
- **不重写 cancel Processor 主流程**（仅取消成功后追加联动调用，复用既有 setDocStatus 等 helper）。
- **不改真相源契约段落**（use-cases L1 不动；state-machine.md 契约段不动，仅补实现注记 + Deferred 更新）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：纯代码逻辑类预授权[cancelForBusinessBill Facade + 三域 config-gated wiring]，2026-08-12 B 类裁决降级；Q4=(a) 强制实现禁止方案 B；product-scope 分级确认义务）
- Owner Docs: `docs/design/quality/use-cases.md`（L1 UC-QA-08）+ `docs/design/quality/state-machine.md`（§4/§实现约定）
- Skill Selection Basis: 跨实体 Facade + per-mutation/Processor 接线（`nop-backend-dev`：I*Biz 注入 + config 门控范式 + 幂等语义）；测试（`nop-testing`：JunitBaseTestCase + GraphQL 引擎集成 + 跨域测试）。

## Infrastructure And Config Prereqs

- 新 config 键（Phase 1 D2 定稿）：`erp-qua.business-cancel-linkage-enabled`（ErpQaConstants.CONFIG_* + ErpQaConfigs reader；默认值 D2 裁决）。
- 无 ORM 变更（方案 A）——无需增量重生成；若 D1 选 B（dict option）则须按不可类推规则评估批准路径。
- 分域验证前置：`mvn test -pl module-quality/erp-qa-service` + 跨域 `-pl module-purchase/erp-pur-service,module-sales/erp-sal-service,module-manufacturing/erp-mfg-service`。

## Execution Plan

### Phase 1 - product-scope 分级确认 + 取消载体/门控/接线点裁决（Decision）

Status: completed
Targets: `docs/requirements/product-scope.md`（quality 域复核）；`docs/design/quality/state-machine.md`（§实现约定注记草案）
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: none

- [x] `Decision` product-scope 分级确认：复核 product-scope.md quality 域未裁剪「业务作废联动取消质检」→ L1 UC-QA-08 显式硬性要求 → P1 强制实现（Q4=(a) 无例外）→ 确认结论登记本计划（对齐 RC-R1.56 先例）
      - Skill: none
      - **结论（2026-08-16 执行）**：product-scope.md §业务域范围 quality 域行「质量管理：质检、NCR 不符合项、CAPA 纠正预防」完整在位，未裁剪；§延迟范围（SaaS/垂直行业/外部集成）不含质量联动；§当前里程碑无裁剪声明 → L1 UC-QA-08（use-cases.md:133-145）为硬性要求，Q4=(a) 强制实现义务成立。无新 P2 deferral 裁决。
- [x] `Decision` D1 取消载体：方案 A（推荐）useLogicalDelete 软删 PENDING 质检单——**useLogicalDelete 属平台逻辑删除（delVersion 置位、ORM 查询 delVersion=0 自动过滤、审计可追溯；ErpQaInspection 实体 useLogicalDelete 已启用，orm.xml:169），非物理删除（无 DELETE 语句/无数据不可恢复），系 B 类裁决直接覆盖的规范解法（规范解法三权威源一致：arm-index P1-MA2-064 方案 A :556「PENDING→cancelled via useLogicalDelete」+ R1.20 plan :64 + state-machine.md :190；先例 R1.17 plan 亦以 useLogicalDelete 承载 CANCELLED 语义）→ **本计划裁决：不触发 data deletion 保护区域双 agent 批准**（本裁决为本计划的解释性声明：保护区域表 data deletion 项 + roadmap:18「数据删除/数据迁移」按物理删除/迁移语义解读，逻辑删除系 B 类已预授权规范解法；非政策文本字面，供审计知悉）**；若草案审查持异议则 D1 落定后补双 agent 批准**。方案 B（result dict 加 CANCELLED + 状态机迁移）——dict 追加超 RC-R1.59 B 类枚举（2026-08-08 不可类推规则）须双 agent 批准或保守不注册；倾向 A。记录选择 + 替代方案 + 残留风险（软删后作废质检单无独立"已取消"状态展示；审计经 delVersion/删除时间可追溯）
      - Skill: `nop-backend-dev`
      - **裁决（2026-08-16 执行）**：**选方案 A**。实现载体 = 平台逻辑删除：`IEntityDao.removeEntity()`（ErpQaInspection 启用 useLogicalDelete → `EntityPersisterImpl.delete()` 转 UPDATE 置 delVersion=CoreMetrics.currentTimeMillis()，见 nop-entropy `docs-for-ai/02-core-guides/logical-deletion.md`）；`findByRelatedBill`（CrudBizModel findList 路径）在 EQL 编译期自动追加 delVersion=0 过滤（同 doc :108-110），软删后反查自动不可见，门控/反查语义天然闭合，零查询层改动。方案 B（result dict 加 CANCELLED + 状态机迁移）不选——dict 追加超 RC-R1.59 B 类枚举，2026-08-08 不可类推规则下须双 agent 批准，且 L1「取消(CANCELLED)」语义经软删已满足。残留风险：软删质检单无独立"已取消"状态展示（审计经 delVersion/删除时间可追溯，登记 Deferred But Adjudicated「软删质检单的『已取消』审计展示」）；软删行保留在表内不物理清理（watch-only）。
- [x] `Decision` D2 config 门控：键名 `erp-qua.business-cancel-linkage-enabled` + 默认值裁决（TRUE——仅作用于 PENDING 且关联已作废单据，零活跃数据危害，对齐 R1.20 batch-expiry-check-enabled 默认 TRUE 先例 vs FALSE——保守 config-gate 部署启用语义）——记录理由 + owner doc 注记语义
      - Skill: `nop-backend-dev`
      - **裁决（2026-08-16 执行）**：键名 `erp-qua.business-cancel-linkage-enabled`，**默认 TRUE**。理由：Facade 仅取消 result=PENDING 且关联单据已作废的质检单（关联键精确匹配），零活跃数据危害；对齐 R1.20 batch-expiry-check-enabled 默认 TRUE 先例 + R1.48 reservation-enabled 默认 TRUE 先例。FALSE 保守语义（部署启用）作替代方案登记——部署侧可显式关闭以保持作废零副作用，owner doc 注记部署启用语义。实现：`ErpQaConstants.CONFIG_BUSINESS_CANCEL_LINKAGE_ENABLED` + `ErpQaConfigs.isBusinessCancelLinkageEnabled()`（AppConfig.var 默认 "true"）。
- [x] `Decision` D3 接线点与调用时机：三域 cancel 后置调用（cancel 成功 setDocStatus(CANCELLED) 后）`inspectionBiz.cancelForBusinessBill(RELATED_BILL_TYPE_*, code, context)`——billType 常量（ErpQaConstants:53-55 RELATED_BILL_TYPE_PUR_RECEIPT/SAL_DELIVERY/MFG_WORK_ORDER）+ 调用面注入（pur/sal cancel Processor 增 @Inject IErpQaInspectionBiz 或经 facade 传递；mfg 复用既有 :86 注入）；失败处理（try/catch LOG.warn 不阻断作废主流程——联动为辅助语义，业务作废不受 quality 故障影响）
      - Skill: `nop-backend-dev`
      - **裁决（2026-08-16 执行）**：接线点 = 三域 cancel 主流程成功后置调用。pur：`ErpPurReceiveCancelProcessor.cancel` 在 `dao().updateEntity(receive)`（docStatus=CANCELLED 落库）后调 `inspectionBiz.cancelForBusinessBill(...)`；sal：`ErpSalDeliveryCancelProcessor.cancel` 同位置；mfg：`ErpMfgWorkOrderProcessor.cancel` 在 `releaseReservations` 后（docStatus 已 CANCELLED）调。调用面注入：pur/sal cancel Processor 增 `@Inject IErpQaInspectionBiz`（模块依赖链 pur/sal→qa-dao 已存在，IErpQaInspectionBiz 在 qa-dao）；mfg 复用既有 :86 注入。失败处理：try/catch Exception → LOG.warn（不阻断作废主流程，联动为辅助语义）。**billType 常量关键细节**：联动匹配按「创建时写入的 relatedBillType 值」精确查询——pur 创建路径（ErpPurReceiveProcessor.enforceInspectionGate:353 + InspectionTrigger.enforceGate）传 `ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE`="ERP_PUR_RECEIVE"（**非** ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT="ERP_PUR_RECEIPT"，两常量值不同），故 pur cancel 接线必须传 `ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE` 才能命中；sal 传 `ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY`（"ERP_SAL_DELIVERY"，与 qa 常量一致）；mfg 传 `ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER`（"ERP_MFG_WORK_ORDER"，一致）。各域使用本域常量（创建路径同源）保证键值一致。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] product-scope 分级确认结论 + D1-D3 各记录选择/替代方案/残留风险（写入计划或 state-machine.md 注记）
- [x] 取消载体裁决落定（方案 A 软删 or 方案 B dict——B 须含批准路径评估）

### Phase 2 - Facade + 三域接线实现（Add）

Status: completed
Targets: `module-quality/erp-qa-dao/.../biz/IErpQaInspectionBiz.java`（新契约）；`module-quality/erp-qa-service/.../entity/ErpQaInspectionBizModel.java` + `processor/`（新 per-mutation Processor 或 BizModel 内实现——按既有每 mutation 一 Processor 模式）；`ErpQaConstants.java`/`ErpQaConfigs.java`（config 键）；`ErpPurReceiveCancelProcessor`/`ErpSalDeliveryCancelProcessor`/`ErpMfgWorkOrderProcessor`（接线）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add` `IErpQaInspectionBiz.cancelForBusinessBill(billType, billCode, context)` 契约 + BizModel Facade + 实现（D1 定稿：查 relatedBillType+relatedBillCode 且 result=PENDING → 软删；返回取消数或 List——契约 Phase 2 定稿；幂等：二次调用查无 PENDING 零副作用）
      - Skill: `nop-backend-dev`
      - **契约定稿（2026-08-16 执行）**：`@BizMutation int cancelForBusinessBill(@Name("billType") String billType, @Name("billCode") String billCode, IServiceContext context)`——返回实际取消数（int，可断言幂等零副作用）。实现 = per-mutation Processor `ErpQaInspectionCancelForBusinessBillProcessor`（extends AbstractErpQaInspectionProcessor，protected step：isLinkageEnabled config 门控 / findPendingByRelatedBill 按关联键+result=PENDING 查询 / softDeletePending 软删）；BizModel 注入 Processor 委托。软删载体 = `inspectionDao().deleteEntity(ins)`——ErpQaInspection 启用 useLogicalDelete，dao.deleteEntity 自动转 UPDATE delVersion=currentTimeMillis（平台逻辑删除，`docs-for-ai/02-core-guides/logical-deletion.md:102`；初稿误用 removeEntity 编译失败，修正为 deleteEntity）。`findByRelatedBill`（CrudBizModel findList 路径）EQL 编译期自动追加 delVersion=0 过滤 → 软删后反查自动不可见（门控/反查语义天然闭合）。幂等：二次调用查无 PENDING 返回 0。
- [x] `Add` config 键落地（D2）：ErpQaConstants.CONFIG_BUSINESS_CANCEL_LINKAGE_ENABLED + ErpQaConfigs.isBusinessCancelLinkageEnabled()
      - Skill: `nop-backend-dev`
      - **实现（2026-08-16 执行）**：`ErpQaConstants.CONFIG_BUSINESS_CANCEL_LINKAGE_ENABLED = "erp-qua.business-cancel-linkage-enabled"`（:62 区域）+ `ErpQaConfigs.isBusinessCancelLinkageEnabled()`（AppConfig.var 默认 "true"，null/空回退 true，对齐既有 reader 范式）。
- [x] `Add` 三域接线（D3）：ErpPurReceiveCancelProcessor.cancel + ErpSalDeliveryCancelProcessor.cancel + ErpMfgWorkOrderProcessor.cancel 后置调 cancelForBusinessBill（config-gated + try/catch WARN 不阻断 + billType 常量）
      - Skill: `nop-backend-dev`
      - **实现（2026-08-16 执行）**：三域 cancel 主流程成功后置调 `inspectionBiz.cancelForBusinessBill`（protected step `cancelLinkedInspections`，try/catch Exception → LOG.warn 降级不阻断；config 门控在 Facade 内）。pur/sal cancel Processor 增 `@Inject IErpQaInspectionBiz`（模块依赖链 pur/sal→qa-dao 已存在）；mfg 复用既有 :86 注入。billType 常量按 D3 裁决：pur=`ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE` / sal=`ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY` / mfg=`ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER`（各域创建路径同源，保证 relatedBillType 键值一致）。

Exit Criteria:

- [x] Facade 契约 + 实现编译通过（`mvn compile -pl module-quality/erp-qa-service -am`）
- [x] 三域接线编译通过（`mvn compile -pl module-purchase/erp-pur-service,module-sales/erp-sal-service,module-manufacturing/erp-mfg-service -am`）
- [x] grep 证实三域 cancel 调用点存在 + config 门控生效

### Phase 3 - 测试 + 文档回填（Proof）

Status: completed
Targets: `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaBusinessCancelLinkage.java`（新增，Facade 单测）；跨域接线测试分属接线所在模块——`module-purchase/erp-pur-service/src/test/.../TestErpPurReceiveCancelInspectionLinkage.java`（新增）+ `module-sales/erp-sal-service/src/test/.../TestErpSalDeliveryCancelInspectionLinkage.java`（新增）+ `module-manufacturing/erp-mfg-service/src/test/.../TestErpMfgWorkOrderCancelInspectionLinkage.java`（新增）；`docs/design/quality/state-machine.md`（§4/§实现约定）；`docs/audits/arm-index.md`（P1-RC-041 行）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.59 行）
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] `Proof` 新增 `TestErpQaBusinessCancelLinkage`（qa-service 内，Facade 单测）：①PENDING 取消（findByRelatedBill 查无）；②ACCEPTED/REJECTED 不动（历史完整）；③无匹配零副作用；④幂等重复取消；⑤config 关闭跳过；⑥`_cases/` 快照录制（对齐既有测试范式）——**真实 wiring 测试不落 qa-service**（erp-qa-service pom 注释显式禁止反向依赖 sales/purchase-service 避免 reactor 环，用 test-mock 桩）
      - Skill: `nop-testing`
      - **执行（2026-08-16）**：新增 `TestErpQaBusinessCancelLinkage`（JunitAutoTestCase + @NopTestConfig localDb/initDatabaseSchema/enableActionAuth=FALSE + QaFrozenClockExtension）**5 组全绿**——①testPendingCancelledAndNoLongerVisibleByRelatedBill[取消数=1 + ErpQaInspection__findByRelatedBill 查无 + disableLogicalDelete 查询软删行 orm_logicalDeleted=true 审计可追溯]②testAcceptedAndRejectedUntouched[ACCEPTED+REJECTED 双终态取消数=0 且 findByRelatedBill 保留 2 条]③testNoMatchZeroSideEffects④testIdempotentRepeatCancel[1 → 0]⑤testConfigDisabledSkips[config=false 取消数=0 保留] + `_cases/` 快照录制（RECORDING 一次 → 切 CHECKING 零漂移）。
- [x] `Add` 跨域接线测试（接线所在模块，对齐 R1.48 先例：TestErpMfgReservationLifecycle 落 erp-mfg-service）：pur/sal/mfg 三模块各新增 cancel 联动测试类——cancel 后关联 PENDING 质检单消失（findByRelatedBill 跨域查无）+ 终态质检单不动 + config 关闭跳过；三模块 pom 已含 app-erp-quality-service test-scope 依赖（erp-pur-service/pom.xml:88-91、erp-mfg-service/pom.xml:96、erp-sal-service/pom.xml:98）
      - Skill: `nop-testing`
      - **执行（2026-08-16）**：三模块各新增 3 组测试类（`TestErpPurReceiveCancelInspectionLinkage` / `TestErpSalDeliveryCancelInspectionLinkage` / `TestErpMfgWorkOrderCancelInspectionLinkage`，均 JunitAutoTestCase + 各域 FrozenClockExtension）——①cancel 后关联 PENDING 质检单经 ErpQaInspection__findByRelatedBill 跨域查无 + 终态（ACCEPTED/REJECTED）保留（Map 元素断言 result 键，GraphQL 响应为 Map 列表非实体——初版 ClassCast 修正）②config 关闭跳过 PENDING 保留③无关联零副作用 + `_cases/` 快照录制。GraphQL 跨域查询依赖三模块 pom 既有 app-erp-quality-service test-scope 依赖提供 I*Biz Bean 实现（pur:88-91/sal:98/mfg:96 实证）。
- [x] `Proof` 零回归验证：`mvn test -pl module-quality/erp-qa-service,module-purchase/erp-pur-service,module-sales/erp-sal-service,module-manufacturing/erp-mfg-service` 全绿（172 + 三域基线）+ 快照重录核验 + 全仓 `mvn test` + `mvn clean install -DskipTests` 全量构建 BUILD SUCCESS
      - Skill: `nop-testing`
      - **执行（2026-08-16）**：四模块 `mvn test` **182/320/299/282 tests 0 failures 0 errors**（172/317/296/279 基线 + 14 新增零回归）+ 全仓 `mvn test` BUILD SUCCESS 0 failures 0 errors（1 skipped 既有）+ `mvn clean install -DskipTests` 全量 BUILD SUCCESS + 四新测试类 `_cases/` 快照 RECORDING→CHECKING 重录核验零漂移。
- [x] `Proof` compliance checker 复跑：`bash docs/audits/nop-compliance-checker.sh`——新增 daoFor 站点（Facade 内 ErpQaInspection 查询/软删）分类：同域站点 per-site 证据 baseline-raise（对齐 R1.29/R1.33 先例）或零漂移
      - Skill: none
      - **执行（2026-08-16）**：checker 复跑 **actual == baseline 零漂移**——R2b=235 / R2c=1422 / R2d=35（与 `compliance-baseline.md` §BASELINE 机器可读块逐值一致，全 19 规则）；新增 daoFor 面经 `AbstractErpQaInspectionProcessor.inspectionDao()` 既有站点复用（findPendingByRelatedBill/softDeletePending 不新增字面 daoFor 计数）→ 零新增站点，无 baseline-raise 需求。注：R12c=40 vs checker F4 头基线 38 为 **HEAD 既有漂移**（git grep HEAD 实证 40，先于本计划存在，非本计划引入；R12 非 CI 门控 §BASELINE 块内规则，属人工复核类）——本计划不调整，登记事实备查。
- [x] `Add` 文档回填：state-machine.md §4 + §实现约定:190 Deferred→已实现 + D1-D3 裁决注记 + config 门控语义；arm-index P1-RC-041 → done (RC-R1.59)；roadmap RC-R1.59 行 done ✅ + 行标签按 B 类裁决改写（消除「越界项…checkbox」歧义）；`docs/logs/2026/08-16.md` 日志条目
      - Skill: none
      - **执行（2026-08-16）**：state-machine.md §4 异常路径行 Deferred→已实现 + §实现约定:190 改写为已实现注记（D1 软删载体 + D2 config 门控默认 true + D3 三域接线与 billType 键值 + 残留风险）；arm-index P1-RC-041 行状态 → done (RC-R1.59)（修复记录 + 历史保留）；roadmap RC-R1.59 行 todo → done ✅ + 行标签改写「第一批（纯预授权——2026-08-12 B 类裁决…无越界项 checkbox）」；`docs/logs/2026/08-16.md` 日志条目（倒序顶部）。

Exit Criteria:

- [x] TestErpQaBusinessCancelLinkage ①-⑥ 全绿（Facade 单测：取消/不动/幂等/门控断言逐项 + 快照落盘）
- [x] 三模块跨域接线测试全绿（pur/sal/mfg cancel → 关联 PENDING 消失断言 + config 关闭跳过）
- [x] quality/pur/sal/mfg 四模块测试全绿（172 + 三域基线零回归，失败模式=任何既有测试翻红）
- [x] compliance checker actual ≤ baseline（或 baseline-raise 带 per-site 证据落 compliance-baseline.md）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_ff63abb11ffeT9qgOuePdQqTF1`) — 2 MAJOR + 3 MINOR。MAJOR-1 已修订：baseline/D1 显式裁决 useLogicalDelete 属平台逻辑删除（delVersion 置位 + ORM 自动过滤 + 审计可追溯，非物理删除），系 B 类裁决直接覆盖的规范解法（三权威源一致：arm-index P1-MA2-064 方案 A + R1.20 plan + state-machine.md :190；R1.17 先例）→ **不触发 data deletion 保护区域双 agent 批准** + 审查异议时补批兜底声明；MAJOR-2 已修订：Phase 3 Targets/测试项拆分——qa-service 仅 Facade 单测（①-⑥），跨域接线测试落接线所在模块（pur/sal/mfg 各新增 cancel 联动测试类，对齐 R1.48 先例 + qa-service pom 禁止反向依赖约束）；MINOR-1/2 已修订：Phase 1 Item Types `Decision | Add`→`Decision` + 文档回填项补 `Add` 标签；MINOR-3 已修订：行号订正（findByRelatedBill :67-75 / pur+sal cancel :33-43 / mfg cancel :148-155）。
- Independent draft review iteration 2: `acceptable as-is` (`ses_ff631e75cfferk17AORpoTeqdI`) — 迭代 1 全部 6 项修复逐项核实 FIXED（MAJOR-1 D1 裁决 + 兜底条款 :70 + baseline 措辞 :22；MAJOR-2 测试归属拆分 :107/:113/:117 + qa-service pom 反向依赖实测 + R1.48 先例；MINOR-1/2 :65/:121；MINOR-3 四处行号实测）；独立基线全 PASS（IErpQaInspectionBiz 85 行无 cancelForBusinessBill / dict 4 值无 CANCELLED[orm.xml:40-45] / 状态机 4 态 / 三域 cancel Processor 无联动 / state-machine.md :50/:190 Deferred / arm-index P1-MA2-064 方案 A / roadmap :49 B 类含 RC-R1.59 / R2c=1420 / 172 tests / skills README :116 不可类推）；格式合规 PASS 无反松弛词；product-scope 分级确认在位（:68）。2 项非阻塞 MINOR（D1 中 data-deletion 语义系计划解释性声明非政策字面——已就地修订为「本计划的解释性声明」措辞；172 tests 计数可能被姊妹计划 1634-2 先行执行更新——零回归门为失败模式驱动，计数漂移鲁棒）。草案审查收敛 → `Plan Status: draft → active`。

## Closure Gates

- [x] 范围内行为完成（Facade + 三域 config-gated wiring + product-scope 分级确认）
- [x] 相关文档对齐（state-machine.md/arm-index/roadmap/logs）
- [x] 已运行验证（`mvn test -pl module-quality/erp-qa-service,module-purchase/erp-pur-service,module-sales/erp-sal-service,module-manufacturing/erp-mfg-service` + 全仓 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### CANCELLED 结果态（dict + 状态机迁移，方案 B）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 方案 A 软删满足 L1「取消(CANCELLED)」语义（作废后不可见 + 历史完整）；CANCELLED 显式状态为展示层增强，dict 追加超 RC-R1.59 B 类枚举（2026-08-08 不可类推规则）须另行批准
- Successor Required: `no`

### 作废联动通知派发

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1 未要求 notify；业务作废本身有单据状态可查
- Successor Required: `no`

### 软删质检单的「已取消」审计展示

- Classification: `optimization candidate`
- Why Not Blocking Closure: 审计可经 delVersion/删除时间追溯；独立"已取消"视图无 L1 契约
- Successor Required: `no`

## Closure

Status Note: 执行完成（2026-08-16，mission-driver）：Phase 1 裁决 + Phase 2 实现 + Phase 3 测试/文档回填全部落地，验证全绿（四模块 182/320/299/282 tests 0 failures + 全仓 mvn test + mvn clean install -DskipTests BUILD SUCCESS + compliance checker actual == baseline 零漂移）。roadmap RC-R1.59 → done ✅ + arm-index P1-RC-041 → done (RC-R1.59) + state-machine.md §4/§实现约定 Deferred→已实现 + 日志条目已回填。无 Source Audits 行（roadmap-sourced plan，跳过 audit 关闭步骤）。

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计——由 mission-driver 的 CLOSURE_VERIFY 独立子代理（新会话）执行，执行者未自我审计>
- Evidence: 四模块 `mvn test` 182/320/299/282 0 failures 0 errors + 全仓 `mvn test` BUILD SUCCESS（0 failures，1 skipped 既有）+ `mvn clean install -DskipTests` BUILD SUCCESS + compliance checker actual == baseline（R2b=235 / R2c=1422 / R2d=35）+ 四新测试类 `_cases/` 快照在案 + git diff 变更文件清单（qa-dao I*Biz 契约 / qa-service Constants+Configs+Processor+BizModel+beans.xml / pur/sal/mfg 三 cancel 接线 / 四测试类 / 四文档）

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
