package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinCashForecastBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinCashForecast;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 现金预测 refreshForecast 聚合单测（Phase 3）。验证批量聚合 AR/AP 辅助账未核销到期项 + 票据到期项，
 * 按 INFLOW/OUTFLOW 写入 ErpFinCashForecast（先清区间再写入）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinCashForecastRefresh extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinCashForecastBiz cashForecastBiz;

    @Test
    public void testRefreshForecastAggregatesArApAndNotes() {
        ormTemplate.runInSession(s -> {
            // 应收辅助账（RECEIVABLE，到期日在区间内）→ INFLOW
            seedArApItem(5001L, ErpFinConstants.DIRECTION_RECEIVABLE, "AR_INVOICE", "AR-F-001",
                    LocalDate.of(2026, 8, 10), new BigDecimal("1000"));
            // 应付辅助账（PAYABLE，到期日在区间内）→ OUTFLOW
            seedArApItem(5002L, ErpFinConstants.DIRECTION_PAYABLE, "AP_INVOICE", "AP-F-001",
                    LocalDate.of(2026, 8, 20), new BigDecimal("600"));
            // 应收票据到期（RECEIVED，非终态）→ INFLOW
            seedReceivable("NR-F-001", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("2000"),
                    LocalDate.of(2026, 8, 15));
            // 应付票据到期（ISSUED）→ OUTFLOW
            seedPayable("NP-F-001", ErpFinConstants.NOTES_PAY_ISSUED, new BigDecimal("800"),
                    LocalDate.of(2026, 8, 25));
            // 区间外到期（不参与预测）
            seedArApItem(5003L, ErpFinConstants.DIRECTION_RECEIVABLE, "AR_INVOICE", "AR-OUT",
                    LocalDate.of(2026, 12, 31), new BigDecimal("9999"));
            return Boolean.TRUE;
        });

        Integer count = ormTemplate.runInSession(session -> cashForecastBiz.refreshForecast(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), CTX));
        assertEquals(Integer.valueOf(4), count, "聚合 4 行（2 AR/AP + 2 票据）");

        List<ErpFinCashForecast> inflows = findByDirection(ErpFinConstants.CASH_FLOW_INFLOW);
        List<ErpFinCashForecast> outflows = findByDirection(ErpFinConstants.CASH_FLOW_OUTFLOW);
        assertEquals(2, inflows.size(), "2 笔流入（应收发票 + 应收票据）");
        assertEquals(2, outflows.size(), "2 笔流出（应付发票 + 应付票据）");

        // 幂等：再次刷新区间，行数不变（先清后写）。
        Integer count2 = ormTemplate.runInSession(session -> cashForecastBiz.refreshForecast(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), CTX));
        assertEquals(Integer.valueOf(4), count2, "幂等刷新仍 4 行");
        assertEquals(4, countAll(), "区间内总行数仍 4");
    }

    // ---------- seed helpers ----------

    private void seedArApItem(Long partnerId, String direction, String sourceBillType, String sourceBillCode,
                              LocalDate dueDate, BigDecimal amount) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = new ErpFinArApItem();
        item.setCode("ARI-" + sourceBillCode);
        item.setOrgId(1L);
        item.setAcctSchemaId(1L);
        item.setDirection(direction);
        item.setPartnerId(partnerId);
        item.setSourceBillType(sourceBillType);
        item.setSourceBillCode(sourceBillCode);
        item.setBusinessDate(LocalDate.of(2026, 7, 1));
        item.setDueDate(dueDate);
        item.setCurrencyId(1L);
        item.setExchangeRate(BigDecimal.ONE);
        item.setAmountSource(amount);
        item.setAmountFunctional(amount);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(amount);
        item.setOpenAmountFunctional(amount);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        dao.saveEntity(item);
    }

    private void seedReceivable(String code, String status, BigDecimal amount, LocalDate dueDate) {
        IEntityDao<ErpFinNotesReceivable> dao = daoProvider.daoFor(ErpFinNotesReceivable.class);
        ErpFinNotesReceivable note = new ErpFinNotesReceivable();
        note.setCode(code);
        note.setOrgId(1L);
        note.setNotesType(ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE);
        note.setNotesNo("N-" + code);
        note.setIssueDate(LocalDate.of(2026, 7, 1));
        note.setDueDate(dueDate);
        note.setCurrencyId(1L);
        note.setExchangeRate(BigDecimal.ONE);
        note.setAmountFunctional(amount);
        note.setAmountSource(amount);
        note.setStatus(status);
        dao.saveEntity(note);
    }

    private void seedPayable(String code, String status, BigDecimal amount, LocalDate dueDate) {
        IEntityDao<ErpFinNotesPayable> dao = daoProvider.daoFor(ErpFinNotesPayable.class);
        ErpFinNotesPayable note = new ErpFinNotesPayable();
        note.setCode(code);
        note.setOrgId(1L);
        note.setNotesType(ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE);
        note.setNotesNo("N-" + code);
        note.setIssueDate(LocalDate.of(2026, 7, 1));
        note.setDueDate(dueDate);
        note.setCurrencyId(1L);
        note.setExchangeRate(BigDecimal.ONE);
        note.setAmountFunctional(amount);
        note.setAmountSource(amount);
        note.setStatus(status);
        dao.saveEntity(note);
    }

    private List<ErpFinCashForecast> findByDirection(String direction) {
        IEntityDao<ErpFinCashForecast> dao = daoProvider.daoFor(ErpFinCashForecast.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("direction", direction));
        return dao.findAllByQuery(q);
    }

    private int countAll() {
        IEntityDao<ErpFinCashForecast> dao = daoProvider.daoFor(ErpFinCashForecast.class);
        QueryBean q = new QueryBean();
        q.addFilter(ge("forecastDate", LocalDate.of(2026, 8, 1)));
        q.addFilter(le("forecastDate", LocalDate.of(2026, 8, 31)));
        return dao.findAllByQuery(q).size();
    }
}
