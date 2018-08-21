package sg.edu.ntu.eee.aedlocator;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.List;


public class MapFragment extends Fragment implements OnMapReadyCallback {
    GoogleMap mMap;
    private Context mContext;
    private String titl, addr, loca, stat, timestamp, hour, hasImg, sourceUserID;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.fragment_map, container, false);

        return v;
    }

    @Override
    public void onViewCreated(View view,  @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContext = getActivity();

        SupportMapFragment mapFragment = (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // retrieve AED data points from Firebase
        retrieveAEDs(mMap);

        // for debug: original point
        LatLng ntuPosition = new LatLng(1.346217, 103.68230);

        MarkerOptions option = new MarkerOptions();
        option.position(ntuPosition).title("NTU Campus");
        mMap.addMarker(option).setTag(Arrays.asList("o"));

        // set map UI
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ntuPosition, 18));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setPadding(5,100,5,100);

        // set my_location service
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        // listener: show preview upon marker click
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
                if (!((List<String>) marker.getTag()).get(0).equals("o")) {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(mContext);
                View parentView = getLayoutInflater().inflate(R.layout.aed_preview, null);
                bottomSheetDialog.setContentView(parentView);
                BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) parentView.getParent());
                bottomSheetBehavior.setPeekHeight(
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 125, getResources().getDisplayMetrics()));

                WindowManager.LayoutParams windowParams = bottomSheetDialog.getWindow().getAttributes();
                bottomSheetDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                bottomSheetDialog.show();

                TextView titlPreview = parentView.findViewById(R.id.aed_preview_titl);
                TextView addrPreview = parentView.findViewById(R.id.aed_preview_addr);
                TextView descPreview = parentView.findViewById(R.id.aed_preview_desc);

                titl = marker.getTitle();
                List<String> markerInfo = (List<String>) marker.getTag();
                addr = markerInfo.get(0) + ", " + markerInfo.get(1);
                loca = markerInfo.get(2);
                stat = markerInfo.get(3);
                hour = markerInfo.get(4);
                hasImg = markerInfo.get(5);
                timestamp = markerInfo.get(6);
                sourceUserID = markerInfo.get(7);

                titlPreview.setText(titl);
                addrPreview.setText(markerInfo.get(0));
                descPreview.setText(loca);

                // set buttons
                // NOTE: no names are given at this point in case of future change in order
                Button b1 = parentView.findViewById(R.id.button1);
                Button b2 = parentView.findViewById(R.id.button2);
                Button b3 = parentView.findViewById(R.id.button3);
                Button b4 = parentView.findViewById(R.id.button4);

                b1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent detailViewIntent = new Intent(mContext, AEDDetailActivity.class);
                        detailViewIntent = putAEDExtras(detailViewIntent);
                        mContext.startActivity(detailViewIntent);
                    }
                });
                b2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent detailVerifyIntent = new Intent(mContext, AEDVerifyActivity.class);
                        detailVerifyIntent = putAEDExtras(detailVerifyIntent);
                        mContext.startActivity(detailVerifyIntent);
                    }
                });
                b3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent detailUpdateIntent = new Intent(mContext, AEDUpdateActivity.class);
                        detailUpdateIntent = putAEDExtras(detailUpdateIntent);
                        mContext.startActivity(detailUpdateIntent);
                    }
                });
                b4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent detailReportIntent = new Intent(mContext, AEDReportActivity.class);
                        detailReportIntent = putAEDExtras(detailReportIntent);
                        mContext.startActivity(detailReportIntent);
                    }
                });
            }
            }
        });

    }

    private void retrieveAEDs(GoogleMap googleMap) {
        mMap = googleMap;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // load system data points
        db.collection("aed_init")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Log.d("", document.getId() + " => " + document.getData());
                                titl = "No." + document.getData().get("AED ID");

                                Double thisLat = Double.parseDouble(document.getData().get("Latitude").toString());
                                Double thisLog = Double.parseDouble(document.getData().get("Longitude").toString());

                                // TODO: handle too many markers if zoom out-> cluster?
//                                Map thisAED = document.getData();

                                int markerIcon;
                                switch (document.getData().get("status").toString().charAt(0)) {
                                    case 'v':
                                        markerIcon = R.drawable.aed_logo_red;
                                        break;
                                    case 'r':
                                        markerIcon = R.drawable.aed_logo_wait;
                                        break;
                                    default:
                                        markerIcon = R.drawable.aed_logo_grey;
                                        break;
                                }
                                            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(thisLat, thisLog))
                    .title(titl)
                    .icon(BitmapDescriptorFactory.fromResource(markerIcon)))
                    .setTag(Arrays.asList(
                            (document.getData().get("ADDRESSUNITNUMBER").toString().length()>0?
                                    document.getData().get("ADDRESSUNITNUMBER") + " " : "")
                                    + document.getData().get("ADDRESSBUILDINGNAME"),
                            document.getData().get("ADDRESSSTREETNAME") + " " + document.getData().get("ADDRESSBLOCKHOUSENUMBER"),
                            document.getData().get("NAME"),
                            document.getData().get("status"),
                            document.getData().get("Operating Hours"),
                            "false",
                            "0",
                            "0"
                    ));
                            }
                        } else {
                            Log.w("", "Error getting documents.", task.getException());
                        }
                    }
                });

        // load crowdsourced (user-generated) data points
        db.collection("aed_crowd")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Log.d("", document.getId() + " => " + document.getData());
                                titl = "User suggested";

                                Double thisLat = Double.parseDouble(document.getData().get("lat").toString());
                                Double thisLog = Double.parseDouble(document.getData().get("lng").toString());

                                int markerIcon = R.drawable.aed_logo_grey;
                                mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(thisLat, thisLog))
                                        .title(titl)
                                        .icon(BitmapDescriptorFactory.fromResource(markerIcon)))
                                        .setTag(Arrays.asList(
                                                document.getData().get("addr"),
                                                "",
                                                document.getData().get("loca"),
                                                "pending",
                                                document.getData().get("hour"),
                                                document.getData().get("hasImg").toString(),
                                                document.getData().get("time")+"",
                                                document.getData().get("contributorID")
                                        ));
                            }
                        } else {
                            Log.w("", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    public void setLocationOnSearch(Place place, Context context){
            LatLng latLng = place.getLatLng();
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private Intent putAEDExtras(Intent intent) {
        intent.putExtra("titl", titl);
        intent.putExtra("addr", addr);
        intent.putExtra("loca", loca);
        intent.putExtra("stat", stat);
        intent.putExtra("hour", hour);
        intent.putExtra("hasImg", hasImg);
        intent.putExtra("timestamp", timestamp);
        intent.putExtra("sourceUserID", sourceUserID);
        return intent;
    }
}
