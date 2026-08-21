# 前向耦合登记册（id-string-migration M0.2 权威工件）

> 生成：2026-08-21（M0.2 计划 `docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md` Phase 3）
> 机器可读权威：`tools/id-migration-registry.json5`（本工件为其逐域视角汇编；条目 status 真相以机器可读文件为准，本工件为登记时点快照）
> 上游证据：`docs/audits/2026-08-21-1657-id-m02-orm-graph.json`（orm 跨域图 640 边）+ `docs/audits/2026-08-21-1657-id-m02-coupling-directions.json`（main 205 文件/300 文件×外域对、test 434 文件；行级 id 语境见其 idContextLines）
> 条目总数：254 active = orm-column-deferral 8 + service-bridge main 94 / test 30 + backward-pointer main 60 / test 62

## 1. 消费协议（各域 plan 强制）

1. **起草消费**：各域 plan 起草时与 Phase 1 必须消费本域条目全集（§6 对应域节的 A + C 全部条目），并把 disposition 写入该 plan：
   - **orm 级列延后**（A1）：早域迁移时延后列保持 `long`（`check-bigint-id-types.mjs` dry-run 已按登记册豁免，不翻转）；
   - **service 级临时桥接**（A2，main）：早域 plan 在调用点加 String→Long 转换桥 + grep 清单登记例外；
   - **test 级桥接**（A3，test）：早域 plan Phase 3 修复本域测试时一并适配（本域测试引用晚域实体的 id 形态）；
   - **后向指针**（C1/C2）：晚域 plan 的编译器驱动修复清单（main）与本域测试适配清单（test）——非新义务，是扫描器预生成的修复定位面。
2. **退役机制**：`status: active → retired` 由 retireOwner 工作项执行——M2.7 projects 翻转 prj orm 时**同批**翻转 fin 6 列 + hr 2 列并退役 orm-deferral-001..008（显式拥有 fin/hr 链修复责任）；main 桥接条目由晚域 plan 翻转 IBiz 参数时退役（同时移除早域桥接点）；test 桥接条目由早域自身 plan Phase 3 适配后退役。退役操作 = 编辑 `tools/id-migration-registry.json5` 对应条目 status（退役后该文件为手工维护权威，重跑 `scan-id-coupling-directions.mjs --registry-out` 仅用于对账复核）。
3. **规则 6 联动**（roadmap 规则 6 / D4 修订）：登记册内预先登记的自身链破坏**不触发**停止；未登记破坏仍触发。本登记册即「预先登记」的权威范围。
4. **工具门控**：`check-bigint-id-types.mjs`（scan/dry-run）与 `verify-id-fix-copy-diff.mjs` 仅消费 **active 的 orm-column-deferral** 条目（门控范围 ≠ 登记范围）；登记册缺失/不可解析时两工具 fail-closed 非零退出。

## 2. 总览矩阵（按冻结序位次）

| 位次 | 域 | 工作项 | A1 orm 延后 | A2 main 桥接 | A3 test 桥接 | C1 后向 main | C2 后向 test | 被引用（main/test 文件数） | B 退役义务 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | master-data | M1.1（done） | 0 | 0 | 0 | 0 | 0 | 114/232 | 0 |
| 2 | notify | M1.2 | 0 | 0 | 0 | 0 | 0 | 43/28 | 0 |
| 3 | aps | M3.9 | 0 | 15 | 4 | 1 | 1 | 0/0 | 0 |
| 4 | b2b | M3.8 | 0 | 7 | 0 | 2 | 2 | 0/0 | 0 |
| 5 | contract | M3.6 | 0 | 20 | 5 | 2 | 2 | 2/2 | 0 |
| 6 | finance | M2.1 | 6 | 10 | 1 | 2 | 2 | 32/112 | 0 |
| 7 | assets | M2.4 | 0 | 2 | 1 | 3 | 3 | 1/1 | 3 |
| 8 | cs | M3.5 | 0 | 8 | 5 | 2 | 2 | 0/0 | 0 |
| 9 | hr | M3.3 | 2 | 0 | 0 | 3 | 3 | 0/0 | 0 |
| 10 | inventory | M2.2 | 0 | 9 | 6 | 3 | 3 | 41/50 | 9 |
| 11 | maintenance | M3.2 | 0 | 6 | 1 | 4 | 4 | 1/1 | 2 |
| 12 | projects | M2.7 | 0 | 0 | 0 | 4 | 4 | 1/1 | 8 |
| 13 | quality | M2.3 | 0 | 11 | 5 | 3 | 3 | 8/5 | 5 |
| 14 | manufacturing | M3.1 | 0 | 4 | 2 | 6 | 6 | 1/1 | 20 |
| 15 | purchase | M2.5 | 0 | 2 | 0 | 6 | 7 | 3/4 | 26 |
| 16 | sales | M2.6 | 0 | 0 | 0 | 6 | 6 | 10/4 | 23 |
| 17 | crm | M3.4 | 0 | 0 | 0 | 3 | 2 | 0/0 | 4 |
| 18 | drp | M3.7 | 0 | 0 | 0 | 6 | 6 | 0/0 | 2 |
| 19 | logistics | M3.10 | 0 | 0 | 0 | 4 | 6 | 0/0 | 0 |

> 被引用列 = 其他域引用本域的 backward-pointer 条目 referencesFrom 合计（main/test 分列），供本域 plan 评估冲击面与晚域 plan 复核定位；B 退役义务 = 本域作为晚域须退役的条目数（orm 延后 + main 桥接；test 桥接由早域自身退役，不计入晚域义务）。
> master-data（位次 1，M1.1 done）/ notify（位次 2，M1.2）无自有条目（根域零前向边、零跨域引用）；common-service（M1.3 done）/ common-test 无登记条目（见 §7）。

## 3. orm 级前向边全量表（列延后，8 条）

| id | 早域 | 实体.列 | 引用 | evidence（file:line） | deferredUntil / retireOwner | status |
| --- | --- | --- | --- | --- | --- | --- |
| orm-deferral-001 | finance（6） | `ErpFinVoucherLine.projectId` | ErpPrjProject（projects，12） | module-finance/model/app-erp-finance.orm.xml:517 | M2.7 / M2.7 | active |
| orm-deferral-002 | finance（6） | `ErpFinGlBalance.projectId` | ErpPrjProject（projects，12） | module-finance/model/app-erp-finance.orm.xml:958 | M2.7 / M2.7 | active |
| orm-deferral-003 | finance（6） | `ErpFinExpenseClaimLine.projectId` | ErpPrjProject（projects，12） | module-finance/model/app-erp-finance.orm.xml:1361 | M2.7 / M2.7 | active |
| orm-deferral-004 | finance（6） | `ErpFinEmployeeAdvance.projectId` | ErpPrjProject（projects，12） | module-finance/model/app-erp-finance.orm.xml:1417 | M2.7 / M2.7 | active |
| orm-deferral-005 | finance（6） | `ErpFinBudgetLine.projectId` | ErpPrjProject（projects，12） | module-finance/model/app-erp-finance.orm.xml:1859 | M2.7 / M2.7 | active |
| orm-deferral-006 | finance（6） | `ErpFinGlMappingRule.projectId` | ErpPrjProject（projects，12） | module-finance/model/app-erp-finance.orm.xml:2063 | M2.7 / M2.7 | active |
| orm-deferral-007 | hr（9） | `ErpHrTimesheetLine.projectId` | ErpPrjProject（projects，12） | module-hr/model/app-erp-hr.orm.xml:651 | M2.7 / M2.7 | active |
| orm-deferral-008 | hr（9） | `ErpHrTimesheetLine.taskId` | ErpPrjTask（projects，12） | module-hr/model/app-erp-hr.orm.xml:652 | M2.7 / M2.7 | active |

> M2.7 执行语义：翻转 prj orm 的同时同批翻转本表 8 列（fin 6 + hr 2）并退役全部条目——这是 D4 裁定的对称破坏自愈闭环（早域引用晚域 Long 实体的 `_gen` 关系胶水，唯两端同时 String 才自愈）。

## 4. service 级前向边（main 编译级 94 条，逐条见 §6 各域 A2）

- 带 Long 参数签名解析（IBiz 接口签名）：6 条（其余为实体/DTO 类型级引用或继承方法调用点，Long 参数以晚域 plan 翻转时编译器定位）。
- 已知簇锚点：ast→mnt `ErpAstDisposalProcessor:264/271` → `IErpMntEquipmentBiz`（Long `assetId`）；fin→inv `ErpFinAccountingPeriodProcessor:222` → `IErpInvCostingBiz.reclosePeriodCosts`（Long `periodId`）。
- 注释级 1 条（`IErpMfgMrpPlanLineBiz.java:14` javadoc `{@link}`）已排除，不在登记册（与 M0 附录 B「补充」段一致）。

## 5. 后向 successor 指针（main 60 + test 62 条，逐条见 §6 各域 C）

> main = 晚域 main 代码引用早域（晚域 plan 编译器驱动修复清单）；test = 晚域测试引用早域（晚域 plan Phase 3 本域测试适配）。evidence 为文件级清单；行级 id 语境见 coupling-directions.json。

## 6. 逐域视角（冻结序）

### 6.1 master-data（位次 1，M1.1（done））

**A. 前向义务（作为早域）**

- 无（本域不引用任何晚域）。

**B. 退役/翻转义务（作为晚域）**

- 无。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：assets(main) 9 文件、b2b(main) 1 文件、contract(main) 2 文件、crm(main) 5 文件、cs(main) 7 文件、drp(main) 3 文件、finance(main) 29 文件、hr(main) 2 文件、inventory(main) 6 文件、maintenance(main) 2 文件、manufacturing(main) 11 文件、projects(main) 1 文件、purchase(main) 16 文件、quality(main) 1 文件、sales(main) 19 文件、assets(test) 3 文件、b2b(test) 1 文件、contract(test) 13 文件、crm(test) 3 文件、cs(test) 8 文件、drp(test) 9 文件、finance(test) 58 文件、hr(test) 1 文件、inventory(test) 17 文件、logistics(test) 3 文件、maintenance(test) 6 文件、manufacturing(test) 24 文件、projects(test) 10 文件、purchase(test) 38 文件、quality(test) 4 文件、sales(test) 34 文件。

