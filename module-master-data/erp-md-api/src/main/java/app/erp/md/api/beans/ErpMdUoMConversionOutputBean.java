//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdUoMConversionOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=2)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _fromUoMId;

    
        @PropMeta(propId=3)
    
        public Long getFromUoMId(){
            return _fromUoMId;
        }

        public void setFromUoMId(Long value){
            this._fromUoMId = value;
        }


        private Long _toUoMId;

    
        @PropMeta(propId=4)
    
        public Long getToUoMId(){
            return _toUoMId;
        }

        public void setToUoMId(Long value){
            this._toUoMId = value;
        }


        private java.math.BigDecimal _conversionRate;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getConversionRate(){
            return _conversionRate;
        }

        public void setConversionRate(java.math.BigDecimal value){
            this._conversionRate = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=6)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=7)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=8)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=10)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _materialName;

    
        public String getMaterialName(){
            return _materialName;
        }

        public void setMaterialName(String value){
            this._materialName = value;
        }


        private String _fromUoMName;

    
        public String getFromUoMName(){
            return _fromUoMName;
        }

        public void setFromUoMName(String value){
            this._fromUoMName = value;
        }


        private String _toUoMName;

    
        public String getToUoMName(){
            return _toUoMName;
        }

        public void setToUoMName(String value){
            this._toUoMName = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _fromUoM;

        public Map<String,Object> getFromUoM(){
            return _fromUoM;
        }

        public void setFromUoM(Map<String,Object> value){
            this._fromUoM = value;
        }


        private Map<String,Object> _toUoM;

        public Map<String,Object> getToUoM(){
            return _toUoM;
        }

        public void setToUoM(Map<String,Object> value){
            this._toUoM = value;
        }


    }
