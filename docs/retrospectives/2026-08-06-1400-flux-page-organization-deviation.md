# 2026-08-06-1400 flux 页面组织方式偏离回顾（complex 槽位 vs 整页直写）

## 摘要

用户方向与计划要求「form/grid/页面整体布局尽量通过 view.xml 模型定义（含 `<complex>` 四槽位）」；实际实施将 20 个复杂页全部以 page.yaml/flux.yaml **整页直写**落地，且文档被回填为「complex 槽位组合模式已落地」——与仓库实况不符。本回顾记录系统核对结论：整页直写是正确的技术通道，complex 槽位当前无法承载 flux 专有控件；文档已对齐并补记裁决。

## 原始要求

- 用户方向（2026-08-03）：「不删除 page.yaml，只新增 flux.yaml；form/grid/页面整体布局尽量通过 view.xml 模型定义；利用 xview.xdef 新增的 complex 页面定义能力」（`flux-complex-pages.md §2.5`）
- Plan `2026-08-03-1232-1/2/3/4` Goals 均写明「页面外壳经 view.xml `<complex>` 四槽位或 `<simple>`/`<tabs>` 容器定义；flux 专有控件经 page.yaml/flux.yaml 直写」
- Roadmap（`frontend-ui-roadmap.md`）未要求 complex，仅写「复杂页面以 flux DSL 编写 page.yaml/flux.yaml」

## 实际行为（实施之路）

- 20 个复杂页（F13 8 + F16 11 + 占位/未实现项）全部由页面重写计划以 **`type: page` 的 flux.yaml 整页直写**落地（`schedule-gantt.flux.yaml`、`period-close-wizard/main.flux.yaml` 等，见计划 `2026-08-03-1232-2/3/4`，E2E 全绿）。
- 生产 `*.view.xml` 中 `<complex>`、`<wizard>` 容器数量 = 0，仅测试夹具 `app-erp-all/src/test/resources/_vfs/nop/test/pages/test-flux-complex.view.xml`。
- 文档侧（`flux-complex-pages.md §2.5/§7 #9`、`frontend-ui-roadmap.md` 残留列表）自称「complex 槽位 + 槽位内嵌控件组合模式已落地；标准布局用 complex」——与实施不符。
- 计划、文档均**未记录该项偏离裁决**（违反 AGENTS.md 操作规则 10）。

## 发现（作为平台能力核对）

- `GenContainerModel` 仅分派 5 种标准容器（crud/simple/tabs/wizard/group，`flux-web.xlib:45-61`），对其他类型直接抛 `nop.err.web.unknown-page-type`。
- `container_simple.xpl` 将 `<simple>` 槽位渲染为**整 `<form>`**，不存在 §2.5 设想的 `beforeForm`/`afterForm` 自定义控件透传点（全库 grep 0 匹配）。
- 20 个复杂页主体控件全部为 flux 专有控件（gantt/kanban/calendar/timeline/tree/diff-view/wizard/loop/steps/collapse），无法进入 complex 槽位；能被槽位承载的仅有 2-3 字段的筛选 form，收益为零。
- 测试夹具 `test-flux-complex.view.xml` 槽位仅含 simple/crud，无自定义控件嵌入用例——「已解决」声明无平台证据支撑。

## 决策/结论

1. **不 rewrite 为 complex**：当前整页直写是平台能力边界下的正确实现，页面已 flux 渲染且 E2E 全绿；重写需先做 nop-entropy 侧任意节点透传能力（外部仓库保护区域，须人工批准），成本高、风险大、用户可见收益≈0。
2. **文档对齐为实况**：`flux-complex-pages.md` §2.5/§7 #9、`frontend-ui-roadmap.md` 已修正为「复杂页整页直写；complex 槽位仅承载标准容器、生产 0 使用」并登记 successor 触发条件（平台 arbitrary-node 透传落地或出现高复用「筛选区+状态区」页面）。
3. **流程改进项**：计划未实现的"用户方向"不得在文档中回填为「已解决」；实施选择设计逃生通道时应先记录裁决再写入文档。

## 动作清单

- [x] `docs/design/flux-complex-pages.md` §2.5 设计分层表 + 用户方向注记 + 落地裁决段落更新
- [x] `docs/design/flux-complex-pages.md` §7 #9 状态更正（能力缺口 + 无平台依据 + successor 触发条件）
- [x] `docs/backlog/frontend-ui-roadmap.md` AMIS 退役路径 + 残留 successor 列表更正（1232-2/3/4 已交付）
- [x] 本回顾已建 + `docs/logs/2026/08-06.md` 日志待更新

## 证据文件

- `module-*/.../*.flux.yaml` ×20（整页直写、data-source + 专有控件）
- `app-erp-all/src/test/resources/_vfs/nop/test/pages/test-flux-complex.view.xml`
- nop-entropy：`nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-web.xlib`（GenContainerModel 分派）、`flux-web/container_simple.xpl`、`flux-web/page_complex.xpl`