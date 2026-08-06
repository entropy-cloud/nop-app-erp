# A4.2.4 完工差异过账失败告警通道 notify 投递 + 运营响应闭环运行时确认（rc-ma4-a4-2-4）

> 报告类型: requirement-compliance MA4 运行时行为验证（A1.9 SP-1 + A1.11 SP-1 合并）
> 生成日期: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.2.4
> 关联 finding: `P1-MA4-007`（resolved R1.16，本验证复核其运行时可见性）
> 关联存疑点: A1.9 §7 SP-1（`2026-08-02-2042-3-...-a1-9-...md` §7:310）+ A1.11 §7 SP-1（`2026-08-02-2245-...-a1-11-...md` §7:311），两报告逐字标注「与 A1.9 SP-1 同根因」
> 范式对齐: A4.2.6-8（done — mfg 成本过账 config-gated 路径运行时探针先例，`2026-08-06-1926-rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime.md`）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）
> 验证性质: **只读评估**（grep notify 投递实现 + dispatchVarianceFailureAlert 调用链 + 手动重算入口 xbiz 注册 + config 默认值 + 监控采集现状），**不改代码/ORM/api.xml/真相源**

## 0. 与 A4.2.6-8 范式对齐声明 + MA4↔A5.6 边界

本验证是 **MA4 运行时行为验证**（审"行为是否符合需求"——差异过账失败告警通道 notify 投递运行时可见性 + 运营响应闭环），范式对齐 A4.2.6-8（done — mfg 成本过账 config-gated 路径运行时探针先例）。

**MA4↔A5.6 边界声明**（methodology §去重协议）：本验证审「行为是否符合需求」（需求契约视角：差异过账失败可被运营感知并可干预），与 A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角）判据不同，按此边界执行，**不重做 A5.6 E2E 断言强度审计**。

## 1. 需求契约原文（L1 真相源）

L1 来源：`docs/design/manufacturing/use-cases.md`

- **UC-MFG-07 工单完工入库与成本结转**（差异过账链）：完工达量（willFinish）触发差异计算/过账，差异过账失败须可被运营感知并可干预（手动重算入口）。
- **UC-MFG-12 完工触发差异过账**：完工达量触发 6 类差异计算 + PRODUCTION_VARIANCE 凭证派发；config-gated `erp-mfg.variance-auto-calc-enabled` 控制完工自动触发；手动入口 `ErpMfgCostVariance__calculateVariances` 可重算（幂等：先红冲 + 删旧 + 重算）。

L1 隐含契约：差异过账失败（config=true + 永久性失败如标准成本未发布/卷算 base cost 缺失）→ GL 缺 PRODUCTION_VARIANCE 凭证悬挂 → **运营须经告警通道感知 + 经手动重算入口干预**（R1.16 resolved P1-MA4-007 即据此）。

**SP-1 存疑点原文**（A1.9 §7:310 + A1.11 §7:311 逐字一致）：

> 「**完工触发差异过账失败运行时悬挂可见性**：P1-MA4-007 已 resolved（R1.16 G3 错误分级 + 告警派发落地），但告警通道 `IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure")` 的实际运行时投递成功率（notify best-effort 降级不阻断主流程）+ 运营对告警的响应闭环（手动重算入口 `calculateVariances` 是否被实际使用）需运行时确认。」

## 2. 实现证据（L3 代码路径，写时实测锚点）

### 2.1 notify 投递语义 census（SP-1 核心）

调用链：`ErpSysNotificationBizModel.notify:54` → `ErpSysNotificationNotifyProcessor.notify:40` → `NotificationDispatcher.dispatch:76`。

