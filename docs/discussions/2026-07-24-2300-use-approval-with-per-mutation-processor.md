# use-approval + nop-wf 与 per-mutation Processor 的集成方案讨论

## 背景

2026-07-24 架构决策：每 mutation 一个独立 Processor，BizModel Java `@BizMutation` 直接调用 Processor，不使用 xbiz `<source>` 委托。

问题：现有的 `use-approval` codegen 标签和 `approval-support.xbiz` 继承链如何与 per-mutation Processor 集成？

## 关键事实

**`use-approval` 标签的实质作用**（`tagSet="...,use-approval"`，纯 codegen 条件标记）：

1. 生成 `I*Biz extends IApprovableBiz`（5 个 default 方法全抛 `UnsupportedOperationException`——编译期占位）
2. 生成 `_*Biz.xbiz` 继承 `approval-support.xbiz`（平台 `nop-wf-core` 资源，提供 5 个 mutation 的 xbiz `<source>`）

**`approval-support.xbiz` 的行为**：

```
submitForApproval → DRAFT/REJECTED→SUBMITTED（含可选 wf 启动）
withdrawApproval  → SUBMITTED→DRAFT
approve           → SUBMITTED→APPROVED（写 approvedBy/approvedAt）
reject            → SUBMITTED→REJECTED（写 approvedBy/approvedAt）
reverseApprove    → APPROVED→SUBMITTED（清 approvedBy/approvedAt）
```

每个 mutation 用 XLang 脚本实现状态校验 + 转换 + 审计字段回写。`submitForApproval` 额外检查实体 xmeta `wf:wfName`，若存在则启动 nop-wf。

**nop-wf 回调路径**：
```
wf 结束 → .xwf <listener> → bizObj.invoke('approve', {id}) → xbiz source（或 Java @BizMutation）
```

wf 不直接写业务实体状态，回调业务动作。

## 候选方案

### 方案 A：在 approval-support.xbiz 中判断 Processor 是否存在

在项目级 delta 中覆写 `approval-support.xbiz`，为每个 mutation 增加 Processor 优先逻辑：

```xml
<mutation name="approve" x:override="replace">
  <source><c:script>
    const procName = inject('nopBizObjectManager')
      .getBizObject(thisObj.requireEntity(id))
      .getBizObjName() + 'ApproveProcessor';
    try {
      const proc = inject(procName);
      return proc.approve(id, svcCtx);
    } catch (e) {
      // Processor 不存在，fallback 到标准 XLang 转换逻辑
      ...
    }
  </c:script></source>
</mutation>
```

**优点**：
- 用户层 xbiz 不动（无 `x:override="remove"`）
- 后向兼容：实体没有 Processor 时自动用标准行为
- 添加 Processor 只需新建 Java 类，不需改 xbiz

**缺点**：
- 需要在项目 VFS 创建 delta 覆写平台 `approval-support.xbiz`（侵入平台资源）
- 每个 mutation 调用添加了运行时反射 + try-catch 开销
- Processor bean 名需要动态推导，XLang 中做字符串拼接 + 反射，脆弱且难测试
- 所有 `use-approval` 实体的审批调用都额外走这个检查，即使没有 Processor
- `thisObj.requireEntity(id)` 在 XLang 中的语义和 Java 版本可能不完全一致

### 方案 B：去掉 use-approval 标签

从 ORM 模型的 `tagSet` 中移除 `use-approval`。代码生成时：
- `I*Biz` 不再继承 `IApprovableBiz`（无 5 个 `UnsupportedOperationException` 占位）
- `_*Biz.xbiz` 不再继承 `approval-support.xbiz`（无 5 个 xbiz mutation source）

用户层 xbiz 不再需要移除任何 mutation。BizModel Java `@BizMutation` 是唯一切入点。

`useWorkflow="true"`（控制 `nopFlowId` 列）独立于 `use-approval`，不受影响。

**优点**：
- 最干净：无未使用的继承链，无 `x:override="remove"` 行
- 不改平台：不碰 nop-entropy，不创建 delta override
- 纯 Java：BizModel + Processor 是完整入口，xbiz 仅用于 Delta 定制
- `IBizObject.invoke('approve')` 自动 fallback 到 Java `@BizMutation`——wf 回调不受影响

