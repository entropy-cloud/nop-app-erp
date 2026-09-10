---
status: active
mission: ai-check-r3
work-item: M2.5
group: "2026-09-10-0705"
verify: [test]
---

# 2026-09-10-0705-3 M2.5 purchase+sales+inventory P1 修复批（域批独立项 P2-CK-pur-015-r3 核销 docStatus 守卫 + P2-CK-inv-012-r3 序列号出库守卫与翻转 writer）

## Current Baseline

- 批内面 = 恰 2 条 P2（三域 r3 新立 P1 = 0）：`P2-CK-pur-015-r3`（purchase）、`P2-CK-inv-012-r3`（inventory）；sales 域 r3 新立 = 0（无批内面）。M2.0 族裁决路由（§2.9 row 73/78）：两条均为域批独立项挂靠本 roadmap 行，域批消化序 P1 → P2 → P3（本批无 P1，两条 P2 按序）。
- 其余 pur/inv 域 r3 新立 finding 去向（M2.0 族裁决，均非本批）：pur-016-r3（doc 批·分片5）、pur-017-r3（E·分片4）。
- `P2-CK-pur-015-r3` 缺陷面（执行者 2026-09-10 HEAD 实核在位）：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/PaymentSettler.java` `#settle`/`#reverseSettlement`（L128 起）仅守卫 approveStatus（javadoc L32 核销约束仅列「同供应商、双方 approveStatus=APPROVED、金额不超」，全文零 docStatus/CANCELLED 引用）——已作废发票可被继续核销、已作废付款单可继续核销/反核销，AR/AP 派生态与作废语义冲突；owner doc `docs/design/purchase/state-machine.md` §异常路径 L99「付款核销时发票已作废→拒绝核销」逐字未实现（owner 断言在先 = 修复义务，非新特性）。
- `P2-CK-inv-012-r3` 缺陷面（执行者 2026-09-10 HEAD 实核在位）：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvSerialNumberBizModel.java` 全文 16 行 CRUD 桩（extends AbstractErpCrudBizModel，零状态方法）；`ErpInvStockMoveProcessor#validateBatchSerialPresence`（L134/L204）仅 presence 校验——「已售序列号拒绝再次出库」状态守卫与 IN_STOCK→OUT 翻转 writer 双缺；owner doc `docs/design/inventory/state-machine.md` §异常路径 L67 + README §关键业务规则 6 逐字未实现。谱系：P1-CK-inv-004（F2.11，fixed）修复注记明示「未售/在库状态翻转为独立特性 Deferred」但无独立 ID 登记 → r3 新立承接（原 ID fixed 状态不动）。
- 跨轮协同义务（裁决 §3）：pur-015-r3 与 r1 `P2-CK-pur-005`（cancel 侧）/`P1-CK-sal-010`（sal 核销侧）为「核销 docStatus 守卫」统一批次设计——本批落统一设计证据（守卫语义/错误码形态/测试程式），r1 两行状态不动、回填归 M2.9；Phase 1 先实核 r1 通道修复状态（若已先修则复用其范式，避免双轨设计）。
- 联动注记：inv-012-r3 与 `P3-CK-inv-017`（r1 死字典，open）联动——writer 落地使字典值复活，本批仅落联动证据注记（inv-017 归 r1 通道，状态不动）。
- 升 P1 触发（索引登记，本批显式承继）：inv 侧 = 序列管控物料 seed 投产启用；pur 侧 = 核销 docStatus 守卫统一批次（r1 sal-010 为 P1 同链）。本批不改 `_init-data/`（不投产序列管控 seed），测试用 case 级 fixture。
- 修复方法约束（r3 M2 前言 + M2.0 owner doc `docs/architecture/finding-remediation-method.md`，其为本计划直接 Prereq）：先写失败测试 → 修复 → 测试绿 + 既有测试零回归；守卫/驳回场景新增域内专用错误码（`erp.err.pur.*` / `erp.err.inv.*` 前缀约定 + 测试可断言），异常参数传码不传散文（MI.5a CAT-2 契约）；跨实体访问注入 `I*Biz`（nop-backend-dev 范式，daoFor 需豁免注释——本批新增调用点优先 I*Biz 注入避免新增 A 族站点）；本批零 ORM/api.xml/seed/页面变更（不触发保护区双批准与 seed 双面重录）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-09 行（全 reactor 基线，41 含测试模块聚合口径真值 4006/0/0/1）；compliance R2b=242/R2c=1542/R12a=71；CJK `--strict` PASS（0/0/0/0）。模块参照（M1.13 切片锚点）：pur 341 / sal 316 / inv 253 全绿（执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露）。
- 剩余差距：两条 P2 修复 + 负路径测试缺位；M2.9 收官需本批 fixed 证据指针与统一批次设计证据。
- 依赖状态：M2.0（plan `2026-09-10-0425-1`）已完成；本计划组 `2026-09-10-0705` 内执行序 N=3（组内三计划无共享文件面：fin intercompany / ast listener / pur settler + inv stock-move 链互不相交）。

