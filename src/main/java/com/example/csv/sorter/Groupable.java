package com.example.csv.sorter;

/**
 * グルーピング可能なオブジェクトを表すインターフェース
 * 
 * <p>このインターフェースを実装することで、大量データのグルーピング処理において
 * グループキーを自動的に取得できます。</p>
 * 
 * <h3>使用例:</h3>
 * <pre>
 * &#64;Data
 * public class Person implements Groupable&lt;String&gt; {
 *     private String name;
 *     private String occupation;
 *     
 *     &#64;Override
 *     public String getGroupKey() {
 *         return occupation;  // 職業でグルーピング
 *     }
 * }
 * </pre>
 * 
 * @param <K> グループキーの型（String, Integer, 複合キーなど）
 */
public interface Groupable<K> {
    
    /**
     * このオブジェクトのグループキーを返します
     * 
     * @return グループキー
     */
    K getGroupKey();
}

