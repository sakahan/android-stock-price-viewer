/*
	Copyright 2010 Kwok Ho Yin

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

public class ActivityMain extends Activity {
	// debug tag for LogCat
	private static final String TAG = "SPV:ActivityMain";
	
	// Intent string for broadcasting
	private static final String ACTIVITY_TO_SERVICE_BROADCAST = "com.hykwok.action.SPV_A_TO_S_BROADCAST";
	private static final String SERVICE_TO_ACTIVITY_BROADCAST = "com.hykwok.action.SPV_S_TO_A_BROADCAST";
	
	// Intent key for broadcasting
	private static final String BROADCAST_KEY_ROAMING_OPT = "roaming";
	private static final String BROADCAST_KEY_LASTUPDATETIME = "lastupdatetime";
	private static final String BROADCAST_KEY_TYPE = "type";
	private static final String BROADCAST_KEY_SYMBOL = "symbol";
	private static final String BROADCAST_KEY_UPDATETIME = "update_interval";
	
	private static final int STOCKDATA_ADD_NEW = 1;
    private static final int STOCKDATA_CONFUPDATED = 2;
    private static final int STOCKDATA_NEWDATA_UPD = 3;
    private static final int STOCKDATA_NODATA_UPD = 4;
	
    private static final int MENU_ABOUT = Menu.FIRST;
	private static final int MENU_PREFERENCE = Menu.FIRST + 1;
	
	private static final int DIALOG_DELETE_SYMBOL = 1;
	private static final int DIALOG_ABOUT = 2;
	
	// Preference keys
	private static final String KEY_ROAMING_OPT = "roaming_option";
	private static final String KEY_UPDATE_INTERVAL = "update_interval";
	private static final String KEY_LASTUPDATETIME = "last_update_time";
	private static final String KEY_BKUPDATE = "background_update";
	
	// backup keys
	private static final String KEY_BK_EDITTEXT = "backup_edittext";
	
	// Message ID
	private static final int GUI_UPDATE_LISTVIEW = 0x100;
	private static final int GUI_UPDATE_LISTVIEW_FAIL = 0x101;
	
	// controls
	private EditText 		m_edittext_symbol;
	private TextView 		m_textview_updatetime;
	private Spinner			m_spinner_region;
	private Button			m_btn_add;
	private ListView		m_listview_stocks;
	private ProgressDialog	m_progress_dialog = null;
	
	// region list
	private ArrayList<String> allregions = null;
	private ArrayAdapter<String> region_adapter = null;
	
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
	private String					m_SavedInstanceText = "";
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate >>>>>");
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        try {        	
        	// get controls
        	m_edittext_symbol = (EditText)findViewById(R.id.EditTextSymbol);
        	m_textview_updatetime = (TextView)findViewById(R.id.TextViewLastUpdate);
        	m_spinner_region = (Spinner)findViewById(R.id.SpinnerRegion);
        	m_btn_add = (Button)findViewById(R.id.ButtonAdd);
        	m_listview_stocks = (ListView)findViewById(R.id.ListViewStock);
        	
        	// database
        	m_DB = new StockData_DB(this);
        	
        	// get preferences
	        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        	
        	// set regions
        	allregions = new ArrayList<String>();
        	for(int i=0; i<StockData_DB.mRegions.length; i++) {
        		allregions.add(StockData_DB.mRegions[i]);
        	}
        	
        	if(savedInstanceState != null) {
	        	// just restore edit text box firstly
	        	String text_symbol = savedInstanceState.getString(KEY_BK_EDITTEXT);
	        	
	        	Log.d(TAG, "text_symbol=" + text_symbol);
	        	
	        	m_SavedInstanceText = text_symbol;
	        	m_edittext_symbol.setText(text_symbol);	        	
	        }
        	
        	// set listers
        	m_edittext_symbol.setSingleLine();
        	m_edittext_symbol.setOnFocusChangeListener(focusListener_Symbol);
        	
        	m_btn_add.setOnClickListener(clickListener_btnadd);
        	
        	m_listview_stocks.setOnItemLongClickListener(longclickListener_listview);
        	mStockPricelistAdapter = new StockPriceListAdapter(this, m_DB.GetAllData());
        	m_listview_stocks.setAdapter(mStockPricelistAdapter);
        	
        	region_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, allregions);
        	region_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);        	
        	m_spinner_region.setAdapter(region_adapter);
        	
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
    		
    		String sztime = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(new Date(m_lastupdatetime));
    		m_textview_updatetime.setText(sztime);
    		    		
    		i.putExtra(BROADCAST_KEY_ROAMING_OPT, m_enableRoaming);
    		i.putExtra(BROADCAST_KEY_LASTUPDATETIME, m_lastupdatetime);
    		i.putExtra(BROADCAST_KEY_UPDATETIME, m_updateinterval);
    		
    		if(m_enablebkupdate) {
    			this.startService(i);
    		} else {
    			this.stopService(i);
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
    	
    	// backup current textview content
    	String txt_symbol = m_edittext_symbol.getText().toString();
    	
    	Log.d(TAG, "txt_symbol=" + txt_symbol);
    	
    	outState.putString(KEY_BK_EDITTEXT, txt_symbol);
    	
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
    			
    		default:
    			break;
    	}
    }
    
    // EditText control
    private OnFocusChangeListener focusListener_Symbol = new OnFocusChangeListener() {
    	int	len;    	
    	String	m_current_input_value = "";

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			m_current_input_value = m_edittext_symbol.getText().toString();
			
			if(hasFocus) {
				Log.d(TAG, "m_edittext_symbol EditText on focus. text=" + m_current_input_value);
				len = m_current_input_value.length();
				if(len > 0) {
					if(m_current_input_value.compareTo(m_SavedInstanceText) == 0) {
						m_SavedInstanceText = "";
						// move the cursor to the end of the text
						m_edittext_symbol.setSelection(len);
					} else {
						m_current_input_value = "";
						m_edittext_symbol.setText(m_current_input_value);
					}
				}				
			} else {
				Log.d(TAG, "m_edittext_symbol EditText loss focus. text=" + m_current_input_value);
				if(m_current_input_value.length() == 0) {					
					m_edittext_symbol.setText("");
				}
			}
		}    	
    };
    
    // ListView control
    private OnItemLongClickListener longclickListener_listview = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			m_selected_symbol = mStockPricelistAdapter.getSymbol(position);
			showDialog(DIALOG_DELETE_SYMBOL);
			return false;
		}    	
    };
    
    // Button control
    private OnClickListener clickListener_btnadd = new OnClickListener() {

		@Override
		public void onClick(View view) {
			String input_symbol = ConvertToSymbol(m_edittext_symbol.getText().toString());
			
			if(input_symbol.contentEquals("") == false) {
				if(m_DB.IsSymbolExist(input_symbol) == false) {
					Log.d(TAG, "Input symbol=" + input_symbol);
					sendSettingToService(STOCKDATA_ADD_NEW, input_symbol);
					
					final CharSequence title = getString(R.string.Dialog_Progress_Title);
					final CharSequence message = getString(R.string.Dialog_Progress_Message);
					
					m_progress_dialog = ProgressDialog.show(ActivityMain.this, title, message, true);
				}
			}
		}
    };
    
    private String ConvertToSymbol(String value) {
    	String szregion = "";
    	String result = "";
    	
    	try {
    		szregion = StockData_DB.mRegions[m_spinner_region.getSelectedItemPosition()];    		
    		result = String.format("%04d.%s", Integer.parseInt(value), szregion);
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
    
    // send data to service
    void sendSettingToService(int type, String symbol) {
    	
    	if(m_enablebkupdate == false) {
    		// has to start service to get data
    		Intent i = new Intent(this, StockDataService.class);
    		    		
    		i.putExtra(BROADCAST_KEY_ROAMING_OPT, m_enableRoaming);
    		i.putExtra(BROADCAST_KEY_LASTUPDATETIME, m_lastupdatetime);
    		i.putExtra(BROADCAST_KEY_UPDATETIME, m_updateinterval);
    		
   			this.startService(i);
    	}
    	
		Intent	intent = new Intent(ACTIVITY_TO_SERVICE_BROADCAST);
		
		intent.putExtra(BROADCAST_KEY_TYPE, type);
		
		switch(type) {
		case STOCKDATA_ADD_NEW:
			intent.putExtra(BROADCAST_KEY_SYMBOL, symbol);
			break;
			
		case STOCKDATA_CONFUPDATED:
			intent.putExtra(BROADCAST_KEY_ROAMING_OPT, m_enableRoaming);
			intent.putExtra(BROADCAST_KEY_LASTUPDATETIME, m_lastupdatetime);
			break;
		}		
		
		Log.d(TAG, "send data to service >>>>>");
		sendBroadcast(intent);
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
					m_lastupdatetime = intent.getExtras().getLong(BROADCAST_KEY_LASTUPDATETIME, 0);
					
					// send message to activity
	    			ActivityMain.this.objHandler.sendEmptyMessage(GUI_UPDATE_LISTVIEW);
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
	    				Toast.makeText(ActivityMain.this, R.string.Toast_Add_Symbol_Ok, Toast.LENGTH_LONG).show();
	    				m_edittext_symbol.setText("");	
					}
    				break;
    				
    			case GUI_UPDATE_LISTVIEW_FAIL:
    				if(m_progress_dialog != null) {
	    				Toast.makeText(ActivityMain.this, R.string.Toast_Add_Symbol_Fail, Toast.LENGTH_LONG).show();
	    				m_edittext_symbol.setText("");
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