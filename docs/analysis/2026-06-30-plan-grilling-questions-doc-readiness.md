# Plan Grilling Questions: 文档就绪度 — 启动 P0 实现前

## Scope

- 审查对象：`docs/backlog/README.md` P0（master-data 域 BizModel 深化 + 页面定制）
- 已阅读的 owner docs / plans / code paths：
  - `docs/design/master-data/README.md` — 主数据域设计（135 行）
  - `docs/design/domain-design-guidelines.md` — 域设计指南（705 行）
  - `docs/architecture/service-layer-orchestration.md` — 服务层编排架构
  - `docs/architecture/system-baseline.md` — 技术基线
  - `docs/backlog/implementation-roadmap.md` — 实施路线图
  - `docs/backlog/README.md` — 待办事项（P0-P10）
  - `docs/context/project-context.md` — 项目状态
  - `docs/analysis/2026-06-25-1649-ai-automation-roadmap.md` — AI 自动化路线图
  - `docs/architecture/customization-capabilities.md` — 定制能力
  - `nop-entropy/docs-for-ai/03-modules/nop-task.md` — task flow 平台文档
  - `nop-entropy/docs-for-ai/03-modules/nop-rule.md` — nop-rule 平台文档
  - module-master-data/model/app-erp-master-data.orm.xml — 权威 ORM 模型

## Decision Summary

1. P0 是否需要一个正式 plan 再开始实现
2. master-data 设计文档是否足够指导 BizModel 实现
3. ErrorCode 的定义时机
4. 第一个 task.xml 示例的产出方式
5. P0 的步实现优先级

## Questions

### Q1. P0 是否需要正式 plan 再编码？

- **Question**: P0（master-data BizModel 深化）在 backlog 中标记为 `implement`（AI 可直接实现），但 `service-layer-orchestration.md` 刚定稿、task.xml 模式未经实际项目检验。是否需要一个正式 plan（含 plan audit + closure audit）再启动？

- **Why this matters**: plan audit 会发现 scope 缺口和 owner doc 不足，closure audit 确保交付质量。跳过的风险是第一次实践 task.xml 时走偏、scope 膨胀、产出不可验证。

- **Evidence**: backlog 标记 `implement` 不要求 plan；但 `service-layer-orchestration.md` 是当天（2026-06-30）新写，task.xml 模式在项目代码中尚无任何使用实例。`nop-entropy` 的 task-ext demo 有 `rule:Execute` 用法（`discount-rule.task.xml`），但本项目的 master-data 域无任何 task.xml。

- **Recommended answer**: 写一个轻量 plan（比完整 plan-audit 轻，但比直接编码重），明确 scope、文件列表、验证方式、结束标准。

- **Options**:
  - **A**（推荐）：写一个简版 plan（scope + step list + proof），省略正式 plan audit，但保留 closure audit
  - **B**：直接按 `implement` 编码，每一步遇到不确定时再问
  - **C**：写完整 plan + plan audit，最重但最安全

- **Consequences**:
  - **A**：适度轻量，first task.xml 有参考坐标，closure audit 兜底
  - **B**：最快启动，但 task.xml 第一次落地可能走偏，且 master-data 涉及跨域引用校验（`IErpMd*Biz`），scope 可能膨胀
  - **C**：最稳但最慢，P0 是个已充分设计的域，完整 plan-audit 可能过度

- **Dependencies**: 无

- **Default if no choice**: A（简版 plan + closure audit）

### Q2. master-data 设计文档是否足够指导 BizModel 实现？

- **Question**: `docs/design/master-data/README.md` 定义了核心对象、启停规则、跨域协作、关键规则，但没有明确列出每个实体需要哪些自定义 BizModel 方法（超出 CrudBizModel 默认的）、哪些方法需要 task.xml 编排、参数签名和返回值是什么。是否需要补一份 BizModel 方法清单再编码？

- **Why this matters**: 没有方法清单，AI 需要自行推断哪些操作需要自定义方法、哪些用 CrudBizModel 默认。不同推断会产出一致性差的 BizModel——有的方法用 task.xml、有的直接 Java、有的塞在 xbiz。

