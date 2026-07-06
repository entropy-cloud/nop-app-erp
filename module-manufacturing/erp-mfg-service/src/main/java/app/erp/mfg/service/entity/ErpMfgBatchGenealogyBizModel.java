
package app.erp.mfg.service.entity;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.mfg.biz.IErpMfgBatchGenealogyBiz;
import app.erp.mfg.biz.RecallReport;
import app.erp.mfg.dao.entity.ErpMfgBatchGenealogy;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.genealogy.BatchGenealogyTracer;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 生产批次基因链追溯 BizModel（plan 2026-07-07-0305-3）。
 *
 * <p>权威：{@code docs/design/manufacturing/batch-genealogy.md}。前向/反向/全链追溯查询经
 * {@link BatchGenealogyTracer}（纯查询服务）；召回报告从问题批次出发全链识别受影响成品批次。
 */
@BizModel("ErpMfgBatchGenealogy")
public class ErpMfgBatchGenealogyBizModel extends CrudBizModel<ErpMfgBatchGenealogy> implements IErpMfgBatchGenealogyBiz {
    public ErpMfgBatchGenealogyBizModel() {
        setEntityName(ErpMfgBatchGenealogy.class.getName());
    }

    @Inject
    BatchGenealogyTracer batchGenealogyTracer;

    public void setBatchGenealogyTracer(BatchGenealogyTracer batchGenealogyTracer) {
        this.batchGenealogyTracer = batchGenealogyTracer;
    }

    @Override
    @BizQuery
    public List<ErpMfgBatchGenealogy> forwardTrace(@Name("outputLotId") Long outputLotId,
                                                    IServiceContext context) {
        requireLot(outputLotId, context);
        return batchGenealogyTracer.forwardTrace(outputLotId);
    }

    @Override
    @BizQuery
    public List<ErpMfgBatchGenealogy> backwardTrace(@Name("inputLotId") Long inputLotId,
                                                     IServiceContext context) {
        requireLot(inputLotId, context);
        return batchGenealogyTracer.backwardTrace(inputLotId);
    }

    @Override
    @BizQuery
    public List<ErpMfgBatchGenealogy> traceChain(@Name("lotId") Long lotId,
                                                  @Name("direction") String direction,
                                                  @Name("maxDepth") Integer maxDepth,
                                                  IServiceContext context) {
        requireLot(lotId, context);
        return batchGenealogyTracer.traceChain(lotId, direction, maxDepth);
    }

    @Override
    @BizQuery
    public RecallReport recallReport(@Name("lotId") Long lotId, IServiceContext context) {
        requireLot(lotId, context);
        RecallReport report = new RecallReport();
        report.setSourceLotId(lotId);
        // 降级标记：当前 inventory 域未暴露按批次的库存位置/已售去向查询方法集，
        // 仅返回受影响成品批次集合（位置/去向归 inventory successor）。
        report.setDegraded(true);

        Set<Long> visited = new HashSet<>();
        visited.add(lotId);

        // 起始批次自身可能是受影响成品批次
        collectAffectedIfFinishedGood(lotId, report);

        // 反向递归（lotId 作为输入或中间品）找出所有下游产出批次（成品）
        List<Long> frontier = new ArrayList<>();
        frontier.add(lotId);
        while (!frontier.isEmpty()) {
            List<Long> nextFrontier = new ArrayList<>();
            for (Long currentLot : frontier) {
                List<ErpMfgBatchGenealogy> edges = batchGenealogyTracer.backwardTrace(currentLot);
                for (ErpMfgBatchGenealogy edge : edges) {
                    Long outputLotId = edge.getOutputLotId();
                    if (outputLotId == null) {
                        continue;
                    }
                    if (visited.add(outputLotId)) {
                        collectAffectedIfFinishedGood(outputLotId, report);
                        nextFrontier.add(outputLotId);
                    }
                }
            }
            frontier = nextFrontier;
        }
        return report;
    }

    protected void collectAffectedIfFinishedGood(Long lotId, RecallReport report) {
        ErpInvBatch lot = batchDao().getEntityById(lotId);
        if (lot == null) {
            return;
        }
        // 排除已拒收批次
        if (ErpMfgConstants.LOT_STATUS_REJECTED.equals(lot.getStatus())) {
            return;
        }
        // 查该批次是否作为某基因行的产出（即被生产出来），视为受影响候选
        List<ErpMfgBatchGenealogy> outputs = batchGenealogyTracer.forwardTrace(lotId);
        if (!outputs.isEmpty()) {
            RecallReport.AffectedLot affected = new RecallReport.AffectedLot();
            affected.setLotId(lotId);
            affected.setBatchNo(lot.getBatchNo());
            affected.setMaterialId(lot.getMaterialId());
            affected.setLotStatus(ErpMfgConstants.LOT_STATUS_RELEASED);
            report.getAffectedLots().add(affected);
        }
    }

    protected void requireLot(Long lotId, IServiceContext context) {
        if (lotId == null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_GENEALOGY_LOT_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_LOT_ID, null);
        }
        ErpInvBatch lot = batchDao().getEntityById(lotId);
        if (lot == null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_GENEALOGY_LOT_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_LOT_ID, lotId);
        }
    }

    protected IEntityDao<ErpInvBatch> batchDao() {
        return daoFor(ErpInvBatch.class);
    }
}
