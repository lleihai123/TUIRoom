// Copyright (c) 2021 Tencent. All rights reserved.
#ifndef MODULE_IM_CORE_H_
#define MODULE_IM_CORE_H_

#include <string>
#include <vector>
#include <map>
#include <mutex>
#include "../CommonDef.h"
#include "../include/TUIRoomDef.h"
#include "IMCoreCallback.h"
#include "V2TIMManager.h"
#include "V2TIMCallback.h"
#include "V2TIMGroup.h"
#include "V2TIMGroupManager.h"
#include "V2TIMConversation.h"
#include "V2TIMListener.h"
#include "V2TIMSignalingManager.h"
#include "V2TIMMessageManager.h"

enum CallBackType {
    kUnkown,
    kLogin,
    kLoginOut,
    kCreateGroup,
    kDestoryGroup,
    kJoinGroup,
    kQuitGroup,
    kTransferGroup,
    kSetSelfProfile,
    kGetGroupMemberList,
    kGetGroupInfo,
    kKickOffUser,
    kMuteChatRoom,
    kMuteUserMicrophone,
    kMuteAllUserMicrophone,
    kMuteUserCamera,
    kMuteAllUserCamera,
    kStartCallingRoll,
    kStopCallingRoll,
    kReplyCallingRoll,
    kSendMessage,
    kSendSpeechInvitation,
    kCancelSpeechInvitation,
    kReplySpeechInvitation,
    kSendOffSpeaker,
    kSendOffAllSpeakers,
    kSendSpeechApplication,
    kCancelSpeechApplication,
    kReplySpeechApplication,
    kForbidSpeechApplication,
};

struct SignalingInfo;
class IMCore final: public V2TIMSDKListener, public V2TIMGroupListener, public V2TIMSignalingListener, public V2TIMAdvancedMsgListener {
public:
    explicit IMCore();
    ~IMCore();
    void SetCallback(IMCoreCallback* callback);

    void Login(int sdk_appid, const std::string& user_id, const std::string& user_sig);
    void Logout();
    void CreateRoom(const std::string& room_id, TUISpeechMode speech_mode);
    void DestroyRoom(const std::string& room_id);
    void EnterRoom(const std::string& room_id, const std::string& user_id);
    void LeaveRoom(const std::string& room_id);
    void TransferRoomMaster(const std::string& room_id, const std::string& user_id);
    void SetSelfProfile(const std::string& user_name, const std::string& avatar_url);
    /**
    * 获取群成员列表
    * 该获取群成员的接口只支持单线程访问，通过OnIMGetRoomMemberInfoList回调接口返回成员列表。
    * 注意：如果返回false,则说明上次的获取还没完成，请稍后再获取。
    */
    bool GetRoomMemberInfoList(const std::string& room_id);
    void GetRoomInfo(const std::string& room_id);
    void SendChatMessage(const std::string& room_id, const std::string& user_id, const std::string& content);
    void SendCustomMessage(const std::string& room_id, const std::string& user_id, const std::string& content);

    // 信令
    // 将用户请出房间
    void KickOffUser(const std::string& room_id, const std::string& user_id, Callback callback);
    // 1V1 信令
    // 邀请发言
    void SendSpeechInvitation(const std::string& room_id, const std::string& sender_id, const std::string& user_id, Callback callback);
    // 取消邀请
    void CancelSpeechInvitation(const std::string& room_id, const std::string& sender_id, const std::string& user_id, Callback callback);
    // 成员回复主持人发言邀请
    void ReplySpeechInvitation(const std::string& room_id, const std::string& sender_id,const std::string& user_id,
        bool agree, Callback callback);

    // 成员申请发言
    void SendSpeechApplication(const std::string& room_id, const std::string& sender_id, const std::string& user_id, Callback callback);
    // 成员取消发言申请
    void CancelSpeechApplication(const std::string& room_id, const std::string& sender_id, const std::string& user_id, Callback callback);
    // 主持人回复成员的发言申请
    void ReplySpeechApplication(const std::string& room_id, const std::string& sender_id, const std::string& user_id,
        bool agree, Callback callback);
    // 禁用用户麦克风
    void MuteUserMicrophone(const std::string& room_id, const std::string& sender_id, const std::string& user_id, bool mute, Callback callback);
    // 禁用用户摄像头
    void MuteUserCamera(const std::string& room_id, const std::string& sender_id, const std::string& user_id, bool mute, Callback callback);
    // 回复主持人点名
    void ReplyCallingRoll(const std::string& room_id, const std::string& sender_id, const std::string& user_id, Callback callback);

