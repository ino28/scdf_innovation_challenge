package sg.edu.ntu.eee.aedlocator;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

public class AEDSuggestActivity extends AppCompatActivity {
    final private int MAX_ADDR = 300, MAX_LOCA = 140, MAX_HOUR = 100;
    private String addr, loca, hour, userID;
    private boolean hasImg;

    private Bitmap bitmap;
    private long uploadTimestamp;
    private String pictureImagePath;

    private EditText et_addr, et_loca, et_hour;
    private Button btn_capt;
    private ImageButton imgBtn;

    private FusedLocationProviderClient client;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aed_suggest);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set location service client
        client = LocationServices.getFusedLocationProviderClient(this);

        // set Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        // set view elements
        imgBtn = findViewById(R.id.new_aed_img);
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

        et_addr = findViewById(R.id.new_aed_addr);
        et_loca = findViewById(R.id.new_aed_loca);
        et_hour = findViewById(R.id.new_aed_hour);
        et_addr.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    et_addr.setHint("unit/building/block/street");
                } else {
                    et_addr.setHint("");
                }
            }
        });
        et_loca.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    et_loca.setHint("floor/description");
                } else {
                    et_loca.setHint("");
                }
            }
        });
        et_hour.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    et_hour.setHint("operating/opening hours");
                } else {
                    et_hour.setHint("");
                }
            }
        });

        btn_capt = findViewById(R.id.btn_capt_suggest);
        btn_capt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAuth.getCurrentUser() == null) {
                    promptMessage("You have not logged in yet! Please go to main menu.");
                } else {
                    userID = mAuth.getCurrentUser().getUid();
                    submitSuggestAfterValidation();
                }
            }
        });
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
    public void submitSuggestAfterValidation () {

        addr = et_addr.getText().toString().trim();
        loca = et_loca.getText().toString().trim();
        hour = et_hour.getText().toString().trim();

        if (addr.isEmpty()) {
            et_addr.setError("Address cannot be empty");
        } else if (loca.isEmpty()) {
            et_loca.setError("Location cannot be empty");
        } else if (addr.length() > MAX_ADDR) {
            et_addr.setError("Sorry, this seems too long.(>)" + MAX_ADDR);
        } else if (loca.length() > MAX_LOCA) {
            et_loca.setError("Sorry, this seems too long.(>)" + MAX_LOCA);
        } else if (hour.length() > MAX_HOUR) {
            et_hour.setError("Sorry, this seems too long.(>)" + MAX_HOUR);
        } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // location permission not granted
            promptMessage("Unable to detect current AED location. Please make sure location permission is turned on in SETTING");
        } else {
            // if passed all previous validation
            Task locationResult = client.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Set the map's camera position to the current location of the device.
                        Location mLastKnownLocation = (Location) task.getResult();
                        double lng = mLastKnownLocation.getLongitude();
                        double lat = mLastKnownLocation.getLatitude();

                        // upload photo to Firebase
                        uploadTimestamp = System.currentTimeMillis();
                        if (hasImg) {
                            uploadPhotoToFirebase();
                        }

                        // submit form to Firebase
                        if (uploadTimestamp != 0) {
                            Map<String, Object> newSuggest = new HashMap<>();
                            newSuggest.put("addr", addr);
                            newSuggest.put("loca", loca);
                            newSuggest.put("hour", hour);
                            newSuggest.put("lng", lng);
                            newSuggest.put("lat", lat);
                            newSuggest.put("hasImg", hasImg);
                            newSuggest.put("contributorID", userID);
                            newSuggest.put("time", uploadTimestamp);

                            // add to aed_init for marker update
                            // TODO: only add when record submission succeeds
                            FirebaseFirestore.getInstance().collection("aed_crowd")
                                    .document(userID + "@"+lng + ", " + lat)
                                    .set(newSuggest)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("AEDSuggestActivity", "Added a newly suggested aed to Firebase Storage");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("AEDSuggestActivity", "ERROR! failed to add the newly suggested aed to Firebase Storage");
                                        }
                                    });

                            // add to submission record
                            FirebaseFirestore.getInstance().collection("submission")
                                    .document("suggestion")
                                    .collection(userID)
                                    .document(lng + ", " + lat)
                                    .set(newSuggest)
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
        StorageReference mRef = mStorageRef.child("images/submission/suggestion/"
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
            mRef.setValue("You suggested an AED on " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date()));
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
