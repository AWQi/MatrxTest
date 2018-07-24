package com.example.dell.matrxtest;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.widget.Toast;

import org.w3c.dom.DOMLocator;
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

import static android.content.ContentValues.TAG;

public class MyImageView extends android.support.v7.widget.AppCompatImageView {
    private  MyImageView myImageView = this;
    private List<Part> partList = null;
    private  Context context;
    float drawWidth ;
    float drawHeight ;
    float[] value = new float[9];
    float initScale = 1;
    float initTransX  = 0;
    float initTransY = 0;
    float initWidth ;
    float initHeight ;
    float maxScale = 5;
    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    private MyOnclickListener myOnclickListener = null;
    /**
     * 用于记录拖拉图片移动的坐标位置
     */
    private Matrix matrix ;
    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.setScaleType(ScaleType.MATRIX);
        this.setOnTouchListener(new TouchListener());
        partList =  analysisMapXml();
    }
    /**
     *
     *   初始化时  设置图片  缩放· 偏移
     * @return
     */
    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
         boolean change = super.setFrame(l, t, r, b);
        if (getScaleType()==ScaleType.MATRIX)transMatrix();
        return  change;
    }
    private void transMatrix() {
        matrix = getMatrix();
        float w = getWidth();
        float dw = getDrawable().getIntrinsicWidth();
        float  h = getHeight();
        float dh = getDrawable().getIntrinsicHeight();
        float drawPro = dw/dh;
        float imagePro = w/h;
        if (drawPro>imagePro){
            float widthScaleFactor = w/dw;
            initScale = widthScaleFactor;
            matrix.postScale(initScale,initScale,0,0);
            initTransY = (h-dh*initScale)/2;
            matrix.postTranslate(0,initTransY);
        }else {
            float heightScaleFactor = h/dh;
            initScale =heightScaleFactor;
            matrix.postScale(initScale,initScale,0,0);
            initTransX = (w-dw*initScale)/2;
            matrix.postTranslate(initTransX,0);
        }
        setImageMatrix(matrix);
        matrix.getValues(value);
        initWidth =  myImageView.getDrawable().getIntrinsicWidth()*initScale;
        initHeight = myImageView.getDrawable().getIntrinsicHeight()*initScale;
        Log.d(TAG, "initScale-------------: "+initScale);
        Log.d(TAG, "initTransX: ----------:"+initTransX);
        Log.d(TAG, "initTransY:-----------:"+initTransY);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawWidth = myImageView.getDrawable().getIntrinsicWidth();
        drawHeight = myImageView.getDrawable().getIntrinsicHeight();
        Log.d(TAG, "onLayout: drawWidth-------:"+drawWidth);
        Log.d(TAG, "onLayout: drawHeight------:"+drawHeight);
        }



    public  interface MyOnclickListener {
        void onClick(String part);
    }
    public void setMyOnClickListener(MyOnclickListener myOnClickListener){
            this.myOnclickListener = myOnClickListener;
}

    private class TouchListener extends SimpleOnScaleGestureListener implements OnTouchListener,GestureDetector.OnGestureListener,GestureDetector.OnDoubleTapListener{
        private static final int FLING_MIN_DISTANCE = 20;// 移动最小距离
        private static final int FLING_MAX_VELOCITY = 200;// 最大移动速度
        // 构建手势探测器
        GestureDetector gestureDetector = new GestureDetector(context, this);
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context,this);

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
        private float scale = 1f;
        /**
         * 用于记录开始时候的坐标位置
         */
        private PointF startPoint = new PointF();
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

        public TouchListener() {
            currentMatrix.postScale(initScale, initScale);
        }

        public RectF getRectF(Matrix matrix) {
            RectF rectF = new RectF();
            Drawable drawable = myImageView.getDrawable();

            if (drawable != null) {
                rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
//                rectF.set(0, 0, myImageView.getWidth(),myImageView.getHeight());
                matrix.mapRect(rectF);
                // 矩形是现在的图片相对于 imageview 左上角  因为 matrix默认在左上角开始画
                Log.d(TAG, "onTouch: rectF.left``" + rectF.left);
                Log.d(TAG, "onTouch: rectF.top``" + rectF.top);
                Log.d(TAG, "onTouch: rectF.right``" + rectF.right);
                Log.d(TAG, "onTouch: rectF.bottom``" + rectF.bottom);

                return rectF;
            }
            return null;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
//            float width = v.getWidth();
//            float height = v.getHeight();
//            Log.d(TAG, "width: --------------------------" + width);
//            Log.d(TAG, "height:--------------------------- " + height);
//            matrix.set(myImageView.getImageMatrix());
//
//            /** 通过与运算保留最后八位 MotionEvent.ACTION_MASK = 255 */
//            switch (event.getAction() & MotionEvent.ACTION_MASK) {
//                // 手指压下屏幕
//                case MotionEvent.ACTION_DOWN:
//                    currentMatrix.set(myImageView.getImageMatrix());
////                    map.setScaleType(ImageView.ScaleType.MATRIX);
//                    mode = MODE_CLICK;
//                    // 记录ImageView当前的移动位置
//                    startPoint.set(event.getX(), event.getY());
//                    break;
//                // 手指在屏幕上移动，改事件会被不断触发
//                case MotionEvent.ACTION_MOVE:
//                    if (mode == MODE_CLICK) {
//                        mode = MODE_DRAG;
//                    }
//                    // 拖拉图片
//                    if (mode == MODE_DRAG) {
//                        float dx = event.getX() - startPoint.x; // 得到x轴的移动距离
//                        float dy = event.getY() - startPoint.y; // 得到y轴的移动距离
//                        Log.d(TAG, "dx:   ----------    " + dx);
//                        Log.d(TAG, "dy: -----------      " + dy);
//                        // 移动
//                        matrix.set(currentMatrix);
//                        matrix.postTranslate(dx, dy);
//                        RectF rectF = getRectF(matrix);
//                        if (rectF != null) {
//                            Log.d(TAG, "onTouch: -----------------rectF.left+dx  " + rectF.left + dx);
//                            Log.d(TAG, "onTouch: -----------------rectF.top+dy  " + rectF.top + dy);
//                            // 调整边界位置
//                            dx = 0;
//                            dy = 0;
//                            if (rectF.left >= initTransX) {
//                                dx = initTransX - rectF.left;
//                            }
//                            if (rectF.top >= initTransY) {
//                                dy = initTransY - rectF.top;
//                            }
//                            if (rectF.right <= initWidth + initTransX) {
//                                dx = initTransX + initWidth - rectF.right;
//                            }
//                            if (rectF.bottom <= initHeight + initTransY) {
//                                dy = initTransY + initHeight - rectF.bottom;
//                            }
//                            matrix.postTranslate(dx, dy);
//                        }
//                    }
//                    // 放大缩小图片
//                    else if (mode == MODE_ZOOM) {
//                        float endDis = distance(event);// 结束距离
//                        if (endDis > 10f) { // 两个手指并拢在一起的时候像素大于10
//                            float sc = (endDis / startDis);// 得到缩放倍数
//                            matrix.getValues(value);
//                            Log.d(TAG, "sc: ------------------:  " + sc * value[Matrix.MSCALE_X]);
//                            // 限定大小
//                            if (sc * value[Matrix.MSCALE_X] >= initScale && sc * value[Matrix.MSCALE_X] <= maxScale) {
//                                // 缩放
//                                matrix.set(currentMatrix);
//                                matrix.postScale(sc, sc, midPoint.x, midPoint.y);
//                                // 调整边界位置
//                                RectF rectF = getRectF(matrix);
//                                if (rectF != null) {
//                                    float dx = 0;
//                                    float dy = 0;
//                                    if (rectF.left >= initTransX) {
//                                        dx = initTransX - rectF.left;
//                                    }
//                                    if (rectF.top >= initTransY) {
//                                        dy = initTransY - rectF.top;
//                                    }
//                                    if (rectF.right <= initWidth + initTransX) {
//                                        dx = initTransX + initWidth - rectF.right;
//                                    }
//                                    if (rectF.bottom <= initHeight + initTransY) {
//                                        dy = initTransY + initHeight - rectF.bottom;
//                                    }
//                                    matrix.postTranslate(dx, dy);
//                                }
//
//                            }
//                        }
//                    }
//                    break;
//                // 手指离开屏幕
//                case MotionEvent.ACTION_UP:
//                    float x = event.getX();
//                    float y = event.getY();
//                    Log.d(TAG, "onTouch: x------" + x);
//                    Log.d(TAG, "onTouch: y------" + y);
//                    /**
//                     *    转换 图片坐标 百分比
//                     */
//                    RectF rectF = getRectF(matrix);
//                    if (rectF != null) {
//                        myImageView.getImageMatrix().getValues(value);
//                        scale = value[Matrix.MSCALE_X];
//                        x = (x - rectF.left) / (drawWidth * scale) * 100;
//                        y = (y - rectF.top) / (drawHeight * scale) * 100;
//                        Log.d(TAG, "onTouch: 区域适配：x   " + x);
//                        Log.d(TAG, "onTouch: 区域适配：y   " + y);
//                        String partName = matchArea(partList, x, y);
////                        Toast.makeText(context,"区域适配：x: "+x+" y:"+y,Toast.LENGTH_SHORT).show();
//                        Log.d(TAG, "您所点击的区域: " + partName);
//                        Log.d(TAG, "myOnclickListener: " + myOnclickListener);
//                        if (myOnclickListener != null) {
//                            myOnclickListener.onClick(partName);
//                        }
//
//                    }
//                    // 当触点离开屏幕，但是屏幕上还有触点(手指)
//                case MotionEvent.ACTION_POINTER_UP:
//                    mode = 0;
//                    return true;
////                    break;
//
//                // 当屏幕上已经有触点(手指)，再有一个触点压下屏幕
//                case MotionEvent.ACTION_POINTER_DOWN:
//                    mode = MODE_ZOOM;
//                    /** 计算两个手指间的距离 */
//                    startDis = distance(event);
//                    /** 计算两个手指间的中间点 */
//                    if (startDis > 10f) { // 两个手指并拢在一起的时候像素大于10
//                        midPoint = mid(event);
//                        //记录当前ImageView的缩放倍数
//                        currentMatrix.set(myImageView.getImageMatrix());
//                    }
//                    break;
//            }
//
//
//            Log.d(TAG, "onTouch: ");
//            myImageView.setImageMatrix(matrix);
//            Log.d(TAG, "getImageMatrix:-------- " + myImageView.getImageMatrix());
//            Log.d(TAG, "matrixScale: ---------" + matrix);
//            Log.d(TAG, "currentMatrixScale: ---------" + currentMatrix);



            matrix.set(myImageView.getImageMatrix());
            // 将事件交给  gestureDetector处理
            gestureDetector.onTouchEvent(event);
            scaleGestureDetector.onTouchEvent(event);
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

        // 用户按下屏幕时触发
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown: ");
            currentMatrix.set(myImageView.getImageMatrix());
            return true;
        }

        //  用户按下100ms后触发
        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(TAG, "onShowPress: ");
        }

        // 手指平常松开屏幕
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(TAG, "onSingleTapUp: ");
            return true;
        }

        // 手指滑动 触发move，且距离大于一定值
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "onScroll: ");
                        // 移动  事件
