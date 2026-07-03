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

import app.erp.mnt.dao.entity.ErpMntSchedule;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  维护计划: erp_mnt_schedule
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMntSchedule extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 计划编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 计划名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 设备ID: EQUIPMENT_ID BIGINT */
    public static final String PROP_NAME_equipmentId = "equipmentId";
    public static final int PROP_ID_equipmentId = 4;
    
    /* 计划类型: SCHEDULE_TYPE VARCHAR */
    public static final String PROP_NAME_scheduleType = "scheduleType";
    public static final int PROP_ID_scheduleType = 5;
    
    /* 频率: FREQUENCY INTEGER */
    public static final String PROP_NAME_frequency = "frequency";
    public static final int PROP_ID_frequency = 6;
    
    /* 重复类型: RECURRENCE_TYPE VARCHAR */
    public static final String PROP_NAME_recurrenceType = "recurrenceType";
    public static final int PROP_ID_recurrenceType = 7;
    
    /* 星期几: DAYS_OF_WEEK VARCHAR */
    public static final String PROP_NAME_daysOfWeek = "daysOfWeek";
    public static final int PROP_ID_daysOfWeek = 8;
    
    /* 开始日期: START_DATE DATE */
    public static final String PROP_NAME_startDate = "startDate";
    public static final int PROP_ID_startDate = 9;
    
    /* 结束日期: END_DATE DATE */
    public static final String PROP_NAME_endDate = "endDate";
    public static final int PROP_ID_endDate = 10;
    
    /* 下次到期日: NEXT_DUE_DATE DATE */
    public static final String PROP_NAME_nextDueDate = "nextDueDate";
    public static final int PROP_ID_nextDueDate = 11;
    
    /* 是否启用: IS_ACTIVE INTEGER */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 12;
    
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
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_equipmentId] = PROP_NAME_equipmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_equipmentId, PROP_ID_equipmentId);
      
          PROP_ID_TO_NAME[PROP_ID_scheduleType] = PROP_NAME_scheduleType;
          PROP_NAME_TO_ID.put(PROP_NAME_scheduleType, PROP_ID_scheduleType);
      
          PROP_ID_TO_NAME[PROP_ID_frequency] = PROP_NAME_frequency;
          PROP_NAME_TO_ID.put(PROP_NAME_frequency, PROP_ID_frequency);
      
          PROP_ID_TO_NAME[PROP_ID_recurrenceType] = PROP_NAME_recurrenceType;
          PROP_NAME_TO_ID.put(PROP_NAME_recurrenceType, PROP_ID_recurrenceType);
      
          PROP_ID_TO_NAME[PROP_ID_daysOfWeek] = PROP_NAME_daysOfWeek;
          PROP_NAME_TO_ID.put(PROP_NAME_daysOfWeek, PROP_ID_daysOfWeek);
      
          PROP_ID_TO_NAME[PROP_ID_startDate] = PROP_NAME_startDate;
          PROP_NAME_TO_ID.put(PROP_NAME_startDate, PROP_ID_startDate);
      
          PROP_ID_TO_NAME[PROP_ID_endDate] = PROP_NAME_endDate;
          PROP_NAME_TO_ID.put(PROP_NAME_endDate, PROP_ID_endDate);
      
          PROP_ID_TO_NAME[PROP_ID_nextDueDate] = PROP_NAME_nextDueDate;
          PROP_NAME_TO_ID.put(PROP_NAME_nextDueDate, PROP_ID_nextDueDate);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
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
    
    /* 计划编码: CODE */
    private java.lang.String _code;
    
    /* 计划名称: NAME */
    private java.lang.String _name;
    
    /* 设备ID: EQUIPMENT_ID */
    private java.lang.Long _equipmentId;
    
    /* 计划类型: SCHEDULE_TYPE */
    private java.lang.String _scheduleType;
    
    /* 频率: FREQUENCY */
    private java.lang.Integer _frequency;
    
    /* 重复类型: RECURRENCE_TYPE */
    private java.lang.String _recurrenceType;
    
    /* 星期几: DAYS_OF_WEEK */
    private java.lang.String _daysOfWeek;
    
    /* 开始日期: START_DATE */
    private java.time.LocalDate _startDate;
    
    /* 结束日期: END_DATE */
    private java.time.LocalDate _endDate;
    
    /* 下次到期日: NEXT_DUE_DATE */
    private java.time.LocalDate _nextDueDate;
    
    /* 是否启用: IS_ACTIVE */
    private java.lang.Integer _isActive;
    
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
    

    public _ErpMntSchedule(){
        // for debug
    }

    protected ErpMntSchedule newInstance(){
        ErpMntSchedule entity = new ErpMntSchedule();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMntSchedule cloneInstance() {
        ErpMntSchedule entity = newInstance();
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
      return "app.erp.mnt.dao.entity.ErpMntSchedule";
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
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_equipmentId:
               return getEquipmentId();
        
            case PROP_ID_scheduleType:
               return getScheduleType();
        
            case PROP_ID_frequency:
               return getFrequency();
        
            case PROP_ID_recurrenceType:
               return getRecurrenceType();
        
            case PROP_ID_daysOfWeek:
               return getDaysOfWeek();
        
            case PROP_ID_startDate:
               return getStartDate();
        
            case PROP_ID_endDate:
               return getEndDate();
        
            case PROP_ID_nextDueDate:
               return getNextDueDate();
        
            case PROP_ID_isActive:
               return getIsActive();
        
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
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
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
        
            case PROP_ID_scheduleType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_scheduleType));
               }
               setScheduleType(typedValue);
               break;
            }
        
            case PROP_ID_frequency:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_frequency));
               }
               setFrequency(typedValue);
               break;
            }
        
            case PROP_ID_recurrenceType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_recurrenceType));
               }
               setRecurrenceType(typedValue);
               break;
            }
        
            case PROP_ID_daysOfWeek:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_daysOfWeek));
               }
               setDaysOfWeek(typedValue);
               break;
            }
        
            case PROP_ID_startDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_startDate));
               }
               setStartDate(typedValue);
               break;
            }
        
            case PROP_ID_endDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_endDate));
               }
               setEndDate(typedValue);
               break;
            }
        
            case PROP_ID_nextDueDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_nextDueDate));
               }
               setNextDueDate(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_isActive));
               }
               setIsActive(typedValue);
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
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_equipmentId:{
               onInitProp(propId);
               this._equipmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_scheduleType:{
               onInitProp(propId);
               this._scheduleType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_frequency:{
               onInitProp(propId);
               this._frequency = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_recurrenceType:{
               onInitProp(propId);
               this._recurrenceType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_daysOfWeek:{
               onInitProp(propId);
               this._daysOfWeek = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_startDate:{
               onInitProp(propId);
               this._startDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_endDate:{
               onInitProp(propId);
               this._endDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_nextDueDate:{
               onInitProp(propId);
               this._nextDueDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Integer)value;
               
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
     * 计划编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 计划编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 计划名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 计划名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
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
     * 计划类型: SCHEDULE_TYPE
     */
    public final java.lang.String getScheduleType(){
         onPropGet(PROP_ID_scheduleType);
         return _scheduleType;
    }

    /**
     * 计划类型: SCHEDULE_TYPE
     */
    public final void setScheduleType(java.lang.String value){
        if(onPropSet(PROP_ID_scheduleType,value)){
            this._scheduleType = value;
            internalClearRefs(PROP_ID_scheduleType);
            
        }
    }
    
    /**
     * 频率: FREQUENCY
     */
    public final java.lang.Integer getFrequency(){
         onPropGet(PROP_ID_frequency);
         return _frequency;
    }

    /**
     * 频率: FREQUENCY
     */
    public final void setFrequency(java.lang.Integer value){
        if(onPropSet(PROP_ID_frequency,value)){
            this._frequency = value;
            internalClearRefs(PROP_ID_frequency);
            
        }
    }
    
    /**
     * 重复类型: RECURRENCE_TYPE
     */
    public final java.lang.String getRecurrenceType(){
         onPropGet(PROP_ID_recurrenceType);
         return _recurrenceType;
    }

    /**
     * 重复类型: RECURRENCE_TYPE
     */
    public final void setRecurrenceType(java.lang.String value){
        if(onPropSet(PROP_ID_recurrenceType,value)){
            this._recurrenceType = value;
            internalClearRefs(PROP_ID_recurrenceType);
            
        }
    }
    
    /**
     * 星期几: DAYS_OF_WEEK
     */
    public final java.lang.String getDaysOfWeek(){
         onPropGet(PROP_ID_daysOfWeek);
         return _daysOfWeek;
    }

    /**
     * 星期几: DAYS_OF_WEEK
     */
    public final void setDaysOfWeek(java.lang.String value){
        if(onPropSet(PROP_ID_daysOfWeek,value)){
            this._daysOfWeek = value;
            internalClearRefs(PROP_ID_daysOfWeek);
            
        }
    }
    
    /**
     * 开始日期: START_DATE
     */
    public final java.time.LocalDate getStartDate(){
         onPropGet(PROP_ID_startDate);
         return _startDate;
    }

    /**
     * 开始日期: START_DATE
     */
    public final void setStartDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_startDate,value)){
            this._startDate = value;
            internalClearRefs(PROP_ID_startDate);
            
        }
    }
    
    /**
     * 结束日期: END_DATE
     */
    public final java.time.LocalDate getEndDate(){
         onPropGet(PROP_ID_endDate);
         return _endDate;
    }

    /**
     * 结束日期: END_DATE
     */
    public final void setEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_endDate,value)){
            this._endDate = value;
            internalClearRefs(PROP_ID_endDate);
            
        }
    }
    
    /**
     * 下次到期日: NEXT_DUE_DATE
     */
    public final java.time.LocalDate getNextDueDate(){
         onPropGet(PROP_ID_nextDueDate);
         return _nextDueDate;
    }

    /**
     * 下次到期日: NEXT_DUE_DATE
     */
    public final void setNextDueDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_nextDueDate,value)){
            this._nextDueDate = value;
            internalClearRefs(PROP_ID_nextDueDate);
            
        }
    }
    
    /**
     * 是否启用: IS_ACTIVE
     */
    public final java.lang.Integer getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Integer value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
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
