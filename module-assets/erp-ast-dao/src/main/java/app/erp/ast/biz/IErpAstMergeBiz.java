
package app.erp.ast.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.ast.dao.entity.ErpAstMerge;

/**
 * 资产合并业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 */
public interface IErpAstMergeBiz extends ICrudBiz<ErpAstMerge>, IApprovableBiz<ErpAstMerge>{

}
