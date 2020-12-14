package org.fog.security;

import static java.util.Arrays.asList;

import java.util.*;

//import di.unipi.socc.fogtorchpi.utils.SecurityParameters;

//import di.unipi.socc.fogtorchpi.utils.SecurityParameters;

public class SecurityConstants {
	
	
	public final static int HOST_TYPES =4;
	
	
	public final static List<String> allCountermeasures = SecurityTaxonomy.getSecurityMeasuresByType("");
	
	
	public final static List<String> set1 = asList(SecurityParameters.ACCESS_LOGS, SecurityParameters.CERTIFICATE);
	public final static List<String> set2 = asList(SecurityParameters.BACKUP, SecurityParameters.ACCESS_CONTROL);
	public final static List<String> set3 = asList(SecurityParameters.AUTHENTICATION, SecurityParameters.FIREWALL);
	public final static List<String> set4 = asList(SecurityParameters.ENCRYPTED_STORAGE, SecurityParameters.ANTI_TAMPERING);
	public final static List<String> set5 = asList(SecurityParameters.IDS_HOST, SecurityParameters.IOT_DATA_ENCRYPTION, SecurityParameters.OBFUSCATED_STORAGE);
	public final static List<String> set6 = asList(SecurityParameters.PROCESS_ISOLATION, SecurityParameters.NODE_ISOLATION_MECHANISMS);
	public final static List<String> set7 = asList(SecurityParameters.PERMISSION_MODEL, SecurityParameters.IDS_NETWORK);
	public final static List<String> set8 = asList(SecurityParameters.RESOURCE_MONITORING, SecurityParameters.PUBLICK_KEY);
	public final static List<String> set9 = asList(SecurityParameters.RESTORE_POINTS, SecurityParameters.WIRELESS_SECURITY);
	public final static List<String> set10 = asList(SecurityParameters.USER_DATA_ISOLATION, SecurityParameters.AUDIT);

	
	public final static List<List<String>> secSets = asList (set1,set2,set3,set4,set5,set6,set7,set8,set9,set10);

	public final static List<String> secType1 = asList(SecurityParameters.AUTHENTICATION, SecurityParameters.ANTI_TAMPERING, SecurityParameters.WIRELESS_SECURITY, SecurityParameters.OBFUSCATED_STORAGE);
	public final static List<String> secType2 = asList(SecurityParameters.ANTI_TAMPERING, SecurityParameters.AUTHENTICATION, SecurityParameters.IOT_DATA_ENCRYPTION, 
			SecurityParameters.FIREWALL, SecurityParameters.PUBLICK_KEY, SecurityParameters.WIRELESS_SECURITY, SecurityParameters.ENCRYPTED_STORAGE);
	public final static List<String> secType3 = asList(SecurityParameters.ACCESS_CONTROL, 
			                                           SecurityParameters.ACCESS_LOGS,
			                                           SecurityParameters.IOT_DATA_ENCRYPTION,
			                                           SecurityParameters.AUTHENTICATION,
			                                           SecurityParameters.FIREWALL,
			                                           SecurityParameters.IDS_HOST,
			                                           SecurityParameters.PUBLICK_KEY,
			                                           SecurityParameters.WIRELESS_SECURITY,
			                                           SecurityParameters.ENCRYPTED_STORAGE);
	
	public final static List<String> secType4 = asList(SecurityParameters.ACCESS_LOGS,
			                                           
			                                           SecurityParameters.AUTHENTICATION,
			                                           SecurityParameters.BACKUP,
			                                           SecurityParameters.RESOURCE_MONITORING,
			                                           SecurityParameters.IOT_DATA_ENCRYPTION,
			                                           SecurityParameters.FIREWALL,
			                                           SecurityParameters.IDS_HOST,
			                                           SecurityParameters.PUBLICK_KEY,
			                                           SecurityParameters.WIRELESS_SECURITY,
			                                           SecurityParameters.ENCRYPTED_STORAGE);
	
	
	
	
	public final static List<String> secType5 = allCountermeasures;
	
	public final static List<List<String>> secTypes = asList (secType1,secType2,secType3,secType4,secType5);
	
	
	public final static List<String> list1 = allCountermeasures;
	
	public final static List<String> list2= asList(SecurityParameters.ANTI_TAMPERING,
            SecurityParameters.WIRELESS_SECURITY,
            SecurityParameters.PUBLICK_KEY);
	
	public final static List<String> list3= asList(
            SecurityParameters.ENCRYPTED_STORAGE,
            SecurityParameters.AUDIT,
            SecurityParameters.RESTORE_POINTS,
            SecurityParameters.BACKUP,
            SecurityParameters.ANTI_TAMPERING
    );
	
	public final static List<String> list4= asList(
            SecurityParameters.ACCESS_LOGS,
            SecurityParameters.FIREWALL,
            SecurityParameters.IDS_NETWORK,
            SecurityParameters.PUBLICK_KEY,
            SecurityParameters.PERMISSION_MODEL,
            SecurityParameters.AUTHENTICATION
    );
	
	public final static List<String> secHost1 = allCountermeasures;
	
	
	public final static List<String> secHost2= asList(
            SecurityParameters.AUTHENTICATION,
            SecurityParameters.PERMISSION_MODEL,
            SecurityParameters.ACCESS_LOGS,
            SecurityParameters.FIREWALL,
            SecurityParameters.PUBLICK_KEY,
            SecurityParameters.WIRELESS_SECURITY);
	
	
	public final static List<String> secHost3= asList(
            SecurityParameters.AUTHENTICATION,
            SecurityParameters.PERMISSION_MODEL,
            SecurityParameters.ACCESS_LOGS,
            SecurityParameters.FIREWALL,
            SecurityParameters.PUBLICK_KEY,
            SecurityParameters.WIRELESS_SECURITY,
            SecurityParameters.ANTI_TAMPERING);

	public final static List<String> secHost4= asList(
            SecurityParameters.AUTHENTICATION,
            SecurityParameters.PERMISSION_MODEL,
            SecurityParameters.ACCESS_LOGS,
            SecurityParameters.FIREWALL,
            SecurityParameters.PUBLICK_KEY,
            SecurityParameters.WIRELESS_SECURITY,
            SecurityParameters.ANTI_TAMPERING,
            SecurityParameters.CERTIFICATE,
            SecurityParameters.BACKUP,
            SecurityParameters.IDS_NETWORK,
            SecurityParameters.IDS_HOST
    );
}

