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

import app.erp.hr.dao.entity.ErpHrEmployee;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  员工: erp_hr_employee
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrEmployee extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 工号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 姓: FIRST_NAME VARCHAR */
    public static final String PROP_NAME_firstName = "firstName";
    public static final int PROP_ID_firstName = 3;
    
    /* 名: LAST_NAME VARCHAR */
    public static final String PROP_NAME_lastName = "lastName";
    public static final int PROP_ID_lastName = 4;
    
    /* 全名: FULL_NAME VARCHAR */
    public static final String PROP_NAME_fullName = "fullName";
    public static final int PROP_ID_fullName = 5;
    
    /* 性别: GENDER INTEGER */
    public static final String PROP_NAME_gender = "gender";
    public static final int PROP_ID_gender = 6;
    
    /* 出生日期: BIRTH_DATE DATE */
    public static final String PROP_NAME_birthDate = "birthDate";
    public static final int PROP_ID_birthDate = 7;
    
    /* 证件类型: ID_CARD_TYPE VARCHAR */
    public static final String PROP_NAME_idCardType = "idCardType";
    public static final int PROP_ID_idCardType = 8;
    
    /* 证件号码: ID_CARD_NO VARCHAR */
    public static final String PROP_NAME_idCardNo = "idCardNo";
    public static final int PROP_ID_idCardNo = 9;
    
    /* 电子邮箱: EMAIL VARCHAR */
    public static final String PROP_NAME_email = "email";
    public static final int PROP_ID_email = 10;
    
    /* 手机号: MOBILE_PHONE VARCHAR */
    public static final String PROP_NAME_mobilePhone = "mobilePhone";
    public static final int PROP_ID_mobilePhone = 11;
    
    /* 婚姻状况: MARITAL_STATUS INTEGER */
    public static final String PROP_NAME_maritalStatus = "maritalStatus";
    public static final int PROP_ID_maritalStatus = 12;
    
    /* 国籍: NATIONALITY VARCHAR */
    public static final String PROP_NAME_nationality = "nationality";
    public static final int PROP_ID_nationality = 13;
    
    /* 紧急联系人: EMERGENCY_CONTACT VARCHAR */
    public static final String PROP_NAME_emergencyContact = "emergencyContact";
    public static final int PROP_ID_emergencyContact = 14;
    
    /* 紧急联系电话: EMERGENCY_PHONE VARCHAR */
    public static final String PROP_NAME_emergencyPhone = "emergencyPhone";
    public static final int PROP_ID_emergencyPhone = 15;
    
    /* 部门: DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_departmentId = "departmentId";
    public static final int PROP_ID_departmentId = 16;
    
    /* 职位: POSITION_ID BIGINT */
    public static final String PROP_NAME_positionId = "positionId";
    public static final int PROP_ID_positionId = 17;
    
    /* 岗位名称: JOB_TITLE VARCHAR */
    public static final String PROP_NAME_jobTitle = "jobTitle";
    public static final int PROP_ID_jobTitle = 18;
    
    /* 直接上级: SUPERIOR_ID BIGINT */
    public static final String PROP_NAME_superiorId = "superiorId";
    public static final int PROP_ID_superiorId = 19;
    
    /* 默认成本中心: COST_CENTER_ID BIGINT */
    public static final String PROP_NAME_costCenterId = "costCenterId";
    public static final int PROP_ID_costCenterId = 20;
    
    /* 入职日期: HIRE_DATE DATE */
    public static final String PROP_NAME_hireDate = "hireDate";
    public static final int PROP_ID_hireDate = 21;
    
    /* 试用期截止: PROBATION_END_DATE DATE */
    public static final String PROP_NAME_probationEndDate = "probationEndDate";
    public static final int PROP_ID_probationEndDate = 22;
    
    /* 转正日期: REGULAR_DATE DATE */
    public static final String PROP_NAME_regularDate = "regularDate";
    public static final int PROP_ID_regularDate = 23;
    
    /* 离职日期: RESIGNATION_DATE DATE */
    public static final String PROP_NAME_resignationDate = "resignationDate";
    public static final int PROP_ID_resignationDate = 24;
    
    /* 离职原因: RESIGNATION_REASON VARCHAR */
    public static final String PROP_NAME_resignationReason = "resignationReason";
    public static final int PROP_ID_resignationReason = 25;
    
    /* 雇佣状态: EMPLOYMENT_STATUS INTEGER */
    public static final String PROP_NAME_employmentStatus = "employmentStatus";
    public static final int PROP_ID_employmentStatus = 26;
    
    /* 员工类型: EMPLOYEE_TYPE INTEGER */
    public static final String PROP_NAME_employeeType = "employeeType";
    public static final int PROP_ID_employeeType = 27;
    
    /* 工资卡账户: BANK_ACCOUNT_ID BIGINT */
    public static final String PROP_NAME_bankAccountId = "bankAccountId";
    public static final int PROP_ID_bankAccountId = 28;
    
    /* 社保号: SOCIAL_SECURITY_NO VARCHAR */
    public static final String PROP_NAME_socialSecurityNo = "socialSecurityNo";
    public static final int PROP_ID_socialSecurityNo = 29;
    
    /* 个税档案号: TAX_FILE_NO VARCHAR */
    public static final String PROP_NAME_taxFileNo = "taxFileNo";
    public static final int PROP_ID_taxFileNo = 30;
    
    /* 系统用户ID: USER_ACCOUNT_ID BIGINT */
    public static final String PROP_NAME_userAccountId = "userAccountId";
    public static final int PROP_ID_userAccountId = 31;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 32;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 33;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 34;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 35;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 36;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 37;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 38;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 39;
    

    private static int _PROP_ID_BOUND = 40;

    
    /* relation:  */
    public static final String PROP_NAME_department = "department";
    
    /* relation:  */
    public static final String PROP_NAME_position = "position";
    
    /* relation:  */
    public static final String PROP_NAME_superior = "superior";
    
    /* relation:  */
    public static final String PROP_NAME_costCenter = "costCenter";
    
    /* relation:  */
    public static final String PROP_NAME_bankAccount = "bankAccount";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[40];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_firstName] = PROP_NAME_firstName;
          PROP_NAME_TO_ID.put(PROP_NAME_firstName, PROP_ID_firstName);
      
          PROP_ID_TO_NAME[PROP_ID_lastName] = PROP_NAME_lastName;
          PROP_NAME_TO_ID.put(PROP_NAME_lastName, PROP_ID_lastName);
      
          PROP_ID_TO_NAME[PROP_ID_fullName] = PROP_NAME_fullName;
          PROP_NAME_TO_ID.put(PROP_NAME_fullName, PROP_ID_fullName);
      
          PROP_ID_TO_NAME[PROP_ID_gender] = PROP_NAME_gender;
          PROP_NAME_TO_ID.put(PROP_NAME_gender, PROP_ID_gender);
      
          PROP_ID_TO_NAME[PROP_ID_birthDate] = PROP_NAME_birthDate;
          PROP_NAME_TO_ID.put(PROP_NAME_birthDate, PROP_ID_birthDate);
      
          PROP_ID_TO_NAME[PROP_ID_idCardType] = PROP_NAME_idCardType;
          PROP_NAME_TO_ID.put(PROP_NAME_idCardType, PROP_ID_idCardType);
      
          PROP_ID_TO_NAME[PROP_ID_idCardNo] = PROP_NAME_idCardNo;
          PROP_NAME_TO_ID.put(PROP_NAME_idCardNo, PROP_ID_idCardNo);
      
          PROP_ID_TO_NAME[PROP_ID_email] = PROP_NAME_email;
          PROP_NAME_TO_ID.put(PROP_NAME_email, PROP_ID_email);
      
          PROP_ID_TO_NAME[PROP_ID_mobilePhone] = PROP_NAME_mobilePhone;
          PROP_NAME_TO_ID.put(PROP_NAME_mobilePhone, PROP_ID_mobilePhone);
      
          PROP_ID_TO_NAME[PROP_ID_maritalStatus] = PROP_NAME_maritalStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_maritalStatus, PROP_ID_maritalStatus);
      
          PROP_ID_TO_NAME[PROP_ID_nationality] = PROP_NAME_nationality;
          PROP_NAME_TO_ID.put(PROP_NAME_nationality, PROP_ID_nationality);
      
          PROP_ID_TO_NAME[PROP_ID_emergencyContact] = PROP_NAME_emergencyContact;
          PROP_NAME_TO_ID.put(PROP_NAME_emergencyContact, PROP_ID_emergencyContact);
      
          PROP_ID_TO_NAME[PROP_ID_emergencyPhone] = PROP_NAME_emergencyPhone;
          PROP_NAME_TO_ID.put(PROP_NAME_emergencyPhone, PROP_ID_emergencyPhone);
      
          PROP_ID_TO_NAME[PROP_ID_departmentId] = PROP_NAME_departmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_departmentId, PROP_ID_departmentId);
      
          PROP_ID_TO_NAME[PROP_ID_positionId] = PROP_NAME_positionId;
          PROP_NAME_TO_ID.put(PROP_NAME_positionId, PROP_ID_positionId);
      
          PROP_ID_TO_NAME[PROP_ID_jobTitle] = PROP_NAME_jobTitle;
          PROP_NAME_TO_ID.put(PROP_NAME_jobTitle, PROP_ID_jobTitle);
      
          PROP_ID_TO_NAME[PROP_ID_superiorId] = PROP_NAME_superiorId;
          PROP_NAME_TO_ID.put(PROP_NAME_superiorId, PROP_ID_superiorId);
      
          PROP_ID_TO_NAME[PROP_ID_costCenterId] = PROP_NAME_costCenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_costCenterId, PROP_ID_costCenterId);
      
          PROP_ID_TO_NAME[PROP_ID_hireDate] = PROP_NAME_hireDate;
          PROP_NAME_TO_ID.put(PROP_NAME_hireDate, PROP_ID_hireDate);
      
          PROP_ID_TO_NAME[PROP_ID_probationEndDate] = PROP_NAME_probationEndDate;
          PROP_NAME_TO_ID.put(PROP_NAME_probationEndDate, PROP_ID_probationEndDate);
      
          PROP_ID_TO_NAME[PROP_ID_regularDate] = PROP_NAME_regularDate;
          PROP_NAME_TO_ID.put(PROP_NAME_regularDate, PROP_ID_regularDate);
      
          PROP_ID_TO_NAME[PROP_ID_resignationDate] = PROP_NAME_resignationDate;
          PROP_NAME_TO_ID.put(PROP_NAME_resignationDate, PROP_ID_resignationDate);
      
          PROP_ID_TO_NAME[PROP_ID_resignationReason] = PROP_NAME_resignationReason;
          PROP_NAME_TO_ID.put(PROP_NAME_resignationReason, PROP_ID_resignationReason);
      
          PROP_ID_TO_NAME[PROP_ID_employmentStatus] = PROP_NAME_employmentStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_employmentStatus, PROP_ID_employmentStatus);
      
          PROP_ID_TO_NAME[PROP_ID_employeeType] = PROP_NAME_employeeType;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeType, PROP_ID_employeeType);
      
          PROP_ID_TO_NAME[PROP_ID_bankAccountId] = PROP_NAME_bankAccountId;
          PROP_NAME_TO_ID.put(PROP_NAME_bankAccountId, PROP_ID_bankAccountId);
      
          PROP_ID_TO_NAME[PROP_ID_socialSecurityNo] = PROP_NAME_socialSecurityNo;
          PROP_NAME_TO_ID.put(PROP_NAME_socialSecurityNo, PROP_ID_socialSecurityNo);
      
          PROP_ID_TO_NAME[PROP_ID_taxFileNo] = PROP_NAME_taxFileNo;
          PROP_NAME_TO_ID.put(PROP_NAME_taxFileNo, PROP_ID_taxFileNo);
      
          PROP_ID_TO_NAME[PROP_ID_userAccountId] = PROP_NAME_userAccountId;
          PROP_NAME_TO_ID.put(PROP_NAME_userAccountId, PROP_ID_userAccountId);
      
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
    
    /* 工号: CODE */
    private java.lang.String _code;
    
    /* 姓: FIRST_NAME */
    private java.lang.String _firstName;
    
    /* 名: LAST_NAME */
    private java.lang.String _lastName;
    
    /* 全名: FULL_NAME */
    private java.lang.String _fullName;
    
    /* 性别: GENDER */
    private java.lang.Integer _gender;
    
    /* 出生日期: BIRTH_DATE */
    private java.time.LocalDate _birthDate;
    
    /* 证件类型: ID_CARD_TYPE */
    private java.lang.String _idCardType;
    
    /* 证件号码: ID_CARD_NO */
    private java.lang.String _idCardNo;
    
    /* 电子邮箱: EMAIL */
    private java.lang.String _email;
    
    /* 手机号: MOBILE_PHONE */
    private java.lang.String _mobilePhone;
    
    /* 婚姻状况: MARITAL_STATUS */
    private java.lang.Integer _maritalStatus;
    
    /* 国籍: NATIONALITY */
    private java.lang.String _nationality;
    
    /* 紧急联系人: EMERGENCY_CONTACT */
    private java.lang.String _emergencyContact;
    
    /* 紧急联系电话: EMERGENCY_PHONE */
    private java.lang.String _emergencyPhone;
    
    /* 部门: DEPARTMENT_ID */
    private java.lang.Long _departmentId;
    
    /* 职位: POSITION_ID */
    private java.lang.Long _positionId;
    
    /* 岗位名称: JOB_TITLE */
    private java.lang.String _jobTitle;
    
    /* 直接上级: SUPERIOR_ID */
    private java.lang.Long _superiorId;
    
    /* 默认成本中心: COST_CENTER_ID */
    private java.lang.Long _costCenterId;
    
    /* 入职日期: HIRE_DATE */
    private java.time.LocalDate _hireDate;
    
    /* 试用期截止: PROBATION_END_DATE */
    private java.time.LocalDate _probationEndDate;
    
    /* 转正日期: REGULAR_DATE */
    private java.time.LocalDate _regularDate;
    
    /* 离职日期: RESIGNATION_DATE */
    private java.time.LocalDate _resignationDate;
    
    /* 离职原因: RESIGNATION_REASON */
    private java.lang.String _resignationReason;
    
    /* 雇佣状态: EMPLOYMENT_STATUS */
    private java.lang.Integer _employmentStatus;
    
    /* 员工类型: EMPLOYEE_TYPE */
    private java.lang.Integer _employeeType;
    
    /* 工资卡账户: BANK_ACCOUNT_ID */
    private java.lang.Long _bankAccountId;
    
    /* 社保号: SOCIAL_SECURITY_NO */
    private java.lang.String _socialSecurityNo;
    
    /* 个税档案号: TAX_FILE_NO */
    private java.lang.String _taxFileNo;
    
    /* 系统用户ID: USER_ACCOUNT_ID */
    private java.lang.Long _userAccountId;
    
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
    

    public _ErpHrEmployee(){
        // for debug
    }

    protected ErpHrEmployee newInstance(){
        ErpHrEmployee entity = new ErpHrEmployee();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrEmployee cloneInstance() {
        ErpHrEmployee entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrEmployee";
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
        
            case PROP_ID_firstName:
               return getFirstName();
        
            case PROP_ID_lastName:
               return getLastName();
        
            case PROP_ID_fullName:
               return getFullName();
        
            case PROP_ID_gender:
               return getGender();
        
            case PROP_ID_birthDate:
               return getBirthDate();
        
            case PROP_ID_idCardType:
               return getIdCardType();
        
            case PROP_ID_idCardNo:
               return getIdCardNo();
        
            case PROP_ID_email:
               return getEmail();
        
            case PROP_ID_mobilePhone:
               return getMobilePhone();
        
            case PROP_ID_maritalStatus:
               return getMaritalStatus();
        
            case PROP_ID_nationality:
               return getNationality();
        
            case PROP_ID_emergencyContact:
               return getEmergencyContact();
        
            case PROP_ID_emergencyPhone:
               return getEmergencyPhone();
        
            case PROP_ID_departmentId:
               return getDepartmentId();
        
            case PROP_ID_positionId:
               return getPositionId();
        
            case PROP_ID_jobTitle:
               return getJobTitle();
        
            case PROP_ID_superiorId:
               return getSuperiorId();
        
            case PROP_ID_costCenterId:
               return getCostCenterId();
        
            case PROP_ID_hireDate:
               return getHireDate();
        
            case PROP_ID_probationEndDate:
               return getProbationEndDate();
        
            case PROP_ID_regularDate:
               return getRegularDate();
        
            case PROP_ID_resignationDate:
               return getResignationDate();
        
            case PROP_ID_resignationReason:
               return getResignationReason();
        
            case PROP_ID_employmentStatus:
               return getEmploymentStatus();
        
            case PROP_ID_employeeType:
               return getEmployeeType();
        
            case PROP_ID_bankAccountId:
               return getBankAccountId();
        
            case PROP_ID_socialSecurityNo:
               return getSocialSecurityNo();
        
            case PROP_ID_taxFileNo:
               return getTaxFileNo();
        
            case PROP_ID_userAccountId:
               return getUserAccountId();
        
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
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
               break;
            }
        
            case PROP_ID_firstName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_firstName));
               }
               setFirstName(typedValue);
               break;
            }
        
            case PROP_ID_lastName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lastName));
               }
               setLastName(typedValue);
               break;
            }
        
            case PROP_ID_fullName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fullName));
               }
               setFullName(typedValue);
               break;
            }
        
            case PROP_ID_gender:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_gender));
               }
               setGender(typedValue);
               break;
            }
        
            case PROP_ID_birthDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_birthDate));
               }
               setBirthDate(typedValue);
               break;
            }
        
            case PROP_ID_idCardType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_idCardType));
               }
               setIdCardType(typedValue);
               break;
            }
        
            case PROP_ID_idCardNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_idCardNo));
               }
               setIdCardNo(typedValue);
               break;
            }
        
            case PROP_ID_email:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_email));
               }
               setEmail(typedValue);
               break;
            }
        
            case PROP_ID_mobilePhone:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_mobilePhone));
               }
               setMobilePhone(typedValue);
               break;
            }
        
            case PROP_ID_maritalStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_maritalStatus));
               }
               setMaritalStatus(typedValue);
               break;
            }
        
            case PROP_ID_nationality:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_nationality));
               }
               setNationality(typedValue);
               break;
            }
        
            case PROP_ID_emergencyContact:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_emergencyContact));
               }
               setEmergencyContact(typedValue);
               break;
            }
        
            case PROP_ID_emergencyPhone:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_emergencyPhone));
               }
               setEmergencyPhone(typedValue);
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
        
            case PROP_ID_positionId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_positionId));
               }
               setPositionId(typedValue);
               break;
            }
        
            case PROP_ID_jobTitle:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_jobTitle));
               }
               setJobTitle(typedValue);
               break;
            }
        
            case PROP_ID_superiorId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_superiorId));
               }
               setSuperiorId(typedValue);
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
        
            case PROP_ID_hireDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_hireDate));
               }
               setHireDate(typedValue);
               break;
            }
        
            case PROP_ID_probationEndDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_probationEndDate));
               }
               setProbationEndDate(typedValue);
               break;
            }
        
            case PROP_ID_regularDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_regularDate));
               }
               setRegularDate(typedValue);
               break;
            }
        
            case PROP_ID_resignationDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_resignationDate));
               }
               setResignationDate(typedValue);
               break;
            }
        
            case PROP_ID_resignationReason:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_resignationReason));
               }
               setResignationReason(typedValue);
               break;
            }
        
            case PROP_ID_employmentStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_employmentStatus));
               }
               setEmploymentStatus(typedValue);
               break;
            }
        
            case PROP_ID_employeeType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_employeeType));
               }
               setEmployeeType(typedValue);
               break;
            }
        
            case PROP_ID_bankAccountId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_bankAccountId));
               }
               setBankAccountId(typedValue);
               break;
            }
        
            case PROP_ID_socialSecurityNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_socialSecurityNo));
               }
               setSocialSecurityNo(typedValue);
               break;
            }
        
            case PROP_ID_taxFileNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_taxFileNo));
               }
               setTaxFileNo(typedValue);
               break;
            }
        
            case PROP_ID_userAccountId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_userAccountId));
               }
               setUserAccountId(typedValue);
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
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_firstName:{
               onInitProp(propId);
               this._firstName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_lastName:{
               onInitProp(propId);
               this._lastName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fullName:{
               onInitProp(propId);
               this._fullName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_gender:{
               onInitProp(propId);
               this._gender = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_birthDate:{
               onInitProp(propId);
               this._birthDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_idCardType:{
               onInitProp(propId);
               this._idCardType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_idCardNo:{
               onInitProp(propId);
               this._idCardNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_email:{
               onInitProp(propId);
               this._email = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_mobilePhone:{
               onInitProp(propId);
               this._mobilePhone = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_maritalStatus:{
               onInitProp(propId);
               this._maritalStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_nationality:{
               onInitProp(propId);
               this._nationality = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_emergencyContact:{
               onInitProp(propId);
               this._emergencyContact = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_emergencyPhone:{
               onInitProp(propId);
               this._emergencyPhone = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_departmentId:{
               onInitProp(propId);
               this._departmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_positionId:{
               onInitProp(propId);
               this._positionId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_jobTitle:{
               onInitProp(propId);
               this._jobTitle = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_superiorId:{
               onInitProp(propId);
               this._superiorId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_costCenterId:{
               onInitProp(propId);
               this._costCenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_hireDate:{
               onInitProp(propId);
               this._hireDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_probationEndDate:{
               onInitProp(propId);
               this._probationEndDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_regularDate:{
               onInitProp(propId);
               this._regularDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_resignationDate:{
               onInitProp(propId);
               this._resignationDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_resignationReason:{
               onInitProp(propId);
               this._resignationReason = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_employmentStatus:{
               onInitProp(propId);
               this._employmentStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_employeeType:{
               onInitProp(propId);
               this._employeeType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_bankAccountId:{
               onInitProp(propId);
               this._bankAccountId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_socialSecurityNo:{
               onInitProp(propId);
               this._socialSecurityNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_taxFileNo:{
               onInitProp(propId);
               this._taxFileNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_userAccountId:{
               onInitProp(propId);
               this._userAccountId = (java.lang.Long)value;
               
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
     * 工号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 工号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 姓: FIRST_NAME
     */
    public final java.lang.String getFirstName(){
         onPropGet(PROP_ID_firstName);
         return _firstName;
    }

    /**
     * 姓: FIRST_NAME
     */
    public final void setFirstName(java.lang.String value){
        if(onPropSet(PROP_ID_firstName,value)){
            this._firstName = value;
            internalClearRefs(PROP_ID_firstName);
            
        }
    }
    
    /**
     * 名: LAST_NAME
     */
    public final java.lang.String getLastName(){
         onPropGet(PROP_ID_lastName);
         return _lastName;
    }

    /**
     * 名: LAST_NAME
     */
    public final void setLastName(java.lang.String value){
        if(onPropSet(PROP_ID_lastName,value)){
            this._lastName = value;
            internalClearRefs(PROP_ID_lastName);
            
        }
    }
    
    /**
     * 全名: FULL_NAME
     */
    public final java.lang.String getFullName(){
         onPropGet(PROP_ID_fullName);
         return _fullName;
    }

    /**
     * 全名: FULL_NAME
     */
    public final void setFullName(java.lang.String value){
        if(onPropSet(PROP_ID_fullName,value)){
            this._fullName = value;
            internalClearRefs(PROP_ID_fullName);
            
        }
    }
    
    /**
     * 性别: GENDER
     */
    public final java.lang.Integer getGender(){
         onPropGet(PROP_ID_gender);
         return _gender;
    }

    /**
     * 性别: GENDER
     */
    public final void setGender(java.lang.Integer value){
        if(onPropSet(PROP_ID_gender,value)){
            this._gender = value;
            internalClearRefs(PROP_ID_gender);
            
        }
    }
    
    /**
     * 出生日期: BIRTH_DATE
     */
    public final java.time.LocalDate getBirthDate(){
         onPropGet(PROP_ID_birthDate);
         return _birthDate;
    }

    /**
     * 出生日期: BIRTH_DATE
     */
    public final void setBirthDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_birthDate,value)){
            this._birthDate = value;
            internalClearRefs(PROP_ID_birthDate);
            
        }
    }
    
    /**
     * 证件类型: ID_CARD_TYPE
     */
    public final java.lang.String getIdCardType(){
         onPropGet(PROP_ID_idCardType);
         return _idCardType;
    }

    /**
     * 证件类型: ID_CARD_TYPE
     */
    public final void setIdCardType(java.lang.String value){
        if(onPropSet(PROP_ID_idCardType,value)){
            this._idCardType = value;
            internalClearRefs(PROP_ID_idCardType);
            
        }
    }
    
    /**
     * 证件号码: ID_CARD_NO
     */
    public final java.lang.String getIdCardNo(){
         onPropGet(PROP_ID_idCardNo);
         return _idCardNo;
    }

    /**
     * 证件号码: ID_CARD_NO
     */
    public final void setIdCardNo(java.lang.String value){
        if(onPropSet(PROP_ID_idCardNo,value)){
            this._idCardNo = value;
            internalClearRefs(PROP_ID_idCardNo);
            
        }
    }
    
    /**
     * 电子邮箱: EMAIL
     */
    public final java.lang.String getEmail(){
         onPropGet(PROP_ID_email);
         return _email;
    }

    /**
     * 电子邮箱: EMAIL
     */
    public final void setEmail(java.lang.String value){
        if(onPropSet(PROP_ID_email,value)){
            this._email = value;
            internalClearRefs(PROP_ID_email);
            
        }
    }
    
    /**
     * 手机号: MOBILE_PHONE
     */
    public final java.lang.String getMobilePhone(){
         onPropGet(PROP_ID_mobilePhone);
         return _mobilePhone;
    }

    /**
     * 手机号: MOBILE_PHONE
     */
    public final void setMobilePhone(java.lang.String value){
        if(onPropSet(PROP_ID_mobilePhone,value)){
            this._mobilePhone = value;
            internalClearRefs(PROP_ID_mobilePhone);
            
        }
    }
    
    /**
     * 婚姻状况: MARITAL_STATUS
     */
    public final java.lang.Integer getMaritalStatus(){
         onPropGet(PROP_ID_maritalStatus);
         return _maritalStatus;
    }

    /**
     * 婚姻状况: MARITAL_STATUS
     */
    public final void setMaritalStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_maritalStatus,value)){
            this._maritalStatus = value;
            internalClearRefs(PROP_ID_maritalStatus);
            
        }
    }
    
    /**
     * 国籍: NATIONALITY
     */
    public final java.lang.String getNationality(){
         onPropGet(PROP_ID_nationality);
         return _nationality;
    }

    /**
     * 国籍: NATIONALITY
     */
    public final void setNationality(java.lang.String value){
        if(onPropSet(PROP_ID_nationality,value)){
            this._nationality = value;
            internalClearRefs(PROP_ID_nationality);
            
        }
    }
    
    /**
     * 紧急联系人: EMERGENCY_CONTACT
     */
    public final java.lang.String getEmergencyContact(){
         onPropGet(PROP_ID_emergencyContact);
         return _emergencyContact;
    }

    /**
     * 紧急联系人: EMERGENCY_CONTACT
     */
    public final void setEmergencyContact(java.lang.String value){
        if(onPropSet(PROP_ID_emergencyContact,value)){
            this._emergencyContact = value;
            internalClearRefs(PROP_ID_emergencyContact);
            
        }
    }
    
    /**
     * 紧急联系电话: EMERGENCY_PHONE
     */
    public final java.lang.String getEmergencyPhone(){
         onPropGet(PROP_ID_emergencyPhone);
         return _emergencyPhone;
    }

    /**
     * 紧急联系电话: EMERGENCY_PHONE
     */
    public final void setEmergencyPhone(java.lang.String value){
        if(onPropSet(PROP_ID_emergencyPhone,value)){
            this._emergencyPhone = value;
            internalClearRefs(PROP_ID_emergencyPhone);
            
        }
    }
    
    /**
     * 部门: DEPARTMENT_ID
     */
    public final java.lang.Long getDepartmentId(){
         onPropGet(PROP_ID_departmentId);
         return _departmentId;
    }

    /**
     * 部门: DEPARTMENT_ID
     */
    public final void setDepartmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_departmentId,value)){
            this._departmentId = value;
            internalClearRefs(PROP_ID_departmentId);
            
        }
    }
    
    /**
     * 职位: POSITION_ID
     */
    public final java.lang.Long getPositionId(){
         onPropGet(PROP_ID_positionId);
         return _positionId;
    }

    /**
     * 职位: POSITION_ID
     */
    public final void setPositionId(java.lang.Long value){
        if(onPropSet(PROP_ID_positionId,value)){
            this._positionId = value;
            internalClearRefs(PROP_ID_positionId);
            
        }
    }
    
    /**
     * 岗位名称: JOB_TITLE
     */
    public final java.lang.String getJobTitle(){
         onPropGet(PROP_ID_jobTitle);
         return _jobTitle;
    }

    /**
     * 岗位名称: JOB_TITLE
     */
    public final void setJobTitle(java.lang.String value){
        if(onPropSet(PROP_ID_jobTitle,value)){
            this._jobTitle = value;
            internalClearRefs(PROP_ID_jobTitle);
            
        }
    }
    
    /**
     * 直接上级: SUPERIOR_ID
     */
    public final java.lang.Long getSuperiorId(){
         onPropGet(PROP_ID_superiorId);
         return _superiorId;
    }

    /**
     * 直接上级: SUPERIOR_ID
     */
    public final void setSuperiorId(java.lang.Long value){
        if(onPropSet(PROP_ID_superiorId,value)){
            this._superiorId = value;
            internalClearRefs(PROP_ID_superiorId);
            
        }
    }
    
    /**
     * 默认成本中心: COST_CENTER_ID
     */
    public final java.lang.Long getCostCenterId(){
         onPropGet(PROP_ID_costCenterId);
         return _costCenterId;
    }

    /**
     * 默认成本中心: COST_CENTER_ID
     */
    public final void setCostCenterId(java.lang.Long value){
        if(onPropSet(PROP_ID_costCenterId,value)){
            this._costCenterId = value;
            internalClearRefs(PROP_ID_costCenterId);
            
        }
    }
    
    /**
     * 入职日期: HIRE_DATE
     */
    public final java.time.LocalDate getHireDate(){
         onPropGet(PROP_ID_hireDate);
         return _hireDate;
    }

    /**
     * 入职日期: HIRE_DATE
     */
    public final void setHireDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_hireDate,value)){
            this._hireDate = value;
            internalClearRefs(PROP_ID_hireDate);
            
        }
    }
    
    /**
     * 试用期截止: PROBATION_END_DATE
     */
    public final java.time.LocalDate getProbationEndDate(){
         onPropGet(PROP_ID_probationEndDate);
         return _probationEndDate;
    }

    /**
     * 试用期截止: PROBATION_END_DATE
     */
    public final void setProbationEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_probationEndDate,value)){
            this._probationEndDate = value;
            internalClearRefs(PROP_ID_probationEndDate);
            
        }
    }
    
    /**
     * 转正日期: REGULAR_DATE
     */
    public final java.time.LocalDate getRegularDate(){
         onPropGet(PROP_ID_regularDate);
         return _regularDate;
    }

    /**
     * 转正日期: REGULAR_DATE
     */
    public final void setRegularDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_regularDate,value)){
            this._regularDate = value;
            internalClearRefs(PROP_ID_regularDate);
            
        }
    }
    
    /**
     * 离职日期: RESIGNATION_DATE
     */
    public final java.time.LocalDate getResignationDate(){
         onPropGet(PROP_ID_resignationDate);
         return _resignationDate;
    }

    /**
     * 离职日期: RESIGNATION_DATE
     */
    public final void setResignationDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_resignationDate,value)){
            this._resignationDate = value;
            internalClearRefs(PROP_ID_resignationDate);
            
        }
    }
    
    /**
     * 离职原因: RESIGNATION_REASON
     */
    public final java.lang.String getResignationReason(){
         onPropGet(PROP_ID_resignationReason);
         return _resignationReason;
    }

    /**
     * 离职原因: RESIGNATION_REASON
     */
    public final void setResignationReason(java.lang.String value){
        if(onPropSet(PROP_ID_resignationReason,value)){
            this._resignationReason = value;
            internalClearRefs(PROP_ID_resignationReason);
            
        }
    }
    
    /**
     * 雇佣状态: EMPLOYMENT_STATUS
     */
    public final java.lang.Integer getEmploymentStatus(){
         onPropGet(PROP_ID_employmentStatus);
         return _employmentStatus;
    }

    /**
     * 雇佣状态: EMPLOYMENT_STATUS
     */
    public final void setEmploymentStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_employmentStatus,value)){
            this._employmentStatus = value;
            internalClearRefs(PROP_ID_employmentStatus);
            
        }
    }
    
    /**
     * 员工类型: EMPLOYEE_TYPE
     */
    public final java.lang.Integer getEmployeeType(){
         onPropGet(PROP_ID_employeeType);
         return _employeeType;
    }

    /**
     * 员工类型: EMPLOYEE_TYPE
     */
    public final void setEmployeeType(java.lang.Integer value){
        if(onPropSet(PROP_ID_employeeType,value)){
            this._employeeType = value;
            internalClearRefs(PROP_ID_employeeType);
            
        }
    }
    
    /**
     * 工资卡账户: BANK_ACCOUNT_ID
     */
    public final java.lang.Long getBankAccountId(){
         onPropGet(PROP_ID_bankAccountId);
         return _bankAccountId;
    }

    /**
     * 工资卡账户: BANK_ACCOUNT_ID
     */
    public final void setBankAccountId(java.lang.Long value){
        if(onPropSet(PROP_ID_bankAccountId,value)){
            this._bankAccountId = value;
            internalClearRefs(PROP_ID_bankAccountId);
            
        }
    }
    
    /**
     * 社保号: SOCIAL_SECURITY_NO
     */
    public final java.lang.String getSocialSecurityNo(){
         onPropGet(PROP_ID_socialSecurityNo);
         return _socialSecurityNo;
    }

    /**
     * 社保号: SOCIAL_SECURITY_NO
     */
    public final void setSocialSecurityNo(java.lang.String value){
        if(onPropSet(PROP_ID_socialSecurityNo,value)){
            this._socialSecurityNo = value;
            internalClearRefs(PROP_ID_socialSecurityNo);
            
        }
    }
    
    /**
     * 个税档案号: TAX_FILE_NO
     */
    public final java.lang.String getTaxFileNo(){
         onPropGet(PROP_ID_taxFileNo);
         return _taxFileNo;
    }

    /**
     * 个税档案号: TAX_FILE_NO
     */
    public final void setTaxFileNo(java.lang.String value){
        if(onPropSet(PROP_ID_taxFileNo,value)){
            this._taxFileNo = value;
            internalClearRefs(PROP_ID_taxFileNo);
            
        }
    }
    
    /**
     * 系统用户ID: USER_ACCOUNT_ID
     */
    public final java.lang.Long getUserAccountId(){
         onPropGet(PROP_ID_userAccountId);
         return _userAccountId;
    }

    /**
     * 系统用户ID: USER_ACCOUNT_ID
     */
    public final void setUserAccountId(java.lang.Long value){
        if(onPropSet(PROP_ID_userAccountId,value)){
            this._userAccountId = value;
            internalClearRefs(PROP_ID_userAccountId);
            
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
    public final app.erp.hr.dao.entity.ErpHrDepartment getDepartment(){
       return (app.erp.hr.dao.entity.ErpHrDepartment)internalGetRefEntity(PROP_NAME_department);
    }

    public final void setDepartment(app.erp.hr.dao.entity.ErpHrDepartment refEntity){
   
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
    public final app.erp.hr.dao.entity.ErpHrPosition getPosition(){
       return (app.erp.hr.dao.entity.ErpHrPosition)internalGetRefEntity(PROP_NAME_position);
    }

    public final void setPosition(app.erp.hr.dao.entity.ErpHrPosition refEntity){
   
           if(refEntity == null){
           
                   this.setPositionId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_position, refEntity,()->{
           
                           this.setPositionId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrEmployee getSuperior(){
       return (app.erp.hr.dao.entity.ErpHrEmployee)internalGetRefEntity(PROP_NAME_superior);
    }

    public final void setSuperior(app.erp.hr.dao.entity.ErpHrEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setSuperiorId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_superior, refEntity,()->{
           
                           this.setSuperiorId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdBankAccount getBankAccount(){
       return (app.erp.md.dao.entity.ErpMdBankAccount)internalGetRefEntity(PROP_NAME_bankAccount);
    }

    public final void setBankAccount(app.erp.md.dao.entity.ErpMdBankAccount refEntity){
   
           if(refEntity == null){
           
                   this.setBankAccountId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_bankAccount, refEntity,()->{
           
                           this.setBankAccountId(refEntity.getId());
                       
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
