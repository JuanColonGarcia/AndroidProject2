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

        // Dibujar todas las imágenes en la lista
        for (BitmapWithProperties item : bitmapsWithProperties) {
            float halfSize = item.size / 2;
            // Dibujar el Bitmap usando el centro y el tamaño
            canvas.drawBitmap(item.bitmap, item.centerX - halfSize, item.centerY - halfSize, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && activeBitmap != null) {
            // Mover la imagen activa con el dedo
            if (activeBitmap != null) {
                float dx = event.getX() - activeBitmap.touchOffsetX;
                float dy = event.getY() - activeBitmap.touchOffsetY;

                activeBitmap.centerX += dx;
                activeBitmap.centerY += dy;

                // Actualizar las coordenadas del punto de toque para el siguiente movimiento
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
            // Verificar si tocamos una imagen activa
            for (BitmapWithProperties item : bitmapsWithProperties) {
                if (item.isTouched(e.getX(), e.getY())) {
                    activeBitmap = item;  // Establecer la imagen activa

                    // Guardar la posición inicial del toque para el movimiento
                    activeBitmap.touchOffsetX = e.getX();
                    activeBitmap.touchOffsetY = e.getY();

                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Doble toque para agregar una nueva imagen (puede ser 'tree' o 'bench')
            float x = e.getX();
            float y = e.getY();
            float size = 500; // Tamaño predeterminado de la imagen

            // Agregar la imagen 'tree' o 'bench' según la posición del toque o alguna lógica específica
            Bitmap bitmapToAdd;
            if (x < getWidth() / 2 && y < getHeight() / 2) {
                // Cuadrante superior izquierdo
                bitmapToAdd = treeBitmap;
            } else if (x >= getWidth() / 2 && y < getHeight() / 2) {
                // Cuadrante superior derecho
                bitmapToAdd = benchBitmap;
            } else if (x < getWidth() / 2 && y >= getHeight() / 2) {
                // Cuadrante inferior izquierdo
                bitmapToAdd = riverBitmap;
            } else {
                // Cuadrante inferior derecho
                bitmapToAdd = rockBitmap;
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapToAdd, (int) size, (int) size, false);
            Log.d("TouchDebug", "Scaled Bitmap width: " + scaledBitmap.getWidth() + ", height: " + scaledBitmap.getHeight());

            Log.d("TouchDebug", "Touch position: x = " + x + ", y = " + y);

            bitmapsWithProperties.add(new BitmapWithProperties(bitmapToAdd, x, y, size));
            invalidate();  // Redibujar la vista
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // Pulsación larga para eliminar una imagen
            for (int i = bitmapsWithProperties.size() - 1; i >= 0; i--) {
                if (bitmapsWithProperties.get(i).isTouched(e.getX(), e.getY())) {
                    bitmapsWithProperties.remove(i);  // Eliminar la imagen tocada
                    invalidate();  // Redibujar la vista
                    break;
                }
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Escalar la imagen activa con el gesto de pellizcar
            if (activeBitmap != null) {

                // Actualizamos el tamaño con el factor de escala
                activeBitmap.size *= detector.getScaleFactor();

                // Limitar el tamaño entre 50 y 300
                activeBitmap.size = Math.max(50, Math.min(activeBitmap.size, 800));



                invalidate();  // Redibujar la vista
            }
            return true;
        }
    }

    // Clase que encapsula un Bitmap con sus propiedades
    private static class BitmapWithProperties {
        Bitmap bitmap;
        float size;
        float centerX, centerY;
        float touchOffsetX, touchOffsetY;
        float touchRadius;  // Radio de interacción para tocar la imagen

        BitmapWithProperties(Bitmap bitmap, float centerX, float centerY, float size) {
            this.bitmap = bitmap;
            this.centerX = centerX;
            this.centerY = centerY;
            this.size = size;
            this.touchRadius = size * 1.5f;  // Hacemos que el radio de toque sea 1.5 veces el tamaño de la imagen
        }

        boolean isTouched(float x, float y) {
            // Verificamos si el toque está dentro del área expandida (radio de toque)
            float dx = x - centerX;
            float dy = y - centerY;
            Log.d("TouchDebug", "Touch at: " + x + "," + y + " - Image center: " + centerX + "," + centerY + " - dx: " + dx + " dy: " + dy);
            return (dx * dx + dy * dy) <= (touchRadius * touchRadius);
        }
    }
}
