# 2026-09-01-0838-2 M1.1b 核心 CRUD 域 seed 扩面 B——inventory（16 CSV）

> Plan Status: completed
> Last Reviewed: 2026-09-01
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.1b（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，270 实体规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁 + CSV 基线常量随批更新协议）、同批 `2026-09-01-0838-1-m11a-md-sal-seed-expansion.md` / `2026-09-01-0838-3-m11c-purchase-seed-expansion.md`
> Audit: required

## Current Baseline

- 实仓 inventory 域 21 个 `app.erp.*` 实体，5 个有 seed（`erp_inv_cost_layer` / `erp_inv_stock_balance` / `erp_inv_stock_ledger` / `erp_inv_stock_move` / `erp_inv_stock_move_line`），**16 缺**（逐实体规格见 `docs/architecture/seed-data.md` 规格表 inventory 节，本计划逐行引用不复制）。
- 16 缺实体构成 **7 组主子表**（CostAdjust / LandedCost / OwnershipTransfer / PickingOrder / Reservation / StockTake / TransferOrder，各 1 头 + 1 行）+ **2 独立表**（Batch / SerialNumber）。注：roadmap M1.1b 行「8 组主子表」为措辞漂移，以 M0.1 规格表（`seed-data.md` inventory 节 7 个「子表」标记）为权威，本计划按规格表口径执行；roadmap 本体勘误归 roadmap owner（沿 M0.2 先例）。必填 FK 全部落在〔已seed〕（ErpMdMaterial / ErpMdWarehouse / ErpMdLocation / ErpMdPartner / ErpMdUoM，跨域 master-data 已 seed）∪〔本批〕集合（M0.1 程序化核验零环、零未知指向）。
- 装载机制（不重做，仅消费）：`DataInitInitializer` 按 ORM 拓扑序按 `{tableName}.csv` 装载，非幂等，fresh-DB 重置为前置；`application.yaml` 默认 `init-database-data: true`（演示/沙盒语义，plan 1143-1）；`zz-sequence-advance.sql` 将 sequence NEXT_VALUE 推进到 100000 → **新增 seed ID 必须落在 < 100000 值域**。
- 门禁基线（M0.2 落地）：`TestErpSeedDataIntegrity` scope-pinning（363 + finance 5 补充集）、零孤儿 CSV、`EXPECTED_APP_ERP_CSV_COUNT`（M0.2 时点 93；本计划执行时基数 = 93 + 已落地 M1.x 批次 CSV 数，按常量 javadoc 随批更新协议推进）+ `EXPECTED_PLATFORM_CSV_COUNT = 4`、全仓 to-one 引用完整性 + 空 `WHITELIST_KEYS`。
- 当前验证基线：`mvn test -pl app-erp-all` 71/0/0/1 全绿（M0.2 plan `2026-09-01-0527-1` 执行实测；`known-good-baselines.md` 最新登记行 69/0/0/1 系 M0.2 前口径，基线追行归 M3.1）；compliance baseline R2c=1542 零漂移；全 reactor 2 处预存回归（hr/drp）为 roadmap Non-Goal successor 项。
- 与既有 inventory seed 的衔接约束：新批次单据（如 StockTake / TransferOrder）的行物料/仓库引用须与既有 `erp_inv_stock_balance` / `erp_inv_stock_move` 的既有维度值（仓库 id 1/2、物料既有 id）语义一致，避免演示数据自相矛盾（如盘盈盘亏指向不存在的库存维度）。
- 剩余差距：16 实体无 seed；`docs/design/inventory/` 无「种子数据」owner doc 段。

## Goals

- 16 个 seed CSV（`erp_inv_*.csv`）落地，7 组主子表引用闭环 + 2 独立表 + 与既有 5 个 inventory seed 衔接，每实体最小可用数据集（行数 ≤ 20，按规格表「建议行数」与用例指示编码 P / N-TERM / N-DIS），FK 引用零悬空。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（+16），seed-data.md 对账表同步。
- `docs/design/inventory/seed-data.md`「种子数据」owner doc 段落地。

## Non-Goals

