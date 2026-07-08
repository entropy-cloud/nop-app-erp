# Playwright E2E 冒烟回归运行手册

## 概述

本手册指导如何运行 `nop-app-erp` 的 Playwright E2E 冒烟回归套件，覆盖 10 域看板 + 24 域报表页面 + 18 域 CRUD 列表/表单页 + 1 KB 建议定向冒烟 + 6 核心域数据驱动数值断言（共 59 spec）。

测试层级：**冒烟级**（页面 DOM 渲染 + 关键元素存在 + GraphQL `/graphql` 请求返回 200 + 无未捕获 console error）。非像素级视觉回归。

在冒烟级之上，核心域（finance/sales/purchase）已叠加**数据驱动数值断言层**（`*.value.spec.ts`）：直接经 GraphQL query 取后端聚合原始值，断言匹配确定性期望值。详见下方「数据驱动数值断言层」。

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
- `-Dnop.orm.init-database-data=true` — 部署期种子数据初始化（21 张主数据表 + 23 张交易单据表，详见下方「种子库启动」）
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

- `-Dnop.orm.init-database-data=true` — 激活平台 `DataInitInitializer`（条件 bean，仅此 JVM 属性开启时实例化），从 `_vfs/_init-data/*.csv` 按拓扑序插入 **44 张 CSV**：
  - **21 张核心主数据表**（1234-1）：组织/币种/计量单位/物料/SKU/往来单位/仓库/员工/科目体系/税率/结算方式等
  - **23 张业务交易单据表**（1445-1）：P2P（PO/Receive/Invoice/Payment 头+行）+ O2C（SO/Delivery/Invoice/Receipt 头+行）+ 已过账财务产物（凭证/凭证行/业财回链/AR-AP 辅助账/GL 余额/会计期间 OPEN）
- `rm -f db/erp.mv.db db/erp.trace.db` — **fresh-DB 重置**。`DataInitInitializer` 非幂等（无存在性检查），持久 H2 文件库重复启动会主键冲突；故每次 webServer 启动前删除 db 文件，确保 seed 在空表上插入。交易种子加入后该行为不变（仅 CSV 增多，webServer JVM 参数与重置逻辑无改动）。

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
- **种子范围**：核心主数据（bootstrap）+ P2P/O2C 最小连通交易单据集（含已过账财务产物）。核心域看板/报表/CRUD 列表经 GraphQL 证实数据非空；交易数据已使 finance/sales/purchase 域 KPI 数值非空（采购额/销售额/收入/净利润可观测）。扩展域交易单据未 seed（Non-Goal，按域逐批补充）。
- 机制详情（主数据门控/非幂等/列映射/平台 bug 修复 见 `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`；交易单据表清单/列映射/拓扑序/范围裁决 见 `docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`）。

## 分层运行

| 级别 | 命令 | 用途 |
| --- | --- | --- |
| 单文件 | `npx playwright test tests/e2e/dashboards/finance.smoke.spec.ts --workers=1` | 调试单个页面 |
| 看板套件 | `npx playwright test tests/e2e/dashboards/ --workers=1` | 看板回归 |
| 报表套件 | `npx playwright test tests/e2e/reports/ --workers=1` | 报表回归 |
| CRUD 套件 | `npx playwright test tests/e2e/crud/ --workers=1` | 18 域 CRUD 列表/表单回归 |
| 全套件 | `npx playwright test --workers=1` | 提交前完整回归 |

全套件运行时间：~8.4 分钟（53 冒烟 spec × ~9.5s/spec，含每测试 UI 登录）。数值断言层（6 spec）约 +1.5 分钟。

## 数据驱动数值断言层

冒烟套件（`*.smoke.spec.ts`）仅验证渲染存在性。在 1445-1 固化交易种子基线上，核心域叠加了**数据驱动数值断言层**（`*.value.spec.ts`，6 spec：dashboards/finance/sales/purchase + reports/fin-balance-sheet/income-statement/ar-ap-aging），验证「seed → 聚合 → GraphQL → 断言」端到端数值链。

