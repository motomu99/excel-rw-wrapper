package com.example.excel.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Excel読み込み結果を保持するクラス
 * 
 * <p>成功した行のデータとエラー行の情報を保持します。</p>
 * 
 * @param <T> Beanの型
 */
@Getter
public class ExcelReadResult<T> {
    
    /** 成功した行のデータリスト */
    private final List<T> data;
    
    /** エラー行の情報リスト */
    private final List<ExcelReadError> errors;
    
    /**
     * ExcelReadResultを作成
     * 
     * @param data 成功した行のデータリスト
     * @param errors エラー行の情報リスト
     */
    public ExcelReadResult(List<T> data, List<ExcelReadError> errors) {
        this.data = data != null ? Collections.unmodifiableList(new ArrayList<>(data)) : Collections.emptyList();
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
    }
    
    /**
     * エラーが存在するかどうかを判定
     * 
     * @return エラーが存在する場合true
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * 成功した行数を取得
     * 
     * @return 成功した行数
     */
    public int getSuccessCount() {
        return data.size();
    }
    
    /**
     * エラー行数を取得
     * 
     * @return エラー行数
     */
    public int getErrorCount() {
        return errors.size();
    }
}
