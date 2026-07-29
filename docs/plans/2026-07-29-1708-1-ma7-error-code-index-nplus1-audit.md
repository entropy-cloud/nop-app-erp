# 2026-07-29-1708-1-ma7-error-code-index-nplus1-audit MA7 错误码完整性 + 索引完整性 + N+1 查询抽样审计（A7.1 + A7.2 + A7.3）

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MA7（A7.1 / A7.2 / A7.3）
> Related: plan `2026-07-29-1708-2`（A7.4 CI/guard 持续激活验证，同批 MA7）；plan `2026-07-27-1015-1`（M0 锚点）
> Audit: required

## Current Baseline

- MA6（安全层审计）已达 `ready`（A6.1-A6.4 里程碑完成，待独立 closure audit 转 `done`）；MA7（运维与性能层审计）4 工作项全部 `todo`。本计划覆盖前 3 项（A7.1 错误码完整性 + A7.2 索引完整性 + A7.3 N+1 查询抽样）；A7.4 CI/guard 验证由 plan `2026-07-29-1708-2` 覆盖。三者均为「代码层质量/性能审计」共享 `open-ended-audit-prompt.md` skill，产同形态交付物（审计报告 + arm-index finding → 目标 MR3），按 authoring guide rule 14 合并为单 plan 的 3 个阶段。
- **A7.1 错误码现状**（实仓 grep 2026-07-29）：全域 service 层 752 处 `throw new NopException`（dao 层 0）；绝大多数直接引用 `Erp*Errors.ERR_*` 常量（类限定），约 10-12 处经 static-import 或变量传递 ErrorCode 常量；4 处原生 `RuntimeException/IllegalArgumentException/IllegalStateException/Exception`；0 处 `new NopException("字面量")`；0 处 `new NopException()`（无参）。20 个 `Erp<Domain>Errors` 定义文件已存在（19 域各 1 + finance 额外 `ErpFinPostingErrors`），owner doc `domain-design-guidelines.md §七`（§7.1 命名空间 `erp.err.<domain-short>.<name>` + §7.2 编码规则 + §7.3 使用规范「禁止 `throw new RuntimeException`」）。
  - **审计须裁决的差距**：约 10-12 处 static-import/变量传递的 ErrorCode 是否全部为合规常量（非字面量/非 null）；4 处原生异常是否为合法平台/技术异常（非业务异常）；已定义 ErrorCode 是否全部注册到 i18n 资源文件；是否存在业务异常吞掉（catch 后仅 log 不重抛）导致 ErrorCode 不可观察。（注：精确的构造形态分布由 Phase 1 grep 重新建立权威计数，不依赖起草期近似值。）
  - **审计须裁决的差距**：S 级域（finance/mfg/hr/assets）列表查询是否在循环中逐条加载关联实体（经 `daoFor().getEntityById` 或 ORM 关系 getter）；是否存在批量加载（`findAllByQuery` + 内存组装 / DataLoader / `batchSize` 配置）替代；列表查询是否触发延迟加载集合导致 N+1。A4 代码质量审计已抽样过 BizModel 但未系统评估 N+1。
- 验证基线：`mvn clean install -DskipTests` 全绿（154 模块）；`mvn test` 全绿（~2890 测试，0 failures）。
- 剩余差距：错误码完整性 / 索引完整性 / N+1 查询三维度从未被系统性审计；各自的运行时影响面（错误可观察性 / 查询性能）未评估。

## Goals

- **A7.1**：grep 全域 service+dao 核实所有 `throw new` 是否符合 `domain-design-guidelines.md §七`（NopException + ErrorCode + 中文描述），裁决 4 处原生异常合法性 + 约 10-12 处 static-import/变量传递 ErrorCode 的合规性 + i18n 注册完整性 + 异常吞掉风险。输出错误码完整性审计报告。
- **A7.2**：对 S+A 级域（finance/mfg/hr/assets + pur/sal/inv/qa/crm/projects = 10 域）ORM index 声明 vs 实际查询模式做覆盖矩阵，标记缺索引热点列 + 冗余索引 + 复合索引合理性。输出索引完整性审计报告。
- **A7.3**：对 S 级域（finance/mfg/hr/assets）BizModel 列表查询抽样 N+1 风险，标记循环内逐条加载关联实体的站点 + 评估批量加载替代可行性。输出 N+1 查询抽样审计报告。
- 注册 P0（即时通道）/ P1（目标 MR3）/ P2（watch-only）发现至 `docs/audits/arm-index.md`，与 MA1-MA6 已登记 P1 交叉去重。
- 推进 roadmap A7.1-A7.3 状态（审计产出后转 `ready`，独立 closure audit 后转 `done`）。

