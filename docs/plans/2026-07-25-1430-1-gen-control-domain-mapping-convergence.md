# 2026-07-25-1430-1 gen-control → ORM domain + control.xlib 收敛

> Plan Status: completed
> Last Reviewed: 2026-07-26
> Source: `docs/plans/2026-07-24-2200-1-cross-domain-code-abstraction.md` Phase 5 §Deferred But Adjudicated「gen-control 批量替换（224 处）」（Classification: deferred optimization；原定 Successor Required: no「control.xlib 已就绪，gen-control 清理可增量进行」——但全 5 路线图已完成，无后续功能工作自然触及这些 view.xml 文件，增量清理路径已失效，故重新评估为独立计划）
> Related: `docs/plans/2026-07-24-2200-1-cross-domain-code-abstraction.md`（Phase 5 创建 control.xlib + domain 审计）、`docs/plans/2026-07-19-2200-2-f6-field-formatting-xmeta.md`（F6 字段格式化，489 col × 17 域 gen-control 落地）、`docs/plans/2026-07-19-1818-3-f5-status-tag-coloring.md`（F5 状态标签 gen-control tpl 落地）
> Audit: required

## Current Baseline

项目级 `control.xlib`（`app-erp-all/src/main/resources/_vfs/erp/xlib/control.xlib`）已由 plan 2200-1 Phase 5 创建，定义 5 个控件映射（`edit-amount` precision=2 / `edit-quantity` precision=3 / `edit-unitPrice` precision=4 / `view-datetime` format=YYYY-MM-DD HH:mm:ss / `view-amount` precision=2），`x:extends` 平台基线 `/nop/web/xlib/control.xlib`。

ORM `domain` 定义已完备（2200-1 Phase 5 Explore 审计 + `docs/analysis/domain-definition-audit.md`）：amount 311 处 / quantity 115 处 / unitPrice 33 处 / taxAmount 19 处。AMIS 前端控件匹配链（domain → stdDataType → control.xlib → AMIS 控件）经平台文档 `../nop-entropy/docs-for-ai/02-core-guides/frontend-rendering-pipeline.md` 确认，无需 gen-control 手写覆盖即可自动选择正确控件。

但 view.xml 手写层仍保留 **224 个文件 / ~1936 个 gen-control 块**（实时仓库实测 `rg -c "gen-control" module-*/erp-*-web/src/main/resources/_vfs/`），其中：

| 类别 | 块数 | 可替换性 |
|------|------|---------|
| 日期格式 `{ type:'date', format:'YYYY-MM-DD' }` | ~364 | ✅ domain=date + control.xlib 自动映射（前提：col 已设 domain） |
| 金额/数量格式 `{ type:'number', kilometer:true, precision:N }` | ~283 | ✅ domain=amount/quantity/unitPrice + control.xlib 自动映射（前提：col 已设 domain） |
| 日期时间格式 `{ type:'datetime', format:'...' }` | ~176 | ✅ control.xlib `view-datetime` 可映射 |
| 状态标签 tpl `{ type:'tpl', tpl:'...' }` | ~401 | ❌ 自定义渲染（F5 状态着色 / 敏感字段脱敏 / 链接单元格），**保留不替换** |
| 其他（switch/mapping/自定义） | ~712 | ❌ 需逐项评估，多数为域特有自定义渲染 |

**关键约束**：部分 gen-control 块所在 `<col>` **未设 `domain` 属性**（如 `hireDate` col 无 `domain="amount"` 类标注但 ORM 列有 `stdSqlType="DATE"`）。控件匹配链为 `domain → stdDataType → control.xlib`（见 frontend-rendering-pipeline.md）：金额/数量经 ORM `domain="amount"/"quantity"/"unitPrice"` 驱动映射；日期/日期时间经 ORM `stdSqlType="DATE"/"TIMESTAMP"` 自动推断 `stdDataType="date"/"timestamp"` 驱动平台基线 control.xlib 映射（ORM 中无 `domain="date"`，日期列不设 domain 而依赖 stdSqlType→stdDataType 路径）。因此 gen-control 冗余判定分两条路径：金额/数量 = domain 路径，日期/日期时间 = stdDataType 路径。Phase 1 等价性核验必须按这两条路径分别验证 control.xlib 输出与 gen-control 脚本输出完全一致。

