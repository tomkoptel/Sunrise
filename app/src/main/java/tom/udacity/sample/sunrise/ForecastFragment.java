/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package tom.udacity.sample.sunrise;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by samsung on 1/25/15.
 */
public class ForecastFragment extends Fragment {
    public static final String TAG = ForecastFragment.class.getSimpleName();

    private ArrayAdapter<String> mForecastAdapter;
    private ListView mListView;

    public ForecastFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mForecastAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.list_item_forecast, R.id.list_item_forecast_textview,
                new ArrayList<String>());
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = mForecastAdapter.getItem(position);
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                detailIntent.putExtra(Intent.EXTRA_TEXT, forecast);
                getActivity().startActivity(detailIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        performReferesh();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastmenu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);

        int itemId  = item.getItemId();

        if (itemId == R.id.action_refresh) {
            performReferesh();
            return true;
        }

        return result;
    }

    private void performReferesh() {
        String location = PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .getString(getActivity().getString(R.string.pref_location_key), "");
        new FetchWeatherTask().execute(location);
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }


            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;

            try {
                Uri builtUri = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily?")
                        .buildUpon()
                        .appendQueryParameter("q", params[0])
                        .appendQueryParameter("mode", "json")
                        .appendQueryParameter("units", "metric")
                        .appendQueryParameter("cnt", "7")
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
                WeatherDataParser parser = new WeatherDataParser(getActivity());
                return parser.getWeatherDataFromJson(forecastJsonStr, 7);
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
}
