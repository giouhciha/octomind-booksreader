# 0004. Señales locales para adaptar el ritmo durante una sesión

- Estado: aceptada
- Fecha: 2026-08-22

## Contexto

Los retrocesos repetidos pueden indicar fatiga o dificultad, mientras que adelantar manualmente varios bloques puede indicar que el marcador avanza por debajo del ritmo cómodo. Un toque aislado también puede ser accidental o responder a navegación, por lo que no debe modificar el perfil.

## Opciones consideradas

1. Cambiar el ritmo después de cada retroceso o adelanto.
2. Enviar cada interacción a un modelo remoto para decidir.
3. Evaluar ventanas breves y robustas directamente en el dispositivo.

## Decisión

Se utilizan señales efímeras locales:

- Tres retrocesos válidos en dos minutos pausan Focus y muestran una recomendación no obligatoria.
- El lector puede tomar una pausa, reducir el ritmo un 10 % o continuar igual.
- Ocho adelantos manuales válidos se comparan con el tiempo lingüístico esperado de sus bloques.
- Una mediana igual o inferior al 80 % del tiempo esperado permite aumentar como máximo un 5 %.
- Un retroceso reciente o un periodo de diez bloques después de un ajuste impide nuevos aumentos.
- Todo ajuste automático es visible, reversible y puede desactivarse.
- Gestos separados por menos de 400 ms y la calibración no alimentan estas señales.

## Consecuencias

- La adaptación responde a patrones y no a toques aislados.
- El lector conserva control sobre recomendaciones y cambios automáticos.
- No se persiste el historial crudo de navegación; únicamente se guarda el ritmo resultante y la preferencia de adaptación.
- Adelantar no demuestra comprensión. Los aumentos deberán condicionarse también a evaluaciones cuando esa fase esté disponible.

## Reversión

El asesor está aislado del motor del marcador. Los umbrales pueden cambiarse o la función puede desactivarse sin modificar las ubicaciones estables ni el plan de lectura.
