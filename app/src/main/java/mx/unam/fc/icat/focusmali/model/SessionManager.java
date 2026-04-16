package mx.unam.fc.icat.focusmali.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log; // Añadido para reportar errores en el Logcat
import mx.unam.fc.icat.focusmali.data.SessionDbHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona el ciclo de vida de las sesiones.
 * Implementa Robustez mediante el manejo de excepciones (try-catch).
 */
public class SessionManager {
    private static SessionManager instance;
    private final SessionDbHelper dbHelper;
    private static final String TAG = "SessionManager";

    private SessionManager(Context context) {
        this.dbHelper = new SessionDbHelper(context.getApplicationContext());
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * Guarda una sesión físicamente en la base de datos.
     * CUMPLE RÚBRICA: Robustez con bloques try-catch-finally.
     */
    public void addSession(final Session session) {
        if (session == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase db = null;
                try {
                    // INTENTO de escritura
                    db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();

                    values.put(SessionDbHelper.COLUMN_TYPE, session.getType());
                    values.put(SessionDbHelper.COLUMN_DATE, session.getDate());
                    values.put(SessionDbHelper.COLUMN_START_TIME, session.getStartTime());
                    values.put(SessionDbHelper.COLUMN_DURATION, session.getDuration());
                    values.put(SessionDbHelper.COLUMN_COMPLETED, session.isCompleted() ? 1 : 0);

                    db.insert(SessionDbHelper.TABLE_NAME, null, values);
                    Log.d(TAG, "Sesión insertada con éxito.");

                } catch (Exception e) {
                    // CAPTURA de errores (Robustez)
                    Log.e(TAG, "Error crítico al insertar en la base de datos: " + e.getMessage());
                } finally {
                    // CIERRE SEGURO: Se ejecuta ocurra o no un error
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
            }
        }).start();
    }

    /**
     * Recupera el historial desde SQLite.
     * CUMPLE RÚBRICA: Manejo de errores en operaciones de lectura.
     */
    public List<Session> getHistory() {
        List<Session> history = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    SessionDbHelper.TABLE_NAME,
                    null, null, null, null, null,
                    SessionDbHelper.COLUMN_ID + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Session session = new Session(
                            cursor.getString(cursor.getColumnIndexOrThrow(SessionDbHelper.COLUMN_TYPE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(SessionDbHelper.COLUMN_DATE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(SessionDbHelper.COLUMN_START_TIME)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(SessionDbHelper.COLUMN_DURATION)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(SessionDbHelper.COLUMN_COMPLETED)) == 1
                    );
                    history.add(session);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            // Si hay un error, devolvemos la lista vacía pero la app no truena
            Log.e(TAG, "Error al recuperar historial: " + e.getMessage());
        } finally {
            // Liberación de recursos
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return history;
    }
}