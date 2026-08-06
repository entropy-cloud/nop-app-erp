# MA4 A4.2.9 运行时验证报告 — best-effort 基因链写失败运行时缺口可观测性确认（A1.11 SP-2，P1-RC-010 协同）

> Audit Status: closed
> Mission: requirement-compliance
> Work Item: A4.2.9（A1.11 SP-2：MA4 运行时行为验证 — best-effort 基因链写失败运行时缺口可观测性，`BatchGenealogyWriter.writeOnCompletion` try/catch 不阻断完工的运行时可观测性 + 基因链缺口业务影响；与 P1-RC-010[UC-MFG-13 ⑫召回报告测试仅冒烟 + best-effort 写失败路径无测试]测试补充协同）
> Source: `docs/plans/2026-08-06-2025-3-rc-ma4-a4-2-9-mfg-batch-genealogy-write-failure-observability.md`；存疑点来源 `docs/audits/2026-08-02-2245-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md` §7 SP-2 + §5 P1-RC-010 裁决
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.9 实体行）；`docs/audits/2026-08-06-1926-rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime.md`（范式参照：mfg 完工 best-effort 路径运行时探针 + config-gated 裁决）；`docs/audits/arm-index.md`（P1-RC-010 finding 行）
> Verdict: ⚠️(P1) — **维持 P1-RC-010**（测试补充义务，归 MR1）+ **登记 residual observability gap watch-only**（归 MR1）。运行时确认：catch 分支**仅 LOG.error**（无 notify 告警派发、无失败标记持久化、无 metrics/日志告警通道）+ LOG.error **无监控采集通道** + 基因链缺口致召回报告**静默漏报受影响成品批次**且**降级无运营感知**（`degraded=true` 为结构性恒置与缺口无关）+ config `erp-mfg.genealogy-write-enabled` **默认 true**（best-effort 写入路径**默认活跃**，缺口存在于默认部署非仅 config-enable 部署）。命中裁决分支①。

---

## 0. 执行摘要

本验证是**只读运行时行为评估**（无代码/ORM/api.xml/view.xml/真相源变更），结果表面 = 本报告 + arm-index `P1-RC-010` 运行时可观测性确认注记。范式对齐 A4.2.6-8（done — mfg 完工 best-effort 路径[BOM 编辑影响]运行时探针 + config-gated 裁决先例）。

**核心问题**（A1.11 §7 SP-2）：`BatchGenealogyWriter.writeOnCompletion` 的 best-effort try/catch（Decision 3，不阻断完工）在运行时的**可观测性**现状如何？catch 分支被触发频率是否被监控采集？基因链缺口（部分完工无追溯行）对 UC-MFG-13 召回报告的业务影响？config 默认值决定写入路径活跃性？

**裁决结论（先行）**：**命中裁决分支①** —— 维持 P1-RC-010（测试补充义务）+ 登记 residual observability gap watch-only（归 MR1）。

| 运行时维度 | 现状 | 证据（file:line） | 运营感知 |
|---|---|---|---|
| catch 分支可观测性 | **仅日志**（LOG.error）—— 无 notify 告警派发、无失败标记持久化、无其他通道 | `BatchGenealogyWriter.java:73-75` | **仅 LOG.error**，运营无主动感知通道 |
| LOG.error 监控采集 | **无结构化日志/metrics/日志告警通道** | `BatchGenealogyWriter.java:51,74`（纯 SLF4J LOG.error） | 无监控采集 |
| 基因链缺口 → 召回报告业务影响 | catch 触发 → 该次完工**零/部分基因行** → `recallReport` 反向递归**漏报**下游受影响成品批次 + `degraded=true` 结构性恒置**不反映缺口** | `BatchGenealogyWriter.java:72-74`（catch 吞 doWrite 异常）；`ErpMfgBatchGenealogyBizModel.java:78,89-105`（recallReport 反向递归 + degraded 恒 true） | **静默漏报**，运营无感知（合规风险） |
| config `erp-mfg.genealogy-write-enabled` 默认值 | **true**（默认 on，best-effort 写入路径**默认活跃**） | `BatchGenealogyWriter.java:239-249`（`AppConfig.var(CONFIG, "true")`，null/空/异常均回落 true）；`ErpMfgConstants.java:227-228`；`module-meta.yaml:21`（configKey 声明）；全 application.yaml **零 override** | 默认部署即承担缺口风险 |

