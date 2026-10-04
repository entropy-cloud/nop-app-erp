# 2026-10-05-0400-3 mfg 菜单角色可达性——运行时探针定案（探针先行，可能撤销）

> Plan Status: active
> Last Reviewed: 2026-10-05
> Source: docs/analysis/2026-10-05-0353-perf-ux-round2-increment-analysis.md §3.2-F6 + §4 批次 3
> Related: USC-02a（roles 批量种子批，D3 镜像规则来源）
> Audit: required
> Protected Zone Note: `auth/permissions` = plan-first（owner doc + tests，见 ai-autonomy-policy §保护区域表）。**auth 保护区禁止无缺陷依据的授权面变更**——本计划以运行时探针为门控，缺陷不成立则撤销、不实施任何授权修改。

## Current Baseline

- 静态字面事实（实核）：`module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/auth/erp-mfg.action-auth.xml` 中 `ErpMfgMrpPlan-main`（:105）与 `ErpMfgForecast-main`（:132）页面菜单 roles="生产主管"，其子 FNPT roles 不同：6 个规划员 FNPT（runMrp/releasePurchaseRequest/releaseWorkRequest/releaseSubcontractRequest/approve/cancel，roles="生产计划员"，:107-121/:133-139）+ 3 个管理员 FNPT（reverseApprove ×3，roles="管理员"，:64-65/:90-91/:188-189，父页面 roles 不含管理员）。
- **缺陷前提存在假阳性高风险（草案审查 round1 BLOCKER 实证）**：平台 `SiteCacheDataBuilder.cascadeResourceToRoles`（nop-auth/nop-auth-service `.../sitemap/SiteCacheDataBuilder.java:221-231`）对 `authCascadeUp`（`SiteResourceBean.java:66` 默认 true；mfg 文件无显式关闭）资源把子 roles **递归 UNION 进所有祖先**；`SiteMapProviderImpl.filterAllowedMenu`（:239-266）基于级联后集合逐节点过滤。按此机制，规划员 FNPT roles 会级联使 `ErpMfgMrpPlan-main`/`ErpMfgForecast-main`/`mfg-mrp` 组对生产计划员可见——「UI 路死授权」可能不成立。
- 本仓运行时实证（同机制）：`tests/e2e/negative/e1-2-menu-filter.smoke.spec.ts` test (b)「M2.8 ct FNPT cascade-up makes erp-ct visible for 财务员」——cascade-up 使 FNPT 角色可看见祖先页面的机制在本 app 已有 E2E 级证明。owner doc `roles-and-permissions.md:172,:174` 亦以 FNPT cascade-up 为 per-entity 可见性通道。
- 原始 finding（分析报告 F6）由纯静态字面比对得出，无 SiteMap 运行时验证。
- 业务语义参考（不预设结论）：`docs/design/roles-and-permissions.md:26`（生产计划员=创建工单、提交审核、取消）与 `:156`（FNPT 前缀授权矩阵仅列 ErpMfgWorkOrder/ErpMfgBom/ErpMdMaterial）——**该矩阵不支持「生产计划员是 MRP 运行/预测维护执行者」的预设**；若修复方向为扩页面 roles，须同步修订角色矩阵本体。
- E1.2 账号池（userId 11-19 角色 account）在位，可支撑角色视角探针。

## Goals

- 以运行时证据定案 F6 是否为真实缺陷：以生产计划员角色实测 `ErpMfgMrpPlan-main`/`ErpMfgForecast-main`/`mfg-mrp` 组的实际菜单可见性。
- 缺陷成立 → 修复错配并建立防复发护栏（护栏语义按级联机制设计，非字面子集比对）；缺陷不成立 → 撤销 finding、修正分析报告、登记「静态字面比对≠运行时可见性」方法教训。两个出口都是本计划的合法交付面。

## Non-Goals

- 其余 17 域 action-auth.xml 的 roles 全量重审。
- 角色体系重构/FNPT 权限点语义变更（permissions 声明本身不动）。
- erp-sys admin 特例（USC-02a D2 已裁决，不动）。
- 在探针证实缺陷不成立时实施任何授权面变更（保护区纪律）。

## Task Route

- Type: `bug investigation`（探针定案）→ 视结果 `implementation-only change` 或 `verification or audit work`（撤销+文档修正）
- Owner Docs: `docs/design/roles-and-permissions.md`（角色矩阵与 cascade-up 通道权威）、`docs/design/manufacturing/README.md`（域用例）
- Skill Selection Basis: `bug-diagnosis-prompt`（根因证实/证伪路径——本计划 Phase 1 即其「复现」步）；`development-wisdom-gate`（自检）

## Infrastructure And Config Prereqs

- 本地 runner + E1.2 账号池测试账号（探针用）。
- 无新增外部依赖。

## Execution Plan

### Phase 1 - 运行时探针定案（门控）

Status: planned
Targets: 无文件改动（证据落 `_tmp/`）
Skill: browser-use:control-browser

- Item Types: `Proof | Decision`

