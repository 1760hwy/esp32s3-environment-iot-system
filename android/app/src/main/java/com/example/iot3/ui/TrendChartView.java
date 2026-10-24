package com.example.iot3.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.iot3.R;
import com.example.iot3.data.EnvironmentData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Native chart: real timestamps on X, monotone cubic segments, no external chart SDK. */
public final class TrendChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path line = new Path();
    private final Path area = new Path();
    private final SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.CHINA);
    private final Date date = new Date();
    private LinearGradient gradient;
    private final List<EnvironmentData.Sample> samples = new ArrayList<>();
    private int metric;
    private int selected = -1;
    private float minimum;
    private float maximum;
    private float left;
    private float right;
    private float top;
    private float bottom;
    private String unit = "°C";
    private boolean compact;

    public TrendChartView(Context context, AttributeSet attributes) {
        super(context, attributes);
        setFocusable(true);
        if (isInEditMode()) {
            // Layout Editor does not run the Activity or its ViewModel observers.
            List<EnvironmentData.Sample> preview = new ArrayList<>();
            long end = 1792627200000L;
            for (int hour = 0; hour <= 24; hour++) {
                preview.add(new EnvironmentData.Sample(end - (24 - hour) * 3600000L,
                        24.6 + 1.8 * Math.sin(hour * Math.PI / 12), 58.2, 426));
            }
            setSeries(preview, 0, getId() == R.id.mini_chart);
        }
    }

    public void setSeries(List<EnvironmentData.Sample> source, int metric, boolean compact) {
        samples.clear();
        samples.addAll(source);
        this.metric = metric;
        this.compact = compact;
        long span = samples.isEmpty() ? 0
                : samples.get(samples.size() - 1).timestamp - samples.get(0).timestamp;
        time.applyPattern(span > 2 * 24 * 3600000L ? "MM/dd" : "HH:mm");
        updateGradient();
        unit = metric == 0 ? "°C" : metric == 1 ? "%" : "lx";
        selected = -1;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (EnvironmentData.Sample sample : samples) {
            min = Math.min(min, sample.value(metric));
            max = Math.max(max, sample.value(metric));
        }
        float padding = (float) Math.max((max - min) * 0.2, metric == 2 ? 10 : 1);
        minimum = (float) min - padding;
        maximum = (float) max + padding;
        if (samples.isEmpty()) {
            setContentDescription("暂无历史数据");
        } else {
            setContentDescription(String.format(Locale.CHINA,
                    "%s历史曲线，%d 个采样点，最低 %.1f，最高 %.1f %s",
                    metric == 0 ? "温度" : metric == 1 ? "湿度" : "光照",
                    samples.size(), min, max, unit));
        }
        invalidate();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private int color(int resource) {
        return ContextCompat.getColor(getContext(), resource);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateGradient();
    }

    private void updateGradient() {
        int accent = color(metric == 0 ? R.color.temperature
                : metric == 1 ? R.color.humidity : R.color.light);
        gradient = new LinearGradient(0, compact ? dp(8) : dp(30),
                0, Math.max(dp(31), getHeight() - dp(26)),
                (accent & 0x00FFFFFF) | 0x30000000, Color.TRANSPARENT, Shader.TileMode.CLAMP);
    }

    private String timeLabel(long timestamp) {
        date.setTime(timestamp);
        return time.format(date);
    }

    private float x(int index) {
        if (samples.size() < 2) {
            return (left + right) / 2;
        }
        long start = samples.get(0).timestamp;
        long span = Math.max(1, samples.get(samples.size() - 1).timestamp - start);
        return left + (right - left) * (samples.get(index).timestamp - start) / span;
    }

    private float y(int index) {
        return bottom - (bottom - top) * ((float) samples.get(index).value(metric) - minimum)
                / (maximum - minimum);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(10) * getResources().getConfiguration().fontScale);
        paint.setColor(color(R.color.text_secondary));
        if (samples.isEmpty()) {
            canvas.drawText("暂无历史数据", dp(20), getHeight() / 2f, paint);
            return;
        }
        left = compact ? dp(3) : dp(42);
        right = getWidth() - dp(8);
        top = compact ? dp(8) : dp(30);
        bottom = getHeight() - dp(26);
        if (right <= left || bottom <= top) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            float gridY = top + (bottom - top) * i / 3f;
            paint.setColor(color(R.color.outline));
            paint.setStrokeWidth(dp(0.7f));
            canvas.drawLine(left, gridY, right, gridY, paint);
            if (!compact) {
                paint.setColor(color(R.color.text_secondary));
                canvas.drawText(String.format(Locale.CHINA, metric == 2 ? "%.0f" : "%.1f",
                        maximum - (maximum - minimum) * i / 3), 0, gridY + dp(4), paint);
            }
        }
        int accent = color(metric == 0 ? R.color.temperature
                : metric == 1 ? R.color.humidity : R.color.light);
        line.reset();
        line.moveTo(x(0), y(0));
        for (int i = 1; i < samples.size(); i++) {
            float middle = (x(i - 1) + x(i)) / 2;
            line.cubicTo(middle, y(i - 1), middle, y(i), x(i), y(i));
        }
        area.set(line);
        area.lineTo(x(samples.size() - 1), bottom);
        area.lineTo(x(0), bottom);
        area.close();
        paint.setShader(gradient);
        canvas.drawPath(area, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2.4f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(accent);
        canvas.drawPath(line, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.text_secondary));
        long firstTime = samples.get(0).timestamp;
        long timeSpan = samples.get(samples.size() - 1).timestamp - firstTime;
        for (int i = 0; i < 3; i++) {
            String label = timeLabel(firstTime + timeSpan * i / 2);
            float labelX = i == 0 ? left : i == 2 ? right - paint.measureText(label)
                    : (left + right - paint.measureText(label)) / 2;
            canvas.drawText(label, labelX, getHeight() - dp(4), paint);
        }
        if (selected >= 0 && selected < samples.size() && !compact) {
            paint.setColor(accent);
            paint.setStrokeWidth(dp(1));
            canvas.drawLine(x(selected), top, x(selected), bottom, paint);
            canvas.drawCircle(x(selected), y(selected), dp(4), paint);
            String label = timeLabel(samples.get(selected).timestamp) + "  "
                    + String.format(Locale.CHINA, "%.1f %s", samples.get(selected).value(metric), unit);
            canvas.drawText(label, left, dp(14), paint);
        } else {
            paint.setColor(accent);
            canvas.drawCircle(x(samples.size() - 1), y(samples.size() - 1), dp(3), paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (compact || samples.isEmpty()) {
            return super.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE) {
            selected = 0;
            for (int i = 1; i < samples.size(); i++) {
                if (Math.abs(x(i) - event.getX()) < Math.abs(x(selected) - event.getX())) {
                    selected = i;
                }
            }
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            return true;
        }
        return event.getAction() == MotionEvent.ACTION_CANCEL;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (!samples.isEmpty() && !compact) {
            if (selected < 0) {
                selected = samples.size() - 1;
            }
            announceForAccessibility(String.format(Locale.CHINA, "采样值 %.1f %s",
                    samples.get(selected).value(metric), unit));
            invalidate();
        }
        return true;
    }
}
