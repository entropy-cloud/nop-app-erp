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

import app.erp.hr.dao.entity.ErpHrShiftAssignment;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  排班分配: erp_hr_shift_assignment
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrShiftAssignment extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 员工: EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_employeeId = "employeeId";
    public static final int PROP_ID_employeeId = 3;
    
    /* 班次: SHIFT_ID BIGINT */
    public static final String PROP_NAME_shiftId = "shiftId";
    public static final int PROP_ID_shiftId = 4;
    
    /* 排班日期: ASSIGNMENT_DATE DATE */
    public static final String PROP_NAME_assignmentDate = "assignmentDate";
    public static final int PROP_ID_assignmentDate = 5;
    
    /* 实际签到时间: ACTUAL_START_TIME DATETIME */
    public static final String PROP_NAME_actualStartTime = "actualStartTime";
    public static final int PROP_ID_actualStartTime = 6;
    
    /* 实际签退时间: ACTUAL_END_TIME DATETIME */
    public static final String PROP_NAME_actualEndTime = "actualEndTime";
    public static final int PROP_ID_actualEndTime = 7;
    
    /* 是否缺勤: IS_ABSENT BOOLEAN */
    public static final String PROP_NAME_isAbsent = "isAbsent";
    public static final int PROP_ID_isAbsent = 8;
    
    /* 缺勤原因: ABSENCE_REASON VARCHAR */
    public static final String PROP_NAME_absenceReason = "absenceReason";
    public static final int PROP_ID_absenceReason = 9;
    
    /* 关联休假: LEAVE_REQUEST_ID BIGINT */
    public static final String PROP_NAME_leaveRequestId = "leaveRequestId";
    public static final int PROP_ID_leaveRequestId = 10;
    
    /* 关联调换: SWAP_REQUEST_ID BIGINT */
    public static final String PROP_NAME_swapRequestId = "swapRequestId";
    public static final int PROP_ID_swapRequestId = 11;
    
    /* 被替换排班: REPLACED_BY_ASSIGNMENT_ID BIGINT */
    public static final String PROP_NAME_replacedByAssignmentId = "replacedByAssignmentId";
    public static final int PROP_ID_replacedByAssignmentId = 12;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 13;
    
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
    public static final String PROP_NAME_shift = "shift";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_employeeId] = PROP_NAME_employeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeId, PROP_ID_employeeId);
      
          PROP_ID_TO_NAME[PROP_ID_shiftId] = PROP_NAME_shiftId;
          PROP_NAME_TO_ID.put(PROP_NAME_shiftId, PROP_ID_shiftId);
      
          PROP_ID_TO_NAME[PROP_ID_assignmentDate] = PROP_NAME_assignmentDate;
          PROP_NAME_TO_ID.put(PROP_NAME_assignmentDate, PROP_ID_assignmentDate);
      
          PROP_ID_TO_NAME[PROP_ID_actualStartTime] = PROP_NAME_actualStartTime;
          PROP_NAME_TO_ID.put(PROP_NAME_actualStartTime, PROP_ID_actualStartTime);
      
          PROP_ID_TO_NAME[PROP_ID_actualEndTime] = PROP_NAME_actualEndTime;
          PROP_NAME_TO_ID.put(PROP_NAME_actualEndTime, PROP_ID_actualEndTime);
      
          PROP_ID_TO_NAME[PROP_ID_isAbsent] = PROP_NAME_isAbsent;
          PROP_NAME_TO_ID.put(PROP_NAME_isAbsent, PROP_ID_isAbsent);
      
          PROP_ID_TO_NAME[PROP_ID_absenceReason] = PROP_NAME_absenceReason;
          PROP_NAME_TO_ID.put(PROP_NAME_absenceReason, PROP_ID_absenceReason);
      
          PROP_ID_TO_NAME[PROP_ID_leaveRequestId] = PROP_NAME_leaveRequestId;
          PROP_NAME_TO_ID.put(PROP_NAME_leaveRequestId, PROP_ID_leaveRequestId);
      
          PROP_ID_TO_NAME[PROP_ID_swapRequestId] = PROP_NAME_swapRequestId;
          PROP_NAME_TO_ID.put(PROP_NAME_swapRequestId, PROP_ID_swapRequestId);
      
          PROP_ID_TO_NAME[PROP_ID_replacedByAssignmentId] = PROP_NAME_replacedByAssignmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_replacedByAssignmentId, PROP_ID_replacedByAssignmentId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
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
    
    /* 员工: EMPLOYEE_ID */
    private java.lang.Long _employeeId;
    
    /* 班次: SHIFT_ID */
    private java.lang.Long _shiftId;
    
    /* 排班日期: ASSIGNMENT_DATE */
    private java.time.LocalDate _assignmentDate;
    
    /* 实际签到时间: ACTUAL_START_TIME */
    private java.time.LocalDateTime _actualStartTime;
    
    /* 实际签退时间: ACTUAL_END_TIME */
    private java.time.LocalDateTime _actualEndTime;
    
    /* 是否缺勤: IS_ABSENT */
    private java.lang.Boolean _isAbsent;
    
    /* 缺勤原因: ABSENCE_REASON */
    private java.lang.String _absenceReason;
    
    /* 关联休假: LEAVE_REQUEST_ID */
    private java.lang.Long _leaveRequestId;
    
    /* 关联调换: SWAP_REQUEST_ID */
    private java.lang.Long _swapRequestId;
    
    /* 被替换排班: REPLACED_BY_ASSIGNMENT_ID */
    private java.lang.Long _replacedByAssignmentId;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
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
    

    public _ErpHrShiftAssignment(){
        // for debug
    }

    protected ErpHrShiftAssignment newInstance(){
        ErpHrShiftAssignment entity = new ErpHrShiftAssignment();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrShiftAssignment cloneInstance() {
        ErpHrShiftAssignment entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrShiftAssignment";
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
        
            case PROP_ID_employeeId:
               return getEmployeeId();
        
            case PROP_ID_shiftId:
               return getShiftId();
        
            case PROP_ID_assignmentDate:
               return getAssignmentDate();
        
            case PROP_ID_actualStartTime:
               return getActualStartTime();
        
            case PROP_ID_actualEndTime:
               return getActualEndTime();
        
            case PROP_ID_isAbsent:
               return getIsAbsent();
        
            case PROP_ID_absenceReason:
               return getAbsenceReason();
        
            case PROP_ID_leaveRequestId:
               return getLeaveRequestId();
        
            case PROP_ID_swapRequestId:
               return getSwapRequestId();
        
            case PROP_ID_replacedByAssignmentId:
               return getReplacedByAssignmentId();
        
            case PROP_ID_status:
               return getStatus();
        
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
        
            case PROP_ID_employeeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_employeeId));
               }
               setEmployeeId(typedValue);
               break;
            }
        
            case PROP_ID_shiftId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_shiftId));
               }
               setShiftId(typedValue);
               break;
            }
        
            case PROP_ID_assignmentDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_assignmentDate));
               }
               setAssignmentDate(typedValue);
               break;
            }
        
            case PROP_ID_actualStartTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_actualStartTime));
               }
               setActualStartTime(typedValue);
               break;
            }
        
            case PROP_ID_actualEndTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_actualEndTime));
               }
               setActualEndTime(typedValue);
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
        
            case PROP_ID_absenceReason:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_absenceReason));
               }
               setAbsenceReason(typedValue);
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
        
            case PROP_ID_swapRequestId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_swapRequestId));
               }
               setSwapRequestId(typedValue);
               break;
            }
        
            case PROP_ID_replacedByAssignmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_replacedByAssignmentId));
               }
               setReplacedByAssignmentId(typedValue);
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
        
            case PROP_ID_employeeId:{
               onInitProp(propId);
               this._employeeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_shiftId:{
               onInitProp(propId);
               this._shiftId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_assignmentDate:{
               onInitProp(propId);
               this._assignmentDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_actualStartTime:{
               onInitProp(propId);
               this._actualStartTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_actualEndTime:{
               onInitProp(propId);
               this._actualEndTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_isAbsent:{
               onInitProp(propId);
               this._isAbsent = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_absenceReason:{
               onInitProp(propId);
               this._absenceReason = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_leaveRequestId:{
               onInitProp(propId);
               this._leaveRequestId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_swapRequestId:{
               onInitProp(propId);
               this._swapRequestId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_replacedByAssignmentId:{
               onInitProp(propId);
               this._replacedByAssignmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
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
     * 班次: SHIFT_ID
     */
    public final java.lang.Long getShiftId(){
         onPropGet(PROP_ID_shiftId);
         return _shiftId;
    }

    /**
     * 班次: SHIFT_ID
     */
    public final void setShiftId(java.lang.Long value){
        if(onPropSet(PROP_ID_shiftId,value)){
            this._shiftId = value;
            internalClearRefs(PROP_ID_shiftId);
            
        }
    }
    
    /**
     * 排班日期: ASSIGNMENT_DATE
     */
    public final java.time.LocalDate getAssignmentDate(){
         onPropGet(PROP_ID_assignmentDate);
         return _assignmentDate;
    }

    /**
     * 排班日期: ASSIGNMENT_DATE
     */
    public final void setAssignmentDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_assignmentDate,value)){
            this._assignmentDate = value;
            internalClearRefs(PROP_ID_assignmentDate);
            
        }
    }
    
    /**
     * 实际签到时间: ACTUAL_START_TIME
     */
    public final java.time.LocalDateTime getActualStartTime(){
         onPropGet(PROP_ID_actualStartTime);
         return _actualStartTime;
    }

    /**
     * 实际签到时间: ACTUAL_START_TIME
     */
    public final void setActualStartTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_actualStartTime,value)){
            this._actualStartTime = value;
            internalClearRefs(PROP_ID_actualStartTime);
            
        }
    }
    
    /**
     * 实际签退时间: ACTUAL_END_TIME
     */
    public final java.time.LocalDateTime getActualEndTime(){
         onPropGet(PROP_ID_actualEndTime);
         return _actualEndTime;
    }

    /**
     * 实际签退时间: ACTUAL_END_TIME
     */
    public final void setActualEndTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_actualEndTime,value)){
            this._actualEndTime = value;
            internalClearRefs(PROP_ID_actualEndTime);
            
        }
    }
    
    /**
     * 是否缺勤: IS_ABSENT
     */
    public final java.lang.Boolean getIsAbsent(){
         onPropGet(PROP_ID_isAbsent);
         return _isAbsent;
    }

    /**
     * 是否缺勤: IS_ABSENT
     */
    public final void setIsAbsent(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isAbsent,value)){
            this._isAbsent = value;
            internalClearRefs(PROP_ID_isAbsent);
            
        }
    }
    
    /**
     * 缺勤原因: ABSENCE_REASON
     */
    public final java.lang.String getAbsenceReason(){
         onPropGet(PROP_ID_absenceReason);
         return _absenceReason;
    }

    /**
     * 缺勤原因: ABSENCE_REASON
     */
    public final void setAbsenceReason(java.lang.String value){
        if(onPropSet(PROP_ID_absenceReason,value)){
            this._absenceReason = value;
            internalClearRefs(PROP_ID_absenceReason);
            
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
     * 关联调换: SWAP_REQUEST_ID
     */
    public final java.lang.Long getSwapRequestId(){
         onPropGet(PROP_ID_swapRequestId);
         return _swapRequestId;
    }

    /**
     * 关联调换: SWAP_REQUEST_ID
     */
    public final void setSwapRequestId(java.lang.Long value){
        if(onPropSet(PROP_ID_swapRequestId,value)){
            this._swapRequestId = value;
            internalClearRefs(PROP_ID_swapRequestId);
            
        }
    }
    
    /**
     * 被替换排班: REPLACED_BY_ASSIGNMENT_ID
     */
    public final java.lang.Long getReplacedByAssignmentId(){
         onPropGet(PROP_ID_replacedByAssignmentId);
         return _replacedByAssignmentId;
    }

    /**
     * 被替换排班: REPLACED_BY_ASSIGNMENT_ID
     */
    public final void setReplacedByAssignmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_replacedByAssignmentId,value)){
            this._replacedByAssignmentId = value;
            internalClearRefs(PROP_ID_replacedByAssignmentId);
            
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
    public final app.erp.hr.dao.entity.ErpHrShift getShift(){
       return (app.erp.hr.dao.entity.ErpHrShift)internalGetRefEntity(PROP_NAME_shift);
    }

    public final void setShift(app.erp.hr.dao.entity.ErpHrShift refEntity){
   
           if(refEntity == null){
           
                   this.setShiftId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_shift, refEntity,()->{
           
                           this.setShiftId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
