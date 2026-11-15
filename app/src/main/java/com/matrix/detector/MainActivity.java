
package com.matrix.detector;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.Configuration;

import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity
        implements SensorEventListener, LocationListener {

    private SensorManager sensorManager;
    private Sensor magneticSensor;
    private Sensor accelerometer;
    private LocationManager locationManager;

    private TextView status, reading, gpsInfo, compassInfo;
    private RadarView radar;

    private float magneticValue = 0;
    private float[] gravityValues;
    private float[] magneticValues;

    private Location currentLocation;
    private boolean scanning = false;

    private static final int LOCATION_REQUEST = 100;
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();

        sensorManager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);

        magneticSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);

        accelerometer = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        if (magneticSensor == null) {
            reading.setText("Sensor magnetico no disponible");
        } else {
            sensorManager.registerListener(
                    this, magneticSensor,
                    SensorManager.SENSOR_DELAY_UI);
        }

        if (accelerometer != null) {
            sensorManager.registerListener(
                    this, accelerometer,
                    SensorManager.SENSOR_DELAY_UI);
        }

        checkLocationPermission();
    }

    private void createInterface() {

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        main.setPadding(20, 20, 20, 20);
        main.setBackgroundColor(Color.rgb(0, 12, 8));

        TextView title = new TextView(this);
        title.setText("MATRIX DETECTOR");
        title.setTextColor(Color.GREEN);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setPadding(5, 15, 5, 15);
        main.addView(title);

        status = new TextView(this);
        status.setText("SISTEMA INICIADO");
        status.setTextColor(Color.GREEN);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        main.addView(status);

        radar = new RadarView(this);
        main.addView(radar,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 320));

        reading = new TextView(this);
        reading.setText("Campo magnetico: esperando sensor");
        reading.setTextColor(Color.GREEN);
        reading.setTextSize(18);
        reading.setGravity(Gravity.CENTER);
        reading.setPadding(5, 12, 5, 12);
        main.addView(reading);

        compassInfo = new TextView(this);
        compassInfo.setText("Brújula: esperando lectura");
        compassInfo.setTextColor(Color.GREEN);
        compassInfo.setTextSize(16);
        compassInfo.setGravity(Gravity.CENTER);
        main.addView(compassInfo);

        gpsInfo = new TextView(this);
        gpsInfo.setText("GPS: esperando ubicacion");
        gpsInfo.setTextColor(Color.GREEN);
        gpsInfo.setTextSize(15);
        gpsInfo.setGravity(Gravity.CENTER);
        gpsInfo.setPadding(5, 12, 5, 12);
        main.addView(gpsInfo);

        Button scanButton = new Button(this);
        scanButton.setText("INICIAR EXPLORACION ALEATORIA 5 KM");
        scanButton.setTextColor(Color.BLACK);
        scanButton.setBackgroundColor(Color.GREEN);
        main.addView(scanButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateRandomLocation();
            }
        });

        Button stopButton = new Button(this);
        stopButton.setText("DETENER EXPLORACION");
        main.addView(stopButton);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanning = false;
                status.setText("EXPLORACION DETENIDA");
                radar.invalidate();
            }
        });

        setContentView(main);
    }

    private void checkLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST);
        } else {
            startGPS();
        }
    }

    private void startGPS() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    2,
                    this);

            Location last = locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER);

            if (last != null) {
                onLocationChanged(last);
            }

            gpsInfo.setText("GPS activado. Buscando satelites...");
        } catch (Exception e) {
            gpsInfo.setText("No se pudo iniciar GPS");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;

        gpsInfo.setText(String.format(Locale.US,
                "LAT: %.6f\nLON: %.6f\nPrecision: %.1f metros",
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy()));
    }

    private void generateRandomLocation() {

        if (currentLocation == null) {
            Toast.makeText(this,
                    "Esperando ubicacion GPS. Activa el GPS.",
                    Toast.LENGTH_LONG).show();
            checkLocationPermission();
            return;
        }

        double radius = 5000.0;

        double distance = Math.sqrt(random.nextDouble()) * radius;
        double angle = random.nextDouble() * 2 * Math.PI;

        double north = distance * Math.cos(angle);
        double east = distance * Math.sin(angle);

        double latitude = currentLocation.getLatitude()
                + (north / 111320.0);

        double longitude = currentLocation.getLongitude()
                + (east / (111320.0 *
                Math.cos(Math.toRadians(
                        currentLocation.getLatitude()))));

        scanning = true;

        status.setText("EXPLORACION ACTIVA - OBJETIVO ALEATORIO");

        gpsInfo.setText(String.format(Locale.US,
                "DESTINO ALEATORIO\nLAT: %.6f\nLON: %.6f\nDistancia: %.0f metros",
                latitude, longitude, distance));

        radar.invalidate();

        Toast.makeText(this,
                "Nuevo punto aleatorio generado dentro de 5 km",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            magneticValue = (float) Math.sqrt(
                    x*x + y*y + z*z);

            reading.setText(String.format(Locale.US,
                    "CAMPO MAGNETICO: %.2f µT\nX: %.2f  Y: %.2f  Z: %.2f",
                    magneticValue, x, y, z));

            radar.setMagneticValue(magneticValue);
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravityValues = event.values.clone();
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values.clone();
        }

        updateCompass();
    }

    private void updateCompass() {

        if (gravityValues == null || magneticValues == null) {
            return;
        }

        float[] rotation = new float[9];
        float[] orientation = new float[3];

        boolean valid = SensorManager.getRotationMatrix(
                rotation, null, gravityValues, magneticValues);

        if (valid) {
            SensorManager.getOrientation(rotation, orientation);

            float degrees = (float) Math.toDegrees(orientation[0]);

            if (degrees < 0) {
                degrees += 360;
            }

            compassInfo.setText(String.format(Locale.US,
                    "BRUJULA: %.0f grados", degrees));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        gpsInfo.setText("GPS desactivado");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGPS();
            } else {
                gpsInfo.setText("Permiso GPS denegado");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager != null) {
            if (magneticSensor != null) {
                sensorManager.registerListener(
                        this, magneticSensor,
                        SensorManager.SENSOR_DELAY_UI);
            }

            if (accelerometer != null) {
                sensorManager.registerListener(
                        this, accelerometer,
                        SensorManager.SENSOR_DELAY_UI);
            }
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startGPS();
        }
    }

    public class RadarView extends View {

        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value = 0;

        public RadarView(Context context) {
            super(context);
        }

        public void setMagneticValue(float magnetic) {
            value = magnetic;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(cx, cy) - 15;

            canvas.drawColor(Color.BLACK);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(Color.GREEN);

            for (int i = 1; i <= 4; i++) {
                canvas.drawCircle(cx, cy,
                        radius * i / 4f, paint);
            }

            canvas.drawLine(cx - radius, cy,
                    cx + radius, cy, paint);

            canvas.drawLine(cx, cy - radius,
                    cx, cy + radius, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.GREEN);

            canvas.drawCircle(cx, cy, 6, paint);

            paint.setTextSize(16);
            canvas.drawText("N", cx - 6, cy - radius - 3, paint);
            canvas.drawText("S", cx - 6, cy + radius + 18, paint);
            canvas.drawText("E", cx + radius + 2, cy + 5, paint);
            canvas.drawText("O", cx - radius - 20, cy + 5, paint);

            paint.setTextSize(14);
            canvas.drawText(
                    String.format(Locale.US, "%.1f µT", value),
                    15, 25, paint);

            if (scanning) {
                paint.setColor(Color.GREEN);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);

                canvas.drawCircle(cx, cy,
                        radius * 0.65f, paint);
            }
        }
    }
}
