# Psych Notes

Aplicación de escritorio (Compose Desktop) para registrar fichas clínico-terapéuticas con soporte para SQLite, migraciones automatizadas mediante SQLDelight y capa de datos compartida en Kotlin Multiplatform.

## Características

- **Ficha digital completa**: replica la hoja clínica original con secciones para identificación, análisis de problemas, objetivos, evolución y tareas.
- **Adjuntos integrados**: arrastra archivos o usa el botón `+` para subirlos; el sistema los guarda en `~/.psych-notes/storage` y genera _tags_ inline (`[documento.pdf]`) dentro del campo de tareas.
- **Persistencia multiplataforma**: base de datos SQLite gestionada con SQLDelight y mapeos a modelos de dominio Kotlin.
- **Inyección de dependencias**: módulos de datos inicializados vía Koin (`AppInitializer` + `DatabaseModule`).

## Requisitos

- JDK 17 (`export JAVA_HOME=$(/usr/libexec/java_home -v 17)` en macOS).
- Gradle Wrapper (incluido).
- macOS / Windows / Linux con soporte para Compose Desktop.

## Estructura del proyecto

```
psych-notes/
├── shared/                # Módulo KMM con dominio, repositorios y migraciones SQLDelight
│   ├── src/commonMain/    # Modelos, viewmodels y esquemas SQL
│   ├── src/desktopMain/   # Driver JDBC (DesktopDatabaseFactory)
│   └── ...
├── desktopApp/            # Shell Compose Desktop (UI)
│   └── src/jvmMain/
├── docs/                  # Material de referencia (mockups, etc.)
├── build.gradle.kts       # Configuración raíz (plugins y repos)
└── README.md
```

## Puesta en marcha

```bash
git clone https://github.com/<usuario>/psych-notes.git
cd psych-notes
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS
./gradlew build
```

### Ejecutar la app de escritorio

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :desktopApp:run
```

Notas:

- La ventana se mantiene abierta mientras el gradle task corre; cerrala (o usa `Ctrl+C`) para liberar la terminal.
- La base de datos se crea en `~/.psych-notes/psych_notes.db` la primera vez que se ejecuta la app.

## Migraciones y esquema SQLDelight

1. Modifica/añade `.sq` en `shared/src/commonMain/sqldelight/` o `migrations/`.
2. Regenera el esquema y verifica migraciones:

   ```bash
   ./gradlew :shared:generateCommonMainPsychNotesDbSchema
   ./gradlew :shared:verifyCommonMainPsychNotesDbMigration
   ```

   Esto actualiza los archivos `databases/1.db` y `databases/2.db` usados por SQLDelight para los tests de migración.

## Desarrollo de la UI

- El campo de “Tareas” acepta texto libre y tags de adjuntos (`[archivo.ext]`).
- Al arrastrar o subir un archivo se crea el tag automáticamente y se registra en la base de datos.
- Los adjuntos se almacenan en `~/.psych-notes/storage/<patientId>/<sessionId>/` y se pueden abrir desde la UI.

## Scripts útiles

| Comando | Descripción |
| --- | --- |
| `./gradlew build` | Compila ambos módulos y ejecuta verificaciones |
| `./gradlew :desktopApp:run` | Lanza la aplicación Compose Desktop |
| `./gradlew :shared:allTests` | Ejecuta las pruebas del módulo compartido |

## Contribuir

1. Asegúrate de que las migraciones SQLDelight pasen (`verify...`).
2. Ejecuta `./gradlew build` antes de abrir un PR.
3. Sigue el estilo Kotlin (4 espacios, trailing commas donde sea útil) y los mensajes de commit convencionales (`feat:`, `fix:`, `chore:`...).

---

_Psych Notes_ facilita la trazabilidad clínica: cualquier mejora (reportes, sincronización, exportes) es bienvenida vía issues o PRs.

