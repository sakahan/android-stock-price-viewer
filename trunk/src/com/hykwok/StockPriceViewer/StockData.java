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

// Refer to the section "Avoid Internal Getters/Setters" of 
// the article "Designing for Performance" (available in Android Development Document)
public class StockData {
	public String symbol;
	public double price;
	public double change;
	public String name;
    public String region;
    public int    tag_id;

    public static final String DEFAULT_REGION = "UNKNOWN";

    public StockData() {
        // set default values
        region = DEFAULT_REGION;
        tag_id = 1;	// the ID of the "default" should be 1 normally
    }
}
