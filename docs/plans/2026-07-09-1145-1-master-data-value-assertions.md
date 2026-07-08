# 2026-07-09-1145-1-master-data-value-assertions 主数据域看板/报表数据驱动数值断言 + vendorCount KPI 修复

> Plan Status: completed
> Mission: erp
> Work Item: master-data 域看板 KPI + 2 报表渲染数据驱动精确数值浏览器 E2E 断言；并修复 `getDashboardKpi.vendorCount` 永远返回 0 的字典值不匹配缺陷
> Last Reviewed: 2026-07-09
> Source: deferred 项承接 `docs/plans/2026-07-09-1045-2-crm-cs-hr-report-value-assertions.md` Deferred「其他扩展域看板/报表数值断言（logistics/b2b/contract/drp/aps/master-data）」中 **master-data 子集**（Successor Required: yes，触发条件「当 master-data 数值断言需独立期望值派生时」——**已满足**：master-data 看板 `ErpMdDashboardBizModel` + 2 报表 `ErpMdReportBizModel` 已就绪，1234-1 主数据种子已固化非空）；AGENTS.md 当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-09-1045-2-crm-cs-hr-report-value-assertions.md`（completed，纯报表域断言范式 + helper，本计划同 helper 向 master-data 延伸）、`docs/plans/2026-07-08-1445-2-data-driven-e2e-value-assertions.md`（completed，`assertDashboardKpiValues`/`assertReportRenderedWithValue` helper 权威源）、`docs/plans/2026-07-08-1234-1-demo-seed-data-init.md`（completed，主数据种子 21 CSV 固化基线）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`/`ls`，非采信旧记忆）：

- **E2E 断言范式已就绪（1445-2/2210-2/0930-3/1045-2 交付）**：`tests/e2e/dashboards/_helper.ts` 含 `assertDashboardKpiValues`（直接 GraphQL query 取 `getDashboardKpi` 原始 Map 逐字段断言，规避 AMIS DOM 抖动）；`tests/e2e/reports/_helper.ts` 含 `assertReportRenderedWithValue`（断言 `renderHtml` HTML 含期望数值 token，剥离千分位后匹配）。当前 `find tests/e2e -name '*.spec.ts'` = **77 spec 文件 / 79 tests**，`_vfs/_init-data/` = **84 CSV**。10 域看板断言已覆盖 9 域（fin/sal/pur/inv/ast/prj/mfg/mnt/qa），**唯一缺口 = master-data**（1045-2 Deferred 明示「master-data 归独立 successor」）。
- **master-data 看板读源已就绪**（`module-master-data/erp-md-service/.../dashboard/ErpMdDashboardBizModel.java`，3 `@BizQuery`）：
  - `getDashboardKpi`（:43-72）：读 `ErpMdMaterial` + `ErpMdPartner` + `ErpMdMaterialSku`，全表内存聚合——`materialCount`=Σ material、`customerCount`=filter `partnerType=="CUSTOMER"`、`vendorCount`=filter `partnerType==PARTNER_TYPE_VENDOR`、`inactiveMaterialCount`=filter `status=="INACTIVE"`、`inactivePartnerCount`=filter `status=="INACTIVE"`。**无 trend 查询**（对齐 `dashboards.md` §说明「主数据看板无趋势图，仅 KPI + 预警」）。
  - `findMaterialWithoutSkuAlert`（:75-96）：返回无 SKU 的 material。
  - `findSkuWithoutPriceAlert`（:99-119）：返回四价（purchase/sale/wholesale/retail）全 ≤0 的 SKU。