    // 命令停止发言
    void SendOffSpeaker(const std::string& room_id, const std::string& sender_id, const std::string& user_id, Callback callback);
    // 命令所有学生停止发言
    void SendOffAllSpeakers(const std::string& room_id, const std::string& sender_id, const std::vector<std::string>& user_id_array, Callback callback);

    // 群信令
    // 禁用IM聊天
    void MuteRoomChat(const std::string& room_id, bool mute);
    // 禁用所有用户麦克风
    void MuteAllUsersMicrophone(const std::string& room_id, bool mute);
    // 禁用所有用户摄像头
    void MuteAllUsersCamera(const std::string& room_id, bool mute);
    // 主持人开始点名
    void StartCallingRoll(const std::string& room_id);
    // 主持人结束点名
    void StopCallingRoll(const std::string& room_id);
    // 禁止发言申请
    void ForbidSpeechApplication(const std::string& room_id, bool forbid);

    // 调用IM接口的返回：code为0表示成功，其他表示失败，对应的失败信息为message
    void OnIMInterfaceCallback(CallBackType type, int code, const std::string& message = "", const V2TIMString& result = "");
    void OnIMInterfaceCallback(CallBackType type, int code, const std::string& message, const V2TIMGroupMemberInfoResult& result);
    void OnIMInterfaceCallback(CallBackType type, int code, const std::string& message, const V2TIMGroupInfoResultVector& result);
    void OnIMInterfaceCallback(CallBackType type, int code, const std::string& message, const V2TIMMessage& result);

    // 信令操作的回调
    void OnInviteeAccepted(const V2TIMString &inviteID, const V2TIMString &invitee, const V2TIMString &data) override;
    void OnInviteeRejected(const V2TIMString &inviteID, const V2TIMString &invitee, const V2TIMString &data) override;
    void OnInvitationCancelled(const V2TIMString &inviteID, const V2TIMString &inviter, const V2TIMString &data) override;
    void OnInvitationTimeout(const V2TIMString &inviteID, const V2TIMStringVector &inviteeList) override;

    void OnConnectSuccess() override;
    void OnConnectFailed(int error_code, const V2TIMString &error_message) override;
    void OnKickedOffline() override;

    // 有用户离开群（全员能够收到）
    void OnMemberLeave(const V2TIMString &groupID, const V2TIMGroupMemberInfo &member) override;
    // 有用户加入群（全员能够收到）
    void OnMemberEnter(const V2TIMString &groupID, const V2TIMGroupMemberInfoVector &memberList) override;
    // 群被解散了（全员能收到）
    void OnGroupDismissed(const V2TIMString &groupID, const V2TIMGroupMemberInfo &opUser) override;
    // 群信息被修改（全员能收到）
    void OnGroupInfoChanged(const V2TIMString &groupID, const V2TIMGroupChangeInfoVector &changeInfos) override;
    // 接收信令消息
    void OnReceiveNewInvitation(const V2TIMString &inviteID, const V2TIMString &inviter,
        const V2TIMString &groupID,
        const V2TIMStringVector &inviteeList,
        const V2TIMString &data) override;
    // 接收新消息
    void OnRecvNewMessage(const V2TIMMessage &message);
private:
    void IMInit(int sdkappid);
    void IMUnInit();
    void AddSignalingInfo(const std::string& user_id, const std::string& invite_id, CallBackType type);
    void RemoveSignalingInfo(const std::string& user_id, const std::string& invite_id);
    void RemoveSignalingInfoByInviteId(const std::string& invite_id);
    void AddSignalingCallback(const std::string& invite_id, Callback callback);
    void RemoveSignalingCallback(const std::string& invite_id);
    void NotifySignalingTimeOut(const std::string& invite_id);
private:
    IMCoreCallback* callback_ = nullptr;
    bool init_sdk_success_ = false;

    std::vector<TUIUserInfo> member_array_;
    bool member_list_request_done_ = true;
    std::string user_sig_ = "";
    std::string user_id_ = "";
    TUIRoomInfo   room_info_;

    std::mutex mutex_;    
    std::map<std::string, std::vector<SignalingInfo>> map_signaling_;
    std::map<std::string, Callback> map_callback_;
};

struct SignalingInfo {
    std::string user_id;
    std::string invite_id;
    CallBackType type;
};

#define NOTIFY_CALLBACK(callback,error_code,error_message) {\
    if (callback != nullptr) {\
        callback(error_code, error_message);\
    }\
}

#endif  //  MODULE_IM_CORE_H_
