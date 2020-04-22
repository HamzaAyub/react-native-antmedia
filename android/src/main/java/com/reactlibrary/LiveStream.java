package com.reactlibrary;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.audio.WebRtcAudioRecord;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;
import io.antmedia.webrtcandroidframework.apprtc.CallFragment;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;

public class LiveStream extends AppCompatActivity implements IWebRTCListener {

    public static final String SERVER_URL = "ws://34.255.219.25:5080/LiveApp/websocket";
    private CallFragment callFragment;

    private WebRTCClient webRTCClient;
    private ImageButton btnCameraSwitch;
    private ImageButton btnFlashLight;
    private LinearLayout showLiveIconWithText;
    private TextView txtShowTime;
    private Handler handler;
    private int i = 0;
    private ImageView btnStopBroadcast;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_live_stream);

        handler = new Handler();
        btnCameraSwitch = findViewById(R.id.btn_switch_camera);
        btnFlashLight = findViewById(R.id.btn_flash_light);
        showLiveIconWithText = findViewById(R.id.show_live_icon_with_text);
        txtShowTime = findViewById(R.id.txt_show_time);
        btnStopBroadcast = findViewById(R.id.btn_stop_broadcast);
        webRTCClient = new WebRTCClient(this, this, new WebRtcAudioRecord.IAudioRecordStatusListener() {
            @Override
            public void audioRecordStarted() {
                Log.i("AudioStatus", "Audio recorder started");
            }

            @Override
            public void audioRecordStopped() {
                Log.i("AudioStatus", "Audio recorder stopped");
            }
        });


        //webRTCClient.setOpenFrontCamera(false);

        String streamId = "stream2";
        String tokenId = "tokenId";

        SurfaceViewRenderer cameraViewRenderer = findViewById(R.id.publish_view_renderer);
        webRTCClient.setVideoRenderers(null, cameraViewRenderer);

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);


        webRTCClient.init(SERVER_URL, streamId, IWebRTCClient.MODE_PUBLISH, tokenId, this.getIntent());

        btnCameraSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webRTCClient.switchCamera();
            }
        });

        btnFlashLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        btnStopBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(LiveStream.this);
                builder.setTitle("Stop Broadcast!");
                builder.setMessage("Are you sure you want to stop this broadcast?");
                builder.setCancelable(true);
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

            }
        });

        startTimer();
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            i++;
            if (showLiveIconWithText.getVisibility() == View.VISIBLE) {
                showLiveIconWithText.setVisibility(View.GONE);
            } else {
                showLiveIconWithText.setVisibility(View.VISIBLE);
            }
            txtShowTime.setText(broadcastTime(i));
            startTimer();
        }
    };

    public void startTimer() {
        handler.postDelayed(runnable, 1000);
    }

    public void cancleTimer() {
        handler.removeCallbacks(runnable);
    }

    public String broadcastTime(int time) {
        int sec = 0;
        String strSeconds = "";
        int mint = 0;
        String strMint = "";
        int hour = 0;
        String totalTime = "";
        sec = time % 60;
        if (sec < 10) {
            strSeconds = "0" + sec;
        } else {
            strSeconds = Integer.toString(sec);
        }
        mint = Math.round((time / 60) % 60);
        if (mint < 10) {
            strMint = "0" + mint;
        } else {
            strMint = Integer.toString(mint);
        }
        hour = Math.round(time / 3600);
        totalTime = hour + ":" + strMint + ":" + strSeconds;
        return totalTime;

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startRecording(View v) {

        if (!webRTCClient.isRecording()) {
            ((Button) v).setText("Stop Recording");
            webRTCClient.startRecording(null, 800000, 64000);
        } else {
            webRTCClient.stopRecording();
            ((Button) v).setText("Start Recording");
        }
    }

    @Override
    public void onPlayStarted() {
        Log.w(getClass().getSimpleName(), "onPlayStarted");
        Toast.makeText(this, "Play started", Toast.LENGTH_LONG).show();
        webRTCClient.switchVideoScaling(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    }

    @Override
    public void onPublishStarted() {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onPublishFinished() {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPlayFinished() {
        Log.w(getClass().getSimpleName(), "onPlayFinished");
        Toast.makeText(this, "Play finished", Toast.LENGTH_LONG).show();
    }

    @Override
    public void noStreamExistsToPlay() {
        Log.w(getClass().getSimpleName(), "noStreamExistsToPlay");
        Toast.makeText(this, "No stream exist to play", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String description) {
        Toast.makeText(this, "Error: " + description, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        webRTCClient.stopStream();
        if (webRTCClient.isRecording()) {
            webRTCClient.stopRecording();
        }

        webRTCClient.releaseResources();
        // remove handler count
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code) {
        Toast.makeText(this, "Signal channel closed with code " + code, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisconnected() {
        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected() {
        //it is called when connected to ice
    }

    @Override
    public void onSurfaceInitialized() {
        Log.i(getClass().getSimpleName(), "Surface initialized");

    }
}