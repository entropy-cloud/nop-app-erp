# ck-maintenance — maintenance 域实现代码检查报告

> 工作项：C5.3。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-maintenance/erp-mnt-service/src/main/java` 全部 56 个手写生产文件——根常量 3 类（`ErpMntConstants`/`ErpMntConfigs`/`ErpMntErrors`）+ entity BizModel 15 类（Equipment/EquipmentCategory/Schedule/Visit/VisitTask/Request/DowntimeEntry/EquipmentStatusLog/TaskTemplate(+Line)/MaintenanceTeam(+Member)/SparePartUsage(+Line)/Calibration）+ per-mutation Processor 15 类（Request 5 + Visit 5 + Downtime 2 + SparePartUsage 2 + ScheduleGenerateDueVisits，含 3 个 Abstract 基类）+ 状态机 Bean 4 类（Visit/Request/SparePartUsage Document+Approval）+ support 7 类（ScheduleDueGenerator/SparePartIssueService/EquipmentRuntimeCalculator/OeeCalculator/EquipmentStatusLinker/EquipmentStatusLogWriter/DecommissionedEquipmentGuard）+ posting 5 类（MntPostingExecutor/两 Dispatcher/两 AcctDocProvider）+ job 1 类（`ErpMntDueVisitJob`）+ dashboard 1 类 + report 1 类 + `app-service.beans.xml` 接线 + `app-erp-all/_vfs/nop/job/conf/erp-mnt-due-visit-generation.job.yaml`（D4；mnt 无 batch.xml，job yaml 直调 bean method）。跨域核实：`ErpInvStockMoveGenerateMoveProcessor`（幂等短路）、`ErpInvStockMoveProcessor#findExisting`（id DESC O-5 修复后语义）、`ErpInvStockMoveReverseProcessor`（reverse 不改原单 docStatus、REVERSAL 独立 relatedBillType）、`ErpFinVoucherBizModel`（post/reverse `@Transactional(REQUIRES_NEW)` 实证）、`module-maintenance/model/app-erp-maintenance.orm.xml`（versionProp/orgId/businessDate/mandatory/UK/dict 逐实体核对）、mnt-meta 全部 12 个 dict.yaml 值集（D3 可达性）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。56 文件全量逐行深读 + 跨域源码实证 + orm/dict 核对 + arm-index 复用裁决。
> D6 说明：设计与代码均无 MTBF/MTTR 计算（`docs/design/maintenance/` 全文 grep 零命中）——该子维度 N/A；停机时长计算 = DowntimeEntry.totalMinutes + OEE downtimeHours（均已查）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-mnt-001（D3/D5）`validateNotConfirmed` 是空守卫——confirm 可作用于任意状态：CANCELLED 终态可复活为 ACTIVE+posted=true，单据状态对已回滚的库存/GL 撒谎

- **控制点**：`app/erp/mnt/service/processor/AbstractErpMntSparePartUsageProcessor.java#validateNotConfirmed`（L67-75：

  ```java
  protected void validateNotConfirmed(ErpMntSparePartUsage usage, IServiceContext context) {
      if (Boolean.TRUE.equals(usage.getPosted())) {
          return;                      // 两个分支 + fallthrough 全部 return，无任何 throw
      }
      String docStatus = usage.getDocStatus();
      if (docStatus != null && Objects.equals(docStatus, ErpMntDaoConstants.DOC_STATUS_ACTIVE)) {
          return;
      }
  }
  ```

  ）对照 `ErpMntSparePartUsageDocumentStateMachine#assertCanConfirm`（L36-40：仅 DRAFT 合法，**完整矩阵已存在但 confirm Processor 从未调用它**）+ 调用方 `ErpMntSparePartUsageConfirmProcessor#confirm`（L22-23：`validateNotConfirmed` 是唯一状态守卫）
- **证据**：状态机 Bean javadoc L26-27 自认：「confirm 的来源态运行时守卫因 `validateNotConfirmed` silent-guard gap 暂不强制（Deferred）」；该 deferral 见 plan `2026-08-14-0930-3-maintenance-m4-state-machine-beans.md`（L157-160「方法体无 throw on not confirmed branch…修复须 ask-first」），**arm-index 零登记**（grep「validateNotConfirmed/silent-guard」零命中）。复活链路逐跳实证：①`reverseConfirm` 后 usage = CANCELLED + posted=false（库存已反向、GL 已红冲）；②对它再调 `confirm`——空守卫放行 → `SparePartIssueService#issue` → inventory `ErpInvStockMoveGenerateMoveProcessor#generateMove` L29-35 幂等短路：`findExisting(ERP_MNT_SPARE_PART, usage.code)` 命中原移动单（`ErpInvStockMoveReverseProcessor` 实证：reverse 建独立 REVERSAL 单、原单 docStatus 保持 DONE）**直接返回、不再出库**；③`applyIssueResult` 置 docStatus=ACTIVE + approveStatus=APPROVED + posted=`isStockIssued(原单 DONE)`=true；④`dispatchIfApplicable` 的 `voucherAlreadyExists(usage.code+"-MI")` 命中残留 billR 行跳过（GL 保持净零）。终态：usage 声称「已出库已过账」而库存已回滚、GL 已红冲。再次 `reverseConfirm` 重新合法（posted=true 通过守卫）→ `reverseIssue` 对已红冲凭证二次红冲的幂等性未证（依赖 `IErpFinVoucherBiz.reverse` 内置守护，见剩余风险③）。对 ACTIVE 单的重复 confirm 为良性幂等（同链路短路），CANCELLED 复活是数据完整性缺陷。
- **问题**：终态可复活 + 单据状态成为库存/GL 状态的谎言；备件消耗闭环（UC-MAIN-04）的核心不变量「CANCELLED 终态不可逆」被旁路。plan 的 deferral 理由（行为保持优先）只考虑了「重复 confirm 幂等」，未覆盖「红冲后复活」这一组合。
- **建议修复方向**：`validateNotConfirmed` 接线状态机 Bean——`documentStateMachine.assertCanConfirm(usage.getDocStatus())`（+ `approvalStateMachine.assertCanConfirmApprove`）映射为领域错误码（cause-chain 范式已在同文件 `validateCanReverse` L90-95 落地，照搬即可）；修复触及 plan 2026-08-14-0930-3 的 ask-first deferral，修复阶段按保护区域规则处理。
- **arm-index 裁决**：新增（plan 层 Deferred 有记录但 arm-index 无 finding；本条登记其未覆盖的红冲复活后果）。

