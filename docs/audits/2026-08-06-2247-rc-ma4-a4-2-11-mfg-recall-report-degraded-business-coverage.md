# MA4 A4.2.11 运行时验证报告 — 召回报告 degraded 模式运行时业务覆盖确认（A1.11 SP-4，P1-RC-010 协同）

> Audit Status: closed
> Mission: requirement-compliance
> Work Item: A4.2.11（A1.11 SP-4：MA4 运行时行为验证 — 召回报告 degraded 模式运行时业务覆盖，`ErpMfgBatchGenealogyBizModel.recallReport` degraded=true 仅返回受影响成品批次集合，实际召回场景下"受影响成品批次集合"是否满足运营召回需求 + 位置/去向查询缺失的业务影响）
> Source: `docs/plans/2026-08-06-2247-1-rc-ma4-a4-2-11-mfg-recall-report-degraded-business-coverage.md`；存疑点来源 `docs/audits/2026-08-02-2245-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md` §7 SP-4
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.11 实体行）、`docs/plans/2026-08-06-2025-3-rc-ma4-a4-2-9-mfg-batch-genealogy-write-failure-observability.md`（范式参照 + 同源 slice A1.11，SP-2 可观测性维度 done；本计划评 SP-4 业务覆盖维度，与 SP-2 不同控制点不重复）、`docs/audits/arm-index.md`（P1-RC-010 finding 行）
> Verdict: ✅(接受，业务覆盖维度) — **维持 P1-RC-010**（测试补充义务，归 MR1）+ **业务覆盖维度接受**（位置/去向 = inventory 域 successor watch-only，不新建 finding）。运行时确认：`recallReport` 反向递归（BFS backwardTrace + visited 防环）**完整覆盖**所有可达产出成品批次（多级递归 + 多分支链无漏召回 + 无 maxDepth 截断）+ `degraded=true` 结构性恒置**如实反映**位置/去向缺失现状（不掩盖反向递归不完整——因递归完整）+ inventory 域位置/去向查询方法集**零暴露**（successor 触发条件未满足）+ L1 UC-MFG-13 ⑫仅要求"识别"（降级版满足）。命中裁决分支①。

---

## 0. 执行摘要

本验证是**只读运行时业务覆盖评估**（无代码/ORM/api.xml/view.xml/真相源变更），结果表面 = 本报告 + arm-index `P1-RC-010` 运行时业务覆盖确认注记。范式对齐 A4.2.9（done — 同源 slice A1.11 best-effort 路径运行时探针先例，不同维度[可观测性 vs 业务覆盖]）。

**核心问题**（A1.11 §7 SP-4）：`RecallReport.degraded=true` 仅返回受影响成品批次集合，实际召回场景下"受影响成品批次集合"是否满足运营召回需求（位置/去向查询缺失的业务影响）？反向递归是否漏召回？

**裁决结论（先行）**：**命中裁决分支①** —— 维持 P1-RC-010（测试补充义务归 MR1）+ 业务覆盖维度接受（位置/去向 = inventory 域 successor watch-only，不新建 finding）。

