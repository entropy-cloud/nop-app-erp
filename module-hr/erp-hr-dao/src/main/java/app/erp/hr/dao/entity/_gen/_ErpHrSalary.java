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

import app.erp.hr.dao.entity.ErpHrSalary;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  薪酬记录: erp_hr_salary
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrSalary extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 员工: EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_employeeId = "employeeId";
    public static final int PROP_ID_employeeId = 2;
    
    /* 年份: YEAR INTEGER */
    public static final String PROP_NAME_year = "year";
    public static final int PROP_ID_year = 3;
    
    /* 月份: MONTH INTEGER */
    public static final String PROP_NAME_month = "month";
    public static final int PROP_ID_month = 4;
    
    /* 基本工资: BASIC_SALARY DECIMAL */
    public static final String PROP_NAME_basicSalary = "basicSalary";
    public static final int PROP_ID_basicSalary = 5;
    
    /* 岗位津贴: POSITION_ALLOWANCE DECIMAL */
    public static final String PROP_NAME_positionAllowance = "positionAllowance";
    public static final int PROP_ID_positionAllowance = 6;
    
    /* 绩效奖金: PERFORMANCE_BONUS DECIMAL */
    public static final String PROP_NAME_performanceBonus = "performanceBonus";
    public static final int PROP_ID_performanceBonus = 7;
    
    /* 加班费: OVERTIME_PAY DECIMAL */
    public static final String PROP_NAME_overtimePay = "overtimePay";
    public static final int PROP_ID_overtimePay = 8;
    
    /* 餐补: MEAL_ALLOWANCE DECIMAL */
    public static final String PROP_NAME_mealAllowance = "mealAllowance";
    public static final int PROP_ID_mealAllowance = 9;
    
    /* 交通补贴: TRANSPORT_ALLOWANCE DECIMAL */
    public static final String PROP_NAME_transportAllowance = "transportAllowance";
    public static final int PROP_ID_transportAllowance = 10;
    
    /* 其他补贴: OTHER_ALLOWANCE DECIMAL */
    public static final String PROP_NAME_otherAllowance = "otherAllowance";
    public static final int PROP_ID_otherAllowance = 11;
    
    /* 应发合计: GROSS_SALARY DECIMAL */
    public static final String PROP_NAME_grossSalary = "grossSalary";
    public static final int PROP_ID_grossSalary = 12;
    
    /* 社保个人部分: SOCIAL_INSURANCE DECIMAL */
    public static final String PROP_NAME_socialInsurance = "socialInsurance";
    public static final int PROP_ID_socialInsurance = 13;
    
    /* 公积金个人部分: HOUSING_FUND DECIMAL */
    public static final String PROP_NAME_housingFund = "housingFund";
    public static final int PROP_ID_housingFund = 14;
    
    /* 个税: TAX_AMOUNT DECIMAL */
    public static final String PROP_NAME_taxAmount = "taxAmount";
    public static final int PROP_ID_taxAmount = 15;
    
    /* 其他扣款: OTHER_DEDUCTIONS DECIMAL */
    public static final String PROP_NAME_otherDeductions = "otherDeductions";
    public static final int PROP_ID_otherDeductions = 16;
    
    /* 实发合计: NET_SALARY DECIMAL */
    public static final String PROP_NAME_netSalary = "netSalary";
    public static final int PROP_ID_netSalary = 17;
    
    /* 支付状态: PAYMENT_STATUS INTEGER */
    public static final String PROP_NAME_paymentStatus = "paymentStatus";
    public static final int PROP_ID_paymentStatus = 18;
    
    /* 实发日期: PAYMENT_DATE DATE */
    public static final String PROP_NAME_paymentDate = "paymentDate";
    public static final int PROP_ID_paymentDate = 19;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 20;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 21;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 22;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 23;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 24;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 25;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 26;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 27;
    

    private static int _PROP_ID_BOUND = 28;

    
    /* relation:  */
    public static final String PROP_NAME_employee = "employee";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[28];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_employeeId] = PROP_NAME_employeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeId, PROP_ID_employeeId);
      
          PROP_ID_TO_NAME[PROP_ID_year] = PROP_NAME_year;
          PROP_NAME_TO_ID.put(PROP_NAME_year, PROP_ID_year);
      
          PROP_ID_TO_NAME[PROP_ID_month] = PROP_NAME_month;
          PROP_NAME_TO_ID.put(PROP_NAME_month, PROP_ID_month);
      
          PROP_ID_TO_NAME[PROP_ID_basicSalary] = PROP_NAME_basicSalary;
          PROP_NAME_TO_ID.put(PROP_NAME_basicSalary, PROP_ID_basicSalary);
      
          PROP_ID_TO_NAME[PROP_ID_positionAllowance] = PROP_NAME_positionAllowance;
          PROP_NAME_TO_ID.put(PROP_NAME_positionAllowance, PROP_ID_positionAllowance);
      
          PROP_ID_TO_NAME[PROP_ID_performanceBonus] = PROP_NAME_performanceBonus;
          PROP_NAME_TO_ID.put(PROP_NAME_performanceBonus, PROP_ID_performanceBonus);
      
          PROP_ID_TO_NAME[PROP_ID_overtimePay] = PROP_NAME_overtimePay;
          PROP_NAME_TO_ID.put(PROP_NAME_overtimePay, PROP_ID_overtimePay);
      
          PROP_ID_TO_NAME[PROP_ID_mealAllowance] = PROP_NAME_mealAllowance;
          PROP_NAME_TO_ID.put(PROP_NAME_mealAllowance, PROP_ID_mealAllowance);
      
          PROP_ID_TO_NAME[PROP_ID_transportAllowance] = PROP_NAME_transportAllowance;
          PROP_NAME_TO_ID.put(PROP_NAME_transportAllowance, PROP_ID_transportAllowance);
      
          PROP_ID_TO_NAME[PROP_ID_otherAllowance] = PROP_NAME_otherAllowance;
          PROP_NAME_TO_ID.put(PROP_NAME_otherAllowance, PROP_ID_otherAllowance);
      
          PROP_ID_TO_NAME[PROP_ID_grossSalary] = PROP_NAME_grossSalary;
          PROP_NAME_TO_ID.put(PROP_NAME_grossSalary, PROP_ID_grossSalary);
      
          PROP_ID_TO_NAME[PROP_ID_socialInsurance] = PROP_NAME_socialInsurance;
          PROP_NAME_TO_ID.put(PROP_NAME_socialInsurance, PROP_ID_socialInsurance);
      
          PROP_ID_TO_NAME[PROP_ID_housingFund] = PROP_NAME_housingFund;
          PROP_NAME_TO_ID.put(PROP_NAME_housingFund, PROP_ID_housingFund);
      
          PROP_ID_TO_NAME[PROP_ID_taxAmount] = PROP_NAME_taxAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_taxAmount, PROP_ID_taxAmount);
      
          PROP_ID_TO_NAME[PROP_ID_otherDeductions] = PROP_NAME_otherDeductions;
          PROP_NAME_TO_ID.put(PROP_NAME_otherDeductions, PROP_ID_otherDeductions);
      
          PROP_ID_TO_NAME[PROP_ID_netSalary] = PROP_NAME_netSalary;
          PROP_NAME_TO_ID.put(PROP_NAME_netSalary, PROP_ID_netSalary);
      
          PROP_ID_TO_NAME[PROP_ID_paymentStatus] = PROP_NAME_paymentStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_paymentStatus, PROP_ID_paymentStatus);
      
          PROP_ID_TO_NAME[PROP_ID_paymentDate] = PROP_NAME_paymentDate;
          PROP_NAME_TO_ID.put(PROP_NAME_paymentDate, PROP_ID_paymentDate);
      
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
    
    /* 员工: EMPLOYEE_ID */
    private java.lang.Long _employeeId;
    
    /* 年份: YEAR */
    private java.lang.Integer _year;
    
    /* 月份: MONTH */
    private java.lang.Integer _month;
    
    /* 基本工资: BASIC_SALARY */
    private java.math.BigDecimal _basicSalary;
    
    /* 岗位津贴: POSITION_ALLOWANCE */
    private java.math.BigDecimal _positionAllowance;
    
    /* 绩效奖金: PERFORMANCE_BONUS */
    private java.math.BigDecimal _performanceBonus;
    
    /* 加班费: OVERTIME_PAY */
    private java.math.BigDecimal _overtimePay;
    
    /* 餐补: MEAL_ALLOWANCE */
    private java.math.BigDecimal _mealAllowance;
    
    /* 交通补贴: TRANSPORT_ALLOWANCE */
    private java.math.BigDecimal _transportAllowance;
    
    /* 其他补贴: OTHER_ALLOWANCE */
    private java.math.BigDecimal _otherAllowance;
    
    /* 应发合计: GROSS_SALARY */
    private java.math.BigDecimal _grossSalary;
    
    /* 社保个人部分: SOCIAL_INSURANCE */
    private java.math.BigDecimal _socialInsurance;
    
    /* 公积金个人部分: HOUSING_FUND */
    private java.math.BigDecimal _housingFund;
    
    /* 个税: TAX_AMOUNT */
    private java.math.BigDecimal _taxAmount;
    
    /* 其他扣款: OTHER_DEDUCTIONS */
    private java.math.BigDecimal _otherDeductions;
    
    /* 实发合计: NET_SALARY */
    private java.math.BigDecimal _netSalary;
    
    /* 支付状态: PAYMENT_STATUS */
    private java.lang.Integer _paymentStatus;
    
    /* 实发日期: PAYMENT_DATE */
    private java.time.LocalDate _paymentDate;
    
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
    

    public _ErpHrSalary(){
        // for debug
    }

    protected ErpHrSalary newInstance(){
        ErpHrSalary entity = new ErpHrSalary();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrSalary cloneInstance() {
        ErpHrSalary entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrSalary";
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
        
            case PROP_ID_employeeId:
               return getEmployeeId();
        
            case PROP_ID_year:
               return getYear();
        
            case PROP_ID_month:
               return getMonth();
        
            case PROP_ID_basicSalary:
               return getBasicSalary();
        
            case PROP_ID_positionAllowance:
               return getPositionAllowance();
        
            case PROP_ID_performanceBonus:
               return getPerformanceBonus();
        
            case PROP_ID_overtimePay:
               return getOvertimePay();
        
            case PROP_ID_mealAllowance:
               return getMealAllowance();
        
            case PROP_ID_transportAllowance:
               return getTransportAllowance();
        
            case PROP_ID_otherAllowance:
               return getOtherAllowance();
        
            case PROP_ID_grossSalary:
               return getGrossSalary();
        
            case PROP_ID_socialInsurance:
               return getSocialInsurance();
        
            case PROP_ID_housingFund:
               return getHousingFund();
        
            case PROP_ID_taxAmount:
               return getTaxAmount();
        
            case PROP_ID_otherDeductions:
               return getOtherDeductions();
        
            case PROP_ID_netSalary:
               return getNetSalary();
        
            case PROP_ID_paymentStatus:
               return getPaymentStatus();
        
            case PROP_ID_paymentDate:
               return getPaymentDate();
        
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
        
            case PROP_ID_employeeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_employeeId));
               }
               setEmployeeId(typedValue);
               break;
            }
        
            case PROP_ID_year:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_year));
               }
               setYear(typedValue);
               break;
            }
        
            case PROP_ID_month:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_month));
               }
               setMonth(typedValue);
               break;
            }
        
            case PROP_ID_basicSalary:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_basicSalary));
               }
               setBasicSalary(typedValue);
               break;
            }
        
            case PROP_ID_positionAllowance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_positionAllowance));
               }
               setPositionAllowance(typedValue);
               break;
            }
        
            case PROP_ID_performanceBonus:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_performanceBonus));
               }
               setPerformanceBonus(typedValue);
               break;
            }
        
            case PROP_ID_overtimePay:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_overtimePay));
               }
               setOvertimePay(typedValue);
               break;
            }
        
            case PROP_ID_mealAllowance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_mealAllowance));
               }
               setMealAllowance(typedValue);
               break;
            }
        
            case PROP_ID_transportAllowance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_transportAllowance));
               }
               setTransportAllowance(typedValue);
               break;
            }
        
            case PROP_ID_otherAllowance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_otherAllowance));
               }
               setOtherAllowance(typedValue);
               break;
            }
        
            case PROP_ID_grossSalary:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_grossSalary));
               }
               setGrossSalary(typedValue);
               break;
            }
        
            case PROP_ID_socialInsurance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_socialInsurance));
               }
               setSocialInsurance(typedValue);
               break;
            }
        
            case PROP_ID_housingFund:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_housingFund));
               }
               setHousingFund(typedValue);
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
        
            case PROP_ID_otherDeductions:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_otherDeductions));
               }
               setOtherDeductions(typedValue);
               break;
            }
        
            case PROP_ID_netSalary:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_netSalary));
               }
               setNetSalary(typedValue);
               break;
            }
        
            case PROP_ID_paymentStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_paymentStatus));
               }
               setPaymentStatus(typedValue);
               break;
            }
        
            case PROP_ID_paymentDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_paymentDate));
               }
               setPaymentDate(typedValue);
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
        
            case PROP_ID_employeeId:{
               onInitProp(propId);
               this._employeeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_year:{
               onInitProp(propId);
               this._year = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_month:{
               onInitProp(propId);
               this._month = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_basicSalary:{
               onInitProp(propId);
               this._basicSalary = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_positionAllowance:{
               onInitProp(propId);
               this._positionAllowance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_performanceBonus:{
               onInitProp(propId);
               this._performanceBonus = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_overtimePay:{
               onInitProp(propId);
               this._overtimePay = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_mealAllowance:{
               onInitProp(propId);
               this._mealAllowance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_transportAllowance:{
               onInitProp(propId);
               this._transportAllowance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_otherAllowance:{
               onInitProp(propId);
               this._otherAllowance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_grossSalary:{
               onInitProp(propId);
               this._grossSalary = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_socialInsurance:{
               onInitProp(propId);
               this._socialInsurance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_housingFund:{
               onInitProp(propId);
               this._housingFund = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_taxAmount:{
               onInitProp(propId);
               this._taxAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_otherDeductions:{
               onInitProp(propId);
               this._otherDeductions = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_netSalary:{
               onInitProp(propId);
               this._netSalary = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_paymentStatus:{
               onInitProp(propId);
               this._paymentStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_paymentDate:{
               onInitProp(propId);
               this._paymentDate = (java.time.LocalDate)value;
               
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
     * 年份: YEAR
     */
    public final java.lang.Integer getYear(){
         onPropGet(PROP_ID_year);
         return _year;
    }

    /**
     * 年份: YEAR
     */
    public final void setYear(java.lang.Integer value){
        if(onPropSet(PROP_ID_year,value)){
            this._year = value;
            internalClearRefs(PROP_ID_year);
            
        }
    }
    
    /**
     * 月份: MONTH
     */
    public final java.lang.Integer getMonth(){
         onPropGet(PROP_ID_month);
         return _month;
    }

    /**
     * 月份: MONTH
     */
    public final void setMonth(java.lang.Integer value){
        if(onPropSet(PROP_ID_month,value)){
            this._month = value;
            internalClearRefs(PROP_ID_month);
            
        }
    }
    
    /**
     * 基本工资: BASIC_SALARY
     */
    public final java.math.BigDecimal getBasicSalary(){
         onPropGet(PROP_ID_basicSalary);
         return _basicSalary;
    }

    /**
     * 基本工资: BASIC_SALARY
     */
    public final void setBasicSalary(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_basicSalary,value)){
            this._basicSalary = value;
            internalClearRefs(PROP_ID_basicSalary);
            
        }
    }
    
    /**
     * 岗位津贴: POSITION_ALLOWANCE
     */
    public final java.math.BigDecimal getPositionAllowance(){
         onPropGet(PROP_ID_positionAllowance);
         return _positionAllowance;
    }

    /**
     * 岗位津贴: POSITION_ALLOWANCE
     */
    public final void setPositionAllowance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_positionAllowance,value)){
            this._positionAllowance = value;
            internalClearRefs(PROP_ID_positionAllowance);
            
        }
    }
    
    /**
     * 绩效奖金: PERFORMANCE_BONUS
     */
    public final java.math.BigDecimal getPerformanceBonus(){
         onPropGet(PROP_ID_performanceBonus);
         return _performanceBonus;
    }

    /**
     * 绩效奖金: PERFORMANCE_BONUS
     */
    public final void setPerformanceBonus(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_performanceBonus,value)){
            this._performanceBonus = value;
            internalClearRefs(PROP_ID_performanceBonus);
            
        }
    }
    
    /**
     * 加班费: OVERTIME_PAY
     */
    public final java.math.BigDecimal getOvertimePay(){
         onPropGet(PROP_ID_overtimePay);
         return _overtimePay;
    }

    /**
     * 加班费: OVERTIME_PAY
     */
    public final void setOvertimePay(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_overtimePay,value)){
            this._overtimePay = value;
            internalClearRefs(PROP_ID_overtimePay);
            
        }
    }
    
    /**
     * 餐补: MEAL_ALLOWANCE
     */
    public final java.math.BigDecimal getMealAllowance(){
         onPropGet(PROP_ID_mealAllowance);
         return _mealAllowance;
    }

    /**
     * 餐补: MEAL_ALLOWANCE
     */
    public final void setMealAllowance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_mealAllowance,value)){
            this._mealAllowance = value;
            internalClearRefs(PROP_ID_mealAllowance);
            
        }
    }
    
    /**
     * 交通补贴: TRANSPORT_ALLOWANCE
     */
    public final java.math.BigDecimal getTransportAllowance(){
         onPropGet(PROP_ID_transportAllowance);
         return _transportAllowance;
    }

    /**
     * 交通补贴: TRANSPORT_ALLOWANCE
     */
    public final void setTransportAllowance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_transportAllowance,value)){
            this._transportAllowance = value;
            internalClearRefs(PROP_ID_transportAllowance);
            
        }
    }
    
    /**
     * 其他补贴: OTHER_ALLOWANCE
     */
    public final java.math.BigDecimal getOtherAllowance(){
         onPropGet(PROP_ID_otherAllowance);
         return _otherAllowance;
    }

    /**
     * 其他补贴: OTHER_ALLOWANCE
     */
    public final void setOtherAllowance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_otherAllowance,value)){
            this._otherAllowance = value;
            internalClearRefs(PROP_ID_otherAllowance);
            
        }
    }
    
    /**
     * 应发合计: GROSS_SALARY
     */
    public final java.math.BigDecimal getGrossSalary(){
         onPropGet(PROP_ID_grossSalary);
         return _grossSalary;
    }

    /**
     * 应发合计: GROSS_SALARY
     */
    public final void setGrossSalary(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_grossSalary,value)){
            this._grossSalary = value;
            internalClearRefs(PROP_ID_grossSalary);
            
        }
    }
    
    /**
     * 社保个人部分: SOCIAL_INSURANCE
     */
    public final java.math.BigDecimal getSocialInsurance(){
         onPropGet(PROP_ID_socialInsurance);
         return _socialInsurance;
    }

    /**
     * 社保个人部分: SOCIAL_INSURANCE
     */
    public final void setSocialInsurance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_socialInsurance,value)){
            this._socialInsurance = value;
            internalClearRefs(PROP_ID_socialInsurance);
            
        }
    }
    
    /**
     * 公积金个人部分: HOUSING_FUND
     */
    public final java.math.BigDecimal getHousingFund(){
         onPropGet(PROP_ID_housingFund);
         return _housingFund;
    }

    /**
     * 公积金个人部分: HOUSING_FUND
     */
    public final void setHousingFund(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_housingFund,value)){
            this._housingFund = value;
            internalClearRefs(PROP_ID_housingFund);
            
        }
    }
    
    /**
     * 个税: TAX_AMOUNT
     */
    public final java.math.BigDecimal getTaxAmount(){
         onPropGet(PROP_ID_taxAmount);
         return _taxAmount;
    }

    /**
     * 个税: TAX_AMOUNT
     */
    public final void setTaxAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_taxAmount,value)){
            this._taxAmount = value;
            internalClearRefs(PROP_ID_taxAmount);
            
        }
    }
    
    /**
     * 其他扣款: OTHER_DEDUCTIONS
     */
    public final java.math.BigDecimal getOtherDeductions(){
         onPropGet(PROP_ID_otherDeductions);
         return _otherDeductions;
    }

    /**
     * 其他扣款: OTHER_DEDUCTIONS
     */
    public final void setOtherDeductions(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_otherDeductions,value)){
            this._otherDeductions = value;
            internalClearRefs(PROP_ID_otherDeductions);
            
        }
    }
    
    /**
     * 实发合计: NET_SALARY
     */
    public final java.math.BigDecimal getNetSalary(){
         onPropGet(PROP_ID_netSalary);
         return _netSalary;
    }

    /**
     * 实发合计: NET_SALARY
     */
    public final void setNetSalary(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_netSalary,value)){
            this._netSalary = value;
            internalClearRefs(PROP_ID_netSalary);
            
        }
    }
    
    /**
     * 支付状态: PAYMENT_STATUS
     */
    public final java.lang.Integer getPaymentStatus(){
         onPropGet(PROP_ID_paymentStatus);
         return _paymentStatus;
    }

    /**
     * 支付状态: PAYMENT_STATUS
     */
    public final void setPaymentStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_paymentStatus,value)){
            this._paymentStatus = value;
            internalClearRefs(PROP_ID_paymentStatus);
            
        }
    }
    
    /**
     * 实发日期: PAYMENT_DATE
     */
    public final java.time.LocalDate getPaymentDate(){
         onPropGet(PROP_ID_paymentDate);
         return _paymentDate;
    }

    /**
     * 实发日期: PAYMENT_DATE
     */
    public final void setPaymentDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_paymentDate,value)){
            this._paymentDate = value;
            internalClearRefs(PROP_ID_paymentDate);
            
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
