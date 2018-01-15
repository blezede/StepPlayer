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
import com.step.mdplayer.adapter.VideoViewAdapter;
import com.step.mdplayer.base.BaseFragment;
import com.step.mdplayer.mdplayer.CustomMediaPlayer;
import com.step.mdplayer.mdplayer.MDPlayerManager;
import com.step.mdplayer.model.VideoListBean;
import com.step.mdplayer.utils.DeviceUtils;
import com.step.mdplayer.widget.media.IjkVideoView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gaoq on 2018/01/15.仿
 */

public class OnlineVideoFragment extends BaseFragment {

    private View mRootView;
    private List<VideoListBean> mDataList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private VideoViewAdapter mVideoViewAdapter;

    private RelativeLayout mFullScreenLayout; //全屏幕视频
    private RelativeLayout mLittleWindow; //小窗口

    private int mCurrPlayPosition = -1;
    private int mLastPlayPosition = -1;

    private CustomMediaPlayer mCustomMediaPlayer;

    /***
     * 全屏播放器放在MainActivity中
     */
    private static MainActivity mMainActivity;

    public static OnlineVideoFragment newInstance(Context context) {
        OnlineVideoFragment fragment = new OnlineVideoFragment();
        mMainActivity = (MainActivity) context;
        return fragment;
    }


    @Override
    protected View getContentView(LayoutInflater inflater, ViewGroup container) {
        mRootView = inflater.inflate(R.layout.activity_scroll_videoview, container, false);
        return mRootView;
    }

    @Override
    protected void initViews(View contentView) {
        SwipeRefreshLayout swipeView = (SwipeRefreshLayout) mRootView.findViewById(R.id.refreshLayout);
        swipeView.setEnabled(false);
        mLittleWindow = (RelativeLayout) mRootView.findViewById(R.id.little_window);
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.videoRecyclerView);
        mLayoutManager = new LinearLayoutManager(mMainActivity);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        /***
         *  全屏播放器放在MainActivity中
         */
        mFullScreenLayout = (RelativeLayout) mMainActivity.findViewById(R.id.full_screen);