**与完工 best-effort 失败可观测性家族对比（A4.2.4 notify 通道家族）**：完工差异过账失败（`ErpMfgWorkOrderReportCompletionProcessor:93-101`）→ LOG.error **+ `dispatchVarianceFailureAlert`**（`ErpMfgWorkOrderProcessor:150-167` 派发 `IErpSysNotificationBiz.notify(...)` 双通道）；完工基因链写失败（`BatchGenealogyWriter:73-75`）→ **仅 LOG.error**（单通道，无 notify）。**同属完工 best-effort 失败家族，可观测性不对称** —— 差异过账失败有告警派发可运营感知，基因链写失败无告警派发运营不可感知。

**P1-RC-010 协同结论**：P1-RC-010（testRecallReport 仅冒烟 + best-effort 写失败路径无测试）是**测试补充**维度（§2 P1⑤），本验证是**运行时可观测性**维度（§2 P2② 可观测性未充分实现）—— 二者不同控制点不同维度互补不重复。运行时确认 catch 分支无监控采集 + 基因链缺口致召回报告降级无运营感知 → **维持 P1-RC-010**（测试补充义务归 MR1）+ **登记 residual observability gap watch-only**（归 MR1：增强 catch 分支可观测性[notify 告警/失败标记]纯 BizModel 预授权不触 §5 ask-first）。

---

## 1. catch 分支可观测性 census（SP-2 核心）

**对象**：`BatchGenealogyWriter.writeOnCompletion`（`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/genealogy/BatchGenealogyWriter.java:64-76`）。

```java
// :64
public void writeOnCompletion(ErpMfgWorkOrder wo, BigDecimal completedQty, IServiceContext context) {
    if (!isWriteEnabled()) {            // :65  config-gated
        return;
    }
    if (completedQty == null || completedQty.signum() <= 0) {  // :68
        return;
    }
    try {
        doWrite(wo, completedQty, context);   // :72
    } catch (Exception e) {
        LOG.error("工单 {} 完工写入批次基因链失败（best-effort，不阻断完工入库）", wo.getCode(), e);  // :74
    }
}
```

**catch 分支（`:73-75`）可观测性逐通道 census**：

| 通道 | 是否存在 | 证据（file:line） |
|---|---|---|
| LOG.error 日志 | **是** | `BatchGenealogyWriter.java:74`（SLF4J `LOG.error(msg, wo.getCode(), e)`，含 workOrderCode + 异常栈） |
| notify 告警派发（`IErpSysNotificationBiz.notify`） | **否** | 全类（272 行）`grep IErpSysNotificationBiz\|notificationBiz\|notify(` **零命中**；类仅注入 `IDaoProvider daoProvider`（`:53-54`），无 notify 字段 |
| 失败标记持久化（基因链缺口标记字段） | **否** | catch 体仅 `LOG.error`，**无任何字段写回** —— 不更新 `ErpMfgWorkOrder` 状态/标记，不写 `ErpMfgBatchGenealogy` 缺口行；`grep setGenealogyStatus\|genealogyGap\|genealogyFailed\|writeFailed` 跨类零命中 |
| metrics / 结构化日志 | **否** | 仅纯 SLF4J `LOG.error`（`:51 LoggerFactory.getLogger`），无 Micrometer/CoreMetrics 计数器、无结构化字段、无 MDC 标记 |
| 其他可观测通道 | **否** | 无事件发布（`grep publish\|EventBus\|fireEvent` 跨类零命中）、无异常工作台登记 |

**判定**：catch 分支可观测性 = **仅日志**（`仅日志`，非`日志+告警`、非`日志+告警+失败标记`）。

---

## 2. LOG.error 监控采集现状 census

