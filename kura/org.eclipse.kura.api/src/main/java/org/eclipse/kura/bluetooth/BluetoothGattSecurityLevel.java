package org.eclipse.kura.bluetooth;

public enum BluetoothGattSecurityLevel {

    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN;

    public static BluetoothGattSecurityLevel getBluetoothGattSecurityLevel(String level) {
        if (LOW.toString().equalsIgnoreCase(level)) {
            return LOW;
        } else if (MEDIUM.toString().equalsIgnoreCase(level)) {
            return MEDIUM;
        } else if (HIGH.toString().equalsIgnoreCase(level)) {
            return HIGH;
        } else {
            return UNKNOWN;
        }

    }

}
