# Playwright E2E 冒烟回归运行手册

## 概述

本手册指导如何运行 `nop-app-erp` 的 Playwright E2E 冒烟回归套件，覆盖 10 域看板 + 24 域报表页面 + 18 域 CRUD 列表/表单页 + 1 KB 建议定向冒烟 + 28 个数据驱动数值断言 spec + 13 域 CRUD 数据驱动列表断言 spec + 4 域 CRUD 写路径 spec（master-data GraphQL 层 + master-data AMIS 表单层 + quality GraphQL 层 + maintenance GraphQL 层）+ 6 代表域业务动作 spec（inventory StockMove 状态机+过账 / CRM Lead 状态迁移 / CS Ticket 六态状态机 / maintenance Visit 5 态+设备联动副作用 / projects Task 4 态+DAG 门控 / quality CAPA 3 态 + NCR 无 CAPA 路径，经 GraphQL 调自定义 `@BizMutation`）+ 2 跨域编排链 spec（P2P PO→Receive→Invoice / O2C SO→Delivery→Invoice 全链审批+过账产物，经 GraphQL 驱动）+ 1 看板 AMIS 前端渲染层 spec（10 域，`dashboards.visual.spec.ts`，DOM 结构 + echarts canvas + 2 非参数化域数值 token）+ 1 报表 AMIS 前端渲染层 spec（4 域，`reports.visual.spec.ts`，AMIS service reload 注入 renderHtml 响应），共 131 测试。

测试层级：**冒烟级**（页面 DOM 渲染 + 关键元素存在 + GraphQL `/graphql` 请求返回 200 + 无未捕获 console error）。非像素级视觉回归。

在冒烟级之上，核心域（finance/sales/purchase）+ 运营域（inventory/assets/projects）+ 扩展域（manufacturing/maintenance/quality）+ 扩展域 CRM/客服/人力（纯报表域，无看板）+ 主数据域（master-data，看板 KPI + 预警 + 2 报表）已叠加**数据驱动数值断言层**（`*.value.spec.ts`）：直接经 GraphQL query 取后端聚合原始值，断言匹配确定性期望值。详见下方「数据驱动数值断言层」。

在数值断言层之上，13 域代表性实体（有 seed 行的）叠加了**CRUD 数据驱动列表断言层**（`*.list-value.spec.ts`）：经 GraphQL `ErpXxx__findPage` 查询断言列表返回 seed 行（行数 ≥ seed + 关键字段 token）。master-data ErpMdPartner 额外叠加**CRUD 写路径验证**（GraphQL 层 `master-data.write.spec.ts` + AMIS 表单层 `master-data.write.amis.spec.ts`）：create→get 验证→update→get 验证→delete→get 验证全链，断言写操作持久化。quality `ErpQaRiskRegister` + maintenance `ErpMntEquipmentCategory` 叠加 GraphQL 层写路径（`quality.write.spec.ts` / `maintenance.write.spec.ts`）。详见下方「CRUD 数据驱动列表断言层 + 写路径验证」。

## 前置条件

1. **JDK 17+**（项目使用 Java 25）
2. **Maven 3.9+**
3. **Node.js 18+**（项目使用 Node 25）
4. **Google Chrome**（系统安装，Playwright `channel: 'chrome'`）
   - 若已安装 Playwright bundled chromium，config 会自动检测并优先使用
5. **预构建 runner jar**：`mvn clean install -DskipTests` 产出 `app-erp-all/target/quarkus-app/quarkus-run.jar`

## 安装依赖

```bash
# 根目录执行一次
npm install
```

## 启动方式

### 方式 A：自动启动（推荐首次）

Playwright `webServer` 自动启动 Quarkus 应用：

```bash
npx playwright test
```

webServer 命令含测试专用 JVM 参数：
- `-Dnop.auth.service-public=true` — 服务端认证旁路（sys 用户上下文）
- `-Dnop.auth.login.allow-create-default-user=true` — 自动创建测试用户 `nop`/`123`
- `-Dnop.orm.init-database-data=true` — 部署期种子数据初始化（21 张主数据表 + 23 张交易单据表 + 13 张运营域表 + 4 张制造域表 + 11 张维护+质量域表 + 12 张 CRM/CS/HR 域表 + 3 张质量域 SPC 表 + 4 张制造域工作中心配置链+crp_load 表，详见下方「种子库启动」）
- `rm -f db/erp.mv.db` — fresh-DB 重置（seed 非幂等，每次启动前清库）
- 页面模型校验保持默认开启（`nop.web.validate-page-model=true`，application.yaml 默认值）——`ErpCsTicket.view.xml`/`ErpHrEmployee.view.xml` layout 缺陷已修复（见 `docs/bugs/`），启动期页面校验安全网已恢复

### 方式 B：复用已运行实例（开发调试推荐）

先手动启动应用：

```bash
lsof -ti :8080 | xargs kill -9 2>/dev/null  # 清理端口
java -Dfile.encoding=UTF8 \
     -Dnop.auth.service-public=true \
     -Dnop.auth.login.allow-create-default-user=true \
     -jar app-erp-all/target/quarkus-app/quarkus-run.jar
```

然后运行测试（跳过 webServer）：

```bash
SKIP_WEBSERVER=1 npx playwright test
```

### 种子库启动（演示 / 数据可见性）

默认 webServer 命令（方式 A）已含 **部署期种子数据初始化**：

