# 多维审计提示


> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` auto + dual-agent-approval、会计/财务/数据删除，双独立子 agent 分别批准）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。


当普通对象特定审计不够且工作必须同时跨多个维度受到挑战时使用此提示。

这是通用默认提示。复制模板后，根据项目的真实 owner docs、保护区域、验证堆栈、部署模型和已知风险区域进行调整。

```text
阅读 `AGENTS.md`、`docs/index.md`、活动需求和 owner docs、相关计划或更改区域，以及最新验证或审计证据。

审计对象是**整件工作**在其多维上下文中的表现，而非单个工件。"相关计划或更改区域"只是入口之一，不是对象的全部；不要因为存在一个活动计划就把审计压成对该计划的审查。

跨多个维度挑战工作，而非一次仅审计一个工件或仅在单一维度深挖。

反窄化自检：如果你发现自己只在一个维度上反复深挖（例如只核实模型字段、只跑测试、只读一份设计文档），你做的是**单维审计**，不是多维审计——回到维度清单，对每个维度至少给出一句裁决（含"本维度无发现"）。

至少检查这些维度。每个维度下方给出了核验方法的**方向性指引**——不是固定步骤，而是帮助你判断"这个维度的检查我做完了吗"：

- **需求正确性**：对照真实需求/原始输入/owner docs，确认审计对象声明的目标和范围不偏离。核验路径：找"承诺但没有证据"的东西。
- **owner-doc 对齐**：审计对象涉及的每一个陈述，是否有对应的 owner doc 作为依据？owner doc 本身是否有矛盾？核验路径：从审计对象的每个断言出发，反向找 owner doc 中的对应条款。
- **架构或边界影响**：审计对象是否引入了新的跨模块依赖、API 契约变更、保护区域触碰？核验路径：检查 `module-boundaries.md`、`data-dependency-matrix.md` 中的 DAG 边是否被隐式突破。
- **验证充分性**：审计对象的关闭门控是否真的可验证？每个验收标准是否有独立的证明策略（不仅仅是"跑测试通过"）？核验路径：对每个验收标准，问"如果它假了，我怎么知道？"
- **回归风险**：审计对象触及的变更是否在测试覆盖之外留下脆弱的、未经验证的路径？核验路径：寻找"仅偶然通过狭窄验证"的代码——这些地方换一个输入值就崩。
- **路由和技能选择正确性**：实现者选择的任务路由（`AGENTS.md` §任务路由）和技能（`docs/skills/`）是否与工作类型匹配？核验路径：检查计划/日志中的路由记录，问"如果换一条路由会漏掉什么？"
- **待办或自主权策略漂移**：审计对象是否无声扩大了范围、关闭了未完成的项目、或将阻塞降级为跟进项？核验路径：对比审计对象最初声明的范围与最终产出的实际边界。

不要假设模板的默认维度对每个仓库都足够。当复制的项目有保护域、集成密集型流程、安全敏感路径、受监管工作流或不寻常的部署约束时，添加项目特定维度。常见项目特定维度包括：**ORM 完整性**（关键 to-one 显示列存在性、跨模块表名前缀正确性）、**代码生成纪律**（生成文件零手改、聚合器配置完整性）、**view.xml gen-control 契约（badge 调色板 vs dict 真值）**（本项目特定维度，详见下方「项目特定维度补充（nop-app-erp）」）。

## 项目特定维度补充（nop-app-erp）

> 本节是本仓库对默认 7 维度的项目覆盖。当审计对象涉及 view.xml 前端 delta 层时，追加此维度；与默认 7 维度并列执行，不删减任何默认维度。

**view.xml gen-control 契约（badge 调色板 vs dict 真值）**

- **审计目标**：delta view 的 gen-control `<c:script>` 内 `== 'ACTIVE'` / `successVals` / `dangerVals` / `primaryVals` / `warningVals` 是否对齐本域 dict 真值。
- **典型失败模式**：
  - `== 'ACTIVE'` 死状态映射：badge 模板 `${valueProp == 'ACTIVE' ? 'primary' : 'default'}`，但本域状态 dict **无 `ACTIVE` 值**（ACTIVE 属共享 `erp/doc-status`，被域专属状态列误用）→ 永不命中 → 状态颜色恒灰。
  - 跨域调色板复制粘贴：`successVals/dangerVals/warningVals/primaryVals` 硬编码 ~40 值数组含非本域值（如 hr view 含 `MATERIAL_TRANSFERRED` / `HONORED` / `DISCOUNTED` / `ENDORSED`；pur view 含 `DELIVERED` / `SETTLED` / `IN_PROGRESS` 等销售/物流残留），且漏入本域专属值 → 死分支永不命中 + 有效值漏渲染。
- **核验方法**：对审计对象触及的每个 delta view 的 gen-control `<c:script>`，对照该列绑定的 ORM `ext:dict` dict yaml 真值，逐项核对调色板数组元素是否都在 dict 中 + dict 中是否所有「应为 success/primary/danger」的值都在调色板中。
- **不影响 label 显示**：label 经 dict `graphql:labelProp` 正确显示，仅颜色类错；纯视觉/可维护性，最坏为按钮颜色错，无活跃数据破坏（故 finding 多为 P2 watch-only）。
- **参考证据**：P2-MA4-014（finance/mfg 11 套 ACTIVE 死状态）/ P2-MA4-015（mfg JobCard/Forecast 跨域调色板）/ P2-MA4-016（pur Order/Receive/Invoice 跨域调色板）/ P2-MA4-017（inv StockMove/LandedCost ACTIVE 死状态）/ P2-MA4-019（hr 10 套跨域调色板系统性偏差）/ P2-MA4-020（crm Lead/Event/ForecastPeriod 共享 dict 错配）。
- **排除项**：P2-MA4-018（StockMoveLine `kilometer` 拼写）属属性拼写错误非 gen-control 契约漂移，不纳入本维度。

`docs/skills/README.md §已知失败模式` 中的清单（扩展后 13 项：8 项 ORM/Java 微模式 + 5 项 lessons 07-11 系统性行为失败模式——compliance 基线漂移 / closure-pending / 业财过账吞异常悬挂 / dict 死状态 / arm-index 状态不回填）应作为**维度内搜索加权**——审查到某个维度时，优先检查那些已知高发区域。但已知失败模式常常偏向某一类（如全是 ORM/Java 微模式）——不要让它们删减维度数量，只让它们调节你在每个维度内花多少精力。具体 lesson 指针见 `docs/lessons/07-11-*.md` 与 `docs/lessons/README.md`。

按严重性排序，首先返回发现。
如果发现阻塞问题，说 `needs revision` 并列出确切的文件、维度和缺失证据。
如果没有阻塞问题，说 `passes multi-dimensional audit` 并按维度列出剩余风险。
```