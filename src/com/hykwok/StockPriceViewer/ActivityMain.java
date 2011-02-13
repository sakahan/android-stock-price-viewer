/*
	Copyright 2010 - 2011 Kwok Ho Yin

   	Licensed under the Apache License, Version 2.0 (the "License");
   	you may not use this file except in compliance with the License.
   	You may obtain a copy of the License at

    	http://www.apache.org/licenses/LICENSE-2.0

   	Unless required by applicable law or agreed to in writing, software
   	distributed under the License is distributed on an "AS IS" BASIS,
   	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   	See the License for the specific language governing permissions and
   	limitations under the License.
*/

package com.hykwok.StockPriceViewer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class ActivityMain extends Activity {
	// debug tag for LogCat
	private static final String TAG = "SPV:ActivityMain";

	// Intent key for stock detail
	private static final String STOCK_DETAIL_SYMBOL = "stock_symbol";
	private static final String STOCK_DETAIL_NAME = "stock_name";

	// Intent string for broadcasting
	private static final String ACTIVITY_TO_SERVICE_BROADCAST = "com.hykwok.action.SPV_A_TO_S_BROADCAST";
	private static final String SERVICE_TO_ACTIVITY_BROADCAST = "com.hykwok.action.SPV_S_TO_A_BROADCAST";

	// Intent key for broadcasting
	private static final String BROADCAST_KEY_ROAMING_OPT = "roaming";
	private static final String BROADCAST_KEY_LASTUPDATETIME = "lastupdatetime";
	private static final String BROADCAST_KEY_TYPE = "type";
	private static final String BROADCAST_KEY_SYMBOL = "symbol";
	private static final String BROADCAST_KEY_REGION = "region";
	private static final String BROADCAST_KEY_UPDATETIME = "update_interval";

	private static final int STOCKDATA_ADD_NEW = 1;
    private static final int STOCKDATA_CONFUPDATED = 2;
    private static final int STOCKDATA_NEWDATA_UPD = 3;
    private static final int STOCKDATA_NEWALLDATA_UPD = 4;
    private static final int STOCKDATA_NODATA_UPD = 5;

    private static final int MENU_ABOUT = Menu.FIRST;
	private static final int MENU_PREFERENCE = Menu.FIRST + 1;

	private static final int DIALOG_DELETE_SYMBOL = 1;
	private static final int DIALOG_ABOUT = 2;
	private static final int DIALOG_ADD_SYMBOL = 3;

	// Preference keys
	private static final String KEY_ROAMING_OPT = "roaming_option";
	private static final String KEY_UPDATE_INTERVAL = "update_interval";
	private static final String KEY_LASTUPDATETIME = "last_update_time";
	private static final String KEY_BKUPDATE = "background_update";
	private static final String KEY_REGIONSELECTION = "region_selection";

	// backup keys
	
	// Message ID
	private static final int GUI_UPDATE_LISTVIEW = 0x100;
	private static final int GUI_UPDATE_LISTVIEW_FAIL = 0x101;

	// controls	
	private TextView 		m_textview_updatetime;	
	private Button			m_btn_add;
	private ListView		m_listview_stocks;
	private ProgressDialog	m_progress_dialog = null;
	private Dialog			m_addsymbol_dialog = null;

	// region list
	private ArrayList<String> allregions = null;
	private ArrayAdapter<String> region_adapter = null;
	private int m_region_selection = 0;

	// list
	private StockPriceListAdapter	mStockPricelistAdapter;

	// internal variables
	private StockData_DB			m_DB;
	private SharedPreferences		mPrefs;

	// preferences
	private long					m_lastupdatetime;
	private boolean					m_enableRoaming;
	private long					m_updateinterval;
	private boolean					m_enablebkupdate;

	// broadcast receiver
	private BroadcastReceiver 		my_intent_receiver;

	// variables
	private String					m_selected_symbol = "";
	private boolean                 m_service_started = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate >>>>>");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        try {
        	// get controls
        	m_textview_updatetime = (TextView)findViewById(R.id.TextViewLastUpdate);
        	m_btn_add = (Button)findViewById(R.id.ButtonAdd);
        	m_listview_stocks = (ListView)findViewById(R.id.ListViewStock);

        	// database
        	m_DB = new StockData_DB(this);

        	// get preferences
	        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	        
	        // setup default preferences
	        boolean updateflag = false;
	        SharedPreferences.Editor editor = mPrefs.edit();
	        
	        if(mPrefs.contains(KEY_ROAMING_OPT) == false) {
	        	editor.putBoolean(KEY_ROAMING_OPT, false);
	        	updateflag = true;
	        }        
	        if(mPrefs.contains(KEY_UPDATE_INTERVAL) == false) {
	        	editor.putString(KEY_UPDATE_INTERVAL, "15");
	        	updateflag = true;
	        }        
	        if(mPrefs.contains(KEY_BKUPDATE) == false) {
	        	editor.putBoolean(KEY_BKUPDATE, false);
	        	updateflag = true;
	        }
	        
	        if(mPrefs.contains(KEY_REGIONSELECTION) == false) {
	        	editor.putInt(KEY_REGIONSELECTION, 0);
	        	updateflag = true;
	        }
	        
	        if(updateflag) {
	        	editor.commit();
	        }

        	// set regions
        	allregions = new ArrayList<String>();
        	for(int i=0; i<StockData_DB.mRegions.length; i++) {
        		allregions.add(StockData_DB.mRegions[i]);
        	}

        	//if(savedInstanceState != null) {
	        //}

        	// set listeners
        	m_btn_add.setOnClickListener(clickListener_btnadd);

        	m_listview_stocks.setOnItemLongClickListener(longclickListener_listview);
        	m_listview_stocks.setOnItemClickListener(clickListener_listview);
        	mStockPricelistAdapter = new StockPriceListAdapter(this, m_DB.GetAllData());
        	m_listview_stocks.setAdapter(mStockPricelistAdapter);

        	region_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, allregions);
        	region_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        	// register broadcast receiver
        	IntentFilter filter = new IntentFilter(SERVICE_TO_ACTIVITY_BROADCAST);
        	my_intent_receiver = new Broadcast_Receiver();
        	registerReceiver(my_intent_receiver, filter);
        } catch (Exception e) {
        	Log.e(TAG, "onCreate: " + e.toString());
        }

        Log.d(TAG, "onCreate <<<<<");
    }

    @Override
    public void onStart() {
    	Log.d(TAG, "onStart >>>>>");

    	super.onStart();

    	try {
    		// check service options
			Intent i = new Intent(this, StockDataService.class);

			m_lastupdatetime = mPrefs.getLong(KEY_LASTUPDATETIME, 0);
    		m_enableRoaming = mPrefs.getBoolean(KEY_ROAMING_OPT, false);
    		String szupdatetime = mPrefs.getString(KEY_UPDATE_INTERVAL, "15");
    		m_updateinterval = Long.parseLong(szupdatetime);
    		m_enablebkupdate = mPrefs.getBoolean(KEY_BKUPDATE, true);
    		m_region_selection = mPrefs.getInt(KEY_REGIONSELECTION, 0);

    		String sztime = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(new Date(m_lastupdatetime));
    		m_textview_updatetime.setText(sztime);

    		i.putExtra(BROADCAST_KEY_ROAMING_OPT, m_enableRoaming);
    		i.putExtra(BROADCAST_KEY_LASTUPDATETIME, m_lastupdatetime);
    		i.putExtra(BROADCAST_KEY_UPDATETIME, m_updateinterval);

    		if(m_enablebkupdate) {
    			this.startService(i);
    			m_service_started = true;
    		} else {
    			this.stopService(i);
    			m_service_started = false;
    		}
    	} catch (Exception e) {
    		Log.e(TAG, "Error:" + e.toString());
    	}

    	Log.d(TAG, "onStart <<<<<");
    }

    @Override
    public void onRestart() {
    	Log.d(TAG, "onRestart >>>>>");

    	super.onRestart();

    	Log.d(TAG, "onRestart <<<<<");
    }

    @Override
    public void onResume() {
    	Log.d(TAG, "onResume >>>>>");

    	super.onResume();

    	Log.d(TAG, "onResume <<<<<");
    }

    @Override
    public void onPause() {
    	Log.d(TAG, "onPause >>>>>");

    	super.onPause();

    	Log.d(TAG, "onPause <<<<<");
    }

    @Override
    public void onStop() {
    	Log.d(TAG, "onStop >>>>>");

    	super.onStop();

    	Log.d(TAG, "onStop <<<<<");
    }

    @Override
    public void onDestroy() {
    	Log.d(TAG, "onDestroy >>>>>");

    	super.onDestroy();

    	try {
    		mStockPricelistAdapter.freeResources();
    		mStockPricelistAdapter = null;

	    	// remove broadcast receiver
			unregisterReceiver(my_intent_receiver);

			// save preference
			SavePreferences();

	    	if(m_DB != null) {
	    		m_DB.CloseDB();
	    		m_DB = null;
	    	}
    	} catch (Exception e) {
    		Log.e(TAG, "onDestroy" + e.toString());
    	}

    	Log.d(TAG, "onDestroy <<<<<");
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
    	Log.d(TAG, "onRetainNonConfigurationInstance >>>>>");

    	Log.d(TAG, "onRetainNonConfigurationInstance <<<<<");

		return null;

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	Log.d(TAG, "onSaveInstanceState >>>>>");

    	Log.d(TAG, "onSaveInstanceState <<<<<");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuItem menu_item;

    	super.onCreateOptionsMenu(menu);
    	// create menu
    	menu_item = menu.add(0, MENU_PREFERENCE, 0, R.string.szMenu_Preference);
    	menu_item.setIcon(android.R.drawable.ic_menu_preferences);
    	menu_item = menu.add(0, MENU_ABOUT, 1, R.string.szMenu_About);
    	menu_item.setIcon(android.R.drawable.ic_menu_info_details);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
	    	case MENU_ABOUT:
	    		// show about dialog box
	    		showDialog(DIALOG_ABOUT);
	    		break;

	    	case MENU_PREFERENCE:
	    		// launch CurrencyPreference activity
	    		Intent i = new Intent(this, StockPricePreferences.class);
	    		startActivity(i);
	    		break;

	    	default:
	    		break;
    	}

    	return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog;

    	switch(id) {
    		case DIALOG_DELETE_SYMBOL:
    			dialog = createDeleteSymbolDialog(this);
    			String title = "Symbol: " + m_selected_symbol;
    			dialog.setTitle(title);
    			return dialog;

    		case DIALOG_ABOUT:
    			return new AboutDialog(this);
    			
    		case DIALOG_ADD_SYMBOL:
    			dialog = createAddSymbolDialog(this);
    			return dialog; 

    		default:
    			break;
    	}

    	return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

    	switch(id) {
    		case DIALOG_DELETE_SYMBOL:
    			String title = "Symbol: " + m_selected_symbol;
    			dialog.setTitle(title);
    			break;
    			
    		case DIALOG_ADD_SYMBOL:
    			EditText edittext_symbol = (EditText)dialog.findViewById(R.id.EditText_AddSymSymbol);
    			Spinner spinner_region = (Spinner)dialog.findViewById(R.id.Spinner_AddSymRegion);
    			
    			edittext_symbol.setSingleLine();
    			edittext_symbol.setText("");
    			spinner_region.setAdapter(region_adapter);
    			spinner_region.setSelection(m_region_selection);
    			m_addsymbol_dialog = dialog;
    			break;

    		default:
    			break;
    	}
    }
    
    // ListView control
    private OnItemLongClickListener longclickListener_listview = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			m_selected_symbol = mStockPricelistAdapter.getSymbol(position);
			showDialog(DIALOG_DELETE_SYMBOL);
			return false;
		}
    };

    private OnItemClickListener clickListener_listview = new OnItemClickListener() {
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    		Intent intent = new Intent(ActivityMain.this, ActivityStockDetail.class);

    		String symbol = mStockPricelistAdapter.getSymbol(position);
    		intent.putExtra(STOCK_DETAIL_SYMBOL, symbol);
    		String name = mStockPricelistAdapter.getName(position);
    		intent.putExtra(STOCK_DETAIL_NAME, name);

			startActivity(intent);
    	}
    };

    // Button control
    private OnClickListener clickListener_btnadd = new OnClickListener() {

		@Override
		public void onClick(View view) {
			showDialog(DIALOG_ADD_SYMBOL);
		}
    };

    private String ConvertToSymbol(String symbol, String region) {    	
    	String result = "";

    	try {
    		if(region.contentEquals("HK")) {
    			// For Hong Kong securities, the symbol format is xxxx.hk.
    			// For example, HSBC is 0005.HK
    			result = String.format("%04d.HK", Integer.parseInt(symbol));
    		} else {
    			result = symbol;
    		}
    	} catch (Exception e) {
    		Log.e(TAG, "ConvertToSymbol:" + e.toString());
    	}

    	return result;
    }

    // create delete symbol dialog box
    private Dialog createDeleteSymbolDialog(Context context) {
    	Log.d(TAG, "----- createDeleteSymbolDialog -----");

    	AlertDialog.Builder builder = new AlertDialog.Builder(context);

    	builder.setMessage(R.string.Dialog_Delete_Symbol_Message);

    	// OK button
    	builder.setPositiveButton(context.getText(R.string.Dialog_Delete_Symbol_OK),
    	new DialogInterface.OnClickListener() {

    		// handle OK button click
			public void onClick(DialogInterface dialog, int which) {
				m_DB.DeleteStockData(m_selected_symbol);
				m_selected_symbol = "";
				mStockPricelistAdapter.UpdateInternalData();
				mStockPricelistAdapter.notifyDataSetChanged();
			}
    	});

    	// Cancel button
    	builder.setNegativeButton(context.getText(R.string.Dialog_Delete_Symbol_Cancel),
    	new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// nothing to do for cancel button
			}
    	});

    	return builder.create();
    }
    
    // create add symbol dialog box
    private Dialog createAddSymbolDialog(Context context) {
    	Log.d(TAG, "----- createAddSymbolDialog -----");

    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	
    	LayoutInflater inflater = LayoutInflater.from(context);
    	final View addsymbolView = inflater.inflate(R.layout.addsymbol_dialog, null);
    	builder.setView(addsymbolView);
    	builder.setTitle(R.string.Dialog_Add_Symbol_Title);

    	// OK button
    	builder.setPositiveButton(context.getText(R.string.Dialog_Add_Symbol_OK),
    	new DialogInterface.OnClickListener() {

    		// handle OK button click
			public void onClick(DialogInterface dialog, int which) {
				EditText edittext_symbol = (EditText)m_addsymbol_dialog.findViewById(R.id.EditText_AddSymSymbol);
    			Spinner spinner_region = (Spinner)m_addsymbol_dialog.findViewById(R.id.Spinner_AddSymRegion);
    			
    			m_region_selection = spinner_region.getSelectedItemPosition();
    			
    			String region = StockData_DB.mRegions[m_region_selection];
				String input_symbol = ConvertToSymbol(edittext_symbol.getText().toString(), region);				
				
				// save selected region
				SharedPreferences.Editor editor = mPrefs.edit();
	        	editor.putInt(KEY_REGIONSELECTION, m_region_selection);
	        	editor.commit();

				if(input_symbol.contentEquals("") == false) {
					if(m_DB.IsSymbolExist(input_symbol) == false) {
						Log.d(TAG, "Input symbol=" + input_symbol + " region=" + region);
						sendSettingToService(STOCKDATA_ADD_NEW, input_symbol, region);

						final CharSequence title = getString(R.string.Dialog_Progress_Title);
						final CharSequence message = getString(R.string.Dialog_Progress_Message);

						m_progress_dialog = ProgressDialog.show(ActivityMain.this, title, message, true);
					}
				}
			}
    	});

    	// Cancel button
    	builder.setNegativeButton(context.getText(R.string.Dialog_Add_Symbol_Cancel),
    	new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Spinner spinner_region = (Spinner)m_addsymbol_dialog.findViewById(R.id.Spinner_AddSymRegion);
    			m_region_selection = spinner_region.getSelectedItemPosition();
    			// save selected region
				SharedPreferences.Editor editor = mPrefs.edit();		        
	        	editor.putInt(KEY_REGIONSELECTION, m_region_selection);
	        	editor.commit();
			}
    	});

    	return builder.create();
    }

    // send data to service
    void sendSettingToService(final int type, final String symbol, final String region) {
    	long delaytime = 10;
    	
    	// has to start service to get data
		Intent i = new Intent(this, StockDataService.class);
		
    	if(m_service_started == false) {
    		i.putExtra(BROADCAST_KEY_ROAMING_OPT, m_enableRoaming);
    		i.putExtra(BROADCAST_KEY_LASTUPDATETIME, m_lastupdatetime);
    		i.putExtra(BROADCAST_KEY_UPDATETIME, m_updateinterval);
    		
    		this.startService(i);
    		
    	    m_service_started = true;
    	    delaytime = 1000;
    	}
    	
    	// just a delay loop
	    Handler handler = new Handler();
	    
	    handler.postDelayed(new Runnable() { 
	         public void run() {
	        	Intent	intent = new Intent(ACTIVITY_TO_SERVICE_BROADCAST);

	     		intent.putExtra(BROADCAST_KEY_TYPE, type);

	     		switch(type) {
	     		case STOCKDATA_ADD_NEW:
	     			intent.putExtra(BROADCAST_KEY_SYMBOL, symbol);
	     			intent.putExtra(BROADCAST_KEY_REGION, region);
	     			break;

	     		case STOCKDATA_CONFUPDATED:
	     			intent.putExtra(BROADCAST_KEY_ROAMING_OPT, m_enableRoaming);
	     			intent.putExtra(BROADCAST_KEY_LASTUPDATETIME, m_lastupdatetime);
	     			break;
	     		}

	     		Log.d(TAG, "send data to service >>>>>");
	     		sendBroadcast(intent);
	         }
	    }, delaytime);		
	}

    // receive data from service
    public class Broadcast_Receiver extends BroadcastReceiver {
    	int type;

		@Override
		public void onReceive(Context context, Intent intent) {
			// receive intent from service
			Log.d(TAG, "receive data from service >>>>>");

			try {
				type = intent.getExtras().getInt(BROADCAST_KEY_TYPE);

				switch(type) {
				case STOCKDATA_NEWDATA_UPD:
				case STOCKDATA_NEWALLDATA_UPD:
					m_lastupdatetime = intent.getExtras().getLong(BROADCAST_KEY_LASTUPDATETIME, 0);

					// send message to activity
					Message msg = Message.obtain(ActivityMain.this.objHandler, GUI_UPDATE_LISTVIEW, type, 0);
	    			ActivityMain.this.objHandler.sendMessage(msg);
					break;

				case STOCKDATA_NODATA_UPD:
					// send message to activity
	    			ActivityMain.this.objHandler.sendEmptyMessage(GUI_UPDATE_LISTVIEW_FAIL);
					break;
				}
			} catch (Exception e) {
				Log.e(TAG, "Broadcast_Receiver:" + e.toString());
			}

			if(m_enablebkupdate == false) {
				// stop the service
				Intent i = new Intent(ActivityMain.this, StockDataService.class);
				ActivityMain.this.stopService(i);
				m_service_started = false;
			}
		}
	}

    // receive message from other threads
    private Handler objHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what) {
    			case GUI_UPDATE_LISTVIEW:
    				// refresh list view
    				Log.d(TAG, "----- refresh listview -----");
    				// update stock price list
    				mStockPricelistAdapter.UpdateInternalData();
    				mStockPricelistAdapter.notifyDataSetChanged();

					// update last update time message
					String sztime = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(new Date(m_lastupdatetime));
					m_textview_updatetime.setText(sztime);

					if(m_progress_dialog != null) {
						if(msg.arg1 == STOCKDATA_NEWDATA_UPD) {
							Toast.makeText(ActivityMain.this, R.string.Toast_Add_Symbol_Ok, Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(ActivityMain.this, R.string.Toast_Add_Symbol_Fail, Toast.LENGTH_LONG).show();
						}
					}
    				break;

    			case GUI_UPDATE_LISTVIEW_FAIL:
    				if(m_progress_dialog != null) {
	    				Toast.makeText(ActivityMain.this, R.string.Toast_Add_Symbol_Fail, Toast.LENGTH_LONG).show();
					}
    				break;
    		}

    		if(m_progress_dialog != null) {
				m_progress_dialog.dismiss();
				m_progress_dialog = null;
			}

    		super.handleMessage(msg);
    	}
    };

    void SavePreferences() {
    	try {
	    	SharedPreferences.Editor editor = mPrefs.edit();

	    	editor.putLong(KEY_LASTUPDATETIME, m_lastupdatetime);
	    	editor.putBoolean(KEY_BKUPDATE, m_enablebkupdate);

	    	editor.commit();
    	} catch (Exception e) {
    		Log.e(TAG, "SavePreferences: " + e.toString());
    	}
    }
}