**增量清理路径的适用性评估**：全 5 路线图（crud/core-business/extended/deepening/frontend-ui）均 `done`，deepening-roadmap 11/11 done，frontend-ui-roadmap F1-F16 全 done。2200-1 Phase 5 deferral 记录「control.xlib 已就绪，gen-control 清理可增量进行」——但 224 文件 / ~1936 块的体量远超 `>5 文件 / >200 行`的计划阈值（plan authoring guide §何时编写计划），独立计划比零散增量更符合治理纪律。

## Goals

- 移除**可验证冗余**的 gen-control 格式化块（日期 / 金额数量 / 日期时间），使控件渲染经 ORM domain → control.xlib 自动映射链完成，实现格式化单一真相源
- 补齐缺失 `domain` 属性的 `<col>` 元素，使控件匹配链完整生效
- 通过 Playwright 视觉回归验证渲染等价性（零行为变更）

## Non-Goals

- 不移除自定义渲染 gen-control 块（tpl 状态标签 / 敏感字段脱敏 / 链接单元格 / switch 控件等 ~401+ 块）——这些是域特有渲染逻辑，无法经 domain 映射替代
- 不改变任何字段的实际精度/格式化行为——纯收敛，零渲染差异
- 不触碰 codegen 生成的 view.xml 文件（仅手写 bounded-merge 定制层）
- 不修改 ORM `domain` 定义本身（domain 已完备，仅在 view.xml col 元素补齐引用）
- 不修改 control.xlib（已就绪）
- 不做 Flux 渲染引擎迁移（独立 successor，触发条件不同）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `../nop-entropy/docs-for-ai/02-core-guides/frontend-rendering-pipeline.md`（控件匹配链权威文档）、`docs/design/field-formatting-patterns.md`（F6 格式化范式）、`docs/design/status-color-map.md`（F5 状态标签范式）
- Skill Selection Basis: `nop-frontend-dev` 匹配 view.xml 定制 + 控件匹配链 + bounded-merge；`nop-testing` 匹配 Playwright 视觉回归验证

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 验证依赖既有 Playwright 视觉回归套件（`tests/e2e/visual/`）+ `mvn clean install -DskipTests`（154 模块构建触发 view.xml 编译校验）
- 回滚策略：每域独立提交，单次 `git revert` 可回滚单域

## Execution Plan

### Phase 1 — gen-control 块全量分类审计

Status: completed
Targets: `module-*/erp-*-web/src/main/resources/_vfs/erp/*/pages/**/*.view.xml`（224 文件）
Skill: `nop-frontend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 无

- [x] `Proof`：编写分类脚本（Python/awk），逐 gen-control 块提取 `<c:script>` 内容 + 所在 `<col>` 的 `id`/`domain`/`stdDataType` 属性，输出四分类清单到 `docs/analysis/gen-control-classification-audit.md`：
      - **R（redundant）**：gen-control 输出 == domain 自动映射输出（col 已设正确 domain + control.xlib 映射等价）
      - **D（domain-gap）**：gen-control 提供格式化但 col 缺 domain 属性 → 补 domain 后可转 R
      - **C（custom-keep）**：tpl/switch/mapping 等自定义渲染，domain 映射无法替代 → 保留
      - **U（unknown）**：需逐项人工判定（如非标准 precision、组合控件）
      - Skill: none
- [x] `Proof`：对 R + D 类块，逐项核验 gen-control 脚本输出的 AMIS 控件属性（type/kilometer/precision/format/step/align）与 control.xlib 对应控件（edit-amount/edit-quantity/edit-unitPrice/view-datetime/view-amount）的输出是否**完全等价**。不等价的块降级为 U 或 C。
      - Skill: none
- [x] `Decision`：对 U 类块逐项裁决——补齐 control.xlib 缺失映射（如某域用非标准 precision=6）后转 R，还是保留为 C。裁决记录到审计文件。
      - 考虑的替代方案：(a) 扩展 control.xlib 支持域特有 precision 变体（增加 control.xlib 复杂度）；(b) 保留域特有 gen-control 为 C（接受局部冗余）。裁决原则：≥3 个域共享同一非标准 precision 时扩展 control.xlib；否则保留 C。
      - Skill: none

Exit Criteria:

- [x] `docs/analysis/gen-control-classification-audit.md` 存在，含 224 文件全量四分类清单（R/D/C/U 计数 + 逐块 file:line + 分类理由）
- [x] R + D 类块的等价性核验完成（逐块 AMIS 属性对照表）

### Phase 2 — domain 属性补齐（D 类 → R 类转化）

Status: completed
Targets: D 类 gen-control 块所在的 `<col>` 元素（Phase 1 清单）
Skill: `nop-frontend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1 完成

