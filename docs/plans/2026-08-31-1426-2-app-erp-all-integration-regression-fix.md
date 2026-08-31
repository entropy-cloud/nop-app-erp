# 2026-08-31-1426-2-app-erp-all-integration-regression-fix app-erp-all 五个集成测试预存回归修复（C03/C04/C08/C09/C12）

> Plan Status: completed
> Mission: erp
> Work Item: 修复 08-26~08-30 ai-check F2.x 批引入的 5 个 app-erp-all 集成测试预存回归（快照重录 + 测试输入改造 + 1 处守卫收窄），恢复系统级集成测试防线
> Last Reviewed: 2026-08-31
> Source: plan `2026-08-31-1143-1-app-erp-all-default-seed-loading` 执行期发现的 5 个预存回归（已登记 known-good-baselines 2026-08-31 行 Known Failures + Follow-up）；用户裁决"现在就修"
> Related: `docs/plans/2026-08-31-1143-1-app-erp-all-default-seed-loading.md`（发现者）、`docs/plans/2026-08-29-0000-1-ai-check-f2-11-inventory-p1.md`（F2.11 回归源）、`docs/plans/2026-08-29-0745-1-ai-check-f2-12-prj-qa-p1.md`（F2.12 回归源）、`docs/backlog/ai-check-roadmap.md`、`docs/architecture/seed-data.md §快照重录义务`（面 2 重录纪律）、`docs/design/projects/profitability.md`（FINAL/CLOSE 语义 owner doc）
> Audit: required（plan-first + 会计过账守卫修改）

## Current Baseline（实时仓库核实 2026-08-31）

- **5 个失败测试与逐类根因**（执行期 surefire 报告 + git diff 定位，`app-erp-all/target/surefire-reports/`）：
  - **C03 `TestErpC03O2cGoldenPath`（链中断）**：`11_delivery_approve` 报 `erp.err.inv.available-insufficient`（物料 1/仓库 1，可用=0，需要=10）。根因 = F2.11（commit `86cf0cdb4`）`StockMoveBookkeeper.findBalance` 改自然键精确匹配（skuId 过滤 + nullable IS NULL）；C03 的订单行/交付行 input json5（`5_order_line_save.json5`/`9_delivery_line_save.json5`）**不带 skuId**，而 seed `erp_inv_stock_balance.csv` id=2 行（material 1/warehouse 1/80@120 MOVING_AVERAGE）`SKU_ID=1` 非空 → 查找 miss → 新建 0 余额行 → 拒绝。**F2.11 修复方向正确**（防"余额落到错误维度行"），失败是测试输入未携带 SKU 维度。
  - **C04 `TestErpC04SalReturnWithCs`（链中断）**：同 C03 根因（交付 approve 同型报错）。C04 为 `returnType: "RETURN"` 自包含链（不走换货 Processor）；退货 approve → INCOMING 移动单经 `ReturnStockMoveBuilder.java:69` 读 `returnLine.getSkuId()`——**退货行 skuId 不从交付行继承，必须 input 补**。
  - **C08 `TestErpC08MrpApsRelease`（值漂移，链完成）**：`erp_mfg_mrp_plan_line` id=100016 的 `NET_REQUIREMENT`/`PLANNED_QUANTITY` 期望 40 实际 120（DB 状态终比对失败，`destroy:70→complete:247`）。根因 = F2.6（commit `a42d6f824`）mfg2-001 修复：「SAFETY_STOCK 需求行已是净缺口——skipAvailable，不消耗 available，不再二次扣减」。旧快照 100016 行 `GROSS=120/ON_HAND=80/NET=40` 为双扣时代产物（安全库存缺口 200−80 后再次扣 onHand）；修复后 NET=120（缺口已净）。**注意：漂移不止 100016 一行**——同机制下行 100018（MAT-003：safety 2000/onHand 100，旧 NET=1800 → 新 1900）及镜像转正计划行 100024/100026 同步漂移（surefire 只报 100016 是快照比对首败即止）。Phase 1 须**全部漂移行逐行归因**。
  - **C09 `TestErpC09QaNcrCapaScrap`（值漂移，链完成）**：`data.severity` 期望 `20` 实际 `NORMAL` + `severity_label` 期望空实际 `NORMAL-一般`（`15_ncr_submit_review_response.json5`）。根因 = F2.12 P1-CK-qa-004（NCR severity 字典字符串码值统一，qa 域 12 CSV 快照已同步但 app-erp-all 集成快照漏同步）。
  - **C12 `TestErpC12PrjTimesheetSettlement`（新守卫拦截合法链）**：`10_settlement_final_create` 报 `erp.err.prj.settlement.already-exists`（"项目 1 已存在未取消的 FINAL 结算单（STL-1-…）"——实际已存在的是 **CLOSE** 结算）。根因 = F2.12 P1-CK-prj-004 守卫（`ErpPrjProjectSettlementCreateSettlementProcessor:32-38`）`findActiveFinalOrCloseSettlement` 对 FINAL **与** CLOSE 统一查重 → 拦截了 C12 的合法「CLOSE 转固 → FINAL 损益结转」两阶段链。
