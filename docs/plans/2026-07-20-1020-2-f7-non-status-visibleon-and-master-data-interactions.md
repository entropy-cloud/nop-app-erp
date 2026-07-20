# 2026-07-20-1020-2-f7-non-status-visibleon-and-master-data-interactions F7 — 非状态 visibleOn 条件 + 主数据专用交互

> Plan Status: active
> Last Reviewed: 2026-07-20
> Source: `docs/backlog/frontend-ui-roadmap.md` §F7（非状态驱动的 visibleOn 条件 P1 — todo）；§跨切面 UI 模式 6（删除/停用引用预览）+ 7（编码唯一性前置校验）部分覆盖
> Related: `docs/plans/2026-07-19-1122-1-view-button-gap-fix.md`（F1 已完成状态驱动 visibleOn，本计划扩展非状态 visibleOn）；`docs/plans/2026-07-20-1020-1-f10-tree-entity-views.md`（同期 plan，F10 落地 ErpMdSubject tree picker 是 F4 finance voucher 的辅助核算 visibleOn 前置之一）；`docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 P0 已为 47 实体 form view/edit 落地 layout 分组，本计划在该 layout 基础上添加 cell 级 visibleOn）
> Audit: required

## Bundling Justification (Rule 4 + Rule 14)

本计划同时承载 2 个子特性：(A) 字段值驱动 visibleOn + (B) 主数据专用交互（编码唯一性 / 删除引用预览 / 启用停用 Switch）。Rule 4 要求「一个计划一个结果面」，Rule 14 允许「同一组件多功能优先使用一个 owner plan」。bundling 裁决：

- **共享 owner doc**：2 子特性均归属 `docs/backlog/frontend-uiroadmap.md §F7`（roadmap §F7 §1 + §3 明确涵盖；§跨切面 6/7/10 也明示「统一实现模式」）
- **共享 view.xml + BizModel 编辑表面**：2 子特性均修改 `ErpMdPartner.view.xml` / `ErpMdMaterial.view.xml` / `ErpInvStockMove.view.xml` 等头实体 view.xml 的 form view/edit cell 段 + 主数据 BizModel；若拆为 2 plan，2 个独立 session 将并发编辑同一文件集合，违反 Rule 14 反并发原则
- **共享 visibleOn / onEvent 表达式库**：2 子特性均依赖 AMIS 表达式（visibleOn / onEvent.change.actions）+ @BizQuery 后端入口，无独立技术路径
- **裁决**：bundling 满足 Rule 14「同一组件多功能一个 plan」，不违反 Rule 4（结果面统一为「非状态驱动的智能 UI 交互」：用户感知是「表单字段/按钮根据业务规则智能响应」，无论交互原语是 visibleOn 表达式 / async validator / 自定义 dialog / Switch 控件）

**注**：原 §F7 §2「配置门控 UI 指示」从本计划剥离至独立 successor（见 Deferred §F7 §2 配置门控 UI 指示），因 (a) 跨域 config 聚合需新建 BizModel 但模块归属复杂（notify 模块不能跨域 import 各域 Configs 类）；(b) 实际 UI 相关 config flag 仅 ~5 个（经实时仓库重新盘点，远少于原估 ~15）；(c) 拆分后两计划结果面更清晰。

## Current Baseline

基于实时仓库核实（2026-07-20，独立子代理审查后修正）：

- **F1 状态驱动 visibleOn 已全 18 域落地**（plan `2026-07-19-1122-1`）：所有 status 驱动按钮已含 `${docStatus == 'X' && approveStatus == 'Y'}` 形式 visibleOn 表达式。本计划扩展的「非状态 visibleOn」属于另一正交维度。
- **F7 §1 字段值驱动 visibleOn 当前缺口**（抽样核实 2 域，**字段名经实时仓库核实**）：
  - `ErpInvStockMove.view.xml:9,61,86,108` form view/edit 含 **`moveType`**（非 operationType）字段，dict `erp-inv/operation-type` 含 `INCOMING / OUTGOING / INTERNAL / MANUFACTURE` 4 值（无 TRANSFER）；form 同含 `sourceLocationId` / `destLocationId` 字段，**当前全部静态显示无 visibleOn**（INCOMING 时 sourceLocationId 无意义——入库无内部来源、OUTGOING 时 destLocationId 无意义——出库无内部去向、INTERNAL/MANUFACTURE 时两者均需）
  - `ErpAstMaintenance.view.xml:13` form view/edit 含 **`treatment`**（非 decideTreatment）字段（EXPENSE/CAPITALIZE）+ 维修中转清算科目字段，**当前静态显示**（CAPITALIZE 时才需中转科目）
  - ErpFinVoucherLine dcDirection 切换 visibleOn 归 F4 finance voucher successor（不在本计划范围；本计划仅为其准备 visibleOn 表达式库于 `visible-on-patterns.md`）
- **F7 §3 主数据专用交互当前缺口**：
  - **编码唯一性前置校验**：抽样核实 `ErpMdMaterial.view.xml` + `ErpMdPartner.view.xml` + `ErpMdSubject.view.xml` form edit/add `code` 字段均无 async validator；用户输入冲突编码后只能在保存时报 `SQLException: Unique constraint violation`，UX 体验差
  - **删除前引用预览**：抽样核实 `_gen/_ErpMdMaterial.view.xml:159-170` + `_gen/_ErpMdPartner.view.xml` **codegen 默认 rowActions** 含 `row-delete-button` 调 `__delete?id=$id` mutation 无引用预览弹窗；手写层 view.xml（如 `ErpMdMaterial.view.xml` + `ErpMdPartner.view.xml`）`<pages>` 段仅含 `<crud name="main"/>` 默认引用，未 bounded-merge 覆盖 rowActions；用户删除被引用主数据时后端报错
  - **启用/停用 Switch 控件**：抽样核实 `ErpMdMaterial.view.xml` + `ErpMdPartner.view.xml` form view/edit `status` 字段为 codegen 默认 `button-group-select`（基于 dict）；设计要求（`master-data/ui-patterns.md`）期望为 Switch 控件 + 停用提示弹窗
- **后端 @BizQuery 能力**：codegen 默认在所有实体 BizModel 生成 `__findPage` / `__findList`（`CrudBizModel.java:1528-1532` 内置），**未生成 `isCodeUnique` / `countReferences` 等专用 @BizQuery**——本计划新增（见 Phase 2）。
- **F4 finance voucher plan 明确将 F7 列为 successor 前置**（plan `2026-07-20-0629-1` l.45：「finance `ui-patterns.md §1` 明确要求科目树 picker（F10 范畴）+ 辅助核算列条件 visibleOn（**F7 范畴**）」）：本计划完成后 F4 finance voucher 的辅助核算 visibleOn 表达式库前置就绪。
- **Nop Platform visibleOn / async validator / Switch 控件范式已文档化**：
  - `../nop-entropy/docs-for-ai/03-runbooks/replace-field-with-complex-control.md`（gen-control + visibleOn 表达式）
  - `../nop-entropy/docs-for-ai/03-runbooks/add-field-and-validation.md`（async validator + onEvent）
  - 参考实例：`NopAuthResource.view.xml:80-88` 示范 `<cell id="menuProps"><visibleOn>${resourceType != 'FNPT'}</visibleOn></cell>` 字段值驱动 visibleOn
- **前置验证基线**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；F1/F3/F4 P0/F4 P1/F5/F6/F8/F9 plan 已全绿。

## Goals

1. **字段值驱动 visibleOn 覆盖 2 代表域**（roadmap §F7 §1）：
   - `ErpInvStockMove` form view/edit：`sourceLocationId` visibleOn `${moveType != 'INCOMING'}` + `destLocationId` visibleOn `${moveType != 'OUTGOING'}`（含 `clearValueOnHidden="true"` 防隐藏字段提交脏数据）
   - `ErpAstMaintenance` form view/edit：维修中转清算科目字段 visibleOn `${treatment == 'CAPITALIZE'}`
2. **主数据编码唯一性前置校验覆盖 3 高频实体**（roadmap §F7 §3 + §跨切面 7）：
   - `ErpMdMaterial` / `ErpMdPartner` / `ErpMdSubject` 各新增 `IErpMdXxxBiz.isCodeUnique(String code, Long excludeId) → boolean` @BizQuery（excludeId 用于 edit 时排除自身）
   - view.xml form edit/add `code` 字段 onEvent.blur 异步调用 isCodeUnique，输入框旁显示 ✓（绿色，可用）或 ✗（红色，「编码已存在」提示）
3. **主数据删除引用预览覆盖 2 高频实体**（roadmap §F7 §3 + §跨切面 6 + 10）：
   - `ErpMdPartner` / `ErpMdMaterial` 各新增 `IErpMdXxxBiz.countReferences(Long id) → Map<String,Long>` @BizQuery 返回各业务单据引用数（如 `{purchaseOrder: 5, salesOrder: 3, ...}`）
   - view.xml 在手写层 bounded-merge rowActions `row-delete-button` 改造：点击先调 countReferences → 若任一引用数 > 0 弹出引用预览 dialog（列出 N 张单据 + 跳转链接）+ 阻断删除；若 0 引用走原 __delete 路径
4. **主数据启用/停用 Switch 控件覆盖 2 高频实体**（roadmap §F7 §3）：
   - `ErpMdMaterial` / `ErpMdPartner` form view/edit `status` 字段从 `button-group-select`（ACTIVE/INACTIVE dict）升级为 AMIS `switch` 控件 + onEvent.change 弹出确认 dialog（停用前提示「停用后该实体不可在新单据中选择，是否继续？」）
5. **Playwright action spec 扩展**：新建 `tests/e2e/business-actions/non-status-visibleon-and-master-data.action.spec.ts` 含 ~8 用例覆盖：2 字段驱动 visibleOn（moveType 切换、treatment 切换）+ 3 唯一性校验（重复编码 ✗ + 新编码 ✓ + edit 自身排除）+ 2 引用预览（有引用阻断 + 无引用放行）+ 1 Switch 切换提示弹窗
6. **更新 `docs/design/master-data/ui-patterns.md` + `inventory/ui-patterns.md` + `assets/ui-patterns.md`**：每域新增「非状态 visibleOn 模式」+「主数据专用交互模式」段落，固化 visibleOn 表达式库 / async validator 范式 / 引用预览 dialog 模板 / Switch 控件范式；新建 `docs/design/visible-on-patterns.md` 跨域范式参考。
7. **完成 F4 finance voucher plan 的辅助核算 visibleOn 表达式库前置**（plan `2026-07-20-0629-1` l.45 显式列出 F7 为前置之一）：本计划在 `visible-on-patterns.md` 中记录 dcDirection 切换 visibleOn 表达式（debitAmount `${dcDirection == 'DEBIT'}` / creditAmount `${dcDirection == 'CREDIT'}`）+ onEvent 切换清空对方字段范式，待 F4 finance voucher plan 落地 sub-grid-edit 时引用。

## Non-Goals

- **配置门控 UI 指示（§F7 §2）**——独立 successor plan（见 Deferred §F7 §2）；本计划仅完成字段驱动 visibleOn + 主数据交互；触发条件「跨域 config 聚合 BizModel 模块归属裁决后」可由独立 successor plan 主动驱动满足。
- **ErpFinVoucher sub-grid-edit 落地**（凭证行编辑控件本身）——F4 finance voucher successor plan 范畴；本计划仅为其准备好 visibleOn 表达式库与 dcDirection 切换 onEvent 范式（在 `visible-on-patterns.md` 记录，待 finance voucher plan 落地 sub-grid-edit 时引用）
- **修改 ORM 模型**——保护区域，本计划不动模型。
- **状态驱动 visibleOn 补齐**（F1 已全 18 域落地）——本计划严格限定非状态 visibleOn。
- **F8 搜索/过滤条件增强**——本计划 visibleOn 限定 form view/edit cell 级，不改 list query / asideFilter 字段集。
- **F11 批量操作**（批量审批 / 批量停用主数据）——F11 plan 范畴；本计划 Switch 控件仅做单条主数据切换。
- **F12 详情页 tabs**——本计划 cell visibleOn 不引入 tabs 容器。
- **F15 i18n**（提示文案 i18n-en）——本计划使用中文提示；F15 plan 范畴。
- **域专用业务规则引擎**（如物料编码规则校验、科目编码层级规则）——业务规则范畴，非前端 UI；本计划仅做唯一性前置校验（DB 查询）。
- **跨域引用预览**（如 ErpMdMaterial 删除时预览所有 18 域引用）——本计划覆盖 purchase/sales/inventory 3 高频引用域，长尾域引用归 successor。
- **WebSocket 实时引用计数刷新**——本计划一次性查询，不引入 WebSocket。
- **审计日志 / 操作回放**（谁在何时尝试删除被引用主数据）——非 UI 范畴。
- **像素级视觉回归**——独立 plan 覆盖。

## Task Route

- Type: `app-layer design change + implementation-only change`（含后端 @BizQuery 新增方法 → 列入 app-layer 设计变更；前端 view.xml + AMIS → implementation-only）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F7 §1+§3 + §跨切面 UI 模式 6/7
  - `docs/design/master-data/ui-patterns.md`（主数据专用交互业务语义）
  - `docs/design/inventory/ui-patterns.md`（StockMove moveType 业务语义）
  - `docs/design/assets/ui-patterns.md`（AstMaintenance treatment 业务语义）
  - `docs/architecture/view-and-page-strategy.md`（cell visibleOn + onEvent 表达式层）
  - `../nop-entropy/docs-for-ai/03-runbooks/replace-field-with-complex-control.md`（gen-control + visibleOn）
  - `../nop-entropy/docs-for-ai/03-runbooks/add-field-and-validation.md`（async validator + onEvent）
  - 参考实例：`NopAuthResource.view.xml`（字段值驱动 visibleOn 范式）
  - 平台源码参考：`CrudBizModel.java:1528-1532`（`findList` 内置 @BizQuery 实现，本计划新增 @BizQuery 范式参考）
- Skill Selection Basis: 加载 `nop-frontend-dev`（cell visibleOn + onEvent + async validator + AMIS switch + dialog）；加载 `nop-backend-dev`（新增 @BizQuery 方法 isCodeUnique/countReferences）；加载 `nop-testing`（playwright action spec 扩展）。

## Infrastructure And Config Prereqs

- 手写层 view.xml 路径（5 实体）：
  - `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdMaterial/ErpMdMaterial.view.xml`
  - `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdPartner/ErpMdPartner.view.xml`
  - `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdSubject/ErpMdSubject.view.xml`
  - `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockMove/ErpInvStockMove.view.xml`
  - `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/ErpAstMaintenance/ErpAstMaintenance.view.xml`
- 后端 BizModel 路径（3 文件，新增 @BizQuery 方法）：
  - `module-master-data/erp-md-service/src/main/java/.../ErpMdMaterialBizModel.java`
  - `module-master-data/erp-md-service/src/main/java/.../ErpMdPartnerBizModel.java`
  - `module-master-data/erp-md-service/src/main/java/.../ErpMdSubjectBizModel.java`
- IBiz 接口路径（同步加性扩展）：
  - `IErpMdMaterialBiz` / `IErpMdPartnerBiz` / `IErpMdSubjectBiz`
- 修改后运行 `mvn clean install -DskipTests` 触发 codegen 增量重新展开 page.yaml + IBiz 同步
- 本地运行验证：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Playwright webServer 已配置
- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 — 范式裁决与 visibleOn 表达式库冻结

Status: planned
Targets: `docs/design/visible-on-patterns.md`（新建跨域范式参考）+ `docs/design/master-data/ui-patterns.md`（主数据专用交互段落）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] `Decision`: 决策字段值驱动 visibleOn 表达式库（约束式表格），每行：实体 / 字段 / 表达式 / clearValueOnHidden。本计划范围（字段名经实时仓库核实）：
  - `ErpInvStockMove.sourceLocationId`：visibleOn `${moveType != 'INCOMING'}` + clearValueOnHidden=true
  - `ErpInvStockMove.destLocationId`：visibleOn `${moveType != 'OUTGOING'}` + clearValueOnHidden=true
  - `ErpAstMaintenance` 维修中转清算科目字段：visibleOn `${treatment == 'CAPITALIZE'}` + clearValueOnHidden=true
  - **ErpFinVoucherLine（F4 finance voucher successor 引用，不在本计划实施范围）**：`debitAmount` visibleOn `${dcDirection == 'DEBIT'}` + clearValueOnHidden=true；`creditAmount` visibleOn `${dcDirection == 'CREDIT'}` + clearValueOnHidden=true；onEvent 切换 dcDirection 时清空对方字段（防借贷同填）
  - 来源：实时仓库 view.xml 字段名核实（moveType 而非 operationType，treatment 而非 decideTreatment）+ 各域 `ui-patterns.md` 业务语义
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策主数据编码唯一性前置校验触发时机：
  - **方案 A（采纳）**：form edit/add `code` 字段 `onEvent.blur` 异步触发（用户离开输入框时校验）；不在 `onEvent.change` 触发避免每键击一次请求。
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策主数据删除引用预览覆盖范围：
  - **方案 A（采纳）**：本计划仅覆盖 ErpMdPartner + ErpMdMaterial（最高频引用），引用域限定 purchase（Order/Receive/Invoice）+ sales（Order/Delivery/Invoice）+ inventory（StockMove）共 6 表 count；长尾域引用归 successor
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策 Switch 控件实现：
  - **方案 A（采纳）**：`<cell id="status"><gen-control><c:script>return {type:'switch', trueValue:'ACTIVE', falseValue:'INACTIVE', onEvent:{change:{actions:[{actionType:'dialog', dialog:{...确认弹窗...}}]}}}</c:script></gen-control></cell>`；停用确认弹窗用 AMIS dialog actionType，确认后调 `__update` mutation 翻转 status；取消回滚 Switch 状态。
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策 ErpMdSubject 是否新增 Switch 控件（不新增 countReferences，因科目可停用不可删除——但 status 字段存在）：
  - **方案 A（采纳）**：本计划仅 ErpMdMaterial + ErpMdPartner 落地 Switch；ErpMdSubject Switch 归 successor（触发条件「按域推进主数据 Switch 控件全覆盖」）。理由：保持 3 实体唯一性 + 2 实体 Switch 的最小完整切片，避免范围蔓延。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 新建 `docs/design/visible-on-patterns.md` 固化跨域范式：字段值驱动 visibleOn 表达式库（含 dcDirection 切换为 F4 finance voucher successor 引用）+ async validator 范式 + 引用预览 dialog 模板 + Switch 控件范式（对齐 `child-table-editor-patterns.md` 结构作为长尾域扩展参考）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] `docs/design/visible-on-patterns.md` 文件存在且包含 4 段范式（visibleOn 表达式库 + async validator + 引用预览 + Switch）
- [ ] Phase 1 各 Decision 在 plan 内记录裁决（5 项）
- [ ] `docs/design/master-data/ui-patterns.md` 新增「主数据专用交互模式」段落

### Phase 2 — 后端 @BizQuery 方法落地（isCodeUnique + countReferences）

Status: planned
Targets: 3 主数据 BizModel + 对应 IBiz 接口
Skill: `nop-backend-dev`

- Item Types: `Add-heavy` (4/4 items tagged Add)
- Prereqs: Phase 1 决策冻结

- [ ] `Add`: `IErpMdMaterialBiz` 加性新增 `isCodeUnique(String code, Long excludeId) → boolean` + `countReferences(Long id) → Map<String,Long>` 接口声明；`ErpMdMaterialBizModel` 实现：
  - `isCodeUnique`：`dao().findList(QueryBean.newBuilder().addFilter(eq("code", code)))` 过滤 `id != excludeId` 返回 isEmpty
  - `countReferences`：分别 count `ErpPurOrderLine` / `ErpPurReceiveLine` / `ErpPurInvoiceLine` / `ErpSalOrderLine` / `ErpSalDeliveryLine` / `ErpSalInvoiceLine` / `ErpInvStockMove` 中 `materialId == id` 的行数（7 表 count），组装 `Map<String,Long>`
  - Skill: `nop-backend-dev`
- [ ] `Add`: `IErpMdPartnerBiz` 同步新增 `isCodeUnique` + `countReferences`（引用域相同，字段名替换为 `partnerId`，含 `ErpPurOrder` / `ErpPurReceive` / `ErpPurInvoice` / `ErpSalOrder` / `ErpSalDelivery` / `ErpSalInvoice` / `ErpInvStockMove` 头表的 `supplierId`/`customerId` 字段，7 表 count）
  - Skill: `nop-backend-dev`
- [ ] `Add`: `IErpMdSubjectBiz` 加性新增 `isCodeUnique`（不新增 countReferences —— 科目引用经 voucherLine 间接，会计语义上科目可停用不可删除，引用预览归 Non-Goal）
  - Skill: `nop-backend-dev`
- [ ] `Add`: JUnit 单元测试覆盖 3 新增方法（每方法至少 2 用例：正路径 + 边界 / 守卫）：
  - `TestErpMdMaterialBiz.testIsCodeUnique`：新编码 true / 重复 false / excludeId 自身排除 true
  - `TestErpMdMaterialBiz.testCountReferences`：0 引用返回全 0 Map / 多引用返回非零计数
  - `TestErpMdPartnerBiz` 同形 2 用例
  - `TestErpMdSubjectBiz.testIsCodeUnique` 同形 1 用例（无 countReferences）
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 3 BizModel 新增方法 + 3 IBiz 接口声明全部落地
- [ ] JUnit 测试方法全绿（`mvn test -pl module-master-data/erp-md-service -am`）
- [ ] 修改后运行 `mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS（IBiz 同步 + xbiz 自动展开不报错）

