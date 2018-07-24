package com.example.dell.matrxtest;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

class Image extends View{
    private static final String TAG = "Image";
    private  Context context;
    private  Drawable drawable;
    private Bitmap bitmap;
    private  float bWidth;
    private  float bHeight;
    private Paint paint = new Paint();
    public Image(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        drawable = getResources().getDrawable(R.drawable.map);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.map);
        bWidth = bitmap.getWidth();
        bHeight = bitmap.getHeight();
        setMeasuredDimension((int) bWidth,(int) bHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap,0,0,paint);
    }

}