- 不修改任何 ORM 模型（保护区 auto + dual-agent-approval）——纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` CSV 基线常量随批更新）。
- 不补 inventory 域已有 seed 的 5 实体（不重做既有资产）。
- 不覆盖 master-data/sales（N=1）、purchase（N=3）及其余 M1.x 工作项。
- 不新增 M2.x 像素断言；不做负向视觉断言、跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 hr/drp 2 处预存回归；不触碰 finance 5 个缺 className 实体。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 inventory 节 + 全量化裁决段）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.1b 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）
- Skill Selection Basis: roadmap M1.1b 行指定 `nop-backend-dev`（对齐实体/列/字典约定，路由平台模型文档）；Proof 阶段运行测试与视觉 spec 属测试域，加载 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载）。
- 回滚策略：纯新增 CSV 文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：按文件名 N 序在本批三计划中第二个执行；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，常量基数以「93 + 已落地批次（含 N=1 若先落地）CSV 数」为准，避免覆盖写。

### Phase 1 - Seed CSV authoring（16 CSV）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪）

- [x] 逐实体核对 `module-inventory/model/app-erp-inventory.orm.xml` tableName 后创建 16 个 CSV：`erp_inv_batch.csv`（2~3 行，P+N-TERM）、`erp_inv_cost_adjust.csv`（2~3，P+N-TERM）+ `erp_inv_cost_adjust_line.csv`（1~2/头，P）、`erp_inv_landed_cost.csv`（2~3，P+N-TERM）+ `erp_inv_landed_cost_line.csv`（1~2/头，P）、`erp_inv_ownership_transfer.csv`（2~3，P+N-TERM）+ `erp_inv_ownership_transfer_line.csv`（1~2/头，P）、`erp_inv_picking_order.csv`（2~3，P+N-TERM）+ `erp_inv_picking_order_line.csv`（1~2/头，P）、`erp_inv_reservation.csv`（2~3，P+N-TERM）+ `erp_inv_reservation_line.csv`（1~2/头，P）、`erp_inv_serial_number.csv`（2~3，P+N-TERM）、`erp_inv_stock_take.csv`（2~3，P+N-TERM）+ `erp_inv_stock_take_line.csv`（1~2/头，P）、`erp_inv_transfer_order.csv`（2~3，P+N-TERM）+ `erp_inv_transfer_order_line.csv`（1~2/头，P）
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：7 组主子表行表头 FK 指向〔本批〕头行；跨域必填 FK（ErpMdMaterial / ErpMdWarehouse / ErpMdLocation / ErpMdPartner / ErpMdUoM）指向〔已seed〕行；禁止悬空引用
      - Skill: `nop-backend-dev`
- [x] 与既有 inventory seed 衔接：引用既有仓库/物料维度值时与 `erp_inv_stock_balance` / `erp_inv_stock_move` 既有行语义一致（仓库 id、物料 id 复用既有值；先抽样既有 CSV 确认值域）
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；列集以 ORM/XMeta 生成实体列为准
      - Skill: `nop-backend-dev`
- [x] Proof: 每行数据可装载可查询——由 Phase 3 `TestErpSeedDataIntegrity` 全绿背书
      - Skill: `nop-testing`

Exit Criteria:

- [x] 16 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表（每表 3 行 = 2 P + 1 N-TERM，行表 1/头，共 48 行）
- [x] 无悬空 FK：7 组主子表闭环 + 2 独立表 + 跨域引用全部指向已 seed 行（程序化核验：FK 闭环/字典码/账实一致性全过，执行证据见 Phase 3 执行证据区）

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/inventory/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：执行时点基数 +16；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES` / `EXPECTED_PLATFORM_CSV_COUNT`（105 → 121，javadoc 批次沿革登记）
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md` 对账表计数（inventory 已 seed 5 → 21；全集缺 seed 相应 -16：有 seed 105 → 121 / 缺 seed 258 → 242 / 运行时口径全集缺 247），登记「M1.1b 批次（本 plan）落地」备注行，并同步门禁强化段与快照重录义务节 CSV 资产计数（125 CSV + 1 SQL）
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/inventory/seed-data.md`「种子数据」段：7 组主子表闭环图 + 2 独立表、CSV 文件与行数、FK 标注（〔已seed〕/〔本批〕）、与既有 5 seed 的衔接说明、negative 行语义
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 常量与对账表数值一致（121 / 242），且与 `_init-data/` 实际 CSV 构成一致（125 CSV = 121 app.erp + 4 平台；`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 4/4 全绿实证）
- [x] inventory seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照 M0.2 实测基线 71/0/0/1 全绿或更优；若新 seed 行引起集成测试快照漂移（C03/C08 涉 inv 表），按既有重录协议评估：良性内容增量 → 重录并登记；行为破坏 → 先 Fix 再闭包（首跑 71/0/0/2 = C13/C20a 两处新 seed 行为交互，Fix 于 seed 侧后复跑 71/0/0/1 = 基线一致，处置登记见下方执行证据）
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行受影响视觉 spec（`dashboards*.visual/snapshot` inventory 域 KPI 面 + `reports*.visual/snapshot` 涉 inventory 数据面）；若 seed 变更触发 DOM/像素漂移，按 e2e-runbook 视觉方法论双面（DOM + 像素）同步重录，并在本计划 Draft Review Record 区追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL 抽样 ≥ 3 张本批表（含 ≥ 1 组主子表头行）findPage 行数 == CSV 行数（实跑 16/16 表，含全部 7 组主子表头行）
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/testing/known-good-baselines.md` 最新行（R2c=1542）零漂移；若有漂移按失败模式速查开独立基线裁决
      - Skill: `nop-testing`