## Non-Goals

- 不修复错误码缺失 / 不补索引 / 不重构 N+1 查询（修复属 MR3 批量修复，由 R3.0 展开机制转化）。
- 不审计 A7.4 CI/guard 持续激活验证（见 plan `2026-07-29-1708-2`）。
- 不审计 B+C 级域索引完整性（A7.2 roadmap 限定 S+A 级域）；B+C 级域 N+1 不抽样（A7.3 限定 S 级域列表查询）。
- 不做 ORM 索引变更的执行计划分析（仅审计发现，ORM 变更修复由 MR3 具体工作项承担并走标准 plan-audit）。
- 不变更任何生产代码 / ORM / 契约（纯审计）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/domain-design-guidelines.md §七`（A7.1 ErrorCode 约定）+ 各域 `module-*/model/app-erp-*.orm.xml`（A7.2 索引权威源）+ 各域 `module-*/erp-*-service/src/main/java` BizModel（A7.3 查询模式）。
- Skill Selection Basis: 三项工作项 roadmap 均指定 `docs/skills/open-ended-audit-prompt.md`（错误码/索引/N+1 均为开放式、需主动搜索未知缺口的维度，非结构化检查清单）。加载后须先读 `docs/skills/README.md §项目定制化层`，注入本仓保护区域与验证命令。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- 三阶段均为静态 grep + 代码/ORM 抽样（需可跑 `rg`/读源码）。A7.3 N+1 评估以静态推理 + 既有测试证据为主（无性能压测设施；通过代码路径分析识别循环加载模式）。审计只读，不改代码。

## Execution Plan

> 共享审计方法（每阶段套用）：(1) 全域 grep 建立事实基线（精确计数）；(2) 按 owner doc 约定逐项裁决合规/缺口；(3) 评估运行时影响面 + 可修复性分级；(4) 与已确认 finding 交叉去重；(5) 分类 P0/P1/P2 + 登记 arm-index。

### Phase 1 - 错误码完整性审计（A7.1）

Status: completed
Targets: 全域 `module-*/erp-*-{service,dao}/src/main/java/**`（752 NopException + 4 原生异常 + 20 `Erp*Errors`）；i18n 资源文件；报告 `docs/audits/2026-07-29-1708-arm-ma7-error-code-completeness.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0 锚点

- [x] 全域 grep 精确核实 752 `throw new NopException` 的构造形态矩阵：携带 `Erp*Errors.ERR_*` 常量 / 携带字面量 ErrorCode 字符串 / `.bizError()` 链 / `.param()` 链 / 其他，逐类计数
      - Skill: `open-ended-audit-prompt.md`
- [x] 裁决 4 处原生 `RuntimeException/IllegalStateException/...` 合法性：是否为平台/技术异常（合法）还是业务异常未用 NopException（违规，须 P1 修复）
- [x] 裁决约 10-12 处 static-import/变量传递 ErrorCode 的合规性：是否均为 `Erp*Errors.ERR_*` 常量（经 `import static` 或变量传入）而非字面量/null；确认不存在「NopException 但未携带 ErrorCode 常量」的隐式缺口
- [x] 核实 20 个 `Erp*Errors` 中已定义 ErrorCode 是否全部注册到对应 i18n 资源文件（`erp.err.*` 键存在性）；owner doc `§7.1` 命名空间合规性（`erp.err.<domain-short>.<name>`）
- [x] 评估异常吞掉风险：grep `catch.*Exception` 后未重抛的站点（业务异常被吞导致 ErrorCode 不可观察），与 MA6 权限审计「高危操作无守卫」交叉
- [x] 产出错误码完整性审计报告，分类 P0/P1/P2，更新 `docs/audits/arm-index.md`（去重 MA1-MA6）
  - Skill: `open-ended-audit-prompt.md`

Exit Criteria:

- [x] 错误码合规性矩阵产出（构造形态分布表 + 4 原生异常逐项裁决 + static-import/变量传递 ErrorCode 合规性裁决 + i18n 注册完整性表）
- [x] A7.1 P0/P1/P2 已登记 arm-index.md，且与既有 P1 交叉去重无重复

### Phase 2 - 索引完整性审计（A7.2）

Status: completed
Targets: S+A 级域 ORM（finance/mfg/hr/assets + pur/sal/inv/qa/crm/projects = 10 域，`module-*/model/app-erp-*.orm.xml`，1056 index + 187 unique-key 中对应子集）+ BizModel/Processor 查询模式；报告 `docs/audits/2026-07-29-1708-arm-ma7-index-completeness.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: Phase 1（方法复用，无数据依赖）

