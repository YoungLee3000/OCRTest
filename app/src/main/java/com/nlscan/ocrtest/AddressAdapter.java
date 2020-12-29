package com.nlscan.ocrtest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AddressAdapter  extends RecyclerView.Adapter<AddressAdapter.MyViewHolder> {



    private  LayoutInflater mLayoutInflater;
    private  Context mContext;
    private  List<Address> mData;

    public AddressAdapter(Context context , List<Address> list) {
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
    public AddressAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AddressAdapter.MyViewHolder(mLayoutInflater.inflate(R.layout.address_item, parent, false));
    }



    /**
     *
     * @param holder
     * @param position
     * 为Item的控件填充信息，并为其中的某些控件指定点击事件
     */
    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {

        Address address = mData.get(position);

        holder.label.setText(address.getName().substring(0,1));
        final String headStr = address.getName() + "  " + address.getPhoneNumber();
        holder.head.setText(headStr);
        String containStr = address.getProvince() + "  " + address.getCity() + "  " +
                            address.getCounty() + "  " + address.getStreet() + "\n" +
                            address.getDetail();
        holder.contain.setText(containStr);
        holder.confirm.setText("确认");

        if (mOnItemClickListener != null){

            holder.confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.confirm,pos);
                }
            });

            holder.contain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.contain,pos);
                }
            });

        }


    }

    @Override
    public int getItemCount() {
        return mData.size();
    }



    class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView label, head, contain, confirm;

        public MyViewHolder(View itemView) {
            super(itemView);
            label = (TextView) itemView.findViewById(R.id.text_label);
            head = (TextView) itemView.findViewById(R.id.text_address_head);
            contain = (TextView) itemView.findViewById(R.id.text_address_contain);
            confirm = (TextView) itemView.findViewById(R.id.text_confirm);
        }
    }


    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private AddressAdapter.OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(AddressAdapter.OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

}
