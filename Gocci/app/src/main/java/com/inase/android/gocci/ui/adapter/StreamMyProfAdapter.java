package com.inase.android.gocci.ui.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.andexert.library.RippleView;
import com.cocosw.bottomsheet.BottomSheet;
import com.inase.android.gocci.Application_Gocci;
import com.inase.android.gocci.R;
import com.inase.android.gocci.consts.Const;
import com.inase.android.gocci.domain.model.PostData;
import com.inase.android.gocci.ui.view.SquareImageView;
import com.inase.android.gocci.utils.Util;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by kinagafuji on 15/10/21.
 */
public class StreamMyProfAdapter extends RecyclerView.Adapter<Const.StreamViewHolder> {

    private Context mContext;

    private ArrayList<PostData> mPostData = new ArrayList<>();

    private MyStreamProfCallback mCallback;

    public StreamMyProfAdapter(Context context, ArrayList<PostData> postData) {
        this.mContext = context;
        this.mPostData = postData;
    }

    public void setMyProfCallback(MyStreamProfCallback callback) {
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
    public Const.StreamViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.cell_stream_list, parent, false);
        return new Const.StreamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final Const.StreamViewHolder holder, final int position) {
        final PostData user = mPostData.get(position);
        holder.mName.setText(user.getRestname());

        holder.mTimeText.setText(user.getPost_date());

        if (!user.getMemo().equals("none")) {
            holder.mComment.setText(user.getMemo());
        } else {
            holder.mComment.setText("");
        }

        holder.mName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onStreamRestClick(user.getPost_rest_id(), user.getRestname());
            }
        });

        holder.mCircleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onStreamRestClick(user.getPost_rest_id(), user.getRestname());
            }
        });

        holder.mMenuRipple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new BottomSheet.Builder(mContext, R.style.BottomSheet_StyleDialog).sheet(R.menu.popup_mypage).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.id.delete:
                                // Util.setViolateDialog(mContext, user.getPost_id());
                                break;
                            case R.id.close:
                                dialog.dismiss();
                        }
                    }
                }).show();
            }
        });
        Picasso.with(mContext)
                .load(user.getThumbnail())
                .placeholder(R.color.videobackground)
                .into(holder.mVideoThumbnail);
        holder.mVideoThumbnail.setVisibility(View.VISIBLE);

        holder.mVideoFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onStreamVideoFrameClick(user);
            }
        });

        if (!user.getCategory().equals(mContext.getString(R.string.nothing_tag))) {
            holder.mCategory.setText(user.getCategory());
        } else {
            holder.mCategory.setText("　　　　");
        }
        if (!user.getTag().equals(mContext.getString(R.string.nothing_tag))) {
            holder.mMood.setText(user.getTag());
        } else {
            holder.mMood.setText("　　　　");
        }
        if (!user.getValue().equals("0")) {
            holder.mValue.setText(user.getValue() + "円");
        } else {
            holder.mValue.setText("　　　　");
        }

        final int currentgoodnum = user.getGochi_num();
        final int currentcommentnum = user.getComment_num();

        holder.mLikesNumber.setText(String.valueOf(currentgoodnum));
        holder.mCommentsNumber.setText(String.valueOf(currentcommentnum));

        if (user.getGochi_flag() == 0) {
            holder.mLikesRipple.setClickable(true);
            holder.mLikesImage.setImageResource(R.drawable.ic_icon_beef);

            holder.mLikesRipple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    user.setGochi_flag(1);
                    user.setGochi_num(currentgoodnum + 1);

                    holder.mLikesNumber.setText(String.valueOf((currentgoodnum + 1)));
                    holder.mLikesImage.setImageResource(R.drawable.ic_icon_beef_orange);
                    holder.mLikesRipple.setClickable(false);

                    Util.postGochiAsync(mContext, user);
                }
            });
        } else {
            holder.mLikesImage.setImageResource(R.drawable.ic_icon_beef_orange);
            holder.mLikesRipple.setClickable(false);
        }

        holder.mCommentsRipple.setOnRippleCompleteListener(new RippleView.OnRippleCompleteListener() {
            @Override
            public void onComplete(RippleView rippleView) {
                mCallback.onStreamCommentClick(Integer.parseInt(user.getPost_id()));
            }
        });

        holder.mShareRipple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Application_Gocci.getShareTransfer() != null) {
                    new BottomSheet.Builder(mContext, R.style.BottomSheet_StyleDialog).sheet(R.menu.menu_share).listener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case R.id.facebook_share:
                                    Toast.makeText(mContext, mContext.getString(R.string.preparing_share), Toast.LENGTH_LONG).show();
                                    mCallback.onFacebookShare(user.getShare());
                                    break;
                                case R.id.twitter_share:
                                    mCallback.onTwitterShare(holder.mVideoThumbnail, user.getRestname());
                                    break;
                                case R.id.other_share:
                                    Toast.makeText(mContext, mContext.getString(R.string.preparing_share), Toast.LENGTH_LONG).show();
                                    mCallback.onInstaShare(user.getShare(), user.getRestname());
                                    break;
                                case R.id.close:
                                    dialog.dismiss();
                            }
                        }
                    }).show();
                } else {
                    Toast.makeText(mContext, mContext.getString(R.string.preparing_share_error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mCallback.onStreamHashHolder(holder, user.getPost_id());
    }

    @Override
    public int getItemCount() {
        return mPostData.size();
    }

    public interface MyStreamProfCallback {

        void onStreamRestClick(int rest_id, String rest_name);

        void onStreamCommentClick(int post_id);

        void onStreamVideoFrameClick(PostData data);

        void onFacebookShare(String share);

        void onTwitterShare(SquareImageView view, String rest_name);

        void onInstaShare(String share, String rest_name);

        void onStreamHashHolder(Const.StreamViewHolder holder, String post_id);

    }
}