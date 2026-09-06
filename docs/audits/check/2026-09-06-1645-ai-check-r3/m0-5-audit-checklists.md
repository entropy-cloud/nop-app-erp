# M0.5 五维符合性审计检查清单（冻结版）

> **冻结声明**：本清单是 ai-check-r3 M1.1~M1.16 全部审计切片的**强制核对矩阵**（roadmap M0.5 交付物，plan `docs/plans/2026-09-06-1451-3-m0-audit-checklists-closure.md` Phase 1 产出）。清单一经 M0.6 收官确认即冻结；M1.x 执行期间只允许在 §3.3 各格的「查重裁决」列追加证据行，不允许修改判定标准、锚点路径或核查程式（发现锚点漂移按 finding 登记，不就地改清单）。
>
> **核对单元**：19 业务域 + common-service + app-erp-all 横切 = **21 格**；五维（DIM-B / DIM-F / DIM-S / DIM-T / DIM-I）× 21 格 = **105 基础格**。S 级域（finance / manufacturing / assets / hr）格内按功能模块再分切片子行（§3.2，11 切片），与 M1.1~M1.11 工作项一一对应。
>
> **禁止抽样、禁止跳维**：每个 M1.x 切片必须按本清单对齐格集合五维全跑并产出覆盖矩阵（`<执行目录>/ck-<slice>.md`）；完成判据 = 覆盖矩阵完整（M1.17 以本清单完整性校验为准，缺一格不算完）。

## 0. 强制使用规则

1. **五维全跑**：M1.x 每切片五维逐格核对，逐格落 verdict（`pass` / `finding` / `n-a（记录理由）`），禁止抽样、禁止跳维。
2. **MI 先行时序（DIM-I 专属口径）**：roadmap 依赖图 M1.x 依赖 MI.9；M1.x 审计的 DIM-I 维**仅验证零回归与白名单合规，不再产生同类 finding**（详见 §1.5 内嵌口径）。
3. **跨轮查重（全维强制列）**：每候选 finding 立项前必做三态裁决（§2）：同型已 fixed 复用范式 / 同型 open 归并原 ID / 确属新发才立 `-r3` 新 ID。历史 ID 永不覆写。
4. **共享代码唯一归属**：跨切片共享代码（posting processor 族、common 抽象族、聚合横切面）的唯一归属切片已在 §3.2 显式登记；非归属切片发现疑似共享代码缺陷 → 归并到归属切片的格，不在本切片重复立项。
5. **两阶段时序硬约束**：M1.x 只产 finding 不改任何代码/ORM/配置/页面文件；切片收官 `git status` 必查零生产代码改动（roadmap 规则 6）。
6. **与 r1/r2 边界**：本轮不接管 r1 行为 finding 修复与 r2 三路交叉审计的工作项（roadmap 横切关注点 5）；查重命中 r1/r2 既有 finding 时按 §2 归并，不重复立项、不重复修复。

## 1. 五维判定标准与机械核查程式

> 每维三件套：权威 owner doc 锚点路径（均已实仓核验可解析）+ 判定标准 + 可机械执行的 grep/脚本核查程式。通用程式在此定义一次；§3.3 各格只写**单元内增量**（域内焦点 + 域内增量命令 + 查重指针）。

### 1.1 DIM-B — 后端平台合规

**锚点**：

- `docs/skills/nop-platform-conformance-audit-prompt.md` — **15 维度**：①决策顺序 Model→Delta→Java ②跨实体访问规则 ③异常处理 ④IoC 与事务 ⑤平台辅助工具 ⑥标准服务模式 ⑦跨模块外部实体引用（机制 B）⑧状态机与规则引擎 ⑨审批流与作业 ⑩定制能力使用顺序 ⑪多租户与本地化 ⑫测试与验证 ⑬Codegen 产物安全意识 ⑭聚合完整性检查 ⑮Owner-doc→代码关键断言抽样核查（漂移检测）
- `docs/audits/nop-compliance-checker.sh` — R1-R12 数值门控（R1a-d / R2a-d / R3-R8 / R10-R12a-c）
- `docs/audits/compliance-baseline.md` §BASELINE (machine-readable) — 基线块；冻结现值快照：R2c=1542（known-good-baselines 2026-09-06 `ai-check-r3-m0` 行，全表 R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）
- 平台判定依据：`../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md` + `04-reference/safe-api-reference.md` + `04-reference/common-java-helpers.md`
- 项目架构：`docs/architecture/processor-extension-pattern.md`、`docs/architecture/module-boundaries.md`、`docs/architecture/system-baseline.md`

**判定标准**：

- 15 维度逐维走查，按 prompt 严重性指南分级（blocker：手改生成代码/跨模块写反向/异常不扩展 NopException/@Inject private；major：硬编码可模型化逻辑/绕过 I*Biz/冗余 @Transactional；minor：辅助工具第三方/命名不规范）。
- R1-R12 逐规则 actual ≤ baseline；基线条目 = 已裁决偏离（只报告**新增违规**与基线条目中的真实缺陷，不重复报告已裁决偏离）。
- 维度⑮强制抽样：每切片 ≥2 个 owner doc × 2 个关键断言对照代码（状态名/字段名/角色名/迁移路径/ErrorCode），≥2 处漂移扩大抽样至全部 owner doc。

**机械核查程式**（切片范围 = 该格单元的 `module-<domain>/erp-<short>-{dao,service}` 的 `src/main/java`，下记 `<SCOPE>`）：

