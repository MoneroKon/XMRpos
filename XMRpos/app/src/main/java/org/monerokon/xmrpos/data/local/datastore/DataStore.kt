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
val BACKEND_CONF_VALUE: Preferences.Key<String> = stringPreferencesKey("backend_conf_value")
val BACKEND_INSTANCE_URL: Preferences.Key<String> = stringPreferencesKey("backend_instance_url")
val BACKEND_ACCESS_TOKEN: Preferences.Key<String> = stringPreferencesKey("backend_access_token")
val BACKEND_REFRESH_TOKEN: Preferences.Key<String> = stringPreferencesKey("backend_refresh_token")
val BACKEND_REFRESH_INTERVAL: Preferences.Key<Int> = intPreferencesKey("backend_refresh_interval")
val PRINTER_CONNECTION_TYPE: Preferences.Key<String> = stringPreferencesKey("printer_connection_type")
val PRINTER_DPI: Preferences.Key<Int> = intPreferencesKey("printer_dpi")
val PRINTER_WIDTH: Preferences.Key<Int> = intPreferencesKey("printer_width")
val PRINTER_NBR_CHARACTERS_PER_LINE: Preferences.Key<Int> = intPreferencesKey("printer_nbr_characters_per_line")
val PRINTER_CHARSET_ENCODING: Preferences.Key<String> = stringPreferencesKey("printer_charset_encoding")
val PRINTER_CHARSET_ID: Preferences.Key<Int> = intPreferencesKey("printer_charset_id")
val PRINTER_ADDRESS: Preferences.Key<String> = stringPreferencesKey("printer_address")
val PRINTER_PORT: Preferences.Key<Int> = intPreferencesKey("printer_port")
