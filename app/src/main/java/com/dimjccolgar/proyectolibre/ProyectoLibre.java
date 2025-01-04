package com.dimjccolgar.proyectolibre;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.util.Log;  // prueba

import java.util.ArrayList;
import java.util.List;

public class ProyectoLibre extends View {
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    private final List<BitmapWithProperties> bitmapsWithProperties = new ArrayList<>();  // Lista para almacenar las imágenes
    private BitmapWithProperties activeBitmap = null;  // Imagen activa para moverla

    private final Paint paint = new Paint();
    private Bitmap treeBitmap;  // Imagen del árbol
    private Bitmap benchBitmap;  // Imagen del banco

    private Bitmap riverBitmap;  // Imagen del banco
    private Bitmap rockBitmap;  // Imagen del banco


    public ProyectoLibre(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Inicialización de los detectores
        GestureListener gestureListener = new GestureListener();
        gestureDetector = new GestureDetector(context, gestureListener);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        paint.setAntiAlias(true);

        // Cargar las imágenes
        treeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tree);  // Imagen 'tree'
        benchBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bench);  // Imagen 'bench'
        riverBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.river);  // Imagen 'bench'
        rockBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rock);  // Imagen 'bench'

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (BitmapWithProperties item : bitmapsWithProperties) {
            float halfWidth = item.bitmap.getWidth() / 2;
            float halfHeight = item.bitmap.getHeight() / 2;
            canvas.drawBitmap(item.bitmap, item.centerX - halfWidth, item.centerY - halfHeight, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && activeBitmap != null) {
            if (activeBitmap != null) {
                float dx = event.getX() - activeBitmap.touchOffsetX;
                float dy = event.getY() - activeBitmap.touchOffsetY;

                activeBitmap.centerX += dx;
                activeBitmap.centerY += dy;

                activeBitmap.touchOffsetX = event.getX();
                activeBitmap.touchOffsetY = event.getY();

                invalidate();
            }
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            // Finaliza el movimiento al soltar el dedo
            activeBitmap = null;
        }

        return true;
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
            bitmapsWithProperties.add(new BitmapWithProperties(scaledBitmap, x, y, size)); // Usa scaledBitmap aquí
            invalidate();            Log.d("TouchDebug", "Scaled Bitmap width: " + scaledBitmap.getWidth() + ", height: " + scaledBitmap.getHeight());

            Log.d("TouchDebug", "Touch position: x = " + x + ", y = " + y);

            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            for (int i = bitmapsWithProperties.size() - 1; i >= 0; i--) {
                if (bitmapsWithProperties.get(i).isTouched(e.getX(), e.getY())) {
                    bitmapsWithProperties.remove(i);
                    invalidate();
                    break;
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
                invalidate();  // Redibujar la vista
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
