package jp.netetahito.nonmagneticcompass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

//太陽から～
//http://www.enjoy.ne.jp/~k-ichikawa/sunShineAngle0m.html

public class MainActivity extends Activity {

    public MyCompass myCompass;

    private float displayDirection = 270; // ディスプレイの傾き

    private float compass = 0; // 向き

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // コンパスの設置
        myCompass = new MyCompass((SensorManager) getSystemService(SENSOR_SERVICE),
                displayDirection);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // センサーリスナー
        myCompass.init(mListener);
    }

    @Override
    public void onPause() {
        // コンパスの停止
        myCompass.unregisterListener();
    }

    private final SensorEventListener mListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // float hosei = (event.values[0] + displayDirection) % 360;
            if (myCompass.update(event, 5)) {
                // ごにょごにょ
                
                float direction = myCompass.getLowDirection();
                Log.d("こんぱすー",String.valueOf(direction));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
    };

}