| 锚点 | 关键行为断言 | 证据 |
|------|-------------|------|
| `ErpSysNotificationBizModel.java#notify:52-58` | `@BizMutation`（事务边界自动包装）→ 委派 `notifyProcessor.notify(eventType, context, ctx)`；返回 `List<ErpSysNotification>` | `module-notify/erp-notify-service/.../entity/ErpSysNotificationBizModel.java:52-58` |
| `ErpSysNotificationNotifyProcessor.java#notify:40-63` | (1) `findActiveTemplate(eventType)` 查 ACTIVE 模板，无模板 → LOG.warn + emptyList（config-gated 静默跳过）；(2) `dispatcher.dispatch(template, context)` 派发；(3) `dao.saveEntity/updateEntity` **同步持久化** 每条结果；(4) `catch (Exception e) { LOG.error(...); return emptyList; }` **best-effort 吞异常** | `module-notify/erp-notify-service/.../processor/ErpSysNotificationNotifyProcessor.java:40-63` |
| `NotificationDispatcher.java#dispatch:76-99` | 渲染模板 + 解析接收人 + `mergeOrPersist:114-148` 构造 ErpSysNotification（设 `STATUS_SENT:143`，**此处不 save**，返回由调用方 NotifyProcessor:48-55 同步 save）+ `dispatchExternalChannels:150-169` 经 nop-integration 派发邮件/短信（config-gated） | `module-notify/erp-notify-service/.../dispatch/NotificationDispatcher.java:76-169` |
| `NotificationDispatcher.java#sendEmailIfPossible:171-185` / `#sendSmsIfPossible:187-200` | 外发通道各包 `try { ... } catch (Exception e) { LOG.error(...); }` — best-effort，失败不阻断站内消息 | `NotificationDispatcher.java:171-200` |

**notify 投递语义结论（半同步 best-effort）**：

- ✅ **成功路径 = 同步持久化**：`@BizMutation` 事务内 `dispatcher.dispatch` → `dao.saveEntity`（NotifyProcessor:48-55）+ `STATUS_SENT`（Dispatcher:143）→ 运营经 `findUnread`/`findRead`/`countUnread` @BizQuery 工作台可见（`ErpSysNotificationBizModel:74-108`）。
- ⚠️ **失败路径 = best-effort 降级吞异常**：`catch (Exception e) { LOG.error(...); return emptyList; }`（NotifyProcessor:57-62）—— 模板缺失/渲染失败/接收人解析失败/落库失败 均被吞，**无 retry / 无告警升级 / 无 ErpFinPostingException 异常工作台接入 / 无外部告警兜底通道**。
- 即"半同步"语义：dispatch + save 同事务，成功则工作台可见，失败则静默吞（仅 LOG.error 痕迹）。

### 2.2 dispatchVarianceFailureAlert 调用链 census（G3 错误分级 R1.16 落地确认）

| 锚点 | 关键行为断言 | 证据 |
|------|-------------|------|
| `ErpMfgWorkOrderReportCompletionProcessor.java#reportCompletion:86-101` | `if (willFinish && facade.isVarianceAutoCalcEnabled())` (line 86) 门控 → try { `reverseIfExists`→`deleteByWorkOrder`→`calculateVariances`→`dispatchIfApplicable` 四步 } (line 87-92) → `catch (Exception e)` (line 93-101)：`isNoStandardCostError(e)` → LOG.warn 容错跳过；else → **LOG.error + `facade.dispatchVarianceFailureAlert(wo, e)`**（G3 真实故障告警派发） | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderReportCompletionProcessor.java:86-101` |
| `ErpMfgWorkOrderProcessor.java#dispatchVarianceFailureAlert:150-167` | (1) `notificationBiz == null` → return（null 守卫）；(2) ctx 富上下文：`workOrderId`/`workOrderCode`/`errorCode`/`errorMessage`/`postingNo`(=wo.code)（运营可定位）；(3) `notificationBiz.notify(NOTIFY_EVENT_VARIANCE_FAILURE, ctx, serviceCtx)`；(4) `catch (Exception notifyErr) { LOG.warn(...); }` **二级降级**（notify 本身失败也吞） | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderProcessor.java:150-167` |
| `ErpMfgWorkOrderProcessor.java#NOTIFY_EVENT_VARIANCE_FAILURE:89` | `static final String NOTIFY_EVENT_VARIANCE_FAILURE = "mfg.production-variance-posting-failure"` | `ErpMfgWorkOrderProcessor.java:89` |
| `ErpMfgWorkOrderProcessor.java#isNoStandardCostError:141-147` | G3 分级核心：`errorCode.contains("VARIANCE_NO_STANDARD_COST")` → 容错跳过（差异未配置非故障） | `ErpMfgWorkOrderProcessor.java:141-147` |

