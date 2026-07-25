package app.erp.common.service;

import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 看板聚合辅助工具（plan 2026-07-24-2200-1 Phase 4）。
 *
 * <p>11 个 {@code *DashboardBizModel} 共用的静态方法：null 安全 BigDecimal、Map 值类型转换、
 * 月份键构造、安全除法、GROUP BY 查询构造。
 */
public final class DashboardUtil {

    private DashboardUtil() {
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(v.toString());
    }

    public static String monthKey(LocalDate date) {
        if (date == null) return "";
        YearMonth ym = YearMonth.from(date);
        return ym.toString();
    }

    public static BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (numerator == null) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, BigDecimal.ROUND_HALF_UP);
    }

    public static QueryBean buildGroupByQuery(String entityName, String groupField, String aggField,
                                              String aggFunc, List<QueryBean> additionalFilters) {
        QueryBean query = new QueryBean();
        if (entityName != null) {
            query.setSourceName(entityName);
        }
        List<QueryFieldBean> fieldList = new ArrayList<>();

        QueryFieldBean group = new QueryFieldBean();
        group.setName(groupField);
        fieldList.add(group);

        QueryFieldBean agg = new QueryFieldBean();
        agg.setName(aggField);
        agg.setAggFunc(aggFunc);
        agg.setAlias("aggValue");
        fieldList.add(agg);

        query.setFields(fieldList);
        return query;
    }
}
