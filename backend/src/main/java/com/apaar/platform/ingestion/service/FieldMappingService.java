package com.apaar.platform.ingestion.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FieldMappingService {

    public static final String FIELD_STUDENT_IDENTIFIER = "studentIdentifier";
    public static final String FIELD_APAAR_ID = "apaarId";
    public static final String FIELD_FULL_NAME = "fullName";
    public static final String FIELD_DATE_OF_BIRTH = "dateOfBirth";
    public static final String FIELD_GENDER = "gender";
    public static final String FIELD_ADMISSION_CATEGORY = "admissionCategory";
    public static final String FIELD_ADMISSION_DATE = "admissionDate";
    public static final String FIELD_PROGRAMME_CODE = "programmeCode";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_GUARDIAN_NAME = "guardianName";
    public static final String FIELD_ADDRESS_LINE = "addressLine";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_QUOTA_TYPE = "quotaType";
    public static final String FIELD_SUPERNUMERARY = "supernumerary";
    public static final String FIELD_NRI = "nriClassification";
    public static final String FIELD_OCI = "ociClassification";
    public static final String FIELD_FOREIGN_NATIONAL = "foreignNationalClassification";

    public static final Set<String> MANDATORY_FIELDS = Set.of(
            FIELD_STUDENT_IDENTIFIER,
            FIELD_FULL_NAME,
            FIELD_ADMISSION_CATEGORY
    );

    private static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put(FIELD_STUDENT_IDENTIFIER, List.of(
                "studentid", "studentidentifier", "registernumber", "registerno", "regno", "rollno",
                "enrollmentnumber", "enrolmentnumber", "enrolmentno", "enrollmentno", "admissionno", "student_id",
                "registrationno", "registrationnumber", "admnumber", "admno"
        ));
        ALIASES.put(FIELD_APAAR_ID, List.of(
                "apaarid", "apaar", "nationalstudentid", "apaarnumber", "apaar_id", "nationalid"
        ));
        ALIASES.put(FIELD_FULL_NAME, List.of(
                "fullname", "studentname", "candidatename", "name", "full_name", "student_name"
        ));
        ALIASES.put(FIELD_DATE_OF_BIRTH, List.of(
                "dateofbirth", "dob", "birthdate", "date_of_birth", "birth_date"
        ));
        ALIASES.put(FIELD_GENDER, List.of(
                "gender", "sex"
        ));
        ALIASES.put(FIELD_ADMISSION_CATEGORY, List.of(
                "admissioncategory", "category", "seatcategory", "socialcategory", "castecategory", "admission_category"
        ));
        ALIASES.put(FIELD_ADMISSION_DATE, List.of(
                "admissiondate", "dateofadmission", "enrolmentdate", "enrollmentdate", "admission_date"
        ));
        ALIASES.put(FIELD_PROGRAMME_CODE, List.of(
                "programme", "course", "programmecode", "degree", "branch", "department", "programme_code", "course_code"
        ));
        ALIASES.put(FIELD_EMAIL, List.of(
                "email", "emailaddress", "studentemail", "email_id", "emailid", "mailid", "mail", "contactemail"
        ));
        ALIASES.put(FIELD_PHONE, List.of(
                "phone", "mobile", "contactnumber", "phonenumber", "mobileno", "contact_no",
                "contactphone", "contactno", "contact", "mobilenumber", "cell", "cellphone",
                "telephone", "studentphone", "studentmobile"
        ));
        ALIASES.put(FIELD_GUARDIAN_NAME, List.of(
                "guardianname", "fathername", "parentname", "fathermothername", "guardian_name"
        ));
        ALIASES.put(FIELD_ADDRESS_LINE, List.of(
                "address", "addressline", "permanentaddress", "residentialaddress", "address_line"
        ));
        ALIASES.put(FIELD_STATE, List.of(
                "state", "domicilestate", "homestate", "state_name"
        ));
        ALIASES.put(FIELD_QUOTA_TYPE, List.of(
                "quotatype", "quota", "seatquota", "quota_type"
        ));
        ALIASES.put(FIELD_SUPERNUMERARY, List.of(
                "supernumerary", "issupernumerary", "tfw", "supernumeraryquota"
        ));
        ALIASES.put(FIELD_NRI, List.of(
                "nri", "isnri", "nristatus", "nriclassification"
        ));
        ALIASES.put(FIELD_OCI, List.of(
                "oci", "isoci", "ocistatus", "ociclassification"
        ));
        ALIASES.put(FIELD_FOREIGN_NATIONAL, List.of(
                "foreignnational", "fn", "isforeignnational", "pio", "foreignnationalclassification"
        ));
    }

    public static String normalizeHeader(String header) {
        if (header == null) return "";
        return header.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    public Map<String, String> suggestMappings(List<String> sourceHeaders) {
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> mappedCanonical = new HashSet<>();

        for (String header : sourceHeaders) {
            String normalized = normalizeHeader(header);
            String matchedCanonical = null;

            for (Map.Entry<String, List<String>> entry : ALIASES.entrySet()) {
                String canonical = entry.getKey();
                if (mappedCanonical.contains(canonical)) {
                    continue;
                }
                if (entry.getValue().contains(normalized)) {
                    matchedCanonical = canonical;
                    break;
                }
            }

            if (matchedCanonical != null) {
                result.put(header, matchedCanonical);
                mappedCanonical.add(matchedCanonical);
            }
        }
        return result;
    }

    public List<String> getMissingMandatoryFields(Map<String, String> mappings) {
        Set<String> mappedFields = new HashSet<>(mappings.values());
        List<String> missing = new ArrayList<>();
        for (String mandatory : MANDATORY_FIELDS) {
            if (!mappedFields.contains(mandatory)) {
                missing.add(mandatory);
            }
        }
        return missing;
    }

    public List<String> getUnmappedHeaders(List<String> headers, Map<String, String> mappings) {
        List<String> unmapped = new ArrayList<>();
        for (String h : headers) {
            if (!mappings.containsKey(h) || mappings.get(h) == null || mappings.get(h).isBlank()) {
                unmapped.add(h);
            }
        }
        return unmapped;
    }

    public Map<String, String> mapRowToCanonical(Map<String, String> rawData, Map<String, String> mappings) {
        Map<String, String> canonicalRow = new HashMap<>();
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String rawHeader = entry.getKey();
            String canonicalField = entry.getValue();
            if (canonicalField != null && !canonicalField.isBlank()) {
                String val = rawData.get(rawHeader);
                if (val != null) {
                    canonicalRow.put(canonicalField, val);
                }
            }
        }
        return canonicalRow;
    }

    public List<String> findConflictingMappings(Map<String, String> mappings) {
        List<String> conflicts = new ArrayList<>();
        Map<String, List<String>> reverse = new HashMap<>();

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                reverse.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
            }
        }

        for (Map.Entry<String, List<String>> entry : reverse.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add("Multiple columns " + entry.getValue() + " mapped to " + entry.getKey());
            }
        }
        return conflicts;
    }
}