**dispatchVarianceFailureAlert 调用链结论**：

- ✅ **G3 错误分级 R1.16 落地确认**：完整调用链 `reportCompletion:99` → `dispatchVarianceFailureAlert:150` → `notify(NOTIFY_EVENT_VARIANCE_FAILURE, ctx):162`，G3 分级（容错跳过 vs 真实故障告警）落地，与 A1.9 §5.4 / A1.11 §5.4 HEAD 复核一致。
- ✅ **context 字段齐全**：workOrderId/workOrderCode/errorCode/errorMessage/postingNo 进 ctx → 运营经工作台通知可定位故障工单 + 错误根因。
- ⚠️ **二级降级**：`dispatchVarianceFailureAlert` 自身 `catch (Exception) → LOG.warn`（line 163-166）—— notify 投递失败时仅 LOG.warn，无兜底。

### 2.3 手动重算入口可达性 census（运营响应闭环）

| 锚点 | 关键行为断言 | 证据 |
|------|-------------|------|
| `ErpMfgCostVarianceBizModel.java#calculateVariances:48-51` | `@BizMutation` → GraphQL `ErpMfgCostVariance__calculateVariances` 可达；委派 `calculateVariancesProcessor.calculateVariances` | `module-manufacturing/erp-mfg-service/.../entity/ErpMfgCostVarianceBizModel.java:48-51` |
| `ErpMfgCostVarianceCalculateVariancesProcessor.java#calculateVariances:33-52` | COMPLETED 守卫（`ERR_VARIANCE_WORKORDER_NOT_COMPLETED`）+ 幂等闭环：`reverseIfExists`（红冲既有 PRODUCTION_VARIANCE 凭证）→ `deleteByWorkOrder`（删差异旧行）→ `calculateVariances`（重算）→ `dispatchIfApplicable`（派发新凭证） | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgCostVarianceCalculateVariancesProcessor.java:33-52` |
| `ProductionVarianceCalculator.java#calculateVariances:106-216` | 6 类差异计算（MATERIAL_USAGE/LABOR_EFFICIENCY/LABOR_RATE/OVERHEAD/VOLUME/SUBCONTRACT）+ `dispatchVarianceAlertIfOverThreshold:214,225-256` 阈值告警旁路（不同 notify event，见 §2.5） | `module-manufacturing/erp-mfg-service/.../costing/ProductionVarianceCalculator.java:106-216` |

**手动重算入口结论（运营响应闭环"可达且被引导"）**：

- ✅ **可达**：`@BizMutation` GraphQL mutation 注册，`ErpMfgCostVariance__calculateVariances(workOrderId)` 运营可经前端/GraphQL 直接调用。
- ✅ **幂等闭环**：reverseIfExists → delete → recompute → dispatch 四步幂等（plan 2026-07-18-2251-1），COMPLETED 守卫防止误用。
- ✅ **owner doc 引导齐全**：`docs/design/manufacturing/variance-analysis.md:85` 文档化手动入口（"亦可经手动入口 `ErpMfgCostVariance__calculateVariances` 重算（幂等：先红冲既有 PRODUCTION_VARIANCE 凭证 + 先删旧行再重算，仅 COMPLETED 工单允许）"）+ `:115` config 表登记 + `:83` 当前实现范围段落标注手动入口存在。