## Goals

- `P2-CK-pur-015-r3` 修复落地：`PaymentSettler#settle`/`#reverseSettlement` 双侧（payment + invoice 两侧单据）补 `docStatus≠CANCELLED` 守卫，已作废发票/付款单核销与反核销被显式拒绝（域内专用错误码 + 传码参数），owner doc §异常路径 L99 语义逐字落地；合法核销路径行为不变。
- `P2-CK-inv-012-r3` 修复落地：出库链（`validateBatchSerialPresence` 及其调用面）补序列号 `IN_STOCK` 守卫（已售/非在库拒绝出库，新错误码 `erp.err.inv.serial-not-in-stock`）+ `ErpInvSerialNumber` 补 IN_STOCK→OUT 翻转 writer（出库记账同事务，经 `I*Biz` 域方法），owner doc §异常路径 L67/README 规则 6 语义逐字落地。
- 先写失败测试：两 finding 各自负路径测试（作废发票核销拒绝 / 已售序列号出库拒绝 + 在库序列号出库后状态翻转为 OUT 的正路径断言）先红后绿。
- 零回归 + 门控持平：pur/sal/inv service 套件全绿 + 全 reactor `mvn test` 零新增失败；compliance/cjk 双 checker 不高于基线；seed 零改动证明。
- M2.9 消费证据落盘：fixed 证据指针 + 核销 docStatus 守卫统一批次设计证据（跨轮 pur-005/sal-010 协同）+ inv-017 字典复活联动注记 + 零触碰声明。

## Non-Goals

- 不修 r1 面：`P2-CK-pur-005`（cancel 侧）、`P1-CK-sal-010`/`P2-CK-sal-012`（sal 核销侧）、`P3-CK-inv-017`（死字典）——仅统一设计证据与联动注记落盘（r1 通道所有，状态回填归 M2.9）；sal 侧 `ReceiptSettler` 零改动。
- 不修 pur/inv 域其他 r3 finding（pur-016/017-r3 等——去向以 M2.0 族裁决为准）。
- 不投产序列管控物料 seed（`_init-data/` 零改动；seed 投产触发升 P1 走独立裁决）；不改序列号字典/ORM（writer 复用既有 IN_STOCK/OUT 字典值，零值域变更）。
- 不改 ORM/api.xml/页面文件；不做双索引状态回填（M2.9 义务）；不做 roadmap 状态翻转。
- 不重构 PaymentSettler 结算编排与 StockMoveProcessor 记账结构（守卫以最小面接入既有校验序列）。

## Phase 1 — 失败测试先行（两 finding 负路径 + r1 修复状态实核）

> 统一类型：Proof（3 项 Proof）。
> Skill: bug-diagnosis-prompt（roadmap M2.5 行指定——同 M2.3 行；先读 `docs/skills/bug-diagnosis-prompt.md` 四阶段定位纪律再动手）
> Targets: `module-purchase/erp-pur-service/src/test/java/`（核销守卫测试，落点沿既有 `TestErpPurPayment*` 布局）+ `module-inventory/erp-inv-service/src/test/java/`（序列号守卫/翻转测试，落点沿既有出库测试布局）；测试数据用 case 级 fixture（作废发票/付款 + 序列管控物料与序列行），禁触 `_init-data/`
> Prereqs: M2.0 计划完成（修复方法 owner doc 在位）

