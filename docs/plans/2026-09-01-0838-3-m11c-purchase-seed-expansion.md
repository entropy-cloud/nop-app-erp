# 2026-09-01-0838-3 M1.1c 核心 CRUD 域 seed 扩面 C——purchase（12 CSV）

> Plan Status: active
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

Status: planned
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪）

- [ ] 逐实体核对 `module-purchase/model/app-erp-purchase.orm.xml` tableName 后创建 12 个 CSV：`erp_pur_quotation.csv`（2~3 行，P+N-TERM）+ `erp_pur_quotation_line.csv`（1~2/头，P）、`erp_pur_requisition.csv`（2~3，P+N-TERM）+ `erp_pur_requisition_line.csv`（1~2/头，P）、`erp_pur_return.csv`（2~3，P+N-TERM）+ `erp_pur_return_line.csv`（1~2/头，P）、`erp_pur_rfq.csv`（2~3，P+N-TERM）+ `erp_pur_rfq_line.csv`（1~2/头，P）、`erp_pur_supplier_price_list.csv`（1~2，P+N-DIS）、`erp_pur_supplier_scorecard.csv`（2~3，P+N-TERM）+ `erp_pur_supplier_scorecard_criteria.csv`（1~2/头，P）、`erp_pur_supplier_scorecard_variable.csv`（1~3，P）
      - Skill: `nop-backend-dev`
- [ ] FK 闭环按规格表逐行落实：四组主子表 + 评分组行表头 FK 指向〔本批〕头行；`erp_pur_supplier_scorecard_variable` FK 指向〔本批〕Criteria 行；跨域必填 FK（ErpMdPartner / ErpMdMaterial / ErpMdUoM / ErpMdCurrency / ErpMdWarehouse / ErpMdEmployee）指向〔已seed〕行；禁止悬空引用
      - Skill: `nop-backend-dev`
- [ ] 与既有 purchase seed 衔接：伙伴/物料/仓库维度值复用既有 `erp_pur_order` / `erp_pur_receive` 行值（先抽样既有 CSV 确认值域）
      - Skill: `nop-backend-dev`
- [ ] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；列集以 ORM/XMeta 生成实体列为准
      - Skill: `nop-backend-dev`
- [ ] Proof: 每行数据可装载可查询——由 Phase 3 `TestErpSeedDataIntegrity` 全绿背书
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 12 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [ ] 无悬空 FK：五组主子链路闭环 + 跨域引用全部指向已 seed 行

### Phase 2 - 门禁常量 + owner doc 同步

Status: planned
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/purchase/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [ ] 按随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：执行时点基数 +12；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES` / `EXPECTED_PLATFORM_CSV_COUNT`
      - Skill: `nop-backend-dev`
- [ ] 同步 `docs/architecture/seed-data.md` 对账表计数（purchase 已 seed 8 → 20；全集缺 seed 相应 -12），登记「M1.1c 批次（本 plan）落地」备注
      - Skill: `nop-backend-dev`
- [ ] 新建 `docs/design/purchase/seed-data.md`「种子数据」段：五组主子链路闭环图、CSV 文件与行数、FK 标注（〔已seed〕/〔本批〕）、与既有 8 seed 的衔接说明、negative 行语义
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [ ] purchase seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: planned
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [ ] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [ ] `mvn test -pl app-erp-all` 对照 M0.2 实测基线 71/0/0/1 全绿或更优；若新 seed 行引起集成测试快照漂移（C03 涉 pur 表），按既有重录协议评估：良性内容增量 → 重录并登记；行为破坏 → 先 Fix 再闭包
      - Skill: `nop-testing`
- [ ] 视觉快照双面义务核查：运行受影响视觉 spec（`dashboards*.visual/snapshot` purchase 域 KPI 面 + `reports*.visual/snapshot` 涉 purchase 数据面）；若 seed 变更触发 DOM/像素漂移，按 e2e-runbook 视觉方法论双面（DOM + 像素）同步重录，并在本计划 Draft Review Record 区追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [ ] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL 抽样 ≥ 3 张本批表（含 ≥ 1 组主子表头行）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [ ] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/testing/known-good-baselines.md` 最新行（R2c=1542）零漂移；若有漂移按失败模式速查开独立基线裁决
      - Skill: `nop-testing`
- [ ] `docs/logs/`（实际执行日）追加执行条目
      - Skill: none

Exit Criteria:

- [ ] TestErpSeedDataIntegrity 全绿且常量断言通过
- [ ] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [ ] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [ ] compliance checker 零漂移；日志条目在位

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 fresh session `ses_fa594f55dffeDEbu4HnnOsVTWT`）——0 Blocker + 0 Major；3 Minor（①日志条目列于 Proof 相属类型错位（功能性等价）；②71/0/0/1 与 known-good-baselines 69/0/0/1 口径澄清；③roadmap「9 组主子表」措辞漂移宜一行对账说明）——②③已并入本稿，①经裁决保持原位（Phase 3 为终相，Closure Gates 第 3 项引用其全部 Proof 项）；审查者实仓核验 12 实体规格与 seed-data.md 规格表 1:1、ORM tableName/FK 抽查全对、敏感度注记（SupplierPriceList CSV 演示种子不触发 plan-first）判定成立。

## 快照重录合规声明

（占位——仅当 Phase 3 视觉快照双面重录实际触发时填写；未触发则声明「本批次未触发快照重录」）

## Closure Gates

> 全量仓库验证不在本计划闭包门控内：变更面 = 资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖；全量基线登记归 M3.1 兜底工作项。

- [ ] 范围内行为完成（12 CSV 落地 + 常量 + 对账表 + purchase owner doc 段）
- [ ] 相关文档对齐（seed-data.md 对账表 + purchase seed-data.md；快照重录若触发已按协议登记）
- [ ] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

（无——M0.1 规格 CSV-only 可满足性核验已确认本批 12 实体零 ORM 依赖；触发条件未发生）

## Closure

Status Note: （待闭包时填写）

Closure Audit Evidence:

- Auditor / Agent: （待独立结束审计填写）
- Evidence: （待填写）

Follow-up:

- （仅非阻塞跟进项目；已确认的缺陷不得出现在此处）