```bash
# ① 全规则数值门控（机械，逐规则对照 §BASELINE + 2026-09-06 行现值快照）
bash docs/audits/nop-compliance-checker.sh

# ② 反模式 grep 族（切片内 expect 0，基线例外见括号）
grep -rn "extends RuntimeException" <SCOPE>                                   # (0)
grep -rn -A1 "@Inject" <SCOPE> | grep "private"                               # (0)
grep -rn "System.currentTimeMillis" <SCOPE>                                   # (0)
grep -rln "@Transactional" <SCOPE> | xargs grep -l "@BizMutation" 2>/dev/null # (对照基线 R6=2)
grep -rn "IDaoProvider\|IOrmTemplate\|@SqlLibMapper" <SCOPE>                  # 命中处须有注释理由
# ③ Codegen 产物安全（维度⑬，机械）
git status --porcelain "<SCOPE 以 _ 或 _gen 命中的产物路径>"                   # expect 空
grep -rn "__XGEN_FORCE_OVERRIDE__" <模块> --include='*.yaml' --include='*.xml' | grep -v '_gen/'  # 命中=只读校验点
# ④ 聚合完整性（维度⑭，机械）
grep 'x:extends' app-erp-all/src/main/resources/_vfs/erp/all/auth/app.action-auth.xml  # 新模块注册核对
# ⑤ 维度①②⑦⑧⑨⑩⑪⑮：按 prompt 各维判定表逐维走查并落 verdict（程序式确定性走查，无脚本面）
```

### 1.2 DIM-F — 前端页面与 E2E

**锚点**：

- `docs/architecture/view-and-page-strategy.md` — 核心原则 / 文件层次结构 / 渲染模式（flux-only，强制）/ 页面数据访问（/r/ REST 约定，强制）/ 代码生成 vs 手写边界 / 国际化策略
- pattern docs：`docs/design/page-structure-patterns.md`、`docs/design/child-table-editor-patterns.md`、`docs/design/field-formatting-patterns.md`、`docs/design/picker-patterns.md`、`docs/design/batch-operation-patterns.md`、`docs/design/non-standard-views-patterns.md`、`docs/design/cross-doc-navigation-patterns.md`、`docs/design/date-ranged-validity-pattern.md`、`docs/design/flux-complex-pages.md`、`docs/design/dashboards.md`
- `docs/testing/e2e-runbook.md` — §渲染模式与 flux 调试三路径（必读）+ §E2E 编写规范（强制）（flux 引擎强制 / PageObject 模式 / 选择器与定位约定 / API 断言 REST /r/）
- `docs/architecture/flux-page-export-and-validation.md` + `docs/architecture/flux-integration-gotchas.md`

**判定标准**：

- flux-only：菜单资源 `component="FLUX"`；ORM 实体带 `ext:web-renderer="flux"`；服务器 `nop.web.render-mode: flux`；禁止新 AMIS 页面。
- 页面数据访问走 REST `/r/`（`@query:` / `@mutation:` / `@rpc:` / 显式 `/r/`）；flux 页面禁止断言 GraphQL。
- E2E 编写规范：PageObject 模式强制；selector 唯一合法位置 = adapter 层；spec 只见业务概念；`E2E_ENGINE` 缺省 flux，禁止产出新 AMIS 用例。
- codegen 页面产物（`_gen/`、`_` 前缀）禁手改；定制走保留层 `x:extends`；页面 yaml 源头链判定以 M0.4 矩阵为准。
- 界面标签 i18n：模型源 `i18n-en:displayName` / `i18n-en:label` 约定（英文承载义务与 DIM-I CAT-4 衔接）。

**机械核查程式**（`<WEB>` = 该格单元 `module-<domain>/erp-<short>-web/src/main/resources/_vfs`）：

```bash
# ① 全局面静态门禁（机械；m2 新鲜度警示：改过页面 yaml 先 mvn install 该模块再跑）
npm run validate:flux                                    # exit 0 = 999 页导出 + flux 编译全通过
# ② flux-only（机械）
grep -rn 'component="AMIS"' module-*/erp-*-web  # 保留层 expect 0（迁移期豁免以豁免清单登记为准）
grep -L 'ext:web-renderer="flux"' module-*/model/app-erp-*.orm.xml              # expect 空
# ③ E2E 纪律（机械，涉及该域 spec 时）
grep -rln 'data-slot\|data-testid\|\.cxd-' tests/e2e --include='*.spec.ts'      # expect 0（adapter 层外）
# ④ 页面 yaml 源头链（codegen/手写判定）查表
#    docs/audits/check/2026-09-06-1645-ai-check-r3/m0-4-page-yaml-source-map.md
# ⑤ CAT-4 页面 yaml 英文承载（与 DIM-I 共用工具）
node tools/check-hardcoded-cjk.mjs --strict
```

### 1.3 DIM-S — seed 数据

**锚点**：

- `docs/architecture/seed-data.md` — **快照重录义务**（seed CSV/SQL 变更 → 面 1 各域 `_cases` + 面 2 app-erp-all 集成快照双面重录 + 提交说明登记）/ **deploy 种子 ↔ 聚合 `_init-data` 同步义务**（登记处表）/ **通用引用完整性校验**（`TestErpSeedDataIntegrity` 门禁：363 实体全量 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数门控）/ 各域 seed 范式与 posted 一致性裁决 / 域内金额自洽约束

**判定标准**：

