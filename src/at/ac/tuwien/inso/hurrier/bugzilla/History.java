/* History.java
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

package at.ac.tuwien.inso.hurrier.bugzilla;

import java.util.Date;


public class History {
	private Date when;
	private String who;
	private Change[] changes;

	public History (Date when, String who, Change[] changes) {
		assert (when != null);
		assert (who != null);
		assert (changes != null);
		
		this.when = when;
		this.who = who;
		this.changes = changes;
	}

	public Date getWhen () {
		return when;
	}

	public String getWho () {
		return who;
	}

	public Change[] getChanges () {
		return changes;
	}

	public String toString () {
		return "[History when="
			+ when
			+ " who="
			+ who
			+ " changes="
			+ changes.length
			+ "]";
	}
}
