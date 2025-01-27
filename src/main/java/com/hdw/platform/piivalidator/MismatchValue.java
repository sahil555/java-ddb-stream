package com.hdw.platform.piivalidator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MismatchValue {

  private String plaintext;
  private String encryptedText;
}