//                float dx = e2.getX()-e1.getX();
//                float dy = e2.getY()-e1.getY();
            float dx = -distanceX;
            float dy = -distanceY;
                 Log.d(TAG, "dx: -------------:"+distanceX);
                 Log.d(TAG, "dy: -------------:"+distanceY);
//                        matrix.set(currentMatrix);
                        matrix.postTranslate(dx, dy);
                        RectF rectF = getRectF(matrix);
                        if (rectF != null) {
                            Log.d(TAG, "onTouch: -----------------rectF.left+dx  " + rectF.left + dx);
                            Log.d(TAG, "onTouch: -----------------rectF.top+dy  " + rectF.top + dy);
                            // 调整边界位置
                            dx = 0; dy = 0;
                            if (rectF.left >= initTransX) {
                                dx = initTransX - rectF.left;
                            }
                            if (rectF.top >= initTransY) {
                                dy = initTransY - rectF.top;
                            }
                            if (rectF.right <= initWidth + initTransX) {
                                dx = initTransX + initWidth - rectF.right;
                            }
                            if (rectF.bottom <= initHeight + initTransY) {
                                dy = initTransY + initHeight - rectF.bottom;
                            }
                            matrix.postTranslate(dx, dy);
                        }
                myImageView.setImageMatrix(matrix);
            return false;
        }

        // 长按事件
        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress: ");
        }

        // 抛事件
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "onFling: ");
            float dx = e2.getX()-e1.getX();
            float dy = e2.getY()-e1.getY();

            return false;
        }

        // 单击事件
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(TAG, "onSingleTapConfirmed: ");
            float x = e.getX();
            float y = e.getY();
            Log.d(TAG, "onTouch: x------" + x);
            Log.d(TAG, "onTouch: y------" + y);
            /**
             *    转换 图片坐标 百分比
             */
            RectF rectF = getRectF(matrix);
            if (rectF != null) {
                myImageView.getImageMatrix().getValues(value);
                scale = value[Matrix.MSCALE_X];
                x = (x - rectF.left) / (drawWidth * scale) * 100;
                y = (y - rectF.top) / (drawHeight * scale) * 100;
                String partName = matchArea(partList, x, y);
                if (myOnclickListener != null) {
                    myOnclickListener.onClick(partName);
                }
            }
            return true;
        }
        // 双击事件
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap: ");
            return false;
        }
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d(TAG, "onDoubleTapEvent: ");
            return false;
        }
        // 缩放事件
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

                            // 放大缩小图片
                            float sc = detector.getScaleFactor();// 得到缩放倍数
                            matrix.set(currentMatrix);

                            matrix.getValues(value);
                            // 限定大小
