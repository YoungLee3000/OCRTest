package com.nlscan.ocrtest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MailInfoAdapter  extends RecyclerView.Adapter<MailInfoAdapter.MyViewHolder> {



    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private List<MailInfo> mData;

    public MailInfoAdapter(Context context , List<MailInfo> list) {
        mLayoutInflater = LayoutInflater.from(context);
        mContext = context;
        this.mData = list;
    }

    /**
     * 初始化每个Item的布局文件
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public MailInfoAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MailInfoAdapter.MyViewHolder(mLayoutInflater.inflate(R.layout.mail_info_item, parent, false));
    }



    /**
     *
     * @param holder
     * @param position
     * 为Item的控件填充信息，并为其中的某些控件指定点击事件
     */
    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {

        MailInfo mailInfo = mData.get(position);

        holder.tvType.setText(mailInfo.getSendType());
        holder.tvWeight.setText(String.valueOf(mailInfo.getSendWeight()) + "kg");

        final String reHead = mailInfo.getReceiverName() + " " + mailInfo.getReceiverPhone();
        final String seHead = mailInfo.getSenderName() + " " + mailInfo.getSenderPhone();

        holder.tvReHead.setText(reHead);
        holder.tvSendHead.setText(seHead);

        holder.tvReContain.setText(mailInfo.getReceiverAddress());
        holder.tvSendContain.setText(mailInfo.getSenderAddress());



        if (mOnItemClickListener != null){



            holder.confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.confirm,pos);
                }
            });



            holder.tvReHead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.tvReHead,pos);
                }
            });

            holder.tvReContain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.tvReContain,pos);
                }
            });

            holder.tvSendHead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.tvSendHead,pos);
                }
            });

            holder.tvSendContain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.tvSendContain,pos);
                }
            });

        }


    }

    @Override
    public int getItemCount() {
        return mData.size();
    }



    class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView tvType,tvWeight,tvReHead,tvReContain,tvSendHead,tvSendContain,confirm;

        public MyViewHolder(View itemView) {
            super(itemView);

            tvType = (TextView) itemView.findViewById(R.id.tv_good_type);
            tvWeight = (TextView) itemView.findViewById(R.id.tv_good_weight);
            tvReHead = (TextView) itemView.findViewById(R.id.tv_receive_head);
            tvReContain = (TextView) itemView.findViewById(R.id.tv_receive_contain);
            tvSendHead = (TextView) itemView.findViewById(R.id.tv_send_head);
            tvSendContain = (TextView) itemView.findViewById(R.id.tv_send_contain);
            confirm = (TextView) itemView.findViewById(R.id.text_confirm);


        }
    }


    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private MailInfoAdapter.OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(MailInfoAdapter.OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

}
