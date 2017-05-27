package tw.jeff.smallcarremotecontroller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FindDevice extends AppCompatActivity {

    private static String NAME = "name";
    private static String ADDR = "addr";
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayList<Map<String, String>> findList;
    private SimpleAdapter simpleAdapter;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String act = intent.getAction();
            switch (act) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    HashMap<String, String> map2 = new HashMap<>();
                    map2.put(NAME, device.getName());
                    map2.put(ADDR, device.getAddress());
                    findList.add(map2);
                    simpleAdapter.notifyDataSetChanged();
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Toast.makeText(context, "Search finish", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.progressBar).setVisibility(View.GONE);

                    break;
            }
        }
    };
    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            bluetoothAdapter.cancelDiscovery();
            TextView tmp1 = (TextView) ((RelativeLayout) view).getChildAt(1);
            String address = tmp1.getText().toString();

            /*
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length()-17);*/
            Intent intent = new Intent();
            intent.putExtra("deviceAddress", address);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_device);
        this.setTitle("請選擇要連接的藍芽");
        if (Build.VERSION.SDK_INT >= 23) {
            int tmp = this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (tmp == PackageManager.PERMISSION_DENIED) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(broadcastReceiver, filter);

        //filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //this.registerReceiver(broadcastReceiver,filter);

        findList = new ArrayList<>();
        ListView listView = (ListView) findViewById(R.id.findDeviceListVIew);
        simpleAdapter = new SimpleAdapter(this, findList, android.R.layout.simple_list_item_2,
                new String[]{NAME, ADDR}, new int[]{android.R.id.text1, android.R.id.text2});
        listView.setAdapter(simpleAdapter);
        listView.setOnItemClickListener(this.onItemClickListener);

        this.setResult(Activity.RESULT_CANCELED);
        bluetoothAdapter.startDiscovery();

       /* Map<String,String> map = new HashMap<>();
        map.put(NAME,"Test1");
        map.put(ADDR,"Test2");
        findList.add(map);
        findList.add(map);
        findList.add(map);
        findList.add(map);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(broadcastReceiver);
    }
}
