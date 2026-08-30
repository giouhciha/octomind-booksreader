# Octomind Books Reader

Aplicación Android de entrenamiento lector que ayuda a cada persona a aumentar su velocidad de lectura sin perder comprensión.

Octomind combina una guía visual de lectura, evaluaciones adaptativas y un tutor basado en IA. El usuario puede subir libros en EPUB, PDF o TXT; la aplicación procesa su contenido y crea una experiencia personalizada a partir de su desempeño.

> Estado del proyecto: primera fase funcional. Existe una aplicación Android local con importación TXT, EPUB y PDF, biblioteca, reanudación exacta, temas de página y Octomind Focus.

La cabecera de la biblioteca ofrece un **Respaldo** manual. El usuario puede guardar en su Google Drive personal —mediante el selector seguro de archivos de Android— un paquete que contiene perfil, preferencias, libros, portadas, progreso, citas, sesiones e imagen personalizada. El paquete completo se cifra con una contraseña que Octomind no almacena. Restaurar valida primero su versión, rutas y límites y después reemplaza la biblioteca local; esta fase todavía no realiza sincronización automática ni combina bibliotecas.

El home presenta la biblioteca como una estantería adaptable. En teléfonos organiza hasta tres cubiertas por repisa y en pantallas de al menos 600 dp utiliza cinco. Los EPUB muestran la portada declarada en sus metadatos cuando es una imagen válida; si no existe, no es compatible o no supera la validación local, el libro conserva la cubierta tipográfica determinista actual. En Focus, avanzar después del último fragmento termina el libro; en lectura normal, llegar mediante desplazamiento al fondo hace lo mismo. La portada muestra entonces una marca verde de lectura completada. Al tocarla se abre su resumen; **Leer de nuevo** inicia otro ciclo desde cero bajo la misma portada y conserva localmente las métricas de lecturas anteriores. No se consulta ningún servicio externo.

## Objetivo

Entrenar al usuario para alcanzar una comprensión medida cercana o superior al 90%, aumentando gradualmente su velocidad efectiva de lectura.

La velocidad efectiva se define como:

```text
velocidad efectiva = palabras por minuto × porcentaje de comprensión
```

El 90% es un objetivo medido mediante evaluaciones; no debe presentarse como una garantía absoluta de aprendizaje o retención.

## Propuesta de valor

- Biblioteca privada con libros EPUB, PDF y TXT.
- Aplicación Android nativa y utilizable durante la lectura sin conexión.
- Marcador visual que guía la mirada por palabras, bloques o líneas.
- Velocidad ajustable y adaptación gradual según la comprensión.
- Preguntas con evidencia verificable dentro del libro.
- Tutor que explica fragmentos sin perder el contexto de la obra.
- Perfil lector personal basado en resultados, pausas y retención.
- Sincronización segura del avance entre sesiones.

## Ciclo de aprendizaje

Cada capítulo sigue un ciclo corto:

1. **Preparar:** presentar objetivos, conceptos y preguntas guía.
2. **Leer:** acompañar el texto con el marcador inteligente.
3. **Comprobar:** realizar preguntas breves durante y después de la lectura.
4. **Reforzar:** explicar los conceptos que el usuario no dominó.
5. **Adaptar:** ajustar velocidad, tamaño de bloque y frecuencia de evaluación.

## Octomind Focus

`Octomind Focus` es una guía manual de lectura. En la prueba actual muestra oraciones completas: ignora comas, punto y coma, dos puntos y guiones, y avanza únicamente al encontrar punto, interrogación, exclamación, puntos suspensivos o final de párrafo. No impone palabras por minuto ni utiliza un temporizador para avanzar.

La segmentación reconoce iniciales y abreviaturas antes de interpretar un punto como cierre. Firmas, direcciones y posdatas como `R.F.D. #2`, `Albert N. Wilmarth`, `H. W. A.` o `P.D. Estoy…` permanecen unidas al contexto que continúa; el final real de una oración o párrafo conserva su pausa natural.

Con Focus activo, un desplazamiento hacia arriba avanza exactamente al siguiente fragmento y uno hacia abajo regresa al anterior. Los movimientos pequeños se ignoran para evitar cambios accidentales; los botones de navegación permanecen como alternativa accesible. El scroll libre se conserva cuando Focus está desactivado.