- [x] <Proof> pur 失败测试：构造已作废（docStatus=CANCELLED）发票与/或付款单，断言 `settle`/`reverseSettlement` 被业务异常拒绝（错误码断言 + AR/AP 派生态不变）；合法核销控制组（非作废）断言现状保持。执行确认红：现零 docStatus 守卫 → 作废单核销放行（status=0），失败输出记入勾选注记（缺陷复现证据）。
      - Skill: bug-diagnosis-prompt
      - [2026-09-10] 红 4/4 复现在案：新增 `TestErpPurPaymentSettlementDocStatusGuard`（沿既有 `TestErpPurPaymentSettlement` 布局），4 负路径（作废发票 settle/作废付款 settle/作废发票 reverse/作废付款 reverse）全部 `expected: <erp.err.pur.settle-*-cancelled> but was: <null>`——作废单核销/反核销放行（status=0），缺陷复现；控制组 `testActiveDocsSettleAndReverseStillPass` 绿。错误码常量（ERR_SETTLE_INVOICE_CANCELLED/ERR_SETTLE_PAYMENT_CANCELLED，CAT-2 参数 invoiceCode/paymentCode+currentDocStatus）随测试先声明供编译面，Phase 2 接线。
- [x] <Proof> inv 失败测试：构造序列管控物料出库场景——(a) 负路径：序列号状态非 IN_STOCK（已售 OUT/缺状态）时断言出库被拒（`erp.err.inv.serial-not-in-stock` 断言）；(b) 正路径：IN_STOCK 序列号出库成功后断言状态翻转为 OUT（同事务）。执行确认红：(a) 现 presence 校验放行已售序列号；(b) 现 CRUD 桩无翻转（状态滞 IN_STOCK），失败输出记入勾选注记。
      - Skill: bug-diagnosis-prompt
      - [2026-09-10] 红 3/3 复现在案：新增 `TestErpInvSerialNumberOutboundGuard`（沿既有 `TestErpInvBatchExpiryInterception` 布局），已售 OUT 放行（`expected: <erp.err.inv.serial-not-in-stock> but was: <null>`）+ RESERVED 放行 + 出库 DONE 后状态滞 `IN_STOCK`（`expected: <OUT> but was: <IN_STOCK>`）全复现；控制组非序列管控路径绿。勘误：「缺状态=null」子场景不可落库（`serial-status` 列 DB 层 NOT NULL，首跑 OrmException mandatory-prop-is-null 实证），负路径改以字典 RESERVED（已预留）覆盖「非 IN_STOCK 拒绝」语义。错误码 `ERR_SERIAL_NOT_IN_STOCK`（参数 materialId/serialNo/currentStatus）随测试先声明供编译面，Phase 2 接线。
- [x] <Proof> r1 修复状态实核 + 统一批次设计输入：grep/索引实核 `P2-CK-pur-005` 与 `P1-CK-sal-010` 当前修复状态（r1 F2.x 批次是否已落地 cancel/settle 侧守卫）；结论决定统一设计是「复用 r1 既有范式」还是「本批首创设计供 r1 复用」，选择与理由记入注记（M2.9 跨轮协同证据的第一件）。
      - Skill: bug-diagnosis-prompt
      - [2026-09-10] 实核结论（三源 grep 在案）：`P2-CK-pur-005` ai-check-index.md L116 = open（cancel 侧处理器零 PaymentSettler#reverseSettlement 编排，ck-purchase-r3.md L37 复核原样）；`P1-CK-sal-010` 实为索引 ID `P2-CK-sal-010`（ai-check-index.md L135 = open，ck-sales-r3.md L42 追加维度⑮证据：ReceiptSettler 全文零 docStatus/CANCELLED 引用）。r1 通道均未修复 → **裁决：本批首创统一设计供 r1 复用**（守卫语义 = settle/reverseSettlement 入口 `docStatus=CANCELLED` 显式拒绝；错误码形态 = `erp.err.{short}.settle-{doc}-cancelled` 域内专用码 + CAT-2 传码传单据号/当前状态；测试程式 = case 级 fixture 直落 CANCELLED+APPROVED 组合 + 负路径错误码断言 + 派生态不变断言 + 合法路径控制组）。sal 侧复用时对齐 `ReceiptSettler` 同型接线（r1 通道所有，状态回填归 M2.9）。

