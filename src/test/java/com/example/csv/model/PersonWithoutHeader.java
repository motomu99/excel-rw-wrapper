package com.example.csv.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ヘッダーなしCSV用のPerson Beanクラス
 * 
 * <p>位置ベースマッピング（@CsvBindByPosition）を使用してCSVデータをマッピングします。
 * ヘッダー行がないCSVファイルを読み込む際に使用します。</p>
 * 
 * <p>Lombokでgetter/setter/toString/equals/hashCodeを自動生成します。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonWithoutHeader {

    @CsvBindByPosition(position = 0)
    private String name;

    @CsvBindByPosition(position = 1)
    private Integer age;

    @CsvBindByPosition(position = 2)
    private String occupation;

    @CsvBindByPosition(position = 3)
    private String birthplace;
}

