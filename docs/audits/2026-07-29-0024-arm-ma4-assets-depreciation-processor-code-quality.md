# ARM-MA4 assets 折旧引擎与 Processor 链路代码质量专属审查报告（A4.3）

> 里程碑：MA4（代码与前端质量层 / 代码实现质量维度）
> Roadmap 工作项：A4.3（assets 折旧引擎与 Processor 链路专属审计，48 Processor 全域最高密度）
> Plan：`docs/plans/2026-07-29-0024-3-audit-remediation-ma4-assets-depreciation-processor-code-quality.md`
> 行为基线：`docs/design/assets/{depreciation-and-posting,state-machine,split-merge,cip,inventory,maintenance}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/code-quality-audit-prompt.md`（7 重点领域 + P0-P3 严重性指南）
> 实仓快照：2026-07-29（HEAD `find module-assets -path "*service*" ...` = 70 文件；9 对 dispatcher↔provider 1:1 配对；核心组件全部存在）
> 裁决：**Verdict = ⚠️(P1)**——折旧引擎与 Processor 链路在**折旧算术正确性（DepreciationCalculator 三方法 + 残值约束统一兜底）/ 编排健壮性（步骤化 protected 方法 + Facade@BizMutation 事务钉 / 9 dispatcher tryPost 异常吞咽语义对齐 sales/inventory）/ 红冲闭环对称性（reverseDepreciation 守卫 EXECUTED + 回滚资产卡片）/ 跨域 Facade（9 dispatcher 经 AssetPostingExecutor→IErpFinVoucherBiz REQUIRES_NEW Facade 过账，无凭证直写）/ 异常规范化（全 NopException + ErpAstErrors erp.err.ast.* ErrorCode）**五面扎实；零 P0——无活跃数据破坏路径（折旧算术经残值约束兜底 + 累计折旧/净值数值断言测试覆盖；并发双计 P1-MA2-089 已登记 deferred 待 MR1；TOCTOU 类已登记 P0-MA2-018 deferred）。**3 项新 P1**（P1-MA4-013 折旧 dispatcher posted=false 业财悬挂无自动重试/告警——MA2 只审 Capitalization/Disposal 文档 Processor tryPost，未覆盖 Depreciation dispatcher 的 executeDepreciation→tryPost 路径[该路径无文档 Processor + 无 DeferredPostingSweepJob 扫描]；P1-MA4-014 折旧/Processor 链路测试有效性不足——异常路径零覆盖[posted=false 窗口 reverseApprove 不对称 / 并发首次折旧重复 / 批量部分失败 / 过账悬挂] + 残值边界仅 residual=0；P1-MA4-015 折旧引擎/Processor 链路跨域 daoFor 绕 I\*Biz——同 P1-MA1-022 根因在 assets 折旧/过账投影，MR1 一并裁决不重复计入 MR2）+ **2 项新 P2** watch-only（P2-MA4-006 可维护性热点合并[9 PostingDispatcher 重复模式 + 折旧方法策略可扩展性 + 48 Processor 审批轴对称性 + 批量单事务错误隔离仅覆盖早抛]；P2-MA4-007 自动化防护[compliance checker R2d 仅覆盖 daoFor(ErpMd\*) 未覆盖 daoFor(ErpFin\*) 致 ErpAstDepreciationScheduleProcessor:290 漏检 + 无折旧算术/并发/过账悬挂回归门控]）。MA1/MA2/MA3 已知 finding 运行时复核 **9 项全部「如登记」无升级**。本审计原则上**无 P0**（代码静态审查 + 测试有效性抽样，无活跃数据破坏；并发双计 + 算术误差均经现有测试/约束兜底或已登记 deferred）。

---

## 1. 范围与基线

### 1.1 在范围（代码实现质量，非状态机业务正确性）

`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/` 折旧引擎 + Processor 链路代码（70 文件，含 ~8 测试）：

| 组件 | 文件 | 职责 |
|------|------|------|
| **折旧算术** | `service/DepreciationCalculator.java` | 三方法（直线/双倍余额递减/工作量）+ 残值约束兜底，纯函数 |
| **折旧编排** | `processor/ErpAstDepreciationScheduleProcessor.java` | executeDepreciation / executeBatchDepreciation / reverseDepreciation / recalculateForCapitalizationMaintenance + protected step 校验 |
| **折旧 Facade** | `entity/ErpAstDepreciationScheduleBizModel.java` | @BizMutation 入口/事务钉，委托 Processor |
| **过账执行器** | `posting/AssetPostingExecutor.java` | 跨域经 IErpFinVoucherBiz Facade（REQUIRES_NEW） |
| **9 PostingDispatcher** | `posting/{Depreciation,Capitalization,Disposal,ValueAdjustment,AssetInventory,AssetMerge,AssetSplit,MaintenanceCapitalization,MaintenanceExpense}PostingDispatcher.java` | 组装 PostingEvent + try/catch 吞咽异常语义 |
| **9 AcctDocProvider** | `posting/{...}AcctDocProvider.java` | 科目文档构造（经 IErpFinAcctDocProvider SPI） |
| **业务实体 BizModel** | `entity/{ErpAstAssetCapitalization,ErpAstDisposal,ErpAstValueAdjustment}BizModel.java` | 业务单据 Facade |

### 1.2 不在范围（Non-Goals 见 plan）

- A2.10 assets 状态机业务正确性（done）——本审计复核其 finding 运行时状态，不重复审计
- A4.1a finance 过账 Facade（IErpFinVoucherBiz）实现质量（done）——本审计复核 assets 侧 9 dispatcher 调用点错误传播
- A4.6/A4.8 view.xml drift / A5.4 测试覆盖深度统计 / A6.x 权限注解完整性
- 代码缺陷批量修复（在 MR2/MR1）