Exit Criteria:

- [x] 两条负路径失败测试在位且当前树红（pur 作废核销放行 + inv 已售出库放行/状态不翻转复现），失败输出注记在案；控制组绿
- [x] r1 修复状态实核结论在案（复用/首创裁决输入齐备），缺陷链与 finding 报告对账一致

## Phase 2 — 守卫与 writer 落地（Decision + Fix + Add）

> 统一类型：Decision | Fix | Add（1 Decision + 2 Fix + 1 Add）。
> Skill: bug-diagnosis-prompt
> Targets: `PaymentSettler.java`（settle/reverseSettlement 守卫）+ `ErpPurErrors`（新码）；`ErpInvStockMoveProcessor.java`（validateBatchSerialPresence 状态守卫）+ `ErpInvSerialNumberBizModel`/`IErpInvSerialNumberBiz`（翻转 writer）+ `ErpInvErrors`（新码）；owner doc 对账面 = `docs/design/purchase/state-machine.md` §异常路径 + `docs/design/inventory/state-machine.md` §异常路径/README 规则 6
> Prereqs: Phase 1 完成（失败测试在位 + r1 状态实核结论）

- [x] <Decision> inv 翻转 writer 拓扑裁决：推荐 (a) `IErpInvSerialNumberBiz` 声明域方法（如 markOutbound，`@BizMutation` 语义经接口声明 + BizModel 实现），`ErpInvStockMoveProcessor` 经 `I*Biz` 注入在记账同事务调用（nop-backend-dev 跨实体范式：接口先行 + I*Biz 注入 + 无 daoFor 新站点）。替代方案：(b) Bookkeeper/Processor 内联 `daoFor(ErpInvSerialNumber.class)` 直改——被否，新增 A 族站点（daoFor 豁免注释族 M2.8 分片2 正在收敛，禁止新增未豁免直查）；(c) 状态翻转下沉 ORM 钩子——被否，隐式副作用违反显式编排原则。残余风险：同事务边界依赖调用方事务（@BizMutation 包装），测试断言同事务翻转可见性。裁决三要素记入注记。
      - Skill: bug-diagnosis-prompt
      - [2026-09-10] 裁决三要素在案（方案 a 落地）：①**写路径载体** = `IErpInvSerialNumberBiz#markOutbound`（`@BizMutation` 接口声明 + `ErpInvSerialNumberBizModel` 实现，对齐 `IErpInvReservationBiz` 既有范式）；②**调用拓扑** = `ErpInvStockMoveProcessor` `@Inject IErpInvSerialNumberBiz` 在 `doComplete` 的 `bookCompletion` 之后同事务调用（`markSerialNumbersOutbound` protected step，仅 OUTGOING 触发——内部转移后序列号仍属在库不翻转；bookkeeping 失败则翻转同回滚）；③**否决面** = (b) daoFor 直改新增 A 族站点（否）、(c) ORM 钩子隐式副作用（否）。残余风险处置：同事务可见性由 `testInStockSerialOutboundFlipsToOut` 断言出库 DONE 后重查台账 status=OUT + outBillCode 回链实证（绿）。
      - [2026-09-10] 执行勘误：I*Biz 注入 bean 为 `BizProxyFactoryBean` 代理（方法经 biz 注册表分发），接口新增方法后须先 `mvn clean install -DskipTests` 刷新模块产物再跑测试——首跑代理抛 method-not-found 即陈旧产物实证，install 后 4/4 绿。
- [x] <Fix> pur 核销守卫：`#settle`/`#reverseSettlement` 双侧在既有 approveStatus 校验后补 payment 与 invoice 两侧 `docStatus≠CANCELLED` 守卫（沿 Phase 1 实核的统一批次设计语义；reverseSettlement 反核销同样拒绝作废单）；行为不变式：合法核销路径逐字节现状保持。
      - Skill: bug-diagnosis-prompt
      - [2026-09-10] 站点：`PaymentSettler#settle`（payment 侧 `assertNotCancelled`，置于既有 approveStatus 守卫后）+ `requireInvoiceForSettle`（invoice 侧 `assertInvoiceNotCancelled`，置于既有 approveStatus 守卫后）+ `#reverseSettlement` 入口双侧守卫（payment 直查 + invoice 按 invoiceId 加载，置于幂等 no-op 判定之前——作废单反核销显式拒绝）。合法路径控制组 + 既有 `TestErpPurPaymentSettlement`/`TestErpPurProcureToPayEnd` 全绿 = 行为不变式成立。
