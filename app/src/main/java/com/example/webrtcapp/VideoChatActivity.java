package com.example.webrtcapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.example.webrtcapp.adapters.ChatMessage;
import com.example.webrtcapp.helpers.Constants;

import android.Manifest;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import me.kevingleason.pnwebrtc.PnPeer;
import me.kevingleason.pnwebrtc.PnRTCClient;
import me.kevingleason.pnwebrtc.PnRTCListener;


public class VideoChatActivity extends AppCompatActivity
{
    public static final String VIDEO_TRACK_ID = "videoPN";
    public static final String AUDIO_TRACK_ID = "audioPN";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStreamPN";

    private PnRTCClient pnRTCClient;
    private VideoSource localVideoSource;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private GLSurfaceView mVideoView;

    private String username;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(Constants.USER_NAME))
        {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Need to pass username to VideoChatActivity in intent extras (Constants.USER_NAME).",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        this.username  = extras.getString(Constants.USER_NAME, "");

       VideoCapture(extras);

    }

    private void VideoCapture(Bundle extras) {
        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,  // Hardware Acceleration Enabled
                null); // Render EGL Context

        PeerConnectionFactory pcFactory = new PeerConnectionFactory();
        this.pnRTCClient = new PnRTCClient(Constants.PUB_KEY, Constants.SUB_KEY, this.username);

        // Returns the number of cams & front/back face device name
        int camNumber = VideoCapturerAndroid.getDeviceCount();
        String frontFacingCam = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        String backFacingCam = VideoCapturerAndroid.getNameOfBackFacingDevice();

        // Creates a VideoCapturerAndroid instance for the device name
        VideoCapturer capturer = VideoCapturerAndroid.create(frontFacingCam);

        // First create a Video Source, then we can make a Video Track
        localVideoSource = pcFactory.createVideoSource(capturer, this.pnRTCClient.videoConstraints());
        VideoTrack localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);

        // First we create an AudioSource then we can create our AudioTrack
        AudioSource audioSource = pcFactory.createAudioSource(this.pnRTCClient.audioConstraints());
        AudioTrack localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);

        // To create our VideoRenderer, we can use the included VideoRendererGui for simplicity
        // First we need to set the GLSurfaceView that it should render to
        this.mVideoView = (GLSurfaceView) findViewById(R.id.gl_surface);

        // Then we set that view, and pass a Runnable to run once the surface is ready
        VideoRendererGui.setView(mVideoView, null);

        // Now that VideoRendererGui is ready, we can get our VideoRenderer.
        // IN THIS ORDER. Effects which is on top or bottom
        remoteRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        localRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);

        // We start out with an empty MediaStream object, created with help from our PeerConnectionFactory
        //  Note that LOCAL_MEDIA_STREAM_ID can be any string
        MediaStream mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);

        // Now we can add our tracks.
        mediaStream.addTrack(localVideoTrack);
        mediaStream.addTrack(localAudioTrack);

        // First attach the RTC Listener so that callback events will be triggered
        this.pnRTCClient.attachRTCListener(new MyRTCListener());

        // Then attach your local media stream to the PnRTCClient.
        //  This will trigger the onLocalStream callback.
        this.pnRTCClient.attachLocalMediaStream(mediaStream);

        // Listen on a channel. This is your "phone number," also set the max chat users.
        this.pnRTCClient.listenOn("Kevin");
        this.pnRTCClient.setMaxConnections(1);

        // If the intent contains a number to dial, call it now that you are connected.
        //  Else, remain listening for a call.
        if (extras.containsKey(Constants.CALL_USER)) {
            String callUser = extras.getString(Constants.CALL_USER, "");
            connectToUser(callUser);
        }
    }

    public void connectToUser(String user)
    {
        this.pnRTCClient.connect(user);
    }

    public void hangup(View view)
    {
        this.pnRTCClient.closeAllConnections();
        startActivity(new Intent(VideoChatActivity.this, MainActivity.class));
    }

    private void endCall()
    {
        startActivity(new Intent(VideoChatActivity.this, MainActivity.class));
        finish();
    }



    @Override
    protected void onPause()
    {
        super.onPause();
        this.mVideoView.onPause();
        this.localVideoSource.stop();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        this.mVideoView.onResume();
        this.localVideoSource.restart();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (this.localVideoSource != null)
        {
            this.localVideoSource.stop();
        }
        if (this.pnRTCClient != null)
        {
            this.pnRTCClient.onDestroy();
        }
    }

/*    private void sendMsg(String msgText)
    {
        JSONObject msgJson = new JSONObject();
        try {
            msgJson.put("msg_user", username);
            msgJson.put("msg_text", msgText);
            this.pnRTCClient.transmitAll(msgJson);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }*/

    // VCA Code
    private class MyRTCListener extends PnRTCListener
    {
        @Override
        public void onLocalStream(final MediaStream localStream)
        {
            VideoChatActivity.this.runOnUiThread(new Runnable()
            {
                @Override
                public void run() {
                    if(localStream.videoTracks.size()==0) return;
                    localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
                }
            });
        }

        @Override
        public void onAddRemoteStream(final MediaStream remoteStream, final PnPeer peer)
        {
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoChatActivity.this,"Connected to " + peer.getId(), Toast.LENGTH_SHORT).show();
                    try {
                        if(remoteStream.videoTracks.size()==0) return;
                        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
                        VideoRendererGui.update(remoteRender, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
                        VideoRendererGui.update(localRender, 72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true);
                    }
                    catch (Exception e){ e.printStackTrace(); }
                }
            });
        }

        @Override
        public void onPeerConnectionClosed(PnPeer peer)
        {
            Intent intent = new Intent(VideoChatActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }


      /*  @Override
        public void onMessage(PnPeer peer, Object message)
        {
            if (!(message instanceof JSONObject)) return; //Ignore if not JSONObject
            JSONObject jsonMsg = (JSONObject) message;
            try {
                String user = jsonMsg.getString("msg_user");
                String text = jsonMsg.getString("msg_text");
                final ChatMessage chatMsg = new ChatMessage(user, text);
                VideoChatActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(VideoChatActivity.this,chatMsg.toString(),Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (JSONException e){
                e.printStackTrace();
            }
        }*/
    }
}
