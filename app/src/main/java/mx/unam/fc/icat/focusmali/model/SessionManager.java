package mx.unam.fc.icat.focusmali.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import mx.unam.fc.icat.focusmali.data.SessionDbHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona el ciclo de vida de las sesiones.
 * Ahora utiliza el patrón Singleton y SQLite para persistencia real.
 */
public class SessionManager {
    private static SessionManager instance;
    private final SessionDbHelper dbHelper;

    // El constructor es privado: nadie puede hacer 'new SessionManager()' desde fuera
    private SessionManager(Context context) {
        this.dbHelper = new SessionDbHelper(context.getApplicationContext());
    }

    /**
     * Método Singleton: garantiza que toda la app use la misma conexión a la DB.
     */
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * Guarda una sesión físicamente en la base de datos.
     */
    public void addSession(Session session) {
        if (session == null) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(SessionDbHelper.COLUMN_TYPE, session.getType());
        values.put(SessionDbHelper.COLUMN_DATE, session.getDate());
        values.put(SessionDbHelper.COLUMN_START_TIME, session.getStartTime());
        values.put(SessionDbHelper.COLUMN_DURATION, session.getDuration());
        // SQLite no maneja booleanos, usamos 1 para true y 0 para false
        values.put(SessionDbHelper.COLUMN_COMPLETED, session.isCompleted() ? 1 : 0);

        try {
            db.insert(SessionDbHelper.TABLE_NAME, null, values);
        } catch (Exception e) {
            e.printStackTrace(); // Requisito: Manejo de errores
        } finally {
            db.close(); // Siempre cerrar para evitar fugas de memoria
        }
    }

    /**
     * Recupera el historial desde SQLite ordenado por lo más reciente.
     */
    public List<Session> getHistory() {
        List<Session> history = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
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
            cursor.close();
        }
        db.close();
        return history;
    }
}