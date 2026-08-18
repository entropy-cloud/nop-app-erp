package app.erp.cs.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.cs.dao.entity.ErpCsTicketFulfillmentStep;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  工单履行步骤: erp_cs_ticket_fulfillment_step
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCsTicketFulfillmentStep extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 关联工单: TICKET_ID BIGINT */
    public static final String PROP_NAME_ticketId = "ticketId";
    public static final int PROP_ID_ticketId = 3;
    
    /* 履行映射: FULFILLMENT_ID BIGINT */
    public static final String PROP_NAME_fulfillmentId = "fulfillmentId";
    public static final int PROP_ID_fulfillmentId = 4;
    
    /* 目录项: CATALOG_ITEM_ID BIGINT */
    public static final String PROP_NAME_catalogItemId = "catalogItemId";
    public static final int PROP_ID_catalogItemId = 5;
    
    /* 执行顺序: SEQUENCE INTEGER */
    public static final String PROP_NAME_sequence = "sequence";
    public static final int PROP_ID_sequence = 6;
    
    /* 动作类型: ACTION_TYPE VARCHAR */
    public static final String PROP_NAME_actionType = "actionType";
    public static final int PROP_ID_actionType = 7;
    
    /* 动作配置快照: ACTION_CONFIG VARCHAR */
    public static final String PROP_NAME_actionConfig = "actionConfig";
    public static final int PROP_ID_actionConfig = 8;
    
    /* 执行状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 9;
    
    /* 重试次数: RETRY_COUNT INTEGER */
    public static final String PROP_NAME_retryCount = "retryCount";
    public static final int PROP_ID_retryCount = 10;
    
    /* 最近错误: LAST_ERROR VARCHAR */
    public static final String PROP_NAME_lastError = "lastError";
    public static final int PROP_ID_lastError = 11;
    
    /* 执行时间: EXECUTED_AT TIMESTAMP */
    public static final String PROP_NAME_executedAt = "executedAt";
    public static final int PROP_ID_executedAt = 12;
    
    /* 执行人: EXECUTED_BY VARCHAR */
    public static final String PROP_NAME_executedBy = "executedBy";
    public static final int PROP_ID_executedBy = 13;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 14;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 15;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 16;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 17;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 18;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 19;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 20;
    

    private static int _PROP_ID_BOUND = 21;

    
    /* relation:  */
    public static final String PROP_NAME_ticket = "ticket";
    
    /* relation:  */
    public static final String PROP_NAME_fulfillment = "fulfillment";
    
    /* relation:  */
    public static final String PROP_NAME_catalogItem = "catalogItem";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_ticketId] = PROP_NAME_ticketId;
          PROP_NAME_TO_ID.put(PROP_NAME_ticketId, PROP_ID_ticketId);
      
          PROP_ID_TO_NAME[PROP_ID_fulfillmentId] = PROP_NAME_fulfillmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_fulfillmentId, PROP_ID_fulfillmentId);
      
          PROP_ID_TO_NAME[PROP_ID_catalogItemId] = PROP_NAME_catalogItemId;
          PROP_NAME_TO_ID.put(PROP_NAME_catalogItemId, PROP_ID_catalogItemId);
      
          PROP_ID_TO_NAME[PROP_ID_sequence] = PROP_NAME_sequence;
          PROP_NAME_TO_ID.put(PROP_NAME_sequence, PROP_ID_sequence);
      
          PROP_ID_TO_NAME[PROP_ID_actionType] = PROP_NAME_actionType;
          PROP_NAME_TO_ID.put(PROP_NAME_actionType, PROP_ID_actionType);
      
          PROP_ID_TO_NAME[PROP_ID_actionConfig] = PROP_NAME_actionConfig;
          PROP_NAME_TO_ID.put(PROP_NAME_actionConfig, PROP_ID_actionConfig);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_retryCount] = PROP_NAME_retryCount;
          PROP_NAME_TO_ID.put(PROP_NAME_retryCount, PROP_ID_retryCount);
      
          PROP_ID_TO_NAME[PROP_ID_lastError] = PROP_NAME_lastError;
          PROP_NAME_TO_ID.put(PROP_NAME_lastError, PROP_ID_lastError);
      
          PROP_ID_TO_NAME[PROP_ID_executedAt] = PROP_NAME_executedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_executedAt, PROP_ID_executedAt);
      
          PROP_ID_TO_NAME[PROP_ID_executedBy] = PROP_NAME_executedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_executedBy, PROP_ID_executedBy);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 关联工单: TICKET_ID */
    private java.lang.Long _ticketId;
    
    /* 履行映射: FULFILLMENT_ID */
    private java.lang.Long _fulfillmentId;
    
    /* 目录项: CATALOG_ITEM_ID */
    private java.lang.Long _catalogItemId;
    
    /* 执行顺序: SEQUENCE */
    private java.lang.Integer _sequence;
    
    /* 动作类型: ACTION_TYPE */
    private java.lang.String _actionType;
    
    /* 动作配置快照: ACTION_CONFIG */
    private java.lang.String _actionConfig;
    
    /* 执行状态: STATUS */
    private java.lang.String _status;
    
    /* 重试次数: RETRY_COUNT */
    private java.lang.Integer _retryCount;
    
    /* 最近错误: LAST_ERROR */
    private java.lang.String _lastError;
    
    /* 执行时间: EXECUTED_AT */
    private java.sql.Timestamp _executedAt;
    
    /* 执行人: EXECUTED_BY */
    private java.lang.String _executedBy;
    
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
    

    public _ErpCsTicketFulfillmentStep(){
        // for debug
    }

    protected ErpCsTicketFulfillmentStep newInstance(){
        ErpCsTicketFulfillmentStep entity = new ErpCsTicketFulfillmentStep();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCsTicketFulfillmentStep cloneInstance() {
        ErpCsTicketFulfillmentStep entity = newInstance();
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
      return "app.erp.cs.dao.entity.ErpCsTicketFulfillmentStep";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_ticketId:
               return getTicketId();
        
            case PROP_ID_fulfillmentId:
               return getFulfillmentId();
        
            case PROP_ID_catalogItemId:
               return getCatalogItemId();
        
            case PROP_ID_sequence:
               return getSequence();
        
            case PROP_ID_actionType:
               return getActionType();
        
            case PROP_ID_actionConfig:
               return getActionConfig();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_retryCount:
               return getRetryCount();
        
            case PROP_ID_lastError:
               return getLastError();
        
            case PROP_ID_executedAt:
               return getExecutedAt();
        
            case PROP_ID_executedBy:
               return getExecutedBy();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_ticketId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_ticketId));
               }
               setTicketId(typedValue);
               break;
            }
        
            case PROP_ID_fulfillmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fulfillmentId));
               }
               setFulfillmentId(typedValue);
               break;
            }
        
            case PROP_ID_catalogItemId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_catalogItemId));
               }
               setCatalogItemId(typedValue);
               break;
            }
        
            case PROP_ID_sequence:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sequence));
               }
               setSequence(typedValue);
               break;
            }
        
            case PROP_ID_actionType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_actionType));
               }
               setActionType(typedValue);
               break;
            }
        
            case PROP_ID_actionConfig:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_actionConfig));
               }
               setActionConfig(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_retryCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_retryCount));
               }
               setRetryCount(typedValue);
               break;
            }
        
            case PROP_ID_lastError:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lastError));
               }
               setLastError(typedValue);
               break;
            }
        
            case PROP_ID_executedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_executedAt));
               }
               setExecutedAt(typedValue);
               break;
            }
        
            case PROP_ID_executedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_executedBy));
               }
               setExecutedBy(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ticketId:{
               onInitProp(propId);
               this._ticketId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fulfillmentId:{
               onInitProp(propId);
               this._fulfillmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_catalogItemId:{
               onInitProp(propId);
               this._catalogItemId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sequence:{
               onInitProp(propId);
               this._sequence = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_actionType:{
               onInitProp(propId);
               this._actionType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_actionConfig:{
               onInitProp(propId);
               this._actionConfig = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_retryCount:{
               onInitProp(propId);
               this._retryCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_lastError:{
               onInitProp(propId);
               this._lastError = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_executedAt:{
               onInitProp(propId);
               this._executedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_executedBy:{
               onInitProp(propId);
               this._executedBy = (java.lang.String)value;
               
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
     * 业务组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 业务组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 关联工单: TICKET_ID
     */
    public final java.lang.Long getTicketId(){
         onPropGet(PROP_ID_ticketId);
         return _ticketId;
    }

    /**
     * 关联工单: TICKET_ID
     */
    public final void setTicketId(java.lang.Long value){
        if(onPropSet(PROP_ID_ticketId,value)){
            this._ticketId = value;
            internalClearRefs(PROP_ID_ticketId);
            
        }
    }
    
    /**
     * 履行映射: FULFILLMENT_ID
     */
    public final java.lang.Long getFulfillmentId(){
         onPropGet(PROP_ID_fulfillmentId);
         return _fulfillmentId;
    }

    /**
     * 履行映射: FULFILLMENT_ID
     */
    public final void setFulfillmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_fulfillmentId,value)){
            this._fulfillmentId = value;
            internalClearRefs(PROP_ID_fulfillmentId);
            
        }
    }
    
    /**
     * 目录项: CATALOG_ITEM_ID
     */
    public final java.lang.Long getCatalogItemId(){
         onPropGet(PROP_ID_catalogItemId);
         return _catalogItemId;
    }

    /**
     * 目录项: CATALOG_ITEM_ID
     */
    public final void setCatalogItemId(java.lang.Long value){
        if(onPropSet(PROP_ID_catalogItemId,value)){
            this._catalogItemId = value;
            internalClearRefs(PROP_ID_catalogItemId);
            
        }
    }
    
    /**
     * 执行顺序: SEQUENCE
     */
    public final java.lang.Integer getSequence(){
         onPropGet(PROP_ID_sequence);
         return _sequence;
    }

    /**
     * 执行顺序: SEQUENCE
     */
    public final void setSequence(java.lang.Integer value){
        if(onPropSet(PROP_ID_sequence,value)){
            this._sequence = value;
            internalClearRefs(PROP_ID_sequence);
            
        }
    }
    
    /**
     * 动作类型: ACTION_TYPE
     */
    public final java.lang.String getActionType(){
         onPropGet(PROP_ID_actionType);
         return _actionType;
    }

    /**
     * 动作类型: ACTION_TYPE
     */
    public final void setActionType(java.lang.String value){
        if(onPropSet(PROP_ID_actionType,value)){
            this._actionType = value;
            internalClearRefs(PROP_ID_actionType);
            
        }
    }
    
    /**
     * 动作配置快照: ACTION_CONFIG
     */
    public final java.lang.String getActionConfig(){
         onPropGet(PROP_ID_actionConfig);
         return _actionConfig;
    }

    /**
     * 动作配置快照: ACTION_CONFIG
     */
    public final void setActionConfig(java.lang.String value){
        if(onPropSet(PROP_ID_actionConfig,value)){
            this._actionConfig = value;
            internalClearRefs(PROP_ID_actionConfig);
            
        }
    }
    
    /**
     * 执行状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 执行状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 重试次数: RETRY_COUNT
     */
    public final java.lang.Integer getRetryCount(){
         onPropGet(PROP_ID_retryCount);
         return _retryCount;
    }

    /**
     * 重试次数: RETRY_COUNT
     */
    public final void setRetryCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_retryCount,value)){
            this._retryCount = value;
            internalClearRefs(PROP_ID_retryCount);
            
        }
    }
    
    /**
     * 最近错误: LAST_ERROR
     */
    public final java.lang.String getLastError(){
         onPropGet(PROP_ID_lastError);
         return _lastError;
    }

    /**
     * 最近错误: LAST_ERROR
     */
    public final void setLastError(java.lang.String value){
        if(onPropSet(PROP_ID_lastError,value)){
            this._lastError = value;
            internalClearRefs(PROP_ID_lastError);
            
        }
    }
    
    /**
     * 执行时间: EXECUTED_AT
     */
    public final java.sql.Timestamp getExecutedAt(){
         onPropGet(PROP_ID_executedAt);
         return _executedAt;
    }

    /**
     * 执行时间: EXECUTED_AT
     */
    public final void setExecutedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_executedAt,value)){
            this._executedAt = value;
            internalClearRefs(PROP_ID_executedAt);
            
        }
    }
    
    /**
     * 执行人: EXECUTED_BY
     */
    public final java.lang.String getExecutedBy(){
         onPropGet(PROP_ID_executedBy);
         return _executedBy;
    }

    /**
     * 执行人: EXECUTED_BY
     */
    public final void setExecutedBy(java.lang.String value){
        if(onPropSet(PROP_ID_executedBy,value)){
            this._executedBy = value;
            internalClearRefs(PROP_ID_executedBy);
            
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
    public final app.erp.cs.dao.entity.ErpCsTicket getTicket(){
       return (app.erp.cs.dao.entity.ErpCsTicket)internalGetRefEntity(PROP_NAME_ticket);
    }

    public final void setTicket(app.erp.cs.dao.entity.ErpCsTicket refEntity){
   
           if(refEntity == null){
           
                   this.setTicketId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_ticket, refEntity,()->{
           
                           this.setTicketId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.cs.dao.entity.ErpCsCatalogFulfillment getFulfillment(){
       return (app.erp.cs.dao.entity.ErpCsCatalogFulfillment)internalGetRefEntity(PROP_NAME_fulfillment);
    }

    public final void setFulfillment(app.erp.cs.dao.entity.ErpCsCatalogFulfillment refEntity){
   
           if(refEntity == null){
           
                   this.setFulfillmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_fulfillment, refEntity,()->{
           
                           this.setFulfillmentId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.cs.dao.entity.ErpCsServiceCatalogItem getCatalogItem(){
       return (app.erp.cs.dao.entity.ErpCsServiceCatalogItem)internalGetRefEntity(PROP_NAME_catalogItem);
    }

    public final void setCatalogItem(app.erp.cs.dao.entity.ErpCsServiceCatalogItem refEntity){
   
           if(refEntity == null){
           
                   this.setCatalogItemId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_catalogItem, refEntity,()->{
           
                           this.setCatalogItemId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdOrganization getOrg(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_org);
    }

    public final void setOrg(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setOrgId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_org, refEntity,()->{
           
                           this.setOrgId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
