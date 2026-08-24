# 修复 flux CrudQueryFormConfig.mode 字段语义不一致（label 位置控制）

> **Plan Status**: active
> **Phase**: 1 — Fix（同步落地 + 验证）
> **Scope**: nop-chaos-flux（types + validator）、nop-entropy xpl（去掉 workaround）、docs（入档 owner-doc）
> **人工授权**: 已确认跳过双独立子 agent 审计流程（用户 2026-08-24 直接授权）；本 plan 由执行者 self-contained 完成 + 独立结束审计将以自我审计记录形式落地

---

## Objective

修复 nop-chaos-flux 中 `CrudQueryFormConfig.mode` 字段的语义不一致问题，使开发者在 `<queryForm mode="horizontal">` 设置后，渲染的表单 label 能正确显示在控件同行（而非上方）。

**Outcome surface**:
- nop-chaos-flux 的 `CrudQueryFormConfig.mode` 字段类型扩展为同时接受 label 位置值（`'normal'|'horizontal'|'inline'|'vertical'`）和原 autoGenerate 行为值（`'manual'|'auto'`）。
- `data-schema-validation.ts` 的 `createCrudQueryFormRegion` 函数支持：当 `queryForm.mode` 是 label 位置值时直接使用，否则回退到 `queryForm.layout` 派生。
- `layout` 字段对 `'vertical'|'inline'` 也正确映射（之前只处理 `horizontal`）。
- nop-entropy 的 `grid_crud.xpl` 可以去掉「同时设 layout + mode」的 workaround，只设 `mode` 即可生效。
- nop-app-erp 的视觉修复（queryForm 水平布局）通过本次上游修复+移除 workaround 后正常生效。

## Current Baseline（实施前盘点）

### 1. 设计不一致的精确代码位置

**`nop-chaos-flux/packages/flux-renderers-data/src/crud-schema.ts:10-31`**：
```typescript
export interface CrudQueryFormConfig extends SchemaObject {
  // ...
  layout?: 'horizontal' | 'vertical' | 'inline';   // L15: 控制布局方向
  mode?: 'manual' | 'auto';                          // L18: ⚠️ 同名字段，语义完全不同
  // ...
}
```

**`nop-chaos-flux/packages/flux-renderers-data/src/data-schema-validation.ts:13-70`**（关键映射逻辑）：
```typescript
function createCrudQueryFormRegion(schema: CrudSchema, path: string) {
  const queryForm = schema.queryForm;
  if (!queryForm?.body) return undefined;

  const region: BaseSchema & Record<string, unknown> = {
    type: 'form',
    id: createCrudQueryFormId(createNodeId(path, schema), path),
    body: queryForm.body,
    mode: queryForm.layout === 'horizontal' ? 'horizontal' : 'normal',  // L23 ⚠️
    actionsClassName: 'flex justify-end gap-2',
  };
  // ...
}
```

### 2. nop-entropy 当前 workaround（workaround 自 2026-08-24 已落地）

**`nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-web/grid_crud.xpl:53-77`**：
```xml
<queryForm xpl:if="filterForm" id="${crudName}-query-form"
           layout="${filterForm.layoutMode || 'horizontal'}"
           mode="${filterForm.layoutMode || 'horizontal'}"     <!-- 同时设两个 -->
           labelWidth="..."
           wrapClassName="m-b-nm"
           actionsClassName="m-t-xs flex justify-end gap-sm">
```

### 3. 已落地的 owner-doc gotcha

- `docs/architecture/flux-integration-gotchas.md` G-001 已记录此问题。

### 4. 测试基础设施盘点

- `nop-chaos-flux/packages/flux-renderers-data/src/__tests__/crud-query-and-pagination.test.tsx` 现有 queryForm 测试用例（L43、L99、L154、L264 等），可扩展以覆盖新模式语义。
- 测试运行命令：`pnpm --filter @nop-chaos/flux-renderers-data test` 或根级 `pnpm test`。

---

## Non-Goals