- `-Dnop.orm.init-database-data=true` — 激活平台 `DataInitInitializer`（条件 bean，仅此 JVM 属性开启时实例化），从 `_vfs/_init-data/*.csv` 按拓扑序插入 **91 张 CSV**：
  - **21 张核心主数据表**（1234-1）：组织/币种/计量单位/物料/SKU/往来单位/仓库/员工/科目体系/税率/结算方式等（科目体系：8 基础科目 + 1249-1 补齐 5 过账科目 1401/1403/1131/2221/6401，使 Pur/Sal/Inv 过账 Provider 硬编码科目码经 `findByCode` 可达，解除过账优雅降级）
  - **23 张业务交易单据表**（1445-1）：P2P（PO/Receive/Invoice/Payment 头+行）+ O2C（SO/Delivery/Invoice/Receipt 头+行）+ 已过账财务产物（凭证/凭证行/凭证回链/AR-AP 辅助账/GL 余额/会计期间 OPEN）
  - **13 张运营域表**（2210-1）：库存（stock_move+line/stock_balance/cost_layer）+ 资产（asset_category/asset/depreciation_schedule）+ 项目（project_type/project/cost_collection/timesheet/budget/project_pnl）；三域看板读域表非 GL，posted 统一 false
  - **4 张制造域表**（2026-07-09-0930-1）：work_order（4 行 IN_PROCESS/STOCK_PARTIAL/COMPLETED×2）+ cost_variance + forecast(APPROVED) + forecast_line；看板 4 `@BizQuery` + production-variance/forecast-variance 报表读域表非 GL，posted 统一 false；crp_load 因 workcenter 配置链依赖归 Deferred
  - **11 张维护+质量域表**（2026-07-09-0930-2）：维护域 8 表（equipment_category/equipment/schedule/request/downtime_entry/visit/visit_task/spare_part_usage）+ 质量域 3 表（inspection/non_conformance/action）；看板 `getDashboardKpi` + 3 预警（findEquipmentDowntimeAlert/findMaintenanceOverdueAlert/findCapaOverdueAlert）+ 4 报表（maintenance-history/downtime-summary/inspection-summary/ncr-capa-summary）读域表非 GL，posted 统一 false；SPC 三表曾因 spc_chart.parameterId 配置链依赖归 Deferred，**已于 2026-07-09-1145-2 落地**（见下方 1145-2 行）
  - **12 张 CRM/CS/HR 域表 + 2 处既有 CSV 加性追加**（2026-07-09-1045-1）：CRM 5 表（stage/lead/forecast_period/forecast/forecast_line）+ CS 3 表（ticket_type/ticket/survey）+ HR 4 表（department/employee/salary_simulation/salary_simulation_item_adj）+ `erp_md_partner` 追加 1 行 EMPLOYEE 类型 + `erp_fin_ar_ap_item` 追加 2 行 EMPLOYEE_ADVANCE/EXPENSE_CLAIM·OPEN；三域为纯报表域（无看板 BizModel），5 报表（CRM lead-conversion-funnel/forecast-accuracy、CS ticket-sla-csat-summary、HR payroll-simulation-comparison/employee-net-balance）读域表非 GL，无 posted 列；HR employee-net-balance 经跨域 finance/master-data 加性追加驱动；CRM/CS/HR 配置表 + GL 凭证归 Deferred
  - **3 张质量域 SPC 表 + 1 处既有 CSV 加性追加**（2026-07-09-1145-2）：spc_chart（1 行，parameterId=0 占位软引用）+ spc_sample（1 行 isOutOfControl=true）+ spc_capability（1 行 capabilityLevel=INADEQUATE）+ `erp_qa_non_conformance` 追加 1 行 sourceType=SPC·status=OPEN；质量看板 `getSpcOutOfControlWarning` 三计数器（outOfControlChartCount/inadequateCapabilityCount/openSpcNcrCount）读 SPC 域表非 GL，由确定性 0 转非空可观测（解除 0930-2/0930-3 SPC Deferred）；Strategy C 完整参照完整性（sample/capability.chartId 指向真实 chart 行）；SPC 引擎双层门控默认关，seed 静态结果行不被重算覆盖；SPC 控制图可视化/ErpQaParameter 物化/SPC 引擎重算链归 Deferred
  - **4 张制造域工作中心配置链 + crp_load 表**（2026-07-09-0628-1）：workcenter（1 行 WC-001 主装配线）+ workcenter_calendar（1 行 单班 08:00~16:00 ALL_WEEK IS_ACTIVE=true）+ workcenter_capacity（1 行 efficiencyFactor=1 IS_ACTIVE=true）+ crp_load（1 行 loadDate=2026-07-15 loadHours=4 workOrderId→0930-1 WO-1）；CRP 负荷报表（crp-load-report）经 `CrpLoadCalculator.getLoadReport` 读 crp_load+workcenter+calendar+capacity 非空可观测（capacityHours=8.00 / loadRate=0.50 确定性派生，解除 0930-1 crp_load Deferred）；Strategy C 完整参照完整性（calendar/capacity/crp_load.workcenterId 指向真实 workcenter 行）；CRP 重算链经 nop-job 双层门控默认关，seed 静态 crp_load 行不被重算覆盖；CRP 前端可视化/crp_load 重算链归 Deferred
- `rm -f db/erp.mv.db db/erp.trace.db` — **fresh-DB 重置**。`DataInitInitializer` 非幂等（无存在性检查），持久 H2 文件库重复启动会主键冲突；故每次 webServer 启动前删除 db 文件，确保 seed 在空表上插入。交易种子加入后该行为不变（仅 CSV 增多，webServer JVM 参数与重置逻辑无改动）。
- **序列推进（0814-1）**：`_init-data/zz-sequence-advance.sql` 在所有 CSV 加载后执行（按文件名排序），`MERGE INTO NOP_SYS_SEQUENCE` 创建 default 序列行 `NEXT_VALUE=100000`（> 种子显式 id 上限 8）。因 `SysSequenceGenerator.lazyInit()` 是 delay-method（所有 bean 启动后才运行），`.sql` 执行时表为空，MERGE 创建行后 `addDefaultSequence()` 的 `if(!exists)` 守卫跳过，advanced 值保留。消除 GraphQL/AMIS 表单 create 首次主键碰撞（首 save id≥100000）。详见 `docs/architecture/seed-data.md`「部署期序列推进修复」。

手动启动种子库（方式 B）：

```bash
lsof -ti :8080 | xargs kill -9 2>/dev/null
rm -f db/erp.mv.db db/erp.trace.db   # 必需：非幂等，须 fresh-DB
java -Dfile.encoding=UTF8 \
     -Dnop.auth.service-public=true \
     -Dnop.auth.login.allow-create-default-user=true \
     -Dnop.orm.init-database-data=true \
     -jar app-erp-all/target/quarkus-app/quarkus-run.jar
```

- **生产安全**：生产 `application.yaml` 保持 `init-database-data` 缺省（`false`）；seed 仅经上述 JVM 属性 / webServer 触发。
- **种子范围**：核心主数据（bootstrap）+ P2P/O2C 最小连通交易单据集（含已过账财务产物）+ 运营域（库存/资产/项目）最小连通集 + 制造域最小连通集（work_order/cost_variance/forecast/forecast_line）+ 维护+质量域最小连通集（equipment/schedule/request/downtime_entry/visit/visit_task/spare_part_usage/inspection/non_conformance/action）+ CRM/CS/HR 最小连通集（stage/lead/forecast·line/ticket_type/ticket/survey/department/employee/salary_simulation·item_adj + 员工型 partner 追加 + ar_ap_item EMPLOYEE_ADVANCE/EXPENSE_CLAIM 追加）+ 质量域 SPC 最小连通集（spc_chart/spc_sample/spc_capability + non_conformance SPC 追加）+ 制造域工作中心配置链+crp_load 最小连通集（workcenter/workcenter_calendar/workcenter_capacity/crp_load）。核心域看板/报表/CRUD 列表经 GraphQL 证实数据非空；交易数据已使 finance/sales/purchase 域 KPI 数值非空（采购额/销售额/收入/净利润可观测）；运营域数据已使 inventory/assets/projects 域 KPI 非空（库存总值/资产原值+折旧/项目数+已发生成本+毛利率可观测）；制造域数据已使 manufacturing 域看板 4 KPI + production-variance/forecast-variance 报表 + crp-load 负荷报表非空（在制工单数/完工量/齐套待产/准时率/状态分布/产出趋势/工单延期预警 + 工作中心负荷/产能/负荷率可观测，crp-load 由 0628-1 解除空集）；维护+质量域数据已使 maintenance/quality 域看板 `getDashboardKpi` + 4 报表 + 3 预警非空（设备总数/运行数/待处理请求/本期维护访问/质检数/合格率/不合格数/开放 NCR/停机预警/维护逾期/CAPA 逾期可观测）；质量域 SPC 数据已使 quality 域看板 `getSpcOutOfControlWarning` 三计数器（outOfControlChartCount/inadequateCapabilityCount/openSpcNcrCount）非空（SPC 失控图/能力不足图/待处置 SPC NCR 可观测，解除 0930-2/0930-3 SPC Deferred）；CRM/CS/HR 域数据已使三域 5 报表非空（线索漏斗 leadCount/expectedRevenue、预测准确率 commitAmount/lineCount、工单 SLA/CSAT 命中率与均分、薪酬模拟差异+部门小计、员工净余额可观测）。其余扩展域交易单据未 seed（logistics/b2b/contract/drp/aps，Non-Goal，按域逐批补充）。
- 机制详情（主数据门控/非幂等/列映射/平台 bug 修复 见 `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`；交易单据表清单/列映射/拓扑序/范围裁决 见 `docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`；运营域表清单/列映射/拓扑序/范围裁决 见 `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`；制造域表清单/列映射/拓扑序/范围裁决 见 `docs/analysis/2026-07-09-0930-1-manufacturing-seed-table-map.md`；维护+质量域表清单/列映射/拓扑序/范围裁决 见 `docs/analysis/2026-07-09-0930-2-maintenance-quality-seed-table-map.md`；CRM/CS/HR 域表清单/列映射/拓扑序/范围裁决 见 `docs/analysis/2026-07-09-1045-1-crm-cs-hr-seed-table-map.md`；制造域工作中心配置链+crp_load 表清单/列映射/拓扑序/范围裁决/期望值派生 见 `docs/analysis/2026-07-09-0628-1-crp-load-seed-table-map.md`）。

## 分层运行

