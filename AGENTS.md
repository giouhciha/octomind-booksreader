# AGENTS.md

Este archivo define las reglas de trabajo para cualquier agente o colaborador que modifique Octomind Books Reader. Aplica a todo el repositorio salvo que un directorio incluya un `AGENTS.md` más específico.

## Misión

Construir una aplicación Android que aumente la velocidad efectiva de lectura mediante una guía visual y entrenamiento adaptativo, protegiendo siempre la comprensión, la privacidad y el control del usuario.

## Prioridades

Ante decisiones en conflicto, utilizar este orden:

1. Privacidad, seguridad y aislamiento de datos.
2. Comprensión y precisión pedagógica.
3. Accesibilidad y control del lector.
4. Funcionamiento sin conexión y resiliencia.
5. Rendimiento y costos de AWS/LLM.
6. Velocidad de entrega.

No optimizar palabras por minuto a costa de comprensión o retención.

## Estado actual

El repositorio comienza sin implementación. Las decisiones confirmadas son:

- El producto principal es una aplicación Android.
- Kotlin y Jetpack Compose son la opción predeterminada para Android.
- Se aceptarán EPUB, PDF y TXT.
- La lectura Focus utiliza fragmentos delimitados por puntuación y avance manual por gestos.
- La mascota de Focus es opcional y sus animaciones nunca deben bloquear gestos ni cubrir el marcador.
- El estilo Octi narrador cambia únicamente la presentación; comparte navegación, progreso y métricas con el marcador sobre texto.
- La ambientación local utiliza únicamente metadatos y muestras en memoria; no registra ni transmite contenido del libro.
- El marcador funciona localmente y no depende de llamadas continuas al backend.
- AWS alojará autenticación, almacenamiento, procesamiento e IA.
- Amazon Bedrock será la interfaz principal con modelos fundacionales.

Antes de introducir otra plataforma, lenguaje o proveedor, documentar por qué la opción actual no resuelve el requisito.

## Reglas del dominio

Estas condiciones son invariantes del producto:

- Toda posición de lectura debe poder relacionarse con una ubicación estable del libro.
- Todo fragmento derivado debe conservar `userId`, `bookId` y su ubicación de origen.
- Ninguna búsqueda semántica puede recuperar contenido perteneciente a otro usuario.
- Toda pregunta generada debe tener evidencia, respuesta esperada y rúbrica.
- Toda explicación sobre el libro debe indicar sus referencias o reconocer que no existe evidencia suficiente.
- La adaptación automática debe poder desactivarse y sus cambios deben ser visibles para el usuario.
- Pausar, retroceder y modificar la velocidad siempre debe ser posible.
- El porcentaje de comprensión es una estimación medida, no una garantía.
- El texto del libro no debe utilizarse para entrenamiento global sin permiso explícito.

## Arquitectura y límites

Mantener separadas las siguientes áreas:

```text
Presentación Android
    -> dominio de lectura y entrenamiento
        -> almacenamiento y sincronización
            -> API y servicios AWS
                -> proveedores de documentos y modelos
```

- La interfaz no contiene reglas de adaptación ni llamadas directas a AWS.
- El motor del marcador no depende del proveedor de EPUB, PDF o LLM.
- Las integraciones externas se ocultan detrás de interfaces propias.
- El dominio utiliza identificadores y tipos propios, no objetos de SDK externos.
- Los procesos de ingesta deben ser idempotentes y reanudables.
- Las tareas largas se modelan como trabajos asíncronos con estados observables.

## Android

- Utilizar Kotlin y Jetpack Compose por defecto.
- Favorecer flujo de datos unidireccional y estado inmutable.
- Mantener la lógica de negocio fuera de composables, activities y fragments.
- Ejecutar análisis de documentos, base de datos y red fuera del hilo principal.
- Diseñar primero para pérdida de red, reintentos y sincronización posterior.
- Guardar el avance con ubicaciones estables, no únicamente con porcentaje o número de página.
- Respetar tamaño de fuente, contraste, TalkBack y reducción de movimiento.
- No bloquear la lectura por una falla de analítica, sincronización o IA.

### Marcador de lectura

- Representar el avance mediante una secuencia de bloques con rangos de texto estables.
- En Focus, un gesto vertical completo avanza o retrocede como máximo un fragmento.
- No utilizar tiempo ni palabras por minuto para mover automáticamente el marcador.
- Pausar automáticamente si la aplicación pierde el primer plano.
- Restaurar con precisión el bloque y desplazamiento anteriores.
- Separar la interpretación del gesto y el cálculo del ancla visual de la animación para poder probarlos independientemente.
- Probar puntuación, palabras largas, cambios de párrafo, orientación y tamaño de fuente.

## Procesamiento documental

- Verificar firma/MIME, extensión, tamaño y límites antes de procesar archivos.
- Tratar nombres y contenido de archivos como datos no confiables.
- EPUB y TXT deben procesarse sin OCR.
- Utilizar OCR únicamente para páginas que no contienen texto aprovechable.
- El modo entrenamiento de PDF puede reorganizar texto, pero la vista original debe conservarse.
- Eliminar encabezados y pies repetidos sin destruir contenido legítimo.
- Conservar capítulos, párrafos, páginas y referencias de origen.
- Versionar el algoritmo de normalización para poder reprocesar libros.
- Nunca registrar el contenido completo de un libro en logs.

