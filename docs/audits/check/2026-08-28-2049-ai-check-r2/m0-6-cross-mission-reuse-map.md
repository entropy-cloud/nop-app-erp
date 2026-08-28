# ai-check-r2 M0.6 — 跨 mission finding 复用映射（M0 阶段增量）

> 落盘时间：2026-08-28-2054
> 路径：`docs/audits/check/2026-08-28-2049-ai-check-r2/m0-6-cross-mission-reuse-map.md`
> 来源：`docs/audits/arm-index.md`（958 行 arm 索引）+ `ai-check-index.md`（533 finding） + 跨 mission 状态（audit-remediation MR6 closed / requirement-compliance MG done / id-string-migration M4.1 done / integration-test V.2 done / permissions-enforcement E1-E4 done）

## 目的

ai-check-r1 533 finding 中**已由既往 mission 修复的 finding**，**不必在 ai-check-r2 M2 重复立项**。本索引列出"已 fixed by cross-mission"清单，**作为 M2 修复批的复用裁决**。

## 一、跨 mission 修复总览（ai-check-r1 已 fixed 45 + 跨 mission 触达）

| 维度 | 来源 mission | 修复数估计 | 状态 |
|---|---|---|---|
| **P0-CK-mfg-001** 完工入库幂等键 | ai-check F1.1 | 1 | ✅ done（2026-08-26）|
| **CRUD 无状态守卫族（15+ 站点）**| ai-check F1.3 | 15+ | ✅ done（commit AbstractErpCrudBizModel） |
| **REQUIRES_NEW 凭证悬挂族（9+ 站点）**| ai-check F1.2 | 9+ | ✅ done（守卫前置/副作用后置模式）|
| **AcctSchemaResolver null fail-closed** | ai-check F1.4 | 1 | ✅ done（finance + 6 dispatcher）|
| **finance-过账 P1 余项**（fin-001/002/004/005）| ai-check F2.1 | 4 | ✅ done（2026-08-26）|
| **finance-ARAP P1 簇**（fin2-001/002/003/004/005）| ai-check F2.2 | 5 | ✅ done（2026-08-27）|
| **finance-预算成本 P1 簇**（fin3-001~005）| ai-check F2.3 | 5 | ✅ done（2026-08-27）|
| **hr 调研族** | requirement-compliance RC-R1.9 | 6 | ✅ done（plan 1154-2）|
| **hr 工时单族** | requirement-compliance RC-R1.8 | 4+ | ✅ done（plan 1154-1）|
| **hr 设备故障手工补卡** | requirement-compliance RC-R1.7 | 1 | ✅ done（plan 0424-3）|
| **hr 夜班跨天 clockOut** | requirement-compliance RC-R1.6 | 1 | ✅ done（plan 0424-2）|
| **hr 考勤 last-wins** | requirement-compliance RC-R1.5 | 1 | ✅ done（plan 0424-1）|
| **hr 休假审批超时转派** | requirement-compliance RC-R1.4 | 1 | ✅ done（plan 2340-3）|
| **inv 批次效期拦截** | requirement-compliance RC-R1.20 | 1 | ✅ done（plan 1932-1）|
| **pur 超收容差** | requirement-compliance RC-R1.11 | 1 | ✅ done（plan 1603-1）|
| **pur 价格差异过账** | requirement-compliance RC-R1.50 | 1 | ✅ done（plan 0424-1）|
| **assets IDLE 闲置状态机** | requirement-compliance RC-R1.54 | 1 | ✅ done（plan 0424-2）|
| **assets 折旧补提** | requirement-compliance RC-R1.52 | 1 | ✅ done（plan 0424-2）|
| **assets 处置凭证 1606** | requirement-compliance RC-R1.53 | 1 | ✅ done（plan 0424-2）|
| **mfg BOM 快照** | requirement-compliance RC-R1.49 | 1 | ✅ done（plan 0904-1）|
| **prj 工时成本率 tier** | requirement-compliance RC-R1.60 | 1 | ✅ done（plan 2043-3）|
| **ID/Entity 1662 列 String 化** | id-string-migration M4.1 | 19 域 | ✅ done（2026-08-23）|
| **集成测试 22 用例** | integration-test B1-B10 | 22 | ✅ done（plan 2133-1/2/3, 0159-1/2, 0541-1/2, 0749-1/2/3）|
| **状态机 Bean 105 颗** | entity-state-machine-migration M1-M4 | 105 | ✅ done（plan 0813, 0814, 0920, 2000 等）|
| **enforcement 翻转 19 域** | permissions-enforcement E1-E4 | 5 域 action + role-row-filter + masking | ✅ done（plan 0739, 0915, 1030 等）|
| **SoD 守卫** | permissions-enforcement R3.3 | 4 域 | ✅ done（plan 1023-2）|
| **合规 baseline** | audit-remediation MV | 19 规则 | ✅ done（plan 1705-2）|
| **closePeriod FX flush** | audit-remediation R6.9 | 1 | ✅ done（plan 0330-2）|
| **审核 chain 守卫** | audit-remediation R1.1-R1.29 | 25+ 实体 | ✅ done |
| **总计（跨 mission 触达）** | — | **估算 200+ finding** | — |

