package com.inase.android.gocci.ui.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cocosw.bottomsheet.BottomSheet;
import com.inase.android.gocci.R;
import com.inase.android.gocci.consts.Const;
import com.inase.android.gocci.domain.model.PostData;
import com.inase.android.gocci.utils.Util;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by kinagafuji on 15/09/29.
 */
public class GridProfAdapter extends RecyclerView.Adapter<Const.TwoCellViewHolder> {

    private Context mContext;
    private int mCellSize;

    private ArrayList<PostData> mPostData = new ArrayList<>();

    private GridProfCallback mCallback;

    public GridProfAdapter(Context context, ArrayList<PostData> postData) {
        this.mContext = context;
        this.mPostData = postData;
        this.mCellSize = Util.getScreenWidth(mContext) / 2;
    }

    public void setMyProfCallback(GridProfCallback callback) {
        mCallback = callback;
    }

    public void setData() {
        this.notifyDataSetChanged();
    }

    public PostData getItem(int position) {
        return mPostData.get(position);
    }

    public boolean isEmpty() {
        return mPostData.isEmpty();
    }

    @Override
    public Const.TwoCellViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.cell_search_grid, parent, false);
        StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        layoutParams.height = mCellSize;
        layoutParams.width = mCellSize;
        layoutParams.setFullSpan(false);
        view.setLayoutParams(layoutParams);
        return new Const.TwoCellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final Const.TwoCellViewHolder holder, final int position) {
        final PostData user = mPostData.get(position);
        Picasso.with(mContext)
                .load(user.getThumbnail())
                .resize(mCellSize, mCellSize)
                .centerCrop()
                .into(holder.mSquareImage);

        holder.mOverlay.setMinimumHeight(mCellSize / 3);

        holder.mSquareImage.setVisibility(View.VISIBLE);

        holder.mRestname.setText(user.getRestname());
        holder.mDistance.setText(getDist(user.getDistance()));

        holder.mOtherAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new BottomSheet.Builder(mContext, R.style.BottomSheet_StyleDialog).sheet(R.menu.menu_cell).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.id.move_to_restpage:
                                mCallback.onGridRestClick(user.getPost_rest_id(), user.getRestname());
                                break;
                            case R.id.move_to_comment:
                                mCallback.onGridCommentClick(Integer.parseInt(user.getPost_id()));
                                break;
                            case R.id.violation:
                                Util.setViolateDialog(mContext, user.getPost_id());
                                break;
                            case R.id.close:
                                dialog.dismiss();
                        }
                    }
                }).show();
            }
        });

        holder.mAspectFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onGridVideoFrameClick(user);
            }
        });

        holder.mAspectFrame.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mCallback.onGridVideoFrameLongClick(user.getPost_id(), position);
                return false;
            }
        });

        mCallback.onGridHashHolder(holder, user.getPost_id());
    }

    @Override
    public int getItemCount() {
        return mPostData.size();
    }

    private String getDist(int distance) {
        String dist = null;
        if (distance > 1000) {
            dist = distance / 1000 + "km";
        } else {
            dist = distance + "m";
        }
        return dist;
    }

    public interface GridProfCallback {

        void onGridRestClick(int rest_id, String rest_name);

        void onGridCommentClick(int post_id);

        void onGridVideoFrameClick(PostData data);

        void onGridVideoFrameLongClick(String post_id, int position);

        void onGridHashHolder(Const.TwoCellViewHolder holder, String post_id);

    }
}