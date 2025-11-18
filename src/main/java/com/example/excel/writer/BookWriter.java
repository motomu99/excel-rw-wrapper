package com.example.excel.writer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.example.common.mapping.FieldMappingCache;
import com.example.common.mapping.FieldMappingCache.FieldMappingInfo;
import com.example.excel.domain.Anchor;
import com.example.excel.domain.Book;
import com.example.excel.domain.Table;

import lombok.extern.slf4j.Slf4j;

/**
 * BookドメインモデルをExcelファイルに書き込むサービス
 * 
 * <p>DDD的な設計に基づき、ドメインモデル（Book/Sheet/Table）をExcelファイルに永続化します。
 * このクラスはインフラストラクチャ層に属し、ドメインモデルとPOIライブラリを橋渡しします。</p>
 * 
 * <p>使用例:
 * <pre>
 * // ドメインモデルを構築
 * Book book = Book.of(Paths.get("output.xlsx"))
 *     .addSheet(Sheet.of("Report")
 *         .addTable(Table.builder(Person.class)
 *             .anchor("A1")
 *             .data(users)
 *             .build())
 *         .addTable(Table.builder(Order.class)
 *             .anchor("A20")
 *             .data(orders)
 *             .build()));
 * 
 * // 書き込み
 * BookWriter.write(book);
 * </pre>
 * </p>
 */
@Slf4j
public class BookWriter {
    
