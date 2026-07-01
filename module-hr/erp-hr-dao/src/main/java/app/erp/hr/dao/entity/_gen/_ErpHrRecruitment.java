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

import app.erp.hr.dao.entity.ErpHrRecruitment;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  招聘记录: erp_hr_recruitment
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrRecruitment extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 招聘职位: POSITION_ID BIGINT */
    public static final String PROP_NAME_positionId = "positionId";
    public static final int PROP_ID_positionId = 3;
    
    /* 招聘部门: DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_departmentId = "departmentId";
    public static final int PROP_ID_departmentId = 4;
    
    /* 招聘人数: HEADCOUNT INTEGER */
    public static final String PROP_NAME_headcount = "headcount";
    public static final int PROP_ID_headcount = 5;
    
    /* 应聘者姓名: CANDIDATE_NAME VARCHAR */
    public static final String PROP_NAME_candidateName = "candidateName";
    public static final int PROP_ID_candidateName = 6;
    
    /* 联系电话: CANDIDATE_PHONE VARCHAR */
    public static final String PROP_NAME_candidatePhone = "candidatePhone";
    public static final int PROP_ID_candidatePhone = 7;
    
    /* 电子邮箱: CANDIDATE_EMAIL VARCHAR */
    public static final String PROP_NAME_candidateEmail = "candidateEmail";
    public static final int PROP_ID_candidateEmail = 8;
    
    /* 来源: SOURCE INTEGER */
    public static final String PROP_NAME_source = "source";
    public static final int PROP_ID_source = 9;
    
    /* 简历附件: RESUME_ATTACHMENT_FILE_ID VARCHAR */
    public static final String PROP_NAME_resumeAttachmentFileId = "resumeAttachmentFileId";
    public static final int PROP_ID_resumeAttachmentFileId = 10;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 11;
    
    /* 面试官: INTERVIEWER_ID BIGINT */
    public static final String PROP_NAME_interviewerId = "interviewerId";
    public static final int PROP_ID_interviewerId = 12;
    
    /* 面试日期: INTERVIEW_DATE DATE */
    public static final String PROP_NAME_interviewDate = "interviewDate";
    public static final int PROP_ID_interviewDate = 13;
    
    /* Offer薪资: OFFER_SALARY DECIMAL */
    public static final String PROP_NAME_offerSalary = "offerSalary";
    public static final int PROP_ID_offerSalary = 14;
    
    /* 入职日期: HIRED_DATE DATE */
    public static final String PROP_NAME_hiredDate = "hiredDate";
    public static final int PROP_ID_hiredDate = 15;
    
    /* 关联员工: EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_employeeId = "employeeId";
    public static final int PROP_ID_employeeId = 16;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 17;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 18;
    
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
    public static final String PROP_NAME_position = "position";
    
    /* relation:  */
    public static final String PROP_NAME_department = "department";
    
    /* relation:  */
    public static final String PROP_NAME_employee = "employee";
    
    /* relation:  */
    public static final String PROP_NAME_interviewer = "interviewer";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* component:  */
    public static final String PROP_NAME_resumeAttachmentFileIdComponent = "resumeAttachmentFileIdComponent";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[25];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_positionId] = PROP_NAME_positionId;
          PROP_NAME_TO_ID.put(PROP_NAME_positionId, PROP_ID_positionId);
      
          PROP_ID_TO_NAME[PROP_ID_departmentId] = PROP_NAME_departmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_departmentId, PROP_ID_departmentId);
      
          PROP_ID_TO_NAME[PROP_ID_headcount] = PROP_NAME_headcount;
          PROP_NAME_TO_ID.put(PROP_NAME_headcount, PROP_ID_headcount);
      
          PROP_ID_TO_NAME[PROP_ID_candidateName] = PROP_NAME_candidateName;
          PROP_NAME_TO_ID.put(PROP_NAME_candidateName, PROP_ID_candidateName);
      
          PROP_ID_TO_NAME[PROP_ID_candidatePhone] = PROP_NAME_candidatePhone;
          PROP_NAME_TO_ID.put(PROP_NAME_candidatePhone, PROP_ID_candidatePhone);
      
          PROP_ID_TO_NAME[PROP_ID_candidateEmail] = PROP_NAME_candidateEmail;
          PROP_NAME_TO_ID.put(PROP_NAME_candidateEmail, PROP_ID_candidateEmail);
      
          PROP_ID_TO_NAME[PROP_ID_source] = PROP_NAME_source;
          PROP_NAME_TO_ID.put(PROP_NAME_source, PROP_ID_source);
      
          PROP_ID_TO_NAME[PROP_ID_resumeAttachmentFileId] = PROP_NAME_resumeAttachmentFileId;
          PROP_NAME_TO_ID.put(PROP_NAME_resumeAttachmentFileId, PROP_ID_resumeAttachmentFileId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_interviewerId] = PROP_NAME_interviewerId;
          PROP_NAME_TO_ID.put(PROP_NAME_interviewerId, PROP_ID_interviewerId);
      
          PROP_ID_TO_NAME[PROP_ID_interviewDate] = PROP_NAME_interviewDate;
          PROP_NAME_TO_ID.put(PROP_NAME_interviewDate, PROP_ID_interviewDate);
      
          PROP_ID_TO_NAME[PROP_ID_offerSalary] = PROP_NAME_offerSalary;
          PROP_NAME_TO_ID.put(PROP_NAME_offerSalary, PROP_ID_offerSalary);
      
          PROP_ID_TO_NAME[PROP_ID_hiredDate] = PROP_NAME_hiredDate;
          PROP_NAME_TO_ID.put(PROP_NAME_hiredDate, PROP_ID_hiredDate);
      
          PROP_ID_TO_NAME[PROP_ID_employeeId] = PROP_NAME_employeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeId, PROP_ID_employeeId);
      
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
    
    /* 编号: CODE */
    private java.lang.String _code;
    
    /* 招聘职位: POSITION_ID */
    private java.lang.Long _positionId;
    
    /* 招聘部门: DEPARTMENT_ID */
    private java.lang.Long _departmentId;
    
    /* 招聘人数: HEADCOUNT */
    private java.lang.Integer _headcount;
    
    /* 应聘者姓名: CANDIDATE_NAME */
    private java.lang.String _candidateName;
    
    /* 联系电话: CANDIDATE_PHONE */
    private java.lang.String _candidatePhone;
    
    /* 电子邮箱: CANDIDATE_EMAIL */
    private java.lang.String _candidateEmail;
    
    /* 来源: SOURCE */
    private java.lang.Integer _source;
    
    /* 简历附件: RESUME_ATTACHMENT_FILE_ID */
    private java.lang.String _resumeAttachmentFileId;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 面试官: INTERVIEWER_ID */
    private java.lang.Long _interviewerId;
    
    /* 面试日期: INTERVIEW_DATE */
    private java.time.LocalDate _interviewDate;
    
    /* Offer薪资: OFFER_SALARY */
    private java.math.BigDecimal _offerSalary;
    
    /* 入职日期: HIRED_DATE */
    private java.time.LocalDate _hiredDate;
    
    /* 关联员工: EMPLOYEE_ID */
    private java.lang.Long _employeeId;
    
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
    

    public _ErpHrRecruitment(){
        // for debug
    }

    protected ErpHrRecruitment newInstance(){
        ErpHrRecruitment entity = new ErpHrRecruitment();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrRecruitment cloneInstance() {
        ErpHrRecruitment entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrRecruitment";
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
        
            case PROP_ID_positionId:
               return getPositionId();
        
            case PROP_ID_departmentId:
               return getDepartmentId();
        
            case PROP_ID_headcount:
               return getHeadcount();
        
            case PROP_ID_candidateName:
               return getCandidateName();
        
            case PROP_ID_candidatePhone:
               return getCandidatePhone();
        
            case PROP_ID_candidateEmail:
               return getCandidateEmail();
        
            case PROP_ID_source:
               return getSource();
        
            case PROP_ID_resumeAttachmentFileId:
               return getResumeAttachmentFileId();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_interviewerId:
               return getInterviewerId();
        
            case PROP_ID_interviewDate:
               return getInterviewDate();
        
            case PROP_ID_offerSalary:
               return getOfferSalary();
        
            case PROP_ID_hiredDate:
               return getHiredDate();
        
            case PROP_ID_employeeId:
               return getEmployeeId();
        
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
        
            case PROP_ID_positionId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_positionId));
               }
               setPositionId(typedValue);
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
        
            case PROP_ID_headcount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_headcount));
               }
               setHeadcount(typedValue);
               break;
            }
        
            case PROP_ID_candidateName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_candidateName));
               }
               setCandidateName(typedValue);
               break;
            }
        
            case PROP_ID_candidatePhone:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_candidatePhone));
               }
               setCandidatePhone(typedValue);
               break;
            }
        
            case PROP_ID_candidateEmail:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_candidateEmail));
               }
               setCandidateEmail(typedValue);
               break;
            }
        
            case PROP_ID_source:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_source));
               }
               setSource(typedValue);
               break;
            }
        
            case PROP_ID_resumeAttachmentFileId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_resumeAttachmentFileId));
               }
               setResumeAttachmentFileId(typedValue);
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
        
            case PROP_ID_interviewerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_interviewerId));
               }
               setInterviewerId(typedValue);
               break;
            }
        
            case PROP_ID_interviewDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_interviewDate));
               }
               setInterviewDate(typedValue);
               break;
            }
        
            case PROP_ID_offerSalary:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_offerSalary));
               }
               setOfferSalary(typedValue);
               break;
            }
        
            case PROP_ID_hiredDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_hiredDate));
               }
               setHiredDate(typedValue);
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
        
            case PROP_ID_positionId:{
               onInitProp(propId);
               this._positionId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_departmentId:{
               onInitProp(propId);
               this._departmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_headcount:{
               onInitProp(propId);
               this._headcount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_candidateName:{
               onInitProp(propId);
               this._candidateName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_candidatePhone:{
               onInitProp(propId);
               this._candidatePhone = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_candidateEmail:{
               onInitProp(propId);
               this._candidateEmail = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_source:{
               onInitProp(propId);
               this._source = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_resumeAttachmentFileId:{
               onInitProp(propId);
               this._resumeAttachmentFileId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_interviewerId:{
               onInitProp(propId);
               this._interviewerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_interviewDate:{
               onInitProp(propId);
               this._interviewDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_offerSalary:{
               onInitProp(propId);
               this._offerSalary = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_hiredDate:{
               onInitProp(propId);
               this._hiredDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_employeeId:{
               onInitProp(propId);
               this._employeeId = (java.lang.Long)value;
               
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
     * 编号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 招聘职位: POSITION_ID
     */
    public final java.lang.Long getPositionId(){
         onPropGet(PROP_ID_positionId);
         return _positionId;
    }

    /**
     * 招聘职位: POSITION_ID
     */
    public final void setPositionId(java.lang.Long value){
        if(onPropSet(PROP_ID_positionId,value)){
            this._positionId = value;
            internalClearRefs(PROP_ID_positionId);
            
        }
    }
    
    /**
     * 招聘部门: DEPARTMENT_ID
     */
    public final java.lang.Long getDepartmentId(){
         onPropGet(PROP_ID_departmentId);
         return _departmentId;
    }

    /**
     * 招聘部门: DEPARTMENT_ID
     */
    public final void setDepartmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_departmentId,value)){
            this._departmentId = value;
            internalClearRefs(PROP_ID_departmentId);
            
        }
    }
    
    /**
     * 招聘人数: HEADCOUNT
     */
    public final java.lang.Integer getHeadcount(){
         onPropGet(PROP_ID_headcount);
         return _headcount;
    }

    /**
     * 招聘人数: HEADCOUNT
     */
    public final void setHeadcount(java.lang.Integer value){
        if(onPropSet(PROP_ID_headcount,value)){
            this._headcount = value;
            internalClearRefs(PROP_ID_headcount);
            
        }
    }
    
    /**
     * 应聘者姓名: CANDIDATE_NAME
     */
    public final java.lang.String getCandidateName(){
         onPropGet(PROP_ID_candidateName);
         return _candidateName;
    }

    /**
     * 应聘者姓名: CANDIDATE_NAME
     */
    public final void setCandidateName(java.lang.String value){
        if(onPropSet(PROP_ID_candidateName,value)){
            this._candidateName = value;
            internalClearRefs(PROP_ID_candidateName);
            
        }
    }
    
    /**
     * 联系电话: CANDIDATE_PHONE
     */
    public final java.lang.String getCandidatePhone(){
         onPropGet(PROP_ID_candidatePhone);
         return _candidatePhone;
    }

    /**
     * 联系电话: CANDIDATE_PHONE
     */
    public final void setCandidatePhone(java.lang.String value){
        if(onPropSet(PROP_ID_candidatePhone,value)){
            this._candidatePhone = value;
            internalClearRefs(PROP_ID_candidatePhone);
            
        }
    }
    
    /**
     * 电子邮箱: CANDIDATE_EMAIL
     */
    public final java.lang.String getCandidateEmail(){
         onPropGet(PROP_ID_candidateEmail);
         return _candidateEmail;
    }

    /**
     * 电子邮箱: CANDIDATE_EMAIL
     */
    public final void setCandidateEmail(java.lang.String value){
        if(onPropSet(PROP_ID_candidateEmail,value)){
            this._candidateEmail = value;
            internalClearRefs(PROP_ID_candidateEmail);
            
        }
    }
    
    /**
     * 来源: SOURCE
     */
    public final java.lang.Integer getSource(){
         onPropGet(PROP_ID_source);
         return _source;
    }

    /**
     * 来源: SOURCE
     */
    public final void setSource(java.lang.Integer value){
        if(onPropSet(PROP_ID_source,value)){
            this._source = value;
            internalClearRefs(PROP_ID_source);
            
        }
    }
    
    /**
     * 简历附件: RESUME_ATTACHMENT_FILE_ID
     */
    public final java.lang.String getResumeAttachmentFileId(){
         onPropGet(PROP_ID_resumeAttachmentFileId);
         return _resumeAttachmentFileId;
    }

    /**
     * 简历附件: RESUME_ATTACHMENT_FILE_ID
     */
    public final void setResumeAttachmentFileId(java.lang.String value){
        if(onPropSet(PROP_ID_resumeAttachmentFileId,value)){
            this._resumeAttachmentFileId = value;
            internalClearRefs(PROP_ID_resumeAttachmentFileId);
            
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
     * 面试官: INTERVIEWER_ID
     */
    public final java.lang.Long getInterviewerId(){
         onPropGet(PROP_ID_interviewerId);
         return _interviewerId;
    }

    /**
     * 面试官: INTERVIEWER_ID
     */
    public final void setInterviewerId(java.lang.Long value){
        if(onPropSet(PROP_ID_interviewerId,value)){
            this._interviewerId = value;
            internalClearRefs(PROP_ID_interviewerId);
            
        }
    }
    
    /**
     * 面试日期: INTERVIEW_DATE
     */
    public final java.time.LocalDate getInterviewDate(){
         onPropGet(PROP_ID_interviewDate);
         return _interviewDate;
    }

    /**
     * 面试日期: INTERVIEW_DATE
     */
    public final void setInterviewDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_interviewDate,value)){
            this._interviewDate = value;
            internalClearRefs(PROP_ID_interviewDate);
            
        }
    }
    
    /**
     * Offer薪资: OFFER_SALARY
     */
    public final java.math.BigDecimal getOfferSalary(){
         onPropGet(PROP_ID_offerSalary);
         return _offerSalary;
    }

    /**
     * Offer薪资: OFFER_SALARY
     */
    public final void setOfferSalary(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_offerSalary,value)){
            this._offerSalary = value;
            internalClearRefs(PROP_ID_offerSalary);
            
        }
    }
    
    /**
     * 入职日期: HIRED_DATE
     */
    public final java.time.LocalDate getHiredDate(){
         onPropGet(PROP_ID_hiredDate);
         return _hiredDate;
    }

    /**
     * 入职日期: HIRED_DATE
     */
    public final void setHiredDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_hiredDate,value)){
            this._hiredDate = value;
            internalClearRefs(PROP_ID_hiredDate);
            
        }
    }
    
    /**
     * 关联员工: EMPLOYEE_ID
     */
    public final java.lang.Long getEmployeeId(){
         onPropGet(PROP_ID_employeeId);
         return _employeeId;
    }

    /**
     * 关联员工: EMPLOYEE_ID
     */
    public final void setEmployeeId(java.lang.Long value){
        if(onPropSet(PROP_ID_employeeId,value)){
            this._employeeId = value;
            internalClearRefs(PROP_ID_employeeId);
            
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
    public final app.erp.hr.dao.entity.ErpHrEmployee getInterviewer(){
       return (app.erp.hr.dao.entity.ErpHrEmployee)internalGetRefEntity(PROP_NAME_interviewer);
    }

    public final void setInterviewer(app.erp.hr.dao.entity.ErpHrEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setInterviewerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_interviewer, refEntity,()->{
           
                           this.setInterviewerId(refEntity.getId());
                       
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
       
   private io.nop.orm.component.OrmFileComponent _resumeAttachmentFileIdComponent;

   private static Map<String,Integer> COMPONENT_PROP_ID_MAP_resumeAttachmentFileIdComponent = new HashMap<>();
   static{
      
         COMPONENT_PROP_ID_MAP_resumeAttachmentFileIdComponent.put(io.nop.orm.component.OrmFileComponent.PROP_NAME_filePath,PROP_ID_resumeAttachmentFileId);
      
   }

   public final io.nop.orm.component.OrmFileComponent getResumeAttachmentFileIdComponent(){
      if(_resumeAttachmentFileIdComponent == null){
          _resumeAttachmentFileIdComponent = new io.nop.orm.component.OrmFileComponent();
          _resumeAttachmentFileIdComponent.bindToEntity(this, COMPONENT_PROP_ID_MAP_resumeAttachmentFileIdComponent);
      }
      return _resumeAttachmentFileIdComponent;
   }

}
// resume CPD analysis - CPD-ON
