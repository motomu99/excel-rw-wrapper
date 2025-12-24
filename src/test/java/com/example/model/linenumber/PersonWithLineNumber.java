package com.example.model.linenumber;

import com.example.common.model.LineNumberAware;
import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * LineNumberAware抽象クラスを継承したテスト用モデル
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PersonWithLineNumber extends LineNumberAware {

    @CsvBindByName(column = "名前")
    private String name;

    @CsvBindByName(column = "年齢")
    private Integer age;

    @CsvBindByName(column = "職業")
    private String occupation;
}
