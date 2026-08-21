package app.erp.fin.service.report;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinGlBalance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 多账套读路径隔离测试（plan 2026-07-30-0841-3-r1-29，P1-MA2-095 Phase 3 Proof）。
 *
 * <p>验证报表按期间所属组织的主账套（FINANCIAL）过滤，多账套部署下不双计：
 * 同期间同组织同科目两账套余额（FINANCIAL 1000 + MANAGEMENT 3000），报表聚合仅取主账套 1000。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinMultiSchemaReportIsolation extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpFinReportBizModel reportBiz;

    @Test
    public void testBalanceSheetScopedToPrimarySchema() {
        ormTemplate.runInSession(session -> {
            String periodId = seedPeriod("801", "2", 2026, 5);
            // 组织 2 的两个账套：501=FINANCIAL（主），502=MANAGEMENT
            seedAcctSchema("501", "FIN-501", "2", "FINANCIAL");
            seedAcctSchema("502", "MGT-502", "2", "MANAGEMENT");
            ErpMdSubject asset = seedSubject("802", "1001-ASSET", "ASSET", ErpFinConstants.DC_DEBIT);

            // 同期间同组织同科目，两账套各一行
            seedGlBalance("9001", "2", "501", periodId, asset.getId(), new BigDecimal("1000"));
            seedGlBalance("9002", "2", "502", periodId, asset.getId(), new BigDecimal("3000"));
            return null;
        });

        String periodId = "801";
        List<Map<String, Object>> rows = reportBiz.balanceSheetData(periodId, CTX);

        BigDecimal totalAsset = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            if ("ASSET".equals(r.get("section"))) {
                totalAsset = totalAsset.add((BigDecimal) r.get("amount"));
            }
        }
        // 仅主账套 FINANCIAL(501)=1000 计入；MANAGEMENT(502)=3000 被过滤，不双计
        assertEquals(0, totalAsset.compareTo(new BigDecimal("1000")),
                "多账套读路径隔离：报表仅取主账套 1000，不双计为 4000");
    }

    // ---------- helpers ----------

    private String seedPeriod(String id, String orgId, int year, int month) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("MS-" + id);
        p.setName("MS-" + id);
        p.setOrgId(orgId);
        p.setYear(year);
        p.setMonth(month);
        LocalDate start = LocalDate.of(year, month, 1);
        p.setStartDate(start);
        p.setEndDate(start.withDayOfMonth(start.lengthOfMonth()));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private void seedAcctSchema(String id, String code, String orgId, String nature) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode(code);
        s.setName(code);
        s.setOrgId(orgId);
        s.setNature(nature);
        s.setFunctionalCurrencyId("1");
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private ErpMdSubject seedSubject(String id, String code, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode(code);
        s.setName(code);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s;
    }

    private void seedGlBalance(String id, String orgId, String schemaId, String periodId, String subjectId,
                               BigDecimal closingDebit) {
        IEntityDao<ErpFinGlBalance> dao = daoProvider.daoFor(ErpFinGlBalance.class);
        ErpFinGlBalance b = dao.newEntity();
        b.orm_propValue(1, id);
        b.setOrgId(orgId);
        b.setAcctSchemaId(schemaId);
        b.setPeriodId(periodId);
        b.setSubjectId(subjectId);
        b.setCurrencyId("1");
        b.setOpeningDebit(BigDecimal.ZERO);
        b.setOpeningCredit(BigDecimal.ZERO);
        b.setPeriodDebit(BigDecimal.ZERO);
        b.setPeriodCredit(BigDecimal.ZERO);
        b.setClosingDebit(closingDebit);
        b.setClosingCredit(BigDecimal.ZERO);
        b.setYearOpeningDebit(BigDecimal.ZERO);
        b.setYearOpeningCredit(BigDecimal.ZERO);
        dao.saveEntity(b);
    }
}
