package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpFinAccountingPeriod generateNextYearPeriods per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含次年期间批量生成编排（{@code period-close.md §年度结转规则} 步骤5）；共享 protected helper 单一真相源在
 * {@link ErpFinAccountingPeriodProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor {

    @Inject
    ErpFinAccountingPeriodProcessor facade;

    /**
     * 批量生成指定年度 1-12 月会计期间（{@code period-close.md §年度结转规则} 步骤5）。
     *
     * <p>幂等策略（Decision）：同年期间已存在时，默认抛 {@code ERR_PERIODS_ALREADY_EXIST}；
     * 配置 {@code erp-fin.period-generate-skip-existing=true} 时仅补建缺失月份。
     * 状态分派：1 月 OPEN（假定次年即将开始核算），2-12 月 NEVER_OPENED（待运营按月开启）。
     */
    public Integer generateNextYearPeriods(Integer year, IServiceContext context) {
        if (year == null) {
            throw new NopException(ErpFinErrors.ERR_PERIOD_NOT_FOUND)
                    .param(ErpFinErrors.ARG_YEAR, year);
        }
        IEntityDao<ErpFinAccountingPeriod> dao = facade.daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean existingQ = new QueryBean();
        existingQ.addFilter(eq("year", year));
        List<ErpFinAccountingPeriod> existing = dao.findAllByQuery(existingQ);

        if (!existing.isEmpty() && !facade.isPeriodGenerateSkipExisting()) {
            throw new NopException(ErpFinErrors.ERR_PERIODS_ALREADY_EXIST)
                    .param(ErpFinErrors.ARG_YEAR, year)
                    .param(ErpFinErrors.ARG_EXISTING_PERIOD_COUNT, existing.size());
        }

        java.util.Set<Integer> existingMonths = existing.stream()
                .map(ErpFinAccountingPeriod::getMonth)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Long orgId = existing.isEmpty() ? facade.resolveDefaultOrgId() : existing.get(0).getOrgId();

        int created = 0;
        java.time.YearMonth ym = java.time.YearMonth.of(year, 1);
        for (int month = 1; month <= 12; month++) {
            if (existingMonths.contains(month)) {
                continue;
            }
            java.time.YearMonth m = ym.withMonth(month);
            ErpFinAccountingPeriod p = dao.newEntity();
            String code = year + "-" + String.format("%02d", month);
            p.setCode(code);
            p.setName(code);
            p.setOrgId(orgId);
            p.setYear(year);
            p.setMonth(month);
            p.setStartDate(m.atDay(1));
            p.setEndDate(m.atEndOfMonth());
            p.setQuarter((month - 1) / 3 + 1);
            p.setIsAdjustment(Boolean.FALSE);
            // 1 月设为 OPEN（假定次年即将开始核算），其余 NEVER_OPENED 待运营开启。
            p.setStatus(month == 1 ? ErpFinConstants.PERIOD_STATUS_OPEN
                    : ErpFinConstants.PERIOD_STATUS_NEVER_OPENED);
            dao.saveEntity(p);
            created++;
        }
        facade.orm().flushSession();
        return created;
    }
}
