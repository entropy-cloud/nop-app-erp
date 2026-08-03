# rc-ma1-a1-22 assets-F1 折旧引擎 需求-实现符合性审计报告

> Report Status: active
> Mission: requirement-compliance
> Work Item: A1.22（MA1 需求追踪矩阵审计 — assets-F1 折旧引擎，UC-AST-02/07/08，3 UC）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级追踪矩阵 / §2 四级分级判据 / §3 完整枚举 / §4 Q1 真相源层级 + 三判据 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 9 段报告骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 通道 / §去重协议）
> 锚点：`docs/audits/rc-requirement-baseline-inventory.md`（A1.22 UC 锚点 = UC-AST-02/07/08，覆盖率 ✅ 一致）
> L1 真相源：`docs/design/assets/use-cases.md`（机制见 `docs/design/assets/depreciation-and-posting.md` §1/§五/§十 + `docs/design/assets/state-machine.md` §1/§2/§4 — L2 设计参考，非真相源；冲突以 L1 为准）
> L5 既有证据复用：A2.10（`2026-07-28-0400-arm-ma2-assets-state-machine.md`，资产状态机 §场景 A 折旧 happy path PASS + §场景(e) 漏提补提"两条路径技术上可达" PASS + 异常路径 PASS）/ A4.3（`2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md`，折旧引擎/Processor 链路代码质量 PASS + P1-MA4-013/014/015 resolved R1.16/R2.12/R1.16）

---

## 9. 与 MA2 报告差异增量声明（前置段，对应方法论 §去重协议）

本报告**不复跑 MA2/A4.3 既有行为审计**，按 §去重协议只补"需求契约↔实际行为"差异：

- **复用 A2.10**（assets 状态机 PASS）：§场景 A 设备折旧 happy path（**PASS**：资本化建卡 IN_SERVICE → 期末批量折旧 → 折旧凭证 借折旧费用/贷累计折旧 → 折旧计划 PENDING→EXECUTED → 卡片累计折旧/净值回写，`2026-07-28-0400-arm-ma2-assets-state-machine.md:118-123`）+ §异常路径（**PASS**：已结账拒绝 `:157` / 净值低于残值 `:158` / 漏提 `:163` / 幂等 `:165` / 并发 `:164`→P1-MA2-089 resolved R1.28）+ §场景(e) 折旧漏提补提（**PASS**："两条路径技术上可达" `:163,236-241`）。折旧 happy path/残值约束/幂等/并发/批量隔离行为由 A2.10 + A4.3 证实，本切片不重测。
- **复用 A4.3**（折旧引擎/Processor 链路代码质量 PASS）：DepreciationCalculator 三方法 + 残值约束双重兜底 + 9 dispatcher tryPost 异常吞咽语义 + 红冲闭环对称 + 跨域 Facade + 异常规范化五面 PASS；P1-MA4-013（折旧 dispatcher posted=false 业财悬挂）**resolved R1.16**（告警派发 `IErpSysNotificationBiz` + owner doc 标注自愈路径）；P1-MA4-014（测试有效性）**resolved R2.12**（5 类异常路径测试全覆盖含非零残值+并发+隔离+过账悬挂）；P1-MA4-015（跨域 daoFor）**resolved 永久只读豁免**。
- **本切片只补的需求视角差异**（候选缺口 #1-#4 见 §5）：①UC-AST-07 方式B 当期一次性补提**能力完全缺失**（倾向 **P1**——会计正确性：漏提额不补致累计折旧低估/净值高估，须人工确认 product-scope 范围裁剪）/ ②UC-AST-07 方式A 反结账补提**未编排**（须复核 §4 三判据——owner doc 未标 Deferred，但 3 步链技术可达 MA2 已 PASS，倾向接受 on 技术可达 + P2 watch-only on 未编排）/ ③UC-AST-08 失败资产无独立重试 API（倾向 **P2** watch-only——手动重调 executeDepreciation 幂等可达 + 告警派发闭环）/ ④UC-AST-08 汇总成功凭证 Deferred（owner doc §十:348 显式 Deferred，复核 §4 三判据）/ ⑤残值术语核对（L1 残值 `use-cases.md:39,40` = code residualValue displayName 残值 `orm.xml:186`，**同概念同术语无 drift**，不产 finding）。

---

## 1. 需求契约原文（3 UC 验收标准逐字引用）

> 来源：`docs/design/assets/use-cases.md`（L1 权威功能契约）；机制引用 `docs/design/assets/depreciation-and-posting.md` §1.2/§1.3/§1.4/§五/§十 + `docs/design/assets/state-machine.md` §1/§2/§4（L2 设计参考，冲突以 L1 为准）。

### UC-AST-02 期末直线法折旧 — `use-cases.md:31-44`

**场景**：期末批量计提折旧,生成折旧凭证。

**可验证断言**（见 depreciation-and-posting.md §1）：
```
期末折旧任务 →
  查所有 IN_SERVICE 资产
  计算月折旧额 = (原值 - 残值) / 使用月数   (直线法)
  残值约束: 计提后净值不低于残值(低于则截断为0)
生成凭证: 借 折旧费用, 贷 累计折旧
折旧计划条目: PENDING → EXECUTED
卡片.累计折旧 += 月折旧额, 卡片.净值 -= 月折旧额
```

**涉及机制**：depreciation-and-posting.md §1.2/§1.3

### UC-AST-07 折旧漏提补提 — `use-cases.md:116-125`

**场景**：前期漏提折旧,需补提(反结账 vs 当期补提)。

**可验证断言**（见 state-machine.md §4）：
```
方式A(反结账): 反结账漏提期间 → 补提 → 重新结账(严格,影响已结账数据)
方式B(当期补提): 当期一次性补提前期漏提额(简化,不追溯)
补提凭证标注所属期间(审计)
```

**涉及机制**：state-machine.md §4、../finance/period-close.md

### UC-AST-08 期末批量折旧容错 — `use-cases.md:131-141`

**场景**：期末批量折旧时,单资产失败不影响其他资产。

**可验证断言**（见 depreciation-and-posting.md §五）：
```
批量折旧 → 并行处理各资产
单资产失败(如科目缺失) → 隔离, 不影响其他资产
汇总成功凭证, 失败资产标记待处理
失败资产可单独重试
```

**涉及机制**：depreciation-and-posting.md §五

> **L2 owner doc 锚点**（设计参考，非真相源；冲突以 L1 为准）：
> - `depreciation-and-posting.md §1.2/§1.3`：折旧执行流程 + 三方法计算规则（直线法 `(原值−残值)/使用年限/12`；双倍余额递减；工作量法）
> - `depreciation-and-posting.md §1.4`：残值约束（折旧后账面净值不得低于残值）
> - `depreciation-and-posting.md §五 :206-252`：期末批量折旧（§5.2 流程含"汇总生成批量折旧凭证（单张凭证多行分录）" + §5.3 优化含"汇总凭证：同类资产折旧合并到同一张凭证" + "错误隔离：单资产折旧失败不影响其他资产"）
> - `depreciation-and-posting.md §十 实现约定 :339-348`：显式列 Deferred/Non-Goal 项——**`:348` 批量折旧并行/汇总**："§5.2 按类别分组并行 + 汇总单张凭证多行为性能优化；基线实现为按资产串行（错误隔离）+ 每资产单张凭证，留 Follow-up"
> - `state-machine.md §1`：资产卡片 5 态 + 折旧计划条目 3 态定义
> - `state-machine.md §4 异常路径 :57-66`：折旧漏提补提（反结账补提，或在当前期间补提）+ 残值约束 + 并发折旧乐观锁 + 重复折旧幂等
> - `state-machine.md §9 场景 D :136-140`：折旧漏提补提演练（选项一反结账 6 月→补提→重新结账；选项二 7 月补提注明"补提 6 月折旧"）
> - **词汇核对**：L1 用中文"残值"（`use-cases.md:39,40`），code `ErpAstAsset.residualValue`（`app-erp-assets.orm.xml:186`，propId=10，displayName="残值"）——**同概念同中文术语，无词汇漂移**（Phase 1 逐字引用确认）。L2 owner doc 部分段落用 `salvageValue` 英文别名（如 `depreciation-and-posting.md §1.4`），但 ORM 权威名 `residualValue` + displayName "残值" 与 L1 一致；L2 英文别名属设计草稿遗留非真相源分歧（不产 drift finding，记入观察）。

