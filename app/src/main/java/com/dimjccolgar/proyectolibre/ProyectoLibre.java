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

    // Variables para el AppMate tangible
    private AppMateCircle activeCircle = null;
    private final List<AppMateCircle> appMateCircles = new ArrayList<>();


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

        // Dibujar imágenes existentes
        for (BitmapWithProperties item : bitmapsWithProperties) {
            float halfWidth = item.bitmap.getWidth() / 2;
            float halfHeight = item.bitmap.getHeight() / 2;
            canvas.drawBitmap(item.bitmap, item.centerX - halfWidth, item.centerY - halfHeight, paint);
        }

        // Dibujar vector del AppMate si está activo
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(8f);
        for (AppMateCircle circle : appMateCircles) {
            canvas.drawCircle(circle.centerX, circle.centerY, circle.radius, paint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);

        // Manejar el movimiento de una imagen activa
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && activeBitmap != null) {
            float dx = event.getX() - activeBitmap.touchOffsetX;
            float dy = event.getY() - activeBitmap.touchOffsetY;

            activeBitmap.centerX += dx;
            activeBitmap.centerY += dy;

            activeBitmap.touchOffsetX = event.getX();
            activeBitmap.touchOffsetY = event.getY();

            invalidate();
            return true;
        }

        // Finalizar el movimiento de la imagen activa
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            activeBitmap = null;
            activeCircle = null; // También finalizamos el movimiento del círculo
            return true;
        }

        // Manejar el movimiento del círculo
        if (!appMateCircles.isEmpty()) {
            AppMateCircle circle = appMateCircles.get(0); // Solo un círculo permitido

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // Detectar si el toque inicial está dentro del círculo
                    float dx = event.getX() - circle.centerX;
                    float dy = event.getY() - circle.centerY;
                    if (Math.sqrt(dx * dx + dy * dy) <= circle.radius) {
                        activeCircle = circle; // Activar el círculo para moverlo
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (activeCircle != null) {
                        // Mover el círculo al actualizar las coordenadas del centro
                        activeCircle.centerX = event.getX();
                        activeCircle.centerY = event.getY();
                        invalidate(); // Redibujar
                        return true;
                    }
                    break;
            }
        }

        // Limitar la creación a un solo círculo
        if (appMateCircles.size() >= 1) {
            return true; // No permitir más de un círculo
        }

        // Crear un círculo cuando se detectan tres contactos simultáneos
        if (event.getPointerCount() == 3) {
            float x1 = event.getX(0), y1 = event.getY(0);
            float x2 = event.getX(1), y2 = event.getY(1);
            float x3 = event.getX(2), y3 = event.getY(2);

            // Calcular el círculo que engloba las tres posiciones
            AppMateCircle circle = calcularCirculo(x1, y1, x2, y2, x3, y3);
            if (circle != null) {
                appMateCircles.clear(); // Limpiar cualquier círculo existente
                appMateCircles.add(circle); // Añadir el nuevo círculo
                invalidate(); // Redibujar
            }
        }

        return true;
    }


    private AppMateCircle calcularCirculo(float x1, float y1, float x2, float y2, float x3, float y3) {
        // Calcular el centroide como aproximación al centro
        float centerX = (x1 + x2 + x3) / 3;
        float centerY = (y1 + y2 + y3) / 3;

        // Calcular el radio como la distancia máxima del centro a los tres puntos
        float r1 = (float) Math.hypot(centerX - x1, centerY - y1);
        float r2 = (float) Math.hypot(centerX - x2, centerY - y2);
        float r3 = (float) Math.hypot(centerX - x3, centerY - y3);

        float radius = Math.max(r1, Math.max(r2, r3)); // Tomar el mayor como radio
        return new AppMateCircle(centerX, centerY, radius);
    }
    private static class AppMateCircle{
        float centerX, centerY;
        float radius;

        AppMateCircle(float centerX, float centerY, float radius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;

        }
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
                    return;
                }
            }

            // Verificar si se toca un círculo y eliminarlo
            float x = e.getX();
            float y = e.getY();

            AppMateCircle circleToRemove = null;
            for (AppMateCircle circle : appMateCircles) {
                float dx = x - circle.centerX;
                float dy = y - circle.centerY;
                if (Math.sqrt(dx * dx + dy * dy) <= circle.radius) { // Tolerancia: dentro del círculo
                    circleToRemove = circle;
                    break;
                }
            }

            if (circleToRemove != null) {
                appMateCircles.remove(circleToRemove);
                invalidate();
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