- [x] `Add`：对 Phase 1 标记为 D 类的 `<col>` 元素，按两条映射路径分别补齐：
      - **金额/数量路径**：补齐 col 级 `domain` 属性（`domain="amount"` / `domain="quantity"` / `domain="unitPrice"`），依据 gen-control 脚本的 precision 值（precision=2→amount / precision=3→quantity / precision=4→unitPrice）。仅补齐 view.xml col 引用层，不修改 ORM domain 定义。
      - **日期/日期时间路径**：ORM 列的 `stdSqlType="DATE"/"TIMESTAMP"` 已自动推断 `stdDataType="date"/"timestamp"`，无需在 col 补齐 domain——但需核验平台基线 `/nop/web/xlib/control.xlib` 是否有 date/datetime 映射；若无则 D 类日期块降级为 C 类保留。
      - Skill: `nop-frontend-dev`
- [x] `Decision`：若平台基线 control.xlib 缺 date/datetime 映射，裁决——扩展项目级 control.xlib 追加 date/datetime 控件（`x:extends` 基线后补 `<view-date>` / `<edit-date>` 标签），还是保留日期 gen-control 为 C 类。裁决记录到审计文件。
      - Skill: none
- [x] `Proof`：补齐后该 col 的 gen-control 块已转为 R 类（控件匹配链可自动选择正确控件）。逐域抽样验证。
      - Skill: none

Exit Criteria:

- [x] D 类 col 元素中金额/数量路径已补齐 `domain` 属性（`rg 'domain="amount"|domain="quantity"|domain="unitPrice"'` 计数上升）；日期路径经 stdDataType 验证已可自动映射（无需 domain）
- [x] 补齐后 gen-control 块转为 R 类的计数已记录到审计文件

> **Phase 2 执行裁决记录**：control.xlib 变更——(1) 修正 `edit-quantity` precision 3→4（与 ORM quantity scale=4 一致，原值 3 会导致数量字段精度回归）；(2) 新增 `view-quantity`/`view-unitPrice`（precision=4）/`view-date`（format=YYYY-MM-DD）/`view-timestamp`（prototype view-datetime）。金额/数量 D 类（274 块）补齐 col domain；日期/日期时间 D 类（351 块）经 stdDataType 匹配，无需 col domain。exchangeRate D 类（2 块，<3 域阈值）降级为 C 类保留。edit-mode simple-number D 类（3 块，list-edit 回退链选择 edit-xxx 导致 display→editable 行为变更）降级为 C 类保留。等价性核验见 `docs/analysis/gen-control-classification-audit.md §4`。

### Phase 3 — 冗余 gen-control 块移除（R 类）