| 级别 | 命令 | 用途 |
| --- | --- | --- |
| 单文件 | `npx playwright test tests/e2e/dashboards/finance.smoke.spec.ts --workers=1` | 调试单个页面 |
| 看板套件 | `npx playwright test tests/e2e/dashboards/ --workers=1` | 看板回归 |
| 报表套件 | `npx playwright test tests/e2e/reports/ --workers=1` | 报表回归 |
| CRUD 套件 | `npx playwright test tests/e2e/crud/ --workers=1` | 18 域 CRUD 列表/表单回归 + 13 域列表断言 + 写路径 |
| 列表断言套件 | `npx playwright test tests/e2e/crud/*.list-value.spec.ts --workers=1` | 13 域 findPage seed 行断言 |
| 写路径套件 | `npx playwright test tests/e2e/crud/*.write.spec.ts --workers=1` | CRUD 写持久化验证（create/update/delete） |
| 业务动作套件 | `npx playwright test tests/e2e/business-actions/ --workers=1` | 6 代表域自定义 @BizMutation 经 GraphQL 全栈可达 + 状态机迁移 |
| 跨域编排套件 | `npx playwright test tests/e2e/orchestration/ --workers=1` | P2P/O2C 全链审批 + 业财过账产物（stockMove/voucher/AR-AP） |
| 全套件 | `npx playwright test --workers=1` | 提交前完整回归 |

全套件运行时间：约 17.5 分钟（131 测试：冒烟 + 数值断言 + CRUD 列表断言 + 4 写路径 spec（master-data GraphQL + master-data AMIS + quality + maintenance）+ 6 业务动作 spec（inventory StockMove + CRM Lead + CS Ticket + maintenance Visit + projects Task + quality CAPA/NCR）+ 2 跨域编排 spec（P2P + O2C）+ 1 看板前端渲染层 spec（10 域 visual）+ 1 报表前端渲染层 spec（4 域 visual），含每测试 UI 登录；`--workers=1`）。

## 数据驱动数值断言层

冒烟套件（`*.smoke.spec.ts`）仅验证渲染存在性。在 1234-1 主数据 + 1445-1 P2P/O2C 交易 + 2210-1 运营域交易 + 0930-1 制造域 + 0930-2 维护+质量域 + 1045-1 CRM/CS/HR 域 + 1145-2 质量域 SPC + 0628-1 制造域工作中心配置链+crp_load 种子基线上，核心域（finance/sales/purchase）+ 运营域（inventory/assets/projects）+ 扩展域（manufacturing/maintenance/quality）+ 扩展域 CRM/客服/人力（纯报表域，无看板）+ 主数据域（master-data，看板 KPI + 2 预警 + 2 报表）叠加了**数据驱动数值断言层**（`*.value.spec.ts`，28 spec：dashboards/{finance,sales,purchase,inventory,assets,projects,manufacturing,maintenance,quality,master-data}.value + reports/fin-{balance-sheet,income-statement,ar-ap-aging}.value + reports/{ast-depreciation,inv-inventory-trace,prj-cost-summary,mfg-production-variance,mfg-forecast-variance,mfg-crp-load,mnt-maintenance-history,qa-inspection-summary}.value + reports/{crm-lead-conversion-funnel,crm-forecast-accuracy,cs-ticket-sla-csat,hr-employee-net-balance,hr-payroll-simulation-comparison}.value + reports/{md-material-price-list,md-partner-list}.value），验证「seed → 聚合 → GraphQL → 断言」端到端数值链。

### 断言范式

- **看板 KPI**：spec 内 `page.request.post('/graphql', { query, variables })`（复用 UI 登录会话 cookie）取后端 `ErpXxxDashboard__getDashboardKpi` 原始返回 Map，与期望值表逐字段 `expect(Number(field)).toBe(expected)` 比对。
  - **日期漂移防护**：销售/采购/库存/维护/质量看板 KPI 默认区间依赖服务端当前日期。spec **显式传 `startDate=2026-07-01` / `endDate=2026-07-31`** 覆盖种子日期区间（制造看板传 `2026-06-01`/`2026-07-31` 覆盖跨 6/7 月 COMPLETED 工单），使断言确定性不依赖运行时日期。财务看板传 `periodId=1` 锁定种子期间；资产看板传 `periodId="2026-07"` 锁定种子折旧期间；项目看板 `getDashboardKpi()`/`getProjectGrossMargin()` 无日期参数，聚合域表全量。质量看板 `getSpcOutOfControlWarning`（无参数）确定性断言非零 1/1/1（SPC 三表已 seed，1145-2）。
- **报表渲染**：`page.request.post` 取 `Erp{Fin,Ast,Inv,Prj,Mfg,Mnt,Qa,Crm,Cs,Hr,Md}Report__renderHtml` 返回 HTML 字符串，断言含期望数值 token（剥离千分位逗号后匹配，规避 AMIS 渲染层 DOM 抖动）。CRM/CS/HR 三域为**纯报表域（无看板）**，故仅有报表渲染断言；可选入参经 `data:{forecastId|ticketType|simulationId:...}` 内联 map 传入（镜像 fin-income-statement `data:{periodId}` 范式）。master-data 两报表（物料价格清单/往来单位清单）零参全量渲染，token 含 materialCode/partnerCode + dict code（materialType/partnerType 经 `orm_propValueByName` 取原始值）+ 数值（NumberFormat `#,##0.00` 剥离千分位）。
- **主数据域看板/预警**：master-data `ErpMdDashboard__getDashboardKpi` 零参全表内存聚合（materialCount/customerCount/vendorCount/inactiveMaterialCount/inactivePartnerCount），无 trend（设计权威 `dashboards.md` 明示「主数据看板无趋势图」）；2 预警（`findMaterialWithoutSkuAlert`/`findSkuWithoutPriceAlert`）断言空集（种子真实态：4 material 全有 SKU、4 SKU 全有 purchasePrice>0）。vendorCount 经 1145-1 修复（常量 `"VENDOR"`→`"SUPPLIER"` 对齐权威字典 `erp-md/partner-type`）后返回真实供应商计数 2。

### 期望值表（确定性派生）

期望值派生自固化 seed CSV 的确定性聚合，落盘于：
- `docs/analysis/2026-07-08-1445-2-kpi-expected-values.md`（核心域 finance/sales/purchase 看板 + finance 报表，每 KPI 标注期望值 + 派生公式 + seed 行依据）
- `docs/analysis/2026-07-08-2210-2-operational-kpi-expected-values.md`（运营域 inventory/assets/projects 看板 + 运营域报表）
- `docs/analysis/2026-07-09-0930-3-mfg-mnt-qa-kpi-expected-values.md`（制造/维护/质量域看板 + 报表）
- `docs/analysis/2026-07-09-1045-2-crm-cs-hr-report-expected-values.md`（CRM/客服/人力域 5 报表，纯报表域无看板）
- `docs/analysis/2026-07-09-1145-1-master-data-expected-values.md`（主数据域看板 KPI + 2 预警 + 2 报表）
- `docs/analysis/2026-07-09-1145-2-quality-spc-seed-table-map.md`（质量域 SPC 三表种子表映射 + Strategy C 裁决 + getSpcOutOfControlWarning 期望值派生；supersede 0930-3 的 SPC=0 + openNcrCount=2 行）
- `docs/analysis/2026-07-09-0628-1-crp-load-seed-table-map.md`（制造域工作中心配置链+crp_load 种子表映射 + capacityHours 口径 + Strategy C 裁决 + crp-load-report 期望值派生；解除 0930-1 crp_load Deferred）

当前基线值：