**对象**：`LOG.error("工单 {} 完工写入批次基因链失败（best-effort，不阻断完工入库）", wo.getCode(), e)`（`BatchGenealogyWriter.java:74`）。

| 采集维度 | 现状 | 证据 |
|---|---|---|
| 结构化日志（字段化 workOrderCode/errorType 供查询/告警） | **否** —— 纯文本 + SLF4J `{}` 占位，无结构化字段 | `BatchGenealogyWriter.java:74` |
| metrics（计数器/gauge 反映 catch 触发频率） | **否** —— 无 `CoreMetrics`/Micrometer 调用 | 全类无 metrics import |
| 日志告警通道（LOG.error → 告警规则派发） | **否** —— 无项目级日志告警通道接线证据 | `grep genealogy\|基因链` 跨监控/告警配置零命中 |

**结论**：LOG.error **未被监控采集** —— 无结构化日志、无 metrics、无日志告警通道。运营实际感知路径 = **依赖人工巡检 LOG.error 文本**（非主动告警/看板/metrics）。catch 分支被触发频率对运营**不可见**。

**与 A4.2.4 notify 通道家族对比（完工 best-effort 失败可观测性家族）**：

| 完工 best-effort 失败路径 | catch 处理 | notify 告警派发 | 可观测通道数 | 证据 |
|---|---|---|---|---|
| 差异过账失败（`ErpMfgWorkOrderReportCompletionProcessor:93-101`） | LOG.error + `dispatchVarianceFailureAlert` | **是** —— `IErpSysNotificationBiz.notify(NOTIFY_EVENT_VARIANCE_FAILURE, ctx, serviceCtx)` | 2（日志 + 告警） | `ErpMfgWorkOrderProcessor.java:150-167` |
| 基因链写失败（`BatchGenealogyWriter:73-75`） | LOG.error | **否** | 1（仅日志） | `BatchGenealogyWriter.java:73-75` |

**不对称裁决**：同属完工 best-effort 失败家族，差异过账失败有 notify 告警派发（运营可经 `ErpSysNotification` 感知 GL 悬挂），基因链写失败**无 notify 告警派发**（运营不可经通知感知基因链缺口）。两者 catch 语义同为"不阻断完工"，但可观测性配置不对称 —— 基因链侧缺失 A4.2.4 先例已落地的 notify 通道。

---

## 3. 基因链缺口业务影响 census（SP-2 核心）

**问题**：catch 分支触发（`doWrite` 抛异常）后，该次完工的基因行**未写入**（或部分写入后中断），对 UC-MFG-13 召回报告（验收标准⑫"识别所有受影响成品批次"）的业务影响。

### 3.1 基因链缺口产生机制

`doWrite`（`BatchGenealogyWriter.java:80-145`）在 try 内执行：找输出行 → 找带批次领料行 → `ensureOutputLot`（自动建/累加产出批次 `:100`）→ 循环按领料行写 `ErpMfgBatchGenealogy` 行（`:114-144`，逐行 `genealogyDao().saveEntity(row)`）。

- catch 在 `:73` 捕获 `doWrite` 任意异常。若异常发生在 `ensureOutputLot`（`:100`，如 `ErpInvBatch` 锁冲突）或首行 `resolveInputLot` 前 → **该次完工零基因行**；若发生在循环中途（某行 `saveEntity` 抛异常）→ **部分基因行已 flush**（取决于事务边界，但 best-effort 不回滚已写行）。
- **触发条件**（A1.11 §7 SP-2）：领料单带批次 + 完工入库 + 写入异常（如 `ErpInvBatch` 锁冲突 / 数据不一致 / `findBatchByNo` 返回 null 致 NPE 边界）。

### 3.2 召回报告对基因链的依赖 + 降级标记语义

`ErpMfgBatchGenealogyBizModel.recallReport`（`:70-107`）：

