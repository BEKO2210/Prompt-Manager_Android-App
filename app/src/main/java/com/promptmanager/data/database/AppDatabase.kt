package com.promptmanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room Database für die Prompt Manager App.
 *
 * Version 1: Initiales Schema mit PromptEntity.
 * Version 2: Versionierungs-System (version, parentId)
 */
@Database(
    entities = [PromptEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun promptDao(): PromptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration von Version 1 zu 2: Versionierungs-System.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE prompts ADD COLUMN version TEXT NOT NULL DEFAULT '1.0'"
                )
                database.execSQL(
                    "ALTER TABLE prompts ADD COLUMN parentId INTEGER DEFAULT NULL"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prompt_manager_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Callback zum Befüllen der DB mit Demo-Daten beim ersten Start.
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.promptDao())
                    }
                }
            }
        }

        /**
         * Fügt Template-Prompts für den ersten Start hinzu.
         */
        private suspend fun populateDatabase(promptDao: PromptDao) {
            val now = System.currentTimeMillis()

            val templates = listOf(
                PromptEntity(
                    title = "E-Mail verfassen",
                    description = "Formelle E-Mail mit anpassbarem Ton und Thema",
                    content = """Schreibe eine professionelle E-Mail zum Thema [Thema=Projektupdate] für [Empfänger=das Team].

Ton: [Ton=freundlich und sachlich]

Kernpunkte:
[Kernpunkte=- Status des aktuellen Sprints
- Nächste Schritte
- Offene Fragen]

Unterschrift: [Name=Max Mustermann]""",
                    category = "E-Mail",
                    createdAt = now,
                    updatedAt = now,
                    isFavorite = true
                ),

                PromptEntity(
                    title = "Code Review durchführen",
                    description = "Systematische Code-Review mit Fokus auf Best Practices",
                    content = """Führe ein Code Review für folgenden [Programmiersprache=Kotlin]-Code durch:

```
[Code]
```

Prüfe auf:
- Code-Qualität und Lesbarkeit
- Performance-Probleme
- Security-Lücken
- Best Practices für [Programmiersprache=Kotlin]

Gib konkrete Verbesserungsvorschläge mit Code-Beispielen.""",
                    category = "Entwicklung",
                    createdAt = now,
                    updatedAt = now,
                    isFavorite = true
                ),

                PromptEntity(
                    title = "Social Media Post",
                    description = "Kreativer Post für verschiedene Plattformen - mit Dropdown-Auswahl",
                    content = """Erstelle einen ansprechenden Social Media Post für [Plattform=LinkedIn,Twitter,Instagram,Facebook] zum Thema: [Thema=Produktlaunch]

Zielgruppe: [Zielgruppe=Entwickler und Tech-Enthusiasten]

Tonalität: [Tonalität=professionell,nahbar,enthusiastisch,sachlich]

Sprache: [Sprache=Deutsch,Englisch,Französisch]

Call-to-Action: [CTA=Mehr erfahren]

Verwende passende Emojis und halte es unter [Zeichenlimit=280] Zeichen.""",
                    category = "Social Media",
                    createdAt = now,
                    updatedAt = now
                ),

                PromptEntity(
                    title = "Meeting-Notizen zusammenfassen",
                    description = "Strukturierte Zusammenfassung von Meeting-Protokollen",
                    content = """Fasse folgende Meeting-Notizen zusammen:

[Meeting-Notizen]

Erstelle eine strukturierte Zusammenfassung mit:
1. **Hauptthemen**: Die wichtigsten besprochenen Punkte
2. **Entscheidungen**: Getroffene Beschlüsse
3. **Action Items**: To-Dos mit Verantwortlichen
4. **Nächste Schritte**: Geplante Follow-ups

Meeting: [Meeting-Titel=Sprint Planning]
Datum: [Datum]""",
                    category = "Business",
                    createdAt = now,
                    updatedAt = now
                ),

                PromptEntity(
                    title = "Lernzusammenfassung erstellen",
                    description = "Komplexe Themen für verschiedene Zielgruppen aufbereiten",
                    content = """Erkläre das Thema [Thema=Machine Learning] für [Zielgruppe=Einsteiger ohne technischen Hintergrund].

Anforderungen:
- Verwende einfache Sprache und alltagsnahe Beispiele
- Strukturiere in [Anzahl_Abschnitte=3-5] Abschnitte
- Füge praktische Anwendungsbeispiele hinzu
- Vermeide Fachbegriffe oder erkläre sie wenn nötig

Länge: [Länge=ca. 300 Wörter]""",
                    category = "Bildung",
                    createdAt = now,
                    updatedAt = now
                )
            )

            templates.forEach { promptDao.insertPrompt(it) }
        }
    }
}
