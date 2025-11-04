package com.example.csv.writer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.common.config.LineSeparatorType;
import com.example.common.mapping.MappingStrategyFactory;
import com.example.common.util.BomHandler;
import com.example.exception.CsvWriteException;
import com.opencsv.bean.MappingStrategy;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.extern.slf4j.Slf4j;

/**
 * CSVファイルにStreamを書き込むビルダークラス
 * Builderパターンを使用してCSV書き込み処理を抽象化
 * 
 * 使用例:
 * <pre>
 * CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
 *     .charset(CharsetType.UTF_8)
 *     .fileType(FileType.CSV)
 *     .lineSeparator(LineSeparatorType.LF)
 *     .write(persons.stream());
 * </pre>
 */
@Slf4j
public class CsvStreamWriter<T> {
    
    private final Class<T> beanClass;
    private final Path filePath;
    CharsetType charsetType = CharsetType.UTF_8;
    FileType fileType = FileType.CSV;
    LineSeparatorType lineSeparatorType = LineSeparatorType.CRLF;
    boolean usePositionMapping = false;
    
    private CsvStreamWriter(Class<T> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }
    
    /**
     * Builderインスタンスを生成
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング元のBeanクラス
     * @param filePath CSVファイルのパス
     * @return Builderインスタンス
     */
    public static <T> Builder<T> builder(Class<T> beanClass, Path filePath) {
        return new Builder<>(beanClass, filePath);
    }
    
    /**
     * Streamを書き込む
     * 
     * @param stream 書き込むBeanのStream
     * @throws IOException ファイル書き込みエラー
     */
    private void write(Stream<T> stream) throws IOException {
        // Streamを一度Listに変換（OpenCSVの制約）
        List<T> beans = stream.collect(Collectors.toList());
        
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            
            // BOM付きUTF-8の場合、BOMを書き込む
            if (charsetType.isWithBom()) {
                BomHandler.writeBom(fos);
            }
            
            try (OutputStreamWriter osw = new OutputStreamWriter(fos, Charset.forName(charsetType.getCharsetName()))) {
                MappingStrategy<T> strategy = MappingStrategyFactory.createStrategy(beanClass, usePositionMapping);
                
                StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(osw)
                        .withMappingStrategy(strategy)
                        .withSeparator(fileType.getDelimiter().charAt(0))
                        .withLineEnd(lineSeparatorType.getSeparator())
                        .build();
                
                beanToCsv.write(beans);
                
                log.info("CSVファイルへの書き込み完了: ファイルパス={}, 件数={}", filePath, beans.size());
            }
        } catch (IOException e) {
            log.error("CSVファイル書き込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvWriteException("CSVファイルの書き込みに失敗しました: " + filePath, e);
        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            log.error("CSV書き込み中にデータ型エラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvWriteException("CSVデータの変換に失敗しました: " + filePath, e);
        }
    }
    
    /**
     * CsvStreamWriterのBuilderクラス
     * 
     * <p>Builderパターンを使用して、CsvStreamWriterの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #write(Stream)}を呼び出すことで
     * CSVファイルに書き込みます。</p>
     */
    public static class Builder<T> {
        private final CsvStreamWriter<T> writer;
        
        private Builder(Class<T> beanClass, Path filePath) {
            this.writer = new CsvStreamWriter<>(beanClass, filePath);
        }
        
        /**
         * 文字エンコーディングを設定
         * 
         * @param charsetType 文字エンコーディングタイプ
         * @return このBuilderインスタンス
         */
        public Builder<T> charset(CharsetType charsetType) {
            writer.charsetType = charsetType;
            return this;
        }
        
        /**
         * ファイルタイプを設定
         * 
         * @param fileType ファイルタイプ
         * @return このBuilderインスタンス
         */
        public Builder<T> fileType(FileType fileType) {
            writer.fileType = fileType;
            return this;
        }
        
        /**
         * 改行コードを設定
         * 
         * @param lineSeparatorType 改行コードタイプ
         * @return このBuilderインスタンス
         */
        public Builder<T> lineSeparator(LineSeparatorType lineSeparatorType) {
            writer.lineSeparatorType = lineSeparatorType;
            return this;
        }
        
        /**
         * 位置ベースマッピングを使用（ヘッダーなし）
         * 
         * @return このBuilderインスタンス
         */
        public Builder<T> usePositionMapping() {
            writer.usePositionMapping = true;
            return this;
        }
        
        /**
         * ヘッダーベースマッピングを使用（ヘッダーあり、デフォルト）
         * 
         * @return このBuilderインスタンス
         */
        public Builder<T> useHeaderMapping() {
            writer.usePositionMapping = false;
            return this;
        }
        
        /**
         * Streamを書き込む
         * 
         * @param stream 書き込むBeanのStream
         * @throws IOException ファイル書き込みエラー
         */
        public void write(Stream<T> stream) throws IOException {
            writer.write(stream);
        }
    }
}
