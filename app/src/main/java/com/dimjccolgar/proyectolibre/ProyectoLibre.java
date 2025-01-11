package com.dimjccolgar.proyectolibre;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProyectoLibre extends View {
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    private final List<BitmapWithProperties> bitmapsWithProperties = new ArrayList<>();
    private BitmapWithProperties activeBitmap = null;

    private final Paint paint = new Paint();
    private Bitmap treeBitmap;
    private Bitmap benchBitmap;
    private Bitmap riverBitmap;
    private Bitmap rockBitmap;

    private Float savedCenterX = null;
    private Float savedCenterY = null;
    private Float savedRadius = null;
    private final Map<Integer, float[]> contactPoints = new HashMap<>();

    private boolean isCircleRotating = false;
    private boolean isTriangleRotating = false;
    private float initialRotationAngleCircle = 0;
    private float initialRotationAngleTriangle = 0;
    private float rotationAngleCircle = 0;
    private float rotationAngleTriangle = 0;

    private boolean isTriangleFixed = false;
    private float fixedTriangleX, fixedTriangleY, fixedTriangleSize;

    private final Paint brownPaint = new Paint();
    private final Paint greenPaint = new Paint();

    public ProyectoLibre(Context context, AttributeSet attrs) {
        super(context, attrs);

        GestureListener gestureListener = new GestureListener();
        gestureDetector = new GestureDetector(context, gestureListener);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

        treeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tree);
        benchBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bench);
        riverBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.river);
        rockBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rock);

        paint.setAntiAlias(true);
        paint.setStrokeWidth(6f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.YELLOW);

        brownPaint.setColor(Color.rgb(139, 69, 19));
        brownPaint.setStyle(Paint.Style.FILL);
        brownPaint.setAntiAlias(true);

        greenPaint.setColor(Color.GREEN);
        greenPaint.setStyle(Paint.Style.FILL);
        greenPaint.setAntiAlias(true);
    }

    private void drawMountain(Canvas canvas, float baseX, float baseY, float size) {
        float halfBase = size * 0.5f;

        float tipX = baseX;
        float tipY = baseY - size;

        float leftBaseX = baseX - halfBase;
        float leftBaseY = baseY;

        float rightBaseX = baseX + halfBase;
        float rightBaseY = baseY;

        Path path = new Path();
        path.moveTo(tipX, tipY);
        path.lineTo(leftBaseX, leftBaseY);
        path.lineTo(rightBaseX, rightBaseY);
        path.close();

        canvas.drawPath(path, brownPaint);

        float smallTipX = baseX;
        float smallTipY = tipY;

        float clipLeftX = baseX - halfBase;

        float clipRightX = baseX + halfBase;
        float clipRightY = tipY + size * (1.0f / 3.0f);

        Path smallPath = new Path();
        smallPath.moveTo(smallTipX, smallTipY);
        smallPath.lineTo(leftBaseX, leftBaseY);
        smallPath.lineTo(rightBaseX, rightBaseY);
        smallPath.close();

        canvas.save();
        canvas.clipRect(clipLeftX, tipY, clipRightX, clipRightY);
        canvas.drawPath(smallPath, greenPaint);
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (BitmapWithProperties item : bitmapsWithProperties) {
            float halfWidth = item.bitmap.getWidth() / 2;
            float halfHeight = item.bitmap.getHeight() / 2;
            canvas.drawBitmap(item.bitmap, item.centerX - halfWidth, item.centerY - halfHeight, paint);
        }

        if (savedCenterX != null && savedCenterY != null && savedRadius != null) {
            canvas.save();
            canvas.translate(savedCenterX, savedCenterY);
            canvas.rotate(rotationAngleCircle);
            canvas.translate(-savedCenterX, -savedCenterY);

            paint.setStyle(Paint.Style.FILL);
            int[] colors = {Color.YELLOW, Color.argb(0, 255, 255, 0)};
            float[] positions = {0.0f, 1.0f};
            android.graphics.RadialGradient gradient = new android.graphics.RadialGradient(
                    savedCenterX, savedCenterY, savedRadius, colors, positions, android.graphics.Shader.TileMode.CLAMP);
            paint.setShader(gradient);
            canvas.drawCircle(savedCenterX, savedCenterY, savedRadius, paint);
            paint.setShader(null);

            canvas.restore();
        }

        if (isTriangleFixed) {
            canvas.save();
            canvas.translate(fixedTriangleX, fixedTriangleY);
            canvas.rotate(rotationAngleTriangle);
            canvas.translate(-fixedTriangleX, -fixedTriangleY);

            drawMountain(canvas, fixedTriangleX, fixedTriangleY, fixedTriangleSize);

            canvas.restore();
        } else if (contactPoints.size() >= 4) {
            List<float[]> points = new ArrayList<>(contactPoints.values());
            float[] point1 = points.get(0);
            float[] point2 = points.get(1);

            float baseX = (point1[0] + point2[0]) / 2;
            float baseY = (point1[1] + point2[1]) / 2;

            float size = (float) Math.sqrt(Math.pow(point2[0] - point1[0], 2) + Math.pow(point2[1] - point1[1], 2));

            canvas.save();
            canvas.translate(baseX, baseY);
            canvas.rotate(rotationAngleTriangle);
            canvas.translate(-baseX, -baseY);

            drawMountain(canvas, baseX, baseY, size);

            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (contactPoints.size() < 5) {
                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);
                    contactPoints.put(pointerId, new float[]{x, y});
                }

                for (BitmapWithProperties item : bitmapsWithProperties) {
                    if (item.isTouched(event.getX(), event.getY())) {
                        activeBitmap = item;
                        activeBitmap.touchOffsetX = event.getX() - item.centerX;
                        activeBitmap.touchOffsetY = event.getY() - item.centerY;
                        return true;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (activeBitmap != null) {
                    float newX = event.getX(pointerIndex) - activeBitmap.touchOffsetX;
                    float newY = event.getY(pointerIndex) - activeBitmap.touchOffsetY;

                    activeBitmap.centerX = newX;
                    activeBitmap.centerY = newY;
                    invalidate();
                    return true;
                }

                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    float[] point = contactPoints.get(id);
                    if (point != null) {
                        point[0] = event.getX(i);
                        point[1] = event.getY(i);
                    }
                }

                if (contactPoints.size() >= 4) {
                    List<float[]> points = new ArrayList<>(contactPoints.values());
                    float[] point1 = points.get(0);
                    float[] point2 = points.get(1);
                    float[] point3 = points.get(2);
                    float[] point4 = points.get(3);

                    float centerX = (point1[0] + point2[0] + point3[0] + point4[0]) / 4;
                    float centerY = (point1[1] + point2[1] + point3[1] + point4[1]) / 4;

                    if (!isTriangleRotating) {
                        initialRotationAngleTriangle = calculateAngle(point1[0], point1[1], centerX, centerY);
                        isTriangleRotating = true;
                    }

                    float currentAngle = calculateAngle(point1[0], point1[1], centerX, centerY);
                    rotationAngleTriangle += currentAngle - initialRotationAngleTriangle;
                    initialRotationAngleTriangle = currentAngle;

                    fixedTriangleX = centerX;
                    fixedTriangleY = centerY;
                    fixedTriangleSize = (float) Math.sqrt(Math.pow(point2[0] - point1[0], 2) + Math.pow(point2[1] - point1[1], 2));

                    invalidate();
                } else if (contactPoints.size() == 3) {
                    List<float[]> points = new ArrayList<>(contactPoints.values());
                    float[] point1 = points.get(0);
                    float[] point2 = points.get(1);
                    float[] point3 = points.get(2);

                    float centerX = (point1[0] + point2[0] + point3[0]) / 3;
                    float centerY = (point1[1] + point2[1] + point3[1]) / 3;

                    float radius = (
                            (float) Math.sqrt(Math.pow(point1[0] - centerX, 2) + Math.pow(point1[1] - centerY, 2)) +
                                    (float) Math.sqrt(Math.pow(point2[0] - centerX, 2) + Math.pow(point2[1] - centerY, 2)) +
                                    (float) Math.sqrt(Math.pow(point3[0] - centerX, 2) + Math.pow(point3[1] - centerY, 2))
                    ) / 3;

                    savedCenterX = centerX;
                    savedCenterY = centerY;
                    savedRadius = radius;

                    if (!isCircleRotating) {
                        initialRotationAngleCircle = calculateAngle(point1[0], point1[1], centerX, centerY);
                        isCircleRotating = true;
                    }

                    float currentAngle = calculateAngle(point1[0], point1[1], centerX, centerY);
                    rotationAngleCircle += currentAngle - initialRotationAngleCircle;
                    initialRotationAngleCircle = currentAngle;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                contactPoints.remove(pointerId);

                if (activeBitmap != null) {
                    activeBitmap = null;
                }

                if (contactPoints.size() >= 4) {
                    isTriangleFixed = true;
                    List<float[]> points = new ArrayList<>(contactPoints.values());
                    float[] point1 = points.get(0);
                    float[] point2 = points.get(1);

                    fixedTriangleX = (point1[0] + point2[0]) / 2;
                    fixedTriangleY = (point1[1] + point2[1]) / 2;
                    fixedTriangleSize = (float) Math.sqrt(Math.pow(point2[0] - point1[0], 2) + Math.pow(point2[1] - point1[1], 2));
                }

                isCircleRotating = false;
                isTriangleRotating = false;
                isTriangleFixed = true;

                invalidate();
                break;
            }
        }

        invalidate();
        return true;
    }
    private float calculateAngle(float x1, float y1, float x2, float y2) {
        return (float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            for (BitmapWithProperties item : bitmapsWithProperties) {
                if (item.isTouched(e.getX(), e.getY())) {
                    activeBitmap = item;
                    activeBitmap.touchOffsetX = e.getX();
                    activeBitmap.touchOffsetY = e.getY();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            float size = 500;

            Bitmap bitmapToAdd;
            if (x < getWidth() / 2 && y < getHeight() / 2) {
                bitmapToAdd = treeBitmap;
            } else if (x >= getWidth() / 2 && y < getHeight() / 2) {
                bitmapToAdd = benchBitmap;
            } else if (x < getWidth() / 2 && y >= getHeight() / 2) {
                bitmapToAdd = riverBitmap;
            } else {
                bitmapToAdd = rockBitmap;
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapToAdd, (int) size, (int) size, false);
            bitmapsWithProperties.add(new BitmapWithProperties(scaledBitmap, x, y, size));
            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            for (int i = bitmapsWithProperties.size() - 1; i >= 0; i--) {
                if (bitmapsWithProperties.get(i).isTouched(e.getX(), e.getY())) {
                    bitmapsWithProperties.remove(i);
                    invalidate();
                    return;
                }
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (activeBitmap != null) {
                float scaleFactor = detector.getScaleFactor();
                activeBitmap.size *= scaleFactor;
                activeBitmap.size = Math.max(50, Math.min(activeBitmap.size, 800));
                activeBitmap.scaleBitmap(activeBitmap.size);
                invalidate();
            }
            return true;
        }
    }

    private static class BitmapWithProperties {
        Bitmap originalBitmap;
        Bitmap bitmap;
        float size;
        float centerX, centerY;
        float touchOffsetX, touchOffsetY;
        float touchRadius;

        BitmapWithProperties(Bitmap bitmap, float centerX, float centerY, float size) {
            this.originalBitmap = bitmap;
            this.bitmap = bitmap;
            this.centerX = centerX;
            this.centerY = centerY;
            this.size = size;
            this.touchRadius = size * 0.5f;
        }

        void scaleBitmap(float newSize) {
            this.size = newSize;
            int newWidth = (int) size;
            int newHeight = (int) (originalBitmap.getHeight() * ((float) newWidth / originalBitmap.getWidth()));
            this.bitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
        }

        boolean isTouched(float x, float y) {
            float dx = x - centerX;
            float dy = y - centerY;
            return (dx * dx + dy * dy) <= (touchRadius * touchRadius);
        }
    }
}