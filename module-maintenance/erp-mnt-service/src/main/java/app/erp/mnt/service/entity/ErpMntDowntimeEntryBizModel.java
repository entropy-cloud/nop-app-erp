package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntDowntimeEntryBiz;
import app.erp.mnt.biz.MntOpenDowntimeWindow;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.service.processor.ErpMntDowntimeEntryCompleteProcessor;
import app.erp.mnt.service.processor.ErpMntDowntimeEntryRecordProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.isNull;

@BizModel("ErpMntDowntimeEntry")
public class ErpMntDowntimeEntryBizModel extends CrudBizModel<ErpMntDowntimeEntry> implements IErpMntDowntimeEntryBiz {

    @Inject
    ErpMntDowntimeEntryRecordProcessor recordProcessor;
    @Inject
    ErpMntDowntimeEntryCompleteProcessor completeProcessor;

    public ErpMntDowntimeEntryBizModel() {
        setEntityName(ErpMntDowntimeEntry.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntDowntimeEntry record(@Name("downtimeId") Long downtimeId, IServiceContext context) {
        return recordProcessor.record(downtimeId, context);
    }

    @Override
    @BizMutation
    public ErpMntDowntimeEntry complete(@Name("downtimeId") Long downtimeId, IServiceContext context) {
        return completeProcessor.complete(downtimeId, context);
    }

    /**
     * RC-R1.76 / UC-MAIN-06 开放停机窗口查询（mfg 排产消费，拉取模型）：endTime null 的停机记录
     * 逐条经 ORM to-one {@code equipment} 桥接（会话内批量装载，开放集小），过滤设备 status=DOWN
     * 且 workcenterId 已映射。daoFor 直读说明（E3）：域内实体的只读投影查询，endTime isNull 不在
     * XMeta 可查询运算符集（eq/in/dateBetween），跨设备状态过滤在 Java 侧完成（开放集 = 当前
     * 未结束停机，规模天然受限）。
     */
    @Override
    @BizQuery
    public List<MntOpenDowntimeWindow> findOpenDowntimeEquipmentWorkcenters(IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(isNull("endTime"));
        List<ErpMntDowntimeEntry> openEntries = daoFor(ErpMntDowntimeEntry.class).findAllByQuery(q);
        List<MntOpenDowntimeWindow> windows = new ArrayList<>();
        for (ErpMntDowntimeEntry entry : openEntries) {
            ErpMntEquipment equipment = entry.getEquipment();
            if (equipment == null
                    || !ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN.equals(equipment.getStatus())
                    || equipment.getWorkcenterId() == null) {
                continue;
            }
            MntOpenDowntimeWindow window = new MntOpenDowntimeWindow();
            window.setEquipmentId(equipment.getId());
            window.setEquipmentCode(equipment.getCode());
            window.setWorkcenterId(equipment.getWorkcenterId());
            window.setStartTime(entry.getStartTime());
            window.setReason(entry.getReason());
            windows.add(window);
        }
        return windows;
    }
}
