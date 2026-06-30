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

import app.erp.hr.dao.entity.ErpHrShiftSwapRequest;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  排班调换申请: erp_hr_shift_swap_request
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrShiftSwapRequest extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 申请人: REQUESTER_ID BIGINT */
    public static final String PROP_NAME_requesterId = "requesterId";
    public static final int PROP_ID_requesterId = 4;
    
    /* 目标员工: TARGET_EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_targetEmployeeId = "targetEmployeeId";
    public static final int PROP_ID_targetEmployeeId = 5;
    
    /* 原排班: SOURCE_ASSIGNMENT_ID BIGINT */
    public static final String PROP_NAME_sourceAssignmentId = "sourceAssignmentId";
    public static final int PROP_ID_sourceAssignmentId = 6;
    
    /* 目标排班: TARGET_ASSIGNMENT_ID BIGINT */
    public static final String PROP_NAME_targetAssignmentId = "targetAssignmentId";
    public static final int PROP_ID_targetAssignmentId = 7;
    
    /* 调换日期: SWAP_DATE DATE */
    public static final String PROP_NAME_swapDate = "swapDate";
    public static final int PROP_ID_swapDate = 8;
    
    /* 调换原因: REASON VARCHAR */
    public static final String PROP_NAME_reason = "reason";
    public static final int PROP_ID_reason = 9;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 10;
    
    /* 审批人: APPROVED_BY_ID BIGINT */
    public static final String PROP_NAME_approvedById = "approvedById";
    public static final int PROP_ID_approvedById = 11;
    
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
    public static final String PROP_NAME_requester = "requester";
    
    /* relation:  */
    public static final String PROP_NAME_targetEmployee = "targetEmployee";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_requesterId] = PROP_NAME_requesterId;
          PROP_NAME_TO_ID.put(PROP_NAME_requesterId, PROP_ID_requesterId);
      
          PROP_ID_TO_NAME[PROP_ID_targetEmployeeId] = PROP_NAME_targetEmployeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_targetEmployeeId, PROP_ID_targetEmployeeId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceAssignmentId] = PROP_NAME_sourceAssignmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceAssignmentId, PROP_ID_sourceAssignmentId);
      
          PROP_ID_TO_NAME[PROP_ID_targetAssignmentId] = PROP_NAME_targetAssignmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_targetAssignmentId, PROP_ID_targetAssignmentId);
      
          PROP_ID_TO_NAME[PROP_ID_swapDate] = PROP_NAME_swapDate;
          PROP_NAME_TO_ID.put(PROP_NAME_swapDate, PROP_ID_swapDate);
      
          PROP_ID_TO_NAME[PROP_ID_reason] = PROP_NAME_reason;
          PROP_NAME_TO_ID.put(PROP_NAME_reason, PROP_ID_reason);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_approvedById] = PROP_NAME_approvedById;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedById, PROP_ID_approvedById);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 申请人: REQUESTER_ID */
    private java.lang.Long _requesterId;
    
    /* 目标员工: TARGET_EMPLOYEE_ID */
    private java.lang.Long _targetEmployeeId;
    
    /* 原排班: SOURCE_ASSIGNMENT_ID */
    private java.lang.Long _sourceAssignmentId;
    
    /* 目标排班: TARGET_ASSIGNMENT_ID */
    private java.lang.Long _targetAssignmentId;
    
    /* 调换日期: SWAP_DATE */
    private java.time.LocalDate _swapDate;
    
    /* 调换原因: REASON */
    private java.lang.String _reason;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 审批人: APPROVED_BY_ID */
    private java.lang.Long _approvedById;
    
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
    

    public _ErpHrShiftSwapRequest(){
        // for debug
    }

    protected ErpHrShiftSwapRequest newInstance(){
        ErpHrShiftSwapRequest entity = new ErpHrShiftSwapRequest();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrShiftSwapRequest cloneInstance() {
        ErpHrShiftSwapRequest entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrShiftSwapRequest";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_requesterId:
               return getRequesterId();
        
            case PROP_ID_targetEmployeeId:
               return getTargetEmployeeId();
        
            case PROP_ID_sourceAssignmentId:
               return getSourceAssignmentId();
        
            case PROP_ID_targetAssignmentId:
               return getTargetAssignmentId();
        
            case PROP_ID_swapDate:
               return getSwapDate();
        
            case PROP_ID_reason:
               return getReason();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_approvedById:
               return getApprovedById();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_requesterId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_requesterId));
               }
               setRequesterId(typedValue);
               break;
            }
        
            case PROP_ID_targetEmployeeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_targetEmployeeId));
               }
               setTargetEmployeeId(typedValue);
               break;
            }
        
            case PROP_ID_sourceAssignmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceAssignmentId));
               }
               setSourceAssignmentId(typedValue);
               break;
            }
        
            case PROP_ID_targetAssignmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_targetAssignmentId));
               }
               setTargetAssignmentId(typedValue);
               break;
            }
        
            case PROP_ID_swapDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_swapDate));
               }
               setSwapDate(typedValue);
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
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_approvedById:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_approvedById));
               }
               setApprovedById(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_requesterId:{
               onInitProp(propId);
               this._requesterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_targetEmployeeId:{
               onInitProp(propId);
               this._targetEmployeeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceAssignmentId:{
               onInitProp(propId);
               this._sourceAssignmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_targetAssignmentId:{
               onInitProp(propId);
               this._targetAssignmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_swapDate:{
               onInitProp(propId);
               this._swapDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_reason:{
               onInitProp(propId);
               this._reason = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_approvedById:{
               onInitProp(propId);
               this._approvedById = (java.lang.Long)value;
               
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
     * 申请人: REQUESTER_ID
     */
    public final java.lang.Long getRequesterId(){
         onPropGet(PROP_ID_requesterId);
         return _requesterId;
    }

    /**
     * 申请人: REQUESTER_ID
     */
    public final void setRequesterId(java.lang.Long value){
        if(onPropSet(PROP_ID_requesterId,value)){
            this._requesterId = value;
            internalClearRefs(PROP_ID_requesterId);
            
        }
    }
    
    /**
     * 目标员工: TARGET_EMPLOYEE_ID
     */
    public final java.lang.Long getTargetEmployeeId(){
         onPropGet(PROP_ID_targetEmployeeId);
         return _targetEmployeeId;
    }

    /**
     * 目标员工: TARGET_EMPLOYEE_ID
     */
    public final void setTargetEmployeeId(java.lang.Long value){
        if(onPropSet(PROP_ID_targetEmployeeId,value)){
            this._targetEmployeeId = value;
            internalClearRefs(PROP_ID_targetEmployeeId);
            
        }
    }
    
    /**
     * 原排班: SOURCE_ASSIGNMENT_ID
     */
    public final java.lang.Long getSourceAssignmentId(){
         onPropGet(PROP_ID_sourceAssignmentId);
         return _sourceAssignmentId;
    }

    /**
     * 原排班: SOURCE_ASSIGNMENT_ID
     */
    public final void setSourceAssignmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceAssignmentId,value)){
            this._sourceAssignmentId = value;
            internalClearRefs(PROP_ID_sourceAssignmentId);
            
        }
    }
    
    /**
     * 目标排班: TARGET_ASSIGNMENT_ID
     */
    public final java.lang.Long getTargetAssignmentId(){
         onPropGet(PROP_ID_targetAssignmentId);
         return _targetAssignmentId;
    }

    /**
     * 目标排班: TARGET_ASSIGNMENT_ID
     */
    public final void setTargetAssignmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_targetAssignmentId,value)){
            this._targetAssignmentId = value;
            internalClearRefs(PROP_ID_targetAssignmentId);
            
        }
    }
    
    /**
     * 调换日期: SWAP_DATE
     */
    public final java.time.LocalDate getSwapDate(){
         onPropGet(PROP_ID_swapDate);
         return _swapDate;
    }

    /**
     * 调换日期: SWAP_DATE
     */
    public final void setSwapDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_swapDate,value)){
            this._swapDate = value;
            internalClearRefs(PROP_ID_swapDate);
            
        }
    }
    
    /**
     * 调换原因: REASON
     */
    public final java.lang.String getReason(){
         onPropGet(PROP_ID_reason);
         return _reason;
    }

    /**
     * 调换原因: REASON
     */
    public final void setReason(java.lang.String value){
        if(onPropSet(PROP_ID_reason,value)){
            this._reason = value;
            internalClearRefs(PROP_ID_reason);
            
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
     * 审批人: APPROVED_BY_ID
     */
    public final java.lang.Long getApprovedById(){
         onPropGet(PROP_ID_approvedById);
         return _approvedById;
    }

    /**
     * 审批人: APPROVED_BY_ID
     */
    public final void setApprovedById(java.lang.Long value){
        if(onPropSet(PROP_ID_approvedById,value)){
            this._approvedById = value;
            internalClearRefs(PROP_ID_approvedById);
            
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
    public final app.erp.hr.dao.entity.ErpHrEmployee getRequester(){
       return (app.erp.hr.dao.entity.ErpHrEmployee)internalGetRefEntity(PROP_NAME_requester);
    }

    public final void setRequester(app.erp.hr.dao.entity.ErpHrEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setRequesterId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_requester, refEntity,()->{
           
                           this.setRequesterId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrEmployee getTargetEmployee(){
       return (app.erp.hr.dao.entity.ErpHrEmployee)internalGetRefEntity(PROP_NAME_targetEmployee);
    }

    public final void setTargetEmployee(app.erp.hr.dao.entity.ErpHrEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setTargetEmployeeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_targetEmployee, refEntity,()->{
           
                           this.setTargetEmployeeId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
