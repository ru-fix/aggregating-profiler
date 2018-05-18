import kotlin.reflect.KProperty

class EnvConfig(val supplier: (String)->String?) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? = supplier.invoke(property.name)
}
