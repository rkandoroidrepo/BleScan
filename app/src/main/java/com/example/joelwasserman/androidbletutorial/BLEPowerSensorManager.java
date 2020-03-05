package com.example.joelwasserman.androidbletutorial;

import java.util.UUID;

public class BLEPowerSensorManager {

    public static final String SERVICE_UUID_CYCLING_POWER = "00001818-0000-1000-8000-00805F9B34FB";
    public static final String CHARACTERISTIC_UUID_CYCLING_POWER = "00002A63-0000-1000-8000-00805F9B34FB";
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final String SERVICE_UUID_CYCLING_SPEED_AND_CADENCE = "00001816-0000-1000-8000-00805F9B34FB";
    public static final String CHARACTERISTIC_UUID_CSC_MEASUREMENT = "00002A5B-0000-1000-8000-00805F9B34FB";

}