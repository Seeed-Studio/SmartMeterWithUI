package com.seeedstudio.smartmeter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class LCDView extends View implements LCDScreen.onLCDUpdateListener {
    // debugging
    private static final boolean D = true;
    private static final String TAG = "LCDView";

    // //////////////////////////////////////////////////////////////////////////
    // field
    // //////////////////////////////////////////////////////////////////////////

    // 水平间距
    private static final int HORIZONTAL_MARGIN = 68;
    // 垂直间距
    private static final float VERTICAL_MARGIN = 50.0f;
    // 左边缘间距
    private static final float LEFT_MARGIN = 58.0f;
    // 右边缘间距
    private static final float RIGHT_MARGIN = 60.0f;
    // background image
    private NinePatchDrawable mBackgroundNinePatch;
    // typeface lcd
    private static Typeface mLCDTypeFace, mLCDHoldTypeFace, mLCDUnitTypeFace;
    // data paint
    private Paint dataPaint, holdPaint, unitPaint;
    // LCD model
    private LCDScreen mLCDModel;

    // something to paint
    String isHoldData = "", isHoldUnit = "";
    String mUnit = ""; // default unit is ""

    // hold mode flag and switch
    private boolean isHold = false, isTurn = true;

    public LCDView(Context context) {
        super(context);
        init();
    }

    public LCDView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        // set model
        setLCDModel(new LCDScreen());

        // get the background resource
        Resources r = getContext().getResources();
        mBackgroundNinePatch = (NinePatchDrawable) r
                .getDrawable(R.drawable.panel);

        // set up typeface
        if (mLCDTypeFace == null) {
            mLCDTypeFace = Typeface.createFromAsset(getContext().getAssets(),
                    "ds_digital.ttf");
        }

        if (mLCDHoldTypeFace == null) {
            mLCDHoldTypeFace = Typeface.createFromAsset(getContext()
                    .getAssets(), "ds_digitalb.ttf");
            // Typeface.create("times new roman", Style.NORMAL);
        }

        if (mLCDUnitTypeFace == null) {
            mLCDUnitTypeFace = Typeface
                    .create("times new roman", Typeface.BOLD);
        }

        // set up data paint
        dataPaint = new Paint();
        dataPaint.setColor(0xFF000000);
        dataPaint.setTypeface(mLCDTypeFace); // 设置排版字体
        dataPaint.setAntiAlias(true); // 设置抗锯齿
        // dataPaint.setTextAlign(Paint.Align.RIGHT);

        holdPaint = new Paint();
        holdPaint.setColor(0xFF000000);
        holdPaint.setTypeface(mLCDHoldTypeFace); // 设置排版字体
        holdPaint.setAntiAlias(true); // 设置抗锯齿
        holdPaint.setTextAlign(Paint.Align.LEFT);

        unitPaint = new Paint();
        unitPaint.setColor(0xFF000000);
        unitPaint.setTypeface(mLCDUnitTypeFace);
        unitPaint.setAntiAlias(true); // 设置抗锯齿
        unitPaint.setTextAlign(Paint.Align.RIGHT);
    }

    private void setLCDModel(LCDScreen lcdModel) {
        if (this.mLCDModel != null) {
            this.mLCDModel.removeListener(this);
        }

        this.mLCDModel = lcdModel;
        if (lcdModel != null) {
            lcdModel.addListener(this);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Rect clip = canvas.getClipBounds();
        int width = clip.width();
        int height = clip.height();

        mBackgroundNinePatch.setBounds(0, 0, width - 1, height - 1);
        mBackgroundNinePatch.draw(canvas);

        if (!isTurn)
            return;

        float ascent = dataPaint.ascent();
        dataPaint.setTextAlign(Paint.Align.RIGHT);

        if (!isHold) {

            // data
            String temp = String.valueOf(mLCDModel.getData());
            canvas.drawText(temp, 0, temp.length(), width - HORIZONTAL_MARGIN,
                    (height - dataPaint.getTextSize()) / 4 - ascent, dataPaint);

            // unit
            canvas.drawText(mUnit, 0, mUnit.length(), width - RIGHT_MARGIN,
                    (height - unitPaint.getTextSize() / 2 - 5), unitPaint);

            isHoldData = temp;
            isHoldUnit = mUnit;
        } else {

            // data
            canvas.drawText(isHoldData, 0, isHoldData.length(), width
                    - HORIZONTAL_MARGIN, (height - dataPaint.getTextSize()) / 4
                    - ascent, dataPaint);

            // unit
            canvas.drawText(isHoldUnit, 0, mUnit.length(),
                    width - RIGHT_MARGIN,
                    (height - unitPaint.getTextSize() / 2 - 5), unitPaint);

            // hold
            canvas.drawText("HOLD", 0, 4, LEFT_MARGIN,
                    (height - holdPaint.getTextSize() / 2), holdPaint);
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = 100;
        }

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = 100;
        }

        setMeasuredDimension(widthSize, heightSize);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 得到当前的去掉垂直间距后的高
        float textHeight = h - VERTICAL_MARGIN;
        dataPaint.setTextSize(textHeight);
        holdPaint.setTextSize(VERTICAL_MARGIN - 10);
        unitPaint.setTextSize(VERTICAL_MARGIN - 15);

        // 得一个大于本身的最近整数（非四舍五入）
        int width = (int) Math.floor((w - HORIZONTAL_MARGIN)
                / dataPaint.measureText(" "));

        mLCDModel.onUpdateWidth(width);
    }

    public LCDScreen getModel() {
        return mLCDModel;
    }

    @Override
    public void lcdWidthChanged(int newWidth) {

    }

    @Override
    public void lcdDataUpdated() {
        if (D)
            Log.d(TAG, "lcdDataUpdated()");
        invalidate();
    }

    @Override
    public void lcdHoldMode(boolean isHold) {
        this.isHold = isHold;
    }

    @Override
    public void lcdChangeUnitMode(String unit) {
        this.mUnit = unit;
        if (mUnit.equals("mDefault")) {
            mUnit = "";
        }
    }

    @Override
    public void lcdSwitch(boolean isTurn) {
        this.isTurn = isTurn;
    }

}
