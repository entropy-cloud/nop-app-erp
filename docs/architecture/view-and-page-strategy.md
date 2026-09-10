# 页面策略与视图设计规范

> 依据 Nop Platform `view-and-page-customization.md` + `page-dsl-pattern-catalog.md` + `admin-page-development-roadmap.md`。

## 核心原则

1. **CRUD 页面用 codegen 生成**：标准实体（18 模块 ~145 实体）的列表/表单页由 `nop-cli gen` 从 ORM 模型生成，手写层仅在需要时做 Delta 定制。
2. **复杂业务页面手写**：凭证录入、库存移动确认、排产甘特图等复杂交互页面，手写 `view.xml` + `page.yaml`。
3. **菜单结构在 action-auth.xml 定义**：已在各模块 `erp-{xx}.action-auth.xml` 中定义 TOPM/SUBM/FNPT 三级菜单。
4. **flux 作为唯一前端渲染模式（flux-only）**：本项目界面设计全面转向 `nop-chaos-flux` 渲染，**后续不再考虑 AMIS 实现**。所有业务页面以 flux 渲染，菜单资源 `component` 一律 `"FLUX"`，服务器端 `nop.web.render-mode=flux` 强制生效。AMIS 仅作为迁移期历史残留（见 `docs/plans/2026-08-03-1232-{1..5}-*.md` 的豁免清单与重写计划），不作为新页面目标。

## 文件层次结构

每个模块按 codegen 产物的三层结构组织：

```
erp-{xx}-web/src/main/resources/_vfs/erp/{xx}/
├── auth/
│   ├── _erp-{xx}.action-auth.xml     # 代码生成（禁止手改）
│   └── erp-{xx}.action-auth.xml      # 手写定制（x:extends 继承生成）
└── pages/
    ├── dashboard/main.page.yaml       # 域看板（手写）
    ├── ErpXxxEntity/
    │   ├── _gen/
    │   │   └── _ErpXxxEntity.view.xml # 生成的 CRUD 视图
    │   ├── ErpXxxEntity.view.xml      # 手写 Delta 定制
    │   ├── main.page.yaml             # 主页入口
    │   └── picker.page.yaml           # 选择器页
    └── special-flow/                  # 复杂业务流页面
        └── main.page.yaml
```

### 每实体页面类型（默认 codegen 产出）

| 页面类型 | 文件名 | 说明 |
|---------|--------|------|
| 主列表 | `main.page.yaml` | 列表页，含搜索/筛选/分页/批量操作 |
| 表单 | 嵌入 main.page.yaml | 新建/编辑表单，AMIS CRUD 内置 |
| 选择器 | `picker.page.yaml` | 弹出选择当前实体 |
| Dashboard | `dashboard/main.page.yaml` | 域概览看板 |

## 菜单到页面的映射

`erp-{xx}.action-auth.xml` 中每个叶子 `resource` 的 `url` 指向对应的 `main.page.yaml`：

```xml
<resource id="ErpCrmLead-main" displayName="线索/商机"
          url="/erp/crm/pages/ErpCrmLead/main.page.yaml" component="FLUX"/>
```

URL 格式规范：`/erp/{appName}/pages/{EntityName}/main.page.yaml`

## 渲染模式（flux-only，强制）

1. **菜单资源**：`erp-{xx}.action-auth.xml` 中所有叶子 `resource` 的 `component` 必须为 `"FLUX"`。由脚本工具强制翻转并保持（见 `scripts/flip-menu-to-flux.sh`，幂等，AMIS→FLUX 单向）。
2. **服务器渲染模式**：`app-erp-all` 的 `application.yaml` 固定 `nop.web.render-mode: flux`，使 `PageProvider__getPage` 对所有页面输出 flux JSON。
3. **ORM 渲染标记（codegen 持久化的关键）**：每个实体必须带 `ext:web-renderer="flux"`。codegen 模板 `nop-kernel/nop-codegen/.../orm-web/.../_{moduleName}.action-auth.xml.xgen` 按 `objMeta['ext:web-renderer'] == 'flux' ? 'FLUX' : 'AMIS'` 生成菜单资源——缺此属性时 `mvn clean install` 增量 codegen 会把生成式 `_erp-{xx}.action-auth.xml` 重新生成回 AMIS（2026-08-03 实测）。由 `scripts/flip-orm-to-flux.sh` 幂等维护（19 个 orm.xml 全 477 实体），统一构建链 `scripts/rebuild-flux-chain.sh` 在 ERP 构建前自动执行。
4. **页面模型**：标准 CRUD 页继续由 codegen（`page.yaml`）+ flux-web.xlib 生成器输出 flux JSON；`*.flux.yaml` 双文件回退优先于 `*.page.yaml`（孪生载体权威裁决见下节）。
5. **AMIS 残留**：16 占位页已由计划 `2026-08-03-1232-4` 落地（12 页 flux 实现 + 4 页 Deferred 带理由），其余手写 AMIS 页（看板/向导等，清单见 `docs/plans/2026-08-03-1232-1-flux-crud-migration.md`）按计划 1232-2/3 重写；重写前其菜单同样标记 `FLUX`，页面渲染失败属已知迁移期状态，处理路径见 `docs/testing/e2e-runbook.md` 的 flux 调试三路径。

