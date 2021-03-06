/* © 2010 Stephan Reichholf <stephan at reichholf dot net>
 * 
 * Licensed under the Create-Commons Attribution-Noncommercial-Share Alike 3.0 Unported
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 */

package net.reichholf.dreamdroid.activities;

import java.util.ArrayList;

import net.reichholf.dreamdroid.DreamDroid;
import net.reichholf.dreamdroid.R;
import net.reichholf.dreamdroid.abstivities.AbstractHttpActivity;
import net.reichholf.dreamdroid.helpers.ExtendedHashMap;
import net.reichholf.dreamdroid.helpers.Python;
import net.reichholf.dreamdroid.helpers.enigma2.Remote;
import net.reichholf.dreamdroid.helpers.enigma2.SimpleResult;
import net.reichholf.dreamdroid.helpers.enigma2.requesthandler.RemoteCommandRequestHandler;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

/**
 * A Virtual dreambox remote control using http-requests to send key-strokes
 * 
 * @author sreichholf
 * 
 */
public class VirtualRemoteActivity extends AbstractHttpActivity {
	public static final int MENU_LAYOUT = 0;

	private Vibrator mVibrator;

	private final int[][] mButtonsCommon = {
		{ R.id.ButtonPower, Remote.KEY_POWER },
		{ R.id.ButtonExit, Remote.KEY_EXIT },
		{ R.id.ButtonVolP, Remote.KEY_VOLP },
		{ R.id.ButtonVolM, Remote.KEY_VOLM },
		{ R.id.ButtonMute, Remote.KEY_MUTE },
		{ R.id.ButtonBouP, Remote.KEY_BOUP },
		{ R.id.ButtonBouM, Remote.KEY_BOUM },
		{ R.id.ButtonUp, Remote.KEY_UP },
		{ R.id.ButtonDown, Remote.KEY_DOWN },
		{ R.id.ButtonLeft, Remote.KEY_LEFT },
		{ R.id.ButtonRight, Remote.KEY_RIGHT },
		{ R.id.ButtonOk, Remote.KEY_OK },
		{ R.id.ButtonInfo, Remote.KEY_INFO },
		{ R.id.ButtonMenu, Remote.KEY_MENU },
		{ R.id.ButtonHelp, Remote.KEY_HELP },
		{ R.id.ButtonPvr, Remote.KEY_PVR },
		{ R.id.ButtonRed, Remote.KEY_RED },
		{ R.id.ButtonGreen, Remote.KEY_GREEN },
		{ R.id.ButtonYellow, Remote.KEY_YELLOW },
		{ R.id.ButtonBlue, Remote.KEY_BLUE }
	};
	
	private final int[][] mButtonsExtended = {
		{ R.id.ButtonRwd, Remote.KEY_REWIND },
		{ R.id.ButtonPlay, Remote.KEY_PLAY },
		{ R.id.ButtonStop, Remote.KEY_STOP },
		{ R.id.ButtonFwd, Remote.KEY_FORWARD },
		{ R.id.ButtonRec, Remote.KEY_RECORD }
	};
	
	private final int[][] mButtonsSimple = {
		{ R.id.ButtonAudio, Remote.KEY_AUDIO }
	};
	
	private final int[][] mButtonsStandard = {
		{ R.id.Button1, Remote.KEY_1 },
		{ R.id.Button2, Remote.KEY_2 },
		{ R.id.Button3, Remote.KEY_3 },
		{ R.id.Button4, Remote.KEY_4 },
		{ R.id.Button5, Remote.KEY_5 },
		{ R.id.Button6, Remote.KEY_6 },
		{ R.id.Button7, Remote.KEY_7 },
		{ R.id.Button8, Remote.KEY_8 },
		{ R.id.Button9, Remote.KEY_9 },
		{ R.id.Button0, Remote.KEY_0 },
		{ R.id.ButtonLeftArrow, Remote.KEY_PREV },
		{ R.id.ButtonRightArrow, Remote.KEY_NEXT },		
		{ R.id.ButtonTv, Remote.KEY_TV },
		{ R.id.ButtonRadio, Remote.KEY_RADIO },
		{ R.id.ButtonText, Remote.KEY_TEXT }		
	};

	private boolean mQuickZap;
	private boolean mSimpleRemote;
	private String mBaseTitle;
	private SharedPreferences mPrefs;
	private SharedPreferences.Editor mEditor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.reichholf.dreamdroid.abstivities.AbstractHttpActivity#onCreate(android
	 * .os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* make window full-screen */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mEditor = mPrefs.edit();
		mQuickZap = mPrefs.getBoolean(DreamDroid.PREFS_KEY_QUICKZAP, false);
		
