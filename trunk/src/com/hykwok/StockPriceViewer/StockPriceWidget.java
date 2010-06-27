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

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.RemoteViews;

public class StockPriceWidget extends AppWidgetProvider {
	// debug tag for LogCat
	private static final String TAG = "SPV:Widget";
	
	@Override
	public void  onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(TAG, "onUpdate >>>>>");
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		// call service to update widget
		context.startService(new Intent(context, UpdateService.class));
		
		Log.d(TAG, "onUpdate <<<<<");
	}
	
	@Override
	public void onEnabled(Context context) {
		Log.d(TAG, "onEnabled >>>>>");
		
		super.onEnabled(context);
		
		Log.d(TAG, "onEnabled <<<<<");
	}
	
	@Override
	public void onDisabled(Context context) {
		Log.d(TAG, "onDisabled >>>>>");
		
		super.onDisabled(context);
		
		// call service to update widget
		context.stopService(new Intent(context, UpdateService.class));
		
		Log.d(TAG, "onDisabled <<<<<");
	}
	
	// service for widget update
	public static class UpdateService extends Service {
		private static final String TAG = "SPV:WidgetService";
		
		private StockData_DB		m_DB = null;
		private Cursor				db_alldata_result = null;
		
		private int					m_current_stock_pos = 0;
		
		@Override
		public void onCreate() {
			Log.d(TAG, "onCreate >>>>>");
			
			super.onCreate();
			
			// database
	    	m_DB = new StockData_DB(this);	    	
	    	db_alldata_result = m_DB.GetAllData();
				    	
			Log.d(TAG, "onCreate <<<<<");
		}
		
		@Override
        public void onStart(Intent intent, int startId) {
			Log.d(TAG, "onStart >>>>>");
			
			super.onStart(intent, startId);
			
			ComponentName thisWidget = new ComponentName(this, StockPriceWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            
			// Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);
            
            // display text
            int total = db_alldata_result.getCount();
            
            for(int i=0; i<total; i++) {
            	SpannableString ss = prepareDisplayString();
            	if(ss != null) {
            		updateViews.setTextViewText(R.id.TextView_Message, ss);
            	}
            
            	// Push update for this widget to the home screen            
            	manager.updateAppWidget(thisWidget, updateViews);
            	
            	try { 
            		Thread.sleep(5000); 
            	} catch (InterruptedException e){
            	} 
            }
			
			Log.d(TAG, "onStart <<<<<");
		}
		
		private SpannableString prepareDisplayString() {
			final String        space = "     ";
			String				result = "", change;			
			SpannableString		ss = null;
			int					start, end;
			
			try {
				if(db_alldata_result.getCount() > 0) {
					Log.d(TAG, "prepareDisplayString: display db index =" + m_current_stock_pos);
					
					if(db_alldata_result.moveToPosition(m_current_stock_pos)) {
						result = db_alldata_result.getString(StockData_DB.COL_SD_SYMBOL_IDX);				
						result = result + space + db_alldata_result.getString(StockData_DB.COL_SD_PRICE_IDX);
						change = db_alldata_result.getString(StockData_DB.COL_SD_CHANGE_IDX);
						result = result + space;
						
						// set color for change
						start = result.length();
						result = result + change;
						end = result.length();
						
						result = result + space + db_alldata_result.getString(StockData_DB.COL_SD_NAME_IDX);
						
						ss = new SpannableString(result);
												
						if(Double.parseDouble(change) < 0) {
							ss.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);							
						} else if(Double.parseDouble(change) > 0) {
							ss.setSpan(new ForegroundColorSpan(Color.GREEN), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						} else {
							ss.setSpan(new ForegroundColorSpan(Color.GRAY), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					}
					
					m_current_stock_pos = (++m_current_stock_pos) % db_alldata_result.getCount();
				} else {
					ss = new SpannableString(getResources().getText(R.string.widget_no_data));
				}
			} catch (Exception e) {
				Log.e(TAG, "prepareDisplayString:" + e.toString());
			}
			
			return ss;
		}
		
		public RemoteViews buildUpdate(Context context) {
			RemoteViews updateViews = null;
			
            // use widget_main.xml for layout
            updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_main);
            
            // set ActivityMain that will be shown when users click the widget
            Intent configIntent = new Intent(context, ActivityMain.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
            // link the layout and the intent
            updateViews.setOnClickPendingIntent(R.id.Frame_Main, pendingIntent);
            
            return updateViews;
		}
		
		@Override
		public void onDestroy() {
			Log.d(TAG, "onDestroy >>>>>");
			
			super.onDestroy();
			
			db_alldata_result.close();
			db_alldata_result = null;
			
			m_DB.CloseDB();
			m_DB = null;
			
	    	Log.d(TAG, "onDestroy <<<<<");
		}

		@Override
		public IBinder onBind(Intent i) {
			// nothing to do
			return null;
		}
	} 
}
