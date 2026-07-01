package app.erp.inv.service.posting;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;

import java.util.List;

/**
 * 存货过账派发器。移动单 DONE 后（流水/余额同事务确立终态之后）按移动类型派生业务类型，
 * 构造 {@code PostingEvent} 调用财务过账引擎；同法人内部调拨不发事件。
 *
 * <p>过账失败不阻塞移动单终态（cross-domain §与财务域协作）：以 try/catch 包裹，成功置 {@code posted=true}，
 * 失败吞异常记日志、保持 {@code posted=false}。Phase 3 接入真实引擎；Phase 1 为占位空实现。
 */
public class InvPostingDispatcher {

    /**
     * DONE 后调用。入库→PURCHASE_INPUT、出库→SALES_OUTPUT；同法人内部调拨跳过。
     */
    public void dispatchIfApplicable(ErpInvStockMove move, List<ErpInvStockMoveLine> lines) {
        // Phase 3 落地：构造 PostingEvent 调用 IErpFinPostingService.post(...)，成功置 posted=true，失败吞异常。
        if (shouldDispatch(move)) {
            // will be implemented in Phase 3
        }
    }

    private boolean shouldDispatch(ErpInvStockMove move) {
        Integer moveType = move.getMoveType();
        if (moveType == null) {
            return false;
        }
        return moveType == ErpInvConstants.MOVE_TYPE_INCOMING || moveType == ErpInvConstants.MOVE_TYPE_OUTGOING;
    }
}
