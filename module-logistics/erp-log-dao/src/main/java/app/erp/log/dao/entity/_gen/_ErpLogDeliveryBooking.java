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

import app.erp.log.dao.entity.ErpLogDeliveryBooking;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  配送时段预约: erp_log_delivery_booking
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpLogDeliveryBooking extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 发运单: SHIPMENT_ID BIGINT */
    public static final String PROP_NAME_shipmentId = "shipmentId";
    public static final int PROP_ID_shipmentId = 2;
    
    /* 配送窗口: WINDOW_ID BIGINT */
    public static final String PROP_NAME_windowId = "windowId";
    public static final int PROP_ID_windowId = 3;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 4;
    
    /* 预约日期: BOOKED_DATE DATE */
    public static final String PROP_NAME_bookedDate = "bookedDate";
    public static final int PROP_ID_bookedDate = 5;
    
    /* 预约时间: BOOKED_TIME VARCHAR */
    public static final String PROP_NAME_bookedTime = "bookedTime";
    public static final int PROP_ID_bookedTime = 6;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 7;
    
    /* 爽约费: MISSED_FEE DECIMAL */
    public static final String PROP_NAME_missedFee = "missedFee";
    public static final int PROP_ID_missedFee = 8;
    
    /* 优先级评分: PRIORITY_SCORE INTEGER */
    public static final String PROP_NAME_priorityScore = "priorityScore";
    public static final int PROP_ID_priorityScore = 9;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 10;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 11;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 12;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 13;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 14;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 15;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 16;
    

    private static int _PROP_ID_BOUND = 17;

    
    /* relation:  */
    public static final String PROP_NAME_shipment = "shipment";
    
    /* relation:  */
    public static final String PROP_NAME_window = "window";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[17];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_shipmentId] = PROP_NAME_shipmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_shipmentId, PROP_ID_shipmentId);
      
          PROP_ID_TO_NAME[PROP_ID_windowId] = PROP_NAME_windowId;
          PROP_NAME_TO_ID.put(PROP_NAME_windowId, PROP_ID_windowId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_bookedDate] = PROP_NAME_bookedDate;
          PROP_NAME_TO_ID.put(PROP_NAME_bookedDate, PROP_ID_bookedDate);
      
          PROP_ID_TO_NAME[PROP_ID_bookedTime] = PROP_NAME_bookedTime;
          PROP_NAME_TO_ID.put(PROP_NAME_bookedTime, PROP_ID_bookedTime);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_missedFee] = PROP_NAME_missedFee;
          PROP_NAME_TO_ID.put(PROP_NAME_missedFee, PROP_ID_missedFee);
      
          PROP_ID_TO_NAME[PROP_ID_priorityScore] = PROP_NAME_priorityScore;
          PROP_NAME_TO_ID.put(PROP_NAME_priorityScore, PROP_ID_priorityScore);
      
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
    
    /* 发运单: SHIPMENT_ID */
    private java.lang.Long _shipmentId;
    
    /* 配送窗口: WINDOW_ID */
    private java.lang.Long _windowId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 预约日期: BOOKED_DATE */
    private java.time.LocalDate _bookedDate;
    
    /* 预约时间: BOOKED_TIME */
    private java.lang.String _bookedTime;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 爽约费: MISSED_FEE */
    private java.math.BigDecimal _missedFee;
    
    /* 优先级评分: PRIORITY_SCORE */
    private java.lang.Integer _priorityScore;
    
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
    

    public _ErpLogDeliveryBooking(){
        // for debug
    }

    protected ErpLogDeliveryBooking newInstance(){
        ErpLogDeliveryBooking entity = new ErpLogDeliveryBooking();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpLogDeliveryBooking cloneInstance() {
        ErpLogDeliveryBooking entity = newInstance();
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
      return "app.erp.log.dao.entity.ErpLogDeliveryBooking";
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
        
            case PROP_ID_shipmentId:
               return getShipmentId();
        
            case PROP_ID_windowId:
               return getWindowId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_bookedDate:
               return getBookedDate();
        
            case PROP_ID_bookedTime:
               return getBookedTime();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_missedFee:
               return getMissedFee();
        
            case PROP_ID_priorityScore:
               return getPriorityScore();
        
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
        
            case PROP_ID_shipmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_shipmentId));
               }
               setShipmentId(typedValue);
               break;
            }
        
            case PROP_ID_windowId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_windowId));
               }
               setWindowId(typedValue);
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
        
            case PROP_ID_bookedDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_bookedDate));
               }
               setBookedDate(typedValue);
               break;
            }
        
            case PROP_ID_bookedTime:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_bookedTime));
               }
               setBookedTime(typedValue);
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
        
            case PROP_ID_missedFee:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_missedFee));
               }
               setMissedFee(typedValue);
               break;
            }
        
            case PROP_ID_priorityScore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_priorityScore));
               }
               setPriorityScore(typedValue);
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
        
            case PROP_ID_shipmentId:{
               onInitProp(propId);
               this._shipmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_windowId:{
               onInitProp(propId);
               this._windowId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_bookedDate:{
               onInitProp(propId);
               this._bookedDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_bookedTime:{
               onInitProp(propId);
               this._bookedTime = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_missedFee:{
               onInitProp(propId);
               this._missedFee = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_priorityScore:{
               onInitProp(propId);
               this._priorityScore = (java.lang.Integer)value;
               
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
     * 发运单: SHIPMENT_ID
     */
    public final java.lang.Long getShipmentId(){
         onPropGet(PROP_ID_shipmentId);
         return _shipmentId;
    }

    /**
     * 发运单: SHIPMENT_ID
     */
    public final void setShipmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_shipmentId,value)){
            this._shipmentId = value;
            internalClearRefs(PROP_ID_shipmentId);
            
        }
    }
    
    /**
     * 配送窗口: WINDOW_ID
     */
    public final java.lang.Long getWindowId(){
         onPropGet(PROP_ID_windowId);
         return _windowId;
    }

    /**
     * 配送窗口: WINDOW_ID
     */
    public final void setWindowId(java.lang.Long value){
        if(onPropSet(PROP_ID_windowId,value)){
            this._windowId = value;
            internalClearRefs(PROP_ID_windowId);
            
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
     * 预约日期: BOOKED_DATE
     */
    public final java.time.LocalDate getBookedDate(){
         onPropGet(PROP_ID_bookedDate);
         return _bookedDate;
    }

    /**
     * 预约日期: BOOKED_DATE
     */
    public final void setBookedDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_bookedDate,value)){
            this._bookedDate = value;
            internalClearRefs(PROP_ID_bookedDate);
            
        }
    }
    
    /**
     * 预约时间: BOOKED_TIME
     */
    public final java.lang.String getBookedTime(){
         onPropGet(PROP_ID_bookedTime);
         return _bookedTime;
    }

    /**
     * 预约时间: BOOKED_TIME
     */
    public final void setBookedTime(java.lang.String value){
        if(onPropSet(PROP_ID_bookedTime,value)){
            this._bookedTime = value;
            internalClearRefs(PROP_ID_bookedTime);
            
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
     * 爽约费: MISSED_FEE
     */
    public final java.math.BigDecimal getMissedFee(){
         onPropGet(PROP_ID_missedFee);
         return _missedFee;
    }

    /**
     * 爽约费: MISSED_FEE
     */
    public final void setMissedFee(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_missedFee,value)){
            this._missedFee = value;
            internalClearRefs(PROP_ID_missedFee);
            
        }
    }
    
    /**
     * 优先级评分: PRIORITY_SCORE
     */
    public final java.lang.Integer getPriorityScore(){
         onPropGet(PROP_ID_priorityScore);
         return _priorityScore;
    }

    /**
     * 优先级评分: PRIORITY_SCORE
     */
    public final void setPriorityScore(java.lang.Integer value){
        if(onPropSet(PROP_ID_priorityScore,value)){
            this._priorityScore = value;
            internalClearRefs(PROP_ID_priorityScore);
            
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
    public final app.erp.log.dao.entity.ErpLogShipment getShipment(){
       return (app.erp.log.dao.entity.ErpLogShipment)internalGetRefEntity(PROP_NAME_shipment);
    }

    public final void setShipment(app.erp.log.dao.entity.ErpLogShipment refEntity){
   
           if(refEntity == null){
           
                   this.setShipmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_shipment, refEntity,()->{
           
                           this.setShipmentId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.log.dao.entity.ErpLogDeliveryWindow getWindow(){
       return (app.erp.log.dao.entity.ErpLogDeliveryWindow)internalGetRefEntity(PROP_NAME_window);
    }

    public final void setWindow(app.erp.log.dao.entity.ErpLogDeliveryWindow refEntity){
   
           if(refEntity == null){
           
                   this.setWindowId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_window, refEntity,()->{
           
                           this.setWindowId(refEntity.getId());
                       
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