**缺点**：
- `I*Biz` 不再有 `IApprovableBiz` 契约——但 5 个 default 方法本都是 `UnsupportedOperationException`，无运行时价值
- 如果某个实体未来想切回标准 `approval-support.xbiz` 行为（不写 Processor），需要加回 `use-approval` 标签 + 重新 codegen
- 轻微破坏 codegen 可复现性：ORM 模型变了，需要增量重生成

### 方案 C：保留 use-approval，在 xbiz 中 remove mutation

用户层 xbiz 用 `x:override="remove"` 移除继承来的 5 个 mutation，让 Java `@BizMutation` 接手：

```xml
<biz x:extends="_ErpPurOrder.xbiz">
  <actions>
    <mutation name="submitForApproval" x:override="remove"/>
    <mutation name="approve" x:override="remove"/>
    <mutation name="reject" x:override="remove"/>
    <mutation name="reverseApprove" x:override="remove"/>
    <mutation name="withdrawApproval" x:override="remove"/>
  </actions>
</biz>
```

**优点**：
- `I*Biz extends IApprovableBiz` 保留（编译期契约，虽然 5 个方法全抛异常——价值极低）
- 不改平台，不改 ORM 模型

**缺点**：
- 每实体 5 行无意义样板（remove 掉永远不用的 mutation）
- 空的继承链（inherits `approval-support.xbiz` → immediately removes 5 mutations）
- 认知负担：看到 remove 的人会困惑"为什么要有再删掉"

## 评估

| 维度 | 方案 A（Processor-first xbiz） | 方案 B（去掉 use-approval） | 方案 C（保留+remove） |
|------|-------------------------------|---------------------------|---------------------|
| 实现复杂度 | 高（delta 覆写 + 动态 bean 名推导 + try-catch） | 低（ORM 删一个 tag + 重生成） | 低（每实体 5 行） |
| 运行时开销 | 有（每次审批多一次反射查找） | 无 | 无 |
| xbiz 文件整洁度 | 高（用户层 xbiz 不动） | 高（无冗余行） | 低（每实体 5 行 remove） |
| 后向兼容 | 高（无 Processor 时 fallback） | 中（需重新 codegen） | 高 |
| 平台侵入 | 中（delta 覆写平台资源） | 无 | 无 |
| 职责清晰度 | 低（逻辑分散在 xbiz + Java） | 高（全部在 Java，xbiz 用于 Delta） | 中（Java 为主，xbiz 有残留） |

## 推荐

**方案 B（去掉 use-approval）**。

理由：

1. **`IApprovableBiz` 接口无运行时价值**——5 个 default 方法全抛 `UnsupportedOperationException`，是对编译器而非对人说话的占位。真正的实现入口在 BizModel `@BizMutation`。

2. **`approval-support.xbiz` 提供的纯 XLang 状态转换已被 Processor 完整替代**——状态校验、状态转换、审计字段回写、wf 启动，Processor 全部能做且做得更好（强类型、可测试、可派生覆盖）。

3. **去掉后无未使用的代码残留**——不继承不存在的资源，不需要反向删除。用户层 xbiz 保持极简。

4. **`useWorkflow` 独立控制 wf 能力**——需要 nop-wf 的实体单独设 `useWorkflow="true"`，不需要 `use-approval`。wf 启动由 `AbstractSubmitForApprovalProcessor` 按实体 xmeta `wf:wfName` 配置条件执行。

5. **wf 回调不受影响**——`bizObj.invoke('approve', {id})` 在没有 xbiz mutation source 时自动 fallback 到 Java `@BizMutation`，路径不变。

## 结论

每 mutation Processor 模式 + 去掉 `use-approval` tag + BizModel Java 直接调用 = 三件套。

## 结论

**采用方案 B**。2026-07-24 决策。

## 后续行动

1. 从所有 ORM `tagSet` 中移除 `use-approval`（独立于 `useWorkflow`）
2. 增量 codegen 重生成
3. 清理用户层 xbiz：删除不再需要的 5 个 mutation `<source>` 块；若 xbiz 无其他内容则删除文件
4. BizModel `@BizMutation` 接管全部审批入口
5. 架构文档更新 `service-layer-orchestration.md` + `processor-extension-pattern.md`

## 未解决问题

（无——已全部闭环）
