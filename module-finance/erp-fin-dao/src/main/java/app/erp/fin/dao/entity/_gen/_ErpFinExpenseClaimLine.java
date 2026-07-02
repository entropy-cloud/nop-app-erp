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

import app.erp.fin.dao.entity.ErpFinExpenseClaimLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  费用报销单行: erp_fin_expense_claim_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinExpenseClaimLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 报销单ID: CLAIM_ID BIGINT */
    public static final String PROP_NAME_claimId = "claimId";
    public static final int PROP_ID_claimId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 费用类型: EXPENSE_TYPE INTEGER */
    public static final String PROP_NAME_expenseType = "expenseType";
    public static final int PROP_ID_expenseType = 4;
    
    /* 项目: PROJECT_ID BIGINT */
    public static final String PROP_NAME_projectId = "projectId";
    public static final int PROP_ID_projectId = 5;
    
    /* 成本中心: COST_CENTER_ID BIGINT */
    public static final String PROP_NAME_costCenterId = "costCenterId";
    public static final int PROP_ID_costCenterId = 6;
    
    /* 费用科目: SUBJECT_ID BIGINT */
    public static final String PROP_NAME_subjectId = "subjectId";
    public static final int PROP_ID_subjectId = 7;
    
    /* 科目编码: SUBJECT_CODE VARCHAR */
    public static final String PROP_NAME_subjectCode = "subjectCode";
    public static final int PROP_ID_subjectCode = 8;
    
    /* 不含税金额: AMOUNT_WITHOUT_TAX DECIMAL */
    public static final String PROP_NAME_amountWithoutTax = "amountWithoutTax";
    public static final int PROP_ID_amountWithoutTax = 9;
    
    /* 税率: TAX_RATE DECIMAL */
    public static final String PROP_NAME_taxRate = "taxRate";
    public static final int PROP_ID_taxRate = 10;
    
    /* 税额: TAX_AMOUNT DECIMAL */
    public static final String PROP_NAME_taxAmount = "taxAmount";
    public static final int PROP_ID_taxAmount = 11;
    
    /* 价税合计: AMOUNT_WITH_TAX DECIMAL */
    public static final String PROP_NAME_amountWithTax = "amountWithTax";
    public static final int PROP_ID_amountWithTax = 12;
    
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
    public static final String PROP_NAME_claim = "claim";
    
    /* relation:  */
    public static final String PROP_NAME_project = "project";
    
    /* relation:  */
    public static final String PROP_NAME_costCenter = "costCenter";
    
    /* relation:  */
    public static final String PROP_NAME_subject = "subject";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_claimId] = PROP_NAME_claimId;
          PROP_NAME_TO_ID.put(PROP_NAME_claimId, PROP_ID_claimId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_expenseType] = PROP_NAME_expenseType;
          PROP_NAME_TO_ID.put(PROP_NAME_expenseType, PROP_ID_expenseType);
      
          PROP_ID_TO_NAME[PROP_ID_projectId] = PROP_NAME_projectId;
          PROP_NAME_TO_ID.put(PROP_NAME_projectId, PROP_ID_projectId);
      
          PROP_ID_TO_NAME[PROP_ID_costCenterId] = PROP_NAME_costCenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_costCenterId, PROP_ID_costCenterId);
      
          PROP_ID_TO_NAME[PROP_ID_subjectId] = PROP_NAME_subjectId;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectId, PROP_ID_subjectId);
      
          PROP_ID_TO_NAME[PROP_ID_subjectCode] = PROP_NAME_subjectCode;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectCode, PROP_ID_subjectCode);
      
          PROP_ID_TO_NAME[PROP_ID_amountWithoutTax] = PROP_NAME_amountWithoutTax;
          PROP_NAME_TO_ID.put(PROP_NAME_amountWithoutTax, PROP_ID_amountWithoutTax);
      
          PROP_ID_TO_NAME[PROP_ID_taxRate] = PROP_NAME_taxRate;
          PROP_NAME_TO_ID.put(PROP_NAME_taxRate, PROP_ID_taxRate);
      
          PROP_ID_TO_NAME[PROP_ID_taxAmount] = PROP_NAME_taxAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_taxAmount, PROP_ID_taxAmount);
      
          PROP_ID_TO_NAME[PROP_ID_amountWithTax] = PROP_NAME_amountWithTax;
          PROP_NAME_TO_ID.put(PROP_NAME_amountWithTax, PROP_ID_amountWithTax);
      
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
    
    /* 报销单ID: CLAIM_ID */
    private java.lang.Long _claimId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 费用类型: EXPENSE_TYPE */
    private java.lang.Integer _expenseType;
    
    /* 项目: PROJECT_ID */
    private java.lang.Long _projectId;
    
    /* 成本中心: COST_CENTER_ID */
    private java.lang.Long _costCenterId;
    
    /* 费用科目: SUBJECT_ID */
    private java.lang.Long _subjectId;
    
    /* 科目编码: SUBJECT_CODE */
    private java.lang.String _subjectCode;
    
    /* 不含税金额: AMOUNT_WITHOUT_TAX */
    private java.math.BigDecimal _amountWithoutTax;
    
    /* 税率: TAX_RATE */
    private java.math.BigDecimal _taxRate;
    
    /* 税额: TAX_AMOUNT */
    private java.math.BigDecimal _taxAmount;
    
    /* 价税合计: AMOUNT_WITH_TAX */
    private java.math.BigDecimal _amountWithTax;
    
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
    

    public _ErpFinExpenseClaimLine(){
        // for debug
    }

    protected ErpFinExpenseClaimLine newInstance(){
        ErpFinExpenseClaimLine entity = new ErpFinExpenseClaimLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinExpenseClaimLine cloneInstance() {
        ErpFinExpenseClaimLine entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinExpenseClaimLine";
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
        
            case PROP_ID_claimId:
               return getClaimId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_expenseType:
               return getExpenseType();
        
            case PROP_ID_projectId:
               return getProjectId();
        
            case PROP_ID_costCenterId:
               return getCostCenterId();
        
            case PROP_ID_subjectId:
               return getSubjectId();
        
            case PROP_ID_subjectCode:
               return getSubjectCode();
        
            case PROP_ID_amountWithoutTax:
               return getAmountWithoutTax();
        
            case PROP_ID_taxRate:
               return getTaxRate();
        
            case PROP_ID_taxAmount:
               return getTaxAmount();
        
            case PROP_ID_amountWithTax:
               return getAmountWithTax();
        
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
        
            case PROP_ID_claimId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_claimId));
               }
               setClaimId(typedValue);
               break;
            }
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
               break;
            }
        
            case PROP_ID_expenseType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_expenseType));
               }
               setExpenseType(typedValue);
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
        
            case PROP_ID_costCenterId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_costCenterId));
               }
               setCostCenterId(typedValue);
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
        
            case PROP_ID_subjectCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subjectCode));
               }
               setSubjectCode(typedValue);
               break;
            }
        
            case PROP_ID_amountWithoutTax:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountWithoutTax));
               }
               setAmountWithoutTax(typedValue);
               break;
            }
        
            case PROP_ID_taxRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_taxRate));
               }
               setTaxRate(typedValue);
               break;
            }
        
            case PROP_ID_taxAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_taxAmount));
               }
               setTaxAmount(typedValue);
               break;
            }
        
            case PROP_ID_amountWithTax:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountWithTax));
               }
               setAmountWithTax(typedValue);
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
        
            case PROP_ID_claimId:{
               onInitProp(propId);
               this._claimId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_expenseType:{
               onInitProp(propId);
               this._expenseType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_projectId:{
               onInitProp(propId);
               this._projectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_costCenterId:{
               onInitProp(propId);
               this._costCenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_subjectId:{
               onInitProp(propId);
               this._subjectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_subjectCode:{
               onInitProp(propId);
               this._subjectCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountWithoutTax:{
               onInitProp(propId);
               this._amountWithoutTax = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_taxRate:{
               onInitProp(propId);
               this._taxRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_taxAmount:{
               onInitProp(propId);
               this._taxAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amountWithTax:{
               onInitProp(propId);
               this._amountWithTax = (java.math.BigDecimal)value;
               
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
     * 报销单ID: CLAIM_ID
     */
    public final java.lang.Long getClaimId(){
         onPropGet(PROP_ID_claimId);
         return _claimId;
    }

    /**
     * 报销单ID: CLAIM_ID
     */
    public final void setClaimId(java.lang.Long value){
        if(onPropSet(PROP_ID_claimId,value)){
            this._claimId = value;
            internalClearRefs(PROP_ID_claimId);
            
        }
    }
    
    /**
     * 行号: LINE_NO
     */
    public final java.lang.Integer getLineNo(){
         onPropGet(PROP_ID_lineNo);
         return _lineNo;
    }

    /**
     * 行号: LINE_NO
     */
    public final void setLineNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_lineNo,value)){
            this._lineNo = value;
            internalClearRefs(PROP_ID_lineNo);
            
        }
    }
    
    /**
     * 费用类型: EXPENSE_TYPE
     */
    public final java.lang.Integer getExpenseType(){
         onPropGet(PROP_ID_expenseType);
         return _expenseType;
    }

    /**
     * 费用类型: EXPENSE_TYPE
     */
    public final void setExpenseType(java.lang.Integer value){
        if(onPropSet(PROP_ID_expenseType,value)){
            this._expenseType = value;
            internalClearRefs(PROP_ID_expenseType);
            
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
     * 成本中心: COST_CENTER_ID
     */
    public final java.lang.Long getCostCenterId(){
         onPropGet(PROP_ID_costCenterId);
         return _costCenterId;
    }

    /**
     * 成本中心: COST_CENTER_ID
     */
    public final void setCostCenterId(java.lang.Long value){
        if(onPropSet(PROP_ID_costCenterId,value)){
            this._costCenterId = value;
            internalClearRefs(PROP_ID_costCenterId);
            
        }
    }
    
    /**
     * 费用科目: SUBJECT_ID
     */
    public final java.lang.Long getSubjectId(){
         onPropGet(PROP_ID_subjectId);
         return _subjectId;
    }

    /**
     * 费用科目: SUBJECT_ID
     */
    public final void setSubjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_subjectId,value)){
            this._subjectId = value;
            internalClearRefs(PROP_ID_subjectId);
            
        }
    }
    
    /**
     * 科目编码: SUBJECT_CODE
     */
    public final java.lang.String getSubjectCode(){
         onPropGet(PROP_ID_subjectCode);
         return _subjectCode;
    }

    /**
     * 科目编码: SUBJECT_CODE
     */
    public final void setSubjectCode(java.lang.String value){
        if(onPropSet(PROP_ID_subjectCode,value)){
            this._subjectCode = value;
            internalClearRefs(PROP_ID_subjectCode);
            
        }
    }
    
    /**
     * 不含税金额: AMOUNT_WITHOUT_TAX
     */
    public final java.math.BigDecimal getAmountWithoutTax(){
         onPropGet(PROP_ID_amountWithoutTax);
         return _amountWithoutTax;
    }

    /**
     * 不含税金额: AMOUNT_WITHOUT_TAX
     */
    public final void setAmountWithoutTax(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountWithoutTax,value)){
            this._amountWithoutTax = value;
            internalClearRefs(PROP_ID_amountWithoutTax);
            
        }
    }
    
    /**
     * 税率: TAX_RATE
     */
    public final java.math.BigDecimal getTaxRate(){
         onPropGet(PROP_ID_taxRate);
         return _taxRate;
    }

    /**
     * 税率: TAX_RATE
     */
    public final void setTaxRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_taxRate,value)){
            this._taxRate = value;
            internalClearRefs(PROP_ID_taxRate);
            
        }
    }
    
    /**
     * 税额: TAX_AMOUNT
     */
    public final java.math.BigDecimal getTaxAmount(){
         onPropGet(PROP_ID_taxAmount);
         return _taxAmount;
    }

    /**
     * 税额: TAX_AMOUNT
     */
    public final void setTaxAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_taxAmount,value)){
            this._taxAmount = value;
            internalClearRefs(PROP_ID_taxAmount);
            
        }
    }
    
    /**
     * 价税合计: AMOUNT_WITH_TAX
     */
    public final java.math.BigDecimal getAmountWithTax(){
         onPropGet(PROP_ID_amountWithTax);
         return _amountWithTax;
    }

    /**
     * 价税合计: AMOUNT_WITH_TAX
     */
    public final void setAmountWithTax(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountWithTax,value)){
            this._amountWithTax = value;
            internalClearRefs(PROP_ID_amountWithTax);
            
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
    public final app.erp.fin.dao.entity.ErpFinExpenseClaim getClaim(){
       return (app.erp.fin.dao.entity.ErpFinExpenseClaim)internalGetRefEntity(PROP_NAME_claim);
    }

    public final void setClaim(app.erp.fin.dao.entity.ErpFinExpenseClaim refEntity){
   
           if(refEntity == null){
           
                   this.setClaimId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_claim, refEntity,()->{
           
                           this.setClaimId(refEntity.getId());
                       
           });
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
       
}
// resume CPD analysis - CPD-ON