| 运行时维度 | 现状 | 证据（file:line） | 业务覆盖裁决 |
|---|---|---|---|
| 反向递归完整性 | **完整覆盖** —— BFS backwardTrace + visited 防环 + 无 maxDepth 截断，多级递归 + 多分支链均可达 | `ErpMfgBatchGenealogyBizModel.java:89-105`（BFS）+ `:98`（visited.add 防环）+ `BatchGenealogyTracer.backwardTrace:53-58` | ✅ 无漏召回 |
| degraded 标记语义 | **如实反映位置/去向缺失** —— 结构性恒置（inventory successor），不掩盖反向递归不完整（因递归完整） | `ErpMfgBatchGenealogyBizModel.java:78` + `RecallReport.java:12-14` Javadoc | ✅ 语义诚实 |
| inventory 域位置/去向方法集 | **零暴露** —— inventory 域仅 CRUD，无按批次的位置/已售去向查询方法集 | grep position/whereabouts/locationOf/whereIs/batchLocation 跨 module-inventory/*.java 零命中 | ✅ successor 未触发 |
| L1 需求契约范围 | **仅要求"识别"** —— UC-MFG-13 ⑫ 字面「识别所有受影响成品批次」，不含位置/去向 | `use-cases.md:248` | ✅ 降级版满足 |

**P1-RC-010 协同结论**：P1-RC-010（testRecallReport 仅冒烟 + best-effort 写失败路径无测试）是**测试补充**维度（§2 P1⑤），A4.2.9（catch 分支运行时可观测性）是**可观测性**维度（§2 P2②），本验证是**业务覆盖**维度（SP-4）—— 三者不同控制点不同维度互补不重复。运行时确认反向递归完整 + degraded 如实反映位置/去向缺失 + L1 仅要求"识别" → **维持 P1-RC-010**（测试补充义务归 MR1）+ **业务覆盖维度接受**（位置/去向 = inventory 域 successor watch-only，登记不新建 finding）。

---

## 1. recallReport 反向递归完整性 census（SP-4 核心）

**对象**：`ErpMfgBatchGenealogyBizModel.recallReport`（`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/ErpMfgBatchGenealogyBizModel.java:72-107`）。

### 1.1 入口与降级标记

```java
// :72
public RecallReport recallReport(@Name("lotId") Long lotId, IServiceContext context) {
    requireLot(lotId, context);                          // :73  lot 存在性守卫
    RecallReport report = new RecallReport();
    report.setSourceLotId(lotId);                        // :75
    // 降级标记：当前 inventory 域未暴露按批次的库存位置/已售去向查询方法集，  // :76-77
    // 仅返回受影响成品批次集合（位置/去向归 inventory successor）。
    report.setDegraded(true);                            // :78  结构性恒置

    Set<Long> visited = new HashSet<>();                 // :80
    visited.add(lotId);                                  // :81  起始防环

    // 起始批次自身可能是受影响成品批次                          // :83
    collectAffectedIfFinishedGood(lotId, report);        // :84
```

### 1.2 BFS 反向递归核心循环

```java
    // 反向递归（lotId 作为输入或中间品）找出所有下游产出批次（成品）  // :86
    List<Long> frontier = new ArrayList<>();             // :87
    frontier.add(lotId);                                 // :88
    while (!frontier.isEmpty()) {                        // :89  BFS 主循环
        List<Long> nextFrontier = new ArrayList<>();     // :90
        for (Long currentLot : frontier) {               // :91
            List<ErpMfgBatchGenealogy> edges = batchGenealogyTracer.backwardTrace(currentLot);  // :92
            for (ErpMfgBatchGenealogy edge : edges) {    // :93
                Long outputLotId = edge.getOutputLotId();// :94  下游产出批次
                if (outputLotId == null) {               // :95-97  null 守卫
                    continue;
                }
                if (visited.add(outputLotId)) {          // :98  防环（已访问则跳过）
                    collectAffectedIfFinishedGood(outputLotId, report);  // :99  加入 affectedLots
                    nextFrontier.add(outputLotId);       // :100  继续 BFS
                }
            }
        }
        frontier = nextFrontier;                         // :104
    }
    return report;                                       // :106
}
```

### 1.3 反向递归完整性逐维度裁决

| 完整性维度 | 实现 | 证据（file:line） | 裁决 |
|---|---|---|---|
| **多级递归**（问题批次→中间半成品→产出成品） | BFS frontier 逐级扩展：每轮 `nextFrontier` 收集本轮所有 outputLot，作为下轮 currentLot 继续查 backwardTrace | `:89-104`（while BFS）+ `:100`（outputLot 入 nextFrontier）+ `:104`（frontier=nextFrontier） | ✅ 完整（无层级上限） |
| **多分支链**（一个 inputLot 经多道工序产出多个 outputLot） | `backwardTrace(currentLot)`（`:92`）查 `inputLotId=currentLot` 的**全部**基因行（`eq("inputLotId", inputLotId)`，`BatchGenealogyTracer:56`），每行 outputLot 均进 nextFrontier | `BatchGenealogyTracer.java:53-58`（findAllByQuery 全集）+ `:93`（for edge : edges 遍历全部） | ✅ 完整（全分支覆盖） |
| **环路防护**（基因链成环） | `visited.add(outputLotId)`（`:98`）返回 false 时跳过（已访问不再入 nextFrontier，不再 collectAffected） | `:80-81`（起始 add）+ `:98`（每节点 add 判重） | ✅ 防环 |
| **maxDepth 截断风险** | recallReport **无 maxDepth 参数/守卫**（对比 `traceChain:65-110` 有 `currentDepth >= depth` 抛 `ERR_MFG_GENEALOGY_MAX_DEPTH_EXCEEDED` at `:89-92`）。recallReport BFS 仅以 `frontier.isEmpty()` 终止（`:89`），运行到所有可达下游产出批次穷尽 | `:89`（while !isEmpty）vs `BatchGenealogyTracer.traceChain:89-92`（有 depth 抛错） | ✅ 无截断（完整穷尽） |
| **起始批次自身** | `collectAffectedIfFinishedGood(lotId, report)`（`:84`）——若问题批次本身是产出（成品），自身也入 affectedLots | `:84` | ✅ 起始自检 |
| **null outputLot 守卫** | `outputLotId == null → continue`（`:95-97`）跳过脏数据边 | `:95-97` | ✅ 健壮 |

### 1.4 collectAffectedIfFinishedGood finished-good 判定

```java
// :109
protected void collectAffectedIfFinishedGood(Long lotId, RecallReport report) {
    ErpInvBatch lot = batchDao().getEntityById(lotId);   // :110
    if (lot == null) { return; }                          // :111  null 守卫
    if (ErpMfgConstants.LOT_STATUS_REJECTED.equals(lot.getStatus())) {  // :115  排除已拒收
        return;
    }
    // 查该批次是否作为某基因行的产出（即被生产出来），视为受影响候选   // :118
    List<ErpMfgBatchGenealogy> outputs = batchGenealogyTracer.forwardTrace(lotId);  // :119  查 outputLotId=lotId
    if (!outputs.isEmpty()) {                             // :120
        RecallReport.AffectedLot affected = ...;          // :121-126  填充 lotId/batchNo/materialId/lotStatus
        report.getAffectedLots().add(affected);           // :126
    }
}
```

**判定逻辑裁决**：`forwardTrace(lotId)`（`BatchGenealogyTracer:46-51`）查 `outputLotId=lotId` 的基因行 —— 即判定 lot 是否**曾被生产**（作为某基因行的产出）。

| 判定场景 | `forwardTrace(lotId)` 结果 | 是否入 affectedLots | 正确性 |
|---|---|---|---|
| BFS 到达的 outputLot（`:99` 传入） | **非空**（至少含刚遍历的 edge，因该 edge.outputLotId==lotId） | **入** | ✅ 正确（所有下游产出批次均识别） |
| 起始 lotId 是产出（成品/半成品） | 非空 | 入 | ✅ 正确 |
| 起始 lotId 是纯原料（从未被生产，仅被消耗） | 空 | 不入 | ✅ 正确（原料非"受影响成品批次"，但其下游成品均经 BFS 识别） |
| 已拒收批次（LOT_STATUS_REJECTED） | — | 不入 | ✅ 正确（召回排除已拒收） |

**保守性说明**：`collectAffectedIfFinishedGood` 将**所有产出批次**（含中间半成品）加入 affectedLots，非严格区分"成品"与"半成品"。这是**保守过包含**（recall 宁多勿少）—— 所有真正的成品批次必然是产出批次的子集，故"识别所有受影响成品批次"（L1 ⑫）必然被满足。区分成品/半成品需依赖物料类型/产品类型主数据，属增强维度非本验证范围。

**完整性结论**：recallReport 反向递归**完整覆盖**所有可达产出成品批次。多级递归（BFS frontier 逐级扩展）+ 多分支链（backwardTrace 返回全集）+ 环路防护（visited.add 判重）+ 无 maxDepth 截断（对比 traceChain 有截断）→ **无漏召回分支**。✅

---

## 2. degraded 标记语义核验

**对象**：`report.setDegraded(true)`（`ErpMfgBatchGenealogyBizModel.java:78`）+ `RecallReport` Javadoc（`RecallReport.java:12-14`）。

### 2.1 degraded 标记语义来源

| 来源 | 位置 | 内容 |
|---|---|---|
| BizModel 注释 | `ErpMfgBatchGenealogyBizModel.java:76-77` | 「降级标记：当前 inventory 域未暴露按批次的库存位置/已售去向查询方法集，仅返回受影响成品批次集合（位置/去向归 inventory successor）」 |
| RecallReport Javadoc | `RecallReport.java:12-14` | 「降级说明：当前 `IErpInvStockBalanceBiz`/`IErpInvStockMoveBiz` 未暴露「按批次的当前库存位置」与「已售去向」查询方法集（仅 CRUD），故 `affectedLots` 仅返回受影响成品批次集合；位置/去向查询归 inventory successor」 |
| owner doc | `batch-genealogy.md:141-143` | 「当前 `IErpInvStockBalanceBiz`/`IErpInvBatchBiz` 仅暴露 CRUD（无按批次的当前库存位置/已售去向查询方法集），故 `recallReport` 降级为仅返回受影响成品批次集合（`RecallReport.degraded=true`）。位置/去向查询归 inventory successor」 |

### 2.2 degraded 是否掩盖其他缺口裁决

| 潜在被掩盖缺口 | 是否被掩盖 | 证据 |
|---|---|---|
| **反向递归不完整（漏召回分支）** | **否** —— 反向递归**完整**（§1），不存在不完整可掩盖 | §1 完整性裁决（BFS + visited + 无 maxDepth） |
| **多级递归 maxDepth 截断** | **否** —— recallReport **无 maxDepth**（§1.3），不存在截断可掩盖 | `:89`（while !isEmpty）vs traceChain 有 depth 抛错 |
| **基因链写失败缺口（best-effort）** | **否（不同控制点）** —— degraded 反映位置/去向缺失，**不反映**基因链写入是否成功。基因链缺口致漏报是 A4.2.9（SP-2 可观测性维度）的裁决范围，degraded 不覆盖亦不应覆盖（语义边界正确） | A4.2.9 §3.2（degraded=true 结构性恒置不反映缺口，归 A4.2.9 residual gap） |

**语义诚实性结论**：`degraded=true` 结构性恒置**如实反映**位置/去向查询缺失现状（inventory 域未暴露方法集）。不掩盖反向递归不完整（因递归完整）。不反映基因链写失败缺口（属 A4.2.9 不同控制点，语义边界正确）。✅

---

## 3. inventory 域位置/去向查询方法集 census

**对象**：module-inventory（`erp-inv-service` + `erp-inv-dao`）是否暴露按批次的当前库存位置/已售去向查询方法集。

### 3.1 grep census

| grep 模式 | 范围 | 命中 |
|---|---|---|
| `position\|whereabouts\|locationOf\|whereIs\|batchLocation` | module-inventory/**/*.java | **0** |
| `当前位置\|已售去向\|库存位置` | module-inventory/**/*.java | **0** |

