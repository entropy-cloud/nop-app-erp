# 2026-09-01-0838-3 M1.1c 核心 CRUD 域 seed 扩面 C——purchase（12 CSV）

> Plan Status: completed
> Last Reviewed: 2026-09-01
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.1c（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，270 实体规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁 + CSV 基线常量随批更新协议）、同批 `2026-09-01-0838-1-m11a-md-sal-seed-expansion.md` / `2026-09-01-0838-2-m11b-inventory-seed-expansion.md`
> Audit: required

## Current Baseline

- 实仓 purchase 域 20 个 `app.erp.*` 实体，8 个有 seed（`erp_pur_invoice(_line)` / `erp_pur_order(_line)` / `erp_pur_payment(_line)` / `erp_pur_receive(_line)`），**12 缺**（逐实体规格见 `docs/architecture/seed-data.md` 规格表 purchase 节，本计划逐行引用不复制）。
- 12 缺实体构成：Quotation / Requisition / Return / Rfq 四组主子表（各 1 头 + 1 行）+ SupplierPriceList 独立表 + SupplierScorecard 主子表（Scorecard + Criteria 行表 + Variable 独立表，Variable FK 指向〔本批〕Criteria）——合计 5 条主子链 + 2 独立表。注：roadmap M1.1c 行「9 组主子表完整链路」为措辞漂移，以 M0.1 规格表（`seed-data.md` purchase 节）为权威，本计划按规格表口径执行；roadmap 本体勘误归 roadmap owner（沿 M0.2 先例）。必填 FK 全部落在〔已seed〕（ErpMdPartner / ErpMdMaterial / ErpMdUoM / ErpMdCurrency / ErpMdWarehouse / ErpMdEmployee，跨域 master-data 已 seed）∪〔本批〕集合（M0.1 程序化核验零环、零未知指向）。
- 装载机制（不重做，仅消费）：`DataInitInitializer` 按 ORM 拓扑序按 `{tableName}.csv` 装载，非幂等，fresh-DB 重置为前置；`application.yaml` 默认 `init-database-data: true`（演示/沙盒语义，plan 1143-1）；`zz-sequence-advance.sql` 将 sequence NEXT_VALUE 推进到 100000 → **新增 seed ID 必须落在 < 100000 值域**。
- 门禁基线（M0.2 落地）：`TestErpSeedDataIntegrity` scope-pinning（363 + finance 5 补充集）、零孤儿 CSV、`EXPECTED_APP_ERP_CSV_COUNT`（M0.2 时点 93；本计划执行时基数 = 93 + 已落地 M1.x 批次 CSV 数，按常量 javadoc 随批更新协议推进）+ `EXPECTED_PLATFORM_CSV_COUNT = 4`、全仓 to-one 引用完整性 + 空 `WHITELIST_KEYS`。
- 当前验证基线：`mvn test -pl app-erp-all` 71/0/0/1 全绿（M0.2 plan `2026-09-01-0527-1` 执行实测；`known-good-baselines.md` 最新登记行 69/0/0/1 系 M0.2 前口径，基线追行归 M3.1）；compliance baseline R2c=1542 零漂移；全 reactor 2 处预存回归（hr/drp）为 roadmap Non-Goal successor 项。
- 敏感度注记：SupplierPriceList 属「供应商价格」语义（roadmap 横切 1 敏感字段处理清单提及项）——本计划仅 CSV 演示种子（非生产数据、非 EDI 凭据、不触会计过账路径），不构成 auth/finances plan-first 触发面；若执行中发现任何字段落在 `docs/context/ai-autonomy-policy.md` 保护区，暂停该字段并按保护区协议处置。
- 与既有 purchase seed 的衔接约束：新批次单据引用的伙伴/物料/仓库维度值复用既有 `erp_pur_order` / `erp_pur_receive` 既有行值，避免演示数据自相矛盾。
- 剩余差距：12 实体无 seed；`docs/design/purchase/` 无「种子数据」owner doc 段。

## Goals

