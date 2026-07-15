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

import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  薪酬模拟调整项: erp_hr_salary_simulation_item_adj
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrSalarySimulationItemAdjustment extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 模拟: SIMULATION_ID BIGINT */
    public static final String PROP_NAME_simulationId = "simulationId";
    public static final int PROP_ID_simulationId = 2;
    
    /* 员工: EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_employeeId = "employeeId";
    public static final int PROP_ID_employeeId = 3;
    
    /* 薪酬项目编码: SALARY_ITEM_CODE VARCHAR */
    public static final String PROP_NAME_salaryItemCode = "salaryItemCode";
    public static final int PROP_ID_salaryItemCode = 4;
    
    /* 源值: ORIGINAL_AMOUNT DECIMAL */
    public static final String PROP_NAME_originalAmount = "originalAmount";
    public static final int PROP_ID_originalAmount = 5;
    
    /* 调整后值: ADJUSTED_AMOUNT DECIMAL */
    public static final String PROP_NAME_adjustedAmount = "adjustedAmount";
    public static final int PROP_ID_adjustedAmount = 6;
    
    /* 调整原因: ADJUSTMENT_REASON VARCHAR */
    public static final String PROP_NAME_adjustmentReason = "adjustmentReason";
    public static final int PROP_ID_adjustmentReason = 7;
    
    /* 调整人: ADJUSTED_BY VARCHAR */
    public static final String PROP_NAME_adjustedBy = "adjustedBy";
    public static final int PROP_ID_adjustedBy = 8;
    
    /* 调整时间: ADJUSTED_AT TIMESTAMP */
    public static final String PROP_NAME_adjustedAt = "adjustedAt";
    public static final int PROP_ID_adjustedAt = 9;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 10;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 11;
    
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
    public static final String PROP_NAME_simulation = "simulation";
    
    /* relation:  */
    public static final String PROP_NAME_employee = "employee";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_simulationId] = PROP_NAME_simulationId;
          PROP_NAME_TO_ID.put(PROP_NAME_simulationId, PROP_ID_simulationId);
      
          PROP_ID_TO_NAME[PROP_ID_employeeId] = PROP_NAME_employeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeId, PROP_ID_employeeId);
      
          PROP_ID_TO_NAME[PROP_ID_salaryItemCode] = PROP_NAME_salaryItemCode;
          PROP_NAME_TO_ID.put(PROP_NAME_salaryItemCode, PROP_ID_salaryItemCode);
      
          PROP_ID_TO_NAME[PROP_ID_originalAmount] = PROP_NAME_originalAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_originalAmount, PROP_ID_originalAmount);
      
          PROP_ID_TO_NAME[PROP_ID_adjustedAmount] = PROP_NAME_adjustedAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_adjustedAmount, PROP_ID_adjustedAmount);
      
          PROP_ID_TO_NAME[PROP_ID_adjustmentReason] = PROP_NAME_adjustmentReason;
          PROP_NAME_TO_ID.put(PROP_NAME_adjustmentReason, PROP_ID_adjustmentReason);
      
          PROP_ID_TO_NAME[PROP_ID_adjustedBy] = PROP_NAME_adjustedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_adjustedBy, PROP_ID_adjustedBy);
      
          PROP_ID_TO_NAME[PROP_ID_adjustedAt] = PROP_NAME_adjustedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_adjustedAt, PROP_ID_adjustedAt);
      
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
    
    /* 模拟: SIMULATION_ID */
    private java.lang.Long _simulationId;
    
    /* 员工: EMPLOYEE_ID */
    private java.lang.Long _employeeId;
    
    /* 薪酬项目编码: SALARY_ITEM_CODE */
    private java.lang.String _salaryItemCode;
    
    /* 源值: ORIGINAL_AMOUNT */
    private java.math.BigDecimal _originalAmount;
    
    /* 调整后值: ADJUSTED_AMOUNT */
    private java.math.BigDecimal _adjustedAmount;
    
    /* 调整原因: ADJUSTMENT_REASON */
    private java.lang.String _adjustmentReason;
    
    /* 调整人: ADJUSTED_BY */
    private java.lang.String _adjustedBy;
    
    /* 调整时间: ADJUSTED_AT */
    private java.sql.Timestamp _adjustedAt;
    
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
    

    public _ErpHrSalarySimulationItemAdjustment(){
        // for debug
    }

    protected ErpHrSalarySimulationItemAdjustment newInstance(){
        ErpHrSalarySimulationItemAdjustment entity = new ErpHrSalarySimulationItemAdjustment();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrSalarySimulationItemAdjustment cloneInstance() {
        ErpHrSalarySimulationItemAdjustment entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment";
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
        
            case PROP_ID_simulationId:
               return getSimulationId();
        
            case PROP_ID_employeeId:
               return getEmployeeId();
        
            case PROP_ID_salaryItemCode:
               return getSalaryItemCode();
        
            case PROP_ID_originalAmount:
               return getOriginalAmount();
        
            case PROP_ID_adjustedAmount:
               return getAdjustedAmount();
        
            case PROP_ID_adjustmentReason:
               return getAdjustmentReason();
        
            case PROP_ID_adjustedBy:
               return getAdjustedBy();
        
            case PROP_ID_adjustedAt:
               return getAdjustedAt();
        
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
        
            case PROP_ID_simulationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_simulationId));
               }
               setSimulationId(typedValue);
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
        
            case PROP_ID_salaryItemCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_salaryItemCode));
               }
               setSalaryItemCode(typedValue);
               break;
            }
        
            case PROP_ID_originalAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_originalAmount));
               }
               setOriginalAmount(typedValue);
               break;
            }
        
            case PROP_ID_adjustedAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_adjustedAmount));
               }
               setAdjustedAmount(typedValue);
               break;
            }
        
            case PROP_ID_adjustmentReason:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_adjustmentReason));
               }
               setAdjustmentReason(typedValue);
               break;
            }
        
            case PROP_ID_adjustedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_adjustedBy));
               }
               setAdjustedBy(typedValue);
               break;
            }
        
            case PROP_ID_adjustedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_adjustedAt));
               }
               setAdjustedAt(typedValue);
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
        
            case PROP_ID_simulationId:{
               onInitProp(propId);
               this._simulationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_employeeId:{
               onInitProp(propId);
               this._employeeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_salaryItemCode:{
               onInitProp(propId);
               this._salaryItemCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_originalAmount:{
               onInitProp(propId);
               this._originalAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_adjustedAmount:{
               onInitProp(propId);
               this._adjustedAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_adjustmentReason:{
               onInitProp(propId);
               this._adjustmentReason = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_adjustedBy:{
               onInitProp(propId);
               this._adjustedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_adjustedAt:{
               onInitProp(propId);
               this._adjustedAt = (java.sql.Timestamp)value;
               
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
     * 模拟: SIMULATION_ID
     */
    public final java.lang.Long getSimulationId(){
         onPropGet(PROP_ID_simulationId);
         return _simulationId;
    }

    /**
     * 模拟: SIMULATION_ID
     */
    public final void setSimulationId(java.lang.Long value){
        if(onPropSet(PROP_ID_simulationId,value)){
            this._simulationId = value;
            internalClearRefs(PROP_ID_simulationId);
            
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
     * 薪酬项目编码: SALARY_ITEM_CODE
     */
    public final java.lang.String getSalaryItemCode(){
         onPropGet(PROP_ID_salaryItemCode);
         return _salaryItemCode;
    }

    /**
     * 薪酬项目编码: SALARY_ITEM_CODE
     */
    public final void setSalaryItemCode(java.lang.String value){
        if(onPropSet(PROP_ID_salaryItemCode,value)){
            this._salaryItemCode = value;
            internalClearRefs(PROP_ID_salaryItemCode);
            
        }
    }
    
    /**
     * 源值: ORIGINAL_AMOUNT
     */
    public final java.math.BigDecimal getOriginalAmount(){
         onPropGet(PROP_ID_originalAmount);
         return _originalAmount;
    }

    /**
     * 源值: ORIGINAL_AMOUNT
     */
    public final void setOriginalAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_originalAmount,value)){
            this._originalAmount = value;
            internalClearRefs(PROP_ID_originalAmount);
            
        }
    }
    
    /**
     * 调整后值: ADJUSTED_AMOUNT
     */
    public final java.math.BigDecimal getAdjustedAmount(){
         onPropGet(PROP_ID_adjustedAmount);
         return _adjustedAmount;
    }

    /**
     * 调整后值: ADJUSTED_AMOUNT
     */
    public final void setAdjustedAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_adjustedAmount,value)){
            this._adjustedAmount = value;
            internalClearRefs(PROP_ID_adjustedAmount);
            
        }
    }
    
    /**
     * 调整原因: ADJUSTMENT_REASON
     */
    public final java.lang.String getAdjustmentReason(){
         onPropGet(PROP_ID_adjustmentReason);
         return _adjustmentReason;
    }

    /**
     * 调整原因: ADJUSTMENT_REASON
     */
    public final void setAdjustmentReason(java.lang.String value){
        if(onPropSet(PROP_ID_adjustmentReason,value)){
            this._adjustmentReason = value;
            internalClearRefs(PROP_ID_adjustmentReason);
            
        }
    }
    
    /**
     * 调整人: ADJUSTED_BY
     */
    public final java.lang.String getAdjustedBy(){
         onPropGet(PROP_ID_adjustedBy);
         return _adjustedBy;
    }

    /**
     * 调整人: ADJUSTED_BY
     */
    public final void setAdjustedBy(java.lang.String value){
        if(onPropSet(PROP_ID_adjustedBy,value)){
            this._adjustedBy = value;
            internalClearRefs(PROP_ID_adjustedBy);
            
        }
    }
    
    /**
     * 调整时间: ADJUSTED_AT
     */
    public final java.sql.Timestamp getAdjustedAt(){
         onPropGet(PROP_ID_adjustedAt);
         return _adjustedAt;
    }

    /**
     * 调整时间: ADJUSTED_AT
     */
    public final void setAdjustedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_adjustedAt,value)){
            this._adjustedAt = value;
            internalClearRefs(PROP_ID_adjustedAt);
            
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
    public final app.erp.hr.dao.entity.ErpHrSalarySimulation getSimulation(){
       return (app.erp.hr.dao.entity.ErpHrSalarySimulation)internalGetRefEntity(PROP_NAME_simulation);
    }

    public final void setSimulation(app.erp.hr.dao.entity.ErpHrSalarySimulation refEntity){
   
           if(refEntity == null){
           
                   this.setSimulationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_simulation, refEntity,()->{
           
                           this.setSimulationId(refEntity.getId());
                       
           });
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