Al activar Focus, la barra con el título, el progreso y el panel de ajustes se ocultan para dedicar toda la pantalla a la lectura. No quedan botones visibles: un toque en la esquina superior izquierda vuelve a la biblioteca, otro en la esquina superior derecha sale de Focus y uno en el centro inferior abre sus ajustes. Las tres zonas conservan etiquetas para TalkBack. Al salir reaparece el lector normal con su menú colapsado. En el modo **Narrador**, una animación local y breve gira únicamente el globo como una tarjeta de papel al cambiar de fragmento; todos los avatares, incluido Octi, permanecen completamente estáticos. La lectura normal y Focus sobre el texto no aplican este efecto.

El bloque activo se ancla al 42 % de la altura física de la pantalla, ligeramente por encima del centro, cuando ese punto está disponible dentro del área de lectura. Si el panel de ajustes ocupa esa posición, se mantiene dentro de la parte visible. Al cambiar de párrafo, orientación, fuente, tamaño o altura del menú, la posición se recalcula. El texto circundante puede atenuarse entre 0 % y 80 %, y el bloque activo conserva contraste completo en los temas claro, sepia y oscuro.

Al abrir un libro nuevo no aparece una etapa de calibración ni **Comenzar aquí**. El lector entra directamente desde la primera ubicación disponible y Focus se restaura si estaba activo. Fuera de Focus puede desplazarse libremente; dentro de Focus, el primer gesto queda disponible de inmediato. La ubicación estable del libro se conserva después de cada movimiento.

La sesión recopila de forma pasiva tiempo activo, fragmentos recorridos, palabras, progreso y retrocesos. Estas métricas se acumulan entre aperturas dentro del mismo ciclo de lectura: cerrar el libro o dejar la aplicación en segundo plano guarda un punto de control, pero el tiempo transcurrido mientras no se lee nunca se suma. Al completar el libro, el acumulado se archiva en su historial; **Leer de nuevo** inicia otro ciclo con estadísticas en cero. Puede calcular un ritmo promedio informativo, pero ninguna métrica mueve el marcador ni modifica automáticamente la experiencia. Su reporte reutiliza la portada EPUB del libro y recurre a la cubierta tipográfica cuando no existe. Mientras Focus está activo y la aplicación permanece visible, Android mantiene la pantalla encendida. El menú comienza abierto en una instalación nueva; después conserva localmente si el usuario lo dejó abierto o colapsado. También persisten los temas, **Lora**, **Roboto**, **Roboto Mono**, tamaño del texto e intensidad de fondo.

La **música ambiental** es opcional y funciona tanto en lectura normal como en Focus. Desde los ajustes puede habilitarse un control compacto en la esquina inferior izquierda, elegir Concentración, Lluvia, Ruido marrón o Noche tranquila y limitar el volumen interno entre 0 % y 50 %. El sonido se sintetiza localmente, no contiene grabaciones ni requiere red, y su selección queda incluida en el respaldo cifrado. La reproducción nunca comienza automáticamente: el lector debe iniciarla en cada sesión y Android la pausa si la aplicación deja de estar visible o pierde el foco de audio.

El resumen de sesión comparte la identidad de la biblioteca: fondo y cabecera de madera, portada derivada del título, panel tipo papel y métricas en tarjetas cálidas. Es desplazable y reserva una barra inferior independiente para **Volver a la biblioteca**, por lo que la acción conserva su texto y altura aun cuando el contenido o las métricas ocupan más espacio que la pantalla.

**Octi** es una mascota opcional: un pulpo lector con lentes que ocupa un espacio reservado al centro de los controles inferiores, sin cubrir el texto ni el marcador. Permanece estático al avanzar o retroceder y puede ocultarse desde los ajustes.

Focus ofrece dos presentaciones persistentes. **Narrador**, la opción inicial, atenúa el libro y presenta la oración actual completa en un globo de diálogo mediante el avatar elegido. **Sobre el texto** conserva el marcador integrado en la página y reúne su intensidad y mascota compacta en una sección propia. Ambos estilos usan exactamente las mismas oraciones, gestos, posición estable y estadísticas de sesión.

