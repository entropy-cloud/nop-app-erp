# 2026-07-18-0949-1-maintenance-labor-cost-posting Maintenance 维修工时费用化 GL 过账后端 + 浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-18
> Mission: erp
> Work Item: 各域细化端到端验证（maintenance 域维修工时费用化过账 successor——后端设计-实现差距收口）
> Source: 三项 Successor Required: yes 的 Deferred 承接（同一缺口面，触发条件经实时仓库核实**可由本计划主动驱动满足**）：
>   - `docs/plans/2026-07-17-2256-2-maintenance-assets-linked-visit-anti-double-deduct-e2e.md` Deferred「维修工时（labor）费用化过账」（触发条件「维修工时成本核算后端落地时」——本计划即此后端）
>   - `docs/plans/2026-07-10-1100-6-maintenance-spare-part-posting.md` Deferred「工时费用化留 successor」（触发条件同上）
>   - `docs/plans/2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime.md` Deferred「备件消耗闭合，工时费用化留 successor」（触发条件同上）
>
> **owner doc 显式声明 successor**：
>   - `docs/design/maintenance/state-machine.md:163` 「工时费用化凭证 successor」
>   - `docs/design/assets/maintenance.md:111` 「维修工时费用化凭证仍 open（successor）」
>   - `docs/design/maintenance/README.md:52` 「维护访问执行时记录实际维护内容、消耗备件、工时」
>   - `docs/design/maintenance/README.md:20` 「维修成本（通常不直接凭证，通过备件消耗与工时）」
>
> erp-survey 对标：ERPNext Maintenance Schedule + Stock Entry Maintenance + Metasfresh Maintenance Order 均内置维修工时费用化过账（Dr 维修费用 / Cr 应付职工薪酬或中转科目）。备件消耗侧 1100-6 已落地（MAINTENANCE_ISSUE(492)），工时侧为 maintenance 域业财过账最后一个 open successor。
> Related: `2026-07-10-1100-6-maintenance-spare-part-posting.md`（备件消耗过账范式源——本计划镜像其 Dispatcher + Provider + bean + config 门控结构）、`2026-07-17-2256-2`（linked-visit 防双重扣减后端链路已闭合备件段，本计划补工时段）、`2026-07-04-0831-2-hr-payroll-engine-income-tax.md`（SALARY 270/SALARY_PAYMENT 280 业务类型范式源——应付职工薪酬科目方向参考）、`2026-07-14-0606-2-landed-cost-spare-part-posting-e2e.md`（备件消耗浏览器层 E2E 范式源）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-18，`read`/`grep` 实测，非采信旧记忆）：

### owner doc 设计与现有后端基线

- **owner doc 设计要求**：`docs/design/maintenance/state-machine.md:163` 明示「工时费用化凭证 successor」与备件消耗凭证共同构成 maintenance 域业财过账；触发点为维护访问完成（`ErpMntVisit.complete`），金额来源 `totalMinutes`（DECIMAL）× 工时费率。
- **`ErpMntVisitBizModel.complete`** 已落地（`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/entity/ErpMntVisitBizModel.java:63-69` + step `doComplete:154-164`）：
  - 状态翻转 VISIT_STATUS_IN_PROGRESS→COMPLETED
  - 计算 `totalMinutes = Duration.between(startTime, endTime).toMinutes()`（line 159-161）
  - `completedAt = CoreMetrics.currentTimestamp()`
  - `updateEntity` 持久化
  - `equipmentStatusLinker.restoreToRunning` 联动设备状态
  - **零业财过账触发**——`grep` 全仓 `ErpMntVisitBizModel.java` 无 `postingDispatcher`/`postLabor`/`LaborPosting` 引用
- **`ErpMntVisit` 实体字段**（实测 `module-maintenance/model/app-erp-maintenance.orm.xml:254-313` 21 列业务字段 + 标准字段）：`totalMinutes`（DECIMAL precision=12 scale=2，domain `downtimeMinutes`）/ `startTime`（TIMESTAMP）/ `endTime`（TIMESTAMP）/ `posted`（BOOLEAN defaultValue=false）/ `postedAt`（TIMESTAMP）/ `postedBy`（VARCHAR(50) stdDomain=userId）/ `businessDate`（DATE）/ `orgId` / `equipmentId` / `code`（domain `visitCode`）/ `assignedTo`（BIGINT）—— **零字段新增需求**。**iter-2 修订**：经实测当前 maintenance schema 无 `ErpMntEquipment.laborHourlyRate` 列也无 `ErpMntTemplate` 实体也无 `standardLaborMinutes` 列——本计划工时费率先经 config 全局费率（`erp-mnt.default-labor-hourly-rate`）单路派生，设备级/模板级/员工级费率物化归各自 successor（详见 Non-Goals）。
- **`ErpMntVisit` tagSet**（实测 ORM l.256）：`gid,erp.maintenance`——**无 `use-approval`**（与 `ErpMntSparePartUsage` 一致，complete 是 DIRECT mutation 不经审批轴）。

### 备件消耗过账范式已就位（本计划镜像范式源 1100-6）

- **`MaintenanceIssuePostingDispatcher`**（实测 `module-maintenance/erp-mnt-service/.../posting/MaintenanceIssuePostingDispatcher.java`）：
  - **iter-2 修订**：既有方法签名实测为 `void dispatchIfApplicable(Long sparePartUsageId)`（file:83）——构造 PostingEvent（businessType=MAINTENANCE_ISSUE + billHeadCode=`usage.code + "-MI"` + orgId/currencyId/acctSchemaId/exchangeRate/voucherDate 派生）+ billData 携带 lines（materialId/amount/inventorySubject/code）+ 调 `executor.postEvent(event)`；**catch Exception → log + return**（吞异常范式，file:115-122；语义等价「失败不阻断业务」，无显式 boolean 返回）。本计划 Dispatcher 签名裁决见 Phase 1 Decision (e)。
  - `resolveCurrencyId(orgId)` + `resolveAcctSchemaId(orgId)` helper（既有，本计划复用）。
- **`MaintenanceIssueAcctDocProvider`**（实测 `module-maintenance/erp-mnt-service/.../posting/MaintenanceIssueAcctDocProvider.java`）：`IErpFinAcctDocProvider` 实现，`getSupportedBusinessTypes()=EnumSet.of(MAINTENANCE_ISSUE)`，`createFacts(event, context)` 按 lines 聚合 Dr 6602 折旧费用（种子 subjectName 实测） / Cr 1403 存货；KEY_LINES / KEY_MATERIAL_AMOUNT / KEY_INVENTORY_SUBJECT / KEY_MATERIAL_CODE / KEY_EQUIPMENT_CODE billData 键定义在本类常量。
- **`MntPostingExecutor`**（实测 `module-maintenance/erp-mnt-service/.../posting/MntPostingExecutor.java`）：经 `<ioc:collect-beans by-type>` 收集 `IErpFinAcctDocProvider` bean 集合，`postEvent(event)` 内查 ErpFinBusinessType → AcctDocRegistry → Provider createFacts → `IErpFinVoucherBiz.post`（REQUIRES_NEW 独立事务）；与 finance 域 `FinPostingExecutor` 范式对齐，仅 bean 集合隔离。
- **`IErpFinVoucherBiz.post`**（实测 `module-finance/erp-fin-service/.../IErpFinVoucherBiz.java`）：`@Transactional(REQUIRES_NEW)` 返回 voucherId 或 null（幂等命中，源单据已过账时返回 null）——REQUIRES_NEW 与外层 `@BizMutation` 事务隔离。
- **`ErpFinBusinessType` 枚举**（实测）：`MAINTENANCE_ISSUE(492)` 已存在；**`MAINTENANCE_LABOR` 不存在**——本计划新增枚举值。
- **`erp-fin/business-type` 字典**（实测 `module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/business-type.dict.yaml`）：**iter-1 修订**——经实测当前 dict 仅含 MAINTENANCE_EXPENSE / MAINTENANCE_CAPITALIZATION，**MAINTENANCE_ISSUE 字典项缺失**（1100-6 closure 声称已补但实际未落地——latent defect）。本计划同步补齐 MAINTENANCE_ISSUE + MAINTENANCE_LABOR 两条字典项（详见 Phase 2）。
- **`app-service.beans.xml`**（**iter-1 修订**——经实测路径 = `module-maintenance/erp-mnt-service/src/main/resources/_vfs/erp/mnt/beans/app-service.beans.xml`，非 `model/`；bean id 经实测为**全限定类名**）：含 `<bean id="app.erp.mnt.service.posting.MaintenanceIssuePostingDispatcher" ...>` + `<bean id="app.erp.mnt.service.posting.MaintenanceIssueAcctDocProvider" ...>` 显式声明——本计划镜像各加 1 条（同全限定类名 bean id 范式）。
- **`ErpMntConstants`**（实测 `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/ErpMntConstants.java`）：**iter-2 修订**——经实测含 `CONFIG_SPARE_PART_POSTING_ENABLED` + `DEFAULT_SPARE_PART_POSTING_ENABLED = false`（**无 `CONFIG_VALUE_` 前缀**），本计划镜像加 `CONFIG_LABOR_POSTING_ENABLED` + `DEFAULT_LABOR_POSTING_ENABLED = false` 同命名范式。
- **`ErpMntErrors`**（实测 `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/ErpMntErrors.java`）：本计划新增 ErrorCode（如 `ERR_VISIT_TOTAL_MINUTES_MISSING` / `ERR_LABOR_RATE_NOT_CONFIGURED`）。
- **`ErpMntConfigs`**（实测同包 `ErpMntConfigs.java`）：**iter-1 修订**——经实测仅有 `autoGenerateDueVisits()` + `equipmentStatusLinkEnabled()` 两 reader，**无 `sparePartPostingEnabled()` reader**；1100-6 范式是 Dispatcher 内联 `AppConfig.var`（`MaintenanceIssuePostingDispatcher.java:221-225`）。本计划**不新增 ErpMntConfigs reader**，对齐 1100-6 内联范式（Dispatcher 内私有 `isPostingEnabled()` helper）。

### 既有备件消耗 E2E 覆盖（本计划仅增量工时段）

- `tests/e2e/business-actions/mnt-spare-part-posting.action.spec.ts`（0606-2 落地）已覆盖：
  - 备件消耗 confirm → MAINTENANCE_ISSUE 凭证 Dr 6602=50 / Cr 1403=50
  - posted=true + postedAt/postedBy 翻转
  - config 门控 `erp-mnt.spare-part-posting-enabled=true`
