package de.falkorichter.android.bluetooth.utils;

import java.util.UUID;

public class BTUUID {

    public static class Characteristic{
        public static final UUID model_number_string        = BTUUID.fromString("2A24");
        public static final UUID serial_number_string       = BTUUID.fromString("2A25");
        public static final UUID firmware_revision_string   = BTUUID.fromString("2A26");
        public static final UUID hardware_revision_string   = BTUUID.fromString("2A27");
        public static final UUID software_revision_string   = BTUUID.fromString("2A28");
        public static final UUID manufacturer_name_string   = BTUUID.fromString("2A29");
        public static final UUID ieee1107320601_regulatory_certification_data_list = BTUUID.fromString("2A2A");
    }

    public static class Service {
        public static final UUID device_information = UUID.fromString("180A");
    }


    public static UUID fromString(String uuidString){
        if (uuidString.length() == 4){
            return UUID.fromString("0000"+ uuidString + "-0000-1000-8000-00805f9b34fb");
        }
        else {
            return UUID.fromString(uuidString);
        }
    }
}