### P1-CK-mnt-002（D2/D7，同型 P1-CK-mfg-005 族）reverseConfirm 红冲 GL/反向库存两步失败吞异常后仍推进 CANCELLED+posted=false 终态——重试入口被守卫永久封死，悬挂不可恢复且无告警

- **控制点**：`app/erp/mnt/service/processor/ErpMntSparePartUsageReverseConfirmProcessor.java#reverseConfirm`（L29-39 红冲凭证 `catch (Exception e) { LOG.warn/error }`、L44-56 反向移动单同型吞咽——两步失败均继续 L58-62 `doReverseConfirm` 翻 `docStatus=CANCELLED + posted=false`）+ `AbstractErpMntSparePartUsageProcessor#validateCanReverse`（L85-89：`!posted` 抛 `ERR_SPARE_PART_USAGE_NOT_POSTED`——posted=false 后红冲入口永久关闭）
- **证据**：时序：confirm 成功（GL + 出库 + posted=true）→ reverseConfirm → GL 红冲失败（如期间锁定）→ 吞 → 库存反向成功 → 翻 CANCELLED。结果：GL 借维修费用凭证滞留而库存已回滚（或反向组合：GL 净零而库存永不回滚）。失败通道仅 LOG.warn/error——**正向过账失败有 G3 告警**（`MaintenanceIssuePostingDispatcher#dispatchFailureAlert` L133-150 → `IErpSysNotificationBiz`），**红冲方向零告警、零重试通道**（maintenance 不在期末前置检查矩阵，state-machine.md L186 自述「经期末试算平衡人工发现」）。与 `TestErpMntSparePartUsageReversal` 的语义不变是「吞异常范式」被逐字搬运（类 javadoc L16-17 自证），非有意闭环。
- **问题**：lesson 09（业财过账吞异常悬挂）红冲方向变体，mnt 站点。与 mfg-005 同根因（吞咽 + 终态推进 + 守卫封口三件套）。
- **建议修复方向**：任一步失败中止红冲（抛 NopException 回滚 `@BizMutation`，状态保持 ACTIVE+posted=true 可重试）；或失败仍推进但派发 G3 告警（复用 `NOTIFY_EVENT_MAINTENANCE_ISSUE_FAILURE` 或新增 reverse 变体）。与 mfg-005 联合裁决。
- **arm-index 裁决**：同型登记（P1-CK-mfg-005 族，mnt 站点新增计数）。

### P1-CK-mnt-003（D3/D8）`restoreToRunning` 无条件恢复覆盖设备当前状态——取消未启动 visit 强改设备 RUNNING、visit/downtime 交叉时互相抹掉对方临时态，污染运行时长聚合与排产门控

- **控制点**：`app/erp/mnt/service/support/EquipmentStatusLinker.java#restoreToRunning`（L120-129：不校验设备当前是否处于本流程置入的临时态，直接 `changeEquipmentStatus(equipmentId, RUNNING|IDLE, ...)`）+ `ErpMntVisitCancelProcessor#cancel`（L30-31：`assertCanCancel` 允许 DRAFT/SCHEDULED/IN_PROGRESS 三源，**一律** `restoreToRunning`）+ `ErpMntDowntimeEntryCompleteProcessor#complete`（L25-26）+ `ErpMntVisitCompleteProcessor#complete`（L45）
- **证据**：三条失真路径——①**取消从未启动的 visit**（DRAFT/SCHEDULED 从未调 `linkToUnderMaintenance`）：设备本处于 DOWN（停机开放中）或 IDLE，cancel 一个不相关 DRAFT visit 强制翻 RUNNING——owner doc `equipment-integration.md §3.3` 表格明示 CANCELLED →「**不变**（或恢复）」，「不变」分支从未实现；测试面 `TestErpMntVisitRequestStateMachine` 的 cancel 用例全部经 start 后 IN_PROGRESS 取消（L125-136），未启动取消零覆盖；②**downtime complete 覆盖 visit**：visit IN_PROGRESS（设备 UNDER_MAINTENANCE）期间停机 record→DOWN→complete→RUNNING，visit 仍在执行但设备已「运行中」；③**visit complete/cancel 覆盖 downtime**：停机开放（DOWN）期间 visit complete→RUNNING——此路径有显式测试背书（`testVisitCompleteFromDownEquipmentRestoresRunning`），属已裁决字面语义，但下游失真未被裁决。下游消费链：`ErpMntEquipmentStatusLog` 是 `EquipmentRuntimeCalculator` Σ RUNNING 段的唯一真相（错误 RUNNING 段 → RUNTIME 计划触发与 OEE 可用率分子虚高）；`findOpenDowntimeEquipmentWorkcenters`（`ErpMntDowntimeEntryBizModel` L57-78）按 `equipment.status == DOWN` 过滤开放窗口——路径③翻 RUNNING 后开放停机窗口不再暴露，**mfg 建卡排产门控（equipment-integration.md §4.2）失效**，生产继续排在停机设备上。
- **问题**：设备状态轴是跨流程共享资源，恢复操作不判定「当前状态是否本流程所置」，交叉场景产生谎言状态并级联到运行时长/OEE/排产门控三个消费面。owner doc §3.3「不变（或恢复）」二选一在实现层退化为恒「恢复」。
- **建议修复方向**：`restoreToRunning` 增加「当前 ∈ {UNDER_MAINTENANCE, DOWN} 才恢复，否则 no-op（不变分支）」守卫；downtime/visit 交叉语义（③）若维持字面「恢复 RUNNING」需 owner doc 显式裁决并接受排产门控窗口失效的残留（登记触发条件）。修复时与 priorStatusCache 残余风险（plan 2026-08-15-1605-2 watch-only，不重复登记）协同。
- **arm-index 裁决**：新增（grep「restoreToRunning/设备状态 恢复/不变」arm-index 零命中；P2-RC-061 只裁决 IDLE 双分支，未覆盖未启动取消与交叉覆盖维度）。

### P2-CK-mnt-004（D6/D8）看板「本期维护访问数」按 `visit.businessDate` 过滤，但两类生成器建 visit 均不写 businessDate（xmeta 无默认值）——自动生成的 visit 完成后 KPI 恒不计入

