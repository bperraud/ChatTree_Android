package com.chattree.chattree.home;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.chattree.chattree.R;

import static com.chattree.chattree.login.LoginActivity.EXTRA_LOGIN_DATA;

public class HomeActivity extends AppCompatActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Intent intent        = getIntent();
        String loginDataJson = intent.getStringExtra(EXTRA_LOGIN_DATA);

        textView = (TextView) findViewById(R.id.textView);
        textView.setText(loginDataJson);
    }
}
