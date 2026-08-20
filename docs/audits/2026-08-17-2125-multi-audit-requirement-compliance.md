# 多维审计报告 — mission `requirement-compliance`

> Audit Status: planned
> Audit Type: multi-dimensional
> Mission: requirement-compliance

- 审计对象：mission `requirement-compliance` 整件工作（M0 → MA1-MA4 → MR0/MR1（RC-R1.1-R1.89）→ MV → MG，roadmap 头部声明 2026-08-20「全路线图闭合」）在其多维上下文中的表现，含 `./` 代码、配置、测试与公共契约（导出 / API 面），并对照架构文档核查已登记契约漂移。
- 审计人：独立子代理（fresh session，非 mission 执行者，未参与任何 MA/MR1/MV/MG 执行环节）。
- Skill：`docs/skills/multi-dimensional-audit-prompt.md`，前置注入 `docs/skills/README.md §项目定制化层`（保护区域 / 验证命令 / 命名约定 / 13 项已知失败模式）+ `docs/audits/requirement-compliance-methodology.md`（mission 审计契约）。
- 基线：HEAD = `ac92d3091`（2026-08-20）。本文件为终版（open）：同路径 `planned` 草稿经本审计**逐条独立复核后定稿**——全部发现均由本审计对实仓重验（非照抄草稿证据）；草稿 F3 的「docStatus `== 'ACTIVE'` 死状态映射」主张经 ORM `ext:dict` 绑定核验为误报已撤回，草稿 successVals 第 9 位 `RETRIEEVED` 笔误按实仓更正为 `RETRIED`（结论不变），草稿对 MV V.3「Tier 2 抽样未命中 R1.47」的叙述经 V.3 报告复核**修正**为「Tier 2 含 R1.47 但仅做代码实存核验」（见 F1 证据 5）。工作树含 mission 结束审计批次 docs 侧产物（roadmap Follow-up Backlog 追加 + multi/open 两份审计报告 + 两份 P1 修复计划 `2026-08-20-2052-1/2`，均 active/planned、零代码变更）——发现仍处 open 状态，修复义务 = 执行既有计划。

## 0. 核验方法与证据基线（本审计独立复跑/重读）

