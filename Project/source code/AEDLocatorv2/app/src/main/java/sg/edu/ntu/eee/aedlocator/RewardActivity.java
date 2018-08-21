package sg.edu.ntu.eee.aedlocator;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

public class RewardActivity extends AppCompatActivity {
    private ImageView imgv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imgv = findViewById(R.id.imgv_reward);
        int heart = getIntent().getIntExtra("earned", 1);

        switch (heart) {
            case -1:
                imgv.setImageResource(R.drawable.heart_bonus_collected);
                break;
            case 3:
                imgv.setImageResource(R.drawable.heart_3);
                break;
            case 2:
                imgv.setImageResource(R.drawable.heart_2);
                break;
            case 1:
            default:
                imgv.setImageResource(R.drawable.heart_1);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.finish();
        return super.onOptionsItemSelected(item);
    }
}
