package org.fog.security;

import java.util.ArrayList;
import java.util.List;

import static org.fog.security.SecurityParameters.*;
import static org.fog.security.SecurityParametersTypes.*;

import static java.util.Arrays.asList;

public class SecurityTaxonomy {
  //  private static final String OBFUSCATED_STORAGE = null;

	public static String getType(String securityMeasure){
        String result = null;
        switch (securityMeasure) {
            case AUTHENTICATION: case IDS_HOST: case ACCESS_LOGS: case PERMISSION_MODEL: case RESTORE_POINTS: case RESOURCE_MONITORING:case USER_DATA_ISOLATION:
                result = VIRTUALISATION;
                break;
            case FIREWALL: case IDS_NETWORK:  case PUBLICK_KEY: case CERTIFICATE: case WIRELESS_SECURITY: case IOT_DATA_ENCRYPTION: case NODE_ISOLATION_MECHANISMS:
                result = COMMUNICATIONS;
                break;
            case ENCRYPTED_STORAGE: case BACKUP: case OBFUSCATED_STORAGE:
                result = DATA;
                break;
            case AUDIT:
                result = SECURITY_AUDIT;
                break;
            case ANTI_TAMPERING: case ACCESS_CONTROL:
                result = PHYSICAL;
                break;
            default:
                break;
        }
        return result;
    }

    public static ArrayList<String> getSecurityMeasuresByType(String type){
        List<String> result = null;
        switch (type) {
            case VIRTUALISATION:
                result = asList(AUTHENTICATION, IDS_HOST, ACCESS_LOGS,PERMISSION_MODEL, RESTORE_POINTS, PROCESS_ISOLATION,RESOURCE_MONITORING,USER_DATA_ISOLATION);
                break;
            case COMMUNICATIONS:
                result = asList(FIREWALL, IDS_NETWORK, PUBLICK_KEY, CERTIFICATE, WIRELESS_SECURITY,IOT_DATA_ENCRYPTION,NODE_ISOLATION_MECHANISMS);
                break;
            case DATA:
                result = asList(ENCRYPTED_STORAGE, BACKUP,OBFUSCATED_STORAGE);
                break;
            case SECURITY_AUDIT:
                result = asList(AUDIT);
                break;
            case PHYSICAL:
                result = asList(ANTI_TAMPERING,ACCESS_CONTROL);
                break;
            default:
                result = asList(AUTHENTICATION, IDS_HOST, ACCESS_LOGS,PERMISSION_MODEL, RESTORE_POINTS, PROCESS_ISOLATION,RESOURCE_MONITORING,USER_DATA_ISOLATION,
                		FIREWALL, IDS_NETWORK, PUBLICK_KEY, CERTIFICATE, WIRELESS_SECURITY,IOT_DATA_ENCRYPTION,NODE_ISOLATION_MECHANISMS,
                		ENCRYPTED_STORAGE, BACKUP,OBFUSCATED_STORAGE, ANTI_TAMPERING,ACCESS_CONTROL, 
                         AUDIT);
                break;
        }
        return new ArrayList<>(result);
    }

}
