package com.example.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import com.opencsv.bean.AbstractBeanField;
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

/**
 * 前後の空白をトリムしてから整数に変換するコンバーター。
 */
class TrimToIntegerConverter extends AbstractBeanField<Integer, String> {
    @Override
    protected Integer convert(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return Integer.parseInt(trimmed);
    }
}

/**
 * 文字列をトリムして大文字に変換するコンバーター。
 */
class UpperCaseConverter extends AbstractBeanField<String, String> {
    @Override
    protected String convert(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}