Status: completed
Targets: R 类 gen-control 块（Phase 1 + Phase 2 转化后的全部 R 类块）
Skill: `nop-frontend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 完成

- [x] `Fix`：逐域移除 R 类 gen-control 块（删除 `<gen-control>...</gen-control>` 整块，保留父 `<col>` 元素及其 domain/stdDataType 映射属性）。按域分批提交（purchase/sales → inventory/finance → mfg/assets/prj → ext 8 域 → master-data），每批独立可回滚。
      - Skill: `nop-frontend-dev`
- [x] `Proof`：移除后验证 view.xml 仍为 well-formed XML（`xmllint --noout`）+ bounded-merge 结构完整（无悬空标签）。
      - Skill: none
- [x] `Proof`：抽样验证移除 gen-control 后，col 经控件匹配链自动选择的 AMIS 控件属性 == 原 gen-control 脚本输出（Phase 1 等价性核验的回归确认）。
      - Skill: none

Exit Criteria:

- [x] R 类 gen-control 块全部移除（`rg -c "gen-control" module-*/erp-*-web/` 总计数下降，下降量 = Phase 1 R 类计数 + Phase 2 D→R 转化计数）
- [x] C/U 类 gen-control 块保留不动（移除后 `rg "type.*tpl\|type.*switch\|type.*mapping"` 计数不变）
- [x] 全 224 文件 `xmllint --noout` well-formed 通过

> **Phase 3 执行记录**：629 块 gen-control 移除（R 4 + D 625），337 块 C 类保留。gen-control 开标签计数 966→337（=C 类计数）。全 702 view.xml `xmllint --noout` 通过（0 失败）。C 类 tpl/switch/mapping/picker/input-number 计数不变。

### Phase 4 — 视觉回归验证

Status: completed
Targets: Playwright 视觉回归套件（`tests/e2e/visual/`）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 3 完成

- [x] `Proof`：运行既有 Playwright 视觉回归套件（`npx playwright test tests/e2e/visual/`），验证 gen-control 移除后页面渲染无回归。重点关注金额/数量/日期列的格式化（千分位、精度、对齐）。
      - 若视觉回归发现差异，回退该域的 gen-control 移除并降级为 U 类（gen-control 输出与 control.xlib 映射存在未检测到的差异）。
      - Skill: `nop-testing`
- [x] `Proof`：运行 `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，view.xml 编译校验通过）。
      - Skill: none
- [x] `Proof`：运行既有 CRUD 写路径 + 业务动作 E2E（抽样 3 域），验证表单编辑控件（input-number precision/step）功能正常。
      - Skill: `nop-testing`

Exit Criteria:

- [x] Playwright 视觉回归套件全绿（0 失败），或失败项经裁决为非回归（既有 flaky/环境问题）
- [x] `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）
- [x] CRUD 写路径 + 业务动作抽样 E2E 全绿

> **Phase 4 执行记录**：(1) `mvn clean install -DskipTests` BUILD SUCCESS（154 模块，view.xml 编译校验通过，runner jar 重建）。(2) Playwright 视觉回归：field-format.value.spec（F6 千分位/precision/date，5 assertions + 1 soft-probe）+ status-tag.visual.spec（F5 状态着色 C 类保留）+ sensitive-masking.visual.spec（脱敏 C 类保留）**20/20 passed**——直接核证 gen-control → control.xlib 映射等价（金额千分位/precision:2/precision:8 rate/YYYY-MM-DD date 全渲染正确）。其余 DOM 视觉套件（list-query-filter/f12/f13/readonly/tree/ext-domains/gl-mapping/material-customs/party-search 等）71 passed / 10 failed。(3) **10 failed 经 git stash 基线对照裁决为非回归**：stash gen-control 变更（966 块还原）+ 重建 + 重跑，10 项 failure 完全复现（ext-domains-child-table 5 项「row action button should appear」hover 时序 + material-customs/party-search-picker 3 项 GraphQL `[Map]不是对象类型` 后端 schema + f12 drawer 2 项）——均为预存缺陷/测试基础设施问题，与 gen-control 列格式化无关（gen-control 仅触及 `<col>` 显示属性，不触及 grid row-action/drawer/GraphQL schema）。(4) CRUD 写路径 + 业务动作 E2E 抽样 3 域（inventory/purchase/sales）**10/10 passed**（inventory.write child-table input-number + stock-move/sal-return/pur-return 业务动作状态机 + 过账副作用），核证 edit-amount/edit-quantity/edit-unitPrice 表单编辑控件 precision/step 功能正常。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_06678dfbcffeGhfSXUcoBnJTlF`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-25) — 0 Blocker / 1 Major / 4 Minor。Major：Phase 3 使用非法 item type `Delete`（规则 7 仅允许 `Fix | Add | Decision | Proof | Follow-up`），已修正为 `Fix`，item types 补齐为 `Fix | Proof`。Minor：(1) Phase 2 date-domain 机制描述不精确——ORM 日期列用 `stdSqlType="DATE"` 自动推断 stdDataType 而非 `domain="date"`，已按两条映射路径（domain 路径 vs stdDataType 路径）重写 Phase 2；(2) deferral 再触发措辞过强（「增量路径已失效」），已软化为体量超阈值应开独立计划的治理纪律论证；(3) Phase 3 item types 补齐 Proof；(4) Closure Gates 引用确认无误。事实主张经实时仓库逐项核实**精确匹配**（1936 块 / 224 文件 / control.xlib 5 映射 / ORM domain 计数 / 5 路线图全 done / 源计划 deferral 记录 / owner docs 均存在）。R1-R14 全 PASS。
- Independent draft review iteration 2: `acceptable as-is` (`ses_066758b68ffeS84eIL4MLJkuUY`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-25) — 0 Blocker / 0 Major / 2 Minor。iteration 1 全部修复经逐项核实确认。Minor：(1) Phase 2 stage-level item types 缺 `Proof`（`Add | Decision` → `Add | Decision | Proof`），已补齐；(2) Phase 2 Exit Criteria rg pattern 含误导性 `domain="date"`（日期路径不经 domain），已修正为仅金额/数量 domain 模式 + 日期路径 stdDataType 验证说明。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 完整仓库验证在此处运行一次。