## IA y personalización

- Utilizar recuperación de fragmentos relevantes; no enviar el libro completo en cada solicitud.
- Tratar el libro y las instrucciones del usuario como datos, no como instrucciones del sistema.
- Proteger las llamadas contra inyección de prompts contenida en documentos.
- Solicitar salidas estructuradas y validarlas antes de persistirlas.
- Guardar modelo, versión de prompt, fragmentos fuente y parámetros de cada artefacto generado.
- Incluir una ruta segura cuando el modelo falle, exceda tiempo o devuelva datos inválidos.
- Evitar ajuste fino por usuario durante el MVP; utilizar un perfil lector estructurado.
- No permitir que una evaluación generada determine por sí sola decisiones irreversibles.
- Comparar respuestas abiertas mediante rúbricas y significado, no coincidencia textual exacta.

## Backend y AWS

- Definir infraestructura como código; evitar recursos configurados manualmente sin documentación.
- Aplicar mínimo privilegio en IAM.
- Cifrar datos en tránsito y reposo.
- Separar originales, derivados y archivos temporales mediante buckets o prefijos explícitos.
- Firmar cargas y descargas con expiraciones cortas.
- Hacer idempotentes los manejadores de eventos y trabajos de procesamiento.
- Establecer límites de tamaño, concurrencia, tokens, tiempo y costo.
- Propagar un identificador de correlación sin incluir datos personales.
- No incluir secretos en el repositorio, aplicación Android, logs o archivos de ejemplo.
- Verificar aislamiento por usuario en la API, base de datos, almacenamiento y vectores.

## Datos y privacidad

- Recopilar únicamente las métricas necesarias para mejorar la lectura.
- Separar telemetría operativa de datos pedagógicos y contenido de libros.
- Definir retención y eliminación para originales, derivados, embeddings y respaldos.
- Una eliminación solicitada por el usuario debe abarcar todas las representaciones del libro.
- Anonimizar o agregar información antes de cualquier análisis global.
- Evitar identificadores personales en eventos analíticos.
- Documentar cualquier dato nuevo, finalidad, duración y mecanismo de eliminación.

## Convenciones de implementación

- Priorizar nombres claros del dominio sobre abreviaturas.
- Mantener funciones pequeñas y efectos secundarios explícitos.
- No agregar abstracciones sin un caso real de uso.
- Los comentarios deben explicar decisiones o restricciones, no repetir el código.
- Mantener mensajes y textos de usuario en recursos localizables.
- Usar español como idioma inicial del producto, sin impedir internacionalización.
- Actualizar documentación y ejemplos junto con cambios de comportamiento.

Cuando exista código, cada módulo deberá documentar sus comandos oficiales de formato, análisis estático y pruebas. No inventar comandos en la documentación antes de incorporarlos al proyecto.

## Estrategia de pruebas

Como mínimo, probar:

- Segmentación por puntuación, umbral de gesto y centrado del marcador.
- Reanudación exacta después de pausar, rotar o cerrar la aplicación.
- Adaptación de velocidad en los límites de comprensión.
- Importación y normalización de ejemplos EPUB, PDF y TXT.
- Aislamiento de usuarios y autorización de cada recurso.
- Idempotencia y reintentos de ingesta.
- Validación de salidas estructuradas del LLM.
- Preguntas sin evidencia o con evidencia contradictoria.
- Lectura, progreso y evaluaciones durante pérdida de conectividad.
- Eliminación integral de un libro.

No utilizar libros protegidos o datos personales reales como fixtures. Crear documentos sintéticos pequeños y versionables.

## Flujo de trabajo

Antes de cambiar código:

1. Leer este archivo y la documentación del área afectada.
2. Revisar el estado del repositorio y conservar cambios ajenos.
3. Identificar riesgos de privacidad, migración y compatibilidad sin conexión.
4. Elegir el cambio más pequeño que complete el requisito.

Después de cambiar código:

1. Ejecutar las verificaciones relevantes del módulo.
2. Revisar fallos, estados vacíos, accesibilidad y pérdida de red.
3. Confirmar que no se agregaron secretos ni contenido de libros.
4. Actualizar README, decisiones o contratos si cambió el comportamiento.
5. Resumir qué cambió, cómo se verificó y qué riesgo permanece.

## Decisiones arquitectónicas

Registrar en `docs/decisions/` cualquier decisión que afecte varias áreas, por ejemplo:

- Motor EPUB o PDF.
- Base de datos local.
- Formato de ubicaciones de lectura.
- Contrato de sincronización.
- Estrategia vectorial.
- Proveedor o selección de modelos.
- Cambio en la política de privacidad o retención.

Cada decisión debe incluir contexto, opciones consideradas, elección, consecuencias y forma de revertirla.

## Definición de terminado

Un cambio está terminado cuando:

- Cumple el comportamiento solicitado y los invariantes del dominio.
- Tiene pruebas proporcionales al riesgo.
- Funciona con errores y estados sin conexión aplicables.
- Mantiene accesibilidad, privacidad y aislamiento.
- No expone secretos ni contenido sensible.
- La documentación relevante está actualizada.
- Las verificaciones del proyecto finalizan correctamente.
