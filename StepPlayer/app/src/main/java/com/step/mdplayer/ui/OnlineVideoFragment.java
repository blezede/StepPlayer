package com.step.mdplayer.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.step.mdplayer.R;
import com.step.mdplayer.adapter.VideoViewAdapater;
import com.step.mdplayer.base.BaseFragment;
import com.step.mdplayer.mdplayer.CustomMediaPlayer;
import com.step.mdplayer.mdplayer.MDPlayerManager;
import com.step.mdplayer.model.VideoListBean;
import com.step.mdplayer.utils.DeviceUtils;
import com.step.mdplayer.widget.media.IjkVideoView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dawish on 2016/10/4.
 */

public class OnlineVideoFragment extends BaseFragment {

    private View rootView;
    private List<VideoListBean> dataList = new ArrayList<>();
    private RecyclerView videoRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private VideoViewAdapater videoViewAdapater;

    private RelativeLayout fullScreen;

    private int postion = -1;
    private int lastPostion = -1;

    private CustomMediaPlayer customMediaPlayer;

    /***
     * 全屏播放器放在MainActivity中
     */
    private static MainActivity mainActivity;

    public static OnlineVideoFragment newInstance(Context context) {
        OnlineVideoFragment fragment = new OnlineVideoFragment();
        mainActivity = (MainActivity)context;
        return fragment;
    }


    @Override
    protected View getContentView(LayoutInflater inflater, ViewGroup container) {
        rootView = inflater.inflate(R.layout.activity_scroll_videoview, container, false);
        return rootView;
    }
    RelativeLayout mLittleWindow;
    @Override
    protected void initViews(View contentView) {
        SwipeRefreshLayout swipeView = (SwipeRefreshLayout) rootView.findViewById(R.id.refreshLayout);
        swipeView.setEnabled(false);
        mLittleWindow = (RelativeLayout)rootView.findViewById(R.id.little_window);
        videoRecyclerView = (RecyclerView) rootView.findViewById(R.id.videoRecyclerView);
        mLayoutManager = new LinearLayoutManager(mainActivity);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        videoRecyclerView.setLayoutManager(mLayoutManager);
        /***
         *  全屏播放器放在MainActivity中
         */
        fullScreen = (RelativeLayout) mainActivity.findViewById(R.id.full_screen);

        /***
         * 初始化播放器
         */
        customMediaPlayer = MDPlayerManager.getMDManager().initialize(mainActivity);
        customMediaPlayer.setShowTopControl(false).setSupportGesture(false);
        customMediaPlayer.setScaleType(CustomMediaPlayer.SCALETYPE_FILLPARENT);
    }



