# 页面策略与视图设计规范

> 依据 Nop Platform `view-and-page-customization.md` + `page-dsl-pattern-catalog.md` + `admin-page-development-roadmap.md`。

## 核心原则

1. **CRUD 页面用 codegen 生成**：标准实体（18 模块 ~145 实体）的列表/表单页由 `nop-cli gen` 从 ORM 模型生成，手写层仅在需要时做 Delta 定制。
2. **复杂业务页面手写**：凭证录入、库存移动确认、排产甘特图等复杂交互页面，手写 `view.xml` + `page.yaml`。
3. **菜单结构在 action-auth.xml 定义**：已在各模块 `erp-{xx}.action-auth.xml` 中定义 TOPM/SUBM/FNPT 三级菜单。
4. **AMIS 作为默认前端框架**：所有业务页面使用 AMIS 渲染，Nop 平台提供 `component="AMIS"` 支持。

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
          url="/erp/crm/pages/ErpCrmLead/main.page.yaml" component="AMIS"/>
```

URL 格式规范：`/erp/{appName}/pages/{EntityName}/main.page.yaml`

## 代码生成 vs 手写边界

| 页面类型 | 生成方式 | 定制方式 |
|---------|---------|---------|
| 标准 CRUD 列表 | `nop-cli gen` 生成 `_view.xml` 和 `page.yaml` | `x:extends` 继承生成产物，覆盖字段/布局 |
| 标准 CRUD 表单 | 嵌入 `_view.xml` 的 form | `view.xml` 中 delta 覆盖 form 段 |
| 复杂表单（凭证/移动单） | 手写 `view.xml`，不依赖生成 | 纯手写 |
| Dashboard | 手写 `page.yaml` | AMIS 页面 DSL |
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

## 参考

- `nop-entropy/docs-for-ai/02-core-guides/view-and-page-customization.md`
- `nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md`
- `nop-entropy/docs-for-ai/03-runbooks/admin-page-development-roadmap.md`
- `nop-entropy/docs-for-ai/03-runbooks/build-admin-workspace-page.md`
- `nop-entropy/docs-for-ai/03-runbooks/choose-entity-bizmodel-processor.md`
