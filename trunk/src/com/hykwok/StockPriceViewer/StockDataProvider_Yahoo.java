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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class StockDataProvider_Yahoo {

	// debug tag for LogCat
	private static final String TAG = "SPV:StockDataProvider_Yahoo";

	private List<StockData> 	data = new ArrayList<StockData>();
	
	// available region
	private static final String[] mRegions = { "HK", "US" };

	// format URL path (refer to http://www.gummy-stuff.org/Yahoo-data.htm for more information)
	private static final String[] szURL_Server = { "http://hk.finance.yahoo.com", "http://download.finance.yahoo.com" };
	
	private static int m_selected_region = 0;
	
	private static final String szURL_Prefix = "/d/quotes.csv?s=";

	// 4 items that we want to get (normal)
	// symbol + last trade (price only) + change + name
	private static final String[] yahoo_flags = { "s", "l1", "c1", "n" };
	// 17 items that we want to get (detail)
	// symbol + previous close + open + change (price) + change (%) + last trade (price) +
	// last trade (date) + last trade (time) + volume + day's low + day's high +
	// 52 week low + 52 week high + 50 day moving average + 200 day moving average +
	// P/E ratio + name
	private static final String[] yahoo_flags_detail = { "s",  "p",  "o",  "c1", "p2",
														 "l1", "d1", "t1", "v",  "g",
														 "h",  "j",  "k",  "m3", "m4",
														 "r",  "n" };
	
	public void selectRegion(String region) {
		for(int i=0; i<mRegions.length; i++) {
			if(mRegions[i].contentEquals(region)) {
				m_selected_region = i;
				return;
			}
		}
	}

	public StockDetailData startGetDetailDataFromYahoo(String symbols) {
		String[]  decodestring = null;
		String 	  szURL = szURL_Server[m_selected_region] + szURL_Prefix + symbols + "&f=";

		for(String x : yahoo_flags_detail) {
			szURL += x;
		}

		Log.d(TAG, "URL=" + szURL);

		try {
			URL url = new URL(szURL);
			InputStream stream = url.openStream();

			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		    String line = null;
		    StockDetailData s_data = null;

		    while ((line = reader.readLine()) != null) {
		        Log.d(TAG, "Received:" + line);

		        try {
		        	decodestring = line.split(",", yahoo_flags_detail.length);

		        	if(Double.parseDouble(decodestring[5]) > 0) {
		        		s_data = new StockDetailData();

		        		// symbol
		        		s_data.symbol = decodestring[0].replace('"', ' ');
		        		s_data.symbol = s_data.symbol.trim();
		        		// previous close
		        		s_data.previous_close = Double.parseDouble(decodestring[1]);
		        		// open
		        		s_data.open = Double.parseDouble(decodestring[2]);
		        		// change
		        		s_data.change = Double.parseDouble(decodestring[3]);
		        		// change (%)
		        		s_data.change_percent = decodestring[4].replace('"', ' ');
		        		s_data.change_percent = s_data.change_percent.trim();
		        		// last trade (price)
		        		s_data.last_trade_price = Double.parseDouble(decodestring[5]);
		        		// last trade (date)
		        		s_data.last_trade_date = decodestring[6].replace('"', ' ');
		        		s_data.last_trade_date = s_data.last_trade_date.trim();
		        		// last trade (time)
		        		s_data.last_trade_time = decodestring[7].replace('"', ' ');
		        		s_data.last_trade_time = s_data.last_trade_time.trim();
		        		// volume
		        		s_data.volume = Long.parseLong(decodestring[8]);
		        		// day's low
		        		s_data.day_low = Double.parseDouble(decodestring[9]);
		        		// day's high
		        		s_data.day_high = Double.parseDouble(decodestring[10]);
		        		// 52 week low
		        		s_data.week_52_low = Double.parseDouble(decodestring[11]);
		        		// 52 week high
		        		s_data.week_52_high = Double.parseDouble(decodestring[12]);
		        		// 50 day moving average
		        		s_data.average_50_day_moving = Double.parseDouble(decodestring[13]);
		        		// 200 day moving average
		        		s_data.average_200_day_moving = Double.parseDouble(decodestring[14]);
		        		// P/E ratio
		        		s_data.p_e_ratio = Double.parseDouble(decodestring[15]);
		        		// name
		        		s_data.name = "";
		        		for(int i=16; i<decodestring.length; i++) {
		        			s_data.name = s_data.name + decodestring[i].replace('"', ' ');
		        			s_data.name = s_data.name.trim();
		        			s_data.name = s_data.name.replace("'","''");
		        		}
		        	}
		        } catch (Exception e) {
		        	// just display error and handle next line
		        	Log.e(TAG, "decode error:" + e.toString());
		        	s_data = null;
		        }

		        decodestring = null;
		    }

		    stream.close();

			return s_data;
		} catch (Exception e) {
			Log.e(TAG, "Cannot start parser for Input steam. Err=" + e.toString());
		}

		return null;
	}

	public boolean startGetDataFromYahoo(String[] symbols) {
		String[]  decodestring = null;
		String 	  szURL = szURL_Server[m_selected_region] + szURL_Prefix;

		try {
			data.clear();

			if(symbols.length < 1) {
				return false;
			}

			szURL += symbols[0];
			for(int i=1; i<symbols.length; i++) {
				szURL += "+" + symbols[i];
			}

			szURL += "&f=";

			for(String x : yahoo_flags) {
				szURL += x;
			}

			Log.d(TAG, "URL=" + szURL);

			URL url = new URL(szURL);
			InputStream stream = url.openStream();

			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		    String line = null;

		    while ((line = reader.readLine()) != null) {
		        Log.d(TAG, "Received:" + line);

		        try {
		        	decodestring = line.split(",", yahoo_flags.length);

		        	if(Double.parseDouble(decodestring[1]) > 0) {
		        		StockData s_data = new StockData();

		        		// symbol
		        		s_data.symbol = decodestring[0].replace('"', ' ');
		        		s_data.symbol = s_data.symbol.trim();
		        		// price
		        		s_data.price = Double.parseDouble(decodestring[1]);
		        		// change
		        		s_data.change = Double.parseDouble(decodestring[2]);
		        		// name
		        		s_data.name = "";
		        		for(int i=3; i<decodestring.length; i++) {
		        			s_data.name = s_data.name + decodestring[i].replace('"', ' ');
		        			s_data.name = s_data.name.trim();
		        			s_data.name = s_data.name.replace("'","''");
		        		}

		        		data.add(s_data);
		        	}
		        } catch (Exception e) {
		        	// just display error and handle next line
		        	Log.e(TAG, "decode error:" + e.toString());
		        }

		        decodestring = null;
		    }

		    stream.close();

			return true;
		} catch (Exception e) {
			Log.e(TAG, "Cannot start parser for Input steam. Err=" + e.toString());
			data.clear();
		}

		return false;
	}

	public int getStockDataCount() {
		return data.size();
	}

	public StockData getStockData(int index) {
		try {
			return data.get(index);
		} catch (Exception e) {
			Log.e(TAG, "getStockData:" + e.toString());
		}

		return null;
	}
}
