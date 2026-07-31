package app.erp.fin.service.processor;

import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ErpFinReconciliation create per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含核销单创建编排（草稿头 + 行）。共享 helper 单一真相源在 {@link AbstractErpFinReconciliationProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinReconciliationCreateProcessor extends AbstractErpFinReconciliationProcessor {

    public ErpFinReconciliation create(String direction, Long partnerId, LocalDate businessDate,
                                       List<ReconciliationLineInput> lines, IServiceContext context) {
        if (direction == null || partnerId == null || businessDate == null
                || lines == null || lines.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_DIRECTION_MISMATCH)
                    .param(ErpFinErrors.ARG_DIRECTION, direction);
        }

        ErpFinArApItem sample = loadItem(lines.get(0).getInvoiceItemId());

        IEntityDao<ErpFinReconciliation> headDao = daoProvider().daoFor(ErpFinReconciliation.class);
        ErpFinReconciliation head = headDao.newEntity();
        head.setCode("REC-" + StringHelper.generateUUID().substring(0, 12));
        head.setOrgId(sample.getOrgId());
        head.setAcctSchemaId(sample.getAcctSchemaId());
        head.setDirection(direction);
        head.setPartnerId(partnerId);
        head.setBusinessDate(businessDate);
        head.setCurrencyId(sample.getCurrencyId());
        head.setExchangeRate(sample.getExchangeRate() != null ? sample.getExchangeRate() : BigDecimal.ONE);
        head.setTotalAmountSource(BigDecimal.ZERO);
        head.setTotalAmountFunctional(BigDecimal.ZERO);
        head.setFxGainLoss(BigDecimal.ZERO);
        head.setDocStatus(ErpFinConstants.RECON_STATUS_DRAFT);
        headDao.saveEntity(head);

        IEntityDao<ErpFinReconciliationLine> lineDao = daoProvider().daoFor(ErpFinReconciliationLine.class);
        int lineNo = 1;
        for (ReconciliationLineInput in : lines) {
            ErpFinReconciliationLine line = lineDao.newEntity();
            line.setReconciliationId(head.getId());
            line.setLineNo(lineNo++);
            line.setPaymentItemId(in.getPaymentItemId());
            line.setInvoiceItemId(in.getInvoiceItemId());
            line.setSettledAmountSource(nz(in.getSettledAmountSource()));
            line.setSettledAmountFunctional(nz(in.getSettledAmountFunctional()));
            lineDao.saveEntity(line);
        }
        return head;
    }
}