| 域 / 报表 | 断言字段 | 期望值 |
| --- | --- | --- |
| finance 看板 | revenue / netProfit | 1130 / 1130 |
| sales 看板 | salesAmount / orderCount | 1000 / 1 |
| purchase 看板 | purchaseAmount / orderCount | 850 / 1 |
| inventory 看板 | totalValue / incomingQty / outgoingQty | 10450 / 100 / 0 |
| assets 看板 | originalValue / accumulatedDepreciation / netBookValue / periodDepreciation | 135000 / 6000 / 129000 / 2000 |
| projects 看板 | openProjectCount / totalBudget / incurredCost / executionRate | 1 / 50000 / 30000 / 0.6 |
| projects 毛利率 | totalRevenue / totalGrossProfit / grossMarginPct | 50000 / 20000 / 0.4 |
| manufacturing 看板 | inProcessCount / periodCompletedQty / stockPartialCount / onTimeRate | 1 / 180 / 1 / 0.5 |
| CRP 负荷报表 | workcenterCode + loadDate + loadHours/capacityHours/loadRate token | WC-001 / 2026-07-15 / 4.00 / 8.00 / 0.50 |
| maintenance 看板 | equipmentTotal / runningCount / openRequestCount / periodVisitCount | 3 / 2 / 1 / 1 |
| quality 看板 | inspectionCount / passRate / rejectedCount / openNcrCount | 3 / 0.6666666666666666 / 1 / 3 |
| quality SPC 预警 | outOfControlChartCount / inadequateCapabilityCount / openSpcNcrCount | 1 / 1 / 1 |
| 利润表 | 主营业务收入 token | 1,130.00 |
| 资产负债表 | 银行存款 token | 169.50 |
| AR-AP 账龄 | 全结算态（空明细） | title 渲染 + 合计行存在 |
| 资产折旧明细表 | asset 2 原值/累计折旧/本期折旧/净值 token | 120000.00 / 6000.00 / 2000.00 / 114000.00 |
| 库存追溯链报表 | 移动单号/数量 token（moveId=1） | MV-2026-001 / 100.00 |
| 项目成本汇总表 | 预算/实际成本/执行率 token | 50000.00 / 30000.00 / 60.00% |
| CRM 线索转化漏斗 | stageName + leadCount + expectedRevenue token | 验证 / 报价 / 50000.00 / 80000.00 |
| CRM 销售预测准确率 | commit/weighted/bestCase + lineWeightedRevenue token（forecastId=1） | 50000.00 / 45000.00 / 80000.00 / 63000.00 |
| CS 工单 SLA/CSAT 综合 | ticketTypeName + avgCsat/avgNps token（ticketType=1 投诉桶） | 投诉 / 5.00 / 9.00 |
| HR 员工净余额 | partnerName + advance/expense/netBalance token | 张三员工往来 / 1000.00 / 700.00 / 员工欠公司 |
| HR 薪酬模拟对比 | employeeName + salaryItemCode + original/adjusted + 部门小计 token（simulationId=1） | 赵明 / 钱华 / BASE_SALARY / 10000.00 / 11000.00 / 部门小计 |
| master-data 看板 | materialCount / customerCount / vendorCount / inactiveMaterialCount / inactivePartnerCount | 4 / 2 / 2（修复后）/ 0 / 0 |
| master-data 预警 | findMaterialWithoutSkuAlert / findSkuWithoutPriceAlert 行数 | 0 / 0（种子全有 SKU、全有 purchase 价） |
| 物料价格清单报表 | materialCode + materialType code + 采购/销售/零售价 token | MAT-001 / MAT-002 / FINISHED_PRODUCT / 120.00 / 200.00 / 280.00 / 300.00 / 500.00 / 680.00 |
| 往来单位清单报表 | partnerCode + partnerType code + creditLimit token | CUST-001 / CUST-002 / SUP-001 / SUP-002 / CUSTOMER / SUPPLIER / 500000.00 / 300000.00 |

### seed 漂移同步机制（强制）

**期望值表依赖 seed CSV 内容**。若 seed 变更（行增减 / 金额改动 / 业务日期/period 改动）：
1. 重新派生期望值（手算或 GraphQL 抽样导出）。
2. 同步更新对应分析文档（核心域 `2026-07-08-1445-2-kpi-expected-values.md` / 运营域 `2026-07-08-2210-2-operational-kpi-expected-values.md` / 制造+维护+质量域 `2026-07-09-0930-3-mfg-mnt-qa-kpi-expected-values.md` / CRM+CS+HR 域 `2026-07-09-1045-2-crm-cs-hr-report-expected-values.md`）。
3. 同步更新对应 `*.value.spec.ts` 的 `expected` / `expectedTokens`。

不更新则数值断言会失败（这是设计预期——暴露 seed 漂移，非测试脆弱）。manufacturing/maintenance/quality 域数值断言层已落地（0930-3，期望值表 `docs/analysis/2026-07-09-0930-3-mfg-mnt-qa-kpi-expected-values.md`）；CRM/客服/人力域报表数值断言层已落地（1045-2，期望值表 `docs/analysis/2026-07-09-1045-2-crm-cs-hr-report-expected-values.md`）；主数据域看板/报表数值断言层已落地（1145-1，期望值表 `docs/analysis/2026-07-09-1145-1-master-data-expected-values.md`，含 vendorCount 字典值漂移修复）；制造域 CRP 负荷报表数值断言层已落地（0628-1，期望值表 + capacityHours 口径 `docs/analysis/2026-07-09-0628-1-crp-load-seed-table-map.md`，完成全报表域数值断言覆盖里程碑）；其余扩展域（logistics/b2b/contract/drp/aps，无看板无报表）数值断言归后续批次（触发条件：对应域有可断言看板/报表时）。

## CRUD 数据驱动列表断言层 + 写路径验证

在数值断言层与 CRUD 冒烟层之上，0628-2 叠加了 CRUD 读写两面的数据驱动验证（解除 1234-2 两项 Deferred）：

### 列表断言层（`*.list-value.spec.ts`，13 域）

13 域代表性实体（有 seed 行的）经 GraphQL `ErpXxx__findPage(query:{offset:0,limit:200})` 查询，断言列表返回 seed 行（`items.length >= seed 行数` + JSON body 含关键字段 token）。调 `_helper.ts` 的 `assertCrudListValues({ entityName, route, expectedCount, expectedTokens, fields })` 委派，镜像 dashboards/reports helper 委派范式。

| 域 | 实体 | seed 行数 | token 依据 |
| --- | --- | --- | --- |
| master-data | ErpMdPartner | 5 | CUST-001/SUP-001/CUSTOMER/SUPPLIER |
| inventory | ErpInvStockMove | 1 | MV-2026-001/INCOMING |
| purchase | ErpPurOrder | 1 | PO-2026-001 |
| sales | ErpSalOrder | 1 | SO-2026-001 |
| finance | ErpFinVoucher | 4 | PZ-2026-001/TRANSFER |
| assets | ErpAstAsset | 3 | AST-2026-001/AST-2026-002 |
| projects | ErpPrjProject | 1 | PRJ-2026-001 |
| manufacturing | ErpMfgWorkOrder | 4 | WO-2026-001/COMPLETED/IN_PROCESS |
| quality | ErpQaInspection | 3 | INS-2026-001/ACCEPTED/REJECTED |
| maintenance | ErpMntVisit | 2 | VIS-2026-001/PLANNED |
| crm | ErpCrmLead | 2 | LEAD-2026-001/OPPORTUNITY |
| cs | ErpCsTicket | 2 | TKT-2026-001/HIGH |
| hr | ErpHrEmployee | 2 | HR-EMP-001/HR-EMP-002 |

### 写路径验证（`*.write.spec.ts`，master-data GraphQL + master-data AMIS + quality/maintenance GraphQL）

master-data ErpMdPartner 经 `runCrudWriteCycle`（GraphQL 层）+ `runAmisFormWrite`（AMIS 表单层）完成 create→get 验证→update→get 验证→delete→get 验证（not-found）全链；quality `ErpQaRiskRegister`（含 dict `status`）+ maintenance `ErpMntEquipmentCategory` 经 `runCrudWriteCycle` 补非 master-data 域覆盖。

