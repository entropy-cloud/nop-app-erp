
package app.erp.ast.biz;

import io.nop.orm.biz.ICrudBiz;

import app.erp.ast.dao.entity.ErpAstMovement;

/**
 * 资产移动业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 */
public interface IErpAstMovementBiz extends ICrudBiz<ErpAstMovement>{

}
