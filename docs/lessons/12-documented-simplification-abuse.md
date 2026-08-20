# Lesson 12: 文档化简化滥用——documented simplification / Deferred 掩盖需求-实现分歧

> **来源**：2026-07/08 requirement-compliance mission（MA2 复查 + MR1 重开族）。audit-remediation（MA1-MA7）以**方案 B 优先**收口（documented simplification / Deferred 合法化 doc↔code 分歧），doc↔code 自洽但需求层（product-scope/use-cases）与实现的分歧被写进文档"合法化"。RC mission Q4=(a) 裁决（P0/P1 必须实现、禁方案 B、无例外通道）后系统性翻转。
> **适用场景**：任何审计收口、fix plan 起草、owner doc 标注 documented simplification / Deferred / Non-Goal 的时刻。发现既有方案 B 关闭项复查时必查。
> **失败模式**：用"文档对齐"替代"需求实现"——把需求契约要求的功能缺失/行为偏离标注为 documented simplification、Deferred 或 owner doc Non-Goal 后关闭 finding。三种典型滥用面：① P0/P1 级分歧用方案 B 关闭（Q4=(a) 禁止，无例外通道）；② AI 自写标注冒充人工批准（§4 三判据不满足）；③ "已在 owner doc 说明"自圆（owner doc 是 L2 设计参考非真相源，不能对抗 L1 use-cases/product-scope）。

## 核心论点

documented simplification 本身是合法工具（P2 级 + 显式人工批准），但**收口偏好把它从"边界工具"膨胀为"万能出口"**后，它系统性地掩盖了三类事实：

1. **功能完全缺失被"设计选择"掩护**：L1 显式要求的功能（如换货、承付恢复）零实现，owner doc 却以 Non-Goal / Deferred / 设计参考语气提及，形成"文档说有、代码没有、审计曾放行"的三重假象。
2. **resolved 状态与实仓行为脱节**：修复计划标 done、arm-index 标 resolved，但计划内的显式裁决是"不实现、归 successor"——**关闭方式是文档，不是实现**。
3. **AI 自写标注无人工批准痕迹**：AI 在 owner doc 写下 Deferred 标注后引用该标注作为"已有文档化简化"证据——自我循环背书，§4 三判据 (ii) 明确不成立。

判别式：**分歧的关闭载体是什么？** 载体是代码行为 = 实现；载体是文档段落 = 方案 B。P0/P1 分歧的合法载体只有前者（唯一例外 = 需求本身不合理，经人工批准改 product-scope，那是需求变更非降级）。

## 失败模式（典型路径）

```
1. 审计发现需求-实现分歧（功能缺失 / 行为偏离验收标准）
2. "完整实现成本高"（跨实体反查 / 部分冲销 / 跨期语义 / ORM 变更）
3. fix plan 裁决："不实现 X（方案A 须 ……；归 successor，保守方向 documented）"
4. owner doc 补 Deferred/Non-Goal 标注 + arm-index 标 resolved (R*.n done)
5. 文档↔代码 重新自洽（doc 已向 code 妥协）→ 审计闭环
6. 需求真相源（use-cases 验收标准）原样未动 → 分歧仍在，只是换了个文档住址
7. 下一次以需求为基线的审计（RC）重新发现 → 重开 → 二次修复成本 + 信任损耗
```

## 真实案例（RC 重开族，节选）

| 原关闭方式 | finding | 滥用面 | RC 翻转 |
|---|---|---|---|
| R1.27 裁决「不实现发票冲销自动恢复承付（方案A 须跨实体反查 + 部分冲销 + 跨期语义；归 successor，保守方向 documented）」（plan `2026-07-30-0841-1-r1-27-budget-commitment-release-path.md`），plan 标 done + arm-index 标 resolved | `P1-MA2-083` 承付恢复不对称（approve→release 但 reverseApprove/cancel 零 `commit()` 恢复，A1.15 HEAD 复核三路径证实） | ① P1 级分歧方案 B 关闭；resolved 状态与实仓脱节 | RC A1.15 reuse 重开 → **RC-R1.12** 实装（invoice/return reverseApprove + cancel Processor 增 `budgetCommitmentBiz.commit()` 恢复，plan `2026-08-08-1603-2`） |
| owner doc `returns.md §退货类型` 含「换货：退货同时重新发货」+ `ui-patterns.md` UI 单选含「换货」，但 ORM 无 returnType 列、生产代码 grep 零命中 | `P1-RC-025` UC-SAL-06 换货完全缺失（4 断言全未实现） | ③ "文档提及"冒充"功能存在"；P1① 功能完全缺失 | MR1 **RC-R1.51** 实装（returnType 列 + generateExchangeDelivery + 双向关联 + 价差分支，plan `2026-08-16-0904-2`） |
| owner doc `scheduling.md` Non-Goal「自动派工执行」（AI 自标，无人工批准痕迹） | `P1-RC-090` aps 自动派工 | ② AI 自写 Non-Goal 标注，§4 三判据不成立 | MR1 **RC-R1.88** 实装（ErpApsAutoDispatchProcessor + job，plan `2026-08-19-2040-3-rc-mr1-r1-86-87-88-aps-auto-create-routing-dispatch-family.md`，arm-index 显式裁决 supersede） |
| payroll.md §6.5 Deferred 表（posted 无 writer + ER 不入凭证，列 4 子任务 successor） | `P1-MA4-017` hr 薪酬计提+公司承担过账 | ① P1 级（会计正确性）Deferred 悬挂 | MR1 **RC-R1.89** 实装（plan `2026-08-20-0518-3`） |
| 表 E 重开族（audit-remediation 方案 B 项在 Q4=(a) 下不成立） | `P1-RC-008/009/056/061` + `P1-MA2-071` + `P1-RC-063` | ① 批量同类 | MR1 RC-R1.48/49/67/54/80/63 逐一实装 |