**写机制说明（关键发现 + 0814-1 修复）**：
- 写持久化经 GraphQL mutation（`page.request.post('/graphql')`，复用 UI 登录会话 cookie）调用平台标准 `ErpXxx__save`/`ErpXxx__update`/`ErpXxx__delete`（同一 `CrudBizModel` 方法，AMIS 表单按钮提交经 `/graphql` 走相同 mutation）。
- **序列碰撞已修复（0814-1）**：平台 `SysSequenceGenerator` 默认序列 `nextValue` 初始化为 1（`addDefaultSequence`），但种子 CSV 显式插入 id=1~8，导致 create 主键冲突。`_init-data/zz-sequence-advance.sql`（0814-1 新增）在种子 CSV 加载后 `MERGE` 创建 default 序列行 `NEXT_VALUE=100000`（> 种子 id 上限 8）；因 `SysSequenceGenerator.lazyInit()` 是 `ioc:delay-method`（所有 bean 启动后才运行），`.sql` 执行时表为空，`MERGE` 创建行后 `addDefaultSequence()` 的 `if(!exists)` 守卫跳过，advanced 值保留。**效果**：首 GraphQL/AMIS save 即返回 id≥100000，无主键碰撞。
- **warm-up 重试已简化（0814-1）**：0628-2 helper 的 `MAX_SEQ_RETRIES=30` warm-up 重试循环（序列碰撞时逐次消费 id 直到越过种子区间）经 0814-1 序列修复后不再需要，简化为**单次容错**（最多 1 次重试，仅防御并发/唯一 code 瞬态冲突）。
- `update` mutation 支持部分字段更新（仅需 id + 待改字段）；`delete` 为逻辑删除（`delVersion`），删除后 `__get` 返回 not-found。
- **状态隔离**：fresh-DB 每次启动重置（webServer 默认 `rm -f db/erp.mv.db`）+ 同运行内唯一 code（`E2E-{entityName}-{ts}`）。
- **AMIS 表单写路径（0814-1 新增）**：`runAmisFormWrite` 经浏览器 UI 点「新增」→填表单（文本 + dict 下拉）→「确认」→列表/GraphQL 验证→行操作「编辑」→验证更新→（delete）。dict 下拉经 DOM evaluate 定位（label/option 多 locale 变体 + dict value code 匹配，规避 zh/en locale 漂移）。**已知限制**：AMIS action-group dropdown 的 Delete action（gated by confirmText）在 Playwright 下不触发其 confirm/API（Edit action 直接开 dialog 正常）→ AMIS spec 的 delete 改用同一 GraphQL `__delete` mutation（UI 按钮调用的同一端点），delete 机制本身由 GraphQL 层写 spec 独立证明。seq-default id 字段在 add 表单被标 mandatory（ORM 仍服务端生成实际 id，表单值仅满足客户端校验）。

## 业务动作浏览器层 E2E（`business-actions/`，6 代表域）

在 CRUD 读写路径之上，0814-2 叠加了自定义 `@BizMutation` 经 GraphQL `/graphql` 的全栈可达性 + 状态机迁移验证（解除 0628-2 Deferred「复杂业务动作 E2E」），覆盖 3 个代表域的非审批状态机/过账动作。2026-07-09-2004-1 将覆盖由 3 域扩展至 6 域：新增 maintenance Visit（5 态+设备联动副作用）、projects Task（4 态+前驱 DAG 门控）、quality CAPA（3 态）+ NCR（无 CAPA 路径），进一步验证三原语 helper 范式在多型状态机（含副作用联动、DAG 门控、过账标志）下的可复用性。审批工作流（xwf）域与剩余 DIRECT 域归 successor。

| 域 | 实体/动作 | spec | 验证内容 |
| --- | --- | --- | --- |
| inventory | `ErpInvStockMove` generateMove/complete/cancel | `inventory-stock-move.action.spec.ts` | 状态机 DRAFT→CONFIRMED→DONE + 过账型下游产物（不可变流水 `ErpInvStockLedger` 非空）+ cancel 异常路径 + CONFIRMED 态 confirm 拒绝守卫 |
| crm | `ErpCrmLead` qualify/moveStage/cancel | `crm-lead.action.spec.ts` | docStatus NEW→QUALIFIED + 漏斗 stageId 翻转（convLog 留痕归 Deferred）+ cancel |
| cs | `ErpCsTicket` assign/start/resolve/close/cancel | `cs-ticket.action.spec.ts` | 六态状态机 NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED 全链 + cancel + 非法迁移 ErrorCode 守卫 |
| maintenance | `ErpMntVisit` schedule/start/complete/cancel | `maintenance-visit.action.spec.ts` | 5 态状态机 DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED + cancel（SCHEDULED→CANCELLED）+ 设备状态联动副作用（start→UNDER_MAINTENANCE，complete/cancel→RUNNING 恢复种子态）+ COMPLETED→start 非法迁移守卫（2004-1） |
| projects | `ErpPrjTask` startTask/completeTask/blockTask/unblockTask | `projects-task.action.spec.ts` | 4 态状态机 TODO→IN_PROGRESS→DONE + blockTask/unblockTask 闭环（reason 必填）+ 前驱 DAG 门控（无前驱任务绕过 STRICT）+ DONE→startTask/blockTask 非法迁移守卫（2004-1） |
| quality | `ErpQaAction`（CAPA）startAction/completeAction/verifyAction | `quality-capa.action.spec.ts` | 3 态状态机 PENDING→IN_PROGRESS→COMPLETED + verifyAction 填充验证字段（不改 status）+ COMPLETED→completeAction 守卫（2004-1，最小状态机证明范式可复用性） |
| quality | `ErpQaNonConformance`（NCR）submitReview/escalateToRecall/cancel | `quality-ncr.action.spec.ts` | 无 CAPA 路径状态机 OPEN→IN_REVIEW→cancel(CANCELLED) / escalateToRecall(ESCALATED_TO_RECALL) + 非法迁移守卫；resolve（CAPA 闭包门控）+ postNcr/reverseNcr（需 RESOLVED）归 successor（2004-1） |

### 业务动作调用范式（`_helper.ts`）

- **三个原语**：`createViaSave`（经标准 `__save` 建前置实体，复用 write.spec.ts `${entity}__save_input` 范式）、`callMutation`（经 GraphQL mutation 调自定义 `@BizMutation`，标量入参内联、复杂入参经 `input(type, value)` 包装走 variable + 显式 GraphQL input 类型名）、`verifyState`（经 `__get` 独立断言状态字段，独立于 mutation 返回值权威查库）。
- **关键发现（Nop GraphQL filter 格式）**：`__findPage` 的 `filter` 必须用 Nop FieldTreeBean Map 格式 `{ $type:'eq', name, value }`（`$type` 持有算子，`name`/`value` 持有属性）；plain-map 字段相等（`{moveId:123}`）报 `nop.err.core.filter.op-is-null`。`eqFilter(field,value)` / `andFilter(...leafs)` helper 封装此格式。
- **状态字段核实**：inventory StockMove 状态字段 `docStatus`（DRAFT/CONFIRMED/DONE/CANCELLED）+ `posted`（Boolean，过账标志）；CRM Lead 状态字段为 `docStatus`（非 `leadStatusId` 后者线索子状态 FK）；CS Ticket 状态字段为 `status`（六态）。
- **实现修订（generateMove 自动 confirm）**：经核实 `ErpInvStockMoveProcessor`，`generateMove` 内部经 `doConfirm` 自动推进 DRAFT→CONFIRMED（独立创建无 relatedBillType 停在 CONFIRMED），「confirm」为内部过渡步骤无独立 DRAFT 创建入口。故 StockMove spec 实测状态链为 `generateMove(独立)→CONFIRMED→complete→DONE`。
- **posted 字段语义**：`posted` 反映跨域财务过账（`InvPostingDispatcher`：成功置 true、失败优雅降级 false 不阻塞 DONE 终态）。spec 断言 `typeof posted === 'boolean'` + 同事务不可变流水 `ErpInvStockLedger` 非空（可靠过账产物）；凭证借贷平衡精确数值归 finance 数值断言层 successor。
- **产物清理（强制）**：业务动作创建不可逆下游产物（库存流水/余额、工单审计/调查、线索转化日志）。全栈共享同一 Quarkus+H2 实例（`reuseExistingServer: true`），不清理会污染下游数值断言（dashboard KPI 聚合 stock_balance/ledger、report 聚合 ticket/survey）。每个 spec 在断言完成后经 `deleteByFilter`/`deleteById` 清理自身产物（镜像 write.spec.ts create→delete 范式，但扩展到不可逆下游产物逐域删除）。

