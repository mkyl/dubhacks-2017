package com.dubhacks.moderately_modest_group.readingaid;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.graphics.Typeface;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "ReadingAid";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    File tempFile;
    TextToSpeech tts;
    String currentSentenceID = null;

    List<List<String>> wholeDigitizedText;
    SpannableStringBuilder readingString;
    Map<String, String> speakingTokenToSentence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Typeface type = Typeface.createFromAsset(getAssets(), "fonts/CormorantGaramond-Light.ttf");
        TextView tf = (TextView) findViewById(R.id.book_display);
        tf.setTypeface(type);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.i("TTS", "TextToSpeech.OnInitListener.onInit...");
            }
        });

        tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(String utteranceId) {
                MainActivity.this.currentSentenceID = utteranceId;
            }
        });
        tts.setSpeechRate(0.75f);

       readingString = new SpannableStringBuilder();
        ((TextView) findViewById(R.id.book_display)).setText(readingString);


        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    @Override
    protected void onDestroy() {
        /*
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        */

        super.onDestroy();
    }

    public void readString(String string, String id) {
        tts.speak(string, TextToSpeech.QUEUE_ADD, null, id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        tempFile = new File(getExternalCacheDir(), "digitize-me.jpg");
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.i(TAG, "Image capture success. Saved?: " + tempFile.exists());

            try {
                RandomAccessFile f = new RandomAccessFile(tempFile, "r");
                final byte[] b = new byte[(int)tempFile.length()];
                f.readFully(b);
                new Thread() {
                    public void run() {
                        try {
                            JSONObject tempJOSN = DigitizeText.digitizeText(b);
                            wholeDigitizedText = DigitizeText.jsonToStringArray(tempJOSN);
                            startReading();
                        } catch (Exception e) {
                            Log.i(TAG, "" + e);
                        }
                    }
                }.start();
            } catch (Exception e) {
                Log.e(TAG, "something bad happened: " + e);
            }


        } else {
            Log.i(TAG, "No, " + resultCode);
        }
    }

    private void startReading() {
        this.speakingTokenToSentence = new HashMap<>();
        SpannableStringBuilder tempString = new SpannableStringBuilder();
        for (List<String> paragraph : wholeDigitizedText) {
            for (String word : paragraph) {
                tempString.append(word);
                tempString.append(" ");
            }
        }

        String[] sentences = tempString.toString().split("\\.");
        for (String sentence : sentences) {
            String sentenceID =   UUID.randomUUID().toString();
            speakingTokenToSentence.put(sentenceID, sentence);
            readString(sentence, sentenceID);
        }

        highlightAsSpoken(wholeDigitizedText, 160);
    }

    private int indexOfStartOfSentence(String sentence, List<List<String>> digitizedText) {
        if (sentence != null) {
            sentence = sentence.trim();
            if (sentence.equals(""))
                return -1;
        }

        Log.i("ReadingAid", "Trying to find sentence: " + sentence);
        List<String> wordsInTheWhole = new ArrayList<>();
        for(List<String> searchedSentence : digitizedText) {
            wordsInTheWhole.addAll(searchedSentence);
        }

        List<String> processedSentence = new ArrayList<>(Arrays.asList(sentence.split(" ")));
        processedSentence.remove(processedSentence.size() - 1);

        Log.i("ReadingAid", "new sentence, starting at:" + Collections.indexOfSubList(wordsInTheWhole, processedSentence) + processedSentence.size() + 1);

        return Collections.indexOfSubList(wordsInTheWhole, processedSentence) + processedSentence.size() + 1;
    }

    private void highlightAsSpoken(List<List<String>> digitizedText, int WPM) {
        int currentWord = 0;
        int wordCount = 0;

        Map<String, Double> wordDifficulties = null;
        try {
            wordDifficulties = WordAnalyzer.getWordDifficulties(this);
        } catch (Exception e) {
            Log.e("ReadingAId", "" + e);
        }

        Map<String, String> mostDiff = WordAnalyzer.getNWords(wholeDigitizedText, wordDifficulties, 2);

        for(List<String> sentence : digitizedText) {
            wordCount += sentence.size();
        }

        while(currentWord <= wordCount) {
            readingString = new SpannableStringBuilder();
            int anotherCounter = 0;
            for (List<String> paragraph : digitizedText) {
                for (String word : paragraph) {
                    if(currentSentenceID != null) {
                        String currentSpokenSentence = speakingTokenToSentence.get(currentSentenceID);
                        Log.i("ReadingAid", "currently saying: " + currentSpokenSentence);
                        currentWord = indexOfStartOfSentence(currentSpokenSentence, digitizedText);
                        currentSentenceID = null;

                        if (currentWord == -1)
                            return;
                    }

                    Boolean difficult = false;
                    if (mostDiff.get(word.toLowerCase()) != null) {
                        difficult = true;
                    }

                    if(anotherCounter == currentWord) {
                            if (difficult)
                                readingString.append("<div style='color:red'>");
                            readingString.append("<b>");
                            readingString.append(word);
                            readingString.append("</b>");
                            readingString.append(" ");
                            if (difficult)
                                readingString.append("[ " + mostDiff.get(word.toLowerCase()) + " ] ");
                            if (difficult)
                                readingString.append("</div>");
                    } else {
                        if (difficult)
                            readingString.append("<div style='color:red'>");

                        readingString.append(word);
                        readingString.append(" ");

                        if (difficult)
                            readingString.append("[ " + mostDiff.get(word) + " ] ");
                        if (difficult)
                            readingString.append("</div>");
                    }
                    anotherCounter++;
                }
                readingString.append("<hr>");
            }

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) findViewById(R.id.book_display)).setText(Html.fromHtml(readingString.toString()));
                }
            });

            SystemClock.sleep(60000/WPM);
            currentWord++;
        }
    }
}