- `tests/e2e/business-actions/mnt-ast-linked-visit-anti-double-deduct.action.spec.ts`（2256-2 落地）已覆盖：
  - linkedVisit=true EXPENSE 路径（assets 侧 MAINTENANCE_EXPENSE(470) Cr 2502 中转清算防双重扣减）
  - linkedVisit=false 对照（Cr 1002 银行存款）
  - maintenance 侧 MAINTENANCE_ISSUE(492) 同 credential 既有断言
- `tests/e2e/business-actions/mnt-request-visit-orchestration.action.spec.ts`（2329-2 落地）已覆盖：
  - Request accept → 响应式 Visit 创建
  - Visit complete 经 GraphQL 触发 + 状态翻转
- **零 spec 覆盖 Visit complete → MAINTENANCE_LABOR 凭证**——本计划补此缺口。

### 浏览器层 E2E 范式（复用，零范式新增）

- `_helper.ts` 三原语 `createViaSave` / `callMutation` / `verifyState` + `findFirst` + `findVoucherIdByBillCode` + `assertVoucherLines`（`tests/e2e/orchestration/_helper.ts`）。
- 自包含 setup（建测试专用 Equipment + Visit 隔离）+ finally 兜底 cleanup（凭证+辅助账+visit+equipment 逐域删，保护 finance/maintenance 看板数值断言基线）。
- GraphQL 入参序列化：`visitId` 经 String scalar（Long 经 Nop GraphQL 暴露为 String，对齐 0606-2 范式）。

### 种子 COA 经实测已就绪（独立草案审查 iter-1 修订）

经实测 `app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv`：
- `2221` = **应交税费**（LIABILITY/CREDIT）—— 已被 VAT 系统占用（PurAcctDocProvider Dr 2221 进项税 / SalAcctDocProvider Cr 2221 销项税，0704-1 凭证行断言依赖），**不可复用为应付职工薪酬**。
- `2211` = **应付职工薪酬**（LIABILITY/CREDIT）—— 0742-2 已加性追加（项目工时贷方 + HR 薪酬贷方 0215-3 SALARY Cr 2211），**正是 maintenance labor 应贷科目**。
- `6602` = **折旧费用**（EXPENSE/DEBIT）—— 1100-6 备件消耗 + assets 折旧共用；**subjectName 是「折旧费用」非「维修费用」**，spec/JUnit 断言按 subjectCode 不受影响，如断言 subjectName 须期望「折旧费用」（对齐 2256-2 M2 caveat）。
- 结论：**Dr 6602 / Cr 2211，无需种子 CSV 加性追加**。
- `erp_md_partner.csv` 含 EMPLOYEE 行（1218-2 / 0215-3 测试期建员工 partner 范式已稳定）。
- `erp_mnt_equipment.csv` 含种子设备 id=1（2256-2 复用范式）。

### 剩余差距

1. `ErpMntVisitBizModel.complete` 无工时费用化过账触发（grep 零 `LaborPostingDispatcher` / `laborPostingDispatcher` 引用）。
2. `MaintenanceLaborPostingDispatcher` 缺失（镜像 `MaintenanceIssuePostingDispatcher.dispatchIfApplicable` 范式，但本计划签名裁决返回 boolean，见 Phase 1 Decision (e)）。
3. `MaintenanceLaborAcctDocProvider` 缺失（镜像 `MaintenanceIssueAcctDocProvider` Dr 维修费用 6602 / Cr 应付职工薪酬 2211）。
4. `ErpFinBusinessType.MAINTENANCE_LABOR` 枚举值缺失 + `erp-fin/business-type` 字典项缺失（保护区域：finance 域跨域契约扩展）。
5. **`erp-fin/business-type.dict.yaml` 经实测缺失 `MAINTENANCE_ISSUE` 字典项**（1100-6 closure 声称已补但实际未落地——latent defect）—— 本计划**同步补齐 MAINTENANCE_ISSUE 字典项**（R13 不可降级，Fix 范畴，与 MAINTENANCE_LABOR 一并补齐）。
6. `ErpMntConstants.CONFIG_LABOR_POSTING_ENABLED` + 默认值常量缺失（对齐既有 `CONFIG_SPARE_PART_POSTING_ENABLED` + `DEFAULT_SPARE_PART_POSTING_ENABLED` 命名）。
7. config 读取范式：经实测 `ErpMntConfigs` 仅有 `autoGenerateDueVisits()` + `equipmentStatusLinkEnabled()` 两 reader，**无 `sparePartPostingEnabled()`**——1100-6 实际范式是 `MaintenanceIssuePostingDispatcher:221-225` 内联 `AppConfig.var(CONFIG_..., DEFAULT_...)`。本计划对齐内联范式（在 Dispatcher 内私有 `isPostingEnabled()` helper）而非新增 ErpMntConfigs reader（避免引入不存在范式引用）。
8. `ErpMntErrors` 新增 ErrorCode（VISIT_TOTAL_MINUTES_MISSING / LABOR_RATE_NOT_CONFIGURED）。
9. `IErpMntVisitBiz.complete` 接口签名不变（既有声明足够），新增触发不需 IBiz 改动。
10. 浏览器层 E2E 缺失（零 spec 覆盖 Visit complete → MAINTENANCE_LABOR 凭证）。
11. bean 注册路径经实测为 `_vfs/erp/mnt/beans/app-service.beans.xml`（**非** `_vfs/erp/mnt/model/`，`model/` 是 ORM/XMeta 目录），bean ID 经实测为**全限定类名**（`app.erp.mnt.service.posting.MaintenanceIssuePostingDispatcher`）非 camelCase——本计划镜像既有约定。

## Goals

- **后端落地工时费率解析器 + Dispatcher + Provider**（镜像 1100-6 范式）：
  - 工时费率先经 config 全局默认（`erp-mnt.default-labor-hourly-rate`，默认 0=关闭门控）。**Non-Goal 明确排除设备级 / 模板级 / 员工级费率物化**（归各自 successor；经实测 maintenance 域当前无 `ErpMntTemplate.standardLaborMinutes` 列也无 `ErpMntEquipment.laborHourlyRate` 列——见 Non-Goals 触发条件）。
  - `MaintenanceLaborPostingDispatcher.postLabor(ErpMntVisit visit, IServiceContext context)` 返回 `boolean`（**签名裁决见 Phase 1 Decision (e)**——既有 `MaintenanceIssuePostingDispatcher.dispatchIfApplicable(Long) → void` 通过吞异常实现「失败不阻断」；本计划选择 boolean 返回以便 BizModel 决定是否 log warn；两者均符合「失败不阻断业务」语义）：构造 PostingEvent（businessType=MAINTENANCE_LABOR + billHeadCode=`visit.code + "-ML"` + orgId/currencyId/acctSchemaId/exchangeRate/voucherDate 派生 + billData 携带 `TOTAL=laborCost` + `EQUIPMENT_CODE` + `TOTAL_MINUTES` + `HOURLY_RATE`）+ 调 `executor.postEvent(event)` 返回 voucherId != null；catch Exception → log + return false（失败不阻断业务，对齐 `dispatchIfApplicable` 范式）。
  - `MaintenanceLaborAcctDocProvider.createFacts(event, context)` 返回 Dr 6602 折旧费用（语义为「维修费用」，种子 subjectName 经实测为「折旧费用」，断言按 subjectCode 不受影响）/ Cr 2211 应付职工薪酬 两行 VoucherFact；amount 来自 billData.TOTAL。
  - bean 注册（Provider + Dispatcher 各 `<bean>` 声明，bean id = 全限定类名，对齐既有 1100-6 bean 注册范式）。
- **`ErpMntVisitBizModel.doComplete` 扩展工时过账触发**（config-gated，默认 false 向后兼容）：
  - `doComplete` 末尾（totalMinutes 已持久化后），如 `isPostingEnabled()`（内联 `AppConfig.var` 范式）且 visit.totalMinutes > 0，委派 `laborPostingDispatcher.postLabor(visit, context)` 返回 boolean（失败 log warn 不阻断，对齐 dispatchIfApplicable 范式）。
  - 守卫：visit.totalMinutes 不为空且 > 0（否则跳过过账，不抛错）；rate 解析失败抛 `ERR_LABOR_RATE_NOT_CONFIGURED`（仅 config-gated 开启时）或跳过（关闭时）。
  - 守卫：visit.posted 已为 true 时幂等跳过（对齐 `IErpFinVoucherBiz.post` 内置幂等范式）。
- **`ErpFinBusinessType.MAINTENANCE_LABOR(493)` 枚举值新增** + **`erp-fin/business-type` 字典项新增**（含 MAINTENANCE_LABOR + **同步补齐 MAINTENANCE_ISSUE latent defect Fix**——见 Current Baseline §剩余差距 #5）。
- **`ErpMntConstants` 扩展**（应用层非保护区域，仅常量；config 读取沿用 1100-6 内联 `AppConfig.var` 范式，不新增 ErpMntConfigs reader）。
- **`ErpMntErrors` 新增 ErrorCode**（VISIT_TOTAL_MINUTES_MISSING / LABOR_RATE_NOT_CONFIGURED）。
- **JUnit**：`TestErpMntLaborPosting`（新建，路径 `module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntLaborPosting.java` —— 对齐既有 `TestErpMntSparePartPosting.java` 扁平 `service/` 包路径，非 `service/posting/` 子包）覆盖正路径（complete → totalMinutes × rate 计算 → MAINTENANCE_LABOR 凭证 Dr 6602 / Cr 2211 精确数值）+ 部分场景（totalMinutes=30）+ totalMinutes=0 跳过 + config-gated 关闭零回归 + rate=0 守卫 + Provider 单元（businessType 三态分派）。
- **浏览器层 E2E**（1 新 spec `mnt-labor-posting.action.spec.ts`）：自包含 setup Equipment + Visit(status=IN_PROGRESS, startTime, endTime 预置产生非零 totalMinutes) → `complete(visitId)` → 断言 visit.status=COMPLETED + visit.posted=true + 经 `findVoucherIdByBillCode(visit.code + "-ML", "NORMAL")` 反查凭证存在 + `assertVoucherLines` Dr 6602 = (totalMinutes / 60) × rate / Cr 2211 = 同金额 精确数值 + 守卫（totalMinutes=0 visit 不产凭证 + config-gated 关闭不产凭证）。
- **owner doc 收口**：解除 2256-2 / 1100-6 / 1018-3 三处 Deferred（各补 `**RELEASED by 2026-07-18-0949-1**`）；`maintenance/state-machine.md:163` 实现注记更新；`assets/maintenance.md:111` 实现注记更新；`e2e-runbook` 业务动作表 + maintenance 工时行 + 凭证行断言表 +1 行（MAINTENANCE_LABOR Dr 6602 / Cr 2211）+ 套件计数对齐；当日日志聚合条目。

