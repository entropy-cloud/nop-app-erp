package io.nop.app.all.it;

import app.erp.inv.service.barcode.ErpInvBarcodeBizModel;
import app.erp.sal.dao.entity.ErpSalDelivery;
import io.nop.api.core.ioc.BeanContainer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * USC-05 Delivery 单号解析集成断言（plan 2026-10-01-0930-1 B1 裁决承载）。
 *
 * <p>B1：inv→sal DAG 违规禁 pom 依赖 ⇒ inv-service 单域 classpath 无 sal-dao，
 * Delivery 反射解析在该层容错跳过；本测试在 app-erp-all 聚合 classpath（sal-dao 在）
 * 下补齐 SAL_DELIVERY 分支的运行时断言，使 D1 四类单据解析面 4/4 有运行时覆盖。
 */
public class TestErpBarcodeDeliveryResolve extends ErpIntegrationTestCase {

    @Test
    public void testDeliveryDocCodeResolved() {
        // 1. DAO 直种 Delivery（resolveBarcode 只需单号存在；mandatory 列按最小集赋值）
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.setCode("BAR-DLV-001");
        delivery.setCustomerId("1");
        delivery.setWarehouseId("1");
        delivery.setBusinessDate(java.time.LocalDate.of(2026, 10, 1));
        delivery.setCurrencyId("1");
        delivery.setExchangeRate(java.math.BigDecimal.ONE);
        delivery.setDocStatus("DRAFT");
        delivery.setApproveStatus("UNSUBMITTED");
        daoProvider().daoFor(ErpSalDelivery.class).saveEntity(delivery);

        // 2. 反射直查分支（Class.forName app.erp.sal.dao.entity.ErpSalDelivery 在聚合 classpath 命中）
        ErpInvBarcodeBizModel biz = BeanContainer.getBeanByType(ErpInvBarcodeBizModel.class);
        Map<String, Object> r = biz.resolveBarcode("BAR-DLV-001", null);

        assertNotNull(r, "Delivery 单号应可解析");
        assertEquals("SAL_DELIVERY", r.get("refType"));
        assertEquals("BAR-DLV-001", r.get("refCode"));
    }
}
