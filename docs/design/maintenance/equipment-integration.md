# 设备维护域 - 设备与业务集成

## 目的

详细说明设备维护域与资产域、库存域、制造域的协作机制，包括备件消耗、设备状态联动、停机影响排产。

---

## 一、设备与资产的关联

### 1.1 设备与资产卡片映射

```
资产卡片（assets 域）←→ 设备记录（maintenance 域）
        │                       │
        ├─► asset_id（唯一）     ├─► asset_id（外键引用）
        ├─► 原值、折旧          ├─► 设备状态、维护计划
        ├─► 资产类别            ├─► 维护团队
        └─► 存放位置            └─► 运行时间、OEE
```

### 1.2 关联规则

| 规则 | 说明 |
|------|------|
| 一对一 | 一个资产卡片对应一个设备记录（需维护的资产） |
| 可选关联 | 不是所有资产都需要维护记录（如办公用品） |
| 双向查询 | 设备可查询资产卡片；资产可查询设备记录 |

### 1.3 资产状态联动

```
资产状态变更（assets 域）
        │
        └─► 发布资产状态事件
                    │
                    ▼
            维护域消费事件
                    │
                    ├─► 资产处置（SCRAPPED/SOLD）→ 设备状态改为已停用
                    │
                    └─► 资产恢复（IN_SERVICE）→ 设备状态改为运行中
```

> **§1.3 实现注记（RC-R1.77 / UC-MAIN-08 / P1-RC-070）**：全仓无事件总线基建，联动落位为 **assets 处置 Processor 直接调 mnt Facade**（D1 裁决：否决 mnt 轮询资产状态[延迟 + 全表扫描]、否决事件总线[全仓无此基建，引入属架构变更另行立项]）——`ErpAstDisposalProcessor.executeApprove` 后置调 `IErpMntEquipmentBiz.changeStatusForAssetDisposal(assetId, disposalCode)`：按 `assetId` 反查关联设备（无关联设备 no-op 返回 0，§1.2 可选关联合法；已 DECOMMISSIONED 幂等跳过）置 **DECOMMISSIONED**，经 `EquipmentStatusLinker.linkToDecommissionedByDisposal` 同链写状态日志行（来源 `DISPOSAL` + sourceBillCode=处置单编码；dict `erp-mnt/status-log-source` 值集仍为 VISIT/DOWNTIME/MANUAL，DISPOSAL 选项加性追加归 successor[ORM 变更]，Java 写路径不做 dict 校验）。**失败语义 = 异常传播回滚处置事务**（同 JVM 同事务强一致——设备停用是 L1 硬断言，处置成功但设备未停用 = 契约破坏）；config `erp-mnt.disposal-link-enabled`（默认 true）关闭时跳过。**对称恢复**：`executeReverseApprove` 仅 posted==TRUE 分支（与资产恢复 IN_SERVICE 同分支，防「设备 RUNNING / 资产 SCRAPPED」分叉）调 `restoreFromAssetDisposal` 恢复 RUNNING（§1.3 字面语义；非 DECOMMISSIONED 幂等跳过）。
>
> **引用守卫（L1「设备不可再被新维护计划/工单引用」）**：Schedule/Request/Visit 三 BizModel `defaultPrepareSave`（新增行）+ `defaultPrepareUpdate`（仅 equipmentId 变更时）经 `DecommissionedEquipmentGuard` 校验设备 status≠DECOMMISSIONED → `ERR_MNT_EQUIPMENT_DECOMMISSIONED` 拒绝；Visit 排程迁移（VisitScheduleProcessor）同守卫。**内部 save 双路径裁决**：①批量生成路径（到期访问日批 job）——`ScheduleDueGenerator.findDueSchedules` 查询侧排除 DECOMMISSIONED 设备计划 + LOG.warn 跳过（日批循环无 per-schedule try/catch，一条违规计划中断整批 = 回归，禁止）；②手工触发路径（request accept 生成 RESPONSIVE visit）——`ErpMntRequestAcceptProcessor` 显式拒绝抛同错误码（对已处置设备开新维护工作正是 L1 禁止语义，事务回滚）；③runtime 触发扫描同口径排除。测试证据：`TestErpMntAssetDisposalLinkage`（联动六态）+ `TestErpMntEquipmentReferenceGuard`（守卫六态）+ `TestErpAstDisposalEquipmentLinkage`（ast 侧 mock Facade 三态）。

