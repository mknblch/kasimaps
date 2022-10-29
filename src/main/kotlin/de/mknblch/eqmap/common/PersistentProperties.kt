package de.mknblch.eqmap.common

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import javafx.beans.property.Property
import javafx.beans.value.WritableValue
import java.io.File
import java.nio.file.Paths

class PersistentProperties(val file: File) {

    val data: MutableMap<String, Any> = if (file.exists()) {
        mapper.readValue(file, typeRef)
    } else {
        HashMap()
    }

    inline fun <reified V : Any> bind(key: String, default: V, property: Property<V>, crossinline func: (V) -> Any = { it } ) {
        property.value = getOrSet(key, default)
        property.addListener { _, _, v ->
            set(key, func(v))
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

        private val typeRef = object : TypeReference<MutableMap<String, Any>>() {}

//        private val ptv: PolymorphicTypeValidator =
//            BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build()
        private val mapper = ObjectMapper()
            .registerKotlinModule().also {
                it.writerWithDefaultPrettyPrinter()
//                it.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT)
            }

        fun load(path: String): PersistentProperties {
            return PersistentProperties(Paths.get(path).toFile())
        }
    }
}