- **控制点**：`app/erp/mnt/service/dashboard/ErpMntDashboardBizModel#countCompletedVisitsInRange`（L323-330：`eq("status", COMPLETED) + ge("businessDate", from) + le("businessDate", to)`）对照生成器 `ScheduleDueGenerator#generateVisit`（L183-195：data map 仅 code/scheduleId/equipmentId/visitDate/status/visitType，**无 businessDate**）与 `ErpMntRequestAcceptProcessor#generateResponsiveVisit`（L41-53：同样无 businessDate）+ `_ErpMntVisit.xmeta` L88-91（businessDate 无 defaultValue）+ `ErpMntVisitCompleteProcessor#doComplete`（L50-59：complete 亦不补写）
- **证据**：PLANNED/RESPONSIVE 两类访问从生成到完成全程 businessDate=null → `ge/le` 过滤排除 NULL → `getDashboardKpi.periodVisitCount` 对自动生成访问恒 0（UC-MAIN-11 看板 KPI 之一）。附带影响：`MaintenanceLaborPostingDispatcher#buildEvent` L207-209 voucherDate 回退 `CoreMetrics.today()`——补录完成的访问凭证日期漂移到完成日而非业务日。
- **问题**：看板 KPI 用户可见失真；visit 表 `businessDate` 列成为「仅手工建单才有值」的半死列。
- **建议修复方向**：生成器写 `businessDate = visitDate`（或 asOfDate）；或 KPI 改按 `visitDate`/`completedAt` 口径并同步 owner doc。修复经 `visitBiz.save` data map，纯 service 层预授权。
- **arm-index 裁决**：新增（grep「businessDate/看板 访问数」零命中）。

### P2-CK-mnt-005（D4/D2）due-visit 日批循环无 per-schedule 失败隔离 + job 失败仅 LOG.error 无告警无重试——一条 poison 计划每天中断全批且不可观测

- **控制点**：`app/erp/mnt/service/support/ScheduleDueGenerator#generateForTimeSchedules`（L94-111：for 循环无 per-schedule try/catch——L100-101 注释自证只对 DECOMMISSIONED 守卫做了查询侧排除豁免，「visitBiz.save 守卫若触发将中断整批（绿基线回归，禁止）」仅消除该单一异常源；模板行保存 `visitTaskBiz.save`、code UK 冲突、乐观锁等仍会中断）+ `ErpMntScheduleBizModel#generateDueVisits`（`@BizMutation` 单事务：任一 schedule 抛错 → **整批回滚**）+ `ErpMntDueVisitJob#execute`（L43-48：`catch (Exception e) { LOG.error }`——无 notify 告警、无失败计数、无重试）
- **证据**：poison 场景：一条计划引用已删除模板之外的坏数据（如 `visitTaskBiz.save` 触发校验错）→ 该 schedule 抛 NopException → 事务回滚 → 全部计划当日 0 生成 → job 仅 LOG.error → 次日同点再炸，**永久静默阻塞全部预防性维护生成**。同型对照：mfg `GeneratePendingJobCardsProcessor` 有 per-WO try/catch（mfg-017 反向登记其单事务缺陷但至少隔离了失败单）；notify 告警范式本域已有（posting failure alert）可复用。
- **问题**：调度链失败接力断裂（B3.1）：失败不隔离、不告警、不自愈（poison 情形）。触发条件：任一 schedule 数据劣化。
- **建议修复方向**：per-schedule try/catch（warn + 失败计数继续下一单，注意与单事务语义的取舍——逐单 savepoint 或接受部分提交）+ job 失败/部分失败派发 notify（复用 7208 范式新模板）。
- **arm-index 裁决**：新增（P1-MA2-086 只登记并发幂等维度（R1.28 已修），失败隔离维度零覆盖；grep「due-visit 失败/poison」零命中）。

### P2-CK-mnt-006（D5/D3，同型 P1-CK-pur-003 族）通用 CRUD update/delete 无单据状态守卫——全 15 实体裸 CrudBizModel，CANCELLED 消耗单/COMPLETED 访问/已结束停机/状态日志行均可直接改写

- **控制点**：`app/erp/mnt/service/entity/` 下 `ErpMntSparePartUsageBizModel`/`ErpMntVisitBizModel`/`ErpMntRequestBizModel`/`ErpMntDowntimeEntryBizModel`/`ErpMntVisitTaskBizModel`/`ErpMntEquipmentStatusLogBizModel` 等 15 类——仅 Schedule/Visit/Request 三类有 `defaultPrepareSave/Update` 且只守卫 DECOMMISSIONED equipmentId 变更（`ErpMntScheduleBizModel` L32-50），无任何终态/posted 守卫
- **证据**：①CANCELLED 消耗单可经 `ErpMntSparePartUsage__update_` 直接翻 docStatus=ACTIVE（与 P1-CK-mnt-001 空守卫叠加，双通道复活）；②COMPLETED visit 可手改 `totalMinutes`（labor 凭证已按原值过账，改后无重算）与 `endTime`；③已 complete 停机记录可改 `startTime/endTime/totalMinutes`（OEE 分母与停机报表失真）；④`ErpMntEquipmentStatusLog` 行可手改/删除——它是运行时长聚合唯一真相（equipment-integration.md §5.2），直接破坏 Σ RUNNING 幂等可重算性；⑤VisitTask 状态推进完全依赖通用 update（PENDING→IN_PROGRESS/COMPLETED/SKIPPED/FAILED 五值 dict 仅 PENDING 有命名 writer，其余经 CRUD 可达且无迁移守卫）。
- **问题**：同 P1-CK-pur-003/P1-CK-sal-004/P1-CK-inv-005/P2-CK-mfg-006 全域同型——命名动作链的状态守卫可被通用 CRUD 旁路。mnt 站点按约定登记为同型 finding。StatusLog 可改写是本域特有加重项（唯一真相源可覆写）。
- **建议修复方向**：终态/已过账实体 update/delete 守卫（`defaultPrepareUpdate` 校验 docStatus/status 非终态 + posted=false）；StatusLog 建议整体禁用通用 update/delete（只读追加）。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，mnt 站点新增计数；StatusLog 加重项注明）。

### P2-CK-mnt-007（D7/D2，同型 P2-CK-inv-012/P1-CK-pur-002/P2-CK-mfg-011/P2-CK-ast-015 族）visit complete 链 REQUIRES_NEW 工时凭证先于主事务末步提交——restoreToRunning/completeLinkedRequest 失败回滚主事务留下孤儿 MAINTENANCE_LABOR 凭证

