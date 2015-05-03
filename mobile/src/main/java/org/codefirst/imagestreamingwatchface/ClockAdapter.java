package org.codefirst.imagestreamingwatchface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.format.Time;
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

    ClockAdapter(Context context, String[] sourceTitleArray, String[] sourceUrlArray) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mSourceTitleArray = sourceTitleArray;
        mSourceUrlArray = sourceUrlArray;
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

        ClockImageLoader(ImageView imageView) {
            mImageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... url) {
            try {
                InputStream stream = new URL(url[0]).openStream();
                return BitmapFactory.decodeStream(stream);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mImageView.setImageBitmap(bitmap);
            }
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.clock, null);
        }

        TextView textView = (TextView)view.findViewById(R.id.textView);
        textView.setText(mSourceTitleArray[i]);

        Time time = new Time();
        time.setToNow();
        ImageView imageView = (ImageView)view.findViewById(R.id.imageView7);

        final String url = String.format(mSourceUrlArray[i], "t1", time.hour, time.minute);
        ClockImageLoader clockImageLoader = new ClockImageLoader(imageView);
        clockImageLoader.execute(url);

        return view;
    }
}
