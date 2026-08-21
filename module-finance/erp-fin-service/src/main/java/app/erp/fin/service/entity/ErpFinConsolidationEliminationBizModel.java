
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinConsolidationEliminationBiz;
import app.erp.fin.dao.entity.ErpFinConsolidationElimination;
import app.erp.fin.service.processor.ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor;
import app.erp.fin.service.processor.ErpFinConsolidationEliminationPostEliminationProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 合并抵消候选识别 BizModel（plan 2026-07-22-1000-1 A3，multi-company.md §合并抵消范围）。
 *
 * <p>{@code generateEliminationCandidates(periodId)} 与 {@code postElimination(candidateId)} 分别委派
 * 对应 per-mutation Processor。config-gated {@code erp-fin.consolidation-elimination-enabled} 默认 false。
 *
 * <p>权威：{@code docs/architecture/multi-company.md §Decision D}。
 */
@BizModel("ErpFinConsolidationElimination")
public class ErpFinConsolidationEliminationBizModel extends CrudBizModel<ErpFinConsolidationElimination>
        implements IErpFinConsolidationEliminationBiz {

    @Inject
    ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor generateEliminationCandidatesProcessor;
    @Inject
    ErpFinConsolidationEliminationPostEliminationProcessor postEliminationProcessor;

    public ErpFinConsolidationEliminationBizModel() {
        setEntityName(ErpFinConsolidationElimination.class.getName());
    }

    @Override
    @BizMutation
    public int generateEliminationCandidates(@Name("periodId") String periodId, IServiceContext context) {
        return generateEliminationCandidatesProcessor.generateEliminationCandidates(periodId, context);
    }

    @Override
    @BizMutation
    public String postElimination(@Name("candidateId") String candidateId, IServiceContext context) {
        return postEliminationProcessor.postElimination(candidateId, context);
    }
}
