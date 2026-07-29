# MA5 测试隔离性审计报告（A5.5）

> Plan: `docs/plans/2026-07-29-1430-2-ma5-cross-cutting-test-audit.md` Phase 1
> Roadmap: `docs/backlog/audit-remediation-roadmap.md` A5.5
> Skill: `docs/skills/open-ended-audit-prompt.md`
> Date: 2026-07-29
> 范围：全域 `module-*/src/test/java/**`（JUnit 层）+ `tests/e2e/`（Playwright 层，隔离性横切）
> M0 锚点：HEAD=`0e963531d`（`docs/audits/compliance-baseline.md §M0 锚点注记`）

## 1. 审计目标与方法

按 `open-ended-audit-prompt.md` 对全域测试隔离性做系统性闭包审计：

1. **验证 6 项已知污染物当前状态**（5 项原 Category (a) + 第 6 项执行期发现 fin-period-close-wizard config），对照 `docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md` 确认是否真解除。
2. **主动搜索新污染物**：跨测试状态泄漏 / 共享静态/单例夹具 / 未清理 lifecycle / 顺序依赖 / 硬编码种子 id 断言。
3. **评估隔离性根因模式**：是否需提升为 lesson / skill。

反窄化自检：本审计不局限于核实已知 5 项是否解除，而是把视野拉到「JUnit 层与 E2E 层的隔离机制结构性差异」+「全域测试夹具卫生」项目级视角。

## 2. 核心结论

**Verdict: PASS（零 P0 + 1 项 P2 watch-only）**

- 6 项已知污染物**全部确认解除**（§3 裁决表）。
- JUnit 层（344 测试文件）**结构性隔离成立**——Nop 平台 `localDb=true` 机制为每个测试类分配唯一 H2 内存库（`jdbc:h2:mem:UUID`），`@BeforeEach` 经 `container.restart()` + `initDatabaseSchema` 重建 schema，跨类/跨方法状态泄漏在平台层被阻断（§4 机制源码实证）。
- E2E 层（258 spec）共享单一种子库，隔离依赖 `finally` 块 cleanup 纪律——**6 项已知污染物全部集中于 E2E 层**，JUnit 层零污染物。
- 主动搜索（§5）未发现新活跃污染物：0 static 可变字段 / 仅 1 文件涉及 `@Transactional`/cleanup（平台自身）/ 308/344 测试显式 `localDb=true`，余 2 个为元数据/作业加载测试不触业务 DB。

## 3. 已知污染物状态裁决表（6 项）

| # | 污染物 | 层 | 07-23 状态 | 当前状态 | 证据 | 裁决 |
|---|--------|-----|-----------|---------|------|------|
| 1 | inv totalValue 10450→16950（+6500） | E2E | FAIL（漂移） | **解除** | `cleanupStockMove` 按 `(materialId,warehouseId)` 删 `ErpInvStockBalance`，4 个 wrapper（cleanupP2p/cleanupO2c/cleanupMfg/cleanupSubcontract）在 `finally` 调用；2026-07-25 全量诊断 dump balances=[] diff=0 | ✅ RESOLVED |
| 2 | mfg KPI 漂移（inProcessCount/periodCompletedQty） | E2E | FAIL（漂移） | **解除** | `cleanupMfg`（`orchestration/_helper.ts:916`）按依赖反向删 WorkOrder+Line+JobCard+TimeLog+MaterialIssue+BOM+移动单+余额+凭证+批次基因链+成本差异；dump mfgKpi 精确匹配基线 | ✅ RESOLVED |
| 3 | md KPI 漂移（materialCount/customerCount/vendorCount） | E2E | FAIL（漂移） | **解除** | 创建测试专用物料/往来的 spec 均 `finally` 调 `deleteById`；dump materialCount=4/customerCount=2/vendorCount=2 精确匹配 | ✅ RESOLVED |
| 4 | findMaterialWithoutSkuAlert 非空 | E2E | FAIL（非空） | **解除** | 同 #3 cleanup 覆盖 + 制造链测试专用组件物料由 cleanupMfg 删；dump noSkuAlert=[] 空 | ✅ RESOLVED |
| 5 | o2c-chain COGS 1200→1150（avgCost 漂移） | E2E | FAIL（漂移） | **解除** | `runMfgChain`（`_helper.ts:698`）改用**测试专用新建组件物料**（`E2E-MFG-MAT-{ts}`）而非种子 MAT-001，组件出库只改测试物料余额；dump o2c COGS=1200 精确匹配 | ✅ RESOLVED |
| 6 | fin-period-close-wizard config 缺口 | E2E | FAIL（全量）/ PASS（隔离） | **解除** | `playwright.config.ts` + `_tmp-server.sh` webServer JVM args 补齐 4 键（ap-subject-code=2202 / exchange-gain-loss-subject-code=6603 / current-year-profit-subject-code=4103 / auto-depreciation-on-close=false）；fresh-DB 全量执行 test #151 由 ✘→✓ | ✅ RESOLVED |

**关键观察**：6 项污染物**全部集中于 E2E 层**（共享种子库 + cleanup 纪律），JUnit 层零污染物。这不是巧合——两层隔离机制结构性不同（§4）。

