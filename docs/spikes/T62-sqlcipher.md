# T62 · Cifrado de la base local con SQLCipher

- **Historia:** US-29, spike de la estrategia de encriptación de la base SQLite local.
- **Responsables:** Becker Caisahuana; colabora Angel Rocca.
- **Fecha:** 2026-11-21.
- **Estado:** decisión propuesta, pendiente de aprobación del equipo.

## Contexto

La aplicación de campo guarda en `inmonode-field.db` (Room3) los datos que el agente registra sin conexión:

- **Prospectos:** DNI, nombre, teléfono y estado civil.
- **Separaciones:** montos y condiciones negociadas (US-58).
- **Vouchers:** datos leídos por OCR (banco, número de operación, monto).
- **Portafolio** descargado.

Las fotos de los vouchers (`files/vouchers/`) y del documento de identidad (`files/identity-documents/`, US-59) son archivos aparte, no filas de la base.

Hoy la base no está cifrada.

- **Lo que ya la protege:** el cifrado por archivos de Android (FBE), mientras el teléfono está apagado o aún no se ha desbloqueado desde que se encendió.
- **Lo que no la protege:** un teléfono rooteado, una extracción con el teléfono desbloqueado, y las copias de seguridad.
- **Copias de seguridad:** `backup_rules.xml` y `data_extraction_rules.xml` solo excluyen la sesión. La base y las fotos viajan en la copia en la nube y en la transferencia entre dispositivos.

Estos datos son datos personales protegidos por la Ley N.° 29733.

## Qué se evaluó

- **Librería:** `net.zetetic:sqlcipher-android:4.20.0`, edición Community con licencia de estilo BSD.
  - Trae `SQLCipherDriver`, que implementa `androidx.sqlite.SQLiteDriver`.
  - Se conecta a Room3 con `Room.databaseBuilder(...).setDriver(...)` sin cambiar entidades, DAOs ni migraciones.
- **Medición:** `LocalDatabaseEncryptionBenchmark` (androidTest) con cuatro variantes:
  - la base como la construye la app hoy;
  - `AndroidSQLiteDriver` sin cifrar;
  - SQLCipher con una clave cruda de 256 bits;
  - SQLCipher con una frase (PBKDF2).
- **Carga de cada variante:** un día de campo.
  - 2 000 lotes en una transacción, como la descarga del portafolio.
  - 300 prospectos y 300 separaciones registrados uno por uno.
  - Luego se reabre en frío y se lee 10 veces el portafolio.
- **Prueba de seguridad:** el archivo cifrado no tiene la cabecera de SQLite, y no lo abren el driver normal ni SQLCipher con otra clave.

## Resultados

### Tamaño del APK de release

Medido sin firmar y con R8 apagado, como se construye hoy.

| | Todas las ABI (hoy) | Solo `arm64-v8a` y `armeabi-v7a` |
|---|---:|---:|
| Sin SQLCipher | 55.92 MiB | 33.59 MiB |
| Con SQLCipher | 63.31 MiB (+7.39 MiB, +13 %) | 36.67 MiB (+3.08 MiB, +9 %) |

- Casi todo el aumento es `libsqlcipher.so`, que se empaqueta sin comprimir (minSdk 24). Pesa 2.0 MiB en arm64-v8a, 1.0 MiB en armeabi-v7a y 2.2 MiB en cada ABI x86. Las clases Java suman unos 120 KB.
- Las ABI x86 pesan más de lo que agrega SQLCipher, sobre todo por el OCR de ML Kit (11 MiB por ABI). Esas ABI solo sirven para el emulador.
- El archivo de la base pasa de 528 KB a 536 KB (+1.5 %).

### Rendimiento

Medido en el emulador Pixel 10a (x86_64, Android 17). Cada celda es la mediana de 5 repeticiones, con el rango de 3 corridas. La escritura incluye la primera apertura.

| Variante | Escritura (ms) | Apertura en frío (ms) | Lectura ×10 (ms) |
|---|---:|---:|---:|
| Room por defecto (la app hoy) | 1241–1256 | 16–17 | 138–151 |
| `AndroidSQLiteDriver`, sin cifrar | 1166–1203 | 17–18 | 130–145 |
| SQLCipher, clave de 256 bits | 1149–1212 | 18–19 | 130–152 |
| SQLCipher, frase con PBKDF2 | 1780–1824 | 663–677 | 129–138 |

- **Clave cruda:** con una clave de 256 bits, el costo de cifrar queda dentro del ruido de la medición.
- **Frase:** cada apertura pasa por PBKDF2 (256 000 iteraciones) y tarda unos 650 ms más. Como la clave se genera al azar, no hace falta derivarla: se usa la forma cruda `x'…'`.
- **Límite de la medición:** el emulador usa la CPU del computador, con AES por hardware. Un teléfono ARM de gama media será más lento.
  - Antes de publicar el cambio hay que repetir la medición en el teléfono de pruebas, durante T87 y con permiso de su dueño.

