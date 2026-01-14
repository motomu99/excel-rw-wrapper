package com.example.csv.reader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
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
    /** クォートを無視するかどうか（デフォルト: false） */
    boolean ignoreQuotations = false;
    
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

        // 文字コードとBOMを決定
        CharsetAndBom charsetAndBom = determineCharsetAndBom();
        Charset charset = charsetAndBom.charset;
        boolean withBom = charsetAndBom.withBom;
        
        CsvColumnValidator.validate(filePath, charset, withBom, fileType.getDelimiter().charAt(0), ignoreQuotations);

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             InputStream is = withBom ? BomHandler.skipBom(fis) : fis;
             InputStreamReader isr = new InputStreamReader(is, charset);
             com.opencsv.CSVReader csvReader = new com.opencsv.CSVReaderBuilder(isr)
                 .withCSVParser(new com.opencsv.CSVParserBuilder()
                     .withSeparator(fileType.getDelimiter().charAt(0))
                     .withQuoteChar('"')
                     .withIgnoreQuotations(ignoreQuotations)
                     .withIgnoreLeadingWhiteSpace(true)
                     .build())
                 .build()) {
            
            MappingStrategy<T> strategy = MappingStrategyFactory.createStrategy(beanClass, usePositionMapping);
            
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(csvReader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            
            Stream<T> stream = csvToBean.stream();

            // 行番号フィールドが存在する場合は行番号を設定
            FieldMappingCache fieldMappingCache = new FieldMappingCache(beanClass);
            if (fieldMappingCache.hasLineNumberField()) {
                java.lang.reflect.Field lineNumberField = fieldMappingCache.getLineNumberField();
                Class<?> fieldType = lineNumberField.getType();

                // 位置ベースマッピング（ヘッダーなし）の場合は1行目から、
                // ヘッダーベースマッピングの場合は2行目からデータが始まる
                int startLineNumber = (usePositionMapping != null && usePositionMapping) ? 1 : 2;
                AtomicInteger lineNumber = new AtomicInteger(startLineNumber);

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
     * 文字コードとBOMを決定する
     * 
     * <p>ファイルが存在しない場合は即座にCsvReadExceptionを投げます。</p>
     * 文字コードの自動判別に失敗した場合はUTF-8をデフォルトとして使用します。
     * 
     * @return 文字コードとBOMの情報
     * @throws CsvReadException ファイルが存在しない場合
     */
    private CharsetAndBom determineCharsetAndBom() {
        if (charsetType == null) {
            // 文字コードが明示的に指定されていない場合は自動判別
            try {
                Charset detectedCharset = CharsetDetector.detect(filePath);
                
                // UTF-8の場合はBOMの有無を確認
                boolean bom = false;
                if (StandardCharsets.UTF_8.equals(detectedCharset)) {
                    bom = BomHandler.hasBom(filePath);
                }
                
                log.debug("文字コードを自動判別しました: charset={}, withBom={}", detectedCharset, bom);
                return new CharsetAndBom(detectedCharset, bom);
            } catch (FileNotFoundException e) {
                // ファイルが存在しない場合は即座にエラーにする
                log.error("ファイルが存在しません: ファイルパス={}", filePath, e);
                throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + filePath, e);
            } catch (IOException e) {
                // 文字コードの自動判別に失敗した場合はUTF-8をデフォルトとして使用
                log.warn("文字コードの自動判別に失敗しました。UTF-8を使用します: {}", e.getMessage());
                return new CharsetAndBom(StandardCharsets.UTF_8, false);
            }
        } else {
            // 明示的に指定された文字コードを使用
            Charset charset = Charset.forName(charsetType.getCharsetName());
            boolean bom = charsetType.isWithBom();
            return new CharsetAndBom(charset, bom);
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
     * CSV行が空かどうかを判定（CsvColumnValidatorと同じロジック）
     * 
     * @param row CSV行
     * @return 空行の場合true
     */
    private static boolean isRowEmptyForCsv(String[] row) {
        return row.length == 1 && row[0].isEmpty();
    }

    /**
     * 文字コードとBOMの情報を保持する内部クラス
     */
    private static class CharsetAndBom {
        final Charset charset;
        final boolean withBom;
        
        CharsetAndBom(Charset charset, boolean withBom) {
            this.charset = charset;
            this.withBom = withBom;
        }
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
         * クォートを無視するかどうかを設定
         * 
         * <p>エスケープされていないダブルクォートが含まれるTSV/CSVファイルを
         * 読み込む場合に使用します。trueに設定すると、ダブルクォートを
         * 通常の文字として扱います。</p>
         * 
         * @param ignoreQuotations クォートを無視する場合true（デフォルト: false）
         * @return このBuilderインスタンス
         */
        public Builder<T> ignoreQuotations(boolean ignoreQuotations) {
            reader.ignoreQuotations = ignoreQuotations;
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
        
        /**
         * 列数チェックを有効にしてCSVファイルを読み込み、エラー行も含めた結果を返す
         * 
         * <p>列数が不一致の行はスキップされ、エラー情報として記録されます。
         * 処理は最後まで続行され、成功した行とエラー行の情報が返されます。</p>
         * 
         * @return CsvReadResult（成功した行のデータとエラー行の情報を含む）
         * @throws IOException ファイル読み込みエラー
         * @throws CsvException CSV解析エラー
         */
        public CsvReadResult<T> readWithValidation() throws IOException, CsvException {
            // マッピング戦略が未設定の場合、アノテーションから自動判定
            if (reader.usePositionMapping == null) {
                reader.usePositionMapping = MappingStrategyDetector.detectUsePositionMapping(reader.beanClass)
                        .orElse(false); // デフォルトはヘッダーベース
            }

            // 文字コードとBOMを決定
            CharsetAndBom charsetAndBom = reader.determineCharsetAndBom();
            Charset charset = charsetAndBom.charset;
            boolean withBom = charsetAndBom.withBom;
            
            // 列数チェックを実行してエラー行を収集
            List<CsvReadError> columnErrors = CsvColumnValidator.validateAndCollectErrors(
                reader.filePath, charset, withBom, reader.fileType.getDelimiter().charAt(0), reader.ignoreQuotations
            );
            
            // エラー行の行番号をSetに変換して高速検索
            java.util.Set<Integer> errorLineNumbers = columnErrors.stream()
                .map(CsvReadError::getLineNumber)
                .collect(java.util.stream.Collectors.toSet());

            // エラー行を除外した一時ファイルを作成
            java.nio.file.Path tempFile = null;
            java.util.Map<Integer, Integer> originalLineNumberMap = new java.util.HashMap<>();
            try {
                if (!errorLineNumbers.isEmpty()) {
                    TempFileResult tempFileResult = createTempFileWithoutErrors(
                        charset, withBom, errorLineNumbers);
                    tempFile = tempFileResult.tempFile;
                    originalLineNumberMap = tempFileResult.originalLineNumberMap;
                }

                // エラー行を除外したファイル（または元のファイル）を読み込む
                java.nio.file.Path fileToRead = tempFile != null ? tempFile : reader.filePath;
                List<T> data = readDataFromFile(fileToRead, charset, withBom, originalLineNumberMap);
                
                return new CsvReadResult<>(data, columnErrors);
            } catch (IOException e) {
                log.error("CSVファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", 
                         reader.filePath, e.getMessage(), e);
                throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + reader.filePath, e);
            } finally {
                // 一時ファイルを削除
                if (tempFile != null) {
                    try {
                        java.nio.file.Files.deleteIfExists(tempFile);
                    } catch (IOException e) {
                        log.warn("一時ファイルの削除に失敗しました: {}", tempFile, e);
                    }
                }
            }
        }
        
        /**
         * エラー行を除外した一時ファイルを作成
         */
        private TempFileResult createTempFileWithoutErrors(Charset charset, boolean withBom,
                java.util.Set<Integer> errorLineNumbers) throws IOException, CsvException {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("csv-read-", ".csv");
            java.util.Map<Integer, Integer> originalLineNumberMap = new java.util.HashMap<>();
            
            try (FileInputStream fis1 = new FileInputStream(reader.filePath.toFile());
                 InputStream is1 = withBom ? BomHandler.skipBom(fis1) : fis1;
                 InputStreamReader isr1 = new InputStreamReader(is1, charset);
                 com.opencsv.CSVReader csvReader = new com.opencsv.CSVReaderBuilder(isr1)
                     .withCSVParser(new com.opencsv.CSVParserBuilder()
                         .withSeparator(reader.fileType.getDelimiter().charAt(0))
                         .withQuoteChar('"')
                         .withIgnoreQuotations(reader.ignoreQuotations)
                         .withIgnoreLeadingWhiteSpace(true)
                         .build())
                     .withFieldAsNull(com.opencsv.enums.CSVReaderNullFieldIndicator.NEITHER)
                     .build();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile.toFile());
                 java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(fos, charset);
                 CSVWriter csvWriter = new CSVWriter(osw, 
                     reader.fileType.getDelimiter().charAt(0),
                     CSVWriter.DEFAULT_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {
                
                String[] row;
                int logicalLineNumber = 0; // 論理的な行番号（空行をスキップ）
                int tempFileLineNumber = 0;
                
                try {
                    row = csvReader.readNext();
                    while (row != null) {
                        // 空行をスキップ（CsvColumnValidatorと同じロジック）
                        if (isRowEmptyForCsv(row)) {
                            // 空行も一時ファイルに書き込む（CSV形式で）
                            csvWriter.writeNext(new String[]{""}, false);
                            row = csvReader.readNext();
                            continue;
                        }
                        logicalLineNumber++;
                        
                        // エラー行でない場合は一時ファイルに書き込む
                        if (!errorLineNumbers.contains(logicalLineNumber)) {
                            tempFileLineNumber++;
                            // CSVパーサーが解析した行をそのままCSV形式で書き込む
                            csvWriter.writeNext(row, false);
                            // 一時ファイルの行番号 → 元のファイルの論理的行番号のマッピング
                            originalLineNumberMap.put(tempFileLineNumber, logicalLineNumber);
                        }
                        // エラー行はスキップ（一時ファイルに書き込まない）
                        
                        row = csvReader.readNext();
                    }
                } catch (CsvValidationException e) {
                    throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + reader.filePath, e);
                }
            }
            
            return new TempFileResult(tempFile, originalLineNumberMap);
        }
        
        /**
         * ファイルからデータを読み込む
         */
        private List<T> readDataFromFile(java.nio.file.Path fileToRead, Charset charset, 
                boolean withBom, java.util.Map<Integer, Integer> originalLineNumberMap) 
                throws IOException, CsvException {
            try (FileInputStream fis = new FileInputStream(fileToRead.toFile());
                 InputStream is = withBom ? BomHandler.skipBom(fis) : fis;
                 InputStreamReader isr = new InputStreamReader(is, charset);
                 com.opencsv.CSVReader csvReader = new com.opencsv.CSVReaderBuilder(isr)
                     .withCSVParser(new com.opencsv.CSVParserBuilder()
                         .withSeparator(reader.fileType.getDelimiter().charAt(0))
                         .withQuoteChar('"')
                         .withIgnoreQuotations(reader.ignoreQuotations)
                         .withIgnoreLeadingWhiteSpace(true)
                         .build())
                     .build()) {
                
                MappingStrategy<T> strategy = MappingStrategyFactory.createStrategy(
                    reader.beanClass, reader.usePositionMapping);
                
                CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(csvReader)
                        .withMappingStrategy(strategy)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withIgnoreEmptyLine(true)
                        .build();
                
                Stream<T> stream = csvToBean.stream();

                // 行番号フィールドが存在する場合は行番号を設定
                FieldMappingCache fieldMappingCache = new FieldMappingCache(reader.beanClass);
                if (fieldMappingCache.hasLineNumberField()) {
                    stream = setLineNumbers(stream, fieldMappingCache, originalLineNumberMap);
                }

                // データ行をスキップする処理
                if (reader.skipLines > 0) {
                    stream = stream.skip(reader.skipLines);
                }

                // ストリームからデータを収集
                return stream.collect(java.util.stream.Collectors.toList());
            }
        }
        
        /**
         * ストリームの各行に元のファイルの行番号を設定
         */
        private Stream<T> setLineNumbers(Stream<T> stream, FieldMappingCache fieldMappingCache,
                java.util.Map<Integer, Integer> originalLineNumberMap) {
            java.lang.reflect.Field lineNumberField = fieldMappingCache.getLineNumberField();
            Class<?> fieldType = lineNumberField.getType();

            // 位置ベースマッピング（ヘッダーなし）の場合は1行目から、
            // ヘッダーベースマッピングの場合は2行目からデータが始まる
            int startLineNumber = (reader.usePositionMapping != null && reader.usePositionMapping) ? 1 : 2;
            AtomicInteger tempFileLineNumber = new AtomicInteger(startLineNumber);

            return stream.peek(bean -> {
                try {
                    int currentTempFileLineNumber = tempFileLineNumber.getAndIncrement();
                    // 一時ファイルの行番号から元のファイルの行番号を取得
                    Integer originalLineNumber = originalLineNumberMap.get(currentTempFileLineNumber);
                    int lineNumberToSet = originalLineNumber != null ? originalLineNumber : currentTempFileLineNumber;
                    
                    if (fieldType == Integer.class || fieldType == int.class) {
                        lineNumberField.set(bean, lineNumberToSet);
                    } else if (fieldType == Long.class || fieldType == long.class) {
                        lineNumberField.set(bean, (long) lineNumberToSet);
                    }
                } catch (IllegalAccessException e) {
                    log.error("行番号の設定に失敗しました: bean={}", bean, e);
                    throw new CsvReadException("行番号の設定に失敗しました", e);
                }
            });
        }
        
        /**
         * 一時ファイル作成結果を保持する内部クラス
         */
        private static class TempFileResult {
            final java.nio.file.Path tempFile;
            final java.util.Map<Integer, Integer> originalLineNumberMap;
            
            TempFileResult(java.nio.file.Path tempFile, 
                    java.util.Map<Integer, Integer> originalLineNumberMap) {
                this.tempFile = tempFile;
                this.originalLineNumberMap = originalLineNumberMap;
            }
        }
    }
}