---

## 2. 实现代码路径（L3 含行号 + 跨域调用链）

> 实仓源：`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/`。跨域 Facade：`IErpFinVoucherBiz`（post/reverse 经 `AssetPostingExecutor`）/ `IErpFinAccountingPeriodBiz`（继承 `IErpFinPeriodCloseBiz.reverseClose`，仅 finance 侧，assets 未编排）/ `IErpSysNotificationBiz`（失败告警）。

### UC-AST-02 期末直线法折旧

| 组件 | 文件:line | 职责 |
|------|----------|------|
| 折旧引擎入口（Facade） | `entity/ErpAstDepreciationScheduleBizModel.java:26-75`（`@BizModel("ErpAstDepreciationSchedule")` extends CrudBizModel，暴露 `executeDepreciation:46-52`/`executeBatchDepreciation:54-58`/`reverseDepreciation:60-66`/`recalculateForCapitalizationMaintenance:68-74` 四 `@BizMutation`）| Facade 入口/事务边界，委托 per-mutation Processor（R6.3 拆分） |
| 单资产折旧计提 | `processor/ErpAstDepreciationScheduleExecuteDepreciationProcessor.java:38-121`（`executeDepreciation` 编排：requireAsset+validateAssetInService+requirePeriodOpen `:39-41` → 方法/月数解析 `:43-48` → 幂等前置红冲 `:54-57` → restore 旧值 `:59-65` → elapsed 计算 `:67` → Calculator 调用 `:71-73` → newAccum/newNbv `:75-76` → 计划条目 PENDING→EXECUTED `:91-92` + executedAt `:92` → UK 守卫 `:96-109` → 资产回写 `:98-100` → tryPost `:111` → posted=true+voucherId `:113-119`）| 单资产折旧完整编排 |
| 直线法公式（纯函数） | `service/DepreciationCalculator.java:18-80`（`calculate` 静态方法；直线法分支 `(original − residual) / months` SCALE=4 HALF_UP `:63-66`；双倍余额递减 `:40-52`；工作量法 `:54-62`）| 三方法折旧额计算 |
| 残值约束（双重兜底） | `DepreciationCalculator.java:32-35`（nbv ≤ residual 提前返回 ZERO）+ `:70-73`（post-compute 截断 if nbv−amount < residual then amount=nbv−residual）+ `:74`（负结果 clamp ZERO）| 折旧后净值不低于残值 |
| 折旧凭证过账派发 | `posting/DepreciationPostingDispatcher.java:53-68`（`tryPost`：buildEvent `:54` → executor.postEvent `:56` → catch 吞异常返回 null 保持 posted=false `:57-67`）+ `buildEvent:110-135`（businessType=`ErpFinBusinessType.DEPRECIATION`=70 `:113` + billHeadCode=assetCode#period `:114` + voucherDate=schedule.businessDate `:119-121`）| DEPRECIATION 业财过账事件组装 + 幂等键 |
| 折旧凭证科目分解 | `posting/DepreciationAcctDocProvider.java:26-87`（supports DEPRECIATION `:41-43`；createFacts `:46-55`：**借 6602 折旧费用 / 贷 1602 累计折旧** `:52-53`；subject fallback 6602/1602 `:31-32`；ACCOUNT_KEY_DEPRECIATION_EXPENSE/ACCUMULATED_DEPRECIATION `:37-38`）| 借折旧费用/贷累计折旧凭证结构 |
| 跨域过账 Facade | `posting/AssetPostingExecutor`（REQUIRES_NEW → `IErpFinVoucherBiz` post/reverse）| 跨域失败隔离 + 凭证持久化 |
| 计划条目迁移 + 卡片回写 | `ErpAstDepreciationScheduleExecuteDepreciationProcessor:91-94`（PENDING→EXECUTED + executedAt `:92`）+ UK_AST_DEPRECIATION_ASSET_PERIOD 守卫 `:96-109`（catch → ERR_AST_DEPRECIATION_ALREADY_EXECUTED）+ re-execute restore `:59-65`（减旧 actualAmount、加回 nbv）+ newAccum/newNbv `:75-76` + 回写 setAccumulatedDepreciation+setNetBookValue+saveOrUpdate `:98-100` + posted=true+voucherId `:111-119` | PENDING→EXECUTED + 卡片累计折旧/净值回写 |
| 共享 helper 单一真相源 | `processor/ErpAstDepreciationScheduleProcessor.java:42-151`（delete-after-extract facade，持有 protected helper：requireAsset/validateAssetInService/requirePeriodOpen/findSchedule/countExecuted/periodFirstDay `:56-131`）| R6.3 拆分后共享 helper |

### UC-AST-07 折旧漏提补提

| 组件 | 文件:line | 职责 |
|------|----------|------|
| 单资产折旧（接受任意 period） | `ErpAstDepreciationScheduleExecuteDepreciationProcessor.executeDepreciation:38-121`（接受任意 `period` 参数，无前置必须等于当前期间校验——用户可在任何 OPEN 期间触发折旧）| 方式A/方式B 共用入口；`elapsed = countExecuted(assetId) - (wasExecuted?1:0) :67`——**每次仅计算单月折旧**（elapsed 来自 EXECUTED 行计数 `ErpAstDepreciationScheduleProcessor.countExecuted:126-131`），无多月补提循环 |
| 期间 CLOSED 守卫 | `ErpAstDepreciationScheduleProcessor.requirePeriodOpen:76-87`（查 `ErpFinAccountingPeriod` status，非 OPEN 抛 `ERR_DEPRECIATION_PERIOD_CLOSED`——错误消息含"已结账，不允许补提折旧" `ErpAstErrors.java:94-97`）| 已结账期间拒绝折旧（方式A 须先反结账） |
| **方式A 反结账（跨域 Facade，未编排）** | `IErpFinPeriodCloseBiz.reverseClose`（`module-finance/erp-fin-dao/.../IErpFinPeriodCloseBiz.java:45`，经 `IErpFinAccountingPeriodBiz` 继承）+ finance 实现 `ErpFinAccountingPeriodBizModel.reverseClose:70-71`（委托 `ErpFinAccountingPeriodReverseCloseProcessor`）| **assets 域零调用**（grep `reverseClose` module-assets 0 命中）——3 步链（finance.reverseClose + ast.executeDepreciation + finance.closePeriod）技术可达但**未编排**为一等公民能力 |
| **方式B 当期一次性补提（完全缺失）** | grep `补提\|catchUp\|backfill\|reversePeriod` 全 `module-assets` 生产代码 → **仅 `ErpAstErrors.java:96` 错误消息串**（"折旧期间 {period} 已结账，不允许补提折旧"，仅拒绝消息无逻辑），catchUp/backfill/reversePeriod **零生产匹配** | **方式B 能力完全缺失**——`executeDepreciation` 每次 Calculator 计算单月折旧，2026-05 漏提、2026-07 调用仅产生 2026-07 单月额而非两月补提额；无 `catchUp`/`backfill` mutation |
| 补提凭证归属期间标注 | `DepreciationPostingDispatcher.buildEvent:119-121`（`voucherDate = schedule.getBusinessDate() != null ? schedule.getBusinessDate() : CoreMetrics.today()`）+ `:114 billHeadCode = assetCode#period`（period 标签携带）| 凭证 voucherDate = schedule.businessDate（= `periodFirstDay(period)` `ErpAstDepreciationScheduleExecuteDepreciationProcessor:85`），期间标签携带但无显式"补提"marker |