### 6.2 notify（位次 2，M1.2）

**A. 前向义务（作为早域）**

- 无（本域不引用任何晚域）。

**B. 退役/翻转义务（作为晚域）**

- 无。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：aps(main) 2 文件、assets(main) 3 文件、b2b(main) 1 文件、contract(main) 6 文件、crm(main) 2 文件、cs(main) 8 文件、finance(main) 4 文件、hr(main) 3 文件、inventory(main) 2 文件、logistics(main) 3 文件、maintenance(main) 3 文件、manufacturing(main) 4 文件、projects(main) 1 文件、sales(main) 1 文件、aps(test) 2 文件、assets(test) 1 文件、b2b(test) 1 文件、contract(test) 5 文件、cs(test) 6 文件、finance(test) 1 文件、hr(test) 2 文件、inventory(test) 2 文件、logistics(test) 1 文件、maintenance(test) 1 文件、manufacturing(test) 3 文件、projects(test) 1 文件、purchase(test) 1 文件、sales(test) 1 文件。

### 6.3 aps（位次 3，M3.9）

**A. 前向义务（作为早域）**

A2 main 临时桥接（15 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-009 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/atpctp/ErpApsAtpCtpServiceImpl.java:12` | ErpInvReservation（符号使用行含 id 语境（见 idContextLines）） | — | inventory（10） | M2.2 |
| bridge-main-010 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/atpctp/ErpApsAtpCtpServiceImpl.java:13` | ErpInvReservationLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | inventory（10） | M2.2 |
| bridge-main-011 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/atpctp/ErpApsAtpCtpServiceImpl.java:14` | ErpInvStockBalance（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | inventory（10） | M2.2 |
| bridge-main-012 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/atpctp/ErpApsAtpCtpServiceImpl.java:15` | ErpMfgBom（符号使用行含 id 语境（见 idContextLines）） | — | manufacturing（14） | M3.1 |
| bridge-main-013 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/atpctp/ErpApsAtpCtpServiceImpl.java:16` | ErpMfgBomOperation（符号使用行含 id 语境（见 idContextLines）） | — | manufacturing（14） | M3.1 |
| bridge-main-014 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/loadsource/ApsLoadSourceProvider.java:5` | ApsLoadSlot（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-015 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/loadsource/ApsLoadSourceProvider.java:6` | IErpApsLoadSourceProvider（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-016 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsAutoDispatchProcessor.java:10` | ErpInvStockBalance（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | inventory（10） | M2.2 |
| bridge-main-017 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsAutoDispatchProcessor.java:11` | ErpMfgBom（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-018 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsAutoDispatchProcessor.java:12` | ErpMfgBomLine（符号使用行含 id 语境（见 idContextLines）） | — | manufacturing（14） | M3.1 |
| bridge-main-019 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsAutoDispatchProcessor.java:13` | ErpMfgWorkOrder（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-020 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsAutoDispatchProcessor.java:14` | ErpMfgWorkcenter（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-021 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsWorkOrderToOperationProcessor.java:8` | ErpMfgRoutingOperation（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-022 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsWorkOrderToOperationProcessor.java:9` | ErpMfgWorkOrder（符号使用行含 id 语境（见 idContextLines）） | — | manufacturing（14） | M3.1 |
| bridge-main-023 | `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsWorkOrderToOperationProcessor.java:10` | ErpMfgWorkcenter（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |

A3 test 桥接（4 条；本域 plan Phase 3 修复本域测试时适配，随后退役）：

| id | 测试文件 | 引用（目标@行） | owner |
| --- | --- | --- | --- |
| bridge-test-103 | `module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsAutoDispatch.java` | ErpInvStockBalance(inventory) @L8, ErpMfgBom(manufacturing) @L9, ErpMfgBomLine(manufacturing) @L10, ErpMfgWorkOrder(manufacturing) @L11 | M3.9 |
| bridge-test-104 | `module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsCrossDomainIntegration.java` | ApsLoadSlot(manufacturing) @L4 | M3.9 |
| bridge-test-105 | `module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsDemandPlanning.java` | ErpInvStockBalance(inventory) @L3 | M3.9 |
| bridge-test-106 | `module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsWorkOrderToOperationOrder.java` | ApsLoadSlot(manufacturing) @L6, IErpApsLoadSourceProvider(manufacturing) @L7, ErpMfgRouting(manufacturing) @L8, ErpMfgRoutingOperation(manufacturing) @L9, ErpMfgWorkOrder(manufacturing) @L10, ErpMfgWorkcenter(manufacturing) @L11 | M3.9 |

**B. 退役/翻转义务（作为晚域）**

- 无。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（1 条，编译器驱动修复清单）：
- backward-133 → notify（2），引用 2 文件：`module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsAutoDispatchProcessor.java`、`module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsWorkOrderToOperationProcessor.java`（successor M3.9）

C2 后向 test（1 条，本域测试适配清单）：
- backward-193 → notify（2），引用 2 文件：`module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsAutoDispatch.java`、`module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsWorkOrderToOperationOrder.java`（successor M3.9）

### 6.4 b2b（位次 4，M3.8）

**A. 前向义务（作为早域）**

A2 main 临时桥接（7 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-026 | `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java:10` | ErpPurOrder（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-027 | `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java:11` | ErpPurOrderLine（符号使用行含 id 语境（见 idContextLines）） | — | purchase（15） | M2.5 |
| bridge-main-028 | `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java:12` | ErpPurReceive（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-029 | `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java:13` | ErpPurReceiveLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-030 | `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnMatchPurchaseOrderProcessor.java:9` | ErpPurOrder（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-031 | `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnMatchPurchaseOrderProcessor.java:10` | ErpPurOrderLine（符号使用行含 id 语境（见 idContextLines）） | — | purchase（15） | M2.5 |
| bridge-main-032 | `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/spi/ubl/UblInvoiceEdiProvider.java:53` | ErpSalInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |

**B. 退役/翻转义务（作为晚域）**

- 无。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（2 条，编译器驱动修复清单）：
- backward-137 → master-data（1），引用 1 文件：`module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java`（successor M3.8）
- backward-138 → notify（2），引用 1 文件：`module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/job/ErpB2bOnboardingMonitorJob.java`（successor M3.8）

C2 后向 test（2 条，本域测试适配清单）：
- backward-197 → master-data（1），引用 1 文件：`module-b2b/erp-b2b-service/src/test/java/app/erp/b2b/service/TestErpB2bPartnerOnboarding.java`（successor M3.8）
- backward-198 → notify（2），引用 1 文件：`module-b2b/erp-b2b-service/src/test/java/app/erp/b2b/service/TestErpB2bPartnerOnboarding.java`（successor M3.8）

### 6.5 contract（位次 5，M3.6）

**A. 前向义务（作为早域）**