### 2.4 config `erp-mfg.variance-auto-calc-enabled` 默认值复核

| 锚点 | 关键行为断言 | 证据 |
|------|-------------|------|
| `ErpMfgConstants.java:171-173` | `CONFIG_VARIANCE_AUTO_CALC_ENABLED = "erp-mfg.variance-auto-calc-enabled"`，注释「默认 false=完工不自动触发差异计算，需手动经 ErpMfgCostVariance__calculateVariances 入口计算；true=工单完工（willFinish）时自动算」 | `module-manufacturing/erp-mfg-service/.../ErpMfgConstants.java:171-173` |
| `ErpMfgWorkOrderProcessor.java#isVarianceAutoCalcEnabled:396-398` | `readBoolConfig(ErpMfgConstants.CONFIG_VARIANCE_AUTO_CALC_ENABLED, false)` → 默认 **false** | `ErpMfgWorkOrderProcessor.java:396-398` |
| 部署 override 普查 | `rg "variance-auto-calc-enabled" *.yaml/*.properties` 全仓 → 仅 `erp-mfg-meta/module-meta.yaml`（元数据声明非 override）+ `erp-mfg-meta/precompile/module-meta.yaml` 命中；**无生产 application.yaml override**。`playwright.config.ts:18` + `_tmp-server.sh:49` 命中为**测试/E2E webServer JVM arg**（非生产部署），与 A4.2.6-8 §5 实测一致 | grep 实测 |

**config 默认值结论**：默认 **false**（三源一致 + 无生产部署 override）→ 完工自动算差异路径**非默认活跃**，告警通道仅在 config=true（生产显式启用或测试基础设施）时被触发。运行时风险限 config=true 部署。

### 2.5 监控/告警采集现状 census

| 锚点 | LOG 级别 | 用途 | 证据 |
|------|---------|------|------|
| `ErpMfgWorkOrderReportCompletionProcessor.java:97` | `LOG.error` | 真实故障（含工单 code + 异常 stack） | `:95-98` |
| `ErpMfgWorkOrderReportCompletionProcessor.java:95` | `LOG.warn` | G3 容错跳过（无 FIRMED 标准成本，非故障） | `:94-95` |
| `ErpMfgWorkOrderProcessor.java:164` | `LOG.warn` | notify 投递本身失败（二级降级） | `:163-166` |
| `ErpSysNotificationNotifyProcessor.java:60` | `LOG.error` | notify 派发失败（不阻断调用方） | `:57-62` |
| `NotificationDispatcher.java:183,198` | `LOG.error` | 外发邮件/短信失败（不阻断站内） | `:171-200` |

**ErpSysNotification 工作台查询入口**（运营感知路径）：

- `ErpSysNotificationBizModel.findUnread:74-76,119-141`（@BizQuery）→ 查 recipientUserId + status in (SENT,MERGED) 排除已读
- `ErpSysNotificationBizModel.findRead:80-102`（@BizQuery）→ 已读通知回查
- `ErpSysNotificationBizModel.countUnread:106-108`（@BizQuery）→ 未读计数

**监控采集结论**：

- ✅ **工作台可见性存在**：notify 成功路径同步落 ErpSysNotification 行 + STATUS_SENT + findUnread/countUnread @BizQuery 可查询。
- ⚠️ **无结构化 metrics / 无外部告警升级通道**：仅 LOG.error/warn（人工扫日志）+ ErpSysNotification 工作台（GraphQL 主动查询），无 Prometheus/metrics/外部告警系统接入，无告警升级机制。

### 2.6 关键发现：FAILURE 告警通道零运行时测试覆盖

**两 notify event 区分**（写时实测）：

