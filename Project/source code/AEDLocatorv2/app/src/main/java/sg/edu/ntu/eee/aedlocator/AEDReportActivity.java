package sg.edu.ntu.eee.aedlocator;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AEDReportActivity extends AppCompatActivity {
    final private int MAX_NAME = 30;
    private String userID;

    private String name, phone, email, titl;

    private Button rptBtn;
    private EditText et_name, et_phone, et_email, et_reason;
    private CheckBox cb_missing, cb_relocated, cb_damaged, cb_other;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aed_report);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set view elements
        et_name = findViewById(R.id.rpt_name);
        et_phone = findViewById(R.id.rpt_phone);
        et_email = findViewById(R.id.rpt_email);
        et_reason = findViewById(R.id.rpt_cmmt_reason);
        cb_missing = findViewById(R.id.rpt_cbx_missing);
        cb_relocated = findViewById(R.id.rpt_cbx_relocated);
        cb_damaged = findViewById(R.id.rpt_cbx_damaged);
        cb_other = findViewById(R.id.rpt_cbx_other);

        rptBtn = findViewById(R.id.btn_submit_report);
        rptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAuth.getCurrentUser() == null) {
                    promptMessage("You have not logged in yet! Please go to main menu.");
                } else {
                    userID = mAuth.getCurrentUser().getUid();
                    if (validate()) {
                        // TODO: camera activity

                        submitReport();
                    }
                }
            }
        });

        // load data from intent
        Intent myIntent = getIntent();
        titl = myIntent.getStringExtra("titl");
        if (titl==null) titl = "unknown";

        // set Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
    }

    /*
     * validate inputs
     */
    private boolean validate(){
        boolean validFlag = false;

            name = et_name.getText().toString().trim();
            phone = et_phone.getText().toString().trim();
            email = et_email.getText().toString().trim();

            if (name.isEmpty() || name.length() > MAX_NAME) {
                et_name.setError("Please enter valid name");
            } else if (phone.isEmpty() || !Patterns.PHONE.matcher(phone).matches()) {
                et_phone.setError("Please enter valid phone");
            } else if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                et_email.setError("Please enter valid email");
            } else if (!(cb_missing.isChecked() || cb_relocated.isChecked()
                    || cb_damaged.isChecked()
                    || (cb_other.isChecked() && et_reason.getText().toString().length() > 0))) {
                promptMessage("Please select or specify the reason for reporting.");
            } else {
                validFlag = true;
            }

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
    private void submitReport(){
        Map<String, Object> newReport = new HashMap<>();
        newReport.put("name", name);
        newReport.put("phone", phone);
        newReport.put("email", email);
        newReport.put("missing", cb_missing.isChecked());
        newReport.put("relocated", cb_relocated.isChecked());
        newReport.put("damaged", cb_damaged.isChecked());
        newReport.put("other", cb_other.isChecked());
        if (cb_other.isChecked()) {
            newReport.put("reason", et_reason.getText().toString());
        }
        newReport.put("time", System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("submission")
                    .document("report")
                    .collection(titl)
                    .document(userID)
                    .set(newReport)
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
            mRef.setValue("You submitted a report on " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date()));
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Submission failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.finish();
        return super.onOptionsItemSelected(item);
    }
}
