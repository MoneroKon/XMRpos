import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val DATASTORE_NAME = "settings"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME
)

// Define the key
val COMPANY_NAME: Preferences.Key<String> = stringPreferencesKey("company_name")
val CONTACT_INFORMATION: Preferences.Key<String> = stringPreferencesKey("contact_information")
val RECEIPT_FOOTER: Preferences.Key<String> = stringPreferencesKey("receipt_footer")
val PRIMARY_FIAT_CURRENCY: Preferences.Key<String> = stringPreferencesKey("primary_fiat_currency")
val REFERENCE_FIAT_CURRENCIES: Preferences.Key<String> = stringPreferencesKey("reference_fiat_currencies")
val REQUIRE_PIN_CODE_ON_APP_START: Preferences.Key<Boolean> = booleanPreferencesKey("require_pin_code_on_app_start")
val REQUIRE_PIN_CODE_OPEN_SETTINGS: Preferences.Key<Boolean> = booleanPreferencesKey("require_pin_code_open_settings")
val PIN_CODE_ON_APP_START: Preferences.Key<String> = stringPreferencesKey("pin_code_on_app_start")
val PIN_CODE_OPEN_SETTINGS: Preferences.Key<String> = stringPreferencesKey("pin_code_open_settings")
val MONERO_PAY_CONF_VALUE: Preferences.Key<String> = stringPreferencesKey("monero_pay_conf_value")
val MONERO_PAY_SERVER_ADDRESS: Preferences.Key<String> = stringPreferencesKey("monero_pay_server_address")
val MONERO_PAY_REFRESH_INTERVAL: Preferences.Key<Int> = intPreferencesKey("monero_pay_refresh_interval")
