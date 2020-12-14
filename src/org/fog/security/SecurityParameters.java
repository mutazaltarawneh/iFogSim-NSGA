package org.fog.security;

import java.util.List;

import static java.util.Arrays.asList;

public class SecurityParameters {

    // virtualisation
    public static final String AUTHENTICATION = "authentication";
    public static final String IDS_HOST = "ids_host";
    public static final String ACCESS_LOGS = "access_logs";
    public static final String PERMISSION_MODEL = "permission_model";
    public static final String RESTORE_POINTS = "restore_points";
    public static final String RESOURCE_MONITORING = "resource_monitoring";
    public static final String USER_DATA_ISOLATION = "user_data_isolation";
    public static final String PROCESS_ISOLATION = "process_isolation";

    // communications
    public static final String FIREWALL = "firewall";
    public static final String IDS_NETWORK = "ids_network";
   // public static final String ENCRYPTION = "encryption";
    public static final String PUBLICK_KEY = "public_key";
    public static final String CERTIFICATE = "certificates";
    public static final String WIRELESS_SECURITY = "wireless_security";
    public static final String IOT_DATA_ENCRYPTION = "iot_data_encryption";
    public static final String NODE_ISOLATION_MECHANISMS = "node_isolation_mechanisms";
    
    

    //data
    public static final String ENCRYPTED_STORAGE = "encrypted_storage";
    public static final String OBFUSCATED_STORAGE = "obfuscated_storage";
    public static final String BACKUP = "backup";

    // audit
    public static final String AUDIT = "audit";

    // physical
    public static final String ANTI_TAMPERING = "anti_tampering";
    public static final String ACCESS_CONTROL = "access_control";

    // things
    //public static final String ANTI_TAMPERING_THINGS = "anti_tampering";

}
