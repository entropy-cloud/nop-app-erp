package app.erp.md.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.md.dao.entity.ErpMdSupplierApproval;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  供应商准入资格: erp_md_supplier_approval
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMdSupplierApproval extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 供应商: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 2;
    
    /* 所属组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 准入类型: APPROVAL_TYPE VARCHAR */
    public static final String PROP_NAME_approvalType = "approvalType";
    public static final int PROP_ID_approvalType = 4;
    
    /* 准入物料类别: MATERIAL_CATEGORY_ID BIGINT */
    public static final String PROP_NAME_materialCategoryId = "materialCategoryId";
    public static final int PROP_ID_materialCategoryId = 5;
    
    /* 生效日期: VALID_FROM DATE */
    public static final String PROP_NAME_validFrom = "validFrom";
    public static final int PROP_ID_validFrom = 6;
    
    /* 失效日期: VALID_TO DATE */
    public static final String PROP_NAME_validTo = "validTo";
    public static final int PROP_ID_validTo = 7;
    
    /* 资质文件: QUALIFICATION_DOC VARCHAR */
    public static final String PROP_NAME_qualificationDoc = "qualificationDoc";
    public static final int PROP_ID_qualificationDoc = 8;
    
    /* 准入状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 9;
    
    /* 批准人: APPROVED_BY VARCHAR */
    public static final String PROP_NAME_approvedBy = "approvedBy";
    public static final int PROP_ID_approvedBy = 10;
    
    /* 批准时间: APPROVED_AT DATETIME */
    public static final String PROP_NAME_approvedAt = "approvedAt";
    public static final int PROP_ID_approvedAt = 11;
    
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
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 18;
    

    private static int _PROP_ID_BOUND = 19;

    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    
    /* relation:  */
    public static final String PROP_NAME_organization = "organization";
    
    /* relation:  */
    public static final String PROP_NAME_materialCategory = "materialCategory";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_approvalType] = PROP_NAME_approvalType;
          PROP_NAME_TO_ID.put(PROP_NAME_approvalType, PROP_ID_approvalType);
      
          PROP_ID_TO_NAME[PROP_ID_materialCategoryId] = PROP_NAME_materialCategoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialCategoryId, PROP_ID_materialCategoryId);
      
          PROP_ID_TO_NAME[PROP_ID_validFrom] = PROP_NAME_validFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_validFrom, PROP_ID_validFrom);
      
          PROP_ID_TO_NAME[PROP_ID_validTo] = PROP_NAME_validTo;
          PROP_NAME_TO_ID.put(PROP_NAME_validTo, PROP_ID_validTo);
      
          PROP_ID_TO_NAME[PROP_ID_qualificationDoc] = PROP_NAME_qualificationDoc;
          PROP_NAME_TO_ID.put(PROP_NAME_qualificationDoc, PROP_ID_qualificationDoc);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_approvedBy] = PROP_NAME_approvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedBy, PROP_ID_approvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_approvedAt] = PROP_NAME_approvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedAt, PROP_ID_approvedAt);
      
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
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 供应商: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 所属组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 准入类型: APPROVAL_TYPE */
    private java.lang.String _approvalType;
    
    /* 准入物料类别: MATERIAL_CATEGORY_ID */
    private java.lang.Long _materialCategoryId;
    
    /* 生效日期: VALID_FROM */
    private java.time.LocalDate _validFrom;
    
    /* 失效日期: VALID_TO */
    private java.time.LocalDate _validTo;
    
    /* 资质文件: QUALIFICATION_DOC */
    private java.lang.String _qualificationDoc;
    
    /* 准入状态: STATUS */
    private java.lang.String _status;
    
    /* 批准人: APPROVED_BY */
    private java.lang.String _approvedBy;
    
    /* 批准时间: APPROVED_AT */
    private java.time.LocalDateTime _approvedAt;
    
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
    

    public _ErpMdSupplierApproval(){
        // for debug
    }

    protected ErpMdSupplierApproval newInstance(){
        ErpMdSupplierApproval entity = new ErpMdSupplierApproval();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMdSupplierApproval cloneInstance() {
        ErpMdSupplierApproval entity = newInstance();
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
      return "app.erp.md.dao.entity.ErpMdSupplierApproval";
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
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_approvalType:
               return getApprovalType();
        
            case PROP_ID_materialCategoryId:
               return getMaterialCategoryId();
        
            case PROP_ID_validFrom:
               return getValidFrom();
        
            case PROP_ID_validTo:
               return getValidTo();
        
            case PROP_ID_qualificationDoc:
               return getQualificationDoc();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_approvedBy:
               return getApprovedBy();
        
            case PROP_ID_approvedAt:
               return getApprovedAt();
        
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
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
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
        
            case PROP_ID_approvalType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approvalType));
               }
               setApprovalType(typedValue);
               break;
            }
        
            case PROP_ID_materialCategoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialCategoryId));
               }
               setMaterialCategoryId(typedValue);
               break;
            }
        
            case PROP_ID_validFrom:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_validFrom));
               }
               setValidFrom(typedValue);
               break;
            }
        
            case PROP_ID_validTo:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_validTo));
               }
               setValidTo(typedValue);
               break;
            }
        
            case PROP_ID_qualificationDoc:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_qualificationDoc));
               }
               setQualificationDoc(typedValue);
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
        
            case PROP_ID_approvedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approvedBy));
               }
               setApprovedBy(typedValue);
               break;
            }
        
            case PROP_ID_approvedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_approvedAt));
               }
               setApprovedAt(typedValue);
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
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_approvalType:{
               onInitProp(propId);
               this._approvalType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_materialCategoryId:{
               onInitProp(propId);
               this._materialCategoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_validFrom:{
               onInitProp(propId);
               this._validFrom = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_validTo:{
               onInitProp(propId);
               this._validTo = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_qualificationDoc:{
               onInitProp(propId);
               this._qualificationDoc = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedBy:{
               onInitProp(propId);
               this._approvedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedAt:{
               onInitProp(propId);
               this._approvedAt = (java.time.LocalDateTime)value;
               
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
     * 供应商: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 供应商: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 所属组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 所属组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 准入类型: APPROVAL_TYPE
     */
    public final java.lang.String getApprovalType(){
         onPropGet(PROP_ID_approvalType);
         return _approvalType;
    }

    /**
     * 准入类型: APPROVAL_TYPE
     */
    public final void setApprovalType(java.lang.String value){
        if(onPropSet(PROP_ID_approvalType,value)){
            this._approvalType = value;
            internalClearRefs(PROP_ID_approvalType);
            
        }
    }
    
    /**
     * 准入物料类别: MATERIAL_CATEGORY_ID
     */
    public final java.lang.Long getMaterialCategoryId(){
         onPropGet(PROP_ID_materialCategoryId);
         return _materialCategoryId;
    }

    /**
     * 准入物料类别: MATERIAL_CATEGORY_ID
     */
    public final void setMaterialCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialCategoryId,value)){
            this._materialCategoryId = value;
            internalClearRefs(PROP_ID_materialCategoryId);
            
        }
    }
    
    /**
     * 生效日期: VALID_FROM
     */
    public final java.time.LocalDate getValidFrom(){
         onPropGet(PROP_ID_validFrom);
         return _validFrom;
    }

    /**
     * 生效日期: VALID_FROM
     */
    public final void setValidFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_validFrom,value)){
            this._validFrom = value;
            internalClearRefs(PROP_ID_validFrom);
            
        }
    }
    
    /**
     * 失效日期: VALID_TO
     */
    public final java.time.LocalDate getValidTo(){
         onPropGet(PROP_ID_validTo);
         return _validTo;
    }

    /**
     * 失效日期: VALID_TO
     */
    public final void setValidTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_validTo,value)){
            this._validTo = value;
            internalClearRefs(PROP_ID_validTo);
            
        }
    }
    
    /**
     * 资质文件: QUALIFICATION_DOC
     */
    public final java.lang.String getQualificationDoc(){
         onPropGet(PROP_ID_qualificationDoc);
         return _qualificationDoc;
    }

    /**
     * 资质文件: QUALIFICATION_DOC
     */
    public final void setQualificationDoc(java.lang.String value){
        if(onPropSet(PROP_ID_qualificationDoc,value)){
            this._qualificationDoc = value;
            internalClearRefs(PROP_ID_qualificationDoc);
            
        }
    }
    
    /**
     * 准入状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 准入状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 批准人: APPROVED_BY
     */
    public final java.lang.String getApprovedBy(){
         onPropGet(PROP_ID_approvedBy);
         return _approvedBy;
    }

    /**
     * 批准人: APPROVED_BY
     */
    public final void setApprovedBy(java.lang.String value){
        if(onPropSet(PROP_ID_approvedBy,value)){
            this._approvedBy = value;
            internalClearRefs(PROP_ID_approvedBy);
            
        }
    }
    
    /**
     * 批准时间: APPROVED_AT
     */
    public final java.time.LocalDateTime getApprovedAt(){
         onPropGet(PROP_ID_approvedAt);
         return _approvedAt;
    }

    /**
     * 批准时间: APPROVED_AT
     */
    public final void setApprovedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_approvedAt,value)){
            this._approvedAt = value;
            internalClearRefs(PROP_ID_approvedAt);
            
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
    public final app.erp.md.dao.entity.ErpMdOrganization getOrganization(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_organization);
    }

    public final void setOrganization(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setOrgId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_organization, refEntity,()->{
           
                           this.setOrgId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterialCategory getMaterialCategory(){
       return (app.erp.md.dao.entity.ErpMdMaterialCategory)internalGetRefEntity(PROP_NAME_materialCategory);
    }

    public final void setMaterialCategory(app.erp.md.dao.entity.ErpMdMaterialCategory refEntity){
   
           if(refEntity == null){
           
                   this.setMaterialCategoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_materialCategory, refEntity,()->{
           
                           this.setMaterialCategoryId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
