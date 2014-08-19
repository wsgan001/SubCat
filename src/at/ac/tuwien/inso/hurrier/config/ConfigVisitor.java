/* ConfigVisitor.java
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

package at.ac.tuwien.inso.hurrier.config;

import at.ac.tuwien.inso.hurrier.config.Query.StringSegment;
import at.ac.tuwien.inso.hurrier.config.Query.VariableSegment;


public class ConfigVisitor {

	public void visitQuery (Query query) {
	}

	public void visitStringSegment (StringSegment stringSegment) {
	}

	public void visitVariableSegment (VariableSegment variableSegment) {
	}

	public void visitPieChartGroupConfig (PieChartGroupConfig pieChartGroupConfig) {
	}

	public void visitPieChartConfig (PieChartConfig pieChartConfig) {
	}

	public void visitTeamViewConfig (TeamViewConfig teamViewConfig) {
	}

	public void visitUserViewConfig (UserViewConfig userViewConfig) {		
	}

	public void visitProjectViewConfig (ProjectViewConfig projectViewConfig) {
	}

	public void visitTrendChartGroupConfig (TrendChartGroupConfig trendChartGroupConfig) {
	}
	
	public void visitTrendChartConfig (TrendChartConfig trendChartConfig) {
	}

	public void visitDropDownConfig (DropDownConfig dropDownConfig) {
	}

	public void visitOptionListConfig (OptionListConfig optionListConfig) {
	}

	public void visitTrendChartPlotConfig (TrendChartPlotConfig trendChartPlotConfig) {
	}

	public void visitDistributionChartConfig (DistributionChartConfig distributionChartConfig) {
	}

	public void visitDistributionChartOptionConfig (ConfigVisitor visitor) {
	}

	public void visitDistributionAttributeConfig (DistributionAttributeConfig distributionAttributeConfig) {
	}

	public void visitDistributionAttributesConfig (DistributionAttributesConfig distributionAttributesConfig) {
	}

}
