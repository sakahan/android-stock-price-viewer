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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ActivityStockDetail extends Activity {
	// debug tag for LogCat
	private static final String TAG = "SPV:ActivityStockDetail";

	// Intent key for stock detail
	private static final String STOCK_DETAIL_SYMBOL = "stock_symbol";
	private static final String STOCK_DETAIL_NAME = "stock_name";

	// Preference keys
	private static final String KEY_ROAMING_OPT = "roaming_option";

	// Message ID
	private static final int GUI_UPDATE_DATA = 0x200;

	// backup keys
	private static final String KEY_BK_SYMBOL = "backup_symbol";
	private static final String KEY_BK_NAME = "backup_name";
	private static final String KEY_BK_AVG_50D = "backup_avg_50d";
	private static final String KEY_BK_AVG_200D = "backup_avg_200d";
	private static final String KEY_BK_CHANGE = "backup_change";
	private static final String KEY_BK_CHANGE_PERCENT = "backup_change_percent";
	private static final String KEY_BK_DAY_HIGH = "backup_day_high";
	private static final String KEY_BK_DAY_LOW = "backup_day_low";
	private static final String KEY_BK_52W_HIGH = "backup_52w_high";
	private static final String KEY_BK_52W_LOW = "backup_52w_low";
	private static final String KEY_BK_VOLUME = "backup_volume";
	private static final String KEY_BK_PREV_CLOSE = "backup_prev_close";
	private static final String KEY_BK_OPEN = "backup_open";
	private static final String KEY_BK_DATE = "backup_date";
	private static final String KEY_BK_TIME = "backup_time";
	private static final String KEY_BK_PRICE = "backup_price";
	private static final String KEY_BK_PE = "backup_pe";
	
	private static final String KEY_CAL_SYMBOL = "cal_symbol";
    private static final String KEY_CAL_NAME = "cal_name";
    private static final String KEY_CAL_BUY = "cal_buy";
    private static final String KEY_CAL_SELL = "cal_sell";

	// controls
	private Button				m_btn_buysell;
	private TextView 			m_textview_symbolname;
	private TextView 			m_textview_date, m_textview_time;
	private TextView 			m_textview_price;
	private TextView			m_textview_change, m_textview_change_percent;
	private TextView			m_textview_volume;
	private TextView			m_textview_prev_close, m_textview_open;
	private TextView			m_textview_day_high, m_textview_day_low;
	private TextView			m_textview_52w_high, m_textview_52w_low;
	private TextView			m_textview_50d_mov_avg, m_textview_200d_mov_avg;
	private TextView			m_textview_pe;

	private StockDetailData					m_stockdata = null;
	private StockDataProvider_Yahoo			provider = null;

	private boolean ref_roaming = false;
	private StockDataConnection	m_connection;

	private SharedPreferences		mPrefs;

	private String m_given_symbol = "";
	private ProgressDialog m_progress_dialog;

	private Thread parser_thread;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate >>>>>");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.stockdetails);

        // connection
		m_connection = new StockDataConnection(this);

		// create provider
    	provider = new StockDataProvider_Yahoo();

		// get preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // get controls
        m_btn_buysell = (Button)findViewById(R.id.Button_BuySell);
        m_textview_symbolname = (TextView)findViewById(R.id.TextView_SymbolName);
        m_textview_date = (TextView)findViewById(R.id.TextView_LastTradeDate);
        m_textview_time = (TextView)findViewById(R.id.TextView_LastTradeTime);
        m_textview_price = (TextView)findViewById(R.id.TextView_LastTradePrice);
        m_textview_change = (TextView)findViewById(R.id.TextView_Change);
        m_textview_change_percent = (TextView)findViewById(R.id.TextView_ChangePercent);
        m_textview_volume = (TextView)findViewById(R.id.TextView_Volume);
        m_textview_prev_close = (TextView)findViewById(R.id.TextView_PrevClose);
        m_textview_open = (TextView)findViewById(R.id.TextView_Open);
        m_textview_day_high = (TextView)findViewById(R.id.TextView_DayHigh);
        m_textview_day_low = (TextView)findViewById(R.id.TextView_DayLow);
        m_textview_52w_high = (TextView)findViewById(R.id.TextView_52WHigh);
        m_textview_52w_low = (TextView)findViewById(R.id.TextView_52WLow);
        m_textview_50d_mov_avg = (TextView)findViewById(R.id.TextView_50DMovingAvg);
        m_textview_200d_mov_avg = (TextView)findViewById(R.id.TextView_200DMovingAvg);
        m_textview_pe = (TextView)findViewById(R.id.TextView_PE);

        // set listeners
        m_btn_buysell.setOnClickListener(clickListener_btnbuysell);

        // thread for get data
        parser_thread = new Thread(mTask);

        // get backup data
        if(savedInstanceState != null) {
        	Log.d(TAG, "Get backup data.....");

        	m_given_symbol = savedInstanceState.getString(KEY_BK_SYMBOL);

        	m_stockdata = new StockDetailData();

        	m_stockdata.symbol = savedInstanceState.getString(KEY_BK_SYMBOL);
        	m_stockdata.name = savedInstanceState.getString(KEY_BK_NAME);
        	m_stockdata.average_200_day_moving = savedInstanceState.getDouble(KEY_BK_AVG_200D);
        	m_stockdata.average_50_day_moving = savedInstanceState.getDouble(KEY_BK_AVG_50D);
        	m_stockdata.change = savedInstanceState.getDouble(KEY_BK_CHANGE);
        	m_stockdata.change_percent = savedInstanceState.getString(KEY_BK_CHANGE_PERCENT);
        	m_stockdata.day_high = savedInstanceState.getDouble(KEY_BK_DAY_HIGH);
        	m_stockdata.day_low = savedInstanceState.getDouble(KEY_BK_DAY_LOW);
        	m_stockdata.week_52_high = savedInstanceState.getDouble(KEY_BK_52W_HIGH);
        	m_stockdata.week_52_low = savedInstanceState.getDouble(KEY_BK_52W_LOW);
        	m_stockdata.last_trade_date = savedInstanceState.getString(KEY_BK_DATE);
        	m_stockdata.last_trade_time = savedInstanceState.getString(KEY_BK_TIME);
        	m_stockdata.last_trade_price = savedInstanceState.getDouble(KEY_BK_PRICE);
        	m_stockdata.previous_close = savedInstanceState.getDouble(KEY_BK_PREV_CLOSE);
        	m_stockdata.open = savedInstanceState.getDouble(KEY_BK_OPEN);
        	m_stockdata.volume = savedInstanceState.getLong(KEY_BK_VOLUME);
        	m_stockdata.p_e_ratio = savedInstanceState.getDouble(KEY_BK_PE);
        }

        Log.d(TAG, "onCreate <<<<<");
    }

    @Override
    public void onStart() {
    	Log.d(TAG, "onStart >>>>>");

    	super.onStart();

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

    	UpdateData();
    	
    	if(m_given_symbol.contains("HK")) {
    		m_btn_buysell.setEnabled(true);
    	} else {
    		m_btn_buysell.setEnabled(false);
    	}

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

    	Log.d(TAG, "onDestroy <<<<<");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	Log.d(TAG, "onSaveInstanceState >>>>>");
    	
    	if(m_stockdata != null) {
	    	outState.putString(KEY_BK_SYMBOL, m_stockdata.symbol);
	    	outState.putString(KEY_BK_NAME, m_stockdata.name);
	    	outState.putDouble(KEY_BK_AVG_50D, m_stockdata.average_50_day_moving);
	    	outState.putDouble(KEY_BK_AVG_200D, m_stockdata.average_200_day_moving);
	    	outState.putDouble(KEY_BK_CHANGE, m_stockdata.change);
	    	outState.putString(KEY_BK_CHANGE_PERCENT, m_stockdata.change_percent);
	    	outState.putDouble(KEY_BK_DAY_HIGH, m_stockdata.day_high);
	    	outState.putDouble(KEY_BK_DAY_LOW, m_stockdata.day_low);
	    	outState.putDouble(KEY_BK_52W_HIGH, m_stockdata.week_52_high);
	    	outState.putDouble(KEY_BK_52W_LOW, m_stockdata.week_52_low);
	    	outState.putLong(KEY_BK_VOLUME, m_stockdata.volume);
	    	outState.putDouble(KEY_BK_PREV_CLOSE, m_stockdata.previous_close);
	    	outState.putDouble(KEY_BK_OPEN, m_stockdata.open);
	    	outState.putString(KEY_BK_DATE, m_stockdata.last_trade_date);
	    	outState.putString(KEY_BK_TIME, m_stockdata.last_trade_time);
	    	outState.putDouble(KEY_BK_PRICE, m_stockdata.last_trade_price);
	    	outState.putDouble(KEY_BK_PE, m_stockdata.p_e_ratio);
    	}

    	Log.d(TAG, "onSaveInstanceState <<<<<");
    }    
    
    // Button control
    private OnClickListener clickListener_btnbuysell = new OnClickListener() {

		@Override
		public void onClick(View view) {
			// launch CurrencyPreference activity
    		Intent i = new Intent(ActivityStockDetail.this, ActivityStockBuySell.class);
    		
    		i.putExtra(KEY_CAL_SYMBOL, m_stockdata.symbol);
    		i.putExtra(KEY_CAL_NAME, m_stockdata.name);
    		i.putExtra(KEY_CAL_BUY, m_stockdata.last_trade_price);
    		i.putExtra(KEY_CAL_SELL, m_stockdata.last_trade_price);
    		
    		startActivity(i);
		}
    };

    private void ShowData() {
    	if(m_stockdata != null) {
			// update display
			m_textview_date.setText(m_stockdata.last_trade_date);
			m_textview_time.setText(m_stockdata.last_trade_time);
			m_textview_price.setText(Double.toString(m_stockdata.last_trade_price));
			m_textview_change.setText(Double.toString(m_stockdata.change));
			m_textview_change_percent.setText("(" + m_stockdata.change_percent + ")");
			m_textview_volume.setText(Long.toString(m_stockdata.volume));
			m_textview_prev_close.setText(Double.toString(m_stockdata.previous_close));
			m_textview_open.setText(Double.toString(m_stockdata.open));
			m_textview_day_high.setText(Double.toString(m_stockdata.day_high));
			m_textview_day_low.setText(Double.toString(m_stockdata.day_low));
			m_textview_52w_high.setText(Double.toString(m_stockdata.week_52_high));
			m_textview_52w_low.setText(Double.toString(m_stockdata.week_52_low));
			m_textview_50d_mov_avg.setText(Double.toString(m_stockdata.average_50_day_moving));
			m_textview_200d_mov_avg.setText(Double.toString(m_stockdata.average_200_day_moving));
			m_textview_pe.setText(Double.toString(m_stockdata.p_e_ratio));
		} else {
			// clear previous data firstly
			m_textview_date.setText("????");
			m_textview_time.setText("????");
			m_textview_price.setText("????");
			m_textview_change.setText("????");
			m_textview_change_percent.setText("????");
			m_textview_volume.setText("????");
			m_textview_prev_close.setText("????");
			m_textview_open.setText("????");
			m_textview_day_high.setText("????");
			m_textview_day_low.setText("????");
			m_textview_52w_high.setText("????");
			m_textview_52w_low.setText("????");
			m_textview_50d_mov_avg.setText("????");
			m_textview_200d_mov_avg.setText("????");
			m_textview_pe.setText("????");
		}
    }

    private void UpdateData() {

		Bundle bundle = getIntent().getExtras();
		String symbol = bundle.getString(STOCK_DETAIL_SYMBOL);
		String name = bundle.getString(STOCK_DETAIL_NAME);

		// set symbol and name
		m_textview_symbolname.setText(name + "   (" + symbol + ")");

		Log.d(TAG, "Intent symbol is " + symbol);
		Log.d(TAG, "My symbol is " + m_given_symbol);

		if(m_given_symbol.contentEquals(symbol) == false) {
			Log.d(TAG, "New symbol is given.....");

			m_given_symbol = symbol;
			m_stockdata = null;

			parser_thread.start();

			final CharSequence title = getString(R.string.Dialog_Progress_Title);
			final CharSequence message = getString(R.string.Dialog_Progress_Message);

			m_progress_dialog = ProgressDialog.show(ActivityStockDetail.this, title, message, true);
		}

		ShowData();
    }

    // receive message from other threads
    private Handler objHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what) {
    			case GUI_UPDATE_DATA:
    				ShowData();
    				break;
    		}

    		if(m_progress_dialog != null) {
				m_progress_dialog.dismiss();
				m_progress_dialog = null;
			}

    		super.handleMessage(msg);
    	}
    };

    private Runnable mTask = new Runnable() {
    	public void run() {
    		// get data from preference first
    		ref_roaming = mPrefs.getBoolean(KEY_ROAMING_OPT, ref_roaming);

    		m_connection.EnableNetworkRoaming(ref_roaming);

    		boolean flag = true;

    		// get stock detail data
			if(m_connection.IsPhoneAvaiable() == false) {
				if(m_connection.IsWIFIAvailabe() == false) {
					Log.e(TAG, "Connection is not available");
					flag = false;
				}
			}

			if(flag) {
				m_stockdata = provider.startGetDetailDataFromYahoo(m_given_symbol);
			} else {
				m_stockdata = null;
			}

			// send message to update display
			ActivityStockDetail.this.objHandler.sendEmptyMessage(GUI_UPDATE_DATA);
    	}
    };
}
