/* Comment.java
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


public class Comment {
	private int id;
	private int bugId;
	// not supporte by all versions
	private Integer attachmentId;
	private String text;
	private String creator;
	private Date time;

	public Comment (int id, int bugId, Integer attachmentId, String text,
			String creator, Date time) {

		assert (creator != null);
		assert (time != null);
		assert (text != null);

		this.id = id;
		this.bugId = bugId;
		this.attachmentId = attachmentId;
		this.text = text;
		this.creator = creator;
		this.time = time;
	}

	public String toString () {
		return "[Comment id="
			+ id
			+ " creator="
			+ creator
			+ " attachmentId="
			+ attachmentId
			+ "]";
	}
	
	public int getId () {
		return id;
	}

	public int getBugId () {
		return bugId;
	}

	public Integer getAttachmentId () {
		return attachmentId;
	}

	public String getText () {
		return text;
	}

	public String getCreator () {
		return creator;
	}

	public Date getTime () {
		return time;
	}
}