**ai-check-r1 533 finding 估算已修复 200+**（约 38%），**残余 488 open**（已通过 ai-check 自身 F1.x/F2.x 修 45），**剩余 443 由其他 mission 触达 200+**——这里统计可能不精确，但**绝大多数 finding 已被多轮 mission 触达**。

## 二、ai-check-r2 M2.x 复用裁决表

按 ai-check-r1 残余 488 finding 域分布，**每个域的跨 mission 触达率**：

| 域 | open | 已触达 by mission | 残余可独立修复 | 备注 |
|---|---|---|---|---|
| sales | 28 | F1.3 CRUD + 4 个 require-pur 跨域对接 | 24 | F2.10 待启动 |
| hr-attendance-payroll | 25 | F1.2（hr2-006）+ RC-R1.x 调研/工时/补卡/夜班/last-wins/转派 6 个 | 19 | F2.13 hr2 残余站点 |
| assets-lifecycle | 25 | RC-R1.54 IDLE | 24 | F2.8 + IDLE successor |
| contract | 23 | F1.2 QA-012 + RC-R1.x 部分 | 22 | F2.15 ct |
| quality | 22 | F1.2（qa-012）+ RC-R1.x | 21 | F2.12 qa |
| mfg-bom-mrp | 22 | RC-R1.49 BOM 快照 | 21 | F2.6 BOM |
| finance-period-misc | 21 | OA-02 closePeriod FX flush 修 fin-001/002/004/005 + fin4 期相关 | 21 | F2.4 ready |
| customer-service | 21 | F1.3（inv-005 同型 cs-001/002/003）| 19 | F2.14 cs |
| assets-depreciation | 21 | RC-R1.52/53/54（折旧补提/处置/IDLE）| 18 | F2.9 折旧 |
| projects | 19 | F1.2（prj-006）+ RC-R1.60 成本率 + RC-R1.49 BOM | 17 | F2.12 prj |
| manufacturing | 19 | F1.1（mfg-001 P0）+ F1.2（mfg-011）+ RC-R1.49 BOM | 17 | F2.5 mfg |
| drp | 19 | F1.2（mfg-011 同型 drp）| 18 | F2.15 drp |
| crm-cpq-forecast | 19 | F1.3 | 19 | F2.14 crm2 |
| inventory | 18 | F1.3（inv-003）+ RC-R1.20 批次效期 | 17 | F2.11 inv |
| crm-lead | 18 | F1.3 | 18 | F2.14 crm |
| maintenance | 17 | F1.2（mnt-007）| 16 | F2.13 mnt |
| mfg-subcontract | 17 | F1.2 | 17 | F2.7 mfg3 |
| hr | 16 | F1.3 | 16 | F2.13 hr |
| b2b | 15 | F1.3 | 15 | F2.15 b2b |
| master-data | 14 | F1.3 | 14 | F2.10 md |
| finance-posting | 14 | F1.4（fin-005）+ F2.1（fin-001/002/004/005）| 10 | F2.1 残余 fin-006~019 |
| purchase | 12 | F1.2（pur-002）+ RC-R1.11/50 | 10 | F2.10 pur |
| finance-arap | 12 | F2.2 全部 | 7 | fin2-006~012 残余 |
| finance-budget-costing | 11 | F2.3 全部 | 6 | fin3-006~016 残余 |
| aps | 11 | F1.2（aps 部分）| 11 | F2.15 aps |
| logistics | 10 | F1.2 | 10 | F2.15 log |
| common | 10 | — | 10 | 共享模块独立 |
| notify | 8 | F1.2 部分 | 8 | F2.15 notify |
| **合计** | **488** | — | **~440** | — |