### 3.2 inventory 域 I*Biz 接口现状

| I*Biz 接口 | 暴露方法 | 位置/去向查询 |
|---|---|---|
| `IErpInvStockBalanceBiz` | CRUD（owner doc `batch-genealogy.md:143` 明示「仅 CRUD」） | **无** |
| `IErpInvBatchBiz` | CRUD（owner doc `batch-genealogy.md:143`） | **无** |
| `IErpInvStockMoveBiz` | CRUD | **无** |

**census 结论**：inventory 域当前**未暴露**按批次的当前库存位置/已售去向查询方法集（仅 CRUD）。degraded 标记的 successor 触发条件（inventory 域暴露按批次位置/去向方法集时）**当前未满足**。✅ degraded=true 如实反映现状。

---

## 4. L1 需求契约范围核验

**L1 真相源**（`docs/design/manufacturing/use-cases.md` UC-MFG-13 ⑫，`:248`）逐字：

```
召回报告：从问题批次出发识别所有受影响成品批次
```

**L1 范围裁决**：
- L1 ⑫ 字面仅要求**"识别"**（identify）所有受影响成品批次。
- L1 ⑫ **不含**位置（position）/去向（whereabouts）查询要求。
- 位置/去向查询出现在 owner doc `batch-genealogy.md §场景 1 :85-91`「确定召回范围」的**增强语义**（`场景 1:90`「识别所有受影响成品批次 → 确定召回范围」），非 L1 验收标准原文。