---

## 2. 7 重点领域逐项审查

### 领域 1：架构和边界完整性（裁决：**PASS（一处已登记跨域只读，复核维持）**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 9 PostingDispatcher 经 IErpFinVoucherBiz Facade 过账（非凭证直写） | ✅ PASS | 全 9 dispatcher 经 `AssetPostingExecutor.postEvent→executor.postEvent→voucherBiz.post(event)`（`AssetPostingExecutor:27-33`）。无任何 dispatcher 直 `daoFor(ErpFinVoucher)` 写凭证。Facade 承接 REQUIRES_NEW 跨域事务隔离（`AssetPostingExecutor:15-17` javadoc 明示） |
| 折旧引擎读 ErpFinAccountingPeriod 跨域（P1-MA1-022 复核） | ⚠️ 维持 | `ErpAstDepreciationScheduleProcessor.findPeriod:289-296` `daoProvider.daoFor(ErpFinAccountingPeriod.class).findAllByQuery` **只读**查询期间状态（OPEN/CLOSED），非状态写。**复核「如 P1-MA1-022 登记」——跨域只读 DAO 治理缺陷，无活跃数据破坏**。详 P1-MA4-015 投影 |
| 9 dispatcher 读 ErpMdSubject 科目兜底解析（P1-MA1-022 复核） | ⚠️ 维持 | 全 9 dispatcher `resolveSubjectCode` 经 `daoProvider.daoFor(ErpMdSubject.class).getEntityById` 只读解析科目代码，异常路径兜底默认科目码（如 1601/1602/6602）。**复核「如 P1-MA1-022 登记」**。详 P1-MA4-015 |
| reverseDepreciation 被 finance 跨域调用点（assets 侧接待） | ✅ PASS | `ErpAstDepreciationScheduleProcessor.reverseDepreciation:154-177` 守卫 EXECUTED 前置 + 回滚资产卡片累计折旧/净值 + 设 REVERSED。finance 侧 `ErpFinAccountingPeriodProcessor:391` 经 `IErpAstDepreciationScheduleBiz.reverseDepreciation` I\*Biz 调用（非 daoFor 直写状态）。assets 侧接待实现质量正确 |
| AcctSchemaResolver 跨域（P1-MA1-022 同型） | ⚠️ 维持 | 全 9 dispatcher + `AcctSchemaResolver.resolvePrimarySchemaId` 经 daoProvider 读 ErpMdAcctSchema（同 mfg/finance 投影）。**复核「如登记」** |

**裁决**：跨域写经 I\*Biz Facade（IErpFinVoucherBiz / IErpAstDepreciationScheduleBiz）合规；跨域读经 daoFor 只读（P1-MA1-022 已登记，本审计复核确认无升级——详 P1-MA4-015）。**无新边界违规站点**。

### 领域 2：核心实现正确性（裁决：**FAIL——折旧 dispatcher posted=false 悬挂无自动重试（P1-MA4-013）+ 已登记 P1-MA2-089/060 复核「如登记」**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **DepreciationCalculator 三方法算术正确性** | ✅ PASS | 直线法 `(原值−残值)/月数`（`:65`）；双倍余额递减最后 24 月改直线 `(nbv−残值)/剩余月数`（`:46-51`）；工作量法 `(原值−残值)/总工作量×本期工作量`（`:58-60`）。三种方法**统一经残值约束兜底**（`:71-73` 若 `nbv−amount<residual` 则 `amount=nbv−residual`）。SCALE=4 HALF_UP 精度。与 owner doc §1.3 公式一致。**算术正确，直接影响财务报表的折旧额计算无缺陷** |
| **残值约束** | ✅ PASS | `DepreciationCalculator:32-34`（已达/低于残值返 0）+ `:71-73`（折旧后兜底不低于残值）双重保护。owner doc §1.4 落实 ✓ |
| **executeDepreciation 幂等重执行** | ✅ PASS | `executeDepreciation:65-81` wasExecuted+posted 时先 reverse 旧凭证再回退旧折旧对资产卡片影响后重算。幂等键 (assetId, period) 经 findSchedule 自然去重。TestErpAstDepreciation.testIdempotentReExecuteReversesAndRegenerates 覆盖（断言 3 回链 + 累计折旧不双计） |
| **executeBatchDepreciation 期间控制 + 错误隔离** | ⚠️ 部分 | `executeBatchDepreciation:132-152` 期间 OPEN 校验 + 逐资产 try/catch 错误隔离（`:146-149` LOG.warn 跳过）。owner doc §5.3「部分资产折旧失败不影响其他资产」落实。**但**：批量在单一 @BizMutation 事务内，错误隔离为「逻辑隔离」（catch 跳过），**非事务隔离**（无 per-asset REQUIRES_NEW 子事务/savepoint）——若资产 mid-flight 失败（如幂等 reverse 抛出后 catch），该资产部分 mutation 可能随主事务提交持久化。实际触发面收窄（早抛路径 requireAsset/validateAssetInService/requirePeriodOpen 在 mutation 前）。归 P2-MA4-006 watch-only |
| **并发首次折旧重复（P1-MA2-089 复核）** | ❌ 维持 | `executeDepreciation:64 findSchedule` 可能返回 null → `:95-102 if(schedule==null) newEntity+saveOrUpdateEntity`（INSERT 无 version 校验）。两并发事务都观察 null 都 INSERT → 重复 schedule 行 + `setAccumulatedDepreciation` 双计。**复核「如 P1-MA2-089 登记」——缺 status==PENDING 守卫，待 MR1 加 (assetId,period) UK 或 requireSchedulePending**。ErpAstDepreciationSchedule 声明 versionProp（透明乐观锁）将 silent lost-update 降级为 detectable conflict，但 INSERT 路径无 version 检查 |
| **9 dispatcher tryPost 异常吞咽一致性（P1-MA2-060 同型根因复核）** | ❌ FAIL | 全 9 dispatcher tryPost 统一 `catch(Exception){ LOG.warn/error; return null/false }` 吞咽过账失败保持 posted=false（Depreciation:47-56 / Capitalization:50-57 / Disposal:39-51 / ValueAdjustment:39-51 / AssetInventory:39-51 / MaintenanceCapitalization:40-52 / MaintenanceExpense:40-52 / AssetSplit / AssetMerge）。**复核确认 P1-MA2-060 同型根因在全部 9 对一致**（MA2 仅显式枚举 Capitalization/Disposal）。**新发现 P1-MA4-013**：Depreciation dispatcher 路径（executeDepreciation→tryPost）无文档 Processor + **无 DeferredPostingSweepJob 扫描 ErpAstDepreciationSchedule**——Cap/Disposal posted=false 至少有 DeferredPostingSweepJob（finance 域扫 ErpFinPostingException）兜底，**折旧 posted=false 无任何自动重试/告警入口**，仅 LOG.warn。GL 缺 DEPRECIATION 凭证但资产累计折旧已回写 → 业财不一致直至运营手动重跑 executeDepreciation（自愈）或 reverseDepreciation。MA2 审文档 Processor tryPost，未覆盖折旧 dispatcher 编排路径 |
| **reverseApprove 红冲闭环对称性（P1-MA2-060 复核）** | ❌ 维持 | `reverseDepreciation:154-177` 守卫 EXECUTED + 回滚资产卡片 + 设 REVERSED——**折旧 reverse 对称** ✓。但 Capitalization/Disposal 文档 Processor reverseApprove 仅 posted=true 时回滚资产（P1-MA2-060）——**复核「如登记」**，待 MR1 |
| **auto-depreciation cron 触发链路（P1-MA3-033 复核）** | ⚠️ 维持 | finance 侧 `ErpFinAccountingPeriodProcessor.isAutoDepreciationOnClose:686-689` 读 `ErpFinConstants.CONFIG_AUTO_DEPRECIATION_ON_CLOSE="erp-fin.auto-depreciation-on-close"`；owner doc `domain-design-guidelines.md:662`+`period-close.md:287` 声明 `erp-fin.auto-depreciation`。**复核「如 P1-MA3-033 登记」——config 键名漂移**。附带：`ErpAstConstants.CONFIG_AUTO_DEPRECIATION_ON_CLOSE="erp-ast.auto-depreciation-on-close"` 常量定义存在但**全 assets 模块零引用**（死常量，prefix erp-ast 与 finance 实读 erp-fin 不一致），归 P2-MA4-007 |