| notify event | 常量 | 触发点 | 测试覆盖 |
|--------------|------|--------|----------|
| `mfg.production-variance` | `ErpMfgConstants.NOTIFY_EVENT_PRODUCTION_VARIANCE:223` | `ProductionVarianceCalculator.dispatchVarianceAlertIfOverThreshold:214,225-256`（**成功计算后**阈值告警旁路） | ✅ `TestErpMfgVarianceAlert` 3 场景强测（超阈值 notify 调用 + ErpSysNotification 行落入 + 未超阈值跳过 + config 关闭静默） |
| `mfg.production-variance-posting-failure` | `ErpMfgWorkOrderProcessor.NOTIFY_EVENT_VARIANCE_FAILURE:89` | `ErpMfgWorkOrderProcessor.dispatchVarianceFailureAlert:162`（**计算/过账失败时**告警派发） | ❌ **零运行时测试覆盖** |

**实测证据**：

- `grep dispatchVarianceFailureAlert|production-variance-posting-failure|variance-posting-failure` 跨 `module-manufacturing/erp-mfg-service/src/test` → **No files found**。
- `grep production-variance-posting-failure` 跨 `tests/e2e/*.ts` → **No files found**。
- `TestErpMfgVarianceAlert` 3 测试覆盖 `NOTIFY_EVENT = ErpMfgConstants.NOTIFY_EVENT_PRODUCTION_VARIANCE`（即 `mfg.production-variance`，line 57），**非** FAILURE event。

**结论**：FAILURE 告警通道（本验证对象）**零运行时测试覆盖**——无测试注入永久性失败（如 mock 缺失 FIRMED rollup 或破坏 dispatcher）以验证 `dispatchVarianceFailureAlert` → `notify("mfg.production-variance-posting-failure")` 实际投递 + 持久化 + 工作台可见。THRESHOLD 通道（不同 event）强测，但不能背书 FAILURE 通道投递可靠性。

## 3. 测试证据（L4）

| 控制点 | 测试 | 断言强度 | 备注 |
|--------|------|----------|------|
| 手动重算红冲闭环 | `TestErpMfgVarianceRecomputeReversal`（4 测试） | **强**：单凭证反向 + 一致不变量（ErpFinVoucherBillR 反查仅 1 条 isReversed=false）+ 红冲失败容错 + 首次无源凭证容错 | 覆盖 `ErpMfgCostVariance__calculateVariances` 入口完整幂等链 |
| 手动重算 E2E | `tests/e2e/business-actions/mfg-variance-recompute-reversal.action.spec.ts` | **强**：行级凭证断言（Dr/Cr 同向取负）+ 孤儿凭证一致性不变量 + cleanup 经 `cleanupMfg` 扩展 | DIRECT `@BizMutation` GraphQL 编排，复用 1800-2 runMfgChain |
| 阈值告警（THRESHOLD 通道） | `TestErpMfgVarianceAlert`（3 场景） | **强**：超阈值 notify 调用 + ErpSysNotification 行落入 + 接收人匹配 + 未超阈值跳过 + config 关闭静默 | **覆盖 `mfg.production-variance`（THRESHOLD），非 `mfg.production-variance-posting-failure`（FAILURE）** |
| **FAILURE 告警通道** | **无** | — | **零运行时测试覆盖**（§2.6 关键发现） |
| 完工触发差异过账（config=true 主路径） | `mfg-variance.spec.ts`（E2E） | **强**：5 类差异行 + PRODUCTION_VARIANCE 凭证 6 行精确数值断言 + posted=true | 主路径覆盖；FAILURE 路径未覆盖 |

## 4. 运行时行为证据（L5）

本验证即 L5 运行时行为证据（grep notify 投递实现 + 调用链推理 + config/监控普查）。复用既有证据：

- **A1.9 §5.4 + A1.11 §5.4 P1-MA4-007 HEAD 复核**（已 done）：R1.16 G3 错误分级 + 告警派发落地于 `ErpMfgWorkOrderReportCompletionProcessor:86-102` + `ErpMfgWorkOrderProcessor.dispatchVarianceFailureAlert:150-167` + owner doc `state-machine.md:180` 同步记录。
- **A4.2.6-8 §5 config 默认值核验**（已 done）：`erp-mfg.variance-auto-calc-enabled` 默认 false（三源一致 + 无生产 application.yaml override）。

