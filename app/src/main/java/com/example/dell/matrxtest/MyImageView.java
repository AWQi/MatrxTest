package com.example.dell.matrxtest;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
        drawWidth = myImageView.getDrawable().getIntrinsicWidth();
        drawHeight = myImageView.getDrawable().getIntrinsicHeight();
        Log.d(TAG, "onLayout: drawWidth-------:"+drawWidth);
        Log.d(TAG, "onLayout: drawHeight------:"+drawHeight);
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
        Log.d(TAG, "onDraw: =========================");

        super.onDraw(canvas);

        }



    public  interface MyOnclickListener {
        void onClick(String part);
    }
    public void setMyOnClickListener(MyOnclickListener myOnClickListener){
            this.myOnclickListener = myOnClickListener;
}

    public  interface TimerTaskCallBack{
        void  callBack(int n);
    }



    private class TouchListener extends SimpleOnScaleGestureListener implements OnTouchListener,GestureDetectorEx.OnGestureListener,GestureDetectorEx.OnDoubleTapListener{
        private static final int FLING_MIN_DISTANCE = 20;// 移动最小距离
        private static final int FLING_MAX_VELOCITY = 200;// 最大移动速度
        // 构建手势探测器
        GestureDetectorEx gestureDetector = new GestureDetectorEx(context, this);
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
        private Handler handler =null;
        private static  final  int  FLING_TRANS = 101;
        private static  final  int BORDER_TRANS = 102;
        private static  final  int QUICK_SCALE = 103;
        private Thread flingThread = null; //快速滑动事件  线程
        private Thread borderThread = null; // 边界调整 线程
        private Thread scaleThread = null;// 快速缩放 线程
        private Timer timer  = new Timer(); // 定时任务用于执行 ，  动画效果
        private TimerTaskCallBack callBack  = null;
        public  void setTimerTaskCallBack(TimerTaskCallBack callBack){
            this.callBack  = callBack;
        }
        public TouchListener() {
            currentMatrix.postScale(initScale, initScale);
            handler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what){
                        case FLING_TRANS : {
                            Bundle bundle = msg.getData();
                            float vx = (float) bundle.get("vx");
                            float vy = (float) bundle.get("vy");
                            Log.d(TAG, "handleMessage: vx" + vx);
                            Log.d(TAG, "handleMessage: vy" + vy);
                            Log.d(TAG, "handleMessage: myImageView.getImageMatrix()" + myImageView.getImageMatrix());

                            matrix.set(myImageView.getImageMatrix());
                            matrix.postTranslate(vx, vy);
                            matrix = borderDefinition(matrix);
                            myImageView.setImageMatrix(matrix);
                        }
                            break;
                        case  BORDER_TRANS: {
                            Log.d(TAG, "BORDER_TRANS: ");
                            Bundle bundle = msg.getData();
                            float vx = (float) bundle.get("vx");
                            float vy = (float) bundle.get("vy");
                            matrix.set(myImageView.getImageMatrix());
                            matrix.postTranslate(vx,vy);
                            myImageView.setImageMatrix(matrix);
                            break;
                        }
                        case QUICK_SCALE:{
                            Bundle bundle = msg.getData();
                            float vs = (float) bundle.get("vs");
                            matrix.set(myImageView.getImageMatrix());
                            Log.d(TAG, "QUICK---------  vs"+vs);
                            matrix.postScale(vs,vs);
                            myImageView.setImageMatrix(matrix);
                             break;
                        }
                        default:break;
                    }
                }
            };
        }
       public float slowMotionFunction(float x){
            return (x-1)*(x-1)*(x-1)*(x-1);
       }

       /**
         * 调整边界位置
         */
       public  void borderAdjustment(){

           /**
            * 分不同的情况 进行图片回调
            * 此时drawable大小不足以填满 imageView 时,就以初始drawable 边框为界
            * 此时drawbale大小足以填满 imageView 时，就以   imageView 边框为界
            */

           RectF rectF = getRectF(matrix);
           matrix.set(myImageView.getImageMatrix());
           matrix.getValues(value);
           float scale = value[Matrix.MSCALE_X];
           float borderLeft;
           float borderTop;
           float borderRight;
           float borderBottom;
           if (myImageView.getWidth()<scale*drawWidth
                   &&myImageView.getHeight()<scale*drawHeight){// 足以填满
               borderLeft =  0;
               borderTop = 0;
               borderRight = myImageView.getWidth();
               borderBottom = myImageView.getHeight();
           }else { // 填不满
                borderLeft = initTransX;
                borderTop = initTransY;
                borderRight = initTransX + initWidth;
                borderBottom = initTransY+ initHeight;

           }
           float dx = 0; float dy = 0;
           if (rectF != null) {
               if (rectF.left >= initTransX) {
                   dx = borderLeft- rectF.left;
               }
               if (rectF.top >= initTransY) {
                   dy = borderTop - rectF.top;
               }
               if (rectF.right <= initWidth + initTransX) {
                   dx = borderRight - rectF.right;
               }
               if (rectF.bottom <= initHeight + initTransY) {
                   dy = borderBottom - rectF.bottom;
               }
           }

           final int t = 50; // 每一次回移的时间差 ms
           final int n = 10;//总共需要回调的次数
           final float vx =  dx/n; // 每次位移的x距离
           final float vy =  dy/n; // 每次位移的y距离
           timer.cancel();
            callBack = null;
           timer = new Timer();
           timer.schedule(new TimerTask() {
               @Override
               public void run() {
                   int cn = 1; // 当前次数
                   /**
                    *   发送数据
                    */
                        Message msg = new Message();
                        msg.what = BORDER_TRANS;
                        Bundle bundle = new Bundle();
                        bundle.putFloat("vx",vx);
                        bundle.putFloat("vy",vy);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                   if (callBack!=null&&cn<n){
                        callBack.callBack(cn+1);
                   }

               }
           },1);
            setTimerTaskCallBack(new TimerTaskCallBack() {
                @Override
                public void callBack(final int cn) {
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            /**
                             *   发送数据
                             */
                            Message msg = new Message();
                            msg.what = BORDER_TRANS;
                            Bundle bundle = new Bundle();
                            bundle.putFloat("vx",vx);
                            bundle.putFloat("vy",vy);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                            Log.d(TAG, "cn: -------------: "+cn);
                            if (callBack!=null&&cn<n){
                                callBack.callBack(cn+1);
                            }
                        }
                    },t);
                }
            });