- 12 个 seed CSV（`erp_pur_*.csv`）落地，四组主子表 + 评分主子表链路引用闭环，每实体最小可用数据集（行数 ≤ 20，按规格表「建议行数」与用例指示编码 P / N-TERM / N-DIS），FK 引用零悬空。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（+12），seed-data.md 对账表同步。
- `docs/design/purchase/seed-data.md`「种子数据」owner doc 段落地。

## Non-Goals

- 不修改任何 ORM 模型（保护区 auto + dual-agent-approval）——纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` CSV 基线常量随批更新）。
- 不补 purchase 域已有 seed 的 8 实体（不重做既有资产）。
- 不覆盖 master-data/sales（N=1）、inventory（N=2）及其余 M1.x 工作项；不触 EDI 凭据（M1.5 口径）与会计过账路径（M1.2a1 口径）。
- 不新增 M2.x 像素断言；不做负向视觉断言、跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 hr/drp 2 处预存回归；不触碰 finance 5 个缺 className 实体。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 purchase 节 + 全量化裁决段）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.1c 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）
- Skill Selection Basis: roadmap M1.1c 行指定 `nop-backend-dev`（对齐实体/列/字典约定，路由平台模型文档）；Proof 阶段运行测试与视觉 spec 属测试域，加载 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载）。
- 回滚策略：纯新增 CSV 文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：按文件名 N 序在本批三计划中第三个执行；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，常量基数以「93 + 已落地批次（含 N=1/N=2 若先落地）CSV 数」为准，避免覆盖写。

### Phase 1 - Seed CSV authoring（12 CSV）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪）

- [x] 逐实体核对 `module-purchase/model/app-erp-purchase.orm.xml` tableName 后创建 12 个 CSV：`erp_pur_quotation.csv`（2~3 行，P+N-TERM）+ `erp_pur_quotation_line.csv`（1~2/头，P）、`erp_pur_requisition.csv`（2~3，P+N-TERM）+ `erp_pur_requisition_line.csv`（1~2/头，P）、`erp_pur_return.csv`（2~3，P+N-TERM）+ `erp_pur_return_line.csv`（1~2/头，P）、`erp_pur_rfq.csv`（2~3，P+N-TERM）+ `erp_pur_rfq_line.csv`（1~2/头，P）、`erp_pur_supplier_price_list.csv`（1~2，P+N-DIS）、`erp_pur_supplier_scorecard.csv`（2~3，P+N-TERM）+ `erp_pur_supplier_scorecard_criteria.csv`（1~2/头，P）、`erp_pur_supplier_scorecard_variable.csv`（1~3，P）
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：四组主子表 + 评分组行表头 FK 指向〔本批〕头行；`erp_pur_supplier_scorecard_variable` FK 指向〔本批〕Criteria 行；跨域必填 FK（ErpMdPartner / ErpMdMaterial / ErpMdUoM / ErpMdCurrency / ErpMdWarehouse / ErpMdEmployee）指向〔已seed〕行；禁止悬空引用
      - Skill: `nop-backend-dev`
- [x] 与既有 purchase seed 衔接：伙伴/物料/仓库维度值复用既有 `erp_pur_order` / `erp_pur_receive` 行值（先抽样既有 CSV 确认值域）
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；列集以 ORM/XMeta 生成实体列为准
      - Skill: `nop-backend-dev`
- [x] Proof: 每行数据可装载可查询——由 Phase 3 `TestErpSeedDataIntegrity` 全绿背书
      - Skill: `nop-testing`

Exit Criteria:

- [x] 12 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表（按本项文件枚举序 3/3/3/3/2/2/3/4/2/3/9/3 = 40 行，均 ≤ 20 上限；quotation/requisition/rfq 三头表 2 P + 1 N-TERM，return 头表 2 行 = 1 P（DRAFT 中立态）+ 1 N-TERM，price_list P + N-DIS，scorecard 2 FINALIZED 终态守卫负例 + 1 DRAFT）
- [x] 无悬空 FK：五组主子链路闭环 + 跨域引用全部指向已 seed 行（程序化核验：6 组主子 FK comm 零悬空 + 跨域值域比对全过 + 评分卡权重和 100 / Σ加权=TOTAL_SCORE / 退货头行金额自洽，证据见 Phase 3 执行证据区）

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/purchase/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：执行时点基数 +12；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES` / `EXPECTED_PLATFORM_CSV_COUNT`（121 → 133，javadoc 批次沿革登记）
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md` 对账表计数（purchase 已 seed 8 → 20；全集缺 seed 相应 -12：有 seed 121 → 133 / 缺 seed 242 → 230 / 运行时口径全集缺 235），登记「M1.1c 批次（本 plan）落地」备注行，并同步门禁强化段与快照重录义务节 CSV 资产计数（137 CSV + 1 SQL）
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/purchase/seed-data.md`「种子数据」段：五组主子链路闭环图、CSV 文件与行数、FK 标注（〔已seed〕/〔本批〕）、与既有 8 seed 的衔接说明、negative 行语义
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 常量与对账表数值一致（133 / 230），且与 `_init-data/` 实际 CSV 构成一致（137 CSV = 133 app.erp + 4 平台；`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 4/4 全绿实证 + jar 打包 137 CSV 实测）
- [x] purchase seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照 M0.2 实测基线 71/0/0/1 全绿或更优；若新 seed 行引起集成测试快照漂移（C03 涉 pur 表），按既有重录协议评估：良性内容增量 → 重录并登记；行为破坏 → 先 Fix 再闭包（终跑 71/0/0/1 = 基线一致，零快照漂移，无处置需求）
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行受影响视觉 spec（`dashboards*.visual/snapshot` purchase 域 KPI 面 + `reports*.visual/snapshot` 涉 purchase 数据面）；若 seed 变更触发 DOM/像素漂移，按 e2e-runbook 视觉方法论双面（DOM + 像素）同步重录，并在本计划 Draft Review Record 区追加「快照重录合规声明」段；禁止单面重录（实跑 4 spec 50/50 全绿零漂移，重录未触发，声明见「快照重录合规声明」节）
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL 抽样 ≥ 3 张本批表（含 ≥ 1 组主子表头行）findPage 行数 == CSV 行数（实跑 12/12 表，含全部 5 组主子表头行）
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/testing/known-good-baselines.md` 最新行（R2c=1542）零漂移；若有漂移按失败模式速查开独立基线裁决（exit 0，R2c=1542 零漂移）
      - Skill: `nop-testing`
