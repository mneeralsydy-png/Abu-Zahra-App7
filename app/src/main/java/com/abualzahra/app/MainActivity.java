package com.abualzahra.app;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // تحميل الصفحة الافتراضية (لوحة المفاتيح)
        loadFragment(new DialerFragment());
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int id = item.getItemId();

                if (id == R.id.nav_dialer) {
                    selectedFragment = new DialerFragment();
                } else if (id == R.id.nav_logs) {
                    selectedFragment = new LogsFragment(); // تحتاج لإنشائه
                } else if (id == R.id.nav_contacts) {
                    selectedFragment = new ContactsFragment(); // تحتاج لإنشائه
                } else if (id == R.id.nav_msgs) {
                    selectedFragment = new MessagesFragment(); // تحتاج لإنشائه
                } else if (id == R.id.nav_more) {
                    selectedFragment = new MoreFragment(); // تحتاج لإنشائه
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }
                return true;
            };

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
