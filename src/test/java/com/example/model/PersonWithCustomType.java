package com.example.model;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 独自の型とコンバーターを使用するテスト用モデル。
 * ポジションベースマッピングで、ヘッダーありCSVを読み込む。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonWithCustomType {

    @CsvBindByPosition(position = 0)
    private String name;

    @CsvBindByPosition(position = 1)
    private Integer age;

    // 独自の型（Email）をコンバーターで変換
    @CsvCustomBindByPosition(position = 2, converter = EmailConverter.class)
    private Email email;

    @CsvBindByPosition(position = 3)
    private String birthplace;
}
