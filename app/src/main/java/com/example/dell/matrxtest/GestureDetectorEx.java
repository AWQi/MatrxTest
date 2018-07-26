package com.example.dell.matrxtest;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class GestureDetectorEx {

    public interface OnGestureListener {
        boolean onDown(MotionEvent e);
        void onShowPress(MotionEvent e);
        boolean onSingleTapUp(MotionEvent e);
        boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
        void onLongPress(MotionEvent e);
        boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
        boolean onScrollUp(MotionEvent e);
    }

    public interface OnDoubleTapListener {
        boolean onSingleTapConfirmed(MotionEvent e);
        boolean onDoubleTap(MotionEvent e);
        boolean onDoubleTapEvent(MotionEvent e);
    }

    public interface OnContextClickListener {
        boolean onContextClick(MotionEvent e);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static class SimpleOnGestureListener implements android.view.GestureDetector.OnGestureListener, android.view.GestureDetector.OnDoubleTapListener,
            android.view.GestureDetector.OnContextClickListener {

        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        public void onLongPress(MotionEvent e) {
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            return false;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            return false;
        }

        public void onShowPress(MotionEvent e) {
        }

        public boolean onDown(MotionEvent e) {
            return false;
        }

        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }

        public boolean onContextClick(MotionEvent e) {
            return false;
        }
    }

    private int mTouchSlopSquare;
    private int mDoubleTapTouchSlopSquare;
    private int mDoubleTapSlopSquare;
    private int mMinimumFlingVelocity;
    private int mMaximumFlingVelocity;

    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final int DOUBLE_TAP_MIN_TIME = 40;

    // constants for Message.what used by GestureHandler below
    private static final int SHOW_PRESS = 1;
    private static final int LONG_PRESS = 2;
    private static final int TAP = 3;

    private final Handler mHandler;
    private final OnGestureListener mListener;
    private android.view.GestureDetector.OnDoubleTapListener mDoubleTapListener;
    private android.view.GestureDetector.OnContextClickListener mContextClickListener;

    private boolean mStillDown;
    private boolean mDeferConfirmSingleTap;
    private boolean mInLongPress;
    private boolean mInContextClick;
    private boolean mAlwaysInTapRegion;
    private boolean mAlwaysInBiggerTapRegion;
    private boolean mIgnoreNextUpEvent;

    private MotionEvent mCurrentDownEvent;
    private MotionEvent mPreviousUpEvent;

    /**
     * True when the user is still touching for the second tap (down, move, and
     * up events). Can only be true if there is a double tap listener attached.
     */
    private boolean mIsDoubleTapping;

    private float mLastFocusX;
    private float mLastFocusY;
    private float mDownFocusX;
    private float mDownFocusY;

    private boolean mIsLongpressEnabled;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    /**
     * Consistency verifier for debugging purposes.
     */
   /* private final InputEventConsistencyVerifier mInputEventConsistencyVerifier =
            InputEventConsistencyVerifier.isInstrumentationEnabled() ?
                    new InputEventConsistencyVerifier(this, 0) : null;*/

    private class GestureHandler extends Handler {
        GestureHandler() {
            super();
        }

        GestureHandler(Handler handler) {
            super(handler.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PRESS:
                    mListener.onShowPress(mCurrentDownEvent);
                    break;

                case LONG_PRESS:
                    dispatchLongPress();
                    break;

                case TAP:
                    // If the user's finger is still down, do not count it as a tap
                    if (mDoubleTapListener != null) {
                        if (!mStillDown) {
                            mDoubleTapListener.onSingleTapConfirmed(mCurrentDownEvent);
                        } else {
                            mDeferConfirmSingleTap = true;
                        }
                    }
                    break;

                default:
                    throw new RuntimeException("Unknown message " + msg); //never
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Deprecated
    public GestureDetectorEx(OnGestureListener listener, Handler handler) {
        this(null, listener, handler);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Deprecated
    public GestureDetectorEx(OnGestureListener listener) {
        this(null, listener, null);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public GestureDetectorEx(Context context, OnGestureListener listener) {
        this(context, listener, null);
    }

    /**
     * Creates a GestureDetector with the supplied listener that runs deferred events on the
     * thread associated with the supplied {@link android.os.Handler}.
     * @see android.os.Handler#Handler()
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     * @param handler the handler to use for running deferred listener events.
     *
     * @throws NullPointerException if {@code listener} is null.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public GestureDetectorEx(Context context, OnGestureListener listener, Handler handler) {
        if (handler != null) {
            mHandler = new GestureHandler(handler);
        } else {
            mHandler = new GestureHandler();
        }
        mListener = listener;
        if (listener instanceof android.view.GestureDetector.OnDoubleTapListener) {
            setOnDoubleTapListener((android.view.GestureDetector.OnDoubleTapListener) listener);
        }
        if (listener instanceof android.view.GestureDetector.OnContextClickListener) {
            setContextClickListener((android.view.GestureDetector.OnContextClickListener) listener);
        }
        init(context);
    }

    /**
     * Creates a GestureDetector with the supplied listener that runs deferred events on the
     * thread associated with the supplied {@link android.os.Handler}.
     * @see android.os.Handler#Handler()
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     * @param handler the handler to use for running deferred listener events.
     * @param unused currently not used.
     *
     * @throws NullPointerException if {@code listener} is null.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public GestureDetectorEx(Context context, OnGestureListener listener, Handler handler,
                             boolean unused) {
        this(context, listener, handler);
    }

    private void init(Context context) {
        if (mListener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
        mIsLongpressEnabled = true;

        // Fallback to support pre-donuts releases
        int touchSlop, doubleTapSlop, doubleTapTouchSlop;
        if (context == null) {
            //noinspection deprecation
            touchSlop = ViewConfiguration.getTouchSlop();
            doubleTapTouchSlop = touchSlop; // Hack rather than adding a hiden method for this
            doubleTapSlop = 100;
            //noinspection deprecation
            mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
            mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
        } else {
            final ViewConfiguration configuration = ViewConfiguration.get(context);
            touchSlop = configuration.getScaledTouchSlop();
            doubleTapTouchSlop = 8;
            doubleTapSlop = configuration.getScaledDoubleTapSlop();
            mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
            mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        }
        mTouchSlopSquare = touchSlop * touchSlop;
        mDoubleTapTouchSlopSquare = doubleTapTouchSlop * doubleTapTouchSlop;
        mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
    }

    /**
     * Sets the listener which will be called for double-tap and related
     * gestures.
     *
     * @param onDoubleTapListener the listener invoked for all the callbacks, or
     *        null to stop listening for double-tap gestures.
     */
    public void setOnDoubleTapListener(android.view.GestureDetector.OnDoubleTapListener onDoubleTapListener) {
        mDoubleTapListener = onDoubleTapListener;
    }

    /**
     * Sets the listener which will be called for context clicks.
     *
     * @param onContextClickListener the listener invoked for all the callbacks, or null to stop
     *            listening for context clicks.
     */
    public void setContextClickListener(android.view.GestureDetector.OnContextClickListener onContextClickListener) {
        mContextClickListener = onContextClickListener;
    }

    /**
     * Set whether longpress is enabled, if this is enabled when a user
     * presses and holds down you get a longpress event and nothing further.
     * If it's disabled the user can press and hold down and then later
     * moved their finger and you will get scroll events. By default
     * longpress is enabled.
     *
     * @param isLongpressEnabled whether longpress should be enabled.
     */
    public void setIsLongpressEnabled(boolean isLongpressEnabled) {
        mIsLongpressEnabled = isLongpressEnabled;
    }

    /**
     * @return true if longpress is enabled, else false.
     */
    public boolean isLongpressEnabled() {
        return mIsLongpressEnabled;
    }

    /**
     * Analyzes the given motion event and if applicable triggers the
     * appropriate callbacks on the {@link android.view.GestureDetector.OnGestureListener} supplied.
     *
     * @param ev The current motion event.
     * @return true if the {@link android.view.GestureDetector.OnGestureListener} consumed the event,
     *              else false.
     */
    public boolean onTouchEvent(MotionEvent ev) {

        final int action = ev.getAction();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final boolean pointerUp =
                (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? ev.getActionIndex() : -1;
        final boolean isGeneratedGesture =
                (ev.getFlags() & 0x8) != 0;

        // Determine focal point
        float sumX = 0, sumY = 0;
        final int count = ev.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (skipIndex == i) continue;
            sumX += ev.getX(i);
            sumY += ev.getY(i);
        }
        final int div = pointerUp ? count - 1 : count;
        final float focusX = sumX / div;
        final float focusY = sumY / div;

        boolean handled = false;

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                // Cancel long press and taps
                cancelTaps();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;

                // Check the dot product of current velocities.
                // If the pointer that left was opposing another velocity vector, clear.
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                final int upIndex = ev.getActionIndex();
                final int id1 = ev.getPointerId(upIndex);
                final float x1 = mVelocityTracker.getXVelocity(id1);
                final float y1 = mVelocityTracker.getYVelocity(id1);
                for (int i = 0; i < count; i++) {
                    if (i == upIndex) continue;

                    final int id2 = ev.getPointerId(i);
                    final float x = x1 * mVelocityTracker.getXVelocity(id2);
                    final float y = y1 * mVelocityTracker.getYVelocity(id2);

                    final float dot = x + y;
                    if (dot < 0) {
                        mVelocityTracker.clear();
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_DOWN:
                if (mDoubleTapListener != null) {
                    boolean hadTapMessage = mHandler.hasMessages(TAP);
                    if (hadTapMessage) mHandler.removeMessages(TAP);
                    if ((mCurrentDownEvent != null) && (mPreviousUpEvent != null) && hadTapMessage &&
                            isConsideredDoubleTap(mCurrentDownEvent, mPreviousUpEvent, ev)) {
                        // This is a second tap
                        mIsDoubleTapping = true;
                        // Give a callback with the first tap of the double-tap
                        handled |= mDoubleTapListener.onDoubleTap(mCurrentDownEvent);
                        // Give a callback with down event of the double-tap
                        handled |= mDoubleTapListener.onDoubleTapEvent(ev);
                    } else {
                        // This is a first tap
                        mHandler.sendEmptyMessageDelayed(TAP, DOUBLE_TAP_TIMEOUT);
                    }
                }

                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(ev);
                mAlwaysInTapRegion = true;
                mAlwaysInBiggerTapRegion = true;
                mStillDown = true;
                mInLongPress = false;
                mDeferConfirmSingleTap = false;

                if (mIsLongpressEnabled) {
                    mHandler.removeMessages(LONG_PRESS);
                    mHandler.sendEmptyMessageAtTime(LONG_PRESS,
                            mCurrentDownEvent.getDownTime() + LONGPRESS_TIMEOUT);
                }
                mHandler.sendEmptyMessageAtTime(SHOW_PRESS,
                        mCurrentDownEvent.getDownTime() + TAP_TIMEOUT);
                handled |= mListener.onDown(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mInLongPress || mInContextClick) {
                    break;
                }
                final float scrollX = mLastFocusX - focusX;
                final float scrollY = mLastFocusY - focusY;
                if (mIsDoubleTapping) {
                    // Give the move events of the double-tap
                    handled |= mDoubleTapListener.onDoubleTapEvent(ev);
                } else if (mAlwaysInTapRegion) {
                    final int deltaX = (int) (focusX - mDownFocusX);
                    final int deltaY = (int) (focusY - mDownFocusY);
                    int distance = (deltaX * deltaX) + (deltaY * deltaY);
                    int slopSquare = isGeneratedGesture ? 0 : mTouchSlopSquare;
                    if (distance > slopSquare) {
                        handled = mListener.onScroll(mCurrentDownEvent, ev, scrollX, scrollY);
                        mLastFocusX = focusX;
                        mLastFocusY = focusY;
                        mAlwaysInTapRegion = false;
                        mHandler.removeMessages(TAP);
                        mHandler.removeMessages(SHOW_PRESS);
                        mHandler.removeMessages(LONG_PRESS);
                    }
                    int doubleTapSlopSquare = isGeneratedGesture ? 0 : mDoubleTapTouchSlopSquare;
                    if (distance > doubleTapSlopSquare) {
                        mAlwaysInBiggerTapRegion = false;
                    }
                } else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                    handled = mListener.onScroll(mCurrentDownEvent, ev, scrollX, scrollY);
                    mLastFocusX = focusX;
                    mLastFocusY = focusY;
                }
                break;

            case MotionEvent.ACTION_UP:
                mStillDown = false;
                MotionEvent currentUpEvent = MotionEvent.obtain(ev);
                if (mIsDoubleTapping) {
                    // Finally, give the up event of the double-tap
                    handled |= mDoubleTapListener.onDoubleTapEvent(ev);
                } else if (mInLongPress) {
                    mHandler.removeMessages(TAP);
                    mInLongPress = false;
                } else if (mAlwaysInTapRegion && !mIgnoreNextUpEvent) {
                    handled = mListener.onSingleTapUp(ev);
                    if (mDeferConfirmSingleTap && mDoubleTapListener != null) {
                        mDoubleTapListener.onSingleTapConfirmed(ev);
                    }
                } else if (!mIgnoreNextUpEvent) {

                    // A fling must travel the minimum tap distance
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    final int pointerId = ev.getPointerId(0);
                    velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                    final float velocityY = velocityTracker.getYVelocity(pointerId);
                    final float velocityX = velocityTracker.getXVelocity(pointerId);

                    if ((Math.abs(velocityY) > mMinimumFlingVelocity)
                            || (Math.abs(velocityX) > mMinimumFlingVelocity)){
                        handled = mListener.onFling(mCurrentDownEvent, ev, velocityX, velocityY);
                        handled = mListener.onScrollUp(mCurrentDownEvent);
                    }
                    else
                    {
                        handled = mListener.onScrollUp(mCurrentDownEvent);
                    }
                }
                if (mPreviousUpEvent != null) {
                    mPreviousUpEvent.recycle();
                }
                // Hold the event we obtained above - listeners may have changed the original.
                mPreviousUpEvent = currentUpEvent;
                if (mVelocityTracker != null) {
                    // This may have been cleared when we called out to the
                    // application above.
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                mIsDoubleTapping = false;
                mDeferConfirmSingleTap = false;
                mIgnoreNextUpEvent = false;
                mHandler.removeMessages(SHOW_PRESS);
                mHandler.removeMessages(LONG_PRESS);
                break;

            case MotionEvent.ACTION_CANCEL:
                cancel();
                break;
        }

        return handled;
    }


   /* public boolean onGenericMotionEvent(MotionEvent ev) {
        if (mInputEventConsistencyVerifier != null) {
            mInputEventConsistencyVerifier.onGenericMotionEvent(ev, 0);
        }

        final int actionButton = ev.getActionButton();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_BUTTON_PRESS:
                if (mContextClickListener != null && !mInContextClick && !mInLongPress
                        && (actionButton == MotionEvent.BUTTON_STYLUS_PRIMARY
                        || actionButton == MotionEvent.BUTTON_SECONDARY)) {
                    if (mContextClickListener.onContextClick(ev)) {
                        mInContextClick = true;
                        mHandler.removeMessages(LONG_PRESS);
                        mHandler.removeMessages(TAP);
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_BUTTON_RELEASE:
                if (mInContextClick && (actionButton == MotionEvent.BUTTON_STYLUS_PRIMARY
                        || actionButton == MotionEvent.BUTTON_SECONDARY)) {
                    mInContextClick = false;
                    mIgnoreNextUpEvent = true;
                }
                break;
        }
        return false;
    }*/

    private void cancel() {
        mHandler.removeMessages(SHOW_PRESS);
        mHandler.removeMessages(LONG_PRESS);
        mHandler.removeMessages(TAP);
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mIsDoubleTapping = false;
        mStillDown = false;
        mAlwaysInTapRegion = false;
        mAlwaysInBiggerTapRegion = false;
        mDeferConfirmSingleTap = false;
        mInLongPress = false;
        mInContextClick = false;
        mIgnoreNextUpEvent = false;
    }

    private void cancelTaps() {
        mHandler.removeMessages(SHOW_PRESS);
        mHandler.removeMessages(LONG_PRESS);
        mHandler.removeMessages(TAP);
        mIsDoubleTapping = false;
        mAlwaysInTapRegion = false;
        mAlwaysInBiggerTapRegion = false;
        mDeferConfirmSingleTap = false;
        mInLongPress = false;
        mInContextClick = false;
        mIgnoreNextUpEvent = false;
    }

    private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp,
                                          MotionEvent secondDown) {
        if (!mAlwaysInBiggerTapRegion) {
            return false;
        }

        final long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
        if (deltaTime > DOUBLE_TAP_TIMEOUT || deltaTime < DOUBLE_TAP_MIN_TIME) {
            return false;
        }

        int deltaX = (int) firstDown.getX() - (int) secondDown.getX();
        int deltaY = (int) firstDown.getY() - (int) secondDown.getY();
        final boolean isGeneratedGesture =
                (firstDown.getFlags() & 0x8) != 0;

        int slopSquare = isGeneratedGesture ? 0 : mDoubleTapSlopSquare;
        return (deltaX * deltaX + deltaY * deltaY < slopSquare);
    }

    private void dispatchLongPress() {
        mHandler.removeMessages(TAP);
        mDeferConfirmSingleTap = false;
        mInLongPress = true;
        mListener.onLongPress(mCurrentDownEvent);
    }
}

