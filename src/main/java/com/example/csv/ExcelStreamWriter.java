package com.example.csv;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.extern.slf4j.Slf4j;

/**
 * ExcelファイルをStreamとして書き込むビルダークラス
 * ビルダーパターンを使用してExcel書き込み処理を抽象化
 *
 * <p>このクラスは {@link ExcelStreamReader} に対応する書き込み機能を提供します。
 * ヘッダーベースマッピングと位置ベースマッピングの両方をサポートし、
 * 様々な型（String, Integer, Long, Double, Boolean, LocalDate, LocalDateTime）を
 * 適切に変換してExcelファイルに書き込みます。</p>
 *
 * 使用例:
 * <pre>
 * // 基本的な使用方法（ヘッダーベースマッピング）
 * ExcelStreamWriter.of(Person.class, Paths.get("output.xlsx"))
 *     .write(persons.stream());
 *
 * // シート名を指定
 * ExcelStreamWriter.of(Person.class, Paths.get("output.xlsx"))
 *     .sheetName("社員データ")
 *     .write(persons.stream());
 *
 * // 位置ベースマッピングを使用
 * ExcelStreamWriter.of(PersonWithoutHeader.class, Paths.get("output.xlsx"))
 *     .usePositionMapping()
 *     .write(persons.stream());
 *
 * // 既存ファイル（テンプレート）にデータを書き込む
 * ExcelStreamWriter.of(Person.class, Paths.get("template.xlsx"))
 *     .loadExisting()
 *     .sheetName("データ")
 *     .startCell(2, 0)  // A3セルから書き込み開始
 *     .write(persons.stream());
 * </pre>
 *
 * @param <T> マッピング先のBeanクラスの型
 */
@Slf4j
public class ExcelStreamWriter<T> {

    private final Class<T> beanClass;
    private final Path filePath;
    private String sheetName = "Sheet1";
    
    /** シートインデックス（現在は未使用、将来の拡張用） */
    @SuppressWarnings("unused")
    private int sheetIndex = 0;
    
    private boolean usePositionMapping = false;
    private boolean loadExisting = false;
    private int startRow = 0;
    private int startColumn = 0;
    
    /** フィールド情報キャッシュ（パフォーマンス最適化用） */
    private FieldMappingCache fieldMappingCache = null;
    
    /** 日付スタイルのキャッシュ */
    private CellStyle dateStyle = null;
    private CellStyle dateTimeStyle = null;

    private ExcelStreamWriter(Class<T> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * ExcelStreamWriterのインスタンスを作成
     *
     * @param <T> Beanの型
     * @param beanClass 書き込むBeanクラス
     * @param filePath 出力先のExcelファイルパス
     * @return ExcelStreamWriterのインスタンス
     */
    public static <T> ExcelStreamWriter<T> of(Class<T> beanClass, Path filePath) {
        return new ExcelStreamWriter<>(beanClass, filePath);
    }

    /**
     * シート名を設定
     *
     * @param sheetName シート名
     * @return このインスタンス
     */
    public ExcelStreamWriter<T> sheetName(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    /**
     * シートのインデックスを設定（0から始まる）
     *
     * @param sheetIndex シートのインデックス
     * @return このインスタンス
     */
    public ExcelStreamWriter<T> sheetIndex(int sheetIndex) {
        this.sheetIndex = sheetIndex;
        return this;
    }

    /**
     * 位置ベースマッピングを使用
     *
     * @return このインスタンス
     */
    public ExcelStreamWriter<T> usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }

    /**
     * ヘッダーベースマッピングを使用（デフォルト）
     *
     * @return このインスタンス
     */
    public ExcelStreamWriter<T> useHeaderMapping() {
        this.usePositionMapping = false;
        return this;
    }

    /**
     * 既存ファイルを読み込んで書き込む
     * テンプレートファイルにデータを追記する際に使用
     *
     * @return このインスタンス
     */
    public ExcelStreamWriter<T> loadExisting() {
        this.loadExisting = true;
        return this;
    }

    /**
     * 書き込み開始セルを設定
     * 指定した行・列から書き込みを開始する（0始まり）
     *
     * @param row 開始行（0始まり）
     * @param column 開始列（0始まり）
     * @return このインスタンス
     */
    public ExcelStreamWriter<T> startCell(int row, int column) {
        this.startRow = row;
        this.startColumn = column;
        return this;
    }

    /**
     * Streamを書き込む
     *
     * <p>Streamの要素をExcelファイルに書き込みます。
     * 最初の行にはヘッダーが自動的に作成され、それ以降の行にデータが書き込まれます。
     * 日付型（LocalDate, LocalDateTime）は自動的に適切なフォーマットで書き込まれます。</p>
     *
     * @param stream 書き込むデータのStream
     * @throws IOException ファイル書き込みエラーが発生した場合
     */
    public void write(Stream<T> stream) throws IOException {
        Workbook workbook = null;
        FileOutputStream fos = null;
        
        try {
            // 既存ファイルを開くか新規作成
            if (loadExisting && Files.exists(filePath)) {
                try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                    workbook = WorkbookFactory.create(fis);
                    log.info("既存のExcelファイルを開きました: {}", filePath);
                }
            } else {
                workbook = new XSSFWorkbook();
                log.info("新規Excelファイルを作成します: {}", filePath);
            }
            
            // スタイルの初期化
            initializeStyles(workbook);
            
            // シートを取得または作成
            Sheet sheet = getOrCreateSheet(workbook);
            
            // フィールドキャッシュを構築
            fieldMappingCache = new FieldMappingCache(beanClass);
            
            // ヘッダー行を作成
            createHeaderRow(sheet);
            
            // データ行を作成
            List<T> dataList = stream.toList();
            int rowIndex = startRow + 1;
            for (T bean : dataList) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    row = sheet.createRow(rowIndex);
                }
                writeDataRow(row, bean);
                rowIndex++;
            }
            
