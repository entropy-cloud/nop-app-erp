# ai-check-r2 M0.3 — 488 open finding 分流（M2.x 修复批边界）

> 落盘时间：2026-08-28-2049
> 路径：`docs/audits/check/2026-08-28-2049-ai-check-r2/m0-3-open-findings-bucketing.md`
> 来源：`docs/audits/check/ai-check-index.md`（607 行索引表）

## 概况

ai-check-r1 总 finding 数 533，已 fixed 45（来自 F1.1-F1.4 / F2.1-F2.3 共 9 个 plan 完成），**残余 open 488**（按 2026-08-28 索引状态）。本 M0.3 按域 × 同型 finding 簇把 488 open finding 分配到 M2.x 修复批，确保后续 M1 切片 + M2 修复的工作流可流水线推进。

## 域分布（按 ai-check-r1 域短码）

| 域 | 短码 | open 数 | ai-check 已有修复族 | 建议 M2.x 批 |
|---|---|---|---|---|
| sales | sal | 28 | 无（早域未启动）| **F2.10 sales+purchase P1 簇**（ai-check-r1 已挂）|
| hr-attendance-payroll | hr2 | 25 | 无 | **F2.13 mnt+hr P1 簇**（含 hr2-001..005）|
| assets-lifecycle | ast | 25 | 无 | **F2.8 assets-生命周期 P1 簇**（含 ast-001..006）|
| contract | ct | 23 | 无 | **F2.15 ct P1 簇**（ct-001..004）|
| quality | qa | 22 | F1.2（qa-012）| **F2.12 prj+qa P1 簇**（含 qa-001..005）|
| mfg-bom-mrp | mfg2 | 22 | 无 | **F2.6 mfg-BOM/MRP P1 簇**（mfg2-001..003 同型扫描）|
| finance-period-misc | fin4 | 21 | 无 | **F2.4 finance-期间 P1 簇**（fin4-001..003，**ready**）|
| customer-service | cs | 21 | 无 | **F2.14 crm+cs P1 簇**（cs-001..003）|
| assets-depreciation | ast2 | 21 | 无 | **F2.9 assets-折旧 P1 簇**（ast2-001..006）|
| projects | prj | 19 | F1.2（prj-006 部分）| **F2.12 prj+qa P1 簇**（prj-001..007）|
| manufacturing | mfg | 19 | F1.1（P0-CK-mfg-001）| **F2.5 mfg-工单 P1 簇**（mfg-002..005）|
| drp | drp | 19 | F2.6 关联 | **F2.15 drp P1 簇**（drp-001..003）|
| crm-cpq-forecast | crm2 | 19 | 无 | **F2.14 crm+cs P1 簇**（crm2-001..002）|
| inventory | inv | 18 | F1.3（inv-003）| **F2.11 inventory P1 簇**（inv-001..004）|
| crm-lead | crm | 18 | 无 | **F2.14 crm P1 簇**（crm-001..002）|
| maintenance | mnt | 17 | F1.2（mnt-007）| **F2.13 mnt P1 簇**（mnt-001..003）|
| mfg-subcontract | mfg3 | 17 | 无 | **F2.7 mfg-委外 P1 簇**（mfg3-001..005）|
| hr | hr | 16 | 无 | **F2.13 hr P1 簇**（hr-001）|
| b2b | b2b | 15 | 无 | **F2.15 b2b P1 簇**（b2b-001）|
| master-data | md | 14 | 无 | **F2.10（含 pur+md）**（P1-CK-md 暂无 P1）|
| finance-posting | fin | 14 | F1.4 / F2.1（fin-001/002/004/005）| **F2.1（部分）**残余 fin-006..019 |
| purchase | pur | 12 | F1.2（pur-002）| **F2.10 sales+purchase P1 簇**（pur-001..003）|
| finance-arap | fin2 | 12 | F2.2 全部（fin2-001..005）| **fin2-006..012** 残余 |
| finance-budget-costing | fin3 | 11 | F2.3 全部（fin3-001..005）| **fin3-006..016** 残余 |
| aps | aps | 11 | 无 | **F2.15 aps P1 簇**（aps-001..002）|
| logistics | log | 10 | 无 | **F2.15 log P1 簇**（log-001..002）|
| common | common | 10 | 无 | **M1 切片重扫**（commmon app 共享模块，**非独立域 P1 簇**）|
| notify | notify | 8 | 无 | **F2.15 notify P1 簇**（notify-001..002）|
| **合计** | — | **488** | — | — |

> 注：short code 中"hr2"指 hr-attendance-payroll 子目录（ck-hr-attendance-payroll.md），与 hr-org 同属 hr 大域。ai-check-r1 28 份报告与域一一对应；M0.3 表保留 28 域拆分。

## 同型 finding 合并（建议基类化修复）

跨域同型 finding 簇（按 ai-check-r1 报告模式已识别的）：

