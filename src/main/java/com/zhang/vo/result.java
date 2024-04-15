package com.zhang.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)//null不展示
public class result implements Serializable {

    private Object base;
    private Object data;


    public result(Object base) {
        this.base = base;
    }

    public result(Object base, Object data) {
        this.base = base;
        this.data = data;
    }

    public result() {
    }
    public static result OK(Object data) {
        return new result(new base(10000,"success"),data);
    }
    public static result OK() {
        return new result(new base(10000,"success"));
    }
    public static result OK(List items,Long total) {
        return new result(new base(10000,"success"),new data(items,total));
    }
    public static result OK(List items) {
        return new result(new base(10000,"success"),new data(items));
    }
    public static result OK(Map<String,String> Map) {
        return new result(new base(10000,"success"),Map);
    }
    public static result Fail() {
        return new result(new base(-1,"执行失败"));
    }
    public static result Fail(String msg) {
        return new result(new base(-1,msg));
    }
}



