package com.abualzahra.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DialerFragment extends Fragment {

    TextView tvNumber;
    String currentNumber = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialer, container, false);

        tvNumber = view.findViewById(R.id.tvNumber);
        
        view.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (currentNumber.length() > 0) {
                currentNumber = currentNumber.substring(0, currentNumber.length() - 1);
                tvNumber.setText(currentNumber);
            }
        });
        
        view.findViewById(R.id.btnCall).setOnClickListener(v -> {
            if (!currentNumber.isEmpty()) {
                Intent intent = new Intent(getActivity(), CallActivity.class);
                intent.putExtra("NUMBER", currentNumber);
                startActivity(intent);
            }
        });

        return view;
    }

    public void onDialClick(View view) {
        Button button = (Button) view;
        currentNumber += button.getText().toString();
        if (tvNumber != null) {
            tvNumber.setText(currentNumber);
        }
    }
}
