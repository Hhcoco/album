package com.yanzhenjie.album.app.album;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumFolder;
import com.yanzhenjie.album.R;
import com.yanzhenjie.album.api.widget.Widget;
import com.yanzhenjie.album.app.Contract;
import com.yanzhenjie.album.impl.DoubleClickWrapper;
import com.yanzhenjie.album.impl.OnCheckedClickListener;
import com.yanzhenjie.album.impl.OnItemClickListener;
import com.yanzhenjie.album.util.AlbumUtils;
import com.yanzhenjie.album.util.SystemBar;
import com.yanzhenjie.album.widget.ColorProgressBar;
import com.yanzhenjie.album.widget.divider.Api21ItemDivider;

/**
 * <p>作者：hsicen  2019/11/6 17:07
 * <p>邮箱：codinghuang@163.com
 * <p>功能：
 * <p>描述：媒体文件浏览控件
 */
class AlbumView extends Contract.AlbumView implements View.OnClickListener {

    private Activity mActivity;
    private Toolbar mToolbar;

    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private AlbumAdapter mAdapter;

    private Button mBtnPreview;
    private Button mBtnSwitchFolder;

    private LinearLayout mLayoutLoading;
    private ColorProgressBar mProgressBar;

    //底部导航栏
    private RelativeLayout mBottomNav;
    private MenuItem mCompleteMenu;
    private int mMode;

    public AlbumView(Activity activity, Contract.AlbumPresenter presenter, int chooseMode) {
        super(activity, presenter);
        this.mActivity = activity;
        this.mMode = chooseMode;

        this.mToolbar = activity.findViewById(R.id.toolbar);
        this.mRecyclerView = activity.findViewById(R.id.recycler_view);
        this.mBottomNav = activity.findViewById(R.id.rl_bottom_nav);

        this.mBtnSwitchFolder = activity.findViewById(R.id.btn_switch_dir);
        this.mBtnPreview = activity.findViewById(R.id.btn_preview);

        this.mLayoutLoading = activity.findViewById(R.id.layout_loading);
        this.mProgressBar = activity.findViewById(R.id.progress_bar);

        this.mToolbar.setOnClickListener(new DoubleClickWrapper(this));
        this.mBtnSwitchFolder.setOnClickListener(this);
        this.mBtnPreview.setOnClickListener(this);
    }

    @Override
    protected void onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.album_menu_album, menu);
        mCompleteMenu = menu.findItem(R.id.album_menu_finish);
    }

    @Override
    protected void onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.album_menu_finish) {
            getPresenter().complete();
        }
    }

    @Override
    public void setupViews(Widget widget, int column, boolean hasCamera, int choiceMode) {
        SystemBar.setNavigationBarColor(mActivity, widget.getNavigationBarColor());

        if (mMode == Album.FUNCTION_CHOICE_VIDEO) {
            mBottomNav.setVisibility(View.GONE);
            mCompleteMenu.setVisible(false);
        } else {
            mBottomNav.setVisibility(View.VISIBLE);
            mCompleteMenu.setVisible(true);
        }

        int statusBarColor = widget.getStatusBarColor();
        if (widget.getUiStyle() == Widget.STYLE_LIGHT) {
            if (SystemBar.setStatusBarDarkFont(mActivity, true)) {
                SystemBar.setStatusBarColor(mActivity, statusBarColor);
            } else {
                SystemBar.setStatusBarColor(mActivity, getColor(R.color.albumColorPrimaryBlack));
            }

            mProgressBar.setColorFilter(getColor(R.color.albumLoadingDark));

            Drawable navigationIcon = getDrawable(R.drawable.album_ic_back_white);
            AlbumUtils.setDrawableTint(navigationIcon, getColor(R.color.albumIconDark));
            setHomeAsUpIndicator(navigationIcon);

            Drawable completeIcon = mCompleteMenu.getIcon();
            AlbumUtils.setDrawableTint(completeIcon, getColor(R.color.albumIconDark));
            mCompleteMenu.setIcon(completeIcon);
        } else {
            mProgressBar.setColorFilter(widget.getToolBarColor());
            SystemBar.setStatusBarColor(mActivity, statusBarColor);
            setHomeAsUpIndicator(R.drawable.album_ic_back_white);
        }
        mToolbar.setBackgroundColor(widget.getToolBarColor());

        Configuration config = mActivity.getResources().getConfiguration();
        mLayoutManager = new GridLayoutManager(getContext(), column, getOrientation(config), false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        int dividerSize = getResources().getDimensionPixelSize(R.dimen.album_dp_3);
        mRecyclerView.addItemDecoration(new Api21ItemDivider(Color.TRANSPARENT, dividerSize, dividerSize));
        mAdapter = new AlbumAdapter(getContext(), hasCamera, choiceMode, mMode, widget.getMediaItemCheckSelector());

        //拍照，视频录制点击监听
        mAdapter.setAddClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                getPresenter().clickCamera(view);
            }
        });

        //选择框点击监听
        mAdapter.setCheckedClickListener(new OnCheckedClickListener() {
            @Override
            public void onCheckedClick(CompoundButton button, int position) {
                //button.setChecked(!button.isChecked(), true);
                getPresenter().tryCheckItem(button, position);
            }
        });

        //资源点击监听
        mAdapter.setItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                getPresenter().tryPreviewItem(position);
            }
        });

        //绑定数据
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void setLoadingDisplay(boolean display) {
        mLayoutLoading.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        int position = mLayoutManager.findFirstVisibleItemPosition();
        mLayoutManager.setOrientation(getOrientation(newConfig));
        mRecyclerView.setAdapter(mAdapter);
        mLayoutManager.scrollToPosition(position);
    }

    @RecyclerView.Orientation
    private int getOrientation(Configuration config) {
        switch (config.orientation) {
            case Configuration.ORIENTATION_PORTRAIT: {
                return LinearLayoutManager.VERTICAL;
            }
            case Configuration.ORIENTATION_LANDSCAPE: {
                return LinearLayoutManager.HORIZONTAL;
            }
            default: {
                throw new AssertionError("This should not be the case.");
            }
        }
    }

    @Override
    public void setCompleteDisplay(boolean display) {
        mCompleteMenu.setVisible(display);
    }

    @Override
    public void bindAlbumFolder(AlbumFolder albumFolder) {
        mBtnSwitchFolder.setText(albumFolder.getName());

        mAdapter.setAlbumFiles(albumFolder.getAlbumFiles());
        mAdapter.notifyDataSetChanged();
        mRecyclerView.scrollToPosition(0);
    }

    @Override
    public void notifyInsertItem(int position) {
        mAdapter.notifyItemInserted(position);
    }

    @Override
    public void notifyItem(int position) {
        mAdapter.notifyItemChanged(position);
    }

    @Override
    public void setCheckedCount(int count) {
        mBtnPreview.setText(" (" + count + ")");
    }

    @Override
    public void onClick(View v) {
        if (v == mToolbar) {
            mRecyclerView.smoothScrollToPosition(0);
        } else if (v == mBtnSwitchFolder) {
            getPresenter().clickFolderSwitch();
        } else if (v == mBtnPreview) {
            getPresenter().tryPreviewChecked();
        }
    }

    @Override
    public void setSubTitle(String title) {
        if (mMode == Album.FUNCTION_CHOICE_VIDEO) {
            return;
        }

        super.setSubTitle(title);
    }
}