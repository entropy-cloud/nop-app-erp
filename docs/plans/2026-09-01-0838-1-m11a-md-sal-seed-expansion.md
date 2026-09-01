# 2026-09-01-0838-1 M1.1a 核心 CRUD 域 seed 扩面 A——master-data + sales（12 CSV）

> Plan Status: completed
> Last Reviewed: 2026-09-01
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.1a（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，270 实体规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁 + CSV 基线常量随批更新协议）、同批 `2026-09-01-0838-2-m11b-inventory-seed-expansion.md` / `2026-09-01-0838-3-m11c-purchase-seed-expansion.md`
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体，93 个有 seed CSV（`app-erp-all/src/main/resources/_vfs/_init-data/`，97 CSV = 93 app.erp + 4 平台），270 缺 seed；本计划覆盖其中 master-data 4 缺 + sales 8 缺 = **12 缺**（逐实体规格见 `docs/architecture/seed-data.md`「270 个缺 seed 实体最小可用数据集规格表」master-data / sales 两节，本计划逐行引用不复制）。
- 装载机制（不重做，仅消费）：`DataInitInitializer` 按 ORM 拓扑序按 `{tableName}.csv` 装载，非幂等，fresh-DB 重置为前置；`application.yaml` 默认 `init-database-data: true`（演示/沙盒语义，plan 1143-1）；`zz-sequence-advance.sql` 在 CSV 装载后将 `NOP_SYS_SEQUENCE` default 行 NEXT_VALUE 推进到 100000 → **新增 seed ID 必须落在 < 100000 值域**。
- 门禁基线（M0.2 落地）：`TestErpSeedDataIntegrity` scope-pinning（`EXPECTED_APP_ERP_ENTITY_COUNT = 363` + finance 5 个缺 className 补充集）、零孤儿 CSV 断言、`EXPECTED_APP_ERP_CSV_COUNT = 93` + `EXPECTED_PLATFORM_CSV_COUNT = 4` 基线常量（javadoc 内置「M1.x 每批随批更新协议」）、全仓 to-one 引用完整性扫描 + 空 `WHITELIST_KEYS`。
- 当前验证基线：`mvn test -pl app-erp-all` 71/0/0/1 全绿（M0.2 plan `2026-09-01-0527-1` 执行实测；`known-good-baselines.md` 最新登记行 69/0/0/1 系 M0.2 前口径，基线追行归 M3.1）；compliance baseline R2c=1542 零漂移；全 reactor 已知 2 处预存回归（hr `TestErpHrDepartmentPositionDeleteGuard` + drp `TestErpDrpCrossDock`）为 roadmap Non-Goal successor 项，与本计划无关。
- 既有 CSV 约定：列头为 DB 列名（大写下划线，如 `erp_sal_order.csv`），省略审计列；ISO 日期；小写布尔；ID 为小整数；同批 CSV 拓扑序由装载器自动排序，无需文件名排序。
- 剩余差距：master-data 4 实体（ErpMdMaterialCustoms / ErpMdSupplierApproval / ErpMdSubjectMapping / ErpSysConfig）与 sales 8 实体（ErpSalContract / ErpSalPriceList / ErpSalPriceListLine / ErpSalPricingRule / ErpSalQuotation / ErpSalQuotationLine / ErpSalReturn / ErpSalReturnLine）无 seed；`docs/design/master-data/` 与 `docs/design/sales/` 无「种子数据」owner doc 段。

## Goals

- 12 个 seed CSV 落地（`erp_md_*.csv` × 4 + `erp_sal_*.csv` × 8），每实体最小可用数据集（行数 ≤ 20，按规格表「建议行数」与用例指示编码 P / N-TERM / N-DIS），FK 引用零悬空。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（93 → 105），seed-data.md 对账表同步。
- `docs/design/master-data/seed-data.md` + `docs/design/sales/seed-data.md`「种子数据」owner doc 段落地。

## Non-Goals