		mSimpleRemote = DreamDroid.PROFILE.isSimpleRemote();
		
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		reinit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_LAYOUT, 0, getText(R.string.quickzap)).setIcon(
				android.R.drawable.ic_menu_always_landscape_portrait);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(MENU_LAYOUT).setTitle(mQuickZap ? R.string.standard : R.string.quickzap);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_LAYOUT:
			setLayout(!mQuickZap);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	/**
	 * @param b
	 *            if true QuickZap Layout will be applied. False = Standard
	 *            Layout
	 */
	private void setLayout(boolean b) {
		if (mQuickZap != b) {
			mQuickZap = b;
			mEditor.putBoolean(DreamDroid.PREFS_KEY_QUICKZAP, mQuickZap);
			mEditor.commit();

			reinit();
		}
	}

	/**
	 * @param buttonmap
	 *            array of (button view id, command id) to register callbacks for
	 */
	private void registerButtons(int[][] buttonmap) {
		for (int i = 0; i < buttonmap.length; i++) {
			Button btn = (Button)findViewById(buttonmap[i][0]);
			registerOnClickListener(btn, buttonmap[i][1]);
		}
	}

	/**
	 * Apply Gui-Element-Attributes and register OnClickListeners in dependence
	 * of the active layout (Standard or QuickZap)
	 */
	private void reinit() {
		if (mQuickZap) {
			setContentView(R.layout.virtual_remote_quick_zap);
			mBaseTitle = getString(R.string.app_name) + " - " + getString(R.string.quickzap);			
		} else {
			if(mSimpleRemote){
				setContentView(R.layout.virtual_remote_simple);
				registerButtons(mButtonsSimple);
			} else {
				setContentView(R.layout.virtual_remote);
				registerButtons(mButtonsExtended);
			}
			registerButtons(mButtonsStandard);
			mBaseTitle = getString(R.string.app_name) + " - " + getString(R.string.virtual_remote);
		}
		registerButtons(mButtonsCommon);
		setTitle(mBaseTitle);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	/**
	 * Registers an OnClickListener for a specific GUI Element. OnClick the
	 * function <code>onButtonClicked</code> will be called with the given id
	 * 
	 * @param v
	 *            The view to register an OnClickListener for
	 * @param id
	 *            The item ID to register the listener for
	 */
	protected void registerOnClickListener(View v, final int id) {
		v.setLongClickable(true);

		v.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				onButtonClicked(id, true);
				return true;
			}
		});

		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onButtonClicked(id, false);
			}
		});
	}

	/**
	 * Called after a Button has been clicked
	 * 
	 * @param id
	 *            The id of the item
	 * @param longClick
	 *            If true the item has been long-clicked
	 */
	private void onButtonClicked(int id, boolean longClick) {
		int msec = 25;
		if (longClick) {
			msec = 100;
		}

		mVibrator.vibrate(msec);

		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("command", new Integer(id).toString()));
		if(mSimpleRemote){
			params.add(new BasicNameValuePair("rcu", "standard"));
		} else {
			params.add(new BasicNameValuePair("rcu", "advanced"));
		}
		if (longClick) {
			params.add(new BasicNameValuePair("type", Remote.CLICK_TYPE_LONG));
		}
		execSimpleResultTask(new RemoteCommandRequestHandler(), params);
	}
	
	/* (non-Javadoc)
	 * @see net.reichholf.dreamdroid.abstivities.AbstractHttpActivity#onSimpleResult(boolean, net.reichholf.dreamdroid.helpers.ExtendedHashMap)
	 */
	protected void onSimpleResult(boolean success, ExtendedHashMap result) {
		boolean hasError = false;
		String toastText = getString(R.string.get_content_error);					
		String stateText = result.getString(SimpleResult.KEY_STATE_TEXT);
		String state = result.getString(SimpleResult.KEY_STATE);		
			
		if (stateText == null || "".equals(stateText)) {			
			hasError = true;
		} 
		
		if (mShc.hasError()) {
			toastText = toastText + "\n" + mShc.getErrorText();
			hasError = true;
		} else if (Python.FALSE.equals(state)){
			hasError = true;
			toastText = stateText;
		}
		
		if(hasError){
			showToast(toastText);
		}
	}
}
