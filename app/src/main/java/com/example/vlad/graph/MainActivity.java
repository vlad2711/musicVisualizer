package com.example.vlad.graph;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.example.vlad.graph.permissions.PermissionsActivity;
import com.example.vlad.graph.permissions.PermissionsChecker;
import com.example.vlad.graph.renderer.RendererFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Visualizer.OnDataCaptureListener {

    private static final int CAPTURE_SIZE = 1024;
    private static final int REQUEST_CODE = 0;
    public static final String TAG = "MainActivity";
    static final String[] PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Visualizer visualiser;
    private WaveformView waveformView;
    private MediaRecorder mediaRecorder;
    private String fileName = Environment.getExternalStorageDirectory() + "/record.3gpp";;
    private AudioRecord audioRecord;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        waveformView = (WaveformView) findViewById(R.id.waveform_view);
        RendererFactory rendererFactory = new RendererFactory();
        waveformView.setRenderer(rendererFactory.createSimpleWaveformRenderer(Color.GREEN, Color.DKGRAY));


        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100,16,3,1024);
        
        File file = new File(fileName);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioSessionId(audioRecord.getAudioSessionId());
        try {
            mediaPlayer.setDataSource(fileName);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PermissionsChecker checker = new PermissionsChecker(this);

        if (checker.lacksPermissions(PERMISSIONS)) {
            startPermissionsActivity();
        } else {
            try {

                audioRecord.startRecording();
                startVisualiser();
                mediaPlayer.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG,"onResume");

    }

    private void startPermissionsActivity() {
        PermissionsActivity.startActivityForResult(this, REQUEST_CODE, PERMISSIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == PermissionsActivity.PERMISSIONS_DENIED) {
            finish();
        }
    }

    private void startVisualiser() throws IOException {

        visualiser = new Visualizer(MediaRecorder.AudioSource.DEFAULT );
        visualiser.setCaptureSize(CAPTURE_SIZE);
        visualiser.setDataCaptureListener(this, Visualizer.getMaxCaptureRate(), true, true);
        visualiser.setEnabled(true);
        visualiser.setScalingMode(audioRecord.getAudioSessionId());
        Log.d(TAG,"visualiser + " + visualiser.getEnabled());

    }

    @Override
    public void onWaveFormDataCapture(Visualizer thisVisualiser, byte[] waveform, int samplingRate) {
        if (waveformView != null) {
            
                new AsyncTask<Void, Void, byte[]>() {
                    @Override
                    protected void onPostExecute(byte[] cat) {
                        super.onPostExecute(cat);
                        waveformView.setWaveform(cat);
                    }

                    @Override
                    protected byte[] doInBackground(Void... params) {
                       byte[] shtuka = new  byte[1024];
                        for (int i = 0; i < 1024; i++) {
                            audioRecord.read(shtuka, i, 1024);
                            
                           // Log.d(TAG, String.valueOf(shtuka[i]));
                        }
                        return shtuka;
                    }
                }.execute();
        }
    }

    @Override
    public void onFftDataCapture(Visualizer thisVisualiser, byte[] fft, int samplingRate) {
        for (int i = 0; i < fft.length; i++) {
            Log.d(TAG, String.valueOf(fft[i]));
        }
    }

    @Override
    protected void onPause() {
        if (visualiser != null) {
            visualiser.setEnabled(false);
            visualiser.setDataCaptureListener(null, 0, false, false);
            visualiser.release();
            audioRecord.stop();
        }
        Log.d(TAG,"pause");
        super.onPause();
    }
}
