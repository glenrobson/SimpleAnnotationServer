package uk.org.llgc.annotation.store.data;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

public class DateRange {
	protected Date _start = null;
	protected Date _end = null;
	// YYYY-MM-DDThh:mm:ssZ/YYYY-MM-DDThh:mm:ssZ
	public DateRange(final String pDaterange) throws ParseException {
		String[] tDateStr = pDaterange.split("/");
		SimpleDateFormat tFormat = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ssZ");

		_start = tFormat.parse(tDateStr[0]);
		_end = tFormat.parse(tDateStr[1]);
	}
	
	/**
	 * Get start.
	 *
	 * @return start as Date.
	 */
	public Date getStart() {
	    return _start;
	}
	
	/**
	 * Set start.
	 *
	 * @param start the value to set.
	 */
	public void setStart(final Date pStart) {
	     _start = pStart;
	}
	
	/**
	 * Get end.
	 *
	 * @return end as Date.
	 */
	public Date getEnd() {
	    return _end;
	}
	
	/**
	 * Set end.
	 *
	 * @param end the value to set.
	 */
	public void setEnd(final Date pEnd) {
	     _end = pEnd;
	}
}