A2 main 临时桥接（20 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-033 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtInvoicePlanBizModel.java:32` | ErpPurInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-034 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtInvoicePlanBizModel.java:33` | ErpPurInvoiceLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-035 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtInvoicePlanBizModel.java:34` | ErpSalInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-036 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtInvoicePlanBizModel.java:35` | ErpSalInvoiceLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-037 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateAgreementBizModel.java:23` | ErpPurInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-038 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateAgreementBizModel.java:24` | ErpSalInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-039 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateSettlementBizModel.java:27` | ErpPurInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-040 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateSettlementBizModel.java:28` | ErpPurInvoiceLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-041 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateSettlementBizModel.java:29` | ErpSalInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-042 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateSettlementBizModel.java:30` | ErpSalInvoiceLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-043 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtInvoicePlanTriggerInvoiceProcessor.java:8` | ErpPurInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-044 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtInvoicePlanTriggerInvoiceProcessor.java:9` | ErpPurInvoiceLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-045 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtInvoicePlanTriggerInvoiceProcessor.java:10` | ErpSalInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-046 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtInvoicePlanTriggerInvoiceProcessor.java:11` | ErpSalInvoiceLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-047 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtRebateAgreementRunAccrualProcessor.java:9` | ErpPurInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-048 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtRebateAgreementRunAccrualProcessor.java:10` | ErpSalInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-049 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtRebateSettlementPostSettlementProcessor.java:11` | ErpPurInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-050 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtRebateSettlementPostSettlementProcessor.java:12` | ErpPurInvoiceLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-051 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtRebateSettlementPostSettlementProcessor.java:13` | ErpSalInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-052 | `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtRebateSettlementPostSettlementProcessor.java:14` | ErpSalInvoiceLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |

A3 test 桥接（5 条；本域 plan Phase 3 修复本域测试时适配，随后退役）：

| id | 测试文件 | 引用（目标@行） | owner |
| --- | --- | --- | --- |
| bridge-test-108 | `module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtBillingFamily.java` | ErpPurInvoice(purchase) @L15 | M3.6 |
| bridge-test-109 | `module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtContractExpiryJob.java` | ErpPurInvoice(purchase) @L13 | M3.6 |
| bridge-test-110 | `module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtContractPosting.java` | ErpPurInvoice(purchase) @L8, ErpSalInvoice(sales) @L9 | M3.6 |
| bridge-test-111 | `module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtContractRebate.java` | ErpPurInvoice(purchase) @L13 | M3.6 |
| bridge-test-112 | `module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtRebateSettlementEnd.java` | ErpSalInvoice(sales) @L10 | M3.6 |

**B. 退役/翻转义务（作为晚域）**

- 无。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（2 条，编译器驱动修复清单）：
- backward-139 → master-data（1），引用 2 文件：`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtRebateSettlementBizModel.java`、`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtRebateSettlementPostSettlementProcessor.java`（successor M3.6）
- backward-140 → notify（2），引用 6 文件：`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtApprovalRecordBizModel.java`、`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtContractBizModel.java`、`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtDocumentBizModel.java`、`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/job/ErpCtApprovalTimeoutEscalationJob.java`、`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/job/ErpCtContractExpiryJob.java`、`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/processor/ErpCtConsumptionPeriodSummarizeProcessor.java`（successor M3.6）

C2 后向 test（2 条，本域测试适配清单）：
- backward-199 → master-data（1），引用 13 文件：`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtApprovalTimeoutJob.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtApprovalWorkflow.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtBillingFamily.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtContractCreateValidate.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtContractExpiryJob.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtContractPosting.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtContractRebate.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtContractTerminate.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtDocRetention.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtDocumentGuards.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtESignature.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtRebateSettlementEnd.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtTerminateGate.java`（successor M3.6）
- backward-200 → notify（2），引用 5 文件：`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtApprovalTimeoutJob.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtBillingFamily.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtContractExpiryJob.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtDocRetention.java`、`module-contract/erp-ct-service/src/test/java/app/erp/ct/service/TestErpCtTerminateGate.java`（successor M3.6）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：purchase(main) 1 文件、sales(main) 1 文件、purchase(test) 1 文件、sales(test) 1 文件。

### 6.6 finance（位次 6，M2.1）

**A. 前向义务（作为早域）**

A1 orm 列延后（6 条，见 §3 全量表）：
- orm-deferral-001 `ErpFinVoucherLine.projectId` → ErpPrjProject（module-finance/model/app-erp-finance.orm.xml:517），保持 long 至 M2.7；
- orm-deferral-002 `ErpFinGlBalance.projectId` → ErpPrjProject（module-finance/model/app-erp-finance.orm.xml:958），保持 long 至 M2.7；
- orm-deferral-003 `ErpFinExpenseClaimLine.projectId` → ErpPrjProject（module-finance/model/app-erp-finance.orm.xml:1361），保持 long 至 M2.7；
- orm-deferral-004 `ErpFinEmployeeAdvance.projectId` → ErpPrjProject（module-finance/model/app-erp-finance.orm.xml:1417），保持 long 至 M2.7；
- orm-deferral-005 `ErpFinBudgetLine.projectId` → ErpPrjProject（module-finance/model/app-erp-finance.orm.xml:1859），保持 long 至 M2.7；
- orm-deferral-006 `ErpFinGlMappingRule.projectId` → ErpPrjProject（module-finance/model/app-erp-finance.orm.xml:2063），保持 long 至 M2.7；

A2 main 临时桥接（10 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-061 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java:4` | ErpAstDepreciationSchedule（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | assets（7） | M2.4 |
| bridge-main-062 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java:15` | CostingRecloseReport（符号使用行含 id 语境（见 idContextLines）） | — | inventory（10） | M2.2 |
| bridge-main-063 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java:194` | IErpAstDepreciationScheduleBiz.executeBatchDepreciation | — | assets（7） | M2.4 |
| bridge-main-064 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java:222` | IErpInvCostingBiz.reclosePeriodCosts | `periodId` | inventory（10） | M2.2 |
| bridge-main-065 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java:256` | IErpAstDepreciationScheduleBiz.reverseDepreciation | `assetId` | assets（7） | M2.4 |
| bridge-main-066 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java:536` | ErpInvLandedCost（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | inventory（10） | M2.2 |
| bridge-main-067 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java:537` | class（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | inventory（10） | M2.2 |
| bridge-main-068 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java:544` | ErpInvLandedCost（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | inventory（10） | M2.2 |
| bridge-main-069 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/reconciliation/DualSideConsistencyChecker.java:8` | ErpPurInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-070 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/reconciliation/DualSideConsistencyChecker.java:9` | ErpSalInvoice（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |

A3 test 桥接（1 条；本域 plan Phase 3 修复本域测试时适配，随后退役）：

| id | 测试文件 | 引用（目标@行） | owner |
| --- | --- | --- | --- |
| bridge-test-118 | `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/reconciliation/TestErpFinDualSideConsistency.java` | ErpPurInvoice(purchase) @L8 | M2.1 |

**B. 退役/翻转义务（作为晚域）**

- 无。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（2 条，编译器驱动修复清单）：
- backward-152 → master-data（1），引用 29 文件：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/annualclose/AnnualCloseService.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/baddebt/BadDebtProvisionService.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/bankrecon/BankReconAdjustmentVoucherBuilder.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/BudgetVoucherGenerator.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/CommitmentVoucherGenerator.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetCommitmentBizModel.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetControlBiz.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinBudgetLineBizModel.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/fx/ExchangeRevaluationService.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/intercompany/ErpFinIntercompanyTransferBizModel.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/intercompany/IntercompanyVoucherGenerator.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/EmployeeAdvancePostingDispatcher.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinGlMappingResolver.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinTransferPriceResolver.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ExpenseClaimPostingDispatcher.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/SchemaPropagator.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/AbstractErpFinReconciliationProcessor.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinBadDebtProcessor.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinBudgetScenarioCarryForwardProcessor.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinConsolidationEliminationPostEliminationProcessor.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinEmployeeAdvanceProcessor.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinExpenseClaimProcessor.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesReceivableProcessor.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/profitloss/ProfitLossClosingService.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/reconciliation/PartnerBalanceUpdater.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/treasury/CreditFacilityInterestVoucherBuilder.java`（successor M2.1）
- backward-153 → notify（2），引用 4 文件：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinPostingExceptionBizModel.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinDeferredPostingRetryHelper.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingExceptionRecorder.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinPostingExceptionIgnoreProcessor.java`（successor M2.1）

C2 后向 test（2 条，本域测试适配清单）：
- backward-211 → master-data（1），引用 58 文件：`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/TestClockRolloverFinance.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/TestErpFinAging.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/bankrecon/TestErpFinBankReconAutoReverseJob.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/bankrecon/TestErpFinBankReconciliation.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/bankrecon/TestErpFinBankReconciliationEndToEnd.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/bankrecon/TestErpFinBankStatementCounterpartyMatch.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/bankrecon/TestErpFinBankStatementMatch.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/dashboard/TestErpFinDashboard.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/PeriodCloseTestSupport.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinAnnualClose.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinAuxiliaryReconGate.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinBadDebt.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinBadDebtProvisionReversal.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinBadDebtReversal.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinBudgetCarryForward.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinBudgetCommitment.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinBudgetEndToEnd.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinBudgetIsolation.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinBudgetRollForward.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinEmployeeAdvanceApproval.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinExchangeRevaluation.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinExpenseClaimApproval.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinNotesPayableStateMachine.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinNotesReceivableStateMachine.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinPostingExceptionWorkbench.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinPostingMetrics.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinProfitLossClosing.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinTrialBalanceCommitmentExclusion.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinVoucherPeriodLock.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/intercompany/TestErpFinIntercompanyTransfer.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/perf/TestErpFinPeriodClosePerf.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/perf/TestErpFinReportRenderPerf.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/perf/TestErpFinVoucherPostingPerf.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinArApItemGeneration.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinEmployeeAdvanceCashRepay.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinEmployeeAdvanceCashRepayReversal.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinEmployeeAdvancePosting.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinExpenseClaimPosting.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinExpenseOffsetAdvance.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinFxRateGuard.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinGlDistribution.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinGlMappingResolver.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinMultiSchemaPosting.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinNotesPayablePosting.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinNotesReceivablePosting.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinPartnerIdResolution.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinPostingExceptionNotify.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinPostingObservability.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinPostingService.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinReversalDispatch.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/reconciliation/TestErpFinAutoReconciliation.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/reconciliation/TestErpFinDualSideConsistency.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/reconciliation/TestErpFinPartnerBalance.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/reconciliation/TestErpFinReconciliation.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/reconciliation/TestErpFinReconciliationReversePreview.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/report/TestErpFinMultiSchemaReportIsolation.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/report/TestErpFinReportRendering.java`、`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/treasury/TestErpFinCreditFacilityInterest.java`（successor M2.1）
- backward-212 → notify（2），引用 1 文件：`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinPostingExceptionNotify.java`（successor M2.1）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：assets(main) 2 文件、hr(main) 3 文件、inventory(main) 4 文件、logistics(main) 1 文件、maintenance(main) 3 文件、manufacturing(main) 2 文件、projects(main) 3 文件、purchase(main) 6 文件、quality(main) 2 文件、sales(main) 6 文件、assets(test) 12 文件、hr(test) 3 文件、inventory(test) 12 文件、logistics(test) 4 文件、maintenance(test) 7 文件、manufacturing(test) 9 文件、projects(test) 10 文件、purchase(test) 27 文件、quality(test) 2 文件、sales(test) 26 文件。

### 6.7 assets（位次 7，M2.4）

**A. 前向义务（作为早域）**

A2 main 临时桥接（2 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-024 | `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstDisposalProcessor.java:264` | IErpMntEquipmentBiz.changeStatusForAssetDisposal | `assetId` | maintenance（11） | M3.2 |
| bridge-main-025 | `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstDisposalProcessor.java:271` | IErpMntEquipmentBiz.restoreFromAssetDisposal | `assetId` | maintenance（11） | M3.2 |

A3 test 桥接（1 条；本域 plan Phase 3 修复本域测试时适配，随后退役）：

| id | 测试文件 | 引用（目标@行） | owner |
| --- | --- | --- | --- |
| bridge-test-107 | `module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestMockMntBizModels.java` | IErpMntEquipmentBiz(maintenance) @L3, ErpMntEquipment(maintenance) @L4 | M2.4 |

**B. 退役/翻转义务（作为晚域）**

- main 桥接退役（3 条）：本域 plan 翻转 IBiz 参数签名时退役对应条目并通知早域移除桥接点：bridge-main-061（早域 finance）、bridge-main-063（早域 finance）、bridge-main-065（早域 finance）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（3 条，编译器驱动修复清单）：
- backward-134 → finance（6），引用 2 文件：`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetPostingExecutor.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstDepreciationScheduleProcessor.java`（successor M2.4）
- backward-135 → master-data（1），引用 9 文件：`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetInventoryPostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetMergePostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/AssetSplitPostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/CapitalizationPostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DepreciationPostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DisposalPostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/MaintenanceCapitalizationPostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/MaintenanceExpensePostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/ValueAdjustmentPostingDispatcher.java`（successor M2.4）
- backward-136 → notify（2），引用 3 文件：`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/CapitalizationPostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DepreciationPostingDispatcher.java`、`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DisposalPostingDispatcher.java`（successor M2.4）

C2 后向 test（3 条，本域测试适配清单）：
- backward-194 → finance（6），引用 12 文件：`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/AstTestSupport.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstAcctDocProviderAccountKey.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstCapitalization.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstCatchUpDepreciation.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstCipTransfer.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstDepreciation.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstDisposal.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstInventory.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstMaintenance.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstPostingReverse.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstSplitMerge.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstValueAdjustment.java`（successor M2.4）
- backward-195 → master-data（1），引用 3 文件：`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/AstTestSupport.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstCapitalization.java`、`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstCipTransfer.java`（successor M2.4）
- backward-196 → notify（2），引用 1 文件：`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/posting/TestDepreciationPostingFailureAlert.java`（successor M2.4）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：projects(main) 1 文件、projects(test) 1 文件。

### 6.8 cs（位次 8，M3.5）

**A. 前向义务（作为早域）**

A2 main 临时桥接（8 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-053 | `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/TicketAssignResolver.java:5` | ErpCrmTeam（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | crm（17） | M3.4 |
| bridge-main-054 | `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/TicketAssignResolver.java:6` | ErpCrmTeamMember（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | crm（17） | M3.4 |
| bridge-main-055 | `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/TicketAssignResolver.java:52` | IErpCrmTeamBiz.findList（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | crm（17） | M3.4 |
| bridge-main-056 | `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/TicketAssignResolver.java:59` | IErpCrmTeamMemberBiz.findList（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | crm（17） | M3.4 |
| bridge-main-057 | `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketEscalateToQualityProcessor.java:10` | ErpQaNonConformance（符号使用行含 id 语境（见 idContextLines）） | — | quality（13） | M2.3 |
| bridge-main-058 | `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketEscalateToQualityProcessor.java:151` | IErpQaNonConformanceBiz.findList（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | quality（13） | M2.3 |
| bridge-main-059 | `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketEscalateToQualityProcessor.java:184` | IErpQaNonConformanceBiz.save（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | quality（13） | M2.3 |
| bridge-main-060 | `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketEscalateToQualityProcessor.java:189` | IErpQaNonConformanceBiz.findList（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | quality（13） | M2.3 |

A3 test 桥接（5 条；本域 plan Phase 3 修复本域测试时适配，随后退役）：

| id | 测试文件 | 引用（目标@行） | owner |
| --- | --- | --- | --- |
| bridge-test-113 | `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsCatalogFulfillmentEngine.java` | ErpCrmTeam(crm) @L8, ErpCrmTeamMember(crm) @L9 | M3.5 |
| bridge-test-114 | `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsQualityEscalation.java` | ErpQaNonConformance(quality) @L6 | M3.5 |
| bridge-test-115 | `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsTicketCreateEnrichment.java` | ErpCrmTeam(crm) @L10, ErpCrmTeamMember(crm) @L11 | M3.5 |
| bridge-test-116 | `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestMockCrmBizModels.java` | IErpCrmTeamBiz(crm) @L3, IErpCrmTeamMemberBiz(crm) @L4, ErpCrmTeam(crm) @L5, ErpCrmTeamMember(crm) @L6 | M3.5 |
| bridge-test-117 | `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestMockQaBizModels.java` | IErpQaNonConformanceBiz(quality) @L3, ErpQaNonConformance(quality) @L4, ErpQaRecall(quality) @L5 | M3.5 |

**B. 退役/翻转义务（作为晚域）**

- 无。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（2 条，编译器驱动修复清单）：
- backward-144 → master-data（1），引用 7 文件：`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsCannedResponseBizModel.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsEntitlementBizModel.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsTicketBizModel.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsEntitlementExpiryJob.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsCannedResponseApplyCannedResponseProcessor.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketScanOverdueTicketsProcessor.java`（successor M3.5）
- backward-145 → notify（2），引用 8 文件：`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsEntitlementBizModel.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsTicketBizModel.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsCsatReminderJob.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsEntitlementExpiryJob.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsSurveySendJob.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketResolveProcessor.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketScanOverdueTicketsProcessor.java`（successor M3.5）

C2 后向 test（2 条，本域测试适配清单）：
- backward-203 → master-data（1），引用 8 文件：`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsCannedResponseBiz.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsCatalogFulfillmentEngine.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsEntitlement.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsMultiLevelEscalation.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsServiceCatalog.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsSlaNotification.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsTicketCreateEnrichment.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/job/TestErpCsSurveySendJob.java`（successor M3.5）
- backward-204 → notify（2），引用 6 文件：`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsCatalogFulfillmentEngine.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsKnowledgeAdoption.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsMultiLevelEscalation.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsSlaNotification.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsTicketCreateEnrichment.java`、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/job/TestErpCsSurveySendJob.java`（successor M3.5）