- **Evidence**: master-data README 的"实现落位提示"节只给出了大方向（"启停动作 → BizModel"），没指定 `MaterialBiz` 具体暴露哪些方法（`activate`/`deactivate`？`disableWithSku`？）。`domain-design-guidelines.md` 也没规定每域的 BizModel 方法命名规范。

- **Recommended answer**: 在 master-data README 中补一节"BizModel 方法清单"，列出每实体需要哪些自定义方法及其签名。增量补充而非另起文档。

- **Options**:
  - **A**（推荐）：在 `docs/design/master-data/README.md` 补 "BizModel 方法清单" 节，列明方法名、参数、返回值、是否需 task.xml
  - **B**：不补，AI 编码时自行推断，遇到不确定时问
  - **C**：另建 `docs/design/master-data/biz-model.md` 独立文档

- **Consequences**:
  - **A**：一致性最好，设计文档作为实现蓝图，AI 和人均可预期产出
  - **B**：启动最快，但同人不同 session 可能产出不一致；后续其他域也需要这个清单时又得讨论
  - **C**：信息完整但分散，README 仍需指向它，维护两个文档的同步成本

- **Dependencies**: 无

- **Default if no choice**: A

### Q3. ErrorCode 何时定义？

- **Question**: `domain-design-guidelines.md §7` 要求每域定义 `Erp{xx}ErrorCode`，但 master-data 域尚未定义。ErrorCode 应在 BizModel 编码前先定义，还是编码过程中逐步追加？

- **Why this matters**: ErrorCode 是 NopException 的核心参数。先定义则编码时可直接用 `new ErpMdErrorCode.ERR_MATERIAL_NOT_FOUND`；后定义则需要编码过程中反复切文件追加，容易遗漏或散落各处。

- **Evidence**: `domain-design-guidelines.md §7` 详细规定了 ErrorCode 命名范式（`ERR_{ENTITY}_{ACTION}_...`）和分段方案，但尚无任何 `ErpMdErrorCode.java`。

- **Recommended answer**: 在编码前一次性定义 master-data 域的完整 ErrorCode 枚举，覆盖所有可预见的业务异常。后续编码中如有新增，再增量追加。

- **Options**:
  - **A**（推荐）：编码前定义 master-data 全集 ErrorCode（参考 `domain-design-guidelines.md §7` 的分段方案）
  - **B**：编码中随用随加，不提前定义
  - **C**：先定义骨架，实现到对应方法时再展开

- **Consequences**:
  - **A**：设计完整、编码流畅，但需要先花时间枚举所有异常场景
  - **B**：灵活但容易遗漏；且 ErrorCode 定义分散在多次编码 session 中，一致性差
  - **C**：折中方案，骨架定义常量名，方法实现时填 message

- **Dependencies**: Q2（方法清单决定了哪些业务场景会抛异常，间接影响了 ErrorCode 覆盖范围）

- **Default if no choice**: A

### Q4. 第一个 task.xml 如何落地？

- **Question**: `service-layer-orchestration.md` 规定了多步骤编排方法用 task.xml，但 master-data 域的自定义方法如 `activate/deactivate` 本质是单步操作（校验 → 改状态），未必需要 task.xml。第一个实际 task.xml 应该在哪个域、哪个方法首次落地？

- **Why this matters**: 第一个 task.xml 起到"模式锚定"作用——它确立了写法规范、测试方式、Delta 定制入口。选错方法（用 task 包装单步操作）会导致过度设计；跳过 task 又丧失模式锚定的机会。

- **Evidence**: master-data 的"启停"操作是单步（状态改+校验），不适合 task.xml。`purchase` 域的"采购审批"（校验 → 规则路由 → 状态机推进 → 通知）天然多步，更适合第一个 task.xml，但 P0 是 master-data 不是 purchase。

