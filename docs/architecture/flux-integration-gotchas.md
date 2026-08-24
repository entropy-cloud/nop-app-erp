# Flux 集成已知设计陷阱（Gotchas Catalog）

> 维护目的：记录 nop-app-erp 与 nop-chaos-flux 集成过程中发现的"看起来是 bug 实则是设计怪味"的坑，避免后续开发者重复踩坑。
>
> 修订规则：每条目含「症状 / 根因 / 现行 workaround / 推荐修复（待上游）」四段；推荐修复需要跨仓库 plan + 双 agent 批准，不可直接修改。

## G-001 `CrudQueryFormConfig.mode` 不控制 label 位置（✅ 已修复，2026-08-25）

> **状态**：上游修复已落地（nop-chaos-flux `ce5e53145`），workaround 已移除（nop-entropy `836a4c4cf5`），`mode` 单字段即可控制 label 位置。下文保留原始诊断记录供追溯。

### 症状
开发者在 `<queryForm mode="horizontal">` 设置后，发现表单 label 仍渲染在控件上方（`label-top`），不是同行右对齐（`label-left`），以为是 bug。

### 根因（已定位）
- `nop-chaos-flux/.../crud-schema.ts` 的 `CrudQueryFormConfig.mode` 字段类型是 `'manual' | 'auto'`，**语义是 autoGenerateQueryFilter 的生成模式，与 label 位置无关**。
- `nop-chaos-flux/.../data-schema-validation.ts:23` 才是真正决定渲染表单 mode 的地方：
  ```js
  mode: queryForm.layout === 'horizontal' ? 'horizontal' : 'normal',
  ```
  它读的是 `queryForm.layout` 不是 `queryForm.mode`。
- 两个 `mode` 字段同名但语义不同——这是设计怪味，不是 bug。

### 现行 workaround（已于 2026-08-25 移除）

> 上游修复落地后，workaround 不再需要。`grid_crud.xpl` 现在只设 `mode`（校验器直读 `queryForm.mode`，`layout` 回退仍兼容）。历史 workaround 形态（2026-08-24 ~ 2026-08-25）：

```xml
<queryForm xpl:if="filterForm" id="${crudName}-query-form"
           layout="${filterForm.layoutMode || 'horizontal'}"     <!-- 校验器读这个 -->
           mode="${filterForm.layoutMode || 'horizontal'}"        <!-- 下游消费用，传到渲染 form -->
           labelWidth="..."
           wrapClassName="m-b-nm"
           actionsClassName="...">
```
两者必须同时设置，缺一不生效。（仅历史参考，勿再用。）

### 推荐修复（已落地，nop-chaos-flux 上游）
**最小改动方案**：
1. `CrudQueryFormConfig.mode` 的 docstring 明确写「此 mode 控制 autoGenerateQueryFilter 自动生成，与 label 位置无关；label 位置请使用 layout 字段」。
2. 渲染层同时读取 `queryForm.layout` 和 `queryForm.mode`，让 `mode` 在 queryForm 语境下也接受 `'horizontal'|'normal'|'inline'`（与渲染 form.mode 同语义）。
3. 修订 schema 文档。

**推进路径**：跨仓库改动 → 须按 AGENTS.md「跨仓库原则」开 plan → 双独立子 agent 批准 → nop-chaos-flux 维护者评审合并。短期不阻塞 nop-app-erp。

### 关联代码
- 触发器：`/nop-entropy/nop-frontend-support/nop-web/.../flux-web/grid_crud.xpl`
- 上游校验：`/nop-chaos-flux/packages/flux-renderers-data/src/data-schema-validation.ts:23`
- 上游 schema：`/nop-chaos-flux/packages/flux-renderers-data/src/crud-schema.ts:10-31`

### 修复落地状态
- 2026-08-24：在 `grid_crud.xpl` 落 workaround（同时设 `layout + mode`）+ 本 gotcha 入档。
- 2026-08-25：上游修复落地（nop-chaos-flux `ce5e53145`：`CrudQueryFormConfig.mode` 类型扩展为并集 + `data-schema-validation.ts` 新增 `resolveFormMode()`，`mode` 优先、`layout` 回退、legacy `'manual'|'auto'` 不受影响；回归测试 `crud-query-form-mode-resolution.test.ts` 11 case 全绿，包级 882/882 全绿）。同批 `grid_crud.xpl` 移除 workaround（nop-entropy `836a4c4cf5`，只设 `mode`）。经 `rebuild-flux-chain.sh` 全链路发布后浏览器层视觉验证：CRUD 列表 queryForm label 同行左置（`01-crud-voucher`/`02-crud-purchase` PNG 确认）；`mvn test -pl app-erp-all` 54/0/0/1 与 B10 基线零漂移。执行计划：`docs/plans/2026-08-24-2115-1-fix-flux-crud-queryform-mode-semantics.md`。

---

## 模板：新增 gotcha 条目

```markdown
## G-NNN <简短标题>

### 症状
<开发者会看到的现象>

### 根因
<具体文件 + 行号 + 解释>

### 现行 workaround
<代码片段>

### 推荐修复（待 <上游仓库>）
<最小改动 + 推进路径>

### 关联代码
<关键路径列表>

### 修复落地状态
<日期 + 落地点>
```