## 4. 隔离机制结构性分析（JUnit vs E2E）

### 4.1 JUnit 层——平台级结构性隔离（强）

源码实证（`../nop-entropy/nop-autotest/`）：

| 机制 | 源码位置 | 行为 | 隔离粒度 |
|------|---------|------|---------|
| 唯一 H2 内存库 | `NopTestConfigProcessor.java:34-40` | `@NopTestConfig(localDb=true)` 设 `jdbc:h2:mem:<UUID>;CASE_INSENSITIVE_IDENTIFIERS=TRUE`，UUID 每测试类唯一 | **每测试类独立 DB** |
| IoC 容器重启 | `AutoTestCase.java:211-225`（`initBeans`） | `@BeforeEach` 调 `container.restart()` 重建 bean 环境 | **每测试方法独立 bean 容器** |
| schema 重建 | `@NopTestConfig(initDatabaseSchema=TRUE)` + `CFG_INIT_DATABASE_SCHEMA` | 容器重启后 ORM 按模型重建表结构 | **每方法空表起点** |
| 快照恢复 | `AutoTestCaseDataBaseInitializer.java:69-88` | CHECKING 模式从 `_cases/input/tables/` CSV 恢复确定性数据 | **每方法确定性种子** |
| 配置重置 | `NopJunitExtension.java:31` `beforeAll` | `AppConfig.getConfigProvider().reset()` | **每类配置隔离** |

**实测分布**：

- 344 测试文件中 308（89.5%）显式 `@NopTestConfig(localDb=true)`；3 个 `TestStub*` 是跨域 I\*Biz 测试替身（非测试）；余 33 个为纯逻辑测试（`BaseTestCase`/`JunitBaseTestCase`，无 DB 无 IoC，如 `TestErpDateRanges` 42 方法测纯函数 helper）。
- 仅 2 个测试不用 `localDb`：`app-erp-all/.../TestModuleMetaReader`（模块元数据扫描，不触业务 DB）+ `TestErpAllJobYamlLoading`（作业 yaml 加载校验，不触业务 DB）——均非业务数据测试，无隔离风险。
- **跨方法状态验证**：`TestErpFinVoucherTemplateCrudSmoke` 5 个 `@Test` 方法均用 `code=SMOKE-FIN` 创建同一实体，若方法间共享持久状态会触发唯一约束冲突。实测 `mvn test` 全绿（基线 2026-07-25），证明每方法从空表起点执行（container.restart + schema 重建）。
- 0 个 static 可变字段（`rg 'static (?!final)'` 全域测试目录零命中）；35 个文件含 `@BeforeEach/@AfterEach`（绝大多数是 `JunitAutoTestCase.init/destroy` 平台自身）；仅 1 个文件涉及 `@Transactional`/cleanup 关键字。

**结论**：JUnit 层隔离在 Nop 平台层结构性成立，业务测试无法绕过——只要继承 `JunitAutoTestCase` + `@NopTestConfig(localDb=true)`（本项目 100% 遵守），跨测试状态泄漏不可能发生。

### 4.2 E2E 层——共享库 + cleanup 纪律（中等，靠纪律非结构）

- 258 spec 共享**单一种子 H2 库**（`db/erp.mv.db`，webServer 启动加载 91 CSV 种子）。
- 无 per-spec DB 重置机制（`SKIP_WEBSERVER=1` 模式下库贯穿全套件）。
- 隔离完全依赖 `tests/e2e/orchestration/_helper.ts` 导出的 cleanup 原语在 `finally` 块调用：
  - `cleanupP2p` / `cleanupO2c` / `cleanupMfg` / `cleanupSubcontract`（链路级，删头单据+子表+余额+凭证+辅助账+测试专用物料）
  - `cleanupStockMove`（按 `(materialId,warehouseId)` 删余额行）
  - `cleanupVoucherByBillCode` / `cleanupArApByCode` / `cleanupPeriod`（按 code 删财务产物）
  - `deleteById`（单实体，30+ spec 在 `finally` 调用）
- **风险结构**：cleanup 纪律是约定非强制——新增写侧 spec 漏调 cleanup 原语即引入新污染物（正是 07-23 5 项的发生机制）。

## 5. 主动搜索新污染物（开放式）

### 5.1 JUnit 层——未发现新污染物

| 搜索维度 | 方法 | 结果 |
|---------|------|------|
| 共享 static/单例夹具 | `rg 'static\s+(?!final)' module-*/src/test` | **0 命中** |
| 未清理 lifecycle | `@BeforeEach/@AfterEach` 全域 | 35 文件，均为 `JunitAutoTestCase` 平台自身 init/destroy，无业务 cleanup 遗漏 |
| 顺序依赖 | 检查多方法类（最大 `TestErpDateRanges` 42 方法） | 纯函数测试无 DB；DB 测试每方法自建数据（如 `SMOKE-FIN` 复用证明空表起点） |
| 硬编码种子 id 断言 | `rg 'eq|equals.*\b[1-9]\b'` 测试目录 | JUnit 用 `localDb` 自建数据，不依赖全局种子 id（与 E2E 不同） |
| `@Transactional`/`@Rollback` 滥用 | 全域 grep | 仅 1 文件命中（平台基类），业务测试零滥用 |

