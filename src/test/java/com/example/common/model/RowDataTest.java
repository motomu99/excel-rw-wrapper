package com.example.common.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * RowDataクラスのテスト
 */
class RowDataTest {

    @Test
    void testConstructor() {
        String data = "test data";
        RowData<String> rowData = new RowData<>(1, data);
        
        assertEquals(1, rowData.getLineNumber());
        assertEquals(data, rowData.getData());
    }

    @Test
    void testOfFactoryMethod() {
        Integer data = 42;
        RowData<Integer> rowData = RowData.of(5, data);
        
        assertEquals(5, rowData.getLineNumber());
        assertEquals(data, rowData.getData());
    }

    @Test
    void testEquals() {
        String data = "test";
        RowData<String> rowData1 = new RowData<>(1, data);
        RowData<String> rowData2 = new RowData<>(1, data);
        RowData<String> rowData3 = new RowData<>(2, data);
        RowData<String> rowData4 = new RowData<>(1, "different");
        
        assertEquals(rowData1, rowData2);
        assertNotEquals(rowData1, rowData3);
        assertNotEquals(rowData1, rowData4);
        assertNotEquals(rowData1, null);
        assertEquals(rowData1, rowData1); // 同一インスタンス
    }

    @Test
    void testHashCode() {
        String data = "test";
        RowData<String> rowData1 = new RowData<>(1, data);
        RowData<String> rowData2 = new RowData<>(1, data);
        
        assertEquals(rowData1.hashCode(), rowData2.hashCode());
    }

    @Test
    void testToString() {
        String data = "test data";
        RowData<String> rowData = new RowData<>(10, data);
        String toString = rowData.toString();
        
        assertTrue(toString.contains("RowData"));
        assertTrue(toString.contains("lineNumber=10"));
        assertTrue(toString.contains("data=test data"));
    }

    @Test
    void testWithNullData() {
        RowData<String> rowData = new RowData<>(1, null);
        
        assertEquals(1, rowData.getLineNumber());
        assertNull(rowData.getData());
    }

    @Test
    void testEqualsWithNullData() {
        RowData<String> rowData1 = new RowData<>(1, null);
        RowData<String> rowData2 = new RowData<>(1, null);
        RowData<String> rowData3 = new RowData<>(1, "not null");
        
        assertEquals(rowData1, rowData2);
        assertNotEquals(rowData1, rowData3);
    }
}