### UC-AST-期末批量折旧容错

| 组件 | 文件:line | 职责 |
|------|----------|------|
| 批量折旧入口 | `processor/ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor.java:37-55`（`executeBatchDepreciation`：requirePeriodOpen `:38` → 查 IN_SERVICE 资产 `:40-43` → **per-asset try/catch 隔离** `:46-53`（失败 LOG.warn 跳过）→ 返回 processed 计数 `:54`）| 批量折旧 + 错误隔离 |
| 批量隔离（per-asset try/catch） | `ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor:46-53`（`for (asset : assets) { try { executeDepreciationProcessor.executeDepreciation(...); processed++; } catch (Exception e) { LOG.warn("批量折旧：资产 {} 期间 {} 计提失败，跳过", ...); } }`）| 单资产失败不影响其他资产 |
| 失败告警派发 | `DepreciationPostingDispatcher.dispatchFailureAlert:74-92`（`tryPost` 失败时 `:65` 派发 `IErpSysNotificationBiz.notify("ast.depreciation-posting-failure", ctx, serviceCtx)` `:87`，event key `ast.depreciation-posting-failure` `:46`；通知失败降级 warn 不阻断 `:88-91`）| 失败资产标记待处理 + 运营感知告警（R1.16 resolved P1-MA4-013） |
| **失败资产独立重试 API（缺口 #3）** | **无独立重试 API/队列**——仅手动重调 `executeDepreciation(assetId, period)`（幂等：posted=true 时前置红冲再重生成 `:54-57`；posted=false 时重算+重试过账 `:111`）| 手动幂等可达，无 dedicated retry API |
| **汇总成功凭证（缺口 #4，owner doc Deferred）** | **每资产单张凭证**——`executeBatchDepreciation:46-53` 逐资产调 `executeDepreciation`，每资产独立生成 DEPRECIATION 凭证（billHeadCode=assetCode#period `DepreciationPostingDispatcher:114`）；**无"汇总单张凭证多行分录"聚合** | owner doc §十:348 显式 Deferred："基线实现为按资产串行（错误隔离）+ 每资产单张凭证，留 Follow-up" |

---

## 3. 测试断言证据（L4 注明断言强度）

> 测试源：`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/`。强度评级对齐 MA5（A5.5 assets 测试覆盖）。**无折旧独立 E2E**（`tests/e2e/` grep `depreciation` 仅处置/资本化流）。

### UC-AST-02 期末直线法折旧

| 测试文件#方法 | 覆盖断言 | 断言强度 | 证据摘要 |
|--------------|---------|---------|---------|
| `TestErpAstDepreciation#testStraightLinePerPeriodEqualAndLastToResidual:62-98` | 直线法每期等额 + 末期到残值 + DEPRECIATION 凭证 + 累计折旧/净值收敛 | **强** | 12 期每期 assertEquals(actualAmount, 1000) + Σ=12000 + asset.accumulatedDepreciation=12000 + asset.netBookValue=ZERO + schedule.posted=true + DEPRECIATION 凭证回链非空 |
| `TestErpAstDepreciation#testDoubleDecliningResidualConstraint:100-128` | 双倍余额递减 + 残值约束（48 期净值收敛到残值）| **强** | 首期 DDB=2000 + 48 期全 actualAmount≥0 + nbv≥0 + 末期满寿命 netBookValue≤1（容许舍入误差收敛残值 0）|
| `TestErpAstDepreciation#testNonZeroResidualStraightLineClampsToResidual:333-368` | 非零残值（残值=2000）直线法截断分支 | **强** | 期 1/2 等额 2666.6667 + 期 3 截断分支触发 amount=2666.6666（=nbv−残值）+ 末期净值精确=残值 2000 + 累计折旧=原值−残值=8000（闭合 P1-MA4-014(e) 残值边界）|
| `TestDepreciationCalculator`（3 @Test 纯函数）| 直线法/双倍余额递减/工作量法纯函数算术 + 残值 clamp 收敛 | **强** | 纯函数覆盖三方法 + 残值约束边界 |
| `PropertyErpAstDepreciationResidual`（jqwik 3 @Property 100 tries + 2 @Test）| 直线/余额递减不破残值不变式（属性测试）| **强** | 100 tries 随机输入验证残值不变式 |
| 凭证借贷方向（借 6602 / 贷 1602）| DEPRECIATION 凭证结构 | **强**（A4.3 + 单测间接覆盖）| `DepreciationAcctDocProvider:52-53` 借 6602/贷 1602 + `TestErpAstDepreciation#testStraightLinePerPeriodEqualAndLastToResidual:97` DEPRECIATION 凭证回链非空 + A4.3 凭证结构 PASS |
| `TestErpAstAcctDocProviderAccountKey` | ACCOUNT_KEY_DEPRECIATION_EXPENSE/ACCUMULATED_DEPRECIATION | **强** | 科目映射键常量断言 |

### UC-AST-07 折旧漏提补提

| 测试文件#方法 | 覆盖断言 | 断言强度 | 证据摘要 |
|--------------|---------|---------|---------|
| `TestErpAstDepreciation#testPeriodControlRejectsClosedAndMissing:163-186` | 已结账期间拒绝 + 未找到期间拒绝 | **强** | assertThrows(ERR_DEPRECIATION_PERIOD_CLOSED) for 2026-05 CLOSED + assertThrows(ERR_DEPRECIATION_PERIOD_NOT_FOUND) for 2099-01 |
| `TestErpAstDepreciation#testIdempotentReExecuteReversesAndRegenerates:188-221` | 同期间重复执行幂等（红冲+重生成）| **强** | 3 条回链（原始已红冲+红字冲销+重生成）+ 恰 1 active 未红冲凭证 + 累计折旧不双计（仍 1000）|
| **方式B 当期一次性补提（缺口 #1）** | 多月漏提一次性补提 | **零测试**（功能缺失导致无可测路径）| grep `catchUp\|backfill\|补提` 全测试代码仅 `TestErpAstDepreciation:177` 注释串（拒绝消息验证），无方式B 补提语义测试 |
| **方式A 反结账补提编排（缺口 #2）** | reverseClose + 补提 + re-close 3 步链 | **零测试**（assets 域零 reverseClose 调用）| finance 侧 `TestErpFinReverseClose` 测试 reverseClose 本身，但 assets 域无 3 步链编排测试 |
| 补提凭证归属期间标注 | voucherDate = schedule.businessDate | **行为 PASS**（代码阅读 + 间接覆盖）| `buildEvent:119-121` voucherDate=businessDate + billHeadCode=assetCode#period；无独立"补提 voucherDate 归属漏提期 vs 当期"断言 |

### UC-AST-08 期末批量折旧容错

