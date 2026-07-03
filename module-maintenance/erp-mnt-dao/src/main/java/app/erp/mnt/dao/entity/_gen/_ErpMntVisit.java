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

import app.erp.mnt.dao.entity.ErpMntVisit;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  维护访问: erp_mnt_visit
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMntVisit extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 访问编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 维护计划ID: SCHEDULE_ID BIGINT */
    public static final String PROP_NAME_scheduleId = "scheduleId";
    public static final int PROP_ID_scheduleId = 3;
    
    /* 设备ID: EQUIPMENT_ID BIGINT */
    public static final String PROP_NAME_equipmentId = "equipmentId";
    public static final int PROP_ID_equipmentId = 4;
    
    /* 访问日期: VISIT_DATE DATE */
    public static final String PROP_NAME_visitDate = "visitDate";
    public static final int PROP_ID_visitDate = 5;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 6;
    
    /* 指派人: ASSIGNED_TO BIGINT */
    public static final String PROP_NAME_assignedTo = "assignedTo";
    public static final int PROP_ID_assignedTo = 7;
    
    /* 完成人: COMPLETED_BY BIGINT */
    public static final String PROP_NAME_completedBy = "completedBy";
    public static final int PROP_ID_completedBy = 8;
    
    /* 完成时间: COMPLETED_AT DATETIME */
    public static final String PROP_NAME_completedAt = "completedAt";
    public static final int PROP_ID_completedAt = 9;
    
    /* 开始时间: START_TIME DATETIME */
    public static final String PROP_NAME_startTime = "startTime";
    public static final int PROP_ID_startTime = 10;
    
    /* 结束时间: END_TIME DATETIME */
    public static final String PROP_NAME_endTime = "endTime";
    public static final int PROP_ID_endTime = 11;
    
    /* 总分钟数: TOTAL_MINUTES DECIMAL */
    public static final String PROP_NAME_totalMinutes = "totalMinutes";
    public static final int PROP_ID_totalMinutes = 12;
    
    /* 维护类型: VISIT_TYPE VARCHAR */
    public static final String PROP_NAME_visitType = "visitType";
    public static final int PROP_ID_visitType = 13;
    
    /* 执行结果: RESULT VARCHAR */
    public static final String PROP_NAME_result = "result";
    public static final int PROP_ID_result = 14;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 15;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 16;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 17;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 18;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 19;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 20;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 21;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 22;
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 23;
    
    /* 是否已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 24;
    
    /* 过账时间: POSTED_AT TIMESTAMP */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 25;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 26;
    

    private static int _PROP_ID_BOUND = 27;

    
    /* relation:  */
    public static final String PROP_NAME_schedule = "schedule";
    
    /* relation:  */
    public static final String PROP_NAME_equipment = "equipment";
    
    /* relation:  */
    public static final String PROP_NAME_tasks = "tasks";
    
    /* relation:  */
    public static final String PROP_NAME_sparePartUsages = "sparePartUsages";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[27];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_scheduleId] = PROP_NAME_scheduleId;
          PROP_NAME_TO_ID.put(PROP_NAME_scheduleId, PROP_ID_scheduleId);
      
          PROP_ID_TO_NAME[PROP_ID_equipmentId] = PROP_NAME_equipmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_equipmentId, PROP_ID_equipmentId);
      
          PROP_ID_TO_NAME[PROP_ID_visitDate] = PROP_NAME_visitDate;
          PROP_NAME_TO_ID.put(PROP_NAME_visitDate, PROP_ID_visitDate);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_assignedTo] = PROP_NAME_assignedTo;
          PROP_NAME_TO_ID.put(PROP_NAME_assignedTo, PROP_ID_assignedTo);
      
          PROP_ID_TO_NAME[PROP_ID_completedBy] = PROP_NAME_completedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_completedBy, PROP_ID_completedBy);
      
          PROP_ID_TO_NAME[PROP_ID_completedAt] = PROP_NAME_completedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_completedAt, PROP_ID_completedAt);
      
          PROP_ID_TO_NAME[PROP_ID_startTime] = PROP_NAME_startTime;
          PROP_NAME_TO_ID.put(PROP_NAME_startTime, PROP_ID_startTime);
      
          PROP_ID_TO_NAME[PROP_ID_endTime] = PROP_NAME_endTime;
          PROP_NAME_TO_ID.put(PROP_NAME_endTime, PROP_ID_endTime);
      
          PROP_ID_TO_NAME[PROP_ID_totalMinutes] = PROP_NAME_totalMinutes;
          PROP_NAME_TO_ID.put(PROP_NAME_totalMinutes, PROP_ID_totalMinutes);
      
          PROP_ID_TO_NAME[PROP_ID_visitType] = PROP_NAME_visitType;
          PROP_NAME_TO_ID.put(PROP_NAME_visitType, PROP_ID_visitType);
      
          PROP_ID_TO_NAME[PROP_ID_result] = PROP_NAME_result;
          PROP_NAME_TO_ID.put(PROP_NAME_result, PROP_ID_result);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 访问编码: CODE */
    private java.lang.String _code;
    
    /* 维护计划ID: SCHEDULE_ID */
    private java.lang.Long _scheduleId;
    
    /* 设备ID: EQUIPMENT_ID */
    private java.lang.Long _equipmentId;
    
    /* 访问日期: VISIT_DATE */
    private java.time.LocalDate _visitDate;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 指派人: ASSIGNED_TO */
    private java.lang.Long _assignedTo;
    
    /* 完成人: COMPLETED_BY */
    private java.lang.Long _completedBy;
    
    /* 完成时间: COMPLETED_AT */
    private java.time.LocalDateTime _completedAt;
    
    /* 开始时间: START_TIME */
    private java.time.LocalDateTime _startTime;
    
    /* 结束时间: END_TIME */
    private java.time.LocalDateTime _endTime;
    
    /* 总分钟数: TOTAL_MINUTES */
    private java.math.BigDecimal _totalMinutes;
    
    /* 维护类型: VISIT_TYPE */
    private java.lang.String _visitType;
    
    /* 执行结果: RESULT */
    private java.lang.String _result;
    
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
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 是否已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账时间: POSTED_AT */
    private java.sql.Timestamp _postedAt;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    

    public _ErpMntVisit(){
        // for debug
    }

    protected ErpMntVisit newInstance(){
        ErpMntVisit entity = new ErpMntVisit();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMntVisit cloneInstance() {
        ErpMntVisit entity = newInstance();
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
      return "app.erp.mnt.dao.entity.ErpMntVisit";
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
        
            case PROP_ID_scheduleId:
               return getScheduleId();
        
            case PROP_ID_equipmentId:
               return getEquipmentId();
        
            case PROP_ID_visitDate:
               return getVisitDate();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_assignedTo:
               return getAssignedTo();
        
            case PROP_ID_completedBy:
               return getCompletedBy();
        
            case PROP_ID_completedAt:
               return getCompletedAt();
        
            case PROP_ID_startTime:
               return getStartTime();
        
            case PROP_ID_endTime:
               return getEndTime();
        
            case PROP_ID_totalMinutes:
               return getTotalMinutes();
        
            case PROP_ID_visitType:
               return getVisitType();
        
            case PROP_ID_result:
               return getResult();
        
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
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
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
        
            case PROP_ID_scheduleId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_scheduleId));
               }
               setScheduleId(typedValue);
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
        
            case PROP_ID_visitDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_visitDate));
               }
               setVisitDate(typedValue);
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
        
            case PROP_ID_assignedTo:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_assignedTo));
               }
               setAssignedTo(typedValue);
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
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_completedAt));
               }
               setCompletedAt(typedValue);
               break;
            }
        
            case PROP_ID_startTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_startTime));
               }
               setStartTime(typedValue);
               break;
            }
        
            case PROP_ID_endTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_endTime));
               }
               setEndTime(typedValue);
               break;
            }
        
            case PROP_ID_totalMinutes:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalMinutes));
               }
               setTotalMinutes(typedValue);
               break;
            }
        
            case PROP_ID_visitType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_visitType));
               }
               setVisitType(typedValue);
               break;
            }
        
            case PROP_ID_result:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_result));
               }
               setResult(typedValue);
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
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
               break;
            }
        
            case PROP_ID_posted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_posted));
               }
               setPosted(typedValue);
               break;
            }
        
            case PROP_ID_postedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_postedAt));
               }
               setPostedAt(typedValue);
               break;
            }
        
            case PROP_ID_postedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_postedBy));
               }
               setPostedBy(typedValue);
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
        
            case PROP_ID_scheduleId:{
               onInitProp(propId);
               this._scheduleId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_equipmentId:{
               onInitProp(propId);
               this._equipmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_visitDate:{
               onInitProp(propId);
               this._visitDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_assignedTo:{
               onInitProp(propId);
               this._assignedTo = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_completedBy:{
               onInitProp(propId);
               this._completedBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_completedAt:{
               onInitProp(propId);
               this._completedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_startTime:{
               onInitProp(propId);
               this._startTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_endTime:{
               onInitProp(propId);
               this._endTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_totalMinutes:{
               onInitProp(propId);
               this._totalMinutes = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_visitType:{
               onInitProp(propId);
               this._visitType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_result:{
               onInitProp(propId);
               this._result = (java.lang.String)value;
               
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
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
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
     * 访问编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 访问编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 维护计划ID: SCHEDULE_ID
     */
    public final java.lang.Long getScheduleId(){
         onPropGet(PROP_ID_scheduleId);
         return _scheduleId;
    }

    /**
     * 维护计划ID: SCHEDULE_ID
     */
    public final void setScheduleId(java.lang.Long value){
        if(onPropSet(PROP_ID_scheduleId,value)){
            this._scheduleId = value;
            internalClearRefs(PROP_ID_scheduleId);
            
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
     * 访问日期: VISIT_DATE
     */
    public final java.time.LocalDate getVisitDate(){
         onPropGet(PROP_ID_visitDate);
         return _visitDate;
    }

    /**
     * 访问日期: VISIT_DATE
     */
    public final void setVisitDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_visitDate,value)){
            this._visitDate = value;
            internalClearRefs(PROP_ID_visitDate);
            
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
    public final java.time.LocalDateTime getCompletedAt(){
         onPropGet(PROP_ID_completedAt);
         return _completedAt;
    }

    /**
     * 完成时间: COMPLETED_AT
     */
    public final void setCompletedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_completedAt,value)){
            this._completedAt = value;
            internalClearRefs(PROP_ID_completedAt);
            
        }
    }
    
    /**
     * 开始时间: START_TIME
     */
    public final java.time.LocalDateTime getStartTime(){
         onPropGet(PROP_ID_startTime);
         return _startTime;
    }

    /**
     * 开始时间: START_TIME
     */
    public final void setStartTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_startTime,value)){
            this._startTime = value;
            internalClearRefs(PROP_ID_startTime);
            
        }
    }
    
    /**
     * 结束时间: END_TIME
     */
    public final java.time.LocalDateTime getEndTime(){
         onPropGet(PROP_ID_endTime);
         return _endTime;
    }

    /**
     * 结束时间: END_TIME
     */
    public final void setEndTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_endTime,value)){
            this._endTime = value;
            internalClearRefs(PROP_ID_endTime);
            
        }
    }
    
    /**
     * 总分钟数: TOTAL_MINUTES
     */
    public final java.math.BigDecimal getTotalMinutes(){
         onPropGet(PROP_ID_totalMinutes);
         return _totalMinutes;
    }

    /**
     * 总分钟数: TOTAL_MINUTES
     */
    public final void setTotalMinutes(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalMinutes,value)){
            this._totalMinutes = value;
            internalClearRefs(PROP_ID_totalMinutes);
            
        }
    }
    
    /**
     * 维护类型: VISIT_TYPE
     */
    public final java.lang.String getVisitType(){
         onPropGet(PROP_ID_visitType);
         return _visitType;
    }

    /**
     * 维护类型: VISIT_TYPE
     */
    public final void setVisitType(java.lang.String value){
        if(onPropSet(PROP_ID_visitType,value)){
            this._visitType = value;
            internalClearRefs(PROP_ID_visitType);
            
        }
    }
    
    /**
     * 执行结果: RESULT
     */
    public final java.lang.String getResult(){
         onPropGet(PROP_ID_result);
         return _result;
    }

    /**
     * 执行结果: RESULT
     */
    public final void setResult(java.lang.String value){
        if(onPropSet(PROP_ID_result,value)){
            this._result = value;
            internalClearRefs(PROP_ID_result);
            
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
     * 业务日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 业务日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
        }
    }
    
    /**
     * 是否已过账: POSTED
     */
    public final java.lang.Boolean getPosted(){
         onPropGet(PROP_ID_posted);
         return _posted;
    }

    /**
     * 是否已过账: POSTED
     */
    public final void setPosted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_posted,value)){
            this._posted = value;
            internalClearRefs(PROP_ID_posted);
            
        }
    }
    
    /**
     * 过账时间: POSTED_AT
     */
    public final java.sql.Timestamp getPostedAt(){
         onPropGet(PROP_ID_postedAt);
         return _postedAt;
    }

    /**
     * 过账时间: POSTED_AT
     */
    public final void setPostedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_postedAt,value)){
            this._postedAt = value;
            internalClearRefs(PROP_ID_postedAt);
            
        }
    }
    
    /**
     * 过账人: POSTED_BY
     */
    public final java.lang.String getPostedBy(){
         onPropGet(PROP_ID_postedBy);
         return _postedBy;
    }

    /**
     * 过账人: POSTED_BY
     */
    public final void setPostedBy(java.lang.String value){
        if(onPropSet(PROP_ID_postedBy,value)){
            this._postedBy = value;
            internalClearRefs(PROP_ID_postedBy);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.mnt.dao.entity.ErpMntSchedule getSchedule(){
       return (app.erp.mnt.dao.entity.ErpMntSchedule)internalGetRefEntity(PROP_NAME_schedule);
    }

    public final void setSchedule(app.erp.mnt.dao.entity.ErpMntSchedule refEntity){
   
           if(refEntity == null){
           
                   this.setScheduleId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_schedule, refEntity,()->{
           
                           this.setScheduleId(refEntity.getId());
                       
           });
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
       
    private final OrmEntitySet<app.erp.mnt.dao.entity.ErpMntVisitTask> _tasks = new OrmEntitySet<>(this, PROP_NAME_tasks,
        null, null,app.erp.mnt.dao.entity.ErpMntVisitTask.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mnt.dao.entity.ErpMntVisitTask> getTasks(){
       return _tasks;
    }
       
    private final OrmEntitySet<app.erp.mnt.dao.entity.ErpMntSparePartUsage> _sparePartUsages = new OrmEntitySet<>(this, PROP_NAME_sparePartUsages,
        null, null,app.erp.mnt.dao.entity.ErpMntSparePartUsage.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mnt.dao.entity.ErpMntSparePartUsage> getSparePartUsages(){
       return _sparePartUsages;
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
