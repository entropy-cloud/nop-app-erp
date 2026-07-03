//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgBomInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private Long _productId;

    
        @PropMeta(propId=3)
    
        public Long getProductId(){
            return _productId;
        }

        public void setProductId(Long value){
            this._productId = value;
        }


        private String _bomType;

    
        @PropMeta(propId=4)
    
        public String getBomType(){
            return _bomType;
        }

        public void setBomType(String value){
            this._bomType = value;
        }


        private String _consumption;

    
        @PropMeta(propId=5)
    
        public String getConsumption(){
            return _consumption;
        }

        public void setConsumption(String value){
            this._consumption = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=6)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private Boolean _isDefault;

    
        @PropMeta(propId=7)
    
        public Boolean getIsDefault(){
            return _isDefault;
        }

        public void setIsDefault(Boolean value){
            this._isDefault = value;
        }


        private Boolean _useMultiLevelBom;

    
        @PropMeta(propId=8)
    
        public Boolean getUseMultiLevelBom(){
            return _useMultiLevelBom;
        }

        public void setUseMultiLevelBom(Boolean value){
            this._useMultiLevelBom = value;
        }


        private Boolean _inspectionRequired;

    
        @PropMeta(propId=9)
    
        public Boolean getInspectionRequired(){
            return _inspectionRequired;
        }

        public void setInspectionRequired(Boolean value){
            this._inspectionRequired = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _versionLabel;

    
        @PropMeta(propId=11)
    
        public String getVersionLabel(){
            return _versionLabel;
        }

        public void setVersionLabel(String value){
            this._versionLabel = value;
        }


        private java.math.BigDecimal _qty;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getQty(){
            return _qty;
        }

        public void setQty(java.math.BigDecimal value){
            this._qty = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private List<ErpMfgBomLineInputBean> _lines;

        public List<ErpMfgBomLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpMfgBomLineInputBean> value){
            this._lines = value;
        }


        private List<ErpMfgBomOperationInputBean> _operations;

        public List<ErpMfgBomOperationInputBean> getOperations(){
            return _operations;
        }

        public void setOperations(List<ErpMfgBomOperationInputBean> value){
            this._operations = value;
        }


        private List<ErpMfgBomByproductInputBean> _byproducts;

        public List<ErpMfgBomByproductInputBean> getByproducts(){
            return _byproducts;
        }

        public void setByproducts(List<ErpMfgBomByproductInputBean> value){
            this._byproducts = value;
        }


    }
