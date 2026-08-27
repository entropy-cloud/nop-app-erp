package app.erp.ast.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.ast.dao.entity.ErpAstAssetActionLog;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  资产操作审计: erp_ast_asset_action_log
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpAstAssetActionLog extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 资产: ASSET_ID BIGINT */
    public static final String PROP_NAME_assetId = "assetId";
    public static final int PROP_ID_assetId = 2;
    
    /* 事件类型: EVENT_TYPE VARCHAR */
    public static final String PROP_NAME_eventType = "eventType";
    public static final int PROP_ID_eventType = 3;
    
    /* 变更前资产状态: FROM_STATUS VARCHAR */
    public static final String PROP_NAME_fromStatus = "fromStatus";
    public static final int PROP_ID_fromStatus = 4;
    
    /* 变更后资产状态: TO_STATUS VARCHAR */
    public static final String PROP_NAME_toStatus = "toStatus";
    public static final int PROP_ID_toStatus = 5;
    
    /* 变更前部门: FROM_DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_fromDepartmentId = "fromDepartmentId";
    public static final int PROP_ID_fromDepartmentId = 6;
    
    /* 变更后部门: TO_DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_toDepartmentId = "toDepartmentId";
    public static final int PROP_ID_toDepartmentId = 7;
    
    /* 变更前地点: FROM_LOCATION_ID BIGINT */
    public static final String PROP_NAME_fromLocationId = "fromLocationId";
    public static final int PROP_ID_fromLocationId = 8;
    
    /* 变更后地点: TO_LOCATION_ID BIGINT */
    public static final String PROP_NAME_toLocationId = "toLocationId";
    public static final int PROP_ID_toLocationId = 9;
    
    /* 变更前使用人: FROM_STAFF_ID BIGINT */
    public static final String PROP_NAME_fromStaffId = "fromStaffId";
    public static final int PROP_ID_fromStaffId = 10;
    
    /* 变更后使用人: TO_STAFF_ID BIGINT */
    public static final String PROP_NAME_toStaffId = "toStaffId";
    public static final int PROP_ID_toStaffId = 11;
    
    /* 关联单据实体: REF_ENTITY_NAME VARCHAR */
    public static final String PROP_NAME_refEntityName = "refEntityName";
    public static final int PROP_ID_refEntityName = 12;
    
    /* 关联单据ID: REF_ENTITY_ID BIGINT */
    public static final String PROP_NAME_refEntityId = "refEntityId";
    public static final int PROP_ID_refEntityId = 13;
    
    /* 摘要: SUMMARY VARCHAR */
    public static final String PROP_NAME_summary = "summary";
    public static final int PROP_ID_summary = 14;
    
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
    public static final String PROP_NAME_asset = "asset";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_assetId] = PROP_NAME_assetId;
          PROP_NAME_TO_ID.put(PROP_NAME_assetId, PROP_ID_assetId);
      
          PROP_ID_TO_NAME[PROP_ID_eventType] = PROP_NAME_eventType;
          PROP_NAME_TO_ID.put(PROP_NAME_eventType, PROP_ID_eventType);
      
          PROP_ID_TO_NAME[PROP_ID_fromStatus] = PROP_NAME_fromStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_fromStatus, PROP_ID_fromStatus);
      
          PROP_ID_TO_NAME[PROP_ID_toStatus] = PROP_NAME_toStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_toStatus, PROP_ID_toStatus);
      
          PROP_ID_TO_NAME[PROP_ID_fromDepartmentId] = PROP_NAME_fromDepartmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_fromDepartmentId, PROP_ID_fromDepartmentId);
      
          PROP_ID_TO_NAME[PROP_ID_toDepartmentId] = PROP_NAME_toDepartmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_toDepartmentId, PROP_ID_toDepartmentId);
      
          PROP_ID_TO_NAME[PROP_ID_fromLocationId] = PROP_NAME_fromLocationId;
          PROP_NAME_TO_ID.put(PROP_NAME_fromLocationId, PROP_ID_fromLocationId);
      
          PROP_ID_TO_NAME[PROP_ID_toLocationId] = PROP_NAME_toLocationId;
          PROP_NAME_TO_ID.put(PROP_NAME_toLocationId, PROP_ID_toLocationId);
      
          PROP_ID_TO_NAME[PROP_ID_fromStaffId] = PROP_NAME_fromStaffId;
          PROP_NAME_TO_ID.put(PROP_NAME_fromStaffId, PROP_ID_fromStaffId);
      
          PROP_ID_TO_NAME[PROP_ID_toStaffId] = PROP_NAME_toStaffId;
          PROP_NAME_TO_ID.put(PROP_NAME_toStaffId, PROP_ID_toStaffId);
      
          PROP_ID_TO_NAME[PROP_ID_refEntityName] = PROP_NAME_refEntityName;
          PROP_NAME_TO_ID.put(PROP_NAME_refEntityName, PROP_ID_refEntityName);
      
          PROP_ID_TO_NAME[PROP_ID_refEntityId] = PROP_NAME_refEntityId;
          PROP_NAME_TO_ID.put(PROP_NAME_refEntityId, PROP_ID_refEntityId);
      
          PROP_ID_TO_NAME[PROP_ID_summary] = PROP_NAME_summary;
          PROP_NAME_TO_ID.put(PROP_NAME_summary, PROP_ID_summary);
      
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
    private java.lang.String _id;
    
    /* 资产: ASSET_ID */
    private java.lang.String _assetId;
    
    /* 事件类型: EVENT_TYPE */
    private java.lang.String _eventType;
    
    /* 变更前资产状态: FROM_STATUS */
    private java.lang.String _fromStatus;
    
    /* 变更后资产状态: TO_STATUS */
    private java.lang.String _toStatus;
    
    /* 变更前部门: FROM_DEPARTMENT_ID */
    private java.lang.String _fromDepartmentId;
    
    /* 变更后部门: TO_DEPARTMENT_ID */
    private java.lang.String _toDepartmentId;
    
    /* 变更前地点: FROM_LOCATION_ID */
    private java.lang.String _fromLocationId;
    
    /* 变更后地点: TO_LOCATION_ID */
    private java.lang.String _toLocationId;
    
    /* 变更前使用人: FROM_STAFF_ID */
    private java.lang.String _fromStaffId;
    
    /* 变更后使用人: TO_STAFF_ID */
    private java.lang.String _toStaffId;
    
    /* 关联单据实体: REF_ENTITY_NAME */
    private java.lang.String _refEntityName;
    
    /* 关联单据ID: REF_ENTITY_ID */
    private java.lang.String _refEntityId;
    
    /* 摘要: SUMMARY */
    private java.lang.String _summary;
    
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
    

    public _ErpAstAssetActionLog(){
        // for debug
    }

    protected ErpAstAssetActionLog newInstance(){
        ErpAstAssetActionLog entity = new ErpAstAssetActionLog();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpAstAssetActionLog cloneInstance() {
        ErpAstAssetActionLog entity = newInstance();
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
      return "app.erp.ast.dao.entity.ErpAstAssetActionLog";
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
        
            case PROP_ID_assetId:
               return getAssetId();
        
            case PROP_ID_eventType:
               return getEventType();
        
            case PROP_ID_fromStatus:
               return getFromStatus();
        
            case PROP_ID_toStatus:
               return getToStatus();
        
            case PROP_ID_fromDepartmentId:
               return getFromDepartmentId();
        
            case PROP_ID_toDepartmentId:
               return getToDepartmentId();
        
            case PROP_ID_fromLocationId:
               return getFromLocationId();
        
            case PROP_ID_toLocationId:
               return getToLocationId();
        
            case PROP_ID_fromStaffId:
               return getFromStaffId();
        
            case PROP_ID_toStaffId:
               return getToStaffId();
        
            case PROP_ID_refEntityName:
               return getRefEntityName();
        
            case PROP_ID_refEntityId:
               return getRefEntityId();
        
            case PROP_ID_summary:
               return getSummary();
        
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
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_id));
               }
               setId(typedValue);
               break;
            }
        
            case PROP_ID_assetId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_assetId));
               }
               setAssetId(typedValue);
               break;
            }
        
            case PROP_ID_eventType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_eventType));
               }
               setEventType(typedValue);
               break;
            }
        
            case PROP_ID_fromStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fromStatus));
               }
               setFromStatus(typedValue);
               break;
            }
        
            case PROP_ID_toStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_toStatus));
               }
               setToStatus(typedValue);
               break;
            }
        
            case PROP_ID_fromDepartmentId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fromDepartmentId));
               }
               setFromDepartmentId(typedValue);
               break;
            }
        
            case PROP_ID_toDepartmentId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_toDepartmentId));
               }
               setToDepartmentId(typedValue);
               break;
            }
        
            case PROP_ID_fromLocationId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fromLocationId));
               }
               setFromLocationId(typedValue);
               break;
            }
        
            case PROP_ID_toLocationId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_toLocationId));
               }
               setToLocationId(typedValue);
               break;
            }
        
            case PROP_ID_fromStaffId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fromStaffId));
               }
               setFromStaffId(typedValue);
               break;
            }
        
            case PROP_ID_toStaffId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_toStaffId));
               }
               setToStaffId(typedValue);
               break;
            }
        
            case PROP_ID_refEntityName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_refEntityName));
               }
               setRefEntityName(typedValue);
               break;
            }
        
            case PROP_ID_refEntityId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_refEntityId));
               }
               setRefEntityId(typedValue);
               break;
            }
        
            case PROP_ID_summary:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_summary));
               }
               setSummary(typedValue);
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
               this._id = (java.lang.String)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_assetId:{
               onInitProp(propId);
               this._assetId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_eventType:{
               onInitProp(propId);
               this._eventType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fromStatus:{
               onInitProp(propId);
               this._fromStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_toStatus:{
               onInitProp(propId);
               this._toStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fromDepartmentId:{
               onInitProp(propId);
               this._fromDepartmentId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_toDepartmentId:{
               onInitProp(propId);
               this._toDepartmentId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fromLocationId:{
               onInitProp(propId);
               this._fromLocationId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_toLocationId:{
               onInitProp(propId);
               this._toLocationId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fromStaffId:{
               onInitProp(propId);
               this._fromStaffId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_toStaffId:{
               onInitProp(propId);
               this._toStaffId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_refEntityName:{
               onInitProp(propId);
               this._refEntityName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_refEntityId:{
               onInitProp(propId);
               this._refEntityId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_summary:{
               onInitProp(propId);
               this._summary = (java.lang.String)value;
               
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
    public final java.lang.String getId(){
         onPropGet(PROP_ID_id);
         return _id;
    }

    /**
     * ID: ID
     */
    public final void setId(java.lang.String value){
        if(onPropSet(PROP_ID_id,value)){
            this._id = value;
            internalClearRefs(PROP_ID_id);
            orm_id();
        }
    }
    
    /**
     * 资产: ASSET_ID
     */
    public final java.lang.String getAssetId(){
         onPropGet(PROP_ID_assetId);
         return _assetId;
    }

    /**
     * 资产: ASSET_ID
     */
    public final void setAssetId(java.lang.String value){
        if(onPropSet(PROP_ID_assetId,value)){
            this._assetId = value;
            internalClearRefs(PROP_ID_assetId);
            
        }
    }
    
    /**
     * 事件类型: EVENT_TYPE
     */
    public final java.lang.String getEventType(){
         onPropGet(PROP_ID_eventType);
         return _eventType;
    }

    /**
     * 事件类型: EVENT_TYPE
     */
    public final void setEventType(java.lang.String value){
        if(onPropSet(PROP_ID_eventType,value)){
            this._eventType = value;
            internalClearRefs(PROP_ID_eventType);
            
        }
    }
    
    /**
     * 变更前资产状态: FROM_STATUS
     */
    public final java.lang.String getFromStatus(){
         onPropGet(PROP_ID_fromStatus);
         return _fromStatus;
    }

    /**
     * 变更前资产状态: FROM_STATUS
     */
    public final void setFromStatus(java.lang.String value){
        if(onPropSet(PROP_ID_fromStatus,value)){
            this._fromStatus = value;
            internalClearRefs(PROP_ID_fromStatus);
            
        }
    }
    
    /**
     * 变更后资产状态: TO_STATUS
     */
    public final java.lang.String getToStatus(){
         onPropGet(PROP_ID_toStatus);
         return _toStatus;
    }

    /**
     * 变更后资产状态: TO_STATUS
     */
    public final void setToStatus(java.lang.String value){
        if(onPropSet(PROP_ID_toStatus,value)){
            this._toStatus = value;
            internalClearRefs(PROP_ID_toStatus);
            
        }
    }
    
    /**
     * 变更前部门: FROM_DEPARTMENT_ID
     */
    public final java.lang.String getFromDepartmentId(){
         onPropGet(PROP_ID_fromDepartmentId);
         return _fromDepartmentId;
    }

    /**
     * 变更前部门: FROM_DEPARTMENT_ID
     */
    public final void setFromDepartmentId(java.lang.String value){
        if(onPropSet(PROP_ID_fromDepartmentId,value)){
            this._fromDepartmentId = value;
            internalClearRefs(PROP_ID_fromDepartmentId);
            
        }
    }
    
    /**
     * 变更后部门: TO_DEPARTMENT_ID
     */
    public final java.lang.String getToDepartmentId(){
         onPropGet(PROP_ID_toDepartmentId);
         return _toDepartmentId;
    }

    /**
     * 变更后部门: TO_DEPARTMENT_ID
     */
    public final void setToDepartmentId(java.lang.String value){
        if(onPropSet(PROP_ID_toDepartmentId,value)){
            this._toDepartmentId = value;
            internalClearRefs(PROP_ID_toDepartmentId);
            
        }
    }
    
    /**
     * 变更前地点: FROM_LOCATION_ID
     */
    public final java.lang.String getFromLocationId(){
         onPropGet(PROP_ID_fromLocationId);
         return _fromLocationId;
    }

    /**
     * 变更前地点: FROM_LOCATION_ID
     */
    public final void setFromLocationId(java.lang.String value){
        if(onPropSet(PROP_ID_fromLocationId,value)){
            this._fromLocationId = value;
            internalClearRefs(PROP_ID_fromLocationId);
            
        }
    }
    
    /**
     * 变更后地点: TO_LOCATION_ID
     */
    public final java.lang.String getToLocationId(){
         onPropGet(PROP_ID_toLocationId);
         return _toLocationId;
    }

    /**
     * 变更后地点: TO_LOCATION_ID
     */
    public final void setToLocationId(java.lang.String value){
        if(onPropSet(PROP_ID_toLocationId,value)){
            this._toLocationId = value;
            internalClearRefs(PROP_ID_toLocationId);
            
        }
    }
    
    /**
     * 变更前使用人: FROM_STAFF_ID
     */
    public final java.lang.String getFromStaffId(){
         onPropGet(PROP_ID_fromStaffId);
         return _fromStaffId;
    }

    /**
     * 变更前使用人: FROM_STAFF_ID
     */
    public final void setFromStaffId(java.lang.String value){
        if(onPropSet(PROP_ID_fromStaffId,value)){
            this._fromStaffId = value;
            internalClearRefs(PROP_ID_fromStaffId);
            
        }
    }
    
    /**
     * 变更后使用人: TO_STAFF_ID
     */
    public final java.lang.String getToStaffId(){
         onPropGet(PROP_ID_toStaffId);
         return _toStaffId;
    }

    /**
     * 变更后使用人: TO_STAFF_ID
     */
    public final void setToStaffId(java.lang.String value){
        if(onPropSet(PROP_ID_toStaffId,value)){
            this._toStaffId = value;
            internalClearRefs(PROP_ID_toStaffId);
            
        }
    }
    
    /**
     * 关联单据实体: REF_ENTITY_NAME
     */
    public final java.lang.String getRefEntityName(){
         onPropGet(PROP_ID_refEntityName);
         return _refEntityName;
    }

    /**
     * 关联单据实体: REF_ENTITY_NAME
     */
    public final void setRefEntityName(java.lang.String value){
        if(onPropSet(PROP_ID_refEntityName,value)){
            this._refEntityName = value;
            internalClearRefs(PROP_ID_refEntityName);
            
        }
    }
    
    /**
     * 关联单据ID: REF_ENTITY_ID
     */
    public final java.lang.String getRefEntityId(){
         onPropGet(PROP_ID_refEntityId);
         return _refEntityId;
    }

    /**
     * 关联单据ID: REF_ENTITY_ID
     */
    public final void setRefEntityId(java.lang.String value){
        if(onPropSet(PROP_ID_refEntityId,value)){
            this._refEntityId = value;
            internalClearRefs(PROP_ID_refEntityId);
            
        }
    }
    
    /**
     * 摘要: SUMMARY
     */
    public final java.lang.String getSummary(){
         onPropGet(PROP_ID_summary);
         return _summary;
    }

    /**
     * 摘要: SUMMARY
     */
    public final void setSummary(java.lang.String value){
        if(onPropSet(PROP_ID_summary,value)){
            this._summary = value;
            internalClearRefs(PROP_ID_summary);
            
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
    public final app.erp.ast.dao.entity.ErpAstAsset getAsset(){
       return (app.erp.ast.dao.entity.ErpAstAsset)internalGetRefEntity(PROP_NAME_asset);
    }

    public final void setAsset(app.erp.ast.dao.entity.ErpAstAsset refEntity){
   
           if(refEntity == null){
           
                   this.setAssetId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_asset, refEntity,()->{
           
                           this.setAssetId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
