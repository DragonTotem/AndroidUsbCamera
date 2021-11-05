package com.jiangdg.usbcamera.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;

import androidx.annotation.Nullable;

/**
 * @author created by knight
 * @organize
 * @Date 2019/10/11 13:54
 * @descript:人脸框
 */

public class FaceDeteView extends View {

    private Paint mPaint;
    private String mColor = "#42ed45";
    private ArrayList<RectF> mFaces = null;
    public FaceDeteView(Context context) {
        super(context);
        init(context);
    }

    public FaceDeteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FaceDeteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }



    private void init(Context context){
        mPaint = new Paint();
        //画笔颜色
        mPaint.setColor(Color.parseColor(mColor));
        //只绘制图形轮廓
        mPaint.setStyle(Paint.Style.STROKE);
        //设置粗细
        mPaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,1f,context.getResources().getDisplayMetrics()));
        //设置抗锯齿
        mPaint.setAntiAlias(true);
    }


    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(mFaces != null){
            for(RectF face:mFaces){
                canvas.drawRect(face,mPaint);
            }

        }
    }


    /**
     * 设置人人脸信息
     */
    public void setFace(ArrayList<RectF> mFaces){
       this.mFaces = mFaces;
       //重绘矩形框
       invalidate();
    }

}
