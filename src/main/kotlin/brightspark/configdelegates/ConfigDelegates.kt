package brightspark.configdelegates

import net.minecraftforge.fml.common.Mod
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Mod(ConfigDelegates.MOD_ID)
class ConfigDelegates {
	companion object {
		const val MOD_ID = "configdelegates"

		internal val LOG: Logger = LoggerFactory.getLogger(ConfigDelegates::class.java)
	}
}
