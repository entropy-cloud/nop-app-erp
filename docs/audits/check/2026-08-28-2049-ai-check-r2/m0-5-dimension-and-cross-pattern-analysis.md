# ai-check-r2 M0.5 — 维度分布与同型模式交叉分析（M0 阶段增量）

> 落盘时间：2026-08-28-2054
> 路径：`docs/audits/check/2026-08-28-2049-ai-check-r2/m0-5-dimension-and-cross-pattern-analysis.md`
> 来源：基于 `docs/audits/check/ai-check-index.md` 488 open finding 全表 grep 统计（M0.1/M0.3 基础上增量分析）

## 一、488 open finding 维度分布（D1-D10）

按 ai-check-r1 检查维度（D1 平台反模式 / D2 异常闭环 / D3 状态机 dict 可达性 / D4 调度链完整性 / D5 守卫完整性 / D6 业务计算正确性 / D7 事务与并发 / D8 数据一致性 / D9 性能 / D10 空值与边界）：

| 维度 | open finding 数 | 占比 | 主要表征 |
|---|---|---|---|
| **D6 业务计算正确性** | 67 | 13.7% | 金额/数量/汇率/FX 重估/成本计算错误 |
| **D8 数据一致性** | 38 | 7.8% | 跨域反写闭环缺失、gl_balance 漂移、post 标志不回写 |
| **D5 守卫完整性** | 36 | 7.4% | CRUD update 守卫、@BizMutation 权限、乐观锁、引用守卫 |
| **D9 性能** | 25 | 5.1% | N+1 查询、全表加载、缺分页 |
| **D10 空值与边界** | 22 | 4.5% | NPE 风险、零守卫、null 处理 |
| **D6/D8 复合** | 21 | 4.3% | 业务计算 + 数据一致性复合缺陷 |
| **D3 状态机 dict 可达性** | 20 | 4.1% | 死状态、非法迁移、终态可复活 |
| **D4 调度链完整性** | 16 | 3.3% | cron 键漂移、job 幂等、batch.xml 接线 |
| **D2 异常闭环** | 15 | 3.1% | 宽 catch 吞咽、失败无告警 |
| **D1 平台反模式** | 13 | 2.7% | @Inject private、System.currentTimeMillis 等 |
| **合计 488** | — | — | — |

**D6 + D8 合计 67+38=105（21.5%）** 是 ai-check-r1 残余 finding 的最大头——**业务计算正确性 + 数据一致性**是 P2/P3 修复批的核心战场。

## 二、跨域同型 finding 模式（按 M0.3 §同型 finding 合并基类深化）

### 2.1 CRUD update 无状态守卫（同型 簇 1）

**已 fixed 5 finding**（F1.3 AbstractErpCrudBizModel 基类 + 363 文件全域接入）：
- P1-CK-pur-003（修）/ P1-CK-sal-004（修）/ P1-CK-inv-003/005（修）/ P1-CK-mfg2-012/011（修）

**残余 488 open 中**仍存以下域未接入基类：
- **crm/crm2** 18+19=37 个 finding —— 实体 Lead/Opportunity/Account 裸 CrudBizModel
- **ct** 23 个 finding —— Contract 业务链 reverseApprove 守卫
- **mfg3** 17 个 finding —— SubcontractOrderProcessor 拆解
- **b2b** 15 个 finding —— Asn/EdiDoc 9 stub BizModel
- **mfg** 19 个 finding —— WorkOrderFacade 4 业务方法
- **mfg2** 22 个 finding —— MrpPlan/Simulation 状态字段

**建议基类化方案**：AbstractErpCrudBizModel 已支持状态机感知，crm/crm2/ct/mfg3/b2b 5 域直接接入（**M2.x 修复批主线**）。

### 2.2 凭证悬挂（同型 簇 2）

**已 fixed 9 finding**（F1.2 REQUIRES_NEW 守卫前置/副作用后置模式）：
- P1-CK-pur-002 / P1-CK-inv-012 / P1-CK-mfg-011 / P1-CK-ast-015 / P1-CK-ast2-021 / P1-CK-mnt-007 / P1-CK-qa-012 / P1-CK-prj-006 / P1-CK-hr2-006