    private BookWriter() {
        // ユーティリティクラスのためインスタンス化を禁止
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * BookドメインモデルをExcelファイルに書き込む
     * 
     * @param book Bookドメインモデル
     */
    public static void write(Book book) {
        try {
            // 既存ファイルを開くか新規作成してWorkbookを取得
            Workbook workbook = loadExistingWorkbook(book);
            
            // try-with-resourcesでWorkbookとFileOutputStreamを管理
            try (workbook; FileOutputStream fos = new FileOutputStream(book.getFilePath().toFile())) {
                // スタイルの初期化
                CellStyle dateStyle = createDateStyle(workbook);
                CellStyle dateTimeStyle = createDateTimeStyle(workbook);
                
                // 各シートを書き込む
                for (com.example.excel.domain.Sheet sheet : book.getSheets()) {
                    writeSheet(workbook, sheet, dateStyle, dateTimeStyle);
                }
                
                // ファイルに書き込み
                workbook.write(fos);
                log.info("Excelファイルに{}個のシートを書き込みました: {}", 
                    book.getSheetCount(), book.getFilePath());
            }
            
        } catch (IOException e) {
            log.error("Excelファイル書き込み中にエラーが発生: ファイルパス={}, エラー={}", 
                book.getFilePath(), e.getMessage(), e);
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            log.error("Excel処理中にエラーが発生: ファイルパス={}, エラー={}", 
                book.getFilePath(), e.getMessage(), e);
            throw new UncheckedIOException(new IOException("Excel処理中にエラーが発生しました", e));
        }
    }
    
    /**
     * 既存のWorkbookを読み込むか、新規Workbookを作成
     * 
     * @param book Bookドメインモデル
     * @return Workbookインスタンス
     */
    private static Workbook loadExistingWorkbook(Book book) throws IOException {
        if (book.isLoadExisting() && Files.exists(book.getFilePath())) {
            try (FileInputStream fis = new FileInputStream(book.getFilePath().toFile())) {
                log.info("既存のExcelファイルを開きました: {}", book.getFilePath());
                return WorkbookFactory.create(fis);
            }
        } else {
            log.info("新規Excelファイルを作成します: {}", book.getFilePath());
            return new XSSFWorkbook();
        }
    }
    
    /**
     * 日付用スタイルを作成
     */
    private static CellStyle createDateStyle(Workbook workbook) {
        CreationHelper createHelper = workbook.getCreationHelper();
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));
        return style;
    }
    
    /**
     * 日時用スタイルを作成
     */
    private static CellStyle createDateTimeStyle(Workbook workbook) {
        CreationHelper createHelper = workbook.getCreationHelper();
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        return style;
    }
    
    /**
     * シートを書き込む
     * 
     * @param workbook Workbook
     * @param sheet Sheetドメインモデル
     * @param dateStyle 日付用スタイル
     * @param dateTimeStyle 日時用スタイル
     */
    private static void writeSheet(Workbook workbook, com.example.excel.domain.Sheet sheet, 
            CellStyle dateStyle, CellStyle dateTimeStyle) {
        // シートを取得または作成
        Sheet poiSheet = workbook.getSheet(sheet.getName());
        if (poiSheet == null) {
            poiSheet = workbook.createSheet(sheet.getName());
            log.debug("新しいシートを作成: {}", sheet.getName());
        } else {
            log.debug("既存のシートを使用: {}", sheet.getName());
        }
        
        // 各テーブルを書き込む
        for (Table<?> table : sheet.getTables()) {
            writeTable(poiSheet, table, dateStyle, dateTimeStyle);
        }
    }
    
    /**
     * テーブルを書き込む
     * 
     * @param poiSheet POIのSheet
     * @param table Tableドメインモデル
     * @param dateStyle 日付用スタイル
     * @param dateTimeStyle 日時用スタイル
     */
    private static <T> void writeTable(Sheet poiSheet, Table<T> table, 
            CellStyle dateStyle, CellStyle dateTimeStyle) {
        Anchor anchor = table.getAnchor();
        int startRow = anchor.getRow();
        int startColumn = anchor.getColumn();
        
        int currentRow = startRow;
        
        // ヘッダー行を作成
        Row headerRow = poiSheet.getRow(currentRow);
        if (headerRow == null) {
            headerRow = poiSheet.createRow(currentRow);
        }
        createHeaderRow(headerRow, table, startColumn);
        currentRow++;
        
        // データ行を作成
        FieldMappingCache fieldMappingCache = table.getFieldMappingCache();
        for (T bean : table.getData()) {
            Row dataRow = poiSheet.getRow(currentRow);
            if (dataRow == null) {
                dataRow = poiSheet.createRow(currentRow);
            }
            writeDataRow(dataRow, bean, fieldMappingCache, table.isUsePositionMapping(), 
                startColumn, dateStyle, dateTimeStyle);
            currentRow++;
        }
        
        log.debug("テーブルを書き込みました: anchor={}, rows={}", 
            anchor, table.getData().size());
    }
    
    /**
     * ヘッダー行を作成
     */
    private static void createHeaderRow(Row headerRow, Table<?> table, int startColumn) {
        FieldMappingCache fieldMappingCache = table.getFieldMappingCache();
        
        if (table.isUsePositionMapping()) {
            // 位置ベースマッピングの場合は、position順にヘッダーを作成
            for (FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
                if (mappingInfo.position != null) {
                    Cell cell = headerRow.createCell(startColumn + mappingInfo.position);
                    cell.setCellValue(mappingInfo.columnName != null ? mappingInfo.columnName : mappingInfo.field.getName());
                }
            }
        } else {
            // ヘッダーベースマッピングの場合は、フィールド定義順にヘッダーを作成
            // @CsvBindByNameアノテーションからcolumn名を取得
            int columnIndex = startColumn;
            for (FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
                if (mappingInfo.columnName != null) {
                    Cell cell = headerRow.createCell(columnIndex++);
                    cell.setCellValue(mappingInfo.columnName);
                }
            }
        }
    }
    
    /**
     * データ行を書き込む
     */
    private static <T> void writeDataRow(Row row, T bean, FieldMappingCache fieldMappingCache, 
            boolean usePositionMapping, int startColumn, CellStyle dateStyle, CellStyle dateTimeStyle) {
        try {
            if (usePositionMapping) {
                // 位置ベースマッピング
                for (FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
                    if (mappingInfo.position != null) {
                        Object value = mappingInfo.field.get(bean);
                        Cell cell = row.createCell(startColumn + mappingInfo.position);
                        setCellValue(cell, value, dateStyle, dateTimeStyle);
                    }
                }
            } else {
                // ヘッダーベースマッピング
                int columnIndex = startColumn;
                for (FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
                    if (mappingInfo.columnName != null) {
                        Object value = mappingInfo.field.get(bean);
                        Cell cell = row.createCell(columnIndex++);
                        setCellValue(cell, value, dateStyle, dateTimeStyle);
                    }
                }
            }
        } catch (Exception e) {
            log.error("データ行の書き込み中にエラーが発生: {}", e.getMessage(), e);
            throw new RuntimeException("データ行の書き込み中にエラーが発生しました", e);
        }
    }
    
    /**
     * セルに値を設定
     */
    private static void setCellValue(Cell cell, Object value, CellStyle dateStyle, CellStyle dateTimeStyle) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDate) {
            LocalDate localDate = (LocalDate) value;
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(date);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime) value;
            Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(date);
            cell.setCellStyle(dateTimeStyle);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}