- [x] `docs/logs/`（实际执行日）追加执行条目
      - Skill: none

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过（4/4，EXPECTED_APP_ERP_CSV_COUNT=133）
- [x] `mvn test -pl app-erp-all` 与基线一致或更优（71/0/0/1）；快照漂移处置已登记（含快照重录合规声明，若触发——本批未触发，见声明节）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位

执行证据（2026-09-01）：

- `TestErpSeedDataIntegrity` 首跑拦截 1 处 CSV 列错误：`erp_pur_supplier_price_list.csv` 误含 REMARK 列（ORM 实体无该列，装载期 `nop.err.orm.model.unknown-column-code`）——移除后复跑 **4/4 全绿**（scope-pinning 363+5 不变 / 零孤儿 CSV / 133+4 基线常量 / 引用完整性零悬空 + 空白名单），二次确认复跑仍 4/4。
- `mvn test -pl app-erp-all` 终态 **71/0/0/1 BUILD SUCCESS** = M0.2/M1.1a/M1.1b 实测基线（C03 O2C / C02 退货 / C13 期末 / C20a DRP 全过，零新 seed 行为交互——执行前干扰面预分析：purchase 看板仅读 invoice/order/receive、期末预检仅扫 fin/ast/inv 三表、DRP 仅读 pur_order、无集成测试保存 quotation → SupplierEligibilityChecker 不触发）。
- `mvn clean install -DskipTests -pl app-erp-all` BUILD SUCCESS，runner jar 实测打包 **137 CSV**（133 app.erp + 4 平台；pur 族 20 = 既有 8 + 本批 12）。
- fresh-DB `./scripts/start-app.sh restart` 装载 0 冲突 0 列映射错误（app.log 无 seed 相关 ERROR）；`/r/{Entity}__findPage` 抽样 **12/12 表 `total == CSV 行数`**：ErpPurRequisition(3)+Line(3) / ErpPurRfq(3)+Line(4) / ErpPurQuotation(3)+Line(3) / ErpPurReturn(2)+Line(2) / ErpPurSupplierPriceList(2) / ErpPurSupplierScorecard(3)+Criteria(9)+Variable(3)（主子表头全覆盖）。
- 视觉双面实跑 4 文件 **50/50 全绿零漂移**：`dashboards.visual`(10) + `dashboards.snapshot`(10，含 purchase 看板像素基线) + `reports.visual` + `reports.snapshot`(30)。诊断附注：首测以 `start-app.sh` 最小 flag 服务器（服务端直连 8011）跑出 10/10 全 500——`SiteMapApi__getSiteMap` 报 `undefined-field:authCascadeUp`（前端壳与后端 site map 契约在最小 flag 环境失配，非 seed 因素、全 10 域dashboard 无差别失败）；改用 `playwright.config.ts` 内嵌 webServer（全量 `-D` 业务 flag + test profile）后 50/50 全绿，证明为环境启动方式差异而非数据漂移。
- compliance checker exit 0，**R2c=1542** 与 known-good-baselines 最新行零漂移（纯资源 + 测试常量 + docs 变更，符合预期）。

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 fresh session `ses_fa594f55dffeDEbu4HnnOsVTWT`）——0 Blocker + 0 Major；3 Minor（①日志条目列于 Proof 相属类型错位（功能性等价）；②71/0/0/1 与 known-good-baselines 69/0/0/1 口径澄清；③roadmap「9 组主子表」措辞漂移宜一行对账说明）——②③已并入本稿，①经裁决保持原位（Phase 3 为终相，Closure Gates 第 3 项引用其全部 Proof 项）；审查者实仓核验 12 实体规格与 seed-data.md 规格表 1:1、ORM tableName/FK 抽查全对、敏感度注记（SupplierPriceList CSV 演示种子不触发 plan-first）判定成立。

