# 2026-07-20-2059-3-f4p2-finance-voucher-child-table-editor F4 Phase 2 — ErpFinVoucher 凭证子表行内编辑 successor

> Plan Status: active
> Last Reviewed: 2026-07-20
> Source: `docs/backlog/frontend-ui-roadmap.md` §F4 Phase 2 P1（finance ErpFinVoucher 行：独立 successor，依赖 F7 + F9 + F10 落地后启动——三前置均已完成）
> Related: `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md`（P0 8 对范式）；`docs/plans/2026-07-20-0629-1-f4p2-child-table-editor-p1-inventory.md`（P1 inventory 退化变体）；`docs/plans/2026-07-20-1020-3-f4p2-child-table-editor-p2-mfg-assets-projects.md`（P2 减法变体 + 本 successor 的原始 deferral §209-213）；`docs/plans/2026-07-20-1020-2-f7-non-status-visibleon-and-master-data-interactions.md`（F7 dcDirection 表达式库预冻结）；`docs/plans/2026-07-20-0629-3-f9-cross-document-navigation.md`（F9）；`docs/plans/2026-07-20-1020-1-f10-tree-entity-views.md`（F10 ErpMdSubject tree picker）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-20，独立子代理 ses_08069b96dffe 三层 ORM + xmeta + API + view.xml + 后端 BizModel 完整核对）：

- **前置 successor 触发条件已满足**：F7（plan `2026-07-20-1020-2` completed）/ F9（plan `2026-07-20-0629-3` completed）/ F10（plan `2026-07-20-1020-1` completed）三计划均 `completed`。roadmap `docs/backlog/frontend-ui-roadmap.md:134` 仍标 `⏳ 待启动`。
- **ErpFinVoucher 当前 view.xml**（`module-finance/erp-fin-web/.../pages/ErpFinVoucher/ErpFinVoucher.view.xml`，120 行）：
  - **无 `<grid name="main">`、无 sub-grid-edit / sub-grid-view**；仅有 `<grid id="list">` + 空 `<grid id="pick-list"/>`
  - `<form id="view">` 3 组（baseInfo / posting / audit）；`<form id="edit">` 仅 baseInfo 组
  - **头实体 `totalDebit`(propId=10) / `totalCredit`(propId=11) 字段在 ORM 存在但 view/edit/add 三表单均未引用**（明显的余额合计绑定缺口）
  - ErpFinVoucherLine 未作为子表被任何 cell 引用；无 add-line / template-import / auto-balance 按钮
  - 既有按钮：`row-post-button`（过账，`visibleOn: docStatus == 'DRAFT'`）+ `row-reverse-button`（红冲）已接线
- **ErpFinVoucherLine 实体字段**（`module-finance/model/app-erp-finance.orm.xml:361-441`，29 列）：
  - 核心行字段：`lineNo` / `subjectId`(科目) / `subjectCode` / `subjectName` / `dcDirection`(dict=`erp-fin/dc-direction` 仅 DEBIT/CREDIT) / `debitAmount` / `creditAmount` / `currencyId` / `exchangeRate` / `amountSource`(源币) / `amountFunctional`(本位币) / `acctSchemaId` / `orgId` / `memo`
  - **辅助核算维度（ORM 核实为 6 个 FK + 1 dict = 7 个，非计划前身的 "9"）**：`partnerId` / `departmentId` / `projectId` / `warehouseId` / `materialId` / `costCenterId` + `businessType`(dict)
  - 维度开关在 `ErpMdSubject` 主数据：`isAuxiliaryPartner` / `isAuxiliaryDepartment` / `isAuxiliaryProject` / `isAuxiliaryWarehouse` / `isAuxiliaryProduct` / `isAuxiliaryCostCenter`（6 flag，注 `isAuxiliaryProduct` 非 material）
  - 11 个 to-one 关系（voucher/subject/acctSchema/currency/partner/department/warehouse/org/material/project/costCenter）均就绪可派生 picker
