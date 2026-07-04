package app.erp.b2b.service.spi.model;

import java.io.Serializable;

/**
 * EDI 格式适用性描述。判断某 Provider 是否处理某类业务单据的导出/导入。
 *
 * <p>对应 {@code edi-formats.md §1.2}。
 */
public class EdiApplicability implements Serializable {

    public static final EdiApplicability NONE = new EdiApplicability(false, false, false);

    private final boolean outbound;
    private final boolean inbound;
    private final boolean batchable;

    public EdiApplicability(boolean outbound, boolean inbound, boolean batchable) {
        this.outbound = outbound;
        this.inbound = inbound;
        this.batchable = batchable;
    }

    public boolean isOutbound() {
        return outbound;
    }

    public boolean isInbound() {
        return inbound;
    }

    public boolean isBatchable() {
        return batchable;
    }
}
