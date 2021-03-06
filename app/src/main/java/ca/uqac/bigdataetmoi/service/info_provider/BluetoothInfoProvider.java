package ca.uqac.bigdataetmoi.service.info_provider;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ca.uqac.bigdataetmoi.database.DataCollection;
import ca.uqac.bigdataetmoi.database.data.BluetoothData;
import ca.uqac.bigdataetmoi.utility.PermissionManager;

import static android.Manifest.permission.*;

/**
 * Created by Raph on 21/11/2017.
 * Modifié par Patrick lapointe le 2018-03-13
 * Récupération des données du bluetooth et écriture dans la base de données
 */

public class BluetoothInfoProvider
{
    private final static int SEARCH_SECONDS = 20;

    private DataReadyListener mListener;

    private BluetoothAdapter mBTAdapter;
    private BroadcastReceiver mBroadCastReceiver;
    private List<String> mBluetoothSSID;

    public BluetoothInfoProvider(final Context context, DataReadyListener listener)
    {
        mListener = listener;

        if(PermissionManager.getInstance().isGranted(BLUETOOTH_ADMIN))
        {
            mBluetoothSSID = new ArrayList<>();
            mBTAdapter = BluetoothAdapter.getDefaultAdapter();

            // Activation du Bluetooth
            if (mBTAdapter != null && !mBTAdapter.isEnabled())
            {
                // TODO: Must ask for turning on Bluetooth by using :
                // startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_CODE);
                // as per https://developer.android.com/guide/topics/connectivity/bluetooth.html#SettingUp
                mBTAdapter.enable();
            }

            // Si le bluetooth est activé, on démarre une découverte
            if (mBTAdapter != null && mBTAdapter.isEnabled())
            {
                mBroadCastReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        mBluetoothSSID.add(device.getName());
                    }
                    }
                };

                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                context.registerReceiver(mBroadCastReceiver, filter);
                mBTAdapter.startDiscovery();

                // On cherche durant un nombre de secondes prédéterminées puis on arrête la recherche et on écris le résultat
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                    mBTAdapter.cancelDiscovery();
                    context.unregisterReceiver(mBroadCastReceiver);

                    // Écriture des ssid lues dans la bd
                    BluetoothData data = new BluetoothData(null, mBluetoothSSID);
                    mListener.dataReady(data);

                    }
                }, SEARCH_SECONDS * 1000);
            }
        }
        else
            mListener.dataReady(null);
    }
}