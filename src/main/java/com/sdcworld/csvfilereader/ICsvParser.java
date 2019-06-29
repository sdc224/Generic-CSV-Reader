/**
 * 
 */
package com.sdcworld.csvfilereader;

/**
 * CSV parser interface to parse CSV in generic way
 * @author souro
 */
public interface ICsvParser<T> {
	T processCSV(T t);
}
