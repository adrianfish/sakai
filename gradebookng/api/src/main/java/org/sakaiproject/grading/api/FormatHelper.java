/**
 * Copyright (c) 2003-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.grading.api;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public interface FormatHelper {

    /**
	 * The value is a double (ie 12.34542) that needs to be formatted as a percentage with two decimal places precision. And drop off any .0
	 * if no decimal places.
	 *
	 * @param score as a double
	 * @return double to decimal places
	 */
	String formatDoubleToDecimal(Double score);

	/**
	 * Convert a double score to match the number of decimal places exhibited in the toMatch string representation of a number
	 *
	 * @param score as a double
	 * @param toMatch the number as a string
	 * @return double to decimal places
	 */
	String formatDoubleToMatch(Double score, String toMatch);

	/**
	 * The value is a double (ie 12.34) that needs to be formatted as a percentage with two decimal places precision.
	 *
	 * @param score as a double
	 * @return percentage to decimal places with a '%' for good measure
	 */
	String formatDoubleAsPercentage(Double score);

	/**
	 * Format the given string as a percentage with two decimal precision. String should be something that can be converted to a number.
	 *
	 * @param string string representation of the number
	 * @return percentage to decimal places with a '%' for good measure
	 */
	String formatStringAsPercentage(String string);

	/**
	 * Format a grade, e.g. 00 => 0 0001 => 1 1.0 => 1 1.25 => 1.25 based on the root locale
	 *
	 * @param grade
	 * @return
	 */
	String formatGrade(String grade);

	/**
	 * Format a grade, e.g. 00 => 0 0001 => 1 1.0 => 1 1.25 => 1.25 based on the user's locale
	 *
	 * @param grade - string representation of a grade
	 * @return - string formatted per the user's preferred locale
	 */
	String formatGradeFromUserLocale(String grade);

	/**
	 * Format a grade, e.g. 00 => 0 0001 => 1 1.0 => 1 1.25 => 1.25
	 *
	 * @param grade - string representation of a grade
	 * @return
	 */
	String formatGradeForDisplay(Double grade);

	/**
	 * Format a grade from the root locale for display using the user's locale
	 *
	 * @param grade - string representation of a grade
	 * @return
	 */
	String formatGradeForDisplay(String grade);

	/**
	 * Convert an empty grade to a dash for display purposes
	 *
	 * @param grade
	 * @return a dash if the grade is empty, the original grade if not
	 */
	String convertEmptyGradeToDash(String grade);

	/**
	 * Format a grade using the locale
	 *
	 * @param grade - string representation of a grade
	 * @param locale
	 * @return
	 */
	String formatGradeForLocale(String grade, Locale locale);

	/**
	 * Format a date but return ifNull if null
	 *
	 * @param date
	 * @param ifNull
	 * @return
	 */
	String formatDate(Date date, String ifNull);

	/**
	 * Strips out line breaks
	 *
	 * @param s String to abbreviate
	 * @return string without line breaks
	 */
	String stripLineBreaks(String s);

	/**
	 * Abbreviate a string via {@link StringUtils#abbreviateMiddle(String, String, int)}
	 *
	 * Set at 45 chars
	 *
	 * @param s String to abbreviate
	 * @return abbreviated string or full string if it was shorter than the setting
	 */
	String abbreviateMiddle(String s);

	/**
	 * Validate/convert a Double using the user's Locale.
	 *
	 * @param value - The value validation is being performed on.
	 * @return The parsed Double if valid or null if invalid.
	 */
	Double validateDouble(String value);

	/**
	 * Helper to encode a string and avoid the ridiculous exception that is never thrown
	 *
	 * @param s
	 * @return encoded s
	 */
	String encode(String s);

	/**
	 * Helper to decode a string and avoid the ridiculous exception that is never thrown
	 *
	 * @param s
	 * @return decoded s
	 */
	String decode(String s);

	/**
	 * Returns a list of drop highest/lowest labels based on the settings of the given category.
	 * @param category the category
	 * @return a list of 1 or 2 labels indicating that drop highest/lowest is in use, or an empty list if not in use.
	 */
	List<String> formatCategoryDropInfo(CategoryDefinition category);

	/**
	* Turn special characters into HTML character references. Handles complete character set defined in HTML 4.01 recommendation.
	* Escapes all special characters to their corresponding entity reference (e.g. &lt;) at least as required by the specified encoding. In other words, if a special character does not have to be escaped for the given encoding, it may not be.
	* Reference: http://www.w3.org/TR/html4/sgml/entities.html
	 */
	String htmlEscape(String input);
	
	String htmlUnescape(String input);

	/**
	 * Helper to accept numerical grades and get the scale value.
	 * Returns the scale whose value equals to the numeric value received, or if it doesn't exists, the highest value lower.
	 *
	 * @param newGrade the grade to convert
	 * @param schema the current schema of Gradebook
	 * @param currentUserLocale the locale to format the grade with the right decimal separator
	 * @return fully formatted string ready for display
	 */
	String getGradeFromNumber(String newGrade, Map<String, Double> schema, Locale currentUserLocale);

	/**
	 *
	 * @param gradeScale the scale value to convert
	 * @param schema the current schema of Gradebook
	 * @param currentUserLocale the locale to format the grade with the right decimal separator
	 * @return the grade
	 */
	String getNumberFromGrade(String gradeScale, Map<String, Double> schema, Locale currentUserLocale);

	/**
	 *
	 * @param newGrade the grade to transform
	 * @param locale the locale to format the grade with the right decimal separator
	 * @return the new grade
	 */
	String transformNewGrade(String newGrade, Locale locale);

	/*
	 *
	 * Method to normalize a grade by removing trailing ".0" or ",0" and trimming it to null if it becomes empty
	 * @param grade String to transform
	 * @return normalized grade stripped of trailing .0 or ,0
	 */
	String normalizeGrade(String grade);
}