    @Override
    protected void initListeners() {
        /**
         * 点击开始播放器
         */
        videoViewAdapater.setPlayClick(new VideoViewAdapater.onPlayClick() {
            @Override
            public void onPlayclick(final int position, final RelativeLayout image) {
                image.setVisibility(View.GONE);
                //重复点击当前视频，返回
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
                if (lastPostion != -1 && mLittleWindow.getChildCount() == 0) {
                    final FrameLayout fl = (FrameLayout) customMediaPlayer.getParent();
                    final View cov = customMediaPlayer.findView(R.id.adapter_player_control);
                    boolean b = customMediaPlayer.removeSelf();
                    if (fl != null && cov != null) {
                        cov.setVisibility(View.VISIBLE);
                        exitAnimator(fl, cov);
                    }
                }

                customMediaPlayer.removeSelf();
                View view = videoRecyclerView.findViewHolderForAdapterPosition(position).itemView;
                final FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.adapter_super_video);
                frameLayout.removeAllViews();
                //mdPlayer.showView(R.id.adapter_player_control);
                customMediaPlayer.setHideControl(false);
                frameLayout.addView(customMediaPlayer);
                customMediaPlayer.play(dataList.get(position).getVideoUrl());
                Toast.makeText(mainActivity, "position:" + position + ", lastPosition:" + lastPostion, Toast.LENGTH_SHORT).show();
                lastPostion = position;
                enterAnimator(frameLayout);

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
                Log.e("online", "onChildViewAttachedToWindow --> "+ index + ", position:" + postion);
                View controlview = view.findViewById(R.id.adapter_player_control);
                if (controlview == null) {
                    return;
                }
                view.findViewById(R.id.adapter_player_control).setVisibility(View.VISIBLE);
                if (index == postion) {
                    FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.adapter_super_video);
                    frameLayout.removeAllViews();
                    /*if (mdPlayer != null &&
                            ((mdPlayer.isPlaying()) || mdPlayer.getVideoStatus() == IjkVideoView.STATE_PAUSED)) {
                        view.findViewById(R.id.adapter_player_control).setVisibility(View.GONE);
                    }
                    if ((mdPlayer.isPlaying()) || mdPlayer.getVideoStatus() == IjkVideoView.STATE_PAUSED) {
                        if (mdPlayer.getParent() != null)
                            mdPlayer.setHideControl(false);
                            ((ViewGroup) mdPlayer.getParent()).removeAllViews();
                        frameLayout.addView(mdPlayer);
                        return;
                    }*/
                    view.findViewById(R.id.adapter_player_control).setVisibility(View.GONE);
                    if (customMediaPlayer.getParent() != null) {
                        customMediaPlayer.setHideControl(false);
                        ((ViewGroup) customMediaPlayer.getParent()).removeAllViews();
                        frameLayout.addView(customMediaPlayer);
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
                Log.e("online", "onChildViewDetachedFromWindow --> "+ index + ", position:" + postion);
                if ((index) == postion) {
                    if (true) {
                        if (customMediaPlayer != null) {
                            /*mdPlayer.stop();
                            mdPlayer.release();
                            mdPlayer.showView(R.id.adapter_player_control);*/
                            customMediaPlayer.showView(R.id.adapter_player_control);
                            customMediaPlayer.setHideControl(true);
                            mLittleWindow.setVisibility(View.VISIBLE);
                            mLittleWindow.removeAllViews();
                            mLittleWindow.addView(customMediaPlayer);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void initDatas() {
        setData();
        videoViewAdapater = new VideoViewAdapater(mainActivity);
        videoViewAdapater.setData(dataList);
        videoRecyclerView.setAdapter(videoViewAdapater);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e("online", "onConfigurationChanged -->" + newConfig.orientation);
        if (customMediaPlayer != null) {
            /**
             * 在activity中监听到横竖屏变化时调用播放器的监听方法来实现播放器大小切换
             */
            customMediaPlayer.onConfigurationChanged(newConfig);
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                /*showActionBar();
                fullScreen.setVisibility(View.GONE);
                fullScreen.removeAllViews();
                videoRecyclerView.setVisibility(View.VISIBLE);
                if (postion <= mLayoutManager.findLastVisibleItemPosition()
                        && postion >= mLayoutManager.findFirstVisibleItemPosition()) {
                    View view = videoRecyclerView.findViewHolderForAdapterPosition(postion).itemView;
                    FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.adapter_super_video);
                    frameLayout.removeAllViews();
                    ViewGroup last = (ViewGroup) mdPlayer.getParent();//找到videoitemview的父类，然后remove
                    if (last != null) {
                        last.removeAllViews();
                    }
                    frameLayout.addView(mdPlayer);
                }
                int mShowFlags =
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                fullScreen.setSystemUiVisibility(mShowFlags);*/
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
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 隐藏ActionBar
     */
    private void hideActionBar(){
        if(mainActivity.getSupportActionBar() != null)
            mainActivity.getSupportActionBar().hide();
    }

    /**
     * 显示ActionBar
     */
    private void showActionBar(){
        if(mainActivity.getSupportActionBar() != null)
            mainActivity.getSupportActionBar().show();
    }

    /**
     * 添加测试数据
     * @return
     */
    private List<VideoListBean> setData() {
        dataList.clear();
        VideoListBean bean0 = new VideoListBean();
        //bean0.setVideoUrl("http://sample.vodobox.net/skate_phantom_flex_4k/skate_phantom_flex_4k.m3u8");
        bean0.setVideoUrl("http://ht.cdn.turner.com/nba/big/channels/nba_tv/2016/04/02/20160402-bop-warriors-celtics.nba_nba_1280x720.mp4");
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
     * 暂停
     */
    public void pauseToPlay(){
        if (customMediaPlayer != null) {
            customMediaPlayer.onPause();
        }
    }

    /**
     * 下面的这几个Activity的生命状态很重要
     */
    @Override
    public void onPause() {
        super.onPause();
        if (customMediaPlayer != null) {
            customMediaPlayer.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (customMediaPlayer != null) {
            customMediaPlayer.onResume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (customMediaPlayer != null) {
            customMediaPlayer.onDestroy();
        }
    }

    public boolean onBackPressed() {
        if (customMediaPlayer != null && fullScreen.getVisibility() == View.VISIBLE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
            return true;
        }
        if (customMediaPlayer != null && customMediaPlayer.onBackPressed()) {
            return true;
        }
        if (postion != -1 && customMediaPlayer != null && customMediaPlayer.isPlaying()) {
            customMediaPlayer.stop();
            customMediaPlayer.release();
            customMediaPlayer.showView(R.id.adapter_player_control);
            return true;
        }
        return false;
    }

    private void exitAnimator(final View playerContainer, final View cover) {
        ValueAnimator animator = ObjectAnimator.ofFloat(100f, 0f);
        animator.setDuration(300);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float s = animation.getAnimatedFraction();
                Log.e("online", "last onAnimationUpdate --> " + s + "last = " + lastPostion);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerContainer.getLayoutParams();
                params.setMargins((int)(DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int)(DeviceUtils.dp2px(getActivity(), 4.5f) * s),
                        (int)(DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int)(DeviceUtils.dp2px(getActivity(), 4.5f) * s));
                playerContainer.setLayoutParams(params);
                RelativeLayout.LayoutParams params11 = (RelativeLayout.LayoutParams) cover.getLayoutParams();
                params11.setMargins((int)(DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int)(DeviceUtils.dp2px(getActivity(), 4.5f) * s),
                        (int)(DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int)(DeviceUtils.dp2px(getActivity(), 4.5f) * s));
                cover.setLayoutParams(params11);
            }
        });
        animator.start();
    }

    private void enterAnimator(final View playerContainer) {
        ValueAnimator animator = ObjectAnimator.ofFloat(100f, 0f);
        animator.setDuration(300);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float s = 1 - animation.getAnimatedFraction();
                Log.e("online", "onAnimationUpdate --> " + s);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerContainer.getLayoutParams();
                params.setMargins((int)(DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int)(DeviceUtils.dp2px(getActivity(), 4.5f) * s),
                        (int)(DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int)(DeviceUtils.dp2px(getActivity(), 4.5f) * s));
                playerContainer.setLayoutParams(params);
            }
        });
        animator.start();
    }
}
