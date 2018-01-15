package com.step.mdplayer.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.step.mdplayer.R;
import com.step.mdplayer.adapter.VideoViewAdapater;
import com.step.mdplayer.base.BaseActivity;
import com.step.mdplayer.mdplayer.CustomMediaPlayer;
import com.step.mdplayer.mdplayer.MDPlayerManager;
import com.step.mdplayer.model.VideoListBean;
import com.step.mdplayer.widget.media.IjkVideoView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dawish on 2016/9/24.
 */
public class RecyclerVideoViewActivity extends BaseActivity {

    private List<VideoListBean> dataList = new ArrayList<>();
    private RecyclerView videoRecyclerView;
    private RelativeLayout fullScreen;
    private LinearLayoutManager mLayoutManager;
    private VideoViewAdapater videoViewAdapater;

    private int postion = -1;
    private int lastPostion = -1;

    private CustomMediaPlayer customMediaPlayer;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_scroll_videoview;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {

        fullScreen = (RelativeLayout) findViewById(R.id.full_screen);
        videoRecyclerView = (RecyclerView) findViewById(R.id.videoRecyclerView);
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        videoRecyclerView.setLayoutManager(mLayoutManager);

        /***
         * 初始化播放器
         */
        customMediaPlayer = MDPlayerManager.getMDManager().initialize(this);
        customMediaPlayer.setShowTopControl(false).setSupportGesture(false);
    }

    @Override
    protected void initData() {
        setData();
        videoViewAdapater = new VideoViewAdapater(RecyclerVideoViewActivity.this);
        videoViewAdapater.setData(dataList);
        videoRecyclerView.setAdapter(videoViewAdapater);

    }

    @Override
    protected void initToolbar(Bundle savedInstanceState) {

    }

    @Override
    protected void initListeners() {
        /**
         * 点击开始播放器
         */
        videoViewAdapater.setPlayClick(new VideoViewAdapater.onPlayClick() {
            @Override
            public void onPlayclick(int position, RelativeLayout image) {
                image.setVisibility(View.GONE);
                if (customMediaPlayer.isPlaying() && lastPostion == position) {
                    return;
                }

                postion = position;
                if (customMediaPlayer.getVideoStatus() == IjkVideoView.STATE_PAUSED) {
                    if (position != lastPostion) {
                        customMediaPlayer.stopPlayVideo();
                        customMediaPlayer.release();
                    }
                }
                if (lastPostion != -1) {
                    customMediaPlayer.showView(R.id.adapter_player_control);
                }

                View view = videoRecyclerView.findViewHolderForAdapterPosition(position).itemView;
                FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.adapter_super_video);
                frameLayout.removeAllViews();
                customMediaPlayer.showView(R.id.adapter_player_control);
                frameLayout.addView(customMediaPlayer);
                customMediaPlayer.play(dataList.get(position).getVideoUrl());
                Toast.makeText(RecyclerVideoViewActivity.this, "position:" + position, Toast.LENGTH_SHORT).show();
                lastPostion = position;
            }
        });