```java
// :72-78
public RecallReport recallReport(@Name("lotId") Long lotId, IServiceContext context) {
    requireLot(lotId, context);
    RecallReport report = new RecallReport();
    report.setSourceLotId(lotId);
    // 降级标记：当前 inventory 域未暴露按批次的库存位置/已售去向查询方法集 ...
    report.setDegraded(true);          // :78  结构性恒置（inventory successor），与基因链缺口无关
    ...
    // 反向递归 backwardTrace 找下游产出批次 :89-105
    while (!frontier.isEmpty()) {
        for (Long currentLot : frontier) {
            List<ErpMfgBatchGenealogy> edges = batchGenealogyTracer.backwardTrace(currentLot);  // :92
            for (ErpMfgBatchGenealogy edge : edges) {
                Long outputLotId = edge.getOutputLotId();
                ...
                if (visited.add(outputLotId)) {
                    collectAffectedIfFinishedGood(outputLotId, report);   // :99  加入 affectedLots
                    ...
```

**关键发现**：
1. **recallReport 完全依赖 `ErpMfgBatchGenealogy` 边**（`:92` `backwardTrace(currentLot)` 查 `ErpMfgBatchGenealogy`）。若某次完工因 catch 触发未写基因行，则该完工产出批次（`outputLot`）与输入批次（`inputLot`）之间**无边**，反向递归**无法到达**该产出批次 → `affectedLots` **漏报**该受影响成品批次。
2. **`degraded=true`（`:78`）为结构性恒置**（注释明示"inventory 域未暴露按批次的位置/已售去向查询方法集"），与基因链写入是否成功**无关**。`degraded=true` 不反映基因链缺口，**不能作为缺口运营感知信号**。

### 3.3 业务影响裁决

| 影响维度 | 裁决 | 证据 |
|---|---|---|
| 召回报告降级运营感知 | **无感知** —— 基因链缺口致召回报告 `affectedLots` 静默漏报受影响成品批次；`degraded=true` 结构性恒置不反映缺口；catch 仅 LOG.error 无 notify | `BatchGenealogyWriter:73-75` + `ErpMfgBatchGenealogyBizModel:78,92,99` |
| 召回场景漏报受影响成品批次合规风险 | **存在** —— UC-MFG-13 ⑫"识别**所有**受影响成品批次"，基因链缺口致部分完工产出批次无基因边 → recallReport 反向递归不可达 → **漏报**。无运营感知通道（无告警/失败标记），漏报对运营不可见 | L1 `use-cases.md:248` + §3.2 |

**与 A1.11 §4.4 / §5.3 分层一致**：A1.11 §4.4 已判 recallReport 降级版**满足** L1 ⑫"识别"（降级指位置/去向查询归 inventory successor）。本验证补的差异 = **基因链缺口（catch 触发）下** recallReport **额外漏报**（基因边缺失致反向递归不可达），此为 catch 可观测性缺口下游的**合规风险**，非 recallReport 功能本身缺陷（recallReport 功能经 A1.11 §4.4 接受）。

---

## 4. config `erp-mfg.genealogy-write-enabled` 默认值复核

`isWriteEnabled()`（`BatchGenealogyWriter.java:239-249`）：

```java
// :239
protected boolean isWriteEnabled() {
    try {
        String value = AppConfig.var(ErpMfgConstants.CONFIG_GENEALOGY_WRITE_ENABLED, "true");  // :241  默认 "true"
        if (value == null || value.trim().isEmpty()) {
            return true;                     // :243  null/空 → true
        }
        return Boolean.parseBoolean(value.trim());
    } catch (Exception e) {
        return true;                         // :247  读取异常 → true
    }
}
```

| 源 | 位置 | 值 |
|---|---|---|
| 常量声明 + 注释 | `ErpMfgConstants.java:227-228`（`CONFIG_GENEALOGY_WRITE_ENABLED`，注释「默认 true=完工时写入 input→output 消耗行」） | **true** |
| 消费点 | `BatchGenealogyWriter.java:241`（`AppConfig.var(CONFIG, "true")`；null/空/异常均回落 `true` `:243,:247`） | **true** |
| 模块元数据 | `erp-mfg-meta/module-meta.yaml:21` + `precompile/module-meta.yaml:21` + `_module-meta.json:41`（configKey 声明） | true（owner doc `batch-genealogy.md:135`「默认 true」） |
| 部署 override | `grep "genealogy-write-enabled" *.yaml/*.properties` 全 application.yaml → **零命中**（仅 module-meta.yaml configKey 声明，无 application.yaml override） | — |

