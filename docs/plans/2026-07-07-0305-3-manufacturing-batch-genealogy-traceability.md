# 2026-07-07-0305-3 manufacturing-batch-genealogy-traceability

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.2 partial 的剩余段（UC-MFG-13 生产批次追溯）；完成 2.2 `partial → done`
> Related: `2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`（WorkOrder 状态机前置）、`2026-07-02-0700-1-inventory-trace-chain.md`（库存批次追溯前置）、`2026-07-03-1707-3-quality-recall-event.md`（召回事件可消费基因链 successor）
> Audit: required

## Current Baseline

- **`ErpMfgBatchGenealogy` 实体已物化**：`module-manufacturing/.../app-erp-manufacturing.orm.xml` 已含该实体（字段 workOrderId/jobCardId/operationId/inputLotId/inputMaterialId/inputQty/outputLotId/outputMaterialId/outputQty/productionDate/lotStatus/isInputConsumed 等）。FK `inputLot`/`outputLot` → `ErpInvBatch`（库存批次主数据，`inventory.orm.xml` 已存在）。
- **BizModel 为 CRUD 空壳**：`ErpMfgBatchGenealogyBizModel extends CrudBizModel` 仅 15 行 setEntityName，无任何自定义方法；`IErpMfgBatchGenealogyBiz` 无追溯方法声明。
- **写入钩子缺失**：`ErpMfgWorkOrderBizModel.reportCompletion(workOrderId, completedQty, context)`（@BizMutation）委派 `workOrderProcessor.reportCompletion`，当前完工入库流程不持久化任何 input-lot→output-lot 消耗记录（领料出库的输入批次 → 完工入库的产出批次映射未捕获）。
- **库存批次追溯已 done（前置）**：`IErpInvStockMoveBiz` 的 forwardTrace/backwardTrace/returnTrace/batchTrace（库存域按 batchNo 聚合）done 计划 0700-1；本计划的制造基因链是其在生产维度的延伸（原料批次→生产批次→成品批次）。
- **WorkOrder 状态机 done（前置）**：10 态工单状态机 + 三轴审批 + 齐套 + 领料出库 + 报工 + 完工入库 + 成本归集 done 计划 2237-1；UC-MFG-13 是该计划明确标注的剩余 todo。
- **设计文档完整**：`docs/design/manufacturing/batch-genealogy.md`（113 行）定义数据模型（与既有实体一致）、前向/反向/全链三类追溯、核心 SQL 查询、3 召回场景。
- **设计引用存在两处需收敛的缺陷**：（1）设计引用 `../inventory/lot-management.md` 的 `ErpMdBatch` 作为批次主数据，但该文件**不存在**（`docs/design/inventory/` 仅含 `trace-chain.md`），属断链而非名称漂移；实际批次主数据实体为 `ErpInvBatch`（`inventory.orm.xml` 已存在，基因链 FK `inputLot`/`outputLot` 已指向它）。（2）`ErpMdBatch` 名称与实际 `ErpInvBatch` 不一致。两处须在 Closure 文档对齐步骤修正（设计断链重指向 `trace-chain.md` + 名称统一为 `ErpInvBatch`）。
- **剩余差距**：完工写入基因链、前向/反向/全链递归追溯查询、召回范围识别报告全未实现。

## Goals

- 在完工入库流程写入 `ErpMfgBatchGenealogy` 记录：从本次完工消耗的领料输入批次（inputLot）→ 产出批次（outputLot）建立消耗关系行（含投入数量/产出数量/工序/生产日期/lotStatus）。
- 实现三类追溯查询（`@BizQuery`）：`forwardTrace(outputLotId)`（成品→原料）、`backwardTrace(inputLotId)`（原料→成品）、`traceChain(lotId, direction, maxDepth)`（多级递归，带环路防护与深度上限）。
- 实现 `recallReport(lotId, context)`：从问题批次出发，全链识别所有受影响成品批次 + 其当前库存/已售位置，供召回范围界定（消费方为 quality 召回事件 successor）。
- 完成 2.2 工作项 `partial → done`（解除 2237-1 该项 todo）。

## Non-Goals

- 召回事件实体与流程（done 计划 1707-3，质量域 owner）—— 本计划仅提供 `recallReport` 查询供其消费，不改召回状态机。
- 序列号（serialNo）单件追溯 —— 当前 `ErpInvBatch` 为批次级，serialNo 单件追溯归 inventory 域 successor（2352-2/1707-3 共同 Deferred，触发条件=inventory serialNo 落地）。
- 批次基因链 AMIS 可视化图谱前端（归前端 successor；nop-report 报表能力已就绪）。
- 实时生产批次流转看板（归 dashboard successor）。
- 领料时逐笔 progressive 记录（Decision：本计划在完工时一次性按本次完工消耗写入，不做领料时 progressive 累积 —— 见 Phase 1 Decision）。