- `TestErpSeedDataIntegrity` 全绿（全表可加载 + 非空关联键零悬空 + 零孤儿 CSV + 白名单零无登记增量）。
- 模块 deploy `_seed_*.sql` 与聚合 `_init-data` 同步：已聚合或已显式登记 Non-Goal，无第三态。
- 任何 seed 资产变更必须触发双面快照重录 + 提交说明登记重录范围（M1.x 为只读审计，正常态 = 零 seed 变更；发现历史变更未履行重录 = finding）。
- seed 静态行不被引擎重算覆盖（域内 config 门控默认关，如 SPC/CRP 双层门控）。
- posted 一致性：`posted=true` 当且仅当有对应凭证回链（域内按 seed-data.md 各批裁决抽查）。

**机械核查程式**：

```bash
# ① 引用完整性门禁（机械）
mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity        # 全绿
# ② seed 资产清点（对照 seed-data.md 对账表最新登记行：M1.5 批次后 = 372 CSV + 1 SQL）
ls app-erp-all/src/main/resources/_vfs/_init-data/*.csv | wc -l
# ③ deploy 种子同步义务（机械 + 逐命中查登记处表）
find module-* -path '*deploy/sql*' -name '_seed_*.sql'
# ④ seed 变更检测（只读审计 expect 空；非空 → 查双面重录证据）
git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/
```

### 1.4 DIM-T — 单元测试

**锚点**：

- `docs/architecture/testing-strategy.md` — **覆盖要求表**（BizModel 每公开方法 ≥1 测试 / 状态机每迁移 ≥1 / 每跨域场景 ≥1 / 每 ErrorCode ≥1）/ **SnapshotTest 纪律**（RECORDING 录制→人工审查→CHECKING 校验；提交态零 RECORDING 残留）/ **test-depth-classification**（三层测试 L1 单元 L2 集成 L3 E2E + 覆盖深度按上表分类）/ 测试基类 `JunitAutoTestCase` + `IGraphQLEngine` / `request.json5` + `@var` 机制 / 自包含设计 / 四类测试资产边界 / 跨域业务流测试归属三层规则 / 异步过账测试时序模型 / P0-P2 关键业务流快照测试清单
- 测试技能路由：`docs/skills/README.md`（nop-testing 技能：测试基类选择 / @NopTestConfig / 快照录制回放）

**判定标准**：

- 覆盖要求表四项逐项核对，缺口 = finding（P2 起，按缺口业务关键度定级）。
- 快照纪律：提交态测试不得残留 `SnapshotTest.RECORDING`（录制态是临时态）；`delVersion`/decimal 列 `*` 通配屏蔽合规；CREATE_TIME/UPDATE_TIME 框架自动屏蔽。
- 跨域归属三层规则：触发域 owner 全链路一份 / 被调域仅契约 / 系统不变量归 app-erp-all——重复全链路或归属错位 = finding。
- 切片本地回归全绿（`mvn test -pl module-<domain>/erp-<short>-service`）。

**机械核查程式**（`<SVC>` = 该格单元 `module-<domain>/erp-<short>-service`）：

```bash
# ① 切片本地回归（机械）
mvn test -pl module-<domain>/erp-<short>-service               # 全绿
# ② 覆盖缺口对账（机械枚举 + 人工核对缺口业务关键度）
grep -rc "@BizQuery\|@BizMutation" <SVC>/src/main/java --include='*BizModel.java'   # 公开方法清单
ls <SVC>/_cases/app/erp/<short>/service/                                            # 测试方法目录清单
# ③ 快照纪律（机械）
grep -rn "SnapshotTest.RECORDING" <SVC>/src/test/java            # expect 0（提交态）
# ④ 关键业务流清单核对：对照 testing-strategy §关键业务流快照测试清单（P0/P1/P2）逐行核覆盖
```

### 1.5 DIM-I — i18n 与日志语言

**锚点**：

- `docs/architecture/i18n-compliance.md`（唯一权威）— 判定准绳表（8 类裁定）/ 白名单登记格式（四要素：文件路径+理由+owner doc 指针+裁决来源）/ 修复模式对照表（CAT-1 LOG 英文化 / CAT-2 异常参数传码 / CAT-3 运行时字符串三选一 / CAT-4 页面 yaml i18nEn 或模型源 i18n-en）/ 探针口径指针
- `tools/check-hardcoded-cjk.mjs` — 五类分级 + `--baseline` 快照 + `--strict` 门控 + `--self-test` 自证
- `docs/audits/cjk-baseline.md` — SNAPSHOT 块（单向收紧）+ WHITELIST 块（唯一豁免通道）
- `docs/design/i18n-glossary.md` — 英译术语冻结基准（414 token，新词先扩表再使用）

> **内嵌 MI 先行时序口径（强制，DIM-I 专属）**：MI.9 已完成（i18n 面已清零）后，M1.x 审计的 DIM-I 维**仅验证零回归与白名单合规，不再产生同类 finding**——判定 = `--strict` 绿（CAT-1/2/3/4 = 0 或白名单显式登记）+ 抽中白名单条目四要素齐全。脚本红 = MI 回归（升级裁决报 MI 通道），**不立 DIM-I 新 finding**；白名单四要素缺失 = 白名单登记缺陷 finding（非同类 CJK finding）。本口径与 roadmap §M1「与 MI 时序」节一致。

**机械核查程式**：

