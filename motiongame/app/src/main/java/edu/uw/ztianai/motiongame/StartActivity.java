package edu.uw.ztianai.motiongame;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Button start = (Button) findViewById(R.id.btnStart);

        //Allow user to read instruction and then start the game
        //Also allow user to type in the number of obstacle balls they want
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                String num = ((EditText)findViewById(R.id.extraBallNum)).getText().toString();
                intent.putExtra("edu.uw.ztianai.motiongame.num", num);
                startActivity(intent);
            }
        });
    }
}