## Non-Goals

- **不加 `ErpMntEquipment.laborHourlyRate` ORM 列**（设备级工时费率物化）：属 ORM 保护区域变更。**经实测当前 maintenance schema 无此列**；本计划工时费率先经 config 全局默认（`erp-mnt.default-labor-hourly-rate`），如产品要求设备级费率分列，归独立 ORM successor（触发条件：产品要求设备级精确费率时——对齐制造域 workcenter.laborRate 拆分 successor 范式）。
- **不做维护任务模板级工时费率**：**经实测当前 maintenance 域无 `ErpMntTemplate` 实体也无 `standardLaborMinutes` 列**（独立草案审查 iter-1 修订——原文错误引用了不存在的实体）；最接近的 `ErpMntVisitTask` 也无 standardLaborMinutes 字段。模板级费率属不同维度 successor（触发条件：维护模板预算 vs 实际差异分析业务需求落地时——届时须先 ORM 加 `ErpMntTemplate` 实体或为 `ErpMntVisitTask` 加性追加列，再开 successor）。
- **不做员工级（`assignedTo`）工时费率**：`ErpMntVisit.assignedTo` 是 BIGINT 员工 id，员工级费率物化需 HR 域扩展（类似 projects 域 `CostRateResolver` 三级优先级 costRate 字段链）。本计划用 config 全局费率；员工级费率归 HR successor（触发条件：HR 薪酬按工时分摊成本核算业务需求落地时——对齐 HR `Additional Salary` 同型 Deferred 触发条件）。
- **不做反向红冲浏览器层 E2E（`reverseLabor` 显式入口）**：通用 `ErpFinVoucher__reverse(billHeadCode, businessType)` 已由 2004-2 orchestration 覆盖 DIRECT 红冲全路径；本计划工时凭证生成后红冲复用既有路径（红字凭证行同向取负断言对齐 0742-1 范式可作 spec 内 1 assertion，不专设 spec）。
- **不做 `cancel visit` 触发红冲**：`ErpMntVisit.cancel` 当前仅翻状态 + 设备恢复；如需 cancel 触发已生成 ML 凭证红冲，属不同结果面 successor（对齐备件消耗 cancel 同型 Non-Goal——1100-6 已声明）。
- **不做 labor 防双重扣减与 assets MAINTENANCE_EXPENSE(470) 联动**：备件消耗侧 2256-2 已闭合 linkedVisit 防双重扣减（Dr 6602 / Cr 2502 中转清算）；工时段无「双重扣减」语义（工时非存货，无 inventory 余额联动）；如资产维修同时含备件+工时，两凭证独立生成（Dr 6602 / Cr 不同科目），不冲突。本计划不扩展 linkedVisit 联动到工时段。
- **不做外币工时费率换算**：本位币工时费率已覆盖核心；外币维护合同（如外包维修）属不同结果面 successor（对齐 EXCHANGE_GAIN_LOSS 范式）。
- **不做 nop-job 定时批量计提**：complete 是事件驱动 DIRECT mutation，已覆盖核心业务路径；定时扫描已 complete 但未 posted 的 visit 批量补提属运营自动化面 successor（对齐 0718-1 同型 Deferred，触发条件「产品要求按日批量补提时」）。
- **不做累计工时字段物化**（如 `equipment.totalLaborCost` / `visit.laborCost`）：经 `ErpFinVoucherBillR` 反查（billHeadCode 含 visit.code）实现，无业务阻断。物化字段优化查询性能但增加一致性维护面。
- **不做 `ErpMntConfigs.laborPostingEnabled()` / `defaultLaborHourlyRate()` reader**：经实测 1100-6 的 config 读取范式是 Dispatcher 内联 `AppConfig.var(CONFIG_..., DEFAULT_...)`（非 ErpMntConfigs reader——ErpMntConfigs 仅有 autoGenerateDueVisits + equipmentStatusLinkEnabled 两 reader）。本计划对齐 1100-6 内联范式（Dispatcher 内私有 `isPostingEnabled()` helper），避免引入不存在范式引用。
- **不实现新后端契约/ORM/codegen**：本计划仅 service 层 BizModel 触发扩展 + 新 Dispatcher + 新 Provider + 1 枚举值 + 2 字典项（含 MAINTENANCE_ISSUE latent defect Fix 同步补齐）+ 常量 + ErrorCode + 测试 + 浏览器层 E2E。若 Explore 发现 latent defect 须根因诊断，重新加载 `nop-debugging`。

## Task Route

