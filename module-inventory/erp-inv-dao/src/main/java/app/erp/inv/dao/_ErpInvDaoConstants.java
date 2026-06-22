package app.erp.inv.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpInvDaoConstants {
    
    /**
     * 库存移动状态: 草稿 
     */
    int MOVE_STATUS_DRAFT = 10;
                    
    /**
     * 库存移动状态: 已确认 
     */
    int MOVE_STATUS_CONFIRMED = 20;
                    
    /**
     * 库存移动状态: 已完成 
     */
    int MOVE_STATUS_DONE = 30;
                    
    /**
     * 库存移动状态: 已取消 
     */
    int MOVE_STATUS_CANCELLED = 40;
                    
    /**
     * 库存作业类型: 入库 
     */
    int OPERATION_TYPE_INCOMING = 10;
                    
    /**
     * 库存作业类型: 出库 
     */
    int OPERATION_TYPE_OUTGOING = 20;
                    
    /**
     * 库存作业类型: 内部调拨 
     */
    int OPERATION_TYPE_INTERNAL = 30;
                    
    /**
     * 库存作业类型: 制造 
     */
    int OPERATION_TYPE_MANUFACTURE = 40;
                    
    /**
     * 库存移动方向: 入库 
     */
    int MOVE_DIRECTION_INBOUND = 10;
                    
    /**
     * 库存移动方向: 出库 
     */
    int MOVE_DIRECTION_OUTBOUND = 20;
                    
    /**
     * 审核状态: 未提交 
     */
    int APPROVE_STATUS_UNSUBMITTED = 10;
                    
    /**
     * 审核状态: 已提交 
     */
    int APPROVE_STATUS_SUBMITTED = 20;
                    
    /**
     * 审核状态: 已审核 
     */
    int APPROVE_STATUS_APPROVED = 30;
                    
    /**
     * 审核状态: 已驳回 
     */
    int APPROVE_STATUS_REJECTED = 40;
                    
    /**
     * 批次状态: 在用 
     */
    int BATCH_STATUS_OPEN = 10;
                    
    /**
     * 批次状态: 锁定 
     */
    int BATCH_STATUS_LOCKED = 20;
                    
    /**
     * 批次状态: 已过期 
     */
    int BATCH_STATUS_EXPIRED = 30;
                    
    /**
     * 批次状态: 已耗尽 
     */
    int BATCH_STATUS_CONSUMED = 40;
                    
    /**
     * 批次状态: 质量冻结 
     */
    int BATCH_STATUS_BLOCKED = 50;
                    
    /**
     * 序列号状态: 在库 
     */
    int SERIAL_STATUS_IN_STOCK = 10;
                    
    /**
     * 序列号状态: 已出库 
     */
    int SERIAL_STATUS_OUT = 20;
                    
    /**
     * 序列号状态: 已预留 
     */
    int SERIAL_STATUS_RESERVED = 30;
                    
    /**
     * 序列号状态: 冻结 
     */
    int SERIAL_STATUS_BLOCKED = 40;
                    
    /**
     * 预留状态: 生效中 
     */
    int RESERVATION_STATUS_OPEN = 10;
                    
    /**
     * 预留状态: 部分消耗 
     */
    int RESERVATION_STATUS_PARTIALLY_CONSUMED = 20;
                    
    /**
     * 预留状态: 已全部消耗 
     */
    int RESERVATION_STATUS_CONSUMED = 30;
                    
    /**
     * 预留状态: 已取消 
     */
    int RESERVATION_STATUS_CANCELLED = 40;
                    
    /**
     * 预留状态: 已过期 
     */
    int RESERVATION_STATUS_EXPIRED = 50;
                    
    /**
     * 拣货状态: 待拣货 
     */
    int PICKING_STATUS_PENDING = 10;
                    
    /**
     * 拣货状态: 拣货中 
     */
    int PICKING_STATUS_PICKING = 20;
                    
    /**
     * 拣货状态: 已拣货 
     */
    int PICKING_STATUS_PICKED = 30;
                    
    /**
     * 拣货状态: 已取消 
     */
    int PICKING_STATUS_CANCELLED = 40;
                    
    /**
     * 盘点类型: 全面盘点 
     */
    int TAKE_TYPE_FULL = 10;
                    
    /**
     * 盘点类型: 抽样盘点 
     */
    int TAKE_TYPE_SAMPLE = 20;
                    
    /**
     * 盘点类型: 循环盘点 
     */
    int TAKE_TYPE_CYCLE = 30;
                    
}
