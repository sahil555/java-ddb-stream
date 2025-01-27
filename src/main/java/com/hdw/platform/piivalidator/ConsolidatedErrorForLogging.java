package com.hdw.platform.piivalidator;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@Component
public class ConsolidatedErrorForLogging {

  public boolean isError = false;
  public HashMap<String, MismatchValue> mismatchKey = new HashMap<>();
  public SortedSet<String> missingKeys = new TreeSet<>();
  public HashMap<String, MismatchValue> invalidKeysAndTheirValue = new HashMap<>();

}
