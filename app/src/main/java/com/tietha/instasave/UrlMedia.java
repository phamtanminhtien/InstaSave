package com.tietha.instasave;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UrlMedia {
    private String mURL;
    private String mURLImage;
    private Context mainCTX;
    private int type;
    private int count;
    final static int TYPE_IMG = 0;
    final static int TYPE_VIDEO = 1;
    private ArrayList<String> mList;
    private JSONObject jsonObject;

    UrlMedia(Context mainCTX, String mURL) {
        this.mURL = mURL;
        this.mainCTX = mainCTX;
        mList = new ArrayList<>();

        RequestQueue queue = Volley.newRequestQueue(mainCTX);
        String url = mURL.split("\\?")[0]+"?__a=1";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        count = 1;
                        jsonObject = null;
                        UrlMedia.this.mURLImage = null;
                        try {
                            jsonObject = new JSONObject(response);
                            Boolean video = jsonObject.getJSONObject("graphql").getJSONObject("shortcode_media").getBoolean("is_video");
                            if(!jsonObject.getJSONObject("graphql").getJSONObject("shortcode_media").isNull("edge_sidecar_to_children")){
                                count = jsonObject.getJSONObject("graphql").getJSONObject("shortcode_media").getJSONObject("edge_sidecar_to_children").getJSONArray("edges").length();
                            }
                            if(video){
                                type = TYPE_VIDEO;
                                getVideo();
                            }else {
                                type = TYPE_IMG;
                                getImage();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                            mOnCallBack.onResponse(UrlMedia.this);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                mOnCallBack.onErrorResponse(error.toString());
            }
        });

        queue.add(stringRequest);

    }

    private void getImage() throws JSONException {
        if(count == 1){
            String url = jsonObject.getJSONObject("graphql").getJSONObject("shortcode_media").getJSONArray("display_resources").getJSONObject(2).getString("src");
            mList.clear();
            mList.add(url);
        }else{
            JSONArray jsonArray = jsonObject.getJSONObject("graphql").getJSONObject("shortcode_media").getJSONObject("edge_sidecar_to_children").getJSONArray("edges");
            mList.clear();
            for (int i = 0; i <  jsonArray.length(); i++ ){
                String url = jsonArray.getJSONObject(i).getJSONObject("node").getJSONArray("display_resources").getJSONObject(2).getString("src");
                mList.add(url);
            }
        }
    }

    private void getVideo() throws JSONException {
        String url = jsonObject.getJSONObject("graphql").getJSONObject("shortcode_media").getString("video_url");
        mList.clear();
        mList.add(url);
    }

    public int count() {
        return count;
    }

    public ArrayList<String> getList() {
        return mList;
    }

    private OnCallBack mOnCallBack;

    public void setmOnCallBack(OnCallBack mOnCallBack) {
        this.mOnCallBack = mOnCallBack;
    }

    interface OnCallBack{
        void onResponse( UrlMedia u );
        void onErrorResponse(String e);
    }

}
