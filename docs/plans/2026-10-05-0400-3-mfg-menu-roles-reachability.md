# 2026-10-05-0400-3 mfg 菜单角色可达性——运行时探针定案（探针先行，可能撤销）

> Plan Status: cancelled
> Last Reviewed: 2026-10-05
> Source: docs/analysis/2026-10-05-0353-perf-ux-round2-increment-analysis.md §3.2-F6 + §4 批次 3
> Related: USC-02a（roles 批量种子批，D3 镜像规则来源）
> Audit: required
> Protected Zone Note: `auth/permissions` = plan-first（owner doc + tests，见 ai-autonomy-policy §保护区域表）。**auth 保护区禁止无缺陷依据的授权面变更**——本计划以运行时探针为门控，缺陷不成立则撤销、不实施任何授权修改。

## Current Baseline

- 静态字面事实（实核）：`module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/auth/erp-mfg.action-auth.xml` 中 `ErpMfgMrpPlan-main`（:105）与 `ErpMfgForecast-main`（:132）页面菜单 roles="生产主管"，其子 FNPT roles 不同：6 个规划员 FNPT（runMrp/releasePurchaseRequest/releaseWorkRequest/releaseSubcontractRequest/approve/cancel，roles="生产计划员"，:107-121/:133-139）+ 3 个管理员 FNPT（reverseApprove ×3，roles="管理员"，:64-65/:90-91/:188-189，父页面 roles 不含管理员）。
- **缺陷前提存在假阳性高风险（草案审查 round1 BLOCKER 实证）**：平台 `SiteCacheDataBuilder.cascadeResourceToRoles`（nop-auth/nop-auth-service `.../sitemap/SiteCacheDataBuilder.java:221-231`）对 `authCascadeUp`（`site.xdef:17` 默认 true（生成类 SiteResourceBean 承载）；mfg 文件无显式关闭）资源把子 roles **递归 UNION 进所有祖先**；`SiteMapProviderImpl.filterAllowedMenu`（:210-235；enable-action-auth=false 时完全不执行过滤——%dev 剖面 getSiteMap 即全树）基于级联后集合逐节点过滤。按此机制，规划员 FNPT roles 会级联使 `ErpMfgMrpPlan-main`/`ErpMfgForecast-main`/`mfg-mrp` 组对生产计划员可见——「UI 路死授权」可能不成立。
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

Status: completed
Targets: 无文件改动（证据落 `_tmp/`）
Skill: browser-use:control-browser

- Item Types: `Proof | Decision`

- [x] Add（前置）: 新增生产计划员测试账号（round2 审查实证：现种子无该角色账号）：按 `_helper.ts` E1.2 扩展点三处机械追加（nop_auth_user.csv userId=22 + nop_auth_user_role.csv 生产计划员 + ROLE_ACCOUNTS 常量），不触生产语义。
- [x] Proof: ~~%dev REST 探针~~（**round1 审计 BLOCKER 更正：该剖面 enable-action-auth=false，getSiteMap 返回未过滤全树，不构成可见性证据**——原始记录留 `_tmp/` 并标注）；**权威证据 = auth-ON E2E 断言**：e1-2-menu-filter.smoke 增 test (f)——生产计划员 VISIBLE ErpMfgMrpPlan-main/ErpMfgForecast-main/mfg-mrp（FNPT cascade-up）且 erp-fin/erp-sal 不可见（过滤剖面自证），spec 全量 **5/5 绿**。
- [x] Decision: 取 **(i) 页面可见 → F6 判假阳性**：撤销修复意图；分析报告 F6 行修正（撤销 + auth-ON 证据归因）；roles-and-permissions.md 登记「可见性判定方法注记」（级联模型权威 + %dev 全树假象警示）；本计划转 **cancelled**。Phase 2A/3 修复路径不执行；planner 账号种子 + spec (f) 永久断言为本计划交付物。

Exit Criteria:

- [x] 探针证据落盘（角色视角 SiteMap 响应记录）
- [x] 三向 Decision 落盘（含后续阶段的继续/终止指令）

### Phase 2A - （缺陷证实路径）修复 + 护栏

Status: not executed（Phase 1 裁决 (i)——缺陷不成立，按计划门控整段不执行）
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

Status: completed
Targets: 分析报告 + owner docs + 全局验证
Skill: none

- Item Types: `Proof`

- [x] Proof: `mvn clean install -DskipTests` BUILD SUCCESS（build14，含种子账号追加）；auth xml 零改动（撤销路径无代码变更）。回归：e1-2-menu-filter.smoke 4/4。
- [x] Proof（文档）: 分析报告 §3.2-F6 撤销回填；roles-and-permissions.md「可见性判定方法注记」登记完成。

Exit Criteria:

- [x] 构建绿或无代码变更声明在案
- [x] 报告回填 + 教训登记完成

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_60688ff5) — 1 BLOCKER / 2 MAJOR / 3 MINOR：B1 缺陷前提与平台 cascade-up 级联机制矛盾且零运行时验证（SiteCacheDataBuilder.cascadeResourceToRoles:221-231 + 本仓 e1-2-menu-filter M2.8 实证 + owner doc :172 通道）→ 可能构成无缺陷依据的授权面扩张；M2 护栏字面子集断言首日即红（3 对管理员 reverseApprove 既有违例）且非平台真实规则；M3 Fix 范围在两种机制模型下都不成立；m4-m6（Forecast cancel FNPT 漏列/裁决 (a) 缺 owner doc 依据/Related 误引 USC-03）。
- Independent draft review iteration 2: needs revision→定点修订 (agent_e9902535) — 4/5 维 accept，本计划余 2 处一行级：新问题 1（生产计划员探针账号不存在——种子 user/user_role CSV 与 ROLE_ACCOUNTS 均无 planner）→ Phase 1 增前置 Add 项（E1.2 扩展点三处机械追加）；新问题 2（Phase 2A Prereqs「与 Phase 3 整体不执行」与 Phase 3「两路径共用」矛盾）→ 已改「本阶段不执行，撤销路径文档回填由 Decision (i)+Phase 3 共用项承载」。修订后按审查意见免第三轮全文重审。

## Closure Gates

- [x] 范围内行为完成（探针定案 + 撤销路径文档回填 + planner 账号种子/探针证据交付）
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

Status Note: **cancelled（撤销路径）**——Phase 1 运行时探针定案 F6 为假阳性（cascade-up 级联使规划员实际可见 MrpPlan/Forecast 页面；证据 `_tmp/2026-10-05-0400-3-probe/`），按计划门控撤销修复意图。auth 零改动（保护区纪律：无缺陷依据不扩授权面）；交付物 = planner 测试账号种子 + 探针证据 + roles-and-permissions.md 方法注记 + 分析报告 F6 撤销回填。Gate 项「独立结束审计」以本 cancelled 计划的探针证据与文档一致性经独立子代理复核为准（见 Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 round1 agent_b58a89c7（NEEDS REVISION）/ round2 agent_749b3c6d（**ACCEPT**，独立复跑 spec 5/5 含 test (f) 于自管 fresh-DB %test server）
- Evidence: `_tmp/2026-10-05-0400-3-probe/`（probe-verdict 诚实重写版 + %dev 探针原始记录标注）+ `tests/e2e/negative/e1-2-menu-filter.smoke.spec.ts` test (f)（auth-ON 权威断言）+ roles-and-permissions.md 方法注记

Follow-up:

- e2e-runbook 已知问题节可登记 approval mobile spec 脏库不可重入事项（plan 2 审计 M-6 移交，非本计划范围）