- **不**修改 AMIS 或下游 form 渲染层（已工作正常）。
- **不**修改 nop-entropy 的 `flux-web/grid_crud.xpl` 之外的其他页面（仅去掉本文件的 workaround）。
- **不**扩展 `CrudQueryFormConfig` 的其他字段（如 `columnCount`、`gap`）——这些工作正常。
- **不**引入新的 schema 字段（如 `labelPosition`）——通过扩展 `mode` 复用现有命名空间。

---

## Phase 1 — Fix（nop-chaos-flux 上游类型扩展 + 校验逻辑修复）

### Phase 1.1 — 类型扩展

**Files**:
- `/Users/abc/app/nop-chaos-flux/packages/flux-renderers-data/src/crud-schema.ts`

**Change**:
```typescript
// before:
mode?: 'manual' | 'auto';

// after:
/**
 * Form mode controlling label position OR auto-generation behavior.
 *
 * **Label position values** (used directly as rendered form mode):
 *   - 'normal'    → labels above inputs (default)
 *   - 'horizontal' → labels left of inputs (same row, labelWidth applies)
 *   - 'inline'    → labels and inputs on same line (compact)
 *   - 'vertical'  → vertical stacking (alias of 'normal')
 *
 * **Auto-generation behavior** values (legacy, for autoGenerateQueryFilter):
 *   - 'manual'    → manual query form (authored explicitly)
 *   - 'auto'      → auto-generated from filterable columns
 *
 * When this field is set to a label position value, it takes precedence over
 * `layout` for determining the rendered form's mode. When unset or set to
 * `'manual' | 'auto'`, the `layout` field determines the rendered form mode:
 *   layout: 'horizontal' → mode: 'horizontal'
 *   layout: 'inline'     → mode: 'inline'
 *   layout: 'vertical'   → mode: 'normal'
 *
 * Fix: G-001 (docs/architecture/flux-integration-gotchas.md).
 * History: prior to this fix, only `layout` was read by the validator, making
 * `mode` (the more discoverable field name) a no-op — see validation L23.
 */
mode?: 'manual' | 'auto' | 'normal' | 'horizontal' | 'vertical' | 'inline';
```

### Phase 1.2 — 校验逻辑修复

**Files**:
- `/Users/abc/app/nop-chaos-flux/packages/flux-renderers-data/src/data-schema-validation.ts`

**Change**（替换 L19-25 的 `region` 对象初始化）:
```typescript
// before:
const region: BaseSchema & Record<string, unknown> = {
  type: 'form',
  id: createCrudQueryFormId(createNodeId(path, schema), path),
  body: queryForm.body,
  mode: queryForm.layout === 'horizontal' ? 'horizontal' : 'normal',
  actionsClassName: 'flex justify-end gap-2',
};

// after:
const LABEL_POSITION_MODES = new Set(['normal', 'horizontal', 'inline', 'vertical']);

// Resolve rendered form mode with priority:
//   1. Explicit `mode` if it's a label position value (most discoverable)
//   2. Fallback to `layout`-based derivation:
//        horizontal → horizontal, inline → inline, vertical → normal
function resolveFormMode(layout: string | undefined, mode: string | undefined): string {
  if (mode && LABEL_POSITION_MODES.has(mode)) return mode;
  if (layout === 'horizontal') return 'horizontal';
  if (layout === 'inline') return 'inline';
  return 'normal';
}

const region: BaseSchema & Record<string, unknown> = {
  type: 'form',
  id: createCrudQueryFormId(createNodeId(path, schema), path),
  body: queryForm.body,
  mode: resolveFormMode(queryForm.layout, queryForm.mode),
  actionsClassName: 'flex justify-end gap-2',
};
```

### Phase 1.3 — 新增单元测试

**Files**:
- `/Users/abc/app/nop-chaos-flux/packages/flux-renderers-data/src/__tests__/crud-query-form-mode-resolution.test.tsx`（新建）