- **Recommended answer**: master-data 的 BizModel 第一轮用 Java/xbiz 实现（因为本质单步），task.xml 在 P1（purchase/sales 域审批流）首次落地。这样既不过度设计 master-data，又为 task.xml 模式锚定选了合适载体。

- **Options**:
  - **A**（推荐）：master-data 第一圈用 Java/xbiz，task.xml 留到 P1 首落
  - **B**：master-data 强制用 task.xml 包装单步操作，作为模式预热
  - **C**：选 master-data 中的一个"非单步"场景（如关键属性变更审核流，实际涉及 nop-wf 跨域依赖），用 task.xml + call-task 实现以提前验证模式

- **Consequences**:
  - **A**：轻量，master-data 快速交付，无过度设计风险；但 P1 首次落地 task.xml 时还要再磨合模式
  - **B**：虽验证了 task.xml 可用，但单步包装 task 会增加不必要的文件数和管理成本
  - **C**：审核流确实适合 task.xml，但涉及 nop-wf 跨域依赖，远超出 P0 范围，scope 膨胀风险高

- **Dependencies**: Q2（方法清单决定了哪些方法存在"步骤"概念）

- **Default if no choice**: A

### Q5. P0 的方法实现顺序

- **Question**: master-data 有 12 个核心业务对象（Material/SKU/Partner/Warehouse/Location/UoM/Conversion/Currency/ExchangeRate/COA/Account/PaymentMethod）。P0 的实现应一次性覆盖全部对象，还是分批（先 1-2 个，固化模式后再铺开）？

- **Why this matters**: 一次性覆盖 12 个对象意味着产出周期长、审查面大、风险集中。分批先做 1-2 个对象（如 Material + SKU），把命名规范、ErrorCode、单元测试模式固化后再批量迁移，效率更高。

- **Evidence**: project-context.md 标注 P0 为 `implement`，未指定分批策略。codegen 已产出 12 个实体的空壳 BizModel。

- **Recommended answer**: 先做 Material + SKU（最核心、被引用最广的两个），固化 ErrorCode、BizModel 方法格式、测试模式，再批量铺开到其余 10 个对象。

- **Options**:
  - **A**（推荐）：Material + SKU 先发，固化模式后批量
  - **B**：12 个对象一次性全部实现
  - **C**：Material + SKU + Partner（往来单位涉及面最广的核心主数据，一起固化的参考价值更高）

- **Consequences**:
  - **A**：节奏可控，模式固化后剩余 10 个可快速批量产出
  - **B**：工作量大单一交付周期长，发现模式问题时回头改 12 个实体成本高
  - **C**：比 A 多了 Partner，参考价值更高但初期工作量接近翻倍

- **Dependencies**: Q2（方法清单）→ 决定了 Material/SKU 各有哪些方法

- **Default if no choice**: A

## Recommended Path

如按推荐答案一路选择：

1. 在 `docs/design/master-data/README.md` 补充 "BizModel 方法清单" 节（Material + SKU 先行）
2. 根据方法清单定义 `ErpMdErrorCode.java`
3. 写轻量 P0 plan 文档（scope = Material + SKU BizModel + ErrorCode + 单元测试，不包含 task.xml）
4. 实现 Material + SKU 的 BizModel 自定义方法（Java/xbiz，不用 task.xml）
5. 固化模式后批量铺开到其余 10 个对象
6. 第一份 task.xml 在 P1（purchase 审批流）首次落地
7. P0 完成后做 closure audit

## Excluded Questions

- **"是否需要补充 master-data 的 xmeta"**：codegen 已生成 `_ErpMdMaterial.xmeta` 等，属标准 CRUD 范畴，不需要特别补充。如有定制需求在保留层 `ErpMdMaterial.xmeta` 中做。
- **"是否需要先写测试用例"**：应产代码同步写，但具体测试框架用法已由 project-context.md 中的 `nop-autotest` 覆盖，不需要额外文档。
- **"task.xml 的 VFS 路径约定是否需要单独文档"**：已在 `service-layer-orchestration.md` 的"映射约定"节写清，不需要再写一份。
