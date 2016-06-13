package com.tcl.downloader.sample.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.tcl.downloader.sample.R;

import org.aisen.android.common.context.GlobalContext;
import org.aisen.android.common.utils.Utils;


/**
 * the download action button
 *
 * @author yjgao
 */
public class ProgressButton extends TextView {
    private int STROKE_WIDTH = 1;
    private boolean isProgressState = false;

    private int mProgressNormalColor;
    private int mProgressPressColor;
    private int mProgressBgColor;
    private int mProgressStrokeColor;

    private Paint mBackgroundPaint;
    private Paint mProgressPaint;
    private Paint mProgressStrokePaint;

    private double unitlength;
    private int progress = 0;

    private int radis = 5;

    private boolean isBorder = true;

    private boolean mTouchDown = false;

    private Path clipPath;

    public ProgressButton(Context context) {
        super(context);

        init(context);
    }

    public ProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        mProgressStrokeColor = context.getResources().getColor(R.color.download_btn_border);

        mBackgroundPaint = new Paint();
        mProgressBgColor = context.getResources().getColor(R.color.download_btn_normal);
        mBackgroundPaint.setColor(mProgressBgColor);

        mProgressNormalColor = context.getResources().getColor(R.color.download_btn_progress_pause);
        mProgressPressColor = context.getResources().getColor(R.color.download_btn_progress);

        mProgressPaint = new Paint();
        mProgressPaint.setColor(mProgressNormalColor);

        mProgressStrokePaint = new Paint();

        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setAntiAlias(true);

        mProgressPaint.setStyle(Paint.Style.FILL);
        mProgressPaint.setAntiAlias(true);

        mProgressStrokePaint.setStyle(Paint.Style.STROKE);
        mProgressStrokePaint.setAntiAlias(true);
        mProgressStrokePaint.setStrokeWidth(Utils.dip2px(GlobalContext.getInstance(), STROKE_WIDTH));
        mProgressStrokePaint.setColor(mProgressStrokeColor);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mTouchDown = true;
                setPressColor(mProgressNormalColor);
            }
            break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_UP: {
                mTouchDown = false;
                setNormalColor(mProgressNormalColor);
            }
            break;

        }
        return super.onTouchEvent(event);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        try {
            if (unitlength == 0)
                unitlength = this.getWidth() / 100.0;

            radis = getHeight() / 2;

            int border = 0;
            if (isBorder) {
                border = Utils.dip2px(GlobalContext.getInstance(), STROKE_WIDTH);
            }
            if (isProgressState && !mTouchDown) {
                // clipPath
                if (clipPath == null && getWidth() > 0 && getHeight() > 0) {
                    clipPath = new Path();
                    RectF tmp = new RectF(border, border, getWidth() - border, getHeight() - border);
                    clipPath.addRoundRect(tmp, radis, radis, Path.Direction.CW);
                }
                if (clipPath != null) {
                    canvas.clipPath(clipPath);
                }

                // 进度条长度
                double progressWidth = unitlength * progress;
                if (progressWidth >= this.getWidth() - border) {
                    progressWidth = this.getWidth() - border;
                }
                RectF progressRect = new RectF(border,
                        border,
                        (int) (border + progressWidth),
                        getHeight() - border);

                // 背景的左侧
                double backgroundLeft = unitlength * progress - radis * 2;
                backgroundLeft = (float) (backgroundLeft < border ? border : backgroundLeft);
                RectF backgroundRect = new RectF((float) backgroundLeft, border, this.getWidth() - border, this.getHeight() - border);

                // 先画背景
                canvas.drawRoundRect(backgroundRect, 0, 0, mBackgroundPaint);
                // 再画进度条
                canvas.drawRoundRect(progressRect, 0, 0, mProgressPaint);
                // 画进度
                canvas.drawText(progress + "%", 0, 0, mBackgroundPaint);
            }
            super.onDraw(canvas);

            // 画边框
//            if (isBorder && !mTouchDown) {
//                Rect boarderR = canvas.getClipBounds();
//
//                Rect tmp = new Rect(boarderR.left + border,
//                                    boarderR.top + border,
//                                    boarderR.right - border,
//                                    boarderR.bottom - border);
//                if (mProgressStrokeColor != 0) {
//                    mProgressStrokePaint.setColor(mProgressStrokeColor);
//                } else {
//                    mProgressStrokePaint.setColor(
//                                    getContext().getResources().getColor(R.color.transparent_bg));
//                }
//                canvas.drawRoundRect(new RectF(tmp), radis, radis, mProgressStrokePaint);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Deprecated
    public void setBackgroundDrawable(Drawable background) {
        if (!mTouchDown) {
//            super.setBackgroundDrawable(background);
            super.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_download));
        }
    }

    public void setBackgroundDrawable() {
        if (!mTouchDown) {
//            super.setBackgroundDrawable(background);
            super.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_download));
        }
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        if (!mTouchDown) {
            super.setTextColor(colors);
        }
    }

    /***
     * rgb not res color id
     */
    public void setNormalColor(int color) {
        mProgressNormalColor = color;
        mProgressPaint.setColor(mProgressNormalColor);
        if (!mTouchDown) {
            invalidate();
        }
    }

    /***
     * rgb not res color id
     */
    public void setPressColor(int color) {
        this.mProgressPressColor = color;
        mProgressPaint.setColor(mProgressPressColor);
        if (!mTouchDown) {
            invalidate();
        }
    }

    /***
     * rgb not res color id
     */
    public void setRightBgColor(int color) {
        this.mProgressBgColor = color;
        mBackgroundPaint.setColor(mProgressBgColor);
        if (!mTouchDown) {
            invalidate();
        }
    }

    public void setStrokeColor(int color) {
//        this.mProgressStrokeColor = color;
//        mProgressStrokePaint.setColor(mProgressStrokeColor);
//        if (!mTouchDown) {
//            invalidate();
//        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
        if (!mTouchDown) {
            postInvalidate();
        }

    }

    public boolean isProgressState() {
        return isProgressState;
    }

    public void setProgressState(boolean isProgressState) {
        this.isProgressState = isProgressState;
    }

    public void setRoundRadis(int radis) {
        this.radis = radis;
    }

    public boolean isBorder() {
        return isBorder;
    }

    public void setBorder(boolean isBorder) {
//        this.isBorder = isBorder;
    }

    @Override
    public void setPressed(boolean pressed) {
        // If the parent is pressed, do not set to pressed.
        if (pressed && ((View) getParent()).isPressed()) {
            return;
        }
        super.setPressed(pressed);
    }

}