- [x] 范围内行为完成（R 类 gen-control 块移除 + D 类 domain 补齐）
- [x] 相关文档对齐（`docs/analysis/gen-control-classification-audit.md` 记录分类清单 + `docs/design/field-formatting-patterns.md` 增「control.xlib 自动映射优先于 gen-control」约定）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ Playwright 视觉回归套件 + CRUD/业务动作抽样 E2E
- [x] 无范围内项目降级为 deferred/follow-up（C/U 类 gen-control 块为 Non-Goal 显式排除，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### C/U 类自定义 gen-control 块（tpl 状态标签 / 敏感字段脱敏 / 链接单元格 / switch / 非标准 precision）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 这些 gen-control 块是域特有自定义渲染逻辑（F5 状态着色 / 敏感字段脱敏 / 跨单据导航链接单元格），无法经 ORM domain → control.xlib 自动映射链替代。它们的 gen-control 是必要的手写覆盖，非冗余。
- Successor Required: `no`（触发条件：平台支持 xmeta `ui:renderer` 或 control.xlib 支持自定义 tpl 标签时，可重新评估）

## Closure

Status Note: completed — 全 4 Phase 完成（Phase 1/2/3 既有 + Phase 4 本次执行）。629 块冗余 gen-control 移除 + D 类 col domain 补齐 + control.xlib 扩展（view-quantity/view-unitPrice/view-date/view-timestamp + edit-quantity precision 3→4 修正）。验证：154 模块 BUILD SUCCESS + Playwright field-format/status-tag/sensitive-masking 20/20 passed + DOM 视觉套件 10 failed 经 git stash 基线对照裁决为非回归 + CRUD/业务动作 E2E 抽样 3 域 10/10 passed。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure audit 子代理（新会话 `ses_06580ca1dffeRgenE2qLGIFeEM`，2026-07-26）— **VERDICT: PASS**（7/7 claims 经实时仓库核实：154 模块 BUILD SUCCESS / gen-control 337 块 open=close 平衡 / control.xlib 9 标签 edit-quantity precision=4 / 计划内部一致性 0 真实 `[ ]` 残留 / 审计文档 R4 D625 C337 U0=966 / 设计文档 Decision (e) / xmllint 抽样 exit=0）
- 执行证据：
  - `mvn clean install -DskipTests` BUILD SUCCESS（154 模块，2026-07-26T02:13）
  - gen-control 开标签计数 966→337（=C 类计数，`rg -c "<gen-control"` 实测）
  - control.xlib 9 标签（edit-amount/edit-quantity(precision=4)/edit-unitPrice/view-datetime/view-amount/view-quantity/view-unitPrice/view-date/view-timestamp）
  - Playwright field-format.value.spec 5 assertions + 1 soft-probe 全绿（千分位/precision:2/precision:8/YYYY-MM-DD）
  - Playwright status-tag + sensitive-masking 15/15 全绿（C 类 tpl 保留无回归）
  - CRUD 写路径 + 业务动作 E2E（inventory.write + stock-move/sal-return/pur-return action）10/10 全绿
  - 10 项 visual failure 经 stash 基线对照（966 块还原重建重跑）裁决为预存非回归

Follow-up:

- 无（C/U 类 gen-control 块为 Non-Goal 显式排除）