- **ErpFinVoucherLine view.xml**（96 行）：`<form id="view">` + `<form id="edit">` **已分组**（1500-1/1818-2 覆盖，4 组 baseInfo/amount/auxiliary/audit）；**无 sub-grid-edit / sub-grid-view**——子表编辑 grid 完全 greenfield
- **finance ui-patterns §1 要求**（`docs/design/finance/ui-patterns.md:29-67`）：6 列子表布局（摘要/科目/辅助核算/借方/贷方/币种）+ 行增删按钮 + `[快速模板]` + `[借贷自动平衡]` toolbar + 合计栏实时计算不平衡显示 ❌ 红色警告且过账按钮禁用；科目选择使用树形弹窗；辅助核算列默认隐藏经配置显式；自动平衡按钮将差额填入最后一行；快速模板弹窗选择凭证模板自动填充分录行
- **visible-on-patterns.md 预冻结表达式**（§3 lines 51-52 + §8 lines 361-409）：
  - `debitAmount` cell `visibleOn="${dcDirection == 'DEBIT'}"` `clearValueOnHidden="true"`
  - `creditAmount` cell `visibleOn="${dcDirection == 'CREDIT'}"` `clearValueOnHidden="true"`
  - §8.3 裁决：**禁止**额外 onEvent.change setValue 清空对侧金额——clearValueOnHidden 已自动处理
  - **仅冻结 dcDirection 驱动**；subject 驱动的辅助维度 visibleOn（如 `partnerId` cell 仅当 `subject.isAuxiliaryPartner==1` 显示）**未冻结任何位置**，须在本计划新设计
- **后端就绪**（无需改后端）：
  - `ErpFinVoucherBizModel extends CrudBizModel<ErpFinVoucher>` —— 平台 `__save` mutation 已支持头+行聚合根原子保存（三层证据：xmeta `_ErpFinVoucher.xmeta:130` `<prop name="lines" tagSet="pub,cascade-delete,insertable,updatable">` + `ErpFinVoucherInputBean._lines: List<ErpFinVoucherLineInputBean>` + ORM `<to-many name="lines">`）
  - `ErpMdSubject.view.xml` F10 tree picker 就绪（`pick-list` grid 含 `isLeaf=1` filter + `pick-query` + `<picker filterForm="pick-query">` loadDataOnce + `filter_parentId=__null&filter_isLeaf=1`）——凭证行科目选择可直接消费
- **关键风险/缺口（须 Explore 验证）**：
  - **DRAFT 凭证 `__save` 不做服务端借贷平衡校验**——`assertBalanced` 仅在过账引擎 `ErpFinPostingProcessor:646` 路径触发；直接 `postVoucher` 将 DRAFT→POSTED **不跑平衡校验**。本计划的余额 UX 是纯客户端约束，无服务端兜底（接受为 Non-Goal 或 flag 为后端 gap）
  - **AMIS `input-table` 列 cell 的 `clearValueOnHidden` + `visibleOn` 语义未在前任一计划验证**（F7 冻结库面向 form cell，非 input-table row cell）——须 Phase 0 Explore 实测
  - **subject 驱动辅助维度 visibleOn 是跨实体表达式**——须将所选科目的 6 个 `isAuxiliary*` flag 快照进行数据 scope（科目 picker onEvent.setValue 批量快照），行 cell `visibleOn="${isAuxiliaryPartner}"` 引用快照字段——该模式未在前任一计划落地，须 Phase 0 Explore 实测
- **复杂度评估**：本项为 F4 Phase 2 最高复杂度（4 个全新模式：dcDirection 行内切换 / 科目树 picker 9 字段快照 / subject 驱动维度 visibleOn / 凭证模板快速导入；1 个新自动推算变体：amountFunctional = amountSource × exchangeRate；1 个客户端单边约束 gap：借贷平衡；列数纪录 17 列）

## Goals

