package com.glassdev.starimpalers;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;

/**
 * This is the primary activity for Star Impalers.
 * It creates the View, receives tap/scroll events, 
 * and directs the events to the View.
 * 
 * @author Matthew Scarpino
 * @version 0.1
 * 
 */
public class StarImpalersActivity extends Activity 
  implements GestureDetector.BaseListener, GestureDetector.ScrollListener {

  private GestureDetector mDetector;
  private StarImpalersView mView;
  
  /**
   * Creates the View and GestureDetector
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    mView = new StarImpalersView(this);
    setContentView(mView);
    
    // Creates and configures the GestureDetector
    mDetector = new GestureDetector(this).setBaseListener(this).setScrollListener(this);   
  }
  
  /**
   * Send events to the GestureDetector for processing
   */
  public boolean onGenericMotionEvent(MotionEvent event) {
    return mDetector.onMotionEvent(event);
  }
  
  /**
   * Send tap events to the View
   */
  public boolean onGesture(Gesture g) {
    if(g == Gesture.TAP)
      mView.fireMissile();
    return true;
  }    
  
  /**
   * Send scroll events to the View
   */
  public boolean onScroll(float displacement, float delta, float velocity) {
    mView.moveShooter(displacement, velocity);
    return true;
  }
  
  /**
   * Stop the service when stopped
   */
  @Override
  protected void onStop() {
    super.onStop();
    stopService(new Intent(this, StarImpalersService.class));
    finish(); 
  }
}
