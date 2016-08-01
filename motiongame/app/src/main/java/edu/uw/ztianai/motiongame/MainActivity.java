package edu.uw.ztianai.motiongame;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private DrawingSurfaceView view;

    private static final String TAG = "Main";

    private GestureDetectorCompat mDetector;

    private float width;
    private float height;
    private float extraBallRadius = 25;
    private Random r = new Random();

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private SoundPool myPool;
    private int[] sounds;
    private boolean[] soundsBoolean;

    private int extraBallAmount = 30;

    //For accelerometer
    private long lastUpdate = 0;
    private float lastX, lastY, lastZ;
    private static final int SHAKE_THRESHOLD = 600;

    private int barHeight = 40;
    private int initBallRadius = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        view = (DrawingSurfaceView)findViewById(R.id.drawingView);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, mSensorManager.SENSOR_DELAY_NORMAL);


        initializeSoundPool();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        //Allow user to determine how many obstacle balls they want to start with
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            String numBall = extras.getString("edu.uw.ztianai.motiongame.num");
            if(numBall != null && numBall.length() > 0){
                int num = Integer.parseInt(numBall);
                extraBallAmount = num;
            }
        }

        createExtraBall();
    }

    //initializes sounds so that they can be played during the game
    @SuppressWarnings("deprecation")
    private void initializeSoundPool(){
        //Create the SoundPool
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            AudioAttributes track = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            myPool= new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(track)
                    .build();
        }
        else {
            //API < 21
            myPool = new SoundPool(5, AudioManager.STREAM_MUSIC,0);
        }

        sounds = new int[5];
        soundsBoolean = new boolean[5];


        myPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener(){
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if(status == 0){
                    if(sampleId == sounds[0]){
                        soundsBoolean[0] = true;
                    }else if(sampleId == sounds[1]){
                        soundsBoolean[1] = true;
                    }else if(sampleId == sounds[2]){
                        soundsBoolean[2] = true;
                    }else if(sampleId == sounds[3]){
                        soundsBoolean[3] = true;
                    }else if(sampleId == sounds[4]){
                        soundsBoolean[4] = true;
                    }
                }
            }
        });

        sounds[0] = myPool.load(this, R.raw.jump, 1);
        sounds[1] = myPool.load(this, R.raw.win, 1);
        sounds[2] = myPool.load(this, R.raw.start, 1);
        sounds[3] = myPool.load(this, R.raw.ping,1);
        sounds[4] = myPool.load(this, R.raw.plop, 1);

    }

    //helper method to play sound
    public void playSound(int index){
        if(soundsBoolean[index]){
            myPool.play(sounds[index], 1, 1, 1, 0, 1);
        }
    }

    @Override
    protected void onResume() {
        mSensorManager.registerListener(this,mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
        initializeSoundPool(); //recreate my sound pool
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(this, mSensor);
        super.onPause();
        myPool.release(); //release my sound pool when the activity is paused
        myPool = null;
    }

    //Handles user's touch
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean gesture = mDetector.onTouchEvent(event);
        if(gesture) return true;

        int action = MotionEventCompat.getActionMasked(event);
        switch(action) {
            case (MotionEvent.ACTION_DOWN):
            case (MotionEvent.ACTION_MOVE):
            case (MotionEvent.ACTION_UP): //every time user tap on the phone, once they lift their finger, a new obstacle ball will be added
                float extraBallX = r.nextFloat() * width;
                float extraBallY = r.nextFloat() * height;
                while(extraBallX - extraBallRadius < 0 ||
                        extraBallX + extraBallRadius > width ||
                        extraBallY - extraBallRadius < barHeight ||
                        extraBallY + extraBallRadius > height - barHeight){
                    extraBallX = r.nextFloat() * width;
                    extraBallY = r.nextFloat() * height;
                }
                Ball extraBall = new Ball(extraBallX, extraBallY, extraBallRadius);
                view.extraBall.add(extraBall);
                playSound(0);
                return true;
            case (MotionEvent.ACTION_CANCEL) : //aborted gesture
            case (MotionEvent.ACTION_OUTSIDE) : //outside bounds
            default :
                return super.onTouchEvent(event);
        }
    }


    //Keep track of the motion of the phone
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        //Use accelerometer to determine whether user shake their phone or not, if yes, then the game will restart
        if(mySensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            view.ball.dx = x * -5;
            view.ball.dy = y * 5;

            long currentTime = System.currentTimeMillis();
            if((currentTime - lastUpdate) > 100){ //check there is a time difference
                long diffTime = currentTime - lastUpdate;
                lastUpdate = currentTime;

                float speed = Math.abs(x + y + z - lastX - lastY - lastZ)/diffTime * 10000;

                if(speed > SHAKE_THRESHOLD){ //based on the shake threshold to determine whether user shake their phone or not
                    view.ball = new Ball(width/2, height-barHeight-initBallRadius, initBallRadius);
                    view.extraBall.clear();
                    createExtraBall();
                    playSound(2);
                }
                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }

        if(view.hit){ //if the user's ball hit other obstacle ball, the game will restart
            playSound(3);
            view.hit = false;
            view.extraBall.clear();
            createExtraBall();
        }

        if(view.win){ //if the user finish the game, restart the game and show a toast message to the user
            playSound(1);
            view.win = false;
            view.extraBall.clear();
            createExtraBall();
            Toast.makeText(this, "Great Job, YOU WON!!!", Toast.LENGTH_SHORT).show();
        }
    }

    //Helper method to generate extra obstacle ball for the users
    public void createExtraBall(){
        for(int i = 0; i < extraBallAmount; i++){
            float extraBallX = r.nextFloat() * width;
            float extraBallY = r.nextFloat() * height;
            while(extraBallX - extraBallRadius < 0 ||
                    extraBallX + extraBallRadius > width ||
                    extraBallY - extraBallRadius < barHeight ||
                    extraBallY + extraBallRadius > height - barHeight){
                extraBallX = r.nextFloat() * width;
                extraBallY = r.nextFloat() * height;
            }
            Ball extraBall = new Ball(extraBallX, extraBallY, extraBallRadius);
            view.extraBall.add(extraBall);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //Handles gesture inputs
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true; //let others respond as well
        }

        //if the users double tap on the ball they control, they can change the size of the ball, a bigger ball will increase difficulties
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            playSound(4);
            if(view.ball.radius == initBallRadius){
                view.ball.radius = extraBallRadius + 5;
            }else{
                view.ball.radius = initBallRadius;
            }
            return true;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {  //allow user to play full screen
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

}
