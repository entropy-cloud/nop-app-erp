package app.erp.inv.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpInvDaoConstants {
    
    /**
     * 库存移动状态: 草稿 
     */
    String MOVE_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 库存移动状态: 已确认 
     */
    String MOVE_STATUS_CONFIRMED = "CONFIRMED";
                    
    /**
     * 库存移动状态: 已完成 
     */
    String MOVE_STATUS_DONE = "DONE";
                    
    /**
     * 库存移动状态: 已取消 
     */
    String MOVE_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 库存作业类型: 入库 
     */
    String OPERATION_TYPE_INCOMING = "INCOMING";
                    
    /**
     * 库存作业类型: 出库 
     */
    String OPERATION_TYPE_OUTGOING = "OUTGOING";
                    
    /**
     * 库存作业类型: 内部调拨 
     */
    String OPERATION_TYPE_INTERNAL = "INTERNAL";
                    
    /**
     * 库存作业类型: 制造 
     */
    String OPERATION_TYPE_MANUFACTURE = "MANUFACTURE";
                    
    /**
     * 库存移动方向: 入库 
     */
    String MOVE_DIRECTION_INBOUND = "INBOUND";
                    
    /**
     * 库存移动方向: 出库 
     */
    String MOVE_DIRECTION_OUTBOUND = "OUTBOUND";
                    
    /**
     * 审核状态: 未提交 
     */
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
                    
    /**
     * 审核状态: 已提交 
     */
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 审核状态: 已审核 
     */
    String APPROVE_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 审核状态: 已驳回 
     */
    String APPROVE_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 批次状态: 在用 
     */
    String BATCH_STATUS_OPEN = "OPEN";
                    
    /**
     * 批次状态: 锁定 
     */
    String BATCH_STATUS_LOCKED = "LOCKED";
                    
    /**
     * 批次状态: 已过期 
     */
    String BATCH_STATUS_EXPIRED = "EXPIRED";
                    
    /**
     * 批次状态: 已耗尽 
     */
    String BATCH_STATUS_CONSUMED = "CONSUMED";
                    
    /**
     * 批次状态: 质量冻结 
     */
    String BATCH_STATUS_BLOCKED = "BLOCKED";
                    
    /**
     * 序列号状态: 在库 
     */
    String SERIAL_STATUS_IN_STOCK = "IN_STOCK";
                    
    /**
     * 序列号状态: 已出库 
     */
    String SERIAL_STATUS_OUT = "OUT";
                    
    /**
     * 序列号状态: 已预留 
     */
    String SERIAL_STATUS_RESERVED = "RESERVED";
                    
    /**
     * 序列号状态: 冻结 
     */
    String SERIAL_STATUS_BLOCKED = "BLOCKED";
                    
    /**
     * 预留状态: 生效中 
     */
    String RESERVATION_STATUS_OPEN = "OPEN";
                    
    /**
     * 预留状态: 部分消耗 
     */
    String RESERVATION_STATUS_PARTIALLY_CONSUMED = "PARTIALLY_CONSUMED";
                    
    /**
     * 预留状态: 已全部消耗 
     */
    String RESERVATION_STATUS_CONSUMED = "CONSUMED";
                    
    /**
     * 预留状态: 已取消 
     */
    String RESERVATION_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 预留状态: 已过期 
     */
    String RESERVATION_STATUS_EXPIRED = "EXPIRED";
                    
    /**
     * 拣货状态: 待拣货 
     */
    String PICKING_STATUS_PENDING = "PENDING";
                    
    /**
     * 拣货状态: 拣货中 
     */
    String PICKING_STATUS_PICKING = "PICKING";
                    
    /**
     * 拣货状态: 已拣货 
     */
    String PICKING_STATUS_PICKED = "PICKED";
                    
    /**
     * 拣货状态: 已取消 
     */
    String PICKING_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 盘点类型: 全面盘点 
     */
    String TAKE_TYPE_FULL = "FULL";
                    
    /**
     * 盘点类型: 抽样盘点 
     */
    String TAKE_TYPE_SAMPLE = "SAMPLE";
                    
    /**
     * 盘点类型: 循环盘点 
     */
    String TAKE_TYPE_CYCLE = "CYCLE";
                    
    /**
     * 库存所有权类型: 自有库存 
     */
    String OWNERSHIP_TYPE_OWNED = "OWNED";
                    
    /**
     * 库存所有权类型: 供应商寄售 
     */
    String OWNERSHIP_TYPE_VMI_SUPPLIER = "VMI_SUPPLIER";
                    
    /**
     * 库存所有权类型: 寄售出去 
     */
    String OWNERSHIP_TYPE_CONSIGNMENT_OUT = "CONSIGNMENT_OUT";
                    
    /**
     * 库存所有权类型: 客供料 
     */
    String OWNERSHIP_TYPE_CUSTOMER_PROVIDED = "CUSTOMER_PROVIDED";
                    
    /**
     * 所有权转移类型: VMI消耗(供应商→自有) 
     */
    String OWNERSHIP_TRANSFER_TYPE_VMI_CONSUME = "VMI_CONSUME";
                    
    /**
     * 所有权转移类型: 寄售回收 
     */
    String OWNERSHIP_TRANSFER_TYPE_CONSIGNMENT_RETURN = "CONSIGNMENT_RETURN";
                    
    /**
     * 所有权转移类型: 所有权转客户 
     */
    String OWNERSHIP_TRANSFER_TYPE_OWNERSHIP_TO_CUSTOMER = "OWNERSHIP_TO_CUSTOMER";
                    
    /**
     * 所有权转移状态: 草稿 
     */
    String OWNERSHIP_TRANSFER_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 所有权转移状态: 已确认 
     */
    String OWNERSHIP_TRANSFER_STATUS_CONFIRMED = "CONFIRMED";
                    
    /**
     * 所有权转移状态: 已完成 
     */
    String OWNERSHIP_TRANSFER_STATUS_DONE = "DONE";
                    
    /**
     * 所有权转移状态: 已取消 
     */
    String OWNERSHIP_TRANSFER_STATUS_CANCELLED = "CANCELLED";
                    
}