本验证新增 L5 证据 = §2.1 notify 投递语义半同步 best-effort 实测 + §2.6 FAILURE 通道零运行时测试覆盖实测。

## 5. 运行时可见性裁决（methodology §2 判据 + 三源对照）

### 5.1 综合裁决：维持 P1-MA4-007 resolved + 登记 residual observability gap watch-only（归 MR1）+ config-enable 运营注意

裁决 = plan §Goals 三选项的**混合**（② + ③，部分 ①）：

| 维度 | 现状 | 对应选项 | 证据 |
|------|------|----------|------|
| notify 同步持久化 | ✅ 成功路径同步落 ErpSysNotification + STATUS_SENT + 工作台 findUnread 可见 | 倾向① | §2.1 |
| notify best-effort 降级 | ⚠️ catch Exception → LOG.error → emptyList + 二级降级 catch → 投递失败静默吞 | ② | §2.1 + §2.2 |
| 兜底（retry/告警升级） | ❌ 无 retry / 无 ErpFinPostingException 接入 / 无外部告警升级 | ② | §2.1 + §2.5 |
| 手动重算入口可达 | ✅ @BizMutation GraphQL + COMPLETED 守卫 + 幂等红冲 + 强测 + owner doc 引导 | 倾向① | §2.3 + §3 |
| config 默认 | ⚠️ false（三源一致 + 无生产 override）→ 告警通道非默认活跃 | ③ | §2.4 |
| 运行时测试可见性（FAILURE 通道） | ❌ 零运行时测试（仅 THRESHOLD 强测，不同 event） | ② | §2.6 |
| 监控采集 | ⚠️ 仅 LOG + 工作台，无 metrics/外部告警升级 | ② | §2.5 |

### 5.2 §2 判据编号对应

- **不构成 P0**（§2 P0①②③④ 均不满足）：告警通道是**可观测性增强**非活跃数据破坏防护；config 默认 off 降低触发面；手动重算入口存在闭环；期末结账前置检查扫 finance 异常工作台 PENDING/RETRYING 间接兜底。
- **不构成新 P1**：R1.16 已 resolved P1-MA4-007 的"告警派发基础设施缺失"根因——本验证确认基础设施层（G3 分级 + dispatchVarianceFailureAlert + notify 通道 + 同步持久化 + 工作台可见 + 手动重算入口可达且被引导）**已落地**，残差是**可靠性/可测性增强 successor**（非功能性缺失），归 §2 P2③ + residual observability gap watch-only，不重开 P1。
- **归 §2 P2③**（改进型——已落地基础设施的可靠性/可测性增强 successor）+ residual observability gap watch-only（归 MR1）。

### 5.3 三源对照（L1/L2/L3）

| 层级 | 来源 | 与实现一致性 |
|------|------|-------------|
| L1（功能契约） | `use-cases.md` UC-MFG-07/12：差异过账失败可被运营感知并可干预 | ✅ R1.16 resolved 满足"可感知"（告警派发）+"可干预"（手动重算入口）|
| L2（设计参考） | `variance-analysis.md:83,85,115` + `state-machine.md:180`：G3 错误分级 + 告警派发 + 手动重算入口 | ✅ 与 L1 一致，文档化实现偏离 |
| L3（代码路径） | `ErpMfgWorkOrderReportCompletionProcessor:86-101` + `dispatchVarianceFailureAlert:150-167` + notify 投递链 + `calculateVariances` 入口 | ✅ 实现落地与 L1/L2 一致 |

三源一致，无冲突。

### 5.4 与 A1.9 / A1.11 §5 P1-MA4-007 resolved 裁决分层一致