| 核验项 | 命令/路径 | 结果 |
|---|---|---|
| 工作区状态 | `git status --short` + `git log --oneline -8` | HEAD `ac92d3091` 零代码变更；仅 docs 侧审计批次产物（roadmap M + 2 审计报告 + 2 修复计划 untracked） |
| Compliance checker 复跑（lesson 07） | `bash docs/audits/nop-compliance-checker.sh` | 全 19 规则 actual == baseline 零漂移（R1d=14 / R2a=34 / R2b=237 / R2c=1507 / R2d=38 / R3=5 / R6=2 / R7=0 / R10=12 / R12a/b/c=70/66/41，与 `compliance-baseline.md §BASELINE` 及 `known-good-baselines.md` 2026-08-20 MV 行一致；不以退出码作门控依据——纯 reporter，真门控在 CI workflow） |
| 全量验证证据有效性 | `git diff --name-only 957888ffc..HEAD` | MV V.1 fresh 全量验证（156 模块 install + 3789/0/0/1 + checker 零漂移）跑于 `957888ffc`；其后 2 个 commit 仅 docs/ + README.md（19 文件，零代码变更）→ 证据对当前 HEAD 仍有效 |
| arm-index P0/P1 闭合状态 | `grep '^| \`P1-RC-'` 全量提取 + 无 done 行筛选 | 87 条 P1-RC 行；85 条含 done 标记，**恰 2 条无 done**（P1-RC-015 / P1-RC-092，见 F1/F5）；`P0-RC-*` 0 条（与 MV V.3「零 P0 证实」一致） |
| P1-RC-092 实仓复核（本审计重读） | `ErpInvLandedCostProcessor.java`（`module-inventory/erp-inv-service/.../processor/`）+ `ErpInvLandedCostApproveProcessor.java` + `app-erp-inventory.orm.xml:1353` + 全仓 `application*.yaml` | `lockReceiveForAllocation:393-395` = `ormTemplate.lock(receive)`（锁 `erp_pur_receive` 行）；`validateNotAlreadyAllocated:397-412` = `dao.findAllByQuery` **非锁 SELECT**（`erp_inv_landed_cost` 表）；ApproveProcessor 时序 = `requireLandedCost`（首读，固定 MVCC 快照）→ … → `lockReceiveForAllocation` → `validateNotAlreadyAllocated`——三修复选项零落地（①零 `transactionIsolation`/`READ-COMMITTED` 配置；②check 仍非锁读；③仅 `UK_INV_LANDED_COST_CODE_ORG(code,orgId)`，无 `(receiveId, approveStatus)` UK） |
| Blocker 修正先例重读 | `docs/plans/2026-08-06-1517-2-rc-ma4-a4-1-17-...md`（Proof 行结论修正注记） | 独立结束审计已把「锁+check 原子化成立」修正为「check 是不同表的**非锁 SELECT**，MySQL-RR 下读视图固定于首读不随锁获取刷新 → 跨方言不一致（MySQL-RR 退化），非原『跨方言一致成立』」——「锁 + check 已足够」论断 2026-08-06 已被 mission 自己的审计链证伪 |
| 公共契约实存抽查 | grep 实仓（本审计直接复核 4 项） | `IErpQaInspectionBiz.cancelForBusinessBill`（`erp-qa-dao/.../IErpQaInspectionBiz.java:98`）/ `IErpCtVolumeDiscountBiz.resolveDiscount`（`erp-ct-dao/.../IErpCtVolumeDiscountBiz.java:27`；pur/sal 消费点 `ErpPurCtDiscountApplier`/`ErpSalCtDiscountApplier` 实存，§2.4 两行登记一致）/ hr `ErpHrSalaryPostApprovalProcessor:55` `postingDispatcher.tryPostAccrual` / `erp-aps-auto-dispatch.job.yaml`（`app-erp-all/src/main/resources/_vfs/nop/job/conf/`）——全部存在，与 roadmap done 行声明一致 |
| 架构矩阵核对 | `docs/architecture/data-dependency-matrix.md §2.4`（:127-140 区）逐行 vs MR1 新增 pom 边 | §2.4 已登记 cs→qa、pur→drp、pur→ct、sal→ct、assets→mnt、mfg→mnt、mnt→mfg、mnt→qa、aps→notify、log→sal、drp→sal/qa/pur 等全部同类边；**cs→crm 边缺失**（`grep -rn 'crm-dao' docs/architecture/` = 0 命中，见 F2） |
| 真相源冻结核对 | `git show 40c95d830 -- docs/design/customer-service/use-cases.md` | commit 含一处**既有行就地改写**（UC-CS-05 附近注记 `actionType=NOTE` → 「RC-R1.69 前为 NOTE，现独立 ADOPT_KNOWLEDGE」）+ 两段附录实现注记；验收标准块零改动（见 F4） |
| view.xml gen-control 契约 | MR1 触及 delta view 逐个 grep 调色板模式 + dict 真值 + `git log` 溯源 | 唯一 mission 触及文件命中 = `ErpCsTicket.view.xml` status 列调色板（见 F3）；该文件 mission 期内经 `a66346c9f`（RC-R1.68/69 损坏重建）/`4f19163b9f`（RC-R1.71）触及，调色板内容为 pre-mission 存量（`8878bb43f` 2026-07-25 gen-control 收敛批次）；repo-wide `successVals` 族另见 12 域 44+ 站点（hr 10 / ct 5 / qa 4 / prj 4 / mnt 4 / mfg 3 / drp 3 / b2b 3 / cs 2 / crm 2 / aps 2 / log 1），经 `git log` 抽样证实为 pre-mission 存量 |
| 同批次审计产物核验 | 两份 2052 修复计划 + roadmap `## Follow-up Backlog` | 计划 `Plan Status: active`、全部 Phase checkbox 未勾（planned、零执行）；Follow-up Backlog 5 行与两份审计 P2 发现一一对应且误报撤回已同步 |