| 测试文件#方法 | 覆盖断言 | 断言强度 | 证据摘要 |
|--------------|---------|---------|---------|
| `TestErpAstDepreciation#testBatchDepreciationProcessesAllAssets:130-161` | 批量折旧处理全部 IN_SERVICE 资产 | **强** | processed=2 + 2 资产 EXECUTED+posted + exact 1000/500 月折旧 |
| `TestErpAstDepreciation#testBatchDepreciationIsolatesFailingAsset:232-272` | 批量错误隔离（失败资产跳过，正常资产不受影响）| **强** | seed 孤儿 EXECUTED+posted 计划（无凭证回链）→ 批量重执行红冲硬前置失败被 try/catch 隔离 → processed=1（仅正常资产）+ 正常资产 EXECUTED+posted+voucherId + 失败资产保持种子态（闭合 P1-MA4-014(c)）|
| `TestErpAstDepreciation#testConcurrentFirstDepreciationNoDuplicate:374-433` | 并发首次折旧 UK 兜底（2 线程 ExecutorService+CountDownLatch）| **强** | 恰 1 条 active schedule 行 + 累计折旧不双计（1000）+ 冲突方抛 ERR_AST_DEPRECIATION_ALREADY_EXECUTED 或乐观锁冲突（闭合 P1-MA2-089 / P1-MA4-014(b)）|
| `TestDepreciationPostingFailureAlert`（2 @Test）| 过账失败告警派发 + null graceful | **强** | 告警派发到 IErpSysNotificationBiz + notificationBiz=null 时 graceful 跳过（闭合 P1-MA4-013 测试可见性 + P1-MA4-014(d)）|
| `TestErpAstDepreciation#testDepreciationTryPostFailureLeavesSuspendedThenSelfHeals:282-323` | tryPost 悬挂 + 重跑自愈 | **强** | 缺 GL 科目→posted=false 持久 + voucherId=null → 补 seed 6602/1602 → 重跑 executeDepreciation → posted=true + voucherId 非空 + 累计折旧不双计（闭合 P1-MA4-014(d)）|
| **失败资产独立重试 API（缺口 #3）** | dedicated retry API/队列 | **零测试**（API 不存在）| 仅手动重调 executeDepreciation 幂等路径经 testIdempotentReExecute + testDepreciationTryPostFailureLeavesSuspendedThenSelfHeals 间接覆盖 |
| **汇总成功凭证（缺口 #4）** | 单张凭证多行分录聚合 | **零测试**（owner doc Deferred）| 每资产单张凭证，无聚合凭证测试 |

**测试缺口汇总**：
- 方式B 当期一次性补提（零测试，功能缺失 → 下游 P1-RC-029 新建）
- 方式A 反结账补提 3 步链编排（零测试，assets 域零 reverseClose 调用 → 技术可达 MA2 PASS，编排缺口记入 §5 观察）
- 失败资产独立重试 API（零测试，API 不存在 → 下游 P2-RC-025 新建 watch-only）
- 汇总成功凭证（零测试，owner doc §十:348 Deferred → 下游 P2-RC-026 watch-only）

---

## 4. 运行时行为证据（L5 — 复用 MA2 + 单测）

### UC-AST-02 期末直线法折旧

| 行为 | 证据来源 | 结论 |
|------|---------|------|
| 直线法公式 `(原值−残值)/使用月数` | `DepreciationCalculator.calculate:63-66` 直线法分支 + `TestErpAstDepreciation#testStraightLinePerPeriodEqualAndLastToResidual` 12 期每期 1000 强断言 | **行为已证实**（单测层强覆盖 + A2.10 §场景 A PASS `:118-123`）|
| 残值约束（计提后净值不低于残值，低于则截断为 0）| `DepreciationCalculator:32-35`（nbv≤residual 返 ZERO）+ `:70-73`（截断 amount=nbv−residual）+ `:74`（负 clamp ZERO）+ `testNonZeroResidualStraightLineClampsToResidual` 残值=2000 截断强断言 + `PropertyErpAstDepreciationResidual` 100 tries 属性测试 | **行为已证实**（双重兜底 + 非零残值 + 属性测试三重覆盖；A2.10 异常路径"净值低于残值"PASS `:158`）|
| 凭证借贷（借 折旧费用 / 贷 累计折旧）| `DepreciationAcctDocProvider.createFacts:52-53` 借 6602/贷 1602 + A4.3 凭证结构 PASS + `testStraightLinePerPeriodEqualAndLastToResidual:97` DEPRECIATION 凭证回链非空 | **行为已证实**（A4.3 + 单测层间接证实）|
| 计划条目 PENDING→EXECUTED + 卡片回写 | `ErpAstDepreciationScheduleExecuteDepreciationProcessor:91-100`（setStatus EXECUTED + executedAt + setAccumulatedDepreciation + setNetBookValue + saveOrUpdate）+ `testStraightLinePerPeriodEqualAndLastToResidual:91-93` asset.accumulatedDepreciation=12000 + netBookValue=ZERO 强断言 | **行为已证实**（A2.10 §场景 A PASS + 单测层强覆盖）|
| 期间 CLOSED 拒绝折旧 | `ErpAstDepreciationScheduleProcessor.requirePeriodOpen:76-87` + `testPeriodControlRejectsClosedAndMissing:178-180` assertThrows(ERR_DEPRECIATION_PERIOD_CLOSED) | **行为已证实**（A2.10 异常路径"已结账拒绝"PASS `:157`）|
| 幂等重执行（红冲+重生成）| `ErpAstDepreciationExecuteDepreciationProcessor:54-57`（posted=true 时前置红冲）+ `testIdempotentReExecuteReversesAndRegenerates` 3 回链+1 active 强断言 | **行为已证实**（A2.10 异常路径"幂等"PASS `:165`）|

### UC-AST-07 折旧漏提补提

| 行为 | 证据来源 | 结论 |
|------|---------|------|
| executeDepreciation 接受任意 period（方式A/方式B 共用入口技术可达）| `ErpAstDepreciationScheduleExecuteDepreciationProcessor:38` 无前置必须等于当前期间校验 + A2.10 §场景(e) PASS："两条路径技术上可达" `:163,236-241` | **行为已证实（方式A 技术可达）**——3 步链各操作存在（finance.reverseClose + ast.executeDepreciation + finance.closePeriod），手动可达 |
| **方式A 3 步链编排（缺口 #2）** | grep `reverseClose` module-assets **0 命中**——assets 域无 reverseClose 调用；finance 侧 `IErpFinAccountingPeriodBiz.reverseClose:70-71` 存在但 assets 未编排 | **行为缺失（编排层）**——3 步链技术可达但未编排为一等公民能力（MA2 PASS on 技术可达；RC 视角：编排缺口属 P2 watch-only 操作便利性，非能力缺失）|
| **方式B 当期一次性补提（缺口 #1）** | grep `catchUp\|backfill\|reversePeriod` 生产代码 **0 匹配**（仅 ErpAstErrors.java:96 消息串）；`executeDepreciation:67 elapsed = countExecuted - (wasExecuted?1:0)` 每次 Calculator 计算单月折旧；2026-05 漏提+2026-07 调用仅产生 2026-07 单月额而非两月补提额 | **行为缺失（能力层）**——方式B "当期一次性补提前期漏提额"完全不可能（无 catchUp mutation + executeDepreciation 单月语义）|
| 补提凭证归属期间标注 | `DepreciationPostingDispatcher.buildEvent:119-121` voucherDate=schedule.businessDate（=periodFirstDay(period)）+ `:114` billHeadCode=assetCode#period | **行为已证实（部分）**——期间标签携带（voucherDate + billHeadCode），但无显式"补提"marker（voucherType/remark 未标注"补提"）；满足 L1 "补提凭证标注所属期间"基本语义（期间可追溯），但审计维度标注弱（无显式"补提"flag）|

### UC-AST-08 期末批量折旧容错

