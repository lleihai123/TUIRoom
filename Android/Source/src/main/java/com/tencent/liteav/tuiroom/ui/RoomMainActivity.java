package com.tencent.liteav.tuiroom.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.DialogFragment;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.tencent.liteav.basic.UserModel;
import com.tencent.liteav.basic.UserModelManager;
import com.tencent.liteav.demo.beauty.view.BeautyPanel;
import com.tencent.liteav.tuiroom.model.TUIRoomCore;
import com.tencent.liteav.tuiroom.model.TUIRoomCoreCallback;
import com.tencent.liteav.tuiroom.model.TUIRoomCoreDef;
import com.tencent.liteav.tuiroom.model.TUIRoomCoreListener;
import com.tencent.liteav.tuiroom.ui.remote.RemoteUserListView;
import com.tencent.liteav.tuiroom.ui.utils.StateBarUtils;
import com.tencent.liteav.tuiroom.ui.widget.base.ConfirmDialogFragment;
import com.tencent.liteav.tuiroom.ui.widget.feature.FeatureConfig;
import com.tencent.liteav.tuiroom.ui.widget.feature.FeatureSettingFragmentDialog;
import com.tencent.liteav.tuiroom.ui.widget.page.AnchorListView;
import com.tencent.liteav.tuiroom.R;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RoomMainActivity extends AppCompatActivity implements TUIRoomCoreListener, View.OnClickListener {
    private static final String TAG = "TuiRoomMain";

    public static final String KEY_ROOM_ID        = "room_id";
    public static final String KEY_USER_ID        = "user_id";
    public static final String KEY_USER_NAME      = "user_name";
    public static final String KEY_USER_AVATAR    = "user_avatar";
    public static final String KEY_OPEN_CAMERA    = "open_camera";
    public static final String KEY_OPEN_AUDIO     = "open_audio";
    public static final String KEY_AUDIO_QUALITY  = "audio_quality";
    public static final String KEY_VIDEO_QUALITY  = "video_quality";
    public static final String KEY_IS_CREATE_ROOM = "is_create_room";

    private String                    mRoomId;
    private String                    mUserId;
    private String                    mUserAvatar;
    private boolean                   mIsCreateRoom; //是否是创建房间
    private boolean                   mOpenCamera;
    private boolean                   mOpenAudio;
    private int                       mAudioQuality;
    private int                       mVideoQuality;
    private String                    mUserName;
    private boolean                   mIsFrontCamera = true;
    private boolean                   mIsUseSpeaker  = true;
    private TUIRoomCore               mTUIRoomCore;
    private List<MemberEntity>        mMemberEntityList;
    private Map<String, MemberEntity> mStringMemberEntityMap;
    private MemberEntity              mSelfEntity;

    private AnchorListView               mAnchorListView;
    private RoomVideoView                mViewVideo;
    private RoomHeadBarView              mRoomHeadBarView;
    private BeautyPanel                  mBeautyControl;
    private AppCompatImageButton         mAudioImg;
    private AppCompatImageButton         mVideoImg;
    private AppCompatImageButton         mBeautyImg;
    private AppCompatImageButton         mMemberImg;
    private AppCompatImageButton         mMoreImg;
    private ViewStub                     mStubRemoteUserView;
    private RemoteUserListView           mRemoteUserView;
    private FeatureSettingFragmentDialog mFeatureSettingFragmentDialog; //更多设置面板
    private View                         mScreenCaptureGroup;
    private Group                        mBottomToolBarGroup;
    private TextView                     mStopScreenCaptureTv;
    private View                         mFloatingWindow;
    private boolean                      isScreenCapture;
    private boolean                      mIsPaused = false;

    public static void enterRoom(Context context,
                                 boolean isCreate,
                                 String roomId,
                                 String userId,
                                 String userName,
                                 String userAvatar,
                                 boolean openCamera,
                                 boolean openAudio,
                                 int audioQuality,
                                 int videoQuality) {
        Intent starter = new Intent(context, RoomMainActivity.class);
        starter.putExtra(KEY_ROOM_ID, roomId);
        starter.putExtra(KEY_USER_ID, userId);
        starter.putExtra(KEY_USER_NAME, userName);
        starter.putExtra(KEY_USER_AVATAR, userAvatar);
        starter.putExtra(KEY_OPEN_CAMERA, openCamera);
        starter.putExtra(KEY_OPEN_AUDIO, openAudio);
        starter.putExtra(KEY_AUDIO_QUALITY, audioQuality);
        starter.putExtra(KEY_VIDEO_QUALITY, videoQuality);
        starter.putExtra(KEY_IS_CREATE_ROOM, isCreate);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 应用运行时，保持不锁屏、全屏化
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        UserModelManager.getInstance().getUserModel().userType = UserModel.UserType.ROOM;
        StateBarUtils.setDarkStatusBar(this);
        setContentView(R.layout.tuiroom_activity_main);
        initData();
        initView();
        checkNeedShowSecurityTips();
        PermissionUtils.permission(PermissionConstants.CAMERA, PermissionConstants.MICROPHONE).callback(
                new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(List<String> permissionsGranted) {
                        startCreateOrEnterRoom();
                        checkNeedShowSecurityTips();
                    }

                    @Override
                    public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                        ToastUtils.showShort(R.string.tuiroom_tips_start_camera_audio);
                        finish();
                    }
                }).request();
    }

    // 首次TRTC打开摄像头提示"Demo特别配置了无限期云端存储"
    private void checkNeedShowSecurityTips() {
        if (UserModelManager.getInstance().needShowSecurityTips() && !isFinishing()) {
            AlertDialog.Builder normalDialog = new AlertDialog.Builder(this);
            normalDialog.setMessage(getResources().getString(R.string.tuiroom_first_enter_room_tips));
            normalDialog.setCancelable(false);
            normalDialog.setPositiveButton(getResources().getString(R.string.tuiroom_dialog_ok), null);
            normalDialog.show();
        }
    }

    @Override
    public void onBackPressed() {
        if (mRemoteUserView.isShown()) {
            mRemoteUserView.setVisibility(View.GONE);
            StateBarUtils.setDarkStatusBar(RoomMainActivity.this);
            return;
        }
        preExitRoom();
    }

    @Override
    protected void onDestroy() {
        hideFloatingWindow();
        mBeautyControl.clear();
        mTUIRoomCore.setListener(null);
        mTUIRoomCore.stopScreenCapture();
        mTUIRoomCore.stopCameraPreview();
        super.onDestroy();
        UserModelManager.getInstance().getUserModel().userType = UserModel.UserType.NONE;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (null != mBeautyControl && mBeautyControl.isShowing()) {
            mBeautyControl.hide();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void preExitRoom() {
        String notifyMsg = "";
        if (mIsCreateRoom) {
            notifyMsg = getString(R.string.tuiroom_msg_exit_room);
        } else {
            notifyMsg = getString(R.string.tuiroom_msg_confirm_exit_room);
        }
        showExitInfoDialog(notifyMsg, false);
    }

    private void exitRoomConfirm() {
        if (mIsCreateRoom) {
            mTUIRoomCore.destroyRoom(null);
        } else {
            mTUIRoomCore.leaveRoom(null);
        }
    }

    private void createRoom() {
        if (mIsCreateRoom) {
            mTUIRoomCore.createRoom(mRoomId, TUIRoomCoreDef.SpeechMode.FREE_SPEECH,
                    new TUIRoomCoreCallback.ActionCallback() {
                        @Override
                        public void onCallback(int code, String msg) {
                            if (code == 0) {
                                // 创建房间成功
                                ToastUtils.showLong(getString(R.string.tuiroom_toast_create_room_successfully));
                                mRoomHeadBarView.setTitle(String.valueOf(mRoomId));
                                changeResolution();
                            } else {
                                ToastUtils.showShort(msg);
                                finish();
                            }
                        }
                    });
        } else {
            mTUIRoomCore.enterRoom(mRoomId, new TUIRoomCoreCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    if (code == 0) {
                        if (isRoomOwner()) {
                            mRemoteUserView.setOwner(true);
                            mIsCreateRoom = true;
                        }
                        ToastUtils.showLong(getString(R.string.tuiroom_enter_room_success));
                        mRoomHeadBarView.setTitle(String.valueOf(mRoomId));
                    } else {
                        ToastUtils.showShort(msg);
                        finish();
                    }
                    changeResolution();
                }
            });
        }
    }

    private boolean isRoomOwner() {
        TUIRoomCoreDef.RoomInfo roomInfo = mTUIRoomCore.getRoomInfo();
        if (roomInfo == null) {
            return false;
        }
        return mUserId.equals(roomInfo.ownerId);
    }

    private void startCreateOrEnterRoom() {
        //设置默认状态
        FeatureConfig.getInstance().setRecording(false);
        FeatureConfig.getInstance().setAudioVolumeEvaluation(true);
        mTUIRoomCore.setListener(this);
        mRoomHeadBarView.setTitle(getString(R.string.tuiroom_title_entering));
        createRoom();
        // 根据外面传入的设置，选择是否打开相应的功能
        mTUIRoomCore.setAudioQuality(mAudioQuality);
        if (mOpenAudio) {
            mTUIRoomCore.startLocalAudio(mAudioQuality);
        } else {
            mTUIRoomCore.stopLocalAudio();
        }
        if (mOpenCamera) {
            mTUIRoomCore.startCameraPreview(mIsFrontCamera, mViewVideo.getLocalPreviewView());
        }
        mTUIRoomCore.setSpeaker(mIsUseSpeaker);
        mRoomHeadBarView.setHeadsetImg(mIsUseSpeaker);
        mTUIRoomCore.enableAudioEvaluation(FeatureConfig.getInstance().isAudioVolumeEvaluation());
    }

    private void initData() {
        mTUIRoomCore = TUIRoomCore.getInstance(this);
        mStringMemberEntityMap = new HashMap<>();
        mMemberEntityList = new ArrayList<>();
        //从外界获取数据源
        Intent starter = getIntent();
        mRoomId = starter.getStringExtra(KEY_ROOM_ID);
        mUserId = starter.getStringExtra(KEY_USER_ID);
        mUserName = starter.getStringExtra(KEY_USER_NAME);
        mUserAvatar = starter.getStringExtra(KEY_USER_AVATAR);
        mOpenCamera = starter.getBooleanExtra(KEY_OPEN_CAMERA, true);
        mOpenAudio = starter.getBooleanExtra(KEY_OPEN_AUDIO, true);
        mAudioQuality = starter.getIntExtra(KEY_AUDIO_QUALITY, TRTCCloudDef.TRTC_AUDIO_QUALITY_DEFAULT);
        mVideoQuality = starter.getIntExtra(KEY_VIDEO_QUALITY, CreateRoomActivity.VIDEO_QUALITY_FAST);
        mIsCreateRoom = starter.getBooleanExtra(KEY_IS_CREATE_ROOM, false);
    }

    private void initView() {
        mAnchorListView = (AnchorListView) findViewById(R.id.anchor_list);
        mRoomHeadBarView = (RoomHeadBarView) findViewById(R.id.view_head_bar);
        mAudioImg = (AppCompatImageButton) findViewById(R.id.img_audio);
        mAudioImg.setOnClickListener(this);
        mVideoImg = (AppCompatImageButton) findViewById(R.id.img_video);
        mVideoImg.setOnClickListener(this);
        mBeautyImg = (AppCompatImageButton) findViewById(R.id.img_beauty);
        mBeautyImg.setOnClickListener(this);
        mMemberImg = (AppCompatImageButton) findViewById(R.id.img_member);
        mMemberImg.setOnClickListener(this);
        mMoreImg = (AppCompatImageButton) findViewById(R.id.img_more);
        mMoreImg.setOnClickListener(this);
        mBeautyControl = new BeautyPanel(this);
        mBeautyControl.setBeautyManager(mTUIRoomCore.getBeautyManager());
        mStubRemoteUserView = (ViewStub) findViewById(R.id.view_stub_remote_user);
        mStubRemoteUserView.inflate();
        mRemoteUserView = (RemoteUserListView) findViewById(R.id.view_remote_user);
        initRemoteMemberView();
        mFeatureSettingFragmentDialog = new FeatureSettingFragmentDialog();
        mFeatureSettingFragmentDialog.setTUIRoomCore(mTUIRoomCore);
        mFeatureSettingFragmentDialog.setShareButtonClickListener(
                new FeatureSettingFragmentDialog.OnShareButtonClickListener() {
                    @Override
                    public void onClick() {
                        onShareScreenClick();
                    }
                });
        // 设置界面UI
        mVideoImg.setSelected(mOpenCamera);
        mAudioImg.setSelected(mOpenAudio);
        mRoomHeadBarView.setTitle(String.valueOf(mRoomId));
        mRoomHeadBarView.setHeadBarCallback(new RoomHeadBarView.HeadBarCallback() {
            @Override
            public void onHeadSetClick() {
                mIsUseSpeaker = !mIsUseSpeaker;
                mTUIRoomCore.setSpeaker(mIsUseSpeaker);
                mRoomHeadBarView.setHeadsetImg(mIsUseSpeaker);
            }

            @Override
            public void onSwitchCameraClick() {
                mIsFrontCamera = !mIsFrontCamera;
                mTUIRoomCore.switchCamera(mIsFrontCamera);
            }

            @Override
            public void onExitClick() {
                preExitRoom();
            }
        });
        mScreenCaptureGroup = findViewById(R.id.group_screen_capture);
        mBottomToolBarGroup = (Group) findViewById(R.id.group_bottom_tool_bar);
        mStopScreenCaptureTv = (TextView) findViewById(R.id.tv_stop_screen_capture);
        //创建自己的 MemberEntity
        RoomVideoView roomVideoView = new RoomVideoView(this);
        roomVideoView.setSelfView(true);
        roomVideoView.setUserId(mUserId);
        roomVideoView.setNeedAttach(true);
        MemberEntity entity = new MemberEntity();
        entity.setRoomVideoView(roomVideoView);
        entity.setShowAudioEvaluation(FeatureConfig.getInstance().isAudioVolumeEvaluation());
        entity.setAudioAvailable(mOpenAudio);
        entity.setVideoAvailable(mOpenCamera);
        entity.setUserId(mUserId);
        entity.setUserName(mUserName);
        entity.setUserAvatar(mUserAvatar);
        entity.setSelf(true);
        entity.setRole(mIsCreateRoom ? TUIRoomCoreDef.Role.MASTER : TUIRoomCoreDef.Role.ANCHOR);
        mSelfEntity = entity;
        addMemberEntity(entity);
        mAnchorListView.setData(mMemberEntityList);
        mViewVideo = mSelfEntity.getRoomVideoView();
        mAnchorListView.setListener(new AnchorListView.Listener() {
            @Override
            public void onViewStart(String userId, TXCloudVideoView txCloudVideoView, boolean isSharingScreen) {
                Log.d(TAG, "onViewStart:" + userId + " isSharingScreen: " + isSharingScreen);
                TUIRoomCoreDef.SteamType steamType = isSharingScreen ? TUIRoomCoreDef.SteamType.SCREE :
                        TUIRoomCoreDef.SteamType.CAMERA;
                mTUIRoomCore.startRemoteView(userId, txCloudVideoView, steamType, null);
            }

            @Override
            public void onViewStop(String userId, boolean isSharingScreen) {
                Log.d(TAG, "onViewStop:" + userId + " isSharingScreen: " + isSharingScreen);
                TUIRoomCoreDef.SteamType steamType = isSharingScreen ? TUIRoomCoreDef.SteamType.SCREE :
                        TUIRoomCoreDef.SteamType.CAMERA;
                mTUIRoomCore.stopRemoteView(userId, steamType, null);
            }

            @Override
            public void onViewStopPlay(String userId) {
                Log.d(TAG, "onViewStopPlay:" + userId);
                MemberEntity entity = mStringMemberEntityMap.get(userId);
                if (entity != null) {
                    entity.getRoomVideoView().setPlayingWithoutSetVisible(false);
                    TUIRoomCoreDef.SteamType steamType = entity.isScreenShareAvailable()
                            ? TUIRoomCoreDef.SteamType.SCREE : TUIRoomCoreDef.SteamType.CAMERA;
                    mTUIRoomCore.stopRemoteView(userId, steamType, null);
                }
            }
        });
    }


    @Override
    public void onError(int code, String message) {
        if (code == -1308) {
            ToastUtils.showLong(getString(R.string.tuiroom_toast_start_screen_recording_failed));
            stopScreenCapture();
        } else {
            ToastUtils.showLong(getString(R.string.tuiroom_toast_error, code, message));
            finish();
        }
    }

    @Override
    public void onDestroyRoom() {
        ToastUtils.showShort(getString(R.string.tuiroom_toast_end_room));
        showSingleConfirmDialog(getString(R.string.tuiroom_room_room_destroyed));
    }

    @Override
    public void onUserVoiceVolume(String userId, int volume) {
        if (!FeatureConfig.getInstance().isAudioVolumeEvaluation()) {
            return;
        }
        if (userId == null) {
            userId = mUserId;
        }
        MemberEntity memberEntity = mStringMemberEntityMap.get(userId);
        if (memberEntity != null) {
            memberEntity.setAudioVolume(volume);
            boolean change = false;
            if (memberEntity.getAudioVolume() > 15) {
                if (!memberEntity.isTalk()) {
                    memberEntity.setTalk(true);
                    change = true;
                }
            } else {
                if (memberEntity.isTalk()) {
                    memberEntity.setTalk(false);
                    change = true;
                }
            }
            if (change) {
                mAnchorListView.notifyItemChanged(mMemberEntityList.indexOf(memberEntity), MemberListAdapter.VOLUME);
            }
        }
    }

    @Override
    public void onRoomMasterChanged(final String previousUserId, final String currentUserId) {
        Log.d(TAG, "onRoomMasterChanged previousUserId:" + previousUserId + " currentUserId:" + currentUserId);
        final MemberEntity currentOwnerInfo = mStringMemberEntityMap.get(currentUserId);
        if (currentOwnerInfo == null) {
            Log.d(TAG, "currentOwnerInfo is null");
            return;
        }
        mTUIRoomCore.getUserInfo(currentUserId, new TUIRoomCoreCallback.UserInfoCallback() {
            @Override
            public void onCallback(int code, String msg, TUIRoomCoreDef.UserInfo userInfo) {
                if (code == 0) {
                    currentOwnerInfo.setRole(userInfo.role);
                    boolean isSelfOwner = currentUserId.equals(mUserId);
                    Log.d(TAG, "onRoomMasterChanged current isSelfOwner " + isSelfOwner);
                    if (isSelfOwner) {
                        mIsCreateRoom = true;
                    }
                    Collections.sort(mMemberEntityList, new Comparator<MemberEntity>() {
                        @Override
                        public int compare(MemberEntity o1, MemberEntity o2) {
                            if (o1.getRole() == TUIRoomCoreDef.Role.MASTER) {
                                return -1;
                            }
                            return 1;
                        }
                    });
                    if (isSelfOwner) {
                        mRemoteUserView.setOwner(true);
                    }
                    int position = mMemberEntityList.indexOf(currentOwnerInfo);
                    mAnchorListView.notifyItemChanged(position, true);
                    final MemberEntity previousInfo = mStringMemberEntityMap.get(previousUserId);
                    if (previousInfo == null) {
                        Log.d(TAG, "previousInfo is null");
                        return;
                    }
                    mTUIRoomCore.getUserInfo(previousUserId, new TUIRoomCoreCallback.UserInfoCallback() {
                        @Override
                        public void onCallback(int code, String msg, TUIRoomCoreDef.UserInfo userInfo) {
                            if (code == 0) {
                                previousInfo.setRole(userInfo.role);
                                boolean isSelfOwner = previousUserId.equals(mUserId);
                                if (isSelfOwner) {
                                    mIsCreateRoom = false;
                                }
                                Log.d(TAG, "onRoomMasterChanged previous isSelfOwner " + isSelfOwner);
                                if (isSelfOwner) {
                                    mRemoteUserView.setOwner(false);
                                }
                                int position = mMemberEntityList.indexOf(previousInfo);
                                mAnchorListView.notifyItemChanged(position);
                            }
                        }
                    });

                }
            }
        });
    }

    @Override
    public void onRemoteUserEnter(String userId) {
        Log.d(TAG, "onRemoteUserEnter userId: " + userId);
    }

    @Override
    public void onRemoteUserLeave(String userId) {
        Log.d(TAG, "onRemoteUserLeave userId: " + userId);
    }

    @Override
    public void onRemoteUserCameraAvailable(String userId, boolean available) {
        Log.d(TAG, "onRemoteUserCameraAvailable userId: " + userId + " available: " + available);
        MemberEntity entity = mStringMemberEntityMap.get(userId);
        if (entity == null) {
            return;
        }
        entity.setCameraAvailable(available);
        if (entity.isScreenShareAvailable()) {
            Log.d(TAG, "camera available in screen capture， ignore");
            return;
        }
        entity.setNeedFresh(true);
        boolean isVideoAvailable = entity.isScreenShareAvailable() || entity.isCameraAvailable();
        entity.setVideoAvailable(isVideoAvailable);
        entity.getRoomVideoView().setNeedAttach(available);
        mRemoteUserView.updateRemoteUserVideo(userId, available);
        if (mAnchorListView != null) {
            mAnchorListView.notifyItemChanged(mMemberEntityList.indexOf(entity));
        }
    }

    @Override
    public void onRemoteUserAudioAvailable(String userId, boolean available) {
        Log.d(TAG, "onRemoteUserAudioAvailable userId: " + userId + " available: " + available);
        MemberEntity entity = mStringMemberEntityMap.get(userId);
        if (entity != null) {
            entity.setAudioAvailable(available);
            mRemoteUserView.updateRemoteUserAudio(userId, available);
        }
    }

    @Override
    public void onRemoteUserScreenVideoAvailable(String userId, boolean available) {
        Log.d(TAG, "onRemoteUserScreenVideoAvailable userId: " + userId + " available: " + available);
        mFeatureSettingFragmentDialog.enableShareButton(!available);
        if (available && mAnchorListView.isShareScreen()) {
            Log.d(TAG, "in screen capture， ignore");
            return;
        }
        final MemberEntity entity = mStringMemberEntityMap.get(userId);
        if (entity != null) {
            entity.setScreenShareAvailable(available);
            boolean isVideoAvailable = entity.isScreenShareAvailable() || entity.isCameraAvailable();
            if (isVideoAvailable) {
                final RoomVideoView roomVideoView = new RoomVideoView(RoomMainActivity.this);
                roomVideoView.setUserId(userId);
                entity.setRoomVideoView(roomVideoView);
            }
            entity.setNeedFresh(true);
            entity.setVideoAvailable(isVideoAvailable);
            entity.getRoomVideoView().setNeedAttach(isVideoAvailable);
            mRemoteUserView.updateRemoteUserVideo(userId, entity.isCameraAvailable());
            if (mAnchorListView != null) {
                mAnchorListView.notifyScreenShare(available, entity);
            }
        }
    }

    @Override
    public void onRemoteUserEnterSpeechState(final String userId) {
        Log.d(TAG, "onRemoteUserEnterSpeechState userId: " + userId);
        final MemberEntity entity = new MemberEntity();
        entity.setUserId(userId);
        entity.setRole(TUIRoomCoreDef.Role.ANCHOR);
        final int insertIndex = mMemberEntityList.size();
        final RoomVideoView roomVideoView = new RoomVideoView(RoomMainActivity.this);
        roomVideoView.setUserId(userId);
        roomVideoView.setNeedAttach(false);
        entity.setRoomVideoView(roomVideoView);
        entity.setShowAudioEvaluation(FeatureConfig.getInstance().isAudioVolumeEvaluation());
        addMemberEntity(entity);
        mAnchorListView.notifyItemInserted(insertIndex);
        changeResolution();
        mTUIRoomCore.getUserInfo(userId,
                new TUIRoomCoreCallback.UserInfoCallback() {
                    @Override
                    public void onCallback(int code, String msg, TUIRoomCoreDef.UserInfo userInfo) {
                        if (code == 0) {
                            entity.setUserName(userInfo.userName);
                            entity.setUserAvatar(userInfo.userAvatar);
                            entity.setRole(userInfo.role);
                            boolean isChangeSort = entity.getRole() == TUIRoomCoreDef.Role.MASTER;
                            if (isChangeSort) {
                                Collections.sort(mMemberEntityList, new Comparator<MemberEntity>() {
                                    @Override
                                    public int compare(MemberEntity o1, MemberEntity o2) {
                                        if (o1.getRole() == TUIRoomCoreDef.Role.MASTER) {
                                            return -1;
                                        }
                                        return 1;
                                    }
                                });
                            }
                            mAnchorListView.notifyItemChanged(mMemberEntityList.indexOf(entity), isChangeSort);
                            mRemoteUserView.updateRemoteUserInfo(userInfo.userId, userInfo.userName,
                                    userInfo.userAvatar);
                        }
                    }
                });
    }

    @Override
    public void onRemoteUserExitSpeechState(String userId) {
        int index = removeMemberEntity(userId);
        if (index >= 0) {
            mAnchorListView.notifyItemRemoved(index, userId);
        }
    }

    @Override
    public void onReceiveChatMessage(String userId, String message) {

    }

    @Override
    public void onReceiveRoomCustomMsg(String userId, String data) {

    }

    @Override
    public void onReceiveSpeechInvitation(String userId) {

    }

    @Override
    public void onReceiveInvitationCancelled(String userId) {

    }

    @Override
    public void onReceiveSpeechApplication(String userId) {

    }

    @Override
    public void onSpeechApplicationCancelled(String userId) {

    }

    @Override
    public void onSpeechApplicationForbidden(boolean isForbidden) {

    }

    @Override
    public void onOrderedToExitSpeechState(String userId) {

    }

    @Override
    public void onCallingRollStarted(String userId) {

    }

    @Override
    public void onCallingRollStopped(String userId) {

    }

    @Override
    public void onMemberReplyCallingRoll(String userId) {

    }

    @Override
    public void onChatRoomMuted(boolean muted) {

    }

    @Override
    public void onMicrophoneMuted(boolean muted) {
        Log.d(TAG, "onMicrophoneMuted muted " + muted);
        if (muted) {
            ToastUtils.showShort(R.string.tuiroom_mute_audio_by_master);
            onAudioMutedByMaster();
        } else {
            ToastUtils.showShort(R.string.tuiroom_un_mute_audio_by_master);
            mAudioImg.setEnabled(true);
        }
    }

    @Override
    public void onCameraMuted(boolean muted) {
        Log.d(TAG, "onCameraMuted " + muted);
        if (muted) {
            ToastUtils.showShort(R.string.tuiroom_mute_video_by_master);
            onVideoMutedByMaster();
        } else {
            ToastUtils.showShort(R.string.tuiroom_un_mute_video_by_master);
            mVideoImg.setEnabled(true);
            mVideoImg.setActivated(true);
        }
    }

    @Override
    public void onReceiveKickedOff(String userId) {
        Log.d(TAG, "onReceiveKickedOff " + userId);
        showSingleConfirmDialog(getString(R.string.tuiroom_kicked_by_master));
    }

    @Override
    public void onStatistics(TRTCStatistics statistics) {

    }

    @Override
    public void onNetworkQuality(TRTCCloudDef.TRTCQuality localQuality, List<TRTCCloudDef.TRTCQuality> remoteQuality) {
        matchQuality(localQuality, mStringMemberEntityMap.get(mUserId));
        for (TRTCCloudDef.TRTCQuality quality : remoteQuality) {
            matchQuality(quality, mStringMemberEntityMap.get(quality.userId));
        }
    }


    private void matchQuality(TRTCCloudDef.TRTCQuality trtcQuality, MemberEntity entity) {
        if (entity == null) {
            return;
        }
        int oldQulity = entity.getQuality();
        switch (trtcQuality.quality) {
            case TRTCCloudDef.TRTC_QUALITY_Excellent:
            case TRTCCloudDef.TRTC_QUALITY_Good:
                entity.setQuality(MemberEntity.QUALITY_GOOD);
                break;
            case TRTCCloudDef.TRTC_QUALITY_Poor:
            case TRTCCloudDef.TRTC_QUALITY_Bad:
                entity.setQuality(MemberEntity.QUALITY_NORMAL);
                break;
            case TRTCCloudDef.TRTC_QUALITY_Vbad:
            case TRTCCloudDef.TRTC_QUALITY_Down:
                entity.setQuality(MemberEntity.QUALITY_BAD);
                break;
            default:
                entity.setQuality(MemberEntity.QUALITY_NORMAL);
                break;
        }
        if (oldQulity != entity.getQuality()) {
            mAnchorListView.notifyItemChanged(mMemberEntityList.indexOf(entity), MemberListAdapter.QUALITY);
        }
    }

    private void changeResolution() {
        if (isScreenCapture) {
            return;
        }
        TRTCCloudDef.TRTCNetworkQosParam qosParam = new TRTCCloudDef.TRTCNetworkQosParam();
        if (mVideoQuality == CreateRoomActivity.VIDEO_QUALITY_HD) {
            qosParam.preference = TRTCCloudDef.TRTC_VIDEO_QOS_PREFERENCE_CLEAR;
            mTUIRoomCore.setVideoQosPreference(qosParam);
            if (mMemberEntityList.size() <= 2) {
                mTUIRoomCore.setVideoResolution(TRTCCloudDef.TRTC_VIDEO_RESOLUTION_960_540);
                mTUIRoomCore.setVideoFps(15);
                mTUIRoomCore.setVideoBitrate(1300);
            } else if (mMemberEntityList.size() < 4) {
                mTUIRoomCore.setVideoResolution(TRTCCloudDef.TRTC_VIDEO_RESOLUTION_640_360);
                mTUIRoomCore.setVideoFps(15);
                mTUIRoomCore.setVideoBitrate(800);
            } else {
                mTUIRoomCore.setVideoResolution(TRTCCloudDef.TRTC_VIDEO_RESOLUTION_480_270);
                mTUIRoomCore.setVideoFps(15);
                mTUIRoomCore.setVideoBitrate(400);
            }
        } else {
            qosParam.preference = TRTCCloudDef.TRTC_VIDEO_QOS_PREFERENCE_SMOOTH;
            mTUIRoomCore.setVideoQosPreference(qosParam);
            if (mMemberEntityList.size() < 5) {
                // 包括自己，一共四个人，选择360p分辨率
                mTUIRoomCore.setVideoResolution(TRTCCloudDef.TRTC_VIDEO_RESOLUTION_640_360);
                mTUIRoomCore.setVideoFps(15);
                mTUIRoomCore.setVideoBitrate(700);
            } else {
                mTUIRoomCore.setVideoResolution(TRTCCloudDef.TRTC_VIDEO_RESOLUTION_480_270);
                mTUIRoomCore.setVideoFps(15);
                mTUIRoomCore.setVideoBitrate(350);
            }
        }
    }

    @Override
    public void onScreenCaptureStarted() {
        isScreenCapture = true;
        Log.d(TAG, "onScreenCaptureStarted");
    }

    @Override
    public void onScreenCaptureStopped(int reason) {
        Log.d(TAG, "onScreenCaptureStopped");
        isScreenCapture = false;
        changeResolution();
        if (mOpenCamera) {
            mTUIRoomCore.startCameraPreview(mIsFrontCamera, mViewVideo.getLocalPreviewView());
        }
    }

    private void onAudioMutedByMaster() {
        mAudioImg.setSelected(false);
        mAudioImg.setEnabled(false);
        mOpenAudio = false;
    }

    private void onVideoMutedByMaster() {
        if (mSelfEntity != null) {
            mTUIRoomCore.stopCameraPreview();
            mSelfEntity.setVideoAvailable(false);
            mVideoImg.setSelected(false);
            mVideoImg.setEnabled(false);
            mOpenCamera = false;
            mAnchorListView.notifyItemChanged(mMemberEntityList.indexOf(mSelfEntity));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.img_audio) {
            boolean isAudioOn = mOpenAudio;
            if (isAudioOn) {
                mTUIRoomCore.stopLocalAudio();
                mAudioImg.setSelected(false);
                mOpenAudio = false;
            } else {
                mTUIRoomCore.startLocalAudio(mAudioQuality);
                mAudioImg.setSelected(true);
                mOpenAudio = true;
            }
            mAudioImg.setSelected(!isAudioOn);
            mOpenAudio = !isAudioOn;
        } else if (id == R.id.img_video) {
            boolean isVideoOn = mOpenCamera;
            if (isVideoOn) {
                mTUIRoomCore.stopCameraPreview();
            } else {
                RoomVideoView videoView = mSelfEntity.getRoomVideoView();
                mTUIRoomCore.startCameraPreview(mIsFrontCamera, videoView.getLocalPreviewView());
            }
            mOpenCamera = !isVideoOn;
            mVideoImg.setSelected(!isVideoOn);
            mSelfEntity.setVideoAvailable(!isVideoOn);
            if (mSelfEntity != null) {
                mAnchorListView.notifyItemChanged(mMemberEntityList.indexOf(mSelfEntity));
            }
        } else if (id == R.id.img_beauty) {
            if (mBeautyControl.isShowing()) {
                mBeautyControl.hide();
            } else {
                mBeautyControl.show();
            }
        } else if (id == R.id.img_member) {
            handleMemberListView();
        } else if (id == R.id.img_more) {
            showDialogFragment(mFeatureSettingFragmentDialog, "FeatureSettingFragmentDialog");
        }
    }

    private void onShareScreenClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionUtils.isGrantedDrawOverlays()) {
                ToastUtils.showLong(getString(R.string.tuiroom_toast_need_floating_window_permission));
                PermissionUtils.requestDrawOverlays(new PermissionUtils.SimpleCallback() {
                    @Override
                    public void onGranted() {
                        startScreenCapture();
                    }

                    @Override
                    public void onDenied() {
                        ToastUtils.showLong(getString(R.string.tuiroom_toast_need_floating_window_permission));
                    }
                });
            } else {
                startScreenCapture();
            }
        } else {
            startScreenCapture();
        }
    }

    private void startScreenCapture() {
        if (mAnchorListView.isShareScreen()) {
            Log.d(TAG, "start screen capture, in screen capture， ignore");
            return;
        }
        mAnchorListView.setVisibility(View.GONE);
        mScreenCaptureGroup.setVisibility(View.VISIBLE);
        mBottomToolBarGroup.setVisibility(View.GONE);

        mStopScreenCaptureTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScreenCapture();
            }
        });

        TRTCCloudDef.TRTCVideoEncParam encParams = new TRTCCloudDef.TRTCVideoEncParam();
        encParams.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_1280_720;
        encParams.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
        encParams.videoFps = 10;
        encParams.enableAdjustRes = false;
        encParams.videoBitrate = 1500;

        TRTCCloudDef.TRTCScreenShareParams params = new TRTCCloudDef.TRTCScreenShareParams();
        mTUIRoomCore.stopCameraPreview();
        mTUIRoomCore.startScreenCapture(encParams, params);

        if (mFloatingWindow == null) {
            LayoutInflater inflater = LayoutInflater.from(this);
            mFloatingWindow = inflater.inflate(R.layout.tuiroom_screen_capture_floating_window, null, false);
            mFloatingWindow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: 悬浮窗");
                }
            });
        }
        showFloatingWindow();
    }

    private void stopScreenCapture() {
        hideFloatingWindow();
        mAnchorListView.setVisibility(View.VISIBLE);
        mScreenCaptureGroup.setVisibility(View.GONE);
        mBottomToolBarGroup.setVisibility(View.VISIBLE);
        mTUIRoomCore.stopScreenCapture();
        onScreenCaptureStopped(0);
    }

    private void showFloatingWindow() {
        if (mFloatingWindow == null) {
            return;
        }
        WindowManager windowManager =
                (WindowManager) mFloatingWindow.getContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return;
        }
        //TYPE_TOAST仅适用于4.4+系统，假如要支持更低版本使用TYPE_SYSTEM_ALERT(需要在manifest中声明权限)
        //7.1（包含）及以上系统对TYPE_TOAST做了限制
        int type = WindowManager.LayoutParams.TYPE_TOAST;
        if (Build.VERSION.SDK_INT >= 26) {
            type = 2038; // WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(type);
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.gravity = Gravity.RIGHT;
        windowManager.addView(mFloatingWindow, layoutParams);
    }

    private void hideFloatingWindow() {
        if (mFloatingWindow == null) {
            return;
        }
        WindowManager windowManager =
                (WindowManager) mFloatingWindow.getContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.removeViewImmediate(mFloatingWindow);
        }
        mFloatingWindow = null;
    }

    /**
     * 展示dialog界面
     */
    private void showDialogFragment(DialogFragment dialogFragment, String tag) {
        if (dialogFragment != null) {
            if (dialogFragment.isVisible()) {
                try {
                    dialogFragment.dismissAllowingStateLoss();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                if (!dialogFragment.isAdded()) {
                    dialogFragment.show(getSupportFragmentManager(), tag);
                }
            }
        }
    }

    private void initRemoteMemberView() {
        mRemoteUserView.init(mUserId, mIsCreateRoom);
        mRemoteUserView.setVisibility(View.GONE);
        // 监听成员列表的变化
        mRemoteUserView.setRemoteUserListCallback(new RemoteUserListView.RemoteUserListCallback() {
            @Override
            public void onFinishClick() {
                mRemoteUserView.setVisibility(View.GONE);
                StateBarUtils.setDarkStatusBar(RoomMainActivity.this);
            }

            @Override
            public void onMuteAllAudioClick() {
                mRemoteUserView.disableMuteAllAudio(true);
                mTUIRoomCore.muteAllUsersMicrophone(true, new TUIRoomCoreCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        Log.i(TAG, "muteAllUsersMicrophone, code: " + code + "msg: " + msg);
                        if (code == 0) {
                            ToastUtils.showShort(getString(R.string.tuiroom_toast_mute_all_audio));
                        }
                        mRemoteUserView.disableMuteAllAudio(false);
                    }
                });
            }

            @Override
            public void onMuteAllAudioOffClick() {
                mRemoteUserView.disableMuteAllAudio(true);
                mTUIRoomCore.muteAllUsersMicrophone(false, new TUIRoomCoreCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        Log.i(TAG, "unmuteAllUsersMicrophone, code: " + code + "msg: " + msg);
                        if (code == 0) {
                            ToastUtils.showShort(getString(R.string.tuiroom_toast_not_mute_all_audio));
                        }
                        mRemoteUserView.disableMuteAllAudio(false);
                    }
                });
            }

            @Override
            public void onMuteAllVideoClick() {
                mRemoteUserView.disableMuteAllVideo(true);
                mTUIRoomCore.muteAllUsersCamera(true, new TUIRoomCoreCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        Log.i(TAG, "muteAllUsersCamera, code: " + code + "msg: " + msg);
                        if (code == 0) {
                            ToastUtils.showShort(getString(R.string.tuiroom_toast_mute_all_video));
                        }
                        mRemoteUserView.disableMuteAllVideo(false);
                    }
                });
            }

            @Override
            public void onMuteAllVideoOffClick() {
                mRemoteUserView.disableMuteAllVideo(true);
                ToastUtils.showShort(getString(R.string.tuiroom_toast_not_mute_all_video));
                mTUIRoomCore.muteAllUsersCamera(false, new TUIRoomCoreCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        Log.i(TAG, "unmuteAllUsersCamera, code: " + code + "msg: " + msg);
                        if (code == 0) {
                            ToastUtils.showShort(getString(R.string.tuiroom_toast_not_mute_all_video));
                        }
                        mRemoteUserView.disableMuteAllVideo(false);
                    }
                });
            }

            @Override
            public void onMuteAudioClick(String userId) {
                mRemoteUserView.disableMuteAudio(true);
                MemberEntity entity = mStringMemberEntityMap.get(userId);
                if (entity != null) {
                    final boolean mute = entity.isAudioAvailable();
                    mTUIRoomCore.muteUserMicrophone(entity.getUserId(), mute,
                            new TUIRoomCoreCallback.ActionCallback() {
                                @Override
                                public void onCallback(int code, String msg) {
                                    Log.i(TAG, "muteUserMicrophone, code: " + code + "msg: " + msg);
                                    mRemoteUserView.disableMuteAudio(false);
                                    if (!mute) {
                                        ToastUtils.showShort(R.string.tuiroom_un_mute_audio_success);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onMuteVideoClick(String userId) {
                mRemoteUserView.disableMuteVideo(true);
                MemberEntity entity = mStringMemberEntityMap.get(userId);
                if (entity != null) {
                    final boolean mute = entity.isVideoAvailable();
                    mTUIRoomCore.muteUserCamera(userId, mute,
                            new TUIRoomCoreCallback.ActionCallback() {
                                @Override
                                public void onCallback(int code, String msg) {
                                    Log.i(TAG, "muteUserMicrophone, code: " + code + "msg: " + msg);
                                    mRemoteUserView.disableMuteVideo(false);
                                    if (!mute) {
                                        ToastUtils.showShort(R.string.tuiroom_un_mute_video_success);
                                    }
                                }
                            });

                }
            }

            @Override
            public void onKickUserClick(String userId) {
                mRemoteUserView.disableKickUser(true);
                MemberEntity entity = mStringMemberEntityMap.get(userId);
                if (entity != null) {
                    showKickDialog(entity);
                }
            }

            @Override
            public void onConfirmButtonClick() {
                handleMemberListView();
            }
        });
        mRemoteUserView.setRemoteUser(mMemberEntityList);
    }

    /**
     * 处理成员列表
     */
    private void handleMemberListView() {
        if (mRemoteUserView.isShown()) {
            mRemoteUserView.setVisibility(View.GONE);
            StateBarUtils.setDarkStatusBar(RoomMainActivity.this);
        } else {
            mRemoteUserView.setVisibility(View.VISIBLE);
            StateBarUtils.setLightStatusBar(RoomMainActivity.this);
        }
    }

    private void addMemberEntity(MemberEntity entity) {
        mMemberEntityList.add(entity);
        mStringMemberEntityMap.put(entity.getUserId(), entity);
        mRemoteUserView.addRemoteUser(entity);
    }

    private int removeMemberEntity(String userId) {
        MemberEntity entity = mStringMemberEntityMap.remove(userId);
        mRemoteUserView.removeRemoteUser(userId);
        if (entity != null) {
            int i = mMemberEntityList.indexOf(entity);
            mMemberEntityList.remove(entity);
            return i;
        }
        return -1;
    }

    /**
     * 显示确认消息
     *
     * @param msg     消息内容
     * @param isError true错误消息（必须退出） false提示消息（可选择是否退出）
     */
    private void showExitInfoDialog(String msg, Boolean isError) {
        final ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment();
        dialogFragment.setCancelable(true);
        dialogFragment.setMessage(msg);
        if (dialogFragment.isAdded()) {
            dialogFragment.dismiss();
            return;
        }
        if (!isError) {
            dialogFragment.setPositiveText(getString(R.string.tuiroom_dialog_ok));
            dialogFragment.setNegativeText(getString(R.string.tuiroom_dialog_cancel));
            dialogFragment.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
                @Override
                public void onClick() {
                    dialogFragment.dismiss();
                    exitRoomConfirm();
                    finish();
                }
            });

            dialogFragment.setNegativeClickListener(new ConfirmDialogFragment.NegativeClickListener() {
                @Override
                public void onClick() {
                    dialogFragment.dismiss();
                }
            });
        } else {
            //当情况为错误的时候，直接停止推流
            dialogFragment.setPositiveText(getString(R.string.tuiroom_dialog_ok));
            dialogFragment.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
                @Override
                public void onClick() {
                    dialogFragment.dismiss();
                }
            });
        }
        dialogFragment.show(getFragmentManager(), "ConfirmDialogFragment");
    }

    private void showKickDialog(final MemberEntity entity) {
        final ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment();
        dialogFragment.setCancelable(true);
        dialogFragment.setMessage(getString(R.string.tuiroom_kick_user_confirm, entity.getUserName()));
        if (dialogFragment.isAdded()) {
            dialogFragment.dismiss();
            mRemoteUserView.disableKickUser(false);
            return;
        }
        dialogFragment.setPositiveText(getString(R.string.tuiroom_dialog_ok));
        dialogFragment.setNegativeText(getString(R.string.tuiroom_dialog_cancel));
        dialogFragment.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
            @Override
            public void onClick() {
                String userId = entity.getUserId();
                if (!mStringMemberEntityMap.containsKey(userId)) {
                    Log.d(TAG, "the user not in group");
                    dialogFragment.dismiss();
                    mRemoteUserView.disableKickUser(false);
                    return;
                }
                mTUIRoomCore.kickOffUser(userId, new TUIRoomCoreCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        Log.i(TAG, "kick user, code: " + code + "msg: " + msg);
                        mRemoteUserView.disableKickUser(false);
                        dialogFragment.dismiss();
                    }
                });
            }
        });

        dialogFragment.setNegativeClickListener(new ConfirmDialogFragment.NegativeClickListener() {
            @Override
            public void onClick() {
                dialogFragment.dismiss();
                mRemoteUserView.disableKickUser(false);
            }
        });
        dialogFragment.show(getFragmentManager(), "ConfirmDialogFragment");
    }

    private void showSingleConfirmDialog(String message) {
        if (mIsPaused) {
            finish();
            return;
        }
        final ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment();
        dialogFragment.setCancelable(true);
        dialogFragment.setMessage(message);
        if (dialogFragment.isAdded()) {
            dialogFragment.dismiss();
            return;
        }
        dialogFragment.setPositiveText(getString(R.string.tuiroom_dialog_ok));
        dialogFragment.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
            @Override
            public void onClick() {
                dialogFragment.dismiss();
                finish();
            }
        });
        dialogFragment.show(getFragmentManager(), "ConfirmDialogFragment");
    }


    @Override
    protected void onResume() {
        super.onResume();
        mIsPaused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsPaused = true;
    }
}
