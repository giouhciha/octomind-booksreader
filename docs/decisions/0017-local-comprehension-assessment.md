# 0017. Evaluación local de comprensión por sesión

- Estado: aceptada
- Fecha: 2026-09-14

## Contexto

Octomind necesita empezar a medir comprensión antes de incorporar cuentas, sincronización o un modelo en la nube. La medición debe estar vinculada al texto leído, funcionar sin conexión y evitar presentar el objetivo de 90 % como una garantía. También debe permitir sustituir la generación local por Amazon Bedrock sin acoplar la interfaz al proveedor.

## Opciones consideradas

1. Esperar a la integración completa con Bedrock.
2. Calificar respuestas abiertas mediante coincidencia exacta de palabras.
3. Implementar recuperación activa local con evidencia y autoevaluación guiada.

## Decisión

Se implementa la tercera opción. Al terminar una sesión con al menos 80 caracteres legibles, un proveedor local crea dos preguntas literales, una de idea principal y una de inferencia. Todas conservan lector, libro, sesión, respuesta esperada, rúbrica y rangos de caracteres de la evidencia. El lector responde antes de ver el texto, declara su confianza y compara su respuesta con la evidencia antes de asignar una valoración.

El puntaje estimado pondera comprensión literal con 40 %, idea principal con 35 % e inferencia con 25 %. El promedio acumulado solo usa evaluaciones completas y se presenta como inicial hasta reunir tres sesiones. No se modifica automáticamente la velocidad ni el marcador a partir de este resultado.

Las evaluaciones se guardan atómicamente en `library/comprehension.json`, aisladas por `bookId` y por el identificador del lector local. Al estar dentro de la biblioteca privada, quedan incluidas en el respaldo cifrado existente. La restauración valida su estructura JSON antes de reemplazar datos. El contenido y las respuestas no se registran ni se transmiten.

## Consecuencias

- La comprensión puede observarse desde la primera versión sin conexión ni costo de inferencia.
- El usuario controla la valoración; esta fase no pretende calificar semánticamente texto libre.
- Las preguntas son generales y menos específicas que las que podrá producir un modelo fundamentado.
- El historial sobrevive cierres y respaldos, y se elimina junto con el libro.
- El proveedor de preguntas puede cambiar sin modificar la navegación, el almacenamiento ni el cálculo.

## Reversión

Una implementación de Bedrock deberá cumplir `ComprehensionQuestionProvider` y conservar el mismo contrato de propiedad, evidencia, respuesta esperada y rúbrica. El proveedor local seguirá disponible como alternativa sin conexión. El archivo de historial no necesita migrarse mientras se mantengan los tipos de pregunta y sus rangos estables.
