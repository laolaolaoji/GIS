package com.laoji.gis;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.getbase.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    //高德地图相关
    public MapView mMapView;
    public AMap aMap;
    public LocationSource aMapLocationSource;
    public AMapLocationClient aMapLocationClient;
    public AMapLocationClientOption aMapLocationClientOption;
    public AMapLocationListener aMapLocationListener;
    public LocationSource.OnLocationChangedListener aMapOnLocationChangedListener;
    public AMapLocation aMapLocation;
    public UiSettings aMapUiSettings;
    public MyLocationStyle aMapLocationStyle;

    //要申请的权限
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private AlertDialog dialog;

    //界面相关
    private Toolbar mainToolbar;
    private FloatingActionButton addGuideButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            for (String permission : permissions) {
                int i = ContextCompat.checkSelfPermission(this, permission);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (i != PackageManager.PERMISSION_GRANTED) {
                    // 如果没有授予该权限，就去提示用户请求
                    showDialogTipUserRequestPermission();
                    break;
                }
            }

        }

        mainToolbar = (Toolbar) findViewById(R.id.toolbar);
        mainToolbar.setTitle("云南地质大数据");
        setSupportActionBar(mainToolbar);

        addGuideButton = (FloatingActionButton) findViewById(R.id.add_guide_button);
        addGuideButton.setOnClickListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mainToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.AMap_MapView);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        aMapUiSettings = aMap.getUiSettings();
        //隐藏logo
        aMapUiSettings.setLogoBottomMargin(-50);
        //隐藏缩放控件
        aMapUiSettings.setZoomControlsEnabled(false);
        //高德定位按钮
        //aMapUiSettings.setMyLocationButtonEnabled(true);
        AMapInit();
    }

    /**
     * 高德地图初始化
     */
    public void AMapInit() {
        //定位监听初始化
        AMapLocationListenerInit();
        //在activity中启动自定义本地服务LocationService
        getApplicationContext().startService(new Intent(this, LOCATION_SERVICE.getClass()));
        if (aMapLocationClient == null) {
            //在LocationService中启动定位
            aMapLocationClient = new AMapLocationClient(this.getApplicationContext());
            aMapLocationClientOption = new AMapLocationClientOption();
            //设置为高精度定位模式
            aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            // 使用连续定位
            aMapLocationClientOption.setOnceLocation(false);
            // 每5秒定位一次
            aMapLocationClientOption.setInterval(5 * 1000);
            aMapLocationClient.setLocationOption(aMapLocationClientOption);
            aMapLocationClient.setLocationListener(aMapLocationListener);
            aMapLocationClient.startLocation();
        }
        LatLng latLng=new LatLng(30.621716,113.542914);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,3));
        aMapLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        aMapLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        aMapLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        aMapLocationStyle.showMyLocation(true);
        aMap.setMyLocationStyle(aMapLocationStyle);//设置定位蓝点的Style

        //定位资源初始化
        //MapLocationSourceInit();
        // 设置定位监听
        //aMap.setLocationSource(aMapLocationSource);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);


    }

    /**
     * 初始化高德地图定位资源
     */
    public void AMapLocationSourceInit() {
        aMapLocationSource = new LocationSource() {
            @Override
            public void activate(OnLocationChangedListener onLocationChangedListener) {
                aMapOnLocationChangedListener = onLocationChangedListener;
                if (aMapLocationClient == null) {
                    //初始化定位
                    aMapLocationClient = new AMapLocationClient(getApplicationContext());
                    //初始化定位参数
                    aMapLocationClientOption = new AMapLocationClientOption();
                    //设置定位回调监听
                    aMapLocationClient.setLocationListener(aMapLocationListener);
                    //设置为高精度定位模式
                    aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                    //设置定位参数
                    aMapLocationClient.setLocationOption(aMapLocationClientOption);
                    // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
                    // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
                    // 在定位结束后，在合适的生命周期调用onDestroy()方法
                    // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
                    aMapLocationClient.startLocation();//启动定位
                }
            }

            @Override
            public void deactivate() {
                aMapOnLocationChangedListener = null;
                if (aMapLocationClient != null) {
                    aMapLocationClient.stopLocation();
                    aMapLocationClient.onDestroy();
                }
                aMapLocationClient = null;
            }
        };
    }

    /**
     * 初始化高德地图定位监听
     */
    public void AMapLocationListenerInit() {
        aMapLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation Location) {
                if (Location != null) {
                    if (Location != null && Location.getErrorCode() == 0) {
                        aMapLocation = Location;
                        //aMapLocationListener.onLocationChanged(Location);// 显示系统小蓝点
                    } else {
                        String errText = "定位失败," + Location.getErrorCode() + ": " + Location.getErrorInfo();
                        Log.e("AmapErr", errText);
                    }
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.menu_locate) {
            if (null != aMapLocation && null != aMap) {
                aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()), 16, 0, 0)));
            } else {
                Toast.makeText(getApplicationContext(), "暂无定位", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_guide_button:
                Toast.makeText(getApplicationContext(), "辅助线", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
        if (null != aMapLocationClient) {
            aMapLocationClient.onDestroy();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    // 提示用户该请求权限的弹出框
    private void showDialogTipUserRequestPermission() {
        new AlertDialog.Builder(this)
                .setTitle("权限不可用")
                .setMessage("由于定位需要获取一些本机权限；\n否则，您将无法正常使用")
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRequestPermission();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }

    // 开始提交请求权限
    private void startRequestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 321);
    }

    // 用户权限 申请 的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                    boolean b = shouldShowRequestPermissionRationale(permissions[0]);
                    if (!b) {
                        // 用户还是想用我的 APP 的
                        // 提示用户去应用设置界面手动开启权限
                        showDialogTipUserGoToAppSettting();
                    } else
                        finish();
                } else {
                    Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // 提示用户去应用设置界面手动开启权限
    private void showDialogTipUserGoToAppSettting() {
        dialog = new AlertDialog.Builder(this)
                .setTitle("权限不可用")
                .setMessage("请在-应用设置-权限-中，允许支付宝使用存储权限来保存用户数据")
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 跳转到应用设置界面
                        goToAppSetting();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);

        startActivityForResult(intent, 123);
    }

    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123) {

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 检查该权限是否已经获取
                int i = ContextCompat.checkSelfPermission(this, permissions[0]);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (i != PackageManager.PERMISSION_GRANTED) {
                    // 提示用户应该去应用设置界面手动开启权限
                    showDialogTipUserGoToAppSettting();
                } else {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


}
