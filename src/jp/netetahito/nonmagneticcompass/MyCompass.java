package jp.netetahito.nonmagneticcompass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/** 
 * to use

MyCompass mycompass;

// �f�B�X�v���C�̌X��
private float displayDirection = 270;

// �R���p�X�̐ݒu
mycompass = new MyCompass(
                (SensorManager) getSystemService(SENSOR_SERVICE),
                displayDirection);
// �Z���T�[���X�i�[
mycompass.init(mListener);
        
// �R���p�X�̒�~
mycompass.unregisterListener();

// ����
private float compass = 0;

// �Z���T�[���X�i�[
private final SensorEventListener mListener = new SensorEventListener() {
    public void onSensorChanged(SensorEvent event) {
        // float hosei = (event.values[0] + displayDirection) % 360;
        if (mycompass.update(event, 5)) {
            //���ɂ傲�ɂ�
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // �����������ꂽ���\�b�h�E�X�^�u
        }
    };
    
 */
/**
 * �R���p�X�N���X
 * �[���̌X���ƕ���������p�����߂�
 * @author osanaikoutarou
 *
 */
public class MyCompass implements SensorEventListener{
    
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;              //�����x�Z���T
    private Sensor mMagneticField;              //���C�Z���T
    private float kaiten;
    private SensorEventListener listener;
    private float direction=0;
    private float lowDirection=0;               //���[�p�X

    private float[] mAccelerometerValue = new float[3]; //�����x�Z���T�l
    private float[] mMagneticFieldValue = new float[3]; //���C�Z���T�l
    private boolean mValidMagneticField = false;    //���C�Z���T�̍X�V���ς񂾂�
    float[] orientation = new float[3];     //���p�A�p�A��]
    
    int updateNum=0;
    final double lowpass = 0.9;
    final double renewalPerDiv = 20.0;
    
    /**
     * SensorManager�A�[���̉�]��o�^����
     * @param mSensorManager
     *      SensorManager
     * @param kaiten
     *      �[���̉�]�i0~360,�c:0�A��:270,�Œ�z��j
     */
    MyCompass(SensorManager mSensorManager,float kaiten){
        this.mSensorManager = mSensorManager;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        this.kaiten=kaiten;
    }
    
    /**
     * ������.Activity������ԂɂȂ������ɌĂ�
     * @return
     *      ���s�̏ꍇfalse
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
     * ������
     * @param listener
     *      �o�^����SensorEventListner
     * @return
     *      ���s�̏ꍇfalse
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
     * ���E�ƒ[���̌X��������p�����߂�.
     * �l���X�V���鎞�ɌĂԁiSensorEvent��Listener����Ăяo���j
     * ���߂����p��direction�ɕۑ�����
     * 
     * @param event
     *      �X�V���ꂽsensor event
     * @return
     *      ���s������false
     */
    public boolean update(SensorEvent event){
        //�Z���T�[���Ƃ̏���
        switch(event.sensor.getType()){
        
        //�����x�Z���T
        case Sensor.TYPE_ACCELEROMETER:
            //�����x�̒l���R�s�[
            mAccelerometerValue = event.values.clone();
            break;
        case Sensor.TYPE_MAGNETIC_FIELD:
            mMagneticFieldValue = event.values.clone();
            mValidMagneticField = true;
            break;
        }
        
        //�l���X�V���ꂽ�p�x���o���������ł�����
        if(mValidMagneticField){
            //���ʂ��o�����߂̕ϊ��s��
            float[] rotate = new float[16];         
            float[] outR = new float[16];
            
            SensorManager.getRotationMatrix(rotate, null, mAccelerometerValue, mMagneticFieldValue);
            SensorManager.remapCoordinateSystem(rotate, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
                
            //���������߂�
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
     * �������Ԉ����i�Z���T�[�̍X�V�p�x����������ꍇ�j
     * @param event
     *      SensorEvent
     * @param doPerNum
     *      ����ɂP����s���邩
     * @return
     *      ���s�����ꍇupdate()�̕Ԃ�l�A���s���Ȃ��ꍇfalse
     */
    public boolean update(SensorEvent event,int doPerNum){
        if(++updateNum % doPerNum == 0){
            return update(event);
        }
        else
            return false;
    }
    
    /**
     * �ۑ�����Ă�����p��Ԃ�
     * @return
     *      ���p(�k��0�Ƃ��Ď��v����360�܂�)
     */
    public float getDirection(){
        if(mValidMagneticField)
            return direction;
        else
            return 0;
    }
        
    /**
     * Activity���s����ԂɂȂ������Ƀ��X�i�[���~�߂�
     */
    public void unregisterListener(){
        mSensorManager.unregisterListener(listener);
    }
    
    
    /**
     * Low-pass filter
     * @return
     *      ���p(�k��0�Ƃ��Ď��v����360�܂�)
     */
    public float getLowDirection(){
        lowDirection = (float) (lowDirection*0.9 + direction*(1.0-lowpass));
        
        if(Math.abs(lowDirection-direction)>renewalPerDiv)
            lowDirection = direction;
        
        return lowDirection;
    }

}
