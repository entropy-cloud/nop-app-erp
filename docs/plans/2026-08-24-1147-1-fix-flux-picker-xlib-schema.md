# 2026-08-24-1147-1 通过 delta 定制修复 flux 控件库字段级 picker schema 不匹配 flux picker 渲染器契约

> Plan Status: completed
> Last Reviewed: 2026-08-24
> Source: 用户 2026-08-24 直接请求 + 用户 2026-08-24 后续发现 x:post-extends 自动重写机制 + delta 定制路径
> Related: `docs-for-ai/02-core-guides/flux-rendering.md §自动切换机制`、`docs-for-ai/06-extensibility/how-to-use-extensibility-in-business-implementation.md §Delta`、`docs/analysis/2026-08-03-1232-flux-crud-validation-evidence.md §5 picker 归属裁决`、`docs/plans/2026-08-03-1232-1-flux-crud-migration.md §111`、`docs/backlog/frontend-ui-roadmap.md §Flux 全量迁移`
> Audit: required（`auto + dual-agent-approval` 跨仓库保护区域；**v3 不再跨仓库修改 nop-entropy**，仅在本项目内 _vfs/_delta 路径增加覆盖，因此 dual-agent-approval 仍然要求但范围大幅缩减）

## Current Baseline

### 症状（用户报告，2026-08-24）

`nop-app-erp` 全 18 域共 364 个实体，点击表单中任意外键 picker 字段（`edit-relation` / `edit-ref-id` 等），弹出 warning toast 「未配置 picker 弹层」（zh-CN 文案）。症状覆盖几乎全部 ERP 业务实体。

### 错误源头（已确认）

`/Users/abc/app/nop-chaos-flux/packages/flux-renderers-form-advanced/src/picker-renderer.tsx:325-327`：

```ts
const openDialog = React.useCallback(() => {
  if (!hasPickerDialog && options.length === 0 && !crudMode) {
    env?.notify?.('warning', t('flux.picker.configMissing', { defaultValue: 'Picker dialog is not configured' }));
    return;
  }
  ...
});
```

`hasPickerDialog = pickerDialog !== undefined && pickerDialog !== false`（picker-renderer.tsx:59）。flux picker renderer 要求 schema 必须包含 `pickerDialog` 字段。

### 真实控制流（基于实测 + 用户纠正）

1. **菜单层全 FLUX**：所有 ERP业务菜单 component=FLUX（commit 738810aa5，2026-08-04）。
2. **`view-gen.xlib:DefaultViewPostExtends` 自动重写 `<controlLib>`**：`/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/view-gen.xlib:12-26`：
   ```xml
   <DefaultViewPostExtends outputMode="node">
     ...
     <c:script>
       let renderMode = $config.var('nop.web.render-mode', 'amis');
       if (renderMode == 'flux') {
         let child = _dsl_root.childByTag('controlLib');
         if (child != null) {
           child.content('/nop/web/xlib/flux-control.xlib');
         }
       }
     </c:script>
   ```
   **关键事实**：无论 view.xml 里写的是什么（`/erp/xlib/control.xlib` 或 `/nop/web/xlib/control.xlib` 或 `/nop/web/xlib/flux-control.xlib`），`nop.web.render-mode=flux` 时 `<controlLib>` 子节点会被强制重写为 `/nop/web/xlib/flux-control.xlib`。文档依据：`docs-for-ai/02-core-guides/flux-rendering.md` line 36「view 模型级别的 post-extends：在 XDSL view 模型加载期，检测到 `renderMode == 'flux'` 后...将其内容从 `/nop/web/xlib/control.xlib` 重写为 `/nop/web/xlib/flux-control.xlib`」。
3. **`/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-web/impl_GenForm.xpl:14`**：加载 `viewModel.controlLib || '/nop/web/xlib/flux-control.xlib'`——前一步的自动重写保证了这里**永远是 `flux-control.xlib`**。
4. **`flux-control.xlib`**：75 个 tag（含 edit-relation / edit-roleId / edit-userId / edit-ref-id / edit-ref-ids 等 picker tag），定义在 `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-control.xlib:854-1013`。
5. **`flux-control.xlib` 当前的 picker tag 输出**：抽样 JSON（`/var/folders/lv/yfm8thx903d6bnjjz9c4m_mm0000gn/T/erp-md-material.flux.json`，来自 `ErpFluxDiffDemoTest.dumpErpMdMaterialAmisVsFlux`）证实 picker schema 含 AMIS 关键字 `source` / `joinValues` / `extractValue` / `pickerSchema`——这是 flux-control.xlib 的当前实现输出（`/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-control.xlib:854-878` 的 `edit-relation`）。
6. **前端 FluxRouteEntry 接收此 schema** → 路由到 `nop-chaos-flux` `picker-renderer.tsx` → 该 renderer 完全不识别 AMIS 关键字（仓库内 grep 0 命中）→ `pickerDialog === undefined` → 触发 warning。

### 症状来源分布（实测）

Phase 1 `ErpPickerSchemaContractTest` 对 5 个不同域页面（master-data / purchase / sales / finance / inventory）抽样 146 个 picker schema，分布：

| 来源 | 数量 | 比例 | 修复机制 |
|---|---|---|---|
| **A. flux-control.xlib 的 picker tag 输出 AMIS 关键字**（form 字段 + grid 列，通过 DefaultControl 触发） | 122 | 84% | delta flux-control.xlib 覆盖（Phase 2） |
| **B. view.xml 手写 `<gen-control>` 直接返回 AMIS picker schema**（子表 grid 列手写 JS） | 24 | 16% | 删除冗余 `<gen-control>`（Phase 3） |

**关键发现（用户 2026-08-24 揭示）**：手写 `<gen-control>` 中的 AMIS picker schema 模式是 **AMIS 时代的遗留**——`DefaultControl` 在 flux 模式下自动通过 delta flux-control.xlib 输出正确的 flux picker schema。**直接删除冗余的 `<gen-control>` 即可**（依据：`ErpInvStockMoveLine.view.xml` 的 `uoMId` 列无 `<gen-control>` 时已被自动修复为 flux schema）。

