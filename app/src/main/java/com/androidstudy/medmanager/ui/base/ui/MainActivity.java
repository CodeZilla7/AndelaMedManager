package com.androidstudy.medmanager.ui.base.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.androidstudy.medmanager.R;
import com.androidstudy.medmanager.data.model.Medicine;
import com.androidstudy.medmanager.data.model.MenuView;
import com.androidstudy.medmanager.data.model.User;
import com.androidstudy.medmanager.databinding.ActivityMainBinding;
import com.androidstudy.medmanager.ui.auth.ui.AuthActivity;
import com.androidstudy.medmanager.ui.base.adapter.CustomItemClickListener;
import com.androidstudy.medmanager.ui.base.adapter.DailyMedicineStatisticsAdapter;
import com.androidstudy.medmanager.ui.base.adapter.MainDashboardAdapter;
import com.androidstudy.medmanager.ui.base.viewmodel.MainViewModel;
import com.androidstudy.medmanager.ui.medicine.ui.AddMedicineActivity;
import com.androidstudy.medmanager.ui.medicine.viewmodel.MedicineViewModel;
import com.androidstudy.medmanager.util.CirclePagerIndicatorDecoration;
import com.androidstudy.medmanager.util.ItemOffsetDecoration;
import com.androidstudy.medmanager.util.ProfileDialog;
import com.androidstudy.medmanager.util.Settings;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.date)
    TextView date;
    @BindView(R.id.recyclerViewDailyMedicineStatistics)
    RecyclerView recyclerViewDailyMedicineStatistics;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    Calendar calendar;
    SimpleDateFormat simpleDateFormat;
    List<MenuView> menuViewList;
    User user;
    private ProfileDialog profileDialog;
    private MainViewModel mainViewModel;
    private SnackProgressBarManager snackProgressBarManager;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        user = mainViewModel.getUserLiveData();

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setUser(user);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // Build a GoogleApiClient with access to the Google Sign-In Api and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        profileDialog = ProfileDialog.newInstance((new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                MainActivity.this.logout();
            }
        }));

        calendar = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("EEEE, MMM d, yyyy");
        String currentDate = simpleDateFormat.format(calendar.getTime());
        date.setText(currentDate);

        menuViewList = getMenuOptions();
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.item_offset);
        recyclerView.addItemDecoration(itemDecoration);

        MainDashboardAdapter mainDashboardAdapter = new MainDashboardAdapter(this, menuViewList, new CustomItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                MenuView role = menuViewList.get(position);
                String menuName = role.getName();
                switch (menuName) {
                    case "Add Medicine":
                        Intent addMedicine = new Intent(MainActivity.this.getApplicationContext(), AddMedicineActivity.class);
                        MainActivity.this.startActivity(addMedicine);
                        break;
                    case "Profile":
                        break;
                    case "Reminders":
                        break;
                    case "Monthly Intake":
                        break;
                    default:
                        Toast.makeText(MainActivity.this, "Sorry, It's Under development", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        recyclerView.setAdapter(mainDashboardAdapter);

        final DailyMedicineStatisticsAdapter dailyMedicineStatisticsAdapter = new DailyMedicineStatisticsAdapter(this, new ArrayList<Medicine>());
        recyclerViewDailyMedicineStatistics.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));

        // add pager behavior
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerViewDailyMedicineStatistics);
        // pager indicator
        recyclerViewDailyMedicineStatistics.addItemDecoration(new CirclePagerIndicatorDecoration());
        recyclerViewDailyMedicineStatistics.setAdapter(dailyMedicineStatisticsAdapter);

        MedicineViewModel medicineViewModel = ViewModelProviders.of(this).get(MedicineViewModel.class);
        medicineViewModel.getMedicineList().observe(MainActivity.this, new Observer<List<Medicine>>() {
            @Override
            public void onChanged(@Nullable List<Medicine> medicineList) {
                dailyMedicineStatisticsAdapter.addItems(medicineList);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem profileItem = menu.findItem(R.id.action_profile);
        Glide.with(this)
                .asBitmap()
                .load("")
                .apply(RequestOptions.circleCropTransform())
                .into(new SimpleTarget<Bitmap>(100, 100) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        profileItem.setIcon(new BitmapDrawable(getResources(), resource));
                    }
                });
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            profileDialog.show(getSupportFragmentManager(), "profile");
            return true;
        } else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Mock Data
    private List<MenuView> getMenuOptions() {
        List<MenuView> listViewItems = new ArrayList<>();
        listViewItems.add(new MenuView(1, "Add Medicine", R.drawable.ic_sample));
        listViewItems.add(new MenuView(2, "Profile", R.drawable.ic_sample));
        listViewItems.add(new MenuView(3, "Reminders", R.drawable.ic_sample));
        listViewItems.add(new MenuView(4, "Monthly Intake", R.drawable.ic_sample));
        return listViewItems;
    }

    private void logout() {
        if (!Settings.isLoggedIn()) {
            return;
        }

        SnackProgressBar snackProgressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_INDETERMINATE,
                "Logging out...")
                .setSwipeToDismiss(false);

        // Show snack progress during logout
        snackProgressBarManager.dismissAll();
        snackProgressBarManager.show(snackProgressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        //TODO :: Enable this Google Sign Out :)
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                //Clear Shared Pref File
                Settings.setLoggedInSharedPref(false);
                //Clear Local DB
                //TODO :: CLEAR DB

                //Redirect User to Login Page
                Intent intent = new Intent(MainActivity.this.getApplicationContext(), AuthActivity.class);
                MainActivity.this.startActivity(intent);
                MainActivity.this.finish();
            }
        });

        //Unreachable anyway
        snackProgressBarManager.dismiss();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}