### 断言范式

- **看板 KPI**：spec 内 `page.request.post('/graphql', { query, variables })`（复用 UI 登录会话 cookie）取后端 `ErpXxxDashboard__getDashboardKpi` 原始返回 Map，与期望值表逐字段 `expect(Number(field)).toBe(expected)` 比对。
  - **日期漂移防护**：销售/采购看板 KPI 默认区间依赖服务端当前日期。spec **显式传 `startDate=2026-07-01` / `endDate=2026-07-31`** 覆盖种子日期区间，使断言确定性不依赖运行时日期。财务看板传 `periodId=1` 锁定种子期间。
- **报表渲染**：`page.request.post` 取 `ErpFinReport__renderHtml` 返回 HTML 字符串，断言含期望数值 token（剥离千分位逗号后匹配，规避 AMIS 渲染层 DOM 抖动）。

### 期望值表（确定性派生）

期望值派生自 1445-1 固化 seed CSV 的确定性聚合，落盘于 `docs/analysis/2026-07-08-1445-2-kpi-expected-values.md`（每 KPI 标注期望值 + 派生公式 + seed 行依据）。当前基线值：

| 域 / 报表 | 断言字段 | 期望值 |
| --- | --- | --- |
| finance 看板 | revenue / netProfit | 1130 / 1130 |
| sales 看板 | salesAmount / orderCount | 1000 / 1 |
| purchase 看板 | purchaseAmount / orderCount | 850 / 1 |
| 利润表 | 主营业务收入 token | 1,130.00 |
| 资产负债表 | 银行存款 token | 169.50 |
| AR-AP 账龄 | 全结算态（空明细） | title 渲染 + 合计行存在 |

### seed 漂移同步机制（强制）

**期望值表依赖 1445-1 seed CSV 内容**。若 1445-1 seed 变更（行增减 / 金额改动 / 业务日期改动）：
1. 重新派生期望值（手算或 GraphQL 抽样导出）。
2. 同步更新 `docs/analysis/2026-07-08-1445-2-kpi-expected-values.md`。
3. 同步更新对应 `*.value.spec.ts` 的 `expected` / `expectedTokens`。

不更新则数值断言会失败（这是设计预期——暴露 seed 漂移，非测试脆弱）。扩展域数值断言归后续批次（触发条件：对应域交易种子 seed 后）。

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

- **空库冒烟**：~~H2 文件库无业务数据，KPI 卡片渲染 DOM 但数值为 0/空。~~ **已解除**：webServer 默认含 `-Dnop.orm.init-database-data=true`（fresh-DB 重置 + 44 张 CSV 种子：21 主数据 + 23 P2P/O2C 交易单据）。核心域（finance/sales/purchase）看板 KPI 与报表数值经交易数据驱动**非空可观测**（采购额/销售额/收入/净利润）。核心域已叠加**数据驱动数值断言层**（6 `*.value.spec.ts`，见上方「数据驱动数值断言层」）。扩展域交易单据未 seed（Non-Goal，按域逐批补充），其数值断言归后续批次。
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
│   ├── assets.smoke.spec.ts
│   ├── projects.smoke.spec.ts
│   ├── manufacturing.smoke.spec.ts
│   ├── maintenance.smoke.spec.ts
│   ├── quality.smoke.spec.ts
│   └── master-data.smoke.spec.ts
├── crud/
│   ├── _helper.ts                    # runCrudListSmoke 共享函数
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
│   └── drp.smoke.spec.ts             # ErpDrpPlan
└── reports/
    ├── _helper.ts                    # runReportSmoke + assertReportRenderedWithValue 共享函数
    ├── fin-balance-sheet.value.spec.ts   # 数值断言层
    ├── fin-income-statement.value.spec.ts # 数值断言层
    ├── fin-ar-ap-aging.value.spec.ts     # 数值断言层
    └── *.smoke.spec.ts               # 24 个报表 spec
```