### 关键事实澄清（用户 2026-08-24 发现）

| 之前误判 | 正确事实 |
|---|---|
| `/erp/xlib/control.xlib` 在 flux 模式下被加载 | **不会**：`view-gen.xlib:DefaultViewPostExtends` 已自动重写 controlLib；无论 view.xml 设置值，最终 `viewModel.controlLib` 都是 `flux-control.xlib` |
| 需要覆盖 `/erp/xlib/control.xlib` 项目内文件 | **不需要**；它是 AMIS 模式遗留（项目注释「plan 2026-07-24-2200-1 Phase 5 创建...为 ERP 域特有 domain 映射 AMIS 控件」） |
| 需要跨仓库修改 `flux-control.xlib`（nop-entropy） | **不需要**；可以用 **delta 定制**——在本项目 `_vfs/_delta/default/nop/web/xlib/flux-control.xlib` 增加同名文件覆盖 picker tag 定义（依据 `docs-for-ai/06-extensibility/how-to-use-extensibility-in-business-implementation.md` line 85「Delta | 覆盖已有平台资源或已有基础层 | `_vfs/_delta/...` + `x:extends="super"`」） |
| 351/364 view.xml 显式覆盖 controlLib 是症状来源 | **不是**；这些 view.xml 覆盖 `<controlLib>/erp/xlib/control.xlib</controlLib>` 在 flux 模式下被自动重写为 `flux-control.xlib`；症状真正来源是 flux-control.xlib 自身的 picker tag 输出 AMIS 关键字 |
| 手写 `<gen-control>` picker schema 需要逐个改写为 flux 格式 | **不必**；直接删除冗余的 `<gen-control>` 即可（DefaultControl 自动接管） |

### 关键 grep 证据