## 跨域编排链浏览器层 E2E（`orchestration/`，P2P + O2C）

在业务动作层之上，1249-1 叠加了 P2P（Procure-to-Pay）+ O2C（Order-to-Cash）核心业财循环的跨域编排链浏览器层端到端验证（解除 0814-2 Deferred「跨域编排链完整 E2E」+「业财过账凭证数值断言」）。经 GraphQL `/graphql` 驱动全链 `__save`（头+行）→ `submitForApproval` → `approve`，每步 `verifyState` 经 `__get` 独立断言 approveStatus 翻转，并断言业财过账产物（库存移动 + GL 凭证 + AR-AP 辅助账）。

| 链路 | spec | 验证内容 |
| --- | --- | --- |
| P2P（PO→Receive→Invoice） | `p2p-chain.spec.ts` | PO/Receive/Invoice approveStatus UNSUBMITTED→SUBMITTED→APPROVED + Receive approve 触发 INCOMING 移动（DONE）+ Invoice approve GL 过账（posted=true + voucher bill_r 回链 + AP 辅助账 PAYABLE/openAmount=含税56.5/OPEN） |
| O2C（SO→Delivery→Invoice） | `o2c-chain.spec.ts` | SO/Delivery/Invoice approveStatus 三态翻转 + SO approve 信用控制通过 + Delivery approve 触发 OUTGOING 移动（DONE）+ Invoice approve GL 过账（posted=true + voucher 回链 + AR 辅助账 RECEIVABLE/openAmount=含税113/OPEN） |

### 编排链范式（`orchestration/_helper.ts`）

- **链式驱动**：`runP2pChain` / `runO2cChain` 编排多实体链式创建（头 `__save` → 行独立 `__save` FK 引用头 id，行实体 `registerShortName=true` 有独立 GraphQL 端点）+ `submitForApproval` + `approve`（DIRECT 审批模式，Payment/Receipt xwf WORKFLOW 模式归 Deferred）。每步 `verifyState` 经 `__get` 权威断言状态翻转。返回各实体 id + 下游移动单（findFirst by relatedBillType+relatedBillCode）供 spec 断言/清理。
- **过账产物断言原语**：`findItems`（`__findPage` 返回 items，读 stockMove.code / ar_ap.openAmount 等详情）；`findPageTotal`（0814-2 原语，total≥1 存在性）。
- **清理原语**：`cleanupVoucherByBillCode`（经 billCode 关联删凭证行+凭证+回链）+ `cleanupArApByCode`（按 sourceBillCode 删 AR-AP 辅助账）+ `cleanupStockMove`（移动单凭证+流水+移动单行+移动单+余额逐域逻辑删除）。链路 spec 在 `finally` 调 `cleanupP2p`/`cleanupO2c` 清理全部不可逆下游产物，保护共享 DB 数值断言基线（inventory dashboard totalValue / ar-ap-aging 报表）。
- **O2C 备货前置**：WH-RAW/MAT-1 种子无余额，出库会因负库存禁止（`CONFIG_ALLOW_NEGATIVE_STOCK` 默认 false）失败。链路前先 `generateMove` INCOMING 备货（独立移动 → CONFIRMED → `complete` → DONE），WH-RAW/MAT-1 余额无种子行清理时整行删除安全。
- **关键发现（业财过账 COA 完备性修复）**：执行发现种子 COA（`erp_md_subject.csv`）与过账 Provider 硬编码科目码不一致——`PurAcctDocProvider`(1403/2221/2202)、`SalAcctDocProvider`(1131/6001/2221)、`InvAcctDocProvider`(1401/6401/2202) 所需 1403/2221/1131/1401/6401 在种子缺失，致 `resolveSubjects` 抛 `ERR_SUBJECT_NOT_FOUND`→过账优雅降级 posted=false。补齐种子 COA（`erp_md_subject.csv` +5 行 1401/1403/1131/2221/6401，`findByCode` 全局按码解析无需 COA 映射）后过账 happy-path 可达。安全性：`persistVoucher` 仅写 voucher/voucher_line/voucher_bill_r（**不写 gl_balance**），finance 看板/资产负债表/利润表读 gl_balance 不受影响，全套件 0 回归。此为种子演示数据完备性修复（非生产代码/契约/模型变更）。
- **Payment/Receipt xwf 浏览器层裁决（Deferred）**：Payment/Receipt 为 `useWorkflow=true` xwf WORKFLOW 模式。原型实证：`nop` 浏览器用户调 `submitForApproval`，xwf 返回 `步骤[submit]不允许被用户[<nop uuid>]调用,步骤的参与者限定为[user:$0]`——wf `submit` 步骤参与者限定为 `user:$0`（SYS id=0，后端测试 `setUserId("0")` 规避），`nop` 用户不匹配致 submit 被拒。归 Deferred successor（触发条件：xwf 浏览器层审批 API 验证可行 / nop 用户 wf 委托配置落地 / wf 步骤参与者配置放宽时）。

## 看板 AMIS 前端渲染层 E2E（`visual/`，10 域）

在数值断言层之上，1249-2 叠加了看板 AMIS 前端渲染层 DOM 断言（`tests/e2e/visual/dashboards.visual.spec.ts`，1 spec / 10 测试）。区别于数值断言层（`page.request.post('/graphql')` 直调后端绕过 AMIS），本层驱动真实 AMIS 页面 → 拦截 AMIS 自身 GraphQL 响应 → 断言 DOM 渲染，验证「page → AMIS GraphQL 调用 → adaptor → DOM 渲染」全路径。解除 0637-1 Deferred「像素级视觉回归」的部分意图（以 DOM 内容/结构断言替代像素 diff；纯像素 diff 仍 Deferred）。

### 断言范式（`visual/_helper.ts`）

`assertDashboardRendered(cfg)` 编排：`loginAndNavigate` → `page.waitForResponse`（拦截含 `getDashboardKpi` 的 `/graphql` 响应，断言 200）→ 断言 KPI 卡片结构（`.border.rounded.p-3` wrapper 可见且 count≥1）→ 条件化数值 token（`cfg.expectedKpiTokens` 提供时，`expect.poll` 断言 `span.h3` textContent 含 token）→ 条件化 echarts canvas（`cfg.hasChart` 时 `toBeVisible` + boundingBox 非零）→ 条件化预警表格（`cfg.alertTable` 时 `table` 可见）。

### 执行期发现的产品缺陷（首个能捕获的层）

本层首次发现 **AMIS `$var` GraphQL 查询模板损坏缺陷**（`docs/bugs/2026-07-09-1249-dashboard-amis-var-mangling.md`）：8 参数化看板 page.yaml 手写 `query($var:Type){...($var)...}` 经 AMIS 运行时模板解析，裸 `$var` 被替换为空，致查询损坏（实测请求体 `query(:Long){ ...periodId: }`）、KPI 恒 0/空。冒烟层（仅查标签 + GraphQL 200，损坏查询仍返回 200）与数值断言层（直调后端绕过 AMIS）均无法捕获。

据此断言分层：
- **全 10 域**：AMIS GraphQL 管线（200）+ KPI 卡片结构 + echarts canvas（master-data 无 trend 跳过）+ 预警表格——全绿。
- **2 非参数化域**（projects `50000`、master-data `4`，查询无 `$var`，AMIS 路径完整）：额外断言确定性数值 token 渲染进 DOM——全绿。
- **8 参数化域**（finance/sales/purchase/inventory/assets/manufacturing/maintenance/quality）：因 `$var` 损坏缺陷暂仅断言结构，数值 token 断言随修复 successor（见 plan 2026-07-09-1249-2 Deferred）落地。

