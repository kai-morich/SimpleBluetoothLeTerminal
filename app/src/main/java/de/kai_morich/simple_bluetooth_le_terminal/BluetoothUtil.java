package de.kai_morich.simple_bluetooth_le_terminal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

import java.util.Map;

public class BluetoothUtil {

    interface PermissionGrantedCallback {
        void call();
    }

    /*
     * more efficient caching of name than BluetoothDevice which always does RPC
     */
    static class Device implements Comparable<Device> {
        BluetoothDevice device;
        String name;

        @SuppressLint("MissingPermission")
        public Device(BluetoothDevice device) {
            this.device = device;
            this.name = device.getName();
        }

        public BluetoothDevice getDevice() { return device; }
        public String getName() { return name; }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Device)
                return device.equals(((Device) o).device);
            return false;
        }

        /**
         * sort by name, then address. sort named devices first
         */
        @Override
        public int compareTo(Device other) {
            boolean thisValid = this.name!=null && !this.name.isEmpty();
            boolean otherValid = other.name!=null && !other.name.isEmpty();
            if(thisValid && otherValid) {
                int ret = this.name.compareTo(other.name);
                if (ret != 0) return ret;
                return this.device.getAddress().compareTo(other.device.getAddress());
            }
            if(thisValid) return -1;
            if(otherValid) return +1;
            return this.device.getAddress().compareTo(other.device.getAddress());
        }

    }


    /**
     * Android 12 permission handling
     */
    private static void showRationaleDialog(Fragment fragment, DialogInterface.OnClickListener listener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle(fragment.getString(R.string.bluetooth_permission_title));
        builder.setMessage(fragment.getString(R.string.bluetooth_permission_grant));
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Continue", listener);
        builder.show();
    }

    private static void showSettingsDialog(Fragment fragment) {
        String s = fragment.getResources().getString(fragment.getResources().getIdentifier("@android:string/permgrouplab_nearby_devices", null, null));
        final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle(fragment.getString(R.string.bluetooth_permission_title));
        builder.setMessage(String.format(fragment.getString(R.string.bluetooth_permission_denied), s));
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Settings", (dialog, which) ->
                fragment.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + BuildConfig.APPLICATION_ID))));
        builder.show();
    }

    /**
     * CONNECT + SCAN are granted together in same permission group, so actually no need to check/request both, but one never knows
     */
    static boolean hasPermissions(Fragment fragment, ActivityResultLauncher<String[]> requestPermissionLauncher) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return true;
        boolean missingPermissions = fragment.getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                                   | fragment.getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED;
        boolean showRationale = fragment.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)
                              | fragment.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN);
        String[] permissions = new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
        if(missingPermissions) {
            if (showRationale) {
                showRationaleDialog(fragment, (dialog, which) ->
                requestPermissionLauncher.launch(permissions));
            } else {
                requestPermissionLauncher.launch(permissions);
            }
            return false;
        } else {
            return true;
        }
    }

    static void onPermissionsResult(Fragment fragment, Map<String, Boolean> grants, PermissionGrantedCallback cb) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return;
        boolean showRationale = fragment.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)
                              | fragment.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN);
        boolean granted = grants.values().stream().reduce(true, (a, b) -> a && b);
        if (granted) {
            cb.call();
        } else if (showRationale) {
            showRationaleDialog(fragment, (dialog, which) -> cb.call());
        } else {
            showSettingsDialog(fragment);
        }
    }

}