            // ファイルに書き込み
            fos = new FileOutputStream(filePath.toFile());
            workbook.write(fos);
            log.info("Excelファイルを保存しました: {}", filePath);
            
        } catch (IOException e) {
            log.error("Excelファイル書き込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Excel処理中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new IOException("Excel処理中にエラーが発生しました", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.warn("FileOutputStreamのクローズに失敗: {}", e.getMessage());
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.warn("Workbookのクローズに失敗: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * シートを取得または作成
     */
    private Sheet getOrCreateSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.createSheet(sheetName);
            log.debug("新しいシートを作成: {}", sheetName);
        } else {
            log.debug("既存のシートを使用: {}", sheetName);
        }
        return sheet;
    }

    /**
     * スタイルを初期化
     */
    private void initializeStyles(Workbook workbook) {
        CreationHelper createHelper = workbook.getCreationHelper();
        
        // 日付用スタイル
        dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));
        
        // 日時用スタイル
        dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
    }


    /**
     * ヘッダー行を作成
     */
    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.getRow(startRow);
        if (headerRow == null) {
            headerRow = sheet.createRow(startRow);
        }
        
        if (usePositionMapping) {
            // 位置ベースマッピングの場合は、position順にヘッダーを作成
            for (FieldMappingCache.FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
                if (mappingInfo.position != null) {
                    Cell cell = headerRow.createCell(startColumn + mappingInfo.position);
                    cell.setCellValue(mappingInfo.columnName != null ? mappingInfo.columnName : mappingInfo.field.getName());
                }
            }
        } else {
            // ヘッダーベースマッピングの場合は、フィールド定義順にヘッダーを作成
            int columnIndex = startColumn;
            for (FieldMappingCache.FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
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
    private void writeDataRow(Row row, T bean) throws Exception {
        if (usePositionMapping) {
            // 位置ベースマッピング
            for (FieldMappingCache.FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
                if (mappingInfo.position != null) {
                    Object value = mappingInfo.field.get(bean);
                    Cell cell = row.createCell(startColumn + mappingInfo.position);
                    setCellValue(cell, value);
                }
            }
        } else {
            // ヘッダーベースマッピング
            int columnIndex = startColumn;
            for (FieldMappingCache.FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
                if (mappingInfo.columnName != null) {
                    Object value = mappingInfo.field.get(bean);
                    Cell cell = row.createCell(columnIndex++);
                    setCellValue(cell, value);
                }
            }
        }
    }

    /**
     * セルに値を設定
     */
    private void setCellValue(Cell cell, Object value) {
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

