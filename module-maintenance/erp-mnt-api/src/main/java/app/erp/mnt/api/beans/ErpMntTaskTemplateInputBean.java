//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntTaskTemplateInputBean extends CrudInputBase {

    
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


        private Long _equipmentCategoryId;

    
        @PropMeta(propId=4)
    
        public Long getEquipmentCategoryId(){
            return _equipmentCategoryId;
        }

        public void setEquipmentCategoryId(Long value){
            this._equipmentCategoryId = value;
        }


        private java.math.BigDecimal _standardMinutes;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getStandardMinutes(){
            return _standardMinutes;
        }

        public void setStandardMinutes(java.math.BigDecimal value){
            this._standardMinutes = value;
        }


        private String _instruction;

    
        @PropMeta(propId=6)
    
        public String getInstruction(){
            return _instruction;
        }

        public void setInstruction(String value){
            this._instruction = value;
        }


        private Integer _isActive;

    
        @PropMeta(propId=7)
    
        public Integer getIsActive(){
            return _isActive;
        }

        public void setIsActive(Integer value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpMntTaskTemplateLineInputBean> _lines;

        public List<ErpMntTaskTemplateLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpMntTaskTemplateLineInputBean> value){
            this._lines = value;
        }


    }
