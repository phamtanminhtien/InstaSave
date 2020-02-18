package com.tietha.instasave;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AdapterListMain extends RecyclerView.Adapter<AdapterListMain.ViewHolder> {

    ArrayList<Bitmap> mList;
    Context context;

    public AdapterListMain(ArrayList<Bitmap> mList, Context context) {
        this.mList = mList;
        this.context = context;
    }

    @NonNull
    @Override
    public AdapterListMain.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_list_main, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterListMain.ViewHolder holder, int position) {
        holder.setImageView(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private int checkType(String s) {
        String[] split = s.split("\\.");
        String type = split[split.length - 1];
        if (type.equals("jpg")) {
            return UrlMedia.TYPE_IMG;
        } else {
            return UrlMedia.TYPE_VIDEO;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }

        public void setImageView(Bitmap btm) {
                imageView.setImageBitmap(btm);
        }
    }
}
