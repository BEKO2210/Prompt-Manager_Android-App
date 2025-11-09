<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9.20-purple?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-UI-blue?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Room-Database-orange" />
  <img src="https://img.shields.io/badge/Gradle-8.2.0-02303A?logo=gradle&logoColor=white" />
  <img src="https://img.shields.io/badge/Android%20Studio-Hedgehog-green?logo=androidstudio&logoColor=white" />
</p>


# Prompt Manager - Android App

**Professioneller Prompt-Manager für Android mit dynamischen Platzhaltern**

Eine moderne Android-App zum Verwalten, Anpassen und Nutzen von KI-Prompts mit dynamischer Platzhalter-Unterstützung. Entwickelt mit Jetpack Compose und Clean Architecture.

---

## 📱 App Preview

<p align="center">
  <img src="https://raw.githubusercontent.com/BEKO2210/Prompt-Manager_Android-App/main/assets/Live_preview%20(1).jpg" width="19%">
  <img src="https://raw.githubusercontent.com/BEKO2210/Prompt-Manager_Android-App/main/assets/Live_preview%20(3).jpg" width="19%">
  <img src="https://raw.githubusercontent.com/BEKO2210/Prompt-Manager_Android-App/main/assets/Live_preview%20(4).jpg" width="19%">
  <img src="https://raw.githubusercontent.com/BEKO2210/Prompt-Manager_Android-App/main/assets/Live_preview%20(5).jpg" width="19%">
  <img src="https://raw.githubusercontent.com/BEKO2210/Prompt-Manager_Android-App/main/assets/Live_preview%20(2).jpg" width="19%">
</p>


## 📋 Inhaltsverzeichnis

