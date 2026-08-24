# Lesson 16: 跨仓库 schema 契约——生成侧必须对照消费端渲染器源码验证，结构性检查会静默漏掉字段名错位

> **来源**：2026-08-24 flux picker 字段级契约修复（plan `docs/plans/2026-08-24-1147-1-fix-flux-picker-xlib-schema.md`）。三轮独立 plan-audit 子代理全部通过的计划，被 closure-audit 发现 2 个 P0 运行时缺陷（valueField/valueKey 契约错位 + 93% picker URL 为 `@query:null__findPage`）——修复本身曾让"warning toast 消失"但 picker 实际不可用。
> **适用场景**：任何"本仓生成 JSON/schema、另一仓库渲染器/运行时消费"的链路——xlib 控件输出 ↔ nop-chaos-flux 渲染器、page.yaml 输出 ↔ 前端路由器、GraphQL payload ↔ 客户端解析。也适用于同仓库内"生成层 ↔ 运行时反射读取"的松耦合契约。
> **失败模式**：验证只做**结构性存在性检查**（键是否存在、类型对不对、错误码是否为零），不核对消费端源码**实际读取的字段名与取值语义**——生成侧输出 A 字段名，消费侧读 B 字段名，运行时静默退化到 fallback，无报错、无告警，直到真实用户点击才暴露。

## 核心论点

跨仓库松耦合契约的失效模式是**静默降级而非抛错**：消费端渲染器对未知键的处理是忽略（`schemaProps.valueKey === undefined` → fallback 到 `row.value`），不是校验失败。因此：

1. **"JSON 合法 + 测试全绿"≠ "功能正确"**。本次 `ErpPickerSchemaContractTest` 第一版断言「pickerDialog 存在 + loadAction 存在 + 无 AMIS 关键字」——146 picker 全绿，但 valueKey 全部错位、93% URL 拼了 null。结构性测试通过率与运行时可用性完全脱钩。
2. **plan-audit 审查的是计划一致性，不是契约保真度**。三轮 plan-audit 都核对了"计划内部自洽 + 与先例裁决一致"，没有一个子代理去 grep nop-chaos-flux 渲染器源码核对实际字段名。审计者没有义务补这个盲区——**生成侧开发者的义务是打开消费端源码读一遍实际读取逻辑**。

## 防御机制（强制清单）

生成或修改跨仓库消费的 schema 前，逐项执行：

1. **打开消费端源码，找到 schema 解析点**（本例：`picker-renderer.tsx:56-59` 逐行读 props；`picker-helpers.ts:81` 看 fallback 链 `args.valueKey ? row[args.valueKey] : row.value`）。确认三件事：字段名的精确拼写、缺失时的 fallback 行为、取值语义（URL? 枚举? 嵌套对象?）。
2. **契约写成对照表**（AMIS 键 ↔ Flux 键逐行映射），进文档（现挂 `nop-entropy/docs-for-ai/02-core-guides/flux-rendering.md §picker 字段级 schema 契约`），禁止凭记忆或类比 AMIS 文档推断。
3. **深度断言进契约测试**：不仅查"键存在"，还查"禁止键不存在"（valueField/labelField）+ "取值有效性"（URL 不含 literal 'null'、枚举值在合法集内）。参考 `ErpPickerSchemaContractTest.isCompliant` 最终版。
4. **抽样落盘人工复核**：把生成的 JSON dump 到文件，脚本遍历统计违规分布（本例发现 136/146 null URL 的就是这一步）——聚合断言会掩盖"大部分坏、少部分好"的局部缺陷。

## 真实案例

| 缺陷 | 结构性测试为何漏过 | 深度断言如何捕获 |
|---|---|---|
| delta xlib 输出 `valueField`/`labelField`，Flux 只读 `valueKey`/`labelKey` | 断言只查 pickerDialog/loadAction 存在；valueField 是"多余键"不影响存在性检查 | F4 断言「containsKey("valueField") → violation」+ 对照渲染器源码 L56-57 |
| bizObjName 从列元数据派生返回 null → `'@query:' + null + '__findPage'` | loadAction 存在即通过；URL 内容从未校验 | F3 断言「url.contains("null") → violation」+ dump 抽样统计出 136/146 分布 |

## 与既有 lesson 的关系

- 与 Lesson 13（基线陈旧）互补：13 防"引用过期事实"，本篇防"引用未验证的契约"。共同根因都是**把未重验的断言当前提**——13 的对象是文档断言，本篇的对象是接口契约。

## 流程修正（配套）

AGENTS.md 已有的路由（非平凡平台交互前读 docs-for-ai）在本次 v1 计划中未被充分执行——controlLib 自动重写机制当时已记载于 `flux-rendering.md:36`。涉及 xlib/controlLib/控件匹配链的问题，第一步固定动作：`grep -rn <关键词> ../nop-entropy/docs-for-ai/02-core-guides/{flux-rendering,frontend-rendering-pipeline}.md`，再动手写计划。