---

## 二、备件消耗流程

### 2.1 备件消耗场景

| 场景 | 触发时机 | 消耗方式 |
|------|----------|----------|
| 预防性维护 | 维护访问执行中 | 按维护任务标准消耗 |
| 故障维修 | 维护访问执行中 | 按需消耗 |
| 计划性更换 | 维护计划执行 | 按计划消耗 |

### 2.2 备件消耗流程

```
维护访问执行
        │
        ├─► 记录消耗备件（物料编码、数量）
        │
        ├─► 调用 IErpInvStockMoveBiz.generateConsumptionMove()
        │           │
        │           └─► 生成出库移动单（CONSUMPTION 类型）
        │           │
        │           └─► 更新库存余额
        │
        ├─► 记录维护访问的备件消耗明细
        │
        └─► 发布备件消耗事件（触发成本归集）
```

### 2.3 备件消耗凭证

```
备件消耗出库
        │
        └─► 发布过账事件
                    │
                    ▼
            财务域生成凭证
                    │
                    ├─► 借：维修费用（按设备所属部门归集）
                    │
                    └─► 贷：存货（备件科目）
```

### 备件消耗成本归属规则

备件消耗成本按以下优先级归属：

| 归属维度 | 优先级 | 说明 |
|----------|--------|------|
| 设备所属部门 | 最高 | 维修费用归入设备使用部门的制造费用或管理费用 |
| 项目（如有） | 中 | 维护工单关联项目时，成本归入项目成本 |
| 成本中心 | 低 | 按设备归属的成本中心归集 |

> 成本归属不影响库存出库凭证，仅影响借方费用科目的辅助核算维度。

---

## 三、设备状态管理

### 3.1 设备状态定义

| 状态 | 业务含义 | 影响 |
|------|----------|------|
| RUNNING | 运行中 | 可排产、可执行维护计划 |
| IDLE | 闲置 | 不可排产、可执行维护计划 |
| MAINTENANCE | 维护中 | 不可排产、维护访问执行中 |
| DOWN | 故障停机 | 不可排产、需维修 |
| DECOMMISSIONED | 已停用 | 不可排产、不可维护 |

### 3.2 状态流转

```
运行中 (RUNNING)
  ├─► 暂停生产 → 闲置 (IDLE)
  │           └─► 恢复生产 → 运行中 (RUNNING)
  ├─► 开始维护 → 维护中 (MAINTENANCE)
  │           └─► 维护完成 → 运行中 (RUNNING)
  ├─► 故障报修 → 故障停机 (DOWN)
  │           └─► 维修完成 → 运行中 (RUNNING)
  └─► 资产处置 → 已停用 (DECOMMISSIONED)

闲置 (IDLE)
  ├─► 开始维护 → 维护中 (MAINTENANCE)
  └─► 资产处置 → 已停用 (DECOMMISSIONED)

故障停机 (DOWN)
  └─► 资产处置 → 已停用 (DECOMMISSIONED)
```

### 3.3 状态联动规则

| 维护访问状态 | 设备状态 |
|--------------|----------|
| IN_PROGRESS | MAINTENANCE |
| COMPLETED | 恢复为 RUNNING 或 IDLE（根据之前状态） |
| CANCELLED | 不变（或恢复） |

