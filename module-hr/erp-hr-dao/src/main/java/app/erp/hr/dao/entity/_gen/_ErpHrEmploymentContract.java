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

import app.erp.hr.dao.entity.ErpHrEmploymentContract;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  劳动合同: erp_hr_employment_contract
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrEmploymentContract extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 合同编号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 员工: EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_employeeId = "employeeId";
    public static final int PROP_ID_employeeId = 3;
    
    /* 合同类型: CONTRACT_TYPE VARCHAR */
    public static final String PROP_NAME_contractType = "contractType";
    public static final int PROP_ID_contractType = 4;
    
    /* 签订日期: SIGN_DATE DATE */
    public static final String PROP_NAME_signDate = "signDate";
    public static final int PROP_ID_signDate = 5;
    
    /* 生效日期: START_DATE DATE */
    public static final String PROP_NAME_startDate = "startDate";
    public static final int PROP_ID_startDate = 6;
    
    /* 到期日期: END_DATE DATE */
    public static final String PROP_NAME_endDate = "endDate";
    public static final int PROP_ID_endDate = 7;
    
    /* 试用期月数: PROBATION_MONTHS INTEGER */
    public static final String PROP_NAME_probationMonths = "probationMonths";
    public static final int PROP_ID_probationMonths = 8;
    
    /* 每周工时: WORKING_HOURS_PER_WEEK DECIMAL */
    public static final String PROP_NAME_workingHoursPerWeek = "workingHoursPerWeek";
    public static final int PROP_ID_workingHoursPerWeek = 9;
    
    /* 年薪(税前): ANNUAL_SALARY DECIMAL */
    public static final String PROP_NAME_annualSalary = "annualSalary";
    public static final int PROP_ID_annualSalary = 10;
    
    /* 月薪(税前): MONTHLY_SALARY DECIMAL */
    public static final String PROP_NAME_monthlySalary = "monthlySalary";
    public static final int PROP_ID_monthlySalary = 11;
    
    /* 薪资币种: SALARY_CURRENCY_ID BIGINT */
    public static final String PROP_NAME_salaryCurrencyId = "salaryCurrencyId";
    public static final int PROP_ID_salaryCurrencyId = 12;
    
    /* 发薪方式: SALARY_PAY_METHOD VARCHAR */
    public static final String PROP_NAME_salaryPayMethod = "salaryPayMethod";
    public static final int PROP_ID_salaryPayMethod = 13;
    
    /* 社保基数: SOCIAL_INSURANCE_BASE DECIMAL */
    public static final String PROP_NAME_socialInsuranceBase = "socialInsuranceBase";
    public static final int PROP_ID_socialInsuranceBase = 14;
    
    /* 公积金基数: HOUSING_FUND_BASE DECIMAL */
    public static final String PROP_NAME_housingFundBase = "housingFundBase";
    public static final int PROP_ID_housingFundBase = 15;
    
    /* 合同状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 16;
    
    /* 合同文件: ATTACHMENT_FILE_ID VARCHAR */
    public static final String PROP_NAME_attachmentFileId = "attachmentFileId";
    public static final int PROP_ID_attachmentFileId = 17;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 18;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 19;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 20;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 21;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 22;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 23;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 24;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 25;
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 26;
    

    private static int _PROP_ID_BOUND = 27;

    
    /* relation:  */
    public static final String PROP_NAME_employee = "employee";
    
    /* relation:  */
    public static final String PROP_NAME_salaryCurrency = "salaryCurrency";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* component:  */
    public static final String PROP_NAME_attachmentFileIdComponent = "attachmentFileIdComponent";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[27];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_employeeId] = PROP_NAME_employeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeId, PROP_ID_employeeId);
      
          PROP_ID_TO_NAME[PROP_ID_contractType] = PROP_NAME_contractType;
          PROP_NAME_TO_ID.put(PROP_NAME_contractType, PROP_ID_contractType);
      
          PROP_ID_TO_NAME[PROP_ID_signDate] = PROP_NAME_signDate;
          PROP_NAME_TO_ID.put(PROP_NAME_signDate, PROP_ID_signDate);
      
          PROP_ID_TO_NAME[PROP_ID_startDate] = PROP_NAME_startDate;
          PROP_NAME_TO_ID.put(PROP_NAME_startDate, PROP_ID_startDate);
      
          PROP_ID_TO_NAME[PROP_ID_endDate] = PROP_NAME_endDate;
          PROP_NAME_TO_ID.put(PROP_NAME_endDate, PROP_ID_endDate);
      
          PROP_ID_TO_NAME[PROP_ID_probationMonths] = PROP_NAME_probationMonths;
          PROP_NAME_TO_ID.put(PROP_NAME_probationMonths, PROP_ID_probationMonths);
      
          PROP_ID_TO_NAME[PROP_ID_workingHoursPerWeek] = PROP_NAME_workingHoursPerWeek;
          PROP_NAME_TO_ID.put(PROP_NAME_workingHoursPerWeek, PROP_ID_workingHoursPerWeek);
      
          PROP_ID_TO_NAME[PROP_ID_annualSalary] = PROP_NAME_annualSalary;
          PROP_NAME_TO_ID.put(PROP_NAME_annualSalary, PROP_ID_annualSalary);
      
          PROP_ID_TO_NAME[PROP_ID_monthlySalary] = PROP_NAME_monthlySalary;
          PROP_NAME_TO_ID.put(PROP_NAME_monthlySalary, PROP_ID_monthlySalary);
      
          PROP_ID_TO_NAME[PROP_ID_salaryCurrencyId] = PROP_NAME_salaryCurrencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_salaryCurrencyId, PROP_ID_salaryCurrencyId);
      
          PROP_ID_TO_NAME[PROP_ID_salaryPayMethod] = PROP_NAME_salaryPayMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_salaryPayMethod, PROP_ID_salaryPayMethod);
      
          PROP_ID_TO_NAME[PROP_ID_socialInsuranceBase] = PROP_NAME_socialInsuranceBase;
          PROP_NAME_TO_ID.put(PROP_NAME_socialInsuranceBase, PROP_ID_socialInsuranceBase);
      
          PROP_ID_TO_NAME[PROP_ID_housingFundBase] = PROP_NAME_housingFundBase;
          PROP_NAME_TO_ID.put(PROP_NAME_housingFundBase, PROP_ID_housingFundBase);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_attachmentFileId] = PROP_NAME_attachmentFileId;
          PROP_NAME_TO_ID.put(PROP_NAME_attachmentFileId, PROP_ID_attachmentFileId);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 合同编号: CODE */
    private java.lang.String _code;
    
    /* 员工: EMPLOYEE_ID */
    private java.lang.Long _employeeId;
    
    /* 合同类型: CONTRACT_TYPE */
    private java.lang.String _contractType;
    
    /* 签订日期: SIGN_DATE */
    private java.time.LocalDate _signDate;
    
    /* 生效日期: START_DATE */
    private java.time.LocalDate _startDate;
    
    /* 到期日期: END_DATE */
    private java.time.LocalDate _endDate;
    
    /* 试用期月数: PROBATION_MONTHS */
    private java.lang.Integer _probationMonths;
    
    /* 每周工时: WORKING_HOURS_PER_WEEK */
    private java.math.BigDecimal _workingHoursPerWeek;
    
    /* 年薪(税前): ANNUAL_SALARY */
    private java.math.BigDecimal _annualSalary;
    
    /* 月薪(税前): MONTHLY_SALARY */
    private java.math.BigDecimal _monthlySalary;
    
    /* 薪资币种: SALARY_CURRENCY_ID */
    private java.lang.Long _salaryCurrencyId;
    
    /* 发薪方式: SALARY_PAY_METHOD */
    private java.lang.String _salaryPayMethod;
    
    /* 社保基数: SOCIAL_INSURANCE_BASE */
    private java.math.BigDecimal _socialInsuranceBase;
    
    /* 公积金基数: HOUSING_FUND_BASE */
    private java.math.BigDecimal _housingFundBase;
    
    /* 合同状态: STATUS */
    private java.lang.String _status;
    
    /* 合同文件: ATTACHMENT_FILE_ID */
    private java.lang.String _attachmentFileId;
    
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
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    

    public _ErpHrEmploymentContract(){
        // for debug
    }

    protected ErpHrEmploymentContract newInstance(){
        ErpHrEmploymentContract entity = new ErpHrEmploymentContract();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrEmploymentContract cloneInstance() {
        ErpHrEmploymentContract entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrEmploymentContract";
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
        
            case PROP_ID_employeeId:
               return getEmployeeId();
        
            case PROP_ID_contractType:
               return getContractType();
        
            case PROP_ID_signDate:
               return getSignDate();
        
            case PROP_ID_startDate:
               return getStartDate();
        
            case PROP_ID_endDate:
               return getEndDate();
        
            case PROP_ID_probationMonths:
               return getProbationMonths();
        
            case PROP_ID_workingHoursPerWeek:
               return getWorkingHoursPerWeek();
        
            case PROP_ID_annualSalary:
               return getAnnualSalary();
        
            case PROP_ID_monthlySalary:
               return getMonthlySalary();
        
            case PROP_ID_salaryCurrencyId:
               return getSalaryCurrencyId();
        
            case PROP_ID_salaryPayMethod:
               return getSalaryPayMethod();
        
            case PROP_ID_socialInsuranceBase:
               return getSocialInsuranceBase();
        
            case PROP_ID_housingFundBase:
               return getHousingFundBase();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_attachmentFileId:
               return getAttachmentFileId();
        
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
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
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
        
            case PROP_ID_employeeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_employeeId));
               }
               setEmployeeId(typedValue);
               break;
            }
        
            case PROP_ID_contractType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_contractType));
               }
               setContractType(typedValue);
               break;
            }
        
            case PROP_ID_signDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_signDate));
               }
               setSignDate(typedValue);
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
        
            case PROP_ID_probationMonths:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_probationMonths));
               }
               setProbationMonths(typedValue);
               break;
            }
        
            case PROP_ID_workingHoursPerWeek:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_workingHoursPerWeek));
               }
               setWorkingHoursPerWeek(typedValue);
               break;
            }
        
            case PROP_ID_annualSalary:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_annualSalary));
               }
               setAnnualSalary(typedValue);
               break;
            }
        
            case PROP_ID_monthlySalary:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_monthlySalary));
               }
               setMonthlySalary(typedValue);
               break;
            }
        
            case PROP_ID_salaryCurrencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_salaryCurrencyId));
               }
               setSalaryCurrencyId(typedValue);
               break;
            }
        
            case PROP_ID_salaryPayMethod:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_salaryPayMethod));
               }
               setSalaryPayMethod(typedValue);
               break;
            }
        
            case PROP_ID_socialInsuranceBase:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_socialInsuranceBase));
               }
               setSocialInsuranceBase(typedValue);
               break;
            }
        
            case PROP_ID_housingFundBase:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_housingFundBase));
               }
               setHousingFundBase(typedValue);
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
        
            case PROP_ID_attachmentFileId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_attachmentFileId));
               }
               setAttachmentFileId(typedValue);
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
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
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
        
            case PROP_ID_employeeId:{
               onInitProp(propId);
               this._employeeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_contractType:{
               onInitProp(propId);
               this._contractType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_signDate:{
               onInitProp(propId);
               this._signDate = (java.time.LocalDate)value;
               
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
        
            case PROP_ID_probationMonths:{
               onInitProp(propId);
               this._probationMonths = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_workingHoursPerWeek:{
               onInitProp(propId);
               this._workingHoursPerWeek = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_annualSalary:{
               onInitProp(propId);
               this._annualSalary = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_monthlySalary:{
               onInitProp(propId);
               this._monthlySalary = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_salaryCurrencyId:{
               onInitProp(propId);
               this._salaryCurrencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_salaryPayMethod:{
               onInitProp(propId);
               this._salaryPayMethod = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_socialInsuranceBase:{
               onInitProp(propId);
               this._socialInsuranceBase = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_housingFundBase:{
               onInitProp(propId);
               this._housingFundBase = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_attachmentFileId:{
               onInitProp(propId);
               this._attachmentFileId = (java.lang.String)value;
               
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
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
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
     * 合同编号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 合同编号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
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
     * 合同类型: CONTRACT_TYPE
     */
    public final java.lang.String getContractType(){
         onPropGet(PROP_ID_contractType);
         return _contractType;
    }

    /**
     * 合同类型: CONTRACT_TYPE
     */
    public final void setContractType(java.lang.String value){
        if(onPropSet(PROP_ID_contractType,value)){
            this._contractType = value;
            internalClearRefs(PROP_ID_contractType);
            
        }
    }
    
    /**
     * 签订日期: SIGN_DATE
     */
    public final java.time.LocalDate getSignDate(){
         onPropGet(PROP_ID_signDate);
         return _signDate;
    }

    /**
     * 签订日期: SIGN_DATE
     */
    public final void setSignDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_signDate,value)){
            this._signDate = value;
            internalClearRefs(PROP_ID_signDate);
            
        }
    }
    
    /**
     * 生效日期: START_DATE
     */
    public final java.time.LocalDate getStartDate(){
         onPropGet(PROP_ID_startDate);
         return _startDate;
    }

    /**
     * 生效日期: START_DATE
     */
    public final void setStartDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_startDate,value)){
            this._startDate = value;
            internalClearRefs(PROP_ID_startDate);
            
        }
    }
    
    /**
     * 到期日期: END_DATE
     */
    public final java.time.LocalDate getEndDate(){
         onPropGet(PROP_ID_endDate);
         return _endDate;
    }

    /**
     * 到期日期: END_DATE
     */
    public final void setEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_endDate,value)){
            this._endDate = value;
            internalClearRefs(PROP_ID_endDate);
            
        }
    }
    
    /**
     * 试用期月数: PROBATION_MONTHS
     */
    public final java.lang.Integer getProbationMonths(){
         onPropGet(PROP_ID_probationMonths);
         return _probationMonths;
    }

    /**
     * 试用期月数: PROBATION_MONTHS
     */
    public final void setProbationMonths(java.lang.Integer value){
        if(onPropSet(PROP_ID_probationMonths,value)){
            this._probationMonths = value;
            internalClearRefs(PROP_ID_probationMonths);
            
        }
    }
    
    /**
     * 每周工时: WORKING_HOURS_PER_WEEK
     */
    public final java.math.BigDecimal getWorkingHoursPerWeek(){
         onPropGet(PROP_ID_workingHoursPerWeek);
         return _workingHoursPerWeek;
    }

    /**
     * 每周工时: WORKING_HOURS_PER_WEEK
     */
    public final void setWorkingHoursPerWeek(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_workingHoursPerWeek,value)){
            this._workingHoursPerWeek = value;
            internalClearRefs(PROP_ID_workingHoursPerWeek);
            
        }
    }
    
    /**
     * 年薪(税前): ANNUAL_SALARY
     */
    public final java.math.BigDecimal getAnnualSalary(){
         onPropGet(PROP_ID_annualSalary);
         return _annualSalary;
    }

    /**
     * 年薪(税前): ANNUAL_SALARY
     */
    public final void setAnnualSalary(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_annualSalary,value)){
            this._annualSalary = value;
            internalClearRefs(PROP_ID_annualSalary);
            
        }
    }
    
    /**
     * 月薪(税前): MONTHLY_SALARY
     */
    public final java.math.BigDecimal getMonthlySalary(){
         onPropGet(PROP_ID_monthlySalary);
         return _monthlySalary;
    }

    /**
     * 月薪(税前): MONTHLY_SALARY
     */
    public final void setMonthlySalary(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_monthlySalary,value)){
            this._monthlySalary = value;
            internalClearRefs(PROP_ID_monthlySalary);
            
        }
    }
    
    /**
     * 薪资币种: SALARY_CURRENCY_ID
     */
    public final java.lang.Long getSalaryCurrencyId(){
         onPropGet(PROP_ID_salaryCurrencyId);
         return _salaryCurrencyId;
    }

    /**
     * 薪资币种: SALARY_CURRENCY_ID
     */
    public final void setSalaryCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_salaryCurrencyId,value)){
            this._salaryCurrencyId = value;
            internalClearRefs(PROP_ID_salaryCurrencyId);
            
        }
    }
    
    /**
     * 发薪方式: SALARY_PAY_METHOD
     */
    public final java.lang.String getSalaryPayMethod(){
         onPropGet(PROP_ID_salaryPayMethod);
         return _salaryPayMethod;
    }

    /**
     * 发薪方式: SALARY_PAY_METHOD
     */
    public final void setSalaryPayMethod(java.lang.String value){
        if(onPropSet(PROP_ID_salaryPayMethod,value)){
            this._salaryPayMethod = value;
            internalClearRefs(PROP_ID_salaryPayMethod);
            
        }
    }
    
    /**
     * 社保基数: SOCIAL_INSURANCE_BASE
     */
    public final java.math.BigDecimal getSocialInsuranceBase(){
         onPropGet(PROP_ID_socialInsuranceBase);
         return _socialInsuranceBase;
    }

    /**
     * 社保基数: SOCIAL_INSURANCE_BASE
     */
    public final void setSocialInsuranceBase(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_socialInsuranceBase,value)){
            this._socialInsuranceBase = value;
            internalClearRefs(PROP_ID_socialInsuranceBase);
            
        }
    }
    
    /**
     * 公积金基数: HOUSING_FUND_BASE
     */
    public final java.math.BigDecimal getHousingFundBase(){
         onPropGet(PROP_ID_housingFundBase);
         return _housingFundBase;
    }

    /**
     * 公积金基数: HOUSING_FUND_BASE
     */
    public final void setHousingFundBase(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_housingFundBase,value)){
            this._housingFundBase = value;
            internalClearRefs(PROP_ID_housingFundBase);
            
        }
    }
    
    /**
     * 合同状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 合同状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 合同文件: ATTACHMENT_FILE_ID
     */
    public final java.lang.String getAttachmentFileId(){
         onPropGet(PROP_ID_attachmentFileId);
         return _attachmentFileId;
    }

    /**
     * 合同文件: ATTACHMENT_FILE_ID
     */
    public final void setAttachmentFileId(java.lang.String value){
        if(onPropSet(PROP_ID_attachmentFileId,value)){
            this._attachmentFileId = value;
            internalClearRefs(PROP_ID_attachmentFileId);
            
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
    public final app.erp.md.dao.entity.ErpMdCurrency getSalaryCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_salaryCurrency);
    }

    public final void setSalaryCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setSalaryCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_salaryCurrency, refEntity,()->{
           
                           this.setSalaryCurrencyId(refEntity.getId());
                       
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
       
   private io.nop.orm.component.OrmFileComponent _attachmentFileIdComponent;

   private static Map<String,Integer> COMPONENT_PROP_ID_MAP_attachmentFileIdComponent = new HashMap<>();
   static{
      
         COMPONENT_PROP_ID_MAP_attachmentFileIdComponent.put(io.nop.orm.component.OrmFileComponent.PROP_NAME_filePath,PROP_ID_attachmentFileId);
      
   }

   public final io.nop.orm.component.OrmFileComponent getAttachmentFileIdComponent(){
      if(_attachmentFileIdComponent == null){
          _attachmentFileIdComponent = new io.nop.orm.component.OrmFileComponent();
          _attachmentFileIdComponent.bindToEntity(this, COMPONENT_PROP_ID_MAP_attachmentFileIdComponent);
      }
      return _attachmentFileIdComponent;
   }

}
// resume CPD analysis - CPD-ON
