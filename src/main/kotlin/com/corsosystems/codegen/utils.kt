package com.corsosystems.codegen
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.inputStream
import kotlin.io.path.readBytes

/*
    Adapted from https://github.com/paul-griffith/modification-updater
 */

@OptIn(ExperimentalSerializationApi::class)
val JSON = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    explicitNulls = false
    encodeDefaults = true
}

class ResourceSigner {
    fun signResource(resourcePath: Path, actor: String, timestamp: Instant) : String {
        val resource = getResource(resourcePath)

        val updated = resource.update(actor, timestamp)
        val signedManifest = JSON.encodeToString(ResourceManifest.serializer(), updated.manifest)
        println(signedManifest)

        return signedManifest;
    }

    private fun getResource(location: Path): ProjectResource {
        val manifest = location.resolve("resource.json").inputStream().toManifest()
        val data = manifest.files.associateWith { fileName ->
            DataLoader { location.resolve(fileName).readBytes() }
        }
        return ProjectResource(manifest, data)
    }
}

object ApplicationScopeDeserializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("scope", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Int {
        return when (decoder.decodeChar()) {
            'N' -> 0
            'G' -> 1
            'D' -> 2
            'C' -> 4
            'A' -> 7
            else -> throw IllegalArgumentException()
        }
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeChar(
            when (value) {
                0 -> 'N'
                1 -> 'G'
                2 -> 'D'
                4 -> 'C'
                7 -> 'A'
                else -> throw IllegalArgumentException()
            }
        )
    }
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value))
    }
}

internal fun Int.toByteArray(): ByteArray = byteArrayOf(
    (this shr 24).toByte(),
    (this shr 16).toByte(),
    (this shr 8).toByte(),
    this.toByte()
)

internal fun Boolean.toByte(): Byte = (if (this) 1 else 0).toByte()

private val hexFormat = HexFormat.of()

internal fun ByteArray.toHexString(): String = hexFormat.formatHex(this)

@OptIn(ExperimentalSerializationApi::class)
fun InputStream.toManifest(): ResourceManifest {
    return use { resourceStream ->
        JSON.decodeFromStream(ResourceManifest.serializer(), resourceStream)
    }
}
