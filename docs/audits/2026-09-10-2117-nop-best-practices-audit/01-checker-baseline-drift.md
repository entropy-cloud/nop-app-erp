# 01 — Compliance 基线漂移 + 时钟违规（事实性发现）

> 复跑命令：`bash docs/audits/nop-compliance-checker.sh`（2026-09-10 21:17，HEAD=dc31a2555）
> 对照基线：`docs/audits/compliance-baseline.md §BASELINE (machine-readable)`（末次裁决 = plan 2026-09-09-2100-1，锚点 commit `060ddab0a`）

## 1. 复跑结果 vs 基线（19 规则）

| 规则 | 基线 | 实测 | 漂移 | 状态 |
|------|------|------|------|------|
| R1a dao().saveEntity (BizModel) | 0 | 0 | — | ✅ |
| **R1b dao().updateEntity (BizModel)** | **0** | **1** | **+1** | ❌ |
| R1c dao().getEntityById (BizModel) | 0 | 0 | — | ✅ |
| **R1d dao().findAllByQuery (BizModel)** | **14** | **15** | **+1** | ❌ |
| R2a BizModel daoFor(ErpMd*) | 34 | 34 | — | ✅ |
| R2b BizModel daoFor(Erp*) | 242 | 242 | — | ✅ |
| **R2c 全生产代码 daoFor() 总量** | **1542** | **1543** | **+1** | ❌ |
| R2d Processor daoFor(ErpMd*) | 38 | 38 | — | ✅ |
| R3 new Erp*() 构造实体 | 5 | 5 | — | ✅ |
| R4 extends RuntimeException | 0 | 0 | — | ✅ |
| R5 @Inject private | 0 | 0 | — | ✅ |
| R6 @Transactional in BizModel | 2 | 2 | — | ✅ |
| R7 System.currentTimeMillis() | 0 | 0 | — | ✅ |
| R8 孤儿 Processor | 0 | 0 | — | ✅ |
| R10 REQUIRES_NEW | 14 | 14 | — | ✅ |
| R11 Processor 重复状态判断方法 | 0 | 0 | — | ✅ |
| R12a/b/c 共享内核 import | 71/66/42 | 71/66/42 | — | ✅ |

## 2. 漂移站点 per-site 证据（3 处，单一提交源）

漂移源全部来自 commit `48b57cd06`（plan `2026-09-10-0705-3` M2.5：P2-CK-pur-015-r3 核销 docStatus 守卫 + P2-CK-inv-012-r3 序列号出库守卫——今日活跃 mission ai-check-r3 的计划）。

### 2.1 [major] R1b — `ErpInvSerialNumberBizModel.java:56`

```java
dao().updateEntity(sn);   // 应为 updateEntity(sn, null, context)（CrudBizModel 管道方法，含权限/审计）
```

文件：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvSerialNumberBizModel.java`。
违反 `safe-api-reference.md` 写操作表（BizModel 内已拿到实体后更新应走 `updateEntity(entity, actionName, context)`）。

### 2.2 [major] R1d — `ErpInvSerialNumberBizModel.java:65`

```java
List<ErpInvSerialNumber> list = dao().findAllByQuery(q);   // 应为 findList(q, null, context) / doFindList
```

同文件同方法（markOutbound）。R1d 基线 14 处均已逐站点裁决（同域只读 + 代码注释明示理由），本站点无豁免注释。

### 2.3 [major] R2c — `PaymentSettler.java`（purchase，核销 docStatus 守卫）

```java
ErpPurInvoice invoice = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
```

文件：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/PaymentSettler.java`。purchase 同域只读——形态上大概率可按先例 baseline-raise 裁决，但**未走裁决程序**（compliance-baseline.md 明文：调高基线唯一途径 = 开独立计划逐项裁决，禁止功能 PR 直接调高）。

### 2.4 已知失败模式复发判定

这是 lesson 07（Compliance 基线漂移）第 4 次复发：1057-1（2026-07-25）→ V.2（2026-07-31）→ 0651-1（2026-08-02）→ 本次。M2.5 计划尚未闭包（mission 进行中），其 closure gates 须包含 checker 复跑 + 漂移裁决（Fix 或 baseline-raise 带 per-site 证据），否则闭包审计应拒收。

**裁决路径建议**：R1b/R1d 两站点建议 **Fix**（改走 CrudBizModel 管道方法，语义等价且恢复 checker 全绿）；R2c 站点可 Fix（若 `IErpPurInvoiceBiz` 可达）或 baseline-raise（同域只读先例充分）。

## 3. 时钟访问扩展排查（checker R7 未覆盖维度）

`ai-defaults.md` 硬规则：**所有获取当前时间的写法一律走 CoreMetrics**（含 `LocalDateTime.now()/LocalDate.now()/LocalTime.now()/new Date()`），否则自外于 IClock 时间线（autotest TestClock 单调递增 + 与 ORM createTime/updateTime 同源）。

### 3.1 [minor→建议随 M2.5 一并修] `ErpB2bOnboardingMonitorJob.java:220`

文件：`module-b2b/erp-b2b-service/src/main/java/app/b2b/service/job/ErpB2bOnboardingMonitorJob.java`

```java
profile.getGoLiveDate().atStartOfDay(), LocalDateTime.now()));
```

全仓唯一一处（生产代码其余时钟访问全部走 CoreMetrics）。应改 `CoreMetrics.currentDateTime()`。该站点在测试中会导致过期判断读到真实时钟（TestClock 注入失效），与 M2.5 同 mission 的修复批次可顺带处理。

## 4. checker 覆盖盲区（本次审计补齐的证据，供 checker 演进参考）

| 盲区 | 本次实测 | 建议 |
|------|---------|------|
| `LocalDateTime.now()` 等非 System 时钟 | 1 处 | R7 扩展正则族（successor checker 校准，须独立计划） |
| 跨域**写** daoFor（R2c 只计数不分类读写） | 5 处未登记（见 `04`） | 新规则 R13：daoFor 后同语句块内 saveEntity/updateEntity/deleteEntity 且实体属他域 → 报警 |
| ORM to-one 可替代 daoFor(FK) | 6 处（见 `04` §3） | 历史上已两轮 Type-1 清零后回潮，建议纳入 checker（orm.xml 关系解析 + 站点交叉引用） |
| `*Service` 命名 | 13+1 处（见 `06`） | 新规则 R14：文件名 `*Service.java`/`*ServiceImpl.java` 计数 |
