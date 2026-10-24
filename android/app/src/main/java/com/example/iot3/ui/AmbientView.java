package com.example.iot3.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.iot3.R;

/** Small botanical sensor illustration, drawn as vectors at any screen density. */
public final class AmbientView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path leaf = new Path();

    public AmbientView(Context context, AttributeSet attributes) {
        super(context, attributes);
        leaf.moveTo(34, 74);
        leaf.cubicTo(17, 43, 46, 32, 73, 30);
        leaf.cubicTo(76, 61, 62, 83, 34, 74);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(getWidth() / 100f, getHeight() / 110f);
        int mint = ContextCompat.getColor(getContext(), R.color.mint);
        paint.setColor(mint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.7f);
        paint.setAlpha(45);
        for (int radius = 20; radius <= 46; radius += 13) {
            canvas.drawCircle(50, 55, radius, paint);
        }
        canvas.drawLine(3, 55, 97, 55, paint);
        canvas.drawLine(50, 8, 50, 102, paint);
        paint.setAlpha(255);
        paint.setStrokeWidth(1.5f);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(50);
        canvas.drawPath(leaf, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(255);
        canvas.drawPath(leaf, paint);
        canvas.drawLine(26, 85, 64, 42, paint);
        canvas.drawLine(46, 63, 45, 47, paint);
        canvas.drawLine(46, 63, 62, 62, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(90, 35, 3, paint);
        canvas.drawCircle(18, 83, 2, paint);
        canvas.restore();
    }
}
