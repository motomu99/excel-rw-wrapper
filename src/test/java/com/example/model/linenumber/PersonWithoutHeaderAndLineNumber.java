package com.example.model.linenumber;

import com.example.common.model.LineNumberAware;
import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ヘッダーなしCSV用のテストモデル（行番号対応）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PersonWithoutHeaderAndLineNumber extends LineNumberAware {

    @CsvBindByPosition(position = 0)
    private String name;

    @CsvBindByPosition(position = 1)
    private Integer age;

    @CsvBindByPosition(position = 2)
    private String occupation;
}
