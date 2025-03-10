//
//  TUIRoomMainViewController+UI.swift
//  TUIRoom
//
//  Created by WesleyLei on 2021/12/16.
//  Copyright © 2021 Tencent. All rights reserved.
//

import Foundation
import ImSDK_Plus
import SnapKit
import Toast_Swift
import TXAppBasic
import UIKit

extension TUIRoomMainViewController {
    func initAttendeeList() {
        currentUser.userInfo.isAudioAvailable = isAudioOn
        currentUser.userInfo.isVideoAvailable = isVideoOn
        if !roomInfo.isHomeowner() {
            currentUser.userInfo.isRemoteAudioMuted = roomInfo.isAllMicMuted
            currentUser.userInfo.isRemoteVideoMuted = roomInfo.isAllCameraMuted
        }
        attendeeList = [currentUser]
        attendeeMap[currentUser.userId()] = currentUser
    }

    func applyConfigs() {
        // 开启摄像头和麦克风
        TUIRoomCore.shareInstance().enableAudioEvaluation(true)
        if currentUser.isVideoOpen() {
            if let localPreviewView = getRenderViewByUserid(userId: currentUser.userId()) {
                TUIRoomCore.shareInstance().startCameraPreview(true, view: localPreviewView)
            }
        } else {
            TUIRoomCore.shareInstance().stopCameraPreview()
        }
        TUIRoomCore.shareInstance().startLocalAudio(.speech)
        if !currentUser.isAudioOpen() {
            TUIRoomCore.shareInstance().stopLocalAudio()
        }
        // Video
        TUIRoomCore.shareInstance().setVideoMirror(.enable)
        // Audio
        TUIRoomCore.shareInstance().setAudioPlayVolume(100)
        TUIRoomCore.shareInstance().setAudioCaptureVolume(100)
        TUIRoomCore.shareInstance().stopFileDumping()
        TUIRoomCore.shareInstance().enableAudioEvaluation(true)
    }

    func userEnterRoom(_ userId: String) {
        debugPrint("userEnterRoom \(userId)")
    }

    func onUserLeaveRoom(_ userId: String) {
        debugPrint("onUserLeaveRoom \(userId)")
        clearUserSource(userId)
        reloadData()
    }

    /**
     * 远端用户开始发言
     *
     * 当您收到此通知时，表示该用户发言成功
     *
     * @param userId 用户ID
     */
    func enterSpeechState(_ userId: String) {
        clearUserSource(userId)
        if let userInfo = TUIRoomUserManage.getUser(userId) {
            let userModel = TUIRoomAttendeeModel()
            userModel.userInfo = userInfo
            attendeeMap[userId] = userModel
            if userModel.userId() == roomInfo.ownerId {
                attendeeList.insert(userModel, at: 0)
            } else {
                attendeeList.append(userModel)
            }
            reloadData()
        } else {
        }
        debugPrint("------enter renderMapView:\(renderMapView.count)  attendeeList:\(attendeeList.count)")
    }

    /**
     * 远端用户下麦
     *
     * 当您收到此通知时，表示该用户已经下麦
     *
     * @param userId 用户ID
     */
    func exitSpeechState(_ userId: String) {
        clearUserSource(userId)
        reloadData()
        debugPrint("------exit renderMapView:\(renderMapView.count)  attendeeList:\(attendeeList.count)")
    }

    /**
     * 清理麦上用户资源
     *
     * @param userId 用户ID
     */
    func clearUserSource(_ userId: String, type: TUIRoomStreamType = .camera) {
        let renderView = getRenderViewByUserid(userId: userId, type: type)
        renderView?.removeFromSuperview()
        if type == .camera {
            attendeeMap.removeValue(forKey: userId)
            renderMapView.removeValue(forKey: userId)
            attendeeList = attendeeList.filter { (model) -> Bool in
                model.userInfo.userId != userId
            }
        } else if type == .screen {
            renderShareMapViews.removeValue(forKey: userId)
            shareAttendeeList = shareAttendeeList.filter { (model) -> Bool in
                model.userInfo.userId != userId
            }
        }
    }

    /**
     * 中断清理
     *
     */
    func interruptClearRoom() {
        if #available(iOS 11.0, *) {
            TUIRoomCore.shareInstance().stopScreenCapture()
        }
        TUIRoomCore.shareInstance().leaveRoom { _, _ in
        }
    }

    /**
     * 中断退出页面
     *
     */
    func interruptQuitRoom() {
        setViewController?.dismiss(animated: false, completion: nil)
        navigationController?.popToRootViewController(animated: true)
    }

    func getRenderViewByUserid(userId: String, type: TUIRoomStreamType = .camera) -> TUIRoomAttendeeRenderView? {
        if type == .camera {
            if let renderView = renderMapView[userId] {
                return renderView
            } else {
                guard let model = attendeeMap[userId] else {
                    return nil
                }
                let renderView = TUIRoomAttendeeRenderView()
                renderView.attendeeModel = model
                renderMapView[model.userId()] = renderView
                return renderView
            }
        } else if type == .screen {
            if let renderView = renderShareMapViews[userId] {
                return renderView
            } else {
                guard let model = attendeeMap[userId] else {
                    return nil
                }
                let renderView = TUIRoomAttendeeRenderView()
                renderView.attendeeModel = model
                renderShareMapViews[model.userId()] = renderView
                return renderView
            }
        }
        return nil
    }
}