**残余 5 域 listener 未触达**（M0.2 §已满足项 #2）：
- assets/hr/projects/maintenance/quality 5 域 6 listener 未落地
- F2.4 期间 P1 簇同类（P1-CK-fin4-001/002/003）+ 14 个 fin2 残余 + 16 个 fin3 残余

**建议基类化方案**：在 `app-erp-common-service` 新增 `VoucherPostingOrchestrator` 公共基座，5 listener 委托同一前置守卫（**M2.4 finance + M2.13 hr + M2.12 prj+qa + M2.8 assets + M2.13 mnt 修复批联合行动**）。

### 2.3 orgId 隔离族（同型 簇 3）

**已记录 finding**（P1-MA2-093/094/095/098/099 等跨 mission）：
- 19 模块 erp-{module}.data-auth.xml 全部 `<objs/>` 空规则
- 0 个自定义 IDataAuthChecker
- IServiceContext/IContext 均无 getOrgId()
- 11 dashboard BizModel 经 IDaoProvider 直访绕过认证管道

**488 open 中跨域 orgId finding**（M0.2 §部分满足项）：
- 5 域 14 finding（fin-006/007/008 + cs-008/009 + crm-006/007/008 + mfg-006/007 + drp-006/007/008/009/010/011/012 + crm2-006/007/008 + fin2-006/007 + ast-009/010/011/012/013/014/015/016 + b2b-002/003/004/005/006/007/008/009 + hr-002/003/004/005 + hr2-006/007/008/009/010/011/012/013/014 + mfg2-006/007/008/009/010/011/012 + mfg3-006/007/008/009/010/011/012 + crm2-009/010/011/012）

**建议基类化方案**：F3.1 专项修复批（**已 todo**）——需 IUserContext.getOrgId() 接入 + IQueryTransformer 接入 + 12 查询补 filter。

### 2.4 cron 键漂移（同型 簇 4）

**已记录 finding**（P1-MA2-086 等）：
- 9 域 job.yaml 双键不一致
- 6 域 cron 三层门控漂移
- 4 类 config-gated 漂移（erp-mnt.due-visit-cron 等）

**488 open 中**：
- 4 finding（aps-006/007/008 + crm-008/009/010 + cs-013/014 + notify-006/007/008 + mfg-008/009/010 + ast-006/007/008 + log-006/007/008/009 + drp-013/014/015/016/017/018/019/020 + hr-013/014/015/016/017 + crm2-013/014/015/016/017/018/019/020 + b2b-010/011/012/013/014/015/016 + fin-016/017/018/019 + fin2-010/011/012/013/014/015/016/017 + fin3-011/012/013/014/015/016 + fin4-012/013/014/015/016/017/018/019/020/021 + cs-015/016/017/018/019/020/021/022 + inv-017/018/019/020/021 + mfg-012/013/014/015/016/017/018/019/020/021 + mfg2-013/014/015/016/017/018/019/020/021/022/023 + mfg3-013/014/015/016/017/018 + mnt-010/011/012/013/014/015/016/017/018/019 + pur-010/011/012/013/014 + sal-021/022/023/024/025/026/027/028/029/030 + qa-015/016/017/018/019/020/021/022/023/024/025 + prj-015/016/017/018/019/020/021 + md-006/007/008/009/010/011/012/013/014 + common-002/003/004/005/006/007/008/009/010）

**F3.2 专项修复批**（已 todo）。

### 2.5 currentUserId 宽 catch（同型 簇 5）

**488 open 中**（跨域 5+ finding）：
- P3-CK-md-008 / P3-CK-pur-010 / P3-CK-mfg2-013 / P3-CK-mnt-008 / P3-CK-crm-012 / P3-CK-hr-008 / P3-CK-cs-016 / P3-CK-b2b-013

**F3.4 专项修复批**（已 todo）。

### 2.6 notify 模板种子缺失（同型 簇 6）

