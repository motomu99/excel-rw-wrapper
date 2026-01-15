package com.example.model;

import com.opencsv.bean.AbstractBeanField;

/**
 * 文字列をEmail型に変換するコンバーター。
 */
public class EmailConverter extends AbstractBeanField<Email, String> {
    @Override
    protected Email convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // 簡単なバリデーション（@が含まれているかチェック）
        String trimmed = value.trim();
        if (trimmed.contains("@")) {
            return new Email(trimmed.toLowerCase());
        }
        // @が含まれていない場合は、ドメインを追加
        return new Email(trimmed.toLowerCase() + "@example.com");
    }
}
