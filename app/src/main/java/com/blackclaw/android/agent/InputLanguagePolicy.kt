package com.blackclaw.android.agent

/**
 * The assistant interface is English-only. Keep language gating in one place
 * so classifiers do not silently route Spanish requests through old aliases.
 */
object InputLanguagePolicy {
    private val NON_ENGLISH_MARKERS = listOf(
        " abre ", " abrir ", " busca ", " buscar ", " manda ", " enviar ", " envia ",
        " tarea ", " tareas ", " rutina ", " alarma ", " recordatorio ", " calendario ",
        " programa ", " programar ", " crea ", " crear ", " agrega ", " agregar ",
        " anota ", " apuntar ", " mañana ", " manana ", " cuando ", " donde ",
        " qué ", " que ", " cómo ", " como ", " activa ", " apagar ", " apaga ",
        " guardar ", " guarda ", " mensaje ", " recuerda ", " cancela ", " cancelar ", " contacto ",
        " recibo ", " factura ", " música ", " musica ", " clima ", " ubicación ",
        " ubicacion ", " cámara ", " camara ", " finanzas ", " gasto ", " pago ",
        " permiso ", " accesibilidad ", " por favor ", " necesito ", " puedes ",
        " apaga ", " apagar ", " enciende ", " encender ", " activa ", " activar ",
        " desactiva ", " descargar ", " instala ", " eliminar ", " comparte ",
        " copia ", " pega ", " traduce ", " calcula ", " captura ", " graba ",
        " escanea ", " ejecuta ", " revisa ", " muestra ", " dime ", " lee ",
        " pon ", " poner ", " reproduce ", " llama ", " avisa ", " anota ",
        " toca ", " cierra ", " borra ", " elimina ", " traduce ", " comparte ",
    )

    fun isEnglish(text: String): Boolean {
        val normalized = " ${text.lowercase().replace(Regex("\\s+"), " ").trim()} "
        return NON_ENGLISH_MARKERS.none { normalized.contains(it) }
    }
}