//
//           final float finalDx = dx;
//           final float finalDy = dy;
//           borderThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    int t = 1; // 每一次回移的时间差 ms
//                    float n = 500;//总共需要回调的次数
//                    //
//                    float timer = 0;
//                    while(timer<1) {
//                        float vx =  finalDx/n;
//                        float vy =  finalDy/n;
//                        Message msg = new Message();
//                        msg.what = BORDER_TRANS;
////                        float vx = slowMotionFunction(timer)* finalDx;
////                        float vy = slowMotionFunction(timer)* finalDy;
//                        Bundle bundle = new Bundle();
//                        bundle.putFloat("vx",vx);
//                        bundle.putFloat("vy",vy);
//                        msg.setData(bundle);
//                        handler.sendMessage(msg);
//                        timer+=1/n;
//                        try {
//                            Thread.sleep(t);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            });
//           borderThread.start();
       }

        /**
         *   边界 限定
         */
       public  Matrix borderDefinition(Matrix matrix){
                        float dd = 100f;  //超出边界限制
                        RectF rectF = getRectF(matrix);
                        if (rectF != null) {

                            float dx = 0; float dy = 0;
                            if (rectF.left >= dd+initTransX) {
                                dx =dd+ initTransX - rectF.left;
                            }
                            if (rectF.top >=dd+ initTransY) {
                                dy = dd+initTransY - rectF.top;
                            }
                            if (rectF.right <= -dd+initWidth + initTransX) {
                                dx = -dd+initTransX + initWidth - rectF.right;
                            }
                            if (rectF.bottom <= -dd+initHeight + initTransY) {
                                dy = -dd+initTransY + initHeight - rectF.bottom;
                            }
                            matrix.postTranslate(dx, dy);
                        }
                        return  matrix;
       }

        /**
         *   获取图形矩阵
         *
         * @param matrix
         * @return
         */
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


                return  true;


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
//            borderAdjustment();
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
            matrix.postTranslate(dx, dy);
            matrix = borderDefinition(matrix);
            myImageView.setImageMatrix(matrix);
            return true;
        }

        // 长按事件
        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress: ");
        }

        // 抛事件
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, final float velocityX, final float velocityY) {
            Log.d(TAG, "onFling: ");


//            timer.cancel();
//            timer = new Timer();
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    float ti =0; // 线程终止标志
//                    while (ti <1){
//                        float k = 0.1f; //  滑动速率限定
//                        float vy = velocityY*slowMotionFunction(ti)*k; //  x速率
//                        float vx= velocityX*slowMotionFunction(ti)*k; //  y 速率
//                        int t = 100;// 每次移动的时间间隔
//                        float n  = 10; // 移动次数
//                        Message msg = new Message();
//                        Bundle bundle =  new Bundle();
//                        bundle.putFloat("vx",vx);
//                        bundle.putFloat("vy",vy);
//                        msg.setData(bundle);
//                        msg.what =  FLING_TRANS;
//                        handler.sendMessage(msg);
//                        ti +=1/n;
//                        try {
//                            Thread.sleep(t);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    borderAdjustment();
//                }
//            },0);


//            flingThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    float ti =0; // 线程终止标志
//                    while (ti <1){
//                        float k = 0.1f; //  滑动速率限定
//                        float vy = velocityY*slowMotionFunction(ti)*k; //  x速率
//                        float vx= velocityX*slowMotionFunction(ti)*k; //  y 速率
//                        int t = 100;// 每次移动的时间间隔
//                        float n  = 10; // 移动次数
//                        Message msg = new Message();
//                        Bundle bundle =  new Bundle();
//                        bundle.putFloat("vx",vx);
//                        bundle.putFloat("vy",vy);
//                        msg.setData(bundle);
//                        msg.what =  FLING_TRANS;
//                        handler.sendMessage(msg);
//                        ti +=1/n;
//                        try {
//                            Thread.sleep(t);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    borderAdjustment();
//                }
//            });
//            flingThread.start();
            return true;
        }

        @Override
        public boolean onScrollUp(MotionEvent e) {
            Log.d(TAG, "onScrollUp: "+e.getX()+","+e.getY());
            borderAdjustment();
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
            return true;
        }
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d(TAG, "onDoubleTapEvent: ");
            return true;
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
                            matrix = borderDefinition(matrix);
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
            final long time = detector.getTimeDelta();// 得到两次事件的时间差 快速缩放与慢速缩放的时间差不同 可以进行判断   100
            final float factor = detector.getScaleFactor();// 得到缩放因子

            if (time<100){// 判断是否是快速缩放
                Log.d(TAG, "quickScale: ");
                int s =factor>1?1:0; //记录是缩还是放, 缩为0 ,放为1
                scaleThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        float n = 10; // 持续缩放的次数
                        int t = 1; // 缩放的时间间隔
                        float timer = 0;//
                        while (timer<1){
                            float vs = slowMotionFunction(timer)*Math.abs(factor-1); // 根据函数获取变化速率
                            vs = factor>1?vs+1:vs;// 速率添加缩放状态
                            Message msg = new Message();
                            msg.what = QUICK_SCALE;
                            Bundle bundle = new Bundle();
                            bundle.putFloat("vs",vs);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                            timer+=1/n;
                            Log.d(TAG, "timer---------------: "+timer);
                            try {
                                Thread.sleep(t);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });
//                scaleThread.start();
            }
            Log.d(TAG, "onScaleEnd factor----------------------------: "+factor);
            Log.d(TAG, "onScaleEnd time------------------------------: "+time);
            super.onScaleEnd(detector);
        }


        public  class  MatrixThread extends  Thread{
            private  int type ;
            public MatrixThread(int type) {
                this.type = type;
            }
            @Override
            public void run() {
                switch (type){
                    case FLING_TRANS :

                        break;
                    case BORDER_TRANS:

                        break;
                    case QUICK_SCALE :

                        break;
                    default:break;
                }
            }
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
