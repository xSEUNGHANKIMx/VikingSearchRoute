package com.example.viking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	private static final int MAX_SELECT_CITY = 4;

	private SelectableAdapter mAdapter;
	private List<String> mCountryList = new ArrayList<String>();
	private List<String> mSelCityList = new ArrayList<String>();
	private HashMap<String, List<String>> mDataSet = new HashMap<String, List<String>>();
	private HashMap<String, List<Double>> mLatLngSet = new HashMap<String, List<Double>>();
	private ArrayList<ArrayList<String>> mPermuted = new ArrayList<ArrayList<String>>();
	private ExpandableListView mListView;
	private TextView mSelCounter;
	private Button mSearchBtn;
	private List<String> mShortestRoute = new ArrayList<String>();
	private double mMinDistance = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mAdapter = new SelectableAdapter();
		mListView = (ExpandableListView) findViewById(R.id.listview);
		mListView.setAdapter(mAdapter);
		mSelCounter = (TextView) findViewById(R.id.selected_count);
		mSearchBtn = (Button) findViewById(R.id.btn_search);

		initListeners();
		initData();
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void initListeners() {
		mListView.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				return true;
			}
		});

		mListView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition,
					long id) {
				return false;
			}
		});

		mSearchBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mSelCityList.size() == MAX_SELECT_CITY) {
					new DataLongOperationAsynchTask().execute();
				}
			}
		});
	}

	private void initData() {
		mCountryList = Arrays.asList(getResources().getStringArray(R.array.array_countries));

		for (int i = 0; i < mCountryList.size(); i++) {
			List<String> arraylist = new ArrayList<String>();

			switch (mCountryList.get(i)) {
			case "Austria":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_austria_city));
				break;
			case "England":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_england_city));
				break;
			case "France":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_france_city));
				break;
			case "Greece":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_greece_city));
				break;
			case "Germany":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_germany_city));
				break;
			case "Hungary":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_hungary_city));
				break;
			case "Italy":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_italy_city));
				break;
			case "Netherlands":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_netherlands_city));
				break;
			case "Russia":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_russia_city));
				break;
			case "Spain":
				arraylist = Arrays.asList(getResources().getStringArray(R.array.array_spain_city));
				break;
			}

			if (!arraylist.isEmpty()) {
				mDataSet.put(mCountryList.get(i), arraylist);
			}
		}

		mSelCounter.setText(String.format(getResources().getString(R.string.city_select_count), mSelCityList.size()));
	}

	@Override
	public void onClick(View v) {
	}

	private void getShortestRoute() {
		mShortestRoute.clear();
		mMinDistance = 0;
		for (int i = 0; i < mPermuted.size(); i++) {
			ArrayList<String> arr = mPermuted.get(i);
			double totalDistance = 0;

			for (int j = 0; j < MAX_SELECT_CITY - 1; j++) {
				Location loc1 = new Location("");
				Location loc2 = new Location("");

				loc1.setLatitude(mLatLngSet.get(arr.get(j)).get(VikingConstants.LATITUDE));
				loc1.setLongitude(mLatLngSet.get(arr.get(j)).get(VikingConstants.LONGITUDE));
				loc2.setLatitude(mLatLngSet.get(arr.get(j + 1)).get(VikingConstants.LATITUDE));
				loc2.setLongitude(mLatLngSet.get(arr.get(j + 1)).get(VikingConstants.LONGITUDE));

				totalDistance += loc1.distanceTo(loc2);
			}

			if (mMinDistance == 0) {
				mMinDistance = totalDistance;
				mShortestRoute.addAll(arr);
			} else {
				if (totalDistance < mMinDistance) {
					mMinDistance = totalDistance;
					mShortestRoute.clear();
					mShortestRoute.addAll(arr);
				}
			}
		}
	}

	private void permute(ArrayList<String> arr, int index) {
		mPermuted.clear();
		permuteHelper(arr, index);
	}

	private void permuteHelper(ArrayList<String> arr, int index) {

		if (index >= arr.size() - 1) {
			ArrayList<String> result = new ArrayList<String>();
			for (int i = 0; i < arr.size(); i++) {
				result.add(arr.get(i));
			}
			mPermuted.add(result);

			return;
		}

		for (int i = index; i < arr.size(); i++) {
			String t = arr.get(index);
			arr.set(index, arr.get(i));
			arr.set(i, t);

			permuteHelper(arr, index + 1);

			t = arr.get(index);
			arr.set(index, arr.get(i));
			arr.set(i, t);
		}
	}

	private void showSelectedCityList() {
		CharSequence[] cities = mSelCityList.toArray(new String[mSelCityList.size()]);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
		dialogBuilder.setTitle(getResources().getString((R.string.select_first_city)));
		dialogBuilder.setItems(cities, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				if (item != 0) {
					Collections.swap(mSelCityList, 0, item);
				}
				permute((ArrayList<String>) mSelCityList, 1);
				getShortestRoute();
				showResult();
			}
		}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		AlertDialog alertDialogObject = dialogBuilder.create();
		alertDialogObject.show();
	}

	private void showResult() {
		final Dialog myDialog = new Dialog(MainActivity.this);
		myDialog.setContentView(R.layout.dialog_result_layout);
		myDialog.setTitle(
				String.format(getResources().getString(R.string.shortest_route_result), (int) mMinDistance / 1000));

		Location loc1 = new Location("");
		Location loc2 = new Location("");
		TextView route = (TextView) myDialog.findViewById(R.id.tv_result_route);
		int distance1, distance2, distance3;
		String detail;

		loc1.setLatitude(mLatLngSet.get(mShortestRoute.get(0)).get(VikingConstants.LATITUDE));
		loc1.setLongitude(mLatLngSet.get(mShortestRoute.get(0)).get(VikingConstants.LONGITUDE));
		loc2.setLatitude(mLatLngSet.get(mShortestRoute.get(1)).get(VikingConstants.LATITUDE));
		loc2.setLongitude(mLatLngSet.get(mShortestRoute.get(1)).get(VikingConstants.LONGITUDE));
		distance1 = (int) loc1.distanceTo(loc2) / 1000;

		loc1.setLatitude(mLatLngSet.get(mShortestRoute.get(1)).get(VikingConstants.LATITUDE));
		loc1.setLongitude(mLatLngSet.get(mShortestRoute.get(1)).get(VikingConstants.LONGITUDE));
		loc2.setLatitude(mLatLngSet.get(mShortestRoute.get(2)).get(VikingConstants.LATITUDE));
		loc2.setLongitude(mLatLngSet.get(mShortestRoute.get(2)).get(VikingConstants.LONGITUDE));
		distance2 = (int) loc1.distanceTo(loc2) / 1000;

		loc1.setLatitude(mLatLngSet.get(mShortestRoute.get(2)).get(VikingConstants.LATITUDE));
		loc1.setLongitude(mLatLngSet.get(mShortestRoute.get(2)).get(VikingConstants.LONGITUDE));
		loc2.setLatitude(mLatLngSet.get(mShortestRoute.get(3)).get(VikingConstants.LATITUDE));
		loc2.setLongitude(mLatLngSet.get(mShortestRoute.get(3)).get(VikingConstants.LONGITUDE));
		distance3 = (int) loc1.distanceTo(loc2) / 1000;

		detail = "1. " + mShortestRoute.get(0) + " to " + mShortestRoute.get(1) + " : " + distance1 + " km\n\n";
		detail += "2. " + mShortestRoute.get(1) + " to " + mShortestRoute.get(2) + " : " + distance2 + " km\n\n";
		detail += "3. " + mShortestRoute.get(2) + " to " + mShortestRoute.get(3) + " : " + distance3 + " km\n\n";
		route.setText(detail);

		Button showmap = (Button) myDialog.findViewById(R.id.btn_show_map);
		showmap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showMap();
				myDialog.dismiss();

			}
		});

		myDialog.show();

	}

	private void showMap() {
		CharSequence[] cities = mShortestRoute.toArray(new String[mShortestRoute.size()]);
		Intent intent = new Intent(MainActivity.this, ShowMapActivity.class);

		intent.putStringArrayListExtra(VikingConstants.INTENT_KEY_ORIGIN,
				new ArrayList<String>(Arrays.asList((String) cities[0],
						Double.toString(mLatLngSet.get(cities[0]).get(VikingConstants.LATITUDE)),
						Double.toString(mLatLngSet.get(cities[0]).get(VikingConstants.LONGITUDE)))));
		intent.putStringArrayListExtra(VikingConstants.INTENT_KEY_WAYPOINT1,
				new ArrayList<String>(Arrays.asList((String) cities[1],
						Double.toString(mLatLngSet.get(cities[1]).get(VikingConstants.LATITUDE)),
						Double.toString(mLatLngSet.get(cities[1]).get(VikingConstants.LONGITUDE)))));
		intent.putStringArrayListExtra(VikingConstants.INTENT_KEY_WAYPOINT2,
				new ArrayList<String>(Arrays.asList((String) cities[2],
						Double.toString(mLatLngSet.get(cities[2]).get(VikingConstants.LATITUDE)),
						Double.toString(mLatLngSet.get(cities[2]).get(VikingConstants.LONGITUDE)))));
		intent.putStringArrayListExtra(VikingConstants.INTENT_KEY_DESTINATION,
				new ArrayList<String>(Arrays.asList((String) cities[3],
						Double.toString(mLatLngSet.get(cities[3]).get(VikingConstants.LATITUDE)),
						Double.toString(mLatLngSet.get(cities[3]).get(VikingConstants.LONGITUDE)))));

		startActivity(intent);
	}

	private void showToast(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (message != null) {
					Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	class SelectableAdapter extends BaseExpandableListAdapter {

		private LayoutInflater mInflator;

		public SelectableAdapter() {
			super();
			this.mInflator = LayoutInflater.from(MainActivity.this);
		}

		@Override
		public String getGroup(int groupPosition) {
			return mCountryList.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return mCountryList.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		protected class GroupViewHolder {
			ImageView expandableIcon;
			TextView countryName;
		}

		@Override
		public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView,
				final ViewGroup parent) {
			View view = convertView;
			GroupViewHolder holder;
			final String countryName = getGroup(groupPosition);

			if (view == null) {
				view = mInflator.inflate(R.layout.listitem_country, parent, false);
				holder = new GroupViewHolder();
				holder.expandableIcon = (ImageView) view.findViewById(R.id.listitem_country_expandable_icon);
				holder.countryName = (TextView) view.findViewById(R.id.listitem_country_name);
				view.setTag(holder);
			} else {
				view.destroyDrawingCache();
				holder = (GroupViewHolder) view.getTag();
			}

			// Set Expanding Icon
			if (isExpanded) {
				holder.expandableIcon.setBackgroundResource(R.drawable.btn_tree_unfold);
			} else {
				holder.expandableIcon.setBackgroundResource(R.drawable.btn_tree_fold);
			}
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (isExpanded) {
						((ExpandableListView) parent).collapseGroup(groupPosition);
					} else {
						((ExpandableListView) parent).expandGroup(groupPosition, true);
					}
				}
			});

			// Set Country Name
			holder.countryName.setText(countryName);
			return view;
		}

		@Override
		public String getChild(int groupPosition, int childPosition) {
			String countryName = mCountryList.get(groupPosition);
			return mDataSet.get(countryName).get(childPosition) + ", " + countryName;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			String countryName = mCountryList.get(groupPosition);
			return mDataSet.get(countryName).size();
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		protected class MemberViewHolder {
			ImageView checkbox;
			TextView cityName;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			View view = convertView;
			MemberViewHolder holder;

			final String cityName = getChild(groupPosition, childPosition);

			if (view == null) {
				holder = new MemberViewHolder();
				view = mInflator.inflate(R.layout.listitem_city, parent, false);
				holder.checkbox = (ImageView) view.findViewById(R.id.listitem_city_checkbox);
				holder.cityName = (TextView) view.findViewById(R.id.listitem_city_name);

				view.setTag(holder);
			} else {
				holder = (MemberViewHolder) view.getTag();
			}

			// Set Check
			holder.checkbox.setSelected(mSelCityList.contains(cityName));

			// Set City Name
			if (StringUtils.isNotEmpty(cityName)) {
				holder.cityName.setText(cityName);
			}

			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ImageView checkBox = (ImageView) v.findViewById(R.id.listitem_city_checkbox);
					boolean bSelected = !checkBox.isSelected();

					if (bSelected) {
						if (mSelCityList.size() < MAX_SELECT_CITY) {
							mSelCityList.add(cityName);
							checkBox.setSelected(bSelected);
						} else {
							showToast(getString(R.string.toast_max_selection));
						}
					} else {
						mSelCityList.remove(cityName);
						checkBox.setSelected(bSelected);
					}

					mSelCounter.setText(
							String.format(getResources().getString(R.string.city_select_count), mSelCityList.size()));

					if (mSelCityList.size() == MAX_SELECT_CITY) {
						mSearchBtn.setEnabled(true);
						mSearchBtn.setTextColor(Color.parseColor("#FF0000"));
					} else {
						mSearchBtn.setEnabled(false);
						mSearchBtn.setTextColor(Color.parseColor("#AAAAAA"));
					}
				}
			});

			return view;

		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}
	}

	private class DataLongOperationAsynchTask extends AsyncTask<Void, Void, String> {
		ProgressDialog dialog = new ProgressDialog(MainActivity.this);

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mLatLngSet.clear();
			dialog.setMessage("Please wait...");
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
		}

		@Override
		protected String doInBackground(Void... params) {
			String response;

			try {
				for (int i = 0; i < mSelCityList.size(); i++) {
					String city = mSelCityList.get(i);
					response = getLatLongByURL(
							"http://maps.google.com/maps/api/geocode/json?address=" + city + "&sensor=false");
					if (response != null) {
						JSONObject jsonObject = new JSONObject(response);
						Double lat = ((JSONArray) jsonObject.get("results")).getJSONObject(0).getJSONObject("geometry")
								.getJSONObject("location").getDouble("lat");
						Double lng = ((JSONArray) jsonObject.get("results")).getJSONObject(0).getJSONObject("geometry")
								.getJSONObject("location").getDouble("lng");

						mLatLngSet.put(city, new ArrayList<Double>(Arrays.asList(lat, lng)));
					}
				}

				return "";
			} catch (JSONException e) {
				e.printStackTrace();
				return "error";
			} catch (Exception e) {
				return "error";
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}

			if (!StringUtils.equals(result, "error")) {
				showSelectedCityList();
			} else {
				showToast(getResources().getString(R.string.search_err));
			}
		}
	}

	public String getLatLongByURL(String requestURL) {
		URL url;
		String response = "";
		try {
			url = new URL(requestURL);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setDoOutput(true);
			int responseCode = conn.getResponseCode();

			if (responseCode == HttpsURLConnection.HTTP_OK) {
				String line;
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				while ((line = br.readLine()) != null) {
					response += line;
				}
			} else {
				response = "";
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

}
