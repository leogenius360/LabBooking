package com.testlab.labbooking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

    private static final String TAG = "DashboardActivity";

    private TextView tvWelcome;
    private Button btnAdminPanel;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fabNewBooking;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;
    private TabLayoutMediator tabLayoutMediator;

    private User currentUser;
    private DashboardPagerAdapter adapter;

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
                updateUIWithUserData(user);
                setupViewPager();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(DashboardActivity.this, "Error loading user data: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading user data: " + error);
                // Still setup view pager but without user data
                setupViewPager();
            }
        });
    }

    private void updateUIWithUserData(User user) {
        if (user != null) {
            tvWelcome.setText("Welcome, " + user.getName() + "!");
            btnAdminPanel.setVisibility(user.isAdmin() ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    private void setupViewPager() {
        // Create adapter with current user context
        adapter = new DashboardPagerAdapter(this, currentUser);
        viewPager.setAdapter(adapter);

        // Connect TabLayout with ViewPager
        tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Available Labs" : "My Bookings");
        });
        tabLayoutMediator.attach();

        // Handle FAB visibility and pass user context to fragments
        setupPageChangeCallback();
    }

    private void setupPageChangeCallback() {
        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Handle FAB visibility
                fabNewBooking.setVisibility(position == 0 ? android.view.View.VISIBLE : android.view.View.GONE);

                // Pass user context to fragments when they become active
                passUserContextToFragment(position);
            }
        };
        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        // Pass user context to initial fragment
        viewPager.postDelayed(() -> passUserContextToFragment(viewPager.getCurrentItem()), 100);
    }

    /**
     * Pass current user context to the active fragment
     */
    private void passUserContextToFragment(int position) {
        if (currentUser == null) {
            Log.w(TAG, "Cannot pass user context: currentUser is null");
            return;
        }

        try {
            // Get the fragment using the ViewPager2 tag pattern
            Fragment fragment = getSupportFragmentManager()
                    .findFragmentByTag("f" + viewPager.getCurrentItem());

            if (fragment == null) {
                // Try alternative approach
                fragment = getSupportFragmentManager()
                        .findFragmentByTag("f" + viewPager.getId() + ":" + position);
            }

            if (fragment instanceof LabsFragment && position == 0) {
                ((LabsFragment) fragment).setCurrentUser(currentUser);
                Log.d(TAG, "Passed user context to LabsFragment");
            } else if (fragment instanceof BookingsFragment && position == 1) {
                ((BookingsFragment) fragment).loadUserBookings(currentUser);
                Log.d(TAG, "Passed user context to BookingsFragment");
            } else {
                Log.w(TAG, "Fragment not found or not ready, trying again...");
                // Retry after a delay
                viewPager.postDelayed(() -> passUserContextToFragment(position), 200);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error passing user context to fragment", e);
        }
    }

    // ViewPager adapter class
    private static class DashboardPagerAdapter extends FragmentStateAdapter {
        private final User userForFragments;

        public DashboardPagerAdapter(FragmentActivity fa, User user) {
            super(fa);
            this.userForFragments = user;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                LabsFragment labsFragment = new LabsFragment();
                // Set user context if available
                if (userForFragments != null) {
                    // Note: We'll set this after the fragment is created via the callback
                }
                return labsFragment;
            } else {
                // Create BookingsFragment with user context
                return BookingsFragment.newInstance(userForFragments);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
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

    /**
     * Refresh the currently active fragment
     */
    public void refreshCurrentFragment() {
        int currentPosition = viewPager.getCurrentItem();

        try {
            Fragment fragment = getSupportFragmentManager()
                    .findFragmentByTag("f" + viewPager.getCurrentItem());

            if (fragment == null) {
                fragment = getSupportFragmentManager()
                        .findFragmentByTag("f" + viewPager.getId() + ":" + currentPosition);
            }

            if (fragment instanceof LabsFragment && currentPosition == 0) {
                ((LabsFragment) fragment).refreshData();
                Toast.makeText(this, "Refreshing labs...", Toast.LENGTH_SHORT).show();
            } else if (fragment instanceof BookingsFragment && currentPosition == 1 && currentUser != null) {
                ((BookingsFragment) fragment).loadUserBookings(currentUser);
                Toast.makeText(this, "Refreshing bookings...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing current fragment", e);
            Toast.makeText(this, "Error refreshing data", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Refresh bookings fragment specifically
     */
    public void refreshBookingsFragment() {
        if (currentUser != null) {
            try {
                Fragment fragment = getSupportFragmentManager()
                        .findFragmentByTag("f" + viewPager.getId() + ":" + 1);

                if (fragment instanceof BookingsFragment) {
                    ((BookingsFragment) fragment).loadUserBookings(currentUser);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing bookings fragment", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh current fragment when returning to activity
        if (currentUser != null && viewPager != null) {
            viewPager.postDelayed(this::refreshCurrentFragment, 100);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pageChangeCallback != null && viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
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
            refreshCurrentFragment();
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}