**裁决**：折旧算术正确性 PASS（直接影响财务报表的折旧额无算术缺陷）；幂等/期间控制 PASS；**核心缺陷在失败恢复闭环**——P1-MA4-013 折旧 posted=false 业财悬挂无自动重试（MA2 未覆盖折旧 dispatcher 编排路径）；并发双计 P1-MA2-089 + 文档 reverseApprove 不对称 P1-MA2-060 复核「如登记」。

### 领域 3：类型和契约质量（裁决：**PASS（一处 P3 契约漂移）**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 9 dispatcher tryPost 返回类型一致性 | ⚠️ P3 | **6 dispatcher 返回 Long voucherId**（Depreciation/Disposal/ValueAdjustment/AssetInventory/MaintenanceCapitalization/MaintenanceExpense）；**3 dispatcher 返回 boolean**（Capitalization/AssetSplit/AssetMerge）。boolean 路径丢失 voucherId（仅 CapitalizationPostingDispatcher 实际不持久化 voucherId，AssetSplit/Merge 同理）。功能正确但**契约漂移**——调用方处理两种返回类型。归 P2-MA4-006 顺手统一 |
| 8 类业务 Processor 审批轴参数/返回契约一致性 | ✅ PASS | 8 审批轴 Processor（submitForApproval/approve/reject/reverseApprove/withdrawApproval/cancel）签名对齐 CrudBizModel + IServiceContext 范式。Split/Merge reverseApprove 抛 ERR_AST_SPLIT/MERGE_REVERSE_NOT_SUPPORTED（不可逆契约） |
| AcctDocProvider 科目文档 BigDecimal 类型安全 | ✅ PASS | 折旧额/净值/残值/原值全 BigDecimal；DepreciationCalculator SCALE=4；schedule 行 actualAmount/plannedAmount/accumulatedDepreciation/netBookValue 全 BigDecimal。无 double/float 货币类型 |
| 折旧计划 schedule 行金额精度 | ✅ PASS | DepreciationCalculator HALF_UP SCALE=4；recalculateForCapitalizationMaintenance:212-213 同 SCALE=4；末月调整到残值（:224-226） |

**裁决**：BigDecimal 货币类型安全；唯一契约漂移是 tryPost 返回类型 Long vs boolean（P3，归 P2-MA4-006 顺手统一）。**无类型不匹配缺陷**。

