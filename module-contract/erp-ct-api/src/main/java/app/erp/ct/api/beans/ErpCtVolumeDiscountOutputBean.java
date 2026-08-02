//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtVolumeDiscountOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _contractLineId;

    
        @PropMeta(propId=2)
    
        public Long getContractLineId(){
            return _contractLineId;
        }

        public void setContractLineId(Long value){
            this._contractLineId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.math.BigDecimal _fromQty;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getFromQty(){
            return _fromQty;
        }

        public void setFromQty(java.math.BigDecimal value){
            this._fromQty = value;
        }


        private java.math.BigDecimal _toQty;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getToQty(){
            return _toQty;
        }

        public void setToQty(java.math.BigDecimal value){
            this._toQty = value;
        }


        private java.math.BigDecimal _discountPercent;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getDiscountPercent(){
            return _discountPercent;
        }

        public void setDiscountPercent(java.math.BigDecimal value){
            this._discountPercent = value;
        }


        private java.math.BigDecimal _unitPrice;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getUnitPrice(){
            return _unitPrice;
        }

        public void setUnitPrice(java.math.BigDecimal value){
            this._unitPrice = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=10)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=11)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=13)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _contractLine;

        public Map<String,Object> getContractLine(){
            return _contractLine;
        }

        public void setContractLine(Map<String,Object> value){
            this._contractLine = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