**降级版满足性裁决**：recallReport 降级版（返回受影响成品批次集合 + degraded=true + 无位置/去向）：
- ✅ **满足"识别"**：反向递归完整识别所有可达产出成品批次（§1）。
- ✅ 位置/去向为**增强 successor**（非 L1 验收标准），归 inventory 域能力演进触发（§3 successor 未触发）。

**与 A1.11 §5.3 裁决一致**：A1.11 §5.3 已裁决「UC-MFG-13 ⑫ 功能 = 接受（降级版满足 L1"识别受影响成品批次"；位置/去向查询为增强 successor，归 inventory 域能力演进触发，不构成本切片 P1/P2 finding）」。本运行时验证**闭合** SP-4，确认该裁决的运行时业务覆盖维度（反向递归完整 + degraded 如实 + successor 未触发）。✅

---

## 5. P1-RC-010 测试补充协同声明 + MA4↔A5.6 边界

### 5.1 P1-RC-010 / A4.2.9 / 本验证 三者协同

| 关注点 | 控制点 | 维度 | 归属 |
|---|---|---|---|
| **P1-RC-010**（A1.11 §5） | `testRecallReport` 仅冒烟（未断言 affectedLots 内容/degraded/sourceLotId）+ best-effort 写失败路径无测试 | **测试断言强度**（§2 P1⑤） | MR1 纯测试补充预授权 |
| **A4.2.9**（SP-2，done） | catch 分支可观测性（无 notify/失败标记）+ LOG 无监控采集 + 基因链缺口致召回报告静默漏报 | **运行时可观测性**（§2 P2②） | residual observability gap watch-only（归 MR1） |
| **本验证**（A4.2.11 SP-4） | recallReport 反向递归完整性 + degraded 标记语义 + 位置/去向缺失业务影响 | **运行时业务覆盖**（SP-4） | 业务覆盖维度接受（位置/去向 successor watch-only） |