**结论**：config 默认 **true**，且 null/空/异常均回落 true → best-effort 写入路径**默认活跃**。与差异计算 config（`erp-mfg.variance-auto-calc-enabled` 默认 **false**，A4.2.6-8 §5）不同：基因链写入默认 on，差异计算默认 off。故**基因链缺口风险存在于默认部署**（非仅 config-enable 部署），裁决分支③（config 默认 off）**不适用**。

---

## 5. P1-RC-010 测试补充协同声明 + MA4↔A5.6 边界

### 5.1 P1-RC-010 协同

| 关注点 | 控制点 | 维度 | 归属 |
|---|---|---|---|
| **P1-RC-010**（A1.11 §5） | `testRecallReport` 仅冒烟（未断言 affectedLots 内容/degraded/sourceLotId）+ best-effort 写失败路径无测试 | **测试断言强度**（§2 P1⑤） | MR1 纯测试补充预授权 |
| **本验证**（A4.2.9 SP-2） | catch 分支可观测性（无 notify/失败标记）+ LOG 无监控采集 + 基因链缺口致召回报告静默漏报 | **运行时可观测性**（§2 P2②） | residual observability gap watch-only（归 MR1） |

**协同结论**：P1-RC-010（测试补充）与本验证（运行时可观测性）**不同控制点不同维度互补不重复**。本验证**不撤销/不降级** P1-RC-010 的 P1 测试补充义务（A1.11 §5 已裁决 §2 P1⑤）；本验证登记的 residual observability gap 是 catch 分支**运行时可观测性**维度（§2 P2②），与测试断言强度独立。二者均归 MR1，修复时协同：
- **P1-RC-010 测试补充**（MR1）：(1) testRecallReport 强化断言（affectedLots 含 lotB/lotC + degraded=true + sourceLotId=lotA + lotStatus）；(2) best-effort 写失败测试（mock `doWrite` 抛异常 → 断言 `reportCompletion` 仍成功 + LOG.error 含 workOrderCode）。纯测试预授权不触 §5 ask-first。
- **residual observability gap 修复**（MR1）：增强 catch 分支可观测性 —— 加 notify 告警派发（对齐 A4.2.4 `dispatchVarianceFailureAlert` 范式）+ 可选失败标记持久化。纯 BizModel 代码逻辑预授权不触 §5 ask-first（不触及 ORM 结构，因不新增持久化字段则无 ORM 变更；若引入失败标记字段则触及 ORM 须 ask-first，但 notify 通道增强不需 ORM 变更）。

### 5.2 MA4 ↔ A5.6 边界

本验证审「行为是否符合需求」（best-effort 写失败运行时可观测性是否符合 UC-MFG-13 ⑫运营感知隐含要求），与 A5.6 审「E2E 断言强度」边界按此执行。本验证**不重做** A5.6 E2E 断言强度审计（A5.6 done，`2026-07-29-1430-arm-ma5-e2e-effectiveness.md`）。本验证证据为 main 源码 grep + catch/recallReport/config 路径推理（只读），非 E2E 注入重现（真实失败注入重现归 MR1 修复验证范围，见 plan §Non-Goals）。

---

## 6. 可观测性裁决（方法论 §2 判据 + 三源对照）

**决策框架**（plan §Goals + Phase 2 Decision）三分支：

| 分支 | 条件 | 裁决 |
|---|---|---|
| ① | catch 仅 LOG.error + 无监控采集 + 基因链缺口致召回报告降级无运营感知 | → 维持 P1-RC-010（测试补充义务）+ 登记 residual observability gap watch-only（归 MR1） |
| ② | catch 已有告警派发/失败标记 | → 闭合可观测性，P1-RC-010 维持测试补充义务 |
| ③ | config 默认 off | → 运行时风险限 config=true 部署，登记 config-enable 运营注意 |

