package com.example.model;

import com.opencsv.bean.AbstractBeanField;

/**
 * 前後の空白をトリムしてから整数に変換するコンバーター。
 */
public class TrimToIntegerConverter extends AbstractBeanField<Integer, String> {
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
