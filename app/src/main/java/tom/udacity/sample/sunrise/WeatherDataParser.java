//  * Copyright (C) 2014 The Android Open Source Project
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

package tom.udacity.sample.sunrise;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import tom.udacity.sample.sunrise.data.WeatherContract;

public class WeatherDataParser {

    private static final String TAG = WeatherDataParser.class.getSimpleName();
    private final Context mContext;

    public WeatherDataParser(Context context) {
        mContext = context;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    public String[] getWeatherDataFromJson(String forecastJsonStr, int numDays, String locationQuery)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DATETIME = "dt";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime = dayForecast.getLong(OWM_DATETIME);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        return resultStrs;
    }

    /**
     * Given a string of the form returned by the api call:
     * http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7
     * retrieve the maximum temperature for the day indicated by dayIndex
     * (Note: 0-indexed, so 0 would refer to the first day).
     */
    private static double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex)
            throws JSONException {
        JSONObject data = new JSONObject(weatherJsonStr);
        JSONArray days = data.getJSONArray("list");
        JSONObject dayJson = days.getJSONObject(dayIndex);
        JSONObject temp = dayJson.getJSONObject("temp");
        return temp.getDouble("max");
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
     * so for convenience we're breaking it out into its own method now.
     */
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        return new SimpleDateFormat("E, MMM d").format(date);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String unitType = sharedPrefs.getString(
                mContext.getString(R.string.pref_units_key),
                mContext.getString(R.string.pref_units_metric)
        );

        if (unitType.equals(mContext.getString(R.string.pref_units_imperial))) {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        } else if (!unitType.equals(mContext.getString(R.string.pref_units_metric))) {
            Log.d(TAG, "Unit type not found: " + unitType);
        }

        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        return roundedHigh + "/" + roundedLow;
    }

    private long addLocation(String locationSettings, String cityName, double lat, double lon) {
        Log.v(TAG, "inserting " + cityName + ", with coord: " + lat + ", " + lon);

        Cursor cursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[] { WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[] {locationSettings},
                null
        );

        try {
            if (cursor != null && cursor.moveToFirst()) {
                Log.v(TAG, "Found in database");
                int locationIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
                return cursor.getLong(locationIndex);
            } else {
                ContentValues contentValues = new ContentValues();
                contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSettings);
                contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
                contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
                contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

                Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, contentValues);
                return ContentUris.parseId(locationUri);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}