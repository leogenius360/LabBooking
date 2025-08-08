package com.testlab.labbooking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.testlab.labbooking.R;
import com.testlab.labbooking.fragments.BookingsFragment;
import com.testlab.labbooking.fragments.LabsFragment;
import com.testlab.labbooking.models.User;
import com.testlab.labbooking.utils.AuthUtils;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnAdminPanel;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fabNewBooking;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (!AuthUtils.isUserLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadUserData();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        btnAdminPanel = findViewById(R.id.btnAdminPanel);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        fabNewBooking = findViewById(R.id.fabNewBooking);

        btnAdminPanel.setOnClickListener(v -> openAdminPanel());
        fabNewBooking.setOnClickListener(v -> openBookingActivity());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void loadUserData() {
        AuthUtils.getCurrentUserData(new AuthUtils.UserDataCallback() {
            @Override
            public void onUserDataReceived(User user) {
                currentUser = user;
                tvWelcome.setText("Welcome, " + user.getName() + "!");
                btnAdminPanel.setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);

                // Setup view pager AFTER user data is loaded
                setupViewPager();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(DashboardActivity.this, "Error loading user data: " + error, Toast.LENGTH_SHORT).show();
                // Still setup view pager but without user data
                setupViewPager();
            }
        });
    }

    private void setupViewPager() {
        // Create adapter that knows which fragment to show
        FragmentStateAdapter adapter = new DashboardPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Available Labs" : "My Bookings");
        }).attach();

        // Handle FAB visibility
        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                fabNewBooking.setVisibility(position == 0 ? View.VISIBLE : View.GONE);

                // Load data for the selected tab
                if (position == 1 && currentUser != null) {
                    ((BookingsFragment) getSupportFragmentManager()
                            .findFragmentByTag("f" + viewPager.getCurrentItem()))
                            .loadUserBookings(currentUser);
                }
            }
        };
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    // ViewPager adapter class
    private class DashboardPagerAdapter extends FragmentStateAdapter {
        public DashboardPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new LabsFragment();
            } else {
                BookingsFragment fragment = new BookingsFragment();
                if (currentUser != null) {
                    fragment.setUser(currentUser);
                }
                return fragment;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private void logout() {
        AuthUtils.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void openAdminPanel() {
        startActivity(new Intent(this, AdminActivity.class));
    }

    private void openBookingActivity() {
        startActivity(new Intent(this, BookingActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pageChangeCallback != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            refreshCurrentTab();
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshCurrentTab() {
        int position = viewPager.getCurrentItem();
        // Implement refresh logic if needed
        Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
    }
}