- [x] `docs/logs/`（实际执行日）追加执行条目
      - Skill: none

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过（4/4，EXPECTED_APP_ERP_CSV_COUNT=121）
- [x] `mvn test -pl app-erp-all` 与基线一致或更优（71/0/0/1）；快照漂移处置已登记（含快照重录合规声明，若触发——本批未触发，见声明节）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位

执行证据（2026-09-01）：

- `TestErpSeedDataIntegrity` 4/4 全绿 ×2（常量更新后首跑 + CSV 内容修正后复跑）：scope-pinning 363+5 不变 / 零孤儿 CSV / 121+4 基线常量 / 引用完整性零悬空 + 空白名单。
- `mvn test -pl app-erp-all` 终态 **71/0/0/1 BUILD SUCCESS** = M0.2/M1.1a 实测基线。首跑 71/0/0/2 暴露 2 处**新 seed 行与既有集成用例的行为交互**（非预存、非产品缺陷，A/B 隔离 + 快照 diff 实证根因）：
  - **C13** `TestErpC13FinPeriodCloseReverse`：pre-check 响应新增 `landed-cost:LC-2026-001/LC-2026-002`——`ErpFinAccountingPeriodProcessor.findUnresolvedLandedCosts` 将「posted=false + approveStatus=APPROVED + businessDate 在期内」到岸成本单列为结账阻断项，本批 P 行（CONFIRMED/APPROVED）恰好命中。
  - **C20a** `TestErpC20aDrpNetRequirementRelease`：DRP totalReplenishmentQty 140→120（`onOrderQty` 断言 compareTo=-1 即正值）——`DrpDemandAggregator.inboundTransferQty` 将「toWarehouseId=仓2 且 docStatus≠CANCELLED」的本批调拨行（物料 1，1→2，qty 20）计入在途减项，净需求 90→70。
  - **处置（先 Fix 再闭包，Fix 于 seed 侧）**：调拨 2 张 P 单改**物料 3（既有在库 100@仓2）仓 2→仓 1** 方向（合计 80 ≤ 在库，不命中 C20a 查询对 (物料1,仓2)）+ 到岸成本 P 行改 **DRAFT/UNSUBMITTED**（未进审核不构成「已审核悬挂」，不命中 C13 扫描过滤器）。修复后 C13/C20a 快照与 Java 锚点**零改动**（`_cases` 实测与 HEAD 逐字节一致），无需重录。
  - 诊断过程附注：A/B 隔离首测因 CSV 同时残留在 `target/classes`（资源已拷贝）得出「无 CSV 也失败」的错误结论，清点 target 后纠正——教训：资源类 A/B 实验须同时清理 source 与 target/classes。