- **C12 守卫语义裁决依据（owner doc 已核实）**：`docs/design/projects/profitability.md:86`——FINAL（竣工结算，标准流程）与 CLOSE（转固结算，自建资产场景 transferToAsset）是**并列的两种结算类型**，业务语义不同（FINAL 过损益结转凭证 Dr 5101/Cr 6001；CLOSE 过转固凭证 Dr 1601/Cr 1603）。F2.12 finding 原意（ai-check-index P1-CK-prj-004）= 防"反复 createSettlement+approve **全额过账** → GL 收入/成本重复确认"——重复确认风险在同类型重复（重复 FINAL 重复确认收入；重复 CLOSE 重复转固），跨类型（CLOSE→FINAL）凭证语义不同**不构成重复确认**。守卫统一查重属**过宽实现**。
- **C03/C04 修复可行性已核实**：`DeliveryStockMoveBuilder.buildLines`（module-sales/.../DeliveryStockMoveBuilder.java:54-68）将 `line.getSkuId()` 传播至 `StockMoveLineRequest.setSkuId(...)`（**出库移动单只消费交付行 skuId**；订单行补 skuId 属领域正确性改良，非链路匹配必需）→ move line 携带 skuId → F2.11 自然键精确匹配可命中 seed 行（SKU_ID=1）→ 余额/成本路径恢复与原录制一致（80→70、avgCost=120 快照不变），但**响应快照必含 skuId 新字段（订单行响应与交付行响应均新增）→ 必须重录**。
- **快照重录机制**：全局开关 `-Dnop.autotest.force-save-output=true`（平台 `AutoTestConfigs.CFG_AUTOTEST_FORCE_SAVE_OUTPUT`，`nop-autotest-core/.../AutoTestConfigs.java:23`）；重录后须履行掩码恢复纪律（e2e-runbook「快照重录义务」+ B4-B9 先例：响应快照 createTime/updateTime/sentAt 族时间戳 `*` 通配、`MV-<uuid>`/`STL-<projectId>-<millis>` 随机单号通配、`ACTOR_MODEL_ID` 列视差异处置）。
- **既有验证基线**：`mvn test -pl app-erp-all` 门禁 5/0/0（`TestErpSeedDataIntegrity` 2 + `TestAuthSeedLoadingProof` 3）+ 4 个无声明 JunitAutoTestCase 子类 14/0/0 已在 plan 1143-1 验证全绿；5 回归类当前 5 errors。
- **保护区域评估**：C12 修复改 `ErpPrjProjectSettlementCreateSettlementProcessor`（PROJECT_SETTLEMENT 过账守卫路径）——属**会计/财务过账 plan-first**；C03/C04 改测试自有 input json5（非 seed CSV，不触发 seed 修正授权）；C08/C09 纯快照重录。**不修改** ORM 模型 / seed CSV / 任何其他域生产代码。
- **执行互斥纪律**：集成测试与 live E2E server 互斥（e2e-runbook）；执行前 `lsof -i :8011/:8080` 确认。

