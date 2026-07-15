package app.erp.fin.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.fin.dao.entity.ErpFinBudgetControlLog;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  预算控制日志: erp_fin_budget_control_log
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinBudgetControlLog extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 3;
    
    /* 预算方案: SCENARIO_ID BIGINT */
    public static final String PROP_NAME_scenarioId = "scenarioId";
    public static final int PROP_ID_scenarioId = 4;
    
    /* 预算明细行: BUDGET_LINE_ID BIGINT */
    public static final String PROP_NAME_budgetLineId = "budgetLineId";
    public static final int PROP_ID_budgetLineId = 5;
    
    /* 触发单据类型: SOURCE_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_sourceBillType = "sourceBillType";
    public static final int PROP_ID_sourceBillType = 6;
    
    /* 触发单据号: SOURCE_BILL_CODE VARCHAR */
    public static final String PROP_NAME_sourceBillCode = "sourceBillCode";
    public static final int PROP_ID_sourceBillCode = 7;
    
    /* 命中科目: SUBJECT_ID BIGINT */
    public static final String PROP_NAME_subjectId = "subjectId";
    public static final int PROP_ID_subjectId = 8;
    
    /* 命中成本中心: COST_CENTER_ID BIGINT */
    public static final String PROP_NAME_costCenterId = "costCenterId";
    public static final int PROP_ID_costCenterId = 9;
    
    /* 命中项目: PROJECT_ID BIGINT */
    public static final String PROP_NAME_projectId = "projectId";
    public static final int PROP_ID_projectId = 10;
    
    /* 命中期间: PERIOD_ID BIGINT */
    public static final String PROP_NAME_periodId = "periodId";
    public static final int PROP_ID_periodId = 11;
    
    /* 申请占用金额: REQUESTED_AMOUNT DECIMAL */
    public static final String PROP_NAME_requestedAmount = "requestedAmount";
    public static final int PROP_ID_requestedAmount = 12;
    
    /* 实际占用金额: COMMITTED_AMOUNT DECIMAL */
    public static final String PROP_NAME_committedAmount = "committedAmount";
    public static final int PROP_ID_committedAmount = 13;
    
    /* 检查时余量: AVAILABLE_AMOUNT DECIMAL */
    public static final String PROP_NAME_availableAmount = "availableAmount";
    public static final int PROP_ID_availableAmount = 14;
    
    /* 控制结果: ACTION_RESULT VARCHAR */
    public static final String PROP_NAME_actionResult = "actionResult";
    public static final int PROP_ID_actionResult = 15;
    
    /* 操作人: OPERATOR_ID VARCHAR */
    public static final String PROP_NAME_operatorId = "operatorId";
    public static final int PROP_ID_operatorId = 16;
    
    /* 操作时间: OPERATED_AT TIMESTAMP */
    public static final String PROP_NAME_operatedAt = "operatedAt";
    public static final int PROP_ID_operatedAt = 17;
    
    /* 原因: REASON VARCHAR */
    public static final String PROP_NAME_reason = "reason";
    public static final int PROP_ID_reason = 18;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 19;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 20;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 21;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 22;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 23;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 24;
    

    private static int _PROP_ID_BOUND = 25;

    
    /* relation:  */
    public static final String PROP_NAME_scenario = "scenario";
    
    /* relation:  */
    public static final String PROP_NAME_budgetLine = "budgetLine";
    
    /* relation:  */
    public static final String PROP_NAME_subject = "subject";
    
    /* relation:  */
    public static final String PROP_NAME_costCenter = "costCenter";
    
    /* relation:  */
    public static final String PROP_NAME_period = "period";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[25];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_scenarioId] = PROP_NAME_scenarioId;
          PROP_NAME_TO_ID.put(PROP_NAME_scenarioId, PROP_ID_scenarioId);
      
          PROP_ID_TO_NAME[PROP_ID_budgetLineId] = PROP_NAME_budgetLineId;
          PROP_NAME_TO_ID.put(PROP_NAME_budgetLineId, PROP_ID_budgetLineId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillType] = PROP_NAME_sourceBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillType, PROP_ID_sourceBillType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillCode] = PROP_NAME_sourceBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillCode, PROP_ID_sourceBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_subjectId] = PROP_NAME_subjectId;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectId, PROP_ID_subjectId);
      
          PROP_ID_TO_NAME[PROP_ID_costCenterId] = PROP_NAME_costCenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_costCenterId, PROP_ID_costCenterId);
      
          PROP_ID_TO_NAME[PROP_ID_projectId] = PROP_NAME_projectId;
          PROP_NAME_TO_ID.put(PROP_NAME_projectId, PROP_ID_projectId);
      
          PROP_ID_TO_NAME[PROP_ID_periodId] = PROP_NAME_periodId;
          PROP_NAME_TO_ID.put(PROP_NAME_periodId, PROP_ID_periodId);
      
          PROP_ID_TO_NAME[PROP_ID_requestedAmount] = PROP_NAME_requestedAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_requestedAmount, PROP_ID_requestedAmount);
      
          PROP_ID_TO_NAME[PROP_ID_committedAmount] = PROP_NAME_committedAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_committedAmount, PROP_ID_committedAmount);
      
          PROP_ID_TO_NAME[PROP_ID_availableAmount] = PROP_NAME_availableAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_availableAmount, PROP_ID_availableAmount);
      
          PROP_ID_TO_NAME[PROP_ID_actionResult] = PROP_NAME_actionResult;
          PROP_NAME_TO_ID.put(PROP_NAME_actionResult, PROP_ID_actionResult);
      
          PROP_ID_TO_NAME[PROP_ID_operatorId] = PROP_NAME_operatorId;
          PROP_NAME_TO_ID.put(PROP_NAME_operatorId, PROP_ID_operatorId);
      
          PROP_ID_TO_NAME[PROP_ID_operatedAt] = PROP_NAME_operatedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_operatedAt, PROP_ID_operatedAt);
      
          PROP_ID_TO_NAME[PROP_ID_reason] = PROP_NAME_reason;
          PROP_NAME_TO_ID.put(PROP_NAME_reason, PROP_ID_reason);
      
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
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 预算方案: SCENARIO_ID */
    private java.lang.Long _scenarioId;
    
    /* 预算明细行: BUDGET_LINE_ID */
    private java.lang.Long _budgetLineId;
    
    /* 触发单据类型: SOURCE_BILL_TYPE */
    private java.lang.String _sourceBillType;
    
    /* 触发单据号: SOURCE_BILL_CODE */
    private java.lang.String _sourceBillCode;
    
    /* 命中科目: SUBJECT_ID */
    private java.lang.Long _subjectId;
    
    /* 命中成本中心: COST_CENTER_ID */
    private java.lang.Long _costCenterId;
    
    /* 命中项目: PROJECT_ID */
    private java.lang.Long _projectId;
    
    /* 命中期间: PERIOD_ID */
    private java.lang.Long _periodId;
    
    /* 申请占用金额: REQUESTED_AMOUNT */
    private java.math.BigDecimal _requestedAmount;
    
    /* 实际占用金额: COMMITTED_AMOUNT */
    private java.math.BigDecimal _committedAmount;
    
    /* 检查时余量: AVAILABLE_AMOUNT */
    private java.math.BigDecimal _availableAmount;
    
    /* 控制结果: ACTION_RESULT */
    private java.lang.String _actionResult;
    
    /* 操作人: OPERATOR_ID */
    private java.lang.String _operatorId;
    
    /* 操作时间: OPERATED_AT */
    private java.sql.Timestamp _operatedAt;
    
    /* 原因: REASON */
    private java.lang.String _reason;
    
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
    

    public _ErpFinBudgetControlLog(){
        // for debug
    }

    protected ErpFinBudgetControlLog newInstance(){
        ErpFinBudgetControlLog entity = new ErpFinBudgetControlLog();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinBudgetControlLog cloneInstance() {
        ErpFinBudgetControlLog entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinBudgetControlLog";
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
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_scenarioId:
               return getScenarioId();
        
            case PROP_ID_budgetLineId:
               return getBudgetLineId();
        
            case PROP_ID_sourceBillType:
               return getSourceBillType();
        
            case PROP_ID_sourceBillCode:
               return getSourceBillCode();
        
            case PROP_ID_subjectId:
               return getSubjectId();
        
            case PROP_ID_costCenterId:
               return getCostCenterId();
        
            case PROP_ID_projectId:
               return getProjectId();
        
            case PROP_ID_periodId:
               return getPeriodId();
        
            case PROP_ID_requestedAmount:
               return getRequestedAmount();
        
            case PROP_ID_committedAmount:
               return getCommittedAmount();
        
            case PROP_ID_availableAmount:
               return getAvailableAmount();
        
            case PROP_ID_actionResult:
               return getActionResult();
        
            case PROP_ID_operatorId:
               return getOperatorId();
        
            case PROP_ID_operatedAt:
               return getOperatedAt();
        
            case PROP_ID_reason:
               return getReason();
        
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
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
               break;
            }
        
            case PROP_ID_scenarioId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_scenarioId));
               }
               setScenarioId(typedValue);
               break;
            }
        
            case PROP_ID_budgetLineId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_budgetLineId));
               }
               setBudgetLineId(typedValue);
               break;
            }
        
            case PROP_ID_sourceBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillType));
               }
               setSourceBillType(typedValue);
               break;
            }
        
            case PROP_ID_sourceBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillCode));
               }
               setSourceBillCode(typedValue);
               break;
            }
        
            case PROP_ID_subjectId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_subjectId));
               }
               setSubjectId(typedValue);
               break;
            }
        
            case PROP_ID_costCenterId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_costCenterId));
               }
               setCostCenterId(typedValue);
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
        
            case PROP_ID_periodId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_periodId));
               }
               setPeriodId(typedValue);
               break;
            }
        
            case PROP_ID_requestedAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_requestedAmount));
               }
               setRequestedAmount(typedValue);
               break;
            }
        
            case PROP_ID_committedAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_committedAmount));
               }
               setCommittedAmount(typedValue);
               break;
            }
        
            case PROP_ID_availableAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_availableAmount));
               }
               setAvailableAmount(typedValue);
               break;
            }
        
            case PROP_ID_actionResult:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_actionResult));
               }
               setActionResult(typedValue);
               break;
            }
        
            case PROP_ID_operatorId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_operatorId));
               }
               setOperatorId(typedValue);
               break;
            }
        
            case PROP_ID_operatedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_operatedAt));
               }
               setOperatedAt(typedValue);
               break;
            }
        
            case PROP_ID_reason:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_reason));
               }
               setReason(typedValue);
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
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_scenarioId:{
               onInitProp(propId);
               this._scenarioId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_budgetLineId:{
               onInitProp(propId);
               this._budgetLineId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceBillType:{
               onInitProp(propId);
               this._sourceBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceBillCode:{
               onInitProp(propId);
               this._sourceBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_subjectId:{
               onInitProp(propId);
               this._subjectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_costCenterId:{
               onInitProp(propId);
               this._costCenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_projectId:{
               onInitProp(propId);
               this._projectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_periodId:{
               onInitProp(propId);
               this._periodId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_requestedAmount:{
               onInitProp(propId);
               this._requestedAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_committedAmount:{
               onInitProp(propId);
               this._committedAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_availableAmount:{
               onInitProp(propId);
               this._availableAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_actionResult:{
               onInitProp(propId);
               this._actionResult = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_operatorId:{
               onInitProp(propId);
               this._operatorId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_operatedAt:{
               onInitProp(propId);
               this._operatedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_reason:{
               onInitProp(propId);
               this._reason = (java.lang.String)value;
               
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
     * 核算组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 核算组织: ORG_ID
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
     * 预算方案: SCENARIO_ID
     */
    public final java.lang.Long getScenarioId(){
         onPropGet(PROP_ID_scenarioId);
         return _scenarioId;
    }

    /**
     * 预算方案: SCENARIO_ID
     */
    public final void setScenarioId(java.lang.Long value){
        if(onPropSet(PROP_ID_scenarioId,value)){
            this._scenarioId = value;
            internalClearRefs(PROP_ID_scenarioId);
            
        }
    }
    
    /**
     * 预算明细行: BUDGET_LINE_ID
     */
    public final java.lang.Long getBudgetLineId(){
         onPropGet(PROP_ID_budgetLineId);
         return _budgetLineId;
    }

    /**
     * 预算明细行: BUDGET_LINE_ID
     */
    public final void setBudgetLineId(java.lang.Long value){
        if(onPropSet(PROP_ID_budgetLineId,value)){
            this._budgetLineId = value;
            internalClearRefs(PROP_ID_budgetLineId);
            
        }
    }
    
    /**
     * 触发单据类型: SOURCE_BILL_TYPE
     */
    public final java.lang.String getSourceBillType(){
         onPropGet(PROP_ID_sourceBillType);
         return _sourceBillType;
    }

    /**
     * 触发单据类型: SOURCE_BILL_TYPE
     */
    public final void setSourceBillType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillType,value)){
            this._sourceBillType = value;
            internalClearRefs(PROP_ID_sourceBillType);
            
        }
    }
    
    /**
     * 触发单据号: SOURCE_BILL_CODE
     */
    public final java.lang.String getSourceBillCode(){
         onPropGet(PROP_ID_sourceBillCode);
         return _sourceBillCode;
    }

    /**
     * 触发单据号: SOURCE_BILL_CODE
     */
    public final void setSourceBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillCode,value)){
            this._sourceBillCode = value;
            internalClearRefs(PROP_ID_sourceBillCode);
            
        }
    }
    
    /**
     * 命中科目: SUBJECT_ID
     */
    public final java.lang.Long getSubjectId(){
         onPropGet(PROP_ID_subjectId);
         return _subjectId;
    }

    /**
     * 命中科目: SUBJECT_ID
     */
    public final void setSubjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_subjectId,value)){
            this._subjectId = value;
            internalClearRefs(PROP_ID_subjectId);
            
        }
    }
    
    /**
     * 命中成本中心: COST_CENTER_ID
     */
    public final java.lang.Long getCostCenterId(){
         onPropGet(PROP_ID_costCenterId);
         return _costCenterId;
    }

    /**
     * 命中成本中心: COST_CENTER_ID
     */
    public final void setCostCenterId(java.lang.Long value){
        if(onPropSet(PROP_ID_costCenterId,value)){
            this._costCenterId = value;
            internalClearRefs(PROP_ID_costCenterId);
            
        }
    }
    
    /**
     * 命中项目: PROJECT_ID
     */
    public final java.lang.Long getProjectId(){
         onPropGet(PROP_ID_projectId);
         return _projectId;
    }

    /**
     * 命中项目: PROJECT_ID
     */
    public final void setProjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_projectId,value)){
            this._projectId = value;
            internalClearRefs(PROP_ID_projectId);
            
        }
    }
    
    /**
     * 命中期间: PERIOD_ID
     */
    public final java.lang.Long getPeriodId(){
         onPropGet(PROP_ID_periodId);
         return _periodId;
    }

    /**
     * 命中期间: PERIOD_ID
     */
    public final void setPeriodId(java.lang.Long value){
        if(onPropSet(PROP_ID_periodId,value)){
            this._periodId = value;
            internalClearRefs(PROP_ID_periodId);
            
        }
    }
    
    /**
     * 申请占用金额: REQUESTED_AMOUNT
     */
    public final java.math.BigDecimal getRequestedAmount(){
         onPropGet(PROP_ID_requestedAmount);
         return _requestedAmount;
    }

    /**
     * 申请占用金额: REQUESTED_AMOUNT
     */
    public final void setRequestedAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_requestedAmount,value)){
            this._requestedAmount = value;
            internalClearRefs(PROP_ID_requestedAmount);
            
        }
    }
    
    /**
     * 实际占用金额: COMMITTED_AMOUNT
     */
    public final java.math.BigDecimal getCommittedAmount(){
         onPropGet(PROP_ID_committedAmount);
         return _committedAmount;
    }

    /**
     * 实际占用金额: COMMITTED_AMOUNT
     */
    public final void setCommittedAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_committedAmount,value)){
            this._committedAmount = value;
            internalClearRefs(PROP_ID_committedAmount);
            
        }
    }
    
    /**
     * 检查时余量: AVAILABLE_AMOUNT
     */
    public final java.math.BigDecimal getAvailableAmount(){
         onPropGet(PROP_ID_availableAmount);
         return _availableAmount;
    }

    /**
     * 检查时余量: AVAILABLE_AMOUNT
     */
    public final void setAvailableAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_availableAmount,value)){
            this._availableAmount = value;
            internalClearRefs(PROP_ID_availableAmount);
            
        }
    }
    
    /**
     * 控制结果: ACTION_RESULT
     */
    public final java.lang.String getActionResult(){
         onPropGet(PROP_ID_actionResult);
         return _actionResult;
    }

    /**
     * 控制结果: ACTION_RESULT
     */
    public final void setActionResult(java.lang.String value){
        if(onPropSet(PROP_ID_actionResult,value)){
            this._actionResult = value;
            internalClearRefs(PROP_ID_actionResult);
            
        }
    }
    
    /**
     * 操作人: OPERATOR_ID
     */
    public final java.lang.String getOperatorId(){
         onPropGet(PROP_ID_operatorId);
         return _operatorId;
    }

    /**
     * 操作人: OPERATOR_ID
     */
    public final void setOperatorId(java.lang.String value){
        if(onPropSet(PROP_ID_operatorId,value)){
            this._operatorId = value;
            internalClearRefs(PROP_ID_operatorId);
            
        }
    }
    
    /**
     * 操作时间: OPERATED_AT
     */
    public final java.sql.Timestamp getOperatedAt(){
         onPropGet(PROP_ID_operatedAt);
         return _operatedAt;
    }

    /**
     * 操作时间: OPERATED_AT
     */
    public final void setOperatedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_operatedAt,value)){
            this._operatedAt = value;
            internalClearRefs(PROP_ID_operatedAt);
            
        }
    }
    
    /**
     * 原因: REASON
     */
    public final java.lang.String getReason(){
         onPropGet(PROP_ID_reason);
         return _reason;
    }

    /**
     * 原因: REASON
     */
    public final void setReason(java.lang.String value){
        if(onPropSet(PROP_ID_reason,value)){
            this._reason = value;
            internalClearRefs(PROP_ID_reason);
            
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
    public final app.erp.fin.dao.entity.ErpFinBudgetScenario getScenario(){
       return (app.erp.fin.dao.entity.ErpFinBudgetScenario)internalGetRefEntity(PROP_NAME_scenario);
    }

    public final void setScenario(app.erp.fin.dao.entity.ErpFinBudgetScenario refEntity){
   
           if(refEntity == null){
           
                   this.setScenarioId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_scenario, refEntity,()->{
           
                           this.setScenarioId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinBudgetLine getBudgetLine(){
       return (app.erp.fin.dao.entity.ErpFinBudgetLine)internalGetRefEntity(PROP_NAME_budgetLine);
    }

    public final void setBudgetLine(app.erp.fin.dao.entity.ErpFinBudgetLine refEntity){
   
           if(refEntity == null){
           
                   this.setBudgetLineId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_budgetLine, refEntity,()->{
           
                           this.setBudgetLineId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdSubject getSubject(){
       return (app.erp.md.dao.entity.ErpMdSubject)internalGetRefEntity(PROP_NAME_subject);
    }

    public final void setSubject(app.erp.md.dao.entity.ErpMdSubject refEntity){
   
           if(refEntity == null){
           
                   this.setSubjectId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_subject, refEntity,()->{
           
                           this.setSubjectId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCostCenter getCostCenter(){
       return (app.erp.md.dao.entity.ErpMdCostCenter)internalGetRefEntity(PROP_NAME_costCenter);
    }

    public final void setCostCenter(app.erp.md.dao.entity.ErpMdCostCenter refEntity){
   
           if(refEntity == null){
           
                   this.setCostCenterId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_costCenter, refEntity,()->{
           
                           this.setCostCenterId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinAccountingPeriod getPeriod(){
       return (app.erp.fin.dao.entity.ErpFinAccountingPeriod)internalGetRefEntity(PROP_NAME_period);
    }

    public final void setPeriod(app.erp.fin.dao.entity.ErpFinAccountingPeriod refEntity){
   
           if(refEntity == null){
           
                   this.setPeriodId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_period, refEntity,()->{
           
                           this.setPeriodId(refEntity.getId());
                       
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
