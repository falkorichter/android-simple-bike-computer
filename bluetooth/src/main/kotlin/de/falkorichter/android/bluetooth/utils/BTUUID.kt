package de.falkorichter.android.bluetooth.utils

import java.util.UUID

public class BTUUID {

    public class Characteristic {
        class object {
            public val model_number_string: UUID = BTUUID.fromString("2A24")
            public val serial_number_string: UUID = BTUUID.fromString("2A25")
            public val firmware_revision_string: UUID = BTUUID.fromString("2A26")
            public val hardware_revision_string: UUID = BTUUID.fromString("2A27")
            public val software_revision_string: UUID = BTUUID.fromString("2A28")
            public val manufacturer_name_string: UUID = BTUUID.fromString("2A29")
            public val ieee1107320601_regulatory_certification_data_list: UUID = BTUUID.fromString("2A2A")
        }
    }

    public class Service {
        class object {
            public val device_information: UUID = BTUUID.fromString("180A")
        }
    }

    class object {

        public fun fromString(uuidString: String): UUID {
            if (uuidString.length() == 4) {
                return UUID.fromString("0000" + uuidString + "-0000-1000-8000-00805f9b34fb")
            } else {
                return UUID.fromString(uuidString)
            }
        }
    }
}