## 1. 发现（按严重性排序）

### F1 — [P1] RC-R1.47 对 P1-RC-092 的「已实现确认」闭包与该 finding 的 Blocker 修正结论相矛盾；arm-index 修复状态未回填（lesson 11 复发）

**一句话理由**：P1 级会计正确性风险（MySQL-RR 下到岸成本重复分摊）被一个重述了 finding 已知前提的核实关闭——未实现、也未驳斥任一修复选项，且索引状态仍是 `todo`——mission「全路线图闭合」声明在该行上实质不成立，属 Q4=(a)（P1 必须实现、禁方案 B 无例外）契约违约。

**证据链**（本审计独立重验，行号为写时实测）：

1. **finding 本体**（`docs/audits/arm-index.md:298` P1-RC-092）：MySQL InnoDB REPEATABLE_READ（MySQL 默认隔离级别）下，`validateNotAlreadyAllocated` 对 `erp_inv_landed_cost` 表的**非锁 SELECT** 读陈旧 MVCC 快照（快照固定于 `ApproveProcessor` 首次非锁读），`SELECT FOR UPDATE` 锁的是 `erp_pur_receive` 行（不同表），不能刷新读视图 → TOCTOU 重新打开 → 并发重复分摊（StockBalance 双计成本调整）。finding 给出三个修复方向：①部署侧强制 READ_COMMITTED ②sibling 行锁定读 ③`(receiveId, approveStatus)` UK 兜底，标注「MR1 + ask-first（锁/数据安全类）」。
2. **Blocker 修正先例**（`docs/plans/2026-08-06-1517-2-...md` Proof 行结论修正注记，本审计重读）：该计划的独立结束审计已把「锁 + check 的 check-then-act 在锁保护下原子化」结论**推翻并修正**为「跨方言不一致（MySQL-RR 退化），非原『跨方言一致成立』」。即「锁 + check 已足够」论断在 2026-08-06 已被本 mission 自己的审计链证伪。
3. **闭包声明**（`docs/backlog/requirement-compliance-roadmap.md:439` RC-R1.47 行 + 头部 `:52` C 类摘要，2026-08-12）：「pessimistic lock `ErpInvLandedCostProcessor:388-407` + app 级唯一性检查 `validateNotAlreadyAllocated:392-407` 已落地，UK 非必需（approveStatus 是状态字段非自然键）」→ done ✅。该核实重新确认的恰是 finding **已承认存在**的两个机制（锁 + check），未触及「check 非锁读陈旧快照」这一核心主张；「UK 非必需」仅回应选项③可行性，①②未处置，也无 §4 式裁决记录——闭包时序（08-12）晚于 Blocker 修正（08-06），与在盘证据直接矛盾。
4. **实仓复核（本审计，HEAD 重读代码）**：见 §0 表「P1-RC-092 实仓复核」行——三修复选项零落地，工作树零 Java 变更。
5. **状态未回填 + 逃逸路径**：arm-index P1-RC-092 行「修复状态」列仍为 `todo（修复方向 MR1 评估…）`，仅末列追注「【R1.0 展开归属】RC-R1.47」——违反 methodology §11.3 四点回写与 lesson 11。MV V.3（`docs/audits/2026-08-20-1255-rc-mv-task-level-closure-audit.md`）未拦截的机理（本审计重读核实）：Layer 1 将 R1.47 计为「C 类无修复计划属预期」（:24/:55）；Tier 2 **实际抽中 R1.47**（:36）但核验方式为「代码实存核验（pessimistic-lock 链…均在）」（:87）——存在性核验对「机制存在但不足」型虚假闭包**构造性失明**；其「arm-index 回填 89/89 OK / lesson-11 零陈旧」声明（:56）对 P1-RC-092 行与实仓 `todo` 字面不符。
6. **修复状态（本审计时点）**：修复计划 `docs/plans/2026-08-20-2052-1-inv-landed-cost-mysql-rr-toctou-remediation.md` 已转 active（Source 指向本报告 F1 + open 审计 F1；含 Phase 1 三选项裁决 + §5 锁/数据安全类双独立子 agent 批准门控 checkbox + Phase 2 守卫实现 + Phase 3 四点回写捆绑 P1-RC-015 规范化；Current Baseline 行号经本审计抽验准确），但全部 Phase 仍 planned、零执行——**发现 open，义务 = 执行并闭合该计划**。