### 6.9 hr（位次 9，M3.3）

**A. 前向义务（作为早域）**

A1 orm 列延后（2 条，见 §3 全量表）：
- orm-deferral-007 `ErpHrTimesheetLine.projectId` → ErpPrjProject（module-hr/model/app-erp-hr.orm.xml:651），保持 long 至 M2.7；
- orm-deferral-008 `ErpHrTimesheetLine.taskId` → ErpPrjTask（module-hr/model/app-erp-hr.orm.xml:652），保持 long 至 M2.7；

**B. 退役/翻转义务（作为晚域）**

- 无。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（3 条，编译器驱动修复清单）：
- backward-154 → finance（6），引用 3 文件：`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingDispatcher.java`、`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingExecutor.java`、`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java`（successor M3.3）
- backward-155 → master-data（1），引用 2 文件：`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingDispatcher.java`、`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java`（successor M3.3）
- backward-156 → notify（2），引用 3 文件：`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/job/ErpHrContractExpiryJob.java`、`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/job/ErpHrLeaveApproverTimeoutJob.java`、`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/SalaryPostingDispatcher.java`（successor M3.3）

C2 后向 test（3 条，本域测试适配清单）：
- backward-213 → finance（6），引用 3 文件：`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrPayrollEngine.java`、`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrSalaryPostingChain.java`、`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/report/TestErpHrReportRendering.java`（successor M3.3）
- backward-214 → master-data（1），引用 1 文件：`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/report/TestErpHrReportRendering.java`（successor M3.3）
- backward-215 → notify（2），引用 2 文件：`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrSalaryPostingChain.java`、`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/job/TestErpHrLeaveApproverTimeoutJob.java`（successor M3.3）

### 6.10 inventory（位次 10，M2.2）

**A. 前向义务（作为早域）**

A2 main 临时桥接（9 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-071 | `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostAdjustmentService.java:13` | ErpMfgCostRollup（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-072 | `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostAdjustmentService.java:14` | ErpMfgCostRollupLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-073 | `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/StandardCostResolver.java:7` | ErpMfgCostRollup（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-074 | `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/StandardCostResolver.java:8` | ErpMfgCostRollupLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-075 | `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostApproveProcessor.java:11` | ErpPurReceive（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-076 | `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostApproveProcessor.java:12` | ErpPurReceiveLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-077 | `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostGenerateFreightLandedCostProcessor.java:5` | ErpPurReceive（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-078 | `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java:14` | ErpPurReceive（符号使用行含 id 语境（见 idContextLines）） | — | purchase（15） | M2.5 |
| bridge-main-079 | `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java:15` | ErpPurReceiveLine（符号使用行含 id 语境（见 idContextLines）） | — | purchase（15） | M2.5 |

A3 test 桥接（6 条；本域 plan Phase 3 修复本域测试时适配，随后退役）：

| id | 测试文件 | 引用（目标@行） | owner |
| --- | --- | --- | --- |
| bridge-test-119 | `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvCostAdjust.java` | ErpMfgCostRollup(manufacturing) @L13, ErpMfgCostRollupLine(manufacturing) @L14 | M2.2 |
| bridge-test-120 | `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvLandedCostEndToEnd.java` | ErpPurReceive(purchase) @L11, ErpPurReceiveLine(purchase) @L12 | M2.2 |
| bridge-test-121 | `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvLandedCostReversal.java` | ErpPurReceive(purchase) @L11, ErpPurReceiveLine(purchase) @L12 | M2.2 |
| bridge-test-122 | `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvStandardCosting.java` | ErpMfgCostRollup(manufacturing) @L10, ErpMfgCostRollupLine(manufacturing) @L11 | M2.2 |
| bridge-test-123 | `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/processor/TestErpInvLandedCostAllocatedGuard.java` | ErpPurReceive(purchase) @L6 | M2.2 |
| bridge-test-124 | `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/processor/TestErpInvLandedCostReceiveMutex.java` | ErpPurReceive(purchase) @L3 | M2.2 |

**B. 退役/翻转义务（作为晚域）**

