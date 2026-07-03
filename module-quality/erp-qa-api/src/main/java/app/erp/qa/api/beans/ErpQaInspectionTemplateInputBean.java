//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaInspectionTemplateInputBean extends CrudInputBase {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private String _inspectionType;

    
        @PropMeta(propId=4)
    
        public String getInspectionType(){
            return _inspectionType;
        }

        public void setInspectionType(String value){
            this._inspectionType = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=5)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Integer _isActive;

    
        @PropMeta(propId=6)
    
        public Integer getIsActive(){
            return _isActive;
        }

        public void setIsActive(Integer value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=7)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=8)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private List<ErpQaInspectionTemplateLineInputBean> _lines;

        public List<ErpQaInspectionTemplateLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpQaInspectionTemplateLineInputBean> value){
            this._lines = value;
        }


    }
