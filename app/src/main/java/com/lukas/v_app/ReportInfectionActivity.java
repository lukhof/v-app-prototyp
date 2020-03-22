package com.lukas.v_app;

import android.content.DialogInterface;
import android.os.Bundle;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import java.util.Objects;

public class ReportInfectionActivity extends AppCompatActivity implements View.OnClickListener {

    private MaterialButton materialButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_infection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        materialButton = findViewById(R.id.reportInfectionButton);
        materialButton.setOnClickListener(this);

    }

    @Override
    public void onClick(final View v) {
        new MaterialAlertDialogBuilder(this,  R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setMessage("Möchten Sie wirklich eine Infektion melden? Bitte melden Sie nur eine Infektion, wenn ein positiver Test vorliegt.")
                .setTitle("Infektion melden")
                .setIcon(R.mipmap.ic_launcher_round)
                .setPositiveButton("Melden", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {

                    }
                }).setNegativeButton("Abbrechen", null)
                .show();
    }
}
