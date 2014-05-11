package com.example.gpsdraw;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import edu.uci.ics.ics163.gpsdrawupload.Point;
import edu.uci.ics.ics163.gpsdrawupload.StrokeManager;
import edu.uci.ics.ics163.gpsdrawupload.UploadCallback;
import android.R.string;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
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
	
	private static int FASTEST_UPDATE = 1000;
	private static int NORMAL_UPDATE = 5000;
	
	PowerManager powerManager;
	WakeLock wakeLock;
	static LatLng cheese;
	
	static StrokeManager strokeManager;

	public static List<Stroke> strokes;

	public static class Stroke {
		public PolylineOptions po;
		public Polyline pl;
		public boolean touched;
		
		public int color;
		public List<LatLng> path;
		public String name;
		public List<Point> points;
		
		public Stroke() {
			path = new LinkedList<LatLng>();
			points = new LinkedList<Point>();
			touched = false;
			name = "Stroke" + strokes.size();
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
		strokes = new LinkedList<Stroke>();
		MapsInitializer.initialize(this);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //force keep awake
		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //force portrait
		
		powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
		        "MyWakelockTag");
		wakeLock.acquire();	
		
		strokeManager = new StrokeManager();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@SuppressLint("Wakelock")
	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationClient.disconnect();
		wakeLock.release();
		

		try {
            trimCache(this);
           // Toast.makeText(this,"onDestroy " ,Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    public static void trimCache(Context context) {
        try {
           File dir = context.getCacheDir();
           if (dir != null && dir.isDirectory()) {
              deleteDir(dir);
           }
        } catch (Exception e) {
           // TODO: handle exception
        }
     }

     public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
           String[] children = dir.list();
           for (int i = 0; i < children.length; i++) {
              boolean success = deleteDir(new File(dir, children[i]));
              if (!success) {
                 return false;
              }
           }
        }

        
        // The directory is now empty so delete it
        return dir.delete();

		
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
	 * ColorWheel Fragment
	 */
	public static class ColorWheelFragment extends Fragment {
		ColorWheel cw;
		ColorWheelChoice cwc;
		public ColorWheelFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			final View rootView = inflater.inflate(R.layout.fragment_colorwheel,
					container, false);

			cwc = (ColorWheelChoice) rootView
					.findViewById(R.id.colorWheelChoice1);
			cwc.setColor(Color.red(color)/255.0f, Color.green(color)/255.0f, Color.blue(color)/255.0f);
			
			cw = (ColorWheel) rootView
					.findViewById(R.id.colorWheel1);
			cw.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					color = cw.bmp.getPixel((int)event.getX(), (int)event.getY());
					cwc.setColor(Color.red(color)/255.0f, Color.green(color)/255.0f, Color.blue(color)/255.0f);
					
					cwc.invalidate();
					return false;
				}
			});	
			
		

			Button btnBack = (Button) rootView
					.findViewById(R.id.buttonColorWheelBack);
			btnBack.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getFragmentManager().beginTransaction()
							.replace(R.id.container, new MainFragment())
							.commit();
				}
			});
			return rootView;
		}

	}	
	
	/**
	 * Google Maps fragment
	 */
	public static class GoogleMapFragment extends Fragment {
		GoogleMap gm;
		Polyline pl;
		private Handler handler = new Handler();

		public GoogleMapFragment() {

		}

		private Runnable runnable = new Runnable() {
			   @Override
			   public void run() {
				    if (pl != null)
				    {
						Stroke s = getLastStroke();
						List<LatLng> ps = pl.getPoints();
						if (cheese != null)
						{
							ps.add(cheese);
							cheese = null;
						}
						pl.setPoints(ps);
						Toast.makeText(getActivity(), "auto updated path",
								Toast.LENGTH_SHORT).show();
				    }
			        handler.postDelayed(this, FASTEST_UPDATE);
			   }
			};		
		
		@Override
		public void onStart() {
			super.onStart();
			handler.postDelayed(runnable, FASTEST_UPDATE);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			final View rootView = inflater.inflate(
					R.layout.fragment_drawcanvas, container, false);
			switch (GooglePlayServicesUtil
					.isGooglePlayServicesAvailable(getActivity())) {
			case ConnectionResult.SUCCESS:
				MapFragment mf = ((MapFragment) getFragmentManager().findFragmentById(
						R.id.map));
				
				gm = mf.getMap();
				gm.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				LatLng place = new LatLng(latitude, longitude);
				gm.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 16));
				gm.getUiSettings().setScrollGesturesEnabled(false);
				gm.getUiSettings().setZoomGesturesEnabled(false);

				
				//draw previous strokes
				for (GPSDraw.Stroke s : strokes)
				{
					if (s.touched)
						pl = s.pl = gm.addPolyline(s.po.addAll(s.pl.getPoints()));
				}

				Stroke s = getLastStroke();
				if (s != null && s.touched == false)
				{
					s.touched = true;
					s.po = new PolylineOptions().width(10).color(color);
					s.pl = gm.addPolyline(s.po);
				}
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
						cheese = null;
					}
				}
			});

			Button btnBack = (Button) rootView
					.findViewById(R.id.buttonMapBack);
			btnBack.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pl = null;
					getFragmentManager().beginTransaction()
							.replace(R.id.container, new MainFragment())
							.commit();
				}
			});
			return rootView;
		}

		protected void runOnUiThread(Runnable runnable) {
			// TODO Auto-generated method stub
			
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
							.replace(R.id.container, new GoogleMapFragment())
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
			
			Button btnPickColor = (Button) rootView.findViewById(R.id.buttonPickColor);
			btnPickColor.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getFragmentManager().beginTransaction()
							.replace(R.id.container, new ColorWheelFragment())
							.commit();
				}
			});	
			
			
			Button btnUpload = (Button) rootView
					.findViewById(R.id.buttonUpload);
			btnUpload.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!penState)
					{
						strokeManager.upload(groupId, drawingId);
						Toast.makeText(getActivity(), "Uploaded",
								Toast.LENGTH_SHORT).show();
					}
					else
					{
						
						Toast.makeText(getActivity(), "Please Lift the Pen",
								Toast.LENGTH_SHORT).show();
						
					}
					
				}
			});
			
			
			
			if (updateLatLng)
			{
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
	
	public static Stroke getLastStroke()
	{
		Stroke s = null;
		if (strokes.size() > 0)
			s = strokes.get(strokes.size() - 1);
		return s;
	}
	
	@Override
	public void onConnected(Bundle arg0) {
		grabLocation();
		updateUI();
		locationClient.requestLocationUpdates(new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(NORMAL_UPDATE).setFastestInterval(FASTEST_UPDATE), new LocationListener() {
			@Override
			public void onLocationChanged(Location arg0) {
				if (penState)
				{
					latitude = arg0.getLatitude();
					longitude = arg0.getLongitude();
					long time = arg0.getTime();
					lastLocation = "(" + latitude + "," + longitude + ")";
					
					cheese = new LatLng(latitude, longitude);
					
					Stroke s = getLastStroke();
					if (s != null)
					{
						s.path.add(new LatLng(latitude, longitude));
						Point p = new Point(time, latitude, longitude);
						s.points.add(p);
						strokeManager.addPoint(s.name, p);
						strokeManager.setStrokeColor(s.name, Color.red(color),Color.green(color), Color.blue(color));
					
					updateUI();
					}
				}
			}
		});
	}

	@Override
	public void onDisconnected() {
	}
}
