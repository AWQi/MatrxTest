package com.example.dell.matrxtest;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

public class MapLayout  extends FrameLayout {
    private static final String TAG = "MapActivity";
    private ImageView map = null;
    private Button vernier = null;
    private List<Part> partList = null;
    private  Context context;

    float mapWidth ;
    float mapHeight ;
    float layoutWidth ;
    float layoutHeight ;
    float mK ;
    float lK ;
    float drawWidth ;
    float drawHeight ;
    float scale;
    boolean init = false;





    public MapLayout(@NonNull Context context) {
        super(context);
        init(context);
    }
    public MapLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public  void init(Context context){
        this.setBackgroundColor(Color.RED);
        this.context = context;
//        map = findViewById(R.id.map);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        map = new ImageView(context);
        map.setLayoutParams(layoutParams);
        layoutParams.gravity = Gravity.CENTER;
        map.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.addView(map);

        map.setImageResource(R.drawable.map);
        LayoutParams btnParam = new LayoutParams(30, 30);
        vernier = new Button(context);

        vernier.setBackgroundColor(Color.RED);
        vernier.setLayoutParams(btnParam);
        this.addView(vernier);
//        vernier = findViewById(R.id.vernier);
        map.setOnTouchListener(new TouchListener());
        partList =  analysisMapXml();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.d(TAG, "```````````onLayout: ");
        mapWidth =map.getMeasuredWidth();
        mapHeight = map.getMeasuredHeight();
        layoutWidth = this.getMeasuredWidth();
        layoutHeight = this.getMeasuredHeight();
        mK = mapWidth/mapHeight;
        lK = layoutWidth/layoutHeight;
        drawWidth = map.getDrawable().getIntrinsicWidth();
        drawHeight = map.getDrawable().getIntrinsicHeight();


        Log.d(TAG, "onLayout: mapWidth-- -----:"+mapWidth);
        Log.d(TAG, "onLayout: mapHeight-------:"+mapHeight);
        Log.d(TAG, "onLayout: layoutWidth-----:"+layoutWidth);
        Log.d(TAG, "onLayout: layoutHeight----:"+layoutHeight);
        Log.d(TAG, "onLayout: drawWidth-------:"+drawWidth);
        Log.d(TAG, "onLayout: drawHeight------:"+drawHeight);


//        if (mK>lK){
//            map.layout(0,(int)((layoutHeight-mapHeight)/2),(int)layoutWidth,(int)((layoutHeight+mapHeight)/2));
//        }

    }

    void translateAnimation(View view, float desX, float desY) {
        ObjectAnimator animatorX = ObjectAnimator.ofFloat(view, "translationX", view.getX(), desX);
        animatorX.setDuration(500);
        animatorX.start();
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(view, "translationY", view.getY(), desY);
        animatorY.setDuration(500);
        animatorY.start();

    }

    private class TouchListener implements OnTouchListener {

        /**
         * 记录是拖拉照片模式还是放大缩小照片模式
         */
        private int mode = 0;// 初始状态
        private static final int MODE_DEFAULT = 0;
        /**
         * 拖拉照片模式
         */
        private static final int MODE_DRAG = 1;
        /**
         * 放大缩小照片模式
         */
        private static final int MODE_ZOOM = 2;
        private static final int MODE_CLICK = 3;
        private static final int MIN_DIS = 50;

        private float transX=0;
        private float transY=0;
        private float scale=1f;


        /**
         * 用于记录开始时候的坐标位置
         */
        private PointF startPoint = new PointF();
        /**
         * 用于记录拖拉图片移动的坐标位置
         */
        private Matrix matrix = new Matrix();
        /**
         * 用于记录图片要进行拖拉时候的坐标位置
         */
        private Matrix currentMatrix = new Matrix();

        /**
         * 两个手指的开始距离
         */
        private float startDis;
        /**
         * 两个手指的中间点
         */
        private PointF midPoint;


        public RectF getRectF(){
            RectF rectF = new RectF();
            Drawable drawable = map.getDrawable();

            if (drawable != null) {
                rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                matrix.mapRect(rectF);
                Log.d(TAG, "onTouch: rectF.left``"+rectF.left);
                Log.d(TAG, "onTouch: rectF.top``"+rectF.top);
                Log.d(TAG, "onTouch: rectF.right``"+rectF.right);
                Log.d(TAG, "onTouch: rectF.bottom``"+rectF.bottom);
                return   rectF;
            }
            return  null;
        }

public  void initImageView(){
    float drawPropor = drawWidth/layoutWidth;
    float mapPropor = mapWidth/layoutHeight;
    map.setScaleType(ImageView.ScaleType.MATRIX);
    map.setImageMatrix(matrix);
    if (drawPropor>mapPropor){// 宽对齐
        float k = mapWidth/drawWidth;
        matrix.postScale(k,k);
    }else if (drawPropor<mapPropor){// 高对齐
        float k = mapHeight/drawHeight;
        matrix.postScale(k,k);
    }else {// 只缩放大小

    }
    map.setImageMatrix(matrix);
    init=true;
}