- Type: `implementation-only change`（maintenance service 层 BizModel 触发扩展 + 新 Dispatcher + 新 Provider + finance 域枚举/字典 1 项扩展 + 测试 + 浏览器层 E2E；除 finance 枚举/字典 ask-first 评估外，ORM/契约/codegen 无变更）
- Owner Docs: `docs/design/maintenance/state-machine.md`（§实现偏离补注 L163，既有）、`docs/design/maintenance/README.md`（§触发凭证，既有）、`docs/design/assets/maintenance.md`（§与 maintenance 域边界 L111，既有）、`docs/design/finance/posting.md`（`IErpFinAcctDocProvider` 机制 + 红冲机制方向二，既有）、`docs/testing/e2e-runbook.md`（业务动作表 + 凭证行断言表 + 套件结构，既有）。
- Skill Selection Basis: maintenance service 层 BizModel `doComplete` 钩子扩展（手写 Java 非 `_gen/`）+ 跨域访问（visit → equipment → acctSchema 解析）+ 业财过账（IErpFinVoucherBiz.post REQUIRES_NEW 事务语义 + `MaintenanceIssuePostingDispatcher.dispatchIfApplicable` 范式 + AcctDocProvider 新建）+ 错误处理（NopException + ErrorCode）→ **必须加载 `nop-backend-dev`**（决策门 / xbiz 动作声明 / 跨实体访问自检 / 异常处理规范 / 产品化可定制性自检）；JUnit 测试经 `JunitAutoTestCase` harness → 加载 `nop-testing`；浏览器层 E2E 本体 `Skill: none`（Playwright 浏览器层非 `nop-testing` 后端快照范畴，对齐 0606-2 范式裁决）。需阅读 `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`StringHelper`/`CoreMetrics`）+ `04-reference/safe-api-reference.md`（`IErpFinVoucherBiz.post` 安全 API 签名 + REQUIRES_NEW 幂等/事务契约）。
- Protected Areas: **finance 域 `ErpFinBusinessType` 枚举 + `erp-fin/business-type` 字典扩展属跨域契约变更**——1100-6 MAINTENANCE_ISSUE(492) 同型扩展先例已审计可接受（business-type 字典为业务类型登记表非会计科目保护区域），但本计划 Phase 1 Decision 须明确 ask-first 评估结论；maintenance service 层 BizModel + Dispatcher + AcctDocProvider 属应用层非会计保护区域；不改 ORM/契约/`_gen/`；任何生产缺陷须 ask-first / 开 successor。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移。
- 复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- **种子 COA 经实测已就绪**（独立草案审查 iter-1 修订）：
  - `2211` = **应付职工薪酬**（0742-2 已补）—— 正是 maintenance labor 应贷科目，**无需种子 CSV 加性追加**。
  - `6602` = **折旧费用**（种子 subjectName；1100-6 备件消耗已用同一 subjectCode；spec/JUnit 断言按 subjectCode 不受影响，如断言 subjectName 须期望「折旧费用」对齐 2256-2 M2 caveat）。
  - **科目方向裁决**：Dr 6602 / Cr 2211（详见 Phase 1 Decision (a)）—— **零种子 CSV 变更**。
- **webServer JVM arg 追加**（对齐 0606-2 / 2256-2 范式）：`-Derp-mnt.labor-posting-enabled=true` + `-Derp-mnt.default-labor-hourly-rate=80`（80 元/小时测试值，使 totalMinutes=60 → laborCost=80 确定性派生）。
- **回滚策略**：全部改动为应用层 Java + 1 枚举 + 2 字典项（含 MAINTENANCE_ISSUE latent Fix）+ 测试 + 文档，git 可逆；config-gated 默认 false 向后兼容（关闭时 `doComplete` 行为不变，既有 visit complete 测试零回归）；自包含 setup（测试专用 equipment+visit 隔离）+ finally cleanup（凭证+visit+equipment 逐域删，不污染 maintenance/finance 看板数值断言基线，对齐 0606-2 隔离范式）。

## Execution Plan

### Phase 1 - Decision：科目方向 + 费率解析链 + 配置点 + 触发点裁决

Status: completed
Targets: （本 Phase 仅产出 Decision 记录，无代码变更）
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

> **执行时实测裁决（HEAD 2026-07-18，read/grep 实测）**：
> - (a) **Dr 6602 / Cr 2211**——实测 `erp_md_subject.csv`：`2211` = 应付职工薪酬（LIABILITY/CREDIT，0742-2 已加性追加），`6602` = 折旧费用（EXPENSE/DEBIT，1100-6 已用），`2221` = 应交税费（VAT 占用，否决）。
> - (b) **单一 config 全局费率**——实测 maintenance schema 无 `ErpMntEquipment.laborHourlyRate`、无 `ErpMntTemplate` 实体、无 `standardLaborMinutes` 列；本计划仅落地 config 兜底层级。
> - (c) **`doComplete` 末尾内嵌触发**（updateEntity 持久化后、`equipmentStatusLinker.restoreToRunning` 之前）——实测 `ErpMntVisitBizModel.java:63-69` complete 结构 + doComplete:154-164。
> - (d) **`MAINTENANCE_LABOR(493)`**——实测 `ErpFinBusinessType` 既有 `MAINTENANCE_ISSUE(492)`、`MANUFACTURING_RECEIPT(500)`，493-499 空闲；同型先例 1100-6 已审计可接受。**latent defect 确认**：`business-type.dict.yaml` 经实测**缺失 MAINTENANCE_ISSUE 字典项**（1100-6 closure 声称已补但实际未落地），本计划同步补齐 MAINTENANCE_ISSUE + MAINTENANCE_LABOR。
> - (e) **`boolean postLabor(visit, context)`**——既有 `MaintenanceIssuePostingDispatcher.dispatchIfApplicable(Long)→void` 经 try/catch 吞异常实现「失败不阻断」；本计划 boolean 返回以便 BizModel 显式 `LOG.warn` 消费失败路径（语义等价，运维可见性更优）。

- [x] `Explore | Decision`：冷核实 5 项关键实现选择（须在编码前定，对齐 1100-6 / 0718-1 / 0718-2 Decision 范式；独立草案审查 iter-1 修订——扩充 Decision (a) 实测依据 + 新增 Decision (e) 签名裁决）：
  - **(a) 科目方向（Dr / Cr 科目编码）**——**经实测种子 CSV 已裁决**：
    - 候选 (1) Dr 6602 折旧费用 / Cr 2211 应付职工薪酬（**实测 2211 = 应付职工薪酬，0742-2 已补**；语义正确：工时本质员工薪酬计提，权责发生制；与 projects 域 0742-2 工时贷方 + HR 域 0215-3 SALARY 贷方跨域一致）
    - 候选 (2) ~~Dr 6602 / Cr 2221 应付职工薪酬-工资~~（**否决**：实测 2221 = **应交税费**，已被 VAT 系统占用——PurAcctDocProvider Dr 2221 进项税 / SalAcctDocProvider Cr 2221 销项税；复用会破坏 0704-1 凭证行断言）
    - 候选 (3) Dr 6602 / Cr 1002 银行存款（对齐 assets MAINTENANCE_EXPENSE 独立维修范式；但隐含「已支付」语义，不符工时计提权责发生制）
    - **倾向裁决**：(1) Dr 6602 / Cr 2211——权责发生制语义正确；与 HR/projects 域科目方向一致；**零种子 CSV 加性追加**（2211 已存在）。Phase 1 Explore 须核实 `ErpFinSubject` 表实测 2211 行存在性（已经核实，但实施时再次确认）。
  - **(b) 工时费率解析链**：
    - 候选 (1) 单一 config 全局费率 `erp-mnt.default-labor-hourly-rate`（默认 0=关闭门控；本计划仅落地此层级）
    - 候选 (2) 三级优先级：设备级 `ErpMntEquipment.laborHourlyRate`（须 ORM 加列，保护区域）→ 模板级（**经实测当前 maintenance schema 无 ErpMntTemplate 实体也无 standardLaborMinutes 列**——iter-1 修订，原文引用错误）→ config 兜底
    - 候选 (3) 员工级 `assignedTo` → HR 域 CostRateResolver（须 HR 域扩展，跨模块）
    - **倾向裁决**：(1) 单一 config 全局费率——本计划 Non-Goal 明确排除设备级/模板级/员工级费率物化（归各自 successor，且 schema 当前无相关列）；config 全局费率先落地，触发条件成熟后再开 successor 扩展优先级链。
  - **(c) 触发点（complete 内嵌 vs 独立 mutation）**：
    - 候选 (1) `doComplete` 末尾内嵌触发（config-gated，对齐 1100-6 SparePartUsage.confirm 范式）
    - 候选 (2) 新增独立 `@BizMutation postLaborCost(visitId)` mutation（用户显式调用）
    - **倾向裁决**：(1) 内嵌触发——对齐 1100-6 范式 + 备件消耗一致触发时机（complete 时点，totalMinutes 已确定）；用户面无需额外操作；config-gated 默认 false 向后兼容。Phase 1 Explore 须核实 `ErpMntVisitBizModel.doComplete:154-164` 当前结构 + `equipmentStatusLinker.restoreToRunning` 调用顺序（labor posting 应在设备状态恢复之前还是之后——倾向之前，对齐「业务字段先于副作用」范式）。
  - **(d) ErpFinBusinessType 枚举值编号 + 字典项扩展（跨域契约 ask-first 评估）**：
    - 候选 (1) `MAINTENANCE_LABOR(493)`——紧邻 MAINTENANCE_ISSUE(492)，编号连续；经实测 493-499 编号均空闲
    - **倾向裁决**：(1) 493——编号连续；Phase 1 Explore 须实测 `ErpFinBusinessType` 枚举 + `business-type.dict.yaml` 当前编号占用情况（已实测：490 LANDED_COST / 492 MAINTENANCE_ISSUE / 500 MANUFACTURING_RECEIPT，493 空闲）。finance 域跨域契约扩展须 Phase 1 Decision 明示 ask-first 评估结论（对齐 1100-6 同型扩展已审计可接受先例，business-type 字典为业务类型登记表非会计科目保护区域）。
    - **额外**：经实测 `business-type.dict.yaml` 缺失 `MAINTENANCE_ISSUE` 字典项（1100-6 closure 声称已补但实际未落地——latent defect）；本计划**同步补齐 MAINTENANCE_ISSUE + MAINTENANCE_LABOR 两条字典项**（R13 不可降级 Fix）。
  - **(e) Dispatcher 签名（boolean 返回 vs void 吞异常）**（iter-1 新增 Decision）：
    - 候选 (1) `boolean postLabor(visit, context)`——本计划可显式 log warn 区分成功/失败（对齐 postSettle 范式）
    - 候选 (2) `void dispatchLabor(Long visitId)` 吞异常（对齐 1100-6 实际 `dispatchIfApplicable(Long) → void` 范式——`MaintenanceIssuePostingDispatcher.java:83 + 115-122`）
    - **倾向裁决**：(1) boolean 返回——BizModel 调用方可在 `doComplete` 内显式 `LOG.warn` 标记失败路径便于运维定位（0718-1/0718-2 范式）；签名差异属可接受扩展（1100-6 void 吞异常范式是隐式 log，本计划 boolean 是显式 log，两者语义等价「失败不阻断业务」）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：将 5 项 Explore 结论记入本计划（更新本 Phase items 的「倾向裁决」为「最终裁决」+ 引用 Explore 实测证据），作为 Phase 2/3 实施依据。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 5 项 Decision 全部裁定并记录（科目方向 / 费率解析链 / 触发点 / 枚举编号 + ask-first 评估 / Dispatcher 签名）；Phase 2/3 实施依据明确；Phase 1 Explore 实测证据（科目存在性 / config 键命名 / BizModel 结构 / 枚举编号占用 / Dispatcher 既有签名范式）落地。

---

### Phase 2 - 后端 LaborPostingDispatcher + LaborAcctDocProvider + Visit.complete 触发扩展 + 枚举/字典/常量/ErrorCode

Status: completed
Targets: `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborPostingDispatcher.java`（新建）、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborAcctDocProvider.java`（新建——对齐既有 `MaintenanceIssueAcctDocProvider.java` 同包路径）、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/entity/ErpMntVisitBizModel.java`（`doComplete` 扩展 labor posting 触发）、`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java`（枚举值加 MAINTENANCE_LABOR）、`module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/business-type.dict.yaml`（字典项加 MAINTENANCE_LABOR + **同步补齐 MAINTENANCE_ISSUE latent defect Fix**）、`module-maintenance/erp-mnt-service/src/main/resources/_vfs/erp/mnt/beans/app-service.beans.xml`（**iter-1 修订：经实测路径是 `beans/` 非 `model/`**，bean 注册）、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/ErpMntConstants.java`（CONFIG_LABOR_POSTING_ENABLED + 默认值常量）、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/ErpMntErrors.java`（新增 ErrorCode）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 Decision 已记录

- [x] `Add`: `MaintenanceLaborPostingDispatcher.postLabor(ErpMntVisit visit, IServiceContext context)` 返回 `boolean`（对齐 `MaintenanceIssuePostingDispatcher` 范式 + Phase 1 Decision (e) 裁决 boolean 返回）：
  - **config 读取经内联 `AppConfig.var` 范式**（**iter-1 修订**——经实测 `ErpMntConfigs` 仅有 autoGenerateDueVisits + equipmentStatusLinkEnabled，无 `sparePartPostingEnabled()` reader；1100-6 范式是 `MaintenanceIssuePostingDispatcher.java:221-225` 内联）：私有 `isPostingEnabled()` + `getDefaultLaborHourlyRate()` 两 helper。
  - 守卫：visit.totalMinutes != null && > 0（否则 return false 不抛错）；rate = `getDefaultLaborHourlyRate() > 0`（否则 return false 跳过）。
  - 计算 `laborCost = totalMinutes × rate / 60`（HALF_UP scale=4；rate 单位「元/小时」totalMinutes 单位「分钟」，除以 60 转换）。
  - 构造 PostingEvent：businessType=MAINTENANCE_LABOR；billHeadCode=`visit.getCode() + "-ML"`；orgId=visit.getOrgId()；acctSchemaId=`resolveAcctSchemaId(visit.getOrgId())`（既有 helper）；currencyId=`resolveCurrencyId(visit.getOrgId())`；exchangeRate=BigDecimal.ONE；voucherDate=`CoreMetrics.today()`。
  - billData 携带：`TOTAL=laborCost` + `EQUIPMENT_CODE=visit.equipment.code`（经 equipment 关系 getter）+ `TOTAL_MINUTES=visit.totalMinutes` + `HOURLY_RATE=rate` + `VISIT_CODE=visit.code`。
  - 调 `executor.postEvent(event)` 返回 voucherId；非 null → true；null → false（幂等命中）；catch Exception → log + return false（失败不阻断业务，对齐既有范式）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `MaintenanceLaborAcctDocProvider implements IErpFinAcctDocProvider`（对齐 `MaintenanceIssueAcctDocProvider.java` 同包路径）：
  - `getSupportedBusinessTypes()` = `EnumSet.of(ErpFinBusinessType.MAINTENANCE_LABOR)`。
  - `createFacts(PostingEvent event, IServiceContext context)` 返回 List<VoucherFact>：
    - amount = `readDecimal(event.getBillData().get(KEY_TOTAL))`（既有 helper 范式）。
    - Dr 6602 折旧费用（DC_DEBIT，amount；种子 subjectName 经实测为「折旧费用」，断言按 subjectCode 不受影响）。
    - Cr 2211 应付职工薪酬（DC_CREDIT，amount——依据 Phase 1 Decision (a) 最终裁决）；partnerId=null（应付职工薪酬非业务伙伴辅助核算）。
  - 常量 `KEY_TOTAL` / `KEY_EQUIPMENT_CODE` / `KEY_TOTAL_MINUTES` / `KEY_HOURLY_RATE` / `KEY_VISIT_CODE`（对齐 MaintenanceIssueAcctDocProvider 命名范式）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpMntVisitBizModel.doComplete` 扩展 labor posting 触发（config-gated 默认 false 向后兼容）：
  - 在现有 `doComplete` 末尾（`updateEntity` 持久化 + `equipmentStatusLinker.restoreToRunning` 之间或之后——Phase 1 Decision (c) 裁决）追加：
    ```java
    if (laborPostingDispatcher.isPostingEnabled()) {
        if (!laborPostingDispatcher.postLabor(visit, context)) {
            LOG.warn("Labor posting skipped or failed for visit {}", visit.getCode());
        }
    }
    ```
    （**iter-2 修订**：config 读取由 Dispatcher 内 `isPostingEnabled()` helper 暴露对齐 1100-6 范式；**boolean 返回值显式消费**——false 时 LOG.warn 标记跳过/失败路径，实现 Decision (e) 显式运维可见性语义。）
  - `@Inject MaintenanceLaborPostingDispatcher laborPostingDispatcher;`（**`@Inject` 字段不能为 `private`**——AGENTS.md Nop 规则；对齐 `ErpMntSparePartUsageBizModel.java:38` 既有 `MaintenanceIssuePostingDispatcher issuePostingDispatcher` 具体类注入范式）。
  - **零回归保证**：config-gated 默认 false → 既有 visit complete 测试（`TestErpMntVisitRequestStateMachine` / `TestErpMntDowntimeAndE2E`）行为不变。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpFinBusinessType.MAINTENANCE_LABOR(493)` 枚举值（依据 Phase 1 Decision (d) 最终编号——经实测 493 空闲）：
  - finance 域跨域契约扩展，对齐 1100-6 MAINTENANCE_ISSUE(492) 同型扩展范式。
  - Skill: `nop-backend-dev`
- [x] `Add | Fix`: `erp-fin/business-type.dict.yaml` 加 2 项字典项：
  - MAINTENANCE_LABOR 新增（本计划目标）。
  - **MAINTENANCE_ISSUE latent defect Fix**（iter-1 修订——经实测 dict 当前缺失此项；1100-6 closure 声称已补但实际未落地；R13 不可降级，本计划同步补齐；label/description/order 对齐既有 MAINTENANCE_EXPENSE 范式）。
  - **iter-3 修订（round 3 修复根因）**：经实测 `business-type.dict.yaml` 是 `# __XGEN_FORCE_OVERRIDE__` codegen 产物，由 `module-finance/model/app-erp-finance.orm.xml` `<dict name="erp-fin/business-type">` 块的 `<option>` 元素生成。实际编辑落地于 **orm.xml 源**（line 117-120 新增 2 个 `<option>` 元素，含 code/label/value/i18n-en:label + 源 plan 注释），重新构建后 dict.yaml 经 codegen 正确重生成（line 196-202 新增 2 条目）。Round 1/2 audit 失败因执行者直接编辑产物被 codegen 抹除；round 3 改锚定 orm.xml 源后通过。**未来此字典加项必须编辑 orm.xml 源**。
  - Skill: `nop-backend-dev`
