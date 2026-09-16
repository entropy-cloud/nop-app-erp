package app.erp.fin.service.processor;


import app.erp.fin.service.ErpFinConstants;
import org.junit.jupiter.api.Test;

import app.erp.fin.dao.ErpFinBusinessType;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F3.8 批（plan 2026-09-17-0330-1）结束审计整改：fin-009/fin-013 Proof 纯函数级断言。
 *
 * <ul>
 *   <li>fin-014/fin-009 的其余 Proof 见 posting 包 TestErpFinP38ReversalProofs / workbench 既有覆盖。</li>
 *   <li>fin-013：手动重试 rebuildEvent 缺失汇率透传 null（交引擎 guardExchangeRate 判定
 *       ERR_EXCHANGE_RATE_REQUIRED），修复原预置 rate=1 击穿 RC-R1.42 的旁路。</li>
 * </ul>
 * 纯函数直接构造被测组件（无 IoC 依赖字段参与），同包访问 protected 方法。
 */
public class TestErpFinP38Proofs {

    // ---------- fin-013：手动重试汇率透传 null ----------

    @Test
    public void testManualRetryRebuildEventPassesThroughNullRate() {
        ErpFinPostingExceptionRetryProcessor processor = new ErpFinPostingExceptionRetryProcessor();
        var entity = new app.erp.fin.dao.entity.ErpFinPostingException();
        entity.setTraceId("tr-1");
        entity.setBillHeadCode("BI-X");
        entity.setBusinessType("AR_INVOICE");
        entity.setVoucherDate(java.time.LocalDate.of(2026, 7, 15));
        entity.setOrgId("1");
        entity.setAcctSchemaId("1");
        entity.setCurrencyId("USD");
        entity.setExchangeRate(null);

        var event = processor.rebuildEvent(entity);
        assertNull(event.getExchangeRate(),
                "缺失汇率透传 null → 引擎 guardExchangeRate 抛 ERR_EXCHANGE_RATE_REQUIRED（修复前预置 rate=1 击穿 RC-R1.42）");
    }

    @Test
    public void testManualRetryRebuildEventKeepsExistingRate() {
        ErpFinPostingExceptionRetryProcessor processor = new ErpFinPostingExceptionRetryProcessor();
        var entity = new app.erp.fin.dao.entity.ErpFinPostingException();
        entity.setTraceId("tr-2");
        entity.setBillHeadCode("BI-Y");
        entity.setBusinessType("AR_INVOICE");
        entity.setExchangeRate(new BigDecimal("7.25"));

        var event = processor.rebuildEvent(entity);
        assertNotNull(event.getExchangeRate());
        assertEquals(0, event.getExchangeRate().compareTo(new BigDecimal("7.25")), "既有汇率保持");
    }
}
