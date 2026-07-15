package app.erp.drp.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.drp.dao.entity.ErpInvDrpDockAppointment;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  月台预约: erp_inv_drp_dock_appointment
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpInvDrpDockAppointment extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 仓库: WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_warehouseId = "warehouseId";
    public static final int PROP_ID_warehouseId = 2;
    
    /* 月台: DOCK_ID BIGINT */
    public static final String PROP_NAME_dockId = "dockId";
    public static final int PROP_ID_dockId = 3;
    
    /* 预约日期: APPOINTMENT_DATE DATE */
    public static final String PROP_NAME_appointmentDate = "appointmentDate";
    public static final int PROP_ID_appointmentDate = 4;
    
    /* 时间窗口开始: SLOT_START TIMESTAMP */
    public static final String PROP_NAME_slotStart = "slotStart";
    public static final int PROP_ID_slotStart = 5;
    
    /* 时间窗口结束: SLOT_END TIMESTAMP */
    public static final String PROP_NAME_slotEnd = "slotEnd";
    public static final int PROP_ID_slotEnd = 6;
    
    /* 关联越库: CROSS_DOCK_ID BIGINT */
    public static final String PROP_NAME_crossDockId = "crossDockId";
    public static final int PROP_ID_crossDockId = 7;
    
    /* 承运商信息: CARRIER_INFO VARCHAR */
    public static final String PROP_NAME_carrierInfo = "carrierInfo";
    public static final int PROP_ID_carrierInfo = 8;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 9;
    
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
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 17;
    

    private static int _PROP_ID_BOUND = 18;

    
    /* relation:  */
    public static final String PROP_NAME_warehouse = "warehouse";
    
    /* relation:  */
    public static final String PROP_NAME_dock = "dock";
    
    /* relation:  */
    public static final String PROP_NAME_crossDock = "crossDock";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_warehouseId] = PROP_NAME_warehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_warehouseId, PROP_ID_warehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_dockId] = PROP_NAME_dockId;
          PROP_NAME_TO_ID.put(PROP_NAME_dockId, PROP_ID_dockId);
      
          PROP_ID_TO_NAME[PROP_ID_appointmentDate] = PROP_NAME_appointmentDate;
          PROP_NAME_TO_ID.put(PROP_NAME_appointmentDate, PROP_ID_appointmentDate);
      
          PROP_ID_TO_NAME[PROP_ID_slotStart] = PROP_NAME_slotStart;
          PROP_NAME_TO_ID.put(PROP_NAME_slotStart, PROP_ID_slotStart);
      
          PROP_ID_TO_NAME[PROP_ID_slotEnd] = PROP_NAME_slotEnd;
          PROP_NAME_TO_ID.put(PROP_NAME_slotEnd, PROP_ID_slotEnd);
      
          PROP_ID_TO_NAME[PROP_ID_crossDockId] = PROP_NAME_crossDockId;
          PROP_NAME_TO_ID.put(PROP_NAME_crossDockId, PROP_ID_crossDockId);
      
          PROP_ID_TO_NAME[PROP_ID_carrierInfo] = PROP_NAME_carrierInfo;
          PROP_NAME_TO_ID.put(PROP_NAME_carrierInfo, PROP_ID_carrierInfo);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 仓库: WAREHOUSE_ID */
    private java.lang.Long _warehouseId;
    
    /* 月台: DOCK_ID */
    private java.lang.Long _dockId;
    
    /* 预约日期: APPOINTMENT_DATE */
    private java.time.LocalDate _appointmentDate;
    
    /* 时间窗口开始: SLOT_START */
    private java.sql.Timestamp _slotStart;
    
    /* 时间窗口结束: SLOT_END */
    private java.sql.Timestamp _slotEnd;
    
    /* 关联越库: CROSS_DOCK_ID */
    private java.lang.Long _crossDockId;
    
    /* 承运商信息: CARRIER_INFO */
    private java.lang.String _carrierInfo;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    

    public _ErpInvDrpDockAppointment(){
        // for debug
    }

    protected ErpInvDrpDockAppointment newInstance(){
        ErpInvDrpDockAppointment entity = new ErpInvDrpDockAppointment();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpInvDrpDockAppointment cloneInstance() {
        ErpInvDrpDockAppointment entity = newInstance();
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
      return "app.erp.drp.dao.entity.ErpInvDrpDockAppointment";
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
        
            case PROP_ID_warehouseId:
               return getWarehouseId();
        
            case PROP_ID_dockId:
               return getDockId();
        
            case PROP_ID_appointmentDate:
               return getAppointmentDate();
        
            case PROP_ID_slotStart:
               return getSlotStart();
        
            case PROP_ID_slotEnd:
               return getSlotEnd();
        
            case PROP_ID_crossDockId:
               return getCrossDockId();
        
            case PROP_ID_carrierInfo:
               return getCarrierInfo();
        
            case PROP_ID_status:
               return getStatus();
        
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
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
        
            case PROP_ID_warehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_warehouseId));
               }
               setWarehouseId(typedValue);
               break;
            }
        
            case PROP_ID_dockId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_dockId));
               }
               setDockId(typedValue);
               break;
            }
        
            case PROP_ID_appointmentDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_appointmentDate));
               }
               setAppointmentDate(typedValue);
               break;
            }
        
            case PROP_ID_slotStart:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_slotStart));
               }
               setSlotStart(typedValue);
               break;
            }
        
            case PROP_ID_slotEnd:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_slotEnd));
               }
               setSlotEnd(typedValue);
               break;
            }
        
            case PROP_ID_crossDockId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_crossDockId));
               }
               setCrossDockId(typedValue);
               break;
            }
        
            case PROP_ID_carrierInfo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_carrierInfo));
               }
               setCarrierInfo(typedValue);
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
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
        
            case PROP_ID_warehouseId:{
               onInitProp(propId);
               this._warehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_dockId:{
               onInitProp(propId);
               this._dockId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_appointmentDate:{
               onInitProp(propId);
               this._appointmentDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_slotStart:{
               onInitProp(propId);
               this._slotStart = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_slotEnd:{
               onInitProp(propId);
               this._slotEnd = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_crossDockId:{
               onInitProp(propId);
               this._crossDockId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_carrierInfo:{
               onInitProp(propId);
               this._carrierInfo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
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
     * 仓库: WAREHOUSE_ID
     */
    public final java.lang.Long getWarehouseId(){
         onPropGet(PROP_ID_warehouseId);
         return _warehouseId;
    }

    /**
     * 仓库: WAREHOUSE_ID
     */
    public final void setWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_warehouseId,value)){
            this._warehouseId = value;
            internalClearRefs(PROP_ID_warehouseId);
            
        }
    }
    
    /**
     * 月台: DOCK_ID
     */
    public final java.lang.Long getDockId(){
         onPropGet(PROP_ID_dockId);
         return _dockId;
    }

    /**
     * 月台: DOCK_ID
     */
    public final void setDockId(java.lang.Long value){
        if(onPropSet(PROP_ID_dockId,value)){
            this._dockId = value;
            internalClearRefs(PROP_ID_dockId);
            
        }
    }
    
    /**
     * 预约日期: APPOINTMENT_DATE
     */
    public final java.time.LocalDate getAppointmentDate(){
         onPropGet(PROP_ID_appointmentDate);
         return _appointmentDate;
    }

    /**
     * 预约日期: APPOINTMENT_DATE
     */
    public final void setAppointmentDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_appointmentDate,value)){
            this._appointmentDate = value;
            internalClearRefs(PROP_ID_appointmentDate);
            
        }
    }
    
    /**
     * 时间窗口开始: SLOT_START
     */
    public final java.sql.Timestamp getSlotStart(){
         onPropGet(PROP_ID_slotStart);
         return _slotStart;
    }

    /**
     * 时间窗口开始: SLOT_START
     */
    public final void setSlotStart(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_slotStart,value)){
            this._slotStart = value;
            internalClearRefs(PROP_ID_slotStart);
            
        }
    }
    
    /**
     * 时间窗口结束: SLOT_END
     */
    public final java.sql.Timestamp getSlotEnd(){
         onPropGet(PROP_ID_slotEnd);
         return _slotEnd;
    }

    /**
     * 时间窗口结束: SLOT_END
     */
    public final void setSlotEnd(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_slotEnd,value)){
            this._slotEnd = value;
            internalClearRefs(PROP_ID_slotEnd);
            
        }
    }
    
    /**
     * 关联越库: CROSS_DOCK_ID
     */
    public final java.lang.Long getCrossDockId(){
         onPropGet(PROP_ID_crossDockId);
         return _crossDockId;
    }

    /**
     * 关联越库: CROSS_DOCK_ID
     */
    public final void setCrossDockId(java.lang.Long value){
        if(onPropSet(PROP_ID_crossDockId,value)){
            this._crossDockId = value;
            internalClearRefs(PROP_ID_crossDockId);
            
        }
    }
    
    /**
     * 承运商信息: CARRIER_INFO
     */
    public final java.lang.String getCarrierInfo(){
         onPropGet(PROP_ID_carrierInfo);
         return _carrierInfo;
    }

    /**
     * 承运商信息: CARRIER_INFO
     */
    public final void setCarrierInfo(java.lang.String value){
        if(onPropSet(PROP_ID_carrierInfo,value)){
            this._carrierInfo = value;
            internalClearRefs(PROP_ID_carrierInfo);
            
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
     * 
     */
    public final app.erp.md.dao.entity.ErpMdWarehouse getWarehouse(){
       return (app.erp.md.dao.entity.ErpMdWarehouse)internalGetRefEntity(PROP_NAME_warehouse);
    }

    public final void setWarehouse(app.erp.md.dao.entity.ErpMdWarehouse refEntity){
   
           if(refEntity == null){
           
                   this.setWarehouseId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_warehouse, refEntity,()->{
           
                           this.setWarehouseId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.drp.dao.entity.ErpInvDrpCrossDock getDock(){
       return (app.erp.drp.dao.entity.ErpInvDrpCrossDock)internalGetRefEntity(PROP_NAME_dock);
    }

    public final void setDock(app.erp.drp.dao.entity.ErpInvDrpCrossDock refEntity){
   
           if(refEntity == null){
           
                   this.setDockId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_dock, refEntity,()->{
           
                           this.setDockId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.drp.dao.entity.ErpInvDrpCrossDock getCrossDock(){
       return (app.erp.drp.dao.entity.ErpInvDrpCrossDock)internalGetRefEntity(PROP_NAME_crossDock);
    }

    public final void setCrossDock(app.erp.drp.dao.entity.ErpInvDrpCrossDock refEntity){
   
           if(refEntity == null){
           
                   this.setCrossDockId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_crossDock, refEntity,()->{
           
                           this.setCrossDockId(refEntity.getId());
                       
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