> **§3.3 双分支实现注记（RC-R1.39 / P2-RC-061）**：「恢复为 RUNNING 或 IDLE（根据之前状态）」已实现（visit 路径，`EquipmentStatusLinker`）：`restoreToRunning` 补 IDLE 分支——`linkToUnderMaintenance`（visit start）前置读取设备当前状态，**仅当前==IDLE 时**写入内部 transient 前态缓存 `priorStatusCache`（`ConcurrentHashMap<Long,String>` 包级可见，`MAX_CACHE_ENTRIES=1024` 超限清空 fail-safe）；complete/cancel 恢复时消费缓存：命中 IDLE → 恢复 IDLE（先 remove 再恢复防并发重复消费），未命中（RUNNING 来源/停机路径/缓存缺失）→ 恢复 RUNNING。停机路径（`linkToDown`）不捕获前态，恢复恒 RUNNING（§4.3「更新设备状态为 RUNNING」字面语义，行为零变化）。**残余风险（watch-only）**：缓存为 JVM 内存态——①容器重启/多实例部署缓存丢失 → 回退 RUNNING（= 现状已接受行为，非新退化）；②缓存写入非事务性——`linkToUnderMaintenance` 所在事务回滚后 IDLE 条目残留，污染该设备下一次 restore（恢复 IDLE 而非 RUNNING），方向保守且条目在下一次 linkTo* 覆盖或 restore 消费时清除；③异常路径悬挂条目由下一次 restore 消费或 linkTo* 覆盖清除；④并发同设备双维护由既有 @Version 乐观锁兜底。**Successor 备选**：持久化前态快照列 `ErpMntEquipment.preMaintenanceStatus`（2026-08-08 §7 A4 人工裁决已排除当前实施，需求立项后按 ask-first 流程评估）。测试证据：`TestErpMntVisitRequestStateMachine#testVisitCompleteFromIdleEquipmentRestoresIdle`（IDLE 输入 complete 恢复 IDLE）/`#testVisitCancelFromIdleEquipmentRestoresIdle`（cancel 同语义）/`#testVisitCompleteFromDownEquipmentRestoresRunning`（DOWN 非缓存态回退 RUNNING）/`#testRestoreWithoutPriorLinkFallsBackToRunning`（缓存缺失回退）/`#testVisitHappyPathWithEquipmentLink`（RUNNING 回归）。

> **§3.3 StatusLog 衔接注记（RC-R1.73 / UC-MAIN-02）**：上述五写点（visit start/complete/cancel + downtime record/complete）与手动 `changeStatus` 现于同一事务经 `EquipmentStatusLogWriter` 追加 `ErpMntEquipmentStatusLog` 日志行（fromStatus/toStatus/changeAt/source VISIT/DOWNTIME/MANUAL + sourceBillCode），供 §5.2 运行时长 Σ RUNNING 段聚合消费——状态迁移行为本身零变化，仅增审计轨迹。

---

## 四、停机记录与排产影响

### 4.1 停机记录内容

停机记录包含：设备编码、关联工作中心（生产设备）、停机开始/结束时间、停机原因、影响范围。

### 4.2 停机影响排产

```
设备故障停机
        │
        ├─► 创建停机记录
        │
        ├─► 更新设备状态为 DOWN
        │
        ├─► 发布设备停机事件
        │
        └─► 制造域消费事件
                    │
                    ├─► 检查工作中心关联的生产工单
                    │
                    ├─► 暂停受影响工单的排产
                    │
                    └─► 通知计划员调整生产计划
```

> **§4.2 实现注记（RC-R1.76 / UC-MAIN-06 / P1-RC-068）**：**联动模型 = mfg 拉取消费（查询时判定）+ mnt notify 事件（计划员通知）**（D3 裁决：否决「mnt push 写 mfg 工单标记」——mfg 工单无「排产暂停」持久状态列，push 无处落且触 ORM 越界；否决「mnt 写 aps MAINTENANCE constraint」——mnt→aps S 写超出依赖矩阵允许清单）。「发布设备停机事件 → 制造域消费」落位为：mnt 侧 `IErpMntDowntimeEntryBiz.findOpenDowntimeEquipmentWorkcenters()` 只读查询暴露**开放停机窗口**（开放 = endTime null 且设备 status=DOWN，经 `equipment.workcenterId` 桥接出「工作中心→窗口」，未映射工作中心的设备不出窗）；mfg 侧 `ErpMfgScheduleToJobCardProcessor` 生成 job card 前拉取一次 → 拟建卡工序任一落在开放停机工作中心 → **该工单本轮跳过建卡 + LOG.warn 保持 pending**（=「暂停该设备的工单排产」，工单级暂停；恢复即时性 = 下次排产执行时点，对齐 §4.3「重新计算生产计划」）。「通知计划员」落位为 notify 模板种子 **7208** `mnt.equipment-downtime`（ROLE 生产计划员，context = 设备/工作中心/原因/起始时间；`erp-mnt.downtime-notify-enabled` 默认 true 门控，try/catch 静默降级不阻断停机主流程；无匹配角色数据 config-gated 空投递）。**successor**：①CRP 负荷停机扣减（L2 §4.2 未断言容量口径，排产门控已达成主语义）；②mnt 自动生成 aps `ErpApsConstraint`(MAINTENANCE) 行（mnt→aps S 写须矩阵修订，RC-R1.86-88 aps 域修复时协同裁决）。测试证据：`TestErpMntDowntimeSchedulingLinkage`（发布侧四态）+ `TestErpMfgJobCardDowntimeGate`（消费侧三态）。

