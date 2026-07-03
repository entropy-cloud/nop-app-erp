
package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaRecallTarget;
import io.nop.orm.biz.ICrudBiz;

/**
 * 召回目标业务接口。标准 CRUD；目标由 {@link IErpQaRecallBiz#locateTargets} 自动生成，
 * 通知/退货状态由召回事件编排驱动（参见 {@code docs/design/quality/recall.md}）。
 */
public interface IErpQaRecallTargetBiz extends ICrudBiz<ErpQaRecallTarget> {
}
