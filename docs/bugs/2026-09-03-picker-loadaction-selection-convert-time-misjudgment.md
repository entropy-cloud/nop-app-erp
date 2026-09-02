# picker loadAction `selection` 被"convert-time only"误判删除——单侧链路验证 + 静默降级假阴性

- 日期：2026-09-03
- 发现于：plan `2026-09-02-2028-1-flux-picker-schema-override` 收尾期，并行会话对 `_delta/.../flux-control.xlib` 5 个 picker tag 及 5 个业务 view.xml 的改动审查
- 状态：open（本仓已还原至正确基线 `b60cd477f`；并行会话侧需对齐）

## 症状

并行会话将 picker `loadAction` 中的 `selection: '{@pageSelection}'` 从 5 个 delta tag 与
5 个业务 view.xml 中删除，同时插入 `dependsOn: ['__crud_load__']`，并给出理由：

> "The `selection` arg is convert-time only (NormalizeApi); omitting it makes the
> backend return its default field set."

若落地，findPage 返回默认投影 → picker 的 labelField（`lineNo`/`roleName`/`name`）不保证
在返回行内 → picker v3 的 label 反应式解析（已选 value 显示 label）静默断裂。

## 实证裁定（selection 是 runtime 一等参数）

| 证据 | 位置 |
|------|------|
| runtime 显式消费 | `flux-runtime/src/async-data/request-runtime.ts:322,346` —— `finalizeApiRequest` / `materializeApiRequest` 均携带 `selection: api.selection` 进入 request，交宿主 fetcher 构建 GraphQL 投影 |
| 官方转换层先例 | `flux-web/page_simple.xpl:28,61`、`container_simple.xpl:24,34` —— 均输出 `selection: _n.selection` 作为 ajax args 运行时字段（与 url/method/data/includeScope 并列） |
| `dependsOn` 归属 | 仅 async-data source/reaction 体系（`source-registry.ts:198`、`reaction-runtime.ts:116`）；ajax 的 BUILT_IN fieldRules 为 url/method/data/params，执行链不读 args.dependsOn → 插入的 `dependsOn: ['__crud_load__']` 是死配置 |

## 根因：为什么会误判（五步误判链）

1. **命名混乱的历史包袱**。delta 中曾长期存在错误写法 `gql:selection`（经人工裁决纠正为
   `selection`）。"gql: 前缀是错的"这一正确结论被泛化为"selection 家族可疑"，为后续误删埋下
   心理倾向。

2. **只验证了半条链路（生产侧 ✓ / 消费侧 ✗）**。会话看到 NormalizeApi / 转换层"加工" selection
   （pageSelection 编码），推断它是 convert-time only。但转换层加工 ≠ 转换期终结——runtime 端
   `request-runtime.ts` 显式透传才是生命周期终点。**只 grep 了生产端，没有 grep 消费端**，与
   Lesson 16（跨仓库 schema 契约须对照消费端渲染器源码验证）完全同构。

3. **静默降级零失败信号**。删除 args 字段不抛错、不告警；后端返回默认投影后页面照常渲染
   （若默认列恰好覆盖 labelField 则视觉无差异）；键存在性等结构测试全绿。**没有失败信号恰是
   最危险信号**——松耦合契约失效是静默降级非抛错（Lesson 16 原话）。

4. **副作用被解读为清理目标**。其调研 tail 已自证"omitting it makes the backend return its
   default field set"——即已观察到**契约行为变更**（投影收缩），却把它当作简化收益收尾，而没有
   触发"这是契约变更，需消费端裁决"的停机检查。

5. **相邻体系字段串味（dependsOn 幻觉）**。调研 async-data（source/reaction）时见到 `dependsOn`，
   未经 ajax fieldRules 核对即迁移到 loadAction args——把 A 体系的合法字段配置进 B 体系，
   产生永不生效的死配置。

环境放大因素：该会话运行于 `oc-EXECUTE`（60min timeout）、系统 load≈13 的压力窗口，倾向快速
"清理"而非系统性契约裁决。

## 处置

- 本仓（nop-app-erp）6 文件已还原至 `b60cd477f` 基线（selection 保留、无 dependsOn）。
  还原动作由执行者以 `git checkout --` 完成，**未先 stash/patch 备份**——因 checkout 前已完整
  留档 diff 而结果无损，但流程上属违规（先备份再还原才是正确姿态），在此记录。
- nop-chaos-flux 侧同期未提交改动（picker JSDoc 增补 / 删 deprecated helpers / 删零引用
  picker-option-list / 文档精简）方向合法，与本次误判无关，另行分拣提交。
