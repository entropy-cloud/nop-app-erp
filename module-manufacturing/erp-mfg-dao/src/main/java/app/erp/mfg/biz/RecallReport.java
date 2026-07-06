package app.erp.mfg.biz;

import java.util.ArrayList;
import java.util.List;

/**
 * 召回范围报告（plan 2026-07-07-0305-3 §Phase 2）。
 *
 * <p>由 {@code IErpMfgBatchGenealogyBiz.recallReport} 返回，供质量召回事件 successor 消费。
 * 从问题批次出发全链识别所有受影响成品批次。
 *
 * <p>降级说明：当前 {@code IErpInvStockBalanceBiz}/{@code IErpInvStockMoveBiz} 未暴露
 * 「按批次的当前库存位置」与「已售去向」查询方法集（仅 CRUD），故 {@link #affectedLots}
 * 仅返回受影响成品批次集合；位置/去向查询归 inventory successor。
 *
 * <p>权威：{@code docs/design/manufacturing/batch-genealogy.md §场景 1：成品批次召回}。
 */
public class RecallReport {

    private Long sourceLotId;
    private List<AffectedLot> affectedLots = new ArrayList<>();
    private boolean degraded;

    public RecallReport() {
    }

    public Long getSourceLotId() {
        return sourceLotId;
    }

    public void setSourceLotId(Long sourceLotId) {
        this.sourceLotId = sourceLotId;
    }

    public List<AffectedLot> getAffectedLots() {
        return affectedLots;
    }

    public void setAffectedLots(List<AffectedLot> affectedLots) {
        this.affectedLots = affectedLots;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public static class AffectedLot {
        private Long lotId;
        private String batchNo;
        private Long materialId;
        private String lotStatus;

        public AffectedLot() {
        }

        public Long getLotId() {
            return lotId;
        }

        public void setLotId(Long lotId) {
            this.lotId = lotId;
        }

        public String getBatchNo() {
            return batchNo;
        }

        public void setBatchNo(String batchNo) {
            this.batchNo = batchNo;
        }

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public String getLotStatus() {
            return lotStatus;
        }

        public void setLotStatus(String lotStatus) {
            this.lotStatus = lotStatus;
        }
    }
}
