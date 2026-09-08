---
status: active
mission: ai-check-r3
work-item: M1.13
group: "2026-09-08-1454"
verify: [test]
---

# 2026-09-08-1454-3 M1.13 purchase + sales + inventory 五维符合性审计（U03 + U04 + U02 全格）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK checker report mode CAT1..4 = 0/0/0/0、`--strict`/`--self-test` PASS、双 checker 零回归、白名单 27 文件四要素齐备）。M1.13 三单元 deps 均为 M0.6 + MI.9，已全部满足。
- 同 mission 先例：M1.1/M1.5/M1.8 三切片（plans `2026-09-08-1042-1/2/3`）已执行并独立闭包审计 ACCEPT，`ck-finance-posting-r3.md` / `ck-mfg-workorder-r3.md` / `ck-assets-lifecycle-r3.md` 已落盘——本计划沿用其通过的七阶段执行形态，扩展为三单元。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U03 × 五维 + U04 × 五维 + U02 × 五维（各全格）= 15 格**（§4 映射表第 13 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准，执行期只允许在查重裁决列追加证据，不改判定标准。
- U03 purchase：owner doc `docs/design/purchase/`（`README.md`/`state-machine.md`/`three-way-match.md`/`requisition.md`/`returns.md`/`supplier-evaluation.md`/`use-cases.md`，实仓核验在盘）；物理面 `module-purchase/erp-pur-{dao,service,web}` 的 `src/main`；§3.3 焦点：B = 三单匹配/SoD 前移/状态锁基类消费/容差配置量纲；F = 三单匹配页（复杂手写页清单成员）；S = P2P 链 seed（PO→Receive→Invoice→Payment）posted 一致性裁决；T = P0 采购到付款全流程清单行覆盖；I = pur 探针族（CAT-1 22/CAT-2 32，MI 后应零）。
- U04 sales：owner doc `docs/design/sales/`（`README.md`/`state-machine.md`/`quotation.md`/`contract.md`/`returns.md`/`use-cases.md`，实仓核验在盘）；物理面 `module-sales/erp-sal-{dao,service,web}` 的 `src/main`；§3.3 焦点：B = 促销规则引擎/取价链/核销状态守卫/红冲前置消费侧行为（归属边界 §3.2）；F = 销售链页面（报价/订单/出库/发票）；S = O2C 链 seed（SO→Delivery→Invoice→Receipt）posted 一致性；T = P0 销售到收款全流程清单行覆盖；I = sal 探针族（CAT-1 22/CAT-2 30，MI 后应零）。
- U02 inventory：owner doc `docs/design/inventory/`（`README.md`/`state-machine.md`/`cross-domain.md`/`trace-chain.md`/`consignment.md`/`audit-snapshot-cycle-count.md`/`use-cases.md`，实仓核验在盘）；物理面 `module-inventory/erp-inv-{dao,service,web}` 的 `src/main`；§3.3 焦点：B = 出库策略族 locationId 回退/自然键余额查找/不可变流水基类/批次序列守卫；F = 库存移动确认页（复杂手写页清单成员）/批次序列选择；S = stock_move/balance/cost_layer seed（2210-1 批）+ 域内金额自洽（balance↔cost_layer）；T = P0 库存移动+流水+余额一致性清单行覆盖；I = inv 探针族（CAT-1 26/CAT-2 7，MI 后应零）。
- 共享代码唯一归属（冻结清单 §3.2）：**posting processor 族本体唯一归属 fin-1**——sales 发票红冲前置 / inventory 凭证 REQUIRES_NEW 为消费侧，消费侧行为归本切片，涉引擎内部缺陷时标注「归属 fin-1」归并（r1 P1-CK-sal-003 即跨域核对归属 C3.1 先例）；common 抽象族（`AbstractErpCrudBizModel` 状态锁基类——pur/sal 大量消费点）行为缺陷归 U20，本切片只审调用点；聚合横切面归 U21；notify 派发子系统本体归 U11。
- 跨轮查重源（§2）：r1 `docs/audits/check/ai-check-index.md`（`ck-purchase.md` C2.1：0 P0 / 3 P1 / 6 P2 / 5 P3，done——P1-CK-pur-001~003 fixed、P2-CK-pur-004~009 open、P3-CK-pur-010~014 open；`ck-sales.md` C2.2：0 P0 / 4 P1 / 16 P2 / 10 P3，done——P1-CK-sal-001~004 fixed（sal-003 跨域归属 C3.1）、P2-CK-sal-005~018 open；`ck-inventory.md` C2.3：0 P0 / 5 P1 / 11 P2 / 5 P3，done）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照（已裁决偏离不重复报告）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：pur/sal/inv 探针族 MI 后已清零——DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级裁决报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。白名单条目：pur 1（`ErpPurDashboardBizModel.java`，C1）+ sal 1（`ErpSalDashboardBizModel.java`，C1）+ inv 3（`IErpInvStockLedgerBiz.java`/`ErpInvDashboardBizModel.java`/`ErpInvStockLedgerBizModel.java`，均 C1）= 5 条。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；模块回归对照计数 pur 341 / sal 316 / inv 248（cjk-baseline §批注账 MI.6 批 2 / MI.8 批 2 行锚点；执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露）；`npm run validate:flux` step [1/3] 导出 0 error（999 页 / erp 855），整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移（successor 在案，非本切片 finding）。
- 仓库现状（2026-09-08 起草时实核）：HEAD `f40b4bbae`，工作树零脏面。审计证据以实跑时点 HEAD + 脏面披露为准（MI.9 收官审计先例：脏树实跑 + 披露）。
- 剩余差距：U03/U04/U02 各五格 verdict 未落盘；执行目录尚无 purchase/sales/inventory 切片 `ck-*.md` 报告。

## Goals

