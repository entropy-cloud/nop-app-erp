package app.erp.md.dao.daterange;

import java.time.LocalDate;

/**
 * 日期区间归一化接口（C3 日期范围有效性模式，docs/design/date-ranged-validity-pattern.md §5）。
 *
 * <p>用于消除历史命名变体（{@code validFrom/validTo} 与 {@code effectiveFrom/effectiveTo}）
 * 在 helper 内部的差异。调用方（ORM 实体或 DTO）实现 2 个 getter 即可让纯函数 helper 工作于任意命名变体。
 *
 * <p>本接口位于 {@code erp-md-dao} 模块（与 ORM 实体同层），以便 master-data 域实体直接 {@code implements}。
 * helper 实现（{@code ErpDateRanges} / {@code ErpDateRangeOverlapValidator}）位于 {@code erp-md-service}
 * 模块，跨域消费者经 {@code app-erp-master-data-service} 依赖即可调用。
 *
 * <p>语义约定（见 owner doc §3.1）：[validFrom, validTo] 双侧闭区间；任一侧 {@code null} 表示该侧开放。
 *
 * <p>跨域接入（hr/crm/sales 等历史命名变体实体）：调用方在 BizModel 内构造匿名 {@link IDateRange}
 * 适配器包装实体的 {@code effectiveFrom/effectiveTo}，再调用 helper；或后续下沉到独立 {@code erp-common-dao}
 * 模块（触发条件：跨域接入数 > 3）。
 */
public interface IDateRange {

    /** 区间起始日（含）。{@code null} 表示左侧开放（无生效日，新实体应避免）。 */
    LocalDate getValidFrom();

    /** 区间结束日（含）。{@code null} 表示右侧开放（无失效日）。 */
    LocalDate getValidTo();
}
