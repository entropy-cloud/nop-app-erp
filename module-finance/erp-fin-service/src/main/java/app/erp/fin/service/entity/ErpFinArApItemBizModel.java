
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import java.util.Objects;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.dto.ArApAgingRow;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

@BizModel("ErpFinArApItem")
public class ErpFinArApItemBizModel extends CrudBizModel<ErpFinArApItem> implements IErpFinArApItemBiz {
    public ErpFinArApItemBizModel() {
        setEntityName(ErpFinArApItem.class.getName());
    }

    @Override
    @BizQuery
    public List<ErpFinArApItem> findOpenItemsByPartner(@Name("partnerId") Long partnerId,
                                                       @Name("direction") String direction,
                                                       IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("partnerId", partnerId));
        query.addFilter(eq("direction", direction));
        query.addFilter(in("status", Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_OPEN, ErpFinConstants.AR_AP_STATUS_PARTIAL)));
        query.addOrderField("businessDate", false);
        return findList(query, null, context);
    }

    @Override
    @BizQuery
    public List<ArApAgingRow> aging(@Name("direction") String direction,
                                    @Name("asOfDate") LocalDate asOfDate,
                                    IServiceContext context) {
        LocalDate asOf = asOfDate != null ? asOfDate : CoreMetrics.today();
        boolean byDueDate = isAgingByDueDate(direction);

        QueryBean query = new QueryBean();
        query.addFilter(eq("direction", direction));
        query.addFilter(in("status", Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_OPEN, ErpFinConstants.AR_AP_STATUS_PARTIAL)));
        List<ErpFinArApItem> items = findList(query, null, context);

        Map<Long, ArApAgingRow> byPartner = new HashMap<>();
        for (ErpFinArApItem item : items) {
            LocalDate baseDate = byDueDate && item.getDueDate() != null ? item.getDueDate() : item.getBusinessDate();
            ArApAgingRow row = byPartner.computeIfAbsent(item.getPartnerId(), k -> {
                ArApAgingRow r = new ArApAgingRow();
                r.setPartnerId(k);
                return r;
            });
            BigDecimal open = item.getOpenAmountFunctional() != null ? item.getOpenAmountFunctional() : BigDecimal.ZERO;
            accumulate(row, daysBetween(baseDate, asOf), open);
        }
        return new ArrayList<>(byPartner.values());
    }

    // ---------- helpers ----------

    protected boolean isAgingByDueDate(String direction) {
        String configKey = direction != null && Objects.equals(direction, ErpFinConstants.DIRECTION_RECEIVABLE)
                ? ErpFinConstants.CONFIG_AR_AGING_BASE
                : ErpFinConstants.CONFIG_AP_AGING_BASE;
        String base = AppConfig.var(configKey, ErpFinConstants.AGING_BASE_DUE_DATE);
        return !ErpFinConstants.AGING_BASE_INVOICE_DATE.equalsIgnoreCase(base);
    }

    protected void accumulate(ArApAgingRow row, long ageDays, BigDecimal open) {
        row.setTotalOpen(row.getTotalOpen().add(open));
        if (ageDays <= 30) {
            row.setBucket030(row.getBucket030().add(open));
        } else if (ageDays <= 60) {
            row.setBucket3160(row.getBucket3160().add(open));
        } else if (ageDays <= 90) {
            row.setBucket6190(row.getBucket6190().add(open));
        } else if (ageDays <= 180) {
            row.setBucket91180(row.getBucket91180().add(open));
        } else {
            row.setBucket180Plus(row.getBucket180Plus().add(open));
        }
    }

    protected long daysBetween(LocalDate from, LocalDate to) {
        if (from == null) {
            return 0;
        }
        return to.toEpochDay() - from.toEpochDay();
    }
}