**影响面**：H2（测试）/ PG / MySQL-RC 部署行为正确（当前全绿验证基线均为 H2）；**MySQL 默认 RR 部署存在重复分摊窗口**（到岸成本重复计入 → 存货成本错报，会计正确性类）。沿袭原 P1 非 P0 四项理由（窄触发 / 下游 version 守护 / 已登记并发敏感点 / 期末对账可发现），无当前基线上的活跃数据破坏——属「应修复非阻断」，但闭包声明失真必须纠正。

**修复义务**（P1，MUST fix）：①执行既有 fix plan `2026-08-20-2052-1`（三选项择一或组合；锁/check 逻辑属 §5 数据安全类，计划内双独立子 agent 批准门控须真实履行）；②roadmap `:439` + 头部 `:52` C 类措辞改写为真实裁决或回退 done；③arm-index P1-RC-092 修复状态回填。**注意**：若裁决维持「不修」，唯一合法出口是证明需求本身不合理并人工批准改 product-scope（Q4=(a)），不得以 C 类「已实现」重述关闭。

### F2 — [P1] data-dependency-matrix §2.4 缺失 cs-service → crm-dao Java 层边（RC-R1.65 落地未登记，架构契约漂移）

**一句话理由**：MR1 新增的真实 compile 级跨域依赖（cs→crm）未登记入模块 DAG 权威文档，而本 mission 全部其余同类边均已登记——架构 owner doc 与实仓 pom 出现实质性契约漂移。

**证据链**（本审计独立重验）：

1. **实仓依赖**：`module-cs/erp-cs-service/pom.xml:61` `app-erp-crm-dao` compile scope（RC-R1.65，2026-08-18 落地，工单创建自动分配候选池；roadmap :457 done 行自述「cs→crm 单向 R：erp-crm-dao compile」，消费点 `TicketAssignResolver` 普通 `@Inject` + try/catch 失败隔离）。
2. **架构文档缺失**：`grep -rn 'crm-dao' docs/architecture/` = **0 命中**（data-dependency-matrix.md 与 module-boundaries.md 双双为 0；§2.4 表 :127-140 逐行复核）。
3. **同 mission 全部同类先例均登记**：cs→qa（R1.68）、pur→drp-dao（R1.81/82）、pur→ct-dao + sal→ct-dao（R1.79）、assets→mnt-dao（R1.77）、mfg→mnt-dao（R1.76）、mnt→mfg-dao / mnt→qa-dao（R1.78）、aps→notify-dao（R1.86/88）、log→sal-dao（R1.85）、drp→sal/qa/pur-dao（R1.81/82）——RC-R1.65 是唯一漏登的 Java 层新边。
4. **闭合声明缺口**：roadmap RC-R1.65 done 行与 plan `2026-08-17-2125-1` 收尾同步清单均未列 matrix 登记（对比 R1.68/76/77/78/81/85/86 行内显式「matrix §2.4 登记」）；MV V.3 五点一致性核验点不含架构矩阵，故未拦截。
5. **修复状态（本审计时点）**：修复计划 `docs/plans/2026-08-20-2052-2-arch-matrix-cs-crm-edge-registration.md` 已转 active（两轮独立草案审查，Current Baseline 与实仓一致），Phase 1 仍 planned——**发现 open，义务 = 执行该计划**。