**本验证命中**：**分支①**：
- catch = 仅 LOG.error（§1，无 notify/失败标记）→ 满足①「catch 仅 LOG.error」。
- LOG.error 无监控采集（§2，无结构化日志/metrics/告警通道）→ 满足①「无监控采集」。
- 基因链缺口致召回报告静默漏报 + degraded 无运营感知（§3）→ 满足①「基因链缺口致召回报告降级无运营感知」。
- 分支③**不适用**（config 默认 **true**，§4，写入路径默认活跃，缺口存在于默认部署）。
- 分支②**不适用**（catch 无告警派发/失败标记）。

**§2 判据对照**：
- **§2 P1⑤（测试断言完全缺失或仅冒烟）**：**成立** —— P1-RC-010 维持（testRecallReport 仅冒烟 + best-effort 写失败路径无测试，A1.11 §5 已裁决）。本验证**不撤销**此项。
- **§2 P2②（需求契约的可用性/可观测性要求未充分实现）**：**成立** —— catch 分支可观测性仅日志、无运营感知通道（residual observability gap）。登记 watch-only（登记不强制，归 MR1）。
- **§2 P0①/③/④（活跃数据破坏/核心循环断裂/会计过账正确性）**：**不成立** —— 基因链属质量追溯辅助功能（非会计正确性/核心循环/活跃数据破坏防护）；catch 不阻断完工（Decision 3 设计选择，owner doc 显式）；缺口致召回报告漏报是合规风险但非活跃数据破坏（已入库完工数据完整，仅追溯辅助链缺失）。

**L1/L2/L3 三源对照**：
- **L1**（`docs/design/manufacturing/use-cases.md` UC-MFG-13 ⑫）：「召回报告：从问题批次出发识别**所有**受影响成品批次」。基因链缺口致漏报 → ⑫隐含运营感知要求未充分满足（无感知通道）→ residual gap（P2② 可观测性维度）。
- **L2**（`batch-genealogy.md:135`）：显式设计 best-effort（try/catch 记日志不阻断完工）+ config 默认 true。best-effort 语义为 owner doc 显式设计（写失败不阻断完工），但**未声明** catch 可观测性增强（notify/失败标记）为 Non-Goal → 可观测性缺口非 documented simplification（§4 三判据：无 plan-audit 裁决 catch 可观测性裁剪 + owner doc 未标 Non-Goal + product-scope 未裁剪），属可增强项归 MR1。
- **L3**（实测）：catch 仅 LOG.error（§1）+ LOG 无监控采集（§2）+ 基因链缺口致 recallReport 漏报（§3）+ config 默认 true 写入路径默认活跃（§4）→ 运行时可观测性缺口确认。

**与 A1.11 §5 P1-RC-010 裁决分层一致**：A1.11 §5 裁决 P1-RC-010 = P1（§2 P1⑤ 测试断言维度）。本验证**闭合** SP-2 运行时可观测性确认：**维持** P1-RC-010（测试补充义务不撤销）+ **追加** residual observability gap watch-only（运行时可观测性维度，§2 P2②，归 MR1）。二者分层一致（测试维度 vs 可观测性维度，不同控制点不冲突）。

**裁决**：**命中分支①** —— 维持 P1-RC-010（测试补充义务归 MR1）+ 登记 residual observability gap watch-only（归 MR1：catch 分支 notify 告警派发增强，对齐 A4.2.4 范式，纯 BizModel 预授权不触 §5 ask-first）。

---

## 7. finding/注记衔接