### 5.2 E2E 层——未发现新活跃污染物，识别 1 项结构性 watch

- 主动核对 cleanup 原语覆盖完整性：`cleanupStockMove`（余额）/ `cleanupMfg`（制造链）/ `cleanupSubcontract`（委外链）/ `cleanupP2p`·`cleanupO2c`（业财链）/ `cleanupVoucherByBillCode`·`cleanupArApByCode`·`cleanupPeriod`（财务产物）/ `deleteById`（单实体）——覆盖全部已知写路径产物，无遗漏链路。
- 唯一已知失败 `master-data.write.amis`（1 spec）经核为**测试基础设施 Non-Goal**（AMIS form-button selectOption↔switch 写周期不可达，`crud/_helper.ts:174`），**非隔离污染物**（隔离首跑亦失败，非跨 spec 状态泄漏）——plan 2026-07-24-1945-1 Non-Goals 显式排除。
- **结构性 watch**：E2E 无 per-spec DB 隔离，cleanup 纪律回归即污染物重现（07-23 → 07-25 的收敛完全靠既有 cleanup 代码 + 生产侧删除语义改善，非新增防护）。登记 P2-MA5-005。

## 6. 隔离性根因模式评估

| 维度 | 评估 | 提升 |
|------|------|------|
| JUnit 层根因 | 平台 `localDb=true` 结构性隔离，业务测试零额外纪律负担 | 无需 lesson——平台文档 `../nop-entropy/docs-for-ai/02-core-guides/testing.md` 已权威记录，本项目 `docs/skills/nop-testing/SKILL.md` 已路由 |
| E2E 层根因 | 共享库 + cleanup 纪律约定（finally 块） | 诊断方法已沉淀于 `docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md` §1（诊断 spec 尾部 dump + fresh-DB 累积执行 + code 前缀定位）——**已充分沉淀，无需新 lesson/skill** |
| 防护网 | E2E 全量门控（490 passed/1 failed 基线）+ 诊断 dump 方法可复现 | 07-23 全量门控是发现污染物的有效机制，维持周期性全量回归即可 |

**结论**：隔离性根因模式已在 plan 2026-07-24-1945-1 + analysis 文件充分沉淀，本轮审计确认无新失败模式需提升为 lesson/skill。E2E cleanup 纪律的「约定非强制」特性登记 P2 watch（§7）。

## 7. Finding 登记

### P2-MA5-005（watch-only）— E2E 共享库隔离结构性脆弱

- **域/层**：`tests/e2e/`（Playwright 层，全域）
- **描述**：258 spec 共享单一 H2 种子库，无 per-spec / per-suite DB 重置机制。测试隔离完全依赖 `_helper.ts` 导出的 cleanup 原语在 `finally` 块手动调用（约定非强制）。2026-07-23 全量门控曾因 cleanup 纪律不足爆发 5 项 Category (a) 污染物（07-25 经既有 cleanup 代码 + 生产侧删除语义改善收敛）。新增写侧 spec 若漏调 cleanup 原语，污染物将重现（表现为受害数值断言 spec 间歇性 FAIL——隔离首跑 PASS / 全量 FAIL）。
- **当前状态**：**无活跃污染物**（07-25 基线 490 passed / 1 failed [Non-Goal] / 3 skipped，诊断 dump 0 残留）。结构性风险存在但未显现。
- **严重性**：P2 watch-only（非活跃缺陷 + 已有诊断方法可快速定位 + cleanup 原语已完备）
- **目标 MR**：MR3（测试维度）。修复选项：(A) 引入 per-suite DB 重置（`globalTeardown` 重启 webServer + fresh-DB，成本高）；(B) 强化 cleanup 纪律 guard（lint 规则检测写侧 spec 未调 cleanup，成本低）；(C) 维持现状 + 周期性全量回归门控（当前实践，可接受）。
- **去重**：与 P1-MA5-001~011（S 级域测试覆盖深度，A5.1-A5.4）无重叠——本项是跨切 E2E 基础设施隔离机制，非域覆盖深度。

## 8. Exit Criteria 核对

- [x] 隔离性报告产出，含 6 项已知污染物状态裁决表（§3，全部 RESOLVED）+ 新污染物搜索结果（§5，零活跃污染物 + 1 项结构性 P2 watch）
- [x] A5.5 P0/P1/P2 已登记 arm-index.md 且去重（P2-MA5-005，与 MA1-MA4 + plan 2026-07-29-1430-1 既有 P1 经交叉去重无重叠——维度不同：跨切 E2E 隔离机制 vs 域覆盖深度 vs 结构/业务/文档/代码质量）

## 9. 备注

- 本审计为纯只读审计，零代码/ORM/契约变更。
- `mvn test` + E2E 回归仅作基线确认（plan Closure Gates 声明）。
- 诊断方法可复现性已由 `docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md` §1 保证。
