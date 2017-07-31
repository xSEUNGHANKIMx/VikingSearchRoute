package com.example.viking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class ShowMapActivity extends FragmentActivity {

	final String TAG = "PathGoogleMapActivity";
	// private static final LatLng LOWER_MANHATTAN = new LatLng(40.722543,
	// -73.998585);
	// private static final LatLng BROOKLYN_BRIDGE = new LatLng(40.7057, -73.9964);
	// private static final LatLng WALL_STREET = new LatLng(40.7064, -74.0094);

	private List<String> mOrigin = new ArrayList<String>();
	private List<String> mWaypoint1 = new ArrayList<String>();
	private List<String> mWaypoint2 = new ArrayList<String>();
	private List<String> mDestination = new ArrayList<String>();

	private String mOriginName, mWaypoint1Name, mWaypoint2Name, mDestinationName;
	private LatLng mOriginLatLng, mWaypoint1LatLng, mWaypoint2LatLng, mDestinationLatLng;
	private GoogleMap googleMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mOrigin = getIntent().getStringArrayListExtra(VikingConstants.INTENT_KEY_ORIGIN);
		mWaypoint1 = getIntent().getStringArrayListExtra(VikingConstants.INTENT_KEY_WAYPOINT1);
		mWaypoint2 = getIntent().getStringArrayListExtra(VikingConstants.INTENT_KEY_WAYPOINT2);
		mDestination = getIntent().getStringArrayListExtra(VikingConstants.INTENT_KEY_DESTINATION);

		mOriginName = mOrigin.get(0);
		mOriginLatLng = new LatLng(Double.parseDouble(mOrigin.get(1)), Double.parseDouble(mOrigin.get(2)));

		mWaypoint1Name = mWaypoint1.get(0);
		mWaypoint1LatLng = new LatLng(Double.parseDouble(mWaypoint1.get(1)), Double.parseDouble(mWaypoint1.get(2)));

		mWaypoint2Name = mWaypoint2.get(0);
		mWaypoint2LatLng = new LatLng(Double.parseDouble(mWaypoint2.get(1)), Double.parseDouble(mWaypoint2.get(2)));

		mDestinationName = mDestination.get(0);
		mDestinationLatLng = new LatLng(Double.parseDouble(mDestination.get(1)),
				Double.parseDouble(mDestination.get(2)));
		setContentView(R.layout.show_map);
		SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		googleMap = fm.getMap();

		MarkerOptions options = new MarkerOptions();
		options.position(mOriginLatLng);
		options.position(mWaypoint1LatLng);
		options.position(mWaypoint2LatLng);
		options.position(mDestinationLatLng);
		googleMap.addMarker(options);
		String url = getMapsApiDirectionsUrl();
		ReadTask downloadTask = new ReadTask();
		downloadTask.execute(url);

		googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mOriginLatLng, 1));
		addMarkers();

	}

	private String getMapsApiDirectionsUrl() {
		String url = "http://maps.googleapis.com/maps/api/directions/json?";

		List<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair("origin", mOriginLatLng.latitude + "," + mOriginLatLng.longitude));
		params.add(new BasicNameValuePair("destination",
				mDestinationLatLng.latitude + "," + mDestinationLatLng.longitude));
		params.add(new BasicNameValuePair("waypoints", mWaypoint1LatLng.latitude + "," + mWaypoint1LatLng.longitude));
		params.add(new BasicNameValuePair("waypoints", mWaypoint2LatLng.latitude + "," + mWaypoint2LatLng.longitude));

		return url + URLEncodedUtils.format(params, "utf-8");
	}

	private void addMarkers() {
		if (googleMap != null) {
			googleMap.addMarker(new MarkerOptions().position(mOriginLatLng).title("1. " + mOriginName));
			googleMap.addMarker(new MarkerOptions().position(mWaypoint1LatLng).title("2. " + mWaypoint1Name));
			googleMap.addMarker(new MarkerOptions().position(mWaypoint2LatLng).title("3. " + mWaypoint2Name));
			googleMap.addMarker(new MarkerOptions().position(mDestinationLatLng).title("4." + mDestinationName));
		}
	}

	private class ReadTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... url) {
			String data = "";
			try {
				HttpConnection http = new HttpConnection();
				data = http.readUrl(url[0]);
			} catch (Exception e) {
				Log.d("Background Task", e.toString());
			}
			return data;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			new ParserTask().execute(result);
		}
	}

	private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

		@Override
		protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

			JSONObject jObject;
			List<List<HashMap<String, String>>> routes = null;

			try {
				jObject = new JSONObject(jsonData[0]);
				PathJSONParser parser = new PathJSONParser();
				routes = parser.parse(jObject);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return routes;
		}

		@Override
		protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
			ArrayList<LatLng> points = null;
			PolylineOptions polyLineOptions = null;

			// traversing through routes
			for (int i = 0; i < routes.size(); i++) {
				points = new ArrayList<LatLng>();
				polyLineOptions = new PolylineOptions();
				List<HashMap<String, String>> path = routes.get(i);

				for (int j = 0; j < path.size(); j++) {
					HashMap<String, String> point = path.get(j);

					double lat = Double.parseDouble(point.get("lat"));
					double lng = Double.parseDouble(point.get("lng"));
					LatLng position = new LatLng(lat, lng);

					points.add(position);
				}

				polyLineOptions.addAll(points);
				polyLineOptions.width(2);
				polyLineOptions.color(Color.BLUE);
			}

			googleMap.addPolyline(polyLineOptions);
		}
	}
}
