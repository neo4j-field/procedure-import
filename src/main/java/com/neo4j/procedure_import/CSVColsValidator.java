package com.neo4j.procedure_import;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum CSVColsValidator implements Predicate<String> {
   INSTANCE;

   private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

   /**
    *
    * @param line the input CSV line to validate
    * @return a boolean true if valid, false if not
    */
   @Override
   public boolean test(String line) {
      String[] columns = line.split(",");
      if (columns.length != 4)
         return false;

      if (columns[0].isEmpty() || columns[1].isEmpty() || columns[2].isEmpty() || columns[3].isEmpty())
         return false;

      try {
         LocalDateTime.parse(columns[0], dateTimeFormatter);
      } catch (RuntimeException e) {
         return false;
      }

      if (!columns[1].equals("SMS") && !columns[1].equals("CALL"))
         return false;

      Pattern phonePattern = Pattern.compile("^(\\+\\d{1,3})(\\d{9})");

      Matcher fromNumberMatcher = phonePattern.matcher(columns[2]);
      if (!fromNumberMatcher.matches())
         return false;

      Matcher toNumberMatcher = phonePattern.matcher(columns[3]);
      return toNumberMatcher.matches();
   }
}