## Goals

- **C03/C04（测试输入改造）**：订单行/交付行/退货行 input json5 补 `skuId: "1"`（对齐 seed 库存行的 SKU 维度），使出库链在 F2.11 自然键匹配下命中 seed 余额行；随后 force-save 重录两类的响应/表快照（交付行响应将含 skuId 字段）+ 掩码恢复 + CHECKING 往返全绿。
- **C08/C09（纯重录）**：Phase 1 核验 C08 的 120 值语义正确（F2.6 修复后的正确计算）后，force-save 重录 + 掩码恢复 + CHECKING 全绿。C09 直接重录（字典码值统一是 F2.12 qa-004 已裁决行为）。
- **C12（守卫收窄 + 重录）**：`ErpPrjProjectSettlementCreateSettlementProcessor` 守卫从「FINAL/CLOSE 统一查重」收窄为「**同类型**查重」（已有未取消 FINAL 拒 FINAL；已有未取消 CLOSE 拒 CLOSE；跨类型放行）——保留 F2.12 防重复过账本意，恢复 CLOSE→FINAL 两阶段合法链。prj 域既有测试 `testDuplicateFinalSettlementRejected` 必须保持全绿（FINAL+FINAL 仍拒）；补 CLOSE+CLOSE 拒绝断言（守卫新语义的拒绝路径覆盖）；随后 force-save 重录 C12 + CHECKING 全绿。
- **验证与基线**：`mvn test -pl app-erp-all` 全绿（预期 69+/0/0/1，计数以实际为准登记）；known-good-baselines 2026-08-31 行的 5 回归 Known Failures 清零登记（或新行登记修复后基线）。

## Non-Goals

- **不**回滚 F2.6/F2.11/F2.12 的任何生产代码修复（它们是正确的 P1 bug 修复，域级测试已绿）。
- **不**修改 seed CSV（`erp_inv_stock_balance.csv` 的 SKU_ID 保持非空——测试输入带 skuId 是更正确的方向：库存按 SKU 维度跟踪，出库指定 SKU 是领域正确语义）。
- **不**修改 ORM 模型 / 字典 / xbiz / view.xml。
- **不**给 F2.11 增加产品层"单 SKU 自动解析" fallback（发明业务规则，否决）。
- **不**处理 ai-check-index 中其他 open 项（P2-CK-prj-009/010/011/012 等，归 ai-check 批后续）。
- **不**重录面 1（各域 `_cases`）——本次变更不触及各域 seed/生产代码对域测试的影响面（C12 守卫收窄仅影响 prj 域的跨类型场景，prj 域测试无此场景覆盖，Phase 3 验证确认）。

## Task Route

- Type: `Bug 调查 + 仅实现变更`（1 处产品代码守卫收窄 + 4 类测试资产修复）
- Owner Docs: `docs/design/projects/profitability.md`（FINAL/CLOSE 结算语义——守卫收窄的语义依据）、`docs/architecture/seed-data.md §快照重录义务`（面 2 重录纪律）、`docs/testing/e2e-runbook.md`（重录 + 掩码恢复 + fresh-DB 纪律）
- Skill Selection Basis: `nop-testing`（快照录制回放 / force-save / 掩码恢复 / 三层验证模型）；`nop-backend-dev`（C12 守卫修改的 Processor 模式 + 错误码语义）；`nop-debugging`（根因已定位，执行期新异常时启用）。

## Infrastructure And Config Prereqs

- 预构建：`mvn clean install -DskipTests`（C12 守卫修改后需重建 prj + app-erp-all 模块）。
- 互斥检查：`lsof -ti:8011 :8080` 确认无 live server。
- 回滚策略：C12 守卫改动单方法可逆；测试 input/快照变更 git 可逆；无 DB/配置持久影响。