- A1.9 §5.4 / A1.11 §5.4 裁决 = **P1-MA4-007 resolved**（R1.16 G3 错误分级 + 告警派发落地）。
- 本验证确认此裁决的**运行时可见性"基础设施层"已达成**（G3 分级 + dispatchVarianceFailureAlert + notify 通道 + 同步持久化 + 工作台可见 + 手动重算入口可达且被引导），**不撤销 resolved**。
- 本验证新增 = **residual observability gap**（基础设施可靠性/可测性增强 successor）→ 归 MR1 watch-only，**不重开 P1-MA4-007**（§去重协议：同根因同控制点，复用既有 finding，仅追加运行时可见性确认注记）。

### 5.5 修复方向（归 MR1，纯 BizModel/notify 通道可靠性预授权不触 §5 ask-first）

按 roadmap 预授权类目「代码逻辑修复（BizModel / Processor / xbiz / view.xml）= 预授权自动执行」，notify 通道可靠性修复**不触 §5 ask-first**（不触及 ORM/会计过账核心路径）：

1. **增强 notify 投递可靠性**：失败进 ErpFinPostingException 异常工作台（或 notify 专用工作台）+ retry 机制（避免 best-effort 降级致告警丢失无痕迹）。
2. **补 FAILURE 通道运行时测试**：mock 永久性失败（如删除 FIRMED rollup / 破坏 dispatcher）→ 断言 `dispatchVarianceFailureAlert` 调用 + `notify("mfg.production-variance-posting-failure")` 投递 + ErpSysNotification 行落入 + 工作台 findUnread 可见（闭合 §2.6 测试空洞，与 P1-MA5-005 mfg 业财异常路径测试系统性空洞互补）。
3. **可选 metrics/外部告警升级通道**（运营实际感知路径增强）。

### 5.6 config-enable 运营注意（裁决选项③落地）

config 默认 off，运行时风险限 config=true 部署（生产显式启用或测试基础设施）。运营启用差异自动计算前应：

- 确认 `mfg.production-variance-posting-failure` notify 模板（ErpSysNotificationTemplate ACTIVE）+ 接收人配置就绪，避免 best-effort 降级致告警丢失；
- 确认 FIRMED 一份基于审核时 BOM 的卷算作为标准成本来源（A4.2.6-8 §5 结论）；
- 监控 `LOG.error` 含"完工触发生产差异计算/过账失败"关键字的频次（运营实际感知路径）。

## 6. 与 arm-index 衔接（§去重协议 + §7 命名）

| 本报告 finding/注记 | 与既有 finding 关系 | 裁决 | 操作 |
|---------------------|---------------------|------|------|
| residual observability gap（notify best-effort 降级无兜底 + FAILURE 通道零运行时测试） | 与 `P1-MA4-007`（resolved R1.16）**同根因同控制点**（完工差异过账失败告警派发） | **复用** `P1-MA4-007`（不新建 RC 编号） | arm-index P1-MA4-007 行追加**运行时可见性确认注记**（基础设施层达成 + residual gap watch-only 归 MR1） |
| config-enable 运营注意 | 与 A4.2.6-8 §5.6 config-enable 运营注意**同主题**（差异计算 config 默认 off） | **复用** A4.2.6-8 结论 | 本报告引用，不重复登记 |

**不新建 P1-RC-xxx**：本验证是运行时可见性评估，结果表面 = 验证报告 + finding/注记登记；residual gap 是验证**输出**（非范围内项目降级），归 MR1 R1.0 展开器读取。

## 7. 静态存疑点清单