```bash
# ① 零回归门控（机械，全域跑）
node tools/check-hardcoded-cjk.mjs --strict                     # exit 0（对照 cjk-baseline.md SNAPSHOT 单向收紧）
# ② 白名单合规抽查（机械枚举 + 人工核对四要素：WHITELIST 块抽 ≥3 条/切片）
#    docs/audits/cjk-baseline.md WHITELIST 块
# ③ @Locale 声明（机械，MI.1 后 expect 空）
grep -L "@Locale" $(find module-* -path '*src/main*' -name '*Errors.java')
# ④ `_` 前缀生成 i18n yaml 禁手改（机械）
git status --porcelain 'module-*/erp-*-meta/**' 'app-erp-all/src/main/resources/_vfs/i18n/**'   # expect 空
# ⑤ 页面 yaml 英文承载用语核对：新增 i18nEn/i18n-en 值 ∈ i18n-glossary.md 或已扩表
```

## 2. 跨轮查重程序（全维统一，每候选 finding 立项前强制）

1. **查 r1**：`docs/audits/check/ai-check-index.md` §报告清单定位同域报告（`ck-<slice>.md`，r1 已登记 21+ 报告行）→ §Finding 追踪逐条比对同型（维度 + 缺陷形态）。
2. **查 r2**：`docs/audits/check/2026-08-28-2049-ai-check-r2/`（只读）— `ai-check-r2-index.md` + `m0-3-open-findings-bucketing.md` + `m0-6-cross-mission-reuse-map.md` 比对同型。
3. **查 mission 基线快照**：`ai-check-index.md` §Mission 基线快照（合规基线表）确认候选发现是否为已裁决偏离。
4. **三态裁决**（裁决结果落 ck-*.md §3 与跨轮索引，不得跳过）：

| 裁决 | 条件 | 动作 |
| --- | --- | --- |
| **复用** | 同型已 `fixed` | 引用原 ID 终态证据，复核修复在 HEAD 仍有效；无新增 finding（ck-*.md 注明"复用已 fixed"） |
| **归并** | 同型 `open` / `deferred` | 归并原 ID 追加新证据；若需升级级别按 §3.1 新立 `-r3` 后缀 ID，原 ID 状态不动 |
| **新立** | 确属新发，无历史同型 | 新 ID 加 `-r3` 后缀（`P{0-3}-CK-{域短码}-{NNN}-r3`） |

- ID 冲突细节（deferred 触发条件已满足→新立 + 原 ID 置 reopened 等）按 `docs/skills/code-history-deferred-triangulation-audit-prompt.md` §3.1 执行；**历史 ID 永不覆写**。

## 3. 21 核对单元 × 五维核对矩阵

### 3.1 单元清单与物理路径

| # | 核对单元 | 物理模块路径（`<SCOPE>` 基准） | 短码 |
|---|---|---|---|
| U01 | master-data | `module-master-data/erp-md-*` | md |
| U02 | inventory | `module-inventory/erp-inv-*` | inv |
| U03 | purchase | `module-purchase/erp-pur-*` | pur |
| U04 | sales | `module-sales/erp-sal-*` | sal |
| U05 | finance（S 级，4 切片） | `module-finance/erp-fin-*` | fin |
| U06 | assets（S 级，2 切片） | `module-assets/erp-ast-*` | ast |
| U07 | projects | `module-projects/erp-prj-*` | prj |
| U08 | manufacturing（S 级，3 切片） | `module-manufacturing/erp-mfg-*` | mfg |
| U09 | quality | `module-quality/erp-qa-*` | qa |
| U10 | maintenance | `module-maintenance/erp-mnt-*` | mnt |
| U11 | notify | `module-notify/erp-notify-*` | notify |
| U12 | crm | `module-crm/erp-crm-*` | crm |
| U13 | cs | `module-cs/erp-cs-*` | cs |
| U14 | hr（S 级，2 切片） | `module-hr/erp-hr-*` | hr |
| U15 | aps | `module-aps/erp-aps-*` | aps |
| U16 | logistics | `module-logistics/erp-log-*` | log |
| U17 | b2b | `module-b2b/erp-b2b-*` | b2b |
| U18 | contract | `module-contract/erp-ct-*` | ct |
| U19 | drp | `module-drp/erp-drp-*` | drp |
| U20 | common-service（+common-test 横切支撑） | `module-common-service/*` + `module-common-test/*` | common |
| U21 | app-erp-all 横切 | `app-erp-all/*`（聚合/seed 装载/菜单/集成测试/flux 导出门禁） | app |

### 3.2 S 级域切片粒度登记（11 切片）与共享代码唯一归属裁决

**Decision（冻结）**：S 级域按功能模块拆分切片，C 级域合并；替代方案（S 级域整域单切片）因单会话不可完成被否决（roadmap 规则 2 工作项粒度）。11 切片与 M1.1~M1.11 一一对应：