**影响面**：无行为影响、DAG 仍无环（cs→crm 单向，crm 对 cs 零反向）；但 §2.4 是跨模块审计与仓库拆分决策的权威输入，缺失行使该依赖对下游架构工作不可见。一行登记修复，属「真实契约漂移、应修非阻断」。

**修复义务**（P1，MUST fix）：执行既有 plan `2026-08-20-2052-2`：`data-dependency-matrix.md §2.4` 补 cs-service → crm-dao 行（消费点 `IErpCrmTeamBiz`/`IErpCrmTeamMemberBiz` 只读 + 单向叶依赖 + DAG 无环说明，镜像 R1.68 cs→qa 行格式）；并复核 RC-R1.65 计划收尾清单补记。

### F3 — [P2] `ErpCsTicket.view.xml` status 列 gen-control 调色板契约漂移：跨域残留 + RESOLVED 未入 successVals（未登记的 P2-MA4-014..020 同族实例，cs 域）

**一句话理由**：cs 工单视图 status 列的状态徽标颜色逻辑对本域 dict 真值大面积不命中（RESOLVED 渲染灰色、successVals 17 值仅 1 个在 dict 内），纯视觉/可维护性、无数据破坏，按本维度定义记 P2 watch-only。

**证据**（`module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/ErpCsTicket.view.xml`，本审计重读）：

- **status 列**（`:18-40`，ORM `app-erp-cs.orm.xml:186` 绑定 dict `erp-cs/ticket-status`，真值 = NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED）：
  - `:23` `successVals` 17 值（COMPLETED/APPROVED/RECEIVED/DELIVERED/PAID/SETTLED/HONORED/RETRIED/EXECUTED/COMPUTED/CONFIRMED/RUNNING/MATERIAL_TRANSFERRED/ACTIVE/CLOSED/DONE/SUCCESS）中**仅 CLOSED 在本域 dict**，16 个跨域残留值（死分支）；**RESOLVED 未入任何调色板** → 已解决工单渲染灰色（应为 success）。
  - primaryVals 13 值仅 IN_PROGRESS 命中；dangerVals 仅 CANCELLED 命中；warningVals 零命中；NEW/ASSIGNED 落 default（中性新单态，可接受）。
- **【草稿误报撤回】docStatus 列**（`:42-56`）`== 'ACTIVE' ? 'primary' : 'default'` —— 该列绑定**共享 dict `erp/doc-status`**（ORM `app-erp-cs.orm.xml:187` `ext:dict="erp/doc-status"`，真值 = DRAFT/ACTIVE/CANCELLED），**ACTIVE 存在 → 映射有效非死状态**。草稿将其误对照 `erp-cs/ticket-status` 判为死映射，本审计经 ORM `ext:dict` 绑定核验撤回该主张；roadmap Follow-up Backlog 对应行已同步修正。
- 既有登记 finding（P2-MA4-014/015/016/017/019/020）覆盖 finance/mfg/pur/inv/hr/crm 六域，**cs 未在清单**——本实例为同族未登记项。该文件被 mission 触及（RC-R1.68/69 损坏重建 + RC-R1.71，commits `a66346c9f`/`4f19163b9f`，`git log` 证实），反模式系按 HEAD 内容恢复的存量非本 mission 引入（palette 源头 `8878bb43f` 2026-07-25 gen-control 收敛批次）；但 mission 触及该文件且本维度属强制检查项，应补登记。
- **族面扩展观察（不改变 P2 定级）**：repo-wide grep `successVals` 另见 12 域 44+ 站点同款调色板（含 aps/b2b/ct/drp，如 `ErpDrpLine.view.xml` 含 `MATERIAL_TRANSFERRED` 而 dict `erp-drp/drp-line-status` 真值 SUGGESTED/APPROVED/ORDERED/CANCELLED——ORDERED/SUGGESTED 未入调色板），经 `git log` 抽样证实为 2026-07 pre-mission 存量，非 mission 引入；后续调色板批量收口批次应一并盘点。
- label 显示经 dict `graphql:labelProp` 正确，仅颜色类错（维度定义：最坏为按钮颜色错，无活跃数据破坏）。