- **控制点**：`app/erp/mnt/service/processor/ErpMntVisitCompleteProcessor#complete`（L44-47：`doComplete`（内含 L64-68 `postLabor` → `MntPostingExecutor#postEvent` → `IErpFinVoucherBiz.post` `@Transactional(REQUIRES_NEW)` 独立提交——ErpFinVoucherBizModel L74 实证）之后仍有 L45 `restoreToRunning`（equipment 乐观锁 updateEntity 可抛）与 L46 `completeLinkedRequest`（request 乐观锁可抛）在主事务内）+ 幂等面 `MaintenanceLaborPostingDispatcher#voucherAlreadyExists`（L231-238 预检）
- **证据**：主事务回滚窗口：postLabor 已提交凭证 → completeLinkedRequest 乐观锁冲突 → 整个 `@BizMutation` 回滚 → visit 回 IN_PROGRESS、凭证滞留 GL。重 complete：预检命中返回 false → LOG.warn「skipped or failed」误报，但主事务成功后凭证恰好匹配（自愈）；若用户转而 cancel：`doCancel` 的 `reverseLabor`（L41-52）红冲对冲（`TestErpMntVisitCancelReversal` 覆盖）——孤儿窗口有限但存在，误 warn 日志污染运维信号。
- **问题**：同型家族（REQUIRES_NEW 之后仍有可失败步骤的编排顺序），mnt 站点登记。
- **建议修复方向**：postLabor 移到主事务末步之后（afterCommit 编排）或与家族联合裁决（对账告警兜底）。
- **arm-index 裁决**：同型登记（P2-CK-inv-012 族，mnt 站点新增计数）。

### P2-CK-mnt-008（D8，同型 P2-CK-fin2-007/P2-CK-mfg-007 族）Dashboard/报表全部查询无 orgId 过滤——多组织部署下 KPI 跨组织混算；job 生成 visit orgId 恒 null

- **控制点**：`app/erp/mnt/service/dashboard/ErpMntDashboardBizModel` 全部查询（`countEquipmentNotDecommissioned`/`countEquipmentByStatus`/`countRequestsByStatus`/`countCompletedVisitsInRange`/`loadEquipmentsNotDecommissioned`/`loadActiveSchedules`/`loadScheduleIdsWithVisit`——均无 `eq("orgId", ...)`）+ `ErpMntReportBizModel#loadVisits/loadDowntimeEntries`（L290-298/L352-358 同型）对照 orm：Equipment/Visit/SparePartUsage/Calibration/MaintenanceTeam 均有 orgId 列（L137/L285/L619/L727/L552）且 Equipment/Visit 带 `(code, orgId)` UK——多组织是 ORM 显式设计维度
- **证据**：Equipment.orgId 在位，但聚合读路径不按组织过滤；job 路径 `ErpMntDueVisitJob` 用 `new ServiceContextImpl()`（无用户上下文）→ 生成 visit orgId=null → `MaintenanceLaborPostingDispatcher#buildEvent` L199 `event.setOrgId(null)` → `resolveAcctSchemaId(null)` 落默认主账套（单账套无影响，多账套挂错）。UC-MAIN-11 看板断言与其他域同口径（mfg-007 引「看板数据受行级权限约束」）。
- **问题**：多组织隔离维度在 mnt 聚合读路径缺失。单组织部署无影响（降 P2 依据，同 mfg-007/fin2-007）。
- **建议修复方向**：dashboard/report 查询补 orgId 过滤（从 IUserContext 取）；生成器 orgId 透传（从 equipment.orgId 继承）。
- **arm-index 裁决**：新增（同族注记——orgId 隔离缺失家族新域站点）。

### P2-CK-mnt-009（D5）备件消耗 confirm 入参边界缺失——warehouseId 可空出库、行数量无正数校验（负出库=加库存）、时长无负值守卫

- **控制点**：`app/erp/mnt/service/processor/ErpMntSparePartUsageConfirmProcessor#confirm`（L21-41：加载后直接 `sparePartIssueService.issue`，无 warehouseId/数量符号守卫）+ `SparePartIssueService#issue`（L39 `request.setSourceWarehouseId(usage.getWarehouseId())`——orm L634 warehouseId **非必填**；L48 `lr.setQuantity(line.getQuantity())`——orm L689 quantity mandatory 仅挡 null 不挡负数）+ 跨域实证 `ErpInvStockMoveProcessor#newMove`（sourceWarehouseId 无校验直写 move）+ 时长面 `ErpMntVisitCompleteProcessor#doComplete` L54-57 与 `ErpMntDowntimeEntryCompleteProcessor#doComplete` L36-39（`Duration.between(start, end).toMinutes()` 无负守卫——CRUD 手改 endTime 早于 startTime 后 complete 即落负 totalMinutes；labor 链有 `signum() <= 0` 跳过兜底（dispatcher L101-103），停机链无兜底负值直接入库供 OEE/报表消费）
- **证据**：①usage 漏配领料仓库 → OUTGOING 移动单 sourceWarehouseId=null 出库（余额扣减行为取决于 inv 侧 null 仓库路径，未定义）；②行 quantity=-5 → 负出库 = 库存净增加 + totalAmount 负数聚合；③负时长落库。
- **问题**：D5 入参边界三连缺失；与 mfg P3-CK-mfg-012（报工负数）同型但 mnt 侧连 warehouseId 必填都缺。
- **建议修复方向**：confirm 前置校验 warehouseId 非空（新错误码或复用通用必填语义）+ 行 quantity `signum() <= 0` 拒绝 + duration 负值拒绝（或钳 0 + warn）。
- **arm-index 裁决**：新增（同族注记 P3-CK-mfg-012；grep「备件 负数量/warehouseId 必填」零命中）。

### P3-CK-mnt-010（D9）OEE fleet 聚合 N+1——每设备 8+ 查询的嵌套循环，看板每次刷新 O(N×8) 次 DAO 往返