## Gestión de la clave con Android Keystore

1. **Clave de la base:** en la primera ejecución se generan 32 bytes con `SecureRandom`.
2. **Clave que la envuelve:** se cifra con una clave AES-256-GCM del Android Keystore (alias `inmonode-field-db`). Esa clave no se puede exportar. Se pide StrongBox cuando el teléfono lo tiene (API 28+) y, si no, se usa el TEE.
   - **Sin desbloqueo obligatorio:** no se activan `setUserAuthenticationRequired` ni `setUnlockedDeviceRequired`, porque la sincronización en segundo plano (US-11) abre la base con el teléfono bloqueado.
3. **Dónde se guarda:** el IV y la clave cifrada van en `noBackupFilesDir`, que el sistema nunca incluye en las copias de seguridad.
4. **Al abrir:** se descifra la clave, se pasa a `SQLCipherDriver` como `x'<hex>'` y se borra el arreglo de bytes.
5. **Una clave por instalación:** la base ya separa los registros por agente (`ownerId`). Cerrar sesión no borra la base, porque puede tener registros pendientes.
6. **Si la clave se pierde:** puede pasar al restablecer el Keystore o al restaurar los datos en otro teléfono. En ese caso la base no se puede abrir: se borra y se vuelve a descargar el portafolio.
   - Los registros que no se sincronizaron se pierden. Por eso la base también debe quedar fuera de las copias de seguridad.

No se usa la contraseña del agente para derivar la clave:

- el inicio de sesión es en línea (JWT) y la sincronización en segundo plano no la tiene;
- cada cambio de contraseña obligaría a recifrar la base;
- volvería a pagar PBKDF2 en cada apertura.

## Migración de las bases que ya existen

Los agentes ya tienen una base sin cifrar, quizá con registros pendientes. Antes de construir Room, una sola vez:

1. **Detectar:** si `inmonode-field.db` empieza con la cabecera `SQLite format 3`, todavía no está cifrada.
2. **Exportar:**
   - Abrirla con SQLCipher sin clave.
   - Adjuntar la base cifrada con `ATTACH DATABASE '<temporal>' AS encrypted KEY "x'…'"`.
   - Ejecutar `SELECT sqlcipher_export('encrypted')`.
   - Copiar `PRAGMA user_version`, que Room usa como versión del esquema.
   - Ejecutar `DETACH`.
3. **Reemplazar:** cambiar el archivo por el temporal con un `rename` y borrar `-wal` y `-shm`.
4. **Probar:** con una prueba instrumentada, migrar una base sin cifrar que tenga una separación pendiente. Después debe leerse con la clave y no sin ella.

## Lo que SQLCipher no cubre

- **Las fotos:** los vouchers y el documento de identidad son archivos JPEG en `filesDir`. La foto del documento es el dato más sensible de la app.
  - Cifrarlas requiere otra pieza: `StreamingAead` de Tink, con su keyset protegido por el mismo Keystore.
  - `androidx.security:security-crypto` (`EncryptedFile`) está deprecada.
- **Las copias de seguridad:** con o sin SQLCipher, conviene excluir de la copia en la nube y de la transferencia entre dispositivos:
  - `database/inmonode-field.db*`;
  - `file/vouchers/`;
  - `file/identity-documents/`.

## Decisión propuesta

Adoptar SQLCipher en InmoNode-Mobile:

1. **Driver:** `SQLCipherDriver` en `DatabaseModule`, con la clave cruda de 256 bits envuelta por Android Keystore como se describe arriba.
2. **ABI de release:** limitarlas a `arm64-v8a` y `armeabi-v7a`. Los teléfonos de campo son ARM, y el emulador x86_64 sigue usando el build de debug. El APK queda en 36.7 MiB, 19 MiB menos que hoy aun con SQLCipher.
3. **Copias de seguridad:** excluir la base y las fotos.
4. **Migración:** hacer una sola vez la migración de la base existente con `sqlcipher_export`.
5. **Medición en el teléfono:** repetir `LocalDatabaseEncryptionBenchmark` en el teléfono de pruebas antes de publicar.

Tareas nuevas que propone el spike para el backlog:

- **Implementar el cifrado de la base:** driver, clave, migración y pruebas, unos 5 puntos.
- **Cifrar las fotos con Tink:** unos 3 puntos.

### Alternativas descartadas

- **No cifrar y confiar en FBE:** no protege un teléfono rooteado o desbloqueado, ni las copias de seguridad, y la app guarda DNI y fotos de documentos.
- **Frase con PBKDF2:** suma unos 650 ms a cada apertura sin dar más seguridad, porque la clave ya es aleatoria.
- **Cifrar columnas sueltas:** impide buscar por DNI con el índice actual, y cada consulta necesitaría código propio de cifrado.
