package com.baidu.paddle.lite.demo.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.baidu.paddle.lite.demo.OcrInfo;

import java.util.ArrayList;
import java.util.List;


public class PainterView extends View {
    private Paint mHandLinePaint;
    private Paint mHandPointPaint;
    private Object LOCK_ARMAX = new Object();
    private OcrInfo ocrInfos[];
    private Paint mMessagePaint = new Paint();

    public PainterView(Context context) {
        this(context, null);
    }

    public PainterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public PainterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHandPointPaint = getPointPaint(3f, "#FFFF0000");
        mHandLinePaint = getPointPaint(3f, "#FFFF0000");
        mMessagePaint.setColor(Color.rgb(00, 255, 60));
        mMessagePaint.setTextAlign(Paint.Align.CENTER);

    }
    public void clean(){
        ocrInfos = null;
    }
    public void setResult(OcrInfo[] infos){
        clean();
        ocrInfos = infos;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (LOCK_ARMAX){
            if(ocrInfos != null && ocrInfos.length> 0){
                for (OcrInfo ocrInfo : ocrInfos) {
                    drawHandPoints(canvas, ocrInfo.point);

                    drawTextInfo(canvas, ocrInfo);
                }
            }else{
                clean();
            }

        }

    }

    private void drawTextInfo(Canvas canvas, OcrInfo ocrInfo) {

        drawMessageInfoTemp(canvas, ocrInfo.point, ocrInfo.text);

    }

    private void drawMessageInfoTemp(Canvas canvas, Point[] point, String text) {

        canvas.save();
        canvas.translate((point[0].x + point[1].x)   / 2f, (point[0].y + point[1].y )  / 2f);
//        int  width = (point[1].x - point[0].x);
//        int  height = (point[1].y - point[0].y);
        mMessagePaint.setTextSize(20);
        canvas.drawText(text, 0, -20, mMessagePaint);
        canvas.restore();


    }

    private void drawHandPoints(Canvas canvas, Point[] points) {
        if (points == null || points.length == 0) {
            return;
        }
        drawHandPointLine(canvas, mHandLinePaint, points);
    }

    protected Paint getPointPaint(Float strokeWidth, String colorString) {
        Paint pointPaintColor = new Paint();
        pointPaintColor.setColor(Color.parseColor(colorString));
        pointPaintColor.setStyle(Paint.Style.STROKE);
        pointPaintColor.setStrokeCap(Paint.Cap.ROUND);
        pointPaintColor.setAntiAlias(true);
        pointPaintColor.setStrokeWidth(strokeWidth);
        return pointPaintColor;
    }

    private void drawHandPointLine(Canvas canvas, Paint mRectanglePaint, Point[] handPoints) {
        for (int i = 0; i < handPoints.length - 1; i++) {
            canvas.drawLine(handPoints[i].x, handPoints[i].y, handPoints[i + 1].x, handPoints[i + 1].y, mRectanglePaint);
        }
        canvas.drawLine(handPoints[0].x, handPoints[0].y, handPoints[handPoints.length - 1].x, handPoints[handPoints.length - 1].y, mRectanglePaint);
    }

}