- **控制点**：`app/erp/mnt/service/dashboard/ErpMntDashboardBizModel#computeOeeList/getDashboardOeeKpi`（L118-132/L139-190：`for (equipment) oeeCalculator.computeOee(equipment, ...)`）→ `OeeCalculator#computeOee` 每设备触发：`findLogs`（全量日志行载入，`EquipmentRuntimeCalculator` L137-145 无窗口过滤）+ `computeCalendarHours`（再逐日循环）+ `computeDowntimeHours` + `collectOutput`（card 查询 + timelog 查询 + WO 查询）+ `resolveCapacityPerHour` + `findLinkedInspections` ≈ 7-8 查询/设备
- **问题**：100 台设备看板刷新 ≈ 800 次查询 + 全量日志实体化。结果正确（纯性能），与 mfg-014 同型但因子更多。设计已裁决「按需聚合无物化」（B 类，plan 2026-08-20-0518-1），登记性能维度非裁决挑战。
- **建议修复方向**：fleet 路径批量预取（一次 in 查询按 equipmentId 分组）或日志查询下推时间窗过滤。
- **arm-index 裁决**：新增（同型家族 mfg-014/pur-011/sal-026/inv-020）。

### P3-CK-mnt-011（D8/D4）设计声明的「DRAFT 访问产生 TODO 提醒维护主管」未落地——防滞留机制缺失

- **控制点**：`docs/design/maintenance/equipment-integration.md` §5.2（L231「产生 TODO 提醒维护主管排程」）+ `state-machine.md` §8 TODO 策略表（DRAFT→assigned TODO 维护主管「待排程」；SCHEDULED/IN_PROGRESS 同列）对照 grep 全 `erp-mnt-service/src/main/java` TODO 生成写点**零命中**（notify 仅 7208/7209 停机与过账失败告警两族）
- **问题**：owner doc 声明的防滞留机制（「避免计划生成的访问长期无人排程」）无任何实现载体；部分兜底 = `findMaintenanceOverdueAlert`（计划逾期维度，非访问滞留维度）。
- **建议修复方向**：生成 DRAFT 访问时派发 notify（新模板）或落 TODO 实体；或 owner doc 显式 Deferred。
- **arm-index 裁决**：新增（grep「TODO 提醒/待排程」零命中）。

### P3-CK-mnt-012（D4，同型 P3-CK-mfg-013/P3-CK-inv-021/P3-CK-sal-023 家族）cron 键三层门控漂移——`erp-mnt.due-visit-cron` 的值从不被当作 cron 表达式消费（仅判空），实际节奏在 nop.job 键且默认非空

- **控制点**：`ErpMntDueVisitJob#resolveCronConfig`（L55-57：`AppConfig.var("erp-mnt.due-visit-cron", "")` 默认空 → `execute` L37-40 判空即 return——**该键的值从未传入任何调度器**）对照 `app-erp-all/_vfs/nop/job/conf/erp-mnt-due-visit-generation.job.yaml`（实际 cron = `@cfg:nop.job.erp-mnt-due-visit-generation.cron-expr|0 0 1 * * ?` 默认非空 + `enabled|false`）+ `ErpMntConstants` L31-32 注释「空=不调度」
- **证据**：启用到期生成需同时配 `nop.job.erp-mnt-due-visit-generation.enabled=true` **和** `erp-mnt.due-visit-cron` 非空（任意垃圾值均放行，如 `"x"`）；运维按 owner doc 只配后者无效果。附带文档漂移：`use-cases.md` L30 写「以 `LocalDate.now()` 为基准」而实现用 `CoreMetrics.today()`。
- **问题**：同型家族（声明 cron 键 vs 实际 nop-job 键漂移 + 双层门控靠第二层兜底）；键语义误导（名为 cron 实为开关）。
- **建议修复方向**：删除该键改为 `erp-mnt.due-visit-generation-enabled` 布尔开关（或消费 nop.job enabled 单层）；owner doc/job-scheduling.md §3.13 同步修正。
- **arm-index 裁决**：新增（同型家族注记）。

### P3-CK-mnt-013（D6/D10）停机/OEE 计算边界四项——重叠停机双计、quality 可 >100%、报表日期右边界含次日零点、开放窗口含 startTime null 草稿行

- **控制点**：①`ErpMntDowntimeEntryBizModel`/record 链无同设备开放停机互斥校验（两条 endTime=null 并存合法）→ `OeeCalculator#computeDowntimeHours`（L246-261 逐条 `overlapSeconds` 求和）重叠段**双计**，计划运行时间被多扣 → availability 偏低；②`OeeCalculator` L144-146 `quality = ΣACCEPTED lotQuantity / actualOutput`——检验批量 > 报工量时 quality > 1（无 `min(1, x)` 钳制，展示层 `percentDisplay` 直接显示 >100%）；③`ErpMntReportBizModel#loadDowntimeEntries`（L356 `le("startTime", endDate.plusDays(1).atStartOfDay())`——次日零点整的记录被含入，应 `lt`）；④`findOpenDowntimeEquipmentWorkcenters`（L57-60 仅 `isNull("endTime")`，startTime null 的草稿行进入窗口集，`window.startTime=null` 传导 mfg 消费方）
- **问题**：D6/D10 边界项聚合登记；单条影响小、均为可见计算失真面。
- **建议修复方向**：record 前置同设备开放停机互斥守卫；quality 钳制 ≤1；`lt` 边界；开放窗口查询补 `startTime` 非空。
- **arm-index 裁决**：新增。

### P3-CK-mnt-014（D5/D1）`changeStatus` 无字典成员校验——任意字符串直写 status 列并同步污染状态日志

- **控制点**：`app/erp/mnt/service/entity/ErpMntEquipmentBizModel#changeStatus`（L30-42：`equipment.setStatus(newStatus)` 前无 `erp-mnt/equipment-status` 五值成员校验，直接 `updateEntity` + `statusLogWriter.append`）对照 dict `erp-mnt/equipment-status`（RUNNING/IDLE/UNDER_MAINTENANCE/DOWN/DECOMMISSIONED）
- **证据**：`changeStatus(equipmentId, "XYZ")` → status 列存非法值 + StatusLog from/to 各含非法码 → 运行时长聚合按 RUNNING 精确匹配（漏计该段）+ 状态分布看板落「UNKNOWN」桶（L205 兜底存在）。手动改状态是设计允许的操作（RC-R1.73 MANUAL 来源），缺的只是值域校验。
- **建议修复方向**：`changeStatus` 前置 dict 成员校验（`IDaoProvider` 查 dict 或常量集），非法抛业务错误码。
- **arm-index 裁决**：新增。

### P3-CK-mnt-015（D10/D2，同型 P3-CK-mfg-016 全域族）reportAdditionalFault 空描述断链 + toLongUserId 宽 catch 静默 null