| 域 | 切片 | 工作项 | 功能模块范围 |
| --- | --- | --- | --- |
| finance | fin-1 过账与凭证 | M1.1 | posting 引擎/凭证/红冲/sweep/过账日志（owner：`docs/design/finance/posting.md`） |
| finance | fin-2 AR/AP 核销与坏账 | M1.2 | 核销/坏账/费用报销抵扣（`ar-ap-reconciliation.md` + `bad-debt.md`） |
| finance | fin-3 预算与成本 | M1.3 | 预算控制/成本方法/成本中心（`budget.md` + `costing-methods.md`） |
| finance | fin-4 期间结账与银行对账 + 跨域凭证链路 | M1.4 | 期间/结账/银行对账/合并抵销/跨法人（`period-close.md` + `bank-reconciliation.md`） |
| manufacturing | mfg-1 工单与报工 | M1.5 | 工单/作业卡/领料/预留/完工入库（`docs/design/manufacturing/state-machine.md`） |
| manufacturing | mfg-2 BOM/MRP/CRP | M1.6 | BOM/路由/MRP/仿真/CRP（`mrp.md` + `crp.md`） |
| manufacturing | mfg-3 委外/批次追溯/差异 | M1.7 | 委外/批次基因/成本差异（`subcontracting.md` + `batch-genealogy.md` + `variance-analysis.md`） |
| assets | ast-1 资产生命周期 | M1.8 | 资产建档/变动/盘点（`docs/design/assets/state-machine.md` 等） |
| assets | ast-2 折旧与过账 + 盘点 | M1.9 | 折旧/资产过账/盘点闭环（`depreciation-and-posting.md`） |
| hr | hr-1 组织与员工 | M1.10 | 组织/岗位/员工/编制（`docs/design/human-resource/state-machine.md` 等） |
| hr | hr-2 考勤、薪酬与排班 | M1.11 | 考勤/薪酬模拟/排班（`payroll.md` + `shift-scheduling.md`） |

**共享代码唯一归属裁决（残留风险控制：防重复审计、防归属真空）**：

| 共享代码族 | 唯一归属 | 边界规则 |
| --- | --- | --- |
| **posting processor 族**（`ErpFinPostingService`/凭证引擎/dispatcher 族/`FinPostedListener`/sweep/`PostingRun`/`posting.md` §同步测试缝） | **fin-1（M1.1）** | 引擎/processor **内部**缺陷一律归 fin-1；跨域消费点（sal 发票红冲前置、inv 凭证 REQUIRES_NEW、mfg 完工入库移动、ast 折旧过账）的**消费侧行为**归各域切片，消费侧 finding 涉及引擎内部时标注「归属 fin-1」归并 |
| **common 抽象族**（`AbstractProcessor.illegal*` helper、`AbstractErpCrudBizModel`/`AbstractErpImmutableCrudBizModel` 状态锁基类、共享 config/错误码基建） | **U20 common（M1.15）** | 各域切片只审计**调用点**合规；基类行为缺陷归 U20 格 |
| **聚合横切面**（app-erp-all 聚合完整性/seed 全量装载/菜单/action-auth 聚合器/集成测试快照/flux 导出门禁） | **U21 app-erp-all（M1.16）** | 域内页面/seed 内容缺陷归各域格；聚合机制缺陷归 U21 |
| **notify 派发子系统**（模板驱动派发/收件箱管线） | **U11 notify（M1.15）** | 跨域消费点（各域通知发送调用）在各域切片记录、子系统本体归 U11 |

### 3.3 全矩阵格册（21 单元 × 5 维，105 基础格）

> 每格行：**单元内焦点**（该维在本域的特定判定面）+ **域内增量程式**（引用 §1 通用程式 + 域内增量）+ **跨轮查重指针**（r1 报告 / r2 文件）。格 verdict 落 ck-*.md 覆盖矩阵，本册为冻结判定源。

#### U01 master-data

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 跨域被引用面（partner/material/uom/exchange-rate 被 18 域引用）：机制 B notGenCode、汇率 MUTEX 钩子、价格链 helper | §1.1 全套 + `grep -c 'notGenCode' module-master-data/model/app-erp-master-data.orm.xml` | r1 `ck-master-data.md`（C1.1）；r2 index |
| F | 主数据选择器页（picker 范式）/SKU 多单位 UI | §1.2 + picker-patterns 对照 | 同上 |
| S | 主数据 21 表基座 CSV（org/currency/partner/material/subject…全域 FK 上游）：变更影响全域 seed | §1.3 ①②③④ | 同上 |
| T | 主数据夹具 `app-erp-test-data` 边界（测试资产 vs 部署资产） | §1.4 ①②③ | 同上 |
| I | 主数据字典/名称运行时面 CJK（CAT-3 探针 md=3 等，MI.9 后应零） | §1.5 全套 | 同上 |

#### U02 inventory

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 出库策略族 locationId 回退/自然键余额查找/不可变流水基类/批次序列守卫 | §1.1 ①②③ + 策略族逐类走查（维度⑧状态机） | r1 `ck-inventory.md`；r2 index |
| F | 库存移动确认页（复杂手写页清单成员）/批次序列选择 | §1.2 全套 | 同上 |
| S | stock_move/balance/cost_layer seed（2210-1 批）+ 域内金额自洽（balance↔cost_layer） | §1.3 全套 + seed-data.md 运营域约束段 | 同上 |
| T | P0 库存移动+流水+余额一致性清单行覆盖 | §1.4 全套（清单 P0 行必查） | 同上 |
| I | inv 运行时面（CAT-1 26/CAT-2 7 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U03 purchase

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 三单匹配/SoD 前移/状态锁基类消费/容差配置量纲 | §1.1 全套 | r1 `ck-purchase.md`；r2 index |
| F | 三单匹配页（复杂手写页清单成员） | §1.2 全套 | 同上 |
| S | P2P 链 seed（PO→Receive→Invoice→Payment）posted 一致性裁决 | §1.3 全套 + seed-data.md P2P 范式段 | 同上 |
| T | P0 采购到付款全流程清单行覆盖 | §1.4 全套（P0 行必查） | 同上 |
| I | pur 运行时面（CAT-1 22/CAT-2 32 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U04 sales

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 促销规则引擎/取价链/核销状态守卫/红冲前置消费侧行为（归属边界 §3.2） | §1.1 全套 | r1 `ck-sales.md`；r2 index |
| F | 销售链页面（报价/订单/出库/发票）codegen+delta 面 | §1.2 全套 | 同上 |
| S | O2C 链 seed（SO→Delivery→Invoice→Receipt）posted 一致性 | §1.3 全套 | 同上 |
| T | P0 销售到收款全流程清单行覆盖 | §1.4 全套（P0 行必查） | 同上 |
| I | sal 运行时面（CAT-1 22/CAT-2 30 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U05 finance（S 级 4 切片：fin-1/fin-2/fin-3/fin-4 → M1.1~M1.4）

