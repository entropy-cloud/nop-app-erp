package app.erp.cs.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.cs.dao.entity.ErpCsServiceCatalogItem;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  服务目录项: erp_cs_service_catalog_item
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCsServiceCatalogItem extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 4;
    
    /* 目录分类: CATEGORY_ID BIGINT */
    public static final String PROP_NAME_categoryId = "categoryId";
    public static final int PROP_ID_categoryId = 5;
    
    /* 父项: PARENT_ID BIGINT */
    public static final String PROP_NAME_parentId = "parentId";
    public static final int PROP_ID_parentId = 6;
    
    /* 简短描述: SHORT_DESCRIPTION VARCHAR */
    public static final String PROP_NAME_shortDescription = "shortDescription";
    public static final int PROP_ID_shortDescription = 7;
    
    /* 详细说明: FULL_DESCRIPTION VARCHAR */
    public static final String PROP_NAME_fullDescription = "fullDescription";
    public static final int PROP_ID_fullDescription = 8;
    
    /* 关联工单类型: TICKET_TYPE_ID BIGINT */
    public static final String PROP_NAME_ticketTypeId = "ticketTypeId";
    public static final int PROP_ID_ticketTypeId = 9;
    
    /* 默认SLA策略: SLA_POLICY_ID BIGINT */
    public static final String PROP_NAME_slaPolicyId = "slaPolicyId";
    public static final int PROP_ID_slaPolicyId = 10;
    
    /* 履行流程标识: FULFILLMENT_PROCESS_ID VARCHAR */
    public static final String PROP_NAME_fulfillmentProcessId = "fulfillmentProcessId";
    public static final int PROP_ID_fulfillmentProcessId = 11;
    
    /* 请求表单配置: REQUEST_FORM_CONFIG VARCHAR */
    public static final String PROP_NAME_requestFormConfig = "requestFormConfig";
    public static final int PROP_ID_requestFormConfig = 12;
    
    /* 是否上架: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 13;
    
    /* 是否客户可见: IS_PUBLIC BOOLEAN */
    public static final String PROP_NAME_isPublic = "isPublic";
    public static final int PROP_ID_isPublic = 14;
    
    /* 排序: SEQUENCE INTEGER */
    public static final String PROP_NAME_sequence = "sequence";
    public static final int PROP_ID_sequence = 15;
    
    /* 预计解决时间: ESTIMATED_RESOLUTION VARCHAR */
    public static final String PROP_NAME_estimatedResolution = "estimatedResolution";
    public static final int PROP_ID_estimatedResolution = 16;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 17;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 18;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 19;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 20;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 21;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 22;
    

    private static int _PROP_ID_BOUND = 23;

    
    /* relation:  */
    public static final String PROP_NAME_category = "category";
    
    /* relation:  */
    public static final String PROP_NAME_parent = "parent";
    
    /* relation:  */
    public static final String PROP_NAME_ticketType = "ticketType";
    
    /* relation:  */
    public static final String PROP_NAME_slaPolicy = "slaPolicy";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_categoryId] = PROP_NAME_categoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_categoryId, PROP_ID_categoryId);
      
          PROP_ID_TO_NAME[PROP_ID_parentId] = PROP_NAME_parentId;
          PROP_NAME_TO_ID.put(PROP_NAME_parentId, PROP_ID_parentId);
      
          PROP_ID_TO_NAME[PROP_ID_shortDescription] = PROP_NAME_shortDescription;
          PROP_NAME_TO_ID.put(PROP_NAME_shortDescription, PROP_ID_shortDescription);
      
          PROP_ID_TO_NAME[PROP_ID_fullDescription] = PROP_NAME_fullDescription;
          PROP_NAME_TO_ID.put(PROP_NAME_fullDescription, PROP_ID_fullDescription);
      
          PROP_ID_TO_NAME[PROP_ID_ticketTypeId] = PROP_NAME_ticketTypeId;
          PROP_NAME_TO_ID.put(PROP_NAME_ticketTypeId, PROP_ID_ticketTypeId);
      
          PROP_ID_TO_NAME[PROP_ID_slaPolicyId] = PROP_NAME_slaPolicyId;
          PROP_NAME_TO_ID.put(PROP_NAME_slaPolicyId, PROP_ID_slaPolicyId);
      
          PROP_ID_TO_NAME[PROP_ID_fulfillmentProcessId] = PROP_NAME_fulfillmentProcessId;
          PROP_NAME_TO_ID.put(PROP_NAME_fulfillmentProcessId, PROP_ID_fulfillmentProcessId);
      
          PROP_ID_TO_NAME[PROP_ID_requestFormConfig] = PROP_NAME_requestFormConfig;
          PROP_NAME_TO_ID.put(PROP_NAME_requestFormConfig, PROP_ID_requestFormConfig);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
          PROP_ID_TO_NAME[PROP_ID_isPublic] = PROP_NAME_isPublic;
          PROP_NAME_TO_ID.put(PROP_NAME_isPublic, PROP_ID_isPublic);
      
          PROP_ID_TO_NAME[PROP_ID_sequence] = PROP_NAME_sequence;
          PROP_NAME_TO_ID.put(PROP_NAME_sequence, PROP_ID_sequence);
      
          PROP_ID_TO_NAME[PROP_ID_estimatedResolution] = PROP_NAME_estimatedResolution;
          PROP_NAME_TO_ID.put(PROP_NAME_estimatedResolution, PROP_ID_estimatedResolution);
      
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
    
    /* 编码: CODE */
    private java.lang.String _code;
    
    /* 名称: NAME */
    private java.lang.String _name;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 目录分类: CATEGORY_ID */
    private java.lang.Long _categoryId;
    
    /* 父项: PARENT_ID */
    private java.lang.Long _parentId;
    
    /* 简短描述: SHORT_DESCRIPTION */
    private java.lang.String _shortDescription;
    
    /* 详细说明: FULL_DESCRIPTION */
    private java.lang.String _fullDescription;
    
    /* 关联工单类型: TICKET_TYPE_ID */
    private java.lang.Long _ticketTypeId;
    
    /* 默认SLA策略: SLA_POLICY_ID */
    private java.lang.Long _slaPolicyId;
    
    /* 履行流程标识: FULFILLMENT_PROCESS_ID */
    private java.lang.String _fulfillmentProcessId;
    
    /* 请求表单配置: REQUEST_FORM_CONFIG */
    private java.lang.String _requestFormConfig;
    
    /* 是否上架: IS_ACTIVE */
    private java.lang.Boolean _isActive;
    
    /* 是否客户可见: IS_PUBLIC */
    private java.lang.Boolean _isPublic;
    
    /* 排序: SEQUENCE */
    private java.lang.Integer _sequence;
    
    /* 预计解决时间: ESTIMATED_RESOLUTION */
    private java.lang.String _estimatedResolution;
    
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
    

    public _ErpCsServiceCatalogItem(){
        // for debug
    }

    protected ErpCsServiceCatalogItem newInstance(){
        ErpCsServiceCatalogItem entity = new ErpCsServiceCatalogItem();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCsServiceCatalogItem cloneInstance() {
        ErpCsServiceCatalogItem entity = newInstance();
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
      return "app.erp.cs.dao.entity.ErpCsServiceCatalogItem";
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
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_categoryId:
               return getCategoryId();
        
            case PROP_ID_parentId:
               return getParentId();
        
            case PROP_ID_shortDescription:
               return getShortDescription();
        
            case PROP_ID_fullDescription:
               return getFullDescription();
        
            case PROP_ID_ticketTypeId:
               return getTicketTypeId();
        
            case PROP_ID_slaPolicyId:
               return getSlaPolicyId();
        
            case PROP_ID_fulfillmentProcessId:
               return getFulfillmentProcessId();
        
            case PROP_ID_requestFormConfig:
               return getRequestFormConfig();
        
            case PROP_ID_isActive:
               return getIsActive();
        
            case PROP_ID_isPublic:
               return getIsPublic();
        
            case PROP_ID_sequence:
               return getSequence();
        
            case PROP_ID_estimatedResolution:
               return getEstimatedResolution();
        
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
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
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
        
            case PROP_ID_categoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_categoryId));
               }
               setCategoryId(typedValue);
               break;
            }
        
            case PROP_ID_parentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_parentId));
               }
               setParentId(typedValue);
               break;
            }
        
            case PROP_ID_shortDescription:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_shortDescription));
               }
               setShortDescription(typedValue);
               break;
            }
        
            case PROP_ID_fullDescription:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fullDescription));
               }
               setFullDescription(typedValue);
               break;
            }
        
            case PROP_ID_ticketTypeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_ticketTypeId));
               }
               setTicketTypeId(typedValue);
               break;
            }
        
            case PROP_ID_slaPolicyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_slaPolicyId));
               }
               setSlaPolicyId(typedValue);
               break;
            }
        
            case PROP_ID_fulfillmentProcessId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fulfillmentProcessId));
               }
               setFulfillmentProcessId(typedValue);
               break;
            }
        
            case PROP_ID_requestFormConfig:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_requestFormConfig));
               }
               setRequestFormConfig(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isActive));
               }
               setIsActive(typedValue);
               break;
            }
        
            case PROP_ID_isPublic:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isPublic));
               }
               setIsPublic(typedValue);
               break;
            }
        
            case PROP_ID_sequence:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sequence));
               }
               setSequence(typedValue);
               break;
            }
        
            case PROP_ID_estimatedResolution:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_estimatedResolution));
               }
               setEstimatedResolution(typedValue);
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
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_categoryId:{
               onInitProp(propId);
               this._categoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_parentId:{
               onInitProp(propId);
               this._parentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_shortDescription:{
               onInitProp(propId);
               this._shortDescription = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fullDescription:{
               onInitProp(propId);
               this._fullDescription = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ticketTypeId:{
               onInitProp(propId);
               this._ticketTypeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_slaPolicyId:{
               onInitProp(propId);
               this._slaPolicyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fulfillmentProcessId:{
               onInitProp(propId);
               this._fulfillmentProcessId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_requestFormConfig:{
               onInitProp(propId);
               this._requestFormConfig = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isPublic:{
               onInitProp(propId);
               this._isPublic = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_sequence:{
               onInitProp(propId);
               this._sequence = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_estimatedResolution:{
               onInitProp(propId);
               this._estimatedResolution = (java.lang.String)value;
               
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
     * 编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
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
     * 目录分类: CATEGORY_ID
     */
    public final java.lang.Long getCategoryId(){
         onPropGet(PROP_ID_categoryId);
         return _categoryId;
    }

    /**
     * 目录分类: CATEGORY_ID
     */
    public final void setCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_categoryId,value)){
            this._categoryId = value;
            internalClearRefs(PROP_ID_categoryId);
            
        }
    }
    
    /**
     * 父项: PARENT_ID
     */
    public final java.lang.Long getParentId(){
         onPropGet(PROP_ID_parentId);
         return _parentId;
    }

    /**
     * 父项: PARENT_ID
     */
    public final void setParentId(java.lang.Long value){
        if(onPropSet(PROP_ID_parentId,value)){
            this._parentId = value;
            internalClearRefs(PROP_ID_parentId);
            
        }
    }
    
    /**
     * 简短描述: SHORT_DESCRIPTION
     */
    public final java.lang.String getShortDescription(){
         onPropGet(PROP_ID_shortDescription);
         return _shortDescription;
    }

    /**
     * 简短描述: SHORT_DESCRIPTION
     */
    public final void setShortDescription(java.lang.String value){
        if(onPropSet(PROP_ID_shortDescription,value)){
            this._shortDescription = value;
            internalClearRefs(PROP_ID_shortDescription);
            
        }
    }
    
    /**
     * 详细说明: FULL_DESCRIPTION
     */
    public final java.lang.String getFullDescription(){
         onPropGet(PROP_ID_fullDescription);
         return _fullDescription;
    }

    /**
     * 详细说明: FULL_DESCRIPTION
     */
    public final void setFullDescription(java.lang.String value){
        if(onPropSet(PROP_ID_fullDescription,value)){
            this._fullDescription = value;
            internalClearRefs(PROP_ID_fullDescription);
            
        }
    }
    
    /**
     * 关联工单类型: TICKET_TYPE_ID
     */
    public final java.lang.Long getTicketTypeId(){
         onPropGet(PROP_ID_ticketTypeId);
         return _ticketTypeId;
    }

    /**
     * 关联工单类型: TICKET_TYPE_ID
     */
    public final void setTicketTypeId(java.lang.Long value){
        if(onPropSet(PROP_ID_ticketTypeId,value)){
            this._ticketTypeId = value;
            internalClearRefs(PROP_ID_ticketTypeId);
            
        }
    }
    
    /**
     * 默认SLA策略: SLA_POLICY_ID
     */
    public final java.lang.Long getSlaPolicyId(){
         onPropGet(PROP_ID_slaPolicyId);
         return _slaPolicyId;
    }

    /**
     * 默认SLA策略: SLA_POLICY_ID
     */
    public final void setSlaPolicyId(java.lang.Long value){
        if(onPropSet(PROP_ID_slaPolicyId,value)){
            this._slaPolicyId = value;
            internalClearRefs(PROP_ID_slaPolicyId);
            
        }
    }
    
    /**
     * 履行流程标识: FULFILLMENT_PROCESS_ID
     */
    public final java.lang.String getFulfillmentProcessId(){
         onPropGet(PROP_ID_fulfillmentProcessId);
         return _fulfillmentProcessId;
    }

    /**
     * 履行流程标识: FULFILLMENT_PROCESS_ID
     */
    public final void setFulfillmentProcessId(java.lang.String value){
        if(onPropSet(PROP_ID_fulfillmentProcessId,value)){
            this._fulfillmentProcessId = value;
            internalClearRefs(PROP_ID_fulfillmentProcessId);
            
        }
    }
    
    /**
     * 请求表单配置: REQUEST_FORM_CONFIG
     */
    public final java.lang.String getRequestFormConfig(){
         onPropGet(PROP_ID_requestFormConfig);
         return _requestFormConfig;
    }

    /**
     * 请求表单配置: REQUEST_FORM_CONFIG
     */
    public final void setRequestFormConfig(java.lang.String value){
        if(onPropSet(PROP_ID_requestFormConfig,value)){
            this._requestFormConfig = value;
            internalClearRefs(PROP_ID_requestFormConfig);
            
        }
    }
    
    /**
     * 是否上架: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否上架: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
        }
    }
    
    /**
     * 是否客户可见: IS_PUBLIC
     */
    public final java.lang.Boolean getIsPublic(){
         onPropGet(PROP_ID_isPublic);
         return _isPublic;
    }

    /**
     * 是否客户可见: IS_PUBLIC
     */
    public final void setIsPublic(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isPublic,value)){
            this._isPublic = value;
            internalClearRefs(PROP_ID_isPublic);
            
        }
    }
    
    /**
     * 排序: SEQUENCE
     */
    public final java.lang.Integer getSequence(){
         onPropGet(PROP_ID_sequence);
         return _sequence;
    }

    /**
     * 排序: SEQUENCE
     */
    public final void setSequence(java.lang.Integer value){
        if(onPropSet(PROP_ID_sequence,value)){
            this._sequence = value;
            internalClearRefs(PROP_ID_sequence);
            
        }
    }
    
    /**
     * 预计解决时间: ESTIMATED_RESOLUTION
     */
    public final java.lang.String getEstimatedResolution(){
         onPropGet(PROP_ID_estimatedResolution);
         return _estimatedResolution;
    }

    /**
     * 预计解决时间: ESTIMATED_RESOLUTION
     */
    public final void setEstimatedResolution(java.lang.String value){
        if(onPropSet(PROP_ID_estimatedResolution,value)){
            this._estimatedResolution = value;
            internalClearRefs(PROP_ID_estimatedResolution);
            
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
    public final app.erp.cs.dao.entity.ErpCsCatalogCategory getCategory(){
       return (app.erp.cs.dao.entity.ErpCsCatalogCategory)internalGetRefEntity(PROP_NAME_category);
    }

    public final void setCategory(app.erp.cs.dao.entity.ErpCsCatalogCategory refEntity){
   
           if(refEntity == null){
           
                   this.setCategoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_category, refEntity,()->{
           
                           this.setCategoryId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.cs.dao.entity.ErpCsServiceCatalogItem getParent(){
       return (app.erp.cs.dao.entity.ErpCsServiceCatalogItem)internalGetRefEntity(PROP_NAME_parent);
    }

    public final void setParent(app.erp.cs.dao.entity.ErpCsServiceCatalogItem refEntity){
   
           if(refEntity == null){
           
                   this.setParentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_parent, refEntity,()->{
           
                           this.setParentId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.cs.dao.entity.ErpCsTicketType getTicketType(){
       return (app.erp.cs.dao.entity.ErpCsTicketType)internalGetRefEntity(PROP_NAME_ticketType);
    }

    public final void setTicketType(app.erp.cs.dao.entity.ErpCsTicketType refEntity){
   
           if(refEntity == null){
           
                   this.setTicketTypeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_ticketType, refEntity,()->{
           
                           this.setTicketTypeId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.cs.dao.entity.ErpCsSlaPolicy getSlaPolicy(){
       return (app.erp.cs.dao.entity.ErpCsSlaPolicy)internalGetRefEntity(PROP_NAME_slaPolicy);
    }

    public final void setSlaPolicy(app.erp.cs.dao.entity.ErpCsSlaPolicy refEntity){
   
           if(refEntity == null){
           
                   this.setSlaPolicyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_slaPolicy, refEntity,()->{
           
                           this.setSlaPolicyId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
