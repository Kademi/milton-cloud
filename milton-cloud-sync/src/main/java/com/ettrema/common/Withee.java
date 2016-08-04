package com.ettrema.common;

/**
 * General purpose interface for consuming a callback for some type. Typically
 * used in "with" style blocks:
 * 
 * someRsourceProvider.with(new Withee<Resource>() {
 *	 public void with(Resource r) {
 *		... do something with the resource
 *   }
 * }
 *
 * @author bradm
 */
public interface Withee<T> {
	void with(T t) throws Exception;
}
