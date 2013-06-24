package com.seeedstudio.smartmeter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.seeedstudio.smartmeter.ControllerWheelModel.onControllerWheelListener;

public class ControllerWheel extends View implements onControllerWheelListener {

    // int screenWidth = 0, screenHeight = 0;
    // //////// variable //////
//    public interface onControllerTouch {
//        boolean onItemTouch();
//    }

    public static final int STATE_V = 0;
    public static final int STATE_O = 1;
    public static final int STATE_mA = 2;
    public static final int STATE_A = 3;
    public static final int STATE_NONE = 4;

    private Paint mNeedleLeftPaint, mNeedleRightPaint, mBackgroundPaint,
            mUnitPaint;
    private Path mNeedleLeftPath, mNeedleRightPath;
    private float mNeedleWidth = 100f;
    private Bitmap background, foreground;// must be refactor by 9png
    private Bitmap v, om, ma, a;// unit bitmap, or using typeface ?
    private Bitmap v_press, om_press, ma_press, a_press;
    private Rect fgRect, bgRect, vRect, omRect, maRect, aRect;
    private float degree = 0.0f;// angle degree
    private float c_degree = 0.0f;
    private float vol_degree, om_degree;
    private float radY = 0.0f;
    private int screenWidth = 400, screenHeight = 800;
    private int viewWidth, viewHeight;// get the view size without the title bar
                                      // and virtual key bar
    private float x2, y2, x, y;
    private float width12; // viewWidth/12，长度分成12份。
    private float forgegroundHeight, backgroundHeight;// 计算半径
    private float emptyHeight; // viewHeight - backgroundHeight
    private boolean NeedleShow = true;
    private boolean isV = false, ismA = false, isA = false, isom = false;
    private int mState = STATE_NONE;

    private ControllerWheelModel mModel;

    public ControllerWheel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ControllerWheel(Context context) {
        super(context);
        init();
    }

    private void init() {

        setModel(new ControllerWheelModel());

        // get fg and bg bitmap
        BitmapDrawable foregroundDrawable = (BitmapDrawable) getContext()
                .getResources().getDrawable(R.drawable.logo);
        BitmapDrawable backgroundDrawable = (BitmapDrawable) getContext()
                .getResources().getDrawable(R.drawable.front);
        background = backgroundDrawable.getBitmap();
        foreground = foregroundDrawable.getBitmap();

        // get unit bitmap
        BitmapDrawable vD = (BitmapDrawable) getContext().getResources()
                .getDrawable(R.drawable.v);
        BitmapDrawable omD = (BitmapDrawable) getContext().getResources()
                .getDrawable(R.drawable.om);
        BitmapDrawable maD = (BitmapDrawable) getContext().getResources()
                .getDrawable(R.drawable.ma);
        BitmapDrawable aD = (BitmapDrawable) getContext().getResources()
                .getDrawable(R.drawable.a);
        BitmapDrawable vDPress = (BitmapDrawable) getContext().getResources()
                .getDrawable(R.drawable.v_press);
        BitmapDrawable omDPress = (BitmapDrawable) getContext().getResources()
                .getDrawable(R.drawable.om_press);
        BitmapDrawable maDPress = (BitmapDrawable) getContext().getResources()
                .getDrawable(R.drawable.ma_press);
        BitmapDrawable aDPress = (BitmapDrawable) getContext().getResources()
                .getDrawable(R.drawable.a_press);
        v = vD.getBitmap();
        om = omD.getBitmap();
        ma = maD.getBitmap();
        a = aD.getBitmap();

        v_press = vDPress.getBitmap();
        om_press = omDPress.getBitmap();
        ma_press = maDPress.getBitmap();
        a_press = aDPress.getBitmap();

        Log.d("SMTest", "view heigth: " + x + ", weight: " + y);

        setupAll();

    }

    public void setModel(ControllerWheelModel controllerWheelModel) {
        this.mModel = controllerWheelModel;
        if (this.mModel != null) {
            this.mModel.removeListener(this);
        }

        this.mModel = controllerWheelModel;
        if (controllerWheelModel != null) {
            controllerWheelModel.addListener(this);
        }
    }