- **P1-RC-010 维持 P1**（本验证结论，测试补充义务不撤销）：arm-index `P1-RC-010` 行追加运行时可观测性确认注记（catch 仅 LOG.error + 无监控采集 + 基因链缺口致召回报告静默漏报 + config 默认 true 写入路径默认活跃）。
- **登记 residual observability gap watch-only**（归 MR1）：catch 分支可观测性增强方向 = 加 notify 告警派发（对齐 A4.2.4 `dispatchVarianceFailureAlert` → `IErpSysNotificationBiz.notify` 范式），纯 BizModel 代码逻辑预授权可自动执行不触 §5 ask-first；可选失败标记持久化若引入新 ORM 字段则触及 ORM 须 ask-first（notify 通道增强不需 ORM 变更，为推荐路径）。
- **不触发 MR0**（无 P0）—— 基因链属质量追溯辅助功能非活跃数据破坏/会计正确性/核心循环；缺口为可观测性增强项归 MR1。
- **MR1 修复方向**（本验证指导，不实施）：
  - **测试补充**（P1-RC-010）：testRecallReport 强化断言 + best-effort 写失败测试（mock doWrite 抛异常）。纯测试预授权。
  - **可观测性增强**（residual gap）：catch 分支加 notify 告警派发（如 `IErpSysNotificationBiz.notify("mfg.genealogy-write-failure", ctx)`）+ 可选 LOG 升级为含结构化 workOrderCode 字段。纯 BizModel 预授权。
- **与既有 finding 交叉去重声明**：
  - **P1-RC-010**（arm-index）：本验证是其运行时可观测性裁决的输入，**不新建** finding，仅追加注记（维持 P1 测试补充义务 + 追加 residual observability gap）。
  - **A1.11 §5/§7**（源报告）：本验证闭合其 SP-2，结论与 §5 P1-RC-010 裁决分层一致（不撤销/不降级/不升级）。
  - **A4.2.4 notify 通道家族**（完工差异过账失败告警，`dispatchVarianceFailureAlert`）：本验证以其为可观测性对称参照（差异失败有 notify，基因链失败无 notify），**不新建** finding，仅在 residual gap 修复方向引用其范式。
  - **A4.2.6-8**（BOM 编辑影响运行时探针，done）：范式参照（mfg 完工 best-effort 路径运行时探针 + config-gated 裁决），不同控制点（BOM 快照 vs 基因链可观测性）不冲突。
  - **MA4 ↔ A5.6 边界**：本验证审行为符合性，不重做 A5.6 E2E 断言强度（§5.2）。

---

## 8. 过程纪律自检

### 8.1 nop-compliance-checker.sh 实测

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，本计划**无生产代码变更**故**无回归风险**）：

| 规则 | baseline（A1.11 实测） | actual（本次） | 状态 |
|------|----------------------|--------|------|
| R1a dao().saveEntity (BizModel) | 0 | 0 | ✅ |
| R1b dao().updateEntity (BizModel) | 0 | 0 | ✅ |
| R1c dao().getEntityById (BizModel) | 0 | 0 | ✅ |
| R1d dao().findAllByQuery (BizModel) | 14 | 14 | ✅ |
| R2a BizModel daoFor(ErpMd*) | 34 | 34 | ✅ |
| R2b BizModel daoFor(Erp*) 跨域 | 229 | 229 | ✅ |
| R2c 全生产代码 daoFor() 总量 | 1382 | 1382 | ✅ |
| R2d Processor daoFor(ErpMd*) | 34 | 34 | ✅ |
| R3 new Erp*() 构造实体 | 5 | 5 | ✅ |
| R5 @Inject private | 0 | 0 | ✅ |
| R6 @Transactional in BizModel | 2 | 2 | ✅ |
| R10 REQUIRES_NEW 事务 | 6 | 6 | ✅ |
| R12a 共享内核 import ErpFinBusinessType | 69 | 69 | ✅ |
| R12b 共享内核 import PostingEvent | 66 | 66 | ✅ |
| R12c 共享内核 import AcctSchemaResolver | 40 | 40 | ✅ |

**说明**：本计划是只读评估，**零生产代码变更**（无 .java/.orm.xml/.api.xml/view.xml 修改），checker 命中均为基线既有项，**无本计划引入的漂移**（R2b=229 / R2c=1382 / R2d=34 经 checker 本次实测逐一确认匹配 baseline；其余规则因零代码变更与 baseline 一致）。按 plan §Closure Gates，**不以 checker 退出码 0 作为门控依据**（checker 是纯 reporter，本计划无代码变更故无回归风险）。

