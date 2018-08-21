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
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

public class UserProfileActivity extends AppCompatActivity {
    final int EDIT_PROFILE_REQUEST = 1;
    private String userID;

    private ImageButton editBtn;
    private ImageView iv_avatar;
    private TextView tv_name, tv_msg, tv_lvl, tv_hrt, tv_cont;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private StorageReference mStorageRef;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
                            UserProfileActivity.this.finish();
                        }
                    })
                    .show();
        } else {
            mUser = mAuth.getCurrentUser();
            userID = mUser.getUid();

            tv_lvl = (TextView) findViewById(R.id.profile_lvl);
            tv_hrt = (TextView) findViewById(R.id.profile_hrt);
            tv_cont = (TextView) findViewById(R.id.profile_cont);
            DatabaseReference mDatabaseRef = mDatabase.getReference("user")
                    .child(userID);
            mDatabaseRef.child("level")
                    .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Long value = dataSnapshot.getValue(Long.class);
                    tv_lvl.setText("Lv." + value);
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    tv_lvl.setText("?");
                    Log.w("FirebaseDatabase", "Failed to load user level.", error.toException());
                }
            });
            mDatabaseRef.child("heart")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Long value = dataSnapshot.getValue(Long.class);
                            tv_hrt.setText(Long.toString(value));
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            tv_hrt.setText("?");
                            Log.w("FirebaseDatabase", "Failed to load user point.", error.toException());
                        }
                    });
            mDatabaseRef.child("contribution")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            Long value = dataSnapshot.getChildrenCount();
                            tv_cont.setText(Long.toString(value));
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            tv_cont.setText("0");
                            Log.w("FirebaseDatabase", "Failed to load user contribution count.", error.toException());
                        }
                    });


            iv_avatar = (ImageView) findViewById(R.id.profile_avatar);
            tv_name = (TextView) findViewById(R.id.profile_username);
            tv_msg = (TextView) findViewById(R.id.profile_message);
            refreshProfile();
        }
    }

    public void toLvl(View view){
        Intent toIntent = new Intent(getApplicationContext(), UserLevelActivity.class);
        startActivity(toIntent);
    }

    public void toHrt(View view) {
        Intent toIntent = new Intent(getApplicationContext(), UserPointActivity.class);
        startActivity(toIntent);
    }

    public void toCont(View view){
        Intent toIntent = new Intent(getApplicationContext(), MyContributionActivity.class);
        startActivity(toIntent);
    }

    public void editProfile(View view) {
        new AlertDialog.Builder(this)
                .setMessage("Do you want to edit your profile?")
                .setPositiveButton(
                        android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent profileEditIntent = new Intent(getApplicationContext(), UserProfileEditActivity.class);
                                profileEditIntent.putExtra("message", tv_msg.getText());
                                startActivityForResult(profileEditIntent, EDIT_PROFILE_REQUEST);
                            }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            Toast.makeText(getApplicationContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
            refreshProfile();
        }
    }

    private void refreshProfile(){
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        StorageReference mRef = mStorageRef.child("images/user/"
                + mUser.getUid()
                + ".jpg");
        try {
            final File localFile = File.createTempFile("Images", "bmp");
            mRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener< FileDownloadTask.TaskSnapshot >() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    iv_avatar.setImageBitmap(scaleRoundBitmap(bitmap));
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
        tv_name.setText(mUser.getDisplayName());
        DatabaseReference mDatabaseRef = mDatabase.getReference("user")
                .child(userID)
                .child("message");
        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String message = dataSnapshot.getValue(String.class);
                tv_msg.setText(message);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                tv_msg.setText("(no user message found)");
                Log.w("FirebaseDatabase", "Failed to load user message.", error.toException());
            }
        });

        progressDialog.dismiss();
    }

    private void promptMessage(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Oops")
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    private Bitmap scaleRoundBitmap(Bitmap raw) {
        final float round = 48;
        final int rawSize = 96;
        raw = Bitmap.createScaledBitmap(raw, rawSize, rawSize, false);
        Bitmap dstBmp = Bitmap.createBitmap(rawSize, rawSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dstBmp);
        canvas.drawARGB(0, 0, 0, 0);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#000000"));

        final Rect rect = new Rect(0, 0, rawSize, rawSize);
        final RectF rectF = new RectF(rect);

        canvas.drawRoundRect(rectF, round, round, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(raw, rect, rect, paint);

        return dstBmp;
    }

    private void setDefaultImage(){
        StorageReference mRef = mStorageRef.child("images/user/default.jpg");
        try {
            final File localFile = File.createTempFile("Images", "bmp");
            mRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener< FileDownloadTask.TaskSnapshot >() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    iv_avatar.setImageBitmap(scaleRoundBitmap(bitmap));
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
    }
}
