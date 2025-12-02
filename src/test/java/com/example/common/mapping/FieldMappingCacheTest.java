package com.example.common.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.mapping.FieldMappingCache.FieldMappingInfo;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

/**
 * FieldMappingCacheのテストクラス
 */
@DisplayName("FieldMappingCache テスト")
class FieldMappingCacheTest {

    /**
     * テスト用Bean: 名前ベースマッピング
     */
    static class PersonWithName {
        @CsvBindByName(column = "名前")
        private String name;
        
        @CsvBindByName(column = "年齢")
        private Integer age;
        
        @CsvBindByName(column = "メールアドレス")
        private String email;
        
        // アノテーションなしフィールド（キャッシュされない）
        private String ignored;
    }

    /**
     * テスト用Bean: 位置ベースマッピング
     */
    static class PersonWithPosition {
        @CsvBindByPosition(position = 0)
        private String name;
        
        @CsvBindByPosition(position = 1)
        private Integer age;
        
        @CsvBindByPosition(position = 2)
        private String email;
    }

    /**
     * テスト用Bean: 両方のマッピング
     */
    static class PersonWithBoth {
        @CsvBindByName(column = "名前")
        @CsvBindByPosition(position = 0)
        private String name;
        
        @CsvBindByName(column = "年齢")
        @CsvBindByPosition(position = 1)
        private Integer age;
    }

    /**
     * テスト用Bean: アノテーションなし
     */
    static class PersonWithoutAnnotations {
        private String name;
        private Integer age;
    }

    @Test
    @DisplayName("名前ベースマッピングのキャッシュ構築")
    void testBuildCache_WithName() throws NoSuchFieldException {
        FieldMappingCache cache = new FieldMappingCache(PersonWithName.class);
        Map<Field, FieldMappingInfo> cacheMap = cache.getCache();
        
        // アノテーション付きフィールドのみキャッシュされる
        assertEquals(3, cacheMap.size());
        
        // nameフィールドの確認
        Field nameField = PersonWithName.class.getDeclaredField("name");
        assertTrue(cacheMap.containsKey(nameField));
        FieldMappingInfo nameInfo = cacheMap.get(nameField);
        assertEquals("名前", nameInfo.columnName);
        assertNull(nameInfo.position);
        
        // ageフィールドの確認
        Field ageField = PersonWithName.class.getDeclaredField("age");
        assertTrue(cacheMap.containsKey(ageField));
        FieldMappingInfo ageInfo = cacheMap.get(ageField);
        assertEquals("年齢", ageInfo.columnName);
        assertNull(ageInfo.position);
        
        // emailフィールドの確認
        Field emailField = PersonWithName.class.getDeclaredField("email");
        assertTrue(cacheMap.containsKey(emailField));
        FieldMappingInfo emailInfo = cacheMap.get(emailField);
        assertEquals("メールアドレス", emailInfo.columnName);
        assertNull(emailInfo.position);
        
        // ignoredフィールドはキャッシュされない
        Field ignoredField = PersonWithName.class.getDeclaredField("ignored");
        assertFalse(cacheMap.containsKey(ignoredField));
    }

    @Test
    @DisplayName("位置ベースマッピングのキャッシュ構築")
    void testBuildCache_WithPosition() throws NoSuchFieldException {
        FieldMappingCache cache = new FieldMappingCache(PersonWithPosition.class);
        Map<Field, FieldMappingInfo> cacheMap = cache.getCache();
        
        assertEquals(3, cacheMap.size());
        
        // nameフィールドの確認
        Field nameField = PersonWithPosition.class.getDeclaredField("name");
        assertTrue(cacheMap.containsKey(nameField));
        FieldMappingInfo nameInfo = cacheMap.get(nameField);
        assertNull(nameInfo.columnName);
        assertEquals(0, nameInfo.position);
        
        // ageフィールドの確認
        Field ageField = PersonWithPosition.class.getDeclaredField("age");
        assertTrue(cacheMap.containsKey(ageField));
        FieldMappingInfo ageInfo = cacheMap.get(ageField);
        assertNull(ageInfo.columnName);
        assertEquals(1, ageInfo.position);
        
        // emailフィールドの確認
        Field emailField = PersonWithPosition.class.getDeclaredField("email");
        assertTrue(cacheMap.containsKey(emailField));
        FieldMappingInfo emailInfo = cacheMap.get(emailField);
        assertNull(emailInfo.columnName);
        assertEquals(2, emailInfo.position);
    }