        /**
         * 播放完设置还原播放界面
         */
        customMediaPlayer.onComplete(new Runnable() {
            @Override
            public void run() {
                ViewGroup last = (ViewGroup) customMediaPlayer.getParent();//找到videoitemview的父类，然后remove
                if (last != null && last.getChildCount() > 0) {
                    last.removeAllViews();
                    View itemView = (View) last.getParent();
                    if (itemView != null) {
                        itemView.findViewById(R.id.adapter_player_control).setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        /***
         * 监听列表的下拉滑动
         */
        videoRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {

            /**
             * 当recyclerView的item添加到屏幕是分情况处理视频播放器
             * @param view
             */
            @Override
            public void onChildViewAttachedToWindow(View view) {
                int index = videoRecyclerView.getChildAdapterPosition(view);
                View controlview = view.findViewById(R.id.adapter_player_control);
                if (controlview == null) {
                    return;
                }
                view.findViewById(R.id.adapter_player_control).setVisibility(View.VISIBLE);
                if (index == postion) {
                    FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.adapter_super_video);
                    frameLayout.removeAllViews();
                    if (customMediaPlayer != null &&
                            ((customMediaPlayer.isPlaying()) || customMediaPlayer.getVideoStatus() == IjkVideoView.STATE_PAUSED)) {
                        view.findViewById(R.id.adapter_player_control).setVisibility(View.GONE);
                    }
                    if (customMediaPlayer.getVideoStatus() == IjkVideoView.STATE_PAUSED) {
                        if (customMediaPlayer.getParent() != null)
                            ((ViewGroup) customMediaPlayer.getParent()).removeAllViews();
                        frameLayout.addView(customMediaPlayer);
                        return;
                    }
                }
            }

            /**
             * 当item离开屏幕时停止视频播放器并释放播放器，并显示播放器按钮
             * @param view
             */
            @Override
            public void onChildViewDetachedFromWindow(View view) {
                int index = videoRecyclerView.getChildAdapterPosition(view);
                if ((index) == postion) {
                    if (true) {
                        if (customMediaPlayer != null) {
                            customMediaPlayer.stop();
                            customMediaPlayer.release();
                            customMediaPlayer.showView(R.id.adapter_player_control);
                        }
                    }
                }
            }
        });

    }

    /**
     * 当大小屏切换时处理
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (customMediaPlayer != null) {
            /**
             * 在activity中监听到横竖屏变化时调用播放器的监听方法来实现播放器大小切换
             */
            customMediaPlayer.onConfigurationChanged(newConfig);
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                showActionBar();
                fullScreen.setVisibility(View.GONE);
                fullScreen.removeAllViews();
                videoRecyclerView.setVisibility(View.VISIBLE);
                if (postion <= mLayoutManager.findLastVisibleItemPosition()
                        && postion >= mLayoutManager.findFirstVisibleItemPosition()) {
                    View view = videoRecyclerView.findViewHolderForAdapterPosition(postion).itemView;
                    FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.adapter_super_video);
                    frameLayout.removeAllViews();
                    ViewGroup last = (ViewGroup) customMediaPlayer.getParent();//找到videoitemview的父类，然后remove
                    if (last != null) {
                        last.removeAllViews();
                    }
                    frameLayout.addView(customMediaPlayer);
                }
                int mShowFlags =
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                fullScreen.setSystemUiVisibility(mShowFlags);
            } else {
                ViewGroup viewGroup = (ViewGroup) customMediaPlayer.getParent();
                if (viewGroup == null)
                    return;
                hideActionBar();
                viewGroup.removeAllViews();
                fullScreen.addView(customMediaPlayer);
                fullScreen.setVisibility(View.VISIBLE);
                int mHideFlags =
                        View.SYSTEM_UI_FLAG_LOW_PROFILE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        ;
                fullScreen.setSystemUiVisibility(mHideFlags);
            }
        } else {
            fullScreen.setVisibility(View.GONE);
        }
    }

    /**
     * 添加测试数据
     * @return
     */
    private List<VideoListBean> setData() {
        dataList.clear();
        VideoListBean bean0 = new VideoListBean();
        bean0.setVideoUrl("http://uc-baobab.wdjcdn.com/1471337537665_b596ac9c.mp4?t=1475424855&k=8d74c859203ccd57");
        dataList.add(bean0);
        VideoListBean bean00 = new VideoListBean();
        bean00.setVideoUrl("http://ips.ifeng.com/video19.ifeng.com/video09/2014/06/16/1989823-102-086-0009.mp4");
        dataList.add(bean00);
        VideoListBean bean1 = new VideoListBean();
        bean1.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9502&editionType=normal");
        dataList.add(bean1);
        VideoListBean bean2 = new VideoListBean();
        bean2.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9508&editionType=normal");
        dataList.add(bean2);
        VideoListBean bean3 = new VideoListBean();
        bean3.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=8438&editionType=normal");
        dataList.add(bean3);
        VideoListBean bean4 = new VideoListBean();
        bean4.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=8340&editionType=normal");
        dataList.add(bean4);
        VideoListBean bean5 = new VideoListBean();
        bean5.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9392&editionType=normal");
        dataList.add(bean5);
        VideoListBean bean6 = new VideoListBean();
        bean6.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=7524&editionType=normal");
        dataList.add(bean6);
        VideoListBean bean7 = new VideoListBean();
        bean7.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9444&editionType=normal");
        dataList.add(bean7);
        VideoListBean bean8 = new VideoListBean();
        bean8.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9442&editionType=normal");
        dataList.add(bean8);
        VideoListBean bean9 = new VideoListBean();
        bean9.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=8530&editionType=normal");
        dataList.add(bean9);
        VideoListBean bean10 = new VideoListBean();
        bean10.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9418&editionType=normal");
        dataList.add(bean10);
        return dataList;
    }

    /**
     * 下面的这几个Activity的生命状态很重要
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (customMediaPlayer != null) {
            customMediaPlayer.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (customMediaPlayer != null) {
            customMediaPlayer.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (customMediaPlayer != null) {
            customMediaPlayer.onDestroy();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (customMediaPlayer != null && customMediaPlayer.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    /**
     * 隐藏ActionBar
     */
    private void hideActionBar(){
        getSupportActionBar().hide();
    }

    /**
     * 显示ActionBar
     */
    private void showActionBar(){
        getSupportActionBar().show();
    }

}