**Test cases**（最小覆盖）:
1. **G-001 backward-compat**: 设置 `queryForm.layout = 'horizontal'` → 渲染 form mode === 'horizontal'（既有行为不变）。
2. **G-001 new feature**: 设置 `queryForm.mode = 'horizontal'`（无 layout） → 渲染 form mode === 'horizontal'。
3. **G-001 priority**: 同时设置 `layout = 'vertical'` + `mode = 'horizontal'` → 渲染 form mode === 'horizontal'（mode 优先）。
4. **layout inline**: 设置 `queryForm.layout = 'inline'` → 渲染 form mode === 'inline'（修复原有 vertical/inline 缺失）。
5. **legacy auto-gen**: 设置 `queryForm.mode = 'manual'` + 无 layout → 渲染 form mode === 'normal'（'manual'/'auto' 不被解读为 label position）。
6. **default**: 既无 mode 也无 layout → 渲染 form mode === 'normal'。

**Implementation skeleton**:
```tsx
import { describe, it, expect } from 'vitest';
import { resolveFormMode } from '../data-schema-validation.js'; // export it
// OR: import the full schema validator and test via integration

describe('createCrudQueryFormRegion: mode resolution (G-001)', () => {
  it('layout=horizontal → mode=horizontal (backward compat)', () => { /* ... */ });
  it('mode=horizontal → mode=horizontal (new feature)', () => { /* ... */ });
  it('mode=horizontal + layout=vertical → mode=horizontal (mode wins)', () => { /* ... */ });
  it('layout=inline → mode=inline', () => { /* ... */ });
  it('mode=manual → falls back to layout, defaults to normal', () => { /* ... */ });
  it('no mode and no layout → mode=normal (default)', () => { /* ... */ });
});
```

> 注：如果 `resolveFormMode` 不便于 export，可改用集成测试 —— 通过 `createCrudQueryFormRegion` 的输入 schema 输出 region，断言 `region.mode`。

### Phase 1.4 — 类型检查与单元测试

**执行**:
```bash
cd /Users/abc/app/nop-chaos-flux
pnpm --filter @nop-chaos/flux-renderers-data typecheck
pnpm --filter @nop-chaos/flux-renderers-data test
```

**Exit criteria**:
- typecheck 通过（类型扩展无冲突）
- 现有 crud-query-and-pagination 测试仍通过
- 新增 G-001 测试 6 个 case 全绿

---

## Phase 2 — Workaround 移除（nop-entropy 同步落地）

### Phase 2.1 — 简化 grid_crud.xpl

**Files**:
- `/Users/abc/app/nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-web/grid_crud.xpl`

**Change**（去掉冗余 `layout` 属性，仅保留 `mode`，附简明注释）:
```xml
<!-- before (workaround 自 2026-08-24 落地): -->
<queryForm xpl:if="filterForm" id="${crudName}-query-form"
           layout="${filterForm.layoutMode || 'horizontal'}"
           mode="${filterForm.layoutMode || 'horizontal'}"
           labelWidth="...">
    <!-- ⚠️ 必须同时设 layout + mode 因为 flux 上游 bug：flux 校验器只读
         queryForm.layout 不读 mode。详见 G-001 与 plan 2026-08-24-2115-1。 -->
</queryForm>

<!-- after (上游修复后): -->
<queryForm xpl:if="filterForm" id="${crudName}-query-form"
           mode="${filterForm.layoutMode || 'horizontal'}"
           labelWidth="...">
    <!-- 上游 flux 已支持 queryForm.mode 控制 label 位置（G-001 修复，
         plan 2026-08-24-2115-1），无需再设 layout。 -->
</queryForm>
```

### Phase 2.2 — 重建 nop-web

**执行**:
```bash
cd /Users/abc/app/nop-entropy
mvn install -pl nop-frontend-support/nop-web -DskipTests
```

**Exit criteria**: BUILD SUCCESS

---

## Phase 3 — 跨链路发布（flux bundle → nop-web-site → ERP runner）

> 依赖前置：Phase 1 + Phase 2 全部完成且 typecheck/test 全绿

### Phase 3.1 — flux 包发布

**执行**（按现有 `scripts/rebuild-flux-chain.sh` 链路）:
```bash
bash scripts/rebuild-flux-chain.sh
```

链路（脚本内已编排）:
1. `pnpm --filter @nop-chaos/flux build` + pack
2. `pnpm build --force`（nop-chaos-next）
3. `scripts/sync-site.sh` 同步 dist → nop-entropy/nop-web-site
4. `mvn clean install -pl nop-frontend-support/nop-web-site -DskipTests`
5. `mvn clean install -DskipTests`（全 reactor，重建 runner jar）