## 快照重录合规声明

**本批次未触发快照重录**（2026-09-01 执行实测，双面核查通过）：

1. **触发条件成立**：本批次新增 12 个部署期 seed CSV（125 → 137，见 `docs/architecture/seed-data.md` 快照重录义务节 M1.1c 注记），按义务规则 1 属触发情形，故执行双面核查。
2. **面 1（各域 `_cases` 快照）零影响**：各域模块测试不声明 `nop.orm.init-database-data`（0330-1/M1.1a/M1.1b 先例口径），本批次未运行各域模块测试，无受影响面；app-erp-all 集成用例 `_cases` 目录经 `mvn test -pl app-erp-all` 71/0/0/1 与基线一致复核零漂移（未触碰任何录制文件）。
3. **面 2（app-erp-all 集成用例快照）实测零漂移**：`mvn test -pl app-erp-all` 71/0/0/1 与 M0.2 基线完全一致——C01-C21 集成用例（CHECKING 态三层全比对）在新增 purchase seed 集下零失败，无需重录（干扰面预分析：purchase 看板 KPI 读 invoice/order/receive 三表不受本批影响；期末预检扫描面 fin/ast/inv 与 DRP 聚合面 pur_order 均不含本批 12 表；无集成测试保存 quotation，`SupplierEligibilityChecker` 评分卡准入守卫不触发）。
4. **视觉双面（DOM + 像素）实测零漂移**：受影响面评估 = purchase 看板 KPI/趋势读 `ErpPurInvoice`/`ErpPurOrder`/`ErpPurReceive`（本批不新增行，KPI 值 purchaseAmount=850/orderCount=1 不变）+ 10 域报表零读取本批 12 表。实跑 4 spec 文件 50 用例：dashboards.visual（10）+ dashboards.snapshot（10，含 purchase 像素基线）+ reports.visual + reports.snapshot（30）全绿零漂移。
5. **E2E 数值断言联动评估**：本批 12 表不被任何既有 `*.list-value.spec.ts` / `*.value.spec.ts` / 看板 KPI 消费（`purchase.value.spec.ts` 断言 850/1 来自 invoice/order；`e1-2-purchase.smoke` 用 DUMMY_ID 仅断言错误码与数据无关；`child-table-write`/`pur-return.action` 自建自清理），无需同步期望值基线。
6. **提交说明登记**（义务规则 3）：seed 变更 = 新增 12 CSV（erp_pur_requisition(+line) / erp_pur_rfq(+line) / erp_pur_quotation(+line) / erp_pur_return(+line) / erp_pur_supplier_price_list / erp_pur_supplier_scorecard(+criteria+variable)）→ 双面核查记录于本节 + plan Phase 3 执行证据，重录范围 = 零。

