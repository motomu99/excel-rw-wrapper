package com.example.csv.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.opencsv.exceptions.CsvException;
import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.common.mapping.FieldMappingCache;
import com.example.common.mapping.MappingStrategyDetector;
import com.example.common.mapping.MappingStrategyFactory;
import com.example.common.util.BomHandler;
import com.example.common.util.CharsetDetector;
import com.example.exception.CsvReadException;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.MappingStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * CSVファイルをStreamとして読み込むビルダークラス
 * Builderパターンを使用してCSV読み込み処理を抽象化
 * 
 * 使用例:
 * <pre>
 * List<Person> persons = CsvStreamReader.builder(Person.class, Paths.get("sample.csv"))
 *     .charset(CharsetType.UTF_8)
 *     .fileType(FileType.CSV)
 *     .skip(1)
 *     .extract(stream -> stream.collect(Collectors.toList()));
 * </pre>
 */
@Slf4j
public class CsvStreamReader<T> {
    
    private final Class<T> beanClass;
    private final Path filePath;
    /** 文字コードが明示的に指定されたかどうか（nullの場合は自動判別） */
    CharsetType charsetType = null;
    FileType fileType = FileType.CSV;
    int skipLines = 0;
    Boolean usePositionMapping = null;
    
    private CsvStreamReader(Class<T> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }
    
    /**
     * Builderインスタンスを生成
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath CSVファイルのパス
     * @return Builderインスタンス
     */
    public static <T> Builder<T> builder(Class<T> beanClass, Path filePath) {
        return new Builder<>(beanClass, filePath);
    }
    
    /**
     * Streamを処理して結果を返す
     * 
     * @param <R> 戻り値の型
     * @param processor Streamを処理する関数
     * @return 処理結果
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    private <R> R extract(Function<Stream<T>, R> processor) throws IOException, CsvException {
        // マッピング戦略が未設定の場合、アノテーションから自動判定
        if (usePositionMapping == null) {
            usePositionMapping = MappingStrategyDetector.detectUsePositionMapping(beanClass)
                    .orElse(false); // デフォルトはヘッダーベース
        }

        // 文字コードが明示的に指定されていない場合は自動判別
        Charset charset;
        boolean withBom;
        if (charsetType == null) {
            try {
                charset = CharsetDetector.detect(filePath);
                // UTF-8の場合はBOMの有無を確認
                if (StandardCharsets.UTF_8.equals(charset)) {
                    withBom = BomHandler.hasBom(filePath);
                } else {
                    withBom = false;
                }
                log.debug("文字コードを自動判別しました: charset={}, withBom={}", charset, withBom);
            } catch (IOException e) {
                log.warn("文字コードの自動判別に失敗しました。UTF-8を使用します: {}", e.getMessage());
                charset = StandardCharsets.UTF_8;
                withBom = false;
            }
        } else {
            charset = Charset.forName(charsetType.getCharsetName());
            withBom = charsetType.isWithBom();
        }
        
        CsvColumnValidator.validate(filePath, charset, withBom, fileType.getDelimiter().charAt(0));

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             InputStream is = withBom ? BomHandler.skipBom(fis) : fis;
             InputStreamReader isr = new InputStreamReader(is, charset)) {
            
            MappingStrategy<T> strategy = MappingStrategyFactory.createStrategy(beanClass, usePositionMapping);
            
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(isr)
                    .withMappingStrategy(strategy)
                    .withSeparator(fileType.getDelimiter().charAt(0))
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            
            Stream<T> stream = csvToBean.stream();

            // 行番号フィールドが存在する場合は行番号を設定
            FieldMappingCache fieldMappingCache = new FieldMappingCache(beanClass);
            if (fieldMappingCache.hasLineNumberField()) {
                java.lang.reflect.Field lineNumberField = fieldMappingCache.getLineNumberField();
                Class<?> fieldType = lineNumberField.getType();

                // CSVファイルの場合、ヘッダー行を考慮して2行目からデータが始まる(1ベースで行番号2)
                AtomicInteger lineNumber = new AtomicInteger(2);

                stream = stream.peek(bean -> {
                    try {
                        int currentLineNumber = lineNumber.getAndIncrement();
                        if (fieldType == Integer.class || fieldType == int.class) {
                            lineNumberField.set(bean, currentLineNumber);
                        } else if (fieldType == Long.class || fieldType == long.class) {
                            lineNumberField.set(bean, (long) currentLineNumber);
                        }
                    } catch (IllegalAccessException e) {
                        log.error("行番号の設定に失敗しました: bean={}", bean, e);
                        throw new CsvReadException("行番号の設定に失敗しました", e);
                    }
                });
            }

            // データ行をスキップする処理
            if (skipLines > 0) {
                stream = stream.skip(skipLines);
            }

            // 呼び出し側でStreamを処理（try-with-resources内で完了）
            return processor.apply(stream);
        } catch (IOException e) {
            log.error("CSVファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + filePath, e);
        }
    }

    /**
     * 戻り値不要の処理用ショートカット
     * 
     * @param consumer Streamを消費する処理
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    private void consume(Consumer<Stream<T>> consumer) throws IOException, CsvException {
        extract(stream -> {
            consumer.accept(stream);
            return null;
        });
    }
    
    /**
     * CsvStreamReaderのBuilderクラス
     * 
     * <p>Builderパターンを使用して、CsvStreamReaderの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #extract(Function)}または{@link #consume(Consumer)}を呼び出すことで
     * CSVファイルを読み込みます。</p>
     */
    public static class Builder<T> {
        private final CsvStreamReader<T> reader;
        
