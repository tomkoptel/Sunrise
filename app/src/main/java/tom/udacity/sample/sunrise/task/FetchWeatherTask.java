package tom.udacity.sample.sunrise.task;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import tom.udacity.sample.sunrise.WeatherDataParser;

public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private static final String TAG = FetchWeatherTask.class.getSimpleName();

    private final Context mContext;
    private final ArrayAdapter<String> mForecastAdapter;

    public FetchWeatherTask(Context context, ArrayAdapter<String> adapter) {
        mContext = context;
        mForecastAdapter = adapter;
    }

    @Override
    protected String[] doInBackground(String... params) {
        if (params.length == 0) {
            return null;
        }

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String forecastJsonStr = null;

        String locationQuery = params[0];
        int numDays = 14;

        try {
            Uri builtUri = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily?")
                    .buildUpon()
                    .appendQueryParameter("q", locationQuery)
                    .appendQueryParameter("mode", "json")
                    .appendQueryParameter("units", "metric")
                    .appendQueryParameter("cnt", Integer.toString(numDays))
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            forecastJsonStr = buffer.toString();
            WeatherDataParser parser = new WeatherDataParser(mContext);
            return parser.getWeatherDataFromJson(forecastJsonStr, numDays, locationQuery);
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String[] results) {
        if (results != null) {
            mForecastAdapter.clear();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
                mForecastAdapter.addAll(Arrays.asList(results));
            } else {
                for (String data : results) {
                    mForecastAdapter.add(data);
                }
            }
        }
    }
}