### 层间区分

| 层 | 验证路径 | 捕获能力 |
| --- | --- | --- |
| 冒烟层 `*.smoke.spec.ts` | AMIS 页面 → GraphQL 200 + 标签关键词 | 页面可达性 + GraphQL 不崩溃（不验数值） |
| 数值断言层 `*.value.spec.ts` | `page.request.post` 直调后端绕过 AMIS | 后端聚合确定性数值（不验前端渲染） |
| 前端渲染层 `*.visual.spec.ts` | AMIS 页面 → AMIS 自身 GraphQL → adaptor → DOM | AMIS 渲染管线完整性 + adaptor/模板回归（首个捕获 `$var` 损坏缺陷） |

## CRUD 套件（18 域列表/表单冒烟）

`tests/e2e/crud/` 下每域 1 个代表性「主单据头」实体 spec（共 18 spec），调 `_helper.ts` 的 `runCrudListSmoke({ domain, entityRoute, addFormField })` 委派。每 spec 断言：列表页 DOM 渲染（`.cxd-Crud`/`.cxd-Table`）+ add 按钮（`button:has(.fa-plus)`）+ `/graphql` 查询 200 + 无 console error + add 表单打开后字段渲染（`input[name="code"]`）。

| 域 | 实体 | 路由 | 选型理由 |
| --- | --- | --- | --- |
| master-data | `ErpMdPartner`（往来单位） | `/ErpMdPartner-main` | 2328-2 基准沿用 |
| inventory | `ErpInvStockMove`（库存移动单） | `/ErpInvStockMove-main` | 2328-2 基准沿用 |
| purchase | `ErpPurOrder`（采购订单） | `/ErpPurOrder-main` | 改选主单据头（基准 `ErpPurRequisition`） |
| sales | `ErpSalOrder`（销售订单） | `/ErpSalOrder-main` | 改选主单据头（基准 `ErpSalQuotation`） |
| finance | `ErpFinVoucher`（会计凭证） | `/ErpFinVoucher-main` | 改选主单据头（基准 `ErpFinVoucherTemplate`） |
| assets | `ErpAstAsset`（固定资产） | `/ErpAstAsset-main` | 2328-2 基准沿用 |
| projects | `ErpPrjProject`（项目） | `/ErpPrjProject-main` | 2328-2 基准沿用 |
| manufacturing | `ErpMfgWorkOrder`（工单） | `/ErpMfgWorkOrder-main` | 改选主单据头（基准 `ErpMfgRouting`） |
| quality | `ErpQaInspection`（质检单） | `/ErpQaInspection-main` | 改选主单据头（基准 `ErpQaInspectionTemplate`） |
| maintenance | `ErpMntVisit`（维护访问） | `/ErpMntVisit-main` | 2328-2 基准沿用 |
| crm | `ErpCrmLead`（线索/商机） | `/ErpCrmLead-main` | 改选主单据头（基准 `ErpCrmBundlePricing`） |
| cs | `ErpCsTicket`（客服工单） | `/ErpCsTicket-main` | 改选主单据头（基准 `ErpCsTicketType`） |
| hr | `ErpHrEmployee`（员工） | `/ErpHrEmployee-main` | 改选主单据头（基准 `ErpHrSurvey`） |
| aps | `ErpApsOperationOrder`（工序工单） | `/ErpApsOperationOrder-main` | 2328-2 基准沿用 |
| logistics | `ErpLogShipment`（发运单） | `/ErpLogShipment-main` | 2328-2 基准沿用 |
| b2b | `ErpB2bAsn`（提前发货通知） | `/ErpB2bAsn-main` | 2328-2 基准沿用 |
| contract | `ErpCtContract`（合同） | `/ErpCtContract-main` | 2328-2 基准沿用 |
| drp | `ErpDrpPlan`（DRP 计划） | `/ErpDrpPlan-main` | 2328-2 基准沿用 |

「改选主单据头」理由：浏览器 E2E 倾向用户最常打开的列表页（业务单据头）而非 config/模板实体。冒烟级仅断言列表 DOM + add 表单字段可见（不持久化），故主单据头 mandatory 字段不阻断。全 343 实体覆盖 + CRUD 写操作 + 数据驱动断言为 successor（触发条件见 `docs/plans/2026-07-08-1234-2-crud-page-e2e-smoke.md` Deferred）。

## 认证机制

- 每个测试通过 UI 登录（`nop`/`123`）获取会话
- storageState 方案不适用（SPA 路由守卫在 context 初始化时拒绝预注入 token）
- 测试用户由 `allow-create-default-user=true` JVM 属性创建（非生产配置变更）

## 诊断

遵循 `docs/references/playwright-e2e-guide.md` 决策树：

1. **应用未启动？** → 检查端口 8080、runner jar 是否存在
2. **页面空白？** → 检查 console error、SPA 是否加载、hash 路由是否正确
3. **GraphQL 非 200？** → 检查 `/graphql` 端点（非 `/api/GenericApi`）、查询语法
4. **登录失败？** → 确认 `allow-create-default-user=true`、H2 DB 是否已初始化

失败时查看 trace：

```bash
npx playwright show-trace test-results/<test-name>/trace.zip
```

## 已知限制

- **空库冒烟**：~~H2 文件库无业务数据，KPI 卡片渲染 DOM 但数值为 0/空。~~ **已解除**：webServer 默认含 `-Dnop.orm.init-database-data=true`（fresh-DB 重置 + 91 张 CSV 种子：21 主数据 + 23 P2P/O2C 交易单据 + 13 运营域表 + 4 制造域表 + 11 维护+质量域表 + 12 CRM/CS/HR 域表 + 3 质量域 SPC 表 + 4 制造域工作中心配置链+crp_load 表 + 3 处加性追加）。核心域（finance/sales/purchase）+ 运营域（inventory/assets/projects）+ 扩展域（manufacturing/maintenance/quality）看板 KPI 与报表数值经交易数据驱动**非空可观测**（含质量域 SPC 失控预警三计数器非空 + 制造域 CRP 负荷报表非空）；CRM/CS/HR 三域 5 报表经种子数据驱动**非空可观测**（三域无看板）。核心域 + 运营域 + 扩展域（manufacturing/maintenance/quality）+ 扩展域（CRM/CS/HR 纯报表域）+ 主数据域（master-data 看板/2 报表）均已叠加**数据驱动数值断言层**（28 `*.value.spec.ts`，见上方「数据驱动数值断言层」）；其余扩展域（logistics/b2b/contract/drp/aps）无看板无报表未 seed。
- **单浏览器**：仅 chromium（Chrome channel），不支持 Firefox/WebKit/移动视口。
- **冒烟级**：不断言像素级视觉一致性、不验证报表渲染内容正确性、不断言下载产物。
- **页面验证已恢复**：`ErpCsTicket.view.xml`/`ErpHrEmployee.view.xml` layout 缺陷已修复（见 `docs/bugs/`），启动期页面模型校验（`validate-page-model=true`）已恢复全绿，不再使用 `-Dnop.web.validate-page-model=false` 绕过。
- **page.yaml 已修复**：全部 34 page.yaml 已修复 API URL（`/api/GenericApi`→`/graphql`）+ GraphQL Map 字段选择移除。原始 page.yaml 存在系统性 bug（从未运行时测试过）。
- **下载功能**：报表下载 button（XLSX/PDF）的后端 `ErpXxxReport__download` 有 DataBean 序列化限制，E2E 降级为 button 存在性检查。

## 文件结构

