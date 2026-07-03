package app.erp.ast.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.ast.dao.entity.ErpAstMovement;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  资产移动: erp_ast_movement
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpAstMovement extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 资产ID: ASSET_ID BIGINT */
    public static final String PROP_NAME_assetId = "assetId";
    public static final int PROP_ID_assetId = 4;
    
    /* 移动日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 5;
    
    /* 开始日期: FROM_DATE DATE */
    public static final String PROP_NAME_fromDate = "fromDate";
    public static final int PROP_ID_fromDate = 6;
    
    /* 截止日期: THRU_DATE DATE */
    public static final String PROP_NAME_thruDate = "thruDate";
    public static final int PROP_ID_thruDate = 7;
    
    /* 原使用部门: FROM_DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_fromDepartmentId = "fromDepartmentId";
    public static final int PROP_ID_fromDepartmentId = 8;
    
    /* 新使用部门: TO_DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_toDepartmentId = "toDepartmentId";
    public static final int PROP_ID_toDepartmentId = 9;
    
    /* 原使用人: FROM_STAFF_ID BIGINT */
    public static final String PROP_NAME_fromStaffId = "fromStaffId";
    public static final int PROP_ID_fromStaffId = 10;
    
    /* 新使用人: TO_STAFF_ID BIGINT */
    public static final String PROP_NAME_toStaffId = "toStaffId";
    public static final int PROP_ID_toStaffId = 11;
    
    /* 原使用地点: FROM_LOCATION_ID BIGINT */
    public static final String PROP_NAME_fromLocationId = "fromLocationId";
    public static final int PROP_ID_fromLocationId = 12;
    
    /* 新使用地点: TO_LOCATION_ID BIGINT */
    public static final String PROP_NAME_toLocationId = "toLocationId";
    public static final int PROP_ID_toLocationId = 13;
    
    /* 经办人: HANDLER_ID BIGINT */
    public static final String PROP_NAME_handlerId = "handlerId";
    public static final int PROP_ID_handlerId = 14;
    
    /* 单据状态: DOC_STATUS VARCHAR */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 15;
    
    /* 审核状态: APPROVE_STATUS VARCHAR */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 16;
    
    /* 已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 17;
    
    /* 过账时间: POSTED_AT DATETIME */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 18;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 19;
    
    /* 单据版本: DOC_VERSION VARCHAR */
    public static final String PROP_NAME_docVersion = "docVersion";
    public static final int PROP_ID_docVersion = 20;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 21;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 22;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 23;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 24;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 25;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 26;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 27;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 28;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 29;
    
    /* 源币种金额: AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_amountSource = "amountSource";
    public static final int PROP_ID_amountSource = 30;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_amountFunctional = "amountFunctional";
    public static final int PROP_ID_amountFunctional = 31;
    

    private static int _PROP_ID_BOUND = 32;

    
    /* relation:  */
    public static final String PROP_NAME_asset = "asset";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_fromDepartment = "fromDepartment";
    
    /* relation:  */
    public static final String PROP_NAME_toDepartment = "toDepartment";
    
    /* relation:  */
    public static final String PROP_NAME_fromStaff = "fromStaff";
    
    /* relation:  */
    public static final String PROP_NAME_toStaff = "toStaff";
    
    /* relation:  */
    public static final String PROP_NAME_fromLocation = "fromLocation";
    
    /* relation:  */
    public static final String PROP_NAME_toLocation = "toLocation";
    
    /* relation:  */
    public static final String PROP_NAME_handler = "handler";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[32];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_assetId] = PROP_NAME_assetId;
          PROP_NAME_TO_ID.put(PROP_NAME_assetId, PROP_ID_assetId);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_fromDate] = PROP_NAME_fromDate;
          PROP_NAME_TO_ID.put(PROP_NAME_fromDate, PROP_ID_fromDate);
      
          PROP_ID_TO_NAME[PROP_ID_thruDate] = PROP_NAME_thruDate;
          PROP_NAME_TO_ID.put(PROP_NAME_thruDate, PROP_ID_thruDate);
      
          PROP_ID_TO_NAME[PROP_ID_fromDepartmentId] = PROP_NAME_fromDepartmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_fromDepartmentId, PROP_ID_fromDepartmentId);
      
          PROP_ID_TO_NAME[PROP_ID_toDepartmentId] = PROP_NAME_toDepartmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_toDepartmentId, PROP_ID_toDepartmentId);
      
          PROP_ID_TO_NAME[PROP_ID_fromStaffId] = PROP_NAME_fromStaffId;
          PROP_NAME_TO_ID.put(PROP_NAME_fromStaffId, PROP_ID_fromStaffId);
      
          PROP_ID_TO_NAME[PROP_ID_toStaffId] = PROP_NAME_toStaffId;
          PROP_NAME_TO_ID.put(PROP_NAME_toStaffId, PROP_ID_toStaffId);
      
          PROP_ID_TO_NAME[PROP_ID_fromLocationId] = PROP_NAME_fromLocationId;
          PROP_NAME_TO_ID.put(PROP_NAME_fromLocationId, PROP_ID_fromLocationId);
      
          PROP_ID_TO_NAME[PROP_ID_toLocationId] = PROP_NAME_toLocationId;
          PROP_NAME_TO_ID.put(PROP_NAME_toLocationId, PROP_ID_toLocationId);
      
          PROP_ID_TO_NAME[PROP_ID_handlerId] = PROP_NAME_handlerId;
          PROP_NAME_TO_ID.put(PROP_NAME_handlerId, PROP_ID_handlerId);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approveStatus] = PROP_NAME_approveStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approveStatus, PROP_ID_approveStatus);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
          PROP_ID_TO_NAME[PROP_ID_docVersion] = PROP_NAME_docVersion;
          PROP_NAME_TO_ID.put(PROP_NAME_docVersion, PROP_ID_docVersion);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 资产ID: ASSET_ID */
    private java.lang.Long _assetId;
    
    /* 移动日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 开始日期: FROM_DATE */
    private java.time.LocalDate _fromDate;
    
    /* 截止日期: THRU_DATE */
    private java.time.LocalDate _thruDate;
    
    /* 原使用部门: FROM_DEPARTMENT_ID */
    private java.lang.Long _fromDepartmentId;
    
    /* 新使用部门: TO_DEPARTMENT_ID */
    private java.lang.Long _toDepartmentId;
    
    /* 原使用人: FROM_STAFF_ID */
    private java.lang.Long _fromStaffId;
    
    /* 新使用人: TO_STAFF_ID */
    private java.lang.Long _toStaffId;
    
    /* 原使用地点: FROM_LOCATION_ID */
    private java.lang.Long _fromLocationId;
    
    /* 新使用地点: TO_LOCATION_ID */
    private java.lang.Long _toLocationId;
    
    /* 经办人: HANDLER_ID */
    private java.lang.Long _handlerId;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.String _docStatus;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.String _approveStatus;
    
    /* 已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账时间: POSTED_AT */
    private java.time.LocalDateTime _postedAt;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
    /* 单据版本: DOC_VERSION */
    private java.lang.String _docVersion;
    
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
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.math.BigDecimal _exchangeRate;
    
    /* 源币种金额: AMOUNT_SOURCE */
    private java.math.BigDecimal _amountSource;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL */
    private java.math.BigDecimal _amountFunctional;
    

    public _ErpAstMovement(){
        // for debug
    }

    protected ErpAstMovement newInstance(){
        ErpAstMovement entity = new ErpAstMovement();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpAstMovement cloneInstance() {
        ErpAstMovement entity = newInstance();
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
      return "app.erp.ast.dao.entity.ErpAstMovement";
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
        
            case PROP_ID_assetId:
               return getAssetId();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_fromDate:
               return getFromDate();
        
            case PROP_ID_thruDate:
               return getThruDate();
        
            case PROP_ID_fromDepartmentId:
               return getFromDepartmentId();
        
            case PROP_ID_toDepartmentId:
               return getToDepartmentId();
        
            case PROP_ID_fromStaffId:
               return getFromStaffId();
        
            case PROP_ID_toStaffId:
               return getToStaffId();
        
            case PROP_ID_fromLocationId:
               return getFromLocationId();
        
            case PROP_ID_toLocationId:
               return getToLocationId();
        
            case PROP_ID_handlerId:
               return getHandlerId();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_approveStatus:
               return getApproveStatus();
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
            case PROP_ID_docVersion:
               return getDocVersion();
        
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
        
            case PROP_ID_remark:
               return getRemark();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
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
        
            case PROP_ID_assetId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_assetId));
               }
               setAssetId(typedValue);
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
        
            case PROP_ID_fromDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_fromDate));
               }
               setFromDate(typedValue);
               break;
            }
        
            case PROP_ID_thruDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_thruDate));
               }
               setThruDate(typedValue);
               break;
            }
        
            case PROP_ID_fromDepartmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fromDepartmentId));
               }
               setFromDepartmentId(typedValue);
               break;
            }
        
            case PROP_ID_toDepartmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_toDepartmentId));
               }
               setToDepartmentId(typedValue);
               break;
            }
        
            case PROP_ID_fromStaffId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fromStaffId));
               }
               setFromStaffId(typedValue);
               break;
            }
        
            case PROP_ID_toStaffId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_toStaffId));
               }
               setToStaffId(typedValue);
               break;
            }
        
            case PROP_ID_fromLocationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fromLocationId));
               }
               setFromLocationId(typedValue);
               break;
            }
        
            case PROP_ID_toLocationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_toLocationId));
               }
               setToLocationId(typedValue);
               break;
            }
        
            case PROP_ID_handlerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_handlerId));
               }
               setHandlerId(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
               break;
            }
        
            case PROP_ID_approveStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approveStatus));
               }
               setApproveStatus(typedValue);
               break;
            }
        
            case PROP_ID_posted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_posted));
               }
               setPosted(typedValue);
               break;
            }
        
            case PROP_ID_postedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_postedAt));
               }
               setPostedAt(typedValue);
               break;
            }
        
            case PROP_ID_postedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_postedBy));
               }
               setPostedBy(typedValue);
               break;
            }
        
            case PROP_ID_docVersion:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docVersion));
               }
               setDocVersion(typedValue);
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
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
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
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_exchangeRate));
               }
               setExchangeRate(typedValue);
               break;
            }
        
            case PROP_ID_amountSource:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountSource));
               }
               setAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_amountFunctional:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountFunctional));
               }
               setAmountFunctional(typedValue);
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
        
            case PROP_ID_assetId:{
               onInitProp(propId);
               this._assetId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_fromDate:{
               onInitProp(propId);
               this._fromDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_thruDate:{
               onInitProp(propId);
               this._thruDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_fromDepartmentId:{
               onInitProp(propId);
               this._fromDepartmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_toDepartmentId:{
               onInitProp(propId);
               this._toDepartmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fromStaffId:{
               onInitProp(propId);
               this._fromStaffId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_toStaffId:{
               onInitProp(propId);
               this._toStaffId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fromLocationId:{
               onInitProp(propId);
               this._fromLocationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_toLocationId:{
               onInitProp(propId);
               this._toLocationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_handlerId:{
               onInitProp(propId);
               this._handlerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approveStatus:{
               onInitProp(propId);
               this._approveStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_docVersion:{
               onInitProp(propId);
               this._docVersion = (java.lang.String)value;
               
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
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_exchangeRate:{
               onInitProp(propId);
               this._exchangeRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amountSource:{
               onInitProp(propId);
               this._amountSource = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amountFunctional:{
               onInitProp(propId);
               this._amountFunctional = (java.math.BigDecimal)value;
               
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
     * 单号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 单号: CODE
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
     * 资产ID: ASSET_ID
     */
    public final java.lang.Long getAssetId(){
         onPropGet(PROP_ID_assetId);
         return _assetId;
    }

    /**
     * 资产ID: ASSET_ID
     */
    public final void setAssetId(java.lang.Long value){
        if(onPropSet(PROP_ID_assetId,value)){
            this._assetId = value;
            internalClearRefs(PROP_ID_assetId);
            
        }
    }
    
    /**
     * 移动日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 移动日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
        }
    }
    
    /**
     * 开始日期: FROM_DATE
     */
    public final java.time.LocalDate getFromDate(){
         onPropGet(PROP_ID_fromDate);
         return _fromDate;
    }

    /**
     * 开始日期: FROM_DATE
     */
    public final void setFromDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_fromDate,value)){
            this._fromDate = value;
            internalClearRefs(PROP_ID_fromDate);
            
        }
    }
    
    /**
     * 截止日期: THRU_DATE
     */
    public final java.time.LocalDate getThruDate(){
         onPropGet(PROP_ID_thruDate);
         return _thruDate;
    }

    /**
     * 截止日期: THRU_DATE
     */
    public final void setThruDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_thruDate,value)){
            this._thruDate = value;
            internalClearRefs(PROP_ID_thruDate);
            
        }
    }
    
    /**
     * 原使用部门: FROM_DEPARTMENT_ID
     */
    public final java.lang.Long getFromDepartmentId(){
         onPropGet(PROP_ID_fromDepartmentId);
         return _fromDepartmentId;
    }

    /**
     * 原使用部门: FROM_DEPARTMENT_ID
     */
    public final void setFromDepartmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_fromDepartmentId,value)){
            this._fromDepartmentId = value;
            internalClearRefs(PROP_ID_fromDepartmentId);
            
        }
    }
    
    /**
     * 新使用部门: TO_DEPARTMENT_ID
     */
    public final java.lang.Long getToDepartmentId(){
         onPropGet(PROP_ID_toDepartmentId);
         return _toDepartmentId;
    }

    /**
     * 新使用部门: TO_DEPARTMENT_ID
     */
    public final void setToDepartmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_toDepartmentId,value)){
            this._toDepartmentId = value;
            internalClearRefs(PROP_ID_toDepartmentId);
            
        }
    }
    
    /**
     * 原使用人: FROM_STAFF_ID
     */
    public final java.lang.Long getFromStaffId(){
         onPropGet(PROP_ID_fromStaffId);
         return _fromStaffId;
    }

    /**
     * 原使用人: FROM_STAFF_ID
     */
    public final void setFromStaffId(java.lang.Long value){
        if(onPropSet(PROP_ID_fromStaffId,value)){
            this._fromStaffId = value;
            internalClearRefs(PROP_ID_fromStaffId);
            
        }
    }
    
    /**
     * 新使用人: TO_STAFF_ID
     */
    public final java.lang.Long getToStaffId(){
         onPropGet(PROP_ID_toStaffId);
         return _toStaffId;
    }

    /**
     * 新使用人: TO_STAFF_ID
     */
    public final void setToStaffId(java.lang.Long value){
        if(onPropSet(PROP_ID_toStaffId,value)){
            this._toStaffId = value;
            internalClearRefs(PROP_ID_toStaffId);
            
        }
    }
    
    /**
     * 原使用地点: FROM_LOCATION_ID
     */
    public final java.lang.Long getFromLocationId(){
         onPropGet(PROP_ID_fromLocationId);
         return _fromLocationId;
    }

    /**
     * 原使用地点: FROM_LOCATION_ID
     */
    public final void setFromLocationId(java.lang.Long value){
        if(onPropSet(PROP_ID_fromLocationId,value)){
            this._fromLocationId = value;
            internalClearRefs(PROP_ID_fromLocationId);
            
        }
    }
    
    /**
     * 新使用地点: TO_LOCATION_ID
     */
    public final java.lang.Long getToLocationId(){
         onPropGet(PROP_ID_toLocationId);
         return _toLocationId;
    }

    /**
     * 新使用地点: TO_LOCATION_ID
     */
    public final void setToLocationId(java.lang.Long value){
        if(onPropSet(PROP_ID_toLocationId,value)){
            this._toLocationId = value;
            internalClearRefs(PROP_ID_toLocationId);
            
        }
    }
    
    /**
     * 经办人: HANDLER_ID
     */
    public final java.lang.Long getHandlerId(){
         onPropGet(PROP_ID_handlerId);
         return _handlerId;
    }

    /**
     * 经办人: HANDLER_ID
     */
    public final void setHandlerId(java.lang.Long value){
        if(onPropSet(PROP_ID_handlerId,value)){
            this._handlerId = value;
            internalClearRefs(PROP_ID_handlerId);
            
        }
    }
    
    /**
     * 单据状态: DOC_STATUS
     */
    public final java.lang.String getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 单据状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.String value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
        }
    }
    
    /**
     * 审核状态: APPROVE_STATUS
     */
    public final java.lang.String getApproveStatus(){
         onPropGet(PROP_ID_approveStatus);
         return _approveStatus;
    }

    /**
     * 审核状态: APPROVE_STATUS
     */
    public final void setApproveStatus(java.lang.String value){
        if(onPropSet(PROP_ID_approveStatus,value)){
            this._approveStatus = value;
            internalClearRefs(PROP_ID_approveStatus);
            
        }
    }
    
    /**
     * 已过账: POSTED
     */
    public final java.lang.Boolean getPosted(){
         onPropGet(PROP_ID_posted);
         return _posted;
    }

    /**
     * 已过账: POSTED
     */
    public final void setPosted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_posted,value)){
            this._posted = value;
            internalClearRefs(PROP_ID_posted);
            
        }
    }
    
    /**
     * 过账时间: POSTED_AT
     */
    public final java.time.LocalDateTime getPostedAt(){
         onPropGet(PROP_ID_postedAt);
         return _postedAt;
    }

    /**
     * 过账时间: POSTED_AT
     */
    public final void setPostedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_postedAt,value)){
            this._postedAt = value;
            internalClearRefs(PROP_ID_postedAt);
            
        }
    }
    
    /**
     * 过账人: POSTED_BY
     */
    public final java.lang.String getPostedBy(){
         onPropGet(PROP_ID_postedBy);
         return _postedBy;
    }

    /**
     * 过账人: POSTED_BY
     */
    public final void setPostedBy(java.lang.String value){
        if(onPropSet(PROP_ID_postedBy,value)){
            this._postedBy = value;
            internalClearRefs(PROP_ID_postedBy);
            
        }
    }
    
    /**
     * 单据版本: DOC_VERSION
     */
    public final java.lang.String getDocVersion(){
         onPropGet(PROP_ID_docVersion);
         return _docVersion;
    }

    /**
     * 单据版本: DOC_VERSION
     */
    public final void setDocVersion(java.lang.String value){
        if(onPropSet(PROP_ID_docVersion,value)){
            this._docVersion = value;
            internalClearRefs(PROP_ID_docVersion);
            
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
    public final java.math.BigDecimal getExchangeRate(){
         onPropGet(PROP_ID_exchangeRate);
         return _exchangeRate;
    }

    /**
     * 汇率: EXCHANGE_RATE
     */
    public final void setExchangeRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_exchangeRate,value)){
            this._exchangeRate = value;
            internalClearRefs(PROP_ID_exchangeRate);
            
        }
    }
    
    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final java.math.BigDecimal getAmountSource(){
         onPropGet(PROP_ID_amountSource);
         return _amountSource;
    }

    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final void setAmountSource(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountSource,value)){
            this._amountSource = value;
            internalClearRefs(PROP_ID_amountSource);
            
        }
    }
    
    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final java.math.BigDecimal getAmountFunctional(){
         onPropGet(PROP_ID_amountFunctional);
         return _amountFunctional;
    }

    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final void setAmountFunctional(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountFunctional,value)){
            this._amountFunctional = value;
            internalClearRefs(PROP_ID_amountFunctional);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstAsset getAsset(){
       return (app.erp.ast.dao.entity.ErpAstAsset)internalGetRefEntity(PROP_NAME_asset);
    }

    public final void setAsset(app.erp.ast.dao.entity.ErpAstAsset refEntity){
   
           if(refEntity == null){
           
                   this.setAssetId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_asset, refEntity,()->{
           
                           this.setAssetId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdOrganization getFromDepartment(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_fromDepartment);
    }

    public final void setFromDepartment(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setFromDepartmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_fromDepartment, refEntity,()->{
           
                           this.setFromDepartmentId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdOrganization getToDepartment(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_toDepartment);
    }

    public final void setToDepartment(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setToDepartmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_toDepartment, refEntity,()->{
           
                           this.setToDepartmentId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdEmployee getFromStaff(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_fromStaff);
    }

    public final void setFromStaff(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setFromStaffId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_fromStaff, refEntity,()->{
           
                           this.setFromStaffId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdEmployee getToStaff(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_toStaff);
    }

    public final void setToStaff(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setToStaffId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_toStaff, refEntity,()->{
           
                           this.setToStaffId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdLocation getFromLocation(){
       return (app.erp.md.dao.entity.ErpMdLocation)internalGetRefEntity(PROP_NAME_fromLocation);
    }

    public final void setFromLocation(app.erp.md.dao.entity.ErpMdLocation refEntity){
   
           if(refEntity == null){
           
                   this.setFromLocationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_fromLocation, refEntity,()->{
           
                           this.setFromLocationId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdLocation getToLocation(){
       return (app.erp.md.dao.entity.ErpMdLocation)internalGetRefEntity(PROP_NAME_toLocation);
    }

    public final void setToLocation(app.erp.md.dao.entity.ErpMdLocation refEntity){
   
           if(refEntity == null){
           
                   this.setToLocationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_toLocation, refEntity,()->{
           
                           this.setToLocationId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdEmployee getHandler(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_handler);
    }

    public final void setHandler(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setHandlerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_handler, refEntity,()->{
           
                           this.setHandlerId(refEntity.getId());
                       
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
       
}
// resume CPD analysis - CPD-ON
