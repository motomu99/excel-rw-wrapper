package com.example.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * カスタムコンバーター（@CsvCustomBindByName / @CsvCustomBindByPosition）の
 * Excel読み込み動作を検証するためのモデル。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonWithCustomConverter {

    @CsvBindByName(column = "名前")
    @CsvBindByPosition(position = 0)
    private String name;

    // ヘッダー名ベースのカスタム変換（前処理）
    @CsvCustomBindByName(column = "年齢", converter = TrimToIntegerConverter.class)
    private Integer age;

    // 位置ベースのカスタム変換（前処理）
    @CsvCustomBindByPosition(position = 2, converter = UpperCaseConverter.class)
    private String occupation;
}


