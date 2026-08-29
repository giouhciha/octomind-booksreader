# 0015. Respaldo cifrado controlado por el usuario

## Contexto

La biblioteca local ya concentra los libros importados, portadas, progreso, citas, ciclos e historial de lectura. Las preferencias y el perfil lector se conservan en DataStore, y el avatar personalizado en almacenamiento privado. El usuario necesita recuperar todo el perfil y desea utilizar su Google Drive personal.

Otorgar a la aplicación acceso directo y permanente a Drive exige identidad OAuth, configuración en Google Cloud y una política de sincronización y conflictos. Esta primera versión necesita ser funcional sin backend, sin secretos dentro de Android y sin ampliar permisos innecesariamente.

## Opciones consideradas

1. Integración directa con Google Drive API y sincronización automática.
2. Android Storage Access Framework para que el usuario elija explícitamente dónde guardar y desde dónde restaurar.
3. Android Auto Backup, sin control suficiente sobre libros y restauraciones manuales.

## Decisión

Usar Storage Access Framework y un único archivo `.octomind`. El selector del sistema puede entregar el archivo a Google Drive u otro proveedor instalado sin que Octomind reciba acceso general a la cuenta.

El contenido se empaqueta en ZIP y se cifra íntegramente con AES-256-GCM. La clave se deriva de una contraseña elegida por el usuario mediante PBKDF2-HMAC-SHA256, sal aleatoria y 210 000 iteraciones. La contraseña nunca se persiste ni se registra.

El respaldo incluye:

- manifiesto y versión de formato;
- perfil y preferencias del lector;
- metadatos, contenido y portadas de la biblioteca;
- progreso, citas, sesiones e historial incluidos en los metadatos por libro;
- avatar personalizado, si existe.

La restauración se ofrece únicamente desde la biblioteca. Primero descifra y extrae en caché, limita entradas y tamaño total, bloquea rutas externas y valida el manifiesto y metadatos. Solo después reemplaza los directorios privados y aplica las preferencias.

## Consecuencias

- Funciona sin permiso de Internet ni credenciales de Google dentro de la aplicación.
- El usuario decide expresamente el archivo y su ubicación en cada operación.
- Perder la contraseña hace imposible recuperar el respaldo.
- Esta versión no sincroniza automáticamente ni combina dos bibliotecas: restaurar reemplaza el estado local.
- El archivo puede almacenarse fuera de Drive si el usuario elige otro proveedor del sistema.

## Reversión o evolución

El contenedor está versionado. Una futura sincronización directa con Drive puede reutilizar el mismo archivo cifrado y añadir identidad, conflictos e historial remoto detrás de una interfaz propia, sin cambiar el dominio de lectura.
