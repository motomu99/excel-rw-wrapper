package com.example.csv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

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
    
    /** フィールド情報キャッシュ（パフォーマンス最適化用） */
    private Map<Field, FieldMappingInfo> fieldCache = null;
    
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
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            
            // スタイルの初期化
            initializeStyles(workbook);
            
            // シートを作成
            Sheet sheet = workbook.createSheet(sheetName);
            
            // フィールドキャッシュを構築
            buildFieldCache();
            
            // ヘッダー行を作成
            createHeaderRow(sheet);
            
            // データ行を作成
            List<T> dataList = stream.toList();
            int rowIndex = 1;
            for (T bean : dataList) {
                Row row = sheet.createRow(rowIndex++);
                writeDataRow(row, bean);
            }
            
            workbook.write(fos);
            log.info("Excelファイルを作成しました: {}", filePath);
            
        } catch (IOException e) {
            log.error("Excelファイル書き込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Excel処理中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new IOException("Excel処理中にエラーが発生しました", e);
        }
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
     * フィールドキャッシュを構築
     */
    private void buildFieldCache() {
        fieldCache = new LinkedHashMap<>();
        Field[] fields = beanClass.getDeclaredFields();
        
        for (Field field : fields) {
            String columnName = null;
            Integer position = null;

            // 位置ベースマッピング
            CsvBindByPosition positionAnnotation = field.getAnnotation(CsvBindByPosition.class);
            if (positionAnnotation != null) {
                position = positionAnnotation.position();
            }

            // ヘッダーベースマッピング
            CsvBindByName nameAnnotation = field.getAnnotation(CsvBindByName.class);
            if (nameAnnotation != null) {
                columnName = nameAnnotation.column();
            }

            if (columnName != null || position != null) {
                fieldCache.put(field, new FieldMappingInfo(field, columnName, position));
            }
        }
    }

    /**
     * ヘッダー行を作成
     */
    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        
        if (usePositionMapping) {
            // 位置ベースマッピングの場合は、position順にヘッダーを作成
            for (FieldMappingInfo mappingInfo : fieldCache.values()) {
                if (mappingInfo.position != null) {
                    Cell cell = headerRow.createCell(mappingInfo.position);
                    cell.setCellValue(mappingInfo.columnName != null ? mappingInfo.columnName : mappingInfo.field.getName());
                }
            }
        } else {
            // ヘッダーベースマッピングの場合は、フィールド定義順にヘッダーを作成
            int columnIndex = 0;
            for (FieldMappingInfo mappingInfo : fieldCache.values()) {
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
            for (FieldMappingInfo mappingInfo : fieldCache.values()) {
                if (mappingInfo.position != null) {
                    Object value = mappingInfo.field.get(bean);
                    Cell cell = row.createCell(mappingInfo.position);
                    setCellValue(cell, value);
                }
            }
        } else {
            // ヘッダーベースマッピング
            int columnIndex = 0;
            for (FieldMappingInfo mappingInfo : fieldCache.values()) {
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

    /**
     * フィールドマッピング情報を保持する内部クラス
     */
    private static class FieldMappingInfo {
        final Field field;
        final String columnName;
        final Integer position;

        FieldMappingInfo(Field field, String columnName, Integer position) {
            this.field = field;
            this.field.setAccessible(true);
            this.columnName = columnName;
            this.position = position;
        }
    }
}