1. **Phase 0 Explore 闭环**：在真实运行时验证 3 个未验证模式——(a) AMIS input-table 列 cell clearValueOnHidden + visibleOn（dcDirection 切换）；(b) 科目 picker onEvent.setValue 多字段快照进行 scope；(c) 行 cell visibleOn 引用快照字段（跨实体 subject 驱动）。Explore 不通过则降级方案在 Decision 项记录
2. ErpFinVoucher 父视图落地 `<cell id="lines">` + `<view path="/erp/fin/pages/ErpFinVoucherLine/ErpFinVoucherLine.view.xml" grid="sub-grid-edit"/>`，支持行内新增/删除/编辑（P0 `sub-grid-edit` 范式）
3. ErpFinVoucherLine view.xml 新增 `<grid id="sub-grid-edit">` + `<grid id="sub-grid-view">`：17 列（lineNo/subjectId/subjectCode/dcDirection/debitAmount/creditAmount/currencyId/exchangeRate/amountSource/amountFunctional/partnerId/departmentId/projectId/warehouseId/materialId/costCenterId/memo），含科目树 picker + 行内 dcDirection 切换 + 多币种自动推算
4. 头表单 `totalDebit` / `totalCredit` 加入 view + edit + add（实时聚合行 SUM），过账按钮在不平衡时禁用 + ❌ 警告，`[借贷自动平衡]` 按钮将差额填入最后一行
5. subject 驱动的辅助维度 visibleOn 落地（6 维度 cell 按 `isAuxiliary*` 快照显隐）；dcDirection 驱动的 debit/credit 落地（引用 F7 预冻结表达式，clearValueOnHidden=true）
6. ErpFinVoucher 头表单补 `totalDebit`/`totalCredit` 显示（当前三表单均缺）

## Non-Goals

- **修改 ORM 模型 / xmeta / 后端 BizModel**（保护区域）——平衡校验客户端约束，接受 DRAFT save 无服务端兜底为既有 gap（Deferred 记录）
- **ErpFinVoucher 第二聚合子 `billLinks`（业财回链）子表编辑**——billLinks 由过账引擎系统生成，非手工编辑；属 F9/F12 范畴
- **凭证模板 `ErpFinVoucherTemplate` 自身的复杂页面**（F16 凭证模板配置 ★★★）——本计划仅消费模板做快速导入（若 Decision 裁定 in-scope）
- **F16 会计凭证录入复杂页面 ★★★（借贷平衡实时校验/辅助核算维度/自动平衡按钮/快捷模板 的完整 page 级增强）**——本计划仅做凭证**子表行内编辑**（child-table-editor），F16 的 page 级 tabs/向导/复杂交互属 F16 结果面
- **ErpFinVoucherLine query filterOp 补全**——1500-1 closure audit 已裁决「line entity, query via parent context, 0 filterOp by design」，接受既有裁决
- **修改 action-auth.xml / 菜单 / i18n**（F14/F15）
- **借贷平衡服务端校验补全**（后端 gap）——Deferred 记录 successor

## Task Route

- Type: `implementation-only change`（含 Explore 子阶段）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F4 Phase 2 P1 finance 行
  - `docs/design/finance/ui-patterns.md` §1（凭证录入要求 lines 29-67）
  - `docs/design/child-table-editor-patterns.md`（P0/P1/P2 范式 §2/§5/§6/§8/§12-§15）
  - `docs/design/visible-on-patterns.md` §3 + §8（dcDirection 预冻结表达式库）
  - `docs/design/status-color-map.md`（docStatus 着色，F5 已落地）
  - `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md`（sub-grid-edit + onEvent.setValue 自动推算 + 行级校验范式）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（view delta + input-table）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml sub-grid-edit + input-table + onEvent + picker + visibleOn）；不加载 `nop-backend-dev`（不改 BizModel/xbiz，平衡校验纯客户端 + 平台 `__save` 已支持聚合根）；不加载 `nop-testing`（浏览器抽样 + 既有 visual spec 归 Closure Gates）。

## Infrastructure And Config Prereqs

- `_dump/nop-app/` 目录存在
- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- **Explore 阶段需可本地运行的 AMIS 页面**用于实测 input-table cell clearValueOnHidden / picker snapshot / 跨实体 visibleOn 语义
- 无新 config / 端口 / 密钥依赖

## Execution Plan

### Phase 0 — Explore：3 个未验证模式运行时 PoC + 凭证模板/平衡范围 Decision

Status: planned
Targets: 临时探针 view.xml（探索后清理）+ plan 内 Decision 记录
Skill: `nop-frontend-dev`

