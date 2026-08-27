# Octomind Books Reader

Aplicación Android de entrenamiento lector que ayuda a cada persona a aumentar su velocidad de lectura sin perder comprensión.

Octomind combina una guía visual de lectura, evaluaciones adaptativas y un tutor basado en IA. El usuario puede subir libros en EPUB, PDF o TXT; la aplicación procesa su contenido y crea una experiencia personalizada a partir de su desempeño.

> Estado del proyecto: primera fase funcional. Existe una aplicación Android local con importación TXT/EPUB, biblioteca, reanudación exacta, temas de página, calibración personal y Octomind Focus.

El home presenta la biblioteca como una estantería adaptable. En teléfonos organiza hasta tres cubiertas por repisa y en pantallas de al menos 600 dp utiliza cinco. Los EPUB muestran la portada declarada en sus metadatos cuando es una imagen válida; si no existe, no es compatible o no supera la validación local, el libro conserva la cubierta tipográfica determinista actual. No se consulta ningún servicio externo.

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

Al activar Focus, la barra con el título, el progreso y el panel de ajustes se ocultan para dedicar toda la pantalla a la lectura. No quedan botones visibles: un toque en la esquina superior izquierda vuelve a la biblioteca, otro en la esquina superior derecha sale de Focus y uno en el centro inferior abre sus ajustes. Las tres zonas conservan etiquetas para TalkBack. Al salir reaparece el lector normal con su menú colapsado.

El bloque activo se ancla al 42 % de la altura física de la pantalla, ligeramente por encima del centro, cuando ese punto está disponible dentro del área de lectura. Si el panel de ajustes ocupa esa posición, se mantiene dentro de la parte visible. Al cambiar de párrafo, orientación, fuente, tamaño o altura del menú, la posición se recalcula. El texto circundante puede atenuarse entre 0 % y 80 %, y el bloque activo conserva contraste completo en los temas claro, sepia y oscuro.

Al abrir un libro nuevo no aparece una etapa de calibración ni **Comenzar aquí**. El lector entra directamente desde la primera ubicación disponible y Focus se restaura si estaba activo. Fuera de Focus puede desplazarse libremente; dentro de Focus, el primer gesto queda disponible de inmediato. La ubicación estable del libro se conserva después de cada movimiento.

La sesión recopila de forma pasiva tiempo activo, fragmentos recorridos, palabras, progreso y retrocesos. Puede calcular un ritmo promedio informativo, pero ninguna métrica mueve el marcador ni modifica automáticamente la experiencia. Su reporte reutiliza la portada EPUB del libro y recurre a la cubierta tipográfica cuando no existe. Mientras Focus está activo y la aplicación permanece visible, Android mantiene la pantalla encendida. El menú comienza abierto en una instalación nueva; después conserva localmente si el usuario lo dejó abierto o colapsado. También persisten los temas, **Lora**, **Roboto**, **Roboto Mono**, tamaño del texto e intensidad de fondo.

El resumen de sesión comparte la identidad de la biblioteca: fondo y cabecera de madera, portada derivada del título, panel tipo papel y métricas en tarjetas cálidas. Es desplazable y reserva una barra inferior independiente para **Volver a la biblioteca**, por lo que la acción conserva su texto y altura aun cuando el contenido o las métricas ocupan más espacio que la pantalla.

**Octi** es una mascota opcional: un pulpo lector con lentes que ocupa un espacio reservado al centro de los controles inferiores, sin cubrir el texto ni el marcador. Cada gesto válido activa un pase de página de 320 ms; los movimientos rechazados no lo animan. Puede ocultarse desde los ajustes y la animación utiliza la escala de movimiento configurada en Android.

Focus ofrece dos presentaciones persistentes. **Octi narrador**, la opción inicial, atenúa el libro y presenta la oración actual completa en un globo de diálogo, como si Octi lo estuviera contando. **Sobre el texto** conserva el marcador integrado en la página y reúne su intensidad y mascota compacta en una sección propia. Ambos estilos usan exactamente las mismas oraciones, gestos, posición estable y estadísticas de sesión.

Octi narrador incorpora una **ambientación local** por capítulo. El título, el encabezado del capítulo y una muestra limitada de su texto se clasifican en el dispositivo como misterio, fantasía, ciencia ficción, romance, naturaleza, conocimiento o neutral. Compose dibuja degradados y formas abstractas sin enviar el libro a un servicio externo. El lector puede desactivar el fondo o elegir intensidad sutil o inmersiva; la preferencia queda guardada.

El lector puede elegir el **avatar del narrador** entre Octi, ilustraciones originales de H. P. Lovecraft, Arthur Schopenhauer, Friedrich Nietzsche y Albert Camus leyendo, o **Mi imagen** desde los ajustes de Focus. La asignación se guarda por libro: dos libros pueden compartir narrador o conservar elecciones diferentes, y cada uno restaura la suya al abrirse. La opción personalizada acepta PNG, JPG y WebP de hasta 10 MB, normaliza el archivo a PNG privado y lo presenta siempre dentro de un círculo. La foto nunca sale del dispositivo y puede reemplazarse o eliminarse desde el mismo menú. Eliminarla devuelve de forma segura a Octi todos los libros que la utilizaban. El avatar no modifica la navegación, el progreso ni las estadísticas.

La indicación de gestos situada debajo de Octi permanece hasta que el lector completa su primer desplazamiento vertical válido. Ese aprendizaje se guarda localmente y la leyenda no vuelve a aparecer al reabrir el libro; los movimientos que no alcanzan el umbral y los botones accesibles no la descartan.

La activación de Focus también es una preferencia persistente. Si el lector sale con Focus activo, el siguiente libro se abre con Focus restaurado; el tiempo de sesión permanece pausado mientras la aplicación no está visible.

## Formatos de libros

### EPUB

Formato preferido para el modo de entrenamiento. Permite reorganizar el texto, cambiar tipografía y conservar capítulos y ubicaciones.

### TXT

Se normaliza en párrafos y secciones antes de crear los fragmentos de lectura.

### PDF

Tendrá dos experiencias:

- **Vista original:** conserva el diseño y las páginas.
- **Modo entrenamiento:** reorganiza el texto para utilizar correctamente el marcador.

Los PDF escaneados requerirán OCR. Todo fragmento procesado debe conservar una referencia a su capítulo, página o ubicación original.

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
- [x] Importación local de EPUB y TXT.
- [x] Biblioteca y pantalla de lectura.
- [x] Octomind Focus manual por oraciones completas, controlado por gestos verticales.
- [x] Presentación opcional Octi narrador mediante globo de diálogo.
- [x] Ambientación local de Octi narrador por libro y capítulo.
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
```

La lectura sintética [samples/lectura-demo.txt](samples/lectura-demo.txt) permite recorrer la importación y Octomind Focus sin utilizar contenido protegido.

Consulta [AGENTS.md](AGENTS.md) antes de realizar cambios.
