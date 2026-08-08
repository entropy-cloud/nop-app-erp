# 技能覆盖缺口分析：RC-R1.x 计划执行期发现 vs 既有技能库

> 分析日期：2026-08-08。驱动请求：「仔细检查历史计划，写一个分析报告，确定是否有更多内容需要补充到 docs/skills 下」。
>
> **分层裁决（2026-08-08 用户修正）**：技能原则上应与具体 Nop 平台、业务应用无关，是相对通用的元信息。因此平台级知识写入 `nop-entropy/docs-for-ai/`（已落地，见 §五），项目技能只保留项目级约定（§六），业务知识归 owner docs（§七）。

## 一、方法

对 2026-08-08 批量执行的 MR1 修复计划（expander `2026-08-07-1819-1` + RC-R1.5 ~ R1.13 共 9 份 + MA4 运行时验证计划 2 份）中的「执行期发现 / 执行期决策 / 执行裁决 / Evidence」段落逐条抽取**可复用的工程经验**，与既有三处知识库逐项比对覆盖：

- `docs/skills/README.md`（技能注册表 + §项目定制化层已知失败模式 1-13）
- `docs/lessons/`（01-11 + README）
- `.opencode/skills/`（nop-backend-dev / nop-testing / nop-debugging 全文）

判定原则：已录入 → 不新增；部分覆盖 → 标注缺口所在；全新 → 推荐补充（含落点与措辞锚点）；属于业务知识而非可复用方法 → 明确「不提升，归 owner docs」（遵循 README「技能不替代 owner docs」规则）。

## 二、覆盖判定总表

