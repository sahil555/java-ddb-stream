package com.hdw.platform.piivalidator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
//@Component
public class PIIConfig {

  @JsonProperty("enabled")
  private Boolean enabled;

  @JsonProperty("piiKeys")
  private Set<String> piiKeys;

  @JsonProperty("indexed_keys")
  private Set<String> indexedKeys;

  @JsonProperty("nottoloop")
  private Set<String> nottoloop;

  final private Set<String> kycFieldsToCheck = Set.of(
      "mobile",
      "email",
//      "accountNumber",
//      "amazonPayId",
//      "bankName",
//      "upiNameAtBank",
//      "upi",
      "panNumber",
      "panName",
//      "id_number",
      "name_on_card",
      "googleIdEmail",
      "facebookIdEmail",
//      "accountNo",
//      "account_number",
      "kycDocNumber",
      "kycUserName",
      "kycDOB",
      "panImage",
      "kycImageURL1",
      "kycImageURL2",
//      "accountName",
      "dob",
      "address",
      "emailEntered",
      "appleIdEmail",
      "googleId",
      "facebookId",
      "aadharName",
      "aadharNumber",
      "house_number",
      "first_name",
      "last_name",
      "middle_name",
      "nsdlName",
      "date_of_birth",
      "fathers_name",
      "panURL",
      "pincode",
      "street_address",
      "year_of_birth",
      "onfidoName",
      "panNameAtConsent",
      "digilockerPanNumber",
      "digilockerPanName",
      "ocrPanNumber",
      "ocrPanNsdlName",
      "age",
      "aadhaarLimitedFormIDInput",
      "aadhaarLimitedState",
      "aadhaarLimitedGender",
      "aadhaarLimitedAgeBand",
      "digilockerName",
      "digilockerDob",
      "digilockerGender",
      "digilockerAddress",
      "digilockerIdNumber",
      "ocrDob",
      "ocrName",
      "ocrIdNumber",
      "ocrAddress",
      "ocrGender",
//      "lower_limit",
//      "upper_limit",
      "gender",
      "mobile_no",
      "customer_email",
      "customer_phone"
  );
}
