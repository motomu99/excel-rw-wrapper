package com.example.model.linenumber;

import com.example.common.annotation.LineNumber;
import com.opencsv.bean.CsvBindByName;
import lombok.Data;

/**
 * @LineNumberアノテーションのみを使用したテスト用モデル
 */
@Data
public class PersonWithLineNumberAnnotation {

    @LineNumber
    private Integer lineNumber;

    @CsvBindByName(column = "名前")
    private String name;

    @CsvBindByName(column = "年齢")
    private Integer age;

    @CsvBindByName(column = "職業")
    private String occupation;
}
