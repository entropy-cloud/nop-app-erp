package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * F3.8 批（plan 2026-09-17-0330-1）fin-009 Proof：红冲草稿逐行复制源币双金额（取负；null 保持回退）——
 * 原实现丢失 amountSource/amountFunctional 致多币种红冲行源币金额被本位币金额顶替。
 * 纯函数直测（buildReversalDraft 无 IoC 依赖字段），同包访问 protected。
 */
public class TestErpFinP38ReversalProofs {

    @Test
    public void testBuildReversalDraftCopiesDualAmountsNegated() {
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();
        ErpFinVoucherLine line = new ErpFinVoucherLine();
        line.setSubjectId("5001");
        line.setSubjectCode("5001");
        line.setDcDirection(ErpFinConstants.DC_DEBIT);
        line.setDebitAmount(new BigDecimal("113"));
        line.setCreditAmount(BigDecimal.ZERO);
        line.setAmountSource(new BigDecimal("100"));
        line.setAmountFunctional(new BigDecimal("113"));

        ErpFinPostingProcessor.ReversalDraft draft =
                processor.buildReversalDraft(java.util.List.of(line), ErpFinBusinessType.AP_INVOICE, null);

        assertEquals(1, draft.facts.size());
        var fact = draft.facts.get(0);
        assertEquals(0, fact.getAmountSource().compareTo(new BigDecimal("-100")),
                "红冲行 amountSource = 原行源币金额取负（修复前被本位币 -113 顶替）");
        assertEquals(0, fact.getAmountFunctional().compareTo(new BigDecimal("-113")),
                "红冲行 amountFunctional = 原行本位币金额取负");
    }

    @Test
    public void testBuildReversalDraftNullAmountsKeepFallback() {
        ErpFinPostingProcessor processor = new ErpFinPostingProcessor();
        ErpFinVoucherLine line = new ErpFinVoucherLine();
        line.setSubjectId("5001");
        line.setDcDirection(ErpFinConstants.DC_CREDIT);
        line.setDebitAmount(BigDecimal.ZERO);
        line.setCreditAmount(new BigDecimal("50"));

        ErpFinPostingProcessor.ReversalDraft draft =
                processor.buildReversalDraft(java.util.List.of(line), ErpFinBusinessType.AP_INVOICE, null);

        assertNull(draft.facts.get(0).getAmountSource(), "null 源币金额不复制（保持引擎回退）");
        assertNull(draft.facts.get(0).getAmountFunctional(), "null 本位币金额不复制（保持引擎回退）");
    }
}
