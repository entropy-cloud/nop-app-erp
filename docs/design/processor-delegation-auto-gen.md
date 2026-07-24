# Processor 委托自动生成（已废弃）

> Status: **deprecated** — 2026-07-24 架构决策：每 mutation 独立 Processor + BizModel Java 直接调用，不再使用 xbiz `<source>` 委托和 `GenApprovalDelegation` xlib 标签。
>
> 原因：新的每 mutation 独立 Processor 模式下，bean 名 = `<entity><Method>Processor` 是机械命名，BizModel Java 的 `@Inject` + `@BizMutation` 委托已是极简模式，不需要 xlib 标签生成。
>
> 保留此文件仅作为设计演进历史参考，不删除。

## 原方案（2026-07-24 前）

通过 `x:gen-extends` + `GenApprovalDelegation` xlib 标签，批量生成 xbiz `<source>` 委托代码。

## 现方案（2026-07-24 生效）

每 mutation 一个 Processor，BizModel Java 直接调用：

```java
@BizMutation
public ErpPurOrder approve(@Name("id") String id, IServiceContext svcCtx) {
    return erpPurOrderApproveProcessor.approve(id, svcCtx);
}

@BizMutation
public ErpPurOrder cancel(@Name("id") String id, IServiceContext svcCtx) {
    return erpPurOrderCancelProcessor.cancel(id, svcCtx);
}
```

无 xbiz `<source>` 委托，无 xlib 标签。