## 孪生双载体权威裁决（M2.8 E 族正式条款，P3-CK-mfg-023-r3 族收口）

> 裁决位（M2.0 E 族裁决 §2.5 → M2.8 分片③ Decision 项落档）；消费方：M2.7 log-012-r3（注册版追踪页串页 + 修复版死产物，反向形态：page.yaml 为注册载体）。

1. **flux.yaml 为唯一运行时权威**：同一页面同时存在 `*.flux.yaml` 与 `*.page.yaml` 孪生载体时，`*.flux.yaml` 是唯一被渲染链消费的运行时真相；页面行为缺陷一律以 flux.yaml 为准修复与审计。页面级注册面（action-auth `url`）若指向 page.yaml（log-012 形态），以注册载体为显示入口、以 flux.yaml 为行为权威双轨并存——缺陷修复仍落 flux.yaml，注册串页缺陷落注册面。
2. **page.yaml 冻结为回退产物**：孪生 `*.page.yaml` 自本裁决起冻结——不再作为新功能/参数修正的维护面；其内容仅在与 flux.yaml 语义冲突且回退通道需要启用时按需对齐。**登记例外**：行为级死代码/死状态样式分支（如 pur-017-r3 three-way-match 死分支）按 M2.8 分片③随批修正语义（使其作为回退产物不再误导），但不作为常态维护义务。
3. **mfg-023-r3 遮蔽文件处置登记（二选一裁决：冻结不修）**：`erp-mfg-web/.../pages/dashboard/main.page.yaml` L190-192 CRP 日期参数错配（`dateFrom`/`dateTo` vs 表单字段 `startDate`/`endDate`）与 `bom-tree.page.yaml` 端点/结构错配——按条款 2 **冻结不修**：运行时权威 `main.flux.yaml` L170-171 已正确绑定参数，遮蔽文件缺陷登记在案（本节即裁决凭据），后续维护者不得按 page.yaml 结构改动日期过滤/端点；若未来需要启用 page.yaml 回退通道，须先按 flux.yaml 现状对齐全量参数（走独立计划）。
4. **一致性无自动校验通道（登记残余风险）**：孪生双文件无构建期一致性校验；新增孪生页面时默认视为「flux.yaml 权威 + page.yaml 冻结」组合，禁止双向手工同步（同步即漂移源）。

## 页面数据访问（/r/ REST 约定，强制）

Flux 模式下的页面数据访问**不使用 GraphQL**，全部经 REST `/r/` 端点（`POST /r/OperationName`）。view.xml / page.yaml 中的 `api`/`url` 支持 `@query:`、`@mutation:`、`@rpc:` 前缀及显式 `/r/` 路径，由浏览器壳层（nop-chaos-next `apps/main/src/services/nopRpcResolver.ts` + `http.ts`）统一转换为 REST 调用：

```text
@mutation:ErpMdMaterial__batchUpdate   →  POST /r/ErpMdMaterial__batchUpdate
@query:ErpMdMaterial__findPage?page=1  →  POST /r/ErpMdMaterial__findPage
```

配套约定（详见 `../nop-entropy/docs-for-ai/02-core-guides/flux-rendering.md`「数据访问约定（强制）：REST /r/，不使用 GraphQL」）：

- `@selection` 参数裁剪返回字段；返回完整 Bean 的服务（如 DictProvider）无需 `@selection`。
- 请求体递归过滤 `__`/`@`/`v_` 前缀键与顶层 `$` 系统键（如 `$form`）；内嵌 `$` 键（TreeBean 的 `$body`/`$type`）为业务结构，保留。
- 表单数据不隐式携带，须显式 `includeScope` 或 `data` 模板映射。

**测试含义**：浏览器层 E2E 对页面数据访问断言 REST `/r/` 调用（e2e-shared 的 `RpcClient`/`rpc()`），禁止在 flux 页面上断言 GraphQL 请求；GraphQL 断言仅限服务端集成测试或非页面路径。

