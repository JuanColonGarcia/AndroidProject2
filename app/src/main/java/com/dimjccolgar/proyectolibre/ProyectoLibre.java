package com.dimjccolgar.proyectolibre;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProyectoLibre extends View {
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    private final List<BitmapWithProperties> bitmapsWithProperties = new ArrayList<>();  // Lista para almacenar las imágenes
    private BitmapWithProperties activeBitmap = null;  // Imagen activa para moverla

    private final Paint paint = new Paint();
    private Bitmap treeBitmap;  // Imagen del árbol
    private Bitmap benchBitmap;  // Imagen del banco
    private Bitmap riverBitmap;  // Imagen del río
    private Bitmap rockBitmap;  // Imagen de la roca

    // Variables para almacenar el círculo persistente
    private Float savedCenterX = null;
    private Float savedCenterY = null;
    private Float savedRadius = null;

    private final Map<Integer, float[]> contactPoints = new HashMap<>();

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
        riverBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.river);  // Imagen 'river'
        rockBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rock);  // Imagen 'rock'

        paint.setAntiAlias(true);
        paint.setStrokeWidth(6f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.YELLOW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Dibujar imágenes existentes
        for (BitmapWithProperties item : bitmapsWithProperties) {
            float halfWidth = item.bitmap.getWidth() / 2;
            float halfHeight = item.bitmap.getHeight() / 2;
            canvas.drawBitmap(item.bitmap, item.centerX - halfWidth, item.centerY - halfHeight, paint);
        }

        // Si hay exactamente 3 puntos de contacto, calcula y dibuja el círculo
        if (contactPoints.size() == 3) {
            float[] point1 = null, point2 = null, point3 = null;

            int index = 0;
            for (float[] point : contactPoints.values()) {
                if (index == 0) point1 = point;
                else if (index == 1) point2 = point;
                else if (index == 2) point3 = point;

                index++;
            }

            if (point1 != null && point2 != null && point3 != null) {
                // Calcula el centro del círculo
                float centerX = (point1[0] + point2[0] + point3[0]) / 3;
                float centerY = (point1[1] + point2[1] + point3[1]) / 3;

                // Calcula el radio como la distancia promedio al centro
                float radius = (
                        (float) Math.sqrt(Math.pow(point1[0] - centerX, 2) + Math.pow(point1[1] - centerY, 2)) +
                                (float) Math.sqrt(Math.pow(point2[0] - centerX, 2) + Math.pow(point2[1] - centerY, 2)) +
                                (float) Math.sqrt(Math.pow(point3[0] - centerX, 2) + Math.pow(point3[1] - centerY, 2))
                ) / 3;

                // Guarda las coordenadas del círculo para persistencia
                savedCenterX = centerX;
                savedCenterY = centerY;
                savedRadius = radius;

                // Dibuja el círculo con los valores calculados
                paint.setColor(Color.YELLOW);
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawCircle(centerX, centerY, radius, paint);
            }
        }

        // Si el círculo fue guardado previamente, redibuja el círculo persistente
        if (savedCenterX != null && savedCenterY != null && savedRadius != null) {
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeJoin(Paint.Join.ROUND);
            canvas.drawCircle(savedCenterX, savedCenterY, savedRadius, paint);
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
                if (contactPoints.size() < 3) {
                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);
                    contactPoints.put(pointerId, new float[]{x, y});
                }

                // Detectar si una imagen activa está siendo tocada
                for (BitmapWithProperties item : bitmapsWithProperties) {
                    if (item.isTouched(event.getX(), event.getY())) {
                        activeBitmap = item;  // Solo se marca como activa la imagen tocada
                        activeBitmap.touchOffsetX = event.getX() - item.centerX;
                        activeBitmap.touchOffsetY = event.getY() - item.centerY;
                        return true;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                // Mover solo la imagen activa
                if (activeBitmap != null) {
                    float newX = event.getX(pointerIndex) - activeBitmap.touchOffsetX;
                    float newY = event.getY(pointerIndex) - activeBitmap.touchOffsetY;

                    activeBitmap.centerX = newX;
                    activeBitmap.centerY = newY;
                }

                // Actualizar las posiciones de los puntos de contacto
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    float[] point = contactPoints.get(id);
                    if (point != null) {
                        point[0] = event.getX(i);
                        point[1] = event.getY(i);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                contactPoints.remove(pointerId);

                // Desmarcar la imagen activa cuando se deja de tocar
                if (activeBitmap != null) {
                    activeBitmap = null;
                }
                break;
            }
        }

        invalidate();  // Redibuja la vista
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
            invalidate();
            Log.d("TouchDebug", "Scaled Bitmap width: " + scaledBitmap.getWidth() + ", height: " + scaledBitmap.getHeight());

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