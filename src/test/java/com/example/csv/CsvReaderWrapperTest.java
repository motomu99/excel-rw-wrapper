package com.example.csv;

import com.example.csv.model.Person;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import com.opencsv.exceptions.CsvException;

public class CsvReaderWrapperTest {

    @Test
    void testReadCsvToBeans() throws IOException, CsvException {
        // 基本的なBean読み込みの使い方

        List<Person> persons = CsvReaderWrapper.execute(Person.class,Paths.get("src/test/resources/sample.csv"),CsvReaderWrapper::read);
       
        assertNotNull(persons);
        assertEquals(5, persons.size()); // ヘッダーを除いた5件のデータ
        
        // 最初のPersonの確認
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        // 2番目のPersonの確認
        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }
}
