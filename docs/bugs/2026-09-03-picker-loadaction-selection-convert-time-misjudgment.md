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