| # | 计划执行期发现 | 证据位置 | 既有覆盖 | 判定 |
|---|---------------|---------|---------|------|
| 1 | daoFor 回落致 R2c 基线漂移 1383→1384 → I*Biz 优先 | R1.6 执行说明 / R1.8 | nop-backend-dev 反模式表 + E3 自检 + 失败模式 #3/#9 + lesson 07 + compliance-baseline-drift-adjudication-prompt | ✅ 已覆盖 |
| 2 | 快照 delVersion 毫秒竞态 → `@EnableSnapshot(checkOutput=false)` 降级 | R1.8 :119（自注「对齐 nop-testing skill 先例」） | nop-testing「拒绝路径快照降级」节 | ✅ 已覆盖 |
| 3 | 拒绝路径零落库断言 + 三层测试验证 | R1.8 测试矩阵 / R1.11 closure | nop-testing「三层测试验证模型」节 | ✅ 已覆盖 |
| 4 | dict 死状态 / 过账吞异常 / closure-pending / 基线回填 | R1.0 展开器背景 | lessons 07-11 + 失败模式 #9-13 + behavioral-failure-mode-scan-prompt | ✅ 已覆盖 |
| 5 | **GraphQL 时间戳秒级精度 → E2E 同秒断言 flaky → `waitForTimeout(1100)` 跨秒** | R1.5 :82/:137 | nop-testing E2E 排查表仅 4 行（白屏/webServer/超时/AMIS） | ❌ 全新，推荐补 |
| 6 | **FrozenClock 只冻结日期不冻结时间（`CoreMetrics.currentTimestamp()` 走真实毫秒）→ 绝对时间断言不可行 → 镜像公式断言 + DAO-seed 确定性构造** | R1.6 :80 / R1.5 :80 | nop-testing「冻结时钟」定位为零；触发词表有「E2E」但无此节 | ❌ 全新，推荐补（最高价值） |
| 7 | **`IUserContext.getRoles()` 返回 roleId 集而非 roleName；`isUserInRole(roleId)`；`action-auth.xml` roles 匹配 roleId（SiteMapProvider containsRole）** | R1.7 :61 执行裁决（双审查核验） | nop-backend-dev 反模式表零命中（无认证语义节） | ❌ 全新，推荐补 |
| 8 | **`afterEntityChange` 2-arg vs 3-arg 重载陷阱：delete 路径直调 2-arg，覆写 3-arg 漏 delete（首轮测试实证 totalHours 停留旧值）** | R1.8 :72 | nop-backend-dev 钩子方法段仅列方法名，无重载陷阱警示 | ❌ 全新，推荐补 |
| 9 | **父级派生汇总须 `orm().flushSession()` 后重查**（防 stale） | R1.8 :72 | 仅 nop-testing 拒绝路径节提 flushSession（测试侧）；后端侧无 | ⚠️ 半覆盖，推荐后端侧补 |
| 10 | **BeanCopier `Map<Long,X>` 键类型转换不完整（Java 直传 Long 键 CCE / JSON 路径键恒 String 静默失效）→ `Map<String,X>`** | R1.10 :66 javadoc | nop-backend-dev 零命中 | ❌ 全新，推荐补 |
| 11 | **测试改名后 `_cases/` 孤儿快照目录清理（必做）** | R1.5 :84 | nop-testing 快照目录结构节未提改名清理 | ❌ 全新，推荐补 |
| 12 | **0 字节 `autotest.yaml` 空壳 = 无快照用例的合法目录占位** | R1.5 :84 / A4.2 plans | nop-testing 未提 | ⚠️ 半覆盖（现象级使用，无文档），推荐补 |
| 13 | **恢复/补偿路径须与正向路径对称 + 幂等守卫 + 前置终端守卫（防双占用）** | R1.12 三个 Decision | 无（业务语义） | ⚠️ 业务知识，归 owner doc（`finance/budget.md` 注记已落） |
| 14 | **精确容错范式（守卫类错误外全部 rethrow）vs 全吞范式名正言顺** | R1.12 :38 | lesson 09 仅覆盖「全吞」反模式，无正范式 | ⚠️ 半覆盖，推荐 lesson 09 补正范式对照 |
| 15 | 守卫/驳回场景优先选**域内专用错误码**（测试可断言），不复用平台通用权限码 | R1.7 :61 / R1.8 :38 | nop-backend-dev「错误码用 `*Errors.java`」但无决策导引 | ⚠️ 半覆盖，推荐 nop-backend-dev 补 |
| 16 | **seedRole 测试范式**（`roleId="role-"+roleName` + `enableActionAuth=FALSE` 下 `IUserContext.set` 注入） | R1.7 :119/79 | nop-testing 有 `setUser` 但无角色守卫测试节 | ⚠️ 半覆盖，可选补 |
| 17 | **Q3 批量授权枚举边界不可类推**：dict 追加不在「加列/加 UK/新增实体」枚举内 → 保守不注册 | R1.7 :63 | ai-autonomy-policy 有保护区域规则但无「枚举外裁决」先例 | ⚠️ 治理面，由 autonomy-policy 承载，可选补 |
| 18 | R1.12 承付对称性 / R1.13 可用性预检 OFF/WARN/HARD + null 保守门禁 / R1.11 超收容差双模式 / R1.9 匿名 eNPS 聚合 | 各计划 | — | ❌ 不建议提升：业务知识，owner doc 注记已妥善 |

## 三、分层归类（平台级 → nop-entropy/docs-for-ai；项目级 → 项目技能/lessons）

> 裁决依据（用户 2026-08-08）：技能应是「与具体 Nop 平台、业务应用无关的相对通用元信息」；平台自身行为的知识属于平台文档，项目技能只保留与本项目约定/先例绑定的部分。

### 平台级知识 → `nop-entropy/docs-for-ai/`（已全部落地，2026-08-08）

| 发现（表中 #） | 落点 | 内容 |
|------|------|------|
| #5 GraphQL 时间戳秒级精度（`nop.graphql.ignore-millis-in-timestamp=true` 默认忽略毫秒）| `02-core-guides/api-and-graphql.md` §「5. 时间戳序列化精度」| 配置语义 + 同秒二次调用断言 flaky 的跨秒确定性等待 |
| #6 FrozenClock 只冻结日期不冻结时间（`CoreMetrics.currentTimestamp()` 真实毫秒）| `02-core-guides/testing.md` 常见坑 #7 | 绝对时间断言不可行 → 镜像公式 / seed 相对时间两式 |
| #11/#12 测试改名后 `_cases/` 孤儿目录 + 0 字节 `autotest.yaml` 占位语义 | `02-core-guides/testing.md` 常见坑 #8 | 孤儿判定规则（占位合法，目录合法） |
| #7 `IUserContext.getRoles()` 返回 roleId 集；`isUserInRole(roleId)`；action-auth `roles` 匹配 roleId（SiteMap containsRole）| `02-core-guides/auth-and-permissions.md` §操作权限新子节 | 平台语义 + Java 守卫写法 + 配置/seed 权威一致性注记 |
| #8/#9 `afterEntityChange` 2-arg/3-arg 重载：delete 直调 2-arg（覆写 3-arg 漏 delete）+ 派生重查先 flushSession | `02-core-guides/service-layer.md` §「afterEntityChange 重载陷阱」| 覆写 2-arg 覆盖三路径 + flush 语义 |
| #10 BeanCopier/JSON 对 `Map<Long,X>` 键类型转换不完整（Java CCE / JSON 键恒 String 静默失效）| `02-core-guides/dto-json-and-message-beans.md` 常见坑 #5 | 统一 `Map<String,X>` 双路径稳定 |