- [Features](#features)
- [Architektur](#architektur)
- [Technologie-Stack](#technologie-stack)
- [Projekt-Struktur](#projekt-struktur)
- [Kernkomponenten](#kernkomponenten)
- [Installation & Setup](#installation--setup)
- [Verwendung](#verwendung)
- [Erweiterungsmöglichkeiten](#erweiterungsmöglichkeiten)

---

## ✨ Features

### Kernfunktionalität

- **Prompt-Verwaltung**: Erstellen, bearbeiten, löschen und duplizieren von Prompts
- **Dynamische Platzhalter**: Verwende `[Label=Standardwert]` Syntax für flexible Prompts
- **Intelligentes Parsing**: Automatische Erkennung und Extraktion von Platzhaltern
- **Live-Preview**: Vorschau des ausgefüllten Prompts während der Eingabe
- **Validierung**: Warnung bei ungültiger Platzhalter-Syntax

### Organisation

- **Kategorien**: Organisiere Prompts nach Themen (E-Mail, Entwicklung, Social Media, etc.)
- **Favoriten**: Markiere häufig verwendete Prompts
- **Suche**: Volltextsuche über Titel, Beschreibung und Inhalt
- **Filter**: Nach Kategorie, Favoriten oder Nutzungshäufigkeit
- **Sortierung**: Nach Name, Datum oder Nutzung

### Benutzerfreundlichkeit

- **Material Design 3**: Moderne UI mit Dynamic Colors und Dark Mode
- **Jetpack Compose**: Flüssige Animationen und reaktive UI
- **Clipboard-Integration**: Ein-Klick-Kopie des ausgefüllten Prompts
- **Share-Funktionalität**: Teile Prompts mit anderen Apps
- **Template-Galerie**: 5 vorgefertigte Beispiel-Prompts beim ersten Start

---

## 🏗 Architektur

Die App folgt der **Clean Architecture** mit klarer Trennung von Verantwortlichkeiten:

```
┌─────────────────────────────────────────────────┐
│            Presentation Layer                   │
│  (Jetpack Compose UI + ViewModels)             │
│  • Screens: Liste, Details, Dialoge            │
│  • ViewModels: State Management mit Flow       │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│            Domain Layer                         │
│  (Business Logic)                               │
│  • Entities: Placeholder                        │
│  • Utils: PlaceholderParser, ClipboardHelper   │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│            Data Layer                           │
│  (Persistenz & Repositories)                    │
│  • Room Database mit PromptEntity              │
│  • Repository Pattern für Datenzugriff         │
└─────────────────────────────────────────────────┘
```

### MVVM Pattern

- **Model**: Room Entities (`PromptEntity`)
- **View**: Composable Functions (`PromptListScreen`, `PromptDetailScreen`)
- **ViewModel**: State Management (`PromptListViewModel`, `PromptDetailViewModel`)

### Datenhaltung

- **Room (SQLite)**: Für Prompt-Daten (zukunftssicher, skalierbar)
- **SharedPreferences/DataStore**: Für Settings (vorbereitet, noch nicht implementiert)

---

## 🛠 Technologie-Stack

| Bereich | Technologie | Version |
|---------|-------------|---------|
| UI Framework | Jetpack Compose | 2023.10.01 |
| Architektur | MVVM + Repository | - |
| Datenbank | Room | 2.6.1 |
| Navigation | Navigation Compose | 2.7.5 |
| Async | Kotlin Coroutines + Flow | 1.7.3 |
| Dependency Injection | Manual (bereit für Hilt) | - |
| Design System | Material Design 3 | - |
| Build System | Gradle Kotlin DSL | 8.2.0 |
| Language | Kotlin | 1.9.20 |

### Wichtige Gradle Dependencies

```kotlin
// Jetpack Compose
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.5")

// ViewModel & Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
```

---

## 📁 Projekt-Struktur

```
app/src/main/java/com/promptmanager/
│
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt           # Room Database Setup
│   │   ├── PromptEntity.kt          # Datenbank-Entity für Prompts
│   │   └── PromptDao.kt             # Data Access Object mit Queries
│   └── repository/
│       └── PromptRepository.kt      # Repository Pattern Implementation
│
├── domain/
│   └── model/
│       └── Placeholder.kt           # Domain Model für Platzhalter
│
├── presentation/
│   ├── screens/
│   │   ├── PromptApp.kt             # Navigation Root
│   │   ├── PromptListScreen.kt      # Hauptbildschirm (Liste)
│   │   └── PromptDetailScreen.kt    # Detail-/Bearbeiten-Screen
│   ├── components/
│   │   └── PlaceholderDialog.kt     # Dynamischer Platzhalter-Dialog
│   ├── viewmodel/
│   │   ├── PromptListViewModel.kt   # ViewModel für Liste
│   │   └── PromptDetailViewModel.kt # ViewModel für Details
│   └── theme/
│       ├── Theme.kt                 # Material 3 Theme
│       └── Type.kt                  # Typography
│
├── util/
│   ├── PlaceholderParser.kt         # Kern-Logik für Platzhalter
│   └── ClipboardHelper.kt           # Clipboard & Share Utils
│
└── MainActivity.kt                  # Single Activity Entry Point
```

---

## 🔑 Kernkomponenten

### 1. PlaceholderParser (Herzstück der App)

**Verantwortlich für:**
- Erkennung von Platzhaltern im Format `[Label=Default]` oder `[Label]`
- Extraktion eindeutiger Platzhalter (Duplikate werden zusammengeführt)
- Ersetzung aller Platzhalter mit Benutzereingaben
- Validierung der Platzhalter-Syntax

**Beispiel:**
```kotlin
val content = "Schreibe über [Thema=KI] für [Zielgruppe=Entwickler]"
val placeholders = PlaceholderParser.extractPlaceholders(content)
// Result: [Placeholder("Thema", "KI"), Placeholder("Zielgruppe", "Entwickler")]

val values = mapOf("Thema" to "Machine Learning", "Zielgruppe" to "Einsteiger")
val filled = PlaceholderParser.fillPlaceholders(content, values)
// Result: "Schreibe über Machine Learning für Einsteiger"
```

### 2. PlaceholderDialog (Dynamische UI-Generierung)

**Features:**
- Automatische Generierung von TextFields für jeden Platzhalter
- Intelligente Heuristik für MultiLine-Felder (Länge > 60 Zeichen)
- Live-Preview des ausgefüllten Prompts
- "Defaults wiederherstellen"-Funktion
- Validierung vor Bestätigung

**Flow:**
1. User klickt "Prompt nutzen"
2. Platzhalter werden extrahiert
3. Dialog zeigt dynamisches Formular
4. User füllt Felder aus (Defaults vorbelegt)
5. Preview zeigt finalen Text
6. Bei "Fertig": Prompt wird ausgefüllt + in Zwischenablage kopiert

### 3. Room Database

**Schema (Version 1):**

```kotlin
@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    val content: String,           // Prompt mit [Platzhaltern]
    val category: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0
)
```

**Migrations vorbereitet für:**
- Tags (Many-to-Many mit `PromptTagCrossRef`)
- Nutzungshistorie (`UsageHistoryEntity`)
- Versionierung von Prompts

### 4. Repository Pattern

**Vorteile:**
- Klare Trennung zwischen UI und Datenbank
- Einfach testbar (Mock-Repository)
- Einheitliche API für alle Datenoperationen
- Vorbereitet für Remote-Datenquellen

**Wichtige Methoden:**
```kotlin
// Lesen
fun getAllPrompts(): Flow<List<PromptEntity>>
fun searchPrompts(query: String): Flow<List<PromptEntity>>
suspend fun getPromptById(id: Long): PromptEntity?

// Schreiben
suspend fun insertPrompt(prompt: PromptEntity): Long
suspend fun updatePrompt(prompt: PromptEntity)
suspend fun deletePrompt(prompt: PromptEntity)

// Utilities
suspend fun duplicatePrompt(promptId: Long): Long?
suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
suspend fun markPromptAsUsed(id: Long)
```

---

## 🚀 Installation & Setup

### Voraussetzungen

- Android Studio Hedgehog (2023.1.1) oder neuer
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Build-Schritte

1. **Repository klonen:**
```bash
git clone <repository-url>
cd Anddroid_Promp_Lib
```

2. **Projekt in Android Studio öffnen:**
   - File → Open → Projektordner auswählen
   - Gradle Sync abwarten

3. **App bauen und starten:**
```bash
# Debug Build
./gradlew assembleDebug

# Auf Emulator/Device installieren
./gradlew installDebug

# Oder: Run-Button in Android Studio
```

4. **Tests ausführen (optional):**
```bash
./gradlew test           # Unit Tests
./gradlew connectedTest  # Instrumented Tests
```

### Erste Schritte

Beim ersten Start werden automatisch **5 Template-Prompts** erstellt:
1. E-Mail verfassen
2. Code Review durchführen
3. Social Media Post
4. Meeting-Notizen zusammenfassen
5. Lernzusammenfassung erstellen

Diese dienen als Beispiele und können angepasst oder gelöscht werden.

---

## 📖 Verwendung

### Prompt erstellen

1. Klicke auf den **+**-Button (FAB)
2. Fülle Titel und Prompt-Text aus
3. Verwende `[Platzhalter]` oder `[Label=Standardwert]` für dynamische Werte
4. Optional: Kategorie und Beschreibung hinzufügen
5. Speichern

**Beispiel-Prompt:**
```
Schreibe eine E-Mail an [Empfänger=Team] zum Thema [Thema].

Ton: [Ton=freundlich]

Kernpunkte:
[Kernpunkte]

Gruß,
[Name=Max Mustermann]
```

### Prompt nutzen

**Variante A: Aus der Liste**
1. Klicke auf "Prompt nutzen"-Button
2. Dialog öffnet sich mit Eingabefeldern
3. Fülle die Platzhalter aus (Defaults sind vorbelegt)
4. Klicke "Fertig & kopieren"
5. Prompt ist in der Zwischenablage → in ChatGPT/etc. einfügen

**Variante B: Aus dem Detail-Screen**
1. Öffne einen Prompt
2. Klicke "Testen & kopieren"
3. (gleicher Flow wie oben)

### Prompt-Platzhalter-Syntax

| Syntax | Beschreibung | Beispiel |
|--------|--------------|----------|
| `[Label]` | Platzhalter ohne Default | `[Thema]` |
| `[Label=Default]` | Mit Standardwert | `[Sprache=Deutsch]` |
| `[Label=Multi\nLine]` | MultiLine-Default (> 60 Zeichen) | `[Nachricht=Lange Nachricht...]` |

**Wichtig:**
- Duplikate (z.B. `[Thema]` mehrfach) werden nur 1x abgefragt
- Erster Default gewinnt bei Konflikten: `[Thema=KI]` und `[Thema=AI]` → "KI" wird verwendet
- Ungültige Syntax wird als normaler Text behandelt

---

## 🔮 Erweiterungsmöglichkeiten

Die Architektur ist vorbereitet für:

### Geplante Features

**Tags & Tagging-System:**
```kotlin
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(primaryKeys = ["promptId", "tagId"])
data class PromptTagCrossRef(
    val promptId: Long,
    val tagId: Long
)
```

**Nutzungshistorie:**
```kotlin
@Entity(tableName = "usage_history")
data class UsageHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val promptId: Long,
    val usedAt: Long,
    val filledValuesJson: String // JSON mit Platzhalter-Werten
)
```

**Cloud-Sync:**
- Repository-Pattern ermöglicht einfache Integration von Remote-Datenquellen
- Firebase Firestore oder eigene REST API

**Export/Import:**
- JSON-Export aller Prompts
- Import von Community-Prompts

**Erweiterte Platzhalter:**
- Dropdown-Auswahl: `[Sprache:Deutsch|Englisch|Französisch]`
- Pflichtfelder vs. Optional: `[Thema!]` vs. `[Beschreibung?]`
- Platzhalter-Gruppen: `[Meta:Sprache]`, `[Content:Thema]`

**AI-Integration:**
- Direkte Integration mit ChatGPT/Claude/etc. APIs
- Vorschau des AI-Outputs in der App

**Collaborative Features:**
- Prompts teilen mit QR-Code
- Community-Marketplace für Prompts

---

## 📄 Lizenz

MIT License - siehe LICENSE-Datei

---

## 🤝 Beiträge

Contributions sind willkommen! Bitte öffne ein Issue oder Pull Request.

### Entwicklungs-Guidelines

- Kotlin Coding Conventions befolgen
- Compose Best Practices nutzen
- Unit Tests für neue Features schreiben
- Material Design 3 Guidelines einhalten

---

## 📞 Support

Bei Fragen oder Problemen:
- Issue auf GitHub erstellen
- Dokumentation prüfen
- Code-Kommentare lesen (ausführlich dokumentiert)

---

**Built with ❤️ using Jetpack Compose & Clean Architecture**
