@file:Suppress("unused")

package brightspark.configdelegates

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class DelegatedConfig {
	private val configs: MutableList<ConfigDelegate<Any>> = mutableListOf()

	private val commonBuilder = ForgeConfigSpec.Builder()
	private val clientBuilder = ForgeConfigSpec.Builder()
	private val serverBuilder = ForgeConfigSpec.Builder()

	private var initialized = false

	lateinit var commonSpec: ForgeConfigSpec
	lateinit var clientSpec: ForgeConfigSpec
	lateinit var serverSpec: ForgeConfigSpec

	private fun getBuilder(side: ConfigSide): ForgeConfigSpec.Builder = when (side) {
		ConfigSide.COMMON -> commonBuilder
		ConfigSide.CLIENT -> clientBuilder
		ConfigSide.SERVER -> serverBuilder
	}

	private fun initialize() {
		if (initialized)
			return
		initialized = true

		ConfigDelegates.LOG.info("Initializing delegated config {}", this::class.qualifiedName)

		configs.forEach { it.setConfigValue(getBuilder(it.side)) }
		commonSpec = commonBuilder.build()
		clientSpec = clientBuilder.build()
		serverSpec = serverBuilder.build()
	}

	fun register(modLoadingContext: ModLoadingContext) {
		initialize()

		var size = commonSpec.size()
		if (size > 0) {
			ConfigDelegates.LOG.info("Registering {} common configs", size)
			modLoadingContext.registerConfig(ModConfig.Type.COMMON, commonSpec)
		}
		size = clientSpec.size()
		if (size > 0) {
			ConfigDelegates.LOG.info("Registering {} client config", size)
			modLoadingContext.registerConfig(ModConfig.Type.CLIENT, clientSpec)
		}
		size = serverSpec.size()
		if (size > 0) {
			ConfigDelegates.LOG.info("Registering {} server config", size)
			modLoadingContext.registerConfig(ModConfig.Type.SERVER, serverSpec)
		}
	}

	private fun regConfigDelegate(configDelegate: ConfigDelegate<out Any>) {
		@Suppress("UNCHECKED_CAST")
		configs.add(configDelegate as ConfigDelegate<Any>)
	}

	fun <T : Any> config(
		side: ConfigSide,
		builder: ForgeConfigSpec.Builder.() -> ConfigValue<T>
	): ConfigDelegate<T> = ConfigDelegate(side, builder).apply { regConfigDelegate(this) }

	fun <T : Any> configMutable(
		side: ConfigSide,
		builder: ForgeConfigSpec.Builder.() -> ConfigValue<T>
	): ConfigDelegateMutable<T> = ConfigDelegateMutable(side, builder).apply { regConfigDelegate(this) }
}

enum class ConfigSide { COMMON, CLIENT, SERVER }

open class ConfigDelegate<T : Any>(
	val side: ConfigSide,
	val builder: ForgeConfigSpec.Builder.() -> ConfigValue<T>
) : ReadOnlyProperty<Any?, T> {
	protected var initialized = false
	protected lateinit var configValue: ConfigValue<T>

	fun setConfigValue(configSpecBuilder: ForgeConfigSpec.Builder) {
		configValue = builder(configSpecBuilder)
		initialized = true
	}

	override fun getValue(thisRef: Any?, property: KProperty<*>): T = if (initialized)
		configValue.get()
	else
		throw UninitializedPropertyAccessException("ConfigDelegate ${property.name} has not been initialized")
}

class ConfigDelegateMutable<T : Any>(
	side: ConfigSide,
	builder: ForgeConfigSpec.Builder.() -> ConfigValue<T>
) : ConfigDelegate<T>(side, builder), ReadWriteProperty<Any?, T> {
	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = if (initialized)
		configValue.set(value)
	else
		throw UninitializedPropertyAccessException("ConfigDelegate ${property.name} has not been initialized")
}