**Exit criteria**:
- 5 步骤全部 BUILD SUCCESS
- `target/app-erp-all-1.0-SNAPSHOT-runner.jar` mtime 更新
- 错误率 0

### Phase 3.2 — 视觉验证

**Files**:
- `tests/e2e/visual/_exploration/verify-fixes.ts`（已存在，2026-08-24 落地）

**执行**:
```bash
# 启动 app
java -Dnop.core.resource.check-duplicate-vfs-resource=false -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar &

# 跑验证脚本
npx tsx tests/e2e/visual/_exploration/verify-fixes.ts
```

**Exit criteria**:
- 7 张 PNG 全部生成至 `tests/e2e/visual/_exploration/verify-screenshots/`
- 关键页面（CRUD 列表）的 PNG 中：**queryForm 字段 label 同行右对齐**（验证 G-001 修复生效）
- 与 2026-08-24 基线（`docs/analysis/2026-08-24-complex-page-visual-snapshot-analysis.md`）对照：01-crud-voucher PNG 中 label 不再 above input

### Phase 3.3 — 测试回归

**执行**:
```bash
cd /Users/abc/app/nop-app-erp
mvn test -pl app-erp-all
```

**Exit criteria**:
- 既有 surefire 计数（2026-08-24 B10 基线：54/0/0/1）零回归
- 若有变动需明确登记

---

## Closure Gates

- [ ] Phase 1.1 `crud-schema.ts` 类型扩展落地
- [ ] Phase 1.2 `data-schema-validation.ts` `resolveFormMode` 函数 + LABEL_POSITION_MODES 集合落地
- [ ] Phase 1.3 新增测试用例 ≥6 个并通过
- [ ] Phase 1.4 typecheck + 既有测试零回归
- [ ] Phase 2.1 grid_crud.xpl 去掉 `layout` 属性 workaround
- [ ] Phase 2.2 nop-web `mvn install` BUILD SUCCESS
- [ ] Phase 3.1 `rebuild-flux-chain.sh` 全链路 BUILD SUCCESS
- [ ] Phase 3.2 视觉验证 PNG 中 CRUD 列表 label 同行显示
- [ ] Phase 3.3 ERP 测试零回归（app-erp-all surefire 不低于基线）
- [ ] owner-doc 一致性检查（plan/log/gotcha 三处引用同步）

## Closure Audit Record（执行者 self-contained 审计）

> 用户明确授权「不用 double audit」，故本节为执行者自我审计记录；未来若需独立审计，可由其他 session 重新执行 §Phase 3.2-3.3 验证。

（实施完成后回填）

---

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| 既有代码有 `mode: 'manual'/'auto'` 设置，修复后被解读成 label position | Low | High | 类型扩展为并集 + 校验函数用 `LABEL_POSITION_MODES.has(mode)` 显式过滤；'manual'/'auto' 不会匹配 set，回退到 layout 派生，行为不变 |
| `layout: 'inline'` 实际不被 form 渲染层支持 | Medium | Low | 即使映射到 'inline'，下层不识别也只会回到 default；非破坏性；下个 sprint 再跟进 |
| bundle 发布失败导致 ERP 启动异常 | Low | High | Phase 3.2 用 verify-fixes.ts 端到端验证；如失败，紧急回滚 grid_crud.xpl 保留 layout 属性（workaround 已验证可工作） |

## Decision Log

- **D1**: 选择扩展 `mode` 字段类型（而非新增 `labelPosition` 字段）。理由：复用现有命名空间、对老代码完全向后兼容、对开发者更直观（设一个字段即可）。替代方案：新增 `labelPosition` 字段——评估为过度拆分。
- **D2**: 校验函数优先级为 `mode > layout > default('normal')`。理由：`mode` 是开发者更可能直接接触的字段名（与 form 渲染层字段同名），应作为首选；`layout` 作为更结构化的方向描述保留作 fallback。
- **D3**: 不动 `autoGenerateQueryFilter` 行为。理由：超出本 fix 范围；既有 `mode: 'manual'/'auto'` 行为若需保留，应作为单独 plan 处理。