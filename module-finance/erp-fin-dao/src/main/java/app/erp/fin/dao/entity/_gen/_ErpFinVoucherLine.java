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

import app.erp.fin.dao.entity.ErpFinVoucherLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  凭证分录行: erp_fin_voucher_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinVoucherLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 凭证ID: VOUCHER_ID BIGINT */
    public static final String PROP_NAME_voucherId = "voucherId";
    public static final int PROP_ID_voucherId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 科目ID: SUBJECT_ID BIGINT */
    public static final String PROP_NAME_subjectId = "subjectId";
    public static final int PROP_ID_subjectId = 4;
    
    /* 科目编码: SUBJECT_CODE VARCHAR */
    public static final String PROP_NAME_subjectCode = "subjectCode";
    public static final int PROP_ID_subjectCode = 5;
    
    /* 科目名称: SUBJECT_NAME VARCHAR */
    public static final String PROP_NAME_subjectName = "subjectName";
    public static final int PROP_ID_subjectName = 6;
    
    /* 借贷方向: DC_DIRECTION INTEGER */
    public static final String PROP_NAME_dcDirection = "dcDirection";
    public static final int PROP_ID_dcDirection = 7;
    
    /* 借方金额(本位币): DEBIT_AMOUNT DECIMAL */
    public static final String PROP_NAME_debitAmount = "debitAmount";
    public static final int PROP_ID_debitAmount = 8;
    
    /* 贷方金额(本位币): CREDIT_AMOUNT DECIMAL */
    public static final String PROP_NAME_creditAmount = "creditAmount";
    public static final int PROP_ID_creditAmount = 9;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 10;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 11;
    
    /* 源币种金额: AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_amountSource = "amountSource";
    public static final int PROP_ID_amountSource = 12;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_amountFunctional = "amountFunctional";
    public static final int PROP_ID_amountFunctional = 13;
    
    /* 账套: ACCT_SCHEMA_ID BIGINT */
    public static final String PROP_NAME_acctSchemaId = "acctSchemaId";
    public static final int PROP_ID_acctSchemaId = 14;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 15;
    
    /* 摘要: MEMO VARCHAR */
    public static final String PROP_NAME_memo = "memo";
    public static final int PROP_ID_memo = 16;
    
    /* 辅助-往来单位: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 17;
    
    /* 辅助-部门: DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_departmentId = "departmentId";
    public static final int PROP_ID_departmentId = 18;
    
    /* 辅助-项目: PROJECT_ID BIGINT */
    public static final String PROP_NAME_projectId = "projectId";
    public static final int PROP_ID_projectId = 19;
    
    /* 辅助-仓库: WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_warehouseId = "warehouseId";
    public static final int PROP_ID_warehouseId = 20;
    
    /* 辅助-物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 21;
    
    /* 业务类型: BUSINESS_TYPE INTEGER */
    public static final String PROP_NAME_businessType = "businessType";
    public static final int PROP_ID_businessType = 22;
    
    /* 成本中心: COST_CENTER_ID BIGINT */
    public static final String PROP_NAME_costCenterId = "costCenterId";
    public static final int PROP_ID_costCenterId = 23;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 24;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 25;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 26;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 27;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 28;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 29;
    

    private static int _PROP_ID_BOUND = 30;

    
    /* relation:  */
    public static final String PROP_NAME_voucher = "voucher";
    
    /* relation:  */
    public static final String PROP_NAME_subject = "subject";
    
    /* relation:  */
    public static final String PROP_NAME_acctSchema = "acctSchema";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    
    /* relation:  */
    public static final String PROP_NAME_department = "department";
    
    /* relation:  */
    public static final String PROP_NAME_warehouse = "warehouse";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_project = "project";
    
    /* relation:  */
    public static final String PROP_NAME_costCenter = "costCenter";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[30];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_voucherId] = PROP_NAME_voucherId;
          PROP_NAME_TO_ID.put(PROP_NAME_voucherId, PROP_ID_voucherId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_subjectId] = PROP_NAME_subjectId;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectId, PROP_ID_subjectId);
      
          PROP_ID_TO_NAME[PROP_ID_subjectCode] = PROP_NAME_subjectCode;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectCode, PROP_ID_subjectCode);
      
          PROP_ID_TO_NAME[PROP_ID_subjectName] = PROP_NAME_subjectName;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectName, PROP_ID_subjectName);
      
          PROP_ID_TO_NAME[PROP_ID_dcDirection] = PROP_NAME_dcDirection;
          PROP_NAME_TO_ID.put(PROP_NAME_dcDirection, PROP_ID_dcDirection);
      
          PROP_ID_TO_NAME[PROP_ID_debitAmount] = PROP_NAME_debitAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_debitAmount, PROP_ID_debitAmount);
      
          PROP_ID_TO_NAME[PROP_ID_creditAmount] = PROP_NAME_creditAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_creditAmount, PROP_ID_creditAmount);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
          PROP_ID_TO_NAME[PROP_ID_acctSchemaId] = PROP_NAME_acctSchemaId;
          PROP_NAME_TO_ID.put(PROP_NAME_acctSchemaId, PROP_ID_acctSchemaId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_memo] = PROP_NAME_memo;
          PROP_NAME_TO_ID.put(PROP_NAME_memo, PROP_ID_memo);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_departmentId] = PROP_NAME_departmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_departmentId, PROP_ID_departmentId);
      
          PROP_ID_TO_NAME[PROP_ID_projectId] = PROP_NAME_projectId;
          PROP_NAME_TO_ID.put(PROP_NAME_projectId, PROP_ID_projectId);
      
          PROP_ID_TO_NAME[PROP_ID_warehouseId] = PROP_NAME_warehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_warehouseId, PROP_ID_warehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_businessType] = PROP_NAME_businessType;
          PROP_NAME_TO_ID.put(PROP_NAME_businessType, PROP_ID_businessType);
      
          PROP_ID_TO_NAME[PROP_ID_costCenterId] = PROP_NAME_costCenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_costCenterId, PROP_ID_costCenterId);
      
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
    
    /* 凭证ID: VOUCHER_ID */
    private java.lang.Long _voucherId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 科目ID: SUBJECT_ID */
    private java.lang.Long _subjectId;
    
    /* 科目编码: SUBJECT_CODE */
    private java.lang.String _subjectCode;
    
    /* 科目名称: SUBJECT_NAME */
    private java.lang.String _subjectName;
    
    /* 借贷方向: DC_DIRECTION */
    private java.lang.Integer _dcDirection;
    
    /* 借方金额(本位币): DEBIT_AMOUNT */
    private java.lang.String _debitAmount;
    
    /* 贷方金额(本位币): CREDIT_AMOUNT */
    private java.lang.String _creditAmount;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.lang.String _exchangeRate;
    
    /* 源币种金额: AMOUNT_SOURCE */
    private java.lang.String _amountSource;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL */
    private java.lang.String _amountFunctional;
    
    /* 账套: ACCT_SCHEMA_ID */
    private java.lang.Long _acctSchemaId;
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 摘要: MEMO */
    private java.lang.String _memo;
    
    /* 辅助-往来单位: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 辅助-部门: DEPARTMENT_ID */
    private java.lang.Long _departmentId;
    
    /* 辅助-项目: PROJECT_ID */
    private java.lang.Long _projectId;
    
    /* 辅助-仓库: WAREHOUSE_ID */
    private java.lang.Long _warehouseId;
    
    /* 辅助-物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 业务类型: BUSINESS_TYPE */
    private java.lang.Integer _businessType;
    
    /* 成本中心: COST_CENTER_ID */
    private java.lang.Long _costCenterId;
    
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
    

    public _ErpFinVoucherLine(){
        // for debug
    }

    protected ErpFinVoucherLine newInstance(){
        ErpFinVoucherLine entity = new ErpFinVoucherLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinVoucherLine cloneInstance() {
        ErpFinVoucherLine entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinVoucherLine";
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
        
            case PROP_ID_voucherId:
               return getVoucherId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_subjectId:
               return getSubjectId();
        
            case PROP_ID_subjectCode:
               return getSubjectCode();
        
            case PROP_ID_subjectName:
               return getSubjectName();
        
            case PROP_ID_dcDirection:
               return getDcDirection();
        
            case PROP_ID_debitAmount:
               return getDebitAmount();
        
            case PROP_ID_creditAmount:
               return getCreditAmount();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
            case PROP_ID_acctSchemaId:
               return getAcctSchemaId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_memo:
               return getMemo();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_departmentId:
               return getDepartmentId();
        
            case PROP_ID_projectId:
               return getProjectId();
        
            case PROP_ID_warehouseId:
               return getWarehouseId();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_businessType:
               return getBusinessType();
        
            case PROP_ID_costCenterId:
               return getCostCenterId();
        
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
        
            case PROP_ID_voucherId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_voucherId));
               }
               setVoucherId(typedValue);
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
        
            case PROP_ID_subjectName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subjectName));
               }
               setSubjectName(typedValue);
               break;
            }
        
            case PROP_ID_dcDirection:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_dcDirection));
               }
               setDcDirection(typedValue);
               break;
            }
        
            case PROP_ID_debitAmount:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_debitAmount));
               }
               setDebitAmount(typedValue);
               break;
            }
        
            case PROP_ID_creditAmount:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_creditAmount));
               }
               setCreditAmount(typedValue);
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
        
            case PROP_ID_exchangeRate:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_exchangeRate));
               }
               setExchangeRate(typedValue);
               break;
            }
        
            case PROP_ID_amountSource:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountSource));
               }
               setAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_amountFunctional:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountFunctional));
               }
               setAmountFunctional(typedValue);
               break;
            }
        
            case PROP_ID_acctSchemaId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_acctSchemaId));
               }
               setAcctSchemaId(typedValue);
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
        
            case PROP_ID_memo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_memo));
               }
               setMemo(typedValue);
               break;
            }
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_departmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_departmentId));
               }
               setDepartmentId(typedValue);
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
        
            case PROP_ID_warehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_warehouseId));
               }
               setWarehouseId(typedValue);
               break;
            }
        
            case PROP_ID_materialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialId));
               }
               setMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_businessType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_businessType));
               }
               setBusinessType(typedValue);
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
        
            case PROP_ID_voucherId:{
               onInitProp(propId);
               this._voucherId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
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
        
            case PROP_ID_subjectName:{
               onInitProp(propId);
               this._subjectName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_dcDirection:{
               onInitProp(propId);
               this._dcDirection = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_debitAmount:{
               onInitProp(propId);
               this._debitAmount = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_creditAmount:{
               onInitProp(propId);
               this._creditAmount = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_exchangeRate:{
               onInitProp(propId);
               this._exchangeRate = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountSource:{
               onInitProp(propId);
               this._amountSource = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountFunctional:{
               onInitProp(propId);
               this._amountFunctional = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_acctSchemaId:{
               onInitProp(propId);
               this._acctSchemaId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_memo:{
               onInitProp(propId);
               this._memo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_departmentId:{
               onInitProp(propId);
               this._departmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_projectId:{
               onInitProp(propId);
               this._projectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_warehouseId:{
               onInitProp(propId);
               this._warehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_businessType:{
               onInitProp(propId);
               this._businessType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_costCenterId:{
               onInitProp(propId);
               this._costCenterId = (java.lang.Long)value;
               
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
     * 凭证ID: VOUCHER_ID
     */
    public final java.lang.Long getVoucherId(){
         onPropGet(PROP_ID_voucherId);
         return _voucherId;
    }

    /**
     * 凭证ID: VOUCHER_ID
     */
    public final void setVoucherId(java.lang.Long value){
        if(onPropSet(PROP_ID_voucherId,value)){
            this._voucherId = value;
            internalClearRefs(PROP_ID_voucherId);
            
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
     * 科目ID: SUBJECT_ID
     */
    public final java.lang.Long getSubjectId(){
         onPropGet(PROP_ID_subjectId);
         return _subjectId;
    }

    /**
     * 科目ID: SUBJECT_ID
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
     * 科目名称: SUBJECT_NAME
     */
    public final java.lang.String getSubjectName(){
         onPropGet(PROP_ID_subjectName);
         return _subjectName;
    }

    /**
     * 科目名称: SUBJECT_NAME
     */
    public final void setSubjectName(java.lang.String value){
        if(onPropSet(PROP_ID_subjectName,value)){
            this._subjectName = value;
            internalClearRefs(PROP_ID_subjectName);
            
        }
    }
    
    /**
     * 借贷方向: DC_DIRECTION
     */
    public final java.lang.Integer getDcDirection(){
         onPropGet(PROP_ID_dcDirection);
         return _dcDirection;
    }

    /**
     * 借贷方向: DC_DIRECTION
     */
    public final void setDcDirection(java.lang.Integer value){
        if(onPropSet(PROP_ID_dcDirection,value)){
            this._dcDirection = value;
            internalClearRefs(PROP_ID_dcDirection);
            
        }
    }
    
    /**
     * 借方金额(本位币): DEBIT_AMOUNT
     */
    public final java.lang.String getDebitAmount(){
         onPropGet(PROP_ID_debitAmount);
         return _debitAmount;
    }

    /**
     * 借方金额(本位币): DEBIT_AMOUNT
     */
    public final void setDebitAmount(java.lang.String value){
        if(onPropSet(PROP_ID_debitAmount,value)){
            this._debitAmount = value;
            internalClearRefs(PROP_ID_debitAmount);
            
        }
    }
    
    /**
     * 贷方金额(本位币): CREDIT_AMOUNT
     */
    public final java.lang.String getCreditAmount(){
         onPropGet(PROP_ID_creditAmount);
         return _creditAmount;
    }

    /**
     * 贷方金额(本位币): CREDIT_AMOUNT
     */
    public final void setCreditAmount(java.lang.String value){
        if(onPropSet(PROP_ID_creditAmount,value)){
            this._creditAmount = value;
            internalClearRefs(PROP_ID_creditAmount);
            
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
     * 汇率: EXCHANGE_RATE
     */
    public final java.lang.String getExchangeRate(){
         onPropGet(PROP_ID_exchangeRate);
         return _exchangeRate;
    }

    /**
     * 汇率: EXCHANGE_RATE
     */
    public final void setExchangeRate(java.lang.String value){
        if(onPropSet(PROP_ID_exchangeRate,value)){
            this._exchangeRate = value;
            internalClearRefs(PROP_ID_exchangeRate);
            
        }
    }
    
    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final java.lang.String getAmountSource(){
         onPropGet(PROP_ID_amountSource);
         return _amountSource;
    }

    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final void setAmountSource(java.lang.String value){
        if(onPropSet(PROP_ID_amountSource,value)){
            this._amountSource = value;
            internalClearRefs(PROP_ID_amountSource);
            
        }
    }
    
    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final java.lang.String getAmountFunctional(){
         onPropGet(PROP_ID_amountFunctional);
         return _amountFunctional;
    }

    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final void setAmountFunctional(java.lang.String value){
        if(onPropSet(PROP_ID_amountFunctional,value)){
            this._amountFunctional = value;
            internalClearRefs(PROP_ID_amountFunctional);
            
        }
    }
    
    /**
     * 账套: ACCT_SCHEMA_ID
     */
    public final java.lang.Long getAcctSchemaId(){
         onPropGet(PROP_ID_acctSchemaId);
         return _acctSchemaId;
    }

    /**
     * 账套: ACCT_SCHEMA_ID
     */
    public final void setAcctSchemaId(java.lang.Long value){
        if(onPropSet(PROP_ID_acctSchemaId,value)){
            this._acctSchemaId = value;
            internalClearRefs(PROP_ID_acctSchemaId);
            
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
     * 摘要: MEMO
     */
    public final java.lang.String getMemo(){
         onPropGet(PROP_ID_memo);
         return _memo;
    }

    /**
     * 摘要: MEMO
     */
    public final void setMemo(java.lang.String value){
        if(onPropSet(PROP_ID_memo,value)){
            this._memo = value;
            internalClearRefs(PROP_ID_memo);
            
        }
    }
    
    /**
     * 辅助-往来单位: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 辅助-往来单位: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 辅助-部门: DEPARTMENT_ID
     */
    public final java.lang.Long getDepartmentId(){
         onPropGet(PROP_ID_departmentId);
         return _departmentId;
    }

    /**
     * 辅助-部门: DEPARTMENT_ID
     */
    public final void setDepartmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_departmentId,value)){
            this._departmentId = value;
            internalClearRefs(PROP_ID_departmentId);
            
        }
    }
    
    /**
     * 辅助-项目: PROJECT_ID
     */
    public final java.lang.Long getProjectId(){
         onPropGet(PROP_ID_projectId);
         return _projectId;
    }

    /**
     * 辅助-项目: PROJECT_ID
     */
    public final void setProjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_projectId,value)){
            this._projectId = value;
            internalClearRefs(PROP_ID_projectId);
            
        }
    }
    
    /**
     * 辅助-仓库: WAREHOUSE_ID
     */
    public final java.lang.Long getWarehouseId(){
         onPropGet(PROP_ID_warehouseId);
         return _warehouseId;
    }

    /**
     * 辅助-仓库: WAREHOUSE_ID
     */
    public final void setWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_warehouseId,value)){
            this._warehouseId = value;
            internalClearRefs(PROP_ID_warehouseId);
            
        }
    }
    
    /**
     * 辅助-物料: MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 辅助-物料: MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
        }
    }
    
    /**
     * 业务类型: BUSINESS_TYPE
     */
    public final java.lang.Integer getBusinessType(){
         onPropGet(PROP_ID_businessType);
         return _businessType;
    }

    /**
     * 业务类型: BUSINESS_TYPE
     */
    public final void setBusinessType(java.lang.Integer value){
        if(onPropSet(PROP_ID_businessType,value)){
            this._businessType = value;
            internalClearRefs(PROP_ID_businessType);
            
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
    public final app.erp.fin.dao.entity.ErpFinVoucher getVoucher(){
       return (app.erp.fin.dao.entity.ErpFinVoucher)internalGetRefEntity(PROP_NAME_voucher);
    }

    public final void setVoucher(app.erp.fin.dao.entity.ErpFinVoucher refEntity){
   
           if(refEntity == null){
           
                   this.setVoucherId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_voucher, refEntity,()->{
           
                           this.setVoucherId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdAcctSchema getAcctSchema(){
       return (app.erp.md.dao.entity.ErpMdAcctSchema)internalGetRefEntity(PROP_NAME_acctSchema);
    }

    public final void setAcctSchema(app.erp.md.dao.entity.ErpMdAcctSchema refEntity){
   
           if(refEntity == null){
           
                   this.setAcctSchemaId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_acctSchema, refEntity,()->{
           
                           this.setAcctSchemaId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCurrency getCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_currency);
    }

    public final void setCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_currency, refEntity,()->{
           
                           this.setCurrencyId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getPartner(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_partner);
    }

    public final void setPartner(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_partner, refEntity,()->{
           
                           this.setPartnerId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdOrganization getDepartment(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_department);
    }

    public final void setDepartment(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setDepartmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_department, refEntity,()->{
           
                           this.setDepartmentId(refEntity.getId());
                       
           });
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
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterial getMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_material);
    }

    public final void setMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_material, refEntity,()->{
           
                           this.setMaterialId(refEntity.getId());
                       
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
       
}
// resume CPD analysis - CPD-ON
