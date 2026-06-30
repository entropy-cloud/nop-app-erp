package app.erp.hr.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.hr.dao.entity.ErpHrAttendance;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  考勤记录: erp_hr_attendance
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrAttendance extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 员工: EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_employeeId = "employeeId";
    public static final int PROP_ID_employeeId = 2;
    
    /* 考勤日期: DATE DATE */
    public static final String PROP_NAME_date = "date";
    public static final int PROP_ID_date = 3;
    
    /* 签到时间: CLOCK_IN DATETIME */
    public static final String PROP_NAME_clockIn = "clockIn";
    public static final int PROP_ID_clockIn = 4;
    
    /* 签退时间: CLOCK_OUT DATETIME */
    public static final String PROP_NAME_clockOut = "clockOut";
    public static final int PROP_ID_clockOut = 5;
    
    /* 出勤时长: WORK_HOURS DECIMAL */
    public static final String PROP_NAME_workHours = "workHours";
    public static final int PROP_ID_workHours = 6;
    
    /* 迟到分钟数: LATE_MINUTES INTEGER */
    public static final String PROP_NAME_lateMinutes = "lateMinutes";
    public static final int PROP_ID_lateMinutes = 7;
    
    /* 早退分钟数: EARLY_LEAVE_MINUTES INTEGER */
    public static final String PROP_NAME_earlyLeaveMinutes = "earlyLeaveMinutes";
    public static final int PROP_ID_earlyLeaveMinutes = 8;
    
    /* 是否旷工: IS_ABSENT BOOLEAN */
    public static final String PROP_NAME_isAbsent = "isAbsent";
    public static final int PROP_ID_isAbsent = 9;
    
    /* 来源: SOURCE INTEGER */
    public static final String PROP_NAME_source = "source";
    public static final int PROP_ID_source = 10;
    
    /* 关联休假: LEAVE_REQUEST_ID BIGINT */
    public static final String PROP_NAME_leaveRequestId = "leaveRequestId";
    public static final int PROP_ID_leaveRequestId = 11;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 12;
    
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
    public static final String PROP_NAME_employee = "employee";
    
    /* relation:  */
    public static final String PROP_NAME_leaveRequest = "leaveRequest";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_employeeId] = PROP_NAME_employeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeId, PROP_ID_employeeId);
      
          PROP_ID_TO_NAME[PROP_ID_date] = PROP_NAME_date;
          PROP_NAME_TO_ID.put(PROP_NAME_date, PROP_ID_date);
      
          PROP_ID_TO_NAME[PROP_ID_clockIn] = PROP_NAME_clockIn;
          PROP_NAME_TO_ID.put(PROP_NAME_clockIn, PROP_ID_clockIn);
      
          PROP_ID_TO_NAME[PROP_ID_clockOut] = PROP_NAME_clockOut;
          PROP_NAME_TO_ID.put(PROP_NAME_clockOut, PROP_ID_clockOut);
      
          PROP_ID_TO_NAME[PROP_ID_workHours] = PROP_NAME_workHours;
          PROP_NAME_TO_ID.put(PROP_NAME_workHours, PROP_ID_workHours);
      
          PROP_ID_TO_NAME[PROP_ID_lateMinutes] = PROP_NAME_lateMinutes;
          PROP_NAME_TO_ID.put(PROP_NAME_lateMinutes, PROP_ID_lateMinutes);
      
          PROP_ID_TO_NAME[PROP_ID_earlyLeaveMinutes] = PROP_NAME_earlyLeaveMinutes;
          PROP_NAME_TO_ID.put(PROP_NAME_earlyLeaveMinutes, PROP_ID_earlyLeaveMinutes);
      
          PROP_ID_TO_NAME[PROP_ID_isAbsent] = PROP_NAME_isAbsent;
          PROP_NAME_TO_ID.put(PROP_NAME_isAbsent, PROP_ID_isAbsent);
      
          PROP_ID_TO_NAME[PROP_ID_source] = PROP_NAME_source;
          PROP_NAME_TO_ID.put(PROP_NAME_source, PROP_ID_source);
      
          PROP_ID_TO_NAME[PROP_ID_leaveRequestId] = PROP_NAME_leaveRequestId;
          PROP_NAME_TO_ID.put(PROP_NAME_leaveRequestId, PROP_ID_leaveRequestId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
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
    
    /* 员工: EMPLOYEE_ID */
    private java.lang.Long _employeeId;
    
    /* 考勤日期: DATE */
    private java.time.LocalDate _date;
    
    /* 签到时间: CLOCK_IN */
    private java.time.LocalDateTime _clockIn;
    
    /* 签退时间: CLOCK_OUT */
    private java.time.LocalDateTime _clockOut;
    
    /* 出勤时长: WORK_HOURS */
    private java.math.BigDecimal _workHours;
    
    /* 迟到分钟数: LATE_MINUTES */
    private java.lang.Integer _lateMinutes;
    
    /* 早退分钟数: EARLY_LEAVE_MINUTES */
    private java.lang.Integer _earlyLeaveMinutes;
    
    /* 是否旷工: IS_ABSENT */
    private java.lang.Boolean _isAbsent;
    
    /* 来源: SOURCE */
    private java.lang.Integer _source;
    
    /* 关联休假: LEAVE_REQUEST_ID */
    private java.lang.Long _leaveRequestId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
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
    

    public _ErpHrAttendance(){
        // for debug
    }

    protected ErpHrAttendance newInstance(){
        ErpHrAttendance entity = new ErpHrAttendance();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrAttendance cloneInstance() {
        ErpHrAttendance entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrAttendance";
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
        
            case PROP_ID_employeeId:
               return getEmployeeId();
        
            case PROP_ID_date:
               return getDate();
        
            case PROP_ID_clockIn:
               return getClockIn();
        
            case PROP_ID_clockOut:
               return getClockOut();
        
            case PROP_ID_workHours:
               return getWorkHours();
        
            case PROP_ID_lateMinutes:
               return getLateMinutes();
        
            case PROP_ID_earlyLeaveMinutes:
               return getEarlyLeaveMinutes();
        
            case PROP_ID_isAbsent:
               return getIsAbsent();
        
            case PROP_ID_source:
               return getSource();
        
            case PROP_ID_leaveRequestId:
               return getLeaveRequestId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
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
        
            case PROP_ID_employeeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_employeeId));
               }
               setEmployeeId(typedValue);
               break;
            }
        
            case PROP_ID_date:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_date));
               }
               setDate(typedValue);
               break;
            }
        
            case PROP_ID_clockIn:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_clockIn));
               }
               setClockIn(typedValue);
               break;
            }
        
            case PROP_ID_clockOut:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_clockOut));
               }
               setClockOut(typedValue);
               break;
            }
        
            case PROP_ID_workHours:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_workHours));
               }
               setWorkHours(typedValue);
               break;
            }
        
            case PROP_ID_lateMinutes:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lateMinutes));
               }
               setLateMinutes(typedValue);
               break;
            }
        
            case PROP_ID_earlyLeaveMinutes:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_earlyLeaveMinutes));
               }
               setEarlyLeaveMinutes(typedValue);
               break;
            }
        
            case PROP_ID_isAbsent:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isAbsent));
               }
               setIsAbsent(typedValue);
               break;
            }
        
            case PROP_ID_source:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_source));
               }
               setSource(typedValue);
               break;
            }
        
            case PROP_ID_leaveRequestId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_leaveRequestId));
               }
               setLeaveRequestId(typedValue);
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
        
            case PROP_ID_employeeId:{
               onInitProp(propId);
               this._employeeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_date:{
               onInitProp(propId);
               this._date = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_clockIn:{
               onInitProp(propId);
               this._clockIn = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_clockOut:{
               onInitProp(propId);
               this._clockOut = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_workHours:{
               onInitProp(propId);
               this._workHours = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_lateMinutes:{
               onInitProp(propId);
               this._lateMinutes = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_earlyLeaveMinutes:{
               onInitProp(propId);
               this._earlyLeaveMinutes = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_isAbsent:{
               onInitProp(propId);
               this._isAbsent = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_source:{
               onInitProp(propId);
               this._source = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_leaveRequestId:{
               onInitProp(propId);
               this._leaveRequestId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
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
     * 员工: EMPLOYEE_ID
     */
    public final java.lang.Long getEmployeeId(){
         onPropGet(PROP_ID_employeeId);
         return _employeeId;
    }

    /**
     * 员工: EMPLOYEE_ID
     */
    public final void setEmployeeId(java.lang.Long value){
        if(onPropSet(PROP_ID_employeeId,value)){
            this._employeeId = value;
            internalClearRefs(PROP_ID_employeeId);
            
        }
    }
    
    /**
     * 考勤日期: DATE
     */
    public final java.time.LocalDate getDate(){
         onPropGet(PROP_ID_date);
         return _date;
    }

    /**
     * 考勤日期: DATE
     */
    public final void setDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_date,value)){
            this._date = value;
            internalClearRefs(PROP_ID_date);
            
        }
    }
    
    /**
     * 签到时间: CLOCK_IN
     */
    public final java.time.LocalDateTime getClockIn(){
         onPropGet(PROP_ID_clockIn);
         return _clockIn;
    }

    /**
     * 签到时间: CLOCK_IN
     */
    public final void setClockIn(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_clockIn,value)){
            this._clockIn = value;
            internalClearRefs(PROP_ID_clockIn);
            
        }
    }
    
    /**
     * 签退时间: CLOCK_OUT
     */
    public final java.time.LocalDateTime getClockOut(){
         onPropGet(PROP_ID_clockOut);
         return _clockOut;
    }

    /**
     * 签退时间: CLOCK_OUT
     */
    public final void setClockOut(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_clockOut,value)){
            this._clockOut = value;
            internalClearRefs(PROP_ID_clockOut);
            
        }
    }
    
    /**
     * 出勤时长: WORK_HOURS
     */
    public final java.math.BigDecimal getWorkHours(){
         onPropGet(PROP_ID_workHours);
         return _workHours;
    }

    /**
     * 出勤时长: WORK_HOURS
     */
    public final void setWorkHours(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_workHours,value)){
            this._workHours = value;
            internalClearRefs(PROP_ID_workHours);
            
        }
    }
    
    /**
     * 迟到分钟数: LATE_MINUTES
     */
    public final java.lang.Integer getLateMinutes(){
         onPropGet(PROP_ID_lateMinutes);
         return _lateMinutes;
    }

    /**
     * 迟到分钟数: LATE_MINUTES
     */
    public final void setLateMinutes(java.lang.Integer value){
        if(onPropSet(PROP_ID_lateMinutes,value)){
            this._lateMinutes = value;
            internalClearRefs(PROP_ID_lateMinutes);
            
        }
    }
    
    /**
     * 早退分钟数: EARLY_LEAVE_MINUTES
     */
    public final java.lang.Integer getEarlyLeaveMinutes(){
         onPropGet(PROP_ID_earlyLeaveMinutes);
         return _earlyLeaveMinutes;
    }

    /**
     * 早退分钟数: EARLY_LEAVE_MINUTES
     */
    public final void setEarlyLeaveMinutes(java.lang.Integer value){
        if(onPropSet(PROP_ID_earlyLeaveMinutes,value)){
            this._earlyLeaveMinutes = value;
            internalClearRefs(PROP_ID_earlyLeaveMinutes);
            
        }
    }
    
    /**
     * 是否旷工: IS_ABSENT
     */
    public final java.lang.Boolean getIsAbsent(){
         onPropGet(PROP_ID_isAbsent);
         return _isAbsent;
    }

    /**
     * 是否旷工: IS_ABSENT
     */
    public final void setIsAbsent(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isAbsent,value)){
            this._isAbsent = value;
            internalClearRefs(PROP_ID_isAbsent);
            
        }
    }
    
    /**
     * 来源: SOURCE
     */
    public final java.lang.Integer getSource(){
         onPropGet(PROP_ID_source);
         return _source;
    }

    /**
     * 来源: SOURCE
     */
    public final void setSource(java.lang.Integer value){
        if(onPropSet(PROP_ID_source,value)){
            this._source = value;
            internalClearRefs(PROP_ID_source);
            
        }
    }
    
    /**
     * 关联休假: LEAVE_REQUEST_ID
     */
    public final java.lang.Long getLeaveRequestId(){
         onPropGet(PROP_ID_leaveRequestId);
         return _leaveRequestId;
    }

    /**
     * 关联休假: LEAVE_REQUEST_ID
     */
    public final void setLeaveRequestId(java.lang.Long value){
        if(onPropSet(PROP_ID_leaveRequestId,value)){
            this._leaveRequestId = value;
            internalClearRefs(PROP_ID_leaveRequestId);
            
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
    public final app.erp.hr.dao.entity.ErpHrEmployee getEmployee(){
       return (app.erp.hr.dao.entity.ErpHrEmployee)internalGetRefEntity(PROP_NAME_employee);
    }

    public final void setEmployee(app.erp.hr.dao.entity.ErpHrEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setEmployeeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_employee, refEntity,()->{
           
                           this.setEmployeeId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrLeaveRequest getLeaveRequest(){
       return (app.erp.hr.dao.entity.ErpHrLeaveRequest)internalGetRefEntity(PROP_NAME_leaveRequest);
    }

    public final void setLeaveRequest(app.erp.hr.dao.entity.ErpHrLeaveRequest refEntity){
   
           if(refEntity == null){
           
                   this.setLeaveRequestId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_leaveRequest, refEntity,()->{
           
                           this.setLeaveRequestId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
