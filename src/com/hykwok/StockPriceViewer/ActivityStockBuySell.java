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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ActivityStockBuySell extends Activity {
	// debug tag for LogCat
	private static final String TAG = "SPV:ActivityStockBuySell";
	
	// preference keys
	private static final String KEY_BROKERAGE_FEE = "Brokerage_Fee_Rate";
	private static final String KEY_BROKERAGE_FEE_MIN = "Brokerage_Fee_Min";
	private static final String KEY_DEPOSIT_CHARGE = "Deposit_Charge";
	private static final String KEY_DEPOSIT_CHARGE_MIN = "Deposit_Charge_Min";
	private static final String KEY_DEPOSIT_CHARGE_MAX = "Deposit_Charge_Max";
	
	// backup keys
	private static final String KEY_BK_SYMBOL = "backup_symbol";
	private static final String KEY_BK_NAME = "backup_name";
	private static final String KEY_BK_BUY = "backup_buy";
	private static final String KEY_BK_SELL = "backup_sell";
	private static final String KEY_BK_VOL = "backup_volume";
	private static final String KEY_BK_OTHERS = "backup_others";
	private static final String KEY_BK_LOTSIZE = "backup_lotsize";
	private static final String KEY_BK_RESULT = "backup_result";
	
	private static final String KEY_CAL_SYMBOL = "cal_symbol";
    private static final String KEY_CAL_NAME = "cal_name";
    private static final String KEY_CAL_BUY = "cal_buy";
    private static final String KEY_CAL_SELL = "cal_sell";
	
	// controls
	private EditText edit_buyprice = null;
	private EditText edit_sellprice = null;
	private EditText edit_volume = null;
	private EditText edit_others = null;
	private EditText edit_lotsize = null;
	private TextView textview_title = null; 
	private Button btn_cal = null;
	private TextView textview_result = null;
	
	private String m_symbol = null;
	private String m_name = null;
	private double m_buy_price = 0;
	private double m_sell_price = 0;
	private double m_other_charges = 0;
	private int m_total_shares = 0;
	private int m_lot_size = 1;
	private String m_result_txt = "";
	
	private static final int MENU_PREFERENCE = Menu.FIRST + 2;
	
	private SharedPreferences		mPrefs;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate >>>>>");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.stockbuysell_hk);
        
        // get preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // setup default preferences
        boolean updateflag = false;
        SharedPreferences.Editor editor = mPrefs.edit();
        
        if(mPrefs.contains(KEY_BROKERAGE_FEE) == false) {
        	editor.putString(KEY_BROKERAGE_FEE, "0.25");
        	updateflag = true;
        }        
        if(mPrefs.contains(KEY_BROKERAGE_FEE_MIN) == false) {
        	editor.putString(KEY_BROKERAGE_FEE_MIN, "100");
        	updateflag = true;
        }        
        if(mPrefs.contains(KEY_DEPOSIT_CHARGE) == false) {
        	editor.putString(KEY_DEPOSIT_CHARGE, "5");
        	updateflag = true;
        }        
        if(mPrefs.contains(KEY_DEPOSIT_CHARGE_MIN) == false) {
        	editor.putString(KEY_DEPOSIT_CHARGE_MIN, "30");
        	updateflag = true;
        }        
        if(mPrefs.contains(KEY_DEPOSIT_CHARGE_MAX) == false) {
        	editor.putString(KEY_DEPOSIT_CHARGE_MAX, "188");
        	updateflag = true;
        }
        
        if(updateflag) {
        	editor.commit();
        }
        
        // controls
        edit_buyprice = (EditText)findViewById(R.id.EditText_BuyPrice);
        edit_sellprice = (EditText)findViewById(R.id.EditText_SellPrice);
        edit_volume = (EditText)findViewById(R.id.EditText_TotalShares);
        edit_others = (EditText)findViewById(R.id.EditText_OtherPrice);
        edit_lotsize = (EditText)findViewById(R.id.EditText_LotSize);
        textview_title = (TextView)findViewById(R.id.TextView_SymbolName); 
        btn_cal = (Button)findViewById(R.id.Button_Cal);
        textview_result = (TextView)findViewById(R.id.TextView_CalResult);
        
        btn_cal.setOnClickListener(clickListener_btnadd);
        
        // set input type for edit text controls
        edit_buyprice.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_sellprice.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_volume.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_others.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_lotsize.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        // when users press enter key, close soft keyboard automatically
        edit_buyprice.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edit_sellprice.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edit_volume.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edit_others.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edit_lotsize.setImeOptions(EditorInfo.IME_ACTION_DONE);
        
        if(savedInstanceState != null) {
        	m_symbol = savedInstanceState.getString(KEY_BK_SYMBOL);
        	m_name = savedInstanceState.getString(KEY_BK_NAME);
        	m_buy_price = savedInstanceState.getDouble(KEY_BK_BUY);
        	m_sell_price = savedInstanceState.getDouble(KEY_BK_SELL);
        	m_total_shares = savedInstanceState.getInt(KEY_BK_VOL);
        	m_other_charges = savedInstanceState.getDouble(KEY_BK_OTHERS);
        	m_lot_size = savedInstanceState.getInt(KEY_BK_LOTSIZE);
        	m_result_txt = savedInstanceState.getString(KEY_BK_RESULT);
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
    	
    	Bundle bundle = getIntent().getExtras();
    	
    	String s = bundle.getString(KEY_CAL_SYMBOL);
    	
    	if((m_symbol == null) || (m_symbol.contentEquals(s) == false)) {
    		m_symbol = s;
    		m_name = bundle.getString(KEY_CAL_NAME);
    		m_buy_price = bundle.getDouble(KEY_CAL_BUY);
        	m_sell_price = bundle.getDouble(KEY_CAL_SELL);
    		m_total_shares = 0;
        	m_other_charges = 0;
        	m_lot_size = 1;
        	m_result_txt = "";
    	}
    	
    	UpdateView();
    	
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
    	
    	outState.putString(KEY_BK_SYMBOL, m_symbol);
    	outState.putString(KEY_BK_NAME, m_name);
    	outState.putDouble(KEY_BK_BUY, m_buy_price);
    	outState.putDouble(KEY_BK_SELL, m_sell_price);
    	outState.putInt(KEY_BK_VOL, m_total_shares);
    	outState.putDouble(KEY_BK_OTHERS, m_other_charges);
    	outState.putInt(KEY_BK_LOTSIZE, m_lot_size);
    	outState.putString(KEY_BK_RESULT, m_result_txt);

    	Log.d(TAG, "onSaveInstanceState <<<<<");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuItem menu_item;

    	super.onCreateOptionsMenu(menu);
    	// create menu
    	menu_item = menu.add(0, MENU_PREFERENCE, 0, R.string.szMenu_Preference);
    	menu_item.setIcon(android.R.drawable.ic_menu_preferences);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
	    	case MENU_PREFERENCE:
	    		// launch CurrencyPreference activity
	    		Intent i = new Intent(this, StockBuySellPreferences.class);
	    		startActivity(i);
	    		break;

	    	default:
	    		break;
    	}

    	return super.onOptionsItemSelected(item);
    }
    
    // Button control
    private OnClickListener clickListener_btnadd = new OnClickListener() {

		@Override
		public void onClick(View view) {
			try {
				m_buy_price = Double.parseDouble(edit_buyprice.getText().toString());
				m_sell_price = Double.parseDouble(edit_sellprice.getText().toString());
				m_other_charges = Double.parseDouble(edit_others.getText().toString());
				m_lot_size = Integer.parseInt(edit_lotsize.getText().toString());
				m_total_shares = Integer.parseInt(edit_volume.getText().toString());
				
				double total_buy_price = m_buy_price * m_total_shares;
				double total_sell_price = m_sell_price * m_total_shares;
				
				double buy_tax = cal_stamp_duty(total_buy_price);
				double buy_fee = cal_trading_fee(total_buy_price);
				buy_fee += cal_brokerage_fee(total_buy_price);
				double charge = cal_deposit_charge(m_total_shares, m_lot_size);
				
				double sell_tax = cal_stamp_duty(total_sell_price);
				double sell_fee = cal_trading_fee(total_sell_price);
				sell_fee += cal_brokerage_fee(total_sell_price);
				
				double total_buy = total_buy_price + buy_tax + buy_fee + charge + m_other_charges;
				double total_sell = total_sell_price - buy_tax - buy_fee;
				
				// prepare results
				m_result_txt = "";
				m_result_txt += "Buy:<br />";
				m_result_txt += "Shares: ";
				m_result_txt += Double.toString(total_buy_price);
				m_result_txt += "<br />";
				m_result_txt += "Fee: ";
				m_result_txt += Double.toString(buy_fee);
				m_result_txt += "<br />";
				m_result_txt += "Tax: ";
				m_result_txt += Double.toString(buy_tax);
				m_result_txt += "<br />";
				m_result_txt += "Deposit Charge: ";
				m_result_txt += Double.toString(charge);
				m_result_txt += "<br />";
				m_result_txt += "Total (Paid): ";
				m_result_txt += Double.toString(total_buy);
				m_result_txt += "<br />";
				m_result_txt += "Sell:<br />";
				m_result_txt += "Shares: ";
				m_result_txt += Double.toString(total_sell_price);
				m_result_txt += "<br />";
				m_result_txt += "Fee: ";
				m_result_txt += Double.toString(sell_fee);
				m_result_txt += "<br />";
				m_result_txt += "Tax: ";
				m_result_txt += Double.toString(sell_tax);
				m_result_txt += "<br />";
				m_result_txt += "Total (Get): ";
				m_result_txt += Double.toString(total_sell);
				m_result_txt += "<br />";
				m_result_txt += "Earn/Loss: ";
				m_result_txt += Double.toString(total_sell - total_buy);
				m_result_txt += "<br />";
			} catch (Exception e) {
				Log.e(TAG, "Invalid input values for calculation");
				e.printStackTrace();
				m_result_txt = "Error occurs.<br />Please check input values or the preference settings.";
			}
			
			UpdateView();
		}
    };
    
    void UpdateView() {
    	textview_title.setText(m_name + "   (" + m_symbol + ")");
    	edit_buyprice.setText(Double.toString(m_buy_price));
    	edit_sellprice.setText(Double.toString(m_sell_price));
    	edit_others.setText(Double.toString(m_other_charges));
    	edit_lotsize.setText(Integer.toString(m_lot_size));
    	edit_volume.setText(Integer.toString(m_total_shares));
    	textview_result.setText(android.text.Html.fromHtml(m_result_txt));
    }
    
    double cal_stamp_duty(double total_price) {    	
    	double duty = Math.floor(total_price / 1000.0);
    	
    	if((duty * 1000) < total_price) {
    		duty++;
    	}
    	
    	return duty;
    }
    
    double cal_trading_fee(double total_price) {
    	double	fee = 0;
    	
    	// trading fee for SFC
    	fee += total_price * 0.003 / 100.0;
    	
    	// trading fee for HKEX
    	fee += total_price * 0.005 / 100.0;
    	
    	return fee;
    }
    
    double cal_brokerage_fee(double total_price) {
    	double rate = Double.parseDouble(mPrefs.getString(KEY_BROKERAGE_FEE, "0.25"));
    	double min_limit = Double.parseDouble(mPrefs.getString(KEY_BROKERAGE_FEE_MIN, "100"));
    	
    	double fee = total_price * rate / 100.0;
    	
    	if(fee < min_limit) {
    		fee = min_limit;
    	}
    	
    	return fee;
    }
    
    double cal_deposit_charge(int total_shares, int lot_size) {
    	double lot_price = Double.parseDouble(mPrefs.getString(KEY_DEPOSIT_CHARGE, "5"));
    	double min_charge = Double.parseDouble(mPrefs.getString(KEY_DEPOSIT_CHARGE_MIN, "30"));
    	double max_charge = Double.parseDouble(mPrefs.getString(KEY_DEPOSIT_CHARGE_MAX, "188"));
    	int	lot = total_shares / lot_size;
    	
    	if((total_shares % lot_size) > 0) {
    		lot++;
    	}
    	
    	double charge = lot * lot_price;
    	
    	if(charge < min_charge) {
    		charge = min_charge;
    	} else if(charge > max_charge) {
    		charge = max_charge;
    	}
    	
    	return charge;
    }
}