- [x] <Fix> inv 出库守卫与翻转：`validateBatchSerialPresence` 扩状态校验（非 IN_STOCK 拒绝）+ 按 Decision 方案落地 IN_STOCK→OUT 翻转 writer（出库记账同事务）；序列管控关闭（无序列行）路径行为不变（presence 校验既有短路语义保持）。
      - Skill: bug-diagnosis-prompt
      - [2026-09-10] 站点：`validateBatchSerialPresence` 序列管控分支扩 `assertSerialInStock`（已登记台账行非 IN_STOCK 抛 `ERR_SERIAL_NOT_IN_STOCK`；台账无记录跳过——presence 短路语义保持，查经 `serialNumberBiz.findList` 与 `findBatch` 同型无 daoFor 新站点）+ `doComplete` 后置 `markSerialNumbersOutbound` writer。控制组（非序列管控物料出库）绿 = 管控关闭路径行为不变。
- [x] <Add> 错误码：`ErpPurErrors` 增核销作废单拒绝码、`ErpInvErrors` 增 `erp.err.inv.serial-not-in-stock`（中文描述合规——ErrorCode.define 中文为源语言；`@Locale("zh-CN")` 接口注解已在位[MI.1]；参数传单据号/序列号/当前状态码本身——CAT-2 传码契约）；测试断言经 Errors 常量传码。
      - Skill: bug-diagnosis-prompt
      - [2026-09-10] 在案：`ErpPurErrors.ERR_SETTLE_INVOICE_CANCELLED`（`erp.err.pur.settle-invoice-cancelled`，参数 invoiceCode+currentDocStatus）/ `ERR_SETTLE_PAYMENT_CANCELLED`（`erp.err.pur.settle-payment-cancelled`，参数 paymentCode+currentDocStatus）/ `ErpInvErrors.ERR_SERIAL_NOT_IN_STOCK`（`erp.err.inv.serial-not-in-stock`，参数 materialId+serialNo+currentStatus）——中文源描述 + `@Locale("zh-CN")` 接口注解既有，参数全为码/号无散文。测试断言均经 `ErpPurErrors`/`ErpInvErrors` 常量 `.getErrorCode()` 传码。时序注记：三常量于 Phase 1 随失败测试先行声明（编译面），本项完成参数契约核验与守卫接线。

Exit Criteria:

- [x] Phase 1 两条负路径测试全绿（拒绝 + 错误码断言）+ inv 正路径翻转断言绿 + 合法路径控制组与既有 pur/inv 套件保持绿
- [x] 守卫落在既有校验序列内、零 daoFor 新增站点；Decision 三要素注记在案；错误码与传参符合 CAT-2 传码契约

## Phase 3 — 批级证明与收官门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 2 完成

- [x] <Proof> 域级回归：`mvn test -pl module-purchase/erp-pur-service,module-inventory/erp-inv-service,module-sales/erp-sal-service -am` 全绿零新增失败（参照 pur 341 / sal 316 / inv 253，数字记入注记；执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露）。
      - Skill: none
      - [2026-09-10] 实测：inv **257**/0/0/0（基线 253 + 本批新增 4）+ pur **346**/0/0/0（基线 341 + 本批新增 5）+ sal **316**/0/0/0（持平）BUILD SUCCESS——零新增失败，增量恰为本批两测试类。
