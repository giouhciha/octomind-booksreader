# 0010. Abreviaturas contextuales y resumen adaptable

## Contexto

En correspondencia, citas y firmas es frecuente encontrar iniciales y abreviaturas terminadas en punto. Interpretarlas siempre como cierre produce fragmentos artificiales como `H.` o `P.D.`. Por otra parte, el resumen de una sesión larga puede exceder la altura disponible y comprimir la acción inferior hasta mostrar únicamente una barra de color.

## Opciones consideradas

1. Ignorar todos los puntos contenidos en palabras cortas.
2. Mantener una lista exhaustiva por idioma.
3. Combinar patrones de iniciales, una lista pequeña de abreviaturas frecuentes y el contexto siguiente.

Para el resumen se consideró reducir métricas, reducir tipografía o permitir desplazamiento reservando la acción.

## Decisión

El segmentador no cierra un fragmento después de un inicialismo como `R.F.D.` mientras exista texto posterior. Una inicial aislada como `N.` o `W.` tampoco cierra cuando la siguiente palabra comienza en mayúscula. Tratamientos y abreviaturas frecuentes reciben el mismo comportamiento. Las comas posteriores, el final del párrafo y los signos fuertes verdaderos conservan sus pausas.

El resumen se representa como contenido desplazable dentro de un `Scaffold`. El botón para volver a la biblioteca vive en una barra inferior con altura y navegación del sistema reservadas, independiente del tamaño del contenido.

La presentación reutiliza la identidad de la biblioteca mediante madera cálida, papel y una portada calculada localmente a partir del título. Las métricas se agrupan en tarjetas de dos columnas y el progreso conserva una barra explícita. Estos cambios son únicamente visuales y no modifican el cálculo ni la persistencia de la sesión.

## Consecuencias

- Nombres, firmas, direcciones y posdatas se leen como unidades más naturales.
- Una abreviatura desconocida todavía puede producir una pausa; se ampliará mediante ejemplos sintéticos observados.
- El botón del resumen permanece visible en pantallas pequeñas y con tamaños de fuente accesibles.
- Las métricas siguen disponibles mediante desplazamiento y no se eliminan.

## Reversión

Retirar la detección contextual devuelve el tratamiento uniforme de puntos. El resumen puede volver a una columna fija sin migrar datos, aunque reaparecería el riesgo de compresión vertical.