**协同结论**：三者**不同控制点不同维度互补不重复**：
- P1-RC-010 评**测试断言强度**（testRecallReport 仅冒烟）—— 修复 = MR1 测试补充。
- A4.2.9 评**运行时可观测性**（catch 分支无 notify/metrics）—— 修复 = MR1 catch 可观测性增强。
- 本验证评**运行时业务覆盖**（反向递归是否漏召回 + degraded 是否如实 + 位置/去向缺失是否阻塞 L1）—— 结论 = 接受（递归完整 + degraded 如实 + L1 仅要求"识别"）。

本验证**不撤销/不降级** P1-RC-010 的 P1 测试补充义务（A1.11 §5 已裁决 §2 P1⑤）；本验证**不撤销/不降级** A4.2.9 的 residual observability gap（A4.2.9 已裁决 §2 P2②）。三者均归 MR1，修复时协同（P1-RC-010 测试补充会断言 affectedLots 内容/degraded/sourceLotId，与本验证的反向递归完整性结论对齐）。

### 5.2 MA4 ↔ A5.6 边界

本验证审「行为是否符合需求」（召回报告业务覆盖是否符合 UC-MFG-13 ⑫"识别所有受影响成品批次"），与 A5.6 审「E2E 断言强度」边界按此执行。本验证**不重做** A5.6 E2E 断言强度审计（A5.6 done，`2026-07-29-1430-arm-ma5-e2e-effectiveness.md`）。本验证证据为 main 源码 grep + recallReport/backwardTrace/collectAffectedIfFinishedGood 路径推理 + inventory 域方法集 census（只读），非 E2E 注入重现（真实召回事件重现属运营范围，见 plan §Non-Goals）。

---