### 领域 4：错误处理和操作安全（裁决：**PASS（异常规范化扎实，失败恢复闭环缺陷归领域 2/3 P1）**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 折旧引擎/Processor 异常扩展 NopException + ErrorCode | ✅ PASS | `ErpAstDepreciationScheduleProcessor` 全业务异常 `throw new NopException(ErpAstErrors.ERR_*)`（requireAsset→ERR_ASSET_NOT_FOUND / validateAssetInService→ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE / requirePeriodOpen→ERR_DEPRECIATION_PERIOD_NOT_FOUND|CLOSED / reverseDepreciation→ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION）。ErpAstErrors 全 `erp.err.ast.*` 前缀 + 中文描述 + ARG_* 作用域参数键 |
| ErrorCode 完整性（折旧算术溢出/过账失败/并发冲突/状态非法迁移） | ⚠️ 部分 | 状态非法迁移/期间/资产未找到 ErrorCode 齐全 ✓。**过账失败无 ErrorCode**——dispatcher tryPost 吞咽返回 null/false（设计容错，非抛 ErrorCode）；**并发冲突无显式 ErrorCode**——依赖 versionProp 透明乐观锁抛平台层冲突（非 erp.err.ast.*）。过账悬挂闭环缺陷归 P1-MA4-013，不重复登记 |
| 错误传播（折旧算术溢出/过账失败/并发冲突） | ✅ PASS | requirePeriodOpen 硬前置阻断非 OPEN；reverse() 硬前置失败向上抛（dispatcher.reverse 全 re-throw）；tryPost 容错吞咽（保持 posted=false）。错误传播分级正确（硬前置抛 / 容错吞咽返回） |
| 批量折旧部分失败告警闭环 | ⚠️ 部分 | `executeBatchDepreciation:148` 单失败 LOG.warn 记录资产编码+期间+消息，返回 processed 计数。**无 IErpSysNotificationBiz 派发**——失败资产仅日志，运营需查日志发现。归 P1-MA4-013 同型（失败无告警入口） |

**裁决**：异常规范化扎实（全 NopException + erp.err.ast.* ErrorCode + 作用域参数键）；错误传播分级正确。失败恢复闭环/告警缺陷归 P1-MA4-013（领域 2）。**无裸异常/ErrorCode 缺失致主路径破坏**。

### 领域 5：测试有效性（裁决：**FAIL——异常路径零覆盖 + 残值边界仅 residual=0（P1-MA4-014）**）

> assets 14 测试（含 7 折旧/Processor 相关）。本审计抽样折旧/处置/价值调整/资本化/过账红冲/科目键 7 测试类。

| 测试类 | 方法数 | 覆盖 | 断言强度 |
|--------|--------|------|---------|
| `TestErpAstDepreciation` | 5 | 直线每期等额+末到残值 / DDB 残值约束 / 批量 / 期间控制 CLOSED+缺失拒绝 / 幂等重执行 | **强**——断言 actualAmount 数值 + 累计折旧 + posted + 凭证回链 + 仅 1 有效凭证 |
| `TestErpAstPostingReverse` | 4 | 资本化 reverseApprove（posted=true 回滚 DRAFT）/ 折旧 reverse（回滚卡片）/ 处置 reverseApprove（posted=true 恢复 IN_SERVICE）/ 端到端 | **强**——断言资产状态 + posted + schedules 状态 + 凭证红冲 |
| `TestErpAstAcctDocProviderAccountKey` | 12 | 科目键兜底解析 | 中——科目码断言 |
| `TestErpAstDisposal/ValueAdjustment/Capitalization` | 2/6/3 | 主路径 | 中 |

**测试空洞（P1-MA4-014）**：

1. **posted=false 窗口 reverseApprove 不对称零覆盖**——P1-MA2-060 的 posted=false 窗口期（资本化/处置过账失败悬挂态）reverseApprove 不回滚资产行为，无测试触发（现有 reverseApprove 测试均为 posted=true 路径）
2. **并发首次折旧重复零覆盖**——P1-MA2-089 executeDepreciation 缺 PENDING 守卫致并发双计，无并发测试
3. **批量折旧部分失败隔离零覆盖**——executeBatchDepreciation 单资产失败 try/catch 跳过不影响他资产，无测试触发（现有批量测试全 happy path）
4. **过账悬挂零覆盖**——Depreciation dispatcher tryPost 吞咽返回 null 致 posted=false 业财悬挂（P1-MA4-013），无 mock post 抛异常→断言 posted=false 测试
5. **残值边界仅 residual=0**——TestErpAstDepreciation 直线法/DDB 全用 residual=ZERO（`:62,:100`），DepreciationCalculator 残值约束兜底（`:71-73`）的实际触发分支（nbv−amount<residual 截断）**无测试覆盖**——非零残值场景未测
6. **折旧算术边界（期末/已达残值）**——`:32-34` 已达残值返 0 分支无直接断言（DDB 末期满寿命收敛到残值是间接覆盖）

**裁决**：主路径断言强度扎实（数值 + 凭证 + 状态），但**异常路径系统性零覆盖 + 残值边界仅 residual=0**——assets 测试/mutation 比 0.23 偏低，异常路径覆盖是重点（P1-MA4-014，目标 MR2）。与 A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A4.2a P1-MA4-009 + A4.2b P1-MA4-011 互补不重叠。

### 领域 6：可维护性和未来变更风险（裁决：**PASS（P2 watch-only 重复模式提取候选）**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 9 PostingDispatcher 重复模式 | ⚠️ P2 | 全 9 dispatcher 重复 `buildEvent` 骨架 + `tryPost try/catch LOG.warn/error return null/false` + `reverse try/catch LOG re-throw` + `resolveSubjectCode(Long,String)` + `loadCategory` + `nz` + `resolveAcctSchemaId` 私有方法**逐字 copy-paste**（9 份几乎相同实现）。**提取候选**：抽象 `AbstractAssetPostingDispatcher` 基类或 `PostingDispatcherSupport` helper 收敛公共方法。归 P2-MA4-006 |
| DepreciationCalculator 折旧方法策略可扩展性 | ✅ PASS | switch(method) 三分支 + default 直线法兜底。新增方法只需加 case 常量。纯函数静态方法易测。可扩展性良好 |
| 48 Processor 审批轴对称性 | ⚠️ P2 | 8 类业务（Capitalization 6 / Disposal 6 / ValueAdjustment 7 / Split 7 / Merge 7 / Inventory 2 / Maintenance 2 / Cip 1）审批轴 submitForApproval/approve/reject/reverseApprove/withdrawApproval Processor 类对称生成。Split/Merge 含 Cancel Processor（xbiz 未暴露，P2-MA2-061 死代码）。重复模式提取候选（每域审批轴 5-7 Processor 高度同构） |
| DepreciationCalculator SCALE 硬编码 | ⚠️ P3 | `:20 SCALE=4` 硬编码常量。可接受（精度统一），但若需按币种/类别差异化精度需重构 |

