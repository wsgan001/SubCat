/* PieChartGroupConfig.java
 *
 * Copyright (C) 2014 Florian Brosch
 *
 * Based on work from Andreas Mauczka
 *
 * This program is developed as part of the research project
 * "Lexical Repository Analyis" which is part of the PhD thesis
 * "Design and evaluation for identification, mapping and profiling
 * of medium sized software chunks" by Andreas Mauczka at
 * INSO - University of Technology Vienna. For questions in regard
 * to the research project contact andreas.mauczka(at)inso.tuwien.ac.at
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
 *       Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.subcat.config;

import java.util.Collection;
import java.util.LinkedList;


public class PieChartGroupConfig extends ChartGroupConfig {
	private Collection<PieChartConfig> charts;
	
	public PieChartGroupConfig(String name, SourcePos start, SourcePos end) {
		super (name, start, end);
		charts = new LinkedList<PieChartConfig> ();
	}

	public void addChart (PieChartConfig chart) {
		assert (chart != null);

		charts.add (chart);
	}
	
	public Collection<PieChartConfig> getCharts () {
		return charts;
	}

	@Override
	public void accept (ConfigVisitor visitor) {
		assert (visitor != null);

		visitor.visitPieChartGroupConfig (this);
	}

	@Override
	public void acceptChildren (ConfigVisitor visitor) {
		super.acceptChildren (visitor);
		
		for (PieChartConfig pie : charts) {
			pie.accept(visitor);
		}
	}
}