## 6. 业务覆盖裁决（方法论 §2 判据 + 三源对照）

**决策框架**（plan §Goals Phase 2 Decision）三分支：

| 分支 | 条件 | 裁决 |
|---|---|---|
| ① | 受影响成品批次集合完整（backwardTrace BFS + visited 防环覆盖所有可达产出成品）+ degraded 如实反映位置/去向缺失 + L1 仅要求"识别" | → 维持 P1-RC-010（测试补充义务）+ 业务覆盖维度接受（位置/去向 = inventory successor watch-only） |
| ② | 反向递归不完整（漏召回分支） | → 登记 finding（按 §2 判据分级，若致活跃召回漏报可能升 P1/P0） |
| ③ | degraded 恒置掩盖其他缺口 | → 登记 finding |

**本验证命中**：**分支①**：
- 反向递归**完整**（§1，BFS + visited + 无 maxDepth，多级/多分支/防环全覆盖）→ 满足①「受影响成品批次集合完整」。
- degraded **如实反映**位置/去向缺失（§2，不掩盖递归不完整——因递归完整）→ 满足①「degraded 如实反映位置/去向缺失」。
- L1 ⑫**仅要求"识别"**（§4，降级版满足）→ 满足①「L1 仅要求"识别"」。
- 分支②**不适用**（反向递归完整，无漏召回）。
- 分支③**不适用**（degraded 不掩盖其他缺口，§2）。

**§2 判据对照**：
- **§2 接受（符合需求契约）**：**成立** —— L1 UC-MFG-13 ⑫全部验收标准（"识别所有受影响成品批次"）在 L3（recallReport 反向递归完整）有证据且一致。业务覆盖维度 = 接受。
- **§2 P1⑤（测试断言完全缺失或仅冒烟）**：**成立（P1-RC-010 维持）** —— testRecallReport 仅冒烟（A1.11 §5 已裁决）。本验证**不撤销**此项（测试补充义务归 MR1）。
- **§2 P0①/③/④（活跃数据破坏/核心循环断裂/会计过账正确性）**：**不成立** —— 召回报告属质量追溯辅助功能（非会计正确性/核心循环/活跃数据破坏防护）；位置/去向缺失是增强 successor 非活跃数据破坏；反向递归完整无漏报。

**L1/L2/L3 三源对照**：
- **L1**（`use-cases.md:248` UC-MFG-13 ⑫）：「召回报告：从问题批次出发识别**所有**受影响成品批次」。降级版反向递归完整识别所有可达产出成品 → 满足"识别"。位置/去向不在 L1 字面。→ 业务覆盖接受。
- **L2**（`batch-genealogy.md:141-143`）：显式标注 degraded=true + 位置/去向归 inventory successor（触发条件=inventory 暴露方法集时）。L2 降级说明与 L1 ⑫不冲突（L1 仅要求"识别"，降级版满足）。§4 三判据（A1.11 §5.3 已核验）：因 L1 ⑫字面仅要求"识别"，位置/去向为增强 successor → 不需要 documented simplification 裁决，L2 登记 successor 触发条件即合规。
- **L3**（实测）：recallReport 反向递归完整（§1）+ degraded 如实反映位置/去向缺失（§2）+ inventory 域位置/去向方法集零暴露（§3）→ 业务覆盖维度运行时确认。

**与 A1.11 §5 P1-RC-010 裁决分层一致**：A1.11 §5 裁决 UC-MFG-13 ⑫功能 = 接受（降级版满足 L1"识别"）+ P1-RC-010（测试有效性 §2 P1⑤）。本验证**闭合** SP-4 运行时业务覆盖确认：**维持** P1-RC-010（测试补充义务不撤销）+ **业务覆盖维度接受**（位置/去向 = inventory successor watch-only，不新建 finding）。与 A1.11 §5 裁决分层一致（功能接受维持 + 测试 P1 维持 + 运行时业务覆盖维度追加接受）。

**裁决**：**命中分支①** —— 维持 P1-RC-010（测试补充义务归 MR1）+ 业务覆盖维度接受（位置/去向 = inventory 域 successor watch-only，不新建 finding）。

