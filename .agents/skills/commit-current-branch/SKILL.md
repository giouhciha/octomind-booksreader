---
name: commit-current-branch
description: Revisa los cambios locales, crea un commit seguro, hace push únicamente a la rama Git actual y ejecuta el job local de Jenkins del proyecto. Usar cuando el usuario pida explícitamente commit y push de los cambios; no usar para releases, PRs, cambios de rama ni reescritura de historial.
---

# Commit y push de la rama actual

Completar el commit y el push solicitados sin ampliar el alcance de los cambios ni alterar el historial existente. Después de confirmar el push, ejecutar una validación en Jenkins y comunicar su resultado por separado.

## Autorización

La invocación de esta habilidad no autoriza mutaciones por sí sola. Ejecutar `git commit` y `git push` únicamente cuando el usuario los haya pedido explícitamente en la solicitud actual. Una petición de revisar estado, preparar cambios o proponer un mensaje no autoriza commit ni push.

## Revisión previa

- Leer el `AGENTS.md` aplicable y respetar sus verificaciones.
- Consultar la rama actual, estado, diff, remotos y relación con upstream.
- Detenerse si HEAD está separado, no existe una rama actual o no hay un remoto adecuado.
- Conservar cambios ajenos. Si no es posible distinguirlos de los cambios solicitados, pedir dirección antes de preparar el commit.
- Revisar archivos nuevos y modificados para evitar secretos, credenciales, contenido privado, binarios temporales, capturas de validación y artefactos de compilación.
- Ejecutar las verificaciones proporcionales al cambio antes de confirmar. Si fallan, no crear el commit salvo que el usuario ordene expresamente registrar el estado fallido.

## Preparación y commit

- Añadir al índice rutas explícitas relacionadas con la tarea. No usar `git add .`, `git add -A` ni patrones amplios cuando puedan incorporar cambios ajenos.
- Revisar el diff staged antes de confirmar y comprobar que el resto del árbol de trabajo permanezca intacto.
- Usar el mensaje indicado por el usuario. Si no proporcionó uno, redactar un mensaje breve en modo imperativo que describa el resultado principal.
- Crear un commit normal. No usar `--amend`, firmas improvisadas, bypass de hooks ni reescritura de historial sin una solicitud explícita adicional.
- Si un hook modifica archivos o rechaza el commit, revisar el estado y detenerse cuando la corrección no sea mecánica y claramente perteneciente a la tarea.

## Push

- Volver a resolver el nombre de la rama inmediatamente antes del push.
- Si ya tiene upstream, usar `git push` sin argumentos que cambien el destino.
- Si no tiene upstream y existe `origin`, usar `git push -u origin <rama-actual>`.
- No usar `--force`, `--force-with-lease`, borrado de referencias, tags ni otra rama.
- Si el remoto rechaza el push, informar la causa. No ejecutar automáticamente pull, merge, rebase ni reset.

## Jenkins

- Ejecutar Jenkins únicamente después de verificar que el push terminó y la rama quedó sincronizada con su upstream.
- El job configurado para este repositorio es `http://localhost:8080/job/octomind-booksreader/`.
- No guardar usuario, contraseña, token ni crumb en la skill, el repositorio, comandos visibles o logs. Leer `JENKINS_USER` y `JENKINS_API_TOKEN` del entorno.
- Si falta alguna variable, conservar el commit y push ya completados, no intentar métodos alternativos de autenticación y explicar cómo configurar las variables.
- Ejecutar una sola vez `scripts/invoke_jenkins_job.ps1`; no reintentar automáticamente un build fallido.
- Esperar hasta que Jenkins termine o hasta el límite del script. Un resultado distinto de `SUCCESS` no revierte el commit ni el push.
- Si la ejecución falla, informar la URL del build cuando esté disponible y resumir la etapa o causa visible sin exponer credenciales.

## Resultado

Confirmar al usuario:

- rama y remoto usados;
- hash corto y mensaje del commit;
- verificaciones ejecutadas y su resultado;
- resultado del push;
- número, URL y resultado de Jenkins, o la razón concreta por la que no pudo ejecutarse;
- cambios locales restantes, si existen.

No afirmar que el push terminó hasta verificar la salida del comando y que la rama local ya no esté adelantada respecto de su upstream.
