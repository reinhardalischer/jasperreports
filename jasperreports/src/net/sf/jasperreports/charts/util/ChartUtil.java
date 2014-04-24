/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2013 Jaspersoft Corporation. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.charts.util;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import net.sf.jasperreports.charts.ChartTheme;
import net.sf.jasperreports.charts.ChartThemeBundle;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintHyperlink;
import net.sf.jasperreports.engine.JRPrintImageArea;
import net.sf.jasperreports.engine.JRPrintImageAreaHyperlink;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.Renderable;
import net.sf.jasperreports.engine.RenderableUtil;
import net.sf.jasperreports.engine.fill.DefaultChartTheme;
import net.sf.jasperreports.engine.util.JRSingletonCache;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.data.Range;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 * @version $Id$
 */
public final class ChartUtil
{
	/**
	 *
	 */
	@SuppressWarnings("deprecation")
	private static final JRSingletonCache<ChartRendererFactory> CHART_RENDERER_FACTORY_CACHE = 
			new JRSingletonCache<ChartRendererFactory>(ChartRendererFactory.class);
	
	protected static final double AUTO_TICK_UNIT_THRESHOLD = 1e12;
	protected static final double AUTO_TICK_UNIT_FACTOR = 1000d;

	private JasperReportsContext jasperReportsContext;


	/**
	 *
	 */
	private ChartUtil(JasperReportsContext jasperReportsContext)
	{
		this.jasperReportsContext = jasperReportsContext;
	}
	
	
	/**
	 *
	 */
	private static ChartUtil getDefaultInstance()//FIXMECONTEXT check this use of this
	{
		return new ChartUtil(DefaultJasperReportsContext.getInstance());
	}
	
	
	/**
	 *
	 */
	public static ChartUtil getInstance(JasperReportsContext jasperReportsContext)
	{
		return new ChartUtil(jasperReportsContext);
	}
	
	
	/**
	 * 
	 */
	public static List<JRPrintImageAreaHyperlink> getImageAreaHyperlinks(
		JFreeChart chart,
		ChartHyperlinkProvider chartHyperlinkProvider,
		Graphics2D grx,
		Rectangle2D renderingArea
		)// throws JRException
	{
		List<JRPrintImageAreaHyperlink> areaHyperlinks = null;
		
		if (chartHyperlinkProvider != null && chartHyperlinkProvider.hasHyperlinks())
		{
			ChartRenderingInfo renderingInfo = new ChartRenderingInfo();

			if (grx == null)
			{
				chart.createBufferedImage((int) renderingArea.getWidth(), (int)  renderingArea.getHeight(), renderingInfo);
			}
			else
			{
				chart.draw(grx, renderingArea, renderingInfo);
			}
			
			EntityCollection entityCollection = renderingInfo.getEntityCollection();
			if (entityCollection != null && entityCollection.getEntityCount() > 0)
			{
				areaHyperlinks = new ArrayList<JRPrintImageAreaHyperlink>(entityCollection.getEntityCount());
				
				for (@SuppressWarnings("unchecked")
				Iterator<ChartEntity> it = entityCollection.iterator(); it.hasNext();)
				{
					ChartEntity entity = it.next();
					JRPrintHyperlink printHyperlink = chartHyperlinkProvider.getEntityHyperlink(entity);
					if (printHyperlink != null)
					{
						JRPrintImageArea area = getImageArea(entity);

						JRPrintImageAreaHyperlink areaHyperlink = new JRPrintImageAreaHyperlink();
						areaHyperlink.setArea(area);
						areaHyperlink.setHyperlink(printHyperlink);
						areaHyperlinks.add(areaHyperlink);
					}
				}
			}
		}
		
		return areaHyperlinks;
	}

	private static JRPrintImageArea getImageArea(ChartEntity entity)
	{
		JRPrintImageArea area = new JRPrintImageArea();
		area.setShape(JRPrintImageArea.getShape(entity.getShapeType()));
		
		int[] coordinates = getCoordinates(entity);
		if (coordinates != null)
		{
			area.setCoordinates(coordinates);
		}
		return area;
	}
	
	private static int[] getCoordinates(ChartEntity entity)
	{
		int[] coordinates = null;
		String shapeCoords = entity.getShapeCoords();
		if (shapeCoords != null && shapeCoords.length() > 0)
		{
			StringTokenizer tokens = new StringTokenizer(shapeCoords, ",");
			coordinates = new int[tokens.countTokens()];
			int idx = 0;
			while (tokens.hasMoreTokens())
			{
				String coord = tokens.nextToken();
				coordinates[idx] = Integer.parseInt(coord);
				++idx;
			}
		}
		return coordinates;
	}

