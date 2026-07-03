package app.erp.qa.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.qa.dao.entity.ErpQaRecallTarget;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  召回目标: erp_qa_recall_target
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaRecallTarget extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 召回事件: RECALL_ID BIGINT */
    public static final String PROP_NAME_recallId = "recallId";
    public static final int PROP_ID_recallId = 2;
    
    /* 受影响客户: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 3;
    
    /* 批号: BATCH_NO VARCHAR */
    public static final String PROP_NAME_batchNo = "batchNo";
    public static final int PROP_ID_batchNo = 4;
    
    /* 序列号: SERIAL_NO VARCHAR */
    public static final String PROP_NAME_serialNo = "serialNo";
    public static final int PROP_ID_serialNo = 5;
    
    /* 销售出库单(弱指针): SALES_DELIVERY_ID BIGINT */
    public static final String PROP_NAME_salesDeliveryId = "salesDeliveryId";
    public static final int PROP_ID_salesDeliveryId = 6;
    
    /* 发货数量: SHIPPED_QTY DECIMAL */
    public static final String PROP_NAME_shippedQty = "shippedQty";
    public static final int PROP_ID_shippedQty = 7;
    
    /* 通知时间: NOTIFIED_AT DATETIME */
    public static final String PROP_NAME_notifiedAt = "notifiedAt";
    public static final int PROP_ID_notifiedAt = 8;
    
    /* 通知人: NOTIFIED_BY VARCHAR */
    public static final String PROP_NAME_notifiedBy = "notifiedBy";
    public static final int PROP_ID_notifiedBy = 9;
    
    /* 退货状态: RETURN_STATUS INTEGER */
    public static final String PROP_NAME_returnStatus = "returnStatus";
    public static final int PROP_ID_returnStatus = 10;
    
    /* 已生成退货单(弱指针): GENERATED_RETURN_ID BIGINT */
    public static final String PROP_NAME_generatedReturnId = "generatedReturnId";
    public static final int PROP_ID_generatedReturnId = 11;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 12;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 13;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 14;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 15;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 16;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 17;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 18;
    

    private static int _PROP_ID_BOUND = 19;

    
    /* relation:  */
    public static final String PROP_NAME_recall = "recall";
    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_recallId] = PROP_NAME_recallId;
          PROP_NAME_TO_ID.put(PROP_NAME_recallId, PROP_ID_recallId);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_batchNo] = PROP_NAME_batchNo;
          PROP_NAME_TO_ID.put(PROP_NAME_batchNo, PROP_ID_batchNo);
      
          PROP_ID_TO_NAME[PROP_ID_serialNo] = PROP_NAME_serialNo;
          PROP_NAME_TO_ID.put(PROP_NAME_serialNo, PROP_ID_serialNo);
      
          PROP_ID_TO_NAME[PROP_ID_salesDeliveryId] = PROP_NAME_salesDeliveryId;
          PROP_NAME_TO_ID.put(PROP_NAME_salesDeliveryId, PROP_ID_salesDeliveryId);
      
          PROP_ID_TO_NAME[PROP_ID_shippedQty] = PROP_NAME_shippedQty;
          PROP_NAME_TO_ID.put(PROP_NAME_shippedQty, PROP_ID_shippedQty);
      
          PROP_ID_TO_NAME[PROP_ID_notifiedAt] = PROP_NAME_notifiedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_notifiedAt, PROP_ID_notifiedAt);
      
          PROP_ID_TO_NAME[PROP_ID_notifiedBy] = PROP_NAME_notifiedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_notifiedBy, PROP_ID_notifiedBy);
      
          PROP_ID_TO_NAME[PROP_ID_returnStatus] = PROP_NAME_returnStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_returnStatus, PROP_ID_returnStatus);
      
          PROP_ID_TO_NAME[PROP_ID_generatedReturnId] = PROP_NAME_generatedReturnId;
          PROP_NAME_TO_ID.put(PROP_NAME_generatedReturnId, PROP_ID_generatedReturnId);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
          PROP_ID_TO_NAME[PROP_ID_delVersion] = PROP_NAME_delVersion;
          PROP_NAME_TO_ID.put(PROP_NAME_delVersion, PROP_ID_delVersion);
      
          PROP_ID_TO_NAME[PROP_ID_version] = PROP_NAME_version;
          PROP_NAME_TO_ID.put(PROP_NAME_version, PROP_ID_version);
      
          PROP_ID_TO_NAME[PROP_ID_createdBy] = PROP_NAME_createdBy;
          PROP_NAME_TO_ID.put(PROP_NAME_createdBy, PROP_ID_createdBy);
      
          PROP_ID_TO_NAME[PROP_ID_createTime] = PROP_NAME_createTime;
          PROP_NAME_TO_ID.put(PROP_NAME_createTime, PROP_ID_createTime);
      
          PROP_ID_TO_NAME[PROP_ID_updatedBy] = PROP_NAME_updatedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_updatedBy, PROP_ID_updatedBy);
      
          PROP_ID_TO_NAME[PROP_ID_updateTime] = PROP_NAME_updateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_updateTime, PROP_ID_updateTime);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 召回事件: RECALL_ID */
    private java.lang.Long _recallId;
    
    /* 受影响客户: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 批号: BATCH_NO */
    private java.lang.String _batchNo;
    
    /* 序列号: SERIAL_NO */
    private java.lang.String _serialNo;
    
    /* 销售出库单(弱指针): SALES_DELIVERY_ID */
    private java.lang.Long _salesDeliveryId;
    
    /* 发货数量: SHIPPED_QTY */
    private java.math.BigDecimal _shippedQty;
    
    /* 通知时间: NOTIFIED_AT */
    private java.time.LocalDateTime _notifiedAt;
    
    /* 通知人: NOTIFIED_BY */
    private java.lang.String _notifiedBy;
    
    /* 退货状态: RETURN_STATUS */
    private java.lang.Integer _returnStatus;
    
    /* 已生成退货单(弱指针): GENERATED_RETURN_ID */
    private java.lang.Long _generatedReturnId;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
    /* 逻辑删除版本: DEL_VERSION */
    private java.lang.Long _delVersion;
    
    /* 数据版本: VERSION */
    private java.lang.Integer _version;
    
    /* 创建人: CREATED_BY */
    private java.lang.String _createdBy;
    
    /* 创建时间: CREATE_TIME */
    private java.sql.Timestamp _createTime;
    
    /* 修改人: UPDATED_BY */
    private java.lang.String _updatedBy;
    
    /* 修改时间: UPDATE_TIME */
    private java.sql.Timestamp _updateTime;
    

    public _ErpQaRecallTarget(){
        // for debug
    }

    protected ErpQaRecallTarget newInstance(){
        ErpQaRecallTarget entity = new ErpQaRecallTarget();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaRecallTarget cloneInstance() {
        ErpQaRecallTarget entity = newInstance();
        orm_forEachInitedProp((value, propId) -> {
            entity.orm_propValue(propId,value);
        });
        return entity;
    }

    @Override
    public String orm_entityName() {
      // 如果存在实体模型对象，则以模型对象上的设置为准
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getName();
      return "app.erp.qa.dao.entity.ErpQaRecallTarget";
    }

    @Override
    public int orm_propIdBound(){
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getPropIdBound();
      return _PROP_ID_BOUND;
    }

    @Override
    public Object orm_id() {
    
        return buildSimpleId(PROP_ID_id);
     
    }

    @Override
    public boolean orm_isPrimary(int propId) {
        
            return propId == PROP_ID_id;
          
    }

    @Override
    public String orm_propName(int propId) {
        if(propId >= PROP_ID_TO_NAME.length)
            return super.orm_propName(propId);
        String propName = PROP_ID_TO_NAME[propId];
        if(propName == null)
           return super.orm_propName(propId);
        return propName;
    }

    @Override
    public int orm_propId(String propName) {
        Integer propId = PROP_NAME_TO_ID.get(propName);
        if(propId == null)
            return super.orm_propId(propName);
        return propId;
    }

    @Override
    public Object orm_propValue(int propId) {
        switch(propId){
        
            case PROP_ID_id:
               return getId();
        
            case PROP_ID_recallId:
               return getRecallId();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_batchNo:
               return getBatchNo();
        
            case PROP_ID_serialNo:
               return getSerialNo();
        
            case PROP_ID_salesDeliveryId:
               return getSalesDeliveryId();
        
            case PROP_ID_shippedQty:
               return getShippedQty();
        
            case PROP_ID_notifiedAt:
               return getNotifiedAt();
        
            case PROP_ID_notifiedBy:
               return getNotifiedBy();
        
            case PROP_ID_returnStatus:
               return getReturnStatus();
        
            case PROP_ID_generatedReturnId:
               return getGeneratedReturnId();
        
            case PROP_ID_remark:
               return getRemark();
        
            case PROP_ID_delVersion:
               return getDelVersion();
        
            case PROP_ID_version:
               return getVersion();
        
            case PROP_ID_createdBy:
               return getCreatedBy();
        
            case PROP_ID_createTime:
               return getCreateTime();
        
            case PROP_ID_updatedBy:
               return getUpdatedBy();
        
            case PROP_ID_updateTime:
               return getUpdateTime();
        
           default:
              return super.orm_propValue(propId);
        }
    }

    

    @Override
    public void orm_propValue(int propId, Object value){
        switch(propId){
        
            case PROP_ID_id:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_id));
               }
               setId(typedValue);
               break;
            }
        
            case PROP_ID_recallId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_recallId));
               }
               setRecallId(typedValue);
               break;
            }
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_batchNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_batchNo));
               }
               setBatchNo(typedValue);
               break;
            }
        
            case PROP_ID_serialNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_serialNo));
               }
               setSerialNo(typedValue);
               break;
            }
        
            case PROP_ID_salesDeliveryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_salesDeliveryId));
               }
               setSalesDeliveryId(typedValue);
               break;
            }
        
            case PROP_ID_shippedQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_shippedQty));
               }
               setShippedQty(typedValue);
               break;
            }
        
            case PROP_ID_notifiedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_notifiedAt));
               }
               setNotifiedAt(typedValue);
               break;
            }
        
            case PROP_ID_notifiedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notifiedBy));
               }
               setNotifiedBy(typedValue);
               break;
            }
        
            case PROP_ID_returnStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_returnStatus));
               }
               setReturnStatus(typedValue);
               break;
            }
        
            case PROP_ID_generatedReturnId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_generatedReturnId));
               }
               setGeneratedReturnId(typedValue);
               break;
            }
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
               break;
            }
        
            case PROP_ID_delVersion:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_delVersion));
               }
               setDelVersion(typedValue);
               break;
            }
        
            case PROP_ID_version:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_version));
               }
               setVersion(typedValue);
               break;
            }
        
            case PROP_ID_createdBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_createdBy));
               }
               setCreatedBy(typedValue);
               break;
            }
        
            case PROP_ID_createTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_createTime));
               }
               setCreateTime(typedValue);
               break;
            }
        
            case PROP_ID_updatedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_updatedBy));
               }
               setUpdatedBy(typedValue);
               break;
            }
        
            case PROP_ID_updateTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_updateTime));
               }
               setUpdateTime(typedValue);
               break;
            }
        
           default:
              super.orm_propValue(propId,value);
        }
    }

    @Override
    public void orm_internalSet(int propId, Object value) {
        switch(propId){
        
            case PROP_ID_id:{
               onInitProp(propId);
               this._id = (java.lang.Long)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_recallId:{
               onInitProp(propId);
               this._recallId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_batchNo:{
               onInitProp(propId);
               this._batchNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_serialNo:{
               onInitProp(propId);
               this._serialNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_salesDeliveryId:{
               onInitProp(propId);
               this._salesDeliveryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_shippedQty:{
               onInitProp(propId);
               this._shippedQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_notifiedAt:{
               onInitProp(propId);
               this._notifiedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_notifiedBy:{
               onInitProp(propId);
               this._notifiedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_returnStatus:{
               onInitProp(propId);
               this._returnStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_generatedReturnId:{
               onInitProp(propId);
               this._generatedReturnId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_delVersion:{
               onInitProp(propId);
               this._delVersion = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_version:{
               onInitProp(propId);
               this._version = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_createdBy:{
               onInitProp(propId);
               this._createdBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_createTime:{
               onInitProp(propId);
               this._createTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_updatedBy:{
               onInitProp(propId);
               this._updatedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_updateTime:{
               onInitProp(propId);
               this._updateTime = (java.sql.Timestamp)value;
               
               break;
            }
        
           default:
              super.orm_internalSet(propId,value);
        }
    }

    
    /**
     * ID: ID
     */
    public final java.lang.Long getId(){
         onPropGet(PROP_ID_id);
         return _id;
    }

    /**
     * ID: ID
     */
    public final void setId(java.lang.Long value){
        if(onPropSet(PROP_ID_id,value)){
            this._id = value;
            internalClearRefs(PROP_ID_id);
            orm_id();
        }
    }
    
    /**
     * 召回事件: RECALL_ID
     */
    public final java.lang.Long getRecallId(){
         onPropGet(PROP_ID_recallId);
         return _recallId;
    }

    /**
     * 召回事件: RECALL_ID
     */
    public final void setRecallId(java.lang.Long value){
        if(onPropSet(PROP_ID_recallId,value)){
            this._recallId = value;
            internalClearRefs(PROP_ID_recallId);
            
        }
    }
    
    /**
     * 受影响客户: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 受影响客户: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 批号: BATCH_NO
     */
    public final java.lang.String getBatchNo(){
         onPropGet(PROP_ID_batchNo);
         return _batchNo;
    }

    /**
     * 批号: BATCH_NO
     */
    public final void setBatchNo(java.lang.String value){
        if(onPropSet(PROP_ID_batchNo,value)){
            this._batchNo = value;
            internalClearRefs(PROP_ID_batchNo);
            
        }
    }
    
    /**
     * 序列号: SERIAL_NO
     */
    public final java.lang.String getSerialNo(){
         onPropGet(PROP_ID_serialNo);
         return _serialNo;
    }

    /**
     * 序列号: SERIAL_NO
     */
    public final void setSerialNo(java.lang.String value){
        if(onPropSet(PROP_ID_serialNo,value)){
            this._serialNo = value;
            internalClearRefs(PROP_ID_serialNo);
            
        }
    }
    
    /**
     * 销售出库单(弱指针): SALES_DELIVERY_ID
     */
    public final java.lang.Long getSalesDeliveryId(){
         onPropGet(PROP_ID_salesDeliveryId);
         return _salesDeliveryId;
    }

    /**
     * 销售出库单(弱指针): SALES_DELIVERY_ID
     */
    public final void setSalesDeliveryId(java.lang.Long value){
        if(onPropSet(PROP_ID_salesDeliveryId,value)){
            this._salesDeliveryId = value;
            internalClearRefs(PROP_ID_salesDeliveryId);
            
        }
    }
    
    /**
     * 发货数量: SHIPPED_QTY
     */
    public final java.math.BigDecimal getShippedQty(){
         onPropGet(PROP_ID_shippedQty);
         return _shippedQty;
    }

    /**
     * 发货数量: SHIPPED_QTY
     */
    public final void setShippedQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_shippedQty,value)){
            this._shippedQty = value;
            internalClearRefs(PROP_ID_shippedQty);
            
        }
    }
    
    /**
     * 通知时间: NOTIFIED_AT
     */
    public final java.time.LocalDateTime getNotifiedAt(){
         onPropGet(PROP_ID_notifiedAt);
         return _notifiedAt;
    }

    /**
     * 通知时间: NOTIFIED_AT
     */
    public final void setNotifiedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_notifiedAt,value)){
            this._notifiedAt = value;
            internalClearRefs(PROP_ID_notifiedAt);
            
        }
    }
    
    /**
     * 通知人: NOTIFIED_BY
     */
    public final java.lang.String getNotifiedBy(){
         onPropGet(PROP_ID_notifiedBy);
         return _notifiedBy;
    }

    /**
     * 通知人: NOTIFIED_BY
     */
    public final void setNotifiedBy(java.lang.String value){
        if(onPropSet(PROP_ID_notifiedBy,value)){
            this._notifiedBy = value;
            internalClearRefs(PROP_ID_notifiedBy);
            
        }
    }
    
    /**
     * 退货状态: RETURN_STATUS
     */
    public final java.lang.Integer getReturnStatus(){
         onPropGet(PROP_ID_returnStatus);
         return _returnStatus;
    }

    /**
     * 退货状态: RETURN_STATUS
     */
    public final void setReturnStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_returnStatus,value)){
            this._returnStatus = value;
            internalClearRefs(PROP_ID_returnStatus);
            
        }
    }
    
    /**
     * 已生成退货单(弱指针): GENERATED_RETURN_ID
     */
    public final java.lang.Long getGeneratedReturnId(){
         onPropGet(PROP_ID_generatedReturnId);
         return _generatedReturnId;
    }

    /**
     * 已生成退货单(弱指针): GENERATED_RETURN_ID
     */
    public final void setGeneratedReturnId(java.lang.Long value){
        if(onPropSet(PROP_ID_generatedReturnId,value)){
            this._generatedReturnId = value;
            internalClearRefs(PROP_ID_generatedReturnId);
            
        }
    }
    
    /**
     * 备注: REMARK
     */
    public final java.lang.String getRemark(){
         onPropGet(PROP_ID_remark);
         return _remark;
    }

    /**
     * 备注: REMARK
     */
    public final void setRemark(java.lang.String value){
        if(onPropSet(PROP_ID_remark,value)){
            this._remark = value;
            internalClearRefs(PROP_ID_remark);
            
        }
    }
    
    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final java.lang.Long getDelVersion(){
         onPropGet(PROP_ID_delVersion);
         return _delVersion;
    }

    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final void setDelVersion(java.lang.Long value){
        if(onPropSet(PROP_ID_delVersion,value)){
            this._delVersion = value;
            internalClearRefs(PROP_ID_delVersion);
            
        }
    }
    
    /**
     * 数据版本: VERSION
     */
    public final java.lang.Integer getVersion(){
         onPropGet(PROP_ID_version);
         return _version;
    }

    /**
     * 数据版本: VERSION
     */
    public final void setVersion(java.lang.Integer value){
        if(onPropSet(PROP_ID_version,value)){
            this._version = value;
            internalClearRefs(PROP_ID_version);
            
        }
    }
    
    /**
     * 创建人: CREATED_BY
     */
    public final java.lang.String getCreatedBy(){
         onPropGet(PROP_ID_createdBy);
         return _createdBy;
    }

    /**
     * 创建人: CREATED_BY
     */
    public final void setCreatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_createdBy,value)){
            this._createdBy = value;
            internalClearRefs(PROP_ID_createdBy);
            
        }
    }
    
    /**
     * 创建时间: CREATE_TIME
     */
    public final java.sql.Timestamp getCreateTime(){
         onPropGet(PROP_ID_createTime);
         return _createTime;
    }

    /**
     * 创建时间: CREATE_TIME
     */
    public final void setCreateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_createTime,value)){
            this._createTime = value;
            internalClearRefs(PROP_ID_createTime);
            
        }
    }
    
    /**
     * 修改人: UPDATED_BY
     */
    public final java.lang.String getUpdatedBy(){
         onPropGet(PROP_ID_updatedBy);
         return _updatedBy;
    }

    /**
     * 修改人: UPDATED_BY
     */
    public final void setUpdatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_updatedBy,value)){
            this._updatedBy = value;
            internalClearRefs(PROP_ID_updatedBy);
            
        }
    }
    
    /**
     * 修改时间: UPDATE_TIME
     */
    public final java.sql.Timestamp getUpdateTime(){
         onPropGet(PROP_ID_updateTime);
         return _updateTime;
    }

    /**
     * 修改时间: UPDATE_TIME
     */
    public final void setUpdateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_updateTime,value)){
            this._updateTime = value;
            internalClearRefs(PROP_ID_updateTime);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.qa.dao.entity.ErpQaRecall getRecall(){
       return (app.erp.qa.dao.entity.ErpQaRecall)internalGetRefEntity(PROP_NAME_recall);
    }

    public final void setRecall(app.erp.qa.dao.entity.ErpQaRecall refEntity){
   
           if(refEntity == null){
           
                   this.setRecallId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_recall, refEntity,()->{
           
                           this.setRecallId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getPartner(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_partner);
    }

    public final void setPartner(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_partner, refEntity,()->{
           
                           this.setPartnerId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
