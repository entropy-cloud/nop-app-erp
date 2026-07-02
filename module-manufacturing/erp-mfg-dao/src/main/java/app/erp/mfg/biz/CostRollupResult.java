package app.erp.mfg.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 成本卷算结果（{@code IErpMfgBomBiz.rollupCost} 返回值）。{@code rollupId} 指向新建的
 * {@code ErpMfgCostRollup}（status=CALCULATED）；{@code lines} 为按物料的单位标准成本分解。
 *
 * <p>FIRMED 由人工动作置位（本计划 Non-Goal）；N=1 STANDARD 成本引擎后继按 {@code materialId} 取最新 FIRMED 行的
 * {@code unitCost}（一次 join，可接受）。
 */
@DataBean
public class CostRollupResult {

    private Long rollupId;
    private Integer status;
    private List<CostRollupLineView> lines = new ArrayList<>();

    public CostRollupResult() {
    }

    public Long getRollupId() {
        return rollupId;
    }

    public void setRollupId(Long rollupId) {
        this.rollupId = rollupId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<CostRollupLineView> getLines() {
        return lines;
    }

    public void setLines(List<CostRollupLineView> lines) {
        this.lines = lines;
    }
}