**处置建议**：arm-index 补一行 P2 watch-only（对齐 P2-MA4-014 族格式，标注 cs 域 + 引用本报告），修复归后续调色板批量收口批次（含 aps/b2b/ct/drp 存量站点盘点）。roadmap Follow-up Backlog 已登记本项（措辞经审计修正后与证据一致）。

### F4 — [P2] 真相源冻结条款边缘案例：RC-R1.68/69 收尾 commit 就地修改了 use-cases.md 既有注记行，「L1 不动」声明仅在验收标准层面成立

**一句话理由**：冻结文件被超出「纯附录追加」的就地编辑触及，但验收标准零改动、行为有 2026-08-12 用户 B 类批量裁决背书——流程纪律 watch-only，非需求语义漂移。

**证据**（commit `40c95d830`，2026-08-19，RC-R1.68/69 收口；本审计 `git show` 复核）：diff 对 `docs/design/customer-service/use-cases.md` 含一处**既有行就地改写**（UC-CS-05 附近注记 `actionType=NOTE` → 「RC-R1.69 前为 NOTE，现独立 ADOPT_KNOWLEDGE——见下方附录实现注记」）+ UC-CS-05/UC-CS-06 两段附录实现注记。methodology §9 将 use-cases.md 列入冻结清单（修订须人工批准 + 登记）；roadmap RC-R1.68/69 行声明「use-cases UC-CS-06 附录注记（L1 不动）」——文件层面该声明不精确（L1 文件被触及，仅验收标准块未动）。行为（actionType dict 追加值）在 2026-08-12 用户批准的 B 类清单内（「RC-R1.68（actionType dict + TicketAction 记录 NCR）」），故无未授权需求变更实质。

**处置建议**：后续修复计划触 use-cases 时一律「附录追加 + 显式 §9 登记行（变更理由/影响面/批准人=对应批量裁决编号）」，避免就地改写既有行；本条不要求回滚。已登记 roadmap Follow-up Backlog。

### F5 — [P2] arm-index P1-RC-015 修复状态使用非规范标记（「修复落地（RC-R1.8…）」而非「【修复状态：done（RC-R1.8）】」）

**一句话理由**：修复内容完整（roadmap RC-R1.8 done + plan + 8 组测试证据齐备），仅回填标记格式与 85 条兄弟行不一致，机器化状态提取会误读为未闭合——cosmetic。

**证据**（本审计全量计数重验）：`docs/audits/arm-index.md:154` 行尾「…零回归全绿 **【R1.0 展开归属】RC-R1.8**」，无规范 done 标签（对照 P1-RC-011..014 的「【修复状态：done（RC-R1.x…）】」格式）；修复内容以「；**修复落地（RC-R1.8，plan …）**」形式内嵌于原 todo 段落。该行是 87 条 P1-RC 中仅有的 2 条无 done 行之一（另一条 = P1-RC-092，归 F1）。

**处置建议**：随 F1 的 arm-index 回填批次一并规范化（已捆绑入 plan `2026-08-20-2052-1` Phase 3），不单独立项。

### F6 — [P2] Follow-up Backlog 前言措辞与本批次审计定稿状态不同步（「均已转 planned/归档处置」）

**一句话理由**：roadmap `## Follow-up Backlog` 前言（:626）称两份审计「均已转 `planned`/归档处置」，本报告定稿为 `open` 后该措辞陈旧——纯措辞同步项，open 审计剩余未知数③已登记同一义务。

**证据**：`docs/backlog/requirement-compliance-roadmap.md:626`（工作树修改态）。**处置建议**：批次收口（两份审计均定稿 + 修复计划闭合 + 日志/提交落盘）时随 Phase 3 文档回填一并更正，不单独立项。