- **控制点**：`ErpMntVisitReportAdditionalFaultProcessor#doCreateRequest`（L65 `data.put("description", description)`——description 为 null/blank 时 visit remark 用 `faultText`（description 回退 remark，L45）**而新请求 description 原样 null**：故障信息只留在 visit remark，新 OPEN 请求（owner doc 语义「另开新维护请求处理额外故障」）无故障描述；request.description 列若 mandatory 则 save 直接失败）；`#toLongUserId`（L79-88 `catch (NumberFormatException) return null` 无日志——requestedBy 静默回退 assignedTo，同型全域族 P3-CK-md-008/pur-010/... 的 mnt 站点）
- **建议修复方向**：`doCreateRequest` 复用 faultText 回退；catch 补 LOG.warn。
- **arm-index 裁决**：新增（toLongUserId 维度同型登记全域族）。

### P3-CK-mnt-016（D6，同型 P2-CK-inv-010/P3-CK-mfg-021 族）两 PostingEvent 汇率硬编码 ONE——issue 侧 ledger 币种非本位币时凭证错配

- **控制点**：`MaintenanceIssuePostingDispatcher#buildEvent`（L161-168 currencyId 取自 ledger 首行 + L174-176 功能币兜底，**L179 `event.setExchangeRate(BigDecimal.ONE)`**——若 ledger 行携带非本位币币种，凭证以该币种金额按汇率 1 记账）+ `MaintenanceLaborPostingDispatcher#buildEvent`（L202-205：currencyId 恒功能币 + rate ONE——labor 侧一致无错配，登记为家族对称注记）
- **问题**：同型「汇率恒 1」家族 mnt 站点（mnt 单币种部署默认无影响；issue 侧触发条件 = ledger 存在非本位币行）。
- **建议修复方向**：issue 侧 currencyId 非功能币时换算金额或显式拒过账（错误码）；随 inv-010 家族联合修复。
- **arm-index 裁决**：新增（同族注记 P2-CK-inv-010）。

### P3-CK-mnt-017（D9）报表/看板无界加载 + `setLimit(5000)` 截断致逾期误报

- **控制点**：`ErpMntReportBizModel#loadVisits`（L290-298：equipmentId/日期全 `@Optional`，全空时全表 visits 实体化——`maintenanceHistoryData` @BizQuery 直通前端）；`ErpMntDashboardBizModel#loadScheduleIdsWithVisit`（L353-362 `q.setLimit(5000)`——注释自认「类 C：带硬上限的受限扫描」：第 5001 条后的 visit 不入集合，其 schedule 被误报为逾期，`findMaintenanceOverdueAlert` 告警噪音 + 状态失真）；`OeeCalculator#computeCalendarHours` L187（窗口逐日循环，跨年窗口千次迭代/行）
- **建议修复方向**：数据集查询强制默认窗口或分页；overdue 判定改为 per-schedule existsVisit 子查询/JOIN。
- **arm-index 裁决**：新增（同型家族 pur-011/sal-026/inv-020/mfg-014）。

### P3-CK-mnt-018（D8）visit cancel 对已确认备件消耗零动作 + 报修-访问一对一硬绑定——作废语义未裁决的两组残留

- **控制点**：`ErpMntVisitCancelProcessor#cancel`（L22-33：仅状态 + 设备恢复 + labor 红冲；对挂在该 visitId 下 ACTIVE+posted=true 的 `ErpMntSparePartUsage` 零检查零动作零提示——orm L638 `visitId` 列 + IDX 存在关联）+ `ErpMntRequestAcceptProcessor#generateResponsiveVisit`（L43 `code = "VST-REQ-" + request.getId()` 确定性幂等锚 + request ACCEPTED 后不可再 accept → visit 作废后请求无法重新生成访问，只能整单重建）
- **问题**：①cancel IN_PROGRESS visit 时已消耗备件（库存已出/GL 已过账）悬挂在 CANCELLED visit 下——物料可能确已实物消耗（人工另行 reverseConfirm 是出路），但无守卫无提示属边界缺失；②一对一绑定使「访问作废重开」路径断裂。设计文档对两者均未裁决——按检查纪律登记不裁决。
- **建议修复方向**：cancel 前置检查 open/ACTIVE usage（拒绝或 warn 确认）；重开路径 owner doc 裁决（code 追加序号或 request 回 OPEN 边）。
- **arm-index 裁决**：新增（疑似需求分歧只登记）。

### P3-CK-mnt-019（D7，复用注记 P1-MA2-086）并发幂等的 UK 兜底因 orgId=null 失效——`(code, orgId)` UK 对 NULL orgId 不去重（多数 DB NULL≠NULL）

- **控制点**：`ScheduleDueGenerator#existsVisitForScheduleDate`（L150-156 预检）+ `#generateVisit`（L186 code=`"VST-SCH-{schedId}-{asOfDate}"`，data map 不含 orgId，job 上下文无用户 → orgId=null）对照 orm L311 `UK_MNT_VISIT_CODE_ORG (code, orgId)`
- **证据**：P1-MA2-086（R1.28 已修）的并发防护 = 预检 + code UK 兜底双层；第二层在 orgId=null 时形同虚设（H2/MySQL unique 索引对 NULL 不判重）——两并发 job 同 schedule 同日均过预检则双行落库。触发面窄（nop-job-local 单实例调度下难并发），降 P3。
- **建议修复方向**：UK 改 `(code)` 单列（ORM 变更走 dual-agent-approval）或生成器写入确定 orgId（随 P2-CK-mnt-008 orgId 透传联合修复）。
- **arm-index 裁决**：新增（带复用注记——P1-MA2-086 已裁决并发幂等主链，本条为其兜底层弱点的未覆盖边界）。

## 跨域关联影响面注记（不新建 finding）