- [ ] Add（前置）: 新增生产计划员测试账号（round2 审查实证：现种子无该角色账号——`nop_auth_user.csv`/`nop_auth_user_role.csv` 无 planner 映射、`tests/e2e/negative/_helper.ts` ROLE_ACCOUNTS 无该角色）：按 `_helper.ts:232` E1.2 扩展点三处机械追加（user CSV + user_role CSV + ROLE_ACCOUNTS 常量），不触生产语义。
- [ ] Proof: 启动本地 runner，以生产计划员角色账号调 `/r/SiteMapApi__getSiteMap`（或经 UI 菜单观测），断言 `ErpMfgMrpPlan-main`/`ErpMfgForecast-main`/`mfg-mrp` 组的实际可见性；同法抽测管理员 reverseApprove FNPT 的可达面。证据（响应 JSON/截图）落 `_tmp/2026-10-05-0400-3-probe/`。
- [ ] Decision: 三向裁决落盘本计划——(i) **页面可见**（预期，级联机制成立）→ F6 判假阳性：撤销修复意图，修正分析报告 F6 行 + 本计划转 cancelled 记录，并在 roles-and-permissions.md 或 lessons 登记方法教训（菜单可见性判定必须经级联模型，静态字面比对产生假阳性）；(ii) **页面不可见** → F6 证实：按级联机制实证重写机制模型（为何级联未生效——authCascadeUp 关闭点/其他过滤层），Phase 2 修复；(iii) 部分可见/其他形态 → 按实测形态具体裁决。

Exit Criteria:

- [ ] 探针证据落盘（角色视角 SiteMap 响应记录）
- [ ] 三向 Decision 落盘（含后续阶段的继续/终止指令）

### Phase 2A - （缺陷证实路径）修复 + 护栏

Status: planned
Targets: `erp-mfg.action-auth.xml`、测试
Skill: development-wisdom-gate
Prereqs: Phase 1 裁决 (ii)/(iii) 且缺陷证实。**裁决 (i) 时本阶段不执行**（计划转 cancelled；撤销路径的文档回填由 Phase 1 Decision (i) 交付物 + Phase 3 共用项承载）。

- Item Types: `Decision | Fix | Add | Proof`

- [ ] Decision: 修复方向裁决（基于 owner doc 角色矩阵实文；若扩页面 roles 则同步修订矩阵本体，记录引文）；3 对管理员 reverseApprove FNPT 的处置一并裁决（修复 / 按 owner doc 既有管理员通道语义登记豁免）。
- [ ] Fix: 按裁决修改 roles 属性。
- [ ] Add: 护栏测试——语义按**级联可见性**设计（对每对页面/子 FNPT 断言「子 FNPT roles 经级联可达其页面」），非字面子集比对（round1 MAJOR-2 教训：字面子集断言首日即被 3 对既有管理员 FNPT 打红且非平台真实规则）；既有违例对先经 Decision 处置后再断言。
- [ ] Proof: 护栏测试绿 + 复跑 Phase 1 探针确认修复后可达性符合预期 + `xmllint --noout` + `mvn test -pl module-manufacturing/erp-mfg-web` 绿。

Exit Criteria:

- [ ] 修复落地且护栏测试绿（级联语义断言）
- [ ] 探针复验与裁决预期一致

### Phase 3 - 收口（文档回填，两路径共用）

Status: planned
Targets: 分析报告 + owner docs + 全局验证
Skill: none

- Item Types: `Proof`

- [ ] Proof: `mvn clean install -DskipTests` 全量构建绿（纯探针/撤销路径无代码变更时记录「无代码变更，构建验证不适用」，以文档一致性代替）。
- [ ] Proof（文档）: 分析报告 §3.2-F6 按探针结论回填（证实修复/证伪撤销两态）；方法教训落盘（lessons 或 roles-and-permissions.md 注记）。

Exit Criteria:

- [ ] 构建绿或无代码变更声明在案
- [ ] 报告回填 + 教训登记完成

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_60688ff5) — 1 BLOCKER / 2 MAJOR / 3 MINOR：B1 缺陷前提与平台 cascade-up 级联机制矛盾且零运行时验证（SiteCacheDataBuilder.cascadeResourceToRoles:221-231 + 本仓 e1-2-menu-filter M2.8 实证 + owner doc :172 通道）→ 可能构成无缺陷依据的授权面扩张；M2 护栏字面子集断言首日即红（3 对管理员 reverseApprove 既有违例）且非平台真实规则；M3 Fix 范围在两种机制模型下都不成立；m4-m6（Forecast cancel FNPT 漏列/裁决 (a) 缺 owner doc 依据/Related 误引 USC-03）。
- Independent draft review iteration 2: needs revision→定点修订 (agent_e9902535) — 4/5 维 accept，本计划余 2 处一行级：新问题 1（生产计划员探针账号不存在——种子 user/user_role CSV 与 ROLE_ACCOUNTS 均无 planner）→ Phase 1 增前置 Add 项（E1.2 扩展点三处机械追加）；新问题 2（Phase 2A Prereqs「与 Phase 3 整体不执行」与 Phase 3「两路径共用」矛盾）→ 已改「本阶段不执行，撤销路径文档回填由 Decision (i)+Phase 3 共用项承载」。修订后按审查意见免第三轮全文重审。

## Closure Gates

- [ ] 范围内行为完成（探针定案 + 视路径的修复/撤销与文档回填）
- [ ] 相关文档对齐（分析报告 F6 回填 + 教训登记 + 若修复路径的 owner doc 矩阵修订）
- [ ] 已运行验证（修复路径：构建+触域测试+护栏+探针复验；撤销路径：文档一致性）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 其余域 roles 镜像全量重扫

- Classification: `watch-only residual`
- Why Not Blocking Closure: 级联机制下「子 FNPT roles 与父页面 roles 字面不一致」本身不构成缺陷（本计划 Phase 1 将定案该机制语义）；全量重扫仅在平台级联语义变更时有意义。
- Successor Required: `no`（触发=平台 SiteMap 级联机制变更或 USC-02a 类 roles 规则再迭代）

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <independent subagent>
- Evidence: <task id / log link>

Follow-up:

- <非阻塞跟进项>
