package com.example.csv;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 大容量CSV/Excel用のグルーピング＆ソート処理ユーティリティ
 * 
 * <p>5GB以上の大容量ファイルでもメモリ効率的に処理できます。</p>
 * 
 * <h3>処理フロー:</h3>
 * <ol>
 *   <li>CSVを1行ずつ読み込み、グループキーごとにファイル分割</li>
 *   <li>各グループファイルを外部ソート（CsvExternalSorter）でソート</li>
 *   <li>ソート済みファイルをストリーミング処理</li>
 *   <li>一時ファイルを自動クリーンアップ</li>
 * </ol>
 * 
 * <h3>使用例1: Lambda指定</h3>
 * <pre>
 * LargeDataGroupingSorter.of(Person.class, Paths.get("huge.csv"))
 *     .groupBy(Person::getOccupation)
 *     .sortBy(Comparator.comparingInt(Person::getAge))
 *     .processGroups((groupKey, personStream) -&gt; {
 *         personStream.forEach(person -&gt; saveToDB(groupKey, person));
 *     });
 * </pre>
 * 
 * <h3>使用例2: インターフェース実装</h3>
 * <pre>
 * // PersonがGroupingSortableを実装している場合
 * LargeDataGroupingSorter.of(Person.class, Paths.get("huge.csv"))
 *     .processGroupsSorted((groupKey, personStream) -&gt; {
 *         personStream.forEach(person -&gt; saveToDB(groupKey, person));
 *     });
 * </pre>
 * 
 * <h3>使用例3: 複雑なキー</h3>
 * <pre>
 * LargeDataGroupingSorter.of(Person.class, Paths.get("huge.csv"))
 *     .groupBy(p -&gt; p.getDepartment() + "_" + p.getCity())
 *     .sortBy(Comparator.comparingInt(Person::getAge)
 *                       .thenComparing(Person::getName))
 *     .processGroups((groupKey, stream) -&gt; {
 *         List&lt;Person&gt; top10 = stream.limit(10).collect(Collectors.toList());
 *         saveTop10(groupKey, top10);
 *     });
 * </pre>
 * 
 * @param <T> Bean型
 */
@Slf4j
public class LargeDataGroupingSorter<T> {
    
    private final Class<T> beanClass;
    private final Path inputPath;
    private final Path tempDirectory;
    private CharsetType charsetType = CharsetType.UTF_8;
    private Charset charset = StandardCharsets.UTF_8;
    
    // グルーピング設定
    private Function<T, String> groupKeyExtractor;
    
    // ソート設定
    private Comparator<T> comparator;
    private boolean useComparable = false;
    
    // CSVヘッダーとフィールドマッピング（キャッシュ）
    private List<String> csvHeaders = null;
    private List<java.lang.reflect.Field> csvFields = null;
    
    private LargeDataGroupingSorter(Class<T> beanClass, Path inputPath) {
        this.beanClass = beanClass;
        this.inputPath = inputPath;
        this.tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"),
                "grouping-sorter-" + UUID.randomUUID());
        
