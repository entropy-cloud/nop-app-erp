package app.erp.inv.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 服务层集成测试：存货过账端到端（DONE→PostingEvent→凭证→posted=true）。
 *
 * <p>seed 会计期间+科目（inventory→finance），调 {@link IErpInvStockMoveBiz#generateMove}（业务联动入库→自动 DONE），
 * 断言 {@link ErpFinVoucher}+{@link ErpFinVoucherBillR} 落库、移动单 {@code posted=true}、凭证 {@code docStatus=已过账}。
 * 另覆盖同法人内部调拨不过账、过账失败不阻塞终态。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvPosting extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();


    static final Long ORG_ID = 1003L;
    static final Long MATERIAL_ID = 2003L;
    static final Long WAREHOUSE_ID = 3003L;
    static final Long LOCATION_ID = 4003L;
    static final Long UOM_ID = 5003L;
    static final Long CURRENCY_ID = 6003L;
    static final Long ACCT_SCHEMA_ID = 7003L;
    static final int VOUCHER_STATUS_POSTED = 20;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Test
    public void testMoveDoneGeneratesVoucherAndPosted() {
        seedPeriodAndSubjects();

        ErpInvStockMove move = generateIncoming("PR-POST-001", new BigDecimal("10"), new BigDecimal("5"));

        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "业务联动应 DONE");
        assertEquals(true, move.getPosted(), "入库 DONE 应过账 posted=true");

        ErpFinVoucherBillR link = findBillLink(move.getCode());
        assertNotNull(link, "应生成业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "凭证应落库");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=已过账");
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("50")) == 0,
                "借方合计=存货 50");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("50")) == 0,
                "贷方合计=暂估应付 50");
        assertEquals(2, countLines(voucher.getId()), "入库估值凭证 2 行（借存货/贷暂估）");
    }

    @Test
    public void testInternalTransferNoPosting() {
        seedPeriodAndSubjects();
        // 先入库建源仓库存（内部调拨源库位需校验可用量）
        generateIncoming("PR-XFER-STOCK", new BigDecimal("10"), new BigDecimal("5"));

        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpInvConstants.MOVE_TYPE_INTERNAL_TRANSFER);
        request.setOrgId(ORG_ID);
        request.setBusinessDate(LocalDate.of(2026, 7, 1));
        request.setSourceWarehouseId(WAREHOUSE_ID);
        request.setSourceLocationId(LOCATION_ID);
        request.setDestWarehouseId(9003L);
        request.setDestLocationId(9103L);
        request.setAcctSchemaId(ACCT_SCHEMA_ID);
        request.setCurrencyId(CURRENCY_ID);
        request.setRelatedBillType("TRANSFER");
        request.setRelatedBillCode("TR-NOPOST-001");
        request.setLines(Collections.singletonList(line(new BigDecimal("5"), null)));
        ErpInvStockMove move = stockMoveBiz.generateMove(request, CTX);

        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "内部调拨应 DONE");
        assertEquals(false, move.getPosted(), "同法人内部调拨不过账 posted=false");
        assertEquals(0, countBillLinks(move.getCode()), "内部调拨不产生业财回链/凭证");
    }

    @Test
    public void testPostingFailureLeavesMoveDonePostedFalse() {
        // 不 seed 会计期间 → 过账引擎 ERR_PERIOD_NOT_FOUND → 派发器吞异常，移动单保持 DONE + posted=false
        seedSubjectsOnly();

        ErpInvStockMove move = generateIncoming("PR-FAIL-001", new BigDecimal("10"), new BigDecimal("5"));

        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "过账失败不阻塞终态，移动单仍 DONE");
        assertEquals(false, move.getPosted(), "过账失败 posted=false");
        assertEquals(0, countBillLinks(move.getCode()), "过账失败不产生凭证/回链");
    }

    // ---------- helpers ----------

    private ErpInvStockMove generateIncoming(String billCode, BigDecimal qty, BigDecimal unitCost) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpInvConstants.MOVE_TYPE_INCOMING);
        request.setOrgId(ORG_ID);
        request.setBusinessDate(LocalDate.of(2026, 7, 1));
        request.setDestWarehouseId(WAREHOUSE_ID);
        request.setDestLocationId(LOCATION_ID);
        request.setAcctSchemaId(ACCT_SCHEMA_ID);
        request.setCurrencyId(CURRENCY_ID);
        request.setRelatedBillType("PUR_RECEIPT");
        request.setRelatedBillCode(billCode);
        request.setLines(Collections.singletonList(line(qty, unitCost)));
        return stockMoveBiz.generateMove(request, CTX);
    }

    private StockMoveLineRequest line(BigDecimal qty, BigDecimal unitCost) {
        StockMoveLineRequest line = new StockMoveLineRequest();
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitCost(unitCost);
        line.setCurrencyId(CURRENCY_ID);
        return line;
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 10);
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedSubject("6401", "主营业务成本");
        });
    }

    private void seedSubjectsOnly() {
        ormTemplate.runInSession(() -> {
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
        });
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, int status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(10);
        subject.setDirection(10);
        subject.setStatus(10);
        dao.saveEntity(subject);
    }

    private ErpFinVoucherBillR findBillLink(String moveCode) {
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(
                new QueryBean());
        return links.stream().filter(l -> moveCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private long countBillLinks(String moveCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> moveCode.equals(l.getBillCode())).count();
    }

    private long countLines(Long voucherId) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> dao = daoProvider
                .daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }
}
