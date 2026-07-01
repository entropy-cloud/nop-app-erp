package app.erp.inv.service.trace;

import app.erp.inv.biz.TraceChainResult;
import app.erp.inv.biz.TraceLink;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 库存追溯链查询助手：基于移动单自追溯上链（{@code originMoveId} 正向链 + {@code originReturnedMoveId} 退货链）
 * 实现四类只读追溯查询。下游链以反向查询表达（不存 M2M 中间表——见 plan Task Route Decision）。
 *
 * <ul>
 *   <li>{@link #forwardTrace}：正向 origin→dest，按 {@code originMoveId=?} 反查下游，递归。</li>
 *   <li>{@link #backwardTrace}：反向 dest→origin，沿 {@code originMoveId} 上溯，递归。</li>
 *   <li>{@link #returnTrace}：退货链，按 {@code originReturnedMoveId} 双向（原单↔退货单）。</li>
 *   <li>{@link #batchTrace}：按 {@code batchNo} 跨移动单行 + 流水聚合相关移动单。</li>
 * </ul>
 *
 * <p>递归带最大深度兜底（{@code erp-inv.trace-chain-max-depth}，默认 10）+ 已访问集合环检测
 * （重复节点截断并置 {@code truncated=true}，成环数据本身是脏数据，查询截断而非报错）。
 * 全部查询过滤 {@code delVersion=0}（逻辑删除/已删节点不出现在链中）。
 *
 * <p>权威：{@code docs/design/inventory/trace-chain.md §追溯链查询}。
 */
public class TraceChainQuery {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 正向追溯：从 {@code moveId} 出发，递归收集所有下游移动单（{@code originMoveId} 指向当前节点的移动单）。
     * 关闭（{@code enabled=false}）时仅返回根节点单节点结果。
     */
    public TraceChainResult forwardTrace(Long moveId, boolean enabled, int maxDepth) {
        TraceChainResult result = new TraceChainResult("FORWARD");
        result.setRootMoveId(moveId);
        ErpInvStockMove root = findActiveMove(moveId);
        if (root == null) {
            return result;
        }
        result.getNodes().add(root);
        if (!enabled) {
            return result;
        }
        Set<Long> visited = new HashSet<>();
        visited.add(moveId);
        Deque<Long> frontier = new ArrayDeque<>();
        frontier.add(moveId);
        int depth = 0;
        while (!frontier.isEmpty()) {
            if (depth >= maxDepth) {
                result.setTruncated(true);
                break;
            }
            int levelSize = frontier.size();
            for (int i = 0; i < levelSize; i++) {
                Long current = frontier.poll();
                for (ErpInvStockMove downstream : findActiveMovesByOrigin(current)) {
                    result.getLinks().add(new TraceLink(current, downstream.getId(),
                            ErpInvConstants.TRACE_LINK_FORWARD));
                    if (!visited.add(downstream.getId())) {
                        result.setTruncated(true);
                        continue;
                    }
                    result.getNodes().add(downstream);
                    frontier.add(downstream.getId());
                }
            }
            depth++;
        }
        return result;
    }

    /**
     * 反向追溯：从 {@code moveId} 出发，沿 {@code originMoveId} 逐层上溯至无上游的根。
     * 关闭时仅返回根节点。
     */
    public TraceChainResult backwardTrace(Long moveId, boolean enabled, int maxDepth) {
        TraceChainResult result = new TraceChainResult("BACKWARD");
        result.setRootMoveId(moveId);
        ErpInvStockMove current = findActiveMove(moveId);
        if (current == null) {
            return result;
        }
        result.getNodes().add(current);
        if (!enabled) {
            return result;
        }
        Set<Long> visited = new HashSet<>();
        visited.add(moveId);
        int depth = 0;
        while (current.getOriginMoveId() != null) {
            if (depth >= maxDepth) {
                result.setTruncated(true);
                break;
            }
            Long parentId = current.getOriginMoveId();
            if (!visited.add(parentId)) {
                result.setTruncated(true);
                break;
            }
            ErpInvStockMove parent = findActiveMove(parentId);
            if (parent == null) {
                break;
            }
            result.getLinks().add(new TraceLink(parentId, current.getId(),
                    ErpInvConstants.TRACE_LINK_FORWARD));
            result.getNodes().add(parent);
            current = parent;
            depth++;
        }
        return result;
    }

    /**
     * 退货追溯（双向）：给定退货移动单 → 返回其原出/入库移动单；给定原移动单 → 返回其全部退货移动单。
     * 锚点（原单）= 根节点的 {@code originReturnedMoveId}（若根为退货单），否则根节点本身（视为原单）。
     */
    public TraceChainResult returnTrace(Long moveId, boolean enabled) {
        TraceChainResult result = new TraceChainResult("RETURN");
        result.setRootMoveId(moveId);
        ErpInvStockMove root = findActiveMove(moveId);
        if (root == null) {
            return result;
        }
        Set<Long> nodeIds = new LinkedHashSet<>();
        nodeIds.add(moveId);
        result.getNodes().add(root);
        if (!enabled) {
            return result;
        }
        Long anchorId = root.getOriginReturnedMoveId() != null
                ? root.getOriginReturnedMoveId() : moveId;
        ErpInvStockMove anchor = findActiveMove(anchorId);
        if (anchor != null && nodeIds.add(anchorId)) {
            result.getNodes().add(anchor);
        }
        for (ErpInvStockMove ret : findActiveReturnsOf(anchorId)) {
            result.getLinks().add(new TraceLink(anchorId, ret.getId(),
                    ErpInvConstants.TRACE_LINK_RETURN));
            if (nodeIds.add(ret.getId())) {
                result.getNodes().add(ret);
            }
        }
        return result;
    }

    /**
     * 批次追溯：按 {@code batchNo} 跨移动单行（{@code ErpInvStockMoveLine.batchNo}）与不可变流水
     * （{@code ErpInvStockLedger.batchNo}）聚合全部相关移动单。关闭时返回空结果。
     */
    public TraceChainResult batchTrace(String batchNo, boolean enabled) {
        TraceChainResult result = new TraceChainResult("BATCH");
        if (batchNo == null || batchNo.isEmpty() || !enabled) {
            return result;
        }
        Set<Long> moveIds = new LinkedHashSet<>();
        for (ErpInvStockMoveLine line : findLinesByBatch(batchNo)) {
            if (line.getMoveId() != null) {
                moveIds.add(line.getMoveId());
            }
        }
        for (ErpInvStockLedger ledger : findLedgersByBatch(batchNo)) {
            if (ledger.getMoveId() != null) {
                moveIds.add(ledger.getMoveId());
            }
        }
        Set<Long> added = new HashSet<>();
        for (Long mid : moveIds) {
            ErpInvStockMove move = findActiveMove(mid);
            if (move != null && added.add(mid)) {
                result.getNodes().add(move);
            }
        }
        return result;
    }

    // ---------- query primitives ----------

    private ErpInvStockMove findActiveMove(Long id) {
        if (id == null) {
            return null;
        }
        List<ErpInvStockMove> list = findActiveMovesByQuery(q -> {
            q.addFilter(eq("id", id));
        });
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvStockMove> findActiveMovesByOrigin(Long originMoveId) {
        if (originMoveId == null) {
            return new ArrayList<>();
        }
        return findActiveMovesByQuery(q -> q.addFilter(eq("originMoveId", originMoveId)));
    }

    private List<ErpInvStockMove> findActiveReturnsOf(Long originReturnedMoveId) {
        if (originReturnedMoveId == null) {
            return new ArrayList<>();
        }
        return findActiveMovesByQuery(q -> q.addFilter(eq("originReturnedMoveId", originReturnedMoveId)));
    }

    private List<ErpInvStockMove> findActiveMovesByQuery(java.util.function.Consumer<QueryBean> preparer) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        preparer.accept(q);
        q.addFilter(eq("delVersion", 0L));
        return dao.findAllByQuery(q);
    }

    private List<ErpInvStockMoveLine> findLinesByBatch(String batchNo) {
        IEntityDao<ErpInvStockMoveLine> dao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("batchNo", batchNo));
        return dao.findAllByQuery(q);
    }

    private List<ErpInvStockLedger> findLedgersByBatch(String batchNo) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("batchNo", batchNo));
        return dao.findAllByQuery(q);
    }
}
