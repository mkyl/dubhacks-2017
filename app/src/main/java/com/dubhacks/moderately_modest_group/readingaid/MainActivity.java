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

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "ReadingAid";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    File tempFile;
    TextToSpeech tts;

    List<List<String>> wholeDigitizedText;
    SpannableStringBuilder readingString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                Log.e("TTS", "TextToSpeech.OnInitListener.onInit...");
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

    public void readString(String string) {
        tts.speak(string, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString());
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
        SpannableStringBuilder tempString = new SpannableStringBuilder();
        for (List<String> paragraph : wholeDigitizedText) {
            for (String word : paragraph) {
                tempString.append(word);
                tempString.append(" ");
            }
        }
        readString(tempString.toString());

        highlightAsSpoken(wholeDigitizedText, 150);
    }

    private void highlightAsSpoken(List<List<String>> digitizedText, int WPM) {
        int currentWord = 0;
        int wordCount = 0;

        for(List<String> sentence : digitizedText) {
            wordCount += sentence.size();
        }

        while(currentWord <= wordCount) {
            readingString = new SpannableStringBuilder();
            int anotherCounter = 0;
            for (List<String> paragraph : digitizedText) {
                for (String word : paragraph) {
                    if(anotherCounter == currentWord) {
                        readingString.append("<b>");
                        readingString.append(word);
                        readingString.append("</b>");
                        readingString.append(" ");
                    } else {
                        readingString.append(word);
                        readingString.append(" ");
                    }
                    anotherCounter++;
                }
                readingString.append("\n \n \n");
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
