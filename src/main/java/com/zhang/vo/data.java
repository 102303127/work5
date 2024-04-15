package com.zhang.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;


import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class data {
    private List items;
    private Long total;

    public data() {
    }
    public data(List items, Long total) {
        this.items = items;
        this.total = total;
    }

    public data(List items) {
        this.items = items;
    }

}