- 不修改任何 ORM 模型（`module-*/model/*.orm.xml`，保护区 auto + dual-agent-approval）——本计划纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` 的 CSV 基线常量按内置协议更新）。
- 不覆盖 inventory / purchase 及其余 9 个 M1.x 工作项的缺 seed 实体（归同批 N=2/N=3 与后续批次）。
- 不新增 M2.x 像素断言、不做负向视觉断言、不做跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 hr/drp 2 处预存回归（roadmap Non-Goal successor）。
- 不引入 production-grade 真实个人数据；不触碰 finance 5 个缺 className 实体（M1.2a1 口径）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 master-data / sales 节 + 全量化裁决段）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.1a 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）
- Skill Selection Basis: roadmap M1.1a 行指定 `nop-backend-dev`（数据资产须对齐实体/字典/列命名约定，技能路由到平台模型文档）；Proof 阶段运行测试套件与视觉 spec 属测试域，加载 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（本地 fresh-DB 由 `./scripts/start-app.sh` 承载；`init-database-data: true` 已默认开启）。
- 回滚策略：seed CSV 为纯新增文件，回滚 = 删除本批新增文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：本计划为同批三计划（N=1/2/3）之首；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，若 N=2/N=3 先落地，常量基数以「93 + 已落地批次 CSV 数」为准，避免覆盖写。

### Phase 1 - Seed CSV authoring（12 CSV）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪）

- [x] 逐实体核对 ORM 表名（`module-master-data/model/` + `module-sales/model/` 的 `*.orm.xml` tableName）后创建 12 个 CSV：`erp_md_material_customs.csv`（1~3 行，P）、`erp_md_supplier_approval.csv`（2~3 行，P+N-TERM）、`erp_md_subject_mapping.csv`（1~3 行，P）、`erp_sys_config.csv`（1~3 行，P）、`erp_sal_contract.csv`（2~3 行，P+N-TERM）、`erp_sal_price_list.csv`（1~2 行，P+N-DIS）、`erp_sal_price_list_line.csv`（1~2/头，P）、`erp_sal_pricing_rule.csv`（1~2 行，P+N-DIS）、`erp_sal_quotation.csv`（2~3 行，P+N-TERM）、`erp_sal_quotation_line.csv`（1~2/头，P）、`erp_sal_return.csv`（2~3 行，P+N-TERM）、`erp_sal_return_line.csv`（1~2/头，P）
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（如 ErpMdMaterial / ErpMdMaterialCategory / ErpMdPartner / ErpMdCurrency / ErpMdWarehouse / ErpMdUoM / ErpMdSubject / ErpMdAcctSchema）或〔本批〕新增行；可选 FK 留空或指向已 seed 行；禁止悬空引用
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；列集以 ORM/XMeta 生成的实体列为准（先抽样同域既有 CSV，如 `erp_sal_order.csv`）
      - Skill: `nop-backend-dev`
- [x] Proof: 每行数据可通过 XMeta 列校验——由 Phase 3 `TestErpSeedDataIntegrity` 全绿背书（列名错误会在装载或 findAll 时暴露）
      - Skill: `nop-testing`

Exit Criteria:

- [x] 12 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表（P 正例 + N-TERM 终态行 / N-DIS 禁用行按 ORM 状态列存在性）
- [x] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/master-data/seed-data.md`、`docs/design/sales/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：93 → 105（+12），`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`（实体集未变）
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md` 对账表计数（app.erp.* 有 seed 93 → 105，缺 seed 270 → 258），保持六档对账表与规格表口径一致；备注行登记「M1.1a 批次（本 plan）落地」
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/master-data/seed-data.md` 与 `docs/design/sales/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕标注）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 常量与对账表数值一致（105 / 258），且与 `_init-data/` 实际 CSV 构成一致
- [x] 两个域 seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照 M0.2 实测基线 71/0/0/1 全绿或更优；若新 seed 行引起集成测试快照漂移（如 C03/C04 涉 sal 表），按既有重录协议评估：良性内容增量 → 重录并在本计划登记；行为破坏 → 先 Fix 再闭包
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行受影响视觉 spec（`material-customs` visual spec 读 ErpMdMaterialCustoms 表 + `dashboards*.visual/snapshot` + `reports*.visual/snapshot` 中涉 sales/master-data 数据面）；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划 Draft Review Record 区追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL 抽样 ≥ 3 张本批表（至少 master-data 1 张 + sales 2 张，含 1 张主子表头）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/testing/known-good-baselines.md` 最新行（R2c=1542）零漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [x] `docs/logs/2026/09-01.md`（或实际执行日）追加执行条目
      - Skill: none

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过
- [x] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位

执行证据（2026-09-01）：

- `TestErpSeedDataIntegrity` 4/4 全绿（scope-pinning 363+5 不变 / 零孤儿 CSV / 105+4 基线常量 / 引用完整性零悬空白名单零增量）。
- `mvn test -pl app-erp-all` 71/0/0/1 BUILD SUCCESS = M0.2 实测基线（零集成快照漂移，C01-C21 全绿）。
- fresh-DB `./scripts/start-app.sh restart` 装载 0 冲突 0 列映射错误；GraphQL `findPage` 抽样 12/12 表 `total == CSV 行数`（ErpMdMaterialCustoms 2 / ErpMdSubjectMapping 2 / ErpMdSupplierApproval 3 / ErpSysConfig 3 / ErpSalQuotation 3（主子表头）/ ErpSalQuotationLine 3 / ErpSalContract 3 / ErpSalReturn 3 / ErpSalReturnLine 3 / ErpSalPriceList 2 / ErpSalPriceListLine 2 / ErpSalPricingRule 2）。
- 视觉 spec 实跑 5 文件 52 用例：`dashboards.visual`(10) + `reports.visual` + `dashboards.snapshot`(10) + `reports.snapshot`(6) 全绿零漂移；`material-customs.visual` 2 失败 = 预存 spec bug（`findPage(limit:)` 非法调用形态，javap 实证平台 `ICrudBiz.findPage` 签名仅 `query` 参数，与 seed 变更无关——证据链登记 `docs/bugs/2026-09-01-0945-material-customs-visual-spec-invalid-findpage-arg.md`，归 successor 修复）。
- compliance checker exit 0，R2c=1542 与 known-good-baselines 最新行零漂移。
- 附带验证：`mvn clean install -DskipTests -pl app-erp-all` BUILD SUCCESS，runner jar 实测打包 109 CSV（runtime 证明前置）。

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 fresh session `ses_fa5956153ffelxl8l0kzEfGM10`）——0 Blocker + 0 Major；2 Minor（①FK 例举缺 ErpMdMaterialCategory；②71/0/0/1 与 known-good-baselines 69/0/0/1 口径澄清）均已并入本稿；审查者实仓核验 12 实体规格与 seed-data.md 规格表 1:1、常量/路径/runbook 协议全部吻合。
- 快照重录合规声明：本批次执行时若触发 DOM/像素漂移双面重录，在此追加声明段（已于执行期落地，见下方「快照重录合规声明」节——实测未触发）。

## 快照重录合规声明

**本批次未触发快照重录**（2026-09-01 执行实测，双面核查通过）：

1. **触发条件成立**：本批次新增 12 个部署期 seed CSV（97 → 109，见 `docs/architecture/seed-data.md` 快照重录义务节 M1.1a 注记），按义务规则 1 属触发情形，故执行双面核查。
2. **面 1（各域 `_cases` 快照）零影响**：各域模块测试不声明 `nop.orm.init-database-data`（0330-1 先例口径）；本批次后各域模块测试未运行变更，无受影响面。
3. **面 2（app-erp-all 集成用例快照）实测零漂移**：`mvn test -pl app-erp-all` 71/0/0/1 与 M0.2 基线完全一致——C01-C21 集成用例（CHECKING 态三层全比对）在新 seed 集下零失败，无需重录。
4. **视觉双面（DOM + 像素）实测零漂移**：受影响面评估 = 全仓 Dashboard/Report BizModel 对本批 12 表**零读取**（grep 实证），`material-customs.visual` 为形态断言（`Array.isArray(items)`，非内容断言）。实跑 5 spec 文件 52 用例：dashboards.visual（10 域 DOM）+ dashboards.snapshot（10 域像素）+ reports.visual + reports.snapshot（DOM + 像素基线）全绿零漂移；material-customs.visual 2 失败为**预存 spec bug**（`findPage(limit:)` 非法调用形态，与 seed 无关，`docs/bugs/2026-09-01-0945` 登记归 successor），非漂移。
5. **E2E 数值断言联动评估**：本批 12 表不被任何既有 `*.list-value.spec.ts` / `*.value.spec.ts` / 看板 KPI 消费（sales 看板仅读 Order/Invoice，md-material-price-list 报表仅读 Material/SKU），无需同步期望值基线。
6. **提交说明登记**（义务规则 3）：seed 变更 = 新增 12 CSV（erp_md_material_customs / erp_md_subject_mapping / erp_md_supplier_approval / erp_sys_config / erp_sal_quotation(+line) / erp_sal_contract / erp_sal_price_list(+line) / erp_sal_pricing_rule / erp_sal_return(+line)）→ 双面核查记录于本节 + plan Phase 3 执行证据，重录范围 = 零。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（12 CSV 落地 + 常量 + 对账表 + 2 个 owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表 + 2 个域 seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——M0.1 规格 CSV-only 可满足性核验已确认本批 12 实体零 ORM 依赖；触发条件未发生）

## Closure

Status Note: 计划可闭合——12 CSV 落地（109 CSV 实仓复核）+ 门禁常量 105 随批更新 + seed-data.md 对账表同步（105/258 + M1.1a 批次行）+ 2 个域 owner doc 落地；Phase 3 全部 Proof 全绿（门禁 4/4、模块回归 71/0/0/1 = 基线、fresh-DB 装载 12/12 表 findPage==CSV 行数、视觉双面零漂移、compliance R2c=1542 零漂移）；快照重录双面义务核查通过未触发重录；1 项预存 spec bug 非本批引入已登记 docs/bugs/2026-09-01-0945 归 successor。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，task id `ses_fa55b8fb1ffeN6SUvRLsQHzhyY`）
- Evidence: VERDICT **APPROVE**（2026-09-01）——11 项声明全部独立实仓复算确认：12 CSV 行数（2/2/3/3/3/3/3/2/2/2/3/3 = 31 行）与 `_init-data` 总数 109 逐一清点；列头对 ORM code= 程序化比对；FK 闭环抽样全部解析至已 seed 行；字典码 ∈ ORM dict；门禁常量 105/4/363/5/空白名单逐一核对；seed-data.md 对账表 105/258/M1.1a 行/263 注记定位核实；2 个 owner doc 与实仓一致；日志与 bug 笔记在位；范围干净（零 ORM/生产 Java 变更）。2 MINOR 非阻塞（闭包门勾选时机 + Draft Review Record 残留草稿措辞），均已在本闭包提交中修复。

Follow-up:

- （仅非阻塞跟进项目；已确认的缺陷不得出现在此处）
