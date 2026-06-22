# AGE 实践差距审计提示

当项目已复制或部分采用 Attractor-Guided Engineering 模板，但团队需要查看实时实践与预期 AGE 工作流仍有差距的地方时使用此提示。

典型触发条件：

- 遗留项目在交付过程中开始使用 AGE
- 发生了多轮文档结构、路由或指南提示更改，怀疑存在漂移
- 仓库似乎有一些 AGE 文件，但工作仍然从请求直接跳转到代码
- 人工希望在收紧自主权或流程规则之前获得具体的迁移基线

## 审计提示

```text
您正在审计当前项目的实际工作实践与预期的 Attractor-Guided Engineering 基线之间的差距。

您的工作不是要求模板完美一致。区分：
- 有意的项目特定定制
- 当前阶段可接受的部分 AGE 推出
- 明显的工作流漂移，会产生交付风险
- 占位符或过时模板内容，使 AGE 看起来已采用但实际上未运行

## 步骤 1 - 阅读 AGE 基线

至少阅读：
- `AGENTS.md`
- `docs/index.md`
- `docs/process/application-development-workflow.md`
- `docs/context/project-context.md`
- `docs/context/ai-autonomy-policy.md`
- `docs/context/codebase-map.md`
- `docs/context/source-of-truth-and-precedence.md`
- `docs/skills/README.md`
- `docs/backlog/README.md`（如果存在）
- `docs/plans/00-plan-authoring-and-execution-guide.md`（如果存在）
- `docs/audits/00-audit-execution-guide.md`（如果存在）
- `docs/logs/index.md`
- `docs/analysis/README.md`
- `docs/context/project-context.md` 引用的活动需求、owner doc 和活动计划（如果存在）

还要检查足够的实时仓库结构，以验证记录的工作流是否实际被使用。

## 步骤 2 - 比较声称的流程 vs 实时证据

检查这些领域中实际使用、部分使用或未使用的证据：

1. 上下文纪律
- `project-context`、自主权策略和代码库映射是否填充了真实项目值还是仍然是占位符？
- 代理是否有足够的实时路由信息来安全行动？

2. 真相源纪律
- 需求、设计、架构、计划、日志、bug、测试、分析和回顾是否用于其预期的所有权边界？
- 重大决策是否仅存在于聊天中或分散在错误的目录中？

3. 需求和设计流程
- 工作是否在需要时通过 `input -> requirements -> design/architecture` 移动？
- 团队是否直接从原始请求或原型跳转到实施？

4. 任务路由和技能选择
- 活动任务是否显示明确的任务分类和 owner-doc 路由？
- 技能是否用作可复用方法而非替代需求或设计真相？

5. 规划纪律
- 对于非平凡工作，规划触发器表明应该有计划时是否存在计划？
- 阶段、证明要求、关闭门控和技能选择是真实的还是仅占位符？

6. 审计纪律
- 创建的计划是否显示真实的计划审计和结束审计证据？
- 在缺少审计的地方，遗漏是低风险且可解释的，还是真正的流程差距？

7. 验证纪律
- 验证命令是否真实？
- 日志、测试笔记或更改的代码是否显示验证实际正在运行？

8. 持久记忆纪律
- 重大工作完成后是否存在日志？
- 工作流表明应该存在的地方是否存在 bug、分析笔记或回顾？

9. 模板适应质量
- 复制的模板是否已针对真实项目进行定制，还是仍然存在误导性的通用内容？
- 对文档结构或提示的重复调整是否被捕获为稳定指南，还是反复的临时修复？

## 步骤 3 - 诚实地分类每个差距

对于与 AGE 的每个显著差异，将其分类为：
- `intentional-customization`
- `acceptable-partial-adoption`
- `operational-gap`
- `stale-template-drift`
- `missing-evidence`

除非存在具体风险，否则不要将差异报告为缺陷，例如：
- AI 可能对过时或占位符文档采取行动
- owner-doc 边界不清楚
- 规划或审计义务被静默跳过
- 重要的交付知识没有落地到持久文件中
- 仓库表明 AGE 合规但证据缺失

## 步骤 4 - 将分析笔记写入 docs/analysis

创建带日期的分析文件：
- `docs/analysis/YYYY-MM-DD-HHmm-age-practice-gap-audit.md`

如果当天已存在同名文件，则附加简短的区分标签，例如：
- `docs/analysis/YYYY-MM-DD-HHmm-age-practice-gap-audit-round-2.md`

文件必须包含这些部分：

1. `# AGE Practice Gap Audit - YYYY-MM-DD`
2. `## Scope`
- 仓库或分支上下文（如果相关）
- 读取了哪些基线文档
- 采样了哪些实时区域

3. `## Executive Summary`
- 3-6 个项目符号
- 包括仓库是否基本对齐、部分采用或尚未在 AGE 下运行

4. `## Alignment Matrix`
- 表格列：
`| Area | Expected AGE Practice | Current Evidence | Status | Classification | Risk | Next Action |`
- 至少涵盖：上下文、路由、需求、设计/架构、规划、审计、验证、日志、可选层、模板定制

5. `## Findings`
- 首先返回发现，按严重性排序
- 对于每个发现，包括：
  - 标题
  - 受影响的文件或区域
  - 当前差距
  - 为什么这在操作上很重要
  - 是漂移、部分采用还是有意定制
  - 建议的最小纠正切片

6. `## Healthy Deviations`
- 列出可接受的项目特定适应差异，不应盲目"修复"

7. `## Suggested Migration Order`
- 按顺序列出最小的实际下一个切片
- 优先选择工作流启用修复，然后再进行广泛文档扩展

8. `## Evidence Reviewed`
- 检查的关键文件和仓库区域的简明项目符号列表

## 步骤 5 - 返回简明的面向用户的摘要

保存文件后，返回：
- 输出路径
- 前 3-5 个差距
- 最重要的可接受定制（如有）

如果没有发现主要差距，明确说明并记录剩余风险或证据限制。
```

## 定制说明

将此模板复制到真实项目后：

- 如果通用 AGE 基线文件路径不同，将其替换为项目最强的流程锚点
- 在差距检查清单中添加项目特定的保护区域和常见失败模式
- 根据项目年龄、迁移阶段和团队成熟度调整可接受的部分采用标准
- 如果项目以相同方式反复未能通过此审计，将重复出现的差距提升为检查清单、可 lint 规则或更强的上下文指南