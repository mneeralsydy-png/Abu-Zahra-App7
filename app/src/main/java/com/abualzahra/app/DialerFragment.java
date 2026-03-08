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
    String number = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialer, container, false);
        tvNumber = view.findViewById(R.id.tvNumber);

        view.findViewById(R.id.btnCall).setOnClickListener(v -> {
            if(!number.isEmpty()){
                Intent i = new Intent(getActivity(), CallActivity.class);
                i.putExtra("NUMBER", number);
                startActivity(i);
            }
        });

        view.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if(number.length() > 0){
                number = number.substring(0, number.length()-1);
                tvNumber.setText(number);
            }
        });
        return view;
    }

    public void onDialClick(View view) {
        Button b = (Button)view;
        number += b.getText().toString();
        tvNumber.setText(number);
    }
}