- [x] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 2026-09-09 基线行，41 含测试模块聚合口径真值 4006/0/0/1）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则持平（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空（`git ls-files` 同路径非空集，证明构成）+ `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: none
      - [2026-09-10] 实测五件：①全 reactor `mvn test` **4023/0/0/1 BUILD SUCCESS**（41 测试模块 Results 求和口径；基线 4006 → 4023 = 本批 +9（pur 5/inv 4）+ 姊妹计划 `2026-09-10-0705` 组内 fin intercompany/ast listener +8，允许并披露；1 skipped 与基线口径一致）；②compliance checker **exit 0**，R2b=242 持平 + R12a=71 持平 + **R2c=1543（基线 1542，漂移 +1）**——per-site 证据：唯一新增站点 `PaymentSettler#reverseSettlement` invoice docStatus 守卫加载（`daoProvider.daoFor(ErpPurInvoice.class).getEntityById`，intra-module pur 域，P2-CK-pur-015-r3 修复义务内在站点——「已作废发票拒绝反核销」必须加载 invoice 校验 docStatus，同文件既有聚合写范式第 3 处同型调用；全树 `git diff` 实证无第二处 daoFor 新增）；按 project-context 已知失败模式既许路径于 Closure Gates 登记基线漂移已知。successor: baseline-raise-adjudication trigger:R2c 1542→1543 +1（PaymentSettler.java reverseSettlement invoice 守卫加载站点，per-site 证据见本注记，归 M2.9/基线 owner 裁决上调）；③CJK `--strict` **PASS exit 0**（0 new violations vs 冻结快照）；④seed 零改动证明：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 输出空 + `git ls-files` 同路径 373 文件非空集；⑤`mvn clean install -DskipTests` **BUILD SUCCESS exit 0**（全部代码终态后运行，156 reactor 模块）。
- [x] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：P2-CK-pur-015-r3 / P2-CK-inv-012-r3 fixed 证据指针（测试类/方法 + 守卫与 writer 站点）；核销 docStatus 守卫统一批次设计证据（Phase 1 裁决 + 守卫语义/错误码形态/测试程式摘要，供 r1 pur-005/sal-010 通道与 M2.9 同型状态继承消费）；P3-CK-inv-017 字典复活联动注记（writer 落地后 IN_STOCK/OUT 值域语义证据，r1 状态不动）；seed/ORM/api.xml 零触碰声明（`git status --porcelain` 全树无 `module-*/model/`、`*.api.xml`、`_init-data/`、页面命中）。
      - Skill: none
      - [2026-09-10] 四件齐备：
        **① fixed 证据指针**——P2-CK-pur-015-r3：测试 `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurPaymentSettlementDocStatusGuard.java`（5 方法：4 负路径 + 1 控制组，红→绿）；守卫站点 `PaymentSettler.java` `#settle`（assertNotCancelled）/`#requireInvoiceForSettle`（assertInvoiceNotCancelled）/`#reverseSettlement`（双侧守卫）；错误码 `ErpPurErrors.ERR_SETTLE_INVOICE_CANCELLED`/`ERR_SETTLE_PAYMENT_CANCELLED`。P2-CK-inv-012-r3：测试 `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvSerialNumberOutboundGuard.java`（4 方法：已售拒绝/RESERVED 拒绝/翻转+回链正路径/非序列管控控制组，红→绿）；守卫站点 `ErpInvStockMoveProcessor#assertSerialInStock`（validateBatchSerialPresence 内）；writer 站点 `IErpInvSerialNumberBiz#markOutbound`（@BizMutation）+ `ErpInvSerialNumberBizModel` 实现 + `ErpInvStockMoveProcessor#markSerialNumbersOutbound`（doComplete 记账同事务）；错误码 `ErpInvErrors.ERR_SERIAL_NOT_IN_STOCK`。
        **② 统一批次设计证据**（供 r1 P2-CK-pur-005 cancel 侧 / P2-CK-sal-010 ReceiptSettler 通道同型继承）：裁决 = 本批首创（Phase 1 实核 r1 两行 open）；守卫语义 = settle/reverseSettlement 入口在 approveStatus 守卫后补 `docStatus=CANCELLED` 显式拒绝（payment 与 invoice 两侧，反核销同守）；错误码形态 = `erp.err.{short}.settle-{doc}-cancelled` 域内专用码 + CAT-2 传码（单据号+currentDocStatus）；测试程式 = case 级 fixture 直落 CANCELLED+APPROVED 组合 + 负路径错误码断言 + AR/AP 派生态不变断言 + 合法路径控制组。r1 通道所有行的状态回填归 M2.9，本批零触碰。
        **③ P3-CK-inv-017 字典复活联动注记**：`serial-status.dict.yaml` OUT 值自本批起有出库链 writer（`markOutbound` IN_STOCK→OUT）+ 确认期/完成期双点守卫消费（`assertSerialInStock`/`markOutbound` 非在库拒绝），RESERVED/BLOCKED 仍为死状态面（OUT 半复活：值域语义由「已售拒绝再出库 + 出库回链」实证）；batch-status/reservation EXPIRED 零变化。r1 P3-CK-inv-017 状态不动，同批收口归 M2.9。
        **④ 零触碰声明**：`git status --porcelain` 全树命中仅 plan 1 + 生产 Java 6（2 错误码接口 + 1 IBiz 接口 + 1 BizModel + 1 Processor + 1 Settler）+ 新增测试 2 类 + `_cases` 快照 2 目录；`module-*/model/*.orm.xml`、`*.api.xml`、`_init-data/`、`view.xml`/`page.yaml`/`_gen/` 零命中（机械过滤 exit 1 实证）。