### 8.2 独立性声明

- 本报告执行者 = 计划执行代理（非草案审查者）。计划草案审查已由独立子代理 ses_028f1a056ffe0ek87hABLZY8J6 完成（fresh session，Draft Review Record iter-1 accept，全 10 checklist 项 PASS，零 Blocker）。
- **结束审计**将由独立子代理（新会话）执行（plan §Closure Gates 最后一项），执行者**不自我审计**。

### 8.3 §8 自检清单

| # | 项 | 状态 | 证据 |
|---|---|---|---|
| 1 | 需求正确性（对照 L1 UC-MFG-13 ⑫） | ✅ | §6 三源对照，基因链缺口致⑫运营感知未充分满足→residual gap（P2②） |
| 2 | owner-doc 对齐（manufacturing/） | ✅ | §1-§4 引用 batch-genealogy.md / use-cases.md（UC-MFG-13） |
| 3 | 架构/边界影响 | ✅ | 只读评估，无跨模块依赖变更；MA4↔A5.6 边界声明 §5.2 |
| 4 | 验证充分性 | ✅ | catch 5 通道 census + LOG 3 采集维度 + recallReport 反向递归依赖链 + config 4 源 + A4.2.4 家族对比，全 file:line 证据 |
| 5 | 回归风险 | ✅ | 零代码变更，无回归 |
| 6 | 路由/技能选择 | ✅ | Skill: multi-dimensional-audit-prompt.md（plan 指定），维度覆盖齐全（7 默认维度 + catch 可观测性/家族对比） |
| 7 | 范围漂移 | ✅ | 不重审 P1-RC-010 P1 定级本身（仅评运行时可观测性差异）；不实施修复；不展开 SP-1/SP-3/SP-4（归独立工作项） |
| 8 | 与 arm-index 交叉去重 | ✅ | §7 声明，P1-RC-010 追加注记非新建；residual gap 归 MR1 不新建 arm-index finding 行 |

---

## 9. 结论

**整体 Verdict**：⚠️(P1) — **命中裁决分支①**：维持 P1-RC-010（测试补充义务归 MR1）+ 登记 residual observability gap watch-only（归 MR1）。

- **catch 分支可观测性 census**（SP-2 核心）：catch（`BatchGenealogyWriter.java:73-75`）= **仅 LOG.error**（无 notify 告警派发、无失败标记持久化、无 metrics/结构化日志/其他通道）。
- **LOG.error 监控采集 census**：LOG.error（`:74`）**无监控采集通道**（无结构化日志/metrics/日志告警），运营实际感知路径仅依赖人工巡检文本。与 A4.2.4 完工差异过账失败（LOG.error **+ notify**）不对称。
- **基因链缺口业务影响 census**（SP-2 核心）：catch 触发 → 该次完工零/部分基因行 → `recallReport`（`ErpMfgBatchGenealogyBizModel:89-105`）反向递归**漏报**下游受影响成品批次 + `degraded=true`（`:78`）结构性恒置**不反映缺口** → **静默漏报，运营无感知**（UC-MFG-13 ⑫合规风险）。
- **config 默认值复核**：`erp-mfg.genealogy-write-enabled` = **true**（`AppConfig.var(CONFIG, "true")`，null/空/异常回落 true；无 application.yaml override）→ best-effort 写入路径**默认活跃**，缺口存在于默认部署。

本验证**闭合** A1.11 §7 SP-2 运行时可观测性裁决，与 A1.11 §5 P1-RC-010 裁决分层一致（测试维度 P1 维持 + 追加运行时可观测性维度 residual gap watch-only 归 MR1）。修复归 MR1：P1-RC-010 测试补充（纯测试预授权）+ residual gap catch 可观测性增强[notify 告警派发，对齐 A4.2.4 范式，纯 BizModel 预授权不触 §5 ask-first]。