## 代码生成 vs 手写边界

| 页面类型 | 生成方式 | 定制方式 |
|---------|---------|---------|
| 标准 CRUD 列表 | `nop-cli gen` 生成 `_view.xml` 和 `page.yaml` | `x:extends` 继承生成产物，覆盖字段/布局 |
| 标准 CRUD 表单 | 嵌入 `_view.xml` 的 form | `view.xml` 中 delta 覆盖 form 段 |
| 复杂表单（凭证/移动单） | 手写 `view.xml`，不依赖生成 | 纯手写 |
| Dashboard | 手写 `page.yaml` | flux 页面 DSL |
| 选择器/弹出页 | codegen 生成 `picker.page.yaml` | 按需定制 |
| 报表/图表 | codegen 或 `nop-report` 设计器 | 引用报表模板 |

## 复杂页面清单（需手写）

以下页面需要手写 `view.xml` 而非依赖 codegen：

| 模块 | 页面 | 复杂度原因 |
|------|------|-----------|
| finance | 凭证录入 | 借贷分录行动态增删、科目树选择、借贷平衡校验 |
| finance | 凭证模板配置 | 科目映射键值对、金额占位符绑定 |
| inventory | 库存移动确认 | PDA扫码、批次/序列号选择、库位树 |
| purchase | 三单匹配 | PO/Receive/Invoice 三表联查、差异高亮 |
| crm | 商机看板 | 拖拽式 Kanban 视图（`page-dsl-pattern-catalog.md` Kanban 模式） |
| crm | 活动日历 | 日/周/月日历视图 |
| cs | 工单看板 | 按状态分组 Kanban，SLA 红黄绿指示灯 |
| aps | 排产甘特图 | 甘特图可视化（`page-dsl-pattern-catalog.md` Gantt 模式） |
| logistics | 发运追踪 | 地图追踪时间线 |
| hr | 组织架构图 | 树形组织图 |
| hr | 薪酬核算 | 批量计算公式、社保/个税配置 |

## 国际化策略

- 所有界面标签在 ORM 模型中通过 `i18n-en:displayName` / `i18n-en:label` 定义
- codegen 自动从 ORM 生成 `i18n/en/_erp-{xx}.i18n.yaml`
- 手写页面的标签在 `i18n/en/erp-{xx}.i18n.yaml` 中追加（手写 Delta）
- 中文为默认语言，英文从 i18n 文件加载

## 看板/报表 AMIS 取数范式约定

看板与报表 page.yaml 中手写 GraphQL 查询的 AMIS 取数机制：

1. **`$var` 转义**：`dataType: raw` 手写 GraphQL 查询字符串里的裸 `$var`（GraphQL 变量语法）**必须**以 `${'$'}` 转义（如 `query(${'$'}periodId:Long){ ...(periodId:${'$'}periodId) }`）。原因：amis-core `dataMapping` 对含 `$` 的字符串值经 `tokenize`（模板模式单趟解析）会把裸 `$var` 当模板变量替换为空，损坏查询致 KPI 恒 0。`${'$'}` 是 YAML 双引号安全的字面 `$` 输出（amis-formula `\$` 转义变体，单趟不回扫）。`variables` 中的 `${expr}` 模板不变。
2. **不要改用 `@query:` URL 范式**：`guessDefinition` 对整数推断 `Int`、浮点 `Float`，无法产出 BizModel 声明的 `Long`/`BigDecimal`，GraphQL 校验会拒绝（`Int` 用于 `Long` 位置）。`dataType: raw` + `${'$'}` 转义是覆盖全参数类型的纯前端方案。
3. **报表渲染容器范式**：渲染 button 用 `actionType: reload target: "reportService"` 触发 form 内部的 `type: service`（name: reportService, initFetch: false），service 的 api 含 adaptor 将 `renderHtml` 返回 HTML 拍平为 `data.reportHtml`，body 为 `type: html html: "${reportHtml}"`。service 必须在 form **内部**（共享表单字段作用域；同级 service 取不到表单字段值）。**禁止**镜像旧 balance-sheet 的 `onEvent: setVariable(event.data.result)+setValue(target)` 范式——该范式运行时损坏。

## 参考

- `nop-entropy/docs-for-ai/02-core-guides/view-and-page-customization.md`
- `nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md`
- `nop-entropy/docs-for-ai/03-runbooks/admin-page-development-roadmap.md`
- `nop-entropy/docs-for-ai/03-runbooks/build-admin-workspace-page.md`
- `nop-entropy/docs-for-ai/03-runbooks/choose-entity-bizmodel-processor.md`