- Item Types: `Explore | Decision`
- Prereqs: none（F7/F9/F10 已 completed）

- [ ] `Explore` (a)：在 ErpFinVoucher 临时探针中验证 AMIS `input-table` 列 cell 的 `clearValueOnHidden="true"` + `<visibleOn>${dcDirection == 'DEBIT'}</visibleOn>` 实际行为——切换 dcDirection 时对侧金额是否自动清空、row scope 是否正确（区别于 form cell）。验证 F7 §8.3 冻结裁决在 input-table 语境仍成立。
  - Skill: `nop-frontend-dev`
- [ ] `Explore` (b)：验证科目 picker onEvent.setValue 能否将所选 `ErpMdSubject` 的多字段（subjectCode + subjectName + 6 个 isAuxiliary* flag = 8 字段）批量快照进 input-table 行 scope（P0 物料 picker 快照 3 字段先例扩展到 8 字段）。
  - Skill: `nop-frontend-dev`
- [ ] `Explore` (c)：验证行 cell `<visibleOn>${isAuxiliaryPartner}</visibleOn>` 能否引用 (b) 快照进行 scope 的字段（跨实体 subject 驱动显隐）。
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 凭证模板快速导入（D6）范围裁决——in-scope（对齐 ui-patterns §1 (e)）或 Non-Goal successor（模板行 shape 与凭证行不同 + amountExpression 占位符替换非平凡）。记录选择 + 替代方案 + 残留风险。
  - Skill: `none`
- [ ] `Decision`: 借贷平衡服务端 gap 处理——接受客户端单边约束（对齐 Non-Goal「不改后端」）或 flag 为后端 successor。记录理由。
  - Skill: `none`

Exit Criteria:

- [ ] Explore (a)(b)(c) 三模式运行时验证有明确结论（通过 / 不通过 + 降级方案）；探针 view.xml 清理
- [ ] 凭证模板快速导入 + 平衡服务端 gap 两 Decision 在 plan 记录裁决

### Phase 1 — 头表单补全 + 子表 grid 骨架

Status: planned
Targets: `module-finance/erp-fin-web/.../pages/ErpFinVoucher/ErpFinVoucher.view.xml`（头表单补 totalDebit/totalCredit + `<cell id="lines">` 引用 sub-grid-edit）+ `module-finance/erp-fin-web/.../pages/ErpFinVoucherLine/ErpFinVoucherLine.view.xml`（新增 `<grid id="sub-grid-edit">` + `<grid id="sub-grid-view">` 骨架）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [ ] `Add`: ErpFinVoucher `<form id="view">` + `<form id="edit">` 在 posting 组补 `totalDebit` / `totalCredit` 显示（只读，经聚合派生）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: ErpFinVoucher 父视图 `<cell id="lines">` + `<view path="/erp/fin/pages/ErpFinVoucherLine/ErpFinVoucherLine.view.xml" grid="sub-grid-edit"/>`（对齐 P0 §4 范式）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: ErpFinVoucherLine `<grid id="sub-grid-edit">` + `<grid id="sub-grid-view">` 17 列骨架（列名见 Goals §3），先无 visibleOn/onEvent，仅列结构 + 科目/币种/维度 picker gen-control 占位。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: `xmllint --noout` 两 view.xml 通过；浏览器打开 ErpFinVoucher 编辑 drawer 见到子表行 + 头 totalDebit/totalCredit（此时为 0）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 头表单含 totalDebit/totalCredit；父视图 cell lines 引用 sub-grid-edit；子表 grid 17 列骨架渲染
- [ ] 两 view.xml `xmllint --noout` 通过

### Phase 2 — 科目树 picker + dcDirection 金额切换（F7 预冻结表达式）

Status: planned
Targets: ErpFinVoucherLine `<grid id="sub-grid-edit">` 的 subjectId/subjectCode + debitAmount/creditAmount cell
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 0 Explore (a)(b) 结论