- [x] 对 S+A 级 10 域提取 ORM 已声明索引清单（单列/复合/unique-key），建立「表 → 已索引列集」矩阵
- [x] 对 S+A 级 10 域 BizModel/Processor grep 高频查询列（where/filter/join on：orgId / status / billType / sourceBillCode / sourceBillType / materialId / warehouseId / bizPartnerId / periodId 等），建立「表 → 实际查询列集」矩阵
- [x] 覆盖矩阵裁决：查询列未进任何索引（缺索引热点，全表扫描风险）+ 已索引列无查询命中（冗余索引）+ 单列索引应升级复合索引（高频组合列）逐项标记
- [x] 评估缺索引热点列的运行时影响面（基于查询频率与表业务规模判断：配置字典小表缺索引可接受，高频事务/明细大表缺索引 P1）
      - Skill: `open-ended-audit-prompt.md`
- [x] 产出索引完整性审计报告，分类 P0/P1/P2，更新 arm-index.md（去重）

Exit Criteria:

- [x] S+A 级 10 域索引覆盖矩阵产出（缺索引热点表 + 冗余索引 + 复合索引升级候选）
- [x] A7.2 P0/P1/P2 已登记 arm-index.md 且去重

### Phase 3 - N+1 查询抽样审计（A7.3）

Status: completed
Targets: S 级域 BizModel 列表查询（finance/mfg/hr/assets，`module-*/erp-*-service/src/main/java`，17 `.findPage/findList/findAll(` 调用站点中 S 级子集 + 列表场景遍历关联实体的其他路径）；报告 `docs/audits/2026-07-29-1708-arm-ma7-nplus1-query-sampling.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: Phase 2（查询列分析为 N+1 入口）

- [x] 对 S 级 4 域 BizModel 列表查询路径逐一标注：是否存在「列表结果循环内逐条加载关联实体」（经 `daoFor().getEntityById` / ORM 关系 getter / 显式子查询）的 N+1 站点
- [x] 评估列表查询关联加载策略：是否使用批量加载（`findAllByQuery` + 内存 join / DataLoader / `@BatchSize` / `fetch="eager"`）/ GridBean 批量装配；标记逐条加载站点
- [x] 评估延迟加载集合触发 N+1 风险（列表实体含 `@OneToMany` 延迟集合在序列化/转换时被逐条触发）
      - Skill: `open-ended-audit-prompt.md`
- [x] 对每个 N+1 站点评估可修复性（批量加载替代可行性 + 影响面），分级 P1（高频大列表）/ P2（低频小列表）
- [x] 产出 N+1 查询抽样审计报告，分类 P0/P1/P2，更新 arm-index.md（去重 MA4 代码质量已抽样站点）

Exit Criteria:

- [x] S 级 4 域 N+1 站点矩阵产出（逐条加载站点 + 批量加载替代候选 + 分级）
- [x] A7.3 P0/P1/P2 已登记 arm-index.md 且去重

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_052dbd591ffeJqIy1nTLFsVdyz`) because baseline 计数未实测核验（ErrorCode 引用数误标 669/构造「83 差距」实际为 ~10-12 static-import 项 / find 调用站点误标 49 实测 17 / BizModel 文件数误标 463 实测 378）；Phase 2 item 4 引用不存在的 EstRows 数据源（`known-good-baselines.md` 无表规模数据）；A7.2 域集遗漏 crm+projects（A1.6/A4.5 标 A 级，应入 S+A 集）；MA6 状态误标「已完成」实际为 `ready`。
- Independent draft review iteration 2: accept (`ses_052d3955affe3yk7oVPQKmNgKx`) after 全部 5 项修订落地并经实仓复核（752 NopException service 层 / 4 原生异常 service+dao / 20 Erp*Errors / 1056 index / 187 unique-key / 17 find 调用站点 / 378 BizModel 文件 / A7.2 10 域集正确 / MA6=ready / skill 存在 / EstRows 引用已移除）；rule 4/14 合并 / rule 7 typing / rule 8 skill / anti-slack / 退出标准本地化 / Closure Gates / 无自我审计 / 无占位门控 全部合规；i18n 注册缺口（0 个 erp.err.* 键）确认为 Phase 1 审计目标（非计划缺陷）。无阻塞问题。

