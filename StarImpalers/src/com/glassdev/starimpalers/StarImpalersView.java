package com.glassdev.starimpalers;

import java.util.ArrayList;
import java.util.List;

import com.glassdev.starimpalers.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.View;

/**
 * This is the primary view for Star Impalers.
 * It creates and draws the graphics required
 * by the game.
 * 
 * @author Matthew Scarpino
 * @version 0.1
 * 
 */
public class StarImpalersView extends View  {

  // The initial delay between impaler movement
  private static final int START_DELAY = 512;

  // The number of impalers and their arrangement
  private static final int NUM_COLS = 7;
  private static final int NUM_ROWS = 3;

  // The game has three states: PLAY (normal),
  // DEATH (a player touches an impaler), 
  // and NEWLEVEL (a player kills all the impalers)
  private enum GameState {PLAY, DEATH, NEWLEVEL};

  // Each impaler has a position, a row/column location,
  // and a field that identifies if it's still alive
  public class Impaler {
    float x, y;
    int row;
  };
  private List<Impaler> mImpalers;

  // Overall game variables
  private int mScore = 0;
  private int mLevel = 1;
  private float mWidth = 640, mHeight = 360;  
  private Paint mPaint = new Paint();
  private Paint mFirePaint = new Paint();
  private GameState mState = GameState.PLAY;
  private String mScoreString = "000000";
  private float mGroundSize = 20.0f;
  private Handler mHandler = new Handler();

  // Impaler-related variables
  private float mImpWidth, mImpHeight;
  private float mImpHSpace, mImpVSpace;
  private float mImpVStep, mImpHStep;
  private Bitmap mBat1, mBat2;
  private long mImpDelay;
  private boolean mSwitchBat, mMoveDown;

  // Shooter-related variables
  private float mShooterX, mShooterY, mShooterWidth;
  private Bitmap mShooter, mSmallShooter;
  private int mNumLives = 3;

  // Missile-related variables
  private boolean mMissileFired = false;
  private float mMissileX, mMissileY;
  private long mMissileDelay = 100;

  /**
   * This Runnable manages the impalers. It checks if 
   * they've hit a wall, hit a missile, or hit the shooter.
   */
  private Runnable mImpRunnable = new Runnable() {
    public void run() {  

      if(mState != GameState.PLAY) return;

      mMoveDown = false;
      for(Impaler imp: mImpalers) {
        if((imp.x + mImpWidth + mImpHStep >= mWidth) ||
           (imp.x + mImpHStep <= 0)) {
          mImpHStep *= -1;
          mMoveDown = true;
          mImpDelay = 3*mImpDelay/4;
          break;
        }
      }

      if(mMoveDown) {
        for(Impaler imp: mImpalers) {
          imp.y += mImpVStep;
        }
      }
      else {
        for(Impaler imp: mImpalers) {
          imp.x += mImpHStep;
        }
      }

        // Check for intersection with shooter
      for(Impaler imp: mImpalers) {      
        if((mShooterX >= imp.x) && 
            (mShooterX <= (imp.x + mImpWidth)) && 
            (mShooterY + 10.0f >= imp.y) && 
            (mShooterY + 10.0f <= (imp.y + mImpHeight))
            || (imp.y > mHeight - 5.0f)) {
          mState = GameState.DEATH;
          invalidate();
          return;
        }
      }

      // Switch bitmaps and redraw the view
      mSwitchBat = !mSwitchBat;    
      invalidate();
      mHandler.postDelayed(this, mImpDelay);
    }
  };

  /**
   * This Runnable manages the missile. It checks if 
   * the missile hits an impaler or leaves the view.
   * 
   * If an impaler is hit, the runnable updates the matrix
   * and checks to see if the level is over.
   */
  private Runnable mMissileRunnable = new Runnable() {
    public void run() {

      if(mState != GameState.PLAY) return;      

      if(mMissileY - 25.0f < 0.0f) {
        mMissileFired = false;
      }
      else {
        mMissileY -= 25.0f;

        // Respond if an impaler was hit
        for(Impaler imp: mImpalers) {
          if((mMissileX >= imp.x) && (mMissileX <= imp.x + mImpWidth) && 
              (mMissileY >= imp.y) && (mMissileY <= (imp.y + mImpHeight))) {

            mMissileFired = false;

            // Update score
            mScore += (imp.row + 1) * 10 * mLevel;
            mScoreString = String.format("%06d", mScore);

            // Update column - check for end of level
            mImpalers.remove(imp);
            if(mImpalers.isEmpty()) {
              mState = GameState.NEWLEVEL;
              invalidate();
              return;
            }

            invalidate();              
            break;
          }  
        }
      }

      if(mMissileFired) {
        invalidate();
        mHandler.postDelayed(this, mMissileDelay);
      }
    }
  };