- [ ] `Add`: subjectId cell picker 引用 F10 `ErpMdSubject.view.xml` picker（`filter_parentId=__null&filter_isLeaf=1`，仅叶科目可选）；onEvent.setValue 快照 subjectCode + subjectName（+ Phase 0 (b) 验证的 6 个 isAuxiliary* flag，若验证通过）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: debitAmount cell `visibleOn="${dcDirection == 'DEBIT'}"` `clearValueOnHidden="true"` + creditAmount cell `visibleOn="${dcDirection == 'CREDIT'}"` `clearValueOnHidden="true"`（引用 visible-on-patterns §8 预冻结，**不写** onEvent.change 清空对侧 per §8.3 裁决）。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 浏览器验证科目树弹窗选择 → 自动填 subjectCode/subjectName；切换行 dcDirection → 对侧金额 cell 隐藏并清空（依 Explore (a) 结论，若不成立走降级方案）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 科目 picker 选叶科目自动快照 code/name(+dim flags)；dcDirection 切换金额 cell 显隐 + 清空按 Explore (a) 结论工作

### Phase 3 — subject 驱动辅助维度 visibleOn + 多币种自动推算

Status: planned
Targets: ErpFinVoucherLine sub-grid-edit 的 6 个辅助维度 cell + currency/exchange/amount cell
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 + Phase 0 Explore (b)(c) 结论

- [ ] `Add`: 6 辅助维度 cell（partnerId/departmentId/projectId/warehouseId/materialId/costCenterId）`visibleOn` 引用 Phase 2 快照的 `isAuxiliary*` flag（依 Explore (c) 结论；若跨实体 visibleOn 不成立，降级为默认显示 + 科目配置驱动后端裁剪 successor）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 多币种自动推算——exchangeRate 默认 1；amountSource 经 picker/输入；onEvent.setValue 推算 `amountFunctional = amountSource × exchangeRate`（P0 乘法变体 §5 直接复用，新应用场景）。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 浏览器验证选科目（isAuxiliaryPartner=1）→ partnerId cell 出现；改科目为 isAuxiliaryPartner=0 → partnerId cell 隐藏；输入 amountSource=100/exchangeRate=6.5 → amountFunctional=650。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] subject 驱动维度显隐按 Explore (c) 结论工作；多币种推算正确

### Phase 4 — 头余额合计 + 自动平衡 + 过账按钮守卫

Status: planned
Targets: ErpFinVoucher 头表单 totalDebit/totalCredit 实时聚合 + 过账按钮 visibleOn/disable + 自动平衡按钮
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [ ] `Add`: totalDebit/totalCredit 实时聚合——input-table onEvent.change 触发 head `totalDebit = SUM(lines WHERE dcDirection=DEBIT, debitAmount)` / `totalCredit = SUM(lines WHERE dcDirection=CREDIT, creditAmount)`（P0 head 聚合 §6 范式扩展为条件 SUM）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 过账按钮（既有 row-post-button）增加 disable 条件——`${totalDebit != totalCredit}` 时禁用 + ❌ 红色警告标记（ui-patterns §1 line 65）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: `[借贷自动平衡]` toolbar 按钮——onClick 计算差额，填入最后一行（借<贷补借方，反之补贷方，ui-patterns §1 line 67）。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 浏览器验证录入不平衡行 → totalDebit ≠ totalCredit 显示 ❌ + 过账禁用；点自动平衡 → 差额填入末行 → 平衡 ✅ + 过账启用；`__save` 持久化 DRAFT 成功（接受无服务端平衡校验 gap）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 头合计实时聚合；不平衡时过账禁用 + 警告；自动平衡按钮填差额；DRAFT save 持久化

### Phase 5 — 凭证模板快速导入（依 Phase 0 Decision；若 in-scope）

Status: planned
Targets: ErpFinVoucher toolbar `[快速模板]` 按钮 + 模板选择弹窗
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 4 + Phase 0 凭证模板 Decision（若 Non-Goal 则本阶段整体移出范围并记 Deferred）

