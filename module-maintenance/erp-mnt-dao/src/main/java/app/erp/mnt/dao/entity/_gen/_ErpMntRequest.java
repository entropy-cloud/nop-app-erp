package app.erp.mnt.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.mnt.dao.entity.ErpMntRequest;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  维护请求: erp_mnt_request
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMntRequest extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 请求编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 设备ID: EQUIPMENT_ID BIGINT */
    public static final String PROP_NAME_equipmentId = "equipmentId";
    public static final int PROP_ID_equipmentId = 3;
    
    /* 请求日期: REQUEST_DATE DATE */
    public static final String PROP_NAME_requestDate = "requestDate";
    public static final int PROP_ID_requestDate = 4;
    
    /* 问题描述: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 5;
    
    /* 优先级: PRIORITY VARCHAR */
    public static final String PROP_NAME_priority = "priority";
    public static final int PROP_ID_priority = 6;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 7;
    
    /* 请求人: REQUESTED_BY BIGINT */
    public static final String PROP_NAME_requestedBy = "requestedBy";
    public static final int PROP_ID_requestedBy = 8;
    
    /* 指派人: ASSIGNED_TO BIGINT */
    public static final String PROP_NAME_assignedTo = "assignedTo";
    public static final int PROP_ID_assignedTo = 9;
    
    /* 受理人: ACCEPTED_BY BIGINT */
    public static final String PROP_NAME_acceptedBy = "acceptedBy";
    public static final int PROP_ID_acceptedBy = 10;
    
    /* 完成人: COMPLETED_BY BIGINT */
    public static final String PROP_NAME_completedBy = "completedBy";
    public static final int PROP_ID_completedBy = 11;
    
    /* 完成时间: COMPLETED_AT TIMESTAMP */
    public static final String PROP_NAME_completedAt = "completedAt";
    public static final int PROP_ID_completedAt = 12;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 13;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 14;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 15;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 16;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 17;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 18;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 19;
    

    private static int _PROP_ID_BOUND = 20;

    
    /* relation:  */
    public static final String PROP_NAME_equipment = "equipment";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_equipmentId] = PROP_NAME_equipmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_equipmentId, PROP_ID_equipmentId);
      
          PROP_ID_TO_NAME[PROP_ID_requestDate] = PROP_NAME_requestDate;
          PROP_NAME_TO_ID.put(PROP_NAME_requestDate, PROP_ID_requestDate);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
          PROP_ID_TO_NAME[PROP_ID_priority] = PROP_NAME_priority;
          PROP_NAME_TO_ID.put(PROP_NAME_priority, PROP_ID_priority);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_requestedBy] = PROP_NAME_requestedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_requestedBy, PROP_ID_requestedBy);
      
          PROP_ID_TO_NAME[PROP_ID_assignedTo] = PROP_NAME_assignedTo;
          PROP_NAME_TO_ID.put(PROP_NAME_assignedTo, PROP_ID_assignedTo);
      
          PROP_ID_TO_NAME[PROP_ID_acceptedBy] = PROP_NAME_acceptedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_acceptedBy, PROP_ID_acceptedBy);
      
          PROP_ID_TO_NAME[PROP_ID_completedBy] = PROP_NAME_completedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_completedBy, PROP_ID_completedBy);
      
          PROP_ID_TO_NAME[PROP_ID_completedAt] = PROP_NAME_completedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_completedAt, PROP_ID_completedAt);
      
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
    
    /* 请求编码: CODE */
    private java.lang.String _code;
    
    /* 设备ID: EQUIPMENT_ID */
    private java.lang.Long _equipmentId;
    
    /* 请求日期: REQUEST_DATE */
    private java.time.LocalDate _requestDate;
    
    /* 问题描述: DESCRIPTION */
    private java.lang.String _description;
    
    /* 优先级: PRIORITY */
    private java.lang.String _priority;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 请求人: REQUESTED_BY */
    private java.lang.Long _requestedBy;
    
    /* 指派人: ASSIGNED_TO */
    private java.lang.Long _assignedTo;
    
    /* 受理人: ACCEPTED_BY */
    private java.lang.Long _acceptedBy;
    
    /* 完成人: COMPLETED_BY */
    private java.lang.Long _completedBy;
    
    /* 完成时间: COMPLETED_AT */
    private java.sql.Timestamp _completedAt;
    
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
    

    public _ErpMntRequest(){
        // for debug
    }

    protected ErpMntRequest newInstance(){
        ErpMntRequest entity = new ErpMntRequest();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMntRequest cloneInstance() {
        ErpMntRequest entity = newInstance();
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
      return "app.erp.mnt.dao.entity.ErpMntRequest";
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
        
            case PROP_ID_code:
               return getCode();
        
            case PROP_ID_equipmentId:
               return getEquipmentId();
        
            case PROP_ID_requestDate:
               return getRequestDate();
        
            case PROP_ID_description:
               return getDescription();
        
            case PROP_ID_priority:
               return getPriority();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_requestedBy:
               return getRequestedBy();
        
            case PROP_ID_assignedTo:
               return getAssignedTo();
        
            case PROP_ID_acceptedBy:
               return getAcceptedBy();
        
            case PROP_ID_completedBy:
               return getCompletedBy();
        
            case PROP_ID_completedAt:
               return getCompletedAt();
        
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
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
               break;
            }
        
            case PROP_ID_equipmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_equipmentId));
               }
               setEquipmentId(typedValue);
               break;
            }
        
            case PROP_ID_requestDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_requestDate));
               }
               setRequestDate(typedValue);
               break;
            }
        
            case PROP_ID_description:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_description));
               }
               setDescription(typedValue);
               break;
            }
        
            case PROP_ID_priority:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_priority));
               }
               setPriority(typedValue);
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
        
            case PROP_ID_requestedBy:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_requestedBy));
               }
               setRequestedBy(typedValue);
               break;
            }
        
            case PROP_ID_assignedTo:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_assignedTo));
               }
               setAssignedTo(typedValue);
               break;
            }
        
            case PROP_ID_acceptedBy:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_acceptedBy));
               }
               setAcceptedBy(typedValue);
               break;
            }
        
            case PROP_ID_completedBy:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_completedBy));
               }
               setCompletedBy(typedValue);
               break;
            }
        
            case PROP_ID_completedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_completedAt));
               }
               setCompletedAt(typedValue);
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
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_equipmentId:{
               onInitProp(propId);
               this._equipmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_requestDate:{
               onInitProp(propId);
               this._requestDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_priority:{
               onInitProp(propId);
               this._priority = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_requestedBy:{
               onInitProp(propId);
               this._requestedBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_assignedTo:{
               onInitProp(propId);
               this._assignedTo = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_acceptedBy:{
               onInitProp(propId);
               this._acceptedBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_completedBy:{
               onInitProp(propId);
               this._completedBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_completedAt:{
               onInitProp(propId);
               this._completedAt = (java.sql.Timestamp)value;
               
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
     * 请求编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 请求编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 设备ID: EQUIPMENT_ID
     */
    public final java.lang.Long getEquipmentId(){
         onPropGet(PROP_ID_equipmentId);
         return _equipmentId;
    }

    /**
     * 设备ID: EQUIPMENT_ID
     */
    public final void setEquipmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_equipmentId,value)){
            this._equipmentId = value;
            internalClearRefs(PROP_ID_equipmentId);
            
        }
    }
    
    /**
     * 请求日期: REQUEST_DATE
     */
    public final java.time.LocalDate getRequestDate(){
         onPropGet(PROP_ID_requestDate);
         return _requestDate;
    }

    /**
     * 请求日期: REQUEST_DATE
     */
    public final void setRequestDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_requestDate,value)){
            this._requestDate = value;
            internalClearRefs(PROP_ID_requestDate);
            
        }
    }
    
    /**
     * 问题描述: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 问题描述: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
        }
    }
    
    /**
     * 优先级: PRIORITY
     */
    public final java.lang.String getPriority(){
         onPropGet(PROP_ID_priority);
         return _priority;
    }

    /**
     * 优先级: PRIORITY
     */
    public final void setPriority(java.lang.String value){
        if(onPropSet(PROP_ID_priority,value)){
            this._priority = value;
            internalClearRefs(PROP_ID_priority);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 请求人: REQUESTED_BY
     */
    public final java.lang.Long getRequestedBy(){
         onPropGet(PROP_ID_requestedBy);
         return _requestedBy;
    }

    /**
     * 请求人: REQUESTED_BY
     */
    public final void setRequestedBy(java.lang.Long value){
        if(onPropSet(PROP_ID_requestedBy,value)){
            this._requestedBy = value;
            internalClearRefs(PROP_ID_requestedBy);
            
        }
    }
    
    /**
     * 指派人: ASSIGNED_TO
     */
    public final java.lang.Long getAssignedTo(){
         onPropGet(PROP_ID_assignedTo);
         return _assignedTo;
    }

    /**
     * 指派人: ASSIGNED_TO
     */
    public final void setAssignedTo(java.lang.Long value){
        if(onPropSet(PROP_ID_assignedTo,value)){
            this._assignedTo = value;
            internalClearRefs(PROP_ID_assignedTo);
            
        }
    }
    
    /**
     * 受理人: ACCEPTED_BY
     */
    public final java.lang.Long getAcceptedBy(){
         onPropGet(PROP_ID_acceptedBy);
         return _acceptedBy;
    }

    /**
     * 受理人: ACCEPTED_BY
     */
    public final void setAcceptedBy(java.lang.Long value){
        if(onPropSet(PROP_ID_acceptedBy,value)){
            this._acceptedBy = value;
            internalClearRefs(PROP_ID_acceptedBy);
            
        }
    }
    
    /**
     * 完成人: COMPLETED_BY
     */
    public final java.lang.Long getCompletedBy(){
         onPropGet(PROP_ID_completedBy);
         return _completedBy;
    }

    /**
     * 完成人: COMPLETED_BY
     */
    public final void setCompletedBy(java.lang.Long value){
        if(onPropSet(PROP_ID_completedBy,value)){
            this._completedBy = value;
            internalClearRefs(PROP_ID_completedBy);
            
        }
    }
    
    /**
     * 完成时间: COMPLETED_AT
     */
    public final java.sql.Timestamp getCompletedAt(){
         onPropGet(PROP_ID_completedAt);
         return _completedAt;
    }

    /**
     * 完成时间: COMPLETED_AT
     */
    public final void setCompletedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_completedAt,value)){
            this._completedAt = value;
            internalClearRefs(PROP_ID_completedAt);
            
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
    public final app.erp.mnt.dao.entity.ErpMntEquipment getEquipment(){
       return (app.erp.mnt.dao.entity.ErpMntEquipment)internalGetRefEntity(PROP_NAME_equipment);
    }

    public final void setEquipment(app.erp.mnt.dao.entity.ErpMntEquipment refEntity){
   
           if(refEntity == null){
           
                   this.setEquipmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_equipment, refEntity,()->{
           
                           this.setEquipmentId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