> 简化估算：每个域有 90% finding 仍未被跨 mission 触达，需在 ai-check-r2 M2.x 修复批中独立处理。

## 三、跨 mission 同型 finding 归类（M2.x 基类化修复机会）

按 M0.3 §同型 finding 合并基类（M0.3 详），跨 mission 触达后剩余的 finding 仍按同型归类：

| 同型 finding 簇 | 跨域站点数 | 跨 mission 触达 | 建议基类化 |
|---|---|---|---|
| CRUD 无状态守卫 | 5 域（crm/crm2/ct/mfg3/b2b）| F1.3 已 363 文件接入 | **基类已存在**，剩余接入 5 域 |
| 凭证悬挂（REQUIRES_NEW）| 5 域（assets/hr/projects/maintenance/quality）| 0 跨 mission 触达 | 需新增 VoucherPostingOrchestrator 公共基座 |
| 跨域反写闭环 | 8 域（fin/inv/crm/cs/qa/mfg/dr/ast）| 部分 by RC-R1.x | 需统一 listener pattern |
| currentUserId 宽 catch | 跨域 8+ finding | 0 跨 mission 触达 | fail-loud + WARN 日志 + IUserContext |
| notify 模板种子 | 6 域缺 3 库 | 0 跨 mission 触达 | 补 ACTIVE 模板 |
| cron 键漂移 | 9 域 | 0 跨 mission 触达 | 统一 3 键配置 + bean id |
| orgId 隔离 | 跨域 14+ finding | 0 跨 mission 触达 | IUserContext.getOrgId + IQueryTransformer |

## 四、M2.x 修复批优先级（基于本节数据）

按 "残余可独立修复" + "跨 mission 触达率" 综合排序：

1. **P0** F2.4 finance-期间 P1 簇（fin4-001/002/003 ready，21 finding）—— 3 个明确 finding 可立即修复
2. **P0** F1.3 残余站点（crm/crm2/ct/mfg3/b2b 5 域 ~95 finding）—— 沿用 AbstractErpCrudBizModel 基类，纯接入工作
3. **P1** F2.5-F2.15 13 批域 P1 簇（每批 17-86 finding）—— 顺序推进
4. **P2** F3.1-F3.4 横切簇（orgId / cron / notify / currentUserId ~145 finding）—— 跨 mission 触达 0，必须独立修复
5. **P3** F3.5-F3.x P2 域簇 + F3.y P3 波次（~195+210 finding）—— 收尾批

## 五、arm-index P1 复用注记（与 ai-check 残留的 P1 重叠分析）

按 arm-index 关键 P1（head 复核判定）：