**裁决**：折旧方法策略可扩展性良好；48 Processor 审批轴对称；**主要可维护性风险是 9 PostingDispatcher 公共方法逐字 copy-paste**（P2-MA4-006 watch-only，重复模式提取候选）。

### 领域 7：自动化和防护覆盖（裁决：**FAIL——compliance checker R2d 未覆盖 daoFor(ErpFin\*) + 无折旧算术/并发回归门控（P2-MA4-007）**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| compliance checker 规则守护跨域 daoFor | ⚠️ 部分 | `docs/audits/nop-compliance-checker.sh` R2d 仅扫 Processor/Dispatcher/Engine 中 `daoFor(ErpMd*)`（:154-160）。**未覆盖 `daoFor(ErpFin*)`**——`ErpAstDepreciationScheduleProcessor:290 daoFor(ErpFinAccountingPeriod)` 漏检。归 P2-MA4-007 |
| R8 Processor 无 xbiz 规则 | ✅ PASS | `processor-extension-pattern.md:146-148` 已澄清 R8「Processor 缺 xbiz 桥接」检查不适用本项目（接线路径 BizModel Java→Processor，xbiz 非 Processor 契约层） |
| 折旧算术回归测试门控 | ⚠️ 部分 | TestErpAstDepreciation 覆盖三方法主路径数值断言 ✓。**但残值约束兜底分支（非零残值）+ 已达残值返 0 分支无直接测试**（领域 5 空洞 5/6）。归 P1-MA4-014 |
| 过账/红冲回归门控 | ⚠️ 部分 | TestErpAstPostingReverse 覆盖 posted=true 红冲闭环 ✓。**但 posted=false 悬挂路径无门控**（领域 5 空洞 4）。归 P1-MA4-013/014 |
| 并发回归门控 | ❌ FAIL | 无 executeDepreciation 并发测试（P1-MA2-089 回归无防护）。归 P1-MA4-014 |

**裁决**：R8 规则澄清正确；R2d 跨域 daoFor 检查**遗漏 ErpFin 方向**（P2-MA4-007）；折旧算术/过账/并发回归门控存在空洞（归 P1-MA4-013/014）。折旧正确性直接影响财务报表，防护优先级高。

---

## 3. MA1/MA2/MA3 已知 finding 运行时复核

> 每项标记「如 owner doc 声明」（无新代码层缺陷）或「发现新代码层缺陷」。

