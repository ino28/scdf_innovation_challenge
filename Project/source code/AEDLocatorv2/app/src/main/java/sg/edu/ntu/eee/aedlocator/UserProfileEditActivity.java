package sg.edu.ntu.eee.aedlocator;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UserProfileEditActivity extends AppCompatActivity {
    private boolean hasImg;
    private Bitmap bitmap;
    private String userID, avatarURL;

    private ImageView iv_avatar;
    private EditText et_name, et_msg;
    private Button epBtn;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private StorageReference mStorageRef;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profileedit);

        progressDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        if (mAuth.getCurrentUser() == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Oops")
                    .setMessage("Sorry you have not logged in yet. Please go to main menu.")
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UserProfileEditActivity.this.finish();
                        }
                    })
                    .show();
        } else {
            mUser = mAuth.getCurrentUser();
            userID = mUser.getUid();

            hasImg = false;

            iv_avatar = (ImageView) findViewById(R.id.prfedit_avatar);
            et_name = (EditText) findViewById(R.id.prfedit_username);
            et_msg = (EditText) findViewById(R.id.prfedit_message);
            epBtn = (Button) findViewById(R.id.prfedit_submit);
            iv_avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editAvatar();
                }
            });
            epBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                promptMessage("will submit");
                    submitEdit();
                }
            });

            progressDialog.show();
            StorageReference mRef = mStorageRef.child("images/user/"
                    + mUser.getUid()
                    + ".jpg");
            try {
                final File localFile = File.createTempFile("Images", "bmp");
                mRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                        iv_avatar.setImageBitmap(scaleBitmap(bitmap));
                        hasImg = true;
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setDefaultImage();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            et_name.setText(mUser.getDisplayName());
            if (getIntent().getStringExtra("message")!=null) {
                et_msg.setText(getIntent().getStringExtra("message"));
            }
            progressDialog.dismiss();
        }
    }

    private void editAvatar(){
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(Intent.createChooser(photoPickerIntent, "Select picture"), 1);
    }

    private void submitEdit(){
        if (mAuth.getCurrentUser() == null) {
            promptMessage("Something's wrong. Please try logging in.");
            Intent loginUponSignUpIntent = new Intent(this, UserLoginActivity.class);
            startActivity(loginUponSignUpIntent);
        } else {
            userID = mAuth.getCurrentUser().getUid();
            if (!hasImg) {
                promptMessage("Please choose a profile photo!");
            } else if (et_name.getText()!=null && et_name.getText().toString().trim().length() > 0
                    && et_msg.getText()!=null
                    && et_msg.getText().toString().trim().length() > 0) {
                progressDialog.show();
                saveToFirebase();
                progressDialog.dismiss();
            } else {
                promptMessage("Please input something valid!");
            }
        }
    }

    private void saveToFirebase() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] file = baos.toByteArray();
        final StorageReference mRef = mStorageRef.child("images/user/"
                + userID
                + ".jpg");

        mRef.putBytes(file).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return mRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    avatarURL = downloadUri.toString();
                        String username = et_name.getText().toString().trim();
                        String userMessage = et_msg.getText().toString().trim();
                        FirebaseUser user = mAuth.getCurrentUser();

                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .setPhotoUri(Uri.parse(avatarURL))
                                .build();

                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d("FIREBASE AUTHENTICATION", "User profile updated.");
                                        }
                                    }
                                });

                        DatabaseReference mRef = mDatabase.getReference("user")
                                .child(userID)
                                .child("message");
                        mRef.setValue(userMessage);
                        onSuccessfullyUpdate(username);
                } else {
                    promptMessage("Unable to upload photo. Please try again.");
                }
            }
        });
    }

    private void onSuccessfullyUpdate(String displayName){
        Intent intent = new Intent();
        intent.putExtra("displayName", displayName);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            try {
                Uri imageUri = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                bitmap = cropScaleRotateBmp(BitmapFactory.decodeFile(picturePath), picturePath);
                iv_avatar.setImageBitmap(bitmap);
                hasImg = true;
            } catch (Exception e) {
                e.printStackTrace();
                promptMessage("Something went wrong. Please try again.");
            }
        }
    }

    private Bitmap cropScaleRotateBmp(Bitmap srcBmp, String bmpPath) {
        Bitmap dstBmp;
        final int dstSize = 96;

        try {
            File imgFile = new File(bmpPath);
            ExifInterface ei = new ExifInterface(imgFile.getPath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    dstBmp = rotateImage(srcBmp, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    dstBmp = rotateImage(srcBmp, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    dstBmp = rotateImage(srcBmp, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    dstBmp = srcBmp;
            }

            if (dstBmp.getWidth() >= dstBmp.getHeight()){
                dstBmp = Bitmap.createBitmap(
                        dstBmp,
                        dstBmp.getWidth()/2 - dstBmp.getHeight()/2,
                        0,
                        dstBmp.getHeight(),
                        dstBmp.getHeight()
                );

            } else {
                dstBmp = Bitmap.createBitmap(
                        dstBmp,
                        0,
                        dstBmp.getHeight()/2 - dstBmp.getWidth()/2,
                        dstBmp.getWidth(),
                        dstBmp.getWidth()
                );
            }

        } catch (Exception e) {
            // TODO: handle properly, e.g. set default and prompt message
            dstBmp = Bitmap.createScaledBitmap(srcBmp, dstSize, dstSize, false);
        }

        return dstBmp;
    }

    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void promptMessage (String message){
        new AlertDialog.Builder(this)
                .setTitle("Oops")
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    private Bitmap scaleBitmap(Bitmap raw) {
        final int rawSize = 96;
        Bitmap dstBmp = Bitmap.createScaledBitmap(raw, rawSize, rawSize, false);
        return dstBmp;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d("CDA", "onKeyDown Called");
            promptMessage("Please save before leaving the page");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.finish();
        return super.onOptionsItemSelected(item);
    }

    private void setDefaultImage(){
        StorageReference mRef = mStorageRef.child("images/user/default.jpg");
        try {
            final File localFile = File.createTempFile("Images", "bmp");
            mRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    iv_avatar.setImageBitmap(scaleBitmap(bitmap));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
