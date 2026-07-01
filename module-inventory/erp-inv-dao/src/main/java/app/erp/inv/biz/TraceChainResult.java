package app.erp.inv.biz;

import app.erp.inv.dao.entity.ErpInvStockMove;

import java.util.ArrayList;
import java.util.List;

/**
 * 追溯链查询结果：移动单节点列表 + 链路边结构 + 截断标志。
 *
 * <p>由 {@code IErpInvStockMoveBiz.forwardTrace/backwardTrace/returnTrace/batchTrace} 返回。
 * 节点列表 {@link #nodes} 含链路中全部相关移动单（含根节点），边列表 {@link #links} 描述节点间的有向关联
 * （正向 {@code FORWARD} 或退货 {@code RETURN}）。{@link #truncated} 为 true 表示因最大深度兜底或环检测而截断。
 *
 * <p>当 {@code erp-inv.trace-chain-enabled=false} 时，forward/backward/return 返回仅含根节点的单节点结果（空链）。
 *
 * <p>权威：{@code docs/design/inventory/trace-chain.md §追溯链查询}。
 */
public class TraceChainResult {
    private String traceType;
    private Long rootMoveId;
    private List<ErpInvStockMove> nodes = new ArrayList<>();
    private List<TraceLink> links = new ArrayList<>();
    private boolean truncated;

    public TraceChainResult() {
    }

    public TraceChainResult(String traceType) {
        this.traceType = traceType;
    }

    public String getTraceType() {
        return traceType;
    }

    public void setTraceType(String traceType) {
        this.traceType = traceType;
    }

    public Long getRootMoveId() {
        return rootMoveId;
    }

    public void setRootMoveId(Long rootMoveId) {
        this.rootMoveId = rootMoveId;
    }

    public List<ErpInvStockMove> getNodes() {
        return nodes;
    }

    public void setNodes(List<ErpInvStockMove> nodes) {
        this.nodes = nodes;
    }

    public List<TraceLink> getLinks() {
        return links;
    }

    public void setLinks(List<TraceLink> links) {
        this.links = links;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }
}