    public ControllerWheelModel getModel() {
        return mModel;
    }

    // ///////////////////////////////////////////////////////////////////////
    // getter and setter
    // ///////////////////////////////////////////////////////////////////////

    private float getRad(float bitmapWidth, float bitmapHeight) {
        float w = bitmapWidth;
        float h = bitmapHeight;

        float y = (h * h - (w * w / 4)) / (2 * h);
        Log.d("SMTest View", "rad: " + y);
        // y = viewHeight + Math.abs(y) - forgegroundHeight;
        y = viewHeight + Math.abs(y);
        return y; // 得到半径的 y 座标。
    }

    /**
     * When the bitmap width full the View width, get the scale bitmap height
     * 
     * @param bitmapWidth
     * @param bitmapHeight
     * @param scalaX
     * @param scalaY
     * @return a float array about [width,height]
     */
    private float[] scalaBitmapFixScreen(float bitmapWidth, float bitmapHeight,
            float scalaWidth, float scalaHeight) {

        Log.d("SMTest view", "scalaBitmapFixScreen()," + bitmapWidth + ","
                + bitmapHeight + "," + scalaWidth + "," + scalaHeight);
        float x = scalaWidth / bitmapWidth;
        float targetHeight = 0.0f;
        float targetWidth = 0.0f;

        // if (x > 1) {
        // targetHeight = bitmapHeight * x;
        // } else {
        // targetHeight = bitmapHeight;
        // }
        targetHeight = bitmapHeight * x;

        float[] r = new float[] { targetWidth, targetHeight };
        return r;
    }

    public float getDegree() {
        return degree;
    }