- 按冻结清单对 U03 + U04 + U02 共 15 格全跑（禁止抽样、禁止跳维），逐格落 verdict（`pass` / `finding` / `n-a` 带理由），产出三份报告 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-purchase-r3.md` + `ck-sales-r3.md` + `ck-inventory-r3.md`（各含五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明，roadmap 横切关注点 13）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；三单元 DIM-B 维各含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部该单元 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单三行 + 跨轮 `docs/audits/check/ai-check-index.md`（§报告清单 M1.13 行 + §Finding 追踪新 ID 行）。
- 全程零生产代码改动（roadmap 规则 6）：只产 finding 与索引，收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（M1 只读审计；修复归 M2.x，强制先写失败测试）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 U05 fin 格（M1.2~M1.4 待其依赖翻转）与三单元之外任何单元格；posting 引擎内部、状态锁基类本体、聚合机制按 §3.2 归属归并，不在本切片重复立项。
- 不接管 r1/r2 工作项（横切关注点 5）；不重开既有裁决（lesson 09/10、compliance baseline、CJK 白名单、P2P/O2C seed posted 双射裁决——M1.1 已核，本切片按 §3.3 各自 S 行焦点复核增量面）；不做 roadmap 状态翻转（done 转换由结束审计机制处置）。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用，不新建目录）；盘点注记落本计划勾选注记
> Prereqs: 无

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，幂等复用；roadmap 规则 8），确认 `ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 在位
      - Skill: none
      - 证据（2026-09-08 T0）：目录已存在（18 条目，幂等复用零新建）；`ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 均在位；索引头部登记路径 = `docs/audits/check/2026-09-06-1645-ai-check-r3/` 与实际一致
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、同批姊妹计划（`2026-09-08-1454-1/2`）执行状态披露（MI.9 收官审计脏树实跑先例）；后续全部证据注记引用该时点
      - Skill: none
      - 证据：**T0 = HEAD `e0eda4d231f0f95e48eb1f2623edb26cd25134d5`（2026-09-08，docs-only 提交链——plan 基线 `f40b4bbae` 后仅推进 M1.10 hr 与 M1.12 projects+quality 审计产物落盘两笔提交，生产代码零变化）**；脏面 = 1 条 untracked 计划文件（本计划 `2026-09-08-1454-3`），无 tracked 修改；姊妹计划状态披露：`1454-1`（M1.10 hr）已执行并独立闭包审计 ACCEPT（提交 c8509908f）；`1454-2`（M1.12 projects+quality）已执行并独立闭包审计 ACCEPT（提交 e0eda4d23，`ck-projects-r3.md`/`ck-quality-r3.md` 已登记索引）；后续全部证据注记引用 T0
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据（T0 实跑）：compliance checker **零漂移**，全表与 M0.3 快照行逐值一致（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）；CJK checker report mode **CAT1..4 = 0/0/0/0**（3430 java + 886 yaml 扫描，per-domain 违规节空）= MI 终态行精确一致；无漂移登记项

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（U03 + U04 + U02，15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（4 Proof + 3 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.13 行指定，承 M1.1 行「同上」）
> Targets: `module-purchase/erp-pur-dao|erp-pur-service/src/main/java` + `module-sales/erp-sal-dao|erp-sal-service/src/main/java` + `module-inventory/erp-inv-dao|erp-inv-service/src/main/java`；走查 verdict 落勾选注记
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑（三单元范围）：compliance checker 逐规则对照 + 反模式 grep 族（`extends RuntimeException`/`@Inject private`/`System.currentTimeMillis`/`@Transactional`×`@BizMutation` 对照 R6 基线/`IDaoProvider|IOrmTemplate|@SqlLibMapper` 命中处注释理由）+ codegen 产物安全（`__XGEN_FORCE_OVERRIDE__`、`_gen` 脏面）+ 聚合完整性（按 §6 勘误 E1 路径）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0 实跑）：①checker 全表 = M0.3 快照行逐值一致（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0，零新增违规）；②反模式 grep 六路径（pur/sal/inv × dao/service）：`extends RuntimeException`=0、`@Inject private`=0、`System.currentTimeMillis`=0（全部 CoreMetrics）；③`@Transactional`×`@BizMutation` 同文件命中 = pur 6 Processor + sal 6 Processor + inv 0——逐文件核验均为 javadoc 事务边界说明行（「本类不带 @Transactional」），非真实方法级共存，真实 R6=2 = ErpFinVoucherBizModel（compliance-baseline §BASELINE 已裁决偏离）；④`IDaoProvider|IOrmTemplate|@SqlLibMapper` 命中处抽查（pur 17 文件/sal 15/inv 25+）均有 javadoc 豁免理由（SPI 解析器/同模块直查/dashboard 聚合/StandardCostResolver 架构不变量 E3.2 Q4 等范式），对齐 R2 基线已裁决面；⑤`__XGEN_FORCE_OVERRIDE__` 命中全部位于 erp-{pur,sal,inv}-meta `dict/*.dict.yaml`（只读校验点，模型 dict 真相源）+ `target/classes` 构建副本，`_gen`/`_` 脏面 = 空；⑥聚合完整性按 §6 勘误 E1 路径：`app-erp-all/.../nop/main/auth/app.action-auth.xml` 在位，`x:extends` 注册 `/erp/pur/auth/erp-pur.action-auth.xml` + `/erp/sal/...` + `/erp/inv/...` 三域齐备
- [x] <Proof> U03 purchase 15 维度逐维走查（程序式确定性走查落 verdict；焦点：⑧状态机——三单匹配与单据状态守卫链/容差配置量纲；④IoC/事务——SoD 前移与 REQUIRES_NEW 边界（P1-CK-pur-002 fixed 复核）；②跨实体——状态锁基类调用点合规（本体归 U20）；⑮断言抽样 `state-machine.md` + `three-way-match.md` ≥2 doc × 2 断言）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0）：15 维无跳维。①Model→Delta→Java：实体服务 20 BizModel 全部继承 `AbstractErpCrudBizModel`/标准 `CrudBizModel`，`_gen` 零脏面；②跨实体：状态锁基类调用点合规（20 文件 extends，基类本体归 U20）；dashboard/priceResolver 等 IDaoProvider 直查均有注释理由；③异常：0 RuntimeException，错误码集中 `ErpPurErrors`（`erp.err.pur.*`）；④IoC/事务：SoD 前移复核有效（`ErpPurInvoiceApproveProcessor#approve` L24-27 SoD-first 于 doPosting 之前 + 承付 hook 前移 L31-33，`doApprove` L225 注记 F1.2）；REQUIRES_NEW = R10 基线 14 内已裁决偏离；⑤平台辅助：0 `System.currentTimeMillis`；⑥标准服务模式：@BizQuery/@BizMutation 规范；⑦跨模块：md 引用经 I*Biz/materialBiz.get 权限管道（StockMoveProcessor L179 注记）；⑧状态机：三单匹配 ThreeWayMatcher 数量强制项 + strict-mode 门控与 three-way-match.md §差异处理一致；**容差量纲缺陷仍在**——Dashboard `loadActiveInvoicesInRange` 族读 `CONFIG_MATCH_PRICE_TOLERANCE` 默认 `0.05`（ratio），ThreeWayMatcher `priceTolerancePercent()` 同键默认 `"5"`（percent），同键双量纲未收敛（= r1 P2-CK-pur-007 现症复核）；⑨审批流：per-mutation Processor + SoDGuard + withdraw 族规范；⑩定制：无越序；⑪多租户/本地化：无源模型预置 tenantId；⑫测试面见 DIM-T；⑬Codegen 安全：dict.yaml 只读校验点齐备零手改；⑭聚合：aggregator 三域注册在案；⑮断言抽样 4 核查点：`state-machine.md` §异常路径 L99「付款核销时发票已作废→拒绝核销」 vs `PaymentSettler.java` settle/reverseSettlement 全文 **零 docStatus/CANCELLED 引用**（settle L65-71 仅验 approveStatus）→ **漂移（major）**；`state-machine.md` §docStatus 轴注记 L23「ACTIVE 死状态零 writer」 vs Dashboard 查询 L252/261/278 已改 `approveStatus=APPROVED+ne(docStatus,CANCELLED)` → 一致（pur-001 修复复核有效；残留 javadoc L54-55/L176 仍书旧「docStatus=ACTIVE」口径措辞 → minor 漂移）；`three-way-match.md` §配置表 L100 `erp-pur.match-price-tolerance | 5 (%)` vs ThreeWayMatcher L174 默认 `"5"` percent → 一致（同时坐实 Dashboard ratio 侧偏离 = pur-007）；`three-way-match.md` §处理策略 L71「发票数量>入库数量→拒绝」vs ThreeWayMatcher L30 javadoc「不得超过（强制项）」→ 一致
- [x] <Decision> U03 候选 finding 逐条过 §2 三态裁决（查 r1 `ck-purchase.md` §Finding 追踪 + r2 目录 + §Mission 基线快照）：复用/归并原 ID/新立 `P{n}-CK-pur-{NNN}-r3`；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt
      - 裁决（14 项全部过三态）：**复用 3**——P1-CK-pur-001（Dashboard 查询 HEAD 复核有效：L252/261/278 真实过滤正确）/ P1-CK-pur-002（SoD-first 前移 HEAD 复核有效：ApproveProcessor L26 守卫先于 L34 doPosting）/ P1-CK-pur-003（状态锁基类 20 pur BizModel 全接入，HEAD 复核有效）；**归并 11**——P2-CK-pur-004/005/006/007（现症复核：同键 ratio vs percent 仍在）/008/009 + P3-CK-pur-010/011/012/013/014（open 状态无修复提交，`git log` 末笔生产提交 `bdb469d22`=2026-09-07 MI.6 批2 早于 r1 修复批次收官，其后仅 docs 提交）；**新立 2**——`P2-CK-pur-015-r3` settle/reverseSettlement 不拒绝已作废发票/付款（PaymentSettler 零 docStatus 守卫；与 pur-005 cancel 侧互为镜像不同控制点，对齐 r1 sal-010/sal-012 分立先例；owner doc 断言漂移即维度⑮抽中项）；`P3-CK-pur-016-r3` Dashboard javadoc 口径措辞漂移（L54-55/L176 仍书「docStatus=ACTIVE」旧口径，代码已修文档未随——pur-001 同站点残留，措辞级 P3）；历史 14 ID 零覆写
- [x] <Proof> U04 sales 15 维度逐维走查（程序式确定性走查落 verdict；焦点：⑧状态机——促销规则引擎/取价链/核销状态守卫；②跨实体——红冲前置消费侧行为（归属边界 §3.2，引擎内部标注归属 fin-1）；⑮断言抽样 `state-machine.md` + `returns.md`/`quotation.md` ≥2 doc × 2 断言）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0）：15 维无跳维。①16+ BizModel 全继承状态锁基类/标准链；②红冲前置消费侧复核有效：`ErpSalInvoiceReverseApproveProcessor` L40-42 + `ErpSalInvoiceCancelProcessor` L42 `posted || hasActivePosting(code)` 双判（P1-CK-sal-003 sales 侧收口在位；引擎内部归 fin-1 不展开）；IDaoProvider/IOrmTemplate 消费点（ReceiptSettler/ReturnRefundOrchestrator/pricing resolver/dashboard）注释理由齐备；③异常 0 RuntimeException；④SoD/REQUIRES_NEW 同 pur 口径（R10 基线内）；⑤CoreMetrics 全量；⑧促销引擎 `ErpSalPricingRuleEngine.lineMatchesRuleTarget` L166-171 仍仅 materialId 匹配（materialCategoryId 维度缺失 = r1 P2-CK-sal-005 现症复核）；`ReceiptSettler` 全文零 docStatus 引用（= P2-CK-sal-010 现症）；`ErpSalDeliveryProcessor.findApprovedDeliveries` L381-385 仅 approveStatus 过滤（= P2-CK-sal-011 现症）；receipt reverseApprove/cancel 无 reverseSettlement 编排（= P2-CK-sal-012 现症）；⑮断言抽样 5 核查点：`state-machine.md` L38「posted=true 后物理锁定」 vs 状态锁基类 16 sal BizModel 接入 → 一致（sal-004 修复复核）；`state-machine.md` L80「收款核销时发票已作废→拒绝核销」 vs ReceiptSettler 零 docStatus → **漂移（= sal-010 归并）**；`returns.md` L427-428 RC-R1.19 已核销发票守卫链路（deliveryLineId→invoiceLine→invoice） vs `ReturnRefundOrchestrator` L77-99 同链路反转 → 一致（sal-001 修复复核）；`returns.md` L33 价差语义（EXDIFF- 前缀/Δ<0 复用 orchestrator/幂等 ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED）vs 实现注记 → 一致；`quotation.md` 面 = sal-015/016 既有 open 归并不重开
- [x] <Decision> U04 候选 finding 逐条过 §2 三态裁决（查 r1 `ck-sales.md` §Finding 追踪 + r2 目录 + §Mission 基线快照）：复用/归并原 ID/新立 `P{n}-CK-sal-{NNN}-r3`；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt
      - 裁决（18 项全部过三态）：**复用 4**——P1-CK-sal-001（关联发票限定链路 HEAD 复核有效 L77-99）/ P1-CK-sal-002（countActiveOrders L229 APPROVED+≠CANCELLED HEAD 复核有效）/ P1-CK-sal-003（finance F2.1 + sales 侧收口 `posted||hasActivePosting` HEAD 复核有效；引擎内部维持归属 C3.1/fin-1）/ P1-CK-sal-004（状态锁基类接入）；**归并 14**——P2-CK-sal-005（现症复核 L166-171）/006/007/008/009/010（现症复核零 docStatus）/011（现症复核 L383）/012（现症复核零 reverseSettlement）/013/014/015/016/017/018（open 无修复提交）；**新立 0**；历史 18 ID 零覆写
- [x] <Proof> U02 inventory 15 维度逐维走查（程序式确定性走查落 verdict；焦点：⑧状态机——出库策略族 locationId 回退/自然键余额查找/不可变流水基类/批次序列守卫；④IoC/事务——凭证 REQUIRES_NEW 消费侧（归属边界 §3.2）；⑮断言抽样 `state-machine.md` + `trace-chain.md`/`cross-domain.md` ≥2 doc × 2 断言）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0）：15 维无跳维。①21 BizModel 全继承状态锁基类；②`ErpInvStockLedgerBizModel extends AbstractErpImmutableCrudBizModel`（P1-CK-inv-003 修复复核：16 行裸 CRUD 桩 → 不可变基类 + 审计快照派生视图）；IDaoProvider/IOrmTemplate（costing 策略族/Bookkeeper/StandardCostResolver E3.2 架构不变量注释）合规；③异常 0 RuntimeException；④凭证 REQUIRES_NEW 消费侧 = R10 基线内（P2-CK-inv-012 已证伪+收敛 F1.1，HEAD 复核无回归）；⑤CoreMetrics 全量；⑧出库策略族：4 策略 locationId 回退均改 `move.getSourceLocationId()`（P1-CK-inv-001 修复复核有效，WeightedAverage/Batch/Lifo/Specific L74-75/L85-86/L82-83/L81-82）；`StockMoveBookkeeper.findBalance` L452-461 统一委托 `findBalanceByNaturalKey`（skuId 过滤 + nullable 列 IS NULL，P1-CK-inv-002 修复复核有效）；批次序列守卫：`validateBatchSerialPresence` L204-233 批次必填+在库+序列必填（P1-CK-inv-004 修复复核有效）——**但序列「已售拒绝出库」状态校验仍缺**（serial-status 无出库链 writer/守卫，仅 ErpInvSkuReferenceChecker 引用检查消费 ACTIVE 集合；owner doc L67 断言漂移）；⑮断言抽样 4 核查点：`state-machine.md` L65「批次/序列号缺失拒绝确认」 vs validateBatchSerialPresence → 一致（inv-004 修复）；`state-machine.md` L67「已售序列号拒绝再次出库」 vs 出库链零序列状态校验 → **漂移（major）**；`state-machine.md` L40「CONFIRMED→DONE 写一条不可变流水」 vs Bookkeeper+Immutable 基类 → 一致；`trace-chain.md` §实现说明 L42「单 uplink 列 + 反向查询（2026-07-02 登记偏离）」 vs ORM `originMoveId`/`originReturnedMoveId` to-one → 一致（偏离已登记非漂移）
- [x] <Decision> U02 候选 finding 逐条过 §2 三态裁决（查 r1 `ck-inventory.md` §Finding 追踪 + r2 目录 + §Mission 基线快照）：复用/归并原 ID/新立 `P{n}-CK-inv-{NNN}-r3`；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt
      - 裁决（22 项全部过三态）：**复用 6**——P1-CK-inv-001/002/003/004/005（五 P1 HEAD 复核全部有效，见走查证据）+ P2-CK-inv-012（证伪+收敛 F1.1 终态复核）；**归并 15**——P2-CK-inv-006（现症复核 L132 `divide(qty, ROUND_HALF_UP)` 废弃重载仍在）/007/008/009/010/011/013/014/015/016 + P3-CK-inv-017/018/019/020/021（open 无修复提交）；**新立 1**——`P2-CK-inv-012-r3` 序列号「已售拒绝出库」状态校验与 IN_STOCK→OUT 翻转 writer 缺失（owner doc state-machine.md L67 断言漂移；inv-004 F2.11 修复注记明示「未售/在库状态翻转为独立特性 Deferred」但无独立 ID 登记，本切片按 §2 新立承接，原 ID 状态不动）；历史 22 ID 零覆写

Exit Criteria:

- [x] U03×B 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维，断言抽样 ≥2 doc × 2 断言在案
- [x] U04×B 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维，断言抽样 ≥2 doc × 2 断言在案
- [x] U02×B 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维，断言抽样 ≥2 doc × 2 断言在案
- [x] 三单元机械程式输出数字在案且与基线对账一致；三态裁决完成（复用/归并/新立逐条在案）

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（4 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 所列 architecture/design docs + e2e-runbook）
> Targets: `module-purchase/erp-pur-web/src/main/resources/_vfs`（三单匹配页）+ `module-sales/erp-sal-web/src/main/resources/_vfs`（销售链页面）+ `module-inventory/erp-inv-web/src/main/resources/_vfs`（库存移动确认页/批次序列选择）；E2E 涉三域 spec 时按 §1.2 ③
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑（三单元共用全局数字）：`npm run validate:flux`（step [1/3] 导出 0 error / 999 页；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移，successor 在案）+ flux-only grep（`component="AMIS"` 保留层 expect 0 / ORM `ext:web-renderer="flux"` 缺失 expect 空）
      - Skill: none
      - 证据（T0 实跑）：step [1/3] 导出正常（app-erp-all JUnit flux 模式）；编译报告 `Results: files=855 validated=855 errors=325 warnings=18491`（erp 面 855 与基线一致）；**机械复核 325 ERR 100% 为 `variant=primary` 族**（report JSON 逐条匹配 variant+primary，non-variant = 0）= 既有 stub 外部漂移，successor 在案不立项；`component="AMIS"` 三域 web 层 = 0；三 ORM `ext:web-renderer="flux"` 全在位（grep -L 空）
- [x] <Proof> U03 页面走查：三单匹配页（复杂手写页清单成员）+ 采购链 codegen+delta 面对照 view-and-page-strategy（REST `/r/` / M0.4 矩阵 / `x:extends` / i18n-en 承载）逐页落 verdict
      - Skill: none
      - 证据（T0）：三单匹配页 `dashboard/three-way-match.page.yaml`（flux 原生手写重写，M0.4 矩阵 row 98 登记）：数据访问全部 `@query:ErpPurDashboard__findThreeWayMatchDiffAlert` / `@query:ErpPur{Order,Receive,Invoice}__findPage` REST 约定 ✅；`i18nEn` 全字段承载 ✅；三表联查结构对齐 three-way-match.md §差异处理；**发现 minor**：L129/170/207 三处 `docStatus == "ACTIVE" ? "primary" : "default"` 样式分支消费死状态（display-only：live 单据恒 DRAFT/CANCELLED → primary 分支不可达）→ 新立 `P3-CK-pur-017-r3`；页面 remark「默认 5%」与后端 Dashboard helper 实际默认 0.05（ratio）不一致 = P2-CK-pur-007 UI 可见面（归并证据增强，不另立）；采购链 12 实体 main/picker 页 = codegen wrapper（`x:gen-extends` → view.xml）+ 保留层 view.xml 定制（`x:extends` + `@mutation:ErpPurOrder__batchApprove` 等标准动作 API），`graphql:labelProp` 36 处命中均为 view.xml 内 to-one label 元数据键读取（平台 codegen 约定，非 GraphQL 传输/断言）
- [x] <Proof> U04 页面走查：报价/订单/出库/发票销售链页面同上逐页落 verdict
      - Skill: none
      - 证据（T0）：销售链 Quotation/Order/Delivery/Invoice/Receipt/Return/Contract/PriceList/PricingRule 全 main+picker 页 codegen wrapper 面（`x:gen-extends` → view.xml，REST `@query/@mutation` 约定在 view.xml 保留层）；`sales-price-list`/`pricing-rule` 手写 wrapper 页零自有 CJK 文案（CAT-4=0 全局面背书）；12 ERR variant stub 命中 sal 面（`sales-price-list` main.page.json 等）= 既有外部漂移不立项；无 GraphQL 断言、无 AMIS、无禁改文件脏面
- [x] <Proof> U02 页面走查：库存移动确认页（复杂手写页清单成员）/批次序列选择同上逐页落 verdict；涉三域 E2E spec 时核对 PageObject 模式 + `E2E_ENGINE` flux 缺省 + 禁 GraphQL 断言；查重：全局面 325 ERR 族与 pur/sal/inv 域命中条数对账（既有外部漂移不立项）
      - Skill: none
      - 证据（T0）：`stock-take-flow/main.page.yaml`（手写流程页）`i18nEn`×15 + `@query/@mutation`×4 ✅；`ErpInvStockLedger/ref-move.page.yaml` = M0.4 row 14 登记 codegen wrapper（fixedProps=moveId 关联子表，文案承载在模型源 view.xml `i18n-en:*`）；批次/序列选择 = Batch/SerialNumber `picker.page.yaml` codegen 族；E2E 涉三域 spec 5 个（pur-return / sal-return / sal-date-range-validation / inventory-stock-move / inventory-stock-move-reverse）：非法 selector（data-slot/data-testid/.cxd-）= 0、flux 页面级 GraphQL 断言 = 0、GraphQL 驱动自定义 @BizMutation = runbook §业务动作套件 sanctioned 范式（L229「经 GraphQL 全栈可达」）非违规、`E2E_ENGINE` 缺省 flux（runbook L9 + engine.ts）spec 零覆盖；域命中对账：325 ERR 中 pur=12 / sal=12 / inv=13（余 288 为其他域，同一 stub 漂移族，successor 在案）

Exit Criteria:

- [x] U03×F / U04×F / U02×F 三格 verdict 落盘；全局面门禁数字在案对账一致
- [x] 三单元范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（4 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ P2P/O2C 链 seed + stock_move/balance/cost_layer seed
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数）
      - Skill: none
      - 证据（T0 实跑）：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS（2026-09-08 22:06）
- [x] <Proof> U03 seed 面核对：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` expect 空 + 资产清点对账 + deploy `_seed_*.sql` 同步义务逐命中查登记处表 + P2P 链（PO→Receive→Invoice→Payment）状态/数量衔接一致性抽查（§3.3 U03 S 行；M1.1 已核 posted 凭证回链双射，本切片复核 pur 侧增量面）
      - Skill: none
      - 证据（T0）：seed 零变更（porcelain 空）；资产清点 = **372 CSV + 1 SQL** 与 seed-data.md M1.5 批后登记行精确一致；三域 deploy `_seed_*.sql` 命中 = 0（无同步义务）；P2P 链抽查：PO-2026-001（960.50 含税，PAID/RECEIVED）→ PRCV-2026-001（orderId=1，850，RECEIVED）→ PINV-2026-001（totalAmountWithTax=960.50，APPROVED/PAID，posted=true）→ PAY-2026-001（960.50，WRITTEN_OFF=PAID，posted=true）——金额链 850→960.50（+110.50 税）→960.50 全程衔接，posted=true ⟺ 财务产物（M1.1 双射裁决面复核一致，不重开）
- [x] <Proof> U04 seed 面核对：零 seed 变更 + O2C 链（SO→Delivery→Invoice→Receipt）状态/数量衔接一致性抽查（§3.3 U04 S 行）
      - Skill: none
      - 证据（T0）：seed 零变更；O2C 链抽查：SO-2026-001（1130.00 含税，RECEIVED/DELIVERED）→ SDLV-2026-001（orderId=1，1130.00）→ SINV-2026-001（1130.00，RECEIVED，posted=true）→ REC-2026-001（1130.00，WRITTEN_OFF=RECEIVED，posted=true）——金额/状态衔接一致，posted 一致性同上
- [x] <Proof> U02 seed 面核对：零 seed 变更 + stock_move/balance/cost_layer（2210-1 批）域内金额自洽（balance ↔ cost_layer 聚合口径抽查）+ deploy `_seed_*.sql` 同步义务逐命中查登记处表
      - Skill: none
      - 证据（T0）：seed 零变更；2210-1 批裁决面（seed-data.md §运营域交易单据种子「域表直 seed」范式）对照抽查：balance#1（org2/mat3/wh2：100×8.50=850.00）↔ cost_layer#1（100/100×8.50=850.00，incomingMoveId=1→MV-2026-001 PURCHASE_RECEIVE）精确一致；balance#2（80×120=9600.00）↔ cost_layer#2（80/80×120=9600.00，OPENING_BALANCE 面）精确一致；ledger LDG-2026-001（+100@8.50，balance_quantity=100/balance_total_cost=850）↔ balance#1 一致；move 链引用有效（MV-2026-001→PRCV-2026-001）；deploy `_seed_*.sql` 命中 = 0

Exit Criteria:

- [x] U03×S / U04×S / U02×S 三格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [x] seed 零变更 + 同步义务核对 + 三单元自洽/衔接抽查结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（9 项 Proof，三单元各 3）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-purchase/erp-pur-service` + `module-sales/erp-sal-service` + `module-inventory/erp-inv-service`（`<SVC>` 记法同冻结清单 §1.4）
> Prereqs: Phase 1 完成

- [x] <Proof> U03 本地回归：`mvn test -pl module-purchase/erp-pur-service` 全绿零失败（数字落注记，对照 known-good-baselines + 姊妹计划增量披露）
      - Skill: none
      - 证据（T0 实跑）：**Tests run: 341, Failures: 0, Errors: 0, Skipped: 0**，BUILD SUCCESS；对照基线 pur 341 精确一致，零增量
- [x] <Proof> U03 覆盖缺口对账：`grep -rc "@BizQuery\|@BizMutation"` 公开方法清单 × `ls <SVC>/_cases/...` 逐项对账；P0 采购到付款全流程清单行覆盖核对；缺口按业务关键度定级
      - Skill: none
      - 证据（T0）：BizModel 注解动作 30（另有大面积动作经 xbiz 声明 + per-mutation Processor 承载，由 Processor 测试类覆盖）；`_cases/app/erp/pur/service/` 测试资产 45 项；**P0 行在位**：`TestErpPurProcureToPayEnd`（PO→Receive→Invoice→Payment 全链 + testReverseScenarios 红冲场景）+ TestErpPurOrderToReceiveEnd；核心族齐备（Invoice/Payment/Order Approval、Posting、Settlement、PriceVariancePosting、MultiCurrencyPosting、CrudStatusLock、BudgetControl、CommitmentRestore、FinanceReversalWriteback）；无未测试新公开动作（末笔生产提交 `bdb469d22` 状态机直抛领域码改造附同码测试）；r1 T 格 pass 基础上零新增缺口
- [x] <Proof> U03 快照纪律：`grep -rn "SnapshotTest.RECORDING" <SVC>/src/test/java` expect 0 + `delVersion`/decimal `*` 通配屏蔽合规抽查
      - Skill: none
      - 证据（T0）：RECORDING = 0；`_cases` 抽查（TestErpPurProcureToPayEnd/testReverseScenarios/output）`delVersion` 屏蔽在位，审计字段（CREATE_TIME/UPDATE_TIME）框架自动屏蔽，decimal 通配使用合规
- [x] <Proof> U04 本地回归：`mvn test -pl module-sales/erp-sal-service` 全绿零失败（数字落注记）
      - Skill: none
      - 证据（T0 实跑）：**Tests run: 316, Failures: 0, Errors: 0, Skipped: 0**，BUILD SUCCESS；对照基线 sal 316 精确一致
- [x] <Proof> U04 覆盖缺口对账：同法对账；P0 销售到收款全流程清单行覆盖核对；缺口按业务关键度定级
      - Skill: none
      - 证据（T0）：BizModel 注解动作 18（xbiz/Processor 动作由 Processor 测试类覆盖）；`_cases` 测试资产 42 项；**P0 行在位**：`TestErpSalOrderToCashEnd`（SO→Delivery→Invoice→Receipt 全链）+ TestErpSalOrderToDeliveryEnd + TestErpSalPricingEndToEnd + TestErpSalReturnRefundEndToEnd；核心族齐备（Dashboard KPI 口径、Return Exchange/Refund、Contract、Quotation）；r1 T 格 pass 基础上零新增缺口
- [x] <Proof> U04 快照纪律：RECORDING grep expect 0 + 屏蔽合规抽查
      - Skill: none
      - 证据（T0）：RECORDING = 0
- [x] <Proof> U02 本地回归：`mvn test -pl module-inventory/erp-inv-service` 全绿零失败（数字落注记）
      - Skill: none
      - 证据（T0 实跑）：**Tests run: 253, Failures: 0, Errors: 0, Skipped: 0**，BUILD SUCCESS；对照基线 inv 248 +5 = 姊妹/MI 批次披露增量（`bdb469d22` 提交注记「inv 253 green」——StateMachine 直抛领域码 + TransferOrder 码修正附测试），已知增量非漂移
- [x] <Proof> U02 覆盖缺口对账：同法对账；P0 库存移动+流水+余额一致性清单行覆盖核对；缺口按业务关键度定级
      - Skill: none
      - 证据（T0）：BizModel 注解动作 42；`_cases` 测试资产 31 项；**P0 行在位**：`TestErpInvStockMoveBookkeeping`（记账↔流水↔余额）+ `TestErpInvLedgerImmutable`（不可变流水）+ TestErpInvSnapshotAndStockCheck（快照↔余额一致性）+ 7 计价策略族测试（WA/FIFO/LIFO/BATCH/SPECIFIC/Standard + CostingDispatch）+ TestErpInvBatchExpiryInterception（inv-004 修复红→绿）+ TestErpInvTraceChain；覆盖密度高于注解面（策略族状态组合多测）；零新增缺口
- [x] <Proof> U02 快照纪律：RECORDING grep expect 0 + 屏蔽合规抽查
      - Skill: none
      - 证据（T0）：RECORDING = 0；TestErpInvResolverRawValueAfterReadPathMasking 专项屏蔽测试在位

Exit Criteria:

- [x] U03×T / U04×T / U02×T 三格 verdict 落盘；三模块本地回归全绿数字在案
- [x] 三单元覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + pur/sal/inv 白名单条目核对（5 条）
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，vs 冻结 SNAPSHOT 单向收紧）+ `--self-test` PASS；脚本红 = MI 回归，升级裁决报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据（T0 实跑）：`--strict` **RESULT: PASS（exit 0）**——0 new violations vs frozen snapshot；170 baseline files 全部单向收紧改善项（removed-from-tree），actual CAT1..4 = 0/0/0/0（report mode 复证）；`--self-test` **RESULT: PASS**（含 CAT-4 blockCarrierCovers 等扩展断言）。零 MI 回归，无升级事项
- [x] <Proof> 白名单合规核对：§WHITELIST pur 1 + sal 1 + inv 3 = 5 条全数核对四要素（文件路径/理由/owner doc 指针/裁决来源）+ `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空；四要素缺失 = 白名单登记缺陷 finding
      - Skill: none
      - 证据（T0）：**5/5 四要素齐备**——pur 1：`ErpPurDashboardBizModel.java` cats[3]（@Description 专属 1 行 E3 豁免 / i18n-compliance.md 准绳表 #5 / plan 2026-09-07-1715-1 Phase 3）；sal 1：`ErpSalDashboardBizModel.java`（同四要素）；inv 3：`IErpInvStockLedgerBiz.java` + `ErpInvDashboardBizModel.java` + `ErpInvStockLedgerBizModel.java`（@Description 专属 1-2 行，owner doc + plan 2026-09-07-1715-1 Phase 2 裁决来源齐备）；`@Locale` 缺失 grep = 空（全部 `*Errors.java` 在位声明）；meta/i18n 禁手改 porcelain = 空；零白名单登记缺陷

Exit Criteria:

- [x] U03×I / U04×I / U02×I 三格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [x] 白名单四要素核对（5/5）+ `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（4 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-purchase-r3.md` + `ck-sales-r3.md` + `ck-inventory-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（15 格 verdict 齐备）

- [x] <Decision> 三单元候选 finding 汇总复裁决：逐条确认三态与级别一致性与 ID 规范（`P{n}-CK-pur-{NNN}-r3` / `P{n}-CK-sal-{NNN}-r3` / `P{n}-CK-inv-{NNN}-r3`，历史 ID 永不覆写；升级级别按 code-history-deferred-triangulation-audit-prompt §3.1）
      - Skill: code-quality-audit-prompt
      - 复裁决（T0）：**新立 4 条 ID 规范/级别一致性逐一确认**——`P2-CK-pur-015-r3`（续 r1 pur 编码 014；P2 与同族 sal-010/pur-005 一致；镜像控制点分立裁决成立）/ `P3-CK-pur-016-r3`（注释级漂移 P3 与 qa-032-r3 同族一致）/ `P3-CK-pur-017-r3`（display-only P3 与 mfg-023-r3 页面级 P3 先例一致）/ `P2-CK-inv-012-r3`（续 r1 inv 编码 021 之 P3 后、inv P2 编码 016 之后取 012-r3 沿用原报告 P2 段位惯例；Deferred 残留承接按 §3.1 原状态不动；门控性降档 P2 与 inv-007/010 同逻辑，seed 投产升 P1 触发条件已登记）；归并 40 条（pur 11 + sal 14 + inv 15）复用 13 条（pur 3 + sal 4 + inv 6）三态与级别复核无冲突；历史 54 ID 零覆写
- [x] <Add> 落盘 `ck-purchase-r3.md`：五维覆盖矩阵（5 格 verdict 全落）+ finding 列表（含归属标注与归并指针）+ 统计 + 剩余风险声明（横切关注点 13 四件套）
      - Skill: none
      - 证据：已落盘 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-purchase-r3.md`（§1 矩阵 B=finding/F=finding(minor)/S=pass/T=pass/I=pass；§2 三态裁决 复用 3/归并 11/新立 3；§2.4 归属标注 fin-1/U20/U21/U11；§3 统计；§4 四件套）
- [x] <Add> 落盘 `ck-sales-r3.md`：同上结构
      - Skill: none
      - 证据：已落盘 `ck-sales-r3.md`（B=finding 归并态 0 新立/F/S/T/I=pass；复用 4/归并 14/新立 0；红冲前置双判消费侧收口归属标注）
- [x] <Add> 落盘 `ck-inventory-r3.md`：同上结构
      - Skill: none
      - 证据：已落盘 `ck-inventory-r3.md`（B=finding/F/S/T/I=pass；复用 6/归并 15/新立 1；Deferred 残留承接归属标注 + inv-017 联动注记）
- [x] <Add> 双索引同步：本轮 `ai-check-r3-index.md` 产物清单追加三行 + 跨轮 `docs/audits/check/ai-check-index.md` §报告清单追加 M1.13 行 + §Finding 追踪新立 `-r3` ID 行（归并的追加证据至原 ID）
      - Skill: none
      - 证据：`ai-check-r3-index.md` 产物清单 +3 行（ck-purchase-r3/ck-sales-r3/ck-inventory-r3，含矩阵摘要与 T0）；`ai-check-index.md` §报告清单 +3 行（M1.13 三报告行，P0/P1/P2/P3 新立计数列 0/0+1+1/0/2+0+0 与各报告 §3 一致）+ §Finding 追踪 +4 行（pur-015/016/017-r3 + inv-012-r3，状态 open，修复方向与联动注记在案；归并 40 条的追加证据落各报告 §2.2 及原 ID 行不重写）
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空（roadmap 规则 6）
      - Skill: none
      - 证据（收官实跑）：过滤 `module-*src/(main|test)` + `app-erp-all/src` = 空；全部脏面 = 2 索引修改 + 3 新报告 + 1 计划文件（全 docs 面），零生产代码/ORM/配置/页面/seed 改动
- [x] <Proof> 收尾回归：`mvn test -pl module-purchase/erp-pur-service` + `mvn test -pl module-sales/erp-sal-service` + `mvn test -pl module-inventory/erp-inv-service` 复跑全绿（审计只读不变式复证；全仓验证归收官机制）
      - Skill: none
      - 证据（收官实跑，顺序执行）：pur **341/0/0/0** + sal **316/0/0/0** + inv **253/0/0/0** 全绿——审计只读不变式复证通过（与 Phase 5 数字一致）

Exit Criteria:

- [x] 三份 `ck-*-r3.md` 落盘且三矩阵 15 格 verdict 完整（缺一格不算完）
- [x] 双索引行追加在案；零生产代码改动核证通过；pur/sal/inv service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-07-171530-2026-09-08-1454-3-m113-pur-sal-inv-five-dim-audit-1-e58a3c72 to opencode-review-agent
- 2026-09-08：iteration 1，共识 accept #review-2026-09-07-171530-2026-09-08-1454-3-m113-pur-sal-inv-five-dim-audit-1-e58a3c72

## Verification

- pass test 20260908-2232-closure-r1 exit=0

> 闭包 visit 记录（2026-09-08 22:32，独立闭包审计，fresh session）：`mvn test -pl module-purchase/erp-pur-service` BUILD SUCCESS（exit 0，Tests run: 341, Failures: 0, Errors: 0, Skipped: 0 = 基线锚点 341 零回归）+ `mvn test -pl module-sales/erp-sal-service` BUILD SUCCESS（exit 0，316/0/0/0 = 锚点 316 一致）+ `mvn test -pl module-inventory/erp-inv-service` BUILD SUCCESS（exit 0，253/0/0/0 = 锚点 248 + 披露增量 +5 一致）+ `node tools/check-hardcoded-cjk.mjs --strict` RESULT: PASS（exit 0，0 new violations vs 冻结快照，actual CAT1..4 = 0/0/0/0）+ `node tools/check-hardcoded-cjk.mjs --self-test` RESULT: PASS——五项均闭包 visit 本 visit 顺序实跑复证。增量口径：闭包时点工作树改动全为 docs 审计产物面（`git status` 过滤 `module-*`/`app-erp-all` 生产路径 = 空），按 mission 增量构建指引以 `-pl` 定面三 service 模块，不清 target 全量重编；完整仓库回归归 roadmap 收官机制（只读审计计划：验证命令组即结果表面本身，MV.1 对照面 = known-good-baselines 2026-09-08 MI 终态行）。以下为执行期各 Phase 红线的 PASS 记录（T0 = HEAD `e0eda4d23` 实跑，各 Phase 勾选注记含同源数字；闭包 visit 复跑覆盖 `test`/`--strict` 收官态等价）：

- PASS 2026-09-08（执行期 Phase 1/2）`bash docs/audits/nop-compliance-checker.sh` — 全规则 = M0.3 快照行逐值一致零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）
- PASS 2026-09-08（执行期 Phase 1/6）`node tools/check-hardcoded-cjk.mjs`（report mode）— CAT1..4 = 0/0/0/0（3430 java + 886 yaml）= MI 终态行精确一致（闭包 visit `--strict` 复跑同结果）
- PASS 2026-09-08（执行期 Phase 3）`npm run validate:flux` — step [1/3] 0 error / 999 页 / erp 855；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移（100% 机械复核 variant 族，successor 在案；域命中 pur 12/sal 12/inv 13 对账不立项）
- PASS 2026-09-08（执行期 Phase 4）`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` — Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS
- PASS 2026-09-08（执行期 Phase 5/7）`mvn test -pl module-purchase/erp-pur-service`（×2）+ `mvn test -pl module-sales/erp-sal-service`（×2）+ `mvn test -pl module-inventory/erp-inv-service`（×2）— 341/0/0/0 + 316/0/0/0 + 253/0/0/0 全绿（闭包 visit 已复跑同结果）
- PASS 2026-09-08（执行期 Phase 6）`node tools/check-hardcoded-cjk.mjs --self-test` — PASS（闭包 visit 已复跑同结果）
- PASS 2026-09-08（收官 Phase 7）`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 — 空（零生产代码改动，roadmap 规则 6；闭包 visit 复核：脏面全为 docs 审计产物面）

## Closure

Status Note: 本计划（只读五维审计切片，零生产代码改动）7 Phase 全部执行项与退出标准 `[x]`（52/52，机械红线数字注记在案）；15 格五维 verdict 全落盘（pur：B=finding（新立 2）/F=finding(minor)（新立 1）/S/T/I=pass；sal：B=finding 归并态 0 新立/F/S/T/I=pass；inv：B=finding（新立 1）/F/S/T/I=pass）；三态裁决 复用 13 / 归并 40 / 新立 4（历史 54 ID 零覆写）+ 三报告 + 双索引行 + roadmap M1.13 行执行完成注记 + 当日日志条目均在案。ledger 协议：frontmatter `status: active` 保持，完成态由全勾选 + `## Verification` pass 线 + 本节回执派生。

Closure Audit Evidence:

- Auditor / Agent: 独立闭包审计子代理（fresh session、read-only、非执行者上下文；mission-driver 流 CLOSURE_SCRIPT_CHECK → 闭包 visit 单一独立 closer）
- Evidence: 本计划 Phase 1~7 勾选注记（含全部红线数字）+ `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-purchase-r3.md` / `ck-sales-r3.md` / `ck-inventory-r3.md`（五维矩阵 15/15 + 三态裁决 + 统计 + 剩余风险四件套）+ 双索引追加行（本轮 `ai-check-r3-index.md` 产物 3 行 + 跨轮 `ai-check-index.md` §报告清单 M1.13 三行与 §Finding 追踪 4 条新立 `-r3` ID 行）+ `docs/backlog/ai-check-r3-roadmap.md` M1.13 行 + `docs/logs/2026/09-08.md` M1.13 条目 + 闭包 visit 实跑记录（见 `## Verification`）

- dispatch audit #audit-20260908-2232-m113-pur-sal-inv-five-dim-audit-1-0fcabd49 to 2026-09-08-193051-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-20260908-2232-m113-pur-sal-inv-five-dim-audit-1-0fcabd49：独立闭包审计 ACCEPT——52/52 勾选全绿，五维矩阵 15/15 格 verdict 落盘（pur：B=finding 新立 P2-CK-pur-015-r3 核销 docStatus 守卫 + P3-CK-pur-016-r3 javadoc 口径漂移、F=finding(minor) 新立 P3-CK-pur-017-r3 页面死状态样式分支、S/T/I=pass；sal：B=finding 归并态 0 新立/F/S/T/I=pass；inv：B=finding 新立 P2-CK-inv-012-r3 序列已售拒绝出库 Deferred 残留承接、F/S/T/I=pass）；三态裁决 复用 13/归并 40/新立 4，历史 54 ID 零覆写，双索引行与三报告 §2/§3 逐条吻合（新立计数 0|1|0|2 + 0|0|0|0 + 0|1|0|0）；闭包 visit（2026-09-08 22:32）顺序实跑 pur 341/0/0/0 + sal 316/0/0/0 + inv 253/0/0/0 三 service 回归全绿（exit 0，锚点零回归/披露增量一致）+ CJK `--strict` PASS exit 0 + `--self-test` PASS + `git status` 生产路径零触碰核证（只读审计零改动红线保持）；语义核对（退出标准对照实仓产物 / anti-hollow 三报告实存非桩 / deferred honesty 新立 4 ID 均登记 open + M2.x 修复方向 / 文档四方一致：报告↔双索引↔roadmap 行↔日志条目）全 PASS；plan-check `--strict` 结构绿，derivedCompleted 由本回执 + `pass test` 线成立（单模型降级如实声明：exec = aud = zhipuai-coding-plan/glm-5.3-flash）
