package com.thedeveloperworldisyours.togethervideo;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.panframe.android.lib.PFAsset;
import com.panframe.android.lib.PFAssetObserver;
import com.panframe.android.lib.PFAssetStatus;
import com.panframe.android.lib.PFNavigationMode;
import com.panframe.android.lib.PFObjectFactory;
import com.panframe.android.lib.PFView;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener, View.OnClickListener,PFAssetObserver, SeekBar.OnSeekBarChangeListener {

    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mVidHolder;
    private SurfaceView mVidSurface;
    private SurfaceView mViewSurfaceLibrary;

    PFView mPfview;
    PFAsset mPfasset;
    PFNavigationMode mCurrentNavigationMode = PFNavigationMode.MOTION;

    boolean 			mUpdateThumb = true;;
    Timer mScrubberMonitorTimer;

    ViewGroup mFrameContainer;
    Button mStopButton;
    Button				mPlayButton;
    Button				mTouchButton;
    SeekBar				mScrubber;
    Button  mBorrarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);


        mFrameContainer = (ViewGroup) findViewById(R.id.framecontainer);
        mFrameContainer.setBackgroundColor(0xFF000000);

//        mVidSurface = (VideoView) findViewById(R.id.surfView);
        mViewSurfaceLibrary = (SurfaceView) findViewById(R.id.surfViewLibrary);



        mPlayButton = (Button)findViewById(R.id.playbutton);
        mStopButton = (Button)findViewById(R.id.stopbutton);
        mTouchButton = (Button)findViewById(R.id.touchbutton);
        mScrubber = (SeekBar)findViewById(R.id.scrubber);
//        mBorrarButton = (Button) findViewById(R.id.stopbuttonBorrar);


        mPlayButton.setOnClickListener(playListener);
        mStopButton.setOnClickListener(stopListener);
        mTouchButton.setOnClickListener(touchListener);
        mScrubber.setOnSeekBarChangeListener(this);

        mScrubber.setEnabled(false);

        loadVideo("android.resource://" + getPackageName() + "/" + R.raw.skyrim360);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(333,333);
        mVidSurface = new SurfaceView(this);
        mVidSurface.setLayoutParams(params);
        mVidSurface.setVisibility(View.VISIBLE);
        mVidHolder = mVidSurface.getHolder();
        mVidHolder.addCallback(this);
        showControls(true);
    }

    public void startAndPlay(){
        mMediaPlayer.start();
        mPfasset.play();
    }

    /**
     * Show/Hide the playback controls
     *
     * @param  bShow  Show or hide the controls. Pass either true or false.
     */
    public void showControls(boolean bShow) {

        int visibility = View.GONE;

        if (bShow)
            visibility = View.VISIBLE;

        mPlayButton.setVisibility(visibility);
        mStopButton.setVisibility(visibility);
        mTouchButton.setVisibility(visibility);
        mScrubber.setVisibility(visibility);

        if (mPfview != null)
        {
            if (!mPfview.supportsNavigationMode(PFNavigationMode.MOTION))
//				_touchButton.setVisibility(View.GONE);
                Log.d("SimplePlayer", "Not supported nav");
        }
    }

    /**
     * Start the video with a local file path
     *
     * @param  filename  The file path on device storage
     */
    public void loadVideo(String filename)
    {

        mPfview = PFObjectFactory.view(MainActivity.this);
        mPfasset = PFObjectFactory.assetFromUri(this, Uri.parse(filename), this);

        mPfview.displayAsset(mPfasset);
        mPfview.setNavigationMode(mCurrentNavigationMode);

        //mViewSurfaceLibrary
        mFrameContainer.addView(mPfview.getView(), 0);

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mUpdateThumb = false;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mPfasset.setPLaybackTime(seekBar.getProgress());
        mUpdateThumb = true;
    }

    @Override
    public void onStatusMessage(final PFAsset pfAsset, PFAssetStatus pfAssetStatus) {

        switch (pfAssetStatus)
        {
            case LOADED:
                Log.d("SimplePlayer", "Loaded");
                break;
            case DOWNLOADING:
                Log.d("SimplePlayer", "Downloading 360 movie: "+ mPfasset.getDownloadProgress()+" percent complete");
                break;
            case DOWNLOADED:
                Log.d("SimplePlayer", "Downloaded to "+pfAsset.getUrl());
                break;
            case DOWNLOADCANCELLED:
                Log.d("SimplePlayer", "Download cancelled");
                break;
            case PLAYING:
                Log.d("SimplePlayer", "Playing");
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mScrubber.setEnabled(true);
                mScrubber.setMax((int) pfAsset.getDuration());
                mPlayButton.setText("pause");
                mScrubberMonitorTimer = new Timer();
                final TimerTask task = new TimerTask() {
                    public void run() {
                        if (mUpdateThumb)
                            mScrubber.setProgress((int) pfAsset.getPlaybackTime());
                    }
                };
                mScrubberMonitorTimer.schedule(task, 0, 33);
                break;
            case PAUSED:
                Log.d("SimplePlayer", "Paused");
                mPlayButton.setText("play");
                break;
            case STOPPED:
                Log.d("SimplePlayer", "Stopped");
                mPlayButton.setText("play");
                mScrubberMonitorTimer.cancel();
                mScrubberMonitorTimer = null;
                mScrubber.setProgress(0);
                mScrubber.setEnabled(false);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case COMPLETE:
                Log.d("SimplePlayer", "Complete");
                mPlayButton.setText("play");
                mScrubberMonitorTimer.cancel();
                mScrubberMonitorTimer = null;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case ERROR:
                Log.d("SimplePlayer", "Error");
                break;
        }
    }

    /**
     * Click listener for the play/pause button
     *
     */
    private View.OnClickListener playListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPfasset.getStatus() == PFAssetStatus.PLAYING)
            {
                mPfasset.pause();
                mMediaPlayer.pause();
            }
            else{
                mMediaPlayer.start();
                mPfasset.play();
            }
        }
    };

    /**
     * Click listener for the stop/back button
     *
     */
    private View.OnClickListener stopListener = new View.OnClickListener() {
        public void onClick(View v) {
            mPfasset.stop();
        }
    };

    /**
     * Click listener for the navigation mode (touch/motion (if available))
     *
     */
    private View.OnClickListener touchListener = new View.OnClickListener() {
        public void onClick(View v) {
//			if (mPfview != null)
//			{

            Button touchButton = (Button)findViewById(R.id.touchbutton);
            if (mCurrentNavigationMode == PFNavigationMode.TOUCH)
            {
                mCurrentNavigationMode = PFNavigationMode.MOTION;
                touchButton.setText("motion");
            }
            else
            {
                mCurrentNavigationMode = PFNavigationMode.TOUCH;
                touchButton.setText("touch");
            }
            mPfview.setNavigationMode(mCurrentNavigationMode);
        }
//		}
    };

    /**
     * Called when pausing the app.
     * This function pauses the playback of the asset when it is playing.
     *
     */
    public void onPause() {
        super.onPause();
        if (mPfasset != null)
        {
            if (mPfasset.getStatus() == PFAssetStatus.PLAYING)
                mPfasset.pause();
        }
    }




    public void  onStartCommand(Intent intent, int flags, int startId) {
        mPfasset.play();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDisplay(mVidHolder);
            mMediaPlayer.setDataSource("https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4");
            mMediaPlayer.prepare();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            startAndPlay();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mMediaPlayer.stop();
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }
}