- **master-data 报表读源已就绪**（`module-master-data/erp-md-service/.../report/ErpMdReportBizModel.java`，模板根 `/nop/main/report/md/`）：`material-price-list.xpt.xml`（`buildMaterialPriceListDataset` 读 material + 默认 SKU isDefault=true，输出 materialCode/name/type/status/skuCode/purchasePrice/salePrice/wholesalePrice/retailPrice）+ `partner-list.xpt.xml`（`buildPartnerListDataset` 读 partner，输出 partnerCode/name/type/status/contactPerson/phone/email/creditLimit/creditPeriodDays）。
- **⚠️ 确认实时缺陷（vendorCount）**：`ErpMdDashboardBizModel.java:36` `PARTNER_TYPE_VENDOR = "VENDOR"`，但权威字典 `erp-md/partner-type`（`module-master-data/model/app-erp-master-data.orm.xml:77-82`）仅定义 `CUSTOMER/SUPPLIER/BOTH/EMPLOYEE`，**无 `VENDOR` 值**。种子 `erp_md_partner.csv` 用 `SUPPLIER`（2 行）。故 `vendorCount` 永远返回 0（应为 2）。`customerCount` 用 `"CUSTOMER"` 正确。此为已确认字典值漂移缺陷，按计划规则须为 `Fix`（不可降级为 follow-up）。
- **主数据种子已固化（1234-1，非空）**：`erp_md_material.csv` 4 行（MAT-001/002/003/004 全 ACTIVE，type FINISHED_PRODUCT/FINISHED_PRODUCT/RAW_MATERIAL/PACKAGING）；`erp_md_material_sku.csv` 4 行（materialId 1-4 全 IS_DEFAULT=true，均有 purchasePrice>0，**CSV 无 WHOLESALE_PRICE 列→wholesale 全 null**）；`erp_md_partner.csv` 5 行（2 CUSTOMER + 2 SUPPLIER + 1 EMPLOYEE，全 ACTIVE）。
- **派生期望值（基线预估，Phase 1 逐 token 再核实落盘）**：KPI materialCount=4 / customerCount=2 / vendorCount=**2（修复后）** / inactiveMaterialCount=0 / inactivePartnerCount=0；两 alert 均返回 0 行（所有 material 有 SKU、所有 SKU 有 purchase 价）；material-price-list token MAT-001/MAT-002 + 120.00/200.00/280.00/300.00/500.00/680.00（wholesale 空白）；partner-list token CUST-001/CUST-002/SUP-001/SUP-002 + CUSTOMER/SUPPLIER。
- **保护区域**：`vendorCount` 修复改 1 处生产常量（`ErpMdDashboardBizModel.java` 私有常量 `"VENDOR"`→`"SUPPLIER"`，非 ORM/非公共契约/非认证）——属 `plan-first`（含生产代码变更）。断言 spec 纯新增 tests/。**非 `ask-first`**（不动 `model/*.orm.xml`；字典 `erp-md/partner-type` 权威值已是 `SUPPLIER`，改 BizModel 对齐字典而非改字典）。

剩余差距：(1) master-data 看板/报表无数值断言（10 域看板断言覆盖唯一缺口）；(2) `vendorCount` 字典值漂移缺陷致 KPI 永远 0；(3) 期望值逐 token 派生 + 落盘待 Phase 1。

## Goals

- 修复 `ErpMdDashboardBizModel.PARTNER_TYPE_VENDOR` 字典值漂移缺陷（`"VENDOR"`→`"SUPPLIER"` 对齐权威字典），使 `vendorCount` 返回真实供应商计数。
- 在 1234-1 固化主数据种子基线上叠加**数据驱动数值断言层**：新增 master-data 看板 KPI 断言 spec（`getDashboardKpi` 5 字段 + 2 alert 空集断言）+ 2 报表渲染数值断言 spec（material-price-list / partner-list）。
- 期望值逐 token 派生（seed 行 + 聚合口径 + NumberFormat 去逗号），落盘分析文档。
- 解除 1045-2 Deferred「其他扩展域看板/报表数值断言（master-data 子集）」；完成 10 域看板 + 全报表域数值断言覆盖里程碑。

## Non-Goals

- **不**做 master-data trend 断言——`ErpMdDashboardBizModel` 无 trend 查询（设计权威 `dashboards.md` 明示「主数据看板无趋势图」）。
- **不**改 `erp-md/partner-type` 字典或种子——字典权威值已是 `SUPPLIER`；缺陷在 BizModel 常量侧，改常量对齐字典。`BOTH` 类型伙伴是否同时计入 customer/vendor 属独立语义增强（种子无 BOTH 行，不影响断言），归 Deferred。
- **不**为使 alert 非空而改种子/加无 SKU/无价 material——alert 返回 0 行属种子数据真实态（所有 material 有 SKU、所有 SKU 有价），断言 0 行即可观测；刻意构造 alert 数据超出「固化种子上叠断言」范式。
- **不**做像素级视觉回归 / 报表下载产物 diff / 跨浏览器矩阵（0637-1 Deferred，触发条件未变）。
- **不**覆盖其他域断言（logistics/b2b/contract/drp/aps 无看板无报表；其余域已 done）。
- **不**修改 `model/*.orm.xml`/`*.xpt.xml`/`*.page.yaml`/`*.view.xml`（除 vendorCount 1 常量外无生产代码变更）。

## Task Route

