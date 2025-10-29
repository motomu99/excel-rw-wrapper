package com.example.csv;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.MappingStrategy;

/**
 * OpenCSVのマッピング戦略を生成するファクトリークラス
 * 
 * <p>ヘッダーベースマッピングと位置ベースマッピングの戦略を生成します。
 * 各クラスで重複していたマッピング戦略生成コードを共通化します。</p>
 */
public class MappingStrategyFactory {

    private MappingStrategyFactory() {
        // ユーティリティクラスのためインスタンス化を禁止
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * マッピング戦略を生成
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング対象のBeanクラス
     * @param usePositionMapping 位置ベースマッピングを使用する場合true
     * @return マッピング戦略
     */
    public static <T> MappingStrategy<T> createStrategy(Class<T> beanClass, boolean usePositionMapping) {
        if (usePositionMapping) {
            return createPositionMappingStrategy(beanClass);
        } else {
            return createHeaderMappingStrategy(beanClass);
        }
    }

    /**
     * ヘッダーベースマッピング戦略を生成
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング対象のBeanクラス
     * @return ヘッダーベースマッピング戦略
     */
    public static <T> MappingStrategy<T> createHeaderMappingStrategy(Class<T> beanClass) {
        HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType((Class<? extends T>) beanClass);
        return strategy;
    }

    /**
     * 位置ベースマッピング戦略を生成
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング対象のBeanクラス
     * @return 位置ベースマッピング戦略
     */
    public static <T> MappingStrategy<T> createPositionMappingStrategy(Class<T> beanClass) {
        ColumnPositionMappingStrategy<T> strategy = new ColumnPositionMappingStrategy<>();
        strategy.setType((Class<? extends T>) beanClass);
        return strategy;
    }
}

