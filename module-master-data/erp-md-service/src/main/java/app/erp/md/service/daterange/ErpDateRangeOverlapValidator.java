package app.erp.md.service.daterange;

import app.erp.md.dao.daterange.IDateRange;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 「同维度互斥」重叠校验器（C3 日期范围有效性模式，docs/design/date-ranged-validity-pattern.md §6）。
 *
 * <p>friendly pre-save check：输入候选记录 + 同维度已存在记录 List，若有重叠则抛 {@link NopException} + 中文 ErrorCode。
 *
 * <p>纯函数工具类，无 IoC 依赖。调用方（试点实体 BizModel）在 {@code defaultPrepareSave/Update} 钩子中：
 * <ol>
 *   <li>按维度键 {@code dao().findAllByQuery(query)} 查询同维度已存在记录</li>
 *   <li>调用 {@link #enforceMutex} 校验候选记录与既有记录是否重叠</li>
 *   <li>重叠则抛异常；无重叠则继续 super 流程</li>
 * </ol>
 *
 * <p>NULL 处理：候选 {@code validFrom == null && validTo == null} 跳过校验（视为「永久无区间」）；
 * 单侧 NULL 按半开区间处理。详见 owner doc §3.1 / §6。
 */
public final class ErpDateRangeOverlapValidator {

    /** 校验异常附加参数键：实体名（如 {@code ErpMdExchangeRate}）。 */
    public static final String ARG_ENTITY_NAME = "entityName";
    /** 校验异常附加参数键：候选记录 validFrom。 */
    public static final String ARG_VALID_FROM = "validFrom";
    /** 校验异常附加参数键：候选记录 validTo。 */
    public static final String ARG_VALID_TO = "validTo";
    /** 校验异常附加参数键：发生冲突的既有记录 id。 */
    public static final String ARG_CONFLICT_ID = "conflictId";

    private ErpDateRangeOverlapValidator() {
    }

    /**
     * 互斥校验：若 candidate 与 existing 中任一记录重叠（排除 selfId），抛 {@link NopException}。
     *
     * @param candidate 候选记录（待保存的实体，实现 {@link IDateRange}）
     * @param existing  同维度已存在记录 List（由调用方查询）
     * @param errorCode 抛出时使用的错误码（按域前缀定义，见 owner doc §6）
     * @param selfId    排除自身的 id（更新场景）；新增场景传 {@code null}
     * @param <T>       实体类型（必须实现 {@link IDateRange} 且提供 {@code getId()}）
     */
    public static <T extends IDateRange> void enforceMutex(T candidate, List<T> existing,
                                                            ErrorCode errorCode, Object selfId) {
        if (candidate == null) {
            return;
        }
        // 永久无区间记录不参与互斥校验（业务上视为总是有效，新实体应在 ORM 层 mandatory=true 规避此场景）
        if (candidate.getValidFrom() == null && candidate.getValidTo() == null) {
            return;
        }
        if (existing == null || existing.isEmpty()) {
            return;
        }
        for (T other : existing) {
            if (other == null) {
                continue;
            }
            Object otherId = idOf(other);
            if (selfId != null && Objects.equals(selfId, otherId)) {
                continue;
            }
            // 既有记录永久无区间也不参与（避免误报）
            if (other.getValidFrom() == null && other.getValidTo() == null) {
                continue;
            }
            if (ErpDateRanges.overlaps(candidate, other)) {
                throw new NopException(errorCode)
                        .param(ARG_ENTITY_NAME, candidate.getClass().getSimpleName())
                        .param(ARG_VALID_FROM, candidate.getValidFrom())
                        .param(ARG_VALID_TO, candidate.getValidTo())
                        .param(ARG_CONFLICT_ID, otherId);
            }
        }
    }

    /**
     * 反射取实体 id（避免强制依赖 {@code io.nop.orm.IOrmEntity} 接口）。
     * 试点实体均继承 {@code _gen._Erp*} 基类，公共 {@code getId()} 返回 {@code String}（plan 2026-08-21-1045-3 迁移后）。
     */
    private static Object idOf(Object entity) {
        try {
            return entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * STACKABLE 混合策略校验（owner doc §4 Decision C / §5）。
     *
     * <p>语义裁决（plan 2026-07-26-0315-1 Phase 1 Decision (a)）：{@code stackable=true} 表达
     * 「我可被叠加」——可与任何规则重叠；仅当两侧均 {@code stackable=false} 时互斥（如促销引擎中
     * 「折扣」与「满减」两条 stackable=false 规则不可同日叠加，但任一与 stackable=true 的「赠品」可同日生效）。
     *
     * <p>校验逻辑：遍历 {@code existing}（排除 {@code selfId} + 永久无区间跳过），
     * 若 candidate 与 other 区间重叠 且 {@code !isStackable.test(candidate) && !isStackable.test(other)}
     * → 抛 {@link NopException}；任一方 stackable=true 则允许重叠。
     *
     * <p>调用方提供 {@code isStackable} Predicate（如 {@code rule -> Boolean.TRUE.equals(rule.getStackable())}），
     * 与 {@link #enforceMutex} 同样为纯函数 + 同维度已存在记录 List 由调用方查询。
     *
     * @param candidate  候选记录（待保存的实体，实现 {@link IDateRange}）
     * @param existing   同维度已存在记录 List（由调用方按 ruleType/targetType/materialId 等维度键查询）
     * @param errorCode  抛出时使用的错误码（按域前缀定义）
     * @param selfId     排除自身的 id（更新场景）；新增场景传 {@code null}
     * @param isStackable 判断实体是否「可叠加」的 Predicate（对应 {@code stackable} 字段）
     * @param <T>        实体类型（必须实现 {@link IDateRange} 且提供 {@code getId()}）
     */
    public static <T extends IDateRange> void enforceStackableAware(T candidate, List<T> existing,
                                                                     ErrorCode errorCode, Object selfId,
                                                                     Predicate<T> isStackable) {
        if (candidate == null) {
            return;
        }
        // 永久无区间记录不参与校验（业务上视为总是有效，新实体应在 ORM 层 mandatory=true 规避此场景）
        if (candidate.getValidFrom() == null && candidate.getValidTo() == null) {
            return;
        }
        if (existing == null || existing.isEmpty()) {
            return;
        }
        boolean candidateStackable = isStackable != null && isStackable.test(candidate);
        for (T other : existing) {
            if (other == null) {
                continue;
            }
            Object otherId = idOf(other);
            if (selfId != null && Objects.equals(selfId, otherId)) {
                continue;
            }
            // 既有记录永久无区间也不参与（避免误报）
            if (other.getValidFrom() == null && other.getValidTo() == null) {
                continue;
            }
            if (!ErpDateRanges.overlaps(candidate, other)) {
                continue;
            }
            // 仅当两侧均非 stackable 时视为冲突；任一方 stackable=true 允许重叠
            boolean otherStackable = isStackable != null && isStackable.test(other);
            if (!candidateStackable && !otherStackable) {
                throw new NopException(errorCode)
                        .param(ARG_ENTITY_NAME, candidate.getClass().getSimpleName())
                        .param(ARG_VALID_FROM, candidate.getValidFrom())
                        .param(ARG_VALID_TO, candidate.getValidTo())
                        .param(ARG_CONFLICT_ID, otherId);
            }
        }
    }
}
