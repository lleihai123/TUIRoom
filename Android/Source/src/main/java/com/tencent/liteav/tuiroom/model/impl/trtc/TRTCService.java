package com.tencent.liteav.tuiroom.model.impl.trtc;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.tencent.liteav.beauty.TXBeautyManager;
import com.tencent.liteav.tuiroom.model.TUIRoomCoreCallback;
import com.tencent.liteav.tuiroom.model.impl.base.RoomConfig;
import com.tencent.liteav.tuiroom.model.impl.base.TRTCLogger;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;
import com.tencent.trtc.TRTCStatistics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TRTCService extends TRTCCloudListener {
    private static final String TAG           = "TRTCService";
    private static final long   PLAY_TIME_OUT = 5000;

    private TRTCCloud                                       mTRTCCloud;
    private TXBeautyManager                                 mTXBeautyManager;
    // 一开始进房的角色
    private TUIRoomCoreCallback.ActionCallback              mEnterRoomCallback;
    private TUIRoomCoreCallback.ActionCallback              mExitRoomCallback;
    private boolean            mIsInRoom;
    private TXTRTCRoomListener mDelegate;
    private String             mUserId;
    private String                                          mRoomId;
    private TRTCCloudDef.TRTCParams                         mTRTCParams;
    private Map<String, TUIRoomCoreCallback.ActionCallback> mPlayCallbackMap;
    private Map<String, Runnable>                           mPlayTimeoutRunnable;
    private Handler                                         mMainHandler;
    private String           mStreamId;
    private RoomConfig       mRoomConfig;
    private OnSwitchListener mOnSwitchListener;

    public TRTCService(Context context) {
        mTRTCCloud = TRTCCloud.sharedInstance(context);
        mTXBeautyManager = mTRTCCloud.getBeautyManager();
        mPlayCallbackMap = new HashMap<>();
        mPlayTimeoutRunnable = new HashMap<>();
        mMainHandler = new Handler(Looper.getMainLooper());
        mRoomConfig = new RoomConfig();
    }

    public void setListener(TXTRTCRoomListener delegate) {
        TRTCLogger.i(TAG, "init delegate:" + delegate);
        mDelegate = delegate;
    }

    public void enterRoom(int sdkAppId, String roomId, String userId, String userSign, int role,
                          TUIRoomCoreCallback.ActionCallback callback) {
        if (sdkAppId == 0 || TextUtils.isEmpty(roomId) || TextUtils.isEmpty(userId) || TextUtils.isEmpty(userSign)) {
            // 参数非法，可能执行了退房，或者登出
            TRTCLogger.e(TAG, "enter trtc room fail. params invalid. room id:" + roomId
                    + " user id:" + userId + " sign is empty:" + TextUtils.isEmpty(userSign));
            if (callback != null) {
                callback.onCallback(-1, "enter trtc room fail. params invalid. room id:"
                        + roomId + " user id:" + userId + " sign is empty:" + TextUtils.isEmpty(userSign));
            }
            return;
        }
        mUserId = userId;
        mRoomId = roomId;
        mStreamId = sdkAppId + "_" + roomId + "_" + userId + "_main";
        TRTCLogger.i(TAG, "enter room, app id:" + sdkAppId + " room id:" + roomId + " user id:"
                + userId + " sign:" + TextUtils.isEmpty(userId) + " role:" + role);
        mEnterRoomCallback = callback;
        mTRTCParams = new TRTCCloudDef.TRTCParams();
        mTRTCParams.sdkAppId = sdkAppId;
        mTRTCParams.userId = userId;
        mTRTCParams.userSig = userSign;
        mTRTCParams.role = role;
        mTRTCParams.streamId = mStreamId;
        // 字符串房间号逻辑
        mTRTCParams.roomId = Integer.valueOf(roomId);
        internalEnterRoom();
    }

    private void setFramework(int framework) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("api", "setFramework");
            JSONObject params = new JSONObject();
            params.put("framework", framework);
            jsonObject.put("params", params);
            mTRTCCloud.callExperimentalAPI(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void internalEnterRoom() {
        // 进房前设置一下监听，不然可能会被其他信息打断
        if (mTRTCParams == null) {
            return;
        }
        setFramework(5);
        mTRTCCloud.setListener(this);
        mTRTCCloud.enterRoom(mTRTCParams, TRTCCloudDef.TRTC_APP_SCENE_LIVE);
    }


    private void setVideoEncoderParam() {
        TRTCCloudDef.TRTCVideoEncParam param = new TRTCCloudDef.TRTCVideoEncParam();
        param.videoResolution = mRoomConfig.resolution;
        param.videoBitrate = mRoomConfig.bitrate;
        param.videoFps = mRoomConfig.fps;
        param.enableAdjustRes = true;
        param.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
        mTRTCCloud.setVideoEncoderParam(param);
    }

    @Override
    public void onFirstVideoFrame(String userId, int streamType, int width, int height) {
        TRTCLogger.i(TAG, "onFirstVideoFrame:" + userId);
        if (userId == null) {
            // userId 为 null，代表开始渲染本地采集的摄像头画面
        } else {
            stopTimeoutRunnable(userId);
            TUIRoomCoreCallback.ActionCallback callback = mPlayCallbackMap.remove(userId);
            if (callback != null) {
                callback.onCallback(0, userId + "播放成功");
            }
        }
    }

    public void exitRoom(TUIRoomCoreCallback.ActionCallback callback) {
        TRTCLogger.i(TAG, "exit room.");
        mUserId = null;
        mTRTCParams = null;
        mExitRoomCallback = callback;
        mPlayCallbackMap.clear();
        mPlayTimeoutRunnable.clear();
        mMainHandler.removeCallbacksAndMessages(null);
        mTRTCCloud.exitRoom();
    }

    public void startCameraPreview(boolean isFront, TXCloudVideoView view,
                                   TUIRoomCoreCallback.ActionCallback callback) {
        TRTCLogger.i(TAG, "start camera preview.");
        mTRTCCloud.startLocalPreview(isFront, view);
        if (callback != null) {
            callback.onCallback(0, "success");
        }
    }

    public void stopCameraPreview() {
        TRTCLogger.i(TAG, "stop camera preview.");
        mTRTCCloud.stopLocalPreview();
    }

    public void switchCamera() {
        mTRTCCloud.switchCamera();
    }

    public void setMirror(boolean isMirror) {
        TRTCLogger.i(TAG, "mirror:" + isMirror);
        if (isMirror) {
            mTRTCCloud.setLocalViewMirror(TRTCCloudDef.TRTC_VIDEO_MIRROR_TYPE_ENABLE);
        } else {
            mTRTCCloud.setLocalViewMirror(TRTCCloudDef.TRTC_VIDEO_MIRROR_TYPE_DISABLE);
        }
    }

    public void muteLocalAudio(boolean mute) {
        TRTCLogger.i(TAG, "mute local audio, mute:" + mute);
        mTRTCCloud.muteLocalAudio(mute);
    }

    public void muteRemoteAudio(String userId, boolean mute) {
        TRTCLogger.i(TAG, "mute remote audio, user id:" + userId + " mute:" + mute);
        mTRTCCloud.muteRemoteAudio(userId, mute);
    }

    public void muteAllRemoteAudio(boolean mute) {
        TRTCLogger.i(TAG, "mute all remote audio, mute:" + mute);
        mTRTCCloud.muteAllRemoteAudio(mute);
    }

    public boolean isEnterRoom() {
        return mIsInRoom;
    }

    public void setMixConfig(List<TXTRTCMixUser> list) {
        if (list == null) {
            mTRTCCloud.setMixTranscodingConfig(null);
        } else {
            // 背景大画面宽高
            int videoWidth = 720;
            int videoHeight = 1280;

            // 小画面宽高
            int subWidth = 180;
            int subHeight = 320;

            int offsetX = 5;
            int offsetY = 50;

            int bitrate = 200;

            int resolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_960_540;
            switch (resolution) {
                case TRTCCloudDef.TRTC_VIDEO_RESOLUTION_160_160: {
                    videoWidth = 160;
                    videoHeight = 160;
                    subWidth = 32;
                    subHeight = 48;
                    offsetY = 10;
                    bitrate = 200;
                    break;
                }
                case TRTCCloudDef.TRTC_VIDEO_RESOLUTION_320_180: {
                    videoWidth = 192;
                    videoHeight = 336;
                    subWidth = 54;
                    subHeight = 96;
                    offsetY = 30;
                    bitrate = 400;
                    break;
                }
                case TRTCCloudDef.TRTC_VIDEO_RESOLUTION_320_240: {
                    videoWidth = 240;
                    videoHeight = 320;
                    subWidth = 54;
                    subHeight = 96;
                    offsetY = 30;
                    bitrate = 400;
                    break;
                }
                case TRTCCloudDef.TRTC_VIDEO_RESOLUTION_480_480: {
                    videoWidth = 480;
                    videoHeight = 480;
                    subWidth = 72;
                    subHeight = 128;
                    bitrate = 600;
                    break;
                }
                case TRTCCloudDef.TRTC_VIDEO_RESOLUTION_640_360: {
                    videoWidth = 368;
                    videoHeight = 640;
                    subWidth = 90;
                    subHeight = 160;
                    bitrate = 800;
                    break;
                }
                case TRTCCloudDef.TRTC_VIDEO_RESOLUTION_640_480: {
                    videoWidth = 480;
                    videoHeight = 640;
                    subWidth = 90;
                    subHeight = 160;
                    bitrate = 800;
                    break;
                }
                case TRTCCloudDef.TRTC_VIDEO_RESOLUTION_960_540: {
                    videoWidth = 544;
                    videoHeight = 960;
                    subWidth = 160;
                    subHeight = 288;
                    bitrate = 1000;
                    break;
                }
                case TRTCCloudDef.TRTC_VIDEO_RESOLUTION_1280_720: {
                    videoWidth = 720;
                    videoHeight = 1280;
                    subWidth = 192;
                    subHeight = 336;
                    bitrate = 1500;
                    break;
                }
                default:break;
            }

            TRTCCloudDef.TRTCTranscodingConfig config = new TRTCCloudDef.TRTCTranscodingConfig();
            config.videoWidth = videoWidth;
            config.videoHeight = videoHeight;
            config.videoGOP = 1;
            config.videoFramerate = 15;
            config.videoBitrate = bitrate;
            config.audioSampleRate = 48000;
            config.audioBitrate = 64;
            config.audioChannels = 1;

            // 设置混流后主播的画面位置
            TRTCCloudDef.TRTCMixUser mixUser = new TRTCCloudDef.TRTCMixUser();
            mixUser.userId = mUserId; // 以主播uid为broadcaster为例
            mixUser.roomId = mRoomId;
            mixUser.zOrder = 1;
            mixUser.x = 0;
            mixUser.y = 0;
            mixUser.width = videoWidth;
            mixUser.height = videoHeight;

            config.mixUsers = new ArrayList<>();
            config.mixUsers.add(mixUser);

            // 设置混流后各个小画面的位置
            int index = 0;
            for (TXTRTCMixUser txtrtcMixUser : list) {
                TRTCCloudDef.TRTCMixUser subMixUser = new TRTCCloudDef.TRTCMixUser();
                subMixUser.userId = txtrtcMixUser.userId;
                subMixUser.roomId = txtrtcMixUser.roomId == null ? mRoomId : txtrtcMixUser.roomId;
                subMixUser.streamType = TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG;
                subMixUser.zOrder = 2 + index;
                if (index < 3) {
                    // 前三个小画面靠右从下往上铺
                    subMixUser.x = videoWidth - offsetX - subWidth;
                    subMixUser.y = videoHeight - offsetY - index * subHeight - subHeight;
                    subMixUser.width = subWidth;
                    subMixUser.height = subHeight;
                } else if (index < 6) {
                    // 后三个小画面靠左从下往上铺
                    subMixUser.x = offsetX;
                    subMixUser.y = videoHeight - offsetY - (index - 3) * subHeight - subHeight;
                    subMixUser.width = subWidth;
                    subMixUser.height = subHeight;
                } else {
                    // 最多只叠加六个小画面
                }
                config.mixUsers.add(subMixUser);
                ++index;
            }
            mTRTCCloud.setMixTranscodingConfig(config);
        }
    }

    public void startScreenCapture(TRTCCloudDef.TRTCVideoEncParam param,
                                   TRTCCloudDef.TRTCScreenShareParams screenShareParams) {
        mTRTCCloud.startScreenCapture(TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_SUB, param, screenShareParams);
    }

    public void stopScreenCapture() {
        mTRTCCloud.stopScreenCapture();
    }

    public void pauseScreenCapture() {
        mTRTCCloud.pauseScreenCapture();
    }

    public void resumeScreenCapture() {
        mTRTCCloud.resumeScreenCapture();
    }

    public void showVideoDebugLog(boolean isShow) {
        if (isShow) {
            mTRTCCloud.showDebugView(2);
        } else {
            mTRTCCloud.showDebugView(0);
        }
    }

    public void startPlay(final String userId, TXCloudVideoView view, TUIRoomCoreCallback.ActionCallback callback) {
        TRTCLogger.i(TAG, "start play user id:" + userId + " view:" + view);
        mPlayCallbackMap.put(userId, callback);
        mTRTCCloud.startRemoteView(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG, view);
        TRTCCloudDef.TRTCRenderParams params = new TRTCCloudDef.TRTCRenderParams();
        params.fillMode = TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT;
        mTRTCCloud.setRemoteRenderParams(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG, params);
        //停掉上一次超时
        stopTimeoutRunnable(userId);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                TRTCLogger.e(TAG, "start play timeout:" + userId);
                TUIRoomCoreCallback.ActionCallback callback = mPlayCallbackMap.remove(userId);
                if (callback != null) {
                    callback.onCallback(-1, "play " + userId + " timeout.");
                }
            }
        };
        mPlayTimeoutRunnable.put(userId, runnable);
        mMainHandler.postDelayed(runnable, PLAY_TIME_OUT);
    }

    public void startPlaySubStream(final String userId, TXCloudVideoView view,
                                   TUIRoomCoreCallback.ActionCallback callback) {
        TRTCLogger.i(TAG, "start play user sub stream id:" + userId + " view:" + view);
        mTRTCCloud.startRemoteView(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_SUB, view);
        TRTCCloudDef.TRTCRenderParams params = new TRTCCloudDef.TRTCRenderParams();
        params.fillMode = TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT;
        mTRTCCloud.setRemoteRenderParams(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_SUB, params);
        if (callback != null) {
            callback.onCallback(0, "play sub stream success");
        }
    }

    public void stopPlaySubStream(final String userId,
                                  TUIRoomCoreCallback.ActionCallback callback) {
        TRTCLogger.i(TAG, "stop user sub stream id:" + userId);
        mTRTCCloud.stopRemoteView(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_SUB);
        if (callback != null) {
            callback.onCallback(0, "stop sub stream success");
        }
    }

    private void stopTimeoutRunnable(String userId) {
        if (mPlayTimeoutRunnable == null) {
            return;
        }
        Runnable runnable = mPlayTimeoutRunnable.get(userId);
        mMainHandler.removeCallbacks(runnable);
    }

    public void stopPlay(String userId, TUIRoomCoreCallback.ActionCallback callback) {
        TRTCLogger.i(TAG, "stop play user id:" + userId);
        mPlayCallbackMap.remove(userId);
        stopTimeoutRunnable(userId);
        mTRTCCloud.stopRemoteView(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);
        if (callback != null) {
            callback.onCallback(0, "stop play success.");
        }
    }

    public void stopAllPlay() {
        TRTCLogger.i(TAG, "stop all play");
        mTRTCCloud.stopAllRemoteView();
    }

    @Override
    public void onEnterRoom(long l) {
        TRTCLogger.i(TAG, "on enter room, result:" + l);
        if (mEnterRoomCallback != null) {
            if (l > 0) {
                mIsInRoom = true;
                mEnterRoomCallback.onCallback(0, "enter room success.");
            } else {
                mIsInRoom = false;
                mEnterRoomCallback.onCallback((int) l, "enter room fail");
            }
        }
    }

    @Override
    public void onExitRoom(int i) {
        TRTCLogger.i(TAG, "on exit room.");
        if (mExitRoomCallback != null) {
            mIsInRoom = false;
            mExitRoomCallback.onCallback(0, "exit room success.");
        }
    }

    @Override
    public void onRemoteUserEnterRoom(String userId) {
        TRTCLogger.i(TAG, "on user enter, user id:" + userId);
        if (mDelegate != null) {
            mDelegate.onTRTCAnchorEnter(userId);
        }
    }

    @Override
    public void onRemoteUserLeaveRoom(String userId, int i) {
        TRTCLogger.i(TAG, "on user exit, user id:" + userId);
        if (mDelegate != null) {
            mDelegate.onTRTCAnchorExit(userId);
        }
    }

    @Override
    public void onUserVideoAvailable(String userId, boolean available) {
        TRTCLogger.i(TAG, "on user video available, user id:" + userId + " available:" + available);
        if (mDelegate != null) {
            mDelegate.onTRTCVideoAvailable(userId, available);
        }
    }

    @Override
    public void onUserSubStreamAvailable(String userId, boolean available) {
        TRTCLogger.i(TAG, "on user sub stream available, user id:" + userId + " available:" + available);
        if (mDelegate != null) {
            mDelegate.onTRTCSubStreamAvailable(userId, available);
        }
    }

    @Override
    public void onUserAudioAvailable(String userId, boolean available) {
        TRTCLogger.i(TAG, "on user audio available, user id:" + userId + " available:" + available);
        if (mDelegate != null) {
            mDelegate.onTRTCAudioAvailable(userId, available);
        }
    }

    @Override
    public void onError(int errorCode, String errorMsg, Bundle bundle) {
        TRTCLogger.i(TAG, "onError: " + errorCode);
        if (mDelegate != null) {
            mDelegate.onError(errorCode, errorMsg);
        }
    }

    @Override
    public void onNetworkQuality(final TRTCCloudDef.TRTCQuality trtcQuality,
                                 final ArrayList<TRTCCloudDef.TRTCQuality> arrayList) {
        if (mDelegate != null) {
            mDelegate.onNetworkQuality(trtcQuality, arrayList);
        }
    }

    @Override
    public void onStatistics(TRTCStatistics statistics) {
        if (mDelegate != null) {
            mDelegate.onStatistics(statistics);
        }
    }

    @Override
    public void onUserVoiceVolume(final ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {
        if (mDelegate != null && userVolumes.size() != 0) {
            mDelegate.onUserVoiceVolume(userVolumes, totalVolume);
        }
    }

    @Override
    public void onSetMixTranscodingConfig(int i, String s) {
        super.onSetMixTranscodingConfig(i, s);
        TRTCLogger.i(TAG, "on set mix transcoding, code:" + i + " msg:" + s);
    }

    public TXBeautyManager getTXBeautyManager() {
        return mTXBeautyManager;
    }

    public void setVideoResolution(int resolution) {
        mRoomConfig.resolution = resolution;
        setVideoEncoderParam();
    }

    public void setVideoFps(int fps) {
        mRoomConfig.fps = fps;
        setVideoEncoderParam();
    }

    public void setVideoBitrate(int bitrate) {
        mRoomConfig.bitrate = bitrate;
        setVideoEncoderParam();
    }

    public void setLocalViewMirror(int type) {
        mTRTCCloud.setLocalViewMirror(type);
    }

    public void setNetworkQosParam(TRTCCloudDef.TRTCNetworkQosParam qosParam) {
        mTRTCCloud.setNetworkQosParam(qosParam);
    }

    public void setAudioQuality(int quality) {
        mTRTCCloud.setAudioQuality(quality);
    }

    public void startMicrophone() {
        mTRTCCloud.startLocalAudio();
    }

    public void stopMicrophone() {
        mTRTCCloud.stopLocalAudio();
    }

    public void setSpeaker(boolean useSpeaker) {
        mTRTCCloud.setAudioRoute(useSpeaker ? TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER
                : TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE);
    }

    public void setAudioCaptureVolume(int volume) {
        mTRTCCloud.setAudioCaptureVolume(volume);
    }

    public void setAudioPlayoutVolume(int volume) {
        mTRTCCloud.setAudioPlayoutVolume(volume);
    }

    public void startFileDumping(TRTCCloudDef.TRTCAudioRecordingParams trtcAudioRecordingParams) {
        mTRTCCloud.startAudioRecording(trtcAudioRecordingParams);
    }

    public void stopFileDumping() {
        mTRTCCloud.stopAudioRecording();
    }

    public void enableAudioEvaluation(boolean enable) {
        mTRTCCloud.enableAudioVolumeEvaluation(enable ? 300 : 0);
    }

    @Override
    public void onScreenCaptureStarted() {
        if (mDelegate != null) {
            mDelegate.onScreenCaptureStarted();
        }
    }

    @Override
    public void onScreenCaptureStopped(int i) {
        if (mDelegate != null) {
            mDelegate.onScreenCaptureStopped(i);
        }
    }

    @Override
    public void onSwitchRole(int errCode, String errMsg) {
        TRTCLogger.d(TAG, "on switch role, code:" + errCode + " msg:" + errMsg);
        if (mOnSwitchListener != null) {
            mOnSwitchListener.onTRTCSwitchRole(errCode, errMsg);
            mOnSwitchListener = null;
        }
    }

    public void switchToAnchor(OnSwitchListener listener) {
        mOnSwitchListener = listener;
        mTRTCCloud.switchRole(TRTCCloudDef.TRTCRoleAnchor);
        mTRTCCloud.startLocalAudio();
    }

    public void switchToAudience(OnSwitchListener listener) {
        mTRTCCloud.stopLocalAudio();
        mOnSwitchListener = listener;
        mTRTCCloud.switchRole(TRTCCloudDef.TRTCRoleAudience);
    }

    public interface OnSwitchListener {
        void onTRTCSwitchRole(int code, String message);
    }
}