### Phase 3 — 前端 view.xml + AMIS 接线

Status: planned
Targets: 5 实体 view.xml + main.page.yaml + cell visibleOn + onEvent + AMIS switch + dialog
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy` (6/6 items tagged Add)
- Prereqs: Phase 2 后端 @BizQuery 落地

- [ ] `Add`: `ErpInvStockMove.view.xml` form view/edit `<cell id="sourceLocationId">` + `<cell id="destLocationId">` 加 visibleOn 表达式（`moveType` 字段名经实时仓库核实）+ clearValueOnHidden=true
  - Skill: `nop-frontend-dev`
- [ ] `Add`: `ErpAstMaintenance.view.xml` form view/edit 维修中转清算科目 `<cell>` 加 visibleOn（`treatment` 字段名经实时仓库核实）+ clearValueOnHidden=true
  - Skill: `nop-frontend-dev`
- [ ] `Add`: `ErpMdMaterial.view.xml` + `ErpMdPartner.view.xml` + `ErpMdSubject.view.xml` form edit/add `<cell id="code">` 加 onEvent.blur async validator 调 `__isCodeUnique`（@BizQuery）+ ✓/✗ 图标显示（gen-control 返回 input-group + addon icon）
  - Skill: `nop-frontend-dev`
- [ ] `Add`: `ErpMdMaterial.view.xml` + `ErpMdPartner.view.xml` 在手写层 bounded-merge rowActions 改造（覆盖 codegen 默认 `row-delete-button`）：点击先调 `__countReferences` → 弹出引用预览 dialog（若任一引用 > 0）+ 阻断 / 走原 __delete（若 0 引用）；dialog 含引用列表 + row-view 跳转链接（对齐 F9 跨单据导航 link 范式）
  - Skill: `nop-frontend-dev`
- [ ] `Add`: `ErpMdMaterial.view.xml` + `ErpMdPartner.view.xml` form view/edit `<cell id="status">` 升级为 AMIS switch 控件 + onEvent.change 弹出停用确认 dialog（status 由 ACTIVE → INACTIVE 时）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 5 实体 view.xml 均含相应 visibleOn / onEvent / switch / dialog 接线
- [ ] 修改后运行 `mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS（codegen 重新展开 page.yaml 不报错）
- [ ] `mvn -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest test` PAGE_ERROR_COUNT=0

