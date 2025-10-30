package com.example.model;

import com.opencsv.bean.CsvBindByName;
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
public class Person {

    @CsvBindByName(column = "名前")
    private String name;

    @CsvBindByName(column = "年齢")
    private Integer age;

    @CsvBindByName(column = "職業")
    private String occupation;

    @CsvBindByName(column = "出身地")
    private String birthplace;
}