    @Test
    @DisplayName("名前と位置の両方が設定されたマッピング")
    void testBuildCache_WithBoth() throws NoSuchFieldException {
        FieldMappingCache cache = new FieldMappingCache(PersonWithBoth.class);
        Map<Field, FieldMappingInfo> cacheMap = cache.getCache();
        
        assertEquals(2, cacheMap.size());
        
        // nameフィールドの確認（両方の情報が保持される）
        Field nameField = PersonWithBoth.class.getDeclaredField("name");
        assertTrue(cacheMap.containsKey(nameField));
        FieldMappingInfo nameInfo = cacheMap.get(nameField);
        assertEquals("名前", nameInfo.columnName);
        assertEquals(0, nameInfo.position);
        
        // ageフィールドの確認
        Field ageField = PersonWithBoth.class.getDeclaredField("age");
        assertTrue(cacheMap.containsKey(ageField));
        FieldMappingInfo ageInfo = cacheMap.get(ageField);
        assertEquals("年齢", ageInfo.columnName);
        assertEquals(1, ageInfo.position);
    }

    @Test
    @DisplayName("アノテーションなしのBeanは空のキャッシュ")
    void testBuildCache_WithoutAnnotations() {
        FieldMappingCache cache = new FieldMappingCache(PersonWithoutAnnotations.class);
        Map<Field, FieldMappingInfo> cacheMap = cache.getCache();
        
        assertEquals(0, cacheMap.size());
    }

    @Test
    @DisplayName("フィールドの順序が保持される（LinkedHashMap）")
    void testBuildCache_OrderPreserved() {
        FieldMappingCache cache = new FieldMappingCache(PersonWithName.class);
        Map<Field, FieldMappingInfo> cacheMap = cache.getCache();
        
        // LinkedHashMapは挿入順序を保持
        assertInstanceOf(java.util.LinkedHashMap.class, cacheMap);
        
        // フィールドが宣言順に格納されていることを確認
        Field[] fields = cacheMap.keySet().toArray(new Field[0]);
        assertEquals("name", fields[0].getName());
        assertEquals("age", fields[1].getName());
        assertEquals("email", fields[2].getName());
    }

    @Test
    @DisplayName("FieldMappingInfoのフィールドが正しく初期化される")
    void testFieldMappingInfo_Initialization() throws NoSuchFieldException {
        Field testField = PersonWithName.class.getDeclaredField("name");
        FieldMappingInfo info = new FieldMappingInfo(testField, "テスト列", 5, null, null);
        
        assertEquals(testField, info.field);
        assertEquals("テスト列", info.columnName);
        assertEquals(5, info.position);
    }

    @Test
    @DisplayName("FieldMappingInfoのpositionがnullでも正常動作")
    void testFieldMappingInfo_NullPosition() throws NoSuchFieldException {
        Field testField = PersonWithName.class.getDeclaredField("name");
        FieldMappingInfo info = new FieldMappingInfo(testField, "テスト列", null, null, null);
        
        assertEquals("テスト列", info.columnName);
        assertNull(info.position);
    }

    @Test
    @DisplayName("FieldMappingInfoのcolumnNameがnullでも正常動作")
    void testFieldMappingInfo_NullColumnName() throws NoSuchFieldException {
        Field testField = PersonWithPosition.class.getDeclaredField("name");
        FieldMappingInfo info = new FieldMappingInfo(testField, null, 0, null, null);
        
        assertNull(info.columnName);
        assertEquals(0, info.position);
    }

    @Test
    @DisplayName("複数のBeanクラスで独立したキャッシュが作成される")
    void testMultipleCaches_Independent() {
        FieldMappingCache cache1 = new FieldMappingCache(PersonWithName.class);
        FieldMappingCache cache2 = new FieldMappingCache(PersonWithPosition.class);
        
        Map<Field, FieldMappingInfo> cacheMap1 = cache1.getCache();
        Map<Field, FieldMappingInfo> cacheMap2 = cache2.getCache();
        
        // 両方とも3つのフィールド
        assertEquals(3, cacheMap1.size());
        assertEquals(3, cacheMap2.size());
        
        // しかしキャッシュの内容は異なる
        assertNotEquals(cacheMap1, cacheMap2);
        
        // cache1は名前ベース、cache2は位置ベース
        FieldMappingInfo info1 = cacheMap1.values().iterator().next();
        FieldMappingInfo info2 = cacheMap2.values().iterator().next();
        
        assertNotNull(info1.columnName);
        assertNull(info1.position);
        
        assertNull(info2.columnName);
        assertNotNull(info2.position);
    }

    @Test
    @DisplayName("privateフィールドがアクセス可能になる")
    void testFieldAccessibility() throws NoSuchFieldException, IllegalAccessException {
        FieldMappingCache cache = new FieldMappingCache(PersonWithName.class);
        Map<Field, FieldMappingInfo> cacheMap = cache.getCache();
        
        Field nameField = PersonWithName.class.getDeclaredField("name");
        FieldMappingInfo info = cacheMap.get(nameField);
                
        // 実際にアクセスできることを確認
        PersonWithName person = new PersonWithName();
        assertDoesNotThrow(() -> info.field.set(person, "テスト太郎"));
        assertEquals("テスト太郎", info.field.get(person));
    }
}