### 4.3 停机恢复

```
设备维修完成
        │
        ├─► 更新停机记录结束时间
        │
        ├─► 更新设备状态为 RUNNING
        │
        ├─► 发布设备恢复事件
        │
        └─► 制造域消费事件
                    │
                    ├─► 恢复受影响工单的排产
                    │
                    └─► 重新计算生产计划
```

> **§4.3 实现注记（RC-R1.76）**：停机 `complete`（endTime + totalMinutes + 设备恢复 RUNNING）后**窗口自然关闭**（endTime 非空即退出开放集）→ mfg 下次排产执行时点拉取开放集为空 → 受影响工单自然恢复建卡（**拉取模型恢复语义免 push**）；同时后置 notify 模板种子 **7209** `mnt.equipment-recovered`（ROLE 生产计划员，context 含 endTime；门控与降级语义同 7208）提示计划员「重新计算生产计划」。恢复即时性 = 下次排产执行时点（日批 job / 手动重触发），非事件驱动即时推送。

---

## 五、维护计划执行

### 5.1 维护计划类型

| 类型 | 周期规则 | 示例 |
|------|----------|------|
| 时间周期 | 按固定时间间隔 | 每月、每季度、每年 |
| 运行时长 | 按设备运行时长 | 每运行 500 小时 |
| 产量周期 | 按产量 | 每生产 1000 件 |

> **实现注记（RC-R1.73 / UC-MAIN-02）**：触发类型落位为 `ErpMntSchedule.triggerType` 列（dict `erp-mnt/trigger-type`，值集 TIME/RUNTIME；**null=TIME 派生兼容存量计划**）。时间周期 = 既有 nextDueDate 链（recurrenceType DAILY/WEEKLY/MONTHLY/YEARLY + frequency）；运行时长 = `thresholdHours` 阈值列 + `runtimeBaselineHours` 基线列（生成 DRAFT 时同事务置 baseline=当前累计，触发条件 = 累计 ≥ baseline + 阈值；thresholdHours 缺失/非正不触发）。**产量周期（OUTPUT）不入 dict 值集**——产量采集无数据源实体，dangling 字典值违反 dict 契约；触发条件 = 产量采集数据落地或 OEE（RC-R1.78）性能效率分量数据源就绪时加性追加（successor 登记 plan 2026-08-19-0445-2 Deferred）。

### 5.2 计划生成流程

```
定时任务（nop-job）执行
        │
        ├─► 查询所有维护计划
        │
        ├─► 计算下次执行日期
        │           │
        │           └─► 时间周期：上次执行 + 周期间隔
        │           └─► 运行时长：累计运行时长 >= 阈值
        │           └─► 产量周期：累计产量 >= 阈值
        │
        ├─► 生成维护访问（DRAFT 状态）
        │
        └─► 产生 TODO 提醒维护主管排程
```

