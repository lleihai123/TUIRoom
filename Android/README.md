# TUIRoom Android 示例工程快速跑通
_中文 | [English](README.en.md)_

本文档主要介绍如何快速跑通TUIRoom 示例工程，体验多人音视频互动，更详细的TUIRoom组件接入流程，请点击腾讯云官网文档： [**TUIRoom 组件 Android 接入说明** ](https://cloud.tencent.com/document/product/647/45667)...



## 目录结构

```
TUIRoom
├─ App          // 主面板，多人音视频互动场景入口
├─ Debug        // 调试相关
├─ Source       // 多人音视频互动业务逻辑
├─ Beauty       // 美颜面板，包含美颜，滤镜，动效等效果
└─ TUICore      // 基础通信组件
```

## 环境准备
- 最低兼容 Android 4.2（SDK API Level 17），建议使用 Android 5.0 （SDK API Level 21）及以上版本
- Android Studio 3.5及以上版本

## 运行示例

### 第一步：创建TRTC的应用
1. 一键进入腾讯云实时音视频控制台的[应用管理](https://console.cloud.tencent.com/trtc/app)界面，选择创建应用，输入应用名称，例如 `TUIKitDemo` ，单击 **创建**；
2. 点击对应应用条目后**应用信息**，具体位置如下下图所示：
    <img src="https://qcloudimg.tencent-cloud.cn/raw/62f58d310dde3de2d765e9a460b8676a.png" width="900">
3. 进入应用信息后，按下图操作，记录SDKAppID和密钥：
    <img src="https://qcloudimg.tencent-cloud.cn/raw/bea06852e22a33c77cb41d287cac25db.png" width="900">

>! 本功能同时使用了腾讯云 [实时音视频 TRTC](https://cloud.tencent.com/document/product/647/16788) 和 [即时通信 IM](https://cloud.tencent.com/document/product/269) 两个基础 PaaS 服务，开通实时音视频后会同步开通即时通信 IM 服务。 即时通信 IM 属于增值服务，详细计费规则请参见 [即时通信 IM 价格说明](https://cloud.tencent.com/document/product/269/11673)。


[](id:ui.step2)
### 第二步：下载源码，配置工程
1. 克隆或者直接下载此仓库源码，**欢迎 Star**，感谢~~
2. 找到并打开 `Android/Debug/src/main/java/com/tencent/liteav/debug/GenerateTestUserSig.java` 文件。
3. 配置 `GenerateTestUserSig.java` 文件中的相关参数：
	<img src="https://main.qcloudimg.com/raw/f9b23b8632058a75b78d1f6fdcdca7da.png" width="900">
	- SDKAPPID：默认为占位符（PLACEHOLDER），请设置为步第一步中记录下的 SDKAppID。
	- SECRETKEY：默认为占位符（PLACEHOLDER），请设置为步第一步中记录下的密钥信息。

### 第三步：编译运行
使用 Android Studio（3.5 以上的版本）打开源码目录 `TUIRoom/Android`，待Android Studio工程同步完成后，连接真机单击 **运行按钮** 即可开始体验本APP。

### 第四步：示例体验

Tips：TUIRoom 使用体验，至少需要两台设备，如果用户A/B分别代表两台不同的设备：

**设备 A（userId：111）**

- 步骤1、在欢迎页，输入用户名(请确保用户名唯一性，不能与其他用户重复)，比如111；
- 步骤2、点击创建房间；
- 步骤3、进入到创建房间界面，可以将创建的房间号记录下来；
- 步骤4、进入房间;

| 步骤1 | 步骤2 | 步骤3 | 步骤4 |
|---------|---------|---------|---------|
| <img src="https://liteav.sdk.qcloud.com/doc/res/trtc/picture/zh-cn/user_a.png" width="320"/> | <img src="https://qcloudimg.tencent-cloud.cn/raw/85ab7ea0a66aba5b9ddf23594bf04ea0.png" width="320"/> | <img src="https://qcloudimg.tencent-cloud.cn/raw/b36383baff761bdaf26da5f191902800.png" width="320"/> | <img src="https://qcloudimg.tencent-cloud.cn/raw/5f8b51e76d044c03af9e579a66fcaa1a.png" width="320"/> |

**设备 B（userId：222）**

- 步骤1：输入用户名(请确保用户名唯一性，不能与其他用户重复)，比如222；
- 步骤2、点击“加入房间”，输入用户 A 创建的房间号（设备A第3步记录的房间号），加入房间；

| 步骤1 | 步骤2 |
|---------|---------|
|<img src="https://qcloudimg.tencent-cloud.cn/raw/0349a16cf0f442016d1262d602327a67.png" width="320"/>|<img src="https://qcloudimg.tencent-cloud.cn/raw/a5f86a91670b56ed39bb40d6d4ea0d24.png" width="320"/>|
## 常见问题

- [TUI 场景化解决方案常见问题](https://cloud.tencent.com/developer/article/1952880)
- 欢迎加入 QQ 群：592465424，进行技术交流和反馈~