## 2. 分维度裁决（反窄化自检：每维至少一句）

| 维度 | 裁决 | 依据摘要 |
|---|---|---|
| 需求正确性 | **通过（余 F1）** | roadmap M0→MG 与实仓一致：MA1-MA4 + MR1 89 行全 done；MV V.1 fresh 3789/0/0/1 @ `957888ffc` 且其后仅 docs 变更（本审计 `git diff` 复核），证据有效；公共契约抽查 4 项本审计直接复核实存（qa cancelForBusinessBill:98 / ct resolveDiscount:27 + pur/sal applier / hr tryPostAccrual:55 / aps auto-dispatch job.yaml）。唯一实质缺口 = F1（一条 P1 finding 的闭合失真）。 |
| owner-doc 对齐 | **通过（余 F2/F4）** | 修复四点回写抽查到位（roadmap done 行普遍含 plan id + 测试计数 + owner doc 注记 + arm-index 回填）；MG 产物实存（lessons 12-15 + methodology §11 回流 + skill 薄壳同步 + project-context 失败模式扩充）。漂移点：matrix §2.4 漏 cs→crm（F2）、use-cases 注记就地改写（F4）。 |
| 架构或边界影响 | **余 F2** | MR1 全部跨域 Java 边 DAG 无环且经 §2.4 登记（除 F2 缺边外逐行复核）；保护区域纪律证据充分——A/B/C 批量裁决（2026-08-12 用户批准，roadmap 头部在盘）+ 越界行双独立子 agent 批准落盘（R1.44/R1.49 等行内 session id 可见）；ORM 变更抽查符合 Q3 纯加性授权边界。 |
| 验证充分性 | **通过（余 F1 逃逸路径）** | 本审计独立复跑 checker 零漂移（19 规则逐行对齐）；MV V.3 冷会话结束审计存在且终裁 passes。盲区（F1 证据 5）：V.3 对「C 类/已实现确认」行采用代码实存核验，对「机制存在但不足」型虚假闭包构造性失明，其「arm-index 回填 89/89 OK」声明与 P1-RC-092 行 `todo` 实况不符——建议后续里程碑级闭合审计对该类行做对抗性（非存在性）全量核验。 |
| 回归风险 | **无发现** | 工作树零代码变更；MV 后零代码 commit（`git diff --name-only` 复核）；唯一 skip（`ErpAllWebPagesCollectTest` @Disabled JDK26/ANTLR 已知项）文档化；2026-08-19 view.xml 损坏事件已立 bugs 档并修复验证（`docs/bugs/2026-08-19-cs-ticket-view-xml-append-corruption.md`，commit `40c95d830` --stat 含该文件）。 |
| 路由和技能选择正确性 | **无发现** | MA1 51 切片均指定 `multi-dimensional-audit-prompt`、MA2/MA3 指定 `open-ended-audit-prompt`（roadmap Skill 列抽查）；MR1 修复行 Skill: none 符合「修复类无匹配技能」路径；两份 P1 修复计划 Task Route + Skill 选择依据齐备（2052-1/2 头部）。 |
| 待办或自主权策略漂移 | **无发现（余 F1 实质面）** | R1.0 展开遵循 2026-08-07/08 人工裁决（分批启动）；P2 登记不强制（Q4 判据）→ P2-RC 行保 todo 属设计而非漏关；B 类降级 26 项、A 类 21 项、C 类 2 项均可追溯到 2026-08-12 用户裁决原文。F1 的 C 类「已实现确认」是裁决质量问题而非越权。审计批次 P2 → Follow-up Backlog 转移符合「P2-only 不立项」分级规则。 |
| ORM 完整性 / 代码生成纪律（项目特定） | **无发现** | 抽查 MR1 新实体/列均在 `<domain>/model/*.orm.xml` 真相源（ErpCsTicketTimerSession、ErpMdMaterialSku.status、BOM 快照三实体族、ErpCrmTeamMember，roadmap 行内 plan 证据链抽查）；`_gen` 产物经增量重生成，`git diff` 范围内无手改生成文件痕迹；已知失败模式 1/2/8（表前缀双重拼接 / 章节重编号残留 / propId 断续）在本批次 diff 范围零触发。 |
| view.xml gen-control 契约（项目特定） | **余 F3** | MR1 触及 delta view 逐一核查：唯一调色板命中 = ErpCsTicket.view.xml status 列（F3）；docStatus 列草稿死映射主张经 ORM dict 绑定核验**撤回**（共享 erp/doc-status 含 ACTIVE，映射有效）；aps/b2b/ct/drp 等 12 域 44+ 调色板站点为 pre-mission 存量（git log 证实），归 F3 批量收口观察项。 |

