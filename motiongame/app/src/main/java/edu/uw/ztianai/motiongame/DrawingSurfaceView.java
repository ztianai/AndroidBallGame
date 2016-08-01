package edu.uw.ztianai.motiongame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

/**
 * An SurfaceView for generating graphics on
 * @author Tianai Zhao
 */
public class DrawingSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "SurfaceView";

    private int viewWidth, viewHeight; //size of the view

    private Bitmap bmp; //image to draw on

    private SurfaceHolder mHolder; //the holder we're going to post updates to
    private DrawingRunnable mRunnable; //the code that we'll want to run on a background thread
    private Thread mThread; //the background thread

    //drawing variables
    private Paint redPaint;
    private Paint yellowPaint;
    private Paint bluePaint;
    private Paint greenPaint;


    public Ball ball;
    public ArrayList<Ball> extraBall = new ArrayList<Ball>(); //obstacle ball

    public boolean hit; //whether player's ball hit the obstacle ball
    public boolean win; //whether player finish the game

    private int barHeight = 40;
    private int initBallRadius = 20;


    public DrawingSurfaceView(Context context) {
        this(context, null);
    }

    public DrawingSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawingSurfaceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);

        viewWidth = 1; viewHeight = 1; //positive defaults; will be replaced when #surfaceChanged() is called

        // register interest in hearing about changes to the surface
        mHolder = getHolder();
        mHolder.addCallback(this);

        mRunnable = new DrawingRunnable();

        //set up drawing variables
        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setColor(Color.RED);

        yellowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yellowPaint.setColor(Color.YELLOW);

        greenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenPaint.setColor(Color.GREEN);

        bluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bluePaint.setColor(Color.BLUE);


        init();
    }


    //Initiate the starting shapes
    public void init(){
        ball = new Ball(viewWidth/2, viewHeight-barHeight-initBallRadius, initBallRadius);
        Rect start = new Rect(0, viewHeight - barHeight, viewWidth, viewHeight);
        Rect end = new Rect(0, 0, viewWidth, barHeight);
    }

    //Helper method for the "game loop"
    public void update(){
        //update the "game state"
        ball.cx += ball.dx; //move
        ball.cy += ball.dy;


        //hit detection
        if(ball.cx + ball.radius > viewWidth) { //left bound
            ball.cx = viewWidth - ball.radius;
            ball.dx *= -1;
        }
        else if(ball.cx - ball.radius < 0) { //right bound
            ball.cx = ball.radius;
            ball.dx *= -1;
        }
        else if(ball.cy + ball.radius > viewHeight - barHeight) { //bottom bound
            ball.cy = viewHeight - barHeight - ball.radius;
            ball.dy *= -1;
        }
        else if(ball.cy - ball.radius < 0) { //top bound
            ball.cy = ball.radius;
            ball.dy *= -1;
        }

        //Win state check
        if(ball.cy < barHeight){
            win = true;
            ball = new Ball(viewWidth/2, viewHeight, initBallRadius);
        }

        //Hit other obstacle ball check
        for(int i = 0; i < extraBall.size(); i++){
            Ball obstacle = extraBall.get(i);
            if(ball.cx <= obstacle.cx + obstacle.radius && ball.cx >= obstacle.cx - obstacle.radius && ball.cy >= obstacle.cy - obstacle.radius && ball.cy <= obstacle.cy + obstacle.radius){
                hit = true;
                ball = new Ball(viewWidth/2, viewHeight, initBallRadius);
            }
        }
    }


    /**
     * Helper method for the "render loop"
     * @param canvas The canvas to draw on
     */
    public void render(Canvas canvas){
        if(canvas == null) return; //if we didn't get a valid canvas for whatever reason

        canvas.drawColor(Color.BLACK); //black out the background

        canvas.drawCircle(ball.cx, ball.cy, ball.radius, bluePaint); //User's blue ball

        canvas.drawRect(0, viewHeight - barHeight, viewWidth, viewHeight, redPaint); //the start bar at the bottom
        canvas.drawRect(0, 0, viewWidth, barHeight, greenPaint); //the finish bar at the top

        //Draw out the obstacle balls
        for(int i = 0; i < extraBall.size(); i++){
            canvas.drawCircle(extraBall.get(i).cx, extraBall.get(i).cy, extraBall.get(i).radius, yellowPaint);
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //create and start the background updating thread
        Log.d(TAG, "Creating new drawing thread");
        mThread = new Thread(mRunnable);
        mRunnable.setRunning(true); //turn on the runner
        mThread.start(); //start up the thread when surface is created

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        synchronized (mHolder) { //synchronized to keep this stuff atomic
            viewWidth = width;
            viewHeight = height;
            bmp = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888); //new buffer to draw on

            init();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        mRunnable.setRunning(false); //turn off
        boolean retry = true;
        while(retry) {
            try {
                mThread.join();
                retry = false;
            } catch (InterruptedException e) {
                //will try again...
            }
        }
        Log.d(TAG, "Drawing thread shut down");
    }

    /**
     * An inner class representing a runnable that does the drawing. Animation timing could go in here.
     * http://obviam.net/index.php/the-android-game-loop/ has some nice details about using timers to specify animation
     */
    public class DrawingRunnable implements Runnable {

        private boolean isRunning; //whether we're running or not (so we can "stop" the thread)

        public void setRunning(boolean running){
            this.isRunning = running;
        }

        public void run() {
            Canvas canvas;
            while(isRunning)
            {
                canvas = null;
                try {
                    canvas = mHolder.lockCanvas(); //grab the current canvas
                    synchronized (mHolder) {
                        update(); //update the game
                        render(canvas); //redraw the screen
                    }
                }
                finally { //no matter what (even if something goes wrong), make sure to push the drawing so isn't inconsistent
                    if (canvas != null) {
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}