> **实现注记（RC-R1.73 / UC-MAIN-02）**：**运行时长来源 = 设备状态记录**（`ErpMntEquipmentStatusLog` 实体，状态变更历史——写点 = `EquipmentStatusLinker` 三迁移方法 + `ErpMntEquipmentBizModel.changeStatus` 同事务追加，来源 dict `erp-mnt/status-log-source` 值集 VISIT/DOWNTIME/MANUAL；RC-R1.77 处置链路追加第六写点 `linkToDecommissionedByDisposal`/`restoreFromDisposal` 写 `DISPOSAL` 来源日志行——dict 选项加性追加归 successor[ORM 变更]，Java 写路径不做 dict 校验，UI 展示回落原值码）。累计运行时长 = `EquipmentRuntimeCalculator` 查询时聚合（**Σ RUNNING 段**，无采集 Job 无物化漂移，状态记录为唯一真相幂等可重算）；遗留无日志设备保守基线——当前 RUNNING 从 createTime 起算，当前非 RUNNING 记 0 直至首条日志行（防虚计触发）。两类计划同一入口评估：TIME 走既有 `nextDueDate ≤ asOfDate` 扫描 + 推进，RUNTIME 分支 `findRuntimeDueSchedules` 在同一 `generateDueVisits` 入口评估（**触发粒度 = job cron 部署节奏**，对齐本节单一定时任务模型；RUNTIME 计划 nextDueDate 不推进保持 null/原值；既有 VST-SCH-{schedId}-{asOfDate} code 幂等锚点双保险保留）。StatusLog 亦为 OEE 可用率分子预留数据源（RC-R1.78 衔接）。产量周期评估分支为 successor（同 §5.1）。测试证据：`TestErpMntRuntimeTrigger`（Σ RUNNING 段聚合数学 / 遗留基线双分支 / 阈值触发 + baseline 重置 / 同日重跑幂等 / TIME 零回归 / linker·manual 写日志行）。

### 5.3 维护任务模板

维护任务模板包含：任务名称、适用设备类型、标准工时、标准备件清单、操作说明。

> **实现注记（RC-R1.74 / UC-MAIN-01）**：模板落位为 `ErpMntTaskTemplate`（code UK + name + equipmentCategoryId 可空[适用设备类型] + standardMinutes[标准工时] + instruction[操作说明] + isActive 空=非启用）+ `ErpMntTaskTemplateLine`（lineNo + taskName + 行级 standardMinutes + materialId/quantity 可空[标准备件提示]）两实体，标准 CRUD 生成。**套用语义**：计划性访问生成（`ScheduleDueGenerator.generateVisit`，TIME/RUNTIME 两路径共享）解析模板——显式 `schedule.templateId` 优先（显式锚定不校验 isActive），空则按 equipment.categoryId 匹配**唯一 active** 模板回退，零/多匹配 LOG.warn 跳过不阻断访问生成 → 模板行逐行复制为 `ErpMntVisitTask`（taskDescription=taskName、standardMinutes 透传[行级缺失回落模板级]、status=PENDING）；visit.totalMinutes 不预填（执行时长语义）。RESPONSIVE 访问（报修受理生成）不套用模板（模板属计划性维护语义）。**标准备件为提示字段不自动产生消耗单据**——实际消耗走既有 confirm 链（SparePartUsage 审批/过账语义保持）；备件自动预填 DRAFT 消耗单为 successor（触发条件：计划性备件预占需求立项）。测试证据：`TestErpMntTaskTemplate`（显式套用 / categoryId 回退 / 零·多匹配跳过 / 备件零消耗单 / CRUD 冒烟 / 无模板零回归）。

---

## 六、设备效率指标（OEE）

### 6.1 OEE 计算公式

```
OEE = 可用率 × 性能效率 × 质量合格率

可用率 = 实际运行时间 / 计划运行时间
性能效率 = 实际产量 / 理论产量
质量合格率 = 合格产品数 / 总产量
```

### 6.2 OEE 数据来源

| 指标 | 来源 |
|------|------|
| 实际运行时间 | 设备状态记录 |
| 计划运行时间 | 生产计划 |
| 实际产量 | 工单报工记录 |
| 理论产量 | 标准产能 × 运行时间 |
| 合格产品数 | 质检记录 |

### 6.3 OEE 报表

