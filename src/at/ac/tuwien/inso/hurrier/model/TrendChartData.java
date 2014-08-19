/* TrendChartData.java
 *
 * Copyright (C) 2014  Brosch Florian
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Author:
 * 	Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.hurrier.model;


public class TrendChartData {
	private Integer[] data;
	
	public TrendChartData () {
		assert (data != null);

		this.data = new Integer[12];
	}
	
	public void add (int month, int value) throws IllegalArgumentException {
		if (month <= 0 || month > 12) {
			throw new IllegalArgumentException ("invalid value for month: expected (0 > month < 13), got " + month);
		}
		if (value < 0) {
			throw new IllegalArgumentException ("invalid value for month " + month + ": expected (value >= 0), got " + value);
		}

		data[month - 1] = value;
	}
	
	public Integer[] getData () {
		return data;
	}
}