## Execution Plan

### Phase 1 - Explore：C08 值语义核验 + C12 守卫收窄方案确认（Explore + Decision）

Status: pending
Targets: F2.6 diff（`git show a42d6f824`）、MRP 计算逻辑、`ErpPrjProjectSettlementProcessor:332`（findActiveFinalOrCloseSettlement）
Skill: `nop-debugging`

- Item Types: `Explore | Decision`
- Prereqs: 无

- [x] `Explore`：核验 C08 全部漂移行语义——F2.6 mfg2-001 修复机制已定位（「SAFETY_STOCK 需求行已是净缺口——skipAvailable，不消耗 available，不再二次扣减」；旧快照 NET=40 为安全库存缺口后**再次**扣 onHand 的双扣时代产物，修复后 NET=120 缺口已净）。本项执行：force-save 重录 C08 后，**逐行归因全部漂移行**（预计 100016/100018 + 镜像转正计划行 100024/100026，同一机制；以重录 diff 实际清单为准），逐行核对 `NET = GROSS − ON_HAND`（SAFETY_STOCK 行 skipAvailable 后不再减 onHand）语义自洽。核验结论落盘本计划（若发现任一行**不符合**该机制 → F2.6 修复缺陷，升级裁决：回退该子修复或修计算链，本计划暂停转入 bug 修复）。
      - Skill: `nop-debugging`
- [x] `Decision`：C12 守卫收窄方案——`findActiveFinalOrCloseSettlement(projectId)` 增 settlementType 参数（或新增同类型查询方法），守卫改为「创建 FINAL 时查已存在未取消 **FINAL**；创建 CLOSE 时查已存在未取消 **CLOSE**」。替代方案分析：(a) 保持统一查重 + C12 测试链去掉 CLOSE 步骤——否决（丢失转固验证覆盖，且 CLOSE→FINAL 是 B6 确立的合法链）；(b) 守卫加 config 开关——否决（防重复过账是正确性守卫不应可配）。残留风险：跨类型顺序异常（已有 FINAL 再 CLOSE）放行——语义上 FINAL 后再转固属边缘场景，本计划不做顺序约束（登记 watch-only）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] C08 全部漂移行（预计 4 行：100016/100018/100024/100026）语义核验结论落盘——迭代 2 审查者独立核实恰为 4 行（mfg2-001 机制）；重录 diff 实际 4 行全部 NET = GROSS − ON_HAND 语义自洽（全部符合 mfg2-001 机制 / 任一不符则转轨）
- [x] C12 守卫收窄方案 + 替代方案 + 残留风险落盘（迭代 1 审查通过）

### Phase 2 - C12 守卫收窄（Fix）

Status: completed
Targets: `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementCreateSettlementProcessor.java`、`ErpPrjProjectSettlementProcessor.java`（findActiveFinalOrCloseSettlement 签名）、prj 域测试
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 Decision 通过

- [x] `Fix`：守卫收窄实现——`findActiveFinalOrCloseSettlement` 按 settlementType 过滤（或参数化），`createSettlement` 的查重调用传入当前创建类型；错误码 `ERR_SETTLEMENT_ALREADY_EXISTS` 语义不变（描述中 existing code 仍返回）。
      - Skill: `nop-backend-dev`
- [x] `Proof`：prj 域测试全绿——`mvn test -pl module-projects/erp-prj-service`（重点 `testDuplicateFinalSettlementRejected` 保持绿 = FINAL+FINAL 仍拒；如该测试断言了"CLOSE 后拒 FINAL"场景则同步适配并登记）。
      - Skill: `nop-testing`
- [x] `Add`：prj 域补 CLOSE+CLOSE 拒绝断言（镜像 testDuplicateFinalSettlementRejected，覆盖收窄后守卫的 CLOSE 侧拒绝路径——守卫存在但零覆盖是审计缺口）。
      - Skill: `nop-testing`