---

## 7. finding/注记衔接

- **P1-RC-010 维持 P1**（本验证结论，测试补充义务不撤销）：arm-index `P1-RC-010` 行追加运行时业务覆盖确认注记（反向递归完整覆盖 + degraded 如实反映位置/去向缺失 + inventory 域方法集零暴露 successor 未触发 + L1 仅要求"识别"降级版满足）。
- **业务覆盖维度接受**（不新建 finding）：位置/去向 = inventory 域 successor watch-only，登记 successor 触发条件（inventory 域暴露按批次位置/去向方法集时增强 recallReport）。属跨域 inventory Facade 扩展，纯 BizModel 读侧 Facade 预授权不触 §5 ask-first。
- **不触发 MR0**（无 P0）—— 召回报告属质量追溯辅助功能非活跃数据破坏/会计正确性/核心循环；反向递归完整无漏报；位置/去向为增强 successor。
- **MR1 修复方向**（本验证指导，不实施）：
  - **测试补充**（P1-RC-010）：testRecallReport 强化断言（affectedLots 含 lotB/lotC + degraded=true + sourceLotId=lotA + lotStatus）。纯测试预授权。
  - **位置/去向增强**（successor）：inventory 域演进暴露按批次位置/去向查询方法集时增强 recallReport（跨域 Facade 扩展，纯读侧预授权不触 ask-first）。
- **与既有 finding 交叉去重声明**：
  - **P1-RC-010**（arm-index）：本验证是其运行时业务覆盖裁决的输入，**不新建** finding，仅追加注记（维持 P1 测试补充义务 + 业务覆盖维度接受）。
  - **A1.11 §5/§7**（源报告）：本验证闭合其 SP-4，结论与 §5 P1-RC-010 裁决 + §5.3 降级满足 L1"识别"裁决分层一致（不撤销/不降级/不升级）。
  - **A4.2.9**（SP-2 可观测性，done）：本验证与其不同维度（业务覆盖 vs 可观测性），不同控制点（反向递归完整性 vs catch 分支可观测性），互补不重复。A4.2.9 的 residual observability gap 维持归 MR1。
  - **MA4 ↔ A5.6 边界**：本验证审行为符合性，不重做 A5.6 E2E 断言强度（§5.2）。

---

## 8. 过程纪律自检

### 8.1 nop-compliance-checker.sh 实测

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，本计划**无生产代码变更**故**无回归风险**）：

| 规则 | baseline | actual（本次） | 状态 |
|------|---------|--------|------|
| R1a dao().saveEntity (BizModel) | 0 | 0 | ✅ |
| R1b dao().updateEntity (BizModel) | 0 | 0 | ✅ |
| R1c dao().getEntityById (BizModel) | 0 | 0 | ✅ |
| R1d dao().findAllByQuery (BizModel) | 14 | 14 | ✅ |
| R2a BizModel daoFor(ErpMd*) | 34 | 34 | ✅ |
| R2b BizModel daoFor(Erp*) 跨域 | 229 | 229 | ✅ |
| R2c 全生产代码 daoFor() 总量 | 1382 | 1382 | ✅ |
| R2d Processor daoFor(ErpMd*) | 34 | 34 | ✅ |
| R3~R12c（R3/R4/R5/R6/R7/R8/R10/R11/R12a/R12b/R12c） | 5/0/0/2/0/0/6/0/69/66/40 | = baseline | ✅（零代码变更，按构造一致） |

**说明**：本计划是只读评估，**零生产代码变更**（无 .java/.orm.xml/.api.xml/view.xml 修改），checker 命中均为基线既有项，**无本计划引入的漂移**（R1a-R2d 经 checker 本次实测逐一确认匹配 baseline；R3~R12c 因零代码变更与 baseline 按构造一致，对齐 A4.2.9 done 实测值）。按 plan §Closure Gates，**不以 checker 退出码 0 作为门控依据**（checker 是纯 reporter，本计划无代码变更故无回归风险）。

### 8.2 独立性声明