- Type: `implementation-only change`（vendorCount Fix）+ `verification or audit work`（数值断言层）
- Owner Docs: `docs/design/dashboards.md`（§主数据看板布局/口径权威）、`docs/testing/e2e-runbook.md`（断言范式 + 期望值表）、`module-master-data/model/app-erp-master-data.orm.xml`（`erp-md/partner-type` 字典权威值）
- Skill Selection Basis: Phase 1 vendorCount 修复触及 BizModel 常量 → `nop-backend-dev`（字典值对齐 + 常量修改）。Phase 2-3 数值断言属浏览器 E2E（Playwright）→ `nop-testing`（`nop-testing` SKILL.md §什么时候用我 明列「E2E 测试 / Playwright / 端到端」并路由 `02-core-guides/e2e-testing.md`，项目方法源 `docs/references/playwright-e2e-guide.md`）。Phase 1 Decision/Proof（期望值派生/分析）与 Phase 4（文档对齐）无技能覆盖（`Skill: none`）。

## Infrastructure And Config Prereqs

- **预构建 runner jar**：断言依赖 `app-erp-all/target/app-erp-all-1.0.0-SNAPSHOT-runner.jar`（`mvn clean install -DskipTests` 产物）。`playwright.config.ts` webServer 经 fresh-DB 重置 + `-Dnop.orm.init-database-data=true` 加载 84 CSV 种子（已就绪，1234-1/1045-1 范式）。
- **端口**：默认 8080；运行前 `lsof -ti :8080 | xargs kill`。
- **无新外部依赖/密钥**。回滚策略：vendorCount 改动为 1 常量（revert 1 行）；断言纯新增 tests/（删除即回滚）。

## Execution Plan

### Phase 1 - vendorCount 字典值漂移修复 + 期望值派生（Fix + Proof + Decision）

Status: completed
Targets: `module-master-data/erp-md-service/src/main/java/app/erp/md/service/dashboard/ErpMdDashboardBizModel.java`、`docs/analysis/2026-07-09-1145-1-master-data-expected-values.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision | Proof`
- Prereqs: 无

- [x] `Fix`：修复 `vendorCount` 字典值漂移——核实缺陷根因（`ErpMdDashboardBizModel.java:36` 常量 `"VENDOR"` vs 字典 `erp-md/partner-type` 权威值 `SUPPLIER`，`module-master-data/model/app-erp-master-data.orm.xml:77-82`）；将 `PARTNER_TYPE_VENDOR = "VENDOR"` 改为 `"SUPPLIER"`。`rg '"VENDOR"' module-master-data/erp-md-service/src/main` 核实无其他代码依赖该字面量（仅此 1 处私有常量）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：断言口径裁决——(a) `vendorCount` 修复后期望=2（2 SUPPLIER 行）；(b) 两 alert（findMaterialWithoutSkuAlert/findSkuWithoutPriceAlert）种子真实态均返回 0 行（所有 material 有 SKU、所有 SKU 有 purchasePrice>0），断言空集即观测；(c) material-price-list 报表 wholesale 列全空（CSV 无 WHOLESALE_PRICE 列），断言不涉及 wholesale token；(d) 报表 partnerType 经 `orm_propValueByName` 渲染——Phase 1 核实渲染为 dict code 还是 label，据实派生 token。记录每 token 期望值 + 派生公式 + seed 行依据。
  - Skill: none
- [x] `Proof`：逐 token 派生期望值并落盘 `docs/analysis/2026-07-09-1145-1-master-data-expected-values.md`——KPI 5 字段（materialCount/customerCount/vendorCount/inactiveMaterialCount/inactivePartnerCount）+ 2 alert 空集 + material-price-list token 集 + partner-list token 集，每项标注 seed 行依据 + 聚合口径 + NumberFormat `#,##0.00` 去逗号处理。
  - Skill: none

Exit Criteria:

- [x] `vendorCount` 修复落地（常量改 `"SUPPLIER"`），`mvn test -pl module-master-data/erp-md-service -am` BUILD SUCCESS 0 failures/0 errors（确认无回归）
- [x] 期望值分析文档落盘，逐 token 标注 seed 行依据

### Phase 2 - 看板 KPI + alert 数值断言 spec（Add + Proof）