| 行为 | 证据来源 | 结论 |
|------|---------|------|
| 批量查 IN_SERVICE 资产 | `ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor:40-43`（`eq("status", ASSET_STATUS_IN_SERVICE)`）+ `testBatchDepreciationProcessesAllAssets` processed=2 强断言 | **行为已证实** |
| 单资产失败隔离（不影响其他资产）| `ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor:46-53` per-asset try/catch + `testBatchDepreciationIsolatesFailingAsset` processed=1（失败跳过，正常资产 EXECUTED+posted）强断言 | **行为已证实**（A2.10 + R2.12 P1-MA4-014(c) 闭合）|
| 失败资产标记待处理 + 告警派发 | `DepreciationPostingDispatcher.dispatchFailureAlert:74-92`（派发 IErpSysNotificationBiz `ast.depreciation-posting-failure`）+ `TestDepreciationPostingFailureAlert` 2 @Test 强断言 | **行为已证实**（R1.16 resolved P1-MA4-013——告警闭环）|
| 失败资产可单独重试（手动幂等）| `executeDepreciation` 幂等（posted=true 红冲重生成 `:54-57`；posted=false 重算重试 `:111`）+ `testDepreciationTryPostFailureLeavesSuspendedThenSelfHeals` 自愈强断言 | **行为已证实（手动幂等路径）**——手动重调 executeDepreciation 可重试 |
| **失败资产独立重试 API（缺口 #3）** | **无 dedicated retry API/队列**——仅手动重调 executeDepreciation | **行为缺失（API 层）**——手动幂等可达（行为等价），缺 dedicated retry API 属 P2 watch-only 操作便利性 |
| **汇总成功凭证（缺口 #4，owner doc Deferred）** | 每资产单张 DEPRECIATION 凭证（billHeadCode=assetCode#period 独立）+ 无"单张凭证多行分录"聚合 | **行为缺失（性能优化层，owner doc §十:348 Deferred）**——GL 正确性不受影响（每资产凭证借贷平衡），仅凭证数量多（性能/存储优化 Deferred）|

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论）

### 5.1 五级追踪矩阵（3 UC × 5 列，逐 UC 一行）

| UC | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-AST-02** 期末直线法折旧 | `use-cases.md:31-44`（6 条断言：查 IN_SERVICE / 直线法公式 `(原值−残值)/使用月数` / 残值约束[计提后净值不低于残值，低于则截断为 0] / 凭证 借折旧费用 贷累计折旧 / 计划 PENDING→EXECUTED / 卡片累计折旧+=净值-=）— 验收标准原文见 §1 | `depreciation-and-posting.md §1.2/§1.3`（直线法 `(原值−残值)/使用年限/12` 每期相等，末期到残值）+ `§1.4`（残值约束）+ `state-machine.md §1`（状态定义）— **设计参考与 L1 一致，无冲突** | 直线法公式：`DepreciationCalculator:63-66`（`(original−residual)/months` SCALE=4 HALF_UP）；残值约束：`:32-35,70-74`（双重兜底）；凭证：`DepreciationAcctDocProvider:52-53`（借 6602/贷 1602）；计划迁移+卡片回写：`ExecuteDepreciationProcessor:91-100`；跨域：`AssetPostingExecutor`→`IErpFinVoucherBiz` REQUIRES_NEW；Facade：`ErpAstDepreciationScheduleBizModel:46-52` | `testStraightLinePerPeriodEqualAndLastToResidual`（**强**：12 期 each=1000 + Σ=12000 + accum/nbv 收敛）+ `testDoubleDecliningResidualConstraint`（**强**：48 期残值约束）+ `testNonZeroResidualStraightLineClampsToResidual`（**强**：残值=2000 截断）+ `TestDepreciationCalculator`（**强**：纯函数）+ `PropertyErpAstDepreciationResidual`（**强**：100 tries 属性）+ `TestErpAstAcctDocProviderAccountKey`（**强**）| 直线法公式 → A2.10 §场景 A PASS + 单测强覆盖；残值约束 → A2.10 异常路径 PASS + 双重兜底 + 属性测试；凭证借贷 → A4.3 PASS；计划迁移+卡片回写 → 单测强覆盖 | **接受**（§2 接受——全部 6 条验收标准在 L3-L5 各级均有证据且一致；直线法+残值+凭证+计划迁移+卡片回写行为正确，测试强含属性测试；A2.10+A4.3 双重证实）|
| **UC-AST-07** 折旧漏提补提 | `use-cases.md:116-125`（3 条断言：方式A 反结账→补提→重新结账 / 方式B 当期一次性补提前期漏提额[简化不追溯] / 补提凭证标注所属期间[审计]）— 验收标准原文见 §1 | `state-machine.md §4 异常路径 :64`（"反结账补提，或在当前期间补提[补提凭证注明归属期间]"）+ `§9 场景 D :136-140`（选项一反结账+选项二当期补提）+ `depreciation-and-posting.md §十`（**未将方式A/B 标为 Deferred**）— **设计参考与 L1 一致；§4 三判据复核：owner doc 未显式标方式A/B 为 Deferred（§十 Deferred 清单不含补提），无 §4 (ii) documented simplification** | executeDepreciation 接受任意 period：`ExecuteDepreciationProcessor:38-121`（单月计算 `:67 elapsed=countExecuted`）；期间 CLOSED 守卫：`Processor.requirePeriodOpen:76-87` + `ErpAstErrors:94-97`（"不允许补提折旧"消息）；**方式A 跨域 Facade**：`IErpFinAccountingPeriodBiz.reverseClose:70-71`（finance 侧，**assets 零调用** grep 0 命中）；**方式B**：catchUp/backfill/reversePeriod **生产代码 0 匹配**（仅 ErpAstErrors:96 消息串）；补提凭证标注：`buildEvent:119-121` voucherDate=businessDate | `testPeriodControlRejectsClosedAndMissing`（**强**：CLOSED/NOT_FOUND 拒绝）+ `testIdempotentReExecuteReversesAndRegenerates`（**强**：幂等红冲重生成）；**方式B 补提**（零测试——功能缺失）；**方式A 3 步链编排**（零测试——assets 零 reverseClose 调用） | 方式A 技术可达 → A2.10 §场景(e) PASS（"两条路径技术上可达"）；**方式A 编排** → 缺失（assets 零 reverseClose 调用）；**方式B 当期一次性补提** → **行为缺失**（executeDepreciation 单月语义，无 catchUp）；补提凭证标注 → voucherDate+billHeadCode 期间携带（部分满足，无显式"补提"marker） | **接受 on 方式A 技术可达 + 补提凭证期间标注**（§2 接受——3 步链各操作存在+MA2 PASS on 技术可达；voucherDate 携带期间可追溯）+ **P1 on 方式B 当期一次性补提缺失**（**新建 P1-RC-029**，§2 P1① 功能完全缺失 + §5 Q4 会计正确性类无例外——漏提额不补致累计折旧低估/净值高估/折旧费用低估；须人工确认 product-scope 范围裁剪）+ **P2 watch-only on 方式A 未编排**（编排缺口属操作便利性，能力存在非能力缺失，倾向 watch-only 不单列 finding）|
| **UC-AST-08** 期末批量折旧容错 | `use-cases.md:131-141`（4 条断言：并行处理各资产 / 单资产失败→隔离不影响其他 / 汇总成功凭证+失败资产标记待处理 / 失败资产可单独重试）— 验收标准原文见 §1 | `depreciation-and-posting.md §五 :206-252`（§5.2 流程含"汇总生成批量折旧凭证[单张多行]" + §5.3 优化含"汇总凭证"+"错误隔离"）+ `§十:348`（**显式 Deferred**："批量折旧并行/汇总——基线实现为按资产串行[错误隔离]+每资产单张凭证，留 Follow-up"）— **设计参考 §5.2/§5.3 与实现偏离已由 §十 Deferred 显式登记；§4 (ii) 复核见下** | 批量入口：`ExecuteBatchDepreciationProcessor:37-55`（查 IN_SERVICE `:40-43` + per-asset try/catch 隔离 `:46-53`）；失败告警：`DepreciationPostingDispatcher.dispatchFailureAlert:74-92`（IErpSysNotificationBiz `ast.depreciation-posting-failure`）；**独立重试 API**：无（手动重调 executeDepreciation 幂等）；**汇总凭证**：无（每资产单张凭证 billHeadCode=assetCode#period） | `testBatchDepreciationProcessesAllAssets`（**强**：processed=2 + exact 1000/500）+ `testBatchDepreciationIsolatesFailingAsset`（**强**：失败隔离 processed=1）+ `testConcurrentFirstDepreciationNoDuplicate`（**强**：UK 兜底）+ `TestDepreciationPostingFailureAlert`（**强**：告警派发）+ `testDepreciationTryPostFailureLeavesSuspendedThenSelfHeals`（**强**：悬挂自愈）；**独立重试 API**（零测试——不存在）；**汇总凭证**（零测试——Deferred） | 批量隔离 → A2.10 PASS + 单测强覆盖；失败告警 → R1.16 resolved P1-MA4-013；失败重试 → 手动幂等可达；**汇总凭证** → 缺失（owner doc §十:348 Deferred） | **接受 on 批量隔离 + 失败告警 + 手动重试**（§2 接受——隔离+告警+幂等重试行为正确，测试强）+ **P2 watch-only on 独立重试 API 缺失**（**新建 P2-RC-025**，§2 P2① 主路径[手动幂等可达]OK 边界[dedicated API]弱）+ **P2 watch-only on 汇总凭证**（**新建 P2-RC-026**，§2 P2③ owner doc §十:348 显式 documented simplification——§4 (ii) 复核：owner doc 显式 Deferred 标注 + R6.3 plan `2026-07-31-2115-3` 独立草案审查记录存在 → §4 (i)/(ii) 满足；性能优化类非会计正确性 → Q4 无例外不适用）|

