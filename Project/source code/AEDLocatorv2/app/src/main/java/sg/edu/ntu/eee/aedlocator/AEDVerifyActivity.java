package sg.edu.ntu.eee.aedlocator;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AEDVerifyActivity extends AppCompatActivity {
    private String titl, addr, loca, hour, timestamp, sourceUserID;
    private String userID;
    private Boolean hasImg;

    private TextView tv_addr, tv_loca, tv_hour;
    private CheckBox cb_capt, cb_addr, cb_loca, cb_hour;
    private EditText et_cmmt;
    private ImageButton imgBtn;
    private Button btn_capt;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aed_verify);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set view elements
        imgBtn = findViewById(R.id.vrf_capt);
        tv_addr = findViewById(R.id.vrf_addr);
        tv_loca = findViewById(R.id.vrf_loca);
        tv_hour = findViewById(R.id.vrf_hour);
        cb_capt = findViewById(R.id.vrf_cbx_capt);
        cb_addr = findViewById(R.id.vrf_cbx_addr);
        cb_loca = findViewById(R.id.vrf_cbx_loca);
        cb_hour = findViewById(R.id.vrf_cbx_hour);
        et_cmmt = findViewById(R.id.vrf_etxt_cmmt);

        btn_capt = findViewById(R.id.btn_submit_verify);
        btn_capt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAuth.getCurrentUser() == null) {
                    promptMessage("You have not logged in yet! Please go to main menu.");
                } else {
                    userID = mAuth.getCurrentUser().getUid();
                    if (validate()) {
                        submitVerification();
                    } else {
                        promptMessage("You did not verify anything. Please check again!");
                    }
                }
            }
        });

        // set Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        // load data from Firebase
        Intent myIntent = getIntent();
        titl = myIntent.getStringExtra("titl");
        addr = myIntent.getStringExtra("addr");
        loca = myIntent.getStringExtra("loca");
        hour = myIntent.getStringExtra("hour");
        hasImg = myIntent.getStringExtra("hasImg").equals("true");
        timestamp = myIntent.getStringExtra("timestamp");
        sourceUserID = myIntent.getStringExtra("sourceUserID");
        tv_addr.setText(addr);
        tv_loca.setText(loca);
        tv_hour.setText(hour);
        if (hasImg) {
            setPhoto();
        }
    }

    /*
     * validate inputs
     */
    private boolean validate (){
            boolean validFlag = (hasImg && cb_capt.isChecked()) || cb_addr.isChecked() || cb_hour.isChecked() || cb_loca.isChecked() || et_cmmt.getText().toString().length() > 0;
            return validFlag;
    }

    /*
     * prompt (error) message
     */
    private void promptMessage (String message){
        new AlertDialog.Builder(this)
                .setTitle("Oops")
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }


    /*
     * submit form to Firebase
     */
    private void submitVerification (){
        Map<String, Object> newVerify = new HashMap<>();
        newVerify.put("addr", cb_addr.isChecked());
        newVerify.put("loca", cb_loca.isChecked());
        newVerify.put("hour", cb_hour.isChecked());
        newVerify.put("cmmt", et_cmmt.getText().toString());
        newVerify.put("time", System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("submission")
                    .document("verification")
                    .collection(titl)
                    .document(userID)
                    .set(newVerify)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            submitFeedback(true);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            submitFeedback(false);
                        }
                    });
    }

    /*
     * display feedback upon successful submission, and update user contribution
     */
    private void submitFeedback(boolean isSuccessful) {
        if (isSuccessful) {
            Toast.makeText(getApplicationContext(), "Submitted successfully. Thank you!", Toast.LENGTH_SHORT).show();

            DatabaseReference mRef = mDatabase.getReference("user")
                    .child(userID)
                    .child("contribution")
                    .child(System.currentTimeMillis()+"");
            mRef.setValue("You helped verify an AED on " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date()));
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Submission failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * set snapshot (img) from Firebase
     */
    private void setPhoto(){
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
                        imgBtn.setImageBitmap(bitmap);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                hasImg = false;
                // TODO: set imgBtn on failure
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.finish();
        return super.onOptionsItemSelected(item);
    }
}