| Finding ID | 原描述 | 代码实现质量角度复核 | 终态 |
|-----------|--------|---------------------|------|
| `P1-MA1-008`（todo MR1，assets 29 列 propId 缺失） | ErpAstDepreciationSchedule/Movement/... 共 29 列 propId 缺失 | propId 是 ORM 元数据治理，非代码实现质量——DepreciationCalculator/Processor/dispatcher 代码不依赖 propId 编号（状态字段经 dict + ErpAstConstants 常量承载）。**「如登记」，无代码层影响** | 不升级（维持 P1 治理待 MR1） |
| `P1-MA1-016`（todo MR1，finance→assets） | ErpFinAccountingPeriodProcessor:389 daoFor(ErpAstDepreciationSchedule) | finance→assets 跨域**只读** DAO（findAllByQuery posted=true 折旧凭证冲销对象），状态写经 IErpAstDepreciationScheduleBiz.reverseDepreciation I\*Biz。assets 侧 reverseDepreciation:154-177 守卫 + 回滚正确。**「如登记」，复核确认 read-only 无数据破坏** | 不升级（维持 P1 待 MR1） |
| `P1-MA1-022`（todo MR1，9 域合并） | ast ErpAstDepreciationScheduleProcessor:290 ErpFinAccountingPeriod + 9 dispatcher ErpMdSubject 只读 | **本审计复核确认**：`:289-296 daoFor(ErpFinAccountingPeriod).findAllByQuery` 只读查期间状态；9 dispatcher `resolveSubjectCode daoFor(ErpMdSubject).getEntityById` 只读科目兜底解析。异常路径兜底默认科目码 + @BizMutation 事务回滚覆盖。**「如登记」**——本审计登记 P1-MA4-015 为其在 assets 折旧/过账投影（不重复计入 MR2，同 P1-MA1-022 一并裁决） | 不升级（维持 P1，投影 P1-MA4-015） |
| `P1-MA2-058`（todo MR1，Movement reverseApprove→SUBMITTED） | ErpAstMovement.xbiz reverseApprove 设 SUBMITTED 违反 owner doc §2 强制 REJECTED | Movement 是资产位置/组织转移单据，无 posted 副作用、无下游业务副作用。**代码实现质量角度「如登记」**——仅 approveStatus 审计轨迹漂移，非代码算术/事务/类型缺陷。状态机业务正确性归 A2.10 | 不升级（维持 P1 待 MR1） |
| `P1-MA2-059`（todo MR1，Movement 5 INLINE 缺 isCancelled 守卫） | ErpAstMovement 全 5 INLINE 动作缺 docStatus 守卫 | Movement 无 cancel mutation 暴露，docStatus=CANCELLED 经服务层不可达。**代码实现质量角度「如登记」**——不产生脏数据，非代码层新缺陷 | 不升级（维持 P1 待 MR1） |
| `P1-MA2-060`（todo MR1，Cap/Disposal tryPost 吞咽悬挂 + reverseApprove 不对称） | Capitalization/Disposal tryPost 吞异常 + reverseApprove 仅 posted=true 回滚资产 | **本审计复核确认 tryPost 吞咽同型根因在全部 9 对 dispatcher 一致**（领域 2）。Capitalization/Disposal posted=false 至少有 DeferredPostingSweepJob 兜底；**新发现 P1-MA4-013**：Depreciation dispatcher 路径无文档 Processor + 无 DeferredPostingSweepJob 扫描，posted=false 无自动重试/告警——MA2 审文档 Processor tryPost 未覆盖折旧 dispatcher 编排路径。「**发现新代码层缺陷**」（P1-MA4-013） | 部分升级（P1-MA2-060 维持 + 新增 P1-MA4-013 折旧路径专属） |
| `P1-MA2-061`（todo MR1，ErpAstAsset IDLE 死状态） | IDLE 状态机迁移完全未实现 + 折旧引擎只查 IN_SERVICE | **代码实现质量角度「如登记」**——折旧引擎 `executeBatchDepreciation:138` 仅查 IN_SERVICE 等价于「IDLE 默认停提」业务语义（owner doc §1 设计意图），非折旧算术/事务缺陷。状态机业务正确性归 A2.10 | 不升级（维持 P1 待 MR1） |
| `P1-MA2-089`（todo MR1，executeDepreciation 缺 PENDING 守卫） | 并发首次折旧 executeDepreciation 缺 status==PENDING 守卫致双计 | **本审计复核确认**：`executeDepreciation:64-102 findSchedule→newEntity+saveOrUpdateEntity` INSERT 无 version 校验。**「如登记」**——并发双计缺陷确认，待 MR1 加 UK/守卫。versionProp 透明乐观锁将 silent lost-update 降级为 detectable conflict（但 INSERT 路径无 version 检查） | 不升级（维持 P1 待 MR1） |
| `P1-MA3-033`（todo MR1，auto-depreciation config 键名漂移） | erp-fin.auto-depreciation vs code erp-fin.auto-depreciation-on-close | **本审计复核确认**：`ErpFinAccountingPeriodProcessor.isAutoDepreciationOnClose:686-688` 读 `erp-fin.auto-depreciation-on-close`；owner doc 声明 `erp-fin.auto-depreciation`。**「如登记」**——config 键名漂移确认。附带：ErpAstConstants.CONFIG_AUTO_DEPRECIATION_ON_CLOSE 死常量（全模块零引用） | 不升级（维持 P1 待 MR1） |
| `P2-MA1-023`（todo MR1，DISPOSED owner doc drift） | state-machine.md §1 列 5 态 vs dict 6 态 | DISPOSED 经 Split/Merge Processor 写入可达。**「如登记」**——owner doc drift，非代码缺陷 | 不升级（维持 P2） |
| `P2-MA1-024`（todo MR1，CANCELLED owner doc drift） | 折旧计划 3 态 vs dict 4 态 | CANCELLED 经 cancelSchedules/cancelPendingSchedules 写入可达。**「如登记」** | 不升级（维持 P2） |

**裁决**：9 项已知 finding 运行时复核 **8 项「如登记」无升级**；**1 项复核发现新代码层缺陷**——P1-MA2-060 复核时发现相邻代码路径新缺陷 P1-MA4-013（Depreciation dispatcher posted=false 业财悬挂无自动重试，MA2 审文档 Processor tryPost、本审折旧 dispatcher 编排路径）。

---

## 4. P0-P3 finding 清单（按严重性排序）

### 4.1 P1 finding（3 项）

