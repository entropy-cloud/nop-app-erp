# Known-Good Baselines

## Purpose

Record the latest verified project state so future AI sessions can tell whether a failure is new or pre-existing.

This file is lightweight. Record only meaningful baselines, not every local command run.

## Baselines

| Date | Source | Git State | Scope | Commands Passed | Known Failures | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-07-07 | local | commit `957c288e` (clean tree) | full | `mvn clean install -DskipTests` | none | commit message 标注 "full-green verification"；154 reactor 模块构建通过 | 18 域 + notify 子系统 codegen 骨架已就绪；本基线为 2026-07-07 综合审计整改（plan-2026-07-07-1915-1）开始前的最近已知良好状态 |
| 2026-07-07 | local | commit `957c288e` + dirty (orm.xml 4 处表名修正、文档对齐) | package (master-data/b2b/cs/logistics/contract 5 域 dao + codegen) | `mvn clean install -DskipTests -pl :app-erp-b2b-dao,:app-erp-cs-dao,:app-erp-logistics-dao,:app-erp-contract-dao -am` | none | 本仓库工作树（执行中） | plan-2026-07-07-1915-1 Phase 1（C-1 表名双前缀修正）的增量构建证据；生成 `_app.orm.xml` 表名已正确为 `erp_md_partner/material/employee` |
| 2026-07-08 | local | commit `aaf42335` + dirty (34 page.yaml 修复 + Playwright E2E 基础设施新增) | full + E2E | `mvn clean install -DskipTests`（154 模块，1:54）；`npx playwright test --workers=1`（34 spec，5.4m） | none | 本仓库工作树（plan-2026-07-08-0637-1 执行中） | E2E 冒烟套件全绿：10 看板 + 24 报表；page.yaml 修复 `/api/GenericApi`→`/graphql`（115 处）+ Map 字段选择移除（91 处）；运行时 JVM 参数 `service-public`/`allow-create-default-user`/`validate-page-model=false` |
| 2026-07-08 | local | dirty (ErpCsTicket.view.xml + ErpHrEmployee.view.xml 修复 + playwright config/e2e-runbook 移除 validate-page-model 覆盖 + 新增 KB suggestion spec) | full + E2E | `mvn clean install -DskipTests`（154 模块）；默认配置启动（`validate-page-model=true`，无 JVM 覆盖，11.3s 启动成功）；`npx playwright test --workers=1`（35 spec，5.6m） | none | 本仓库工作树（plan-2026-07-08-1107-1 执行中） | 页面模型校验安全网已恢复：修复 ErpCsTicket layout `=` 前缀 + 4 处 `/api/GenericApi`→`/graphql` + Map 字段选择移除；修复 ErpHrEmployee cell `visible` + `<option>` 结构；移除 `-Dnop.web.validate-page-model=false` 全局绕过；新增 KB suggestion 冒烟 spec（`suggestForTicket` GraphQL 200） |
| 2026-07-08 | local | dirty (21 张主数据 seed CSV + playwright.config.ts webServer 加 seed flag/fresh-DB reset + nop-entropy DataInitInitializer sessionFactory patch) | full + E2E (seeded DB) | `mvn clean install -DskipTests`（154 模块）；fresh-DB 启动 `-Dnop.orm.init-database-data=true`（21 表 0 冲突，11.4s）；`SKIP_WEBSERVER=1 npx playwright test --workers=1`（35 spec，5.7m）；平台单测 `TestDataInitInitializer`（4 spec） | none | 本仓库工作树（plan-2026-07-08-1234-1 执行中） | 部署期种子数据启用：21 张主数据 CSV（`_vfs/_init-data/`）经 `DataInitInitializer` 加载；修复平台 bug（`ormTemplate.sessionFactory` 在 Quarkus `@PostConstruct` 因 IoC 循环依赖为 null → NPE）；seed 经 GraphQL 抽样证实非空（material=4/partner=4/subject=8...，`createdBy='sys'` 自动填充）；E2E 在种子库上 0 回归 |
| 2026-07-08 | local | dirty (18 域 CRUD smoke spec + crud/_helper.ts + e2e-runbook/known-good-baselines 更新) | E2E (seeded DB) | `npx playwright test tests/e2e/crud/ --workers=1`（19 spec，2.8m，含新增 18 CRUD + 既有 cs-kb-suggestion） | none | 本仓库工作树（plan-2026-07-08-1234-2 执行中） | 18 域 CRUD 列表/表单冒烟套件落地：每域 1 代表性主单据头实体（7 域改选主单据头：pur/sal/fin/mfg/qa/crm/cs/hr），`runCrudListSmoke` 共享 helper（列表 DOM + add 按钮 + GraphQL 200 + add 表单字段 + 无 console error）；spec 总数 35→53；零生产代码/契约/模型变更（纯消费侧测试） |
| 2026-07-08 | local | dirty (23 张交易单据 seed CSV erp_{pur,sal,fin}_* + 交易种子表映射 analysis + seed-data.md 交易种子段) | full + fresh-DB 启动 + E2E (seeded DB) | `mvn clean install -DskipTests`（154 模块）；fresh-DB 启动 `-Dnop.orm.init-database-data=true`（44 CSV：21 主数据 + 23 交易表，0 冲突/0 列映射错误/0 参照失败，13.1s）；`npx playwright test`（53 spec，7.8m）；GraphQL 抽样 FK 一致（voucher 平衡/voucher_bill_r 串联/ar_ap_item SETTLED openAmount=0/GL 余额自洽） | none | 本仓库工作树（plan-2026-07-08-1445-1 执行中） | 业务交易单据种子（P2P+O2C）落地：每域 1 条端到端连通链 + 对应已过账财务产物（凭证/凭证行/回链/AR-AP/GL 余额/期间 OPEN）；看板 KPI 数值由 0 转非空（采购额=850/销售额=1000/收入=1130/净利润=1130）；纯数据文件（CSV）+ 文档，零 ORM/契约变更 |
| 2026-07-08 | local | dirty (6 数值断言 spec：dashboards/{finance,sales,purchase}.value.spec.ts + reports/fin-{balance-sheet,income-statement,ar-ap-aging}.value.spec.ts + 2 helper 扩展 _helper.ts + 期望值表 analysis) | E2E (seeded DB) | `npx playwright test tests/e2e/dashboards/ --workers=1`（13 spec：10 冒烟 + 3 数值，0 回归，1.8m）；`npx playwright test tests/e2e/reports/ --workers=1`（27 spec：24 冒烟 + 3 数值，0 回归，4.5m） | none | 本仓库工作树（plan-2026-07-08-1445-2 执行中） | 数据驱动数值断言层落地：看板 KPI 经 GraphQL `getDashboardKpi` 取值断言（fin revenue/netProfit=1130、sal salesAmount=1000/orderCount=1、pur purchaseAmount=850/orderCount=1）；报表 `renderHtml` HTML 含确定性 token（利润表 主营业务收入 1,130.00、资产负债表 银行存款 169.50、AR-AP 账龄 全结算空明细）。期望值表见 `docs/analysis/2026-07-08-1445-2-kpi-expected-values.md`；spec 总数 53→59。纯测试新增，零生产代码变更 |
| 2026-07-08 | local | dirty (7 扩展域 orm.xml 22 实体加 businessDate + 3 A 档加 posted + IDX 索引；22 BizModel defaultPrepareSave；13 生产直接 ORM 创建点 setBusinessDate；7 域 test seed/快照更新) | full + 7 域单测 | `xmllint --noout`（7 域源 orm.xml，exit=0）；`mvn clean install -DskipTests`（154 模块，1:32）；`mvn test`（cs/hr/logistics/b2b/contract/drp/aps 7 域 service，0 failures/0 errors） | none | 本仓库工作树（plan-2026-07-08-0056-1 执行中） | 7 扩展域 posted/businessDate 标准字段补齐：22 事务头 businessDate（DATE mandatory）+ 3 过账绑定头 posted（BOOLEAN default false）。codegen 增量重生成 Entity/XMeta/view 自动含新列；22 BizModel defaultPrepareSave 兜底（CoreMetrics.today()）+ 13 生产直接 ORM 创建点显式 setBusinessDate 解决 mandatory 回归（~190 测试）。ORM ask-first 经 mission-driver 显式指令授权。详见 `docs/analysis/2026-07-08-0056-extended-domains-posted-businessdate-classification.md` |

## When To Update

Update this file when:

- full typecheck/build/lint/test verification passes after a meaningful change
- a previously failing command becomes green and should be remembered
- a team intentionally accepts a known failing command and records it as a known failure, not as a passed command

## Rule

Do not mark a command as passed unless it actually ran in the current repository state.

`Commands Passed` must contain only passing commands. Put accepted failures in `Known Failures` with the reason and evidence.

A dirty working-tree baseline must name the changed files in `Notes` or link to a dated log/testing note that does.

`full` means all real verification commands configured in `docs/context/project-context.md`. Commands explicitly marked `none` are excluded and should be noted.
