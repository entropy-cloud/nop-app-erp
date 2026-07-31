package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinCashForecast;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * ErpFinCashForecast refreshForecast per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含现金预测批量聚合编排（{@code treasury.md §现金预测派生}）：先清区间再聚合 ArApItem 未核销到期项 +
 * 票据到期项。各 step 为 protected。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinCashForecastRefreshForecastProcessor {

    @Inject
    IDaoProvider daoProvider;

    public Integer refreshForecast(LocalDate fromDate, LocalDate toDate, IServiceContext context) {
        IEntityDao<ErpFinCashForecast> forecastDao = daoProvider.daoFor(ErpFinCashForecast.class);

        // 先清区间：删除 [fromDate, toDate] 内已有预测行（重新生成）。
        QueryBean clearQuery = new QueryBean();
        clearQuery.addFilter(ge("forecastDate", fromDate));
        clearQuery.addFilter(le("forecastDate", toDate));
        List<ErpFinCashForecast> stale = forecastDao.findAllByQuery(clearQuery);
        for (ErpFinCashForecast row : stale) {
            forecastDao.deleteEntity(row);
        }

        int count = 0;
        // 1. ArApItem 未核销到期项：RECEIVABLE→INFLOW，PAYABLE→OUTFLOW。
        count += collectArApItems(forecastDao, fromDate, toDate);

        // 2. 应收票据到期（非终态）→ INFLOW。
        count += collectReceivableNotes(forecastDao, fromDate, toDate);

        // 3. 应付票据到期（ISSUED）→ OUTFLOW。
        count += collectPayableNotes(forecastDao, fromDate, toDate);

        return count;
    }

    protected int collectArApItems(IEntityDao<ErpFinCashForecast> forecastDao, LocalDate fromDate, LocalDate toDate) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(ge("dueDate", fromDate));
        q.addFilter(le("dueDate", toDate));
        // 仅未核销（OPEN/PARTIAL）项参与预测。
        q.addFilter(in("status", Arrays.asList(ErpFinConstants.AR_AP_STATUS_OPEN, ErpFinConstants.AR_AP_STATUS_PARTIAL)));
        List<ErpFinArApItem> items = dao.findAllByQuery(q);

        int count = 0;
        for (ErpFinArApItem item : items) {
            ErpFinCashForecast row = newForecast(item.getOrgId(), null, item.getDueDate(),
                    item.getSourceBillType(), item.getSourceBillCode(),
                    directionForArAp(item.getDirection()), item.getPartnerId(),
                    item.getAmountSource(), item.getAmountFunctional());
            forecastDao.saveEntity(row);
            count++;
        }
        return count;
    }

    protected int collectReceivableNotes(IEntityDao<ErpFinCashForecast> forecastDao, LocalDate fromDate, LocalDate toDate) {
        IEntityDao<ErpFinNotesReceivable> dao = daoProvider.daoFor(ErpFinNotesReceivable.class);
        QueryBean q = new QueryBean();
        q.addFilter(ge("dueDate", fromDate));
        q.addFilter(le("dueDate", toDate));
        // 排除已承兑/已拒付/已注销（已不再是未来现金流）。
        q.addFilter(in("status", Arrays.asList(
                ErpFinConstants.NOTES_RECV_RECEIVED,
                ErpFinConstants.NOTES_RECV_DISCOUNTED,
                ErpFinConstants.NOTES_RECV_ENDORSED,
                ErpFinConstants.NOTES_RECV_COLLECTION_PENDING)));
        List<ErpFinNotesReceivable> notes = dao.findAllByQuery(q);

        int count = 0;
        for (ErpFinNotesReceivable note : notes) {
            ErpFinCashForecast row = newForecast(note.getOrgId(), null, note.getDueDate(),
                    ErpFinConstants.SOURCE_BILL_NOTES_RECEIVABLE, note.getCode(),
                    ErpFinConstants.CASH_FLOW_INFLOW, note.getPartnerId(),
                    note.getAmountSource(), note.getAmountFunctional());
            forecastDao.saveEntity(row);
            count++;
        }
        return count;
    }

    protected int collectPayableNotes(IEntityDao<ErpFinCashForecast> forecastDao, LocalDate fromDate, LocalDate toDate) {
        IEntityDao<ErpFinNotesPayable> dao = daoProvider.daoFor(ErpFinNotesPayable.class);
        QueryBean q = new QueryBean();
        q.addFilter(ge("dueDate", fromDate));
        q.addFilter(le("dueDate", toDate));
        // 仅 ISSUED（未兑付）的应付票据参与预测。
        q.addFilter(eq("status", ErpFinConstants.NOTES_PAY_ISSUED));
        List<ErpFinNotesPayable> notes = dao.findAllByQuery(q);

        int count = 0;
        for (ErpFinNotesPayable note : notes) {
            ErpFinCashForecast row = newForecast(note.getOrgId(), null, note.getDueDate(),
                    ErpFinConstants.SOURCE_BILL_NOTES_ENDORSED, note.getCode(),
                    ErpFinConstants.CASH_FLOW_OUTFLOW, note.getPartnerId(),
                    note.getAmountSource(), note.getAmountFunctional());
            forecastDao.saveEntity(row);
            count++;
        }
        return count;
    }

    protected String directionForArAp(String direction) {
        return direction != null && Objects.equals(direction, ErpFinConstants.DIRECTION_RECEIVABLE)
                ? ErpFinConstants.CASH_FLOW_INFLOW : ErpFinConstants.CASH_FLOW_OUTFLOW;
    }

    protected ErpFinCashForecast newForecast(Long orgId, Long fundAccountId, LocalDate forecastDate,
                                             String sourceBillType, String sourceBillCode, String direction,
                                             Long partnerId,
                                             BigDecimal amountSource, BigDecimal amountFunctional) {
        IEntityDao<ErpFinCashForecast> dao = daoProvider.daoFor(ErpFinCashForecast.class);
        ErpFinCashForecast row = dao.newEntity();
        row.setOrgId(orgId);
        row.setFundAccountId(fundAccountId);
        row.setForecastDate(forecastDate);
        row.setSourceBillType(sourceBillType);
        row.setSourceBillCode(sourceBillCode);
        row.setDirection(direction);
        row.setPartnerId(partnerId);
        row.setAmountSource(amountSource);
        row.setAmountFunctional(amountFunctional);
        return row;
    }
}