```
package.json                          # @playwright/test 依赖 + scripts
playwright.config.ts                  # 配置（端口/webServer/testDir/channel）
tests/e2e/
├── auth.ts                           # 登录 helper（performLogin + loginAndNavigate）
├── fixtures.ts                       # test 扩展（console error 检查器）
├── global-setup.ts                   # globalSetup（保留，当前未使用——storageState 不适用）
├── dashboards/
│   ├── _helper.ts                    # runDashboardSmoke + assertDashboardKpiValues 共享函数
│   ├── finance.smoke.spec.ts         # 独立 spec（含 KPI 文本断言）
│   ├── finance.value.spec.ts         # 数值断言层（GraphQL getDashboardKpi 取值）
│   ├── sales.smoke.spec.ts           # 使用 _helper
│   ├── sales.value.spec.ts           # 数值断言层
│   ├── purchase.smoke.spec.ts
│   ├── purchase.value.spec.ts        # 数值断言层
│   ├── inventory.smoke.spec.ts
│   ├── inventory.value.spec.ts       # 数值断言层（运营域）
│   ├── assets.smoke.spec.ts
│   ├── assets.value.spec.ts          # 数值断言层（运营域）
│   ├── projects.smoke.spec.ts
│   ├── projects.value.spec.ts        # 数值断言层（运营域：getDashboardKpi + getProjectGrossMargin）
│   ├── manufacturing.smoke.spec.ts
│   ├── manufacturing.value.spec.ts   # 数值断言层（扩展域）
│   ├── maintenance.smoke.spec.ts
│   ├── maintenance.value.spec.ts     # 数值断言层（扩展域）
│   ├── quality.smoke.spec.ts
│   ├── quality.value.spec.ts         # 数值断言层（扩展域：getDashboardKpi + getSpcOutOfControlWarning）
│   ├── master-data.smoke.spec.ts
│   └── master-data.value.spec.ts     # 数值断言层（主数据域：getDashboardKpi 5 字段 + 2 预警空集）
├── crud/
│   ├── _helper.ts                    # runCrudListSmoke + assertCrudListValues + runCrudWriteCycle 共享函数
│   ├── cs-kb-suggestion.smoke.spec.ts  # KB 建议定向冒烟（suggestForTicket GraphQL 200）
│   ├── master-data.smoke.spec.ts     # ErpMdPartner
│   ├── inventory.smoke.spec.ts       # ErpInvStockMove
│   ├── purchase.smoke.spec.ts        # ErpPurOrder
│   ├── sales.smoke.spec.ts           # ErpSalOrder
│   ├── finance.smoke.spec.ts         # ErpFinVoucher
│   ├── assets.smoke.spec.ts          # ErpAstAsset
│   ├── projects.smoke.spec.ts        # ErpPrjProject
│   ├── manufacturing.smoke.spec.ts   # ErpMfgWorkOrder
│   ├── quality.smoke.spec.ts         # ErpQaInspection
│   ├── maintenance.smoke.spec.ts     # ErpMntVisit
│   ├── crm.smoke.spec.ts             # ErpCrmLead
│   ├── cs.smoke.spec.ts              # ErpCsTicket
│   ├── hr.smoke.spec.ts              # ErpHrEmployee
│   ├── aps.smoke.spec.ts             # ErpApsOperationOrder
│   ├── logistics.smoke.spec.ts       # ErpLogShipment
│   ├── b2b.smoke.spec.ts             # ErpB2bAsn
│   ├── contract.smoke.spec.ts        # ErpCtContract
│   ├── drp.smoke.spec.ts             # ErpDrpPlan
│   ├── *.list-value.spec.ts          # 13 域 CRUD 数据驱动列表断言（findPage seed 行 + token）
│   ├── master-data.write.spec.ts     # master-data CRUD 写路径 GraphQL 层（create/update/delete 持久化）
│   ├── master-data.write.amis.spec.ts # master-data CRUD 写路径 AMIS 表单层（UI 新增/编辑/删除，0814-1）
│   ├── quality.write.spec.ts         # quality ErpQaRiskRegister CRUD 写路径 GraphQL 层（0814-1，含 dict status）
│   └── maintenance.write.spec.ts     # maintenance ErpMntEquipmentCategory CRUD 写路径 GraphQL 层（0814-1）
├── business-actions/                 # 业务动作浏览器层 E2E（0814-2 + 2004-1，自定义 @BizMutation 经 GraphQL，6 代表域）
│   ├── _helper.ts                    # createViaSave/callMutation/verifyState/eqFilter/deleteByFilter 原语
│   ├── inventory-stock-move.action.spec.ts  # StockMove generateMove/complete/cancel 状态机+过账
│   ├── crm-lead.action.spec.ts              # Lead qualify/moveStage/cancel 状态迁移
│   ├── cs-ticket.action.spec.ts             # Ticket 六态状态机 + 非法迁移守卫
│   ├── maintenance-visit.action.spec.ts     # Visit schedule/start/complete/cancel 5 态 + 设备联动副作用（2004-1）
│   ├── projects-task.action.spec.ts         # Task startTask/completeTask/blockTask/unblockTask 4 态 + DAG 门控（2004-1）
│   ├── quality-capa.action.spec.ts          # CAPA startAction/completeAction/verifyAction 3 态（2004-1）
│   └── quality-ncr.action.spec.ts           # NCR submitReview/escalateToRecall/cancel 无 CAPA 路径（2004-1）
├── orchestration/                       # 跨域编排链浏览器层 E2E（1249-1，P2P/O2C 全链审批+过账产物）
│   ├── _helper.ts                        # runP2pChain/runO2cChain + findItems + 清理原语（凭证/AR-AP/移动单产物）
│   ├── p2p-chain.spec.ts                 # P2P PO→Receive→Invoice 全链 + stockMove + GL/AP 过账产物
│   └── o2c-chain.spec.ts                 # O2C SO→Delivery→Invoice 全链 + 出库移动 + GL/AR 过账产物
├── visual/                              # 看板 AMIS 前端渲染层 E2E（1249-2，DOM 结构 + echarts canvas + 数值 token）
│   ├── _helper.ts                       # assertDashboardRendered（waitForResponse getDashboardKpi + 卡片结构 + canvas + 表格）
│   └── dashboards.visual.spec.ts        # 10 域看板 AMIS 渲染管线断言（2 非参数化域含数值 token）
└── reports/
    ├── _helper.ts                    # runReportSmoke + assertReportRenderedWithValue 共享函数
    ├── fin-balance-sheet.value.spec.ts   # 数值断言层
    ├── fin-income-statement.value.spec.ts # 数值断言层
    ├── fin-ar-ap-aging.value.spec.ts     # 数值断言层
    ├── ast-depreciation.value.spec.ts     # 数值断言层（运营域）
    ├── inv-inventory-trace.value.spec.ts  # 数值断言层（运营域）
    ├── prj-cost-summary.value.spec.ts     # 数值断言层（运营域）
    ├── mfg-production-variance.value.spec.ts  # 数值断言层（扩展域）
    ├── mfg-forecast-variance.value.spec.ts    # 数值断言层（扩展域）
    ├── mfg-crp-load.value.spec.ts             # 数值断言层（扩展域，工作中心负荷报表）
    ├── mnt-maintenance-history.value.spec.ts  # 数值断言层（扩展域）
    ├── qa-inspection-summary.value.spec.ts    # 数值断言层（扩展域）
    ├── crm-lead-conversion-funnel.value.spec.ts   # 数值断言层（扩展域 CRM，纯报表域无看板）
    ├── crm-forecast-accuracy.value.spec.ts        # 数值断言层（扩展域 CRM）
    ├── cs-ticket-sla-csat.value.spec.ts           # 数值断言层（扩展域 CS）
    ├── hr-employee-net-balance.value.spec.ts      # 数值断言层（扩展域 HR）
    ├── hr-payroll-simulation-comparison.value.spec.ts  # 数值断言层（扩展域 HR）
    ├── md-material-price-list.value.spec.ts       # 数值断言层（主数据域，物料价格清单）
    ├── md-partner-list.value.spec.ts              # 数值断言层（主数据域，往来单位清单）
    └── *.smoke.spec.ts               # 24 个报表 spec
```
