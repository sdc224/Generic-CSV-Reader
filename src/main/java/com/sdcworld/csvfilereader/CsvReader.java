/**
 * 
 */
package com.sdcworld.csvfilereader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sdcworld.utility.ConvertUtility;
import com.sdcworld.utility.StringUtility;

/**
 * @author souro
 *
 */
public class CsvReader<T> {

	private String filePath;

	private Class<T> genericType;

	private List<T> listOfData;

	private List<String> orderOfFields;

	private String seperator;

	private boolean hasHeader;

	private List<String> listOfheader;

	private boolean hasAutomaticField;

	private boolean hasDifferentNullIndicator;

	private Set<String> nullIndicators = new HashSet<String>();

	private Map<String, Field> privateFields = new LinkedHashMap<String, Field>();

	private boolean isInitializationCompleted;

	// Constructors
	public CsvReader(final Class<T> type, String file, boolean hasHeader) {
		this.filePath = file;
		this.hasHeader = hasHeader;
		this.nullIndicators.add("");
		this.genericType = type;
		this.seperator = ","; // default
	}

	public CsvReader(final Class<T> type, String file, boolean hasHeader, boolean hasDifferentNullIndicator,
			Set<String> nullIndicator) {
		this.filePath = file;
		this.hasHeader = hasHeader;
		this.nullIndicators.add("");
		for (String nullInd : nullIndicator)
			this.nullIndicators.add(nullInd);
		this.genericType = type;
		this.seperator = ","; // default
	}

	public CsvReader(final Class<T> type, String file, boolean hasHeader, boolean hasAutomaticField) {
		this.filePath = file;
		this.hasHeader = hasHeader;
		this.nullIndicators.add("");
		this.hasAutomaticField = hasAutomaticField;
		this.genericType = type;
		this.seperator = ","; // default
	}

	public CsvReader(final Class<T> type, String file, boolean hasHeader, String seperator,
			boolean hasDifferentNullIndicator, Set<String> nullIndicator) {
		this.filePath = file;
		this.hasHeader = hasHeader;
		this.hasDifferentNullIndicator = true;
		this.nullIndicators.add("");
		for (String nullInd : nullIndicator)
			this.nullIndicators.add(nullInd);
		this.genericType = type;
		this.seperator = seperator;
	}

	public CsvReader(final Class<T> type, String file, boolean hasHeader, String separator) {
		this.filePath = file;
		this.hasHeader = hasHeader;
		this.genericType = type;
		this.seperator = separator;
		this.nullIndicators.add("");
	}

	public CsvReader(final Class<T> type, String file, boolean hasHeader, boolean hasAutomaticField, String separator) {
		this.filePath = file;
		this.hasHeader = hasHeader;
		this.nullIndicators.add("");
		this.hasAutomaticField = hasAutomaticField;
		this.genericType = type;
		this.seperator = separator;
	}

	// All methods

	/**
	 * Initialize field and start reading CSV file
	 */
	private void initialize() {
		if (!this.isInitializationCompleted) {
			// this section is only for finding private fields
			// in a class to add them in field class
			Field[] fields = genericType.getDeclaredFields();
			for (Field field : fields) {
				if (Modifier.isPrivate(field.getModifiers())) {
					privateFields.put(field.getName(), field);
				}
			}

			try {
				readCsvFile();
			} catch (IllegalAccessException | InstantiationException e) {
				this.isInitializationCompleted = false;
			}

			this.isInitializationCompleted = true;
		}
	}

	/**
	 * @return the listOfData
	 */
	public List<T> getListOfData() {

		// I don't know why equals method not working, but I have not checked yet
		if (listOfData == null) {
			listOfData = new ArrayList<>();
		}

		return listOfData;
	}

	/**
	 * @param listOfData the listOfData to set
	 */
	public void setListOfData(List<T> listOfData) {
		this.listOfData = listOfData;
	}

	/**
	 * @return the orderOfFields
	 */
	public List<String> getOrderOfFields() {
		return orderOfFields;
	}

	/**
	 * @param orderOfFields the orderOfFields to set
	 */
	public CsvReader<T> setOrderOfFields(List<String> orderOfFields) {
		this.orderOfFields = orderOfFields;
		return this;
	}

	/**
	 * @return the listOfheader
	 */
	public List<String> getListOfheader() {
		return listOfheader;
	}

	/**
	 * @param listOfheader the listOfheader to set
	 */
	public void setListOfheader(List<String> listOfheader) {
		this.listOfheader = listOfheader;
	}

	public CsvReader<T> readCSV(List<String> orderOfFields) {
		this.setOrderOfFields(orderOfFields);
		initialize();
		return this;
	}

	public CsvReader<T> readCSV() {
		initialize();
		return this;
	}

	private void readCsvFile() throws IllegalAccessException, InstantiationException {

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(filePath));

			String line = null;

			while ((line = reader.readLine()) != null) {

				List<String> row = Arrays.asList(line.split(seperator));

				if (this.hasHeader) {
					this.hasHeader = false;
					this.setListOfheader(row);

					if (this.getOrderOfFields() == null) {
						List<String> order = new ArrayList<>();

						for (String column : getListOfheader()) {
							order.add(StringUtility.toCamelCase(column));
						}

						this.setOrderOfFields(order);
					}

					continue;
				}

				// The generic magic starts from here
				T refObject = genericType.newInstance();
				int index = 0;

				List<String> listOfFieldNames;
				// as reverse is not always true
				listOfFieldNames = (null != getOrderOfFields()) ? getOrderOfFields()
						: new ArrayList<>(privateFields.keySet());

				for (String fieldName : listOfFieldNames) {
					if (index >= row.size()) {
						break;
					}

					assign(refObject, privateFields.get(fieldName), row.get(index++));
				}

				getListOfData().add(refObject);
			}

			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		finally {
			try {
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Field assign(T refObject, Field field, String value) throws IllegalAccessException {

		field.setAccessible(true);
		try {

			if (nullIndicators.contains(value)) {
				field.set(refObject, ConvertUtility.toObject(field.getType(), null));
			} else {
				field.set(refObject, ConvertUtility.toObject(field.getType(), value));
			}
		} catch (Exception e) {
			field.set(refObject, null);
		}
		field.setAccessible(false);
		return field;
	}
}