El Narrador conserva siempre el tamaño y el interlineado elegidos por el lector. Cuando una oración o párrafo excede el área legible, lo distribuye en tarjetas de continuación calculadas con el ancho, alto, fuente y tamaño reales de la pantalla. Prefiere cortar después de puntuación natural y muestra un indicador discreto como `1 de 3`. Avanzar entre continuaciones no adelanta el progreso ni cuenta otra oración; el bloque lógico cambia únicamente al abandonar su última tarjeta. Los gestos y controles accesibles permiten recorrerlas en ambos sentidos.

El modo Narrador incorpora una **ambientación local** por capítulo. El título, el encabezado del capítulo y una muestra limitada de su texto se clasifican en el dispositivo como misterio, fantasía, ciencia ficción, romance, naturaleza, conocimiento o neutral. Compose dibuja degradados y formas abstractas sin enviar el libro a un servicio externo. El lector puede desactivar el fondo o elegir intensidad sutil o inmersiva; la preferencia queda guardada.

El lector puede elegir el **avatar del narrador** entre Octi, ilustraciones originales de H. P. Lovecraft, Arthur Schopenhauer, Friedrich Nietzsche, Albert Camus, Stranger, Lila, Achu! y frank-n-furter leyendo, o **Mi imagen** desde los ajustes de Focus. La asignación se guarda por libro: dos libros pueden compartir narrador o conservar elecciones diferentes, y cada uno restaura la suya al abrirse. La opción personalizada acepta PNG, JPG y WebP de hasta 10 MB, normaliza el archivo a PNG privado y lo presenta siempre dentro de un círculo. La foto no sale del almacenamiento privado salvo cuando el usuario crea expresamente un respaldo cifrado; puede reemplazarse o eliminarse desde el mismo menú. Eliminarla devuelve de forma segura a Octi todos los libros que la utilizaban. El avatar no modifica la navegación, el progreso ni las estadísticas.

En los narradores incluidos, la cubierta del libro que sostienen adopta localmente el color dominante de la portada que se está leyendo. El tratamiento se limita a la región turquesa de la ilustración y conserva luces, sombras, páginas y detalles; no altera rostro, ropa ni fondo. Si el libro no tiene una portada válida, reutiliza la paleta tipográfica determinista de la biblioteca. **Mi imagen** permanece sin modificaciones. La tarjeta visual de una cita utiliza la misma personalización.

La cabecera de la biblioteca incluye **Mis citas**, una colección local agrupada por libro. En lectura normal, una pulsación prolongada selecciona la palabra inicial; sin soltar, el lector arrastra para ampliar el rango y al levantar el dedo guarda exactamente ese texto. Durante el arrastre, Android muestra una lupa que sigue el punto de selección en dispositivos compatibles. La selección se limita al párrafo actual para conservar un rango estable. En cualquiera de los estilos Focus, mantener presionado el fragmento o globo guarda el bloque completo. Cada cita conserva el texto, capítulo y rango estable de caracteres, evita duplicados exactos y puede eliminarse desde la colección. Tocarla abre una vista del libro en su ubicación exacta; explorar esa vista o regresar a la colección no modifica el progreso ni las estadísticas actuales. El botón de compartir permite enviar la cita como texto o generar localmente una tarjeta PNG con el narrador asignado, un globo, el libro, capítulo y firma discreta de Octomind. La imagen personalizada también se admite y el archivo compartible se limita al directorio privado de caché antes de delegar el destino a la hoja nativa de Android.

La indicación de gestos situada debajo de Octi permanece hasta que el lector completa su primer desplazamiento vertical válido. Ese aprendizaje se guarda localmente y la leyenda no vuelve a aparecer al reabrir el libro; los movimientos que no alcanzan el umbral y los botones accesibles no la descartan.

La activación de Focus también es una preferencia persistente. Si el lector sale con Focus activo, el siguiente libro se abre con Focus restaurado; el tiempo de sesión permanece pausado mientras la aplicación no está visible.

## Formatos de libros

### EPUB

Formato preferido para el modo de entrenamiento. Permite reorganizar el texto, cambiar tipografía y conservar capítulos y ubicaciones.

### TXT

Se normaliza en párrafos y secciones antes de crear los fragmentos de lectura.

### PDF

Incluye dos experiencias locales y conectadas por anclas de página:

- **Vista original:** conserva el diseño y las páginas.
- **Modo entrenamiento:** reorganiza el texto para utilizar correctamente el marcador.