Status: completed
Targets: `tests/e2e/dashboards/master-data.value.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（vendorCount 修复 + 期望值派生）

- [x] `Add`：新增 `tests/e2e/dashboards/master-data.value.spec.ts`，调 `assertDashboardKpiValues`（`dashboards/_helper.ts`）取 GraphQL `ErpMdDashboard__getDashboardKpi` 原始 Map，逐字段断言 materialCount=4 / customerCount=2 / vendorCount=2（修复后）/ inactiveMaterialCount=0 / inactivePartnerCount=0。master-data 无 trend（不断言 trend）。日期漂移防护：master-data KPI 全表聚合无日期参数（确定性来自 seed 行本身，镜像 1045-2 CRM/HR 零参范式）。
  - Skill: `nop-testing`
- [x] `Add`：同 spec 增 2 alert 空集断言——GraphQL `ErpMdDashboard__findMaterialWithoutSkuAlert` + `__findSkuWithoutPriceAlert` 返回 list 长度=0（种子真实态：所有 material 有 SKU、所有 SKU 有 purchase 价）。
  - Skill: `nop-testing`
- [x] `Proof`：`npx playwright test tests/e2e/dashboards/master-data.value.spec.ts --workers=1` 全绿（断言 5 KPI + 2 alert 空集，与 Phase 1 期望值表逐项匹配）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] master-data 看板 KPI（5 字段）+ 2 alert 空集断言 spec 全绿，与 Phase 1 期望值表逐项一致

### Phase 3 - 2 报表渲染数值断言 spec（Add + Proof）

Status: completed
Targets: `tests/e2e/reports/{md-material-price-list,md-partner-list}.value.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（期望值派生）

- [x] `Add`：新增 `tests/e2e/reports/md-material-price-list.value.spec.ts`，调 `assertReportRenderedWithValue`（`reports/_helper.ts`）断言 `ErpMdReport__renderHtml`（reportName=`material-price-list`）HTML 含 seed 派生 token（MAT-001/MAT-002 + 120.00/200.00/280.00/300.00/500.00/680.00 等，剥离千分位后匹配；wholesale 空白不涉及）。零参（全量 material，镜像 fin-balance-sheet 零参范式）或按 Phase 1 Decision 传 `materialCode`。
  - Skill: `nop-testing`
- [x] `Add`：新增 `tests/e2e/reports/md-partner-list.value.spec.ts`，断言 `renderHtml`（reportName=`partner-list`）HTML 含 token（CUST-001/CUST-002/SUP-001/SUP-002 + partnerType 渲染值 + creditLimit 等）。零参或按 Phase 1 Decision 传 `partnerType`。
  - Skill: `nop-testing`
- [x] `Proof`：`npx playwright test tests/e2e/reports/md-material-price-list.value.spec.ts tests/e2e/reports/md-partner-list.value.spec.ts --workers=1` 全绿（与 Phase 1 期望值表逐 token 匹配）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] master-data 2 报表渲染数值断言 spec 全绿，token 与 Phase 1 期望值表逐项一致

### Phase 4 - 文档对齐 + Deferred 解除登记（Add）

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/backlog/README.md`、`docs/logs/2026/07-09.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 + Phase 3

- [x] `Add`：`docs/testing/e2e-runbook.md` 数值断言层段补 master-data（看板 5 KPI + 2 alert + 2 报表）+ 期望值表 master-data 行 + 套件计数（spec 文件 77→80 / tests 79→84，以实际为准）+ 文件结构补 3 新 spec。
  - Skill: none
- [x] `Add`：`docs/testing/known-good-baselines.md` 增 1145-1 基线行；`docs/backlog/README.md` 增 master-data 数值断言工作项 ✅ done；`docs/logs/2026/07-09.md` 增 1145-1 日志条目（含 vendorCount 修复 + 验证状态）；1045-2 plan Deferred「master-data 子集」标记 RELEASED（本计划 Closure 段登记）。
  - Skill: none

Exit Criteria:

