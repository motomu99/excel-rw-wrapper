package com.example.csv.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日本語サンプル用のPerson Beanクラス
 * アノテーションで項目名を指定してBeanにマッピングできる
 * Lombokでgetter/setter/toString/equals/hashCodeを自動生成
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person2 {

    @CsvBindByPosition(position = 0)
    private String name;

    @CsvBindByPosition(position = 1)
    private Integer age;

    @CsvBindByPosition(position = 2)
    private String occupation;

    @CsvBindByPosition(position = 3)
    private String birthplace;
}