- main 桥接退役（9 条）：本域 plan 翻转 IBiz 参数签名时退役对应条目并通知早域移除桥接点：bridge-main-009（早域 aps）、bridge-main-010（早域 aps）、bridge-main-011（早域 aps）、bridge-main-016（早域 aps）、bridge-main-062（早域 finance）、bridge-main-064（早域 finance）、bridge-main-066（早域 finance）、bridge-main-067（早域 finance）、bridge-main-068（早域 finance）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（3 条，编译器驱动修复清单）：
- backward-157 → finance（6），引用 4 文件：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/CostAdjustmentPostingDispatcher.java`、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvPostingExecutor.java`、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/LandedCostPostingDispatcher.java`、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvTransferOrderConfirmProcessor.java`（successor M2.2）
- backward-158 → master-data（1），引用 6 文件：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostAdjustmentService.java`、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostMethodResolver.java`、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/StandardCostResolver.java`、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java`、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockMoveProcessor.java`、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/spi/ErpInvSkuReferenceChecker.java`（successor M2.2）
- backward-159 → notify（2），引用 2 文件：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java`、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockTakeCompleteTakeProcessor.java`（successor M2.2）

C2 后向 test（3 条，本域测试适配清单）：
- backward-216 → finance（6），引用 12 文件：`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvAcctDocProviderAccountKey.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvCostAdjust.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvFifoCostingEndToEnd.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvFinanceReversalWriteback.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvLandedCostEndToEnd.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvLandedCostReversal.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvOwnershipTransfer.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvPosting.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvStandardCosting.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvStockTakeCompleteDiffMove.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/perf/TestErpInvCostingReclosePerf.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/posting/TestErpInvPostingDispatcherFailureHangs.java`（successor M2.2）
- backward-217 → master-data（1），引用 17 文件：`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvBatchCosting.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvBatchExpiryInterception.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvCostAdjust.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvCostingDispatch.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvFifoCosting.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvFifoCostingEndToEnd.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvLandedCostEndToEnd.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvLandedCostReversal.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvLifoCosting.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvOwnershipTransfer.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvPosting.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvSkuReferenceChecker.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvSpecificCosting.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvStandardCosting.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvWeightedAverageCosting.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/dashboard/TestErpInvDashboard.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/perf/TestErpInvCostingReclosePerf.java`（successor M2.2）
- backward-218 → notify（2），引用 2 文件：`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvStockTakeCompleteDiffMove.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/processor/TestErpInvLandedCostReverseFailureAlert.java`（successor M2.2）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：drp(main) 5 文件、logistics(main) 1 文件、maintenance(main) 5 文件、manufacturing(main) 14 文件、purchase(main) 6 文件、quality(main) 3 文件、sales(main) 7 文件、drp(test) 9 文件、logistics(test) 1 文件、maintenance(test) 4 文件、manufacturing(test) 15 文件、purchase(test) 8 文件、quality(test) 3 文件、sales(test) 10 文件。

### 6.11 maintenance（位次 11，M3.2）

**A. 前向义务（作为早域）**

A2 main 临时桥接（6 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-080 | `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/OeeCalculator.java:3` | ErpMfgJobCard（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-081 | `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/OeeCalculator.java:4` | ErpMfgJobCardTimeLog（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-082 | `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/OeeCalculator.java:5` | ErpMfgWorkOrder（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-083 | `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/OeeCalculator.java:6` | ErpMfgWorkcenterCalendar（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-084 | `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/OeeCalculator.java:7` | ErpMfgWorkcenterCapacity（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | manufacturing（14） | M3.1 |
| bridge-main-085 | `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/OeeCalculator.java:10` | ErpQaInspection（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | quality（13） | M2.3 |

A3 test 桥接（1 条；本域 plan Phase 3 修复本域测试时适配，随后退役）：

| id | 测试文件 | 引用（目标@行） | owner |
| --- | --- | --- | --- |
| bridge-test-125 | `module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntOee.java` | ErpMfgJobCard(manufacturing) @L3, ErpMfgJobCardTimeLog(manufacturing) @L4, ErpMfgWorkOrder(manufacturing) @L5, ErpMfgWorkcenterCalendar(manufacturing) @L6, ErpMfgWorkcenterCapacity(manufacturing) @L7, ErpQaInspection(quality) @L13 | M3.2 |

**B. 退役/翻转义务（作为晚域）**

- main 桥接退役（2 条）：本域 plan 翻转 IBiz 参数签名时退役对应条目并通知早域移除桥接点：bridge-main-024（早域 assets）、bridge-main-025（早域 assets）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（4 条，编译器驱动修复清单）：
- backward-164 → finance（6），引用 3 文件：`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceIssuePostingDispatcher.java`、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborPostingDispatcher.java`、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MntPostingExecutor.java`（successor M3.2）
- backward-165 → inventory（10），引用 5 文件：`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceIssuePostingDispatcher.java`、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/AbstractErpMntSparePartUsageProcessor.java`、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntSparePartUsageConfirmProcessor.java`、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/ErpMntSparePartUsageReverseConfirmProcessor.java`、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/SparePartIssueService.java`（successor M3.2）
- backward-166 → master-data（1），引用 2 文件：`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceIssuePostingDispatcher.java`、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborPostingDispatcher.java`（successor M3.2）
- backward-167 → notify（2），引用 3 文件：`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceIssuePostingDispatcher.java`、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborPostingDispatcher.java`、`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/processor/AbstractErpMntDowntimeEntryProcessor.java`（successor M3.2）

C2 后向 test（4 条，本域测试适配清单）：
- backward-225 → finance（6），引用 7 文件：`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntAcctDocProviderAccountKey.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntDowntimeAndE2E.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntLaborPosting.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartAndSchedule.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartPosting.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartUsageReversal.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntVisitCancelReversal.java`（successor M3.2）
- backward-226 → inventory（10），引用 4 文件：`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntDowntimeAndE2E.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartAndSchedule.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartPosting.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartUsageReversal.java`（successor M3.2）
- backward-227 → master-data（1），引用 6 文件：`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntDowntimeAndE2E.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntLaborPosting.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartAndSchedule.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartPosting.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartUsageReversal.java`、`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntVisitCancelReversal.java`（successor M3.2）
- backward-228 → notify（2），引用 1 文件：`module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntDowntimeSchedulingLinkage.java`（successor M3.2）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：manufacturing(main) 1 文件、manufacturing(test) 1 文件。

### 6.12 projects（位次 12，M2.7）

**A. 前向义务（作为早域）**

- 无（本域不引用任何晚域）。

**B. 退役/翻转义务（作为晚域）**

- orm 列延后退役（8 条）：orm-deferral-001, orm-deferral-002, orm-deferral-003, orm-deferral-004, orm-deferral-005, orm-deferral-006, orm-deferral-007, orm-deferral-008——本域 orm 翻转时同批翻转早域延后列并退役条目（显式拥有早域链修复责任）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（4 条，编译器驱动修复清单）：
- backward-174 → assets（7），引用 1 文件：`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementProcessor.java`（successor M2.7）
- backward-175 → finance（6），引用 3 文件：`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/cost/ExpenseCostAggregator.java`、`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectPostingExecutor.java`、`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementProcessor.java`（successor M2.7）
- backward-176 → master-data（1），引用 1 文件：`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/TimesheetPostingDispatcher.java`（successor M2.7）
- backward-177 → notify（2），引用 1 文件：`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/TimesheetPostingDispatcher.java`（successor M2.7）

C2 后向 test（4 条，本域测试适配清单）：
- backward-235 → assets（7），引用 1 文件：`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjProjectSettlement.java`（successor M2.7）
- backward-236 → finance（6），引用 10 文件：`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjAcctDocProviderAccountKey.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjBudgetAndCollection.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjExpenseAggregation.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjMaterialAggregation.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjPnlCalcJob.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjProjectPnl.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjProjectSettlement.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjProjectSettlementRetention.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjTimesheetCost.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjTimesheetMulticurrencyPosting.java`（successor M2.7）
- backward-237 → master-data（1），引用 10 文件：`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjBudgetAndCollection.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjCostRateTier.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjExpenseAggregation.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjMaterialAggregation.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjPnlCalcJob.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjProjectPnl.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjProjectSettlement.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjProjectSettlementRetention.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjTimesheetCost.java`、`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjTimesheetMulticurrencyPosting.java`（successor M2.7）
- backward-238 → notify（2），引用 1 文件：`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/posting/TestTimesheetPostingFailureAlert.java`（successor M2.7）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：purchase(main) 1 文件、purchase(test) 1 文件。

### 6.13 quality（位次 13，M2.3）

**A. 前向义务（作为早域）**

A2 main 临时桥接（11 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-092 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/RecallTargetLocator.java:17` | ErpSalDelivery（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-093 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/RecallTargetLocator.java:141` | IErpSalDeliveryBiz.findFirst（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | sales（16） | M2.6 |
| bridge-main-094 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrReturnOrchestrator.java:6` | ErpPurReturn（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-095 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrReturnOrchestrator.java:11` | ErpSalReturn（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-096 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrReturnOrchestrator.java:99` | IErpPurReturnBiz.save（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | purchase（15） | M2.5 |
| bridge-main-097 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrReturnOrchestrator.java:117` | IErpSalReturnBiz.save（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | sales（16） | M2.6 |
| bridge-main-098 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaRecallGenerateReturnsProcessor.java:8` | ErpSalDelivery（符号使用行含 id 语境（见 idContextLines）） | — | sales（16） | M2.6 |
| bridge-main-099 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaRecallGenerateReturnsProcessor.java:9` | ErpSalDeliveryLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-100 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaRecallGenerateReturnsProcessor.java:10` | ErpSalReturn（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-101 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaRecallGenerateReturnsProcessor.java:55` | IErpSalDeliveryBiz.get（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | sales（16） | M2.6 |
| bridge-main-102 | `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaRecallGenerateReturnsProcessor.java:77` | IErpSalReturnBiz.save（方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准） | — | sales（16） | M2.6 |

