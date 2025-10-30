package com.example.csv.sorter;

/**
 * グルーピング＆ソート可能なオブジェクトを表すインターフェース
 * 
 * <p>このインターフェースを実装することで、大量データのグルーピング＆ソート処理において
 * グループキーとソート順を自動的に適用できます。</p>
 * 
 * <h3>使用例:</h3>
 * <pre>
 * &#64;Data
 * public class Person implements GroupingSortable&lt;String&gt; {
 *     private String name;
 *     private Integer age;
 *     private String occupation;
 *     
 *     &#64;Override
 *     public String getGroupKey() {
 *         return occupation;  // 職業でグルーピング
 *     }
 *     
 *     &#64;Override
 *     public int compareTo(GroupingSortable&lt;String&gt; other) {
 *         Person p = (Person) other;
 *         return Integer.compare(this.age, p.age);  // 年齢でソート
 *     }
 * }
 * </pre>
 * 
 * @param <K> グループキーの型（String, Integer, 複合キーなど）
 */
public interface GroupingSortable<K> extends Groupable<K>, Comparable<GroupingSortable<K>> {
    
    /**
     * このオブジェクトのグループキーを返します
     * 
     * @return グループキー
     */
    @Override
    K getGroupKey();
    
    /**
     * グループ内でのソート順を定義します
     * 
     * @param other 比較対象のオブジェクト
     * @return 負の値（this &lt; other）、0（等しい）、正の値（this &gt; other）
     */
    @Override
    int compareTo(GroupingSortable<K> other);
}