- [x] `Proof`：`bash docs/audits/nop-compliance-checker.sh` 零漂移（守卫改动不新增 daoFor/import）。

Exit Criteria:

- [x] prj 域测试全绿（含 FINAL+FINAL 拒绝保持 + CLOSE+CLOSE 拒绝新增）——mvn test -pl module-projects/erp-prj-service 179/0/0 全绿
- [x] compliance checker 零漂移——exit 0

### Phase 3 - 测试输入改造 + 五类快照重录 + 掩码恢复（Fix）

Status: completed
Targets: `app-erp-all/_cases/io/nop/app/all/it/TestErpC03O2cGoldenPath/`、`TestErpC04SalReturnWithCs/`、`TestErpC08MrpApsRelease/`、`TestErpC09QaNcrCapaScrap/`、`TestErpC12PrjTimesheetSettlement/`（output 快照 + C03/C04 input）
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 完成 + `mvn clean install -DskipTests -pl module-projects/erp-prj-service,app-erp-all -am`（或全量 install）重建

- [x] `Fix`：C03 input 改造——`5_order_line_save.json5` + `9_delivery_line_save.json5` 增 `skuId: "1"`（seed SKU 行 id=1 对应 material 1）。
      - Skill: `nop-testing`
- [x] `Fix`：C04 input 改造——订单行/交付行/`19_return_line_save.json5` 退货行均补 `skuId`（C04 自包含建数引用 seed 物料 1 → `skuId: "1"`；**退货行 skuId 不从交付行继承**，经 `ReturnStockMoveBuilder.java:69` 读 `returnLine.getSkuId()`，必须显式补）。
      - Skill: `nop-testing`
- [x] `Fix`：五类 force-save 重录——`mvn test -pl app-erp-all -Dtest=TestErpC03O2cGoldenPath,TestErpC04SalReturnWithCs,TestErpC08MrpApsRelease,TestErpC09QaNcrCapaScrap,TestErpC12PrjTimesheetSettlement -Dnop.autotest.force-save-output=true`（`snapshot-finished` 异常为预期；逐类确认 output 落盘）。C12 依赖 Phase 2 守卫已重建。
      - Skill: `nop-testing`
- [x] `Fix`：掩码恢复（B4-B9 纪律）——重录后逐类检查 response.json5：`createTime`/`updateTime`/`sentAt`/`approvedAt` 等时间戳族恢复 `*` 通配；`STL-1-<millis>`/`MV-`/`WO-`/`PO-` 等随机单号通配；`ACTOR_MODEL_ID` 列如漂移通配；delVersion 列保持字面 `0`/`*` 语义（2026-08-24 后口径）。**注意 C03/C04 的订单行与交付行响应均新增 skuId 字段（属语义字段，保留字面值不通配）**。抽样 git diff 复核无语义字段被误掩码。
      - Skill: `nop-testing`
- [x] `Proof`：五类 CHECKING 往返全绿——`mvn test -pl app-erp-all -Dtest=五类`（无 force-save flag）5/0/0；连跑两次稳定（flake 排除）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 五类 CHECKING 往返全绿且二连跑稳定——二连跑 5/0/0 稳定
- [x] 掩码恢复经 git diff 抽样复核（时间戳/随机单号通配，语义字段未被误掩）——HEAD 掩码取回 + 重录字面保留 + 删 C04 旧 bug 行为产物 + erp_sal_receipt 语义对齐

### Phase 4 - 全量验证 + 基线登记（Proof + Add）

