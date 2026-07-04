
package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.dao.entity.ErpHrShiftRotationPattern;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;
import java.util.List;

/**
 * 轮换排班模板 Biz（shift-scheduling.md §三）。除标准 CRUD 外，承载：
 * <ul>
 *   <li>{@link #generateRotation} 按 patternData + startDate + 组成员 + staggerDays 错峰生成排班。</li>
 * </ul>
 *
 * <p>本期按 Phase 2 Decision：组成员与错峰天数作为方法瞬态入参传入，不新增 RotationGroup 实体。
 * 「重新生成」由调用方重传 groupMemberIds/staggerDays 并清旧重生成。
 */
public interface IErpHrShiftRotationPatternBiz extends ICrudBiz<ErpHrShiftRotationPattern> {

    /**
     * 按轮换模板生成排班（shift-scheduling.md §3.3）。
     *
     * @param patternId       轮换模板 id（patternData 为班次 code 的 JSON 数组）
     * @param groupMemberIds  组成员员工 id 列表（按顺序错峰）
     * @param staggerDays     组内成员之间的错峰天数（每个成员相对首位延后该天数 × 索引）
     * @param startDate       生成范围起始日
     * @param endDate         生成范围结束日
     * @param regenerate      是否清旧重生成（删除同员工同日期范围已有 assignment 后重生成）
     */
    @BizMutation
    @SingleSession
    List<ErpHrShiftAssignment> generateRotation(@Name("patternId") Long patternId,
                                                @Name("groupMemberIds") List<Long> groupMemberIds,
                                                @Name("staggerDays") int staggerDays,
                                                @Name("startDate") LocalDate startDate,
                                                @Name("endDate") LocalDate endDate,
                                                @Name("regenerate") boolean regenerate,
                                                IServiceContext context);
}