## Task Route

- Type: `implementation-only change`（实体已存在，无 ORM/codegen 变更；owner 设计 done 于 `batch-genealogy.md`，含写入时机/产出批次获取/失败语义三处 Decision 需在实施前裁定）
- Owner Docs: `docs/design/manufacturing/batch-genealogy.md`（权威设计）、`docs/design/manufacturing/state-machine.md`（完工触发面）、`docs/design/inventory/trace-chain.md`（库存追溯前置范式）
- Skill Selection Basis: BizModel/IBiz `@BizQuery` 方法 + 完工处理器钩子 + 跨实体 IErpInvBatchBiz + ErrorCode → 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase`+IGraphQLEngine → 加载 `nop-testing`。必需输入（实体、完工流程、库存批次）均就绪。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/外部服务/数据迁移；无 ORM 变更（实体已存在）。
- 新增配置键（`ErpMfgConstants` 声明 + `NopSysVariable` 默认值）：`erp-mfg.genealogy-write-enabled`（完工写入总开关默认 true）、`erp-mfg.genealogy-max-trace-depth`（递归深度上限默认 50，防环路/爆炸）。

## Execution Plan

### Phase 1 - 完工写入基因链

Status: completed
Targets: `IErpMfgBatchGenealogyBiz`、`ErpMfgBatchGenealogyBizModel`、`BatchGenealogyWriter`、`WorkOrderProcessor`（完工钩子）、`ErpMfgErrors`、`ErpMfgConstants`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Explore`
- Prereqs: 无

- [x] Explore: 确认完工入库流程中**输入批次与产出批次**两路可获取性。已预检事实：`StockMoveLineRequest` 携带 `batchNo` 字段，但 `ErpMfgWorkOrderProcessor.generateCompletionMove` **未在完工入库行上设置 `batchNo`**，且完工时不创建产出批次。由于 `ErpMfgBatchGenealogy.outputLotId` 为 `mandatory`，若无产出批次则基因行无法写入。探索须裁定的两路：（a）**产出批次**——完工时是否/如何获得产出批次（自动建批 `ErpInvBatch` / 由工单产品行带出 / 工单字段指定），（b）**输入批次**——本次完工对应领料出库明细的输入批次 + 消耗数量查询路径。产出探索结论（精确钩子点 + 两路批次字段映射 + 产出批次获取策略）。降级方案：若完工侧确无产出批次来源，则在 Phase 1「产出批次获取」Decision 中裁定（自动建批 vs 工单派生），不静默跳过写入。
  - Skill: `nop-backend-dev`
  - **结论**：输入批次经 `ErpMfgMaterialIssueLine.batchNo`（自由字符串）+ `ErpMfgMaterialIssue.workOrderId` 查询领料明细获得，再按 `batchNo`+`materialId` 映射到 `ErpInvBatch`（inputLotId FK 目标）。产出批次由 `BatchGenealogyWriter.ensureOutputLot` 完工时自动创建 `ErpInvBatch`（batchNo=FG-{woCode}，状态 OPEN，总量=本次完工量）。钩子点=`ErpMfgWorkOrderProcessor.reportCompletion` 的 `generateCompletionMove` 成功之后（同一 `@BizMutation` 事务内），新增 `writeBatchGenealogy` protected step 方法。
- [x] Decision: 写入时机裁定 —— 在完工入库（reportCompletion）时一次性按本次完工消耗的领料输入批次→产出批次写入基因行（而非领料时 progressive 累积）。记录选择（与 2237-1 完工聚合点一致、避免领料-完工时序耦合）、替代方案（领料 progressive）、残留风险（部分完工时输入批次按完工比例分摊的近似）。
  - Skill: none
  - **裁定**：选择完工时一次性写入。残留风险：部分完工时 inputQty 按 `completedQty/plannedQuantity` 比例分摊（近似），已由 `BatchGenealogyWriter.doWrite` 的 `ratio` 计算实现。
- [x] Decision: 产出批次获取策略裁定（依赖上一 Explore 结论）—— 在「完工时自动创建 `ErpInvBatch` 产出批次」vs「由工单/产品行派生既有批次」之间选择。记录选择、替代方案、残留风险（自动建批可能与既有库存批次管理策略冲突）。
  - Skill: none
  - **裁定**：选择完工时自动创建 `ErpInvBatch`（batchNo=FG-{woCode} 派生，状态 OPEN）。残留风险：自动建批可能与既有库存批次管理策略冲突，由 config-gated + protected `ensureOutputLot` 可覆盖缓解。