        /***
         * 初始化播放器
         */
        mCustomMediaPlayer = MDPlayerManager.getMDManager().initialize(mMainActivity);
        mCustomMediaPlayer.setShowTopControl(false).setSupportGesture(false);
        mCustomMediaPlayer.setScaleType(CustomMediaPlayer.SCALETYPE_FILLPARENT);//自适应
    }


    @Override
    protected void initListeners() {
        /**
         * 点击开始播放器
         */
        mVideoViewAdapter.setPlayClick(new VideoViewAdapter.onPlayClick() {
            @Override
            public void onPlayClicked(final int position, final RelativeLayout image) {
                image.setVisibility(View.GONE);
                //重复点击当前视频，返回
                if (mCustomMediaPlayer.isPlaying() && mLastPlayPosition == position) {
                    return;
                }

                mCurrPlayPosition = position;
                if (mCustomMediaPlayer.getVideoStatus() == IjkVideoView.STATE_PAUSED) {
                    if (position != mLastPlayPosition) {
                        mCustomMediaPlayer.stopPlayVideo();
                        mCustomMediaPlayer.release();
                    }
                }
                //上次播放位置有效且非小窗模式,将上一播放位置player移除,恢复初始状态
                if (mLastPlayPosition != -1 && mLittleWindow.getChildCount() == 0) {
                    final FrameLayout fl = (FrameLayout) mCustomMediaPlayer.getParent();
                    final View cov = mCustomMediaPlayer.findView(R.id.adapter_player_control);
                    boolean b = mCustomMediaPlayer.removeSelf();
                    if (fl != null && cov != null) {
                        cov.setVisibility(View.VISIBLE);
                        exitAnimator(fl, cov);
                    }
                }
                //小窗模式或其他情况,先移除player
                mCustomMediaPlayer.removeSelf();
                View view = mRecyclerView.findViewHolderForAdapterPosition(position).itemView;
                FrameLayout videoContainer = (FrameLayout) view.findViewById(R.id.adapter_super_video);
                videoContainer.removeAllViews();
                //mdPlayer.showView(R.id.adapter_player_control);
                mCustomMediaPlayer.setHideControl(false);
                videoContainer.addView(mCustomMediaPlayer);
                mCustomMediaPlayer.play(mDataList.get(position).getVideoUrl());
                //Toast.makeText(mMainActivity, "position:" + position + ", lastPosition:" + mLastPlayPosition, Toast.LENGTH_SHORT).show();
                mLastPlayPosition = position;
                enterAnimator(videoContainer);
            }
        });

        /**
         * 播放完设置还原播放界面
         */
        mCustomMediaPlayer.onComplete(new Runnable() {
            @Override
            public void run() {
                ViewGroup last = (ViewGroup) mCustomMediaPlayer.getParent();//找到videoitemview的父类，然后remove
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
        mRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {

            /**
             * 当recyclerView的item添加到屏幕是分情况处理视频播放器
             * @param view
             */
            @Override
            public void onChildViewAttachedToWindow(View view) {
                int index = mRecyclerView.getChildAdapterPosition(view);
                Log.e("online", "onChildViewAttachedToWindow --> " + index + ", position:" + mCurrPlayPosition);
                View controlView = view.findViewById(R.id.adapter_player_control);
                if (controlView == null) {
                    return;
                }
                view.findViewById(R.id.adapter_player_control).setVisibility(View.VISIBLE);
                if (index == mCurrPlayPosition) {
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
                    if (mCustomMediaPlayer.getParent() != null) {
                        mCustomMediaPlayer.setHideControl(false);
                        ((ViewGroup) mCustomMediaPlayer.getParent()).removeAllViews();
                        frameLayout.addView(mCustomMediaPlayer);
                    }
                }
            }

            /**
             * 当item离开屏幕时停止视频播放器并释放播放器，并显示播放器按钮
             * @param view
             */
            @Override
            public void onChildViewDetachedFromWindow(View view) {
                int index = mRecyclerView.getChildAdapterPosition(view);
                Log.e("online", "onChildViewDetachedFromWindow --> " + index + ", position:" + mCurrPlayPosition);
                if ((index) == mCurrPlayPosition) {
                    if (true) {
                        if (mCustomMediaPlayer != null) {
                            /*mdPlayer.stop();
                            mdPlayer.release();
                            mdPlayer.showView(R.id.adapter_player_control);*/
                            mCustomMediaPlayer.showView(R.id.adapter_player_control);
                            mCustomMediaPlayer.setHideControl(true);
                            mLittleWindow.setVisibility(View.VISIBLE);
                            mLittleWindow.removeAllViews();
                            mLittleWindow.addView(mCustomMediaPlayer);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void initDatas() {
        setData();
        mVideoViewAdapter = new VideoViewAdapter(mMainActivity);
        mVideoViewAdapter.setData(mDataList);
        mRecyclerView.setAdapter(mVideoViewAdapter);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e("online", "onConfigurationChanged -->" + newConfig.orientation);
        if (mCustomMediaPlayer != null) {
            /**
             * 在activity中监听到横竖屏变化时调用播放器的监听方法来实现播放器大小切换
             */
            mCustomMediaPlayer.onConfigurationChanged(newConfig);
            //屏蔽了点击全屏后,再次竖屏返回到列表,无限全屏的操作
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                /*showActionBar();
                mFullScreenLayout.setVisibility(View.GONE);
                mFullScreenLayout.removeAllViews();
                mRecyclerView.setVisibility(View.VISIBLE);
                if (mCurrPlayPosition <= mLayoutManager.findLastVisibleItemPosition()
                        && mCurrPlayPosition >= mLayoutManager.findFirstVisibleItemPosition()) {
                    View view = mRecyclerView.findViewHolderForAdapterPosition(mCurrPlayPosition).itemView;
                    FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.adapter_super_video);
                    frameLayout.removeAllViews();
                    ViewGroup last = (ViewGroup) mdPlayer.getParent();//找到videoitemview的父类，然后remove
                    if (last != null) {
                        last.removeAllViews();
                    }
                    frameLayout.addView(mdPlayer);
                }
                int mShowFlags = View.SYSTEM_UI_FLAG_LAYOUT_mFullScreenLayout
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                mFullScreenLayout.setSystemUiVisibility(mShowFlags);*/
            } else {
                ViewGroup viewGroup = (ViewGroup) mCustomMediaPlayer.getParent();
                if (viewGroup == null)
                    return;
                hideActionBar();
                viewGroup.removeAllViews();
                mFullScreenLayout.addView(mCustomMediaPlayer);
                mFullScreenLayout.setVisibility(View.VISIBLE);
                int mHideFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                mFullScreenLayout.setSystemUiVisibility(mHideFlags);
            }
        } else {
            mFullScreenLayout.setVisibility(View.GONE);
        }
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 隐藏ActionBar
     */
    private void hideActionBar() {
        if (mMainActivity.getSupportActionBar() != null)
            mMainActivity.getSupportActionBar().hide();
    }

    /**
     * 显示ActionBar
     */
    private void showActionBar() {
        if (mMainActivity.getSupportActionBar() != null)
            mMainActivity.getSupportActionBar().show();
    }

    /**
     * 添加测试数据
     *
     * @return
     */
    private List<VideoListBean> setData() {
        mDataList.clear();
        VideoListBean bean0 = new VideoListBean();
        //bean0.setVideoUrl("http://sample.vodobox.net/skate_phantom_flex_4k/skate_phantom_flex_4k.m3u8");
        bean0.setVideoUrl("http://ht.cdn.turner.com/nba/big/channels/nba_tv/2016/04/02/20160402-bop-warriors-celtics.nba_nba_1280x720.mp4");
        mDataList.add(bean0);
        VideoListBean bean00 = new VideoListBean();
        bean00.setVideoUrl("http://ips.ifeng.com/video19.ifeng.com/video09/2014/06/16/1989823-102-086-0009.mp4");
        mDataList.add(bean00);
        VideoListBean bean1 = new VideoListBean();
        bean1.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9502&editionType=normal");
        mDataList.add(bean1);
        VideoListBean bean2 = new VideoListBean();
        bean2.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9508&editionType=normal");
        mDataList.add(bean2);
        VideoListBean bean3 = new VideoListBean();
        bean3.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=8438&editionType=normal");
        mDataList.add(bean3);
        VideoListBean bean4 = new VideoListBean();
        bean4.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=8340&editionType=normal");
        mDataList.add(bean4);
        VideoListBean bean5 = new VideoListBean();
        bean5.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9392&editionType=normal");
        mDataList.add(bean5);
        VideoListBean bean6 = new VideoListBean();
        bean6.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=7524&editionType=normal");
        mDataList.add(bean6);
        VideoListBean bean7 = new VideoListBean();
        bean7.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9444&editionType=normal");
        mDataList.add(bean7);
        VideoListBean bean8 = new VideoListBean();
        bean8.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9442&editionType=normal");
        mDataList.add(bean8);
        VideoListBean bean9 = new VideoListBean();
        bean9.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=8530&editionType=normal");
        mDataList.add(bean9);
        VideoListBean bean10 = new VideoListBean();
        bean10.setVideoUrl("http://baobab.wandoujia.com/api/v1/playUrl?vid=9418&editionType=normal");
        mDataList.add(bean10);
        return mDataList;
    }

    /**
     * 暂停
     */
    public void pauseToPlay() {
        if (mCustomMediaPlayer != null) {
            mCustomMediaPlayer.onPause();
        }
    }

    /**
     * 下面的这几个Activity的生命状态很重要
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mCustomMediaPlayer != null) {
            mCustomMediaPlayer.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCustomMediaPlayer != null) {
            mCustomMediaPlayer.onResume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCustomMediaPlayer != null) {
            mCustomMediaPlayer.onDestroy();
        }
    }

    public boolean onBackPressed() {
        //退出全屏模式
        if (mCustomMediaPlayer != null && mFullScreenLayout.getVisibility() == View.VISIBLE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            showActionBar();
            mFullScreenLayout.setVisibility(View.GONE);
            mFullScreenLayout.removeAllViews();
            mRecyclerView.setVisibility(View.VISIBLE);
            if (mCurrPlayPosition <= mLayoutManager.findLastVisibleItemPosition()
                    && mCurrPlayPosition >= mLayoutManager.findFirstVisibleItemPosition()) {
                View view = mRecyclerView.findViewHolderForAdapterPosition(mCurrPlayPosition).itemView;
                FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.adapter_super_video);
                frameLayout.removeAllViews();
                ViewGroup last = (ViewGroup) mCustomMediaPlayer.getParent();//找到videoitemview的父类，然后remove
                if (last != null) {
                    last.removeAllViews();
                }
                frameLayout.addView(mCustomMediaPlayer);
            }
            int mShowFlags =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            mFullScreenLayout.setSystemUiVisibility(mShowFlags);
            return true;
        }
        //如果有播放视频,停止播放
        if (mCurrPlayPosition != -1 && mCustomMediaPlayer != null && mCustomMediaPlayer.isPlaying()) {
            mCustomMediaPlayer.showView(R.id.adapter_player_control);
            mCustomMediaPlayer.stop();
            mCurrPlayPosition = -1;
            return true;
        }
        //托管事件
        if (mCustomMediaPlayer != null && mCustomMediaPlayer.onBackPressed()) {
            return true;
        }
        return false;
    }

    //停止播放的动画
    private void exitAnimator(final View playerContainer, final View cover) {
        ValueAnimator animator = ObjectAnimator.ofFloat(100f, 0f);
        animator.setDuration(300);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float s = animation.getAnimatedFraction();
                Log.e("online", "last onAnimationUpdate --> " + s + "last = " + mLastPlayPosition);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerContainer.getLayoutParams();
                params.setMargins((int) (DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int) (DeviceUtils.dp2px(getActivity(), 4.5f) * s),
                        (int) (DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int) (DeviceUtils.dp2px(getActivity(), 4.5f) * s));
                playerContainer.setLayoutParams(params);
                RelativeLayout.LayoutParams params11 = (RelativeLayout.LayoutParams) cover.getLayoutParams();
                params11.setMargins((int) (DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int) (DeviceUtils.dp2px(getActivity(), 4.5f) * s),
                        (int) (DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int) (DeviceUtils.dp2px(getActivity(), 4.5f) * s));
                cover.setLayoutParams(params11);
            }
        });
        animator.start();
    }

    //播放视频的动画
    private void enterAnimator(final View playerContainer) {
        ValueAnimator animator = ObjectAnimator.ofFloat(100f, 0f);
        animator.setDuration(300);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float s = 1 - animation.getAnimatedFraction();
                Log.e("online", "onAnimationUpdate --> " + s);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerContainer.getLayoutParams();
                params.setMargins((int) (DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int) (DeviceUtils.dp2px(getActivity(), 4.5f) * s),
                        (int) (DeviceUtils.dp2px(getActivity(), 8f) * s),
                        (int) (DeviceUtils.dp2px(getActivity(), 4.5f) * s));
                playerContainer.setLayoutParams(params);
            }
        });
        animator.start();
    }
}