Exit Criteria:

- [x] pur/sal/inv 三模块全绿 + 全 reactor 零新增失败 + 双 checker 持平基线 + seed 零改动证明在案
      - [2026-09-10] 勘误注记：R2c 1542→1543（+1，本批修复义务内在站点）非持平——已按 project-context 既许路径在上一条 <Proof> 注记内以 per-site 证据登记「基线漂移已知」+ successor: baseline-raise-adjudication trigger 触发；checker exit 0（CI 绿）不受影响。余项全数持平/全绿在案。
- [x] M2.9 消费证据四件（fixed 指针×2 / 统一批次设计证据 / inv-017 联动注记 + 零触碰声明）落盘于计划注记

## Draft Review Record

- dispatch review #review-2026-09-09-210030-2026-09-10-0705-3-m25-pur-inv-settlement-serial-fix-1-41276d77 to opencode-m25-draft-review
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-2026-09-10-0705-3-m25-pur-inv-settlement-serial-fix-1-41276d77

## Closure Gates

> 仅在所有 Phase 项目与各阶段 Exit Criteria 全部勾选 `[x]` 后关闭。完整仓库验证在此处运行一次，不在阶段退出标准重复（见执行时规则 7）。

- 范围内行为完成（P2-CK-pur-015-r3 双侧 docStatus 守卫 + P2-CK-inv-012-r3 IN_STOCK 守卫与翻转 writer + 负路径测试先红后绿）— [2026-09-10] 实证：pur 4 负路径红→绿 + inv 3 负/正路径红→绿（Phase 1 失败输出与 Phase 2 绿色输出注记在案）；守卫与 writer 站点见 Phase 3 ①。
- 相关文档对齐（owner doc §异常路径语义落地对账一致；seed/ORM/api.xml/页面零触碰声明成立）— [2026-09-10] `docs/design/purchase/state-machine.md` §异常路径 L99「付款核销时发票已作废→拒绝核销」= `assertInvoiceNotCancelled` 行为一致；`docs/design/inventory/state-machine.md` §异常路径 L67「已售序列号拒绝再次出库」+ README 规则 6「序列号未售」= `assertSerialInStock`/`markOutbound` 行为一致（确认+完成双点守卫，OUT 翻转+回链）；owner doc 文本零改动（语义本就由其拥有，本批落地实现对账）。零触碰声明见 Phase 3 ④。
- 已运行验证（指定哪些命令）：pur/sal/inv 三模块 `mvn test -pl module-purchase/erp-pur-service,module-inventory/erp-inv-service,module-sales/erp-sal-service -am` 全绿 + 全 reactor `mvn test` 零新增失败（对照 `docs/testing/known-good-baselines.md` 最新行）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则持平 + `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain` seed/ORM/api.xml/页面零命中 + `mvn clean install -DskipTests` BUILD SUCCESS — [2026-09-10] 全数实跑在案（Phase 3 注记 ①②③④⑤）。唯一偏差：R2c 1542→1543 +1（非持平）——per-site 证据 + successor: baseline-raise-adjudication trigger:R2c +1（PaymentSettler reverseSettlement invoice 守卫加载站点）已登记，checker exit 0 不阻断。
- 无范围内项目降级为 deferred/follow-up — [2026-09-10] 两 finding 修复面全数落地；唯一 external 面（R2c 基线上调裁决）按 mission 规则登记 successor 触发，非范围内项降级。
- 独立草案审查已完成并记录 — [2026-09-10] Draft Review Record：#review-2026-09-09-210030-…-1-41276d77 iteration 1 共识 approved 在案。
- 文本一致性已验证：状态、阶段、门控和日志都一致 — [2026-09-10] frontmatter `status: active` 保持（ledger 派生完成，不写 completed）；三 Phase 项目 + Exit Criteria 全 [x]；日志 `docs/logs/2026/09-10.md` 同步落盘。
- 结束审计由独立子代理（新会话）执行；执行者未自我审计 — [2026-09-10] 执行者未自我审计；本项由独立结束审计步骤（CLOSURE_AUDIT，新会话）在本日执行并回填 `## Verification` 通过行与 `## Closure` 接收回执（见下）。
- 结束证据存在于文件中 — [2026-09-10] 全部证据在本计划勾选注记 + `docs/logs/2026/09-10.md` + 测试类/快照 `_cases` 产物，无聊天独占证据。