- [x] `Add`: `app-service.beans.xml` 注册 `MaintenanceLaborPostingDispatcher` + `MaintenanceLaborAcctDocProvider` 各 1 条 `<bean>` 声明：
  - **iter-1 修订**：路径 `_vfs/erp/mnt/beans/app-service.beans.xml`（非 `model/`）；bean id 经实测为**全限定类名**（`app.erp.mnt.service.posting.MaintenanceLaborPostingDispatcher` 等），非 camelCase（对齐既有 `app.erp.mnt.service.posting.MaintenanceIssuePostingDispatcher` 范式）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpMntConstants` 扩展（**iter-1 修订**——仅常量；config 读取对齐 1100-6 内联 `AppConfig.var` 范式，不新增 ErpMntConfigs reader）：
  - `String CONFIG_LABOR_POSTING_ENABLED = "erp-mnt.labor-posting-enabled"` + `boolean DEFAULT_LABOR_POSTING_ENABLED = false`（对齐既有 `CONFIG_SPARE_PART_POSTING_ENABLED` + `DEFAULT_SPARE_PART_POSTING_ENABLED` 命名——**iter-1 修订：经实测无 `CONFIG_VALUE_` 前缀**）。
  - `String CONFIG_DEFAULT_LABOR_HOURLY_RATE = "erp-mnt.default-labor-hourly-rate"` + `String DEFAULT_LABOR_HOURLY_RATE_VALUE = "0"`。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpMntErrors` 新增 ErrorCode：
  - `ERR_VISIT_TOTAL_MINUTES_MISSING`（visit.totalMinutes 为空或 ≤0 时——但 config-gated 关闭时不抛，仅开启时若 rate>0 但 totalMinutes=0 抛）。
  - `ERR_LABOR_RATE_NOT_CONFIGURED`（config-gated 开启但 rate≤0 时——仅 Explore 裁决是否抛错 vs 跳过）。
  - 描述用中文（i18n 处理翻译），对齐既有 `ERR_VISIT_NOT_FOUND` 范式。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 后端方法签名稳定（`MaintenanceLaborPostingDispatcher.postLabor` + `MaintenanceLaborAcctDocProvider.createFacts` + `ErpMntVisitBizModel.doComplete` 扩展触发）；本地化 `mvn install -pl module-maintenance/erp-mnt-service -am -DskipTests` BUILD SUCCESS（解除 Phase 3/4 测试与 E2E 阻塞）；finance-dao 传递新枚举值 install 成功。
- [x] Provider `getSupportedBusinessTypes` + `createFacts` 业务类型分派不破坏既有 MAINTENANCE_ISSUE Provider（不同 supportedTypes 集合，AcctDocRegistry 按 businessType 路由）；既有 `TestErpMntSparePartPosting` SETTLE_TYPE=null 默认路径行为不变（零回归验证）。
- [x] config-gated 默认 false 时 `ErpMntVisitBizModel.doComplete` 行为零回归（既有 `TestErpMntVisitRequestStateMachine` / `TestErpMntDowntimeAndE2E` 全绿）。

---

### Phase 3 - JUnit 后端单元/集成测试

Status: completed
Targets: `module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntLaborPosting.java`（新建——**iter-1 修订：对齐既有 `TestErpMntSparePartPosting.java` 扁平 `service/` 包路径，非 `service/posting/` 子包**）
Skill: `nop-testing`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 2

- [x] `Decision`: 测试 harness 选择——`JunitAutoTestCase` + `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`（端到端经真实 ORM/DB + `IErpFinVoucherBiz.post` 链路，对齐 `TestErpMntSparePartPosting` 范式）。**新建文件**（既有仅 `TestErpMntSparePartPosting`，无 `LaborPosting` 测试）。
  - Skill: `nop-testing`
- [x] `Add`: `TestErpMntLaborPosting` 覆盖：
  - **测试 1（正路径 complete → totalMinutes × rate 计算 → MAINTENANCE_LABOR 凭证精确数值）**：setup equipment + visit(status=IN_PROGRESS, startTime=now-60min, endTime=now) → 启用 config `erp-mnt.labor-posting-enabled=true` + `erp-mnt.default-labor-hourly-rate=80` → `complete(visitId)` → 断言 visit.status=COMPLETED + visit.totalMinutes=60 + 经 `IErpFinVoucherBiz` 反查凭证 businessType=MAINTENANCE_LABOR + voucherType=NORMAL + billHeadCode=`visit.code + "-ML"` + 凭证行 Dr 6602 = 60×80/60 = 80 / Cr 2211 = 80（依据 Phase 1 Decision (a) Dr 6602 / Cr 2211）。
  - **测试 2（部分时长 totalMinutes=30）**：setup visit(startTime=now-30min, endTime=now) → complete → 断言凭证行 Dr 6602 = 30×80/60 = 40 / Cr 2211 = 40。
  - **测试 3（totalMinutes=0 跳过过账）**：setup visit(startTime=null 或 startTime=endTime) → complete → 断言无 MAINTENANCE_LABOR 凭证生成（visit.posted 保持 false 或不变）。
  - **测试 4（config-gated 关闭零回归）**：关闭 config `erp-mnt.labor-posting-enabled=false` → complete → 断言无 MAINTENANCE_LABOR 凭证（既有 visit complete 行为不变）。
  - **测试 5（rate=0 跳过或抛错——依据 Phase 1 Decision (b)）**：rate=0 + config-gated 开启 → complete → 断言行为（跳过无凭证 OR 抛 ERR_LABOR_RATE_NOT_CONFIGURED）。
  - **测试 6（Provider createFacts 单元）**：直接 `new MaintenanceLaborAcctDocProvider().createFacts(event, ctx)` 单测：(a) MAINTENANCE_LABOR businessType → Dr 6602 / Cr 2211；(b) 非 MAINTENANCE_LABOR businessType 返回空列表（getSupportedBusinessTypes 守门）。
  - Skill: `nop-testing`
- [x] `Proof`: `mvn test -pl module-maintenance/erp-mnt-service -am` 全绿（既有 mnt-service tests + 6 新增 0 failures/0 errors）+ 既有 `TestErpMntSparePartPosting`（备件消耗路径）零回归。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 6 测试用例全绿（正路径 60min + 部分 30min + totalMinutes=0 跳过 + config 关闭零回归 + rate=0 守卫 + Provider 单元）；凭证行精确数值断言对齐 Phase 1 Decision (a) Dr 6602 / Cr 2211。
- [x] 既有 `TestErpMntSparePartPosting`（0606-2 落地 JUnit）+ maintenance service 抽样回归零回归（实测 49 tests/0 failures/0 errors；config-gated 默认 false 路径行为不变；Playwright spec 回归归 Phase 4 / Closure Gates 范畴）。

---

### Phase 4 - 浏览器层 E2E spec + 文档对齐 + 日志