**488 open 中**：
- notify-001/002 已记录（3 库缺 ACTIVE 模板）
- 6 域联动：aps-006 + log-002 + mnt-012 + cs-013 + prj-012 + qa-013

**F3.3 专项修复批**（已 todo）。

### 2.7 dict 死状态（同型 簇 7）

**488 open 中**：
- crm/crm2/ct/b2b/qa/mnt/fin2/hr2 多域死状态 finding

**建议**：M1 切片重扫统一裁决（删死状态 or 移 Deferred）。

## 三、跨 mission finding 索引（按域 × 优先级）

按 M0.3 域分布 + 严重性 + 同型合并基类，**488 open finding 推荐修复优先级**：

| 优先级 | 修复批 | 涉及域 | open finding 数估计 | 难度 | 跨 mission 状态 |
|---|---|---|---|---|---|
| **P0** | F2.4 finance-期间 P1 簇 | fin4 | 21 | 中（3 finding，需 mvn test）| **ready** 唯一项 |
| P1 | F2.5 mfg-工单 P1 簇 | mfg | 19 | 中 | todo |
| P1 | F2.6 mfg-BOM/MRP P1 簇 | mfg2 | 22 | 高 | todo |
| P1 | F2.7 mfg-委外 P1 簇 | mfg3 | 17 | 中 | todo |
| P1 | F2.8 assets-生命周期 P1 簇 | ast | 25 | 中 | todo |
| P1 | F2.9 assets-折旧 P1 簇 | ast2 | 21 | 高 | todo |
| P1 | F2.10 sales+purchase P1 簇 | sal+pur+md | 54 | 高 | todo |
| P1 | F2.11 inventory P1 簇 | inv | 18 | 中 | todo |
| P1 | F2.12 prj+qa P1 簇 | prj+qa | 41 | 高 | todo |
| P1 | F2.13 mnt+hr P1 簇 | mnt+hr+hr2 | 58 | 高 | todo |
| P1 | F2.14 crm+cs P1 簇 | crm+crm2+cs | 58 | 中 | todo |
| P1 | F2.15 ct+b2b+drp+log+aps+notify P1 簇 | 6 域 | 86 | 高 | todo |
| P2 | F3.1 orgId 隔离族 | 跨域 | 30+ | 高 | todo |
| P2 | F3.2 cron 键漂移 | 9 域 | 100+ | 中 | todo |
| P2 | F3.3 notify 模板种子 | 6 域 | 7 | 中 | todo |
| P2 | F3.4 currentUserId 宽 catch | 跨域 | 8 | 中 | todo |
| P2 | F3.5+ 各域 P2 簇 | 跨域 | 195 | 中-高 | todo |
| P3 | F3.y P3 波次 | 跨域 | 210 | 中 | todo |
| **合计** | — | — | **488** | — | — |

## 四、M1 切片执行优先级（基于维度分布）

按 D6/D8 占比最大（21.5%），M1 切片优先聚焦**业务计算正确性 + 数据一致性**域：
- **M1.1-M1.4 finance 4 切片**（fin D6/D8 主力，fin-006/007/008 等 + fin4-001~003 已 ready）
- **M1.5-M1.7 manufacturing 3 切片**（mfg/mfg2/mfg3 D6/D9 N+1 + D8 跨域反写）
- **M1.8-M1.9 assets 2 切片**（ast/ast2 D6 折旧计算 + D8 净值/累计折旧）
- **M1.10-M1.11 hr 2 切片**（hr/hr2 D6 工资多币种 + D5 守卫）
- **M1.12-M1.15 4 切片**（残余域）

## 五、增量产物（在本 M0.5 落盘）

- 本文件（M0.5 维度分布 + 同型模式交叉分析 + M1 切片优先级）
- 配合 M0.1/M0.2/M0.3/M0.4 + ai-check-r2-index.md 共同构成 M0 阶段完整产出

## 多次执行隔离

本文件落盘于 `docs/audits/check/2026-08-28-2049-ai-check-r2/m0-5-dimension-and-cross-pattern-analysis.md`（不写到 `docs/audits/check/` 扁平空间）。
