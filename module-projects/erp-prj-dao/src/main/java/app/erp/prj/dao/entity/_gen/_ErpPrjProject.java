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

import app.erp.prj.dao.entity.ErpPrjProject;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  项目: erp_prj_project
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpPrjProject extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 项目编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 项目名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 4;
    
    /* 项目类型: PROJECT_TYPE_ID BIGINT */
    public static final String PROP_NAME_projectTypeId = "projectTypeId";
    public static final int PROP_ID_projectTypeId = 5;
    
    /* 客户: CUSTOMER_ID BIGINT */
    public static final String PROP_NAME_customerId = "customerId";
    public static final int PROP_ID_customerId = 6;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 7;
    
    /* 开始日期: START_DATE DATE */
    public static final String PROP_NAME_startDate = "startDate";
    public static final int PROP_ID_startDate = 8;
    
    /* 结束日期: END_DATE DATE */
    public static final String PROP_NAME_endDate = "endDate";
    public static final int PROP_ID_endDate = 9;
    
    /* 预算总额: BUDGET DECIMAL */
    public static final String PROP_NAME_budget = "budget";
    public static final int PROP_ID_budget = 10;
    
    /* 已承诺成本: COMMITTED_COST DECIMAL */
    public static final String PROP_NAME_committedCost = "committedCost";
    public static final int PROP_ID_committedCost = 11;
    
    /* 实际成本: ACTUAL_COST DECIMAL */
    public static final String PROP_NAME_actualCost = "actualCost";
    public static final int PROP_ID_actualCost = 12;
    
    /* 已开票金额: BILLED_AMOUNT DECIMAL */
    public static final String PROP_NAME_billedAmount = "billedAmount";
    public static final int PROP_ID_billedAmount = 13;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 14;
    
    /* 项目经理(职员): MANAGER_ID BIGINT */
    public static final String PROP_NAME_managerId = "managerId";
    public static final int PROP_ID_managerId = 15;
    
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
    public static final String PROP_NAME_projectType = "projectType";
    
    /* relation:  */
    public static final String PROP_NAME_tasks = "tasks";
    
    /* relation:  */
    public static final String PROP_NAME_timesheets = "timesheets";
    
    /* relation:  */
    public static final String PROP_NAME_members = "members";
    
    /* relation:  */
    public static final String PROP_NAME_budgets = "budgets";
    
    /* relation:  */
    public static final String PROP_NAME_milestones = "milestones";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_projectTypeId] = PROP_NAME_projectTypeId;
          PROP_NAME_TO_ID.put(PROP_NAME_projectTypeId, PROP_ID_projectTypeId);
      
          PROP_ID_TO_NAME[PROP_ID_customerId] = PROP_NAME_customerId;
          PROP_NAME_TO_ID.put(PROP_NAME_customerId, PROP_ID_customerId);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_startDate] = PROP_NAME_startDate;
          PROP_NAME_TO_ID.put(PROP_NAME_startDate, PROP_ID_startDate);
      
          PROP_ID_TO_NAME[PROP_ID_endDate] = PROP_NAME_endDate;
          PROP_NAME_TO_ID.put(PROP_NAME_endDate, PROP_ID_endDate);
      
          PROP_ID_TO_NAME[PROP_ID_budget] = PROP_NAME_budget;
          PROP_NAME_TO_ID.put(PROP_NAME_budget, PROP_ID_budget);
      
          PROP_ID_TO_NAME[PROP_ID_committedCost] = PROP_NAME_committedCost;
          PROP_NAME_TO_ID.put(PROP_NAME_committedCost, PROP_ID_committedCost);
      
          PROP_ID_TO_NAME[PROP_ID_actualCost] = PROP_NAME_actualCost;
          PROP_NAME_TO_ID.put(PROP_NAME_actualCost, PROP_ID_actualCost);
      
          PROP_ID_TO_NAME[PROP_ID_billedAmount] = PROP_NAME_billedAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_billedAmount, PROP_ID_billedAmount);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_managerId] = PROP_NAME_managerId;
          PROP_NAME_TO_ID.put(PROP_NAME_managerId, PROP_ID_managerId);
      
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
    
    /* 项目编码: CODE */
    private java.lang.String _code;
    
    /* 项目名称: NAME */
    private java.lang.String _name;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 项目类型: PROJECT_TYPE_ID */
    private java.lang.Long _projectTypeId;
    
    /* 客户: CUSTOMER_ID */
    private java.lang.Long _customerId;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 开始日期: START_DATE */
    private java.time.LocalDate _startDate;
    
    /* 结束日期: END_DATE */
    private java.time.LocalDate _endDate;
    
    /* 预算总额: BUDGET */
    private java.lang.String _budget;
    
    /* 已承诺成本: COMMITTED_COST */
    private java.lang.String _committedCost;
    
    /* 实际成本: ACTUAL_COST */
    private java.lang.String _actualCost;
    
    /* 已开票金额: BILLED_AMOUNT */
    private java.lang.String _billedAmount;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 项目经理(职员): MANAGER_ID */
    private java.lang.Long _managerId;
    
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
    

    public _ErpPrjProject(){
        // for debug
    }

    protected ErpPrjProject newInstance(){
        ErpPrjProject entity = new ErpPrjProject();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpPrjProject cloneInstance() {
        ErpPrjProject entity = newInstance();
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
      return "app.erp.prj.dao.entity.ErpPrjProject";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_projectTypeId:
               return getProjectTypeId();
        
            case PROP_ID_customerId:
               return getCustomerId();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_startDate:
               return getStartDate();
        
            case PROP_ID_endDate:
               return getEndDate();
        
            case PROP_ID_budget:
               return getBudget();
        
            case PROP_ID_committedCost:
               return getCommittedCost();
        
            case PROP_ID_actualCost:
               return getActualCost();
        
            case PROP_ID_billedAmount:
               return getBilledAmount();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_managerId:
               return getManagerId();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_projectTypeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_projectTypeId));
               }
               setProjectTypeId(typedValue);
               break;
            }
        
            case PROP_ID_customerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_customerId));
               }
               setCustomerId(typedValue);
               break;
            }
        
            case PROP_ID_currencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_currencyId));
               }
               setCurrencyId(typedValue);
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
        
            case PROP_ID_budget:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_budget));
               }
               setBudget(typedValue);
               break;
            }
        
            case PROP_ID_committedCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_committedCost));
               }
               setCommittedCost(typedValue);
               break;
            }
        
            case PROP_ID_actualCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_actualCost));
               }
               setActualCost(typedValue);
               break;
            }
        
            case PROP_ID_billedAmount:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_billedAmount));
               }
               setBilledAmount(typedValue);
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
        
            case PROP_ID_managerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_managerId));
               }
               setManagerId(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_projectTypeId:{
               onInitProp(propId);
               this._projectTypeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_customerId:{
               onInitProp(propId);
               this._customerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
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
        
            case PROP_ID_budget:{
               onInitProp(propId);
               this._budget = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_committedCost:{
               onInitProp(propId);
               this._committedCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_actualCost:{
               onInitProp(propId);
               this._actualCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_billedAmount:{
               onInitProp(propId);
               this._billedAmount = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_managerId:{
               onInitProp(propId);
               this._managerId = (java.lang.Long)value;
               
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
     * 项目编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 项目编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 项目名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 项目名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
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
     * 项目类型: PROJECT_TYPE_ID
     */
    public final java.lang.Long getProjectTypeId(){
         onPropGet(PROP_ID_projectTypeId);
         return _projectTypeId;
    }

    /**
     * 项目类型: PROJECT_TYPE_ID
     */
    public final void setProjectTypeId(java.lang.Long value){
        if(onPropSet(PROP_ID_projectTypeId,value)){
            this._projectTypeId = value;
            internalClearRefs(PROP_ID_projectTypeId);
            
        }
    }
    
    /**
     * 客户: CUSTOMER_ID
     */
    public final java.lang.Long getCustomerId(){
         onPropGet(PROP_ID_customerId);
         return _customerId;
    }

    /**
     * 客户: CUSTOMER_ID
     */
    public final void setCustomerId(java.lang.Long value){
        if(onPropSet(PROP_ID_customerId,value)){
            this._customerId = value;
            internalClearRefs(PROP_ID_customerId);
            
        }
    }
    
    /**
     * 币种: CURRENCY_ID
     */
    public final java.lang.Long getCurrencyId(){
         onPropGet(PROP_ID_currencyId);
         return _currencyId;
    }

    /**
     * 币种: CURRENCY_ID
     */
    public final void setCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_currencyId,value)){
            this._currencyId = value;
            internalClearRefs(PROP_ID_currencyId);
            
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
     * 预算总额: BUDGET
     */
    public final java.lang.String getBudget(){
         onPropGet(PROP_ID_budget);
         return _budget;
    }

    /**
     * 预算总额: BUDGET
     */
    public final void setBudget(java.lang.String value){
        if(onPropSet(PROP_ID_budget,value)){
            this._budget = value;
            internalClearRefs(PROP_ID_budget);
            
        }
    }
    
    /**
     * 已承诺成本: COMMITTED_COST
     */
    public final java.lang.String getCommittedCost(){
         onPropGet(PROP_ID_committedCost);
         return _committedCost;
    }

    /**
     * 已承诺成本: COMMITTED_COST
     */
    public final void setCommittedCost(java.lang.String value){
        if(onPropSet(PROP_ID_committedCost,value)){
            this._committedCost = value;
            internalClearRefs(PROP_ID_committedCost);
            
        }
    }
    
    /**
     * 实际成本: ACTUAL_COST
     */
    public final java.lang.String getActualCost(){
         onPropGet(PROP_ID_actualCost);
         return _actualCost;
    }

    /**
     * 实际成本: ACTUAL_COST
     */
    public final void setActualCost(java.lang.String value){
        if(onPropSet(PROP_ID_actualCost,value)){
            this._actualCost = value;
            internalClearRefs(PROP_ID_actualCost);
            
        }
    }
    
    /**
     * 已开票金额: BILLED_AMOUNT
     */
    public final java.lang.String getBilledAmount(){
         onPropGet(PROP_ID_billedAmount);
         return _billedAmount;
    }

    /**
     * 已开票金额: BILLED_AMOUNT
     */
    public final void setBilledAmount(java.lang.String value){
        if(onPropSet(PROP_ID_billedAmount,value)){
            this._billedAmount = value;
            internalClearRefs(PROP_ID_billedAmount);
            
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
     * 项目经理(职员): MANAGER_ID
     */
    public final java.lang.Long getManagerId(){
         onPropGet(PROP_ID_managerId);
         return _managerId;
    }

    /**
     * 项目经理(职员): MANAGER_ID
     */
    public final void setManagerId(java.lang.Long value){
        if(onPropSet(PROP_ID_managerId,value)){
            this._managerId = value;
            internalClearRefs(PROP_ID_managerId);
            
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
    public final app.erp.prj.dao.entity.ErpPrjProjectType getProjectType(){
       return (app.erp.prj.dao.entity.ErpPrjProjectType)internalGetRefEntity(PROP_NAME_projectType);
    }

    public final void setProjectType(app.erp.prj.dao.entity.ErpPrjProjectType refEntity){
   
           if(refEntity == null){
           
                   this.setProjectTypeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_projectType, refEntity,()->{
           
                           this.setProjectTypeId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.prj.dao.entity.ErpPrjTask> _tasks = new OrmEntitySet<>(this, PROP_NAME_tasks,
        null, null,app.erp.prj.dao.entity.ErpPrjTask.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.prj.dao.entity.ErpPrjTask> getTasks(){
       return _tasks;
    }
       
    private final OrmEntitySet<app.erp.prj.dao.entity.ErpPrjTimesheet> _timesheets = new OrmEntitySet<>(this, PROP_NAME_timesheets,
        null, null,app.erp.prj.dao.entity.ErpPrjTimesheet.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.prj.dao.entity.ErpPrjTimesheet> getTimesheets(){
       return _timesheets;
    }
       
    private final OrmEntitySet<app.erp.prj.dao.entity.ErpPrjProjectUser> _members = new OrmEntitySet<>(this, PROP_NAME_members,
        null, null,app.erp.prj.dao.entity.ErpPrjProjectUser.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.prj.dao.entity.ErpPrjProjectUser> getMembers(){
       return _members;
    }
       
    private final OrmEntitySet<app.erp.prj.dao.entity.ErpPrjBudget> _budgets = new OrmEntitySet<>(this, PROP_NAME_budgets,
        null, null,app.erp.prj.dao.entity.ErpPrjBudget.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.prj.dao.entity.ErpPrjBudget> getBudgets(){
       return _budgets;
    }
       
    private final OrmEntitySet<app.erp.prj.dao.entity.ErpPrjMilestone> _milestones = new OrmEntitySet<>(this, PROP_NAME_milestones,
        null, null,app.erp.prj.dao.entity.ErpPrjMilestone.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.prj.dao.entity.ErpPrjMilestone> getMilestones(){
       return _milestones;
    }
       
}
// resume CPD analysis - CPD-ON