    public void setDegree(float degree) {
        this.degree = degree;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public int getmState() {
        return mState;
    }

    public void setmState(int mState) {
        this.mState = mState;
    }

    // ///////////////////////////////////////////////////////////////////////
    // Init setup method
    // ///////////////////////////////////////////////////////////////////////

    private final void setAllWidthAndHeight() {
        viewWidth = this.getWidth();
        viewHeight = this.getHeight();
        x2 = viewWidth / 2;
        y2 = viewHeight / 2;
        width12 = viewWidth / 12;
        mNeedleWidth = viewWidth / 6;
//        forgegroundHeight = scalaBitmapFixScreen(foreground.getWidth(),
//                foreground.getHeight(), getScreenWidth(), getScreenHeight())[1];
//        backgroundHeight = scalaBitmapFixScreen(background.getWidth(),
//                background.getHeight(), getScreenWidth(), getScreenHeight())[1];
        emptyHeight = viewHeight - backgroundHeight;
        radY = getRad(background.getWidth(), backgroundHeight
                - forgegroundHeight);
        setupRect();
        setupAngel();
    }

    private void setupAngel() {
        float r_y = radY - viewHeight;
        float targetX = width12 - x2 + v.getWidth();
        float targetY = viewHeight - emptyHeight + r_y;
        vol_degree = (float) (((Math.tan(targetX / targetY) * 180) / Math.PI));
        Log.d("SMTest Unit Angel", "V angel: " + c_degree);

        targetX = 4 * width12 + om.getWidth() - x2;
        targetY = viewHeight - emptyHeight - om.getHeight() + r_y;
        om_degree = (float) (((Math.tan(targetX / targetY) * 180) / Math.PI));
        Log.d("SMTest Unit Angel", "Om angel: " + c_degree);

        vol_degree = Math.abs(vol_degree);
        om_degree = Math.abs(om_degree);
        // targetX = 7 * width12 + ma.getWidth() - x2;
        // targetY = viewHeight - emptyHeight - om.getHeight() + r_y;
        // c_degree = (float) (((Math.tan(targetX / targetY) * 180) / Math.PI));
        // Log.d("SMTest Unit Angel", "mA angel: " + c_degree);
        //
        // targetX = 10 * width12 + a.getWidth() - x2;
        // targetY = viewHeight - emptyHeight + r_y;
        // c_degree = (float) (((Math.tan(targetX / targetY) * 180) / Math.PI));
        // Log.d("SMTest Unit Angel", "A angel: " + c_degree);
    }

    private final void setupAll() {

        mNeedleLeftPaint = getDefaultNeedleLeftPaint();
        mNeedleRightPaint = getDefaultNeedleRightPaint();

        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setFilterBitmap(true);

        mUnitPaint = new Paint();
        mUnitPaint.setColor(Color.YELLOW);
        
        //////////////setup the scale ///////////////
        forgegroundHeight = scalaBitmapFixScreen(foreground.getWidth(),
                foreground.getHeight(), getScreenWidth(), getScreenHeight())[1];
        backgroundHeight = scalaBitmapFixScreen(background.getWidth(),
                background.getHeight(), getScreenWidth(), getScreenHeight())[1];
    }

    private void setupRect() {
        bgRect = new Rect(0, (int) (emptyHeight), viewWidth, viewHeight);

        fgRect = new Rect(0, (int) (viewHeight - forgegroundHeight), viewWidth,
                viewHeight);

    }

    private final void setDefaultNeedlePaths() {

        setAllWidthAndHeight();

        mNeedleLeftPath = new Path();
        mNeedleLeftPath.moveTo(x2, viewHeight); // 设置轮廓起始点位置，view 的底端中心点
        mNeedleLeftPath.lineTo(x2, emptyHeight + 30); // 上移
        mNeedleLeftPath.lineTo(x2 - mNeedleWidth, viewHeight); // 左移

        mNeedleRightPath = new Path();
        mNeedleRightPath.moveTo(x2, viewHeight);
        mNeedleRightPath.lineTo(x2, emptyHeight + 30);
        mNeedleRightPath.lineTo(x2 + mNeedleWidth, viewHeight);// 右移
    }

    public Paint getDefaultNeedleLeftPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(0, 100, 19));
        return paint;
    }

    public Paint getDefaultNeedleRightPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(0, 80, 30));
        paint.setShadowLayer(0.01f, 0.005f, -0.005f, Color.argb(127, 0, 0, 0));
        return paint;
    }

    // ///////////////////////////////////////////////////////////////////////
    // View life cycle
    // ///////////////////////////////////////////////////////////////////////

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(final Canvas canvas) {
        Log.d("SMTest onDraw()", "");

        // canvas 抗锯齿
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG
                | Paint.FILTER_BITMAP_FLAG));

        drawBG(canvas);
        if (NeedleShow) {
            drawNeedle(canvas);
        }
        drawFG(canvas);
        drawUnit(canvas);
        // drawUnitLine(canvas);
        // drawMyRect(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("SMTest view onSizeChanged(int w, int h, int oldw, int oldh)", w
                + "," + h + "," + oldw + "," + oldh);
        super.onSizeChanged(w, h, oldw, oldh);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d("SMTest view onMeasure", "");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setDefaultNeedlePaths();
        // setMeasuredDimension(widthMeasureSpec,
        // background.getHeight() + om.getHeight());
        /*
         * 如果是AT_MOST，specSize 代表的是最大可获得的空间；对应的是 wrap_content 布局参数
         * 如果是EXACTLY，specSize 代表的是精确的尺寸；对应的是 fill_parent 布局参数
         * 如果是UNSPECIFIED，对于控件尺寸来说，没有任何参考意义。
         */
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        //
        final int chosenWidth = chooseDimension(widthMode, widthSize);
        final int chosenHeight = chooseDimension(heightMode, heightSize);
        setMeasuredDimension(chosenWidth, chosenHeight);
    }

    private int chooseDimension(final int mode, final int size) {
        switch (mode) {
        case View.MeasureSpec.AT_MOST:
        case View.MeasureSpec.EXACTLY:
            return size;
        case View.MeasureSpec.UNSPECIFIED:
        default:
            return 0;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        Log.d("SMTest view onLayout", this.getWidth() + ", " + this.getHeight());
    }

    // ///////////////////////////////////////////////////////////////////////
    // draw method
    // ///////////////////////////////////////////////////////////////////////

    private void drawUnitLine(Canvas canvas) {
        canvas.drawLine(width12, om.getHeight() + emptyHeight, x2, radY,
                mUnitPaint);
        canvas.drawLine(viewWidth / 3, emptyHeight, x2, radY, mUnitPaint);
        canvas.drawLine(viewWidth / 12 * 7, emptyHeight, x2, radY, mUnitPaint);
        canvas.drawLine(viewWidth / 12 * 10, om.getHeight() + emptyHeight, x2,
                radY, mUnitPaint);

        double xy = (x2 - viewWidth / 3 - om.getWidth() / 2) / radY;
        double a = Math.tan(Math.abs(xy));
        Log.d("SMTest view", "angel arc: " + a);
        Log.d("SMTest view", "angel : " + (float) ((a * 180) / Math.PI));
    }

    private void drawUnit(final Canvas canvas) {
        flipUnitState();
        if (isV) {
            canvas.drawBitmap(v_press, width12, emptyHeight, mBackgroundPaint);
        } else {
            canvas.drawBitmap(v, width12, emptyHeight, mBackgroundPaint);
        }

        if (isom) {
            canvas.drawBitmap(om_press, 4 * width12,
                    emptyHeight - om.getHeight(), mBackgroundPaint);
        } else {
            canvas.drawBitmap(om, 4 * width12, emptyHeight - om.getHeight(),
                    mBackgroundPaint);
        }

        if (ismA) {
            canvas.drawBitmap(ma_press, 7 * width12,
                    emptyHeight - ma.getHeight(), mBackgroundPaint);
        } else {
            canvas.drawBitmap(ma, 7 * width12, emptyHeight - ma.getHeight(),
                    mBackgroundPaint);
        }

        if (isA) {
            canvas.drawBitmap(a_press, 10 * width12, emptyHeight,
                    mBackgroundPaint);
        } else {
            canvas.drawBitmap(a, 10 * width12, emptyHeight, mBackgroundPaint);
        }
    }

    private void drawFG(final Canvas canvas) {
        canvas.drawBitmap(foreground, null, fgRect, mBackgroundPaint);
    }

    private void drawBG(final Canvas canvas) {
        canvas.drawBitmap(background, null, bgRect, mBackgroundPaint);
    }

    private void drawNeedle(final Canvas canvas) {
        final float angle = getDegree();

        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate(angle, x2, radY);// 设置圆心位置

        // draw path
        canvas.drawPath(mNeedleLeftPath, mNeedleLeftPaint);
        canvas.drawPath(mNeedleRightPath, mNeedleRightPaint);

        canvas.restore();
    }

    private void drawMyRect(final Canvas canvas) {
        Rect r = new Rect((int) width12, (int) (emptyHeight - om.getHeight()),
                (int) (viewWidth - width12),
                (int) (viewHeight - forgegroundHeight / 2));
        // Rect r = new Rect((int) width12, om.getHeight(), (int) (2 * width12),
        // (int) (2 * width12));
        canvas.drawRect(r, mUnitPaint);
    }

    // ///////////////////////////////////////////////////////////////////////
    // Gesture and touch
    // ///////////////////////////////////////////////////////////////////////

    /**
     * 计算以(src_x,src_y)为坐标圆点，建立直角体系，求出(target_x,target_y)坐标与x轴的夹角
     * 主要是利用反正切函数的知识求出夹角
     * 
     * @param src_x
     * @param src_y
     * @param target_x
     * @param target_y
     * @return
     */
    float detaDegree(float src_x, float src_y, float target_x, float target_y) {

        float detaX = target_x - src_x;
        float detaY = target_y - src_y;
        double d;
        // 坐标在四个象限里
        if (detaX != 0) {
            float tan = Math.abs(detaY / detaX);

            if (detaX > 0) {

                // 第一象限
                if (detaY >= 0) {
                    d = Math.atan(tan);

                } else {
                    // 第四象限
                    d = 2 * Math.PI - Math.atan(tan);
                }

            } else {
                if (detaY >= 0) {
                    // 第二象限
                    d = Math.PI - Math.atan(tan);
                } else {
                    // 第三象限
                    d = Math.PI + Math.atan(tan);
                }
            }

        } else {
            // 坐标在y轴上
            if (detaY > 0) {
                // 坐标在y>0上
                d = Math.PI / 2;
            } else {
                // 坐标在y<0上
                d = -Math.PI / 2;
            }
        }

        float r = (float) ((d * 180) / Math.PI);
        Log.d("SMTest detaDegree", r + "");
        return r;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Let the GestureDetector interpret this event

        float down_x, down_y;
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            down_x = event.getX();
            down_y = event.getY();
            // Log.d("SMTest onTouch ACTION_DOWN", "down_x: " + down_x
            // + ", down_y: " + down_y);
            // Log.d("SMTest", "radY: " + radY);
            flushUnitState();
            float targetX = down_x - x2;
            float targetY = viewHeight - down_y + y;
            degree = (float) (((Math.tan(targetX / targetY) * 180) / Math.PI));
        case MotionEvent.ACTION_MOVE:
            down_x = event.getX();
            down_y = event.getY();
            dealWithNeedleAnimation(down_x, down_y);
            break;
        case MotionEvent.ACTION_UP:
            down_x = event.getX();
            down_y = event.getY();
            // Log.d("SMTest onTouch ACTION_UP", "down_x: " + down_x
            // + ", down_y: " + down_y);
            autoCentrel(down_x - x2, viewHeight - down_y + y);
            break;
        default:
            break;
        }
        return true;
    }

    private void flushUnitState() {
        ismA = false;
        isA = false;
        isom = false;
        isV = false;
    }

    private void autoCentrel(float targetX, float targetY) {
        if (1 <= c_degree && c_degree <= om_degree * 2) {
            setDegree(om_degree);
            ismA = true;
            setmState(STATE_mA);
        } else if (om_degree * 2 < c_degree && c_degree <= 100) {
            setDegree(vol_degree);
            isA = true;
            setmState(STATE_A);
        } else if (-100 <= c_degree && c_degree <= -om_degree * 2) {
            setDegree(-vol_degree);
            isV = true;
            setmState(STATE_V);
        } else if (-om_degree * 2 < c_degree && c_degree <= 0) {
            setDegree(-om_degree);
            isom = true;
            setmState(STATE_O);
        }
        flipUnitState();
        invalidate();
    }

    private void flipUnitState() {
        if (ismA) {
            isA = false;
            isom = false;
            isV = false;
        }

        if (isA) {
            ismA = false;
            isom = false;
            isV = false;
        }

        if (isom) {
            isA = false;
            ismA = false;
            isV = false;
        }

        if (isV) {
            isA = false;
            isom = false;
            ismA = false;
        }

        Log.d("SMTest flipUnitState()", "isV: " + isV + ", isom :" + isom
                + ", ismA: " + ismA + ", isA: " + isA);
    }

    private void dealWithNeedleAnimation(float posX, float posY) {

        float y = radY - viewHeight;
        float targetX = posX - x2;
        float targetY = viewHeight - posY + y;
        if (width12 <= posX && posX <= (viewWidth - width12)) {
            if (emptyHeight - om.getHeight() <= posY
                    && posY <= (viewHeight - forgegroundHeight / 2)) {

                // 得到 degree，对边比斜边。。。。
                c_degree = (float) (((Math.tan(targetX / targetY) * 180) / Math.PI));
                // 滑过的弧度增量
                float dete = c_degree - degree;
                // 如果小于-90度说明 它跨周了，需要特殊处理350->17,
                if (dete < -270) {
                    dete = dete + 360;
                    // 如果大于90度说明 它跨周了，需要特殊处理-350->-17,
                } else if (dete > 270) {
                    dete = dete - 360;
                }

                Log.d("SMTest dealWithNeedleAnimation", "detaDegree: " + dete
                        + ", c_degreee: " + c_degree);

                setDegree(dete);
                degree = c_degree;
                invalidate();
            }

        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // for listener
    // ///////////////////////////////////////////////////////////////////////


    @Override
    public void wheelChangeVmode(boolean isV) {

    }

    @Override
    public void wheelChangeOmmode(boolean isom) {

    }

    @Override
    public void wheelChangemAmode(boolean ismA) {

    }

    @Override
    public void wheelChangeAmode(boolean isA) {

    }

    @Override
    public void onItemTouch() {
//        return getmState();
    }

    @Override
    public int getState() {
        return getmState();
    }

}
