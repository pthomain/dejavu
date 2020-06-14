package dev.pthomain.android.dejavu.serialisation

import java.util.Locale.UK

class SerialisationArgumentValidator(
        decorators: List<SerialisationDecorator>
) {
    private val decoratorNames = decorators.map { it.uniqueName.toUpperCase(UK) }

    fun validate(serialisation: String) {
        serialisation
                .split(",")
                .filter { !it.isBlank() && !decoratorNames.contains(it) }
                .also {
                    if (it.isNotEmpty()) throw SerialisationException(
                            "Invalid serialisation argument(s): ${it.joinToString(separator = ", ")}"
                    )
                }
    }
}