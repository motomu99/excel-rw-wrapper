package com.example.csv;

/**
 * グルーピング可能なBeanを表すインターフェース
 * 
 * <p>このインターフェースを実装すると、LargeDataGroupingSorterで
 * グループキーを自動的に取得できます。</p>
 * 
 * <h3>使用例:</h3>
 * <pre>
 * public class Person implements Groupable&lt;String&gt; {
 *     private String occupation;
 *     
 *     {@literal @}Override
 *     public String getGroupKey() {
 *         return occupation;
 *     }
 * }
 * 
 * // グループキーを自動取得
 * LargeDataGroupingSorter.of(Person.class, inputPath)
 *     .sortBy(Comparator.comparingInt(Person::getAge))
 *     .processGroups((groupKey, stream) -> { ... });
 * </pre>
 * 
 * @param <K> グループキーの型
 */
public interface Groupable<K> {
    
    /**
     * グループキーを取得
     * 
     * @return グループキー
     */
    K getGroupKey();
}