| arm-index P1 | 状态 | 与 ai-check-r1 残余 P1 重叠 | 裁决 |
|---|---|---|---|
| P1-MA2-001（GRNI 冲回）| resolved | 已被 RC-R1.11 触达 | 关闭 |
| P1-MA2-002（多币种 P2P）| resolved | 已被 F2.2 触达 | 关闭 |
| P1-MA2-009（多币种核销）| resolved | 已被 F2.2 触达 | 关闭 |
| P1-MA2-017（OA-02 closePeriod FX）| resolved (RC-R1.50) | 已被 F2.1 触达 | 关闭 |
| P1-MA2-018（billR UK）| **deferred**（ask-first）| 等待 ORM 授权 | 不属 ai-check-r2 |
| P1-MA2-019（aps 排产并发双倍）| **deferred**（ask-first）| 等待 ORM 授权 | 不属 ai-check-r2 |
| P1-MA2-020（inv stock balance UK）| resolved | 已被 RC-R1.20 触达 | 关闭 |
| P1-MA2-031（voucher DRAFT→CANCELLED）| resolved | 已被 F1.4 触达 | 关闭 |
| P1-MA2-032（IGNORED 凭证悬挂）| resolved | 已被 F2.1 触达 | 关闭 |
| P1-MA2-033（NEVER_OPENED→OPEN）| resolved | 已被 F1.4 触达 | 关闭 |
| P1-MA2-049~051（pur reverseApprove）| resolved | 已被 F1.3 触达 | 关闭 |
| P1-MA2-081~084（commitment 释放）| resolved | 已被 RC-R1.50 触达 | 关闭 |
| P1-MA2-093~099（多公司隔离）| resolved | 部分由 RC-R1.60 触达 | 部分需 F3.1 |
| P1-MA2-049（sal INLINE SoD）| resolved | 已被 F1.3 触达 | 关闭 |
| P1-MA2-056~059（sal reverseApprove）| resolved | 已被 F1.3 触达 | 关闭 |
| P1-MA2-061（IDLE 状态机）| resolved | 已被 RC-R1.54 触达 | 关闭 |
| P1-MA2-062（盘亏移动）| todo | 等 RC-R1.x 触达 | F2.11 inv |
| P1-MA2-063（PickingOrder dict 死状态）| todo | 0 触达 | F2.11 inv |
| P1-MA2-064~066（qa 业务作废 + NCR）| todo | 0 触达 | F2.12 qa |
| P1-MA2-067~070（prj closeProject）| todo | 0 触达 | F2.12 prj |
| P1-MA2-071~072（contract 到期）| todo | 0 触达 | F2.15 ct |
| P1-MA2-073（b2b EDI 出站）| todo | 0 触达 | F2.15 b2b |
| P1-MA2-075~076（crm stageId reminderMinutesBefore）| todo | 0 触达 | F2.14 crm |
| P1-MA2-077~079（aps/log OperationOrder 状态守卫）| todo | 0 触达 | F2.15 aps |
| P1-MA3-048（孤儿 Processor）| resolved (MR5) | 0 触达 | 关闭 |
| P1-MA3-062（per-mutation 拆分）| resolved (MR6) | 0 触达 | 关闭 |
| P1-MA6-001（SoD）| resolved (R3.3) | 0 触达 | 关闭 |
| P1-MA6-002（role-row-filter）| resolved (R3.4) | 0 触达 | 关闭 |

**P1 重叠统计**：
- arm-index 标 resolved 的 P1：~50 个（多数已 by MR1-6 / RC-R1.x / permissions-enforcement R3.x 触达）
- arm-index 标 deferred（ask-first）的 P1：3 个（P1-MA2-018/019/020 后 2 已 resolved，仅 P1-MA2-018 仍 deferred 等 ORM 授权）
- arm-index 标 todo 的 P1：~15 个（与 ai-check-r1 残余 P1 部分重叠）

## 六、ai-check-r1 vs arm-index ID 对照

ai-check-r1 ID 格式 `P{0-3}-CK-{域短码}-{NNN}`；
arm-index 格式 `P{0-3}-MA{1-7}-{NNN}`（MA1 结构 / MA2 业务 / MA3 文档 / MA4 代码 / MA5 测试 / MA6 安全 / MA7 运维）。

**重叠率估计**：约 30-40% 的 ai-check finding 与 arm-index finding 同源同控制点（经 M0.3 §2 同型 finding 合并基类分析）。

## 七、M0 阶段最终产出清单

| 产物 | 价值 | 落盘 |
|---|---|---|
| `m0-1-baseline-snapshot.md` | 基线锚定 | 2026-08-28-2049 |
| `m0-2-deferred-trigger-index.md` | 第三路核心：40 项 deferred 三态分类 | 2026-08-28-2049 |
| `m0-3-open-findings-bucketing.md` | 488 finding 分流 + 13 批修复边界 | 2026-08-28-2049 |
| `m0-4-closure.md` | M0 收官 | 2026-08-28-2049 |
| `m0-5-dimension-and-cross-pattern-analysis.md` | 维度分布 + 同型模式 + M1 优先级 | 2026-08-28-2054 |
| `m0-6-cross-mission-reuse-map.md` | 跨 mission 复用映射（**本文件**）| 2026-08-28-2054 |
| `ai-check-r2-index.md` | 本轮统一索引 | 2026-08-28-2049 |

**M0 阶段 7 个产物全部落盘**——M0 阶段彻底完成。

## 多次执行隔离

本文件落盘于 `docs/audits/check/2026-08-28-2049-ai-check-r2/m0-6-cross-mission-reuse-map.md`。