| 已登记 finding | mnt 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-fin-003`（post() 幂等命中返回 null 与 dispatcher「null=失败」语义冲突） | `MaintenanceIssuePostingDispatcher#dispatchIfApplicable`（L118-119）**不消费** `postEvent` 返回值（posted 语义=库存出库，独立于 voucherId）——无传导；`MaintenanceLaborPostingDispatcher#postLabor`（L117-118）`return voucherId != null` 仅致调用方 LOG.warn 误报（L66-67），且幂等命中主场景已被 `voucherAlreadyExists` 预检挡住，仅剩并发 race 窗口 | **传导面最小**：issue 侧无传导、labor 侧仅误 warn；随 fin-003 三态化修复时顺手核对 labor 返回值语义 |
| `P1-CK-inv-001`（出库策略 locationId 回退误用 warehouseId） | `SparePartIssueService#issue` 行请求不设 sourceLocationId（usage 行无库位列，orm 实证）→ inv 侧出库策略对无库位 OUTGOING 的回退行为（仓库 ID 写库位维度）传导至 mnt 备件出库移动单 | **同型受影响面确认**：mnt 侧无需新建 finding，修复随 inv-001 落地 |
| `P1-CK-pur-003`（CRUD update 无已审守卫全域同型） | mnt 15 实体裸 CrudBizModel（StatusLog 可改写为加重项） | **同型登记为 P2-CK-mnt-006** |
| `P2-CK-inv-012 / P1-CK-pur-002 / P2-CK-mfg-011 / P2-CK-ast-015`（REQUIRES_NEW 凭证先于主事务提交族） | mnt visit complete 链（labor 先提交、restore/联动后置） | **同型登记为 P2-CK-mnt-007** |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：56 文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now/new Date()`=0、`extends RuntimeException/Exception`=0、字典字符串 `==` 比较=0、零 `@Transactional`（javadoc 提及不计）——全部时间经 `CoreMetrics`、异常全部 `NopException`+`ErrorCode`、配置读取集中 `ErpMntConfigs`/`AppConfig.var`。
- **乐观锁在位**：mnt 全部实体 `versionProp="version"`（orm 逐实体实证 L132/194/224/268/334/370/404/438/479/516/547/583/615/683/723）——visit/request/usage/schedule 并发更新由乐观锁检测。
- **arm-index 已 resolved 的 RC 修复在位验证**：P1-RC-064（运行时长触发：`triggerType`/`thresholdHours`/`runtimeBaselineHours` + `EquipmentRuntimeCalculator` + RUNTIME 分支）、P1-RC-065（任务模板：`applyTaskTemplate`/`resolveTaskTemplate` 显式+categoryId 唯一回退）、P1-RC-066（排程冲突双维度：`checkScheduleConflict` 设备/人员独立查询）、P1-RC-070（处置联动：`linkToDecommissionedByDisposal`/`restoreFromDisposal` + 六消费点守卫）、P1-RC-071（OEE：三分量+乘积按需聚合）——实现与 owner doc 注记及测试证据一致，不重复登记。P2-RC-060（冲突 warn/config 模式）watch-only 已裁决不登记。
- **备件出库幂等键语义正确**（对照 P0-CK-mfg-001 反例）：`RELATED_BILL_TYPE_MNT_SPARE_PART + usage.code` 一对一（一消耗单一次 confirm 一张移动单），与 mfg「多次部分报工共用一键」不同构——正向主路径键语义正确（本域的缺陷在守卫缺失不在键设计，见 P1-CK-mnt-001）。
- **正向过账失败 G3 告警闭环在位**（lesson 09 正向方向）：两 dispatcher `dispatchFailureAlert` → `IErpSysNotificationBiz`（`mnt.spare-part-posting-failure`/`mnt.labor-posting-failure`）+ notify 失败降级 warn 不阻断——已修复范式，仅红冲方向缺失（P1-CK-mnt-002）。
- **postLabor 幂等与守卫在位**：`voucherAlreadyExists` 预检 + fin post 内置幂等双层；`totalMinutes signum()<=0` 跳过（负工时不过账）、`rate signum()<=0` 跳过（费率未配置不抛错）——与 owner doc Non-Goal（config 全局费率）一致。
- **completeLinkedRequest 语义正确**：requestId null 短路 / request 缺失 warn 跳过 / 终态 no-op warn / ACCEPTED 合成迁移走两条既有合法边（不加新边）/ 失败异常传播回滚（L1 硬语义）——与 state-machine.md §4 注记逐条一致（`TestErpMntVisitRequestLinkage` 覆盖）。
- **DECOMMISSIONED 守卫六消费点在位**：Schedule/Request/Visit save+update 钩子（update 仅 equipmentId 变更时）+ visit schedule 迁移 + request accept 显式拒绝 + 两批量路径查询侧排除（豁免理由注释自证）——`TestErpMntEquipmentReferenceGuard` 覆盖。
- **运行时长聚合数学正确**：`EquipmentRuntimeCalculator` 段扫描（非 RUNNING 段跳过、末段至 asOf、未来行/倒序段 `segmentSeconds` 钳 0、遗留基线 RUNNING-from-createTime/非 RUNNING-记 0 保守双分支）与 javadoc/owner doc §5.2 一致；`toHours` scale 4 HALF_UP。D6 闰年/月末：`advanceNextDueDate` 用 `plusMonths/plusYears`（java.time 处理 Jan31→Feb28/闰年正确）。
- **平台 API `addOrderField(name, desc)` 使用正确**：`ErpMntReportBizModel#loadVisits` L295-296 `addOrderField("visitDate", false)`=ASC（历史报表时序）、`addOrderField("code", false)`=ASC——与 C1.2 校准的 desc 语义相符。
- **报表路径注入防护在位**：`resolveReportPath` 经 `StringHelper.isValidVPath` + 渲染类型白名单（html/xlsx/pdf）+ 临时资源延迟清理——非缺陷。
- **dict 死状态检查（B2）无死值**：equipment-status 5 值全可达（UNDER_MAINTENANCE/DOWN=linker、DECOMMISSIONED=disposal、IDLE=restore 分支+MANUAL、RUNNING=restore+MANUAL）；visit-status 5 值/visit-type/visit-result/request-status 6 值均可达（部分经 CRUD 写入——守卫缺失归 P2-CK-mnt-006 不重复计死状态）；status-log-source DISPOSAL 值不在 dict（加性追加已声明 successor，orm L74 注释在案）不登记。
- **beans 接线完整（D4）**：`app-service.beans.xml` 19 support/posting/job/statemachine bean + 15 per-mutation Processor 全注册（逐 bean id 核对）；job yaml `erp-mnt-due-visit-generation` → `erpMntDueVisitJob.execute` 直调无孤立 job；mnt 无 batch.xml（job yaml 模式）。
- **停机通知 7208/7209 门控降级在位**：`erp-mnt.downtime-notify-enabled` + try/catch warn 降级，与 owner doc §4.2/§4.3 注记一致。
- **priorStatusCache JVM 态残余风险**：plan 2026-08-15-1605-2 Deferred But Adjudicated（watch-only 五点），不重复登记。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `validateNotConfirmed/silent-guard`/`restoreToRunning 未启动取消`/`reverseConfirm 吞`/`businessDate 生成器`/`due-visit poison`/`TODO 提醒`/`due-visit-cron`/`changeStatus 字典`/`setLimit(5000)`/`VST-REQ 一对一`/`UK orgId null` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记，报告中注记）**：
  - 并发幂等主链 → **P1-MA2-086**（resolved R1.28）——P3-CK-mnt-019 仅登记其 UK 兜底弱点。
  - 排程冲突双维度 / 运行时长触发 / 任务模板 / 处置联动 / OEE → **P1-RC-066 / P1-RC-064 / P1-RC-065 / P1-RC-070 / P1-RC-071**（resolved）——实现验证在位。
  - 冲突 warn/config 模式 → **P2-RC-060**（watch-only）——不登记。
  - priorStatusCache JVM 态 → plan 2026-08-15-1605-2 Deferred——不登记。
  - 红冲吞异常终态 → **P1-CK-mfg-005 族**（本 mission）——P1-CK-mnt-002 同型登记。
  - CRUD 无守卫 → **P1-CK-pur-003 族**——P2-CK-mnt-006 同型登记。
  - REQUIRES_NEW 先提交 → **P2-CK-inv-012 / P1-CK-pur-002 / P2-CK-mfg-011 / P2-CK-ast-015 族**——P2-CK-mnt-007 同型登记。
  - orgId 聚合缺失 → **P2-CK-fin2-007 / P2-CK-mfg-007 族**——P2-CK-mnt-008 新域站点。
  - 汇率恒 1 → **P2-CK-inv-010 / P3-CK-mfg-021 族**——P3-CK-mnt-016 同族站点。
  - cron 键漂移 → **P3-CK-mfg-013 / P3-CK-inv-021 / P3-CK-sal-023 家族**——P3-CK-mnt-012 同型。
  - 负数量入参 → **P3-CK-mfg-012**（同族注记，mnt 站点新增 warehouseId/时长维度）。
  - currentUserId 类宽 catch 静默 → **P3-CK-md-008 全域族**——P3-CK-mnt-015 内同型注记。
  - 看板/报表无界加载 → **P3-CK-pur-011 / sal-026 / inv-020 / mfg-014 家族**——P3-CK-mnt-010/017 同型家族。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 3 | P1-CK-mnt-001..003 |