## Verification

- pass test 2026-09-10-1130 exit=0

## Closure

Status Note: 独立结束审计（新会话 CLOSURE_AUDIT，非执行者自审）通过——两 finding 修复体实核在位（pur `PaymentSettler#settle`/`#requireInvoiceForSettle`/`#reverseSettlement` 三站点 docStatus 守卫 + inv `validateBatchSerialPresence#assertSerialInStock` 状态守卫 + `IErpInvSerialNumberBiz#markOutbound`/`ErpInvSerialNumberBizModel` IN_STOCK→OUT 翻转 writer 经 `doComplete` 记账同事务接线；三新错误码 CAT-2 传码）；全部 Phase 项目与 Exit Criteria 勾选与实况一致，无 deferred 内藏缺陷；ledger 证据齐备可推导 completed。

Closure Audit Evidence:

- Auditor / Agent: 独立闭包审计子代理（CLOSURE_AUDIT 新会话，opencode/glm-5.3-flash）
- Evidence: `## Verification` 通过行（本访 2026-09-10-1130 全 reactor `mvn test` BUILD SUCCESS + `mvn clean install -DskipTests` exit=0）+ 本计划勾选注记 + `docs/logs/2026/09-10.md` 同日条目

- dispatch audit #audit-2026-09-10-1130-2026-09-10-0705-3-m25-pur-inv-settlement-serial-fix-1-cbd801ab to opencode-closure-auditor-session-2026-09-10-1130 models={exec:opencode/glm-5.3-flash,aud:opencode/glm-5.3-flash}
- accepted #audit-2026-09-10-1130-2026-09-10-0705-3-m25-pur-inv-settlement-serial-fix-1-cbd801ab：独立闭包审计通过——修复体实核在位（pur 三站点 docStatus 守卫 + inv `assertSerialInStock`/`markOutbound` 守卫与翻转 writer，三新码 `erp.err.pur.settle-invoice-cancelled`/`erp.err.pur.settle-payment-cancelled`/`erp.err.inv.serial-not-in-stock` CAT-2 传码，两新测试类 9 方法在案），本访验证全绿：`mvn clean install -DskipTests` exit=0 + 全 reactor `mvn test` BUILD SUCCESS（0 failures/0 errors/1 skipped = 基线预存 skip；两新测试类 inv 4/4 + pur 5/5 本访实跑绿；surefire 汇总 4008/0/0/1，与计划所记 4023 口径差 15 系 M1.17 已裁计数口径差，同 M2.4 闭包审计先例，失败画像逐值一致零新增失败）+ Ledger 结构 `plan-check.mjs --strict` 结构项修复后复核；owner doc 语义对账一致（purchase §异常路径 L99 / inventory §异常路径 L67 + README 规则 6），无 deferred 内藏缺陷，ledger 证据齐备可推导 completed。