La importación guarda una copia privada del original, extrae metadatos y texto seleccionable, crea la portada a partir de la primera página y relaciona cada página con un desplazamiento estable del texto. Los índices conservan cada capítulo como una entrada independiente, unen títulos continuados en la línea siguiente y distinguen visualmente encabezados y secciones. Los PDF importados con una normalización anterior se reprocesan una sola vez al abrirlos, conservando progreso y citas. Desde el lector se puede alternar entre **Página original** y **Lectura adaptada**; cambiar de página actualiza el mismo progreso y al volver permanecen disponibles Focus, el narrador elegido, temas y estadísticas. Los PDF escaneados sin texto seleccionable se rechazan con una explicación porque todavía requieren OCR. El original queda incluido automáticamente en el respaldo cifrado de la biblioteca.

## Medición de comprensión

La evaluación debe combinar:

- Comprensión literal de hechos e ideas explícitas.
- Inferencia y relación entre conceptos.
- Aplicación, síntesis y explicación con palabras propias.
- Retención diferida en una sesión posterior.

Las preguntas generadas por IA deben incluir una respuesta esperada, una rúbrica y referencias al texto que las respalda. No se debe penalizar automáticamente una respuesta abierta únicamente por diferencias de redacción.

## Personalización

El MVP no entrenará un modelo fundacional por cada usuario. Mantendrá un perfil lector estructurado con:

- Velocidad cómoda por tipo y dificultad de contenido.
- Comprensión y retención históricas.
- Conceptos dominados y conceptos débiles.
- Tipos de preguntas con mayor dificultad.
- Tamaño de bloque y frecuencia de pausas preferidos.
- Sesiones, retrocesos y fragmentos releídos.

Los ajustes globales del producto solamente deben utilizar información anónima y agregada, con consentimiento explícito cuando corresponda.

## Arquitectura propuesta

### Android

- Kotlin.
- Jetpack Compose.
- Motor local para el marcador y la lectura sin conexión.
- Base de datos local para libros procesados, avance y evaluaciones descargadas.
- Procesamiento en segundo plano para cargas y sincronización.

### AWS

- Amazon Cognito para identidad y acceso.
- Amazon S3 para originales y contenido procesado cifrado.
- API Gateway como entrada a la API.
- AWS Lambda y, cuando el procesamiento lo requiera, ECS Fargate.
- EventBridge y Step Functions para coordinar la ingesta de documentos.
- Amazon Textract como OCR para PDF escaneado.
- Aurora PostgreSQL Serverless para usuarios, libros, progreso y perfiles.
- `pgvector` o una base de conocimiento equivalente para recuperación semántica.
- Amazon Bedrock para tutoría, evaluaciones y explicaciones.
- Bedrock Guardrails para contenido, datos sensibles e intentos de inyección de instrucciones.

Flujo general:

```text
Android -> carga firmada en S3 -> flujo de procesamiento
        -> extracción y normalización -> capítulos y fragmentos
        -> embeddings y evaluaciones -> sincronización con Android
        -> lectura local -> métricas -> perfil lector adaptativo
```

El avance del marcador se calcula en el dispositivo. No se debe realizar una llamada al LLM por cada palabra o bloque.

## Estructura del repositorio

```text
.
├── app/              # Aplicación Android
├── samples/          # Lecturas sintéticas para pruebas manuales
├── docs/             # Decisiones, flujos y documentación del producto
├── services/         # Procesamiento y API de backend (fase futura)
├── infrastructure/   # Infraestructura como código para AWS (fase futura)
├── AGENTS.md         # Reglas de colaboración e implementación
└── README.md
```

Los directorios de servicios e infraestructura se agregarán cuando comience la fase de nube.

## Fases del MVP

### Fase 1: lector local

- [x] Proyecto Android base.
- [x] Confirmación de mayoría de edad.
- [x] Importación local de EPUB, PDF y TXT.
- [x] Biblioteca y pantalla de lectura.
- [x] Octomind Focus manual por oraciones completas, controlado por gestos verticales.
- [x] Presentación opcional Narrador mediante globo de diálogo.
- [x] Ambientación local del modo Narrador por libro y capítulo.
- [x] Avatar de narrador seleccionable y persistente.
- [x] Registro local y reanudación exacta del progreso por libro.
- [x] Temas de página claro, sepia y oscuro.
- [x] Selección del inicio real, calibración por toques y perfil lector local.
- [x] Pruebas unitarias y validación en emulador.

