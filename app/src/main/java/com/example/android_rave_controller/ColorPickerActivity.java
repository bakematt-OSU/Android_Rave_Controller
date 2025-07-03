package com.example.android_rave_controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.listener.ColorListener;
import com.github.dhaval2404.colorpicker.model.ColorShape;
import com.example.android_rave_controller.R; // <-- Add this line

public class ColorPickerActivity extends AppCompatActivity {

    private int selectedColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_picker);

        Button selectColorButton = findViewById(R.id.selectColorButton);

        selectColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ColorPickerDialog.Builder(ColorPickerActivity.this)
                        .setTitle("Pick a Color")
                        .setColorShape(ColorShape.SQAURE)
                        .setDefaultColor(R.color.colorPrimary)
                        .setColorListener(new ColorListener() {
                            @Override
                            public void onColorSelected(int color, String colorHex) {
                                selectedColor = color;
                                // Return the selected color to the previous activity
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("selectedColor", selectedColor);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            }
                        })
                        .show();
            }
        });
    }
}