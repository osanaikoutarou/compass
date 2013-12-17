package jp.netetahito.nonmagneticcompass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/** 
 * to use

MyCompass mycompass;

// ディスプレイの傾き
private float displayDirection = 270;

// コンパスの設置
mycompass = new MyCompass(
                (SensorManager) getSystemService(SENSOR_SERVICE),
                displayDirection);
// センサーリスナー
mycompass.init(mListener);
        
// コンパスの停止
mycompass.unregisterListener();

// 向き
private float compass = 0;

// センサーリスナー
private final SensorEventListener mListener = new SensorEventListener() {
    public void onSensorChanged(SensorEvent event) {
        // float hosei = (event.values[0] + displayDirection) % 360;
        if (mycompass.update(event, 5)) {
            //ごにょごにょ
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // 自動生成されたメソッド・スタブ
        }
    };
    
 */
/**
 * コンパスクラス
 * 端末の傾きと方向から方角を求める
 * @author osanaikoutarou
 *
 */
public class MyCompass implements SensorEventListener{
    
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;              //加速度センサ
    private Sensor mMagneticField;              //磁気センサ
    private float kaiten;
    private SensorEventListener listener;
    private float direction=0;
    private float lowDirection=0;               //ローパス

    private float[] mAccelerometerValue = new float[3]; //加速度センサ値
    private float[] mMagneticFieldValue = new float[3]; //磁気センサ値
    private boolean mValidMagneticField = false;    //磁気センサの更新が済んだか
    float[] orientation = new float[3];     //方角、仰角、回転
    
    int updateNum=0;
    final double lowpass = 0.9;
    final double renewalPerDiv = 20.0;
    
    /**
     * SensorManager、端末の回転を登録する
     * @param mSensorManager
     *      SensorManager
     * @param kaiten
     *      端末の回転（0~360,縦:0、横:270,固定想定）
     */
    MyCompass(SensorManager mSensorManager,float kaiten){
        this.mSensorManager = mSensorManager;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        this.kaiten=kaiten;
    }
    
    /**
     * 初期化.Activityが可視状態になった時に呼ぶ
     * @return
     *      失敗の場合false
     */
    public boolean init(){
        if(mSensorManager!=null){
            mSensorManager.registerListener(this,
                            mAccelerometer,
                            SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(
                            this, this.mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
            this.listener = this;
            return true;
        }
        else{
            return false;
        }
    }
    
    /**
     * 初期化
     * @param listener
     *      登録するSensorEventListner
     * @return
     *      失敗の場合false
     */
    public boolean init(SensorEventListener listener){
        if(mSensorManager!=null){
            mSensorManager.registerListener(listener,mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(listener,mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
            this.listener = listener;
            return true;
        }
        else{
            return false;
        }
    }
        
    /**
     * for SensorEventListener
     */
    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }
    
    /**
     * for SensorEventListener
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        update(event);
    }
    
    /**
     * 磁界と端末の傾きから方角を求める.
     * 値を更新する時に呼ぶ（SensorEventのListenerから呼び出す）
     * 求めた方角はdirectionに保存する
     * 
     * @param event
     *      更新されたsensor event
     * @return
     *      失敗したらfalse
     */
    public boolean update(SensorEvent event){
        //センサーごとの処理
        switch(event.sensor.getType()){
        
        //加速度センサ
        case Sensor.TYPE_ACCELEROMETER:
            //加速度の値をコピー
            mAccelerometerValue = event.values.clone();
            break;
        case Sensor.TYPE_MAGNETIC_FIELD:
            mMagneticFieldValue = event.values.clone();
            mValidMagneticField = true;
            break;
        }
        
        //値が更新された角度を出す準備ができたら
        if(mValidMagneticField){
            //方位を出すための変換行列
            float[] rotate = new float[16];         
            float[] outR = new float[16];
            
            SensorManager.getRotationMatrix(rotate, null, mAccelerometerValue, mMagneticFieldValue);
            SensorManager.remapCoordinateSystem(rotate, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
                
            //方向を求める
            SensorManager.getOrientation(outR, orientation);
                    
            float angle = (float) Math.toDegrees(orientation[0]);
                    
            if(angle >= 0)
                orientation[0] = angle;
            else if(angle < 0)
                orientation[0] = 360 + angle;
            
            orientation[0]-=kaiten;

            if(orientation[0] < 0)
                orientation[0]+=360;
                    
            orientation[1] = (float) Math.toDegrees(orientation[1]);
            orientation[2] = (float) Math.toDegrees(orientation[2]);
            
            direction = orientation[0];
            if(direction==0)
                lowDirection = direction;
            
            return true;
        }
        else
            return false;
    }
    
    /**
     * 処理を間引く（センサーの更新頻度が高すぎる場合）
     * @param event
     *      SensorEvent
     * @param doPerNum
     *      何回に１回実行するか
     * @return
     *      実行される場合update()の返り値、実行しない場合false
     */
    public boolean update(SensorEvent event,int doPerNum){
        if(++updateNum % doPerNum == 0){
            return update(event);
        }
        else
            return false;
    }
    
    /**
     * 保存されている方角を返す
     * @return
     *      方角(北を0として時計回りに360まで)
     */
    public float getDirection(){
        if(mValidMagneticField)
            return direction;
        else
            return 0;
    }
        
    /**
     * Activityが不可視状態になった時にリスナーを止める
     */
    public void unregisterListener(){
        mSensorManager.unregisterListener(listener);
    }
    
    
    /**
     * Low-pass filter
     * @return
     *      方角(北を0として時計回りに360まで)
     */
    public float getLowDirection(){
        lowDirection = (float) (lowDirection*0.9 + direction*(1.0-lowpass));
        
        if(Math.abs(lowDirection-direction)>renewalPerDiv)
            lowDirection = direction;
        
        return lowDirection;
    }

}