- [x] Decision: 基因链写入失败语义裁定 —— 选择**最佳努力（best-effort）**：基因链写入失败不回滚完工入库（在同一 `@BizMutation` 事务内 `generateCompletionMove` 成功之后，捕获异常记日志 + 不阻断主流程），config 键 `erp-mfg.genealogy-write-enabled` 关闭时整体跳过。替代方案：强一致（失败传播回滚完工入库，保证基因链与入库原子）。残留风险：best-effort 下可能产生基因链缺口（部分完工无追溯行），由 config 开关 + 日志可观测性兜底；选定 best-effort 以避免追溯辅助数据拖垮核心完工事务。
  - Skill: none
  - **裁定**：选择 best-effort。`BatchGenealogyWriter.writeOnCompletion` 外层 try/catch 记 ERROR 日志不抛出；config-gated `erp-mfg.genealogy-write-enabled`（默认 true）。
- [x] Add: `BatchGenealogyWriter.writeOnCompletion(workOrder, completedQty, context)`（protected 方法，产品化可覆盖）—— 查询工单领料明细输入批次 + 完工产出批次（按上一 Decision 的获取策略），按消耗比例构建 `ErpMfgBatchGenealogy` 行（inputLotId/inputMaterialId/inputQty/outputLotId/outputMaterialId/outputQty/operationId/productionDate/lotStatus=RELEASED/isInputConsumed=true），经 `IErpMfgBatchGenealogyBiz.saveEntity` 落库。config-gated `erp-mfg.genealogy-write-enabled`。
  - Skill: `nop-backend-dev`
