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

    // Variables para almacenar el círculo
    private Float savedCenterX = null;
    private Float savedCenterY = null;
    private Float savedRadius = null;

    private final Map<Integer, float[]> contactPoints = new HashMap<>();

    private float lineStartX = -1, lineStartY = -1;
    private float lineEndX = -1, lineEndY = -1;


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

        // Dibujar las imágenes
        for (BitmapWithProperties item : bitmapsWithProperties) {
            float halfWidth = item.bitmap.getWidth() / 2;
            float halfHeight = item.bitmap.getHeight() / 2;
            canvas.drawBitmap(item.bitmap, item.centerX - halfWidth, item.centerY - halfHeight, paint);
        }

        // Dibujar el círculo con sombreado si ya fue calculado
        if (savedCenterX != null && savedCenterY != null && savedRadius != null) {
            // Definir el color de relleno y el sombreado (gradiente)
            paint.setStyle(Paint.Style.FILL);

            // Crear un gradiente radial que simula el sombreado
            int[] colors = {Color.YELLOW, Color.argb(0, 255, 255, 0)};  // Amarillo a transparente
            float[] positions = {0.0f, 1.0f};  // De amarillo a transparente

            // Asegúrate de usar primitivos 'float' y un valor adecuado para TileMode
            android.graphics.RadialGradient gradient = new android.graphics.RadialGradient(
                    savedCenterX, savedCenterY, savedRadius, colors, positions, android.graphics.Shader.TileMode.CLAMP);

            paint.setShader(gradient);

            // Dibujar el círculo con el sombreado aplicado
            canvas.drawCircle(savedCenterX, savedCenterY, savedRadius, paint);

            // Resetear el shader para evitar afectar otros dibujos
            paint.setShader(null);
        }

        // Dibujar la línea solo si hay dos dedos
        if (lineStartX != -1 && lineStartY != -1 && lineEndX != -1 && lineEndY != -1) {
            paint.setColor(Color.BLUE);  // Cambia el color según tu preferencia
            paint.setStrokeWidth(6f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, paint);
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
                // Si hay una imagen activa, se mueve y se ignora la lógica de líneas/círculos
                if (activeBitmap != null) {
                    float newX = event.getX(pointerIndex) - activeBitmap.touchOffsetX;
                    float newY = event.getY(pointerIndex) - activeBitmap.touchOffsetY;

                    activeBitmap.centerX = newX;
                    activeBitmap.centerY = newY;
                    invalidate();
                    return true;
                }

                // Actualizar los puntos de contacto
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    float[] point = contactPoints.get(id);
                    if (point != null) {
                        point[0] = event.getX(i);
                        point[1] = event.getY(i);
                    }
                }

                // Si hay tres puntos, actualiza el círculo
                if (contactPoints.size() == 3) {
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

                    // Resetea las coordenadas de la línea
                    lineStartX = -1;
                    lineStartY = -1;
                    lineEndX = -1;
                    lineEndY = -1;
                }
                // Si hay exactamente dos puntos, actualiza las coordenadas de la línea
                else if (contactPoints.size() == 2) {
                    List<float[]> points = new ArrayList<>(contactPoints.values());
                    lineStartX = points.get(0)[0];
                    lineStartY = points.get(0)[1];
                    lineEndX = points.get(1)[0];
                    lineEndY = points.get(1)[1];
                }
                // Si no hay dos o tres puntos, resetea la línea, pero el círculo permanece
                else if (contactPoints.size() < 2) {
                    lineStartX = -1;
                    lineStartY = -1;
                    lineEndX = -1;
                    lineEndY = -1;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                contactPoints.remove(pointerId);

                // Resetear la imagen activa
                if (activeBitmap != null) {
                    activeBitmap = null;
                }

                // Verificar si los puntos restantes son menos de 2
                if (contactPoints.size() < 2) {
                    // Reinicia la línea si ya no hay suficientes puntos para una línea
                    lineStartX = -1;
                    lineStartY = -1;
                    lineEndX = -1;
                    lineEndY = -1;
                }

                invalidate();
                break;
            }

        }

        invalidate();
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