| P2 | 6 | P2-CK-mnt-004..009 |
| P3 | 10 | P3-CK-mnt-010..019 |

按主维度：D3×3（001/003 + 006 跨 D5 计 D5）、D2×2（002 跨 D7 计 D2、005 跨 D4 计 D4 由 005 承担 D4）、D5×2（006/009/014 计 D5 三项）、D8×3（004 跨 D6 计 D6 由 004 承担、008/011/018）、D7×2（002/007/019 计 007/019）、D4×2（005/012）、D6×2（004/013/016）、D9×2（010/017）、D10×1（013 跨计 D6 后由 015 承担 D10）、D1×0（014 跨 D1 由 D5 主导）。（精确主维度归属：001 D3、002 D2、003 D3、004 D6、005 D4、006 D5、007 D7、008 D8、009 D5、010 D9、011 D8、012 D4、013 D6、014 D5、015 D10、016 D6、017 D9、018 D8、019 D7。）

跨域关联注记 4 项（fin-003 传导面最小确认 / inv-001 同型受影响面确认 / pur-003 同型登记 006 / REQUIRES_NEW 族同型登记 007）。

## 剩余风险（查了什么/没查什么）

- **已查**：`erp-mnt-service/src/main/java` 56 文件全量逐行深读（含全部 Processor/support/posting/job/dashboard/report/状态机/常量）；跨域源码实证 5 处（`ErpInvStockMoveGenerateMoveProcessor` 幂等短路、`ErpInvStockMoveProcessor#findExisting` id DESC、`ErpInvStockMoveReverseProcessor` REVERSAL 语义、`ErpFinVoucherBizModel` post/reverse REQUIRES_NEW、inv `newMove` 无仓库校验）；orm 逐实体核对（versionProp/orgId/businessDate/mandatory/UK/dict 引用）；12 个 dict.yaml 值集全读（D3 可达性）；`app-service.beans.xml` 全 bean 接线 + job yaml；测试面交叉验证（`TestErpMntVisitRequestStateMachine` cancel 用例路径、`TestErpMntVisitCancelReversal` 红冲语义、plan 2026-08-14-0930-3 deferral 原文）。
- **未深查**：`erp-mnt-web` AMIS view.xml 契约 drift（归 C8.2）；xmeta 层前端必填拦截（如 warehouseId 若前端必填则 P2-CK-mnt-009①触发面收窄但后端无守卫的事实不变）；平台 `CrudBizModel` 是否在特定条件下自动填充 orgId/businessDate（job 的 `ServiceContextImpl` 无用户场景下 null 是确定的）；`IErpFinVoucherBiz.reverse` 对已红冲凭证二次红冲的幂等行为（影响 P1-CK-mnt-001 复活后再红冲是否翻负 GL）；测试代码正确性（31 个测试文件仅用于行为语义交叉验证）；nop-entropy `IUserContext` 取 orgId 的规范途径（P2-CK-mnt-008 修复参考）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-mnt-001**——若产品语义是「confirm 恒可重入且复活需人工承担」则降 P2；但 CANCELLED 终态可复活 + posted 语义谎言确凿，且 plan deferral 理由未覆盖该组合。建议修复阶段先写「红冲后 confirm」集成测试实证库存/GL/单据三面状态。
  2. **P1-CK-mnt-003**——severity 依赖「未启动 visit 取消」与「visit/downtime 交叉」的实际发生频率；路径①（取消 DRAFT）无测试覆盖、路径③有测试背书（字面语义）——若 owner doc 裁决「恒恢复」为既定语义，①仍属「不变」分支未实现的文档-实现分歧，建议与设计 owner 确认后定级。
  3. **P2-CK-mnt-005**——poison 中断整批的前提是 `generateDueVisits` 的 `@BizMutation` 事务边界覆盖整个循环（job → biz 方法反射调用）。若 nop-job 对 biz 方法的事务包装与预期不同（如每 saveEntity 独立提交），破坏面从「全批回滚」变为「部分提交」，两种形态都需隔离但修复方式不同——修复阶段用集成测试实证事务边界。
