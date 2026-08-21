
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBankReconciliationBiz;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.service.processor.ErpFinBankReconciliationGenerateProcessor;
import app.erp.fin.service.processor.ErpFinBankReconciliationPostProcessor;
import app.erp.fin.service.processor.ErpFinBankReconciliationReverseProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

@BizModel("ErpFinBankReconciliation")
public class ErpFinBankReconciliationBizModel extends CrudBizModel<ErpFinBankReconciliation>
        implements IErpFinBankReconciliationBiz {
    public ErpFinBankReconciliationBizModel() {
        setEntityName(ErpFinBankReconciliation.class.getName());
    }

    @Inject
    ErpFinBankReconciliationGenerateProcessor generateProcessor;
    @Inject
    ErpFinBankReconciliationPostProcessor postProcessor;
    @Inject
    ErpFinBankReconciliationReverseProcessor reverseProcessor;

    @Override
    @BizMutation
    public ErpFinBankReconciliation generate(@Name("statementId") String statementId, IServiceContext context) {
        return generateProcessor.generate(statementId, context);
    }

    @Override
    @BizMutation
    public ErpFinBankReconciliation post(@Name("reconciliationId") String reconciliationId, IServiceContext context) {
        return postProcessor.post(reconciliationId, context);
    }

    @Override
    @BizMutation
    public ErpFinBankReconciliation reverse(@Name("reconciliationId") String reconciliationId, IServiceContext context) {
        return reverseProcessor.reverse(reconciliationId, context);
    }

}