### 5.2 候选缺口分级汇总（4 项 + 1 观察）

| 缺口# | UC | 描述 | 分级 | 命中判据 | finding ID |
|-------|----|------|------|---------|-----------|
| #1 | UC-AST-07 | **方式B 当期一次性补提完全缺失**（grep catchUp/backfill 生产代码 0 匹配[仅 ErpAstErrors:96 消息串]；executeDepreciation 每次 Calculator 单月计算 `elapsed=countExecuted`；2026-05 漏提+2026-07 调用仅产生 2026-07 单月额而非两月补提额；无 catchUp/backfill mutation）| **P1**（须人工确认 product-scope 范围裁剪）| §2 P1①（功能完全缺失——方式B 当期一次性补提前期漏提额能力不存在）+ §5 Q4（会计正确性类——漏提额不补致累计折旧低估/净值高估/折旧费用低估，无例外）| **新建 P1-RC-029** |
| #2 | UC-AST-07 | **方式A 反结账补提未编排**（3 步链各操作存在：finance.reverseClose + ast.executeDepreciation + finance.closePeriod，但 assets 域零 reverseClose 调用；MA2 PASS on 技术可达；编排缺口属操作便利性）| **P2 watch-only（不单列 finding）**| §2 P2①（次要验收标准——主路径[3 步手动技术可达，MA2 PASS]OK 边界[未编排为一等公民能力]弱）+ 注：§4 三判据复核——owner doc 未标方式A 为 Deferred（§十 Deferred 清单不含补提），但能力存在（3 步手动）非能力缺失 → 倾向 watch-only 观察，记入方式A 编排缺口非独立 finding | **不新建**（记入 UC-AST-07 矩阵行观察 + §7 SP-1 存疑点交 MA4 运行时确认 3 步链实际编排可达性）|
| #3 | UC-AST-08 | **失败资产无独立重试 API**（无 dedicated retry API/队列；仅手动重调 executeDepreciation 幂等 + 告警派发 `IErpSysNotificationBiz` 闭环）| **P2 watch-only**| §2 P2①（次要验收标准——主路径[手动幂等可达+告警闭环]OK 边界[dedicated retry API]弱）| **新建 P2-RC-025** |
| #4 | UC-AST-08 | **汇总成功凭证 Deferred**（每资产单张凭证；owner doc §十:348 显式"留 Follow-up"；无"单张凭证多行分录"聚合）| **P2 watch-only**（§4 (ii) 复核满足）| §2 P2③（owner doc 显式 documented simplification——§十:348 Deferred + R6.3 plan-audit 存在 + 性能优化类非会计正确性）| **新建 P2-RC-026** |
| 观察 | UC-AST-02/07 | **残值术语核对**（L1 残值 `use-cases.md:39,40` = code residualValue displayName 残值 `orm.xml:186`，同概念同术语）| **接受（不产 finding）**| 逐字引用确认无 drift（draft review 已修订，避免伪 drift finding）| **不产 finding** |

### 5.3 每 UC 总结论

- **UC-AST-02 期末直线法折旧**：
  - **接受**（§2 接受——全部 6 条验收标准在 L3-L5 各级均有证据且一致；直线法公式+残值约束+凭证借贷+计划迁移+卡片回写行为正确，A2.10 §场景 A PASS + A4.3 PASS + 单测强覆盖含属性测试 + 非零残值截断边界覆盖）

- **UC-AST-07 折旧漏提补提**：
  - **接受 on 方式A 技术可达**（§2 接受——3 步链各操作存在[finance.reverseClose + ast.executeDepreciation + finance.closePeriod]，MA2 §场景(e) PASS "两条路径技术上可达"；方式A 编排缺口属 P2 watch-only 操作便利性，能力存在非能力缺失）
  - **接受 on 补提凭证期间标注**（§2 接受——voucherDate=schedule.businessDate + billHeadCode=assetCode#period 期间标签携带可追溯；部分满足——无显式"补提"marker 但期间归属可审计）
  - **P1 on 方式B 当期一次性补提缺失**（**新建 P1-RC-029**，§2 P1① + §5 Q4——方式B "当期一次性补提前期漏提额"能力完全缺失[catchUp/backfill 0 生产匹配 + executeDepreciation 单月语义]，会计正确性类无例外；**须人工确认 product-scope 范围裁剪**——若方式B 裁剪 → §4 (iii) 改 product-scope 真相源非降级；若未裁剪 → P1 强制实现 Q4 无例外，MR1 触及 ORM 结构变更[catchUp mutation + 补提追溯逻辑] + 会计过账逻辑须 ask-first + 独立 plan-audit §5）

- **UC-AST-08 期末批量折旧容错**：
  - **接受 on 批量隔离 + 失败告警 + 手动重试**（§2 接受——per-asset try/catch 隔离 + 告警派发 IErpSysNotificationBiz[R1.16 resolved P1-MA4-013] + 手动重调 executeDepreciation 幂等自愈，测试强覆盖含隔离+并发+悬挂自愈）
  - **P2 watch-only on 独立重试 API 缺失**（**新建 P2-RC-025**，§2 P2① 主路径[手动幂等+告警]OK 边界[dedicated retry API]弱）
  - **P2 watch-only on 汇总凭证**（**新建 P2-RC-026**，§2 P2③ owner doc §十:348 显式 documented simplification + §4 (i)/(ii) 满足[R6.3 plan-audit + owner doc Deferred 标注]；性能优化类非会计正确性 → Q4 无例外不适用）

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

### 6.1 arm-index grep 比对 + 复用 or 新增裁决（§7 规则）