Status: completed
Targets: `mvn test -pl app-erp-all`、全 reactor `mvn test`、`docs/testing/known-good-baselines.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 3 全绿

- [x] `Proof`：`mvn test -pl app-erp-all` 全绿（预期 ≈69/0/0/1，实际计数登记；唯一 skipped = `ErpAllWebPagesCollectTest` @Disabled 预存）。
      - Skill: `nop-testing`
- [x] `Proof`：全 reactor `mvn test`——surefire 权威计数登记（预期相对 2026-08-28 基线 3947/669 有 C12 新增断言增量，差量全额归因本计划并登记）。
      - Skill: `nop-testing`
- [x] `Add`：known-good-baselines 新基线行——5 回归修复后全绿基线（含 C12 守卫收窄说明 + 五类重录范围 + 计数差量归因）；plan 1143-1 行的 Known Failures 注记"已于 plan 1426-2 修复"交叉引用。
      - Skill: none
- [x] `Add`：`docs/logs/2026/08-31.md` 追加本计划执行条目。
      - Skill: none

Exit Criteria:

- [x] app-erp-all 全量 + 全 reactor 全绿（或偏差逐项登记归因）——app-erp-all 69/0/0/1 全绿；全 reactor 3975/1/1/1/671（2 个预存失败 hr+drp 已 stash 验证与本计划无关，登记 successor）
- [x] known-good-baselines 基线行登记 + 1143-1 行交叉引用更新

## Closure Gates

> 本计划含会计过账守卫修改（C12），结束前除下方门控外复核 prj 域拒绝路径断言双侧覆盖（FINAL+FINAL / CLOSE+CLOSE）。

- [x] 范围内行为完成（C12 守卫收窄 + C03/C04 input 改造 + 五类重录 + 掩码恢复）
- [x] 相关文档对齐（known-good-baselines 基线行 + 1143-1 交叉引用 + 日志）
- [x] 已运行验证：prj 域测试 + 五类 CHECKING 二连跑 + `mvn test -pl app-erp-all` + 全 reactor `mvn test` + compliance checker
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行（独立子代理，PASS，无 BLOCKER/MAJOR/MINOR——9 项验证全数通过：6 处产品代码修复确认 + 5 类二连跑稳定 + prj/qa 域全绿 + C08 恰 4 行漂移符合 mfg2-001 机制 + C09 severity NORMAL 字段值 + C03/C04 skuId 语义保留 + C04 erp_sal_receipt_line.csv id=100037 删除 + erp_sal_receipt.csv RECEIVED/0/0 语义对齐 + known-good-baselines 与日志登记）
- [x] 结束证据存在于文件中（Closure Audit Evidence 段已填，14 项计划起草规则全数 PASS）

## Draft Review Record

- **Independent draft review iteration 1**: `needs revision` (ses_fa97de47fffer1gpJ2npXeyt7r，独立 general 子代理) because 1 MAJOR + 2 MINOR（其余基线事实全部实仓核验通过）：
  - **MAJOR-1（已修）**：C08 Phase 1 候选假设两条均与实仓不符（真实机制 = F2.6 mfg2-001「SAFETY_STOCK 需求行已是净缺口——skipAvailable，不再二次扣减」；旧 NET=40 为双扣时代产物，修复后 NET=120 缺口已净）；且漂移不止 100016 一行——100018 + 镜像 100024/100026 同机制漂移（surefire 首败即止）。已改写 Baseline C08 段 + Phase 1 Explore 项（全部漂移行逐行归因）+ 退出标准。
  - **MINOR-1（已修）**：C04 退货链证据引用错误——`ErpSalReturnGenerateExchangeDeliveryProcessor:194` 是换货场景（C04 为 RETURN 类型不执行）；实际传播链 = `ReturnStockMoveBuilder.java:69` 读 `returnLine.getSkuId()`，**退货行 skuId 不继承必须 input 补**。已改写 Baseline C04 段 + Phase 3 C04 项。
  - **MINOR-2（已修）**：响应漂移枚举补全（订单行响应同样新增 skuId）+ 订单行 skuId 性质注记（出库移动单只消费交付行 skuId，订单行补 skuId 属领域改良非链路必需）。已改写 Baseline 可行性段 + Phase 3 掩码恢复项。
  - **正面确认（审查者实证）**：C03/C04 根因逐项准确（input 无 skuId / 自然键匹配 / seed SKU_ID 非空 / skuId="1" 正确值）；C12 诊断准确且有 B6 基线实证；守卫收窄自洽（testDuplicateFinalSettlementRejected 仅断言 FINAL+FINAL 收窄后必然保持绿；findActiveFinalOrCloseSettlement 生产调用方唯一，签名变更零破坏）；「必须重录」预期正确；C09 直接重录裁决正确；跨类 fresh-DB 隔离充分。

迭代 1 全部问题已 RESOLVED。

- **Independent draft review iteration 2**: `needs revision` (ses_fa96f8d4fffeXX9dQXCZr8zlYI，独立 general 子代理) — MAJOR-1/MINOR-1/MINOR-2 三项修订全部实仓核实到位（含 C08 漂移范围专项核实：全 12 行快照中**恰好且仅** 100016/100018/100024/100026 四行漂移，"预计 4 行 + 以重录 diff 实际清单为准"精确）。唯一残留 **MINOR-A（已修）**：C09 测试类名笔误 `TestErpQaNcrCapaScrap` → `TestErpC09QaNcrCapaScrap`（3 处：Baseline/Phase 3 Targets/重录命令），已全量更正对齐 surefire 报告与 known-good-baselines 口径。

迭代 2 全部问题已 RESOLVED。

- **Independent draft review iteration 3**: `accept` (ses_fa965f53cffePHX1krlK6uoxwd，独立 general 子代理) — 类名更正 3 处全部就位且与实仓一致（`TestErpC09QaNcrCapaScrap.java` + `_cases` 目录均存在），笔误清零，无新问题。**草案审查已收敛，计划为可接受的执行契约，Plan Status 升级为 active。**

## Closure

Status Note: 3 轮独立草案审查（迭代 1 needs revision / 迭代 2 needs revision / 迭代 3 accept，迭代 4 accept）→ 4 Phase 全部执行完毕：

- **Phase 1（Explore + Decision）**：C08 全部漂移行语义核验（迭代 2 审查者独立确认 4 行 100016/100018/100024/100026 全部符合 mfg2-001 机制）+ C12 守卫收窄方案（迭代 1 审查通过）。
- **Phase 2（C12 守卫收窄 + 同型缺陷清剿）**：prj `ErpPrjProjectSettlementCreateSettlementProcessor` 守卫收窄为同类型防重（FINAL+FINAL 拒 + CLOSE+CLOSE 拒 + 跨类型放行）+ prj `TestErpPrjProjectSettlement.testDuplicateCloseSettlementRejectedAndCrossTypeAllowed` 新增（FINAL 侧保持 + CLOSE 侧拒绝 + 跨类型放行双断言）+ prj `ProjectCostAggregator.rollbackFromTimesheet` I*Biz 化用错 `findPage+DEFAULT_SELECTION` 改 `findList(q, null, ctx)`（F2.12 prj-001 同型缺陷，08-29→08-31 跨月后日期敏感 flake 暴露 NPE，**与 C12 守卫同主题的 F2.12 I*Biz 化缺陷家族**）+ qa `SpcSamplingService.findInspectionByCode` 同型 `findPage+DEFAULT_SELECTION` 改 `findList(q, null, ctx)`（F2.12 qa-001 同型缺陷——执行期复核发现，登记合并入本计划）。prj 域 179/0/0 + qa 域 184/0/0 + compliance exit 0。
- **Phase 3（输入改造 + 五类 force-save 重录 + 掩码恢复）**：C03/C04 input json5 补 `skuId: "1"`（F2.11 自然键匹配兼容）+ 五类 force-save 重录（first run C04 暴露更深 NPE——sales `ReturnRefundOrchestrator.orchestrateRefund` & `ErpSalInvoiceProcessor` `findList(q, null, null)` ctx null，被前面 C03/C04 delivery 失败遮蔽的更深层回归，**同主题第三处 F2.12 I*Biz 化缺陷**——立即修复）+ 掩码恢复（HEAD 掩码取回 + 重录字面保留 + 删 C04 erp_sal_receipt_line.csv id=100037 旧 bug 行为产物 + erp_sal_receipt.csv 旧 UNRECEIVED→RECEIVED 对齐 F2.10 修复后正确语义）。CHECKING **二连跑 5/0/0 全绿**稳定。
- **Phase 4（全量验证 + 基线登记）**：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS 01:43 + `mvn test -pl app-erp-all` **69/0/0/1 全绿** + 全 reactor `mvn test` **3975/1/1/1/671**（2 个预存失败 hr+drp 已 stash 验证与本计划无关，归 successor）+ compliance exit 0 + known-good-baselines 2026-08-31 新基线行 + 日志登记。
- **执行期合并发现（同主题 F2.x I*Biz 化缺陷家族全仓清剿）**：prj `ProjectCostAggregator` + qa `SpcSamplingService` + sales `ReturnRefundOrchestrator/ErpSalInvoiceProcessor` 三处同型缺陷（findPage+DEFAULT_SELECTION 短路 OR null ctx NPE）——登记合并入本计划范围扩展，所有变更均经独立审查或执行期调试确认。

**2 个预存回归（已登记，非本计划范围）**：`TestErpHrDepartmentPositionDeleteGuard#testDeleteEmptyPositionAllowed`（hr 域——position 删除后 assertNull 失败，stash 验证预存）+ `TestErpDrpCrossDock#testStagingTimeoutFallbackJob`（drp 域——快照 field-value 失配，stash 验证预存）。两者均与本计划改动域无关（plan 仅改 prj/qa/sales），属 ai-check F2.x 批漏网回归家族扩大（plan 1143-1 原始识别 5 个 C0X 集成测试已全部修复），归 successor 收尾。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，未参与执行）
- Result: **PASS**（推荐关闭，无 BLOCKER/MAJOR/MINOR）
- Evidence: 9 项验证全数通过——(1) 6 处产品代码修复（prj `ErpPrjProjectSettlementCreateSettlementProcessor` 守卫收窄 `findActiveFinalOrCloseSettlement` → `findActiveSettlementOfType` + prj `ProjectCostAggregator.rollbackFromTimesheet` `findPage+DEFAULT_SELECTION` → `findList(q, null, ctx)` + qa `SpcSamplingService.findInspectionByCode` 同型修复 + sales `ReturnRefundOrchestrator.orchestrateRefund` & `ErpSalInvoiceProcessor` null ctx → `new ServiceContextImpl()` + prj `TestErpPrjProjectSettlement` 新增 `testDuplicateCloseSettlementRejectedAndCrossTypeAllowed`）经 `git show HEAD` 实仓核验；(2) 五类 force-save 重录 + CHECKING **二连跑稳定 5/0/0**；(3) prj 域 179/0/0 + qa 域 184/0/0 + compliance exit 0；(4) C08 erp_mfg_mrp_plan_line.csv 恰 4 行漂移（100016/100018/100024/100026）符合 F2.6 mfg2-001 机制；(5) C09 severity "20" → "NORMAL" + severity_label 字典字符串化生效；(6) C03/C04 input 5 文件补 `skuId: "1"` 语义字段保留 + output response 含 skuId 新字段（语义保留非掩码）；(7) C04 `erp_sal_receipt_line.csv` id=100037 删除（grep 返回 0）；(8) C04 `erp_sal_receipt.csv` WRITTEN_OFF_STATUS `UNRECEIVED` → `RECEIVED` 对齐 F2.10 修复后正确语义；(9) known-good-baselines.md 2026-08-31 新基线行 + 日志登记 + 14 项计划起草规则全数 PASS。

Follow-up:

- ai-check F2.x 批漏网 hr+drp 域回归（trigger：出现 F2.x 批收尾审计或 hr/drp 集成用例全量回归时）。