| 维 | 单元内焦点（按切片细分） | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | fin-1：posting 引擎/凭证平衡/幂等/账套解析（**共享族归属格，§3.2**）；fin-2：核销聚合守卫/FX 对称回滚/坏账现态守卫；fin-3：预算三量口径/TOCTOU 锁/结转科目维度；fin-4：结转账套过滤/FX 重估累计口径/期间竞态 | §1.1 全套逐切片跑；fin-1 额外走查 processor-extension-pattern 对齐 | r1 `ck-finance-posting.md` / `ck-finance-arap.md` / `ck-finance-budget-costing.md` / `ck-finance-period-misc.md`；r2 index |
| F | 凭证录入/凭证模板页（复杂手写页清单成员）；报表/期末结账页 | §1.2 全套 | 同上 |
| S | P2P/O2C 已过账财务产物 seed（voucher/ar_ap_item/gl_balance/期间 OPEN）+ seed-data.md 業财范式段 | §1.3 全套 | 同上 |
| T | P0 凭证生成+过账+红字冲销 / P1 多币种+期末结账+成本核算清单行覆盖；postNow 同步缝纪律 | §1.4 全套 | 同上 |
| I | fin 运行时面（CAT-1 54/CAT-2 20 探针族，MI.9 后应零）；posting dispatcher 族 warn/error 语义不变（横切 8） | §1.5 全套 | 同上 |

