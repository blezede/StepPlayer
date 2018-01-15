package com.step.mdplayer.mdplayer;

import android.content.Context;

/**
 *
 * 类描述：获取唯一的视频控制器
 *
 */
public class MDPlayerManager {
    public static MDPlayerManager videoPlayViewManage;
    private CustomMediaPlayer videoPlayView;

    private MDPlayerManager() {

    }

    public static MDPlayerManager getMDManager() {
        if (videoPlayViewManage == null) {
            videoPlayViewManage = new MDPlayerManager();
        }
        return videoPlayViewManage;
    }

    public CustomMediaPlayer initialize(Context context) {
        if (videoPlayView == null) {
            videoPlayView = new CustomMediaPlayer(context);
        }
        return videoPlayView;
    }
}
