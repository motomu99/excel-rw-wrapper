package com.example.exception;

/**
 * セルの値を指定された型に変換できない場合にスローされる例外
 */
public class CellValueConversionException extends ExcelReaderException {

    private final int rowIndex;
    private final String columnName;
    private final String cellValue;
    private final String targetType;

    /**
     * 詳細な情報を指定して例外を構築
     *
     * @param rowIndex 行番号（0から始まる）
     * @param columnName 列名
     * @param cellValue セルの値
     * @param targetType 変換しようとした型
     * @param cause 原因となった例外
     */
    public CellValueConversionException(int rowIndex, String columnName, String cellValue, Class<?> targetType, Throwable cause) {
        super(String.format("セル値の変換に失敗しました: 行=%d, 列='%s', 値='%s', 変換先の型=%s, エラー=%s",
                rowIndex + 1, // 1から始まる行番号で表示
                columnName,
                cellValue,
                targetType.getSimpleName(),
                cause.getMessage()),
                cause);
        this.rowIndex = rowIndex;
        this.columnName = columnName;
        this.cellValue = cellValue;
        this.targetType = targetType.getSimpleName();
    }

    /**
     * 行番号を取得（0から始まる）
     *
     * @return 行番号
     */
    public int getRowIndex() {
        return rowIndex;
    }

    /**
     * 列名を取得
     *
     * @return 列名
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * セルの値を取得
     *
     * @return セルの値
     */
    public String getCellValue() {
        return cellValue;
    }

    /**
     * 変換しようとした型を取得
     *
     * @return 型名
     */
    public String getTargetType() {
        return targetType;
    }
}