#### U06 assets（S 级 2 切片：ast-1/ast-2 → M1.8~M1.9）

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | ast-1：资产生命周期状态机/变动处置；ast-2：折旧批量（nop-job）/资产过账/盘点闭环 | §1.1 全套逐切片 | r1 `ck-assets-lifecycle.md` / `ck-assets-depreciation.md`；r2 index |
| F | 资产台账/折旧表页面 | §1.2 全套 | 同上 |
| S | asset/depreciation_schedule seed（2210-1 批）+ 金额自洽（asset↔最新 schedule） | §1.3 全套 | 同上 |
| T | 折旧→过账链测试覆盖（P1 清单族） | §1.4 全套 | 同上 |
| I | ast 运行时面（CAT-1 39/CAT-2 29± 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U07 projects

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 结算防重守卫（同类型）/工时回滚/成本聚合 | §1.1 全套 | r1 `ck-projects.md`；r2 index |
| F | 项目看板/盈利页 | §1.2 全套 | 同上 |
| S | project/cost_collection/timesheet/budget/project_pnl seed + 金额自洽（pnl↔collection） | §1.3 全套 | 同上 |
| T | P1 工时结算清单行覆盖 | §1.4 全套 | 同上 |
| I | prj 运行时面（CAT-1 14/CAT-2 7 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U08 manufacturing（S 级 3 切片：mfg-1/mfg-2/mfg-3 → M1.5~M1.7）

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | mfg-1：工单双轴状态机（approveStatus×docStatus）/领料红冲三件套回退/完工入库幂等键；mfg-2：MRP 净额归集/低阶码/仿真版本/CRP 负荷链；mfg-3：委外/批次基因/差异公式 | §1.1 全套逐切片 | r1 `ck-mfg-workorder.md` / `ck-mfg-bom-mrp.md` / `ck-mfg-subcontract.md`；r2 index |
| F | 排产/工单看板页面 | §1.2 全套 | 同上 |
| S | work_order/cost_variance/forecast 族 + workcenter 配置链 + crp_load seed（0930-1/0628-1 批）+ 自洽约束 | §1.3 全套 | 同上 |
| T | P1 工单→领料→报工→完工入库→成本结转清单行覆盖 | §1.4 全套 | 同上 |
| I | mfg 运行时面（CAT-1 31/CAT-2 9 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U09 quality

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | SPC 引擎双层门控/NCR 链/I*Biz 化消费点 | §1.1 全套 | r1 `ck-quality.md`；r2 index |
| F | 质检/NCR/SPC 页面 | §1.2 全套 | 同上 |
| S | inspection/non_conformance/action + SPC 三表（1145-2 批 Strategy C）+ 引擎重算覆盖防护 | §1.3 全套 | 同上 |
| T | P1 来料检验→NCR→退货清单行覆盖 | §1.4 全套 | 同上 |
| I | qa 运行时面（CAT-1 1/CAT-2 10 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U10 maintenance

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 设备状态机/维保计划/预警查询 | §1.1 全套 | r1 `ck-maintenance.md`；r2 index |
| F | 设备台账/维保看板页面 | §1.2 全套 | 同上 |
| S | equipment/schedule/request/downtime/visit 族 seed（0930-2 批）+ M1.2b 批全量扩展 | §1.3 全套 | 同上 |
| T | 维护链测试覆盖 | §1.4 全套 | 同上 |
| I | mnt 运行时面（CAT-1 10/CAT-2 7 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U11 notify

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 通知派发子系统（模板驱动/收件箱管线/**子系统本体归属格 §3.2**）/nop-message 跨域事件 | §1.1 全套 | r1 `ck-notify.md`；r2 index |
| F | 通知收件箱（user TOPM 注册，维度⑭特例核对） | §1.2 全套 + app.action-auth.xml user TOPM grep | 同上 |
| S | `erp_sys_notification_template.csv`（27 行，0825 聚合先例）+ deploy 同步登记处 | §1.3 全套 | 同上 |
| T | 通知派发集成测试（C16 族快照纪律） | §1.4 全套 | 同上 |
| I | notify 运行时面（CAT-1 15 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U12 crm

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 线索评分重算/线索瀑布/CPQ 规则 | §1.1 全套 | r1 `ck-crm-lead.md` / `ck-crm-cpq-forecast.md`；r2 index |
| F | 商机看板/活动日历（复杂手写页清单成员） | §1.2 全套 | 同上 |
| S | CRM 5 表 seed（1045-1 批）+ M1.4a 批配置链 | §1.3 全套 | 同上 |
| T | P1 Lead→Convert 清单行覆盖 | §1.4 全套 | 同上 |
| I | crm 运行时面（CAT-1 3 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U13 cs

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 工单 SLA/TK 编号规则消费/满意度链 | §1.1 全套 | r1 `ck-cs.md`；r2 index |
| F | 工单看板（复杂手写页清单成员） | §1.2 全套 | 同上 |
| S | CS 3 表 seed + `nop_sys_code_rule.csv`（String PK 先例） | §1.3 全套 | 同上 |
| T | P1 Ticket→SLA 清单行覆盖 | §1.4 全套 | 同上 |
| I | cs 运行时面（CAT-1 23 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U14 hr（S 级 2 切片：hr-1/hr-2 → M1.10~M1.11）

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | hr-1：组织/岗位删除守卫/员工-as-partner 跨域写；hr-2：薪酬模拟/排班/考勤语义（散布 README/state-machine） | §1.1 全套逐切片 | r1 `ck-hr-org.md` / `ck-hr-attendance-payroll.md`；r2 index |
| F | 组织架构图/薪酬核算页（复杂手写页清单成员） | §1.2 全套 | 同上 |
| S | HR 4 表 seed（1045-1 批）+ md_partner/ar_ap_item 跨域追加行裁决 + M1.3 批配置链 | §1.3 全套 | 同上 |
| T | P1 考勤→薪酬→过账清单行覆盖；employee-net-balance 跨域读取契约测试 | §1.4 全套 | 同上 |
| I | hr 运行时面（CAT-1 20 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U15 aps

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 前向/后向排产引擎/约束求解/dispatch | §1.1 全套 | r1 `ck-aps.md`；r2 index |
| F | 排产甘特图（复杂手写页清单成员） | §1.2 全套 | 同上 |
| S | aps 7 表 seed（M1.4b 批，`docs/design/aps/seed-data.md`） | §1.3 全套 | 同上 |
| T | P2 OperationOrder 排产清单行覆盖 | §1.4 全套 | 同上 |
| I | aps 运行时面（探针 CAT-1 = 0，MI.9 后维持零） | §1.5 全套 | 同上 |

#### U16 logistics

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 承运商调度/运费过账/发运状态机 | §1.1 全套 | r1 `ck-logistics.md`；r2 index |
| F | 发运追踪页（复杂手写页清单成员） | §1.2 全套 | 同上 |
| S | logistics 8 表 seed（M1.4b 批） | §1.3 全套 | 同上 |
| T | P2 Shipment 清单行覆盖 | §1.4 全套 | 同上 |
| I | log 运行时面（CAT-1 19 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U17 b2b

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | EDI 文档状态机/MFT 证书链/伙伴凭证 | §1.1 全套 | r1 `ck-b2b.md`；r2 index |
| F | EDI/伙伴管理页面 | §1.2 全套 | 同上 |
| S | b2b 13 表 seed（M1.5 批，含 EDI 段） | §1.3 全套 | 同上 |
| T | P2 EDI outbound 清单行覆盖 | §1.4 全套 | 同上 |
| I | b2b 运行时面（CAT-1 22 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U18 contract

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 合同审批矩阵/返利结算/e-sign 状态机/发票计划生成 | §1.1 全套 | r1 `ck-contract.md`；r2 index |
| F | 合同库/审批页面 | §1.2 全套 | 同上 |
| S | contract 14 表 seed（M1.5 批） | §1.3 全套 | 同上 |
| T | P2 InvoicePlan 清单行覆盖 | §1.4 全套 | 同上 |
| I | ct 运行时面（CAT-1 12 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U19 drp

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 净需求引擎/补货单生成/越库/安全库存 | §1.1 全套 | r1 `ck-drp.md`；r2 index |
| F | DRP 计划页面 | §1.2 全套 | 同上 |
| S | drp 11 表 seed（M1.5 批，含 inv_drp_* 跨域表） | §1.3 全套 | 同上 |
| T | P2 DRP 清单行覆盖 | §1.4 全套 | 同上 |
| I | drp 运行时面（CAT-1 2 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U20 common-service（+ common-test）

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | **common 抽象族归属格（§3.2）**：`AbstractProcessor.illegal*` helper 期望态参数、`AbstractErpCrudBizModel`/Immutable 状态锁基类行为、共享 config 基建 | §1.1 全套 + 基类逐方法走查 | r1 `ck-common-app.md`（C8.2）；r2 index |
| F | —（common 层无独立页面；判定 = 该格 pass/n-a 记录理由） | n-a（记录理由） | 同上 |
| S | —（common 层无独立 seed；判定 = n-a 记录理由） | n-a（记录理由） | 同上 |
| T | common-test 基座（BaseTestCase/初始化模式）对全域测试的一致性支撑 | §1.4 ②③（对 `module-common-test`） | 同上 |
| I | common 运行时面（CAT-2 5/CAT-3 探针族，MI.9 后应零） | §1.5 全套 | 同上 |

#### U21 app-erp-all 横切

| 维 | 单元内焦点 | 域内增量程式 | 跨轮查重 |
| --- | --- | --- | --- |
| B | 聚合完整性（维度⑭全局面）：action-auth 聚合器 x:extends 全模块注册/POM 聚合/菜单注册 | §1.1 ①④ + 聚合器全量 grep | r1 `ck-common-app.md`；r2 index |
| F | 菜单 component=FLUX 全局面/`render-mode: flux`/flux 页面导出门禁（999 页/ERP 855） | `npm run validate:flux` + 菜单全量 grep | 同上 |
| S | `_init-data` 全量 seed 口径（372 CSV + 1 SQL 现值）/fresh-DB 启动纪律/`TestErpSeedDataIntegrity` 门禁宿主 | §1.3 全套 | 同上 |
| T | 集成测试快照纪律（C01-C21 族三层全比对）/全 reactor 回归对照基线 | `mvn test -pl app-erp-all` + 快照纪律 grep | 同上 |
| I | E2E runbook 合规抽样（flux 引擎强制/PageObject/REST 断言）+ 页面 yaml CAT-4 全局面 | §1.2 ③ + §1.5 全套 + e2e-runbook §编写规范逐条 | 同上 |

## 4. M1.1~M1.16 工作项 → 唯一格集合映射（冻结）

> 唯一性：每个工作项对应**恰好一个**格集合（无重叠、无空隙）；M1.17 收官以「105 基础格全部被其唯一工作项覆盖且 verdict 落盘」为完整性判据。

| 工作项 | 唯一格集合（单元 × 维 × 切片） |
| --- | --- |
| M1.1 | U05 × 五维 × **fin-1**（含 posting processor 族本体） |
| M1.2 | U05 × 五维 × **fin-2** |
| M1.3 | U05 × 五维 × **fin-3** |
| M1.4 | U05 × 五维 × **fin-4** |
| M1.5 | U08 × 五维 × **mfg-1** |
| M1.6 | U08 × 五维 × **mfg-2** |
| M1.7 | U08 × 五维 × **mfg-3** |
| M1.8 | U06 × 五维 × **ast-1** |
| M1.9 | U06 × 五维 × **ast-2** |
| M1.10 | U14 × 五维 × **hr-1** |
| M1.11 | U14 × 五维 × **hr-2** |
| M1.12 | U07 × 五维（全格）+ U09 × 五维（全格） |
| M1.13 | U03 × 五维 + U04 × 五维 + U02 × 五维（各全格） |
| M1.14 | U12 + U13 + U18 + U17 + U19 各 × 五维（全格） |
| M1.15 | U10 + U15 + U16 + U11 + U01 + U20 各 × 五维（全格） |
| M1.16 | U21 × 五维（全格） |

格数对账（切片子格 vs 基础格）：C 级全域格 15 格（U01-U04、U07、U09-U13、U15-U19）× 5 = **75 子格**；S 级 4 格按切片展开 fin 4×5 + mfg 3×5 + ast 2×5 + hr 2×5 = **55 子格**；U20/U21 2 格 × 5 = **10 子格**。切片子格合计 75+55+10 = **140**。基础格合计 = 15×5 + 4×5 + 2×5 = **105**（19 域格 + common 格 + app 格）。S 级格内细分净增 140 − 105 = 35 子格（55 切片子格落在 20 个 S 级基础格上，每基础格按其切片数细分，不新增基础格、仅细分唯一归属）。

## 5. 覆盖完整性自证（M0.5 冻结时 Proof）

- **五维 × 21 格全矩阵无空格**：§3.3 逐单元 5 行（U20 DIM-F/S 为显式 `n-a` 带理由，非空缺），21 × 5 = 105 行全部落册。✅
- **锚点路径实仓可解析**：§1 全部锚点文件于 2026-09-06 逐一 `test -f` 核验通过（执行记录见本计划 Phase 1 Proof 注记与日志）。✅
- **M1.1~M1.16 唯一映射**：§4 映射表 16 行，格集合两两不相交、并集 = 105 基础格（S 级切片为格内细分）。✅
- **跨轮查重源在位**：r1 `ai-check-index.md`（§报告清单 + §Finding 追踪 + §Mission 基线快照）、r2 目录（只读）均已实仓核验。✅

## 6. 冻结勘误登记（M0.6 独立 closure audit 产物，只追加不改正文）

| # | 位置 | 勘误 | 依据 |
| --- | --- | --- | --- |
| E1 | §1.1 DIM-B 机械核查程式 ④ | 聚合器路径 `app-erp-all/src/main/resources/_vfs/erp/all/auth/app.action-auth.xml` 实仓不存在，**勘误为** `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`（`x:extends` ×2，程式意图可执行） | 独立子代理 closure audit MINOR-1（task `ses_f8968ae6dffesJIwQ5iQycehHF`，2026-09-06，54 路径抽样唯一未命中项）；M1.x 执行 DIM-B ④ 时以本行勘误路径为准 |
