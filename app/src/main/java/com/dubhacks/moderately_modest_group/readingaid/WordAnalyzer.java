package com.dubhacks.moderately_modest_group.readingaid;

import android.content.Context;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Robert on 10/22/2017.
 */

public class WordAnalyzer {
    public static Map<String, Double> getWordDifficulties(Context context) throws IOException {
        Map<String, Double> wordDifficulties = new HashMap<>();
        InputStream input = context.getResources().openRawResource(R.raw.worddifficulties);
        // Log.i("Reading Aid", input.toString());
        CSVParser parser = CSVParser.parse(input, StandardCharsets.US_ASCII, CSVFormat.EXCEL);
        List<CSVRecord> records = parser.getRecords();
        int i = 0;
        for (CSVRecord record : records) {
            if (i < 1) {
                i++;
                continue;
            }

            String word = record.get(0);
            Double ageOfAcquisition;
            try {
                ageOfAcquisition = Double.parseDouble(record.get(1));
            } catch (NumberFormatException e) {
                ageOfAcquisition = null;
            }
            wordDifficulties.put(word, ageOfAcquisition);

        }

        return wordDifficulties;
    }
}
