package org.codefirst.imagestreamingwatchface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ClockAdapter extends BaseAdapter {
    protected Context mContext;
    protected LayoutInflater mLayoutInflater;
    protected String[] mSourceTitleArray;
    protected String[] mSourceUrlArray;

    protected LruCache<String, Bitmap> mBitmapCache;

    ClockAdapter(Context context, String[] sourceTitleArray, String[] sourceUrlArray) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mSourceTitleArray = sourceTitleArray;
        mSourceUrlArray = sourceUrlArray;

        mBitmapCache = new LruCache<String, Bitmap>(100);
    }

    @Override
    public int getCount() {
        return mSourceTitleArray.length;
    }

    @Override
    public Object getItem(int i) {
        return mSourceTitleArray[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    class ClockImageLoader extends AsyncTask<String, Void, Bitmap> {
        ImageView mImageView;
        String mUrl;
        int mTag;

        ClockImageLoader(ImageView imageView, int tag) {
            mImageView = imageView;
            mTag = tag;
        }

        @Override
        protected Bitmap doInBackground(String... url) {
            try {
                mUrl = url[0];
                InputStream stream = new URL(mUrl).openStream();
                return BitmapFactory.decodeStream(stream);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mBitmapCache.put(mUrl, bitmap);

                if (mImageView.getTag() == mTag) {
                    mImageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.clock, null);
        }

        TextView textView = (TextView)view.findViewById(R.id.textView);
        ImageView imageView = (ImageView)view.findViewById(R.id.imageView7);

        textView.setText(mSourceTitleArray[i]);

        Time time = new Time();
        time.setToNow();
        final String url = String.format(mSourceUrlArray[i], "t1", time.hour, time.minute);

        Bitmap bitmap = mBitmapCache.get(url);
        if (bitmap == null) {
            ClockImageLoader clockImageLoader = new ClockImageLoader(imageView, i);
            clockImageLoader.execute(url);

            if (imageView.getTag() != i) {
                imageView.setImageResource(R.mipmap.ic_launcher);
            }
        }
        else {
            imageView.setImageBitmap(bitmap);
        }

        imageView.setTag(i);

        return view;
    }
}