| 同型簇 | finding 域 | 建议基类化 | 对应 ai-check-r2 M2.x 批 |
|---|---|---|---|
| CRUD 通用 update 无状态守卫（已 F1.3 修 15+）| 残余站点的 mfg3 / crm / crm2 / ct / b2b / prj / qa / mnt / drp / log / aps / notify | 沿用 AbstractErpCrudBizModel | M2.5/M2.7/M2.8/M2.9/M2.11/M2.12/M2.14/M2.15 |
| REQUIRES_NEW 凭证悬挂（已 F1.2 修 9 站点）| 5 域 listener 残余（assets/hr/projects/maintenance/quality）| 沿用 守卫前置/副作用后置模式 | M2.4 / M2.13 修整 |
| batch Approve 共享单事务吞 NopException | pur-013 / sal-016 / mfg2-008 等同型 | 单事务 + 失败行隔离 | M2.10 / M2.6 |
| currentUserId 宽 catch 静默 null | pur-010 / hr-008 / mfg2-013 / mnt-008 / crm-012 等 | fail-loud + WARN 日志 | M3.4（专项）|
| notify 模板种子三库 | 6 域（aps/fin/mnt/cs/prj/qa）| 补 3 库缺 ACTIVE 模板 | M3.3（专项）|
| cron 键漂移（双键/三层门控）| 9 域（aps/cs/hr/fin/inv/mnt/prj/b2b/notify）| 统一 3 键配置 + bean id 统一 | M3.2（专项）|
| 状态机 dict 死状态 | crm/crm2/ct/b2b/qa/mnt/fin2/hr2 | 删死状态 or 移 Deferred | M1.1-M1.16 各切片 |
| orgId 隔离族 | fin/cs/sal/inv/qa/crm/crm2/hr2 | IUserContext.getOrgId + IQueryTransformer | M3.1（专项）|
| ERP-Flux 转换（XView complex/embed）| 74 个非标页 AMIS DSL | x:extends + xlib 抽象 | 由 `2026-08-28-1600-1` non-standard-pages plan 吸收 |

## M2.x 修复批边界建议（13 批）

按 ai-check-r1 F2.x / F3.x 已就绪的修复批，**完全继承 roadmap 工作项结构**，不重定义批：

| 批 | 范围 | open finding 数估计 | 跨 mission 状态 |
|---|---|---|---|
| F2.4 finance-期间 P1 簇 | fin4-001..003 | 21 | **ready**（唯一可启动）|
| F2.5 mfg-工单 P1 簇 | mfg-002..005 | 19 | todo（**M1 切片可重扫同型**）|
| F2.6 mfg-BOM/MRP P1 簇 | mfg2-001..003 + drp-012 | 23 | todo |
| F2.7 mfg-委外 P1 簇 | mfg3-001..005 | 17 | todo |
| F2.8 assets-生命周期 P1 簇 | ast-001..006 | 25 | todo |
| F2.9 assets-折旧 P1 簇 | ast2-001..006 | 21 | todo |
| F2.10 sales+purchase P1 簇 | sal-001/002/004 + pur-001/002/003 + md 14 项 P2 折算 | 28+12+14=54 | todo |
| F2.11 inventory P1 簇 | inv-001..004 | 18 | todo |
| F2.12 prj+qa P1 簇 | prj-001..007 + qa-001..005 | 19+22=41 | todo |
| F2.13 mnt+hr P1 簇 | mnt-001..003 + hr-001 + hr2-001..005 + mfg-001 close 流 | 17+16+25+部分 mfg = 60+ | todo |
| F2.14 crm+cs P1 簇 | crm-001/002 + crm2-001/002 + cs-001..003 | 18+19+21=58 | todo |
| F2.15 ct+b2b+drp+log+aps+notify P1 簇 | ct 23 + b2b 15 + drp 19 + log 10 + aps 11 + notify 8 | 86 | todo |
| F3.1-F3.4 P2 横切簇 | orgId / cron / notify 模板 / currentUserId 宽 catch | 30+（跨域）| todo |
| F3.5-F3.x P2 域簇 + F3.y P3 波次 | 残余 P2/P3 | 195 | todo |
| **合计** | — | **488** | — |

## M2.0 修复方法基线（继承 ai-check-r1 F0.2）

每 finding 强制流程（**与 r1 F0.2 同规，加严**）：
1. 读报告 finding（控制点/证据/建议修复方向）
2. **先写失败测试**（复现缺陷）
3. 修复
4. 测试绿 + 既有测试零回归
5. **回填 ai-check-index 状态**（fixed + 测试与提交指针 / not-a-problem + 说明）
6. 同步更新 ai-check-r1 F 工作项 ready→done

## 保护区域路由（不变）

- ORM 变更（.orm.xml）= auto + dual-agent-approval（两个独立子 agent 批准落盘 plan）
- 会计过账（VoucherFact / PostingProcessor）/ auth / 数据删除 = plan-first（owner doc + tests + 独立 plan-audit）
- 不触保护区域的纯逻辑修复 = plan-first 标准流程

## 多次执行隔离

本文件落盘于 `docs/audits/check/2026-08-28-2049-ai-check-r2/m0-3-open-findings-bucketing.md`。后续 M2 修复批可独立创建子目录 `<YYYY-MM-DD-HHmm>-ai-check-r2-fix-<batch>/`。

## 落盘物

- 本文件（M0.3 域 × 同型分流 + 13 批修复边界）
- 配合 M0.1 / M0.2 / M0.4 共同构成 M0 阶段产出