	/**
	 * 
	 */
	public ChartTheme getTheme(String themeName)
	{
		if (themeName == null)
		{
			return new DefaultChartTheme();
		}

		List<ChartThemeBundle> themeBundles = jasperReportsContext.getExtensions(ChartThemeBundle.class);
		for (Iterator<ChartThemeBundle> it = themeBundles.iterator(); it.hasNext();)
		{
			ChartThemeBundle bundle = it.next();
			ChartTheme chartTheme = bundle.getChartTheme(themeName);
			if (chartTheme != null)
			{
				return chartTheme;
			}
		}
		throw new JRRuntimeException("Chart theme '" + themeName + "' not found.");
	}

	/**
	 * @deprecated Replaced by {@link #getTheme(String)}.
	 */
	public static ChartTheme getChartTheme(String themeName)
	{
		return getDefaultInstance().getTheme(themeName);
	}

	/**
	 * 
	 */
	public ChartRenderableFactory getChartRenderableFactory(String renderType)
	{
		String factoryClass = JRPropertiesUtil.getInstance(jasperReportsContext).getProperty(ChartRenderableFactory.PROPERTY_CHART_RENDERER_FACTORY_PREFIX + renderType);
		if (factoryClass == null)
		{
			throw new JRRuntimeException("No chart renderer factory specifyed for '" + renderType + "' render type.");
		}

		try
		{
			@SuppressWarnings("deprecation")
			ChartRendererFactory factory = CHART_RENDERER_FACTORY_CACHE.getCachedInstance(factoryClass);
			if (factory instanceof ChartRenderableFactory)
			{
				return (ChartRenderableFactory)factory;
			}
			
			return new WrappingChartRenderableFactory(factory); 
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	/**
	 * @deprecated Replaced by {@link #getChartRenderableFactory(String)}.
	 */
	public static ChartRendererFactory getChartRendererFactory(String renderType)
	{
		return getDefaultInstance().getChartRenderableFactory(renderType);
	}

	/**
	 * @deprecated To be removed.
	 */
	public static class WrappingChartRenderableFactory implements ChartRenderableFactory
	{
		private ChartRendererFactory factory;
		
		public WrappingChartRenderableFactory(ChartRendererFactory factory)
		{
			this.factory = factory;
		}

		public net.sf.jasperreports.engine.JRRenderable getRenderer(
			JFreeChart chart,
			ChartHyperlinkProvider chartHyperlinkProvider,
			Rectangle2D rectangle
			) 
		{
			return factory.getRenderer(chart, chartHyperlinkProvider, rectangle);
		}

		public Renderable getRenderable(
			JasperReportsContext jasperReportsContext,
			JFreeChart chart,
			ChartHyperlinkProvider chartHyperlinkProvider,
			Rectangle2D rectangle) 
		{
			net.sf.jasperreports.engine.JRRenderable deprecatedRenderer 
				= getRenderer(chart, chartHyperlinkProvider, rectangle);
			return RenderableUtil.getWrappingRenderable(deprecatedRenderer);
		}
	}
	
	/**
	 * @deprecated replaced by {@link #createIntegerTickUnits(Locale)}
	 */
	@Deprecated
	public TickUnitSource createIntegerTickUnits()
	{
		return createIntegerTickUnits(Locale.getDefault());
	}
	
	public TickUnitSource createIntegerTickUnits(Locale locale)
	{
        DecimalFormatSymbols formatSymbols = DecimalFormatSymbols.getInstance(locale);
        
		// copied from NumberAxis.createIntegerTickUnits() to preserve backward behaviour
        TickUnits units = new TickUnits();
		DecimalFormat df0 = new DecimalFormat("0", formatSymbols);
        DecimalFormat df1 = new DecimalFormat("#,##0", formatSymbols);
        units.add(new NumberTickUnit(1, df0));
        units.add(new NumberTickUnit(2, df0));
        units.add(new NumberTickUnit(5, df0));
        units.add(new NumberTickUnit(10, df0));
        units.add(new NumberTickUnit(20, df0));
        units.add(new NumberTickUnit(50, df0));
        units.add(new NumberTickUnit(100, df0));
        units.add(new NumberTickUnit(200, df0));
        units.add(new NumberTickUnit(500, df0));
        units.add(new NumberTickUnit(1000, df1));
        units.add(new NumberTickUnit(2000, df1));
        units.add(new NumberTickUnit(5000, df1));
        units.add(new NumberTickUnit(10000, df1));
        units.add(new NumberTickUnit(20000, df1));
        units.add(new NumberTickUnit(50000, df1));
        units.add(new NumberTickUnit(100000, df1));
        units.add(new NumberTickUnit(200000, df1));
        units.add(new NumberTickUnit(500000, df1));
        units.add(new NumberTickUnit(1000000, df1));
        units.add(new NumberTickUnit(2000000, df1));
        units.add(new NumberTickUnit(5000000, df1));
        units.add(new NumberTickUnit(10000000, df1));
        units.add(new NumberTickUnit(20000000, df1));
        units.add(new NumberTickUnit(50000000, df1));
        units.add(new NumberTickUnit(100000000, df1));
        units.add(new NumberTickUnit(200000000, df1));
        units.add(new NumberTickUnit(500000000, df1));
        units.add(new NumberTickUnit(1000000000, df1));
        units.add(new NumberTickUnit(2000000000, df1));
        units.add(new NumberTickUnit(5000000000.0, df1));
        units.add(new NumberTickUnit(10000000000.0, df1));
		
		// adding further values by default because 1E10 is not enough for some people
		// using getNumberInstance because that's what NumberAxis.createIntegerTickUnits does
		units.add(new NumberTickUnit(20000000000L, df1));
		units.add(new NumberTickUnit(50000000000L, df1));
		units.add(new NumberTickUnit(100000000000L, df1));
		units.add(new NumberTickUnit(200000000000L, df1));
		units.add(new NumberTickUnit(500000000000L, df1));
		units.add(new NumberTickUnit(1000000000000L, df1));
		units.add(new NumberTickUnit(2000000000000L, df1));
		units.add(new NumberTickUnit(5000000000000L, df1));
		units.add(new NumberTickUnit(10000000000000L, df1));
		units.add(new NumberTickUnit(20000000000000L, df1));
		units.add(new NumberTickUnit(50000000000000L, df1));
		units.add(new NumberTickUnit(100000000000000L, df1));
		units.add(new NumberTickUnit(200000000000000L, df1));
		units.add(new NumberTickUnit(500000000000000L, df1));
		units.add(new NumberTickUnit(1000000000000000L, df1));
		units.add(new NumberTickUnit(2000000000000000L, df1));
		units.add(new NumberTickUnit(5000000000000000L, df1));
		units.add(new NumberTickUnit(10000000000000000L, df1));
		units.add(new NumberTickUnit(20000000000000000L, df1));
		units.add(new NumberTickUnit(50000000000000000L, df1));
		units.add(new NumberTickUnit(100000000000000000L, df1));
		units.add(new NumberTickUnit(200000000000000000L, df1));
		units.add(new NumberTickUnit(500000000000000000L, df1));
		units.add(new NumberTickUnit(1000000000000000000L, df1));
		units.add(new NumberTickUnit(2000000000000000000L, df1));
		units.add(new NumberTickUnit(5000000000000000000L, df1));
		
		return units;
	}
	
	/**
	 * @deprecated replaced by {@link #createStandardTickUnits(Locale)}
	 */
	@Deprecated
	public TickUnitSource createStandardTickUnits()
	{
		return createStandardTickUnits(Locale.getDefault());
	}
	
	public TickUnitSource createStandardTickUnits(Locale locale)
	{
        DecimalFormatSymbols formatSymbols = DecimalFormatSymbols.getInstance(locale);
        
		//copied from NumberAxis.createStandardTickUnits() to preserve backward behaviour 
        TickUnits units = new TickUnits();
        DecimalFormat df0 = new DecimalFormat("0.00000000", formatSymbols);
        DecimalFormat df1 = new DecimalFormat("0.0000000", formatSymbols);
        DecimalFormat df2 = new DecimalFormat("0.000000", formatSymbols);
        DecimalFormat df3 = new DecimalFormat("0.00000", formatSymbols);
        DecimalFormat df4 = new DecimalFormat("0.0000", formatSymbols);
        DecimalFormat df5 = new DecimalFormat("0.000", formatSymbols);
        DecimalFormat df6 = new DecimalFormat("0.00", formatSymbols);
        DecimalFormat df7 = new DecimalFormat("0.0", formatSymbols);
        DecimalFormat df8 = new DecimalFormat("#,##0", formatSymbols);
        //these two are probably not needed
        DecimalFormat df9 = new DecimalFormat("#,###,##0", formatSymbols);
        DecimalFormat df10 = new DecimalFormat("#,###,###,##0", formatSymbols);

        // we can add the units in any order, the TickUnits collection will
        // sort them...
        units.add(new NumberTickUnit(0.0000001, df1));
        units.add(new NumberTickUnit(0.000001, df2));
        units.add(new NumberTickUnit(0.00001, df3));
        units.add(new NumberTickUnit(0.0001, df4));
        units.add(new NumberTickUnit(0.001, df5));
        units.add(new NumberTickUnit(0.01, df6));
        units.add(new NumberTickUnit(0.1, df7));
        units.add(new NumberTickUnit(1, df8));
        units.add(new NumberTickUnit(10, df8));
        units.add(new NumberTickUnit(100, df8));
        units.add(new NumberTickUnit(1000, df8));
        units.add(new NumberTickUnit(10000, df8));
        units.add(new NumberTickUnit(100000, df8));
        units.add(new NumberTickUnit(1000000, df9));
        units.add(new NumberTickUnit(10000000, df9));
        units.add(new NumberTickUnit(100000000, df9));
        units.add(new NumberTickUnit(1000000000, df10));
        units.add(new NumberTickUnit(10000000000.0, df10));
        units.add(new NumberTickUnit(100000000000.0, df10));

        units.add(new NumberTickUnit(0.00000025, df0));
        units.add(new NumberTickUnit(0.0000025, df1));
        units.add(new NumberTickUnit(0.000025, df2));
        units.add(new NumberTickUnit(0.00025, df3));
        units.add(new NumberTickUnit(0.0025, df4));
        units.add(new NumberTickUnit(0.025, df5));
        units.add(new NumberTickUnit(0.25, df6));
        units.add(new NumberTickUnit(2.5, df7));
        units.add(new NumberTickUnit(25, df8));
        units.add(new NumberTickUnit(250, df8));
        units.add(new NumberTickUnit(2500, df8));
        units.add(new NumberTickUnit(25000, df8));
        units.add(new NumberTickUnit(250000, df8));
        units.add(new NumberTickUnit(2500000, df9));
        units.add(new NumberTickUnit(25000000, df9));
        units.add(new NumberTickUnit(250000000, df9));
        units.add(new NumberTickUnit(2500000000.0, df10));
        units.add(new NumberTickUnit(25000000000.0, df10));
        units.add(new NumberTickUnit(250000000000.0, df10));

        units.add(new NumberTickUnit(0.0000005, df1));
        units.add(new NumberTickUnit(0.000005, df2));
        units.add(new NumberTickUnit(0.00005, df3));
        units.add(new NumberTickUnit(0.0005, df4));
        units.add(new NumberTickUnit(0.005, df5));
        units.add(new NumberTickUnit(0.05, df6));
        units.add(new NumberTickUnit(0.5, df7));
        units.add(new NumberTickUnit(5L, df8));
        units.add(new NumberTickUnit(50L, df8));
        units.add(new NumberTickUnit(500L, df8));
        units.add(new NumberTickUnit(5000L, df8));
        units.add(new NumberTickUnit(50000L, df8));
        units.add(new NumberTickUnit(500000L, df8));
        units.add(new NumberTickUnit(5000000L, df9));
        units.add(new NumberTickUnit(50000000L, df9));
        units.add(new NumberTickUnit(500000000L, df9));
        units.add(new NumberTickUnit(5000000000L, df10));
        units.add(new NumberTickUnit(50000000000L, df10));
        units.add(new NumberTickUnit(500000000000L, df10));
		
		// adding further values by default because 5E11 is not enough for some people
		units.add(new NumberTickUnit(1000000000000L, df8));
		units.add(new NumberTickUnit(2500000000000L, df8));
		units.add(new NumberTickUnit(5000000000000L, df8));
		units.add(new NumberTickUnit(10000000000000L, df8));
		units.add(new NumberTickUnit(25000000000000L, df8));
		units.add(new NumberTickUnit(50000000000000L, df8));
		units.add(new NumberTickUnit(100000000000000L, df8));
		units.add(new NumberTickUnit(250000000000000L, df8));
		units.add(new NumberTickUnit(500000000000000L, df8));
		units.add(new NumberTickUnit(1000000000000000L, df8));
		units.add(new NumberTickUnit(2500000000000000L, df8));
		units.add(new NumberTickUnit(5000000000000000L, df8));
		units.add(new NumberTickUnit(10000000000000000L, df8));
		units.add(new NumberTickUnit(25000000000000000L, df8));
		units.add(new NumberTickUnit(50000000000000000L, df8));
		units.add(new NumberTickUnit(100000000000000000L, df8));
		units.add(new NumberTickUnit(250000000000000000L, df8));
		units.add(new NumberTickUnit(500000000000000000L, df8));
		units.add(new NumberTickUnit(1000000000000000000L, df8));
		units.add(new NumberTickUnit(2500000000000000000L, df8));
		units.add(new NumberTickUnit(5000000000000000000L, df8));

		return units;
	}
	
	public void setAutoTickUnit(NumberAxis numberAxis)
	{
		if (numberAxis.isAutoTickUnitSelection())
		{
			Range range = numberAxis.getRange();
			if (range.getLength() >= AUTO_TICK_UNIT_THRESHOLD)
			{
				// this is a workaround for a floating point error makes JFreeChart
				// select tick units that are too small when the values are very large
				double autoSize = range.getLength() / AUTO_TICK_UNIT_THRESHOLD;
				TickUnit unit = numberAxis.getStandardTickUnits().getCeilingTickUnit(autoSize);
				numberAxis.setTickUnit((NumberTickUnit) unit, false, false);
			}
		}
	}
}