> 对每条候选缺口 grep arm-index assets 折旧同域同控制点后裁决（禁止未经比对新建）。**RC 系列对 assets 为零**——A1.22 为 assets 域首个 RC 切片。

| 缺口# | grep 关键词 | 既有 finding | 裁决 |
|-------|------------|-------------|------|
| #1 | 「补提」「catchUp」「backfill」「当期补提」「漏提」「UC-AST-07」「折旧漏提」 | **assets 折旧补提同控制点零命中**——既有 finding 覆盖：P1-MA4-013（折旧 dispatcher posted=false 业财悬挂，**resolved R1.16**，不同根因——013 是悬挂，本切片是补提能力完全缺失）/ P1-MA2-089（executeDepreciation 缺 PENDING 守卫并发双计，**resolved R1.28**，不同控制点——并发守卫 vs 补提能力）/ P1-MA5-011（折旧异常路径测试空洞，归并 MA4/MA2 测试层投影，不含补提能力维度）/ P2-MA4-006（dispatcher copy-paste，watch-only，不同维度）| **新建 P1-RC-029**（列明差异依据：vs P1-MA4-013 根因不同[013 悬挂 vs 本切片能力完全缺失] / vs P1-MA2-089 控制点不同[并发守卫 vs 补提能力] / vs P1-MA5-011 维度不同[测试覆盖 vs 需求契约能力]）|
| #2 | 「反结账」「reverseClose」「编排」「方式A」 | P1-RC-006（A1.6 finance UC-FIN-07 反结账审计轨迹缺失，finance 域）+ P1-MA2-020（反结账 kill-switch vs 审批流，resolved）—— **均 finance 域不同控制点**（finance 反结账审计轨迹/门控 vs assets 折旧补提编排）；assets 域零 reverseClose 编排 finding | **不新建**（方式A 编排缺口记入 UC-AST-07 矩阵行观察 + §7 SP-1 存疑点；能力存在[3 步手动技术可达 MA2 PASS]非能力缺失，倾向 watch-only 不单列 finding）|
| #3 | 「重试」「retry」「失败资产」「UC-AST-08」「批量重试」 | P1-MA4-013（折旧 dispatcher posted=false 业财悬挂，**resolved R1.16 含告警派发**，不同控制点——悬挂恢复 vs 独立重试 API）/ P2-MA4-006（dispatcher copy-paste watch-only，不同维度）| **新建 P2-RC-025**（assets 域首个批量重试 API 缺口 finding；与 P1-MA4-013 不同控制点——013 是悬挂恢复[已 resolved 告警+自愈]，本 finding 是 dedicated retry API 便利性缺失）|
| #4 | 「汇总凭证」「汇总」「single voucher」「多行分录」「批量凭证」 | P2-MA4-006（dispatcher copy-paste watch-only，不同维度）/ owner doc §十:348 显式 Deferred（本切片复核 §4 三判据）| **新建 P2-RC-026**（assets 域首个汇总凭证 finding；§4 (ii) 复核满足——owner doc §十 Deferred + R6.3 plan-audit 存在 + 性能优化类非会计正确性）|
| 观察 | 「残值」「residual」「salvage」「词汇漂移」 | L1 残值 = code residualValue displayName 残值（orm.xml:186），**同概念同术语无 drift**（draft review 已修订确认）| **不产 finding** |

### 6.2 双向可追溯（finding ↔ 修复行预留 MR0/MR1）

| Finding ID | 域 | UC | 分级 | 目标 MR | 触及保护区域 | 修复状态 |
|-----------|---|----|------|--------|------------|---------|
| `P1-RC-029` | assets | UC-AST-07 方式B | P1（须人工确认范围裁剪）| MR1（R1.0 → RC-R1.n）/ §4 (iii) product-scope 修订（若人工确认裁剪）| **是——ORM 结构变更 + 会计过账逻辑**（方式B 实现 = `ErpAstDepreciationScheduleBizModel` 增 `catchUpDepreciation(assetId, currentPeriod, missedPeriods[])` mutation + `ErpAstDepreciationScheduleExecuteDepreciationProcessor` 或新 Processor 实现多月补提循环[累加漏提期折旧额] + 补提凭证 voucherType/remark 标注"补提" + 可能新增 `ErpAstDepreciationSchedule.isCatchUp` 列；**触及 ORM 结构变更 + 会计过账逻辑[补提重算]须 ask-first + 独立 plan-audit §5**）| todo（本审计仅登记，不实施修复；**先须人工确认 product-scope 是否裁剪方式B**：若裁剪 → 按 §4 (iii) 改 product-scope 真相源非降级[需求变更]；若未裁剪 → P1 强制实现 §2 P1① Q4 会计正确性类无例外。本切片 UC-AST-07 方式B 结论为 A1.23 UC-AST-05"先补提当期折旧至出售日"前置证据）|
| `P2-RC-025` | assets | UC-AST-08 | P2 | successor watch-only（P2 登记不强制）| 否（纯 BizModel 代码逻辑——`ErpAstDepreciationScheduleBizModel` 增 `retryFailedDepreciation(assetId, period)` mutation 委托 executeDepreciation 幂等路径 + 批量返回 failed assets 列表的 query 方法；**按 roadmap 预授权类目[代码逻辑修复]可自动执行，不触发 §5 ask-first**）| todo |
| `P2-RC-026` | assets | UC-AST-08 | P2 | successor watch-only（P2 登记不强制）| 否（纯 BizModel 代码逻辑——`executeBatchDepreciation` 增 config-gated 汇总分支[同类资产折旧合并到同一张凭证多行分录]；**按 roadmap 预授权类目[代码逻辑修复]可自动执行，不触发 §5 ask-first**；性能优化类非会计正确性，owner doc §十:348 Deferred 满足 §4 (i)/(ii)）| todo |

### 6.3 §4 三判据复核结论汇总

| 缺口# | §4 (i) plan-audit | §4 (ii) owner doc Deferred | §4 (iii) product-scope 裁剪 | 复核结论 |
|-------|-------------------|---------------------------|---------------------------|---------|
| #1 方式B 补提缺失 | **无**（无独立 plan-audit 专门裁决方式B 裁剪）| **无**（owner doc §十 Deferred 清单不含补提；state-machine.md §4/§9 描述方式A/B 为并列选项未标 Deferred）| **无**（product-scope 未将方式B 列入范围裁剪）| **三判据均不满足 → 非 documented simplification** → 须按 Q4=(a) 评估：方式B 能力完全缺失 + 会计正确性类 → **P1 强制实现**（须人工确认是否裁剪，裁剪则 §4 (iii) 改真相源非降级）|
| #2 方式A 未编排 | **无**（无独立 plan-audit）| **无**（owner doc 未标方式A 为 Deferred）| **无** | **三判据均不满足 → 非 documented simplification** → 但方式A 能力存在（3 步手动技术可达 MA2 PASS）非能力缺失 → 编排缺口属 **P2 watch-only 操作便利性**（倾向不单列 finding，记入观察）|
| #3 独立重试 API | N/A（P2 不强制 §4 复核）| N/A | N/A | P2 watch-only（主路径[手动幂等+告警]OK，边界[dedicated API]弱）|
| #4 汇总凭证 Deferred | **满足**（R6.3 plan `2026-07-31-2115-3` assets 拆分含独立草案审查记录 `Draft Review Record` + §十 Deferred 标注可追溯）| **满足**（owner doc §十:348 显式 "留 Follow-up" 标注）| **无**（product-scope 未裁剪，但性能优化类非会计正确性）| **§4 (i)/(ii) 满足 → 维持 P2 watch-only**（性能优化类，Q4 无例外不适用；不重新打开为 P1）|

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行；MA4（A4.1/A4.2 展开器）运行时验证后回填结论。