| Finding ID | 域 | 描述 | 严重性 | 影响 | 修复方式 | 目标 MR |
|-----------|-----|------|-------|------|---------|---------|
| `P1-MA4-013` | assets | **折旧 dispatcher posted=false 业财悬挂无自动重试/告警（失败恢复闭环缺失）**：`DepreciationPostingDispatcher.tryPost:43-57` `catch(Exception){ LOG.warn/error; return null }` 吞咽过账失败保持 posted=false。`ErpAstDepreciationScheduleProcessor.executeDepreciation:120` 据返回 voucherId 置 posted——null 时 schedule 保持 EXECUTED+posted=false + 资产累计折旧已回写（`:114-116`）。**关键差异**：折旧路径无文档 Processor + **无 DeferredPostingSweepJob 扫描 ErpAstDepreciationSchedule**（sweep 仅扫 finance ErpFinPostingException）——Cap/Disposal posted=false 至少有 sweep 兜底重试，**折旧 posted=false 无任何自动重试入口/告警**，仅 LOG.warn。GL 缺 DEPRECIATION 凭证但资产累计折旧已反映折旧 → 业财不一致（累计折旧低估 GL / 费用低估）直至运营手动重跑 executeDepreciation（自愈，因 `:69 wasExecuted+posted` 不触发 reverse，重跑重算+重试过账）或 reverseDepreciation。期末结账前置检查 findUnresolvedPostingExceptionKeys 仅扫 finance 异常工作台，**不覆盖** assets 折旧 posted=false，间接兜底失效。与 P1-MA2-060（Cap/Disposal tryPost 吞咽）同型根因（业财悬挂 + 异常吞咽），但 MA2 审文档 Processor tryPost、本审**折旧 dispatcher 编排路径**（executeDepreciation→tryPost，无文档 Processor + 无 sweep）——MA2 未覆盖。非 P0：(1) 失败模式需过账引擎异常（基础设施故障/科目配置错误，非正常路径）；(2) LOG.warn 提供运维可见性；(3) 重跑 executeDepreciation 自愈；(4) 业财不一致可经期末试算平衡人工发现。折旧正确性直接影响财务报表（累计折旧/折旧费用）。 | major（功能性业财悬挂需运营介入，非数据破坏——重跑自愈 + LOG + 期末试算兜底；折旧 posted=false 自愈路径存在故轻于 Cap/Disposal 终态悬挂） | MR1 裁决——方案 A（推荐）折旧 posted=failure 派发 IErpSysNotificationBiz 告警 + owner doc depreciation-and-posting.md §七 错误处理 标注「折旧过账失败悬挂经运营手动重跑 executeDepreciation 自愈或 reverseDepreciation」+ executeBatchDepreciation 单失败告警闭环；方案 B 折旧 posted=false 进 finance 异常工作台由 DeferredPostingSweepJob/期末前置检查兜底（与 P1-MA2-060 方案 B 一并裁决）。触及会计保护区域，修复须独立 plan-audit + 人工确认 | MR1 |
| `P1-MA4-014` | assets | **折旧/Processor 链路测试有效性系统性不足（异常路径零覆盖 + 残值边界仅 residual=0）**：(a) posted=false 窗口 reverseApprove 不对称零覆盖——P1-MA2-060 资本化/处置过账失败悬挂态 reverseApprove 不回滚资产，无测试触发（现有 TestErpAstPostingReverse 全为 posted=true 路径）；(b) 并发首次折旧重复零覆盖——P1-MA2-089 executeDepreciation 缺 PENDING 守卫致并发双计，无并发测试；(c) 批量折旧部分失败隔离零覆盖——executeBatchDepreciation 单资产失败 try/catch 跳过不影响他资产，无测试触发；(d) 过账悬挂零覆盖——Depreciation dispatcher tryPost 吞咽返回 null 致 posted=false 业财悬挂（P1-MA4-013），无 mock post 抛异常→断言 posted=false 测试；(e) 残值边界仅 residual=0——TestErpAstDepreciation 直线法/DDB 全用 residual=ZERO，DepreciationCalculator 残值约束兜底（`:71-73 nbv−amount<residual 截断`）+ 已达残值返 0（`:32-34`）分支无测试覆盖。assets 测试/mutation 比 0.23 偏低，异常路径覆盖是重点。 | major（测试空洞致 P1-MA2-060/089 + P1-MA4-013 回归无防护 + 折旧算术边界 bug 对测试不可见——折旧正确性直接影响财务报表） | MR2 补——(1) posted=false 窗口 reverseApprove 不对称测试（mock tryPost 返回 false/null→reverseApprove→断言资产状态不回滚，闭合 P1-MA2-060 测试可见性）；(2) 并发首次折旧重复测试（双线程 executeDepreciation 同 assetId+period→断言无双计或 UK 冲突，闭合 P1-MA2-089）；(3) 批量部分失败隔离测试（seed 一失败资产+一正常资产→断言正常资产仍计提 + processed 计数）；(4) 折旧过账悬挂测试（mock post 抛异常→断言 posted=false + schedule EXECUTED，闭合 P1-MA4-013 测试可见性）；(5) 非零残值折旧算术测试（残值=2000 原值=12000→断言末期净值=残值非 0 + 截断分支触发）。与 A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A4.2a P1-MA4-009 + A4.2b P1-MA4-011 + A5.1 互补不重叠 | MR2 |
| `P1-MA4-015` | assets | **折旧引擎/Processor 链路跨域 daoFor 绕 I\*Biz（同 P1-MA1-022 根因在 assets 折旧/过账投影）**：assets→finance 只读 `ErpAstDepreciationScheduleProcessor.findPeriod:289-296 daoFor(ErpFinAccountingPeriod).findAllByQuery` 查期间状态；assets→master-data 只读 全 9 dispatcher `resolveSubjectCode daoFor(ErpMdSubject).getEntityById` + AcctSchemaResolver daoFor(ErpMdAcctSchema)。违反 AGENTS.md「跨实体访问应通过 I\*Biz 接口」+ data-dependency-matrix.md §5.3。与 P1-MA1-022（pur/sal/ast/inv/mnt/prj/qa/drp/aps 9 域同型）+ P1-MA4-003/006/008/012 同根因，本批是其在 assets 折旧引擎/过账 dispatcher 的投影（P1-MA1-022 原枚举 ast 含 ErpAstDepreciationScheduleProcessor:290 + 9 dispatcher ErpMdSubject，本审计复核确认 + 显式登记）。read-only 无活跃数据破坏。 | major（架构边界违规，无活跃数据破坏——只读查询） | MR1——同 P1-MA1-022 方案 A（finance/master-data I\*Biz 补便捷只读方法后迁移多站点）或方案 B（永久接受登记 posting-exemptions.md）。**不重复计入 MR2**（同 P1-MA1-022/P1-MA4-003/006/008/012 一并裁决） | MR1 |