- [x] 文档对齐落地（runbook + baselines + backlog + 日志 + 1045-2 Deferred 解除登记）

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_0bc78e029ffejiuO1mtxE1zP93`，独立 general 子代理，新会话冷重播无执行者上下文) — 基线逐项 live 验真全 PASS（vendorCount 缺陷 orm:77-82 字典无 VENDOR + BizModel:36 常量 + 种子 SUPPLIER 2 行 + helper/spec 计数 77/84 全核实；缺陷修复路径正确改常量非改字典；scope bundling 同组件合规 rule 14；vendorCount 为范围内 Fix 不降级）。1 MAJOR：Skill 选择事实反转——Task Route 误称「nop-testing 覆盖 Java 后端测试不适用浏览器 E2E」，实际 `nop-testing` SKILL.md:20 明列「E2E 测试 / Playwright」并路由 `e2e-testing.md`（1045-2 iter1 同类已修）→ **已修复**（Task Route Skill Selection Basis 重写 + Phase 2/3 E2E spec authoring 项 Skill: none→nop-testing，Phase 1 分析项与 Phase 4 文档项保留 none）。2 MINOR（Phase 1 退出 localized test 略宽但属改动包合规 rule 7 可接受；material-price-list token 枚举部分由「等」+ Phase 1 Proof 逐 token 派生覆盖）保留。迭代 1 后无 BLOCKER/MAJOR。
- Independent draft review iteration 2: accept (`ses_0bc7269f3ffeGSWBb8mnC2EVuR`，独立 general 子代理，新会话冷重播无执行者上下文) — M1 修复完整落地核实（Task Route Skill Selection Basis 重写 + Phase 2/3 E2E 项 Skill: nop-testing + Phase 1 分析/Phase 4 文档保留 none，14 处 Skill 标记矩阵一致；nop-testing SKILL.md:20/43 事实独立复核）。基线抽查全 PASS（vendorCount 缺陷 + 种子行数 + 77 spec / master-data 唯一缺口）。规则合规：rule 1/7/13、item typing、slack 词、Deferred 触发条件、Closure Gates 全核实。无 BLOCKER/MAJOR/MINOR-blocker。草案收敛为可接受执行契约，计划转 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（vendorCount 修复 + master-data 看板/2 报表数值断言全绿）
- [x] 相关文档对齐（e2e-runbook + known-good-baselines + backlog + 日志）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块，确认 vendorCount 修复无后端回归）+ `mvn test -pl module-master-data/erp-md-service -am`（md-service 0 failures/0 errors）+ `npx playwright test`（全套件 fresh-DB seed：既有 + 新增 master-data 断言全绿 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up（vendorCount 为范围内 Fix 不可降级；master-data trend/像素回归/BOTH 语义均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### master-data trend 断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpMdDashboardBizModel` 无 trend 查询（设计权威 `dashboards.md` 明示「主数据看板无趋势图，仅 KPI + 预警」）。断言对象不存在。
- Successor Required: `no`

### BOTH 类型伙伴同时计入 customer/vendor 语义增强

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前 KPI 严格按 partnerType 精确匹配（CUSTOMER→customerCount、SUPPLIER→vendorCount），BOTH 类型不计入任一。种子无 BOTH 行，不影响断言。是否将 BOTH 同时计入两计数属独立产品语义决策。
- Successor Required: `yes`
- Trigger Condition: 当产品要求 BOTH 类型伙伴同时纳入客户/供应商计数时。

### 像素级视觉回归 + 报表下载产物 diff + 跨浏览器矩阵

- Classification: `optimization candidate`
- Why Not Blocking Closure: 0637-1 既定 Deferred，触发条件未变。本计划数值断言属数据正确性层，非视觉/格式层。
- Successor Required: `yes`
- Trigger Condition: 同 0637-1 Deferred。

## Closure

Status Note: 执行完成（2026-07-09，4 Phase 全绿）。在 1234-1 固化主数据种子基线上叠加**数据驱动数值断言层** + 修复 vendorCount 字典值漂移缺陷。

**vendorCount 修复（范围内 Fix）**：`ErpMdDashboardBizModel.PARTNER_TYPE_VENDOR` 常量 `"VENDOR"`→`"SUPPLIER"` 对齐权威字典 `erp-md/partner-type`（orm:77-82 仅 CUSTOMER/SUPPLIER/BOTH/EMPLOYEE 无 VENDOR），原致 `vendorCount` 永远返回 0（应为 2）。缺陷在 BizModel 常量侧（字典权威值已是 SUPPLIER），改常量对齐字典而非改字典——非 ORM/非公共契约/非认证。同步更新 2 测试 seed（TestErpMdDashboard/TestErpMdReportRendering VENDOR→SUPPLIER）；`rg '"VENDOR"'` 核实仅 1 处私有常量。