A3 test 桥接（5 条；本域 plan Phase 3 修复本域测试时适配，随后退役）：

| id | 测试文件 | 引用（目标@行） | owner |
| --- | --- | --- | --- |
| bridge-test-128 | `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaRecallE2E.java` | ErpSalDelivery(sales) @L14, ErpSalDeliveryLine(sales) @L15 | M2.3 |
| bridge-test-129 | `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaRecallLocateNotifyReturn.java` | ErpSalDelivery(sales) @L13, ErpSalDeliveryLine(sales) @L14 | M2.3 |
| bridge-test-130 | `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestStubErpPurReturnBiz.java` | IErpPurReturnBiz(purchase) @L3, ErpPurReturn(purchase) @L4 | M2.3 |
| bridge-test-131 | `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestStubErpSalDeliveryBiz.java` | IErpSalDeliveryBiz(sales) @L3, ErpSalDelivery(sales) @L4 | M2.3 |
| bridge-test-132 | `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestStubErpSalReturnBiz.java` | IErpSalReturnBiz(sales) @L3, ErpSalReturn(sales) @L4 | M2.3 |

**B. 退役/翻转义务（作为晚域）**

- main 桥接退役（5 条）：本域 plan 翻转 IBiz 参数签名时退役对应条目并通知早域移除桥接点：bridge-main-057（早域 cs）、bridge-main-058（早域 cs）、bridge-main-059（早域 cs）、bridge-main-060（早域 cs）、bridge-main-085（早域 maintenance）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（3 条，编译器驱动修复清单）：
- backward-184 → finance（6），引用 2 文件：`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrPostingDispatcher.java`、`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrPostingExecutor.java`（successor M2.3）
- backward-185 → inventory（10），引用 3 文件：`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/RecallTargetLocator.java`、`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrPostingDispatcher.java`、`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrReturnOrchestrator.java`（successor M2.3）
- backward-186 → master-data（1），引用 1 文件：`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/report/ErpQaReportBizModel.java`（successor M2.3）

C2 后向 test（3 条，本域测试适配清单）：
- backward-246 → finance（6），引用 2 文件：`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaAcctDocProviderAccountKey.java`、`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaNcrPosting.java`（successor M2.3）
- backward-247 → inventory（10），引用 3 文件：`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaNcrPosting.java`、`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaRecallE2E.java`、`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaRecallLocateNotifyReturn.java`（successor M2.3）
- backward-248 → master-data（1），引用 4 文件：`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaNcrPosting.java`、`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaRecallE2E.java`、`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaRecallLocateNotifyReturn.java`、`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/report/TestErpQaReportRendering.java`（successor M2.3）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：drp(main) 2 文件、manufacturing(main) 2 文件、purchase(main) 2 文件、sales(main) 2 文件、drp(test) 2 文件、manufacturing(test) 1 文件、purchase(test) 1 文件、sales(test) 1 文件。

### 6.14 manufacturing（位次 14，M3.1）

**A. 前向义务（作为早域）**

A2 main 临时桥接（4 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-086 | `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/DemandAggregator.java:11` | ErpSalOrder（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-087 | `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/DemandAggregator.java:12` | ErpSalOrderLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | sales（16） | M2.6 |
| bridge-main-088 | `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/MrpReleaseService.java:13` | ErpPurOrder（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |
| bridge-main-089 | `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/MrpReleaseService.java:14` | ErpPurOrderLine（类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）） | — | purchase（15） | M2.5 |

A3 test 桥接（2 条；本域 plan Phase 3 修复本域测试时适配，随后退役）：

| id | 测试文件 | 引用（目标@行） | owner |
| --- | --- | --- | --- |
| bridge-test-126 | `module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMrpEndToEnd.java` | ErpPurOrder(purchase) @L9, ErpPurOrderLine(purchase) @L10, ErpSalOrder(sales) @L11, ErpSalOrderLine(sales) @L12 | M3.1 |
| bridge-test-127 | `module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMrpEngine.java` | ErpSalOrder(sales) @L10, ErpSalOrderLine(sales) @L11 | M3.1 |

**B. 退役/翻转义务（作为晚域）**

- main 桥接退役（20 条）：本域 plan 翻转 IBiz 参数签名时退役对应条目并通知早域移除桥接点：bridge-main-012（早域 aps）、bridge-main-013（早域 aps）、bridge-main-014（早域 aps）、bridge-main-015（早域 aps）、bridge-main-017（早域 aps）、bridge-main-018（早域 aps）、bridge-main-019（早域 aps）、bridge-main-020（早域 aps）、bridge-main-021（早域 aps）、bridge-main-022（早域 aps）、bridge-main-023（早域 aps）、bridge-main-071（早域 inventory）、bridge-main-072（早域 inventory）、bridge-main-073（早域 inventory）、bridge-main-074（早域 inventory）、bridge-main-080（早域 maintenance）、bridge-main-081（早域 maintenance）、bridge-main-082（早域 maintenance）、bridge-main-083（早域 maintenance）、bridge-main-084（早域 maintenance）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（6 条，编译器驱动修复清单）：
- backward-168 → finance（6），引用 2 文件：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/MfgPostingExecutor.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgSubcontractOrderProcessor.java`（successor M3.1）
- backward-169 → inventory（10），引用 14 文件：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/ErpMfgBatchGenealogyBizModel.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/MaterialIssueStockMoveBuilder.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/genealogy/BatchGenealogyWriter.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/DemandAggregator.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/MrpEngine.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ManufacturingIssuePostingDispatcher.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractPostingDispatcher.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/AbstractErpMfgMaterialIssueProcessor.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgMaterialIssueConfirmProcessor.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgMaterialIssueReverseConfirmProcessor.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgSubcontractOrderProcessor.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/simulation/SimulationMrpEngine.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/workorder/KitAvailabilityChecker.java`（successor M3.1）
- backward-170 → maintenance（11），引用 1 文件：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgScheduleToJobCardProcessor.java`（successor M3.1）
- backward-171 → master-data（1），引用 11 文件：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/costing/CostRollupService.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/MaterialIssueStockMoveBuilder.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/DemandAggregator.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/MrpEngine.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/ManufacturingIssuePostingDispatcher.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractPostingDispatcher.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgSubcontractOrderProcessor.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/simulation/SimulationMrpEngine.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/simulation/SimulationVersionComparator.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/spi/ErpMfgSkuReferenceChecker.java`（successor M3.1）
- backward-172 → notify（2），引用 4 文件：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/costing/ProductionVarianceCalculator.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/genealogy/BatchGenealogyWriter.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/SubcontractPostingDispatcher.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java`（successor M3.1）
- backward-173 → quality（13），引用 2 文件：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java`、`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderReportCompletionProcessor.java`（successor M3.1）

