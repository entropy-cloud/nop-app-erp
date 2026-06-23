package app.erp.prj.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.prj.dao.entity.ErpPrjTask;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  任务: erp_prj_task
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpPrjTask extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 项目: PROJECT_ID BIGINT */
    public static final String PROP_NAME_projectId = "projectId";
    public static final int PROP_ID_projectId = 2;
    
    /* 父任务: PARENT_TASK_ID BIGINT */
    public static final String PROP_NAME_parentTaskId = "parentTaskId";
    public static final int PROP_ID_parentTaskId = 3;
    
    /* 任务标题: TITLE VARCHAR */
    public static final String PROP_NAME_title = "title";
    public static final int PROP_ID_title = 4;
    
    /* 负责人: ASSIGNEE_ID BIGINT */
    public static final String PROP_NAME_assigneeId = "assigneeId";
    public static final int PROP_ID_assigneeId = 5;
    
    /* 计划开始日期: PLANNED_START_DATE DATE */
    public static final String PROP_NAME_plannedStartDate = "plannedStartDate";
    public static final int PROP_ID_plannedStartDate = 6;
    
    /* 计划结束日期: PLANNED_END_DATE DATE */
    public static final String PROP_NAME_plannedEndDate = "plannedEndDate";
    public static final int PROP_ID_plannedEndDate = 7;
    
    /* 实际开始日期: ACTUAL_START_DATE DATE */
    public static final String PROP_NAME_actualStartDate = "actualStartDate";
    public static final int PROP_ID_actualStartDate = 8;
    
    /* 实际结束日期: ACTUAL_END_DATE DATE */
    public static final String PROP_NAME_actualEndDate = "actualEndDate";
    public static final int PROP_ID_actualEndDate = 9;
    
    /* 预估工时: ESTIMATED_HOURS DECIMAL */
    public static final String PROP_NAME_estimatedHours = "estimatedHours";
    public static final int PROP_ID_estimatedHours = 10;
    
    /* 实际工时: ACTUAL_HOURS DECIMAL */
    public static final String PROP_NAME_actualHours = "actualHours";
    public static final int PROP_ID_actualHours = 11;
    
    /* 依赖任务: DEPENDS_ON_ID BIGINT */
    public static final String PROP_NAME_dependsOnId = "dependsOnId";
    public static final int PROP_ID_dependsOnId = 12;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 13;
    
    /* 优先级: PRIORITY INTEGER */
    public static final String PROP_NAME_priority = "priority";
    public static final int PROP_ID_priority = 14;
    
    /* 排序号: SORT_NUM INTEGER */
    public static final String PROP_NAME_sortNum = "sortNum";
    public static final int PROP_ID_sortNum = 15;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 16;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 17;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 18;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 19;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 20;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 21;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 22;
    

    private static int _PROP_ID_BOUND = 23;

    
    /* relation:  */
    public static final String PROP_NAME_project = "project";
    
    /* relation:  */
    public static final String PROP_NAME_parentTask = "parentTask";
    
    /* relation:  */
    public static final String PROP_NAME_assignee = "assignee";
    
    /* relation:  */
    public static final String PROP_NAME_dependsOn = "dependsOn";
    
    /* relation:  */
    public static final String PROP_NAME_childTasks = "childTasks";
    
    /* relation:  */
    public static final String PROP_NAME_timesheets = "timesheets";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_projectId] = PROP_NAME_projectId;
          PROP_NAME_TO_ID.put(PROP_NAME_projectId, PROP_ID_projectId);
      
          PROP_ID_TO_NAME[PROP_ID_parentTaskId] = PROP_NAME_parentTaskId;
          PROP_NAME_TO_ID.put(PROP_NAME_parentTaskId, PROP_ID_parentTaskId);
      
          PROP_ID_TO_NAME[PROP_ID_title] = PROP_NAME_title;
          PROP_NAME_TO_ID.put(PROP_NAME_title, PROP_ID_title);
      
          PROP_ID_TO_NAME[PROP_ID_assigneeId] = PROP_NAME_assigneeId;
          PROP_NAME_TO_ID.put(PROP_NAME_assigneeId, PROP_ID_assigneeId);
      
          PROP_ID_TO_NAME[PROP_ID_plannedStartDate] = PROP_NAME_plannedStartDate;
          PROP_NAME_TO_ID.put(PROP_NAME_plannedStartDate, PROP_ID_plannedStartDate);
      
          PROP_ID_TO_NAME[PROP_ID_plannedEndDate] = PROP_NAME_plannedEndDate;
          PROP_NAME_TO_ID.put(PROP_NAME_plannedEndDate, PROP_ID_plannedEndDate);
      
          PROP_ID_TO_NAME[PROP_ID_actualStartDate] = PROP_NAME_actualStartDate;
          PROP_NAME_TO_ID.put(PROP_NAME_actualStartDate, PROP_ID_actualStartDate);
      
          PROP_ID_TO_NAME[PROP_ID_actualEndDate] = PROP_NAME_actualEndDate;
          PROP_NAME_TO_ID.put(PROP_NAME_actualEndDate, PROP_ID_actualEndDate);
      
          PROP_ID_TO_NAME[PROP_ID_estimatedHours] = PROP_NAME_estimatedHours;
          PROP_NAME_TO_ID.put(PROP_NAME_estimatedHours, PROP_ID_estimatedHours);
      
          PROP_ID_TO_NAME[PROP_ID_actualHours] = PROP_NAME_actualHours;
          PROP_NAME_TO_ID.put(PROP_NAME_actualHours, PROP_ID_actualHours);
      
          PROP_ID_TO_NAME[PROP_ID_dependsOnId] = PROP_NAME_dependsOnId;
          PROP_NAME_TO_ID.put(PROP_NAME_dependsOnId, PROP_ID_dependsOnId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_priority] = PROP_NAME_priority;
          PROP_NAME_TO_ID.put(PROP_NAME_priority, PROP_ID_priority);
      
          PROP_ID_TO_NAME[PROP_ID_sortNum] = PROP_NAME_sortNum;
          PROP_NAME_TO_ID.put(PROP_NAME_sortNum, PROP_ID_sortNum);
      
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
    
    /* 项目: PROJECT_ID */
    private java.lang.Long _projectId;
    
    /* 父任务: PARENT_TASK_ID */
    private java.lang.Long _parentTaskId;
    
    /* 任务标题: TITLE */
    private java.lang.String _title;
    
    /* 负责人: ASSIGNEE_ID */
    private java.lang.Long _assigneeId;
    
    /* 计划开始日期: PLANNED_START_DATE */
    private java.time.LocalDate _plannedStartDate;
    
    /* 计划结束日期: PLANNED_END_DATE */
    private java.time.LocalDate _plannedEndDate;
    
    /* 实际开始日期: ACTUAL_START_DATE */
    private java.time.LocalDate _actualStartDate;
    
    /* 实际结束日期: ACTUAL_END_DATE */
    private java.time.LocalDate _actualEndDate;
    
    /* 预估工时: ESTIMATED_HOURS */
    private java.lang.String _estimatedHours;
    
    /* 实际工时: ACTUAL_HOURS */
    private java.lang.String _actualHours;
    
    /* 依赖任务: DEPENDS_ON_ID */
    private java.lang.Long _dependsOnId;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 优先级: PRIORITY */
    private java.lang.Integer _priority;
    
    /* 排序号: SORT_NUM */
    private java.lang.Integer _sortNum;
    
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
    

    public _ErpPrjTask(){
        // for debug
    }

    protected ErpPrjTask newInstance(){
        ErpPrjTask entity = new ErpPrjTask();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpPrjTask cloneInstance() {
        ErpPrjTask entity = newInstance();
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
      return "app.erp.prj.dao.entity.ErpPrjTask";
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
        
            case PROP_ID_projectId:
               return getProjectId();
        
            case PROP_ID_parentTaskId:
               return getParentTaskId();
        
            case PROP_ID_title:
               return getTitle();
        
            case PROP_ID_assigneeId:
               return getAssigneeId();
        
            case PROP_ID_plannedStartDate:
               return getPlannedStartDate();
        
            case PROP_ID_plannedEndDate:
               return getPlannedEndDate();
        
            case PROP_ID_actualStartDate:
               return getActualStartDate();
        
            case PROP_ID_actualEndDate:
               return getActualEndDate();
        
            case PROP_ID_estimatedHours:
               return getEstimatedHours();
        
            case PROP_ID_actualHours:
               return getActualHours();
        
            case PROP_ID_dependsOnId:
               return getDependsOnId();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_priority:
               return getPriority();
        
            case PROP_ID_sortNum:
               return getSortNum();
        
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
        
            case PROP_ID_projectId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_projectId));
               }
               setProjectId(typedValue);
               break;
            }
        
            case PROP_ID_parentTaskId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_parentTaskId));
               }
               setParentTaskId(typedValue);
               break;
            }
        
            case PROP_ID_title:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_title));
               }
               setTitle(typedValue);
               break;
            }
        
            case PROP_ID_assigneeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_assigneeId));
               }
               setAssigneeId(typedValue);
               break;
            }
        
            case PROP_ID_plannedStartDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_plannedStartDate));
               }
               setPlannedStartDate(typedValue);
               break;
            }
        
            case PROP_ID_plannedEndDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_plannedEndDate));
               }
               setPlannedEndDate(typedValue);
               break;
            }
        
            case PROP_ID_actualStartDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_actualStartDate));
               }
               setActualStartDate(typedValue);
               break;
            }
        
            case PROP_ID_actualEndDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_actualEndDate));
               }
               setActualEndDate(typedValue);
               break;
            }
        
            case PROP_ID_estimatedHours:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_estimatedHours));
               }
               setEstimatedHours(typedValue);
               break;
            }
        
            case PROP_ID_actualHours:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_actualHours));
               }
               setActualHours(typedValue);
               break;
            }
        
            case PROP_ID_dependsOnId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_dependsOnId));
               }
               setDependsOnId(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_priority:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_priority));
               }
               setPriority(typedValue);
               break;
            }
        
            case PROP_ID_sortNum:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sortNum));
               }
               setSortNum(typedValue);
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
        
            case PROP_ID_projectId:{
               onInitProp(propId);
               this._projectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_parentTaskId:{
               onInitProp(propId);
               this._parentTaskId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_title:{
               onInitProp(propId);
               this._title = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_assigneeId:{
               onInitProp(propId);
               this._assigneeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_plannedStartDate:{
               onInitProp(propId);
               this._plannedStartDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_plannedEndDate:{
               onInitProp(propId);
               this._plannedEndDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_actualStartDate:{
               onInitProp(propId);
               this._actualStartDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_actualEndDate:{
               onInitProp(propId);
               this._actualEndDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_estimatedHours:{
               onInitProp(propId);
               this._estimatedHours = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_actualHours:{
               onInitProp(propId);
               this._actualHours = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_dependsOnId:{
               onInitProp(propId);
               this._dependsOnId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_priority:{
               onInitProp(propId);
               this._priority = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_sortNum:{
               onInitProp(propId);
               this._sortNum = (java.lang.Integer)value;
               
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
     * 项目: PROJECT_ID
     */
    public final java.lang.Long getProjectId(){
         onPropGet(PROP_ID_projectId);
         return _projectId;
    }

    /**
     * 项目: PROJECT_ID
     */
    public final void setProjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_projectId,value)){
            this._projectId = value;
            internalClearRefs(PROP_ID_projectId);
            
        }
    }
    
    /**
     * 父任务: PARENT_TASK_ID
     */
    public final java.lang.Long getParentTaskId(){
         onPropGet(PROP_ID_parentTaskId);
         return _parentTaskId;
    }

    /**
     * 父任务: PARENT_TASK_ID
     */
    public final void setParentTaskId(java.lang.Long value){
        if(onPropSet(PROP_ID_parentTaskId,value)){
            this._parentTaskId = value;
            internalClearRefs(PROP_ID_parentTaskId);
            
        }
    }
    
    /**
     * 任务标题: TITLE
     */
    public final java.lang.String getTitle(){
         onPropGet(PROP_ID_title);
         return _title;
    }

    /**
     * 任务标题: TITLE
     */
    public final void setTitle(java.lang.String value){
        if(onPropSet(PROP_ID_title,value)){
            this._title = value;
            internalClearRefs(PROP_ID_title);
            
        }
    }
    
    /**
     * 负责人: ASSIGNEE_ID
     */
    public final java.lang.Long getAssigneeId(){
         onPropGet(PROP_ID_assigneeId);
         return _assigneeId;
    }

    /**
     * 负责人: ASSIGNEE_ID
     */
    public final void setAssigneeId(java.lang.Long value){
        if(onPropSet(PROP_ID_assigneeId,value)){
            this._assigneeId = value;
            internalClearRefs(PROP_ID_assigneeId);
            
        }
    }
    
    /**
     * 计划开始日期: PLANNED_START_DATE
     */
    public final java.time.LocalDate getPlannedStartDate(){
         onPropGet(PROP_ID_plannedStartDate);
         return _plannedStartDate;
    }

    /**
     * 计划开始日期: PLANNED_START_DATE
     */
    public final void setPlannedStartDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_plannedStartDate,value)){
            this._plannedStartDate = value;
            internalClearRefs(PROP_ID_plannedStartDate);
            
        }
    }
    
    /**
     * 计划结束日期: PLANNED_END_DATE
     */
    public final java.time.LocalDate getPlannedEndDate(){
         onPropGet(PROP_ID_plannedEndDate);
         return _plannedEndDate;
    }

    /**
     * 计划结束日期: PLANNED_END_DATE
     */
    public final void setPlannedEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_plannedEndDate,value)){
            this._plannedEndDate = value;
            internalClearRefs(PROP_ID_plannedEndDate);
            
        }
    }
    
    /**
     * 实际开始日期: ACTUAL_START_DATE
     */
    public final java.time.LocalDate getActualStartDate(){
         onPropGet(PROP_ID_actualStartDate);
         return _actualStartDate;
    }

    /**
     * 实际开始日期: ACTUAL_START_DATE
     */
    public final void setActualStartDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_actualStartDate,value)){
            this._actualStartDate = value;
            internalClearRefs(PROP_ID_actualStartDate);
            
        }
    }
    
    /**
     * 实际结束日期: ACTUAL_END_DATE
     */
    public final java.time.LocalDate getActualEndDate(){
         onPropGet(PROP_ID_actualEndDate);
         return _actualEndDate;
    }

    /**
     * 实际结束日期: ACTUAL_END_DATE
     */
    public final void setActualEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_actualEndDate,value)){
            this._actualEndDate = value;
            internalClearRefs(PROP_ID_actualEndDate);
            
        }
    }
    
    /**
     * 预估工时: ESTIMATED_HOURS
     */
    public final java.lang.String getEstimatedHours(){
         onPropGet(PROP_ID_estimatedHours);
         return _estimatedHours;
    }

    /**
     * 预估工时: ESTIMATED_HOURS
     */
    public final void setEstimatedHours(java.lang.String value){
        if(onPropSet(PROP_ID_estimatedHours,value)){
            this._estimatedHours = value;
            internalClearRefs(PROP_ID_estimatedHours);
            
        }
    }
    
    /**
     * 实际工时: ACTUAL_HOURS
     */
    public final java.lang.String getActualHours(){
         onPropGet(PROP_ID_actualHours);
         return _actualHours;
    }

    /**
     * 实际工时: ACTUAL_HOURS
     */
    public final void setActualHours(java.lang.String value){
        if(onPropSet(PROP_ID_actualHours,value)){
            this._actualHours = value;
            internalClearRefs(PROP_ID_actualHours);
            
        }
    }
    
    /**
     * 依赖任务: DEPENDS_ON_ID
     */
    public final java.lang.Long getDependsOnId(){
         onPropGet(PROP_ID_dependsOnId);
         return _dependsOnId;
    }

    /**
     * 依赖任务: DEPENDS_ON_ID
     */
    public final void setDependsOnId(java.lang.Long value){
        if(onPropSet(PROP_ID_dependsOnId,value)){
            this._dependsOnId = value;
            internalClearRefs(PROP_ID_dependsOnId);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 优先级: PRIORITY
     */
    public final java.lang.Integer getPriority(){
         onPropGet(PROP_ID_priority);
         return _priority;
    }

    /**
     * 优先级: PRIORITY
     */
    public final void setPriority(java.lang.Integer value){
        if(onPropSet(PROP_ID_priority,value)){
            this._priority = value;
            internalClearRefs(PROP_ID_priority);
            
        }
    }
    
    /**
     * 排序号: SORT_NUM
     */
    public final java.lang.Integer getSortNum(){
         onPropGet(PROP_ID_sortNum);
         return _sortNum;
    }

    /**
     * 排序号: SORT_NUM
     */
    public final void setSortNum(java.lang.Integer value){
        if(onPropSet(PROP_ID_sortNum,value)){
            this._sortNum = value;
            internalClearRefs(PROP_ID_sortNum);
            
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
    public final app.erp.prj.dao.entity.ErpPrjProject getProject(){
       return (app.erp.prj.dao.entity.ErpPrjProject)internalGetRefEntity(PROP_NAME_project);
    }

    public final void setProject(app.erp.prj.dao.entity.ErpPrjProject refEntity){
   
           if(refEntity == null){
           
                   this.setProjectId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_project, refEntity,()->{
           
                           this.setProjectId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.prj.dao.entity.ErpPrjTask getParentTask(){
       return (app.erp.prj.dao.entity.ErpPrjTask)internalGetRefEntity(PROP_NAME_parentTask);
    }

    public final void setParentTask(app.erp.prj.dao.entity.ErpPrjTask refEntity){
   
           if(refEntity == null){
           
                   this.setParentTaskId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_parentTask, refEntity,()->{
           
                           this.setParentTaskId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getAssignee(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_assignee);
    }

    public final void setAssignee(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setAssigneeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_assignee, refEntity,()->{
           
                           this.setAssigneeId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.prj.dao.entity.ErpPrjTask getDependsOn(){
       return (app.erp.prj.dao.entity.ErpPrjTask)internalGetRefEntity(PROP_NAME_dependsOn);
    }

    public final void setDependsOn(app.erp.prj.dao.entity.ErpPrjTask refEntity){
   
           if(refEntity == null){
           
                   this.setDependsOnId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_dependsOn, refEntity,()->{
           
                           this.setDependsOnId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.prj.dao.entity.ErpPrjTask> _childTasks = new OrmEntitySet<>(this, PROP_NAME_childTasks,
        null, null,app.erp.prj.dao.entity.ErpPrjTask.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.prj.dao.entity.ErpPrjTask> getChildTasks(){
       return _childTasks;
    }
       
    private final OrmEntitySet<app.erp.prj.dao.entity.ErpPrjTimesheet> _timesheets = new OrmEntitySet<>(this, PROP_NAME_timesheets,
        null, null,app.erp.prj.dao.entity.ErpPrjTimesheet.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.prj.dao.entity.ErpPrjTimesheet> getTimesheets(){
       return _timesheets;
    }
       
}
// resume CPD analysis - CPD-ON
