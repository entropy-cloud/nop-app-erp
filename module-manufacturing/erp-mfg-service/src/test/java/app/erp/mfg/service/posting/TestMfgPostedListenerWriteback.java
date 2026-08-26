package app.erp.mfg.service.posting;

import app.erp.fin.service.posting.VoucherPostedEvent;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F2.1（P1-CK-fin-001）：MfgSubcontractReversalListener 正向过账回写单测（dual 接口 onVoucherPosted）。
 * 覆盖 mfg 域后缀 strip 域清单：SUBCONTRACT_FEE（-SF→SubcontractOrder）、MANUFACTURING_ISSUE
 * （-MI→MaterialIssue）、PRODUCTION_VARIANCE（-PV→WorkOrder 名下 CostVariance 行 posted）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestMfgPostedListenerWriteback extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testSubcontractFeeSuffixStrippedAndPosted() {
        String code = "SC-F21-POST-001";
        ormTemplate.runInSession(session -> {
            seedSubcontractOrder(code, false);
            return null;
        });

        ormTemplate.runInSession(session -> {
            dispatch("SUBCONTRACT_FEE", code + "-SF");
            session.flush();
            return null;
        });

        ErpMfgSubcontractOrder after = findSubcontractOrder(code);
        assertEquals(Boolean.TRUE, after.getPosted(), "委外加工费凭证（-SF 后缀）strip 后回写 posted");
        assertNotNull(after.getPostedAt(), "postedAt 回写");
    }

    @Test
    public void testManufacturingIssueSuffixStrippedAndPosted() {
        String code = "MI-F21-POST-001";
        ormTemplate.runInSession(session -> {
            seedMaterialIssue(code, false);
            return null;
        });

        ormTemplate.runInSession(session -> {
            dispatch("MANUFACTURING_ISSUE", code + "-MI");
            session.flush();
            return null;
        });

        ErpMfgMaterialIssue after = findMaterialIssue(code);
        assertEquals(Boolean.TRUE, after.getPosted(), "生产领料凭证（-MI 后缀）strip 后回写 posted");
    }

    @Test
    public void testProductionVarianceMarksCostVarianceLines() {
        String woCode = "WO-F21-PV-001";
        String woId = ormTemplate.runInSession(session -> {
            app.erp.mfg.dao.entity.ErpMfgWorkOrder wo = new app.erp.mfg.dao.entity.ErpMfgWorkOrder();
            wo.setCode(woCode);
            wo.setProductId("990601");
            wo.setPlannedQuantity(new BigDecimal("10"));
            wo.setBusinessDate(LocalDate.of(2026, 8, 10));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
            wo.setExchangeRate(BigDecimal.ONE);
            daoProvider.daoFor(app.erp.mfg.dao.entity.ErpMfgWorkOrder.class).saveEntity(wo);

            ErpMfgCostVariance line1 = new ErpMfgCostVariance();
            line1.setWorkOrderId(wo.getId());
            line1.setLineNo(1);
            line1.setVarianceType(ErpMfgConstants.VARIANCE_TYPE_MATERIAL_USAGE);
            line1.setCostElement(ErpMfgConstants.COST_ELEMENT_MATERIAL);
            line1.setBusinessDate(LocalDate.of(2026, 8, 10));
            line1.setVarianceAmount(new BigDecimal("12.5"));
            daoProvider.daoFor(ErpMfgCostVariance.class).saveEntity(line1);

            ErpMfgCostVariance line2 = new ErpMfgCostVariance();
            line2.setWorkOrderId(wo.getId());
            line2.setLineNo(2);
            line2.setVarianceType(ErpMfgConstants.VARIANCE_TYPE_LABOR_RATE);
            line2.setCostElement(ErpMfgConstants.COST_ELEMENT_LABOR);
            line2.setBusinessDate(LocalDate.of(2026, 8, 10));
            line2.setVarianceAmount(new BigDecimal("-3"));
            line2.setPosted(true);
            daoProvider.daoFor(ErpMfgCostVariance.class).saveEntity(line2);
            return wo.getId();
        });

        ormTemplate.runInSession(session -> {
            dispatch("PRODUCTION_VARIANCE", woCode + "-PV");
            session.flush();
            return null;
        });

        ormTemplate.runInSession(session -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("workOrderId", woId));
            for (ErpMfgCostVariance line : daoProvider.daoFor(ErpMfgCostVariance.class).findAllByQuery(q)) {
                assertEquals(Boolean.TRUE, line.getPosted(),
                        "PV 凭证（-PV 后缀）strip 反查工单名下全部差异行 posted=true");
            }
            return null;
        });
    }

    @Test
    public void testAlreadyPostedSubcontractSkipsUpdate() {
        String code = "SC-F21-POST-002";
        ormTemplate.runInSession(session -> {
            seedSubcontractOrder(code, true);
            return null;
        });
        int versionBefore = ormTemplate.runInSession(session ->
                findSubcontractOrder(code).getVersion());

        ormTemplate.runInSession(session -> {
            dispatch("SUBCONTRACT_FEE", code + "-SF");
            session.flush();
            return null;
        });

        ErpMfgSubcontractOrder after = findSubcontractOrder(code);
        assertEquals(Boolean.TRUE, after.getPosted(), "已 posted 保持 true");
        assertEquals(versionBefore, after.getVersion(), "已 true 跳过：version 无谓递增被防住");
    }

    // ---------- helpers ----------

    private void dispatch(String businessType, String billHeadCode) {
        MfgSubcontractReversalListener listener = new MfgSubcontractReversalListener();
        listener.daoProvider = daoProvider;
        VoucherPostedEvent event = new VoucherPostedEvent();
        event.setBusinessType(businessType);
        event.setBillHeadCode(billHeadCode);
        listener.onVoucherPosted(event, CTX);
    }

    private ErpMfgSubcontractOrder findSubcontractOrder(String code) {
        return ormTemplate.runInSession(session -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
            q.setLimit(1);
            java.util.List<ErpMfgSubcontractOrder> list =
                    daoProvider.daoFor(ErpMfgSubcontractOrder.class).findAllByQuery(q);
            return list.isEmpty() ? null : list.get(0);
        });
    }

    private ErpMfgMaterialIssue findMaterialIssue(String code) {
        return ormTemplate.runInSession(session -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
            q.setLimit(1);
            java.util.List<ErpMfgMaterialIssue> list =
                    daoProvider.daoFor(ErpMfgMaterialIssue.class).findAllByQuery(q);
            return list.isEmpty() ? null : list.get(0);
        });
    }

    private void seedSubcontractOrder(String code, boolean posted) {
        ErpMfgSubcontractOrder order = new ErpMfgSubcontractOrder();
        order.setCode(code);
        order.setSupplierId("990101");
        order.setProductId("990601");
        order.setBusinessDate(LocalDate.of(2026, 8, 10));
        order.setCurrencyId("990301");
        order.setExchangeRate(BigDecimal.ONE);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED);
        order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_APPROVED);
        order.setPostedStatus("PENDING");
        order.setPosted(posted);
        if (posted) {
            order.setPostedAt(CoreMetrics.currentTimestamp());
            order.setPostedBy("poster");
        }
        daoProvider.daoFor(ErpMfgSubcontractOrder.class).saveEntity(order);
    }

    private void seedMaterialIssue(String code, boolean posted) {
        ErpMfgMaterialIssue issue = new ErpMfgMaterialIssue();
        issue.setCode(code);
        issue.setWorkOrderId("990701");
        issue.setWarehouseId("990201");
        issue.setBusinessDate(LocalDate.of(2026, 8, 10));
        issue.setExchangeRate(BigDecimal.ONE);
        issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_DONE);
        issue.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_APPROVED);
        issue.setPosted(posted);
        if (posted) {
            issue.setPostedAt(CoreMetrics.currentTimestamp());
            issue.setPostedBy("poster");
        }
        daoProvider.daoFor(ErpMfgMaterialIssue.class).saveEntity(issue);
    }
}
