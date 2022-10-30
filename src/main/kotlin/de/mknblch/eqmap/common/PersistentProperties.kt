package de.mknblch.eqmap.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import javafx.beans.property.Property
import javafx.scene.paint.Color
import java.io.File
import java.io.IOException
import java.nio.file.Paths


class PersistentProperties(val file: File) {

    val data: MutableMap<String, Any> = if (file.exists()) {
        mapper.readValue(file, typeRef)
    } else {
        HashMap()
    }

    inline fun <reified V : Any> bind(
        key: String,
        default: V,
        property: Property<V>,
        crossinline serializer: (V) -> Any = { it },
        crossinline deserializer: (Any) -> V = { it as V }
    ) {
        property.value = deserializer(getOrSet(key, default))
        property.addListener { _, _, v ->
            set(key, serializer(v))
        }
    }

    inline fun <reified V : Any> get(key: String): V? {
        return if (data[key] is V) data[key] as V else null
    }

    inline fun <reified V : Any> getOrSet(key: String, default: V): V {
        return if (data.containsKey(key)) data[key] as V else kotlin.run {
            data[key] = default
            default
        }
    }

    inline fun <reified V : Any> getMap(key: String): MutableMap<String, V> {
        return if (data.containsKey(key) && data[key] is Map<*, *>) data[key] as MutableMap<String, V> else kotlin.run {
            HashMap<String, V>().also {
                data[key] = it
            }
        }
    }

    inline fun <reified V : Any> getOrEval(key: String, lambda: () -> V?): V? {
        return if (data.containsKey(key)) data[key] as V else kotlin.run {
            lambda()?.also {
                data[key] = it
            }
        }
    }

    fun <V : Any> set(key: String, value: V?): V? {
        if (value == null) {
            return null
        }
        data[key] = value
        return value
    }

    fun write() {
        file.outputStream().bufferedWriter(Charsets.UTF_8).use {
            it.write(mapper.writeValueAsString(data))
        }
    }

    companion object {

        object ColorDeserializer : JsonDeserializer<Color>() {
            @Throws(IOException::class)
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Color {
                return Color.web(p.valueAsString)
            }
        }

        object ColorSerializer : StdSerializer<Color>(Color::class.java) {
            @Throws(IOException::class)
            override fun serialize(p0: Color, p1: JsonGenerator, p2: SerializerProvider) {
                p1.writeString("""#${p0.toString().removePrefix("0x")}""")
            }

            override fun serializeWithType(
                value: Color,
                gen: JsonGenerator,
                serializers: SerializerProvider,
                typeSer: TypeSerializer
            ) {
                val typeId = typeSer.typeId(value, JsonToken.VALUE_STRING)
                typeSer.writeTypePrefix(gen, typeId)
                serialize(value, gen, serializers)
                typeSer.writeTypeSuffix(gen, typeId)
            }
        }

        private val colorModule = SimpleModule().also {
            it.addSerializer(ColorSerializer)
            it.addDeserializer(Color::class.java, ColorDeserializer)
        }

        private val typeRef = object : TypeReference<MutableMap<String, Any>>() {}

        private val ptv: PolymorphicTypeValidator =
            BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build()
        private val mapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(colorModule)
            .also {
                it.writerWithDefaultPrettyPrinter()
                it.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING)
            }

        fun load(path: String): PersistentProperties {
            return PersistentProperties(Paths.get(path).toFile())
        }
    }
}