- 本报告执行者 = 计划执行代理（非草案审查者）。计划草案审查已由独立子代理 ses_028706024ffedVF8mBevGTrVW6 完成（fresh session，Draft Review Record iter-1 accept，全 10 checklist 项 PASS，零 Blocker）。
- **结束审计**将由独立子代理（新会话）执行（plan §Closure Gates 最后一项），执行者**不自我审计**。

### 8.3 §8 自检清单

| # | 项 | 状态 | 证据 |
|---|---|---|---|
| 1 | 需求正确性（对照 L1 UC-MFG-13 ⑫） | ✅ | §4/§6 三源对照，L1 仅要求"识别"降级版满足 + 反向递归完整无漏召回 |
| 2 | owner-doc 对齐（manufacturing/） | ✅ | §2/§4 引用 batch-genealogy.md / use-cases.md（UC-MFG-13）+ RecallReport Javadoc |
| 3 | 架构/边界影响 | ✅ | 只读评估，无跨模块依赖变更；MA4↔A5.6 边界声明 §5.2；inventory successor 不触 ask-first |
| 4 | 验证充分性 | ✅ | 反向递归 5 完整性维度 + collectAffectedIfFinishedGood 4 判定场景 + degraded 3 掩盖裁决 + inventory 4 grep/I*Biz + L1 范围，全 file:line 证据 |
| 5 | 回归风险 | ✅ | 零代码变更，无回归 |
| 6 | 路由/技能选择 | ✅ | Skill: multi-dimensional-audit-prompt.md（plan 指定），维度覆盖齐全（7 默认维度 + 反向递归完整性/degraded 语义/inventory successor/P1-RC-010 协同） |
| 7 | 范围漂移 | ✅ | 不重审 P1-RC-010 P1 定级本身（仅评业务覆盖差异）；不实施修复；不展开 SP-1/SP-2/SP-3（归独立工作项 done） |
| 8 | 与 arm-index 交叉去重 | ✅ | §7 声明，P1-RC-010 追加注记非新建；业务覆盖接受不新建 arm-index finding 行 |

---

## 9. 结论

**整体 Verdict**：✅(接受，业务覆盖维度) — **命中裁决分支①**：维持 P1-RC-010（测试补充义务归 MR1）+ 业务覆盖维度接受（位置/去向 = inventory 域 successor watch-only，不新建 finding）。

- **recallReport 反向递归完整性 census**（SP-4 核心）：recallReport（`ErpMfgBatchGenealogyBizModel:89-105`）BFS backwardTrace + visited 防环 + **无 maxDepth 截断**（对比 traceChain 有截断）→ **完整覆盖**所有可达产出成品批次。多级递归（frontier 逐级扩展）+ 多分支链（backwardTrace 返回全集）+ 环路防护（visited.add 判重）→ **无漏召回分支**。
- **degraded 标记语义核验**：`degraded=true`（`:78`）结构性恒置**如实反映**位置/去向缺失现状（inventory successor），**不掩盖**反向递归不完整（因递归完整），**不反映**基因链写失败缺口（属 A4.2.9 不同控制点，语义边界正确）。
- **inventory 域位置/去向方法集 census**：module-inventory **零暴露**按批次的位置/已售去向查询方法集（grep position/whereabouts/locationOf/whereIs/batchLocation 零命中 + I*Biz 仅 CRUD）→ successor 触发条件**未满足**，degraded=true 如实反映现状。
- **L1 需求契约范围**：UC-MFG-13 ⑫字面仅要求**"识别"**（不含位置/去向），降级版反向递归完整识别所有可达产出成品 → **满足 L1"识别"**。位置/去向为增强 successor 归 inventory 域演进。

本验证**闭合** A1.11 §7 SP-4 运行时业务覆盖裁决，与 A1.11 §5 P1-RC-010 裁决 + §5.3 降级满足 L1"识别"裁决分层一致（功能接受维持 + 测试 P1 维持 + 运行时业务覆盖维度追加接受）。修复归 MR1：P1-RC-010 测试补充（纯测试预授权）。位置/去向增强归 inventory 域 successor（跨域 Facade 扩展，纯读侧预授权不触 §5 ask-first）。