## Closure Gates

> 本 plan 为纯审计，不改代码。`mvn test` 仅作回归基线确认（见 roadmap 横切关注点 §审计 plan 的 BUILD_VERIFY）。

- [x] A7.1 + A7.2 + A7.3 三份审计报告产出
- [x] arm-index.md 已登记本批次全部 P0/P1/P2，且与 MA1-MA6 既有 P1 交叉去重无重复
- [x] roadmap A7.1-A7.3 状态推进至 `ready`（独立 closure audit 后转 `done`）
- [x] 已运行 `mvn clean install -DskipTests`（154 模块绿）+ `mvn test`（0 failures）作回归基线确认
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态 / 阶段 / 门控 / 日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留作未勾选占位符（非人工门控占位）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### B+C 级域索引完整性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A7.2 roadmap 限定 S+A 级域；B+C 级域索引不在本批次。
- Successor Required: `no`（若 MA7 完成后需扩展，由新 plan 触发）

### B+C 级域 N+1 抽样

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A7.3 roadmap 限定 S 级域列表查询；B+C 级域不在本批次。
- Successor Required: `no`

## Closure

Status Note: EXECUTE 完成。A7.1 + A7.2 + A7.3 三份审计报告产出（`docs/audits/2026-07-29-1708-arm-ma7-{error-code-completeness,index-completeness,nplus1-query-sampling}.md`），arm-index.md 已登记本批次全部 finding（零 P0 + 1 项 P1[P1-MA7-001 ErpFinVoucherBillR 缺索引] + 6 项 P2 watch-only[P2-MA7-001~006，其中 P2-MA7-006 归并 P2-MA4-003 同族]），与 MA1-MA6 既有 P1 交叉去重无重复（P1-MA7-001 ↔ P0-MA2-018 互补不重复 / P2-MA7-006 ↔ P2-MA4-003 同族归并）。roadmap A7.1-A7.3 推进至 `ready`（待独立 closure audit 转 `done`）。回归基线确认：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test` 全绿 0 failures（纯文档审计，零代码变更，基线与 M0 锚点一致）。三阶段全部 `completed` + 退出标准全 `[x]` + Closure Gates 全 `[x]`（除「结束审计由独立子代理执行」按 authoring guide 规则 12 由独立 closure audit 子代理勾选，执行者不自查）。本计划产出已达 EXECUTE 退出条件；最终 `done` 状态转捩需独立 closure audit 通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure audit 子代理（新会话，不重用执行者上下文）
- Evidence: 实仓复核通过——(1) 三份报告 `docs/audits/2026-07-29-1708-arm-ma7-{error-code-completeness,index-completeness,nplus1-query-sampling}.md` 均存在且非 hollow（15K/12K/14K 字节，含构造形态矩阵 / 索引覆盖矩阵 / N+1 站点矩阵 + 逐项裁决 + Finding 表 + Verdict）；(2) `docs/audits/arm-index.md` MA7 块登记齐全（§报告清单 +3 行 / §MA7 汇总段 / P1-MA7-001 + P2-MA7-001~006 共 7 行 + MA7 交叉去重段，P2-MA7-006 明确归并 P2-MA4-003 同族）；(3) `docs/backlog/audit-remediation-roadmap.md` A7.1-A7.3 状态 = `ready`（行 127-129）+ §MA7 工作项详情刷新（行 249）；(4) `docs/logs/2026/07-29.md` MA7 日志条目存在（行 3-10，含任务/核心结论/finding/变更/后续五段）；(5) Phase 1-3 全 `completed` + 全退出标准 `[x]` + Closure Gates 全 `[x]`；(6) anti-hollow 复核：P1-MA7-001 引 `ErpFinPostingProcessor.findBillLinks:884-888` + `alreadyPosted:473` + `app-erp-finance.orm.xml:643-647` 实仓可定位，非占位结论。文本一致 / 反松弛 / 退出标准本地化 / Deferred honesty 全部 sustained。

Follow-up:

- 错误码/索引/N+1 修复不在此处；由 R3.0 展开机制将本批次 P1（P1-MA7-001）转化为 MR3 具体修复工作项行。
