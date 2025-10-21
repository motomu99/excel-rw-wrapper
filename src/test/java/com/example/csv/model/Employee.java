package com.example.csv.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 英語サンプル用のEmployee Beanクラス
 * アノテーションで項目名を指定してBeanにマッピングできる
 * Lombokでgetter/setter/toString/equals/hashCodeを自動生成
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @CsvBindByName(column = "id")
    private Long id;

    @CsvBindByName(column = "name")
    private String name;

    @CsvBindByName(column = "department")
    private String department;

    @CsvBindByName(column = "salary")
    private Integer salary;

    @CsvBindByName(column = "hire_date")
    @CsvDate("yyyy-MM-dd")
    private LocalDate hireDate;
}