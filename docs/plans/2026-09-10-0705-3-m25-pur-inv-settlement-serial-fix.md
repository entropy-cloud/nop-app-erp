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

- [ ] <Proof> pur 失败测试：构造已作废（docStatus=CANCELLED）发票与/或付款单，断言 `settle`/`reverseSettlement` 被业务异常拒绝（错误码断言 + AR/AP 派生态不变）；合法核销控制组（非作废）断言现状保持。执行确认红：现零 docStatus 守卫 → 作废单核销放行（status=0），失败输出记入勾选注记（缺陷复现证据）。
      - Skill: bug-diagnosis-prompt
- [ ] <Proof> inv 失败测试：构造序列管控物料出库场景——(a) 负路径：序列号状态非 IN_STOCK（已售 OUT/缺状态）时断言出库被拒（`erp.err.inv.serial-not-in-stock` 断言）；(b) 正路径：IN_STOCK 序列号出库成功后断言状态翻转为 OUT（同事务）。执行确认红：(a) 现 presence 校验放行已售序列号；(b) 现 CRUD 桩无翻转（状态滞 IN_STOCK），失败输出记入勾选注记。
      - Skill: bug-diagnosis-prompt
- [ ] <Proof> r1 修复状态实核 + 统一批次设计输入：grep/索引实核 `P2-CK-pur-005` 与 `P1-CK-sal-010` 当前修复状态（r1 F2.x 批次是否已落地 cancel/settle 侧守卫）；结论决定统一设计是「复用 r1 既有范式」还是「本批首创设计供 r1 复用」，选择与理由记入注记（M2.9 跨轮协同证据的第一件）。
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [ ] 两条负路径失败测试在位且当前树红（pur 作废核销放行 + inv 已售出库放行/状态不翻转复现），失败输出注记在案；控制组绿
- [ ] r1 修复状态实核结论在案（复用/首创裁决输入齐备），缺陷链与 finding 报告对账一致

## Phase 2 — 守卫与 writer 落地（Decision + Fix + Add）

> 统一类型：Decision | Fix | Add（1 Decision + 2 Fix + 1 Add）。
> Skill: bug-diagnosis-prompt
> Targets: `PaymentSettler.java`（settle/reverseSettlement 守卫）+ `ErpPurErrors`（新码）；`ErpInvStockMoveProcessor.java`（validateBatchSerialPresence 状态守卫）+ `ErpInvSerialNumberBizModel`/`IErpInvSerialNumberBiz`（翻转 writer）+ `ErpInvErrors`（新码）；owner doc 对账面 = `docs/design/purchase/state-machine.md` §异常路径 + `docs/design/inventory/state-machine.md` §异常路径/README 规则 6
> Prereqs: Phase 1 完成（失败测试在位 + r1 状态实核结论）

- [ ] <Decision> inv 翻转 writer 拓扑裁决：推荐 (a) `IErpInvSerialNumberBiz` 声明域方法（如 markOutbound，`@BizMutation` 语义经接口声明 + BizModel 实现），`ErpInvStockMoveProcessor` 经 `I*Biz` 注入在记账同事务调用（nop-backend-dev 跨实体范式：接口先行 + I*Biz 注入 + 无 daoFor 新站点）。替代方案：(b) Bookkeeper/Processor 内联 `daoFor(ErpInvSerialNumber.class)` 直改——被否，新增 A 族站点（daoFor 豁免注释族 M2.8 分片2 正在收敛，禁止新增未豁免直查）；(c) 状态翻转下沉 ORM 钩子——被否，隐式副作用违反显式编排原则。残余风险：同事务边界依赖调用方事务（@BizMutation 包装），测试断言同事务翻转可见性。裁决三要素记入注记。
      - Skill: bug-diagnosis-prompt
- [ ] <Fix> pur 核销守卫：`#settle`/`#reverseSettlement` 双侧在既有 approveStatus 校验后补 payment 与 invoice 两侧 `docStatus≠CANCELLED` 守卫（沿 Phase 1 实核的统一批次设计语义；reverseSettlement 反核销同样拒绝作废单）；行为不变式：合法核销路径逐字节现状保持。
      - Skill: bug-diagnosis-prompt