- [ ] `Add`（若 in-scope）：`[快速模板]` toolbar 按钮 → 弹窗选择 `ErpFinVoucherTemplate` → 读取其 `ErpFinVoucherTemplateLine`（subjectCode/dcDirection/amountExpression/accountKey/amountKey/memoTemplate）→ 经占位符替换映射覆写 input-table lines（ui-patterns §1 line 68）。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`（若 in-scope）：浏览器验证选模板 → 行自动填充 + 占位符替换正确。
  - Skill: `nop-frontend-dev`

Exit Criteria（若 in-scope）:

- [ ] 模板快速导入覆写 lines + 占位符替换正确

> 若 Phase 0 Decision 裁定 Non-Goal：本阶段移出范围，记入 Deferred But Adjudicated「凭证模板快速导入 successor」。

## Draft Review Record

- Independent draft review iteration 1: **accept** (`ses_0805cc9a1ffejogibwO715sQvY`) — 无 blocker。全 baseline spot-check 通过（ErpFinVoucher/VoucherLine view.xml 现状 + ORM 29 列/7 辅助维度非 9 + ErpMdSubject 6 isAuxiliary flag 含 isAuxiliaryProduct 陷阱 + xmeta/ORM 聚合根 save + 后端 `postVoucher` 绕过 `assertBalanced` gap 独立确认 + F7/F9/F10 均 completed + roadmap:134 待启动 + 前任 deferral §209-213）。Explore 阶段为真实门控含降级路径；后端 gap 经三处（baseline/Non-Goal/Deferred）如实披露并归类 `watch-only residual + Successor Required: yes`。4 项非阻塞建议（D 标签定义、Explore a/b 降级路径预注、条件 SUM 可作第 4 Explore、materialId←isAuxiliaryProduct 映射显式）可执行期处理。Plan 可晋 `active`。

## Closure Gates

- [ ] 范围内行为完成（Phase 0–4 全部 done；Phase 5 依 Decision done 或显式移出范围）
- [ ] 相关文档对齐：`docs/design/child-table-editor-patterns.md` 追加「凭证子表变体」段落（dcDirection 行内切换 + 科目 picker 多字段快照 + subject 驱动维度 visibleOn + 多币种推算 + 条件 SUM 余额）；`docs/design/visible-on-patterns.md` §3/§8 将预冻结表达式状态由 🟡 F4 successor 更新为 ✅ 落地 + 新增 subject 驱动维度表达式库段落
- [ ] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test` 全绿（含 `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）+ 两 view.xml `xmllint --noout` 通过 + 浏览器抽样：科目树选择/dim 显隐/dcDirection 切换/多币种推算/余额平衡/自动平衡/过账守卫全路径
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 借贷平衡服务端校验（DRAFT save / postVoucher 路径）

- Classification: `watch-only residual`
- Why Not Blocking Closure: `ErpFinVoucherBizModel.__save` DRAFT 持久化与 `postVoucher` DRAFT→POSTED 均不跑 `assertBalanced`（仅过账引擎 `ErpFinPostingProcessor:646` 跨域 post 路径跑）；本计划接受客户端单边约束为既有 gap，不改后端（Non-Goal）。
- Successor Required: `yes`（触发条件：后端凭证录入校验增强 plan 启动时，或在生产中发生不平衡 DRAFT 凭证被 postVoucher 通过的缺陷报告时）

### 凭证模板快速导入（若 Phase 0 Decision 裁定 Non-Goal）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 模板行 shape 与凭证行不同 + amountExpression 占位符替换非平凡；若 Phase 0 裁定 Non-Goal 则归本 successor。
- Successor Required: `yes`（触发条件：F16 凭证模板配置复杂页面 plan 启动时）

### ErpFinVoucher billLinks 子表编辑（业财回链）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: billLinks 由过账引擎系统生成非手工编辑；属 F9/F12 范畴。
- Successor Required: `no`（除非未来业务需手工维护业财回链）

### F16 会计凭证录入复杂页面（page 级 tabs/向导/复杂交互）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅做凭证子表行内编辑（child-table-editor 结果面）；F16 的 page 级 ★★★ 复杂交互（独立 view.xml 或 AMIS 定制）属 F16 结果面。
- Successor Required: `yes`（触发条件：F16 plan 启动时）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>
- Evidence: <待填写>

Follow-up:

- 借贷平衡服务端校验 successor（依触发条件）
- F16 会计凭证录入复杂页面 plan
- F16 凭证模板配置复杂页面 plan（若 Phase 0 模板导入裁定 Non-Goal）
