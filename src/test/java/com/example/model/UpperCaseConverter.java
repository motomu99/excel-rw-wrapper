package com.example.model;

import com.opencsv.bean.AbstractBeanField;

/**
 * 文字列をトリムして大文字に変換するコンバーター。
 */
public class UpperCaseConverter extends AbstractBeanField<String, String> {
    @Override
    protected String convert(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