- **SP-1**（#2 方式A 编排可达性）：方式A 3 步链（finance.reverseClose + ast.executeDepreciation + finance.closePeriod）在实仓的实际编排可达性及凭证期间效果——3 步均存在但 assets 域零 reverseClose 调用，需运行时确认：(a) 手动执行 finance.reverseClose(2026-05) 后 ast.executeDepreciation(assetId, "2026-05") 是否成功（期间已回 OPEN）；(b) 补提凭证 voucherDate 是否正确归属漏提期 2026-05（而非当期）；(c) 重新 closePeriod(2026-05) 后补提凭证是否被锁定。若 3 步链实际可达且凭证正确，方式A 编排缺口确认为 P2 watch-only 操作便利性；若有阻塞，升级为 P1。
- **SP-2**（#1 方式B 补提在多漏提期下的累计折旧偏差）：当前 executeDepreciation 单月语义下，若 2026-05/06 漏提、2026-07 调用 executeDepreciation(assetId, "2026-07")，实际累计折旧偏差 = 2 个月折旧额（2026-05+06 漏提额）——需运行时构造多漏提期场景断言 asset.accumulatedDepreciation 偏差量 + GL 累计折旧/折旧费用低估量（量化会计影响）。
- **SP-3**（#3 批量隔离在 GL 科目部分缺失场景的实际跳过行为）：`executeBatchDepreciation:46-53` per-asset try/catch 在 GL 科目部分缺失（如部分资产类别配置了折旧费用科目但未配置累计折旧科目）场景下，实际跳过行为是否符合预期——需运行时构造 3 资产（1 缺费用科目 + 1 缺累计折旧科目 + 1 完整）批量折旧场景，断言 processed=1 + 失败 2 资产 posted=false 保持 + 告警派发 2 次。
- **SP-4**（补提凭证显式"补提"marker）：当前补提凭证 voucherDate=businessDate + billHeadCode=assetCode#period 携带期间标签，但无显式"补提"voucherType/remark 标注——需运行时确认审计维度（按期间反查补提凭证）是否可仅凭 voucherDate 归属漏提期识别，还是需显式 marker（影响 UC-AST-07 "补提凭证标注所属期间[审计]" 验收强度的判定）。

**P0 即时通道**：本切片**未触发 P0**（最高级 = P1[方式B 补提缺失 P1-RC-029]，无活跃数据破坏 / 会计过账破坏 / 安全漏洞 / 核心循环断裂；方式B 缺失虽影响会计正确性——累计折旧/净值/折旧费用错报——但 GL 平衡不破坏故非 §2 P0④[借贷仍平衡，仅金额错报] + 漏提是低频边界场景 + 方式A 手动可达兜底 + 可经期末试算平衡人工发现；其余为 P2 watch-only 操作便利性/性能优化类）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本报告无生产代码变更**（纯审计报告 + arm-index 文档更新），checker 无回归风险。

  | 规则 | Baseline | Actual | 状态 |
  |------|----------|--------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a/R2b/R2c/R2d | 34/229/1382/34 | 34/229/1382/34 | ✅ |
  | R3 | 5 | 5 | ✅ |
  | R4/R5 | 0/0 | 0/0 | ✅ |
  | R6 | 2 | 2 | ✅ |
  | R7 | 0 | 0 | ✅ |
  | R8 | 0 | 0 | ✅ |
  | R10 | 6 | 6 | ✅ |
  | R11 | 0 | 0 | ✅ |
  | R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

  全 16 规则 actual ≤ baseline（精确匹配，0 漂移；与 A1.21 报告基线一致——本切片仅追加文档无生产代码变更）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index assets 折旧同域同控制点后给出"复用 or 新增"裁决（详见 §6.1 比对表），无未经比对直接新建的 finding。P1-RC-029（方式B 补提缺失）+ P2-RC-025（独立重试 API）+ P2-RC-026（汇总凭证）经 grep 确认为 assets 域新控制点（RC 系列对 assets 为零——A1.22 为 assets 域首个 RC 切片）；既有 P1-MA4-013/P1-MA2-089/P1-MA5-011 经 grep 确认不同根因/不同控制点/不同维度 → 新建并列明差异依据。

---

## 落盘完整性自检（§6 9 段完整性）

报告产出 agent 在落盘前自查 9 段全部存在：

- [x] §1 需求契约原文（3 UC 验收标准逐字引用）
- [x] §2 实现代码路径（含行号 + 跨域调用链）
- [x] §3 测试断言证据（注明强度）
- [x] §4 运行时行为证据（复用 MA2 + 单测）
- [x] §5 符合性结论（3 UC × 5 列矩阵 + 候选缺口 4 项+1 观察分级 + 每 UC 总结论 + §4 三判据复核汇总）
- [x] §6 与 arm-index 衔接（grep 比对 + 复用/新增裁决 + 双向可追溯 + §4 三判据复核汇总）
- [x] §7 静态存疑点清单（4 项 SP-1..SP-4 + P0 即时通道未触发声明）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + closure-audit 独立性 + 交叉去重）
- [x] §9 与 MA2 报告差异增量声明（前置段 — 复用 A2.10/A4.3 + 只补需求视角差异）

9 段齐全，落盘。

---

## 附录：A1.22 切片裁决摘要

- **新 P1（1 项）**：P1-RC-029（UC-AST-07 方式B 当期一次性补提缺失，会计正确性类 Q4 无例外，须人工确认 product-scope 范围裁剪）
- **新 P2（2 项）**：P2-RC-025（UC-AST-08 失败资产无独立重试 API，watch-only 操作便利性）/ P2-RC-026（UC-AST-08 汇总成功凭证 Deferred，§4 (i)/(ii) 满足，性能优化类）
- **不新建（1 项观察）**：方式A 反结账补提未编排（记入 UC-AST-07 矩阵行观察 + SP-1 存疑点交 MA4 运行时确认 3 步链编排可达性；能力存在[3 步手动技术可达 MA2 PASS]非能力缺失）
- **不产 finding（1 项）**：残值术语核对（L1 残值 = code residualValue displayName 残值，同概念同术语无 drift）
- **P0 即时通道**：未触发（本切片无 P0）
- **静态存疑点**：4 项（SP-1..SP-4）交 MA4 A4.1/A4.2 运行时展开
- **§4 三判据复核结论**：#1 方式B 三判据均不满足 → 非 documented simplification → P1 须人工确认范围裁剪；#2 方式A 三判据均不满足 → 非 documented simplification 但能力存在 → P2 watch-only 观察；#4 汇总凭证 §4 (i)/(ii) 满足 → 维持 P2 watch-only
- **行为接受面**：UC-AST-02 全 6 条验收标准接受（直线法+残值+凭证+计划迁移+卡片回写，A2.10+A4.3+单测强覆盖含属性测试）；UC-AST-07 方式A 技术可达接受 + 补提凭证期间标注接受；UC-AST-08 批量隔离+失败告警+手动重试接受（R1.16 resolved P1-MA4-013 告警闭环 + R2.12 resolved P1-MA4-014 测试覆盖 + R1.28 resolved P1-MA2-089 并发守卫）
- **既有 finding 衔接**：P1-MA4-013（折旧 dispatcher 悬挂，resolved R1.16）/ P1-MA4-014（测试有效性，resolved R2.12）/ P1-MA2-089（并发双计，resolved R1.28）/ P1-MA2-060（Cap/Disposal tryPost 吞咽，resolved R1.16）/ P1-MA5-011（测试空洞，归并）/ P2-MA4-006（dispatcher copy-paste，watch-only）—— 均不同根因/不同控制点/不同维度，本切片新建 finding 不复用