        @Override
        public boolean onTouch(View v, MotionEvent event) {
//            if (!init)initImageView();
            float[] s = new float[9];
            float width = v.getWidth();
            float height = v.getHeight();
            Log.d(TAG, "width: --------------------------"+width);
            Log.d(TAG, "height:--------------------------- "+height);





            /** 通过与运算保留最后八位 MotionEvent.ACTION_MASK = 255 */
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                // 手指压下屏幕
                case MotionEvent.ACTION_DOWN:
//                    map.setScaleType(ImageView.ScaleType.MATRIX);
                    mode = MODE_CLICK;
                    // 记录ImageView当前的移动位置
                    currentMatrix.set(map.getImageMatrix());
                    startPoint.set(event.getX(), event.getY());
                    break;
                // 手指在屏幕上移动，改事件会被不断触发
                case MotionEvent.ACTION_MOVE:
                    if (mode == MODE_CLICK) {
                        mode = MODE_DRAG;
                    }
                    // 拖拉图片
                    if (mode == MODE_DRAG) {
                        float dx = event.getX() - startPoint.x; // 得到x轴的移动距离
                        float dy = event.getY() - startPoint.y; // 得到y轴的移动距离
                        Log.d(TAG, "dx:   ----------    "+dx);
                        Log.d(TAG, "dy: -----------      "+dy);
                        // 在没有移动之前的位置上进行移动

                        matrix.getValues(s);
//                        float sX = s[Matrix.MTRANS_X]+dx;
//                        float sY = s[Matrix.MTRANS_Y]+dy;
                        float sX =transX+dx;
                        float sY =transY+dy;
                        Log.d(TAG, "sX: -------------     "+sX);
                        Log.d(TAG, "sY: ------------      "+sY);
                        RectF rectF = getRectF();
                        if ( rectF != null) {
                            Log.d(TAG, "onTouch: -----------------rectF.left+dx  "+rectF.left+dx);
                            Log.d(TAG, "onTouch: -----------------rectF.top+dy  "+rectF.top+dy);
                            if (rectF.left+dx<=0
                                    &&rectF.top+dy<=0
                                    &&rectF.right+dx>=drawWidth
                                    &&rectF.bottom+dy>=drawHeight){
                                matrix.set(currentMatrix);
                                matrix.postTranslate(dx, dy);
                                transX  = sX;
                                transY = sY;
                                Log.d(TAG, "transX:----------------------: "+transX);
                                Log.d(TAG, "transY:----------------------: "+transY);

                            }
                        }


//                        sX = sX<(mapWidth-drawWidth)?mapWidth-drawWidth:sX;
//                        sX = sX>0?0:sX;
//                        sY = sY<mapHeight-drawHeight?mapHeight-drawHeight:sY;
//                        sY = sY>0?0:sY;
//
//                        matrix.set(currentMatrix);
//                        matrix.setTranslate(sX,sY);
//                        transX  = sX;
//                        transY = sY;
//                        Log.d(TAG, "transX:----------------------: "+transX);
//                        Log.d(TAG, "transY:----------------------: "+transY);


//                        if (sX<=0&&sX>=(mapWidth-drawWidth)&&sY<=0&&sY>=(mapHeight-drawHeight))
//                        {
//                            matrix.set(currentMatrix);
//                            matrix.postTranslate(dx, dy);
//                            transX  = sX;
//                            transY = sY;
////                            transX +=dx;
////                            transY+=dy;
//                            Log.d(TAG, "transX:----------------------: "+transX);
//                            Log.d(TAG, "transY:----------------------: "+transY);
//                        }


                    }
                    // 放大缩小图片
                    else if (mode == MODE_ZOOM) {
                        float endDis = distance(event);// 结束距离
                        if (endDis > 10f) { // 两个手指并拢在一起的时候像素大于10
                            float sc = (endDis / startDis);// 得到缩放倍数
                            matrix.getValues(s);
                            Log.d(TAG, "sc: ------------------:  "+sc*s[Matrix.MSCALE_X]);
                            if (sc*s[Matrix.MSCALE_X]>=1&&sc*s[Matrix.MSCALE_X]<=3){
                                matrix.set(currentMatrix);
                                matrix.postScale(sc,sc, midPoint.x, midPoint.y);
                            }

//                            matrix.postScale(sc, sc, midPoint.x, midPoint.y);
//                            sc = sc*s[Matrix.MSCALE_X]>1?sc*s[Matrix.MSCALE_X]:1;
//                            sc = sc*s[Matrix.MSCALE_X]<3?sc*s[Matrix.MSCALE_X]:3;
//                            scale = sc;//     scale = s[Matrix.MSCALE_Y];
//                            Log.d(TAG, "scale:----------------------: "+scale);
                        }


                    }
                    break;
                // 手指离开屏幕
                case MotionEvent.ACTION_UP:
                    float x = event.getX();
                    float y = event.getY();
                    Log.d(TAG, "onTouch: x------" + x);
                    Log.d(TAG, "onTouch: y------" + y);
                    translateAnimation(vernier, x, y);
                    /**
                     *    转换 图片坐标 百分比
                     */

                    RectF rectF =getRectF();
                    if (rectF!=null){
                        x=(x-rectF.left)/(drawWidth*scale)*100;
                        y=(y-rectF.top)/(drawHeight*scale)*100;
                        Log.d(TAG, "onTouch: scale-----:"+scale);
                        Log.d(TAG, "onTouch: 区域适配：x   "+x);
                        Log.d(TAG, "onTouch: 区域适配：y   "+y);
                        String partName = matchArea(partList,x,y);
                        Toast.makeText(context,partName,Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "您所点击的区域: "+partName);
                    }
                    // 当触点离开屏幕，但是屏幕上还有触点(手指)
                case MotionEvent.ACTION_POINTER_UP:
                    mode = 0;
                    break;
                // 当屏幕上已经有触点(手指)，再有一个触点压下屏幕
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = MODE_ZOOM;
                    /** 计算两个手指间的距离 */
                    startDis = distance(event);
                    /** 计算两个手指间的中间点 */
                    if (startDis > 10f) { // 两个手指并拢在一起的时候像素大于10
                        midPoint = mid(event);
                        //记录当前ImageView的缩放倍数
                        currentMatrix.set(map.getImageMatrix());
                    }
                    break;
            }
            map.setImageMatrix(matrix);
            return true;
        }