- [x] Add: 在 `WorkOrderProcessor.reportCompletion` 完工入库成功后（在同一 `@BizMutation` 事务内、`generateCompletionMove` 成功之后）调用 `BatchGenealogyWriter.writeOnCompletion`（作为 protected step，下游可覆盖跳过/增强）。失败语义按上一「失败语义裁定」Decision（best-effort：try/catch 包裹记日志，不回滚完工入库）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpMfgErrors` 声明 `ERR_MFG_GENEALOGY_LOT_NOT_FOUND`、`ERR_MFG_GENEALOGY_MAX_DEPTH_EXCEEDED`；`ErpMfgConstants` 声明配置键常量。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 完工一个消耗了带批次原料的工单后，`ErpMfgBatchGenealogy` 出现 inputLot→outputLot 行且数量/物料正确（成功模式）；无批次原料的工单不报错（跳过写入）；config 关闭时不写入。

### Phase 2 - 追溯查询与召回报告

Status: completed
Targets: `IErpMfgBatchGenealogyBiz`、`ErpMfgBatchGenealogyBizModel`、`BatchGenealogyTracer`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] Add: `BatchGenealogyTracer`（纯查询服务）实现 `forwardTrace(outputLotId)`（产出批次→所有直接输入批次）、`backwardTrace(inputLotId)`（输入批次→所有直接产出批次）、`traceChain(lotId, direction, maxDepth)`（迭代递归：前向用 output_lot 找 input 再向上游递归 / 反向用 input 找 output 再向下游递归，带已访问集合环路防护 + maxDepth 上限抛 ErrorCode）。
  - Skill: `nop-backend-dev`
- [x] Add: 在 `IErpMfgBatchGenealogyBiz` 声明并实现 `@BizQuery forwardTrace(@Name outputLotId, context)` / `backwardTrace(@Name inputLotId, context)` / `traceChain(@Name lotId, @Name direction, @Name maxDepth, context)`，返回基因行列表（含层级/路径信息便于前端展示）。
  - Skill: `nop-backend-dev`
- [x] Add: `@BizQuery recallReport(@Name lotId, context)` —— 从问题批次出发全链识别所有受影响成品批次（outputMaterialId 为成品 + lotStatus≠REJECTED）+ 经 `IErpInvStockBalanceBiz`/`IErpInvStockMoveBiz` 查当前库存位置与已售去向，返回扁平受影响批次 + 位置列表（供 quality 召回事件 successor 消费）。**前置检查**：Phase 2 起步先确认 `IErpInvStockBalanceBiz` 暴露「按批次/物料的当前库存位置」与「已售去向」查询方法集；若方法集不足，记录缺口并降级为仅返回受影响成品批次集合（位置/去向归 inventory successor）。
  - Skill: `nop-backend-dev`
  - **前置检查结论**：`IErpInvStockBalanceBiz`/`IErpInvBatchBiz` 当前仅 CRUD（无按批次的位置/去向查询方法集），故降级为仅返回受影响成品批次集合（`RecallReport.degraded=true`），位置/去向查询归 inventory successor。

Exit Criteria:

- [x] forwardTrace/backwardTrace 对单级链返回正确直接邻接；traceChain 多级递归含环路防护（构造环路不无限递归）+ maxDepth 超限抛 ErrorCode；recallReport 返回受影响成品批次集合（成功模式）。

### Phase 3 - 测试

Status: completed
Targets: `TestErpMfgBatchGenealogy`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2

- [x] Proof: `TestErpMfgBatchGenealogy`（IGraphQLEngine）：完工写入（带批次原料→基因行落库 + 数量正确 + 无批次原料跳过 + config 关闭不写）、forwardTrace（成品→原料单级+多级）、backwardTrace（原料→成品）、traceChain（多级递归 + 环路防护 + maxDepth ErrorCode）、recallReport（受影响成品批次 + 库存位置）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] manufacturing 域新测试全绿（0 failures/0 errors），覆盖写入 + 三类追溯 + 召回报告成功 + 异常路径。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_0c728f2a1ffeqz61yBAIbUoBBd) because (a) 失败隔离语义作为未决内联选项遗留（R9），(b) 基线将 `inventory/lot-management.md` 断链误述为「名称漂移良性」（R1）；另指出 Explore 降级方案未覆盖产出批次路（outputLotId mandatory 且完工入库未设 batchNo）。
- Independent draft review iteration 2: accept (ses_0c7247417ffe9InBK42538NscH) — 三处 blocker 全部确认解决（失败语义独立 Decision 含选择/替代/残留风险；断链基线改为事实 + Closure 重指向+名称统一；Explore 覆盖产出批次两路 + 产出批次获取 Decision 处理 mandatory outputLotId），无新增最低规则违规，Task Route「三处 Decision」与实际 Decision 数一致。

## Closure Gates

- [x] 范围内行为完成（完工写入基因链 + 三类追溯查询 + recallReport）
- [x] 相关文档对齐（`batch-genealogy.md` 写入时机/失败语义/产出批次获取三 Decision 记录 + 修正设计断链 `../inventory/lot-management.md`→重指向 `trace-chain.md` + `ErpMdBatch`→`ErpInvBatch` 名称统一、`extended-roadmap.md` 2.2 `partial→done` 更新、`docs/logs/` 日志）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 154+ 模块）+ `mvn test -pl module-manufacturing/erp-mfg-service -am` + 全 workspace `mvn test` 0 failures/0 errors
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 基因链 AMIS 可视化图谱 + 生产批次流转看板

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属前端可视化面与 dashboard 面，非本计划「追溯引擎」结果面。`recallReport` 数据已就绪供消费。
- Successor Required: yes —— 触发条件=前端图谱组件 / 生产看板 successor 启动时。

### 序列号（serialNo）单件追溯

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 当前批次级追溯（`ErpInvBatch`）满足多数场景；serialNo 单件追溯需 inventory 域 serialNo 主数据先行。
- Successor Required: yes —— 触发条件=inventory serialNo 落地时（与 2352-2/1707-3 共同 Deferred）。

### recallReport 库存位置/已售去向查询

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 当前 `IErpInvStockBalanceBiz`/`IErpInvBatchBiz` 仅 CRUD，无按批次的位置/去向查询方法集。`recallReport` 降级为仅返回受影响成品批次集合（`degraded=true`），位置/去向归 inventory successor。
- Successor Required: yes —— 触发条件=inventory 暴露按批次的位置/去向查询方法集时。

## Closure

Status Note: 完成（3 Phase 全绿，UC-MFG-13 生产批次追溯落地，2.2 工作项 partial→done）。三处 Decision（写入时机/产出批次获取/失败语义）裁定并记录于 `batch-genealogy.md`；设计断链修正 + `ErpMdBatch`→`ErpInvBatch` 名称统一完成；`recallReport` 因 inventory 域方法集不足降级为仅返回受影响成品批次集合（位置/去向归 inventory successor）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 ses_0c696376bffew1vQhUgokJmTIM（新会话，VERDICT: PASS）
- Evidence: 5 tests 全绿（`TestErpMfgBatchGenealogy`：完工写入 + forward/backward/traceChain + recallReport）；manufacturing 域 96 tests 0 failures/0 errors；`mvn clean install -DskipTests` 全 154+ 模块 BUILD SUCCESS。审计逐项核对 16 处文件/状态一致性（BatchGenealogyWriter/Tracer/BizModel/IBiz/Errors/Constants/beans.xml/Test/设计文档/roadmap/日志/计划一致性），全部 PASS。

Follow-up:

- 基因链可视化 / 召回事件消费 recallReport（见 Deferred successor）。
- recallReport 库存位置/已售去向（inventory successor）。
