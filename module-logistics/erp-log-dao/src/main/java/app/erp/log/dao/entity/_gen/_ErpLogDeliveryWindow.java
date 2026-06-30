package app.erp.log.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.log.dao.entity.ErpLogDeliveryWindow;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  配送时间窗口: erp_log_delivery_window
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpLogDeliveryWindow extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 客户: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 星期: WEEKDAY INTEGER */
    public static final String PROP_NAME_weekday = "weekday";
    public static final int PROP_ID_weekday = 4;
    
    /* 开始时间: START_TIME VARCHAR */
    public static final String PROP_NAME_startTime = "startTime";
    public static final int PROP_ID_startTime = 5;
    
    /* 结束时间: END_TIME VARCHAR */
    public static final String PROP_NAME_endTime = "endTime";
    public static final int PROP_ID_endTime = 6;
    
    /* 最大预约数: MAX_CAPACITY INTEGER */
    public static final String PROP_NAME_maxCapacity = "maxCapacity";
    public static final int PROP_ID_maxCapacity = 7;
    
    /* 当前已预约: CURRENT_BOOKED INTEGER */
    public static final String PROP_NAME_currentBooked = "currentBooked";
    public static final int PROP_ID_currentBooked = 8;
    
    /* 是否启用: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 9;
    
    /* 生效日期: EFFECTIVE_FROM DATE */
    public static final String PROP_NAME_effectiveFrom = "effectiveFrom";
    public static final int PROP_ID_effectiveFrom = 10;
    
    /* 失效日期: EFFECTIVE_TO DATE */
    public static final String PROP_NAME_effectiveTo = "effectiveTo";
    public static final int PROP_ID_effectiveTo = 11;
    
    /* 允许发运类型: ALLOWED_SHIPMENT_TYPES VARCHAR */
    public static final String PROP_NAME_allowedShipmentTypes = "allowedShipmentTypes";
    public static final int PROP_ID_allowedShipmentTypes = 12;
    
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

    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_weekday] = PROP_NAME_weekday;
          PROP_NAME_TO_ID.put(PROP_NAME_weekday, PROP_ID_weekday);
      
          PROP_ID_TO_NAME[PROP_ID_startTime] = PROP_NAME_startTime;
          PROP_NAME_TO_ID.put(PROP_NAME_startTime, PROP_ID_startTime);
      
          PROP_ID_TO_NAME[PROP_ID_endTime] = PROP_NAME_endTime;
          PROP_NAME_TO_ID.put(PROP_NAME_endTime, PROP_ID_endTime);
      
          PROP_ID_TO_NAME[PROP_ID_maxCapacity] = PROP_NAME_maxCapacity;
          PROP_NAME_TO_ID.put(PROP_NAME_maxCapacity, PROP_ID_maxCapacity);
      
          PROP_ID_TO_NAME[PROP_ID_currentBooked] = PROP_NAME_currentBooked;
          PROP_NAME_TO_ID.put(PROP_NAME_currentBooked, PROP_ID_currentBooked);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
          PROP_ID_TO_NAME[PROP_ID_effectiveFrom] = PROP_NAME_effectiveFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_effectiveFrom, PROP_ID_effectiveFrom);
      
          PROP_ID_TO_NAME[PROP_ID_effectiveTo] = PROP_NAME_effectiveTo;
          PROP_NAME_TO_ID.put(PROP_NAME_effectiveTo, PROP_ID_effectiveTo);
      
          PROP_ID_TO_NAME[PROP_ID_allowedShipmentTypes] = PROP_NAME_allowedShipmentTypes;
          PROP_NAME_TO_ID.put(PROP_NAME_allowedShipmentTypes, PROP_ID_allowedShipmentTypes);
      
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
    
    /* 客户: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 星期: WEEKDAY */
    private java.lang.Integer _weekday;
    
    /* 开始时间: START_TIME */
    private java.lang.String _startTime;
    
    /* 结束时间: END_TIME */
    private java.lang.String _endTime;
    
    /* 最大预约数: MAX_CAPACITY */
    private java.lang.Integer _maxCapacity;
    
    /* 当前已预约: CURRENT_BOOKED */
    private java.lang.Integer _currentBooked;
    
    /* 是否启用: IS_ACTIVE */
    private java.lang.Boolean _isActive;
    
    /* 生效日期: EFFECTIVE_FROM */
    private java.time.LocalDate _effectiveFrom;
    
    /* 失效日期: EFFECTIVE_TO */
    private java.time.LocalDate _effectiveTo;
    
    /* 允许发运类型: ALLOWED_SHIPMENT_TYPES */
    private java.lang.String _allowedShipmentTypes;
    
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
    

    public _ErpLogDeliveryWindow(){
        // for debug
    }

    protected ErpLogDeliveryWindow newInstance(){
        ErpLogDeliveryWindow entity = new ErpLogDeliveryWindow();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpLogDeliveryWindow cloneInstance() {
        ErpLogDeliveryWindow entity = newInstance();
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
      return "app.erp.log.dao.entity.ErpLogDeliveryWindow";
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
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_weekday:
               return getWeekday();
        
            case PROP_ID_startTime:
               return getStartTime();
        
            case PROP_ID_endTime:
               return getEndTime();
        
            case PROP_ID_maxCapacity:
               return getMaxCapacity();
        
            case PROP_ID_currentBooked:
               return getCurrentBooked();
        
            case PROP_ID_isActive:
               return getIsActive();
        
            case PROP_ID_effectiveFrom:
               return getEffectiveFrom();
        
            case PROP_ID_effectiveTo:
               return getEffectiveTo();
        
            case PROP_ID_allowedShipmentTypes:
               return getAllowedShipmentTypes();
        
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
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
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
        
            case PROP_ID_weekday:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_weekday));
               }
               setWeekday(typedValue);
               break;
            }
        
            case PROP_ID_startTime:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_startTime));
               }
               setStartTime(typedValue);
               break;
            }
        
            case PROP_ID_endTime:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_endTime));
               }
               setEndTime(typedValue);
               break;
            }
        
            case PROP_ID_maxCapacity:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_maxCapacity));
               }
               setMaxCapacity(typedValue);
               break;
            }
        
            case PROP_ID_currentBooked:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_currentBooked));
               }
               setCurrentBooked(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isActive));
               }
               setIsActive(typedValue);
               break;
            }
        
            case PROP_ID_effectiveFrom:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_effectiveFrom));
               }
               setEffectiveFrom(typedValue);
               break;
            }
        
            case PROP_ID_effectiveTo:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_effectiveTo));
               }
               setEffectiveTo(typedValue);
               break;
            }
        
            case PROP_ID_allowedShipmentTypes:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_allowedShipmentTypes));
               }
               setAllowedShipmentTypes(typedValue);
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
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_weekday:{
               onInitProp(propId);
               this._weekday = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_startTime:{
               onInitProp(propId);
               this._startTime = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_endTime:{
               onInitProp(propId);
               this._endTime = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_maxCapacity:{
               onInitProp(propId);
               this._maxCapacity = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_currentBooked:{
               onInitProp(propId);
               this._currentBooked = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_effectiveFrom:{
               onInitProp(propId);
               this._effectiveFrom = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_effectiveTo:{
               onInitProp(propId);
               this._effectiveTo = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_allowedShipmentTypes:{
               onInitProp(propId);
               this._allowedShipmentTypes = (java.lang.String)value;
               
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
     * 客户: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 客户: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
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
     * 星期: WEEKDAY
     */
    public final java.lang.Integer getWeekday(){
         onPropGet(PROP_ID_weekday);
         return _weekday;
    }

    /**
     * 星期: WEEKDAY
     */
    public final void setWeekday(java.lang.Integer value){
        if(onPropSet(PROP_ID_weekday,value)){
            this._weekday = value;
            internalClearRefs(PROP_ID_weekday);
            
        }
    }
    
    /**
     * 开始时间: START_TIME
     */
    public final java.lang.String getStartTime(){
         onPropGet(PROP_ID_startTime);
         return _startTime;
    }

    /**
     * 开始时间: START_TIME
     */
    public final void setStartTime(java.lang.String value){
        if(onPropSet(PROP_ID_startTime,value)){
            this._startTime = value;
            internalClearRefs(PROP_ID_startTime);
            
        }
    }
    
    /**
     * 结束时间: END_TIME
     */
    public final java.lang.String getEndTime(){
         onPropGet(PROP_ID_endTime);
         return _endTime;
    }

    /**
     * 结束时间: END_TIME
     */
    public final void setEndTime(java.lang.String value){
        if(onPropSet(PROP_ID_endTime,value)){
            this._endTime = value;
            internalClearRefs(PROP_ID_endTime);
            
        }
    }
    
    /**
     * 最大预约数: MAX_CAPACITY
     */
    public final java.lang.Integer getMaxCapacity(){
         onPropGet(PROP_ID_maxCapacity);
         return _maxCapacity;
    }

    /**
     * 最大预约数: MAX_CAPACITY
     */
    public final void setMaxCapacity(java.lang.Integer value){
        if(onPropSet(PROP_ID_maxCapacity,value)){
            this._maxCapacity = value;
            internalClearRefs(PROP_ID_maxCapacity);
            
        }
    }
    
    /**
     * 当前已预约: CURRENT_BOOKED
     */
    public final java.lang.Integer getCurrentBooked(){
         onPropGet(PROP_ID_currentBooked);
         return _currentBooked;
    }

    /**
     * 当前已预约: CURRENT_BOOKED
     */
    public final void setCurrentBooked(java.lang.Integer value){
        if(onPropSet(PROP_ID_currentBooked,value)){
            this._currentBooked = value;
            internalClearRefs(PROP_ID_currentBooked);
            
        }
    }
    
    /**
     * 是否启用: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
        }
    }
    
    /**
     * 生效日期: EFFECTIVE_FROM
     */
    public final java.time.LocalDate getEffectiveFrom(){
         onPropGet(PROP_ID_effectiveFrom);
         return _effectiveFrom;
    }

    /**
     * 生效日期: EFFECTIVE_FROM
     */
    public final void setEffectiveFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_effectiveFrom,value)){
            this._effectiveFrom = value;
            internalClearRefs(PROP_ID_effectiveFrom);
            
        }
    }
    
    /**
     * 失效日期: EFFECTIVE_TO
     */
    public final java.time.LocalDate getEffectiveTo(){
         onPropGet(PROP_ID_effectiveTo);
         return _effectiveTo;
    }

    /**
     * 失效日期: EFFECTIVE_TO
     */
    public final void setEffectiveTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_effectiveTo,value)){
            this._effectiveTo = value;
            internalClearRefs(PROP_ID_effectiveTo);
            
        }
    }
    
    /**
     * 允许发运类型: ALLOWED_SHIPMENT_TYPES
     */
    public final java.lang.String getAllowedShipmentTypes(){
         onPropGet(PROP_ID_allowedShipmentTypes);
         return _allowedShipmentTypes;
    }

    /**
     * 允许发运类型: ALLOWED_SHIPMENT_TYPES
     */
    public final void setAllowedShipmentTypes(java.lang.String value){
        if(onPropSet(PROP_ID_allowedShipmentTypes,value)){
            this._allowedShipmentTypes = value;
            internalClearRefs(PROP_ID_allowedShipmentTypes);
            
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
    
}
// resume CPD analysis - CPD-ON