- fresh-DB `./scripts/start-app.sh restart` 装载 0 冲突 0 列映射错误（app.log 无 seed 相关 ERROR）；GraphQL findPage 抽样 **16/16 表 `total == CSV 行数`（3=3）**：ErpInvBatch / ErpInvSerialNumber / ErpInvReservation(+Line) / ErpInvTransferOrder(+Line)（主子表头）/ ErpInvStockTake(+Line)（主子表头）/ ErpInvPickingOrder(+Line)（主子表头）/ ErpInvOwnershipTransfer(+Line)（主子表头）/ ErpInvCostAdjust(+Line)（主子表头）/ ErpInvLandedCost(+Line)（主子表头）。
- 视觉双面实跑 4 文件 **50/50 全绿零漂移**：`dashboards.visual`(10 域 DOM，含 inventory KPI 面) + `dashboards.snapshot`(10 域像素) + `reports.visual` + `reports.snapshot`(DOM + 像素基线)。
- compliance checker exit 0，**R2c=1542** 与 known-good-baselines 最新行零漂移（纯资源 + 测试常量 + docs 变更，符合预期）。
- `mvn clean install -DskipTests -pl app-erp-all` BUILD SUCCESS，runner jar 实测打包 **125 CSV**（121 app.erp + 4 平台，runtime 证明前置）。

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 fresh session `ses_fa5952d2bffe1FvyRZSsSFLva1`）——1 Major（「8 组主子表」计数错误，规格表权威口径为 7 组主子表 + 2 独立表，roadmap 行措辞漂移）+ 2 Minor（`mvn test -pl app-erp-all` 位于 Phase 3 属模板字面偏差、日志条目列为 Phase 3 项属计划级步骤错位——审查者均判功能等价非阻塞）。Major 与基线口径 Minor 已并入本稿；2 个位置类 Minor 审查者判功能等价，经裁决保持原位（Phase 3 为终相，Closure Gates 第 3 项引用其全部 Proof 项）。
- Independent draft review iteration 2: accept（独立子代理 fresh session `ses_fa58bd522ffeRS2IRsZ3hbY7iA`）——0 残留问题：Major 全解决（「7 组主子表 + 2 独立表」在基线/目标/Phase 1 FK 项/Phase 1 Exit/Phase 2 owner doc 五处一致，对账注记在位）；无新增事实错误（16 实体 / 7 子表标记 / FK 并集与规格表 1:1，7×2+2=16）；2 个裁决保留的 Minor 判定可接受（终相 + Closure Gates 第 3 项覆盖闭环）；模板合规复验通过。

## 快照重录合规声明

**本批次未触发快照重录**（2026-09-01 执行实测，双面核查通过）：