        private Builder(Class<T> beanClass, Path filePath) {
            this.reader = new CsvStreamReader<>(beanClass, filePath);
        }
        
        /**
         * 文字エンコーディングを設定
         * 
         * <p>明示的に文字コードを指定した場合、自動判別は行われません。
         * 指定しない場合は、ファイルの文字コードを自動判別します。</p>
         * 
         * @param charsetType 文字エンコーディングタイプ
         * @return このBuilderインスタンス
         */
        public Builder<T> charset(CharsetType charsetType) {
            reader.charsetType = charsetType;
            return this;
        }
        
        /**
         * ファイルタイプを設定
         * 
         * @param fileType ファイルタイプ
         * @return このBuilderインスタンス
         */
        public Builder<T> fileType(FileType fileType) {
            reader.fileType = fileType;
            return this;
        }
        
        /**
         * スキップする行数を設定
         * 
         * @param lines スキップする行数
         * @return このBuilderインスタンス
         */
        public Builder<T> skip(int lines) {
            reader.skipLines = lines;
            return this;
        }
        
        /**
         * 位置ベースマッピングを使用
         * 
         * @return このBuilderインスタンス
         */
        public Builder<T> usePositionMapping() {
            reader.usePositionMapping = true;
            return this;
        }
        
        /**
         * ヘッダーベースマッピングを使用（デフォルト）
         * 
         * @return このBuilderインスタンス
         */
        public Builder<T> useHeaderMapping() {
            reader.usePositionMapping = false;
            return this;
        }
        
        /**
         * Streamを処理して結果を返す
         * 
         * @param <R> 戻り値の型
         * @param processor Streamを処理する関数
         * @return 処理結果
         * @throws IOException ファイル読み込みエラー
         * @throws CsvException CSV解析エラー
         */
        public <R> R extract(Function<Stream<T>, R> processor) throws IOException, CsvException {
            return reader.extract(processor);
        }
        
        /**
         * 戻り値不要の処理用ショートカット
         * 
         * @param consumer Streamを消費する処理
         * @throws IOException ファイル読み込みエラー
         * @throws CsvException CSV解析エラー
         */
        public void consume(Consumer<Stream<T>> consumer) throws IOException, CsvException {
            reader.consume(consumer);
        }
    }
}

