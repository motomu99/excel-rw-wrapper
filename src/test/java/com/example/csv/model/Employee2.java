package com.example.csv.model;

import com.opencsv.bean.CsvBindByPosition;
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
public class Employee2 {

    @CsvBindByPosition(position = 0)
    private Long id;

    @CsvBindByPosition(position = 1)
    private String name;

    @CsvBindByPosition(position = 2)
    private String department;

    @CsvBindByPosition(position = 3)
    private Integer salary;

    @CsvBindByPosition(position = 4)
    @CsvDate("yyyy-MM-dd")
    private LocalDate hireDate;
}