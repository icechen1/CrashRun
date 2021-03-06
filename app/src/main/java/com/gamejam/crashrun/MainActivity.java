
package com.gamejam.crashrun;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.gamejam.crashrun.ViewMapFragment.onCameraListener;
import com.gamejam.crashrun.game.Game;
import com.gamejam.crashrun.rest.StepCounter;
import com.google.android.gms.games.Games;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.melnykov.fab.FloatingActionButton;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

@EActivity
public class MainActivity
    extends BaseGameActivity
    implements onCameraListener

{
	/*TODO 
	 * ADD CREDITS FOR GMAPS AND OSM
	 * TODO one point per round?
	 */
	
    public static long a = 300000; //time remaining
    static long orb_value = 60000;

    Game game;
    long tStart;
    double elapsedSeconds;



    public static String TAG = "BathroomFinder";
	
    ViewMapFragment_ mMapFragment;
    Fragment mListFragment;
    TextView timerText;
    TextView roundText;
    public static CountDownTimer cdt;

    int rounds = 0;
	static boolean paused = true;
	private Menu _abs_menu;
	View LL ;
    static boolean DEMO = false;
    private WatchSync watchSync;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _abs_menu = menu;
        getMenuInflater().inflate(R.menu.activity_main, menu);
        LL = LayoutInflater.from(this).inflate(R.layout.text_counters, null);
        getSupportActionBar().setCustomView(LL);
        timerText = (TextView) LL.findViewById(R.id.textTimeronTheActionBar);
        //timerText.setText("00");
        
        roundText = (TextView) LL.findViewById(R.id.textRounds);
       // roundText.setText("00");

        getSupportActionBar().setDisplayShowCustomEnabled(true);
        
        //Workaround enable location layer on map after menu load
      	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
		if (mMapFragment.mMap != null) {
      	mMapFragment.mMap.setMyLocationEnabled(true);
		}
        _abs_menu.findItem(R.id.sign_out).setVisible(false);
        _abs_menu.findItem(R.id.sign_in).setVisible(true);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.credits) {
        startActivity(new Intent(this, LegalNoticesActivity.class));

        return(true);
      }else if(item.getItemId() == R.id.satellite){
    	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
  		mMapFragment.changeView(ViewMapFragment.MapType.Satellite);
    	  
      }else if(item.getItemId() == R.id.hybrid){
      	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
    		mMapFragment.changeView(ViewMapFragment.MapType.Hybrid);
      }else if(item.getItemId() == R.id.map_only){
      	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
    		mMapFragment.changeView(ViewMapFragment.MapType.Map);
      }else if(item.getItemId() == R.id.terrain){
      	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
    		mMapFragment.changeView(ViewMapFragment.MapType.Terrain);
      }else if(item.getItemId() == R.id.menu_quit){
        	finish();
        	
      }else if(item.getItemId() == R.id.help){
          showSimplePopUp(this.getString(R.string.help1), this.getString(R.string.help_text));
      }else if(item.getItemId() == R.id.share){
    	  Intent s = new Intent(android.content.Intent.ACTION_SEND);

          s.setType("text/plain");
          s.putExtra(Intent.EXTRA_SUBJECT, "I just ran " + rounds + " rounds in CrashCourse!");
          s.putExtra(Intent.EXTRA_TEXT, "How many can you do? Get the game at http://globalgamejam.org/2013/crashcourse");

          startActivity(Intent.createChooser(s, "Quote"));

      }else if(item.getItemId() == R.id.demo_mode){
    	  if(DEMO == false) {
    		  DEMO = true;
    		  }
    	  else{
    		  DEMO = false;
    	  }
      }else if(item.getItemId() == R.id.sign_in){
          // start the asynchronous sign in flow
          mHelper.mGoogleApiClient.connect();
      }
      else if(item.getItemId() == R.id.sign_out){
          // sign out.
          Games.signOut(mHelper.mGoogleApiClient);
          _abs_menu.findItem(R.id.sign_out).setVisible(false);
          _abs_menu.findItem(R.id.sign_in).setVisible(true);
      }
      return super.onOptionsItemSelected(item);
    }

    public void gameToggle(final View v){

        final View myView = findViewById(R.id.card_view);
        final View shade = findViewById(R.id.shade);

        if(paused)
        {
            game = new Game();
            mMapFragment.make(game);
            mMapFragment.startGame();
            tStart = System.currentTimeMillis();

            SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putLong("steps", 0);
            editor.commit();

            startService(new Intent( this, StepCounter.class));



            cdt = null;
            Countdown();
            paused  = false;
            roundText = (TextView) LL.findViewById(R.id.textRounds);
            roundText.setText("Round " + rounds);
            ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
            mMapFragment.checkForNearbyItems();
            ((FloatingActionButton)v).setImageDrawable(getResources().getDrawable(R.drawable.ic_action_navigation_close));

            //TRANSITION ANIMATION!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //only with api>=21
                // previously visible view
                // get the center for the clipping circle
                int cx = (myView.getLeft() + myView.getRight()) / 2;
                int cy = (myView.getTop() + myView.getBottom()) / 2;
                int shadex = (shade.getLeft() + shade.getRight()) / 2;
                int shadey = (shade.getTop() + shade.getBottom()) / 2;
                // get the initial radius for the clipping circle
                int initialRadius = myView.getWidth();
                int shadeRadius = shade.getWidth();
                // create the animation (the final radius is zero)
                Animator anim =
                        ViewAnimationUtils.createCircularReveal(myView, cx, cy, initialRadius, 0);
                final Animator shadeanim =
                        ViewAnimationUtils.createCircularReveal(shade, shadex, shadey, shadeRadius, 0);
                // make the view invisible when the animation is done
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        myView.setVisibility(View.INVISIBLE);
                    }
                });
                // make the view invisible when the animation is done
                shadeanim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        shade.setVisibility(View.INVISIBLE);
                    }
                });
                // start the animation
                anim.start();
                shadeanim.start();
            }else{
                //legacy behavior
                myView.setVisibility(View.INVISIBLE);
                shade.setVisibility(View.INVISIBLE);
            }
        } else {
            //Show confirmation dialog

            long tEnd = System.currentTimeMillis();
            long tDelta = tEnd - tStart;
            elapsedSeconds = tDelta / 1000.0;

            SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
            long stepsTaken=  pref.getLong("steps", 0);
            double distTravelled = stepsTaken*1.75;
            double averageSpeed = distTravelled*1.0/elapsedSeconds;



            double[] stats = game.stats(distTravelled, stepsTaken, averageSpeed);
            double dist = stats[0];
            double steps = stats[1];
            double speed = stats[2];


            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Stop game?");



            //alertDialog.setIcon(R.drawable.icon);
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    //Okay, then
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //only with api>=21
                        // previously invisible view
                        // get the center for the clipping circle
                        int cx = (myView.getLeft() + myView.getRight()) / 2;
                        int cy = (myView.getTop() + myView.getBottom()) / 2;
                        int shadex = (shade.getLeft() + shade.getRight()) / 2;
                        int shadey = (shade.getTop() + shade.getBottom()) / 2;
                        // get the final radius for the clipping circle
                        int finalRadius = Math.max(myView.getWidth(), myView.getHeight());
                        int shadefinalRadius = Math.max(shade.getWidth(), shade.getHeight());
                        // create the animator for this view (the start radius is zero)
                        Animator anim =
                                ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);
                        Animator shadeanim =
                                ViewAnimationUtils.createCircularReveal(shade, shadex, shadey, 0, shadefinalRadius);
                        // make the view visible and start the animation
                        myView.setVisibility(View.VISIBLE);
                        shade.setVisibility(View.VISIBLE);
                        anim.start();
                        shadeanim.start();
                    } else {
                        myView.setVisibility(View.VISIBLE);
                        shade.setVisibility(View.VISIBLE);
                    }

                    cdt.cancel();
                    mMapFragment.stopGame();
                    paused = true;
                    roundText = (TextView) LL.findViewById(R.id.textRounds);
                    roundText.setText("Game Stopped");
                    ((FloatingActionButton) v).setImageDrawable(getResources().getDrawable(R.drawable.ic_action_av_play_arrow));


                    stopService(new Intent(getApplicationContext(), StepCounter.class));

                    long tEnd = System.currentTimeMillis();
                    long tDelta = tEnd - tStart;
                    elapsedSeconds = tDelta / 1000.0;


                    SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);

                    long stepsTaken = pref.getLong("steps", 0);
                    double distTravelled = stepsTaken * 1.75;
                    double averageSpeed = distTravelled * 1.0 / elapsedSeconds;

                    game.stats(distTravelled, stepsTaken, averageSpeed);

                    Log.d("steps taken", String.valueOf(stepsTaken));

                    SharedPreferences.Editor editor = pref.edit();
                    editor.putLong("steps", 0);
                    editor.commit();
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing but close the dialog
                }
            });
            // Remember, create doesn't show the dialog
            alertDialog.show();

        }
    }
    public void showSimplePopUp(String title, String text) {

      	 AlertDialog alertDialog = new AlertDialog.Builder(this).create();
      	 alertDialog.setTitle(title);
      	 alertDialog.setMessage(text);
       //alertDialog.setIcon(R.drawable.icon);
      	 alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
      	  
      		 public void onClick(DialogInterface dialog, int which) {
      	     // Do nothing but close the dialog
      	    }
      	   });

      	 // Remember, create doesn't show the dialog
      	alertDialog.show();
      	}
    @Override
    public void onResume(){
        super.onResume();
    }
    @Override
    protected void onStart() {
        super.onStart();
        watchSync = WatchSync.newInstance(this);
        watchSync.onStart();

    }
    @Override
    protected void onStop() {
        super.onStop();
        watchSync.onStop();

    }
    //Do not exit the game if it is in progress! Let's overwrite the back button
    @Override
    public void onBackPressed() {
        if(!paused) gameToggle(findViewById(R.id.fab));
        else super.onBackPressed();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUND‌​S);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.primary));
        }

        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        if(savedInstanceState != null){
        //restore instances here
        }
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setContentView(R.layout.activity_main);
    	FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		mMapFragment = (ViewMapFragment_) getSupportFragmentManager().findFragmentByTag("map");

		if (mMapFragment == null) {
			// If not, instantiate and add it to the activity
            mMapFragment = new ViewMapFragment_();

			ft.add(R.id.containerFrag, mMapFragment, "map").commit();
		} else {
			// If it exists, simply attach it in order to show it
			ft.show(mMapFragment).commit();
		}
		//timerText = (TextView) findViewById(R.id.timerText);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState){
    //save instances here
    super.onSaveInstanceState(outState);
    }

	@Override
	public void onCameraLocationChange(LatLng loc) {
      //  setProgressBarIndeterminateVisibility(true); 
		//updatePOIs(loc);
	}

	@UiThread
	public void stopProgressbar() {
	  //  setProgressBarIndeterminateVisibility(false); 
		
	}

	@Override
	public void onMyLocationChange(Location location) {
		Log.d(TAG, "onMyLocationChange()");

	}

	@UiThread
	public void Countdown()
	{		
		if(cdt == null)
		{
			cdt = new CountDownTimer(a, 1000) {


				public void onTick(long millisUntilFinished) 
				{
					long s = 0;
					long m = 0;
                    game.scoreAdd(-1);
					//timerText.setText("" + millisUntilFinished / 1000);
					a = millisUntilFinished;
					//Log.d(TAG, "&" + millisUntilFinished);
					m = millisUntilFinished/60000;
					s = millisUntilFinished/1000 - m * 60;
					String sec = String.valueOf(s);
					if (s < 10){
						sec = "0" + s;
					}
                    if (s<10) {
                        sec = "0"+String.valueOf(s);
                    }
					timerText.setText("" + m + ":" + sec);
					setProgressBarIndeterminateVisibility(true);
                    watchSync.sendUpdate(null,null,null,"" + m + ":" + sec,(byte)0);
				}

				public void onFinish()
				{



                    watchSync.sendUpdate(null,null,null, "00:00",(byte)1);
					timerText.setText("Game over!");
                    //Submit score
                    if (mHelper.mGoogleApiClient != null && mHelper.mGoogleApiClient.isConnected())
                        Games.Leaderboards.submitScore(mHelper.mGoogleApiClient, "CgkI-uCdiKAKEAIQAQ", game.scoreAdd(0));
					setProgressBarIndeterminateVisibility(false);
					
					// Get instance of Vibrator from current Context
					Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					 
					int dot = 300;
					int short_gap = 200;    // Length of Gap Between dots/dashes
					long[] pattern = {
					    0,  // Start immediately
					    dot, short_gap, dot, short_gap, dot
					};
					 
					// Only perform this pattern one time (-1 means "do not repeat")
					v.vibrate(pattern, -1);


                    long tEnd = System.currentTimeMillis();
                    long tDelta = tEnd - tStart;
                    elapsedSeconds = tDelta / 1000.0;


                    game.newGame();
                    stopService(new Intent(getApplicationContext(), StepCounter.class));
                    SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
                    long stepsTaken=  pref.getLong("steps", 0);

                    Log.d("steps taken", String.valueOf(stepsTaken));

                    SharedPreferences.Editor editor = pref.edit();
                    editor.putLong("steps", 0);
                    editor.commit();


                    a = game.getTime();

                    final View myView = findViewById(R.id.card_view);
                    final View shade = findViewById(R.id.shade);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //only with api>=21
                        // previously invisible view
                        // get the center for the clipping circle
                        int cx = (myView.getLeft() + myView.getRight()) / 2;
                        int cy = (myView.getTop() + myView.getBottom()) / 2;
                        int shadex = (shade.getLeft() + shade.getRight()) / 2;
                        int shadey = (shade.getTop() + shade.getBottom()) / 2;
                        // get the final radius for the clipping circle
                        int finalRadius = Math.max(myView.getWidth(), myView.getHeight());
                        int shadefinalRadius = Math.max(shade.getWidth(), shade.getHeight());
                        // create the animator for this view (the start radius is zero)
                        Animator anim =
                                ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);
                        Animator shadeanim =
                                ViewAnimationUtils.createCircularReveal(shade, shadex, shadey, 0, shadefinalRadius);
                        // make the view visible and start the animation
                        myView.setVisibility(View.VISIBLE);
                        shade.setVisibility(View.VISIBLE);
                        anim.start();
                        shadeanim.start();
                    }else{
                        myView.setVisibility(View.VISIBLE);
                        shade.setVisibility(View.VISIBLE);
                    }





                    cdt.cancel();
                    mMapFragment.stopGame();
                    paused = true;
                    roundText = (TextView) LL.findViewById(R.id.textRounds);
                    roundText.setText("Game Stopped");



                    ((FloatingActionButton)findViewById(R.id.fab)).setImageDrawable(getResources().getDrawable(R.drawable.ic_action_av_play_arrow));









                }
			}.start();
		}
	}
    public void leaderboard(View v){
        if (mHelper.mGoogleApiClient != null && mHelper.mGoogleApiClient.isConnected())
        startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mHelper.mGoogleApiClient,
                "CgkI-uCdiKAKEAIQAQ"), 1337);
        else
            // start the asynchronous sign in flow
            mHelper.mGoogleApiClient.connect();
    }

    public void achievements(View v){
        if (mHelper.mGoogleApiClient != null && mHelper.mGoogleApiClient.isConnected())
            startActivityForResult( Games.Achievements.getAchievementsIntent(getApiClient()), 13737);
        else
            // start the asynchronous sign in flow
            mHelper.mGoogleApiClient.connect();
    }
	@Override
	public void onOrbGet(int i)
	{
		Log.d(TAG, ""+ a);
		cdt.cancel();
		cdt = null;
        game.scoreAdd(100);
		Countdown();
		// Get instance of Vibrator from current Context
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		int dot = 300;
		int short_gap = 100;    // Length of Gap Between dots/dashes
		long[] pattern = {
		    0,  // Start immediately
		    dot, short_gap, dot
		};
		// Only perform this pattern one time (-1 means "do not repeat")
		v.vibrate(pattern, -1);

        if (i == 2) {
            a += 60*1000;
        }
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        elapsedSeconds = tDelta / 1000.0;
        long stepsTaken=  pref.getLong("steps", 0);
        double distTravelled = stepsTaken*1.75;
        double averageSpeed = distTravelled*1.0/elapsedSeconds;
        game.stats(distTravelled, stepsTaken, averageSpeed);
	}

	@Override
	public void onNewRound() {
		// TODO Auto-generated method stub
		rounds = game.levelAdd(0);
		Log.d(TAG, "rounds: " + rounds);
        timerText = (TextView) LL.findViewById(R.id.textTimeronTheActionBar);
        roundText = (TextView) LL.findViewById(R.id.textRounds);
		roundText.setText("Round " + rounds);
		timerText.setText("5:00");
        a = game.getTime();
  		Countdown();
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        elapsedSeconds = tDelta / 1000.0;
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        long stepsTaken=  pref.getLong("steps", 0);
        double distTravelled = stepsTaken*1.75;
        double averageSpeed = distTravelled*1.0/elapsedSeconds;
        Log.d("stepstaken", String.valueOf(stepsTaken));
        Log.d("distTravelled", String.valueOf(distTravelled));
        Log.d("averageSpeed", String.valueOf(averageSpeed));
        game.stats(distTravelled, stepsTaken, averageSpeed);

	}

    @Override
    public void onSignInFailed() {

    }

    @Override
    public void onSignInSucceeded() {
        _abs_menu.findItem(R.id.sign_out).setVisible(true);
        _abs_menu.findItem(R.id.sign_in).setVisible(false);
    }
}