Status: completed
Targets: `tests/e2e/business-actions/mnt-labor-posting.action.spec.ts`（新建）、`docs/testing/e2e-runbook.md`（业务动作表 + 凭证行断言表 + 套件计数）、`docs/plans/2026-07-17-2256-2-...md`（Deferred RELEASED）、`docs/plans/2026-07-10-1100-6-...md`（Deferred RELEASED）、`docs/plans/2026-07-03-1018-3-...md`（Deferred RELEASED）、`docs/design/maintenance/state-machine.md`（实现注记更新）、`docs/design/assets/maintenance.md`（实现注记更新）、`docs/logs/2026/07-18.md`、`docs/backlog/README.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] `Add`: `mnt-labor-posting.action.spec.ts` —— 自包含 setup（finally 兜底 cleanup），inline 复用 0606-2 spec 内 setup 范式：
  - **测试 1（正路径 complete → MAINTENANCE_LABOR 凭证精确数值）**：自包含建 Equipment（id 自分配）+ Visit(DRAFT, equipmentId, assignedTo, visitDate, startTime 预置 60min 前, endTime 预置 now) → `schedule` → `start` → `complete(visitId)` → verifyState 经 `__get` 独立断言 visit.status=COMPLETED + visit.totalMinutes=60 → 经 `findVoucherIdByBillCode(visit.code + "-ML", "NORMAL")` 反查凭证 + `assertVoucherLines` Dr 6602=80 / Cr 2211=80 精确数值（rate=80 元/小时，totalMinutes=60 → 60×80/60=80）。
  - **测试 2（部分时长）**：同 setup 但 startTime 预置 30min 前 → complete → 凭证行 Dr 6602=40 / Cr 2211=40。
  - **测试 3（totalMinutes=0 守卫）**：setup visit(startTime=endTime=now 或 startTime=null) → complete → verifyState status=COMPLETED + `findVoucherIdByBillCode` 返回 null（无 MAINTENANCE_LABOR 凭证）。
  - **清理**：finally 删 voucher（经 cleanupVoucherByBillCode 删 `visit.code + "-ML"` 凭证）+ visit + equipment。
  - Skill: none
- [x] `Proof`: spec 独立运行全绿（`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/mnt-labor-posting.action.spec.ts --workers=1` 3 passed）+ maintenance 既有 spec 抽样回归（mnt-spare-part-posting + mnt-request + mnt-visit + mnt-ast-linked-visit-anti-double-deduct）0 新增失败。
  - Skill: none
- [x] `Add`: 文档对齐：
  - `e2e-runbook` 业务动作表 +1 maintenance 工时行 + 凭证行断言表 +1 行（MAINTENANCE_LABOR Dr 6602 / Cr 2211）+ 套件计数对齐（实测后增）。
  - `2026-07-17-2256-2` Deferred 段补 `**RELEASED by 2026-07-18-0949-1**` 行 + 实施摘要。
  - `2026-07-10-1100-6` Deferred 段补 `**RELEASED by 2026-07-18-0949-1**` 行 + 实施摘要。
  - `2026-07-03-1018-3` Deferred 段补 `**RELEASED by 2026-07-18-0949-1**` 行 + 实施摘要。
  - `docs/design/maintenance/state-machine.md:163` 「工时费用化凭证 successor」实现注记更新为「已实现（plan 2026-07-18-0949-1）：Dr 6602 / Cr 2211，config-gated `erp-mnt.labor-posting-enabled` 默认关」。
  - `docs/design/assets/maintenance.md:111` 实现注记更新（同上 + 注明与 MAINTENANCE_EXPENSE(470) 的语义差异）。
  - `docs/logs/2026/07-18.md` 增聚合条目（背景 / Phase 1-4 / 验证状态 / 范围纪律）。
  - `docs/backlog/README.md` +1 done 行。
  - Skill: none

Exit Criteria:

- [x] 1 spec 全绿（3 用例：正路径 + 部分时长 + totalMinutes=0 守卫）；字段翻转 + 凭证行数值均经 `verifyState` `__get` 与 `findVoucherIdByBillCode` + `assertVoucherLines` 独立反查（非仅 mutation 返回值）。
- [x] finally cleanup 保护共享 DB（maintenance/finance 看板数值断言基线无漂移，对齐 0606-2 自包含 equipment+visit 隔离范式）。
- [x] e2e-runbook 业务动作表 + 凭证行断言表 + 套件计数对齐；2256-2 + 1100-6 + 1018-3 三处 Deferred RELEASED 登记落地；owner doc 实现注记就位；日志 + backlog 条目在位。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_08d109ad8ffe8QKjAvbyplEaq3`，general agent 新会话冷审计) — 2 BLOCKERS / 4 MAJORS / 4 MINORS。15+ load-bearing 事实主张经实时仓库逐项核实，发现 2 处事实伪（科目方向）+ 1 处 latent defect 漏识别（字典项缺失）+ 4 处范式/路径引用错误：
  - **B1（Blocker）**：Decision (a) preferred `Dr 6602 / Cr 2221 应付职工薪酬-工资` 建立在**伪前提**上——实测 `erp_md_subject.csv` 2221 = **应交税费**（Taxes Payable），已被 VAT 系统占用（PurAcctDocProvider Dr 2221 进项税 / SalAcctDocProvider Cr 2221 销项税，0704-1 凭证行断言依赖）。复用 2221 会破坏既有 AP/AR VAT 测试。
  - **B2（Blocker）**：原文错误地将 2211 描述为「其他应收款-员工预支」并 dismiss——实测 `erp_md_subject.csv` 2211 = **应付职工薪酬**（0742-2 已加性追加，0742-2 line 32+66 + 0215-3 line 140 均确认）；1218-2 实际加的是 2241 + 1221，非 2211。**正确的科目方向裁决应为 Dr 6602 / Cr 2211**（与 projects/hr 域跨域一致），零种子 CSV 变更。
  - **M1（Major）**：`erp-fin/business-type.dict.yaml` 经实测**缺失 MAINTENANCE_ISSUE 字典项**（1100-6 closure 声称已补但实际未落地——latent defect）；本计划应同步补齐（R13 不可降级 Fix）。
  - **M2（Major）**：`ErpMntConfigs` 实测仅有 `autoGenerateDueVisits()` + `equipmentStatusLinkEnabled()` 两 reader，**无 `sparePartPostingEnabled()`**——1100-6 实际范式是 `MaintenanceIssuePostingDispatcher.java:221-225` 内联 `AppConfig.var`；本计划应改用内联范式而非新增 ErpMntConfigs reader。
  - **M3（Major）**：**当前 maintenance schema 无 `ErpMntTemplate` 实体也无 `standardLaborMinutes` 列**——原文多次引用此不存在实体；应移除三级费率解析链中的模板级 + 修正 Non-Goals。
  - **M4（Major）**：`app-service.beans.xml` 路径实测为 `_vfs/erp/mnt/beans/`（**非** `_vfs/erp/mnt/model/`），bean id 实测为全限定类名（**非** camelCase）；原文路径 + bean id 均错误。
  - **m1**：`TestErpMntLaborPosting.java` 应放在 `service/` 扁平包路径（对齐 `TestErpMntSparePartPosting.java`），非 `service/posting/` 子包。
  - **m2**：常量名应为 `DEFAULT_SPARE_PART_POSTING_ENABLED`（无 `CONFIG_VALUE_` 前缀）。
  - **m3**：种子 6602 subjectName 实测为「折旧费用」非「维修费用」（断言按 subjectCode 不受影响，但 subjectName 断言须期望实际值）。
  - **m4**：`MaintenanceIssuePostingDispatcher` 既有方法签名是 `void dispatchIfApplicable(Long)` 非 `boolean postIssue`（语义等价「失败不阻断」但范式引用不准确）。
- 修订落地（iteration 1 → 2）：
  - **B1/B2 修订**：Decision (a) preferred 裁决**反转**为 `Dr 6602 / Cr 2211`（零种子 CSV 变更）；删除「须种子 CSV 加 1 行 2221」Phase 2 deliverable；所有 JUnit/E2E 断言 + owner-doc 收口 + Goals 中科目方向从 2221 改为 2211；Infrastructure Prereqs 段更新种子 COA 实测状态（含 6602/2211/2221 三科目语义注）。
  - **M1 修订**：Phase 2 字典项 deliverable 从「加 1 项 MAINTENANCE_LABOR」改为「加 2 项 MAINTENANCE_LABOR + MAINTENANCE_ISSUE latent Fix」（Item Types 加 `Fix`）；Current Baseline §剩余差距 #5 新增条目说明 latent defect。
  - **M2 修订**：Phase 2 deliverable 删除 `ErpMntConfigs.laborPostingEnabled()` + `defaultLaborHourlyRate()` reader；改为 Dispatcher 内私有 `isPostingEnabled()` + `getDefaultLaborHourlyRate()` helper（对齐 1100-6 内联范式）；Goals + Non-Goals + Closure Gates 同步更新。
  - **M3 修订**：移除所有 `ErpMntTemplate.standardLaborMinutes` 引用；Goals 中三级费率解析链改为单一 config 全局费率（Non-Goal 明确排除设备级/模板级/员工级，并加注「schema 当前无相关列」）；Deferred「维护任务模板级」条目加 iter-1 修订注。
  - **M4 修订**：Phase 2 Targets 路径 `_vfs/erp/mnt/model/app-service.beans.xml` 改为 `_vfs/erp/mnt/beans/app-service.beans.xml`；bean id 改为全限定类名格式（对齐既有 MaintenanceIssuePostingDispatcher bean 注册范式）。
  - **m1 修订**：Phase 3 Targets 测试路径 `service/posting/TestErpMntLaborPosting.java` 改为 `service/TestErpMntLaborPosting.java`。
  - **m2 修订**：常量命名订正 `CONFIG_VALUE_DEFAULT_SPARE_PART_POSTING_ENABLED` → `DEFAULT_SPARE_PART_POSTING_ENABLED`（对齐既有命名）。
  - **m3 修订**：Infrastructure Prereqs + Goals + Phase 2 Provider item 加 6602 subjectName caveat（实测「折旧费用」）。
  - **m4 修订**：Goals Dispatcher 描述 + Phase 2 第一 item 引用改为 `MaintenanceIssuePostingDispatcher.dispatchIfApplicable` 范式，并新增 Decision (e) 裁决 boolean 返回 vs void 吞异常（本计划选 boolean 以便 BizModel 显式 log warn）。
  - R1-R14 + anti-slack + legitimately warranted iter-1 评估作为修订依据保留。
- Independent draft review iteration 2: needs-revision (`ses_08d07dd88ffex3Utfasu7gToNq`，general agent 新会话冷审计) — **0 BLOCKERS / 3 MAJORS / 3 MINORS**。iter-1 修订全部 2 BLOCKERS + 4 MAJORS 经实时仓库逐项核实 FIXED；新增 6 处局部 sweep/consistency 问题（无架构重审需要）：
  - **N1（Major）**：Phase 4 owner doc update 文本 L312 `Dr 6602 / Cr 2221` 漏改 → 修订为 `Cr 2211`。
  - **N2（Major）**：Decision (e) boolean 返回值的 rationale 未被 Phase 2 snippet 消费（return 被 discard，仅 try/catch 包裹——但 Dispatcher 已内部吞异常，外层 try/catch 是死代码）→ 修订 snippet 为 `if (!postLabor(...)) LOG.warn(...)` 显式消费 boolean。
  - **N3（Major）**：Current Baseline L36 仍含 `ErpMntTemplate.standardLaborMinutes（已在 schema）` 伪陈述（与 M3 修订矛盾）→ 修订为「schema 当前无相关列」+ 单路 config 派生。
  - **n1（Minor）**：Baseline L52 仍含 stale `ErpMntConfigs reader` deliverables（与 M2 修订矛盾）→ 修订为「不新增 reader，对齐 1100-6 内联范式」。
  - **n2（Minor）**：Baseline L50 仍含 `CONFIG_VALUE_DEFAULT_SPARE_PART_POSTING_ENABLED` 错误命名（与 m2 修订矛盾）→ 修订为 `DEFAULT_SPARE_PART_POSTING_ENABLED`。
  - **n3（Minor）**：Baseline L42 仍含 `postIssue(...) → boolean` 错误签名描述（与 m4 修订矛盾）→ 修订为 `dispatchIfApplicable(Long) → void`。
  - iter-2 修订：6 处 sweep 全部落地（N1 L312 Cr 2221→2211 / N2 Phase 2 snippet `if (!postLabor) LOG.warn` 消费 boolean / N3 Baseline L36 重写 + n1 L52 / n2 L50 / n3 L42 sweep 对齐 iter-1 修订）。R1-R14 + anti-slack + anti-hollow + legitimately warranted iter-2 评估全 PASS。
