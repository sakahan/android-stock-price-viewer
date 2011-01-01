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

public class StockDetailData {
	public String symbol;
	public double last_trade_price;
	public String last_trade_date;
	public String last_trade_time;
	public double change;
	public String change_percent;
	public double previous_close;
	public double open;
	public long volume;
	public double day_low;
	public double day_high;
	public double week_52_low;
	public double week_52_high;
	public double average_50_day_moving;
	public double average_200_day_moving;
	public double p_e_ratio;
	public String name;
}