## Closure Gates

> 全量仓库验证不在本计划闭包门控内：变更面 = 资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（12 CSV 落地 + 常量 + 对账表 + purchase owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表 + purchase seed-data.md；快照重录若触发已按协议登记——未触发，合规声明节）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——M0.1 规格 CSV-only 可满足性核验已确认本批 12 实体零 ORM 依赖；触发条件未发生）

## Closure

Status Note: 计划可闭合——12 CSV 落地（4 组单据主子表 + 评分主子链 + 2 独立表，40 行，137 CSV 实仓/打包双重复核）+ 门禁常量 133 随批更新 + seed-data.md 对账表同步（133/230 + M1.1c 批次行 + 运行时口径 235 + 快照重录资产 137 CSV + 1 SQL）+ purchase owner doc 落地（含评分卡数值自洽表与退货 DRAFT 中立性裁决）；Phase 3 全部 Proof 全绿（门禁 4/4（首跑拦截 price_list 误含 REMARK 列即修）、模块回归 71/0/0/1 = 基线、fresh-DB 装载 `/r/` findPage 12/12 表 total==CSV 行数、视觉双面 50/50 零漂移、compliance R2c=1542 零漂移）；快照重录双面义务未触发（声明节 6 点核查）；范围纪律维持（零 ORM / 零生产 Java 变更，唯一 Java 触点 = 门禁常量随批更新）。执行期环境注记：视觉 spec 须以 playwright config 内嵌 webServer（全量 `-D` flag）运行，`start-app.sh` 最小 flag 服务器直连会因 site map 契约失配全数 500（非 seed 因素，已登记执行证据）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，task id `ses_fa4cef22fffe83z5W2gSTUJsM7`）
- Evidence: VERDICT **APPROVE**（2026-09-01）——A-K 全部 11 审计步 PASS：12 CSV 存在且行数 40（12 文件 multiset 与规格表/Phase 3 运行时证据 1:1）；12 实体列头 vs ORM `code=` 程序化比对零未知列（含 `UO_M_ID` 分叉核实 + price_list 无 REMARK 确认）+ 零审计/留痕列；FK 闭环（6 组主子 join + 跨域值域对实仓 master-data CSV 核验 + 既有 receive/receive_line 回链）零悬空 + 字典码全命中；数值自洽（退货头=行合计、评分卡权重 100 / Σ加权=TOTAL_SCORE / standing 落档口径、ID < 100000、报价-PO-协议价 8.50 三点一致）独立复算通过；门禁常量 133/4/363/空白名单 + git 实证唯一 Java 触点；seed-data.md 对账表 133/230/235/137+1 四处定位核实 + owner doc 抽查 3 项事实全对；roadmap M1.1c 行 `done` + 12 实体名完整 + 日志条目在位（纯追加）；结束审计独立复跑 `TestErpSeedDataIntegrity` 4/4/0/0 全绿；范围纪律（12 CSV + 1 doc 新增，5 文件修改，零 ORM/生成代码变更）。3 MINOR 非阻塞（Phase 1 行数串位序已按审查意见修正为文件枚举序 + return P 行措辞精确化；roadmap done 注引用 Closure 段——本 APPROVE 落地后即为事实）。

Follow-up:

- （仅非阻塞跟进项目；已确认的缺陷不得出现在此处）
