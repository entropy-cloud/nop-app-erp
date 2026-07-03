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

import app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  发展计划项: erp_hr_development_plan_item
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrDevelopmentPlanItem extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 发展计划: PLAN_ID BIGINT */
    public static final String PROP_NAME_planId = "planId";
    public static final int PROP_ID_planId = 2;
    
    /* 目标胜任力: COMPETENCY_ID BIGINT */
    public static final String PROP_NAME_competencyId = "competencyId";
    public static final int PROP_ID_competencyId = 3;
    
    /* 关联差距分析: GAP_ID BIGINT */
    public static final String PROP_NAME_gapId = "gapId";
    public static final int PROP_ID_gapId = 4;
    
    /* 目标等级: TARGET_LEVEL INTEGER */
    public static final String PROP_NAME_targetLevel = "targetLevel";
    public static final int PROP_ID_targetLevel = 5;
    
    /* 发展行动: DEVELOPMENT_ACTION VARCHAR */
    public static final String PROP_NAME_developmentAction = "developmentAction";
    public static final int PROP_ID_developmentAction = 6;
    
    /* 导师: MENTOR_ID BIGINT */
    public static final String PROP_NAME_mentorId = "mentorId";
    public static final int PROP_ID_mentorId = 7;
    
    /* 开始日期: START_DATE DATE */
    public static final String PROP_NAME_startDate = "startDate";
    public static final int PROP_ID_startDate = 8;
    
    /* 预计完成日期: END_DATE DATE */
    public static final String PROP_NAME_endDate = "endDate";
    public static final int PROP_ID_endDate = 9;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 10;
    
    /* 进度说明: PROGRESS_NOTE VARCHAR */
    public static final String PROP_NAME_progressNote = "progressNote";
    public static final int PROP_ID_progressNote = 11;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 12;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 13;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 14;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 15;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 16;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 17;
    

    private static int _PROP_ID_BOUND = 18;

    
    /* relation:  */
    public static final String PROP_NAME_plan = "plan";
    
    /* relation:  */
    public static final String PROP_NAME_competency = "competency";
    
    /* relation:  */
    public static final String PROP_NAME_gap = "gap";
    
    /* relation:  */
    public static final String PROP_NAME_mentor = "mentor";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_planId] = PROP_NAME_planId;
          PROP_NAME_TO_ID.put(PROP_NAME_planId, PROP_ID_planId);
      
          PROP_ID_TO_NAME[PROP_ID_competencyId] = PROP_NAME_competencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_competencyId, PROP_ID_competencyId);
      
          PROP_ID_TO_NAME[PROP_ID_gapId] = PROP_NAME_gapId;
          PROP_NAME_TO_ID.put(PROP_NAME_gapId, PROP_ID_gapId);
      
          PROP_ID_TO_NAME[PROP_ID_targetLevel] = PROP_NAME_targetLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_targetLevel, PROP_ID_targetLevel);
      
          PROP_ID_TO_NAME[PROP_ID_developmentAction] = PROP_NAME_developmentAction;
          PROP_NAME_TO_ID.put(PROP_NAME_developmentAction, PROP_ID_developmentAction);
      
          PROP_ID_TO_NAME[PROP_ID_mentorId] = PROP_NAME_mentorId;
          PROP_NAME_TO_ID.put(PROP_NAME_mentorId, PROP_ID_mentorId);
      
          PROP_ID_TO_NAME[PROP_ID_startDate] = PROP_NAME_startDate;
          PROP_NAME_TO_ID.put(PROP_NAME_startDate, PROP_ID_startDate);
      
          PROP_ID_TO_NAME[PROP_ID_endDate] = PROP_NAME_endDate;
          PROP_NAME_TO_ID.put(PROP_NAME_endDate, PROP_ID_endDate);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_progressNote] = PROP_NAME_progressNote;
          PROP_NAME_TO_ID.put(PROP_NAME_progressNote, PROP_ID_progressNote);
      
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
    
    /* 发展计划: PLAN_ID */
    private java.lang.Long _planId;
    
    /* 目标胜任力: COMPETENCY_ID */
    private java.lang.Long _competencyId;
    
    /* 关联差距分析: GAP_ID */
    private java.lang.Long _gapId;
    
    /* 目标等级: TARGET_LEVEL */
    private java.lang.Integer _targetLevel;
    
    /* 发展行动: DEVELOPMENT_ACTION */
    private java.lang.String _developmentAction;
    
    /* 导师: MENTOR_ID */
    private java.lang.Long _mentorId;
    
    /* 开始日期: START_DATE */
    private java.time.LocalDate _startDate;
    
    /* 预计完成日期: END_DATE */
    private java.time.LocalDate _endDate;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 进度说明: PROGRESS_NOTE */
    private java.lang.String _progressNote;
    
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
    

    public _ErpHrDevelopmentPlanItem(){
        // for debug
    }

    protected ErpHrDevelopmentPlanItem newInstance(){
        ErpHrDevelopmentPlanItem entity = new ErpHrDevelopmentPlanItem();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrDevelopmentPlanItem cloneInstance() {
        ErpHrDevelopmentPlanItem entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem";
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
        
            case PROP_ID_planId:
               return getPlanId();
        
            case PROP_ID_competencyId:
               return getCompetencyId();
        
            case PROP_ID_gapId:
               return getGapId();
        
            case PROP_ID_targetLevel:
               return getTargetLevel();
        
            case PROP_ID_developmentAction:
               return getDevelopmentAction();
        
            case PROP_ID_mentorId:
               return getMentorId();
        
            case PROP_ID_startDate:
               return getStartDate();
        
            case PROP_ID_endDate:
               return getEndDate();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_progressNote:
               return getProgressNote();
        
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
        
            case PROP_ID_planId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_planId));
               }
               setPlanId(typedValue);
               break;
            }
        
            case PROP_ID_competencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_competencyId));
               }
               setCompetencyId(typedValue);
               break;
            }
        
            case PROP_ID_gapId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_gapId));
               }
               setGapId(typedValue);
               break;
            }
        
            case PROP_ID_targetLevel:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_targetLevel));
               }
               setTargetLevel(typedValue);
               break;
            }
        
            case PROP_ID_developmentAction:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_developmentAction));
               }
               setDevelopmentAction(typedValue);
               break;
            }
        
            case PROP_ID_mentorId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_mentorId));
               }
               setMentorId(typedValue);
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
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_progressNote:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_progressNote));
               }
               setProgressNote(typedValue);
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
        
            case PROP_ID_planId:{
               onInitProp(propId);
               this._planId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_competencyId:{
               onInitProp(propId);
               this._competencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_gapId:{
               onInitProp(propId);
               this._gapId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_targetLevel:{
               onInitProp(propId);
               this._targetLevel = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_developmentAction:{
               onInitProp(propId);
               this._developmentAction = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_mentorId:{
               onInitProp(propId);
               this._mentorId = (java.lang.Long)value;
               
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
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_progressNote:{
               onInitProp(propId);
               this._progressNote = (java.lang.String)value;
               
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
     * 发展计划: PLAN_ID
     */
    public final java.lang.Long getPlanId(){
         onPropGet(PROP_ID_planId);
         return _planId;
    }

    /**
     * 发展计划: PLAN_ID
     */
    public final void setPlanId(java.lang.Long value){
        if(onPropSet(PROP_ID_planId,value)){
            this._planId = value;
            internalClearRefs(PROP_ID_planId);
            
        }
    }
    
    /**
     * 目标胜任力: COMPETENCY_ID
     */
    public final java.lang.Long getCompetencyId(){
         onPropGet(PROP_ID_competencyId);
         return _competencyId;
    }

    /**
     * 目标胜任力: COMPETENCY_ID
     */
    public final void setCompetencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_competencyId,value)){
            this._competencyId = value;
            internalClearRefs(PROP_ID_competencyId);
            
        }
    }
    
    /**
     * 关联差距分析: GAP_ID
     */
    public final java.lang.Long getGapId(){
         onPropGet(PROP_ID_gapId);
         return _gapId;
    }

    /**
     * 关联差距分析: GAP_ID
     */
    public final void setGapId(java.lang.Long value){
        if(onPropSet(PROP_ID_gapId,value)){
            this._gapId = value;
            internalClearRefs(PROP_ID_gapId);
            
        }
    }
    
    /**
     * 目标等级: TARGET_LEVEL
     */
    public final java.lang.Integer getTargetLevel(){
         onPropGet(PROP_ID_targetLevel);
         return _targetLevel;
    }

    /**
     * 目标等级: TARGET_LEVEL
     */
    public final void setTargetLevel(java.lang.Integer value){
        if(onPropSet(PROP_ID_targetLevel,value)){
            this._targetLevel = value;
            internalClearRefs(PROP_ID_targetLevel);
            
        }
    }
    
    /**
     * 发展行动: DEVELOPMENT_ACTION
     */
    public final java.lang.String getDevelopmentAction(){
         onPropGet(PROP_ID_developmentAction);
         return _developmentAction;
    }

    /**
     * 发展行动: DEVELOPMENT_ACTION
     */
    public final void setDevelopmentAction(java.lang.String value){
        if(onPropSet(PROP_ID_developmentAction,value)){
            this._developmentAction = value;
            internalClearRefs(PROP_ID_developmentAction);
            
        }
    }
    
    /**
     * 导师: MENTOR_ID
     */
    public final java.lang.Long getMentorId(){
         onPropGet(PROP_ID_mentorId);
         return _mentorId;
    }

    /**
     * 导师: MENTOR_ID
     */
    public final void setMentorId(java.lang.Long value){
        if(onPropSet(PROP_ID_mentorId,value)){
            this._mentorId = value;
            internalClearRefs(PROP_ID_mentorId);
            
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
     * 预计完成日期: END_DATE
     */
    public final java.time.LocalDate getEndDate(){
         onPropGet(PROP_ID_endDate);
         return _endDate;
    }

    /**
     * 预计完成日期: END_DATE
     */
    public final void setEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_endDate,value)){
            this._endDate = value;
            internalClearRefs(PROP_ID_endDate);
            
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
     * 进度说明: PROGRESS_NOTE
     */
    public final java.lang.String getProgressNote(){
         onPropGet(PROP_ID_progressNote);
         return _progressNote;
    }

    /**
     * 进度说明: PROGRESS_NOTE
     */
    public final void setProgressNote(java.lang.String value){
        if(onPropSet(PROP_ID_progressNote,value)){
            this._progressNote = value;
            internalClearRefs(PROP_ID_progressNote);
            
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
    public final app.erp.hr.dao.entity.ErpHrDevelopmentPlan getPlan(){
       return (app.erp.hr.dao.entity.ErpHrDevelopmentPlan)internalGetRefEntity(PROP_NAME_plan);
    }

    public final void setPlan(app.erp.hr.dao.entity.ErpHrDevelopmentPlan refEntity){
   
           if(refEntity == null){
           
                   this.setPlanId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_plan, refEntity,()->{
           
                           this.setPlanId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrCompetency getCompetency(){
       return (app.erp.hr.dao.entity.ErpHrCompetency)internalGetRefEntity(PROP_NAME_competency);
    }

    public final void setCompetency(app.erp.hr.dao.entity.ErpHrCompetency refEntity){
   
           if(refEntity == null){
           
                   this.setCompetencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_competency, refEntity,()->{
           
                           this.setCompetencyId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrGapAnalysis getGap(){
       return (app.erp.hr.dao.entity.ErpHrGapAnalysis)internalGetRefEntity(PROP_NAME_gap);
    }

    public final void setGap(app.erp.hr.dao.entity.ErpHrGapAnalysis refEntity){
   
           if(refEntity == null){
           
                   this.setGapId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_gap, refEntity,()->{
           
                           this.setGapId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrEmployee getMentor(){
       return (app.erp.hr.dao.entity.ErpHrEmployee)internalGetRefEntity(PROP_NAME_mentor);
    }

    public final void setMentor(app.erp.hr.dao.entity.ErpHrEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setMentorId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_mentor, refEntity,()->{
           
                           this.setMentorId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
