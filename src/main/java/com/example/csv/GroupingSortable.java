package com.example.csv;

/**
 * グルーピング＆ソート可能なBeanを表すインターフェース
 * 
 * <p>このインターフェースを実装すると、LargeDataGroupingSorterで
 * グループキーとソート順を自動的に取得できます。</p>
 * 
 * <h3>使用例:</h3>
 * <pre>
 * public class Person implements GroupingSortable&lt;String&gt; {
 *     private String occupation;
 *     private int age;
 *     
 *     {@literal @}Override
 *     public String getGroupKey() {
 *         return occupation;
 *     }
 *     
 *     {@literal @}Override
 *     public int compareTo(GroupingSortable&lt;String&gt; other) {
 *         Person otherPerson = (Person) other;
 *         return Integer.compare(this.age, otherPerson.age);
 *     }
 * }
 * 
 * // グループキーとソート順を自動取得
 * LargeDataGroupingSorter.of(Person.class, inputPath)
 *     .processGroupsSorted((groupKey, stream) -> { ... });
 * </pre>
 * 
 * @param <K> グループキーの型
 */
public interface GroupingSortable<K> extends Groupable<K>, Comparable<GroupingSortable<K>> {
    
    /**
     * 同じグループ内でのソート順を定義
     * 
     * @param other 比較対象
     * @return 比較結果（負：this < other、0：等しい、正：this > other）
     */
    @Override
    int compareTo(GroupingSortable<K> other);
}