- 若未来确需收缩投影：正确路径 = ①证明 fetcher 默认投影 ⊇ 全部消费方 labelField 集合
  （逐 picker 域枚举）；②作为契约变更走 dual-agent 复核；③依赖的 `dependsOn` 若真需要，
  载体是 async-data source 声明而非 ajax args。

## 自检清单（动任何 ajax args 字段前）

- [ ] 生产侧（转换层/生成器）与消费侧（runtime 执行器/fetcher）**两侧**都 grep 过该字段？
- [ ] 删除后行为差异是否被显式陈述？陈述的语感是"简化"还是"契约变更"？
- [ ] 失效模式是否静默（无抛错/无测试红）？静默契约变更必须对照消费端源码裁决。
- [ ] 新增字段是否核对过所属体系的 fieldRules / BUILT_IN 定义表（防跨体系串味）？

## 关联

- Lesson 16（跨仓库 schema 契约消费端验证）、Lesson 18（契约重设计禁类型嗅探与语法糖）
- `nop-chaos-flux/flux-guide/design-patterns/picker-transfer.md` v3.3 协议（scope 发布 + builtin pick 回调）

## 补记(2026-09-03 深夜):真根因定位 + x:extends 复用尝试

### 真根因(修正前文"半条链路"表述的粒度)

`selection` 是 runtime 一等参数没错,但**误删的直接诱因**是:控件级手写 loadAction **不经过
NormalizeApi**(它只被 pageModel 层 api 字段显式调用),因此 `'gql:selection': '{@pageSelection}'`
中的 `{@...}` 模板**从不被渲染**,以字面量直达运行时(实证:导出 JSON `main.page.json:543` picker
的 `gql:selection` 为字面量,而同文件 :977 页面级 CRUD 的 `selection` 是 NormalizeApi 渲染的真值
`total,page,items{...}`)。后端对非法 selection 容错 → 默认投影 → 页面"碰巧能用",死值潜伏至今。
`pageSelection` 本身没有问题——它是 grid_crud.xpl genScope 的变量,经 NormalizeApi 正常渲染;
问题是 delta tag / gen-control 上下文**既无该变量、也不经过渲染管线**。

### AMIS 先例与 x:extends 复用(正确方向)

AMIS `control.xlib:912-960`(edit-relation/edit-roleId/edit-userId)**从不手写 loadAction**:
`type:'picker'` + `x:extends: <实体>/picker.page.yaml`(该 yaml 仅一行 `x:gen-extends: GenPage(page="picker")`,
经 grid_crud.xpl picker 分支动态展开,loadAction 由 pageModel 管道拼出真实 selection)+ 字段级覆盖
(valueField/labelField/multiple)。flux 控件可完全复用此模式。

### 复用尝试的新阻塞:自引用递归(未解决,successor)

delta 5 tag 改为 x:extends 复用后:`ErpMntEquipment/main.page.yaml` 报
`handler-exceed-max-nested-level (maxLevel=50)`——设备表含 parentId 自关联,picker.page.yaml 展开的
grid/queryForm 中关联列再次生成 edit-relation picker → 再次 x:extends 同一 picker.page.yaml → 无限
递归。已回滚 flux-control.xlib 至手写基线(999/0 可用性优先)。

**Successor 修复方案(两选一,需先查清 GenGridCols/GenFormBody 对 picker page 的控件调用链)**:
1. 自引用守卫:tag 内 `bizObjName === objMeta.name` 时不复用(退化为无内容 picker 或安全手写)
2. picker.page.yaml 的列/查询生成禁用编辑控件(picker page 本无编辑语义),根除递归

同时回滚并行会话的 `dependsOn: ['__crud_load__']`(页面级官方产物存在该字段,但控件级手写
args 中同样无消费点;其真实消费体系为 async-data source/reaction)。

## 修复落地(d8825a8cc, 2026-09-03)

采用 flux 原生 **dynamic-renderer** 机制(`flux-renderers-basic/src/dynamic-renderer.tsx`):
控件级 picker 的 `pickerSchema` = `{type:'dynamic-renderer', loadAction:{ajax: 实体 picker 页面路径}}`,
运行时异步拉取页面 schema 并渲染,**已加载页面缓存**(二次打开无异步);异步边界天然打破
x:extends 构建期自引用递归(ErpMntEquipment parentId 自关联实测触发 maxLevel=50 后已消)。
5 个 delta tag 全部改为该形态,selection 由页面级 NormalizeApi 权威拼出,`{@pageSelection}` 死值与
无消费的 `dependsOn` 一并清除。验证:999/999 页 0 错误 + 契约测试绿 + BUILD SUCCESS。
