package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P2-CK-pur-006：聚合锁点直接调用测试（plan 2026-09-16-1230-1 Phase 3 Proof）。
 * {@code lockOrderForToleranceAggregation} 在显式事务上下文中可锁（防 {@code ERR_ORM_LOCK_MUST_RUN_IN_TXN}
 * 回归）且锁后同事务聚合读正常；SELECT FOR UPDATE 为纯锁不写——订单 version 不被污染
 * （对齐 inv 域 {@code TestErpInvLandedCostReceiveMutex} 同型先例）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurAggregationLock extends JunitAutoTestCase {

    static final String ORG_ID = "1901";
    static final String SUPPLIER_ID = "2901";
    static final String WAREHOUSE_ID = "3901";
    static final String CURRENCY_ID = "6901";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    ErpPurReceiveProcessor receiveProcessor;

    @Test
    public void testLockOrderForToleranceAggregationInTransaction() {
        String orderId = seedOrder("PO-LOCK-001");

        transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn -> {
            receiveProcessor.lockOrderForToleranceAggregation(orderId);
            // 锁后同事务聚合读正常（容差校验真实路径的前置步骤）
            assertEquals(1, daoProvider.daoFor(ErpPurOrder.class).findAllByQuery(
                            new io.nop.api.core.beans.query.QueryBean()).size(),
                    "锁后聚合读返回种子订单");
            return null;
        });

        ErpPurOrder after = daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId);
        assertEquals(0, after.getVersion(), "SELECT FOR UPDATE 纯锁不写，订单 version 保持 0");
    }

    private String seedOrder(String code) {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder order = dao.newEntity();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setTotalAmount(new BigDecimal("50"));
        order.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        dao.saveEntity(order);
        return order.getId();
    }
}
