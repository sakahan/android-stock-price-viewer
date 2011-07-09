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

import java.util.Calendar;
import java.util.TimeZone;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class StockDataService extends Service {
	// This variable is used for debug log (LogCat)
	private static final String TAG = "SPV:Service";

	// Intent string for broadcasting
	public static final String ACTIVITY_TO_SERVICE_BROADCAST = "com.hykwok.action.SPV_A_TO_S_BROADCAST";
	public static final String SERVICE_TO_ACTIVITY_BROADCAST = "com.hykwok.action.SPV_S_TO_A_BROADCAST";

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
    private static final int STOCKDATA_REFRESH = 6;

    // Preference keys
	private static final String KEY_ROAMING_OPT = "roaming_option";
	private static final String KEY_UPDATE_INTERVAL = "update_interval";
	private static final String KEY_LASTUPDATETIME = "last_update_time";

	private StockData_DB					m_DB = null;
	private StockDataProvider_Yahoo			provider = null;

	private String							m_new_symbol = "";
	private String							m_new_region = "";
	private Cursor							db_alldata_result = null;

	// broadcast receiver
	private Broadcast_Receiver my_intent_receiver = null;

	// task delay time (in ms)
	private long task_delay = 5000;						// 5 seconds
	private long user_task_delay = 900000;				// 15 min
	private final long general_task_delay = 900000;		// 15 min
	private final long max_task_delay = 86400000;		// 1 days
	private long nFailConnectionCount = 0;

	private long ref_time = 0;
	private boolean ref_roaming = false;

	private StockDataConnection	m_connection;

	private Thread parser_thread;
	private boolean parser_thread_alive = true;

	private final IBinder mBinder = new LocalBinder();

	private long  m_start_times = 0;
	private boolean m_force_update = true;

	private SharedPreferences		mPrefs;

	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
	public class LocalBinder extends Binder {
		StockDataService getService() {
            return StockDataService.this;
        }
    }

	@Override
	public IBinder onBind(Intent i) {
		Log.d(TAG, "onBind >>>>>");

    	Log.d(TAG, "onBind <<<<<");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent i) {
		Log.d(TAG, "onUnbind >>>>>");

    	Log.d(TAG, "onUnbind <<<<<");
		return false;
	}

	@Override
	public void onRebind(Intent i) {
		Log.d(TAG, "onRebind >>>>>");

    	Log.d(TAG, "onRebind <<<<<");
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate >>>>>");
		super.onCreate();

		// connection
		m_connection = new StockDataConnection(this);

		// create provider
    	provider = new StockDataProvider_Yahoo();

    	// database
    	connect_Database();
    	
		// register broadcast receiver
		IntentFilter filter = new IntentFilter(ACTIVITY_TO_SERVICE_BROADCAST);
		my_intent_receiver = new Broadcast_Receiver();
		registerReceiver(my_intent_receiver, filter);

		// create a new thread to handle database update
		parser_thread_alive = true;
		parser_thread = new Thread(mTask);

		// get preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		m_start_times++;
		Log.d(TAG, "m_start_times=" + m_start_times);

    	Log.d(TAG, "onCreate <<<<<");
	}

	@Override
	public void onStart(Intent i, int startId) {
		Log.d(TAG, "onStart >>>>>");
		super.onStart(i, startId);

		// get data from preference first
		ref_time = mPrefs.getLong(KEY_LASTUPDATETIME, ref_time);
		ref_roaming = mPrefs.getBoolean(KEY_ROAMING_OPT, ref_roaming);
		String szupdatetime = mPrefs.getString(KEY_UPDATE_INTERVAL, "15");
		user_task_delay = Long.parseLong(szupdatetime);

		// then get data from intent if possible
		ref_time = i.getExtras().getLong(BROADCAST_KEY_LASTUPDATETIME, ref_time);
		ref_roaming = i.getExtras().getBoolean(BROADCAST_KEY_ROAMING_OPT, ref_roaming);
		user_task_delay = i.getExtras().getLong(BROADCAST_KEY_UPDATETIME, user_task_delay) * 60000;

		m_connection.EnableNetworkRoaming(ref_roaming);

		// start a new thread to handle database update
		if(parser_thread.isAlive() == false) {
			parser_thread.start();
		}

    	Log.d(TAG, "onStart <<<<<");
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy >>>>>");
		super.onDestroy();

		parser_thread_alive = false;
		parser_thread.interrupt();

		// remove broadcast receiver
		unregisterReceiver(my_intent_receiver);

		db_alldata_result.close();

		if(m_DB != null) {
			m_DB.CloseDB();
			m_DB = null;
		}

		m_start_times--;

    	Log.d(TAG, "onDestroy <<<<<");
	}

	// background thread to get data from internet
	private Runnable mTask = new Runnable() {
		String		symbol[], db_symbol;
		StockData	stock_data;
		int			cnt, i, total;
		long        current_time;
		boolean		bConnectionFlag, bUseBackupServer;

		private void delay() {
			try {
				Log.d(TAG, "sleep=" + task_delay);
				Thread.sleep(task_delay);
			} catch (InterruptedException e) {
				Log.d(TAG, "Parser thread receive interrupt");
			}
		}

		public void run() {
			do {
				// pause the task
				delay();
				
				// update
				current_time = System.currentTimeMillis();
				bConnectionFlag = true;

				if(m_connection.IsPhoneAvaiable() == false) {
					if(m_connection.IsWIFIAvailabe() == false) {
						Log.e(TAG, "mTask: Connection is not available");
						sendSettingToActivity(STOCKDATA_NODATA_UPD);
						bConnectionFlag = false;
					}
				}

				if(bConnectionFlag) {
					try {
						Log.d(TAG, "Start connection: m_new_symbol="+m_new_symbol+" m_new_region="+m_new_region);
						
						if(m_new_symbol.equalsIgnoreCase("")) {
							Log.d(TAG, "Update all symbols...");
							
							// update all symbols
							connect_Database();
							db_alldata_result.requery();

							if(db_alldata_result.getCount() > 0) {
								symbol = new String[db_alldata_result.getCount()];

								db_alldata_result.moveToFirst();

								// total symbol that have to be updated
								cnt = 0;

								do {
									db_symbol = db_alldata_result.getString(StockData_DB.COL_SD_SYMBOL_IDX);
									if(checkNeedUpdate(db_symbol, current_time)) {
										symbol[cnt++] = db_symbol;
									} else if(m_force_update == true) {
										symbol[cnt++] = db_symbol;
									}
								} while(db_alldata_result.moveToNext());
								
								if(cnt == 0) {
									Log.d(TAG, "No symbol has to be checked...");
									task_delay = general_task_delay;
								} else {
									// use another server if necessary
									if(nFailConnectionCount > 4) {
										bUseBackupServer = true;
									} else {
										bUseBackupServer = false;
									}
									
									total = 0;
									
									if(provider.startGetDataFromYahoo(symbol, bUseBackupServer) == true) {
										total = provider.getStockDataCount();
									}
									
									Log.d(TAG, "Total data has to be updated is " + total);
	
									if(total > 0) {
										connect_Database();
										
										for(i=0; i<total; i++) {
											stock_data = provider.getStockData(i);
											m_DB.UpdateStockPriceData(stock_data);
										}
										
										// update last update time
										ref_time = current_time;
										
										// update delay time
										task_delay = user_task_delay;
										
										// disable "force" update flag
										m_force_update = false;
	
										// send data to activity to update view
										sendSettingToActivity(STOCKDATA_NEWALLDATA_UPD);
										nFailConnectionCount = 0;
									} else {
										sendSettingToActivity(STOCKDATA_NODATA_UPD);
										
										if(cnt > 0) {
											// Cannot connect to the server
											nFailConnectionCount++;
										}
									}
									
									if(nFailConnectionCount > 10) {
										Log.d(TAG, "Too many fail connections...");
										task_delay = general_task_delay * 2;
									}
								}
							} else {
								// no symbol
								Log.d(TAG, "No symbol in the database");
								task_delay = max_task_delay;
							}
						} else {
							Log.d(TAG, "Update the given symbols...");
							
							symbol = new String[1];
							symbol[0] = m_new_symbol;
							m_new_symbol = "";
							nFailConnectionCount = 0;
							
							if(provider.startGetDataFromYahoo(symbol, false) == false) {
								// try to use backup server
								if(provider.startGetDataFromYahoo(symbol, true) == false) {
									nFailConnectionCount++;
								}
							}
							
							if(nFailConnectionCount == 0) {
								stock_data = provider.getStockData(0);
                                stock_data.region = m_new_region;
								connect_Database();
								m_DB.InsertStockData(stock_data);
								sendSettingToActivity(STOCKDATA_NEWDATA_UPD);
							} else {
								sendSettingToActivity(STOCKDATA_NODATA_UPD);
							}

							task_delay = general_task_delay - (current_time - ref_time);
							if(task_delay < 1) {
								task_delay = 1;
							}
						}

						symbol = null;
					} catch (Exception e) {
						Log.e(TAG, "mTask: " + e.toString());
						e.printStackTrace();
						sendSettingToActivity(STOCKDATA_NODATA_UPD);
					}
				} else {
					// no connection, delay 1 min
					task_delay = 60000;
				}
			} while(parser_thread_alive);
		}
	};
	
	private void connect_Database() {
		if(m_DB == null) {
			m_DB = new StockData_DB(this);
			db_alldata_result = null;
	    	db_alldata_result = m_DB.GetAllData();
		}
	}

	private boolean checkNeedUpdate(String symbol, long current_time) {
		TimeZone	tz;
		Calendar	ca;
		int			current_hour, day_of_week;

		for(int i=0; i<StockData_DB.mRegions.length; i++) {
			if(symbol.contains(StockData_DB.mRegions[i])) {
				tz = TimeZone.getTimeZone(StockData_DB.mTimeZone[i]);
				ca = Calendar.getInstance(tz);
				ca.setTimeInMillis(current_time);
				current_hour = ca.get(Calendar.HOUR_OF_DAY);
				day_of_week = ca.get(Calendar.DAY_OF_WEEK);

				Log.d(TAG, "For symbol: " + symbol + ", given hour is " + current_hour + " and given day of week is " + day_of_week);

				if((day_of_week >= Calendar.MONDAY) && (day_of_week <= Calendar.FRIDAY)) {
					// We assume the price has one hour delay
					long market_open_hour = StockData_DB.mMarketOpenTime[i];
					long market_close_hour = StockData_DB.mMarketCloseTime[i] + 1;

					if((current_hour >= market_open_hour) && (current_hour <= market_close_hour)) {
						return true;
					}
				}

				Log.d(TAG, "Market was closed.");
				return false;
			}
		}

		return false;
	}

	// receive data from other activities
	public class Broadcast_Receiver extends BroadcastReceiver {
		int	type;

		@Override
		public void onReceive(Context context, Intent intent) {
			// receive intent from activity
			Log.d(TAG, "receive data from activity >>>>>");

			try {
				type = intent.getExtras().getInt(BROADCAST_KEY_TYPE);

				switch(type) {
				case STOCKDATA_ADD_NEW:
					m_new_symbol = intent.getExtras().getString(BROADCAST_KEY_SYMBOL);
					m_new_region = intent.getExtras().getString(BROADCAST_KEY_REGION);
					
					Log.d(TAG, "Broadcast_Receiver: m_new_symbol=" + m_new_symbol + " m_new_region=" + m_new_region);
					
					if(m_new_symbol.equalsIgnoreCase("")) {
						Log.e(TAG, "Given symbol for ADD NEW is empty!");
						sendSettingToActivity(STOCKDATA_NODATA_UPD);
					} else {
						parser_thread.interrupt();
					}
					break;

				case STOCKDATA_CONFUPDATED:
					ref_time = intent.getExtras().getLong(BROADCAST_KEY_LASTUPDATETIME);
					ref_roaming = intent.getExtras().getBoolean(BROADCAST_KEY_ROAMING_OPT, false);
					break;
					
				case STOCKDATA_REFRESH:
					m_force_update = true;
					parser_thread.interrupt();
					break;

				default:
					break;
				}

			} catch (Exception e) {
				Log.e(TAG, "Broadcast_Receiver:" + e.toString());
			}
		}
	}

	// send data to activity
	void sendSettingToActivity(int type) {
		Intent	intent = new Intent(SERVICE_TO_ACTIVITY_BROADCAST);

		intent.putExtra(BROADCAST_KEY_TYPE, type);
		intent.putExtra(BROADCAST_KEY_LASTUPDATETIME, ref_time);

		SavePreferences();

		Log.d(TAG, "send data to activity >>>>>");
		sendBroadcast(intent);
	}

	void SavePreferences() {
		try {
	    	SharedPreferences.Editor editor = mPrefs.edit();

	    	editor.putLong(KEY_LASTUPDATETIME, ref_time);

	    	editor.commit();
    	} catch (Exception e) {
    		Log.e(TAG, "SavePreferences: " + e.toString());
    	}
    }
}