### 项目级知识（绑定本项目约定/先例，留在项目层）

| # | 内容 | 保留落点 | 处置 |
|---|------|---------|------|
| #15 守卫/驳回场景优先域内专用错误码（`erp.err.<domain>` + 测试可断言） | `.opencode/skills/nop-backend-dev`（错误处理节） | ✅ 已补（2026-08-08，ERR_MAKEUP_ROLE_REQUIRED 范例） |
| #16 seedRole 范式（`roleId="role-"+roleName` + `enableActionAuth=FALSE` 下 `IUserContext.set` 注入） | `.opencode/skills/nop-testing`（新增「角色守卫测试」节） | ✅ 已补（2026-08-08，双侧断言 + roleId 语义） |
| #14 精确容错 vs 全吞正范式对照（`ErpPurInvoiceProcessor:285-290` vs `ErpPurReturnProcessor:293-295`） | `docs/lessons/09-*.md`（新增「精确容错正范式」对照段） | ✅ 已补（2026-08-08，白名单 + rethrow 判定规则） |
| #17 批量预授权枚举不可类推（dict 追加不在 Q3 枚举 → 保守不注册） | `docs/skills/README.md` §保护区 | ✅ 已补（2026-08-08，RC-R1.7 dict MANUAL 裁决留痕） |

### 边界情形（已有覆盖，不再添加）

- #2 delVersion 毫秒竞态 → `@EnableSnapshot(checkOutput=false)`：项目 nop-testing 拒绝路径节已有先例，平台 `02-core-guides/testing.md` 快照诊断表已有 `DEL_VERSION` 行 + tagSet 指引——两侧都已内置，不重复写。

## 四、不提升为技能的理由（业务知识留在 owner docs）

R1.9 调研聚合（respondentHash/eNPS/driverScores）、R1.11 超收容差（strict/非 strict + 配置复用）、R1.12 承付对称性（commit/release/恢复三守卫 + 交互矩阵）、R1.13 可用性预检（OFF/WARN/HARD 三级 + null→0 保守门禁 + 组织隔离经 ICrudBiz 管道生效）均属域业务语义——owner doc 实现注记已落（`three-way-match.md` / `requisition.md` / `state-machine.md` / `shift-scheduling.md`），遵循技能库「业务知识属于 owner docs，技能捕获如何检查」原则，不提升。

## 五、落地状态登记

- **已落地（平台级，2026-08-08）**：上表 6 项全部写入 `nop-entropy/docs-for-ai/`（5 个文件：api-and-graphql.md / testing.md ×2 节 / auth-and-permissions.md / service-layer.md / dto-json-and-message-beans.md）。对 nop-entropy 的变更按 AGENTS.md 规则 8 记录于 `nop-entropy/ai-dev/logs/`。
- **已补全（项目级 4 项，2026-08-08 与报告同日落地）**：① nop-backend-dev 错误处理节新增「守卫场景域内专用错误码」导引；② nop-testing 新增「角色守卫测试」节（seedRole 范式 + 双侧断言）；③ lesson 09 新增「精确容错正范式」对照段（白名单 + rethrow 判定）；④ docs/skills/README §保护区新增「批量预授权枚举不可类推」条目（RC-R1.7 dict MANUAL 裁决留痕）。
- **审计闭环**：平台级条目均来自计划执行期留痕（R1.5/R1.6/R1.7/R1.8/R1.10），内容已在上表锚定文件位置；未改动计划本身。