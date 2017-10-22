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
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Created by Robert on 10/22/2017.
 */

public class WordAnalyzer {
    private static Map<String, String> dictionary;
    static {
        dictionary = new HashMap<>();
        dictionary.put("artist", "A person who produces paintings or drawings as a profession or hobby.");
        dictionary.put("studio", "A room where an artist, photographer, etc. works.");
        dictionary.put("spend", "To pass time");
        dictionary.put("beside", "Near, next to");
        dictionary.put("stroke", "To pet or touch an animal");
        dictionary.put("moment", "The time");
    }

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
                String ageString = record.get(i);
                if (ageString != null) {
                    ageOfAcquisition = Double.parseDouble(ageString);
                } else {
                    ageOfAcquisition = null;
                }
            } catch (NumberFormatException e) {
                ageOfAcquisition = null;
            }
            wordDifficulties.put(word, ageOfAcquisition);

        }

        return wordDifficulties;
    }


    public static Map<String, String> getNWords(List<List<String>> words,
                                               Map<String, Double> wordDifficulties, int numberOfWords) {
        Queue<Word> pq = new PriorityQueue<>();

        for (List<String> wordsList : words) {
            for (String word : wordsList) {
                pq.add(new Word(word, wordDifficulties.get(word)));
            }
        }

        Map<String, String> definitions = new HashMap<>();

        for (int i = 0; i < numberOfWords; i++) {
            String word = pq.remove().word;
            definitions.put(word, dictionary.get(word));
        }

        return definitions;
    }
}