- [ ] <Fix> inv 出库守卫与翻转：`validateBatchSerialPresence` 扩状态校验（非 IN_STOCK 拒绝）+ 按 Decision 方案落地 IN_STOCK→OUT 翻转 writer（出库记账同事务）；序列管控关闭（无序列行）路径行为不变（presence 校验既有短路语义保持）。
      - Skill: bug-diagnosis-prompt
- [ ] <Add> 错误码：`ErpPurErrors` 增核销作废单拒绝码、`ErpInvErrors` 增 `erp.err.inv.serial-not-in-stock`（中文描述合规——ErrorCode.define 中文为源语言；`@Locale("zh-CN")` 接口注解已在位[MI.1]；参数传单据号/序列号/当前状态码本身——CAT-2 传码契约）；测试断言经 Errors 常量传码。
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [ ] Phase 1 两条负路径测试全绿（拒绝 + 错误码断言）+ inv 正路径翻转断言绿 + 合法路径控制组与既有 pur/inv 套件保持绿
- [ ] 守卫落在既有校验序列内、零 daoFor 新增站点；Decision 三要素注记在案；错误码与传参符合 CAT-2 传码契约

## Phase 3 — 批级证明与收官门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 2 完成

- [ ] <Proof> 域级回归：`mvn test -pl module-purchase/erp-pur-service,module-inventory/erp-inv-service,module-sales/erp-sal-service -am` 全绿零新增失败（参照 pur 341 / sal 316 / inv 253，数字记入注记；执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露）。
      - Skill: none
- [ ] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 2026-09-09 基线行，41 含测试模块聚合口径真值 4006/0/0/1）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则持平（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空（`git ls-files` 同路径非空集，证明构成）+ `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: none
- [ ] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：P2-CK-pur-015-r3 / P2-CK-inv-012-r3 fixed 证据指针（测试类/方法 + 守卫与 writer 站点）；核销 docStatus 守卫统一批次设计证据（Phase 1 裁决 + 守卫语义/错误码形态/测试程式摘要，供 r1 pur-005/sal-010 通道与 M2.9 同型状态继承消费）；P3-CK-inv-017 字典复活联动注记（writer 落地后 IN_STOCK/OUT 值域语义证据，r1 状态不动）；seed/ORM/api.xml 零触碰声明（`git status --porcelain` 全树无 `module-*/model/`、`*.api.xml`、`_init-data/`、页面命中）。
      - Skill: none

Exit Criteria:

- [ ] pur/sal/inv 三模块全绿 + 全 reactor 零新增失败 + 双 checker 持平基线 + seed 零改动证明在案
- [ ] M2.9 消费证据四件（fixed 指针×2 / 统一批次设计证据 / inv-017 联动注记 + 零触碰声明）落盘于计划注记

## Draft Review Record

- dispatch review #review-2026-09-09-210030-2026-09-10-0705-3-m25-pur-inv-settlement-serial-fix-1-41276d77 to opencode-m25-draft-review
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-2026-09-10-0705-3-m25-pur-inv-settlement-serial-fix-1-41276d77

## Closure Gates

> 仅在所有 Phase 项目与各阶段 Exit Criteria 全部勾选 `[x]` 后关闭。完整仓库验证在此处运行一次，不在阶段退出标准重复（见执行时规则 7）。

- [ ] 范围内行为完成（P2-CK-pur-015-r3 双侧 docStatus 守卫 + P2-CK-inv-012-r3 IN_STOCK 守卫与翻转 writer + 负路径测试先红后绿）
- [ ] 相关文档对齐（owner doc §异常路径语义落地对账一致；seed/ORM/api.xml/页面零触碰声明成立）
- [ ] 已运行验证（指定哪些命令）：pur/sal/inv 三模块 `mvn test -pl module-purchase/erp-pur-service,module-inventory/erp-inv-service,module-sales/erp-sal-service -am` 全绿 + 全 reactor `mvn test` 零新增失败（对照 `docs/testing/known-good-baselines.md` 最新行）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则持平 + `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain` seed/ORM/api.xml/页面零命中 + `mvn clean install -DskipTests` BUILD SUCCESS
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Closure

Status Note: <结束时填写：为什么计划可以关闭>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理>
- Evidence: <task id / 日志链接 / 走查记录>
