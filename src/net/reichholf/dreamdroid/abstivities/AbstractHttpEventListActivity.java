package net.reichholf.dreamdroid.abstivities;

import net.reichholf.dreamdroid.DreamDroid;
import net.reichholf.dreamdroid.R;
import net.reichholf.dreamdroid.activities.TimerEditActivity;
import net.reichholf.dreamdroid.helpers.ExtendedHashMap;
import net.reichholf.dreamdroid.helpers.enigma2.Event;
import net.reichholf.dreamdroid.helpers.enigma2.SimpleResult;
import net.reichholf.dreamdroid.helpers.enigma2.Timer;
import net.reichholf.dreamdroid.helpers.enigma2.requesthandler.impl.TimerAddByEventIdRequestHandler;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author sreichholf
 * 
 */
public abstract class AbstractHttpEventListActivity extends AbstractHttpListActivity {
	public static final int DIALOG_EPG_ITEM_ID = 9382893;

	protected String mReference;
	protected String mName;

	protected ProgressDialog mProgress;
	protected ExtendedHashMap mCurrentItem;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		final Dialog dialog;
		
		if(mCurrentItem != null){
		
			switch (id) {
			case DIALOG_EPG_ITEM_ID:
	
				String servicename = mCurrentItem.getString(Event.SERVICE_NAME);
				String title = mCurrentItem.getString(Event.EVENT_TITLE);
				String date = mCurrentItem.getString(Event.EVENT_START_READABLE);
				if (!"N/A".equals(title) && date != null) {
					date = date.concat(" (" + (String) mCurrentItem.getString(Event.EVENT_DURATION_READABLE) + " "
							+ getText(R.string.minutes_short) + ")");
					String descEx = mCurrentItem.getString(Event.EVENT_DESCRIPTION_EXTENDED);
	
					dialog = new Dialog(this);
					dialog.setContentView(R.layout.epg_item_dialog);
					dialog.setTitle(title);
	
					TextView textServiceName = (TextView) dialog.findViewById(R.id.service_name);
					textServiceName.setText(servicename);
	
					TextView textTime = (TextView) dialog.findViewById(R.id.epg_time);
					textTime.setText(date);
	
					TextView textDescEx = (TextView) dialog.findViewById(R.id.epg_description_extended);
					textDescEx.setText(descEx);
	
					Button buttonSetTimer = (Button) dialog.findViewById(R.id.ButtonSetTimer);
					buttonSetTimer.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							setTimerById(mCurrentItem);
							dialog.dismiss();
						}
					});
	
					Button buttonEditTimer = (Button) dialog.findViewById(R.id.ButtonEditTimer);
					buttonEditTimer.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							setTimerByEventData(mCurrentItem);
							dialog.dismiss();
						}
					});
					
					Button buttonIMDb = (Button) dialog.findViewById(R.id.ButtonImdb);
					buttonIMDb.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							callImdb(mCurrentItem);
							dialog.dismiss();
						}
					});
				} else {
					// No EPG Information is available!
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.no_epg_available).setCancelable(true)
							.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.cancel();
								}
							});
					dialog = builder.create();
				}
				break;
			default:
				dialog = super.onCreateDialog(id);
			}
		} else {
			dialog = null;
			showToast(getString(R.string.error));
		}
		return dialog;
	}
	
	/**
	 * @param event
	 */
	protected void callImdb(ExtendedHashMap event){
		Intent intent = new Intent(Intent.ACTION_VIEW);
		String uriString = "imdb:///find?q=" + event.getString(Event.EVENT_TITLE);
		intent.setData(Uri.parse(uriString));
		try{			
			startActivity(intent);
		} catch(ActivityNotFoundException anfex) {
			uriString = "http://www.imdb.com/find?q=" + event.getString(Event.EVENT_TITLE);
			intent.setData(Uri.parse(uriString));
			startActivity(intent);
		}
	}
	
	/**
	 * @param event
	 */
	protected void setTimerById(ExtendedHashMap event) {
		if (mProgress != null) {
			if (mProgress.isShowing()) {
				mProgress.dismiss();
			}
		}

		mProgress = ProgressDialog.show(this, "", getText(R.string.saving), true);
		execSimpleResultTask(new TimerAddByEventIdRequestHandler(), Timer.getEventIdParams(event));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.reichholf.dreamdroid.abstivities.AbstractHttpListActivity#onSimpleResult
	 * (boolean, net.reichholf.dreamdroid.helpers.ExtendedHashMap)
	 */
	protected void onSimpleResult(boolean success, ExtendedHashMap result) {
		if (mProgress != null) {
			if (mProgress.isShowing()) {
				mProgress.dismiss();
			}
		}
		super.onSimpleResult(success, result);
	}

	/**
	 * @param event
	 */
	protected void setTimerByEventData(ExtendedHashMap event) {
		ExtendedHashMap timer = Timer.createByEvent(event);
		ExtendedHashMap data = new ExtendedHashMap();
		data.put("timer", timer);

		Intent intent = new Intent(this, TimerEditActivity.class);
		intent.putExtra(sData, data);
		intent.setAction(DreamDroid.ACTION_NEW);

		this.startActivity(intent);
	}

	/**
	 * @param result
	 */
	protected void onTimerSet(ExtendedHashMap result) {
		mProgress.dismiss();

		String toastText = (String) getText(R.string.get_content_error);

		String stateText = result.getString(SimpleResult.STATE_TEXT);

		if (stateText != null && !"".equals(stateText)) {
			toastText = stateText;
		}

		Toast toast = Toast.makeText(this, toastText, Toast.LENGTH_LONG);
		toast.show();
	}

}