        /**
         * 计算两个手指间的距离
         */
        private float distance(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            /** 使用勾股定理返回两点之间的距离 */
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        /**
         * 计算两个手指间的中间点
         */
        private PointF mid(MotionEvent event) {
            float midX = (event.getX(1) + event.getX(0)) / 2;
            float midY = (event.getY(1) + event.getY(0)) / 2;
            return new PointF(midX, midY);
        }

    }


    //  解析XML
    public List<Part> analysisMapXml() {
        //获取网络XML数据
        try {
            AssetManager assetManager = context.getAssets();

            InputStream is = assetManager.open("map.xml");
            //解析XMLDOM解析=====================================
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(is);
            //获取根标签
            Element element = document.getDocumentElement();
            Log.i("test", "根标签：" + element.getNodeName());
            NodeList nodeList = element.getElementsByTagName("part");
            List<Part> partList = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                //获取单个
                Element personElement = (Element) nodeList.item(i);
                //获取<person>属性id的值
                int id  =  Integer.parseInt(personElement.getAttribute("id"));
                //获取<person>下面的子标签<name><age>的值
                Element nameElement = (Element) personElement.getElementsByTagName("name").item(0);
                String name = nameElement.getTextContent();


                Element lElement = (Element) personElement.getElementsByTagName("l").item(0);
                float l = Float.parseFloat(lElement.getTextContent());
                Element tElement = (Element) personElement.getElementsByTagName("t").item(0);
                float t = Float.parseFloat(tElement.getTextContent());
                Element rElement = (Element) personElement.getElementsByTagName("r").item(0);
                float r = Float.parseFloat(rElement.getTextContent());
                Element bElement = (Element) personElement.getElementsByTagName("b").item(0);
                float b = Float.parseFloat(bElement.getTextContent());
                Part part = new Part(id,name,l,t,r,b);
                Log.d(TAG, "analysisMapXml:------------ "+part.toString());
                partList.add(part);
            }
            return  partList;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }
    public  String matchArea(List<Part> partList,float x,float y){

        for (Part p :partList){
            if (x>p.getL()&&x<p.getR()&&y>p.getT()&&y<p.getB()){
                return p.getName();
            }
        }
        return null;
    }
}
