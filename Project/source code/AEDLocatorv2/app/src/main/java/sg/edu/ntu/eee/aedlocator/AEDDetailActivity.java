package sg.edu.ntu.eee.aedlocator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class AEDDetailActivity extends AppCompatActivity {
    private String titl, addr, loca, stat, date, hour, timestamp, sourceUserID;
    private boolean hasImg;
    private TextView txtv_addr, txtv_loca, txtv_stat, txtv_hour;
    private ImageView ic_stat, aed_img;

    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aed_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // load data from intent
        Intent myIntent = getIntent();
        titl = myIntent.getStringExtra("titl");
        addr = myIntent.getStringExtra("addr");
        loca = myIntent.getStringExtra("loca");
        stat = myIntent.getStringExtra("stat");
        hour = myIntent.getStringExtra("hour");
//        date = myIntent.getStringExtra("time");
        hasImg = myIntent.getStringExtra("hasImg").equals("true");
        timestamp = myIntent.getStringExtra("timestamp");
        sourceUserID = myIntent.getStringExtra("sourceUserID");

        getSupportActionBar().setTitle(titl);

        // set FAB
        FloatingActionButton fab = findViewById(R.id.fab_detailview);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Upcoming functionality: Navigation!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // set Firebase
        mStorageRef = FirebaseStorage.getInstance().getReference();

        // display current information
        txtv_addr = findViewById(R.id.info_aed_addr);
        txtv_loca = findViewById(R.id.info_aed_loca);
        txtv_stat = findViewById(R.id.info_aed_stat);
        txtv_hour = findViewById(R.id.info_aed_hour);
        ic_stat = findViewById(R.id.ic_aed_stat);
        aed_img = findViewById(R.id.info_aed_img);

        txtv_addr.setText(addr);
        txtv_loca.setText(loca);
        txtv_hour.setText(hour);
        switch (stat.charAt(0)) {
            case 'v': // verified
                ic_stat.setImageResource(R.drawable.ic_status_green);
                txtv_stat.setText(R.string.detail_status);
                txtv_stat.setTextColor(getResources().getColor(R.color.colorVerified));
                break;
            case 'r': // reported
                ic_stat.setImageResource(R.drawable.ic_status_reported);
                txtv_stat.setText(txtv_stat.getText());
                txtv_stat.setTextColor(getResources().getColor(R.color.colorReported));
                break;
            default: // pending
                ic_stat.setImageResource(R.drawable.ic_status_pending);
                txtv_stat.setText(R.string.detail_status_pending);
                txtv_stat.setTextColor(getResources().getColor(R.color.colorPending));
                break;
        }

        if (hasImg) setPhoto();
    }

    /*
     * load snapshot (img) from Firebase
     */
    private void setPhoto() {
        StorageReference mRef = mStorageRef.child("images/submission/suggestion/"
                + sourceUserID
                + "/"
                + timestamp
                + ".jpg");

        mRef.getBytes(Long.MAX_VALUE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        aed_img.setImageBitmap(bitmap);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                hasImg = false;
                // TODO: set imgBtn on failure
            }
        });
    }

    /*
     * show tip (for demonstration only)
     * TODO: proper implementation
     */
    public void showTip(View view){
        new AlertDialog.Builder(this)
                .setTitle("*Bonus*")
                .setMessage("The first one gets bonus award. Go to VERIFY and claim your award!")
                .setNeutralButton(
                        "Got it",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                AEDDetailActivity.this.finish();
                            }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.finish();
        return super.onOptionsItemSelected(item);
    }
}