  /**
   * This Runnable is called at the start of every level.
   * It creates impalers and places them into position.
   * 
   * If the game is over, this Runnable ends the Activity.
   */  
  private Runnable mStartRunnable = new Runnable() {
    
    Impaler imp;
    
    public void run() {

      if(mNumLives > 0) {
        mState = GameState.PLAY;
        mSwitchBat = true;
        mImpDelay = START_DELAY - 100 * (mLevel - 1);
        mMissileFired = false;
        mShooterX = mWidth/2;

        // Allocate and initialize impalers
        mImpalers.clear();
        for(int i=0; i<NUM_ROWS; i++) {
          for(int j=0; j<NUM_COLS; j++) {
            imp = new Impaler();
            imp.row = i;
            imp.x = j * (mImpHSpace + mImpWidth);
            imp.y = 50.0f + i * (mImpVSpace + mImpHeight);
            mImpalers.add(imp);
          }
        }
        mImpRunnable.run();
      }
      else {
        ((Activity)getContext()).stopService(new Intent(getContext(), StarImpalersService.class));
        ((Activity)getContext()).finish();
      }
    }
  };  

  /**
   * This constructor creates the Paints that draw the view
   * and the Bitmaps for the impalers and shooter. Then it
   * starts the impalers' motion.
   */    
  public StarImpalersView(Context context) {
    super(context);
    requestFocus();

    // Configure paints
    mPaint.setColor(0xffc6e2ff);
    mPaint.setStrokeWidth(20.0f);
    mPaint.setTextSize(25.0f);
    mPaint.setTypeface(Typeface.DEFAULT_BOLD);

    mFirePaint.setColor(0xffffffff);
    mFirePaint.setStrokeWidth(5.0f);

    // Create array of impalers
    mImpalers = new ArrayList<Impaler>(NUM_ROWS * NUM_COLS);    

    // Access the bitmap for the impalers
    mBat1 = BitmapFactory.decodeResource(getResources(), R.drawable.bat);
    mBat2 = BitmapFactory.decodeResource(getResources(), R.drawable.bat2);
    mImpWidth = mBat1.getWidth();
    mImpHeight = mBat1.getHeight();

    // Access the bitmap for the shooter
    mShooter = BitmapFactory.decodeResource(getResources(), R.drawable.shooter);
    mSmallShooter = BitmapFactory.decodeResource(getResources(), R.drawable.small_shooter);
    mShooterWidth = mShooter.getWidth();
    mShooterX = mWidth/2 - mShooterWidth/2;
    mShooterY = mHeight - mShooter.getHeight() - mGroundSize;

    // Set dimensions according to density
    float density = getResources().getDisplayMetrics().density;
    mImpHSpace = density * 8;
    mImpVSpace = density * 12;
    mImpHStep = density * 12;
    mImpVStep = density * 16;

    // Start the impalers moving
    mStartRunnable.run();
  }

  /**
   * If the size of the view changes, this method updates the 
   * mWidth and mHeight variables.
   */    
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mWidth = (float)w;
    mHeight = (float)h;
  }

  /**
   * This method receives scroll events from the 
   * Activity and moves the shooter accordingly.
   */ 
  public void moveShooter(float d, float v) {

    if(mState == GameState.PLAY) {
      mShooterX += d * mWidth/5200;
      if(mShooterX < 0.0f) {
        mShooterX = 0.0f;
      }
      else if(mShooterX > (mWidth - mShooterWidth)) {
        mShooterX = mWidth - mShooterWidth;
      }
      invalidate();
    }
  }

  /**
   * This method receives tap events from the 
   * Activity and fires the missile.
   */   
  public void fireMissile() {
    if(!mMissileFired) {
      mMissileFired = true;
      mMissileX = mShooterX + mShooterWidth/2;
      mMissileY = mShooterY + 10.0f;
      mMissileRunnable.run();
    }
  }

  /**
   * This method draws graphics on the View's canvas.
   */ 
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    switch(mState) {

    case PLAY:

      // Draw the impalers
      for(Impaler imp: mImpalers) {
        if(mSwitchBat) {
          canvas.drawBitmap(mBat1, imp.x, imp.y, mPaint);
        }
        else {
          canvas.drawBitmap(mBat2, imp.x, imp.y, mPaint);
        }
      }

      // Draw the shooter
      canvas.drawBitmap(mShooter, mShooterX, mShooterY, mPaint);

      // Draw the ground
      canvas.drawLine(0.0f, mHeight - mGroundSize, mWidth, mHeight - mGroundSize, mPaint);

      // Draw the missile
      if(mMissileFired) {
        canvas.drawLine(mMissileX, mMissileY, mMissileX, mMissileY - 20.0f, mFirePaint);
      }

      // Draw the number of lives (minus 1)
      for(int i=1; i<mNumLives; i++) {
        canvas.drawBitmap(mSmallShooter, mWidth - 30 - 40*i, 10, mPaint);
      }

      // Draw the score
      canvas.drawText(mScoreString, 30.0f, 25.0f, mPaint);
      break;

    case DEATH:
      mNumLives--;
      canvas.drawColor(0xff000000);

      if(mNumLives == 2)
        canvas.drawText("TWO LIVES REMAINING", 2*mWidth/7, mHeight/2, mPaint);
      else if(mNumLives == 1)
        canvas.drawText("ONE LIFE REMAINING", 2*mWidth/7, mHeight/2, mPaint);
      else
        canvas.drawText("GAME OVER", 2*mWidth/5, mHeight/2, mPaint);

      mHandler.postDelayed(mStartRunnable, 1000);
      break;

    case NEWLEVEL:
      mLevel++;
      canvas.drawColor(0xff000000);
      canvas.drawText("LEVEL " + mLevel, 3*mWidth/7, mHeight/2, mPaint);
      mHandler.postDelayed(mStartRunnable, 1000);
      break;
    }
  }
}

