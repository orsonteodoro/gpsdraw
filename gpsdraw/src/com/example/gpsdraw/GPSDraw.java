package com.example.gpsdraw;

import java.lang.Object;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.internal.et;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class GPSDraw extends Activity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {
	// Request code to send to Google Play services
	public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private static String lastLocation;
	LocationClient locationClient;
	private static String debugMessage = "";
	private static int color = Color.BLACK;

	private static String groupId = "";
	private static String drawingId = "";
	private static double latitude = 0;
	private static double longitude = 0;
	private static boolean updateLatLng = false;
	private static boolean penState = false;

	public static List<Stroke> strokes;

	public static class Stroke {
		public int color;
		public List<LatLng> path;
		public Stroke() {
			path = new LinkedList<LatLng>();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gpsdraw);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new MainFragment()).commit();
		}
		locationClient = new LocationClient(this, this, this);
		locationClient.connect();
		Toast.makeText(this, "Connected LocationClient", Toast.LENGTH_SHORT)
				.show();
		strokes = new LinkedList<Stroke>();
		MapsInitializer.initialize(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Toast.makeText(this, "Disconnecting", Toast.LENGTH_SHORT).show();
		locationClient.disconnect();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gpsdraw, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Google Maps fragment
	 */
	public static class DrawCanvasFragment extends Fragment {
		GoogleMap gm;

		public DrawCanvasFragment() {

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			final View rootView = inflater.inflate(
					R.layout.fragment_drawcanvas, container, false);
			switch (GooglePlayServicesUtil
					.isGooglePlayServicesAvailable(getActivity())) {
			case ConnectionResult.SUCCESS:
				Toast.makeText(getActivity(), "Connect success",
						Toast.LENGTH_SHORT).show();
				MapFragment mf = ((MapFragment) getFragmentManager().findFragmentById(
						R.id.map));
				gm = mf.getMap();
				gm.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				LatLng place = new LatLng(latitude, longitude);
				gm.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 16));
				gm.getUiSettings().setScrollGesturesEnabled(false);
				gm.getUiSettings().setZoomGesturesEnabled(false);

				/*
				for (GPSDraw.Stroke s : strokes)
				{
					if (s.po != null)
						s.pl = gm.addPolyline(s.po.addAll(s.path));
					Toast.makeText(getActivity(), "added stroke: "+s.path.size(),
							Toast.LENGTH_SHORT).show();
				}*/
				

				for (GPSDraw.Stroke s : strokes)
				{
					PolylineOptions po = new PolylineOptions().width(10).color(s.color);
					gm.addPolyline(po.addAll(s.path));
				}

				//Stroke s = new GPSDraw.Stroke();
				//strokes.add(s);
				if (strokes.size() > 0)
				{
					Stroke s = strokes.get(strokes.size() - 1);
					PolylineOptions po = new PolylineOptions().width(10).color(color);
					gm.addPolyline(po.addAll(s.path));
				}

				break;
			case ConnectionResult.SERVICE_MISSING:
				Toast.makeText(getActivity(), "Missing service",
						Toast.LENGTH_SHORT).show();
				break;
			case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
				Toast.makeText(getActivity(), "Update required",
						Toast.LENGTH_SHORT).show();
				break;
			}

			Switch swLock = (Switch) rootView.findViewById(R.id.switchLock);
			swLock.setChecked(true);
			swLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					if (isChecked) {
						gm.getUiSettings().setScrollGesturesEnabled(false);
					} else {
						gm.getUiSettings().setScrollGesturesEnabled(true);
					}
				}
			});

			Button btnPenUp = (Button) rootView
					.findViewById(R.id.buttonBack);
			btnPenUp.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getFragmentManager().beginTransaction()
							.replace(R.id.container, new MainFragment())
							.commit();
				}
			});
			return rootView;
		}

		@Override
		public void onDestroyView() {
			FragmentManager fm = getFragmentManager();
			Fragment f = fm.findFragmentById(R.id.map);
			if (f != null)
				fm.beginTransaction().remove(f).commit();
			super.onDestroyView();
		}
	}

	/**
	 * Main UI Fragment
	 */
	public static class MainFragment extends Fragment {

		public static TextView locationView;

		public MainFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			final View rootView = inflater.inflate(R.layout.fragment_gpsdraw,
					container, false);

			locationView = (TextView) rootView
					.findViewById(R.id.textViewLocation);
			locationView.setText(lastLocation);

			Switch sPen = (Switch) rootView.findViewById(R.id.switchPen);
			sPen.setChecked(penState);
			sPen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked)
					{
						Stroke s = new GPSDraw.Stroke();
						strokes.add(s);
						//s.po = new PolylineOptions().width(10).color(color);
					}
					penState = isChecked;
				}
			});
			Button btnShowMap = (Button) rootView
					.findViewById(R.id.buttonShowMap);
			btnShowMap.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getFragmentManager().beginTransaction()
							.replace(R.id.container, new DrawCanvasFragment())
							.commit();
				}
			});

			Button btnNew = (Button) rootView.findViewById(R.id.buttonNew);
			btnNew.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					EditText etDrawingId = (EditText) rootView
							.findViewById(R.id.editTextDrawingId);
					etDrawingId.setText(UUID.randomUUID().toString());
				}
			});

			EditText etGroupId = (EditText) rootView
					.findViewById(R.id.editTextGroupId);
			etGroupId.setText(groupId);					
			etGroupId.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					groupId = s.toString();
					Toast.makeText(getActivity(), groupId, Toast.LENGTH_SHORT)
							.show();
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
				}
			});

			EditText etDrawingId = (EditText) rootView
					.findViewById(R.id.editTextDrawingId);
			etDrawingId.setText(drawingId);
			etDrawingId.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					drawingId = s.toString();
					Toast.makeText(getActivity(), drawingId, Toast.LENGTH_SHORT)
							.show();
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
				}
			});

	
			
			EditText et = (EditText) rootView.findViewById(R.id.editTextDebug);
			et.setText(debugMessage);

			RadioGroup rgColors = (RadioGroup) rootView
					.findViewById(R.id.radioGroupColors);
			switch(color)
			{
				case Color.BLACK:
					((RadioButton)rootView.findViewById(R.id.radioButtonColorBlack)).setChecked(true);
					break;
				case Color.BLUE:
					((RadioButton)rootView.findViewById(R.id.radioButtonColorBlue)).setChecked(true);
					break;
				case Color.GREEN:
					((RadioButton)rootView.findViewById(R.id.radioButtonColorGreen)).setChecked(true);
					break;
				case Color.RED:
					((RadioButton)rootView.findViewById(R.id.radioButtonColorRed)).setChecked(true);
					break;
				case Color.WHITE:
					((RadioButton)rootView.findViewById(R.id.radioButtonColorWhite)).setChecked(true);
					break;
					
			}
			rgColors.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					switch (checkedId) {
					case R.id.radioButtonColorBlack:
						color = Color.BLACK;
						break;
					case R.id.radioButtonColorBlue:
						color = Color.BLUE;
						break;
					case R.id.radioButtonColorGreen:
						color = Color.GREEN;
						break;
					case R.id.radioButtonColorRed:
						color = Color.RED;
						break;
					case R.id.radioButtonColorWhite:
						color = Color.WHITE;
						break;
					}
				}
			});

			if (updateLatLng)
			{
				Toast.makeText(getActivity(), lastLocation, Toast.LENGTH_SHORT)
				.show();
				locationView.setText(lastLocation);
				updateLatLng = false;
			}
			
			return rootView;
		}
	}

	private void updateUI() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if ((MainFragment.locationView != null)
						&& (lastLocation != null)) {
					MainFragment.locationView.setText(lastLocation);
				}

			}
		});
	}

	// Dialog for displaying errors
	public static class ErrorDialogFragment extends DialogFragment {
		private Dialog dialog;

		public ErrorDialogFragment() {
			super();
			dialog = null;
		}

		public void setDialog(Dialog dialog) {
			this.dialog = dialog;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return dialog;
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (result.hasResolution()) {
			try {
				result.startResolutionForResult(this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
			} catch (IntentSender.SendIntentException e) {
				e.printStackTrace();
			}
		} else {
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					result.getErrorCode(), this,
					CONNECTION_FAILURE_RESOLUTION_REQUEST);
			if (errorDialog != null) {
				ErrorDialogFragment ef = new ErrorDialogFragment();
				ef.setDialog(errorDialog);
				ef.show(getFragmentManager(), "Location Updates");
			}
		}
	}

	public void grabLocation()
	{
		if (penState)
		{
			Location cl = locationClient.getLastLocation();
			latitude = cl.getLatitude();
			longitude = cl.getLongitude();
			lastLocation = "(" + latitude + "," + longitude + ")";
		}
	}
	
	public Stroke getLastStroke()
	{
		Stroke s = null;
		if (strokes.size() > 0)
			s = strokes.get(strokes.size() - 1);
		return s;
	}
	
	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		System.err.printf("connected");
		grabLocation();
		updateUI();
		locationClient.requestLocationUpdates(new LocationRequest().setInterval(5000), new LocationListener() {
			@Override
			public void onLocationChanged(Location arg0) {
				if (penState)
				{
					latitude = arg0.getLatitude();
					longitude = arg0.getLongitude();
					lastLocation = "(" + latitude + "," + longitude + ")";
					
					Stroke s = getLastStroke();
					if (s != null)
						s.path.add(new LatLng(latitude, longitude));
					
					updateUI();
					Toast.makeText(GPSDraw.this, "requestLocationUpdates:"+lastLocation, Toast.LENGTH_SHORT).show();
				}
				polyline = null;
			}
		});
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		System.err.printf("disconnected");
	}


	
}
