# Lesson 14: config-gate 认定范式——「功能默认关闭」不等于「功能缺失」

> **来源**：2026-08 requirement-compliance MA4。A4.1.4（`docs/audits/2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`）首立「部署启用决策 vs 契约缺失」认定范式，其后跨 ≥10 切片复用（A4.2.18 PC-3 强制核销 config 不存在 / A4.2.31 承付 config-gate 非默认活跃 / A4.2.5 检验双门控默认 OFF / A4.2.4 variance-failure config 默认 false 等），并反向指导 MR1 修复的 config 化裁决（RC-R1.58 D4 硬契约不 config 化 / RC-R1.48 config 默认 true / RC-R1.52 家族 config 门控）。
> **适用场景**：审计中遇到「功能存在但 config 默认关闭」时的符合性认定；fix plan 起草时决定"是否 config 门控 / 默认值取什么"；部署契约核对。
> **失败模式**：两个对称的误判——**假阳性**：把 config 默认关闭的功能判为"需求分歧/功能缺失"（重开无谓 finding，浪费 MR1 修复预算）；**假阴性**：把真缺失用"可以 config 化/后续开启"掩饰（方案 B 变体，违反 Q4=(a)），或把验收标准硬契约 config 化稀释（默认关闭 = 默认不达标）。

## 核心论点

**「功能存在但 config-gate 条件启用」与「功能缺失」是两种不同的事实**，认定必须回到部署契约真相源，而不是靠功能开关的直觉：

1. **功能存在性看控制机制**：守卫/拦截/编排逻辑是否完整实现（含异常路径——HARD 拦截抛错、告警派发）。机制完整 + config 条件启用 = 功能存在。
2. **契约性看真相源声明**：product-scope / use-cases / owner doc §部署 是否声明「开箱即用 / 默认开启」。**零声明 = 默认关闭合法**（opt-in 是 ERP 通用启用范式）；显式声明默认开启而实现默认关闭 = 部署契约分歧（真 finding）。
3. **建筑层自证**：`module-meta.yaml §optionalFeatures` 的 `defaultValue` 分类（opt-in 组 vs default-on 组）是 config 语义的登记面，与代码默认值交叉核对。
4. **反向禁令**：L1 验收标准是**硬契约**时不得 config 化（如关键项否决——config 默认关闭意味着默认不满足验收标准，见 RC-R1.58 D4）；config 门控只适用"可配置行为"（部署策略类），不适用"验收标准类"。

## 认定方法（三源核对，A4.1.4 首立）

```
发现「功能经 config-gate 默认关闭」时：
1. 控制机制完整性：守卫/拦截/编排逻辑（含异常路径）是否完整实现？
   → 不完整：功能缺失，正常分级（与 config 无关）。
   → 完整：进入 2。
2. 部署契约真相源三处核对：
   a. 真相源声明（product-scope / use-cases / owner doc §部署/§配置项）
      ——是否声明「开箱即用 / 默认开启」？
   b. 建筑层登记（module-meta.yaml §optionalFeatures defaultValue）
      ——opt-in 组还是 default-on 组？
   c. 部署侧工件（全量生产 application.yaml 普查）
      ——是否有 override（有 override = 部署侧已显式决策）？
   → 三处零「默认开启」声明：**维持接受**（opt-in 合法，非需求分歧）。
   → 任一处声明默认开启：部署契约分歧 → 按 §2 分级进入修复（修默认值或修声明，经裁决）。
3. 修复侧对称应用（fix plan 时）：
   → 验收标准硬契约：不 config 化（默认必须达标）。
   → 部署策略类可配置行为：config 门控 + owner doc §配置项 登记默认值与理由 + module-meta optionalFeatures 同步登记。
```

## 真实案例（节选）

| 案例 | 判定 | 依据 |
|---|---|---|
| 预算控制/承付 config 默认关闭（A4.1.4） | **维持接受** | 真相源 + 部署工件零「开箱即用」声明；module-meta 显式 opt-in 组（对照 exchange-revaluation/annual-close default-on 组）；10 个 config-gate 站点全 `Boolean.FALSE` + 生产 yaml 零覆盖；控制机制（HARD/WARN/NONE 三通道）完整 |
| PC-3「强制核销模式」config 不存在（A4.1.18） | **维持 P2 watch-only** | config key grep 三重证实不存在 → L1 限定词「强制核销模式」不活跃 → 分支①不升 P1（**config 不存在 = 模式不可达 = 无运行时分歧**） |
| 承付恢复不对称（A4.2.31） | **维持 P1 重开但注明非默认活跃** | 功能性不对称成立（P1），但 config-gate 默认 false + 生产 yaml 零覆盖 → 非默认活跃路径，指导 MR1 优先级排序（不触发 MR0） |
| 关键项否决（RC-R1.58 D4） | **不 config 化** | L1「关键项否决，无论其他项」是验收标准硬契约——config 默认关闭 = 默认不达标，A4.1.4 范式仅适用可配置行为 |
| mfg 物料预留（RC-R1.48）/ drp 越库（RC-R1.81） | config 门控 + 默认值裁决 | 部署策略类：config 登记 + 默认值经裁决（对齐既有测试保护 / 行为语义）+ owner doc §配置项 登记理由 |

## 自检清单（审计认定 / fix plan config 化时）

- [ ] 认定「功能缺失」前是否核对了 config-gate 状态（机制完整 + 仅条件启用 ≠ 缺失）？
- [ ] 三源核对做全了吗（真相源声明 / module-meta optionalFeatures / 生产 application.yaml 普查）？
- [ ] config 默认值变更的裁决是否区分了「验收标准硬契约」（不 config 化）与「部署策略类」（可 config 化）？
- [ ] 新增 config 门控是否同步登记三处（owner doc §配置项 + module-meta optionalFeatures + `Erp*Configs`）？
- [ ] finding 分级是否注明「config-gate 默认关闭 → 非默认活跃」以指导修复优先级（非 MR0 级）？

## 何时复发

- 审计者第一眼看到「默认关闭」就登记功能缺失（假阳性）。
- 修复者为规避回归风险把验收标准行为 config 化且默认关闭（假阴性，硬契约稀释）。
- config 键声明了但无消费方（`P1-RC-005` 型孤儿 config——A4.1.14 部署认知面）。

## 关联

- 首立报告：`docs/audits/2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`（三源核对法 + module-meta 对照组）
- 复用切片：A4.1.18 / A4.2.4 / A4.2.5 / A4.2.31 / A4.1.14 等（arm-index 各行运行时注记）
- 反向应用：RC-R1.58 D4（硬契约不 config 化）/ RC-R1.48 / RC-R1.81（config 门控登记范式）
- 建筑层登记：`docs/architecture/business-module-metadata.md` §2.1（optionalFeatures 设计意图）
- 划界：与 lesson 12（文档化简化滥用）正交——本课是**认定方法**（避免误判），12 是**收口纪律**（禁止错位关闭）；「用 config 化掩饰真缺失」的假阴性面对应 lesson 12 的禁用关闭方式清单
