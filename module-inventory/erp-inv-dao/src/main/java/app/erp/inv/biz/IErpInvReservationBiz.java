
package app.erp.inv.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.inv.dao.entity.ErpInvReservation;

/**
 * 库存预留单业务接口。除标准 CRUD 外，定义跨域/物料预留写路径契约（mfg 工单审核/取消/完工/领料消费侧调用）：
 *
 * <ul>
 *   <li>{@link #createReservation(ReservationCreateRequest, IServiceContext)}：按来源单据创建预留头+行，
 *       行预留量 = min(需求, 可用量)，库存余额 reservedQuantity 同步增加（乐观锁 + 重试防护）。</li>
 *   <li>{@link #releaseReservation(String, String, String, IServiceContext)}：按来源单据释放未消耗部分，
 *       库存余额 reservedQuantity 同步减少；头 status 按释放语义映射（reason=CANCELLED → CANCELLED；
 *       reason=COMPLETED → 剩余>0 则 PARTIALLY_CONSUMED / 已全领则 CONSUMED）。</li>
 *   <li>{@link #consumeReservation(ReservationConsumeRequest, IServiceContext)}：领料消耗预留
 *       （consumedQuantity+= / reservedQuantity-= / 库存余额.预留量-=），超出部分按 min 语义封顶。</li>
 * </ul>
 *
 * <p>no-op 语义（MINOR-4）：release/consume 按 {@code sourceBillType}+{@code sourceBillCode} 查无预留记录时
 * <b>静默跳过返回 null（零异常零写入）</b>——既有无预留工单（功能上线前数据/config 关闭）零回归依赖此语义。
 *
 * <p>权威：{@code docs/design/inventory/cross-domain.md}、{@code docs/design/manufacturing/material-reservation.md}
 * （D1-D3 裁决：状态语义映射 / 超预留警告放行 / config 默认值）。
 */
public interface IErpInvReservationBiz extends ICrudBiz<ErpInvReservation>{

    /**
     * 创建预留（UC-MFG-05 ①②③）。同 (sourceBillType, sourceBillCode) 已存在未取消预留时幂等返回既有头
     * （不重复预留、不重复占用余额）。行预留量 = min(requestedQuantity, 该行余额可用量)，行维度
     * 库存余额 reservedQuantity 增加（经 {@code StockMoveBookkeeper.updateBalanceWithRetry} 乐观锁）。
     *
     * @return 预留头（含已落库行；行 reservedQuantity 为实际预留量）
     */
    @BizMutation
    ErpInvReservation createReservation(@Name("request") ReservationCreateRequest request,
                                        IServiceContext context);

    /**
     * 释放预留（UC-MFG-08 ⑤⑥⑦）。按 (sourceBillType, sourceBillCode) 定位预留头，未消耗部分
     * （reservedQuantity − consumedQuantity）全释放：行 reservedQuantity 归位至 consumedQuantity +
     * 库存余额 reservedQuantity 同步减少（乐观锁）。头 status 按释放语义（reason）映射 D2：
     * {@code CANCELLED} → CANCELLED（取消全释放）；{@code COMPLETED} → 剩余>0 则 PARTIALLY_CONSUMED，
     * 已全领则 CONSUMED。
     *
     * @param reason 释放语义：{@code "CANCELLED"}（工单取消）/ {@code "COMPLETED"}（工单完工释放未领料）
     * @return 释放后的预留头；查无预留记录返回 null（no-op，零写入）
     */
    @BizMutation
    ErpInvReservation releaseReservation(@Name("sourceBillType") String sourceBillType,
                                         @Name("sourceBillCode") String sourceBillCode,
                                         @Name("reason") String reason,
                                         IServiceContext context);

    /**
     * 领料消耗预留（UC-MFG-06 ⑬⑭⑯）。按 (sourceBillType, sourceBillCode) 定位预留头，行维度消耗：
     * consumedQuantity += 实耗、reservedQuantity −= 实耗、库存余额 reservedQuantity −= 实耗
     * （乐观锁）。实耗 = min(请求量, 该物料预留未消耗量)——超出部分封顶，不产生负预留；
     * 超预留的警告/放行语义由调用方（mfg）按 D1 裁决（config over-pick-warning）负责。
     *
     * @return 消耗后的预留头；查无预留记录返回 null（no-op，零写入）
     */
    @BizMutation
    ErpInvReservation consumeReservation(@Name("request") ReservationConsumeRequest request,
                                         IServiceContext context);
}
