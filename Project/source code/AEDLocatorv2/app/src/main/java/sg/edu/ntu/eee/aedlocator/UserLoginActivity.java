package sg.edu.ntu.eee.aedlocator;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserLoginActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnLogin;
    private EditText etxt_email, etxt_password;
    private TextView txtv_signup;

    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        // set Firebase
        mAuth = FirebaseAuth.getInstance();

        // set view elements
        etxt_email = findViewById(R.id.login_email);
        etxt_password = findViewById(R.id.login_password);

        btnLogin = findViewById(R.id.email_log_in_button);
        btnLogin.setOnClickListener(this);

        txtv_signup = findViewById(R.id.txtv_sign_up);
        txtv_signup.setOnClickListener(this);

        progressDialog = new ProgressDialog(this);

        // load user email from sign-up page
        if (getIntent().getStringExtra("userEmail")!=null) {
            etxt_email.setText(getIntent().getStringExtra(("userEmail")));
            etxt_password.requestFocus();
        }
    }

    /*
     * login
     */
    private void userLogin() {
        String email = etxt_email.getText().toString().trim();
        String password = etxt_password.getText().toString().trim();

        if (!validateEmailPassword(email, password)) {
            return;
        }

        progressDialog.setMessage("Yeahhh we are loading...");
        progressDialog.show();

        // Log in user
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("My FIREBASE 2018", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user, null);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("My FIREBASE 2018", "createUserWithEmail:failure", task.getException());
                            updateUI(null, task.getException().getMessage());
                        }
                    }
                });
    }

    /*
     * validate inputs
     */
    private boolean validateEmailPassword(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etxt_email.setError(getString(R.string.error_field_required));
            etxt_email.requestFocus();
            return false;
        } else if (!email.contains("@") || !email.contains(".")) {
            etxt_email.setError(getString(R.string.error_invalid_email));
            etxt_email.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(password) || password.length()<6) {
            etxt_password.setError(getString(R.string.error_invalid_password));
            etxt_password.requestFocus();
            return false ;
        }
        return true;
    }

    /*
     * update UI
     */
    private void updateUI(FirebaseUser user, String errormsg) {
        // Update ui if the user is currently signed in
        progressDialog.dismiss();
        if (errormsg==null) {
            if (mAuth.getCurrentUser().getDisplayName()!=null){
                Toast.makeText(UserLoginActivity.this, "Welcome back, " + mAuth.getCurrentUser().getDisplayName() + "!",
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Intent profileEditIntent = new Intent(getApplicationContext(), UserProfileEditActivity.class);
                startActivityForResult(profileEditIntent, 1);
            }
        } else {
            Toast.makeText(UserLoginActivity.this, "Login failed: " + errormsg,
                    Toast.LENGTH_LONG).show();
        }
    }

    /*
     * handle clicks
     */
    @Override
    public void onClick(View view) {
        if (view == btnLogin) {
            userLogin();
        } else if (view == txtv_signup) {
            finish();
            startActivity(new Intent(this, UserSignUpActivity.class));
        }
    }

    /*
     * prepare activity result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String username = data.getStringExtra("displayName");
            Toast.makeText(UserLoginActivity.this, "Welcome back, " + username + "!",
                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.putExtra("displayName", username);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            etxt_email.setText("");
            etxt_password.setText("");
        }
    }
}
