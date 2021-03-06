package com.noob.noobcameraflash.Utilities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;

import com.noob.lumberjack.LumberJack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Abhishek on 21-11-2015.
 */
@SuppressWarnings("ConstantConditions")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraUtilLollipop extends BaseCameraUtil {
    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mBuilder;
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;


    public CameraUtilLollipop(Context context) throws CameraAccessException, SecurityException {
        super(context);
        openCamera(context);
    }

    @SuppressLint("MissingPermission")
    private void openCamera(Context context) throws CameraAccessException, SecurityException {
        if (mCameraManager == null)
            mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (isFlashAvailable()) {
            mCameraManager.openCamera("0", new CameraDeviceStateCallback(), null);
        } else {
            LumberJack.e("Camera Permission is not provided");
        }
    }


    private boolean isFlashAvailable() throws CameraAccessException {
        CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics("0");
        return cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
    }

    @Override
    public void turnOnFlash() {
        try {
            mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            mSession.setRepeatingRequest(mBuilder.build(), null, null);
            setTorchMode(TorchMode.SwitchedOn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void turnOffFlash() {
        try {
            mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            mSession.setRepeatingRequest(mBuilder.build(), null, null);
            setTorchMode(TorchMode.SwitchedOff);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class CameraDeviceStateCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            //get builder
            try {
                mBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
                //flash on, default is on
                mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
                mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                List<Surface> list = new ArrayList<>();
                SurfaceTexture mSurfaceTexture = new SurfaceTexture(1);
                Size size = getSmallestSize(mCameraDevice.getId());
                mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                Surface mSurface = new Surface(mSurfaceTexture);
                list.add(mSurface);
                mBuilder.addTarget(mSurface);
                camera.createCaptureSession(list, new MyCameraCaptureSessionStateCallback(), null);
                mSurfaceTexture.release();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    }


    private Size getSmallestSize(String cameraId) throws CameraAccessException {
        Size[] outputSizes = getCameraManager().getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0) {
            throw new IllegalStateException(
                    "Camera " + cameraId + "doesn't support any outputSize.");
        }
        Size chosen = outputSizes[0];
        for (Size s : outputSizes) {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                chosen = s;
            }
        }
        return chosen;
    }

    /**
     * session callback
     */
    class MyCameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mSession = session;
            try {
                mSession.setRepeatingRequest(mBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    }

    @Override
    public void release() {
        if (mCameraManager != null) {
            mCameraManager = null;
        }
        if (mCameraDevice == null || mSession == null) {
            return;
        }
        mSession.close();
        mCameraDevice.close();
        mCameraDevice = null;
        mSession = null;
    }

    //region Accessors

    private CameraManager getCameraManager() {
        if (mCameraManager == null) {
            try {
                openCamera(getContext());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        return mCameraManager;
    }

    //endregion
}