//                            sc = sc * value[Matrix.MSCALE_X] < initScale?
                            if (sc * value[Matrix.MSCALE_X] < initScale){
                                matrix.setScale(initScale,initScale);
                            }else if ( sc * value[Matrix.MSCALE_X] > maxScale){
                                matrix.set(currentMatrix);
                                matrix.setScale(maxScale,maxScale);
                            }else {
                                matrix.set(currentMatrix);
                                matrix.postScale(sc,sc,detector.getFocusX(),detector.getFocusY());
                            }
                                // 调整边界位置
                                RectF rectF = getRectF(matrix);
                                if (rectF != null) {
                                    float dx = 0;
                                    float dy = 0;
                                    if (rectF.left >= initTransX) {
                                        dx = initTransX - rectF.left;
                                    }
                                    if (rectF.top >= initTransY) {
                                        dy = initTransY - rectF.top;
                                    }
                                    if (rectF.right <= initWidth + initTransX) {
                                        dx = initTransX + initWidth - rectF.right;
                                    }
                                    if (rectF.bottom <= initHeight + initTransY) {
                                        dy = initTransY + initHeight - rectF.bottom;
                                    }
                                    matrix.postTranslate(dx, dy);
                                }


//            Log.d(TAG, "onScale: ");
//            float s = detector.getScaleFactor();
//            float x = detector.getFocusX();
//            float y = detector.getFocusY();
//            matrix.postScale(s,s,x,y);
            myImageView.setImageMatrix(matrix);
            return super.onScale(detector);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleBegin: ");
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleEnd: ");
            super.onScaleEnd(detector);
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