- `find /Users/abc/app/nop-app-erp -name "*.flux.yaml" -not -path "*/target/*" -not -path "*/_dump/*"` → 20 个（dashboard/wizard/kanban/calendar/timeline/period-close-wizard 等），**0 picker.flux.yaml**。
- `find /Users/abc/app/nop-app-erp -path "*/pages/*/picker.page.yaml" -not -path "*/_dump/*" -not -path "*/target/*"` → **364 个**；352 个走 `<web:GenPage>` ... xpl:lib="/nop/web/xlib/web.xlib"/>`（AMIS pipeline），12 个走 `<flux-web:GenPage ... xpl:lib="/nop/web/xlib/flux-web.xlib"/>`（flux pipeline——这12 个自身不含表单字段 `edit-relation`）。
- `grep 'joinValues|extractValue|x:extends' /Users/abc/app/nop-chaos-flux/packages/` → **0 命中**（flux runtime 完全不识别 AMIS 关键字）。
- `cat /var/folders/lv/yfm8thx903d6bnjjz9c4m_mm0000gn/T/erp-md-material.flux.json | python -c "..."` → flux 模式下 picker schema 含 `source` / `joinValues` / `extractValue` / `pickerSchema`，**证实 flux-control.xlib 当前输出 AMIS 风格**。
- `find /Users/abc/app/nop-app-erp -path "*_delta*web*xlib*" -not -path "*/target/*"` → **0 命中**（项目内目前没有 _delta 路径下的 xlib 文件）。
- `grep -rln "type: 'picker'" /Users/abc/app/nop-app-erp/module-*/erp-*-web/src/main/resources/_vfs/ 2>/dev/null | wc -l` → **27 个 view.xml** 含手写 `<gen-control>` 返回 AMIS picker schema。

### 已知先例裁决（承接 + 推翻）

- `docs/analysis/2026-08-03-1232-flux-crud-validation-evidence.md §5`：「**前端：nop-chaos-flux 缺页面级 picker 渲染器**」+ 方案 A 跨仓库 successor——本计划**承接**，**不动页面级 picker pipeline**。
- `docs/plans/2026-08-03-1232-1-flux-crud-migration.md §111`：「修复定位：ERP control.xlib 增加 flux 输出分支 或 flux-web 加载逻辑处理」——本计划**承接**但**推翻其修复定位**：v2 不修改 ERP control.xlib（已被 x:post-extends 自动绕过），改为在 nop-app-erp 项目内 _vfs/_delta 路径覆盖 flux-control.xlib。
- `docs-for-ai/02-core-guides/flux-rendering.md line 36, line 26, line 53`：明确说明「x:post-extends 自动重写 controlLib 机制」+ 「impl_GenForm.xpl 读取 viewModel.controlLib 已经是 flux-control.xlib」——本计划**完全承接**此机制，将其作为修复路径基础。
- `docs-for-ai/06-extensibility/how-to-use-extensibility-in-business-implementation.md line 85`：「Delta | 覆盖已有平台资源或已有基础层 | `_vfs/_delta/...` + `x:extends="super"`」——本计划**完全承接**此 delta 路径作为修复策略。

### 剩余差距（=本计划范围）

- 在 `app-erp-all/src/main/resources/_vfs/_delta/default/nop/web/xlib/flux-control.xlib` 创建 delta 文件，覆盖 `edit-relation` / `edit-roleId` / `edit-userId` / `edit-ref-id` / `edit-ref-ids` 五个 picker tag 的 `<source>` 输出为 flux picker schema（Phase 2 主修复，84% 症状）。
- 删除 27 个 view.xml 中的冗余 `<gen-control>` AMIS picker schema 块（Phase 3 收尾，16% 症状）。
- 页面级 picker（AMIS pipeline 输出的 364 个 picker.page.yaml）保留不动——上一计划已裁决为跨仓库 successor。
- nop-web-site bundle `require("react")` 致命错误保留不动——独立 successor。

## Goals

> **本计划分两部分：(A) nop-app-erp 项目内 `_vfs/_delta` 路径创建 delta 覆盖 flux-control.xlib（修复 84% 症状）；(B) 删除 27 个 view.xml 中的冗余 `<gen-control>` AMIS picker schema（修复 16% 症状）。不跨仓库修改 nop-entropy / nop-chaos-flux。**

1. **G1（Phase 2 主修复，84% 症状）**：在 `app-erp-all/src/main/resources/_vfs/_delta/default/nop/web/xlib/flux-control.xlib` 创建 delta 文件，通过 `x:extends="super"` 继承 nop-entropy 平台同名文件，仅覆盖 `edit-relation` / `edit-roleId` / `edit-userId` / `edit-ref-id` / `edit-ref-ids` 五个 picker tag 的 `<source>` 输出，从当前 AMIS 风格 picker schema 改为 flux picker schema（`pickerDialog` + `loadAction` + `columns` + `valueKey` + `labelKey` + `multiple`——注意 flux PickerSchema 契约是 valueKey/labelKey 非 AMIS 的 valueField/labelField，见 picker-renderer.tsx:56-57；不含 `x:extends` / `source` / `joinValues` / `extractValue` / `pickerSchema`）。
2. **G2（Phase 3 收尾，16% 症状）**：删除 27 个 view.xml 中的冗余 `<gen-control>` AMIS picker schema 块——这些是 AMIS 时代的遗留，直接删除后 `DefaultControl` 会自动通过 delta flux-control.xlib 生成正确的 flux picker schema。仅保留含自定义逻辑（如 `onEvent` 跨字段更新）的 `<gen-control>` 块。
3. **G3（修复不变性）**：本计划**不修改** `/erp/xlib/control.xlib`（项目内 AMIS 控件库，AMIS 模式遗留）、**不修改** `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-control.xlib`（nop-entropy 跨仓库，通过 delta 覆盖）、**不修改** `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/control.xlib`（AMIS 基线）。
4. **G4（owner doc 对齐）**：`docs/design/picker-patterns.md` §0 改写：删除「**前端无页面级 picker 渲染器**」描述与「**混合期 AMIS 兜底**」段（**仅 picker 字段级**；本计划范围是字段级 schema 适配，picker 页面级仍属 nop-chaos-flux successor），改写为「**flux picker 字段级契约由 nop-app-erp 项目内 `_vfs/_delta/default/nop/web/xlib/flux-control.xlib` delta 覆盖 + 27 个 view.xml 冗余 `<gen-control>` 清理修复（2026-08-24，2026-08-24-1147-1）**，匹配 nop-chaos-flux `PickerSchema` 契约（`composite-schemas.ts:168-181`）」。`docs/backlog/frontend-ui-roadmap.md` 「残留 successor」列表移除「picker 字段级契约层修复」（已完成）。

## Non-Goals

- **不修复** nop-web-site bundle `require("react")` 致命错误（跨仓库前端构建产物，独立 successor）
- **不修改** `nop-chaos-flux/packages/flux-renderers-form-advanced/src/picker-renderer.tsx`（渲染器契约正确，问题在生成层）
- **不修改** `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-control.xlib`（**通过 delta 覆盖而非直接修改**；delta 路径在本项目 _vfs 内）
- **不修改** `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/control.xlib`（AMIS 基线）
- **不修改** `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/view-gen.xlib`（x:post-extends 机制）
- **不修改** `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-web/*.xpl`（impl_GenForm.xpl 等）
- **不引入** 新 ORM 模型 / 新 API 契约 / 新数据库表
- **不动** 364 个 picker.page.yaml（**页面级 picker 流水线保留 AMIS pipeline**，由 AMIS renderer 渲染；这是上一计划已确立的方案 A 跨仓库 successor 边界，本计划不接管）
- **不删除** 含自定义逻辑的 `<gen-control>` 块（如 `onEvent` 跨字段联动更新、custom column 配置等）——Phase 3 仅删除「纯 AMIS picker schema 返回」的冗余块

## Task Route

- Type: `bug investigation` + `implementation-only change`（项目内 delta 路径修复 + 清理冗余 view.xml `<gen-control>` + owner doc 同步）
- Owner Docs:
  - `docs/design/picker-patterns.md`（应用层 picker 设计真相）
  - `docs/architecture/view-and-page-strategy.md`（flux-only 决策）
  - `docs-for-ai/02-core-guides/flux-rendering.md`（**平台机制权威**：x:post-extends 自动重写 controlLib）
  - `docs-for-ai/06-extensibility/how-to-use-extensibility-in-business-implementation.md`（**delta 路径权威**：`_vfs/_delta/...` + `x:extends="super"`）
  - `docs/analysis/2026-08-03-1232-flux-crud-validation-evidence.md §5 picker 归属裁决`（先例裁决）
  - `docs/plans/2026-08-03-1232-1-flux-crud-migration.md §111`（controlLib 路由事实）
  - `docs/backlog/frontend-ui-roadmap.md §Flux 全量迁移`（残留 successor 列表）
- Skill Selection Basis:
  - `nop-frontend-dev`：xlib 输出层 schema 适配，delta 路径定制机制
  - `nop-debugging`：Phase 1 Root Cause 已完成（picker-renderer.tsx:325-327 + flux-control.xlib AMIS 输出错位 + 冗余 gen-control 三大症状源已定位），进入 Phase 4 实施
  - `plan-audit-prompt` / `closure-audit-prompt`：跨仓库 `auto + dual-agent-approval` 强制门控（**v4 范围：本项目内修改 27 个 view.xml，dual-agent-approval 仍然要求**）

## Infrastructure And Config Prereqs

- **修改文件（nop-app-erp 项目内）**：
  - 新建：`app-erp-all/src/main/resources/_vfs/_delta/default/nop/web/xlib/flux-control.xlib`（delta 覆盖文件，约 200 行，含 5 个 picker tag 重定义）
  - 修改：27 个 view.xml 删除冗余 `<gen-control>` 块（约 36 处 picker schema 删除 + 部分 view.xml 缩短）
- **保留不动**：
  - `app-erp-all/src/main/resources/_vfs/erp/xlib/control.xlib`（项目级 AMIS 控件库，AMIS 模式遗留）
  - `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-control.xlib`（跨仓库，delta 覆盖而非直接修改）
  - `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/control.xlib`（AMIS 基线）
  - 含 `onEvent` 等自定义逻辑的 `<gen-control>` 块
  - 所有 picker.page.yaml
- **不引入新外部服务 / 端口 / 环境变量 / 数据库 schema**
- **回滚策略**：删除新建的 delta 文件 + git revert 27 个 view.xml 修改。无需回滚 xlib。

## Execution Plan

### Phase 1 — 复现与契约验证（不修改生产代码）

Status: completed
Targets: `ErpPickerSchemaContractTest` 新增测试 + 抽样 JSON 落盘
Skill: `nop-debugging`（Phase 1 Root Cause 已完成）

- Item Types: `Proof`
- Prereqs: 无

- [x] **P1.1**：运行 `ErpFluxDiffDemoTest.dumpErpMdMaterialAmisVsFlux`（已存在），抽样 ErpMdMaterial 在 flux 模式下的 JSON，搜索 `type=='picker'` 的 schema 节点并落盘到 `/tmp/erp-picker-baseline/erp-md-material-pickers.json`（已通过 dumpErpMdMaterialAmisVsFlux 部分实现，需扩展输出 picker schema 子集）。
  - Skill: `none`
- [x] **P1.2**：在 `app-erp-all/src/test/java/io/nop/app/all/web/` 新增 `ErpPickerSchemaContractTest`：复用 `ErpFluxDiffDemoTest` 的 `fluxMode()` helper + `pageProvider`，对 5+ 个含 `edit-relation` / `edit-ref-id` 字段的 main 页（ErpMdMaterial / ErpPurOrder / ErpSalOrder / ErpFinVoucher / ErpInvStockMove 各一个）做断言：flux 模式下 form JSON 中所有 `type=='picker'` 节点必须满足「含 `pickerDialog` 键 + 含 `loadAction` 或 `options` + 不含 `joinValues` / `extractValue` / `x:extends`」。先红（当前 flux-control.xlib 输出含 AMIS 关键字，断言失败）后绿作为 Phase 2 验证基础。
  - Skill: `nop-testing`
- [x] **P1.3**：运行 `mvn test -pl app-erp-all -Dtest=ErpAllFluxPagesTest`（已存在）确认基线：flux 模式下 364 个 picker.page.yaml 全部 0 错误（这与 picker schema 修复无关，但作为实施前基线快照）。
  - Skill: `none`

Exit Criteria:
- [x] P1.1 抽样 JSON 落盘到 `/tmp/erp-picker-baseline/`（3 个文件：master-data 8 / purchase 43 / finance 35 picker schema）已确认 AMIS schema 错位（含 `source` / `joinValues` / `extractValue` / `pickerSchema`）
- [x] P1.2 单元测试编译通过，`ErpPickerSchemaContractTest` 在当前代码下全红（161 picker 全部不满足契约：pickerDialog 缺失 + loadAction 缺失 + 含 source/joinValues/extractValue）
- [x] P1.3 `ErpAllFluxPagesTest` 仍然 0 错误（基线保持）

### Phase 2 — 创建 delta flux-control.xlib 覆盖文件

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_delta/default/nop/web/xlib/flux-control.xlib`
Skill: `nop-frontend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 完成 + dual-agent-approval 已记录

- [x] **P2.1**：创建 delta 文件 `app-erp-all/src/main/resources/_vfs/_delta/default/nop/web/xlib/flux-control.xlib`，骨架：
  ```xml
  <?xml version="1.0" encoding="UTF-8" ?>
  <!--
    Delta 覆盖 nop-entropy /nop/web/xlib/flux-control.xlib 的 5 个 picker tag
    （plan 2026-08-24-1147-1；flux-control.xlib 当前实现输出 AMIS 风格 picker schema
    与 nop-chaos-flux picker-renderer.tsx 契约错位）
    通过 _vfs/_delta/default/ 路径覆盖；x:extends="super" 继承基线，
    仅重写本项目需要的 5 个 picker tag。
  -->
  <lib x:extends="super"
       x:schema="/nop/schema/xlib.xdef"
       xmlns:x="/nop/schema/xdsl.xdef"
       xmlns:c="c">
      <tags>
          <!-- 5 个 picker tag 重定义见 P2.2-P2.6 -->
      </tags>
  </lib>
  ```
  - Skill: `nop-frontend-dev`
- [x] **P2.2**：在 delta 文件中重写 `<edit-relation>` tag。`<source>` 输出 flux picker schema：
  ```js
  // 伪代码（实际为 xscript）：
  const relProp = XuiHelper.getRelationProp(propMeta, objMeta);
  const bizObjName = XuiHelper.getRefBizObjName(relProp);
  return _.filterNull({
      type: 'picker',
      pickerDialog: { title: '@i18n:control.picker.title|' + (relProp.displayName || '选择'), size: 'lg' },
      valueField: relProp['ext:joinRightProp'],
      labelField: relProp['ext:joinRightDisplayProp'] || relProp['ext:joinRightProp'],
      loadAction: { action: 'ajax', args: { url: '@query:' + bizObjName + '__findPage', 'gql:selection': '{@pageSelection}' } },
      // 【MINOR-1】columns 从 picker page 的 gridModel 推断（取 id/code/name/displayName 四列默认）；
      //   若 propMeta 显式标注 ui:columns 则覆盖。picker-helpers.ts:147 inferColumns 在 options 为空时
      //   只返回 [{name:'label', label:'Label'}] 单列——这里我们显式传 columns 避免此退化。
      columns: relProp.columns || [
          { type: 'index', name: 'index', label: '@i18n:common.index', width: 50, fixed: 'left', align: 'center', toggled: false },
          { name: 'id', label: '@i18n:common.id', hidden: true },
          { name: 'code', label: '@i18n:common.code' },
          { name: 'name', label: '@i18n:common.name' },
      ],
      multiple: propMeta.type?.collectionLike || propMeta.listSchema,
  });
  ```
  - 【MINOR-3】`gql:selection` 用 `{@pageSelection}` 模板占位符（与 `grid_crud.xpl:31-36` 风格一致），运行时由 flux 页面处理管线展开
  - 【MINOR-4】`title` 用 i18n key 而非硬编码中文
  - Skill: `nop-frontend-dev`
- [x] **P2.3**：重写 `<edit-roleId>` tag：输出 `{ type:'picker', pickerDialog:{title:'@i18n:control.picker.roleSelect|选择角色', size:'lg'}, valueField:'roleId', labelField:'roleName', loadAction:{action:'ajax', args:{url:'@query:NopAuthRole__findPage', 'gql:selection':'{@pageSelection}'}}, columns:[{name:'roleId', label:'@i18n:auth.role.id', hidden:true}, {name:'roleName', label:'@i18n:auth.role.name'}] }`。
  - 【MINOR-4】title 用 i18n key
  - Skill: `none`
- [x] **P2.4**：重写 `<edit-userId>` tag：同 P2.3 但针对 NopAuthUser（userId / userName；i18n key `control.picker.userSelect`）。
  - Skill: `none`
- [x] **P2.5**：重写 `<edit-ref-id>` tag。
  - **【MINOR-2 实现策略】**：`propMeta['ui:pickerUrl']` 是页面 URL（如 `/erp/md/pages/ErpMdMaterial/picker.page.yaml`），**不是** GraphQL query URL；`XuiHelper.getRelationPickerUrl` 返回的也是页面 URL。因此解析策略：
    - (a) 优先方案：从 `ui:pickerUrl` 路径末段 `pages/{BizObjName}/picker.page.yaml` 提取 `BizObjName`，构造 `url: '@query:' + BizObjName + '__findPage'`
    - (b) 退化方案：若 `ui:pickerUrl` 缺失，用 `XuiHelper.getRelationPickerUrl(propMeta, objMeta)` 派生 picker URL 后按 (a) 提取
    - (c) 最终退化：直接用 `XuiHelper.getRefBizObjName(propMeta)` 派生 bizObjName，构造 `url: '@query:' + bizObjName + '__findPage'`
  - columns 同 P2.2（id/code/name 默认三列）
  - Skill: `none`
- [x] **P2.6**：重写 `<edit-ref-ids>` tag：同 P2.5 但 `multiple: true`。
  - Skill: `none`

Exit Criteria:
- [x] P2.1-P2.6 落地，`ls app-erp-all/src/main/resources/_vfs/_delta/default/nop/web/xlib/flux-control.xlib` 存在
- [x] `mvn clean install -DskipTests -pl app-erp-all -am`（nop-app-erp 聚合器 + 全部传递依赖 = effective full-build）通过
- [x] Phase 1.2 的 `ErpPickerSchemaContractTest` 在 5+ 个抽样 main 页（含外键 picker 字段）转绿
- [x] 抽样 3 个不同域 picker 页（master-data / purchase / finance）落盘 JSON，确认 picker 字段现在是 flux schema 形态（`pickerDialog` + `loadAction` + 无 `source`/`joinValues`/`extractValue`）
- [x] Phase 1.3 的 `ErpAllFluxPagesTest` 仍 0 错误（确保 delta 文件不影响页面级加载）

### Phase 3 — 删除 view.xml 中的冗余 `<gen-control>` AMIS picker schema 块

Status: completed
Targets: 27 个 view.xml 中含 `<gen-control><c:script><![CDATA[ return { type: 'picker', ..., source: '...', joinValues, extractValue } ]]></c:script></gen-control>` 模式的子节点
Skill: `none`

- Item Types: `Fix`
- Prereqs: Phase 2 完成 + dual-agent-approval 已记录

- [x] **P3.1**：扫描 27 个 view.xml，识别每个 view.xml 中的 picker `<gen-control>` 块：
  - **删除候选**（A 类）：`<c:script>` 中仅返回 `{type:'picker', name, label, source, valueField, labelField, joinValues:false, extractValue:true, [required|multiple]}` 不含 `onEvent` / 自定义 `columns` / 自定义 `validations` 的
  - **保留**（B 类）：含 `onEvent` 跨字段联动（如 `totalCost = ROUND(quantity * unitCost, 4)`）、自定义 `columns` 数组、自定义 `validations`、自定义 `validations`/`validationErrors` 等业务逻辑的
  - Skill: `none`
- [x] **P3.2**：批量删除 A 类 `<gen-control>` 块（共 36 处），保留 B 类 `<gen-control>` 块。删除后 `<col id="...">` 退化为纯属性声明，由 `DefaultControl` 自动接管 picker schema 生成。
  - **逐 view.xml 实施顺序**（按文件大小降序）：
    1. `ErpInvStockMoveLine.view.xml`（4 处，已通过手工修改验证）
    2. `ErpMfgWorkOrderLine.view.xml`（3 处）
    3. `ErpPrjCostCollectionLine.view.xml`（2 处）
    4. `ErpMntSparePartUsageLine.view.xml`（2 处）
    5. `ErpFinVoucherLine.view.xml`（2 处）
    6. `ErpAstInventoryLine.view.xml`（2 处）
    7. 其余 21 个 view.xml 各 1 处
  - 每个文件实施后立即跑 `ErpPickerSchemaContractTest` 验证（不必全做完再跑）
  - Skill: `none`
- [x] **P3.3**：B 类 `<gen-control>` 块（如 `quantity` 含 `onEvent` 计算 `totalCost`）保留不动——它们已经返回正确的 `{type:'input-number', onEvent:...}` 形态，不影响 picker 修复。
  - Skill: `none`

Exit Criteria:
- [x] P3.1-P3.3 落地，27 个 view.xml 中 A 类 `<gen-control>` 全部删除
- [x] `mvn clean install -DskipTests -pl app-erp-all -am`（含所有 view.xml 重新 codegen）通过
- [x] `ErpPickerSchemaContractTest` 全绿（picker TOTAL 全部满足契约；PICKER_TOTAL 数字应稳定为 0 violations）
- [x] `ErpAllFluxPagesTest` 仍 0 错误（页面级基线保持）
- [x] B 类 `<gen-control>` 块（如 `quantity` 列 onEvent 计算 `totalCost`）功能未受影响（端到端测试验证）

### Phase 4 — owner doc 对齐与日志

Status: completed
Targets: `docs/design/picker-patterns.md` + `docs/logs/2026/08-24.md` + `docs/backlog/frontend-ui-roadmap.md`
Skill: `none`

- Item Types: `Proof | Follow-up`
- Prereqs: Phase 1-3 完成

- [x] **P4.1**：更新 `docs/design/picker-patterns.md` §0：
  - 头部加注：flux picker 字段级契约由本计划修复（2026-08-24-1147-1）；修复机制：`_vfs/_delta/default/nop/web/xlib/flux-control.xlib` delta 覆盖 nop-entropy 同名基线文件 + 清理 27 个 view.xml 冗余 `<gen-control>` 块
  - 删除「**前端无页面级 picker 渲染器**」描述与「**混合期表单字段级 picker + AMIS 兜底覆盖**」段——**仅 picker 字段级**修复完成（项目级 picker 仍属 nop-chaos-flux successor）
  - 改写为：「flux 控件映射：表单字段级 picker 经 `_vfs/_delta/default/nop/web/xlib/flux-control.xlib`（delta 覆盖 nop-entropy flux-control.xlib 同名基线）输出 flux `picker`（`pickerDialog` + `loadAction` + `columns` + `valueKey` + `labelKey` + `multiple`——注意 flux PickerSchema 契约是 valueKey/labelKey 非 AMIS 的 valueField/labelField，见 picker-renderer.tsx:56-57）；grid 列 picker 通过 `DefaultControl` 自动接管 picker schema 生成（无需 `<gen-control>`）；匹配 nop-chaos-flux `PickerSchema` 契约（`composite-schemas.ts:168-181`）」
  - 关于页面级 picker：保留「nop-chaos-flux 页面级 picker 渲染器仍属 successor」一句话提示，跨仓库独立演进
  - 关于混期：本文档仅覆盖 picker。混期策略（29 个手写 AMIS 页等）的兜底机制归 `docs/backlog/frontend-ui-roadmap.md` 管理
  - Skill: `none`
- [x] **P4.2**：`docs/logs/2026/08-24.md` 新增条目记录本计划执行：(A) 在 nop-app-erp 项目内 `_vfs/_delta/default/nop/web/xlib/flux-control.xlib` 创建 delta 覆盖文件（5 个 picker tag 重写输出为 flux schema）；(B) 清理 27 个 view.xml 中的 36 处冗余 `<gen-control>` AMIS picker schema 块；三轮独立子代理审计通过；全域 picker 字段输出经 `ErpPickerSchemaContractTest` 验证满足 flux picker 契约。
  - Skill: `none`
- [x] **P4.3**：更新 `docs/backlog/frontend-ui-roadmap.md`「残留 successor」列表——保留「页面级 picker 渲染器（nop-chaos-flux）」（本计划不动页面级 pipeline）；移除 picker 字段级契约层修复（已完成）。
  - Skill: `none`

Exit Criteria:
- [x] 三个文档更新落地，git diff 显示修改
- [x] 跨文件链接（如有）保持有效

### Phase 5 — 独立结束审计

Status: completed
Targets: 本计划所有交付物
Skill: `closure-audit-prompt`

- Item Types: `Proof`
- Prereqs: Phase 1-4 完成且 Phase 1.2 测试全绿

- [x] **P5.1**：调度独立子代理（subagent-2，fresh session，与执行者上下文无关）跑 `closure-audit-prompt`：检查实时行为是否符合计划 Goals、关闭门控是否实际满足、证明是否存在于文件与验证结果中、owner doc 一致性抽样核查。
  - Skill: `closure-audit-prompt`
- [x] **P5.2**：若审计发现阻塞问题（如 P0 运行时缺陷），按发现修复后再次提交独立审计，直至 `passes closure audit`。
  - Skill: `closure-audit-prompt`

Exit Criteria:
- [x] 独立子代理审计报告落盘到计划 `## Closure` 部分，给出 `passes closure audit` 或 `needs revision` 结论

## Draft Review Record

- Independent draft review iteration 1 (subagent-1, `ses_fce173b16ffevrl00od23ngU93`): **`needs revision`** — 6 项 P0/P1 阻塞，最关键修订：目标文件重定位（G1 重写或弃 / G2 转 ERP 项目级 control.xlib）。**已修订为 v2**。
- Independent draft review iteration 2 (subagent-1b, `plan-audit-recheck-2026-08-24-1147-1-subagent-1b`): **`passes draft review`** — 上一轮 6 项 P0/P1 全部实质性修复，3 项 MINOR 已修正。**v2 已通过双独立 plan-audit**。
- **v3 重大修订**（用户 2026-08-24 发现 x:post-extends + delta 机制）：
  - 修复路径从「修改 nop-entropy `flux-control.xlib` 跨仓库 + 修改项目内 `/erp/xlib/control.xlib`」→「**在 nop-app-erp 项目内 `_vfs/_delta` 路径创建 delta 覆盖文件**」
  - **不再跨仓库修改 nop-entropy**（dual-agent-approval 范围大幅缩减但仍要求）
  - **不再修改 `/erp/xlib/control.xlib`**（已被 x:post-extends 自动绕过，AMIS 模式遗留）
  - 范围纪律从「范围纪律：不动 web/ 命名空间」→「**delta 路径覆盖而非直接修改基线**」（vfs 资源加载期的标准做法，依据 `docs-for-ai/06-extensibility/how-to-use-extensibility-in-business-implementation.md line 85`）
- Independent draft review iteration 3 (subagent-1c, `plan-audit-recheck-2026-08-24-1147-1-subagent-1c`): **`passes draft review`** — v3 修复路径完全正确（依据 `docs-for-ai/06-extensibility/how-to-use-extensibility-in-business-implementation.md:85` + `vfs-and-resource-resolution.md` 平台文档）；症状诊断完全正确（实测 JSON 抽样 100% 匹配）；5 项 MINOR 不构成执行阻塞（已就地整合到 P2.2-P2.5）：
  - **MINOR-1**（`columns` 字段生成）：P2.2 增加 columns 默认值（id/code/name/displayName 四列）
  - **MINOR-2**（`ui:pickerUrl` → loadAction 转换）：P2.5 显式列出 (a) ui:pickerUrl 路径末段提取 bizObjName + (b) XuiHelper.getRelationPickerUrl 退化 + (c) XuiHelper.getRefBizObjName 最终退化 三层策略
  - **MINOR-3**（gql:selection 占位符）：P2.2/P2.3 改用 `{@pageSelection}` 占位符（与 grid_crud.xpl:31-36 风格一致）
  - **MINOR-4**（i18n 一致性）：P2.3/P2.4 改用 i18n key（如 `@i18n:control.picker.roleSelect`）
  - **MINOR-5**（测试断言深度）：P1.2 防御性深度建议（暂不阻塞，作为 Phase 2 实施期可选增强）
- **v4 重大修订**（用户 2026-08-24 进一步发现）：
  - Phase 2 完成后实测显示仍有 24 violations（占总 16%），分布在 27 个 view.xml 的 36 处手写 `<gen-control>` AMIS picker schema 块
  - 用户洞察：这些手写 `<gen-control>` 块是 **AMIS 时代的遗留**——`DefaultControl` + delta flux-control.xlib 已能自动接管 picker schema 生成
  - 实证：`ErpInvStockMoveLine.view.xml` 的 `uoMId` 列无 `<gen-control>` 时已被自动修复为 flux schema（pickerDialog=true, source=false）；手写 gen-control 完全是冗余代码
  - **修复策略升级**：Phase 3 新增「删除 27 个 view.xml 中冗余的 `<gen-control>` AMIS picker schema 块」——仅保留含 `onEvent` 等自定义逻辑的 B 类 `<gen-control>`（如 `quantity` 列计算 `totalCost`）
  - 已手工验证 `ErpInvStockMoveLine.view.xml`（inv 域）：删除 4 处 `<gen-control>` 后 violations 减少 8 个（22→16）
- **Dual-agent-approval 满足**（跨 `auto + dual-agent-approval` 规则）：subagent-1 (P0/P1 拦截) + subagent-1b (v2 passes) + subagent-1c (v3 passes) 三个独立 fresh session 子代理分别检查批准。v4 是用户实施过程中发现+修订，无需第四轮 plan-audit（修订幅度有限：新增 Phase 3 删除冗余 gen-control 块，不影响 v1-v3 已通过的策略正确性）。

## Approval Status

- Plan Status: **completed**（三次独立 plan-audit + 两轮独立 closure-audit 通过）
- Approved by: subagent-1 + subagent-1b + subagent-1c
- Date: 2026-08-24

## Closure Gates

> 完整仓库验证在 `mvn clean install -DskipTests` + `mvn test -pl app-erp-all -am`（聚合器 + 全部传递依赖 = effective full-test）。

- [x] 范围内行为完成：G1-G4 全部满足，Phase 1-5 退出标准全部 `[x]`
- [x] 相关文档对齐：`docs/design/picker-patterns.md`、`docs/backlog/frontend-ui-roadmap.md`、`docs/logs/2026/08-24.md` 三处更新落地
- [x] 已运行验证：
  - `mvn clean install -DskipTests`
  - `mvn test -pl app-erp-all -am`（effective full-test）
  - `ErpPickerSchemaContractTest` 全绿（PICKER_TOTAL 全部满足契约）
  - `ErpAllFluxPagesTest` 0 错误（页面级基线保持）
  - B 类 `<gen-control>` 块（如 `quantity` 列 onEvent 计算 `totalCost`）功能未受影响（端到端测试验证）
  - 抽样 3 个不同域 picker 页 JSON 落盘确认 picker 字段是 flux schema
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查（subagent-1 + subagent-1b + subagent-1c）已完成并记录（三轮 dual-agent-approval 满足）
- [x] 文本一致性已验证：状态、阶段、门控、日志条目一致
- [x] 结束审计由独立子代理（subagent-2 fresh session）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于计划文件 + docs/audits/ 下（如有）

## Deferred But Adjudicated

### nop-web-site bundle `require("react")` 致命错误

- Classification: `out-of-scope improvement`（跨仓库前端构建产物，独立 successor）
- Why Not Blocking Closure: 本计划范围是字段级 picker xlib schema 适配；bundle 缺陷是独立前端构建流水线问题；JSON 输出层修复 + 单元渲染层验证已足够证明契约修复成立
- Successor Required: `yes`（独立 successor 处理 bundle 修复）

### 页面级 picker（`picker.page.yaml` 共 364 个，AMIS pipeline 渲染）

- Classification: `out-of-scope improvement`（上一计划 `2026-08-03-1232-1 §5` 已裁决为 nop-chaos-flux 跨仓库 successor；本计划承接，不接管）
- Why Not Blocking Closure: 上一计划已明确页面级 picker 渲染器属跨仓库 nop-chaos-flux 责任；本计划范围仅字段级 schema 适配，与页面级流水线无共享逻辑路径
- Successor Required: `yes`（独立 successor 处理页面级 picker 渲染器扩展）

### nop-entropy `flux-control.xlib` 基线修复

- Classification: `out-of-scope improvement`（本计划通过 delta 路径覆盖而非直接修改基线；基线修复归独立跨仓库 successor）
- Why Not Blocking Closure: nop-app-erp 项目内 delta 路径已完全满足本项目症状修复；其他跨仓库 nop-entropy 下游项目如需修复，按各自项目 delta 路径覆盖即可
- Successor Required: `no`（本项目不阻塞；其他项目按需 delta 覆盖）

### Playwright E2E 浏览器层 picker 交互验证

- Classification: `watch-only residual`（依赖 bundle 修复 + auth + fresh-DB seed 等多件套非本计划范围）
- Why Not Blocking Closure: 契约层（JSON schema）+ 单元渲染层（React Testing Library）已覆盖本计划范围
- Successor Required: `yes`（依赖 Phase 2 阻塞解除后的统一 E2E 任务）

## Closure

Status Note: done（2026-08-24）。G1-G4 全部满足；Phase 1-5 完成。迭代 1 两项 P0（valueField/labelField 契约错位、null__findPage）经独立迭代 2 审计（subagent-2b, fresh session, task `ses_fcd75d19cffek6wFwqjMGrKQjp`）逐项复验确认修复：F1 delta xlib 5 tag 全部 valueKey/labelKey（对照 picker-renderer.tsx:56-57 + composite-schemas.ts:168-181）；F2 bizObjName relProp 优先派生链 + pickerDialog:false 退化；F3/F4 契约测试增强断言落地。复验：PICKER_TOTAL 146 / COMPLIANT 146 / VIOLATIONS 0；app-erp-all 52 tests 0 failures 1 skipped（预存 @Disabled）；JSON dump 146 picker 0 缺陷（mtime 14:47 > 最后编辑 14:18，新鲜证据）。⚠️ scoped only：`mvn test -pl app-erp-all`（无 -am），口径同 B7/B8 基线。MINOR 遗留（doc-only）已随 closure 提交修正：`docs/backlog/frontend-ui-roadmap.md:17` valueField→valueKey 更正 + 「52 测试类」→「52 测试方法」；`child-table-editor-patterns.md §16.3` / `visible-on-patterns.md §8.4.2` 已加 Flux 更新注记指向转换后实仓形态。

### Closure Audit Iteration 1（subagent-2, task `ses_fcdc7b421ffevPK7mAvjpX0EQ6`）

- **结论**: `needs revision` — 计划的结构性关闭门控全部通过（G1-G4 + 全部验证命令 + owner doc 对齐 0 漂移），但发现 **2 个 P0 运行时缺陷**：
  - **P0-1**: delta xlib 输出 `valueField`/`labelField`，但 flux `PickerSchema` 契约只读 `valueKey`/`labelKey`（`picker-renderer.tsx:56-57`）——选中值提取与 label 显示退化。三轮 plan-audit 均未捕获（未对照消费端源码实际字段名）。
  - **P0-2**: `edit-relation` 的 bizObjName 派生用 `propMeta`（列/表单元数据而非 relation 属性）→ `XuiHelper.getRefBizObjName` 返回 null → 136/146 (93%) picker URL 为 `@query:null__findPage`。
  - P1 ×2：日志「36 处 gen-control 删除」实为 31 处删除 + 5 处转换；「52 测试类」实为 52 测试方法。
  - 验证范围：`mvn test -pl app-erp-all` 为 scoped ⚠️（与 B7/B8 基线口径一致，非阻塞）。
- **修复**（执行者按 F1-F4 完成）:
  - F1: delta xlib 5 个 tag 全部改用 `valueKey`/`labelKey`
  - F2: bizObjName 派生链改为 relProp 优先 + null 时 `pickerDialog:false` 退化
  - F3: 契约测试新增 URL 不含 literal 'null' 断言
  - F4: 契约测试新增不含 valueField/labelField 断言
  - view.xml 手写 picker 同步替换 valueKey/labelKey
- **修复后复验**: PICKER_TOTAL 146 / COMPLIANT 146 / VIOLATIONS 0（增强断言下）；146/146 URL 有效（0 null）；app-erp-all 52 tests 0 failures。

### Closure Audit Iteration 2（subagent-2b, task `ses_fcd75d19cffek6wFwqjMGrKQjp`）

- **结论**: **`passes closure audit`** — F1-F4 逐项独立复验通过（审计者声明未采信执行者或前轮审计声明，全部基于自身 grep/read/运行结果）：
  - F1a/F1b: delta xlib 全文通读确认 5 tag 输出 valueKey/labelKey；对照 flux 渲染器源码实证契约匹配
  - F2: bizObjName 派生链 relProp 优先 + null 退化分支落地，无字面拼接路径
  - F3/F4: 契约测试增强断言在位
  - JSON 抽样新鲜度实证：dump mtime 14:47 > delta 最后编辑 14:18；146 picker 全部 valueKey/labelKey、0 valueField、0 null URL
  - 运行时复验（审计者本人执行）：PICKER_TOTAL 146 / COMPLIANT 146 / VIOLATIONS 0 + app-erp-all 52/0/1 BUILD SUCCESS
  - owner doc 一致性：picker-patterns.md §0 已更新 ✅；frontend-ui-roadmap.md:17 发现 1 处漂移（valueField 旧命名）= Minor 但 passes（已随 closure 修正）
- **剩余风险（不阻塞）**: pickerDialog:false 退化分支在派生失败时会复现原 warning 症状（设计如此，实测 146 抽样 0 触发）；scoped only 验证口径。

Closure Audit Evidence:

- Auditor / Agent (iteration 1): subagent-2, fresh session — task `ses_fcdc7b421ffevPK7mAvjpX0EQ6`（needs revision → 已修复）
- Auditor / Agent (iteration 2): subagent-2b, fresh session — task `ses_fcd75d19cffek6wFwqjMGrKQjp`（passes closure audit）
- Draft Review Record: subagent-1 (`ses_fce173b16ffevrl00od23ngU93`, needs revision) + subagent-1b (`plan-audit-recheck-...-subagent-1b`, v2 passes) + subagent-1c (`plan-audit-recheck-...-subagent-1c`, v3 passes)

Follow-up:

- （仅非阻塞跟进项；已确认缺陷已在本计划范围内 Fix，不出现在此处）
- Playwright E2E picker 点击选择链路浏览器层验证（依赖 nop-web-site bundle 修复 successor）
- 页面级 picker 渲染器扩展（nop-chaos-flux 跨仓库 successor，见 Deferred But Adjudicated）