package com.glassdev.starimpalers;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;

public class StarImpalersService extends Service {

  private static final String TAG = "StarImpalersService";
  private LiveCard mCard;
  private TimelineManager mManager;
  private Intent mIntent;
  private PendingIntent mPending;

  @Override
  public void onCreate() {
    super.onCreate();

    // Create the PendingIntent
    mIntent = new Intent(this, StarImpalersActivity.class);
    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
        Intent.FLAG_ACTIVITY_CLEAR_TASK);
    mPending = PendingIntent.getActivity(this, 0, mIntent, 0);

    // Access timeline manager and create live card
    mManager = TimelineManager.from(this);
    mCard = mManager.createLiveCard(TAG);
    mCard.setAction(mPending);
    mCard.setViews(new RemoteViews(getPackageName(), R.layout.main));
    
    // Insert the card into the timeline
    mCard.publish(PublishMode.REVEAL);
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return null;
  }

  @Override
  public void onDestroy() {
    if(mCard != null) {
      mCard.unpublish();
      mCard = null;
    }
    super.onDestroy();
  }
}