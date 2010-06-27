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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class StockData_DB {
	// This variable is used for debug log (LogCat)
	private static final String TAG = "SPV:StockDataDB";
	
	// available region
	public static final String[] mRegions = { "HK" };
	// time zone
	public static final String[] mTimeZone = { "Asia/Hong_Kong" };
	// opening time
	public static final long[] mMarketOpenTime = { 10 };
	// close time
	public static final long[] mMarketCloseTime = { 16 };
	
	// Database setting variables
	private static final String DATABASE_NAME = "db_sd.db";
	private static final int DATABASE_VERSION = 1;
	
	/**
	 * 	Table: sd_latestprice
	 * 	Columns:
	 * 		sd_symbol	TEXT		// stock symbol
	 * 		sd_name		TEXT		// stock name
	 * 		sd_price	FLOAT		// latest price
	 *      sd_change	FLOAT		// latest change 	
	 */
	private static final String TABLE_SD_LASTPRICE = "sd_latestprice";	
	private static final String COL_SD_SYMBOL = "sd_symbol";
	private static final String COL_SD_NAME = "sd_name";
	private static final String COL_SD_PRICE = "sd_price";
	private static final String COL_SD_CHANGE = "sd_change";
	
	public static final int COL_SD_SYMBOL_IDX = 0;
	public static final int COL_SD_NAME_IDX = 1;
	public static final int COL_SD_PRICE_IDX = 2;
	public static final int COL_SD_CHANGE_IDX = 3;
	
	// Database helper class	
	private class SDDB_Helper extends SQLiteOpenHelper {
		public SDDB_Helper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
	        
			try {
				String str_sql;
				
				// if database is not existed, create new tables
				str_sql = "CREATE TABLE " + TABLE_SD_LASTPRICE + " ( " +
				          COL_SD_SYMBOL + " TEXT" + ", " +
				          COL_SD_NAME + " TEXT" +  ", " +
				          COL_SD_PRICE + " FLOAT" +  ", " +
				          COL_SD_CHANGE + " FLOAT" +
				          " );";
				Log.d(TAG, "setup tables: SQL="+str_sql);
				db.execSQL(str_sql);
			} catch (Exception e) {
				Log.e(TAG, "onCreate:" + e.toString());				
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Currently, nothing to do for database upgrade
		}
	}
	
	// database variables
	private SDDB_Helper		helper;
	private SQLiteDatabase	core_db = null;
	
	public StockData_DB(Context context) {
		helper = new SDDB_Helper(context);		
		if(core_db == null) {
			Log.d(TAG, "Open database...");
			core_db = helper.getWritableDatabase();
		}
	}	
	
	@Override
	public void finalize() {
		CloseDB();
	}
	
	public void CloseDB() {
		if(core_db != null) {
			if(core_db.isOpen()) {
				Log.d(TAG, "Close database...");
				core_db.close();
				core_db = null;
			}
		}
	}
	
	public void InsertStockData(StockData data) {
		String 	str_sql;
		
		Log.d(TAG, "Insert Symbol=" + data.symbol + " name=" + data.name + " price=" + Double.toString(data.price) + " change=" + Double.toString(data.change));
		
		try {
			// insert new stock data
			str_sql = "INSERT INTO " + TABLE_SD_LASTPRICE + 
	          " ( " + COL_SD_SYMBOL + ", " 
	                + COL_SD_NAME + ", " 
	                + COL_SD_PRICE + ", " 
	                + COL_SD_CHANGE
	          + " ) VALUES ('" + 
	                data.symbol + "', '" + 
	                data.name + "', " + 
	                Double.toString(data.price) + ", " + 
	                Double.toString(data.change) + ");";
			
			Log.d(TAG, "InsertStockData: SQL="+str_sql);
			core_db.execSQL(str_sql);			
		} catch (Exception e) {
			Log.e(TAG, "InsertStockData:" + e.toString());
		}
	}
	
	public void UpdateStockData(StockData data) {
		String 	str_sql;
		
		Log.d(TAG, "Update Symbol=" + data.symbol + " name=" + data.name + " price=" + Double.toString(data.price) + " change=" + Double.toString(data.change));
		
		try {
			// update stock data
			str_sql = "UPDATE " + TABLE_SD_LASTPRICE + " SET " +
			          COL_SD_NAME + "='" + data.name + "', " +
					  COL_SD_PRICE + "=" + Double.toString(data.price) + ", " +
					  COL_SD_CHANGE + "=" + Double.toString(data.change) + " " +
			          " WHERE " + COL_SD_SYMBOL + "='" + data.symbol + "';";
			Log.d(TAG, "UpdateStockData: SQL="+str_sql);
			core_db.execSQL(str_sql);			
		} catch (Exception e) {
			Log.e(TAG, "UpdateStockData:" + e.toString());
		}
	}
	
	public void DeleteStockData(String symbol) {
		String 	str_sql;
		
		Log.d(TAG, "Delete Symbol=" + symbol);
		
		try {
			// delete stock data
			str_sql = "DELETE FROM " + TABLE_SD_LASTPRICE + 
			          " WHERE " + COL_SD_SYMBOL + "='" + symbol + "';";
			
			Log.d(TAG, "DeleteStockData: SQL="+str_sql);
			core_db.execSQL(str_sql);
		} catch (Exception e) {
			Log.e(TAG, "DeleteStockData:" + e.toString());
		}
	}	
	
	public Cursor GetAllData() {
		String	str_sql = "SELECT * FROM " + TABLE_SD_LASTPRICE;
		
		try {
			Log.d(TAG, "GetAllData: SQL="+str_sql);
			return core_db.rawQuery(str_sql, null);
		} catch (Exception e) {
			Log.e(TAG, "GetAllData:" + e.toString());
		}
		
		return null;
	}
	
	public boolean IsSymbolExist(String symbol) {
		Cursor result = null;
		boolean flag = true;
		
		String	str_sql = "SELECT * FROM " + TABLE_SD_LASTPRICE +
		                  " WHERE " + COL_SD_SYMBOL + "='" + symbol + "';";
		
		try {
			Log.d(TAG, "IsSymbolExist: SQL="+str_sql);
			result = core_db.rawQuery(str_sql, null);
			
			if(result.getCount() == 0) {
				flag = false;
			}
			
			result.close();
			result = null;
		} catch (Exception e) {
			Log.e(TAG, "IsSymbolExist:" + e.toString());
		}
		
		return flag;
	}
}