### 4.2 P2 finding（2 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA4-006` | **可维护性热点合并 5 项**：(a) **9 PostingDispatcher 公共方法逐字 copy-paste**——buildEvent 骨架 + tryPost try/catch LOG.warn/error return null/false + reverse try/catch LOG re-throw + resolveSubjectCode + loadCategory + nz + resolveAcctSchemaId 全 9 份几乎相同实现。提取候选：抽象 AbstractAssetPostingDispatcher 基类或 PostingDispatcherSupport helper 收敛；(b) **tryPost 返回类型契约漂移**——6 dispatcher 返回 Long voucherId，3（Capitalization/AssetSplit/AssetMerge）返回 boolean（boolean 路径丢失 voucherId）；(c) **48 Processor 审批轴对称性**——8 类业务审批轴 5-7 Processor 高度同构，重复模式提取候选；(d) **executeBatchDepreciation 单事务错误隔离仅覆盖早抛**——批量在单一 @BizMutation 事务内 try/catch，非 per-asset REQUIRES_NEW 子事务/savepoint，mid-flight 失败（如幂等 reverse 抛出后 catch）该资产部分 mutation 可能随主事务提交持久化（触发面收窄，早抛路径主导）；(e) ErpAstConstants.CONFIG_AUTO_DEPRECIATION_ON_CLOSE 死常量（全模块零引用，prefix erp-ast 与 finance 实读 erp-fin 不一致）。 | watch-only，MR2 顺手——方案 A（推荐）(a) 抽象 dispatcher 基类收敛公共方法 + (b) 统一 tryPost 返回类型（Long voucherId 或 posted VO）+ (d) 批量改 per-asset 子事务或文档标注「早抛隔离」语义；方案 B 接受现状登记 posting-exemptions.md |
| `P2-MA4-007` | **自动化防护缺口 2 项**：(a) **compliance checker R2d 未覆盖 daoFor(ErpFin\*)**——`docs/audits/nop-compliance-checker.sh:154-160` R2d 仅扫 Processor/Dispatcher/Engine 中 `daoFor(ErpMd*)`，**未覆盖 `daoFor(ErpFin*)`** 致 `ErpAstDepreciationScheduleProcessor:290 daoFor(ErpFinAccountingPeriod)` 漏检（P1-MA4-015 同型站点无自动防护）；(b) **无折旧算术/并发/过账悬挂回归门控**——残值约束兜底分支 + 并发首次折旧（P1-MA2-089）+ 过账悬挂（P1-MA4-013）无自动化测试门控（归 P1-MA4-014 补测试后形成门控）。折旧正确性直接影响财务报表，防护优先级高。 | watch-only，MR2 顺手——方案 A（推荐）(a) R2d 扩展扫描 `daoFor(ErpFin` 覆盖 finance 跨域方向（与 P2-MA4-002 R2d 未覆盖 Resolver/Propagator/Helper 同型扩展）；(b) P1-MA4-014 测试补齐后形成 CI 门控 |

### 4.3 P3 finding

- DepreciationCalculator `SCALE=4` 硬编码常量（可接受，精度统一；若需按币种/类别差异化精度需重构）。即时风险低，不单独登记。

---

## 5. 综合裁决

### 5.1 Verdict

**⚠️(P1)**——折旧引擎与 Processor 链路代码实现质量**核心扎实**（折旧算术正确性 + 编排健壮性 + 红冲闭环对称 + 跨域 Facade + 异常规范化五面），但**失败恢复闭环（P1-MA4-013 折旧 posted=false 业财悬挂无自动重试/告警，MA2 未覆盖折旧 dispatcher 编排路径）+ 测试有效性（P1-MA4-014 异常路径零覆盖 + 残值边界仅 residual=0）+ 架构边界（P1-MA4-015 跨域 daoFor 投影）** 三项 P1 缺陷需 MR1/MR2 修复。

### 5.2 P0 评估

**无 P0**——无活跃数据破坏路径：
- 折旧算术经 DepreciationCalculator 残值约束双重兜底（`:32-34` 已达残值返 0 + `:71-73` 截断不低于残值）+ 三方法主路径数值断言测试覆盖
- 并发双计（P1-MA2-089）已登记 deferred 待 MR1，versionProp 透明乐观锁将 silent lost-update 降级为 detectable conflict
- TOCTOU 类已登记 P0-MA2-018 deferred
- 业财悬挂（P1-MA4-013）需过账引擎异常前置（非正常路径）+ 重跑自愈 + LOG + 期末试算兜底

### 5.3 剩余风险

1. **折旧正确性直接影响财务报表**——P1-MA4-013 业财悬挂（GL 缺 DEPRECIATION 凭证）+ P1-MA2-089 并发双计，虽非 P0 但属高风区域，MR1 优先
2. **48 Processor 全域最高密度**——P2-MA4-006 重复模式提取降低未来回归风险
3. **compliance checker 防护缺口**（P2-MA4-007）——daoFor(ErpFin*) 漏检 + 无回归门控，MR2 补测试 + R2d 扩展后形成防护

### 5.4 与 MA2/A4.1a/A4.1b/A4.2a/A4.2b 交叉去重

- **P1-MA4-013** 与 P1-MA2-060（Cap/Disposal tryPost 吞咽）同型根因但**不同代码路径**（折旧 dispatcher 编排路径 vs 文档 Processor tryPost），MA2 未覆盖折旧 dispatcher——新登记，MR1 协同
- **P1-MA4-014** 与 A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A4.2a P1-MA4-009 + A4.2b P1-MA4-011 + A5.1 互补不重叠（各域测试空洞独立登记）
- **P1-MA4-015** 同 P1-MA1-022/P1-MA4-003/006/008/012 根因在 assets 折旧/过账投影，MR1 一并裁决不重复计入 MR2
- **P2-MA4-006/007** 与 A4.1a P2-MA4-001/002 + A4.1b P2-MA4-003 + A4.2a P2-MA4-004 + A4.2b P2-MA4-005 同型（可维护性热点 + 自动化防护），独立登记

**assets 域 MA4 代码质量终态在此收口：3 P1 + 2 P2，零 P0。** roadmap A4.3 推进至 done（待独立 closure audit）。