无新增静态存疑点（本验证即 A1.9 §7 SP-1 + A1.11 §7 SP-1 的运行时展开，结论已落 §5）。

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。**本报告无生产代码变更（纯只读评估），checker 无回归风险**（methodology §8：纯审计报告注明"无生产代码变更，checker 无回归风险"）。actual vs baseline 摘录（partial，脚本在 R3 白名单构建处因 pre-existing `set -e`/`pipefail` 与部分 orm.xml 无 className 匹配的 xargs 非零返回交互提前 exit，**非本审计引入**）：

  | 规则 | 描述 | baseline | actual（本次实测） | delta | 归因 |
  |------|------|----------|-------------------|-------|------|
  | R1a | dao().saveEntity (BizModel) | 0 | 0 | = | — |
  | R1b | dao().updateEntity (BizModel) | 0 | 0 | = | — |
  | R1c | dao().getEntityById (BizModel) | 0 | 0 | = | — |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | = | — |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | = | — |
  | R2b | BizModel daoFor(Erp*) 跨域 | 240 | 229 | -11（≤） | 改善，不触发门控 |
  | R2c | 全生产代码 daoFor() 总量 | 1380 | 1382 | +2 | **pre-existing**（本审计零代码变更，不引入） |
  | R2d | Processor daoFor(ErpMd*) | 32 | 34 | +2 | **pre-existing**（本审计零代码变更，不引入） |
  | R3–R12c | — | — | （脚本 R3 提前 exit 未捕获） | — | 脚本 pre-existing 行为，非本审计引入 |

  **门控声明**：本审计**零生产代码变更**（仅 grep/read + 落盘验证报告 + plan/arm-index/roadmap 文档更新），R2c/R2d 的 +2 delta 是**先前提交引入的 pre-existing 状态**（本审计未触任何 .java 文件），**不构成本审计引入的回归**。本报告**不**以 checker 脚本退出码（R3 处提前 exit=1）作为门控通过依据（methodology §8：checker 是纯 reporter，真正门控在 CI workflow）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告 residual observability gap 已按 §7 规则 grep arm-index 同域同控制点（P1-MA4-007 resolved R1.16）后给出"**复用**"裁决（§6），**无未经比对直接新建的 finding**（不新建 P1-RC-xxx，仅追加 P1-MA4-007 运行时可见性确认注记）。与 A1.9 §5.4/§7 + A1.11 §5.4/§7 的复用关系已声明（§5.4）；MA4↔A5.6 边界已声明（§0）。

## 9. 与既有审计报告差异增量声明

- **复用 A1.9 §5.4 + A1.11 §5.4**（P1-MA4-007 HEAD 复核 resolved R1.16 G3 错误分级 + 告警派发落地）—— 本验证不重审 R1.16 修复落地本身，只评**运行时可见性是否真正达成**（notify 投递可靠性 + 运营响应闭环可达性）。
- **复用 A4.2.6-8 §5**（config `erp-mfg.variance-auto-calc-enabled` 默认 false 三源一致 + 无生产 override）—— 本验证不重审 config 默认值，只复用其结论评估告警通道默认活跃性。
- **本验证新增差异增量** = §2.1 notify 投递语义半同步 best-effort 实测 + §2.6 FAILURE 通道零运行时测试覆盖实测 + §5 residual observability gap 裁决（基础设施层达成 + 可靠性/可测性增强 successor 归 MR1）。

## 整体 Verdict

**⚠️（resolved 维持 + residual observability gap watch-only 归 MR1）** — P1-MA4-007 resolved R1.16 的运行时可见性**基础设施层已达成**（G3 错误分级 + dispatchVarianceFailureAlert + notify 通道 + 同步持久化 + 工作台可见 + 手动重算入口可达且被引导），**不撤销 resolved**。但登记 **residual observability gap watch-only（归 MR1）**：(a) notify best-effort 降级吞异常无兜底；(b) FAILURE 告警通道零运行时测试覆盖（仅 THRESHOLD 强测，不同 event）；(c) 监控采集仅 LOG + 工作台，无 metrics/外部告警升级。同时登记 **config-enable 运营注意**：config 默认 off，运行时风险限 config=true 部署。零 P0/零新 P1，residual gap 是验证输出非范围内项目降级，归 MR1 R1.0 展开器读取。
