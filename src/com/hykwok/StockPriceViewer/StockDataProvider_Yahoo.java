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
	
	// format URL path (refer to http://www.gummy-stuff.org/Yahoo-data.htm for more information)
	private static final String szURL_Prefix = "http://finance.yahoo.com/d/quotes.csv?s=";
	
	// 4 items that we want to get
	// symbol + last trade (price only) + change + name
	private static final String[] yahoo_flags = { "s", "l1", "c1", "n" };
	
	public boolean startGetDataFromYahoo(String[] symbols) {
		String[]  decodestring = null;
		String 	  szURL = szURL_Prefix;
		
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
