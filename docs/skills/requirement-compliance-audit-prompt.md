# 需求-实现符合性审计提示（Requirement Compliance Audit）


> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。


对 mission `requirement-compliance`（需求→实现符合性审计，从需求真相源出发逐模块逐功能点核对运行时行为）做五级追踪 + §4 三判据核验时使用此提示。

**本技能是 `docs/audits/requirement-compliance-methodology.md` 的可路由入口**——方法论主体（§1 五级追踪矩阵 / §2 P0-P2 分级判据 / §3 完整枚举纪律 / §4 真相源层级与冲突裁决 / §5 修复义务与保护区域暂停协议 / §6 报告 9 段落骨架 / §7 arm-index 命名衔接 / §8 过程纪律自检 / §9 真相源冻结条款 / §10 MR0 即时通道 + MR1 展开器机制）与真相源冻结条款以 methodology 为准。本文件不重复方法论主体，只做路由与必需输入声明。

**使用场景**：需求→实现五级追踪矩阵审计（L1 use-cases → L2 owner doc → L3 代码路径 → L4 测试断言 → L5 运行时行为）+ §4 三判据核验（plan-audit 通过 / owner doc 显式 documented simplification + 人工批准痕迹 / product-scope 范围裁剪登记）+ 方案 B 关闭项复查 + successor 触发条件复查。

**不使用场景**：
- audit-remediation 的 doc↔code 文本一致性审计 → 已由 MA1-MA7 收口（methodology §去重协议），本审计不复跑。
- 单一对象的窄审计（ORM 规范 / 代码质量 / 状态机设计）→ 用 `docs/skills/` 下对象级提示（`orm-model-audit-prompt.md` / `code-quality-audit-prompt.md` / `state-machine-business-review-prompt.md`）。
- 单域代码层行为失败模式扫描 → 用 `behavioral-failure-mode-scan-prompt.md`（catch 吞咽 / dict 死状态 / 调度链断裂 / 守卫散点）。
- 需求本身的修订 → 属 mission 0.2 范围，须经人工批准（methodology §真相源冻结条款）。

**必需输入**：
- L1 use-cases：目标域 `docs/design/<domain>/use-cases.md`（UC 功能契约真相源，验收标准**逐字引用禁止转述**）
- L2 owner docs：目标域机制 / 状态机 / 跨域协作契约（**设计参考非真相源**——与 L1 冲突时以 L1 为准）
- L3 code：`module-<domain>/erp-<short>-service/.../Erp<Domain>*BizModel.java#<method>`（方法锚点 + 关键行为断言；行号仅写时实测导航，跨域调用链须列全）
- L4 tests：单测 `Test*.java#<method>` + E2E `tests/e2e/<domain>/<spec>.spec.ts#<describe>`（注明断言强度）
- L5 运行时：复用既有 MA2 报告已证实行为 + `tests/e2e/` + 临时探针（仅 MA4 展开项）
- `docs/requirements/product-scope.md`（顶层范围真相源）
- `docs/audits/arm-index.md`（finding 真相源，复用 or 新增裁决）

**预期输出**：methodology §6 的 9 段落报告骨架（需求契约原文 / 实现证据 / 测试证据 / 运行时行为证据 / 符合性结论 + 五级追踪矩阵 / arm-index 衔接 / 静态存疑点清单 / 过程纪律自检段 / 与 MA2 报告差异增量声明）+ arm-index 衔接（P1-RC-xxx 命名 + 「复用 or 新增」裁决 + 双向可追溯）。

```text
阅读以下文件并严格按 methodology 执行（methodology 是主体，本提示不重复）：

- `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §归档规范 + §自检清单）
- `docs/discussions/2026-08-02-1700-requirement-implementation-compliance-audit.md`（Q1/Q4 裁决原文）
- `docs/backlog/requirement-compliance-roadmap.md`（工作项定义）
- 目标域 use-cases.md + owner doc + ORM 模型 + 代码 + 测试 + 既有 MA2 报告

执行步骤（按 methodology §1-§10）：建五级追踪矩阵（L1 验收标准逐字引用禁止转述）→ 逐 UC 符合性裁决（§2 取最高原则；Q4 例外通道禁令——P0/P1 禁方案 B / 「技术不可行」降级，唯一出口=需求本身不合理经人工批准改 product-scope）→ 完整枚举（§3 禁抽样）→ 冲突裁决（§4 owner doc 与需求冲突一律以需求真相源为准；三判据 (i)→(ii)→(iii)，AI 自写不算）→ 保护区域暂停协议（§5 ORM/会计过账/数据删除 ask-first）→ 9 段落报告（§6 落盘前自查）→ arm-index 衔接（§7 P1-RC-xxx + 「复用 or 新增」裁决，禁止未比对新建）→ 过程纪律自检（§8 checker=纯 reporter 真正门控在 CI）→ 真相源冻结（§9 doc 分歧记入报告不直接改真相源）。

裁决输出：`passes requirement-compliance audit` 或 `needs revision`，附 9 段落报告 + 五级追踪矩阵 + finding 清单（P1-RC-xxx）+ arm-index 衔接表。
```

## 声明

本技能是 `docs/audits/requirement-compliance-methodology.md` 的可路由入口。方法论主体（§1-§10 全部条款）、真相源冻结条款、Q1=(c) / Q4=(a) 裁决、arm-index 命名衔接规则均以 methodology 为准；本文件冲突时以 methodology 为唯一真相源。本技能存在的理由：methodology（446 行）作为审计契约文件体量大，skill 注册表需要一个薄壳入口让 audit-remediation MR2+ 经 `docs/skills/` 路由复用 §4 三判据核验（NG2：不把 methodology 全文搬进 skills）。