| 报表 | 内容 | 周期 |
|------|------|------|
| 设备效率日报 | 各设备当日 OEE | 每日 |
| 设备效率月报 | OEE 趋势、排名 | 月度 |
| 停机原因分析 | 按原因分类的停机时间 | 月度 |

---

## 七、跨域协作协议

### 7.1 与资产域协作

```
维护域 → 资产域：
        │
        ├─► 查询资产卡片信息（原值、折旧、类别）
        └─► 资产处置通知

资产域 → 维护域：
        │
        └─► 资产状态变更事件（触发设备状态联动）
```

> **§7.1 实现注记（RC-R1.77）**：「资产状态变更事件」落位为 **assets 处置 Processor 直接调 mnt Facade**（全仓零事件总线，直接 Facade + notify 是既有跨域范式；「事件」为 L1 概念措辞，机制实现见 §1.3 注记）：`IErpMntEquipmentBiz.changeStatusForAssetDisposal/restoreFromAssetDisposal`（Java 边 assets-service→mnt-dao 登记 `docs/architecture/data-dependency-matrix.md` §2.4——**assets↔maintenance 双向域耦合显式披露**：mnt-dao 已 pom 依赖 ast-dao[ORM to-one shadow]，ast-service→mnt-dao→ast-dao 构成 Maven 菱形非环，两域互持对方 dao 接口，重构拆分须两域协同）。

### 7.2 与库存域协作

```
维护域 → 库存域：
        │
        └─► 备件消耗出库（IErpInvStockMoveBiz）

库存域 → 维护域：
        │
        └─► 库存不足预警（备件库存低于安全库存）
```

### 7.3 与制造域协作

```
维护域 → 制造域：
        │
        ├─► 设备停机事件（影响排产）
        └─► 设备恢复事件（恢复排产）

制造域 → 维护域：
        │
        └─► 工单报工记录（用于计算运行时长和产量）
```

> **§7.3 实现注记（RC-R1.76）**：「设备停机/恢复事件」落位为 **mfg 拉取消费 + notify 计划员通知**（D3 裁决，机制实现见 §4.2/§4.3 注记）：mfg `ErpMfgScheduleToJobCardProcessor` 生成 job card 前经 `IErpMntDowntimeEntryBiz.findOpenDowntimeEquipmentWorkcenters` 拉取开放停机窗口做排产门控（Java 边 mfg-service→mnt-dao 登记 `docs/architecture/data-dependency-matrix.md` §2.4——矩阵「maintenance 被 manufacturing 查」预期方向的落地，mnt 不依赖 mfg，DAG 无环；mnt 模块缺失时 mfg `@Nullable` 注入容错零门控）；计划员通知经 notify 7208/7209。「制造域 → 维护域：工单报工记录」为 RC-R1.78 OEE 数据来源 successor，本切片未接线。

### 7.4 事件内容

| 事件 | 关键字段 | 实现载体（RC-R1.76/77 注记） |
|------|----------|------------------------------|
| 设备停机事件 | 设备编码、工作中心、开始时间、停机原因 | notify 模板 **7208** `mnt.equipment-downtime` context（equipmentCode/workcenterId/startTime/reason）+ 开放停机窗口查询（mfg 排产消费） |
| 设备恢复事件 | 设备编码、工作中心、结束时间 | notify 模板 **7209** `mnt.equipment-recovered` context（equipmentCode/workcenterId/endTime）+ 窗口关闭（mfg 拉取模型自然恢复） |
| 备件消耗事件 | 维护访问编码、物料编码、消耗数量 | SparePartUsage 过账链（§二，既有实现） |

---

## 八、关键业务规则总结

1. **设备资产关联**：一对一映射，资产处置触发设备停用
2. **备件消耗闭环**：维护访问消耗备件 → 库存出库 → 成本归集
3. **状态联动**：维护访问状态驱动设备状态变更
4. **停机影响排产**：设备故障自动通知制造域调整计划
5. **OEE 计算**：多来源数据聚合计算设备综合效率
6. **计划自动生成**：定时任务按周期规则生成维护访问