## 3. 结论

**needs revision** —— 无 P0；存在 2 项 P1（F1 闭包失真 + F2 架构矩阵漂移）须经修订收口后方可接受 mission「全路线图闭合」声明。修复路径已具雏形（两份 active 修复计划在盘），义务 = 执行并闭合：

1. **F1**（维度：需求正确性 / 验证充分性）：`docs/backlog/requirement-compliance-roadmap.md:439` + `:52`、`docs/audits/arm-index.md:298`、`module-inventory/.../ErpInvLandedCostProcessor.java` —— 缺失证据：对 MySQL-RR TOCTOU 主张的处置（实现①②③之一 / 人工批准的需求层出口 / 显式 successor 登记），及 arm-index 状态回填。执行计划：`docs/plans/2026-08-20-2052-1`（Phase 1 裁决 + §5 双批准门控已在计划内；Phase 3 捆绑 F5 处置）。
2. **F2**（维度：架构或边界影响）：`docs/architecture/data-dependency-matrix.md §2.4` —— 缺失证据：cs-service → crm-dao 边登记行。执行计划：`docs/plans/2026-08-20-2052-2`。

P2 项（F3/F4/F5/F6）登记不强制，已归 roadmap Follow-up Backlog（F5 另捆绑入 plan 2052-1 Phase 3；F6 随批次收口更正）。

### 正面确认（mission 声明 vs 实仓抽样全对齐项）

- MV V.1/V.2 全绿证据有效（HEAD 无代码漂移，本审计 `git diff` 复核）；checker 本审计复跑零漂移。
- 89 条 MR1 修复行中 88 条五点闭合证据齐备（roadmap done / plan / 测试 / owner doc / 日志抽查）。
- 公共契约面（IBiz 新方法 / 新实体 / job.yaml / ORM 列）抽查实存（本审计直接复核 4 项）。
- 保护区域 dual-agent-approval 纪律抽查无违规（A/B/C 批量裁决 + 越界行双 session id 在盘）。
- MG 知识沉淀产物（4 新 lesson + methodology §11 回流 + 上下文失败模式扩充）实存且计数准确。

### 过程纪律自检

- [x] 本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`：全 19 规则 actual == baseline 零漂移（§0 表）；不以脚本退出码作门控依据（纯 reporter，门控在 CI workflow）。
- [x] closure-audit 独立性：本审计由独立子代理 fresh session 执行，未参与 mission 任何执行；对同路径 `planned` 草稿逐条独立复核后定稿而非照抄（F3 误报撤回、successVals 值勘误、F1 证据 5 的 V.3 叙述修正均为本审计复核产物）。
- [x] 与 arm-index 交叉去重：F1/F5 基于 P1-RC-092/P1-RC-015 既有行（复用）；F2/F3/F4/F6 为新发现，已按 §7 规则与既有 finding（P2-MA4-014 族、lesson 11/13）及同批次 open 审计比对面差异依据。
- [x] 本审计未修改任何真相源或生产代码；报告为纯落盘定稿（roadmap Follow-up Backlog 的 F3 行事实性修正在草稿阶段已完成，本审计未再改动）。

<AI_STEP_RESULT>issues</AI_STEP_RESULT>