1. **触发条件成立**：本批次新增 16 个部署期 seed CSV（109 → 125，见 `docs/architecture/seed-data.md` 快照重录义务节 M1.1b 注记），按义务规则 1 属触发情形，故执行双面核查。
2. **面 1（各域 `_cases` 快照）零影响**：各域模块测试不声明 `nop.orm.init-database-data`（0330-1/M1.1a 先例口径），本批次未运行各域模块测试，无受影响面；app-erp-all 集成用例 `_cases` 目录经 git 复核与 HEAD 逐字节一致（执行期 C13/C20a 交互已 Fix 于 seed 侧，未触碰任何录制文件）。
3. **面 2（app-erp-all 集成用例快照）实测零漂移**：`mvn test -pl app-erp-all` 71/0/0/1 与 M0.2 基线完全一致——C01-C21 集成用例（CHECKING 态三层全比对）在重设计后 seed 集下零失败，无需重录。
4. **视觉双面（DOM + 像素）实测零漂移**：受影响面评估 = inventory 看板读 stock_balance/stock_move/stock_ledger/stock_move_line + config-gated 批次效期预警（默认关，且本批 batch 行效期 2027 不触发）；10 域报表零读取本批 16 表（grep 实证 ErpInvReportBizModel 仅读 stock_move 族）。实跑 4 spec 文件 50 用例：dashboards.visual（10）+ dashboards.snapshot（10，含 inventory 像素基线）+ reports.visual + reports.snapshot（30）全绿零漂移。
5. **E2E 数值断言联动评估**：本批 16 表不被任何既有 `*.list-value.spec.ts` / `*.value.spec.ts` / 看板 KPI 消费（inventory 看板 KPI 值 = Σ stock_balance.totalCost 与 DONE moves 出入库量，本批不新增 balance/move 行，KPI 值不变），无需同步期望值基线。
6. **提交说明登记**（义务规则 3）：seed 变更 = 新增 16 CSV（erp_inv_batch / serial_number / reservation(+line) / transfer_order(+line) / stock_take(+line) / picking_order(+line) / ownership_transfer(+line) / cost_adjust(+line) / landed_cost(+line)）→ 双面核查记录于本节 + plan Phase 3 执行证据，重录范围 = 零。

## Closure Gates

> 全量仓库验证不在本计划闭包门控内：变更面 = 资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（16 CSV 落地 + 常量 + 对账表 + inventory owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表 + inventory seed-data.md；快照重录若触发已按协议登记——未触发，合规声明节）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——M0.1 规格 CSV-only 可满足性核验已确认本批 16 实体零 ORM 依赖；触发条件未发生）

## Closure

Status Note: 计划可闭合——16 CSV 落地（7 组主子表 + 2 独立表，48 行，125 CSV 实仓/打包双重复核）+ 门禁常量 121 随批更新 + seed-data.md 对账表同步（121/242 + M1.1b 批次行 + 运行时口径 247 + 快照重录资产 125 CSV + 1 SQL）+ inventory owner doc 落地（含执行期测试流中立性裁决登记）；Phase 3 全部 Proof 全绿（门禁 4/4 ×2、模块回归 71/0/0/1 = 基线、fresh-DB 装载 GraphQL 16/16 表 findPage==CSV 行数、视觉双面 50/50 零漂移、compliance R2c=1542 零漂移）；执行期 2 处新 seed 与 C13/C20a 集成用例的行为交互经 seed 侧 Fix 清零（C13/C20a 快照与 Java 锚点零改动，快照重录双面义务未触发）；范围纪律维持（零 ORM / 零生产 Java 变更，唯一 Java 触点 = 门禁常量随批更新）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，task id `ses_fa515a496ffe2CojZ4qmTwv60x`）
- Evidence: VERDICT **APPROVE**（2026-09-01）——A-K 全部 11 审计步 PASS：16 CSV 存在且逐文件 3 行（48 行）、_init-data 全量 125 CSV + 1 SQL 清点；16 表全部列头 vs ORM `code=` 程序化比对零偏差（含 UOM_ID/UO_M_ID 命名分叉两向核实）+ 零审计列；FK 闭环（7 组主子表 + 跨域 md/pur_receive）零悬空 + 字典码全部命中 dict yaml；账实一致性（盘点 BOOK=既有 balance、预留/拣货/调拨/转移数量 ≤ 在库、批次行=balance 值、SOURCE_LOC=DEST_LOC）独立复算通过；门禁常量 121/4/363/空白名单 + git status 实证唯一 Java 触点；seed-data.md 对账表 121/242/247/125+1 四处定位核实 + owner doc 16 表逐行与实仓一致；roadmap M1.1b 行 `done` + plan id 在位；日志条目在位；runner jar 实测 125 CSV；结束审计独立复跑 `TestErpSeedDataIntegrity` 4/4/0/0 全绿。2 MINOR 非阻塞（roadmap done 单元格预登记审计结论措辞——本 APPROVE 落地后即为事实、known-good-baselines 69/0/0/1 滞后口径——已在计划基线登记归 M3.1）。

Follow-up:

- （仅非阻塞跟进项目；已确认的缺陷不得出现在此处）