C2 后向 test（6 条，本域测试适配清单）：
- backward-229 → finance（6），引用 9 文件：`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgAcctDocProviderAccountKey.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgCompletionPosting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgCostFlowEndToEnd.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgIssuePosting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMaterialIssueReversal.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgProductionVariance.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgSubcontractReverse.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgSubcontracting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgVarianceRecomputeReversal.java`（successor M3.1）
- backward-230 → inventory（10），引用 15 文件：`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgBatchGenealogy.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgBomSnapshot.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgCompletionPosting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgCostFlowEndToEnd.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgForecastSource.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgIssuePosting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMaterialIssue.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMaterialIssueReversal.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMrpEngine.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMrpSimulation.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgReservationLifecycle.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgSubcontractReverse.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgSubcontracting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgWorkOrderEndToEnd.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgWorkOrderStateMachine.java`（successor M3.1）
- backward-231 → maintenance（11），引用 1 文件：`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgJobCardDowntimeGate.java`（successor M3.1）
- backward-232 → master-data（1），引用 24 文件：`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgBatchGenealogy.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgBomSnapshot.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgCompletionPosting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgCostFlowEndToEnd.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgCostRollup.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgForecastSource.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgIssuePosting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgJobCardDowntimeGate.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMaterialIssue.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMaterialIssueReversal.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMrpEndToEnd.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMrpEngine.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgMrpSimulation.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgProductionVariance.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgReservationLifecycle.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgScheduleToJobCard.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgSkuReferenceChecker.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgSubcontractReverse.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgSubcontracting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgVarianceAlert.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgVarianceRecomputeReversal.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgWorkOrderEndToEnd.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgWorkOrderStateMachine.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/report/TestErpMfgReportRendering.java`（successor M3.1）
- backward-233 → notify（2），引用 3 文件：`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgBatchGenealogy.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgSubcontracting.java`、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgVarianceAlert.java`（successor M3.1）
- backward-234 → quality（13），引用 1 文件：`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgWorkOrderCancelInspectionLinkage.java`（successor M3.1）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：drp(main) 1 文件、drp(test) 1 文件。

### 6.15 purchase（位次 15，M2.5）

**A. 前向义务（作为早域）**

A2 main 临时桥接（2 条；本域 plan 在调用点加 String→Long 桥 + grep 例外登记）：

| id | 调用点 | 目标符号 | Long 参数 | 晚域 | 退役 owner |
| --- | --- | --- | --- | --- | --- |
| bridge-main-090 | `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java:324` | IErpInvDrpCrossDockBiz.markReceivedFromPurchase | `inboundMoveId` | drp（18） | M3.7 |
| bridge-main-091 | `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java:378` | IErpInvDrpLeadTimeRecordBiz.recordFromPurchaseReceive | `supplierId` | drp（18） | M3.7 |

**B. 退役/翻转义务（作为晚域）**

- main 桥接退役（26 条）：本域 plan 翻转 IBiz 参数签名时退役对应条目并通知早域移除桥接点：bridge-main-026（早域 b2b）、bridge-main-027（早域 b2b）、bridge-main-028（早域 b2b）、bridge-main-029（早域 b2b）、bridge-main-030（早域 b2b）、bridge-main-031（早域 b2b）、bridge-main-033（早域 contract）、bridge-main-034（早域 contract）、bridge-main-037（早域 contract）、bridge-main-039（早域 contract）、bridge-main-040（早域 contract）、bridge-main-043（早域 contract）、bridge-main-044（早域 contract）、bridge-main-047（早域 contract）、bridge-main-049（早域 contract）、bridge-main-050（早域 contract）、bridge-main-069（早域 finance）、bridge-main-075（早域 inventory）、bridge-main-076（早域 inventory）、bridge-main-077（早域 inventory）、bridge-main-078（早域 inventory）、bridge-main-079（早域 inventory）、bridge-main-088（早域 manufacturing）、bridge-main-089（早域 manufacturing）、bridge-main-094（早域 quality）、bridge-main-096（早域 quality）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（6 条，编译器驱动修复清单）：
- backward-178 → contract（5），引用 1 文件：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/support/ErpPurCtDiscountApplier.java`（successor M2.5）
- backward-179 → finance（6），引用 6 文件：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurPostingExecutor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurInvoiceProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurOrderProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurPaymentProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnProcessor.java`（successor M2.5）
- backward-180 → inventory（10），引用 6 文件：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ReceiveStockMoveBuilder.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ReturnStockMoveBuilder.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveApproveProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnApproveProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnProcessor.java`（successor M2.5）
- backward-181 → master-data（1），引用 16 文件：`module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/IErpPurPaymentBiz.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/ScorecardStandingLinker.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/SupplierEligibilityChecker.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurPaymentBizModel.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/PaymentSettler.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ReceiveStockMoveBuilder.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ReturnStockMoveBuilder.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurInvoiceProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurOrderProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurPaymentProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurPaymentSettleProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/spi/ErpPurSkuReferenceChecker.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/support/ErpPurSupplierPriceResolver.java`（successor M2.5）
- backward-182 → projects（12），引用 1 文件：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java`（successor M2.5）
- backward-183 → quality（13），引用 2 文件：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveCancelProcessor.java`、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java`（successor M2.5）

C2 后向 test（7 条，本域测试适配清单）：
- backward-239 → contract（5），引用 1 文件：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurOrderCtDiscount.java`（successor M2.5）
- backward-240 → finance（6），引用 27 文件：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurBudgetControlIntegration.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurFinanceReversalWriteback.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurInvoiceCommitmentRestore.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurInvoicePosting.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurMultiCurrencyPosting.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurOrderCommitment.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurOrderToReceiveEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPaymentApproval.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPaymentApprovalNotifications.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPaymentWorkflowApproval.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPriceVariancePosting.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurProcureToPayEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveMaterialCostAggregation.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveOverReceiptTolerance.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveStockMove.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnApproval.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnCommitmentRelease.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnCommitmentRestore.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnInventory.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnPosting.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnQty.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnRefundEndToEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnTrace.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurToInvToFinPostingEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/dashboard/TestErpPurDashboard.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/posting/TestErpPurPostingDispatcherFailureHangs.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/posting/TestPurReversalListenerReceiveRollback.java`（successor M2.5）
- backward-241 → inventory（10），引用 8 文件：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurOrderToReceiveEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurProcureToPayEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveStockMove.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnInventory.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnPosting.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnRefundEndToEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnTrace.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurToInvToFinPostingEnd.java`（successor M2.5）
- backward-242 → master-data（1），引用 38 文件：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurBudgetControlIntegration.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurFinanceReversalWriteback.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurInvoiceApproval.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurInvoiceCommitmentRestore.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurInvoicePosting.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurMultiCurrencyPosting.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurOrderApproval.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurOrderCommitment.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurOrderCtDiscount.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurOrderToReceiveEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPaymentApproval.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPaymentApprovalNotifications.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPaymentSettlement.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPaymentWorkflowApproval.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPriceVariancePosting.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurProcureToPayEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurQuotationRfqReverseApprove.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveApproval.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveCancelInspectionLinkage.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveMaterialCostAggregation.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveOverReceiptTolerance.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveStockMove.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurRequisitionToOrderEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnApproval.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnCommitmentRelease.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnCommitmentRestore.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnInventory.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnPosting.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnQty.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnRefundEndToEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnTrace.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurScorecardLinkage.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurSettleThreeWayMatchRecheck.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurSkuReferenceChecker.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurSupplierPriceResolver.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurThreeWayMatch.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurToInvToFinPostingEnd.java`、`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/dashboard/TestErpPurDashboard.java`（successor M2.5）
- backward-243 → notify（2），引用 1 文件：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPaymentApprovalNotifications.java`（successor M2.5）
- backward-244 → projects（12），引用 1 文件：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveMaterialCostAggregation.java`（successor M2.5）
- backward-245 → quality（13），引用 1 文件：`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveCancelInspectionLinkage.java`（successor M2.5）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：drp(main) 3 文件、drp(test) 3 文件、logistics(test) 1 文件。

### 6.16 sales（位次 16，M2.6）

**A. 前向义务（作为早域）**

- 无（本域不引用任何晚域）。

**B. 退役/翻转义务（作为晚域）**

- main 桥接退役（23 条）：本域 plan 翻转 IBiz 参数签名时退役对应条目并通知早域移除桥接点：bridge-main-032（早域 b2b）、bridge-main-035（早域 contract）、bridge-main-036（早域 contract）、bridge-main-038（早域 contract）、bridge-main-041（早域 contract）、bridge-main-042（早域 contract）、bridge-main-045（早域 contract）、bridge-main-046（早域 contract）、bridge-main-048（早域 contract）、bridge-main-051（早域 contract）、bridge-main-052（早域 contract）、bridge-main-070（早域 finance）、bridge-main-086（早域 manufacturing）、bridge-main-087（早域 manufacturing）、bridge-main-092（早域 quality）、bridge-main-093（早域 quality）、bridge-main-095（早域 quality）、bridge-main-097（早域 quality）、bridge-main-098（早域 quality）、bridge-main-099（早域 quality）、bridge-main-100（早域 quality）、bridge-main-101（早域 quality）、bridge-main-102（早域 quality）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（6 条，编译器驱动修复清单）：
- backward-187 → contract（5），引用 1 文件：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/support/ErpSalCtDiscountApplier.java`（successor M2.6）
- backward-188 → finance（6），引用 6 文件：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/posting/SalPostingExecutor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalInvoiceProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalOrderProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnProcessor.java`（successor M2.6）
- backward-189 → inventory（10），引用 7 文件：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/DeliveryStockMoveBuilder.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReturnCostStrategyResolver.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReturnStockMoveBuilder.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryApproveProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalOrderProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnProcessor.java`（successor M2.6）
- backward-190 → master-data（1），引用 19 文件：`module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/IErpSalReceiptBiz.java`、`module-sales/erp-sal-dao/src/main/java/app/erp/sal/dao/entity/ErpSalPriceList.java`、`module-sales/erp-sal-dao/src/main/java/app/erp/sal/dao/entity/ErpSalPriceListLine.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/DeliveryStockMoveBuilder.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalOrderBizModel.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalPricingRuleBizModel.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalReceiptBizModel.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReceiptSettler.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ReturnStockMoveBuilder.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalInvoiceProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalOrderProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReceiptProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReceiptSettleProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/spi/ErpSalSkuReferenceChecker.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/support/ErpSalCustomerPriceResolver.java`（successor M2.6）
- backward-191 → notify（2），引用 1 文件：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java`（successor M2.6）
- backward-192 → quality（13），引用 2 文件：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryCancelProcessor.java`、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalDeliveryProcessor.java`（successor M2.6）

C2 后向 test（6 条，本域测试适配清单）：
- backward-249 → contract（5），引用 1 文件：`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderCtDiscount.java`（successor M2.6）
- backward-250 → finance（6），引用 26 文件：`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalAcctDocProviderAccountKey.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalCreditHoldOnDelivery.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalCreditHoldOnInvoice.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalDeliveryStockMove.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalFinanceReversalWriteback.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalInvoicePosting.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalMultiCurrencyReconFx.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderCommitment.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderToCashEnd.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderToDeliveryEnd.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReceiptApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReceiptWorkflowApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnCompliance.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnCostAndGuards.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnExchange.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnInventory.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnPosting.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnQty.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnRefund.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnRefundEndToEnd.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnTrace.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/dashboard/TestErpSalDashboard.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/posting/TestErpSalPostingDispatcherFailureHangs.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/posting/TestSalReversalListenerRollback.java`（successor M2.6）
- backward-251 → inventory（10），引用 10 文件：`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalCreditHoldOnDelivery.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalDeliveryStockMove.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderAvailabilityCheck.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderToCashEnd.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderToDeliveryEnd.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnCostAndGuards.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnExchange.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnInventory.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnRefundEndToEnd.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnTrace.java`（successor M2.6）
- backward-252 → master-data（1），引用 34 文件：`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalContractReverseApprove.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalCreditHoldOnDelivery.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalCreditHoldOnInvoice.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalCreditNotify.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalDeliveryApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalDeliveryCancelInspectionLinkage.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalDeliveryStockMove.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalFinanceReversalWriteback.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalInvoiceApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalInvoicePosting.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalMultiCurrencyReconFx.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderAvailabilityCheck.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderCommitment.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderCtDiscount.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderToCashEnd.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderToDeliveryEnd.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalPricingCompliance.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalQuotationToOrder.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReceiptApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReceiptSettlement.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReceiptWorkflowApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnApproval.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnCompliance.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnCostAndGuards.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnExchange.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnInventory.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnPosting.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnQty.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnRefund.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnRefundEndToEnd.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnTrace.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalSkuReferenceChecker.java`、`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/dashboard/TestErpSalDashboard.java`（successor M2.6）
- backward-253 → notify（2），引用 1 文件：`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalCreditNotify.java`（successor M2.6）
- backward-254 → quality（13），引用 1 文件：`module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalDeliveryCancelInspectionLinkage.java`（successor M2.6）

被引用清单（其他域引用本域，供冲击面评估；引用方 plan 各自消费）：crm(main) 8 文件、drp(main) 1 文件、logistics(main) 1 文件、crm(test) 2 文件、drp(test) 1 文件、logistics(test) 1 文件。

### 6.17 crm（位次 17，M3.4）

**A. 前向义务（作为早域）**

- 无（本域不引用任何晚域）。

**B. 退役/翻转义务（作为晚域）**

- main 桥接退役（4 条）：本域 plan 翻转 IBiz 参数签名时退役对应条目并通知早域移除桥接点：bridge-main-053（早域 cs）、bridge-main-054（早域 cs）、bridge-main-055（早域 cs）、bridge-main-056（早域 cs）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（3 条，编译器驱动修复清单）：
- backward-141 → master-data（1），引用 5 文件：`module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmConversionBiz.java`、`module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmLeadBiz.java`、`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/entity/ErpCrmLeadBizModel.java`、`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmConversionConvertToCustomerProcessor.java`、`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmConversionProcessor.java`（successor M3.4）
- backward-142 → notify（2），引用 2 文件：`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/job/ErpCrmEventReminderJob.java`、`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/job/ErpCrmSequenceOverdueJob.java`（successor M3.4）
- backward-143 → sales（16），引用 8 文件：`module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmConversionBiz.java`、`module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmLeadBiz.java`、`module-crm/erp-crm-dao/src/main/java/app/erp/crm/biz/IErpCrmProductConfiguratorBiz.java`、`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/entity/ErpCrmLeadBizModel.java`、`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/entity/ErpCrmProductConfiguratorBizModel.java`、`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmConversionConvertToQuotationProcessor.java`、`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmConversionProcessor.java`、`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmProductConfiguratorGenerateQuoteProcessor.java`（successor M3.4）

C2 后向 test（2 条，本域测试适配清单）：
- backward-201 → master-data（1），引用 3 文件：`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/TestErpCrmConversionGuards.java`、`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/TestErpCrmCpqGenerateQuote.java`、`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/TestErpCrmLeadConversion.java`（successor M3.4）
- backward-202 → sales（16），引用 2 文件：`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/TestErpCrmCpqGenerateQuote.java`、`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/TestErpCrmLeadConversion.java`（successor M3.4）

### 6.18 drp（位次 18，M3.7）

**A. 前向义务（作为早域）**

- 无（本域不引用任何晚域）。

**B. 退役/翻转义务（作为晚域）**

- main 桥接退役（2 条）：本域 plan 翻转 IBiz 参数签名时退役对应条目并通知早域移除桥接点：bridge-main-090（早域 purchase）、bridge-main-091（早域 purchase）。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（6 条，编译器驱动修复清单）：
- backward-146 → inventory（10），引用 5 文件：`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/drp/DrpDemandAggregator.java`、`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/drp/DrpReleaseService.java`、`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/job/ErpDrpCrossDockStagingTimeoutJob.java`、`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpCrossDockProcessor.java`、`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/safetystock/SafetyStockEngine.java`（successor M3.7）
- backward-147 → manufacturing（14），引用 1 文件：`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/drp/DrpDemandAggregator.java`（successor M3.7）
- backward-148 → master-data（1），引用 3 文件：`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/drp/DrpReleaseService.java`、`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/job/ErpDrpCrossDockStagingTimeoutJob.java`、`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpCrossDockProcessor.java`（successor M3.7）
- backward-149 → purchase（15），引用 3 文件：`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/drp/DrpDemandAggregator.java`、`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/drp/DrpReleaseService.java`、`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpLeadTimeProcessor.java`（successor M3.7）
- backward-150 → quality（13），引用 2 文件：`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpCrossDockProcessor.java`、`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpLeadTimeProcessor.java`（successor M3.7）
- backward-151 → sales（16），引用 1 文件：`module-drp/erp-drp-service/src/main/java/app/erp/drp/service/processor/ErpInvDrpCrossDockProcessor.java`（successor M3.7）

C2 后向 test（6 条，本域测试适配清单）：
- backward-205 → inventory（10），引用 9 文件：`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpCrossDock.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpEngine.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpForecastSource.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpInventoryIntegration.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpLeadTimeStats.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpSafetyStock.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpScheduleRelease.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpSimulation.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpWiringRegression.java`（successor M3.7）
- backward-206 → manufacturing（14），引用 1 文件：`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpForecastSource.java`（successor M3.7）
- backward-207 → master-data（1），引用 9 文件：`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpCrossDock.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpEngine.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpForecastSource.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpInventoryIntegration.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpLeadTimeStats.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpSafetyStock.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpScheduleRelease.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpSimulation.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpWiringRegression.java`（successor M3.7）
- backward-208 → purchase（15），引用 3 文件：`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpEngine.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpLeadTimeStats.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpScheduleRelease.java`（successor M3.7）
- backward-209 → quality（13），引用 2 文件：`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpCrossDock.java`、`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpLeadTimeStats.java`（successor M3.7）
- backward-210 → sales（16），引用 1 文件：`module-drp/erp-drp-service/src/test/java/app/erp/drp/service/TestErpDrpCrossDock.java`（successor M3.7）

### 6.19 logistics（位次 19，M3.10）

**A. 前向义务（作为早域）**

- 无（本域不引用任何晚域）。

**B. 退役/翻转义务（作为晚域）**

- 无。

**C. plan 起草消费集（本域 plan Phase 1/2/3 定位面）**

C1 后向 main（4 条，编译器驱动修复清单）：
- backward-160 → finance（6），引用 1 文件：`module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/AbstractErpLogShipmentDeliveredProcessor.java`（successor M3.10）
- backward-161 → inventory（10），引用 1 文件：`module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/AbstractErpLogShipmentDeliveredProcessor.java`（successor M3.10）
- backward-162 → notify（2），引用 3 文件：`module-logistics/erp-log-service/src/main/java/app/erp/log/service/gateway/GatewayDispatcher.java`、`module-logistics/erp-log-service/src/main/java/app/erp/log/service/job/ErpLogDraftEscalationJob.java`、`module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/AbstractErpLogShipmentDeliveredProcessor.java`（successor M3.10）
- backward-163 → sales（16），引用 1 文件：`module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/AbstractErpLogShipmentDeliveredProcessor.java`（successor M3.10）

C2 后向 test（6 条，本域测试适配清单）：
- backward-219 → finance（6），引用 4 文件：`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogFreightPosting.java`、`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogShipmentPostingEnd.java`、`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogTrackingPollJob.java`、`module-logistics/erp-log-service/src/test/java/app/erp/log/service/processor/TestErpLogSalesDeliveryLinkage.java`（successor M3.10）
- backward-220 → inventory（10），引用 1 文件：`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogPath2LandedCost.java`（successor M3.10）
- backward-221 → master-data（1），引用 3 文件：`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogFreightPosting.java`、`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogShipmentPostingEnd.java`、`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogTrackingPollJob.java`（successor M3.10）
- backward-222 → notify（2），引用 1 文件：`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogDraftEscalationJob.java`（successor M3.10）
- backward-223 → purchase（15），引用 1 文件：`module-logistics/erp-log-service/src/test/java/app/erp/log/service/TestErpLogPath2LandedCost.java`（successor M3.10）
- backward-224 → sales（16），引用 1 文件：`module-logistics/erp-log-service/src/test/java/app/erp/log/service/processor/TestErpLogSalesDeliveryLinkage.java`（successor M3.10）

## 7. 特殊行（无登记条目的冻结序成员与共享模块）

| 成员 | 位次/角色 | 工作项 | 条目 | 说明 |
| --- | --- | --- | --- | --- |
| master-data | 1（已迁移） | M1.1 done | 0 | 根域；迁移完成，prj-dao 27 错/15 文件 + fin-dao 97 错/32 文件 `_gen` 中间态已登记（successor M2.7/M2.1 + M4.1 兜底），非本登记册条目（属 M1.1 plan 登记范围）。被 15 域 main（114 文件）/ 16 域 test（232 文件）引用（见各域 C 段 refDomain=master-data 行）。 |
| notify | 2 | M1.2 | 0 | 根域：零前向义务（无 orm 延后、零 main/test 桥接）+ 零晚域退役义务；被 14 域 main（43 文件）/ 14 域 test（28 文件）引用（backward refDomain=notify 共 28 条）。M1.2 消费结论见计划 Phase 3 走查记录。 |
| common-service | 共享模块 | M1.3 done | 0 | orgId String 语义已落（`ErpOrgContext` 等 3 文件）；扫描范围内无跨域前向边登记条目；`TestErpOrgIsolation` 编译破坏已按中间态登记（successor M2.1）。 |
| common-test | 共享模块 | — | 0 | seq-string Proof 载体（`TestSeqStringIdProof`）；无迁移工作项、无登记条目。 |

## 8. 复核与再生成

- 对账复核：`node tools/scan-id-coupling-directions.mjs --orm-graph=docs/audits/2026-08-21-1657-id-m02-orm-graph.json --directions=docs/audits/2026-08-21-1657-id-m02-coupling-directions.json --registry-out=<临时路径>`（**勿直接覆盖登记册**——会重置 status；退役后登记册为手工维护权威）。
- 门控口径：`node tools/check-bigint-id-types.mjs scan` = 1586 NEEDS FIX + 8 DEFERRED(registry) + md 段 0；`node tools/verify-id-fix-copy-diff.mjs` 变更行计数排除延后列、延后列差异行 = FAIL。