## 决策树：遇到 documented simplification / Deferred / Non-Goal 标注时

```
1. 该分歧的分级是什么（对照 L1 验收标准逐条核）？
   → P0/P1：方案 B 关闭非法（Q4=(a) 无例外）。重开 → 强制实现。
     唯一出口 = 需求本身不合理：经人工批准改 product-scope（需求变更非降级，§9 登记）。
   → P2：进入步骤 2。
   → 验收标准全部满足：无分歧，标注本身是噪音（清理或保留均可）。

2. P2 的"显式人工批准记录"三判据（满足其一，按序）：
   (i) plan 含独立 plan-audit 通过记录（区分静默降级 vs 经审计裁决的简化）；
   (ii) owner doc 显式 documented simplification 标注 + 人工批准痕迹可追溯（git log / commit / 讨论文档；AI 自写不算）；
   (iii) product-scope 范围裁剪登记（裁剪理由 + 影响面 + 批准人）。
   → 均不满足：静默降级，按无批准处理（重开或补批准）。

3. 标注的语义方向核查：
   → "归 successor，保守方向 documented"类措辞 = 关闭载体是文档 → 检查 successor 触发条件是否已满足（已满足 → 回队实现）。
   → owner doc 与 L1 冲突：一律以 L1 为准，推定 owner doc 已向实现妥协（§4）。
```

## 自检清单（fix plan 起草 / 审计收口前）

- [ ] 本分歧的关闭载体是**代码行为**还是**文档段落**？P0/P1 是否有任何"方案 B / 技术不可行降级 / representative enough"措辞残留？
- [ ] "不实现 X"类裁决是否逐项列出了方案 A 的真实成本，而非用"须跨实体反查"等模糊成本一笔带过？
- [ ] 新写的 Deferred/Non-Goal 标注是否有**人工批准痕迹**（或明确登记"AI 自写，待人工追认"）？是否写了**触发条件**而非只写 Deferred？
- [ ] owner doc 提及的功能（哪怕一句话）在代码中有对应实现或显式 Deferred？没有 = "文档提及"假象（P1-RC-025 型）。
- [ ] 引用既有 resolved finding 时是否 HEAD 复核了实仓行为（plan done ≠ 行为落地）？
- [ ] arm-index 状态回填与实仓行为二次核对（对齐 lesson 11 状态回填纪律）？

## 何时复发

- 修复成本高的跨域/跨实体语义（对称恢复、红冲、多级联动）被"保守方向 documented"话术推迟。
- owner doc 先写了完整设计（含 UI 选项），实现只做了子集，审计按 doc↔code 一致性（而非需求↔实现）收口。
- 无人值守 mission driver 下 AI 用自写标注自我背书（§4 (ii) 陷阱）。

## 关联

- 真相源：`docs/audits/requirement-compliance-methodology.md` §2（分级判据 + Q4 修复义务绑定）/ §4（三判据 + 真相源层级）/ §5（禁用关闭方式清单）
- 重开证据：arm-index `P1-MA2-083`/`P1-RC-025`/`P1-MA4-017`/表 E 重开族行 + RC-R1.12/51/88/89 修复计划
- 划界：与 lesson 07（compliance 基线漂移 = **过程纪律**失败）正交；与 lesson 08（缺独立结束审计 = **验证门控**缺失）正交——本课是**收口方式语义**失败（关闭载体错位）。与 lesson 13（需求基线陈旧）互为镜像：13 是文档**被动过时**，本课是文档**主动收口**分歧。
- 裁决原文：`docs/discussions/2026-08-02-1700-requirement-implementation-compliance-audit.md` Q4=(a)
