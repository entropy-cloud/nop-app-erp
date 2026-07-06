
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mfg.dao.entity.ErpMfgBatchGenealogy;

import java.util.List;

/**
 * 生产批次基因链追溯业务接口。
 *
 * <p>除标准 CRUD 外，定义前向/反向/全链追溯查询与召回范围报告（plan 2026-07-07-0305-3）。
 *
 * <p>权威：{@code docs/design/manufacturing/batch-genealogy.md}。
 */
public interface IErpMfgBatchGenealogyBiz extends ICrudBiz<ErpMfgBatchGenealogy>{

    /**
     * 前向追溯：给定产出批次，查找所有直接输入批次（成品→原料单级）。
     */
    @BizQuery
    List<ErpMfgBatchGenealogy> forwardTrace(@Name("outputLotId") Long outputLotId,
                                            IServiceContext context);

    /**
     * 反向追溯：给定输入批次，查找所有直接产出批次（原料→成品单级）。
     */
    @BizQuery
    List<ErpMfgBatchGenealogy> backwardTrace(@Name("inputLotId") Long inputLotId,
                                             IServiceContext context);

    /**
     * 全链递归追溯（含环路防护与深度上限）。
     *
     * @param lotId     起始批次ID
     * @param direction FORWARD（成品→原料）/ BACKWARD（原料→成品）
     * @param maxDepth  递归深度上限（null 取配置 erp-mfg.genealogy-max-trace-depth 默认 50）
     */
    @BizQuery
    List<ErpMfgBatchGenealogy> traceChain(@Name("lotId") Long lotId,
                                          @Name("direction") String direction,
                                          @Name("maxDepth") Integer maxDepth,
                                          IServiceContext context);

    /**
     * 召回范围报告：从问题批次出发全链识别所有受影响成品批次。
     *
     * <p>降级说明：当前仅返回受影响成品批次集合；位置/去向查询归 inventory successor。
     */
    @BizQuery
    RecallReport recallReport(@Name("lotId") Long lotId, IServiceContext context);
}
