package com.example.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全ての型をテストするためのモデル
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonWithAllTypes {
    
    @CsvBindByName(column = "名前")
    private String name;
    
    @CsvBindByName(column = "年齢")
    private Integer age;
    
    @CsvBindByName(column = "ID")
    private Long id;
    
    @CsvBindByName(column = "給料")
    private Double salary;
    
    @CsvBindByName(column = "有効")
    private Boolean active;
}

