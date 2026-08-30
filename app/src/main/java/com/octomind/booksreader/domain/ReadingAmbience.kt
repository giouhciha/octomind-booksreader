package com.octomind.booksreader.domain

import java.text.Normalizer

enum class ReadingAmbience {
    MYSTERY,
    FANTASY,
    SCIENCE_FICTION,
    ROMANCE,
    NATURE,
    KNOWLEDGE,
    NEUTRAL,
}

object ReadingAmbienceSelector {
    private val keywords =
        linkedMapOf(
            ReadingAmbience.MYSTERY to
                setOf(
                    "misterio",
                    "secreto",
                    "noche",
                    "sombra",
                    "crimen",
                    "detective",
                    "terror",
                    "oscuro",
                    "fantasma",
                    "muerte",
                    "sangre",
                    "solitaria",
                    "extraño",
                ),
            ReadingAmbience.FANTASY to
                setOf(
                    "magia",
                    "reino",
                    "dragon",
                    "hechizo",
                    "elfo",
                    "hada",
                    "castillo",
                    "espada",
                    "bruja",
                    "oraculo",
                    "leyenda",
                ),
            ReadingAmbience.SCIENCE_FICTION to
                setOf(
                    "espacio",
                    "planeta",
                    "robot",
                    "nave",
                    "galaxia",
                    "futuro",
                    "alien",
                    "cosmos",
                    "tecnologia",
                    "marte",
                    "orbita",
                ),
            ReadingAmbience.ROMANCE to
                setOf(
                    "amor",
                    "corazon",
                    "beso",
                    "romance",
                    "amante",
                    "boda",
                    "pasion",
                    "querida",
                    "enamor",
                ),
            ReadingAmbience.NATURE to
                setOf(
                    "bosque",
                    "mar",
                    "montaña",
                    "rio",
                    "jardin",
                    "naturaleza",
                    "animal",
                    "selva",
                    "campo",
                    "granja",
                    "colina",
                    "arbol",
                ),
            ReadingAmbience.KNOWLEDGE to
                setOf(
                    "historia",
                    "ciencia",
                    "ensayo",
                    "manual",
                    "metodo",
                    "economia",
                    "filosofia",
                    "investigacion",
                    "capitulo",
                    "aprende",
                    "ejercicio",
                ),
        )

    fun select(
        title: String,
        chapterTitle: String?,
        chapterSample: String,
    ): ReadingAmbience {
        val normalizedTitle = normalize("$title ${chapterTitle.orEmpty()}")
        val normalizedSample = normalize(chapterSample.take(4_000))
        val scores =
            keywords.mapValues { (_, terms) ->
                terms.sumOf { term ->
                    (if (containsWord(normalizedTitle, term)) 3 else 0) +
                        (if (containsWord(normalizedSample, term)) 1 else 0)
                }
            }
        val best = scores.maxByOrNull { it.value }
        return best?.takeIf { it.value > 0 }?.key ?: ReadingAmbience.NEUTRAL
    }

    private fun containsWord(
        text: String,
        term: String,
    ): Boolean = Regex("(^|[^a-z0-9])${Regex.escape(normalize(term))}([^a-z0-9]|$)").containsMatchIn(text)

    private fun normalize(value: String): String =
        Normalizer
            .normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
}
