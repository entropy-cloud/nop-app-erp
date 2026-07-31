# Lesson 09: 业财过账 `tryPost` 吞异常——业务侧 `posted=false` 永久悬挂无告警闭环

> **来源**：2026-07 审计-修复任务。R1.16（跨域合并 12 findings）+ MA4 P1 家族。该模式在 finance / manufacturing / assets / hr / purchase / sales / inventory / maintenance 多个域**同族复发**，由 MA2 状态机审计与 MA4 代码质量审计分别独立发现后交叉去重归并为同一根因家族。
> **适用场景**：任何「业务单据 → 业财过账 dispatcher → tryPost 包裹」的链路。特别当过账失败时业务侧状态不应进入终态、且失败需有可观测告警闭环时。
> **失败模式**：业财过账用 `try { post(...) } catch (Exception e) { log.warn(e); }` 吞掉异常——业务单据的 `posted` 标志**停留在 false**（悬挂，既非成功也非显式失败），异常被静默吞咽**不进任何告警通道**，`posted=false` 永久下沉无运维介入信号。期末结账前置检查仅扫描 PENDING 异常，IGNORED / 已吞咽的悬挂逃逸日常运营监控。

## 核心论点

业财过账的「成功 / 失败 / 待重试」三态必须有**显式闭环**：

1. **成功** → `posted=true` + 凭证生成。
2. **失败** → 异常传播或落 `posted=FAILED`（可重试）+ **告警通知**（`IErpSysNotificationBiz`）+ **不进业务终态**（保持可重试状态）。
3. **放弃处置**（IGNORED）→ 必须有处置理由 + 关联单据 visible flag + 期末结账强制复核。

`tryPost` 用宽 `catch (Exception)` 吞咽、只 `log.warn`、不通知、不改状态，是把「失败」伪装成「待重试」再悄悄遗忘——业务侧 `posted=false` 永久悬挂，是最隐蔽的数据一致性破坏（无异常抛出，silent）。

## 失败模式（典型路径）

```
1. 业务单据 approve → 触发 PostingDispatcher.tryPost()
2. 过账内部失败（凭证生成异常 / 余额不足 / 期间锁定 / NPE）
3. catch (Exception e) { log.warn(e); }   ← 吞咽，不抛出
4. 业务单据 posted 仍 = false，docStatus 可能已 APPROVED
5. 无 IErpSysNotificationBiz 告警 → 运维无信号
6. posted=false 悬挂：既不能重试（无入口标记失败），也不报错（异常已吞）
7. 期末结账 preCheck 扫 PENDING 异常 → 漏掉已吞咽的悬挂 → 结账放行 → GL 残缺
```

## 真实案例

### Case: 12 findings 同族根因（R1.16）

R1.16 合并裁决的 12 项 finding 分布在两个审计里程碑、多个域：

- **MA2 状态机侧（6 项）**：`P1-MA2-032`（finance IGNORED 凭证悬挂缺告警闭环）/ `P1-MA2-048` / `P1-MA2-060` / `P1-MA2-068` / `P1-MA2-074` / `P1-MA2-080`——各域 PostingDispatcher 的异常处理路径。
- **MA4 代码质量侧（6 项）**：`P1-MA4-001` / `P1-MA4-004` / `P1-MA4-007` / `P1-MA4-010` / `P1-MA4-013` / `P1-MA4-020`——finance/mfg/assets/hr/pur/sal/inv 过账链路 catch 宽度 + 断言强度 + 异常路径测试空洞。
- **修复（plan `2026-07-29-2322-*` R1.16）**：统一错误传播分级策略——catch 收窄 + `IErpSysNotificationBiz` 告警 + 不进终态 + 期末结账前置检查扩展（覆盖 IGNORED）。

### Case: P1-MA2-032 IGNORED 凭证悬挂

finance `ErpFinPostingException` 工作台 IGNORED 状态是「显式放弃处置」。若业务自动过账失败（凭证未创建 → 业务侧 `posted=false`）后异常被置 IGNORED，凭证**永不创建**，业务侧 `posted=false` 永久悬挂。`TestErpFinPostingExceptionNotify` 覆盖了告警通知，但告警通道仅日志/通知，**无强制处置门控**；期末结账 preCheck 只扫 PENDING 阻止结账是间接保护，日常运营中 IGNORED 凭证可静默下沉。

## 决策树：写过 catch 包裹业财过账时，问「失败后业务侧进什么状态？」

```
1. catch 块是否吞咽异常（catch 后不 rethrow / 不落 FAILED 状态）？
   → 是：禁止。进入步骤 2。
   → 否（rethrow 或落 FAILED）：正常。

2. 过账失败后，业务单据 posted 标志 + docStatus 是什么？
   → posted=false 且 docStatus=APPROVED（终态）：悬挂。禁止。改为不进终态（保持可重试）。
   → posted=FAILED 且可重试：进入步骤 3。

3. 失败是否有可观测告警闭环？
   → 仅 log.warn：不够。补 IErpSysNotificationBiz 告警。
   → 通知 + 期末结账 preCheck 扫描（含 IGNORED）：正常。
```

## 自检清单（写过业财过账 catch 后）

- [ ] catch 块**没有**宽 `catch (Exception)` 吞咽后只 `log.warn`？
- [ ] 过账失败时业务单据**不进业务终态**（保持可重试状态）？
- [ ] 失败经 `IErpSysNotificationBiz` 发出告警（非仅日志）？
- [ ] 期末结账前置检查扫描覆盖 IGNORED / 已吞咽悬挂（非仅 PENDING）？
- [ ] 是否有「过账失败 → posted 悬挂 → 运维介入 → 重试成功」的测试覆盖？
- [ ] IGNORED 状态是否强制处置理由 + 关联单据 visible flag？

## 何时复发

- 新增业财过账 dispatcher 时复制了旧的吞异常模板。
- 为「不阻断主流程」而 catch 宽异常，忘记补告警闭环。
- 异步 / 兜底重试（`ErpFinDeferredPostingRetryHelper`）路径与人工重试并发时异常处理不对称。

## 关联

- 真相源：`docs/audits/arm-index.md`（P1-MA2-032/048/060/068/074/080 + P1-MA4-001/004/007/010/013/020）
- 修复证据：roadmap R1.16（plan `2026-07-29-2322-*` 家族）done
- owner doc：`docs/design/finance/posting-log.md` + 各域 `depreciation-and-posting.md` / `payroll.md`
- 关联速查：`docs/context/project-context.md` §已知失败模式
- 平台规则：异常必须扩展 `NopException` + `ErrorCode`（见 `docs-for-ai/00-start-here/ai-defaults.md`）
