package app.erp.inv.biz;

/**
 * 追溯链的一条边（移动单间有向关联），用于表达 {@link TraceChainResult} 的链路结构。
 *
 * <p>{@code linkType} 取 {@code app.erp.inv.service.ErpInvConstants#TRACE_LINK_FORWARD}
 * （正向上下游，originMoveId 链）或 {@code TRACE_LINK_RETURN}（退货链，originReturnedMoveId 链）。
 */
public class TraceLink {
    private Long fromMoveId;
    private Long toMoveId;
    private String linkType;

    public TraceLink() {
    }

    public TraceLink(Long fromMoveId, Long toMoveId, String linkType) {
        this.fromMoveId = fromMoveId;
        this.toMoveId = toMoveId;
        this.linkType = linkType;
    }

    public Long getFromMoveId() {
        return fromMoveId;
    }

    public void setFromMoveId(Long fromMoveId) {
        this.fromMoveId = fromMoveId;
    }

    public Long getToMoveId() {
        return toMoveId;
    }

    public void setToMoveId(Long toMoveId) {
        this.toMoveId = toMoveId;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }
}