**数值断言层（3 `*.value.spec.ts`）**：`tests/e2e/dashboards/master-data.value.spec.ts`（getDashboardKpi 5 字段 + 2 预警空集断言）+ `tests/e2e/reports/{md-material-price-list,md-partner-list}.value.spec.ts`。断言值（逐 token 与 `docs/analysis/2026-07-09-1145-1-master-data-expected-values.md` 期望值表一致）：KPI materialCount=4/customerCount=2/vendorCount=2（修复后）/inactiveMaterialCount=0/inactivePartnerCount=0；2 预警均 0 行（种子全有 SKU、全有 purchasePrice>0）；物料价格清单 MAT-001/MAT-002/FINISHED_PRODUCT/RAW_MATERIAL/120.00/200.00/280.00/300.00/500.00/680.00/8.50；往来单位清单 CUST-001/CUST-002/SUP-001/SUP-002/CUSTOMER/SUPPLIER/500000.00/300000.00。master-data 看板零参全表内存聚合无 trend（设计权威 `dashboards.md` 明示「主数据看板无趋势图」）；partnerType/materialType 经 `orm_propValueByName` 渲染为 dict code 非 label（Phase 1 Decision 据实派生）；creditLimit 经 NumberFormat `#,##0.00` 渲染（500,000.00 剥离千分位匹配）。

**验证全绿（full-green）**：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，确认 vendorCount 修复无后端回归）；`mvn test -pl module-master-data/erp-md-service -am`（45 测试 0 failures/0 errors，含 TestErpMdDashboard 4 + TestErpMdReportRendering 9）；`npx playwright test tests/e2e/dashboards/master-data.value.spec.ts tests/e2e/reports/{md-material-price-list,md-partner-list}.value.spec.ts --workers=1`（5 passed，1.0m）；`npx playwright test` 全套件 fresh-DB seed **84 passed (11.7m) 0 回归**（79 既有 + 5 新增）。spec 文件总数 77→80（测试 79→84）。

**文档对齐**：e2e-runbook（数值断言层段补主数据域 + 套件计数 24→27 spec/77→80 文件/79→84 测试 + 期望值表 4 行 + 文件结构补 3 新 spec）、known-good-baselines（增 1145-1 基线行）、backlog README（增 1145-1 + 补 1045-2 工作项 ✅ done）、日志（07-09.md 增 1145-1 条目含 vendorCount 修复 + 验证状态）。

**Deferred 解除登记**：1045-2 Deferred「其他扩展域看板/报表数值断言（logistics/b2b/contract/drp/aps/master-data）」**master-data 子集 RELEASED**（本计划交付）。完成 **10 域看板 + 全报表域数值断言覆盖里程碑**。剩余 logistics/b2b/contract/drp/aps 无看板无报表仍 open。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话冷重播，无执行者上下文）— 语义审计 APPROVED。
- Audit Scope: 逐项 live 真伪核验（grep/read/glob，非采信 [x] 标记）。
- Audit Findings:
  - vendorCount 修复落地核实：`ErpMdDashboardBizModel.java:36` `PARTNER_TYPE_VENDOR = "SUPPLIER"`；`rg '"VENDOR"' module-master-data` 0 命中（无残留字面量）。
  - 期望值派生核实：`app-erp-all/.../_init-data/erp_md_partner.csv` 逐行核实 = 2 CUSTOMER + 2 SUPPLIER + 1 EMPLOYEE 全 ACTIVE → customerCount=2 / vendorCount=2（修复后）/ inactivePartnerCount=0，与 spec 期望值逐项一致。
  - Anti-Hollow 核实：3 spec 文件均调用真实 helper（`assertDashboardKpiValues`/`assertReportRenderedWithValue`）并传具体期望 token，非空体；BizModel `getDashboardKpi`(:44-72) 含真实全表过滤聚合逻辑。
  - 五点一致性：Plan Status completed / 4 Phase 全 completed / 各 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure evidence 非占位符 — 全一致。
  - Deferred honesty：3 Deferred 项（trend 不存在/BOTH 语义/像素回归）均附触发条件，无范围内缺陷降级。
  - Docs sync：`docs/logs/2026/07-09.md` 含 1145-1 条目 + 验证状态；分析文档落盘 `docs/analysis/2026-07-09-1145-1-master-data-expected-values.md`。
- Executor Evidence（执行环境产物，非自审）：(1) `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；(2) `mvn test -pl module-master-data/erp-md-service -am` 45 测试 0 failures/0 errors；(3) `npx playwright test` 全套件 84 passed 0 回归；(4) `git diff` 范围 = ErpMdDashboardBizModel 常量 1 处 + 2 测试 seed 对齐 + 3 新 spec（tests/）+ 1 分析文档（docs/analysis/）+ 4 文档对齐（e2e-runbook/known-good-baselines/backlog/log）+ 1045-2 Deferred 解除登记 + 本 plan 勾选。

Follow-up:

- 无非阻塞跟进项（master-data trend 不存在；BOTH 语义/像素回归均为计划内 Non-Goal 附触发条件）。
