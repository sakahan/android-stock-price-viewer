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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class StockData_DB {
	// This variable is used for debug log (LogCat)
	private static final String TAG = "SPV:StockDataDB";

	// available region
	public static final String[] mRegions = { "HK", "US" };
	// time zone
	public static final String[] mTimeZone = { "Asia/Hong_Kong", "US/Eastern" };
	// opening time
	public static final long[] mMarketOpenTime = { 10, 10 };
	// close time
	public static final long[] mMarketCloseTime = { 16, 16 };

	// Database setting variables
	private static final String DATABASE_NAME = "db_sd.db";
	private static final int DATABASE_VERSION = 2;

	/**
	 * 	Table: sd_latestprice
	 * 	Columns:
	 * 		sd_symbol	TEXT		// stock symbol
	 * 		sd_name		TEXT		// stock name
	 * 		sd_price	FLOAT		// latest price
	 *      sd_change	FLOAT		// latest change
     *****  new columns for version 2  *****
     *      sd_region   TEXT        // region
     *      sd_tag_id   INTEGER     // tag id
	 */
	private static final String TABLE_SD_LASTPRICE = "sd_latestprice";
	private static final String COL_SD_SYMBOL = "sd_symbol";
	private static final String COL_SD_NAME = "sd_name";
	private static final String COL_SD_PRICE = "sd_price";
	private static final String COL_SD_CHANGE = "sd_change";
	private static final String COL_SD_REGION = "sd_region";
	private static final String COL_SD_TAG_ID = "sd_tag_id";

	public static final int COL_SD_SYMBOL_IDX = 0;
	public static final int COL_SD_NAME_IDX = 1;
	public static final int COL_SD_PRICE_IDX = 2;
	public static final int COL_SD_CHANGE_IDX = 3;
	public static final int COL_SD_REGION_IDX = 4;
	public static final int COL_SD_TAG_ID_IDX = 5;

    /**
	 * 	Table: sd_table_tag
	 * 	Columns:
	 * 		sd_tag_id	    INTEGER     // tag id (auto increment)
	 * 		sd_tag_name	    TEXT		// tag name
	 * 		sd_tag_desc	    TEXT		// tag description
	 */
	private static final String TABLE_SD_TAG = "sd_table_tag";
	private static final String COL_SD_TT_TAG_ID = "sd_tag_id";
	private static final String COL_SD_TT_TAG_NAME = "sd_tag_name";
	private static final String COL_SD_TT_TAG_DESC = "sd_tag_desc";

    public static final int COL_SD_TT_TAG_ID_IDX = 0;
	public static final int COL_SD_TT_TAG_NAME_IDX = 1;
	public static final int COL_SD_TT_TAG_DESC_IDX = 2;

    void create_DB(SQLiteDatabase db) {
        try {
			String str_sql;
			
			str_sql = "CREATE TABLE " + TABLE_SD_LASTPRICE + " ( " +
			          COL_SD_SYMBOL + " TEXT" + ", " +
			          COL_SD_NAME + " TEXT" +  ", " +
			          COL_SD_PRICE + " FLOAT" +  ", " +
			          COL_SD_CHANGE + " FLOAT" + ", " +
                      COL_SD_REGION + " TEXT" + ", " +
                      COL_SD_TAG_ID + " INTEGER" +
			          " );";
			Log.d(TAG, "create_DB: SQL="+str_sql);
			db.execSQL(str_sql);

            str_sql = "CREATE TABLE " + TABLE_SD_TAG + " ( " +
			          COL_SD_TT_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + ", " +
			          COL_SD_TT_TAG_NAME + " TEXT" +  ", " +
			          COL_SD_TT_TAG_DESC + " TEXT" +
			          " );";
			Log.d(TAG, "create_DB: SQL="+str_sql);
			db.execSQL(str_sql);

            // insert default tag data
			str_sql = "INSERT INTO " + TABLE_SD_TAG +
	          " ( " + COL_SD_TT_TAG_NAME + ", "
	                + COL_SD_TT_TAG_DESC
	          + " ) VALUES ('Default', 'Default');";

			Log.d(TAG, "create_DB: SQL="+str_sql);
			db.execSQL(str_sql);            
		} catch (Exception e) {
			Log.e(TAG, "create_DB:" + e.toString());
		}
    }

    void upgrade_DB(SQLiteDatabase db, int oldversion) {
        try {
            String str_sql;

            if(oldversion < 2) {
                str_sql = "ALTER TABLE " + TABLE_SD_LASTPRICE +
                          " ADD " + COL_SD_REGION + " TEXT" + ";";
                Log.d(TAG, "upgrade_DB: SQL="+str_sql);
			    db.execSQL(str_sql);

                str_sql = "ALTER TABLE " + TABLE_SD_LASTPRICE +
                          " ADD " + COL_SD_TAG_ID + " INT" + ";";
                Log.d(TAG, "upgrade_DB: SQL="+str_sql);
			    db.execSQL(str_sql);

                str_sql = "CREATE TABLE " + TABLE_SD_TAG + " ( " +
			              COL_SD_TT_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + ", " +
			              COL_SD_TT_TAG_NAME + " TEXT" +  ", " +
			              COL_SD_TT_TAG_DESC + " TEXT" +
			              " );";

			    Log.d(TAG, "upgrade_DB: SQL="+str_sql);
			    db.execSQL(str_sql);

                // insert default tag data
			    str_sql = "INSERT INTO " + TABLE_SD_TAG +
	              " ( " + COL_SD_TT_TAG_NAME + ", "
	                    + COL_SD_TT_TAG_DESC
	              + " ) VALUES ('Default', 'Default');";

			    Log.d(TAG, "upgrade_DB: SQL="+str_sql);
			    db.execSQL(str_sql);
            }
        } catch (Exception e) {
			Log.e(TAG, "upgrade_DB:" + e.toString());
		}
    }

	// Database helper class
	private class SDDB_Helper extends SQLiteOpenHelper {
		public SDDB_Helper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// if our database is not existed, create a new one
			create_DB(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// database upgrade
            upgrade_DB(db, oldVersion);
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

    public boolean InsertTag(String name, String desc) {
        String 	str_sql;

		Log.d(TAG, "Insert Tag: name=" + name + " desc=" + desc);

		try {
			// insert new tag data
			str_sql = "INSERT INTO " + TABLE_SD_TAG +
	          " ( " + COL_SD_TT_TAG_NAME + ", "
	                + COL_SD_TT_TAG_DESC
	          + " ) VALUES ('" +
	                name + "', '" +
	                desc + "');";

			Log.d(TAG, "InsertTag: SQL="+str_sql);
			core_db.execSQL(str_sql);
            return true;
		} catch (Exception e) {
			Log.e(TAG, "InsertTag:" + e.toString());
		}

        return false;
    }

    public boolean UpdateTag(int id, String name, String desc) {
        String 	str_sql;

		Log.d(TAG, "Update Tag: id=" + Integer.toString(id) + " name=" + name + " desc=" + desc);

		try {
			// update tag data
			str_sql = "UPDATE " + TABLE_SD_TAG + " SET " +
			          COL_SD_TT_TAG_NAME + "='" + name + "', " +
					  COL_SD_TT_TAG_DESC + "='" + desc + "' " +
			          " WHERE " + COL_SD_TT_TAG_ID + "=" + Integer.toString(id) + ";";
			Log.d(TAG, "UpdateTag: SQL="+str_sql);
			core_db.execSQL(str_sql);
            return true;
		} catch (Exception e) {
			Log.e(TAG, "UpdateTag:" + e.toString());
		}

        return false;
    }

    public boolean DeleteTag(int id) {
        String 	str_sql;

		Log.d(TAG, "Delete Symbol=" + Integer.toString(id));

		try {
			// delete stock data
			str_sql = "DELETE FROM " + TABLE_SD_TAG +
			          " WHERE " + COL_SD_TT_TAG_ID + "=" + Integer.toString(id) + ";";

			Log.d(TAG, "DeleteTag: SQL="+str_sql);
			core_db.execSQL(str_sql);
            return true;
		} catch (Exception e) {
			Log.e(TAG, "DeleteTag:" + e.toString());
		}

        return false;
    }

    public Cursor GetAllTag() {
        String	str_sql = "SELECT * FROM " + TABLE_SD_TAG;

		try {
			Log.d(TAG, "GetAllTag: SQL="+str_sql);
			return core_db.rawQuery(str_sql, null);
		} catch (Exception e) {
			Log.e(TAG, "GetAllTag:" + e.toString());

		}

		return null;
    }

	public boolean InsertStockData(StockData data) {
		String 	str_sql;
        
        // set default region
        if(data.region == StockData.DEFAULT_REGION) {
            data.region = "US";
        }

		Log.d(TAG, "Insert Symbol=" + data.symbol + 
                   " name=" + data.name + 
                   " price=" + Double.toString(data.price) + 
                   " change=" + Double.toString(data.change) +
                   " region=" + data.region +
                   " tag=" + Integer.toString(data.tag_id));

		try {
			// insert new stock data
			str_sql = "INSERT INTO " + TABLE_SD_LASTPRICE +
	          " ( " + COL_SD_SYMBOL + ", "
	                + COL_SD_NAME + ", "
	                + COL_SD_PRICE + ", "
	                + COL_SD_CHANGE + ", "
                    + COL_SD_REGION + ", "
                    + COL_SD_TAG_ID + 
	                " ) VALUES ('" +
	                data.symbol + "', '" +
	                data.name + "', " +
	                Double.toString(data.price) + ", " +
	                Double.toString(data.change) + ", '" +
                    data.region + "', " +
                    Integer.toString(data.tag_id) +
                    ");";

			Log.d(TAG, "InsertStockData: SQL="+str_sql);
			core_db.execSQL(str_sql);
            return true;
		} catch (Exception e) {
			Log.e(TAG, "InsertStockData:" + e.toString());
		}

        return false;
	}

    public boolean UpdateStockPriceData(StockData data) {
		String 	str_sql;

		Log.d(TAG, "Update Symbol=" + data.symbol + 
                   " name=" + data.name + 
                   " price=" + Double.toString(data.price) + 
                   " change=" + Double.toString(data.change));

		try {
			// update stock data
			str_sql = "UPDATE " + TABLE_SD_LASTPRICE + " SET " +
			          COL_SD_NAME + "='" + data.name + "', " +
					  COL_SD_PRICE + "=" + Double.toString(data.price) + ", " +
					  COL_SD_CHANGE + "=" + Double.toString(data.change) + " " +
			          " WHERE " + COL_SD_SYMBOL + "='" + data.symbol + "';";
			Log.d(TAG, "UpdateStockPriceData: SQL="+str_sql);
			core_db.execSQL(str_sql);
            return true;
		} catch (Exception e) {
			Log.e(TAG, "UpdateStockPriceData:" + e.toString());
		}

        return false;
	}

	public boolean UpdateStockData(StockData data) {
		String 	str_sql;

        // set default region
        if(data.region == StockData.DEFAULT_REGION) {
            data.region = "US";
        }

		Log.d(TAG, "Update Symbol=" + data.symbol + 
                   " name=" + data.name + 
                   " price=" + Double.toString(data.price) + 
                   " change=" + Double.toString(data.change) +
                   " region=" + data.region +
                   " tag=" + Integer.toString(data.tag_id));

		try {
			// update stock data
			str_sql = "UPDATE " + TABLE_SD_LASTPRICE + " SET " +
			          COL_SD_NAME + "='" + data.name + "', " +
					  COL_SD_PRICE + "=" + Double.toString(data.price) + ", " +
					  COL_SD_CHANGE + "=" + Double.toString(data.change) + ", " +
                      COL_SD_REGION + "=" + data.region + ", " +
                      COL_SD_TAG_ID + "=" + Integer.toString(data.tag_id) + " " +
			          " WHERE " + COL_SD_SYMBOL + "='" + data.symbol + "';";
			Log.d(TAG, "UpdateStockData: SQL="+str_sql);
			core_db.execSQL(str_sql);
            return true;
		} catch (Exception e) {
			Log.e(TAG, "UpdateStockData:" + e.toString());
		}

        return false;
	}

	public boolean DeleteStockData(String symbol) {
		String 	str_sql;

		Log.d(TAG, "Delete Symbol=" + symbol);

		try {
			// delete stock data
			str_sql = "DELETE FROM " + TABLE_SD_LASTPRICE +
			          " WHERE " + COL_SD_SYMBOL + "='" + symbol + "';";

			Log.d(TAG, "DeleteStockData: SQL="+str_sql);
			core_db.execSQL(str_sql);
            return true;
		} catch (Exception e) {
			Log.e(TAG, "DeleteStockData:" + e.toString());
		}

        return false;
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

    public String getRegion(String symbol) {
		Cursor result = null;
        String ret = StockData.DEFAULT_REGION;

		String	str_sql = "SELECT " + COL_SD_REGION + " FROM " + TABLE_SD_LASTPRICE +
		                  " WHERE " + COL_SD_SYMBOL + "='" + symbol + "';";

		try {
			Log.d(TAG, "getRegion: SQL="+str_sql);
			result = core_db.rawQuery(str_sql, null);

			if(result.getCount() > 0) {
				ret = result.getString(COL_SD_REGION_IDX);
			}

			result.close();
			result = null;
		} catch (Exception e) {
			Log.e(TAG, "getRegion:" + e.toString());
		}

		return ret;
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