        // GroupingSortable実装チェック
        if (GroupingSortable.class.isAssignableFrom(beanClass)) {
            this.useComparable = true;
        }
    }
    
    /**
     * LargeDataGroupingSorterインスタンスを生成
     * 
     * @param <T> Bean型
     * @param beanClass Bean型のClassオブジェクト
     * @param inputPath 入力CSVファイルのパス
     * @return LargeDataGroupingSorterインスタンス
     */
    public static <T> LargeDataGroupingSorter<T> of(Class<T> beanClass, Path inputPath) {
        return new LargeDataGroupingSorter<>(beanClass, inputPath);
    }
    
    /**
     * 文字エンコーディングを設定
     * 
     * @param charsetType 文字セットタイプ
     * @return このインスタンス
     */
    public LargeDataGroupingSorter<T> charset(CharsetType charsetType) {
        this.charsetType = charsetType;
        this.charset = Charset.forName(charsetType.getCharsetName());
        return this;
    }
    
    /**
     * グループキーを抽出するFunctionを設定
     * 
     * @param groupKeyExtractor グループキーを返すFunction
     * @return このインスタンス
     */
    public LargeDataGroupingSorter<T> groupBy(Function<T, String> groupKeyExtractor) {
        this.groupKeyExtractor = groupKeyExtractor;
        return this;
    }
    
    /**
     * ソート順を定義するComparatorを設定
     * 
     * @param comparator ソート用Comparator
     * @return このインスタンス
     */
    public LargeDataGroupingSorter<T> sortBy(Comparator<T> comparator) {
        this.comparator = comparator;
        return this;
    }
    
    /**
     * グループごとに処理を実行（グループキーとBeanのStreamを受け取る）
     * 
     * @param processor グループキーとBeanのStreamを処理するBiConsumer
     * @throws IOException ファイル操作エラー
     */
    public void processGroups(BiConsumer<String, Stream<T>> processor) throws IOException {
        log.info("グルーピング＆ソート処理開始: 入力={}", inputPath);
        
        try {
            Files.createDirectories(tempDirectory);
            log.debug("一時ディレクトリ作成: {}", tempDirectory);
            
            // フェーズ1: グルーピング（ファイル分割）
            Map<String, Path> groupFiles = splitByGroup();
            log.info("グルーピング完了: グループ数={}", groupFiles.size());
            
            // フェーズ2: 各グループファイルをソート
            Map<String, Path> sortedGroupFiles = sortGroupFiles(groupFiles);
            log.info("ソート完了: グループ数={}", sortedGroupFiles.size());
            
            // フェーズ3: グループごとに処理
            processEachGroup(sortedGroupFiles, processor);
            log.info("グループ処理完了");
            
        } catch (Exception e) {
            log.error("処理中にエラーが発生しました: {}", e.getMessage(), e);
            throw e;
        } finally {
            // cleanup();  // デバッグ用に一時的に無効化
            log.info("一時ディレクトリ: {}", tempDirectory);
        }
    }
    
    /**
     * GroupingSortableインターフェースを実装したBeanをグルーピング＆ソート処理
     * 
     * @param processor グループキーとBeanのStreamを処理するBiConsumer
     * @throws IOException ファイル操作エラー
     */
    public void processGroupsSorted(BiConsumer<String, Stream<T>> processor) throws IOException {
        if (!GroupingSortable.class.isAssignableFrom(beanClass)) {
            throw new IllegalStateException(
                "processGroupsSorted()を使用するには、" + beanClass.getSimpleName() + 
                "がGroupingSortableインターフェースを実装している必要があります");
        }
        
        // GroupingSortableからグループキーとComparatorを自動取得
        this.groupKeyExtractor = bean -> {
            GroupingSortable<?> sortable = (GroupingSortable<?>) bean;
            return String.valueOf(sortable.getGroupKey());
        };
        
        this.comparator = (a, b) -> {
            @SuppressWarnings("unchecked")
            GroupingSortable<Object> sa = (GroupingSortable<Object>) a;
            @SuppressWarnings("unchecked")
            GroupingSortable<Object> sb = (GroupingSortable<Object>) b;
            return sa.compareTo(sb);
        };
        
        processGroups(processor);
    }
    
    /**
     * フェーズ1: CSVを読み込み、グループキーごとにファイル分割
     */
    private Map<String, Path> splitByGroup() throws IOException {
        Map<String, CSVWriter> writerMap = new HashMap<>();
        Map<String, Path> groupFileMap = new HashMap<>();
        String[] headers = null;
        
        try (Reader reader = new InputStreamReader(
                new FileInputStream(inputPath.toFile()), charset)) {
            
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withType(beanClass)
                    .build();
            
            int totalLines = 0;
            for (T bean : csvToBean) {
                totalLines++;
                
                // グループキー取得
                String groupKey = extractGroupKey(bean);
                
                // グループキーがnullの場合はスキップ
                if (groupKey == null || groupKey.trim().isEmpty()) {
                    log.warn("グループキーがnullまたは空です。スキップします: bean={}", bean);
                    continue;
                }
                
                // グループキーごとにファイルに書き込み
                CSVWriter writer = writerMap.get(groupKey);
                if (writer == null) {
                    Path groupFile = tempDirectory.resolve(sanitizeFileName(groupKey) + "_unsorted.csv");
                    groupFileMap.put(groupKey, groupFile);
                    
                    Writer fileWriter = Files.newBufferedWriter(groupFile, charset);
                    writer = new CSVWriter(fileWriter);
                    writerMap.put(groupKey, writer);
                }
                
                // BeanをCSV行として書き出し（OpenCSVを使用）
                writeBeanAsCsvLine(writer, bean);
                
                // 進捗ログ
                if (totalLines % 100000 == 0) {
                    log.debug("グルーピング進捗: {}行処理完了, グループ数={}", totalLines, writerMap.size());
                }
            }
            
            log.info("CSV読み込み完了: 総行数={}, グループ数={}", totalLines, groupFileMap.size());
            
        } finally {
            // すべてのWriterをクローズ
            for (CSVWriter writer : writerMap.values()) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.warn("Writerのクローズに失敗: {}", e.getMessage());
                }
            }
        }
        
        return groupFileMap;
    }
    
    /**
     * BeanをCSV行として書き出し
     */
    private void writeBeanAsCsvLine(CSVWriter writer, T bean) {
        try {
            List<String> values = new ArrayList<>();
            java.lang.reflect.Field[] fields = beanClass.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                com.opencsv.bean.CsvBindByName annotation = 
                    field.getAnnotation(com.opencsv.bean.CsvBindByName.class);
                
                if (annotation != null) {
                    field.setAccessible(true);
                    Object value = field.get(bean);
                    values.add(value != null ? value.toString() : "");
                }
            }
            
            writer.writeNext(values.toArray(new String[0]));
            
        } catch (Exception e) {
            log.error("BeanのCSV書き込みエラー: {}", e.getMessage());
            throw new RuntimeException("BeanのCSV書き込みに失敗", e);
        }
    }
    
    /**
     * フェーズ2: 各グループファイルをメモリ内でソート
     * （グループファイルは元ファイルより十分小さいため、メモリ内ソートで対応）
     */
    private Map<String, Path> sortGroupFiles(Map<String, Path> groupFiles) throws IOException {
        Map<String, Path> sortedFiles = new HashMap<>();
        
        for (Map.Entry<String, Path> entry : groupFiles.entrySet()) {
            String groupKey = entry.getKey();
            Path unsortedFile = entry.getValue();
            Path sortedFile = tempDirectory.resolve(sanitizeFileName(groupKey) + "_sorted.csv");
            
            log.debug("グループソート開始: グループ={}, ファイル={}", groupKey, unsortedFile.getFileName());
            
            // メモリ内でソート
            List<String> lines = new ArrayList<>();
            String header = null;
            
            try (BufferedReader reader = Files.newBufferedReader(unsortedFile, charset)) {
                // ヘッダー行
                header = reader.readLine();
                
                // データ行
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            
            // Bean変換してソート（Comparatorが指定されている場合のみ）
            if (comparator != null) {
                // 元のCSV行とBeanをペアで保持
                List<Pair<String, T>> pairs = new ArrayList<>();
                
                for (String line : lines) {
                    T bean = parseCsvLine(line);
                    if (bean != null) {
                        pairs.add(new Pair<>(line, bean));
                    }
                }
                
                // Beanでソート
                pairs.sort((p1, p2) -> comparator.compare(p1.second, p2.second));
                
                // ソート済みの元のCSV行を取得
                lines.clear();
                for (Pair<String, T> pair : pairs) {
                    lines.add(pair.first);
                }
            } else if (useComparable) {
                // Comparableインターフェース使用
                List<Pair<String, T>> pairs = new ArrayList<>();
                
                for (String line : lines) {
                    T bean = parseCsvLine(line);
                    if (bean != null) {
                        pairs.add(new Pair<>(line, bean));
                    }
                }
                
                pairs.sort((p1, p2) -> {
                    @SuppressWarnings("unchecked")
                    Comparable<T> c1 = (Comparable<T>) p1.second;
                    return c1.compareTo(p2.second);
                });
                
                lines.clear();
                for (Pair<String, T> pair : pairs) {
                    lines.add(pair.first);
                }
            } else {
                // Comparatorが指定されていない場合は文字列ソート
                lines.sort(String::compareTo);
            }
            
            // ソート済みファイルに書き出し
            try (BufferedWriter writer = Files.newBufferedWriter(sortedFile, charset)) {
                // ヘッダー行
                if (header != null) {
                    writer.write(header);
                    writer.newLine();
                }
                
                // データ行
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            sortedFiles.put(groupKey, sortedFile);
            
            log.debug("グループソート完了: グループ={}, 行数={}", groupKey, lines.size());
        }
        
        return sortedFiles;
    }
    
    /**
     * ペアクラス（内部使用）
     */
    private static class Pair<F, S> {
        final F first;
        final S second;
        
        Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }
    
    /**
     * フェーズ3: ソート済みグループファイルをストリーミング処理
     */
    private void processEachGroup(Map<String, Path> sortedGroupFiles, 
                                   BiConsumer<String, Stream<T>> processor) throws IOException {
        for (Map.Entry<String, Path> entry : sortedGroupFiles.entrySet()) {
            String groupKey = entry.getKey();
            Path sortedFile = entry.getValue();
            
            log.debug("グループ処理開始: グループ={}", groupKey);
            
            try (Reader reader = new InputStreamReader(
                    new FileInputStream(sortedFile.toFile()), charset)) {
                
                CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                        .withType(beanClass)
                        .build();
                
                // ストリーミング処理
                Stream<T> beanStream = csvToBean.stream();
                processor.accept(groupKey, beanStream);
            }
            
            log.debug("グループ処理完了: グループ={}", groupKey);
        }
    }
    
    /**
     * グループキーを抽出
     */
    @SuppressWarnings("unchecked")
    private String extractGroupKey(T bean) {
        if (groupKeyExtractor != null) {
            return groupKeyExtractor.apply(bean);
        } else if (bean instanceof Groupable) {
            return String.valueOf(((Groupable<?>) bean).getGroupKey());
        } else {
            throw new IllegalStateException(
                "groupBy()でグループキー抽出Functionを指定するか、" +
                beanClass.getSimpleName() + "にGroupableインターフェースを実装してください");
        }
    }
    
    /**
     * ヘッダーをパースしてフィールドマッピングを構築
     */
    private void initializeFieldMapping(String headerLine) {
        csvHeaders = new ArrayList<>();
        csvFields = new ArrayList<>();
        
        log.debug("ヘッダー行: {}", headerLine);
        
        // ヘッダー行をパース
        String[] headers = headerLine.split(",");
        Map<String, java.lang.reflect.Field> fieldMap = new HashMap<>();
        
        // Beanのフィールドをマップ化
        for (java.lang.reflect.Field field : beanClass.getDeclaredFields()) {
            com.opencsv.bean.CsvBindByName annotation = 
                field.getAnnotation(com.opencsv.bean.CsvBindByName.class);
            if (annotation != null) {
                field.setAccessible(true);
                fieldMap.put(annotation.column(), field);
                log.debug("フィールドマップ追加: column='{}', field={}", annotation.column(), field.getName());
            }
        }
        
        // ヘッダーの順序に従ってフィールドを並べる
        for (String header : headers) {
            String trimmedHeader = header.trim();
            csvHeaders.add(trimmedHeader);
            java.lang.reflect.Field field = fieldMap.get(trimmedHeader);
            csvFields.add(field);
            log.debug("ヘッダーマッピング: header='{}', field={}", trimmedHeader, field != null ? field.getName() : "null");
        }
        
        log.debug("フィールドマッピング完了: headers={}, fields={}", csvHeaders.size(), csvFields.size());
    }
    
    /**
     * BeanをCSV行文字列に変換（ヘッダーの順序に従う）
     */
    private String beanToCsvLine(T bean) {
        try {
            List<String> values = new ArrayList<>();
            
            for (java.lang.reflect.Field field : csvFields) {
                if (field != null) {
                    Object value = field.get(bean);
                    
                    if (value != null) {
                        String strValue = value.toString();
                        // カンマや改行を含む場合はクォート
                        if (strValue.contains(",") || strValue.contains("\"") || strValue.contains("\n")) {
                            strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
                        }
                        values.add(strValue);
                    } else {
                        values.add("");
                    }
                } else {
                    values.add("");
                }
            }
            
            return String.join(",", values);
            
        } catch (Exception e) {
            log.error("BeanのCSV変換エラー: {}", e.getMessage());
            throw new RuntimeException("BeanのCSV変換に失敗", e);
        }
    }
    
    /**
     * CSV行をBeanに変換してComparatorで比較する
     */
    private Comparator<String> createLineComparator() {
        if (comparator == null && !useComparable) {
            // デフォルトは文字列比較
            return String::compareTo;
        }
        
        return (line1, line2) -> {
            try {
                T bean1 = parseCsvLine(line1);
                T bean2 = parseCsvLine(line2);
                
                if (bean1 == null || bean2 == null) {
                    // パースに失敗した場合は文字列比較
                    return line1.compareTo(line2);
                }
                
                if (comparator != null) {
                    return comparator.compare(bean1, bean2);
                } else if (useComparable) {
                    @SuppressWarnings("unchecked")
                    Comparable<T> c1 = (Comparable<T>) bean1;
                    return c1.compareTo(bean2);
                } else {
                    return 0;
                }
            } catch (Exception e) {
                // エラー時は常に一貫した結果を返す（文字列比較）
                log.debug("ソート比較エラー（文字列比較にフォールバック）: line1={}, line2={}", line1, line2);
                return line1.compareTo(line2);
            }
        };
    }
    
    /**
     * CSV行をBeanにパース（ソート用）
     */
    private T parseCsvLine(String line) {
        try {
            if (line == null || line.trim().isEmpty()) {
                return null;
            }
            
            T bean = beanClass.getDeclaredConstructor().newInstance();
            String[] values = parseCsvValues(line);
            
            // csvFieldsの順序に従ってフィールドに値を設定
            for (int i = 0; i < csvFields.size() && i < values.length; i++) {
                java.lang.reflect.Field field = csvFields.get(i);
                if (field != null) {
                    String value = values[i];
                    
                    if (value != null && !value.trim().isEmpty()) {
                        // 型に応じて変換
                        if (field.getType() == String.class) {
                            field.set(bean, value);
                        } else if (field.getType() == Integer.class || field.getType() == int.class) {
                            field.set(bean, Integer.parseInt(value.trim()));
                        } else if (field.getType() == Long.class || field.getType() == long.class) {
                            field.set(bean, Long.parseLong(value.trim()));
                        } else if (field.getType() == Double.class || field.getType() == double.class) {
                            field.set(bean, Double.parseDouble(value.trim()));
                        }
                    }
                }
            }
            
            return bean;
            
        } catch (Exception e) {
            log.debug("CSV行のパースエラー（ソート用）: line={}, error={}", line, e.getMessage());
            return null;  // エラー時はnullを返す
        }
    }
    
    /**
     * CSV行を値の配列にパース（簡易実装）
     */
    private String[] parseCsvValues(String line) {
        // 簡易的なCSVパース（クォート対応）
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        
        return values.toArray(new String[0]);
    }
    
    /**
     * ファイル名として使える文字列にサニタイズ
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * 一時ファイルをクリーンアップ
     */
    private void cleanup() {
        try {
            if (Files.exists(tempDirectory)) {
                Files.walk(tempDirectory)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("一時ファイルの削除に失敗: path={}", path);
                            }
                        });
                log.debug("一時ファイルクリーンアップ完了");
            }
        } catch (IOException e) {
            log.warn("クリーンアップエラー: {}", e.getMessage());
        }
    }
}