### Fase 2: cuentas y nube

- Registro e inicio de sesión.
- Carga segura a S3.
- Procesamiento asíncrono de EPUB, TXT y PDF.
- Sincronización del avance.

### Fase 3: comprensión e IA

- Diagnóstico inicial.
- Preguntas fundamentadas en el libro.
- Tutor contextual.
- Perfil lector y adaptación de velocidad.
- Evaluaciones de retención.

### Fase 4: validación

- Pruebas con lectores reales.
- Calibración del marcador y las evaluaciones.
- Medición de velocidad efectiva y retención.
- Accesibilidad, rendimiento, privacidad y control de costos.

## Seguridad y derechos de autor

- El usuario debe confirmar que tiene derecho a utilizar el archivo.
- Los libros son privados y nunca se comparten entre cuentas.
- Los objetos, datos y respaldos deben estar cifrados.
- Cada consulta y fragmento debe estar aislado por usuario y libro.
- El usuario puede eliminar el original, el texto procesado y sus embeddings.
- Los logs no deben almacenar libros completos, credenciales ni información personal.
- Los datos de lectura no se utilizan para entrenamiento global sin consentimiento.

## Principios del producto

1. Comprensión antes que velocidad.
2. La IA debe estar fundamentada en el texto.
3. El lector debe funcionar sin conexión después de procesar el libro.
4. La personalización debe ser explicable y controlable.
5. Privacidad y aislamiento por usuario desde el diseño inicial.
6. El marcador debe sentirse fluido y nunca impedir que el usuario pause o retroceda.

## Métricas de éxito

- Velocidad efectiva, no solamente palabras por minuto.
- Comprensión inmediata por capítulo.
- Retención después de 24 horas o más.
- Porcentaje de libros y capítulos terminados.
- Frecuencia de pausas, retrocesos y abandono.
- Latencia y costo de procesamiento por libro.
- Porcentaje de respuestas del tutor con referencias válidas.

## Desarrollo

### Requisitos

- Android Studio compatible con Android Gradle Plugin 9.2.
- JDK 17 o posterior.
- Android SDK 37 para compilar.
- Un dispositivo o emulador con Android 8.0 (API 26) o posterior.

### Compilar

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

En macOS o Linux:

```bash
./gradlew assembleDebug
```

El APK de desarrollo se genera en `app/build/outputs/apk/debug/app-debug.apk`.

### Pruebas y análisis

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat ktlintCheck
.\gradlew.bat detekt
.\gradlew.bat --no-configuration-cache :app:dependencyCheckAnalyze
```

`dependencyCheckAnalyze` requiere una [clave de la NVD](https://nvd.nist.gov/developers/request-an-api-key) para consultar su base. En Jenkins debe guardarse como una credencial de tipo **Secret text** con el identificador `nvd-api-key`; el pipeline la expone temporalmente como `NVD_API_KEY`. Los hallazgos con CVSS 7 o superior detienen el pipeline. Gitleaks revisa el historial Git mediante `scripts/jenkins/Invoke-Gitleaks.ps1` y valida el checksum del binario fijado antes de ejecutarlo.

Jenkins no utiliza emulador. Después de lint, formato, análisis estático, seguridad y pruebas unitarias, construye y archiva un APK debug identificable. La versión base se define en `app/build.gradle.kts`; cada build de Jenkins agrega su número, por ejemplo `0.51.0.123`, y produce `app/build/outputs/jenkins/octomind-booksreader-0.51.0.123.apk`. El mismo valor queda grabado como `versionName`, mientras `versionCode` se calcula de forma monotónica para permitir actualizaciones entre compilaciones.

Los baselines de Ktlint y Detekt registran únicamente la deuda existente. Una infracción nueva falla la validación; no se debe regenerar un baseline para ocultar un hallazgo sin revisarlo.

La lectura sintética [samples/lectura-demo.txt](samples/lectura-demo.txt) permite recorrer la importación y Octomind Focus sin utilizar contenido protegido.

Consulta [AGENTS.md](AGENTS.md) antes de realizar cambios.