### Phase 4 — Playwright action spec + 文档对齐

Status: planned
Targets: `tests/e2e/business-actions/non-status-visibleon-and-master-data.action.spec.ts`（新建）+ 各域 ui-patterns.md 更新
Skill: `nop-frontend-dev | nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 3 view.xml 接线完成

- [ ] `Add`: 新建 `tests/e2e/business-actions/non-status-visibleon-and-master-data.action.spec.ts` 含 ~8 用例：
  - **字段值 visibleOn**（2）：StockMove moveType=INCOMING → sourceLocationId 隐藏 / moveType=OUTGOING → destLocationId 隐藏 / moveType=INTERNAL → 两者均显（合并 1 用例含 3 断言）；AstMaintenance treatment=CAPITALIZE → 中转清算科目显 / treatment=EXPENSE → 隐藏（合并 1 用例含 2 断言）
  - **唯一性校验**（3）：Material 重复 code ✗ 提示 + 新 code ✓ + edit 自身 code 不报 ✗
  - **引用预览**（2）：Material 有引用 → 弹 dialog + 删除阻断 / Material 0 引用 → 走原 __delete 成功
  - **Switch 控件**（1）：Material status ACTIVE→INACTIVE 弹停用确认 dialog / 取消回滚
  - Skill: `nop-testing`
- [ ] `Proof`: ~8 用例全部 PASS（`npx playwright test non-status-visibleon-and-master-data.action.spec.ts`）
  - Skill: `nop-testing`
- [ ] `Add`: 更新 `docs/design/master-data/ui-patterns.md` + `inventory/ui-patterns.md` + `assets/ui-patterns.md`，每域新增「非状态 visibleOn 模式」+「主数据专用交互模式」段落引用 `visible-on-patterns.md`
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] `tests/e2e/business-actions/non-status-visibleon-and-master-data.action.spec.ts` 文件存在且 ~8 用例 PASS
- [ ] 3 域 ui-patterns.md 各含相应新增段落

## Draft Review Record

- Independent draft review iteration 1: needs-revision（ses_082a44b1effemUjTVk7z6dbec9，2026-07-20）——发现 6 blocking issues：(B1) 字段名 `operationType` 应为 `moveType`，且无 TRANSFER 值；(B2) 模块路径 `module-notify/erp-sys-service` 不存在；(B3) 跨模块 config 聚合依赖断裂；(B4) 字段名 `decideTreatment` 应为 `treatment`；(B5) config flag 数量 ~15 实际 ~5；(B6) row-delete-button 描述误导（codegen 默认）。iteration 2 修订：剥离 §F7 §2 配置门控至独立 successor（规避 B2/B3/B5）；修正字段名 moveType + treatment（B1/B4）；修正 row-delete-button codegen 默认描述（B6）。
- Independent draft review iteration 2: accept-with-minor-comments（ses_0828d4835ffeX1piO4NmfQbJDi，2026-07-20）——B1-B6 全部 resolved；field 名一致 + dict 值经 `2026-07-01-0811-2-inventory-stockmove-bizmodel.md:16,24` 交叉核实；§F7 §2 deferral 触发条件明确；bundling 范围 (A)+(B) 清晰。minor N1（line 26 source/dest 语义 prose 反转，visibleOn 表达式本身正确）已修复；N2（phase exit 全 mvn build）defensible 保留。Plan Status 翻转为 active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在结束时运行一次 `mvn clean install -DskipTests` + `mvn test -pl module-master-data/erp-md-service -am` + `mvn -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest test` + `npx playwright test non-status-visibleon-and-master-data.action.spec.ts`。

- [ ] 范围内行为完成（2 字段 visibleOn + 3 唯一性校验 + 2 引用预览 + 2 Switch 控件）
- [ ] 相关文档对齐（`docs/design/visible-on-patterns.md` + 3 域 ui-patterns.md 段落）
- [ ] 已运行验证（`mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS + `mvn test` JUnit 全绿 + `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0 + `npx playwright test non-status-visibleon-and-master-data.action.spec.ts` 全绿）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### F7 §2 配置门控 UI 指示（剥离至独立 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 原 §F7 §2 列入本计划，但实时仓库审查发现：(a) 跨域 config 聚合 BizModel 模块归属复杂（notify 模块不可跨域 import 各域 Configs 类）；(b) 实际 UI 相关 config flag 经重新盘点仅 ~5 个（laborPostingEnabled / sparePartPostingEnabled / asnAutoCreateReceive / webhookSignatureRequired b2b+log / cipInterestCapitalizationEnabled 等少数），远少于原估 ~15；(c) 拆分后两计划结果面更清晰。独立 successor plan 须先决策「跨域 config 聚合实现策略」（候选方案：per-domain `getUiConfigFlags` @BizQuery + 前端合并 vs 全局 BizModel via `AppConfig.var(...)` literal strings vs typed DTO）
- Successor Required: yes（触发条件「跨域 config 聚合 BizModel 模块归属裁决后」，可由独立 successor plan 主动驱动满足）

### ErpFinVoucherLine sub-grid-edit + dcDirection 切换 onEvent

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ErpFinVoucher sub-grid-edit 本身属 F4 finance voucher successor plan（依赖 F7 + F10 落地后启动）；本计划仅为其准备好 visibleOn 表达式库与 dcDirection 切换 onEvent 范式（记录于 `visible-on-patterns.md`），待 finance voucher plan 落地 sub-grid-edit 时引用
- Successor Required: yes（F4 finance voucher plan 启动时引用本计划 visibleOn 表达式）

### 主数据删除引用预览覆盖长尾引用域

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划覆盖 purchase/sales/inventory 3 高频引用域（7 表 count）；长尾域引用（assets/projects/quality/maintenance/manufacturing/contract/drp 等引用 materialId 或 partnerId 的表）按相同 countReferences 范式补齐，触发条件「按域推进主数据引用预览全覆盖」
- Successor Required: yes（按域推进剩余引用域时复用本计划 countReferences 范式）

### ErpMdSubject countReferences（科目引用预览）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 会计语义上科目可停用不可删除（保留历史凭证完整性），F1 已移除 ErpMdSubject 删除按钮；countReferences 无删除路径消费者，无价值
- Successor Required: no

### ErpMdSubject Switch 控件

- Classification: `optimization candidate`
- Why Not Blocking Closure: ErpMdSubject 有 `status` 字段（ACTIVE/INACTIVE）理论上可应用 Switch 控件；本计划仅落地 Material/Partner 2 高频实体保持最小完整切片；触发条件「按域推进主数据 Switch 控件全覆盖」
- Successor Required: yes

### 编码规则引擎（物料编码规则校验、科目编码层级规则）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 业务规则范畴（如物料编码必须「字母-数字」格式、科目编码必须与父级编码前缀匹配），非前端 UI 范畴；本计划仅做唯一性 DB 校验
- Successor Required: no

## Closure

Status Note: 计划已通过独立草案审查 iteration 2 (accept-with-minor-comments，minor 已修复)，Plan Status 翻转为 active，可进入实施。