- Independent draft review iteration 3: **accept** (`ses_08d03da7fffeuUhq2nyXmLoKMT`，general agent 新会话冷审计) — **0 BLOCKERS / 0 MAJORS / 3 MINORS**。iter-2 修订全部 6 处 sweep 经实时仓库逐项核实 FIXED（N1/N2/N3/n1/n2/n3）；新增 3 处文档级 consistency Minors（无架构影响）：
  - **m-i（Minor）**：Goals L103 + Task Route L135 仍含 stale `postIssue` 范式引用（应仅 `dispatchIfApplicable`）→ 已 sweep。
  - **m-ii（Minor）**：Phase 3 Exit Criteria L284 误引用 Playwright spec `mnt-spare-part-posting.action.spec.ts`（应 JUnit `TestErpMntSparePartPosting`，Playwright 归 Phase 4/Closure Gates）→ 已订正。
  - **m-iii（Minor）**：iter-3 placeholder 未持久化（bookkeeping）→ 本条记录即填补。
  - Stale reference sweep 全 PASS：无 `2221` / `ErpMntTemplate` / `laborPostingEnabled()` reader / `CONFIG_VALUE_` 前缀污染 active deliverables。Decision (e) + Phase 2 snippet 完全 coherent（boolean 显式消费于 LOG.warn，无冗余 try/catch）。
  - R1-R14 + anti-slack + anti-hollow + legitimately warranted 全 PASS。
  - 共识达成 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划触及 maintenance service 层生产代码（1 BizModel 触发扩展 + 1 新 Dispatcher + 1 新 Provider + bean + 枚举 + 2 字典项含 MAINTENANCE_ISSUE latent Fix + 常量 + ErrorCode）+ finance 域枚举/字典跨域契约扩展（ask-first 评估在 Phase 1 Decision）+ 测试 + 文档；Closure Gates 须含完整 `mvn` 验证 + Playwright 全套件回归。

- [x] 范围内行为完成（LaborPostingDispatcher + LaborAcctDocProvider + Visit.complete 触发扩展 + MAINTENANCE_LABOR 枚举/字典 + MAINTENANCE_ISSUE 字典 latent Fix + bean 注册 + config 常量 + ErrorCode + JUnit 6 用例 + 1 spec 3 用例）
- [x] 相关文档对齐（e2e-runbook + 2256-2/1100-6/1018-3 三处 Deferred RELEASED + maintenance/state-machine.md + assets/maintenance.md 实现注记 + 当日日志 + backlog）
- [x] 已运行验证：`mvn test -pl module-maintenance/erp-mnt-service -am` 全绿（含新增 6 tests）+ 新增 spec 独立运行全绿 + maintenance 既有 spec 抽样回归 0 新增失败（特别 mnt-spare-part-posting + mnt-ast-linked-visit-anti-double-deduct + TestErpMntSparePartPosting 既有路径零回归）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（closure gate，确认零后端污染）
- [x] 无范围内项目降级为 deferred/follow-up（设备级费率 / 模板级费率 / 员工级费率 / cancel 红冲 / 外币费率 / nop-job 定时批量 / 累计字段物化 / linkedVisit 工时联动 / ErpMntConfigs reader 均为计划内 Non-Goal 附触发条件；MAINTENANCE_ISSUE latent Fix 经 R13 不可降级纳入 Phase 2）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（round 3 APPROVED — 见 Closure Audit Evidence 段 round 3 verdict）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 设备级工时费率物化（`ErpMntEquipment.laborHourlyRate` ORM 列）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 不同设备不同费率属合理业务需求（设备等级差异），但落地需 ORM 加列（保护区域）+ UI 维护面 + 配置同步。**经实测当前 maintenance schema 无此列**；本计划用 config 全局费率满足核心计提；设备级覆盖属不同结果面。
- Successor Required: `yes`（触发条件：产品要求设备级精确费率时——对齐制造域 workcenter.laborRate 拆分 successor 范式）

### 维护任务模板级工时费率（`ErpMntTemplate.standardLaborMinutes` 反推）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: **经实测当前 maintenance 域无 `ErpMntTemplate` 实体也无 `standardLaborMinutes` 列**（iter-1 修订——原文错误引用了不存在的实体）。模板级费率属预算 vs 实际差异分析面 successor，须先 ORM 加实体或加性列再开 successor。
- Successor Required: `yes`（触发条件：维护模板预算 vs 实际差异分析业务需求落地时）

### 员工级（`assignedTo`）工时费率

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpMntVisit.assignedTo` 是员工 id，员工级费率物化需 HR 域扩展（类似 projects 域 CostRateResolver）。本计划用 config 全局费率。
- Successor Required: `yes`（触发条件：HR 薪酬按工时分摊成本核算业务需求落地时——对齐 HR `Additional Salary` 同型 Deferred 触发条件）

### cancel visit 触发已生成 ML 凭证红冲

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpMntVisit.cancel` 当前仅翻状态 + 设备恢复；cancel 触发红冲属不同结果面 successor（对齐备件消耗 cancel 同型 Non-Goal——1100-6 已声明）。
- Successor Required: `yes`（触发条件：cancel visit 业务流程须回滚已生成凭证时）
- **RELEASED by 2026-07-18-1745-1**（plan `2026-07-18-1745-1-maintenance-posting-reversal-closure.md` 落地 `MntPostingExecutor.reverse` + `MaintenanceLaborPostingDispatcher.reverseLabor` + `ErpMntVisitBizModel.doCancel` 内嵌 config-gated 红冲触发 + JUnit 3 用例 + E2E 1 spec 1 用例，已完成独立结束审计 APPROVED）

### nop-job 定时批量补提

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: complete 是事件驱动 DIRECT mutation，已覆盖核心业务路径；定时扫描已 complete 但未 posted 的 visit 批量补提属运营自动化面 successor。
- Successor Required: `yes`（触发条件：产品要求按日批量补提时——对齐 0718-1 同型 Deferred 触发条件）

### 累计工时字段物化（`equipment.totalLaborCost` / `visit.laborCost`）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 经 `ErpFinVoucherBillR` 反查（billHeadCode 含 visit.code）实现，无业务阻断。物化字段优化查询性能但增加一致性维护面。
- Successor Required: `yes`（触发条件：设备维度累计工时成本高频查询性能问题落地时）

### 外币工时费率换算

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本位币工时费率已覆盖核心；外币维护合同（如外包维修）属不同结果面 successor。
- Successor Required: `yes`（触发条件：外币维护合同业务落地时——对齐 EXCHANGE_GAIN_LOSS 范式）

### ErpMntConfigs reader 引入（laborPostingEnabled / defaultLaborHourlyRate）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划对齐 1100-6 内联 `AppConfig.var` 范式（Dispatcher 内私有 helper）。ErpMntConfigs reader 引入属可读性优化面 successor（如统一所有 mnt config reader 模式时），非功能性缺口。
- Successor Required: `no`（触发条件：maintenance 域 config reader 统一重构时）

## Closure

Status Note: **结束审计通过（round 3 APPROVED，独立子代理冷审计新会话）**。Phase 1-4 主体落地（Java Dispatcher/Provider/BizModel 钩子/枚举/bean/JUnit/E2E/owner-doc 均实测对齐）。Round 1 audit 发现 Phase 2 deliverable #5（dict.yaml 加 MAINTENANCE_LABOR + MAINTENANCE_ISSUE 字典项）经实时仓库核实未落地——执行者首次修复误编辑生成产物 dict.yaml（`# __XGEN_FORCE_OVERRIDE__` 标记），被 `mvn clean install` codegen 覆写抹除（round 2 audit 实测 git diff 为空，PHANTOM fix）；round 3 修复根因——识别 dict.yaml 是 `module-finance/model/app-erp-finance.orm.xml` `<dict name="erp-fin/business-type">` 块的 codegen 产物，改为在 orm.xml 源加 2 个 `<option>` 元素（MAINTENANCE_ISSUE + MAINTENANCE_LABOR，含 i18n-en:label + 源 plan 引用注释），重新构建后 dict.yaml 经 codegen 正确重生成且经实测在 `mvn clean install` 后保留（build idempotence 验证通过）。四 Phase 主体：

