@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package brightspark.configdelegates

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Extending this class on a config class enables the use of [config] and [configMutable] methods to create configs.
 *
 * The [commonSpec], [clientSpec] and [serverSpec] are exposed for convenience, however an implementation should use
 * [register] to register configs with Forge.
 */
abstract class DelegatedConfig(val defaultType: ConfigType = ConfigType.COMMON) {
	private val configs: MutableList<ConfigDelegate<Any>> = mutableListOf()

	private val commonBuilder = ForgeConfigSpec.Builder()
	private val clientBuilder = ForgeConfigSpec.Builder()
	private val serverBuilder = ForgeConfigSpec.Builder()

	private var initialized = false

	lateinit var commonSpec: ForgeConfigSpec
	lateinit var clientSpec: ForgeConfigSpec
	lateinit var serverSpec: ForgeConfigSpec

	private fun getBuilder(type: ConfigType): ForgeConfigSpec.Builder = when (type) {
		ConfigType.COMMON -> commonBuilder
		ConfigType.CLIENT -> clientBuilder
		ConfigType.SERVER -> serverBuilder
	}

	private fun initialize() {
		if (initialized)
			return
		initialized = true

		ConfigDelegates.LOG.info("Initializing delegated config {}", this::class.qualifiedName)

		// Initialize each config
		configs.forEach { it.setConfigValue(getBuilder(it.type)) }

		// Build the config specs
		commonSpec = commonBuilder.build()
		clientSpec = clientBuilder.build()
		serverSpec = serverBuilder.build()
	}

	/**
	 * Registers this [DelegatedConfig] instance with Forge using [ModLoadingContext.registerConfig] for each type that
	 * configs exist for.
	 */
	fun register(modLoadingContext: ModLoadingContext) {
		initialize()

		var size = commonSpec.size()
		if (size > 0) {
			ConfigDelegates.LOG.info("Registering {} common configs", size)
			modLoadingContext.registerConfig(ModConfig.Type.COMMON, commonSpec)
		}

		size = clientSpec.size()
		if (size > 0) {
			ConfigDelegates.LOG.info("Registering {} client configs", size)
			modLoadingContext.registerConfig(ModConfig.Type.CLIENT, clientSpec)
		}

		size = serverSpec.size()
		if (size > 0) {
			ConfigDelegates.LOG.info("Registering {} server configs", size)
			modLoadingContext.registerConfig(ModConfig.Type.SERVER, serverSpec)
		}
	}

	internal fun regConfigDelegate(configDelegate: ConfigDelegate<out Any>) {
		@Suppress("UNCHECKED_CAST")
		configs.add(configDelegate as ConfigDelegate<Any>)
	}

	/**
	 * A read-only config delegate.
	 * This is your typical config delegate to use, and only allows for the config to be read from.
	 */
	protected fun <T : Any> config(
		type: ConfigType = defaultType,
		builder: ForgeConfigSpec.Builder.() -> ConfigValue<T>
	): ConfigDelegate<T> = ConfigDelegate(this, type, builder)

	/**
	 * A mutable config delegate.
	 * Extends functionality of the read-only delegate to allow modification of the config at runtime.
	 */
	protected fun <T : Any> configMutable(
		type: ConfigType = defaultType,
		builder: ForgeConfigSpec.Builder.() -> ConfigValue<T>
	): ConfigDelegateMutable<T> = ConfigDelegateMutable(this, type, builder)
}

/**
 * The config type, which typically follows format with [ModConfig.Type].
 */
enum class ConfigType { COMMON, CLIENT, SERVER }

/**
 * The typical read-only config delegate class.
 * Use [DelegatedConfig.config] to easily use this delegate.
 */
open class ConfigDelegate<T : Any>(
	configInstance: DelegatedConfig,
	val type: ConfigType,
	val builder: ForgeConfigSpec.Builder.() -> ConfigValue<T>
) : ReadOnlyProperty<Any?, T> {
	protected var initialized = false
	protected lateinit var configValue: ConfigValue<T>

	init {
		@Suppress("LeakingThis")
		configInstance.regConfigDelegate(this)
	}

	fun setConfigValue(configSpecBuilder: ForgeConfigSpec.Builder) {
		configValue = builder(configSpecBuilder)
		initialized = true
	}

	override fun getValue(thisRef: Any?, property: KProperty<*>): T = if (initialized)
		configValue.get()
	else
		throw UninitializedPropertyAccessException("ConfigDelegate ${property.name} has not been initialized")
}

/**
 * A config delegate extending the functionality of [ConfigDelegate] to allow setting of the config value at runtime.
 * Use [DelegatedConfig.configMutable] to easily use this delegate.
 */
class ConfigDelegateMutable<T : Any>(
	configInstance: DelegatedConfig,
	type: ConfigType,
	builder: ForgeConfigSpec.Builder.() -> ConfigValue<T>
) : ConfigDelegate<T>(configInstance, type, builder), ReadWriteProperty<Any?, T> {
	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = if (initialized)
		configValue.set(value)
	else
		throw UninitializedPropertyAccessException("ConfigDelegate ${property.name} has not been initialized")
}
