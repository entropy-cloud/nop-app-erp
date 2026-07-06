package app.erp.mfg.service.genealogy;

import app.erp.mfg.dao.entity.ErpMfgBatchGenealogy;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 生产批次基因链追溯查询服务（plan 2026-07-07-0305-3 §Phase 2）。
 *
 * <p>权威：{@code docs/design/manufacturing/batch-genealogy.md}。
 *
 * <ul>
 *   <li>{@link #forwardTrace(Long)}：产出批次 → 所有直接输入批次（成品→原料）。</li>
 *   <li>{@link #backwardTrace(Long)}：输入批次 → 所有直接产出批次（原料→成品）。</li>
 *   <li>{@link #traceChain(Long, String, Integer)}：迭代递归多级追溯，带环路防护 + maxDepth 上限。</li>
 * </ul>
 *
 * <p>递归方向（traceChain）：
 * <ul>
 *   <li>FORWARD：用 output_lot 找 input 再向上游递归（output_lot 的 input 即更上游的产出）。</li>
 *   <li>BACKWARD：用 input 找 output 再向下游递归（input_lot 的 output 即更下游的投入）。</li>
 * </ul>
 */
public class BatchGenealogyTracer {

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public List<ErpMfgBatchGenealogy> forwardTrace(Long outputLotId) {
        IEntityDao<ErpMfgBatchGenealogy> dao = genealogyDao();
        QueryBean q = new QueryBean();
        q.addFilter(eq("outputLotId", outputLotId));
        return dao.findAllByQuery(q);
    }

    public List<ErpMfgBatchGenealogy> backwardTrace(Long inputLotId) {
        IEntityDao<ErpMfgBatchGenealogy> dao = genealogyDao();
        QueryBean q = new QueryBean();
        q.addFilter(eq("inputLotId", inputLotId));
        return dao.findAllByQuery(q);
    }

    /**
     * 多级递归追溯。direction=FORWARD 时从 lotId 作为产出批次向上游（原料方向）递归；
     * direction=BACKWARD 时从 lotId 作为输入批次向下游（成品方向）递归。
     * 含已访问集合环路防护 + maxDepth 上限（超限抛 ErrorCode）。
     */
    public List<ErpMfgBatchGenealogy> traceChain(Long lotId, String direction, Integer maxDepth) {
        if (direction == null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_GENEALOGY_INVALID_DIRECTION)
                    .param(ErpMfgErrors.ARG_DIRECTION, String.valueOf(direction));
        }
        boolean forward;
        if (ErpMfgConstants.TRACE_DIRECTION_FORWARD.equals(direction)) {
            forward = true;
        } else if (ErpMfgConstants.TRACE_DIRECTION_BACKWARD.equals(direction)) {
            forward = false;
        } else {
            throw new NopException(ErpMfgErrors.ERR_MFG_GENEALOGY_INVALID_DIRECTION)
                    .param(ErpMfgErrors.ARG_DIRECTION, direction);
        }

        int depth = resolveMaxDepth(maxDepth);
        List<ErpMfgBatchGenealogy> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        List<Long> frontier = new ArrayList<>();
        frontier.add(lotId);
        visited.add(lotId);

        int currentDepth = 0;
        while (!frontier.isEmpty()) {
            if (currentDepth >= depth) {
                throw new NopException(ErpMfgErrors.ERR_MFG_GENEALOGY_MAX_DEPTH_EXCEEDED)
                        .param(ErpMfgErrors.ARG_DEPTH, depth);
            }
            List<Long> nextFrontier = new ArrayList<>();
            for (Long currentLot : frontier) {
                List<ErpMfgBatchGenealogy> edges = forward
                        ? forwardTrace(currentLot)
                        : backwardTrace(currentLot);
                for (ErpMfgBatchGenealogy edge : edges) {
                    result.add(edge);
                    Long nextLot = forward ? edge.getInputLotId() : edge.getOutputLotId();
                    if (nextLot != null && visited.add(nextLot)) {
                        nextFrontier.add(nextLot);
                    }
                }
            }
            frontier = nextFrontier;
            currentDepth++;
        }
        return result;
    }

    protected int resolveMaxDepth(Integer maxDepth) {
        int depth = maxDepth != null ? maxDepth : defaultMaxDepth();
        if (depth <= 0) {
            depth = defaultMaxDepth();
        }
        return depth;
    }

    protected int defaultMaxDepth() {
        try {
            String value = AppConfig.var(ErpMfgConstants.CONFIG_GENEALOGY_MAX_TRACE_DEPTH,
                    String.valueOf(ErpMfgConstants.DEFAULT_GENEALOGY_MAX_TRACE_DEPTH));
            if (value == null || value.trim().isEmpty()) {
                return ErpMfgConstants.DEFAULT_GENEALOGY_MAX_TRACE_DEPTH;
            }
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : ErpMfgConstants.DEFAULT_GENEALOGY_MAX_TRACE_DEPTH;
        } catch (Exception e) {
            return ErpMfgConstants.DEFAULT_GENEALOGY_MAX_TRACE_DEPTH;
        }
    }

    protected IEntityDao<ErpMfgBatchGenealogy> genealogyDao() {
        return daoProvider.daoFor(ErpMfgBatchGenealogy.class);
    }
}