- **Phase 1（Decision）**：5 项 Decision 裁定并记录（科目方向 Dr 6602 / Cr 2211 + 单一 config 全局费率 + doComplete 内嵌触发 + MAINTENANCE_LABOR(493) 枚举 + boolean postLabor 签名），含 MAINTENANCE_ISSUE 字典项 latent defect 同步补齐裁决。
- **Phase 2（后端实现）**：`MaintenanceLaborPostingDispatcher`（config-gated + 幂等判重 + try/catch 吞异常）+ `MaintenanceLaborAcctDocProvider`（Dr 6602 / Cr 2211）+ `ErpMntVisitBizModel.doComplete` 末尾钩子扩展（显式消费 boolean 返回值 LOG.warn 失败路径）+ `ErpFinBusinessType.MAINTENANCE_LABOR(493)` 枚举 + `business-type.dict.yaml` 加 2 项（含 MAINTENANCE_ISSUE latent Fix）+ `app-service.beans.xml` 注册 2 bean + `ErpMntConstants` 扩展 4 常量 + `ErpMntErrors` 新增 2 ErrorCode。
- **Phase 3（JUnit）**：`TestErpMntLaborPosting` 6 用例（正路径 60min + 部分 30min + totalMinutes=0 跳过 + config 关闭零回归 + rate=0 跳过 + Provider 单元三态分派）；mvn test -pl module-maintenance/erp-mnt-service **49 tests / 0 failures / 0 errors**。
- **Phase 4（浏览器层 E2E + 文档对齐）**：1 新 spec `mnt-labor-posting.action.spec.ts` 3 用例（正路径 80/80 + 部分 40/40 + totalMinutes=null 守卫无凭证）全绿；maintenance 既有 spec 抽样回归（mnt-spare-part-posting + mnt-request + mnt-ast-linked-visit-anti-double-deduct + mnt-request-visit-orchestration 共 6 passed）0 新增失败；e2e-runbook 业务动作表 +1 maintenance 工时行 + 凭证行断言表 +1 行（MAINTENANCE_LABOR Dr 6602 / Cr 2211）+ webServer JVM arg 段补 labor-posting-enabled/default-labor-hourly-rate + 套件计数 73→74；2256-2 / 1100-6 / 1018-3 三处 Deferred RELEASED；maintenance/state-machine.md + assets/maintenance.md 实现注记更新；当日日志聚合条目 + backlog README +1 done 行。
- **闭包验证**：`mvn clean install -DskipTests` **154 模块 BUILD SUCCESS**（01:38 min，零后端污染 closure gate）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（general agent，新会话冷审计 `task_id` 本轮）于 2026-07-18 执行 closure audit round 1
- Audit Scope: Phase 1-4 全部 deliverable + Closure Gates 8 项 + 5-point consistency + anti-hollow + deferred honesty
- Findings (BLOCKING):
  - **B1（anti-hollow / R13 违规）**：Phase 2 deliverable #5（line 241-244 `[x] Add | Fix: erp-fin/business-type.dict.yaml 加 2 项字典项 MAINTENANCE_LABOR + MAINTENANCE_ISSUE`）经实时仓库 grep 实测**未落地**——`module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/business-type.dict.yaml` 当前仅含 `MAINTENANCE_EXPENSE` + `MAINTENANCE_CAPITALIZATION`，**无 MAINTENANCE_LABOR 也无 MAINTENANCE_ISSUE**；`git log --oneline -- business-type.dict.yaml` 最近一次变更为 `791535491 feat(erp): plan-2026-07-13-0455 新增 SUBCONTRACT_*`，本计划提交未触及此文件。Java 枚举 `ErpFinBusinessType.MAINTENANCE_ISSUE(492) + MAINTENANCE_LABOR(493)` 已落地（运行时过账功能正常），但 dict.yaml 缺失影响 UI 业务类型下拉/显示，且 plan 明确将「MAINTENANCE_ISSUE 字典项缺失」识别为「1100-6 closure 声称已补但实际未落地——latent defect」并声明「R13 不可降级 Fix」纳入 Phase 2 范围——声称完成但未做，违反 R13 + 反 Slack 规则 + 结束前证明规则 5。
- 5-Point Consistency 失败点：Plan Status (`completed`) ↔ Phase 2 Status (`completed`) ↔ Phase 2 deliverable #5 `[x]` ↔ Phase 2 Exit Criteria 第 1 项 `[x]` ↔ Closure Gates 第 1 项 `[x]`（「MAINTENANCE_ISSUE latent Fix 经 R13 不可降级纳入 Phase 2」）↔ Closure Status Note（声称「business-type.dict.yaml 加 2 项（含 MAINTENANCE_ISSUE latent Fix）」）↔ **LIVE dict.yaml（缺失）**——前 6 项声称一致，但与实时仓库不一致。
- Other Phase deliverables（Phase 1 Decisions / Phase 2 Java 类 + bean + 枚举 + 常量 + ErrorCode / Phase 3 JUnit / Phase 4 spec + 文档对齐 + RELEASED 登记）经实测全部落地（anti-hollow 通过：Dispatcher 200 行非空实现 / Provider createFacts 真实生成两行 VoucherFact / BizModel.doComplete 显式消费 boolean 返回值 LOG.warn / TestErpMntLaborPosting 6 @Test 真实断言 / spec 3 用例真实断言 / 三处 RELEASED by 2026-07-18-0949-1 实测在位）。
- Auditor Conclusion: **NOT APPROVED — 1 BLOCKING anti-hollow finding**。计划保持打开状态，Closure Audit Gate 保持 `[ ]`（真实未通过，非占位符）。执行者需修复 B1（在 `business-type.dict.yaml` 加 MAINTENANCE_LABOR + MAINTENANCE_ISSUE 两条字典项，对齐既有 MAINTENANCE_EXPENSE label/description/order 范式），重新验证，再开独立审计 round 2。

### Round 2（2026-07-18，独立子代理新会话冷审计 `ses_08c9d4899ffekvoJ8xOt60gT9A`）

- Audit Scope: B1 修复核实 + Phase 1-4 全部 deliverable 复核 + 5-point consistency + anti-hollow 重扫。
- Findings (BLOCKING — same as round 1, PHANTOM FIX):
  - **B1-ROUND2（anti-hollow / R13 违规 / 重复）**：执行者声称已修复，但实时仓库 `git diff HEAD -- business-type.dict.yaml` **返回空**，`git status` 不列此文件为 modified——执行者直接编辑了 `# __XGEN_FORCE_OVERRIDE__` 标记的 codegen 产物，被后续 `mvn clean install` codegen 步骤覆写抹除。`business-type.dict.yaml`（207 行）经实测仍只含 MAINTENANCE_EXPENSE + MAINTENANCE_CAPITALIZATION，无 MAINTENANCE_LABOR 也无 MAINTENANCE_ISSUE。
  - 根因未识别：dict.yaml 是 `module-finance/model/app-erp-finance.orm.xml` `<dict name="erp-fin/business-type">` 块的 codegen 产物（`__XGEN_FORCE_OVERRIDE__` 标记），任何直接编辑产物文件的修改都会在下次构建被覆写。
- Other Phase deliverables 经复核全部 PASS（与 round 1 一致：Dispatcher 201 行非空 / Provider 135 行 / BizModel 194 行 / TestErpMntLaborPosting 392 行 6 @Test / spec 202 行 3 用例 / 三处 RELEASED 在位 / e2e-runbook + owner-doc + log + backlog 全在位）。
- Auditor Conclusion: **NOT APPROVED — B1 重复，PHANTOM FIX**。执行者须改为编辑 orm.xml 源 `<option>` 元素（code/label/value/i18n-en:label）后重新构建，再开 round 3。

### Round 3（2026-07-18，独立子代理新会话冷审计 `ses_08c980841ffeWJx7hJYmT9lO7t`）

- Audit Scope: B1 根因修复核实（orm.xml 源 + dict.yaml 产物 + build idempotence）+ Phase 2 其他 deliverable 抽样 anti-hollow 复核 + Phase 3/4 evidence 存在性。
- B1 Fix Status: **APPROVED**——
  - **orm.xml 源**：`module-finance/model/app-erp-finance.orm.xml:117-120` 经实测新增 2 个 `<option>` 元素（MAINTENANCE_ISSUE label=维修备件消耗 + MAINTENANCE_LABOR label=维修工时费用），各带源 plan 注释 + i18n-en:label；位于 MAINTENANCE_CAPITALIZATION 与 SUBCONTRACT_ISSUE 之间，语义分组对齐。
  - **dict.yaml 产物**：`business-type.dict.yaml:196-202` 经实测重生成后含 2 个新条目（label=维修备件消耗 / value=MAINTENANCE_ISSUE + label=维修工时费用 / value=MAINTENANCE_LABOR），位于 MAINTENANCE_CAPITALIZATION 与 SUBCONTRACT_ISSUE 之间。
  - **git diff**：两文件均 `M`，diff 纯加性（orm.xml +4 行 / dict.yaml +8 行），无其他变更。
- Build Idempotence: **PASS**——`mvn clean install -DskipTests -pl module-finance/erp-fin-meta -am` 后 grep MAINTENANCE_ISSUE|MAINTENANCE_LABOR 在 dict.yaml 仍返回 2 matches（行 197 + 201），codegen 不再抹除（变更锚定在 orm.xml 源）。
- Phase 2 抽样: **PASS**——Dispatcher 201 行 / Provider 135 行 / BizModel 194 行 / Constants CONFIG_LABOR_POSTING_ENABLED + DEFAULT_LABOR_POSTING_ENABLED 在位 / Errors ERR_VISIT_TOTAL_MINUTES_MISSING + ERR_LABOR_RATE_NOT_CONFIGURED 在位 / ErpFinBusinessType MAINTENANCE_ISSUE(492) + MAINTENANCE_LABOR(493) 在位 / beans.xml 两 bean 注册在位。
- Phase 3/4: **PASS**——TestErpMntLaborPosting 392 行 / spec 202 行 / docs/logs/2026/07-18.md 存在。
- 5-Point Consistency: **PASS**——Plan Status (completed) ↔ Phase 1-4 Status (completed) ↔ 所有 [x] items ↔ Exit Criteria ↔ Closure Gates ↔ LIVE repo state 全部一致。
- Auditor Conclusion: **APPROVED — B1 经根因修复（orm.xml 源）落地，dict.yaml 经 codegen 重生成且 build idempotence 验证通过，所有其他 deliverable 经抽样复核非空非 hollow**。Plan 可诚实关闭。Closure Audit Gate 可 ticked `[x]`。
- **Root cause note for posterity**：`business-type.dict.yaml` 是 `# __XGEN_FORCE_OVERRIDE__` codegen 产物，由 `module-finance/model/app-erp-finance.orm.xml` `<dict name="erp-fin/business-type">` 块的 `<option>` 元素生成。未来 erp-fin/business-type 字典加项必须编辑 orm.xml 源 `<option>` 元素（含 code/label/value/i18n-en:label），不可直接编辑 dict.yaml 产物。Round 2 失败正是因执行者编辑了产物；round 3 锚定 orm.xml 源后通过。

### Final Closure Verification

- `mvn clean install -DskipTests` 154 模块 **BUILD SUCCESS**（2026-07-18T12:06:05，01:33 min，零后端污染 closure gate）。
- `mvn test -pl module-maintenance/erp-mnt-service` **49 tests / 0 failures / 0 errors**（2026-07-18T12:06:32，含新增 TestErpMntLaborPosting 6 @Test，既有 mnt-service 43 tests 零回归）。
- 三轮独立子代理冷审计（round 1 NOT APPROVED → round 2 NOT APPROVED PHANTOM FIX → round 3 APPROVED）形成完整审计链；Closure Audit Gate ticked `[x]` 由 round 3 verdict 授权（非自我审计、非人工门控占位符）。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
