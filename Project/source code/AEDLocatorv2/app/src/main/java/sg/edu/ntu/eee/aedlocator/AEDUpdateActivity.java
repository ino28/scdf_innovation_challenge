package sg.edu.ntu.eee.aedlocator;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AEDUpdateActivity extends AppCompatActivity {
    final private int MAX_ADDR = 300, MAX_LOCA = 140, MAX_HOUR = 100;
    private String titl, addr, loca, hour, userID;
    private boolean hasImg;

    private Bitmap bitmap;
    private long uploadTimestamp;
    private String pictureImagePath;

    private EditText et_addr, et_loca, et_hour;
    private Button btn_capt;
    private ImageButton imgBtn;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aed_update);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        // set view elements
        imgBtn = findViewById(R.id.upd_img);
        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = timeStamp + ".jpg";
                File storageDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName;
                File file = new File(pictureImagePath);
                Uri outputFileUri = Uri.fromFile(file);
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                startActivityForResult(cameraIntent, 1);
            }
        });
        hasImg = false;
        uploadTimestamp = 0;

        et_addr = findViewById(R.id.upd_addr);
        et_loca = findViewById(R.id.upd_loca);
        et_hour = findViewById(R.id.upd_hour);
        btn_capt = findViewById(R.id.btn_capt_update);
        btn_capt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAuth.getCurrentUser() == null) {
                    promptMessage("You have not logged in yet! Please go to main menu.");
                } else {
                    userID = mAuth.getCurrentUser().getUid();
                    if (validate()) {
                        submitUpdate();
                    }
                }
            }
        });

        // load data from intent
        Intent myIntent = getIntent();
        titl = myIntent.getStringExtra("titl");
        addr = myIntent.getStringExtra("addr");
        loca = myIntent.getStringExtra("loca");
        hour = myIntent.getStringExtra("hour");
        et_addr.setText(addr);
        et_loca.setText(loca);
        et_hour.setText(hour);
    }

    /*
     * validate inputs
     */
    private boolean validate (){
        boolean validFlag = true;

        if (!hasImg && addr.equals(et_addr.getText().toString())
                && loca.equals(et_loca.getText().toString())
                && hour.equals(et_hour.getText().toString())) {
            validFlag = false;
            promptMessage("You did not make any changes. Please check again!");
        } else {
            addr = et_addr.getText().toString().trim();
            loca = et_loca.getText().toString().trim();
            hour = et_hour.getText().toString().trim();

            if (addr.length() > MAX_ADDR) {
                et_addr.setError("Sorry, this seems too long.(>)" + MAX_ADDR);
                validFlag = false;
            } else if (loca.length() > MAX_LOCA) {
                et_loca.setError("Sorry, this seems too long.(>)" + MAX_LOCA);
                validFlag = false;
            } else if (hour.length() > MAX_HOUR) {
                et_hour.setError("Sorry, this seems too long.(>)" + MAX_HOUR);
                validFlag = false;
            }
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
    private void submitUpdate(){
        // upload photo to Firebase
        uploadTimestamp = System.currentTimeMillis();
        if (hasImg) {
            uploadPhotoToFirebase();
        }

        // submit form to Firebase
        if (uploadTimestamp != 0) {
            Map<String, Object> newUpdate = new HashMap<>();
            newUpdate.put("addr", addr);
            newUpdate.put("loca", loca);
            newUpdate.put("hour", hour);
            newUpdate.put("hasImg", hasImg);
            newUpdate.put("contributorID", userID);
            newUpdate.put("time", uploadTimestamp);

            FirebaseFirestore.getInstance().collection("submission")
                    .document("update")
                    .collection(titl)
                    .document(userID)
                    .set(newUpdate)
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
    }

    /*
     * upload photo to Firebase
     */
    private void uploadPhotoToFirebase() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] file = baos.toByteArray();
        StorageReference mRef = mStorageRef.child("images/submission/update/"
                + userID
                + "/"
                + uploadTimestamp
                + ".jpg");

        mRef.putBytes(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
//                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
//                        promptMessage(downloadUrl.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        promptMessage("Photo Upload Error:\n" + exception.getMessage());
                        uploadTimestamp = 0;
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
            mRef.setValue("You helped update the database on " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date()));
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Submission failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * process and set snapshot (img) from camera result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            File imgFile = new  File(pictureImagePath);
            if(imgFile.exists()){
                Bitmap originalBM = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                try {
                    ExifInterface ei = new ExifInterface(imgFile.getPath());
                    int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED);
                    switch(orientation) {

                        case ExifInterface.ORIENTATION_ROTATE_90:
                            originalBM = rotateImage(originalBM, 90);
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_180:
                            originalBM = rotateImage(originalBM, 180);
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_270:
                            originalBM = rotateImage(originalBM, 270);
                            break;

                        case ExifInterface.ORIENTATION_NORMAL:
                        default:
                            // originalBM = originalBM;
                    }
                } catch (Exception e) {
                    //
                }

                bitmap = Bitmap.createScaledBitmap(originalBM, originalBM.getWidth()/6, originalBM.getHeight()/6, false);
                imgBtn.setImageBitmap(Bitmap.createScaledBitmap(originalBM, originalBM.getWidth()/8, originalBM.getHeight()/8, false));

                hasImg = true;
            }

        }
    }

    /*
     * process bitmap (snapshot): rotate
     */
    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.finish();
        return super.onOptionsItemSelected(item);
    }
}
